package org.chessworks.uscl.model.listeners;

import org.chessworks.uscl.model.Color;
import org.chessworks.uscl.model.LiveGame;
import org.chessworks.uscl.model.User;

public interface UserStatusListener {

	public void onArrival(User user);

	public void onDeparture(User user);

	public void onPlayingStarted(User player, LiveGame game, Color color);

	public void onPlayingFinished(User player, LiveGame game, Color color, boolean becomesExamined);

	public void onGameDeparture(User user, LiveGame game);

	public void onExamineStarted(User examiner, LiveGame game);

	public void onExamineFinished(User examiner, LiveGame game);

	public void onObserveStart(User observer, LiveGame game);

	public void onObserveFinished(User observer, LiveGame game);

}
