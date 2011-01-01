package org.chessworks.chess.model.listeners;

import org.chessworks.chess.model.User;
import org.chessworks.uscl.model.LiveGame;

public interface GameStatusListener {

	public void onBoardOpened(LiveGame game);

	public void onBoardClosed(LiveGame game);

	public void onPlayingStarted(LiveGame game);

	public void onPlayingFinished(LiveGame game, boolean becomesExamined);

	public void onExamineStarted(User examiner, LiveGame game);

	public void onExamineFinished(User examiner, LiveGame game);

	public void onObserverArrived(User user);

	public void onObserverDeparted(User user);

	public void onExaminerArrived(User user);

	public void onExaminerDeparted(User user);

}
