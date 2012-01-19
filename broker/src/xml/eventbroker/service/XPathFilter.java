package xml.eventbroker.service;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.logging.Logger;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

public class XPathFilter extends AbstractServiceEntry {

	private static final Logger logger = Logger.getAnonymousLogger();
	private static final XPathFactory factory = XPathFactory.newInstance();
	private final Transformer transformer;
	private final AbstractServiceEntry service;
	private final XPathExpression path;

	public XPathFilter(String event, Element xml) throws InstantiationException {
		super(event);
		try {
			path = factory.newXPath().compile(xml.getAttribute("path"));
			
			//get first non-text element
			Node item = null;
			NodeList childNodes = xml.getChildNodes();
			for(int i = 0; i < childNodes.getLength(); i++) {
				Node child = childNodes.item(i);
				if(child.getNodeType() == Node.ELEMENT_NODE) {
					item = child;
					break;
				}
			}

			service = ServiceEntryFactory
					.getServiceEntry(event, (Element) item);
			
			TransformerFactory transFactory = TransformerFactory
					.newInstance();
			transformer = transFactory.newTransformer();
			transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION,
					"yes");

		} catch (XPathExpressionException e) {
			throw new InstantiationException();
		} catch (TransformerConfigurationException e) {
			throw new InstantiationException();
		}
	}

	@Override
	public void deliver(String eventBody) throws IOException {
		Node result;
		try {
			
			//TODO: Better tell on registration if simple String or Dom representation is needed
			result = (Node) path.evaluate(new InputSource(new StringReader(
					eventBody)), XPathConstants.NODE);

			if (result != null) {
				StringWriter buffer = new StringWriter();
				transformer.transform(new DOMSource(result), new StreamResult(
						buffer));

				service.deliver(buffer.toString());
			}

		} catch (XPathExpressionException e) {
			throw new IOException(e);
		} catch (TransformerConfigurationException e) {
			throw new IOException(e);
		} catch (TransformerException e) {
			throw new IOException(e);
		}
	}

}
