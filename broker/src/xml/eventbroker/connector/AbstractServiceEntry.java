package xml.eventbroker.connector;

import java.io.IOException;

public abstract class AbstractServiceEntry {
	private final String event, id;
	
	public AbstractServiceEntry(String event, String id) {
		this.event = event;
		this.id = id;
	}

	public String getEvent() {
		return event;
	}
	
	public String getID() {
		return id;
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
