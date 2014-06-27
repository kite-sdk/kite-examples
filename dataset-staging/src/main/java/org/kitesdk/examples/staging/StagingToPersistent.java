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

import java.util.TimeZone;
import org.kitesdk.data.Dataset;
import org.kitesdk.data.DatasetRepositories;
import org.kitesdk.data.DatasetRepository;
import org.kitesdk.data.View;
import org.kitesdk.data.crunch.CrunchDatasets;
import java.io.Serializable;
import java.util.Calendar;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericRecord;
import org.apache.crunch.DoFn;
import org.apache.crunch.Emitter;
import org.apache.crunch.PCollection;
import org.apache.crunch.PipelineResult;
import org.apache.crunch.Target;
import org.apache.crunch.io.ReadableSource;
import org.apache.crunch.types.avro.Avros;
import org.apache.crunch.util.CrunchTool;
import org.apache.hadoop.util.ToolRunner;

public class StagingToPersistent extends CrunchTool implements Serializable {

  @Override
  public int run(String[] args) throws Exception {
    // open the repository
    DatasetRepository repo = DatasetRepositories.open("repo:file:/tmp/data");

    final Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
    cal.set(Calendar.HOUR_OF_DAY, 0);
    cal.set(Calendar.MINUTE, 0);
    cal.set(Calendar.SECOND, 0);
    cal.set(Calendar.MILLISECOND, 0);
    long midnight = cal.getTimeInMillis();
    cal.add(Calendar.DATE, -1);
    long yesterdayMidnight = cal.getTimeInMillis();

    // the destination dataset
    Dataset<GenericRecord> persistent = repo.load("logs");

    // the source dataset: yesterday's partition in the staging area
    Dataset<GenericRecord> staging = repo.load("logs_staging");
    View<GenericRecord> yesterday = staging.from("timestamp", yesterdayMidnight)
        .toBefore("timestamp", midnight);

    ReadableSource<GenericRecord> source = CrunchDatasets.asSource(yesterday, GenericRecord.class);

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
      return yesterday.deleteAll() ? 0 : 1;
    } else {
      return 1;
    }
  }

  public static void main(String... args) throws Exception {
    int rc = ToolRunner.run(new StagingToPersistent(), args);
    System.exit(rc);
  }
}
