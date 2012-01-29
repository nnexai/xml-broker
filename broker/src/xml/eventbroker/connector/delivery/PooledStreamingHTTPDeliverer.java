package xml.eventbroker.connector.delivery;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

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

import sun.net.www.protocol.http.HttpURLConnection;

public class PooledStreamingHTTPDeliverer implements IHTTPDeliverer {

	private static final Logger logger = Logger.getAnonymousLogger();

	static class PersistentConnection {

		private HttpURLConnection con;
		private OutputStreamWriter writer;

		private final URL url;

		public URL getURL() {
			return url;
		}

		private void connect() throws IOException {
			logger.info("Connecting to "+url.toString());
			
			con = (HttpURLConnection) url.openConnection();

			con.setRequestMethod("POST");
			con.setDoOutput(true);
			con.setDoInput(false);
			con.setChunkedStreamingMode(-1);
			con.setRequestProperty("Connection", "keep-alive");
			con.setConnectTimeout(60 * 60 * 1000); // ONE-Hour
			con.setRequestProperty("Content-type", "text/xml");
			
			OutputStream out = con.getOutputStream();
			BufferedOutputStream bos = new BufferedOutputStream(out);
			writer = new OutputStreamWriter(bos, "UTF-8");
		}

		public synchronized void disconnect() throws IOException {
			if (con != null) {
				con.getInputStream();
				int rCode;
				if ((rCode = con.getResponseCode()) != HttpURLConnection.HTTP_OK)
					logger.warning("Persistent connection <" + url.toString()
							+ "> closed: Service answered: " + rCode);
				con.disconnect();
				con = null;
			}

		}

		public PersistentConnection(String urlString) throws IOException {
			url = new URL(urlString);
			connect();
		}

		public synchronized void pushEvent(String event) throws IOException {
			try {
				writer.append(event);
				writer.flush();
			} catch (IOException e) {
				connect();
				writer.append(event);
				writer.flush();
			} 
		}
	}

	Map<String, PersistentConnection> map;

	@Override
	public void init() {
		map = Collections
				.synchronizedMap(new LinkedHashMap<String, PersistentConnection>() {

				});
	}

	@Override
	public void shutdown() {
		for (PersistentConnection con : map.values()) {
			try {
				con.disconnect();
			} catch (IOException e) {
				logger.log(Level.WARNING,
						"Could not disconnect from " + con.getURL(), e);
			}
		}
	}

	@Override
	public void deliver(String event, String urlString) throws IOException {
		PersistentConnection con;
		synchronized (map) {
			if ((con = map.get(urlString)) == null) {
				con = new PersistentConnection(urlString);
				map.put(urlString, con);
			}
		}
		con.pushEvent(event);
	}
}
