package db.migration;

import io.xeros.model.entity.player.save.PlayerSave;
import io.xeros.util.Misc;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import java.io.File;
import java.sql.PreparedStatement;
import java.util.HashMap;
import java.util.Map;

public class V8__add_created_accounts_to_display_names extends BaseJavaMigration {
    @Override
    public void migrate(Context context) throws Exception {
        File[] saves = PlayerSave.getAllCharacterSaves();

        if (saves == null)
            return; // Don't need to migrate non-existent players

        Map<String, String> loginNameToDisplayNames = new HashMap<>();

        // We map to lowercase name here to avoid duplicate login names being inserted
        // For instance 'Test.txt' and 'test.txt' are too distinct files on linux, but not on windows
        for (File save : saves) {
            String name = Misc.capitalizeEveryWord(save.getName().replace(".txt", ""));
            String nameLower = name.toLowerCase();
            loginNameToDisplayNames.put(nameLower, name);
        }

        PreparedStatement insert = context.getConnection().prepareStatement("INSERT INTO display_names VALUES(?, ?, ?)");
        for (Map.Entry<String, String> entry : loginNameToDisplayNames.entrySet()) {
            String name = entry.getValue();
            String nameLower = entry.getKey();
            insert.setString(1, nameLower);
            insert.setString(2, name);
            insert.setString(3, nameLower);
            insert.addBatch();
        }

        insert.executeBatch();
    }
}
