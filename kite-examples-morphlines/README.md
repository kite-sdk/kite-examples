# Kite - Morphlines Examples

This module contains examples for how to unit test Morphline config files and custom Morphline commands.
For details consult the `pom.xml` build file, 
as well as the Morphline config files in the `src/test/resources/test-morphlines` directory, 
as well as the test data files in the `src/test/resources/test-documents` directory, 
as well as unit tests in the `src/test/java/` directory tree,
as well as the example custom morphline command implementations in the `src/main/java/` directory tree.

## Building

This step builds the software from source. It also runs the unit tests.

```bash
git clone https://github.com/kite-sdk/kite-examples.git
cd kite-examples/kite-examples-morphlines
#git checkout master
#git checkout 0.12.0 # or whatever the latest version is
mvn clean package
```

## Using the Maven CLI to run test data through a morphline

* This section describes how to use the mvn CLI to run test data through a morphline config file. 
* Here we use the simple [MorphlineDemo](https://github.com/kite-sdk/kite/blob/master/kite-morphlines/kite-morphlines-core/src/test/java/org/kitesdk/morphline/api/MorphlineDemo.java) class.

```bash
cd kite-examples/kite-examples-morphlines
mvn test -DskipTests exec:java -Dexec.mainClass="org.kitesdk.morphline.api.MorphlineDemo" -Dexec.args="src/test/resources/test-morphlines/addValues.conf src/test/resources/test-documents/email.txt" -Dexec.classpathScope=test
```

* The first parameter in `exec.args` above is the morphline config file and the remaining parameters specify one or more data files to run over. At least one data file is required.
* To print diagnostic information such as the content of records as they pass through the morphline commands, consider enabling TRACE log level, for example by adding the following line to your 
`src/test/resources/log4j.properties` file:

```
log4j.logger.org.kitesdk.morphline=TRACE
```

## Integrating with Eclipse

* This section describes how to integrate the codeline with Eclipse.
* Build the software as described above. Then create Eclipse projects like this:

```bash
cd kite-examples/kite-examples-morphlines
mvn eclipse:eclipse
```

* `mvn eclipse:eclipse` creates several Eclipse projects, one for each maven submodule.
It will also download and attach the jars of all transitive dependencies and their source code to the eclipse
projects, so you can readily browse around the source of the entire call stack.
* Then in eclipse do Menu `File/Import/Maven/Existing Maven Project/` on the root parent
directory `~/kite-examples/kite-examples-morphlines` and select all submodules, then "Next" and "Finish".
* You will see some maven project errors that keep eclipse from building the workspace because
the eclipse maven plugin has some weird quirks and limitations. To work around this, next, disable
the maven "Nature" by clicking on the project in the browser, right clicking on Menu
`Maven/Disable Maven Nature`. Repeat this for each project. This way you get all the niceties of the maven dependency management
without the hassle of the (current) Maven Eclipse plugin, everything compiles fine from within
Eclipse, and junit works and passes from within Eclipse as well.
* When a pom changes simply rerun `mvn eclipse:eclipse` and
then run Menu `Eclipse/Refresh Project`. No need to disable the Maven "Nature" again and again.
* To run junit tests from within eclipse click on the project (e.g. `kite-examples-morphlines`)
in the eclipse project explorer, right click, `Run As/JUnit Test`.

## Integrating with IntelliJ

* This section describes how to integrate the codeline with IntelliJ.
* Build the software as described above. 
* Open the `pom.xml` file in IntelliJ. This should create the entire project in the IDE. You
  do not need to "import the project" or anything like that, just `file>>open` and pick the
  `pom.xml` file.
  * You may have to select `build>>rebuild` project to get all the dependencies.
  * You may have to build the project externally via `mvn test` to resolve dependencies.  
* In IntelliJ, you should be able to right-click on the `ExampleMorphlineTest.java` file and see a 
   choice to "Run ExampleMorphlineTest" or "Debug ExampleMorphlineTest" to run the unit test and see
   the magic green bar.

##  Play around a bit _before_ changing anything!

1. Set some breakpoints and examine the Record for instance.
2. Examine the contents of the two Records.
3. Change one of the Asserts to insure failure to see what that looks like.
4. Skip all this of course if you're already familiar with jUnit etc.
  
## Get to work

1. Put your input file into the `resources/test-documents` directory, as a sibling to `simpleCSV.txt`
2. Change the Java unit test code method `ExampleMorphlineTest.testSimpleCSV()` to use that file by replacing `simpleCSV.txt` with your file.
3. Now start adding commands to the `simpleCSV.conf` morphlines file in the `resources/test-morphlines` directory
  1. You can use a different morphlines file, just put it in the same directory
     as `simpleCSV.conf` and load it in the test by changing the `createMorphline` call.
4. In the `simpleCSV.conf` file, you'll see a `SOLR_HOME_DIR` variable. That points to the
    `resources/solr/collection1/conf` directory (the /conf if implied). This is where your 
    Solr `schema.xml` file must live. As you add morphline commands to put new fields into the record,
    you'll _probably_ be changing the schema as well by adding those fields.
  1. If you examine your records and don't see fields that you _know_ you put in,
     it's quite likely that you didn't add them to the `schema.xml` file and thus the Morphlines command
     `sanitizeUnknownSolrFields` took the field out.
5. Pedantic recommendation: Just add one or two Morphlines commands at a time, adding
    lots of things at once is an easy way to get lost.
    
## Notice several things

1. Notice several things about the current `simpleCSV.conf` file.
2. Actually adding the record to Solr is commented out. We don't need the
   complications of setting that up too at this stage.
3. Near the bottom of the Morphlines config file, there are the import statements,
   one for kite and one for the cdk. Use the kite one! The pom is set up for kite (e.g. for use with CDH 5). 
   The cdk import is there for reference (e.g. for use with CDH 4).

## Deploy to Flume or MapReduce

1. Once this all runs to your satisfaction, copy the morphlines config (and possibly the Solr schema file if you've modified it) to your Flume or MapReduce configuration and give it a spin.
2. It's probably useful to just copy/paste the bits in the "commands" section of 
    the morphlines configuration. Otherwise be careful to modify the SOLR_LOCATOR and 
    (perhaps) import statements to reflect your setup.

Happy Morphlining!
