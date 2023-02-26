package io.xeros.model.entity.player.packets;

import java.util.Objects;

import io.xeros.Server;
import io.xeros.content.lootbag.LootingBag;
import io.xeros.model.cycleevent.CycleEvent;
import io.xeros.model.cycleevent.CycleEventContainer;
import io.xeros.model.cycleevent.CycleEventHandler;
import io.xeros.model.entity.player.Boundary;
import io.xeros.model.entity.player.PacketType;
import io.xeros.model.entity.player.Player;
import io.xeros.model.entity.player.PlayerHandler;
import io.xeros.model.entity.player.mode.group.GroupIronmanGroup;
import io.xeros.model.entity.player.mode.group.GroupIronmanRepository;
import io.xeros.model.items.GroundItem;
import io.xeros.model.multiplayersession.MultiplayerSessionFinalizeType;
import io.xeros.model.multiplayersession.MultiplayerSessionStage;
import io.xeros.model.multiplayersession.MultiplayerSessionType;
import io.xeros.model.multiplayersession.duel.DuelSession;

/**
 * Pickup Item
 **/
public class PickupItem implements PacketType {

    @Override
    public void processPacket(final Player c, int packetType, int packetSize) {
        if (c.getMovementState().isLocked() || c.getLock().cannotInteract(c))
            return;
        if (c.isFping()) {
            /**
             * Cannot do action while fping
             */
            return;
        }
        c.interruptActions();
        c.walkingToItem = false;
        c.itemY = c.getInStream().readSignedWordBigEndian();
        c.itemId = c.getInStream().readUnsignedWord();
        c.itemX = c.getInStream().readSignedWordBigEndian();
        if (Math.abs(c.getX() - c.itemX) > 25 || Math.abs(c.getY() - c.itemY) > 25) {
            c.resetWalkingQueue();
            return;
        }
        DuelSession duelSession = (DuelSession) Server.getMultiplayerSessionListener().getMultiplayerSession(c, MultiplayerSessionType.DUEL);
        if (Objects.nonNull(duelSession) && duelSession.getStage().getStage() > MultiplayerSessionStage.REQUEST
                && duelSession.getStage().getStage() < MultiplayerSessionStage.FURTHER_INTERATION) {
            c.sendMessage("Your actions have declined the duel.");
            duelSession.getOther(c).sendMessage("The challenger has declined the duel.");
            duelSession.finish(MultiplayerSessionFinalizeType.WITHDRAW_ITEMS);
            return;
        }
        if (c.itemId == LootingBag.LOOTING_BAG && (c.getItems().getItemCount(LootingBag.LOOTING_BAG, true) >= 1
                || c.getItems().getItemCount(LootingBag.LOOTING_BAG_OPEN, true) >= 1)) {
            c.sendMessage("You cannot own multiples of this item.");
            return;
        }
        if (c.itemId == 12791 && c.getItems().getItemCount(12791, true) > 1) {
            c.sendMessage("You cannot own multiples of this item.");
            return;
        }
        if (c.getBankPin().requiresUnlock()) {
            c.getBankPin().open(2);
            return;
        }
        if (c.isStuck) {
            c.isStuck = false;
            c.sendMessage("@red@You've disrupted stuck command, you will no longer be moved home.");
            return;
        }
        if (c.isNpc) {
            return;
        }
        if ((Boundary.isIn(c, Boundary.OUTLAST_AREA) || Boundary.isIn(c, Boundary.LUMBRIDGE_OUTLAST_AREA)) && c.spectatingTournament) {
            return;
        }
        if (c.isDead || c.getHealth().getCurrentHealth() <= 0) {
            return;
        }
        GroundItem item = Server.itemHandler.getGroundItem(c, c.itemId, c.itemX, c.itemY, c.heightLevel);
        if (item == null) {
            return;
        }

        if (!c.getMode().isItemScavengingPermitted()) {
            Player owner = PlayerHandler.getPlayerByLoginName(item.getOwnerName());
            GroupIronmanGroup group = GroupIronmanRepository.getGroupForOnline(c).orElse(null);
            if (owner == null || group == null && !c.getLoginNameLower().equalsIgnoreCase(item.getOwnerName()) || group != null && c.isApartOfGroupIronmanGroup() && !group.isGroupMember(owner)) {
                c.sendMessage("Your mode restricts you from picking up items that are not yours.");
                return;
            }
        }

        if (c.getInterfaceEvent().isActive()) {
            c.sendMessage("Please finish what you're doing.");
            return;
        }
        if (c.getPA().viewingOtherBank) {
            c.getPA().resetOtherBank();
        }
        c.attacking.reset();
        if (c.teleportingToDistrict) {
            return;
        }
        if (c.getPosition().inClanWars() || c.getPosition().inClanWarsSafe()) {
            if (!c.pkDistrict) {
                return;
            }
        }
        if (c.getX() == c.itemX && c.getY() == c.itemY) {
            pickup(c);
        } else {
            c.walkingToItem = true;
            CycleEventHandler.getSingleton().addEvent(c, new CycleEvent() {
                @Override
                public void execute(CycleEventContainer container) {
                    if (!c.walkingToItem) {
                        container.stop();
                        return;
                    }
                    if (c.getX() == c.itemX && c.getY() == c.itemY) {
                        pickup(c);
                        container.stop();
                    }
                }

                @Override
                public void onStopped() {
                    c.walkingToItem = false;
                }
            }, 1);
        }

    }

    private void pickup(Player c) {
        Server.itemHandler.removeGroundItem(c, c.itemId, c.itemX, c.itemY, c.heightLevel, true);
        c.getPA().sendSound(2582);
    }

}
