package com.craftzero.multiplayer;

import java.io.Closeable;
import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

public class MultiplayerClient implements Closeable {
    private static final Duration INITIAL_SYNC_TIMEOUT = Duration.ofSeconds(3);
    private static final Duration SNAPSHOT_SYNC_TIMEOUT = Duration.ofSeconds(10);

    private final List<Consumer<NetworkMessage>> listeners = new CopyOnWriteArrayList<>();
    private volatile ProtocolClient protocolClient;
    private volatile int clientId = -1;
    private volatile long seed;
    private volatile float worldTime;
    private volatile String worldWeather = "clear";
    private volatile int worldSpawnX;
    private volatile int worldSpawnY = 80;
    private volatile int worldSpawnZ;
    private volatile String worldGameMode = "SURVIVAL";
    private volatile String worldDifficulty = "EASY";
    private volatile boolean worldHardcore;
    private volatile boolean worldAllowCheats;
    private volatile boolean worldPvp = true;
    private volatile boolean worldSpawnAnimals = true;
    private volatile boolean worldSpawnMonsters = true;
    private volatile boolean worldSpawnNpcs = true;
    private volatile boolean worldAllowNether = true;
    private volatile boolean worldAllowFlight;
    private volatile String worldDimension = "overworld";
    private volatile int worldMaxPlayers = MultiplayerProtocol.DEFAULT_MAX_PLAYERS;
    private volatile int worldViewDistance = MultiplayerProtocol.DEFAULT_VIEW_DISTANCE;
    private volatile int worldMaxBuildHeight = MultiplayerProtocol.DEFAULT_MAX_BUILD_HEIGHT;
    private volatile boolean worldGenerateStructures = MultiplayerProtocol.DEFAULT_GENERATE_STRUCTURES;
    private volatile String username = "Player";
    private volatile boolean initialSnapshotComplete;

    public void connect(String host, int port) throws IOException {
        connect(host, port, username);
    }

    public void connect(String host, int port, String username) throws IOException {
        ProtocolClient existing = protocolClient;
        if (existing != null && existing.isConnected()) {
            return;
        }

        resetConnectionState(username);
        ProtocolClient client = new ProtocolClient(normalizeHost(host), port, this.username);
        client.addListener(this::handleProtocolMessage);
        protocolClient = client;
        try {
            client.connect();
            ProtocolMessage.Hello hello = client.awaitHello(INITIAL_SYNC_TIMEOUT);
            if (hello == null) {
                throw new IOException("Timed out waiting for multiplayer hello");
            }
            awaitJoinAccepted(client, hello);
            ProtocolMessage.WorldState worldState = client.awaitWorldState(INITIAL_SYNC_TIMEOUT);
            if (worldState == null) {
                ProtocolMessage.Disconnect disconnect = client.getDisconnect();
                if (disconnect != null) {
                    throw new IOException(disconnect.reason());
                }
                throw new IOException("Timed out waiting for multiplayer world sync");
            }
            applyInitialSync(hello, worldState);
            awaitInitialSnapshotComplete(client);
        } catch (IOException exception) {
            client.close();
            protocolClient = null;
            throw exception;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            client.close();
            protocolClient = null;
            throw new IOException("Interrupted while waiting for multiplayer initial sync", exception);
        }
    }

    public void send(NetworkMessage message) throws IOException {
        ProtocolClient client = requireClient();
        ProtocolMessage protocolMessage;
        try {
            protocolMessage = MultiplayerServer.toProtocolMessage(message);
        } catch (RuntimeException ignored) {
            protocolMessage = null;
        }
        if (protocolMessage == null) {
            return;
        }
        if (protocolMessage instanceof ProtocolMessage.BlockUpdate blockUpdate) {
            client.sendBlockAction(blockUpdate);
            return;
        }
        if (protocolMessage instanceof ProtocolMessage.InventoryUpdate inventoryUpdate) {
            client.sendInventoryUpdate(inventoryUpdate.slot(), inventoryUpdate.itemId(),
                    inventoryUpdate.count(), inventoryUpdate.damage(), inventoryUpdate.data());
            return;
        }
        if (isServerOwnedStateMessage(protocolMessage)) {
            return;
        }
        client.send(protocolMessage);
    }

    private static boolean isServerOwnedStateMessage(ProtocolMessage message) {
        return message instanceof ProtocolMessage.EntityUpdate
                || message instanceof ProtocolMessage.WorldEvent
                || message instanceof ProtocolMessage.PlayerState
                || message instanceof ProtocolMessage.WorldState
                || message instanceof ProtocolMessage.PlayerList
                || message instanceof ProtocolMessage.Hello;
    }

    public void sendPlayerState(float x, float y, float z, float yaw, float pitch) throws IOException {
        requireClient().sendInput(new ProtocolMessage.PlayerPose(x, y, z, yaw, pitch));
    }

    public void sendPlayerState(float x, float y, float z, float yaw, float pitch,
            boolean onGround, boolean sneaking, float health, String heldItemId,
            int heldItemCount, int heldItemDamage, int selectedSlot, String gameMode) throws IOException {
        sendPlayerState(x, y, z, yaw, pitch, onGround, sneaking, health, heldItemId,
                heldItemCount, heldItemDamage, selectedSlot, gameMode, Map.of());
    }

    public void sendPlayerState(float x, float y, float z, float yaw, float pitch,
            boolean onGround, boolean sneaking, float health, String heldItemId,
            int heldItemCount, int heldItemDamage, int selectedSlot, String gameMode,
            Map<String, String> data) throws IOException {
        sendPlayerState(x, y, z, yaw, pitch, false, false, false, false, !onGround,
                onGround, sneaking, health, heldItemId, heldItemCount, heldItemDamage,
                selectedSlot, gameMode, data);
    }

    public void sendPlayerState(float x, float y, float z, float yaw, float pitch,
            boolean forward, boolean backward, boolean left, boolean right, boolean jumping,
            boolean onGround, boolean sneaking, float health, String heldItemId,
            int heldItemCount, int heldItemDamage, int selectedSlot, String gameMode,
            Map<String, String> data) throws IOException {
        requireClient().sendInput(
                new ProtocolMessage.PlayerPose(x, y, z, yaw, pitch),
                forward,
                backward,
                left,
                right,
                jumping,
                sneaking,
                onGround,
                health,
                heldItemId,
                heldItemCount,
                heldItemDamage,
                selectedSlot,
                gameMode,
                data
        );
    }

    public void sendInventoryUpdate(int slot, String itemId, int count, int damage) throws IOException {
        sendInventoryUpdate(slot, itemId, count, damage, Map.of());
    }

    public void sendInventoryUpdate(int slot, String itemId, int count, int damage, Map<String, String> data)
            throws IOException {
        requireClient().sendInventoryUpdate(slot, itemId, count, damage, data);
    }

    public void sendBedSleepStart() throws IOException {
        requireClient().sendClientAction(MultiplayerProtocol.ACTION_BED_SLEEP_START, Map.of());
    }

    public void sendBedSleepStop() throws IOException {
        requireClient().sendClientAction(MultiplayerProtocol.ACTION_BED_SLEEP_STOP, Map.of());
    }

    public void sendChat(String text) throws IOException {
        requireClient().sendChat(text);
    }

    public void addListener(Consumer<NetworkMessage> listener) {
        if (listener != null) {
            listeners.add(listener);
        }
    }

    public boolean isConnected() {
        ProtocolClient client = protocolClient;
        return client != null && client.isConnected();
    }

    public int clientId() {
        return clientId;
    }

    public boolean awaitHello(long timeoutMillis) {
        long deadline = System.currentTimeMillis() + Math.max(0L, timeoutMillis);
        while (isConnected() && clientId < 0 && System.currentTimeMillis() <= deadline) {
            try {
                Thread.sleep(10L);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        return isConnected() && clientId >= 0;
    }

    public long seed() {
        return seed;
    }

    public float worldTime() {
        return worldTime;
    }

    public String worldWeather() {
        return worldWeather;
    }

    public int worldSpawnX() {
        return worldSpawnX;
    }

    public int worldSpawnY() {
        return worldSpawnY;
    }

    public int worldSpawnZ() {
        return worldSpawnZ;
    }

    public String worldGameMode() {
        return worldGameMode;
    }

    public String worldDifficulty() {
        return worldDifficulty;
    }

    public boolean worldHardcore() {
        return worldHardcore;
    }

    public boolean worldAllowCheats() {
        return worldAllowCheats;
    }

    public boolean worldPvp() {
        return worldPvp;
    }

    public boolean worldSpawnAnimals() {
        return worldSpawnAnimals;
    }

    public boolean worldSpawnMonsters() {
        return worldSpawnMonsters;
    }

    public boolean worldSpawnNpcs() {
        return worldSpawnNpcs;
    }

    public boolean worldAllowNether() {
        return worldAllowNether;
    }

    public boolean worldAllowFlight() {
        return worldAllowFlight;
    }

    public String worldDimension() {
        return worldDimension;
    }

    public int worldMaxPlayers() {
        return worldMaxPlayers;
    }

    public int worldViewDistance() {
        return worldViewDistance;
    }

    public int worldMaxBuildHeight() {
        return worldMaxBuildHeight;
    }

    public boolean worldGenerateStructures() {
        return worldGenerateStructures;
    }

    @Override
    public void close() {
        ProtocolClient client = protocolClient;
        protocolClient = null;
        if (client != null) {
            if (client.isConnected() && client.getDisconnect() == null) {
                client.disconnect("Disconnected");
            } else {
                client.close();
            }
        }
        resetConnectionState(username);
    }

    private ProtocolClient requireClient() throws IOException {
        ProtocolClient client = protocolClient;
        if (client == null || !client.isConnected()) {
            throw new IOException("Client is not connected");
        }
        return client;
    }

    private void handleProtocolMessage(ProtocolMessage message) {
        if (message instanceof ProtocolMessage.Hello hello) {
            clientId = MultiplayerServer.legacyClientId(hello.playerId());
        } else if (message instanceof ProtocolMessage.WorldState worldState) {
            applyWorldState(worldState);
        } else if (isInitialSnapshotComplete(message)) {
            initialSnapshotComplete = true;
        }

        boolean remoteDisconnect = message instanceof ProtocolMessage.Disconnect;
        if (remoteDisconnect) {
            initialSnapshotComplete = false;
        }

        notifyListeners(MultiplayerServer.toNetworkMessage(message));
        if (message instanceof ProtocolMessage.WorldState worldState) {
            replaySnapshotMessages(worldState);
        }
        if (remoteDisconnect) {
            close();
        }
    }

    private void applyInitialSync(ProtocolMessage.Hello hello, ProtocolMessage.WorldState worldState) {
        clientId = MultiplayerServer.legacyClientId(hello.playerId());
        applyWorldState(worldState);
    }

    private void awaitJoinAccepted(ProtocolClient client, ProtocolMessage.Hello hello)
            throws IOException, InterruptedException {
        long deadline = System.nanoTime() + INITIAL_SYNC_TIMEOUT.toNanos();
        String playerId = hello == null ? "" : hello.playerId();
        while (System.nanoTime() <= deadline) {
            ProtocolMessage.Disconnect currentDisconnect = client.getDisconnect();
            if (currentDisconnect != null) {
                throw new IOException(currentDisconnect.reason());
            }
            if (!client.isConnected()) {
                throw new IOException("Disconnected while joining server");
            }
            long remaining = Math.max(1L, deadline - System.nanoTime());
            ProtocolMessage message = client.waitForMessage(ProtocolMessage.class, Duration.ofNanos(remaining));
            if (message == null) {
                break;
            }
            if (message instanceof ProtocolMessage.Disconnect disconnect) {
                throw new IOException(disconnect.reason());
            }
            if (message instanceof ProtocolMessage.PlayerList playerList && playerListContains(playerList, playerId)) {
                return;
            }
        }
        throw new IOException("Timed out waiting for multiplayer join acceptance");
    }

    private void awaitInitialSnapshotComplete(ProtocolClient client) throws IOException, InterruptedException {
        if (initialSnapshotComplete) {
            return;
        }
        long deadline = System.nanoTime() + SNAPSHOT_SYNC_TIMEOUT.toNanos();
        while (System.nanoTime() <= deadline) {
            ProtocolMessage.Disconnect currentDisconnect = client.getDisconnect();
            if (currentDisconnect != null) {
                throw new IOException(currentDisconnect.reason());
            }
            if (!client.isConnected()) {
                throw new IOException("Disconnected during multiplayer snapshot sync");
            }
            long remaining = Math.max(1L, deadline - System.nanoTime());
            ProtocolMessage message = client.waitForMessage(ProtocolMessage.Type.WORLD_EVENT,
                    Duration.ofNanos(remaining));
            if (message == null) {
                break;
            }
            if (isInitialSnapshotComplete(message)) {
                initialSnapshotComplete = true;
                return;
            }
        }
        throw new IOException("Timed out waiting for multiplayer snapshot sync");
    }

    private static boolean isInitialSnapshotComplete(ProtocolMessage message) {
        return message instanceof ProtocolMessage.WorldEvent event
                && MultiplayerProtocol.EVENT_INITIAL_SYNC_COMPLETE.equals(event.eventType());
    }

    private static boolean playerListContains(ProtocolMessage.PlayerList playerList, String playerId) {
        if (playerList == null || playerId == null || playerId.isBlank()) {
            return false;
        }
        for (ProtocolMessage.PlayerListEntry entry : playerList.players()) {
            if (playerId.equals(entry.playerId())) {
                return true;
            }
        }
        return false;
    }

    private void applyWorldState(ProtocolMessage.WorldState worldState) {
        seed = worldState.seed();
        worldTime = (float) worldState.timeOfDay();
        worldWeather = worldState.weatherState();
        worldSpawnX = worldState.spawnX();
        worldSpawnY = worldState.spawnY();
        worldSpawnZ = worldState.spawnZ();
        worldGameMode = worldState.gameMode();
        worldDifficulty = worldState.difficulty();
        worldHardcore = worldState.hardcore();
        worldAllowCheats = worldState.allowCheats();
        worldPvp = worldState.pvp();
        worldSpawnAnimals = worldState.spawnAnimals();
        worldSpawnMonsters = worldState.spawnMonsters();
        worldSpawnNpcs = worldState.spawnNpcs();
        worldAllowNether = worldState.allowNether();
        worldAllowFlight = worldState.allowFlight();
        worldDimension = worldState.dimension();
        worldMaxPlayers = worldState.maxPlayers();
        worldViewDistance = worldState.viewDistance();
        worldMaxBuildHeight = worldState.maxBuildHeight();
        worldGenerateStructures = worldState.generateStructures();
    }

    private void replaySnapshotMessages(ProtocolMessage.WorldState worldState) {
        for (ProtocolMessage.PlayerState playerState : worldState.players()) {
            notifyListeners(MultiplayerServer.toNetworkMessage(playerState));
        }
        for (ProtocolMessage.BlockUpdate blockUpdate : worldState.blockUpdates()) {
            notifyListeners(MultiplayerServer.toNetworkMessage(blockUpdate));
        }
        for (ProtocolMessage.EntityUpdate entityUpdate : worldState.entityUpdates()) {
            notifyListeners(MultiplayerServer.toNetworkMessage(entityUpdate));
        }
        for (ProtocolMessage.InventoryUpdate inventoryUpdate : worldState.inventoryUpdates()) {
            notifyListeners(MultiplayerServer.toNetworkMessage(inventoryUpdate));
        }
    }

    private void notifyListeners(NetworkMessage networkMessage) {
        if (networkMessage == null) {
            return;
        }
        for (Consumer<NetworkMessage> listener : listeners) {
            listener.accept(networkMessage);
        }
    }

    private static String normalizeHost(String host) {
        return host == null || host.isBlank() ? "127.0.0.1" : host;
    }

    private void resetConnectionState(String username) {
        this.username = username == null || username.isBlank() ? "Player" : username;
        clientId = -1;
        seed = 0L;
        worldTime = 0.0f;
        worldWeather = "clear";
        worldSpawnX = 0;
        worldSpawnY = 80;
        worldSpawnZ = 0;
        worldGameMode = "SURVIVAL";
        worldDifficulty = "EASY";
        worldHardcore = false;
        worldAllowCheats = false;
        worldPvp = true;
        worldSpawnAnimals = true;
        worldSpawnMonsters = true;
        worldSpawnNpcs = true;
        worldAllowNether = true;
        worldAllowFlight = false;
        worldDimension = "overworld";
        worldMaxPlayers = MultiplayerProtocol.DEFAULT_MAX_PLAYERS;
        worldViewDistance = MultiplayerProtocol.DEFAULT_VIEW_DISTANCE;
        worldMaxBuildHeight = MultiplayerProtocol.DEFAULT_MAX_BUILD_HEIGHT;
        worldGenerateStructures = MultiplayerProtocol.DEFAULT_GENERATE_STRUCTURES;
        initialSnapshotComplete = false;
    }
}
