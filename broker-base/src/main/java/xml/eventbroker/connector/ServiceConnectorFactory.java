package xml.eventbroker.connector;

import java.io.StringWriter;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

import xml.eventbroker.DeliveryStatistics;
import xml.eventbroker.connector.delivery.AbstractHTTPDeliverer;
import xml.eventbroker.connector.delivery.BufferedNettyStreamingHTTPDeliverer;
import xml.eventbroker.connector.delivery.BufferedNettyStreamingHTTPDeliverer2;
import xml.eventbroker.connector.delivery.BufferedNettyStreamingHTTPDeliverer3;
import xml.eventbroker.connector.delivery.NettyStreamingHTTPDeliverer;
import xml.eventbroker.connector.delivery.PooledHTTPDeliverer;
import xml.eventbroker.connector.delivery.SimpleHTTPDeliverer;
import xml.eventbroker.connector.delivery.StreamingHTTPDeliverer;

public class ServiceConnectorFactory implements IEventConnectorFactory {
	private static final Logger logger = Logger.getAnonymousLogger();

	Map<String, AbstractHTTPDeliverer> m;

	private final ExecutorService pool;
	private final DeliveryStatistics stats;

	public ServiceConnectorFactory(ExecutorService pool,
			DeliveryStatistics stats) {
		this.pool = pool;
		this.stats = stats;
		m = new HashMap<String, AbstractHTTPDeliverer>();
	}

	public void init() {

		AbstractHTTPDeliverer d;

		// TODO: Find solution for hardcoding these entries (and their "get"
		// counterpart"
		d = new PooledHTTPDeliverer(pool);
		d.init(stats);
		m.put(d.getClass().getSimpleName(), d);

		d = new SimpleHTTPDeliverer(pool);
		d.init(stats);
		m.put(d.getClass().getSimpleName(), d);

		d = new StreamingHTTPDeliverer(pool);
		d.init(stats);
		m.put(d.getClass().getSimpleName(), d);

		d = new NettyStreamingHTTPDeliverer(pool);
		d.init(stats);
		m.put(d.getClass().getSimpleName(), d);

		d = new BufferedNettyStreamingHTTPDeliverer(pool);
		d.init(stats);
		m.put(d.getClass().getSimpleName(), d);

		d = new BufferedNettyStreamingHTTPDeliverer2(pool);
		d.init(stats);
		m.put(d.getClass().getSimpleName(), d);

		d = new BufferedNettyStreamingHTTPDeliverer3(pool);
		d.init(stats);
		m.put(d.getClass().getSimpleName(), d);

	}

	public void shutdown() {
		for (AbstractHTTPDeliverer del : m.values()) {
			del.shutdown();
		}
	}

	/**
	 * Test if a String is a valid Java-Classname.
	 * 
	 * @see http://www.java2s.com/Code/Java/Reflection/
	 *      DeterminewhetherthesuppliedstringrepresentsawellformedfullyqualifiedJavaclassname
	 *      .htm
	 * @param name
	 * @return
	 */
	private boolean isClassName(String name) {
		CharacterIterator iter = new StringCharacterIterator(name);
		char c = iter.first();
		if (c == CharacterIterator.DONE)
			return false;

		if (!Character.isJavaIdentifierStart(c)
				&& !Character.isIdentifierIgnorable(c))
			return false;

		c = iter.next();
		while (c != CharacterIterator.DONE) {
			if (!Character.isJavaIdentifierPart(c)
					&& !Character.isIdentifierIgnorable(c))
				return false;
			c = iter.next();
		}

		return true;
	}

	@Override
	public AbstractServiceEntry getServiceEntry(Element doc, String uri)
			throws InstantiationException {
		return getServiceEntry(null, uri, doc);
	}

	@Override
	public AbstractServiceEntry getServiceEntry(String eventType, String uri,
			Element doc) throws InstantiationException {
		String className = doc.getNodeName();

		if (!isClassName(className))
			logException(new SecurityException(
					"Given class-name is not a valid java-class-name."), doc);

		className = ServiceConnectorFactory.class.getPackage().getName() + '.'
				+ className;

		AbstractServiceEntry entry = null;

		try {
			Class<?> clazz = ServiceConnectorFactory.class.getClassLoader()
					.loadClass(className);
			Class<? extends AbstractServiceEntry> loadClass = clazz
					.asSubclass(AbstractServiceEntry.class);
			Constructor<? extends AbstractServiceEntry> constructor;
			constructor = loadClass.getConstructor(String.class, String.class,
					Element.class, IEventConnectorFactory.class);

			if (eventType == null)
				eventType = doc.getAttribute("event");

			entry = constructor.newInstance(eventType, uri, doc, this);

		} catch (NoSuchMethodException e) {
			logException(e, doc);
		} catch (SecurityException e) {
			logException(e, doc);
		} catch (IllegalAccessException e) {
			logException(e, doc);
		} catch (IllegalArgumentException e) {
			logException(e, doc);
		} catch (InvocationTargetException e) {
			logException(e, doc);
		} catch (ClassCastException e) {
			logException(e, doc);
		} catch (ClassNotFoundException e) {
			logException(e, doc);
		}

		return entry;
	}

	@Override
	public AbstractHTTPDeliverer getHTTPDeliverer(String type) {
		AbstractHTTPDeliverer deliverer = m.get(type);
		if (deliverer == null)
			deliverer = m.get(SimpleHTTPDeliverer.class.getSimpleName());
		return deliverer;
	}

	/**
	 * Log exception together with a formatted output of the used XML-Node.
	 * 
	 * @param e
	 *            Exception thrown during instantiation.
	 * @param doc
	 *            XML-Node that was used for instantiation.
	 * @throws InstantiationException
	 *             re-throws an exception so the calling method knows.
	 */
	private final void logException(Exception e, Node doc)
			throws InstantiationException {
		TransformerFactory transFactory = TransformerFactory.newInstance();
		Transformer transformer;
		String str = "<-- Could not format XML-Node -->";
		try {
			transformer = transFactory.newTransformer();
			StringWriter buffer = new StringWriter();
			transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION,
					"yes");
			transformer.transform(new DOMSource(doc), new StreamResult(buffer));
			str = buffer.toString();
		} catch (TransformerException e1) {
			logger.log(Level.WARNING, "Error during formating of XML-Node", e1);
		}

		logger.log(Level.SEVERE, "Could not instantiate:\n" + str, e);
		throw new InstantiationException();
	}
}
