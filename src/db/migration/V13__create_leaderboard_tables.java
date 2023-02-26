package db.migration;

import io.xeros.sql.leaderboard.LeaderboardsCollectionBoxTable;
import io.xeros.sql.leaderboard.LeaderboardsRewardsTable;
import io.xeros.sql.leaderboard.LeaderboardsTable;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

public class V13__create_leaderboard_tables extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {
        new LeaderboardsTable().createTable(context.getConnection());
        new LeaderboardsRewardsTable().createTable(context.getConnection());
        new LeaderboardsCollectionBoxTable().createTable(context.getConnection());
    }

}
