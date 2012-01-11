package xml.eventbroker;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.logging.Logger;

public class EventDeliveryTask implements Runnable {


	private final String event;
	private final String url;

	public EventDeliveryTask(String url, String event) {
		this.event = event;
		this.url = url;
	}

	@Override
	public void run() {
		try {
			URL url = new URL(this.url);
			HttpURLConnection con = (HttpURLConnection) url.openConnection();
			con.setRequestMethod("POST");
			con.setDoOutput(true);
			con.setDoInput(false);
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
			
			con.getInputStream();
			int rCode;
			if( (rCode = con.getResponseCode()) != HttpURLConnection.HTTP_OK)
				Logger.getAnonymousLogger().warning("Service answered: "+rCode);
			
		} catch (IOException e) {
		}
	}

}
