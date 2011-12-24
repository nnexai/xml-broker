package xml.eventbroker.client;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.events.XMLEvent;

public class XMLEventBrokerClient {

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
					.getResourceAsStream("events.xml");

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
	
	static class StaxEventWriter extends Thread {
		final OutputStreamWriter out;
		final OutputStream stream;

		public StaxEventWriter(OutputStream out)
				throws UnsupportedEncodingException {
			super();
			this.stream = out;
			this.out = new OutputStreamWriter(out, "utf-8");
		}

		private void send(String str) {
			try {
				System.out.println(">: " + str);
				out.append(str);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		@Override
		public void run() {
			
			int no_of_events = 0;
			
			URL res = XMLEventBrokerClient.class.getResource("events.xml");
			XMLInputFactory f = XMLInputFactory.newInstance();

			XMLStreamReader r;
			try {
				
				r = f.createXMLStreamReader(res.openStream());

				if (r.hasNext())
					send("<?xml version=\"1.0\" encoding=\"utf-8\"?><events>");

				int level = 0;
				StringBuilder event = new StringBuilder(0x1000);

				while (r.hasNext()) {
					
					r.next();

					switch (r.getEventType()) {
					case XMLEvent.END_DOCUMENT:
						send("</events>");
						break;

					case XMLEvent.START_ELEMENT:
						level++;

						if (level >= 2) { // Top-Level-Event
							event.append('<').append(r.getLocalName());

							// get all attributes
							if (r.getAttributeCount() > 0) {
								for (int i = 0; i < r.getAttributeCount(); i++) {
									event.append(' ')
											.append(r.getAttributeLocalName(i))
											.append("=\"");
									event.append(r.getAttributeValue(i))
											.append('\"');
								}

							}

							event.append('>');
						}
						break;

					case XMLEvent.END_ELEMENT:
						level--;

						if (level >= 1)
							event.append("</").append(r.getLocalName())
									.append('>');

						if (level == 1) { // SEND!

							String ev = event.toString();
							send(ev);
							
							if(++no_of_events % 10 == 0) {
								System.out.printf("Parsed %d Events\n", no_of_events);
							}
							
							event.setLength(0);
						}
						break;

					case XMLEvent.CHARACTERS:
						if (level >= 2) {
							int start = r.getTextStart();
							int length = r.getTextLength();
							String text = new String(r.getTextCharacters(),
									start, length);
							event.append(text);
						}
					default:
						break;
					}
				}

			} catch (XMLStreamException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}

			try {
				out.flush();
				out.close();
			} catch (IOException e) {
				e.printStackTrace();
			}

		}
	}

	public static void main(String[] args) {
		URL url;

		try {
			url = new URL("http://localhost:8080/servlet-web/XMLEventBroker");
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
