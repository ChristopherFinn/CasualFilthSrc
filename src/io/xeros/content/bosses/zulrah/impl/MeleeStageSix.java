package io.xeros.content.bosses.zulrah.impl;

import io.xeros.content.bosses.zulrah.Zulrah;
import io.xeros.content.bosses.zulrah.ZulrahLocation;
import io.xeros.content.bosses.zulrah.ZulrahStage;
import io.xeros.model.CombatType;
import io.xeros.model.cycleevent.CycleEventContainer;
import io.xeros.model.entity.player.Player;

public class MeleeStageSix extends ZulrahStage {

	public MeleeStageSix(Zulrah zulrah, Player player) {
		super(zulrah, player);
	}

	@Override
	public void execute(CycleEventContainer container) {
		if (container.getOwner() == null || zulrah == null || zulrah.getNpc() == null || zulrah.getNpc().isDead() || player == null || player.isDead
				|| zulrah.getInstancedZulrah() == null) {
			container.stop();
			return;
		}
		if (zulrah.getNpc().totalAttacks > 1 && zulrah.getNpc().attackTimer == 9) {
			player.getZulrahEvent().changeStage(7, CombatType.RANGE, ZulrahLocation.EAST);
			zulrah.getNpc().totalAttacks = 0;
			zulrah.getNpc().setFacePlayer(true);
			container.stop();
			return;
		}
	}

}
