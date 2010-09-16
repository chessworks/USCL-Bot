package org.chessworks.uscl.services.simple;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.chessworks.uscl.model.Player;
import org.chessworks.uscl.model.Team;
import org.chessworks.uscl.services.InvalidNameException;
import org.chessworks.uscl.services.TournamentService;

public class SimpleTournamentService implements TournamentService {

	private Map<Player, Integer> playerBoards = new LinkedHashMap<Player, Integer>();

	/** Map codes to teams */
	private final Map<String, Team> teams;

	/** A read-only wrapper for returning all teams. */
	private final Set<Team> allTeams;

	/** Map handles to players */
	private final Map<String, Player> players;

	/** A read-only wrapper for returning all players. */
	private final Set<Player> allPlayers;

	public SimpleTournamentService() {
		teams = new TreeMap<String, Team>();
		allTeams = Collections.unmodifiableSet((Set<Team>) teams.values());
		players = new TreeMap<String, Player>();
		allPlayers = Collections.unmodifiableSet((Set<Player>) players.values());
	}

	public void clearSchedule() {
		playerBoards.clear();
	}

	public void schedule(Player white, Player black, int board) {
		reserveBoard(white, board);
		reserveBoard(black, board);
	}

	public void reserveBoard(Player player, int board) {
		unreserveBoard(player);
		playerBoards.put(player, board);
	}

	public int unreserveBoard(Player player) {
		if (player == null)
			return -1;
		Integer board = playerBoards.remove(player);
		if (board == null)
			return -1;
		return board;
	}

	public int getPlayerBoard(Player player) {
		if (player == null)
			return -1;
		Integer board = playerBoards.get(player);
		if (board == null)
			return -1;
		return board;
	}

	public Map<Player, Integer> getPlayerBoardMap() {
		return Collections.unmodifiableMap(playerBoards);
	}

	/**
	 * {@inheritDoc}
	 *
	 * @see org.chessworks.uscl.services.TournamentService#findPlayer(java.lang.String)
	 */
	public Player findPlayer(String handle) {
		Player p = players.get(handle);
		return p;
	}

	/**
	 * {@inheritDoc}
	 *
	 * @see org.chessworks.uscl.services.TournamentService#findTeam(java.lang.String)
	 */
	public Team findTeam(String teamCode) {
		Team t = teams.get(teamCode);
		return t;
	}

	/**
	 * {@inheritDoc}
	 *
	 * @see org.chessworks.uscl.services.TournamentService#findAllPlayers()
	 */
	public Set<Player> findAllPlayers() {
		return allPlayers;
	}

	/**
	 * {@inheritDoc}
	 *
	 * @see org.chessworks.uscl.services.TournamentService#findAllTeams()
	 */
	public Set<Team> findAllTeams() {
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
		players.put(handle, p);
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
		teams.put(teamCode, t);
		return t;
	}

}
