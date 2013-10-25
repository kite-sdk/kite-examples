package com.cloudera.cdk.examples.data;


import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;

import com.cloudera.cdk.data.Dataset;
import com.cloudera.cdk.data.DatasetDescriptor;
import com.cloudera.cdk.data.DatasetReader;
import com.cloudera.cdk.data.DatasetRepositories;
import com.cloudera.cdk.data.DatasetRepository;
import com.cloudera.cdk.data.DatasetWriter;

/**
 * Create a dataset then write and read from it.
 */
public class HelloCDK extends Configured implements Tool {

  @Override
  public int run(String[] args) throws Exception {

    // Construct a local filesystem dataset repository rooted at /tmp/hellocdk
    DatasetRepository repo = DatasetRepositories.open("repo:file:/tmp/hello-cdk");

    // Create a dataset of Hellos
    DatasetDescriptor descriptor = new DatasetDescriptor.Builder().schema(Hello.class).get();
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
