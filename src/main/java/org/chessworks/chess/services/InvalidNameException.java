/**
 *
 */
package org.chessworks.chess.services;

import org.chessworks.common.javatools.BaseException;

public class InvalidNameException extends BaseException {

	private static final long serialVersionUID = 7948897917575723116L;

	public InvalidNameException(String msg, Object... args) {
		super(msg, args);
	}

	public InvalidNameException(Throwable t, String msg, Object... args) {
		super(t, msg, args);
	}

}
