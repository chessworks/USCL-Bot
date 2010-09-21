/**
 *
 */
package org.chessworks.uscl.converters;

import org.chessworks.uscl.util.NamingService;
import org.chessworks.uscl.util.SimpleName;

public class NameConverter<T extends SimpleName> extends AbstractConverter<T> {

	private NamingService<T> service;

	public NameConverter(Class<T> type) {
		super(type);
	}

	public NameConverter(Class<T> type, T nullValue) {
		super(type, nullValue);
	}

	@Override
	public T convert(String s) throws ConversionException {
		if (checkNull(s))
			return nullValue;
		T value = service.lookup(s);
		return value;
	}

	public void setLookupService(NamingService<T> service) {
		this.service = service;
	}

}
