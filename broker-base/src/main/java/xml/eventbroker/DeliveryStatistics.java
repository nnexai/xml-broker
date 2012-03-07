package xml.eventbroker;

import java.util.concurrent.atomic.AtomicInteger;

public class DeliveryStatistics {
	AtomicInteger counter = new AtomicInteger(0);
	Object lock = new Object();

	public void addDelivery() {
		counter.incrementAndGet();
	}

	public void finishedDelivery() {
		if (counter.decrementAndGet() == 0)
			synchronized (lock) {
				lock.notifyAll();
			}
	}

	public int getPendingDeliveryCount() {
		return counter.get();
	}

	public void waitForPendingDeliveries() {
		while (counter.get() > 0)
			synchronized (lock) {
				try {
					lock.wait(5000);
				} catch (InterruptedException e) {
					// Want to be interrupted!
				}
			}
	}
}
