package org.chessworks.uscl;

import junit.framework.TestCase;

import org.chessworks.chess.model.User;
import org.chessworks.chess.services.simple.SimpleUserService;
import org.chessworks.uscl.model.Player;
import org.chessworks.uscl.model.Team;
import org.chessworks.uscl.services.simple.SimpleTournamentService;
import org.easymock.EasyMock;

public class TestCommandDispatcher extends TestCase {

	public static interface Sample {

		public void cmdBasicTeller(String teller);

		public void cmdPlayerTeller(Player teller);

		public void cmdUserTeller(User teller);

		public void cmdReserveGame(String teller, Player player, int board);

		public void cmdSchedule(String teller, int boardNum, Player white, Player black);

		public void cmdUnreserveGame(String teller, Player player);

	}

	private SimpleUserService userService;
	private SimpleTournamentService tournamentService;
	private User testUser;
	private Team testTeam;
	private Player testPlayer;
	private Player testPlayer2;
	private Sample mock;
	private CommandDispatcher dispatch;

	public void setUp() throws Exception {
		userService = new SimpleUserService();
		tournamentService = new SimpleTournamentService();
		testUser = userService.findUser("TestUser");
		testTeam = tournamentService.createTeam("XYZ");
		testPlayer = tournamentService.createPlayer("TestPlayer-XYZ");
		testPlayer2 = tournamentService.createPlayer("TestPlayer2-XYZ", testTeam);
		mock = EasyMock.createStrictMock(Sample.class);
		dispatch = new CommandDispatcher();
		dispatch.setTarget(mock);
		dispatch.setTournamentService(tournamentService);
		dispatch.setUserService(userService);
	}

	public void testBasicTeller() throws Exception {
		mock.cmdBasicTeller("TestUser");
		EasyMock.replay();
		dispatch.dispatch("TestUser", "basicTeller");
		EasyMock.verify();
	}

	public void testPlayerTeller() throws Exception {
		mock.cmdUserTeller(testPlayer);
		EasyMock.replay();
		dispatch.dispatch("TestPlayer-XYZ", "playerTeller");
		EasyMock.verify();
	}

	public void testUserTeller() throws Exception {
		mock.cmdUserTeller(testUser);
		EasyMock.replay();
		dispatch.dispatch("TestUser", "userTeller");
		EasyMock.verify();
	}

	public void testReserveGame() throws Exception {
		mock.cmdReserveGame("TestUser", testPlayer, 5);
		EasyMock.replay();
		dispatch.dispatch("TestUser", "reserve-game TestPlayer-XYZ 5");
		EasyMock.verify();
	}

	public void testSchedule() throws Exception {
		mock.cmdSchedule("TestUser", 6, testPlayer, testPlayer2);
		EasyMock.replay();
		dispatch.dispatch("TestUser", "schedule 6 TestPlayer-XYZ TestPlayer2-XYZ");
		EasyMock.verify();
	}

	public void testUnreserveGame() throws Exception {
		mock.cmdReserveGame("TestUser", testPlayer, 5);
		mock.cmdUnreserveGame("TestUser", testPlayer);
		dispatch.dispatch("TestUser", "reserve-game TestPlayer-XYZ 5");
		dispatch.dispatch("TestUser", "unreserve-game TestPlayer-XYZ");
		EasyMock.verify();
	}

}
