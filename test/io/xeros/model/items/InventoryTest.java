package io.xeros.model.items;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import io.xeros.Server;
import io.xeros.model.Items;
import io.xeros.model.definitions.ItemDef;
import io.xeros.model.entity.player.Player;
import io.xeros.model.items.bank.BankTab;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class InventoryTest {

    private final Player player = new Player(null);
    private final Inventory inv = player.getInventory();
    private static final ImmutableItem UNSTACKABLE_ITEM = new ImmutableItem(Items.ABYSSAL_WHIP);
    private static final ImmutableItem STACKABLE_ITEM = new ImmutableItem(Items.FEATHER);

    @BeforeAll
    static void setup() throws Exception {
        ItemDef.load();
        Server.loadConfiguration();
    }

    @BeforeEach
    void reset() {
        player.getItems().deleteAllItems();
        player.getBank().deleteAllItems();
    }

    @Test
    void testHasRoomFor() {
        assertFalse(inv.hasRoomInInventory(UNSTACKABLE_ITEM.withAmount(29)));
        assertTrue(inv.hasRoomInInventory(UNSTACKABLE_ITEM.withAmount(28)));
        assertTrue(inv.hasRoomInInventory(STACKABLE_ITEM.withAmount(29)));
        assertTrue(inv.hasRoomInInventory(STACKABLE_ITEM.withAmount(Integer.MAX_VALUE)));
    }

    @Test
    void testFreeSlots() {
        assertEquals(28, inv.freeInventorySlots());
        assertEquals(0, inv.freeInventorySlot());

        assertTrue(inv.addToInventory(UNSTACKABLE_ITEM));
        assertEquals(27, inv.freeInventorySlots());
        assertEquals(1, inv.freeInventorySlot());

        assertTrue(inv.addToInventory(UNSTACKABLE_ITEM.withAmount(26)));
        assertEquals(1, inv.freeInventorySlots());
        assertEquals(27, inv.freeInventorySlot());

        assertTrue(inv.addToInventory(UNSTACKABLE_ITEM.withAmount(1)));
        assertEquals(0, inv.freeInventorySlots());
        assertEquals(-1, inv.freeInventorySlot());
    }

    @Test
    void testAddItem() {
        assertFalse(inv.addToInventory(UNSTACKABLE_ITEM.withAmount(29)));
        assertTrue(inv.addToInventory(UNSTACKABLE_ITEM.withAmount(26)));
        assertTrue(inv.addToInventory(STACKABLE_ITEM.withAmount(2_000_000_000)));
        assertFalse(inv.addToInventory(STACKABLE_ITEM.withAmount(500_000_000)));
        assertFalse(inv.addToInventory(UNSTACKABLE_ITEM.withAmount(2)));
        assertTrue(inv.addToInventory(UNSTACKABLE_ITEM.withAmount(1)));
        assertFalse(inv.addToInventory(UNSTACKABLE_ITEM.withAmount(1)));
    }

    @Test
    void testStacking() {
        player.getInventory().addToBank(UNSTACKABLE_ITEM);
        player.getInventory().addToBank(new ImmutableItem(ItemDef.forId(UNSTACKABLE_ITEM.getId()).getNoteId(), UNSTACKABLE_ITEM.getAmount()));
        assertEquals(1, player.getBank().getItemCount());
    }

    @Test
    void testAddToBank() {
        List<ItemDef> collect = ItemDef.getDefinitions().values().stream().filter(def -> !def.isNoted())
                .limit(player.getBank().getBankCapacity()).collect(Collectors.toList());
        int itemCount = 0;
        for (ItemDef def : collect) {
            System.out.println(itemCount++);
            assertTrue(inv.addToBank(new ImmutableItem(def.getId(), 1)));
        }

        for (ItemDef def : collect) {
            assertTrue(inv.addToBank(new ImmutableItem(def.getId(), 1)));
        }

        for (ItemDef def : collect) {
            assertTrue(inv.addToBank(new ImmutableItem(def.getId(), 1)));
        }

        for (ItemDef def : collect) {
            assertTrue(inv.addToBank(new ImmutableItem(def.getNoteId() != 0 ? def.getNoteId() : def.getId(), 1)));
        }

        for (ItemDef def : collect) {
            assertTrue(inv.addToBank(new ImmutableItem(def.getId(), Integer.MAX_VALUE - 4)));
        }

        for (ItemDef def : collect) {
            assertFalse(inv.addToBank(new ImmutableItem(def.getId(), 1)));
        }

        assertFalse(Arrays.stream(player.getBank().getBankTab()).anyMatch(bank -> bank.getItems().stream().anyMatch(item -> item.getId() == 30_000)));
        assertFalse(inv.addToBank(new ImmutableItem(30_000, Integer.MAX_VALUE)));

        assertEquals(player.getBank().getBankCapacity(), player.getBank().getItemCount());
        for (BankTab bankTab : player.getBank().getBankTab()) {
            assertEquals(0, bankTab.freeSlots());
        }
    }

}