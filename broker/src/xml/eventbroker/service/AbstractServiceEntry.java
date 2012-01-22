package xml.eventbroker.service;

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

	public boolean requiresDOM() {
		return false;
	}
		
	public abstract void deliver(Object eventBody) throws IOException;
}
