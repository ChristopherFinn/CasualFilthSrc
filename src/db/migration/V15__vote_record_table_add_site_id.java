package db.migration;

import io.xeros.sql.voterecord.VoteRecordTable;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

/**
 * @author Chris | 8/14/21
 */
public class V15__vote_record_table_add_site_id extends BaseJavaMigration {
    @Override
    public void migrate(Context context) throws Exception {
        context.getConnection().createStatement().execute("ALTER TABLE vote_record ADD site_id INT NOT NULL DEFAULT 0");

        // Add primary key with site id
        context.getConnection().createStatement().execute("ALTER TABLE vote_record DROP PRIMARY KEY");
        context.getConnection().createStatement().execute("ALTER TABLE vote_record ADD PRIMARY KEY (ip_address, mac_address, uuid, site_id)");

        context.getConnection().createStatement().execute( "CREATE INDEX vote_record_ip_address_index ON vote_record (ip_address)");
        context.getConnection().createStatement().execute( "CREATE INDEX vote_record_mac_address_index ON vote_record (mac_address)");
        context.getConnection().createStatement().execute( "CREATE INDEX vote_record_uuid_index ON vote_record (uuid)");
        context.getConnection().createStatement().execute( "CREATE INDEX vote_record_date_claimed ON vote_record (date_claimed)");
    }
}
