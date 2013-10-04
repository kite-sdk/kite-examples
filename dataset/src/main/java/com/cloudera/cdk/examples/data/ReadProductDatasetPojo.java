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
import com.cloudera.cdk.data.filesystem.FileSystemDatasetRepository;
import java.net.URI;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;

/**
 * Read all the {@link Product} objects from the products dataset.
 */
public class ReadProductDatasetPojo extends Configured implements Tool {

  @Override
  public int run(String[] args) throws Exception {

    // Construct a local filesystem dataset repository rooted at /tmp/data
    DatasetRepository repo = new FileSystemDatasetRepository.Builder()
        .rootDirectory(new URI("/tmp/data")).configuration(getConf()).get();

    // Load the products dataset
    Dataset products = repo.load("products");

    // Get a reader for the dataset and read all the products
    DatasetReader<Product> reader = products.getReader();
    try {
      reader.open();
      for (Product product : reader) {
        System.out.println(product);
      }
    } finally {
      reader.close();
    }

    return 0;
  }

  public static void main(String... args) throws Exception {
    int rc = ToolRunner.run(new ReadProductDatasetPojo(), args);
    System.exit(rc);
  }
}
