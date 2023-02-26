package io.xeros.util;

import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import io.xeros.content.commands.owner.Npc;
import io.xeros.model.Npcs;
import io.xeros.model.definitions.NpcDef;
import io.xeros.model.definitions.NpcStats;
import io.xeros.model.entity.npc.NPCSpawning;
import io.xeros.model.entity.npc.stats.NpcCombatDefinition;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * @author Arthur Behesnilian 11:51 AM
 */
public class CheckMissingNpcDefs {

    public static void main(String[] args) throws IOException {

        ArrayList<String> missing = new ArrayList<>();
        NpcStats.load();
        NpcCombatDefinition.load();
        NpcDef.load();

        NpcStats.getNpcStatsMap().forEach((i, stats) -> {
            if (!NpcCombatDefinition.definitions.containsKey(i)) {
                NpcDef def = NpcDef.forId(i);
                missing.add(def.getName() + " - " + i);
            }
        });

        System.out.println("Missing " + missing.size());
        for (String s : missing) {
            System.out.println(s);
        }
    }

}
