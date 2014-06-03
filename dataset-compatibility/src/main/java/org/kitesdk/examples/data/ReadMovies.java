package org.kitesdk.examples.data;

import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;
import org.kitesdk.data.Dataset;
import org.kitesdk.data.DatasetReader;
import org.kitesdk.data.DatasetRepositories;
import org.kitesdk.data.DatasetRepository;

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
