import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

import sun.net.www.protocol.http.HttpURLConnection;

public class StreamingConnectionTest {

	private static Logger logger = Logger.getAnonymousLogger();
	
	public static void main(String[] args) {
		
		HttpURLConnection con;
		URL url;
		
		try {
			url = new URL("http://localhost:8080/logging-app/XMLEventExample");
		con = (HttpURLConnection) url.openConnection();

		final byte MSG1[] = "<test>1</test>".getBytes("UTF-8");
		final byte MSG2[] = "<test>2</test>".getBytes("UTF-8");
		
		con.setRequestMethod("POST");
		con.setDoOutput(true);
		con.setDoInput(true);
		con.setChunkedStreamingMode(-1);
//		con.setFixedLengthStreamingMode(MSG1.length+MSG2.length);
		con.setUseCaches(false);
		// needed, so that the sockets buffer is not reused!
		con.setRequestProperty("CONNECTION", "close");
		// logging service should be set to timeout after ~20 seconds.. 
		con.setConnectTimeout(2 * 1000); // 2 Seconds
		con.setReadTimeout(10*1000); // 10 Seconds for reading Answer
		con.setRequestProperty("CONTENT-TYPE", "text/xml");
		
		OutputStream out = con.getOutputStream();

		try {
			out.write(MSG1);
			out.flush();
			logger.info("Hopefully send MSG1");
		} catch (IOException e) {
			logger .log(Level.SEVERE, "This should have worked", e);
		} 
		
		Thread.sleep(30*1000);

		try {
			logger.info("Now trying to send MSG2");
			out.write(MSG2);
			out.flush();
			logger.log(Level.SEVERE, "The connection should have just timed out!");
		} catch (IOException e) {
			logger.log(Level.INFO, "This is normal", e);
		} 
		
		// also closes the stream
		System.out.println(con.getResponseMessage());

		} catch (UnsupportedEncodingException e) {
			logger.log(Level.SEVERE, "Could not translate Messages to byte array", e);
		} catch (MalformedURLException e) {
			logger.log(Level.SEVERE, "URLError", e);
		} catch (IOException e) {
			logger.log(Level.SEVERE, "ConnectionError", e);
		} catch (InterruptedException e) {
			logger.log(Level.WARNING, "Could not sleep well =)", e);
		} 

	}
}

