package io.xeros.util;

import com.google.common.collect.Lists;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MiscTest {

    @Test
    public void testGetRandoms() {
        ArrayList<Integer> list = Lists.newArrayList(1);
        List<Integer> randoms1 = Misc.randoms(list, 2);
        assertTrue(randoms1.size() == 1 && randoms1.get(0) == 1);

        list.add(2);
        randoms1 = Misc.randoms(list, 2);
        assertTrue(randoms1.size() == 2 && randoms1.contains(1) && randoms1.contains(2));

        list.add(3);
        randoms1 = Misc.randoms(list, 2);
        assertEquals(2, randoms1.size());

        list.add(4);
        randoms1 = Misc.randoms(list, 1);
        assertEquals(1, randoms1.size());

        list.add(5);
        randoms1 = Misc.randoms(list, 6);
        assertEquals(5, randoms1.size());

        list.add(3);
        randoms1 = Misc.randoms(list, 0);
        assertEquals(0, randoms1.size());
    }

    @Test
    void replaceBracketsWithArguments() {
        assertEquals("1 test ty player", Misc.replaceBracketsWithArguments("{} test ty {}", 1, "player"));
    }
}