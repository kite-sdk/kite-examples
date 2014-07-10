package org.kitesdk.examples.data;

import org.apache.avro.Schema;
import org.apache.avro.SchemaBuilder;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;
import org.kitesdk.data.DatasetDescriptor;
import org.kitesdk.data.Datasets;
import org.kitesdk.data.Formats;

public class DescribeDatasets extends Configured implements Tool {

  @Override
  public int run(String[] args) throws Exception {
    String home = "hdfs:/user/" + System.getProperty("user.name");

    Schema ratingSchema = SchemaBuilder.record("Rating")
        .fields()
        .name("userId").type().intType().noDefault()
        .name("movieId").type().intType().noDefault()
        .name("rating").type().intType().noDefault()
        .name("timeInSeconds").type().intType().noDefault()
        .endRecord();

    Datasets.create("dataset:hdfs:/tmp/data/ratings",
        new DatasetDescriptor.Builder()
            .location(home + "/ratings.tsv") // originally u.data
            .format(Formats.CSV)
            .property("kite.csv.delimiter", "\t")
            .schema(ratingSchema)
            .build(), Object.class);

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

    Datasets.create("dataset:hdfs:/tmp/data/movies",
        new DatasetDescriptor.Builder()
            .location(home + "/movies.psv") // originally u.item
            .format(Formats.CSV)
            .property("kite.csv.delimiter", "|")
            .schema(movieSchema)
            .build(), Object.class);

    return 0;
  }

  public static void main(String... args) throws Exception {
    int rc = ToolRunner.run(new DescribeDatasets(), args);
    System.exit(rc);
  }
}
