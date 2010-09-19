package org.chessworks.uscl.services.file;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Properties;
import java.util.Set;
import java.util.Map.Entry;

import org.chessworks.uscl.USCLBot;
import org.chessworks.uscl.model.Role;
import org.chessworks.uscl.model.Title;
import org.chessworks.uscl.model.User;
import org.chessworks.uscl.services.InvalidNameException;
import org.chessworks.uscl.services.simple.SimpleTitleService;
import org.chessworks.uscl.services.simple.SimpleUserService;
import org.chessworks.uscl.util.IO;

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
				String title = data.getProperty(prefix + ".titles");
				String roleList = data.getProperty(prefix + ".roles", "");
				User u = FileUserService.super.findUser(handle);
				u.setRealName(realName);
				if (title != null) {
					Set<Title> titles = titleService.lookupAll(title);
					u.getTitles().addAll(titles);
				}
				String[] roleNames = roleList.split("[, ]+");
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
