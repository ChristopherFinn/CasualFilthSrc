package io.xeros.content.items.item_combinations;

import java.util.List;
import java.util.Optional;

import io.xeros.model.entity.player.Player;
import io.xeros.model.items.GameItem;
import io.xeros.model.items.ItemCombination;

public class EternalBoots extends ItemCombination {

	public EternalBoots(GameItem outcome, Optional<List<GameItem>> revertedItems, GameItem[] items) {
		super(outcome, revertedItems, items);
	}

	@Override
	public void combine(Player player) {
		if (player.playerLevel[6] < 60 || player.playerLevel[20] < 60) {
			player.sendMessage("You must have a magic and runecrafting level of at least 60 to do this.");
			return;
		}
		items.forEach(item -> player.getItems().deleteItem2(item.getId(), item.getAmount()));
		player.getItems().addItem(outcome.getId(), outcome.getAmount());
		player.startAnimation(6929);
		player.getDH().sendItemStatement("You combined the items and created a pair of eternal boots.", outcome.getId());
		player.setCurrentCombination(Optional.empty());
		player.nextChat = -1;
	}

	@Override
	public void showDialogue(Player player) {
		player.getDH().sendStatement("Combining these are final.", "You cannot revert this.");
	}

}
