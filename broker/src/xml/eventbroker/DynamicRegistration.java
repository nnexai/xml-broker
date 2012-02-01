package xml.eventbroker;

import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Logger;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import xml.eventbroker.connector.AbstractServiceEntry;
import xml.eventbroker.connector.IEventConnectorFactory;

public class DynamicRegistration {

	private static final Logger logger = Logger.getAnonymousLogger();

	private final RegisteredServices regServices;
	private final DocumentBuilder _builder;
	private final IEventConnectorFactory fac;

	public DynamicRegistration(RegisteredServices regServices,
			IEventConnectorFactory fac) throws ParserConfigurationException {
		this.regServices = regServices;
		this.fac = fac;

		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		_builder = factory.newDocumentBuilder();
	}

	public boolean subscribe(InputStream in, String path) {
		logger.info("Registering one service");
		if (path != null) {
			String[] split = path.split("/", 2);

			if (split.length == 2) {
				String uri = split[1];

				try {
					Document doc = _builder.parse(new InputSource(in));

					AbstractServiceEntry serviceEntry = fac.getServiceEntry(
							doc.getDocumentElement(), uri);
					return regServices.registerService(serviceEntry);
				} catch (SAXException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				} catch (InstantiationException e) {
					e.printStackTrace();
				}
			}
		}
		return false;
	}

	public boolean unsubscribe(String path) {

		if (path != null) {
			String[] split = path.split("/", 2);

			if (split.length == 2) {
				String uri = split[1];

				logger.info("Removing one service: " + uri);
				return regServices.unsubscribe(uri);
			}
		}
		return false;
	}
}
