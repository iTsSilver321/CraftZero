package com.craftzero.multiplayer;

import java.time.Instant;
import java.util.Objects;

public record SavedServer(
        String name,
        String host,
        int port,
        long lastConnectedEpochMillis
) {
    public SavedServer {
        name = normalize(name, "Server");
        host = normalize(host, "127.0.0.1");
        if (port <= 0 || port > 65535) {
            throw new IllegalArgumentException("Port must be between 1 and 65535");
        }
    }

    public static SavedServer direct(String name, String host) {
        return new SavedServer(name, host, MultiplayerProtocol.DEFAULT_PORT, 0L);
    }

    public SavedServer markConnected(Instant instant) {
        Objects.requireNonNull(instant, "instant");
        return new SavedServer(name, host, port, instant.toEpochMilli());
    }

    private static String normalize(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value;
    }
}
