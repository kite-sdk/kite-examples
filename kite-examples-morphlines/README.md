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
#git checkout 0.11.0 # or whatever the latest version is
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

