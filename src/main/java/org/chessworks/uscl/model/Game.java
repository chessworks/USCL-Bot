package org.chessworks.uscl.model;

public class Game {
	public static enum Status {
		NOT_STARTED, PLAYING, ADJOURNED, WHITE_WINS, BLACK_WINS, DRAW
	};
	
    /**
     * The name of the white player, indexed by server game id.
     */
	public Player whitePlayer;
    /**
     * The name of the black player, indexed by server game id.
     */
	public Player blackPlayer;
	public int boardNumber;
	public int status;
    /**
     * True if the game hasn't started or hasn't yet been announced. Indexed by server game id.
     */
	public boolean needsAnnounce;
	
    /**
     * The number of people currently observing the game. Indexed by server game id.
     */
	public int observerCountCurrent;
	
    /**
     * The max number of people who have concurrently observed the game. Indexed by server game id.
     */
	public int observerCountMax;
}
