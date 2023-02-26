package io.xeros.content.combat.formula;

import org.junit.jupiter.api.Test;

class TwistedBowTest {

    int[] accuracyTestValues = {20, 50, 70, 100, 120, 170, 200, 500};

    @Test
    public void testTwistedBowFormula() {
        for (int index = 0; index < accuracyTestValues.length; index++) {
            System.out.println("Magic level " + accuracyTestValues[index] + ": "
                    + "Accuracy: " + RangeMaxHit.getTwistedBowAccuracyBoost(accuracyTestValues[index]) + ", "
                    + "Damage: " + RangeMaxHit.getTwistedBowDamageBoost(accuracyTestValues[index], false) + ", "
                    + "Damage-cox: " + RangeMaxHit.getTwistedBowDamageBoost(accuracyTestValues[index], true));
        }
        assert true;
    }

}