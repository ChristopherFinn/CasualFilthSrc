package io.xeros.sql.donation.reclaim;

import com.google.common.base.Preconditions;
import io.xeros.Server;
import io.xeros.ServerState;
import io.xeros.model.entity.player.Player;
import io.xeros.model.entity.player.save.PlayerSaveOffline;
import io.xeros.sql.DatabaseCredentials;
import io.xeros.sql.DatabaseManager;
import io.xeros.sql.EmbeddedDatabase;
import io.xeros.sql.SqlQuery;
import io.xeros.test.ServerTest;
import io.xeros.test.TestPlayer;
import org.junit.jupiter.api.*;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ReclaimTest {

    private static final DatabaseCredentials RECLAIM_DATABSE = new EmbeddedDatabase("reclaim_test");
    private static final DatabaseManager manager = new DatabaseManager(true);

    private static final String PASSWORD_PLAIN_TEXT = "testing";
    private static final int AMOUNT_DONATED = 150;

    private static final ServerTest serverTest = new ServerTest(ServerState.PUBLIC);
    private static final Player oldPlayer = TestPlayer.builder().username("Will").build().player(); // Will has donated $1k+
    private static final Player newPlayer = TestPlayer.builder().username("Will2").build().player();
    private static final Player newPlayerAlt = TestPlayer.builder().username("Will3").build().player();

    private static <T> T execReclaim(SqlQuery<T> query) throws Exception {
        return Server.getDatabaseManager().executeImmediate(query);
    }

    @Test
    @Order(1)
    public void testPasswordRetrieval() throws IOException {
        File oldFilesLocation = ReclaimQuery.OLD_FILES_LOCATION;

        if (!oldFilesLocation.exists()) {
            oldFilesLocation.mkdirs();
        }

        BufferedWriter writer = new BufferedWriter(new FileWriter(oldFilesLocation + "/" + oldPlayer.getLoginName() + ".txt"));
        writer.write("[ACCOUNT]\n" +
                "character-username = " + oldPlayer.getLoginName() + "\n" +
                "character-password = ae2b1fca515949e5d54fb22b8ed95575");
        writer.close();

        File characterFile = PlayerSaveOffline.getCharacterFile(oldFilesLocation, oldPlayer.getLoginName().toUpperCase());
        Preconditions.checkState(characterFile != null, "Character file is null.");
        String retrievedPassword = PlayerSaveOffline.getPassword(characterFile);
        assertTrue(PlayerSaveOffline.passwordMatches(PASSWORD_PLAIN_TEXT, retrievedPassword));
    }

    @Test
    @Order(2)
    public void reclaimed_is_false_before_claiming() throws Exception {
        Boolean claimed = execReclaim(new ReclaimCheckQuery(oldPlayer.getLoginName().toUpperCase()));
        assertFalse(claimed);
    }

    @Test
    public void no_account_found_or_password_incorrect() throws Exception {
        assertTrue(execReclaim(new ReclaimQuery(newPlayer, oldPlayer.getLoginName(), "incorrect")).getResponse() == ReclaimDonationResponse.Response.INVALID_PASSWORD);
        assertTrue(execReclaim(new ReclaimQuery(newPlayer,"ted turners big bonanza", "incorrect")).getResponse() == ReclaimDonationResponse.Response.NO_CHARACTER_FILE_WITH_NAME);
    }

    @Test
    @Order(2)
    public ReclaimDonationResponse reclaim_response_returns_useful_data() throws Exception {
        ReclaimDonationResponse response = execReclaim(new ReclaimQuery(newPlayer, oldPlayer.getLoginName().toUpperCase(), PASSWORD_PLAIN_TEXT));
        assertTrue(response.getAmountDonated() > 0);
        assertTrue(response.getPoints() == response.getAmountDonated());
        assertTrue(response.getResponse() == ReclaimDonationResponse.Response.SUCCESS);
        return response;
    }

    @Test
    @Order(3)
    public void reclaim_not_repeatable() throws Exception {
        Boolean success = execReclaim(new ReclaimSuccessQuery(oldPlayer.getLoginName(), newPlayer.getLoginName(), 55));
        assertTrue(success);

        assertTrue(execReclaim(new ReclaimQuery(newPlayer, oldPlayer.getLoginName(), PASSWORD_PLAIN_TEXT)).getResponse() == ReclaimDonationResponse.Response.ALREADY_CLAIMED);
        assertTrue(execReclaim(new ReclaimQuery(newPlayer, oldPlayer.getLoginName().toUpperCase(), PASSWORD_PLAIN_TEXT)).getResponse() == ReclaimDonationResponse.Response.ALREADY_CLAIMED);
        assertTrue(execReclaim(new ReclaimQuery(newPlayerAlt, oldPlayer.getLoginName().toUpperCase(), PASSWORD_PLAIN_TEXT)).getResponse() == ReclaimDonationResponse.Response.ALREADY_CLAIMED);
    }

    @AfterAll
    @BeforeAll
    public static void clear() throws Exception {
        execReclaim(((context, connection) -> {
            connection.createStatement().executeUpdate("DELETE FROM reclaimed_donations");
            return null;
        }));
    }
}