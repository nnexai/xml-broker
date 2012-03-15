package xml.eventbroker.connector.delivery;

import java.io.IOException;
import java.net.URI;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
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

public class BufferedNettyStreamingHTTPDeliverer extends AbstractHTTPDeliverer {

	private final Logger log = Logger.getAnonymousLogger();
	ConcurrentMap<URI, ConsumerTask> bufferMap;

	class ConsumerTask implements Runnable {
		final BlockingQueue<String> buffer;
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

		public ConsumerTask(BlockingQueue<String> buffer, URI uri)
				throws IOException {
			this.buffer = buffer;
			this.pool = Executors.newSingleThreadExecutor();
			this.con = new PersistentConnection(uri, bootstrap, stats);
		}

		public void enqueue(String event) {
			if (finished.get())
				pool.execute(this);

			try {
				buffer.put(event);
			} catch (InterruptedException e) {
				log.log(Level.WARNING, "Could not add Event to Buffer", e);
			}

		}

		@Override
		public void run() {
			finished.set(false);
			StringBuilder events = new StringBuilder(0x1000);

			int cnt;
			do {
				cnt = 0;
				String event = null;

				try {
					if (buffer.isEmpty())
						Thread.sleep(30);
					while ((cnt < 5000) && (event = buffer.poll()) != null) {
						events.append(event);
						cnt++;
					}

					if (cnt < 10) {
						if (buffer.isEmpty())
							Thread.sleep(30);
						while ((cnt < 5000) && (event = buffer.poll()) != null) {
							events.append(event);
							cnt++;

						}
					}

					if (cnt > 0) {
						// add a fake message since we will count down
						// manually
						stats.addDelivery();
						con.pushEvent(events.toString());
						events.setLength(0);
						for (int i = 0; i < cnt; i++) {
							stats.finishedDelivery();

						}
					}
				} catch (InterruptedException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}

			} while (cnt > 0);
			finished.set(true);
		}
	}

	public BufferedNettyStreamingHTTPDeliverer(ExecutorService pool) {
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
			task = new ConsumerTask(new ArrayBlockingQueue<String>(6000),
					urlString);
			ConsumerTask cached = bufferMap.putIfAbsent(urlString, task);
			if (cached != null)
				task = cached;
		}
		task.enqueue(event);
	}
}
