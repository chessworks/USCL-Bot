/**
 *
 */
package org.chessworks.uscl.converters;

import org.chessworks.bots.common.converters.AbstractConverter;
import org.chessworks.bots.common.converters.ConversionException;
import org.chessworks.uscl.model.Game;
import org.chessworks.uscl.services.TournamentService;

public class GameConverter extends AbstractConverter<Game> {

	private TournamentService service;

	public GameConverter() {
		super(Game.class);
	}

	@Override
	public Game getAsObject(String s) throws ConversionException {
		if (s == null)
			throw new ConversionException("Missing required input: <boardNum>");
		try {
    		int boardNum = Integer.parseInt(s);
    		Game result = service.findGame(boardNum);
    		if (result == null) {
    			throw new ConversionException("Unknown board number: %s", s);
    		}
    		return result;
		} catch (NumberFormatException e) {
            throw new ConversionException(e, "Invalid board number: %s", s);
		}
	}

	public void setTournamentService(TournamentService service) {
		this.service = service;
	}

}
