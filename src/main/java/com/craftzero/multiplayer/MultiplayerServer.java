package com.craftzero.multiplayer;

import com.google.gson.JsonObject;

import java.io.Closeable;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Compatibility facade for the live runtime, backed by the typed protocol.
 */
public class MultiplayerServer implements Closeable {
    public static final int DEFAULT_PORT = MultiplayerProtocol.DEFAULT_PORT;

    private final int port;
    private final long seed;
    private volatile float worldTime;
    private final List<Consumer<NetworkMessage>> listeners = new CopyOnWriteArrayList<>();
    private final Map<Integer, JsonObject> playerStates = new ConcurrentHashMap<>();
    private final Map<String, String> playerNames = new ConcurrentHashMap<>();
    private volatile ProtocolServer protocolServer;

    public MultiplayerServer(int port, long seed, float worldTime) {
        this.port = port;
        this.seed = seed;
        this.worldTime = worldTime;
    }

    public void start() throws IOException {
        if (protocolServer != null && protocolServer.isRunning()) {
            return;
        }

        ProtocolServer server = new ProtocolServer(port, seed, worldTime);
        server.addListener(this::handleProtocolMessage);
        server.start();
        protocolServer = server;
    }

    public void broadcastWorldState(float time) {
        this.worldTime = time;
        ProtocolServer server = protocolServer;
        if (server == null || !server.isRunning()) {
            return;
        }
        server.setWorldTime(time);
        server.broadcast(new ProtocolMessage.WorldState(seed, time, server.currentPlayers()));
    }

    public void broadcastBlockUpdate(int x, int y, int z, int blockId, int metadata) {
        ProtocolServer server = protocolServer;
        if (server != null && server.isRunning()) {
            server.broadcastBlockUpdate(new ProtocolMessage.BlockUpdate(
                    x,
                    y,
                    z,
                    Integer.toString(blockId),
                    metadata,
                    ""
            ));
        }
    }

    public void broadcastChat(String sender, String text) {
        ProtocolServer server = protocolServer;
        if (server != null && server.isRunning()) {
            server.broadcast(new ProtocolMessage.Chat("", normalizeSender(sender, "Server"), text));
        }
    }

    public void broadcast(NetworkMessage message) {
        ProtocolMessage protocolMessage = toProtocolMessage(message);
        ProtocolServer server = protocolServer;
        if (protocolMessage != null && server != null && server.isRunning()) {
            server.broadcast(protocolMessage);
        }
    }

    public void addListener(Consumer<NetworkMessage> listener) {
        if (listener != null) {
            listeners.add(listener);
        }
    }

    public int clientCount() {
        ProtocolServer server = protocolServer;
        return server == null ? 0 : server.connectedPlayerCount();
    }

    public boolean disconnectClient(int clientId, String reason) {
        ProtocolServer server = protocolServer;
        if (server == null) {
            return false;
        }
        String playerId = protocolPlayerId(clientId);
        boolean disconnected = server.disconnectPlayer(playerId, reason);
        if (disconnected) {
            playerStates.remove(clientId);
            playerNames.remove(playerId);
        }
        return disconnected;
    }

    public int getPort() {
        ProtocolServer server = protocolServer;
        return server == null ? port : server.getPort();
    }

    public Map<Integer, JsonObject> playerStates() {
        return Map.copyOf(playerStates);
    }

    @Override
    public void close() {
        ProtocolServer server = protocolServer;
        protocolServer = null;
        if (server != null) {
            server.close();
        }
        playerStates.clear();
        playerNames.clear();
    }

    private void handleProtocolMessage(String playerId, ProtocolMessage message) {
        int clientId = legacyClientId(playerId);
        if (message instanceof ProtocolMessage.Join join) {
            playerNames.put(playerId, join.username());
            return;
        }
        if (message instanceof ProtocolMessage.ClientInput input) {
            if (clientId > 0) {
                playerStates.put(clientId, playerStateData(playerId, clientId, input));
            }
            return;
        }
        if (message instanceof ProtocolMessage.Chat chat) {
            NetworkMessage networkMessage = toNetworkMessage(chat.withSender(
                    normalizeSender(chat.sender(), playerNames.get(playerId))
            ));
            notifyListeners(networkMessage);
            return;
        }
        if (message instanceof ProtocolMessage.Disconnect) {
            if (clientId > 0) {
                playerStates.remove(clientId);
            }
            playerNames.remove(playerId);
        }
    }

    private JsonObject playerStateData(String playerId, int clientId, ProtocolMessage.ClientInput input) {
        JsonObject data = NetworkMessage.object();
        data.addProperty("clientId", clientId);
        data.addProperty("playerId", playerId);
        data.addProperty("username", playerNames.getOrDefault(playerId, "Player" + clientId));
        data.addProperty("x", input.pose().x());
        data.addProperty("y", input.pose().y());
        data.addProperty("z", input.pose().z());
        data.addProperty("yaw", input.pose().yaw());
        data.addProperty("pitch", input.pose().pitch());
        data.addProperty("onGround", !input.jumping());
        return data;
    }

    private void notifyListeners(NetworkMessage message) {
        for (Consumer<NetworkMessage> listener : listeners) {
            listener.accept(message);
        }
    }

    static ProtocolMessage toProtocolMessage(NetworkMessage message) {
        if (message == null || message.type() == null) {
            return null;
        }
        JsonObject data = message.data() == null ? NetworkMessage.object() : message.data();
        return switch (message.type()) {
            case "chat" -> new ProtocolMessage.Chat(
                    string(data, "playerId", ""),
                    string(data, "sender", "Server"),
                    string(data, "text", "")
            );
            case "worldState" -> new ProtocolMessage.WorldState(
                    longNumber(data, "seed", 0L),
                    decimal(data, "time", 0.0f),
                    List.of()
            );
            case "blockUpdate", "blockAction" -> new ProtocolMessage.BlockUpdate(
                    integer(data, "x", 0),
                    integer(data, "y", 0),
                    integer(data, "z", 0),
                    string(data, "blockId", "0"),
                    integer(data, "metadata", 0),
                    string(data, "playerId", "")
            );
            case "entityUpdate", "entityAction" -> new ProtocolMessage.EntityUpdate(
                    string(data, "entityId", ""),
                    string(data, "entityType", "entity"),
                    pose(data),
                    Map.of()
            );
            case "inventoryUpdate", "inventoryAction", "containerAction" -> new ProtocolMessage.InventoryUpdate(
                    string(data, "playerId", ""),
                    integer(data, "slot", 0),
                    string(data, "itemId", "air"),
                    integer(data, "count", 0),
                    integer(data, "damage", 0)
            );
            case "disconnect" -> new ProtocolMessage.Disconnect(
                    string(data, "playerId", ""),
                    string(data, "reason", "Disconnected")
            );
            default -> null;
        };
    }

    static NetworkMessage toNetworkMessage(ProtocolMessage message) {
        JsonObject data = NetworkMessage.object();
        if (message instanceof ProtocolMessage.Hello hello) {
            data.addProperty("clientId", legacyClientId(hello.playerId()));
            data.addProperty("playerId", hello.playerId());
            data.addProperty("protocolVersion", hello.protocolVersion());
            data.addProperty("serverName", hello.serverName());
            return NetworkMessage.of("hello", data);
        }
        if (message instanceof ProtocolMessage.WorldState worldState) {
            data.addProperty("seed", worldState.seed());
            data.addProperty("time", worldState.timeOfDay());
            return NetworkMessage.of("worldState", data);
        }
        if (message instanceof ProtocolMessage.PlayerState playerState) {
            int clientId = legacyClientId(playerState.playerId());
            data.addProperty("clientId", clientId);
            data.addProperty("playerId", playerState.playerId());
            data.addProperty("username", playerState.username());
            data.addProperty("x", playerState.pose().x());
            data.addProperty("y", playerState.pose().y());
            data.addProperty("z", playerState.pose().z());
            data.addProperty("yaw", playerState.pose().yaw());
            data.addProperty("pitch", playerState.pose().pitch());
            data.addProperty("onGround", playerState.onGround());
            return NetworkMessage.of("playerState", data);
        }
        if (message instanceof ProtocolMessage.BlockUpdate blockUpdate) {
            data.addProperty("x", blockUpdate.x());
            data.addProperty("y", blockUpdate.y());
            data.addProperty("z", blockUpdate.z());
            data.addProperty("blockId", blockUpdate.blockId());
            data.addProperty("metadata", blockUpdate.metadata());
            data.addProperty("playerId", blockUpdate.sourcePlayerId());
            data.addProperty("clientId", legacyClientId(blockUpdate.sourcePlayerId()));
            return NetworkMessage.of("blockUpdate", data);
        }
        if (message instanceof ProtocolMessage.EntityUpdate entityUpdate) {
            data.addProperty("entityId", entityUpdate.entityId());
            data.addProperty("entityType", entityUpdate.entityType());
            addPose(data, entityUpdate.pose());
            return NetworkMessage.of("entityUpdate", data);
        }
        if (message instanceof ProtocolMessage.InventoryUpdate inventoryUpdate) {
            data.addProperty("playerId", inventoryUpdate.playerId());
            data.addProperty("clientId", legacyClientId(inventoryUpdate.playerId()));
            data.addProperty("slot", inventoryUpdate.slot());
            data.addProperty("itemId", inventoryUpdate.itemId());
            data.addProperty("count", inventoryUpdate.count());
            data.addProperty("damage", inventoryUpdate.damage());
            return NetworkMessage.of("inventoryUpdate", data);
        }
        if (message instanceof ProtocolMessage.Chat chat) {
            data.addProperty("playerId", chat.playerId());
            data.addProperty("clientId", legacyClientId(chat.playerId()));
            data.addProperty("sender", chat.sender());
            data.addProperty("text", chat.text());
            return NetworkMessage.of("chat", data);
        }
        if (message instanceof ProtocolMessage.Disconnect disconnect) {
            data.addProperty("playerId", disconnect.playerId());
            data.addProperty("clientId", legacyClientId(disconnect.playerId()));
            data.addProperty("reason", disconnect.reason());
            return NetworkMessage.of("disconnect", data);
        }
        return NetworkMessage.of(message.type(), data);
    }

    private static ProtocolMessage.PlayerPose pose(JsonObject data) {
        return new ProtocolMessage.PlayerPose(
                decimal(data, "x", 0.0),
                decimal(data, "y", 0.0),
                decimal(data, "z", 0.0),
                (float) decimal(data, "yaw", 0.0),
                (float) decimal(data, "pitch", 0.0)
        );
    }

    private static void addPose(JsonObject data, ProtocolMessage.PlayerPose pose) {
        data.addProperty("x", pose.x());
        data.addProperty("y", pose.y());
        data.addProperty("z", pose.z());
        data.addProperty("yaw", pose.yaw());
        data.addProperty("pitch", pose.pitch());
    }

    private static String normalizeSender(String sender, String fallback) {
        if (sender == null || sender.isBlank() || "Player".equals(sender)) {
            return fallback == null || fallback.isBlank() ? "Player" : fallback;
        }
        return sender;
    }

    private static String protocolPlayerId(int clientId) {
        return "player-" + clientId;
    }

    static int legacyClientId(String playerId) {
        if (playerId == null || playerId.isBlank()) {
            return -1;
        }
        String normalized = playerId.trim().toLowerCase(java.util.Locale.ROOT);
        if (normalized.startsWith("player-")) {
            normalized = normalized.substring("player-".length());
        } else if (normalized.startsWith("player")) {
            normalized = normalized.substring("player".length());
        }
        try {
            return Integer.parseInt(normalized);
        } catch (NumberFormatException ignored) {
            return -1;
        }
    }

    private static String string(JsonObject data, String key, String fallback) {
        return data.has(key) && !data.get(key).isJsonNull() ? data.get(key).getAsString() : fallback;
    }

    private static int integer(JsonObject data, String key, int fallback) {
        return data.has(key) && !data.get(key).isJsonNull() ? data.get(key).getAsInt() : fallback;
    }

    private static long longNumber(JsonObject data, String key, long fallback) {
        return data.has(key) && !data.get(key).isJsonNull() ? data.get(key).getAsLong() : fallback;
    }

    private static double decimal(JsonObject data, String key, double fallback) {
        return data.has(key) && !data.get(key).isJsonNull() ? data.get(key).getAsDouble() : fallback;
    }
}
