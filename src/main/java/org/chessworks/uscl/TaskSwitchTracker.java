package org.chessworks.uscl;

import java.io.IOException;

import org.chessworks.chess.model.Role;
import org.chessworks.chess.services.UserService;
import org.chessworks.chessclub.ChatType;
import org.chessworks.uscl.model.Game;
import org.chessworks.uscl.services.TournamentService;

import free.chessclub.ChessclubConnection;
import free.chessclub.level2.Datagram;
import free.chessclub.level2.DatagramEvent;
import free.chessclub.level2.DatagramListener;

public class TaskSwitchTracker implements DatagramListener {

    /** Users with the busters role get notifications about task-switch events. */
    private Role bustersRole;
    
    private ChessclubConnection conn;
    private USCLBot usclBot;
    private TournamentService tournamentService;
    
    /**
     * Sends a routine tell to busters/speedtrap. This is typically used to keep them informed of task-switch notifications. The usclBot uses atells
     * rather than regular tells, as this makes it easy for the manager to distinguish between tells sent by players (who expect a reply) and routine
     * tells sent by the usclBot.
     */
    public void tellBusters(String msg, Object... args) {
        usclBot.broadcast(ChatType.PERSONAL_ADMIN_TELL, bustersRole, msg, args);
    }

    /**
     * Handles incoming DG_GAME_MESSAGE datagrams from the server.
     * 
     * The server sends this datagram anytime it wishes to print a message about
     * the game. Such messages include going forward/backwards/etc.. USCLBot is
     * a Speedtrap member and uses this datagram receive task-switch
     * notification messages.
     * 
     * If a task switch event occurs, the usclBot will alert the event channel so
     * the tournament director can take appropriate action.
     */
    public void processGameMessage(int gameNumber, String message) {
        boolean taskSwitch = (message.contains(" focus "));
        if (!taskSwitch)
            return;
        Game game = tournamentService.findGame(gameNumber);
        if (game == null) {
            return;
        }
        if (!game.status.isPlaying()) {
            return;
        }
        tellBusters("Task Switch in {0}: {1}", game.getStatusString(), message);
    }

    @Override
    public void datagramReceived(DatagramEvent evt) {
        Datagram datagram = evt.getDatagram();
        int code = datagram.getId();
        switch (code) {
            case Datagram.DG_GAME_MESSAGE : {
                int gameNumber = datagram.getInteger(0);
                String message = datagram.getString(1);
                processGameMessage(gameNumber, message);
            }
        }
    }
    
    public void start() throws IOException {
        conn.addDatagramListener(this, Datagram.DG_GAME_MESSAGE);
    }

    public void setConnection(ChessclubConnection conn) {
        this.conn = conn;
    }

    public final void setUSCLBot(USCLBot bot) {
        this.usclBot = bot;
    }

    public void setUserService(UserService userService) {
        this.bustersRole = userService.findOrCreateRole("busters");
    }

    public void setTournamentService(TournamentService tournamentService) {
        this.tournamentService = tournamentService;
    }

}
