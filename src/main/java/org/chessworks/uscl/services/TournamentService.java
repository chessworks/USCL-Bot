package org.chessworks.uscl.services;

import java.util.Set;

import org.chessworks.uscl.model.Player;
import org.chessworks.uscl.model.Team;

public interface TournamentService {

	public abstract Player findPlayer(String handle);

	public abstract Team findTeam(String teamCode);

	public abstract Set<Player> findAllPlayers();

	public abstract Set<Team> findAllTeams();

	public abstract Player createPlayer(String handle) throws InvalidNameException;

	public abstract Player createPlayer(String handle, Team team) throws InvalidNameException;

	public abstract Team createTeam(String teamCode) throws InvalidNameException;

}