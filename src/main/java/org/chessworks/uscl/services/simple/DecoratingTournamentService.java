package org.chessworks.uscl.services.simple;

import java.util.Collection;

import org.chessworks.uscl.model.Game;
import org.chessworks.uscl.model.GameState;
import org.chessworks.uscl.model.Player;
import org.chessworks.uscl.model.Team;
import org.chessworks.uscl.services.InvalidPlayerException;
import org.chessworks.uscl.services.InvalidTeamException;
import org.chessworks.uscl.services.TournamentService;

/**
 * Delegates all calls to the underlying {@link TournamentService}. This is principally intended to be used as a base class for services which wish to
 * decorate a TournamentService used for data storage with live functionality that connects with a chess server.
 *
 * @author Doug Bateman
 */
public class DecoratingTournamentService implements TournamentService {

	private TournamentService service;

	/**
	 * Delegates all calls to the underlying {@link TournamentService}.
	 *
	 * @see org.chessworks.uscl.services.TournamentService#cancelGame(Game)
	 */
	@Override
	public Game cancelGame(Game game) {
		return service.cancelGame(game);
	}

	/**
	 * Delegates all calls to the underlying {@link TournamentService}.
	 *
	 * @see org.chessworks.uscl.services.TournamentService#cancelGame(int)
	 */
	@Override
	public Game cancelGame(int board) {
		return service.cancelGame(board);
	}

	/**
	 * Delegates all calls to the underlying {@link TournamentService}.
	 *
	 * @see org.chessworks.uscl.services.TournamentService#cancelGame(Player)
	 */
	@Override
	public Game cancelGame(Player player) {
		return service.cancelGame(player);
	}

	/**
	 * Delegates all calls to the underlying {@link TournamentService}.
	 *
	 * @see org.chessworks.uscl.services.TournamentService#clearSchedule()
	 */
	@Override
	public void clearSchedule() {
		service.clearSchedule();
	}

	/**
	 * Delegates all calls to the underlying {@link TournamentService}.
	 * @throws InvalidTeamException
	 * @throws InvalidPlayerException
	 *
	 * @see org.chessworks.uscl.services.TournamentService#createPlayer(java.lang.String)
	 */
	@Override
	public Player createPlayer(String handle) throws InvalidPlayerException, InvalidTeamException {
		return service.createPlayer(handle);
	}

	/**
	 * Delegates all calls to the underlying {@link TournamentService}.
	 * @throws InvalidPlayerException
	 *
	 * @see org.chessworks.uscl.services.TournamentService#createPlayer(java.lang.String, org.chessworks.uscl.model.Team)
	 */
	@Override
	public Player createPlayer(String handle, Team team) throws InvalidPlayerException {
		return service.createPlayer(handle, team);
	}

	/**
	 * Delegates all calls to the underlying {@link TournamentService}.
	 * @throws InvalidTeamException
	 *
	 * @see org.chessworks.uscl.services.TournamentService#createTeam(java.lang.String)
	 */
	@Override
	public Team createTeam(String teamCode) throws InvalidTeamException {
		return service.createTeam(teamCode);
	}

	/**
	 * Delegates all calls to the underlying {@link TournamentService}.
	 *
	 * @see org.chessworks.uscl.services.TournamentService#findAllGames()
	 */
	@Override
	public Collection<Game> findAllGames() {
		return service.findAllGames();
	}

	/**
	 * Delegates all calls to the underlying {@link TournamentService}.
	 *
	 * @see org.chessworks.uscl.services.TournamentService#findAllPlayers()
	 */
	@Override
	public Collection<Player> findAllPlayers() {
		return service.findAllPlayers();
	}

	/**
	 * Delegates all calls to the underlying {@link TournamentService}.
	 *
	 * @see org.chessworks.uscl.services.TournamentService#findAllTeams()
	 */
	@Override
	public Collection<Team> findAllTeams() {
		return service.findAllTeams();
	}

	/**
	 * Delegates all calls to the underlying {@link TournamentService}.
	 *
	 * @see org.chessworks.uscl.services.TournamentService#findGame(int)
	 */
	@Override
	public Game findGame(int gameNumber) {
		return service.findGame(gameNumber);
	}

    /**
     * Delegates all calls to the underlying {@link TournamentService}.
     *
     * @see org.chessworks.uscl.services.TournamentService#findMatchGames(Team, Team)
     */
    @Override
    public Collection<Game> findMatchGames(Team team1, Team team2) {
        return service.findMatchGames(team1, team2);
    }

	/**
	 * Delegates all calls to the underlying {@link TournamentService}.
	 *
	 * @see org.chessworks.uscl.services.TournamentService#findOnlinePlayers()
	 */
	@Override
	public Collection<Player> findOnlinePlayers() {
		return service.findOnlinePlayers();
	}

	/**
	 * Delegates all calls to the underlying {@link TournamentService}.
	 * @throws InvalidTeamException
	 * @throws InvalidPlayerException
	 *
	 * @see org.chessworks.uscl.services.TournamentService#findOrCreatePlayer(java.lang.String)
	 */
	@Override
	public Player findOrCreatePlayer(String handle) throws InvalidPlayerException, InvalidTeamException {
		return service.findOrCreatePlayer(handle);
	}
	
	/**
	 * Delegates all calls to the underlying {@link TournamentService}.
	 * @throws InvalidTeamException
	 *
	 * @see org.chessworks.uscl.services.TournamentService#findOrCreateTeam(java.lang.String)
	 */
	@Override
	public Team findOrCreateTeam(String handle) throws InvalidTeamException {
		return service.findOrCreateTeam(handle);
	}

	/**
	 * Delegates all calls to the underlying {@link TournamentService}.
	 *
	 * @see org.chessworks.uscl.services.TournamentService#findPlayer(java.lang.String)
	 */
	@Override
	public Player findPlayer(String handle) {
		return service.findPlayer(handle);
	}

	/**
	 * Delegates all calls to the underlying {@link TournamentService}.
	 *
	 * @see org.chessworks.uscl.services.TournamentService#findPlayerGame(Player)
	 */
	@Override
	public Game findPlayerGame(Player player) {
		return service.findPlayerGame(player);
	}

	/**
	 * Delegates all calls to the underlying {@link TournamentService}.
	 *
	 * @see org.chessworks.uscl.services.TournamentService#findScheduledPlayers()
	 */
	@Override
	public Collection<Player> findScheduledPlayers() {
		return service.findScheduledPlayers();
	}

	/**
	 * Delegates all calls to the underlying {@link TournamentService}.
	 *
	 * @see org.chessworks.uscl.services.TournamentService#findTeam(java.lang.String)
	 */
	@Override
	public Team findTeam(String teamCode) {
		return service.findTeam(teamCode);
	}

	/**
	 * Delegates all calls to the underlying {@link TournamentService}.
	 *
	 * @see org.chessworks.uscl.services.TournamentService#flush()
	 */
	public void flush() {
		service.flush();
	}

	/**
	 * Delegates all calls to the underlying {@link TournamentService}.
	 *
	 * @see org.chessworks.uscl.services.TournamentService#removePlayer(org.chessworks.uscl.model.Player)
	 */
	@Override
	public boolean removePlayer(Player player) {
		return service.removePlayer(player);
	}

	/**
	 * Delegates all calls to the underlying {@link TournamentService}.
	 *
	 * @see org.chessworks.uscl.services.TournamentService#removeTeam(org.chessworks.uscl.model.Team)
	 */
	@Override
	public int removeTeam(Team team) {
		return service.removeTeam(team);
	}

	/**
	 * Delegates all calls to the underlying {@link TournamentService}.
	 *
	 * @see org.chessworks.uscl.services.TournamentService#scheduleGame(Game)
	 */
	@Override
	public Game scheduleGame(Game game) {
		return service.scheduleGame(game);
	}

	/**
	 * Delegates all calls to the underlying {@link TournamentService}.
	 *
	 * @see org.chessworks.uscl.services.TournamentService#scheduleGame(int, int, Player, Player)
	 */
	@Override
	public Game scheduleGame(int board, int event, Player white, Player black) {
		return service.scheduleGame(board, event, white, black);
	}

    /**
     * Delegates all calls to the underlying {@link TournamentService}.
     *
     * @see org.chessworks.uscl.services.TournamentService#updateGameStatus(Game, GameState)
     */
	@Override
    public void updateGameStatus(Game game, GameState status) {
	    service.updateGameStatus(game, status);
    }

    /**
	 * Delegates all calls to the underlying {@link TournamentService}.
	 *
	 * @see org.chessworks.uscl.services.TournamentService#updatePlayer(org.chessworks.uscl.model.Player)
	 */
	@Override
	public void updatePlayer(Player player) {
		service.updatePlayer(player);
	}

	/**
	 * Delegates all calls to the underlying {@link TournamentService}.
	 *
	 * @see org.chessworks.uscl.services.TournamentService#updateTeam(org.chessworks.uscl.model.Team)
	 */
	@Override
	public void updateTeam(Team team) {
		service.updateTeam(team);
	}


}
