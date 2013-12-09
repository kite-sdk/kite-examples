package org.kitesdk.examples.data;


import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;

import org.kitesdk.data.Dataset;
import org.kitesdk.data.DatasetDescriptor;
import org.kitesdk.data.DatasetReader;
import org.kitesdk.data.DatasetRepositories;
import org.kitesdk.data.DatasetRepository;
import org.kitesdk.data.DatasetWriter;

/**
 * Create a dataset then write and read from it.
 */
public class HelloCDK extends Configured implements Tool {

  @Override
  public int run(String[] args) throws Exception {

    // Construct a local filesystem dataset repository rooted at /tmp/hello-cdk
    DatasetRepository repo = DatasetRepositories.open("repo:file:/tmp/hello-cdk");

    // Create a dataset of Hellos
    DatasetDescriptor descriptor = new DatasetDescriptor.Builder()
        .schema(Hello.class).build();
    Dataset<Hello> hellos = repo.create("hellos", descriptor);

    // Write some Hellos in to the dataset
    DatasetWriter<Hello> writer = hellos.newWriter();
    try {
      writer.open();
      
      Hello cdk = new Hello("CDK");
      writer.write(cdk);
    } finally {
      writer.close();
    }
    
    // Read the Hellos from the dataset
    DatasetReader<Hello> reader = hellos.newReader();
    try {
      reader.open();
      for (Hello hello : reader) {
        hello.sayHello();
      }
    } finally {
      reader.close();
    }
    
    // Delete the dataset now that we are done with it
    repo.delete("hellos");

    return 0;
  }

  public static void main(String... args) throws Exception {
    int rc = ToolRunner.run(new HelloCDK(), args);
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
