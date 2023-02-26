package db.migration;

import io.xeros.sql.eventcalendar.tables.EventCalendarBlacklistTable;
import io.xeros.sql.eventcalendar.tables.EventCalendarParticipantsTable;
import io.xeros.sql.eventcalendar.tables.EventCalendarWinnersTable;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

public class V10__create_event_calendar_tables extends BaseJavaMigration {
    @Override
    public void migrate(Context context) throws Exception {
        new EventCalendarWinnersTable().createTable(context.getConnection());
        new EventCalendarParticipantsTable().createTable(context.getConnection());
        new EventCalendarBlacklistTable().createTable(context.getConnection());
    }
}
