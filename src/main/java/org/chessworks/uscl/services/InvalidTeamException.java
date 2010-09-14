/**
 *
 */
package org.chessworks.uscl.services;

import org.chessworks.uscl.util.BaseException;

public class InvalidTeamException extends BaseException {

	private static final long serialVersionUID = 7948897917575723116L;

	public InvalidTeamException(String msg, Object... args) {
		super(msg, args);
	}

	public InvalidTeamException(Throwable t, String msg, Object... args) {
		super(t, msg, args);
	}

}
