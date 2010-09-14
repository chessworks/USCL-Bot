/**
 *
 */
package org.chessworks.uscl.services;

import org.chessworks.uscl.util.BaseException;

public class InvalidPlayerException extends BaseException {

	private static final long serialVersionUID = -7448831074478034561L;

	public InvalidPlayerException(String msg, Object... args) {
		super(msg, args);
	}

	public InvalidPlayerException(Throwable t, String msg, Object... args) {
		super(t, msg, args);
	}

}
