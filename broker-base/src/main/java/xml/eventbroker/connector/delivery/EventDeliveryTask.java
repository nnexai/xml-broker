package xml.eventbroker.connector.delivery;

import java.io.IOException;
import java.net.URI;
import java.util.logging.Level;
import java.util.logging.Logger;


public class EventDeliveryTask implements Runnable {

	private static final Logger logger = Logger.getAnonymousLogger();

	private final String event;
	private final URI uri;
	private final IHTTPDeliverer deliverer;

	public EventDeliveryTask(String event, URI uri, IHTTPDeliverer deliverer) {
		this.event = event;
		this.uri = uri;
		this.deliverer = deliverer;
	}

	@Override
	public void run() {
		try {
			deliverer.deliver(event, uri);
		} catch (IOException e) {
			logger.log(Level.WARNING,
					"Error during transmission of xml-events", e);
		}

	}

}
