package org.kitesdk.examples.logging.webapp;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.concurrent.atomic.AtomicLong;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericRecordBuilder;
import org.apache.log4j.Logger;

public class LoggingServlet extends HttpServlet {

  private static final AtomicLong id = new AtomicLong();

  private final Logger logger = Logger.getLogger(LoggingServlet.class);
  private Schema schema;

  @Override
  public void init() throws ServletException {
    try {
      // Load the schema from our classpath
      this.schema = new Schema.Parser().parse(
          getClass().getResourceAsStream("/event.avsc"));
    } catch (IOException ex) {
      throw new ServletException("Can't load events.avsc schema", ex);
    }
  }

  @Override
  protected void doGet(HttpServletRequest request, HttpServletResponse
      response) throws ServletException, IOException {

    response.setContentType("text/html");
    PrintWriter pw = response.getWriter();
    pw.println("<html>");
    pw.println("<head><title>Kite Example</title></title>");
    pw.println("<body>");

    String message = request.getParameter("message");
    if (message == null) {
      pw.println("<p>No message specified.</p>");
    } else {
      pw.println("<p>Message: " + message + "</p>");
      GenericData.Record event = new GenericRecordBuilder(schema)
          .set("id", id.incrementAndGet())
          .set("message", message)
          .build();
      logger.info(event);
    }
    pw.println("<p><a href=\"/logging-webapp\">Home</a></p>");
    pw.println("</body></html>");

  }

}
