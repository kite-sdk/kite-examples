<%--
  Copyright 2013 Cloudera Inc.

  Licensed under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
--%>
<%--
  This file is here even though this module's packaging is 'pom' not 'war' to stop
  'mvn tomcat7:run' from reporting errors.
--%>
<html>
<head>
<title>CDK Demo</title>
<head>
<body>
<h2>CDK Demo</h2>
<p>
Start by <a href="../demo-logging-webapp/">logging event messages</a> to HDFS via Flume.
</p>
<p>
Then (after running through the project README to generate derived datasets) try running
one of the Impala-backed <a href="../demo-reports-webapp/">reports</a>.
</p>
</body>
</html>
