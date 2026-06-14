package com.craftzero.multiplayer;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public final class ProtocolServer implements AutoCloseable {
    private final int requestedPort;
    private final long worldSeed;
    private final String serverName;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicInteger nextPlayerId = new AtomicInteger(1);
    private final ConcurrentHashMap<String, ClientConnection> clients = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, ProtocolMessage.PlayerState> playerStates = new ConcurrentHashMap<>();
    private final CopyOnWriteArrayList<ServerMessageListener> listeners = new CopyOnWriteArrayList<>();

    private volatile double worldTime;
    private volatile ServerSocket serverSocket;
    private volatile Thread acceptThread;

    public ProtocolServer(long worldSeed, double worldTime) {
        this(MultiplayerProtocol.DEFAULT_PORT, worldSeed, worldTime, "CraftZero");
    }

    public ProtocolServer(int port, long worldSeed, double worldTime) {
        this(port, worldSeed, worldTime, "CraftZero");
    }

    public ProtocolServer(int port, long worldSeed, double worldTime, String serverName) {
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
            socket.bind(new InetSocketAddress(requestedPort));
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

    public int connectedPlayerCount() {
        return clients.size();
    }

    public int trackedPlayerCount() {
        return playerStates.size();
    }

    public List<ProtocolMessage.PlayerState> currentPlayers() {
        return playerStates.values().stream()
                .sorted(Comparator.comparing(ProtocolMessage.PlayerState::playerId))
                .toList();
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

        ArrayList<ClientConnection> failed = new ArrayList<>();
        for (ClientConnection client : clients.values()) {
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

    public void broadcastBlockUpdate(ProtocolMessage.BlockUpdate update) {
        broadcast(update);
    }

    public void broadcastEntityUpdate(ProtocolMessage.EntityUpdate update) {
        broadcast(update);
    }

    public void broadcastInventoryUpdate(ProtocolMessage.InventoryUpdate update) {
        broadcast(update);
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
        if (socket != null) {
            try {
                socket.close();
            } catch (IOException ignored) {
            }
        }

        for (ClientConnection client : List.copyOf(clients.values())) {
            removeClient(client, "server closed", false);
        }

        Thread thread = acceptThread;
        acceptThread = null;
        if (thread != null && thread != Thread.currentThread()) {
            try {
                thread.join(1000L);
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
        String playerId = "player-" + nextPlayerId.getAndIncrement();
        ClientConnection client = new ClientConnection(playerId, socket);
        clients.put(playerId, client);

        try {
            client.send(new ProtocolMessage.Hello(
                    MultiplayerProtocol.VERSION,
                    playerId,
                    serverName
            ));
            client.send(new ProtocolMessage.WorldState(worldSeed, worldTime, currentPlayers()));
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
                ProtocolMessage message = ProtocolCodec.decode(line);
                notifyListeners(client.playerId(), message);
                handleMessage(client, message);
            }
        } catch (IOException | IllegalArgumentException ignored) {
        } finally {
            removeClient(client, "connection closed", true);
        }
    }

    private void handleMessage(ClientConnection client, ProtocolMessage message) {
        if (message instanceof ProtocolMessage.Join join) {
            client.setUsername(join.username());
            return;
        }

        if (message instanceof ProtocolMessage.ClientInput input) {
            ProtocolMessage.PlayerState state = new ProtocolMessage.PlayerState(
                    client.playerId(),
                    client.username(),
                    input.pose(),
                    !input.jumping()
            );
            playerStates.put(client.playerId(), state);
            broadcast(state);
            return;
        }

        if (message instanceof ProtocolMessage.ClientAction action) {
            if (action.blockUpdate() != null) {
                broadcast(action.blockUpdate().withSourcePlayerId(client.playerId()));
            }
            return;
        }

        if (message instanceof ProtocolMessage.Chat chat) {
            ProtocolMessage.Chat normalized = chat
                    .withPlayerId(client.playerId())
                    .withSender("Player".equals(chat.sender()) ? client.username() : chat.sender());
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

    private void removeClient(ClientConnection client, String reason, boolean announce) {
        if (!clients.remove(client.playerId(), client)) {
            client.close();
            return;
        }

        playerStates.remove(client.playerId());
        client.close();
        if (announce && running.get()) {
            broadcast(new ProtocolMessage.Disconnect(client.playerId(), reason));
        }
    }

    @FunctionalInterface
    public interface ServerMessageListener {
        void onMessage(String playerId, ProtocolMessage message);
    }

    private final class ClientConnection {
        private final String playerId;
        private final Socket socket;
        private final BufferedReader reader;
        private final BufferedWriter writer;
        private final AtomicBoolean open = new AtomicBoolean(true);
        private volatile String username = "Player";

        private ClientConnection(String playerId, Socket socket) throws IOException {
            this.playerId = playerId;
            this.socket = socket;
            this.reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
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

        private void start() {
            Thread thread = new Thread(() -> readClient(this), "CraftZero-Protocol-Client-" + playerId);
            thread.setDaemon(true);
            thread.start();
        }

        private String readLine() throws IOException {
            return reader.readLine();
        }

        private boolean isOpen() {
            return open.get();
        }

        private void send(ProtocolMessage message) throws IOException {
            synchronized (writer) {
                writer.write(ProtocolCodec.encode(message));
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
