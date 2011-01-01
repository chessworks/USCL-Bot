package org.chessworks.bots.common.converters;

import java.util.HashMap;
import java.util.Map;


public class ConverterFactory {

	private final Map<Class<?>, Converter<?>> converters = new HashMap<Class<?>, Converter<?>>();

	public <T> void register(Converter<T> converter) {
		Class<T> type = converter.getTargetType();
		converters.put(type, converter);
	}

	public <T> void register(Converter<T> converter, Class<T> type) {
		converters.put(type, converter);
	}

	public void unregister(Class<?> type) {
		converters.remove(type);
	}

	public <T> Converter<T> forType(Class<? extends T> type) {
		@SuppressWarnings("unchecked")
		Converter<T> converter = (Converter<T>) converters.get(type);
		return converter;
	}

}