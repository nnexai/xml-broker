package xml.eventbroker.client;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.Map;

public class XMLEventBrokerClient {

	static final Map<String, String> config;
	
	static {
		config = ConfigLoader.getConfig(XMLEventBrokerClient.class.getResource("config.xml"));
	}
	
	static class SimpleEventWriter extends Thread {
		final OutputStream stream;

		public SimpleEventWriter(OutputStream out)
				throws UnsupportedEncodingException {
			super();
			this.stream = out;
		}

		/**
		 * Fast copying of Data from one Channel into another.
		 * @see http://thomaswabner.wordpress.com/2007/10/09/fast-stream-copy-using-javanio-channels/
		 */
		public static void fastChannelCopy(final ReadableByteChannel src,
				final WritableByteChannel dest) throws IOException {
			final ByteBuffer buffer = ByteBuffer.allocateDirect(16 * 1024);
			while (src.read(buffer) != -1) {
				// prepare the buffer to be drained
				buffer.flip();
				// write to the channel, may block
				dest.write(buffer);
				// If partial transfer, shift remainder down
				// If buffer is empty, same as doing clear()
				buffer.compact();
			}
			// EOF will leave buffer in fill state
			buffer.flip();
			// make sure the buffer is fully drained.
			while (buffer.hasRemaining()) {
				dest.write(buffer);
			}
		}

		@Override
		public void run() {

			InputStream res = XMLEventBrokerClient.class
					.getResourceAsStream( config.get("resource") );

			ReadableByteChannel rbc = Channels.newChannel(res);
			WritableByteChannel wbc = Channels.newChannel(stream);

			try {
				fastChannelCopy(rbc, wbc);
				stream.flush();
				stream.close();
			} catch (IOException e) {
				e.printStackTrace();
			}

		}
	}
	
	public static void main(String[] args) {
		URL url;

		try {
			url = new URL( config.get("url") );
			HttpURLConnection con = (HttpURLConnection) url.openConnection();
			con.setRequestMethod("POST");
			con.setDoOutput(true);
			con.setDoInput(true);
			con.setChunkedStreamingMode(-1);
			con.setRequestProperty("Connection", "keep-alive");
			con.setConnectTimeout(120 * 1000);
			con.setRequestProperty("Content-type", "text/xml");

			con.connect();

			System.out.println(con.getRequestMethod());

			SimpleEventWriter w = new SimpleEventWriter(con.getOutputStream());
			w.run();
			
			con.getInputStream();
			System.out.println("Response: "+con.getResponseMessage());
			con.disconnect();

		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (ProtocolException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
