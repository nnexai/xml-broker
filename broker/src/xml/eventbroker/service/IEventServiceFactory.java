package xml.eventbroker.service;

import org.w3c.dom.Element;

import xml.eventbroker.service.delivery.IHTTPDeliverer;

public interface IEventServiceFactory {
	public AbstractServiceEntry getServiceEntry(Element doc) throws InstantiationException;
	
	public AbstractServiceEntry getServiceEntry(String eventType, String id, Element doc)
			throws InstantiationException;
	
	public IHTTPDeliverer getHTTPDeliverer(Class<? extends IHTTPDeliverer> clazz);
}
