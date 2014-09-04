/**
 *
 */
package org.chessworks.uscl.converters;

import org.chessworks.bots.common.converters.AbstractConverter;
import org.chessworks.bots.common.converters.ConversionException;
import org.chessworks.uscl.model.Player;
import org.chessworks.uscl.services.TournamentService;

public class PlayerConverter extends AbstractConverter<Player> {

	private TournamentService service;

	public PlayerConverter() {
		super(Player.class);
	}

	@Override
	public Player getAsObject(String s) throws ConversionException {
		if (s == null)
			throw new ConversionException("Missing required input: <player>");
		Player result = service.findPlayer(s);
		if (result == null) {
			throw new ConversionException("Unknown player: %s", s);
		}
		return result;
	}

    public void setTournamentService(TournamentService service) {
		this.service = service;
	}

}
