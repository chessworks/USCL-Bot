package org.chessworks.uscl;

import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.Formatter;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.chessworks.uscl.converters.ConversionException;
import org.chessworks.uscl.model.TournamentService;

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

	public static final String BOT_RELEASE_DATE = "September 7, 2010";

	public static final String BOT_RELEASE_NAME = "USCL-Bot";

	public static final String BOT_RELEASE_NUMBER = "1.01";

	public static final PrintStream ECHO_STREAM = System.out;

	public static final int CHANNEL_USCL = 129;

	public static final int CHANNEL_CHESS_FM = 165;

	public static final int CHANNEL_EVENTS_GROUP = 399;

	/**
	 * The path to the file on disk where the configured bot settings are located. The path defaults to "USCL-Bot.properties", but can be changed by
	 * setting the "usclbot.settingsFile" system property on the command-line: "-usclbot.settingsFile=myFile.properties".
	 */
	public static final String SETTINGS_FILE = System.getProperty("usclbot.settingsFile", "USCL-Bot.properties");

	public static Properties loadSettingsFile(String settingsFile) {
		Properties configuredSettings = FileHelper.loadExternalPropertiesFile(settingsFile, null);
		/* System properties will override the settings file. */
		Properties systemProperties = System.getProperties();
		configuredSettings.putAll(systemProperties);
		return configuredSettings;
	}

	public static void main(String[] args) throws IOException {
		Properties settings = loadSettingsFile(SETTINGS_FILE);

		USCLBot bot = new USCLBot();
		setConnectionSettings(settings, bot);

		String boardsFile = (args.length > 0) ? args[0] : BOARDS_FILE;
		TournamentService tourn = new TournamentService(boardsFile);
		tourn.load();
		bot.setTournamentService(tourn);
		bot.start();
	}

	private static void setConnectionSettings(Properties settings, USCLBot bot) {
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

		System.out.println("Managers:");
		String userPrefix = "user.";
		String programmerRole = "programmer";
		String managerRole = "manager";
		List<String> programmers = new LinkedList<String>();
		List<String> managers = new LinkedList<String>();
		for (Map.Entry<Object, Object> entry : settings.entrySet()) {
			String key = (String) entry.getKey();
			String role = (String) entry.getValue();
			if (!key.startsWith(userPrefix))
				continue;
			String user = key.substring(userPrefix.length());
			user = user.toLowerCase();
			if (managerRole.equals(role)) {
				managers.add(user);
			} else if (programmerRole.equals(role)) {
				managers.add(user);
				programmers.add(user);
			}
			System.out.println(role + "\t\t" + user);
		}
		bot.setProgrammerList(programmers);
		bot.setManagerList(managers);
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

	private List<String> managerList;
	private Set<String> managerSet;

	private List<String> programmerList;

	private ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

	private TournamentService tournamentService;
	/** The user name assigned by the chess server upon login. e.g. guest233. */
	private String userName;

	/** Sends an atell followed by tell to all managers. */
	public void alertManagers(String msg, Object... args) {
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
	public void broadcast(String tellType, Collection<String> users, String msg, Object... args) {
		if (args.length > 0) {
			msg = MessageFormat.format(msg, args);
		}
		for (String user : users) {
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
	public void broadcastAdmin(String tellType, Collection<String> users, String msg, Object... args) {
		if (args.length > 0) {
			msg = MessageFormat.format(msg, args);
		}
		sendQuietly("admin {0}", adminPass);
		for (String user : users) {
			sendQuietly("{0} {1} {2}", tellType, user, msg);
		}
		sendQuietly("admin");
	}

	public void cmdClear(String teller) {
		tournamentService.clearSchedule();
		try {
			tournamentService.save();
		} catch (IOException e) {
			reportException(e);
			tell(teller, "Uggg, something went wrong.  Unable to save.");
			return;
		}
		tell(teller, "Okay, I've cleared the schedule.  Tell me \"show\" to see.");
		sendCommand("-notify *");
	}

	public void cmdKill(String teller) {
		exit(3, "Quitting at the request of {0}.  Bye!", teller);
	}

	public void cmdReboot(String teller) {
		exit(2, "Rebooting at the request of {0}.  I'll be right back!", teller);
	}

	public void cmdRecompile(String teller) {
		exit(5, "Deploying version update at the request of {0}.  I'll be right back!", teller);
	}

	public void cmdReserveGame(String teller, String player, int board) {
		tournamentService.reserveBoard(player, board);
		try {
			tournamentService.save();
		} catch (Exception e) {
			reportException(e);
			tell(teller, "Uggg, something went wrong.  Unable to save.");
			return;
		}
		tell(teller, "Okay, I''ve reserved board \"{0}\" for player \"{1}\".", board, player);
		sendCommand("+notify {0}", player);
		sendAdminCommand("reserve-game {0} {1}", player, board);
	}

	public void cmdSchedule(String teller, int board, String white, String black) {
		tournamentService.schedule(white, black, board);
		sendCommand("+notify {0}", white);
		sendCommand("+notify {0}", black);
		sendAdminCommand("reserve-game {0} {1}", white, board);
		sendAdminCommand("reserve-game {0} {1}", black, board);
		tell(teller, "Okay, I''ve reserved board \"{0}\" for players \"{1}\" and \"{2}\".", board, white, black);
		try {
			tournamentService.save();
		} catch (IOException e) {
			reportException(e);
			tell(teller, "Uggg, something went wrong.  Unable to save.");
		}
	}

	public void cmdShow(String teller) {
		int indent = 0;
		Map<String, Integer> playerBoards = tournamentService.getPlayerBoardMap();
		for (String player : playerBoards.keySet()) {
			int len = player.length();
			if (len > indent)
				indent = len;
		}
		String qtellPattern = MessageFormat.format("%1${0}s - %2$d\\n", indent);
		String consolePattern = MessageFormat.format("%1${0}s - %2$d%n", indent);
		Formatter qtell = new Formatter();
		for (Entry<String, Integer> entry : playerBoards.entrySet()) {
			String player = entry.getKey();
			int board = entry.getValue();
			qtell.format(qtellPattern, player, board);
			System.out.format(consolePattern, player, board);
		}
		String msg = qtell.toString();
		sendQuietly("tell {0} Player Boards:", teller);
		sendQuietly("qtell {0} {1}", teller, msg);
	}

	public void cmdTestError(String teller) {
		try {
			throw new Exception("This is a test.  Don't worry.");
		} catch (Exception e) {
			reportException(e);
			tell(teller, "Test successful.");
			return;
		}
	}

	public void cmdUnreserveGame(String teller, String player) {
		int board = tournamentService.unreserveBoard(player);
		if (board < 0) {
			tell(teller, "Sorry, player \"{0}\" was not associated wtih any boards.", player);
		} else {
			tell(teller, "Okay, player \"{0}\" is no longer tied to board \"{1}\".", player, board);
			sendCommand("-notify {0}", player);
		}
		try {
			tournamentService.save();
		} catch (Exception e) {
			reportException(e);
			tell(teller, "Uggg, something went wrong.  Unable to save.");
			return;
		}
	}

	/** Shuts down the bot with the given exit code, after sending this exit message. */
	public void exit(int code, String msg, Object... args) {
		tellManagers(msg, args);
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
		}
		System.exit(3);
	}

	/** Returns true if the user is a bot manager. False otherwise. */
	public boolean isManager(String handle) {
		handle = handle.toLowerCase();
		boolean result = managerSet.contains(handle);
		return result;
	}

	private void onCommand(String teller, String message) {
		try {
			cmd.dispatch(teller, message);
		} catch (ConversionException e) {
			String err = e.getMessage();
			tell(teller, err);
		} catch (NoSuchCommandException e) {
			tell(teller, "I don't understand.  Are you sure you spelled the command correctly?");
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

	protected void processMoveList(int gameNumber, String initialPosition, int numHalfMoves) {
		if (!_needsAnnounce[gameNumber])
			return;
		_needsAnnounce[gameNumber] = false;
		if (loggingIn)
			return;
		String whiteName = _whiteNames[gameNumber];
		String blackName = _blackNames[gameNumber];
		String startOrResume = (numHalfMoves == 0) ? "Started" : "Resumed";
		tellEventChannels("{0} vs {1}: {2} on board {3}.  To watch, type or click: \"observe {3}\".", whiteName, blackName, startOrResume, gameNumber);
		sshout("{0} vs {1}: {2} on board {3}.  To watch, type or click: \"observe {3}\".  Results will be announced in channel 129.", whiteName,
				blackName, startOrResume, gameNumber);
	}

	protected void processMyGameResult(int gameNumber, boolean becomesExamined, String gameResultCode, String scoreString, String descriptionString) {
		String whiteName = _whiteNames[gameNumber];
		String blackName = _blackNames[gameNumber];
		/* Subtract USCL-Bot itself */
		int observerCount = _observerCountMax[gameNumber] - 1;
		tellEventChannels("{0} vs {1}: {2}  ({3} observers)", whiteName, blackName, descriptionString, observerCount);
		boolean adjourned = (descriptionString.indexOf("adjourn") >= 0);
		_whiteNames[gameNumber] = null;
		_blackNames[gameNumber] = null;
		if (!adjourned) {
			_observerCountMax[gameNumber] = 0;
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
		} else if (!isManager(teller)) {
			return;
		}
		onCommand(teller, message);
	}

	protected void processPlayerArrived(String name) {
		int board = tournamentService.getPlayerBoard(name);
		if (board < 0) {
			alertManagers("{0} is on my notify list, but I don't have a Game ID for him.", name);
		} else {
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

	private void processPlayersInMyGame(int gameNumber, String playerName, PlayerState state, boolean seesKibitz) {
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
			qChanPlus(playerName, CHANNEL_USCL);
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
		broadcast("qtell", programmerList, msg, args);
	}

	private void reportException(Throwable t) {
		t.printStackTrace(System.err);
		StringWriter w = new StringWriter();
		PrintWriter p = new PrintWriter(w);
		t.printStackTrace(p);
		String msg = w.toString();
		msg = msg.replaceAll("\n", "\\\\n");
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
		sendQuietly(command);
		qtellProgrammers(" -  {0}", command);
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

	/**
	 * @param managers
	 *            the managers to set
	 */
	public void setManagerList(List<String> managers) {
		this.managerList = managers;
		this.managerSet = new HashSet<String>();
		for (String m : managerList) {
			m = m.toLowerCase();
			managerSet.add(m);
		}
	}

	public void setProgrammerList(List<String> programmers) {
		this.programmerList = programmers;
	}

	/**
	 * @param tournamentService
	 *            the tournamentService to set
	 */
	public void setTournamentService(TournamentService service) {
		this.tournamentService = service;
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
		conn.addDatagramListener(conn, Datagram.DG_NOTIFY_ARRIVED);
		conn.addDatagramListener(conn, Datagram.DG_NOTIFY_LEFT);
		conn.addDatagramListener(conn, Datagram.DG_PERSONAL_TELL);
		conn.addDatagramListener(conn, Datagram.DG_MY_NOTIFY_LIST);
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
	public void tell(String handle, String msg, Object... args) {
		msg = MessageFormat.format(msg, args);
		sendCommand("tell {0} {1}", handle, msg);
	}

	/**
	 * Sends a tell to the channel.
	 */
	public void tell(int channel, String msg, Object... args) {
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
		tell(CHANNEL_USCL, msg);
		tell(CHANNEL_CHESS_FM, msg);
		tell(CHANNEL_EVENTS_GROUP, msg);
	}

	/**
	 * Sends a personal tell to all managers.
	 */
	public void tellManagers(String msg, Object... args) {
		broadcastAdmin("atell", managerList, msg, args);
	}

	public void qChanPlus(String player, int channel) {
		sendCommand("qchanplus {0} {1}", player, channel);
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
				String playerName = datagram.getString(1);
				String statusSymbol = datagram.getString(2);
				boolean seesKibitz = datagram.getBoolean(3);
				PlayerState status = PlayerState.forCode(statusSymbol);
				processPlayersInMyGame(gameNumber, playerName, status, seesKibitz);
				break;
			}
			}
		}

		@Override
		protected void handleLoginSucceeded() {
			super.handleLoginSucceeded();
			onConnected();
		}

	}

}
