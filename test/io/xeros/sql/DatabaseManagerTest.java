package io.xeros.sql;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import io.xeros.Server;
import io.xeros.ServerState;
import io.xeros.test.ServerTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DatabaseManagerTest {

    private static final DatabaseCredentials configuration = new EmbeddedDatabase("manager_test");
    private static final String TABLE_NAME = "TEST_TABLE";
    private static String[] INSERTS = {
            "INSERT INTO " + TABLE_NAME + "(name, age) VALUES('michael', 25)",
            "INSERT INTO " + TABLE_NAME + "(name, age) VALUES('noah', 24)",
            "INSERT INTO " + TABLE_NAME + "(name, age) VALUES('aaron', 22)",
            "INSERT INTO " + TABLE_NAME + "(name, age) VALUES('jesus', 100)",
    };

    private DatabaseTable getTestTable() {
        return new DatabaseTable() {
            @Override
            public String getName() {
                return TABLE_NAME;
            }

            @Override
            public void createTable(Connection connection) throws SQLException {
                connection.createStatement().execute(
                        "CREATE TABLE " + getName() + "("
                        + "name VARCHAR (255),"
                        + "age INT NOT NULL"
                        + ")"
                );
            }
        };
    }

    @Test
    public void test_database() throws Exception {
        DatabaseManager manager = new DatabaseManager(true);

        DatabaseTable testTable = getTestTable();
        manager.executeImmediate(configuration, (context, connection) -> {
            if (!manager.isTablePresent(testTable, connection)) {
                testTable.createTable(connection);
            }

            Statement statement = connection.createStatement();
            for (String insert : INSERTS) {
                statement.execute(insert);
            }

            ResultSet rs = statement.executeQuery("SELECT * FROM " + testTable.getName()
                    + " WHERE name='michael'");

            while (rs.next()) {
                assertEquals("michael", rs.getString("name"));
                System.out.println(rs.getString("name"));
            }

            return null;
        });
    }

    @Test
    public void test_exec_with_exception_handling() throws ExecutionException, InterruptedException {
        DatabaseManager manager = new DatabaseManager(true);
        DatabaseResult<Object> result = manager.exec(configuration, (context, connection) -> {
            throw new Exception("test");
        }).get();

        assertNotNull(result.getException());
        assertNull(result.getResult());
        assertTrue(result.hasErrorOccurred());

        DatabaseResult<Integer> noException = manager.exec(configuration, (context, connection) -> 5).get();
        assertNull(noException.getException());
        assertNotNull(noException.getResult());
        assertFalse(noException.hasErrorOccurred());
        assertEquals(5, noException.getResult());
    }

    @Test
    public void test_executeImmediate() throws Exception {
        DatabaseManager manager = new DatabaseManager(true);
        assertThrows(Exception.class, () -> manager.executeImmediate(configuration, (context, connection) -> { throw new Exception(); }));
        int result = manager.executeImmediate(configuration, (context, connection) -> 66);
        assertEquals(66, result);
    }

    @Test
    public void consumer_called_on_future_finished() {
        DatabaseManager manager = new DatabaseManager(true);
        manager.exec(configuration, (context, connection) -> 5, result -> assertEquals(5, result.getResult()));
    }

    @Test
    public void too_many_connections() {
        ServerTest serverTest = new ServerTest(ServerState.PUBLIC);
        for (int i = 0; i < 20_000; i++)
            Server.getDatabaseManager().exec((context, connection) -> {
                connection.createStatement().execute("SELECT * FROM leaderboards");
                return null;
            });
    }
}