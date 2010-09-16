package org.chessworks.uscl.model;

import java.net.MalformedURLException;
import java.net.URL;

public class Player extends User {

	private final Team team;
	private URL website;

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

	public URL getWebsite() {
		return website;
	}

	public void setWebsite(URL website) {
		this.website = website;
	}

	public void setWebsite(String website) throws MalformedURLException {
		if (website == null)
			this.website = null;
		else
			this.website = new URL(website);
	}

}
