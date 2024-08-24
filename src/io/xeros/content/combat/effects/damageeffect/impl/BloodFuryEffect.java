package io.xeros.content.combat.effects.damageeffect.impl;

import java.util.Optional;

import io.xeros.Configuration;
import io.xeros.content.combat.Damage;
import io.xeros.content.combat.effects.damageeffect.DamageEffect;
import io.xeros.content.combat.range.RangeData;
import io.xeros.model.entity.HealthStatus;
import io.xeros.model.entity.npc.NPC;
import io.xeros.model.entity.player.Player;
import io.xeros.util.Misc;

public class BloodFuryEffect implements DamageEffect {

    @Override
    public void execute(Player attacker, Player defender, Damage damage) {
        int change = Misc.random((int) (damage.getAmount() * 1.25));
        damage.setAmount(change);
        RangeData.createCombatGraphic(defender, 753, false);
        attacker.getHealth().increase(change / 4);
        attacker.getPA().refreshSkill(3);
    }

    @Override
    public void execute(Player attacker, NPC defender, Damage damage) {
        if (defender.getDefinition().getName() == null) {
            return;
        }
        attacker.getHealth().increase(damage.getAmount() / 3);
        attacker.getPA().refreshSkill(3);
    }

    @Override
    public boolean isExecutable(Player operator) {
        return operator.getItems().isWearingItem(24780) && Misc.random(4) == 0;

    }

}


