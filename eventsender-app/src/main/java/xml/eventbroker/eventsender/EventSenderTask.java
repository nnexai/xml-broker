package xml.eventbroker.eventsender;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.logging.Logger;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

public class EventSenderTask implements Runnable {

	private String eventBrokerURL;
	private String events;
	
	public EventSenderTask(String eventBrokerURL, String events) {
		this.events = events;
		this.eventBrokerURL = eventBrokerURL;
	}

	@Override
	public void run() {
		
		try {
			SAXBuilder builder = new SAXBuilder();
			Document eventdoc = builder.build(new StringReader(events));
			Element root= eventdoc.getRootElement();			 		
			List<Element> eventelements = root.getChildren();
			
			for (Element event : eventelements) {
				if(event.getName().equals("wait")) {
                	System.out.println("waiting...");
                	try {
                		Thread.sleep(Long.parseLong(event.getAttributeValue("time")));
                	} catch (Exception e) {
                		e.printStackTrace();
                	}	                	
				} else {
					XMLOutputter outputter = new XMLOutputter(Format.getPrettyFormat());
					String xmlString = outputter.outputString(event);
					send(xmlString);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

	}
	
	private void send(String event) {		
		System.out.println("sending event: " + event);
		
		try {
			URL url = new URL(eventBrokerURL);
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
			
			writer.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>" + event);
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