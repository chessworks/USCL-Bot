/**
 *
 */
package org.chessworks.uscl.converters;

public class StringArrayConverter extends AbstractConverter<String[]> {

	public StringArrayConverter() {
		super();
	}

	public StringArrayConverter(String[] nullValue) {
		super(nullValue);
	}

	@Override
	public String[] convert(String s) throws ConversionException {
		if (checkNull(s))
			return nullValue;
		String[] args = s.split("[\t ]+)");
		return args;
	}

}
