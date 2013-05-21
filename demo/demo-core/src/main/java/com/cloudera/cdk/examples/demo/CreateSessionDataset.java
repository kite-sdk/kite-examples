package com.cloudera.cdk.examples.demo;

import com.cloudera.cdk.examples.demo.event.Session;
import com.cloudera.data.DatasetDescriptor;
import com.cloudera.data.DatasetRepository;
import com.cloudera.data.hcatalog.HCatalogDatasetRepository;
import org.apache.avro.Schema;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;

public class CreateSessionDataset extends Configured implements Tool {

  @Override
  public int run(String[] args) throws Exception {

    // Construct an HCatalog dataset repository using external Hive tables
    DatasetRepository repo = new HCatalogDatasetRepository(
        FileSystem.get(new Configuration()), new Path("/tmp/data"));

    // Get the Avro schema from the Session class
    Schema schema = Session.SCHEMA$;

    // Create a dataset of events with the Avro schema in the repository
    DatasetDescriptor descriptor = new DatasetDescriptor.Builder().schema(schema).get();
    repo.create("sessions", descriptor);

    return 0;
  }

  public static void main(String... args) throws Exception {
    int rc = ToolRunner.run(new CreateSessionDataset(), args);
    System.exit(rc);
  }
}
