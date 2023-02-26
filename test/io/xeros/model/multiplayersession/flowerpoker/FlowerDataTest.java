package io.xeros.model.multiplayersession.flowerpoker;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FlowerDataTest {

    @Test
    void check_flower_rolls() {
        assertThrows(IllegalArgumentException.class, () -> FlowerData.getRandomFlower(10_000));
        assertThrows(IllegalArgumentException.class, () -> FlowerData.getRandomFlower(-1));

        for (int i = 0; i < 10_000; i++)
            assertNotNull(FlowerData.getRandomFlower(i));
        for (int i = 0; i < 30_000; i++)
            assertNotNull(FlowerData.getRandomFlower());
    }

}