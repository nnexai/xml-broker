package xml.eventbroker.example;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.LinkedList;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringEscapeUtils;

public class ExampleService extends HttpServlet {

	private static final long serialVersionUID = 1L;

	List<String> events;
	
	@Override
	public void init() throws ServletException {
		super.init();
		events = new LinkedList<String>();
	}
	
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		PrintWriter writer = resp.getWriter();
		
		writer.append("<html><body>");
		writer.append("<ul>");
		
		for (String event : events) {
			writer.append("<li>");
			writer.append(StringEscapeUtils.escapeXml(event));
			writer.append("</li>");
		}
		
		writer.append("</ul>");
		writer.append("</body></html>");
		
		writer.flush();
		
		resp.setStatus(HttpServletResponse.SC_OK);
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		BufferedReader reader = req.getReader();
		
		StringBuilder buffer = new StringBuilder(0x1000);
		String r = null;
		while( (r = reader.readLine()) != null)
			buffer.append(r);
		
		events.add(buffer.toString());
		reader.close();
		
		resp.setStatus(HttpServletResponse.SC_OK);
	}
	
}
