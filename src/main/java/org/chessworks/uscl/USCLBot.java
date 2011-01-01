package org.chessworks.uscl;

import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Formatter;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.chessworks.bots.common.NoSuchCommandException;
import org.chessworks.bots.common.converters.ConversionException;
import org.chessworks.chess.model.PlayerState;
import org.chessworks.chess.model.RatingCategory;
import org.chessworks.chess.model.Role;
import org.chessworks.chess.model.Title;
import org.chessworks.chess.model.User;
import org.chessworks.chess.services.InvalidNameException;
import org.chessworks.chess.services.UserService;
import org.chessworks.chess.services.file.FileUserService;
import org.chessworks.chess.services.simple.SimpleTitleService;
import org.chessworks.common.javatools.ComparisionHelper;
import org.chessworks.common.javatools.collections.CollectionHelper;
import org.chessworks.common.javatools.io.FileHelper;
import org.chessworks.uscl.model.Player;
import org.chessworks.uscl.model.Team;
import org.chessworks.uscl.services.InvalidPlayerException;
import org.chessworks.uscl.services.InvalidTeamException;
import org.chessworks.uscl.services.TournamentService;
import org.chessworks.uscl.services.file.FileTournamentService;

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

	public static final String BOT_RELEASE_DATE = "September 27, 2010";

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

	/** A list of titles used by players on ICC. */
	public static final List<Title> ICC_TITLES;
	static {
		ArrayList<Title> list = new ArrayList<Title>(6);
		list.add(SimpleTitleService.FM);
		list.add(SimpleTitleService.IM);
		list.add(SimpleTitleService.GM);
		list.add(SimpleTitleService.WFM);
		list.add(SimpleTitleService.WIM);
		list.add(SimpleTitleService.WGM);
		list.trimToSize();
		ICC_TITLES = Collections.unmodifiableList(list);
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

	public static Properties loadSettingsFile(String settingsFile) {
		Properties configuredSettings = FileHelper.loadExternalPropertiesFile(settingsFile, null);
		/* System properties will override the settings file. */
		Properties systemProperties = System.getProperties();
		configuredSettings.putAll(systemProperties);
		return configuredSettings;
	}

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

	/**
	 * The name of the black player, indexed by server game id.
	 */
	//TODO: Move this into a game object.
	private String[] _blackNames = new String[5000];

	/**
	 * True if the game hasn't started or hasn't yet been announced. Indexed by server game id.
	 */
	//TODO: Move this into a game object.
	private boolean[] _needsAnnounce = new boolean[5000];

	/**
	 * The name of the white player, indexed by server game id.
	 */
	//TODO: Move this into a game object.
	private String[] _whiteNames = new String[5000];

	/**
	 * The number of people currently observing the game. Indexed by server game id.
	 */
	//TODO: Move this into a game object.
	private int[] _observerCountNow = new int[5000];

	/**
	 * The max number of people who have concurrently observed the game. Indexed by server game id.
	 */
	//TODO: Move this into a game object.
	private int[] _observerCountMax = new int[5000];

	/**
	 * The password used when executing admin commands on the server.
	 *
	 * @see #setAdminPass(String)
	 */
	private String adminPass = "*****";

	/** Utility to convert incoming tells into calls to cmdXXX(). */
	private CommandDispatcher cmd = new CommandDispatcher(this);

	/** Used to send commands to the chess server. Such as qtell, tell, reserve-game, etc. */
	private Commands command = new Commands();

	/** The underlying connection to the server. Uses Jin's connection library. */
	private Connection conn;

	/**
	 * The host name or I.P. address of the chess server.
	 *
	 * @see #setHostName(String)
	 */
	private String hostName = "chessclub.com";

	/**
	 * The TCP port number used when connecting to the chess server.
	 *
	 * @see #setHostPort(int)
	 */
	private int hostPort = 5001;

	/**
	 * When first connecting, we'll get lots of notifications about games in progress, etc.. We need to distinguish this information delivered on
	 * login from events that happen later (like a game starting that we want to announce). This setting makes that possible.
	 */
	private volatile boolean loggingIn = true;

	/**
	 * The user name used during login, such as guest.
	 *
	 * @see #setLoginName(String)
	 */
	private String loginName = "USCL-Bot";

	/**
	 * The password used during login.
	 *
	 * @see #setLoginPass(String)
	 */
	private String loginPass = "*****";

	/** The scheduler lets us schedule commands to execute at some future time. */
	private ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

	/**
	 * TitleService provides facts about known titles. For example that (IM) is short for "International Master".
	 */
	private SimpleTitleService titleService;

	/**
	 * UserService stores the list of managers & programmers authorized to use this bot.
	 */
	private UserService userService;

	/**
	 * TournamentService is used to save information to disk about who is scheduled to play whom, when, and what chess board.
	 */
	private TournamentService tournamentService;

	/** Users with the manager role can talk to the bot. */
	private Role managerRole;

	/** Users with the programmer role receive extra debugging information from the bot. */
	private Role programmerRole;

	/** The user name assigned by the chess server upon login. e.g. guest233. */
	private String userName;

	/**
	 * Sends an important (non-routine) tell to all managers to alert them to something requiring their attention.
	 *
	 * To gain the attention of the manager, this method will use both a tell and an atell.
	 */
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
			command.sendQuietly("{0} {1} {2}", tellType, user, msg);
		}
	}

	/**
	 * Sends a message to all players in the list, with the admin (*) enabled.
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
	public void broadcastAsAdmin(String tellType, Collection<User> users, String msg, Object... args) {
		if (args.length > 0) {
			msg = MessageFormat.format(msg, args);
		}
		command.sendQuietly("admin {0}", adminPass);
		for (User user : users) {
			command.sendQuietly("{0} {1} {2}", tellType, user, msg);
		}
		command.sendQuietly("admin");
	}

	/**
	 * Commands the bot to add a player to the tournament. The player name must contain the team's three letter code.
	 *
	 * Syntax: <tt>add-player Shirov-NYC</tt>
	 *
	 * @param teller
	 *            The user/manager issuing the command.
	 * @param playerHandle
	 *            The player's ICC handle.
	 */
	public void cmdAddPlayer(User teller, String playerHandle) {
		Player player = tournamentService.findPlayer(playerHandle);
		try {
			player = tournamentService.createPlayer(playerHandle);
		} catch (InvalidPlayerException e) {
			replyError(teller, e);
			return;
		} catch (InvalidTeamException e) {
			replyError(teller, e);
			command.tell(teller, "To create a new team, use the \"add-team\" command.  Usage: add-team XXX");
			return;
		}
		tournamentService.flush();
		command.tell(teller, "Done.  Player {0} has joined the \"{1}\".", player, player.getTeam());
		command.tell(teller, "To set the player''s real name, use: \"set-player {0} name 2200\"", player);
		cmdShowPlayer(teller, player);
	}

	/**
	 * Commands the bot to add a team to the tournament.
	 *
	 * Syntax: <tt>add-team NYC</tt>
	 *
	 * @param teller
	 *            The user/manager issuing the command.
	 * @param teamCode
	 *            The three-letter code for the team, such as NYC.
	 */
	public void cmdAddTeam(User teller, String teamCode) {
		Team team = tournamentService.findTeam(teamCode);
		if (team != null) {
			command.tell(teller, "A team with name {0} already exists.", teamCode);
			return;
		}
		try {
			team = tournamentService.createTeam(teamCode);
		} catch (InvalidNameException e) {
			replyError(teller, e);
			return;
		}
		tournamentService.flush();
		command.tell(teller, "Done.  Team {0} has now been created.", team.getTeamCode());
		command.tell(teller, "To set the team name, use: \"set-team {0} name New York Giants\"", team.getTeamCode());
		cmdShowTeam(teller, team);
	}

	/**
	 * Deprecated. Use {@link #cmdClearGames()} instead.
	 *
	 * @deprecated
	 */
	public void cmdClear(User teller) {
		Formatter msg = new Formatter();
		msg.format("%s\\n", " ** The new preferred name for this command is: clear-games.");
		msg.format("%s\\n", " ** The old name (\"clear\") will continue to work.");
		command.qtell(teller, msg);
		cmdClearGames(teller);
	}

	/**
	 * Commands the bot to clears all games from the schedule. Boards will no longer be reserved for players.
	 *
	 * Syntax: <tt>clear-games</tt>
	 *
	 * @param teller
	 *            The user/manager issuing the command.
	 */
	public void cmdClearGames(User teller) {
		tournamentService.clearSchedule();
		tournamentService.flush();
		command.qtell(teller, " Okay, I''ve cleared the schedule.  Tell me \"show\" to see.");
		command.sendCommand("-notify *");
	}

	/**
	 * Commands the bot to logout and shut down..
	 *
	 * Syntax: <tt>KILL</tt>
	 *
	 * @param teller
	 *            The user/manager issuing the command.
	 */
	public void cmdKill(User teller) {
		exit(3, "Quitting at the request of {0}.  Bye!", teller);
	}

	/**
	 * Commands the bot to logout, quit, and then restart itself.
	 *
	 * For this to work, the bot must be started by a shell script which knows to restart the bot upon receiving exit code 2.
	 *
	 * Syntax: <tt>REBOOT</tt>
	 *
	 * @param teller
	 *            The user/manager issuing the command.
	 */
	public void cmdReboot(User teller) {
		exit(2, "Rebooting at the request of {0}.  I''ll be right back!", teller);
	}

	/**
	 * Commands the bot to logout, quit, update to the latest software version, and then restart itself.
	 *
	 * For this to work, the bot must be started by a shell script which knows to recompile and update the bot upon receiving exit code 5.
	 *
	 * Syntax: <tt>RECOMPILE</tt>
	 *
	 * @param teller
	 *            The user/manager issuing the command.
	 */
	public void cmdRecompile(User teller) {
		exit(5, "Deploying version update at the request of {0}.  I''ll be right back!", teller);
	}

	/**
	 * Commands the bot to update the online finger notes for all players. The profile will contain the user's real name, USCL rating, USCL profile on
	 * the USCL website, team name, and team website.
	 *
	 * Syntax: <tt>refresh-all-profiles</tt>
	 *
	 * @param teller
	 *            The user/manager issuing the command.
	 * @see #cmdRefreshProfile(User, Player)
	 */
	public void cmdRefreshAllProfiles(User teller) {
		Collection<Player> players = tournamentService.findAllPlayers();
		for (Player p : players) {
			cmdRefreshProfile(teller, p);
		}
	}

	/**
	 * Commands the bot to update the online finger notes for the given player. The profile will contain the user's real name, USCL rating, USCL
	 * profile on the USCL website, team name, and team website.
	 *
	 * Syntax: <tt>refresh-profile</tt>
	 *
	 * @param teller
	 *            The user/manager issuing the command.
	 */
	public void cmdRefreshProfile(User teller, Player player) {
		Team team = player.getTeam();
		Integer r = player.ratings().get(USCL_RATING);

		String playerName = player.getTitledRealName();
		String rating = (r == null || r < 0) ? "Unavailable" : r.toString();
		String playerPage = player.getWebsite();
		String teamName = team.toString();
		String teamPage = player.getTeam().getWebsite();
		for (Title title : player.getTitles()) {
			if (ICC_TITLES.contains(title)) {
				command.sendAdminCommand("+{0} {1}", title, player);
			}
		}

		command.sendAdminCommand("set-other {0} 1 Name: {1}", player, playerName);
		command.sendAdminCommand("set-other {0} 2 USCL rating: {1}", player, rating);
		command.sendAdminCommand("set-other {0} 3 Profile page: {1}", player, playerPage);
		command.sendAdminCommand("set-other {0} 4 ", player);
		command.sendAdminCommand("set-other {0} 5 Team: {1}", player, teamName);
		command.sendAdminCommand("set-other {0} 6 Team page: {1}", player, teamPage);
		command.sendAdminCommand("set-other {0} 7", player);
	}

	/**
	 * Commands the bot to drop a player from the tournament.
	 *
	 * Syntax: <tt>remove-player Shirov-NYC</tt>
	 *
	 * @param teller
	 *            The user/manager issuing the command.
	 * @param playerHandle
	 *            The player's ICC handle.
	 */
	public void cmdRemovePlayer(User teller, Player player) {
		boolean removed = tournamentService.removePlayer(player);
		tournamentService.flush();
		if (!removed) {
			command.tell(teller, "I''m not able to find {0} in the tournament.", player);
		} else {
			command.tell(teller, "Done.  Player {0} is no longer in the tournament.", player);
		}
	}

	/**
	 * Commands the bot to drop an entire team from the tournament, including all players in the team.
	 *
	 * Syntax: <tt>remove-team NYC</tt>
	 *
	 * @param teller
	 *            The user/manager issuing the command.
	 * @param teamCode
	 *            The three-letter code for the team, such as NYC.
	 */
	public void cmdRemoveTeam(User teller, Team team) {
		int playerCount = tournamentService.removeTeam(team);
		tournamentService.flush();
		if (playerCount < 0) {
			command.tell(teller, "I''m not able to find {0} in the tournament.", team);
		} else {
			command.tell(teller, "Done.  Team {0} is no longer in the tournament.", team.getTeamCode());
			command.tell(teller, "{0} players were also removed.", playerCount);
		}
	}

	/**
	 * Commands the bot to ensure the server reserves the given board number for the player.
	 *
	 * Syntax: <tt>reserve-game Shirov-NYC 5</tt>
	 *
	 * @param teller
	 *            The user/manager issuing the command.
	 * @param playerHandle
	 *            The player's ICC handle.
	 * @param boardNum
	 *            The game/board number to reserve on the server for the game. Must be between 1 and 100, inclusive.
	 */
	public void cmdReserveGame(User teller, String playerHandle, int boardNum) {
		Player player = tournamentService.findPlayer(playerHandle);
		if (player != null) {
			playerHandle = player.getHandle();
			tournamentService.reserveBoard(player, boardNum);
		} else {
			command.tell(teller, "Warning: {0} is not a recognized player name.", playerHandle);
			command.tell(teller, "    ...  Please tell me \"addPlayer {0}\".", playerHandle);
			try {
				player = tournamentService.reserveBoard(playerHandle, boardNum, true);
			} catch (InvalidNameException e) {
				replyError(teller, e);
				return;
			}
		}
		tournamentService.flush();
		command.tell(teller, "Okay, I''ve reserved board \"{0}\" for player \"{1}\".", boardNum, player);
		command.sendCommand("+notify {0}", player);
		command.sendAdminCommand("reserve-game {0} {1}", player, boardNum);
	}

	/**
	 * Commands the bot to restart itself with an earlier stable version of the bot software. This is useful if a problem in a new version is
	 * discovered mid-tournament.
	 *
	 * For this to work, the bot must be started by a shell script which knows to restore the old version of the bot upon receiving exit code 6.
	 *
	 * Syntax: <tt>RECOMPILE</tt>
	 *
	 * @param teller
	 *            The user/manager issuing the command.
	 */
	public void cmdRevert(User teller) {
		exit(6, "Reverting to prior release at the request of {0}.  I''ll be right back!", teller);
	}

	/**
	 * Commands the bot to pair the players on the specified board.
	 *
	 * Syntax: <tt>schedule 5 Shirov-NYC DuckStorm-YVR</tt>
	 *
	 * @param teller
	 *            The user/manager issuing the command.
	 * @param boardNum
	 *            The game/board number to reserve on the server for the game. Must be between 1 and 100, inclusive.
	 * @param white
	 *            The white player's ICC handle.
	 * @param black
	 *            The white player's ICC handle.
	 */
	public void cmdSchedule(User teller, int boardNum, Player white, Player black) {
		tournamentService.schedule(white, black, boardNum);
		tournamentService.flush();
		command.sendCommand("+notify {0}", white);
		command.sendCommand("+notify {0}", black);
		command.sendAdminCommand("reserve-game {0} {1}", white, boardNum);
		command.sendAdminCommand("reserve-game {0} {1}", black, boardNum);
		command.tell(teller, "Okay, I''ve reserved board \"{0}\" for players \"{1}\" and \"{2}\".", boardNum, white, black);
	}

	/**
	 * Commands the bot to set a value in the player's profile. Such as the player's real name, USCL profile page, USCL rating, etc..
	 *
	 * Syntax: <tt>set-player Nakamura-STL fullname Hikaru Nakamura</tt><br/>
	 * Syntax: <tt>set-player Nakamura-STL title GM</tt><br/>
	 * Syntax: <tt>set-player Nakamura-STL rating 2822</tt><br/>
	 * Syntax: <tt>set-player Nakamura-STL webpage http://www.uschessleague.com/HikaruNakamura.html</tt>
	 *
	 * @param teller
	 *            The user/manager issuing the command.
	 * @param playerHandle
	 *            The player's ICC handle.
	 */
	public void cmdSetPlayer(User teller, Player player, String var, StringBuffer setting) throws MalformedURLException, InvalidNameException {
		String value = setting.toString();
		var = var.toLowerCase();
		if (ComparisionHelper.anyEquals(var, "name", "fullname")) {
			player.setRealName(value);
		} else if (ComparisionHelper.anyEquals(var, "handle", "username")) {
			player.setHandle(var);
		} else if (ComparisionHelper.anyEquals(var, "title", "titles")) {
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
		} else if (ComparisionHelper.anyEquals(var, "web", "webpage", "website")) {
			player.setWebsite(value);
		} else {
			command.tell(teller, "Unknown variable: " + var);
			return;
		}
		cmdShowPlayer(teller, player);
		cmdRefreshProfile(teller, player);
		tournamentService.updatePlayer(player);
		tournamentService.flush();
	}

	/**
	 * Commands the bot to set a value in the team's profile. Such as the team's name, USCL profile page, etc..
	 *
	 * Syntax: <tt>set-player STL name Arch Bishops</tt><br/>
	 * Syntax: <tt>set-player STL loc St. Louis</tt><br/>
	 * Syntax: <tt>set-player STL web http://www.uschessleague.com/StLouisRoster.html</tt><br/>
	 * Syntax: <tt>set-player Nakamura-STL webpage http://www.uschessleague.com/HikaruNakamura.html</tt>
	 *
	 * @param teller
	 *            The user/manager issuing the command.
	 * @param playerHandle
	 *            The player's ICC handle.
	 */
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
			command.tell(teller, "Unknown variable: " + var);
			return;
		}
		cmdShowTeam(teller, team);
		tournamentService.updateTeam(team);
		tournamentService.flush();
	}

	/**
	 * Deprecated. Use {@link #cmdShowGames()} instead.
	 *
	 * @deprecated
	 */
	public void cmdShow(User teller) {
		Formatter msg = new Formatter();
		msg.format("%s\\n", " ** The new preferred name for this command is: show-games.");
		msg.format("%s\\n", " ** The old name (\"show\") will continue to work.");
		command.qtell(teller, msg);
		cmdShowGames(teller);
	}

	/**
	 * Commands the bot to list the currently scheduled games.
	 *
	 * Syntax: <tt>show-games</tt>
	 *
	 * @param teller
	 *            The user/manager issuing the command.
	 */
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
		command.qtell(teller, msg);
	}

	/**
	 * Commands the bot to list the player's profile settings.
	 *
	 * Syntax: <tt>show-player</tt>
	 *
	 * @param teller
	 *            The user/manager issuing the command.
	 */
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
		command.qtell(teller, msg);
	}

	/**
	 * Commands the bot to list the team's profile settings.
	 *
	 * Syntax: <tt>show-team</tt>
	 *
	 * @param teller
	 *            The user/manager issuing the command.
	 */
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
		command.qtell(teller, msg);
	}

	/**
	 * Commands the bot to simulate an unexpected internal error. This is used to verify the bot will respond semi-gracefully to unexpected problems.
	 *
	 * When an unexpected error happens in the bot, the bot sends a tell to all programmers, followed by a series of qtells containing debugging
	 * information.
	 *
	 * Syntax: <tt>test-error</tt>
	 *
	 * @param teller
	 *            The user/manager issuing the command.
	 */
	public void cmdTestError(User teller) {
		try {
			throw new Exception("This is a test.  Don''t worry.");
		} catch (Exception e) {
			reportException(e);
			command.tell(teller, "Test successful.");
			return;
		}
	}

	/**
	 * Commands the bot to cancel a board reservation made previously using the reserve-game command.
	 *
	 * Syntax: <tt>unreserve-game Shirov-NYC</tt>
	 *
	 * @param teller
	 *            The user/manager issuing the command.
	 */
	public void cmdUnreserveGame(User teller, Player player) {
		int board = tournamentService.unreserveBoard(player);
		tournamentService.flush();
		if (board < 0) {
			command.tell(teller, "Sorry, player \"{0}\" was not associated wtih any boards.", player);
		} else {
			command.tell(teller, "Okay, player \"{0}\" is no longer tied to board \"{1}\".", player, board);
			command.sendCommand("-notify {0}", player);
		}
	}

	/** Shuts down the bot with the given exit code, after sending this good-bye message. */
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

	/** Returns true if the user is a bot manager and false otherwise. */
	public boolean isManager(String handle) {
		User user = userService.findUser(handle);
		boolean result = userService.isUserInRole(user, managerRole);
		return result;
	}

	/**
	 * Processes incoming commands (tells) sent to the bot by managers.
	 *
	 * When a command arrives, this method reads the first word of the tell, interpret it as the command name, and dispatch it to the corresponding
	 * cmdXXX() method. Dashes and capitalization in the command-name are ignored, ensuring that show-games will dispatch to cmdShowGames(). Any
	 * exceptions thrown by the corresponding cmdXXX() methods are converted into semi-intelligent reply messages to the user.
	 *
	 * The inner-workings of this method uses the {@link CommandDispatcher} library to parse the command arguments and invoke the proper cmdXXX
	 * method.
	 */
	private void onCommand(String teller, String message) {
		try {
			cmd.dispatch(teller, message);
		} catch (ConversionException e) {
			replyError(teller, e);
		} catch (NoSuchCommandException e) {
			command.tell(teller, "I don''t understand.  Are you sure you spelled the command correctly?");
		} catch (Exception e) {
			reportException(e);
			command.tell(teller, "Uggg, something went wrong.  Unable to execute command.");
		}
	}

	/**
	 * When first connecting to the server, this method is used to set all the preferences, such as "set noautologout 1".
	 */
	public void onConnected() {
		userName = conn.getUsername();
		tellManagers("I have arrived.");
		tellManagers("Running {0} version {1} built on {2}", BOT_RELEASE_NAME, BOT_RELEASE_NUMBER, BOT_RELEASE_DATE);
		command.sendCommand("set noautologout 1");
		command.sendCommand("set style 13");
		command.sendCommand("-notify *");
		Set<Player> players = tournamentService.getPlayerBoardMap().keySet();
		for (Player p : players) {
			command.sendCommand("+notify {0}", p);
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

	/**
	 * Indicates the bot has finished setup and processed all the initial datagrams sent by the server.
	 *
	 * After a successful login, onConnected() is called to initialize the bot's settings on the server, such as "set noautologout 1". The server will
	 * also send a large number of datagrams, such as the current state of everyone on the bot's notify list. After all this is complete,
	 * onConnectSpamDone() is called to inform the bot it is now entering normal operations mode. All future datagrams from the server can safely be
	 * assumed to represent actual activity on the server rather than initial startup.
	 */
	public void onConnectSpamDone() {
		loggingIn = false;
	}

	public void onDisconnected() {
		scheduler.shutdown();
		exit(1, "Disconnected.");
	}

	/**
	 * Handles incoming DG_MOVE_LIST datagrams from the server.
	 *
	 * The server may send a move list for a variety of reasons, such as when starting to observe a game in progress, or when using the smoves
	 * command. In this case, the datagram is always received as a result of starting to observe a game, and arrives immediately after
	 * DG_STARTED_OBSERVING.
	 *
	 * The bot uses the moves list to distinguish between new games and resumed ones. It then announces the game in the events channel. The bot also
	 * qsets the "isolated" variable for the players, to ensure they don't chat during the game.
	 */
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
			command.sshout("{0} vs {1}: {2} on board {3}.  To watch, type or click: \"observe {3}\".  Results will be announced in channel 129.",
					whiteName, blackName, startOrResume, gameNumber);
		}
		command.sendCommand("qset {0} isolated 1", whiteName);
		command.sendCommand("qset {0} isolated 1", blackName);
	}

	/**
	 * Handles incoming DG_MY_GAME_RESULT datagrams from the server.
	 *
	 * The server sends this datagram anytime a game observed by (or played by) the bot finishes or adjourns. This datagram indicates who won and how
	 * (e.g. checkmate, resign, forfeit on disconnect, etc.).
	 *
	 * The bot uses this to both inform the event channel about the result, and to remove the "isolated" setting on players. The isolated setting on
	 * the server ensures that two players can't receive tells or chat while they play.
	 */
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
			command.sendCommand("qset {0} isolated 0", whiteName);
			command.sendCommand("qset {0} isolated 0", blackName);
		}
	}

	/**
	 * Handles incoming DG_PERSONAL_TELL datagrams from the server.
	 *
	 * The server sends this datagram anytime someone sends the bot a personal tell, qtell, or atell.
	 *
	 * The bot uses this datagram to process incoming messages from players, typically commands. Commands from non-managers are ignored. Commands from
	 * Jeeves are handled as a special case, with replies going to MrBob instead. This ensures that replies sent from the bot aren't read as input
	 * Jeeves. It's been known to happen that two bots get into a private conversation, constantly telling each other that their tell contained an
	 * invalid command.
	 * */
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

	/**
	 * Handles incoming DG_NOTIFY_ARRIVED datagrams from the server.
	 *
	 * The server sends this datagram anytime a player on the bots notify list arrives on the server.
	 */
	protected void processPlayerArrived(String name) {
		Player player = tournamentService.findPlayer(name);
		if (player == null) {
			alertManagers("{0} is on my notify list, but I don''t have him in the tournament roster.", name);
		}
		int board = tournamentService.getPlayerBoard(player);
		if (board >= 0) {
			if (!loggingIn)
				tellManagers("{0} has arrived.  Reserving game {1}.", name, board);
			command.sendAdminCommand("reserve-game {0} {1}", name, board);
			command.sendCommand("observe {0}", name);
		}
	}

	/**
	 * Handles incoming DG_NOTIFY_LEFT datagrams from the server.
	 *
	 * The server sends this datagram anytime a player on the bots notify list disconnects from the server.
	 */
	protected void processPlayerDeparted(String name) {
		tellManagers("{0} departed", name);
	}

	/**
	 * Handles incoming DG_PLAYERS_IN_MY_GAME datagrams from the server.
	 *
	 * The server sends this datagram anytime a player joins the game, as a player or as an observer. This enables the bot to learn which game was the
	 * most popular (in terms of number of observers.
	 */
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
			command.qChanPlus(playerHandle, CHANNEL_USCL);
		}
	}

	/**
	 * Handles incoming DG_NOTIFY_STATE datagrams from the server.
	 *
	 * The server sends this datagram anytime a player on my notify list starts or stops playing, examining, or observing a game. If a player in the
	 * tournament starts a game, we want to observe it.
	 */
	public void processPlayerStateChange(String player, PlayerState state, int game) {
		if (state.isPlaying()) {
			command.sendCommand("observe {0}", game);
		} else {
			command.sendCommand("unobserve {0}", player);
		}
	}

	/**
	 * Handles incoming DG_STARTED_OBSERVING datagrams from the server.
	 *
	 * The server sends this datagram anytime the bot starts observing a game. It contains information about the game, such as who is playing. This
	 * datagram doesn't include the move-list for the game so far. That comes immediately after, in a DG_MOVE_LIST datagram.
	 *
	 * The bot simply stores information about the game and then waits for the move list to arrive. How the bot announces the game will depend on
	 * whether it's a resumed adjourned game or a new game. And only by inspecting the move list can the bot make this determination.
	 */
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
		command.spoof("ROBOadmin", "observe {0}", gameNumber);
		/* Announcement will occur when the move list arrives, since we can then tell if it's a resumed game. */
	}

	/** Sends a qtell to all programmers. Typically this is used to send debugging information. */
	public void qtellProgrammers(String msg, Object... args) {
		Collection<User> programmerList = userService.findUsersInRole(programmerRole);
		broadcast("qtell", programmerList, msg, args);
	}

	/**
	 * If the user sends an invalid command, this sends the user an appropriate error message. For example if the user tries to schedule user for a
	 * game, and the user doesn't exist, you'd want to say "Invalid Player Name".
	 */
	public void replyError(String teller, Throwable t) {
		t.printStackTrace(System.err);
		command.tell(teller, "Error - " + t.getMessage());
	}

	/**
	 * If the user sends an invalid command, this sends the user an appropriate error message. For example if the user tries to schedule user for a
	 * game, and the user doesn't exist, you'd want to say "Invalid Player Name".
	 */
	public void replyError(User teller, Throwable t) {
		t.printStackTrace(System.err);
		command.tell(teller, "Error - " + t.getMessage());
	}

	/**
	 * If something goes unexpectedly wrong in the bot, this will send a series of qtells to all programmers with useful debugging information.
	 */
	public void reportException(Throwable t) {
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

	/**
	 * Set the administrator password the bot uses when turning on the (*). This should always be set prior to calling {@link #start()}.
	 */
	public synchronized void setAdminPass(String adminPass) {
		this.adminPass = adminPass;
	}

	/**
	 * Sets the host name or ip address to use when connecting to the server. The default value is "chessclub.com". This is should only be set prior
	 * to calling {@link #start()}.
	 *
	 * @param hostName
	 *            the host name or ip address of the chess server.
	 */
	public synchronized void setHostName(String hostName) {
		this.hostName = hostName;
	}

	/**
	 * Sets the port number to use when connecting to the server. The default value is 5001. This is should only be set prior to calling
	 * {@link #start()}.
	 */
	public synchronized void setHostPort(int hostPort) {
		this.hostPort = hostPort;
	}

	/**
	 * Sets the host name or ip address to use when connecting to the server. The default value is "chessclub.com". This is should only be set prior
	 * to calling {@link #start()}.
	 */
	public synchronized void setHostPort(String hostPort) {
		int port = Integer.parseInt(hostPort);
		this.hostPort = port;
	}

	/**
	 * Sets the user name used during login, such as "USCL-Bot" or "guest". The default value is "USCL-Bot". This is should only be set prior to
	 * calling {@link #start()}.
	 */
	public synchronized void setLoginName(String loginName) {
		this.loginName = loginName;
	}

	/**
	 * Sets the password used during login. This should always be set prior to calling {@link #start()}.
	 */
	public synchronized void setLoginPass(String loginPass) {
		this.loginPass = loginPass;
	}

	/**
	 * Injects the instance of {@link SimpleTitleService TitleService} to use. The {@link SimpleTitleService TitleService} service is used to convert
	 * strings like "(IM)" into objects that provide additional information, such as the long form description "International Master".
	 */
	public void setTitleService(SimpleTitleService service) {
		this.titleService = service;
		this.cmd.setTitleService(titleService);
	}

	/**
	 * Injects the instance of {@link TournamentService} to use. The {@link TournamentService} service saves information to disk about the tournament,
	 * including information about players, teams, and pairings.
	 */
	public void setTournamentService(TournamentService service) {
		this.tournamentService = service;
		this.cmd.setTournamentService(tournamentService);
	}

	/**
	 * Injects the instance of {@link UserService} to use. The {@link UserService} service stores the list of managers & programmers authorized to use
	 * this bot.
	 */
	public void setUserService(UserService service) {
		this.userService = service;
		this.managerRole = service.findOrCreateRole("manager");
		this.programmerRole = service.findOrCreateRole("debugger");
		this.cmd.setUserService(service);
	}

	/**
	 * Start the bot and connect it to the chess server. All configuration changes (done via the setXXX methods) should be done prior to calling
	 * start.
	 */
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
	 * Sends tells to the event channels (129 and 399).
	 */
	public void tellEventChannels(String msg, Object... args) {
		if (args.length > 0) {
			msg = MessageFormat.format(msg, args);
		}
		command.tellAndEcho(CHANNEL_USCL, msg);
		command.tellAndEcho(CHANNEL_EVENTS_GROUP, msg);
	}

	/**
	 * Sends a routine tell to all managers. This is typically used to keep them informed of the progress of the tournament. The bot uses atells
	 * rather than regular tells, as this makes it easy for the manager to distinguish between tells sent by players (who expect a reply) and routine
	 * tells sent by the bot.
	 */
	public void tellManagers(String msg, Object... args) {
		Collection<User> managerList = userService.findUsersInRole(managerRole);
		broadcastAsAdmin("atell", managerList, msg, args);
	}

	/** Sends commands to the ICC server, such as qtell, tell, reserve-game, etc. */
	/** Used to send commands to the chess server. Such as qtell, tell, reserve-game, etc. */
	public class Commands {

		public void qChanPlus(String player, int channel) {
			sendQuietly("qchanplus {0} {1}", player, channel);
		}

		public void qtell(String handle, Formatter qtell) {
			sendQuietly("qtell {0} {1}", handle, qtell);
		}

		public void qtell(String handle, String qtell) {
			sendQuietly("qtell {0} {1}\\n", handle, qtell);
		}

		public void qtell(User user, Formatter qtell) {
			sendQuietly("qtell {0} {1}", user, qtell);
		}

		public void qtell(User user, String qtell) {
			sendQuietly("qtell {0} {1}\\n", user, qtell);
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

		public void sshout(String msg, Object... args) {
			if (args.length > 0) {
				msg = MessageFormat.format(msg, args);
			}
			sendCommand("sshout {0}", msg);
		}

		public void spoof(String handle, String command, Object... args) {
			if (args.length > 0) {
				command = MessageFormat.format(command, args);
			}
			sendCommand("spoof {0} {1}", handle, command);
		}

		/**
		 * Sends a personal tell to the user.
		 */
		public void tell(String handle, String msg, Object... args) {
			if (args.length > 0) {
				msg = MessageFormat.format(msg, args);
			}
			sendQuietly("tell {0} {1}", handle, msg);
		}

		/**
		 * Sends a personal tell to the user.
		 */
		public void tell(User user, String msg, Object... args) {
			if (args.length > 0) {
				msg = MessageFormat.format(msg, args);
			}
			sendQuietly("tell {0} {1}", user, msg);
		}

		/**
		 * Sends a tell to the channel.
		 */
		public void tellAndEcho(int channel, String msg, Object... args) {
			if (args.length > 0) {
				msg = MessageFormat.format(msg, args);
			}
			sendCommand("tell {0} {1}", channel, msg);
		}
	}

	/** The underlying connection to the chess server. This uses the Jin connection libraries. */
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
		protected void handleDisconnection(IOException e) {
			onDisconnected();
		}

		@Override
		protected void handleLoginSucceeded() {
			super.handleLoginSucceeded();
			onConnected();
		}

	}

}
