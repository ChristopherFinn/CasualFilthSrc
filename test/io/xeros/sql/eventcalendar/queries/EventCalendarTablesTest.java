package io.xeros.sql.eventcalendar.queries;

import java.sql.Statement;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import com.google.common.collect.Lists;
import io.xeros.Server;
import io.xeros.ServerState;
import io.xeros.content.event.eventcalendar.ChallengeParticipant;
import io.xeros.content.event.eventcalendar.ChallengeWinner;
import io.xeros.content.event.eventcalendar.EventCalendarDay;
import io.xeros.content.event.eventcalendar.EventChallengeMonthlyReward;
import io.xeros.model.entity.player.Player;
import io.xeros.sql.DatabaseManager;
import io.xeros.sql.DatabaseTable;
import io.xeros.sql.displayname.GetDisplayNameSqlQuery;
import io.xeros.sql.displayname.RemoveDisplayNameSqlQuery;
import io.xeros.sql.displayname.SetDisplayNameSqlQuery;
import io.xeros.sql.eventcalendar.tables.EventCalendarParticipantsTable;
import io.xeros.sql.eventcalendar.tables.EventCalendarWinnersTable;
import io.xeros.test.ServerTest;
import io.xeros.test.TestPlayer;
import lombok.extern.java.Log;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@Log
class EventCalendarTablesTest {

    private static final ServerTest serverTest = new ServerTest(ServerState.PUBLIC);
    private static final Player player = TestPlayer.builder().username("Michael").build().player();

    private static final String[] USERNAMES = new String[9];
    private static final String ip = "127.0.0.1";
    private static final String mac = "unknown";
    private static final int amount = 2;

    private static final DatabaseTable PARTICIPANTS_TABLE = new EventCalendarParticipantsTable();
    private static final DatabaseTable WINNERS_TABLE = new EventCalendarWinnersTable();

    private static void clearDatabases(DatabaseManager manager) throws ExecutionException, InterruptedException {
        manager.exec((context, connection) -> {
            Statement statement = connection.createStatement();
            if (context.isTablePresent(PARTICIPANTS_TABLE, connection)) {
                statement.executeUpdate("DELETE FROM " + PARTICIPANTS_TABLE.getName());
                log.info("Cleared " + PARTICIPANTS_TABLE.getName());
            }
            if (context.isTablePresent(WINNERS_TABLE, connection)) {
                statement.executeUpdate("DELETE FROM " + WINNERS_TABLE.getName());
                log.info("Cleared " + WINNERS_TABLE.getName());
            }
            return null;
        }).get();
    }

    @BeforeAll
    public static void addDisplayNames() throws Exception {
        for (int i = 0; i < USERNAMES.length; i++)
            USERNAMES[i] = "CalendarTestUser" + (i + 1);
        Server.getDatabaseManager().executeImmediate(((context, connection) -> {
            for (String username : USERNAMES) {
                new SetDisplayNameSqlQuery(username, username).execute(context, connection);
            }

            return null;
        }));
    }

    @AfterAll
    public static void removeDisplayNames() throws Exception {
        Server.getDatabaseManager().executeImmediate(((context, connection) -> {
            for (String username : USERNAMES) {
                new RemoveDisplayNameSqlQuery(username).execute(context, connection);
            }

            return null;
        }));
    }

    @Test
    public void test() throws Exception {
        test_blacklist();
        test_add_another_entry_on_vote();
        test_queries();
        test_player_already_participated();
    }

    @Test
    public void test_blacklist() {
        DatabaseManager manager = new DatabaseManager(true);

        try {
            clearDatabases(manager);
            manager.exec((context, connection) -> {
                ChallengeParticipant computer_a1 = new ChallengeParticipant(USERNAMES[0], ip, mac, 1);
                ChallengeParticipant computer_a2 = new ChallengeParticipant(USERNAMES[1], ip, mac + "1", 1);
                ChallengeParticipant computer_a3 = new ChallengeParticipant(USERNAMES[2], ip + "1", mac, 1);
                ChallengeParticipant computer_b1 = new ChallengeParticipant(USERNAMES[3], ip + "1", mac + "1", 1);
                new AddToBlacklistQuery(computer_a1).execute(context, connection);
                assertTrue(new CheckForBlacklistQuery(computer_a1).execute(context, connection));
                assertTrue(new CheckForBlacklistQuery(computer_a2).execute(context, connection));
                assertTrue(new CheckForBlacklistQuery(computer_a3).execute(context, connection));
                assertFalse(new CheckForBlacklistQuery(computer_b1).execute(context, connection));
                new RemoveFromBlacklistQuery(computer_a1).execute(context, connection);
                assertFalse(new CheckForBlacklistQuery(computer_a1).execute(context, connection));
                assertFalse(new CheckForBlacklistQuery(computer_a2).execute(context, connection));
                assertFalse(new CheckForBlacklistQuery(computer_a3).execute(context, connection));
                assertFalse(new CheckForBlacklistQuery(computer_b1).execute(context, connection));
                return null;
            });

            clearDatabases(manager);
            manager.shutdown();

        } catch (Exception e) {
            e.printStackTrace();
            fail();
        }
    }

    @Test
    public void test_player_already_participated() {
        DatabaseManager manager = new DatabaseManager(true);

        try {
            clearDatabases(manager);
            manager.exec((context, connection) -> {
                ChallengeParticipant computer_a1 = new ChallengeParticipant(USERNAMES[0], ip, mac, 1);
                ChallengeParticipant computer_a2 = new ChallengeParticipant(USERNAMES[1], ip, mac + "1", 1);
                ChallengeParticipant computer_a3 = new ChallengeParticipant(USERNAMES[2], ip + "1", mac, 1);
                ChallengeParticipant computer_b1 = new ChallengeParticipant(USERNAMES[3], ip + "1", mac + "1", 1);
                new AddParticipantQuery(new ChallengeParticipant(USERNAMES[0], ip, mac, 1), 1).execute(context, connection);
                assertEquals(true, new HasPlayerAlreadyParticipatedQuery(computer_a1).execute(context, connection));
                assertEquals(true, new HasPlayerAlreadyParticipatedQuery(computer_a2).execute(context, connection));
                assertEquals(true, new HasPlayerAlreadyParticipatedQuery(computer_a3).execute(context, connection));
                assertEquals(false, new HasPlayerAlreadyParticipatedQuery(computer_b1).execute(context, connection));
                return null;
            });

            clearDatabases(manager);
            manager.shutdown();
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        }
    }

    @Test
    public void test_add_another_entry_on_vote() {
        DatabaseManager manager = new DatabaseManager(true);

        try {
            clearDatabases(manager);
            manager.exec(((context, connection) -> {
                ChallengeParticipant participant = new ChallengeParticipant(USERNAMES[0], ip, mac, 1);
                new AddParticipantQuery(participant, 1);
                assertEquals(new AddParticipantEntryOnVoteQuery(participant).execute(context, connection), true);
                return null;
            }));

            clearDatabases(manager);
            manager.shutdown();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void test_queries() throws Exception {
        DatabaseManager manager = new DatabaseManager(true);

        try {
            clearDatabases(manager);

            for (EventCalendarDay day : EventCalendarDay.values()) {
                testCalendarSystem(manager, day.getDay());
            }

            log.info("Printing all winners..");
            for (ChallengeWinner winner : manager.executeImmediate(new GetWinnersListQuery())) {
                log.info(winner.toString());
            }

            log.info("Printing participants..");
            for (EventCalendarDay day : EventCalendarDay.values()) {
                for (ChallengeParticipant participant : manager.executeImmediate(new GetParticipantsListQuery(day.getDay()))) {
                    log.info("Iterating over day [" + day + "], partipant: " + participant.toString());
                }
            }

            System.out.println(manager.executeImmediate(new GetMonthlyCalendarParticipants()));
            clearDatabases(manager);
            manager.shutdown();
        } catch (ExecutionException | InterruptedException e) {
            e.printStackTrace();
            fail();
        }
    }

    public void testCalendarSystem(DatabaseManager manager, int day) throws Exception {
        List<Future<?>> futureList = Lists.newArrayList();

        for (String username : USERNAMES) {
            futureList.add(manager.exec(new AddParticipantQuery(new ChallengeParticipant(username, ip, mac, day), amount)));
        }

        for (Future future : futureList) {
            future.get(); // wait
        }

        ChallengeParticipant winner = manager.executeImmediate(new SelectWinnerQuery(day));
        log.info("Winner selected: " + winner + " for day " + day);
    }

}