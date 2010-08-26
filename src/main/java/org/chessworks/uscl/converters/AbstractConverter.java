/**
 *
 */
package org.chessworks.uscl.converters;

public abstract class AbstractConverter<T> implements Converter<T> {
	public final boolean nullForbidden;
	public final T nullValue;

	public AbstractConverter() {
		this.nullForbidden = true;
		this.nullValue = null;
	}

	public AbstractConverter(T nullValue) {
		this.nullForbidden = false;
		this.nullValue = nullValue;
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
