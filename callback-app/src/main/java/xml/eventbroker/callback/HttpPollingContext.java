package xml.eventbroker.callback;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.AsyncContext;
import javax.servlet.ServletResponse;

public class HttpPollingContext {

	private final AsyncContext ctx;

	public HttpPollingContext(AsyncContext ctx) {
		this.ctx = ctx;
	}
	
	public void answer(String answer) throws IOException {
		System.out.println("Will answer: "+answer);
		ServletResponse response = ctx.getResponse();
		response.setContentLength(answer.length());
		PrintWriter wrt = response.getWriter();
		wrt.append(answer);
		wrt.close();
		ctx.complete();
	} 

}
