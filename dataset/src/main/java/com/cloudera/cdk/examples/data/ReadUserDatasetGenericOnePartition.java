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
import com.cloudera.cdk.data.DatasetReader;
import com.cloudera.cdk.data.DatasetRepository;
import com.cloudera.cdk.data.PartitionKey;
import com.cloudera.cdk.data.PartitionStrategy;
import com.cloudera.cdk.data.filesystem.FileSystemDatasetRepository;
import java.net.URI;
import org.apache.avro.generic.GenericRecord;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;

/**
 * Read one partition of user objects from the users dataset using Avro generic records.
 */
public class ReadUserDatasetGenericOnePartition extends Configured implements Tool {

  @Override
  public int run(String[] args) throws Exception {

    // Construct a local filesystem dataset repository rooted at /tmp/data
    DatasetRepository repo = new FileSystemDatasetRepository.Builder()
        .rootDirectory(new URI("/tmp/data")).configuration(getConf()).get();

    // Get the users dataset
    Dataset users = repo.get("users");

    // Get the partition strategy and use it to construct a partition key for
    // hash(username)=0
    PartitionStrategy partitionStrategy = users.getDescriptor().getPartitionStrategy();
    PartitionKey partitionKey = partitionStrategy.partitionKey(0);

    // Get the dataset partition for the partition key
    Dataset partition = users.getPartition(partitionKey, false);

    // Get a reader for the partition and read all the users
    DatasetReader<GenericRecord> reader = partition.getReader();
    try {
      reader.open();
      for (GenericRecord user : reader) {
        System.out.println(user);
      }
    } finally {
      reader.close();
    }

    return 0;
  }

  public static void main(String... args) throws Exception {
    int rc = ToolRunner.run(new ReadUserDatasetGenericOnePartition(), args);
    System.exit(rc);
  }
}
