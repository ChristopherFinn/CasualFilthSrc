package io.xeros.model.entity.npc.stats;

import com.google.gson.Gson;
import io.xeros.Server;
import io.xeros.content.combat.weapon.AttackStyle;
import io.xeros.content.commands.owner.Npc;
import io.xeros.model.entity.npc.NPC;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Arthur Behesnilian 2:17 PM
 */
public class NpcCombatDefinition {

    /**
     * A Map of all Npc Combat Definitions
     */
    public static HashMap<Integer, NpcCombatDefinition> definitions = new HashMap<>();

    /**
     * Creates a generic NpcCombatDefinition
     * @param npc The NPC to create a definition for
     */
    public NpcCombatDefinition(NPC npc) {
        this.id = npc.getNpcId();
        this.levels = new HashMap<>();
        this.attackBonuses = new HashMap<>();
        this.defensiveBonuses = new HashMap<>();

        this.attackSpeed = 4;
        this.attackStyle = "Melee";
        this.aggressive = false;
        this.isPoisonous = false;
        this.isImmuneToPoison = false;
        this.isImmuneToVenom = false;
        this.isImmuneToCannons = false;
        this.isImmuneToThralls = false;
        for (NpcCombatSkill value : NpcCombatSkill.values()) {
            this.levels.put(value, 1);
        }
        for (NpcBonus value : NpcBonus.values()) {
            this.attackBonuses.put(value, 0);
            this.defensiveBonuses.put(value, 0);
        }
    }

    /**
     * A deep copy of the Npc Combat definitions
     * @param other
     */
    public NpcCombatDefinition(NpcCombatDefinition other) {
        this.id = other.id;
        this.levels = new HashMap<>();
        this.attackBonuses = new HashMap<>();
        this.defensiveBonuses = new HashMap<>();

        this.attackSpeed = other.attackSpeed;
        this.attackStyle = other.attackStyle;
        this.aggressive = other.aggressive;
        this.isPoisonous = other.isPoisonous;
        this.isImmuneToPoison = other.isImmuneToPoison;
        this.isImmuneToVenom = other.isImmuneToVenom;
        this.isImmuneToCannons = other.isImmuneToCannons;
        this.isImmuneToThralls = other.isImmuneToThralls;
        this.levels.putAll(other.levels);
        this.attackBonuses.putAll(other.attackBonuses);
        this.defensiveBonuses.putAll(other.defensiveBonuses);
    }

    public static void load() throws IOException {
        Reader reader = Files.newBufferedReader(Paths.get(Server.getDataDirectory() + "/cfg/npc/npc_combat_defs.json"));
        NpcCombatDefinition[] npcCombatDefinitions = new Gson().fromJson(reader, NpcCombatDefinition[].class);
        for (NpcCombatDefinition npcCombatDefinition : npcCombatDefinitions) {
            if (npcCombatDefinition != null)
                definitions.put(npcCombatDefinition.id, npcCombatDefinition);
        }
        System.out.println("Loaded " + definitions.size() + " NPC Combat definitions...");
        reader.close();
    }

    /**
     * The ID of the NPC
     */
    private int id;

    /**
     * The Attack speed of the NPC
     */
    private int attackSpeed;

    /**
     * The Attack style of the NPC
     */
    private String attackStyle;

    /**
     * A flag that determines if the NPC is aggressive
     */
    private boolean aggressive;
    /**
     * A flag that determines if the NPC is isPoisonous
     */
    private boolean isPoisonous;
    /**
     * A flag that determines if the NPC is isImmuneToPoison
     */
    private boolean isImmuneToPoison;
    /**
     * A flag that determines if the NPC is isImmuneToVenom
     */
    private boolean isImmuneToVenom;
    /**
     * A flag that determines if the NPC is isImmuneToPoison
     */
    private boolean isImmuneToCannons;
    /**
     * A flag that determines if the NPC is isImmuneToVenom
     */
    private boolean isImmuneToThralls;
    /**
     * All the NPCs combat levels
     */
    private Map<NpcCombatSkill, Integer> levels;
    /**
     * All the NPC's attack bonuses
     */
    private Map<NpcBonus, Integer> attackBonuses;
    /**
     * All the NPc's defensive bonuses
     */
    private Map<NpcBonus, Integer> defensiveBonuses;

    public NpcCombatDefinition(int id) {
        this.id = id;
        this.levels = new HashMap<>();
        this.attackBonuses = new HashMap<>();
        this.defensiveBonuses = new HashMap<>();
    }

    public int getId() {
        return this.id;
    }

    public void setLevel(NpcCombatSkill npcCombatSkill, int level) {
        this.levels.put(npcCombatSkill, level);
    }

    public void setAttackBonus(NpcBonus npcAttackBonus, int bonus) {
        this.attackBonuses.put(npcAttackBonus, bonus);
    }

    public void setDefenceBonus(NpcBonus npcDefenceBonus, int bonus) {
        this.defensiveBonuses.put(npcDefenceBonus, bonus);
    }

    public void setAttackSpeed(int attackSpeed) {
        this.attackSpeed = attackSpeed;
    }

    public void setAttackStyle(String attackStyle) {
        this.attackStyle = attackStyle;
    }

    public void setAggressive(boolean aggressive) {
        this.aggressive = aggressive;
    }

    public void setPoisonous(boolean isPoisonous) {
        this.isPoisonous = isPoisonous;
    }

    public void setImmuneToPoison(boolean isImmuneToPoison) {
        this.isImmuneToPoison = isImmuneToPoison;
    }

    public void setImmuneToVenom(boolean isImmuneToVenom) {
        this.isImmuneToVenom = isImmuneToVenom;
    }

    public boolean isImmuneToPoison() {
        return this.isImmuneToPoison;
    }

    public boolean isImmuneToVenom() {
        return this.isImmuneToVenom;
    }

    public boolean isAggressive() {
        return this.aggressive;
    }

    public boolean isPoisonous() {
        return this.isPoisonous;
    }

    public String getAttackStyle() {
        return this.attackStyle;
    }

    public Map<NpcCombatSkill, Integer> getLevels() {
        return this.levels;
    }

    public Map<NpcBonus, Integer> getAttackBonuses() {
        return this.attackBonuses;
    }

    public Map<NpcBonus, Integer> getDefenceBonuses() {
        return this.defensiveBonuses;
    }

    public int getLevel(NpcCombatSkill npcCombatSkill) {
        return this.levels.getOrDefault(npcCombatSkill, 1);
    }

    public int getAttackBonus(NpcBonus npcBonus) {
        return this.attackBonuses.getOrDefault(npcBonus, 1);
    }

    public int getDefenceBonus(NpcBonus npcBonus) {
        return this.defensiveBonuses.getOrDefault(npcBonus, 1);
    }

    public int getAttackSpeed() {
        return this.attackSpeed;
    }

    public boolean isImmuneToCannons() {
        return isImmuneToCannons;
    }

    public void setImmuneToCannons(boolean immuneToCannons) {
        isImmuneToCannons = immuneToCannons;
    }

    public boolean isImmuneToThralls() {
        return isImmuneToThralls;
    }

    public void setImmuneToThralls(boolean immuneToThralls) {
        isImmuneToThralls = immuneToThralls;
    }

}
