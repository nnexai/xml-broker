package xml.eventbroker.benchmark;

public abstract class Strategy {
	protected final int maxPersistentConnections;

	public Strategy(int maxPersistentConnections) {
		this.maxPersistentConnections = maxPersistentConnections;
	}

	public abstract void sendMessageOver(long sendTime, Connection connection);

	@Override
	public String toString() {
		return getClass().getSimpleName();
	}
}
