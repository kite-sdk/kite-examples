package com.cloudera.cdk.examples.data;

import com.cloudera.cdk.data.Dataset;
import com.cloudera.cdk.data.DatasetDescriptor;
import com.cloudera.cdk.data.DatasetReader;
import com.cloudera.cdk.data.DatasetRepositories;
import com.cloudera.cdk.data.DatasetRepository;
import com.cloudera.cdk.data.Formats;
import org.apache.avro.Schema;
import org.apache.avro.SchemaBuilder;
import org.apache.avro.generic.GenericData;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;

public class ReadMovies extends Configured implements Tool {

  @Override
  public int run(String[] args) throws Exception {
    DatasetRepository repo = DatasetRepositories.open("repo:hdfs://localhost:8020/user/cloudera");

    Dataset movies = repo.load("movies");
    DatasetReader reader = movies.newReader();
    try {
      reader.open();
      for (Object rec : reader) {
        System.err.println("Movie: " + rec);
      }
    } finally {
      reader.close();
    }

    return 0;
  }

  public static void main(String... args) throws Exception {
    int rc = ToolRunner.run(new ReadMovies(), args);
    System.exit(rc);
  }
}
