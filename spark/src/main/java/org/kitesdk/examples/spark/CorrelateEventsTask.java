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

import com.google.common.base.Objects;
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
  String correlatedEventsUri;

  public CorrelateEventsTask(String eventsUri, String correlatedEventsUri) {
    this.eventsUri = eventsUri;
    this.correlatedEventsUri = correlatedEventsUri;
  }

  /*
   * This task correlates events based on IP address and timestamp. The goal is
   * to find any "click" events that come from the same IP address and occur
   * within 5 minutes of an "alert" event. The process works by first converting
   * timestamps into 5 minute increments. This means each event will be mapped
   * to the nearest 5 minute mark before the event happened and the nearest
   * 5 minute mark after the event happened. These rounded timestamps are
   * combined with the IP address of the event to do an approximate self join of
   * the data. The events are then iterated over to check for two conditions:
   *
   *   1) There is an alert event in the same bucket
   *   2) That alert is actually less than 5 minutes apart from the given click
   *
   * The task will write out all of the "alert" events that have at least one
   * "click" event from the same IP address and within 5 minutes along with the
   * list of "click" events that were correlated.
   */
  public void run() throws IOException {
    Configuration conf = new Configuration();
    DatasetKeyInputFormat.configure(conf).readFrom(eventsUri).withType(StandardEvent.class);
    DatasetKeyOutputFormat.configure(conf).writeTo(correlatedEventsUri).withType(CorrelatedEvents.class);

    // Create our Spark configuration and get a Java context
    SparkConf sparkConf = new SparkConf()
        .setAppName("Correlate Events")
        // Configure the use of Kryo serialization including our Avro registrator
        .set("spark.serializer", "org.apache.spark.serializer.KryoSerializer")
        .set("spark.kryo.registrator", "org.kitesdk.examples.spark.AvroKyroRegistrator");
    JavaSparkContext sparkContext = new JavaSparkContext(sparkConf);

    JavaPairRDD<StandardEvent, Void> events = sparkContext.newAPIHadoopRDD(conf,
        DatasetKeyInputFormat.class, StandardEvent.class, Void.class);

    // Map each event to two correlation keys. One with the IP address and the
    // nearest 5 minute interval that happened before the event and one with the
    // IP address and the nearest 5 minute interval that happened after the event
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

    // Group the events by they correlation key
    JavaPairRDD<CorrelationKey, Iterable<StandardEvent>> groupedEvents = mappedEvents.groupByKey();

    // Generate potential matches by creating a list of alerts along with the
    // matched list of clicks. If no alerts were found with this correlation
    // key, then output an empty pair
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

    // Verify that the matched events are true matches (i.e. the timestamps
    // are really less than or equal to 5 minutes apart
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

    // Write the data to a Kite dataset
    matches.saveAsNewAPIHadoopFile("dummy", CorrelatedEvents.class, Void.class,
        DatasetKeyOutputFormat.class, conf);
  }

  private static long createLoTimestamp(long timestamp) {
    return timestamp - (timestamp % FIVE_MIN_MILLIS) - FIVE_MIN_MILLIS;
  }

  private static long createHiTimestamp(long timestamp) {
    return timestamp - (timestamp % FIVE_MIN_MILLIS) + FIVE_MIN_MILLIS;
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
