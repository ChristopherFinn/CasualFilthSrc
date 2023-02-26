package db.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

public class V1__Example extends BaseJavaMigration {

    @Override
    public void migrate(Context context) {
        System.out.println("Running example database migration.");
    }

}
