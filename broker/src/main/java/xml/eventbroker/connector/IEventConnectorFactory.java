package xml.eventbroker.connector;

import org.w3c.dom.Element;

import xml.eventbroker.connector.delivery.IHTTPDeliverer;

public interface IEventConnectorFactory {
	public AbstractServiceEntry getServiceEntry(Element doc, String uri) throws InstantiationException;
	
	public AbstractServiceEntry getServiceEntry(String eventType, String uri, Element doc)
			throws InstantiationException;
	
	public IHTTPDeliverer getHTTPDeliverer(String type);
}
