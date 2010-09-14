package org.chessworks.uscl.model;

import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.chessworks.uscl.util.SimpleName;

public class User extends SimpleName {

	private final Set<Title> titles = new TitlesSet();
	private String realName;
	private Map<RatingCategory, Integer> ratings;

	public User(String handle) {
		super(handle);
	}

	public String getRealName() {
		return realName;
	}

	public void setRealName(String realName) {
		this.realName = realName;
	}

	public Map<RatingCategory, Integer> ratings() {
		return ratings;
	}

	public String getHandle() {
		return super.toString();
	}

	public Set<Title> getTitles() {
		return titles;
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
