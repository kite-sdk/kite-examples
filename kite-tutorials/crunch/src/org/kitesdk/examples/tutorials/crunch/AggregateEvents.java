/**
 * Copyright 2015 Cloudera Inc.
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
package org.kitesdk.examples.tutorials.crunch;

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
import org.kitesdk.data.crunch.CrunchDatasets;
import org.kitesdk.data.event.StandardEvent;
import org.kitesdk.data.spi.filesystem.FileSystemDatasets;
import org.kitesdk.examples.demo.event.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AggregateEvents extends CrunchTool implements Serializable {

  private static final Logger LOG = LoggerFactory.getLogger(AggregateEvents.class);

  @Override
  public int run(String[] args) throws Exception {
	  
    // Turn debug on while in development.
    getPipeline().enableDebug();
    getPipeline().getConfiguration().set("crunch.log.job.progress", "true");
    
    // Step 1. Load the dataset into the Pipeline.
    Dataset<StandardEvent> eventsDataset = Datasets.load(
        "dataset:hive:events", StandardEvent.class);

	// If the dataset is empty, stop.
    if (eventsDataset.isEmpty()) {
      LOG.info("No records to process.");
      return 0;
    }

    // Create a parallel collection from the Kite CrunchDatasets
    // Source "events" dataset.
    PCollection<StandardEvent> events = read(
        CrunchDatasets.asSource(eventsDataset));

    /* Step 2. Process the data.
     * a. Create a session key by combining the user ID and session ID.
     * b. Group together all events with the same session key.
     * c. For each group, create a Session record as an Avro Specific object. 
     */
    PCollection<Session> sessions = events
        .by(new GetSessionKey(), Avros.strings())
        .groupByKey()
        .parallelDo(new MakeSession(), Avros.specifics(Session.class));

    // Step 3. Append the derived sessions to the Kite CrunchDatasets
    // Target "sessions" dataset.
    getPipeline().write(sessions,
        CrunchDatasets.asTarget("dataset:hive:sessions"),
        Target.WriteMode.APPEND);

    return run().succeeded() ? 0 : 1;
  }

  private static class GetSessionKey extends MapFn<StandardEvent, String> {
    @Override
    public String map(StandardEvent event) {
      // Create a key by combining the session id and user id
      return event.getSessionId() + event.getUserId();
    }
  }

  private static class MakeSession
      extends DoFn<Pair<String, Iterable<StandardEvent>>, Session> {

    // The process method iterates through a group of events that have
    // the same sessionKey.
    @Override
    public void process(
        Pair<String, Iterable<StandardEvent>> keyAndEvents,
        Emitter<Session> emitter) {
      final Iterator<StandardEvent> events = keyAndEvents.second().iterator();
      if (!events.hasNext()) {
        return;
      }

      // Initialize the values needed to create a session object for
      // this group.
      final StandardEvent firstEvent = events.next();
      long startTime = firstEvent.getTimestamp();
      long endTime = firstEvent.getTimestamp();
      int numEvents = 1;

      // Inspect each event in this session group. Track the earliest 
      // timestamp (start time) and latest timestamp (end time). Keep a
      // count of the events in this session group.
      while (events.hasNext()) {
        final StandardEvent event = events.next();
        
        // Reset the start time if the timestamp is earlier than the
        // current start time value.
        startTime = Math.min(startTime, event.getTimestamp());
        
        // Reset the end time if the timestamp is later then the current
        // end time value.
        endTime = Math.max(endTime, event.getTimestamp());
        
        // Keep a count of the events.
        numEvents += 1;
      }

      // Create a session. Use values from the first event in the group
      // for fields that don't change.
      emitter.emit(Session.newBuilder()             
          .setUserId(firstEvent.getUserId())        
          .setSessionId(firstEvent.getSessionId())  
          .setIp(firstEvent.getIp())                
          .setStartTimestamp(startTime)
          .setDuration(endTime - startTime)
          .setSessionEventCount(numEvents)
          .build());
    }
  }

  public static void main(String... args) throws Exception {
    int rc = ToolRunner.run(new AggregateEvents(), args);
    System.exit(rc);
  }

}