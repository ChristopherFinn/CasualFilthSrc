package io.xeros.content.combat;

import java.util.Optional;

import io.xeros.content.combat.specials.Special;
import io.xeros.content.combat.weapon.RangedWeaponType;
import io.xeros.model.CombatType;
import io.xeros.model.entity.Entity;

/**
 * 
 * @author Jason MacKeigan
 * @date Nov 6, 2014, 1:01:03 PM
 */
public class Damage {

	@Override
	public String toString() {
		return "Damage{" +
				"special=" + special +
				", amount=" + amount +
				", ticks=" + ticks +
				", target=" + target +
				", timestamp=" + timestamp +
				", success=" + success +
				'}';
	}

	/**
	 * The special attack assigned to this damage
	 */
	private Special special;

	/**
	 * The type of combat this damage is relative to
	 */
	private CombatType combatType;

	private RangedWeaponType rangedWeaponType = RangedWeaponType.SHOT;

	/**
	 * The equipment worn by the player
	 */
	private Optional<int[]> equipment;

	/**
	 * The amount of damage caused by the weapon
	 */
	private int amount;

	/**
	 * The damage object will be stored in a queue. Every 600ms, or every game tick, the number of ticks will be reduced by 1 until the ticks is 0
	 */
	private int ticks;

	/**
	 * The target that this damage will be inflicted upon
	 */
	private Entity target;

	/**
	 * The time that the damage was created
	 */
	private final long timestamp;

	/**
	 * The hitmark for this damage
	 */
	private Hitmark hitmark;

	private boolean success;

	/**
	 * Creates a new damage object with a specific amount registered
	 *
	 * @param amount the amount of damage
	 */
	public Damage(int amount) {
		success = amount > 0;
		this.amount = amount;
		this.timestamp = System.currentTimeMillis();
	}

	/**
	 * Creates a new {@link Damage} object that will be added to the damage queue for the target
	 *
	 * @param target the target being dealt the damage
	 * @param amount the amount of damage being dealt
	 * @param ticks the amount of ticks until the damage is applied
	 * @param hitmark the type of hitmark that is displayed
	 * @param combatType the special attack
	 */
	public Damage(Entity target, int amount, int ticks, int[] equipment, Hitmark hitmark, CombatType combatType) {
		this(amount);
		if (ticks < 0) {
			throw new IllegalStateException("The number of ticks for the damage is negative.");
		}
		success = amount > 0;
		this.target = target;
		this.ticks = ticks;
		this.equipment = Optional.of(equipment);
		this.hitmark = hitmark;
		this.combatType = combatType;
	}

	/**
	 * Creates a new {@link Damage} object that will be added to the damage queue for the target
	 *
	 * @param target the target being dealt the damage
	 * @param amount the amount of damage being dealt
	 * @param ticks the amount of ticks until the damage is applied
	 * @param hitmark the type of hitmark that is displayed
	 */
	public Damage(Entity target, int amount, int ticks, int[] equipment, Hitmark hitmark, CombatType combatType, RangedWeaponType rangedWeaponType,
				  Special special, boolean success) {
		this(target, amount, ticks, equipment, hitmark, combatType);
		this.special = special;
		this.success = success;
		this.rangedWeaponType = rangedWeaponType;
	}

	/**
	 * Modifies the amount of damage being dealt to the target
	 *
	 * @param amount the new amount of damage
	 * @return the damage to be dealt
	 */
	public int setAmount(int amount) {
		return this.amount = amount;
	}

	/**
	 * Returns the amount of damage
	 *
	 * @return the amount of damage
	 */
	public int getAmount() {
		return amount;
	}

	/**
	 * The target of the damage attack
	 *
	 * @return the target
	 */
	public Entity getTarget() {
		return target;
	}

	/**
	 * The number of ticks this damage is alive for
	 *
	 * @return the ticks
	 */
	public int getTicks() {
		return ticks;
	}

	/**
	 * Removes a single tick from the total number of ticks
	 */
	public void removeTick() {
		ticks--;
	}

	/**
	 * The time that the damage was created
	 *
	 * @return the time the damage was created
	 */
	public long getTimestamp() {
		return timestamp;
	}

	/**
	 * The hitmark for this damage
	 *
	 * @return the hitmark
	 */
	public Hitmark getHitmark() {
		return hitmark;
	}

	/**
	 * The type of combat this damage is relative to
	 *
	 * @return the type of damage
	 */
	public CombatType getCombatType() {
		return combatType;
	}

	/**
	 * The equipment warn by the attacker
	 *
	 * @return the equipment;
	 */
	public Optional<int[]> getEquipment() {
		return equipment;
	}

	/**
	 * The special attack assigned to this damage
	 *
	 * @return the special attack
	 */
	public Special getSpecial() {
		return special;
	}

	public boolean isSuccess() {
		return success;
	}

	public RangedWeaponType getRangedWeaponType() {
		return rangedWeaponType == null ? RangedWeaponType.SHOT : rangedWeaponType;
	}
}
