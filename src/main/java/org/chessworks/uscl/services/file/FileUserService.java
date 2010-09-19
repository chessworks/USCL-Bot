package org.chessworks.uscl.services.file;

import static org.chessworks.common.javatools.io.IOHelper.UTF8;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Properties;
import java.util.Set;
import java.util.Map.Entry;

import org.chessworks.common.javatools.io.DirtyFileHelper;
import org.chessworks.uscl.USCLBot;
import org.chessworks.uscl.model.Title;
import org.chessworks.uscl.model.User;
import org.chessworks.uscl.services.DataStoreException;
import org.chessworks.uscl.services.InvalidNameException;
import org.chessworks.uscl.services.simple.SimpleTitleService;
import org.chessworks.uscl.services.simple.SimpleUserService;

public class FileUserService extends SimpleUserService {

	private static final File DEFAULT_USERS_FILE = new File("data/Users.txt");
	private SimpleTitleService titleService = new SimpleTitleService();

	public void setDataFile(File file) {
		this.usersIO.setFile(file);
	}

	public void setDataFile(String fileName) {
		this.usersIO.setFile(fileName);
	}

	public void load() {
		try {
			usersIO.readText();
		} catch (IOException e) {
			throw new DataStoreException("Error saving changes to disk.", e);
		}
	}

	public void save() {
		try {
			usersIO.writeText();
		} catch (IOException e) {
			throw new DataStoreException("Error saving changes to disk.", e);
		}
	}

	private DirtyFileHelper usersIO = new DirtyFileHelper(DEFAULT_USERS_FILE, UTF8) {

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
				String ratingStr = data.getProperty(prefix + ".rating");
				String title = data.getProperty(prefix + ".titles");
				int rating = (ratingStr==null) ? -1 : Integer.parseInt(ratingStr);
				User u = findUser(handle);
				u.setRealName(realName);
				u.ratings().put(USCLBot.USCL_RATING, rating);
				if (title != null) {
					Set<Title> titles = titleService.lookupAll(title);
					u.getTitles().addAll(titles);
				}
			}
		}

		@Override
		public void doWrite(PrintWriter out) throws IOException {
			out.println("#USCL Players");
			out.println();
			for (User player : findAllKnownUsers()) {
				String handle = player.getHandle();
				String name = player.getRealName();
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
