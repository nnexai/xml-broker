package xml.eventbroker;

public class TestStatistics {
	long sendingTimeInMs;
	long processingTimeInMs;
	
	int currentEvent = 0;
	final int maxEvent;
	
	double progress = 0;

	public TestStatistics(int maxEvent) {
		super();
		this.maxEvent = maxEvent;
	}
}
