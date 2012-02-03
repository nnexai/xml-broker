package xml.eventbroker.example;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.net.SocketException;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.AsyncContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringEscapeUtils;

import xml.eventbroker.shared.MultiXMLRootFilter;

public class ExampleService extends HttpServlet {

	private static final long serialVersionUID = 1L;
	private static final Logger logger = Logger.getAnonymousLogger();

	List<String> events;

	@Override
	public void init() throws ServletException {
		super.init();
		events = new LinkedList<String>();
	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		PrintWriter writer = resp.getWriter();

		writer.append("<html><body>");
		writer.append("<ul>");

		synchronized (events) {
			for (String event : events) {
				writer.append("<li>");
				writer.append(StringEscapeUtils.escapeXml(event));
				writer.append("</li>");
			}
		}

		writer.append("</ul>");
		writer.append("</body></html>");

		writer.flush();

		resp.setStatus(HttpServletResponse.SC_OK);
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		AsyncContext async = req.startAsync();
		logger.info("Setting session timeout");
		async.setTimeout(20 * 1000);
		Thread t = new ProcessingThread(async);
		t.start();

	}

	class ProcessingThread extends Thread {

		private final AsyncContext cxt;

		public ProcessingThread(AsyncContext cxt) {
			this.cxt = cxt;
		}

		@Override
		public void run() {

			try {
				readXMLEvents(cxt.getRequest().getReader());
			} catch (IOException e) {
				logger.log(Level.WARNING,
						"Error getting or closing Request-Reader", e);
				cxt.complete();
			}

		}

		private void readXMLEvents(Reader reader) throws IOException {
			MultiXMLRootFilter filter = new MultiXMLRootFilter(reader, 0x100);
			try {

				while (!filter.hasFinished()) {
					int r;
					StringBuilder b = new StringBuilder(0x1000);
					char[] buf = new char[0x100];

					while ((r = filter.read(buf)) >= 0) {
						b.append(buf, 0, r);
					}
					System.err.println("<--- EVENT!! --->");

					synchronized (events) {
						logger.info("Added event " + b.toString());
						events.add(b.toString());
					}
				}
				filter.forceClose();
				
				logger.info("Connection was closed by remote");
				cxt.complete();
			} catch (SocketException e) {
				logger.log(Level.WARNING, "Connection timed out", e);
			} catch (IOException e) {
				logger.log(Level.WARNING, "Connection timed out", e);
			} finally {
				reader.close();
			}

		}
	}
}
