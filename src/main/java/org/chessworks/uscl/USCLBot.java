package org.chessworks.uscl;

import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.Formatter;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.chessworks.common.javatools.ComparisionHelper;
import org.chessworks.common.javatools.collections.CollectionHelper;
import org.chessworks.common.javatools.io.FileHelper;
import org.chessworks.uscl.converters.ConversionException;
import org.chessworks.uscl.model.Player;
import org.chessworks.uscl.model.RatingCategory;
import org.chessworks.uscl.model.Role;
import org.chessworks.uscl.model.Team;
import org.chessworks.uscl.model.Title;
import org.chessworks.uscl.model.User;
import org.chessworks.uscl.services.InvalidNameException;
import org.chessworks.uscl.services.InvalidPlayerException;
import org.chessworks.uscl.services.InvalidTeamException;
import org.chessworks.uscl.services.TournamentService;
import org.chessworks.uscl.services.UserService;
import org.chessworks.uscl.services.file.FileTournamentService;
import org.chessworks.uscl.services.file.FileUserService;
import org.chessworks.uscl.services.simple.SimpleTitleService;

import free.chessclub.ChessclubConstants;
import free.chessclub.level2.Datagram;
import free.chessclub.level2.DatagramEvent;
import free.chessclub.level2.DatagramListener;
import free.util.SafeRunnable;

/**
 * @author Doug Bateman
 */
public class USCLBot {

	/**
	 * The path to the file on disk where the configured bot settings are located. The path defaults to "Reserve-Games.b2s", but can be changed using
	 * the command-line.
	 */
	public static final String BOARDS_FILE = "Games.txt";

	public static final String BOT_RELEASE_DATE = "September 21, 2010";

	public static final String BOT_RELEASE_NAME = "USCL-Bot";

	public static final String BOT_RELEASE_NUMBER = "1.02";

	public static final PrintStream ECHO_STREAM = System.out;

	public static final int CHANNEL_USCL = 129;

	public static final int CHANNEL_CHESS_FM = 165;

	public static final int CHANNEL_EVENTS_GROUP = 399;

	/**
	 * The path to the file on disk where the configured bot settings are located. The path defaults to "USCL-Bot.properties", but can be changed by
	 * setting the "usclbot.settingsFile" system property on the command-line: "-usclbot.settingsFile=myFile.properties".
	 */
	public static final String SETTINGS_FILE = System.getProperty("usclbot.settingsFile", "USCL-Bot.properties");

	public static final RatingCategory USCL_RATING = new RatingCategory("USCL");

	public static void main(String[] args) throws IOException, InvalidNameException {
		Properties settings = loadSettingsFile(SETTINGS_FILE);

		USCLBot bot = new USCLBot();
		loadConnectionSettings(settings, bot);

		String managersFile = settings.getProperty("file.managers", "data/Managers.txt");
		String playersFile = settings.getProperty("file.players", "data/Players.txt");
		String scheduleFile = settings.getProperty("file.schedule", "data/Games.txt");
		String teamsFile = settings.getProperty("file.teams", "data/Teams.txt");

		SimpleTitleService titleService = new SimpleTitleService();

		FileUserService userService = new FileUserService();
		userService.setDataFile(managersFile);
		userService.setTitleService(titleService);
		userService.load();

		FileTournamentService tournamentService = new FileTournamentService();
		tournamentService.setPlayersFile(playersFile);
		tournamentService.setScheduleFile(scheduleFile);
		tournamentService.setTeamsFile(teamsFile);
		tournamentService.setTitleService(titleService);
		tournamentService.load();

		bot.setTitleService(titleService);
		bot.setUserService(userService);
		bot.setTournamentService(tournamentService);
		bot.start();
	}

	public static Properties loadSettingsFile(String settingsFile) {
		Properties configuredSettings = FileHelper.loadExternalPropertiesFile(settingsFile, null);
		/* System properties will override the settings file. */
		Properties systemProperties = System.getProperties();
		configuredSettings.putAll(systemProperties);
		return configuredSettings;
	}

	private static void loadConnectionSettings(Properties settings, USCLBot bot) {
		String loginName = settings.getProperty("chessclub.loginName", "USCL-Bot");
		String loginPass = settings.getProperty("chessclub.loginPass", "unknown");
		String adminPass = settings.getProperty("chessclub.adminPass", "unkonwn");
		String hostName = settings.getProperty("chessclub.hostName", "chessclub.com");
		String hostPort = settings.getProperty("chessclub.hostPort", "5001");

		System.out.println("Connection Settings:");
		System.out.println("chessclub.loginName  = " + loginName);
		System.out.println("chessclub.hostName   = " + hostName);
		System.out.println("chessclub.hostPort   = " + hostPort);
		System.out.println();
		bot.setHostName(hostName);
		bot.setHostPort(hostPort);
		bot.setLoginName(loginName);
		bot.setLoginPass(loginPass);
		bot.setAdminPass(adminPass);
	}

	//TODO: Fix this ugly hack.
	private String[] _blackNames = new String[5000];

	//TODO: Fix this ugly hack.
	private boolean[] _needsAnnounce = new boolean[5000];

	//TODO: Fix this ugly hack.
	private String[] _whiteNames = new String[5000];

	//TODO: Fix this ugly hack.
	private int[] _observerCountNow = new int[5000];

	//TODO: Fix this ugly hack.
	private int[] _observerCountMax = new int[5000];

	private String adminPass = "*****";

	private CommandDispatcher cmd = new CommandDispatcher(this);

	private Connection conn;

	private String hostName = "chessclub.com";

	private int hostPort = 5001;

	private volatile boolean loggingIn = true;

	private String loginName = "USCL-Bot";

	private String loginPass = "*****";

	private ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

	private SimpleTitleService titleService;

	private UserService userService;

	private TournamentService tournamentService;

	private Role managerRole;

	private Role programmerRole;

	/** The user name assigned by the chess server upon login. e.g. guest233. */
	private String userName;

	/** Sends an atell followed by tell to all managers. */
	public void alertManagers(String msg, Object... args) {
		Set<User> managerList = userService.findUsersInRole(managerRole);
		broadcast("tell", managerList, "!!!!!!!!! IMPORTANT ALERT !!!!!!!!!");
		tellManagers(msg, args);
	}

	/**
	 * Sends a message to all players in the list.
	 *
	 * @param tellType
	 *            The type of tell to use: "tell", "qtell", "message", etc.
	 * @param users
	 *            The users to receive the message.
	 * @param msg
	 *            The message to send. It may optionally use {@link MessageFormat} style formatting.
	 * @param args
	 *            The values inserted into {@link MessageFormat} {0} style place holders in the message.
	 */
	public void broadcast(String tellType, Collection<User> users, String msg, Object... args) {
		if (args.length > 0) {
			msg = MessageFormat.format(msg, args);
		}
		for (User user : users) {
			sendQuietly("{0} {1} {2}", tellType, user, msg);
		}
	}

	/**
	 * Sends a message to all players in the list.
	 *
	 * @param tellType
	 *            The type of tell to use: "tell", "qtell", "message", etc.
	 * @param users
	 *            The users to receive the message.
	 * @param msg
	 *            The message to send. It may optionally use {@link MessageFormat} style formatting.
	 * @param args
	 *            The values inserted into {@link MessageFormat} {0} style place holders in the message.
	 */
	public void broadcastAdmin(String tellType, Collection<User> users, String msg, Object... args) {
		if (args.length > 0) {
			msg = MessageFormat.format(msg, args);
		}
		sendQuietly("admin {0}", adminPass);
		for (User user : users) {
			sendQuietly("{0} {1} {2}", tellType, user, msg);
		}
		sendQuietly("admin");
	}

	public void cmdAddPlayer(User teller, String playerHandle) {
		Player player = tournamentService.findPlayer(playerHandle);
		try {
			player = tournamentService.createPlayer(playerHandle);
		} catch (InvalidPlayerException e) {
			replyError(teller, e);
			return;
		} catch (InvalidTeamException e) {
			replyError(teller, e);
			tell(teller, "To create a new team, use the \"add-team\" command.  Usage: add-team XXX");
			return;
		}
		tournamentService.flush();
		tell(teller, "Done.  Player {0} has joined the \"{1}\".", player, player.getTeam());
		tell(teller, "To set the player's real name, use: \"set-player {0} rating 2200\"", player);
		cmdShowPlayer(teller, player);
	}

	public void cmdAddTeam(User teller, String teamCode) {
		Team team = tournamentService.findTeam(teamCode);
		if (team != null) {
			tell(teller, "A team with name {0} already exists.", teamCode);
			return;
		}
		try {
			team = tournamentService.createTeam(teamCode);
		} catch (InvalidNameException e) {
			replyError(teller, e);
			return;
		}
		tournamentService.flush();
		tell(teller, "Done.  Team {0} has now been created.", team.getTeamCode());
		tell(teller, "To set the team name, use: \"set-team {0} name New York Giants\"", team.getTeamCode());
		cmdShowTeam(teller, team);
	}

	public void cmdClear(User teller) {
		Formatter msg = new Formatter();
		msg.format("%s\\n", " ** The new preferred name for this command is: clear-games.");
		msg.format("%s\\n", " ** The old name (\"clear\") will continue to work.");
		qtell(teller, msg);
		cmdClearGames(teller);
	}

	public void cmdClearGames(User teller) {
		tournamentService.clearSchedule();
		tournamentService.flush();
		qtell(teller, " Okay, I''ve cleared the schedule.  Tell me \"show\" to see.");
		sendCommand("-notify *");
	}

	public void cmdKill(User teller) {
		exit(3, "Quitting at the request of {0}.  Bye!", teller);
	}

	public void cmdReboot(User teller) {
		exit(2, "Rebooting at the request of {0}.  I''ll be right back!", teller);
	}

	public void cmdRecompile(User teller) {
		exit(5, "Deploying version update at the request of {0}.  I''ll be right back!", teller);
	}

	public void cmdRevert(User teller) {
		exit(6, "Reverting to prior release at the request of {0}.  I''ll be right back!", teller);
	}

	public void cmdReserveGame(User teller, String playerHandle, int board) {
		Player player = tournamentService.findPlayer(playerHandle);
		if (player != null) {
			playerHandle = player.getHandle();
			tournamentService.reserveBoard(player, board);
		} else {
			tell(teller, "Warning: {0} is not a recognized player name.", playerHandle);
			tell(teller, "    ...  Please tell me \"addPlayer {0}\".", playerHandle);
			try {
				player = tournamentService.reserveBoard(playerHandle, board, true);
			} catch (InvalidNameException e) {
				replyError(teller, e);
				return;
			}
		}
		tournamentService.flush();
		tell(teller, "Okay, I''ve reserved board \"{0}\" for player \"{1}\".", board, player);
		sendCommand("+notify {0}", player);
		sendAdminCommand("reserve-game {0} {1}", player, board);
	}

	public void cmdSchedule(User teller, int boardNum, Player white, Player black) {
		tournamentService.schedule(white, black, boardNum);
		tournamentService.flush();
		sendCommand("+notify {0}", white);
		sendCommand("+notify {0}", black);
		sendAdminCommand("reserve-game {0} {1}", white, boardNum);
		sendAdminCommand("reserve-game {0} {1}", black, boardNum);
		tell(teller, "Okay, I''ve reserved board \"{0}\" for players \"{1}\" and \"{2}\".", boardNum, white, black);
	}

	public void cmdSetPlayer(User teller, Player player, String var, StringBuffer setting) throws MalformedURLException, InvalidNameException {
		String value = setting.toString();
		var = var.toLowerCase();
		if ("name".equals(var)) {
			player.setRealName(value);
		} else if ("handle".equals(var)) {
			player.setHandle(var);
		} else if ("title".equals(var)) {
			Set<Title> titles = player.getTitles();
			titles.clear();
			String[] titleNames = CollectionHelper.split(value);
			List<Title> newTitles = titleService.lookupAll(titleNames);
			titles.addAll(newTitles);
		} else if ("rating".equals(var)) {
			Map<RatingCategory, Integer> ratings = player.ratings();
			if (value.isEmpty()) {
				ratings.remove(USCL_RATING);
			} else {
				int r = Integer.parseInt(value);
				ratings.put(USCL_RATING, r);
			}
		} else if ("website".equals(var)) {
			player.setWebsite(value);
		} else {
			tell(teller, "Unknown variable: " + var);
			return;
		}
		cmdShowPlayer(teller, player);
		cmdRefreshProfile(teller, player);
	}

	public void cmdSetTeam(User teller, Team team, String var, StringBuffer setting) throws MalformedURLException, InvalidNameException {
		String value = setting.toString();
		var = var.toLowerCase();
		if ("name".equals(var)) {
			team.setRealName(value);
		} else if (ComparisionHelper.anyEquals(var, "loc", "loc.", "location")) {
			team.setLocation(value);
		} else if (ComparisionHelper.anyEquals(var, "web", "webpage", "website")) {
			team.setWebsite(value);
		} else {
			tell(teller, "Unknown variable: " + var);
			return;
		}
		cmdShowTeam(teller, team);
	}

	private void cmdRefreshProfile(User teller, Player player) {
		Team team = player.getTeam();
		Integer r = player.ratings().get(USCL_RATING);

		String playerName = player.getTitledRealName();
		String rating = (r == null || r < 0) ? "Unavailable" : r.toString();
		String playerPage = player.getWebsite();
		String teamName = team.toString();
		String teamPage = player.getTeam().getWebsite();

		sendAdminCommand("set-other {0} 1 Name: {1}", player, playerName);
		sendAdminCommand("set-other {0} 2 USCL rating: {1}", player, rating);
		sendAdminCommand("set-other {0} 3 Profile page: {1}", player, playerPage);
		sendAdminCommand("set-other {0} 4 ", player);
		sendAdminCommand("set-other {0} 5 Team: {1}", player, teamName);
		sendAdminCommand("set-other {0} 6 Team page: {1}", player, teamPage);
		sendAdminCommand("set-other {0} 7 ", player);
	}

	public void cmdShow(User teller) {
		Formatter msg = new Formatter();
		msg.format("%s\\n", " ** The new preferred name for this command is: show-games.");
		msg.format("%s\\n", " ** The old name (\"show\") will continue to work.");
		qtell(teller, msg);
		cmdShowGames(teller);
	}

	public void cmdShowGames(User teller) {
		int indent = 0;
		Map<Player, Integer> playerBoards = tournamentService.getPlayerBoardMap();
		for (Player player : playerBoards.keySet()) {
			String handle = player.getHandle();
			int len = handle.length();
			if (len > indent)
				indent = len;
		}
		String qtellPattern = MessageFormat.format("  %1${0}s - %2$d\\n", indent);
		String consolePattern = MessageFormat.format("  %1${0}s - %2$d%n", indent);
		Formatter msg = new Formatter();
		msg.format(" Active Boards:\\n");
		for (Entry<Player, Integer> entry : playerBoards.entrySet()) {
			Player player = entry.getKey();
			int board = entry.getValue();
			msg.format(qtellPattern, player, board);
			System.out.format(consolePattern, player, board);
		}
		qtell(teller, msg);
	}

	public void cmdShowPlayer(User teller, Player player) {
		Formatter msg = new Formatter();
		Integer rating = player.ratings().get(USCL_RATING);
		String ratingStr = (rating == null || rating < 0) ? "Unavailable" : rating.toString();
		msg.format(" Player %s:\\n", player);
		msg.format("   %6s: %s\\n", "Name", player.getRealName());
		msg.format("   %6s: %s\\n", "Title", player.getTitles());
		msg.format("   %6s: %s\\n", "Rating", ratingStr);
		msg.format("   %6s: %s\\n", "Team", player.getTeam());
		msg.format("   %6s: %s\\n", "Web", player.getWebsite());
		qtell(teller, msg);
	}

	public void cmdShowTeam(User teller, Team team) {
		Formatter msg = new Formatter();
		msg.format(" Team %s:\\n", team.getTeamCode());
		msg.format("   %4s: %s\\n", "Name", team.getRealName());
		msg.format("   %4s: %s\\n", "Loc.", team.getLocation());
		msg.format("   %4s: %s\\n", "Web ", team.getWebsite());
		msg.format(" Team Members:\\n");
		int indent = 0;
		for (Player player : team.getPlayers()) {
			int len = player.getHandle().length();
			if (len > indent)
				indent = len;
		}
		String fmt = "   %s\\n";
		for (Player player : team.getPlayers()) {
			msg.format(fmt, player);
		}
		qtell(teller, msg);
	}

	public void cmdTestError(User teller) {
		try {
			throw new Exception("This is a test.  Don't worry.");
		} catch (Exception e) {
			reportException(e);
			tell(teller, "Test successful.");
			return;
		}
	}

	public void cmdUnreserveGame(User teller, Player player) {
		int board = tournamentService.unreserveBoard(player);
		tournamentService.flush();
		if (board < 0) {
			tell(teller, "Sorry, player \"{0}\" was not associated wtih any boards.", player);
		} else {
			tell(teller, "Okay, player \"{0}\" is no longer tied to board \"{1}\".", player, board);
			sendCommand("-notify {0}", player);
		}
	}

	/** Shuts down the bot with the given exit code, after sending this exit message. */
	public void exit(int code, String msg, Object... args) {
		if (conn.isConnected()) {
			tellManagers(msg, args);
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
			}
		}
		tournamentService.flush();
		userService.flush();
		System.exit(code);
	}

	/** Returns true if the user is a bot manager. False otherwise. */
	public boolean isManager(String handle) {
		User user = userService.findUser(handle);
		boolean result = userService.isUserInRole(user, managerRole);
		return result;
	}

	private void onCommand(String teller, String message) {
		try {
			cmd.dispatch(teller, message);
		} catch (ConversionException e) {
			replyError(teller, e);
		} catch (NoSuchCommandException e) {
			tell(teller, "I don''t understand.  Are you sure you spelled the command correctly?");
		} catch (Exception e) {
			reportException(e);
			tell(teller, "Uggg, something went wrong.  Unable to execute command.");
		}
	}

	public void onConnected() {
		userName = conn.getUsername();
		tellManagers("I have arrived.");
		tellManagers("Running {0} version {1} built on {2}", BOT_RELEASE_NAME, BOT_RELEASE_NUMBER, BOT_RELEASE_DATE);
		sendCommand("set noautologout 1");
		sendCommand("set style 13");
		sendCommand("-notify *");
		Set<Player> players = tournamentService.getPlayerBoardMap().keySet();
		for (Player p : players) {
			sendCommand("+notify {0}", p);
		}
		conn.addDatagramListener(conn, Datagram.DG_NOTIFY_ARRIVED);
		conn.addDatagramListener(conn, Datagram.DG_NOTIFY_LEFT);

		Runnable task = new SafeRunnable() {
			@Override
			public void safeRun() {
				onConnectSpamDone();
			}
		};
		//In 2 seconds, call onConnectSpamDone().
		scheduler.schedule(task, 2, TimeUnit.SECONDS);
		System.out.println();
	}

	public void onConnectSpamDone() {
		loggingIn = false;
	}

	public void onDisconnected() {
		scheduler.shutdown();
		exit(1, "Disconnected.");
	}

	protected void processMoveList(int gameNumber, String initialPosition, int numHalfMoves) {
		if (!_needsAnnounce[gameNumber])
			return;
		_needsAnnounce[gameNumber] = false;
		if (loggingIn)
			return;
		String whiteName = _whiteNames[gameNumber];
		String blackName = _blackNames[gameNumber];
		boolean resumed = (numHalfMoves != 0);
		String startOrResume = (!resumed) ? "Started" : "Resumed";
		tellEventChannels("{0} vs {1}: {2} on board {3}.  To watch, type or click: \"observe {3}\".", whiteName, blackName, startOrResume, gameNumber);
		if (!resumed) {
			sshout("{0} vs {1}: {2} on board {3}.  To watch, type or click: \"observe {3}\".  Results will be announced in channel 129.", whiteName,
					blackName, startOrResume, gameNumber);
		}
		sendCommand("qset {0} isolated 1", whiteName);
		sendCommand("qset {0} isolated 1", blackName);
	}

	protected void processMyGameResult(int gameNumber, boolean becomesExamined, String gameResultCode, String scoreString, String descriptionString) {
		String whiteName = _whiteNames[gameNumber];
		String blackName = _blackNames[gameNumber];
		/* Subtract USCL-Bot itself */
		int observerCount = _observerCountMax[gameNumber] - 1;
		if (whiteName == null)
			return;
		tellEventChannels("{0} vs {1}: {2}  ({3} observers)", whiteName, blackName, descriptionString, observerCount);
		boolean adjourned = (descriptionString.indexOf("adjourn") >= 0);
		_whiteNames[gameNumber] = null;
		_blackNames[gameNumber] = null;
		if (!adjourned) {
			_observerCountMax[gameNumber] = 0;
			sendCommand("qset {0} isolated 0", whiteName);
			sendCommand("qset {0} isolated 0", blackName);
		}
	}

	protected void processPersonalTell(String teller, String titles, String message, int tellType) {
		if (tellType != ChessclubConstants.REGULAR_TELL)
			return;
		String myName = userName;
		if (myName.equals(teller)) {
			return;
		}
		boolean jeeves = "Jeeves".equals(teller);
		if (jeeves) {
			alertManagers("{0} just executed command: {1}", teller, message);
			teller = "MrBob";
		} else {
			if (!isManager(teller)) {
				return;
			}
		}
		onCommand(teller, message);
	}

	protected void processPlayerArrived(String name) {
		Player player = tournamentService.findPlayer(name);
		if (player == null) {
			alertManagers("{0} is on my notify list, but I don''t have him in the tournament roster.", name);
		}
		int board = tournamentService.getPlayerBoard(player);
		if (board >= 0) {
			if (!loggingIn)
				tellManagers("{0} has arrived.  Reserving game {1}.", name, board);
			sendAdminCommand("reserve-game {0} {1}", name, board);
			sendCommand("observe {0}", name);
		}
	}

	protected void processPlayerDeparted(String name) {
		tellManagers("{0} departed", name);
	}

	public void processPlayerStateChange(String player, PlayerState state, int game) {
		if (state.isPlaying()) {
			USCLBot.this.sendCommand("observe {0}", game);
		} else {
			USCLBot.this.sendCommand("unobserve {0}", player);
		}
	}

	private void processPlayersInMyGame(int gameNumber, String playerHandle, PlayerState state, boolean seesKibitz) {
		switch (state) {
		case NONE:
			_observerCountNow[gameNumber]--;
			break;
		case OBSERVING:
			_observerCountNow[gameNumber]++;
			if (_observerCountMax[gameNumber] < _observerCountNow[gameNumber]) {
				_observerCountMax[gameNumber] = _observerCountNow[gameNumber];
			}
			//Fall through...
		case PLAYING:
			qChanPlus(playerHandle, CHANNEL_USCL);
		}
	}

	protected void processStartedObserving(int gameNumber, String whiteName, String blackName, int wildNumber, String ratingCategoryString,
			boolean isRated, int whiteInitial, int whiteIncrement, int blackInitial, int blackIncrement, boolean isPlayedGame, String exString,
			int whiteRating, int blackRating, long gameID, String whiteTitles, String blackTitles, boolean isIrregularLegality,
			boolean isIrregularSemantics, boolean usesPlunkers, String fancyTimeControls) {
		if (isPlayedGame) {
			_needsAnnounce[gameNumber] = true;
			_whiteNames[gameNumber] = whiteName;
			_blackNames[gameNumber] = blackName;
		} else {
			_needsAnnounce[gameNumber] = false;
			_whiteNames[gameNumber] = null;
			_blackNames[gameNumber] = null;
		}
		_observerCountNow[gameNumber] = 0;
		/* Announcement will occur when the move list arrives, since we can then tell if it's a resumed game. */
	}

	public void qtellProgrammers(String msg, Object... args) {
		Collection<User> programmerList = userService.findUsersInRole(programmerRole);
		broadcast("qtell", programmerList, msg, args);
	}

	private void replyError(String teller, Throwable t) {
		t.printStackTrace(System.err);
		tell(teller, t.getMessage());
	}

	private void replyError(User user, Throwable t) {
		t.printStackTrace(System.err);
		tell(user, "Error - " + t.getMessage());
	}

	private void reportException(Throwable t) {
		t.printStackTrace(System.err);
		StringWriter w = new StringWriter();
		PrintWriter p = new PrintWriter(w);
		t.printStackTrace(p);
		String msg = w.toString();
		msg = msg.replaceAll("\n", "\\\\n");
		Collection<User> programmerList = userService.findUsersInRole(programmerRole);
		broadcast("tell", programmerList, t.toString());
		qtellProgrammers(msg);
	}

	public void sendAdminCommand(String command, Object... args) {
		if (args.length > 0) {
			command = MessageFormat.format(command, args);
		}
		sendQuietly("admin {0}", adminPass);
		sendCommand(command);
		sendQuietly("admin");
	}

	/**
	 * Sends a command to the server, and echo it as a qtell to all managers.
	 */
	public void sendCommand(String command, Object... args) {
		if (args.length > 0) {
			command = MessageFormat.format(command, args);
		}
		qtellProgrammers(" -  {0}", command);
		sendQuietly(command);
	}

	/**
	 * Sends a command to the server. The command is not echoed as a qtell to managers.
	 */
	public void sendQuietly(String command, Object... args) {
		if (args.length > 0) {
			command = MessageFormat.format(command, args);
		}
		conn.sendCommand(command, true, false, null);
	}

	/**
	 * Set the administrator password the bot uses when turning on the (*).
	 */
	public synchronized void setAdminPass(String adminPass) {
		this.adminPass = adminPass;
	}

	/**
	 * The host name or ip address of the chess server. This defaults to "chessclub.com".
	 *
	 * @param hostName
	 *            the hostName to set
	 */
	public synchronized void setHostName(String hostName) {
		this.hostName = hostName;
	}

	/**
	 * @param hostPort
	 *            the hostPort to set
	 */
	public synchronized void setHostPort(int hostPort) {
		this.hostPort = hostPort;
	}

	/**
	 * @param hostPort
	 *            the hostPort to set
	 */
	public synchronized void setHostPort(String hostPort) {
		int port = Integer.parseInt(hostPort);
		this.hostPort = port;
	}

	/**
	 * @param loginName
	 *            the loginName to set
	 */
	public synchronized void setLoginName(String loginName) {
		this.loginName = loginName;
	}

	/**
	 * @param loginPass
	 *            the loginPass to set
	 */
	public synchronized void setLoginPass(String loginPass) {
		this.loginPass = loginPass;
	}

	public void setTitleService(SimpleTitleService service) {
		this.titleService = service;
		this.cmd.setTitleService(titleService);
	}

	public void setTournamentService(TournamentService service) {
		this.tournamentService = service;
		this.cmd.setTournamentService(tournamentService);
	}

	public void setUserService(UserService service) {
		this.userService = service;
		this.managerRole = service.findOrCreateRole("manager");
		this.programmerRole = service.findOrCreateRole("debugger");
		this.cmd.setUserService(service);
	}

	private void sshout(String msg, Object... args) {
		if (args.length > 0) {
			msg = MessageFormat.format(msg, args);
		}
		sendCommand("sshout {0}", msg);
	}

	public void start() throws IOException {
		System.out.println("Starting USCL-Bot...");
		System.out.println();
		conn = new Connection(hostName, hostPort, loginName, loginPass);
		conn.addDatagramListener(conn, Datagram.DG_PERSONAL_TELL);
		conn.addDatagramListener(conn, Datagram.DG_NOTIFY_STATE);
		conn.addDatagramListener(conn, Datagram.DG_MY_GAME_RESULT);
		conn.addDatagramListener(conn, Datagram.DG_STARTED_OBSERVING);
		conn.addDatagramListener(conn, Datagram.DG_MOVE_LIST);
		conn.addDatagramListener(conn, Datagram.DG_SEND_MOVES);
		conn.addDatagramListener(conn, Datagram.DG_PLAYERS_IN_MY_GAME);
		conn.initiateConnect(hostName, hostPort);
	}

	/**
	 * Sends a personal tell to the user.
	 */
	public void tell(User user, String msg, Object... args) {
		msg = MessageFormat.format(msg, args);
		sendQuietly("tell {0} {1}", user, msg);
	}

	/**
	 * Sends a personal tell to the user.
	 */
	public void tell(String handle, String msg, Object... args) {
		msg = MessageFormat.format(msg, args);
		sendQuietly("tell {0} {1}", handle, msg);
	}

	public void qtell(String handle, String qtell) {
		sendQuietly("qtell {0} {1}\\n", handle, qtell);
	}

	public void qtell(User user, String qtell) {
		sendQuietly("qtell {0} {1}\\n", user, qtell);
	}

	public void qtell(String handle, Formatter qtell) {
		sendQuietly("qtell {0} {1}", handle, qtell);
	}

	public void qtell(User user, Formatter qtell) {
		sendQuietly("qtell {0} {1}", user, qtell);
	}

	/**
	 * Sends a tell to the channel.
	 */
	public void tellAndEcho(int channel, String msg, Object... args) {
		msg = MessageFormat.format(msg, args);
		sendCommand("tell {0} {1}", channel, msg);
	}

	/**
	 * Sends tells to the event channels (129, 165, and 399).
	 */
	private void tellEventChannels(String msg, Object... args) {
		if (args.length > 0) {
			msg = MessageFormat.format(msg, args);
		}
		tellAndEcho(CHANNEL_USCL, msg);
		tellAndEcho(CHANNEL_EVENTS_GROUP, msg);
	}

	/**
	 * Sends a personal tell to all managers.
	 */
	public void tellManagers(String msg, Object... args) {
		Collection<User> managerList = userService.findUsersInRole(managerRole);
		broadcastAdmin("atell", managerList, msg, args);
	}

	public void qChanPlus(String player, int channel) {
		sendQuietly("qchanplus {0} {1}", player, channel);
	}

	private class Connection extends free.chessclub.ChessclubConnection implements DatagramListener {

		public Connection(String hostname, int port, String username, String password) {
			super(username, password, ECHO_STREAM);
		}

		@Override
		public void datagramReceived(DatagramEvent evt) {
			Datagram datagram = evt.getDatagram();
			int code = datagram.getId();
			switch (code) {
			case Datagram.DG_NOTIFY_ARRIVED: {
				String player = datagram.getString(0);
				processPlayerArrived(player);
				break;
			}
			case Datagram.DG_NOTIFY_LEFT: {
				String player = datagram.getString(0);
				processPlayerDeparted(player);
				break;
			}
			case Datagram.DG_NOTIFY_STATE: {
				String player = datagram.getString(0);
				String stateCode = datagram.getString(1);
				PlayerState status = PlayerState.forCode(stateCode);
				int game = datagram.getInteger(2);
				processPlayerStateChange(player, status, game);
				break;
			}
			case Datagram.DG_PERSONAL_TELL: {
				String teller = datagram.getString(0);
				String titles = datagram.getString(1);
				String message = datagram.getString(2);
				int tellType = datagram.getInteger(3);
				if (tellType == ChessclubConstants.REGULAR_TELL) {
					processPersonalTell(teller, titles, message, tellType);
				}
				break;
			}
			case Datagram.DG_MY_NOTIFY_LIST: {
				break;
			}
			case Datagram.DG_MY_GAME_RESULT: {
				int gameNumber = datagram.getInteger(0);
				boolean becomesExamined = datagram.getBoolean(1);
				String gameResultCode = datagram.getString(2);
				String scoreString = datagram.getString(3);
				String descriptionString = datagram.getString(4);
				processMyGameResult(gameNumber, becomesExamined, gameResultCode, scoreString, descriptionString);
				break;
			}
			case Datagram.DG_STARTED_OBSERVING: {
				int gameNumber = datagram.getInteger(0);
				String whiteName = datagram.getString(1);
				String blackName = datagram.getString(2);
				int wildNumber = datagram.getInteger(3);
				String ratingCategoryString = datagram.getString(4);
				boolean isRated = datagram.getBoolean(5);
				int whiteInitial = datagram.getInteger(6);
				int whiteIncrement = datagram.getInteger(7);
				int blackInitial = datagram.getInteger(8);
				int blackIncrement = datagram.getInteger(9);
				boolean isPlayedGame = datagram.getBoolean(10);
				String exString = datagram.getString(11);
				int whiteRating = datagram.getInteger(12);
				int blackRating = datagram.getInteger(13);
				long gameID = datagram.getLong(14);
				String whiteTitles = datagram.getString(15);
				String blackTitles = datagram.getString(16);
				boolean isIrregularLegality = datagram.getBoolean(17);
				boolean isIrregularSemantics = datagram.getBoolean(18);
				boolean usesPlunkers = datagram.getBoolean(19);
				String fancyTimeControls = datagram.getString(20);
				processStartedObserving(gameNumber, whiteName, blackName, wildNumber, ratingCategoryString, isRated, whiteInitial, whiteIncrement,
						blackInitial, blackIncrement, isPlayedGame, exString, whiteRating, blackRating, gameID, whiteTitles, blackTitles,
						isIrregularLegality, isIrregularSemantics, usesPlunkers, fancyTimeControls);
				break;
			}
			case Datagram.DG_MOVE_LIST: {
				int gameNumber = datagram.getInteger(0);
				String initialPosition = datagram.getString(1);
				int numHalfMoves = datagram.getFieldCount() - 2;
				processMoveList(gameNumber, initialPosition, numHalfMoves);
				break;
			}
			case Datagram.DG_SEND_MOVES: {
				break;
			}
			case Datagram.DG_PLAYERS_IN_MY_GAME: {
				int gameNumber = datagram.getInteger(0);
				String playerHandle = datagram.getString(1);
				String statusSymbol = datagram.getString(2);
				boolean seesKibitz = datagram.getBoolean(3);
				PlayerState status = PlayerState.forCode(statusSymbol);
				processPlayersInMyGame(gameNumber, playerHandle, status, seesKibitz);
				break;
			}
			}
		}

		@Override
		protected void handleLoginSucceeded() {
			super.handleLoginSucceeded();
			onConnected();
		}

		@Override
		protected void handleDisconnection(IOException e) {
			onDisconnected();
		}

	}

}
