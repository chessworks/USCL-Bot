package org.chessworks.uscl.services;

import org.chessworks.common.javatools.exceptions.BaseRuntimeException;

public class NoSuchSettingException extends BaseRuntimeException {

	private static final long serialVersionUID = -4770850795897737735L;

	public NoSuchSettingException(String settingName) {
		super("No such setting: %s.", settingName);
	}

}
