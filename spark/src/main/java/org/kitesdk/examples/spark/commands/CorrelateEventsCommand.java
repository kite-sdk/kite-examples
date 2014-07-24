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

package org.kitesdk.examples.spark.commands;

import com.beust.jcommander.Parameter;
import com.google.common.base.Preconditions;
import java.io.IOException;
import java.util.List;
import org.kitesdk.data.DatasetDescriptor;
import org.kitesdk.data.Datasets;
import org.kitesdk.data.event.CorrelatedEvents;
import org.kitesdk.examples.spark.CorrelateEventsTask;
import org.slf4j.Logger;


public class CorrelateEventsCommand extends BaseEventsCommand {

  @Parameter(names = {"--master"},
      description = "Spark master (defaults to local)")
  String master = "local";

  public CorrelateEventsCommand(Logger console) {
    super(console);
  }

  @Override
  public int run() throws IOException {
    String inputUri = createUri();
    String outputUri = createUri("correlated_events");

    Preconditions.checkState(Datasets.exists(inputUri),
        "events dataset doesn't exists");

    if (!Datasets.exists(outputUri)) {
      Datasets.create(outputUri, new DatasetDescriptor.Builder()
          .format("avro")
          .schema(CorrelatedEvents.class)
          .build());
    }
    CorrelateEventsTask task = new CorrelateEventsTask(inputUri, master,
        outputUri, console);
    task.run();

    return 0;
  }

  @Override
  public List<String> getExamples() {
    throw new UnsupportedOperationException("Not supported yet.");
  }

}
