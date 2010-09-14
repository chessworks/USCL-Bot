package org.chessworks.uscl.util;

import java.util.Comparator;

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

	private final String name;

	public SimpleName(String name) {
		if (this.name == null) {
			String type = this.getClass().getSimpleName();
			throw new NullPointerException(type + " may not have a null name.");
		}
		this.name = name;
	}

	public int compareTo(String s) {
		int result = name.compareTo(s);
		return result;
	}

	@Override
	public int compareTo(SimpleName o) {
		int result = name.compareTo(o.name);
		return result;
	}

	public int compareToWithCase(String s) {
		int result = name.compareToIgnoreCase(s);
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

	public boolean equalsToWithCase(Object obj) {
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
		if (!this.name.equals(other.name)) {
			return false;
		}
		return true;
	}

}
