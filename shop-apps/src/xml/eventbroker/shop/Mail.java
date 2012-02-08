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

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;

/**
 * Servlet implementation class ConverterService
 */
public class Mail extends HttpServlet {
	
	private static final long serialVersionUID = 1L;
		
	private Map<String, List<Message>> reg = new HashMap<String, List<Message>>(); 
	
    public Mail() {
        super();
    }
    
    public void init(ServletConfig servletConfig) throws ServletException{
    	createAccount("benjamin");
    	createAccount("shop");
    	createAccount("parcelservice");
    	createAccount("bank");

    }


	protected void doGet(HttpServletRequest request, HttpServletResponse resp) throws ServletException, IOException {
		
		String path = request.getPathInfo();
		if(path != null && path.equals("/reset")){  
			for(String s : reg.keySet()) {
				reg.put(s, new ArrayList<Message>());
			}
		} else {		
			PrintWriter writer = resp.getWriter();
			
			writer.append("<html><body><h2>Mail</h2>");
			writer.append("<ul>");
			
			for (String user : reg.keySet()) {
				writer.append("<li>");
				writer.append("<b>"+user+"</b>");
				List<Message> messages = reg.get(user);
				writer.append("<table border='1'><tr><td><b>from</b></td><td><b>time</b></td><td><b>message</b></td></tr>");
				for (Message m : messages) {
					writer.append("<tr><td>" + m.from + "</td><td>" + m.time + "</td><td>" + m.message + "</td></tr>");
				}
				writer.append("</table><br></li>");
			}
			
			writer.append("</ul>");
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
			if (event.getName().equals("mail")){				
				String from = event.getAttributeValue("from"); 
				String to = event.getAttributeValue("to"); 
				String message = event.getText();		
				java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd.MM.yyyy HH.mm.ss");	
				addMessage(from, to, sdf.format(new java.util.Date()), message);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}		
		
		
		response.setStatus(HttpServletResponse.SC_OK);	
	}
	
	private boolean createAccount(String username) {
		if(reg.containsKey(username)) {
			return false;
		}
		
		reg.put(username, new ArrayList<Message>());		
		return true;		
	}
	
	private void addMessage(String from, String to, String time, String message){
		if(! reg.containsKey(to)) {
			return;
		}
		
		Message m = new Message(from, to, time, message);
		reg.get(to).add(m);
	}
	
	private class Message{
		public String from;
		public String to;
		public String time;
		public String message;
		
		public Message(String from, String to, String time, String message){
			this.from = from;
			this.to = to;
			this.time = time;
			this.message = message;
		}
	}
	

}

