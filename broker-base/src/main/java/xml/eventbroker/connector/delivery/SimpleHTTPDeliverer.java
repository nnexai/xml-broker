package xml.eventbroker.connector.delivery;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.logging.Logger;

import xml.eventbroker.DeliveryStatistics;

public class SimpleHTTPDeliverer extends AbstractHTTPDeliverer {
	public SimpleHTTPDeliverer(ExecutorService pool) {
		super(pool);
	}

	private static final Logger logger = Logger.getAnonymousLogger();

	@Override
	public void deliver(String event, URI urlString) throws IOException {
		URL url = urlString.toURL();
		HttpURLConnection con = (HttpURLConnection) url.openConnection();
		con.setRequestMethod("POST");
		con.setDoOutput(true);
		con.setDoInput(true);
		con.setChunkedStreamingMode(-1);
		con.setRequestProperty("Connection", "keep-alive");
		con.setConnectTimeout(120 * 1000);
		con.setRequestProperty("Content-type", "text/xml");

		OutputStream out = con.getOutputStream();
		BufferedOutputStream bos = new BufferedOutputStream(out);
		OutputStreamWriter writer = new OutputStreamWriter(bos, "UTF-8");

		writer.append(event);
		writer.flush();
		writer.close();

		stats.finishedDelivery();

		con.getInputStream();
		int rCode;
		if ((rCode = con.getResponseCode()) != HttpURLConnection.HTTP_OK)
			logger.warning("Service answered: " + rCode);
	}

	DeliveryStatistics stats;

	@Override
	public void init(DeliveryStatistics stats) {
		this.stats = stats;
	}

	@Override
	public void shutdown() {
	}

	@Override
	public String toString() {
		return "[simple-http-request]";
	}
}
