/**
 * Copyright 2013 Cloudera Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.kitesdk.examples.demo;

import java.io.Serializable;
import java.net.URI;
import java.util.Calendar;
import java.util.Iterator;
import java.util.TimeZone;
import org.apache.crunch.DoFn;
import org.apache.crunch.Emitter;
import org.apache.crunch.MapFn;
import org.apache.crunch.PCollection;
import org.apache.crunch.Pair;
import org.apache.crunch.Target;
import org.apache.crunch.types.avro.Avros;
import org.apache.crunch.util.CrunchTool;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.util.ToolRunner;
import org.kitesdk.data.Dataset;
import org.kitesdk.data.Datasets;
import org.kitesdk.data.View;
import org.kitesdk.data.crunch.CrunchDatasets;
import org.kitesdk.data.event.StandardEvent;
import org.kitesdk.data.spi.filesystem.FileSystemDatasets;
import org.kitesdk.examples.demo.event.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CreateSessions extends CrunchTool implements Serializable {

  private static final Logger LOG = LoggerFactory.getLogger(CreateSessions.class);

  @Override
  public int run(String[] args) throws Exception {
    // Turn debug on while in development.
    getPipeline().enableDebug();
    getPipeline().getConfiguration().set("crunch.log.job.progress", "true");

    Dataset<StandardEvent> eventsDataset = Datasets.load(
        "dataset:hdfs:/tmp/data/events", StandardEvent.class);

    View<StandardEvent> eventsToProcess;
    if (args.length == 0 || (args.length == 1 && args[0].equals("LATEST"))) {
      // get the current minute
      Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
      cal.set(Calendar.SECOND, 0);
      cal.set(Calendar.MILLISECOND, 0);
      long currentMinute = cal.getTimeInMillis();
      // restrict events to before the current minute
      // in the workflow, this also has a lower bound for the timestamp
      eventsToProcess = eventsDataset.toBefore("timestamp", currentMinute);

    } else if (isView(args[0])) {
      eventsToProcess = Datasets.load(args[0], StandardEvent.class);
    } else {
      eventsToProcess = FileSystemDatasets.viewForPath(eventsDataset, new Path(args[0]));
    }

    if (eventsToProcess.isEmpty()) {
      LOG.info("No records to process.");
      return 0;
    }

    // Create a parallel collection from the working partition
    PCollection<StandardEvent> events = read(
        CrunchDatasets.asSource(eventsToProcess));

    // Group events by user and cookie id, then create a session for each group
    PCollection<Session> sessions = events
        .by(new GetSessionKey(), Avros.strings())
        .groupByKey()
        .parallelDo(new MakeSession(), Avros.specifics(Session.class));

    // Write the sessions to the "sessions" Dataset
    getPipeline().write(sessions,
        CrunchDatasets.asTarget("dataset:hive:/tmp/data/sessions"),
        Target.WriteMode.APPEND);

    return run().succeeded() ? 0 : 1;
  }

  private static boolean isView(String uri) {
    String scheme = URI.create(uri).getScheme();
    return "view".equals(scheme) || "dataset".equals(scheme);
  }

  private static class GetSessionKey extends MapFn<StandardEvent, String> {
    @Override
    public String map(StandardEvent event) {
      // Create a key from the session id and user id
      return event.getSessionId() + event.getUserId();
    }
  }

  private static class MakeSession
      extends DoFn<Pair<String, Iterable<StandardEvent>>, Session> {

    @Override
    public void process(
        Pair<String, Iterable<StandardEvent>> keyAndEvents,
        Emitter<Session> emitter) {
      final Iterator<StandardEvent> events = keyAndEvents.second().iterator();
      if (!events.hasNext()) {
        return;
      }

      // Initialize the values needed to create a session for this group
      final StandardEvent firstEvent = events.next();
      long startTime = firstEvent.getTimestamp();
      long endTime = firstEvent.getTimestamp();
      int numEvents = 1;

      // Inspect each event and keep track of start time, end time, and count
      while (events.hasNext()) {
        final StandardEvent event = events.next();
        startTime = Math.min(startTime, event.getTimestamp());
        endTime = Math.max(endTime, event.getTimestamp());
        numEvents += 1;
      }

      // Create a session. Use the first event for fields that do not change
      emitter.emit(Session.newBuilder()             // same on all events:
          .setUserId(firstEvent.getUserId())        // the user id (grouped by)
          .setSessionId(firstEvent.getSessionId())  // session id (grouped by)
          .setIp(firstEvent.getIp())                // the source IP address
          .setStartTimestamp(startTime)
          .setDuration(endTime - startTime)
          .setSessionEventCount(numEvents)
          .build());
    }
  }

  public static void main(String... args) throws Exception {
    int rc = ToolRunner.run(new CreateSessions(), args);
    System.exit(rc);
  }

}
