package xml.eventbroker;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import xml.eventbroker.EventTestStream.IEventStreamStatusUpdate;

public class SpeedTest extends HttpServlet {

	private static final long serialVersionUID = 1L;

	AtomicBoolean running = new AtomicBoolean(false);

	// Immutable Data Object
	TestStatistics stats = null;
	
	//Object error = null; unused atm

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
		int ev, throu;
		String con, statUrl;
		
		public MeasureThread(int ev, int throu, String con, String statUrl) {
			this.ev = ev;
			this.throu = throu;
			this.con = con;
			this.statUrl = statUrl;
		}
		
		@Override
		public void run() {
		startMeasurement(ev, throu, con, statUrl);
		};
	};

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {

		if (running.compareAndSet(false, true)) {

			int events = 0x1000, throughput = -1;
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

			// start run
			new MeasureThread(events, throughput, connector, statisticsUrl).start(); 

			resp.setStatus(HttpServletResponse.SC_OK);
		} else {
			// already running
		}
	}

	public void startMeasurement(int noOfEvents, int throuput, String connector, String statisticsURL) {
		stats = null;
		//error = null;

		final XMLEventBroker broker = new XMLEventBroker();
		Class<? extends XMLEventBroker> clazz = broker.getClass();
		try {
			// Initialize the Broker
			broker.init();

			// Register the speed-statistics-app
			Field dynRegF = clazz.getDeclaredField("dynReg");
			dynRegF.setAccessible(true);
			DynamicRegistration dynReg = (DynamicRegistration) dynRegF
					.get(broker);
			byte[] regEvent = ("<HTTPConnector type=\"" + connector + "\" event=\"timed-event\" url=\""+statisticsURL+"\"/>")
					.getBytes("UTF-8");
			dynReg.subscribe(new ByteArrayInputStream(regEvent),
					"XMLBroker/speed-test/1");

			Method pM = clazz
					.getDeclaredMethod("processXML", InputStream.class);
			pM.setAccessible(true);

			// Get access to the threadpool, so we know when sending has
			// finished
			Field poolF = clazz.getDeclaredField("pool");
			poolF.setAccessible(true);
			ExecutorService pool = (ExecutorService) poolF.get(broker);

			final TestStatistics stats_local = new TestStatistics(noOfEvents);
			stats = stats_local;

			final IEventStreamStatusUpdate statCallback = new IEventStreamStatusUpdate() {

				@Override
				public void updateProgress(int currentEventNo, int maxEventNo,
						double percentage) {
					/*
					 System.out.println((int) (percentage * 10 + 0.5) / 10.
							+ "% [" + currentEventNo + '/' + maxEventNo + ']');
					*/
					stats_local.currentEvent = currentEventNo;
					stats_local.progress = percentage;
				}

				@Override
				public void signalThroughputNotAchieved(int currentEventNo,
						int sendPerWait, int desiredEventsPerSecond,
						double achievedEventsPerSecond) {
					System.out.println("Only achieved "
							+ (long) (achievedEventsPerSecond * 10 + 0.05)
							/ 10.0 + '/' + desiredEventsPerSecond
							+ " packets/s for the last " + sendPerWait
							+ " packets.");
				}
			};
			// TODO: WARMUP HERE
			System.out.println("GC");
			System.gc();
			System.out.println("Warmup");
			pM.invoke(
					broker,
					new ByteArrayInputStream(
							"<?xml version=\"1.0\" encoding=\"UTF-8\"?><events><warmup/></events>"
									.getBytes("UTF-8")));
			Thread.sleep(1000);

			// Start
			long start = System.nanoTime();
			pM.invoke(broker, new EventTestStream(noOfEvents, statCallback,
					throuput));
			stats_local.processingTimeInMs = ((System.nanoTime() - start) / 1000000);

			pool.shutdown();
			if (!pool.awaitTermination(10, TimeUnit.MINUTES))
				pool.shutdownNow();
			stats_local.sendingTimeInMs = ((System.nanoTime() - start) / 1000000);
			System.out.println("Finished!");
		} catch (SecurityException e) {
			e.printStackTrace();
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		} catch (ServletException e) {
			e.printStackTrace();
		} catch (NoSuchFieldException e) {
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		} finally {
			running.set(false);
		}
	}
}
