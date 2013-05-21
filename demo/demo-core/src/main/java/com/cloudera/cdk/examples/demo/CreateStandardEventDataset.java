package com.cloudera.cdk.examples.demo;

import com.cloudera.data.DatasetDescriptor;
import com.cloudera.data.DatasetRepository;
import com.cloudera.data.event.StandardEvent;
import com.cloudera.data.filesystem.FileSystemDatasetRepository;
import java.net.URI;
import org.apache.avro.Schema;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;

public class CreateStandardEventDataset extends Configured implements Tool {

  @Override
  public int run(String[] args) throws Exception {

    // Construct a local filesystem dataset repository rooted at /tmp/data
    DatasetRepository repo = new FileSystemDatasetRepository.Builder()
        .rootDirectory(new URI("/tmp/data")).get();

    // Get the Avro schema from the StandardEvent class
    Schema schema = StandardEvent.SCHEMA$;

    // Create a dataset of events with the Avro schema in the repository
    DatasetDescriptor descriptor = new DatasetDescriptor.Builder().schema(schema).get();
    repo.create("events", descriptor);

    return 0;
  }

  public static void main(String... args) throws Exception {
    int rc = ToolRunner.run(new CreateStandardEventDataset(), args);
    System.exit(rc);
  }
}
