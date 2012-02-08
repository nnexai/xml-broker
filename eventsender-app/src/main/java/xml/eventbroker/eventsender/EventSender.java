package xml.eventbroker.eventsender;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;


public class EventSender extends HttpServlet {
	private static final long serialVersionUID = 1L;
    
	ExecutorService pool;
	private String eventBrokerURL;

	@Override
	public void init(ServletConfig servletConfig) throws ServletException {
		super.init();
    	eventBrokerURL = servletConfig.getInitParameter("xmlbroker");
		pool = Executors.newCachedThreadPool();
	}
	

    public EventSender() {
        super();
    }

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		resp.setContentType("text/html");
		PrintWriter writer = resp.getWriter();
		
		writer.append("<html><body>");
		writer.append("<form action='EventSender' method='post' enctype='multipart/form-data'>" +
					 "Select event file (xml): <input type='file' name='eventfile'> <br/><br/><b>OR</b><br/><br/>" + 
					  "&lt;events&gt;<br /> <textarea name='events' cols='75' rows='15'></textarea><br/>&lt;/events&gt;"+
				"<br/><br/><input type='submit' value='senden'> <input type='reset' /></form>");
		writer.append("</body></html>");
		
		writer.flush();
		
		resp.setStatus(HttpServletResponse.SC_OK);
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		
		String events = null;
		   try {
		        List<FileItem> items = new ServletFileUpload(new DiskFileItemFactory()).parseRequest(req);
		        for (FileItem item : items) {
		            if (item.isFormField()) {
		                // Process regular form field (input type="text|radio|checkbox|etc", select, etc).
		                String fieldname = item.getFieldName();
		                String fieldvalue = item.getString();
		                if (fieldname.equals("events") && ! fieldvalue.equals("")) {
		                	events = "<?xml version=\"1.0\" encoding=\"utf-8\"?><events>" + fieldvalue + "</events>";
		                }		                
		            } else {
		                InputStream filecontent = item.getInputStream();
		                BufferedReader br = new BufferedReader(new InputStreamReader(filecontent));
		                String line;
		                StringBuffer file = new StringBuffer();
		                while((line = br.readLine()) != null) {
		                	file.append(line);
		                }
		                
		                if(file.toString().startsWith("<?xml")) {
		                	events = file.toString();
		                }
		            }
		        }
		    } catch (FileUploadException e) {
		        throw new ServletException("Cannot parse multipart request.", e);
		    }

		PrintWriter writer = resp.getWriter();
		writer.append("<html>" +
				"<meta http-equiv='refresh' content='2; URL=EventSender'>" +
				"<body>");
			
		if (events != null && ! events.equals("")) {
			pool.execute(new EventSenderTask(eventBrokerURL, events));	
			writer.append("<b>Events werden verarbeitet.</b>");
		} else {
			writer.append("<b>Es wurden keine Events übergeben.</b>");
		}
	
		writer.append("</body></html>");	
		writer.flush();
		writer.close();

		resp.setStatus(HttpServletResponse.SC_OK); 
	}

}
