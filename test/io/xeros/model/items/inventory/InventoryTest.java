package io.xeros.model.items.inventory;

import io.xeros.ServerState;
import io.xeros.model.Items;
import io.xeros.model.items.GameItem;
import io.xeros.test.ServerTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class InventoryTest {

    private static final ServerTest serverTest = new ServerTest(ServerState.PUBLIC);
    private Inventory inventory;

    @BeforeEach
    void beforeEach() {
        inventory = new Inventory(28, Inventory.StackMode.STACK_STACKABLE_ITEMS);;
    }

    @Test
    void hasSpaceFor_unstackables() {
        assertFalse(inventory.hasSpaceFor(new GameItem(Items.ABYSSAL_WHIP, 29)));
        assertTrue(inventory.hasSpaceFor(new GameItem(Items.ABYSSAL_WHIP, 27)));
        assertTrue(inventory.hasSpaceFor(new GameItem(Items.ABYSSAL_WHIP, 28)));

        inventory.add(new GameItem(4151, 27));
        assertTrue(inventory.hasSpaceFor(new GameItem(Items.ABYSSAL_WHIP, 1)));
        assertFalse(inventory.hasSpaceFor(new GameItem(Items.ABYSSAL_WHIP, 2)));
    }

    @Test
    void hasSpaceFor_stackable() {
        assertTrue(inventory.hasSpaceFor(new GameItem(Items.COINS, Integer.MAX_VALUE)));

        inventory.add(new GameItem(Items.COINS, Integer.MAX_VALUE - 1));
        assertTrue(inventory.hasSpaceFor(new GameItem(Items.COINS, 1)));
        assertFalse(inventory.hasSpaceFor(new GameItem(Items.COINS, 2)));
    }

}