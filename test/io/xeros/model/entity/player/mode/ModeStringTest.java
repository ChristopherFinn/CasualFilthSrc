package io.xeros.model.entity.player.mode;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ModeStringTest {

    @Test
    public void strings() {
        Arrays.stream(ModeType.values()).forEach(mode -> System.out.println(mode.toString()));
    }

}