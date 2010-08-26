/**
 *
 */
package org.chessworks.uscl.converters;

public class IntegerConverter extends AbstractConverter<Integer> {

	public IntegerConverter() {
		super();
	}

	public IntegerConverter(Integer nullValue) {
		super(nullValue);
	}

	@Override
	public Integer convert(String s) throws ConversionException {
		if (this.checkNull(s))
			return nullValue;
		try {
			int i = Integer.parseInt(s);
			return i;
		} catch (NumberFormatException e) {
			throw new ConversionException(e, "Invalid number: {0}", s);
		}
	}

}
