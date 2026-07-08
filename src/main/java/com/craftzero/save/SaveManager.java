package com.craftzero.save;

import com.craftzero.entity.DroppedItem;
import com.craftzero.entity.ArrowEntity;
import com.craftzero.entity.BoatEntity;
import com.craftzero.entity.ChestMinecartEntity;
import com.craftzero.entity.EndCrystalEntity;
import com.craftzero.entity.EnderPearlEntity;
import com.craftzero.entity.Entity;
import com.craftzero.entity.EyeOfEnderEntity;
import com.craftzero.entity.ExperienceOrbEntity;
import com.craftzero.entity.FallingBlockEntity;
import com.craftzero.entity.FireballEntity;
import com.craftzero.entity.FishingHookEntity;
import com.craftzero.entity.FurnaceMinecartEntity;
import com.craftzero.entity.LivingEntity;
import com.craftzero.entity.MinecartEntity;
import com.craftzero.entity.PaintingEntity;
import com.craftzero.entity.PrimedTntEntity;
import com.craftzero.entity.SplashPotionEntity;
import com.craftzero.entity.ThrownItemEntity;
import com.craftzero.entity.ai.MeleeAttackGoal;
import com.craftzero.entity.ai.PanicGoal;
import com.craftzero.entity.ai.TargetNearestGoal;
import com.craftzero.entity.mob.Blaze;
import com.craftzero.entity.mob.CaveSpider;
import com.craftzero.entity.mob.Chicken;
import com.craftzero.entity.mob.Creeper;
import com.craftzero.entity.mob.EnderDragon;
import com.craftzero.entity.mob.Ghast;
import com.craftzero.entity.mob.Mob;
import com.craftzero.entity.mob.MobDefinition;
import com.craftzero.entity.mob.MobFactory;
import com.craftzero.entity.mob.MagmaCube;
import com.craftzero.entity.mob.Pig;
import com.craftzero.entity.mob.Sheep;
import com.craftzero.entity.mob.Skeleton;
import com.craftzero.entity.mob.Slime;
import com.craftzero.entity.mob.SnowGolem;
import com.craftzero.entity.mob.Spider;
import com.craftzero.entity.mob.Squid;
import com.craftzero.entity.mob.Villager;
import com.craftzero.entity.mob.Wolf;
import com.craftzero.entity.mob.Enderman;
import com.craftzero.entity.mob.ZombiePigman;
import com.craftzero.inventory.Inventory;
import com.craftzero.inventory.ItemStack;
import com.craftzero.inventory.ItemType;
import com.craftzero.inventory.MapItemData;
import com.craftzero.inventory.ToolType;
import com.craftzero.main.Difficulty;
import com.craftzero.main.GameMode;
import com.craftzero.main.GameSettings;
import com.craftzero.main.Player;
import com.craftzero.main.PlayerStats;
import com.craftzero.multiplayer.MultiplayerProtocol;
import com.craftzero.progression.ArmorMaterial;
import com.craftzero.progression.ArmorSlot;
import com.craftzero.progression.AchievementType;
import com.craftzero.progression.EnchantmentInstance;
import com.craftzero.progression.EnchantmentResolver;
import com.craftzero.progression.EnchantmentType;
import com.craftzero.progression.PotionData;
import com.craftzero.progression.StatusEffectInstance;
import com.craftzero.progression.StatusEffectType;
import com.craftzero.world.Chunk;
import com.craftzero.world.Block;
import com.craftzero.world.BlockShape;
import com.craftzero.world.BlockType;
import com.craftzero.world.DayCycleManager;
import com.craftzero.world.Dimension;
import com.craftzero.world.RedstoneEngine;
import com.craftzero.world.World;
import com.craftzero.world.WorldGenerator;
import com.craftzero.world.WorldGenerators;
import com.craftzero.world.tile.BlockPos;
import com.craftzero.world.tile.BrewingRecipeRegistry;
import com.craftzero.world.tile.ChestTileEntity;
import com.craftzero.world.tile.BrewingStandTileEntity;
import com.craftzero.world.tile.DispenserTileEntity;
import com.craftzero.world.tile.EnchantingTableTileEntity;
import com.craftzero.world.tile.FurnaceTileEntity;
import com.craftzero.world.tile.JukeboxTileEntity;
import com.craftzero.world.tile.MonsterSpawnerTileEntity;
import com.craftzero.world.tile.NoteBlockTileEntity;
import com.craftzero.world.tile.SignTileEntity;
import com.craftzero.world.tile.TileEntity;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

/**
 * Automatic single-world save/load support for saves/default.
 */
public class SaveManager {
    public static final int FORMAT_VERSION = 10;
    public static final int MIN_SUPPORTED_FORMAT_VERSION = 1;
    public static final String TARGET_VERSION = "Minecraft Java Release 1.0";
    public static final Path DEFAULT_WORLD_DIR = Paths.get("saves", "default");
    private static final Set<String> TILE_ENTITY_TYPE_IDS = Set.of(
            "chest",
            "furnace",
            "brewing_stand",
            "dispenser",
            "note_block",
            "jukebox",
            "enchanting_table",
            "sign",
            "mob_spawner");
    private static final Set<String> ENTITY_TYPE_IDS = Set.of(
            "EXPERIENCE_ORB",
            "ARROW",
            "FIREBALL",
            "ENDER_PEARL",
            "FISHING_HOOK",
            "THROWN_ITEM",
            "SPLASH_POTION",
            "EYE_OF_ENDER",
            "FALLING_BLOCK",
            "PRIMED_TNT",
            "BOAT",
            "PAINTING",
            "MINECART",
            "END_CRYSTAL");
    private static final Set<PotionData> VALID_POTION_DATA =
            Set.copyOf(BrewingRecipeRegistry.creativePotions());
    private static final int RELEASE_FISHING_MAX_WAIT_TICKS = 699;
    private static final int RELEASE_FISHING_MAX_CATCHABLE_TICKS = 39;
    private static final int CREEPER_MAX_FUSE_TICKS = 30;
    private static final int MAX_SAVED_LOOTING_LEVEL = 10;
    private static final float TAMED_WOLF_MAX_HEALTH = 20.0f;
    private static final String RELEASE_PLAYER_DIR = "players";
    private static final String OPS_FILE = "ops.txt";
    private static final String BANNED_PLAYERS_FILE = "banned-players.txt";
    private static final String BANNED_IPS_FILE = "banned-ips.txt";
    private static final String WHITELIST_FILE = "white-list.txt";
    private static final String SERVER_PROPERTIES_FILE = "server.properties";
    private static final int DEFAULT_SERVER_SPAWN_PROTECTION = 16;
    private static final int DEFAULT_SERVER_VIEW_DISTANCE = 10;
    private static final int DEFAULT_SERVER_MAX_BUILD_HEIGHT = MultiplayerProtocol.DEFAULT_MAX_BUILD_HEIGHT;
    private static final String MAP_INITIALIZED_KEY = "map.initialized";
    private static final String MAP_ID_KEY = "map.id";
    private static final String MAP_SCALE_KEY = "map.scale";
    private static final String MAP_CENTER_X_KEY = "map.centerX";
    private static final String MAP_CENTER_Z_KEY = "map.centerZ";
    private static final String MAP_DIMENSION_KEY = "map.dimension";
    private static final String MAP_COLOR_FORMAT_KEY = "map.colorFormat";
    private static final String MAP_COLOR_FORMAT_SHADED = "release-shaded";
    private static final String MAP_ID_PREFIX = "map_";

    private final Path worldDir;
    private final Path chunksDir;
    private final Path levelPath;
    private final Path releaseLevelPath;
    private final Path releaseLevelOldPath;
    private final Path releaseSessionLockPath;
    private final Path opsPath;
    private final Path bannedPlayersPath;
    private final Path bannedIpsPath;
    private final Path whitelistPath;
    private final Path serverPropertiesPath;
    private final long releaseSessionLockTimestamp;
    private final Gson gson;
    private String levelName = "Default World";
    private String serverMotd = "CraftZero";
    private String serverIp = "";
    private int serverPort = MultiplayerProtocol.DEFAULT_PORT;
    private int serverMaxPlayers = MultiplayerProtocol.DEFAULT_MAX_PLAYERS;
    private boolean serverPvp = true;
    private boolean serverSpawnAnimals = true;
    private boolean serverSpawnMonsters = true;
    private boolean serverSpawnNpcs = true;
    private boolean serverAllowNether = true;
    private boolean serverOnlineMode;
    private boolean serverAllowFlight;
    private boolean serverEnableQuery;
    private int serverQueryPort = MultiplayerProtocol.DEFAULT_QUERY_PORT;
    private int serverSpawnProtection = DEFAULT_SERVER_SPAWN_PROTECTION;
    private int serverViewDistance = DEFAULT_SERVER_VIEW_DISTANCE;
    private int serverMaxBuildHeight = DEFAULT_SERVER_MAX_BUILD_HEIGHT;
    private String serverLevelSeed = "";
    private boolean generateStructures = true;
    private GameMode gameMode = GameMode.SURVIVAL;
    private Difficulty difficulty = Difficulty.EASY;
    private boolean hardcore;
    private boolean allowCheats;
    private int spawnX;
    private int spawnY = 80;
    private int spawnZ;
    private String weatherState = "clear";
    private Set<String> operators = Set.of();
    private Set<String> bannedPlayers = Set.of();
    private Set<String> bannedIps = Set.of();
    private Set<String> whitelist = Set.of();
    private boolean whitelistEnabled;
    private final Map<String, byte[]> globalFilledMapColors = new HashMap<>();
    private int globalFilledMapNextId;
    private final Map<Dimension, DimensionRuntimeData> dimensionRuntimeCache = new HashMap<>();
    private boolean releaseSessionLockClaimed;

    public SaveManager(Path worldDir) {
        this.worldDir = worldDir;
        this.chunksDir = worldDir.resolve("chunks");
        this.levelPath = worldDir.resolve("level.json");
        this.releaseLevelPath = worldDir.resolve(ReleaseLevelDat.FILE_NAME);
        this.releaseLevelOldPath = worldDir.resolve(ReleaseLevelDat.OLD_FILE_NAME);
        this.releaseSessionLockPath = worldDir.resolve(ReleaseLevelDat.SESSION_LOCK_FILE_NAME);
        this.opsPath = worldDir.resolve(OPS_FILE);
        this.bannedPlayersPath = worldDir.resolve(BANNED_PLAYERS_FILE);
        this.bannedIpsPath = worldDir.resolve(BANNED_IPS_FILE);
        this.whitelistPath = worldDir.resolve(WHITELIST_FILE);
        this.serverPropertiesPath = worldDir.resolve(SERVER_PROPERTIES_FILE);
        this.releaseSessionLockTimestamp = System.currentTimeMillis();
        this.gson = new GsonBuilder().setPrettyPrinting().create();
    }

    public boolean hasSave() {
        return Files.exists(levelPath)
                || Files.exists(SafeFiles.backupPath(levelPath))
                || Files.exists(releaseLevelPath)
                || Files.exists(releaseLevelOldPath)
                || Files.exists(serverPropertiesPath);
    }

    public SaveLoadResult loadLevel() {
        if (!Files.exists(levelPath)) {
            SaveLoadResult releaseResult = loadReleaseLevelDatIfExists();
            if (releaseResult.status == SaveLoadStatus.LOADED) {
                return releaseResult;
            }
            Path backup = SafeFiles.backupPath(levelPath);
            if (Files.isRegularFile(backup)) {
                SaveLoadResult backupResult = loadLevelFrom(backup);
                if (backupResult.status == SaveLoadStatus.LOADED) {
                    return backupResult;
                }
            }
            if (releaseResult.status != SaveLoadStatus.MISSING) {
                return releaseResult;
            }
            return SaveLoadResult.missing();
        }

        SaveLoadResult primary = loadLevelFrom(levelPath);
        if (primary.status == SaveLoadStatus.LOADED) {
            return primary;
        }

        Path backup = SafeFiles.backupPath(levelPath);
        if (Files.isRegularFile(backup)) {
            SaveLoadResult backupResult = loadLevelFrom(backup);
            if (backupResult.status == SaveLoadStatus.LOADED) {
                return backupResult;
            }
        }
        SaveLoadResult releaseResult = loadReleaseLevelDatIfExists();
        if (releaseResult.status == SaveLoadStatus.LOADED) {
            return releaseResult;
        }
        return primary;
    }

    private SaveLoadResult loadLevelFrom(Path path) {
        try (Reader reader = Files.newBufferedReader(path)) {
            LevelData data = gson.fromJson(reader, LevelData.class);
            if (data == null) {
                return SaveLoadResult.corrupt("Level save is empty.");
            }
            if (data.formatVersion < MIN_SUPPORTED_FORMAT_VERSION || data.formatVersion > FORMAT_VERSION) {
                return SaveLoadResult.unsupported(
                        "Unsupported level format " + data.formatVersion + " at " + path);
            }
            return finishLoadedLevelData(data, path.toString());
        } catch (Exception e) {
            return SaveLoadResult.corrupt("Failed to load level save: " + e.getMessage());
        }
    }

    private SaveLoadResult loadReleaseLevelDatIfExists() {
        if (Files.isRegularFile(releaseLevelPath)) {
            SaveLoadResult primary = loadReleaseLevelDatFrom(releaseLevelPath);
            if (primary.status == SaveLoadStatus.LOADED || !Files.isRegularFile(releaseLevelOldPath)) {
                return primary;
            }
            SaveLoadResult old = loadReleaseLevelDatFrom(releaseLevelOldPath);
            return old.status == SaveLoadStatus.LOADED ? old : primary;
        }
        if (Files.isRegularFile(releaseLevelOldPath)) {
            return loadReleaseLevelDatFrom(releaseLevelOldPath);
        }
        return SaveLoadResult.missing();
    }

    private SaveLoadResult loadReleaseLevelDatFrom(Path path) {
        try {
            LevelData data = ReleaseLevelDat.read(path);
            mergeReleasePlayerFileIfExists(data);
            ReleaseMapData.ImportResult mapData = loadReleaseMapDataIfExists();
            if (!mapData.isEmpty()) {
                data.filledMaps = mapData.filledMaps();
            }
            data.nextFilledMapId = Math.max(data.nextFilledMapId, mapData.nextMapId());
            mergeReleaseRegionRuntime(data, Dimension.fromSaveName(data.dimension));
            applyReleaseMapStates(data, mapData.states());
            return finishLoadedLevelData(data, path.toString());
        } catch (Exception e) {
            return SaveLoadResult.corrupt("Failed to load Release " + path.getFileName() + ": " + e.getMessage());
        }
    }

    private SaveLoadResult finishLoadedLevelData(LevelData data, String source) {
        if (data == null) {
            return SaveLoadResult.corrupt("Level save is empty.");
        }
        normalizeLevelData(data);
        mergeServerProperties(data);
        mergeAdminSidecars(data);
        String validationError = validateLevelData(data);
        if (validationError != null) {
            return SaveLoadResult.corrupt("Invalid level save at " + source + ": " + validationError);
        }
        replaceGlobalFilledMapColors(FilledMapData.toMap(data.filledMaps));
        reserveGlobalFilledMapIdsUpTo(data.nextFilledMapId);
        primeDimensionRuntimeCache(data);
        setLevelMetadata(data.levelName, data.getGameMode(), data.getDifficulty(), data.hardcore, data.allowCheats);
        setServerProperties(data.serverMotd, data.getServerIp(), data.getServerPort(), data.serverMaxPlayers,
                data.isServerPvp(), data.isServerSpawnAnimals(), data.isServerSpawnMonsters(), data.isServerSpawnNpcs(),
                data.isServerAllowNether(), data.isServerOnlineMode(), data.isServerAllowFlight(),
                data.getServerSpawnProtection(), data.getServerViewDistance(), data.getServerMaxBuildHeight());
        setWorldGenerationMetadata(data.getServerLevelSeed(), data.shouldGenerateStructures());
        setServerQueryProperties(data.isServerEnableQuery(), data.getServerQueryPort());
        setWorldStateMetadata(data.spawnX, data.spawnY, data.spawnZ, data.weatherState);
        setAdminState(data.operators, data.bannedPlayers, data.bannedIps, data.whitelist, data.whitelistEnabled);
        return SaveLoadResult.loaded(data);
    }

    public LevelData loadLevelIfExists() {
        SaveLoadResult result = loadLevel();
        return result.status == SaveLoadStatus.LOADED ? result.levelData : null;
    }

    public LevelData createServerPropertiesBootstrap(String fallbackLevelName, long fallbackSeed,
            GameMode fallbackGameMode, Difficulty fallbackDifficulty) {
        if (!Files.isRegularFile(serverPropertiesPath)) {
            return null;
        }
        LevelData data = new LevelData();
        data.formatVersion = FORMAT_VERSION;
        data.targetVersion = TARGET_VERSION;
        data.levelName = fallbackLevelName == null || fallbackLevelName.isBlank()
                ? "Default World"
                : fallbackLevelName.trim();
        data.serverMotd = data.levelName;
        data.serverMaxPlayers = MultiplayerProtocol.DEFAULT_MAX_PLAYERS;
        data.serverPvp = true;
        data.serverSpawnAnimals = true;
        data.serverSpawnMonsters = true;
        data.serverSpawnNpcs = true;
        data.serverAllowNether = true;
        data.serverOnlineMode = false;
        data.serverAllowFlight = false;
        data.serverEnableQuery = false;
        data.serverQueryPort = MultiplayerProtocol.DEFAULT_QUERY_PORT;
        data.serverSpawnProtection = DEFAULT_SERVER_SPAWN_PROTECTION;
        data.serverViewDistance = DEFAULT_SERVER_VIEW_DISTANCE;
        data.serverMaxBuildHeight = DEFAULT_SERVER_MAX_BUILD_HEIGHT;
        data.serverLevelSeed = "";
        data.generateStructures = true;
        data.lastPlayed = System.currentTimeMillis();
        data.gameMode = (fallbackGameMode == null ? GameMode.SURVIVAL : fallbackGameMode).name();
        data.difficulty = (fallbackDifficulty == null ? Difficulty.EASY : fallbackDifficulty).name();
        data.seed = fallbackSeed;
        data.generatorId = WorldGenerator.RELEASE_ONE;
        data.dimension = Dimension.OVERWORLD.getSaveName();
        data.spawnY = 80;
        data.weatherState = "clear";
        data.operators = new ArrayList<>();
        data.bannedPlayers = new ArrayList<>();
        data.bannedIps = new ArrayList<>();
        data.whitelist = new ArrayList<>();
        data.filledMaps = new ArrayList<>();
        normalizeLevelData(data);
        mergeServerProperties(data);
        normalizeLevelData(data);
        return data;
    }

    public void save(World world, Player player, DayCycleManager dayCycle) throws IOException {
        SaveSnapshot snapshot = createSnapshot(world, player, dayCycle);
        writeSnapshot(snapshot);
        clearSnapshotModifiedFlags(world, snapshot);
    }

    public void setLevelMetadata(String levelName, GameMode gameMode, Difficulty difficulty, boolean hardcore) {
        setLevelMetadata(levelName, gameMode, difficulty, hardcore, false);
    }

    public void setLevelMetadata(String levelName, GameMode gameMode, Difficulty difficulty, boolean hardcore,
            boolean allowCheats) {
        this.levelName = levelName == null || levelName.isBlank() ? "Default World" : levelName;
        this.gameMode = gameMode == null ? GameMode.SURVIVAL : gameMode;
        this.hardcore = hardcore || this.gameMode == GameMode.HARDCORE;
        this.difficulty = this.hardcore ? Difficulty.HARD : (difficulty == null ? Difficulty.EASY : difficulty);
        this.allowCheats = allowCheats;
    }

    public void setServerProperties(String serverMotd, int serverMaxPlayers) {
        setServerProperties(serverMotd, "", MultiplayerProtocol.DEFAULT_PORT, serverMaxPlayers,
                true, true, true, true, true, false, false,
                DEFAULT_SERVER_SPAWN_PROTECTION, DEFAULT_SERVER_VIEW_DISTANCE, DEFAULT_SERVER_MAX_BUILD_HEIGHT);
    }

    public void setServerProperties(String serverMotd, int serverPort, int serverMaxPlayers,
            boolean serverPvp, boolean serverSpawnAnimals, boolean serverSpawnMonsters, boolean serverAllowNether,
            boolean serverOnlineMode, boolean serverAllowFlight, int serverSpawnProtection, int serverViewDistance) {
        setServerProperties(serverMotd, "", serverPort, serverMaxPlayers, serverPvp, serverSpawnAnimals,
                serverSpawnMonsters, true, serverAllowNether, serverOnlineMode, serverAllowFlight,
                serverSpawnProtection, serverViewDistance, DEFAULT_SERVER_MAX_BUILD_HEIGHT);
    }

    public void setServerProperties(String serverMotd, String serverIp, int serverPort, int serverMaxPlayers,
            boolean serverPvp, boolean serverSpawnAnimals, boolean serverSpawnMonsters, boolean serverAllowNether,
            boolean serverOnlineMode, boolean serverAllowFlight, int serverSpawnProtection, int serverViewDistance) {
        setServerProperties(serverMotd, serverIp, serverPort, serverMaxPlayers, serverPvp, serverSpawnAnimals,
                serverSpawnMonsters, true, serverAllowNether, serverOnlineMode, serverAllowFlight,
                serverSpawnProtection, serverViewDistance, DEFAULT_SERVER_MAX_BUILD_HEIGHT);
    }

    public void setServerProperties(String serverMotd, String serverIp, int serverPort, int serverMaxPlayers,
            boolean serverPvp, boolean serverSpawnAnimals, boolean serverSpawnMonsters, boolean serverSpawnNpcs,
            boolean serverAllowNether, boolean serverOnlineMode, boolean serverAllowFlight,
            int serverSpawnProtection, int serverViewDistance) {
        setServerProperties(serverMotd, serverIp, serverPort, serverMaxPlayers, serverPvp, serverSpawnAnimals,
                serverSpawnMonsters, serverSpawnNpcs, serverAllowNether, serverOnlineMode, serverAllowFlight,
                serverSpawnProtection, serverViewDistance, DEFAULT_SERVER_MAX_BUILD_HEIGHT);
    }

    public void setServerProperties(String serverMotd, String serverIp, int serverPort, int serverMaxPlayers,
            boolean serverPvp, boolean serverSpawnAnimals, boolean serverSpawnMonsters, boolean serverSpawnNpcs,
            boolean serverAllowNether, boolean serverOnlineMode, boolean serverAllowFlight,
            int serverSpawnProtection, int serverViewDistance, int serverMaxBuildHeight) {
        this.serverMotd = sanitizeServerPropertyText(serverMotd,
                levelName == null || levelName.isBlank() ? "CraftZero" : levelName);
        this.serverIp = sanitizeServerIp(serverIp);
        this.serverPort = clampServerPort(serverPort);
        this.serverMaxPlayers = Math.max(1, serverMaxPlayers);
        this.serverPvp = serverPvp;
        this.serverSpawnAnimals = serverSpawnAnimals;
        this.serverSpawnMonsters = serverSpawnMonsters;
        this.serverSpawnNpcs = serverSpawnNpcs;
        this.serverAllowNether = serverAllowNether;
        this.serverOnlineMode = serverOnlineMode;
        this.serverAllowFlight = serverAllowFlight;
        this.serverSpawnProtection = Math.max(0, serverSpawnProtection);
        this.serverViewDistance = Math.max(3, Math.min(15, serverViewDistance));
        this.serverMaxBuildHeight = clampServerMaxBuildHeight(serverMaxBuildHeight);
    }

    public void setWorldGenerationMetadata(String serverLevelSeed, boolean generateStructures) {
        this.serverLevelSeed = sanitizeServerLevelSeed(serverLevelSeed);
        this.generateStructures = generateStructures;
    }

    public void setServerQueryProperties(boolean serverEnableQuery, int serverQueryPort) {
        this.serverEnableQuery = serverEnableQuery;
        this.serverQueryPort = clampServerPort(serverQueryPort);
    }

    public void setWorldStateMetadata(int spawnX, int spawnY, int spawnZ, String weatherState) {
        this.spawnX = spawnX;
        this.spawnY = spawnY;
        this.spawnZ = spawnZ;
        this.weatherState = World.normalizeWeatherState(weatherState);
    }

    public void setAdminState(Collection<String> operators, Collection<String> bannedPlayers,
            Collection<String> bannedIps, Collection<String> whitelist, boolean whitelistEnabled) {
        this.operators = normalizedSet(operators);
        this.bannedPlayers = normalizedSet(bannedPlayers);
        this.bannedIps = normalizedSet(bannedIps);
        this.whitelist = normalizedSet(whitelist);
        this.whitelistEnabled = whitelistEnabled;
    }

    public void applyGlobalWorldData(World world) {
        if (world == null) {
            return;
        }
        world.replaceFilledMapColors(globalFilledMapColorsSnapshot());
        world.reserveFilledMapIdsUpTo(globalFilledMapNextIdSnapshot());
    }

    public SaveSnapshot createSnapshot(World world, Player player, DayCycleManager dayCycle) {
        return createSnapshot(createLevelData(world, player, dayCycle), world.getLoadedChunks());
    }

    public LevelData createLevelDataSnapshot(World world, Player player, DayCycleManager dayCycle) {
        return createLevelData(world, player, dayCycle);
    }

    public SaveSnapshot createSnapshot(LevelData levelData, Collection<Chunk> loadedChunks) {
        List<ChunkSaveData> chunks = new ArrayList<>();
        if (loadedChunks != null) {
            for (Chunk chunk : loadedChunks) {
                if (chunk == null) {
                    continue;
                }
                boolean runtimePayload = hasRuntimeDataInChunk(levelData, chunk.getChunkX(), chunk.getChunkZ());
                if (!chunk.isModified() && !runtimePayload) {
                    continue;
                }
                chunk.calculateSkyLight();
                short[] blockIds = chunk.copyBlockIds();
                byte[] metadata = chunk.copyBlockMetadata();
                clearSnapshotSleepingBedOccupancy(levelData, chunk, blockIds, metadata);
                chunks.add(new ChunkSaveData(
                        chunk.getChunkX(),
                        chunk.getChunkZ(),
                        blockIds,
                        metadata,
                        chunk.copySkyLight(),
                        chunk.copyBlockLight(),
                        chunk.copyHeightMap(),
                        chunk.getModificationVersion()));
            }
        }
        return new SaveSnapshot(levelData, chunks);
    }

    public void writeSnapshot(SaveSnapshot snapshot) throws IOException {
        Files.createDirectories(worldDir);
        claimOrVerifyReleaseSessionLock();
        mergeGlobalFilledMapColors(FilledMapData.toMap(snapshot.levelData().filledMaps));
        snapshot.levelData().filledMaps = filledMapDataSnapshot();
        snapshot.levelData().nextFilledMapId = Math.max(snapshot.levelData().nextFilledMapId,
                globalFilledMapNextIdSnapshot());
        reserveGlobalFilledMapIdsUpTo(snapshot.levelData().nextFilledMapId);
        Dimension dimension = Dimension.fromSaveName(snapshot.levelData().dimension);
        DimensionRuntimeData snapshotRuntime = DimensionRuntimeData.from(snapshot.levelData());
        rememberDimensionRuntime(dimension, snapshotRuntime);
        Files.createDirectories(chunkDirFor(dimension));

        for (ChunkSaveData chunk : snapshot.chunks()) {
            ChunkCodec.write(chunkPath(dimension, chunk.chunkX(), chunk.chunkZ()), chunk.blockIds(), chunk.metadata(),
                    chunk.skyLight(), chunk.blockLight(), chunk.heightMap());
            ReleaseRegionFile.writeChunk(worldDir, dimension, chunk.chunkX(), chunk.chunkZ(),
                    new ChunkCodec.ChunkData(chunk.blockIds(), chunk.metadata(),
                            chunk.skyLight(), chunk.blockLight(), chunk.heightMap()),
                    snapshotRuntime);
        }

        for (Map.Entry<Dimension, DimensionRuntimeData> entry : dimensionRuntimeSnapshot().entrySet()) {
            writeDimensionRuntime(entry.getKey(), entry.getValue());
        }

        SafeFiles.writeAtomic(levelPath, writer -> gson.toJson(snapshot.levelData(), writer),
                SafeFiles.BackupPolicy.BAK);
        writeServerProperties(snapshot.levelData());
        writeAdminSidecars(snapshot.levelData());
        claimOrVerifyReleaseSessionLock();
        ReleaseLevelDat.writePlayer(releasePlayerPath(), snapshot.levelData());
        ReleaseMapData.writeAll(worldDir, snapshot.levelData().filledMaps,
                collectReleaseMapStates(snapshot.levelData()), snapshot.levelData().nextFilledMapId);
        ReleaseLevelDat.write(releaseLevelPath, snapshot.levelData(), calculateSaveDirectorySize());
    }

    private synchronized void claimOrVerifyReleaseSessionLock() throws IOException {
        Long lockedTimestamp = ReleaseLevelDat.readSessionLock(releaseSessionLockPath);
        if (releaseSessionLockClaimed && lockedTimestamp != null
                && lockedTimestamp.longValue() != releaseSessionLockTimestamp) {
            throw new IOException("Release save session changed while saving " + worldDir);
        }
        ReleaseLevelDat.writeSessionLock(releaseSessionLockPath, releaseSessionLockTimestamp);
        releaseSessionLockClaimed = true;
    }

    private long calculateSaveDirectorySize() throws IOException {
        if (!Files.exists(worldDir)) {
            return 0L;
        }
        long total = 0L;
        try (java.util.stream.Stream<Path> paths = Files.walk(worldDir)) {
            for (Path path : (Iterable<Path>) paths::iterator) {
                if (!Files.isRegularFile(path)) {
                    continue;
                }
                long size = Files.size(path);
                if (Long.MAX_VALUE - total < size) {
                    return Long.MAX_VALUE;
                }
                total += size;
            }
        }
        return total;
    }

    public void clearSnapshotModifiedFlags(World world, SaveSnapshot snapshot) {
        if (world == null || snapshot == null) {
            return;
        }
        for (ChunkSaveData saved : snapshot.chunks()) {
            Chunk chunk = world.getLoadedChunk(saved.chunkX(), saved.chunkZ());
            if (chunk != null) {
                chunk.clearModifiedIfVersion(saved.modificationVersion());
            }
        }
    }

    private void clearSnapshotSleepingBedOccupancy(LevelData levelData, Chunk chunk,
            short[] blockIds, byte[] metadata) {
        if (levelData == null || levelData.player == null
                || !levelData.player.hasCompleteSleepingState()
                || chunk == null || blockIds == null || metadata == null) {
            return;
        }
        clearSnapshotBedOccupancyAt(levelData.player.sleepingBedFootX, levelData.player.sleepingBedFootY,
                levelData.player.sleepingBedFootZ, chunk, blockIds, metadata);
        clearSnapshotBedOccupancyAt(levelData.player.sleepingBedHeadX, levelData.player.sleepingBedHeadY,
                levelData.player.sleepingBedHeadZ, chunk, blockIds, metadata);
    }

    private void clearSnapshotBedOccupancyAt(int worldX, int y, int worldZ, Chunk chunk,
            short[] blockIds, byte[] metadata) {
        if (y < 0 || y >= Chunk.HEIGHT) {
            return;
        }
        int chunkX = Math.floorDiv(worldX, Chunk.WIDTH);
        int chunkZ = Math.floorDiv(worldZ, Chunk.DEPTH);
        if (chunk.getChunkX() != chunkX || chunk.getChunkZ() != chunkZ) {
            return;
        }
        int localX = Math.floorMod(worldX, Chunk.WIDTH);
        int localZ = Math.floorMod(worldZ, Chunk.DEPTH);
        int index = Chunk.getIndex(localX, y, localZ);
        if (index < 0 || index >= blockIds.length || index >= metadata.length) {
            return;
        }
        if (BlockType.fromId(blockIds[index]) == BlockType.BED) {
            metadata[index] = (byte) ((metadata[index] & 0xFF) & ~World.BED_OCCUPIED_BIT);
        }
    }

    public void saveModifiedChunk(Chunk chunk) throws IOException {
        saveModifiedChunk(chunk, Dimension.OVERWORLD);
    }

    public void saveModifiedChunk(Chunk chunk, Dimension dimension) throws IOException {
        if (chunk == null || !chunk.isModified()) {
            return;
        }
        long version = chunk.getModificationVersion();
        chunk.calculateSkyLight();
        short[] blockIds = chunk.copyBlockIds();
        byte[] metadata = chunk.copyBlockMetadata();
        byte[] skyLight = chunk.copySkyLight();
        byte[] blockLight = chunk.copyBlockLight();
        int[] heightMap = chunk.copyHeightMap();
        ChunkCodec.write(chunkPath(dimension, chunk.getChunkX(), chunk.getChunkZ()),
                blockIds, metadata, skyLight, blockLight, heightMap);
        claimOrVerifyReleaseSessionLock();
        ReleaseRegionFile.writeChunk(worldDir, dimension, chunk.getChunkX(), chunk.getChunkZ(),
                new ChunkCodec.ChunkData(blockIds, metadata, skyLight, blockLight, heightMap));
        chunk.clearModifiedIfVersion(version);
    }

    public void saveModifiedChunkData(Dimension dimension, int chunkX, int chunkZ,
            short[] blockIds, byte[] metadata) throws IOException {
        saveModifiedChunkData(dimension, chunkX, chunkZ, blockIds, metadata, null, null, null);
    }

    public void saveModifiedChunkData(Dimension dimension, int chunkX, int chunkZ,
            short[] blockIds, byte[] metadata, byte[] skyLight, byte[] blockLight, int[] heightMap) throws IOException {
        saveModifiedChunkData(dimension, chunkX, chunkZ, blockIds, metadata, skyLight, blockLight, heightMap, null);
    }

    public void saveModifiedChunkData(Dimension dimension, int chunkX, int chunkZ,
            short[] blockIds, byte[] metadata, byte[] skyLight, byte[] blockLight, int[] heightMap,
            DimensionRuntimeData runtimeData) throws IOException {
        if (blockIds == null || metadata == null) {
            return;
        }
        Dimension normalized = dimension == null ? Dimension.OVERWORLD : dimension;
        Files.createDirectories(chunkDirFor(normalized));
        ChunkCodec.ChunkData chunkData = new ChunkCodec.ChunkData(blockIds, metadata, skyLight, blockLight, heightMap);
        ChunkCodec.write(chunkPath(normalized, chunkX, chunkZ), blockIds, metadata, skyLight, blockLight, heightMap);
        claimOrVerifyReleaseSessionLock();
        ReleaseRegionFile.writeChunk(worldDir, normalized, chunkX, chunkZ, chunkData,
                copyDimensionRuntimeData(normalized, runtimeData));
        DimensionRuntimeData mergedRuntime = mergeChunkRuntimeData(normalized, chunkX, chunkZ, runtimeData);
        if (mergedRuntime != null) {
            writeDimensionRuntime(normalized, mergedRuntime);
        }
    }

    public boolean loadChunkIfExists(Chunk chunk) {
        return loadChunkIfExists(chunk, Dimension.OVERWORLD);
    }

    public boolean loadChunkIfExists(Chunk chunk, Dimension dimension) {
        Path path = chunkPath(dimension, chunk.getChunkX(), chunk.getChunkZ());
        if (!Files.exists(path)) {
            return loadReleaseRegionChunkIfExists(chunk, dimension);
        }

        try {
            ChunkCodec.ChunkData data = ChunkCodec.read(path);
            loadChunkData(chunk, data);
            return true;
        } catch (Exception e) {
            System.err.println("Failed to load chunk " + chunk.getChunkX() + "," + chunk.getChunkZ()
                    + ": " + e.getMessage());
            Path backup = SafeFiles.backupPath(path);
            if (!Files.isRegularFile(backup)) {
                return loadReleaseRegionChunkIfExists(chunk, dimension);
            }
            try {
                ChunkCodec.ChunkData data = ChunkCodec.read(backup);
                loadChunkData(chunk, data);
                return true;
            } catch (Exception backupError) {
                System.err.println("Failed to load backup chunk " + chunk.getChunkX() + "," + chunk.getChunkZ()
                        + ": " + backupError.getMessage());
                return loadReleaseRegionChunkIfExists(chunk, dimension);
            }
        }
    }

    private boolean loadReleaseRegionChunkIfExists(Chunk chunk, Dimension dimension) {
        try {
            ChunkCodec.ChunkData data = ReleaseRegionFile.readChunk(worldDir, dimension,
                    chunk.getChunkX(), chunk.getChunkZ());
            if (data == null) {
                return false;
            }
            loadChunkData(chunk, data);
            return true;
        } catch (Exception regionError) {
            System.err.println("Failed to load Release region chunk " + chunk.getChunkX() + ","
                    + chunk.getChunkZ() + ": " + regionError.getMessage());
            return false;
        }
    }

    private static void loadChunkData(Chunk chunk, ChunkCodec.ChunkData data) {
        if (data.hasLightingData()) {
            chunk.loadBlockData(data.blockIds(), data.metadata(), data.skyLight(), data.blockLight(),
                    data.heightMap(), false);
            chunk.markLoadedFromStorage();
            return;
        }
        chunk.loadBlockData(data.blockIds(), data.metadata(), false);
        chunk.markLoadedFromStorage();
    }

    public void applyDimensionRuntime(World world, Player player) {
        applyDimensionRuntime(world, player, null);
    }

    public void applyDimensionRuntime(World world, Player player, DayCycleManager dayCycle) {
        if (world == null || player == null) {
            return;
        }
        applyGlobalWorldData(world);
        DimensionRuntimeData runtimeData = dimensionRuntimeFor(world.getDimension());
        if (runtimeData == null) {
            return;
        }
        applyRuntimeWorldState(runtimeData, dayCycle, world);
        applyRuntimeData(runtimeData, player, world, null);
        world.reconcileLoadedTileEntities();
    }

    public void applyLevel(LevelData data, Player player, DayCycleManager dayCycle, World world) {
        if (data == null) {
            return;
        }
        world.setPlayer(player);

        if (data.worldTime != null && data.worldTime >= 0) {
            dayCycle.setWorldTime(data.worldTime);
        } else if (data.time >= 0) {
            dayCycle.setTime(data.time);
        }
        world.setWeatherState(data.weatherState, data.weatherRainTime, data.weatherThunderTime);
        replaceGlobalFilledMapColors(FilledMapData.toMap(data.filledMaps));
        applyGlobalWorldData(world);

        if (data.player != null) {
            player.setPosition(data.player.x, data.player.y, data.player.z);
            player.getCamera().setYaw(data.player.yaw);
            player.getCamera().setPitch(data.player.pitch);
            if (data.player.hasCompleteMovementState()) {
                player.restoreMovementState(data.player.motionX, data.player.motionY, data.player.motionZ,
                        data.player.onGround, data.player.fallStartY, data.player.wasFalling);
            }

            PlayerStats stats = player.getStats();
            stats.restore(data.player.health, data.player.hunger, data.player.saturation, data.player.air,
                    data.player.exhaustion);
            stats.restoreRuntimeState(data.player.regenTimer, data.player.peacefulRegenTimer,
                    data.player.starvationTimer, data.player.drownTimer, data.player.airTickAccumulator,
                    data.player.invincibilityTimer, data.player.hurtInvulnerabilityTimer,
                    data.player.lastDamageAmount);
            if (data.player.foodTickTimer > 0
                    && data.player.regenTimer <= 0.0f
                    && data.player.starvationTimer <= 0.0f) {
                stats.restoreFoodTickTimer(data.player.foodTickTimer);
            }
            stats.getProgression().restore(data.player.totalExperience, data.player.score);
            stats.getAchievements().restoreUnlocked(data.player.achievements);
            stats.getStatistics().restore(data.player.statPlayTimeTicks, data.player.statDistanceWalkedCm,
                    data.player.statJumps, data.player.statBlocksMined, data.player.statSuccessfulAttacks,
                    data.player.statDamageDealtTenths, data.player.statDamageTakenTenths, data.player.statDeaths,
                    data.player.statMobKills, data.player.statMonsterKills,
                    data.player.statItemsPickedUp, data.player.statItemsCrafted,
                    data.player.statItemsUsed, data.player.statItemsDepleted,
                    data.player.statBlocksMinedByType, data.player.statItemsPickedUpByType,
                    data.player.statItemsCraftedByType, data.player.statItemsUsedByType,
                    data.player.statItemsDepletedByType);
            stats.getStatistics().restoreGamesQuit(data.player.statGamesQuit);
            stats.getStatistics().restoreSessionCounters(data.player.statTimesPlayed, data.player.statWorldsLoaded,
                    data.player.statMultiplayerJoins, data.player.statWorldsSaved);
            stats.getStatistics().restoreFishCaught(data.player.statFishCaught);
            stats.getStatistics().restorePlayerKills(data.player.statPlayerKills);
            stats.getStatistics().restoreItemsDropped(data.player.statItemsDropped);
            stats.getStatistics().restoreItemsDroppedByType(data.player.statItemsDroppedByType);
            stats.getStatistics().restoreTravelDistances(data.player.statDistanceSwumCm,
                    data.player.statDistanceFallenCm, data.player.statDistanceClimbedCm,
                    data.player.statDistanceFlownCm, data.player.statDistanceDoveCm,
                    data.player.statDistanceByMinecartCm, data.player.statDistanceByBoatCm,
                    data.player.statDistanceByPigCm);
            stats.setActiveEffects(data.player.activeEffects);
            player.restoreDeathState(data.player.deathTime, data.player.hurtFlashTimer);
            player.setFireTicks(data.player.fireTicks);
            if (data.player.spawnY > 0.0f) {
                player.setSpawnPosition(data.player.spawnX, data.player.spawnY, data.player.spawnZ);
            } else {
                player.setSpawnPosition(data.spawnX + 0.5f, data.spawnY, data.spawnZ + 0.5f);
            }
            if (data.player.bedSpawnSet) {
                player.setBedSpawnPosition(new BlockPos(data.player.bedSpawnX, data.player.bedSpawnY,
                        data.player.bedSpawnZ),
                        data.player.spawnX, data.player.spawnY, data.player.spawnZ);
            }
            restoreSavedSleepingState(data.player, player, world);
        }

        if (data.inventory != null) {
            restoreInventory(player.getInventory(), data.inventory);
        }

        DimensionRuntimeData runtimeData = dimensionRuntimeFor(world.getDimension());
        if (runtimeData == null) {
            runtimeData = DimensionRuntimeData.from(data);
            rememberDimensionRuntime(world.getDimension(), runtimeData);
        }
        applyRuntimeData(runtimeData != null ? runtimeData : DimensionRuntimeData.from(data), player, world,
                data.player);

        world.reconcileLoadedTileEntities();
    }

    private void applyRuntimeWorldState(DimensionRuntimeData data, DayCycleManager dayCycle, World world) {
        if (data == null || world == null) {
            return;
        }
        if (dayCycle != null) {
            if (data.worldTime != null && data.worldTime >= 0L) {
                dayCycle.setWorldTime(data.worldTime);
            } else if (data.time != null && data.time >= 0.0f) {
                dayCycle.setTime(data.time);
            }
        }
        if (data.weatherState != null || data.weatherRainTime != null || data.weatherThunderTime != null) {
            world.setWeatherState(data.weatherState, data.weatherRainTime, data.weatherThunderTime);
        }
    }

    private void applyRuntimeData(DimensionRuntimeData data, Player player, World world, PlayerData playerData) {
        if (data == null) {
            return;
        }
        if (data.droppedItems != null) {
            List<DroppedItem> restored = new ArrayList<>();
            for (DroppedItemData itemData : data.droppedItems) {
                ItemStack stack = itemData.toStack();
                if (stack == null || stack.isEmpty()) {
                    continue;
                }
                DroppedItem item = new DroppedItem(itemData.x, itemData.y, itemData.z, stack,
                        itemData.velocityX, itemData.velocityY, itemData.velocityZ);
                item.setAge(itemData.age);
                if (itemData.pickupDelayTicks != null) {
                    item.restorePickupDelayState(itemData.pickupDelayTicks, itemData.pickupDelayAccumulator);
                }
                if (itemData.health != null) {
                    item.setHealth(itemData.health);
                }
                item.setOnGround(itemData.onGround);
                item.setAnimationState(itemData.rotation, itemData.bobPhase);
                restored.add(item);
            }
            world.replaceDroppedItems(restored);
        }

        if (data.tileEntities != null) {
            List<TileEntity> restored = new ArrayList<>();
            for (TileEntityData tileData : data.tileEntities) {
                TileEntity tile = tileData.toTileEntity();
                if (tile != null) {
                    restored.add(tile);
                }
            }
            world.replaceTileEntities(restored);
        }

        if (data.entities != null) {
            List<Entity> restored = new ArrayList<>();
            List<EntityData> restoredData = new ArrayList<>();
            Map<Integer, Entity> restoredBySaveId = new HashMap<>();
            for (EntityData entityData : data.entities) {
                Entity entity = entityData.toEntity(player);
                if (entity != null) {
                    entityData.restoreGenericPhysicsState(entity);
                    restored.add(entity);
                    restoredData.add(entityData);
                    if (entityData.entitySaveId > 0) {
                        restoredBySaveId.put(entityData.entitySaveId, entity);
                    }
                }
            }
            for (int i = 0; i < restored.size(); i++) {
                restoredData.get(i).restoreEntityReferences(restored.get(i), restoredBySaveId);
            }
            world.replaceEntities(restored);
            if (playerData != null) {
                playerData.restorePlayerEntityReferences(player, restoredBySaveId);
            }
        }

        if (data.movingPistons != null) {
            List<World.MovingPistonState> restored = new ArrayList<>();
            long blockTickClock = world.getBlockTickClock();
            for (MovingPistonData pistonData : data.movingPistons) {
                World.MovingPistonState state = pistonData == null ? null : pistonData.toState(blockTickClock);
                if (state != null) {
                    restored.add(state);
                }
            }
            world.replaceMovingPistonStates(restored);
        }

        if (data.scheduledBlockTicks != null) {
            List<World.ScheduledBlockTickState> restored = new ArrayList<>();
            for (ScheduledBlockTickData tickData : data.scheduledBlockTicks) {
                World.ScheduledBlockTickState tick = tickData == null ? null : tickData.toState();
                if (tick != null) {
                    restored.add(tick);
                }
            }
            world.replaceScheduledBlockTicks(restored);
        }
    }

    private void restoreSavedSleepingState(PlayerData data, Player player, World world) {
        if (data == null || player == null || world == null || !data.hasCompleteSleepingState()) {
            return;
        }
        BlockPos foot = new BlockPos(data.sleepingBedFootX, data.sleepingBedFootY, data.sleepingBedFootZ);
        BlockPos head = new BlockPos(data.sleepingBedHeadX, data.sleepingBedHeadY, data.sleepingBedHeadZ);
        player.restoreSleepingState(foot, head,
                data.sleepReturnX, data.sleepReturnY, data.sleepReturnZ,
                data.sleepReturnYaw, data.sleepReturnPitch);
        BlockPos wakePos = world.findBedRespawnPosition(foot.x(), foot.y(), foot.z());
        world.setBedOccupied(foot.x(), foot.y(), foot.z(), false);
        player.wakeFromBed(wakePos);
    }

    private void saveModifiedChunks(Collection<Chunk> chunks) throws IOException {
        for (Chunk chunk : chunks) {
            if (chunk.isModified()) {
                chunk.calculateSkyLight();
                short[] blockIds = chunk.copyBlockIds();
                byte[] metadata = chunk.copyBlockMetadata();
                byte[] skyLight = chunk.copySkyLight();
                byte[] blockLight = chunk.copyBlockLight();
                int[] heightMap = chunk.copyHeightMap();
                ChunkCodec.write(chunkPath(Dimension.OVERWORLD, chunk.getChunkX(), chunk.getChunkZ()),
                        blockIds, metadata, skyLight, blockLight, heightMap);
                claimOrVerifyReleaseSessionLock();
                ReleaseRegionFile.writeChunk(worldDir, Dimension.OVERWORLD, chunk.getChunkX(), chunk.getChunkZ(),
                        new ChunkCodec.ChunkData(blockIds, metadata, skyLight, blockLight, heightMap));
                chunk.clearModified();
            }
        }
    }

    private Path chunkPath(int chunkX, int chunkZ) {
        return chunkPath(Dimension.OVERWORLD, chunkX, chunkZ);
    }

    private Path chunkPath(Dimension dimension, int chunkX, int chunkZ) {
        return chunkDirFor(dimension).resolve("c." + chunkX + "." + chunkZ + ".bin");
    }

    private Path chunkDirFor(Dimension dimension) {
        Dimension normalized = dimension == null ? Dimension.OVERWORLD : dimension;
        if (normalized == Dimension.OVERWORLD) {
            return chunksDir;
        }
        return dimensionDirFor(normalized).resolve("chunks");
    }

    private Path dimensionDirFor(Dimension dimension) {
        Dimension normalized = dimension == null ? Dimension.OVERWORLD : dimension;
        return worldDir.resolve("dimensions").resolve(normalized.getSaveName());
    }

    private Path dimensionRuntimePath(Dimension dimension) {
        return dimensionDirFor(dimension).resolve("runtime.json");
    }

    private void writeDimensionRuntime(Dimension dimension, DimensionRuntimeData data) throws IOException {
        if (data == null) {
            return;
        }
        Files.createDirectories(dimensionDirFor(dimension));
        SafeFiles.writeAtomic(dimensionRuntimePath(dimension), writer -> gson.toJson(data, writer),
                SafeFiles.BackupPolicy.BAK);
    }

    private DimensionRuntimeData loadDimensionRuntimeIfExists(Dimension dimension) {
        Path path = dimensionRuntimePath(dimension);
        if (Files.isRegularFile(path)) {
            DimensionRuntimeData data = loadDimensionRuntimeFrom(path);
            if (data != null) {
                return data;
            }
        }

        Path backup = SafeFiles.backupPath(path);
        if (Files.isRegularFile(backup)) {
            DimensionRuntimeData data = loadDimensionRuntimeFrom(backup);
            if (data != null) {
                return data;
            }
        }
        return null;
    }

    private DimensionRuntimeData loadDimensionRuntimeFrom(Path path) {
        try (Reader reader = Files.newBufferedReader(path)) {
            DimensionRuntimeData data = gson.fromJson(reader, DimensionRuntimeData.class);
            String error = validateDimensionRuntimeData(data);
            if (error != null) {
                throw new IOException(error);
            }
            normalizeDimensionRuntimeData(data);
            return data;
        } catch (Exception e) {
            System.err.println("Failed to load dimension runtime " + path + ": " + e.getMessage());
            return null;
        }
    }

    private void primeDimensionRuntimeCache(LevelData data) {
        Map<Dimension, DimensionRuntimeData> loaded = new HashMap<>();
        if (data != null) {
            Dimension current = Dimension.fromSaveName(data.dimension);
            loaded.put(current, DimensionRuntimeData.from(data));
        }
        for (Dimension dimension : Dimension.values()) {
            DimensionRuntimeData runtime = loadDimensionRuntimeIfExists(dimension);
            if (runtime == null) {
                runtime = loadReleaseRegionRuntimeIfExists(dimension);
            }
            if (runtime != null) {
                loaded.put(dimension, runtime);
            }
        }
        synchronized (this) {
            dimensionRuntimeCache.clear();
            for (Map.Entry<Dimension, DimensionRuntimeData> entry : loaded.entrySet()) {
                DimensionRuntimeData copy = copyDimensionRuntimeData(entry.getKey(), entry.getValue());
                if (copy != null) {
                    dimensionRuntimeCache.put(entry.getKey(), copy);
                }
            }
        }
    }

    private void mergeReleaseRegionRuntime(LevelData data, Dimension dimension) {
        if (data == null) {
            return;
        }
        DimensionRuntimeData runtime = loadReleaseRegionRuntimeIfExists(dimension);
        if (runtime == null) {
            return;
        }
        if (data.droppedItems == null) {
            data.droppedItems = new ArrayList<>();
        }
        if (data.tileEntities == null) {
            data.tileEntities = new ArrayList<>();
        }
        if (data.entities == null) {
            data.entities = new ArrayList<>();
        }
        if (data.scheduledBlockTicks == null) {
            data.scheduledBlockTicks = new ArrayList<>();
        }
        data.droppedItems.addAll(DimensionRuntimeData.copyList(runtime.droppedItems));
        data.tileEntities.addAll(DimensionRuntimeData.copyList(runtime.tileEntities));
        data.entities.addAll(DimensionRuntimeData.copyList(runtime.entities));
        data.scheduledBlockTicks.addAll(DimensionRuntimeData.copyList(runtime.scheduledBlockTicks));
    }

    private DimensionRuntimeData loadReleaseRegionRuntimeIfExists(Dimension dimension) {
        try {
            DimensionRuntimeData runtime = ReleaseRegionFile.readRuntime(worldDir, dimension);
            if (runtime == null) {
                return null;
            }
            String error = validateDimensionRuntimeData(runtime);
            if (error != null) {
                throw new IOException(error);
            }
            normalizeDimensionRuntimeData(runtime);
            return runtime;
        } catch (Exception e) {
            System.err.println("Failed to import Release region runtime for "
                    + (dimension == null ? Dimension.OVERWORLD : dimension).getSaveName()
                    + ": " + e.getMessage());
            return null;
        }
    }

    private ReleaseMapData.ImportResult loadReleaseMapDataIfExists() {
        try {
            return ReleaseMapData.readAll(worldDir);
        } catch (Exception e) {
            System.err.println("Failed to import Release map data: " + e.getMessage());
            return new ReleaseMapData.ImportResult(Map.of());
        }
    }

    private void mergeReleasePlayerFileIfExists(LevelData data) {
        for (Path playerPath : selectReleasePlayerFiles()) {
            try {
                ReleaseLevelDat.PlayerSaveData playerData = ReleaseLevelDat.readPlayer(playerPath, data);
                if (playerData == null || playerData.player() == null || playerData.inventory() == null) {
                    continue;
                }
                data.player = playerData.player();
                data.inventory = playerData.inventory();
                return;
            } catch (Exception e) {
                System.err.println("Failed to import Release player data " + playerPath + ": " + e.getMessage());
            }
        }
    }

    private List<Path> selectReleasePlayerFiles() {
        Path playersDir = worldDir.resolve(RELEASE_PLAYER_DIR);
        if (!Files.isDirectory(playersDir)) {
            return List.of();
        }
        String preferred = GameSettings.DEFAULT_PLAYER_NAME + ".dat";
        try (var stream = Files.list(playersDir)) {
            return stream
                    .filter(Files::isRegularFile)
                    .filter(path -> {
                        String fileName = path.getFileName() == null ? "" : path.getFileName().toString();
                        return fileName.toLowerCase(Locale.ROOT).endsWith(".dat");
                    })
                    .sorted(Comparator
                            .comparingInt((Path path) -> isPreferredReleasePlayerFile(path, preferred) ? 0 : 1)
                            .thenComparing(Comparator.comparingLong(SaveManager::releasePlayerModifiedMillis)
                                    .reversed())
                            .thenComparing(path -> releasePlayerFileName(path).toLowerCase(Locale.ROOT)))
                    .toList();
        } catch (IOException e) {
            System.err.println("Failed to scan Release player data: " + e.getMessage());
            return List.of();
        }
    }

    private static boolean isPreferredReleasePlayerFile(Path path, String preferred) {
        return releasePlayerFileName(path).equalsIgnoreCase(preferred);
    }

    private static String releasePlayerFileName(Path path) {
        return path == null || path.getFileName() == null ? "" : path.getFileName().toString();
    }

    private static long releasePlayerModifiedMillis(Path path) {
        try {
            return Files.getLastModifiedTime(path).toMillis();
        } catch (IOException e) {
            return Long.MIN_VALUE;
        }
    }

    private Path releasePlayerPath() {
        return worldDir.resolve(RELEASE_PLAYER_DIR).resolve(GameSettings.DEFAULT_PLAYER_NAME + ".dat");
    }

    private void applyReleaseMapStates(LevelData data, Map<String, ReleaseMapData.MapState> states) {
        if (data == null || states == null || states.isEmpty()) {
            return;
        }
        applyReleaseMapStates(data.inventory, states);
        if (data.droppedItems != null) {
            for (DroppedItemData dropped : data.droppedItems) {
                applyReleaseMapState(dropped, states);
            }
        }
        if (data.tileEntities != null) {
            for (TileEntityData tile : data.tileEntities) {
                if (tile == null) {
                    continue;
                }
                applyReleaseMapStates(tile.inventory, states);
                applyReleaseMapState(tile.record, states);
            }
        }
        if (data.entities != null) {
            for (EntityData entity : data.entities) {
                if (entity != null) {
                    applyReleaseMapStates(entity.inventory, states);
                }
            }
        }
    }

    private static void applyReleaseMapStates(InventoryData inventory, Map<String, ReleaseMapData.MapState> states) {
        if (inventory == null) {
            return;
        }
        applyReleaseMapStates(inventory.hotbar, states);
        applyReleaseMapStates(inventory.main, states);
        applyReleaseMapStates(inventory.crafting, states);
        applyReleaseMapStates(inventory.armor, states);
        applyReleaseMapState(inventory.cursor, states);
    }

    private static void applyReleaseMapStates(StackData[] stacks, Map<String, ReleaseMapData.MapState> states) {
        if (stacks == null) {
            return;
        }
        for (StackData stack : stacks) {
            applyReleaseMapState(stack, states);
        }
    }

    private static void applyReleaseMapState(StackData stack, Map<String, ReleaseMapData.MapState> states) {
        if (stack == null) {
            return;
        }
        String id = mapIdForItem(stack.itemId, stack.dataValue, stack.durability, stack.metadata);
        ReleaseMapData.MapState state = id == null ? null : states.get(id);
        if (state == null) {
            return;
        }
        stack.metadata = releaseMapMetadata(stack.metadata, state);
        if (stack.durability < 0) {
            stack.durability = mapNumericId(state.id());
        }
    }

    private static void applyReleaseMapState(DroppedItemData item, Map<String, ReleaseMapData.MapState> states) {
        if (item == null) {
            return;
        }
        String id = mapIdForItem(item.itemId, item.dataValue, item.durability, item.metadata);
        ReleaseMapData.MapState state = id == null ? null : states.get(id);
        if (state == null) {
            return;
        }
        item.metadata = releaseMapMetadata(item.metadata, state);
        if (item.durability < 0) {
            item.durability = mapNumericId(state.id());
        }
    }

    private static Map<String, ReleaseMapData.MapState> collectReleaseMapStates(LevelData data) {
        Map<String, ReleaseMapData.MapState> states = new HashMap<>();
        if (data == null) {
            return states;
        }
        collectReleaseMapStates(data.inventory, states);
        if (data.droppedItems != null) {
            for (DroppedItemData item : data.droppedItems) {
                collectReleaseMapState(item == null ? 0 : item.itemId, item == null ? 0 : item.dataValue,
                        item == null ? -1 : item.durability, item == null ? null : item.metadata, states);
            }
        }
        if (data.tileEntities != null) {
            for (TileEntityData tile : data.tileEntities) {
                if (tile == null) {
                    continue;
                }
                collectReleaseMapStates(tile.inventory, states);
                collectReleaseMapState(tile.record, states);
            }
        }
        if (data.entities != null) {
            for (EntityData entity : data.entities) {
                if (entity != null) {
                    collectReleaseMapStates(entity.inventory, states);
                }
            }
        }
        return states;
    }

    private static void collectReleaseMapStates(InventoryData inventory, Map<String, ReleaseMapData.MapState> states) {
        if (inventory == null) {
            return;
        }
        collectReleaseMapStates(inventory.hotbar, states);
        collectReleaseMapStates(inventory.main, states);
        collectReleaseMapStates(inventory.crafting, states);
        collectReleaseMapStates(inventory.armor, states);
        collectReleaseMapState(inventory.cursor, states);
    }

    private static void collectReleaseMapStates(StackData[] stacks, Map<String, ReleaseMapData.MapState> states) {
        if (stacks == null) {
            return;
        }
        for (StackData stack : stacks) {
            collectReleaseMapState(stack, states);
        }
    }

    private static void collectReleaseMapState(StackData stack, Map<String, ReleaseMapData.MapState> states) {
        if (stack == null) {
            return;
        }
        collectReleaseMapState(stack.itemId, stack.dataValue, stack.durability, stack.metadata, states);
    }

    private static void collectReleaseMapState(int itemId, int dataValue, int durability,
            Map<String, String> metadata, Map<String, ReleaseMapData.MapState> states) {
        if (states == null) {
            return;
        }
        String id = mapIdForItem(itemId, dataValue, durability, metadata);
        if (id == null || states.containsKey(id)) {
            return;
        }
        states.put(id, new ReleaseMapData.MapState(id,
                readMapMetadataInt(metadata, MAP_SCALE_KEY, MapItemData.DEFAULT_SCALE),
                readMapMetadataInt(metadata, MAP_CENTER_X_KEY, 0),
                readMapMetadataInt(metadata, MAP_CENTER_Z_KEY, 0),
                readMapMetadataInt(metadata, MAP_DIMENSION_KEY, 0),
                null));
    }

    private static String mapIdForItem(int itemId, int dataValue, int durability, Map<String, String> metadata) {
        if (ItemType.fromId(itemId, dataValue) != ItemType.MAP) {
            return null;
        }
        String id = metadata == null ? null : metadata.get(MAP_ID_KEY);
        if (id != null && mapNumericId(id) >= 0) {
            return id;
        }
        if (durability >= 0) {
            return MAP_ID_PREFIX + durability;
        }
        return null;
    }

    private static Map<String, String> releaseMapMetadata(Map<String, String> existing,
            ReleaseMapData.MapState state) {
        Map<String, String> metadata = existing == null ? new HashMap<>() : new HashMap<>(existing);
        metadata.put(MAP_INITIALIZED_KEY, "true");
        metadata.put(MAP_ID_KEY, state.id());
        metadata.put(MAP_SCALE_KEY, Integer.toString(Math.max(0, Math.min(4, state.scale()))));
        metadata.put(MAP_CENTER_X_KEY, Integer.toString(state.centerX()));
        metadata.put(MAP_CENTER_Z_KEY, Integer.toString(state.centerZ()));
        metadata.put(MAP_DIMENSION_KEY, Integer.toString(state.dimension()));
        metadata.put(MAP_COLOR_FORMAT_KEY, MAP_COLOR_FORMAT_SHADED);
        return metadata;
    }

    private static int readMapMetadataInt(Map<String, String> metadata, String key, int fallback) {
        if (metadata == null) {
            return fallback;
        }
        String raw = metadata.get(key);
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private static int mapNumericId(String id) {
        if (id == null || !id.startsWith(MAP_ID_PREFIX)) {
            return -1;
        }
        try {
            return Integer.parseInt(id.substring(MAP_ID_PREFIX.length()));
        } catch (NumberFormatException ignored) {
            return -1;
        }
    }

    private static int nextFilledMapIdFor(List<FilledMapData> maps) {
        int next = 0;
        if (maps == null) {
            return next;
        }
        for (FilledMapData map : maps) {
            int numericId = map == null ? -1 : mapNumericId(map.id);
            if (numericId >= 0) {
                next = Math.max(next, numericId + 1);
            }
        }
        return next;
    }

    private synchronized void rememberDimensionRuntime(Dimension dimension, DimensionRuntimeData data) {
        DimensionRuntimeData copy = copyDimensionRuntimeData(dimension, data);
        if (copy != null) {
            Dimension normalized = dimension == null ? Dimension.fromSaveName(copy.dimension) : dimension;
            dimensionRuntimeCache.put(normalized, copy);
        }
    }

    private synchronized DimensionRuntimeData dimensionRuntimeFor(Dimension dimension) {
        Dimension normalized = dimension == null ? Dimension.OVERWORLD : dimension;
        return copyDimensionRuntimeData(normalized, dimensionRuntimeCache.get(normalized));
    }

    private synchronized Map<Dimension, DimensionRuntimeData> dimensionRuntimeSnapshot() {
        Map<Dimension, DimensionRuntimeData> snapshot = new HashMap<>();
        for (Map.Entry<Dimension, DimensionRuntimeData> entry : dimensionRuntimeCache.entrySet()) {
            DimensionRuntimeData copy = copyDimensionRuntimeData(entry.getKey(), entry.getValue());
            if (copy != null) {
                snapshot.put(entry.getKey(), copy);
            }
        }
        return snapshot;
    }

    private synchronized DimensionRuntimeData mergeChunkRuntimeData(Dimension dimension, int chunkX, int chunkZ,
            DimensionRuntimeData chunkRuntime) {
        if (chunkRuntime == null) {
            return null;
        }
        Dimension normalized = dimension == null ? Dimension.OVERWORLD : dimension;
        DimensionRuntimeData merged = copyDimensionRuntimeData(normalized, dimensionRuntimeCache.get(normalized));
        if (merged == null) {
            merged = emptyDimensionRuntimeData(normalized);
        }
        removeChunkRuntimeData(merged, chunkX, chunkZ);
        DimensionRuntimeData chunkCopy = copyDimensionRuntimeData(normalized, chunkRuntime);
        if (chunkCopy == null) {
            dimensionRuntimeCache.put(normalized, merged);
            return copyDimensionRuntimeData(normalized, merged);
        }
        mergeRuntimeWorldState(merged, chunkCopy);
        merged.droppedItems.addAll(DimensionRuntimeData.copyList(chunkCopy.droppedItems));
        merged.tileEntities.addAll(DimensionRuntimeData.copyList(chunkCopy.tileEntities));
        merged.entities.addAll(DimensionRuntimeData.copyList(chunkCopy.entities));
        merged.movingPistons.addAll(DimensionRuntimeData.copyList(chunkCopy.movingPistons));
        merged.scheduledBlockTicks.addAll(DimensionRuntimeData.copyList(chunkCopy.scheduledBlockTicks));
        dimensionRuntimeCache.put(normalized, copyDimensionRuntimeData(normalized, merged));
        return copyDimensionRuntimeData(normalized, merged);
    }

    private static DimensionRuntimeData emptyDimensionRuntimeData(Dimension dimension) {
        Dimension normalized = dimension == null ? Dimension.OVERWORLD : dimension;
        DimensionRuntimeData data = new DimensionRuntimeData();
        data.dimension = normalized.getSaveName();
        data.droppedItems = new ArrayList<>();
        data.tileEntities = new ArrayList<>();
        data.entities = new ArrayList<>();
        data.movingPistons = new ArrayList<>();
        data.scheduledBlockTicks = new ArrayList<>();
        return data;
    }

    private static void mergeRuntimeWorldState(DimensionRuntimeData target, DimensionRuntimeData source) {
        if (target == null || source == null) {
            return;
        }
        if (source.time != null) {
            target.time = source.time;
        }
        if (source.worldTime != null) {
            target.worldTime = source.worldTime;
        }
        if (source.dayCount != null) {
            target.dayCount = source.dayCount;
        }
        if (source.moonPhase != null) {
            target.moonPhase = source.moonPhase;
        }
        if (source.weatherState != null) {
            target.weatherState = source.weatherState;
        }
        if (source.weatherRainTime != null) {
            target.weatherRainTime = source.weatherRainTime;
        }
        if (source.weatherThunderTime != null) {
            target.weatherThunderTime = source.weatherThunderTime;
        }
    }

    private static void removeChunkRuntimeData(DimensionRuntimeData data, int chunkX, int chunkZ) {
        if (data == null) {
            return;
        }
        data.droppedItems.removeIf(item -> item != null && isEntityInChunk(item.x, item.z, chunkX, chunkZ));
        data.tileEntities.removeIf(tile -> tile != null && isBlockInChunk(tile.x, tile.z, chunkX, chunkZ));
        data.entities.removeIf(entity -> entity != null && isEntityInChunk(entity.x, entity.z, chunkX, chunkZ));
        data.movingPistons.removeIf(piston -> piston != null && isBlockInChunk(piston.x, piston.z, chunkX, chunkZ));
        data.scheduledBlockTicks.removeIf(tick -> tick != null && isBlockInChunk(tick.x, tick.z, chunkX, chunkZ));
    }

    private static DimensionRuntimeData copyDimensionRuntimeData(Dimension dimension, DimensionRuntimeData source) {
        if (source == null) {
            return null;
        }
        Dimension normalized = dimension == null ? Dimension.fromSaveName(source.dimension) : dimension;
        DimensionRuntimeData copy = new DimensionRuntimeData();
        copy.dimension = normalized.getSaveName();
        copy.time = source.time;
        copy.worldTime = source.worldTime;
        copy.dayCount = source.dayCount;
        copy.moonPhase = source.moonPhase;
        copy.weatherState = source.weatherState;
        copy.weatherRainTime = source.weatherRainTime;
        copy.weatherThunderTime = source.weatherThunderTime;
        copy.droppedItems = DimensionRuntimeData.copyList(source.droppedItems);
        copy.tileEntities = DimensionRuntimeData.copyList(source.tileEntities);
        copy.entities = DimensionRuntimeData.copyList(source.entities);
        copy.movingPistons = DimensionRuntimeData.copyList(source.movingPistons);
        copy.scheduledBlockTicks = DimensionRuntimeData.copyList(source.scheduledBlockTicks);
        return copy;
    }

    public DimensionRuntimeData createChunkRuntimeDataSnapshot(World world, int chunkX, int chunkZ) {
        if (world == null) {
            return null;
        }
        DimensionRuntimeData data = new DimensionRuntimeData();
        data.dimension = world.getDimension().getSaveName();
        DayCycleManager dayCycle = world.getDayCycleManager();
        if (dayCycle != null) {
            data.time = dayCycle.getTime();
            data.worldTime = dayCycle.getWorldTime();
            data.dayCount = dayCycle.getDayCount();
            data.moonPhase = dayCycle.getMoonPhase();
        }
        data.weatherState = world.getWeatherState();
        data.weatherRainTime = world.getRainTime();
        data.weatherThunderTime = world.getThunderTime();
        data.droppedItems = new ArrayList<>();
        data.tileEntities = new ArrayList<>();
        data.entities = new ArrayList<>();
        data.movingPistons = new ArrayList<>();
        data.scheduledBlockTicks = new ArrayList<>();

        for (DroppedItem item : world.getDroppedItems()) {
            if (item != null && isEntityInChunk(item.getX(), item.getZ(), chunkX, chunkZ)) {
                data.droppedItems.add(DroppedItemData.from(item));
            }
        }
        for (TileEntity tile : world.getTileEntities()) {
            BlockPos pos = tile == null ? null : tile.getPos();
            if (pos != null && isBlockInChunk(pos.x(), pos.z(), chunkX, chunkZ)) {
                data.tileEntities.add(TileEntityData.from(tile));
            }
        }

        List<Entity> savedEntities = new ArrayList<>();
        Map<Entity, Integer> entitySaveIds = new IdentityHashMap<>();
        for (Entity entity : world.getEntitiesIncludingPending()) {
            if (entity == null || !isEntityInChunk(entity.getX(), entity.getZ(), chunkX, chunkZ)) {
                continue;
            }
            EntityData entityData = EntityData.from(entity);
            if (entityData != null) {
                entityData.entitySaveId = data.entities.size() + 1;
                entitySaveIds.put(entity, entityData.entitySaveId);
                savedEntities.add(entity);
                data.entities.add(entityData);
            }
        }
        for (int i = 0; i < data.entities.size(); i++) {
            data.entities.get(i).captureEntityReferences(savedEntities.get(i), entitySaveIds);
        }

        long blockTickClock = world.getBlockTickClock();
        for (World.MovingPistonState state : world.getMovingPistonStates()) {
            if (state != null && isBlockInChunk(state.x(), state.z(), chunkX, chunkZ)) {
                data.movingPistons.add(MovingPistonData.from(state, blockTickClock));
            }
        }
        for (World.ScheduledBlockTickState tick : world.getScheduledBlockTickStates()) {
            if (tick != null && isBlockInChunk(tick.x(), tick.z(), chunkX, chunkZ)) {
                data.scheduledBlockTicks.add(ScheduledBlockTickData.from(tick));
            }
        }
        return data;
    }

    private static boolean isBlockInChunk(int x, int z, int chunkX, int chunkZ) {
        return Math.floorDiv(x, Chunk.WIDTH) == chunkX
                && Math.floorDiv(z, Chunk.DEPTH) == chunkZ;
    }

    private static boolean isEntityInChunk(float x, float z, int chunkX, int chunkZ) {
        return isBlockInChunk((int) Math.floor(x), (int) Math.floor(z), chunkX, chunkZ);
    }

    private static boolean hasRuntimeDataInChunk(LevelData data, int chunkX, int chunkZ) {
        if (data == null) {
            return false;
        }
        if (data.droppedItems != null) {
            for (DroppedItemData item : data.droppedItems) {
                if (item != null && isEntityInChunk(item.x, item.z, chunkX, chunkZ)) {
                    return true;
                }
            }
        }
        if (data.tileEntities != null) {
            for (TileEntityData tile : data.tileEntities) {
                if (tile != null && isBlockInChunk(tile.x, tile.z, chunkX, chunkZ)) {
                    return true;
                }
            }
        }
        if (data.entities != null) {
            for (EntityData entity : data.entities) {
                if (entity != null && isEntityInChunk(entity.x, entity.z, chunkX, chunkZ)) {
                    return true;
                }
            }
        }
        if (data.movingPistons != null) {
            for (MovingPistonData piston : data.movingPistons) {
                if (piston != null && isBlockInChunk(piston.x, piston.z, chunkX, chunkZ)) {
                    return true;
                }
            }
        }
        if (data.scheduledBlockTicks != null) {
            for (ScheduledBlockTickData tick : data.scheduledBlockTicks) {
                if (tick != null && isBlockInChunk(tick.x, tick.z, chunkX, chunkZ)) {
                    return true;
                }
            }
        }
        return false;
    }

    private LevelData createLevelData(World world, Player player, DayCycleManager dayCycle) {
        LevelData data = new LevelData();
        data.formatVersion = FORMAT_VERSION;
        data.targetVersion = TARGET_VERSION;
        data.levelName = levelName;
        data.serverMotd = serverMotd;
        data.serverIp = serverIp;
        data.serverPort = serverPort;
        data.serverMaxPlayers = serverMaxPlayers;
        data.serverPvp = serverPvp;
        data.serverSpawnAnimals = serverSpawnAnimals;
        data.serverSpawnMonsters = serverSpawnMonsters;
        data.serverSpawnNpcs = serverSpawnNpcs;
        data.serverAllowNether = serverAllowNether;
        data.serverOnlineMode = serverOnlineMode;
        data.serverAllowFlight = serverAllowFlight;
        data.serverEnableQuery = serverEnableQuery;
        data.serverQueryPort = serverQueryPort;
        data.serverSpawnProtection = serverSpawnProtection;
        data.serverViewDistance = serverViewDistance;
        data.serverMaxBuildHeight = serverMaxBuildHeight;
        data.serverLevelSeed = serverLevelSeed.isBlank() ? Long.toString(world.getSeed()) : serverLevelSeed;
        data.generateStructures = world.shouldGenerateStructures();
        data.lastPlayed = System.currentTimeMillis();
        data.gameMode = gameMode.name();
        data.difficulty = difficulty.name();
        data.hardcore = hardcore;
        data.allowCheats = allowCheats;
        data.seed = world.getSeed();
        data.generatorId = world.getGeneratorId();
        data.dimension = world.getDimension().getSaveName();
        data.spawnX = spawnX;
        data.spawnY = spawnY;
        data.spawnZ = spawnZ;
        data.time = dayCycle.getTime();
        data.worldTime = dayCycle.getWorldTime();
        data.dayCount = dayCycle.getDayCount();
        data.moonPhase = dayCycle.getMoonPhase();
        data.weatherState = world.getWeatherState();
        data.weatherRainTime = world.getRainTime();
        data.weatherThunderTime = world.getThunderTime();
        mergeGlobalFilledMapColors(world.getFilledMapColorsSnapshot());
        data.filledMaps = filledMapDataSnapshot();
        data.nextFilledMapId = Math.max(world.getNextFilledMapId(), globalFilledMapNextIdSnapshot());
        data.operators = new ArrayList<>(operators);
        data.bannedPlayers = new ArrayList<>(bannedPlayers);
        data.bannedIps = new ArrayList<>(bannedIps);
        data.whitelist = new ArrayList<>(whitelist);
        data.whitelistEnabled = whitelistEnabled;
        data.player = PlayerData.from(player);
        data.inventory = InventoryData.from(player.getInventory());
        data.droppedItems = new ArrayList<>();
        for (DroppedItem item : world.getDroppedItems()) {
            data.droppedItems.add(DroppedItemData.from(item));
        }
        data.tileEntities = new ArrayList<>();
        for (TileEntity tile : world.getTileEntities()) {
            data.tileEntities.add(TileEntityData.from(tile));
        }
        data.entities = new ArrayList<>();
        List<Entity> savedEntities = new ArrayList<>();
        Map<Entity, Integer> entitySaveIds = new IdentityHashMap<>();
        for (Entity entity : world.getEntitiesIncludingPending()) {
            EntityData entityData = EntityData.from(entity);
            if (entityData != null) {
                entityData.entitySaveId = data.entities.size() + 1;
                entitySaveIds.put(entity, entityData.entitySaveId);
                savedEntities.add(entity);
                data.entities.add(entityData);
            }
        }
        for (int i = 0; i < data.entities.size(); i++) {
            data.entities.get(i).captureEntityReferences(savedEntities.get(i), entitySaveIds);
        }
        data.player.captureEntityReferences(player, entitySaveIds);
        data.movingPistons = new ArrayList<>();
        for (World.MovingPistonState state : world.getMovingPistonStates()) {
            data.movingPistons.add(MovingPistonData.from(state, world.getBlockTickClock()));
        }
        data.scheduledBlockTicks = new ArrayList<>();
        for (World.ScheduledBlockTickState tick : world.getScheduledBlockTickStates()) {
            data.scheduledBlockTicks.add(ScheduledBlockTickData.from(tick));
        }
        return data;
    }

    private String validateLevelData(LevelData data) {
        String worldStateError = validateWorldStateData(data);
        if (worldStateError != null) {
            return worldStateError;
        }
        if (data.player == null) {
            return "missing player data";
        }
        String playerError = validatePlayerData(data.player);
        if (playerError != null) {
            return playerError;
        }
        if (data.inventory == null) {
            return "missing player inventory";
        }
        if (data.inventory.selectedSlot < 0 || data.inventory.selectedSlot >= Inventory.HOTBAR_SIZE) {
            return "invalid selected hotbar slot";
        }
        if (!hasExpectedLength(data.inventory.hotbar, Inventory.HOTBAR_SIZE)) {
            return "invalid hotbar inventory";
        }
        if (!hasExpectedLength(data.inventory.main, Inventory.MAIN_SIZE)) {
            return "invalid main inventory";
        }
        if (!hasExpectedLength(data.inventory.crafting, Inventory.CRAFTING_SIZE)) {
            return "invalid crafting inventory";
        }
        if (!hasExpectedLength(data.inventory.armor, 4)) {
            return "invalid armor inventory";
        }
        String playerStackError = validateInventoryStacks(data.inventory);
        if (playerStackError != null) {
            return playerStackError;
        }
        String playerEffectError = validateStatusEffects("player effects", data.player.activeEffects);
        if (playerEffectError != null) {
            return playerEffectError;
        }
        if (data.generatorId != null && !data.generatorId.isBlank()
                && !WorldGenerators.isSupportedGeneratorId(data.generatorId)) {
            return "invalid world generator";
        }
        if (data.dimension != null && !data.dimension.isBlank() && !Dimension.isValidSaveName(data.dimension)) {
            return "invalid world dimension";
        }
        if (containsNullElement(data.droppedItems)) {
            return "invalid dropped item list";
        }
        String droppedItemError = validateDroppedItems(data.droppedItems);
        if (droppedItemError != null) {
            return droppedItemError;
        }
        if (containsNullElement(data.tileEntities)) {
            return "invalid tile entity list";
        }
        String tileEntityError = validateTileEntityTypes(data.tileEntities);
        if (tileEntityError != null) {
            return tileEntityError;
        }
        if (containsNullElement(data.entities)) {
            return "invalid entity list";
        }
        String entityError = validateEntityTypes(data.entities);
        if (entityError != null) {
            return entityError;
        }
        String entityReferenceError = validateEntityReferences(data.entities);
        if (entityReferenceError != null) {
            return entityReferenceError;
        }
        String playerReferenceError = validatePlayerEntityReferences(data.player, data.entities);
        if (playerReferenceError != null) {
            return playerReferenceError;
        }
        if (containsNullElement(data.movingPistons)) {
            return "invalid moving piston list";
        }
        String movingPistonError = validateMovingPistons(data.movingPistons);
        if (movingPistonError != null) {
            return movingPistonError;
        }
        if (containsNullElement(data.scheduledBlockTicks)) {
            return "invalid scheduled block tick list";
        }
        String scheduledTickError = validateScheduledBlockTicks(data.scheduledBlockTicks);
        if (scheduledTickError != null) {
            return scheduledTickError;
        }
        if (containsNullElement(data.filledMaps)) {
            return "invalid filled map list";
        }
        String filledMapError = validateFilledMaps(data.filledMaps);
        if (filledMapError != null) {
            return filledMapError;
        }
        return null;
    }

    private String validateDimensionRuntimeData(DimensionRuntimeData data) {
        if (data == null) {
            return "missing dimension runtime data";
        }
        if (data.dimension != null && !data.dimension.isBlank() && !Dimension.isValidSaveName(data.dimension)) {
            return "invalid world dimension";
        }
        String worldStateError = validateDimensionRuntimeWorldStateData(data);
        if (worldStateError != null) {
            return worldStateError;
        }
        if (containsNullElement(data.droppedItems)) {
            return "invalid dropped item list";
        }
        String droppedItemError = validateDroppedItems(data.droppedItems);
        if (droppedItemError != null) {
            return droppedItemError;
        }
        if (containsNullElement(data.tileEntities)) {
            return "invalid tile entity list";
        }
        String tileEntityError = validateTileEntityTypes(data.tileEntities);
        if (tileEntityError != null) {
            return tileEntityError;
        }
        if (containsNullElement(data.entities)) {
            return "invalid entity list";
        }
        String entityError = validateEntityTypes(data.entities);
        if (entityError != null) {
            return entityError;
        }
        String entityReferenceError = validateEntityReferences(data.entities);
        if (entityReferenceError != null) {
            return entityReferenceError;
        }
        if (containsNullElement(data.movingPistons)) {
            return "invalid moving piston list";
        }
        String movingPistonError = validateMovingPistons(data.movingPistons);
        if (movingPistonError != null) {
            return movingPistonError;
        }
        if (containsNullElement(data.scheduledBlockTicks)) {
            return "invalid scheduled block tick list";
        }
        String scheduledTickError = validateScheduledBlockTicks(data.scheduledBlockTicks);
        if (scheduledTickError != null) {
            return scheduledTickError;
        }
        return null;
    }

    private static String validateDimensionRuntimeWorldStateData(DimensionRuntimeData data) {
        if (data.time != null && (!isFinite(data.time) || data.time < -1.0f)) {
            return "invalid dimension runtime time";
        }
        if (data.worldTime != null && data.worldTime < 0L) {
            return "invalid dimension runtime total time";
        }
        if (data.dayCount != null && data.dayCount < 0) {
            return "invalid dimension runtime day count";
        }
        if (data.moonPhase != null && (data.moonPhase < 0 || data.moonPhase > 7)) {
            return "invalid dimension runtime moon phase";
        }
        if (data.weatherState != null
                && !data.weatherState.isBlank()
                && !isValidWeatherState(data.weatherState)) {
            return "invalid dimension runtime weather";
        }
        if ((data.weatherRainTime != null && data.weatherRainTime <= 0)
                || (data.weatherThunderTime != null && data.weatherThunderTime <= 0)) {
            return "invalid dimension runtime weather timer";
        }
        return null;
    }

    private static boolean hasExpectedLength(Object[] values, int expectedLength) {
        return values != null && values.length == expectedLength;
    }

    private boolean containsNullElement(List<?> values) {
        return values != null && values.contains(null);
    }

    private static String validateFilledMaps(List<FilledMapData> maps) {
        if (maps == null) {
            return null;
        }
        Set<String> ids = new HashSet<>();
        int expectedColorBytes = MapItemData.MAP_SIZE * MapItemData.MAP_SIZE;
        for (FilledMapData map : maps) {
            if (map.id == null || map.id.isBlank()) {
                return "invalid filled map id";
            }
            if (!ids.add(map.id)) {
                return "duplicate filled map id";
            }
            if (map.colors == null || map.colors.isBlank()) {
                return "invalid filled map colors";
            }
            try {
                if (Base64.getDecoder().decode(map.colors).length != expectedColorBytes) {
                    return "invalid filled map colors";
                }
            } catch (IllegalArgumentException ignored) {
                return "invalid filled map colors";
            }
        }
        return null;
    }

    private static String validateWorldStateData(LevelData data) {
        if (!isFinite(data.time) || data.time < -1.0f) {
            return "invalid world time";
        }
        if (data.worldTime != null && data.worldTime < 0L) {
            return "invalid total world time";
        }
        if (data.dayCount != null && data.dayCount < 0) {
            return "invalid day count";
        }
        if (data.moonPhase != null && (data.moonPhase < 0 || data.moonPhase > 7)) {
            return "invalid moon phase";
        }
        if (data.nextFilledMapId < 0) {
            return "invalid map id counter";
        }
        if (data.weatherState != null
                && !data.weatherState.isBlank()
                && !isValidWeatherState(data.weatherState)) {
            return "invalid world weather";
        }
        if ((data.weatherRainTime != null && data.weatherRainTime <= 0)
                || (data.weatherThunderTime != null && data.weatherThunderTime <= 0)) {
            return "invalid world weather timer";
        }
        return null;
    }

    private static boolean isValidWeatherState(String weather) {
        String normalized = weather.trim().toLowerCase(java.util.Locale.ROOT);
        return "clear".equals(normalized) || "rain".equals(normalized) || "thunder".equals(normalized);
    }

    private static String validatePlayerData(PlayerData player) {
        if (!isFinite(player.x)
                || !isFinite(player.y)
                || !isFinite(player.z)
                || !isFinite(player.yaw)
                || !isFinite(player.pitch)
                || !isFinite(player.spawnX)
                || !isFinite(player.spawnY)
                || !isFinite(player.spawnZ)) {
            return "invalid player numeric state";
        }
        if (player.hasMovementState()) {
            if (!player.hasCompleteMovementState()
                    || !isFinite(player.motionX)
                    || !isFinite(player.motionY)
                    || !isFinite(player.motionZ)
                    || !isFinite(player.fallStartY)) {
                return "invalid player movement state";
            }
        }
        if (!isFinite(player.health)
                || !isFinite(player.hunger)
                || !isFinite(player.saturation)
                || !isFinite(player.exhaustion)
                || !isFinite(player.air)
                || player.health < 0.0f
                || player.health > PlayerStats.MAX_HEALTH
                || player.hunger < 0.0f
                || player.hunger > PlayerStats.MAX_HUNGER
                || player.saturation < 0.0f
                || player.saturation > player.hunger
                || player.exhaustion < 0.0f
                || player.exhaustion > PlayerStats.MAX_EXHAUSTION
                || player.air < 0.0f
                || player.air > PlayerStats.MAX_AIR_SECONDS) {
            return "invalid player stats";
        }
        if (player.fireTicks < 0) {
            return "invalid player fire state";
        }
        if (!isFinite(player.regenTimer)
                || !isFinite(player.peacefulRegenTimer)
                || !isFinite(player.starvationTimer)
                || !isFinite(player.drownTimer)
                || !isFinite(player.airTickAccumulator)
                || !isFinite(player.invincibilityTimer)
                || !isFinite(player.hurtInvulnerabilityTimer)
                || !isFinite(player.lastDamageAmount)
                || !isFinite(player.hurtFlashTimer)
                || player.regenTimer < 0.0f
                || player.peacefulRegenTimer < 0.0f
                || player.starvationTimer < 0.0f
                || player.drownTimer < 0.0f
                || player.airTickAccumulator < 0.0f
                || player.invincibilityTimer < 0.0f
                || player.hurtInvulnerabilityTimer < 0.0f
                || player.lastDamageAmount < 0.0f
                || player.deathTime < 0
                || player.hurtFlashTimer < 0.0f
                || player.foodTickTimer < 0) {
            return "invalid player runtime state";
        }
        if (player.ridingEntitySaveId < 0) {
            return "invalid player riding entity";
        }
        if (player.ridingEntityType != null && !player.ridingEntityType.isBlank()
                && !"MINECART".equals(player.ridingEntityType)
                && !"BOAT".equals(player.ridingEntityType)
                && !"PIG".equals(player.ridingEntityType)) {
            return "invalid player riding entity";
        }
        String sleepingError = validatePlayerSleepingState(player);
        if (sleepingError != null) {
            return sleepingError;
        }
        if (player.totalExperience < 0 || player.score < 0) {
            return "invalid player progression";
        }
        if (player.statPlayTimeTicks < 0
                || player.statDistanceWalkedCm < 0
                || player.statDistanceSwumCm < 0
                || player.statDistanceFallenCm < 0
                || player.statDistanceClimbedCm < 0
                || player.statDistanceFlownCm < 0
                || player.statDistanceDoveCm < 0
                || player.statDistanceByMinecartCm < 0
                || player.statDistanceByBoatCm < 0
                || player.statDistanceByPigCm < 0
                || player.statTimesPlayed < 0
                || player.statGamesQuit < 0
                || player.statWorldsLoaded < 0
                || player.statMultiplayerJoins < 0
                || player.statWorldsSaved < 0
                || player.statJumps < 0
                || player.statBlocksMined < 0
                || player.statSuccessfulAttacks < 0
                || player.statDamageDealtTenths < 0
                || player.statDamageTakenTenths < 0
                || player.statDeaths < 0
                || player.statMobKills < 0
                || player.statMonsterKills < 0
                || player.statPlayerKills < 0
                || player.statFishCaught < 0
                || player.statItemsPickedUp < 0
                || player.statItemsDropped < 0
                || player.statItemsCrafted < 0
                || player.statItemsUsed < 0
                || player.statItemsDepleted < 0) {
            return "invalid player statistics";
        }
        if (hasInvalidStatisticMap(player.statBlocksMinedByType)
                || hasInvalidStatisticMap(player.statItemsPickedUpByType)
                || hasInvalidStatisticMap(player.statItemsDroppedByType)
                || hasInvalidStatisticMap(player.statItemsCraftedByType)
                || hasInvalidStatisticMap(player.statItemsUsedByType)
                || hasInvalidStatisticMap(player.statItemsDepletedByType)) {
            return "invalid player statistics";
        }
        String achievementError = validatePlayerAchievements(player.achievements);
        if (achievementError != null) {
            return achievementError;
        }
        return null;
    }

    private static String validatePlayerSleepingState(PlayerData player) {
        if (player == null || !player.sleeping) {
            return null;
        }
        if (!player.hasCompleteSleepingState()) {
            return "invalid player sleep state";
        }
        if (player.sleepingBedFootY < 0 || player.sleepingBedFootY >= Chunk.HEIGHT
                || player.sleepingBedHeadY < 0 || player.sleepingBedHeadY >= Chunk.HEIGHT) {
            return "invalid player sleep state";
        }
        int dx = Math.abs(player.sleepingBedHeadX - player.sleepingBedFootX);
        int dz = Math.abs(player.sleepingBedHeadZ - player.sleepingBedFootZ);
        if (player.sleepingBedHeadY != player.sleepingBedFootY || dx + dz != 1) {
            return "invalid player sleep state";
        }
        if (!isFinite(player.sleepReturnX)
                || !isFinite(player.sleepReturnY)
                || !isFinite(player.sleepReturnZ)
                || !isFinite(player.sleepReturnYaw)
                || !isFinite(player.sleepReturnPitch)) {
            return "invalid player sleep state";
        }
        return null;
    }

    private static boolean hasInvalidStatisticMap(Map<?, Long> statistics) {
        if (statistics == null) {
            return false;
        }
        for (Map.Entry<?, Long> entry : statistics.entrySet()) {
            if (entry.getKey() == null || entry.getValue() == null || entry.getValue() < 0) {
                return true;
            }
        }
        return false;
    }

    private static String validatePlayerAchievements(List<String> achievements) {
        if (achievements == null) {
            return null;
        }
        Set<String> seen = new HashSet<>();
        for (String id : achievements) {
            if (id == null || id.isBlank() || AchievementType.fromId(id) == null) {
                return "invalid player achievement";
            }
            if (!seen.add(id)) {
                return "duplicate player achievement";
            }
        }
        return null;
    }

    private String validateInventoryStacks(InventoryData inventory) {
        String error = validateStackArray("hotbar inventory", inventory.hotbar);
        if (error != null) {
            return error;
        }
        error = validateStackArray("main inventory", inventory.main);
        if (error != null) {
            return error;
        }
        error = validateStackArray("crafting inventory", inventory.crafting);
        if (error != null) {
            return error;
        }
        error = validateStackArray("armor inventory", inventory.armor);
        if (error != null) {
            return error;
        }
        error = validateArmorInventory(inventory.armor);
        if (error != null) {
            return error;
        }
        return validateStack("cursor item", inventory.cursor);
    }

    private String validateArmorInventory(StackData[] armor) {
        ArmorSlot[] slots = ArmorSlot.values();
        for (int i = 0; i < armor.length; i++) {
            StackData stack = armor[i];
            if (stack == null) {
                continue;
            }
            ItemType type = ItemType.fromId(stack.itemId, stack.dataValue);
            if (ArmorMaterial.slotOf(type) != slots[i]) {
                return "invalid armor slot item in armor inventory[" + i + "]";
            }
        }
        return null;
    }

    private String validateDroppedItems(List<DroppedItemData> droppedItems) {
        if (droppedItems == null) {
            return null;
        }
        for (DroppedItemData itemData : droppedItems) {
            String error = validateItemPayload("dropped item",
                    itemData.itemId, itemData.dataValue, itemData.count,
                    itemData.durability, itemData.potion);
            if (error != null) {
                return error;
            }
            error = validateEnchantments("dropped item",
                    ItemType.fromId(itemData.itemId, itemData.dataValue), itemData.enchantments);
            if (error != null) {
                return error;
            }
            if (!isFinite(itemData.age) || itemData.age < 0.0f
                    || itemData.age >= DroppedItem.DESPAWN_TIME_SECONDS) {
                return "invalid dropped item age";
            }
            if (itemData.pickupDelayTicks != null && itemData.pickupDelayTicks < 0) {
                return "invalid dropped item pickup delay";
            }
            if (!isFinite(itemData.pickupDelayAccumulator) || itemData.pickupDelayAccumulator < 0.0f) {
                return "invalid dropped item pickup delay";
            }
            if (!isFinite(itemData.rotation) || !isFinite(itemData.bobPhase)) {
                return "invalid dropped item animation";
            }
            if (itemData.health != null
                    && (itemData.health <= 0 || itemData.health > DroppedItem.MAX_HEALTH)) {
                return "invalid dropped item health";
            }
        }
        return null;
    }

    private String validateTileEntityTypes(List<TileEntityData> tileEntities) {
        if (tileEntities == null) {
            return null;
        }
        for (TileEntityData tileData : tileEntities) {
            if (tileData.type == null || !TILE_ENTITY_TYPE_IDS.contains(tileData.type)) {
                return "unknown tile entity type: " + tileData.type;
            }
            String tileStateError = validateTileEntityState(tileData);
            if (tileStateError != null) {
                return tileStateError;
            }
            String stackError = validateStackArray(tileData.type + " inventory", tileData.inventory);
            if (stackError != null) {
                return stackError;
            }
            stackError = validateStack(tileData.type + " record", tileData.record);
            if (stackError != null) {
                return stackError;
            }
            if ("mob_spawner".equals(tileData.type)
                    && tileData.mobType != null
                    && !isImplementedMobDefinition(tileData.mobType)) {
                return "unknown mob spawner type: " + tileData.mobType;
            }
        }
        return null;
    }

    private String validateTileEntityState(TileEntityData tileData) {
        return switch (tileData.type) {
            case "chest" -> validateChestTileEntity(tileData);
            case "furnace" -> validateFurnaceTileEntity(tileData);
            case "brewing_stand" -> validateBrewingStandTileEntity(tileData);
            case "dispenser" -> validateSizedTileInventory(tileData, DispenserTileEntity.SIZE, "invalid dispenser inventory");
            case "note_block" -> validateNoteBlockTileEntity(tileData);
            case "jukebox" -> validateJukeboxTileEntity(tileData);
            case "enchanting_table" -> validateEnchantingTableTileEntity(tileData);
            case "sign" -> validateSignTileEntity(tileData);
            case "mob_spawner" -> validateMonsterSpawnerTileEntity(tileData);
            default -> null;
        };
    }

    private static String validateChestTileEntity(TileEntityData tileData) {
        String inventoryError = validateSizedTileInventory(tileData, ChestTileEntity.SIZE, "invalid chest inventory");
        if (inventoryError != null) {
            return inventoryError;
        }
        if (tileData.lidAngle < 0.0f || tileData.lidAngle > 1.0f || !isFinite(tileData.lidAngle)) {
            return "invalid chest lid angle";
        }
        return null;
    }

    private static String validateFurnaceTileEntity(TileEntityData tileData) {
        String inventoryError = validateSizedTileInventory(tileData, FurnaceTileEntity.SIZE, "invalid furnace inventory");
        if (inventoryError != null) {
            return inventoryError;
        }
        if (tileData.burnTime < 0) {
            return "invalid furnace burn time";
        }
        if (tileData.currentFuelBurnTime < 0) {
            return "invalid furnace fuel time";
        }
        if (tileData.burnTime > 0 && tileData.currentFuelBurnTime <= 0) {
            return "invalid furnace fuel time";
        }
        if (tileData.cookTime < 0 || tileData.cookTime > FurnaceTileEntity.COOK_TIME_TOTAL) {
            return "invalid furnace cook time";
        }
        if (!isUnitInterval(tileData.furnaceTickAccumulator)) {
            return "invalid furnace tick accumulator";
        }
        return null;
    }

    private static String validateBrewingStandTileEntity(TileEntityData tileData) {
        String inventoryError = validateSizedTileInventory(tileData, BrewingStandTileEntity.SIZE,
                "invalid brewing stand inventory");
        if (inventoryError != null) {
            return inventoryError;
        }
        if (tileData.brewTime < 0 || tileData.brewTime > BrewingStandTileEntity.BREW_TIME_TOTAL) {
            return "invalid brewing stand brew time";
        }
        if (!isUnitInterval(tileData.brewingTickAccumulator)) {
            return "invalid brewing stand tick accumulator";
        }
        return null;
    }

    private static String validateNoteBlockTileEntity(TileEntityData tileData) {
        if (tileData.notePitch < 0 || tileData.notePitch > 24) {
            return "invalid note block pitch";
        }
        if (tileData.noteInstrument < NoteBlockTileEntity.INSTRUMENT_HARP
                || tileData.noteInstrument > NoteBlockTileEntity.INSTRUMENT_BASS) {
            return "invalid note block instrument";
        }
        if (tileData.playTicks < 0) {
            return "invalid note block play ticks";
        }
        return null;
    }

    private String validateJukeboxTileEntity(TileEntityData tileData) {
        if (tileData.playTicks < 0) {
            return "invalid jukebox play ticks";
        }
        if (tileData.record == null) {
            return null;
        }
        String recordError = validateStack("jukebox record", tileData.record);
        if (recordError != null) {
            return recordError;
        }
        ItemType recordType = ItemType.fromId(tileData.record.itemId, tileData.record.dataValue);
        if (recordType == null || !recordType.isRecord()) {
            return "invalid jukebox record";
        }
        if (tileData.record.count != 1) {
            return "invalid jukebox record count";
        }
        return null;
    }

    private static String validateEnchantingTableTileEntity(TileEntityData tileData) {
        if (tileData.enchantingTickCount < 0) {
            return "invalid enchanting table tick count";
        }
        if (!isFinite(tileData.enchantingPageFlip)
                || !isFinite(tileData.enchantingPrevPageFlip)
                || !isFinite(tileData.enchantingPageFlipTarget)
                || !isFinite(tileData.enchantingPageFlipVelocity)
                || !isFinite(tileData.enchantingBookRotation)
                || !isFinite(tileData.enchantingBookRotation2)
                || !isFinite(tileData.enchantingPrevBookRotation)) {
            return "invalid enchanting table animation";
        }
        if (!isUnitInterval(tileData.enchantingBookSpread)
                || !isUnitInterval(tileData.enchantingPrevBookSpread)
                || !isUnitInterval(tileData.enchantingTickAccumulator)) {
            return "invalid enchanting table animation";
        }
        return null;
    }

    private static String validateSignTileEntity(TileEntityData tileData) {
        if (tileData.signText != null && tileData.signText.length != 4) {
            return "invalid sign text";
        }
        return null;
    }

    private String validateMonsterSpawnerTileEntity(TileEntityData tileData) {
        if (tileData.mobType != null && !isImplementedMobDefinition(tileData.mobType)) {
            return "unknown mob spawner type: " + tileData.mobType;
        }
        if (tileData.spawnDelay < 0) {
            return "invalid mob spawner delay";
        }
        if (tileData.minSpawnDelay < 0 || tileData.maxSpawnDelay < 0) {
            return "invalid mob spawner delay range";
        }
        if ((tileData.minSpawnDelay == 0) != (tileData.maxSpawnDelay == 0)) {
            return "invalid mob spawner delay range";
        }
        if (tileData.minSpawnDelay > 0 && tileData.maxSpawnDelay < tileData.minSpawnDelay) {
            return "invalid mob spawner delay range";
        }
        if (tileData.spawnCount < 0 || tileData.maxNearbyEntities < 0) {
            return "invalid mob spawner counts";
        }
        if (!isUnitInterval(tileData.spawnerTickAccumulator)) {
            return "invalid mob spawner tick accumulator";
        }
        return null;
    }

    private static String validateSizedTileInventory(TileEntityData tileData, int expectedLength, String error) {
        return hasExpectedLength(tileData.inventory, expectedLength) ? null : error;
    }

    private String validateScheduledBlockTicks(List<ScheduledBlockTickData> ticks) {
        if (ticks == null) {
            return null;
        }
        for (ScheduledBlockTickData tick : ticks) {
            if (tick.delayTicks < 0) {
                return "invalid scheduled block tick delay";
            }
            if (tick.y < 0 || tick.y >= Chunk.HEIGHT) {
                return "invalid scheduled block tick position";
            }
            BlockType type = BlockType.fromId(tick.blockId);
            if (type == null || !isScheduledTickBlock(type)) {
                return "invalid scheduled block tick type";
            }
        }
        return null;
    }

    private String validateMovingPistons(List<MovingPistonData> pistons) {
        if (pistons == null) {
            return null;
        }
        for (MovingPistonData piston : pistons) {
            String error = validateMovingPiston(piston);
            if (error != null) {
                return error;
            }
        }
        return null;
    }

    private String validateMovingPiston(MovingPistonData piston) {
        if (piston.y < 0 || piston.y >= Chunk.HEIGHT) {
            return "invalid moving piston position";
        }
        if (!isValidFace(piston.facing)) {
            return "invalid moving piston facing";
        }
        BlockType carried = BlockType.fromId(piston.carriedBlockId);
        BlockType finalType = BlockType.fromId(piston.finalBlockId);
        if (carried == null || finalType == null) {
            return "invalid moving piston block";
        }
        if (piston.carriedMetadata < 0 || piston.carriedMetadata > 15
                || piston.finalMetadata < 0 || piston.finalMetadata > 15) {
            return "invalid moving piston metadata";
        }
        if (!isFinite(piston.fromX)
                || !isFinite(piston.fromY)
                || !isFinite(piston.fromZ)
                || !isFinite(piston.toX)
                || !isFinite(piston.toY)
                || !isFinite(piston.toZ)) {
            return "invalid moving piston path";
        }
        if (piston.elapsedTicks < 0 || piston.elapsedTicks > RedstoneEngine.PISTON_MOVEMENT_TICKS) {
            return "invalid moving piston elapsed ticks";
        }
        return null;
    }

    private String validateEntityTypes(List<EntityData> entities) {
        if (entities == null) {
            return null;
        }
        for (EntityData entityData : entities) {
            if (entityData.type == null) {
                return "missing entity type";
            }
            String runtimeError = validateEntityRuntimeData(entityData);
            if (runtimeError != null) {
                return runtimeError;
            }
            String stackError = validateStackArray(entityData.type + " inventory", entityData.inventory);
            if (stackError != null) {
                return stackError;
            }
            String effectError = validateStatusEffects(entityData.type + " effects", entityData.activeEffects);
            if (effectError != null) {
                return effectError;
            }
            if (ENTITY_TYPE_IDS.contains(entityData.type)) {
                if ("FIREBALL".equals(entityData.type)) {
                    String fireballError = validateFireballEntity(entityData);
                    if (fireballError != null) {
                        return fireballError;
                    }
                }
                if ("FALLING_BLOCK".equals(entityData.type)) {
                    BlockType fallingBlock = BlockType.fromId(entityData.fallingBlockId);
                    if (fallingBlock == null) {
                        return "unknown falling block type: " + entityData.fallingBlockId;
                    }
                    if (!fallingBlock.isFallingBlock()) {
                        return "invalid falling block entity";
                    }
                    if (entityData.fallingBlockMetadata < 0 || entityData.fallingBlockMetadata > 15) {
                        return "invalid falling block metadata";
                    }
                }
                if ("ARROW".equals(entityData.type)) {
                    String arrowError = validateArrowEntity(entityData);
                    if (arrowError != null) {
                        return arrowError;
                    }
                }
                if ("FISHING_HOOK".equals(entityData.type)) {
                    String fishingHookError = validateFishingHookEntity(entityData);
                    if (fishingHookError != null) {
                        return fishingHookError;
                    }
                }
                if ("ENDER_PEARL".equals(entityData.type)) {
                    String pearlError = validateEnderPearlEntity(entityData);
                    if (pearlError != null) {
                        return pearlError;
                    }
                }
                if ("THROWN_ITEM".equals(entityData.type)) {
                    String projectileError = validateItemPayload("thrown item",
                            entityData.projectileItemId, entityData.projectileDataValue, 1);
                    if (projectileError != null) {
                        return projectileError;
                    }
                    ItemType projectileType = ItemType.fromId(entityData.projectileItemId,
                            entityData.projectileDataValue);
                    if (!isValidThrownItemProjectile(projectileType)) {
                        return "invalid thrown item projectile";
                    }
                    String thrownItemError = validateThrownItemEntity(entityData);
                    if (thrownItemError != null) {
                        return thrownItemError;
                    }
                }
                if ("SPLASH_POTION".equals(entityData.type)) {
                    String potionError = validatePotionData("splash potion", entityData.potion);
                    if (potionError != null) {
                        return potionError;
                    }
                    String splashPotionError = validateSplashPotionEntity(entityData);
                    if (splashPotionError != null) {
                        return splashPotionError;
                    }
                }
                if ("MINECART".equals(entityData.type)) {
                    String minecartError = validateMinecartEntity(entityData);
                    if (minecartError != null) {
                        return minecartError;
                    }
                }
                if ("PAINTING".equals(entityData.type)) {
                    String paintingError = validatePaintingEntity(entityData);
                    if (paintingError != null) {
                        return paintingError;
                    }
                }
                if ("EXPERIENCE_ORB".equals(entityData.type)) {
                    String orbError = validateExperienceOrbEntity(entityData);
                    if (orbError != null) {
                        return orbError;
                    }
                }
                if ("EYE_OF_ENDER".equals(entityData.type)) {
                    String eyeError = validateEyeOfEnderEntity(entityData);
                    if (eyeError != null) {
                        return eyeError;
                    }
                }
                if ("PRIMED_TNT".equals(entityData.type)) {
                    String tntError = validatePrimedTntEntity(entityData);
                    if (tntError != null) {
                        return tntError;
                    }
                }
                if ("BOAT".equals(entityData.type)) {
                    String boatError = validateBoatEntity(entityData);
                    if (boatError != null) {
                        return boatError;
                    }
                }
                if ("END_CRYSTAL".equals(entityData.type)) {
                    String crystalError = validateEndCrystalEntity(entityData);
                    if (crystalError != null) {
                        return crystalError;
                    }
                }
                continue;
            }
            MobDefinition definition = parseMobDefinition(entityData.type);
            if (definition == null || !MobFactory.isImplemented(definition)) {
                return "unknown entity type: " + entityData.type;
            }
            String mobError = validateMobEntity(entityData, definition);
            if (mobError != null) {
                return mobError;
            }
        }
        return null;
    }

    private String validateEntityReferences(List<EntityData> entities) {
        if (entities == null) {
            return null;
        }
        Map<Integer, EntityData> entitiesById = new HashMap<>();
        for (EntityData entityData : entities) {
            if (entityData.entitySaveId < 0) {
                return "invalid entity reference id";
            }
            if (entityData.entitySaveId > 0
                    && entitiesById.put(entityData.entitySaveId, entityData) != null) {
                return "duplicate entity reference id";
            }
        }

        for (EntityData entityData : entities) {
            String mobReferenceError = validateMobTargetReference(entityData, entitiesById);
            if (mobReferenceError != null) {
                return mobReferenceError;
            }
            if ("FISHING_HOOK".equals(entityData.type)) {
                String fishingReferenceError = validateFishingHookReferences(entityData, entitiesById);
                if (fishingReferenceError != null) {
                    return fishingReferenceError;
                }
            }
            if ("MINECART".equals(entityData.type)) {
                String passengerReferenceError = validateMinecartPassengerReference(entityData, entitiesById);
                if (passengerReferenceError != null) {
                    return passengerReferenceError;
                }
            }
            String jockeyReferenceError = validateSpiderJockeyReference(entityData, entitiesById);
            if (jockeyReferenceError != null) {
                return jockeyReferenceError;
            }
            String projectileReferenceError = validateProjectileShooterReference(entityData, entitiesById);
            if (projectileReferenceError != null) {
                return projectileReferenceError;
            }
        }
        return null;
    }

    private static String validatePlayerEntityReferences(PlayerData player, List<EntityData> entities) {
        if (player == null || player.ridingEntitySaveId == 0) {
            return null;
        }
        Map<Integer, EntityData> entitiesById = new HashMap<>();
        if (entities != null) {
            for (EntityData entityData : entities) {
                if (entityData != null && entityData.entitySaveId > 0) {
                    entitiesById.put(entityData.entitySaveId, entityData);
                }
            }
        }
        EntityData mount = entitiesById.get(player.ridingEntitySaveId);
        if (mount == null) {
            return "invalid player riding entity";
        }
        String type = player.ridingEntityType == null ? "" : player.ridingEntityType;
        if ("BOAT".equals(type)) {
            return "BOAT".equals(mount.type) ? null : "invalid player riding entity";
        }
        if ("MINECART".equals(type)) {
            if (!"MINECART".equals(mount.type)
                    || EntityData.parseCartKind(mount.cartKind) != MinecartEntity.CartKind.RIDEABLE) {
                return "invalid player riding entity";
            }
            return null;
        }
        if ("PIG".equals(type)) {
            if (parseMobDefinition(mount.type) != MobDefinition.PIG || !mount.saddled) {
                return "invalid player riding entity";
            }
            return null;
        }
        return "invalid player riding entity";
    }

    private static String validateMobTargetReference(EntityData entityData,
                                                     Map<Integer, EntityData> entitiesById) {
        if (entityData.mobTargetSaveId < 0) {
            return "invalid mob target";
        }
        if (entityData.mobTargetSaveId == 0) {
            return null;
        }
        MobDefinition definition = parseMobDefinition(entityData.type);
        if (definition == null || !MobFactory.isImplemented(definition)) {
            return "invalid mob target";
        }
        if (entityData.entitySaveId > 0 && entityData.mobTargetSaveId == entityData.entitySaveId) {
            return "invalid mob target";
        }
        if (!isSavedLivingEntityType(entitiesById.get(entityData.mobTargetSaveId))) {
            return "invalid mob target";
        }
        return null;
    }

    private static String validateFishingHookReferences(EntityData entityData,
                                                        Map<Integer, EntityData> entitiesById) {
        if (entityData.fishingHookedEntitySaveId < 0) {
            return "invalid fishing hook target";
        }
        if (entityData.fishingHookedEntitySaveId == 0) {
            return null;
        }
        if (entityData.entitySaveId > 0
                && entityData.fishingHookedEntitySaveId == entityData.entitySaveId) {
            return "invalid fishing hook target";
        }
        EntityData target = entitiesById.get(entityData.fishingHookedEntitySaveId);
        if (!isFishingHookTargetType(target)) {
            return "invalid fishing hook target";
        }
        return null;
    }

    private static boolean isFishingHookTargetType(EntityData entityData) {
        if (entityData == null) {
            return false;
        }
        if ("BOAT".equals(entityData.type) || "MINECART".equals(entityData.type)) {
            return true;
        }
        return isSavedLivingEntityType(entityData);
    }

    private static String validateMinecartPassengerReference(EntityData entityData,
                                                             Map<Integer, EntityData> entitiesById) {
        if (entityData.minecartPassengerSaveId < 0) {
            return "invalid minecart passenger";
        }
        if (entityData.minecartPassengerSaveId == 0) {
            return null;
        }
        if (EntityData.parseCartKind(entityData.cartKind) != MinecartEntity.CartKind.RIDEABLE) {
            return "invalid minecart passenger";
        }
        if (entityData.entitySaveId > 0 && entityData.minecartPassengerSaveId == entityData.entitySaveId) {
            return "invalid minecart passenger";
        }
        if (!isSavedLivingEntityType(entitiesById.get(entityData.minecartPassengerSaveId))) {
            return "invalid minecart passenger";
        }
        return null;
    }

    private static String validateSpiderJockeyReference(EntityData entityData,
                                                        Map<Integer, EntityData> entitiesById) {
        if (entityData.spiderJockeyRiderSaveId < 0) {
            return "invalid spider jockey rider";
        }
        if (entityData.spiderJockeyRiderSaveId == 0) {
            return null;
        }
        if (parseMobDefinition(entityData.type) != MobDefinition.SPIDER) {
            return "invalid spider jockey rider";
        }
        if (entityData.entitySaveId > 0 && entityData.spiderJockeyRiderSaveId == entityData.entitySaveId) {
            return "invalid spider jockey rider";
        }
        EntityData rider = entitiesById.get(entityData.spiderJockeyRiderSaveId);
        if (rider == null || parseMobDefinition(rider.type) != MobDefinition.SKELETON) {
            return "invalid spider jockey rider";
        }
        return null;
    }

    private static boolean isSavedLivingEntityType(EntityData entityData) {
        if (entityData == null) {
            return false;
        }
        MobDefinition definition = parseMobDefinition(entityData.type);
        return definition != null && MobFactory.isImplemented(definition);
    }

    private String validateStackArray(String context, StackData[] stacks) {
        if (stacks == null) {
            return null;
        }
        for (int i = 0; i < stacks.length; i++) {
            String error = validateStack(context + "[" + i + "]", stacks[i]);
            if (error != null) {
                return error;
            }
        }
        return null;
    }

    private String validateStack(String context, StackData stack) {
        if (stack == null) {
            return null;
        }
        String error = validateItemPayload(context, stack.itemId, stack.dataValue,
                stack.count, stack.durability, stack.potion);
        if (error != null) {
            return error;
        }
        return validateEnchantments(context, ItemType.fromId(stack.itemId, stack.dataValue), stack.enchantments);
    }

    private String validateItemPayload(String context, int itemId, int dataValue, int count) {
        return validateItemPayload(context, itemId, dataValue, count, -1, null);
    }

    private String validateItemPayload(String context, int itemId, int dataValue, int count, PotionData potion) {
        return validateItemPayload(context, itemId, dataValue, count, -1, potion);
    }

    private String validateItemPayload(String context, int itemId, int dataValue,
                                       int count, int durability, PotionData potion) {
        ItemType type = ItemType.fromId(itemId, dataValue);
        if (type == null) {
            return "unknown item in " + context + ": " + itemId + ":" + dataValue;
        }
        if (count <= 0) {
            return "invalid item count in " + context + ": " + count;
        }
        if (count > type.getMaxStackSize()) {
            return "invalid item count in " + context + ": " + count
                    + " > " + type.getMaxStackSize();
        }
        String durabilityError = validateItemDurability(context, type, durability);
        if (durabilityError != null) {
            return durabilityError;
        }
        if (potion != null) {
            if (type != ItemType.POTION) {
                return "invalid potion data in " + context;
            }
            String potionError = validatePotionData(context, potion);
            if (potionError != null) {
                return potionError;
            }
        }
        return null;
    }

    private String validatePotionData(String context, PotionData potion) {
        if (potion == null) {
            return null;
        }
        if (!VALID_POTION_DATA.contains(potion)) {
            return "invalid potion data in " + context;
        }
        return null;
    }

    private String validateItemDurability(String context, ItemType type, int durability) {
        if (type.isDamageable()) {
            if (durability <= 0 || durability > type.getMaxDurability()) {
                return "invalid item durability in " + context + ": " + durability;
            }
            return null;
        }
        if (type == ItemType.MAP) {
            if (durability < -1) {
                return "invalid item durability in " + context + ": " + durability;
            }
            return null;
        }
        if (durability != -1) {
            return "invalid item durability in " + context + ": " + durability;
        }
        return null;
    }

    private String validateEnchantments(String context, ItemType itemType,
                                        List<EnchantmentInstance> enchantments) {
        if (enchantments == null) {
            return null;
        }
        List<EnchantmentType> seen = new ArrayList<>();
        for (EnchantmentInstance enchantment : enchantments) {
            if (enchantment == null || enchantment.type() == null) {
                return "invalid enchantment in " + context;
            }
            if (enchantment.level() <= 0) {
                return "invalid enchantment level in " + context;
            }
            if (enchantment.level() > maxEnchantmentLevel(enchantment.type())) {
                return "invalid enchantment level in " + context;
            }
            if (!canApplyEnchantment(itemType, enchantment.type())) {
                return "invalid enchantment in " + context;
            }
            for (EnchantmentType existing : seen) {
                if (!EnchantmentResolver.compatible(existing, enchantment.type())) {
                    return "invalid enchantment combination in " + context;
                }
            }
            seen.add(enchantment.type());
        }
        return null;
    }

    private int maxEnchantmentLevel(EnchantmentType type) {
        return switch (type) {
            case PROTECTION, FIRE_PROTECTION, FEATHER_FALLING, BLAST_PROTECTION,
                    PROJECTILE_PROTECTION -> 4;
            case RESPIRATION, LOOTING, UNBREAKING, FORTUNE -> 3;
            case AQUA_AFFINITY, SILK_TOUCH -> 1;
            case SHARPNESS, SMITE, BANE_OF_ARTHROPODS, EFFICIENCY -> 5;
            case KNOCKBACK, FIRE_ASPECT -> 2;
            case POWER, PUNCH, FLAME, INFINITY -> 0;
        };
    }

    private boolean canApplyEnchantment(ItemType itemType, EnchantmentType enchantmentType) {
        ArmorMaterial armor = ArmorMaterial.materialOf(itemType);
        if (armor != null) {
            ArmorSlot slot = ArmorMaterial.slotOf(itemType);
            return switch (enchantmentType) {
                case PROTECTION, FIRE_PROTECTION, BLAST_PROTECTION,
                        PROJECTILE_PROTECTION, UNBREAKING -> true;
                case FEATHER_FALLING -> slot == ArmorSlot.BOOTS;
                case RESPIRATION, AQUA_AFFINITY -> slot == ArmorSlot.HELMET;
                default -> false;
            };
        }
        if (itemType == null || !itemType.isTool()) {
            return false;
        }
        ToolType.Category category = itemType.getToolType().getCategory();
        if (category == ToolType.Category.SWORD) {
            return switch (enchantmentType) {
                case SHARPNESS, SMITE, BANE_OF_ARTHROPODS,
                        KNOCKBACK, FIRE_ASPECT, LOOTING, UNBREAKING -> true;
                default -> false;
            };
        }
        if (category == ToolType.Category.PICKAXE
                || category == ToolType.Category.SHOVEL
                || category == ToolType.Category.AXE) {
            return switch (enchantmentType) {
                case EFFICIENCY, SILK_TOUCH, FORTUNE, UNBREAKING -> true;
                default -> false;
            };
        }
        return false;
    }

    private static boolean isValidThrownItemProjectile(ItemType itemType) {
        return itemType == ItemType.EGG || itemType == ItemType.SNOWBALL;
    }

    private static String validateEntityRuntimeData(EntityData entityData) {
        if (entityData.age < 0) {
            return "invalid entity age";
        }
        if (!isFinite(entityData.x)
                || !isFinite(entityData.y)
                || !isFinite(entityData.z)
                || !isFinite(entityData.motionX)
                || !isFinite(entityData.motionY)
                || !isFinite(entityData.motionZ)
                || !isFinite(entityData.yaw)
                || !isFinite(entityData.pitch)
                || (entityData.fallStartY != null && !isFinite(entityData.fallStartY))) {
            return "invalid entity numeric state";
        }
        return null;
    }

    private static String validateArrowEntity(EntityData entityData) {
        if (entityData.damage < 0.0f || !isFinite(entityData.damage)) {
            return "invalid arrow damage";
        }
        if (entityData.knockbackHorizontal < 0.0f
                || entityData.knockbackVertical < 0.0f
                || !isFinite(entityData.knockbackHorizontal)
                || !isFinite(entityData.knockbackVertical)) {
            return "invalid arrow knockback";
        }
        if (entityData.fireTicksOnHit < 0) {
            return "invalid arrow fire ticks";
        }
        if (entityData.stuckTicks < 0) {
            return "invalid arrow stuck ticks";
        }
        if (!entityData.inGround && entityData.stuckTicks != 0) {
            return "invalid arrow stuck state";
        }
        if (entityData.inGround && entityData.stuckTicks >= ArrowEntity.STUCK_DESPAWN_TICKS) {
            return "invalid arrow stuck ticks";
        }
        if (entityData.inGround && (entityData.blockY < 0 || entityData.blockY >= Chunk.HEIGHT)) {
            return "invalid arrow stuck block";
        }
        return null;
    }

    private static String validateFishingHookEntity(EntityData entityData) {
        if (!entityData.ownerPlayer) {
            return "invalid fishing hook owner";
        }
        if (entityData.age >= FishingHookEntity.DESPAWN_TICKS) {
            return "invalid fishing hook age";
        }
        if (entityData.fishingWaitTicks < 0
                || entityData.fishingWaitTicks > RELEASE_FISHING_MAX_WAIT_TICKS) {
            return "invalid fishing hook wait ticks";
        }
        if (entityData.fishingCatchableTicks < 0
                || entityData.fishingCatchableTicks > RELEASE_FISHING_MAX_CATCHABLE_TICKS) {
            return "invalid fishing hook catchable ticks";
        }
        if (entityData.fishingWaitTicks > 0 && entityData.fishingCatchableTicks > 0) {
            return "invalid fishing hook phase";
        }
        return null;
    }

    private String validateMinecartEntity(EntityData entityData) {
        MinecartEntity.CartKind kind = EntityData.parseCartKind(entityData.cartKind);
        if (kind == null) {
            return "invalid minecart kind";
        }
        if (entityData.cartDamage < 0.0f
                || entityData.cartDamage > MinecartEntity.BREAK_DAMAGE
                || !isFinite(entityData.cartDamage)) {
            return "invalid minecart damage";
        }
        String rollingError = validateVehicleRollingState(entityData.rollingAmplitude,
                entityData.rollingDirection, MinecartEntity.HIT_ROLLING_TICKS, "minecart");
        if (rollingError != null) {
            return rollingError;
        }
        if (kind == MinecartEntity.CartKind.CHEST) {
            if (!hasExpectedLength(entityData.inventory, ChestMinecartEntity.SIZE)) {
                return "invalid minecart inventory";
            }
        } else if (entityData.inventory != null) {
            return "invalid minecart inventory";
        }
        if (kind == MinecartEntity.CartKind.FURNACE) {
            if (entityData.fuelTicks < 0 || entityData.fuelTicks > FurnaceMinecartEntity.MAX_FUEL_TICKS) {
                return "invalid furnace minecart fuel";
            }
            if (!isFinite(entityData.pushX) || !isFinite(entityData.pushZ)) {
                return "invalid furnace minecart push";
            }
        } else if (entityData.fuelTicks != 0 || entityData.pushX != 0.0f || entityData.pushZ != 0.0f) {
            return "invalid minecart furnace state";
        }
        return null;
    }

    private String validatePaintingEntity(EntityData entityData) {
        if (!isValidPaintingArt(entityData.paintingArt)) {
            return "invalid painting art";
        }
        if (!isHorizontalFace(entityData.paintingFacing)) {
            return "invalid painting facing";
        }
        return null;
    }

    private static boolean isValidPaintingArt(String motive) {
        if (motive == null || motive.isBlank()) {
            return false;
        }
        for (PaintingEntity.Art art : PaintingEntity.Art.values()) {
            if (art.motive().equals(motive) || art.name().equalsIgnoreCase(motive)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isHorizontalFace(int face) {
        return face == Block.FACE_NORTH
                || face == Block.FACE_SOUTH
                || face == Block.FACE_EAST
                || face == Block.FACE_WEST;
    }

    private static String validateExperienceOrbEntity(EntityData entityData) {
        if (entityData.experienceValue <= 0) {
            return "invalid experience orb value";
        }
        if (entityData.age >= ExperienceOrbEntity.DESPAWN_TICKS) {
            return "invalid experience orb age";
        }
        if (entityData.pickupDelayTicks < 0) {
            return "invalid experience orb pickup delay";
        }
        if (entityData.orbHealth <= 0 || entityData.orbHealth > ExperienceOrbEntity.MAX_HEALTH) {
            return "invalid experience orb health";
        }
        return null;
    }

    private static String validateEyeOfEnderEntity(EntityData entityData) {
        if (entityData.age > EyeOfEnderEntity.LIFE_TICKS) {
            return "invalid Eye of Ender age";
        }
        if (!isFinite(entityData.targetX)
                || !isFinite(entityData.targetY)
                || !isFinite(entityData.targetZ)) {
            return "invalid Eye of Ender target";
        }
        return null;
    }

    private static String validateEnderPearlEntity(EntityData entityData) {
        if (!entityData.ownerPlayer) {
            return "invalid Ender pearl owner";
        }
        if (entityData.age >= EnderPearlEntity.DESPAWN_TICKS) {
            return "invalid Ender pearl age";
        }
        return null;
    }

    private static String validateFireballEntity(EntityData entityData) {
        if (entityData.age > FireballEntity.DESPAWN_TICKS) {
            return "invalid fireball age";
        }
        return null;
    }

    private static String validateThrownItemEntity(EntityData entityData) {
        if (entityData.age >= ThrownItemEntity.DESPAWN_TICKS) {
            return "invalid thrown item age";
        }
        return null;
    }

    private static String validateSplashPotionEntity(EntityData entityData) {
        if (entityData.age >= SplashPotionEntity.DESPAWN_TICKS) {
            return "invalid splash potion age";
        }
        return null;
    }

    private static String validatePrimedTntEntity(EntityData entityData) {
        if (entityData.fuseTicksPresent
                && (entityData.fuseTicks < 0 || entityData.fuseTicks > RedstoneEngine.TNT_FUSE_TICKS)) {
            return "invalid primed TNT fuse";
        }
        return null;
    }

    private static String validateBoatEntity(EntityData entityData) {
        if (entityData.boatDamage < 0.0f
                || entityData.boatDamage > BoatEntity.BREAK_DAMAGE
                || !isFinite(entityData.boatDamage)) {
            return "invalid boat damage";
        }
        String rollingError = validateVehicleRollingState(entityData.rollingAmplitude,
                entityData.rollingDirection, BoatEntity.HIT_ROLLING_TICKS, "boat");
        if (rollingError != null) {
            return rollingError;
        }
        return null;
    }

    private static String validateProjectileShooterReference(EntityData entityData,
                                                            Map<Integer, EntityData> entitiesById) {
        if (entityData.projectileShooterSaveId < 0) {
            return "invalid projectile shooter";
        }
        if (entityData.projectileShooterSaveId == 0) {
            return null;
        }
        if (!isProjectileWithSavedShooter(entityData.type)) {
            return "invalid projectile shooter";
        }
        if ("FIREBALL".equals(entityData.type) && entityData.ownerPlayer) {
            return "invalid projectile shooter";
        }
        if (entityData.entitySaveId > 0 && entityData.projectileShooterSaveId == entityData.entitySaveId) {
            return "invalid projectile shooter";
        }
        if (!isSavedLivingEntityType(entitiesById.get(entityData.projectileShooterSaveId))) {
            return "invalid projectile shooter";
        }
        return null;
    }

    private static boolean isProjectileWithSavedShooter(String type) {
        return "ARROW".equals(type)
                || "FIREBALL".equals(type)
                || "THROWN_ITEM".equals(type)
                || "SPLASH_POTION".equals(type);
    }

    private static String validateVehicleRollingState(int amplitude, int direction, int maxAmplitude, String type) {
        if (amplitude < 0 || amplitude > maxAmplitude) {
            return "invalid " + type + " rolling amplitude";
        }
        if (direction != 0 && direction != -1 && direction != 1) {
            return "invalid " + type + " rolling direction";
        }
        return null;
    }

    private static String validateEndCrystalEntity(EntityData entityData) {
        String healthError = validateSavedHealth("end crystal health", entityData.health, 5.0f);
        if (healthError != null) {
            return healthError;
        }
        if (entityData.health <= 0.0f) {
            return "invalid end crystal health";
        }
        if (entityData.activeEffects != null && !entityData.activeEffects.isEmpty()) {
            return "invalid end crystal effects";
        }
        return null;
    }

    private static String validateMobEntity(EntityData entityData, MobDefinition definition) {
        String healthError = validateSavedHealth("mob health", entityData.health,
                maxSavedHealth(definition, entityData));
        if (healthError != null) {
            return healthError;
        }
        if (definition != MobDefinition.ENDER_DRAGON && entityData.health <= 0.0f) {
            return "invalid mob health";
        }
        if (entityData.fireTicks < 0) {
            return "invalid mob fire ticks";
        }
        if (entityData.livingAttackCooldown < 0) {
            return "invalid mob attack cooldown";
        }
        String damageStateError = validateLivingDamageState(entityData);
        if (damageStateError != null) {
            return damageStateError;
        }
        if (entityData.growingAge < Mob.BABY_GROWING_AGE
                || entityData.growingAge > Mob.BREEDING_COOLDOWN_AGE) {
            return "invalid mob growing age";
        }
        if (entityData.loveTicks < 0 || entityData.loveTicks > Mob.LOVE_MODE_TICKS) {
            return "invalid mob love ticks";
        }
        if (entityData.loveTicks > 0 && entityData.growingAge != 0) {
            return "invalid mob breeding state";
        }
        String aiError = validateMobAiState(entityData);
        if (aiError != null) {
            return aiError;
        }
        return switch (definition) {
            case SLIME, MAGMA_CUBE -> validateSlimeLikeEntity(entityData);
            case CHICKEN -> entityData.eggTimer < 0 ? "invalid chicken egg timer" : null;
            case BLAZE -> validateBlazeEntity(entityData);
            case GHAST -> validateGhastEntity(entityData);
            case SQUID -> validateSquidEntity(entityData);
            case ENDER_DRAGON -> validateEnderDragonEntity(entityData);
            case CREEPER -> validateCreeperEntity(entityData);
            case SHEEP -> validateSheepEntity(entityData);
            case VILLAGER -> validateVillagerEntity(entityData);
            case ENDERMAN -> validateEndermanEntity(entityData);
            case SNOW_GOLEM -> entityData.snowGolemAttackCooldown < 0
                    ? "invalid snow golem attack cooldown" : null;
            case ZOMBIE_PIGMAN -> entityData.angerTicks < 0 ? "invalid zombie pigman anger" : null;
            case WOLF -> validateWolfEntity(entityData);
            default -> null;
        };
    }

    private static String validateWolfEntity(EntityData entityData) {
        if (entityData.wolfSitting && !entityData.tamed) {
            return "invalid wolf sitting state";
        }
        if (!Wolf.isValidSavedShakeTime(entityData.wolfShakeTime)
                || !Wolf.isValidSavedShakeTime(entityData.wolfPrevShakeTime)) {
            return "invalid wolf shake state";
        }
        if (entityData.wolfShaking && !entityData.wolfWet) {
            return "invalid wolf shake state";
        }
        if (entityData.wolfPrevShakeTime > entityData.wolfShakeTime) {
            return "invalid wolf shake state";
        }
        return null;
    }

    private static String validateSavedHealth(String context, float health, float maxHealth) {
        if (!isFinite(health)) {
            return "invalid " + context;
        }
        if (health < 0.0f || health > maxHealth) {
            return "invalid " + context;
        }
        return null;
    }

    private static String validateLivingDamageState(EntityData entityData) {
        if (entityData.hurtTime < 0 || entityData.hurtTime > LivingEntity.MAX_HURT_TIME) {
            return "invalid mob hurt time";
        }
        if (entityData.invulnerableTime < 0
                || entityData.invulnerableTime > LivingEntity.MAX_INVULNERABLE_TIME) {
            return "invalid mob invulnerability time";
        }
        if (entityData.lastDamageAmount < 0.0f || !isFinite(entityData.lastDamageAmount)) {
            return "invalid mob recent damage";
        }
        if (entityData.recentPlayerHitTicks < 0
                || entityData.recentPlayerHitTicks > LivingEntity.RECENT_PLAYER_HIT_TICKS) {
            return "invalid mob player-hit timer";
        }
        if (entityData.recentPlayerLootingLevel < 0
                || entityData.recentPlayerLootingLevel > MAX_SAVED_LOOTING_LEVEL
                || (entityData.recentPlayerHitTicks == 0 && entityData.recentPlayerLootingLevel != 0)) {
            return "invalid mob player-hit looting";
        }
        return null;
    }

    private static float maxSavedHealth(MobDefinition definition, EntityData entityData) {
        if (definition == MobDefinition.WOLF && entityData.tamed) {
            return TAMED_WOLF_MAX_HEALTH;
        }
        if (definition == MobDefinition.SLIME || definition == MobDefinition.MAGMA_CUBE) {
            if (!isValidSavedSlimeSize(entityData.slimeSize)) {
                return definition.maxHealth();
            }
            int size = normalizedSavedSlimeSize(entityData.slimeSize);
            return size * size;
        }
        return definition.maxHealth();
    }

    private static String validateMobAiState(EntityData entityData) {
        if (entityData.mobMoveTargetSet
                && (!isFinite(entityData.mobMoveTargetX)
                        || !isFinite(entityData.mobMoveTargetY)
                        || !isFinite(entityData.mobMoveTargetZ))) {
            return "invalid mob move target";
        }
        if (entityData.panicTime < 0 || !isFinite(entityData.panicFleeX) || !isFinite(entityData.panicFleeZ)) {
            return "invalid panic state";
        }
        if (entityData.targetNearestCheckCooldown < 0
                || entityData.targetNearestSightLostTicks < 0
                || entityData.targetNearestRefreshCooldown < 0) {
            return "invalid target tracking state";
        }
        if (entityData.meleePathRecalcCooldown < 0
                || entityData.meleeStuckTicks < 0
                || !isFinite(entityData.meleeLastX)
                || !isFinite(entityData.meleeLastZ)) {
            return "invalid melee state";
        }
        if (entityData.rangedAttackCooldown < 0
                || entityData.rangedStrafeTime < 0
                || entityData.rangedStrafeSpeed < 0.0f
                || !isFinite(entityData.rangedStrafeSpeed)) {
            return "invalid ranged attack state";
        }
        return null;
    }

    private static String validateSlimeLikeEntity(EntityData entityData) {
        if (!isValidSavedSlimeSize(entityData.slimeSize)) {
            return "invalid slime size";
        }
        if (entityData.jumpDelay < 0) {
            return "invalid slime jump delay";
        }
        return null;
    }

    private static int normalizedSavedSlimeSize(int size) {
        return size == 0 ? 4 : size;
    }

    private static boolean isValidSavedSlimeSize(int size) {
        return size == 0 || size == 1 || size == 2 || size == 4;
    }

    private static String validateBlazeEntity(EntityData entityData) {
        if (entityData.attackCooldown < 0 || entityData.burstShots < 0 || entityData.burstCooldown < 0) {
            return "invalid blaze attack state";
        }
        return null;
    }

    private static String validateGhastEntity(EntityData entityData) {
        if (entityData.fireCooldown < 0 || entityData.ghastAttackCharge < 0 || entityData.wanderCooldown < 0) {
            return "invalid ghast cooldown";
        }
        if (!isFinite(entityData.targetX) || !isFinite(entityData.targetY) || !isFinite(entityData.targetZ)) {
            return "invalid ghast target";
        }
        return null;
    }

    private static String validateSquidEntity(EntityData entityData) {
        if (entityData.swimTimer < 0
                || entityData.airTicks < LivingEntity.DROWN_DAMAGE_AIR_TICKS
                || entityData.airTicks > LivingEntity.MAX_AIR_TICKS) {
            return "invalid squid timers";
        }
        if (!isFinite(entityData.swimX)
                || !isFinite(entityData.swimY)
                || !isFinite(entityData.swimZ)
                || !isFinite(entityData.squidPitch)
                || !isFinite(entityData.prevSquidPitch)
                || !isFinite(entityData.squidYaw)
                || !isFinite(entityData.prevSquidYaw)
                || !isFinite(entityData.squidRotation)
                || !isFinite(entityData.prevSquidRotation)
                || !isFinite(entityData.tentacleAngle)
                || !isFinite(entityData.prevTentacleAngle)) {
            return "invalid squid swim state";
        }
        return null;
    }

    private static String validateEnderDragonEntity(EntityData entityData) {
        if (entityData.targetCooldown < 0 || entityData.dragonDeathTicks < 0) {
            return "invalid dragon timers";
        }
        if (!isFinite(entityData.targetX) || !isFinite(entityData.targetY) || !isFinite(entityData.targetZ)) {
            return "invalid dragon target";
        }
        return null;
    }

    private static String validateCreeperEntity(EntityData entityData) {
        if (entityData.creeperFuseTicks < 0 || entityData.creeperFuseTicks > CREEPER_MAX_FUSE_TICKS) {
            return "invalid creeper fuse";
        }
        return null;
    }

    private static String validateSheepEntity(EntityData entityData) {
        if (entityData.woolColor < 0 || entityData.woolColor > 15) {
            return "invalid sheep wool color";
        }
        return null;
    }

    private static String validateVillagerEntity(EntityData entityData) {
        if (entityData.profession < Villager.PROFESSION_FARMER || entityData.profession > Villager.PROFESSION_BUTCHER) {
            return "invalid villager profession";
        }
        return null;
    }

    private static String validateEndermanEntity(EntityData entityData) {
        if (entityData.stareTicks < 0 || entityData.teleportCooldown < 0) {
            return "invalid enderman attention state";
        }
        if (entityData.carriedMetadata < 0 || entityData.carriedMetadata > 15) {
            return "invalid enderman carried metadata";
        }
        if (entityData.carriedBlockId != 0 && BlockType.fromId(entityData.carriedBlockId) == null) {
            return "invalid enderman carried block";
        }
        return null;
    }

    private static boolean isFinite(float value) {
        return Float.isFinite(value);
    }

    private static boolean isUnitInterval(float value) {
        return value >= 0.0f && value <= 1.0f && isFinite(value);
    }

    private static boolean isScheduledTickBlock(BlockType type) {
        return type != null
                && (type.isFluid()
                        || type == BlockType.FIRE
                        || type.isFallingBlock()
                        || type == BlockType.MOVING_PISTON
                        || type == BlockType.FARMLAND
                        || type.isCrop()
                        || type == BlockType.CACTUS
                        || type == BlockType.SUGAR_CANE
                        || type == BlockType.NETHER_WART
                        || type == BlockType.SNOW_LAYER
                        || type == BlockType.LEAVES
                        || type == BlockType.LOCKED_CHEST
                        || type == BlockType.GLOWING_REDSTONE_ORE
                        || RedstoneEngine.isRedstoneTickable(type));
    }

    private static boolean isValidFace(int face) {
        return face == Block.FACE_TOP
                || face == Block.FACE_BOTTOM
                || face == Block.FACE_NORTH
                || face == Block.FACE_SOUTH
                || face == Block.FACE_EAST
                || face == Block.FACE_WEST;
    }

    private static MobDefinition parseMobDefinition(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        try {
            return MobDefinition.valueOf(name);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private String validateStatusEffects(String context, List<StatusEffectInstance> effects) {
        if (effects == null) {
            return null;
        }
        Set<StatusEffectType> seen = new HashSet<>();
        for (StatusEffectInstance effect : effects) {
            if (effect == null || effect.type() == null) {
                return "invalid status effect in " + context;
            }
            if (effect.durationTicks() <= 0) {
                return "invalid status effect duration in " + context;
            }
            if (effect.amplifier() < 0) {
                return "invalid status effect amplifier in " + context;
            }
            if (!seen.add(effect.type())) {
                return "duplicate status effect in " + context;
            }
        }
        return null;
    }

    private boolean isImplementedMobDefinition(String name) {
        if (name == null || name.isBlank()) {
            return false;
        }
        try {
            return MobFactory.isImplemented(MobDefinition.valueOf(name));
        } catch (IllegalArgumentException ignored) {
            return false;
        }
    }

    private void normalizeLevelData(LevelData data) {
        if (data.levelName == null || data.levelName.isBlank()) {
            data.levelName = worldDir.getFileName() != null && "default".equals(worldDir.getFileName().toString())
                    ? "Default World"
                    : worldDir.getFileName().toString();
        }
        data.serverMotd = sanitizeServerPropertyText(data.serverMotd, data.levelName);
        data.serverIp = sanitizeServerIp(data.serverIp);
        data.serverPort = clampServerPort(data.serverPort);
        data.serverMaxPlayers = data.serverMaxPlayers <= 0
                ? MultiplayerProtocol.DEFAULT_MAX_PLAYERS
                : data.serverMaxPlayers;
        data.serverPvp = serverBoolean(data.serverPvp, true);
        data.serverSpawnAnimals = serverBoolean(data.serverSpawnAnimals, true);
        data.serverSpawnMonsters = serverBoolean(data.serverSpawnMonsters, true);
        data.serverSpawnNpcs = serverBoolean(data.serverSpawnNpcs, true);
        data.serverAllowNether = serverBoolean(data.serverAllowNether, true);
        data.serverOnlineMode = serverBoolean(data.serverOnlineMode, false);
        data.serverAllowFlight = serverBoolean(data.serverAllowFlight, false);
        data.serverEnableQuery = serverBoolean(data.serverEnableQuery, false);
        data.serverQueryPort = data.serverQueryPort == null
                ? MultiplayerProtocol.DEFAULT_QUERY_PORT
                : clampServerPort(data.serverQueryPort);
        data.serverSpawnProtection = data.serverSpawnProtection == null
                ? DEFAULT_SERVER_SPAWN_PROTECTION
                : Math.max(0, data.serverSpawnProtection);
        data.serverViewDistance = data.serverViewDistance == null
                ? DEFAULT_SERVER_VIEW_DISTANCE
                : Math.max(3, Math.min(15, data.serverViewDistance));
        data.serverMaxBuildHeight = data.serverMaxBuildHeight == null
                ? DEFAULT_SERVER_MAX_BUILD_HEIGHT
                : clampServerMaxBuildHeight(data.serverMaxBuildHeight);
        data.serverLevelSeed = sanitizeServerLevelSeed(data.serverLevelSeed);
        data.generateStructures = serverBoolean(data.generateStructures, true);
        if (data.lastPlayed <= 0) {
            data.lastPlayed = System.currentTimeMillis();
        }
        if (data.gameMode == null || data.gameMode.isBlank()) {
            data.gameMode = data.hardcore ? GameMode.HARDCORE.name() : GameMode.SURVIVAL.name();
        }
        if (data.difficulty == null || data.difficulty.isBlank()) {
            data.difficulty = data.hardcore ? Difficulty.HARD.name() : Difficulty.EASY.name();
        }
        if (data.generatorId == null || data.generatorId.isBlank()) {
            data.generatorId = data.formatVersion < 4 ? WorldGenerator.LEGACY_CRAFTZERO : WorldGenerator.RELEASE_ONE;
        }
        if (data.dimension == null || data.dimension.isBlank()) {
            data.dimension = Dimension.OVERWORLD.getSaveName();
        }
        if (data.worldTime == null && data.time >= 0.0f) {
            long dayTicks = data.dayCount != null && data.dayCount > 0
                    ? (long) data.dayCount * DayCycleManager.TICKS_PER_DAY
                    : 0L;
            long wrappedTime = (long) Math.floor(
                    ((data.time % DayCycleManager.TICKS_PER_DAY) + DayCycleManager.TICKS_PER_DAY)
                            % DayCycleManager.TICKS_PER_DAY);
            data.worldTime = dayTicks + wrappedTime;
        }
        if (data.dayCount == null && data.worldTime != null) {
            data.dayCount = (int) Math.min(Integer.MAX_VALUE, data.worldTime / DayCycleManager.TICKS_PER_DAY);
        }
        if (data.moonPhase == null && data.dayCount != null) {
            data.moonPhase = Math.floorMod(data.dayCount, 8);
        }
        data.weatherState = World.normalizeWeatherState(data.weatherState);
        if (data.filledMaps == null) {
            data.filledMaps = new ArrayList<>();
        }
        data.nextFilledMapId = Math.max(0, Math.max(data.nextFilledMapId, nextFilledMapIdFor(data.filledMaps)));
        if (data.spawnY <= 0) {
            data.spawnX = 0;
            data.spawnY = 80;
            data.spawnZ = 0;
        }
        if (data.hardcore) {
            data.gameMode = GameMode.HARDCORE.name();
            data.difficulty = Difficulty.HARD.name();
        }
        if (data.operators == null) {
            data.operators = new ArrayList<>();
        }
        if (data.bannedPlayers == null) {
            data.bannedPlayers = new ArrayList<>();
        }
        if (data.bannedIps == null) {
            data.bannedIps = new ArrayList<>();
        }
        if (data.whitelist == null) {
            data.whitelist = new ArrayList<>();
        }
        if (data.player == null) {
            data.player = defaultPlayerData(data);
        } else {
            normalizePlayerData(data.player, data);
        }
        if (data.inventory == null) {
            data.inventory = defaultInventoryData();
        } else {
            normalizeInventoryData(data.inventory);
        }
    }

    private static PlayerData defaultPlayerData(LevelData level) {
        PlayerData player = new PlayerData();
        int spawnX = level == null ? 0 : level.spawnX;
        int spawnY = level == null || level.spawnY <= 0 ? 80 : level.spawnY;
        int spawnZ = level == null ? 0 : level.spawnZ;
        player.x = spawnX + 0.5f;
        player.y = spawnY;
        player.z = spawnZ + 0.5f;
        player.health = PlayerStats.MAX_HEALTH;
        player.hunger = PlayerStats.MAX_HUNGER;
        player.saturation = 5.0f;
        player.air = PlayerStats.MAX_AIR_SECONDS;
        player.spawnX = spawnX + 0.5f;
        player.spawnY = spawnY;
        player.spawnZ = spawnZ + 0.5f;
        player.achievements = new ArrayList<>();
        player.activeEffects = new ArrayList<>();
        return player;
    }

    private static InventoryData defaultInventoryData() {
        InventoryData inventory = new InventoryData();
        inventory.selectedSlot = 0;
        inventory.hotbar = new StackData[Inventory.HOTBAR_SIZE];
        inventory.main = new StackData[Inventory.MAIN_SIZE];
        inventory.crafting = new StackData[Inventory.CRAFTING_SIZE];
        inventory.armor = new StackData[4];
        return inventory;
    }

    private static void normalizePlayerData(PlayerData player, LevelData level) {
        if (player.spawnY <= 0.0f) {
            int spawnX = level == null ? 0 : level.spawnX;
            int spawnY = level == null || level.spawnY <= 0 ? 80 : level.spawnY;
            int spawnZ = level == null ? 0 : level.spawnZ;
            player.spawnX = spawnX + 0.5f;
            player.spawnY = spawnY;
            player.spawnZ = spawnZ + 0.5f;
        }
        if (player.air <= 0.0f) {
            player.air = PlayerStats.MAX_AIR_SECONDS;
        }
        if (player.health <= 0.0f && (level == null || level.formatVersion < 2)) {
            player.health = PlayerStats.MAX_HEALTH;
        }
        if (player.hunger <= 0.0f && (level == null || level.formatVersion < 2)) {
            player.hunger = PlayerStats.MAX_HUNGER;
        }
        if (player.saturation > player.hunger) {
            player.saturation = player.hunger;
        }
        if (player.health > 0.0f) {
            player.deathTime = 0;
        }
        if (player.hurtInvulnerabilityTimer <= 0.0f) {
            player.lastDamageAmount = 0.0f;
        }
        player.foodTickTimer = Math.max(0, player.foodTickTimer);
        if (player.achievements == null) {
            player.achievements = new ArrayList<>();
        }
        if (player.activeEffects == null) {
            player.activeEffects = new ArrayList<>();
        }
    }

    private static void normalizeInventoryData(InventoryData inventory) {
        inventory.selectedSlot = Math.max(0, Math.min(Inventory.HOTBAR_SIZE - 1, inventory.selectedSlot));
        inventory.hotbar = normalizeStackArray(inventory.hotbar, Inventory.HOTBAR_SIZE);
        inventory.main = normalizeStackArray(inventory.main, Inventory.MAIN_SIZE);
        inventory.crafting = normalizeStackArray(inventory.crafting, Inventory.CRAFTING_SIZE);
        inventory.armor = normalizeStackArray(inventory.armor, 4);
    }

    private static StackData[] normalizeStackArray(StackData[] source, int expectedLength) {
        StackData[] normalized = new StackData[expectedLength];
        if (source != null) {
            System.arraycopy(source, 0, normalized, 0, Math.min(source.length, expectedLength));
        }
        return normalized;
    }

    private void normalizeDimensionRuntimeData(DimensionRuntimeData data) {
        if (data.worldTime == null && data.time != null && data.time >= 0.0f) {
            long dayTicks = data.dayCount != null && data.dayCount > 0
                    ? (long) data.dayCount * DayCycleManager.TICKS_PER_DAY
                    : 0L;
            long wrappedTime = (long) Math.floor(
                    ((data.time % DayCycleManager.TICKS_PER_DAY) + DayCycleManager.TICKS_PER_DAY)
                            % DayCycleManager.TICKS_PER_DAY);
            data.worldTime = dayTicks + wrappedTime;
        }
        if (data.dayCount == null && data.worldTime != null) {
            data.dayCount = (int) Math.min(Integer.MAX_VALUE, data.worldTime / DayCycleManager.TICKS_PER_DAY);
        }
        if (data.moonPhase == null && data.dayCount != null) {
            data.moonPhase = Math.floorMod(data.dayCount, 8);
        }
        if (data.weatherState != null && !data.weatherState.isBlank()) {
            data.weatherState = World.normalizeWeatherState(data.weatherState);
        }
        if (data.droppedItems == null) {
            data.droppedItems = new ArrayList<>();
        }
        if (data.tileEntities == null) {
            data.tileEntities = new ArrayList<>();
        }
        if (data.entities == null) {
            data.entities = new ArrayList<>();
        }
        if (data.movingPistons == null) {
            data.movingPistons = new ArrayList<>();
        }
        if (data.scheduledBlockTicks == null) {
            data.scheduledBlockTicks = new ArrayList<>();
        }
    }

    private void mergeAdminSidecars(LevelData data) {
        data.operators = adminSidecarOrExisting(opsPath, data.operators);
        data.bannedPlayers = adminSidecarOrExisting(bannedPlayersPath, data.bannedPlayers);
        data.bannedIps = adminSidecarOrExisting(bannedIpsPath, data.bannedIps);
        data.whitelist = adminSidecarOrExisting(whitelistPath, data.whitelist);
    }

    private void mergeServerProperties(LevelData data) {
        if (data == null || !Files.isRegularFile(serverPropertiesPath)) {
            return;
        }
        Properties properties = new Properties();
        try (Reader reader = Files.newBufferedReader(serverPropertiesPath, StandardCharsets.UTF_8)) {
            properties.load(reader);
        } catch (IOException exception) {
            System.err.println("Failed to read server.properties: " + exception.getMessage());
            return;
        }
        data.levelName = sanitizeServerPropertyText(properties.getProperty("level-name"), data.levelName);
        data.serverLevelSeed = sanitizeServerLevelSeed(properties.getProperty("level-seed", data.getServerLevelSeed()));
        Long propertySeed = serverPropertySeed(data.serverLevelSeed);
        if (propertySeed != null) {
            data.seed = propertySeed;
        }
        data.generateStructures = booleanServerProperty(properties, "generate-structures",
                data.shouldGenerateStructures());
        data.serverMotd = sanitizeServerPropertyText(properties.getProperty("motd"), data.serverMotd);
        data.serverIp = sanitizeServerIp(properties.getProperty("server-ip", data.getServerIp()));
        data.serverPort = boundedServerProperty(properties, "server-port", data.getServerPort(), 1, 65535);
        data.serverMaxPlayers = positiveServerProperty(properties, "max-players", data.serverMaxPlayers);
        data.whitelistEnabled = booleanServerProperty(properties, "white-list", data.whitelistEnabled);
        GameMode propertyGameMode = serverPropertyGameMode(properties, data.getGameMode());
        data.gameMode = propertyGameMode.name();
        data.hardcore = booleanServerProperty(properties, "hardcore", data.hardcore)
                || propertyGameMode == GameMode.HARDCORE;
        data.difficulty = serverPropertyDifficulty(properties, data.getDifficulty()).name();
        data.serverPvp = booleanServerProperty(properties, "pvp", data.isServerPvp());
        data.serverSpawnAnimals = booleanServerProperty(properties, "spawn-animals", data.isServerSpawnAnimals());
        data.serverSpawnMonsters = booleanServerProperty(properties, "spawn-monsters", data.isServerSpawnMonsters());
        data.serverSpawnNpcs = booleanServerProperty(properties, "spawn-npcs", data.isServerSpawnNpcs());
        data.serverAllowNether = booleanServerProperty(properties, "allow-nether", data.isServerAllowNether());
        data.serverOnlineMode = booleanServerProperty(properties, "online-mode", data.isServerOnlineMode());
        data.serverAllowFlight = booleanServerProperty(properties, "allow-flight", data.isServerAllowFlight());
        data.serverEnableQuery = booleanServerProperty(properties, "enable-query", data.isServerEnableQuery());
        data.serverQueryPort = boundedServerProperty(properties, "query.port", data.getServerQueryPort(), 1, 65535);
        data.serverSpawnProtection = boundedServerProperty(properties, "spawn-protection",
                data.getServerSpawnProtection(), 0, Integer.MAX_VALUE);
        data.serverViewDistance = boundedServerProperty(properties, "view-distance",
                data.getServerViewDistance(), 3, 15);
        data.serverMaxBuildHeight = boundedServerProperty(properties, "max-build-height",
                data.getServerMaxBuildHeight(), MultiplayerProtocol.MIN_MAX_BUILD_HEIGHT,
                MultiplayerProtocol.WORLD_HEIGHT);
        if (data.hardcore) {
            data.gameMode = GameMode.HARDCORE.name();
            data.difficulty = Difficulty.HARD.name();
        }
    }

    private void writeServerProperties(LevelData data) throws IOException {
        if (data == null) {
            return;
        }
        String motd = sanitizeServerPropertyText(data.serverMotd, data.levelName);
        int port = data.getServerPort();
        int maxPlayers = Math.max(1, data.serverMaxPlayers);
        SafeFiles.writeAtomic(serverPropertiesPath, writer -> {
            writer.write("# CraftZero Release-style multiplayer server properties");
            writer.write(System.lineSeparator());
            writeServerProperty(writer, "level-name", data.levelName);
            writeServerProperty(writer, "level-seed", data.getServerLevelSeed().isBlank()
                    ? Long.toString(data.seed)
                    : data.getServerLevelSeed());
            writeServerProperty(writer, "generate-structures", Boolean.toString(data.shouldGenerateStructures()));
            writeServerProperty(writer, "motd", motd);
            writeServerProperty(writer, "server-ip", data.getServerIp());
            writeServerProperty(writer, "server-port", Integer.toString(port));
            writeServerProperty(writer, "max-players", Integer.toString(maxPlayers));
            writeServerProperty(writer, "white-list", Boolean.toString(data.whitelistEnabled));
            writeServerProperty(writer, "gamemode", Integer.toString(serverPropertyGameMode(data.getGameMode())));
            writeServerProperty(writer, "difficulty", Integer.toString(serverPropertyDifficulty(data.getDifficulty())));
            writeServerProperty(writer, "hardcore", Boolean.toString(data.hardcore));
            writeServerProperty(writer, "online-mode", Boolean.toString(data.isServerOnlineMode()));
            writeServerProperty(writer, "enable-query", Boolean.toString(data.isServerEnableQuery()));
            writeServerProperty(writer, "query.port", Integer.toString(data.getServerQueryPort()));
            writeServerProperty(writer, "pvp", Boolean.toString(data.isServerPvp()));
            writeServerProperty(writer, "spawn-animals", Boolean.toString(data.isServerSpawnAnimals()));
            writeServerProperty(writer, "spawn-monsters", Boolean.toString(data.isServerSpawnMonsters()));
            writeServerProperty(writer, "spawn-npcs", Boolean.toString(data.isServerSpawnNpcs()));
            writeServerProperty(writer, "allow-nether", Boolean.toString(data.isServerAllowNether()));
            writeServerProperty(writer, "allow-flight", Boolean.toString(data.isServerAllowFlight()));
            writeServerProperty(writer, "spawn-protection", Integer.toString(data.getServerSpawnProtection()));
            writeServerProperty(writer, "view-distance", Integer.toString(data.getServerViewDistance()));
            writeServerProperty(writer, "max-build-height", Integer.toString(data.getServerMaxBuildHeight()));
        }, SafeFiles.BackupPolicy.BAK);
    }

    private static void writeServerProperty(java.io.Writer writer, String key, String value) throws IOException {
        writer.write(key);
        writer.write('=');
        writer.write(serverPropertyValue(value));
        writer.write(System.lineSeparator());
    }

    private static int positiveServerProperty(Properties properties, String key, int fallback) {
        return boundedServerProperty(properties, key, fallback, 1, Integer.MAX_VALUE);
    }

    private static int boundedServerProperty(Properties properties, String key, int fallback, int min, int max) {
        if (properties == null || key == null) {
            return Math.max(min, Math.min(max, fallback));
        }
        try {
            return Math.max(min, Math.min(max, Integer.parseInt(properties.getProperty(key, "").trim())));
        } catch (RuntimeException ignored) {
            return Math.max(min, Math.min(max, fallback));
        }
    }

    private static boolean booleanServerProperty(Properties properties, String key, boolean fallback) {
        if (properties == null || key == null) {
            return fallback;
        }
        String value = properties.getProperty(key);
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return parseServerBoolean(value, fallback);
    }

    private static boolean parseServerBoolean(String value, boolean fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return switch (value.trim().toLowerCase(Locale.ROOT)) {
            case "true", "yes", "1", "on" -> true;
            case "false", "no", "0", "off" -> false;
            default -> fallback;
        };
    }

    private static boolean serverBoolean(Boolean value, boolean fallback) {
        return value == null ? fallback : value;
    }

    private static int clampServerPort(int port) {
        return port <= 0 || port > 65535 ? MultiplayerProtocol.DEFAULT_PORT : port;
    }

    private static int clampServerMaxBuildHeight(int height) {
        return Math.max(MultiplayerProtocol.MIN_MAX_BUILD_HEIGHT,
                Math.min(MultiplayerProtocol.WORLD_HEIGHT, height));
    }

    private static GameMode serverPropertyGameMode(Properties properties, GameMode fallback) {
        if (properties == null) {
            return fallback == null ? GameMode.SURVIVAL : fallback;
        }
        String value = properties.getProperty("gamemode");
        if (value == null || value.isBlank()) {
            value = properties.getProperty("game-mode");
        }
        return value == null || value.isBlank()
                ? (fallback == null ? GameMode.SURVIVAL : fallback)
                : GameMode.fromName(value);
    }

    private static Difficulty serverPropertyDifficulty(Properties properties, Difficulty fallback) {
        if (properties == null) {
            return fallback == null ? Difficulty.EASY : fallback;
        }
        String value = properties.getProperty("difficulty");
        return value == null || value.isBlank()
                ? (fallback == null ? Difficulty.EASY : fallback)
                : Difficulty.fromName(value);
    }

    private static Long serverPropertySeed(String value) {
        String seedText = sanitizeServerLevelSeed(value);
        if (seedText.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(seedText);
        } catch (NumberFormatException ignored) {
            return (long) seedText.hashCode();
        }
    }

    private static String sanitizeServerPropertyText(String value, String fallback) {
        String text = value == null || value.isBlank() ? fallback : value;
        if (text == null || text.isBlank()) {
            text = "CraftZero";
        }
        return serverPropertyValue(text).trim();
    }

    private static String sanitizeServerLevelSeed(String value) {
        return serverPropertyValue(value);
    }

    private static String sanitizeServerIp(String value) {
        return serverPropertyValue(value);
    }

    private static String serverPropertyValue(String value) {
        if (value == null) {
            return "";
        }
        return value.replace('\r', ' ').replace('\n', ' ').trim();
    }

    private static int serverPropertyGameMode(GameMode mode) {
        return mode == GameMode.CREATIVE ? 1 : 0;
    }

    private static int serverPropertyDifficulty(Difficulty difficulty) {
        Difficulty normalized = difficulty == null ? Difficulty.EASY : difficulty;
        return switch (normalized) {
            case PEACEFUL -> 0;
            case EASY -> 1;
            case NORMAL -> 2;
            case HARD -> 3;
        };
    }

    private List<String> adminSidecarOrExisting(Path path, List<String> existing) {
        List<String> sidecar = readAdminSidecar(path);
        return sidecar == null ? existing : sidecar;
    }

    private List<String> readAdminSidecar(Path path) {
        if (path == null || !Files.isRegularFile(path)) {
            return null;
        }
        try {
            return normalizedAdminLines(Files.readAllLines(path, StandardCharsets.UTF_8));
        } catch (IOException exception) {
            System.err.println("Failed to read admin sidecar " + path.getFileName() + ": " + exception.getMessage());
            return null;
        }
    }

    private void writeAdminSidecars(LevelData data) throws IOException {
        if (data == null) {
            return;
        }
        writeAdminSidecar(opsPath, data.operators);
        writeAdminSidecar(bannedPlayersPath, data.bannedPlayers);
        writeAdminSidecar(bannedIpsPath, data.bannedIps);
        writeAdminSidecar(whitelistPath, data.whitelist);
    }

    private void writeAdminSidecar(Path path, Collection<String> values) throws IOException {
        List<String> lines = normalizedAdminLines(values);
        SafeFiles.writeAtomic(path, writer -> {
            for (String line : lines) {
                writer.write(line);
                writer.write(System.lineSeparator());
            }
        }, SafeFiles.BackupPolicy.BAK);
    }

    private static List<String> normalizedAdminLines(Collection<String> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        return values.stream()
                .filter(value -> value != null && !value.isBlank())
                .map(value -> value.trim().toLowerCase(java.util.Locale.ROOT))
                .filter(value -> !value.isBlank() && !value.startsWith("#"))
                .distinct()
                .sorted()
                .toList();
    }

    private static Set<String> normalizedSet(Collection<String> values) {
        if (values == null || values.isEmpty()) {
            return Set.of();
        }
        Set<String> normalized = new HashSet<>();
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                normalized.add(value.trim().toLowerCase(java.util.Locale.ROOT));
            }
        }
        return Collections.unmodifiableSet(normalized);
    }

    private synchronized void replaceGlobalFilledMapColors(Map<String, byte[]> maps) {
        globalFilledMapColors.clear();
        globalFilledMapNextId = 0;
        mergeGlobalFilledMapColors(maps);
    }

    private synchronized void mergeGlobalFilledMapColors(Map<String, byte[]> maps) {
        if (maps == null || maps.isEmpty()) {
            return;
        }
        for (Map.Entry<String, byte[]> entry : maps.entrySet()) {
            String id = entry.getKey();
            byte[] colors = entry.getValue();
            if (id == null || id.isBlank() || !isFilledMapColorPayload(colors)) {
                continue;
            }
            globalFilledMapColors.put(id, colors.clone());
            reserveGlobalFilledMapId(id);
        }
    }

    private synchronized Map<String, byte[]> globalFilledMapColorsSnapshot() {
        Map<String, byte[]> snapshot = new HashMap<>();
        for (Map.Entry<String, byte[]> entry : globalFilledMapColors.entrySet()) {
            snapshot.put(entry.getKey(), entry.getValue().clone());
        }
        return snapshot;
    }

    private synchronized List<FilledMapData> filledMapDataSnapshot() {
        List<FilledMapData> maps = new ArrayList<>();
        for (Map.Entry<String, byte[]> entry : globalFilledMapColors.entrySet()) {
            FilledMapData map = FilledMapData.from(entry.getKey(), entry.getValue());
            if (map != null) {
                maps.add(map);
            }
        }
        return maps;
    }

    private synchronized int globalFilledMapNextIdSnapshot() {
        return globalFilledMapNextId;
    }

    private synchronized void reserveGlobalFilledMapIdsUpTo(int nextId) {
        globalFilledMapNextId = Math.max(globalFilledMapNextId, Math.max(0, nextId));
    }

    private void reserveGlobalFilledMapId(String id) {
        int numericId = mapNumericId(id);
        if (numericId >= 0) {
            reserveGlobalFilledMapIdsUpTo(numericId + 1);
        }
    }

    private static boolean isFilledMapColorPayload(byte[] colors) {
        return colors != null && colors.length == MapItemData.MAP_SIZE * MapItemData.MAP_SIZE;
    }

    private void restoreInventory(Inventory inventory, InventoryData data) {
        restoreArray(inventory.getHotbar(), data.hotbar);
        restoreArray(inventory.getMainInventory(), data.main);
        restoreArray(inventory.getCraftingGrid(), data.crafting);
        restoreArray(inventory.getArmor(), data.armor);
        inventory.setCursorItem(data.cursor != null ? data.cursor.toStack() : null);
        inventory.setSelectedSlot(data.selectedSlot);
    }

    private void restoreArray(ItemStack[] target, StackData[] source) {
        for (int i = 0; i < target.length; i++) {
            target[i] = source != null && i < source.length && source[i] != null
                    ? source[i].toStack()
                    : null;
        }
    }

    public static class LevelData {
        public int formatVersion;
        public String targetVersion;
        public String levelName;
        public String serverMotd;
        public String serverIp;
        public int serverPort;
        public int serverMaxPlayers;
        public Boolean serverPvp;
        public Boolean serverSpawnAnimals;
        public Boolean serverSpawnMonsters;
        public Boolean serverSpawnNpcs;
        public Boolean serverAllowNether;
        public Boolean serverOnlineMode;
        public Boolean serverAllowFlight;
        public Boolean serverEnableQuery;
        public Integer serverQueryPort;
        public Integer serverSpawnProtection;
        public Integer serverViewDistance;
        public Integer serverMaxBuildHeight;
        public String serverLevelSeed;
        public Boolean generateStructures;
        public long lastPlayed;
        public String gameMode;
        public String difficulty;
        public boolean hardcore;
        public boolean allowCheats;
        public long seed;
        public String generatorId;
        public String dimension;
        public int spawnX;
        public int spawnY;
        public int spawnZ;
        public float time = -1;
        public Long worldTime;
        public Integer dayCount;
        public Integer moonPhase;
        public String weatherState;
        public Integer weatherRainTime;
        public Integer weatherThunderTime;
        public List<FilledMapData> filledMaps;
        public int nextFilledMapId;
        public List<String> operators;
        public List<String> bannedPlayers;
        public List<String> bannedIps;
        public List<String> whitelist;
        public boolean whitelistEnabled;
        public PlayerData player;
        public InventoryData inventory;
        public List<DroppedItemData> droppedItems;
        public List<TileEntityData> tileEntities;
        public List<EntityData> entities;
        public List<MovingPistonData> movingPistons;
        public List<ScheduledBlockTickData> scheduledBlockTicks;

        public GameMode getGameMode() {
            return hardcore ? GameMode.HARDCORE : GameMode.fromName(gameMode);
        }

        public Difficulty getDifficulty() {
            return hardcore ? Difficulty.HARD : Difficulty.fromName(difficulty);
        }

        public int getServerPort() {
            return clampServerPort(serverPort);
        }

        public String getServerIp() {
            return sanitizeServerIp(serverIp);
        }

        public boolean isServerPvp() {
            return serverBoolean(serverPvp, true);
        }

        public boolean isServerSpawnAnimals() {
            return serverBoolean(serverSpawnAnimals, true);
        }

        public boolean isServerSpawnMonsters() {
            return serverBoolean(serverSpawnMonsters, true);
        }

        public boolean isServerSpawnNpcs() {
            return serverBoolean(serverSpawnNpcs, true);
        }

        public boolean isServerAllowNether() {
            return serverBoolean(serverAllowNether, true);
        }

        public boolean isServerOnlineMode() {
            return serverBoolean(serverOnlineMode, false);
        }

        public boolean isServerAllowFlight() {
            return serverBoolean(serverAllowFlight, false);
        }

        public boolean isServerEnableQuery() {
            return serverBoolean(serverEnableQuery, false);
        }

        public int getServerQueryPort() {
            return serverQueryPort == null
                    ? MultiplayerProtocol.DEFAULT_QUERY_PORT
                    : clampServerPort(serverQueryPort);
        }

        public int getServerSpawnProtection() {
            return serverSpawnProtection == null ? DEFAULT_SERVER_SPAWN_PROTECTION : Math.max(0, serverSpawnProtection);
        }

        public int getServerViewDistance() {
            return serverViewDistance == null
                    ? DEFAULT_SERVER_VIEW_DISTANCE
                    : Math.max(3, Math.min(15, serverViewDistance));
        }

        public int getServerMaxBuildHeight() {
            return serverMaxBuildHeight == null
                    ? DEFAULT_SERVER_MAX_BUILD_HEIGHT
                    : clampServerMaxBuildHeight(serverMaxBuildHeight);
        }

        public String getServerLevelSeed() {
            return sanitizeServerLevelSeed(serverLevelSeed);
        }

        public boolean shouldGenerateStructures() {
            return serverBoolean(generateStructures, true);
        }
    }

    public enum SaveLoadStatus {
        MISSING,
        LOADED,
        UNSUPPORTED,
        CORRUPT
    }

    public record SaveLoadError(SaveLoadStatus status, String message) {
    }

    public static final class SaveLoadResult {
        private final SaveLoadStatus status;
        private final LevelData levelData;
        private final SaveLoadError error;

        private SaveLoadResult(SaveLoadStatus status, LevelData levelData, SaveLoadError error) {
            this.status = status;
            this.levelData = levelData;
            this.error = error;
        }

        public static SaveLoadResult missing() {
            return new SaveLoadResult(SaveLoadStatus.MISSING, null, null);
        }

        public static SaveLoadResult loaded(LevelData levelData) {
            return new SaveLoadResult(SaveLoadStatus.LOADED, levelData, null);
        }

        public static SaveLoadResult unsupported(String message) {
            return new SaveLoadResult(SaveLoadStatus.UNSUPPORTED, null,
                    new SaveLoadError(SaveLoadStatus.UNSUPPORTED, message));
        }

        public static SaveLoadResult corrupt(String message) {
            return new SaveLoadResult(SaveLoadStatus.CORRUPT, null,
                    new SaveLoadError(SaveLoadStatus.CORRUPT, message));
        }

        public SaveLoadStatus status() {
            return status;
        }

        public LevelData levelData() {
            return levelData;
        }

        public SaveLoadError error() {
            return error;
        }

        public boolean shouldBlockLoad() {
            return status == SaveLoadStatus.UNSUPPORTED || status == SaveLoadStatus.CORRUPT;
        }
    }

    public record SaveSnapshot(LevelData levelData, List<ChunkSaveData> chunks) {
    }

    public record ChunkSaveData(int chunkX, int chunkZ, short[] blockIds, byte[] metadata, byte[] skyLight,
            byte[] blockLight, int[] heightMap, long modificationVersion) {
    }

    public static class DimensionRuntimeData {
        public String dimension;
        public Float time;
        public Long worldTime;
        public Integer dayCount;
        public Integer moonPhase;
        public String weatherState;
        public Integer weatherRainTime;
        public Integer weatherThunderTime;
        public List<DroppedItemData> droppedItems;
        public List<TileEntityData> tileEntities;
        public List<EntityData> entities;
        public List<MovingPistonData> movingPistons;
        public List<ScheduledBlockTickData> scheduledBlockTicks;

        static DimensionRuntimeData from(LevelData data) {
            if (data == null) {
                return null;
            }
            DimensionRuntimeData runtime = new DimensionRuntimeData();
            runtime.dimension = data.dimension;
            runtime.time = data.time;
            runtime.worldTime = data.worldTime;
            runtime.dayCount = data.dayCount;
            runtime.moonPhase = data.moonPhase;
            runtime.weatherState = data.weatherState;
            runtime.weatherRainTime = data.weatherRainTime;
            runtime.weatherThunderTime = data.weatherThunderTime;
            runtime.droppedItems = copyList(data.droppedItems);
            runtime.tileEntities = copyList(data.tileEntities);
            runtime.entities = copyList(data.entities);
            runtime.movingPistons = copyList(data.movingPistons);
            runtime.scheduledBlockTicks = copyList(data.scheduledBlockTicks);
            return runtime;
        }

        private static <T> List<T> copyList(List<T> values) {
            return values == null ? new ArrayList<>() : new ArrayList<>(values);
        }
    }

    public static class FilledMapData {
        public String id;
        public String colors;

        static FilledMapData from(String id, byte[] colors) {
            if (id == null || id.isBlank() || colors == null) {
                return null;
            }
            FilledMapData data = new FilledMapData();
            data.id = id;
            data.colors = Base64.getEncoder().encodeToString(colors);
            return data;
        }

        static Map<String, byte[]> toMap(List<FilledMapData> filledMaps) {
            Map<String, byte[]> maps = new HashMap<>();
            if (filledMaps == null) {
                return maps;
            }
            for (FilledMapData filledMap : filledMaps) {
                if (filledMap == null || filledMap.id == null || filledMap.colors == null) {
                    continue;
                }
                try {
                    byte[] colors = Base64.getDecoder().decode(filledMap.colors);
                    if (colors.length == MapItemData.MAP_SIZE * MapItemData.MAP_SIZE) {
                        maps.put(filledMap.id, colors);
                    }
                } catch (IllegalArgumentException ignored) {
                    // Already validated on normal load; ignore invalid caller-provided data defensively.
                }
            }
            return maps;
        }
    }

    public static class PlayerData {
        public float x;
        public float y;
        public float z;
        public float yaw;
        public float pitch;
        public Float motionX;
        public Float motionY;
        public Float motionZ;
        public Boolean onGround;
        public Float fallStartY;
        public Boolean wasFalling;
        public float health;
        public float hunger;
        public float saturation;
        public float exhaustion;
        public float air;
        public int fireTicks;
        public float regenTimer;
        public float peacefulRegenTimer;
        public float starvationTimer;
        public float drownTimer;
        public float airTickAccumulator;
        public float invincibilityTimer;
        public float hurtInvulnerabilityTimer;
        public float lastDamageAmount;
        public int deathTime;
        public float hurtFlashTimer;
        public int foodTickTimer;
        public int totalExperience;
        public int score;
        public long statPlayTimeTicks;
        public long statDistanceWalkedCm;
        public long statDistanceSwumCm;
        public long statDistanceFallenCm;
        public long statDistanceClimbedCm;
        public long statDistanceFlownCm;
        public long statDistanceDoveCm;
        public long statDistanceByMinecartCm;
        public long statDistanceByBoatCm;
        public long statDistanceByPigCm;
        public long statTimesPlayed;
        public long statGamesQuit;
        public long statWorldsLoaded;
        public long statMultiplayerJoins;
        public long statWorldsSaved;
        public long statJumps;
        public long statBlocksMined;
        public long statSuccessfulAttacks;
        public long statDamageDealtTenths;
        public long statDamageTakenTenths;
        public long statDeaths;
        public long statMobKills;
        public long statMonsterKills;
        public long statPlayerKills;
        public long statFishCaught;
        public long statItemsPickedUp;
        public long statItemsDropped;
        public long statItemsCrafted;
        public long statItemsUsed;
        public long statItemsDepleted;
        public Map<BlockType, Long> statBlocksMinedByType;
        public Map<ItemType, Long> statItemsPickedUpByType;
        public Map<ItemType, Long> statItemsDroppedByType;
        public Map<ItemType, Long> statItemsCraftedByType;
        public Map<ItemType, Long> statItemsUsedByType;
        public Map<ItemType, Long> statItemsDepletedByType;
        public float spawnX;
        public float spawnY;
        public float spawnZ;
        public boolean bedSpawnSet;
        public int bedSpawnX;
        public int bedSpawnY;
        public int bedSpawnZ;
        public int ridingEntitySaveId;
        public String ridingEntityType;
        public boolean sleeping;
        public Integer sleepingBedFootX;
        public Integer sleepingBedFootY;
        public Integer sleepingBedFootZ;
        public Integer sleepingBedHeadX;
        public Integer sleepingBedHeadY;
        public Integer sleepingBedHeadZ;
        public Float sleepReturnX;
        public Float sleepReturnY;
        public Float sleepReturnZ;
        public Float sleepReturnYaw;
        public Float sleepReturnPitch;
        public List<String> achievements;
        public List<StatusEffectInstance> activeEffects;

        static PlayerData from(Player player) {
            PlayerData data = new PlayerData();
            data.x = player.getPosition().x;
            data.y = player.getPosition().y;
            data.z = player.getPosition().z;
            data.yaw = player.getCamera().getYaw();
            data.pitch = player.getCamera().getPitch();
            var velocity = player.getVelocity();
            data.motionX = velocity.x;
            data.motionY = velocity.y;
            data.motionZ = velocity.z;
            data.onGround = player.isOnGround();
            data.fallStartY = player.getFallStartY();
            data.wasFalling = player.wasFalling();
            data.health = player.getStats().getHealth();
            data.hunger = player.getStats().getHunger();
            data.saturation = player.getStats().getSaturation();
            data.exhaustion = player.getStats().getExhaustion();
            data.air = player.getStats().getCurrentAir();
            data.fireTicks = player.getFireTicks();
            data.regenTimer = player.getStats().getRegenTimer();
            data.peacefulRegenTimer = player.getStats().getPeacefulRegenTimer();
            data.starvationTimer = player.getStats().getStarvationTimer();
            data.drownTimer = player.getStats().getDrownTimer();
            data.airTickAccumulator = player.getStats().getAirTickAccumulator();
            data.invincibilityTimer = player.getStats().getInvincibilityTimer();
            data.hurtInvulnerabilityTimer = player.getStats().getHurtInvulnerabilityTimer();
            data.lastDamageAmount = player.getStats().getLastDamageAmount();
            data.deathTime = player.getDeathTime();
            data.hurtFlashTimer = player.getHurtFlashTimer();
            data.foodTickTimer = player.getStats().getFoodTickTimerTicks();
            data.totalExperience = player.getStats().getProgression().getTotalExperience();
            data.score = player.getStats().getProgression().getScore();
            var statistics = player.getStats().getStatistics();
            data.statPlayTimeTicks = statistics.getPlayTimeTicks();
            data.statDistanceWalkedCm = statistics.getDistanceWalkedCm();
            data.statDistanceSwumCm = statistics.getDistanceSwumCm();
            data.statDistanceFallenCm = statistics.getDistanceFallenCm();
            data.statDistanceClimbedCm = statistics.getDistanceClimbedCm();
            data.statDistanceFlownCm = statistics.getDistanceFlownCm();
            data.statDistanceDoveCm = statistics.getDistanceDoveCm();
            data.statDistanceByMinecartCm = statistics.getDistanceByMinecartCm();
            data.statDistanceByBoatCm = statistics.getDistanceByBoatCm();
            data.statDistanceByPigCm = statistics.getDistanceByPigCm();
            data.statTimesPlayed = statistics.getTimesPlayed();
            data.statGamesQuit = statistics.getGamesQuit();
            data.statWorldsLoaded = statistics.getWorldsLoaded();
            data.statMultiplayerJoins = statistics.getMultiplayerJoins();
            data.statWorldsSaved = statistics.getWorldsSaved();
            data.statJumps = statistics.getJumps();
            data.statBlocksMined = statistics.getBlocksMined();
            data.statSuccessfulAttacks = statistics.getSuccessfulAttacks();
            data.statDamageDealtTenths = statistics.getDamageDealtTenths();
            data.statDamageTakenTenths = statistics.getDamageTakenTenths();
            data.statDeaths = statistics.getDeaths();
            data.statMobKills = statistics.getMobKills();
            data.statMonsterKills = statistics.getMonsterKills();
            data.statPlayerKills = statistics.getPlayerKills();
            data.statFishCaught = statistics.getFishCaught();
            data.statItemsPickedUp = statistics.getItemsPickedUp();
            data.statItemsDropped = statistics.getItemsDropped();
            data.statItemsCrafted = statistics.getItemsCrafted();
            data.statItemsUsed = statistics.getItemsUsed();
            data.statItemsDepleted = statistics.getItemsDepleted();
            data.statBlocksMinedByType = statistics.getBlocksMinedByType();
            data.statItemsPickedUpByType = statistics.getItemsPickedUpByType();
            data.statItemsDroppedByType = statistics.getItemsDroppedByType();
            data.statItemsCraftedByType = statistics.getItemsCraftedByType();
            data.statItemsUsedByType = statistics.getItemsUsedByType();
            data.statItemsDepletedByType = statistics.getItemsDepletedByType();
            data.spawnX = player.getSpawnX();
            data.spawnY = player.getSpawnY();
            data.spawnZ = player.getSpawnZ();
            data.bedSpawnSet = player.hasBedSpawn();
            BlockPos bedSpawn = player.getBedSpawnPos();
            if (bedSpawn != null) {
                data.bedSpawnX = bedSpawn.x();
                data.bedSpawnY = bedSpawn.y();
                data.bedSpawnZ = bedSpawn.z();
            }
            data.sleeping = player.isSleeping();
            if (data.sleeping) {
                BlockPos sleepingFoot = player.getSleepingBedFootPos();
                BlockPos sleepingHead = player.getSleepingBedHeadPos();
                if (sleepingFoot != null && sleepingHead != null) {
                    data.sleepingBedFootX = sleepingFoot.x();
                    data.sleepingBedFootY = sleepingFoot.y();
                    data.sleepingBedFootZ = sleepingFoot.z();
                    data.sleepingBedHeadX = sleepingHead.x();
                    data.sleepingBedHeadY = sleepingHead.y();
                    data.sleepingBedHeadZ = sleepingHead.z();
                    data.sleepReturnX = player.getSleepReturnX();
                    data.sleepReturnY = player.getSleepReturnY();
                    data.sleepReturnZ = player.getSleepReturnZ();
                    data.sleepReturnYaw = player.getSleepReturnYaw();
                    data.sleepReturnPitch = player.getSleepReturnPitch();
                }
            }
            data.achievements = player.getStats().getAchievements().unlockedIds();
            data.activeEffects = new ArrayList<>(player.getStats().getActiveEffects());
            return data;
        }

        boolean hasMovementState() {
            return motionX != null
                    || motionY != null
                    || motionZ != null
                    || onGround != null
                    || fallStartY != null
                    || wasFalling != null;
        }

        boolean hasCompleteMovementState() {
            return motionX != null
                    && motionY != null
                    && motionZ != null
                    && onGround != null
                    && fallStartY != null
                    && wasFalling != null;
        }

        boolean hasCompleteSleepingState() {
            return sleeping
                    && sleepingBedFootX != null
                    && sleepingBedFootY != null
                    && sleepingBedFootZ != null
                    && sleepingBedHeadX != null
                    && sleepingBedHeadY != null
                    && sleepingBedHeadZ != null
                    && sleepReturnX != null
                    && sleepReturnY != null
                    && sleepReturnZ != null
                    && sleepReturnYaw != null
                    && sleepReturnPitch != null;
        }

        void captureEntityReferences(Player player, Map<Entity, Integer> entitySaveIds) {
            ridingEntitySaveId = 0;
            ridingEntityType = null;
            if (player == null || entitySaveIds == null) {
                return;
            }
            Entity mount = null;
            String mountType = null;
            if (player.getRidingMinecart() != null) {
                mount = player.getRidingMinecart();
                mountType = "MINECART";
            } else if (player.getRidingBoat() != null) {
                mount = player.getRidingBoat();
                mountType = "BOAT";
            } else if (player.getRidingPig() != null) {
                mount = player.getRidingPig();
                mountType = "PIG";
            }
            int saveId = mount == null ? 0 : entitySaveIds.getOrDefault(mount, 0);
            if (saveId > 0) {
                ridingEntitySaveId = saveId;
                ridingEntityType = mountType;
            }
        }

        void restorePlayerEntityReferences(Player player, Map<Integer, Entity> restoredBySaveId) {
            if (player == null || ridingEntitySaveId <= 0 || restoredBySaveId == null) {
                return;
            }
            Entity mount = restoredBySaveId.get(ridingEntitySaveId);
            if (mount != null) {
                player.restoreVehicleMount(mount);
            }
        }
    }

    public static class InventoryData {
        public int selectedSlot;
        public StackData[] hotbar;
        public StackData[] main;
        public StackData[] crafting;
        public StackData[] armor;
        public StackData cursor;

        static InventoryData from(Inventory inventory) {
            InventoryData data = new InventoryData();
            data.selectedSlot = inventory.getSelectedSlot();
            data.hotbar = stackArray(inventory.getHotbar());
            data.main = stackArray(inventory.getMainInventory());
            data.crafting = stackArray(inventory.getCraftingGrid());
            data.armor = stackArray(inventory.getArmor());
            data.cursor = StackData.from(inventory.getCursorItem());
            return data;
        }

        static StackData[] stackArray(ItemStack[] stacks) {
            StackData[] data = new StackData[stacks.length];
            for (int i = 0; i < stacks.length; i++) {
                data[i] = StackData.from(stacks[i]);
            }
            return data;
        }
    }

    private static void restoreStackArray(ItemStack[] target, StackData[] source) {
        for (int i = 0; i < target.length; i++) {
            target[i] = source != null && i < source.length && source[i] != null
                    ? source[i].toStack()
                    : null;
        }
    }

    public static class StackData {
        public int itemId;
        public int dataValue;
        public int count;
        public int durability = -1;
        public String customName;
        public List<EnchantmentInstance> enchantments;
        public PotionData potion;
        public Map<String, String> metadata;

        static StackData from(ItemStack stack) {
            if (stack == null || stack.isEmpty()) {
                return null;
            }
            StackData data = new StackData();
            data.itemId = stack.getType().getId();
            data.dataValue = stack.getType().getDataValue();
            data.count = stack.getCount();
            data.durability = stack.getDurability();
            data.customName = stack.getCustomName();
            data.enchantments = new ArrayList<>(stack.getEnchantments());
            data.potion = stack.getPotionData();
            data.metadata = stack.getMetadata().isEmpty() ? null : Map.copyOf(stack.getMetadata());
            return data;
        }

        ItemStack toStack() {
            ItemType type = ItemType.fromId(itemId, dataValue);
            if (type == null || count <= 0) {
                return null;
            }
            ItemStack stack = new ItemStack(type, count, durability);
            stack.setCustomName(customName);
            stack.setEnchantments(enchantments);
            stack.setPotionData(potion);
            stack.setMetadata(metadata);
            return stack;
        }
    }

    public static class DroppedItemData {
        public int itemId;
        public int dataValue;
        public int count;
        public int durability = -1;
        public String customName;
        public List<EnchantmentInstance> enchantments;
        public PotionData potion;
        public Map<String, String> metadata;
        public float x;
        public float y;
        public float z;
        public float age;
        public Integer pickupDelayTicks;
        public float pickupDelayAccumulator;
        public Integer health;
        public float velocityX;
        public float velocityY;
        public float velocityZ;
        public boolean onGround;
        public float rotation;
        public float bobPhase;

        static DroppedItemData from(DroppedItem item) {
            DroppedItemData data = new DroppedItemData();
            data.itemId = item.getItemType().getId();
            data.dataValue = item.getItemType().getDataValue();
            data.count = item.getCount();
            data.durability = item.getDurability();
            ItemStack stack = item.getStack();
            data.customName = stack.getCustomName();
            data.enchantments = new ArrayList<>(stack.getEnchantments());
            data.potion = stack.getPotionData();
            data.metadata = stack.getMetadata().isEmpty() ? null : Map.copyOf(stack.getMetadata());
            data.x = item.getX();
            data.y = item.getY();
            data.z = item.getZ();
            data.age = item.getAge();
            data.pickupDelayTicks = item.getPickupDelayTicks();
            data.pickupDelayAccumulator = item.getPickupDelayAccumulator();
            data.health = item.getHealth();
            data.velocityX = item.getVelocityX();
            data.velocityY = item.getVelocityY();
            data.velocityZ = item.getVelocityZ();
            data.onGround = item.isOnGround();
            data.rotation = item.getRotation();
            data.bobPhase = item.getBobPhase();
            return data;
        }

        ItemStack toStack() {
            ItemType type = ItemType.fromId(itemId, dataValue);
            if (type == null || count <= 0) {
                return null;
            }
            ItemStack stack = new ItemStack(type, count, durability);
            stack.setCustomName(customName);
            stack.setEnchantments(enchantments);
            stack.setPotionData(potion);
            stack.setMetadata(metadata);
            return stack;
        }
    }

    public static class ScheduledBlockTickData {
        public int x;
        public int y;
        public int z;
        public int blockId;
        public int delayTicks;

        static ScheduledBlockTickData from(World.ScheduledBlockTickState tick) {
            ScheduledBlockTickData data = new ScheduledBlockTickData();
            data.x = tick.x();
            data.y = tick.y();
            data.z = tick.z();
            data.blockId = tick.type().getId();
            data.delayTicks = tick.delayTicks();
            return data;
        }

        World.ScheduledBlockTickState toState() {
            BlockType type = BlockType.fromId(blockId);
            if (type == null
                    || !isScheduledTickBlock(type)
                    || delayTicks < 0
                    || y < 0
                    || y >= Chunk.HEIGHT) {
                return null;
            }
            return new World.ScheduledBlockTickState(x, y, z, type, delayTicks);
        }
    }

    public static class MovingPistonData {
        public int x;
        public int y;
        public int z;
        public int facing;
        public int carriedBlockId;
        public int carriedMetadata;
        public int finalBlockId;
        public int finalMetadata;
        public float fromX;
        public float fromY;
        public float fromZ;
        public float toX;
        public float toY;
        public float toZ;
        public int elapsedTicks;

        static MovingPistonData from(World.MovingPistonState state, long blockTickClock) {
            MovingPistonData data = new MovingPistonData();
            data.x = state.x();
            data.y = state.y();
            data.z = state.z();
            data.facing = state.facing();
            data.carriedBlockId = state.carriedType().getId();
            data.carriedMetadata = state.carriedMetadata();
            data.finalBlockId = state.finalType().getId();
            data.finalMetadata = state.finalMetadata();
            data.fromX = state.fromX();
            data.fromY = state.fromY();
            data.fromZ = state.fromZ();
            data.toX = state.toX();
            data.toY = state.toY();
            data.toZ = state.toZ();
            long elapsed = Math.max(0L, blockTickClock - state.startTick());
            data.elapsedTicks = (int) Math.min(elapsed, RedstoneEngine.PISTON_MOVEMENT_TICKS);
            return data;
        }

        World.MovingPistonState toState(long blockTickClock) {
            BlockType carried = BlockType.fromId(carriedBlockId);
            BlockType finalType = BlockType.fromId(finalBlockId);
            if (carried == null
                    || finalType == null
                    || y < 0
                    || y >= Chunk.HEIGHT
                    || !isValidFace(facing)
                    || carriedMetadata < 0
                    || carriedMetadata > 15
                    || finalMetadata < 0
                    || finalMetadata > 15
                    || !isFinite(fromX)
                    || !isFinite(fromY)
                    || !isFinite(fromZ)
                    || !isFinite(toX)
                    || !isFinite(toY)
                    || !isFinite(toZ)
                    || elapsedTicks < 0
                    || elapsedTicks > RedstoneEngine.PISTON_MOVEMENT_TICKS) {
                return null;
            }
            return new World.MovingPistonState(x, y, z, facing,
                    carried, carriedMetadata,
                    finalType, finalMetadata,
                    fromX, fromY, fromZ,
                    toX, toY, toZ,
                    blockTickClock - elapsedTicks, true);
        }
    }

    public static class TileEntityData {
        public String type;
        public int x;
        public int y;
        public int z;
        public StackData[] inventory;
        public int burnTime;
        public int currentFuelBurnTime;
        public int cookTime;
        public float furnaceTickAccumulator;
        public int brewTime;
        public float brewingTickAccumulator;
        public float lidAngle;
        public int enchantingTickCount;
        public float enchantingPageFlip;
        public float enchantingPrevPageFlip;
        public float enchantingPageFlipTarget;
        public float enchantingPageFlipVelocity;
        public float enchantingBookSpread;
        public float enchantingPrevBookSpread;
        public float enchantingBookRotation;
        public float enchantingBookRotation2;
        public float enchantingPrevBookRotation;
        public float enchantingTickAccumulator;
        public String[] signText;
        public String mobType;
        public int spawnDelay;
        public int minSpawnDelay;
        public int maxSpawnDelay;
        public int spawnCount;
        public int maxNearbyEntities;
        public float spawnerTickAccumulator;
        public int notePitch;
        public int noteInstrument;
        public int playTicks;
        public StackData record;

        static TileEntityData from(TileEntity tile) {
            TileEntityData data = new TileEntityData();
            BlockPos pos = tile.getPos();
            data.type = tile.getTypeId();
            data.x = pos.x();
            data.y = pos.y();
            data.z = pos.z();

            if (tile instanceof ChestTileEntity chest) {
                data.inventory = InventoryData.stackArray(chest.getInventory());
                data.lidAngle = chest.getLidAngle();
            } else if (tile instanceof FurnaceTileEntity furnace) {
                data.inventory = InventoryData.stackArray(furnace.getInventory());
                data.burnTime = furnace.getBurnTime();
                data.currentFuelBurnTime = furnace.getCurrentFuelBurnTime();
                data.cookTime = furnace.getCookTime();
                data.furnaceTickAccumulator = furnace.getTickAccumulator();
            } else if (tile instanceof BrewingStandTileEntity brewingStand) {
                data.inventory = InventoryData.stackArray(brewingStand.getInventory());
                data.brewTime = brewingStand.getBrewTime();
                data.brewingTickAccumulator = brewingStand.getTickAccumulator();
            } else if (tile instanceof DispenserTileEntity dispenser) {
                data.inventory = InventoryData.stackArray(dispenser.getInventory());
            } else if (tile instanceof EnchantingTableTileEntity enchantingTable) {
                data.enchantingTickCount = enchantingTable.getTickCount();
                data.enchantingPageFlip = enchantingTable.getPageFlip();
                data.enchantingPrevPageFlip = enchantingTable.getPrevPageFlip();
                data.enchantingPageFlipTarget = enchantingTable.getPageFlipTarget();
                data.enchantingPageFlipVelocity = enchantingTable.getPageFlipVelocity();
                data.enchantingBookSpread = enchantingTable.getBookSpread();
                data.enchantingPrevBookSpread = enchantingTable.getPrevBookSpread();
                data.enchantingBookRotation = enchantingTable.getBookRotation();
                data.enchantingBookRotation2 = enchantingTable.getBookRotation2();
                data.enchantingPrevBookRotation = enchantingTable.getPrevBookRotation();
                data.enchantingTickAccumulator = enchantingTable.getTickAccumulator();
            } else if (tile instanceof NoteBlockTileEntity note) {
                data.notePitch = note.getPitch();
                data.noteInstrument = note.getLastInstrument();
                data.playTicks = note.getPlayTicks();
            } else if (tile instanceof JukeboxTileEntity jukebox) {
                data.record = StackData.from(jukebox.getRecord());
                data.playTicks = jukebox.getPlayTicks();
            } else if (tile instanceof SignTileEntity sign) {
                data.signText = java.util.Arrays.copyOf(sign.getLines(), sign.getLines().length);
            } else if (tile instanceof MonsterSpawnerTileEntity spawner) {
                data.mobType = spawner.getMobDefinition().name();
                data.spawnDelay = spawner.getDelay();
                data.minSpawnDelay = spawner.getMinDelay();
                data.maxSpawnDelay = spawner.getMaxDelay();
                data.spawnCount = spawner.getSpawnCount();
                data.maxNearbyEntities = spawner.getMaxNearbyEntities();
                data.spawnerTickAccumulator = spawner.getTickAccumulator();
            }
            return data;
        }

        TileEntity toTileEntity() {
            if (!hasValidRuntimeState()) {
                return null;
            }
            if ("chest".equals(type)) {
                ChestTileEntity chest = new ChestTileEntity(x, y, z);
                restoreArray(chest.getInventory(), inventory);
                chest.setLidAngle(lidAngle);
                chest.clearDirty();
                return chest;
            }
            if ("furnace".equals(type)) {
                FurnaceTileEntity furnace = new FurnaceTileEntity(x, y, z);
                restoreArray(furnace.getInventory(), inventory);
                furnace.setBurnTime(burnTime);
                furnace.setCurrentFuelBurnTime(currentFuelBurnTime);
                furnace.setCookTime(cookTime);
                furnace.setTickAccumulator(furnaceTickAccumulator);
                furnace.clearDirty();
                return furnace;
            }
            if ("brewing_stand".equals(type)) {
                BrewingStandTileEntity brewingStand = new BrewingStandTileEntity(x, y, z);
                restoreArray(brewingStand.getInventory(), inventory);
                brewingStand.setBrewTime(brewTime);
                brewingStand.setTickAccumulator(brewingTickAccumulator);
                brewingStand.clearDirty();
                return brewingStand;
            }
            if ("dispenser".equals(type)) {
                DispenserTileEntity dispenser = new DispenserTileEntity(x, y, z);
                restoreArray(dispenser.getInventory(), inventory);
                dispenser.clearDirty();
                return dispenser;
            }
            if ("note_block".equals(type)) {
                NoteBlockTileEntity note = new NoteBlockTileEntity(x, y, z);
                note.setPitch(notePitch);
                note.setLastInstrument(noteInstrument);
                note.setPlayTicks(playTicks);
                note.clearDirty();
                return note;
            }
            if ("jukebox".equals(type)) {
                JukeboxTileEntity jukebox = new JukeboxTileEntity(x, y, z);
                if (record != null) {
                    jukebox.insertRecord(record.toStack());
                }
                jukebox.setPlayTicks(playTicks);
                jukebox.clearDirty();
                return jukebox;
            }
            if ("enchanting_table".equals(type)) {
                EnchantingTableTileEntity enchantingTable = new EnchantingTableTileEntity(x, y, z);
                enchantingTable.setAnimationState(enchantingTickCount,
                        enchantingPageFlip,
                        enchantingPrevPageFlip,
                        enchantingPageFlipTarget,
                        enchantingPageFlipVelocity,
                        enchantingBookSpread,
                        enchantingPrevBookSpread,
                        enchantingBookRotation,
                        enchantingBookRotation2,
                        enchantingPrevBookRotation,
                        enchantingTickAccumulator);
                enchantingTable.clearDirty();
                return enchantingTable;
            }
            if ("sign".equals(type)) {
                SignTileEntity sign = new SignTileEntity(x, y, z);
                if (signText != null) {
                    for (int i = 0; i < signText.length && i < sign.getLines().length; i++) {
                        sign.setLine(i, signText[i]);
                    }
                }
                sign.clearDirty();
                return sign;
            }
            if ("mob_spawner".equals(type)) {
                MonsterSpawnerTileEntity spawner = new MonsterSpawnerTileEntity(x, y, z);
                if (mobType != null) {
                    try {
                        spawner.setMobDefinition(MobDefinition.valueOf(mobType));
                    } catch (IllegalArgumentException ignored) {
                        spawner.setMobDefinition(MobDefinition.PIG);
                    }
                }
                spawner.setDelay(spawnDelay);
                if (minSpawnDelay > 0 || maxSpawnDelay > 0) {
                    spawner.setDelayRange(minSpawnDelay > 0 ? minSpawnDelay : 200,
                            maxSpawnDelay > 0 ? maxSpawnDelay : 800);
                }
                if (spawnCount > 0) {
                    spawner.setSpawnCount(spawnCount);
                }
                if (maxNearbyEntities > 0) {
                    spawner.setMaxNearbyEntities(maxNearbyEntities);
                }
                spawner.setTickAccumulator(spawnerTickAccumulator);
                spawner.clearDirty();
                return spawner;
            }
            return null;
        }

        private boolean hasValidRuntimeState() {
            if ("chest".equals(type)) {
                return hasExpectedLength(inventory, ChestTileEntity.SIZE)
                        && isUnitInterval(lidAngle);
            }
            if ("furnace".equals(type)) {
                return hasExpectedLength(inventory, FurnaceTileEntity.SIZE)
                        && burnTime >= 0
                        && currentFuelBurnTime >= 0
                        && (burnTime <= 0 || currentFuelBurnTime > 0)
                        && cookTime >= 0
                        && cookTime <= FurnaceTileEntity.COOK_TIME_TOTAL
                        && isUnitInterval(furnaceTickAccumulator);
            }
            if ("brewing_stand".equals(type)) {
                return hasExpectedLength(inventory, BrewingStandTileEntity.SIZE)
                        && brewTime >= 0
                        && brewTime <= BrewingStandTileEntity.BREW_TIME_TOTAL
                        && isUnitInterval(brewingTickAccumulator);
            }
            if ("dispenser".equals(type)) {
                return hasExpectedLength(inventory, DispenserTileEntity.SIZE);
            }
            if ("note_block".equals(type)) {
                return notePitch >= 0
                        && notePitch <= 24
                        && noteInstrument >= NoteBlockTileEntity.INSTRUMENT_HARP
                        && noteInstrument <= NoteBlockTileEntity.INSTRUMENT_BASS
                        && playTicks >= 0;
            }
            if ("jukebox".equals(type)) {
                if (playTicks < 0) {
                    return false;
                }
                if (record == null) {
                    return true;
                }
                ItemType recordType = ItemType.fromId(record.itemId, record.dataValue);
                return recordType != null
                        && recordType.isRecord()
                        && record.count == 1
                        && record.toStack() != null;
            }
            if ("enchanting_table".equals(type)) {
                return enchantingTickCount >= 0
                        && isFinite(enchantingPageFlip)
                        && isFinite(enchantingPrevPageFlip)
                        && isFinite(enchantingPageFlipTarget)
                        && isFinite(enchantingPageFlipVelocity)
                        && isUnitInterval(enchantingBookSpread)
                        && isUnitInterval(enchantingPrevBookSpread)
                        && isFinite(enchantingBookRotation)
                        && isFinite(enchantingBookRotation2)
                        && isFinite(enchantingPrevBookRotation)
                        && isUnitInterval(enchantingTickAccumulator);
            }
            if ("sign".equals(type)) {
                return signText == null || signText.length == 4;
            }
            if ("mob_spawner".equals(type)) {
                return (mobType == null || parseMobDefinition(mobType) != null)
                        && spawnDelay >= 0
                        && minSpawnDelay >= 0
                        && maxSpawnDelay >= 0
                        && ((minSpawnDelay == 0) == (maxSpawnDelay == 0))
                        && (minSpawnDelay <= 0 || maxSpawnDelay >= minSpawnDelay)
                        && spawnCount >= 0
                        && maxNearbyEntities >= 0
                        && isUnitInterval(spawnerTickAccumulator);
            }
            return false;
        }

        private static void restoreArray(ItemStack[] target, StackData[] source) {
            for (int i = 0; i < target.length; i++) {
                target[i] = source != null && i < source.length && source[i] != null
                        ? source[i].toStack()
                        : null;
            }
        }
    }

    public static class EntityData {
        public int entitySaveId;
        public String type;
        public float x;
        public float y;
        public float z;
        public float motionX;
        public float motionY;
        public float motionZ;
        public float yaw;
        public float pitch;
        public Boolean onGround;
        public Float fallStartY;
        public Boolean falling;
        public float health;
        public int fireTicks;
        public int age;
        public int growingAge;
        public int loveTicks;
        public int slimeSize;
        public int carriedBlockId;
        public int carriedMetadata;
        public boolean angry;
        public boolean tamed;
        public boolean wolfSitting;
        public String wolfOwnerName;
        public boolean wolfWet;
        public boolean wolfShaking;
        public float wolfShakeTime;
        public float wolfPrevShakeTime;
        public boolean sheared;
        public boolean saddled;
        public int woolColor;
        public boolean ignited;
        public int creeperFuseTicks;
        public boolean creeperPowered;
        public int eggTimer;
        public int jumpDelay;
        public int livingAttackCooldown;
        public int snowGolemAttackCooldown;
        public boolean mobMoveTargetSet;
        public float mobMoveTargetX;
        public float mobMoveTargetY;
        public float mobMoveTargetZ;
        public boolean panicActive;
        public int panicTime;
        public float panicFleeX;
        public float panicFleeZ;
        public int targetNearestCheckCooldown;
        public int targetNearestSightLostTicks;
        public int targetNearestRefreshCooldown;
        public boolean meleeAttackActive;
        public int meleePathRecalcCooldown;
        public int meleeStuckTicks;
        public float meleeLastX;
        public float meleeLastZ;
        public boolean rangedAttackActive;
        public int rangedAttackCooldown;
        public int rangedStrafeTime;
        public boolean rangedStrafingClockwise;
        public float rangedStrafeSpeed;
        public int attackCooldown;
        public int burstShots;
        public int burstCooldown;
        public int fireCooldown;
        public int ghastAttackCharge;
        public int wanderCooldown;
        public int stareTicks;
        public int teleportCooldown;
        public int swimTimer;
        public int airTicks;
        public float swimX;
        public float swimY;
        public float swimZ;
        public float squidPitch;
        public float prevSquidPitch;
        public float squidYaw;
        public float prevSquidYaw;
        public float squidRotation;
        public float prevSquidRotation;
        public float tentacleAngle;
        public float prevTentacleAngle;
        public int angerTicks;
        public boolean spiderProvoked;
        public int profession;
        public int experienceValue;
        public int hurtTime;
        public int invulnerableTime;
        public float lastDamageAmount;
        public int recentPlayerHitTicks;
        public int recentPlayerLootingLevel;
        public int pickupDelayTicks;
        public int orbHealth;
        public int projectileItemId;
        public int projectileDataValue;
        public PotionData potion;
        public boolean explosive;
        public boolean playerOwned;
        public boolean ownerPlayer;
        public int projectileShooterSaveId;
        public int mobTargetSaveId;
        public int fishingWaitTicks;
        public int fishingCatchableTicks;
        public boolean fishingHookStuckInGround;
        public int fishingHookedEntitySaveId;
        public int spiderJockeyRiderSaveId;
        public float damage;
        public float knockbackHorizontal;
        public float knockbackVertical;
        public int fireTicksOnHit;
        public boolean critical;
        public boolean inGround;
        public int stuckTicks;
        public int blockX;
        public int blockY;
        public int blockZ;
        public int fallingBlockId;
        public int fallingBlockMetadata;
        public float targetX;
        public float targetY;
        public float targetZ;
        public int targetCooldown;
        public int dragonDeathTicks;
        public boolean dragonDeathStarted;
        public boolean dropsItem;
        public List<StatusEffectInstance> activeEffects;
        public String cartKind;
        public int minecartPassengerSaveId;
        public StackData[] inventory;
        public int fuelTicks;
        public float pushX;
        public float pushZ;
        public float cartDamage;
        public float boatDamage;
        public int rollingAmplitude;
        public int rollingDirection;
        public String paintingArt;
        public int paintingFacing;
        public int fuseTicks;
        public boolean fuseTicksPresent;

        static EntityData from(Entity entity) {
            if (entity == null || entity.isRemoved()) {
                return null;
            }
            EntityData data = new EntityData();
            data.x = entity.getX();
            data.y = entity.getY();
            data.z = entity.getZ();
            data.motionX = entity.getMotionX();
            data.motionY = entity.getMotionY();
            data.motionZ = entity.getMotionZ();
            data.yaw = entity.getYaw();
            data.pitch = entity.getPitch();
            data.onGround = entity.isOnGround();
            data.fallStartY = entity.getFallStartY();
            data.falling = entity.isFalling();
            data.age = entity.getTicksExisted();
            if (entity instanceof ExperienceOrbEntity orb) {
                data.type = "EXPERIENCE_ORB";
                data.experienceValue = orb.getValue();
                data.pickupDelayTicks = orb.getPickupDelayTicks();
                data.orbHealth = orb.getHealth();
                return data;
            }
            if (entity instanceof ArrowEntity arrow) {
                data.type = "ARROW";
                data.playerOwned = arrow.isPlayerOwned();
                data.damage = arrow.getDamage();
                data.knockbackHorizontal = arrow.getKnockbackHorizontal();
                data.knockbackVertical = arrow.getKnockbackVertical();
                data.fireTicksOnHit = arrow.getFireTicksOnHit();
                data.critical = arrow.isCritical();
                data.inGround = arrow.isInGround();
                data.stuckTicks = arrow.getStuckTicks();
                data.blockX = arrow.getBlockX();
                data.blockY = arrow.getBlockY();
                data.blockZ = arrow.getBlockZ();
                return data;
            }
            if (entity instanceof FireballEntity fireball) {
                data.type = "FIREBALL";
                data.explosive = fireball.isExplosive();
                data.ownerPlayer = fireball.isDeflectedByPlayer();
                return data;
            }
            if (entity instanceof EnderPearlEntity pearl) {
                data.type = "ENDER_PEARL";
                data.ownerPlayer = pearl.getOwner() != null;
                return data;
            }
            if (entity instanceof FishingHookEntity hook) {
                data.type = "FISHING_HOOK";
                data.ownerPlayer = hook.getOwner() != null;
                data.fishingWaitTicks = hook.getWaitTicks();
                data.fishingCatchableTicks = hook.getCatchableTicks();
                data.fishingHookStuckInGround = hook.isStuckInGround();
                return data;
            }
            if (entity instanceof ThrownItemEntity thrown) {
                data.type = "THROWN_ITEM";
                ItemType itemType = thrown.getItemType();
                data.projectileItemId = itemType.getId();
                data.projectileDataValue = itemType.getDataValue();
                data.playerOwned = thrown.isPlayerOwned();
                return data;
            }
            if (entity instanceof SplashPotionEntity splashPotion) {
                data.type = "SPLASH_POTION";
                data.potion = splashPotion.getPotionData();
                return data;
            }
            if (entity instanceof EyeOfEnderEntity eye) {
                data.type = "EYE_OF_ENDER";
                data.targetX = eye.getTargetX();
                data.targetY = eye.getTargetY();
                data.targetZ = eye.getTargetZ();
                data.dropsItem = eye.dropsItem();
                return data;
            }
            if (entity instanceof FallingBlockEntity falling) {
                data.type = "FALLING_BLOCK";
                data.fallingBlockId = falling.getBlockType().getId();
                data.fallingBlockMetadata = falling.getMetadata();
                return data;
            }
            if (entity instanceof PrimedTntEntity tnt) {
                data.type = "PRIMED_TNT";
                data.fuseTicks = tnt.getFuseTicks();
                data.fuseTicksPresent = true;
                return data;
            }
            if (entity instanceof BoatEntity boat) {
                data.type = "BOAT";
                data.boatDamage = boat.getDamage();
                data.rollingAmplitude = boat.getRollingAmplitude();
                data.rollingDirection = boat.getRollingDirection();
                return data;
            }
            if (entity instanceof PaintingEntity painting) {
                data.type = "PAINTING";
                data.paintingArt = painting.getArt().motive();
                data.paintingFacing = painting.getFacing();
                return data;
            }
            if (entity instanceof MinecartEntity cart) {
                data.type = "MINECART";
                data.cartKind = cart.getKind().name();
                data.cartDamage = cart.getDamage();
                data.rollingAmplitude = cart.getRollingAmplitude();
                data.rollingDirection = cart.getRollingDirection();
                if (cart instanceof ChestMinecartEntity chestCart) {
                    data.inventory = InventoryData.stackArray(chestCart.getInventory());
                }
                if (cart instanceof FurnaceMinecartEntity furnaceCart) {
                    data.fuelTicks = furnaceCart.getFuelTicks();
                    data.pushX = furnaceCart.getPushX();
                    data.pushZ = furnaceCart.getPushZ();
                }
                return data;
            }
            if (entity instanceof EndCrystalEntity crystal) {
                data.type = "END_CRYSTAL";
                data.health = crystal.getHealth();
                data.activeEffects = new ArrayList<>(crystal.getActiveEffects());
                return crystal.isDead() ? null : data;
            }
            if (entity instanceof Mob mob && (!mob.isDead() || mob instanceof EnderDragon)
                    && mob.getDefinition() != null) {
                data.type = mob.getDefinition().name();
                data.health = mob.getHealth();
                data.fireTicks = mob.getFireTicks();
                data.growingAge = mob.getGrowingAge();
                data.loveTicks = mob.getLoveTicks();
                data.livingAttackCooldown = mob.getAttackCooldown();
                data.hurtTime = mob.getHurtTime();
                data.invulnerableTime = mob.getInvulnerableTime();
                data.lastDamageAmount = mob.getLastDamageAmount();
                data.recentPlayerHitTicks = mob.getRecentPlayerHitTicks();
                data.recentPlayerLootingLevel = mob.getRecentPlayerLootingLevel();
                data.activeEffects = new ArrayList<>(mob.getActiveEffects());
                data.mobMoveTargetSet = mob.getAI().hasMoveTarget();
                data.mobMoveTargetX = mob.getAI().getTargetX();
                data.mobMoveTargetY = mob.getAI().getTargetY();
                data.mobMoveTargetZ = mob.getAI().getTargetZ();
                PanicGoal panicGoal = mob.getAI().getGoal(PanicGoal.class);
                if (panicGoal != null) {
                    PanicGoal.State panicState = panicGoal.getState();
                    data.panicActive = panicState.panicking();
                    data.panicTime = panicState.panicTime();
                    data.panicFleeX = panicState.fleeX();
                    data.panicFleeZ = panicState.fleeZ();
                }
                TargetNearestGoal targetNearestGoal = mob.getAI().getGoal(TargetNearestGoal.class);
                if (targetNearestGoal != null) {
                    TargetNearestGoal.State targetNearestState = targetNearestGoal.getState();
                    data.targetNearestCheckCooldown = targetNearestState.checkCooldown();
                    data.targetNearestSightLostTicks = targetNearestState.sightLostTicks();
                    data.targetNearestRefreshCooldown = targetNearestState.targetRefreshCooldown();
                }
                MeleeAttackGoal meleeAttackGoal = mob.getAI().getGoal(MeleeAttackGoal.class);
                if (meleeAttackGoal != null) {
                    MeleeAttackGoal.State meleeState = meleeAttackGoal.getState();
                    data.meleeAttackActive = mob.getAI().isGoalActive(meleeAttackGoal);
                    data.meleePathRecalcCooldown = meleeState.pathRecalcCooldown();
                    data.meleeStuckTicks = meleeState.stuckTicks();
                    data.meleeLastX = meleeState.lastX();
                    data.meleeLastZ = meleeState.lastZ();
                }
                if (mob instanceof Slime slime) {
                    data.slimeSize = slime.getSize();
                    data.jumpDelay = slime.getJumpDelay();
                }
                if (mob instanceof Skeleton skeleton) {
                    var rangedState = skeleton.getRangedAttackState();
                    data.rangedAttackActive = skeleton.isRangedAttackActive();
                    data.rangedAttackCooldown = rangedState.attackCooldown();
                    data.rangedStrafeTime = rangedState.strafeTime();
                    data.rangedStrafingClockwise = rangedState.strafingClockwise();
                    data.rangedStrafeSpeed = rangedState.strafeSpeed();
                }
                if (mob instanceof SnowGolem snowGolem) {
                    data.snowGolemAttackCooldown = snowGolem.getSnowballAttackCooldown();
                }
                if (mob instanceof Chicken chicken) {
                    data.eggTimer = chicken.getEggTimer();
                }
                if (mob instanceof Blaze blaze) {
                    data.attackCooldown = blaze.getAttackCooldown();
                    data.burstShots = blaze.getBurstShots();
                    data.burstCooldown = blaze.getBurstCooldown();
                }
                if (mob instanceof Ghast ghast) {
                    data.fireCooldown = ghast.getFireCooldown();
                    data.ghastAttackCharge = ghast.getAttackCharge();
                    data.wanderCooldown = ghast.getWanderCooldown();
                    data.targetX = ghast.getTargetX();
                    data.targetY = ghast.getTargetY();
                    data.targetZ = ghast.getTargetZ();
                }
                if (mob instanceof Squid squid) {
                    data.swimTimer = squid.getSwimTimer();
                    data.airTicks = squid.getAirTicks();
                    data.swimX = squid.getSwimX();
                    data.swimY = squid.getSwimY();
                    data.swimZ = squid.getSwimZ();
                    data.squidPitch = squid.getSquidPitch();
                    data.prevSquidPitch = squid.getPrevSquidPitch();
                    data.squidYaw = squid.getSquidYaw();
                    data.prevSquidYaw = squid.getPrevSquidYaw();
                    data.squidRotation = squid.getSquidRotation();
                    data.prevSquidRotation = squid.getPrevSquidRotation();
                    data.tentacleAngle = squid.getTentacleAngle();
                    data.prevTentacleAngle = squid.getPrevTentacleAngle();
                }
                if (mob instanceof EnderDragon dragon) {
                    data.targetX = dragon.getTargetX();
                    data.targetY = dragon.getTargetY();
                    data.targetZ = dragon.getTargetZ();
                    data.targetCooldown = dragon.getTargetCooldown();
                    data.dragonDeathTicks = dragon.getDeathTicks();
                    data.dragonDeathStarted = dragon.isDead();
                }
                if (mob instanceof Creeper creeper) {
                    data.ignited = creeper.isIgnited();
                    data.creeperFuseTicks = creeper.getFuseTime();
                    data.creeperPowered = creeper.isPowered();
                }
                if (mob instanceof Sheep sheep) {
                    data.sheared = sheep.isSheared();
                    data.woolColor = sheep.getWoolColor();
                }
                if (mob instanceof Pig pig) {
                    data.saddled = pig.isSaddled();
                }
                if (mob instanceof Wolf wolf) {
                    data.angry = wolf.isAngry();
                    data.tamed = wolf.isTamed();
                    data.wolfSitting = wolf.isSitting();
                    data.wolfOwnerName = wolf.getOwnerName();
                    data.wolfWet = wolf.isWet();
                    data.wolfShaking = wolf.isShaking();
                    data.wolfShakeTime = wolf.getShakeTime();
                    data.wolfPrevShakeTime = wolf.getPrevShakeTime();
                }
                if (mob instanceof Enderman enderman) {
                    data.carriedBlockId = enderman.getCarriedBlock().getId();
                    data.carriedMetadata = enderman.getCarriedMetadata();
                    data.angry = enderman.isAngry();
                    data.stareTicks = enderman.getStareTicks();
                    data.teleportCooldown = enderman.getTeleportCooldown();
                }
                if (mob instanceof ZombiePigman pigman) {
                    data.angerTicks = pigman.getAngerTicks();
                }
                if (mob instanceof Spider spider) {
                    data.spiderProvoked = spider.isProvoked();
                }
                if (mob instanceof Villager villager) {
                    data.profession = villager.getProfession();
                }
                return data;
            }
            return null;
        }

        void captureEntityReferences(Entity entity, Map<Entity, Integer> entitySaveIds) {
            Entity projectileShooter = projectileShooter(entity);
            if (projectileShooter != null) {
                projectileShooterSaveId = entitySaveIds.getOrDefault(projectileShooter, 0);
            }
            if (entity instanceof Mob mob) {
                LivingEntity target = mob.getAI().getTarget();
                if (target != null) {
                    mobTargetSaveId = entitySaveIds.getOrDefault(target, 0);
                }
            }
            if (entity instanceof FishingHookEntity hook) {
                Entity hooked = hook.getHookedEntity();
                if (hooked != null) {
                    fishingHookedEntitySaveId = entitySaveIds.getOrDefault(hooked, 0);
                }
            }
            if (entity instanceof MinecartEntity cart) {
                LivingEntity passenger = cart.getLivingPassenger();
                if (passenger != null) {
                    minecartPassengerSaveId = entitySaveIds.getOrDefault(passenger, 0);
                }
            }
            if (entity instanceof Spider spider) {
                Skeleton rider = spider.getJockeyRider();
                if (rider != null) {
                    spiderJockeyRiderSaveId = entitySaveIds.getOrDefault(rider, 0);
                }
            }
        }

        void restoreEntityReferences(Entity entity, Map<Integer, Entity> restoredBySaveId) {
            if (projectileShooterSaveId > 0
                    && restoredBySaveId.get(projectileShooterSaveId) instanceof LivingEntity shooter) {
                if (entity instanceof ArrowEntity arrow) {
                    arrow.restoreShooter(shooter);
                } else if (entity instanceof FireballEntity fireball && !ownerPlayer) {
                    fireball.restoreShooter(shooter);
                } else if (entity instanceof ThrownItemEntity thrown) {
                    thrown.restoreShooter(shooter);
                } else if (entity instanceof SplashPotionEntity splashPotion) {
                    splashPotion.restoreShooter(shooter);
                }
            }
            if (entity instanceof Mob mob
                    && mobTargetSaveId > 0
                    && restoredBySaveId.get(mobTargetSaveId) instanceof LivingEntity target) {
                if (mob instanceof Wolf wolf) {
                    wolf.setAssistTarget(target);
                } else {
                    mob.getAI().setTarget(target);
                    mob.getAI().setMoveTarget(target.getX(), target.getZ());
                }
            }
            if (entity instanceof FishingHookEntity hook && fishingHookedEntitySaveId > 0) {
                hook.restoreHookedEntity(restoredBySaveId.get(fishingHookedEntitySaveId));
            }
            if (entity instanceof MinecartEntity cart
                    && minecartPassengerSaveId > 0
                    && restoredBySaveId.get(minecartPassengerSaveId) instanceof LivingEntity passenger) {
                cart.mountLivingEntity(passenger);
            }
            if (entity instanceof Spider spider
                    && spiderJockeyRiderSaveId > 0
                    && restoredBySaveId.get(spiderJockeyRiderSaveId) instanceof Skeleton rider) {
                spider.mountJockey(rider);
            }
        }

        void restoreGenericPhysicsState(Entity entity) {
            if (entity == null) {
                return;
            }
            if (onGround == null && fallStartY == null && falling == null) {
                return;
            }
            float restoredFallStartY = fallStartY != null && isFinite(fallStartY) ? fallStartY : y;
            entity.restoreSavedPhysicsState(Boolean.TRUE.equals(onGround), restoredFallStartY,
                    Boolean.TRUE.equals(falling));
        }

        private static Entity projectileShooter(Entity entity) {
            if (entity instanceof ArrowEntity arrow) {
                return arrow.getShooter();
            }
            if (entity instanceof FireballEntity fireball && !fireball.isDeflectedByPlayer()) {
                return fireball.getShooter();
            }
            if (entity instanceof ThrownItemEntity thrown) {
                return thrown.getShooter();
            }
            if (entity instanceof SplashPotionEntity splashPotion) {
                return splashPotion.getShooter();
            }
            return null;
        }

        Entity toEntity(Player player) {
            if (validateEntityRuntimeData(this) != null) {
                return null;
            }
            if ("EXPERIENCE_ORB".equals(type)) {
                if (validateExperienceOrbEntity(this) != null) {
                    return null;
                }
                ExperienceOrbEntity orb = new ExperienceOrbEntity(x, y, z, experienceValue);
                orb.setMotion(motionX, motionY, motionZ);
                orb.setYaw(yaw);
                orb.setPitch(pitch);
                orb.setTicksExisted(age);
                orb.setPickupDelayTicks(pickupDelayTicks);
                orb.setHealth(orbHealth);
                return orb;
            }
            if ("ARROW".equals(type)) {
                if (validateArrowEntity(this) != null) {
                    return null;
                }
                ArrowEntity arrow = new ArrowEntity(x, y, z, motionX, motionY, motionZ,
                        null, playerOwned, damage <= 0.0f ? 2.0f : damage);
                arrow.setYaw(yaw);
                arrow.setPitch(pitch);
                arrow.setTicksExisted(age);
                arrow.setKnockback(knockbackHorizontal, knockbackVertical);
                arrow.setFireTicksOnHit(fireTicksOnHit);
                arrow.setCritical(critical);
                if (inGround) {
                    arrow.setStuckInBlock(blockX, blockY, blockZ, stuckTicks);
                }
                return arrow;
            }
            if ("FIREBALL".equals(type)) {
                FireballEntity fireball = new FireballEntity(x, y, z, motionX, motionY, motionZ, null, explosive);
                fireball.setYaw(yaw);
                fireball.setPitch(pitch);
                fireball.setTicksExisted(age);
                fireball.setDeflectedByPlayer(ownerPlayer);
                return fireball;
            }
            if ("ENDER_PEARL".equals(type)) {
                EnderPearlEntity pearl = new EnderPearlEntity(x, y, z, motionX, motionY, motionZ,
                        ownerPlayer ? player : null);
                pearl.setYaw(yaw);
                pearl.setPitch(pitch);
                pearl.setTicksExisted(age);
                return pearl;
            }
            if ("FISHING_HOOK".equals(type)) {
                if (validateFishingHookEntity(this) != null) {
                    return null;
                }
                FishingHookEntity hook = new FishingHookEntity(x, y, z, motionX, motionY, motionZ,
                        ownerPlayer ? player : null);
                hook.setYaw(yaw);
                hook.setPitch(pitch);
                hook.setTicksExisted(age);
                hook.restoreFishingState(fishingWaitTicks, fishingCatchableTicks, fishingHookStuckInGround);
                if (ownerPlayer && player != null) {
                    player.attachFishingHook(hook);
                }
                return hook;
            }
            if ("THROWN_ITEM".equals(type)) {
                ItemType itemType = ItemType.fromId(projectileItemId, projectileDataValue);
                if (!isValidThrownItemProjectile(itemType)) {
                    return null;
                }
                ThrownItemEntity thrown = new ThrownItemEntity(x, y, z, motionX, motionY, motionZ,
                        itemType, null, playerOwned);
                thrown.setYaw(yaw);
                thrown.setPitch(pitch);
                thrown.setTicksExisted(age);
                return thrown;
            }
            if ("SPLASH_POTION".equals(type)) {
                SplashPotionEntity splashPotion = new SplashPotionEntity(x, y, z, motionX, motionY, motionZ,
                        null, potion);
                splashPotion.setYaw(yaw);
                splashPotion.setPitch(pitch);
                splashPotion.setTicksExisted(age);
                return splashPotion;
            }
            if ("EYE_OF_ENDER".equals(type)) {
                EyeOfEnderEntity eye = new EyeOfEnderEntity(x, y, z, targetX, targetY, targetZ, dropsItem);
                eye.setMotion(motionX, motionY, motionZ);
                eye.setYaw(yaw);
                eye.setPitch(pitch);
                eye.setTicksExisted(age);
                return eye;
            }
            if ("FALLING_BLOCK".equals(type)) {
                BlockType blockType = BlockType.fromId(fallingBlockId);
                if (blockType == null || !blockType.isFallingBlock()
                        || fallingBlockMetadata < 0 || fallingBlockMetadata > 15) {
                    return null;
                }
                FallingBlockEntity falling = new FallingBlockEntity(blockType, fallingBlockMetadata);
                falling.setPosition(x, y, z);
                falling.setMotion(motionX, motionY, motionZ);
                falling.setYaw(yaw);
                falling.setPitch(pitch);
                falling.setTicksExisted(age);
                return falling;
            }
            if ("PRIMED_TNT".equals(type)) {
                if (validatePrimedTntEntity(this) != null) {
                    return null;
                }
                PrimedTntEntity tnt = new PrimedTntEntity(x, y, z, fuseTicksPresent ? fuseTicks : 80);
                tnt.setMotion(motionX, motionY, motionZ);
                tnt.setYaw(yaw);
                tnt.setPitch(pitch);
                tnt.setTicksExisted(age);
                return tnt;
            }
            if ("BOAT".equals(type)) {
                if (validateBoatEntity(this) != null) {
                    return null;
                }
                BoatEntity boat = new BoatEntity(x, y, z);
                boat.setMotion(motionX, motionY, motionZ);
                boat.setYaw(yaw);
                boat.setPitch(pitch);
                boat.setTicksExisted(age);
                boat.setDamage(boatDamage);
                boat.restoreRollingState(rollingAmplitude, rollingDirection);
                return boat;
            }
            if ("PAINTING".equals(type)) {
                if (!isValidPaintingArt(paintingArt) || !isHorizontalFace(paintingFacing)) {
                    return null;
                }
                PaintingEntity painting = new PaintingEntity(x, y, z, paintingFacing,
                        PaintingEntity.Art.fromMotive(paintingArt));
                painting.setMotion(motionX, motionY, motionZ);
                painting.setYaw(yaw);
                painting.setPitch(pitch);
                painting.setTicksExisted(age);
                return painting;
            }
            if ("MINECART".equals(type)) {
                MinecartEntity.CartKind kind = parseCartKind(cartKind);
                if (kind == null) {
                    return null;
                }
                if (cartDamage < 0.0f
                        || cartDamage > MinecartEntity.BREAK_DAMAGE
                        || !isFinite(cartDamage)) {
                    return null;
                }
                if (kind == MinecartEntity.CartKind.CHEST) {
                    if (inventory == null || inventory.length != ChestMinecartEntity.SIZE) {
                        return null;
                    }
                } else if (inventory != null) {
                    return null;
                }
                MinecartEntity cart = switch (kind) {
                    case CHEST -> new ChestMinecartEntity();
                    case FURNACE -> new FurnaceMinecartEntity();
                    default -> new MinecartEntity(MinecartEntity.CartKind.RIDEABLE);
                };
                cart.setPosition(x, y, z);
                cart.setMotion(motionX, motionY, motionZ);
                cart.setYaw(yaw);
                cart.setPitch(pitch);
                cart.setTicksExisted(age);
                cart.setDamage(cartDamage);
                cart.restoreRollingState(rollingAmplitude, rollingDirection);
                if (cart instanceof ChestMinecartEntity chestCart) {
                    restoreStackArray(chestCart.getInventory(), inventory);
                }
                if (cart instanceof FurnaceMinecartEntity furnaceCart) {
                    if (fuelTicks < 0 || fuelTicks > FurnaceMinecartEntity.MAX_FUEL_TICKS
                            || !isFinite(pushX) || !isFinite(pushZ)) {
                        return null;
                    }
                    furnaceCart.setFuelTicks(fuelTicks);
                    furnaceCart.setPush(pushX, pushZ);
                } else if (fuelTicks != 0 || pushX != 0.0f || pushZ != 0.0f) {
                    return null;
                }
                return cart;
            }
            if ("END_CRYSTAL".equals(type)) {
                if (validateEndCrystalEntity(this) != null) {
                    return null;
                }
                EndCrystalEntity crystal = new EndCrystalEntity(x, y, z);
                crystal.setMotion(motionX, motionY, motionZ);
                crystal.setYaw(yaw);
                crystal.setPitch(pitch);
                crystal.setTicksExisted(age);
                return crystal;
            }
            MobDefinition definition = parseDefinition(type);
            if (definition == null) {
                return null;
            }
            if (validateMobEntity(this, definition) != null) {
                return null;
            }
            Mob mob = createMob(definition, player);
            if (mob == null) {
                return null;
            }
            mob.setPosition(x, y, z);
            mob.setMotion(motionX, motionY, motionZ);
            mob.setYaw(yaw);
            mob.setPitch(pitch);
            mob.setTicksExisted(age);
            mob.setGrowingAge(growingAge);
            mob.setLoveTicks(loveTicks);
            if (mobMoveTargetSet) {
                mob.getAI().navigateTo(mobMoveTargetX, mobMoveTargetY, mobMoveTargetZ);
            }
            if (mob instanceof LivingEntity living) {
                living.setHealth(health);
                if (fireTicks > 0) {
                    living.setOnFire(fireTicks);
                }
                living.setActiveEffects(activeEffects);
                living.restoreDamageState(hurtTime, invulnerableTime, lastDamageAmount,
                        recentPlayerHitTicks, recentPlayerLootingLevel);
                if (mob instanceof EnderDragon dragon) {
                    dragon.setDeathState(dragonDeathTicks, dragonDeathStarted);
                }
                mob.setAttackCooldown(livingAttackCooldown);
            }
            return mob;
        }

        private Mob createMob(MobDefinition definition, Player player) {
            Mob mob;
            if (definition == MobDefinition.SLIME) {
                mob = new Slime(slimeSize <= 0 ? 4 : slimeSize);
            } else if (definition == MobDefinition.MAGMA_CUBE) {
                mob = new MagmaCube(slimeSize <= 0 ? 4 : slimeSize);
            } else {
                mob = MobFactory.create(definition);
            }
            if (mob == null) {
                return null;
            }
            if (mob instanceof Enderman enderman) {
                enderman.setCarriedBlock(BlockType.fromId(carriedBlockId), carriedMetadata);
                enderman.setAngry(angry);
                enderman.setAttentionState(stareTicks, teleportCooldown);
            }
            if (mob instanceof Slime slime) {
                slime.setJumpDelay(jumpDelay);
            }
            PanicGoal panicGoal = mob.getAI().getGoal(PanicGoal.class);
            if (panicGoal != null && (panicActive || panicTime > 0)) {
                panicGoal.restoreState(new PanicGoal.State(panicActive, panicTime, panicFleeX, panicFleeZ));
            }
            TargetNearestGoal targetNearestGoal = mob.getAI().getGoal(TargetNearestGoal.class);
            if (targetNearestGoal != null
                    && (targetNearestCheckCooldown > 0
                            || targetNearestSightLostTicks > 0
                            || targetNearestRefreshCooldown > 0)) {
                targetNearestGoal.restoreState(new TargetNearestGoal.State(targetNearestCheckCooldown,
                        targetNearestSightLostTicks, targetNearestRefreshCooldown));
            }
            MeleeAttackGoal meleeAttackGoal = mob.getAI().getGoal(MeleeAttackGoal.class);
            if (meleeAttackGoal != null
                    && (meleeAttackActive || meleePathRecalcCooldown > 0 || meleeStuckTicks > 0
                            || meleeLastX != 0.0f || meleeLastZ != 0.0f)) {
                meleeAttackGoal.restoreState(new MeleeAttackGoal.State(meleePathRecalcCooldown,
                        meleeStuckTicks, meleeLastX, meleeLastZ), meleeAttackActive);
            }
            if (mob instanceof Skeleton skeleton
                    && (rangedAttackActive || rangedAttackCooldown > 0 || rangedStrafeTime > 0
                            || rangedStrafeSpeed > 0.0f)) {
                skeleton.restoreRangedAttackState(
                        new com.craftzero.entity.ai.RangedAttackGoal.State(rangedAttackCooldown,
                                rangedStrafeTime, rangedStrafingClockwise, rangedStrafeSpeed),
                        rangedAttackActive);
            }
            if (mob instanceof Chicken chicken) {
                chicken.setEggTimer(eggTimer);
            }
            if (mob instanceof Blaze blaze) {
                blaze.setAttackState(attackCooldown, burstShots, burstCooldown);
            }
            if (mob instanceof SnowGolem snowGolem) {
                snowGolem.setSnowballAttackCooldown(snowGolemAttackCooldown);
            }
            if (mob instanceof Ghast ghast) {
                ghast.setFlightState(fireCooldown, ghastAttackCharge, wanderCooldown, targetX, targetY, targetZ);
            }
            if (mob instanceof Squid squid) {
                squid.setSwimState(swimTimer, airTicks, swimX, swimY, swimZ,
                        squidPitch, prevSquidPitch, squidYaw, prevSquidYaw,
                        squidRotation, prevSquidRotation, tentacleAngle, prevTentacleAngle);
            }
            if (mob instanceof EnderDragon dragon && (targetY != 0.0f || targetCooldown > 0)) {
                dragon.setFlightState(targetX, targetY, targetZ, targetCooldown);
            }
            if (mob instanceof Creeper creeper) {
                creeper.setFuseState(creeperFuseTicks, ignited);
                creeper.setPowered(creeperPowered);
            }
            if (mob instanceof Sheep sheep) {
                sheep.setSheared(sheared);
                sheep.setWoolColor(woolColor);
            }
            if (mob instanceof Pig pig) {
                pig.setSaddled(saddled);
            }
            if (mob instanceof Wolf wolf) {
                wolf.setAngry(angry);
                wolf.setTamed(tamed);
                wolf.setOwnerName(wolfOwnerName);
                if (wolf.isTamed() && !wolf.hasOwner() && player != null) {
                    wolf.setOwnerName(player.getPlayerName());
                }
                wolf.setSitting(wolfSitting);
                wolf.setWetShakeState(wolfWet, wolfShaking, wolfShakeTime, wolfPrevShakeTime);
            }
            if (mob instanceof ZombiePigman pigman) {
                pigman.setAngerTicks(angerTicks);
            }
            if (mob instanceof Spider spider) {
                spider.setProvoked(spiderProvoked);
            }
            if (mob instanceof Villager villager) {
                villager.setProfession(profession);
            }
            return mob;
        }

        private static MobDefinition parseDefinition(String type) {
            if (type == null || type.isBlank()) {
                return null;
            }
            try {
                return MobDefinition.valueOf(type);
            } catch (IllegalArgumentException ignored) {
                return null;
            }
        }

        private static MinecartEntity.CartKind parseCartKind(String value) {
            if (value == null || value.isBlank()) {
                return MinecartEntity.CartKind.RIDEABLE;
            }
            try {
                return MinecartEntity.CartKind.valueOf(value);
            } catch (IllegalArgumentException ignored) {
                return null;
            }
        }
    }
}
