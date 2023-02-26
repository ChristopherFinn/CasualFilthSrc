package io.xeros.model.entity.player.save.impl;

import java.util.List;

import com.google.common.collect.Lists;
import io.xeros.content.combat.magic.CombatSpellData;
import io.xeros.model.Spell;
import io.xeros.model.entity.player.Player;
import io.xeros.model.entity.player.packets.AutocastSpell;
import io.xeros.model.entity.player.save.PlayerSaveEntry;

public class AutocastPlayerSaveEntry implements PlayerSaveEntry {

    @Override
    public List<String> getKeys(Player player) {
        return Lists.newArrayList("autocast", "autocast_defensive", "autocast_id");
    }

    @Override
    public boolean decode(Player player, String key, String value) {
        switch (key) {
            case "autocast":
                player.autocasting = Boolean.parseBoolean(value);
                return true;
            case "autocast_defensive":
                player.autocastingDefensive = Boolean.parseBoolean(value);
                return true;
            case "autocast_id":
                player.autocastId = Integer.parseInt(value);
                return true;
            default:
                return false;
        }
    }

    @Override
    public String encode(Player player, String key) {
        switch (key) {
            case "autocast":
                return String.valueOf(player.autocasting);
            case "autocast_defensive":
                return String.valueOf(player.autocastingDefensive);
            case "autocast_id":
                return String.valueOf(player.autocastId);
            default:
                return null;
        }
    }

    @Override
    public void login(Player player) {
        if (player.autocasting && player.autocastId > -1) {
            Spell spell = Spell.forId(CombatSpellData.MAGIC_SPELLS[player.autocastId][0]);
            if (spell != null) {
                AutocastSpell.updateConfig(player, spell);
            }
        } else {
            player.autocasting = false;
            player.autocastingDefensive = false;
            player.autocastId = -1;
            player.getPA().sendFrame36(108, 0);
            player.getPA().sendFrame36(109, 0);
        }
    }
}
