package org.chessworks.uscl.services;

import java.io.Flushable;
import java.io.IOException;
import java.util.Collection;
import java.util.Map;

import org.chessworks.uscl.model.Player;
import org.chessworks.uscl.model.Team;

public interface TournamentService extends Flushable {

	void clearSchedule();

	Player createPlayer(String handle) throws InvalidNameException;

	Player createPlayer(String handle, Team team) throws InvalidNameException;

	Team createTeam(String teamCode) throws InvalidNameException;

	Collection<Player> findAllPlayers();

	Collection<Team> findAllTeams();

	Player findOrCreatePlayer(String handle) throws InvalidNameException;

	Team findOrCreateTeam(String handle) throws InvalidNameException;

	Player findPlayer(String handle);

	Team findTeam(String teamCode);

	int getPlayerBoard(Player player);

	Map<Player, Integer> getPlayerBoardMap();

	void reserveBoard(Player player, int board);

	void schedule(Player white, Player black, int board);

	int unreserveBoard(Player player);

	/** Saves any unwritten data. */
	void flush();

}
