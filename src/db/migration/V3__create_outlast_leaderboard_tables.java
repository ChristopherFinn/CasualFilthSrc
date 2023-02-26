package db.migration;

import io.xeros.sql.outlast.OutlastLeaderboardTable;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

public class V3__create_outlast_leaderboard_tables extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {
        new OutlastLeaderboardTable().createTable(context.getConnection());
    }

}
