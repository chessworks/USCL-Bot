/**
 *
 */
package org.chessworks.uscl.util;

import java.text.MessageFormat;

public class BaseRuntimeException extends RuntimeException {

	private static final long serialVersionUID = -6407268394894783630L;

	public BaseRuntimeException(String msg, Object... args) {
		super(MessageFormat.format(msg, args));
	}

	public BaseRuntimeException(Throwable t, String msg, Object... args) {
		super(MessageFormat.format(msg, args), t);
	}

}
