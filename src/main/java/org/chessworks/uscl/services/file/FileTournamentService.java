package org.chessworks.uscl.services.file;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;

import org.chessworks.chess.model.Title;
import org.chessworks.chess.services.InvalidNameException;
import org.chessworks.chess.services.file.IO;
import org.chessworks.chess.services.simple.SimpleTitleService;
import org.chessworks.common.javatools.collections.CollectionHelper;
import org.chessworks.uscl.USCLBot;
import org.chessworks.uscl.model.Game;
import org.chessworks.uscl.model.GameState;
import org.chessworks.uscl.model.Player;
import org.chessworks.uscl.model.Team;
import org.chessworks.uscl.services.InvalidPlayerException;
import org.chessworks.uscl.services.InvalidTeamException;
import org.chessworks.uscl.services.simple.SimpleTournamentService;

public class FileTournamentService extends SimpleTournamentService {

	private static final File DEFAULT_PLAYERS_FILE = new File("data/Players.txt");
	private static final File DEFAULT_SCHEDULE_FILE = new File("data/Games.txt");
	private static final File DEFAULT_TEAMS_FILE = new File("data/Teams.txt");
	private final TeamsIO teamsIO = new TeamsIO();
	private final ScheduleIO scheduleIO = new ScheduleIO();
	private final PlayersIO playersIO = new PlayersIO();
	private SimpleTitleService titleService = new SimpleTitleService();

	@Override
	public Player createPlayer(String handle) throws InvalidPlayerException, InvalidTeamException {
		Player players = super.createPlayer(handle);
		playersIO.setDirty();
		return players;
	}

	@Override
	public Player createPlayer(String handle, Team team) throws InvalidPlayerException {
		Player players = super.createPlayer(handle, team);
		playersIO.setDirty();
		return players;
	}

	@Override
	public Team createTeam(String teamCode) throws InvalidTeamException {
		Team team = super.createTeam(teamCode);
		teamsIO.setDirty();
		return team;
	}

	@Override
	public void clearSchedule() {
		super.clearSchedule();
		scheduleIO.setDirty();
	}

	@Override
	public Game scheduleGame(Game game) {
		Game result = super.scheduleGame(game);
		scheduleIO.setDirty();
		return result;
	}

	@Override
    public void updateGameStatus(Game game, GameState status) {
        game.status = status;
        scheduleIO.setDirty();
    }

	@Override
	public boolean removePlayer(Player player) {
		boolean removed = super.removePlayer(player);
		playersIO.setDirty();
		scheduleIO.setDirty();
		return removed;
	}

	@Override
	public int removeTeam(Team team) {
		int playerCount = super.removeTeam(team);
		playersIO.setDirty();
		scheduleIO.setDirty();
		teamsIO.setDirty();
		return playerCount;
	}

	@Override
	public Game cancelGame(Game game) {
		Game result = super.cancelGame(game);
		if (result != null) {
			scheduleIO.setDirty();
		}
		return result;
	}

	@Override
	public void updatePlayer(Player player) {
		super.updatePlayer(player);
		playersIO.setDirty();
	}

	@Override
	public void updateTeam(Team team) {
		super.updateTeam(team);
		teamsIO.setDirty();
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

	public void setTitleService(SimpleTitleService titleService) {
		this.titleService = titleService;
	}

	public void load() {
		super.reset();
		teamsIO.load();
		playersIO.load();
		scheduleIO.load();
	}

	public void save() {
		teamsIO.save();
		playersIO.save();
		scheduleIO.save();
	}

	public void flush() {
		save();
	}

	private class PlayersIO extends IO {

		public PlayersIO() {
			super(DEFAULT_PLAYERS_FILE, UTF8);
		}

		@Override
		public void doRead(BufferedReader in) throws IOException, InvalidNameException {
			Properties data = new Properties();
			data.load(in);
			for (Entry<Object, Object> entry : data.entrySet()) {
				String propName = (String) entry.getKey();
				String propValue = (String) entry.getValue();
				if (!propName.endsWith(".handle"))
					continue;
				String prefix = propName.substring(0, propName.length() - ".handle".length());
				String handle = propValue;
				String realName = data.getProperty(prefix + ".name");
				String ratingStr = data.getProperty(prefix + ".rating");
				String teamCode = data.getProperty(prefix + ".team");
				String titleStr = data.getProperty(prefix + ".titles");
				String website = data.getProperty(prefix + ".website");
				if (website == null) website = "Unavailable";
				int rating = (ratingStr == null) ? -1 : Integer.parseInt(ratingStr);
				Team team = FileTournamentService.super.findTeam(teamCode);
				if (team == null)
					throw new InvalidTeamException("Team \"%s\" does not exist.", teamCode);
				assert team != null;
				Player p = FileTournamentService.super.createPlayer(handle, team);
				p.setRealName(realName);
				p.ratings().put(USCLBot.USCL_RATING, rating);
				if (titleStr != null) {
					Set<Title> titles = p.getTitles();
					String[] titleNames = CollectionHelper.split(titleStr);
					titleService.lookupAll(titles, titleNames);
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
				String id = handle;
				String website = player.getWebsite();
				if (website == null) website = "Unavailable";
				out.format("player.%s.handle=%s%n", id, handle);
				out.format("player.%s.name=%s%n", id, name);
				if (rating != null) {
					out.format("player.%s.rating=%d%n", id, rating);
				}
				out.format("player.%s.team=%s%n", id, teamCode);
				out.format("player.%s.titles=%s%n", id, title);
				out.format("player.%s.website=%s%n", id, website);
				out.println();
			}
		}

	};

	private final class TeamsIO extends IO {

		public TeamsIO() {
			super(DEFAULT_TEAMS_FILE, UTF8);
		}

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
				String division = data.getProperty(prefix + ".division");
				Team t = FileTournamentService.super.createTeam(teamCode);
				t.setRealName(name);
				t.setLocation(location);
				t.setWebsite(url);
				t.setDivision(division);
			}
		}

		@Override
		public void doWrite(PrintWriter out) throws IOException {
			out.println("#USCL Teams");
			out.println();
			for (Team team : findAllTeams()) {
				String code = team.getTeamCode();
				String location = team.getLocation();
				String name = team.getRealName();
				String website = team.getWebsite();
				String division = team.getDivision();
				out.format("team.%s.code=%s%n", code, code);
				out.format("team.%s.location=%s%n", code, location);
				out.format("team.%s.name=%s%n", code, name);
				if (website != null) {
					out.format("team.%s.website=%s%n", code, website);
				}
				out.format("team.%s.division=%s%n", code, division);
				out.println();
			}
		}

	};

	private final class ScheduleIO extends IO {

		public ScheduleIO() {
			super(DEFAULT_SCHEDULE_FILE, UTF8);
		}

		@Override
		public void doRead(BufferedReader in) throws IOException, InvalidPlayerException, InvalidTeamException {
			while (true) {
				String line = in.readLine();
				if (line == null)
					break;
				line = line.trim();
				if (line.length() == 0)
					continue;
				if (line.startsWith("#"))
					continue;
				String[] args = line.split("[ \t]+");
				String board = args[1];
				String white = args[2];
				String black = args[3];
                String statusString = args[4];
                String event = args[5];
				int boardNum = Integer.parseInt(board);
				int eventSlot = Integer.parseInt(event);
				Player whitePlayer = findPlayer(white);
				Player blackPlayer = findPlayer(black);
                GameState status = GameState.valueOf(statusString);
				Game g = FileTournamentService.super.scheduleGame(boardNum, eventSlot, whitePlayer, blackPlayer);
                FileTournamentService.super.updateGameStatus(g, status);
			}
		}

		@Override
		public void doWrite(PrintWriter out) throws IOException {
			out.println("#USCL Schedule");
			out.println();
			Collection<Game> schedule = FileTournamentService.super.findAllGames();
			for (Game game : schedule) {
				Player white = game.whitePlayer;
				Player black = game.blackPlayer;
				int board = game.boardNumber;
				int eventSlot = game.eventSlot;
				GameState status = game.status;
				String line = MessageFormat.format("schedule-game {0} {1} {2} {3} {4}", board, white.getHandle(), black.getHandle(), status.name(), eventSlot);
				out.println(line);
			}
			out.println();
		}

	}

}
