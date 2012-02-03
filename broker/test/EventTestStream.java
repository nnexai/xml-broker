import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;

public class EventTestStream extends InputStream {
	private static final int WAIT_PER_X_EVENTS = 100;
	private final long waitTime;

	private byte[] currentEvent;
	private int currentEventOffset;

	private int currentEventNo = -1;
	private int noOfEvents = 0;

	public EventTestStream(int noOfEvents) {
		this(noOfEvents, Integer.MAX_VALUE);
	}

	public EventTestStream(int noOfEvents, int throughput) {
		this.noOfEvents = noOfEvents;
		
		if(throughput > 0 )
			waitTime = 1000 * WAIT_PER_X_EVENTS / throughput;
		else
			waitTime = 0;

	}

	StringBuilder strB = new StringBuilder(50);
	long lastTime;

	private boolean generateEvent() throws UnsupportedEncodingException {
		if (currentEventNo == 0)
			lastTime = System.nanoTime();
		else if (waitTime > 0 && currentEventNo % WAIT_PER_X_EVENTS == 0) {
			long timeDiffinMs = (System.nanoTime() - lastTime) / 1000000;
			long timeToWait = waitTime - timeDiffinMs;

			if (timeToWait > 0)
				try {
					Thread.sleep(timeToWait);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			else
				System.out.println("Could not achieve throughput -- ");

			lastTime = System.nanoTime();
		}
		if (currentEventNo > noOfEvents)
			return false;

		if (currentEvent != null && currentEventOffset < currentEvent.length)
			return true;

		if (currentEventNo % Math.max(noOfEvents / 100, 1) == 0) {
			System.out.println((100. * currentEventNo / noOfEvents) + "% ["
					+ currentEventNo + "]");
		}

		String str;

		if (currentEventNo == -1) {
			str = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><events>";
		} else if (currentEventNo == noOfEvents) {
			str = "</events>";
		} else {
			strB.append("<timed-event send-time=\"").append(System.nanoTime())
					.append("\"/>");
			str = strB.toString();
			strB.setLength(0);
		}
		currentEvent = str.getBytes("UTF-8");
		currentEventOffset = 0;
		currentEventNo++;
		return true;
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
		InputStreamReader isr = new InputStreamReader(new EventTestStream(10));
		StringBuilder str = new StringBuilder(0x1000);
		char[] buf = new char[100];
		int read;
		try {
			while ((read = isr.read(buf)) > -1) {
				str.append(buf, 0, read);
			}
			System.out.println(str.toString());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
