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
package com.cloudera.cdk.examples.data;

import com.cloudera.cdk.data.Dataset;
import com.cloudera.cdk.data.DatasetAccessor;
import com.cloudera.cdk.data.DatasetReader;
import com.cloudera.cdk.data.PartitionKey;
import com.cloudera.cdk.data.hbase.HBaseDatasetRepository;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;

/**
 * Read the user objects from the users dataset by key lookup, and by scanning.
 */
public class ReadUserDataset extends Configured implements Tool {

  @Override
  public int run(String[] args) throws Exception {

    // Construct an HBase dataset repository using the local HBase database
    HBaseDatasetRepository repo = new HBaseDatasetRepository.Builder()
        .configuration(HBaseConfiguration.create()).get();

    // Load the users dataset
    Dataset users = repo.load("users");

    // Get an accessor for the dataset and look up a user by username
    DatasetAccessor<User> accessor = users.newAccessor();
    PartitionKey key = users.getDescriptor().getPartitionStrategy().partitionKey("bill");
    System.out.println(accessor.get(key));

    // Get a reader for the dataset and read all the users
    DatasetReader<User> reader = users.newReader();
    try {
      reader.open();
      for (User user : reader) {
        System.out.println(user);
      }
    } finally {
      reader.close();
    }

    return 0;
  }

  public static void main(String... args) throws Exception {
    int rc = ToolRunner.run(new ReadUserDataset(), args);
    System.exit(rc);
  }
}
