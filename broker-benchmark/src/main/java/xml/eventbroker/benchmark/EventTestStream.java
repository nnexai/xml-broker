package xml.eventbroker.benchmark;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.Scanner;

import xml.eventbroker.shared.MultiXMLRootFilter;

public class EventTestStream extends InputStream {

	public static interface IEventStreamStatusUpdate {
		void updateProgress(int currentEventNo, int maxEventNo,
				double percentage);

		void signalThroughputNotAchieved(int currentEventNo, int sendPerWait,
				int desiredEventsPerSecond, double achievedEventsPerSecond);
	}

	private static final int WAIT_PER_X_EVENTS = 100;
	private final long waitTime;
	private final int desiredEventsPerSecond;

	private byte[] currentEvent;
	private int currentEventOffset;

	private int currentEventNo = -1;
	private int maxEventNo = 0;

	private final IEventStreamStatusUpdate statUpdateCallback;

	public EventTestStream(int noOfEvents,
			IEventStreamStatusUpdate statUpdateCallback) {
		this(noOfEvents, statUpdateCallback, Integer.MAX_VALUE);
	}

	public EventTestStream(int noOfEvents,
			IEventStreamStatusUpdate statUpdateCallback, int throughput) {
		this.maxEventNo = noOfEvents;
		this.statUpdateCallback = statUpdateCallback;
		this.desiredEventsPerSecond = throughput;

		if (throughput > 0)
			waitTime = 1000 * WAIT_PER_X_EVENTS / throughput;
		else
			waitTime = 0;

	}

	StringBuilder strB = new StringBuilder(70);
	long lastTime;

	private boolean generateEvent() throws UnsupportedEncodingException {

		if (currentEventNo >= maxEventNo) {
			statUpdateCallback.updateProgress(currentEventNo, maxEventNo, 100);
			return false;
		} else if (currentEvent != null
				&& currentEventOffset < currentEvent.length)
			return true;

		/*
		 * // Update Progress if ((statUpdateCallback != null) &&
		 * (currentEventNo % Math.max(maxEventNo / 50, 2) == 0)) {
		 * statUpdateCallback.updateProgress(currentEventNo, maxEventNo,
		 * 100.*currentEventNo/maxEventNo); }
		 */
		// Update Progress
		if ((statUpdateCallback != null)
				&& (currentEventNo
						% Math.min(Math.max(maxEventNo / 100, 2), 500) == 0)) {
			statUpdateCallback.updateProgress(currentEventNo, maxEventNo, 100.
					* currentEventNo / maxEventNo);
		}

		// Wait if we send too much
		if (currentEventNo == 0)
			lastTime = System.nanoTime();
		else if ((waitTime > 0) && (currentEventNo % WAIT_PER_X_EVENTS) == 0) {
			long timeDiffinMs = (System.nanoTime() - lastTime) / 1000000;
			long timeToWait = waitTime - timeDiffinMs;

			if (timeToWait > 0)
				try {
					Thread.sleep(timeToWait);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			else if (statUpdateCallback != null) {
				final double achievedEventsPerSecond = WAIT_PER_X_EVENTS
						* 1000. / timeDiffinMs;
				statUpdateCallback.signalThroughputNotAchieved(currentEventNo,
						WAIT_PER_X_EVENTS, desiredEventsPerSecond,
						achievedEventsPerSecond);
			}

			lastTime = System.nanoTime();
		}

		// Generate Event
		String str;

		if (currentEventNo == -1) {
			str = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";
		} else {
			strB.append('<').append(getEventName(currentEventNo));
			strB.append(" send-time=\"").append(System.nanoTime())
					.append("\" id=\"").append(currentEventNo).append("\"/>");
			str = strB.toString();
			strB.setLength(0);
		}
		currentEvent = str.getBytes("UTF-8");
		currentEventOffset = 0;
		currentEventNo++;
		return true;
	}

	protected String getEventName(long currentEventNo) {
		return "timed-event";
	}

	@Override
	public int read() throws IOException {
		if (!generateEvent())
			return -1;

		return currentEvent[currentEventOffset++];
	}

	@Override
	public int read(byte[] b, int off, int len) throws IOException {
		if (!generateEvent())
			return -1;
		int remaining = currentEvent.length - currentEventOffset;

		int read = Math.min(remaining, len);
		System.arraycopy(currentEvent, currentEventOffset, b, off, read);
		currentEventOffset += read;
		return read;
	}

	public static void main(String[] args) {
		MultiXMLRootFilter isr = new MultiXMLRootFilter(new InputStreamReader(
				new EventTestStream(1000001, new IEventStreamStatusUpdate() {

					@Override
					public void updateProgress(int currentEventNo,
							int maxEventNo, double percentage) {

					}

					@Override
					public void signalThroughputNotAchieved(int currentEventNo,
							int sendPerWait, int desiredEventsPerSecond,
							double achievedEventsPerSecond) {

					}
				})), 1000);
		char[] buf = new char[1000];
		int read;
		long cnt = 0;

		Scanner sc = new Scanner(System.in);
		// Wait for Enter
		System.out.println("Press Enter to Warmup");
		sc.nextLine();

		try {
			// First read to allow benchmark tools to instrument classes
			if (isr.hasNext()) {
				while ((read = isr.read(buf)) > 0) {
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

		System.out.println("Warmup complete. Press Enter to run benchmark");
		sc.nextLine();

		long time = System.currentTimeMillis();

		try {
			while (isr.hasNext()) {
				while ((read = isr.read(buf)) > 0) {
				}
				cnt++;
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		System.out.println(cnt + " in "
				+ (System.currentTimeMillis() - time + "ms"));
	}
}
