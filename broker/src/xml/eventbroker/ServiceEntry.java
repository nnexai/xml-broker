package xml.eventbroker;

public class ServiceEntry {
	private final String event;
	private final String url;
	
	public ServiceEntry(String event, String url) {
		super();
		this.event = event;
		this.url = url;
	}

	public String getEvent() {
		return event;
	}
	
	public String getUrl() {
		return url;
	}
}
