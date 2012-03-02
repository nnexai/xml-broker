package xml.eventbroker.benchmark;

public class Pool {
	private final Connection[] connectionMap;

	private final Strategy strategy;

	public Pool(TimeRecorder rec, int connectionCount, Strategy strategy) {
		connectionMap = new Connection[connectionCount];
		this.strategy = strategy;

		for (int i = 0; i < connectionCount; i++)
			connectionMap[i] = new Connection(rec);
	}

	public void sendMessage(long sendTime, int connectionIndex) {
		strategy.sendMessageOver(sendTime, connectionMap[connectionIndex]);
	}

}
