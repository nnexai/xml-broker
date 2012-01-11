package xml.eventbroker;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class RegisteredServices {
	Map<String, List<String>> regServ = new HashMap<String, List<String>>();
	
	public Collection<String> getURL(String eventType) {
		return regServ.get(eventType);
	}
	
	public void registerService(Collection<? extends ServiceEntry> services) {
		for (ServiceEntry entry : services) {
			registerService(entry);
		}
	}
	
	public void registerService(ServiceEntry entry) {
		List<String> list = regServ.get(entry.getEvent());
		if(list == null) {
			list = new LinkedList<String>();
			regServ.put(entry.getEvent(), list);
		}
		list.add(entry.getUrl());
	}
}
