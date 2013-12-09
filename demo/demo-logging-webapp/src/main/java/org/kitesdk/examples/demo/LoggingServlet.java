/**
 * Copyright 2013 Cloudera Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.kitesdk.examples.demo;

import org.kitesdk.data.event.StandardEvent;
import java.io.IOException;
import java.io.PrintWriter;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.log4j.Logger;

public class LoggingServlet extends HttpServlet {

  private Logger logger = Logger.getLogger(LoggingServlet.class);

  @Override
  protected void doGet(HttpServletRequest request, HttpServletResponse
      response) throws ServletException, IOException {

    response.setContentType("text/html");
    PrintWriter pw = response.getWriter();
    pw.println("<html>");
    pw.println("<head><title>Kite Example</title></title>");
    pw.println("<body>");

    String userId = request.getParameter("user_id");
    String message = request.getParameter("message");
    if (message == null) {
      pw.println("<p>No message specified.</p>");
    } else {
      pw.println("<p>Message: " + message + "</p>");
      StandardEvent event = StandardEvent.newBuilder()
          .setEventInitiator("server_user")
          .setEventName("web:message")
          .setUserId(Long.parseLong(userId))
          .setSessionId(request.getSession(true).getId())
          .setIp(request.getRemoteAddr())
          .setTimestamp(System.currentTimeMillis())
          .build();
      logger.info(event);
    }
    pw.println("<p><a href=\"/demo-logging-webapp\">Home</a></p>");
    pw.println("</body></html>");

  }

}
