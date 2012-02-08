package xml.eventbroker.connector.delivery;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
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
import org.jboss.netty.handler.codec.http.HttpVersion;

public class NettyStreamingHTTPDeliverer implements IHTTPDeliverer {

	private static final Logger logger = Logger.getAnonymousLogger();

	class PersistentConnection {

		private static final int DISCONNECTED = 0;
		private static final int CONNECTED = 2;

		AtomicInteger state = new AtomicInteger(DISCONNECTED);
		private Channel connection;
		private final URI url;

		private void connect() throws IOException {

			if (state.get() == CONNECTED)
				return;

			logger.info("Connecting to " + url.toString());

			ChannelFuture future = bootstrap.connect(new InetSocketAddress(url
					.getHost(), url.getPort()));
			connection = future.awaitUninterruptibly().getChannel();
			connection.getCloseFuture().addListener(
					new ChannelFutureListener() {
						@Override
						public void operationComplete(ChannelFuture future)
								throws Exception {
							System.out.println(future.toString());
							state.set(DISCONNECTED);
						}
					});

			logger.info("Connected to " + url.toString());

			HttpRequest req = new DefaultHttpRequest(HttpVersion.HTTP_1_1,
					HttpMethod.POST, url.getPath());
			req.setChunked(true);
			req.setHeader(HttpHeaders.Names.HOST,
					url.getHost() + ':' + url.getPort());
			req.setHeader(HttpHeaders.Names.CONTENT_TYPE, "text/xml");
			req.setHeader(HttpHeaders.Names.ACCEPT,
					"text/html, image/gif, image/jpeg, *; q=.2, */*; q=.2");
			req.setHeader(HttpHeaders.Names.TRANSFER_ENCODING, "chunked");

			connection.write(req).awaitUninterruptibly();
			if (state.compareAndSet(DISCONNECTED, CONNECTED)) {
			} else {
				logger.warning("Called connect on already connected or pending connection");
			}
		}

		public void disconnect() {
			if (state.compareAndSet(CONNECTED, DISCONNECTED)
					|| state.compareAndSet(CONNECTED, DISCONNECTED)) {
				connection.close().awaitUninterruptibly();
			}
		}

		public PersistentConnection(URI url) throws IOException {
			this.url = url;
		}

		public void pushEvent(String event) throws IOException {
			if (state.get() == DISCONNECTED || connection == null
					|| !connection.isConnected()) {
				synchronized (this) {
					connect();
				}
			}
			HttpChunk chunk = new DefaultHttpChunk(
					ChannelBuffers.copiedBuffer(event.getBytes("UTF-8")));
			connection.write(chunk).awaitUninterruptibly();
		}
	}

	public static class ClientMessageHandler extends SimpleChannelHandler {
		// @Override
		// public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent
		// e)
		// throws Exception {
		// e.getCause().printStackTrace();
		// }

		@Override
		public void messageReceived(ChannelHandlerContext ctx, MessageEvent e)
				throws Exception {
			// HttpResponse httpResponse = (HttpResponse) e.getMessage();
			e.getChannel().close();
		}
	}

	Map<URI, PersistentConnection> map;

	final ClientBootstrap bootstrap = new ClientBootstrap(
			new NioClientSocketChannelFactory(Executors.newCachedThreadPool(),
					Executors.newCachedThreadPool()));

	@Override
	public void init() {
		map = Collections
				.synchronizedMap(new LinkedHashMap<URI, PersistentConnection>());

		bootstrap.setPipelineFactory(new ChannelPipelineFactory() {

			@Override
			public ChannelPipeline getPipeline() throws Exception {
				ChannelPipeline pipeline = Channels
						.pipeline(new HttpClientCodec());
				pipeline.addLast("aggregator", new HttpChunkAggregator(5242880));
				pipeline.addLast("someID", new ClientMessageHandler());
				return pipeline;
			}
		});

	}

	@Override
	public void shutdown() {
		for (PersistentConnection con : map.values()) {
			con.disconnect();
		}

		bootstrap.releaseExternalResources();

	}

	@Override
	public void deliver(String event, URI urlString) throws IOException {
		PersistentConnection con;
		synchronized (map) {
			if ((con = map.get(urlString)) == null) {
				con = new PersistentConnection(urlString);
				map.put(urlString, con);
			}
		}
		con.pushEvent(event);
	}

	@Override
	public String toString() {
		return "[streaming-http-request]";
	}
}
