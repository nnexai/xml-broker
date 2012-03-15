package xml.eventbroker.connector.delivery;

import java.net.URI;
import java.net.URISyntaxException;

public class BufferedDelivererTest {

	public static void main(String[] args) {

		AbstractHTTPDeliverer deliv = new BufferedNettyStreamingHTTPDeliverer(null);
		deliv.init(null);

		for (int i = 0; i < 500; i++) {
			try {
				deliv.enqueue("test" + i + " ", new URI("same"));
			} catch (URISyntaxException e) {
				e.printStackTrace();
			}

			if (i % 5 == 0)
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
		}

		deliv.shutdown();
		System.out.println("Finished");
	}
}
