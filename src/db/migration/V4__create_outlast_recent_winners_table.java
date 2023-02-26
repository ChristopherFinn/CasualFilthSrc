package db.migration;

import io.xeros.sql.outlast.OutlastRecentWinnersTable;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

public class V4__create_outlast_recent_winners_table extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {
        new OutlastRecentWinnersTable().createTable(context.getConnection());
    }

}
