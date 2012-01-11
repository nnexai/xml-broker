package xml.eventbroker;

import java.io.IOException;
import java.net.URL;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class ConfigLoader {

	static class GetConfigHandler extends DefaultHandler {
		String currentEvent = null;
		String currentValue = null;
		Collection<ServiceEntry> list = new LinkedList<ServiceEntry>();

		public Collection<ServiceEntry> getServices() {
			return list;
		}

		@Override
		public void startElement(String uri, String localName, String qName,
				Attributes attributes) throws SAXException {
			if (qName.equals("register")) {
				currentEvent = attributes.getValue("event");
			}
		}

		@Override
		public void endElement(String uri, String localName, String qName)
				throws SAXException {
			if (qName.equals("register")) {
				list.add(new ServiceEntry(currentEvent, currentValue));
				currentEvent = null;
				currentValue = null;
			}
		}

		@Override
		public void characters(char[] ch, int start, int length)
				throws SAXException {
			currentValue = new String(ch, start, length);
		}

	}

	static Collection<ServiceEntry> getConfig(URL configFile) {
		SAXParserFactory fact = SAXParserFactory.newInstance();
		try {
			SAXParser parser = fact.newSAXParser();
			GetConfigHandler handler = new GetConfigHandler();
			parser.parse(configFile.toString(), handler);
			return handler.getServices();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}
}
