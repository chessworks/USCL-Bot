package org.chessworks.uscl.services;

import java.io.Flushable;
import java.util.Collection;
import java.util.Map;

import org.chessworks.uscl.model.Player;
import org.chessworks.uscl.model.Team;

public interface TournamentService extends Flushable {

	void clearSchedule();

	Player createPlayer(String handle) throws InvalidPlayerException, InvalidTeamException;

	Player createPlayer(String handle, Team team) throws InvalidPlayerException;

	Team createTeam(String teamCode) throws InvalidTeamException;

	boolean removePlayer(Player player);

	int removeTeam(Team team);

	Collection<Player> findAllPlayers();

	Collection<Team> findAllTeams();

	Player findOrCreatePlayer(String handle) throws InvalidPlayerException, InvalidTeamException;

	Team findOrCreateTeam(String handle) throws InvalidTeamException;

	Player findPlayer(String handle);

	Team findTeam(String teamCode);

	int getPlayerBoard(Player player);

	Map<Player, Integer> getPlayerBoardMap();

	void reserveBoard(Player player, int board);

	Player reserveBoard(String playerName, int board, boolean allowNewPlayer) throws InvalidPlayerException, InvalidTeamException;

	void schedule(Player white, Player black, int board);

	int unreserveBoard(Player player);

	/** Saves any unwritten data. */
	void flush();

}
