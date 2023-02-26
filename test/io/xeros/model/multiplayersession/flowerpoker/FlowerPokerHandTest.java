package io.xeros.model.multiplayersession.flowerpoker;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static io.xeros.model.multiplayersession.flowerpoker.FlowerPokerHand.getWinner;
import static org.junit.jupiter.api.Assertions.*;

class FlowerPokerHandTest {

    @Test
    void hands() {
        List<FlowerHand> flowers = Arrays.stream(FlowerHand.values()).filter(it -> it == FlowerHand.BLACK_FLOWER
                || it == FlowerHand.WHITE_FLOWER).collect(Collectors.toList());

        // Automatic check
        flowers.forEach(a -> flowers.forEach(b -> {
            if (a == b)
                assertThrows(IllegalStateException.class, () -> getWinner(a, b));
            else
                assertEquals(a.ordinal() > b.ordinal() ? a : b, getWinner(a, b));
        }));

        // Manual checks
        assertThrows(IllegalStateException.class, () -> getWinner(FlowerHand.BUST, FlowerHand.BUST));

        // Pairs
        assertEquals(FlowerHand.ONE_PAIR, getWinner(FlowerHand.ONE_PAIR, FlowerHand.BUST));
        assertEquals(FlowerHand.TWO_PAIRS, getWinner(FlowerHand.TWO_PAIRS, FlowerHand.ONE_PAIR));
        assertEquals(FlowerHand.THREE_OF_A_KIND, getWinner(FlowerHand.THREE_OF_A_KIND, FlowerHand.TWO_PAIRS));
        assertEquals(FlowerHand.FOUR_OF_A_KIND, getWinner(FlowerHand.FOUR_OF_A_KIND, FlowerHand.THREE_OF_A_KIND));
        assertEquals(FlowerHand.FIVE_OF_A_KIND, getWinner(FlowerHand.FIVE_OF_A_KIND, FlowerHand.FOUR_OF_A_KIND));

        assertEquals(FlowerHand.FOUR_OF_A_KIND, getWinner(FlowerHand.FOUR_OF_A_KIND, FlowerHand.FULL_HOUSE));
    }

}