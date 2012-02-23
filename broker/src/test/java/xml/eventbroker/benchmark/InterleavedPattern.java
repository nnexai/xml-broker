package xml.eventbroker.benchmark;

public class InterleavedPattern extends Pattern {

	@Override
	public int generateNextIndex(int currentNo, int maxIndex) {
		return currentNo % maxIndex;
	}

}
