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
import com.cloudera.cdk.data.DatasetDescriptor;
import com.cloudera.cdk.data.DatasetRepositories;
import com.cloudera.cdk.data.DatasetRepository;
import com.cloudera.cdk.data.DatasetWriter;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;

/**
 * Create a dataset on the local filesystem and write some {@link Product} objects to it.
 */
public class CreateProductDatasetPojo extends Configured implements Tool {

  @Override
  public int run(String[] args) throws Exception {

    // Construct a filesystem dataset repository rooted at /tmp/data
    DatasetRepository repo = DatasetRepositories.open("repo:file:/tmp/data");

    // Create a dataset of products with the Avro schema in the repository
    DatasetDescriptor descriptor = new DatasetDescriptor.Builder().schema(Product.class).get();
    Dataset products = repo.create("products", descriptor);

    // Get a writer for the dataset and write some products to it
    DatasetWriter<Product> writer = products.getWriter();
    try {
      writer.open();
      String[] names = { "toaster", "teapot", "butter dish" };
      int i = 0;
      for (String name : names) {
        Product product = new Product();
        product.setName(name);
        product.setId(i++);
        writer.write(product);
      }
    } finally {
      writer.close();
    }

    return 0;
  }

  public static void main(String... args) throws Exception {
    int rc = ToolRunner.run(new CreateProductDatasetPojo(), args);
    System.exit(rc);
  }
}
