package org.chessworks.uscl.model;

import org.chessworks.chess.model.User;

public class Player extends User {

	public static final String UNAVAILABLE = "Unavailable";

	private final Team team;
	private String website = UNAVAILABLE;

	public Player(String handle, Team team) {
		super(handle);
		if (team == null) {
			throw new NullPointerException("Player.team");
		}
		this.team = team;
	}

	public Team getTeam() {
		return team;
	}

	public String getWebsite() {
		return website;
	}

	public void setWebsite(String website) {
		if (website == null) {
			this.website = UNAVAILABLE;
		} else if (website.isEmpty()) {
			this.website = UNAVAILABLE;
		} else {
			this.website = website;
		}
	}

}
