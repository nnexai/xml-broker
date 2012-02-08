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

public class Shop extends HttpServlet {
	
	private static final long serialVersionUID = 1L;
	
	private String eventBrokerURL;
	
	private List<Product> products = new ArrayList<Product>();
	
    public Shop() {
        super();
        products.add(new Product("Laptop", 2, 1000));
        products.add(new Product("TV", 4, 1500));
        products.add(new Product("Xbox", 2, 200));
    }
    
    public void init(ServletConfig servletConfig) throws ServletException{
	    eventBrokerURL = servletConfig.getInitParameter("xmlbroker");  	
    }

    
    private int id = 0;
    private synchronized int getID(){
    	return id++;
    }
    
	protected void doGet(HttpServletRequest request, HttpServletResponse resp) throws ServletException, IOException {
		
		
		String path = request.getPathInfo();
		
		if(path != null && path.equals("/order")){
			PrintWriter writer = resp.getWriter();		
			writer.append("Bestellung gesendet");
			String customer = request.getParameter("name");
			String mail = "[Order confirmation] ";
			int size = 0;
			double price = 0;
			for (Product p: products) {
				int anz = Integer.parseInt(request.getParameter(p.name));
				if(anz > 0) {
					price += p.price * anz;
					size += p.size * anz;
					mail += p.name + " (" + p.price + ") x " + anz + " = " + p.price * anz + "    "; 
				}
			}
			
			if(size > 0) {
				mail += "total " + price;
				int ordernr = getID();
				mail += "  #order nr:" + ordernr;

				String orderConfitmation = "<mail from='shop' to='"+customer+"'>"+ mail +"</mail>";
				String portoEvent = "<parcel from='shop' to='"+customer+"' size='"+ size +"' />";
				String debitEvent = "<transfer to='shop' from='"+customer+"' amount='"+price+"' comment='Order "+ordernr+"'/>";
				writer.append(orderConfitmation + "<br>" +portoEvent + "<br>" +  debitEvent);
				sendEvent(orderConfitmation);
				sendEvent(debitEvent);				
				sendEvent(portoEvent);
			}
			writer.flush();
						
		} else {
			PrintWriter writer = resp.getWriter();		
			writer.append("<html><body><h2>Shop</h2><br>");
			writer.append("<form method='get' action='http://localhost:8080/TomcatTest/Shop/order'><table><tr>");
			writer.append("<td><b>Produkt</b></td><td><b>Preis</b></td><td><b>Menge</b></td>");
			writer.append("</tr>");
			
			for (Product p: products) {
				writer.append("<tr>");
				writer.append("<td>"+ p.name +"</td><td>"+ p.price +"</td><td><input type='0' size='2' value='0' name='"+p.name+"'></td>");
				writer.append("</tr>");
			}
			
			writer.append("</table>");
			writer.append("<br><br>Name: <input type='text' name='name' /><input type='submit' value='kaufen' /></form>");
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
			}
		} catch (Exception e) {
			e.printStackTrace();
		}		
		
		
		response.setStatus(HttpServletResponse.SC_OK);	
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
	
	private class Product{
		String name;
		int size;
		double price;
		
		public Product(String name, int size, double price) {
			this.name = name;
			this.size = size;
			this.price = price;
		}
	}

}

