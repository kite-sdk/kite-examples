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

import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;
import org.kitesdk.data.Dataset;
import org.kitesdk.data.DatasetDescriptor;
import org.kitesdk.data.DatasetWriter;
import org.kitesdk.data.Datasets;

/**
 * Create a dataset on the local filesystem and write some {@link Product} objects to it.
 */
public class CreateProductDatasetPojo extends Configured implements Tool {
  private static final String[] names = { "toaster", "teapot", "butter dish" };

  @Override
  public int run(String[] args) throws Exception {

    // Create a dataset of products with the Avro schema in the repository
    DatasetDescriptor descriptor = new DatasetDescriptor.Builder()
        .schema(Product.class)
        .build();
    Dataset<Product> products = Datasets.create(
        "dataset:hdfs:/tmp/data/products", descriptor, Product.class);

    // Get a writer for the dataset and write some products to it
    DatasetWriter<Product> writer = null;
    try {
      writer = products.newWriter();
      int i = 0;
      for (String name : names) {
        Product product = new Product();
        product.setName(name);
        product.setId(i++);
        writer.write(product);
      }
    } finally {
      if (writer != null) {
        writer.close();
      }
    }

    return 0;
  }

  public static void main(String... args) throws Exception {
    int rc = ToolRunner.run(new CreateProductDatasetPojo(), args);
    System.exit(rc);
  }
}
