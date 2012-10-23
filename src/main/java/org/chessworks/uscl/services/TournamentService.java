package org.chessworks.uscl.services;

import java.io.Flushable;
import java.util.Collection;

import org.chessworks.uscl.model.Game;
import org.chessworks.uscl.model.GameState;
import org.chessworks.uscl.model.Player;
import org.chessworks.uscl.model.Team;

public interface TournamentService extends Flushable {

    Player createPlayer(String handle) throws InvalidPlayerException, InvalidTeamException;

    Player createPlayer(String handle, Team team) throws InvalidPlayerException;

    Player findOrCreatePlayer(String handle) throws InvalidPlayerException, InvalidTeamException;

    Player findPlayer(String handle);

    Collection<Player> findOnlinePlayers();

    Collection<Player> findAllPlayers();

    Collection<Player> findScheduledPlayers();

    boolean removePlayer(Player player);

    void updatePlayer(Player player);

    Team createTeam(String teamCode) throws InvalidTeamException;

    Team findOrCreateTeam(String handle) throws InvalidTeamException;

    Team findTeam(String teamCode);

    Collection<Team> findAllTeams();

    int removeTeam(Team team);

    void updateTeam(Team team);

    Game findGame(int gameNumber);

    Game findPlayerGame(Player player);

    Collection<Game> findAllGames();

    Game scheduleGame(Game game);

    Game scheduleGame(int board, Player white, Player black);

    void updateGameStatus(Game game, GameState status);

    Game cancelGame(Game game);

    Game cancelGame(Player player);

    Game cancelGame(int board);

    void clearSchedule();

    /** Saves any unwritten data. */
    void flush();

}
