package xml.eventbroker.connector.delivery;

import java.io.IOException;

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

public class PooledHTTPDeliverer implements IHTTPDeliverer {
	private HttpClient httpClient;
	private ClientConnectionManager cm;
	private ResponseHandler<byte[]> h;

	@Override
	public void init() {
		/*SchemeRegistry schemeRegistry = new SchemeRegistry();
		schemeRegistry.register(new Scheme("http", 80, PlainSocketFactory
				.getSocketFactory()));*/
		ThreadSafeClientConnManager tcm = new ThreadSafeClientConnManager(/*schemeRegistry*/);
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
	public void deliver(String event, String urlString) throws IOException {
		HttpPost httpPost = new HttpPost(urlString);
		HttpEntity entity = new StringEntity(event);
		httpPost.setEntity(entity);
		byte[] response = httpClient.execute(httpPost, h);
		
	}
}
