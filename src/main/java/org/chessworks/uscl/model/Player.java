/**
 * 
 */
package org.chessworks.uscl.model;

import java.util.Set;



public class Player {
	/**
	 * @return the realName
	 */
	public synchronized String getRealName() {
		return realName;
	}

	/**
	 * @param realName
	 *            the realName to set
	 */
	public synchronized void setRealName(String realName) {
		this.realName = realName;
	}

	/**
	 * @return the title
	 */
	public synchronized String getTitle() {
		return title;
	}

	/**
	 * @param title
	 *            the title to set
	 */
	public synchronized void setTitle(String title) {
		this.title = title;
	}

	/**
	 * @return the userName
	 */
	public synchronized String getHandle() {
		return userName;
	}

	/**
	 * @param userName
	 *            the userName to set
	 */
	public synchronized void setUserName(String userName) {
		this.userName = userName;
	}

	/**
	 * @return the games
	 */
	public synchronized Set<ScheduledGame> getGames() {
		return games;
	}

	private Set<ScheduledGame> games;
	private String realName;
	private String title;
	private String userName;
}