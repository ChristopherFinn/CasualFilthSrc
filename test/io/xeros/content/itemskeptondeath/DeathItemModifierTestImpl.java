package io.xeros.content.itemskeptondeath;

import io.xeros.model.entity.player.Player;
import io.xeros.model.items.GameItem;

import java.util.List;
import java.util.Set;

public class DeathItemModifierTestImpl implements DeathItemModifier {

    @Override
    public Set<Integer> getItemIds() {
        return Set.of(1);
    }

    @Override
    public void modify(Player player, GameItem gameItem, boolean kept, List<GameItem> keptItems, List<GameItem> lostItems) {
        lostItems.removeIf(it -> it.getId() == gameItem.getId());
        lostItems.add(new GameItem(2));
    }
}