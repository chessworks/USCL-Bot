package org.chessworks.uscl.model;

import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.chessworks.uscl.services.InvalidNameException;
import org.chessworks.uscl.util.SimpleName;

public class User extends SimpleName {

	private final Set<Title> titles = new TitlesSet();
	private final Map<RatingCategory, Integer> ratings = new TreeMap<RatingCategory, Integer>();
	private String realName;

	public User(String handle) {
		super(handle);
		this.realName = handle;
	}

	public String getRealName() {
		return realName;
	}

	public void setRealName(String realName) {
		if (realName == null) {
			this.realName = getHandle();
		} else if (realName.isEmpty()) {
			this.realName = getHandle();
		} else {
			this.realName = realName;
		}
	}

	public Map<RatingCategory, Integer> ratings() {
		return ratings;
	}

	public int getRating(RatingCategory category) {
		Integer r = ratings.get(category);
		if (r == null) {
			return -1;
		} else {
			return r.intValue();
		}
	}

	public void setRating(RatingCategory category, int rating) {
		if (rating == -1) {
			ratings.remove(category);
		} else {
			ratings.put(category, rating);
		}
	}

	public String getRatingText(RatingCategory category) {
		Integer r = ratings.get(category);
		if (r == null) {
			return "Unavailable";
		} else {
			return r.toString();
		}
	}

	public String getHandle() {
		return super.getSimpleName();
	}

	public void setHandle(String handle) throws InvalidNameException {
		super.setSimpleName(handle);
	}

	public Set<Title> getTitles() {
		return titles;
	}

	public String getTitledHandle() {
		String handle = getHandle();
		Set<Title> titles = getTitles();
		if (titles.isEmpty())
			return handle;
		else
			return titles + handle;
	}

	public String getTitledRealName() {
		String realName = getRealName();
		Set<Title> titles = getTitles();
		if (titles.isEmpty())
			return realName;
		else
			return titles + " " + realName;
	}

	private static class TitlesSet extends TreeSet<Title> {

		private static final long serialVersionUID = -8305367820345053217L;

		@Override
		public String toString() {
			if (this.size() == 0)
				return "";
			StringBuffer buf = new StringBuffer();
			buf.append('(');
			for (Title t : this) {
				buf.append(t);
				buf.append(' ');
			}
			int lastChar = buf.length() - 1;
			buf.setCharAt(lastChar, ')');
			return buf.toString();
		}
	}

}
