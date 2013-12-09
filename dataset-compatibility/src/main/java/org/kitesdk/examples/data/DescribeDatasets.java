package org.kitesdk.examples.data;

import org.kitesdk.data.DatasetDescriptor;
import org.kitesdk.data.DatasetRepositories;
import org.kitesdk.data.DatasetRepository;
import org.kitesdk.data.Formats;
import org.apache.avro.Schema;
import org.apache.avro.SchemaBuilder;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;

public class DescribeDatasets extends Configured implements Tool {

  @Override
  public int run(String[] args) throws Exception {
    DatasetRepository repo = DatasetRepositories.open("repo:hdfs://localhost:8020/user/cloudera");

    Schema ratingSchema = SchemaBuilder.record("Rating")
        .fields()
        .name("userId").type().intType().noDefault()
        .name("movieId").type().intType().noDefault()
        .name("rating").type().intType().noDefault()
        .name("timeInSeconds").type().intType().noDefault()
        .endRecord();

    // create
    repo.create("ratings", new DatasetDescriptor.Builder()
//        .location("hdfs:u.data")
        .format(Formats.CSV)
        .property("cdk.csv.delimiter", "\t")
        .schema(ratingSchema)
        .build());

//    movie id | movie title | release date | video release date |
//    IMDb URL | unknown | Action | Adventure | Animation |
//        Children's | Comedy | Crime | Documentary | Drama | Fantasy |
//    Film-Noir | Horror | Musical | Mystery | Romance | Sci-Fi |
//        Thriller | War | Western |
    Schema movieSchema = SchemaBuilder.record("Movie")
        .fields()
        .name("movieId").type().intType().noDefault()
        .name("title").type().stringType().noDefault()
        .name("releaseDate").type().stringType().noDefault()
        .name("videoReleaseDate").type().stringType().noDefault()
        .name("imdbURL").type().stringType().noDefault()
        // ignore genre fields for now
        .endRecord();

    repo.create("movies", new DatasetDescriptor.Builder()
//        .location("hdfs:u.item")
        .format(Formats.CSV)
        .property("cdk.csv.delimiter", "|")
        .schema(movieSchema)
        .build());

    return 0;
  }

  public static void main(String... args) throws Exception {
    int rc = ToolRunner.run(new DescribeDatasets(), args);
    System.exit(rc);
  }
}
