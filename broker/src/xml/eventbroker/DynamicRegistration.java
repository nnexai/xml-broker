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

import xml.eventbroker.service.AbstractServiceEntry;
import xml.eventbroker.service.ServiceEntryFactory;

public class DynamicRegistration {
	
	private static final Logger logger = Logger.getAnonymousLogger();
	
	private final RegisteredServices regServices;
	private final DocumentBuilder _builder;

	public DynamicRegistration(RegisteredServices regServices)
			throws ParserConfigurationException {
		this.regServices = regServices;

		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		_builder = factory.newDocumentBuilder();
	}

	public void subscribe(InputStream in) {
		logger.info("Registering one service");
		try {
			Document doc = _builder.parse(new InputSource(in));
			logger.info(doc.getDocumentElement().toString());
			AbstractServiceEntry serviceEntry = ServiceEntryFactory
					.getServiceEntry(doc.getDocumentElement());
			regServices.registerService(serviceEntry);
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InstantiationException e) {
			e.printStackTrace();
		}
	}

	public void unsubscribe(String path) {
		if(path != null) {
			String[] split = path.split("/");
			
			if(split.length == 3) {
				String event = split[1];
				String id = split[2];
			
				logger.info("Removing one service: "+event+"/"+id);
				regServices.unsubscribe(event, id);
			}
		}
	}
}
