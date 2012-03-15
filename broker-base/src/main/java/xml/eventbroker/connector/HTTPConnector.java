package xml.eventbroker.connector;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import org.w3c.dom.Element;

import xml.eventbroker.connector.delivery.AbstractHTTPDeliverer;

public class HTTPConnector extends AbstractServiceEntry {
	private final URI url;
	private final AbstractHTTPDeliverer deliverer;

	public HTTPConnector(String event, String uri, String url, String type,
			IEventConnectorFactory fac) throws URISyntaxException {
		super(event, uri);
		this.url = new URI(url);
		this.deliverer = fac.getHTTPDeliverer(type);
	}

	public HTTPConnector(String event, String uri, Element xml,
			IEventConnectorFactory fac) throws URISyntaxException {
		this(event, uri, xml.getAttribute("url"), xml.getAttribute("type"), fac);
	}

	@Override
	public void deliver(Object eventBody) throws IOException {
		deliverer.enqueue((String) eventBody, url);
	}

	@Override
	public String toString() {
		return url.toString() + " > " + deliverer;
	}
}
