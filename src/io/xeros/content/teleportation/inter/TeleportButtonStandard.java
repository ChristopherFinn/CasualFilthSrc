package io.xeros.content.teleportation.inter;

import io.xeros.model.entity.player.Position;

public class TeleportButtonStandard extends TeleportButton {

    public TeleportButtonStandard(String name, Position teleport, boolean warning) {
        super(name, plr -> DangerousWarning.handleTeleport(plr, teleport, warning));
    }

    public TeleportButtonStandard(String name, Position teleport) {
        super(name, plr -> plr.getPA().startTeleport(teleport.getX(), teleport.getY(), teleport.getHeight(), "modern", false));
    }
}
