package xml.eventbroker;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import xml.eventbroker.connector.AbstractServiceEntry;

public class EventDeliveryTask implements Runnable {

	private static final Logger logger = Logger.getAnonymousLogger();

	private final Object event;
	private final AbstractServiceEntry service;

	public EventDeliveryTask( Object event, AbstractServiceEntry service) {
		this.event = event;
		this.service = service;
	}

	@Override
	public void run() {
		try {
			service.deliver(event);
		} catch (IOException e) {
			logger.log(Level.WARNING, "Error during transmission of xml-events", e);
		}
			
	}

}
