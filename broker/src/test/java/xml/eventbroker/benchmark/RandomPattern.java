package xml.eventbroker.benchmark;

import java.util.Random;

public class RandomPattern extends Pattern {

	Random rnd;

	public RandomPattern() {
		rnd = new Random();
	}

	@Override
	public Pattern clone() {
		return new RandomPattern();
	}

	@Override
	public int generateNextIndex(int currentNo, int maxIndex) {
		return rnd.nextInt(maxIndex);
	}

}
