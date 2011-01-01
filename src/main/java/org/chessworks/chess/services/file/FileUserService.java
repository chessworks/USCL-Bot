package org.chessworks.chess.services.file;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Properties;
import java.util.Set;
import java.util.Map.Entry;

import org.chessworks.chess.model.Role;
import org.chessworks.chess.model.Title;
import org.chessworks.chess.model.User;
import org.chessworks.chess.services.InvalidNameException;
import org.chessworks.chess.services.simple.SimpleTitleService;
import org.chessworks.chess.services.simple.SimpleUserService;
import org.chessworks.common.javatools.collections.CollectionHelper;
import org.chessworks.uscl.USCLBot;

public class FileUserService extends SimpleUserService {

	private static final File DEFAULT_USERS_FILE = new File("data/Users.txt");
	private final IO usersIO = new UsersIO();
	private SimpleTitleService titleService = new SimpleTitleService();

	public void setDataFile(File file) {
		this.usersIO.setFile(file);
	}

	public void setDataFile(String fileName) {
		this.usersIO.setFile(fileName);
	}

	/**
	 * {@inheritDoc}
	 *
	 * @see org.chessworks.chess.services.simple.SimpleUserService#addUserToRole(org.chessworks.chess.model.User, org.chessworks.chess.model.Role)
	 */
	@Override
	public void addUserToRole(User user, Role role) {
		super.addUserToRole(user, role);
		usersIO.setDirty();
	}

	/**
	 * {@inheritDoc}
	 *
	 * @see org.chessworks.chess.services.simple.SimpleUserService#register(org.chessworks.chess.model.User)
	 */
	@Override
	public void register(User user) throws InvalidNameException {
		super.register(user);
		usersIO.setDirty();
	}

	public void load() {
		super.reset();
		usersIO.load();
	}

	public void save() {
		usersIO.save();
	}

	public void flush() {
		save();
	}

	public void setTitleService(SimpleTitleService titleService) {
		this.titleService = titleService;
	}

	private final class UsersIO extends IO {

		public UsersIO() {
			super(DEFAULT_USERS_FILE, UTF8);
		}

		@Override
		public void doRead(BufferedReader in) throws IOException, InvalidNameException {
			Properties data = new Properties();
			data.load(in);
			for (Entry<Object, Object> entry : data.entrySet()) {
				String propName = (String) entry.getKey();
				String propValue = (String) entry.getValue();
				if (!propName.endsWith(".handle"))
					continue;
				String prefix = propName.substring(0,propName.length() - ".handle".length());
				String handle = propValue;
				String realName = data.getProperty(prefix + ".name");
				String titleStr = data.getProperty(prefix + ".titles");
				String roleList = data.getProperty(prefix + ".roles", "");
				User u = FileUserService.super.findUser(handle);
				u.setRealName(realName);
				String[] titleNames = CollectionHelper.split(titleStr);
				Set<Title> titleSet = u.getTitles();
				titleService.lookupAll(titleSet, titleNames);
				String[] roleNames = CollectionHelper.split(roleList);
				for (String s : roleNames) {
					Role r = FileUserService.super.findOrCreateRole(s);
					FileUserService.super.addUserToRole(u, r);
				}
				FileUserService.super.register(u);
			}
		}

		@Override
		public void doWrite(PrintWriter out) throws IOException {
			out.println("#USCL Players");
			out.println();
			for (User player : FileUserService.super.findAllKnownUsers()) {
				String handle = player.getHandle();
				String name = player.getRealName();
				//TODO: Fix saving roles, need a way to query for roles
				Integer rating = player.ratings().get(USCLBot.USCL_RATING);
				String title = player.getTitles().toString();
				out.format("player.%s.handle=%s%n", handle, handle);
				out.format("player.%s.name=%s%n", handle, name);
				if (rating != null) {
					out.format("player.%s.rating=%d%n", handle, rating);
				}
				out.format("player.%s.title=%s%n", handle, title);
				out.println();
			}
		}

	};

}
