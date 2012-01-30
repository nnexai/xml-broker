import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

import sun.net.www.protocol.http.HttpURLConnection;

public class StreamingConnectionTest {
	private static final String MSG2 = "<test>2</test>";
	private static final String MSG1 = "<test>1</test>";
	private static Logger logger = Logger.getAnonymousLogger();
	
	public static void main(String[] args) {
		
		HttpURLConnection con;
		OutputStreamWriter writer;
		URL url;
		
		try {
			url = new URL("http://localhost:8080/logging-app/XMLEventExample");
		con = (HttpURLConnection) url.openConnection();

		con.setRequestMethod("POST");
		con.setDoOutput(true);
		con.setDoInput(true);
		con.setFixedLengthStreamingMode(MSG1.length()+MSG2.length());
		con.setUseCaches(false);
		con.setRequestProperty("Connection", "keep-alive");
		// logging service should be set to timeout after ~20 seconds.. 
		con.setConnectTimeout(60 * 60 * 1000); // ONE-Hour
		con.setRequestProperty("Content-type", "text/xml");
		
		OutputStream out = con.getOutputStream();
		BufferedOutputStream bos = new BufferedOutputStream(out);
		writer = new OutputStreamWriter(bos, "UTF-8");
		
		try {
			writer.append(MSG1);
			writer.flush();
			out.flush();
		} catch (IOException e) {
			logger .log(Level.SEVERE, "This should have worked", e);
		} 
		
		Thread.sleep(30*1000);
		
		try {
			writer.append(MSG2);
			writer.flush();
			out.flush();
			logger.log(Level.SEVERE, "The connection should have just timed out!");
		} catch (IOException e) {
			logger.log(Level.INFO, "This is normal", e);
		} 
		
		out.close();
		System.out.println(con.getResponseMessage());
		
		} catch (MalformedURLException e) {
			logger.log(Level.SEVERE, "URLError", e);
		} catch (IOException e) {
			logger.log(Level.SEVERE, "ConnectionError", e);
		} catch (InterruptedException e) {
			logger.log(Level.WARNING, "Could not sleep well =)", e);
		} 

	}
}

