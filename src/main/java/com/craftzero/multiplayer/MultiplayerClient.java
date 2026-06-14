package com.craftzero.multiplayer;

import java.io.Closeable;
import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

public class MultiplayerClient implements Closeable {
    private static final Duration INITIAL_SYNC_TIMEOUT = Duration.ofSeconds(3);

    private final List<Consumer<NetworkMessage>> listeners = new CopyOnWriteArrayList<>();
    private volatile ProtocolClient protocolClient;
    private volatile int clientId = -1;
    private volatile long seed;
    private volatile float worldTime;
    private volatile String username = "Player";

    public void connect(String host, int port) throws IOException {
        connect(host, port, username);
    }

    public void connect(String host, int port, String username) throws IOException {
        ProtocolClient existing = protocolClient;
        if (existing != null && existing.isConnected()) {
            return;
        }

        this.username = username == null || username.isBlank() ? "Player" : username;
        ProtocolClient client = new ProtocolClient(normalizeHost(host), port, this.username);
        client.addListener(this::handleProtocolMessage);
        protocolClient = client;
        try {
            client.connect();
            ProtocolMessage.Hello hello = client.awaitHello(INITIAL_SYNC_TIMEOUT);
            ProtocolMessage.WorldState worldState = client.awaitWorldState(INITIAL_SYNC_TIMEOUT);
            if (hello == null || worldState == null) {
                throw new IOException("Timed out waiting for multiplayer initial sync");
            }
            applyInitialSync(hello, worldState);
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
        ProtocolMessage protocolMessage = MultiplayerServer.toProtocolMessage(message);
        if (protocolMessage == null) {
            return;
        }
        if (protocolMessage instanceof ProtocolMessage.BlockUpdate blockUpdate) {
            client.sendBlockAction(blockUpdate);
            return;
        }
        client.send(protocolMessage);
    }

    public void sendPlayerState(float x, float y, float z, float yaw, float pitch) throws IOException {
        requireClient().sendInput(new ProtocolMessage.PlayerPose(x, y, z, yaw, pitch));
    }

    public void sendChat(String sender, String text) throws IOException {
        requireClient().sendChat(sender == null || sender.isBlank() ? username : sender, text);
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

    @Override
    public void close() {
        ProtocolClient client = protocolClient;
        protocolClient = null;
        if (client != null) {
            client.close();
        }
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
            seed = worldState.seed();
            worldTime = (float) worldState.timeOfDay();
        } else if (message instanceof ProtocolMessage.Disconnect) {
            close();
        }

        NetworkMessage networkMessage = MultiplayerServer.toNetworkMessage(message);
        for (Consumer<NetworkMessage> listener : listeners) {
            listener.accept(networkMessage);
        }
    }

    private void applyInitialSync(ProtocolMessage.Hello hello, ProtocolMessage.WorldState worldState) {
        clientId = MultiplayerServer.legacyClientId(hello.playerId());
        seed = worldState.seed();
        worldTime = (float) worldState.timeOfDay();
    }

    private static String normalizeHost(String host) {
        return host == null || host.isBlank() ? "127.0.0.1" : host;
    }
}
