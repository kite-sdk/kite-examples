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
package org.kitesdk.examples.data;

import org.kitesdk.data.DatasetReader;
import org.kitesdk.data.DatasetRepositories;
import org.kitesdk.data.Key;
import org.kitesdk.data.RandomAccessDataset;
import org.kitesdk.data.RandomAccessDatasetRepository;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;

/**
 * Read the user objects from the users dataset by key lookup, and by scanning.
 */
public class ReadUserDataset extends Configured implements Tool {

  @Override
  public int run(String[] args) throws Exception {

    // Construct an HBase dataset repository using the local HBase database
    RandomAccessDatasetRepository repo =
        DatasetRepositories.openRandomAccess("repo:hbase:localhost.localdomain");

    // Load the users dataset
    // Dataset is named [table].[entity]
    RandomAccessDataset<User> users = repo.load("users.User");

    // Get an accessor for the dataset and look up a user by username
    Key key = new Key.Builder(users).add("username", "bill").build();
    System.out.println(users.get(key));
    System.out.println("----");

    // Get a reader for the dataset and read the users from "bill" onwards
    DatasetReader<User> reader = users.from("username", "bill").newReader();
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
