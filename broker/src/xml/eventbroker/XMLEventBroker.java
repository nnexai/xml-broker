package xml.eventbroker;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import xml.eventbroker.connector.AbstractServiceEntry;
import xml.eventbroker.connector.ServiceConnectorFactory;

/**
 * Servlet implementation class XMLEventBroker
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
	DocumentBuilder docBuilder;

	ServiceConnectorFactory factory;

	@Override
	public void init() throws ServletException {
		super.init();
		pool = Executors.newCachedThreadPool();

		factory = new ServiceConnectorFactory();
		factory.init();

		regServ = new RegisteredServices();
		regServ.registerService(ConfigLoader.getConfig(
				XMLEventBroker.class.getResource("config.xml"), factory));
		try {
			dynReg = new DynamicRegistration(regServ, factory);
			docBuilder = DocumentBuilderFactory.newInstance()
					.newDocumentBuilder();
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

		factory.shutdown();

	}

	private void processXML(final InputStream in) {

		EventParser evP = new EventParser() {

			@Override
			public void handleEvent(String eventType, String event) {

				Document doc = null;

				for (AbstractServiceEntry service : regServ
						.getServices(eventType)) {
					Object ev = event;

					try {
						if (service.requiresDOM()) {
							if (doc == null) {
								doc = docBuilder.newDocument();
								SAX2DomHandler.generateDOM(event, doc);
							}
							ev = doc;
						}

						EventDeliveryTask task = new EventDeliveryTask(ev,
								service);
						pool.execute(task);
					} catch (SAXException e) {
						e.printStackTrace();
					}
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
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		final PrintWriter writer = resp.getWriter();
		writer.append("<html><body>");
		writer.append("<table>");
		writer.append("<tr><th>Event</th><th>ID</th><th>Service</th></tr>");
		regServ.iterate(new IRegisteredServiceHandler() {
			boolean first = true;
			
			@Override
			public void handleService(String key, AbstractServiceEntry srvEntry) {
				if(first){
					first = false;
				} else {
					writer.append("<tr><td></td>");					
				}
				
				writer.append("<td>"+srvEntry.getID()+"</td><td>"+srvEntry+"</td></tr>");										
			}
			
			@Override
			public void handleEventType(String key) {
				first = true;
				writer.append("<tr><td>"+key+"</td>");
			}
		});
		writer.append("</table>");
		writer.append("</body></html>");
		writer.close();
		resp.setStatus(resp.SC_OK);
	}

	@Override
	protected void doPut(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		boolean success = dynReg.subscribe(req.getInputStream());
		resp.setStatus(success ? HttpServletResponse.SC_OK
				: HttpServletResponse.SC_NOT_ACCEPTABLE);
	}

	@Override
	protected void doDelete(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		boolean success = dynReg.unsubscribe(req.getPathInfo());
		resp.setStatus(success ? HttpServletResponse.SC_OK
				: HttpServletResponse.SC_NOT_ACCEPTABLE);
	}
}
