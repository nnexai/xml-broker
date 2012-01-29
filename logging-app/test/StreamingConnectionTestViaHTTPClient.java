import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.EntityTemplate;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;

import sun.net.www.protocol.http.HttpURLConnection;

public class StreamingConnectionTestViaHTTPClient {

	public class RequestEntity extends EntityTemplate {
	    
	    private List<String> list;

		public RequestEntity(List<String> list) {
			super();
	        this.list = list;
	    }
	    
	    public void writeRequest(OutputStream out) throws IOException {
	    	OutputStreamWriter outW = new OutputStreamWriter(out);
	        try {
	        	for (String str : list) {
					outW.write(str);
					Thread.sleep(30*1000);
				}
	        } finally {
	        }
	    }

	    public long getContentLength() {
	        return -1;
	    }

		@Override
		public void consumeContent() throws IOException {
			// TODO Auto-generated method stub
			
		}

		@Override
		public InputStream getContent() throws IOException,
				IllegalStateException {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public Header getContentEncoding() {
			return null;
		}

		@Override
		public boolean isChunked() {
			return true;
		}

		@Override
		public boolean isStreaming() {
			return true;
		}

		@Override
		public void writeTo(OutputStream arg0) throws IOException {
			// TODO Auto-generated method stub
			
		}

		@Override
		public Header getContentType() {
			return contentEncoding;
		}

		@Override
		public boolean isRepeatable() {
			return false;
		}
	}

	
	private static Logger logger = Logger.getAnonymousLogger();

	public static void main(String[] args) {
		
		HttpURLConnection con;
		OutputStreamWriter writer;
		URL url;
		
		try {
			url = new URL("http://localhost:8080/xml-logging-app/XMLEventExample");
			
		HttpClient client = new DefaultHttpClient();
		
		HttpPost httpPost = new HttpPost(url.toURI());
		HttpEntity entity = new 

		con.setRequestMethod("POST");
		con.setDoOutput(true);
		con.setDoInput(true);
		con.setChunkedStreamingMode(-1);
		con.setUseCaches(false);
		con.setRequestProperty("Connection", "keep-alive");
		con.setConnectTimeout(60 * 60 * 1000); // ONE-Hour
		con.setRequestProperty("Content-type", "text/xml");
		
		OutputStream out = con.getOutputStream();
		BufferedOutputStream bos = new BufferedOutputStream(out);
		writer = new OutputStreamWriter(bos, "UTF-8");
		
		try {
			writer.append("<test>1</test>");
			writer.flush();
			out.flush();
		} catch (IOException e) {
			logger .log(Level.SEVERE, "This should have worked", e);
		} 
		
		Thread.sleep(30*1000);
		
		try {
			writer.append("<test>2</test>");
			writer.flush();
			out.flush();
			logger.log(Level.SEVERE, "The connection should have just timed out!");
		} catch (IOException e) {
			logger.log(Level.INFO, "This is normal", e);
		} 
		
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

