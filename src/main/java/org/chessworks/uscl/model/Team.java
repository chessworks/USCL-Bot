package org.chessworks.uscl.model;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;

public class Team implements Comparable<Team> {

	public Team(String teamCode) {
		if (teamCode == null) {
			throw new NullPointerException("Team.code");
		}
		this.teamCode = teamCode;
	}

	@Override
	public int compareTo(Team o) {
		return teamCode.compareTo(o.teamCode);
	}

	public List<Player> getPlayers() {
		return players;
	}

	public String getTeamCode() {
		return teamCode;
	}

	public String getLocation() {
		return location;
	}

	public void setLocation(String location) {
		this.location = location;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public URL getWebsite() {
		return website;
	}

	public void setWebsite(URL url) {
		this.website = url;
	}

	public void setWebsite(String url) throws MalformedURLException {
		if (url == null)
			this.website = null;
		else
			this.website = new URL(url);
	}

	public String toString() {
		return name;
	}

	private List<Player> players = new LinkedList<Player>();
	private String teamCode;
	private String name;
	private String location;
	private URL website;

}
