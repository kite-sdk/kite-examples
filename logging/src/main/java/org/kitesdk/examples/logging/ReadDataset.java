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

import org.kitesdk.data.Dataset;
import org.kitesdk.data.DatasetReader;
import org.apache.avro.generic.GenericRecord;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;
import org.kitesdk.data.Datasets;

/**
 * Read all the event objects from the events dataset using Avro generic records.
 */
public class ReadDataset extends Configured implements Tool {

  @Override
  public int run(String[] args) throws Exception {

    // Load the events dataset
    Dataset<GenericRecord> events = Datasets.load("dataset:hdfs:/tmp/data/default/events");

    // Get a reader for the dataset and read all the events
    DatasetReader<GenericRecord> reader = events.newReader();
    try {
      for (GenericRecord event : reader) {
        System.out.println(event);
      }
    } finally {
      reader.close();
    }

    return 0;
  }

  public static void main(String... args) throws Exception {
    int rc = ToolRunner.run(new ReadDataset(), args);
    System.exit(rc);
  }
}
