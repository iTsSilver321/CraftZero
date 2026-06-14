package com.craftzero.multiplayer;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class SavedServerListTest {
    @TempDir
    Path tempDir;

    @Test
    @DisplayName("Saved server list should persist and update direct-connect entries")
    void savedServerListPersistsEntries() throws Exception {
        Path file = tempDir.resolve("servers.json");
        SavedServerList list = SavedServerList.load(file);

        assertTrue(list.entries().isEmpty());

        list.addOrUpdate(new SavedServer("Localhost", "127.0.0.1", 25565, 1000L));
        list.addOrUpdate(new SavedServer("Loopback", "127.0.0.1", 25565, 2000L));
        list.addOrUpdate(new SavedServer("LAN", "192.168.1.20", 25566, 3000L));
        list.save();

        SavedServerList loaded = SavedServerList.load(file);

        assertEquals(2, loaded.entries().size());
        assertEquals("Loopback", loaded.entries().get(0).name());
        assertEquals("127.0.0.1", loaded.entries().get(0).host());
        assertEquals(25565, loaded.entries().get(0).port());
        assertEquals(2000L, loaded.entries().get(0).lastConnectedEpochMillis());
        assertTrue(loaded.remove("192.168.1.20", 25566));
        assertEquals(1, loaded.entries().size());
    }
}
