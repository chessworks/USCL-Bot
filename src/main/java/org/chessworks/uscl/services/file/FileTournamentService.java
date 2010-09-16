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
import org.chessworks.uscl.services.InvalidNameException;
import org.chessworks.uscl.services.simple.SimpleTitleService;
import org.chessworks.uscl.services.simple.SimpleTournamentService;

public class FileTournamentService extends SimpleTournamentService {

	private File playersFile = new File("data/Players.txt");
	private File scheduleFile = new File("data/Games.txt");
	private File teamsFile = new File("data/Teams.txt");
	private SimpleTitleService titleService = new SimpleTitleService();

	public void load() throws IOException {
		teamsIO.readText();
		playersIO.readText();
		scheduleIO.readText();
	}

	public void save() throws IOException {
		teamsIO.writeText();
		playersIO.writeText();
		scheduleIO.writeText();
	}

	public Player createPlayer(String handle) throws InvalidNameException {
		playersIO.setDirty();
		return super.createPlayer(handle);
	}

	public Team createTeam(String teamCode) throws InvalidNameException {
		teamsIO.setDirty();
		return super.createTeam(teamCode);
	}

	public void clearSchedule() {
		scheduleIO.setDirty();
		super.clearSchedule();
	}

	public void schedule(Player white, Player black, int board) {
		scheduleIO.setDirty();
		super.schedule(white, black, board);
	}

	public void reserveBoard(Player player, int board) {
		scheduleIO.setDirty();
		super.reserveBoard(player, board);
	}

	public int unreserveBoard(Player player) {
		scheduleIO.setDirty();
		return super.unreserveBoard(player);
	}

	public void setPlayersFile(File file) {
		this.teamsFile = file;
	}

	public void setPlayersFile(String fileName) {
		this.teamsFile = new File(fileName);
	}

	public void setScheduleFile(File file) {
		this.scheduleFile = file;
	}

	public void setScheduleFile(String fileName) {
		this.scheduleFile = new File(fileName);
	}

	public void setTeamsFile(File file) {
		this.teamsFile = file;
	}

	public void setTeamsFile(String fileName) {
		this.teamsFile = new File(fileName);
	}

	private DirtyFileHelper playersIO = new DirtyFileHelper(playersFile, UTF8) {

		@Override
		public void doRead(BufferedReader in) throws IOException, InvalidNameException {
			Properties data = new Properties();
			data.load(in);
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
				String title = data.getProperty(prefix + ".titles");
				String website = data.getProperty(prefix + ".website");
				int rating = Integer.parseInt(ratingStr);
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
		}

	};

	private DirtyFileHelper teamsIO = new DirtyFileHelper(teamsFile, UTF8) {

		@Override
		public void doRead(BufferedReader in) throws IOException, InvalidNameException {
			Properties data = new Properties();
			data.load(in);
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

	private final DirtyFileHelper scheduleIO = new DirtyFileHelper(scheduleFile, UTF8) {

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
