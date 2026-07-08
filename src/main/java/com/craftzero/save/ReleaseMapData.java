package com.craftzero.save;

import com.craftzero.inventory.MapItemData;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

final class ReleaseMapData {
    private static final String DATA_DIR = "data";
    private static final String MAP_PREFIX = "map_";
    private static final String MAP_SUFFIX = ".dat";
    private static final String IDCOUNTS_FILE = "idcounts.dat";
    private static final int MAP_COLOR_BYTES = MapItemData.MAP_SIZE * MapItemData.MAP_SIZE;
    private static final int DEFAULT_SCALE = MapItemData.DEFAULT_SCALE;

    private static final int TAG_END = 0;
    private static final int TAG_BYTE = 1;
    private static final int TAG_SHORT = 2;
    private static final int TAG_INT = 3;
    private static final int TAG_LONG = 4;
    private static final int TAG_FLOAT = 5;
    private static final int TAG_DOUBLE = 6;
    private static final int TAG_BYTE_ARRAY = 7;
    private static final int TAG_STRING = 8;
    private static final int TAG_LIST = 9;
    private static final int TAG_COMPOUND = 10;
    private static final int TAG_INT_ARRAY = 11;

    private ReleaseMapData() {
    }

    static ImportResult readAll(Path worldDir) throws IOException {
        Map<String, MapState> states = new HashMap<>();
        Path dataDir = worldDir == null ? null : worldDir.resolve(DATA_DIR);
        if (dataDir == null || !Files.isDirectory(dataDir)) {
            return new ImportResult(states);
        }
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dataDir, MAP_PREFIX + "*" + MAP_SUFFIX)) {
            for (Path path : stream) {
                String id = mapIdFromPath(path);
                if (id == null) {
                    continue;
                }
                try {
                    MapState state = read(path, id);
                    if (state != null) {
                        states.put(id, state);
                    }
                } catch (Exception e) {
                    System.err.println("Failed to import Release map data " + path + ": " + e.getMessage());
                }
            }
        }
        int nextMapId = nextMapIdFromStates(states);
        Path idCountsPath = dataDir.resolve(IDCOUNTS_FILE);
        if (Files.isRegularFile(idCountsPath)) {
            try {
                nextMapId = Math.max(nextMapId, readNextMapId(idCountsPath));
            } catch (Exception e) {
                System.err.println("Failed to import Release map id counter " + idCountsPath + ": " + e.getMessage());
            }
        }
        return new ImportResult(states, nextMapId);
    }

    static void writeAll(Path worldDir, List<SaveManager.FilledMapData> maps,
            Map<String, MapState> metadataById) throws IOException {
        writeAll(worldDir, maps, metadataById, 0);
    }

    static void writeAll(Path worldDir, List<SaveManager.FilledMapData> maps,
            Map<String, MapState> metadataById, int nextMapId) throws IOException {
        if (worldDir == null || ((maps == null || maps.isEmpty()) && nextMapId <= 0)) {
            return;
        }
        Path dataDir = worldDir.resolve(DATA_DIR);
        Files.createDirectories(dataDir);

        int maxMapId = -1;
        for (SaveManager.FilledMapData map : maps) {
            if (map == null || map.id == null || map.colors == null) {
                continue;
            }
            int numericId = mapNumericId(map.id);
            if (numericId < 0) {
                continue;
            }
            byte[] colors;
            try {
                colors = Base64.getDecoder().decode(map.colors);
            } catch (IllegalArgumentException ignored) {
                continue;
            }
            if (colors.length != MAP_COLOR_BYTES) {
                continue;
            }
            MapState metadata = metadataById == null ? null : metadataById.get(map.id);
            MapState state = new MapState(map.id,
                    metadata == null ? DEFAULT_SCALE : metadata.scale(),
                    metadata == null ? 0 : metadata.centerX(),
                    metadata == null ? 0 : metadata.centerZ(),
                    metadata == null ? 0 : metadata.dimension(),
                    colors.clone());
            write(dataDir.resolve(map.id + MAP_SUFFIX), state);
            maxMapId = Math.max(maxMapId, numericId);
        }
        int idCount = Math.max(maxMapId, nextMapId - 1);
        if (idCount >= 0) {
            writeIdCounts(dataDir.resolve(IDCOUNTS_FILE), idCount);
        }
    }

    private static MapState read(Path path, String id) throws IOException {
        Map<String, Object> root = readRoot(path);
        Map<String, Object> data = compound(root.get("data"));
        if (data.isEmpty()) {
            data = root;
        }
        byte[] colors = byteArray(data.get("colors"));
        if (colors == null || colors.length != MAP_COLOR_BYTES) {
            return null;
        }
        return new MapState(id,
                clampScale(intValue(data, "scale", DEFAULT_SCALE)),
                intValue(data, "xCenter", 0),
                intValue(data, "zCenter", 0),
                intValue(data, "dimension", 0),
                colors.clone());
    }

    private static void write(Path path, MapState state) throws IOException {
        SafeFiles.writeAtomicBytes(path, stream -> {
            GZIPOutputStream gzip = new GZIPOutputStream(stream);
            DataOutputStream out = new DataOutputStream(gzip);
            out.writeByte(TAG_COMPOUND);
            writeString(out, "");
            writeNamedCompound(out, "data");
            writeByte(out, "scale", clampScale(state.scale()));
            writeInt(out, "xCenter", state.centerX());
            writeInt(out, "zCenter", state.centerZ());
            writeByte(out, "dimension", state.dimension());
            writeShort(out, "width", MapItemData.MAP_SIZE);
            writeShort(out, "height", MapItemData.MAP_SIZE);
            writeByteArray(out, "colors", state.colors());
            out.writeByte(TAG_END);
            out.writeByte(TAG_END);
            out.flush();
            gzip.finish();
        }, SafeFiles.BackupPolicy.BAK);
    }

    private static void writeIdCounts(Path path, int maxMapId) throws IOException {
        SafeFiles.writeAtomicBytes(path, stream -> {
            GZIPOutputStream gzip = new GZIPOutputStream(stream);
            DataOutputStream out = new DataOutputStream(gzip);
            out.writeByte(TAG_COMPOUND);
            writeString(out, "");
            writeShort(out, "map", maxMapId);
            out.writeByte(TAG_END);
            out.flush();
            gzip.finish();
        }, SafeFiles.BackupPolicy.BAK);
    }

    private static int readNextMapId(Path path) throws IOException {
        Map<String, Object> root = readRoot(path);
        Map<String, Object> data = compound(root.get("data"));
        if (data.isEmpty()) {
            data = root;
        }
        int lastMapId = intValue(data, "map", -1);
        return lastMapId < 0 ? 0 : lastMapId + 1;
    }

    private static Map<String, Object> readRoot(Path path) throws IOException {
        try (DataInputStream in = new DataInputStream(new GZIPInputStream(Files.newInputStream(path)))) {
            int type = in.readUnsignedByte();
            if (type != TAG_COMPOUND) {
                throw new IOException("map data root is not a compound");
            }
            readString(in);
            return readCompound(in);
        } catch (EOFException exception) {
            throw new IOException("map data ended unexpectedly", exception);
        }
    }

    private static Map<String, Object> readCompound(DataInputStream in) throws IOException {
        Map<String, Object> values = new HashMap<>();
        while (true) {
            int type = in.readUnsignedByte();
            if (type == TAG_END) {
                return values;
            }
            String name = readString(in);
            values.put(name, readPayload(in, type));
        }
    }

    private static Object readPayload(DataInputStream in, int type) throws IOException {
        return switch (type) {
            case TAG_BYTE -> in.readByte();
            case TAG_SHORT -> in.readShort();
            case TAG_INT -> in.readInt();
            case TAG_LONG -> in.readLong();
            case TAG_FLOAT -> in.readFloat();
            case TAG_DOUBLE -> in.readDouble();
            case TAG_BYTE_ARRAY -> {
                int length = checkedLength(in.readInt());
                byte[] data = new byte[length];
                in.readFully(data);
                yield data;
            }
            case TAG_STRING -> readString(in);
            case TAG_LIST -> readList(in);
            case TAG_COMPOUND -> readCompound(in);
            case TAG_INT_ARRAY -> {
                int length = checkedLength(in.readInt());
                int[] values = new int[length];
                for (int i = 0; i < values.length; i++) {
                    values[i] = in.readInt();
                }
                yield values;
            }
            default -> throw new IOException("Unsupported map data NBT tag type: " + type);
        };
    }

    private static Object[] readList(DataInputStream in) throws IOException {
        int elementType = in.readUnsignedByte();
        int length = checkedLength(in.readInt());
        Object[] values = new Object[length];
        for (int i = 0; i < values.length; i++) {
            values[i] = readPayload(in, elementType);
        }
        return values;
    }

    private static int checkedLength(int length) throws IOException {
        if (length < 0 || length > 2_000_000) {
            throw new IOException("invalid map data array/list length: " + length);
        }
        return length;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> compound(Object value) {
        return value instanceof Map<?, ?> map ? (Map<String, Object>) map : Map.of();
    }

    private static byte[] byteArray(Object value) {
        return value instanceof byte[] bytes ? bytes : null;
    }

    private static int intValue(Map<String, Object> data, String key, int fallback) {
        Object value = data.get(key);
        return value instanceof Number number ? number.intValue() : fallback;
    }

    private static void writeNamedCompound(DataOutputStream out, String name) throws IOException {
        out.writeByte(TAG_COMPOUND);
        writeString(out, name);
    }

    private static void writeByte(DataOutputStream out, String name, int value) throws IOException {
        out.writeByte(TAG_BYTE);
        writeString(out, name);
        out.writeByte(value);
    }

    private static void writeShort(DataOutputStream out, String name, int value) throws IOException {
        out.writeByte(TAG_SHORT);
        writeString(out, name);
        out.writeShort(value);
    }

    private static void writeInt(DataOutputStream out, String name, int value) throws IOException {
        out.writeByte(TAG_INT);
        writeString(out, name);
        out.writeInt(value);
    }

    private static void writeByteArray(DataOutputStream out, String name, byte[] value) throws IOException {
        out.writeByte(TAG_BYTE_ARRAY);
        writeString(out, name);
        out.writeInt(value.length);
        out.write(value);
    }

    private static void writeString(DataOutputStream out, String value) throws IOException {
        byte[] bytes = (value == null ? "" : value).getBytes(java.nio.charset.StandardCharsets.UTF_8);
        if (bytes.length > 65_535) {
            throw new IOException("map data NBT string too long");
        }
        out.writeShort(bytes.length);
        out.write(bytes);
    }

    private static String readString(DataInputStream in) throws IOException {
        int length = in.readUnsignedShort();
        byte[] bytes = new byte[length];
        in.readFully(bytes);
        return new String(bytes, java.nio.charset.StandardCharsets.UTF_8);
    }

    private static String mapIdFromPath(Path path) {
        if (path == null || path.getFileName() == null) {
            return null;
        }
        String fileName = path.getFileName().toString();
        if (!fileName.startsWith(MAP_PREFIX) || !fileName.endsWith(MAP_SUFFIX)) {
            return null;
        }
        String id = fileName.substring(0, fileName.length() - MAP_SUFFIX.length());
        return mapNumericId(id) >= 0 ? id : null;
    }

    private static int mapNumericId(String id) {
        if (id == null || !id.startsWith(MAP_PREFIX)) {
            return -1;
        }
        try {
            return Integer.parseInt(id.substring(MAP_PREFIX.length()));
        } catch (NumberFormatException ignored) {
            return -1;
        }
    }

    private static int nextMapIdFromStates(Map<String, MapState> states) {
        int next = 0;
        if (states == null) {
            return next;
        }
        for (String id : states.keySet()) {
            int numericId = mapNumericId(id);
            if (numericId >= 0) {
                next = Math.max(next, numericId + 1);
            }
        }
        return next;
    }

    private static int clampScale(int scale) {
        return Math.max(0, Math.min(4, scale));
    }

    record MapState(String id, int scale, int centerX, int centerZ, int dimension, byte[] colors) {
    }

    static final class ImportResult {
        private final Map<String, MapState> states;
        private final int nextMapId;

        ImportResult(Map<String, MapState> states) {
            this(states, nextMapIdFromStates(states));
        }

        ImportResult(Map<String, MapState> states, int nextMapId) {
            this.states = states == null ? Map.of() : Map.copyOf(states);
            this.nextMapId = Math.max(Math.max(0, nextMapId), nextMapIdFromStates(this.states));
        }

        boolean isEmpty() {
            return states.isEmpty();
        }

        Map<String, MapState> states() {
            return states;
        }

        int nextMapId() {
            return nextMapId;
        }

        List<SaveManager.FilledMapData> filledMaps() {
            List<SaveManager.FilledMapData> maps = new ArrayList<>();
            for (MapState state : states.values()) {
                SaveManager.FilledMapData data = SaveManager.FilledMapData.from(state.id(), state.colors());
                if (data != null) {
                    maps.add(data);
                }
            }
            return maps;
        }
    }
}
