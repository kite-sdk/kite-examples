/*
 * Copyright 2014 Cloudera, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.kitesdk.examples.spark;

import com.google.common.base.Joiner;
import com.google.common.base.Objects;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.apache.avro.util.Utf8;
import org.apache.hadoop.conf.Configuration;
import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.api.java.function.PairFlatMapFunction;
import org.apache.spark.api.java.function.PairFunction;
import org.kitesdk.data.event.CorrelatedEvents;
import org.kitesdk.data.event.StandardEvent;
import org.kitesdk.data.mapreduce.DatasetKeyInputFormat;
import org.kitesdk.data.mapreduce.DatasetKeyOutputFormat;
import scala.Tuple2;

public class CorrelateEventsTask implements Serializable {

  private static final long FIVE_MIN_MILLIS = TimeUnit.MINUTES.toMillis(5);
  String eventsUri;
  String master;
  String correlatedEventsUri;

  public CorrelateEventsTask(String eventsUri, String master,
      String correlatedEventsUri) {
    this.eventsUri = eventsUri;
    this.master = master;
    this.correlatedEventsUri = correlatedEventsUri;
  }

  public void run() throws IOException {
    Configuration conf = new Configuration();
    DatasetKeyInputFormat.configure(conf).readFrom(eventsUri).withType(StandardEvent.class);
    DatasetKeyOutputFormat.configure(conf).writeTo(correlatedEventsUri).withType(CorrelatedEvents.class);

    SparkConf sparkConf = new SparkConf()
        .setAppName("Correlate Events")
        .setMaster(master)
        .set("spark.serializer", "org.apache.spark.serializer.KryoSerializer")
        .set("spark.kryo.registrator", "org.kitesdk.examples.spark.AvroKyroRegistrator");
    JavaSparkContext sparkContext = new JavaSparkContext(sparkConf);
    addJarFromClass(sparkContext, getClass());
    addJars(sparkContext, System.getenv("HIVE_HOME"), "lib");
    sparkContext.addFile(System.getenv("HIVE_HOME")+"/conf/hive-site.xml");

    JavaPairRDD<StandardEvent, Void> events = sparkContext.newAPIHadoopRDD(conf,
        DatasetKeyInputFormat.class, StandardEvent.class, Void.class);

    JavaPairRDD<CorrelationKey, StandardEvent> mappedEvents = events.flatMapToPair(
        new PairFlatMapFunction<Tuple2<StandardEvent, Void>, CorrelationKey, StandardEvent>() {
          @Override
          public Iterable<Tuple2<CorrelationKey, StandardEvent>>
              call(Tuple2<StandardEvent, Void> t) throws Exception {
            List<Tuple2<CorrelationKey, StandardEvent>> result =
                new ArrayList<Tuple2<CorrelationKey, StandardEvent>>(2);

            StandardEvent event = t._1();
            long loTimestamp = createLoTimestamp(event.getTimestamp());
            long hiTimestamp = createHiTimestamp(event.getTimestamp());
            String ip = event.getIp().toString();

            result.add(new Tuple2<CorrelationKey, StandardEvent>(
                new CorrelationKey(loTimestamp, ip), event));
            result.add(new Tuple2<CorrelationKey, StandardEvent>(
                new CorrelationKey(hiTimestamp, ip), event));

            return result;
          }
        });

    JavaPairRDD<CorrelationKey, Iterable<StandardEvent>> groupedEvents = mappedEvents.groupByKey();
    
    JavaPairRDD<List<StandardEvent>, List<StandardEvent>> potentialMatches = groupedEvents.mapToPair(
        new PairFunction<Tuple2<CorrelationKey, Iterable<StandardEvent>>, List<StandardEvent>, List<StandardEvent>>(){

          @Override
          public Tuple2<List<StandardEvent>, List<StandardEvent>> call(Tuple2<CorrelationKey, Iterable<StandardEvent>> t) throws Exception {
            Iterable<StandardEvent> allEvents = t._2();
            List<StandardEvent> alerts = new ArrayList<StandardEvent>();
            List<StandardEvent> clicks = new ArrayList<StandardEvent>();

            for (StandardEvent event : allEvents) {
              if (event.getEventDetails() != null &&
                  event.getEventDetails().containsKey(new Utf8("type")) &&
                  "alert".equals(event.getEventDetails().get(new Utf8("type")).toString())) {
                alerts.add(event);
              } else if (event.getEventDetails() != null &&
                  event.getEventDetails().containsKey(new Utf8("type")) &&
                  "click".equals(event.getEventDetails().get(new Utf8("type")).toString())) {
                clicks.add(event);
              }
            }

            if (alerts.isEmpty()) {
              return new Tuple2<List<StandardEvent>, List<StandardEvent>>(alerts, alerts);
            } else {
              return new Tuple2<List<StandardEvent>, List<StandardEvent>>(alerts, clicks);
            }
          }
        });

    JavaPairRDD<CorrelatedEvents, Void> matches = potentialMatches.flatMapToPair(
        new PairFlatMapFunction<Tuple2<List<StandardEvent>, List<StandardEvent>>, CorrelatedEvents, Void>() {

        @Override
        public Iterable<Tuple2<CorrelatedEvents, Void>> call(Tuple2<List<StandardEvent>, List<StandardEvent>> t) throws Exception {
          List<Tuple2<CorrelatedEvents, Void>> results =
              new ArrayList<Tuple2<CorrelatedEvents, Void>>();
          List<StandardEvent> alerts = t._1();
          List<StandardEvent> clicks = t._2();

          for (StandardEvent alert : alerts) {
            List<StandardEvent> correlated = new ArrayList<StandardEvent>();
            for (StandardEvent click : clicks) {
              if (Math.abs(alert.getTimestamp() - click.getTimestamp())
                  <= FIVE_MIN_MILLIS) {
                System.out.println(TimeUnit.MILLISECONDS.toSeconds(Math.abs(alert.getTimestamp() - click.getTimestamp())));
                correlated.add(click);
              }
            }
            if (!correlated.isEmpty()) {
              results.add(new Tuple2(CorrelatedEvents.newBuilder()
                  .setEvent(alert)
                  .setCorrelated(correlated)
                  .build(), null));
            }
          }

          return results;
        }
      });

    matches.saveAsNewAPIHadoopFile("dummy", CorrelatedEvents.class, Void.class,
        DatasetKeyOutputFormat.class, conf);
  }

  private static long createLoTimestamp(long timestamp) {
    return timestamp - (timestamp % FIVE_MIN_MILLIS) - FIVE_MIN_MILLIS;
  }

  private static long createHiTimestamp(long timestamp) {
    return timestamp - (timestamp % FIVE_MIN_MILLIS) + FIVE_MIN_MILLIS;
  }

  private void addJarFromClass(JavaSparkContext context, Class<?> klass) {
    String jarPath = klass.getResource("/" +
        klass.getName().replace(".", "/") + ".class").toString()
        .replace("jar:", "")
        .split("!")[0];
    addJar(context, jarPath);
  }

  private void addJars(JavaSparkContext context, String... path) throws IOException {
    String root = Joiner.on('/').join(path);
    String[] jars = new File(root).getCanonicalFile().list(new FilenameFilter() {

      @Override
      public boolean accept(File dir, String name) {
        return name.endsWith(".jar");
      }
    });

    for (String jar : jars) {
      addJar(context, root, jar);
    }
  }

  private void addJar(JavaSparkContext context, String... path) {
    String jarPath = Joiner.on('/').join(path);
    context.addJar(jarPath);
    System.out.println("Adding jar " + jarPath + " to the SparkContext");
  }

  private static class CorrelationKey implements Serializable {
    Long timeStamp;
    String ip;

    public CorrelationKey(Long timeStamp, String ip) {
      this.timeStamp = timeStamp;
      this.ip = ip;
    }

    public String getIp() {
      return ip;
    }

    public void setIp(String ip) {
      this.ip = ip;
    }

    public Long getTimeStamp() {
      return timeStamp;
    }

    public void setTimeStamp(Long timeStamp) {
      this.timeStamp = timeStamp;
    }

    @Override
    public boolean equals(Object obj) {
      if (obj == null) {
        return false;
      }
      if (getClass() != obj.getClass()) {
        return false;
      }
      final CorrelationKey other = (CorrelationKey) obj;

      return Objects.equal(this.timeStamp, other.timeStamp) &&
          Objects.equal(this.ip, other.ip);
    }

    @Override
    public int hashCode() {
      return Objects.hashCode(timeStamp, ip);
    }
  }
}
