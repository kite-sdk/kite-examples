# Kite - Examples Module

The Examples Module is a collection of Kite examples that demonstrate how
Kite can help you solve your problems using Hadoop.

## Example - Staging data into persistent storage

Kite Datasets can store data in [Parquet][par], which is a file format that
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
[schema]: https://github.com/kite-sdk/kite-examples/blob/staging-example/dataset-staging/src/main/resources/simple-log.avsc

### Creating the datasets

This example uses two datasets, one as a staging area where data is temporarily
written and the other as a persistent store:

* `logs_staging`: This dataset is in avro (record-based) format. It is
  partitioned by time, so that parts can be read, written to the persistent
  dataset, and then deleted.
* `logs`: This is in Parquet format and is the persistent store.

For simplicity, this example partitions the staging dataset by day.

To create the two datasets, run `CreateStagedDataset`:
```bash
mvn exec:java -Dexec.mainClass="org.kitesdk.examples.staging.CreateStagedDataset"
```

Now `tree` shows that there are two empty datasets in `/tmp/data`:
```
/tmp/data/
├── logs
└── logs_staging
```

### Adding log data to staging

Next, we'll add some simulated log data to the staging dataset.
`GenerateSimpleLogs` creates 15,000 fake log messages at various log levels,
starting with timestamps 24 hours ago and each spaced 5 seconds apart. This is
a little less than 24 hours worth of messages, so there should be messages for
yesterday and today.

This step is identical to writing data in the other dataset examples; Kite
handles the partitioning logic set up when the tables were created.

To generate these messages in your repository, run:
```bash
mvn exec:java -Dexec.mainClass="org.kitesdk.examples.staging.GenerateSimpleLogs"
```

Using `tree` again, we can see that there are avro files for yesterday (the
5th) and today (the 6th).
```
/tmp/data/
├── logs
└── logs_staging
    ├── day=05
    │   └── 754ed830-074d-4600-8e89-24f6eb5ffc9b.avro
    └── day=06
        └── c547e07a-e47b-4ba1-a588-15abbbdb9631.avro
```

### Moving data from staging to persistent

The last step is to move yesterday's data from the staging dataset to the
persistent dataset. The data in yesterday's partition is no longer being
written and is safe to move because the partition scheme is now writing today's
data to the next partition.

The `StagingToPersistent` program moves yesterday's partition by opening
the staging dataset, selecting the partition for yesterday, and then writing
each record in parallel using Crunch.

To run `StagingToPersistent`, run:

```bash
mvn exec:java -Dexec.mainClass="org.kitesdk.examples.staging.StagingToPersistent"
```

After the move completes, the repository should look like this:
```
/tmp/data/
├── logs
│   └── year=2014
│       └── month=03
│           └── day=05
│               └── bd0a2ae1-5e35-48cf-8419-cd8332c0441f.parquet
└── logs_staging
    └── day=06
        └── c547e07a-e47b-4ba1-a588-15abbbdb9631.avro
```

Keep in mind that this example uses a day-long partitions to keep the finished
data in staging (yesterday) separate from the currently appended data (today),
but a different partition scheme could be used instead.

Finish up by deleting the data with

```bash
rm -rf /tmp/data/logs /tmp/data/logs_staging
```
