package org.chessworks.chess.services.simple;

import org.chessworks.chess.model.Title;
import org.chessworks.chess.services.TitleService;

public class SimpleTitleService extends SimpleNameLookupService<Title> implements TitleService {

	public static final Title WGM = new Title("WGM", "Woman Grandmaster");
	public static final Title WIM = new Title("WIM", "Woman International Master");
	public static final Title WFM = new Title("WFM", "Woman FIDE Master");
	public static final Title WCM = new Title("WCM", "Woman Candidate Master");
	public static final Title GM = new Title("GM", "Grandmaster");
	public static final Title IM = new Title("IM", "International Master");
	public static final Title FM = new Title("FM", "FIDE Master");
	public static final Title CM = new Title("CM", "Candidate Master");
	public static final Title SM = new Title("SM", "Senior Master");
	public static final Title NM = new Title("NM", "National Master");

	public SimpleTitleService() {
		register(GM);
		register(IM);
		register(FM);
		register(SM);
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
