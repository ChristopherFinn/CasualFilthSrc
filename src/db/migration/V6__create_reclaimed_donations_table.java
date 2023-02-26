package db.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

public class V6__create_reclaimed_donations_table extends BaseJavaMigration {
    @Override
    public void migrate(Context context) throws Exception {
        context.getConnection().createStatement().execute("CREATE TABLE reclaimed_donations (claimed VARCHAR(255), claimed_by VARCHAR(255), claimed_amount INT NOT NULL, date DATE)");
        context.getConnection().createStatement().execute("CREATE INDEX idx_username on reclaimed_donations (claimed)");
    }
}
