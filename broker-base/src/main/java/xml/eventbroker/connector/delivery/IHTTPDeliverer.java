package xml.eventbroker.connector.delivery;

import java.io.IOException;
import java.net.URI;

import xml.eventbroker.DeliveryStatistics;

public interface IHTTPDeliverer {
	void init(DeliveryStatistics stats);
	void shutdown();
	void deliver(String event, URI urlString) throws IOException;
}
