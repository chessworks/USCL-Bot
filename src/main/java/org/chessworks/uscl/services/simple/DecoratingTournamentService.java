package org.chessworks.uscl.services.simple;

import java.util.Collection;
import java.util.Map;

import org.chessworks.uscl.model.Player;
import org.chessworks.uscl.model.Team;
import org.chessworks.uscl.services.InvalidNameException;
import org.chessworks.uscl.services.TournamentService;

/**
 * Delegates all calls to the underlying {@link TournamentService}. This is principally intended to be used as a base class for services which wish to
 * decorate a TournamentService used for data storage with live functionality that connects with a chess server.
 *
 * @author Doug Bateman
 *
 */
public class DecoratingTournamentService implements TournamentService {

	private TournamentService service;

	/**
	 * Delegates all calls to the underlying {@link TournamentService}.
	 *
	 * @see org.chessworks.uscl.services.TournamentService#clearSchedule()
	 */
	public void clearSchedule() {
		service.clearSchedule();
	}

	/**
	 * Delegates all calls to the underlying {@link TournamentService}.
	 *
	 * @see org.chessworks.uscl.services.TournamentService#createPlayer(java.lang.String, org.chessworks.uscl.model.Team)
	 */
	public Player createPlayer(String handle, Team team) throws InvalidNameException {
		return service.createPlayer(handle, team);
	}

	/**
	 * Delegates all calls to the underlying {@link TournamentService}.
	 *
	 * @see org.chessworks.uscl.services.TournamentService#createPlayer(java.lang.String)
	 */
	public Player createPlayer(String handle) throws InvalidNameException {
		return service.createPlayer(handle);
	}

	/**
	 * Delegates all calls to the underlying {@link TournamentService}.
	 *
	 * @see org.chessworks.uscl.services.TournamentService#createTeam(java.lang.String)
	 */
	public Team createTeam(String teamCode) throws InvalidNameException {
		return service.createTeam(teamCode);
	}

	/**
	 * Delegates all calls to the underlying {@link TournamentService}.
	 *
	 * @see org.chessworks.uscl.services.TournamentService#findAllPlayers()
	 */
	public Collection<Player> findAllPlayers() {
		return service.findAllPlayers();
	}

	/**
	 * Delegates all calls to the underlying {@link TournamentService}.
	 *
	 * @see org.chessworks.uscl.services.TournamentService#findAllTeams()
	 */
	public Collection<Team> findAllTeams() {
		return service.findAllTeams();
	}

	/**
	 * Delegates all calls to the underlying {@link TournamentService}.
	 *
	 * @see org.chessworks.uscl.services.TournamentService#findOrCreatePlayer(java.lang.String)
	 */
	public Player findOrCreatePlayer(String handle) throws InvalidNameException {
		return service.findOrCreatePlayer(handle);
	}

	/**
	 * Delegates all calls to the underlying {@link TournamentService}.
	 *
	 * @see org.chessworks.uscl.services.TournamentService#findOrCreateTeam(java.lang.String)
	 */
	public Team findOrCreateTeam(String handle) throws InvalidNameException {
		return service.findOrCreateTeam(handle);
	}

	/**
	 * Delegates all calls to the underlying {@link TournamentService}.
	 *
	 * @see org.chessworks.uscl.services.TournamentService#findPlayer(java.lang.String)
	 */
	public Player findPlayer(String handle) {
		return service.findPlayer(handle);
	}

	/**
	 * Delegates all calls to the underlying {@link TournamentService}.
	 *
	 * @see org.chessworks.uscl.services.TournamentService#findTeam(java.lang.String)
	 */
	public Team findTeam(String teamCode) {
		return service.findTeam(teamCode);
	}

	/**
	 * Delegates all calls to the underlying {@link TournamentService}.
	 *
	 * @see org.chessworks.uscl.services.TournamentService#getPlayerBoard(org.chessworks.uscl.model.Player)
	 */
	public int getPlayerBoard(Player player) {
		return service.getPlayerBoard(player);
	}

	/**
	 * Delegates all calls to the underlying {@link TournamentService}.
	 *
	 * @see org.chessworks.uscl.services.TournamentService#getPlayerBoardMap()
	 */
	public Map<Player, Integer> getPlayerBoardMap() {
		return service.getPlayerBoardMap();
	}

	/**
	 * Delegates all calls to the underlying {@link TournamentService}.
	 *
	 * @see org.chessworks.uscl.services.TournamentService#reserveBoard(org.chessworks.uscl.model.Player, int)
	 */
	public void reserveBoard(Player player, int board) {
		service.reserveBoard(player, board);
	}

	/**
	 * Delegates all calls to the underlying {@link TournamentService}.
	 *
	 * @see org.chessworks.uscl.services.TournamentService#schedule(org.chessworks.uscl.model.Player, org.chessworks.uscl.model.Player, int)
	 */
	public void schedule(Player white, Player black, int board) {
		service.schedule(white, black, board);
	}

	/**
	 * Delegates all calls to the underlying {@link TournamentService}.
	 *
	 * @see org.chessworks.uscl.services.TournamentService#unreserveBoard(org.chessworks.uscl.model.Player)
	 */
	public int unreserveBoard(Player player) {
		return service.unreserveBoard(player);
	}

	/**
	 * Delegates all calls to the underlying {@link TournamentService}.
	 *
	 * @see org.chessworks.uscl.services.TournamentService#flush()
	 */
	public void flush() {
		service.flush();
	}

}
