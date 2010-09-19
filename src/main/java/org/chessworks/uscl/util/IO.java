/**
 *
 */
package org.chessworks.uscl.util;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;

import org.chessworks.common.javatools.io.DirtyFileHelper;
import org.chessworks.uscl.services.DataStoreException;

public class IO extends DirtyFileHelper {

	public IO(File file, Charset encoding) {
		super(file, encoding);
	}

	public void load() {
		try {
			readText();
		} catch (IOException e) {
			throw new DataStoreException("Error reading data from disk.", e);
		}
	}

	public void save() {
		try {
			writeText();
		} catch (IOException e) {
			throw new DataStoreException("Error saving changes to disk.", e);
		}
	}

}