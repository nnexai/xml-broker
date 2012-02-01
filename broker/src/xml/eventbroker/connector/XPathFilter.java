package xml.eventbroker.connector;

import java.io.IOException;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactory;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class XPathFilter extends AbstractServiceEntry {

	private static final XPathFactory factory = XPathFactory.newInstance();
	private final Transformer transformer;
	private final AbstractServiceEntry service;
	private final XPathExpression path;
	private final String pathString;

	public XPathFilter(String event, String uri, Element xml,
			IEventConnectorFactory fac) throws InstantiationException {
		super(event, uri);
		try {
			StringBuilder str = new StringBuilder();
			str.append('/').append(event).append('[').append(xml.getAttribute("filter")).append(']');
			this.pathString = str.toString();
			path = factory.newXPath().compile(pathString);

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

			service = fac.getServiceEntry(event, uri, (Element) item);

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

		DOMEventDescription descr = (DOMEventDescription) eventB;
		Node node = descr.doc;
		String eventStr = descr.eventString;
		try {

			result = (Node) path.evaluate(node, XPathConstants.NODE);

			System.out.println("Got: "+eventStr+" will evaluate "+pathString);
			System.out.println("Result: "+result);
			
			if (result != null) {
				Object resultB = result;
				if (!service.requiresDOM()) {
					resultB = eventStr;
				}
				service.deliver(resultB);
			}

		} catch (XPathExpressionException e) {
			throw new IOException(e);
		}
	}

	@Override
	public String toString() {
		return pathString+" > "+service;
	}
}
