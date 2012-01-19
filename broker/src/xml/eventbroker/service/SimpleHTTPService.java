package xml.eventbroker.service;

import java.io.IOException;

import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import xml.eventbroker.service.delivery.SimpleHTTPDeliverer;

public class SimpleHTTPService extends AbstractServiceEntry {
	private final String url;
	
	public SimpleHTTPService(String event, String url) {
		super(event);
		this.url = url;
	}
	
	public SimpleHTTPService(String event, Element xml) {
		super(event);
		this.url = xml.getAttribute("url");
	}

	@Override
	public void deliver(String eventBody) throws IOException {
		SimpleHTTPDeliverer.deliver(eventBody, url);
	}

}
