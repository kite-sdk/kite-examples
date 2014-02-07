# Kite - Morphlines Examples

This module contains examples for how to unit test Morphline config files and custom Morphline commands.

## Building

This step builds the software from source.

```bash
git clone https://github.com/kite-sdk/kite-examples.git
cd kite-examples/kite-examples-morphlines
#git checkout master
#git checkout 0.11.0 # or whatever the latest version is
mvn clean test
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
directory `~/kite-examples/kite-morphlines-examples` and select all submodules, then "Next" and "Finish".
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
