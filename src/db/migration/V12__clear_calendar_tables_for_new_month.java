package db.migration;

import io.xeros.sql.DatabaseTable;
import io.xeros.sql.eventcalendar.tables.EventCalendarBlacklistTable;
import io.xeros.sql.eventcalendar.tables.EventCalendarParticipantsTable;
import io.xeros.sql.eventcalendar.tables.EventCalendarWinnersTable;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import java.sql.SQLException;
import java.util.List;

public class V12__clear_calendar_tables_for_new_month extends BaseJavaMigration {
    @Override
    public void migrate(Context context) {
        List<DatabaseTable> tables = List.of(new EventCalendarWinnersTable(), new EventCalendarParticipantsTable(), new EventCalendarBlacklistTable());
        tables.forEach(it -> {
            try {
                context.getConnection().createStatement().execute("DELETE FROM " + it.getName());
            } catch (SQLException throwables) {
                throwables.printStackTrace();
            }
        });
    }
}
