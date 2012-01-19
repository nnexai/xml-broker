package xml.eventbroker;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.ParserConfigurationException;

import xml.eventbroker.service.AbstractServiceEntry;

/**
 * Servlet implementation class FirstServlet
 */
public class XMLEventBroker extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private static final Logger logger = Logger.getAnonymousLogger();

	/**
	 * @see HttpServlet#HttpServlet()
	 */
	public XMLEventBroker() {
		super();
	}

	ExecutorService pool;
	RegisteredServices regServ;
	DynamicRegistration dynReg;

	@Override
	public void init() throws ServletException {
		super.init();
		pool = Executors.newCachedThreadPool();
		regServ = new RegisteredServices();
		regServ.registerService(ConfigLoader.getConfig(XMLEventBroker.class
				.getResource("config.xml")));
		try {
			dynReg = new DynamicRegistration(regServ);
		} catch (ParserConfigurationException e) {
			throw new ServletException(e);
		}
	}

	@Override
	public void destroy() {
		super.destroy();
		try {
			pool.shutdown();
			if (!pool.awaitTermination(4, TimeUnit.SECONDS))
				pool.shutdownNow();
		} catch (InterruptedException e) {
			logger.log(Level.WARNING, "Unable to shutdown Threadpool", e);
		}

	}

	private void processXML(final InputStream in) {

		EventParser evP = new EventParser() {
			@Override
			public void handleEvent(String eventType, String event) {

				for (AbstractServiceEntry service : regServ.getServices(eventType)) {
					EventDeliveryTask task = new EventDeliveryTask(event, service);
					pool.execute(task);
				}
			}
		};

		evP.parseStream(in);

		try {
			in.close();
		} catch (IOException e) {
			logger.log(Level.WARNING, "Could not close request-stream", e);
		}
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		final BufferedInputStream inStream = new BufferedInputStream(
				req.getInputStream());		
		processXML(inStream);
		resp.setStatus(HttpServletResponse.SC_OK);
	}
	
	@Override
	protected void doPut(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		dynReg.subscribe(req.getInputStream());
		resp.setStatus(HttpServletResponse.SC_OK);
	}
	
	@Override
	protected void doDelete(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		dynReg.unsubscribe(req.getPathInfo());
		resp.setStatus(HttpServletResponse.SC_OK);
	}
}
