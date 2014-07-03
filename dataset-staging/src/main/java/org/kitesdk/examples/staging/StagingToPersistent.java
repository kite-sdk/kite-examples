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

import java.io.Serializable;
import java.lang.System;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericRecord;
import org.apache.crunch.PCollection;
import org.apache.crunch.PipelineResult;
import org.apache.crunch.Target;
import org.apache.crunch.io.ReadableSource;
import org.apache.crunch.util.CrunchTool;
import org.apache.hadoop.util.ToolRunner;
import org.kitesdk.data.Dataset;
import org.kitesdk.data.Datasets;
import org.kitesdk.data.View;
import org.kitesdk.data.crunch.CrunchDatasets;

import static org.apache.avro.generic.GenericData.Record;

@SuppressWarnings("deprecation")
public class StagingToPersistent extends CrunchTool implements Serializable {
  public static final long DAY_IN_MILLIS = 24 * 60 * 60 * 1000;

  @Override
  public int run(String[] args) throws Exception {
    final long startOfToday = startOfDay(System.currentTimeMillis());

    // the destination dataset
    Dataset<Record> persistent = Datasets.<Record, Dataset<Record>>
        load("dataset:file:/tmp/data/logs");

    // the source: anything before today in the staging area
    Dataset<Record> staging = Datasets.<Record, Dataset<Record>>
        load("dataset:file:/tmp/data/logs_staging");
    View<Record> ready = staging.toBefore("timestamp", startOfToday);

    ReadableSource<Record> source = CrunchDatasets.asSource(ready, Record.class);

    PCollection<Record> stagedLogs = read(source);

    getPipeline().write(stagedLogs,
        CrunchDatasets.asTarget(persistent), Target.WriteMode.APPEND);

    PipelineResult result = run();

    if (result.succeeded()) {
      // remove the source data partition from staging
      ready.deleteAll();
      return 0;
    } else {
      return 1;
    }
  }

  private long startOfDay(long timestamp) {
    return (timestamp / DAY_IN_MILLIS) * DAY_IN_MILLIS;
  }

  public static void main(String... args) throws Exception {
    int rc = ToolRunner.run(new StagingToPersistent(), args);
    System.exit(rc);
  }
}
