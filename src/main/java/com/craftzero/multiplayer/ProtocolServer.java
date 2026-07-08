package com.craftzero.multiplayer;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PushbackInputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public final class ProtocolServer implements AutoCloseable {
    private static final int SERVER_LIST_PING_PACKET_ID = 0xFE;
    private static final int LEGACY_LOGIN_PACKET_ID = 0x01;
    private static final int LEGACY_HANDSHAKE_PACKET_ID = 0x02;
    private static final int KICK_PACKET_ID = 0xFF;
    private static final byte QUERY_STATUS_PACKET_ID = 0x00;
    private static final byte QUERY_HANDSHAKE_PACKET_ID = 0x09;
    private static final int QUERY_MAGIC_0 = 0xFE;
    private static final int QUERY_MAGIC_1 = 0xFD;
    private static final int QUERY_TOKEN_MIN = 1_000_000;
    private static final int QUERY_TOKEN_RANGE = 8_999_999;
    private static final int PING_PEEK_TIMEOUT_MILLIS = 75;
    private static final int QUERY_SOCKET_TIMEOUT_MILLIS = 500;
    private static final long QUERY_CHALLENGE_TTL_MILLIS = 30_000L;
    private static final long FLYING_CHECK_GRACE_MILLIS = 4_000L;
    private static final double FLYING_CHECK_FALL_RESET_VELOCITY = -0.03d;
    private static final String DIMENSION_DATA_KEY = "dimension";
    private static final String LEGACY_PROTOCOL_REQUIRED_REASON =
            "CraftZero protocol v" + MultiplayerProtocol.VERSION + " required";

    private final String requestedBindAddress;
    private final int requestedPort;
    private final long worldSeed;
    private final String serverName;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicInteger nextPlayerId = new AtomicInteger(1);
    private final AtomicInteger nextKeepAliveId = new AtomicInteger(1);
    private final ConcurrentHashMap<String, ClientConnection> clients = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, ProtocolMessage.PlayerState> playerStates = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, ProtocolMessage.BlockUpdate> blockStates = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, ProtocolMessage.EntityUpdate> entityStates = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, ProtocolMessage.InventoryUpdate> inventoryStates = new ConcurrentHashMap<>();
    private final CopyOnWriteArrayList<ServerMessageListener> listeners = new CopyOnWriteArrayList<>();

    private volatile double worldTime;
    private volatile String worldWeather = "clear";
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
    private volatile boolean queryEnabled;
    private volatile int queryPort = MultiplayerProtocol.DEFAULT_QUERY_PORT;
    private volatile ServerSocket serverSocket;
    private volatile DatagramSocket querySocket;
    private volatile Thread acceptThread;
    private volatile Thread keepAliveThread;
    private volatile Thread queryThread;
    private volatile JoinAdmission joinAdmission = (username, remoteAddress) -> null;

    public ProtocolServer(long worldSeed, double worldTime) {
        this("", MultiplayerProtocol.DEFAULT_PORT, worldSeed, worldTime, "CraftZero");
    }

    public ProtocolServer(int port, long worldSeed, double worldTime) {
        this("", port, worldSeed, worldTime, "CraftZero");
    }

    public ProtocolServer(int port, long worldSeed, double worldTime, String serverName) {
        this("", port, worldSeed, worldTime, serverName);
    }

    public ProtocolServer(String bindAddress, int port, long worldSeed, double worldTime, String serverName) {
        this.requestedBindAddress = normalizeBindAddress(bindAddress);
        this.requestedPort = port;
        this.worldSeed = worldSeed;
        this.worldTime = worldTime;
        this.serverName = serverName == null || serverName.isBlank() ? "CraftZero" : serverName;
    }

    public void start() throws IOException {
        if (!running.compareAndSet(false, true)) {
            return;
        }

        ServerSocket socket = new ServerSocket();
        socket.setReuseAddress(true);
        try {
            if (requestedBindAddress.isBlank()) {
                socket.bind(new InetSocketAddress(requestedPort));
            } else {
                socket.bind(new InetSocketAddress(requestedBindAddress, requestedPort));
            }
        } catch (IOException exception) {
            running.set(false);
            socket.close();
            throw exception;
        }

        serverSocket = socket;
        Thread thread = new Thread(this::acceptLoop, "CraftZero-Protocol-Accept");
        thread.setDaemon(true);
        acceptThread = thread;
        thread.start();

        Thread keepAlive = new Thread(this::keepAliveLoop, "CraftZero-Protocol-KeepAlive");
        keepAlive.setDaemon(true);
        keepAliveThread = keepAlive;
        keepAlive.start();
        if (queryEnabled) {
            startQueryResponder();
        }
    }

    public boolean isRunning() {
        return running.get();
    }

    public int getPort() {
        ServerSocket socket = serverSocket;
        return socket == null ? requestedPort : socket.getLocalPort();
    }

    public long getWorldSeed() {
        return worldSeed;
    }

    public double getWorldTime() {
        return worldTime;
    }

    public void setWorldTime(double worldTime) {
        this.worldTime = worldTime;
    }

    public String getWorldWeather() {
        return worldWeather;
    }

    public void setWorldWeather(String worldWeather) {
        this.worldWeather = worldWeather == null || worldWeather.isBlank() ? "clear" : worldWeather;
    }

    public void configureQuery(boolean enabled, int port) {
        this.queryEnabled = enabled;
        this.queryPort = clampPort(port);
        if (!running.get()) {
            return;
        }
        if (!enabled) {
            stopQueryResponder();
            return;
        }
        startQueryResponder();
    }

    public void configureWorldMetadata(int spawnX, int spawnY, int spawnZ, String gameMode, String difficulty,
            boolean hardcore, boolean allowCheats, String dimension, int maxPlayers) {
        configureWorldMetadata(spawnX, spawnY, spawnZ, gameMode, difficulty, hardcore, allowCheats,
                true, true, true, true, true, false, dimension, maxPlayers,
                MultiplayerProtocol.DEFAULT_VIEW_DISTANCE, MultiplayerProtocol.DEFAULT_MAX_BUILD_HEIGHT,
                MultiplayerProtocol.DEFAULT_GENERATE_STRUCTURES);
    }

    public void configureWorldMetadata(int spawnX, int spawnY, int spawnZ, String gameMode, String difficulty,
            boolean hardcore, boolean allowCheats, boolean pvp, boolean spawnAnimals, boolean spawnMonsters,
            boolean allowNether, boolean allowFlight, String dimension, int maxPlayers) {
        configureWorldMetadata(spawnX, spawnY, spawnZ, gameMode, difficulty, hardcore, allowCheats,
                pvp, spawnAnimals, spawnMonsters, true, allowNether, allowFlight, dimension, maxPlayers,
                MultiplayerProtocol.DEFAULT_VIEW_DISTANCE, MultiplayerProtocol.DEFAULT_MAX_BUILD_HEIGHT,
                MultiplayerProtocol.DEFAULT_GENERATE_STRUCTURES);
    }

    public void configureWorldMetadata(int spawnX, int spawnY, int spawnZ, String gameMode, String difficulty,
            boolean hardcore, boolean allowCheats, boolean pvp, boolean spawnAnimals, boolean spawnMonsters,
            boolean allowNether, boolean allowFlight, String dimension, int maxPlayers, int viewDistance) {
        configureWorldMetadata(spawnX, spawnY, spawnZ, gameMode, difficulty, hardcore, allowCheats,
                pvp, spawnAnimals, spawnMonsters, true, allowNether, allowFlight, dimension, maxPlayers,
                viewDistance, MultiplayerProtocol.DEFAULT_MAX_BUILD_HEIGHT,
                MultiplayerProtocol.DEFAULT_GENERATE_STRUCTURES);
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
        String normalizedDimension = normalizeText(dimension, "overworld");
        boolean dimensionChanged = !normalizedDimension.equals(this.dimension);
        this.spawnX = spawnX;
        this.spawnY = spawnY;
        this.spawnZ = spawnZ;
        this.gameMode = normalizeText(gameMode, "SURVIVAL");
        this.difficulty = normalizeText(difficulty, "EASY");
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
            resetSnapshotsForDimensionChange();
        }
    }

    public void setJoinAdmission(JoinAdmission joinAdmission) {
        this.joinAdmission = joinAdmission == null ? (username, remoteAddress) -> null : joinAdmission;
    }

    public int connectedPlayerCount() {
        return (int) clients.values().stream()
                .filter(ClientConnection::isJoined)
                .count();
    }

    public int trackedPlayerCount() {
        return playerStates.size();
    }

    public List<ProtocolMessage.PlayerState> currentPlayers() {
        return playerStates.values().stream()
                .map(this::withCurrentDimension)
                .filter(ProtocolServer::isValidPlayerState)
                .sorted(Comparator.comparing(ProtocolMessage.PlayerState::playerId))
                .toList();
    }

    public List<ProtocolMessage.BlockUpdate> currentBlockUpdates() {
        return blockStates.values().stream()
                .map(this::withCurrentDimension)
                .filter(ProtocolServer::isValidBlockUpdate)
                .sorted(Comparator.comparing(ProtocolServer::blockUpdateKey))
                .toList();
    }

    public List<ProtocolMessage.EntityUpdate> currentEntityUpdates() {
        return entityStates.values().stream()
                .map(this::withCurrentDimension)
                .filter(ProtocolServer::isValidEntityUpdate)
                .sorted(Comparator.comparing(ProtocolMessage.EntityUpdate::entityId))
                .toList();
    }

    public List<ProtocolMessage.InventoryUpdate> currentInventoryUpdates() {
        return inventoryStates.values().stream()
                .map(this::withCurrentDimension)
                .filter(ProtocolServer::isValidInventoryUpdate)
                .sorted(Comparator.comparing(ProtocolServer::inventoryUpdateKey))
                .toList();
    }

    public List<String> connectedPlayerIds() {
        return clients.values().stream()
                .filter(ClientConnection::isJoined)
                .map(ClientConnection::playerId)
                .sorted()
                .toList();
    }

    public int clientLatencyMillis(String playerId) {
        ClientConnection client = clients.get(playerId);
        return client == null ? -1 : client.latencyMillis();
    }

    public String clientRemoteAddress(String playerId) {
        ClientConnection client = clients.get(playerId);
        return client == null ? "" : client.remoteAddress();
    }

    public void addListener(ServerMessageListener listener) {
        if (listener != null) {
            listeners.add(listener);
        }
    }

    public void removeListener(ServerMessageListener listener) {
        listeners.remove(listener);
    }

    public void broadcast(ProtocolMessage message) {
        if (message == null) {
            return;
        }
        message = withCurrentDimension(message);
        if (!isValidStateMessage(message)) {
            return;
        }
        rememberState(message);

        ArrayList<ClientConnection> failed = new ArrayList<>();
        for (ClientConnection client : clients.values()) {
            if (!client.isJoined()) {
                continue;
            }
            try {
                client.send(message);
            } catch (IOException exception) {
                failed.add(client);
            }
        }
        for (ClientConnection client : failed) {
            removeClient(client, "send failed", true);
        }
    }

    public boolean sendTo(String playerId, ProtocolMessage message) {
        if (!MultiplayerProtocol.isValidNetworkId(playerId) || message == null) {
            return false;
        }
        ClientConnection client = clients.get(playerId);
        if (client == null || !client.isJoined()) {
            return false;
        }
        message = withCurrentDimension(message);
        if (!isValidStateMessage(message)) {
            return false;
        }
        rememberState(message);
        try {
            client.send(message);
            return true;
        } catch (IOException exception) {
            removeClient(client, "send failed", true);
            return false;
        }
    }

    public void broadcastBlockUpdate(ProtocolMessage.BlockUpdate update) {
        broadcast(update);
    }

    public void rememberBlockUpdate(ProtocolMessage.BlockUpdate update) {
        rememberState(update);
    }

    public void rememberPlayerState(ProtocolMessage.PlayerState state) {
        rememberState(state);
    }

    public void broadcastEntityUpdate(ProtocolMessage.EntityUpdate update) {
        broadcast(update);
    }

    public void rememberEntityUpdate(ProtocolMessage.EntityUpdate update) {
        rememberState(update);
    }

    public void broadcastInventoryUpdate(ProtocolMessage.InventoryUpdate update) {
        broadcast(update);
    }

    public void rememberInventoryUpdate(ProtocolMessage.InventoryUpdate update) {
        rememberState(update);
    }

    public void clearWorldSnapshotState() {
        clearDimensionScopedSnapshotState();
        inventoryStates.clear();
    }

    private void clearDimensionScopedSnapshotState() {
        playerStates.clear();
        blockStates.clear();
        entityStates.clear();
    }

    private void resetSnapshotsForDimensionChange() {
        Map<String, ProtocolMessage.PlayerState> retainedPlayerStates = new LinkedHashMap<>(playerStates);
        clearDimensionScopedSnapshotState();
        rebaseInventorySnapshotsToCurrentDimension();
        for (ClientConnection client : clients.values()) {
            if (client.isJoined()) {
                seedJoinedPlayerState(client, retainedPlayerStates.get(client.playerId()));
            }
        }
    }

    private void rebaseInventorySnapshotsToCurrentDimension() {
        inventoryStates.replaceAll((key, update) -> withCurrentDimension(update));
    }

    public void broadcastWorldState(double time, String weatherState) {
        setWorldTime(time);
        setWorldWeather(weatherState);
        broadcast(currentWorldMetadataSnapshot());
    }

    public boolean disconnectPlayer(String playerId, String reason) {
        ClientConnection client = clients.get(playerId);
        if (client == null) {
            return false;
        }
        try {
            client.send(new ProtocolMessage.Disconnect(playerId, reason == null ? "Disconnected" : reason));
        } catch (IOException ignored) {
        }
        removeClient(client, reason == null ? "Disconnected" : reason, true);
        return true;
    }

    @Override
    public void close() {
        if (!running.compareAndSet(true, false)) {
            return;
        }

        ServerSocket socket = serverSocket;
        serverSocket = null;
        stopQueryResponder();
        if (socket != null) {
            try {
                socket.close();
            } catch (IOException ignored) {
            }
        }

        for (ClientConnection client : List.copyOf(clients.values())) {
            try {
                client.send(new ProtocolMessage.Disconnect(client.playerId(), "server closed"));
            } catch (IOException ignored) {
            }
            removeClient(client, "server closed", false);
        }
        playerStates.clear();
        blockStates.clear();
        entityStates.clear();
        inventoryStates.clear();

        Thread thread = acceptThread;
        acceptThread = null;
        if (thread != null && thread != Thread.currentThread()) {
            try {
                thread.join(1000L);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
            }
        }
        Thread keepAlive = keepAliveThread;
        keepAliveThread = null;
        if (keepAlive != null && keepAlive != Thread.currentThread()) {
            try {
                keepAlive.join(1000L);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private void acceptLoop() {
        ServerSocket acceptSocket = serverSocket;
        while (running.get() && acceptSocket != null) {
            try {
                Socket socket = acceptSocket.accept();
                socket.setTcpNoDelay(true);
                acceptClient(socket);
            } catch (SocketException exception) {
                if (running.get()) {
                    continue;
                }
                return;
            } catch (IOException exception) {
                if (!running.get()) {
                    return;
                }
            }
        }
    }

    private void acceptClient(Socket socket) throws IOException {
        PushbackInputStream input = new PushbackInputStream(socket.getInputStream(), 1);
        int firstByte = peekInitialByte(socket, input);
        if (firstByte == -1) {
            socket.close();
            return;
        }
        if (firstByte == SERVER_LIST_PING_PACKET_ID) {
            respondToLegacyServerListPing(socket, input);
            return;
        }
        if (isLegacyMinecraftClientPacket(firstByte)) {
            respondToUnsupportedLegacyClient(socket);
            return;
        }
        if (firstByte >= 0) {
            input.unread(firstByte);
        }

        String playerId = "player-" + nextPlayerId.getAndIncrement();
        ClientConnection client = new ClientConnection(playerId, socket, input);
        clients.put(playerId, client);

        try {
            client.send(new ProtocolMessage.Hello(
                    MultiplayerProtocol.VERSION,
                    playerId,
                    serverName
            ));
            client.start();
        } catch (IOException exception) {
            removeClient(client, "hello failed", false);
            throw exception;
        }
    }

    private void readClient(ClientConnection client) {
        try {
            String line;
            while (running.get() && client.isOpen() && (line = client.readLine()) != null) {
                ProtocolMessage message;
                try {
                    message = ProtocolCodec.decode(line);
                } catch (IllegalArgumentException exception) {
                    rejectClient(client, "Bad packet");
                    return;
                }
                if (message instanceof ProtocolMessage.KeepAlive keepAlive) {
                    if (client.acknowledgeKeepAlive(keepAlive.id(), System.currentTimeMillis())) {
                        broadcast(currentPlayerListSnapshot());
                    }
                    continue;
                }
                if (message instanceof ProtocolMessage.Join join) {
                    if (client.isJoined()) {
                        rejectClient(client, "Already joined");
                        return;
                    }
                    if (handleJoin(client, join)) {
                        try {
                            sendInitialWorldSync(client);
                        } catch (IOException exception) {
                            removeClient(client, "initial sync failed", true);
                            return;
                        }
                        notifyListeners(client.playerId(), join);
                        broadcast(currentPlayerListSnapshot());
                        ProtocolMessage.PlayerState joinedState = playerStates.get(client.playerId());
                        if (joinedState != null) {
                            broadcast(joinedState);
                        }
                    }
                    continue;
                }
                if (!client.isJoined()) {
                    rejectClient(client, "Join required");
                    return;
                }
                if (!isAcceptedClientMessage(client, message)) {
                    continue;
                }
                message = withCurrentDimension(normalizeAcceptedClientMessage(client, message));
                if (message instanceof ProtocolMessage.EntityUpdate) {
                    continue;
                }
                if (!(message instanceof ProtocolMessage.Disconnect)) {
                    notifyListeners(client.playerId(), message);
                }
                handleMessage(client, message);
            }
        } catch (IOException ignored) {
        } finally {
            removeClient(client, "connection closed", true);
        }
    }

    private void keepAliveLoop() {
        while (running.get()) {
            try {
                Thread.sleep(MultiplayerProtocol.KEEP_ALIVE_INTERVAL_MILLIS);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                return;
            }
            if (!running.get()) {
                return;
            }
            long now = System.currentTimeMillis();
            for (ClientConnection client : List.copyOf(clients.values())) {
                if (!client.isOpen()) {
                    removeClient(client, "connection closed", false);
                    continue;
                }
                if (!client.isJoined()) {
                    if (client.joinTimedOut(now)) {
                        rejectClient(client, "Join timeout");
                    }
                    continue;
                }
                if (client.keepAliveTimedOut(now)) {
                    removeClient(client, "Timed out", true);
                    continue;
                }
                if (client.hasPendingKeepAlive()) {
                    continue;
                }
                try {
                    client.sendKeepAlive(nextKeepAliveId.getAndIncrement(), now);
                } catch (IOException exception) {
                    removeClient(client, "Timed out", true);
                }
            }
        }
    }

    private boolean handleJoin(ClientConnection client, ProtocolMessage.Join join) {
        if (join.protocolVersion() != MultiplayerProtocol.VERSION) {
            rejectClient(client, "Outdated client");
            return false;
        }
        String username = normalizeUsername(join.username());
        if (username == null) {
            rejectClient(client, "Invalid username");
            return false;
        }
        if (listedPlayerCount() >= maxPlayers) {
            rejectClient(client, "The server is full");
            return false;
        }
        if (usernameInUse(username, client.playerId())) {
            rejectClient(client, "That username is already taken");
            return false;
        }
        String accessRejectReason = joinAdmission.rejectReason(username, client.remoteAddress());
        if (accessRejectReason != null && !accessRejectReason.isBlank()) {
            rejectClient(client, accessRejectReason);
            return false;
        }
        client.setUsername(username);
        client.markJoined();
        seedJoinedPlayerState(client);
        return true;
    }

    private void seedJoinedPlayerState(ClientConnection client) {
        seedJoinedPlayerState(client, null);
    }

    private void seedJoinedPlayerState(ClientConnection client, ProtocolMessage.PlayerState retainedState) {
        ProtocolMessage.PlayerPose spawnPose =
                new ProtocolMessage.PlayerPose(spawnX + 0.5d, spawnY, spawnZ + 0.5d, 0.0f, 0.0f);
        int selectedSlot = retainedSelectedSlot(retainedState);
        ProtocolMessage.InventoryUpdate held =
                inventoryStates.get(inventoryUpdateKey(client.playerId(), selectedSlot));
        String heldItemId = "air";
        int heldItemCount = 0;
        int heldItemDamage = 0;
        if (held != null && isValidInventoryUpdate(held)) {
            heldItemId = held.itemId();
            heldItemCount = held.count();
            heldItemDamage = held.damage();
        } else if (retainedState != null && isValidPlayerState(retainedState)) {
            heldItemId = retainedState.heldItemId();
            heldItemCount = retainedState.heldItemCount();
            heldItemDamage = retainedState.heldItemDamage();
        }
        client.resetFloatingCheck(spawnPose);
        playerStates.put(client.playerId(), new ProtocolMessage.PlayerState(
                client.playerId(),
                client.username(),
                spawnPose,
                true,
                false,
                20.0f,
                heldItemId,
                heldItemCount,
                heldItemDamage,
                selectedSlot,
                gameMode,
                dataWithCurrentDimension(Map.of())
        ));
    }

    private static int retainedSelectedSlot(ProtocolMessage.PlayerState retainedState) {
        if (retainedState == null) {
            return 0;
        }
        int selectedSlot = retainedState.selectedSlot();
        if (selectedSlot < 0 || selectedSlot >= MultiplayerProtocol.INVENTORY_HOTBAR_SLOTS) {
            return 0;
        }
        return selectedSlot;
    }

    private void handleMessage(ClientConnection client, ProtocolMessage message) {
        if (message instanceof ProtocolMessage.Join join) {
            handleJoin(client, join);
            return;
        }

        if (message instanceof ProtocolMessage.ClientInput input) {
            if (!isValidClientInput(input)) {
                return;
            }
            if (!acceptClientFlightState(client, input)) {
                removeClient(client, "Flying is not enabled on this server", true);
                return;
            }
            ProtocolMessage.PlayerState state = normalizedClientPlayerState(client, input);
            playerStates.put(client.playerId(), state);
            broadcast(state);
            return;
        }

        if (message instanceof ProtocolMessage.ClientAction action) {
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
            if (chat.text().trim().startsWith("/")) {
                return;
            }
            ProtocolMessage.Chat normalized = chat
                    .withPlayerId(client.playerId())
                    .withSender(client.username());
            broadcast(normalized);
            return;
        }

        if (message instanceof ProtocolMessage.Disconnect disconnect) {
            removeClient(client, disconnect.reason(), true);
        }
    }

    private void notifyListeners(String playerId, ProtocolMessage message) {
        for (ServerMessageListener listener : listeners) {
            listener.onMessage(playerId, message);
        }
    }

    private ProtocolMessage.PlayerState normalizedClientPlayerState(
            ClientConnection client, ProtocolMessage.ClientInput input) {
        int selectedSlot = Math.max(0, Math.min(
                MultiplayerProtocol.INVENTORY_HOTBAR_SLOTS - 1,
                input.selectedSlot()));
        ProtocolMessage.InventoryUpdate held = inventoryStates.get(inventoryUpdateKey(client.playerId(), selectedSlot));
        String heldItemId = held == null ? "air" : held.itemId();
        int heldItemCount = held == null ? 0 : held.count();
        int heldItemDamage = held == null ? 0 : held.damage();
        return new ProtocolMessage.PlayerState(
                client.playerId(),
                client.username(),
                input.pose(),
                input.onGround(),
                input.sneaking(),
                input.health(),
                heldItemId,
                heldItemCount,
                heldItemDamage,
                selectedSlot,
                input.gameMode(),
                playerStateDataFor(client.playerId(), input)
        );
    }

    private boolean acceptClientFlightState(ClientConnection client, ProtocolMessage.ClientInput input) {
        if (client == null || input == null || input.pose() == null) {
            return false;
        }
        if (allowFlight || input.onGround() || isCreativeGameMode(input.gameMode()) || input.health() <= 0.0f) {
            client.resetFloatingCheck(input.pose());
            return true;
        }
        ProtocolMessage.PlayerPose previous = client.lastPose();
        if (previous == null) {
            client.resetFloatingCheck(input.pose());
            return true;
        }
        double verticalDelta = input.pose().y() - previous.y();
        client.setLastPose(input.pose());
        if (verticalDelta < FLYING_CHECK_FALL_RESET_VELOCITY) {
            client.clearFloatingStart();
            return true;
        }
        long now = System.currentTimeMillis();
        if (!client.hasFloatingStart()) {
            client.setFloatingStart(now);
            return true;
        }
        return now - client.floatingStartMillis() <= FLYING_CHECK_GRACE_MILLIS;
    }

    private static boolean isCreativeGameMode(String gameMode) {
        return gameMode != null && "CREATIVE".equalsIgnoreCase(gameMode.trim());
    }

    private Map<String, String> playerStateDataFor(String playerId, ProtocolMessage.ClientInput input) {
        java.util.LinkedHashMap<String, String> data = new java.util.LinkedHashMap<>();
        if (input != null) {
            copyClientPlayerStateData(data, input.data());
            putClientInputState(data, input);
        }
        putPlayerArmorState(data, playerId);
        data.put(DIMENSION_DATA_KEY, currentDimension());
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
            ProtocolMessage.InventoryUpdate update = inventoryStates.get(
                    inventoryUpdateKey(playerId, armorStart + armorSlot));
            putInventoryUpdateStackData(data, "armor." + armorSlot, update);
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

    private void removeClient(ClientConnection client, String reason, boolean announce) {
        if (!clients.remove(client.playerId(), client)) {
            client.close();
            return;
        }

        playerStates.remove(client.playerId());
        removeInventoryState(client.playerId());
        boolean joined = client.isJoined();
        client.close();
        if (announce && running.get() && joined) {
            ProtocolMessage.Disconnect disconnect = new ProtocolMessage.Disconnect(client.playerId(), "Disconnected");
            notifyListeners(client.playerId(), disconnect);
            broadcast(disconnect);
            broadcast(currentPlayerListSnapshot());
        }
    }

    private void rejectClient(ClientConnection client, String reason) {
        try {
            client.send(new ProtocolMessage.Disconnect(client.playerId(), reason));
        } catch (IOException ignored) {
        }
        removeClient(client, reason, false);
    }

    private boolean usernameInUse(String username, String joiningPlayerId) {
        for (ClientConnection client : clients.values()) {
            if (!client.playerId().equals(joiningPlayerId) && client.isJoined()
                    && client.username().equalsIgnoreCase(username)) {
                return true;
            }
        }
        for (ProtocolMessage.PlayerState state : playerStates.values()) {
            if (state != null
                    && !state.playerId().equals(joiningPlayerId)
                    && state.username().equalsIgnoreCase(username)) {
                return true;
            }
        }
        return false;
    }

    private String normalizeUsername(String username) {
        if (username == null) {
            return null;
        }
        String trimmed = username.trim();
        if (trimmed.isEmpty() || trimmed.length() > 16) {
            return null;
        }
        for (int i = 0; i < trimmed.length(); i++) {
            char c = trimmed.charAt(i);
            if (!(c == '_' || Character.isDigit(c)
                    || (c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z'))) {
                return null;
            }
        }
        return trimmed;
    }

    private int peekInitialByte(Socket socket, PushbackInputStream input) throws IOException {
        int previousTimeout = socket.getSoTimeout();
        socket.setSoTimeout(PING_PEEK_TIMEOUT_MILLIS);
        try {
            return input.read();
        } catch (SocketTimeoutException ignored) {
            return -2;
        } finally {
            socket.setSoTimeout(previousTimeout);
        }
    }

    private void respondToLegacyServerListPing(Socket socket, PushbackInputStream input) {
        respondWithLegacyPacket(socket, legacyServerListPingResponse(isExtendedLegacyServerListPing(socket, input)));
    }

    private void respondToUnsupportedLegacyClient(Socket socket) {
        respondWithLegacyKickPacket(socket, LEGACY_PROTOCOL_REQUIRED_REASON);
    }

    private static boolean isLegacyMinecraftClientPacket(int firstByte) {
        return firstByte == LEGACY_LOGIN_PACKET_ID || firstByte == LEGACY_HANDSHAKE_PACKET_ID;
    }

    private void respondWithLegacyKickPacket(Socket socket, String message) {
        respondWithLegacyPacket(socket, LegacyServerStatus.sanitizeKickText(message, LEGACY_PROTOCOL_REQUIRED_REASON));
    }

    private void respondWithLegacyPacket(Socket socket, String response) {
        try (socket; DataOutputStream out = new DataOutputStream(socket.getOutputStream())) {
            response = response == null || response.isBlank() ? LEGACY_PROTOCOL_REQUIRED_REASON : response;
            out.writeByte(KICK_PACKET_ID);
            out.writeShort(response.length());
            out.write(response.getBytes(StandardCharsets.UTF_16BE));
            out.flush();
        } catch (IOException ignored) {
        }
    }

    private boolean isExtendedLegacyServerListPing(Socket socket, PushbackInputStream input) {
        if (socket == null || input == null) {
            return false;
        }
        int previousTimeout;
        try {
            previousTimeout = socket.getSoTimeout();
        } catch (SocketException exception) {
            return false;
        }
        try {
            socket.setSoTimeout(PING_PEEK_TIMEOUT_MILLIS);
            int next = input.read();
            return next == 0x01;
        } catch (SocketTimeoutException ignored) {
            return false;
        } catch (IOException ignored) {
            return false;
        } finally {
            try {
                socket.setSoTimeout(previousTimeout);
            } catch (SocketException ignored) {
            }
        }
    }

    private String legacyServerListPingResponse(boolean extended) {
        LegacyServerStatus status = LegacyServerStatus.of(serverName, listedPlayerCount(), maxPlayers);
        return extended ? status.toExtendedStatusText() : status.toReleaseOneStatusText();
    }

    private void startQueryResponder() {
        if (!queryEnabled || querySocket != null) {
            return;
        }
        DatagramSocket socket = null;
        try {
            socket = new DatagramSocket(new InetSocketAddress(
                    requestedBindAddress.isBlank() ? "0.0.0.0" : requestedBindAddress, queryPort));
            socket.setSoTimeout(QUERY_SOCKET_TIMEOUT_MILLIS);
        } catch (IOException exception) {
            if (socket != null) {
                socket.close();
            }
            return;
        }
        querySocket = socket;
        Thread thread = new Thread(this::queryLoop, "CraftZero-Query");
        thread.setDaemon(true);
        queryThread = thread;
        thread.start();
    }

    private void stopQueryResponder() {
        DatagramSocket socket = querySocket;
        querySocket = null;
        if (socket != null) {
            socket.close();
        }
        Thread thread = queryThread;
        queryThread = null;
        if (thread != null && thread != Thread.currentThread()) {
            try {
                thread.join(1000L);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private void queryLoop() {
        DatagramSocket socket = querySocket;
        Map<String, QueryChallenge> challenges = new ConcurrentHashMap<>();
        Random random = new Random(worldSeed ^ System.nanoTime());
        byte[] buffer = new byte[1460];
        while (running.get() && queryEnabled && socket != null && !socket.isClosed()) {
            try {
                DatagramPacket request = new DatagramPacket(buffer, buffer.length);
                socket.receive(request);
                DatagramPacket response = queryResponse(request, Arrays.copyOf(request.getData(), request.getLength()),
                        challenges, random);
                if (response != null) {
                    socket.send(response);
                }
                pruneQueryChallenges(challenges, System.currentTimeMillis());
            } catch (SocketTimeoutException ignored) {
                pruneQueryChallenges(challenges, System.currentTimeMillis());
            } catch (SocketException exception) {
                return;
            } catch (IOException ignored) {
            }
            socket = querySocket;
        }
    }

    private DatagramPacket queryResponse(DatagramPacket request, byte[] data, Map<String, QueryChallenge> challenges,
            Random random) {
        if (request == null || data == null || data.length < 7
                || (data[0] & 0xFF) != QUERY_MAGIC_0 || (data[1] & 0xFF) != QUERY_MAGIC_1) {
            return null;
        }
        byte type = data[2];
        int sessionId = readQueryInt(data, 3);
        String addressKey = request.getAddress().getHostAddress() + ":" + request.getPort();
        if (type == QUERY_HANDSHAKE_PACKET_ID) {
            int token = QUERY_TOKEN_MIN + random.nextInt(QUERY_TOKEN_RANGE);
            challenges.put(addressKey, new QueryChallenge(token, System.currentTimeMillis()));
            return queryPacket(request, queryHandshakePayload(sessionId, token));
        }
        if (type != QUERY_STATUS_PACKET_ID || data.length < 11) {
            return null;
        }
        QueryChallenge challenge = challenges.get(addressKey);
        int token = readQueryInt(data, 7);
        if (challenge == null || challenge.isExpired(System.currentTimeMillis()) || challenge.token() != token) {
            return null;
        }
        boolean fullStat = data.length >= 15;
        return queryPacket(request, fullStat ? queryFullStatPayload(sessionId) : queryBasicStatPayload(sessionId));
    }

    private byte[] queryHandshakePayload(int sessionId, int token) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(QUERY_HANDSHAKE_PACKET_ID);
        writeQueryInt(out, sessionId);
        writeQueryString(out, Integer.toString(token));
        return out.toByteArray();
    }

    private byte[] queryBasicStatPayload(int sessionId) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(QUERY_STATUS_PACKET_ID);
        writeQueryInt(out, sessionId);
        writeQueryString(out, LegacyServerStatus.sanitizeStatusText(serverName, "CraftZero", false));
        writeQueryString(out, "SMP");
        writeQueryString(out, queryMapName());
        writeQueryString(out, Integer.toString(listedPlayerCount()));
        writeQueryString(out, Integer.toString(maxPlayers));
        writeQueryShortLittleEndian(out, getPort());
        writeQueryString(out, queryHostIp());
        return out.toByteArray();
    }

    private byte[] queryFullStatPayload(int sessionId) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(QUERY_STATUS_PACKET_ID);
        writeQueryInt(out, sessionId);
        out.write(0);
        out.write(0);
        out.write(0);
        out.write(0);
        writeQueryKeyValue(out, "hostname", LegacyServerStatus.sanitizeStatusText(serverName, "CraftZero", false));
        writeQueryKeyValue(out, "gametype", "SMP");
        writeQueryKeyValue(out, "game_id", "MINECRAFT");
        writeQueryKeyValue(out, "version", "CraftZero");
        writeQueryKeyValue(out, "plugins", "");
        writeQueryKeyValue(out, "map", queryMapName());
        writeQueryKeyValue(out, "numplayers", Integer.toString(listedPlayerCount()));
        writeQueryKeyValue(out, "maxplayers", Integer.toString(maxPlayers));
        writeQueryKeyValue(out, "hostport", Integer.toString(getPort()));
        writeQueryKeyValue(out, "hostip", queryHostIp());
        out.write(0);
        out.write(0);
        out.write(1);
        writeQueryString(out, "player_");
        out.write(0);
        for (String playerName : queryPlayerNames()) {
            writeQueryString(out, playerName);
        }
        out.write(0);
        return out.toByteArray();
    }

    private List<String> queryPlayerNames() {
        return currentPlayerListSnapshot().players().stream()
                .map(ProtocolMessage.PlayerListEntry::username)
                .map(name -> LegacyServerStatus.sanitizeStatusText(name, "Player", false))
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();
    }

    private String queryMapName() {
        return normalizeText(dimension, "overworld");
    }

    private String queryHostIp() {
        if (!requestedBindAddress.isBlank()) {
            return requestedBindAddress;
        }
        DatagramSocket socket = querySocket;
        if (socket != null && socket.getLocalAddress() != null) {
            return socket.getLocalAddress().getHostAddress();
        }
        return "0.0.0.0";
    }

    private DatagramPacket queryPacket(DatagramPacket request, byte[] payload) {
        return new DatagramPacket(payload, payload.length, request.getAddress(), request.getPort());
    }

    private static int readQueryInt(byte[] data, int offset) {
        return (data[offset] & 0xFF) << 24
                | (data[offset + 1] & 0xFF) << 16
                | (data[offset + 2] & 0xFF) << 8
                | (data[offset + 3] & 0xFF);
    }

    private static void writeQueryInt(ByteArrayOutputStream out, int value) {
        out.write((value >>> 24) & 0xFF);
        out.write((value >>> 16) & 0xFF);
        out.write((value >>> 8) & 0xFF);
        out.write(value & 0xFF);
    }

    private static void writeQueryShortLittleEndian(ByteArrayOutputStream out, int value) {
        out.write(value & 0xFF);
        out.write((value >>> 8) & 0xFF);
    }

    private static void writeQueryKeyValue(ByteArrayOutputStream out, String key, String value) {
        writeQueryString(out, key);
        writeQueryString(out, value);
    }

    private static void writeQueryString(ByteArrayOutputStream out, String value) {
        if (value != null && !value.isBlank()) {
            byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
            out.write(bytes, 0, bytes.length);
        }
        out.write(0);
    }

    private static void pruneQueryChallenges(Map<String, QueryChallenge> challenges, long nowMillis) {
        challenges.entrySet().removeIf(entry -> entry.getValue().isExpired(nowMillis));
    }

    private ProtocolMessage.WorldState currentWorldStateSnapshot() {
        return new ProtocolMessage.WorldState(
                worldSeed,
                worldTime,
                worldWeather,
                spawnX,
                spawnY,
                spawnZ,
                gameMode,
                difficulty,
                hardcore,
                allowCheats,
                pvp,
                spawnAnimals,
                spawnMonsters,
                spawnNpcs,
                allowNether,
                allowFlight,
                dimension,
                maxPlayers,
                viewDistance,
                maxBuildHeight,
                generateStructures,
                currentPlayers(),
                currentBlockUpdates(),
                currentEntityUpdates(),
                currentInventoryUpdates()
        );
    }

    private ProtocolMessage.WorldState currentWorldMetadataSnapshot() {
        return new ProtocolMessage.WorldState(
                worldSeed,
                worldTime,
                worldWeather,
                spawnX,
                spawnY,
                spawnZ,
                gameMode,
                difficulty,
                hardcore,
                allowCheats,
                pvp,
                spawnAnimals,
                spawnMonsters,
                spawnNpcs,
                allowNether,
                allowFlight,
                dimension,
                maxPlayers,
                viewDistance,
                maxBuildHeight,
                generateStructures,
                List.of(),
                List.of(),
                List.of(),
                List.of()
        );
    }

    private void sendInitialWorldSync(ClientConnection client) throws IOException {
        List<ProtocolMessage.PlayerState> players = currentPlayers();
        List<ProtocolMessage.BlockUpdate> blockUpdates = currentBlockUpdates();
        List<ProtocolMessage.EntityUpdate> entityUpdates = currentEntityUpdates();
        List<ProtocolMessage.InventoryUpdate> inventoryUpdates = currentInventoryUpdates();
        client.send(currentPlayerListSnapshot());
        client.send(currentWorldMetadataSnapshot());
        sendInitialSnapshotMessages(client, players, blockUpdates, entityUpdates, inventoryUpdates);
        client.send(initialSyncCompleteMessage(players.size(), blockUpdates.size(),
                entityUpdates.size(), inventoryUpdates.size()));
    }

    private void sendInitialSnapshotMessages(ClientConnection client,
            List<ProtocolMessage.PlayerState> players,
            List<ProtocolMessage.BlockUpdate> blockUpdates,
            List<ProtocolMessage.EntityUpdate> entityUpdates,
            List<ProtocolMessage.InventoryUpdate> inventoryUpdates) throws IOException {
        for (ProtocolMessage.PlayerState playerState : players) {
            client.send(playerState);
        }
        for (ProtocolMessage.BlockUpdate blockUpdate : blockUpdates) {
            client.send(blockUpdate);
        }
        for (ProtocolMessage.EntityUpdate entityUpdate : entityUpdates) {
            client.send(entityUpdate);
        }
        for (ProtocolMessage.InventoryUpdate inventoryUpdate : inventoryUpdates) {
            client.send(inventoryUpdate);
        }
    }

    private ProtocolMessage.WorldEvent initialSyncCompleteMessage(
            int players, int blockUpdates, int entityUpdates, int inventoryUpdates) {
        LinkedHashMap<String, String> data = new LinkedHashMap<>();
        data.put(DIMENSION_DATA_KEY, currentDimension());
        data.put("players", Integer.toString(Math.max(0, players)));
        data.put("blockUpdates", Integer.toString(Math.max(0, blockUpdates)));
        data.put("entityUpdates", Integer.toString(Math.max(0, entityUpdates)));
        data.put("inventoryUpdates", Integer.toString(Math.max(0, inventoryUpdates)));
        return new ProtocolMessage.WorldEvent(MultiplayerProtocol.EVENT_INITIAL_SYNC_COMPLETE, data);
    }

    private ProtocolMessage.PlayerList currentPlayerListSnapshot() {
        LinkedHashMap<String, ProtocolMessage.PlayerListEntry> players = new LinkedHashMap<>();
        playerStates.values().stream()
                .filter(ProtocolServer::isValidPlayerState)
                .sorted(Comparator.comparing(ProtocolMessage.PlayerState::playerId))
                .forEach(state -> players.put(state.playerId(), new ProtocolMessage.PlayerListEntry(
                        state.playerId(),
                        state.username(),
                        0)));
        clients.values().stream()
                .filter(ClientConnection::isJoined)
                .sorted(Comparator.comparing(ClientConnection::playerId))
                .forEach(client -> players.put(client.playerId(), new ProtocolMessage.PlayerListEntry(
                        client.playerId(),
                        client.username(),
                        client.latencyMillis())));
        return new ProtocolMessage.PlayerList(List.copyOf(players.values()));
    }

    private int listedPlayerCount() {
        return currentPlayerListSnapshot().players().size();
    }

    private static String normalizeText(String value, String fallback) {
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

    private static int clampPort(int port) {
        return port <= 0 || port > 65535 ? MultiplayerProtocol.DEFAULT_QUERY_PORT : port;
    }

    private boolean isAcceptedClientMessage(ClientConnection client, ProtocolMessage message) {
        if (message instanceof ProtocolMessage.EntityUpdate) {
            return false;
        }
        if (message instanceof ProtocolMessage.WorldEvent) {
            return false;
        }
        if (message instanceof ProtocolMessage.InventoryUpdate) {
            return false;
        }
        if (message instanceof ProtocolMessage.BlockUpdate) {
            return false;
        }
        if (message instanceof ProtocolMessage.PlayerState
                || message instanceof ProtocolMessage.WorldState
                || message instanceof ProtocolMessage.PlayerList
                || message instanceof ProtocolMessage.Hello) {
            return false;
        }
        if (message instanceof ProtocolMessage.ClientAction action) {
            return isValidClientAction(client, action);
        }
        if (message instanceof ProtocolMessage.ClientInput input) {
            return isValidClientInput(input);
        }
        if (message instanceof ProtocolMessage.Chat chat) {
            return isValidChat(chat);
        }
        return isValidStateMessage(message);
    }

    private ProtocolMessage normalizeAcceptedClientMessage(ClientConnection client, ProtocolMessage message) {
        if (message instanceof ProtocolMessage.Chat chat) {
            return new ProtocolMessage.Chat(client.playerId(), client.username(), chat.text());
        }
        if (message instanceof ProtocolMessage.ClientAction action) {
            return new ProtocolMessage.ClientAction(
                    client.playerId(),
                    action.action(),
                    sanitizeClientBlockUpdate(client, action.blockUpdate()),
                    action.data()
            );
        }
        return message;
    }

    private static boolean isValidStateMessage(ProtocolMessage message) {
        if (message == null) {
            return false;
        }
        if (message instanceof ProtocolMessage.WorldState worldState) {
            return isValidWorldState(worldState);
        }
        if (message instanceof ProtocolMessage.PlayerList playerList) {
            return isValidPlayerList(playerList);
        }
        if (message instanceof ProtocolMessage.BlockUpdate blockUpdate) {
            return isValidBlockUpdate(blockUpdate);
        }
        if (message instanceof ProtocolMessage.InventoryUpdate inventoryUpdate) {
            return isValidInventoryUpdate(inventoryUpdate);
        }
        if (message instanceof ProtocolMessage.PlayerState playerState) {
            return isValidPlayerState(playerState);
        }
        if (message instanceof ProtocolMessage.EntityUpdate entityUpdate) {
            return isValidEntityUpdate(entityUpdate);
        }
        if (message instanceof ProtocolMessage.WorldEvent worldEvent) {
            return MultiplayerProtocol.isValidNetworkId(worldEvent.eventType())
                    && MultiplayerProtocol.isValidItemStackData(worldEvent.data());
        }
        if (message instanceof ProtocolMessage.Chat chat) {
            return isValidChat(chat);
        }
        if (message instanceof ProtocolMessage.ClientAction action) {
            return isValidServerClientAction(action);
        }
        if (message instanceof ProtocolMessage.Disconnect disconnect) {
            return isValidDisconnect(disconnect);
        }
        return false;
    }

    private static boolean isValidWorldState(ProtocolMessage.WorldState worldState) {
        if (worldState == null
                || !isFinite(worldState.timeOfDay())
                || !isValidWorldCoordinate(worldState.spawnX())
                || !isValidWorldCoordinate(worldState.spawnZ())
                || worldState.spawnY() < MultiplayerProtocol.WORLD_MIN_Y
                || worldState.spawnY() >= MultiplayerProtocol.WORLD_HEIGHT
                || !MultiplayerProtocol.isValidNetworkId(worldState.weatherState())
                || !MultiplayerProtocol.isValidNetworkId(worldState.gameMode())
                || !MultiplayerProtocol.isValidNetworkId(worldState.difficulty())
                || !MultiplayerProtocol.isValidNetworkId(worldState.dimension())
                || worldState.maxPlayers() < 1
                || worldState.viewDistance() < MultiplayerProtocol.MIN_VIEW_DISTANCE
                || worldState.viewDistance() > MultiplayerProtocol.MAX_VIEW_DISTANCE
                || worldState.maxBuildHeight() < MultiplayerProtocol.MIN_MAX_BUILD_HEIGHT
                || worldState.maxBuildHeight() > MultiplayerProtocol.WORLD_HEIGHT) {
            return false;
        }
        return worldState.players().size() <= MultiplayerProtocol.MAX_LEGACY_MESSAGE_DATA_ENTRIES
                && worldState.blockUpdates().size() <= MultiplayerProtocol.MAX_LEGACY_MESSAGE_DATA_ENTRIES
                && worldState.entityUpdates().size() <= MultiplayerProtocol.MAX_LEGACY_MESSAGE_DATA_ENTRIES
                && worldState.inventoryUpdates().size() <= MultiplayerProtocol.MAX_LEGACY_MESSAGE_DATA_ENTRIES
                && worldState.players().stream().allMatch(ProtocolServer::isValidPlayerState)
                && worldState.blockUpdates().stream().allMatch(ProtocolServer::isValidBlockUpdate)
                && worldState.entityUpdates().stream().allMatch(ProtocolServer::isValidEntityUpdate)
                && worldState.inventoryUpdates().stream().allMatch(ProtocolServer::isValidInventoryUpdate);
    }

    private static boolean isValidPlayerList(ProtocolMessage.PlayerList playerList) {
        return playerList != null
                && playerList.players().size() <= MultiplayerProtocol.MAX_LEGACY_MESSAGE_DATA_ENTRIES
                && playerList.players().stream().allMatch(entry -> entry != null
                && MultiplayerProtocol.isValidNetworkId(entry.playerId())
                && isValidUsername(entry.username())
                && entry.latencyMillis() >= -1);
    }

    private static boolean isValidBlockUpdate(ProtocolMessage.BlockUpdate update) {
        return update != null
                && isValidWorldCoordinate(update.x())
                && isValidWorldCoordinate(update.z())
                && MultiplayerProtocol.isValidBlockUpdate(update.blockId(), update.y(), update.metadata())
                && isValidOptionalNetworkId(update.sourcePlayerId())
                && MultiplayerProtocol.isValidItemStackData(update.data());
    }

    private static boolean isValidServerClientAction(ProtocolMessage.ClientAction action) {
        if (action == null
                || !isValidOptionalNetworkId(action.playerId())
                || !isKnownServerClientAction(action.action())
                || !isValidServerActionData(action.data())) {
            return false;
        }
        if (action.blockUpdate() != null) {
            return ProtocolMessage.Type.BLOCK_UPDATE.equals(action.action())
                    && isValidBlockUpdate(action.blockUpdate());
        }
        return isValidServerClientActionPayload(action);
    }

    private static boolean isValidServerActionData(Map<String, String> data) {
        if (data == null) {
            return true;
        }
        if (data.size() > MultiplayerProtocol.MAX_CONTAINER_UPDATE_DATA_ENTRIES) {
            return false;
        }
        for (Map.Entry<String, String> entry : data.entrySet()) {
            if (!MultiplayerProtocol.isValidLegacyDataKey(entry.getKey())
                    || !MultiplayerProtocol.isValidLegacyDataValue(entry.getValue())) {
                return false;
            }
        }
        return true;
    }

    private static boolean isKnownServerClientAction(String action) {
        return switch (action) {
            case ProtocolMessage.Type.BLOCK_UPDATE,
                    MultiplayerProtocol.ACTION_BED_SLEEP_START,
                    MultiplayerProtocol.ACTION_BED_SLEEP_STOP,
                    MultiplayerProtocol.ACTION_BED_SLEEP_COMPLETE,
                    MultiplayerProtocol.ACTION_SIGN_UPDATE,
                    MultiplayerProtocol.ACTION_CONTAINER_UPDATE,
                    MultiplayerProtocol.ACTION_ENCHANT_ITEM,
                    MultiplayerProtocol.ACTION_CRAFT_ITEM,
                    MultiplayerProtocol.ACTION_INVENTORY_SYNC,
                    MultiplayerProtocol.ACTION_ENTITY_ATTACK,
                    MultiplayerProtocol.ACTION_ENTITY_USE,
                    MultiplayerProtocol.ACTION_PLAYER_ATTACK,
                    MultiplayerProtocol.ACTION_ITEM_USE,
                    MultiplayerProtocol.ACTION_PLAYER_RESPAWN,
                    MultiplayerProtocol.ACTION_COMMAND_PRIVATE_MESSAGE,
                    MultiplayerProtocol.ACTION_COMMAND_GIVE,
                    MultiplayerProtocol.ACTION_COMMAND_TELEPORT,
                    MultiplayerProtocol.ACTION_COMMAND_KILL,
                    MultiplayerProtocol.ACTION_COMMAND_CLEAR,
                    MultiplayerProtocol.ACTION_COMMAND_SPAWNPOINT,
                    MultiplayerProtocol.ACTION_COMMAND_GAMEMODE,
                    MultiplayerProtocol.ACTION_COMMAND_EXPERIENCE,
                    MultiplayerProtocol.ACTION_COMMAND_DAMAGE,
                    MultiplayerProtocol.ACTION_COMMAND_VELOCITY,
                    MultiplayerProtocol.ACTION_COMMAND_POTION_EFFECT -> true;
            default -> false;
        };
    }

    private static boolean isValidServerClientActionPayload(ProtocolMessage.ClientAction action) {
        Map<String, String> data = action.data();
        return switch (action.action()) {
            case MultiplayerProtocol.ACTION_BED_SLEEP_COMPLETE -> hasOnlyServerActionKeys(data, "time")
                    && optionalFiniteFloat(data, "time");
            case MultiplayerProtocol.ACTION_COMMAND_PRIVATE_MESSAGE -> hasTargetPlayer(action)
                    && hasOnlyServerActionKeys(data, "text")
                    && data.containsKey("text")
                    && MultiplayerProtocol.isValidLegacyDataValue(data.get("text"));
            case MultiplayerProtocol.ACTION_COMMAND_GIVE -> hasTargetPlayer(action)
                    && hasOnlyServerActionKeys(data, "itemId", "itemData", "count")
                    && isClampedIntegerText(data.get("itemId"), 0, Short.MAX_VALUE)
                    && optionalClampedInteger(data, "itemData", 0, Short.MAX_VALUE)
                    && isClampedIntegerText(data.get("count"), 1, MultiplayerProtocol.MAX_STACK_COUNT);
            case MultiplayerProtocol.ACTION_COMMAND_TELEPORT -> hasTargetPlayer(action)
                    && hasOnlyServerActionKeys(data, "x", "y", "z")
                    && isValidTeleportCoordinate(data.get("x"), false)
                    && isValidTeleportCoordinate(data.get("y"), true)
                    && isValidTeleportCoordinate(data.get("z"), false);
            case MultiplayerProtocol.ACTION_COMMAND_KILL -> hasTargetPlayer(action)
                    && hasOnlyServerActionKeys(data);
            case MultiplayerProtocol.ACTION_COMMAND_CLEAR -> hasTargetPlayer(action)
                    && hasOnlyServerActionKeys(data, "itemId", "itemData")
                    && optionalClampedInteger(data, "itemId", 0, Short.MAX_VALUE)
                    && optionalClampedInteger(data, "itemData", 0, Short.MAX_VALUE);
            case MultiplayerProtocol.ACTION_COMMAND_SPAWNPOINT -> hasTargetPlayer(action)
                    && hasOnlyServerActionKeys(data, "x", "y", "z")
                    && hasAllOrNone(data, "x", "y", "z")
                    && optionalTeleportCoordinate(data, "x", false)
                    && optionalTeleportCoordinate(data, "y", true)
                    && optionalTeleportCoordinate(data, "z", false);
            case MultiplayerProtocol.ACTION_COMMAND_GAMEMODE -> hasTargetPlayer(action)
                    && hasOnlyServerActionKeys(data, "gameMode")
                    && data.containsKey("gameMode")
                    && MultiplayerProtocol.isValidNetworkId(data.get("gameMode"));
            case MultiplayerProtocol.ACTION_COMMAND_EXPERIENCE -> hasTargetPlayer(action)
                    && hasOnlyServerActionKeys(data, "amount", "pickup")
                    && isClampedIntegerText(data.get("amount"), 0, Integer.MAX_VALUE)
                    && optionalBoolean(data, "pickup");
            case MultiplayerProtocol.ACTION_COMMAND_DAMAGE -> hasTargetPlayer(action)
                    && hasOnlyServerActionKeys(data, "amount", "damageType", "sourceX", "sourceY", "sourceZ",
                            "horizontalKnockback", "verticalKnockback", "fireTicks", "sourcePlayerId")
                    && optionalClampedFiniteFloat(data, "amount", 0.0f, MultiplayerProtocol.MAX_PLAYER_HEALTH)
                    && optionalNetworkId(data, "damageType")
                    && optionalNetworkId(data, "sourcePlayerId")
                    && optionalTeleportCoordinate(data, "sourceX", false)
                    && optionalTeleportCoordinate(data, "sourceY", true)
                    && optionalTeleportCoordinate(data, "sourceZ", false)
                    && optionalClampedFiniteFloat(data, "horizontalKnockback", 0.0f, 16.0f)
                    && optionalClampedFiniteFloat(data, "verticalKnockback", 0.0f, 16.0f)
                    && optionalClampedInteger(data, "fireTicks", 0, MultiplayerProtocol.MAX_CLIENT_FIRE_TICKS);
            case MultiplayerProtocol.ACTION_COMMAND_VELOCITY -> hasTargetPlayer(action)
                    && hasOnlyServerActionKeys(data, "motionX", "motionY", "motionZ")
                    && isClampedFiniteFloatText(data.get("motionX"), -16.0f, 16.0f)
                    && isClampedFiniteFloatText(data.get("motionY"), -16.0f, 16.0f)
                    && isClampedFiniteFloatText(data.get("motionZ"), -16.0f, 16.0f);
            case MultiplayerProtocol.ACTION_COMMAND_POTION_EFFECT -> hasTargetPlayer(action)
                    && hasOnlyServerActionKeys(data, "potionType", "potionSplash", "potionExtended",
                            "potionEnhanced", "strength", "effectType", "duration", "amplifier")
                    && isValidCommandPotionEffectPayload(data);
            case ProtocolMessage.Type.BLOCK_UPDATE -> false;
            default -> true;
        };
    }

    private static boolean hasTargetPlayer(ProtocolMessage.ClientAction action) {
        return action != null && MultiplayerProtocol.isValidNetworkId(action.playerId());
    }

    private static boolean isValidCommandPotionEffectPayload(Map<String, String> data) {
        if (data == null) {
            return false;
        }
        boolean hasPotionPayload = data.containsKey("potionType")
                || data.containsKey("potionSplash")
                || data.containsKey("potionExtended")
                || data.containsKey("potionEnhanced")
                || data.containsKey("strength");
        boolean hasStatusPayload = data.containsKey("effectType")
                || data.containsKey("duration")
                || data.containsKey("amplifier");
        if (hasPotionPayload == hasStatusPayload) {
            return false;
        }
        if (hasStatusPayload) {
            return data.containsKey("effectType")
                    && data.containsKey("duration")
                    && optionalNetworkId(data, "effectType")
                    && isClampedIntegerText(data.get("duration"), 1,
                            MultiplayerProtocol.MAX_CLIENT_STATUS_EFFECT_DURATION)
                    && optionalClampedInteger(data, "amplifier", 0,
                            MultiplayerProtocol.MAX_CLIENT_STATUS_EFFECT_AMPLIFIER);
        }
        return optionalNetworkId(data, "potionType")
                && optionalBoolean(data, "potionSplash")
                && optionalBoolean(data, "potionExtended")
                && optionalBoolean(data, "potionEnhanced")
                && optionalClampedFiniteFloat(data, "strength", 0.0f, 1.0f);
    }

    private static boolean hasOnlyServerActionKeys(Map<String, String> data, String... allowedKeys) {
        if (data == null) {
            return true;
        }
        java.util.HashSet<String> allowed = new java.util.HashSet<>();
        allowed.add("dimension");
        if (allowedKeys != null) {
            for (String key : allowedKeys) {
                if (key != null && !key.isBlank()) {
                    allowed.add(key);
                }
            }
        }
        for (String key : data.keySet()) {
            if (!allowed.contains(key)) {
                return false;
            }
        }
        return true;
    }

    private static boolean hasAllOrNone(Map<String, String> data, String... keys) {
        if (data == null || keys == null || keys.length == 0) {
            return true;
        }
        int present = 0;
        for (String key : keys) {
            if (data.containsKey(key)) {
                present++;
            }
        }
        return present == 0 || present == keys.length;
    }

    private static boolean optionalNetworkId(Map<String, String> data, String key) {
        return data == null || !data.containsKey(key) || MultiplayerProtocol.isValidNetworkId(data.get(key));
    }

    private static boolean optionalBoolean(Map<String, String> data, String key) {
        if (data == null || !data.containsKey(key)) {
            return true;
        }
        String value = data.get(key);
        return "true".equalsIgnoreCase(value) || "false".equalsIgnoreCase(value);
    }

    private static boolean optionalFiniteFloat(Map<String, String> data, String key) {
        return data == null || !data.containsKey(key) || isFiniteFloatText(data.get(key));
    }

    private static boolean optionalTeleportCoordinate(Map<String, String> data, String key, boolean yAxis) {
        return data == null || !data.containsKey(key) || isValidTeleportCoordinate(data.get(key), yAxis);
    }

    private static boolean isValidTeleportCoordinate(String value, boolean yAxis) {
        try {
            float parsed = Float.parseFloat(value);
            if (!isFinite(parsed)) {
                return false;
            }
            if (yAxis) {
                return parsed >= MultiplayerProtocol.MIN_PROTOCOL_PLAYER_Y
                        && parsed <= MultiplayerProtocol.MAX_PROTOCOL_PLAYER_Y;
            }
            return Math.abs(parsed) <= MultiplayerProtocol.MAX_WORLD_COORDINATE;
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    private static boolean optionalClampedInteger(Map<String, String> data, String key, int min, int max) {
        return data == null || !data.containsKey(key) || isClampedIntegerText(data.get(key), min, max);
    }

    private static boolean optionalClampedFiniteFloat(Map<String, String> data, String key, float min, float max) {
        return data == null || !data.containsKey(key) || isClampedFiniteFloatText(data.get(key), min, max);
    }

    private static boolean isFiniteFloatText(String value) {
        try {
            return isFinite(Float.parseFloat(value));
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    private static boolean isClampedIntegerText(String value, int min, int max) {
        try {
            int parsed = Integer.parseInt(value);
            return parsed >= min && parsed <= max;
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    private static boolean isClampedFiniteFloatText(String value, float min, float max) {
        try {
            float parsed = Float.parseFloat(value);
            return isFinite(parsed) && parsed >= min && parsed <= max;
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    private static boolean isValidDisconnect(ProtocolMessage.Disconnect disconnect) {
        return disconnect != null
                && isValidOptionalNetworkId(disconnect.playerId())
                && MultiplayerProtocol.isValidLegacyDataValue(disconnect.reason());
    }

    private static boolean isValidOptionalNetworkId(String value) {
        return value == null || value.isBlank() || MultiplayerProtocol.isValidNetworkId(value);
    }

    private static boolean isValidPlayerState(ProtocolMessage.PlayerState state) {
        return state != null
                && MultiplayerProtocol.isValidNetworkId(state.playerId())
                && isValidUsername(state.username())
                && isValidPlayerPose(state.pose())
                && isValidPlayerHealth(state.health())
                && MultiplayerProtocol.isValidNetworkId(state.heldItemId())
                && state.heldItemCount() >= 0
                && state.heldItemCount() <= MultiplayerProtocol.MAX_STACK_COUNT
                && state.heldItemDamage() >= MultiplayerProtocol.MIN_ITEM_DAMAGE
                && state.heldItemDamage() <= MultiplayerProtocol.MAX_ITEM_DAMAGE
                && state.selectedSlot() >= 0
                && state.selectedSlot() < MultiplayerProtocol.INVENTORY_HOTBAR_SLOTS
                && MultiplayerProtocol.isValidNetworkId(state.gameMode())
                && MultiplayerProtocol.isValidItemStackData(state.data());
    }

    private static boolean isValidEntityUpdate(ProtocolMessage.EntityUpdate update) {
        return update != null
                && MultiplayerProtocol.isValidNetworkId(update.entityId())
                && MultiplayerProtocol.isValidNetworkId(update.entityType())
                && isValidPlayerPose(update.pose())
                && MultiplayerProtocol.isValidItemStackData(update.data());
    }

    private static boolean isValidWorldCoordinate(int value) {
        return Math.abs((double) value) <= MultiplayerProtocol.MAX_WORLD_COORDINATE;
    }

    private static boolean isValidUsername(String username) {
        if (username == null) {
            return false;
        }
        String trimmed = username.trim();
        if (!trimmed.equals(username) || trimmed.isEmpty() || trimmed.length() > 16) {
            return false;
        }
        for (int i = 0; i < trimmed.length(); i++) {
            char c = trimmed.charAt(i);
            if (!(c == '_' || Character.isDigit(c)
                    || (c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z'))) {
                return false;
            }
        }
        return true;
    }

    private boolean isValidClientBlockUpdate(ClientConnection client, ProtocolMessage.BlockUpdate update) {
        return isValidBlockUpdate(update) && isBlockEditInReach(client, update);
    }

    private boolean isValidClientAction(ClientConnection client, ProtocolMessage.ClientAction action) {
        if (action == null || !MultiplayerProtocol.isValidNetworkId(action.action())) {
            return false;
        }
        if (action.blockUpdate() != null) {
            return isValidClientBlockAction(client, action);
        }
        if (isClientBedSleepAction(action)) {
            return action.blockUpdate() == null && action.data().isEmpty();
        }
        if (isClientEntityAction(action)) {
            return isValidClientEntityAction(client, action);
        }
        if (isClientPlayerAttackAction(action)) {
            return isValidClientPlayerAttackAction(client, action);
        }
        if (MultiplayerProtocol.ACTION_PLAYER_RESPAWN.equals(action.action())) {
            return isValidClientRespawnAction(client, action);
        }
        if (MultiplayerProtocol.ACTION_ITEM_USE.equals(action.action())) {
            return isValidClientItemUseAction(client, action);
        }
        if (MultiplayerProtocol.ACTION_CONTAINER_UPDATE.equals(action.action())) {
            return isValidClientContainerUpdateAction(client, action);
        }
        if (MultiplayerProtocol.ACTION_ENCHANT_ITEM.equals(action.action())) {
            return isValidClientEnchantItemAction(client, action);
        }
        if (MultiplayerProtocol.ACTION_CRAFT_ITEM.equals(action.action())) {
            return isValidClientCraftItemAction(client, action);
        }
        if (MultiplayerProtocol.ACTION_INVENTORY_SYNC.equals(action.action())) {
            return isValidClientInventorySyncAction(action);
        }
        return isClientSignUpdateAction(client, action);
    }

    private boolean isValidClientBlockAction(ClientConnection client, ProtocolMessage.ClientAction action) {
        return ProtocolMessage.Type.BLOCK_UPDATE.equals(action.action())
                && action.data().isEmpty()
                && isValidClientBlockUpdate(client, action.blockUpdate());
    }

    private static boolean isClientBedSleepAction(ProtocolMessage.ClientAction action) {
        return MultiplayerProtocol.ACTION_BED_SLEEP_START.equals(action.action())
                || MultiplayerProtocol.ACTION_BED_SLEEP_STOP.equals(action.action());
    }

    private static boolean isClientEntityAction(ProtocolMessage.ClientAction action) {
        return MultiplayerProtocol.ACTION_ENTITY_ATTACK.equals(action.action())
                || MultiplayerProtocol.ACTION_ENTITY_USE.equals(action.action());
    }

    private static boolean isClientPlayerAttackAction(ProtocolMessage.ClientAction action) {
        return MultiplayerProtocol.ACTION_PLAYER_ATTACK.equals(action.action());
    }

    private boolean isValidClientRespawnAction(ClientConnection client, ProtocolMessage.ClientAction action) {
        return client != null
                && action != null
                && action.blockUpdate() == null
                && action.data().isEmpty()
                && isDeadClientPlayerState(client);
    }

    private boolean isValidClientEntityAction(ClientConnection client, ProtocolMessage.ClientAction action) {
        if (client == null || action == null || !isValidActionDataText(action.data())
                || !hasOnlyActionKeys(action.data(), new String[] {"entityId"})) {
            return false;
        }
        String entityId = action.data().getOrDefault("entityId", "");
        if (!MultiplayerProtocol.isValidNetworkId(entityId)) {
            return false;
        }
        ProtocolMessage.EntityUpdate target = entityStates.get(entityId);
        if (target == null || target.pose() == null
                || "true".equalsIgnoreCase(target.data().getOrDefault("removed", "false"))) {
            return false;
        }
        return isEntityActionInReach(client, target);
    }

    private boolean isValidClientPlayerAttackAction(ClientConnection client, ProtocolMessage.ClientAction action) {
        if (client == null || action == null || !isValidActionDataText(action.data())
                || !hasOnlyActionKeys(action.data(), new String[] {"targetPlayerId"})) {
            return false;
        }
        String targetPlayerId = action.data().getOrDefault("targetPlayerId", "");
        if (!MultiplayerProtocol.isValidNetworkId(targetPlayerId) || targetPlayerId.equals(client.playerId())) {
            return false;
        }
        ProtocolMessage.PlayerState target = playerStates.get(targetPlayerId);
        if (target == null || target.pose() == null || target.health() <= 0.0f) {
            return false;
        }
        return isPlayerAttackInReach(client, target);
    }

    private boolean isValidClientItemUseAction(ClientConnection client, ProtocolMessage.ClientAction action) {
        if (client == null || action == null || !isValidActionDataText(action.data())) {
            return false;
        }
        if (!playerStates.containsKey(client.playerId())) {
            return false;
        }
        String useAction = action.data().getOrDefault("useAction", "");
        if (!isKnownClientItemUseAction(useAction)) {
            return false;
        }
        if (!isValidClientItemUseActionKeys(useAction, action.data())) {
            return false;
        }
        if (!isFiniteText(action.data().getOrDefault("dirX", "0"))
                || !isFiniteText(action.data().getOrDefault("dirY", "0"))
                || !isFiniteText(action.data().getOrDefault("dirZ", "0"))
                || !isFiniteText(action.data().getOrDefault("power", "0"))
                || !isFiniteText(action.data().getOrDefault("velocityY", "0"))
                || !isFiniteText(action.data().getOrDefault("motionX", "0"))
                || !isFiniteText(action.data().getOrDefault("motionY", "0"))
                || !isFiniteText(action.data().getOrDefault("motionZ", "0"))) {
            return false;
        }
        if ("drop_stack".equals(useAction)) {
            return isValidActionPresentItemStack(action.data(), "stack")
                    && isClampedDropFloat(action.data().getOrDefault("power", "4"), 0.0f, 8.0f)
                    && isClampedDropFloat(action.data().getOrDefault("velocityY", "2"), -4.0f, 8.0f);
        }
        if ("death_drop_stack".equals(useAction)) {
            return isDeadClientPlayerState(client)
                    && isValidActionPresentItemStack(action.data(), "stack")
                    && isValidDeathDropSourceSlot(action.data().getOrDefault("sourceSlot", "-1"))
                    && isClampedDropFloat(action.data().getOrDefault("motionX", "0"), -8.0f, 8.0f)
                    && isClampedDropFloat(action.data().getOrDefault("motionY", "0"), -8.0f, 8.0f)
                    && isClampedDropFloat(action.data().getOrDefault("motionZ", "0"), -8.0f, 8.0f)
                    && isValidDeathDropPickupDelay(action.data().getOrDefault("pickupDelay", "0"));
        }
        if ("death_drop_xp".equals(useAction)) {
            return isDeadClientPlayerState(client)
                    && isValidDeathExperienceAmount(action.data().getOrDefault("amount", "0"));
        }
        if ("place_minecart".equals(useAction)) {
            try {
                int x = Integer.parseInt(action.data().getOrDefault("blockX", ""));
                int y = Integer.parseInt(action.data().getOrDefault("blockY", ""));
                int z = Integer.parseInt(action.data().getOrDefault("blockZ", ""));
                return isBlockEditInReach(client, new ProtocolMessage.BlockUpdate(x, y, z, "66", 0, ""));
            } catch (NumberFormatException ignored) {
                return false;
            }
        }
        if ("place_painting".equals(useAction)) {
            try {
                int x = Integer.parseInt(action.data().getOrDefault("blockX", ""));
                int y = Integer.parseInt(action.data().getOrDefault("blockY", ""));
                int z = Integer.parseInt(action.data().getOrDefault("blockZ", ""));
                int face = Integer.parseInt(action.data().getOrDefault("blockFace", ""));
                return y >= 0 && y < MultiplayerProtocol.WORLD_HEIGHT
                        && face >= 2 && face <= 5
                        && isBlockEditInReach(client, new ProtocolMessage.BlockUpdate(x, y, z, "0", 0, ""));
            } catch (NumberFormatException ignored) {
                return false;
            }
        }
        if ("play_note_block".equals(useAction)
                || "tune_note_block".equals(useAction)
                || "insert_record".equals(useAction)
                || "eject_record".equals(useAction)) {
            try {
                int x = Integer.parseInt(action.data().getOrDefault("blockX", ""));
                int y = Integer.parseInt(action.data().getOrDefault("blockY", ""));
                int z = Integer.parseInt(action.data().getOrDefault("blockZ", ""));
                return y >= 0 && y < MultiplayerProtocol.WORLD_HEIGHT
                        && isBlockEditInReach(client, new ProtocolMessage.BlockUpdate(x, y, z, "0", 0, ""));
            } catch (NumberFormatException ignored) {
                return false;
            }
        }
        return true;
    }

    private static boolean isValidClientItemUseActionKeys(String useAction, Map<String, String> data) {
        if (data == null) {
            return false;
        }
        return switch (useAction) {
            case "bow", "throw_item", "ender_pearl", "eye_of_ender", "splash_potion",
                    "consume_food", "drink_milk", "drink_potion", "use_map", "equip_armor",
                    "place_boat", "fishing_cast", "fishing_reel", "drop_item" ->
                    hasOnlyActionKeys(data, new String[] {"useAction", "itemId", "dirX", "dirY", "dirZ", "power"});
            case "place_minecart" -> hasOnlyActionKeys(data,
                    new String[] {"useAction", "itemId", "dirX", "dirY", "dirZ", "power",
                            "blockX", "blockY", "blockZ"});
            case "place_painting" -> hasOnlyActionKeys(data,
                    new String[] {"useAction", "itemId", "dirX", "dirY", "dirZ", "power",
                            "blockX", "blockY", "blockZ", "blockFace"});
            case "play_note_block", "tune_note_block", "insert_record", "eject_record" ->
                    hasOnlyActionKeys(data, new String[] {"useAction", "itemId", "dirX", "dirY", "dirZ",
                            "power", "blockX", "blockY", "blockZ"});
            case "drop_stack" -> hasOnlyActionKeysAndItemStacks(data,
                    new String[] {"useAction", "dirX", "dirY", "dirZ", "power", "velocityY"}, "stack");
            case "death_drop_stack" -> hasOnlyActionKeysAndItemStacks(data,
                    new String[] {"useAction", "sourceSlot", "motionX", "motionY", "motionZ", "pickupDelay"},
                    "stack");
            case "death_drop_xp" -> hasOnlyActionKeys(data, new String[] {"useAction", "amount", "power"});
            default -> false;
        };
    }

    private static boolean hasOnlyActionKeys(Map<String, String> data, String[] exactKeys, String... allowedPrefixes) {
        if (data == null) {
            return false;
        }
        for (String key : data.keySet()) {
            if (isExactActionKey(key, exactKeys) || hasAllowedActionPrefix(key, allowedPrefixes)) {
                continue;
            }
            return false;
        }
        return true;
    }

    private static boolean isExactActionKey(String key, String[] exactKeys) {
        if (key == null || exactKeys == null) {
            return false;
        }
        for (String exact : exactKeys) {
            if (key.equals(exact)) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasAllowedActionPrefix(String key, String[] allowedPrefixes) {
        if (key == null || allowedPrefixes == null) {
            return false;
        }
        for (String prefix : allowedPrefixes) {
            if (prefix != null && key.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasOnlyActionKeysAndItemStacks(
            Map<String, String> data, String[] exactKeys, String... itemStackPrefixes) {
        if (data == null) {
            return false;
        }
        for (String key : data.keySet()) {
            if (isExactActionKey(key, exactKeys) || isActionItemStackKey(data, key, itemStackPrefixes)) {
                continue;
            }
            return false;
        }
        return true;
    }

    private static boolean isActionItemStackKey(
            Map<String, String> data, String key, String[] itemStackPrefixes) {
        if (key == null || itemStackPrefixes == null) {
            return false;
        }
        for (String prefix : itemStackPrefixes) {
            if (isAllowedActionItemStackKey(data, prefix, key)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isAllowedActionItemStackKey(Map<String, String> data, String prefix, String key) {
        if (prefix == null || prefix.isBlank() || key == null) {
            return false;
        }
        String fullPrefix = prefix + ".";
        if (!key.startsWith(fullPrefix)) {
            return false;
        }
        String suffix = key.substring(fullPrefix.length());
        return isExactActionKey(suffix, new String[] {
                "itemId", "count", "damage", "customName",
                "potionType", "potionSplash", "potionExtended", "potionEnhanced",
                "enchantmentCount", "metadataCount"
        })
                || isIndexedActionStackKey(data, suffix, "enchantment",
                        prefix + ".enchantmentCount", new String[] {"type", "level"})
                || isIndexedActionStackKey(data, suffix, "metadata",
                        prefix + ".metadataCount", new String[] {"key", "value"});
    }

    private static boolean isIndexedActionStackKey(Map<String, String> data, String suffix,
            String group, String countKey, String[] fields) {
        if (data == null || suffix == null || group == null || countKey == null) {
            return false;
        }
        String groupPrefix = group + ".";
        if (!suffix.startsWith(groupPrefix)) {
            return false;
        }
        String rest = suffix.substring(groupPrefix.length());
        int separator = rest.indexOf('.');
        if (separator <= 0 || separator >= rest.length() - 1) {
            return false;
        }
        if (!isExactActionKey(rest.substring(separator + 1), fields)) {
            return false;
        }
        int declaredCount = actionCountOrInvalid(data, countKey, 0,
                MultiplayerProtocol.MAX_ITEM_STACK_DATA_ENTRIES);
        if (declaredCount < 0) {
            return false;
        }
        try {
            int index = Integer.parseInt(rest.substring(0, separator));
            return index >= 0 && index < declaredCount;
        } catch (NumberFormatException ignored) {
            return false;
        }
    }

    private static boolean hasOnlyActionItemStackKeys(Map<String, String> data, String prefix) {
        if (data == null || prefix == null || prefix.isBlank()) {
            return false;
        }
        for (String key : data.keySet()) {
            if (key != null && key.startsWith(prefix + ".")
                    && !isAllowedActionItemStackKey(data, prefix, key)) {
                return false;
            }
        }
        return true;
    }

    private static boolean hasValidActionItemStackExtraValues(
            Map<String, String> data, String prefix, String itemId, int count) {
        if (data == null || prefix == null || prefix.isBlank()) {
            return false;
        }
        if (isEmptyActionItemStack(itemId, count)) {
            return !hasNonBaseActionItemStackKey(data, prefix);
        }
        if (!hasValidActionPotionFields(data, prefix)) {
            return false;
        }
        int enchantmentCount = actionCountOrInvalid(data, prefix + ".enchantmentCount", 0,
                MultiplayerProtocol.MAX_ITEM_STACK_DATA_ENTRIES);
        int metadataCount = actionCountOrInvalid(data, prefix + ".metadataCount", 0,
                MultiplayerProtocol.MAX_ITEM_STACK_DATA_ENTRIES);
        if (enchantmentCount < 0 || metadataCount < 0) {
            return false;
        }
        for (int i = 0; i < enchantmentCount; i++) {
            String type = data.get(prefix + ".enchantment." + i + ".type");
            String level = data.get(prefix + ".enchantment." + i + ".level");
            if (!MultiplayerProtocol.isValidNetworkId(type)
                    || !isClampedActionIntegerText(level, 1, Short.MAX_VALUE)) {
                return false;
            }
        }
        for (int i = 0; i < metadataCount; i++) {
            String key = data.get(prefix + ".metadata." + i + ".key");
            String value = data.get(prefix + ".metadata." + i + ".value");
            if (key == null || key.isBlank()
                    || key.length() > MultiplayerProtocol.MAX_ITEM_STACK_DATA_KEY_LENGTH
                    || value == null
                    || value.length() > MultiplayerProtocol.MAX_ITEM_STACK_DATA_VALUE_LENGTH) {
                return false;
            }
        }
        return true;
    }

    private static boolean hasValidActionPotionFields(Map<String, String> data, String prefix) {
        if (!data.containsKey(prefix + ".potionType")) {
            return !(data.containsKey(prefix + ".potionSplash")
                    || data.containsKey(prefix + ".potionExtended")
                    || data.containsKey(prefix + ".potionEnhanced"));
        }
        return MultiplayerProtocol.isValidNetworkId(data.get(prefix + ".potionType"))
                && isBooleanActionText(data.getOrDefault(prefix + ".potionSplash", "false"))
                && isBooleanActionText(data.getOrDefault(prefix + ".potionExtended", "false"))
                && isBooleanActionText(data.getOrDefault(prefix + ".potionEnhanced", "false"));
    }

    private static boolean hasNonBaseActionItemStackKey(Map<String, String> data, String prefix) {
        String fullPrefix = prefix + ".";
        for (String key : data.keySet()) {
            if (key == null || !key.startsWith(fullPrefix)) {
                continue;
            }
            String suffix = key.substring(fullPrefix.length());
            if (!isExactActionKey(suffix, new String[] {"itemId", "count", "damage"})) {
                return true;
            }
        }
        return false;
    }

    private static boolean isEmptyActionItemStack(String itemId, int count) {
        return count <= 0
                || itemId == null
                || "0".equals(itemId)
                || "air".equalsIgnoreCase(itemId)
                || "minecraft:air".equalsIgnoreCase(itemId);
    }

    private static int actionCountOrInvalid(Map<String, String> data, String key, int fallback, int max) {
        if (data == null || key == null) {
            return -1;
        }
        if (!data.containsKey(key)) {
            return fallback;
        }
        try {
            int value = Integer.parseInt(data.get(key));
            return value >= 0 && value <= max ? value : -1;
        } catch (RuntimeException ignored) {
            return -1;
        }
    }

    private static boolean isBooleanActionText(String value) {
        return "true".equalsIgnoreCase(value) || "false".equalsIgnoreCase(value);
    }

    private static boolean isClampedActionIntegerText(String value, int min, int max) {
        try {
            int parsed = Integer.parseInt(value);
            return parsed >= min && parsed <= max;
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    private static String[] indexedActionStackPrefixes(String basePrefix, int slots) {
        if (basePrefix == null || basePrefix.isBlank() || slots <= 0) {
            return new String[0];
        }
        String[] prefixes = new String[slots];
        for (int slot = 0; slot < slots; slot++) {
            prefixes[slot] = basePrefix + "." + slot;
        }
        return prefixes;
    }

    private static String[] craftActionStackPrefixes(int slots) {
        String[] prefixes = new String[Math.max(0, slots) + 1];
        for (int slot = 0; slot < slots; slot++) {
            prefixes[slot] = "craft.grid." + slot;
        }
        prefixes[prefixes.length - 1] = "cursor";
        return prefixes;
    }

    private static String[] signUpdateActionKeys() {
        String[] keys = new String[3 + MultiplayerProtocol.SIGN_LINE_COUNT];
        keys[0] = "x";
        keys[1] = "y";
        keys[2] = "z";
        for (int i = 0; i < MultiplayerProtocol.SIGN_LINE_COUNT; i++) {
            keys[3 + i] = "signLine" + i;
        }
        return keys;
    }

    private static boolean isKnownClientItemUseAction(String useAction) {
        return switch (useAction) {
            case "bow", "throw_item", "ender_pearl", "eye_of_ender", "splash_potion", "play_note_block",
                    "consume_food", "drink_milk", "drink_potion", "use_map", "equip_armor",
                    "tune_note_block", "insert_record", "eject_record", "place_boat", "place_painting", "place_minecart",
                    "fishing_cast", "fishing_reel", "drop_item", "drop_stack",
                    "death_drop_stack", "death_drop_xp" -> true;
            default -> false;
        };
    }

    private boolean isDeadClientPlayerState(ClientConnection client) {
        if (client == null) {
            return false;
        }
        ProtocolMessage.PlayerState state = playerStates.get(client.playerId());
        return state != null && state.health() <= 0.0f;
    }

    private boolean isValidClientContainerUpdateAction(ClientConnection client,
            ProtocolMessage.ClientAction action) {
        if (client == null || action == null || action.data() == null
                || action.data().size() > MultiplayerProtocol.MAX_CONTAINER_UPDATE_DATA_ENTRIES) {
            return false;
        }
        Map<String, String> data = action.data();
        if (!isValidActionDataText(data)) {
            return false;
        }
        String tileType = data.getOrDefault("tileType", "");
        int slots = containerSlotCount(tileType);
        if (slots <= 0) {
            return false;
        }
        String entityId = data.getOrDefault("entityId", "");
        String[] stackPrefixes = indexedActionStackPrefixes("tile.inventory", slots);
        if (entityId.isBlank()) {
            if (!hasOnlyActionKeysAndItemStacks(data,
                    new String[] {"x", "y", "z", "tileType", "tile.inventory.size"}, stackPrefixes)) {
                return false;
            }
            int x;
            int y;
            int z;
            try {
                x = Integer.parseInt(data.getOrDefault("x", ""));
                y = Integer.parseInt(data.getOrDefault("y", ""));
                z = Integer.parseInt(data.getOrDefault("z", ""));
            } catch (NumberFormatException ignored) {
                return false;
            }
            if (!isBlockEditInReach(client, new ProtocolMessage.BlockUpdate(x, y, z, "54", 0, ""))) {
                return false;
            }
        } else {
            if (!hasOnlyActionKeysAndItemStacks(data,
                    new String[] {"entityId", "tileType", "tile.inventory.size"}, stackPrefixes)) {
                return false;
            }
            if (!"chest_minecart".equals(tileType) || !MultiplayerProtocol.isValidNetworkId(entityId)) {
                return false;
            }
            ProtocolMessage.EntityUpdate target = entityStates.get(entityId);
            if (target == null || target.pose() == null || !isChestMinecartEntityType(target.entityType())
                    || "true".equalsIgnoreCase(target.data().getOrDefault("removed", "false"))) {
                return false;
            }
            if (!isEntityActionInReach(client, target)) {
                return false;
            }
        }
        int declaredSlots;
        try {
            declaredSlots = Integer.parseInt(data.getOrDefault("tile.inventory.size", "-1"));
        } catch (NumberFormatException ignored) {
            return false;
        }
        if (declaredSlots != slots) {
            return false;
        }
        for (int slot = 0; slot < slots; slot++) {
            String prefix = "tile.inventory." + slot;
            String itemId = data.getOrDefault(prefix + ".itemId", "air");
            int count;
            int damage;
            try {
                count = Integer.parseInt(data.getOrDefault(prefix + ".count", "0"));
                damage = Integer.parseInt(data.getOrDefault(prefix + ".damage", "-1"));
            } catch (NumberFormatException ignored) {
                return false;
            }
            if (!MultiplayerProtocol.isValidInventoryUpdate(itemId, 0, count, damage)) {
                return false;
            }
            if (!isValidActionItemStack(data, prefix)) {
                return false;
            }
        }
        return true;
    }

    private static int containerSlotCount(String tileType) {
        return switch (tileType) {
            case "chest" -> 27;
            case "chest_minecart" -> 27;
            case "furnace" -> 3;
            case "dispenser" -> 9;
            case "brewing_stand" -> 4;
            default -> -1;
        };
    }

    private static boolean isChestMinecartEntityType(String entityType) {
        String normalized = entityType == null ? "" : entityType.trim()
                .replace('-', '_')
                .replace(' ', '_')
                .toUpperCase(java.util.Locale.ROOT);
        return "CHEST_MINECART".equals(normalized) || "CHESTMINECARTENTITY".equals(normalized);
    }

    private boolean isValidClientEnchantItemAction(ClientConnection client,
            ProtocolMessage.ClientAction action) {
        if (client == null || action == null || action.data() == null
                || action.data().size() > MultiplayerProtocol.MAX_ITEM_STACK_DATA_ENTRIES) {
            return false;
        }
        Map<String, String> data = action.data();
        if (!isValidActionDataText(data) || data.containsKey("accepted")) {
            return false;
        }
        if (!hasOnlyActionKeysAndItemStacks(data,
                new String[] {"x", "y", "z", "offerSlot", "offerCost", "offerSeed"}, "table.item")) {
            return false;
        }
        int x;
        int y;
        int z;
        int offerSlot;
        int offerCost;
        try {
            x = Integer.parseInt(data.getOrDefault("x", ""));
            y = Integer.parseInt(data.getOrDefault("y", ""));
            z = Integer.parseInt(data.getOrDefault("z", ""));
            offerSlot = Integer.parseInt(data.getOrDefault("offerSlot", "-1"));
            offerCost = Integer.parseInt(data.getOrDefault("offerCost", "0"));
            Long.parseLong(data.getOrDefault("offerSeed", ""));
        } catch (NumberFormatException ignored) {
            return false;
        }
        if (offerSlot < 0 || offerSlot > 2 || offerCost <= 0 || offerCost > 50) {
            return false;
        }
        if (!isBlockEditInReach(client, new ProtocolMessage.BlockUpdate(x, y, z, "116", 0, ""))) {
            return false;
        }
        return isValidActionItemStack(data, "table.item");
    }

    private boolean isValidClientCraftItemAction(ClientConnection client,
            ProtocolMessage.ClientAction action) {
        if (client == null || action == null || action.data() == null
                || action.data().size() > MultiplayerProtocol.MAX_CONTAINER_UPDATE_DATA_ENTRIES) {
            return false;
        }
        Map<String, String> data = action.data();
        if (!isValidActionDataText(data) || data.containsKey("accepted")) {
            return false;
        }
        int gridSize;
        int crafts;
        try {
            gridSize = Integer.parseInt(data.getOrDefault("gridSize", "0"));
            crafts = Integer.parseInt(data.getOrDefault("crafts", "0"));
        } catch (NumberFormatException ignored) {
            return false;
        }
        if ((gridSize != 2 && gridSize != 3) || crafts <= 0 || crafts > 64) {
            return false;
        }
        String[] stackPrefixes = craftActionStackPrefixes(gridSize * gridSize);
        String[] exactKeys = gridSize == 2
                ? new String[] {"gridSize", "quickMove", "crafts", "craft.grid.size"}
                : new String[] {"gridSize", "quickMove", "crafts", "x", "y", "z", "craft.grid.size"};
        if (!hasOnlyActionKeysAndItemStacks(data, exactKeys, stackPrefixes)
                || !isBooleanActionText(data.getOrDefault("quickMove", "false"))) {
            return false;
        }
        int declaredSlots;
        try {
            declaredSlots = Integer.parseInt(data.getOrDefault("craft.grid.size", "-1"));
        } catch (NumberFormatException ignored) {
            return false;
        }
        int expectedSlots = gridSize * gridSize;
        if (declaredSlots != expectedSlots) {
            return false;
        }
        for (int slot = 0; slot < declaredSlots; slot++) {
            if (!isValidActionItemStack(data, "craft.grid." + slot)) {
                return false;
            }
        }
        if (!isValidActionItemStack(data, "cursor")) {
            return false;
        }
        if (gridSize == 2) {
            return true;
        }
        int x;
        int y;
        int z;
        try {
            x = Integer.parseInt(data.getOrDefault("x", ""));
            y = Integer.parseInt(data.getOrDefault("y", ""));
            z = Integer.parseInt(data.getOrDefault("z", ""));
        } catch (NumberFormatException ignored) {
            return false;
        }
        return isBlockEditInReach(client, new ProtocolMessage.BlockUpdate(x, y, z, "58", 0, ""));
    }

    private static boolean isValidActionItemStack(Map<String, String> data, String prefix) {
        String itemId = data.getOrDefault(prefix + ".itemId", "air");
        int count;
        int damage;
        try {
            count = Integer.parseInt(data.getOrDefault(prefix + ".count", "0"));
            damage = Integer.parseInt(data.getOrDefault(prefix + ".damage", "-1"));
        } catch (NumberFormatException ignored) {
            return false;
        }
        return MultiplayerProtocol.isValidInventoryUpdate(itemId, 0, count, damage)
                && hasOnlyActionItemStackKeys(data, prefix)
                && hasValidActionItemStackExtraValues(data, prefix, itemId, count);
    }

    private static boolean isValidActionPresentItemStack(Map<String, String> data, String prefix) {
        if (!isValidActionItemStack(data, prefix)) {
            return false;
        }
        String itemId = data.getOrDefault(prefix + ".itemId", "air");
        int count;
        try {
            count = Integer.parseInt(data.getOrDefault(prefix + ".count", "0"));
        } catch (NumberFormatException ignored) {
            return false;
        }
        return count > 0
                && !"0".equals(itemId)
                && !"air".equalsIgnoreCase(itemId)
                && !"minecraft:air".equalsIgnoreCase(itemId);
    }

    private static boolean isValidClientInventorySyncAction(ProtocolMessage.ClientAction action) {
        if (action == null || action.data() == null
                || action.data().size() > MultiplayerProtocol.MAX_ITEM_STACK_DATA_ENTRIES + 4
                || !isValidActionDataText(action.data())) {
            return false;
        }
        Map<String, String> data = action.data();
        if (!hasOnlyActionKeysAndItemStacks(data,
                new String[] {"slot", "itemId", "count", "damage"}, "stack")) {
            return false;
        }
        int slot;
        int count;
        int damage;
        try {
            slot = Integer.parseInt(data.getOrDefault("slot", "-1"));
            count = Integer.parseInt(data.getOrDefault("count", "-1"));
            damage = Integer.parseInt(data.getOrDefault("damage",
                    Integer.toString(MultiplayerProtocol.MIN_ITEM_DAMAGE)));
        } catch (NumberFormatException ignored) {
            return false;
        }
        String itemId = data.getOrDefault("itemId", "air");
        if (!MultiplayerProtocol.isValidInventoryUpdate(itemId, slot, count, damage)) {
            return false;
        }
        return MultiplayerProtocol.isValidItemStackData(inventorySyncStackData(data))
                && hasValidActionItemStackExtraValues(data, "stack", itemId, count);
    }

    private static Map<String, String> inventorySyncStackData(Map<String, String> data) {
        LinkedHashMap<String, String> stackData = new LinkedHashMap<>();
        if (data == null) {
            return stackData;
        }
        for (Map.Entry<String, String> entry : data.entrySet()) {
            String key = entry.getKey();
            if (key != null && key.startsWith("stack.")) {
                stackData.put(key, entry.getValue());
            }
        }
        return stackData;
    }

    private static boolean isClampedDropFloat(String value, float min, float max) {
        try {
            float parsed = Float.parseFloat(value);
            return isFinite(parsed) && parsed >= min && parsed <= max;
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    private static boolean isValidDeathDropPickupDelay(String value) {
        try {
            int parsed = Integer.parseInt(value);
            return parsed >= 0 && parsed <= 200;
        } catch (NumberFormatException ignored) {
            return false;
        }
    }

    private static boolean isValidDeathDropSourceSlot(String value) {
        try {
            int parsed = Integer.parseInt(value);
            return MultiplayerProtocol.isValidInventorySlot(parsed);
        } catch (NumberFormatException ignored) {
            return false;
        }
    }

    private static boolean isValidDeathExperienceAmount(String value) {
        try {
            int parsed = Integer.parseInt(value);
            return parsed > 0 && parsed <= 100_000;
        } catch (NumberFormatException ignored) {
            return false;
        }
    }

    private static boolean isValidActionDataText(Map<String, String> data) {
        if (data == null) {
            return true;
        }
        for (Map.Entry<String, String> entry : data.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            if (key == null || key.isBlank()
                    || key.length() > MultiplayerProtocol.MAX_ITEM_STACK_DATA_KEY_LENGTH
                    || value == null
                    || value.length() > MultiplayerProtocol.MAX_ITEM_STACK_DATA_VALUE_LENGTH) {
                return false;
            }
        }
        return true;
    }

    private static boolean isFiniteText(String value) {
        try {
            return isFinite(Float.parseFloat(value));
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    private static ProtocolMessage.BlockUpdate sanitizeClientBlockUpdate(
            ClientConnection client, ProtocolMessage.BlockUpdate update) {
        if (update == null) {
            return null;
        }
        return new ProtocolMessage.BlockUpdate(
                update.x(),
                update.y(),
                update.z(),
                update.blockId(),
                update.metadata(),
                client == null ? "" : client.playerId(),
                Map.of());
    }

    private boolean isClientSignUpdateAction(ClientConnection client, ProtocolMessage.ClientAction action) {
        if (client == null || action == null || action.blockUpdate() != null
                || !MultiplayerProtocol.ACTION_SIGN_UPDATE.equals(action.action())) {
            return false;
        }
        Map<String, String> data = action.data();
        if (!isValidActionDataText(data)
                || !hasOnlyActionKeys(data, signUpdateActionKeys())) {
            return false;
        }
        int x;
        int y;
        int z;
        try {
            x = Integer.parseInt(data.getOrDefault("x", ""));
            y = Integer.parseInt(data.getOrDefault("y", ""));
            z = Integer.parseInt(data.getOrDefault("z", ""));
        } catch (NumberFormatException ignored) {
            return false;
        }
        if (!isBlockEditInReach(client, new ProtocolMessage.BlockUpdate(x, y, z, "63", 0, ""))) {
            return false;
        }
        for (int i = 0; i < MultiplayerProtocol.SIGN_LINE_COUNT; i++) {
            String line = data.getOrDefault("signLine" + i, "");
            if (line.length() > MultiplayerProtocol.MAX_SIGN_LINE_LENGTH) {
                return false;
            }
        }
        return true;
    }

    private boolean isBlockEditInReach(ClientConnection client, ProtocolMessage.BlockUpdate update) {
        if (client == null || update == null) {
            return false;
        }
        if (update.y() < MultiplayerProtocol.WORLD_MIN_Y || update.y() >= maxBuildHeight) {
            return false;
        }
        ProtocolMessage.PlayerState state = playerStates.get(client.playerId());
        if (state == null || state.pose() == null) {
            return false;
        }
        ProtocolMessage.PlayerPose pose = state.pose();
        double dx = update.x() + 0.5d - pose.x();
        double dy = update.y() + 0.5d - (pose.y() + MultiplayerProtocol.PLAYER_EYE_HEIGHT);
        double dz = update.z() + 0.5d - pose.z();
        double distanceSq = dx * dx + dy * dy + dz * dz;
        return distanceSq <= MultiplayerProtocol.MAX_CLIENT_BLOCK_EDIT_DISTANCE_SQ;
    }

    private boolean isEntityActionInReach(ClientConnection client, ProtocolMessage.EntityUpdate target) {
        if (client == null || target == null || target.pose() == null) {
            return false;
        }
        ProtocolMessage.PlayerState state = playerStates.get(client.playerId());
        if (state == null || state.pose() == null) {
            return false;
        }
        ProtocolMessage.PlayerPose pose = state.pose();
        double dx = target.pose().x() - pose.x();
        double dy = target.pose().y() - (pose.y() + MultiplayerProtocol.PLAYER_EYE_HEIGHT);
        double dz = target.pose().z() - pose.z();
        double distanceSq = dx * dx + dy * dy + dz * dz;
        return distanceSq <= MultiplayerProtocol.MAX_CLIENT_ENTITY_ACTION_DISTANCE_SQ;
    }

    private boolean isPlayerAttackInReach(ClientConnection client, ProtocolMessage.PlayerState target) {
        if (client == null || target == null || target.pose() == null) {
            return false;
        }
        ProtocolMessage.PlayerState state = playerStates.get(client.playerId());
        if (state == null || state.pose() == null || state.health() <= 0.0f) {
            return false;
        }
        ProtocolMessage.PlayerPose pose = state.pose();
        ProtocolMessage.PlayerPose targetPose = target.pose();
        double dx = targetPose.x() - pose.x();
        double dy = targetPose.y() + MultiplayerProtocol.PLAYER_EYE_HEIGHT
                - (pose.y() + MultiplayerProtocol.PLAYER_EYE_HEIGHT);
        double dz = targetPose.z() - pose.z();
        double distanceSq = dx * dx + dy * dy + dz * dz;
        return distanceSq <= MultiplayerProtocol.MAX_CLIENT_ENTITY_ACTION_DISTANCE_SQ;
    }

    private static boolean isValidInventoryUpdate(ProtocolMessage.InventoryUpdate update) {
        return update != null
                && MultiplayerProtocol.isValidNetworkId(update.playerId())
                && MultiplayerProtocol.isValidInventoryUpdate(update.itemId(), update.slot(),
                        update.count(), update.damage())
                && MultiplayerProtocol.isValidItemStackData(update.data());
    }

    private static boolean isValidClientInput(ProtocolMessage.ClientInput input) {
        return input != null
                && isValidPlayerPose(input.pose())
                && isValidPlayerHealth(input.health())
                && MultiplayerProtocol.isValidNetworkId(input.heldItemId())
                && input.heldItemCount() >= 0
                && input.heldItemCount() <= MultiplayerProtocol.MAX_STACK_COUNT
                && input.heldItemDamage() >= MultiplayerProtocol.MIN_ITEM_DAMAGE
                && input.heldItemDamage() <= MultiplayerProtocol.MAX_ITEM_DAMAGE
                && input.selectedSlot() >= 0
                && input.selectedSlot() < MultiplayerProtocol.INVENTORY_HOTBAR_SLOTS
                && MultiplayerProtocol.isValidNetworkId(input.gameMode())
                && MultiplayerProtocol.isValidClientPlayerStateData(input.data());
    }

    private static boolean isValidChat(ProtocolMessage.Chat chat) {
        return chat != null
                && isValidOptionalNetworkId(chat.playerId())
                && MultiplayerProtocol.isValidLegacyDataValue(chat.sender())
                && chat.text().length() <= MultiplayerProtocol.MAX_CHAT_TEXT_LENGTH;
    }

    private static boolean isValidPlayerPose(ProtocolMessage.PlayerPose pose) {
        return pose != null
                && isFinite(pose.x())
                && isFinite(pose.y())
                && isFinite(pose.z())
                && isFinite(pose.yaw())
                && isFinite(pose.pitch())
                && Math.abs(pose.x()) <= MultiplayerProtocol.MAX_WORLD_COORDINATE
                && pose.y() >= MultiplayerProtocol.MIN_PROTOCOL_PLAYER_Y
                && pose.y() <= MultiplayerProtocol.MAX_PROTOCOL_PLAYER_Y
                && Math.abs(pose.z()) <= MultiplayerProtocol.MAX_WORLD_COORDINATE;
    }

    private static boolean isValidPlayerHealth(float health) {
        return isFinite(health) && health >= 0.0f && health <= MultiplayerProtocol.MAX_PLAYER_HEALTH;
    }

    private static boolean isFinite(double value) {
        return !Double.isNaN(value) && !Double.isInfinite(value);
    }

    private static boolean isFinite(float value) {
        return !Float.isNaN(value) && !Float.isInfinite(value);
    }

    private void rememberState(ProtocolMessage message) {
        message = withCurrentDimension(message);
        if (!isValidStateMessage(message)) {
            return;
        }
        if (message instanceof ProtocolMessage.PlayerState playerState) {
            playerStates.put(playerState.playerId(), playerState);
        } else if (message instanceof ProtocolMessage.BlockUpdate blockUpdate) {
            blockStates.put(blockUpdateKey(blockUpdate), blockUpdate);
        } else if (message instanceof ProtocolMessage.EntityUpdate entityUpdate) {
            if ("true".equalsIgnoreCase(entityUpdate.data().getOrDefault("removed", "false"))
                    || "removed".equalsIgnoreCase(entityUpdate.entityType())) {
                entityStates.remove(entityUpdate.entityId());
            } else {
                entityStates.put(entityUpdate.entityId(), entityUpdate);
            }
        } else if (message instanceof ProtocolMessage.InventoryUpdate inventoryUpdate) {
            inventoryStates.put(inventoryUpdateKey(inventoryUpdate), inventoryUpdate);
        }
    }

    private void removeInventoryState(String playerId) {
        if (playerId == null || playerId.isBlank()) {
            return;
        }
        String prefix = playerId + '\u0000';
        inventoryStates.keySet().removeIf(key -> key.startsWith(prefix));
    }

    private static String blockUpdateKey(ProtocolMessage.BlockUpdate update) {
        return update.x() + "," + update.y() + "," + update.z();
    }

    private static String inventoryUpdateKey(ProtocolMessage.InventoryUpdate update) {
        return inventoryUpdateKey(update.playerId(), update.slot());
    }

    private static String inventoryUpdateKey(String playerId, int slot) {
        return playerId + '\u0000' + slot;
    }

    private ProtocolMessage withCurrentDimension(ProtocolMessage message) {
        if (message instanceof ProtocolMessage.PlayerState playerState) {
            return withCurrentDimension(playerState);
        }
        if (message instanceof ProtocolMessage.BlockUpdate blockUpdate) {
            return withCurrentDimension(blockUpdate);
        }
        if (message instanceof ProtocolMessage.EntityUpdate entityUpdate) {
            return withCurrentDimension(entityUpdate);
        }
        if (message instanceof ProtocolMessage.InventoryUpdate inventoryUpdate) {
            return withCurrentDimension(inventoryUpdate);
        }
        if (message instanceof ProtocolMessage.ClientAction action) {
            ProtocolMessage.BlockUpdate blockUpdate = action.blockUpdate() == null
                    ? null
                    : withCurrentDimension(action.blockUpdate());
            return new ProtocolMessage.ClientAction(
                    action.playerId(),
                    action.action(),
                    blockUpdate,
                    dataWithCurrentDimension(action.data()));
        }
        if (message instanceof ProtocolMessage.WorldEvent event) {
            return new ProtocolMessage.WorldEvent(event.eventType(), dataWithCurrentDimension(event.data()));
        }
        return message;
    }

    private ProtocolMessage.PlayerState withCurrentDimension(ProtocolMessage.PlayerState state) {
        return new ProtocolMessage.PlayerState(
                state.playerId(),
                state.username(),
                state.pose(),
                state.onGround(),
                state.sneaking(),
                state.health(),
                state.heldItemId(),
                state.heldItemCount(),
                state.heldItemDamage(),
                state.selectedSlot(),
                state.gameMode(),
                dataWithCurrentDimension(state.data()));
    }

    private ProtocolMessage.BlockUpdate withCurrentDimension(ProtocolMessage.BlockUpdate update) {
        return new ProtocolMessage.BlockUpdate(
                update.x(),
                update.y(),
                update.z(),
                update.blockId(),
                update.metadata(),
                update.sourcePlayerId(),
                dataWithCurrentDimension(update.data()));
    }

    private ProtocolMessage.EntityUpdate withCurrentDimension(ProtocolMessage.EntityUpdate update) {
        return new ProtocolMessage.EntityUpdate(
                update.entityId(),
                update.entityType(),
                update.pose(),
                dataWithCurrentDimension(update.data()));
    }

    private ProtocolMessage.InventoryUpdate withCurrentDimension(ProtocolMessage.InventoryUpdate update) {
        return new ProtocolMessage.InventoryUpdate(
                update.playerId(),
                update.slot(),
                update.itemId(),
                update.count(),
                update.damage(),
                dataWithCurrentDimension(update.data()));
    }

    private Map<String, String> dataWithCurrentDimension(Map<String, String> data) {
        String current = currentDimension();
        if (data != null && current.equals(data.get(DIMENSION_DATA_KEY))) {
            return data;
        }
        LinkedHashMap<String, String> copy = new LinkedHashMap<>();
        if (data != null) {
            copy.putAll(data);
        }
        copy.put(DIMENSION_DATA_KEY, current);
        return copy;
    }

    private String currentDimension() {
        return normalizeText(dimension, "overworld");
    }

    @FunctionalInterface
    public interface ServerMessageListener {
        void onMessage(String playerId, ProtocolMessage message);
    }

    @FunctionalInterface
    public interface JoinAdmission {
        String rejectReason(String username, String remoteAddress);
    }

    private record QueryChallenge(int token, long issuedAtMillis) {
        private boolean isExpired(long nowMillis) {
            return nowMillis - issuedAtMillis > QUERY_CHALLENGE_TTL_MILLIS;
        }
    }

    private final class ClientConnection {
        private final String playerId;
        private final Socket socket;
        private final BufferedReader reader;
        private final BufferedWriter writer;
        private final AtomicBoolean open = new AtomicBoolean(true);
        private final long connectedAtMillis = System.currentTimeMillis();
        private volatile String username = "Player";
        private volatile boolean joined;
        private volatile int pendingKeepAliveId;
        private volatile long lastKeepAliveSentMillis;
        private volatile int latencyMillis = -1;
        private volatile ProtocolMessage.PlayerPose lastPose;
        private volatile long floatingStartMillis;

        private ClientConnection(String playerId, Socket socket, PushbackInputStream input) throws IOException {
            this.playerId = playerId;
            this.socket = socket;
            this.reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8));
            this.writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
        }

        private String playerId() {
            return playerId;
        }

        private String username() {
            return username;
        }

        private void setUsername(String username) {
            if (username != null && !username.isBlank()) {
                this.username = username;
            }
        }

        private String remoteAddress() {
            return socket.getInetAddress() == null ? "" : socket.getInetAddress().getHostAddress();
        }

        private boolean isJoined() {
            return joined;
        }

        private void markJoined() {
            joined = true;
        }

        private void start() {
            Thread thread = new Thread(() -> readClient(this), "CraftZero-Protocol-Client-" + playerId);
            thread.setDaemon(true);
            thread.start();
        }

        private String readLine() throws IOException {
            return ProtocolLine.read(reader);
        }

        private boolean isOpen() {
            return open.get();
        }

        private boolean hasPendingKeepAlive() {
            return pendingKeepAliveId != 0;
        }

        private boolean joinTimedOut(long nowMillis) {
            return !joined && nowMillis - connectedAtMillis > MultiplayerProtocol.JOIN_TIMEOUT_MILLIS;
        }

        private boolean keepAliveTimedOut(long nowMillis) {
            return pendingKeepAliveId != 0
                    && nowMillis - lastKeepAliveSentMillis > MultiplayerProtocol.KEEP_ALIVE_TIMEOUT_MILLIS;
        }

        private int latencyMillis() {
            return latencyMillis;
        }

        private ProtocolMessage.PlayerPose lastPose() {
            return lastPose;
        }

        private void setLastPose(ProtocolMessage.PlayerPose pose) {
            lastPose = pose;
        }

        private void resetFloatingCheck(ProtocolMessage.PlayerPose pose) {
            lastPose = pose;
            floatingStartMillis = 0L;
        }

        private boolean hasFloatingStart() {
            return floatingStartMillis > 0L;
        }

        private long floatingStartMillis() {
            return floatingStartMillis;
        }

        private void setFloatingStart(long nowMillis) {
            floatingStartMillis = Math.max(1L, nowMillis);
        }

        private void clearFloatingStart() {
            floatingStartMillis = 0L;
        }

        private void sendKeepAlive(int id, long nowMillis) throws IOException {
            pendingKeepAliveId = id;
            lastKeepAliveSentMillis = nowMillis;
            send(new ProtocolMessage.KeepAlive(id));
        }

        private boolean acknowledgeKeepAlive(int id, long nowMillis) {
            if (id == pendingKeepAliveId) {
                latencyMillis = (int) Math.min(Integer.MAX_VALUE, Math.max(0L, nowMillis - lastKeepAliveSentMillis));
                pendingKeepAliveId = 0;
                lastKeepAliveSentMillis = nowMillis;
                return true;
            }
            return false;
        }

        private void send(ProtocolMessage message) throws IOException {
            synchronized (writer) {
                String encoded = ProtocolCodec.encode(message);
                ProtocolLine.validateEncoded(encoded);
                writer.write(encoded);
                writer.newLine();
                writer.flush();
            }
        }

        private void close() {
            if (!open.compareAndSet(true, false)) {
                return;
            }
            try {
                socket.close();
            } catch (IOException ignored) {
            }
        }
    }
}
