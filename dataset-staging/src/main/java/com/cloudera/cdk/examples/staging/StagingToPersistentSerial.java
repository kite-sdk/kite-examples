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
package com.cloudera.cdk.examples.staging;

import com.cloudera.cdk.data.Dataset;
import com.cloudera.cdk.data.DatasetReader;
import com.cloudera.cdk.data.DatasetRepositories;
import com.cloudera.cdk.data.DatasetRepository;
import com.cloudera.cdk.data.DatasetWriter;
import com.cloudera.cdk.data.PartitionKey;
import com.cloudera.cdk.data.PartitionStrategy;
import java.util.Calendar;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.generic.GenericRecordBuilder;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StagingToPersistentSerial extends Configured implements Tool {

  private static final Logger LOG = LoggerFactory.getLogger(StagingToPersistentSerial.class);

  public static final long DAY_IN_MILLIS = 24 * 60 * 60 * 1000;

  private static PartitionKey getPartitionKey(Dataset data, long timestamp) {
    // need to build a fake record to get a partition key
    final GenericRecordBuilder builder = new GenericRecordBuilder(
        data.getDescriptor().getSchema());
    builder.set("timestamp", timestamp);
    builder.set("level", "INFO");
    builder.set("component", "StagingToPersistentSerial");
    builder.set("message", "Fake log message");

    // access the partition strategy, which produces keys from records
    final PartitionStrategy partitioner = data.getDescriptor()
        .getPartitionStrategy();

    return partitioner.partitionKeyForEntity(builder.build());
  }

  @Override
  public int run(String[] args) throws Exception {
    // open the repository
    final DatasetRepository repo = DatasetRepositories.open("repo:file:/tmp/data");

    final Calendar now = Calendar.getInstance();
    final long yesterdayTimestamp = now.getTimeInMillis() - DAY_IN_MILLIS;

    // the destination dataset
    final Dataset persistent = repo.load("logs");
    final DatasetWriter<GenericRecord> writer = persistent.getWriter();
    writer.open();

    // the source dataset: yesterday's partition in the staging area
    final Dataset staging = repo.load("logs-staging");
    final PartitionKey yesterday = getPartitionKey(staging, yesterdayTimestamp);
    final DatasetReader<GenericRecord> reader = staging
        .getPartition(yesterday, false).getReader();

    try {
      reader.open();

      // yep, it's that easy.
      for (GenericRecord record : reader) {
        writer.write(record);
      }

    } finally {
      reader.close();
      writer.flush();
    }

    // remove the source data partition from staging
    staging.dropPartition(yesterday);

    // if the above didn't throw an exception, commit the data
    writer.close();

    return 0;
  }

  public static void main(String... args) throws Exception {
    int rc = ToolRunner.run(new StagingToPersistentSerial(), args);
    System.exit(rc);
  }
}
