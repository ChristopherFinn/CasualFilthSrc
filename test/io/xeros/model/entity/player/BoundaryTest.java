package io.xeros.model.entity.player;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BoundaryTest {

    @Test
    void boundary_intersects() {
        Boundary a = new Boundary(0, 0, 4, 4);
        Boundary b = new Boundary(1, 1, 5, 5);
        assertTrue(a.intersects(b));
        assertTrue(b.intersects(a));
    }

    @Test
    void boundary_does_not_intersect() {
        Boundary a = new Boundary(0, 0, 4, 4);
        Boundary b = new Boundary(5, 5, 6, 6);
        assertFalse(a.intersects(b));
        assertFalse(b.intersects(a));
    }

}