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
import com.cloudera.cdk.data.DatasetRepository;
import com.cloudera.cdk.data.DatasetWriter;
import com.cloudera.cdk.data.filesystem.FileSystemDatasetRepository;
import com.google.common.collect.DiscreteDomains;
import com.google.common.collect.Ranges;
import java.util.Calendar;
import java.util.Random;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.generic.GenericRecordBuilder;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GenerateSimpleLogs extends Configured implements Tool {

  private static final Logger LOG = LoggerFactory.getLogger(GenerateSimpleLogs.class);

  public static final long DAY_IN_MILLIS = 24 * 60 * 60 * 1000;

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

    // open the repository
    final DatasetRepository repo = new FileSystemDatasetRepository.Builder()
        .rootDirectory(new Path("/tmp/data"))
        .get();

    // data is written to the staging dataset
    final Dataset staging = repo.load("logs-staging");
    final DatasetWriter<GenericRecord> writer = staging.newWriter();

    // this is going to build our simple log records
    final GenericRecordBuilder builder = new GenericRecordBuilder(
        staging.getDescriptor().getSchema());

    // generate timestamps 1 second apart starting... now
    final Calendar now = Calendar.getInstance();
    final long yesterday = now.getTimeInMillis() - DAY_IN_MILLIS;

    try {
      writer.open();

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
    } finally {
      writer.flush();
      writer.close();
    }

    return 0;
  }

  public static void main(String... args) throws Exception {
    int rc = ToolRunner.run(new GenerateSimpleLogs(), args);
    System.exit(rc);
  }
}
