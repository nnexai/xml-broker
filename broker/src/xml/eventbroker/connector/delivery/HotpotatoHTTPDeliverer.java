package xml.eventbroker.connector.delivery;

import java.io.IOException;
import java.net.URI;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.util.logging.Logger;

import org.factor45.hotpotato.client.DefaultHttpClient;
import org.factor45.hotpotato.client.HttpClient;
import org.factor45.hotpotato.client.connection.factory.DefaultHttpConnectionFactory;
import org.factor45.hotpotato.client.connection.factory.PipeliningHttpConnectionFactory;
import org.factor45.hotpotato.client.factory.DefaultHttpClientFactory;
import org.factor45.hotpotato.client.host.factory.EagerDrainHostContextFactory;
import org.factor45.hotpotato.request.HttpRequestFuture;
import org.factor45.hotpotato.request.factory.ConcurrentHttpRequestFutureFactory;
import org.factor45.hotpotato.response.DiscardProcessor;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.handler.codec.http.DefaultHttpRequest;
import org.jboss.netty.handler.codec.http.HttpHeaders;
import org.jboss.netty.handler.codec.http.HttpMethod;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpVersion;

public class HotpotatoHTTPDeliverer implements IHTTPDeliverer {
	private static final Logger logger = Logger.getAnonymousLogger();
	private HttpClient client;
	
	@Override
	public void deliver(String event, URI url) throws IOException {
		HttpRequest request = new DefaultHttpRequest(HttpVersion.HTTP_1_1,
		                                             HttpMethod.POST, url.getPath());
		byte[] content = event.getBytes("UTF-8");
		request.setHeader(HttpHeaders.Names.CONTENT_LENGTH, content.length);
		request.setHeader(HttpHeaders.Names.CONTENT_TYPE, "text/xml");
		request.setHeader(HttpHeaders.Names.HOST, url.getHost());
		request.setContent(ChannelBuffers.copiedBuffer(content));
		HttpRequestFuture future = client.execute(url.getHost(), url.getPort(), request);
		//future.addListener(new ...)
//		future.awaitUninterruptibly();
	}

	@Override
	public void init() {
//		/*
		DefaultHttpClientFactory factory = new DefaultHttpClientFactory();
        //factory.setDebug(true);
        factory.setGatherEventHandlingStats(false);
        factory.setMaxConnectionsPerHost(10);
        factory.setUseNio(true);
//        factory.setConnectionFactory(new PipeliningHttpConnectionFactory());
        factory.setConnectionFactory(new DefaultHttpConnectionFactory());
        factory.setHostContextFactory(new EagerDrainHostContextFactory());
//        factory.setFutureFactory(new ConcurrentHttpRequestFutureFactory());
        factory.setConnectionTimeoutInMillis(20000);
        client = factory.getClient();
//		 */
//        client = new DefaultHttpClient();
        client.init();
	}

	@Override
	public void shutdown() {		
		client.terminate();
	}

	@Override
	public String toString() {
		return "[hotpotato-http-request]";
	}
}
