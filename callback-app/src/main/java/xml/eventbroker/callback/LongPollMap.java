package xml.eventbroker.callback;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

public class LongPollMap {
	private static final Logger logger = Logger.getAnonymousLogger();
	
	long currentIndex = -1;
	
	private final Map<Long, HttpPollingContext> map;
	
	public LongPollMap() {
		this.map = new HashMap<Long, HttpPollingContext>();
	}
	
	public long register(HttpPollingContext ctx) {
		synchronized (map) {
			map.put(Long.valueOf(++currentIndex), ctx);
		}
		return currentIndex;
	}
	
	public void answer(long index, String answer) throws IOException {
		HttpPollingContext ctx = null;
		synchronized (map) {
			ctx = map.remove(Long.valueOf(index));
		}
		if(ctx != null)
			ctx.answer(answer);
		else
			logger.warning("Recieved answer for non-existing connection!");
			
	} 
}
