package io.xeros.sql.outlast;

import com.google.common.collect.Lists;
import io.xeros.Server;
import io.xeros.ServerState;
import io.xeros.content.tournaments.OutlastLeaderboardType;
import io.xeros.content.tournaments.OutlastRecentWinner;
import io.xeros.content.tournaments.OutlastLeaderboardEntry;
import io.xeros.sql.SqlQuery;
import io.xeros.sql.displayname.RemoveDisplayNameSqlQuery;
import io.xeros.sql.displayname.SetDisplayNameSqlQuery;
import io.xeros.test.ServerTest;
import org.junit.jupiter.api.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.*;

class OutlastLeaderboardDatabaseTest {

    private static final String USERNAME = "Loving ThE LifE";
    private static final ServerTest serverTest = new ServerTest(ServerState.TEST_PUBLIC);

    private static <T> T exec(SqlQuery<T> query) throws Exception {
        return Server.getDatabaseManager().executeImmediate(query);
    }

    @BeforeEach
    @AfterEach
    public void deleteFromDatabase() throws Exception {
        exec((context, connection) -> connection.createStatement().execute("DELETE FROM " + new OutlastLeaderboardTable().getName()));
        exec((context, connection) -> connection.createStatement().execute("DELETE FROM " + new OutlastRecentWinnersTable().getName()));
    }

    @BeforeAll
    public static void addDisplayName() throws Exception {
        exec(new SetDisplayNameSqlQuery(USERNAME, USERNAME));
    }

    @AfterAll
    public static void removeDisplayName() throws Exception {
        exec(new RemoveDisplayNameSqlQuery(USERNAME));
    }

    @Test
    public void test_leaderboard_table() throws Exception {
        OutlastLeaderboardEntry entry = new OutlastLeaderboardEntry(USERNAME, USERNAME, 25, 5, 5.0, 10, 20);
        exec(new OutlastLeaderboardAdd(entry));

        List<OutlastLeaderboardEntry> entries = exec(new OutlastLeaderboardGetTop(OutlastLeaderboardType.WINS, 1));
        assertEquals(1, entries.size());
        assertEquals(entry, entries.get(0));
    }

    @Test
    public void test_recent_winners_table() throws Exception {
        int wins = 10;
        OutlastLeaderboardEntry entry = new OutlastLeaderboardEntry(USERNAME, USERNAME, 25, 5, 5.0, wins, 20);
        exec(new OutlastLeaderboardAdd(entry));

        OutlastRecentWinner winner = new OutlastRecentWinner(USERNAME, USERNAME, 0, LocalDateTime.now());
        exec(new OutlastRecentWinnersAdd(winner));

        List<OutlastRecentWinner> recents = exec(new OutlastRecentWinnersGetRecent(1));
        assertEquals(1, recents.size());

        OutlastRecentWinner recent = recents.get(0);
        assertEquals(wins, recent.getWins());
    }

    @Test
    public void test_leaderboard_sorting() throws Exception {
        int amount = 50;
        double dividend = 500.0;
        List<OutlastLeaderboardEntry> entries = Lists.newArrayList();
        for (int i = amount; i > 0; i--) {
            String username = "" + i;
            exec(new SetDisplayNameSqlQuery(username, username));
            OutlastLeaderboardEntry entry = new OutlastLeaderboardEntry(username, USERNAME, i, i, i / dividend, i, i);
            entries.add(entry);
            exec(new OutlastLeaderboardAdd(entry));
        }

        List<OutlastLeaderboardEntry> moreThanTotal = exec(new OutlastLeaderboardGetTop(OutlastLeaderboardType.KILLS, amount + 5));
        assertEquals(amount, moreThanTotal.size());

        List<OutlastLeaderboardEntry> killsLeader = exec(new OutlastLeaderboardGetTop(OutlastLeaderboardType.KILLS, 10));
        assertEquals(10, killsLeader.size());
        assertEquals(amount, killsLeader.get(0).getKills());
        assertEquals(entries.subList(0, 10), killsLeader);

        List<OutlastLeaderboardEntry> winsLeader = exec(new OutlastLeaderboardGetTop(OutlastLeaderboardType.WINS, 5));
        assertEquals(5, winsLeader.size());
        assertEquals(amount, winsLeader.get(0).getKills());
        assertEquals(entries.subList(0, 5), winsLeader);

        List<OutlastLeaderboardEntry> kdrLeader = exec(new OutlastLeaderboardGetTop(OutlastLeaderboardType.KDR, 45));
        assertEquals(45, kdrLeader.size());
        assertEquals(amount / dividend, kdrLeader.get(0).getKdr());
        assertEquals(entries.subList(0, 45), kdrLeader);

        for (OutlastLeaderboardEntry it : entries) {
            exec(new RemoveDisplayNameSqlQuery(it.getUsername()));
        }
    }

}