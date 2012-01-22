package xml.eventbroker.service.delivery;

import java.io.IOException;

public interface IHTTPDeliverer {
	void init();
	void shutdown();
	void deliver(String event, String urlString) throws IOException;
}
