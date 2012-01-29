package xml.eventbroker.callback;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class LongPollMap {
	long currentIndex = -1;
	
	private final Map<Long, HttpPollingContext> map;
	
	public LongPollMap() {
		this.map = new HashMap<Long, HttpPollingContext>();
	}
	
	public long register(HttpPollingContext ctx) {
		synchronized (map) {
			map.put(Long.valueOf(++currentIndex), ctx);
		}
		ctx.index = currentIndex;
		return currentIndex;
	}
	
	public void answer(long index, String answer) throws IOException {
		HttpPollingContext ctx = null;
		synchronized (map) {
			ctx = map.remove(Long.valueOf(index));
		}
		if(ctx != null)
			ctx.answer(answer);
	} 
}
