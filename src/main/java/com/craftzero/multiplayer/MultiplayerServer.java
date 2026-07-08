package com.craftzero.multiplayer;

import com.craftzero.world.WorldSoundEvent;
import com.craftzero.world.WorldParticle;
import com.craftzero.world.WorldLightningBolt;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.io.Closeable;
import java.io.IOException;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * Compatibility facade for the live runtime, backed by the typed protocol.
 */
public class MultiplayerServer implements Closeable {
    public static final int DEFAULT_PORT = MultiplayerProtocol.DEFAULT_PORT;
    private static final String ONLINE_MODE_AUTH_REQUIRED_REASON = "Online-mode authentication is required";

    public record ConnectedPlayer(int clientId, String playerId, String username, int latencyMillis,
            String remoteAddress) {
    }

    private final int port;
    private final String bindAddress;
    private final long seed;
    private final String serverName;
    private volatile float worldTime;
    private final List<Consumer<NetworkMessage>> listeners = new CopyOnWriteArrayList<>();
    private final Map<Integer, JsonObject> playerStates = new ConcurrentHashMap<>();
    private final Map<String, String> playerNames = new ConcurrentHashMap<>();
    private final Set<String> sleepingPlayers = ConcurrentHashMap.newKeySet();
    private final AtomicBoolean sleepCompletePending = new AtomicBoolean(false);
    private volatile int spawnX;
    private volatile int spawnY = 80;
    private volatile int spawnZ;
    private volatile String gameMode = "SURVIVAL";
    private volatile String difficulty = "EASY";
    private volatile boolean hardcore;
    private volatile boolean allowCheats;
    private volatile boolean pvp = true;
    private volatile boolean spawnAnimals = true;
    private volatile boolean spawnMonsters = true;
    private volatile boolean spawnNpcs = true;
    private volatile boolean allowNether = true;
    private volatile boolean allowFlight;
    private volatile String dimension = "overworld";
    private volatile int maxPlayers = MultiplayerProtocol.DEFAULT_MAX_PLAYERS;
    private volatile int viewDistance = MultiplayerProtocol.DEFAULT_VIEW_DISTANCE;
    private volatile int maxBuildHeight = MultiplayerProtocol.DEFAULT_MAX_BUILD_HEIGHT;
    private volatile boolean generateStructures = MultiplayerProtocol.DEFAULT_GENERATE_STRUCTURES;
    private volatile Set<String> bannedPlayers = Set.of();
    private volatile Set<String> bannedIps = Set.of();
    private volatile Set<String> whitelist = Set.of();
    private volatile boolean whitelistEnabled;
    private volatile boolean onlineMode;
    private volatile boolean queryEnabled;
    private volatile int queryPort = MultiplayerProtocol.DEFAULT_QUERY_PORT;
    private volatile ProtocolServer protocolServer;
    private volatile boolean hostSleeping;

    public MultiplayerServer(int port, long seed, float worldTime) {
        this("", port, seed, worldTime, "CraftZero");
    }

    public MultiplayerServer(int port, long seed, float worldTime, String serverName) {
        this("", port, seed, worldTime, serverName);
    }

    public MultiplayerServer(String bindAddress, int port, long seed, float worldTime, String serverName) {
        this.port = port;
        this.bindAddress = normalizeBindAddress(bindAddress);
        this.seed = seed;
        this.worldTime = worldTime;
        this.serverName = serverName == null || serverName.isBlank() ? "CraftZero" : serverName.trim();
    }

    public void start() throws IOException {
        if (protocolServer != null && protocolServer.isRunning()) {
            return;
        }

        ProtocolServer server = new ProtocolServer(bindAddress, port, seed, worldTime, serverName);
        server.addListener(this::handleProtocolMessage);
        applyWorldMetadata(server);
        applyQuerySettings(server);
        server.setJoinAdmission(this::joinRejectReason);
        server.start();
        protocolServer = server;
    }

    public void broadcastWorldState(float time) {
        broadcastWorldState(time, "clear");
    }

    public void broadcastWorldState(float time, String weatherState) {
        this.worldTime = time;
        ProtocolServer server = protocolServer;
        if (server == null || !server.isRunning()) {
            return;
        }
        server.setWorldTime(time);
        server.setWorldWeather(weatherState);
        server.broadcastWorldState(time, weatherState);
    }

    public void configureWorldMetadata(int spawnX, int spawnY, int spawnZ, String gameMode, String difficulty,
            boolean hardcore, boolean allowCheats, String dimension, int maxPlayers) {
        configureWorldMetadata(spawnX, spawnY, spawnZ, gameMode, difficulty, hardcore, allowCheats,
                true, true, true, true, true, false, dimension, maxPlayers, MultiplayerProtocol.DEFAULT_VIEW_DISTANCE);
    }

    public void configureWorldMetadata(int spawnX, int spawnY, int spawnZ, String gameMode, String difficulty,
            boolean hardcore, boolean allowCheats, boolean pvp, boolean spawnAnimals, boolean spawnMonsters,
            boolean allowNether, boolean allowFlight, String dimension, int maxPlayers) {
        configureWorldMetadata(spawnX, spawnY, spawnZ, gameMode, difficulty, hardcore, allowCheats,
                pvp, spawnAnimals, spawnMonsters, true, allowNether, allowFlight, dimension, maxPlayers,
                MultiplayerProtocol.DEFAULT_VIEW_DISTANCE);
    }

    public void configureWorldMetadata(int spawnX, int spawnY, int spawnZ, String gameMode, String difficulty,
            boolean hardcore, boolean allowCheats, boolean pvp, boolean spawnAnimals, boolean spawnMonsters,
            boolean allowNether, boolean allowFlight, String dimension, int maxPlayers, int viewDistance) {
        configureWorldMetadata(spawnX, spawnY, spawnZ, gameMode, difficulty, hardcore, allowCheats,
                pvp, spawnAnimals, spawnMonsters, true, allowNether, allowFlight, dimension, maxPlayers,
                viewDistance);
    }

    public void configureWorldMetadata(int spawnX, int spawnY, int spawnZ, String gameMode, String difficulty,
            boolean hardcore, boolean allowCheats, boolean pvp, boolean spawnAnimals, boolean spawnMonsters,
            boolean spawnNpcs, boolean allowNether, boolean allowFlight, String dimension, int maxPlayers,
            int viewDistance) {
        configureWorldMetadata(spawnX, spawnY, spawnZ, gameMode, difficulty, hardcore, allowCheats,
                pvp, spawnAnimals, spawnMonsters, spawnNpcs, allowNether, allowFlight, dimension, maxPlayers,
                viewDistance, MultiplayerProtocol.DEFAULT_MAX_BUILD_HEIGHT,
                MultiplayerProtocol.DEFAULT_GENERATE_STRUCTURES);
    }

    public void configureWorldMetadata(int spawnX, int spawnY, int spawnZ, String gameMode, String difficulty,
            boolean hardcore, boolean allowCheats, boolean pvp, boolean spawnAnimals, boolean spawnMonsters,
            boolean spawnNpcs, boolean allowNether, boolean allowFlight, String dimension, int maxPlayers,
            int viewDistance, int maxBuildHeight) {
        configureWorldMetadata(spawnX, spawnY, spawnZ, gameMode, difficulty, hardcore, allowCheats,
                pvp, spawnAnimals, spawnMonsters, spawnNpcs, allowNether, allowFlight, dimension, maxPlayers,
                viewDistance, maxBuildHeight, MultiplayerProtocol.DEFAULT_GENERATE_STRUCTURES);
    }

    public void configureWorldMetadata(int spawnX, int spawnY, int spawnZ, String gameMode, String difficulty,
            boolean hardcore, boolean allowCheats, boolean pvp, boolean spawnAnimals, boolean spawnMonsters,
            boolean spawnNpcs, boolean allowNether, boolean allowFlight, String dimension, int maxPlayers,
            int viewDistance, int maxBuildHeight, boolean generateStructures) {
        String normalizedDimension = normalizeMetadata(dimension, "overworld");
        boolean dimensionChanged = !normalizedDimension.equals(this.dimension);
        this.spawnX = spawnX;
        this.spawnY = spawnY;
        this.spawnZ = spawnZ;
        this.gameMode = normalizeMetadata(gameMode, "SURVIVAL");
        this.difficulty = normalizeMetadata(difficulty, "EASY");
        this.hardcore = hardcore;
        this.allowCheats = allowCheats;
        this.pvp = pvp;
        this.spawnAnimals = spawnAnimals;
        this.spawnMonsters = spawnMonsters;
        this.spawnNpcs = spawnNpcs;
        this.allowNether = allowNether;
        this.allowFlight = allowFlight;
        this.dimension = normalizedDimension;
        this.maxPlayers = Math.max(1, maxPlayers);
        this.viewDistance = clampViewDistance(viewDistance);
        this.maxBuildHeight = clampMaxBuildHeight(maxBuildHeight);
        this.generateStructures = generateStructures;
        if (dimensionChanged) {
            resetWorldSnapshotState();
        }
        ProtocolServer server = protocolServer;
        if (server != null) {
            applyWorldMetadata(server);
        }
    }

    public void configureAccessControl(Set<String> bannedPlayers, Set<String> bannedIps,
            Set<String> whitelist, boolean whitelistEnabled) {
        configureAccessControl(bannedPlayers, bannedIps, whitelist, whitelistEnabled, false);
    }

    public void configureAccessControl(Set<String> bannedPlayers, Set<String> bannedIps,
            Set<String> whitelist, boolean whitelistEnabled, boolean onlineMode) {
        this.bannedPlayers = normalizeNames(bannedPlayers);
        this.bannedIps = normalizeIps(bannedIps);
        this.whitelist = normalizeNames(whitelist);
        this.whitelistEnabled = whitelistEnabled;
        this.onlineMode = onlineMode;
        ProtocolServer server = protocolServer;
        if (server != null) {
            server.setJoinAdmission(this::joinRejectReason);
        }
    }

    public void configureQuery(boolean enabled, int port) {
        this.queryEnabled = enabled;
        this.queryPort = validPort(port);
        ProtocolServer server = protocolServer;
        if (server != null) {
            applyQuerySettings(server);
        }
    }

    public int enforceAccessControl(String reason) {
        ProtocolServer server = protocolServer;
        if (server == null || !server.isRunning()) {
            return 0;
        }
        String disconnectReason = reason == null || reason.isBlank() ? "Disconnected" : reason;
        int disconnected = 0;
        for (ConnectedPlayer player : connectedPlayers()) {
            if (!isDeniedByAccessControl(player)) {
                continue;
            }
            if (disconnectClient(player.clientId(), disconnectReason)) {
                disconnected++;
            }
        }
        return disconnected;
    }

    public void broadcastBlockUpdate(int x, int y, int z, int blockId, int metadata) {
        broadcastBlockUpdate(x, y, z, blockId, metadata, Map.of());
    }

    public void broadcastBlockUpdate(int x, int y, int z, int blockId, int metadata, Map<String, String> data) {
        ProtocolServer server = protocolServer;
        if (server != null && server.isRunning()) {
            server.broadcastBlockUpdate(new ProtocolMessage.BlockUpdate(
                    x,
                    y,
                    z,
                    Integer.toString(blockId),
                    metadata,
                    "",
                    data
            ));
        }
    }

    public void seedBlockState(int x, int y, int z, int blockId, int metadata) {
        seedBlockState(x, y, z, blockId, metadata, Map.of());
    }

    public void seedBlockState(int x, int y, int z, int blockId, int metadata, Map<String, String> data) {
        ProtocolServer server = protocolServer;
        if (server != null && server.isRunning()) {
            server.rememberBlockUpdate(new ProtocolMessage.BlockUpdate(
                    x,
                    y,
                    z,
                    Integer.toString(blockId),
                    metadata,
                    "",
                    data
            ));
        }
    }

    public void broadcastEntityUpdate(String entityId, String entityType,
            float x, float y, float z, float yaw, float pitch, Map<String, String> data) {
        ProtocolServer server = protocolServer;
        if (server != null && server.isRunning()) {
            server.broadcastEntityUpdate(new ProtocolMessage.EntityUpdate(
                    entityId,
                    entityType,
                    new ProtocolMessage.PlayerPose(x, y, z, yaw, pitch),
                    data
            ));
        }
    }

    public void seedEntityState(String entityId, String entityType,
            float x, float y, float z, float yaw, float pitch, Map<String, String> data) {
        ProtocolServer server = protocolServer;
        if (server != null && server.isRunning()) {
            server.rememberEntityUpdate(new ProtocolMessage.EntityUpdate(
                    entityId,
                    entityType,
                    new ProtocolMessage.PlayerPose(x, y, z, yaw, pitch),
                    data
            ));
        }
    }

    public void broadcastInventoryUpdate(String playerId, int slot, String itemId, int count, int damage) {
        broadcastInventoryUpdate(playerId, slot, itemId, count, damage, Map.of());
    }

    public void broadcastInventoryUpdate(String playerId, int slot, String itemId, int count, int damage,
            Map<String, String> data) {
        ProtocolServer server = protocolServer;
        if (server != null && server.isRunning()) {
            server.broadcastInventoryUpdate(new ProtocolMessage.InventoryUpdate(
                    playerId,
                    slot,
                    itemId,
                    count,
                    damage,
                    data
            ));
        }
    }

    public void broadcastWorldSound(WorldSoundEvent event) {
        if (event == null || event.soundId().isBlank()) {
            return;
        }
        ProtocolServer server = protocolServer;
        if (server != null && server.isRunning()) {
            server.broadcast(new ProtocolMessage.WorldEvent("sound", soundEventData(event)));
        }
    }

    public void broadcastWorldParticle(WorldParticle particle) {
        if (particle == null || particle.getType() == null) {
            return;
        }
        ProtocolServer server = protocolServer;
        if (server != null && server.isRunning()) {
            server.broadcast(new ProtocolMessage.WorldEvent("particle", particleEventData(particle)));
        }
    }

    public void broadcastWorldLightning(WorldLightningBolt bolt) {
        if (bolt == null) {
            return;
        }
        ProtocolServer server = protocolServer;
        if (server != null && server.isRunning()) {
            server.broadcast(new ProtocolMessage.WorldEvent("lightning", lightningEventData(bolt)));
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

    public boolean sendToClient(int clientId, NetworkMessage message) {
        if (clientId <= 0) {
            return false;
        }
        ProtocolMessage protocolMessage = toProtocolMessage(message);
        ProtocolServer server = protocolServer;
        return protocolMessage != null
                && server != null
                && server.isRunning()
                && server.sendTo(protocolPlayerId(clientId), protocolMessage);
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

    public boolean beginHostSleep() {
        hostSleeping = true;
        return tryCompleteAllPlayerSleep();
    }

    public void stopHostSleep() {
        hostSleeping = false;
    }

    public boolean hasPendingSleepCompletion() {
        return sleepCompletePending.get();
    }

    public boolean consumePendingSleepCompletion() {
        return sleepCompletePending.getAndSet(false);
    }

    public int sleepingPlayerCount() {
        int count = sleepingPlayers.size();
        return hostSleeping ? count + 1 : count;
    }

    public int sleepEligiblePlayerCount() {
        return clientCount() + 1;
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
            sleepingPlayers.remove(playerId);
            tryCompleteAllPlayerSleep();
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

    public List<ConnectedPlayer> connectedPlayers() {
        ProtocolServer server = protocolServer;
        return playerNames.entrySet().stream()
                .map(entry -> connectedPlayer(entry.getKey(), entry.getValue(),
                        server == null ? -1 : server.clientLatencyMillis(entry.getKey()),
                        server == null ? "" : server.clientRemoteAddress(entry.getKey())))
                .filter(player -> player.clientId() > 0)
                .sorted(Comparator.comparingInt(ConnectedPlayer::clientId))
                .toList();
    }

    public String usernameForClient(int clientId) {
        if (clientId <= 0) {
            return null;
        }
        String playerId = protocolPlayerId(clientId);
        String username = playerNames.get(playerId);
        return username == null ? null : normalizeSender(username, fallbackPlayerName(clientId));
    }

    public int clientIdForUsername(String username) {
        if (username == null || username.isBlank()) {
            return -1;
        }
        String normalized = normalizeName(username);
        for (ConnectedPlayer player : connectedPlayers()) {
            if (normalized.equals(normalizeName(player.username()))) {
                return player.clientId();
            }
        }
        return -1;
    }

    public List<NetworkMessage> blockStates() {
        ProtocolServer server = protocolServer;
        if (server == null) {
            return List.of();
        }
        return server.currentBlockUpdates().stream()
                .map(MultiplayerServer::toNetworkMessage)
                .toList();
    }

    public List<NetworkMessage> entityStates() {
        ProtocolServer server = protocolServer;
        if (server == null) {
            return List.of();
        }
        return server.currentEntityUpdates().stream()
                .map(MultiplayerServer::toNetworkMessage)
                .toList();
    }

    public List<NetworkMessage> inventoryStates() {
        ProtocolServer server = protocolServer;
        if (server == null) {
            return List.of();
        }
        return server.currentInventoryUpdates().stream()
                .map(MultiplayerServer::toNetworkMessage)
                .toList();
    }

    public NetworkMessage inventoryState(String playerId, int slot) {
        ProtocolMessage.InventoryUpdate update = currentInventoryUpdate(playerId, slot);
        return update == null ? null : toNetworkMessage(update);
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
        sleepingPlayers.clear();
        hostSleeping = false;
        sleepCompletePending.set(false);
    }

    private void handleProtocolMessage(String playerId, ProtocolMessage message) {
        int clientId = legacyClientId(playerId);
        if (message instanceof ProtocolMessage.Join join) {
            playerNames.put(playerId, join.username());
            announceSystemChat(join.username() + " joined the game");
            broadcastPlayerList();
            return;
        }
        if (message instanceof ProtocolMessage.ClientInput input) {
            if (clientId > 0) {
                playerStates.put(clientId, playerStateData(playerId, clientId, input));
            }
            return;
        }
        if (message instanceof ProtocolMessage.ClientAction action) {
            handleClientAction(playerId, action);
            if (MultiplayerProtocol.ACTION_INVENTORY_SYNC.equals(action.action())) {
                ProtocolMessage.InventoryUpdate inventoryUpdate = inventoryUpdateFromAction(playerId, action);
                if (inventoryUpdate != null) {
                    notifyListeners(toNetworkMessage(inventoryUpdate));
                    ProtocolServer server = protocolServer;
                    if (server != null && server.isRunning()) {
                        server.broadcastInventoryUpdate(inventoryUpdate);
                    }
                }
                return;
            }
            if (isBlockUpdateAction(action)) {
                notifyListeners(toNetworkMessage(action.blockUpdate().withSourcePlayerId(playerId)));
            }
            if (MultiplayerProtocol.ACTION_SIGN_UPDATE.equals(action.action())) {
                notifyListeners(toNetworkMessage(action));
            }
            if (isHostHandledClientAction(action.action())) {
                notifyListeners(toNetworkMessage(action));
            }
            return;
        }
        if (message instanceof ProtocolMessage.BlockUpdate) {
            return;
        }
        if (message instanceof ProtocolMessage.EntityUpdate) {
            return;
        }
        if (message instanceof ProtocolMessage.InventoryUpdate) {
            return;
        }
        if (message instanceof ProtocolMessage.Chat chat) {
            String sender = normalizeSender(playerNames.get(playerId),
                    clientId > 0 ? "Player" + clientId : "Player");
            String text = chat.text() == null ? "" : chat.text();
            if (text.trim().startsWith("/")) {
                JsonObject data = NetworkMessage.object();
                data.addProperty("playerId", playerId);
                data.addProperty("clientId", clientId);
                data.addProperty("sender", sender);
                data.addProperty("text", text);
                notifyListeners(NetworkMessage.of("serverCommand", data));
            } else {
                notifyListeners(toNetworkMessage(chat.withSender(sender)));
            }
            return;
        }
        if (message instanceof ProtocolMessage.Disconnect) {
            String username = playerNames.getOrDefault(playerId, clientId > 0 ? "Player" + clientId : "Player");
            if (clientId > 0) {
                playerStates.remove(clientId);
            }
            playerNames.remove(playerId);
            sleepingPlayers.remove(playerId);
            announceSystemChat(username + " left the game");
            broadcastPlayerList();
        }
    }

    public void seedInventoryState(String playerId, int slot, String itemId, int count, int damage) {
        seedInventoryState(playerId, slot, itemId, count, damage, Map.of());
    }

    public void seedInventoryState(String playerId, int slot, String itemId, int count, int damage,
            Map<String, String> data) {
        ProtocolServer server = protocolServer;
        if (server != null && server.isRunning()) {
            server.rememberInventoryUpdate(new ProtocolMessage.InventoryUpdate(
                    playerId,
                    slot,
                    itemId,
                    count,
                    damage,
                    data
            ));
        }
    }

    public void seedPlayerState(String playerId, String username, float x, float y, float z,
            float yaw, float pitch, boolean onGround, boolean sneaking, float health,
            String heldItemId, int heldItemCount, int heldItemDamage, int selectedSlot, String gameMode) {
        seedPlayerState(playerId, username, x, y, z, yaw, pitch, onGround, sneaking, health,
                heldItemId, heldItemCount, heldItemDamage, selectedSlot, gameMode, Map.of());
    }

    public void seedPlayerState(String playerId, String username, float x, float y, float z,
            float yaw, float pitch, boolean onGround, boolean sneaking, float health,
            String heldItemId, int heldItemCount, int heldItemDamage, int selectedSlot, String gameMode,
            Map<String, String> data) {
        ProtocolServer server = protocolServer;
        if (server != null && server.isRunning()) {
            server.rememberPlayerState(new ProtocolMessage.PlayerState(
                    playerId,
                    username,
                    new ProtocolMessage.PlayerPose(x, y, z, yaw, pitch),
                    onGround,
                    sneaking,
                    health,
                    heldItemId,
                    heldItemCount,
                    heldItemDamage,
                    selectedSlot,
                    gameMode,
                    data
            ));
        }
    }

    public void broadcastPlayerState(String playerId, String username, float x, float y, float z,
            float yaw, float pitch, boolean onGround, boolean sneaking, float health,
            String heldItemId, int heldItemCount, int heldItemDamage, int selectedSlot, String gameMode) {
        broadcastPlayerState(playerId, username, x, y, z, yaw, pitch, onGround, sneaking, health,
                heldItemId, heldItemCount, heldItemDamage, selectedSlot, gameMode, Map.of());
    }

    public void broadcastPlayerState(String playerId, String username, float x, float y, float z,
            float yaw, float pitch, boolean onGround, boolean sneaking, float health,
            String heldItemId, int heldItemCount, int heldItemDamage, int selectedSlot, String gameMode,
            Map<String, String> data) {
        ProtocolServer server = protocolServer;
        if (server != null && server.isRunning()) {
            server.broadcast(new ProtocolMessage.PlayerState(
                    playerId,
                    username,
                    new ProtocolMessage.PlayerPose(x, y, z, yaw, pitch),
                    onGround,
                    sneaking,
                    health,
                    heldItemId,
                    heldItemCount,
                    heldItemDamage,
                    selectedSlot,
                    gameMode,
                    data
            ));
        }
    }

    public void resetWorldSnapshotState() {
        playerStates.clear();
        sleepingPlayers.clear();
        hostSleeping = false;
        sleepCompletePending.set(false);
        ProtocolServer server = protocolServer;
        if (server != null && server.isRunning()) {
            server.clearWorldSnapshotState();
        }
    }

    private void handleClientAction(String playerId, ProtocolMessage.ClientAction action) {
        if (MultiplayerProtocol.ACTION_BED_SLEEP_START.equals(action.action())) {
            sleepingPlayers.add(playerId);
            tryCompleteAllPlayerSleep();
        } else if (MultiplayerProtocol.ACTION_BED_SLEEP_STOP.equals(action.action())) {
            sleepingPlayers.remove(playerId);
        }
    }

    private static ProtocolMessage.InventoryUpdate inventoryUpdateFromAction(String playerId,
            ProtocolMessage.ClientAction action) {
        if (action == null || action.data() == null) {
            return null;
        }
        Map<String, String> data = action.data();
        int slot;
        int count;
        int damage;
        try {
            slot = Integer.parseInt(data.getOrDefault("slot", "-1"));
            count = Integer.parseInt(data.getOrDefault("count", "-1"));
            damage = Integer.parseInt(data.getOrDefault("damage",
                    Integer.toString(MultiplayerProtocol.MIN_ITEM_DAMAGE)));
        } catch (NumberFormatException ignored) {
            return null;
        }
        String itemId = data.getOrDefault("itemId", "air");
        if (!MultiplayerProtocol.isValidInventoryUpdate(itemId, slot, count, damage)) {
            return null;
        }
        LinkedHashMap<String, String> stackData = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : data.entrySet()) {
            String key = entry.getKey();
            if (key != null && key.startsWith("stack.")) {
                stackData.put(key, entry.getValue());
            }
        }
        if (!MultiplayerProtocol.isValidItemStackData(stackData)) {
            return null;
        }
        return new ProtocolMessage.InventoryUpdate(playerId, slot, itemId, count, damage, stackData);
    }

    private static boolean isEntityAction(String action) {
        return MultiplayerProtocol.ACTION_ENTITY_ATTACK.equals(action)
                || MultiplayerProtocol.ACTION_ENTITY_USE.equals(action);
    }

    private static boolean isBlockUpdateAction(ProtocolMessage.ClientAction action) {
        return action != null
                && action.blockUpdate() != null
                && ProtocolMessage.Type.BLOCK_UPDATE.equals(action.action())
                && action.data().isEmpty();
    }

    private static boolean isHostHandledClientAction(String action) {
        return isEntityAction(action)
                || MultiplayerProtocol.ACTION_PLAYER_ATTACK.equals(action)
                || MultiplayerProtocol.ACTION_PLAYER_RESPAWN.equals(action)
                || MultiplayerProtocol.ACTION_ITEM_USE.equals(action)
                || MultiplayerProtocol.ACTION_CONTAINER_UPDATE.equals(action)
                || MultiplayerProtocol.ACTION_ENCHANT_ITEM.equals(action)
                || MultiplayerProtocol.ACTION_CRAFT_ITEM.equals(action);
    }

    private String joinRejectReason(String username, String remoteAddress) {
        String normalizedName = normalizeName(username);
        String normalizedAddress = normalizeIp(remoteAddress);
        if (bannedPlayers.contains(normalizedName)) {
            return "You are banned from this server";
        }
        if (!normalizedAddress.isBlank() && bannedIps.contains(normalizedAddress)) {
            return "Your IP address is banned from this server";
        }
        if (whitelistEnabled && !whitelist.contains(normalizedName)) {
            return "You are not white-listed on this server";
        }
        if (onlineMode) {
            return ONLINE_MODE_AUTH_REQUIRED_REASON;
        }
        return null;
    }

    private boolean isDeniedByAccessControl(ConnectedPlayer player) {
        if (player == null) {
            return false;
        }
        String normalizedName = normalizeName(player.username());
        String normalizedAddress = normalizeIp(player.remoteAddress());
        if (!normalizedName.isBlank() && bannedPlayers.contains(normalizedName)) {
            return true;
        }
        if (!normalizedAddress.isBlank() && bannedIps.contains(normalizedAddress)) {
            return true;
        }
        return onlineMode || (whitelistEnabled && !whitelist.contains(normalizedName));
    }

    private static Set<String> normalizeNames(Set<String> names) {
        if (names == null || names.isEmpty()) {
            return Set.of();
        }
        return names.stream()
                .map(MultiplayerServer::normalizeName)
                .filter(name -> !name.isBlank())
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
    }

    private static Set<String> normalizeIps(Set<String> ips) {
        if (ips == null || ips.isEmpty()) {
            return Set.of();
        }
        return ips.stream()
                .map(MultiplayerServer::normalizeIp)
                .filter(ip -> !ip.isBlank())
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
    }

    private static String normalizeName(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private static String normalizeIp(String value) {
        String text = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
        return text.startsWith("/") ? text.substring(1) : text;
    }

    private boolean tryCompleteAllPlayerSleep() {
        ProtocolServer server = protocolServer;
        if (server == null || !server.isRunning() || !hostSleeping) {
            return false;
        }
        for (String playerId : server.connectedPlayerIds()) {
            if (!sleepingPlayers.contains(playerId)) {
                return false;
            }
        }
        hostSleeping = false;
        sleepingPlayers.clear();
        sleepCompletePending.set(true);
        worldTime = 0.0f;
        server.setWorldTime(0.0);
        server.setWorldWeather("clear");
        server.broadcastWorldState(0.0, "clear");
        server.broadcast(new ProtocolMessage.ClientAction(
                "",
                MultiplayerProtocol.ACTION_BED_SLEEP_COMPLETE,
                null,
                Map.of("time", "0.0")
        ));
        return true;
    }

    private JsonObject playerStateData(String playerId, int clientId, ProtocolMessage.ClientInput input) {
        JsonObject data = NetworkMessage.object();
        String username = usernameForClient(clientId);
        data.addProperty("clientId", clientId);
        data.addProperty("playerId", playerId);
        data.addProperty("username", username == null ? fallbackPlayerName(clientId) : username);
        data.addProperty("x", input.pose().x());
        data.addProperty("y", input.pose().y());
        data.addProperty("z", input.pose().z());
        data.addProperty("yaw", input.pose().yaw());
        data.addProperty("pitch", input.pose().pitch());
        data.addProperty("onGround", input.onGround());
        data.addProperty("sneaking", input.sneaking());
        data.addProperty("health", input.health());
        int selectedSlot = Math.max(0, Math.min(MultiplayerProtocol.INVENTORY_HOTBAR_SLOTS - 1,
                input.selectedSlot()));
        ProtocolMessage.InventoryUpdate held = currentInventoryUpdate(playerId, selectedSlot);
        data.addProperty("heldItemId", held == null ? "air" : held.itemId());
        data.addProperty("heldItemCount", held == null ? 0 : held.count());
        data.addProperty("heldItemDamage", held == null ? 0 : held.damage());
        data.addProperty("selectedSlot", selectedSlot);
        data.addProperty("gameMode", input.gameMode());
        for (Map.Entry<String, String> entry : playerStateDataFor(playerId, input).entrySet()) {
            data.addProperty(entry.getKey(), entry.getValue());
        }
        return data;
    }

    private Map<String, String> playerStateDataFor(String playerId, ProtocolMessage.ClientInput input) {
        java.util.LinkedHashMap<String, String> data = new java.util.LinkedHashMap<>();
        if (input != null) {
            copyClientPlayerStateData(data, input.data());
            putClientInputState(data, input);
        }
        putPlayerArmorState(data, playerId);
        data.put("dimension", dimension);
        return data;
    }

    private static void putClientInputState(Map<String, String> data, ProtocolMessage.ClientInput input) {
        if (data == null || input == null) {
            return;
        }
        data.put("input.forward", Boolean.toString(input.forward()));
        data.put("input.backward", Boolean.toString(input.backward()));
        data.put("input.left", Boolean.toString(input.left()));
        data.put("input.right", Boolean.toString(input.right()));
        data.put("input.jumping", Boolean.toString(input.jumping()));
        data.put("input.sneaking", Boolean.toString(input.sneaking()));
    }

    private static void copyClientPlayerStateData(Map<String, String> target, Map<String, String> inputData) {
        if (target == null || inputData == null || inputData.isEmpty()) {
            return;
        }
        for (Map.Entry<String, String> entry : inputData.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            if (MultiplayerProtocol.isValidClientPlayerStateData(key, value)) {
                target.put(key, value);
            }
        }
    }

    private void putPlayerArmorState(Map<String, String> data, String playerId) {
        data.put("armor.size", Integer.toString(MultiplayerProtocol.INVENTORY_ARMOR_SLOTS));
        int armorStart = MultiplayerProtocol.INVENTORY_HOTBAR_SLOTS
                + MultiplayerProtocol.INVENTORY_MAIN_SLOTS
                + MultiplayerProtocol.INVENTORY_CRAFTING_SLOTS;
        for (int armorSlot = 0; armorSlot < MultiplayerProtocol.INVENTORY_ARMOR_SLOTS; armorSlot++) {
            putInventoryUpdateStackData(data, "armor." + armorSlot,
                    currentInventoryUpdate(playerId, armorStart + armorSlot));
        }
    }

    private static void putInventoryUpdateStackData(Map<String, String> data, String prefix,
            ProtocolMessage.InventoryUpdate update) {
        if (update == null || update.count() <= 0 || "air".equalsIgnoreCase(update.itemId())) {
            data.put(prefix + ".itemId", "air");
            data.put(prefix + ".count", "0");
            data.put(prefix + ".damage", "-1");
            return;
        }
        for (Map.Entry<String, String> entry : update.data().entrySet()) {
            String key = entry.getKey();
            if (key != null && key.startsWith("stack.")
                    && !key.equals("stack.itemId")
                    && !key.equals("stack.count")
                    && !key.equals("stack.damage")) {
                data.put(prefix + key.substring("stack".length()), entry.getValue());
            }
        }
        data.put(prefix + ".itemId", update.itemId());
        data.put(prefix + ".count", Integer.toString(update.count()));
        data.put(prefix + ".damage", Integer.toString(update.damage()));
    }

    private ProtocolMessage.InventoryUpdate currentInventoryUpdate(String playerId, int slot) {
        ProtocolServer server = protocolServer;
        if (server == null || playerId == null || playerId.isBlank()) {
            return null;
        }
        for (ProtocolMessage.InventoryUpdate update : server.currentInventoryUpdates()) {
            if (update.slot() == slot && playerId.equals(update.playerId())) {
                return update;
            }
        }
        return null;
    }

    private void notifyListeners(NetworkMessage message) {
        for (Consumer<NetworkMessage> listener : listeners) {
            listener.accept(message);
        }
    }

    private void broadcastPlayerList() {
        ProtocolServer server = protocolServer;
        if (server != null && server.isRunning()) {
            server.broadcast(playerListMessage());
        }
    }

    private ProtocolMessage.PlayerList playerListMessage() {
        ProtocolServer server = protocolServer;
        LinkedHashMap<String, ProtocolMessage.PlayerListEntry> players = new LinkedHashMap<>();
        if (server != null) {
            server.currentPlayers().stream()
                    .sorted(Comparator.comparing(ProtocolMessage.PlayerState::playerId))
                    .forEach(state -> players.put(state.playerId(), new ProtocolMessage.PlayerListEntry(
                            state.playerId(),
                            state.username(),
                            0)));
        }
        connectedPlayers().stream()
                .forEach(player -> players.put(player.playerId(), new ProtocolMessage.PlayerListEntry(
                        player.playerId(),
                        player.username(),
                        player.latencyMillis())));
        return new ProtocolMessage.PlayerList(List.copyOf(players.values()));
    }

    static ProtocolMessage toProtocolMessage(NetworkMessage message) {
        if (message == null || !MultiplayerProtocol.isValidLegacyMessageType(message.type())) {
            return null;
        }
        JsonObject data = message.data() == null ? NetworkMessage.object() : message.data();
        try {
            return switch (message.type()) {
                case "chat" -> new ProtocolMessage.Chat(
                        string(data, "playerId", ""),
                        string(data, "sender", "Server"),
                        string(data, "text", "")
                );
                case "worldState" -> new ProtocolMessage.WorldState(
                        longNumber(data, "seed", 0L),
                        decimal(data, "time", 0.0f),
                        string(data, "weatherState", "clear"),
                        integer(data, "spawnX", 0),
                        integer(data, "spawnY", 80),
                        integer(data, "spawnZ", 0),
                        string(data, "gameMode", "SURVIVAL"),
                        string(data, "difficulty", "EASY"),
                        bool(data, "hardcore", false),
                        bool(data, "allowCheats", false),
                        bool(data, "pvp", true),
                        bool(data, "spawnAnimals", true),
                        bool(data, "spawnMonsters", true),
                        bool(data, "spawnNpcs", true),
                        bool(data, "allowNether", true),
                        bool(data, "allowFlight", false),
                        string(data, "dimension", "overworld"),
                        integer(data, "maxPlayers", MultiplayerProtocol.DEFAULT_MAX_PLAYERS),
                        integer(data, "viewDistance", MultiplayerProtocol.DEFAULT_VIEW_DISTANCE),
                        integer(data, "maxBuildHeight", MultiplayerProtocol.DEFAULT_MAX_BUILD_HEIGHT),
                        bool(data, "generateStructures", MultiplayerProtocol.DEFAULT_GENERATE_STRUCTURES),
                        List.of(),
                        List.of(),
                        List.of(),
                        List.of()
                );
                case "blockUpdate", "blockAction" -> new ProtocolMessage.BlockUpdate(
                        integer(data, "x", 0),
                        integer(data, "y", 0),
                        integer(data, "z", 0),
                        string(data, "blockId", "0"),
                        integer(data, "metadata", 0),
                        string(data, "playerId", ""),
                        stringMapWithout(data, "x", "y", "z", "blockId", "metadata", "playerId", "clientId")
                );
                case "clientAction" -> new ProtocolMessage.ClientAction(
                        string(data, "playerId", ""),
                        string(data, "action", "action"),
                        null,
                        stringMapWithout(data, "playerId", "clientId", "action")
                );
                case "serverCommand" -> new ProtocolMessage.Chat(
                        string(data, "playerId", ""),
                        string(data, "sender", "Player"),
                        string(data, "text", "")
                );
                case "playerState" -> new ProtocolMessage.PlayerState(
                        string(data, "playerId", ""),
                        string(data, "username", "Player"),
                        pose(data),
                        bool(data, "onGround", true),
                        bool(data, "sneaking", false),
                        (float) decimal(data, "health", 20.0),
                        string(data, "heldItemId", "air"),
                        integer(data, "heldItemCount", 0),
                        integer(data, "heldItemDamage", 0),
                        integer(data, "selectedSlot", 0),
                        string(data, "gameMode", "SURVIVAL"),
                        stringMapWithout(data, "playerId", "clientId", "username", "x", "y", "z", "yaw", "pitch",
                                "onGround", "sneaking", "health", "heldItemId", "heldItemCount", "heldItemDamage",
                                "selectedSlot", "gameMode")
                );
                case "entityUpdate", "entityAction" -> new ProtocolMessage.EntityUpdate(
                        string(data, "entityId", ""),
                        string(data, "entityType", "entity"),
                        pose(data),
                        stringMapWithout(data, "entityId", "entityType", "x", "y", "z", "yaw", "pitch")
                );
                case "inventoryUpdate", "inventoryAction", "containerAction" -> new ProtocolMessage.InventoryUpdate(
                        string(data, "playerId", ""),
                        integer(data, "slot", 0),
                        string(data, "itemId", "air"),
                        integer(data, "count", 0),
                        integer(data, "damage", 0),
                        stringMapWithout(data, "playerId", "clientId", "slot", "itemId", "count", "damage")
                );
                case "worldSound" -> new ProtocolMessage.WorldEvent(
                        "sound",
                        stringMapWithout(data, "eventType")
                );
                case "worldParticle" -> new ProtocolMessage.WorldEvent(
                        "particle",
                        stringMapWithout(data, "eventType")
                );
                case "worldLightning" -> new ProtocolMessage.WorldEvent(
                        "lightning",
                        stringMapWithout(data, "eventType")
                );
                case "disconnect" -> new ProtocolMessage.Disconnect(
                        string(data, "playerId", ""),
                        string(data, "reason", "Disconnected")
                );
                default -> null;
            };
        } catch (RuntimeException ignored) {
            return null;
        }
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
            data.addProperty("weatherState", worldState.weatherState());
            data.addProperty("spawnX", worldState.spawnX());
            data.addProperty("spawnY", worldState.spawnY());
            data.addProperty("spawnZ", worldState.spawnZ());
            data.addProperty("gameMode", worldState.gameMode());
            data.addProperty("difficulty", worldState.difficulty());
            data.addProperty("hardcore", worldState.hardcore());
            data.addProperty("allowCheats", worldState.allowCheats());
            data.addProperty("pvp", worldState.pvp());
            data.addProperty("spawnAnimals", worldState.spawnAnimals());
            data.addProperty("spawnMonsters", worldState.spawnMonsters());
            data.addProperty("spawnNpcs", worldState.spawnNpcs());
            data.addProperty("allowNether", worldState.allowNether());
            data.addProperty("allowFlight", worldState.allowFlight());
            data.addProperty("dimension", worldState.dimension());
            data.addProperty("maxPlayers", worldState.maxPlayers());
            data.addProperty("viewDistance", worldState.viewDistance());
            data.addProperty("maxBuildHeight", worldState.maxBuildHeight());
            data.addProperty("generateStructures", worldState.generateStructures());
            data.addProperty("players", worldState.players().size());
            data.addProperty("blockUpdates", worldState.blockUpdates().size());
            data.addProperty("entityUpdates", worldState.entityUpdates().size());
            data.addProperty("inventoryUpdates", worldState.inventoryUpdates().size());
            return NetworkMessage.of("worldState", data);
        }
        if (message instanceof ProtocolMessage.PlayerList playerList) {
            JsonArray players = new JsonArray();
            for (ProtocolMessage.PlayerListEntry entry : playerList.players()) {
                JsonObject player = NetworkMessage.object();
                player.addProperty("clientId", legacyClientId(entry.playerId()));
                player.addProperty("playerId", entry.playerId());
                player.addProperty("username", entry.username());
                player.addProperty("latencyMillis", entry.latencyMillis());
                players.add(player);
            }
            data.add("players", players);
            data.addProperty("count", playerList.players().size());
            return NetworkMessage.of("playerList", data);
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
            data.addProperty("sneaking", playerState.sneaking());
            data.addProperty("health", playerState.health());
            data.addProperty("heldItemId", playerState.heldItemId());
            data.addProperty("heldItemCount", playerState.heldItemCount());
            data.addProperty("heldItemDamage", playerState.heldItemDamage());
            data.addProperty("selectedSlot", playerState.selectedSlot());
            data.addProperty("gameMode", playerState.gameMode());
            for (Map.Entry<String, String> entry : playerState.data().entrySet()) {
                data.addProperty(entry.getKey(), entry.getValue());
            }
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
            for (Map.Entry<String, String> entry : blockUpdate.data().entrySet()) {
                data.addProperty(entry.getKey(), entry.getValue());
            }
            return NetworkMessage.of("blockUpdate", data);
        }
        if (message instanceof ProtocolMessage.ClientAction action) {
            data.addProperty("playerId", action.playerId());
            data.addProperty("clientId", legacyClientId(action.playerId()));
            data.addProperty("action", action.action());
            for (Map.Entry<String, String> entry : action.data().entrySet()) {
                data.addProperty(entry.getKey(), entry.getValue());
            }
            return NetworkMessage.of("clientAction", data);
        }
        if (message instanceof ProtocolMessage.EntityUpdate entityUpdate) {
            data.addProperty("entityId", entityUpdate.entityId());
            data.addProperty("entityType", entityUpdate.entityType());
            addPose(data, entityUpdate.pose());
            for (Map.Entry<String, String> entry : entityUpdate.data().entrySet()) {
                data.addProperty(entry.getKey(), entry.getValue());
            }
            return NetworkMessage.of("entityUpdate", data);
        }
        if (message instanceof ProtocolMessage.InventoryUpdate inventoryUpdate) {
            data.addProperty("playerId", inventoryUpdate.playerId());
            data.addProperty("clientId", legacyClientId(inventoryUpdate.playerId()));
            data.addProperty("slot", inventoryUpdate.slot());
            data.addProperty("itemId", inventoryUpdate.itemId());
            data.addProperty("count", inventoryUpdate.count());
            data.addProperty("damage", inventoryUpdate.damage());
            for (Map.Entry<String, String> entry : inventoryUpdate.data().entrySet()) {
                data.addProperty(entry.getKey(), entry.getValue());
            }
            return NetworkMessage.of("inventoryUpdate", data);
        }
        if (message instanceof ProtocolMessage.WorldEvent worldEvent) {
            data.addProperty("eventType", worldEvent.eventType());
            for (Map.Entry<String, String> entry : worldEvent.data().entrySet()) {
                data.addProperty(entry.getKey(), entry.getValue());
            }
            return NetworkMessage.of(switch (worldEvent.eventType()) {
                case "sound" -> "worldSound";
                case "particle" -> "worldParticle";
                case "lightning" -> "worldLightning";
                default -> "worldEvent";
            }, data);
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

    private static Map<String, String> soundEventData(WorldSoundEvent event) {
        LinkedHashMap<String, String> data = new LinkedHashMap<>();
        data.put("soundId", event.soundId());
        data.put("x", Float.toString(event.x()));
        data.put("y", Float.toString(event.y()));
        data.put("z", Float.toString(event.z()));
        data.put("volume", Float.toString(event.volume()));
        data.put("pitch", Float.toString(event.pitch()));
        return data;
    }

    private static Map<String, String> particleEventData(WorldParticle particle) {
        LinkedHashMap<String, String> data = new LinkedHashMap<>();
        data.put("particleType", particle.getType().name());
        data.put("x", Float.toString(particle.getX()));
        data.put("y", Float.toString(particle.getY()));
        data.put("z", Float.toString(particle.getZ()));
        data.put("motionX", Float.toString(particle.getMotionX()));
        data.put("motionY", Float.toString(particle.getMotionY()));
        data.put("motionZ", Float.toString(particle.getMotionZ()));
        data.put("scale", Float.toString(particle.getBaseScale()));
        data.put("lifetime", Integer.toString(Math.max(1,
                Math.round(particle.getLifetimeTicks() - particle.getAgeTicks()))));
        data.put("data", Float.toString(particle.getData()));
        data.put("hasTarget", Boolean.toString(particle.hasTarget()));
        if (particle.hasTarget()) {
            data.put("targetX", Float.toString(particle.getTargetX()));
            data.put("targetY", Float.toString(particle.getTargetY()));
            data.put("targetZ", Float.toString(particle.getTargetZ()));
        }
        return data;
    }

    private static Map<String, String> lightningEventData(WorldLightningBolt bolt) {
        LinkedHashMap<String, String> data = new LinkedHashMap<>();
        data.put("x", Float.toString(bolt.getX()));
        data.put("y", Float.toString(bolt.getY()));
        data.put("z", Float.toString(bolt.getZ()));
        data.put("lifetime", Integer.toString(Math.max(1,
                Math.round(bolt.getLifetimeTicks() - bolt.getAgeTicks()))));
        List<WorldLightningBolt.Segment> segments = bolt.getSegments();
        data.put("segmentCount", Integer.toString(segments.size()));
        for (int i = 0; i < segments.size(); i++) {
            WorldLightningBolt.Segment segment = segments.get(i);
            String prefix = "segment." + i;
            data.put(prefix + ".x1", Float.toString(segment.x1()));
            data.put(prefix + ".y1", Float.toString(segment.y1()));
            data.put(prefix + ".z1", Float.toString(segment.z1()));
            data.put(prefix + ".x2", Float.toString(segment.x2()));
            data.put(prefix + ".y2", Float.toString(segment.y2()));
            data.put(prefix + ".z2", Float.toString(segment.z2()));
        }
        List<WorldLightningBolt.FlashWindow> windows = bolt.getFlashWindows();
        data.put("flashCount", Integer.toString(windows.size()));
        for (int i = 0; i < windows.size(); i++) {
            WorldLightningBolt.FlashWindow window = windows.get(i);
            String prefix = "flash." + i;
            data.put(prefix + ".start", Float.toString(window.startTick()));
            data.put(prefix + ".end", Float.toString(window.endTick()));
        }
        return data;
    }

    private static ConnectedPlayer connectedPlayer(String playerId, String username, int latencyMillis,
            String remoteAddress) {
        int clientId = legacyClientId(playerId);
        return new ConnectedPlayer(clientId, playerId, normalizeSender(username, fallbackPlayerName(clientId)),
                Math.max(-1, latencyMillis), normalizeIp(remoteAddress));
    }

    private static String fallbackPlayerName(int clientId) {
        return clientId > 0 ? "Player" + clientId : "Player";
    }

    private void applyWorldMetadata(ProtocolServer server) {
        server.configureWorldMetadata(spawnX, spawnY, spawnZ, gameMode, difficulty, hardcore, allowCheats,
                pvp, spawnAnimals, spawnMonsters, spawnNpcs, allowNether, allowFlight, dimension, maxPlayers,
                viewDistance, maxBuildHeight, generateStructures);
    }

    private void applyQuerySettings(ProtocolServer server) {
        server.configureQuery(queryEnabled, queryPort);
    }

    private static String normalizeMetadata(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private static String normalizeBindAddress(String value) {
        return value == null ? "" : value.replace('\r', ' ').replace('\n', ' ').trim();
    }

    private static int clampViewDistance(int viewDistance) {
        return Math.max(MultiplayerProtocol.MIN_VIEW_DISTANCE,
                Math.min(MultiplayerProtocol.MAX_VIEW_DISTANCE, viewDistance));
    }

    private static int clampMaxBuildHeight(int maxBuildHeight) {
        return Math.max(MultiplayerProtocol.MIN_MAX_BUILD_HEIGHT,
                Math.min(MultiplayerProtocol.WORLD_HEIGHT, maxBuildHeight));
    }

    private static int validPort(int port) {
        return port <= 0 || port > 65535 ? MultiplayerProtocol.DEFAULT_QUERY_PORT : port;
    }

    private void announceSystemChat(String text) {
        if (text == null || text.isBlank()) {
            return;
        }
        ProtocolMessage.Chat chat = new ProtocolMessage.Chat("", "Server", text);
        notifyListeners(toNetworkMessage(chat));
        ProtocolServer server = protocolServer;
        if (server != null && server.isRunning()) {
            server.broadcast(chat);
        }
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
        JsonElement value = value(data, key);
        if (value == null) {
            return fallback;
        }
        if (!value.isJsonPrimitive()) {
            throw new IllegalArgumentException("Invalid string field: " + key);
        }
        String text = value.getAsString();
        if (!MultiplayerProtocol.isValidLegacyDataValue(text)) {
            throw new IllegalArgumentException("String field too long: " + key);
        }
        return text;
    }

    private static Map<String, String> stringMapWithout(JsonObject data, String... excludedKeys) {
        if (data == null) {
            return Map.of();
        }
        java.util.HashSet<String> excluded = new java.util.HashSet<>();
        if (excludedKeys != null) {
            for (String key : excludedKeys) {
                excluded.add(key);
            }
        }
        java.util.LinkedHashMap<String, String> result = new java.util.LinkedHashMap<>();
        for (Map.Entry<String, com.google.gson.JsonElement> entry : data.entrySet()) {
            if (result.size() >= MultiplayerProtocol.MAX_LEGACY_MESSAGE_DATA_ENTRIES) {
                break;
            }
            String key = entry.getKey();
            JsonElement value = entry.getValue();
            if (!excluded.contains(key)
                    && MultiplayerProtocol.isValidLegacyDataKey(key)
                    && value != null
                    && value.isJsonPrimitive()) {
                String text = value.getAsString();
                if (text.length() <= MultiplayerProtocol.MAX_ITEM_STACK_DATA_VALUE_LENGTH) {
                    result.put(key, text);
                }
            }
        }
        return result;
    }

    private static int integer(JsonObject data, String key, int fallback) {
        JsonElement value = value(data, key);
        return value == null ? fallback : value.getAsInt();
    }

    private static long longNumber(JsonObject data, String key, long fallback) {
        JsonElement value = value(data, key);
        return value == null ? fallback : value.getAsLong();
    }

    private static double decimal(JsonObject data, String key, double fallback) {
        JsonElement element = value(data, key);
        if (element == null) {
            return fallback;
        }
        double value = element.getAsDouble();
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException("Non-finite decimal field: " + key);
        }
        return value;
    }

    private static boolean bool(JsonObject data, String key, boolean fallback) {
        JsonElement value = value(data, key);
        if (value == null) {
            return fallback;
        }
        if (!value.isJsonPrimitive()) {
            throw new IllegalArgumentException("Invalid boolean field: " + key);
        }
        String text = value.getAsString();
        if ("true".equalsIgnoreCase(text)) {
            return true;
        }
        if ("false".equalsIgnoreCase(text)) {
            return false;
        }
        throw new IllegalArgumentException("Invalid boolean field: " + key);
    }

    private static JsonElement value(JsonObject data, String key) {
        if (data == null || key == null || !data.has(key)) {
            return null;
        }
        JsonElement value = data.get(key);
        return value == null || value.isJsonNull() ? null : value;
    }
}
