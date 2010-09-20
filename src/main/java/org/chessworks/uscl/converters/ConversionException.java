/**
 *
 */
package org.chessworks.uscl.converters;

import org.chessworks.uscl.BaseException;

public class ConversionException extends BaseException {

	private static final long serialVersionUID = -7303947618110414528L;

	public ConversionException(String msg, Object... args) {
		super(msg, args);
	}

	public ConversionException(Throwable t, String msg, Object... args) {
		super(t, msg, args);
	}

}
