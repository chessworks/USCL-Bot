package org.chessworks.uscl.model;

import org.chessworks.chess.model.PlayerState;
import org.chessworks.chess.model.User;

public class Player extends User {

	public static final String WEBSITE_UNAVAILABLE = "Unavailable";

	private PlayerState status = PlayerState.OFFLINE;
	private final Team team;
	private String website = WEBSITE_UNAVAILABLE;

	public Player(String handle, Team team) {
		super(handle);
		if (team == null) {
			throw new NullPointerException("Player.team");
		}
		this.team = team;
	}

	public PlayerState getStatus() {
		return status;
	}

	public void setState(PlayerState status) {
		this.status = status;
	}
	
	public Team getTeam() {
		return team;
	}

	public String getWebsite() {
		return website;
	}

	public void setWebsite(String website) {
		if (website == null) {
			this.website = WEBSITE_UNAVAILABLE;
		} else if (website.isEmpty()) {
			this.website = WEBSITE_UNAVAILABLE;
		} else {
			this.website = website;
		}
	}

	public boolean isOnline() {
		return status.isOnline();
	}

}
