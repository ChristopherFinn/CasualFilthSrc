package io.xeros.model.entity.player.packets;

import io.xeros.model.entity.player.PacketType;
import io.xeros.model.entity.player.Player;

/**
 * Slient Packet
 **/
public class SilentPacket implements PacketType {

	@Override
	public void processPacket(Player c, int packetType, int packetSize) {

	}
}
