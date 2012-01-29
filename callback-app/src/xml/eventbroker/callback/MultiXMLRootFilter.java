package xml.eventbroker.callback;

import java.io.FilterReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;

public class MultiXMLRootFilter extends FilterReader {
	static enum ParserStatus {
		PARSING, ATTRIBUTE, QUOTE, OPENING_NODE, CLOSING_NODE,
	}

	char[] buf;
	int level = 0;

	int pendingBytes = 0;
	int pendingOffset = 0;

	boolean stop = false;
	boolean hasFinished = false;

	ParserStatus status = ParserStatus.PARSING;

	protected MultiXMLRootFilter(Reader in, int buffer) {
		super(in);
		this.buf = new char[buffer];
	}
	
	public boolean isFinished() {
		return hasFinished;
	}

	/**
	 * Often the class using this Stream tries to close it, after EOF has been reached. 
	 * Since this class fakes EOF on each XML-Boundary we need to hold the underlying channel open!
	 */
	@Override
	public void close() throws IOException {
		System.out.println("Someone tried to close the stream!");
	}
	
	/**
	 * Closes the underlying stream.
	 * @throws IOException
	 */
	public void forceClose() throws IOException {
		super.close();
	}
	
	@Override
	public int read(char[] cbuf, int off, int len) throws IOException {
		
		int r = 0;

		if (pendingBytes > 0) {
			if (stop) {
				System.out.println("Stop request on pending! -- read again to continue");
				stop = false;
				return -1;
			}
			
			// some Bytes pending from last batch --> use them 
			r = pendingBytes;
			pendingBytes = 0;
		} else {
			// nothing pending so read some new
			try {
				r = super.read(buf, 0, len);
			} catch (Exception e) {
				e.printStackTrace();
				r = -1;
			}

			if (r < 0) {
				this.hasFinished = true;
				return -1;
			}
			pendingOffset = 0;

			if (stop) {
				System.out.println("Stop request! -- read again to continue");
				stop = false;
				pendingBytes = r;
				return -1;
			}
		}

		System.out.println("Working on: " + "[" + r + "]"
				+ new String(buf, pendingOffset, r));

		int i;
		for (i = pendingOffset; (i < r + pendingOffset) & (pendingBytes == 0); i++) {
			parse(r, i);
		}


		r -= pendingBytes;
		

		System.out.println("Returning: " + r
				+ " from internal offset: " + pendingOffset
				+ " -- Remaining: " + pendingBytes);		
		System.out.println("Out: " + "[" + r + "]"
				+ new String(buf, pendingOffset, r));
		System.arraycopy(buf, pendingOffset, cbuf, off, r);
		if (pendingBytes > 0) {	
			pendingOffset = i;
		} 
		return r;
	}

	private void stop(int r, int i) {
		pendingBytes = r - i - 1;
		stop = true;
	}

	private void parse(int r, int i) {
		final char c = buf[i];

		if (status.equals(ParserStatus.OPENING_NODE)) {
			if (c == '/') {
				// if we found '</' at the beginning of a node good =)
				level--;
				status = ParserStatus.CLOSING_NODE;
			} else {
				level++;
				status = ParserStatus.ATTRIBUTE;
			}
		}

		switch (c) {
		case '/':
			if (status.equals(ParserStatus.ATTRIBUTE)) {
				// if we found '<... /' outside quotes we have a shortened
				// node
				level--;
				status = ParserStatus.CLOSING_NODE;
			}
			break;
		case '<':
			if (status.equals(ParserStatus.PARSING))
				status = ParserStatus.OPENING_NODE;
			break;
		case '>':
			if (!status.equals(ParserStatus.QUOTE)) {
				if (status.equals(ParserStatus.CLOSING_NODE) & (level == 0)) {
					stop(r, i);
				}

				status = ParserStatus.PARSING;
			}
			break;
		case '\'':
		case '"':
			if (status.equals(ParserStatus.ATTRIBUTE))
				status = ParserStatus.QUOTE;
			else if (status.equals(ParserStatus.QUOTE))
				status = ParserStatus.ATTRIBUTE;
			break;

		default:
			break;
		}
	}

	public static void main(String[] args) {
		/*
		 * Reader in = new MultiXMLRootFilter(new
		 * StringReader("<a attr=\"hallo\">" + "\"this is a test\"" + "</a>" +
		 * "<b/>" + "<c><d/>" + "</c>" + "<d />" + "<e>" + "some more body text"
		 * + "</e>"));
		 */
		/*
		 * Reader in = new MultiXMLRootFilter(new StringReader(
		 * "<a attr=\"hallo\">\"this is a test\"</a> <a/>"), 0x10);
		 */
		Reader in = new MultiXMLRootFilter(new StringReader(
				"<log attr=\"This should be logged\"></log>"), 0x10);

		try {
			while (true) {
				int r;
				StringBuilder b = new StringBuilder(0x100);
				char[] buf = new char[0x10];

				while ((r = in.read(buf)) > 0) {
					b.append(buf, 0, r);
				}

				System.out.println("--> " + b.toString());

				if (r == -2)
					break;
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
