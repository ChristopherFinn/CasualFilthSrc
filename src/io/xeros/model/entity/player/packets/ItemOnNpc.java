package io.xeros.model.entity.player.packets;

import java.util.Objects;

import io.xeros.Server;
import io.xeros.content.items.UseItem;
import io.xeros.model.cycleevent.CycleEvent;
import io.xeros.model.cycleevent.CycleEventContainer;
import io.xeros.model.cycleevent.CycleEventHandler;
import io.xeros.model.entity.npc.NPC;
import io.xeros.model.entity.npc.NPCHandler;
import io.xeros.model.entity.player.PacketType;
import io.xeros.model.entity.player.Player;
import io.xeros.model.multiplayersession.MultiplayerSessionFinalizeType;
import io.xeros.model.multiplayersession.MultiplayerSessionStage;
import io.xeros.model.multiplayersession.MultiplayerSessionType;
import io.xeros.model.multiplayersession.duel.DuelSession;

public class ItemOnNpc implements PacketType {

	@Override
	public void processPacket(Player c, int packetType, int packetSize) {
		if (c.getMovementState().isLocked() || c.getLock().cannotInteract(c))
			return;

		if (c.isFping()) {
			/**
			 * Cannot do action while fping
			 */
			return;
		}
		c.interruptActions();
		int itemId = c.getInStream().readSignedWordA();
		int npcIndex = c.getInStream().readSignedWordA();
		int slot = c.getInStream().readSignedWordBigEndian();

		if (npcIndex >= NPCHandler.npcs.length || npcIndex < 0)
			return;

		NPC npc = NPCHandler.npcs[npcIndex];

		if (npc == null)
			return;

		if (c.getInterfaceEvent().isActive()) {
			c.sendMessage("Please finish what you're doing.");
			return;
		}

		if (c.getBankPin().requiresUnlock()) {
			c.getBankPin().open(2);
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

		if (npc.distance(c.getPosition()) <= 1) {
			completeItemOnNpc(c, npc, itemId, slot);
		} else {
			c.getPA().followNPC(npc, false);
			CycleEventHandler.getSingleton().addEvent(c, new CycleEvent() {
				@Override
				public void execute(CycleEventContainer container) {
					if (npc.distance(c.getPosition()) <= 1) {
						completeItemOnNpc(c, npc, itemId, slot);
						container.stop();
					}
				}

				@Override
				public void onStopped() {
					c.getPA().resetFollow();
				}
			}, 1);
		}
	}

	private void completeItemOnNpc(Player c, NPC npc, int itemId, int itemSlot) {
		c.facePosition(npc.getX(), npc.getY());
		npc.facePlayer(c.getIndex());
		c.faceUpdate(0);
		c.getPA().resetFollow();
		if (!c.getItems().playerHasItem(itemId, 1))
			return;
		UseItem.handleItemOnNPC(c, npc, itemId, itemSlot);
	}
}
