package xml.eventbroker.example;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.AsyncContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang3.StringEscapeUtils;

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
		HttpSession session =  req.getSession( true );
		logger.info("Setting session timeout");
		session.setMaxInactiveInterval(60*60*1000);
		async.setTimeout(60*60*1000);
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
					logger.log(Level.WARNING, "Error getting or closing Request-Reader", e);
					cxt.complete();
				}
			
			}

		private void readXMLEvents(Reader reader) throws IOException {
		reader = new MultiXMLRootFilter(reader, 0x100);
		try {

			while(true) {
				int r;
				StringBuilder b  = new StringBuilder(0x100);
				char[] buf = new char[0x100];
								
				while( (r = reader.read(buf)) > 0 ) {
					b.append(buf, 0, r);
				}
				
				synchronized (events) {
					logger.info("Added event "+b.toString());
					events.add(b.toString());
				}
				
				if(r == -2)
					break;
			}
		} catch (IOException e) {
			logger.log(Level.WARNING, "Connection timed out", e);
		} finally {
			reader.close();
		}
		
		logger.info("Connection was closed");
		cxt.complete();
	}
	}
}
