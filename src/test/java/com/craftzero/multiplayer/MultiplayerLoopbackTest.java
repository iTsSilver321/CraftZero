package com.craftzero.multiplayer;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;

import static org.junit.jupiter.api.Assertions.*;

class MultiplayerLoopbackTest {
    private static final Duration TIMEOUT = Duration.ofSeconds(3);

    @Test
    @DisplayName("Host should start on the protocol port by default and sync hello/world state")
    void hostStartConnectAndWorldStateSync() throws Exception {
        assertEquals(25565, MultiplayerProtocol.DEFAULT_PORT);

        try (ProtocolServer server = new ProtocolServer(0, 123456789L, 6000L)) {
            server.start();

            assertTrue(server.isRunning());
            assertTrue(server.getPort() > 0);

            try (ProtocolClient client = ProtocolClient.connect("127.0.0.1", server.getPort(), "Alex")) {
                ProtocolMessage.Hello hello = client.awaitHello(TIMEOUT);
                ProtocolMessage.WorldState worldState = client.awaitWorldState(TIMEOUT);

                assertNotNull(hello);
                assertEquals(MultiplayerProtocol.VERSION, hello.protocolVersion());
                assertFalse(hello.playerId().isBlank());
                assertEquals(hello.playerId(), client.getPlayerId());

                assertNotNull(worldState);
                assertEquals(123456789L, worldState.seed());
                assertEquals(6000L, worldState.timeOfDay());
            }
        }
    }

    @Test
    @DisplayName("Client movement input should broadcast typed player state")
    void clientMovementBroadcastsPlayerState() throws Exception {
        try (ProtocolServer server = new ProtocolServer(0, 42L, 0L)) {
            server.start();

            try (ProtocolClient client = ProtocolClient.connect("127.0.0.1", server.getPort(), "Mover")) {
                ProtocolMessage.Hello hello = client.awaitHello(TIMEOUT);
                assertNotNull(hello);
                assertNotNull(client.awaitWorldState(TIMEOUT));

                ProtocolMessage.PlayerPose pose = new ProtocolMessage.PlayerPose(10.5, 65.0, -3.25, 90.0f, 15.0f);
                client.sendInput(pose, true, false, false, false, false, false);

                ProtocolMessage.PlayerState playerState = client.waitForMessage(
                        ProtocolMessage.PlayerState.class,
                        TIMEOUT
                );

                assertNotNull(playerState);
                assertEquals(hello.playerId(), playerState.playerId());
                assertEquals("Mover", playerState.username());
                assertEquals(10.5, playerState.pose().x(), 0.0001);
                assertEquals(65.0, playerState.pose().y(), 0.0001);
                assertEquals(-3.25, playerState.pose().z(), 0.0001);
                assertEquals(90.0f, playerState.pose().yaw(), 0.0001f);
                assertEquals(15.0f, playerState.pose().pitch(), 0.0001f);
                assertTrue(playerState.onGround());
                assertEquals(1, server.trackedPlayerCount());
            }
        }
    }

    @Test
    @DisplayName("Client block actions should broadcast typed block updates")
    void clientBlockActionBroadcastsBlockUpdate() throws Exception {
        try (ProtocolServer server = new ProtocolServer(0, 7L, 100L)) {
            server.start();

            try (ProtocolClient client = ProtocolClient.connect("127.0.0.1", server.getPort(), "Builder")) {
                ProtocolMessage.Hello hello = client.awaitHello(TIMEOUT);
                assertNotNull(hello);
                assertNotNull(client.awaitWorldState(TIMEOUT));

                client.sendBlockAction(new ProtocolMessage.BlockUpdate(4, 63, -8, "craftzero:stone", 0, ""));

                ProtocolMessage.BlockUpdate blockUpdate = client.waitForMessage(
                        ProtocolMessage.BlockUpdate.class,
                        TIMEOUT
                );

                assertNotNull(blockUpdate);
                assertEquals(4, blockUpdate.x());
                assertEquals(63, blockUpdate.y());
                assertEquals(-8, blockUpdate.z());
                assertEquals("craftzero:stone", blockUpdate.blockId());
                assertEquals(0, blockUpdate.metadata());
                assertEquals(hello.playerId(), blockUpdate.sourcePlayerId());
            }
        }
    }

    @Test
    @DisplayName("Typed chat should round-trip sender identity")
    void typedChatBroadcastsSenderIdentity() throws Exception {
        try (ProtocolServer server = new ProtocolServer(0, 12L, 0L)) {
            server.start();

            try (ProtocolClient client = ProtocolClient.connect("127.0.0.1", server.getPort(), "Alex")) {
                ProtocolMessage.Hello hello = client.awaitHello(TIMEOUT);
                assertNotNull(hello);
                assertNotNull(client.awaitWorldState(TIMEOUT));

                client.sendChat("hello typed chat");

                ProtocolMessage.Chat chat = client.waitForMessage(ProtocolMessage.Chat.class, TIMEOUT);
                assertNotNull(chat);
                assertEquals(hello.playerId(), chat.playerId());
                assertEquals("Alex", chat.sender());
                assertEquals("hello typed chat", chat.text());
            }
        }
    }

    @Test
    @DisplayName("Legacy server facade should expose joined usernames in player state")
    void legacyFacadeTracksPlayerNamesForCommands() throws Exception {
        try (MultiplayerServer server = new MultiplayerServer(0, 98765L, 42.5f)) {
            server.start();

            try (MultiplayerClient client = new MultiplayerClient()) {
                client.connect("127.0.0.1", server.getPort(), "Alex");
                assertEventually(() -> client.clientId() > 0);

                client.sendPlayerState(1.0f, 2.0f, 3.0f, 90.0f, 15.0f);

                assertEventually(() -> server.playerStates().containsKey(client.clientId()));
                assertEquals(
                        "Alex",
                        server.playerStates().get(client.clientId()).get("username").getAsString()
                );
            }
        }
    }

    @Test
    @DisplayName("Legacy server facade should broadcast addressed command actions")
    void legacyFacadeBroadcastsAddressedCommandActions() throws Exception {
        try (MultiplayerServer server = new MultiplayerServer(0, 13579L, 0.0f)) {
            server.start();
            CountDownLatch received = new CountDownLatch(1);
            AtomicReference<NetworkMessage> actionMessage = new AtomicReference<>();

            try (MultiplayerClient client = new MultiplayerClient()) {
                client.addListener(message -> {
                    if ("clientAction".equals(message.type())
                            && MultiplayerProtocol.ACTION_COMMAND_GIVE.equals(message.data().get("action").getAsString())) {
                        actionMessage.set(message);
                        received.countDown();
                    }
                });
                client.connect("127.0.0.1", server.getPort(), "Alex");
                assertEventually(() -> client.clientId() > 0);

                com.google.gson.JsonObject payload = NetworkMessage.object();
                payload.addProperty("playerId", "player-" + client.clientId());
                payload.addProperty("action", MultiplayerProtocol.ACTION_COMMAND_GIVE);
                payload.addProperty("itemId", "264");
                payload.addProperty("itemData", "0");
                payload.addProperty("count", "2");
                server.broadcast(NetworkMessage.of("clientAction", payload));

                assertTrue(received.await(TIMEOUT.toMillis(), TimeUnit.MILLISECONDS));
                assertEquals(client.clientId(), actionMessage.get().data().get("clientId").getAsInt());
                assertEquals("player-" + client.clientId(), actionMessage.get().data().get("playerId").getAsString());
                assertEquals("264", actionMessage.get().data().get("itemId").getAsString());
                assertEquals("2", actionMessage.get().data().get("count").getAsString());
            }
        }
    }

    @Test
    @DisplayName("Hosted bed sleep should wait for the host and every connected player")
    void hostedBedSleepRequiresEveryConnectedPlayer() throws Exception {
        try (MultiplayerServer server = new MultiplayerServer(0, 24680L, 18000.0f)) {
            server.start();

            try (ProtocolClient alex = ProtocolClient.connect("127.0.0.1", server.getPort(), "Alex");
                    ProtocolClient steve = ProtocolClient.connect("127.0.0.1", server.getPort(), "Steve")) {
                assertNotNull(alex.awaitHello(TIMEOUT));
                assertNotNull(alex.awaitWorldState(TIMEOUT));
                assertNotNull(steve.awaitHello(TIMEOUT));
                assertNotNull(steve.awaitWorldState(TIMEOUT));

                alex.sendClientAction(MultiplayerProtocol.ACTION_BED_SLEEP_START, Map.of());
                assertNull(alex.waitForMessage(ProtocolMessage.ClientAction.class, Duration.ofMillis(150)));
                assertFalse(server.hasPendingSleepCompletion());

                assertFalse(server.beginHostSleep());
                assertNull(alex.waitForMessage(ProtocolMessage.ClientAction.class, Duration.ofMillis(150)));

                steve.sendClientAction(MultiplayerProtocol.ACTION_BED_SLEEP_START, Map.of());

                ProtocolMessage.WorldState morning = waitForWorldTime(alex, 0.0);
                assertNotNull(morning);
                assertEquals(0.0, morning.timeOfDay(), 0.0001);
                ProtocolMessage.ClientAction completion =
                        alex.waitForMessage(ProtocolMessage.ClientAction.class, TIMEOUT);
                assertNotNull(completion);
                assertEquals(MultiplayerProtocol.ACTION_BED_SLEEP_COMPLETE, completion.action());
                assertEquals("0.0", completion.data().get("time"));
                assertTrue(server.hasPendingSleepCompletion());
                assertTrue(server.consumePendingSleepCompletion());
                assertFalse(server.hasPendingSleepCompletion());
            }
        }
    }

    @Test
    @DisplayName("Stopping bed sleep should remove the client from the sleep quorum")
    void stoppedBedSleepNoLongerCountsTowardAllPlayerSleep() throws Exception {
        try (MultiplayerServer server = new MultiplayerServer(0, 24681L, 18000.0f)) {
            server.start();

            try (ProtocolClient alex = ProtocolClient.connect("127.0.0.1", server.getPort(), "Alex");
                    ProtocolClient steve = ProtocolClient.connect("127.0.0.1", server.getPort(), "Steve")) {
                assertNotNull(alex.awaitHello(TIMEOUT));
                assertNotNull(alex.awaitWorldState(TIMEOUT));
                assertNotNull(steve.awaitHello(TIMEOUT));
                assertNotNull(steve.awaitWorldState(TIMEOUT));

                alex.sendClientAction(MultiplayerProtocol.ACTION_BED_SLEEP_START, Map.of());
                alex.sendClientAction(MultiplayerProtocol.ACTION_BED_SLEEP_STOP, Map.of());
                assertFalse(server.beginHostSleep());

                steve.sendClientAction(MultiplayerProtocol.ACTION_BED_SLEEP_START, Map.of());

                assertNull(alex.waitForMessage(ProtocolMessage.ClientAction.class, Duration.ofMillis(150)));
                assertFalse(server.hasPendingSleepCompletion());
                assertEquals(3, server.sleepEligiblePlayerCount());
                assertEquals(2, server.sleepingPlayerCount());
            }
        }
    }

    @Test
    @DisplayName("Disconnect should clean up server-side client and player state")
    void disconnectCleansUpServerState() throws Exception {
        try (ProtocolServer server = new ProtocolServer(0, 99L, 0L)) {
            server.start();

            ProtocolClient client = ProtocolClient.connect("127.0.0.1", server.getPort(), "Leaver");
            ProtocolMessage.Hello hello = client.awaitHello(TIMEOUT);
            assertNotNull(hello);
            assertNotNull(client.awaitWorldState(TIMEOUT));
            client.sendInput(ProtocolMessage.PlayerPose.origin());

            assertEventually(() -> server.connectedPlayerCount() == 1);
            assertEventually(() -> server.trackedPlayerCount() == 1);

            client.disconnect("leaving");

            assertEventually(() -> server.connectedPlayerCount() == 0);
            assertEventually(() -> server.trackedPlayerCount() == 0);
        }
    }

    @Test
    @DisplayName("Typed client waits should not discard unmatched messages")
    void filteredWaitKeepsUnmatchedMessages() throws Exception {
        try (ProtocolServer server = new ProtocolServer(0, 55L, 1234L)) {
            server.start();

            try (ProtocolClient client = ProtocolClient.connect("127.0.0.1", server.getPort(), "Patient")) {
                ProtocolMessage.Hello hello = client.waitForMessage(ProtocolMessage.Hello.class, TIMEOUT);
                assertNotNull(hello);

                ProtocolMessage.BlockUpdate missing = client.waitForMessage(
                        ProtocolMessage.BlockUpdate.class,
                        Duration.ofMillis(100)
                );
                assertNull(missing);

                ProtocolMessage.WorldState worldState = client.waitForMessage(
                        ProtocolMessage.WorldState.class,
                        TIMEOUT
                );
                assertNotNull(worldState);
                assertEquals(55L, worldState.seed());
                assertEquals(1234L, worldState.timeOfDay());
            }
        }
    }

    @Test
    @DisplayName("Legacy multiplayer facade should connect over loopback and release its port")
    void legacyFacadeLoopbackAndPortCleanup() throws Exception {
        assertEquals(MultiplayerProtocol.DEFAULT_PORT, MultiplayerServer.DEFAULT_PORT);

        int port;
        try (MultiplayerServer server = new MultiplayerServer(0, 98765L, 42.5f)) {
            server.start();
            port = server.getPort();
            assertTrue(port > 0);

            try (MultiplayerClient client = new MultiplayerClient()) {
                client.connect("", port);

                assertEventually(() -> client.clientId() > 0);
                assertEquals(98765L, client.seed());
                assertEquals(42.5f, client.worldTime(), 0.0001f);

                client.sendPlayerState(1.0f, 2.0f, 3.0f, 90.0f, 15.0f);
                assertEventually(() -> server.playerStates().containsKey(client.clientId()));
            }

            assertEventually(() -> server.clientCount() == 0);
        }

        try (MultiplayerServer restarted = new MultiplayerServer(port, 1L, 0.0f)) {
            restarted.start();
            assertEquals(port, restarted.getPort());
        }
    }

    @Test
    @DisplayName("Legacy multiplayer facade should broadcast chat messages over loopback")
    void legacyFacadeBroadcastsChat() throws Exception {
        try (MultiplayerServer server = new MultiplayerServer(0, 123L, 0.0f)) {
            server.start();
            CountDownLatch received = new CountDownLatch(1);
            AtomicReference<NetworkMessage> chatMessage = new AtomicReference<>();

            try (MultiplayerClient client = new MultiplayerClient()) {
                client.addListener(message -> {
                    if ("chat".equals(message.type())) {
                        chatMessage.set(message);
                        received.countDown();
                    }
                });
                client.connect("127.0.0.1", server.getPort(), "Alex");
                assertEventually(() -> client.clientId() > 0);

                client.sendChat("hello world");

                assertTrue(received.await(TIMEOUT.toMillis(), TimeUnit.MILLISECONDS));
                assertEquals("Alex", chatMessage.get().data().get("sender").getAsString());
                assertEquals("hello world", chatMessage.get().data().get("text").getAsString());
            }
        }
    }

    private static void assertEventually(BooleanSupplier condition) throws InterruptedException {
        long deadline = System.nanoTime() + TIMEOUT.toNanos();
        while (System.nanoTime() < deadline) {
            if (condition.getAsBoolean()) {
                return;
            }
            Thread.sleep(10L);
        }
        assertTrue(condition.getAsBoolean());
    }

    private static ProtocolMessage.WorldState waitForWorldTime(ProtocolClient client, double expectedTime)
            throws InterruptedException {
        long deadline = System.nanoTime() + TIMEOUT.toNanos();
        while (System.nanoTime() < deadline) {
            ProtocolMessage.WorldState state = client.waitForMessage(
                    ProtocolMessage.WorldState.class,
                    Duration.ofMillis(100)
            );
            if (state != null && Math.abs(state.timeOfDay() - expectedTime) < 0.0001) {
                return state;
            }
        }
        return null;
    }
}
