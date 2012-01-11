package xml.eventbroker.client;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class ConfigLoader {

	static class GetConfigHandler extends DefaultHandler {
		String currentValue = null;
		Map<String, String> values = new HashMap<String, String>();		
		
		public Map<String, String> getConfigMap() {
			return values;
		}
		
		@Override
		public void endElement(String uri, String localName, String qName)
				throws SAXException {
			if(!qName.equals("config")) {
				values.put(qName, currentValue);
				currentValue = null;
			}
		}
		
		@Override
		public void characters(char[] ch, int start, int length)
				throws SAXException {
			currentValue = new String(ch, start, length);
		}

	}
	
	
	static Map<String, String> getConfig(URL configFile) {
		SAXParserFactory fact = SAXParserFactory.newInstance();
		try {
			SAXParser parser = fact.newSAXParser();
			GetConfigHandler handler = new GetConfigHandler();
			parser.parse(configFile.toString(), handler);
			return handler.getConfigMap();
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
