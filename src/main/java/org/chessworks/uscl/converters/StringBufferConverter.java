/**
 *
 */
package org.chessworks.uscl.converters;

public class StringBufferConverter implements Converter<StringBuffer> {

	@Override
	public Class<StringBuffer> getTargetType() {
		return StringBuffer.class;
	}

	@Override
	public StringBuffer convert(String s) throws ConversionException {
		if (s == null)
			return new StringBuffer();
		else
			return new StringBuffer(s);
	}

}
