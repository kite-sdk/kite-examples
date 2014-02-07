# Kite SDK Examples

The Kite Examples project provides examples of how to use the Kite SDK.

Each example is a standalone Maven module with associated documentation.

## Basic Examples

* `dataset` is the closest to a HelloWorld example of Kite. It shows how to create datasets and perform streaming writes and reads over them.
* `dataset-hbase` shows how to store entities in HBase using the `RandomAccessDataset` API.
* `dataset-staging` shows how to use two datasets to manage Parquet-formatted data
* `logging` is an example of logging events from a command-line programs to Hadoop via Flume, using log4j as the logging API.
* `logging-webapp` is like `logging`, but the logging source is a webapp.

## Advanced Examples

* `demo` is a full end-to-end example of a webapp that logs events using Flume and performs session analysis using Crunch and Hive.

## Getting Started

The easiest way to run the examples is on the
[Cloudera QuickStart VM](https://ccp.cloudera.com/display/SUPPORT/Cloudera+QuickStart+VM),
which has all the necessary Hadoop services pre-installed, configured, and
running locally. See the notes below for any initial setup steps you should take.

The current examples run on version 4.4.0 of the QuickStart VM.

Checkout the latest [branch](https://github.com/kite-sdk/kite-examples/branches) of this repository in the VM:

```bash
git clone git://github.com/kite-sdk/kite-examples.git
cd kite-examples
```

If you are using a prepared Kite VM, the `git clone` command is already done for you.

Then choose the example you want to try and refer to the README in the relevant subdirectory.

### Setting up the QuickStart VM

There are two ways to run the examples with the QuickStart VM:

1. Logged in to the VM guest (username and password are both `cloudera`).
2. From your host computer.

The advantage of the first approach is that you don't need to install anything extra on
your host computer, such as Java or Maven, so there are no extra set up steps.

The second approach is preferable when you want to use tools from your own development
environment (browser, IDE, command line). However, there are a few extra steps you
need to take to configure the QuickStart VM, listed below.

* __Enable port forwarding__ For VirtualBox, open the Settings dialog for the VM,
select the Network tab, and click the Port Forwarding button. Map the following ports -
in each case the host port and the guest port should be the same. Also, your VM should
not be running when you are making these changes.
    * 7180 (Cloudera Manager web UI)
    * 8020, 50010, 50020, 50070, 50075 (HDFS NameNode and DataNode)
    * 8021 (MapReduce JobTracker)
    * 8888 (Hue web UI)
    * 9083 (Hive/HCatalog metastore)
    * 41415 (Flume agent)
    * 11000 (Oozie server)
    * 21050 (Impala JDBC port)

If you have `VBoxManage` installed on your host machine, you can do this via command line
as well. In bash, this would look something like:
```bash
# Set VM_NAME to the name of your VM as it appears in VirtualBox
VM_NAME="QuickStart VM"
PORTS="7180 8020 50010 50020 50070 50075 8021 8888 9083 41415 11000 21050"
for port in $PORTS; do
  VBoxManage modifyvm "$VM_NAME" --natpf1 "Rule $port,tcp,,$port,,$port"
done
```
* __Add a host entry for localhost.localdomain__ If your host computer does not have a
mapping for `localhost.localdomain`, then add a line like the following to `/etc/hosts`
```
127.0.0.1       localhost       localhost.localdomain
```
* __Sync the system clock__ For some of the examples it's important that the host and
guest times are in sync. To synchronize the guest, login and type
`sudo ntpdate pool.ntp.org`.
* __Restart the cluster__ Restart the whole cluster in Cloudera Manager.

# Troubleshooting

## Working with the VM

* __What are the usernames/passwords for the VM?__
  * Cloudera manager: 4.4.0: cloudera/cloudera, 4.3.0: admin/admin
  * HUE: cloudera/cloudera
  * Login: cloudera/cloudera

* __I can't find the file in VirtualBox (or VMWare)!__
  * You probably need to unpack it: In Windows, install 7zip, and _extract_ the
    VM files from the `.7z` file. In linux or mac, `cd` to where you copied the
    file and run `7zr e cloudera-quickstart-vm-4.3.0-kite-vbox-4.4.0.7z`
  * You should be able to import the extracted files to VirtualBox or VMWare

* __How do I open a `.ovf` file?__
  * Install and open [VirtualBox][vbox] on your computer
  * Under the menu "File", select "Import..."
  * Navigate to where you unpacked the `.ovf` file and select it

* __What is a `.vmdk` file?__
  * The `.vmdk` file is the virtual machine disk image that accompanies a
    `.ovf` file, which is a portable VM description.

* __How do I open a `.vbox` file?__
  * Install and open [VirtualBox][vbox] on your computer
  * Under the menu "Machine", select "Add..."
  * Navigate to where you unpacked the `.vbox` file and select it

* __How do I fix "VTx" errors?__
  * Reboot your computer and enter BIOS
  * Find the "Virtualization" settings, usually under "Security" and _enable_
    all of the virtualization options

* __How do I get my mouse back?__
  * If your mouse/keyboard is stuck in the VM (captured), you can usually
    release it by pressing the right `CTRL` key. If you don't have one (or that
    didn't work), then the release key will be in the __lower-right__ of the
    VirtualBox window

* __Other problems__
  * Using VirtualBox? Try using VMWare.
  * Using VMWare? Try using VirtualBox.

[vbox]: https://www.virtualbox.org/wiki/Downloads

