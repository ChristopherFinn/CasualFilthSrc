package io.xeros.sql.leaderboard;

import io.xeros.Server;
import io.xeros.ServerState;
import io.xeros.content.leaderboards.LeaderboardEntry;
import io.xeros.content.leaderboards.LeaderboardType;
import io.xeros.test.ServerTest;
import org.junit.jupiter.api.Test;

import java.sql.ResultSet;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LeaderboardAddTest {

    private static final int addAmount = 77;
    private static final ServerTest serverTest = new ServerTest(ServerState.PUBLIC);

    @Test
    void leaderboard_transaction_prevents_multiple_entries() throws Exception {
        List<Future<?>> futures = new ArrayList<>();
        List<String> usernames = new ArrayList<>();
        for (int i = 0; i < 35; i++) {
            String name = "test" + i;
            usernames.add(name);
            Server.getDatabaseManager().executeImmediate(((context, connection) -> connection.createStatement().execute("DELETE FROM leaderboards WHERE username = '" + name + "'")));
        }

        for (String username : usernames) {
            futures.add(Server.getDatabaseManager().exec((context, connection) -> {
                for (int i = 0; i < addAmount; i++)
                    new LeaderboardAdd(new LeaderboardEntry(LeaderboardType.BOSS_POINTS, username, 1, LocalDateTime.now())).execute(context, connection);
                return null;
            }));
        }

        futures.forEach(it -> {
            try {
                it.get();
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        });


        for (String username : usernames) {
            Server.getDatabaseManager().executeImmediate(((context, connection) -> {
                ResultSet rs = connection.createStatement().executeQuery("SELECT * FROM leaderboards WHERE username = '" + username + "' AND type = " + LeaderboardType.BOSS_POINTS.ordinal());

                int count = 0;
                int amount = 0;

                while (rs.next()) {
                    count++;
                    amount = rs.getInt("amount");
                }

                assertEquals(1, count);
                assertEquals(amount, addAmount);
                return null;
            }));
        }
    }


}