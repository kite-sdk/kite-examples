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
import org.kitesdk.data.DatasetReader;
import org.kitesdk.data.Datasets;

/**
 * Read all the {@link Product} objects from the products dataset.
 */
public class ReadProductDatasetPojo extends Configured implements Tool {

  @Override
  public int run(String[] args) throws Exception {
    // Load the products dataset
    Dataset<Product> products = Datasets.load(
        "dataset:hdfs:/tmp/data/products", Product.class);

    // Get a reader for the dataset and read all the users
    DatasetReader<Product> reader = null;
    try {
      reader = products.newReader();
      for (Product product : reader) {
        System.out.println(product);
      }

    } finally {
      if (reader != null) {
        reader.close();
      }
    }

    return 0;
  }

  public static void main(String... args) throws Exception {
    int rc = ToolRunner.run(new ReadProductDatasetPojo(), args);
    System.exit(rc);
  }
}
