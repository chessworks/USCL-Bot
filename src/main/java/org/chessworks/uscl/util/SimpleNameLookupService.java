package org.chessworks.uscl.util;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.chessworks.common.service.BasicLifecycle;


public class SimpleNameLookupService<T extends SimpleName> extends BasicLifecycle {

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

	public Collection<T> all() {
		return values;
	}

	public void clear() {
		map.clear();
	}

	public void register(T name) {
		String key = name.toString();
		this.map.put(key, name);
	}

}
