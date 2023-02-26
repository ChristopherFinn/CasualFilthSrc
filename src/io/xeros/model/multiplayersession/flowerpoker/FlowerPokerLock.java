package io.xeros.model.multiplayersession.flowerpoker;

import io.xeros.model.Items;
import io.xeros.model.entity.player.Player;
import io.xeros.model.entity.player.lock.CompleteLock;

public class FlowerPokerLock extends CompleteLock {
    @Override
    public boolean cannotClickItem(Player player, int itemId) {
        if (itemId == Items.MITHRIL_SEEDS)
            return false;
        return true;
    }
}
