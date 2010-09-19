/**
 *
 */
package org.chessworks.uscl.services;

import org.chessworks.uscl.util.BaseRuntimeException;

public class DataStoreException extends BaseRuntimeException {

	private static final long serialVersionUID = 4256091048161681573L;

	public DataStoreException(String msg, Object... args) {
		super(msg, args);
	}

	public DataStoreException(Throwable t, String msg, Object... args) {
		super(t, msg, args);
	}

}
