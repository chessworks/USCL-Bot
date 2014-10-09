package org.chessworks.uscl.model;

public enum GameState {
	NOT_STARTED("NOT_STARTED"), PLAYING("*", "LIVE"), ADJOURNED("adj"), WHITE_WINS("1-0"), BLACK_WINS("0-1"), DRAW("1/2-1/2","1/2"), UNKNOWN("?");
	
	private final String code;
    private final String displayCode;
	
    private GameState(String code) {
        this(code, code);
    }
    
    private GameState(String code, String displayCode) {
        this.code = code;
        this.displayCode = displayCode;
    }
    
    public String getProtocolCode() {
        return code;
    }
    
    public String getDisplayCode() {
        return displayCode;
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
	
	public boolean isAdjourned() {
		return (this == ADJOURNED);
	}
	
	@Override
	public String toString() {
	    return displayCode;
	}
	
}
