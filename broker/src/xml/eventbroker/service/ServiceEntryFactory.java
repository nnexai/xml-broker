package xml.eventbroker.service;

import java.io.StringWriter;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
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

public class ServiceEntryFactory {
	private static final Logger logger = Logger.getAnonymousLogger();

	/**
	 * Test if a String is a valid Java-Classname.
	 * @see http://www.java2s.com/Code/Java/Reflection/DeterminewhetherthesuppliedstringrepresentsawellformedfullyqualifiedJavaclassname.htm
	 * @param name
	 * @return
	 */
	private static boolean isClassName(String name) {
		CharacterIterator iter = new StringCharacterIterator(name);
        char c = iter.first();
        if (c == CharacterIterator.DONE) return false;

        if (!Character.isJavaIdentifierStart(c) && !Character.isIdentifierIgnorable(c)) return false;
        
        c = iter.next();
        while (c != CharacterIterator.DONE) {
            if (!Character.isJavaIdentifierPart(c) && !Character.isIdentifierIgnorable(c)) return false;
            c = iter.next();
        }

        return true;
	}
	
	public static AbstractServiceEntry getServiceEntry(Element doc) throws InstantiationException {
		return getServiceEntry(null, doc);
	}
	
	public static AbstractServiceEntry getServiceEntry(String eventType, Element doc)
			throws InstantiationException {
		String className = doc.getNodeName();
		
		if (!isClassName(className))
			logException(new SecurityException("Given class-name is not a valid java-class-name."), doc);
		
		className = ServiceEntryFactory.class.getPackage().getName()+'.'+className;
		
		AbstractServiceEntry entry = null;

		try {
			Class<?> clazz = ServiceEntryFactory.class.getClassLoader()
					.loadClass(className);
			Class<? extends AbstractServiceEntry> loadClass = clazz
					.asSubclass(AbstractServiceEntry.class);
			Constructor<? extends AbstractServiceEntry> constructor;
			constructor = loadClass.getConstructor(String.class,Element.class);
			
			if(eventType == null)
				eventType = doc.getAttribute("event");
			
			entry = constructor.newInstance(eventType, doc);

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

	/**
	 * Log exception together with a formatted output of the used XML-Node.
	 * @param e Exception thrown during instantiation.
	 * @param doc XML-Node that was used for instantiation.
	 * @throws InstantiationException re-throws an exception so the calling method knows.
	 */
	private static final void logException(Exception e, Node doc)
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
