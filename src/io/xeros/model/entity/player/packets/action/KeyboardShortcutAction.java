package io.xeros.model.entity.player.packets.action;

import io.xeros.content.keyboard_actions.KeyboardAction;
import io.xeros.model.entity.player.PacketType;
import io.xeros.model.entity.player.Player;

/**
 * @author Leviticus | www.rune-server.ee/members/leviticus
 * @version 1.0
 */
public class KeyboardShortcutAction implements PacketType {

	@Override
	public void processPacket(Player player, int packetType, int packetSize) {
		if (player.getMovementState().isLocked() || player.getLock().cannotInteract(player))
			return;
		int action = player.getInStream().readUnsignedByte();
		player.debug(String.format("KeyboardShortcutAction action=%d", action));

		for(KeyboardAction keyboardAction : KeyboardAction.values()) {
			if (action == keyboardAction.getAction()) {
				keyboardAction.execute(player);
				break;
			}
		}
	}
}