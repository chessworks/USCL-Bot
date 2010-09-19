package org.chessworks.uscl.services.simple;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

import org.chessworks.uscl.model.Player;
import org.chessworks.uscl.model.Team;
import org.chessworks.uscl.services.InvalidNameException;
import org.chessworks.uscl.services.TournamentService;

public class SimpleTournamentService implements TournamentService {

	public Map<Player, Integer> playerBoards = new LinkedHashMap<Player, Integer>();

	/** Map codes to teams */
	private final Map<String, Team> teams;

	/** A read-only wrapper for returning all teams. */
	private final Collection<Team> allTeams;

	/** Map handles to players */
	private final Map<String, Player> players;

	/** A read-only wrapper for returning all players. */
	private final Collection<Player> allPlayers;

	public SimpleTournamentService() {
		teams = new TreeMap<String, Team>();
		allTeams = Collections.unmodifiableCollection(teams.values());
		players = new TreeMap<String, Player>();
		allPlayers = Collections.unmodifiableCollection(players.values());
	}

	/**
	 * {@inheritDoc}
	 *
	 * @see org.chessworks.uscl.services.TournamentService#clearSchedule()
	 */
	public void clearSchedule() {
		playerBoards.clear();
	}

	/**
	 * {@inheritDoc}
	 *
	 * @see org.chessworks.uscl.services.TournamentService#schedule(Player, Player, int)
	 */
	public void schedule(Player white, Player black, int board) {
		reserveBoard(white, board);
		reserveBoard(black, board);
	}

	/**
	 * {@inheritDoc}
	 *
	 * @see org.chessworks.uscl.services.TournamentService#reserveBoard(Player, int)
	 */
	public void reserveBoard(Player player, int board) {
		unreserveBoard(player);
		playerBoards.put(player, board);
	}

	/**
	 * {@inheritDoc}
	 *
	 * @see org.chessworks.uscl.services.TournamentService#unreserveBoard(Player)
	 */
	public int unreserveBoard(Player player) {
		if (player == null)
			return -1;
		Integer board = playerBoards.remove(player);
		if (board == null)
			return -1;
		return board;
	}

	/**
	 * {@inheritDoc}
	 *
	 * @see org.chessworks.uscl.services.TournamentService#getPlayerBoard(Player)
	 */
	public int getPlayerBoard(Player player) {
		if (player == null)
			return -1;
		Integer board = playerBoards.get(player);
		if (board == null)
			return -1;
		return board;
	}

	/**
	 * {@inheritDoc}
	 *
	 * @see org.chessworks.uscl.services.TournamentService#getPlayerBoardMap()
	 */
	public Map<Player, Integer> getPlayerBoardMap() {
		return Collections.unmodifiableMap(playerBoards);
	}

	/**
	 * {@inheritDoc}
	 *
	 * @see org.chessworks.uscl.services.TournamentService#findOrCreatePlayer(java.lang.String)
	 */
	public Player findOrCreatePlayer(String handle) throws InvalidNameException {
		Player p = players.get(handle.toLowerCase());
		if (p == null) {
			p = createPlayer(handle);
		}
		return p;
	}

	/**
	 * {@inheritDoc}
	 *
	 * @see org.chessworks.uscl.services.TournamentService#findPlayer(java.lang.String)
	 */
	public Player findPlayer(String handle) {
		Player p = players.get(handle.toLowerCase());
		return p;
	}

	/**
	 * {@inheritDoc}
	 *
	 * @see org.chessworks.uscl.services.TournamentService#findOrCreateTeam(java.lang.String)
	 */
	public Team findOrCreateTeam(String handle) throws InvalidNameException {
		Team t = teams.get(handle.toUpperCase());
		if (t == null) {
			t = createTeam(handle);
		}
		return t;
	}

	/**
	 * {@inheritDoc}
	 *
	 * @see org.chessworks.uscl.services.TournamentService#findTeam(java.lang.String)
	 */
	public Team findTeam(String teamCode) {
		Team t = teams.get(teamCode.toUpperCase());
		return t;
	}

	/**
	 * {@inheritDoc}
	 *
	 * @see org.chessworks.uscl.services.TournamentService#findAllPlayers()
	 */
	public Collection<Player> findAllPlayers() {
		return allPlayers;
	}

	/**
	 * {@inheritDoc}
	 *
	 * @see org.chessworks.uscl.services.TournamentService#findAllTeams()
	 */
	public Collection<Team> findAllTeams() {
		return allTeams;
	}

	/**
	 * {@inheritDoc}
	 *
	 * @see org.chessworks.uscl.services.TournamentService#createPlayer(java.lang.String)
	 */
	public Player createPlayer(String handle) throws InvalidNameException {
		int i = handle.lastIndexOf('-');
		if (i < 0) {
			throw new InvalidNameException("Player handle \"{0}\" must end with a valid team code.", handle);
		}
		String teamCode = handle.substring(i + 1);
		Team team = teams.get(teamCode);
		if (team == null) {
			throw new InvalidNameException("Unknown team \"{0}\" for player handle \"{1}\".  Either correct the name or first add the team.", team,
					handle);
		}
		return createPlayer(handle, team);
	}

	/**
	 * {@inheritDoc}
	 *
	 * @see org.chessworks.uscl.services.TournamentService#createPlayer(java.lang.String, org.chessworks.uscl.model.Team)
	 */
	public Player createPlayer(String handle, Team team) throws InvalidNameException {
		Player p = players.get(handle);
		if (p != null) {
			throw new InvalidNameException("Player with the handle \"{0}\" already exists.", handle);
		}
		p = new Player(handle, team);
		team.getPlayers().add(p);
		players.put(handle.toLowerCase(), p);
		return p;
	}

	/**
	 * {@inheritDoc}
	 *
	 * @see org.chessworks.uscl.services.TournamentService#createTeam(java.lang.String)
	 */
	public Team createTeam(String teamCode) throws InvalidNameException {
		Team t = teams.get(teamCode);
		if (t != null) {
			throw new InvalidNameException("Team with the handle \"{0}\" is already in the tournament.", teamCode);
		}
		if (teamCode == null || teamCode.length() < 2 || teamCode.length() > 3) {
			throw new InvalidNameException("Teams must have a 2 or 3-letter team code.");
		}
		t = new Team(teamCode);
		teams.put(teamCode.toUpperCase(), t);
		return t;
	}

}
