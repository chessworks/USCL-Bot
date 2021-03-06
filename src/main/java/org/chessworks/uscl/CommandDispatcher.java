package org.chessworks.uscl;

import org.chessworks.uscl.converters.GameConverter;
import org.chessworks.uscl.converters.PlayerConverter;
import org.chessworks.uscl.converters.TeamConverter;
import org.chessworks.uscl.services.TournamentService;

public class CommandDispatcher extends org.chessworks.bots.common.CommandDispatcher {

	/* The fields must be listed prior to the argConverter fields. This ensures these values have been set in time. */
	PlayerConverter playerConverter = new PlayerConverter();
    TeamConverter teamConverter = new TeamConverter();
    GameConverter gameConverter = new GameConverter();

	public CommandDispatcher() {
		argConverters.register(playerConverter);
		argConverters.register(teamConverter);
		argConverters.register(gameConverter);
		tellerConverters.register(playerConverter);
	}

	public void setTournamentService(TournamentService service) {
		playerConverter.setTournamentService(service);
		teamConverter.setTournamentService(service);
		gameConverter.setTournamentService(service);
	}

}
