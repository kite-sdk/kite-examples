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
package com.cloudera.cdk.examples.logging;

import com.cloudera.data.filesystem.FileSystemDatasetRepository;
import java.net.URI;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.generic.GenericRecordBuilder;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;
import org.apache.log4j.Logger;

/**
 * A simple application to log events to a dataset.
 */
public class App extends Configured implements Tool {

  @Override
  public int run(String[] args) throws Exception {

    // Get a log4j logger
    Logger logger = Logger.getLogger(App.class);

    // Find the schema from the repository
    FileSystemDatasetRepository repo = new FileSystemDatasetRepository.Builder()
        .rootDirectory(new URI("/tmp/data")).configuration(getConf()).get();
    Schema schema = repo.getMetadataProvider().load("events").getSchema();

    // Build some events using the generic Avro API and log them using log4j
    GenericRecordBuilder builder = new GenericRecordBuilder(schema);
    for (long i = 0; i < 10; i++) {
      GenericRecord event = builder.set("id", i)
          .set("message", "Hello " + i).build();
      System.out.println("Sending to log4j: " + event);
      logger.info(event);
    }

    return 0;
  }

  public static void main(String... args) throws Exception {
    int rc = ToolRunner.run(new App(), args);
    System.exit(rc);
  }
}
