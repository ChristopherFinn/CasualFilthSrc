package io.xeros.model.entity.player;

import io.xeros.ServerState;
import io.xeros.model.Items;
import io.xeros.model.items.GameItem;
import io.xeros.model.items.ItemAssistant;
import io.xeros.test.ServerTest;
import io.xeros.test.TestPlayer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class InventoryTest {

    private Player player;
    private ItemAssistant items;
    private static final ServerTest serverTest = new ServerTest(ServerState.PUBLIC);
    private static final int customMaxStack = 20;

    @BeforeEach
    void setup() {
        player = TestPlayer.named("test");
        items = player.getItems();
    }

    @Test
    public void addItem__add_unstackable_item_works() {
        assertTrue(items.addItem(Items.ABYSSAL_WHIP, 3));
        assertEquals(3, items.getItemAmount(Items.ABYSSAL_WHIP));
        assertEquals(25, items.freeSlots());

        assertFalse(items.addItem(Items.ABYSSAL_WHIP, 26));
        assertEquals(3, items.getItemAmount(Items.ABYSSAL_WHIP));
        assertEquals(25, items.freeSlots());
    }

    @Test
    void addItem__add_stackable_works() {
        assertTrue(items.addItem(Items.FIRE_RUNE, 10_000));
        assertEquals(10_000, items.getItemAmount(Items.FIRE_RUNE));
        assertEquals(27, items.freeSlots());

        assertTrue(items.addItem(Items.FIRE_RUNE, 1));
        assertEquals(10_001, items.getItemAmount(Items.FIRE_RUNE));
        assertEquals(27, items.freeSlots());

        assertFalse(items.addItem(Items.FIRE_RUNE, Integer.MAX_VALUE - 9));
        assertEquals(10_001, items.getItemAmount(Items.FIRE_RUNE));
        assertEquals(27, items.freeSlots());
    }

    @Test
    public void addItem__dont_delete_max_stack_overflow() {
        assertTrue(items.addItem(Items.COINS, Integer.MAX_VALUE - 5));
        assertEquals(Integer.MAX_VALUE - 5, items.getItemAmount(Items.COINS));

        assertTrue(items.addItem(Items.COINS, 5));
        assertEquals(Integer.MAX_VALUE, items.getItemAmount(Items.COINS));

        assertFalse(items.addItem(Items.COINS, 1));
        assertEquals(Integer.MAX_VALUE, items.getItemAmount(Items.COINS));
    }

    @Test
    public void addItemUntilFull__add_unstackables_without_filling() {
        assertEquals(Optional.empty(), items.addItemUntilFull(new GameItem(Items.ABYSSAL_WHIP, 15)));
    }

    @Test
    public void addItemUntilFull__add_stackables_without_filling() {
        assertEquals(Optional.empty(), items.addItemUntilFull(new GameItem(Items.COINS, 15)));
    }


    @Test
    public void addItemUntilFullReverse__add_unstackables_without_filling() {
        assertEquals(Optional.of(new GameItem(Items.ABYSSAL_WHIP, 15)), items.addItemUntilFullReverse(new GameItem(Items.ABYSSAL_WHIP, 15)));
    }

    @Test
    public void addItemUntilFullReverse__add_stackables_without_filling() {
        assertEquals(Optional.of(new GameItem(Items.COINS, 15)), items.addItemUntilFullReverse(new GameItem(Items.COINS, 15)));
    }

    @Test
    public void addItemUntilFull__add_unstackables_until_full() {
        assertEquals(Optional.of(new GameItem(Items.ABYSSAL_WHIP, 1)), items.addItemUntilFull(new GameItem(Items.ABYSSAL_WHIP, 29)));
    }

    @Test
    public void addItemUntilFullReverse__add_unstackables_until_full() {
        assertEquals(Optional.of(new GameItem(Items.ABYSSAL_WHIP, 28)), items.addItemUntilFullReverse(new GameItem(Items.ABYSSAL_WHIP, 29)));
    }

    @Test
    public void addItemUntilFull__add_stackables_until_full() {
        assertTrue(items.addItem(Items.COINS, Integer.MAX_VALUE - 10));

        assertEquals(Optional.of(new GameItem(Items.COINS, 1)), items.addItemUntilFull(new GameItem(Items.COINS, 11)));
    }

    @Test
    public void addItemUntilFullReverse__add_stackables_until_full() {
        assertTrue(items.addItem(Items.COINS, Integer.MAX_VALUE - 10));

        assertEquals(Optional.of(new GameItem(Items.COINS, 10)), items.addItemUntilFullReverse(new GameItem(Items.COINS, 11)));
    }

    @Test
    public void addItemUntilFull__add_stackables_until_full_with_custom_maxStack() {
        assertTrue(items.addItem(Items.COINS, customMaxStack - 10));

        assertEquals(Optional.of(new GameItem(Items.COINS, 1)), items.addItemUntilFull(new GameItem(Items.COINS, 11), customMaxStack, true));
    }

    @Test
    public void addItemUntilFullReverse__add_stackables_until_full_with_custom_maxStack() {
        assertTrue(items.addItem(Items.COINS, customMaxStack - 10));

        assertEquals(Optional.of(new GameItem(Items.COINS, 10)), items.addItemUntilFullReverse(new GameItem(Items.COINS, 11), customMaxStack, true));
    }
}































