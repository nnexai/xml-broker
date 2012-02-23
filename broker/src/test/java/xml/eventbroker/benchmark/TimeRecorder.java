package xml.eventbroker.benchmark;

public class TimeRecorder {

	private static final int DISCONNECT_STREAMING = 10;
	private static final int SEND_STREAMING = 10;
	private static final int CONNECT_AND_SEND_ONCE = 100;
	private static final int CONNECT_STREAMING = 400;

	long totalDuration = 0;
	private int streamConnectCount;

	public long getTotalDuration() {
		return totalDuration;
	}

	public void timeStreamingConnect() {
		totalDuration += CONNECT_STREAMING;
		streamConnectCount++;
	}

	public void timeOneShotMessage() {
		totalDuration += CONNECT_AND_SEND_ONCE;
	}

	public void timeStreamingMessage() {
		totalDuration += SEND_STREAMING;
	}

	public void timeStreamingDisconnect() {
		totalDuration += DISCONNECT_STREAMING;
	}

	@Override
	public String toString() {
		StringBuilder str = new StringBuilder(0x1000);
		str.append("The whole Simulation took ").append(totalDuration)
				.append(" ms\n");
		str.append("Stream-Connection switches: ").append(streamConnectCount);
		return str.toString();
	}
}
