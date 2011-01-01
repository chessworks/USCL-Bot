package org.chessworks.chess.model;

import java.util.HashMap;

public enum PlayerState {
	OBSERVING,
	PLAYING,
	SIMUL,
	EXAMINING,
	NONE;

	private static final HashMap<String, PlayerState> symbolMap;

	static {
		int size = values().length;
		symbolMap = new HashMap<String, PlayerState>(size);
		symbolMap.put("O", OBSERVING);
		symbolMap.put("P", PLAYING);
		symbolMap.put("PW", PLAYING);
		symbolMap.put("PB", PLAYING);
		symbolMap.put("S", SIMUL);
		symbolMap.put("SW", SIMUL);
		symbolMap.put("SB", SIMUL);
		symbolMap.put("E", EXAMINING);
		symbolMap.put("X", NONE);
	}

	public boolean isPlaying() {
		return this == PLAYING | this==SIMUL;
	}

	public boolean isObserving() {
		return this == OBSERVING;
	}

	public boolean isExamining() {
		return this == EXAMINING;
	}

	public boolean isSimul() {
		return this == SIMUL;
	}

	public static PlayerState forCode(String symbol) {
		PlayerState status = symbolMap.get(symbol);
		return status;
	}
}
