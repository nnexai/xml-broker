package xml.eventbroker.benchmark;

import java.util.LinkedList;

public class LRUStrategy extends Strategy {

	LinkedList<Connection> ll;

	public LRUStrategy(int maxPersistentConnections) {
		super(maxPersistentConnections);
		ll = new LinkedList<Connection>();
	}

	@Override
	public void sendMessageOver(long sendTime, Connection connection) {
		if (!connection.isConnected()) {
			if (ll.size() >= maxPersistentConnections) {
				Connection oldest = ll.removeFirst();
				oldest.disconnect();
			}

			ll.add(connection);
			connection.connect();
		}
		connection.send();
	}

}
