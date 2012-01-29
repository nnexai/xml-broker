package xml.eventbroker;

import xml.eventbroker.connector.AbstractServiceEntry;

public interface IRegisteredServiceHandler {
	void handleEventType(String key);
	void handleService(String key, AbstractServiceEntry srvEntry);
}
