package db.migration;

import com.fasterxml.jackson.core.type.TypeReference;
import io.xeros.Server;
import io.xeros.util.JsonUtil;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import java.io.File;
import java.sql.PreparedStatement;
import java.util.HashMap;
import java.util.Map;

public class V9__migrate_wogw_contributions_to_database extends BaseJavaMigration {
    @Override
    public void migrate(Context context) throws Exception {
        context.getConnection().createStatement().execute("CREATE TABLE wogw_total_contributions (" +
                "login_name VARCHAR(255) NOT NULL  PRIMARY KEY"  +
                ", total BIGINT NOT NULL" +
                ", INDEX idx_total (total)" +
                ", constraint wogw_total_contributions_login_name check (BINARY login_name = LOWER(login_name))" +
                ")"
        );

        context.getConnection().createStatement().execute("CREATE TABLE wogw_recent_contributions (" +
                "login_name VARCHAR(255) NOT NULL"  +
                ", total BIGINT NOT NULL" +
                ", date TIMESTAMP" +
                ", INDEX idx_date (date)" +
                ", constraint wogw_recent_contributions_login_name check (BINARY login_name = LOWER(login_name))" +
                ")"
        );

        // Not gonna transfer recent contributions
        String allContributions = Server.getSaveDirectory() + "wogw_all_contributions.json";
        if (new File(allContributions).exists()) {
            HashMap<String, WOGWContributionRecordLegacy> allContributionsMap = JsonUtil.fromJacksonJson(allContributions, new TypeReference<>() {});

            if (allContributionsMap != null && !allContributionsMap.isEmpty()) {

                // Insert all contributions loaded from json into database
                PreparedStatement insert = context.getConnection().prepareStatement("INSERT INTO wogw_total_contributions VALUES(?, ?)");
                for (Map.Entry<String, WOGWContributionRecordLegacy> entry : allContributionsMap.entrySet()) {
                    insert.setString(1, entry.getKey().toLowerCase());
                    insert.setLong(2, entry.getValue().getContribution());
                    insert.addBatch();
                }

                insert.executeBatch();
            }
        }
    }

    private static class WOGWContributionRecordLegacy {
        private final String username;
        private final long contribution;

        // For Jackson
        @SuppressWarnings({"unused", "RedundantSuppression" /* intellij lol */})
        WOGWContributionRecordLegacy() {
            username = null;
            contribution = 0;
        }

        public String getUsername() {
            return username;
        }

        public long getContribution() {
            return contribution;
        }
    }
}
