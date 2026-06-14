package com.craftzero.save;

import com.craftzero.entity.DroppedItem;
import com.craftzero.entity.Entity;
import com.craftzero.entity.LivingEntity;
import com.craftzero.entity.mob.Blaze;
import com.craftzero.entity.mob.CaveSpider;
import com.craftzero.entity.mob.Mob;
import com.craftzero.entity.mob.MobDefinition;
import com.craftzero.entity.mob.MobFactory;
import com.craftzero.entity.mob.MagmaCube;
import com.craftzero.entity.mob.Slime;
import com.craftzero.entity.mob.Enderman;
import com.craftzero.entity.mob.ZombiePigman;
import com.craftzero.inventory.Inventory;
import com.craftzero.inventory.ItemStack;
import com.craftzero.inventory.ItemType;
import com.craftzero.main.Difficulty;
import com.craftzero.main.GameMode;
import com.craftzero.main.Player;
import com.craftzero.main.PlayerStats;
import com.craftzero.progression.EnchantmentInstance;
import com.craftzero.progression.PotionData;
import com.craftzero.progression.StatusEffectInstance;
import com.craftzero.world.Chunk;
import com.craftzero.world.BlockType;
import com.craftzero.world.DayCycleManager;
import com.craftzero.world.Dimension;
import com.craftzero.world.World;
import com.craftzero.world.WorldGenerator;
import com.craftzero.world.tile.BlockPos;
import com.craftzero.world.tile.ChestTileEntity;
import com.craftzero.world.tile.FurnaceTileEntity;
import com.craftzero.world.tile.MonsterSpawnerTileEntity;
import com.craftzero.world.tile.SignTileEntity;
import com.craftzero.world.tile.TileEntity;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Automatic single-world save/load support for saves/default.
 */
public class SaveManager {
    public static final int FORMAT_VERSION = 5;
    public static final int MIN_SUPPORTED_FORMAT_VERSION = 1;
    public static final String TARGET_VERSION = "Minecraft Java Release 1.0";
    public static final Path DEFAULT_WORLD_DIR = Paths.get("saves", "default");

    private final Path worldDir;
    private final Path chunksDir;
    private final Path levelPath;
    private final Gson gson;
    private String levelName = "Default World";
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

    public SaveManager(Path worldDir) {
        this.worldDir = worldDir;
        this.chunksDir = worldDir.resolve("chunks");
        this.levelPath = worldDir.resolve("level.json");
        this.gson = new GsonBuilder().setPrettyPrinting().create();
    }

    public boolean hasSave() {
        return Files.exists(levelPath);
    }

    public SaveLoadResult loadLevel() {
        if (!hasSave()) {
            Path backup = SafeFiles.backupPath(levelPath);
            if (Files.isRegularFile(backup)) {
                SaveLoadResult backupResult = loadLevelFrom(backup);
                if (backupResult.status == SaveLoadStatus.LOADED) {
                    return backupResult;
                }
            }
            return SaveLoadResult.missing();
        }

        SaveLoadResult primary = loadLevelFrom(levelPath);
        if (primary.status != SaveLoadStatus.CORRUPT) {
            return primary;
        }

        Path backup = SafeFiles.backupPath(levelPath);
        if (Files.isRegularFile(backup)) {
            SaveLoadResult backupResult = loadLevelFrom(backup);
            if (backupResult.status == SaveLoadStatus.LOADED) {
                return backupResult;
            }
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
            normalizeLevelData(data);
            setLevelMetadata(data.levelName, data.getGameMode(), data.getDifficulty(), data.hardcore, data.allowCheats);
            setWorldStateMetadata(data.spawnX, data.spawnY, data.spawnZ, data.weatherState);
            setAdminState(data.operators, data.bannedPlayers, data.bannedIps, data.whitelist, data.whitelistEnabled);
            return SaveLoadResult.loaded(data);
        } catch (Exception e) {
            return SaveLoadResult.corrupt("Failed to load level save: " + e.getMessage());
        }
    }

    public LevelData loadLevelIfExists() {
        SaveLoadResult result = loadLevel();
        return result.status == SaveLoadStatus.LOADED ? result.levelData : null;
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

    public void setWorldStateMetadata(int spawnX, int spawnY, int spawnZ, String weatherState) {
        this.spawnX = spawnX;
        this.spawnY = spawnY;
        this.spawnZ = spawnZ;
        this.weatherState = weatherState == null || weatherState.isBlank() ? "clear" : weatherState;
    }

    public void setAdminState(Collection<String> operators, Collection<String> bannedPlayers,
            Collection<String> bannedIps, Collection<String> whitelist, boolean whitelistEnabled) {
        this.operators = normalizedSet(operators);
        this.bannedPlayers = normalizedSet(bannedPlayers);
        this.bannedIps = normalizedSet(bannedIps);
        this.whitelist = normalizedSet(whitelist);
        this.whitelistEnabled = whitelistEnabled;
    }

    public SaveSnapshot createSnapshot(World world, Player player, DayCycleManager dayCycle) {
        List<ChunkSaveData> chunks = new ArrayList<>();
        for (Chunk chunk : world.getLoadedChunks()) {
            if (chunk.isModified()) {
                chunks.add(new ChunkSaveData(
                        chunk.getChunkX(),
                        chunk.getChunkZ(),
                        chunk.copyBlockIds(),
                        chunk.copyBlockMetadata(),
                        chunk.getModificationVersion()));
            }
        }
        return new SaveSnapshot(createLevelData(world, player, dayCycle), chunks);
    }

    public void writeSnapshot(SaveSnapshot snapshot) throws IOException {
        Files.createDirectories(worldDir);
        Files.createDirectories(chunksDir);

        for (ChunkSaveData chunk : snapshot.chunks()) {
            ChunkCodec.write(chunkPath(chunk.chunkX(), chunk.chunkZ()), chunk.blockIds(), chunk.metadata());
        }

        SafeFiles.writeAtomic(levelPath, writer -> gson.toJson(snapshot.levelData(), writer),
                SafeFiles.BackupPolicy.BAK);
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

    public void saveModifiedChunk(Chunk chunk) throws IOException {
        if (chunk == null || !chunk.isModified()) {
            return;
        }
        long version = chunk.getModificationVersion();
        ChunkCodec.write(chunkPath(chunk.getChunkX(), chunk.getChunkZ()), chunk);
        chunk.clearModifiedIfVersion(version);
    }

    public boolean loadChunkIfExists(Chunk chunk) {
        Path path = chunkPath(chunk.getChunkX(), chunk.getChunkZ());
        if (!Files.exists(path)) {
            return false;
        }

        try {
            ChunkCodec.ChunkData data = ChunkCodec.read(path);
            chunk.loadBlockData(data.blockIds(), data.metadata(), false);
            return true;
        } catch (Exception e) {
            System.err.println("Failed to load chunk " + chunk.getChunkX() + "," + chunk.getChunkZ()
                    + ": " + e.getMessage());
            return false;
        }
    }

    public void applyLevel(LevelData data, Player player, DayCycleManager dayCycle, World world) {
        if (data == null) {
            return;
        }

        if (data.time >= 0) {
            dayCycle.setTime(data.time);
        }

        if (data.player != null) {
            player.setPosition(data.player.x, data.player.y, data.player.z);
            player.getCamera().setYaw(data.player.yaw);
            player.getCamera().setPitch(data.player.pitch);

            PlayerStats stats = player.getStats();
            stats.restore(data.player.health, data.player.hunger, data.player.saturation, data.player.air);
            stats.getProgression().restore(data.player.totalExperience, data.player.score);
            stats.setActiveEffects(data.player.activeEffects);
            if (data.player.spawnY > 0.0f) {
                player.setSpawnPosition(data.player.spawnX, data.player.spawnY, data.player.spawnZ);
            } else {
                player.setSpawnPosition(data.spawnX + 0.5f, data.spawnY, data.spawnZ + 0.5f);
            }
        }

        if (data.inventory != null) {
            restoreInventory(player.getInventory(), data.inventory);
        }

        if (data.droppedItems != null) {
            List<DroppedItem> restored = new ArrayList<>();
            for (DroppedItemData itemData : data.droppedItems) {
                ItemStack stack = itemData.toStack();
                if (stack == null || stack.isEmpty()) {
                    continue;
                }
                DroppedItem item = new DroppedItem(itemData.x, itemData.y, itemData.z, stack, 0, 0, 0);
                item.setAge(itemData.age);
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
            for (EntityData entityData : data.entities) {
                Entity entity = entityData.toEntity();
                if (entity != null) {
                    restored.add(entity);
                }
            }
            world.replaceEntities(restored);
        }

        world.reconcileLoadedTileEntities();
    }

    private void saveModifiedChunks(Collection<Chunk> chunks) throws IOException {
        for (Chunk chunk : chunks) {
            if (chunk.isModified()) {
                ChunkCodec.write(chunkPath(chunk.getChunkX(), chunk.getChunkZ()), chunk);
                chunk.clearModified();
            }
        }
    }

    private Path chunkPath(int chunkX, int chunkZ) {
        return chunksDir.resolve("c." + chunkX + "." + chunkZ + ".bin");
    }

    private LevelData createLevelData(World world, Player player, DayCycleManager dayCycle) {
        LevelData data = new LevelData();
        data.formatVersion = FORMAT_VERSION;
        data.targetVersion = TARGET_VERSION;
        data.levelName = levelName;
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
        data.worldTime = (long) dayCycle.getTime();
        data.weatherState = weatherState;
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
        for (Entity entity : world.getEntities()) {
            EntityData entityData = EntityData.from(entity);
            if (entityData != null) {
                data.entities.add(entityData);
            }
        }
        return data;
    }

    private void normalizeLevelData(LevelData data) {
        if (data.levelName == null || data.levelName.isBlank()) {
            data.levelName = worldDir.getFileName() != null && "default".equals(worldDir.getFileName().toString())
                    ? "Default World"
                    : worldDir.getFileName().toString();
        }
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
        if (data.weatherState == null || data.weatherState.isBlank()) {
            data.weatherState = "clear";
        }
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
        public long worldTime;
        public String weatherState;
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

        public GameMode getGameMode() {
            return hardcore ? GameMode.HARDCORE : GameMode.fromName(gameMode);
        }

        public Difficulty getDifficulty() {
            return hardcore ? Difficulty.HARD : Difficulty.fromName(difficulty);
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

    public record ChunkSaveData(int chunkX, int chunkZ, short[] blockIds, byte[] metadata, long modificationVersion) {
    }

    public static class PlayerData {
        public float x;
        public float y;
        public float z;
        public float yaw;
        public float pitch;
        public float health;
        public float hunger;
        public float saturation;
        public float air;
        public int totalExperience;
        public int score;
        public float spawnX;
        public float spawnY;
        public float spawnZ;
        public List<StatusEffectInstance> activeEffects;

        static PlayerData from(Player player) {
            PlayerData data = new PlayerData();
            data.x = player.getPosition().x;
            data.y = player.getPosition().y;
            data.z = player.getPosition().z;
            data.yaw = player.getCamera().getYaw();
            data.pitch = player.getCamera().getPitch();
            data.health = player.getStats().getHealth();
            data.hunger = player.getStats().getHunger();
            data.saturation = player.getStats().getSaturation();
            data.air = player.getStats().getCurrentAir();
            data.totalExperience = player.getStats().getProgression().getTotalExperience();
            data.score = player.getStats().getProgression().getScore();
            data.spawnX = player.getSpawnX();
            data.spawnY = player.getSpawnY();
            data.spawnZ = player.getSpawnZ();
            data.activeEffects = new ArrayList<>(player.getStats().getActiveEffects());
            return data;
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

        private static StackData[] stackArray(ItemStack[] stacks) {
            StackData[] data = new StackData[stacks.length];
            for (int i = 0; i < stacks.length; i++) {
                data[i] = StackData.from(stacks[i]);
            }
            return data;
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

    public static class TileEntityData {
        public String type;
        public int x;
        public int y;
        public int z;
        public StackData[] inventory;
        public int burnTime;
        public int currentFuelBurnTime;
        public int cookTime;
        public float lidAngle;
        public String[] signText;
        public String mobType;
        public int spawnDelay;
        public int minSpawnDelay;
        public int maxSpawnDelay;
        public int spawnCount;
        public int maxNearbyEntities;

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
            } else if (tile instanceof SignTileEntity sign) {
                data.signText = java.util.Arrays.copyOf(sign.getLines(), sign.getLines().length);
            } else if (tile instanceof MonsterSpawnerTileEntity spawner) {
                data.mobType = spawner.getMobDefinition().name();
                data.spawnDelay = spawner.getDelay();
                data.minSpawnDelay = spawner.getMinDelay();
                data.maxSpawnDelay = spawner.getMaxDelay();
                data.spawnCount = spawner.getSpawnCount();
                data.maxNearbyEntities = spawner.getMaxNearbyEntities();
            }
            return data;
        }

        TileEntity toTileEntity() {
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
                furnace.clearDirty();
                return furnace;
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
                        spawner.setMobDefinition(MobDefinition.ZOMBIE);
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
                spawner.clearDirty();
                return spawner;
            }
            return null;
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
        public String type;
        public float x;
        public float y;
        public float z;
        public float motionX;
        public float motionY;
        public float motionZ;
        public float yaw;
        public float pitch;
        public float health;
        public int fireTicks;
        public int age;
        public int slimeSize;
        public int carriedBlockId;
        public int carriedMetadata;
        public boolean angry;
        public int angerTicks;

        static EntityData from(Entity entity) {
            if (!(entity instanceof Mob mob) || mob.isRemoved() || mob.isDead() || mob.getDefinition() == null) {
                return null;
            }
            EntityData data = new EntityData();
            data.type = mob.getDefinition().name();
            data.x = mob.getX();
            data.y = mob.getY();
            data.z = mob.getZ();
            data.motionX = mob.getMotionX();
            data.motionY = mob.getMotionY();
            data.motionZ = mob.getMotionZ();
            data.yaw = mob.getYaw();
            data.pitch = mob.getPitch();
            data.health = mob.getHealth();
            data.fireTicks = mob.getFireTicks();
            data.age = mob.getTicksExisted();
            if (mob instanceof Slime slime) {
                data.slimeSize = slime.getSize();
            }
            if (mob instanceof Enderman enderman) {
                data.carriedBlockId = enderman.getCarriedBlock().getId();
                data.carriedMetadata = enderman.getCarriedMetadata();
                data.angry = enderman.isAngry();
            }
            if (mob instanceof ZombiePigman pigman) {
                data.angerTicks = pigman.getAngerTicks();
            }
            return data;
        }

        Entity toEntity() {
            MobDefinition definition = parseDefinition(type);
            if (definition == null) {
                return null;
            }
            Mob mob = createMob(definition);
            if (mob == null) {
                return null;
            }
            mob.setPosition(x, y, z);
            mob.setMotion(motionX, motionY, motionZ);
            mob.setYaw(yaw);
            mob.setPitch(pitch);
            if (mob instanceof LivingEntity living) {
                living.setHealth(health <= 0.0f ? living.getMaxHealth() : health);
                if (fireTicks > 0) {
                    living.setOnFire(fireTicks);
                }
            }
            return mob;
        }

        private Mob createMob(MobDefinition definition) {
            if (definition == MobDefinition.SLIME) {
                return new Slime(slimeSize <= 0 ? 4 : slimeSize);
            }
            if (definition == MobDefinition.MAGMA_CUBE) {
                return new MagmaCube(slimeSize <= 0 ? 4 : slimeSize);
            }
            Mob mob = MobFactory.create(definition);
            if (mob instanceof Enderman enderman) {
                enderman.setCarriedBlock(BlockType.fromId(carriedBlockId), carriedMetadata);
                enderman.setAngry(angry);
            }
            if (mob instanceof ZombiePigman pigman) {
                pigman.setAngerTicks(angerTicks);
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
    }
}
