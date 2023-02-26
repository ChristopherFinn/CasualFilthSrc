package io.xeros.content.donationrewards;

import java.util.List;
import java.util.stream.Collectors;

import io.xeros.model.definitions.ItemDef;
import io.xeros.model.entity.player.Player;
import io.xeros.model.items.GameItem;
import io.xeros.model.items.ImmutableItem;
import io.xeros.util.SundayReset;

public class DonationRewards {

    private static final int INTERFACE_ID = 22693;
    private static final int DONATOR_REWARDS_CURRENT_PROGRESS = 1375;
    private static final int AMOUNT_DONATED_TEXT_ID = 22722;
    private static final int TIME_UNTIL_RESET_ID = 22696;
    private static final int ITEM_CONTAINER_ID = 22713;
    private static final int PRICING_ITEM_CONTAINER_ID = 22714;
    private static final int[] PRICE_TEXTS = {22699, 22701, 22703, 22705, 22707, 22709};
    private static final String TEXT_COLOUR = "<col=205209><img=1>";

    private final Player player;
    private SundayReset sundayReset = new SundayReset();
    private int amountDonatedThisWeek;

    public DonationRewards(Player player) {
        this.player = player;
    }

    public void tick() {
        if (sundayReset.isReset()) {
            sundayReset = new SundayReset();
            if (amountDonatedThisWeek > 0) {
                amountDonatedThisWeek = 0;
                player.sendMessage(TEXT_COLOUR + "Weekly donation rewards have reset.");
            }
        }
    }

    public void openInterface() {
        List<DonationReward> rewards = DonationReward.getRewardList();
        int lastItemPrice = rewards.get(5).getPrice();


        for (int index = 0; index < PRICE_TEXTS.length; index++) {
            player.getPA().sendString("$" + rewards.get(index).getPrice(), PRICE_TEXTS[index]);
        }

        player.getPA().sendString(AMOUNT_DONATED_TEXT_ID, "$" + amountDonatedThisWeek + " / $" + lastItemPrice);
        player.getPA().sendString(TIME_UNTIL_RESET_ID, "Week ends in: " + getSundayReset().getTimeUntilReset());
        player.getItems().sendItemContainer(ITEM_CONTAINER_ID, rewards.stream().map(DonationReward::getItem).collect(Collectors.toList()));

        // Container that sends the item prices to the client
        player.getItems().sendItemContainer(PRICING_ITEM_CONTAINER_ID, DonationReward.getRewardList().stream().map(reward ->
                new GameItem(reward.getItem().getId(), reward.getPrice())).collect(Collectors.toList()));

        player.getPA().sendConfig(DONATOR_REWARDS_CURRENT_PROGRESS, amountDonatedThisWeek);
        player.getPA().showInterface(INTERFACE_ID);
    }

    public void increaseDonationAmount(int amount) {
        if (amount > 0) {
            boolean gainedItem = false;
            for (DonationReward reward : DonationReward.getRewardList()) {
                if (reward.getPrice() > amountDonatedThisWeek && reward.getPrice() <= amountDonatedThisWeek + amount) {
                    player.getInventory().addAnywhere(new ImmutableItem(reward.getItem().getId(), reward.getItem().getAmount()));
                    player.sendMessage(TEXT_COLOUR + "You have received x" + reward.getItem().getAmount() + " " + ItemDef.forId(reward.getItem().getId()).getName() + " from donation rewards!");
                    gainedItem = true;

                    if (reward.getPrice() == DonationReward.getRewardList().get(5).getPrice()) {
                        player.sendMessage(TEXT_COLOUR + "You have completed the donation reward track for this week, thanks for donating!");
                    }
                }
            }

            amountDonatedThisWeek += amount;
            if (gainedItem || player.getOpenInterface() == INTERFACE_ID) {
                player.getDonationRewards().openInterface();
            }
        }
    }

    public SundayReset getSundayReset() {
        return sundayReset;
    }

    public void setSundayReset(SundayReset sundayReset) {
        this.sundayReset = sundayReset;
    }

    public int getAmountDonatedThisWeek() {
        return amountDonatedThisWeek;
    }

    public void setAmountDonatedThisWeek(int amountDonatedThisWeek) {
        this.amountDonatedThisWeek = amountDonatedThisWeek;
    }
}
