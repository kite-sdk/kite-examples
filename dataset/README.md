# Kite - Examples Module

This module is a set of examples that demonstrate the `kite-data` module, which
makes manging data sets in Hadoop easy with a declarative API and good defaults
based on Hadoop best practices.
* Data sets you create with the Kite data API are ready to use with Hive,
  Impala, and Crunch.
* Records are automatically compressed and stored in splittable files for
  Map/Reduce performance.
* Data schema, storage format, and partition strategy are declared when
  creating a data set, and work without hassle after that.

## Example - Hello World!

The simplest example is ["Hello Kite"][hello-java]. In this example, we will
create a data set, "hellos", that stores `Hello` objects. The `Hello` class is
defined at the end of [`HelloKite.java`][hello-java], and has a single field,
`name`, and a method called `sayHello`. To create a `Dataset` for these, we need to:

1. Create metadata that describes the data set (a `DatasetDescriptor`)
2. Create the data set with the descriptor and a URI for its location

For step 1, we use the `DatasetDescriptor.Builder` class to make a
`DatasetDescriptor` that holds the description of this data set. The only
required property is the schema (what the data looks like) and we can set that
by using a Builder that automatically inspects the `Hello` class:

```java
DatasetDescriptor descriptor = new DatasetDescriptor.Builder().schema(Hello.class).get();
```

For step 2, we use the `create` factory method in `Datasets` with a dataset URI
that points to the local filesystem to store the data (and metadata). The URI
here, "dataset:file:/tmp/hellos", tells Kite to store data in `/tmp/hellos`.

```java
DatasetRepository repo = DatasetRepositories.open("repo:file:/tmp/hello-kite");
```

With the repository and descriptor, we can now create a dataset.

```java
Dataset hellos = Datasets.create("dataset:file:/tmp/hellos", descriptor);
```

Create returns a working `Dataset` instance, and the `Dataset` can be loaded
later using the `Datasets.load` method and the dataset's URI. The descriptor's
configuration is stored in the dataset, so there is no need to pass it the next
time.

After creating the data set, the example creates a `Hello` object and writes it
out. Then, it reads it back in, calls `sayHello`, and finally, deletes the data
set. After you read through the rest of [`HelloKite code`][hello-java], you can
run the example from the `dataset/` folder with this command:
```bash
mvn exec:java -Dexec.mainClass="org.kitesdk.examples.data.HelloKite"
```

[hello-java]: src/main/java/org/kitesdk/examples/data/HelloKite.java

## Example - Products Dataset

This example shows basic usage of the Kite Data API for performing streaming writes
to (and reads from) a dataset. Like the "Hello Kite" example above, the
products are plain (old) java object, POJOs.

Build the code with:

```bash
mvn compile
```

Then create the dataset with:

```bash
mvn exec:java -Dexec.mainClass="org.kitesdk.examples.data.CreateProductDatasetPojo"
```

You can look at the files that were created in
[`/tmp/data/products`](http://localhost:8888/filebrowser/#/tmp/data/products).

Once we have created a dataset and written some data to it, the next thing to do is to
read it back. We can do this with the `ReadProductDatasetPojo` program.

```bash
mvn exec:java -Dexec.mainClass="org.kitesdk.examples.data.ReadProductDatasetPojo"
```

Finally, delete the dataset:

```bash
mvn exec:java -Dexec.mainClass="org.kitesdk.examples.data.DeleteProductDataset"
```

### Using the local filesystem

__Note__: The above assumes that you are running against a single-node localhost HDFS
installation, such as the one on the QuickStart VM.
If this is not the case, then you can change the repository URI in the source files
(e.g. `CreateProductDatasetPojo.java`) from `repo:hdfs:/tmp/data` to
`repo:file:///tmp/data` to use the local filesystem.

### Generic records vs. POJOs

The previous examples used POJOs, since they are the most familiar data transfer
objects for most Java programmers. Avro supports generic records too,
which are more efficient, since they don't require reflection,
and also don't require either the reader or writer to have the POJO class available.

__Note__: It's currently not possible to write POJOs and then read them back as generic
objects since the return type will be the POJO class. So for the generic examples below
we model a dataset of users rather than products.

Run the following to use the generic writer and reader:

```bash
mvn exec:java -Dexec.mainClass="org.kitesdk.examples.data.CreateUserDatasetGeneric"
mvn exec:java -Dexec.mainClass="org.kitesdk.examples.data.ReadUserDatasetGeneric"
mvn exec:java -Dexec.mainClass="org.kitesdk.examples.data.DeleteUserDataset"
```

### Partitioning

The API supports partitioning, so that records are written to different
partition files according to the value of particular partition fields. In this
case, we are storing the users by favorite color.

```bash
mvn exec:java -Dexec.mainClass="org.kitesdk.examples.data.CreateUserDatasetGenericPartitioned"
```

You can see how partitioning affects the data layout by looking at the subdirectories
created in [`/tmp/data/users`](http://localhost:8888/filebrowser/#/tmp/data/users).

Configuring a partition strategy helps Kite efficiently scan through datasets.
Try running the `ReadUserDatasetGeneric` command again, where this time the
dataset is partitioned. The output includes the file reader's debug messages,
which logs when files are opened and closed. The users are grouped by color and
to read the dataset, Kite reads through each file.

```bash
mvn exec:java -Dexec.mainClass="org.kitesdk.examples.data.ReadUserDatasetGeneric"
 . . .
{"username": "user-94", "creationDate": 1404431068720, "favoriteColor": "brown"}
DEBUG :: Closing reader on path:hdfs://.../users/favorite_color=brown/a13ce52d-819e-455e-a57b-0d948461d543.avro
DEBUG :: Opening reader on path:hdfs://.../users/favorite_color=green/95730f5e-214d-4452-89f4-f2d4c709bc29.avro
{"username": "user-4", "creationDate": 1404431068693, "favoriteColor": "green"}
 . . .
{"username": "user-98", "creationDate": 1404431068720, "favoriteColor": "green"}
DEBUG :: Closing reader on path:hdfs://.../users/favorite_color=green/95730f5e-214d-4452-89f4-f2d4c709bc29.avro
DEBUG :: Opening reader on path:hdfs://.../users/favorite_color=pink/6cb3aa1c-f800-4c3f-9b19-4be9a72ee14d.avro
{"username": "user-3", "creationDate": 1404431068679, "favoriteColor": "pink"}
 . . .
{"username": "user-99", "creationDate": 1404431068720, "favoriteColor": "pink"}
DEBUG :: Closing reader on path:hdfs://.../users/favorite_color=pink/6cb3aa1c-f800-4c3f-9b19-4be9a72ee14d.avro
DEBUG :: Opening reader on path:hdfs://.../users/favorite_color=yellow/8ef04d26-906d-4bfc-b045-cd6667635907.avro
{"username": "user-0", "creationDate": 1404431068625, "favoriteColor": "yellow"}
```

Now run the `ReadUserDatasetGenericOnePartition` tool, which selects users with
the favorite color green when creating the reader:

```java
reader = users.with("favoriteColor", "green").newReader();
```

```bash
mvn exec:java -Dexec.mainClass="org.kitesdk.examples.data.ReadUserDatasetGenericOnePartition"
DEBUG :: Opening reader on path:hdfs://.../users/favorite_color=green/95730f5e-214d-4452-89f4-f2d4c709bc29.avro
{"username": "user-4", "creationDate": 1404431068693, "favoriteColor": "green"}
 . . .
{"username": "user-98", "creationDate": 1404431068720, "favoriteColor": "green"}
DEBUG :: Closing reader on path:hdfs://.../users/favorite_color=green/95730f5e-214d-4452-89f4-f2d4c709bc29.avro
```

Notice that Kite doesn't scan through all of the other directories, the only
file it opens is in the green directory. The partition strategy is what makes
this possible. Otherwise, Kite would sort through all of the data to find just
the users with the favorite color green.

Last, delete the partitioned dataset before moving on to the next example.

```bash
mvn exec:java -Dexec.mainClass="org.kitesdk.examples.data.DeleteUserDataset"
```

### Parquet Columnar Format

Parquet is a new columnar format for data. Columnar formats provide performance
advantages over row-oriented formats like Avro data files (which is the default in Kite),
when the number of columns is large (typically dozens) and the typical queries that you perform
over the data only retrieve a small number of the columns.

```bash
mvn exec:java -Dexec.mainClass="org.kitesdk.examples.data.CreateUserDatasetGenericParquet"
```

You can see the parquet file extension for files in
[`/tmp/data/users`](http://localhost:8888/filebrowser/#/tmp/data/users).

```bash
mvn exec:java -Dexec.mainClass="org.kitesdk.examples.data.ReadUserDatasetGeneric"
mvn exec:java -Dexec.mainClass="org.kitesdk.examples.data.DeleteUserDataset"
```

### Hive

So far all metadata has been stored in the _.metadata_ directory on the filesystem.
It's possible to store metadata in Hive so that other MetaStore-enabled applications
like Hive can make use of it.

Run the following to create the dataset:

```bash
mvn exec:java -Dexec.mainClass="org.kitesdk.examples.data.CreateHiveUserDatasetGeneric"
```

Note: This example assumes a local (not embedded) metastore running on the local machine. You can
change the default to use a different metastore by editing _src/main/resources/hive-site.xml_.

Now inspect the dataset storage area in
[`/user/hive/warehouse/users`](http://localhost:8888/filebrowser/#/user/hive/warehouse/users).

Notice that there is no metadata stored there, since the metadata is stored in
[Hive's metastore](http://localhost:8888/metastore/tables/).

You can use SQL to query the data directly using the
[Hive UI (Beeswax)](http://localhost:8888/beeswax/) in Hue. For example:

```
select * from users
```

Alternatively, you can use the Java API to read the data:

```bash
mvn exec:java -Dexec.mainClass="org.kitesdk.examples.data.ReadHiveUserDatasetGeneric"
```

Deleting the dataset deletes the metadata from the metastore and the data from the
filesystem:

```bash
mvn exec:java -Dexec.mainClass="org.kitesdk.examples.data.DeleteHiveUserDataset"
```

## Scala

Run the equivalent example with:

```bash
scala -cp "$(mvn dependency:build-classpath | grep -v '^\[')" src/main/scala/createpojo.scala
```

Or for the generic example:

```bash
scala -cp "$(mvn dependency:build-classpath | grep -v '^\[')" src/main/scala/creategeneric.scala
```

The Java examples can be used to read (and delete) the dataset written from Scala:

```bash
mvn exec:java -Dexec.mainClass="org.kitesdk.examples.data.ReadUserDatasetGeneric"
mvn exec:java -Dexec.mainClass="org.kitesdk.examples.data.DeleteUserDataset"
```
