package xml.eventbroker.callback;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.SocketException;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
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

import xml.eventbroker.shared.MultiXMLRootFilter;

public class CallbackService extends HttpServlet {

	private static final long serialVersionUID = 1L;
	private static final Logger logger = Logger.getAnonymousLogger();

	private LongPollMap lpm;

	private ExecutorService pool;

	@Override
	public void init() throws ServletException {
		super.init();
		pool = Executors.newCachedThreadPool();
		lpm = new LongPollMap();
		factory.setValidating(false);

		try {
			t = TransformerFactory.newInstance().newTransformer();
			// t.setOutputProperty(OutputKeys.CDATA_SECTION_ELEMENTS, "jython");
			t.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
		} catch (TransformerConfigurationException e) {
			e.printStackTrace();
		} catch (TransformerFactoryConfigurationError e) {
			e.printStackTrace();
		}
	}

	@Override
	public void destroy() {
		super.destroy();
		try {
			pool.shutdown();
			if (!pool.awaitTermination(4, TimeUnit.SECONDS))
				pool.shutdownNow();
		} catch (InterruptedException e) {
			logger.log(Level.WARNING, "Unable to shutdown Threadpool", e);
		}
	}

	// @Override
	// protected void doGet(HttpServletRequest req, HttpServletResponse resp)
	// throws ServletException, IOException {
	// PrintWriter writer = resp.getWriter();
	//
	// writer.append("<html><body>");
	// writer.append("<ul>");
	//
	// synchronized (events) {
	// for (String event : events) {
	// writer.append("<li>");
	// writer.append(StringEscapeUtils.escapeXml(event));
	// writer.append("</li>");
	// }
	// }
	//
	// writer.append("</ul>");
	// writer.append("</body></html>");
	//
	// writer.flush();
	//
	// resp.setStatus(HttpServletResponse.SC_OK);
	// }

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		req.getSession(true).setMaxInactiveInterval(60*60*1000);
		AsyncContext async = req.startAsync(req, resp);
		logger.info("Setting session timeout");
		async.setTimeout(60*60*1000);
		
		Runnable t = new XMLProcessorThread(async);
		pool.execute(t);
	}

	DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
	Transformer t;

	class XMLProcessorThread implements Runnable {

		AsyncContext ctx;

		public XMLProcessorThread(AsyncContext ctx) {
			this.ctx = ctx;
		}

		@Override
		public void run() {
			try {
				readXMLEvents(ctx);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		private void readXMLEvents(AsyncContext async) throws IOException {
			MultiXMLRootFilter filtered = new MultiXMLRootFilter(async
					.getRequest().getReader(), 0x1000);
			boolean isAnswer = false;
			try {

				DocumentBuilder builder = factory.newDocumentBuilder();
				while (!filtered.hasFinished()) {
					try {
						StringWriter sw = new StringWriter(0x1000);

						Document doc = builder.parse(new InputSource(filtered));
						Element ele = doc.getDocumentElement();

						if ("callback".equals(ele.getNodeName())) {
							isAnswer = true;
							String idStr = ele.getAttribute("id");
							if (idStr != null & !"".equals(idStr)) {
								long index = Long.valueOf(idStr);
								Node child = DomUtil.getChild(ele, Node.ELEMENT_NODE);
								String answer;
								if(child != null) {
									t.transform(new DOMSource(child),
									new StreamResult(sw));
									answer = sw.toString();
								} else
									answer = DomUtil.getContent(ele);
								lpm.answer(index, answer);
							}
						} else {
							// this is a request
							HttpPollingContext ctx = new HttpPollingContext(
									async);
							long index = lpm.register(ctx);
							System.out
									.println("Registered request with index: "
											+ index);
							Element cbElement = doc.createElement("callback");
							cbElement.setAttribute("id", Long.toString(index));
							ele.appendChild(cbElement);

							Element root = doc.createElement("elements");
							root.appendChild(ele);

							t.transform(new DOMSource(root), new StreamResult(
									sw));
							proxyRequest(sw.toString());
							break;
						}
						builder.reset();
					} catch (SAXException e) {
						logger.log(Level.WARNING, "Error during parse", e);
					} catch (TransformerException e) {
						logger.log(Level.WARNING,
								"Error during generation of String", e);
					}
				}
				logger.info("Incoming Request-Stream was closed");

			} catch (SocketException e) {
				logger.log(Level.WARNING, "Connection timed out", e);
			} catch (IOException e) {
				logger.log(Level.WARNING, "Connection timed out", e);
			} catch (ParserConfigurationException e) {
				logger.log(Level.WARNING,
						"Could not instantiate Document Builder", e);
			} finally {
				filtered.forceClose();
				if (isAnswer) {
					logger.info("Request was an answer so lets close everything");
					async.complete();
				}
			}
		}

		private void proxyRequest(String string) {
			URL url;
			try {
				url = new URL("http://localhost:8080/broker/XMLEventBroker/");
				HttpURLConnection con = (HttpURLConnection) url
						.openConnection();
				con.setRequestMethod("POST");
				con.setDoOutput(true);
				con.setDoInput(true);
				con.setAllowUserInteraction(false);
				con.setUseCaches(false);
				con.setRequestProperty("Content-type", "text/xml");

				con.connect();

				System.out.println("Sending:\n" + string);

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
}
