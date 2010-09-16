/**
 *
 */
package org.chessworks.uscl.converters;

public abstract class AbstractConverter<T> implements Converter<T> {
	public final Class<T> type;
	public final boolean nullForbidden;
	public final T nullValue;

	public AbstractConverter(Class<T> type) {
		this.type = type;
		this.nullForbidden = true;
		this.nullValue = null;
	}

	public AbstractConverter(Class<T> type, T nullValue) {
		this.type = type;
		this.nullForbidden = false;
		this.nullValue = nullValue;
	}

	@Override
	public Class<T> getTargetType() {
		return type;
	}

	protected boolean checkNull(String s) throws ConversionException {
		if (s != null)
			return false;
		if (nullForbidden) {
			throw new ConversionException("Missing required input.");
		}
		return true;
	}

}
