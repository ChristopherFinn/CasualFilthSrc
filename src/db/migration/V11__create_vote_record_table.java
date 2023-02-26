package db.migration;

import io.xeros.sql.voterecord.VoteRecordTable;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

/**
 * @author Chris | 8/14/21
 */
public class V11__create_vote_record_table extends BaseJavaMigration {
    @Override
    public void migrate(Context context) throws Exception {
        new VoteRecordTable().createTable(context.getConnection());
    }
}
