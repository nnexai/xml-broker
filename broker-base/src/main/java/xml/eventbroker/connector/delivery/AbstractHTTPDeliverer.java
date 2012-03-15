package xml.eventbroker.connector.delivery;

import java.io.IOException;
import java.net.URI;
import java.util.concurrent.ExecutorService;

import xml.eventbroker.DeliveryStatistics;

public abstract class AbstractHTTPDeliverer {
	private final ExecutorService pool;

	public AbstractHTTPDeliverer(ExecutorService pool) {
		this.pool = pool;
	}

	public abstract void init(DeliveryStatistics stats);

	public abstract void shutdown();

	public void enqueue(String event, URI uri) {
		pool.execute(new EventDeliveryTask(event, uri, this));
	}

	protected abstract void deliver(String event, URI urlString)
			throws IOException;
}
