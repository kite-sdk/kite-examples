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
package com.cloudera.cdk.examples.demo;

import com.cloudera.cdk.data.DatasetDescriptor;
import com.cloudera.cdk.data.DatasetRepository;
import com.cloudera.cdk.data.PartitionStrategy;
import com.cloudera.cdk.data.event.StandardEvent;
import com.cloudera.cdk.data.filesystem.FileSystemDatasetRepository;
import com.cloudera.cdk.data.partition.DayOfMonthFieldPartitioner;
import com.cloudera.cdk.data.partition.HourFieldPartitioner;
import com.cloudera.cdk.data.partition.MinuteFieldPartitioner;
import com.cloudera.cdk.data.partition.MonthFieldPartitioner;
import com.cloudera.cdk.data.partition.YearFieldPartitioner;
import java.net.URI;
import org.apache.avro.Schema;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;

public class CreateStandardEventDataset extends Configured implements Tool {

  @Override
  public int run(String[] args) throws Exception {

    // Construct a local filesystem dataset repository rooted at /tmp/data
    DatasetRepository repo = new FileSystemDatasetRepository.Builder()
        .rootDirectory(new URI("/tmp/data")).configuration(getConf()).get();

    // Get the Avro schema from the StandardEvent class
    Schema schema = StandardEvent.SCHEMA$;

    // Create a dataset of events with the Avro schema in the repository,
    // partitioned by time
    PartitionStrategy partitionStrategy = new PartitionStrategy(
        new YearFieldPartitioner("timestamp", "year"),
        new MonthFieldPartitioner("timestamp", "month"),
        new DayOfMonthFieldPartitioner("timestamp", "day"),
        new HourFieldPartitioner("timestamp", "hour"),
        new MinuteFieldPartitioner("timestamp", "minute")
    );
    DatasetDescriptor descriptor = new DatasetDescriptor.Builder()
        .schema(schema)
        .partitionStrategy(partitionStrategy)
        .get();
    repo.create("events", descriptor);

    return 0;
  }

  public static void main(String... args) throws Exception {
    int rc = ToolRunner.run(new CreateStandardEventDataset(), args);
    System.exit(rc);
  }
}
