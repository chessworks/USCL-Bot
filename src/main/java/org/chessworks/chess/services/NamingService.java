package org.chessworks.chess.services;

import org.chessworks.chess.model.SimpleName;

public interface NamingService<T extends SimpleName> {

	public abstract T lookup(String name);

}