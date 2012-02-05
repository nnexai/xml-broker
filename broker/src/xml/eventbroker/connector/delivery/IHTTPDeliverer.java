package xml.eventbroker.connector.delivery;

import java.io.IOException;
import java.net.URI;

public interface IHTTPDeliverer {
	void init();
	void shutdown();
	void deliver(String event, URI urlString) throws IOException;
}
