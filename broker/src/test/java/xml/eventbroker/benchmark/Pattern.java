package xml.eventbroker.benchmark;

public abstract class Pattern {

	public void sendMessage(Pool pool, int currentNo, int maxIndex) {
		pool.sendMessage(absoluteSendTime(currentNo, maxIndex),
				generateNextIndex(currentNo, maxIndex));
	}

	long absoluteSendTime(int currentNo, int maxIndex) {
		return (long) 10 * currentNo;
	};

	abstract int generateNextIndex(int currentNo, int maxIndex);

	@Override
	public String toString() {
		StringBuilder str = new StringBuilder(1000);
		for (int i = 0; i < 1000; i++)
			str.append((char) ('a' + generateNextIndex(i, 20)));
		return str.toString();
	}

	@Override
	public abstract Pattern clone();
}
