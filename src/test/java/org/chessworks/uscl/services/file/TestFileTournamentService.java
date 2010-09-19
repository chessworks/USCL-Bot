package org.chessworks.uscl.services.file;

import java.io.File;
import java.util.Collection;
import java.util.Map;

import junit.framework.TestCase;

import org.apache.commons.io.FileUtils;
import org.chessworks.uscl.model.Player;
import org.chessworks.uscl.model.Team;

public class TestFileTournamentService extends TestCase {

	private static final File PLAYERS_FILE = new File("src/test/resources/org/chessworks/uscl/services/file/Players.txt");
	private static final File SCHEDULE_FILE = new File("src/test/resources/org/chessworks/uscl/services/file/Games.txt");
	private static final File TEAMS_FILE = new File("src/test/resources/org/chessworks/uscl/services/file/Teams.txt");

	FileTournamentService service;
	File playersFile;
	File scheduleFile;
	File teamsFile;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		//Copy the data files so we can do practice saves.
		playersFile = File.createTempFile("Players", ".txt");
		scheduleFile = File.createTempFile("Games", ".txt");
		teamsFile = File.createTempFile("Teams", ".txt");
		FileUtils.copyFile(PLAYERS_FILE, playersFile);
		FileUtils.copyFile(SCHEDULE_FILE, scheduleFile);
		FileUtils.copyFile(TEAMS_FILE, teamsFile);
		playersFile.deleteOnExit();
		scheduleFile.deleteOnExit();
		teamsFile.deleteOnExit();
		service = new FileTournamentService();
		service.setPlayersFile(playersFile);
		service.setScheduleFile(scheduleFile);
		service.setTeamsFile(teamsFile);
	}

	public void clearSchedule() throws Exception {
		service.load();
		Player duckstorm = service.findPlayer("DuckStorm");
		int board = service.getPlayerBoard(duckstorm);
		assertEquals(90, board);
		service.clearSchedule();
		assertEquals(-1, board);
		duckstorm = service.findPlayer("DuckStorm");
		assertNotNull(duckstorm);
		Team icc = service.findTeam("ICC");
		assertNotNull(icc);
	}

	public void getPlayerBoard() throws Exception {
		service.load();
		Player duckstorm = service.findPlayer("DuckStorm");
		int board = service.getPlayerBoard(duckstorm);
		assertEquals(90, board);
	}

	public void getPlayerBoardMap() throws Exception {
		service.load();
		Map<Player, Integer> map = service.getPlayerBoardMap();
		Player duckstorm = service.findPlayer("DuckStorm");
		Integer board = map.get(duckstorm);
		assertEquals(new Integer(90), board);
		service.clearSchedule();
		board = map.get(duckstorm);
		assertEquals(null, board);
	}

	public void reserveBoard() throws Exception {
		service.load();
		service.clearSchedule();
		Player duckstorm = service.findPlayer("DuckStorm");
		assertNotNull(duckstorm);
		int board = service.getPlayerBoard(duckstorm);
		assertEquals(-1, board);
		service.reserveBoard(duckstorm, 99);
		assertEquals(69, board);
		service.save();
		service.clearSchedule();
		board = service.getPlayerBoard(duckstorm);
		assertEquals(-1, board);
		service.load();
		board = service.getPlayerBoard(duckstorm);
		assertEquals(69, board);
	}

	public void schedule() throws Exception {
	}

	public void testCreatePlayer() throws Exception {
		service.load();
		Team icc = service.findTeam("ICC");
		assertNotNull(icc);
		assertEquals("ICC", icc.getTeamCode());
		assertEquals("Internet Chess Club", icc.getLocation());
		Player test = service.findPlayer("TestPlayer-ICC");
		assertNull(test);
		Player test1 = service.createPlayer("TestPlayer-ICC");
		assertNotNull(test1);
		Player test2 = service.findPlayer("TestPlayer-ICC");
		assertNotNull(test2);
		assertSame(test1, test2);
		assertEquals("TestPlayer-ICC", test1.getHandle());
		assertEquals(icc, test1.getTeam());
		test1.setRealName("Test1");
		service.save();
		test1.setRealName("Test2");
		service = new FileTournamentService();
		service.setPlayersFile(playersFile);
		service.setScheduleFile(scheduleFile);
		service.setTeamsFile(teamsFile);
		service.load();
		Player test3 = service.findPlayer("TestPlayer-ICC");
		assertNotNull(test3);
		assertEquals("Test1", test3.getRealName());
	}

	public void testCreateTeam() throws Exception {
	}

	public void testFindAllPlayers() throws Exception {
		service.load();
		Collection<Player> players = service.findAllPlayers();
		for (Player p : players) {
			if (p.getHandle().equals("DuckStorm")) {
				return;
			}
		}
		fail();
	}

	public void testFindAllTeams() throws Exception {
		service.load();
		Collection<Team> teams = service.findAllTeams();
		for (Team t : teams) {
			if (t.getLocation().contains("Carolina")) {
				return;
			}
		}
		fail();
	}

	public void testFindPlayer() throws Exception {
		service.load();
		Player player = service.findPlayer("aramirez-DaL");
		assertEquals("ARamirez-DAL", player.getHandle());
		assertEquals("Alejandro Ramirez", player.getRealName());
	}

	public void testFindTeam() throws Exception {
		service.load();
		Team team = service.findTeam("ArZ");
		assertEquals("ARZ", team.getTeamCode());
		assertEquals("Arizona", team.getLocation());
		assertEquals("Arizona Scorpions", team.getName());
	}

	public void testReserveBoard() throws Exception {
	}

	public void testUnreserveBoard() throws Exception {
	}

	public void testLoad() throws Exception {
		service.load();
	}

	public void testSave() throws Exception {
	}

}
