package xml.eventbroker;

import java.util.Collection;
import java.util.LinkedList;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.xml.sax.Attributes;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import xml.eventbroker.service.AbstractServiceEntry;
import xml.eventbroker.service.IEventServiceFactory;

public class ConfigLoaderHandler extends DefaultHandler {
	
	private static final Logger logger = Logger.getAnonymousLogger();

	private final IEventServiceFactory fac;
	
	Collection<AbstractServiceEntry> list = new LinkedList<AbstractServiceEntry>();

	public Collection<AbstractServiceEntry> getServices() {
		return list;
	}

	DefaultHandler handler;
	private final DocumentBuilder docBuilder;
	Document current = null;

	boolean inRegister = false;
	int level = 0;

	public ConfigLoaderHandler(IEventServiceFactory fac) throws ParserConfigurationException {
		docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
		this.fac = fac;
	}

	@Override
	public void characters(char[] ch, int start, int length)
			throws SAXException {
		if (handler != null)
			handler.characters(ch, start, length);
	}

	@Override
	public void endElement(String uri, String localName, String qName)
			throws SAXException {
		if (handler != null)
			handler.endElement(uri, localName, qName);
		if (level == 3 && inRegister) {
			handler = null;
			instantiate(current);
			current = null;
		} else if (level == 2 && qName.equals("register"))
			inRegister = false;
		level--;
	}

	private void instantiate(Document serv) {
		try {
			AbstractServiceEntry srvEntry = fac.getServiceEntry(serv.getDocumentElement());
			list.add(srvEntry);
		} catch (InstantiationException e) {
			logger.log(Level.WARNING,
					"Instantiating a service from the config file failed", e);
		}
	}

	@Override
	public void ignorableWhitespace(char[] ch, int start, int length)
			throws SAXException {
		if (handler != null)
			handler.ignorableWhitespace(ch, start, length);
	}

	@Override
	public void processingInstruction(String target, String data)
			throws SAXException {
		if (handler != null)
			handler.processingInstruction(target, data);
	}

	@Override
	public void setDocumentLocator(Locator locator) {
		if (handler != null)
			handler.setDocumentLocator(locator);
	}

	@Override
	public void skippedEntity(String name) throws SAXException {
		if (handler != null)
			handler.skippedEntity(name);
	}

	@Override
	public void startDocument() throws SAXException {
		if (handler != null)
			handler.startDocument();
	}

	@Override
	public void startElement(String uri, String localName, String qName,
			Attributes attributes) throws SAXException {
		level++;
		if (level == 2 && qName.equals("register")) {
			inRegister = true;
		} else if (level == 3 && inRegister) {
			current = docBuilder.newDocument();
			handler = new SAX2DomHandler(current);
		} 
		if (handler != null)
			handler.startElement(uri, localName, qName, attributes);
	}

	@Override
	public void startPrefixMapping(String prefix, String uri)
			throws SAXException {
		if (handler != null)
			handler.startPrefixMapping(prefix, uri);
	}
}
