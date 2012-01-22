package xml.eventbroker;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import xml.eventbroker.service.AbstractServiceEntry;

public class RegisteredServices {
	private Map<String, List<AbstractServiceEntry>> regServ = new HashMap<String, List<AbstractServiceEntry>>();
	private final List<AbstractServiceEntry> EMPTY = Collections.emptyList();

	public void iterate(final IRegisteredServiceHandler h) {
		synchronized (regServ) {
			for (Entry<String, List<AbstractServiceEntry>> entry : regServ.entrySet()) {
				h.handleEventType(entry.getKey());
				
				for (AbstractServiceEntry srvEntry : entry.getValue()) {
					h.handleService(entry.getKey(), srvEntry);
				}
				
			}
		}
		
	}
	
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

	public boolean registerService(AbstractServiceEntry entry) {
		synchronized (regServ) {
			List<AbstractServiceEntry> list = regServ.get(entry.getEvent());
			if (list == null) {
				list = new LinkedList<AbstractServiceEntry>();
				regServ.put(entry.getEvent(), list);
			}
			
			for (AbstractServiceEntry lEntry : list) {
				if (entry.getID().equals(lEntry.getID()))
					return false;
			}
			
			list.add(entry);
		}
		return true;
	}

	public boolean unsubscribe(String event, String id) {
		boolean found = false;
		synchronized (regServ) {
			List<AbstractServiceEntry> list = regServ.get(event);
			if(list != null) {
				for (Iterator<AbstractServiceEntry> iterator = list.iterator(); iterator.hasNext();) {
					AbstractServiceEntry entry = iterator
							.next();
					if(entry.getID().equals(id)) {
						iterator.remove();
						found = true;
					}
				}
			}
		}
		return found;
	}
}
