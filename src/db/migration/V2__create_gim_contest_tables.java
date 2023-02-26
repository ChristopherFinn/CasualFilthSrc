package db.migration;

import io.xeros.sql.gim.GimContestPlayerTable;
import io.xeros.sql.gim.GimContestTotalTable;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import java.sql.SQLException;

public class V2__create_gim_contest_tables extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws SQLException {
        new GimContestTotalTable().createTable(context.getConnection());
        new GimContestPlayerTable().createTable(context.getConnection());
    }

}
