/**
 *
 */
package org.chessworks.uscl.converters;

public class StringConverter extends AbstractConverter<String> {

	public StringConverter() {
		super(String.class);
	}

	public StringConverter(String nullValue) {
		super(String.class, nullValue);
	}

	@Override
	public String convert(String s) throws ConversionException {
		if (checkNull(s))
			return nullValue;
		return s;
	}

}
