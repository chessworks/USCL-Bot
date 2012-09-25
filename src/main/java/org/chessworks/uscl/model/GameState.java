package org.chessworks.uscl.model;

public enum GameState {
	NOT_STARTED("NOT_STARTED"), PLAYING("*"), ADJOURNED("adj"), WHITE_WINS("1-0"), BLACK_WINS("0-1"), DRAW("1/2-1/2"), UNKNOWN("?");
	
	private String code;
	
	private GameState(String code) {
		this.code = code;
	}
	
	public String getCode() {
		return code;
	}
	
	public boolean isPlaying() {
		return (this == PLAYING);
	}
	
	public boolean isFinished() {
		switch(this) {
		case WHITE_WINS:
		case BLACK_WINS:
		case DRAW:
			return true;
		default:
			return false;
		}
	}
	
}