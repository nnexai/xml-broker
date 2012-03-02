package xml.eventbroker.benchmark;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class BurstPattern extends Pattern {

	static class BurstData {
		int index;
		int sequenceLength;
		boolean longSeq = false;
	}

	int curBurstNo;

	private final Random rnd;

	public BurstPattern() {
		rnd = new Random();
		curBurstNo = 0;
		l = new ArrayList<BurstData>(100);
	}

	@Override
	public Pattern clone() {
		return new BurstPattern();
	}

	List<BurstData> l;

	private void generateNew(int maxIndex) {
		BurstData d = new BurstData();
		d.index = rnd.nextInt(maxIndex);
		d.sequenceLength = 1;
		d.sequenceLength += rnd.nextInt(10);
		if (rnd.nextFloat() > 0.90) {
			// long sequence
			d.sequenceLength += rnd.nextInt(1000) + 100;
			d.longSeq = true;
			longSeq++;
		} else
			shortSeq++;
		l.add(d);
	}

	int longSeq = 0;
	int shortSeq = 0;

	@Override
	public int generateNextIndex(int currentNo, int maxIndex) {
		if (l.size() == 0 || (l.size() < 2 * maxIndex && rnd.nextFloat() < 0.2)) {
			generateNew(maxIndex);
		}

		int curIndex = curBurstNo++ % l.size();

		BurstData d = l.get(curIndex);
		if (d.sequenceLength-- <= 0)
			l.remove(curIndex);
		return d.index;
	}

	@Override
	public String toString() {
		String str = super.toString();
		return str;
	}
}
