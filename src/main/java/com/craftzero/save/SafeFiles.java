package com.craftzero.save;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;

/**
 * Crash-safer same-directory writes for save data and options.
 */
public final class SafeFiles {
    public enum BackupPolicy {
        NONE,
        BAK
    }

    @FunctionalInterface
    public interface WriterAction {
        void write(Writer writer) throws IOException;
    }

    @FunctionalInterface
    public interface StreamAction {
        void write(OutputStream stream) throws IOException;
    }

    private SafeFiles() {
    }

    public static void writeAtomic(Path path, WriterAction action, BackupPolicy backupPolicy) throws IOException {
        writeAtomicBytes(path, stream -> {
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(stream, StandardCharsets.UTF_8));
            action.write(writer);
            writer.flush();
        }, backupPolicy);
    }

    public static void writeStringAtomic(Path path, String content, BackupPolicy backupPolicy) throws IOException {
        writeAtomic(path, writer -> writer.write(content == null ? "" : content), backupPolicy);
    }

    public static void writeAtomicBytes(Path path, StreamAction action, BackupPolicy backupPolicy) throws IOException {
        if (path == null) {
            throw new IllegalArgumentException("path cannot be null");
        }
        Path absolute = path.toAbsolutePath().normalize();
        Path parent = absolute.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        String prefix = tempPrefix(absolute);
        Path temp = Files.createTempFile(parent == null ? Path.of(".") : parent, prefix, ".tmp");
        boolean moved = false;
        try {
            try (FileChannel channel = FileChannel.open(temp, StandardOpenOption.WRITE,
                    StandardOpenOption.TRUNCATE_EXISTING);
                    OutputStream stream = Channels.newOutputStream(channel)) {
                action.write(stream);
                stream.flush();
                channel.force(true);
            }
            if (backupPolicy == BackupPolicy.BAK && Files.exists(absolute)) {
                Files.copy(absolute, backupPath(absolute), StandardCopyOption.REPLACE_EXISTING);
            }
            moveIntoPlace(temp, absolute);
            moved = true;
        } finally {
            if (!moved) {
                Files.deleteIfExists(temp);
            }
        }
    }

    public static Path backupPath(Path path) {
        return path.resolveSibling(path.getFileName().toString() + ".bak");
    }

    private static String tempPrefix(Path path) {
        String fileName = path.getFileName() == null ? "craftzero" : path.getFileName().toString();
        return fileName.length() >= 3 ? fileName : "cz-" + fileName;
    }

    private static void moveIntoPlace(Path source, Path target) throws IOException {
        try {
            Files.move(source, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException ignored) {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
