package org.chessworks.chess.model;

import java.util.Comparator;

import org.chessworks.chess.services.InvalidNameException;

public class SimpleName implements Comparable<SimpleName> {

	public static final Comparator<SimpleName> CASE_INSENSITIVE_ORDER = new Comparator<SimpleName>() {
		@Override
		public int compare(SimpleName o1, SimpleName o2) {
			int result = o1.compareTo(o2);
			return result;
		}
	};

	public static final Comparator<SimpleName> CASE_SENSITIVE_ORDER = new Comparator<SimpleName>() {
		@Override
		public int compare(SimpleName o1, SimpleName o2) {
			int result = o1.compareToWithCase(o2);
			return result;
		}
	};

	private String name;

	public SimpleName(String name) {
		if (name == null) {
			String type = this.getClass().getSimpleName();
			throw new NullPointerException(type + ".simpleName");
		}
		this.name = name;
	}

	public String getSimpleName() {
		return name;
	}

	public void setSimpleName(String name) throws InvalidNameException {
		if (this.name.equalsIgnoreCase(name)) {
			this.name = name;
		} else {
			throw new InvalidNameException("Name changes are restricted to capitalization changes only.");
		}
	}

	@Override
	public int compareTo(SimpleName o) {
		int result = name.compareToIgnoreCase(o.name);
		return result;
	}

	public int compareToWithCase(SimpleName o) {
		int result = name.compareTo(o.name);
		return result;
	}

	@Override
	public String toString() {
		return name;
	}

	@Override
	public int hashCode() {
		return name.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		SimpleName other = (SimpleName) obj;
		if (!this.name.equalsIgnoreCase(other.name)) {
			return false;
		}
		return true;
	}

	public final boolean equalsWithCase(Object obj) {
		if (!equals(obj))
			return false;
		SimpleName other = (SimpleName) obj;
		if (!this.name.equals(other.name))
			return false;
		return true;
	}

}
