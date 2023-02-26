package io.xeros.net.login;

import io.xeros.Configuration;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class RS2LoginProtocolTest {

    private static final List<String> MACS = List.of(
        "01-80-C2-00-00-00", "01-80-C2-00-00-01", "01-80-C2-00-00-00"
    );

    @Test
    public void test_valid_address() {
        MACS.forEach(it -> assertTrue(RS2LoginProtocol.isValidMacAddress(it)));

        for (int i = 0; i < 100; i++)
            assertTrue(RS2LoginProtocol.isValidUUID(UUID.randomUUID().toString()));
    }

}