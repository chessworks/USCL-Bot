/**
 *
 */
package org.chessworks.uscl;

import java.text.MessageFormat;

public class BaseException extends Exception {
	private static final long serialVersionUID = -5595747244640026192L;

	public BaseException(String msg, Object... args) {
		super(MessageFormat.format(msg, args));
	}

	public BaseException(Throwable t, String msg, Object... args) {
		super(MessageFormat.format(msg, args), t);
	}
}
