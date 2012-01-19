package xml.eventbroker;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.ProcessingInstruction;
import org.w3c.dom.Text;
import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;

/**
 * SAXHandler that converts a sequence of sax-events into a dom-tree
 */
public class SAX2DomHandler extends DefaultHandler {
	private final Document doc;
	private Node current;

	public SAX2DomHandler(Document doc) {
		this.doc = doc;
		this.current = doc;
	}

	public void startElement(String uri, String name, String qName,
			Attributes attrs) {
		Element elem = doc.createElementNS(uri, qName);

		// Add attributes.
		for (int i = 0; i < attrs.getLength(); ++i) {
			String ns_uri = attrs.getURI(i);
			String qname = attrs.getQName(i);
			String value = attrs.getValue(i);
			Attr attr = doc.createAttributeNS(ns_uri, qname);
			attr.setValue(value);
			elem.setAttributeNodeNS(attr);
		}

		current.appendChild(elem);
		current = elem;
	}

	public void endElement(String uri, String name, String qName) {
		current = current.getParentNode();
	}

	public void characters(char[] ch, int start, int length) {
		String str = new String(ch, start, length);
		if(length > 0) {
			Text text = doc.createTextNode(str);
			current.appendChild(text);
		}
	}

	public void processingInstruction(String target, String data) {
		ProcessingInstruction pi = doc
				.createProcessingInstruction(target, data);
		current.appendChild(pi);
	}

}
