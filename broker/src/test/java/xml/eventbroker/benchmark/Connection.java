package xml.eventbroker.benchmark;

public class Connection {

	private final TimeRecorder timer;

	public Connection(TimeRecorder timer) {
		this.timer = timer;
	}

	private boolean connected = false;

	public boolean isConnected() {
		return connected;
	}

	private ScoreData scoreData = null;

	public void setScoreData(ScoreData scoreData) {
		this.scoreData = scoreData;
	}

	public ScoreData getScoreData() {
		return scoreData;
	}

	public void connect() {
		connected = true;
		timer.timeStreamingConnect();
	}

	public void disconnect() {
		connected = false;
		timer.timeStreamingDisconnect();
	}

	public void send() {
		if (connected)
			streamingMessage();
		else
			oneShotMessage();
	}

	private void streamingMessage() {
		timer.timeStreamingMessage();
	}

	private void oneShotMessage() {
		timer.timeOneShotMessage();
	}
}
