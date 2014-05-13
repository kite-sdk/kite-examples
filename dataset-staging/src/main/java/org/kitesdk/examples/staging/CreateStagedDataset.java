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
package org.kitesdk.examples.staging;

import java.net.URI;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;
import org.kitesdk.data.DatasetDescriptor;
import org.kitesdk.data.Datasets;
import org.kitesdk.data.Formats;
import org.kitesdk.data.PartitionStrategy;

public class CreateStagedDataset extends Configured implements Tool {

  @Override
  public int run(String[] args) throws Exception {
    // where the schema is stored
    URI schemaURI = URI.create("resource:simple-log.avsc");

    // create a Parquet dataset for long-term storage
    Datasets.create("dataset:file:/tmp/data/logs", new DatasetDescriptor.Builder()
        .format(Formats.PARQUET)
        .schemaUri(schemaURI)
        .partitionStrategy(new PartitionStrategy.Builder()
            .year("timestamp", "year")
            .month("timestamp", "month")
            .day("timestamp", "day")
            .build())
        .build());

    // create an Avro dataset to temporarily hold data
    Datasets.create("dataset:file:/tmp/data/logs_staging", new DatasetDescriptor.Builder()
        .format(Formats.AVRO)
        .schemaUri(schemaURI)
        .partitionStrategy(new PartitionStrategy.Builder()
            .day("timestamp", "day")
            .build())
        .build());

    return 0;
  }

  public static void main(String... args) throws Exception {
    int rc = ToolRunner.run(new CreateStagedDataset(), args);
    System.exit(rc);
  }
}
