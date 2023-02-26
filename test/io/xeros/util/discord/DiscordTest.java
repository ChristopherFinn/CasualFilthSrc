package io.xeros.util.discord;

import io.xeros.ServerState;
import io.xeros.test.ServerTest;
import org.junit.jupiter.api.Test;

class DiscordTest {

    private static final ServerTest test = new ServerTest(ServerState.PUBLIC);

    @Test
    public void sendDiscordTestMessages() throws InterruptedException {
        Discord.writeServerSyncMessage("test");
        Discord.writeBugMessage("Test");
        Discord.writeCheatEngineMessage("test");
        Discord.writeFoeMessage("Test");
        Discord.writeReferralMessage("Test");
        Discord.writeSuggestionMessage("test");
        Discord.writeAddressSwapMessage("test");
        Thread.sleep(60_000 * 15);
    }

}