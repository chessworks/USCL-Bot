package org.chessworks.bots.common.converters;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.chessworks.chess.model.SimpleName;
import org.chessworks.chess.services.NamingService;
import org.chessworks.common.javatools.collections.CollectionHelper;

public class NameListConverter<T extends SimpleName> extends AbstractConverter<List<T>> {

	private NamingService<T> service;

	@SuppressWarnings({ "unchecked", "rawtypes" })
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
