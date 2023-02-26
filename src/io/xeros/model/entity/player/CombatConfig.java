package io.xeros.model.entity.player;

import io.xeros.Server;
import io.xeros.content.combat.weapon.WeaponData;
import io.xeros.content.combat.weapon.WeaponMode;
import io.xeros.model.items.ItemAssistant;

/**
 * @author Arthur Behesnilian 12:37 PM
 */
public class CombatConfig {

    /**
     * The Player bound to this configuration
     */
    private Player player;

    /**
     * The WeaponData for the Player's current weapon
     */
    private WeaponData weaponData;

    /**
     * The WeaponMode for the Player's current weapon
     */
    private WeaponMode weaponMode;

    /**
     * Determines the combat style
     * [Accurate, Aggressive, Etc...]
     */
    private int attackStyle;

    public CombatConfig(Player player) {
        this.player = player;
    }

    public int getAttackStyle() {
        return attackStyle;
    }

    public void setAttackStyle(int attackStyle) {
        this.attackStyle = attackStyle;
        this.updateWeapon();
    }

    public void updateWeapon() {
        int weaponId = player.getItems().getWeapon();

        WeaponData data = WeaponData.forItemId(weaponId);
        if (this.attackStyle >= data.getWeaponModes().length) {
            this.attackStyle = data.getWeaponModes().length - 1;
        }

        WeaponMode mode = data.getWeaponModes()[this.attackStyle];
        if (weaponData == null || weaponMode == null || !weaponData.equals(data) || !weaponMode.equals(mode)) {
            this.weaponMode = mode;
            this.weaponData = data;
            updateWeaponModeConfig();
            if (player.debugMessage) {
                player.sendMessage("Setting weapon mode: " + weaponData + ", " + weaponMode);
            }
        }
    }

    public void updateWeaponModeConfig() {
        int value = this.attackStyle;
        if (this.weaponData == WeaponData.SCYTHE) {
            value = value == 2 ? 1 : value == 1 ? 2 : value;
        }
        player.getPA().sendConfig(ItemAssistant.FIGHT_MODE_CONFIG, value);
    }

    public WeaponMode getWeaponMode() {
        return weaponMode;
    }

    public WeaponData getWeaponData() {
        return weaponData;
    }

}
