package org.chessworks.uscl.services.simple;

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

	public Title lookup(String title) {
		Title t = super.lookup(title);
		if (t == null) {
			title = title.toUpperCase();
			t = new Title(title);
			register(t);
		}
		return t;
	}

}