package org.chessworks.uscl.services.simple;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

import org.chessworks.common.service.BasicLifecycle;
import org.chessworks.uscl.model.Player;
import org.chessworks.uscl.model.Team;
import org.chessworks.uscl.services.InvalidPlayerException;
import org.chessworks.uscl.services.InvalidTeamException;
import org.chessworks.uscl.services.TournamentService;

public class SimpleTournamentService extends BasicLifecycle implements TournamentService {

	/** Map players to board numbers */
	private final Map<Player, Integer> playerBoards = new LinkedHashMap<Player, Integer>();

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
	@Override
	public void clearSchedule() {
		playerBoards.clear();
	}

	/**
	 * {@inheritDoc}
	 *
	 * @see org.chessworks.uscl.services.TournamentService#schedule(Player, Player, int)
	 */
	@Override
	public void schedule(Player white, Player black, int board) {
		reserveBoard(white, board);
		reserveBoard(black, board);
	}

	/**
	 * {@inheritDoc}
	 *
	 * @see org.chessworks.uscl.services.TournamentService#reserveBoard(Player, int)
	 */
	@Override
	public void reserveBoard(Player player, int board) {
		playerBoards.put(player, board);
	}

	/**
	 * {@inheritDoc}
	 * @see org.chessworks.uscl.services.TournamentService#reserveBoard(String, int, boolean)
	 */
	@Override
	public Player reserveBoard(String playerName, int board, boolean allowNewPlayer) throws InvalidPlayerException, InvalidTeamException {
		Player player = findPlayer(playerName);
		if (player == null) {
			if (!allowNewPlayer) {
				throw new InvalidPlayerException("\"{0} is not a recognized player name. Tell me \\\"addPlayer {0}\\\" to register the player.\"", playerName);
			} else {
				String teamCode = teamCode(playerName);
				Team team = findTeam(teamCode);
				if (team == null) {
					throw new InvalidTeamException("Unknown team: {0}", teamCode);
				}
				player = new Player(playerName, team);
			}
		}
		playerBoards.put(player, board);
		return player;
	}

	/**
	 * {@inheritDoc}
	 *
	 * @see org.chessworks.uscl.services.TournamentService#unreserveBoard(Player)
	 */
	@Override
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
	@Override
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
	@Override
	public Map<Player, Integer> getPlayerBoardMap() {
		return Collections.unmodifiableMap(playerBoards);
	}

	/**
	 * {@inheritDoc}
	 * @throws InvalidTeamException
	 * @throws InvalidPlayerException
	 *
	 * @see org.chessworks.uscl.services.TournamentService#findOrCreatePlayer(java.lang.String)
	 */
	@Override
	public Player findOrCreatePlayer(String handle) throws InvalidPlayerException, InvalidTeamException {
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
	@Override
	public Player findPlayer(String handle) {
		Player p = players.get(handle.toLowerCase());
		return p;
	}

	/**
	 * {@inheritDoc}
	 * @throws InvalidTeamException
	 *
	 * @see org.chessworks.uscl.services.TournamentService#findOrCreateTeam(java.lang.String)
	 */
	@Override
	public Team findOrCreateTeam(String handle) throws InvalidTeamException {
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
	@Override
	public Team findTeam(String teamCode) {
		Team t = teams.get(teamCode.toUpperCase());
		return t;
	}

	/**
	 * {@inheritDoc}
	 *
	 * @see org.chessworks.uscl.services.TournamentService#findAllPlayers()
	 */
	@Override
	public Collection<Player> findAllPlayers() {
		return allPlayers;
	}

	/**
	 * {@inheritDoc}
	 *
	 * @see org.chessworks.uscl.services.TournamentService#findAllTeams()
	 */
	@Override
	public Collection<Team> findAllTeams() {
		return allTeams;
	}

	/**
	 * {@inheritDoc}
	 *
	 * @see org.chessworks.uscl.services.TournamentService#createPlayer(java.lang.String)
	 */
	@Override
	public Player createPlayer(String handle) throws InvalidPlayerException, InvalidTeamException {
		String teamCode = teamCode(handle);
		Team team = teams.get(teamCode.toUpperCase());
		if (team == null) {
			throw new InvalidTeamException("Unknown team: {0}", teamCode);
		}
		return createPlayer(handle, team);
	}

	/**
	 * {@inheritDoc}
	 *
	 * @see org.chessworks.uscl.services.TournamentService#createPlayer(java.lang.String, org.chessworks.uscl.model.Team)
	 */
	@Override
	public Player createPlayer(String handle, Team team) throws InvalidPlayerException {
		Player p = players.get(handle.toLowerCase());
		if (p != null) {
			throw new InvalidPlayerException("Player with the handle \"{0}\" already exists", handle);
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
	@Override
	public Team createTeam(String teamCode) throws InvalidTeamException {
		Team t = teams.get(teamCode.toUpperCase());
		if (t != null) {
			throw new InvalidTeamException("Team with the handle \"{0}\" already exists", teamCode);
		}
		if (teamCode == null || teamCode.length() < 2 || teamCode.length() > 3) {
			throw new InvalidTeamException("Teams must have a 2 or 3-letter team code");
		}
		t = new Team(teamCode);
		teams.put(teamCode.toUpperCase(), t);
		return t;
	}

	/**
	 * {@inheritDoc}
	 *
	 * @see org.chessworks.uscl.services.TournamentService#removePlayer(Player)
	 */
	@Override
	public boolean removePlayer(Player player) {
		playerBoards.remove(player);
		String key = player.getHandle().toLowerCase();
		player = players.remove(key);
		if (player == null)
			return false;
		Team team = player.getTeam();
		team.getPlayers().remove(player);
		return true;
	}

	/**
	 * {@inheritDoc}
	 *
	 * @see org.chessworks.uscl.services.TournamentService#removeTeam(Team)
	 */
	@Override
	public int removeTeam(Team team) {
		String key = team.getTeamCode().toUpperCase();
		team = teams.remove(key);
		if (team == null)
			return -1;
		int count = 0;
		ArrayList<Player> list = new ArrayList<Player>(team.getPlayers());
		for (Player p : list) {
			boolean done = removePlayer(p);
			if (done) count++;
		}
		return count;
	}

	/**
	 * {@inheritDoc}
	 *
	 * @see org.chessworks.uscl.services.TournamentService#updatePlayer(Player)
	 */
	@Override
	public void updatePlayer(Player player) {
	}

	/**
	 * {@inheritDoc}
	 *
	 * @see org.chessworks.uscl.services.TournamentService#updateTeam(Team)
	 */
	@Override
	public void updateTeam(Team team) {
	}

	/**
	 * {@inheritDoc}
	 *
	 * @see org.chessworks.uscl.services.TournamentService#flush()
	 */
	@Override
	public void flush() {
	}

	protected void reset() {
		this.playerBoards.clear();
		this.players.clear();
		this.teams.clear();
	}

	public static String teamCode(String handle) throws InvalidPlayerException {
		int i = handle.lastIndexOf('-');
		if (i < 0) {
			throw new InvalidPlayerException("Player handle \"{0}\" must end with a valid team code", handle);
		}
		String teamCode = handle.substring(i + 1);
		return teamCode;
	}

}
