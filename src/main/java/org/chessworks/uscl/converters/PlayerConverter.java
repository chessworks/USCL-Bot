/**
 *
 */
package org.chessworks.uscl.converters;

import org.chessworks.uscl.model.Player;
import org.chessworks.uscl.services.TournamentService;

public class PlayerConverter extends AbstractConverter<Player> {

	private TournamentService playerService;

	public PlayerConverter() {
		super(Player.class);
	}

	public PlayerConverter(Player nullValue) {
		super(Player.class, nullValue);
	}

	@Override
	public Player convert(String s) throws ConversionException {
		if (checkNull(s))
			return nullValue;
		Player u = playerService.findPlayer(s);
		return u;
	}

	public void setPlayerService(TournamentService playerService) {
		this.playerService = playerService;
	}

}
