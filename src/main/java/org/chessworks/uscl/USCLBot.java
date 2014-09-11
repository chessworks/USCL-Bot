package org.chessworks.uscl;

import java.io.FileNotFoundException;
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
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;

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
import org.chessworks.chessclub.ChatType;
import org.chessworks.common.javatools.ComparisionHelper;
import org.chessworks.common.javatools.collections.CollectionHelper;
import org.chessworks.common.javatools.exceptions.BaseException;
import org.chessworks.common.javatools.io.FileHelper;
import org.chessworks.uscl.model.Game;
import org.chessworks.uscl.model.GameState;
import org.chessworks.uscl.model.Player;
import org.chessworks.uscl.model.Team;
import org.chessworks.uscl.services.InvalidPlayerException;
import org.chessworks.uscl.services.InvalidTeamException;
import org.chessworks.uscl.services.TournamentService;
import org.chessworks.uscl.services.file.FileTournamentService;
import org.chessworks.uscl.services.file.UsclSettingsService;

import free.chessclub.ChessclubConstants;
import free.chessclub.level2.Datagram;
import free.chessclub.level2.DatagramEvent;
import free.chessclub.level2.DatagramListener;
import free.util.SafeRunnable;

/**
 * @author Doug Bateman
 */
@RolesAllowed("manager")
public class USCLBot {

    /**
     * The path to the file on disk where the configured bot settings are located. The path defaults to "Reserve-Games.b2s", but can be changed using
     * the command-line.
     */
    public static final String BOARDS_FILE = "Games.txt";
    public static final String BOT_RELEASE_DATE = "September 10, 2014";
    public static final String BOT_RELEASE_NAME = "USCL-Bot";
    public static final String BOT_RELEASE_NUMBER = "1.13";
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
    public static final List<Title> USCL_TITLES;

    static {
        ArrayList<Title> list = new ArrayList<Title>(6);
        list.add(SimpleTitleService.FM);
        list.add(SimpleTitleService.IM);
        list.add(SimpleTitleService.GM);
        list.add(SimpleTitleService.WFM);
        list.add(SimpleTitleService.WIM);
        list.add(SimpleTitleService.WGM);
        list.add(SimpleTitleService.NM);
        list.trimToSize();
        USCL_TITLES = Collections.unmodifiableList(list);
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
        String settingsFile = settings.getProperty("file.settings", "data/Settings.txt");

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
        
        UsclSettingsService settingsService = new UsclSettingsService();
        settingsService.setSettingsFile(settingsFile);
        settingsService.load();

        bot.setTitleService(titleService);
        bot.setUserService(userService);
        bot.setTournamentService(tournamentService);
        bot.setSettingsService(settingsService);
        bot.start();
    }

	/**
     * The password used when executing admin commands on the server.
     *
     * @see #setAdminPass(String)
     */
    private String adminPass = "*****";
    
    /** Utility to convert incoming tells into calls to cmdXXX(). */
    private final CommandDispatcher cmd = new CommandDispatcher();
    {
        cmd.setTarget(this);
    }
    
    /** Used to send commands to the chess server. Such as qtell, tell, reserve-game, etc. */
    private final Commands command = new Commands();
    
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
    
    /**
     * The {@link UsclSettingsService} service saves bot settings such as the library in which to save completed games.
     */
    private UsclSettingsService settingsService;
    
    /** Users with the manager role can talk to the bot. */
    private Role managerRole;
    
    /** Users with the programmer role receive extra debugging information from the bot. */
    private Role programmerRole;
    
    /** Module which assists speedtrap by tracking task-switch notifications during tournament games */
    private TaskSwitchTracker taskSwitchTracker = new TaskSwitchTracker();
    
    /** The user name assigned by the chess server upon login. e.g. guest233. */
    private String userName;

    /**
     * Sends an important (non-routine) tell to all managers to alert them to something requiring their attention.
     *
     * To gain the attention of the manager, this method will use both a tell and an atell.
     */
    public void alertManagers(String msg, Object... args) {
        broadcast(ChatType.PERSONAL_TELL, managerRole, "!!!!!!!!! IMPORTANT ALERT !!!!!!!!!");
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
     *            The message to send. It may optionally use {@link Formatter} style formatting.
     * @param args
     *            The values inserted into {@link Formatter} %s style place holders in the message.
     */
    public void broadcast(ChatType tellType, Collection<User> users, String msg, Object... args) {
        if (args.length > 0) {
            msg = MessageFormat.format(msg, args);
        }
        if (tellType.requiresAdmin) {
            command.sendQuietly("admin {0}", adminPass);
        }
        for (User user : users) {
            command.sendQuietly("{0} {1} {2}", tellType.command, user, msg);
        }
        if (tellType.requiresAdmin) {
            command.sendQuietly("admin");
        }
    }

    /**
     * Sends a message to all users with the given role/group.
     *
     * @param tellType
     *            The type of tell to use: "tell", "qtell", "message", etc.
     * @param role
     *            The role/group who's users will receive the message.
     * @param msg
     *            The message to send. It may optionally use {@link Formatter} style formatting.
     * @param args
     *            The values inserted into {@link Formatter} %s style place holders in the message.
     */
    public void broadcast(ChatType tellType, Role role, String msg, Object... args) {
        Set<User> users = userService.findUsersInRole(role);
        broadcast(tellType, users, msg, args);
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

    public void cmdCreateScript(User teller, int event, int board, Player player1, Player player2, StringBuffer timeControl) throws FileNotFoundException {
        //String template = ClassloaderHelper.readResource(USCLBot.class, "script.txt", TextCodec.UTF8);
        Integer r1 = player1.ratings().get(USCL_RATING);
        Integer r2 = player2.ratings().get(USCL_RATING);
        command.sendQuietly("qtell {0}  reserve-game {1} {2}", teller, player1, board);
        command.sendQuietly("qtell {0}  reserve-game {1} {2}", teller, player2, board);
        command.sendQuietly("qtell {0}  spoof {1} set open 1", teller, player1);
        command.sendQuietly("qtell {0}  spoof {1} set open 1", teller, player2);
        command.sendQuietly("qtell {0}  spoof {1} match {2} u w0 white {3}", teller, player1, player2, timeControl);
        command.sendQuietly("qtell {0}  spoof {1} accept {2}", teller, player2, player1);
        command.sendQuietly("qtell {0}  spoof jimmys qset {1} isolated 1", teller, player1);
        command.sendQuietly("qtell {0}  spoof jimmys qset {1} isolated 1", teller, player2);
        command.sendQuietly("qtell {0}  spoof {1} set examine 1", teller, player1);
        command.sendQuietly("qtell {0}  spoof {1} set examine 1", teller, player2);
        command.sendQuietly("qtell {0}  spoof {1} set kib 0", teller, player1);
        command.sendQuietly("qtell {0}  spoof {1} set kib 0", teller, player2);
        command.sendQuietly("qtell {0}  spoof {1} set allowkib 0", teller, player1);
        command.sendQuietly("qtell {0}  spoof {1} set allowkib 0", teller, player2);
        command.sendQuietly("qtell {0}  spoof {1} set quietplay 2", teller, player1);
        command.sendQuietly("qtell {0}  spoof {1} set quietplay 2", teller, player2);
        command.sendQuietly("qtell {0}  spoof {1} set busy 2", teller, player1);
        command.sendQuietly("qtell {0}  spoof {1} set busy 2", teller, player2);        
        command.sendQuietly("qtell {0}  observe {1}", teller, board);
        command.sendQuietly("qtell {0}  spoof roboadmin observe {1}", teller, board);
        command.sendQuietly("qtell {0}  qadd {1} 5 LIVE {2}({3}) - {4}({5}) || observe {6}", teller, event,
            player1.getTitledHandle(), r1, player2.getTitledHandle(), r2, board);
        /*
        PrintWriter out = null;
        try {
            out = new PrintWriter("data/sched.txt");
            String line = MessageFormat.format("{0} -> {1}({2,0}) vs {3}({4,0})", board, player1.getTitledRealName(), player1.getRatingText(USCL_RATING), player2.getTitledRealName(), player2.getRatingText(USCL_RATING));
        out.println(line);

        } finally {
        FileHelper.closeQuietly(out);
        }
        */
    }

    /**
     * Commands the bot to announce live games between two teams to the given channel.
     *
     * Syntax: <tt>announce-match NYC STL 129</tt>
     *
     * @param teller
     *            The user/manager issuing the command.
     * @param team1
     *            The home team in the match.
     * @param team2
     *            The visiting team in the match.
     * @param channel
     *            The channel to send the announcement.
     */
    public void cmdAnnounceMatch(User teller, Team team1, Team team2, int channel) {
        Collection<Game> games = tournamentService.findMatchGames(team1, team2);
        Collection<Game> liveGames = new ArrayList<Game>();
        for (Game game : games) {
            if (game.status.isPlaying()) {
                liveGames.add(game);
            }
        }
        if (liveGames.isEmpty()) {
            command.tell(teller, "Error - There are no active games between the {0} and the {1}.", team1, team2);
            return;
        }
        command.tell(channel, "US Chess League - {0} vs {1}", team1.getRealName(), team2.getRealName());
        for (Game game : games) {
            String white = game.whitePlayer.getTitledRealName(USCL_RATING);
            String black = game.blackPlayer.getTitledRealName(USCL_RATING);
            if (game.status.isFinished()) {
                command.tell(channel, "  {0} vs {1} - \"{2}\"", white, black, game.status.getCode());
            } else {
                command.tell(channel, "  {0} vs {1} - \"observe {2}\"", white, black, game.boardNumber);
            }
        }
    }
    
    /**
     * Commands the bot to send sample commands once for each player.  The placeholder {0} should
     * be put into the command wherever the player name should go.
     * 
     * Syntax: <tt>do-all-players +ban {0}</tt>
     * Syntax: <tt>do-all-players g-invite uscl {0}</tt>
     * Syntax: <tt>do-all-players message {0} great season!</tt>
     * 
     * @param teller
     *            The user/manager issuing the command.
     */
    public void cmdDoAllPlayers(User teller, String command) {
        Collection<Player> players = tournamentService.findAllPlayers();
        for (Player p : players) {
            this.command.sendAdminCommand(command, p);
        }
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
     * Commands the bot to logout, quit, compile the latest version, and then restart itself.
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
            if (USCL_TITLES.contains(title)) {
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
        command.sendAdminCommand("rating {0} standard {1}", player, rating);
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
     * Deprecated. Use {@link #cmdScheduleGame()} instead.
     *
     * @deprecated
     */
    public void cmdReserveGame(User teller, String playerHandle, int boardNum) {
        command.qtell(teller, " ** Use: schedule-game <board> <white> <black>");
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
     * Syntax: <tt>schedule-game 5 Shirov-NYC DuckStorm-YVR</tt>
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
    public void cmdScheduleGame(User teller, int boardNum, Player white, Player black) throws IOException {
        tournamentService.scheduleGame(boardNum, white, black);
        tournamentService.flush();
        command.sendCommand("+notify {0}", white);
        command.sendCommand("+notify {0}", black);
        command.sendAdminCommand("spoof {0} +notify {1}", teller, white);
        command.sendAdminCommand("spoof {0} +notify {1}", teller, black);
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
            String handle = setting.toString();
            player.setHandle(handle);
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
        } else if (ComparisionHelper.anyEquals(var, "div", "division")) {
            team.setDivision(value);
        } else {
            command.tell(teller, "Unknown variable: " + var);
            return;
        }
        cmdShowTeam(teller, team);
        tournamentService.updateTeam(team);
        tournamentService.flush();
    }
    
    /**
     * An alias for "show-schedule".
     */
    @PermitAll
    public void cmdShow(User teller) {
        cmdShowSchedule(teller);
    }

    /**
     * Deprecated. Use {@link #cmdShowSchedule()} instead.
     *
     * @deprecated
     */
    public void cmdShowGames(User teller) {
        command.qtell(teller, " ** Use: show-schedule");
    }

    /**
     * Commands the bot to list the currently scheduled games.
     *
     * Syntax: <tt>show-schedule</tt>
     *
     * @param teller
     *            The user/manager issuing the command.
     */
    @PermitAll
    public void cmdShowSchedule(User teller) {
        command.sendQuietly("qtell {0} {1}", teller, "Current Schedule:\\n");
        Collection<Game> games = tournamentService.findAllGames();
        for (Game game : games) {
            int boardNum = game.boardNumber;
            String whiteStatus = (game.whitePlayer.isOnline()) ? "" : " ?";
            String whitePlayer = game.whitePlayer.getHandle();
            String blackStatus = (game.blackPlayer.isOnline()) ? "" : " ?";
            String blackPlayer = game.blackPlayer.getHandle();
            String gameStatus = game.getStatusString();
            String msg = String.format("Board %2d: %18s%2s %18s%2s     %s", boardNum, whitePlayer, whiteStatus, blackPlayer, blackStatus, gameStatus);
            command.sendQuietly("qtell {0}  {1}", teller, msg);
        }
        command.sendQuietly("qtell {0}", teller);
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
        msg.format("   %4s: %s\\n", "Div ", team.getDivision());
        msg.format(" Team Members:\\n");
        int indent = 0;
        for (Player player : team.getPlayers()) {
            int len = player.getHandle().length();
            if (len > indent) {
                indent = len;
            }
        }
        String fmt = "   %s\\n";
        for (Player player : team.getPlayers()) {
            msg.format(fmt, player);
        }
        command.qtell(teller, msg);
    }

    /**
     * Commands the bot to spoof takeback requests to both players in a game.
     * 
     * Syntax:  <tt>takeback <boardNum> <half-moves></tt><br/>
     * Example: <tt>takeback 10 1</tt><br/>
     * Example: <tt>takeback 10 2</tt><br/>
     * 
     * @param teller
     *            The user/manager issuing the command.
     * @param game
     *            The board where the game is played.
     * @param halfmoves
     *            The number of half-moves to takeback in the game. For
     *            example, "takeback 2" takes back both players most recent
     *            move, backing up the game by 1 full move.
     */
    public void cmdTakeBack(User teller, Game game, int halfmoves) {
        if (!game.status.isPlaying()) {
            command.tell(teller, "Unable to takeback.  {0} isn't currently active.", game);
            return;
        }
        int remaining = halfmoves;
        while(remaining >= 2) {
            command.spoof(game.whitePlayer, "takeback 2");
            command.spoof(game.blackPlayer, "takeback 2");
            remaining-=2;
        }
        if (remaining==1) {
            command.spoof(game.whitePlayer, "takeback");
            command.spoof(game.blackPlayer, "takeback");
        }
        command.tell(teller, "Taking back {0} moves in {1}.", halfmoves, game);
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
        throw new RuntimeException("This is a test.  Don''t worry.");
    }

	/**
	 * Commands the bot to set a bot setting variable. This is used to change
	 * any system settings, such as the LibList to append games.
	 * 
	 * Syntax: <tt>tset variable value</tt>
	 * 
	 * @param teller
	 *            The user/manager issuing the command.
	 */
	public void cmdTSet(User teller, String settingName,
			StringBuffer settingValue) {
		String val = settingValue.toString().trim();
		try {
			settingsService.setSettingAsString(settingName, val);
			command.qtell(teller, " {0} set to {1}.", settingName, val);
		} catch (ConversionException e) {
			replyError(teller, e);
		}
	}

	/**
	 * Commands the bot to list all changeable bot settings.
	 * 
	 * Syntax: <tt>tvar</tt>
	 * 
	 * @param teller
	 *            The user/manager issuing the command.
	 */
	public void cmdTVar(User teller) {
    	Properties vars = settingsService.listSettings();
    	int maxKeyLen = 0;
    	for (Object key : vars.keySet()) {
    		String varName = (String) key;
    		maxKeyLen = Math.max(maxKeyLen, varName.length());
    	}
    	String pattern = "   %-" + maxKeyLen + "s : %s\\n";
    	Formatter msg = new Formatter();
    	msg.format(" Bot Variables:\\n");
    	for (Map.Entry<Object, Object> entry : vars.entrySet()) {
    		msg.format(pattern, entry.getKey(), entry.getValue());
    	}
    	command.qtell(teller, msg);
    }

    /**
     * Commands the bot to show the online players at his notify.
     *
     * Syntax: <tt>who</tt>
     *
     * @param teller
     *            The users issuing the command.
     */
    @PermitAll
    public void cmdWho(User teller) {
        Formatter msg = new Formatter();
        Collection<Player> playersOnline = tournamentService.findOnlinePlayers();
        for (Player player : playersOnline) {
            msg.format("  %s \\n", player);
        }
        command.qtell(teller, msg);
    }

    /**
     * Deprecated. Use {@link #cmdCancelGame()} instead.
     *
     * @deprecated
     */
    public void cmdUnreserveGame(User teller, Player player) {
        command.qtell(teller, " ** Use: cancel-game <player>");
    }

    /**
     * Commands the bot to cancel a board reservation made previously.
     *
     * Syntax: <tt>cancel-game Shirov-NYC</tt>
     *
     * @param teller
     *            The user/manager issuing the command.
     * @param player
     *            Either of the players in the game.
     */
    public void cmdCancelGame(User teller, Player player) {
        Game game = tournamentService.cancelGame(player);
        tournamentService.flush();
        if (game == null) {
            command.tell(teller, "Sorry, player \"{0}\" was not associated wtih any boards.", player);
        } else {
            command.tell(teller, "Okay, game \"{0} - {1} {2}\" is no longer scheduled.", game.boardNumber, game.whitePlayer, game.blackPlayer);
            command.sendCommand("-notify {0}", game.whitePlayer);
            command.sendCommand("-notify {0}", game.blackPlayer);
            command.spoof("rdgmx", "-notify {0}", game.whitePlayer);
            command.spoof("rdgmx", "-notify {0}", game.blackPlayer);
        }
    }

    /**
     * Commands the bot to logout, quit, update to the latest software version, and then restart itself.
     *
     * For this to work, the bot must be started by a shell script which knows to recompile and update the bot upon receiving exit code 7.
     *
     * Syntax: <tt>UPDATE</tt>
     *
     * @param teller
     *            The user/manager issuing the command.
     */
    public void cmdUpgrade(User teller) {
        exit(7, "Deploying version update at the request of {0}.  I''ll be right back!", teller);
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
        } catch (SecurityException e) {
            command.tell(teller, "I don''t understand.  Are you sure you spelled the command correctly?");
        } catch (BaseException e) {
            //TODO: We need something better than BaseException to capture user friendly messages.
            String msg = e.getMessage();
            command.tell(teller, msg);
        } catch (Exception e) {
            reportException(teller, message, e);
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
        Collection<Player> players = tournamentService.findScheduledPlayers();
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
        Game game = tournamentService.findGame(gameNumber);
        if (game == null) {
            return;
        }
        if (!game.needsAnnounce) {
            return;
        }
        game.needsAnnounce = false;
        if (loggingIn) {
            return;
        }
        boolean resumed = (numHalfMoves != 0);
        String startOrResume = (!resumed) ? "Started" : "Resumed";
        tellEventChannels("{0} vs {1}: {2} on board {3}.  To watch, type or click: \"observe {3}\".", game.whitePlayer, game.blackPlayer,
                startOrResume, game.boardNumber);
        if (!resumed) {
            command.sshout("{0} vs {1}: {2} on board {3}.  To watch, type or click: \"observe {3}\".  Results will be announced in channel 129.",
                    game.whitePlayer, game.blackPlayer, startOrResume, game.boardNumber);
        }
        command.sendCommand("qset {0} isolated 1", game.whitePlayer);
        command.sendCommand("qset {0} isolated 1", game.blackPlayer);
        command.sendAdminCommand("+kmuzzle {0}", game.whitePlayer);
        command.sendAdminCommand("+kmuzzle {0}", game.blackPlayer);
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
        Game game = tournamentService.findGame(gameNumber);
        if (game == null) {
            return;
        }
        /* Subtract USCL-Bot itself */
        int observerCount = game.observerCountMax - 1;
        tellEventChannels("{0} vs {1}: {2}  ({3} observers)", game.whitePlayer, game.blackPlayer, descriptionString, observerCount);
        boolean adjourned = (descriptionString.indexOf("adjourn") >= 0);
        
        int eventNumber = (game.boardNumber + 380);
        if(game.boardNumber>4 && game.boardNumber<9) {  }
        
        if (adjourned) {
            tournamentService.updateGameStatus(game, GameState.ADJOURNED);
        } else if ("0-1".equals(scoreString)) {
            tournamentService.updateGameStatus(game, GameState.BLACK_WINS);
            command.sendCommand("xtell rdgmx qadd {0} 6 0-1 {1} {2}", eventNumber, game.whitePlayer, game.blackPlayer);
        } else if ("1-0".equals(scoreString)) {
            tournamentService.updateGameStatus(game, GameState.WHITE_WINS);
            command.sendCommand("xtell rdgmx qadd {0} 6 1-0 {1} {2}", eventNumber, game.whitePlayer, game.blackPlayer);
        } else if ("1/2-1/2".equals(scoreString)) {
            tournamentService.updateGameStatus(game, GameState.DRAW);
            command.sendCommand("xtell rdgmx qadd {0} 6 1/2 {1} {2}", eventNumber, game.whitePlayer, game.blackPlayer);
        } else if ("aborted".equals(scoreString)) {
            tournamentService.updateGameStatus(game, GameState.NOT_STARTED);
        } else {
            tournamentService.updateGameStatus(game, GameState.UNKNOWN);
            alertManagers("Error: unexpected game status \"{0}\": {1}", gameResultCode, scoreString);
        }
        if (!adjourned) {
            command.sendCommand("qset {0} isolated 0", game.whitePlayer);
            command.sendCommand("qset {0} isolated 0", game.blackPlayer);
            command.sendAdminCommand("-kmuzzle {0}", game.whitePlayer);
            command.sendAdminCommand("-kmuzzle {0}", game.blackPlayer);
        }
        tournamentService.flush();
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
        if (tellType != ChessclubConstants.REGULAR_TELL) {
            return;
        }
        String myName = userName;
        if (myName.equals(teller)) {
            return;
        }
        boolean jeeves = "Jeeves".equals(teller);
        if (jeeves) {
            tellManagers("{0} just executed command: {1}", teller, message);
            teller = "Ralph";
        } else {
        	onCommand(teller, message);
        }
    }

    /**
     * Handles incoming DG_NOTIFY_ARRIVED datagrams from the server.
     *
     * The server sends this datagram anytime a player on the bots notify list arrives on the server.
     */
    protected void processPlayerArrived(String name) {
        Player player = tournamentService.findPlayer(name);
        if (player == null) {
            alertManagers("Arriving player {0} is on my notify list, but I don''t have him in the tournament roster.", name);
            return;
        }
        player.setState(PlayerState.WAITING);
        Game game = tournamentService.findPlayerGame(player);
        if (game == null) return;
        if (game.status.isFinished()) return;
        if (loggingIn) {
        	command.sendCommand("observe {0}", name);
        } else {
            tellManagers("{0} has arrived.  Reserving game {1}.", name, game.boardNumber);
            command.sendCommand("tell 399 {0} has arrived.", player.getTitledHandle());
        }
        command.sendAdminCommand("spoof {0} tell JudgeBot nowin", game.whitePlayer);
        command.sendAdminCommand("spoof {0} tell JudgeBot nowin", game.blackPlayer);
        command.sendAdminCommand("set-other {0} kib 0", game.blackPlayer);
        command.sendAdminCommand("set-other {0} kib 0", game.whitePlayer);
        command.sendAdminCommand("reserve-game {0} {1}", game.whitePlayer, game.boardNumber);
        command.sendAdminCommand("reserve-game {0} {1}", game.blackPlayer, game.boardNumber);
        if (game.status.isAdjourned()) {
            command.sendAdminCommand("spoof {0} match {1}", game.whitePlayer, game.blackPlayer);
            command.sendAdminCommand("spoof {0} match {1}", game.blackPlayer, game.whitePlayer);
        }
    }

    /**
     * Handles incoming DG_NOTIFY_LEFT datagrams from the server.
     *
     * The server sends this datagram anytime a player on the bots notify list disconnects from the server.
     */
    protected void processPlayerDeparted(String name) {
        Player player = tournamentService.findPlayer(name);
        if (player == null) {
            alertManagers("Departing player {0} is on my notify list, but I don''t have him in the tournament roster.", name);
            return;
        }
        player.setState(PlayerState.OFFLINE);
        Game game = tournamentService.findPlayerGame(player);
        if (!game.status.isFinished()) {
            tellManagers("{0} departed", name);
            command.sendCommand("tell 399 {0} has departed.", name);
        }
    }

    /**
     * Handles incoming DG_PLAYERS_IN_MY_GAME datagrams from the server.
     *
     * The server sends this datagram anytime a player joins the game, as a player or as an observer. This enables the bot to learn which game was the
     * most popular (in terms of number of observers.
     */
    private void processPlayersInMyGame(int gameNumber, String playerHandle, PlayerState state, boolean seesKibitz) {
        Game game = tournamentService.findGame(gameNumber);
        if (game == null) {
            return;
        }
        switch (state) {
            case WAITING:
                game.observerCountCurrent--;
                break;
            case OBSERVING:
                game.observerCountCurrent++;
                if (game.observerCountMax < game.observerCountCurrent) {
                    game.observerCountMax = game.observerCountCurrent;
                }
            //Fall through...
            case PLAYING:
               // command.qChanPlus(playerHandle, CHANNEL_USCL);
            default:
        }
    }

    /**
     * Handles incoming DG_NOTIFY_STATE datagrams from the server.
     *
     * The server sends this datagram anytime a player on my notify list starts or stops playing, examining, or observing a game. If a player in the
     * tournament starts a game, we want to observe it.
     */
    public void processPlayerStateChange(String player, PlayerState state, int game) {
        Player p = tournamentService.findPlayer(player);
        if (p == null)
            return;
        p.setState(state);
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
        if (!isPlayedGame) {
            return;
        }
        Player whitePlayer = tournamentService.findPlayer(whiteName);
        Player blackPlayer = tournamentService.findPlayer(blackName);
        if (whitePlayer == null) return;
        if (blackPlayer == null) return;
        Game game = tournamentService.findGame(gameNumber);
        if (game == null) {
            alertManagers("A game has started on an unpredicted board: {0} - {1} {2}", gameNumber, whiteName, blackName);
            game = tournamentService.findPlayerGame(whitePlayer);
        }
        if (game == null) {
            alertManagers("I am totally confused.  I don't have a scheduled game for: {0} {1}", whiteName, blackName);
            return;
        }
        tournamentService.updateGameStatus(game, GameState.PLAYING);
        tournamentService.flush();
        game.needsAnnounce = true;
        game.whitePlayer = whitePlayer;
        game.blackPlayer = blackPlayer;
        game.observerCountCurrent = 0;
        command.spoof("ROBOadmin", "observe {0}", gameNumber);
        command.sendAdminCommand("spoof {0} set busy 2", whitePlayer);
        command.sendAdminCommand("spoof {0} set busy 2", blackPlayer);
        command.qsuggest("USCLTD", "observe {0}", gameNumber);
        command.sendAdminCommand("spoof {0} ;-notify USCL; +notify USCLTD", whitePlayer);
        command.sendAdminCommand("spoof {0} ;-notify USCL; +notify USCLTD", blackPlayer);
        /* Announcement will occur when the move list arrives, since we can then tell if it's a resumed game. */
    }

    /** Sends a qtell to all programmers. Typically this is used to send debugging information. */
    public void qtellProgrammers(String msg, Object... args) {
        broadcast(ChatType.PERSONAL_QTELL, programmerRole, msg, args);
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
    public void reportException(String teller, String message, Throwable t) {
        System.err.println("Error- " + teller + ": " + message);
        t.printStackTrace(System.err);
        StringWriter w = new StringWriter();
        PrintWriter p = new PrintWriter(w);
        t.printStackTrace(p);
        String msg = w.toString();
        msg = msg.replaceAll("\n", "\\\\n");
        broadcast(ChatType.PERSONAL_TELL, programmerRole, "Error- " + teller + ": " + message);
        broadcast(ChatType.PERSONAL_TELL, programmerRole, t.toString());
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
     * Injects the instance of {@link UsclSettingsService} to use. The {@link UsclSettingsService} service saves bot settings such as the
     * library in which to save completed games.
     */
    public void setSettingsService(UsclSettingsService settingsService) {
    	this.settingsService = settingsService;
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
        cmd.init();
        conn = new Connection(hostName, hostPort, loginName, loginPass);
        conn.addDatagramListener(conn, Datagram.DG_PERSONAL_TELL);
        conn.addDatagramListener(conn, Datagram.DG_NOTIFY_STATE);
        conn.addDatagramListener(conn, Datagram.DG_MY_GAME_RESULT);
        conn.addDatagramListener(conn, Datagram.DG_STARTED_OBSERVING);
        conn.addDatagramListener(conn, Datagram.DG_MOVE_LIST);
        conn.addDatagramListener(conn, Datagram.DG_SEND_MOVES);
        conn.addDatagramListener(conn, Datagram.DG_PLAYERS_IN_MY_GAME);
        conn.addDatagramListener(conn, Datagram.DG_GAME_MESSAGE);
        taskSwitchTracker.setUserService(userService);
        taskSwitchTracker.setTournamentService(tournamentService);
        taskSwitchTracker.setConnection(conn);
        taskSwitchTracker.setUSCLBot(this);
        taskSwitchTracker.start();
        conn.initiateConnect(hostName, hostPort);
    }

    /**
     * Sends tells to the event channels (129 and 399).
     */
    public void tellEventChannels(String msg, Object... args) {
        if (loggingIn)
            return;
        if (args.length > 0) {
            msg = MessageFormat.format(msg, args);
        }
        command.tell(CHANNEL_USCL, msg);
        command.tell(CHANNEL_EVENTS_GROUP, msg);
    }

    /**
     * Sends a routine tell to all managers. This is typically used to keep them informed of the progress of the tournament. The bot uses atells
     * rather than regular tells, as this makes it easy for the manager to distinguish between tells sent by players (who expect a reply) and routine
     * tells sent by the bot.
     */
    public void tellManagers(String msg, Object... args) {
        broadcast(ChatType.PERSONAL_ADMIN_TELL, managerRole, msg, args);
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

        public void qtell(String handle, String pattern, Object... args) {
            String qtell = MessageFormat.format(pattern, args);
            sendQuietly("qtell {0} {1}\\n", handle, qtell);
        }

        public void qtell(User user, Formatter qtell) {
            sendQuietly("qtell {0} {1}", user, qtell);
        }

        public void qtell(User user, String qtell) {
            sendQuietly("qtell {0} {1}\\n", user, qtell);
        }

        public void qtell(User user, String pattern, Object... args) {
            String qtell = MessageFormat.format(pattern, args);
            sendQuietly("qtell {0} {1}\\n", user, qtell);
        }
        
        public void qsuggest(User user, String pattern, Object... args) {
            if (loggingIn)
                return;
            String qtell = MessageFormat.format(pattern, args);
            sendQuietly("qsuggest {0} {1}\\n", user, qtell);
        }
        
        public void qsuggest(String handle, String pattern, Object... args) {
            if (loggingIn)
                return;
            String qtell = MessageFormat.format(pattern, args);
            sendQuietly("qsuggest {0} {1}\\n", handle, qtell);
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
            sendAdminCommand("spoof {0} {1}", handle, command);
        }

        public void spoof(User user, String command, Object... args) {
            if (args.length > 0) {
                command = MessageFormat.format(command, args);
            }
            sendAdminCommand("spoof {0} {1}", user, command);
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
         * Sends an admin tell to the user.  Admin tells should be used for routine
         * messages and tells reserved for important messages.  While this seems
         * backwards, it helps the user avoid confusing routine bot tells with personal
         * tells from others.
         */
        public void atell(String handle, String msg, Object... args) {
            if (args.length > 0) {
                msg = MessageFormat.format(msg, args);
            }
            sendAdminCommand("atell {0} {1}", handle, msg);
        }

        /**
         * Sends an admin tell to the user.  Admin tells should be used for routine
         * messages and tells reserved for important messages.  While this seems
         * backwards, it helps the user avoid confusing routine bot tells with personal
         * tells from others.
         */
        public void atell(User user, String msg, Object... args) {
            if (args.length > 0) {
                msg = MessageFormat.format(msg, args);
            }
            sendAdminCommand("atell {0} {1}", user, msg);
        }

        /**
         * Sends a tell to the channel.
         */
        public void tell(int channel, String msg, Object... args) {
            if (args.length > 0) {
                msg = MessageFormat.format(msg, args);
            }
            sendQuietly("tell {0} {1}", channel, msg);
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
