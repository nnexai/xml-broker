package xml.eventbroker.shop;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;

public class Bank extends HttpServlet {
	
	private static final long serialVersionUID = 1L;
	
	private String eventBrokerURL;
	
	private Map<String, Double> accounts = new HashMap<String,Double>(); 
	private StringBuffer log = new StringBuffer();	
	
    public Bank() {
        super();
    }
    
    public void init(ServletConfig servletConfig) throws ServletException{
	    eventBrokerURL = servletConfig.getInitParameter("xmlbroker");
    	createAccount("benjamin");
    	createAccount("frank");
    	createAccount("parcelservice");
    	createAccount("shop");
    }


	protected void doGet(HttpServletRequest request, HttpServletResponse resp) throws ServletException, IOException {
		
		String path = request.getPathInfo();
		if(path != null && path.equals("/reset")){  
			for(String s : accounts.keySet()) {
				accounts.put(s, .0);
			}
			log = new StringBuffer();
		} else {
			PrintWriter writer = resp.getWriter();
			
			writer.append("<html><body><h2>Bank</h2><br>");
			writer.append("<ul>");
			
			for (String user : accounts.keySet()) {
				writer.append("<li>");
				Double b = accounts.get(user);
				writer.append("<b>"+user+"</b>: " +  b);
				writer.append("</li>");
			}
			
			writer.append("</ul>");
			writer.append(log);
			writer.append("</body></html>");
			
			writer.flush();
		}
		
		resp.setStatus(HttpServletResponse.SC_OK);
	
	}


	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		try {
			// parse event 
			SAXBuilder builder = new SAXBuilder();
			Document eventdoc = builder.build(request.getReader());
			Element event = eventdoc.getRootElement();			 
			if (event.getName().equals("transfer")){				
				String from = event.getAttributeValue("from"); 
				String to = event.getAttributeValue("to"); 
				Double amount = Double.parseDouble(event.getAttributeValue("amount"));		
				String comment = event.getAttributeValue("comment"); 
				transfer(from, to, amount, comment);
			} 
		} catch (Exception e) {
			e.printStackTrace();
		}		
		
		
		response.setStatus(HttpServletResponse.SC_OK);	
	}
	
	private boolean createAccount(String username) {
		if(accounts.containsKey(username)) {
			return false;
		}
		
		accounts.put(username, new Double(0));		
		return true;		
	}
	
	private synchronized void transfer(String from, String to, Double amount, String comment){
		if(! (accounts.containsKey(to) && accounts.containsKey(from))) {
			return;
		}
		
		accounts.put(from, accounts.get(from) - amount);
		accounts.put(to, accounts.get(to) + amount);
		log.append("Transfer: " + from + " => " + to + ": " + amount + " ["+comment+"]<br />");
		
		sendMessage(from, "[Ueberweisungsbeleg] An:" + to + " Betrag: -" + amount + " ("+comment+")");
		sendMessage(to, "[Ueberweisungsbeleg] Von:" + from + " Betrag: +" + amount + " ("+comment+")");
	}

	private void sendMessage(String to, String message) {
		String event = "<mail from='bank' to='"+to+"'>"+message+"</mail>";
		sendEvent(event);
	}
	

	private void sendEvent(String event) {
		try{
			URL url = new URL(eventBrokerURL);
			HttpURLConnection con = (HttpURLConnection) url.openConnection();
			con.setRequestMethod("POST");
			con.setDoOutput(true);
			con.setDoInput(false);
			con.setChunkedStreamingMode(-1);
			con.setRequestProperty("Connection", "keep-alive");
			con.setConnectTimeout(120 * 1000);
			con.setRequestProperty("Content-type", "text/xml");
	
			OutputStream out = con.getOutputStream();
			BufferedOutputStream bos = new BufferedOutputStream(out);
			OutputStreamWriter writer = new OutputStreamWriter(bos, "UTF-8");
			
			writer.append("<?xml version=\"1.0\" encoding=\"utf-8\"?><events>" + event + "</events>");
			writer.flush();
			writer.close();			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	

}

