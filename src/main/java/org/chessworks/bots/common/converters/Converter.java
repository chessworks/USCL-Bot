/**
 *
 */
package org.chessworks.bots.common.converters;

public interface Converter<T> {

	public Class<T> getTargetType();

	public T convert(String s) throws ConversionException;

}
