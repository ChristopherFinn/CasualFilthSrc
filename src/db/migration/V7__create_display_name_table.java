package db.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

public class V7__create_display_name_table extends BaseJavaMigration {
    @Override
    public void migrate(Context context) throws Exception {
        context.getConnection().createStatement().execute("CREATE TABLE display_names (" +
                "login_name VARCHAR(255) NOT NULL UNIQUE," +
                " display_name VARCHAR(255)," +
                " display_name_lower VARCHAR(255) NOT NULL UNIQUE," +

                " INDEX idx_login_name (login_name)," +
                " INDEX idx_display_name_lower (display_name_lower)," +

                " constraint check_lowercase_login_name check (BINARY login_name = LOWER(login_name))," +
                " constraint check_lowercase_display_name_lower check (BINARY display_name_lower = LOWER(display_name_lower))" +
                ")");
    }
}
