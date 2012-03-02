package xml.eventbroker;

import java.io.IOException;
import java.io.StringReader;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.ProcessingInstruction;
import org.w3c.dom.Text;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * SAXHandler that converts a sequence of sax-events into a dom-tree
 */
public class SAX2DomHandler extends DefaultHandler {
	private static final Logger logger = Logger.getAnonymousLogger();
	private final Document doc;
	private Node current;

	public SAX2DomHandler(Document doc) {
		this.doc = doc;
		this.current = doc;
	}

	@Override
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

	@Override
	public void endElement(String uri, String name, String qName) {
		current = current.getParentNode();
	}

	@Override
	public void characters(char[] ch, int start, int length) {
		String str = new String(ch, start, length);
		if(length > 0) {
			Text text = doc.createTextNode(str);
			current.appendChild(text);
		}
	}

	@Override
	public void processingInstruction(String target, String data) {
		ProcessingInstruction pi = doc
				.createProcessingInstruction(target, data);
		current.appendChild(pi);
	}

	public static void generateDOM(String xml, Document doc) throws SAXException {
		SAXParserFactory fact = SAXParserFactory.newInstance();
		try {
			SAXParser parser = fact.newSAXParser();
			SAX2DomHandler handler = new SAX2DomHandler(doc);
			
			parser.parse(new InputSource(new StringReader(xml)), handler);
		} catch (ParserConfigurationException e) {
			logger .log(Level.SEVERE, "Error configuring parser", e);
		} catch (SAXException e) {
			logger.log(Level.SEVERE, "Error parsing configuration file", e);
			throw e;
		} catch (IOException e) {
			logger.log(Level.SEVERE, "Error opening configuration file", e);
		}
	}
}
