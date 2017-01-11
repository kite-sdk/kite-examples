## Working with existing data sets

Sometimes you already have data in your cluster and would like to read it using
Kite. This tutorial explains how to point Kite at your existing dataset and work
with it, either to copy it into a recommended format or to use it as-is.

### Example data

This example works with the MovieLens data set from the GroupLens Research
Project at the University of Minnesota. The data set is made of viewer ratings,
1-5, of about 1600 movies. The data set is available from
[GroupLens](http://grouplens.org/datasets/movielens/).

This example uses the smallest dataset of 100,000 ratings. First, download the
`ml-100k.zip` file and unpack it. That zip file contains a `u.data` file with
all of the rating data and a `u.item` file with information about each movie.
To add these file to HDFS:

1. Unzip the file: `unzip ml-100k.zip`
2. Copy the `u.data` file into HDFS: `hdfs dfs -copyFromLocal ml-100k/u.data ratings.tsv`
3. Copy the `u.item` file into HDFS: `hdfs dfs -copyFromLocal ml-100k/u.item movies.psv`

This also renames the files to be a little more friendly.

### Configuring Kite Datasets

The first step is to configure Kite so that it knows how to read the two data
files we just added. We do this by building a DatasetDescriptor with
information about each data set and saving that description.

The rating data has 4 columns: user id, movie id, rating, and time. All of
these fields are integers, and we want Kite to read them as integers. We can
create a Schema to describe the file using a SchemaBuilder:

```java
Schema csvSchema = SchemaBuilder.record("Rating")
    .fields()
    .name("userId").type().intType().noDefault()
    .name("movieId").type().intType().noDefault()
    .name("rating").type().intType().noDefault()
    .name("timeInSeconds").type().intType().noDefault()
    .endRecord();
```

Alternatively, we could have created an `avsc` file with the schema:

```json
{
  "type" : "record",
  "name" : "Rating",
  "fields" : [ {
    "name" : "userId",
    "type" : "int"
  }, {
    "name" : "movieId",
    "type" : "int"
  }, {
    "name" : "rating",
    "type" : "int"
  }, {
    "name" : "timeInSeconds",
    "type" : "int"
  } ]
}
```

Next, we need to create a `DatasetDescriptor` with the schema and rest of the
information, like location and format:
```java
DatasetDescriptor ratings = DatasetDescriptor.Builder()
    .location("hdfs:ratings.tsv")
    .format(Formats.CSV)
    .property("kite.csv.delimiter", "\t")
    .schema(csvSchema)
    .build();
```

Finally, save the descriptor so it can be used later:
```java
Datasets.create("dataset:hdfs:/tmp/data/ratings", ratings);
```

Similarly, we will create a dataset for movies the same way. The file of movies
uses a pipe, `|`, as the delimiter and has a lot of genre columns that we want
to ignore. To ignore the unnecessary columns, we just leave them out of the
Schema definition (and can add them back later). Leaving out columns can only
be done for columns at the end of the record.
```java
Schema movieSchema = SchemaBuilder.record("Movie")
    .fields()
    .name("movieId").type().intType().noDefault()
    .name("title").type().stringType().noDefault()
    .name("releaseDate").type().stringType().noDefault()
    .name("videoReleaseDate").type().stringType().noDefault()
    .name("imdbURL").type().stringType().noDefault()
    // ignore genre fields for now
    .endRecord();

Datasets.create("dataset:hdfs:/tmp/data/movies", new DatasetDescriptor.Builder()
    .location("hdfs:movies.psv")
    .format(Formats.CSV)
    .property("kite.csv.delimiter", "|")
    .schema(movieSchema)
    .build());
```

These steps are done in the `org.kitesdk.examples.data.DescribeDatasets`
program:
```bash
mvn compile
mvn exec:java -Dexec.mainClass="org.kitesdk.examples.data.DescribeDatasets"
```

Now the datasets are ready to be used. You can read movies with this command:

```bash
mvn exec:java -Dexec.mainClass="org.kitesdk.examples.data.ReadMovies"
```

You should see information for each movie printed out:
```
Movie: {"movieId": 1, "title": "Toy Story (1995)", "releaseDate": "01-Jan-1995", "videoReleaseDate": "", "imdbURL": "http:\/\/us.imdb.com\/M\/title-exact?Toy%20Story%20(1995)"}
Movie: {"movieId": 2, "title": "GoldenEye (1995)", "releaseDate": "01-Jan-1995", "videoReleaseDate": "", "imdbURL": "http:\/\/us.imdb.com\/M\/title-exact?GoldenEye%20(1995)"}
Movie: {"movieId": 3, "title": "Four Rooms (1995)", "releaseDate": "01-Jan-1995", "videoReleaseDate": "", "imdbURL": "http:\/\/us.imdb.com\/M\/title-exact?Four%20Rooms%20(1995)"}
```

### Working with partitioned data

This will mainly describe how to express your partition strategy in Kite terms and go over using `DateFormatPartitioner`

### Reformatting a Dataset

This section will cover copying data into an avro dataset and what the advantages are. Hopefully this will use something like `Jobs.copy` (which doesn't exist yet).
