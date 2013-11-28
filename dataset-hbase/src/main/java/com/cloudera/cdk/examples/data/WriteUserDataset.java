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

import com.cloudera.cdk.data.DatasetRepositories;
import com.cloudera.cdk.data.RandomAccessDataset;
import com.cloudera.cdk.data.RandomAccessDatasetRepository;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;

/**
 * Write some user objects to the users dataset using Avro specific records.
 */
public class WriteUserDataset extends Configured implements Tool {

  @Override
  public int run(String[] args) throws Exception {

    // Construct an HBase dataset repository using the local HBase database
    RandomAccessDatasetRepository repo =
        DatasetRepositories.openRandomAccess("repo:hbase:localhost.localdomain");

    // Load the users dataset
    RandomAccessDataset<User> users = repo.load("users");

    // Get an accessor for the dataset and write some users to it
    users.put(user("bill", "green"));
    users.put(user("alice", "blue"));
    users.put(user("cuthbert", "pink"));
    users.put(user("belinda", "yellow"));

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
    int rc = ToolRunner.run(new WriteUserDataset(), args);
    System.exit(rc);
  }
}
