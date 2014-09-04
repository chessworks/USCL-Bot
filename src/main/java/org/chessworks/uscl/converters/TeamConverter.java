/**
 *
 */
package org.chessworks.uscl.converters;

import org.chessworks.bots.common.converters.AbstractConverter;
import org.chessworks.bots.common.converters.ConversionException;
import org.chessworks.uscl.model.Team;
import org.chessworks.uscl.services.TournamentService;

public class TeamConverter extends AbstractConverter<Team> {

	private TournamentService service;

	public TeamConverter() {
		super(Team.class);
	}

	@Override
	public Team getAsObject(String s) throws ConversionException {
		if (s == null)
			throw new ConversionException("Missing required input: <team>");
		Team result = service.findTeam(s);
		if (result == null) {
			throw new ConversionException("Unknown team: %s", s);
		}
		return result;
	}

    public void setTournamentService(TournamentService service) {
		this.service = service;
	}

}
