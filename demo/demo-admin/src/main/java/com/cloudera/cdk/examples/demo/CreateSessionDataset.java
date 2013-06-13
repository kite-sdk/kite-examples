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

import com.cloudera.cdk.examples.demo.event.Session;
import com.cloudera.data.DatasetDescriptor;
import com.cloudera.data.DatasetRepository;
import com.cloudera.data.hcatalog.HCatalogDatasetRepository;
import java.net.URI;
import org.apache.avro.Schema;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;

public class CreateSessionDataset extends Configured implements Tool {

  @Override
  public int run(String[] args) throws Exception {

    // Construct an HCatalog dataset repository using external Hive tables
    DatasetRepository repo = new HCatalogDatasetRepository.Builder()
        .rootDirectory(new URI("/tmp/data")).configuration(getConf()).get();

    // Get the Avro schema from the Session class
    Schema schema = Session.SCHEMA$;

    // Create a dataset of events with the Avro schema in the repository
    DatasetDescriptor descriptor = new DatasetDescriptor.Builder().schema(schema).get();
    repo.create("sessions", descriptor);

    return 0;
  }

  public static void main(String... args) throws Exception {
    int rc = ToolRunner.run(new CreateSessionDataset(), args);
    System.exit(rc);
  }
}
