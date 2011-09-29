package org.chessworks.uscl.model;

public class Game {

	/**
     * The name of the white player, indexed by server game id.
     */
	public Player whitePlayer;
    /**
     * The name of the black player, indexed by server game id.
     */
	public Player blackPlayer;
	public int boardNumber;
	public GameState status = GameState.NOT_STARTED;
	
	public Game(int boardNumber, Player whitePlayer, Player blackPlayer) {
		this.boardNumber = boardNumber;
		this.whitePlayer = whitePlayer;
		this.blackPlayer = blackPlayer;
	}
	
    /**
     * True if the game hasn't started or hasn't yet been announced. Indexed by server game id.
     */
	public boolean needsAnnounce = true;
	
    /**
     * The number of people currently observing the game. Indexed by server game id.
     */
	public int observerCountCurrent = 0;
	
    /**
     * The max number of people who have concurrently observed the game. Indexed by server game id.
     */
	public int observerCountMax = 0;
	
	public String getStatusString() {
		if (status.isPlaying()) {
			return String.format("\"observe %d\"", boardNumber);
		}
		else return status.toString();
	}
}
