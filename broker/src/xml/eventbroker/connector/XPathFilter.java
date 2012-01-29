package xml.eventbroker.connector;

import java.io.IOException;
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

public class XPathFilter extends AbstractServiceEntry {

	private static final Logger logger = Logger.getAnonymousLogger();
	private static final XPathFactory factory = XPathFactory.newInstance();
	private final Transformer transformer;
	private final AbstractServiceEntry service;
	private final XPathExpression path;

	public XPathFilter(String event, String id, Element xml,
			IEventConnectorFactory fac) throws InstantiationException {
		super(event, id);
		try {
			path = factory.newXPath().compile(xml.getAttribute("path"));

			// get first non-text element
			Node item = null;
			NodeList childNodes = xml.getChildNodes();
			for (int i = 0; i < childNodes.getLength(); i++) {
				Node child = childNodes.item(i);
				if (child.getNodeType() == Node.ELEMENT_NODE) {
					item = child;
					break;
				}
			}

			service = fac.getServiceEntry(event, id, (Element) item);

			TransformerFactory transFactory = TransformerFactory.newInstance();
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
	public boolean requiresDOM() {
		return true;
	}

	@Override
	public void deliver(Object eventB) throws IOException {
		Node result;

		Node node = (Node) eventB;
		try {

			result = (Node) path.evaluate(node, XPathConstants.NODE);

			if (result != null) {
				Object resultB = result;
				if (!service.requiresDOM()) {
					StringWriter buffer = new StringWriter();
					transformer.transform(new DOMSource(result),
							new StreamResult(buffer));

					resultB = buffer.toString();
				}
				service.deliver(resultB);
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
