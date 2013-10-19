# Cloudera Development Kit - Examples Module

This module is a set of examples that demonstrate the `cdk-data` module, which
makes manging data sets in Hadoop easy with a declarative API and good defaults
based on Hadoop best practices.
* Data sets you create with the CDK data API are ready to use with Hive,
  Impala, and Crunch.
* Records are automatically compressed and stored in splittable files for
  Map/Reduce performance.
* Data schema, storage format, and partition strategy are declared when
  creating a data set, and work without hassle after that.

## Example - Hello World!

The simplest example is ["Hello CDK"][hello-java]. In this example, we will
create a data set, "hellos", that stores `Hello` objects. The `Hello` class is
defined at the end of [`HelloCDK.java`][hello-java], and has a single field,
`name`, and a method called `sayHello`. To create a `Dataset` for these, we need to:

1. Create metadata that describes the data set (a `DatasetDescriptor`)
2. Open a repository to create the data set

For step 1, we use the `DatasetDescriptor.Builder` class to make a
`DatasetDescriptor` that holds the description of this data set. The only
required property is the schema (what the data looks like) and we can set that
by using a Builder that automatically inspects the `Hello` class:

```java
DatasetDescriptor descriptor = new DatasetDescriptor.Builder().schema(Hello.class).get();
```

For step 2, we are going to use a repository in the local file system with data
(and metadata) stored in `/tmp/hello-cdk`. We can open this repository with
this line:

```java
DatasetRepository repo = DatasetRepositories.open("repo:file:/tmp/hello-cdk");
```

With the repository and descriptor, we can now create a dataset.

```java
Dataset hellos = repo.create("hellos", descriptor);
```

After creating the data set, the example creates a `Hello` object and writes it
out. Then, it reads it back in, calls `sayHello`, and finally, deletes the data
set. After you read through the rest of [`HelloCDK code`][hello-java], you can
run the example from the `dataset/` folder with this command:
```bash
mvn exec:java -Dexec.mainClass="com.cloudera.cdk.examples.data.CreateProductDatasetPojo"
```

[hello-java]: src/main/java/com/cloudera/cdk/examples/data/HelloCDK.java

## Example - Products Dataset

This example shows basic usage of the CDK Data API for performing streaming writes
to (and reads from) a dataset.

Build the code with:

```bash
mvn compile
```

Then create the dataset with:

```bash
mvn exec:java -Dexec.mainClass="com.cloudera.cdk.examples.data.CreateProductDatasetPojo"
```

You can look at the files that were created in
[`/tmp/data/products`](http://localhost:8888/filebrowser/#/tmp/data/products).

Once we have created a dataset and written some data to it, the next thing to do is to
read it back. We can do this with the `ReadProductDatasetPojo` program.

```bash
mvn exec:java -Dexec.mainClass="com.cloudera.cdk.examples.data.ReadProductDatasetPojo"
```

Finally, delete the dataset:

```bash
mvn exec:java -Dexec.mainClass="com.cloudera.cdk.examples.data.DeleteProductDataset"
```

### Using the local filesystem

__Note__: The above assumes that you are running against a single-node localhost HDFS
installation, such as the one on the QuickStart VM.
If this is not the case, then you can change `fs.default.name` in
`src/main/resources/core-site.xml`, e.g. to `file:///` to use the local filesystem.
Alternatively, you can pass in extra arguments to the command, as follows:

```bash
mvn exec:java -Dexec.mainClass="com.cloudera.cdk.examples.data.CreateProductDatasetPojo" \
  -Dexec.args="-fs file:///"
```

For the rest of the examples we will assume a single-node localhost HDFS installation.

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
mvn exec:java -Dexec.mainClass="com.cloudera.cdk.examples.data.CreateUserDatasetGeneric"
mvn exec:java -Dexec.mainClass="com.cloudera.cdk.examples.data.ReadUserDatasetGeneric"
mvn exec:java -Dexec.mainClass="com.cloudera.cdk.examples.data.DeleteUserDataset"
```

### Partitioning

The API supports partitioning, so that records are written to different partition files
according to the value of particular partition fields.

```bash
mvn exec:java -Dexec.mainClass="com.cloudera.cdk.examples.data.CreateUserDatasetGenericPartitioned"
```

You can see how partitioning affects the data layout by looking at the subdirectories
created in [`/tmp/data/users`](http://localhost:8888/filebrowser/#/tmp/data/users).

```bash
mvn exec:java -Dexec.mainClass="com.cloudera.cdk.examples.data.ReadUserDatasetGeneric"
mvn exec:java -Dexec.mainClass="com.cloudera.cdk.examples.data.ReadUserDatasetGenericOnePartition"
mvn exec:java -Dexec.mainClass="com.cloudera.cdk.examples.data.DeleteUserDataset"
```

### Parquet Columnar Format

Parquet is a new columnar format for data. Columnar formats provide performance
advantages over row-oriented formats like Avro data files (which is the default in CDK),
when the number of columns is large (typically dozens) and the typical queries that you perform
over the data only retrieve a small number of the columns.

```bash
mvn exec:java -Dexec.mainClass="com.cloudera.cdk.examples.data.CreateUserDatasetGenericParquet"
```

You can see the parquet file extension for files in
[`/tmp/data/users`](http://localhost:8888/filebrowser/#/tmp/data/users).

```bash
mvn exec:java -Dexec.mainClass="com.cloudera.cdk.examples.data.ReadUserDatasetGeneric"
mvn exec:java -Dexec.mainClass="com.cloudera.cdk.examples.data.DeleteUserDataset"
```

### HCatalog

So far all metadata has been stored in the _.metadata_ directory on the filesystem.
It's possible to store metadata in HCatalog so that other HCatalog-aware applications
like Hive can make use of it.

Run the following to create the dataset:

```bash
mvn exec:java -Dexec.mainClass="com.cloudera.cdk.examples.data.CreateHCatalogUserDatasetGeneric"
```

Note: This example assumes a local (not embedded) metastore running on the local machine. You can
change the default to use a different metastore by editing _src/main/resources/hive-site.xml_.

Now inspect the dataset storage area in
[`/user/hive/warehouse/users`](http://localhost:8888/filebrowser/#/user/hive/warehouse/users).

Notice that there is no metadata stored there, since the metadata is stored in
[Hive/HCatalog's metastore](http://localhost:8888/metastore/tables/).

You can use SQL to query the data directly using the
[Hive UI (Beeswax)](http://localhost:8888/beeswax/) in Hue. For example:

```
select * from users
```

Alternatively, you can use the Java API to read the data:

```bash
mvn exec:java -Dexec.mainClass="com.cloudera.cdk.examples.data.ReadHCatalogUserDatasetGeneric"
```

Deleting the dataset deletes the metadata from the metastore and the data from the
filesystem:

```bash
mvn exec:java -Dexec.mainClass="com.cloudera.cdk.examples.data.DeleteHCatalogUserDataset"
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
mvn exec:java -Dexec.mainClass="com.cloudera.cdk.examples.data.ReadUserDatasetGeneric"
mvn exec:java -Dexec.mainClass="com.cloudera.cdk.examples.data.DeleteUserDataset"
```
