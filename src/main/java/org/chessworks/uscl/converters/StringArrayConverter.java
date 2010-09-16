/**
 *
 */
package org.chessworks.uscl.converters;

public class StringArrayConverter extends AbstractConverter<String[]> {

	public StringArrayConverter() {
		super(String[].class);
	}

	public StringArrayConverter(String[] nullValue) {
		super(String[].class, nullValue);
	}

	@Override
	public String[] convert(String s) throws ConversionException {
		if (checkNull(s))
			return nullValue;
		String[] args = s.split("[\t ]+)");
		return args;
	}

}
