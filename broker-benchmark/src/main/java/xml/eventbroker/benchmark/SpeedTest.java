package xml.eventbroker.benchmark;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.ParserConfigurationException;

import xml.eventbroker.DeliveryStatistics;
import xml.eventbroker.XMLEventBroker;
import xml.eventbroker.benchmark.EventTestStream.IEventStreamStatusUpdate;
import xml.eventbroker.connector.ServiceConnectorFactory;

public class SpeedTest extends HttpServlet {

	private static final long serialVersionUID = 1L;

	private static final Logger LOG = Logger.getAnonymousLogger();

	AtomicBoolean running = new AtomicBoolean(false);

	// Immutable Data Object
	TestStatistics stats = null;

	// Object error = null; unused atm

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		if (stats != null) {
			resp.setContentType("text/xml");
			ServletOutputStream out = resp.getOutputStream();
			out.write(stats.toXML().getBytes("UTF-8"));
		}
	}

	private class MeasureThread extends Thread {
		int ev, throu, serviceCount;
		String con, statUrl;

		public MeasureThread(int ev, int throu, String con, String statUrl,
				int serviceCount) {
			this.ev = ev;
			this.throu = throu;
			this.con = con;
			this.statUrl = statUrl;
			this.serviceCount = serviceCount;
		}

		@Override
		public void run() {
			startMeasurement(ev, throu, con, statUrl, serviceCount);
		};
	};

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {

		if (running.compareAndSet(false, true)) {

			int events = 0x1000, throughput = -1, serviceCount = 1;
			String connector = "PooledHTTPDeliverer", statisticsUrl = "http://localhost:8080/speed-statistics/SpeedStatistics";

			String statsUrlS = req.getParameter("test.url");
			if (statsUrlS != null && !"".equals(statsUrlS))
				statisticsUrl = statsUrlS;

			String conS = req.getParameter("test.type");
			if (conS != null && !"".equals(conS))
				connector = conS;

			String throughS = req.getParameter("test.throughput");
			if (throughS != null && !"".equals(throughS))
				throughput = Integer.valueOf(throughS);

			String evtCntS = req.getParameter("test.eventcount");
			if (evtCntS != null && !"".equals(evtCntS))
				events = Integer.valueOf(evtCntS);

			String serviceCountS = req.getParameter("test.service_count");
			if (serviceCountS != null && !"".equals(serviceCountS))
				serviceCount = Integer.valueOf(serviceCountS);

			// start run
			new MeasureThread(events, throughput, connector, statisticsUrl,
					serviceCount).start();

			resp.setStatus(HttpServletResponse.SC_OK);
		} else {
			// already running
		}
	}

	private void registerServices(String connector, String statisticsURL,
			XMLEventBroker broker, int serviceCount)
			throws UnsupportedEncodingException {
		if (serviceCount == 1) {
			byte[] regEvent = ("<HTTPConnector type=\"" + connector
					+ "\" event=\"timed-event\" url=\"" + statisticsURL + "/\"/>")
					.getBytes("UTF-8");
			broker.subscribe(new ByteArrayInputStream(regEvent),
					"XMLBroker/speed-test/");
		} else
			for (int i = 1; i <= serviceCount; i++) {
				byte[] regEvent = ("<HTTPConnector type=\"" + connector
						+ "\" event=\"timed-event" + i + "\" url=\""
						+ statisticsURL + '/' + i + "\"/>").getBytes("UTF-8");
				broker.subscribe(new ByteArrayInputStream(regEvent),
						"XMLBroker/speed-test/" + i);
			}
	}

	public void startMeasurement(int noOfEvents, int throuput,
			String connector, String statisticsURL, final int serviceCount) {
		stats = null;
		// error = null;

		final XMLEventBroker broker = new XMLEventBroker();
		Class<? extends XMLEventBroker> clazz = broker.getClass();
		try {
			// Initialize the Broker
			broker.init();

			registerServices(connector, statisticsURL, broker, serviceCount);

			// Get access to the threadpool, so we know when sending has
			// finished
			Field poolF = clazz.getDeclaredField("pool");
			poolF.setAccessible(true);
			ExecutorService pool = (ExecutorService) poolF.get(broker);

			Field factoryF = clazz.getDeclaredField("factory");
			factoryF.setAccessible(true);
			ServiceConnectorFactory factory = (ServiceConnectorFactory) factoryF
					.get(broker);

			Field delivStatsF = clazz.getDeclaredField("stats");
			delivStatsF.setAccessible(true);
			DeliveryStatistics delivStats = (DeliveryStatistics) delivStatsF
					.get(broker);

			final TestStatistics stats_local = new TestStatistics(noOfEvents);
			stats = stats_local;

			final IEventStreamStatusUpdate statCallback = new IEventStreamStatusUpdate() {

				@Override
				public void updateProgress(int currentEventNo, int maxEventNo,
						double percentage) {
					stats_local.currentEvent = currentEventNo;
					stats_local.progress = percentage;
				}

				@Override
				public void signalThroughputNotAchieved(int currentEventNo,
						int sendPerWait, int desiredEventsPerSecond,
						double achievedEventsPerSecond) {
				}
			};
			// TODO: WARMUP HERE
			LOG.info("GC");
			System.gc();
			LOG.info("Warmup");
			broker.processXML(new ByteArrayInputStream(
					"<?xml version=\"1.0\" encoding=\"UTF-8\"?><warmup/>"
							.getBytes("UTF-8")));
			Thread.sleep(1000);

			// Start
			long start = System.nanoTime();
			EventTestStream eventTestStream = new EventTestStream(noOfEvents,
					statCallback, throuput);
			if (serviceCount > 1) {
				eventTestStream = new EventTestStream(noOfEvents, statCallback,
						throuput) {

					@Override
					protected String getEventName(long currentEventNo) {
						return "timed-event"
								+ (currentEventNo % serviceCount + 1);
					}
				};
			}
			broker.processXML(eventTestStream);
			stats_local.processingTimeInMs = ((System.nanoTime() - start) / 1000000);

			// wait for all pending sends to finish
			LOG.info("Waiting");
			delivStats.waitForPendingDeliveries();
			LOG.info("Finished");
			stats_local.sendingTimeInMs = ((System.nanoTime() - start) / 1000000);

			factory.shutdown();
			pool.shutdown();
			if (!pool.awaitTermination(1, TimeUnit.HOURS))
				pool.shutdownNow();
		} catch (SecurityException e) {
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (NoSuchFieldException e) {
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			running.set(false);
		}
	}
}
