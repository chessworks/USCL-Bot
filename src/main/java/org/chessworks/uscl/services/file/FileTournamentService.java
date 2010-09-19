package org.chessworks.uscl.services.file;

import static org.chessworks.common.javatools.io.IOHelper.UTF8;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URL;
import java.text.MessageFormat;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.Map.Entry;

import org.chessworks.common.javatools.io.DirtyFileHelper;
import org.chessworks.uscl.USCLBot;
import org.chessworks.uscl.model.Player;
import org.chessworks.uscl.model.Team;
import org.chessworks.uscl.model.Title;
import org.chessworks.uscl.services.DataStoreException;
import org.chessworks.uscl.services.InvalidNameException;
import org.chessworks.uscl.services.simple.SimpleTitleService;
import org.chessworks.uscl.services.simple.SimpleTournamentService;

public class FileTournamentService extends SimpleTournamentService {

	private static final File DEFAULT_PLAYERS_FILE = new File("data/Players.txt");
	private static final File DEFAULT_SCHEDULE_FILE = new File("data/Games.txt");
	private static final File DEFAULT_TEAMS_FILE = new File("data/Teams.txt");
	private SimpleTitleService titleService = new SimpleTitleService();

	public Player createPlayer(String handle) throws InvalidNameException {
		playersIO.setDirty();
		Player p = super.createPlayer(handle);
		save();
		return p;
	}

	public Team createTeam(String teamCode) throws InvalidNameException {
		teamsIO.setDirty();
		Team t = super.createTeam(teamCode);
		save();
		return t;
	}

	public void clearSchedule() {
		scheduleIO.setDirty();
		super.clearSchedule();
		save();
	}

	public void schedule(Player white, Player black, int board) {
		scheduleIO.setDirty();
		super.schedule(white, black, board);
		save();
	}

	public void reserveBoard(Player player, int board) {
		scheduleIO.setDirty();
		super.reserveBoard(player, board);
		save();
	}

	public int unreserveBoard(Player player) {
		scheduleIO.setDirty();
		int board = super.unreserveBoard(player);
		save();
		return board;
	}

	public void setPlayersFile(File file) {
		this.playersIO.setFile(file);
	}

	public void setPlayersFile(String fileName) {
		this.playersIO.setFile(fileName);
	}

	public void setScheduleFile(File file) {
		this.scheduleIO.setFile(file);
	}

	public void setScheduleFile(String fileName) {
		this.scheduleIO.setFile(fileName);
	}

	public void setTeamsFile(File file) {
		this.teamsIO.setFile(file);
	}

	public void setTeamsFile(String fileName) {
		this.teamsIO.setFile(fileName);
	}

	public void load() {
		try {
			teamsIO.readText();
			playersIO.readText();
			scheduleIO.readText();
		} catch (IOException e) {
			throw new DataStoreException("Error saving changes to disk.", e);
		}
	}

	public void save() {
		try {
			teamsIO.writeText();
			playersIO.writeText();
			scheduleIO.writeText();
		} catch (IOException e) {
			throw new DataStoreException("Error saving changes to disk.", e);
		}
	}

	private final DirtyFileHelper playersIO = new DirtyFileHelper(DEFAULT_PLAYERS_FILE, UTF8) {

		@Override
		public void doRead(BufferedReader in) throws IOException, InvalidNameException {
			Properties data = new Properties();
			data.load(in);
			for (Entry<Object, Object> entry : data.entrySet()) {
				String propName = (String) entry.getKey();
				String propValue = (String) entry.getValue();
				if (!propName.endsWith(".handle"))
					continue;
				String prefix = propName.substring(0,propName.length() - ".handle".length());
				String handle = propValue;
				String realName = data.getProperty(prefix + ".name");
				String ratingStr = data.getProperty(prefix + ".rating");
				String teamCode = data.getProperty(prefix + ".team");
				String title = data.getProperty(prefix + ".titles");
				String website = data.getProperty(prefix + ".website");
				int rating = (ratingStr==null) ? -1 : Integer.parseInt(ratingStr);
				Team team = findTeam(teamCode);
				if (team == null)
					throw new InvalidNameException("Team \"{0}\" does not exist.");
				assert team != null;
				Player p = createPlayer(handle, team);
				p.setRealName(realName);
				p.ratings().put(USCLBot.USCL_RATING, rating);
				if (title != null) {
					Set<Title> titles = titleService.lookupAll(title);
					p.getTitles().addAll(titles);
				}
				p.setWebsite(website);
			}
		}

		@Override
		public void doWrite(PrintWriter out) throws IOException {
			out.println("#USCL Players");
			out.println();
			for (Player player : findAllPlayers()) {
				String handle = player.getHandle();
				String name = player.getRealName();
				Integer rating = player.ratings().get(USCLBot.USCL_RATING);
				String teamCode = player.getTeam().getTeamCode();
				String title = player.getTitles().toString();
				String id = handle.substring(handle.length() - teamCode.length() - 1);
				URL website = player.getWebsite();
				out.format("player.%s.handle=%s%n", id, handle);
				out.format("player.%s.name=%s%n", id, name);
				if (rating != null) {
					out.format("player.%s.rating=%d%n", id, rating);
				}
				out.format("player.%s.team=%s%n", id, teamCode);
				out.format("player.%s.title=%s%n", id, title);
				if (website != null) {
					out.format("team.%s.website=%s%n", id, website);
				}
				out.println();
			}
		}

	};

	private final DirtyFileHelper teamsIO = new DirtyFileHelper(DEFAULT_TEAMS_FILE, UTF8) {

		@Override
		public void doRead(BufferedReader in) throws IOException, InvalidNameException {
			Properties data = new Properties();
			data.load(in);
			for (Entry<Object, Object> entry : data.entrySet()) {
				String propName = (String) entry.getKey();
				String propValue = (String) entry.getValue();
				if (!propName.endsWith(".code"))
					continue;
				String prefix = propName.substring(0, propName.length() - ".code".length());
				String teamCode = propValue;
				String location = data.getProperty(prefix + ".location");
				String name = data.getProperty(prefix + ".name");
				String url = data.getProperty(prefix + ".website");
				Team t = createTeam(teamCode);
				t.setName(name);
				t.setLocation(location);
				t.setWebsite(url);
			}
		}

		@Override
		public void doWrite(PrintWriter out) throws IOException {
			out.println("#USCL Teams");
			out.println();
			for (Team team : findAllTeams()) {
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
		}

	};

	private final DirtyFileHelper scheduleIO = new DirtyFileHelper(DEFAULT_SCHEDULE_FILE, UTF8) {

		@Override
		public void doRead(BufferedReader in) throws IOException {
			while (true) {
				String line = in.readLine();
				if (line == null)
					break;
				line = line.trim();
				if (line.length() == 0)
					continue;
				String[] args = line.split("[ \t]+");
				String handle = args[1];
				String game = args[2];
				int gameNum = Integer.parseInt(game);
				Player player = findPlayer(handle);
				FileTournamentService.super.reserveBoard(player, gameNum);
			}
		}

		@Override
		public void doWrite(PrintWriter out) throws IOException {
			Map<Player, Integer> playerBoards = FileTournamentService.super.getPlayerBoardMap();
			for (Map.Entry<Player, Integer> entry : playerBoards.entrySet()) {
				Player player = entry.getKey();
				int board = entry.getValue();
				String line = MessageFormat.format("reserve-game {0} {1}", player, board);
				out.println(line);
			}
		}

	};

}
