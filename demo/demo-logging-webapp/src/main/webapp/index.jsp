<%--
  Copyright 2015 Cloudera Inc.

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
<html>
<meta http-equiv="Content-Type" content="text/html; charset=utf-8">
<head>
<title>Kite Example</title>
<script>
function one() {
	document.input.submit();
}
</script>
</head>
<body>
<h2>Kite Example</h2>
<form name="input" action="send" method="get">
User ID: <input type="text" name="user_id" value="1">
Message: <input type="text" name="message" value="Hello!">
<table border="2" cellpadding="5" width="800">
<tr><td>
<select name="movie" onchange="one()">
<%
for (int i=1; i<1683; i++) {
%>
<%="<option>" + i + "</option>" %>
<%
}
%>
</select>
</td></tr>
<tr><td>
<iframe src="http://www.rottentomatoes.com/m/toy_story/" width="1200" height="800" />
</td></tr></table>
</form>
</body>
</html>
