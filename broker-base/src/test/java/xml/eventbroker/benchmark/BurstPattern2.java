package xml.eventbroker.benchmark;

import java.util.LinkedList;
import java.util.List;
import java.util.Random;

public class BurstPattern2 extends Pattern {

	static class BurstData {
		int index;
		int sequenceLength;
	}

	int curBurstNo;

	private final Random rnd;

	public BurstPattern2() {
		rnd = new Random();
		curBurstNo = 0;
		l = new LinkedList<BurstData>();
		lastRemoved = new LinkedList<Integer>();
	}

	@Override
	public Pattern clone() {
		return new BurstPattern2();
	}

	List<BurstData> l;
	List<Integer> lastRemoved;

	private void generateNew(int maxIndex) {
		BurstData d = new BurstData();
		d.index = rnd.nextFloat() > 0.9 ? getRemovedIndex() : rnd
				.nextInt(maxIndex);
		d.sequenceLength = 1;
		d.sequenceLength += rnd.nextInt(10);
		if (rnd.nextFloat() > 0.99)
			d.sequenceLength += rnd.nextInt(400) + 100;

		l.add(d);
	}

	private int getRemovedIndex() {
		return lastRemoved.size() > 0 ? lastRemoved.remove(rnd
				.nextInt(lastRemoved.size())) : 0;
	}

	@Override
	public int generateNextIndex(int currentNo, int maxIndex) {
		if (l.size() <= 2 || rnd.nextInt(20) == 0) {
			generateNew(maxIndex);
		}

		int curIndex = curBurstNo % l.size();
		if (rnd.nextFloat() > 0.6)
			curBurstNo++;
		BurstData d = l.get(curIndex);
		if (--d.sequenceLength <= 0) {
			lastRemoved.add(l.remove(curIndex).index);
		}

		return d.index;
	}

	@Override
	public String toString() {
		String str = super.toString();
		curBurstNo = 0;
		l = new LinkedList<BurstData>();
		lastRemoved = new LinkedList<Integer>();
		return str;
	}
}
