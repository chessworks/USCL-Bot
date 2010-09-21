/**
 *
 */
package org.chessworks.uscl.services;


public class InvalidPlayerException extends InvalidNameException {

	private static final long serialVersionUID = -7401142037799328379L;

	public InvalidPlayerException(String msg, Object... args) {
		super(msg, args);
	}

	public InvalidPlayerException(Throwable t, String msg, Object... args) {
		super(t, msg, args);
	}

}
