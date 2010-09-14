/**
 *
 */
package org.chessworks.uscl;

import org.chessworks.uscl.util.BaseException;

class NoSuchCommandException extends BaseException {

	private static final long serialVersionUID = 6635766410355769157L;

	public NoSuchCommandException(String msg, Object... args) {
		super(msg, args);
	}

	public NoSuchCommandException(Throwable t, String msg, Object... args) {
		super(t, msg, args);
	}

}
