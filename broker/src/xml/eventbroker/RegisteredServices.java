package xml.eventbroker;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import xml.eventbroker.service.AbstractServiceEntry;

public class RegisteredServices {
	Map<String, List<AbstractServiceEntry>> regServ = new HashMap<String, List<AbstractServiceEntry>>();
	private final List<AbstractServiceEntry> EMPTY = Collections.emptyList();

	public Collection<AbstractServiceEntry> getServices(String eventType) {
		synchronized (regServ) {
			List<AbstractServiceEntry> inMem = regServ.get(eventType);
			return inMem != null 
					? new ArrayList<AbstractServiceEntry>(regServ.get(eventType))
					: EMPTY;
		}
	}

	public void registerService(Collection<? extends AbstractServiceEntry> services) {
		for (AbstractServiceEntry entry : services) {
			registerService(entry);
		}
	}

	public void registerService(AbstractServiceEntry entry) {
		synchronized (regServ) {
			List<AbstractServiceEntry> list = regServ.get(entry.getEvent());
			if (list == null) {
				list = new LinkedList<AbstractServiceEntry>();
				regServ.put(entry.getEvent(), list);
			}
			list.add(entry);
		}
	}
}
