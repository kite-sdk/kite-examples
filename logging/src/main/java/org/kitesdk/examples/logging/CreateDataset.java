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
package org.kitesdk.examples.logging;

import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;
import org.kitesdk.data.DatasetDescriptor;
import org.kitesdk.data.Datasets;

/**
 * Create a dataset on the local filesystem.
 */
public class CreateDataset extends Configured implements Tool {

  @Override
  public int run(String[] args) throws Exception {

    // Create a dataset of events with the Avro schema
    DatasetDescriptor descriptor = new DatasetDescriptor.Builder()
        .schemaUri("resource:event.avsc")
        .build();
    Datasets.create("dataset:hdfs:/tmp/data/events", descriptor);

    return 0;
  }

  public static void main(String... args) throws Exception {
    int rc = ToolRunner.run(new CreateDataset(), args);
    System.exit(rc);
  }
}
