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

            send(new ProtocolMessage.Join(username));
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
        send(new ProtocolMessage.ClientInput(
                getPlayerId(),
                pose,
                forward,
                backward,
                left,
                right,
                jumping,
                sneaking
        ));
    }

    public void sendBlockAction(ProtocolMessage.BlockUpdate update) throws IOException {
        send(ProtocolMessage.ClientAction.blockUpdate(getPlayerId(), update));
    }

    public void sendChat(String sender, String text) throws IOException {
        String chatSender = sender == null || sender.isBlank() ? username : sender;
        send(new ProtocolMessage.Chat(getPlayerId(), chatSender, text));
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
            currentWriter.write(ProtocolCodec.encode(message));
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
            while (connected.get() && (line = reader.readLine()) != null) {
                ProtocolMessage message = ProtocolCodec.decode(line);
                handleMessage(message);
                inbox.offer(message);
                notifyListeners(message);
            }
        } catch (IOException | IllegalArgumentException ignored) {
        } finally {
            connected.set(false);
            closeSocket();
        }
    }

    private void handleMessage(ProtocolMessage message) {
        if (message instanceof ProtocolMessage.Hello currentHello) {
            hello = currentHello;
        } else if (message instanceof ProtocolMessage.WorldState currentWorldState) {
            worldState = currentWorldState;
        } else if (message instanceof ProtocolMessage.Disconnect currentDisconnect) {
            disconnect = currentDisconnect;
        }
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
