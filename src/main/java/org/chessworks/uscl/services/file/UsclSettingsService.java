package org.chessworks.uscl.services.file;

import java.io.File;

import org.chessworks.bots.common.converters.IntegerConverter;
import org.chessworks.bots.common.converters.StringConverter;

public class UsclSettingsService extends FileSettingsService {

	public static final String LIBRARY_HANDLE = "library-handle";
	public static final String LIBRARY_NEXTSLOT = "library-nextslot";
	public static final String EVENTS_LIST_MIN_ID = "events-list-min-id";
	public static final String EVENTS_LIST_MAX_ID = "events-list-max-id";
	
	private static final File DEFAULT_SETTINGS_FILE = new File("data/Settings.txt");

	public UsclSettingsService() {
		this.setSettingsFile(DEFAULT_SETTINGS_FILE);
		this.defineSetting(LIBRARY_HANDLE, "uscl", new StringConverter());
		this.defineSetting(LIBRARY_NEXTSLOT, 0, new IntegerConverter(0, 400));
		this.defineSetting(EVENTS_LIST_MIN_ID, 5, new IntegerConverter(0, 500));
		this.defineSetting(EVENTS_LIST_MAX_ID, 10, new IntegerConverter(0, 500));
	}
	
	public String getLibraryHandle() {
		String handle = getSetting(LIBRARY_HANDLE, String.class);
		return handle;
	}

	public int getAndIncrementNextLibrarySlot() {
		int slot = getSetting(LIBRARY_NEXTSLOT, Integer.class);
		if (slot >= 400)
			throw new IllegalStateException("Exceeded maximum library side of 400.");
		int nextSlot = slot + 1;
		setSetting(LIBRARY_NEXTSLOT, nextSlot);
		return slot;
	}
	
	public int getEventsListMinId() {
		int id = getSetting(EVENTS_LIST_MIN_ID, Integer.class);
		return id;
	}
	
	public int getEventsListMaxId() {
		int id = getSetting(EVENTS_LIST_MAX_ID, Integer.class);
		return id;
	}

}
