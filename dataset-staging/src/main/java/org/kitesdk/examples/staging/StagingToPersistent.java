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

import org.kitesdk.data.Dataset;
import org.kitesdk.data.DatasetRepositories;
import org.kitesdk.data.DatasetRepository;
import org.kitesdk.data.DatasetWriter;
import org.kitesdk.data.PartitionKey;
import org.kitesdk.data.PartitionStrategy;
import org.kitesdk.data.crunch.CrunchDatasets;
import java.io.Serializable;
import java.util.Calendar;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.generic.GenericRecordBuilder;
import org.apache.crunch.DoFn;
import org.apache.crunch.Emitter;
import org.apache.crunch.PCollection;
import org.apache.crunch.PipelineResult;
import org.apache.crunch.Target;
import org.apache.crunch.io.ReadableSource;
import org.apache.crunch.types.avro.Avros;
import org.apache.crunch.util.CrunchTool;
import org.apache.hadoop.util.ToolRunner;

@SuppressWarnings("deprecation")
public class StagingToPersistent extends CrunchTool implements Serializable {
  public static final long DAY_IN_MILLIS = 24 * 60 * 60 * 1000;

  private static PartitionKey getPartitionKey(Dataset data, long timestamp) {
    // need to build a fake record to get a partition key
    final GenericRecordBuilder builder = new GenericRecordBuilder(
        data.getDescriptor().getSchema());
    builder.set("timestamp", timestamp);
    builder.set("level", "INFO");
    builder.set("component", "StagingToPersistent");
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
    final Dataset<GenericRecord> persistent = repo.load("logs");
    final DatasetWriter<GenericRecord> writer = persistent.newWriter();
    writer.open();

    // the source dataset: yesterday's partition in the staging area
    final Dataset<GenericRecord> staging = repo.load("logs_staging");
    final PartitionKey yesterday = getPartitionKey(staging, yesterdayTimestamp);
    System.out.println(yesterday);

    ReadableSource<GenericRecord> source = CrunchDatasets.asSource
        (staging.getPartition(yesterday, false), GenericRecord.class);

    PCollection<GenericRecord> logsStaging = read(source);
    PCollection<GenericData.Record> logs = logsStaging.parallelDo(
        new DoFn<GenericRecord, GenericData.Record>() {
      @Override
      public void process(GenericRecord genericRecord, Emitter<GenericData.Record>
          emitter) {
        emitter.emit((GenericData.Record) genericRecord);
      }
    }, Avros.generics(persistent.getDescriptor().getSchema()));

    getPipeline().write(logs, CrunchDatasets.asTarget(persistent), Target.WriteMode.APPEND);

    PipelineResult result = run();

    if (result.succeeded()) {
      // remove the source data partition from staging
      staging.dropPartition(yesterday);
      return 0;
    } else {
      return 1;
    }
  }

  public static void main(String... args) throws Exception {
    int rc = ToolRunner.run(new StagingToPersistent(), args);
    System.exit(rc);
  }
}
