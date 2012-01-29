package xml.eventbroker.callback;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.AsyncContext;
import javax.servlet.ServletResponse;

public class HttpPollingContext {

	public long index;
	private final AsyncContext ctx;

	public HttpPollingContext(AsyncContext ctx) {
		this.ctx = ctx;
	}
	
	public void answer(String answer) throws IOException {
		ServletResponse response = ctx.getResponse();
		PrintWriter wrt = response.getWriter();
		wrt.append(answer);
		wrt.close();
	} 

}
