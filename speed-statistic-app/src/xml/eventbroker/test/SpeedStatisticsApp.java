package xml.eventbroker.test;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import xml.eventbroker.shared.MultiXMLRootFilter;

public class SpeedStatisticsApp extends HttpServlet {

	private static final long serialVersionUID = 1L;
	private static final Logger logger = Logger.getAnonymousLogger();

	List<Long> events;
	XMLInputFactory f;

	@Override
	public void init() throws ServletException {
		super.init();
		f = XMLInputFactory.newInstance();
		f.setProperty(XMLInputFactory.IS_COALESCING, Boolean.FALSE);

		events = new LinkedList<Long>();
	}
	
	boolean changed = true;
	Statistics statistics;

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {

		if(changed)
			synchronized (events) {
				statistics = new Statistics(events);	
				changed = false;
			}
		
		String[] paths = null;
		if (req.getPathInfo() != null)
			paths = req.getPathInfo().split("/", 2);

		if (paths != null && paths.length == 2
				&& "statistics.svg".equals(paths[1])) {

			req.setAttribute("statistic", statistics);
			javax.servlet.RequestDispatcher rd = this.getServletContext()
					.getRequestDispatcher("/time_diff_line.jsp");
			rd.forward(req, resp);

		} else if (paths == null || paths.length == 1) {
			req.setAttribute("statistic", statistics);
			javax.servlet.RequestDispatcher rd = this.getServletContext()
					.getRequestDispatcher("/statistics.jsp");
			rd.forward(req, resp);
		} else
			resp.sendError(HttpServletResponse.SC_NOT_FOUND);
	}

	private void registerEvent(long timeDiff) {
		synchronized (events) {
			changed = true;
			events.add(Long.valueOf(timeDiff));
		}
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		readXMLEvents(req.getReader());
	}

	private void parseStream(Reader in) throws XMLStreamException {
		XMLStreamReader r;

		r = f.createXMLStreamReader(in);

		long timeDiff;
		int level = 0;

		while (r.hasNext()) {
			switch (r.next()) {

			case XMLStreamConstants.START_ELEMENT:
				if (level == 0) {
					String tStr = r.getAttributeValue(null, "send-time");
					// store diff in milliseconds
					timeDiff = (System.nanoTime() - Long.valueOf(tStr)) / 1000000;
					registerEvent(timeDiff);
				}
				level++;
				break;
			case XMLStreamConstants.END_ELEMENT:
				level--;
				break;
			default:
			}
		}

	}

	private void readXMLEvents(Reader reader) throws IOException {
		MultiXMLRootFilter filter = new MultiXMLRootFilter(reader, 0x100);
		try {

			while (!filter.hasFinished()) {
				parseStream(filter);
			}
			filter.forceClose();
		} catch (SocketException e) {
			logger.log(Level.WARNING, "Connection timed out", e);
		} catch (IOException e) {
			logger.log(Level.WARNING, "Connection timed out", e);
		} catch (XMLStreamException e) {
			logger.log(Level.WARNING, "Error during parse", e);
		} finally {
			reader.close();
		}
	}
}
