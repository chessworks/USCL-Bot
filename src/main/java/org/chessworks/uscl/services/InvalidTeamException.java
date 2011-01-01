/**
 *
 */
package org.chessworks.uscl.services;

import org.chessworks.chess.services.InvalidNameException;


public class InvalidTeamException extends InvalidNameException {

	private static final long serialVersionUID = 824391146044077492L;

	public InvalidTeamException(String msg, Object... args) {
		super(msg, args);
	}

	public InvalidTeamException(Throwable t, String msg, Object... args) {
		super(t, msg, args);
	}

}
