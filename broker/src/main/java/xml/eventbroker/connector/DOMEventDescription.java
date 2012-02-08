package xml.eventbroker.connector;

import org.w3c.dom.Document;

public class DOMEventDescription {
	public final Document doc;
	public final String eventString;
	public DOMEventDescription(Document doc, String eventString) {
		super();
		this.doc = doc;
		this.eventString = eventString;
	}
}
