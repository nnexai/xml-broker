package xml.eventbroker.benchmark;

public class TestStatistics {
	long sendingTimeInMs = -1;
	long processingTimeInMs = -1;
	
	int currentEvent = 0;
	final int maxEvent;
	
	double progress = 0;

	public TestStatistics(int maxEvent) {
		super();
		this.maxEvent = maxEvent;
	}
	
	public String toXML() {
		StringBuilder str = new StringBuilder(100);
		str.append("<stats><send-events>").append(currentEvent);
		str.append("</send-events><exspected-events>").append(maxEvent);
		str.append("</exspected-events>");
		if(sendingTimeInMs != -1)
			str.append("<sending-time>").append(sendingTimeInMs).append("</sending-time>");
		if(processingTimeInMs != -1)
			str.append("<processing-time>").append(processingTimeInMs).append("</processing-time>");
		str.append("</stats>");
		return str.toString();
	}
}
