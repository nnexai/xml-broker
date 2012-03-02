package xml.eventbroker.benchmark;

import java.util.LinkedList;

public class FCFSWithAgingStrategy extends Strategy {

	static class AgingData implements ScoreData {
		private long lastSendTime;
	}

	LinkedList<Connection> ll;
	private final int timeout;

	public FCFSWithAgingStrategy(int maxPersistentConnections, int timeout) {
		super(maxPersistentConnections);
		ll = new LinkedList<Connection>();
		this.timeout = timeout;
	}

	@Override
	public void sendMessageOver(long currentTime, Connection connection) {
		AgingData d = (AgingData) connection.getScoreData();
		if (d == null) {
			d = new AgingData();
			connection.setScoreData(d);
		}
		d.lastSendTime = currentTime;

		if (!connection.isConnected()) {
			if (ll.size() >= maxPersistentConnections) {
				Connection oldest = ll.getFirst();
				long oldestTime = ((AgingData) (oldest.getScoreData())).lastSendTime;
				if (currentTime - oldestTime > timeout) {
					ll.removeFirst();
					oldest.disconnect();
					connection.connect();
					ll.add(connection);
				}
			} else {
				connection.connect();
				ll.add(connection);
			}
		}
		connection.send();
	}
}
