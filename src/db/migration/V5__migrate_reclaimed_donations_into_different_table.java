package db.migration;

import io.xeros.Server;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * We had a donation reclaiming system that went live on July 11th, 2021. There was an issue
 * where it would insert the username of the person who claimed and not the old account that was claimed.
 * So we're transferring the names of individuals who used the claimed command into a new database.
 */
public class V5__migrate_reclaimed_donations_into_different_table extends BaseJavaMigration {
    @Override
    public void migrate(Context context) throws Exception {
        context.getConnection().createStatement().execute("CREATE TABLE reclaimed_donation_users (USERNAME VARCHAR(255), date DATE)");
        PreparedStatement trans = context.getConnection().prepareStatement("INSERT INTO reclaimed_donation_users (username, date) VALUES(?, ?)");
        Server.getDatabaseManager().exec(Server.getEmbeddedDatabase(), (_context, connection) -> {
            if (_context.isTablePresent("reclaimed_donations", connection)) {
                ResultSet rs = connection.createStatement().executeQuery("SELECT * FROM reclaimed_donations");
                while (rs.next()) {
                    String username = rs.getString("username");
                    Date date = rs.getDate("date");
                    trans.setString(1, username.toLowerCase());
                    trans.setDate(2, date);
                    trans.addBatch();
                }
                trans.executeBatch();
            }
            return null;
        }).get();
    }
}
