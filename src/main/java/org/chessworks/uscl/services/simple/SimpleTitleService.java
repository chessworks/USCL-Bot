package org.chessworks.uscl.services.simple;

import java.util.Set;
import java.util.TreeSet;

import org.chessworks.uscl.model.Title;
import org.chessworks.uscl.util.SimpleNameLookupService;

public class SimpleTitleService extends SimpleNameLookupService<Title> {

	public static final Title GM = new Title("GM", "Grandmaster");
	public static final Title IM = new Title("IM", "International Master");
	public static final Title FM = new Title("FM", "FIDE Master");
	public static final Title NM = new Title("NM", "National Master");

	public SimpleTitleService() {
		register(GM);
		register(IM);
		register(FM);
		register(NM);
	}

	public Title lookupOrRegister(String title) {
		Title t = lookup(title);
		if (t == null) {
			t = new Title(title);
			register(t);
		}
		return t;
	}

	public Set<Title> lookupAll(String... titles) {
		Set<Title> titleSet = new TreeSet<Title>();
		for (String s : titles) {
			Title t = lookupOrRegister(s);
			titleSet.add(t);
		}
		return titleSet;
	}

	public Set<Title> lookupAll(String titleList) {
		if (includesBrackets(titleList)) {
			int len = titleList.length();
			titleList = titleList.substring(1, len - 1);
		}
		String[] titles = titleList.split(",?  *");
		Set<Title> titleSet = lookupAll(titles);
		return titleSet;
	}

	private static final boolean includesBrackets(String titleString) {
		int len = titleString.length();
		if (len == 0) return false;
		int first = titleString.charAt(0);
		int last = titleString.charAt(len - 1);
		if (first == '(' && last == ')')
			return true;
		if (first == '{' && last == '}')
			return true;
		if (first == '[' && last == ']')
			return true;
		return false;
	}

}
