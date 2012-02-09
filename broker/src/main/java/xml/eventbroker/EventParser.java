package xml.eventbroker;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import xml.eventbroker.shared.MultiXMLRootFilter;

public abstract class EventParser {

	private static final Logger logger = Logger.getAnonymousLogger();

	public abstract void handleEvent(String eventType, String event);

	public void parseStream(InputStream in) {
		MultiXMLRootFilter filter;
		try {
			filter = new MultiXMLRootFilter(new InputStreamReader(in, "UTF-8"),
					0x100);

			char buf[] = new char[0x100];
			StringBuilder str = new StringBuilder(100);
			int r = 0;

			try {
				while (filter.hasNext()) {
					str.setLength(0);
					while ((r = filter.read(buf)) >= 0)
						str.append(buf, 0, r);

					String name = filter.getCurrentRootName();
					String ev = str.toString();
					handleEvent(name, ev);
				}
			} catch (IOException e) {
				logger.log(Level.WARNING, "Error during parsing of Event-Stream", e);
			}
		} catch (UnsupportedEncodingException e) {
			logger.log(Level.SEVERE, "Failed to decode stream to UTF-8", e);
		}
	}

}
