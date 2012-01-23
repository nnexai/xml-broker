package xml.eventbroker.service;

import java.io.IOException;

import org.w3c.dom.Element;

import xml.eventbroker.service.delivery.IHTTPDeliverer;
import xml.eventbroker.service.delivery.PooledHTTPDeliverer;
import xml.eventbroker.service.delivery.PooledStreamingHTTPDeliverer;
import xml.eventbroker.service.delivery.SimpleHTTPDeliverer;

public class HTTPService extends AbstractServiceEntry {
	private final String url;
	private final IHTTPDeliverer deliverer; 
	
	public HTTPService(String event, String id, String url, boolean streaming, IEventServiceFactory fac) {
		super(event, id);
		this.url = url;
		
		//TODO: maybe put this responsibility inside the factory?.. getPooledDeliverer, getDeliverer etc.? 
		//Class<? extends IHTTPDeliverer> delivC = streaming ? PooledHTTPDeliverer.class : SimpleHTTPDeliverer.class;
		Class<? extends IHTTPDeliverer> delivC = streaming ? PooledStreamingHTTPDeliverer.class : PooledHTTPDeliverer.class;
		
		this.deliverer = fac.getHTTPDeliverer(delivC);
	}
	
	public HTTPService(String event, String id, Element xml, IEventServiceFactory fac) {
		this(event, id, xml.getAttribute("url"), "True".equals(xml.getAttribute("streaming")), fac);
	}

	@Override
	public void deliver(Object eventBody) throws IOException {
		deliverer.deliver((String)eventBody, url);
	}

}
