package org.chessworks.uscl.services;

import java.util.Collection;
import java.util.Map;

import org.chessworks.uscl.model.Player;
import org.chessworks.uscl.model.Team;

public interface TournamentService {

	public void clearSchedule();

	public Player createPlayer(String handle) throws InvalidNameException;

	public Player createPlayer(String handle, Team team) throws InvalidNameException;

	public Team createTeam(String teamCode) throws InvalidNameException;

	public Collection<Player> findAllPlayers();

	public Collection<Team> findAllTeams();

	public Player findOrCreatePlayer(String handle) throws InvalidNameException;

	public Team findOrCreateTeam(String handle) throws InvalidNameException;

	public Player findPlayer(String handle);

	public Team findTeam(String teamCode);

	public int getPlayerBoard(Player player);

	public Map<Player, Integer> getPlayerBoardMap();

	public void reserveBoard(Player player, int board);

	public void schedule(Player white, Player black, int board);

	public int unreserveBoard(Player player);

}