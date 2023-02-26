package db.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

public class V14__leaderboards_add_composite_key extends BaseJavaMigration {
    @Override
    public void migrate(Context context) throws Exception {
        context.getConnection().createStatement().execute("ALTER TABLE leaderboards add primary key(username, type, date)");
        context.getConnection().createStatement().execute("DROP INDEX idx_amount ON leaderboards");
    }
}
