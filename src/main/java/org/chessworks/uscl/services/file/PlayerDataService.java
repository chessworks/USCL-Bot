package org.chessworks.uscl.services.file;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.net.URL;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.Map.Entry;

import org.chessworks.uscl.USCLBot;
import org.chessworks.uscl.model.Player;
import org.chessworks.uscl.model.Team;
import org.chessworks.uscl.model.Title;
import org.chessworks.uscl.services.InvalidPlayerException;
import org.chessworks.uscl.services.InvalidTeamException;
import org.chessworks.uscl.services.simple.TitleService;

public class PlayerDataService {

	/** Map codes to teams */
	private Map<String, Team> teams = new TreeMap<String, Team>();

	/** Map handles to players */
	private Map<String, Player> players = new TreeMap<String, Player>();

	public static final File TEAMS_FILE = new File("Teams.txt");
	public static final File PLAYERS_FILE = new File("Players.txt");
	public static final String ENCODING = "UTF-8";

	public static final File TEAMS_FILE_TEMP = new File("Teams.tmp");
	public static final File PLAYERS_FILE_TEMP = new File("Players.tmp");

	private TitleService titleService = new TitleService();

	public Player getPlayer(String handle) {
		Player p = players.get(handle);
		return p;
	}

	public Team getTeam(String teamCode) {
		Team t = teams.get(teamCode);
		return t;
	}

	public Player createPlayer(String handle) throws InvalidPlayerException {
		Player p = players.get(handle);
		if (p != null) {
			throw new InvalidPlayerException("Player with the handle \"{0}\" is already in the tournament.", handle);
		}
		int i = handle.lastIndexOf('-');
		if (i < 0) {
			throw new InvalidPlayerException("Player handle \"{0}\" must end with a valid team code.", handle);
		}
		String teamCode = handle.substring(i + 1);
		Team team = teams.get(teamCode);
		if (team == null) {
			throw new InvalidPlayerException("Unknown team \"{0}\" for player handle \"{1}\".  Either correct the name or first add the team.", team,
					handle);
		}
		p = new Player(handle, team);
		team.getPlayers().add(p);
		players.put(handle, p);
		return p;
	}

	public Team createTeam(String teamCode) throws InvalidTeamException {
		Team t = teams.get(teamCode);
		if (t != null) {
			throw new InvalidTeamException("Team with the handle \"{0}\" is already in the tournament.", teamCode);
		}
		if (teamCode == null || teamCode.length() < 2 || teamCode.length() > 3) {
			throw new InvalidTeamException("Teams must have a 2 or 3-letter team code.");
		}
		t = new Team(teamCode);
		teams.put(teamCode, t);
		return t;
	}

	public void load() throws IOException {
		loadTeams();
		loadPlayers();
	}

	private void loadTeams() throws IOException {
		FileInputStream stream = new FileInputStream(TEAMS_FILE);
		Reader in = new InputStreamReader(stream, ENCODING);
		Properties data = new Properties();
		data.load(in);
		in.close();
		for (Entry<Object, Object> entry : data.entrySet()) {
			String propName = (String) entry.getKey();
			String propValue = (String) entry.getValue();
			if (!propName.endsWith(".code"))
				continue;
			String prefix = propName.substring(propName.length() - ".code".length());
			String teamCode = propValue;
			String location = data.getProperty(prefix + ".location");
			String name = data.getProperty(prefix + ".name");
			String url = data.getProperty(prefix + ".website");
			Team t = new Team(teamCode);
			t.setName(name);
			t.setLocation(location);
			t.setWebsite(url);
			teams.put(teamCode, t);
		}
	}

	private void loadPlayers() throws IOException {
		FileInputStream stream = new FileInputStream(PLAYERS_FILE);
		Reader in = new InputStreamReader(stream, ENCODING);
		Properties data = new Properties();
		data.load(in);
		in.close();
		for (Entry<Object, Object> entry : data.entrySet()) {
			String propName = (String) entry.getKey();
			String propValue = (String) entry.getValue();
			if (!propName.endsWith(".handle"))
				continue;
			String prefix = propName.substring(propName.length() - ".handle".length());
			String handle = propValue;
			String realName = data.getProperty(prefix + ".name");
			String ratingStr = data.getProperty(prefix + ".rating");
			String teamCode = data.getProperty(prefix + ".team");
			String title = data.getProperty(prefix + ".title");
			String website = data.getProperty(prefix + ".website");
			int rating = Integer.parseInt(ratingStr);
			Team team = teams.get(teamCode);
			assert team != null;
			Player p = new Player(handle, team);
			p.setRealName(realName);
			p.ratings().put(USCLBot.USCL_RATING, rating);
			if (title != null) {
				Set<Title> titles = titleService.lookupAll(title);
				p.getTitles().addAll(titles);
			}
			p.setWebsite(website);
			players.put(handle, p);
		}
	}

	public void save() throws IOException {
		saveTeams();
		savePlayers();
	}

	private void saveTeams() throws IOException {
		PrintWriter out = new PrintWriter(TEAMS_FILE_TEMP, ENCODING);
		out.println("#USCL Teams");
		out.println();
		for (Team team : teams.values()) {
			String code = team.getTeamCode();
			String location = team.getLocation();
			String name = team.getName();
			URL website = team.getWebsite();
			out.format("team.%s.code=%s%n", code, code);
			out.format("team.%s.location=%s%n", code, location);
			out.format("team.%s.name=%s%n", code, name);
			if (website != null) {
				out.format("team.%s.website=%s%n", code, website);
			}
			out.println();
		}
		boolean success = TEAMS_FILE_TEMP.renameTo(TEAMS_FILE);
		assert success;
	}

	private void savePlayers() throws IOException {
		PrintWriter out = new PrintWriter(PLAYERS_FILE_TEMP, ENCODING);
		out.println("#USCL Players");
		out.println();
		for (Player player : players.values()) {
			String handle = player.getHandle();
			String name = player.getRealName();
			int rating = player.ratings().get(USCLBot.USCL_RATING);
			String teamCode = player.getTeam().getTeamCode();
			String title = player.getTitles().toString();
			String id = handle.substring(handle.length() - teamCode.length() - 1);
			URL website = player.getWebsite();
			out.format("player.%s.handle=%s%n", id, handle);
			out.format("player.%s.name=%s%n", id, name);
			out.format("player.%s.rating=%d%n", id, rating);
			out.format("player.%s.team=%d%n", id, teamCode);
			out.format("player.%s.title=%d%n", id, title);
			if (website != null) {
				out.format("team.%s.website=%s%n", id, website);
			}
			out.println();
		}
		boolean success = PLAYERS_FILE_TEMP.renameTo(PLAYERS_FILE);
		assert success;
	}

}
