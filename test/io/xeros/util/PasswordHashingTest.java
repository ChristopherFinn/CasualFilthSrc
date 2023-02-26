package io.xeros.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PasswordHashingTest {

    @Test
    public void testPasswordHash() {
        String password = "sa9$@in90U$W%0-inRH";
        String hashed = PasswordHashing.hash(password);
        assertTrue(PasswordHashing.check(password, hashed));
        assertFalse(PasswordHashing.check(password + "1", hashed));
    }

}