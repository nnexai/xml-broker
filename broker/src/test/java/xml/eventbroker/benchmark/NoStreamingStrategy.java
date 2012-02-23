package xml.eventbroker.benchmark;

import java.util.LinkedList;

public class NoStreamingStrategy extends Strategy {

	LinkedList<Connection> ll;

	public NoStreamingStrategy(int maxPersistentConnections) {
		super(maxPersistentConnections);
		ll = new LinkedList<Connection>();
	}

	@Override
	public void sendMessageOver(long sendTime, Connection connection) {
		connection.send();
	}

}
