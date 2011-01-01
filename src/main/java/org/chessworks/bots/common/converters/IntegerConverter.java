/**
 *
 */
package org.chessworks.bots.common.converters;

public class IntegerConverter extends AbstractConverter<Integer> {

	public IntegerConverter() {
		super(Integer.class);
	}

	public IntegerConverter(Integer nullValue) {
		super(Integer.class, nullValue);
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
