package org.chessworks.chess.model;


public class Title extends SimpleName {

	private final String description;

	public Title(String name) {
		super(name);
		this.description = "";
	}

	public Title(String name, String description) {
		super(name);
		this.description = description;
	}

	public String getName() {
		return super.toString();
	}

	public String getDescription() {
		return description;
	}

}
