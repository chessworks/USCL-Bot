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
import java.util.concurrent.Semaphore;

import org.chessworks.uscl.converters.ConversionException;
import org.chessworks.uscl.model.TournamentService;

import free.chessclub.ChessclubConnection;
import free.chessclub.level2.Datagram;

/**
 * @author Doug Bateman
 */
public class USCLBot {

	/**
	 * The path to the file on disk where the configured bot settings are located. The path defaults to "Reserve-Games.b2s", but can be changed using
	 * the command-line.
	 */
	public static final String BOARDS_FILE = "Reserve-Games.b2s";

	public static final String BOT_RELEASE_DATE = "August 26, 2010";

	public static final String BOT_RELEASE_NAME = "USCL-Bot";

	public static final String BOT_RELEASE_NUMBER = "1.0 beta 2";

	public static final PrintStream ECHO_STREAM = System.out;

	/**
	 * The path to the file on disk where the configured bot settings are located. The path defaults to "USCL-Bot.properties", but can be changed by
	 * setting the "usclBot.settingsFile" system property on the command-line: "-usclBot.settingsFile=myFile.properties".
	 */
	public static final String SETTINGS_FILE = System.getProperty("usclBot.settingsFile", "USCL-Bot.properties");

	/**
	 * The bot sends this string to itself to indicate it's done with startup.
	 */
	private static final String STARTUP_COMPLETE_SIGNAL = "-- startup complete";

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

	private String adminPass = "*****";

	private CommandDispatcher cmd = new CommandDispatcher(this);

	private Connection conn;

	private String hostName = "chessclub.com";

	private int hostPort = 5001;

	private String loginName = "USCL-Bot";

	private String loginPass = "*****";

	private List<String> programmerList;

	private List<String> managerList;

	private Set<String> managerSet;

	private Semaphore startupLock = new Semaphore(0);

	private TournamentService tournamentService;

	/** The user name assigned by the chess server upon login. e.g. guest233. */
	private String userName;

	/** Sends an atell followed by tell to all managers. */
	public void alertManagers(String msg, Object... args) {
		broadcast("atell", managerList, "!!!!!!!!! IMPORTANT ALERT !!!!!!!!!");
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

	public void cmdKill(String teller) {
		tellManagers("Quitting at the request of {0}.  Bye!");
		System.exit(3);
	}

	public void cmdReboot(String teller) {
		tellManagers("Rebooting at the request of {0}.  I'll be right back!");
		System.exit(2);
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

	public void cmdTestError(String teller) {
		try {
			throw new Exception("This is a test.  Don't worry.");
		} catch (Exception e) {
			reportException(e);
			tell(teller, "Test successful.");
			return;
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

	private void onPlayerArrived(String name) {
		int board = tournamentService.getPlayerBoard(name);
		if (board < 0) {
			alertManagers("{0} is on my notify list, but I don't have a Game ID for him.", name);
		} else {
			tellManagers("{0} has arrived.  Reserving game {1}.", name, board);
			sendAdminCommand("reserve-game {0} {1}", name, board);
		}
	}

	private void onPlayerDeparted(String name) {
		tellManagers("{0} departed", name);
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
		tellProgrammers(t.toString());
		qtellProgrammers(msg);
	}

	public void sendAdminCommand(String command, Object... args) {
		sendQuietly("admin {0}", adminPass);
		if (args.length > 0) {
			command = MessageFormat.format(command, args);
		}
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
		conn.sendCommand(command);
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

	public void start() throws IOException {
		System.out.println("Starting USCL-Bot...");
		System.out.println();
		conn = new Connection(hostName, hostPort, loginName, loginPass);
		conn.connect();
		userName = conn.getUsername();
		tellManagers("I have arrived.");
		tellManagers("Running {0} version {1} built on {2}", BOT_RELEASE_NAME, BOT_RELEASE_NUMBER, BOT_RELEASE_DATE);
		conn.setDGState(Datagram.DG_NOTIFY_ARRIVED, true);
		conn.setDGState(Datagram.DG_NOTIFY_LEFT, true);
		conn.setDGState(Datagram.DG_PERSONAL_TELL, true);
		conn.setDGState(Datagram.DG_MY_NOTIFY_LIST, true);
		conn.setDGState(Datagram.DG_NOTIFY_STATE, true);
		conn.setDGState(Datagram.DG_MY_GAME_RESULT, true);
		conn.setDGState(Datagram.DG_STARTED_OBSERVING, true);
		sendQuietly("tell {0} {1}", conn.getUsername(), STARTUP_COMPLETE_SIGNAL);
		startupLock.acquireUninterruptibly();
		System.out.println();
	}

	/**
	 * Sends a personal tell to the user.
	 */
	public void tell(String handle, String msg, Object... args) {
		msg = MessageFormat.format(msg, args);
		sendCommand("tell {0} {1}", handle, msg);
	}

	/**
	 * Sends a personal tell to all managers.
	 */
	public void tellManagers(String msg, Object... args) {
		broadcast("tell", managerList, msg, args);
	}

	/**
	 * Sends a personal tell to all managers.
	 */
	public void tellProgrammers(String msg, Object... args) {
		broadcast("tell", programmerList, msg, args);
	}

	private class Connection extends free.chessclub.ChessclubConnection {

		public Connection(String hostname, int port, String username, String password) {
			super(hostname, port, username, password, ECHO_STREAM);
		}

		@Override
		protected void processDatagram(Datagram datagram) {
			int code = datagram.getType();
			String player = datagram.getString(0);
			switch (code) {
			case Datagram.DG_NOTIFY_ARRIVED:
				onPlayerArrived(player);
				break;
			case Datagram.DG_NOTIFY_LEFT:
				onPlayerDeparted(player);
				break;
			case Datagram.DG_NOTIFY_STATE:
				String state = datagram.getString(1);
				int game = datagram.getInteger(2);
				onPlayerStateChange(player, state, game);
				break;
			case Datagram.DG_NOTIFY_OPEN:
				break;
			}
			super.processDatagram(datagram);
		}

		public void onPlayerStateChange(String player, String state, int game) {
			if ("P".equals(state)) {
				USCLBot.this.sendCommand("observe {0}", game);
			} else {
				USCLBot.this.sendCommand("unobserve {0}", player);
			}
		}

		@Override
		protected void processStartedObserving(int gameNumber, String whiteName, String blackName, int wildNumber, String ratingCategoryString,
				boolean isRated, int whiteInitial, int whiteIncrement, int blackInitial, int blackIncrement, boolean isPlayedGame, String exString,
				int whiteRating, int blackRating, long gameID, String whiteTitles, String blackTitles, boolean isIrregularLegality,
				boolean isIrregularSemantics, boolean usesPlunkers, String fancyTimeControls) {
			tellManagers("{0} vs {1} has started on board {2}", whiteName, blackName, gameNumber);
		}

		@Override
		protected void processMyGameResult(int gameNumber, boolean becomesExamined, String gameResultCode, String scoreString,
				String descriptionString) {
			tellManagers("Game {0} ended: {1}.", gameNumber, descriptionString);
		}

		@Override
		protected void processPersonalTell(String teller, String titles, String message, int tellType) {
			if (tellType != ChessclubConnection.TELL)
				return;
			String myName = userName;
			if (myName.equals(teller)) {
				if (message.equals(STARTUP_COMPLETE_SIGNAL)) {
					startupLock.release();
				}
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

	}

}
