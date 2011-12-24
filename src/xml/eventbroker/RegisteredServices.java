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
	
	public void registerService(String eventType, String url) {
		List<String> list = regServ.get(eventType);
		if(list == null) {
			list = new LinkedList<String>();
			regServ.put(eventType, list);
		}
		list.add(url);
	}
}
