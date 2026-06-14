package com.craftzero.multiplayer;

import com.craftzero.save.SafeFiles;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class ServerList {
    private final Path path;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public ServerList(Path path) {
        this.path = path;
    }

    public List<ServerEntry> load() {
        if (!Files.exists(path)) {
            return new ArrayList<>();
        }
        try (Reader reader = Files.newBufferedReader(path)) {
            ServerEntry[] entries = gson.fromJson(reader, ServerEntry[].class);
            return entries == null ? new ArrayList<>() : new ArrayList<>(List.of(entries));
        } catch (Exception e) {
            System.err.println("Failed to load server list: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    public void save(List<ServerEntry> entries) throws IOException {
        SafeFiles.writeAtomic(path, writer -> gson.toJson(entries, writer), SafeFiles.BackupPolicy.BAK);
    }

    public record ServerEntry(String name, String host, int port) {
        public String address() {
            return host + ":" + port;
        }
    }
}
