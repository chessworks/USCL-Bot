package org.chessworks.uscl.services.file;

import java.io.BufferedReader;
import java.io.File;
import java.io.Flushable;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.chessworks.bots.common.converters.ConversionException;
import org.chessworks.bots.common.converters.Converter;
import org.chessworks.chess.services.file.IO;
import org.chessworks.uscl.services.NoSuchSettingException;

public class FileSettingsService implements Flushable {

	private Properties storage = new Properties();
	private Map<String, Object> values = new HashMap<String, Object>();
	private Map<String, Converter<?>> converters = new HashMap<String, Converter<?>>();
	private final SettingsIO settingsIO = new SettingsIO();

	public Properties listSettings() {
		return storage;
	}

	public String getSettingAsString(String settingName)
			throws NoSuchSettingException {
		verifySettingIsDefined(settingName);
		String setting = storage.getProperty(settingName);
		return setting;
	}

	public void setSettingAsString(String settingName, String value)
			throws NoSuchSettingException, ConversionException {
		verifySettingIsDefined(settingName);
		if (value == null) {
			throw new NullPointerException("Setting value may not be null.");
		}
		Converter<?> converter = converters.get(settingName);
		if (converter == null) {
			throw new NoSuchSettingException(settingName);
		}
		Object objectValue = converter.convert(value);
		storage.setProperty(settingName, value);
		values.put(settingName, objectValue);
		settingsIO.setDirty();
		flush();
	}

	public <T> T getSetting(String settingName, Class<T> settingType)
			throws NoSuchSettingException {
		verifySettingIsDefined(settingName);
		Object o = values.get(settingName);
		T settingValue = settingType.cast(o);
		return settingValue;
	}

	public <T> void setSetting(String settingName, T value)
			throws NoSuchSettingException {
		if (value == null) {
			throw new NullPointerException("Setting value may not be null.");
		}
		@SuppressWarnings("unchecked")
		Converter<T> converter = (Converter<T>) converters.get(settingName);
		if (converter == null) {
			throw new NoSuchSettingException(settingName);
		}
		Class<T> settingType = converter.getTargetType();
		settingType.cast(value); //verify type.
		String stringValue = converter.stringValue(value);
		storage.setProperty(settingName, stringValue);
		values.put(settingName, value);
		settingsIO.setDirty();
		flush();
	}

	public void verifySettingIsDefined(String settingName) {
		Converter<?> converter = converters.get(settingName);
		if (converter == null) {
			throw new NoSuchSettingException(settingName);
		}
	}

	public <T> void defineSetting(String settingName, T defaultValue,
			Converter<T> converter) {
		values.put(settingName, defaultValue);
		converters.put(settingName, converter);
	}

	public void load() {
		settingsIO.load();
	}

	public void save() {
		settingsIO.save();
	}

	@Override
	public void flush() {
		settingsIO.save();
	}

	public void setSettingsFile(String fileName) {
		settingsIO.setFile(fileName);
	}

	public void setSettingsFile(File file) {
		settingsIO.setFile(file);
	}

	private final class SettingsIO extends IO {

		public SettingsIO() {
			super(null, UTF8);
		}

		@Override
		public void doRead(BufferedReader in) throws IOException, ConversionException {
			storage.clear();
			storage.load(in);
			for (Map.Entry<Object, Object> entry : storage.entrySet()) {
				String settingName = (String) entry.getKey();
				String settingValue = (String) entry.getValue();
				Converter<?> converter = converters.get(settingName);
				if (converter != null) {
					Object value = converter.convert(settingValue);
					values.put(settingName,  value);
				} else {
					values.put(settingName, settingValue);
				}
			}
		}

		@Override
		public void doWrite(PrintWriter out) throws Exception {
			storage.store(out, "USCL Bot Settings");
		}
	}
}
