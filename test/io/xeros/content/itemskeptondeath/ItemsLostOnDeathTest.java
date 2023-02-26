package io.xeros.content.itemskeptondeath;

import io.xeros.ServerState;
import io.xeros.content.combat.melee.CombatPrayer;
import io.xeros.model.Items;
import io.xeros.model.definitions.ItemDef;
import io.xeros.model.definitions.ItemStats;
import io.xeros.model.entity.player.Player;
import io.xeros.model.items.GameItem;
import io.xeros.test.ServerTest;
import io.xeros.test.TestPlayer;
import org.junit.jupiter.api.*;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ItemsLostOnDeathTest {

    private static Player player;
    private static final ServerTest TEST = new ServerTest(ServerState.PUBLIC);

    @BeforeEach
    public void setupPlayer() {
        player = TestPlayer.named("test");
    }

    public void withInventory(List<GameItem> inventory) {
        inventory.forEach(item -> assertTrue(player.getItems().addItem(item.getId(), item.getAmount())));
        assertEquals(inventory.size(), player.getItems().getInventoryItems().size());
    }

    public void withEquipment(List<GameItem> equipment) {
        equipment.forEach(item -> {
            ItemStats def = ItemStats.forId(item.getId());
            player.getItems().setEquipment(item.getId(), item.getAmount(), def.getEquipment().getSlot(), true);
        });
        assertEquals(equipment.size(), player.getItems().getEquipmentItems().size());
    }

    public void items(List<GameItem> inventory, List<GameItem> equipment) {
        withInventory(inventory);
        withEquipment(equipment);
    }

    public void skulled(boolean skulled) {
        player.isSkulled = skulled;
        player.skullTimer = skulled ? 99999 : 0;
    }

    public void protectItem(boolean active) {
        player.prayerActive[CombatPrayer.PROTECT_ITEM] = active;
    }

    public void wildLevel(int wildLevel) {
        player.wildLevel = wildLevel;
    }

    public List<GameItem> lost() {
        List<GameItem> lost = ItemsLostOnDeath.generate(player).getLost();
        assertNotNull(lost);
        return lost;
    }

    public List<GameItem> kept() {
        List<GameItem> kept = ItemsLostOnDeath.generate(player).getKept();
        assertNotNull(kept);
        return kept;
    }

    @Test
    public void item_stack_goes_by_shop_price() {
        items(
                List.of(new GameItem(4151, 1),
                        new GameItem(4153, 1),
                        new GameItem(4153, 1),
                        new GameItem(2, 11)),
                List.of()
        );

        DeathItemStack stack = new DeathItemStack();
        stack.create(player);

        int last = -1;
        while (!stack.getValuedItemStack().isEmpty()) {
            GameItem item = stack.getValuedItemStack().pop();
            if (last != -1) {
                assertTrue(item.getDef().getShopValue() <= last);
            }
            last = item.getDef().getShopValue();
        }
    }

    @Test
    public void all_items_lost_when_skulled() {
        items(
                List.of(new GameItem(4151, 1)),
                List.of(new GameItem(4151, 1))
        );

        skulled(true);

        assertEquals(player.getItems().getHeldItems(), lost());
        assertEquals(List.of(), kept());
    }

    @Test
    public void three_items_kept_when_not_skulled() {
        items(
                List.of(new GameItem(4157, 1), new GameItem(4153, 1), new GameItem(4155, 1)),
                List.of(new GameItem(4151, 1))
        );

        assertEquals(3, kept().size());
        assertEquals(1, lost().size());
    }

    @Test
    public void stackables_split_for_kept_items() {
        items(
                List.of(new GameItem(Items.COINS, 555)),
                List.of(new GameItem(Items.RUNE_ARROW, 100))
        );

        assertEquals(1, kept().size());
        assertEquals(2, lost().size());
        assertEquals(kept().get(0).getAmount(), 3);
        assertEquals(lost().get(0).getAmount(), 97);
        assertEquals(lost().get(1).getAmount(), 555);
    }

    @Test
    public void most_valuable_items_are_kept() {
        items(
                List.of(new GameItem(Items.SCYTHE_OF_VITUR), new GameItem(Items.SCYTHE_OF_VITUR),new GameItem(Items.SCYTHE_OF_VITUR), new GameItem(Items.SHARK, 1), new GameItem(Items.SHARK, 1)),
                List.of(new GameItem(Items.SCYTHE_OF_VITUR), new GameItem(Items.RUNE_ARROW, 100))
        );

        assertEquals(3, kept().size());
        assertEquals(4, lost().size());
        assertEquals(3, kept().stream().filter(it -> it.getId() == Items.SCYTHE_OF_VITUR).count());
        assertEquals(1, lost().stream().filter(it -> it.getId() == Items.SCYTHE_OF_VITUR).count());
        assertEquals(1, lost().stream().filter(it -> it.getId() == Items.RUNE_ARROW).count());
        assertEquals(2, lost().stream().filter(it -> it.getId() == Items.SHARK).count());
    }

    @Test
    public void keep_extra_item_when_using_protect_item() {
        items(
                List.of(new GameItem(Items.MONKFISH, 1)),
                List.of(new GameItem(Items.ABYSSAL_WHIP))
        );

        skulled(true);
        protectItem(true);

        assertEquals(1, kept().size());
        assertEquals(1, lost().size());
        assertEquals(Items.ABYSSAL_WHIP, kept().get(0).getId());
    }

    @Test
    public void lost_items_are_modified() {
        assertNotNull(DeathItemModifiers.get(1));
        items(List.of(new GameItem(1)), List.of());

        skulled(true);
        ItemsLostOnDeathList items = ItemsLostOnDeath.generateModified(player);
        assertEquals(1, items.getLost().size());
        assertEquals(2, items.getLost().get(0).getId());
    }

    @Test
    public void untradeables_are_lost_over_20_wild() {
        wildLevel(21);

        assertFalse(ItemDef.forId(Items.FIGHTER_TORSO).isTradable());
        items(
                List.of(new GameItem(Items.FIGHTER_TORSO)),
                List.of()
        );

        assertTrue(kept().isEmpty());
        assertEquals(1, lost().size());
        assertTrue(lost().stream().anyMatch(it -> it.getId() == Items.FIGHTER_TORSO));
    }
}