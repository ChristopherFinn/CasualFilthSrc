package io.xeros.test;

import io.xeros.Server;
import io.xeros.ServerConfiguration;
import io.xeros.ServerState;
import io.xeros.model.entity.player.Player;
import org.junit.jupiter.api.BeforeAll;

import java.io.IOException;

public class ServerTest {

    private final ServerConfiguration configuration;

    public ServerTest(ServerConfiguration configuration) {
        this.configuration = configuration;
        try {
            Server.startServerless(configuration);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public ServerTest(ServerState serverState) {
        this(TestServerConfiguration.getConfiguration(serverState));
    }

    public ServerConfiguration getConfiguration() {
        return configuration;
    }
}
