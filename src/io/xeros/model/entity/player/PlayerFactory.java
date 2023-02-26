package io.xeros.model.entity.player;

import java.util.Arrays;

public class PlayerFactory {

    public static void createTestPlayers() {
        Arrays.stream(Right.values()).forEach(right -> {
            String name = right.toString();
            if (name.length() > 12)
                name = name.substring(0, 12);
            Player player = Player.createBot(name, right);
            player.addQueuedAction(Player::forceLogout);
        });
    }
}
