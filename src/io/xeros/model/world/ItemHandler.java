package io.xeros.model.world;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

import io.xeros.Configuration;
import io.xeros.Server;
import io.xeros.content.instances.InstancedArea;
import io.xeros.content.lootbag.LootingBag;
import io.xeros.content.skills.Skill;
import io.xeros.content.skills.prayer.Bone;
import io.xeros.content.skills.prayer.Prayer;
import io.xeros.model.definitions.ItemDef;
import io.xeros.model.entity.player.Boundary;
import io.xeros.model.entity.player.Player;
import io.xeros.model.entity.player.PlayerHandler;
import io.xeros.model.entity.player.Position;
import io.xeros.model.items.GameItem;
import io.xeros.model.items.GroundItem;
import io.xeros.model.shops.ShopAssistant;
import io.xeros.util.Misc;
import io.xeros.util.logging.player.ItemPickupLog;
import io.xeros.util.discord.Discord;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ItemHandler {

	private static final Logger logger = LoggerFactory.getLogger(ItemHandler.class);

	/**
	 * A list of all {@link GroundItem}'s in the game.
	 */
	private final List<GroundItem> items = new LinkedList<>();

	/**
	 * The amount of ticks before the game item become visible for everyone (globalisation).
	 */
	public static final int GLOBALISATION_TICKS = Misc.toCycles(1, TimeUnit.MINUTES);

	public int getGlobalisationTicks() {
		return Server.isDebug() ? 10 : GLOBALISATION_TICKS;
	}

	/**
	 * The amount of ticks, after becoming globalised, if applicable, will be visible.
	 * The total time that a ground item will be registered in the game is calculated as
	 * {@link ItemHandler#GLOBALISATION_TICKS} plus this field.
	 */
	public static final int REMOVE_TICKS = Misc.toCycles(2, TimeUnit.MINUTES);

	public ItemHandler() { }

	public GroundItem getGroundItem(Player player, int itemId, int x, int y, int height) {
		Predicate<GroundItem> exists = (GroundItem i) -> i.getId() == itemId && i.getX() == x && i.getY() == y && i.getHeight() == height;
		if (player != null) {
			exists = exists.and((GroundItem i) -> i.getInstance() == player.getInstance() && i.isViewable(player));
		}

		return items.stream().filter(exists).findFirst().orElse(null);
	}

	/**
	 * Item amount
	 **/
	public int itemAmount(String player, int itemId, int itemX, int itemY, int height, InstancedArea instance) {
		for (GroundItem i : items) {
			if (i.globalisationTicks < 1 || player.equalsIgnoreCase(i.getOwnerName())) {
				if (i.getId() == itemId && i.getX() == itemX && i.getY() == itemY && i.getHeight() == height && i.getInstance() == instance) {
					return i.getAmount();
				}
			}
		}
		return 0;
	}

	/**
	 * Item exists
	 **/
	public boolean itemExists(Player player, int itemId, int itemX, int itemY, int height) {
		return getGroundItem(player, itemId, itemX, itemY, height) != null;
	}

	public void reloadItems(Player player) {
		if (player.getMode() == null) {
			return;
		}

		player.getLocalGroundItems().stream().filter(it -> it.inDistance(player) /* optimization because others are removed by client */)
				.forEach(it -> player.getItems().removeGroundItem(it, false));
		player.getLocalGroundItems().clear();

		items.stream().filter(item -> item.isVisible(player)).forEach(item -> player.getItems().createGroundItem(item));
	}

	public void process() {
		Iterator<GroundItem> it = items.iterator();
		while (it.hasNext()) {
			GroundItem i = it.next();
			if (i == null) {
				it.remove();
				continue;
			}
			if (i.getInstance() != null && i.getInstance().isDisposed()) {
				i.removeTicks = 0;
				sendRemovedGroundItem(i);
				it.remove();
				logger.debug("Removed ground item because instance disposed {}", i);
				continue;
			}
			if (i.globalisationTicks > 0) {
				i.globalisationTicks--;
			}
			if (i.globalisationTicks == 1) {
				i.globalisationTicks = 0;
				if (i.canGlobalise()) {
					PlayerHandler.stream().filter(Objects::nonNull).filter(player -> i.isVisible(player) && !i.isOwner(player) /* because owner already sees it */)
							.forEach(plr -> plr.getItems().createGroundItem(i));
					logger.debug("Globalised ground item: " + i);
				} else {
					logger.debug("Ground item can't be globalised: " + i);
				}
				i.removeTicks = REMOVE_TICKS;
			}
			if (i.removeTicks > 0) {
				i.removeTicks--;
			}
			if (i.removeTicks == 1) {
				i.removeTicks = 0;
				sendRemovedGroundItem(i);
				it.remove();
				logger.debug("Removed ground item: " + i);
			}
		}
	}

	public void createUnownedGroundItem(GameItem gameItem, Position position) {
		createUnownedGroundItem(gameItem.getId(), position.getX(), position.getY(), position.getHeight(), gameItem.getAmount());
	}

	public void createUnownedGroundItem(int id, int x, int y, int height, int amount) {
		if (id > 0 && amount > 0) {
			if (id >= 2412 && id <= 2414) {
				return;
			}
			if (!ItemDef.forId(id).isStackable() && amount > 0) {
				if (amount > 28) {
					amount = 28;
				}
				for (int j = 0; j < amount; j++) {
					GroundItem item = new GroundItem(id, x, y, height, 1, getGlobalisationTicks(), "");
					items.add(item);
					logger.debug("Added ground item: " + item);
				}
			} else {
				GroundItem item = new GroundItem(id, x, y, height, amount, getGlobalisationTicks(), "");
				items.add(item);
				logger.debug("Added ground item: " + item);
			}
		}
	}

	public void createGroundItemFromDrop(Player player, int itemId, int itemX, int itemY, int height, int itemAmount, int playerId) {
		boolean newPlayer = player.hasNewPlayerRestriction();
		boolean inWild = Boundary.isIn(player, Boundary.WILDERNESS_PARAMETERS);
		boolean global = !player.hasNewPlayerRestriction();
		createGroundItem(player, itemId, itemX, itemY, height, itemAmount, playerId, global, inWild ? 3 : getGlobalisationTicks());
		if (newPlayer) {
			player.sendMessage("The dropped item won't become global because you haven't played for " + Configuration.NEW_PLAYER_RESTRICT_TIME_MIN + " minutes.");
		}
	}

	public void createGroundItem(Player player, GameItem gameItem, Position position, int hideTicks) {
		createGroundItem(player, gameItem.getId(), position.getX(), position.getY(), position.getHeight(), gameItem.getAmount(), player.getIndex(), true, hideTicks);
	}

	public void createGroundItem(Player player, GameItem gameItem, Position position) {
		createGroundItem(player, gameItem.getId(), position.getX(), position.getY(), position.getHeight(), gameItem.getAmount(), player.getIndex());
	}

	public void createGroundItem(Player player, int itemId, int itemX, int itemY, int height, int itemAmount, int playerId) {
		createGroundItem(player, itemId, itemX, itemY, height, itemAmount, playerId, true, getGlobalisationTicks());
	}

	/**
	 * @param globalise If the item should become globalised after the timer.
	 */
	public void createGroundItem(Player player, int itemId, int itemX, int itemY, int height, int itemAmount, int playerId, boolean globalise, int hideTicks) {
		if (playerId < 0 || playerId > PlayerHandler.players.length - 1) {
			return;
		}
		Player owner = PlayerHandler.players[playerId];
		if (owner == null) {
			return;
		}
		if (itemId > 0 && itemAmount > 0) {
			/**
			 * Lootvalue
			 */
			if (player.lootValue > 0) {
				if (ShopAssistant.getItemShopValue(itemId) >= player.lootValue) {
					player.getPA().stillGfx(1177, itemX, itemY, height, 5);
					player.sendMessage("@red@Your lootvalue senses a drop valued at or over "
							+ Misc.getValueWithoutRepresentation(player.lootValue) + " coins.");
				}
			}
			/**
			 * Bone crusher
			 */
			boolean crusher = player.getItems().playerHasItem(13116) || player.playerEquipment[Player.playerAmulet] == 22986 ;
			
			Optional<Bone> boneOptional = Prayer.isOperableBone(itemId);
			if (crusher && boneOptional.isPresent()) {
				Bone bone = boneOptional.get();

				double experience = player.getRechargeItems().hasItem(13114) ? 0.75 : player.getRechargeItems().hasItem(13115) ? 1 : player.getDiaryManager().getMorytaniaDiary().hasCompleted("ELITE") ?  1 : 0.50 ;
				if (itemId == bone.getItemId()) {
					player.getPrayer().onBonesBuriedOrCrushed(bone, true);
					player.getPA().addSkillXPMultiplied((int) (bone.getExperience() * experience),
							Skill.PRAYER.getId(), true);
					return;
				}
			}

			if (!ItemDef.forId(itemId).isStackable()) {
				if (itemAmount > 28) {
					itemAmount = 28;
				}
				for (int j = 0; j < itemAmount; j++) {
					GroundItem item = new GroundItem(itemId, itemX, itemY, height, 1, hideTicks, owner.getLoginNameLower());
					player.getItems().createGroundItem(item);
					item.setGlobalise(globalise);
					item.setInstance(player.getInstance());
					items.add(item);
					logger.debug("Added ground item: " + item);
				}
			} else {
				GroundItem current = getGroundItem(player, itemId, itemX, itemY, height);
				int existingAmount = 0;
				if (current != null) {
					existingAmount += current.getAmount();
					player.getItems().removeGroundItem(current, true);
					removeGroundItem(player, current, false);
				}
				int newAmount = (int) Math.min(Integer.MAX_VALUE, (long) itemAmount + existingAmount);
				if (newAmount <= 0) {
					return;
				}
				GroundItem item = new GroundItem(itemId, itemX, itemY, height, newAmount, hideTicks, owner.getLoginNameLower());
				player.getItems().createGroundItem(item);
				item.setGlobalise(globalise);
				item.setInstance(player.getInstance());
				items.add(item);
				logger.debug("Added ground item: " + item);
			}
		}
	}


	public void createGroundItem(Player player, int itemId, int itemX, int itemY, int height, int itemAmount) {
		if (itemId > 0 && itemAmount > 0) {
			if (!ItemDef.forId(itemId).isStackable()) {
				if (itemAmount > 28) {
					itemAmount = 28;
				}
				for (int j = 0; j < itemAmount; j++) {
					GroundItem item = new GroundItem(itemId, itemX, itemY, height, 1, getGlobalisationTicks(), player.getLoginNameLower());
					player.getItems().createGroundItem(item);
					item.setInstance(player.getInstance());
					items.add(item);
					logger.debug("Added ground item: " + item);
				}
			} else {
				if (itemId == 11849 && Boundary.isIn(player, Boundary.ROOFTOP_COURSES)) {
					player.getItems().addItemUnderAnyCircumstance(11849, 1);
				} else {
					GroundItem current = getGroundItem(player, itemId, itemX, itemY, height);
					int existingAmount = 0;
					if (current != null) {
						existingAmount += current.getAmount();
						player.getItems().removeGroundItem(current, true);
						removeGroundItem(player, current, false);
					}
					int newAmount = (int) Math.min(Integer.MAX_VALUE, (long) itemAmount + existingAmount);
					if (newAmount <= 0) {
						return;
					}
					GroundItem item = new GroundItem(itemId, itemX, itemY, height, newAmount, getGlobalisationTicks(), player.getLoginNameLower());
					player.getItems().createGroundItem(item);
					item.setInstance(player.getInstance());
					items.add(item);
					logger.debug("Added ground item: " + item);
				}
			}
		}
	}

	/**
	 * Removing the ground item
	 **/

	public void removeGroundItem(Player c, int itemId, int itemX, int itemY, int height, boolean add) {
		removeGroundItem(c, getGroundItem(c, itemId, itemX, itemY, height), add);
	}

	public void removeGroundItem(Player c, GroundItem i, boolean add) {
		if (i == null)
			return;

		if (!add) {
			items.remove(i);
			return;
		}

		if (c.getPosition().inWild() && c.getItems().playerHasItem(LootingBag.LOOTING_BAG_OPEN)
				&& c.getLootingBag().getLootingBagContainer().deposit(i.getId(), i.getAmount(), false)) {
			logPickup(c, i);
			sendRemovedGroundItem(i);
			items.remove(i);
			return;
		}

		if (c.getItems().hasRoomInInventory(i.getId(), i.getAmount())) {
			c.getItems().addItem(i.getId(), i.getAmount());
			logPickup(c, i);
			sendRemovedGroundItem(i);
			items.remove(i);
		}
	}

	private void logPickup(Player player, GroundItem groundItem) {
		ItemDef def = ItemDef.forId(groundItem.getId());
		Server.getLogging().write(new ItemPickupLog(player, new GameItem(groundItem.getId(), groundItem.getAmount()), groundItem.getPosition(), groundItem.getOwnerName()));
		if (def.getShopValue() > 30_000 || def.getShopValue() < 2) {
			Discord.writePickupMessage("[Pickup]: " + player.getDisplayName() +
					" picked up " + def.getName() +
					" x " + groundItem.getAmount() +
					" at " + groundItem.getPosition() +
					" from " + groundItem.getOwnerName());
		}
	}

	private void sendRemovedGroundItem(GroundItem groundItem) {
		PlayerHandler.nonNullStream().forEach(plr -> {
			if (plr.getLocalGroundItems().contains(groundItem)) {
				plr.getItems().removeGroundItem(groundItem);
			}
		});
	}

	/**
	 * The counterpart of the item whether it is the noted or un noted version
	 * 
	 * @param itemId the item id we're finding the counterpart of
	 * @return the note or unnoted version or -1 if none exists
	 */
	public int getCounterpart(int itemId) {
		return ItemDef.forId(itemId).getNoteId() == 0 ? -1 : ItemDef.forId(itemId).getNoteId();
	}

}
