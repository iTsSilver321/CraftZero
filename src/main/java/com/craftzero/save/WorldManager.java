package com.craftzero.save;

import com.craftzero.main.Difficulty;
import com.craftzero.main.GameMode;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.stream.Stream;

/**
 * Test-friendly world catalog for singleplayer menus.
 */
public final class WorldManager {
    public static final Path DEFAULT_SAVES_ROOT = Paths.get("saves");
    public static final String DEFAULT_WORLD_ID = "default";
    public static final String DEFAULT_WORLD_NAME = "Default World";
    public static final String METADATA_FILE = "world.json";
    private static final String LEVEL_FILE = "level.json";
    private static final String SERVER_PROPERTIES_FILE = "server.properties";

    private final Path savesRoot;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public WorldManager() {
        this(DEFAULT_SAVES_ROOT);
    }

    public WorldManager(Path savesRoot) {
        if (savesRoot == null) {
            throw new IllegalArgumentException("savesRoot cannot be null");
        }
        this.savesRoot = savesRoot.toAbsolutePath().normalize();
    }

    public Path getSavesRoot() {
        return savesRoot;
    }

    public List<WorldInfo> listWorlds() throws IOException {
        if (!Files.isDirectory(savesRoot)) {
            return List.of();
        }

        List<WorldInfo> worlds = new ArrayList<>();
        try (Stream<Path> entries = Files.list(savesRoot)) {
            for (Path entry : entries.filter(Files::isDirectory).toList()) {
                WorldInfo info = readWorldInfo(entry);
                if (info != null) {
                    worlds.add(info);
                }
            }
        }

        worlds.sort(Comparator
                .comparing((WorldInfo info) -> !DEFAULT_WORLD_ID.equals(info.id()))
                .thenComparing(WorldInfo::lastModified, Comparator.reverseOrder())
                .thenComparing(WorldInfo::displayName, String.CASE_INSENSITIVE_ORDER));
        return List.copyOf(worlds);
    }

    public WorldInfo createWorld(String displayName) throws IOException {
        return createWorld(displayName, System.currentTimeMillis(), GameMode.SURVIVAL, Difficulty.EASY);
    }

    public WorldInfo createWorld(String displayName, long seed, GameMode gameMode, Difficulty difficulty) throws IOException {
        Files.createDirectories(savesRoot);

        String cleanedName = cleanDisplayName(displayName);
        String id = uniqueWorldId(cleanedName);
        Path worldPath = resolveWorldPath(id);
        Files.createDirectories(worldPath);

        WorldMetadata metadata = new WorldMetadata();
        metadata.displayName = cleanedName;
        metadata.seed = seed;
        metadata.gameMode = (gameMode == null ? GameMode.SURVIVAL : gameMode).optionName();
        metadata.difficulty = (difficulty == null ? Difficulty.EASY : difficulty).optionName();
        metadata.createdAt = Instant.now().toString();
        metadata.lastPlayed = metadata.createdAt;
        writeMetadata(worldPath, metadata);

        return readWorldInfo(worldPath);
    }

    public WorldInfo renameWorld(String worldId, String newDisplayName) throws IOException {
        Path worldPath = resolveWorldPath(worldId);
        if (!Files.isDirectory(worldPath)) {
            throw new NoSuchFileException(worldPath.toString());
        }

        WorldInfo current = readWorldInfo(worldPath);
        WorldMetadata metadata = readMetadata(worldPath);
        if (metadata == null) {
            metadata = new WorldMetadata();
            metadata.seed = current == null ? 0L : current.seed();
            metadata.gameMode = current == null ? GameMode.SURVIVAL.optionName() : current.gameMode().optionName();
            metadata.difficulty = current == null ? Difficulty.EASY.optionName() : current.difficulty().optionName();
            metadata.createdAt = current == null ? Instant.now().toString() : current.createdAt().toString();
        }
        metadata.displayName = cleanDisplayName(newDisplayName);
        metadata.lastPlayed = Instant.now().toString();
        writeMetadata(worldPath, metadata);

        return readWorldInfo(worldPath);
    }

    public boolean deleteWorld(String worldId) throws IOException {
        Path worldPath = resolveWorldPath(worldId);
        if (!Files.exists(worldPath)) {
            return false;
        }
        if (!Files.isDirectory(worldPath)) {
            throw new IOException("World path is not a directory: " + worldPath);
        }

        try (Stream<Path> walk = Files.walk(worldPath)) {
            for (Path path : walk.sorted(Comparator.reverseOrder()).toList()) {
                Files.delete(path);
            }
        }
        return true;
    }

    public SaveManager openSaveManager(String worldId) {
        return new SaveManager(resolveWorldPath(worldId));
    }

    public Path getWorldPath(String worldId) {
        return resolveWorldPath(worldId);
    }

    private WorldInfo readWorldInfo(Path worldPath) throws IOException {
        String id = worldPath.getFileName().toString();
        Path metadataPath = worldPath.resolve(METADATA_FILE);
        Path levelPath = worldPath.resolve(LEVEL_FILE);
        Path levelBackupPath = SafeFiles.backupPath(levelPath);
        Path releaseLevelPath = worldPath.resolve(ReleaseLevelDat.FILE_NAME);
        Path releaseLevelOldPath = worldPath.resolve(ReleaseLevelDat.OLD_FILE_NAME);
        Path serverPropertiesPath = worldPath.resolve(SERVER_PROPERTIES_FILE);
        boolean hasMetadata = Files.isRegularFile(metadataPath);
        boolean hasLevelData = Files.isRegularFile(levelPath)
                || Files.isRegularFile(levelBackupPath)
                || Files.isRegularFile(releaseLevelPath)
                || Files.isRegularFile(releaseLevelOldPath)
                || Files.isRegularFile(serverPropertiesPath);
        boolean legacyDefault = DEFAULT_WORLD_ID.equals(id) && !hasMetadata;

        WorldMetadata metadata = readMetadata(worldPath);
        SaveManager.LevelData levelData = readLevelData(worldPath);
        Properties serverProperties = readServerProperties(worldPath);
        String displayName = displayNameFor(id, metadata, levelData, serverProperties);
        long seed = seedFor(metadata, levelData, serverProperties);
        GameMode gameMode = gameModeFor(metadata, levelData, serverProperties);
        Difficulty difficulty = difficultyFor(metadata, levelData, serverProperties, gameMode);
        Instant createdAt = parseInstant(metadata == null ? null : metadata.createdAt, fileInstant(worldPath));
        Instant lastModified = latestInstant(
                parseInstantOrNull(metadata == null ? null : metadata.lastPlayed),
                millisInstant(levelData == null ? 0L : levelData.lastPlayed),
                fileInstantIfExists(worldPath),
                fileInstantIfExists(metadataPath),
                fileInstantIfExists(levelPath),
                fileInstantIfExists(levelBackupPath),
                fileInstantIfExists(releaseLevelPath),
                fileInstantIfExists(releaseLevelOldPath),
                fileInstantIfExists(serverPropertiesPath));

        return new WorldInfo(id, displayName, worldPath, seed, gameMode, difficulty,
                legacyDefault, hasLevelData, hasMetadata, createdAt, lastModified);
    }

    private WorldMetadata readMetadata(Path worldPath) {
        Path metadataPath = worldPath.resolve(METADATA_FILE);
        if (!Files.isRegularFile(metadataPath)) {
            return null;
        }

        try (Reader reader = Files.newBufferedReader(metadataPath)) {
            return gson.fromJson(reader, WorldMetadata.class);
        } catch (Exception ignored) {
            return null;
        }
    }

    private SaveManager.LevelData readLevelData(Path worldPath) {
        Path levelPath = worldPath.resolve(LEVEL_FILE);
        if (!Files.isRegularFile(levelPath)) {
            SaveManager.LevelData backupData = readSupportedCatalogLevelData(SafeFiles.backupPath(levelPath));
            if (backupData != null) {
                return backupData;
            }
            return readReleaseLevelData(worldPath);
        }

        SaveManager.LevelData data = readSupportedCatalogLevelData(levelPath);
        if (data != null) {
            return data;
        }
        SaveManager.LevelData backupData = readSupportedCatalogLevelData(SafeFiles.backupPath(levelPath));
        return backupData != null ? backupData : readReleaseLevelData(worldPath);
    }

    private SaveManager.LevelData readSupportedCatalogLevelData(Path path) {
        if (path == null || !Files.isRegularFile(path)) {
            return null;
        }
        try (Reader reader = Files.newBufferedReader(path)) {
            SaveManager.LevelData data = gson.fromJson(reader, SaveManager.LevelData.class);
            return isSupportedCatalogLevelData(data) ? data : null;
        } catch (Exception ignored) {
            return null;
        }
    }

    private static boolean isSupportedCatalogLevelData(SaveManager.LevelData data) {
        return data != null
                && data.formatVersion >= SaveManager.MIN_SUPPORTED_FORMAT_VERSION
                && data.formatVersion <= SaveManager.FORMAT_VERSION;
    }

    private SaveManager.LevelData readReleaseLevelData(Path worldPath) {
        Path releaseLevelPath = worldPath.resolve(ReleaseLevelDat.FILE_NAME);
        if (Files.isRegularFile(releaseLevelPath)) {
            try {
                return ReleaseLevelDat.read(releaseLevelPath);
            } catch (Exception ignored) {
            }
        }

        Path releaseLevelOldPath = worldPath.resolve(ReleaseLevelDat.OLD_FILE_NAME);
        if (Files.isRegularFile(releaseLevelOldPath)) {
            try {
                return ReleaseLevelDat.read(releaseLevelOldPath);
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    private Properties readServerProperties(Path worldPath) {
        Path path = worldPath.resolve(SERVER_PROPERTIES_FILE);
        if (!Files.isRegularFile(path)) {
            return null;
        }
        Properties properties = new Properties();
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            properties.load(reader);
            return properties;
        } catch (Exception ignored) {
            return null;
        }
    }

    private void writeMetadata(Path worldPath, WorldMetadata metadata) throws IOException {
        SafeFiles.writeAtomic(worldPath.resolve(METADATA_FILE), writer -> gson.toJson(metadata, writer),
                SafeFiles.BackupPolicy.BAK);
    }

    private Path resolveWorldPath(String worldId) {
        if (worldId == null || worldId.isBlank()) {
            throw new IllegalArgumentException("worldId cannot be blank");
        }

        Path worldPath = savesRoot.resolve(worldId).normalize();
        if (!worldPath.startsWith(savesRoot)
                || worldPath.equals(savesRoot)
                || !savesRoot.equals(worldPath.getParent())) {
            throw new IllegalArgumentException("World id must resolve to a direct child of " + savesRoot);
        }
        return worldPath;
    }

    private String uniqueWorldId(String displayName) {
        String base = slug(displayName);
        String id = base;
        int suffix = 1;
        while (Files.exists(resolveWorldPath(id))) {
            id = base + "-" + suffix;
            suffix++;
        }
        return id;
    }

    private static String displayNameFor(String id, WorldMetadata metadata, SaveManager.LevelData levelData,
            Properties serverProperties) {
        if (metadata != null && metadata.displayName != null && !metadata.displayName.isBlank()) {
            return metadata.displayName.trim();
        }
        String serverLevelName = serverPropertyText(serverProperties, "level-name");
        if (serverLevelName != null) {
            return serverLevelName;
        }
        if (levelData != null && levelData.levelName != null && !levelData.levelName.isBlank()) {
            return levelData.levelName.trim();
        }
        if (DEFAULT_WORLD_ID.equals(id)) {
            return DEFAULT_WORLD_NAME;
        }
        return humanizeId(id);
    }

    private static long seedFor(WorldMetadata metadata, SaveManager.LevelData levelData, Properties serverProperties) {
        long seed = levelData != null ? levelData.seed : metadata != null ? metadata.seed : 0L;
        return serverPropertySeed(serverProperties, seed);
    }

    private static GameMode gameModeFor(WorldMetadata metadata, SaveManager.LevelData levelData,
            Properties serverProperties) {
        GameMode gameMode = levelData != null ? levelData.getGameMode()
                : metadata != null ? GameMode.fromName(metadata.gameMode) : GameMode.SURVIVAL;
        gameMode = serverPropertyGameMode(serverProperties, gameMode);
        return serverPropertyBoolean(serverProperties, "hardcore", false) || gameMode == GameMode.HARDCORE
                ? GameMode.HARDCORE
                : gameMode;
    }

    private static Difficulty difficultyFor(WorldMetadata metadata, SaveManager.LevelData levelData,
            Properties serverProperties, GameMode gameMode) {
        Difficulty difficulty = levelData != null ? levelData.getDifficulty()
                : metadata != null ? Difficulty.fromName(metadata.difficulty) : Difficulty.EASY;
        difficulty = serverPropertyDifficulty(serverProperties, difficulty);
        return serverPropertyBoolean(serverProperties, "hardcore", false) || gameMode == GameMode.HARDCORE
                ? Difficulty.HARD
                : difficulty;
    }

    private static String serverPropertyText(Properties properties, String key) {
        if (properties == null || key == null) {
            return null;
        }
        String value = properties.getProperty(key);
        if (value == null || value.isBlank()) {
            return null;
        }
        String cleaned = value.replace('\r', ' ').replace('\n', ' ').trim();
        return cleaned.isEmpty() ? null : cleaned;
    }

    private static long serverPropertySeed(Properties properties, long fallback) {
        String seedText = serverPropertyText(properties, "level-seed");
        if (seedText == null) {
            return fallback;
        }
        try {
            return Long.parseLong(seedText);
        } catch (NumberFormatException ignored) {
            return seedText.hashCode();
        }
    }

    private static GameMode serverPropertyGameMode(Properties properties, GameMode fallback) {
        String value = serverPropertyText(properties, "gamemode");
        if (value == null) {
            value = serverPropertyText(properties, "game-mode");
        }
        return value == null ? (fallback == null ? GameMode.SURVIVAL : fallback) : GameMode.fromName(value);
    }

    private static Difficulty serverPropertyDifficulty(Properties properties, Difficulty fallback) {
        String value = serverPropertyText(properties, "difficulty");
        return value == null ? (fallback == null ? Difficulty.EASY : fallback) : Difficulty.fromName(value);
    }

    private static boolean serverPropertyBoolean(Properties properties, String key, boolean fallback) {
        String value = serverPropertyText(properties, key);
        if (value == null) {
            return fallback;
        }
        return switch (value.toLowerCase(Locale.ROOT)) {
            case "true", "yes", "1", "on" -> true;
            case "false", "no", "0", "off" -> false;
            default -> fallback;
        };
    }

    private static String cleanDisplayName(String displayName) {
        String cleaned = displayName == null ? "" : displayName.trim();
        return cleaned.isEmpty() ? "New World" : cleaned;
    }

    private static String slug(String value) {
        String lower = value.toLowerCase(Locale.ROOT);
        StringBuilder builder = new StringBuilder();
        boolean previousDash = false;
        for (int i = 0; i < lower.length(); i++) {
            char c = lower.charAt(i);
            boolean allowed = c >= 'a' && c <= 'z' || c >= '0' && c <= '9';
            if (allowed) {
                builder.append(c);
                previousDash = false;
            } else if (!previousDash && builder.length() > 0) {
                builder.append('-');
                previousDash = true;
            }
        }
        while (builder.length() > 0 && builder.charAt(builder.length() - 1) == '-') {
            builder.deleteCharAt(builder.length() - 1);
        }
        return builder.length() == 0 ? "world" : builder.toString();
    }

    public static long parseSeed(String seedText) {
        if (seedText == null || seedText.isBlank()) {
            return System.currentTimeMillis();
        }
        try {
            return Long.parseLong(seedText.trim());
        } catch (NumberFormatException ignored) {
            return seedText.hashCode();
        }
    }

    private static String humanizeId(String id) {
        String[] parts = id.replace('_', '-').split("-");
        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            if (part.isBlank()) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append(' ');
            }
            builder.append(Character.toUpperCase(part.charAt(0)));
            if (part.length() > 1) {
                builder.append(part.substring(1));
            }
        }
        return builder.length() == 0 ? id : builder.toString();
    }

    private static Instant latestInstant(Path... paths) {
        Instant latest = Instant.EPOCH;
        for (Path path : paths) {
            if (path == null || !Files.exists(path)) {
                continue;
            }
            Instant instant = fileInstant(path);
            if (instant.isAfter(latest)) {
                latest = instant;
            }
        }
        return latest.equals(Instant.EPOCH) ? Instant.now() : latest;
    }

    private static Instant latestInstant(Instant... instants) {
        Instant latest = Instant.EPOCH;
        for (Instant instant : instants) {
            if (instant != null && instant.isAfter(latest)) {
                latest = instant;
            }
        }
        return latest.equals(Instant.EPOCH) ? Instant.now() : latest;
    }

    private static Instant fileInstantIfExists(Path path) {
        return path != null && Files.exists(path) ? fileInstant(path) : null;
    }

    private static Instant fileInstant(Path path) {
        try {
            return Files.getLastModifiedTime(path).toInstant();
        } catch (IOException ignored) {
            return Instant.now();
        }
    }

    private static Instant parseInstant(String value, Instant fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        try {
            return Instant.parse(value);
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private static Instant parseInstantOrNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Instant.parse(value);
        } catch (Exception ignored) {
            return null;
        }
    }

    private static Instant millisInstant(long epochMillis) {
        return epochMillis > 0L ? Instant.ofEpochMilli(epochMillis) : null;
    }

    public record WorldInfo(String id, String displayName, Path path, long seed, GameMode gameMode,
            Difficulty difficulty, boolean legacyDefault, boolean hasLevelData, boolean hasMetadata,
            Instant createdAt, Instant lastModified) {
        public boolean isLegacyDefault() {
            return legacyDefault;
        }

        public boolean hasLevelData() {
            return hasLevelData;
        }

        public boolean hasMetadata() {
            return hasMetadata;
        }

        public SaveManager createSaveManager() {
            return new SaveManager(path);
        }
    }

    private static final class WorldMetadata {
        String displayName;
        long seed;
        String gameMode;
        String difficulty;
        String createdAt;
        String lastPlayed;
    }
}
