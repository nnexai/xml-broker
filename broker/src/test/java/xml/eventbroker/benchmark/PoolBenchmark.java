package xml.eventbroker.benchmark;

public class PoolBenchmark {

	private static void runTest(int messageCount, int connectionCount,
			int maxPersistentConnections, Pattern pattern, Strategy strat) {
		TimeRecorder rec = new TimeRecorder();
		Pool pool = new Pool(rec, connectionCount, strat);

		for (int i = 0; i < messageCount; i++)
			pattern.sendMessage(pool, i, connectionCount);

		System.out.println(strat);
		System.out.println(rec);
		System.out.println();
	}

	public static void main(String[] args) {
		int messageCount = 10000000;
		int connectionCount = 400;
		int maxPersistentConnections = 100;

		Pattern pattern = new RandomPattern();
		System.out.println("Used Pattern example: " + pattern);

		Strategy strat = new LRUStrategy(maxPersistentConnections);
		runTest(messageCount, connectionCount, maxPersistentConnections,
				pattern, strat);

		pattern = pattern.clone();
		strat = new FCFSStrategy(maxPersistentConnections);
		runTest(messageCount, connectionCount, maxPersistentConnections,
				pattern, strat);

		pattern = pattern.clone();
		strat = new FCFSWithAgingStrategy(maxPersistentConnections, 500);
		runTest(messageCount, connectionCount, maxPersistentConnections,
				pattern, strat);

		pattern = pattern.clone();
		strat = new OwnScoringStrategy(maxPersistentConnections, 20000);
		runTest(messageCount, connectionCount, maxPersistentConnections,
				pattern, strat);

		pattern = pattern.clone();
		strat = new NoStreamingStrategy(maxPersistentConnections);
		runTest(messageCount, connectionCount, maxPersistentConnections,
				pattern, strat);

	}

}
