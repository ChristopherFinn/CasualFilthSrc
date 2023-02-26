package io.xeros.test;

import io.xeros.model.entity.player.Player;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Data
@Builder
public class TestPlayer {

    public static Player named(String name) {
        return TestPlayer.builder().username(name).build().player();
    }

    /**
     * @param nameFormat A {@link String#format(String, Object...)} name, where an argument (%d) will be replaced with a number.
     * @param amount The amount of players to generate
     * @return The list of generated players
     */
    public static List<Player> gen(String nameFormat, int amount) {
        return IntStream.range(0, amount).mapToObj(it -> named(String.format(nameFormat, amount))).collect(Collectors.toList());
    }

    private String username;
    private String password;

    public Player player() {
        Player player = new Player(null);
        player.setLoginName(username);
        player.setDisplayName(username);
        player.playerPass = password;
        return player;
    }
}
