package xml.eventbroker.example;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.SocketException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.tomcat.util.DomUtil;
import org.python.core.PyException;
import org.python.core.PySystemState;
import org.python.util.PythonInterpreter;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import xml.eventbroker.shared.MultiXMLRootFilter;

public class JythonReplyService extends HttpServlet {

	private static final Logger logger = Logger.getAnonymousLogger();
	private DocumentBuilderFactory factory = DocumentBuilderFactory
			.newInstance();
	private Transformer t;

	@Override
	public void init() throws ServletException {
		super.init();
		factory.setValidating(false);
		PySystemState.initialize(  );

		try {
			t = TransformerFactory.newInstance().newTransformer();
			t.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
		} catch (TransformerConfigurationException e) {
			e.printStackTrace();
		} catch (TransformerFactoryConfigurationError e) {
			e.printStackTrace();
		}
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		readXMLEvents(req.getReader());
	}

	private void readXMLEvents(Reader in) throws IOException {
		MultiXMLRootFilter filtered = new MultiXMLRootFilter(in, 0x1000);
		try {

			DocumentBuilder builder = factory.newDocumentBuilder();
			while (!filtered.hasFinished()) {
				try {
					StringWriter sw = new StringWriter(0x1000);

					Document doc = builder.parse(new InputSource(filtered));

					Element ele = doc.getDocumentElement();
					Element cb = (Element) DomUtil.getChild(ele, "callback");

					if (cb != null) {
						ele.removeChild(cb);
						
						String script = ele.getTextContent();
						String out = ele.getAttribute("outVariable");
						Element a = doc.createElement("result");
						a.setTextContent(StringEscapeUtils.escapeXml(runScript(out, script).toString()));

						cb.appendChild(a);

						Element ctx = doc.createElement("elements");
						ctx.appendChild(cb);
						t.transform(new DOMSource(ctx), new StreamResult(sw));
						answerRequest(sw.toString());
					}
				} catch (SAXException e) {
					logger.log(Level.WARNING, "Error during parse", e);
				} catch (TransformerException e) {
					logger.log(Level.WARNING,
							"Error during generation of String", e);
				}
				builder.reset();
			}
			logger.info("Incoming Request-Stream was closed");

		} catch (SocketException e) {
			logger.log(Level.WARNING, "Connection timed out", e);
		} catch (IOException e) {
			logger.log(Level.WARNING, "Connection timed out", e);
		} catch (ParserConfigurationException e) {
			logger.log(Level.WARNING, "Could not instantiate Document Builder",
					e);
		} finally {
			filtered.forceClose();
		}
	}

	private Object runScript(String out, String script) {
		Object result = "<null>";
		if(script != null)
			try {
				PythonInterpreter interp = new PythonInterpreter(  );
				interp.exec(script);
				Object get = interp.get(out);
				if(get != null)
					result = get;
			} catch (PyException e) {
				result = e;
			}
		return result;
	}

	private void answerRequest(String string) {
		URL url;
		try {
			url = new URL("http://localhost:8080/broker/XMLEventBroker/");
			HttpURLConnection con = (HttpURLConnection) url.openConnection();
			con.setRequestMethod("POST");
			con.setDoOutput(true);
			con.setDoInput(true);
			con.setAllowUserInteraction(false);
			con.setUseCaches(false);
			con.setRequestProperty("Content-type", "text/xml");

			con.connect();

			System.out.println("Replying:\n" + string);

			Writer out = new OutputStreamWriter(con.getOutputStream());
			out.append(string);
			out.close();
			con.getInputStream().close();
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
