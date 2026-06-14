package com.craftzero.save;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SafeFilesTest {
    @TempDir
    Path tempDir;

    @Test
    void failedWriteLeavesExistingFileIntact() throws Exception {
        Path file = tempDir.resolve("options.txt");
        Files.writeString(file, "old");

        assertThrows(IOException.class, () -> SafeFiles.writeAtomic(file, writer -> {
            writer.write("new");
            throw new IOException("boom");
        }, SafeFiles.BackupPolicy.BAK));

        assertEquals("old", Files.readString(file));
        assertFalse(Files.exists(SafeFiles.backupPath(file)));
    }

    @Test
    void successfulWriteCreatesBackupWhenRequested() throws Exception {
        Path file = tempDir.resolve("level.json");
        Files.writeString(file, "old");

        SafeFiles.writeStringAtomic(file, "new", SafeFiles.BackupPolicy.BAK);

        assertEquals("new", Files.readString(file));
        assertTrue(Files.exists(SafeFiles.backupPath(file)));
        assertEquals("old", Files.readString(SafeFiles.backupPath(file)));
    }
}
