package xml.eventbroker.shop;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;

/**
 * Servlet implementation class TimerService
 */
public class TimerService extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private String eventBrokerURL;
    
	private Map<String, List<String>> alarm = new HashMap<String, List<String>>();
	ExecutorService pool;
	Timer clock;

	public void init(ServletConfig servletConfig) throws ServletException {
	    eventBrokerURL = servletConfig.getInitParameter("xmlbroker");

		pool = Executors.newCachedThreadPool();

		TimerTask t = new TimerTask() {
			public void run() {
				GregorianCalendar c = new GregorianCalendar();
				String day = c.get(Calendar.YEAR)+ "-" + c.get(Calendar.MONTH)+1+ "-" + c.get(Calendar.DAY_OF_MONTH); 
				String hour = c.get(Calendar.HOUR_OF_DAY) < 10 ? "0" + c.get(Calendar.HOUR_OF_DAY) : "" + c.get(Calendar.HOUR_OF_DAY);
				String minute = c.get(Calendar.MINUTE) < 10 ? "0" + c.get(Calendar.MINUTE) : "" + c.get(Calendar.MINUTE);
				String timestring = day+"#"+hour+"#"+minute;		

				String timeEvent = "<time day='"+day+"' hour='"+hour+"' minute='"+minute+"' />";  
				pool.execute(new EventSenderThread(timeEvent));	

				if(alarm.containsKey(timestring)){
					for(String id :  alarm.get(timestring)) {
						String alertEvent = "<alarm day='"+day+"' hour='"+hour+"' minute='"+minute+"' id='"+id+"' />";  
						pool.execute(new EventSenderThread(alertEvent));
					}
					
					alarm.remove(timestring);
				}
			}
		};

		clock = new Timer();
		clock.scheduleAtFixedRate(t, 0, 60000);
	}

	public void destroy(){
		clock.cancel();
		pool.shutdown();
	}
	
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		PrintWriter writer = response.getWriter();
		
		writer.append("<html><body><h2>Timer Service</h2><br><ul>");
		for (String s : alarm.keySet()) {
			for(String id : alarm.get(s)){
				writer.append("<li>"+ s + ": " + id+"</li>");
			}
		}
		writer.append("</ul></body></html>");
		
		writer.flush();
		
		response.setContentType("text/html");
		response.setStatus(HttpServletResponse.SC_OK);
		
	}

	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		try {
			// parse event 
			SAXBuilder builder = new SAXBuilder();
			Document eventdoc = builder.build(request.getReader());
			Element event = eventdoc.getRootElement();			 	
			if (event.getName().equals("alarm-registration")){				
				String day = event.getAttributeValue("day"); 
				String hour = event.getAttributeValue("hour"); 
				String minute = event.getAttributeValue("minute"); 
				String id = event.getAttributeValue("id"); 
				addAlarm(day, hour, minute, id);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}				
		
		response.setStatus(HttpServletResponse.SC_OK);		
		
	}
	
	private synchronized void addAlarm(String day, String hour, String minute, String id) {
		String s = day +"#"+ hour +"#"+ minute;
		if(alarm.containsKey(s)) {
			alarm.get(s).add(id);
		} else {
			List<String> ids = new ArrayList<String>();
			ids.add(id);
			alarm.put(s, ids);
		}
	}
	

	private class EventSenderThread extends Thread {
		
		private String event;
		
		public EventSenderThread(String event){
			this.event = event;
		}
				
		
		public void run(){
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
				System.out.println("[Timer Service] sending: " + event);
				writer.append("<?xml version=\"1.0\" encoding=\"utf-8\"?><events>" + event + "</events>");			
				writer.flush();
				writer.close();			
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	

}
