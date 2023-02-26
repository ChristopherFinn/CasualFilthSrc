package io.xeros.content.teleportation.inter;

import java.util.List;

import io.xeros.model.entity.player.Player;

import static io.xeros.content.teleportation.inter.TeleportInterfaceConstants.*;

public class TeleportInterface {

    private final Player player;
    private int menuSelectionIndex;
    private TeleportButton lastTeleport = null;

    public TeleportInterface(Player player) {
        this.player = player;
    }

    public void openInterface() {
        if (!player.getController().canMagicTeleport(player)) {
            player.sendMessage("You can't teleport right now.");
            return;
        }

        if (player.getPosition().inWild() && player.wildLevel > 20) {
            player.sendMessage("You can't teleport above level 20 in the wilderness.");
        } else {
            sendTeleportList();
            player.getPA().showInterface(INTERFACE_ID);
        }
    }

    private void sendTeleportList() {
        TeleportContainer teleportContainer = TELEPORT_CONTAINER_LIST.get(menuSelectionIndex);
        List<TeleportButton> buttonList = teleportContainer.getButtonList();
        for (int index = 0; index < buttonList.size(); index++) {
            player.getPA().sendString(buttonList.get(index).getName(), TELEPORT_MESSAGE_ID_LIST.get(index));
        }

        for (int index = buttonList.size(); index < TELEPORT_MESSAGE_ID_LIST.size(); index++) {
            player.getPA().sendString("", TELEPORT_MESSAGE_ID_LIST.get(index));
        }

        player.getPA().setScrollableMaxHeight(INTERFACE_SCROLLABLE_ID, 175
                + (buttonList.size() > 7 ? (buttonList.size() - 7) * 25 : 0));
    }

    public boolean clickButton(int buttonId) {
        for (int index = 0; index < TAB_SELECTION_BUTTON_IDS.length; index++) {
            if (buttonId == TAB_SELECTION_BUTTON_IDS[index]) {
                menuSelectionIndex = index;
                sendTeleportList();
                player.getPA().resetScrollBar(INTERFACE_SCROLLABLE_ID);
                return true;
            }
        }

        for (int index = 0; index < TELEPORT_BUTTON_ID_LIST.size(); index++) {
            if (buttonId == TELEPORT_BUTTON_ID_LIST.get(index)) {
                List<TeleportButton> buttonList = TELEPORT_CONTAINER_LIST.get(menuSelectionIndex).getButtonList();
                if (index < buttonList.size()) {
                    TeleportButton teleportButton = buttonList.get(index);
                    teleportButton.getAction().accept(player);
                    lastTeleport = teleportButton;
                }
                return true;
            }
        }

        return false;
    }

    public void repeatLastTeleport() {
        if (lastTeleport != null) {
            lastTeleport.getAction().accept(player);
            player.sendMessage("You teleport to your most recent location. <col=7a2100>(CTRL + T)");
        } else {
            player.sendMessage("You haven't teleported anywhere recently. <col=7a2100>(CTRL + T)");
        }
    }
}
