package io.xeros.content.item.lootable.unref;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import io.xeros.model.cycleevent.CycleEvent;
import io.xeros.model.cycleevent.CycleEventContainer;
import io.xeros.model.cycleevent.CycleEventHandler;
import io.xeros.model.entity.player.Player;
import io.xeros.model.items.GameItem;
import io.xeros.util.Misc;

/**
 * Revamped a simple means of receiving a random item based on chance.
 * 
 * @author Jason MacKeigan
 * @date Oct 29, 2014, 1:43:44 PM
 */
public class CoinBagMedium extends CycleEvent {

	/**
	 * The item id of the PvM Casket required to trigger the event
	 */
	public static final int MYSTERY_BOX = 10833; //Casket

	/**
	 * A map containing a List of {@link GameItem}'s that contain items relevant to their rarity.
	 */
	private static final Map<Rarity, List<GameItem>> items = new HashMap<>();

	/**
	 * Stores an array of items into each map with the corresponding rarity to the list
	 */
	static {
		items.put(Rarity.COMMON, 
			Arrays.asList(
					new GameItem(995, 33000))
	);
			
		items.put(Rarity.UNCOMMON,
				Arrays.asList(
						new GameItem(995, 45000))
	);
			
			items.put(Rarity.RARE,
					Arrays.asList(
							new GameItem(995, 60000))
	);
							
							
	}

	/**
	 * The player object that will be triggering this event
	 */
	private final Player player;

	/**
	 * Constructs a new PvM Casket to handle item receiving for this player and this player alone
	 * 
	 * @param player the player
	 */
	public CoinBagMedium(Player player) {
		this.player = player;
	}

	/**
	 * Opens a PvM Casket if possible, and ultimately triggers and event, if possible.
	 *
	 */
	public void openall() {
		if (player.isDisconnected() || Objects.isNull(player)) {
			return;
		}
		int coins = 35000 + Misc.random(20000);
		int coinsDouble = 200000 + Misc.random(50000);
		int random = Misc.random(100);
		List<GameItem> itemList = random < 55 ? items.get(Rarity.COMMON) : random >= 55 && random <= 80 ? items.get(Rarity.UNCOMMON) : items.get(Rarity.RARE);
		GameItem item = Misc.getRandomItem(itemList);
		GameItem itemDouble = Misc.getRandomItem(itemList);
		int amount = player.getItems().getItemAmount(10833);
		if (Misc.random(1000) == 1) {
			int rewardAllGpAmount = (coins + coinsDouble* amount) + (item.getAmount()*amount) + (itemDouble.getAmount() * amount);
			player.getItems().addItem(995, rewardAllGpAmount);
			player.sendMessage("@red@You dig deeper and find a hidden pocket of " + Misc.formatCoins(rewardAllGpAmount) + " coins!");
			player.getItems().deleteItem(10833, amount);
		} else {
			int rewardAllGpAmount = (coins*amount) + (item.getAmount()*amount);
			player.getItems().deleteItem(10833, amount);
			player.getItems().addItem(995, coins * amount);
			player.getItems().addItem(item.getId(), item.getAmount()* amount);
			player.sendMessage("You receive " + Misc.formatCoins(rewardAllGpAmount) + " coins!");
		}
		
	}
	public void open() {
		if (System.currentTimeMillis() - player.lastMysteryBox < 1200) {
			return;
		}
		if (player.getItems().freeSlots() < 1) {
			player.sendMessage("You need at least one free slots to open a Coin Bag.");
			return;
		}
		if (!player.getItems().playerHasItem(MYSTERY_BOX)) {
			player.sendMessage("You need Coin Bag to do this.");
			return;
		}
		player.getItems().deleteItem(MYSTERY_BOX, 1);
		player.lastMysteryBox = System.currentTimeMillis();
		CycleEventHandler.getSingleton().stopEvents(this);
		CycleEventHandler.getSingleton().addEvent(this, this, 2);
	}

	/**
	 * Executes the event for receiving the mystery box
	 */
	@Override
	public void execute(CycleEventContainer container) {
		if (player.isDisconnected() || Objects.isNull(player)) {
			container.stop();
			return;
		}
		int coins = 35000 + Misc.random(20000);
		int coinsDouble = 200000 + Misc.random(50000);
		int random = Misc.random(100);
		List<GameItem> itemList = random < 55 ? items.get(Rarity.COMMON) : random >= 55 && random <= 80 ? items.get(Rarity.UNCOMMON) : items.get(Rarity.RARE);
		GameItem item = Misc.getRandomItem(itemList);
		GameItem itemDouble = Misc.getRandomItem(itemList);

		if (Misc.random(1000) == 1) {
			int rewardAmount = (coins+coinsDouble + item.getAmount()+itemDouble.getAmount());
			player.getItems().addItem(995, rewardAmount);
			player.sendMessage("@red@You dig deeper and find a hidden pocket of coins!");
			player.sendMessage("You receive " + Misc.formatCoins(rewardAmount) + " coins!");
		} else {
			int rewardAmount = coins + item.getAmount();
			player.getItems().addItem(995, rewardAmount);
			player.sendMessage("You receive " + Misc.formatCoins(rewardAmount) + " coins!");
		}

		container.stop();
	}

	/**
	 * Represents the rarity of a certain list of items
	 */
	enum Rarity {
		UNCOMMON, COMMON, RARE
	}

}