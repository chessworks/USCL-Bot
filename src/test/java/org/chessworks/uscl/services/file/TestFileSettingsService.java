package org.chessworks.uscl.services.file;

import java.io.File;

import junit.framework.TestCase;

import org.apache.commons.io.FileUtils;
import org.chessworks.bots.common.converters.BooleanConverter;
import org.chessworks.bots.common.converters.IntegerConverter;
import org.chessworks.bots.common.converters.StringConverter;
import org.chessworks.uscl.services.NoSuchSettingException;

public class TestFileSettingsService extends TestCase {

	private final File SETTINGS_FILE = new File(
			"src/test/resources/org/chessworks/uscl/services/file/Settings.txt");

	private FileSettingsService service;
	private FileSettingsService service2;
	private File settingsFile;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		// Copy the data files so we can do practice saves.
		settingsFile = File.createTempFile("Players.", ".txt");
		FileUtils.copyFile(SETTINGS_FILE, settingsFile);
		settingsFile.deleteOnExit();
		service = createFileSettingsService();
		service2 = createFileSettingsService();
	}

	public FileSettingsService createFileSettingsService() {
		FileSettingsService service = new FileSettingsService();
		service.setSettingsFile(settingsFile);
		service.defineSetting("testTrue", false, new BooleanConverter());
		service.defineSetting("testFalse", true, new BooleanConverter());
		service.defineSetting("test0", -1, new IntegerConverter());
		service.defineSetting("test1", -1, new IntegerConverter());
		service.defineSetting("testHello", "goodbye", new StringConverter());
		service.defineSetting("testDefaultTrue", true, new BooleanConverter());
		service.defineSetting("testDefaultFalse", false, new BooleanConverter());
		service.defineSetting("testDefault5", 5, new IntegerConverter());
		service.defineSetting("testDefaultHello", "hello", new StringConverter());
		return service;
	}

	@Override
	protected void tearDown() throws Exception {
		settingsFile.delete();
	}

	public void testLoadSettings() throws Exception {
		service.load();
		boolean testTrue = service.getSetting("testTrue", Boolean.class);
		boolean testFalse = service.getSetting("testFalse", Boolean.class);
		int test0 = service.getSetting("test0", Integer.class);
		int test1 = service.getSetting("test1", Integer.class);
		String testHello = service.getSetting("testHello", String.class);
		boolean testDefaultTrue = service.getSetting("testDefaultTrue",
				Boolean.class);
		boolean testDefaultFalse = service.getSetting("testDefaultFalse",
				Boolean.class);
		int testDefault5 = service.getSetting("testDefault5", Integer.class);
		String testDefaultHello = service.getSetting("testDefaultHello",
				String.class);
		assertTrue(testTrue);
		assertFalse(testFalse);
		assertEquals(0, test0);
		assertEquals(1, test1);
		assertEquals("hello", testHello);
		assertTrue(testDefaultTrue);
		assertFalse(testDefaultFalse);
		assertEquals(5, testDefault5);
		assertEquals("hello", testDefaultHello);

	}

	public void testSaveSettings() throws Exception {
		service.load();
		int testChangeMe1 = service.getSetting("testDefault5", Integer.class);
		assertEquals(5, testChangeMe1);
		service.setSetting("testDefault5", 6);
		service2.load();
		int testChangeMe2 = service2.getSetting("testDefault5", Integer.class);
		assertEquals(6, testChangeMe2);
	}

	public void testGetInvalidSetting() throws Exception {
		service.load();
		try {
			service.getSettingAsString("testDoesNotExist");
			fail();
		} catch (NoSuchSettingException e) {
			// pass
		}
	}

	public void testSetInvalidSetting() throws Exception {
		service.load();
		try {
			service.setSetting("testDoesNotExist", "testValue");
			fail();
		} catch (NoSuchSettingException e) {
			// pass
		}
	}

}
