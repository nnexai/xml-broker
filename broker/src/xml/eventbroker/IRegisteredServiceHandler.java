package xml.eventbroker;

import xml.eventbroker.service.AbstractServiceEntry;

public interface IRegisteredServiceHandler {
	void handleEventType(String key);
	void handleService(String key, AbstractServiceEntry srvEntry);
}
