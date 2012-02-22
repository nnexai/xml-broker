package xml.eventbroker.connector;

import java.io.IOException;
import java.util.concurrent.ExecutorService;

public abstract class AbstractServiceEntry {
	private final String event, uri;
	
	public AbstractServiceEntry(String event, String uri) {
		this.event = event;
		this.uri = uri;
	}

	public String getEvent() {
		return event;
	}
	
	public String getURI() {
		return uri;
	}

	/**
	 * Test if this implementation of AbstractServiceEntry requires a DOM-Tree.
	 * @return true, if deliver requires a DOM-Node. false, if a simple String representation if needed.
	 */
	public boolean requiresDOM() {
		return false;
	}
		
	public abstract void deliver(Object eventBody) throws IOException;
}
