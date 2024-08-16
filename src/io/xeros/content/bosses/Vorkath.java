package io.xeros.content.bosses;

import java.util.ArrayList;

import com.google.common.collect.Lists;
import io.xeros.Server;
import io.xeros.model.cycleevent.CycleEvent;
import io.xeros.model.cycleevent.CycleEventContainer;
import io.xeros.model.cycleevent.CycleEventHandler;
import io.xeros.model.entity.npc.NPC;
import io.xeros.model.entity.npc.NPCSpawning;
import io.xeros.model.entity.player.Player;
import io.xeros.model.entity.player.Position;
import io.xeros.model.items.GameItem;
import io.xeros.util.Misc;

public class Vorkath {

	public static int attackStyle;

	public static int[] lootCoordinates = { 2268, 4061 };

	public static final int[] NPC_IDS = {8026, 8027, 8028};

	public static final Position SPAWN = new Position(2268, 4061);

	public static ArrayList<GameItem> getVeryRareDrops() {
		return Lists.newArrayList(new GameItem(11286, 1), new GameItem(22006, 1));
	}

	public static boolean inVorkath(Player player) {
		return (player.absX > 2255 && player.absX < 2288 && player.absY > 4053 && player.absY < 4083);
	}

	public static void poke(Player player, NPC npc) {
		if(player.heightLevel == 0) {
			player.sendMessage("Vorkath isn't interested in fighting right now... try rejoining the instance.");
			return;
		}
		npc.requestTransform(8027);
		CycleEventHandler.getSingleton().addEvent(player, new CycleEvent() {
			@Override
			public void execute(CycleEventContainer container) {
				npc.requestTransform(8028);
			}

			@Override
			public void onStopped() {
			}
		}, 7);

	}

	public static void spawn(Player player) {
		NPCSpawning.spawnNpc(player, 8026, SPAWN.getX(), SPAWN.getY(), player.getIndex() * 4, 0,
				player.antifireDelay > 0 ? 0 : 61, true, false);
	}

	public static void enterInstance(Player player, int instance) {
		CycleEventHandler.getSingleton().addEvent(player, new CycleEvent() {
			@Override
			public void execute(CycleEventContainer container) {
				if (player.absY == 4052 && player.absX != 2272) {
					player.setForceMovement(2272, 4054, 10, 10, "NORTH", 1660);
				}
				if (player.absY == 4052 && player.absX == 2272) {
					player.setForceMovement(player.absX, 4054, 10, 10, "NORTH", 839);
					player.getPA().movePlayer(player.absX, player.absY, player.getIndex() * 4);
					spawn(player);
					container.stop();
				}
			}

			@Override
			public void onStopped() {
			}
		}, 1);
	}

	public static void exit(Player player) {
		CycleEventHandler.getSingleton().addEvent(player, new CycleEvent() {
			@Override
			public void execute(CycleEventContainer container) {
				if (player.absY == 4054 && player.absX != 2272) {
					player.setForceMovement(2272, 4052, 10, 10, "SOUTH", 1660);
				}
				if (player.absY == 4054 && player.absX == 2272) {
					player.setForceMovement(player.absX, 4052, 10, 10, "SOUTH", 839);
					player.getPA().movePlayer(player.absX, player.absY, 0);
					container.stop();
				}
			}

			@Override
			public void onStopped() {
			}
		}, 1);

	}

}
