package xml.eventbroker.callback;

import java.io.FilterReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;

public class MultiXMLRootFilter extends FilterReader {
	private static final boolean DEBUG = false;

	static enum ParserStatus {
		PARSING, ERROR, CONSUME_ROOT_ELEMENT, TAG_OPEN, COMMENT_INTRO_1, COMMENT_INTRO_2, COMMENT, COMMENT_OUTRO_1, COMMENT_OUTRO_2, INSTRUCTION, INSTRUCTION_OUTRO, ELEMENT, ATTRIB_VALUE, ELEMENT_CLOSING, ELEMENT_SINGLE_TAG,
	}

	private char[] buf;
	private int level = 0;

	private int pendingBytes = 0;
	private int pendingOffset = 0;

	private boolean stop = false;
	private boolean finished = false;

	private ParserStatus status = ParserStatus.PARSING;

	protected MultiXMLRootFilter(Reader in, int buffer) {
		super(in);
		this.buf = new char[buffer];
	}

	public boolean hasFinished() {
		return finished;
	}

	/**
	 * Often the class using this Stream tries to close it, after EOF has been
	 * reached. Since this class fakes EOF on each XML-Boundary we need to hold
	 * the underlying channel open!
	 */
	@Override
	public void close() throws IOException {
		if (DEBUG)
			System.out.println("Someone tried to close the stream!");
	}

	/**
	 * Closes the underlying stream.
	 * 
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
				if (DEBUG)
					System.out
							.println("Stop request on pending! -- read again to continue");
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
				this.finished = true;
				return -1;
			}
			pendingOffset = 0;

			if (stop) {
				if (DEBUG)
					System.out
							.println("Stop request! -- read again to continue");
				stop = false;
				pendingBytes = r;
				return -1;
			}
		}

		if (DEBUG)
			System.out.println("Working on: " + "[" + r + "]"
					+ new String(buf, pendingOffset, r));

		int i;
		for (i = pendingOffset; (i < r + pendingOffset) & (pendingBytes == 0); i++) {
			parse(r, i);
		}

		r -= pendingBytes;

		if (DEBUG) {
			System.out.println("Returning: " + r + " from internal offset: "
					+ pendingOffset + " -- Remaining: " + pendingBytes);
			System.out.println("Out: " + "[" + r + "]"
					+ new String(buf, pendingOffset, r));
		}

		System.arraycopy(buf, pendingOffset, cbuf, off, r);
		if (pendingBytes > 0) {
			pendingOffset += r;
		}
		return r;
	}

	private void stop(int r, int i) {
		pendingBytes = r - i - 1;
		stop = true;
	}

	private void parse(int r, int i) {
		ParserStatus old = status;

		final char c = buf[i];
		switch (status) {

		case ERROR:
			break;

		// try to get around case where the root-element is followed by
		// whitespace.
		// do not want to split the last part so that we get a part containing
		// only whitespace
		// since this will lead to a parse-exception in SAX etc! Thats why we
		// take all whitespace
		// following a root-element as belonging to it.
		case CONSUME_ROOT_ELEMENT:
			if (c == '<') {
				stop(r, i - 1);
				status = ParserStatus.TAG_OPEN;
			} else if (!Character.isSpaceChar(c))
				status = ParserStatus.ERROR;
			break;

		case PARSING:
			if (c == '<')
				status = ParserStatus.TAG_OPEN;
			else if (!Character.isSpaceChar(c))
				status = ParserStatus.ERROR;
			break;

		case TAG_OPEN:
			switch (c) {
			case '!':
				status = ParserStatus.COMMENT_INTRO_1;
				break;
			case '?':
				status = ParserStatus.INSTRUCTION;
				break;
			case '/':
				status = ParserStatus.ELEMENT_CLOSING;
				break;
			default:
				status = ParserStatus.ELEMENT;
				break;
			}
			break;

		case COMMENT_INTRO_1:
			if (c == '-')
				status = ParserStatus.COMMENT_INTRO_2;
			else
				status = ParserStatus.ERROR;
			break;

		case COMMENT_INTRO_2:
			if (c == '-')
				status = ParserStatus.COMMENT;
			else
				status = ParserStatus.ERROR;
			break;

		case COMMENT:
			if (c == '-')
				status = ParserStatus.COMMENT_OUTRO_1;
			break;

		case COMMENT_OUTRO_1:
			if (c == '-')
				status = ParserStatus.COMMENT_OUTRO_2;
			else
				status = ParserStatus.COMMENT;
			break;

		case COMMENT_OUTRO_2:
			if (c == '>')
				status = ParserStatus.PARSING;
			else
				status = ParserStatus.COMMENT;
			break;

		case INSTRUCTION:
			if (c == '?')
				status = ParserStatus.INSTRUCTION_OUTRO;
			break;

		case INSTRUCTION_OUTRO:
			if (c == '>')
				status = ParserStatus.PARSING;
			else
				status = ParserStatus.INSTRUCTION;
			break;

		case ELEMENT:
			switch (c) {
			case '>':
				level++;
				status = ParserStatus.PARSING;
				break;
			case '/':
				status = ParserStatus.ELEMENT_SINGLE_TAG;
				break;
			case '\'':
			case '\"':
				status = ParserStatus.ATTRIB_VALUE;
				break;
			}
			break;

		case ATTRIB_VALUE:
			if (c == '\'' | c == '\"')
				status = ParserStatus.ELEMENT;

		case ELEMENT_CLOSING:
			if (c == '>') {
				level--;
				status = level == 0 ? ParserStatus.CONSUME_ROOT_ELEMENT
						: ParserStatus.PARSING;
			}

		case ELEMENT_SINGLE_TAG:
			if (c == '>') {
				status = level == 0 ? ParserStatus.CONSUME_ROOT_ELEMENT
						: ParserStatus.PARSING;
			}
		}

		if (DEBUG)
			if (!old.equals(status))
				System.out.println(old + " -- " + c + " --> " + status);
	}

	public static void main(String[] args) {

		MultiXMLRootFilter in = new MultiXMLRootFilter(
				new StringReader(
						"<!-- this is just a comment --> <a> </a> <log attr=\"This should be logged\"><something /></log> <bla/>  "),
				0x10);

		try {
			while (!in.hasFinished()) {
				int r;
				StringBuilder b = new StringBuilder(0x100);
				char[] buf = new char[0x10];

				while ((r = in.read(buf)) >= 0) {
					b.append(buf, 0, r);
				}
				System.out.println("--> " + b.toString());
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
