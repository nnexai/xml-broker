package xml.eventbroker.example;

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

	ParserStatus status = ParserStatus.PARSING;

	protected MultiXMLRootFilter(Reader in, int buffer) {
		super(in);
		this.buf = new char[buffer];
	}

	@Override
	public int read(char[] cbuf, int off, int len) throws IOException {
		if (stop) {
			System.out.println("Stop request! -- read again to continue");
			stop = false;
			return -1;
		}

		int r;

		if (pendingBytes > 0) {
			r = pendingBytes;
			pendingBytes = 0;
		} else {
			r = super.read(buf, 0, len);
			if (r == -1)
				return -2;
			pendingOffset = 0;
		}

		System.out.println("Working on: " + "[" + r + "]"
				+ new String(buf, pendingOffset, r));

		int i;
		for (i = pendingOffset; (i < r + pendingOffset) & (pendingBytes == 0); i++) {
			char c = buf[i];

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
						pendingBytes = r - i - 1;
						stop = true;
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

		if (pendingBytes > 0) {
			int byteCount = r - pendingBytes;
			System.out.println("Returning: " + byteCount
					+ " from internal offset: " + pendingOffset
					+ " -- Remaining: " + pendingBytes);
			System.arraycopy(buf, pendingOffset, cbuf, off, byteCount);
			pendingOffset += i;
			return byteCount;
		} else {
			System.out.println("Returning: " + r + " from internal offset: "
					+ pendingOffset);
			System.arraycopy(buf, pendingOffset, cbuf, off, r);
			return r;
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
