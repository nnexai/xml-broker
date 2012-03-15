package xml.eventbroker;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import xml.eventbroker.connector.AbstractServiceEntry;
import xml.eventbroker.connector.DOMEventDescription;
import xml.eventbroker.connector.ServiceConnectorFactory;

/**
 * Servlet implementation class XMLEventBroker
 */
public class XMLEventBroker {
	private static final Logger logger = Logger.getAnonymousLogger();
	private static final boolean WAIT_FOR_DELIVERY = true;

	private DeliveryStatistics stats;

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

	public void init() throws ParserConfigurationException {
		Runtime runtime = Runtime.getRuntime();
		int nrOfProcessors = runtime.availableProcessors();
		int desiredThreads = Math.max(nrOfProcessors + 1, 1);
		System.out.println("Available cores: " + nrOfProcessors
				+ " allocating threadpool of size " + desiredThreads);
		pool = Executors.newFixedThreadPool(desiredThreads);
		// pool = Executors.newSingleThreadExecutor();

		stats = new DeliveryStatistics();

		factory = new ServiceConnectorFactory(pool, stats);
		factory.init();

		regServ = new RegisteredServices();
		regServ.registerService(ConfigLoader.getConfig(
				XMLEventBroker.class.getResource("config.xml"), factory));

		dynReg = new DynamicRegistration(regServ, factory);
		docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
	}

	public void destroy() {
		factory.shutdown();
		try {
			pool.shutdown();
			if (!pool.awaitTermination(4, TimeUnit.SECONDS))
				pool.shutdownNow();
		} catch (InterruptedException e) {
			logger.log(Level.WARNING, "Unable to shutdown Threadpool", e);
		}

	}

	public void processXML(final InputStream in) {

		EventParser evP = new EventParser() {

			@Override
			public void handleEvent(String eventType, String event) {
				DOMEventDescription domdescr = null;

				for (AbstractServiceEntry service : regServ
						.getServices(eventType)) {
					Object ev = event;

					try {
						if (service.requiresDOM()) {
							if (domdescr == null) {
								Document doc = docBuilder.newDocument();
								SAX2DomHandler.generateDOM(event, doc);
								domdescr = new DOMEventDescription(doc, event);
							}
							ev = domdescr;
						}
						// EventDeliveryTask task = new EventDeliveryTask(ev,
						// service);
						// pool.execute(task);
						stats.addDelivery();
						service.deliver(ev);

					} catch (SAXException e) {
						logger.log(Level.WARNING, "Unable to generate DOM", e);
					} catch (IOException e) {
						logger.log(Level.WARNING, "Could not deliver Event", e);
					}
				}

				// wait if sending-queue is to long
				if (WAIT_FOR_DELIVERY && stats.counter.get() > 6000)
					while (stats.counter.get() > 1) {
						try {
							Thread.sleep(50);
						} catch (InterruptedException e) {
						}
					}
			}
		};

		// Add a fake delivery s.th. stats.waitForPendingDeliveries does not
		// return before every Msg has been touched at least once
		stats.addDelivery();
		evP.parseStream(in);
		stats.finishedDelivery();

		try {
			in.close();
		} catch (IOException e) {
			logger.log(Level.WARNING, "Could not close request-stream", e);
		}
	}

	public boolean unsubscribe(String path) {
		return dynReg.unsubscribe(path);
	}

	public boolean subscribe(InputStream in, String path) {
		return dynReg.subscribe(in, path);
	}

	public void iterate(IRegisteredServiceHandler handler) {
		regServ.iterate(handler);
	}
}
