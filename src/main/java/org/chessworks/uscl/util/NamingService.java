package org.chessworks.uscl.util;

public interface NamingService<T extends SimpleName> {

	public abstract T lookup(String name);

}