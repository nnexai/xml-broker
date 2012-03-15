package xml.eventbroker.connector.delivery;

import java.io.IOException;
import java.net.URI;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;
import org.jboss.netty.handler.codec.http.HttpChunkAggregator;
import org.jboss.netty.handler.codec.http.HttpClientCodec;
import org.jboss.netty.util.internal.ConcurrentIdentityHashMap;

import xml.eventbroker.DeliveryStatistics;
import xml.eventbroker.connector.delivery.NettyStreamingHTTPDeliverer.ClientMessageHandler;
import xml.eventbroker.connector.delivery.NettyStreamingHTTPDeliverer.PersistentConnection;

public class BufferedNettyStreamingHTTPDeliverer3 extends AbstractHTTPDeliverer {

	private final Logger log = Logger.getAnonymousLogger();
	ConcurrentMap<URI, ConsumerTask> bufferMap;

	class ConsumerTask implements Runnable {

		final PersistentConnection con;
		public final AtomicBoolean finished = new AtomicBoolean(true);
		private final ExecutorService pool;

		public void shutdown() {
			try {
				pool.shutdown();
				pool.awaitTermination(500, TimeUnit.MILLISECONDS);
			} catch (InterruptedException e) {
				e.printStackTrace();
			} finally {
				con.disconnect();
			}
		}

		public ConsumerTask(URI uri) throws IOException {
			this.pool = Executors.newSingleThreadExecutor();
			this.con = new PersistentConnection(uri, bootstrap, stats);
		}

		StringBuilder events = new StringBuilder(0x10000);
		StringBuilder old = new StringBuilder(0x10000);

		protected void deliver() throws IOException {
			StringBuilder tmp;

			tmp = events;
			events = old;

			stats.addDelivery();
			con.pushEvent(tmp.toString());
			tmp.setLength(0);
			old = tmp;
			int deliv = cnt.get();
			for (int i = 0; i < deliv; i++)
				stats.finishedDelivery();
			cnt.getAndAdd(-deliv);
		}

		AtomicInteger cnt = new AtomicInteger(0);

		public void enqueue(String event) throws IOException {

			events.append(event);
			int countV = cnt.incrementAndGet();

			if (countV >= 1000) {
				deliver();
			}
		}

		@Override
		public void run() {
		}
	}

	public BufferedNettyStreamingHTTPDeliverer3(ExecutorService pool) {
		super(pool);
		bootstrap = new ClientBootstrap(new NioClientSocketChannelFactory(
				Executors.newCachedThreadPool(),
				Executors.newCachedThreadPool()));
	}

	@Override
	public void init(DeliveryStatistics stats) {
		bufferMap = new ConcurrentIdentityHashMap<URI, ConsumerTask>();
		// bufferMap = new ConcurrentHashMap<URI, ConsumerTask>();
		this.stats = stats;
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
		for (ConsumerTask task : bufferMap.values()) {
			task.shutdown();
		}
		bootstrap.releaseExternalResources();
	}

	@Override
	public void enqueue(String event, URI uri) {
		try {
			deliver(event, uri);
		} catch (IOException e) {
		}
	}

	final ClientBootstrap bootstrap;
	DeliveryStatistics stats;

	@Override
	protected void deliver(String event, URI urlString) throws IOException {
		ConsumerTask task;
		if ((task = bufferMap.get(urlString)) == null) {
			task = new ConsumerTask(urlString);
			ConsumerTask cached = bufferMap.putIfAbsent(urlString, task);
			if (cached != null)
				task = cached;
		}
		task.enqueue(event);
	}
}
