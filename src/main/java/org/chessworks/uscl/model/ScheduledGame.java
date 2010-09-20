/**
 *
 */
package org.chessworks.uscl.model;

import java.util.Date;


public class ScheduledGame {
	public synchronized int getAssignedBoard() {
		return assignedBoard;
	}

	public synchronized void setAssignedBoard(int assignedBoard) {
		this.assignedBoard = assignedBoard;
	}

	public synchronized Player getBlackPlayer() {
		return blackPlayer;
	}

	public synchronized void setBlackPlayer(Player blackPlayer) {
		this.blackPlayer = blackPlayer;
	}

	public synchronized Date getStartTime() {
		return startTime;
	}

	public synchronized void setStartTime(Date startTime) {
		this.startTime = startTime;
	}

	public synchronized Player getWhitePlayer() {
		return whitePlayer;
	}

	public synchronized void setWhitePlayer(Player whitePlayer) {
		this.whitePlayer = whitePlayer;
	}

	private int assignedBoard;
	private Player blackPlayer;
	private Date startTime;
	private Player whitePlayer;
}