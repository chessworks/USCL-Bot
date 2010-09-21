package org.chessworks.uscl.converters;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.chessworks.common.javatools.collections.CollectionHelper;
import org.chessworks.uscl.util.NamingService;
import org.chessworks.uscl.util.SimpleName;

public class NameListConverter<T extends SimpleName> extends AbstractConverter<List<T>> {

	private NamingService<T> service;

	@SuppressWarnings("unchecked")
	public NameListConverter(Class<T> type) {
		super((Class) Collection.class, Collections.<T> emptyList());
	}

	@Override
	public List<T> convert(String s) throws ConversionException {
		if (checkNull(s))
			return nullValue;
		List<T> list = new ArrayList<T>();
		String[] strings = CollectionHelper.split(s);
		for (String v : strings) {
			T value = service.lookup(v);
			list.add(value);
		}
		return list;
	}

	public void setNamingService(NamingService<T> service) {
		this.service = service;
	}

}
