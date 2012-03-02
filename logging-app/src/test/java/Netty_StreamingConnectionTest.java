import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelHandler;
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;
import org.jboss.netty.handler.codec.http.DefaultHttpChunk;
import org.jboss.netty.handler.codec.http.DefaultHttpRequest;
import org.jboss.netty.handler.codec.http.HttpChunk;
import org.jboss.netty.handler.codec.http.HttpChunkAggregator;
import org.jboss.netty.handler.codec.http.HttpClientCodec;
import org.jboss.netty.handler.codec.http.HttpHeaders;
import org.jboss.netty.handler.codec.http.HttpMethod;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpResponse;
import org.jboss.netty.handler.codec.http.HttpVersion;

public class Netty_StreamingConnectionTest {

	private static Logger logger = Logger.getAnonymousLogger();

	public static class ClientMessageHandler extends SimpleChannelHandler {
	    @Override
	    public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) throws Exception {
	        e.getCause().printStackTrace();
	    }

	    @Override
	    public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
	        HttpResponse httpResponse = (HttpResponse) e.getMessage();
	        System.out.println(httpResponse.getStatus());
	    }
	}
	
	public static void main(String[] args) {

		try {
			final ClientBootstrap bootstrap = new ClientBootstrap(
					new NioClientSocketChannelFactory(
							Executors.newCachedThreadPool(),
							Executors.newCachedThreadPool()));
			bootstrap.setPipelineFactory(new ChannelPipelineFactory() {
				
				@Override
				public ChannelPipeline getPipeline() throws Exception {
					ChannelPipeline pipeline = Channels.pipeline(new HttpClientCodec());
					pipeline.addLast("aggregator", new HttpChunkAggregator(5242880));
					pipeline.addLast("someID", new ClientMessageHandler());
					return pipeline;
				}
			});

			final String host = "localhost";
			final int port = 8080;

			ChannelFuture future = bootstrap.connect(new InetSocketAddress(
					host, port));
			Channel channel = future.awaitUninterruptibly().getChannel();
			// Wait until the connection is closed or the connection attempt
			// fails.
			channel.getCloseFuture().addListener(new ChannelFutureListener() {
				@Override
				public void operationComplete(ChannelFuture future)
						throws Exception {
					System.out.println(future.toString());
					new Thread(new Runnable() {
						public void run() {
							// Shut down thread pools to exit
							// (cannot be executed in the same thread pool!
							bootstrap.releaseExternalResources();

							logger.log(Level.INFO, "Shutting down");
						}
					}).start();
				}
			});

			logger.info("Secured a Channel.. now lets go!");

			HttpRequest req = new DefaultHttpRequest(HttpVersion.HTTP_1_1,
					HttpMethod.POST, "/logging-app/XMLEventExample");

			req.setChunked(true);
			req.setHeader(HttpHeaders.Names.HOST, host + ':' + port);
			req.setHeader(HttpHeaders.Names.CONTENT_TYPE, "text/xml");
			req.setHeader(HttpHeaders.Names.ACCEPT,
					"text/html, image/gif, image/jpeg, *; q=.2, */*; q=.2");
			req.setHeader(HttpHeaders.Names.TRANSFER_ENCODING, "chunked");

			logger.info("Sending Request!");
			channel.write(req);

			logger.info("Sending MSG1");
			final byte MSG1[] = "<test>1</test>".getBytes("UTF-8");
			HttpChunk chunk = new DefaultHttpChunk(
					ChannelBuffers.copiedBuffer(MSG1));
			channel.write(chunk).awaitUninterruptibly();

			logger.info("Waiting!");
			Thread.sleep(30 * 1000);
			
			logger.info("Sending MSG2");
			final byte MSG2[] = "<test>2</test>".getBytes("UTF-8");
			chunk = new DefaultHttpChunk(ChannelBuffers.copiedBuffer(MSG2));
			ChannelFuture arg0 = channel.write(chunk).awaitUninterruptibly();
			if (arg0.isSuccess())
				System.out.println("Send MSG2!");
			else {
				System.out.println("Failed to sendMSG2!");
				if (arg0.getCause() != null)
					arg0.getCause().printStackTrace();
			}
			
			channel.write(HttpChunk.LAST_CHUNK);

		} catch (UnsupportedEncodingException e) {
			logger.log(Level.SEVERE,
					"Could not translate Messages to byte array", e);
		} catch (Exception e) {
			e.printStackTrace();
		}

	}
}
