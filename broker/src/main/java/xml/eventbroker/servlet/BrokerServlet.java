package xml.eventbroker.servlet;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.ParserConfigurationException;

import xml.eventbroker.IRegisteredServiceHandler;
import xml.eventbroker.XMLEventBroker;
import xml.eventbroker.connector.AbstractServiceEntry;

/**
 * Servlet implementation class XMLEventBroker
 */
public class BrokerServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private static final Logger logger = Logger.getAnonymousLogger();

	private static final XMLEventBroker broker = new XMLEventBroker();

	@Override
	public void init() throws ServletException {
		try {
			broker.init();
		} catch (ParserConfigurationException e) {
			new ServletException(e);
		}
	}

	@Override
	public void destroy() {
		broker.destroy();
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		final BufferedInputStream inStream = new BufferedInputStream(
				req.getInputStream());
		broker.processXML(inStream);
		resp.setStatus(HttpServletResponse.SC_OK);
	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		final PrintWriter writer = resp.getWriter();
		resp.setContentType("text/html");
		writer.append("<html><body>");
		writer.append("<table>");
		writer.append("<tr><th>Event</th><th>URI</th><th>Service</th></tr>");
		broker.iterate(new IRegisteredServiceHandler() {
			boolean first = true;

			@Override
			public void handleService(String key, AbstractServiceEntry srvEntry) {
				if (first) {
					first = false;
				} else {
					writer.append("<tr><td></td>");
				}

				writer.append("<td>" + srvEntry.getURI() + "</td><td>"
						+ srvEntry + "</td></tr>");
			}

			@Override
			public void handleEventType(String key) {
				first = true;
				writer.append("<tr><td>" + key + "</td>");
			}
		});
		writer.append("</table>");
		writer.append("</body></html>");
		writer.close();
		resp.setStatus(HttpServletResponse.SC_OK);
	}

	@Override
	protected void doPut(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		boolean success = broker.subscribe(req.getInputStream(),
				req.getPathInfo());
		resp.setStatus(success ? HttpServletResponse.SC_OK
				: HttpServletResponse.SC_NOT_ACCEPTABLE);
	}

	@Override
	protected void doDelete(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		boolean success = broker.unsubscribe(req.getPathInfo());
		resp.setStatus(success ? HttpServletResponse.SC_OK
				: HttpServletResponse.SC_NOT_ACCEPTABLE);
	}
}
