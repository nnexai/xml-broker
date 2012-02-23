package xml.eventbroker.benchmark;

public class SeqPattern extends Pattern {

	int seqLength = 10;

	public SeqPattern(int seqLength) {
		this.seqLength = seqLength;
	}

	@Override
	public int generateNextIndex(int currentNo, int maxIndex) {
		return (currentNo / seqLength) % maxIndex;
	}

}
