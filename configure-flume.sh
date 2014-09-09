#!/bin/bash

if [[ "$EUID" -ne 0 ]]; then
  echo "Please run using sudo: sudo $0"
  exit
fi

# Make sure there isn't a plugins.d in /usr/lib/flume-ng already
if [[ -d /usr/lib/flume-ng/plugins.d ]]; then
  echo "Error: /usr/lib/flume-ng/plugins.d already exists and is a directory"
  exit
fi

# Create the plugins.d folder in /var/lib/flume-ng
if [[ ! -d /var/lib/flume-ng/plugins.d ]]; then
  mkdir -p /var/lib/flume-ng/plugins.d
fi


# Link /usr/lib/flume-ng/plugins.d to /var/lib/flume-ng/plugins.d
if [[ -d /usr/lib/flume-ng && ! -L /usr/lib/flume-ng/plugins.d ]]; then
  ln -s /var/lib/flume-ng/plugins.d /usr/lib/flume-ng/plugins.d
fi

# Create the lib and libext directories for the dataset-sink plugin
mkdir -p /var/lib/flume-ng/plugins.d/dataset-sink/lib
mkdir -p /var/lib/flume-ng/plugins.d/dataset-sink/libext

# Remove any existing libraries/symlinks
rm -f /var/lib/flume-ng/plugins.d/dataset-sink/lib/*
rm -f /var/lib/flume-ng/plugins.d/dataset-sink/libext/*

# Create links to the kite-data-hcatalog and kite-data-hbase jars
ln -s /usr/lib/kite/kite-data-hcatalog.jar /var/lib/flume-ng/plugins.d/dataset-sink/lib/kite-data-hcatalog.jar
ln -s /usr/lib/kite/kite-data-hbase.jar /var/lib/flume-ng/plugins.d/dataset-sink/lib/kite-data-hbase.jar

# Create links to the Kite dependencies
ln -s /usr/lib/hive/lib/antlr-2.7.7.jar /var/lib/flume-ng/plugins.d/dataset-sink/libext/antlr-2.7.7.jar 
ln -s /usr/lib/hive/lib/antlr-runtime-3.4.jar /var/lib/flume-ng/plugins.d/dataset-sink/libext/antlr-runtime-3.4.jar 
ln -s /usr/lib/hive/lib/datanucleus-api-jdo-3.2.1.jar /var/lib/flume-ng/plugins.d/dataset-sink/libext/datanucleus-api-jdo-3.2.1.jar 
ln -s /usr/lib/hive/lib/datanucleus-core-3.2.2.jar /var/lib/flume-ng/plugins.d/dataset-sink/libext/datanucleus-core-3.2.2.jar 
ln -s /usr/lib/hive/lib/datanucleus-rdbms-3.2.1.jar /var/lib/flume-ng/plugins.d/dataset-sink/libext/datanucleus-rdbms-3.2.1.jar 
ln -s /usr/lib/hive/lib/hive-common.jar /var/lib/flume-ng/plugins.d/dataset-sink/libext/hive-common.jar 
ln -s /usr/lib/hive/lib/hive-exec.jar /var/lib/flume-ng/plugins.d/dataset-sink/libext/hive-exec.jar 
ln -s /usr/lib/hive/lib/hive-metastore.jar /var/lib/flume-ng/plugins.d/dataset-sink/libext/hive-metastore.jar 
ln -s /usr/lib/hive/lib/jdo-api-3.0.1.jar /var/lib/flume-ng/plugins.d/dataset-sink/libext/jdo-api-3.0.1.jar 
ln -s /usr/lib/hive/lib/libfb303-0.9.0.jar /var/lib/flume-ng/plugins.d/dataset-sink/libext/libfb303-0.9.0.jar 
