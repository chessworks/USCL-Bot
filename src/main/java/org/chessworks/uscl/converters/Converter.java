/**
 *
 */
package org.chessworks.uscl.converters;

public interface Converter<T> {

	public Class<T> getTargetType();

	public T convert(String s) throws ConversionException;

}
