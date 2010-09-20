package org.chessworks.uscl;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.Closeable;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Properties;

public class FileHelper {

	/**
	 * Opens a text file located somewhere in the classpath (ie inside the JAR file).
	 *
	 * @param path
	 *            The path to the file, relative to the classpath or JAR.
	 * @return A BufferedReader for reading the text file.
	 */
	public static BufferedReader openInternalTextFile(String path) {
		ClassLoader cl = Thread.currentThread().getContextClassLoader();
		InputStream is = cl.getResourceAsStream(path);
		InputStreamReader r = new InputStreamReader(is);
		BufferedReader br = new BufferedReader(r);
		return br;
	}

	/**
	 * Opens a binary file located somewhere in the classpath (ie inside the JAR file).
	 *
	 * @param path
	 *            The path to the file, relative to the classpath or JAR.
	 * @return A BufferedInputStream for reading the binary file.
	 */
	public static BufferedInputStream openInternalBinaryFile(String path) {
		ClassLoader cl = Thread.currentThread().getContextClassLoader();
		InputStream is = cl.getResourceAsStream(path);
		BufferedInputStream in = new BufferedInputStream(is);
		return in;
	}

	/**
	 * Loads a properties file located somewhere in the classpath (ie inside the JAR file).
	 *
	 * @param path
	 *            The path to the file, relative to the classpath or JAR.
	 * @param defaults
	 *            A properties file containing default settings to be used in the event the provided file does not deliver.
	 * @return The properties file. If the file is not found, a new properties object initialized with the defaults is returned. If no defaults are
	 *         provided, null is returned.
	 */
	public static Properties loadInternalPropertiesFile(String path, Properties defaults) {
		Properties props = new Properties(defaults);
		InputStream in = null;
		try {
			ClassLoader cl = Thread.currentThread().getContextClassLoader();
			in = cl.getResourceAsStream(path);
			props.load(in);
		} catch (IOException e) {
			e.printStackTrace();
			if (defaults == null)
				return null;
		} finally {
			close(in);
		}
		return props;
	}

	/**
	 * Opens a text file located on the filesystem relative to the current directory. This does NOT search inside the JAR file.
	 *
	 * @param path
	 *            The path to the file, relative to the current directory when the program is run. The path may be absolute or relative.
	 * @return A BufferedReader for reading the text file.
	 */
	public static BufferedReader openExternalTextFile(String path) throws java.io.IOException {
		InputStreamReader r = new FileReader(path);
		BufferedReader br = new BufferedReader(r);
		return br;
	}

	/**
	 * Opens a binary file located on the filesystem relative to the current directory. This does NOT search inside the JAR file.
	 *
	 * @param path
	 *            The path to the file, relative to the current directory when the program is run. The path may be absolute or relative.
	 * @return A BufferedInputStream for reading the binary file.
	 */
	public static BufferedInputStream openExternalBinaryFile(String path) throws java.io.IOException {
		InputStream is = new FileInputStream(path);
		BufferedInputStream in = new BufferedInputStream(is);
		return in;
	}

	/**
	 * Loads a properties file located on the filesystem. This does NOT search inside the JAR file.
	 *
	 * @param path
	 *            The path to the file, relative to the current directory when the program is run. The path may be absolute or relative.
	 * @param defaults
	 *            A properties file containing default settings to be used in the event the provided file does not deliver.
	 * @return The properties file. If the file is not found, a new properties object initialized with the defaults is returned. If no defaults are
	 *         provided, null is returned.
	 */
	public static Properties loadExternalPropertiesFile(String path, Properties defaults) {
		Properties props = new Properties(defaults);
		InputStream in = null;
		try {
			in = new FileInputStream(path);
			props.load(in);
		} catch (IOException e) {
			e.printStackTrace();
			if (defaults == null)
				return null;
		} finally {
			close(in);
		}
		return props;
	}

	/**
	 * Safely closes the IO Stream, logging any exceptions.
	 *
	 * @param stream
	 *            The stream to close.
	 */
	public static void close(Closeable stream) {
		try {
			if (stream != null) {
				stream.close();
			}
		} catch (Exception e) {
			System.err.println("Exception trapped while closing file!");
			e.printStackTrace();
		} catch (Error e) {
			System.err.println("Error detected while closing file!");
			System.err.println(errorsMsg);
			e.printStackTrace();
			throw e;
		}
	}

	private static final String errorsMsg = "Errors thrown from a finally block can cause exceptions\n"
			+ "from the try block to be masked and lost.  Errors in a\n" + "finally block can also interfere with the proper release\n"
			+ "of resources.  However, catching and not propogating errors\n" + "can be even worse, causing hung threads.  Therefore logging\n"
			+ "the java.lang.Error here, and rethrowing.";
}
