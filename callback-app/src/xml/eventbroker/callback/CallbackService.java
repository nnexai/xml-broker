package xml.eventbroker.callback;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.SocketException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.AsyncContext;
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

import org.apache.tomcat.util.DomUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class CallbackService extends HttpServlet {

	private static final long serialVersionUID = 1L;
	private static final Logger logger = Logger.getAnonymousLogger();

	private static LongPollMap lpm;

	@Override
	public void init() throws ServletException {
		super.init();
		lpm = new LongPollMap();
		factory.setValidating(false);

		try {
			t = TransformerFactory.newInstance().newTransformer();
			t.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
		} catch (TransformerConfigurationException e) {
			e.printStackTrace();
		} catch (TransformerFactoryConfigurationError e) {
			e.printStackTrace();
		}
	}
	

//	@Override
//	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
//			throws ServletException, IOException {
//		PrintWriter writer = resp.getWriter();
//
//		writer.append("<html><body>");
//		writer.append("<ul>");
//
//		synchronized (events) {
//			for (String event : events) {
//				writer.append("<li>");
//				writer.append(StringEscapeUtils.escapeXml(event));
//				writer.append("</li>");
//			}
//		}
//
//		writer.append("</ul>");
//		writer.append("</body></html>");
//
//		writer.flush();
//
//		resp.setStatus(HttpServletResponse.SC_OK);
//	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		AsyncContext async = req.startAsync();
		logger.info("Setting session timeout");
		async.setTimeout(20*1000);
		readXMLEvents(async);
	}

	DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
	Transformer t;
	
	private void readXMLEvents(AsyncContext async) throws IOException {
		MultiXMLRootFilter filtered = new MultiXMLRootFilter(async.getRequest().getReader(), 0x100);
		boolean isAnswer = false;
		try {
			
			DocumentBuilder builder = factory.newDocumentBuilder();
			while(!filtered.hasFinished()) {				
				try {
					StringWriter sw = new StringWriter(0x100);

					Document doc = builder.parse(new InputSource(filtered));
					Element ele = doc.getDocumentElement();
					
					if("callback".equals(ele.getNodeName())){
						isAnswer = true;
						String idStr = ele.getAttribute("id");
						if(idStr != null) {
							long index = Long.valueOf(idStr);
							Node answer = DomUtil.getChild(ele, Node.ELEMENT_NODE);
							t.transform(new DOMSource(answer), new StreamResult(sw));
							lpm.answer(index, sw.toString());
						}
					} else {
						// this is a request
						HttpPollingContext ctx = new HttpPollingContext(async);
						long index = lpm.register(ctx);
						System.out.println("Registered request with index: "+index);
						Element cbElement = doc.createElement("callback");
						cbElement.setAttribute("id", Long.toString(index));
						ele.appendChild(cbElement);
						t.transform(new DOMSource(ele), new StreamResult(sw));
						proxyRequest(sw.toString());
						break;
					}
					
					builder.reset();
				} catch (SAXException e) {					
					logger.log(Level.WARNING, "Error during parse", e);			
				} catch (TransformerException e) {
					logger.log(Level.WARNING, "Error during generation of String", e);			
				}				
			}
			logger.info("Incoming Request-Stream was closed");

		} catch (SocketException e) {
			logger.log(Level.WARNING, "Connection timed out", e);			
		} catch (IOException e) {
			logger.log(Level.WARNING, "Connection timed out", e);
		} catch (ParserConfigurationException e) {
			logger.log(Level.WARNING, "Could not instantiate Document Builder", e);
		} finally {
			if(isAnswer) {
				logger.info("Request was an answer so lets close everything");
				filtered.forceClose();
				async.complete();
			}
		}
	}


	private void proxyRequest(String string) {
		URL url;
		try {
			url = new URL("http://localhost:8080/broker/XMLEventBroker/");
			HttpURLConnection con = (HttpURLConnection) url.openConnection();
			con.setRequestMethod("POST");
			con.setDoOutput(true);
			con.setDoInput(true);
			con.setRequestProperty("Content-type", "text/xml");

			con.connect();

			System.out.println("Sending:\n"+string);

			Writer out = new OutputStreamWriter(con.getOutputStream());
			out.append(string);
			out.close();
			con.getInputStream();
			con.disconnect();
		
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
