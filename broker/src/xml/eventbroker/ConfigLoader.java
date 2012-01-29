package xml.eventbroker;

import java.io.IOException;
import java.net.URL;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.SAXException;

import xml.eventbroker.connector.AbstractServiceEntry;
import xml.eventbroker.connector.IEventConnectorFactory;

public class ConfigLoader {

	private static final Logger logger = Logger.getAnonymousLogger();

	static Collection<AbstractServiceEntry> getConfig(URL configFile, IEventConnectorFactory fac) {
		SAXParserFactory fact = SAXParserFactory.newInstance();
		try {
			SAXParser parser = fact.newSAXParser();
			ConfigLoaderHandler handler = new ConfigLoaderHandler(fac);
			parser.parse(configFile.toString(), handler);
			return handler.getServices();
		} catch (ParserConfigurationException e) {
			logger.log(Level.SEVERE, "Error configuring parser", e);
		} catch (SAXException e) {
			logger.log(Level.SEVERE, "Error parsing configuration file", e);
		} catch (IOException e) {
			logger.log(Level.SEVERE, "Error opening configuration file", e);
		}
		return null;
	}
}
