package xml.eventbroker.connector;

import java.io.IOException;

import org.w3c.dom.Element;

import xml.eventbroker.connector.delivery.IHTTPDeliverer;
import xml.eventbroker.connector.delivery.PooledHTTPDeliverer;
import xml.eventbroker.connector.delivery.PooledStreamingHTTPDeliverer;
import xml.eventbroker.connector.delivery.SimpleHTTPDeliverer;

public class HTTPConnector extends AbstractServiceEntry {
	private final String url;
	private final IHTTPDeliverer deliverer; 
	
	public HTTPConnector(String event, String id, String url, boolean streaming, IEventConnectorFactory fac) {
		super(event, id);
		this.url = url;
		
		//TODO: maybe put this responsibility inside the factory?.. getPooledDeliverer, getDeliverer etc.? 
		//Class<? extends IHTTPDeliverer> delivC = streaming ? PooledHTTPDeliverer.class : SimpleHTTPDeliverer.class;
		Class<? extends IHTTPDeliverer> delivC = streaming ? PooledStreamingHTTPDeliverer.class : PooledHTTPDeliverer.class;
		
		this.deliverer = fac.getHTTPDeliverer(delivC);
	}
	
	public HTTPConnector(String event, String id, Element xml, IEventConnectorFactory fac) {
		this(event, id, xml.getAttribute("url"), "True".equals(xml.getAttribute("streaming")), fac);
	}

	@Override
	public void deliver(Object eventBody) throws IOException {
		deliverer.deliver((String)eventBody, url);
	}

}
