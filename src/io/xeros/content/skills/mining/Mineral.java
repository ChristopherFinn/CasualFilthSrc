package io.xeros.content.skills.mining;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

import io.xeros.util.Misc;
import org.apache.commons.lang3.RandomUtils;

/**
 * An enumeration of ore vein information.
 * 
 * @author Jason MacKeigan
 * @date Feb 18, 2015, 5:14:50 PM
 */
public enum Mineral {
			ENCHANTED_STONE(new int[] { 33223 }, "none", 50, 5, 1000, 1, 15, 15000, true, generateExclusive(23778)),
			BURNING_ORE(new int[] { 33222 }, "none", 50, 5, 10000, 1, 15, 15000, true, generateExclusive(9698)),
			CLAY(new int[] { 7487, 7454, 11363 }, "none", 1, 18, 0, 3, 15, 30000, true, generateExclusive(434)),
			COPPER(new int[] { 7484, 11961, 11960, 11962, 13709, 11161 }, "bronze", 1, 18, 5, 5, 15, 15000, true, generateExclusive(436)), 
			TIN(new int[] { 7485, 9714, 9716, 11957, 11958, 11959, 13712, 11360, 11361 }, "bronze", 1, 18, 5, 5, 15, 15000, true, generateExclusive(438)),
			IRON(new int[] { 7488, 11954, 11955, 11956, 13710, 13444, 13445, 13446, 7455, 11364, 11365 }, "iron", 15, 35, 5, 8, 17, 14800, true, generateExclusive(440)),
			COAL(new int[] { 7489, 9717, 9718, 9719, 2096, 13714, 13706, 11366, 11367 }, "none", 30, 50, 5, 15, 29, 14600, true, generateExclusive(453)),
			GOLD(new int[] { 7491, 7458, 9722, 9720, 13707, 14962, 14963, 14964, 13441, 13442, 13443, 8728, 8975, 11370 }, "gold", 40, 65, 5, 25, 32, 14200, true, generateExclusive(444)),
			MITHRIL(new int[] { 11373, 13718, 7459, 11373, 11372 }, "mithril", 55, 80, 5, 40, 35, 13800, true, generateExclusive(447)),
			ADAMANT(new int[] { 13720, 7460, 11375, 11374 }, "adamant", 70, 95, 5, 35, 35, 13200, true, generateExclusive(449)),
			RUNE(new int[] { 11376, 7461, 14175, 11376, 11377 }, "rune", 85, 125, 5, 35, 35, 3600, true, generateExclusive(451)),
			ESSENCE(new int[] { 7471, 14912, 34773 }, "none", 30, 5, -1, -1, 5, 50000, false, generateExclusive(7936)),
			AMETHYST(new int[] { 30371, 30372, 11389, 11388 }, "none", 92, 140, 0, 100, 40, 11500, false, generateExclusive(21347)),			
			GEM(new int[] { 9030 }, "none", 40, 5, 20, 25, 20, 15000, true, new MineralReturn() {

				@Override
				public int generate() {
					int[] inclusives = inclusives();
					int percent = RandomUtils.nextInt(0, 10000);
					
					return percent < 2800 ? Misc.randomSearch(inclusives, 0, 2) :
						   percent >= 2800 && percent < 5200 ? inclusives[2] :
						   percent >= 5200 && percent < 7500 ? inclusives[3] : 
						   percent >= 7500 && percent < 8700 ? inclusives[4] : 
						   percent >= 8700 && percent < 9700 ? inclusives[5] : 
						   percent >= 9700 && percent < 9996 ? inclusives[6] :
					       percent >= 9996 && percent < 10000 ? inclusives[7] :
						   inclusives[inclusives.length - 1];
				}

				@Override
				public int[] inclusives() {
					return new int[] { 1625, 1627, 1623, 1621, 1619, 1617, 1631, 6571 };
				}//                    0      1     2     3   4ruby     5    6      7

			});

	/**
	 * An array of object ids that are associated with the mineral obtained from them
	 */
	private final int[] objectIds;
	
	/**
	 * A string of the bar name being created
	 */
	private final String barName;

	/**
	 * The level required to mine this ore
	 */
	private final int level;

	/**
	 * The experience gained from mining this mineral
	 */
	private final double experience;

	/**
	 * The probability that the mineral will deplete
	 */
	private final int depletionProbability;

	/**
	 * The amount of cycles that need to pass before the mineral is extractable
	 */
	private final int respawnRate;

	/**
	 * The default amount of cycles it takes to extract ore from a vein
	 */
	private final int extractionRate;

	/**
	 * The default amount of chance to receive a pet from mining
	 */
	private final int petChance;

	/**
	 * Determines if this mineral depletes
	 */
	private final boolean depletes;

	/**
	 * The mineral that is returned to the player as a result of mining the ore
	 */
	private final MineralReturn mineralReturn;

	/**
	 * Constructs a new mineral
	 * 
	 * @param objectIds the objects that the mineral can be extracted from
	 * @param level the level required to extract minerals from the object(s)
	 * @param experience the experience gain after extraction
	 * @param depletionProbability the probability in terms of a 1: {@link #depletionProbability} ratio.
	 * @param respawnRate the rate at which the mineral respawn's to the world
	 * @param extractionRate the rate at which the mineral is extracted
	 * @param depletes determine if the mineral depletes in the world
	 * @param mineralReturn the mineral that is returned to the player as a result of mining the resource
	 */
    Mineral(int[] objectIds, String barName, int level, double experience, int depletionProbability, int respawnRate, int extractionRate, int petChance, boolean depletes, MineralReturn mineralReturn) {
		this.objectIds = objectIds;
		this.barName = barName;
		this.level = level;
		this.experience = experience;
		this.depletionProbability = depletionProbability;
		this.respawnRate = respawnRate;
		this.extractionRate = extractionRate;
		this.petChance = petChance;
		this.depletes = depletes;
		this.mineralReturn = mineralReturn;
	}

	/**
	 * The array of objectId values associated with this mineral
	 * 
	 * @return the array of object id's
	 */
	public int[] getObjectIds() {
		return objectIds;
	}
	
	public String getBarName() {
		return barName;
	}

	/**
	 * The level required to extract minerals
	 * 
	 * @return the level required
	 */
	public int getLevel() {
		return level;
	}

	/**
	 * The experience gained from extracting a mineral
	 * 
	 * @return the experience gained
	 */
	public double getExperience() {
		return experience;
	}

	/**
	 * The rate, in cycles, the mineral respawns at
	 * 
	 * @return the rate at which the mineral respawns
	 */
	public int getRespawnRate() {
		return respawnRate;
	}

	/**
	 * Every mineral has a difference rate at which they deplete. Some minerals faster then others, and some minerals instantly after being extracted from.
	 * 
	 * @return the probability of depletion. When the probability is 0 the chance of depletion is 1:1.
	 */
	public int getDepletionProbability() {
		return depletionProbability;
	}

	/**
	 * The default amount of cycles it takes for a single ore to be extracted
	 * 
	 * @return the default extraction rate
	 */
	public int getExtractionRate() {
		return extractionRate;
	}

	/**
	 * The default amount of chance to receive a pet while mining
	 * 
	 * @return the default pet rate
	 */
	public int getPetChance() {
		return petChance;
	}

	/**
	 * Determines if the mineral depletes or not
	 * 
	 * @return true if object depletes
	 */
	public boolean isDepletable() {
		return depletes;
	}

	/**
	 * Accesses the {@link MineralReturn} instance's {@link MineralReturn#generate()} function.
	 * 
	 * @return An {@link Integer} value as the result of mining the mineral
	 */
	public MineralReturn getMineralReturn() {
		return mineralReturn;
	}

	/**
	 * The identification value of the object with no mineral remaining after extraction
	 */
	public static final int EMPTY_VEIN = 2835;

	/**
	 * An unmodifiable set of {@link Mineral} objects that will be used as a constant for obtaining information about certain minerals.
	 */
	private static final Set<Mineral> MINERALS = Collections.unmodifiableSet(EnumSet.allOf(Mineral.class));

	/**
	 * Retrieves the {@link Mineral} object with the same objectId as the parameter passed.
	 * 
	 * @param objectId the object id of the mineral
	 * @return the mineral object with the corresponding object id
	 */
	public static Mineral forObjectId(int objectId) {
		return MINERALS.stream().filter(mineral -> Arrays.stream(mineral.objectIds).anyMatch(id -> id == objectId)).findFirst().orElse(null);
	}

	/**
	 * Creates a {@link MineralReturn} object that always generates the same item.
	 * 
	 * @param id the item identification value
	 * @return an object that only returns the id specified
	 */
	private static MineralReturn generateExclusive(int id) {
		return new MineralReturn() {

			@Override
			public int generate() {
				return id;
			}

			@Override
			public int[] inclusives() {
				return new int[] { id };
			}

		};
	}

}
