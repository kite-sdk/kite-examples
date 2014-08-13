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

import com.google.common.base.Preconditions;
import java.util.List;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.util.ToolRunner;
import org.kitesdk.data.DatasetDescriptor;
import org.kitesdk.data.Datasets;
import org.kitesdk.data.event.CorrelatedEvents;


public class CorrelateEvents extends BaseEventsTool {

  String master = "localhost.localdomain";

  @Override
  public int run(List<String> args) throws Exception {

    String inputUri = uri;
    String outputUri = "dataset:hive?dataset=correlated_events";

    if (args.size() > 0) {
      if ("--master".equals(args.get(0))) {
        if (args.size() >= 2) {
          master = args.get(1);
        } else {
          System.err.println("Error: --master requires an argument");
          System.err.println("Usage: correlate [--master <spark master>] [output-dataset-uri]");
          return -1;
        }
      } else {
        outputUri = args.get(0);
      }
    }

    Preconditions.checkState(Datasets.exists(inputUri),
        "input dataset doesn't exists");

    if (!Datasets.exists(outputUri)) {
      Datasets.create(outputUri, new DatasetDescriptor.Builder()
          .format("avro")
          .schema(CorrelatedEvents.class)
          .build());
    }
    CorrelateEventsTask task = new CorrelateEventsTask(inputUri, master,
        outputUri);
    task.run();

    return 0;
  }

  public static void main(String[] args) throws Exception {
    int rc = ToolRunner.run(new Configuration(), new CorrelateEvents(), args);

    System.exit(rc);
  }
}
