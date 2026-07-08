package com.craftzero.multiplayer;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public final class ProtocolClient implements AutoCloseable {
    private final String host;
    private final int port;
    private final String username;
    private final AtomicBoolean connected = new AtomicBoolean(false);
    private final BlockingQueue<ProtocolMessage> inbox = new LinkedBlockingQueue<>();
    private final CopyOnWriteArrayList<ClientMessageListener> listeners = new CopyOnWriteArrayList<>();

    private volatile Socket socket;
    private volatile BufferedReader reader;
    private volatile BufferedWriter writer;
    private volatile Thread readerThread;
    private volatile ProtocolMessage.Hello hello;
    private volatile ProtocolMessage.WorldState worldState;
    private volatile ProtocolMessage.Disconnect disconnect;

    public ProtocolClient(String host, int port, String username) {
        this.host = host == null || host.isBlank() ? "127.0.0.1" : host;
        this.port = port;
        this.username = username == null || username.isBlank() ? "Player" : username;
    }

    public static ProtocolClient connect(String host, int port, String username) throws IOException {
        ProtocolClient client = new ProtocolClient(host, port, username);
        client.connect();
        return client;
    }

    public void connect() throws IOException {
        if (!connected.compareAndSet(false, true)) {
            return;
        }

        resetSessionState();
        Socket newSocket = new Socket();
        try {
            newSocket.setTcpNoDelay(true);
            newSocket.connect(new InetSocketAddress(host, port), 5000);
            socket = newSocket;
            reader = new BufferedReader(new InputStreamReader(newSocket.getInputStream(), StandardCharsets.UTF_8));
            writer = new BufferedWriter(new OutputStreamWriter(newSocket.getOutputStream(), StandardCharsets.UTF_8));

            Thread thread = new Thread(this::readLoop, "CraftZero-Protocol-Client-Reader");
            thread.setDaemon(true);
            readerThread = thread;
            thread.start();

            send(new ProtocolMessage.Join(username, MultiplayerProtocol.VERSION));
        } catch (IOException exception) {
            connected.set(false);
            try {
                newSocket.close();
            } catch (IOException ignored) {
            }
            socket = null;
            reader = null;
            writer = null;
            throw exception;
        }
    }

    public boolean isConnected() {
        return connected.get();
    }

    public String getPlayerId() {
        ProtocolMessage.Hello currentHello = hello;
        return currentHello == null ? "" : currentHello.playerId();
    }

    public ProtocolMessage.Hello getHello() {
        return hello;
    }

    public ProtocolMessage.WorldState getWorldState() {
        return worldState;
    }

    public ProtocolMessage.Disconnect getDisconnect() {
        return disconnect;
    }

    public void addListener(ClientMessageListener listener) {
        if (listener != null) {
            listeners.add(listener);
        }
    }

    public void removeListener(ClientMessageListener listener) {
        listeners.remove(listener);
    }

    public void sendInput(ProtocolMessage.PlayerPose pose) throws IOException {
        sendInput(pose, false, false, false, false, false, false);
    }

    public void sendInput(
            ProtocolMessage.PlayerPose pose,
            boolean forward,
            boolean backward,
            boolean left,
            boolean right,
            boolean jumping,
            boolean sneaking
    ) throws IOException {
        sendInput(pose, forward, backward, left, right, jumping, sneaking, !jumping,
                20.0f, "air", 0, 0, 0, "SURVIVAL");
    }

    public void sendInput(
            ProtocolMessage.PlayerPose pose,
            boolean forward,
            boolean backward,
            boolean left,
            boolean right,
            boolean jumping,
            boolean sneaking,
            boolean onGround,
            float health,
            String heldItemId,
            int heldItemCount,
            int heldItemDamage,
            int selectedSlot,
            String gameMode
    ) throws IOException {
        sendInput(pose, forward, backward, left, right, jumping, sneaking, onGround, health,
                heldItemId, heldItemCount, heldItemDamage, selectedSlot, gameMode, Map.of());
    }

    public void sendInput(
            ProtocolMessage.PlayerPose pose,
            boolean forward,
            boolean backward,
            boolean left,
            boolean right,
            boolean jumping,
            boolean sneaking,
            boolean onGround,
            float health,
            String heldItemId,
            int heldItemCount,
            int heldItemDamage,
            int selectedSlot,
            String gameMode,
            Map<String, String> data
    ) throws IOException {
        send(new ProtocolMessage.ClientInput(
                getPlayerId(),
                pose,
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
        ));
    }

    public void sendBlockAction(ProtocolMessage.BlockUpdate update) throws IOException {
        send(ProtocolMessage.ClientAction.blockUpdate(getPlayerId(), update));
    }

    public void sendInventoryUpdate(int slot, String itemId, int count, int damage) throws IOException {
        sendInventoryUpdate(slot, itemId, count, damage, Map.of());
    }

    public void sendInventoryUpdate(int slot, String itemId, int count, int damage, Map<String, String> data)
            throws IOException {
        LinkedHashMap<String, String> actionData = new LinkedHashMap<>();
        if (data != null) {
            actionData.putAll(data);
        }
        actionData.put("slot", Integer.toString(slot));
        actionData.put("itemId", itemId == null ? "air" : itemId);
        actionData.put("count", Integer.toString(count));
        actionData.put("damage", Integer.toString(damage));
        send(new ProtocolMessage.ClientAction(getPlayerId(),
                MultiplayerProtocol.ACTION_INVENTORY_SYNC, null, actionData));
    }

    public void sendClientAction(String action, Map<String, String> data) throws IOException {
        send(new ProtocolMessage.ClientAction(getPlayerId(), action, null, data));
    }

    public void sendChat(String text) throws IOException {
        send(new ProtocolMessage.Chat(getPlayerId(), username, text));
    }

    public void send(ProtocolMessage message) throws IOException {
        if (!connected.get()) {
            throw new IOException("Client is not connected");
        }
        BufferedWriter currentWriter = writer;
        if (currentWriter == null) {
            throw new IOException("Client writer is not ready");
        }
        synchronized (currentWriter) {
            String encoded = ProtocolCodec.encode(message);
            ProtocolLine.validateEncoded(encoded);
            currentWriter.write(encoded);
            currentWriter.newLine();
            currentWriter.flush();
        }
    }

    public ProtocolMessage.Hello awaitHello(Duration timeout) throws InterruptedException {
        ProtocolMessage.Hello current = hello;
        if (current != null) {
            return current;
        }
        return waitForMessage(ProtocolMessage.Hello.class, timeout);
    }

    public ProtocolMessage.WorldState awaitWorldState(Duration timeout) throws InterruptedException {
        ProtocolMessage.WorldState current = worldState;
        if (current != null) {
            return current;
        }
        return waitForMessage(ProtocolMessage.WorldState.class, timeout);
    }

    public <T extends ProtocolMessage> T waitForMessage(Class<T> messageType, Duration timeout) throws InterruptedException {
        long remainingNanos = timeout == null ? 0L : timeout.toNanos();
        long deadline = System.nanoTime() + remainingNanos;
        ArrayList<ProtocolMessage> deferred = new ArrayList<>();
        try {
            while (remainingNanos >= 0L) {
                ProtocolMessage message = inbox.poll(remainingNanos, TimeUnit.NANOSECONDS);
                if (message == null) {
                    return null;
                }
                if (messageType.isInstance(message)) {
                    return messageType.cast(message);
                }
                deferred.add(message);
                remainingNanos = deadline - System.nanoTime();
            }
            return null;
        } finally {
            restoreDeferred(deferred);
        }
    }

    public ProtocolMessage waitForMessage(String type, Duration timeout) throws InterruptedException {
        long remainingNanos = timeout == null ? 0L : timeout.toNanos();
        long deadline = System.nanoTime() + remainingNanos;
        ArrayList<ProtocolMessage> deferred = new ArrayList<>();
        try {
            while (remainingNanos >= 0L) {
                ProtocolMessage message = inbox.poll(remainingNanos, TimeUnit.NANOSECONDS);
                if (message == null) {
                    return null;
                }
                if (message.type().equals(type)) {
                    return message;
                }
                deferred.add(message);
                remainingNanos = deadline - System.nanoTime();
            }
            return null;
        } finally {
            restoreDeferred(deferred);
        }
    }

    public void disconnect(String reason) {
        if (connected.get()) {
            try {
                send(new ProtocolMessage.Disconnect(getPlayerId(), reason));
            } catch (IOException ignored) {
            }
        }
        close();
    }

    @Override
    public void close() {
        if (!connected.compareAndSet(true, false)) {
            return;
        }

        Socket currentSocket = socket;
        socket = null;
        reader = null;
        writer = null;
        if (currentSocket != null) {
            try {
                currentSocket.close();
            } catch (IOException ignored) {
            }
        }

        Thread thread = readerThread;
        if (thread != null && thread != Thread.currentThread()) {
            try {
                thread.join(1000L);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private void readLoop() {
        try {
            String line;
            while (connected.get() && (line = ProtocolLine.read(reader)) != null) {
                ProtocolMessage message;
                try {
                    message = ProtocolCodec.decode(line);
                } catch (IllegalArgumentException exception) {
                    installLocalDisconnect("Bad packet");
                    break;
                }
                if (!isAcceptedServerMessage(message)) {
                    installLocalDisconnect("Bad packet");
                    break;
                }
                if (!handleMessage(message)) {
                    break;
                }
                inbox.offer(message);
                notifyListeners(message);
                if (message instanceof ProtocolMessage.Disconnect) {
                    break;
                }
            }
        } catch (IOException ignored) {
        } finally {
            boolean wasConnected = connected.getAndSet(false);
            if (wasConnected && disconnect == null) {
                installLocalDisconnect("Connection lost");
            }
            closeSocket();
        }
    }

    private boolean handleMessage(ProtocolMessage message) {
        if (message instanceof ProtocolMessage.Hello currentHello) {
            hello = currentHello;
        } else if (message instanceof ProtocolMessage.KeepAlive keepAlive) {
            try {
                send(keepAlive);
            } catch (IOException ignored) {
                installLocalDisconnect("Timed out");
                connected.set(false);
                closeSocket();
                return false;
            }
        } else if (message instanceof ProtocolMessage.WorldState currentWorldState) {
            worldState = currentWorldState;
        } else if (message instanceof ProtocolMessage.Disconnect currentDisconnect) {
            disconnect = currentDisconnect;
        }
        return true;
    }

    private boolean isAcceptedServerMessage(ProtocolMessage message) {
        if (message instanceof ProtocolMessage.Hello currentHello) {
            return hello == null
                    && currentHello.protocolVersion() == MultiplayerProtocol.VERSION
                    && MultiplayerProtocol.isValidNetworkId(currentHello.playerId())
                    && MultiplayerProtocol.isValidLegacyDataValue(currentHello.serverName());
        }
        if (message instanceof ProtocolMessage.KeepAlive keepAlive) {
            return keepAlive.id() > 0;
        }
        if (message instanceof ProtocolMessage.WorldState currentWorldState) {
            return isValidWorldState(currentWorldState);
        }
        if (message instanceof ProtocolMessage.PlayerList playerList) {
            return isValidPlayerList(playerList);
        }
        if (message instanceof ProtocolMessage.PlayerState playerState) {
            return isValidPlayerState(playerState);
        }
        if (message instanceof ProtocolMessage.BlockUpdate blockUpdate) {
            return isValidBlockUpdate(blockUpdate);
        }
        if (message instanceof ProtocolMessage.EntityUpdate entityUpdate) {
            return isValidEntityUpdate(entityUpdate);
        }
        if (message instanceof ProtocolMessage.InventoryUpdate inventoryUpdate) {
            return isValidInventoryUpdate(inventoryUpdate);
        }
        if (message instanceof ProtocolMessage.WorldEvent worldEvent) {
            return MultiplayerProtocol.isValidNetworkId(worldEvent.eventType())
                    && MultiplayerProtocol.isValidItemStackData(worldEvent.data());
        }
        if (message instanceof ProtocolMessage.Chat chat) {
            return isValidOptionalNetworkId(chat.playerId())
                    && MultiplayerProtocol.isValidLegacyDataValue(chat.sender())
                    && chat.text().length() <= MultiplayerProtocol.MAX_CHAT_TEXT_LENGTH;
        }
        if (message instanceof ProtocolMessage.ClientAction action) {
            return isValidServerClientAction(action);
        }
        if (message instanceof ProtocolMessage.Disconnect currentDisconnect) {
            return isValidOptionalNetworkId(currentDisconnect.playerId())
                    && MultiplayerProtocol.isValidLegacyDataValue(currentDisconnect.reason());
        }
        return false;
    }

    private static boolean isValidWorldState(ProtocolMessage.WorldState worldState) {
        return worldState != null
                && isFinite(worldState.timeOfDay())
                && isValidWorldCoordinate(worldState.spawnX())
                && isValidWorldCoordinate(worldState.spawnZ())
                && worldState.spawnY() >= MultiplayerProtocol.WORLD_MIN_Y
                && worldState.spawnY() < MultiplayerProtocol.WORLD_HEIGHT
                && MultiplayerProtocol.isValidNetworkId(worldState.weatherState())
                && MultiplayerProtocol.isValidNetworkId(worldState.gameMode())
                && MultiplayerProtocol.isValidNetworkId(worldState.difficulty())
                && MultiplayerProtocol.isValidNetworkId(worldState.dimension())
                && worldState.maxPlayers() > 0
                && worldState.viewDistance() >= MultiplayerProtocol.MIN_VIEW_DISTANCE
                && worldState.viewDistance() <= MultiplayerProtocol.MAX_VIEW_DISTANCE
                && worldState.maxBuildHeight() >= MultiplayerProtocol.MIN_MAX_BUILD_HEIGHT
                && worldState.maxBuildHeight() <= MultiplayerProtocol.WORLD_HEIGHT
                && worldState.players().size() <= MultiplayerProtocol.MAX_LEGACY_MESSAGE_DATA_ENTRIES
                && worldState.blockUpdates().size() <= MultiplayerProtocol.MAX_LEGACY_MESSAGE_DATA_ENTRIES
                && worldState.entityUpdates().size() <= MultiplayerProtocol.MAX_LEGACY_MESSAGE_DATA_ENTRIES
                && worldState.inventoryUpdates().size() <= MultiplayerProtocol.MAX_LEGACY_MESSAGE_DATA_ENTRIES
                && worldState.players().stream().allMatch(ProtocolClient::isValidPlayerState)
                && worldState.blockUpdates().stream().allMatch(ProtocolClient::isValidBlockUpdate)
                && worldState.entityUpdates().stream().allMatch(ProtocolClient::isValidEntityUpdate)
                && worldState.inventoryUpdates().stream().allMatch(ProtocolClient::isValidInventoryUpdate);
    }

    private static boolean isValidPlayerList(ProtocolMessage.PlayerList playerList) {
        if (playerList == null || playerList.players().size() > MultiplayerProtocol.MAX_LEGACY_MESSAGE_DATA_ENTRIES) {
            return false;
        }
        for (ProtocolMessage.PlayerListEntry entry : playerList.players()) {
            if (!MultiplayerProtocol.isValidNetworkId(entry.playerId())
                    || !MultiplayerProtocol.isValidLegacyDataValue(entry.username())
                    || entry.latencyMillis() < -1) {
                return false;
            }
        }
        return true;
    }

    private static boolean isValidPlayerState(ProtocolMessage.PlayerState state) {
        return state != null
                && MultiplayerProtocol.isValidNetworkId(state.playerId())
                && MultiplayerProtocol.isValidLegacyDataValue(state.username())
                && isValidPlayerPose(state.pose())
                && isFinite(state.health())
                && state.health() >= 0.0f
                && state.health() <= MultiplayerProtocol.MAX_PLAYER_HEALTH
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

    private static boolean isValidBlockUpdate(ProtocolMessage.BlockUpdate update) {
        return update != null
                && MultiplayerProtocol.isValidBlockUpdate(update.blockId(), update.y(), update.metadata())
                && isValidWorldCoordinate(update.x())
                && isValidWorldCoordinate(update.z())
                && isValidOptionalNetworkId(update.sourcePlayerId())
                && MultiplayerProtocol.isValidItemStackData(update.data());
    }

    private static boolean isValidEntityUpdate(ProtocolMessage.EntityUpdate update) {
        return update != null
                && MultiplayerProtocol.isValidNetworkId(update.entityId())
                && MultiplayerProtocol.isValidNetworkId(update.entityType())
                && isValidPlayerPose(update.pose())
                && MultiplayerProtocol.isValidItemStackData(update.data());
    }

    private static boolean isValidInventoryUpdate(ProtocolMessage.InventoryUpdate update) {
        return update != null
                && MultiplayerProtocol.isValidNetworkId(update.playerId())
                && MultiplayerProtocol.isValidInventoryUpdate(update.itemId(), update.slot(),
                        update.count(), update.damage())
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
            String key = entry.getKey();
            String value = entry.getValue();
            if (!MultiplayerProtocol.isValidLegacyDataKey(key)
                    || !MultiplayerProtocol.isValidLegacyDataValue(value)) {
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

    private static boolean isValidWorldCoordinate(int value) {
        return Math.abs((double) value) <= MultiplayerProtocol.MAX_WORLD_COORDINATE;
    }

    private static boolean isValidOptionalNetworkId(String value) {
        return value == null || value.isBlank() || MultiplayerProtocol.isValidNetworkId(value);
    }

    private static boolean isFinite(double value) {
        return !Double.isNaN(value) && !Double.isInfinite(value);
    }

    private static boolean isFinite(float value) {
        return !Float.isNaN(value) && !Float.isInfinite(value);
    }

    private void installLocalDisconnect(String reason) {
        if (disconnect != null) {
            return;
        }
        ProtocolMessage.Disconnect localDisconnect = new ProtocolMessage.Disconnect(getPlayerId(), reason);
        disconnect = localDisconnect;
        inbox.offer(localDisconnect);
        notifyListeners(localDisconnect);
    }

    private void resetSessionState() {
        inbox.clear();
        hello = null;
        worldState = null;
        disconnect = null;
    }

    private void notifyListeners(ProtocolMessage message) {
        for (ClientMessageListener listener : listeners) {
            listener.onMessage(message);
        }
    }

    private void restoreDeferred(ArrayList<ProtocolMessage> deferred) {
        for (ProtocolMessage message : deferred) {
            inbox.offer(message);
        }
    }

    private void closeSocket() {
        Socket currentSocket = socket;
        socket = null;
        reader = null;
        writer = null;
        if (currentSocket != null) {
            try {
                currentSocket.close();
            } catch (IOException ignored) {
            }
        }
    }

    @FunctionalInterface
    public interface ClientMessageListener {
        void onMessage(ProtocolMessage message);
    }
}
