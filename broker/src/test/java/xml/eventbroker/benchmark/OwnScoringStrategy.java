package xml.eventbroker.benchmark;

import java.util.ArrayList;

public class OwnScoringStrategy extends Strategy {

	static class ScoredAgingData implements ScoreData {
		private long lastSendTime;
		double score = 0.0;
	}

	ArrayList<Connection> ll;
	int timeout;

	public OwnScoringStrategy(int maxPersistentConnections, int timeout) {
		super(maxPersistentConnections);
		ll = new ArrayList<Connection>(maxPersistentConnections + 1);
		this.timeout = timeout;
	}

	private double calculatedScore(double lastScore, long passedTime) {
		return lastScore
				* (passedTime < timeout ? 1 - (passedTime / (float) timeout)
						: 0);
	}

	private void insertAtRightPosition(Connection connection,
			ScoredAgingData d, long currentTime) {
		ll.add(connection);
		for (int i = 0; i < ll.size() - 1; i++) {
			ScoredAgingData oldScore = (ScoredAgingData) ll.get(i)
					.getScoreData();
			long oldestTime = currentTime - oldScore.lastSendTime;
			double oldestScore = calculatedScore(oldScore.score, oldestTime);
			if (d.score < oldestScore) {
				ll.remove(ll.size() - 1);
				// ll.removeLast();
				ll.add(i, connection);
				break;
			}
		}
	}

	@Override
	public void sendMessageOver(long currentTime, Connection connection) {
		ScoredAgingData d = (ScoredAgingData) connection.getScoreData();
		if (d == null) {
			d = new ScoredAgingData();
			connection.setScoreData(d);
		}
		long timeSinceLast = currentTime - d.lastSendTime;
		d.score = calculatedScore(d.score, timeSinceLast) + 1;
		d.lastSendTime = currentTime;

		if (!connection.isConnected()) {
			if (ll.size() >= maxPersistentConnections) {
				ScoredAgingData oldest = (ScoredAgingData) ll.get(0)
						.getScoreData();
				// ScoredAgingData oldest = (ScoredAgingData)
				// ll.getFirst().getScoreData();
				long oldestTime = currentTime - oldest.lastSendTime;
				double oldestScore = calculatedScore(oldest.score, oldestTime);
				if (d.score / oldestScore > 1.5) {
					ll.remove(0).disconnect();
					// ll.removeFirst().disconnect();
					connection.connect();
					ll.add(connection);
				}
			} else {
				connection.connect();
				insertAtRightPosition(connection, d, currentTime);
			}
		} else if (ll.remove(connection)) {
			// move to end
			insertAtRightPosition(connection, d, currentTime);
		} else {
			System.err.println(connection
					+ " was not in list but was connected either!");
		}
		connection.send();
	}
}
