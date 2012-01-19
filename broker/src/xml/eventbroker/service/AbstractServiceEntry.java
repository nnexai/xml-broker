package xml.eventbroker.service;

import java.io.IOException;

public abstract class AbstractServiceEntry {
	private final String event;
	
	public AbstractServiceEntry(String event) {
		this.event = event;
	}

	public String getEvent() {
		return event;
	}

	public abstract void deliver(String eventBody) throws IOException;
}
