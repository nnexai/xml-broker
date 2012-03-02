package xml.eventbroker.benchmark;

import java.util.LinkedList;

public class FCFSStrategy extends Strategy {

	LinkedList<Connection> ll;

	public FCFSStrategy(int maxPersistentConnections) {
		super(maxPersistentConnections);
		ll = new LinkedList<Connection>();
	}

	@Override
	public void sendMessageOver(long sendTime, Connection connection) {
		if (!connection.isConnected()) {
			if (ll.size() < maxPersistentConnections) {
				ll.add(connection);
				connection.connect();
			}

		}
		connection.send();
	}

}
