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

import com.google.common.collect.DiscreteDomains;
import com.google.common.collect.Ranges;
import java.util.Calendar;
import java.util.Random;
import java.util.TimeZone;
import org.apache.avro.generic.GenericRecordBuilder;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;
import org.kitesdk.data.Dataset;
import org.kitesdk.data.DatasetWriter;
import org.kitesdk.data.Datasets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.avro.generic.GenericData.Record;

public class GenerateSimpleLogs extends Configured implements Tool {

  private static final Logger LOG = LoggerFactory.getLogger(GenerateSimpleLogs.class);

  private static final long DAY_IN_MILLIS = 24 * 60 * 60 * 1000;

  public static final String[] LOG_LEVELS = new String[]
      {"DEBUG", "INFO", "WARN", "ERROR"};
  public static final String[] LOG_MESSAGES = new String[] {
      "Calling internalFunction!!!",
      "Still working",
      "You probably don't care, but...",
      "Users typed garbage into a field and something blew up"
    };

  @Override
  public int run(String[] args) throws Exception {
    // going to generate a lot of random log messages
    final Random rand = new Random();

    // data is written to the staging dataset
    Dataset<Record> staging = Datasets.<Record, Dataset<Record>>
        load("dataset:file:/tmp/data/logs_staging");

    // this is going to build our simple log records
    GenericRecordBuilder builder = new GenericRecordBuilder(
        staging.getDescriptor().getSchema());

    // generate timestamps 1 second apart starting 1 day ago
    final Calendar now = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
    final long yesterday = now.getTimeInMillis() - DAY_IN_MILLIS;

    DatasetWriter<Record> writer = null;
    try {
      writer = staging.newWriter();

      // generate 15,000 messages, each 5 seconds apart, starting 24 hours ago
      // this is a little less than 24 hours worth of messages
      for (int second : Ranges.closed(0, 15000).asSet(DiscreteDomains.integers())) {
        LOG.info("Generating log message " + second);

        builder.set("timestamp", yesterday + second * 5000);
        builder.set("component", "GenerateSimpleLogs");

        int level = rand.nextInt(LOG_LEVELS.length);
        builder.set("level", LOG_LEVELS[level]);
        builder.set("message", LOG_MESSAGES[level]);

        writer.write(builder.build());
      }

      writer.flush();
    } finally {
      if (writer != null) {
        writer.close();
      }
    }

    return 0;
  }

  public static void main(String... args) throws Exception {
    int rc = ToolRunner.run(new GenerateSimpleLogs(), args);
    System.exit(rc);
  }
}
