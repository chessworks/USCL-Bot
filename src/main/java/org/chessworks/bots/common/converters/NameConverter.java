/**
 *
 */
package org.chessworks.bots.common.converters;

import org.chessworks.chess.model.SimpleName;
import org.chessworks.chess.services.NamingService;

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
