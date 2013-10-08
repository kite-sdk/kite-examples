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
import com.cloudera.cdk.data.DatasetDescriptor;
import com.cloudera.cdk.data.PartitionStrategy;
import com.cloudera.cdk.data.hbase.HBaseDatasetRepository;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;

/**
 * Create a dataset in HBase and write some user objects to it,
 * using Avro specific records.
 */
public class CreateUserDataset extends Configured implements Tool {

  @Override
  public int run(String[] args) throws Exception {

    // Construct an HBase dataset repository using the local HBase database
    HBaseDatasetRepository repo = new HBaseDatasetRepository.Builder()
        .configuration(HBaseConfiguration.create()).get();

    // Create a dataset of users with the Avro schema of User in the repository.
    // The key for the entities is defined as the username by means of the partition
    // strategy.
    DatasetDescriptor descriptor = new DatasetDescriptor.Builder()
        .schema(User.getClassSchema())
        .partitionStrategy(new PartitionStrategy.Builder().identity("username", 1).get())
        .get();
    Dataset users = repo.create("users", descriptor);

    // Get an accessor for the dataset and write some users to it
    DatasetAccessor<User> accessor = users.newAccessor();
    accessor.put(user("bill", "green"));
    accessor.put(user("alice", "blue"));
    accessor.put(user("cuthbert", "pink"));
    accessor.put(user("belinda", "yellow"));

    return 0;
  }

  private static User user(String username, String favoriteColor) {
    return User.newBuilder()
        .setUsername(username)
        .setFavoriteColor(favoriteColor)
        .setCreationDate(System.currentTimeMillis())
        .build();
  }

  public static void main(String... args) throws Exception {
    int rc = ToolRunner.run(new CreateUserDataset(), args);
    System.exit(rc);
  }
}
