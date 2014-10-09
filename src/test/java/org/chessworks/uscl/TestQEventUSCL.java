package org.chessworks.uscl;

import org.chessworks.chess.model.RatingCategory;
import org.chessworks.chess.model.User;
import org.chessworks.chess.services.simple.SimpleTitleService;
import org.chessworks.uscl.model.Player;
import org.chessworks.uscl.model.Team;
import org.junit.Assert;
import org.junit.Test;

public class TestQEventUSCL {

    private static final RatingCategory fiveMinute = new RatingCategory("5-minute");
    private static final Team team = new Team("ICC");
    private static final Player player1 = new Player("Duckstorm-ICC", team);
    static {
        player1.setRating(fiveMinute, 1900);
        player1.setRealName("Doug Bateman");
    }
    private static final Player player2 = new Player("RdgMx-ICC", team);
    static {
        player2.getTitles().add(SimpleTitleService.SM);
        player2.setRating(fiveMinute, 1250);
        player2.setRealName("Rodrigo de Mello");
    }
    private static final Player player3 = new Player("Long-Handle-ICC", team);
    static {
        player3.getTitles().add(SimpleTitleService.WGM);
        player3.setRating(fiveMinute, 2500);
        player3.setRealName("FirstName MiddleName LastName");
    }
    private static final String libraryHandle = "USCL";
    private static final int librarySlot = 55;
    
    @Test
    public void testUsclExamine() {
        String expectedResult1 = "qaddevent 196 15 1-0  Duckstorm-ICC (1900) - SM RdgMx-ICC (1250) | examine USCL %55 |  |  | ";
        String expectedResult2 = "qaddevent 196 15 1-0  SM RdgMx-ICC (1250) - WGM Long-Handle-ICC (2500) | examine USCL %55 |  |  | ";
        String expectedResult3 = "qaddevent 196 15 1-0  WGM Long-Handle-ICC (2500) - Duckstorm-ICC (1900) | examine USCL %55 |  |  | ";
        testUsclExamine(player1, player2, expectedResult1);
        testUsclExamine(player2, player3, expectedResult2);
        testUsclExamine(player3, player1, expectedResult3);
    }

    public void testUsclExamine(User player1, User player2, String expectedResult) {
        int eventSlot = 196;
        String player1Name = player1.getPreTitledHandle(fiveMinute);
        String player2Name = player2.getPreTitledHandle(fiveMinute);
        QEvent event = QEvent.event(eventSlot)
            .description("%-4s %s - %s", "1-0", player1Name, player2Name)
            .addJoinCommand("examine %s %%%d", libraryHandle, librarySlot)
            .allowGuests(true)
            .validate();
        String actualResult = event.toString();
        System.out.println(actualResult);
        Assert.assertEquals(expectedResult, actualResult);
    }

    @Test
    public void testUsclExamineTabular() {
        String expectedResult1 = "qaddevent 196 15 1-0        Duckstorm-ICC (1900) -        SM RdgMx-ICC (1250) | examine USCL %55 |  |  | ";
        String expectedResult2 = "qaddevent 196 15 1-0         SM RdgMx-ICC (1250) - WGM Long-Handle-ICC (2500) | examine USCL %55 |  |  | ";
        String expectedResult3 = "qaddevent 196 15 1-0  WGM Long-Handle-ICC (2500) -       Duckstorm-ICC (1900) | examine USCL %55 |  |  | ";
        testUsclExamineTabular(player1, player2, expectedResult1);
        testUsclExamineTabular(player2, player3, expectedResult2);
        testUsclExamineTabular(player3, player1, expectedResult3);
    }

    public void testUsclExamineTabular(User player1, User player2, String expectedResult) {
        int eventSlot = 196;
        String player1Name = player1.getPreTitledHandle(fiveMinute);
        String player2Name = player2.getPreTitledHandle(fiveMinute);
        QEvent event = QEvent.event(eventSlot)
            .description("%-4s %26s - %26s", "1-0", player1Name, player2Name)
            .addJoinCommand("examine %s %%%d", libraryHandle, librarySlot)
            .allowGuests(true)
            .validate();
        String actualResult = event.toString();
        System.out.println(actualResult);
        Assert.assertEquals(expectedResult, actualResult);
    }

    @Test
    public void testUsclObserve() {
        String expectedResult1 = "qaddevent 196 14 LIVE Duckstorm-ICC (1900) - SM RdgMx-ICC (1250) |  | follow Duckstorm-ICC |  | ";
        String expectedResult2 = "qaddevent 196 14 LIVE SM RdgMx-ICC (1250) - WGM Long-Handle-ICC (2500) |  | follow RdgMx-ICC |  | ";
        String expectedResult3 = "qaddevent 196 14 LIVE WGM Long-Handle-ICC (2500) - Duckstorm-ICC (1900) |  | follow Long-Handle-ICC |  | ";
        testUsclObserve(player1, player2, expectedResult1);
        testUsclObserve(player2, player3, expectedResult2);
        testUsclObserve(player3, player1, expectedResult3);
    }
    
    public void testUsclObserve(User player1, User player2, String expectedResult) {
        int eventSlot = 196;
        String player1Name = player1.getPreTitledHandle(fiveMinute);
        String player2Name = player2.getPreTitledHandle(fiveMinute);
        QEvent event = QEvent.event(eventSlot)
            .description("%-4s %s - %s", "LIVE", player1Name, player2Name)
            .addWatchCommand("follow %s", player1.getHandle())
            .allowGuests(false)
            .validate();
        String actualResult = event.toString();
        System.out.println(actualResult);
        Assert.assertEquals(expectedResult, actualResult);
    }

    @Test
    public void testUsclObserveTabular() {
        String expectedResult1 = "qaddevent 196 14 LIVE       Duckstorm-ICC (1900) -        SM RdgMx-ICC (1250) |  | follow Duckstorm-ICC |  | ";
        String expectedResult2 = "qaddevent 196 14 LIVE        SM RdgMx-ICC (1250) - WGM Long-Handle-ICC (2500) |  | follow RdgMx-ICC |  | ";
        String expectedResult3 = "qaddevent 196 14 LIVE WGM Long-Handle-ICC (2500) -       Duckstorm-ICC (1900) |  | follow Long-Handle-ICC |  | ";
        testUsclObserveTabular(player1, player2, expectedResult1);
        testUsclObserveTabular(player2, player3, expectedResult2);
        testUsclObserveTabular(player3, player1, expectedResult3);
    }
    
    public void testUsclObserveTabular(User player1, User player2, String expectedResult) {
        int eventSlot = 196;
        String player1Name = player1.getPreTitledHandle(fiveMinute);
        String player2Name = player2.getPreTitledHandle(fiveMinute);
        QEvent event = QEvent.event(eventSlot)
            .description("%-4s %26s - %26s", "LIVE", player1Name, player2Name)
            .addWatchCommand("follow %s", player1.getHandle())
            .allowGuests(false)
            .validate();
        String actualResult = event.toString();
        System.out.println(actualResult);
        Assert.assertEquals(expectedResult, actualResult);
    }
}

/*
qaddevent 321 7 Kasparov - Leko game (being played at Linares, Spain) live discussion and analysis! | | observe 1 | finger Linares2000 |   
qaddevent 350 7 CHESS.FM Internet radio "Chess & Books" with Fred Wilson and Bruce Pandolfini | http://chess.fm | | finger chessfm    
qaddevent 420 0 Blitz tournament! Tomato 5 0 Swiss, have 3 players, looking for 8..16. | tell tomato join & tell 46 Hi | | tell tomato info | This tournament will take an hour or two, are you fairly sure you will have time to complete it?   
qaddevent 412 0 Tournament upcoming at 7pm:  3 0 Championship Qualifier!  Run by LittlePer(TD) in channel 225, Daruma and WilyEcote managing. | | | finger championship |   
qaddevent 412 0 3 0 Championship Qualifier Tournament!  LittlePer(TD) 12-round Swiss underway already. | tell littlePer latejoin & tell 225 Hi | | finger championship | This tourney will take a couple hours; are you fairly sure you'll have time to co
mplete it?   
qaddevent 20 7 Ftacnik(GM) and DrK(IM) give a Lecture!  Petrosian - Botvinnik 1963 | | follow DrK | finger DrK   
qaddevent 22 7 Play the Master, lhg!  For players under 1800.  Matches are 2 5 u, kibitzers are welcome.  | match lhg 2 5 u | follow lhg | help PLAYtheMASTER   
qaddevent 3 4 Play Schroer(IM) in a simultaneous exhibition, free!  35 35 time control.  Limited to 35 entries, some restrictions apply. | play schroer & tell 3 Hi| | help schroer |   
qaddevent 3 7 Schroer(IM) is giving a simultaneous exhibition!  5-chekel entry.   30 0 time control. | tell 3 hi & c-offer schroer 5 for simul & play schroer | follow schroer | news 1307 | This will cost 5 chekels, are you sure?   
qaddevent 996 14 Sign up for the August Standard Tournament | view activities/sttourney-signup.php | | news 1815
qaddevent 50 7 Guests:  Register an ICC name, and start your free trial!  No obligations, no bills. | register | | help register | | guest
*/
