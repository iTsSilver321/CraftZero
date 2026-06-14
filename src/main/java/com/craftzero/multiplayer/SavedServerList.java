package com.craftzero.multiplayer;

import com.craftzero.save.SafeFiles;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class SavedServerList {
    private final Path file;
    private final ArrayList<SavedServer> servers;

    private SavedServerList(Path file, List<SavedServer> servers) {
        this.file = file;
        this.servers = new ArrayList<>(servers);
    }

    public static Path defaultPath(Path craftZeroDirectory) {
        return craftZeroDirectory.resolve("servers.json");
    }

    public static SavedServerList load(Path file) throws IOException {
        if (!Files.exists(file)) {
            return new SavedServerList(file, List.of());
        }

        String content = Files.readString(file, StandardCharsets.UTF_8);
        if (content.isBlank()) {
            return new SavedServerList(file, List.of());
        }

        try {
            Map<String, Object> root = asObject(Json.parse(content));
            Object value = root.get("servers");
            if (!(value instanceof List<?> rawServers)) {
                return new SavedServerList(file, List.of());
            }

            ArrayList<SavedServer> loaded = new ArrayList<>();
            for (Object rawServer : rawServers) {
                Map<String, Object> server = asObject(rawServer);
                loaded.add(new SavedServer(
                        string(server, "name", "Server"),
                        string(server, "host", "127.0.0.1"),
                        integer(server, "port", MultiplayerProtocol.DEFAULT_PORT),
                        longNumber(server, "lastConnectedEpochMillis", 0L)
                ));
            }
            return new SavedServerList(file, loaded);
        } catch (IllegalArgumentException exception) {
            throw new IOException("Invalid saved server list: " + file, exception);
        }
    }

    public Path file() {
        return file;
    }

    public synchronized List<SavedServer> entries() {
        return List.copyOf(servers);
    }

    public synchronized void addOrUpdate(SavedServer server) {
        for (int i = 0; i < servers.size(); i++) {
            SavedServer existing = servers.get(i);
            if (sameAddress(existing, server)) {
                servers.set(i, server);
                return;
            }
        }
        servers.add(server);
    }

    public synchronized boolean remove(String host, int port) {
        return servers.removeIf(server -> server.host().equalsIgnoreCase(host) && server.port() == port);
    }

    public synchronized void save() throws IOException {
        Path parent = file.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        SafeFiles.writeStringAtomic(file, Json.stringify(toJson()), SafeFiles.BackupPolicy.BAK);
    }

    private synchronized Map<String, Object> toJson() {
        LinkedHashMap<String, Object> root = new LinkedHashMap<>();
        ArrayList<Map<String, Object>> serializedServers = new ArrayList<>();
        for (SavedServer server : servers) {
            LinkedHashMap<String, Object> object = new LinkedHashMap<>();
            object.put("name", server.name());
            object.put("host", server.host());
            object.put("port", server.port());
            object.put("lastConnectedEpochMillis", server.lastConnectedEpochMillis());
            serializedServers.add(object);
        }
        root.put("servers", serializedServers);
        return root;
    }

    private static boolean sameAddress(SavedServer left, SavedServer right) {
        return left.host().equalsIgnoreCase(right.host()) && left.port() == right.port();
    }

    private static Map<String, Object> asObject(Object value) {
        if (!(value instanceof Map<?, ?> raw)) {
            throw new IllegalArgumentException("Expected JSON object");
        }
        LinkedHashMap<String, Object> object = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : raw.entrySet()) {
            object.put(String.valueOf(entry.getKey()), entry.getValue());
        }
        return object;
    }

    private static String string(Map<String, Object> object, String key, String fallback) {
        Object value = object.get(key);
        return value == null ? fallback : String.valueOf(value);
    }

    private static int integer(Map<String, Object> object, String key, int fallback) {
        return (int) longNumber(object, key, fallback);
    }

    private static long longNumber(Map<String, Object> object, String key, long fallback) {
        Object value = object.get(key);
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value instanceof String string) {
            try {
                return Long.parseLong(string);
            } catch (NumberFormatException ignored) {
                return fallback;
            }
        }
        return fallback;
    }
}
