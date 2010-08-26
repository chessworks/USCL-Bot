/**
 *
 */
package org.chessworks.uscl.converters;

public class StringConverter extends AbstractConverter<String> {

	public StringConverter() {
		super();
	}

	public StringConverter(String nullValue) {
		super(nullValue);
	}

	@Override
	public String convert(String s) throws ConversionException {
		if (checkNull(s))
			return nullValue;
		return s;
	}

}
