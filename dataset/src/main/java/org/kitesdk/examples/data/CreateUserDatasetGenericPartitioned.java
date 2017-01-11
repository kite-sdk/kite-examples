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
package org.kitesdk.examples.data;

import java.util.Random;
import org.apache.avro.generic.GenericRecordBuilder;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;
import org.kitesdk.data.Dataset;
import org.kitesdk.data.DatasetDescriptor;
import org.kitesdk.data.DatasetWriter;
import org.kitesdk.data.Datasets;
import org.kitesdk.data.PartitionStrategy;

import static org.apache.avro.generic.GenericData.Record;

/**
 * Create a dataset on the local filesystem and write some user objects to it,
 * using Avro generic records.
 */
public class CreateUserDatasetGenericPartitioned extends Configured implements Tool {
  private static final String[] colors = { "green", "blue", "pink", "brown", "yellow" };

  @Override
  public int run(String[] args) throws Exception {
    // Create a partition strategy that hash partitions on username with 10 buckets
    PartitionStrategy partitionStrategy = new PartitionStrategy.Builder()
        .identity("favoriteColor", "favorite_color")
        .build();

    // Create a dataset of users with the Avro schema
    DatasetDescriptor descriptor = new DatasetDescriptor.Builder()
        .schemaUri("resource:user.avsc")
        .partitionStrategy(partitionStrategy)
        .build();
    Dataset<Record> users = Datasets.create(
        "dataset:hdfs:/tmp/data/users", descriptor, Record.class);

    // Get a writer for the dataset and write some users to it
    DatasetWriter<Record> writer = null;
    try {
      writer = users.newWriter();
      Random rand = new Random();
      GenericRecordBuilder builder = new GenericRecordBuilder(descriptor.getSchema());
      for (int i = 0; i < 100; i++) {
        Record record = builder.set("username", "user-" + i)
            .set("creationDate", System.currentTimeMillis())
            .set("favoriteColor", colors[rand.nextInt(colors.length)]).build();
        writer.write(record);
      }
    } finally {
      if (writer != null) {
        writer.close();
      }
    }

    return 0;
  }

  public static void main(String... args) throws Exception {
    int rc = ToolRunner.run(new CreateUserDatasetGenericPartitioned(), args);
    System.exit(rc);
  }
}
