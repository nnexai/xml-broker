package xml.eventbroker.connector.delivery;

import java.io.IOException;
import java.net.URI;
import java.util.concurrent.ExecutorService;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.util.EntityUtils;

import xml.eventbroker.DeliveryStatistics;

public class PooledHTTPDeliverer extends AbstractHTTPDeliverer {
	public PooledHTTPDeliverer(ExecutorService pool) {
		super(pool);
	}

	private HttpClient httpClient;
	private ClientConnectionManager cm;
	private ResponseHandler<byte[]> h;
	DeliveryStatistics stats;

	@Override
	public void init(DeliveryStatistics stats) {
		this.stats = stats;
		/*
		 * SchemeRegistry schemeRegistry = new SchemeRegistry();
		 * schemeRegistry.register(new Scheme("http", 80, PlainSocketFactory
		 * .getSocketFactory()));
		 */
		ThreadSafeClientConnManager tcm = new ThreadSafeClientConnManager(/* schemeRegistry */);
		tcm.setMaxTotal(100);
		tcm.setDefaultMaxPerRoute(4);
		cm = tcm;

		httpClient = new DefaultHttpClient(tcm);

		h = new ResponseHandler<byte[]>() {
			@Override
			public byte[] handleResponse(HttpResponse response)
					throws ClientProtocolException, IOException {
				HttpEntity entity = response.getEntity();
				if (entity != null) {
					return EntityUtils.toByteArray(entity);
				} else {
					return null;
				}
			}
		};

	}

	@Override
	public void shutdown() {
		cm.shutdown();
	}

	@Override
	public void deliver(String event, URI url) throws IOException {
		HttpPost httpPost = new HttpPost(url);
		HttpEntity entity = new StringEntity(event);
		httpPost.setEntity(entity);
		httpClient.execute(httpPost, h);
		stats.finishedDelivery();
	}

	@Override
	public String toString() {
		return "[pooled-http-request]";
	}
}
