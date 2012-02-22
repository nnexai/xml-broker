package xml.eventbroker.shop;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;

public class ParcelService extends HttpServlet {
	
	private static final long serialVersionUID = 1L;
	
	private String eventBrokerURL;
	private String myURL = "http://localhost:8080/shop-apps/ParcelService";
			
	private Map<Integer, Parcel> parcels = new HashMap<Integer,Parcel>(); 
	
    public ParcelService() {
        super();
    }
    
    public void init(ServletConfig servletConfig) throws ServletException{
	    eventBrokerURL = servletConfig.getInitParameter("xmlbroker");
    }

    private String[] state = {"Das Paket wird zur Zustellbasis transportiert.", "Das Paket wird heute ausgeliefert.", "Das Paket wurde zugestellt."};

	protected void doGet(HttpServletRequest request, HttpServletResponse resp) throws ServletException, IOException {
		
		PrintWriter writer = resp.getWriter();
		
		writer.append("<html><body><h2>Parcel Service</h2><br>");
		writer.append("<ul>");
		
		for (Integer id : parcels.keySet()) {
			writer.append("<li>");
			Parcel p = parcels.get(id);
			writer.append("<b>"+id+"</b>: from:" +  p.from + " to:"+ p.to + " Size:" + p.size + " State:"+ state[p.state]);
			writer.append("</li>");
		}
		
		
		writer.append("</ul>");
		writer.append("</body></html>");
		
		writer.flush();
		
		resp.setContentType("text/html");
		resp.setStatus(HttpServletResponse.SC_OK);
	
	}


	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		try {
			// parse event 
			SAXBuilder builder = new SAXBuilder();
			Document eventdoc = builder.build(request.getReader());
			Element event = eventdoc.getRootElement();			 	
			if (event.getName().equals("parcel")){				
				String from = event.getAttributeValue("from"); 
				String to = event.getAttributeValue("to"); 
				int size = Integer.parseInt(event.getAttributeValue("size"));		
				addParcel(from, to, size);
			} else if(event.getName().equals("alarm")) {
				System.out.println("asdf");
				String id = event.getAttributeValue("id"); 
				if (id.startsWith(myURL)) {
					int parcelnr = Integer.parseInt(id.split(myURL)[1]);
					changeParcelState(parcelnr);
				}
			} 
			
		} catch (Exception e) {
			e.printStackTrace();
		}		
		
		
		response.setStatus(HttpServletResponse.SC_OK);	
	}
	
	private void changeParcelState(int id) {
		if(! parcels.containsKey(id)){
			return;
		}
		
		
		Parcel parcel = parcels.get(id);		
		if(parcel.state == 0) {
			parcel.state++;
			sendMessage(parcel.to, "[Parcel Service] Das Paket  " + id + " wird heute ausgeliefert.");
		} else if(parcel.state == 1) {
			parcel.state++;
			sendMessage(parcel.from, "[Parcel Service] Das Paket  " + id + " wurde erfolgreich zugestellt.");
		}

		if(parcel.state < 2) {
			GregorianCalendar c = new GregorianCalendar();
			c.add(Calendar.MINUTE, 1);
			String day = c.get(Calendar.YEAR)+ "-" + c.get(Calendar.MONTH)+1+ "-" + c.get(Calendar.DAY_OF_MONTH); 
			String hour = c.get(Calendar.HOUR_OF_DAY) < 10 ? "0" + c.get(Calendar.HOUR_OF_DAY) : "" + c.get(Calendar.HOUR_OF_DAY);
			String minute = c.get(Calendar.MINUTE) < 10 ? "0" + (c.get(Calendar.MINUTE)) : "" + (c.get(Calendar.MINUTE));
			sendEvent("<alarm-registration day='"+day+"' hour='"+hour+"' minute='"+minute+"' id='"+myURL+id+"'/>");
		}
	}
	
	private boolean addParcel(String from, String to, int size) {
		Parcel p = new Parcel(from, to, size);
		int id = getID();
		parcels.put(id, p);		
		sendMessage(to, "[Parcel Service] Ein Paket von " + from + " mit der Trackingnummer "+ id +" wurde an Sie verschickt.");
		debit(from, size, id);
		GregorianCalendar c = new GregorianCalendar();
		c.add(Calendar.MINUTE, 1);
		String day = c.get(Calendar.YEAR)+ "-" + c.get(Calendar.MONTH)+1+ "-" + c.get(Calendar.DAY_OF_MONTH); 
		String hour = c.get(Calendar.HOUR_OF_DAY) < 10 ? "0" + c.get(Calendar.HOUR_OF_DAY) : "" + c.get(Calendar.HOUR_OF_DAY);
		String minute = c.get(Calendar.MINUTE) < 10 ? "0" + (c.get(Calendar.MINUTE)) : "" + (c.get(Calendar.MINUTE));
		sendEvent("<alarm-registration day='"+day+"' hour='"+hour+"' minute='"+minute+"' id='"+myURL+id+"'/>");
		return true;		
	}

	private void debit(String from, int size, int id) {
		String event = "<transfer to='parcelservice' from='"+from+"' amount='"+ getPostage(size) +"' comment='Portage for parcel"+ id +"' />";
		sendEvent(event);
	}
	
	private int getPostage(int size){
		return size * 2;
	}
	
	private class Parcel {
		String from;
		String to;
		int size;
		int state;
		
		public Parcel(String from, String to, int size){
			this.from = from;
			this.to = to;
			this.size = size;
			state = 0;
		}
	}
	
	private static synchronized int getID(){
		return id++;
	}
	
	private static int id = 0;

	private void sendMessage(String to, String message) {
		String event = "<mail from='parcel service' to='"+to+"'>"+message+"</mail>";
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
			
			writer.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>" + event);
			writer.flush();
			writer.close();			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	

}

