/**
 *
 */
package org.chessworks.uscl.converters;

public interface Converter<T> {

	public T convert(String s) throws ConversionException;

}
