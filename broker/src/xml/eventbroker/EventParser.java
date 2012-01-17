package xml.eventbroker;

import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.events.XMLEvent;

public abstract class EventParser {

	private static final Logger logger = Logger.getAnonymousLogger();
	
	public abstract void handleEvent(String eventType, String event);

	public void parseStream(InputStream in) {

		XMLInputFactory f = XMLInputFactory.newInstance();
		XMLStreamReader r;

		try {
			r = f.createXMLStreamReader(in, "utf-8");

			int level = 0;

			String name = null;
			StringBuilder event = new StringBuilder(0x1000);

			while (r.hasNext()) {
				r.next();

				switch (r.getEventType()) {

				case XMLEvent.START_ELEMENT:
					level++;

					// store top-level event information
					if (level == 2)
						name = r.getLocalName();

					if (level >= 2) { // Top-Level-Event
						event.append('<').append(r.getLocalName());

						// get all attributes
						if (r.getAttributeCount() > 0) {
							for (int i = 0; i < r.getAttributeCount(); i++) {
								event.append(' ')
										.append(r.getAttributeLocalName(i))
										.append("=\"");
								event.append(r.getAttributeValue(i)).append(
										'\"');
							}
						}

						event.append('>');
					}
					break;

				case XMLEvent.END_ELEMENT:
					level--;

					if (level >= 1)
						event.append("</").append(r.getLocalName()).append('>');

					if (level == 1) { // SEND!
						String ev = event.toString();
						handleEvent(name, ev);
						event.setLength(0); // reset String
					}
					break;

				case XMLEvent.CHARACTERS:
					if (level >= 2) {
						int start = r.getTextStart();
						int length = r.getTextLength();
						String text = new String(r.getTextCharacters(), start,
								length);
						event.append(text);
					}
				default:
					break;
				}
			}

		} catch (XMLStreamException e) {
			logger.log(Level.WARNING, "Exception during parsing of incoming XML-Event-Stream", e);
		}
	}

}
