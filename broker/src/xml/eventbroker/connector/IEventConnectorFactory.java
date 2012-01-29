package xml.eventbroker.connector;

import org.w3c.dom.Element;

import xml.eventbroker.connector.delivery.IHTTPDeliverer;

public interface IEventConnectorFactory {
	public AbstractServiceEntry getServiceEntry(Element doc) throws InstantiationException;
	
	public AbstractServiceEntry getServiceEntry(String eventType, String id, Element doc)
			throws InstantiationException;
	
	public IHTTPDeliverer getHTTPDeliverer(Class<? extends IHTTPDeliverer> clazz);
}
