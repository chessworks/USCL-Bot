package org.chessworks.uscl;

import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.Formatter;
import java.util.Properties;
import java.util.Set;

import org.chessworks.common.javatools.io.FileHelper;
import org.chessworks.uscl.converters.ConversionException;
import org.chessworks.uscl.converters.Converter;
import org.chessworks.uscl.model.Role;
import org.chessworks.uscl.model.User;
import org.chessworks.uscl.services.InvalidNameException;
import org.chessworks.uscl.services.UserService;
import org.chessworks.uscl.services.file.FileUserService;

import free.chessclub.ChessclubConstants;
import free.chessclub.level2.Datagram;
import free.chessclub.level2.DatagramEvent;
import free.chessclub.level2.DatagramListener;

/**
 * Welcome to SimpleBot, an example program to illustrate writing bots for ICC.
 *
 * This example supports 4 commands: echo, greet, add, and spoof. For example:
 * <ul>
 * <li> <tt>tell simplebot echo Hello there!</tt><br/>
 * This echo's the command back to the sender.</li>
 * <li> <tt>tell simplebot greet DuckStorm</tt><br/>
 * This tells the bot to send "Hello, DuckStorm" to user DuckStorm.</li>
 * <li> <tt>tell simplebot add 2 10</tt><br/>
 * This tells the bot to reply with the sum of 2 and 10.
 * <li> <tt>tell simplebot spoof shout Testing!</tt><br/>
 * This tells the bot to execute an arbitrary command, in this case a shout.</li>
 * </ul>
 *
 * Commands sent to the bot will result in calling a cmdXXX() method. For example, sending the echo command results in a call to the
 * {@link #cmdEcho(User, StringBuffer) cmdEcho()} method. The other command handlers are similarly {@link #cmdGreet(User, User) cmdGreet()},
 * {@link #cmdAdd(User, int, int) cmdAdd()}, and {@link #cmdSpoof(User, StringBuffer) cmdSpoof()}.
 *
 * When writing your own bot, this is the first place you'll start... creating your own commands. Every command sent to the bot will result in calling
 * a cmdXXX() method. You can extend the behavior of the bot simply by adding your own cmdXXX() methods.
 *
 * Try looking at the source code for cmdAdd() now. It's only 2 lines. Use this as an example to create a cmdMultiply() method. Then test out the new
 * multiply command by running the bot.it out. Adding new commands is really just that easy.
 *
 * Later, you'll probably get curious how those cmdXXX methods actually get called. Most of the hard work is hidden by the framework. However here is
 * a quick glimps at the process:
 * <ol>
 * <li>A user on the server sends <tt>tell SimpleBot echo This is a test.</tt></li>
 * <li>The server sends the tell as a Level2 datagram, as specified in {@link ftp://ftp.chessclub.com/pub/icc/formats/formats.txt}.</li>
 * <li>The connection library (taken from Jin) reads the datagram and delivers it to {@link SimpleBot.Connection#datagramReceived(DatagramEvent)}.
 * <li>datagramReceived() recognizes the DG_PERSONAL_TELL datagram and dispatches it to
 * {@link SimpleBot#processPersonalTell(String, String, String, int) processPersonalTell(teller, titles, message, tellType)}.</li>
 * <li>processPersonalTell() does a few checks for special cases (such as chat from other bots) and then uses the {@link CommandDispatcher} library to
 * call the corresponding cmdXXXX() method.
 * <li>The CommandDispatcher then examines the tell and attempts to match the first word to the XXXX part of a cmdXXXX method. Dashes and
 * capitalization changes are ignored when matching. Once the proper cmdXXXX method is found, the CommandDispatcher examines the input parameter types
 * and attempts to convert the rest of the tell to the required data types, using various {@link Converter Converters}. For example,
 * {@link #cmdAdd(User, int, int)} takes 2 numbers as input (the first parameter is always the user sending the command). The command dispatcher
 * ensures that incoming text is first converted to an int. If that process fails (perhaps the text wasn't a number), the user will receive an
 * appropriate error message. It then dispatches to the cmdXXXX method using the {@link java.lang.reflect Java Reflection API}. If you don't want to
 * use Converters, you can also use parameter types String (to capture a single word) or StringBuffer (to capture text through to the end of the
 * line). But for now just know this part of the framework dispatches to the corresponding cmdXXXX() method and save the magic for later.</li>
 * <li>Finally cmdXXXX() is where you come in, adding the logic to handle the user's command.</li>
 *
 * Writing bots gets fancier when you start wanting information from the server other than tells. You'll need to locate the corresponding Level2
 * datagram in {@link ftp://ftp.chessclub.com/pub/icc/formats/formats.txt formats.txt} and then modify
 * {@link SimpleBot.Connection#datagramReceived(DatagramEvent)} to handle the new datagram type. The {@link USCLBot USCL-Bot} uses a number of
 * different datagrams to be notified when players arrive/depart, when games begin/end, and even when players start observing a given game. When you
 * get to this stage in your learning, examine both start() and datagramReceived() in SimpleBot, and then compare it to the corresponding versions in
 * USCLBot. And for now, don't worry about all this... practice adding a few new commands first Maybe by then I'll have improved the framework so you
 * don't even need to concern yourself with datagrams ever.
 *
 * @author Doug Bateman
 */

//Welcome to SimpleBot, an example program to illustrate writing bots for ICC.
//
// This example supports 4 commands: echo, greet, add, and spoof. For example:
// + tell simplebot echo Hello there!
//   This echo's the command back to the sender.
// + tell simplebot greet DuckStorm
//   This tells the bot to send "Hello, DuckStorm" to user DuckStorm.
// + tell simplebot add 2 10
//   This tells the bot to reply with the sum of 2 and 10.
// + tell simplebot spoof shout Testing!
//   This tells the bot to execute an arbitrary command, in this case a shout.
//
// Commands sent to the bot will result in calling a cmdXXX() method. For example, sending the
// echo command results in a call to the cmdEcho() method. The other command handlers are
// similarly cmdGreet(), cmdAdd(), and cmdSpoof().
//
// When writing your own bot, this is the first place you'll start... creating your own commands.
// Every command sent to the bot will result in calling a cmdXXX() method. You can extend the
// behavior of the bot simply by adding your own cmdXXX() methods.
//
// Try looking at the source code for cmdAdd() now. It's only 2 lines. Use this as an example to
// create a cmdMultiply() method. Then test out the new multiply command by running the bot it out.
// Adding new commands is really just that easy.
//
// Later, you'll probably get curious how those cmdXXX methods actually get called. Most of the hard
// work is hidden by the framework. However here is a quick glimps at the process:
//
// 1. A user on the server sends tell SimpleBot echo This is a test.
//
// 2. The server sends the tell as a Level2 datagram, as specified in
//    ftp://ftp.chessclub.com/pub/icc/formats/formats.txt .
//
// 3. The connection library (taken from Jin) reads the datagram and delivers it to
//    Connection.datagramReceived(DatagramEvent).
//
// 4. datagramReceived() recognizes the DG_PERSONAL_TELL datagram and dispatches it to
//    processPersonalTell(teller, titles, message, tellType).
//
// 5. processPersonalTell() does a few checks for special cases (such as chat from other bots) and
//    then uses the CommandDispatcher library to call the corresponding cmdXXXX() method.
//
// 6. The CommandDispatcher then examines the tell and attempts to match the first word to the
//    XXXX part of a cmdXXXX method. Dashes and capitalization changes are ignored when matching.
//    Once the proper cmdXXXX method is found, the CommandDispatcher examines the input parameter
//    types and attempts to convert the rest of the tell to the required data types, using various
//    converters.
//
//    For example, cmdAdd(User, int, int) takes 2 numbers as input (the first parameter is always
//    the user sending the command). The command dispatcher ensures that incoming text is first
//    converted to an int.  If that process fails (perhaps the text wasn't a number), the user
//    will receive an appropriate error message. It then dispatches to the cmdXXXX method using
//    the Java Reflection API.
//
//    If you don't want the dispatcher to automatically convert data types for you, you can also use
//    String (to capture a single word) or StringBuffer (to capture text through to the end of
//    the line). But for now just know this part of the framework dispatches to the corresponding
//    cmdXXXX() method and save the magic for later.
//
// 7. Finally, cmdXXXX() is where you come in, adding the logic to handle the user's command.
//
// Writing bots gets fancier when you start wanting information from the server other than tells.
// You'll need to locate the corresponding Level2 datagram in
// ftp://ftp.chessclub.com/pub/icc/formats/formats.txt and then modify datagramReceived() to handle
// the new datagram type. USCLBot uses a number of different datagrams to be notified when players
// arrive/depart, when games begin/end, and even when players start observing a given game.
// When you get to this stage in your learning, examine both start() and datagramReceived() in
// SimpleBot, and then compare it to the corresponding versions in USCLBot. And for now, don't
// worry about all this... practice adding a few new commands first. Maybe by then I'll have improved
// the framework so you don't even need to concern yourself with datagrams ever. :-).

public class SimpleBot {

	/**
	 * Commands the bot to reply with the result of adding two numbers together.
	 *
	 * Syntax: <tt>add 10 5</tt>
	 *
	 * @param teller
	 *            The user issuing the command.
	 * @param num1
	 *            The first number to add.
	 * @param num2
	 *            The second nubmer to add.
	 */
	public void cmdAdd(User teller, int num1, int num2) {
		int sum = num1 + num2;
		command.tell(teller, "The sum is: {0}.", sum);
	}

	/**
	 * Commands the bot to echo the text back to the teller.
	 *
	 * Syntax: <tt>echo HELLO, Hello, hello, ello, o, o, o....</tt>
	 *
	 * @param teller
	 *            The user issuing the command.
	 * @param text
	 *            The text to echo back to the sender.
	 */
	public void cmdEcho(User teller, StringBuffer text) {
		/* Note: When the command dispatcher sees a StringBuffer parameter, it will pass the entire remaining line of text. */
		command.tell(teller, text.toString());
	}

	/**
	 * Commands the bot to send a hello greeting to the specified user.
	 *
	 * Syntax: <tt>greet RdgMx</tt>
	 *
	 * @param teller
	 *            The user issuing the command.
	 * @param greetMe
	 *            The person to whom the bot will send the greeting.
	 */
	public void cmdGreet(User teller, User greetMe) {
		command.tell(greetMe, "Hello, {1}.", greetMe.getTitledHandle());
	}

	/**
	 * Instructs the bot to issue the attached command to the server.
	 *
	 * Syntax: <tt>spoof tell RdgMx Hello.  I am SimpleBot 2000, at your service.</tt>
	 *
	 * @param teller
	 *            The user issuing the command.
	 * @param cmd
	 *            The command to have the bot send to the chess server.
	 */
	public void cmdSpoof(User teller, StringBuffer cmd) {
		/* Note: When the command dispatcher sees a StringBuffer parameter, it will pass the entire remaining line of text. */
		if (userService.isUserInRole(teller, managerRole)) {
			command.sendCommand(cmd.toString());
		} else {
			command.tell(teller, "I'm sorry, but this is a restricted command.");
		}
	}

	/**
	 * Shuts down the bot.
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
	 * The "test-error" command is used to test the bot's graceful response to an unexpected internal error.
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

	/** Shuts down the bot with the given exit code, after sending this good-bye message. */
	public void exit(int code, String msg, Object... args) {
		if (conn.isConnected()) {
			tellManagers(msg, args);
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
			}
		}
		userService.flush();
		System.exit(code);
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
	 * Sends a routine tell to all managers. This is typically used to keep them informed of the progress of the tournament. The bot uses atells
	 * rather than regular tells, as this makes it easy for the manager to distinguish between tells sent by players (who expect a reply) and routine
	 * tells sent by the bot.
	 */
	public void tellManagers(String msg, Object... args) {
		Collection<User> managerList = userService.findUsersInRole(managerRole);
		broadcastAsAdmin("atell", managerList, msg, args);
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
		System.out.println();
	}

	public void onDisconnected() {
		exit(1, "Disconnected.");
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
		}
		onCommand(teller, message);
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
	 * Sets the user name used during login, such as "SimpleBot" or "guest". The default value is "SimpleBot". This is should only be set prior to
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
		System.out.println("Starting SimpleBot...");
		System.out.println();
		conn = new Connection(hostName, hostPort, loginName, loginPass);
		conn.addDatagramListener(conn, Datagram.DG_PERSONAL_TELL);
		conn.initiateConnect(hostName, hostPort);
	}

	/** Sends commands to the ICC server, such as qtell, tell, reserve-game, etc. */
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
			case Datagram.DG_PERSONAL_TELL: {
				String teller = datagram.getString(0);
				String titles = datagram.getString(1);
				String message = datagram.getString(2);
				int tellType = datagram.getInteger(3);
				processPersonalTell(teller, titles, message, tellType);
				break;
			}
			default: {
				//ignore other datagrams
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
	 * The user name used during login, such as guest.
	 *
	 * @see #setLoginName(String)
	 */
	private String loginName = "SimpleBot";

	/** The user name assigned by the chess server upon login. e.g. guest233. */
	private String userName;

	/**
	 * The password used during login.
	 *
	 * @see #setLoginPass(String)
	 */
	private String loginPass = "*****";

	/**
	 * The password used when executing admin commands on the server.
	 *
	 * @see #setAdminPass(String)
	 */
	private String adminPass = "*****";

	/**
	 * UserService stores the list of managers & programmers authorized to use this bot.
	 */
	private UserService userService;

	/** Users with the manager role can talk to the bot. */
	private Role managerRole;

	/** Users with the programmer role receive extra debugging information from the bot. */
	private Role programmerRole;

	/** Utility to convert incoming tells into calls to cmdXXX(). */
	private CommandDispatcher cmd = new CommandDispatcher(this);

	/** Used to send commands to the chess server. Such as qtell, tell, reserve-game, etc. */
	private Commands command = new Commands();

	/** The underlying connection to the server. Uses Jin's connection library. */
	private Connection conn;

	private static void loadConnectionSettings(Properties settings, SimpleBot bot) {
		String loginName = settings.getProperty("chessclub.loginName", "SimpleBot");
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
		SimpleBot bot = new SimpleBot();
		loadConnectionSettings(settings, bot);

		String managersFile = settings.getProperty("file.managers", "data/Managers.txt");
		FileUserService userService = new FileUserService();
		userService.setDataFile(managersFile);
		userService.load();
		bot.setUserService(userService);

		bot.start();
	}

	/**
	 * The path to the file on disk where the configured bot settings are located. The path defaults to "Reserve-Games.b2s", but can be changed using
	 * the command-line.
	 */
	public static final String BOARDS_FILE = "Games.txt";

	public static final String BOT_RELEASE_DATE = "December 30, 2010";

	public static final String BOT_RELEASE_NAME = "SimpleBot";

	public static final String BOT_RELEASE_NUMBER = "1.0.0";

	public static final PrintStream ECHO_STREAM = System.out;

	/**
	 * The path to the file on disk where the configured bot settings are located. The path defaults to "SimpleBot.properties", but can be changed by
	 * setting the "SimpleBot.settingsFile" system property on the command-line: "-SimpleBot.settingsFile=myFile.properties".
	 */
	public static final String SETTINGS_FILE = System.getProperty("SimpleBot.settingsFile", "SimpleBot.properties");

}
