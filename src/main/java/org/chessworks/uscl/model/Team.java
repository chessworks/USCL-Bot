package org.chessworks.uscl.model;

import java.util.LinkedList;
import java.util.List;

public class Team implements Comparable<Team>{

	public static final String UNAVAILABLE = "Unavailable";

	private String teamCode;
	private String realName;
	private String division = UNAVAILABLE;
	private String location = UNAVAILABLE;
	private String website = UNAVAILABLE;
	private List<Player> players = new LinkedList<Player>();

	public Team(String teamCode) {
		if (teamCode == null) {
			throw new NullPointerException("Team.code");
		}
		this.teamCode = teamCode;
		this.realName = teamCode;
	}

	/**
	 * Teams are naturally sorted by the case-sensitive alphabetical ordering of their teamCode's.
	 */
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

	public String getDivision() {
		return division;
	}

	public void setDivision(String division) {
		if (division == null) {
			this.division = UNAVAILABLE;
		} else if (location.isEmpty()) {
			this.division = UNAVAILABLE;
		} else {
			this.division = division;
		}
	}

	public String getLocation() {
		return location;
	}

	public void setLocation(String location) {
		if (location == null) {
			this.location = UNAVAILABLE;
		} else if (location.isEmpty()) {
			this.location = UNAVAILABLE;
		} else {
			this.location = location;
		}
	}

	public String getRealName() {
		return realName;
	}

	public void setRealName(String realName) {
		if (realName == null) {
			this.realName = getTeamCode();
		} else if (realName.isEmpty()) {
			this.realName = getTeamCode();
		} else {
			this.realName = realName;
		}
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

	public String toString() {
		String teamCode = this.getTeamCode();
		String teamName = this.getRealName();
		if (teamCode.equals(teamName)) {
			return teamCode;
		} else {
			return teamName + " (" + teamCode + ")";
		}
	}

}
