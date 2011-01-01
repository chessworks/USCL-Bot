package org.chessworks.chess.services.simple;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.chessworks.chess.model.SimpleName;
import org.chessworks.chess.services.NamingService;
import org.chessworks.common.service.BasicLifecycle;

public class SimpleNameLookupService<T extends SimpleName> extends BasicLifecycle implements NamingService<T> {

	private final Map<String, T> map;
	private final Map<String, T> readOnly;
	private final Collection<T> values;

	public SimpleNameLookupService() {
		this(true);
	}

	public SimpleNameLookupService(boolean ignoreCase) {
		if (ignoreCase) {
			this.map = new TreeMap<String, T>(String.CASE_INSENSITIVE_ORDER);
		} else {
			this.map = new TreeMap<String, T>();
		}
		this.readOnly = Collections.unmodifiableMap(map);
		this.values = readOnly.values();
	}

	public SimpleNameLookupService(Map<String, T> storageMap) {
		this.map = storageMap;
		this.readOnly = Collections.unmodifiableMap(map);
		this.values = (Set<T>) readOnly.values();
	}

	public T lookup(String name) {
		T result = map.get(name);
		return result;
	}

	public List<T> lookupAll(String... names) {
		ArrayList<T> result = new ArrayList<T>(names.length);
		lookupAll(result, names);
		return result;
	}

	public void lookupAll(Collection<? super T> dest, String... names) {
		for (String s : names) {
			if (s == null) {
				dest.add(null);
			} else {
				T value = lookup(s);
				dest.add(value);
			}
		}
	}

	public void register(T value) {
		String key = value.toString();
		this.map.put(key, value);
	}

	public boolean isRegistered(String name) {
		boolean result = map.containsKey(name);
		return result;
	}

	public boolean isRegistered(T value) {
		boolean result = map.containsValue(value);
		return result;
	}

	public Collection<T> all() {
		return values;
	}

	public void clear() {
		map.clear();
	}

}
