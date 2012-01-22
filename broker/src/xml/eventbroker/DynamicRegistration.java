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
import xml.eventbroker.service.IEventServiceFactory;

public class DynamicRegistration {
	
	private static final Logger logger = Logger.getAnonymousLogger();
	
	private final RegisteredServices regServices;
	private final DocumentBuilder _builder;
	private final IEventServiceFactory fac;

	public DynamicRegistration(RegisteredServices regServices, IEventServiceFactory fac)
			throws ParserConfigurationException {
		this.regServices = regServices;
		this.fac = fac;
		
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		_builder = factory.newDocumentBuilder();
	}

	public boolean subscribe(InputStream in) {
		logger.info("Registering one service");
		try {
			Document doc = _builder.parse(new InputSource(in));
			logger.info(doc.getDocumentElement().toString());
			AbstractServiceEntry serviceEntry = fac.getServiceEntry(doc.getDocumentElement());
			return regServices.registerService(serviceEntry);
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InstantiationException e) {
			e.printStackTrace();
		}
		return false;
	}

	public boolean unsubscribe(String path) {
		
		if(path != null) {
			String[] split = path.split("/", 3);
			
			if(split.length == 3) {
				String event = split[1];
				String id = split[2];
			
				logger.info("Removing one service: "+event+"/"+id);
				return regServices.unsubscribe(event, id);
			}
		}
		return false;
	}
}
