package org.kitesdk.examples.data;


import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;

import org.kitesdk.data.Dataset;
import org.kitesdk.data.DatasetDescriptor;
import org.kitesdk.data.DatasetReader;
import org.kitesdk.data.DatasetWriter;
import org.kitesdk.data.Datasets;

/**
 * Create a dataset then write and read from it.
 */
public class HelloKite extends Configured implements Tool {

  @Override
  public int run(String[] args) throws Exception {
    String datasetUri = "dataset:file:/tmp/hellos";

    // Create a dataset of Hellos
    DatasetDescriptor descriptor = new DatasetDescriptor.Builder()
        .schema(Hello.class).build();
    Dataset<Hello> hellos = Datasets.create(datasetUri, descriptor, Hello.class);

    // Write some Hellos in to the dataset
    DatasetWriter<Hello> writer = null;
    try {
      writer = hellos.newWriter();
      
      Hello hello = new Hello("Kite");
      writer.write(hello);

    } finally {
      if (writer != null) {
        writer.close();
      }
    }
    
    // Read the Hellos from the dataset
    DatasetReader<Hello> reader = null;
    try {
      reader = hellos.newReader();

      for (Hello hello : reader) {
        hello.sayHello();
      }

    } finally {
      if (reader != null) {
        reader.close();
      }
    }
    
    // Delete the dataset now that we are done with it
    Datasets.delete(datasetUri);

    return 0;
  }

  public static void main(String... args) throws Exception {
    int rc = ToolRunner.run(new HelloKite(), args);
    System.exit(rc);
  }
}

/** Simple Hello class */
class Hello {
  private String name;

  public Hello(String name) {
    this.name = name;
  }
	
  public Hello() {
    // Empty constructor for serialization purposes
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public void sayHello() {
    System.out.println("Hello, " + name + "!");
  }
}
