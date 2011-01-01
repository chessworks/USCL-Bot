/**
 *
 */
package org.chessworks.chess.services;

import org.chessworks.common.javatools.BaseRuntimeException;

public class DataStoreException extends BaseRuntimeException {

	private static final long serialVersionUID = 4256091048161681573L;

	public DataStoreException(String msg, Object... args) {
		super(msg, args);
	}

	public DataStoreException(Throwable t, String msg, Object... args) {
		super(t, msg, args);
	}

}
