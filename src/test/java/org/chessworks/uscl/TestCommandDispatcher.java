package org.chessworks.uscl;

import javax.annotation.security.DenyAll;
import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;

import junit.framework.Assert;
import junit.framework.TestCase;

import org.chessworks.chess.model.Role;
import org.chessworks.chess.model.User;
import org.chessworks.chess.services.simple.SimpleUserService;
import org.chessworks.uscl.model.Player;
import org.chessworks.uscl.model.Team;
import org.chessworks.uscl.services.simple.SimpleTournamentService;
import org.easymock.EasyMock;

public class TestCommandDispatcher extends TestCase {

	@RolesAllowed("user")
	public static interface Sample {

		public void cmdBasicTeller(String teller);

		@PermitAll
		public void cmdPlayerTeller(Player teller);

		public void cmdUserTeller(User teller);

		public void cmdReserveGame(String teller, Player player, int board);

		public void cmdSchedule(String teller, int boardNum, Player white, Player black);

		public void cmdUnreserveGame(String teller, Player player);
		
		@RolesAllowed("manager")
		public void cmdManagersOnly(User teller);
		
		@DenyAll
		public void cmdDenyAll(User teller);
		
		@PermitAll
		public void cmdPermitAll(User teller);

	}

	private SimpleUserService userService;
	private SimpleTournamentService tournamentService;
	private Role testRole;
	private Role managerRole;
	private User testUser;
	private Team testTeam;
	private Player testPlayer;
	private Player testPlayer2;
	private Sample mock;
	private CommandDispatcher dispatch;

	public void setUp() throws Exception {
		userService = new SimpleUserService();
		tournamentService = new SimpleTournamentService();
		testRole = userService.findOrCreateRole("user");
		managerRole = userService.findOrCreateRole("manager");
		testUser = userService.findUser("TestUser");
		testTeam = tournamentService.createTeam("XYZ");
		testPlayer = tournamentService.createPlayer("TestPlayer-XYZ");
		testPlayer2 = tournamentService.createPlayer("TestPlayer2-XYZ", testTeam);
		userService.addUserToRole(testUser,  testRole);
		mock = EasyMock.createStrictMock(Sample.class);
		dispatch = new CommandDispatcher();
		dispatch.setTournamentService(tournamentService);
		dispatch.setUserService(userService);
		dispatch.setTarget(mock);
		dispatch.init();
	}
	
	public void testRoles() {
		Assert.assertTrue(userService.isUserInRole(testUser,  testRole));
		Assert.assertFalse(userService.isUserInRole(testUser,  managerRole));
		Assert.assertTrue(userService.isUserInAnyRole(testUser,  new Role[] {testRole, managerRole}));
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

	public void testManagersOnly() throws Exception {
		EasyMock.replay();
		try {
			dispatch.dispatch("TestUser", "managers-only");
			Assert.fail();
		} catch (SecurityException e) {
			//success
		}
		EasyMock.verify();
	}

	public void testDenyAll() throws Exception {
		EasyMock.replay();
		try {
			dispatch.dispatch("TestUser", "deny-all");
			Assert.fail();
		} catch (SecurityException e) {
			//success
		}
		EasyMock.verify();
	}

	public void testUsersOnly() throws Exception {
		EasyMock.replay();
		try {
			dispatch.dispatch("Guest", "basic-teller");
			Assert.fail();
		} catch (SecurityException e) {
			//success
		}
		EasyMock.verify();
	}

}
