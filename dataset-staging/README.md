# Cloudera Development Kit - Examples Module

The Examples Module is a collection of CDK examples that demonstrate how the
CDK can help you solve your problems using Hadoop.

## Example - Staging data into persistent storage

CDK Datasets can store data in [Parquet][par], which is a file format that
stores data organized by column rather than by record. Because Parquet files
keep the data in contiguous chunks by column, appending new records to a
dataset requires rewriting substantial portions of existing an file or
buffering records to create a new file. So while Parquet may have storage and
query benefits, it doesn't make sense to write to it _directly_ from
record-based tools like Flume. Instead, this example demonstrates how to use an
avro dataset to _stage_ records and then write them in larger groups to a
persistent parquet dataset.

This example works with [simple log data][schema], which is generated in the
second step. This simulates an environment where log data is constantly being
written (probably by Flume), and eventually gets stored in Parquet.

[par]: http://parquet.io/
[schema]: https://github.com/cloudera/cdk-examples/blob/staging-example/dataset-staging/src/main/resources/simple-log.avsc

### Creating the datasets

This example uses two datasets, one as a staging area where data is temporarily
written and the other as a persistent store:

* `logs-staging`: This dataset is in avro (record-based) format. It is
  partitioned by time, so that parts can be read, written to the persistent
  dataset, and then deleted.
* `logs`: This is in Parquet format and is the persistent store.

For simplicity, this example partitions the staging dataset by day.

To create the two datasets, run `CreateStagedDataset`:
```bash
mvn exec:java -Dexec.mainClass="com.cloudera.cdk.examples.staging.CreateStagedDataset"
```

Now `tree` shows that there are two empty datasets in `/tmp/data`:
```
/tmp/data/
├── logs
└── logs-staging
```

### Adding log data to staging

Next, we'll add some simulated log data to the staging dataset.
`GenerateSimpleLogs` creates 15,000 fake log messages at various log levels,
starting with timestamps 24 hours ago and each spaced 5 seconds apart. This is
a little less than 24 hours worth of messages, so there should be messages for
yesterday and today.

This step is identical to writing data in the other dataset examples; the CDK
handles the partitioning logic set up when the tables were created.

To generate these messages in your repository, run:
```bash
mvn exec:java -Dexec.mainClass="com.cloudera.cdk.examples.staging.GenerateSimpleLogs"
```

Using `tree` again, we can see that there are avro files for yesterday (the
4th) and today (the 5th).
```
/tmp/data
├── logs
└── logs-staging
    ├── day=04
    │   └── 1378422612083-10.avro
    └── day=05
        └── 1378422612173-10.avro
```

### Moving data from staging to persistent

The last step is to move yesterday's data from the staging dataset to the
persistent dataset. The data in yesterday's partition is no longer being
written and is safe to move because the partition scheme is now writing today's
data to the next partition.

The `StagingToPersistentSerial` program moves yesterday's partition by opening
the staging dataset, selecting the partition for yesterday, and then writing
each record. To avoid duplicating data, the persistent dataset's writer is
closed last, after the partition is successfully deleted.

To run `StagingToPersistentSerial`, run:

```bash
mvn exec:java -Dexec.mainClass="com.cloudera.cdk.examples.staging.StagingToPersistentSerial"
```

After the move completes, the repository should look like this:
```
/tmp/data
├── logs
│   └── year=2013
│       └── month=09
│           └── day=04
│               └── 1378423563347-10.parquet
└── logs-staging
    └── day=05
        └── 1378422612173-10.avro
```

Keep in mind that this example uses a day-long partitions to keep the finished
data in staging (yesterday) separate from the currently appended data (today),
but a different partition scheme could be used instead.

## To do

* Staging to persistent in parallel - this requires a crunch parquet sink.
