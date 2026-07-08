package com.craftzero.save;

import com.craftzero.inventory.Inventory;
import com.craftzero.inventory.ItemType;
import com.craftzero.inventory.ToolType;
import com.craftzero.main.Difficulty;
import com.craftzero.main.GameMode;
import com.craftzero.main.PlayerStats;
import com.craftzero.progression.ArmorMaterial;
import com.craftzero.progression.ArmorSlot;
import com.craftzero.progression.EnchantmentInstance;
import com.craftzero.progression.EnchantmentResolver;
import com.craftzero.progression.EnchantmentType;
import com.craftzero.progression.PlayerProgression;
import com.craftzero.progression.PotionData;
import com.craftzero.progression.PotionType;
import com.craftzero.progression.StatusEffectInstance;
import com.craftzero.progression.StatusEffectType;
import com.craftzero.world.DayCycleManager;
import com.craftzero.world.Dimension;
import com.craftzero.world.WorldGenerator;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

final class ReleaseLevelDat {
    static final String FILE_NAME = "level.dat";
    static final String OLD_FILE_NAME = "level.dat_old";
    static final String SESSION_LOCK_FILE_NAME = "session.lock";

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
    private static final int RELEASE_REGION_SAVE_VERSION = 19132;
    private static final int RELEASE_POTION_ENHANCED_BIT = 0x20;
    private static final int RELEASE_POTION_EXTENDED_BIT = 0x40;
    private static final int RELEASE_POTION_EFFECT_BIT = 0x2000;
    private static final int RELEASE_POTION_SPLASH_BIT = 0x4000;
    private static final int RELEASE_MAP_DEFAULT_SCALE = 3;
    private static final int MAX_NBT_ARRAY_LENGTH = 2_000_000;
    private static final int MAX_NBT_STRING_BYTES = 65_535;
    private static final String MAP_ID_PREFIX = "map_";
    private static final String LEGACY_NBT_METADATA_PREFIX = "legacyNbt.";
    private static final String LEGACY_DISPLAY_METADATA_PREFIX = "legacyDisplay.";
    private static final String LEGACY_SCALAR_NBT_METADATA_PREFIX = "nbt.";
    private static final String CZ_SLEEP_FOOT_X = "CraftZeroSleepingBedFootX";
    private static final String CZ_SLEEP_FOOT_Y = "CraftZeroSleepingBedFootY";
    private static final String CZ_SLEEP_FOOT_Z = "CraftZeroSleepingBedFootZ";
    private static final String CZ_SLEEP_HEAD_X = "CraftZeroSleepingBedHeadX";
    private static final String CZ_SLEEP_HEAD_Y = "CraftZeroSleepingBedHeadY";
    private static final String CZ_SLEEP_HEAD_Z = "CraftZeroSleepingBedHeadZ";
    private static final String CZ_SLEEP_RETURN_X = "CraftZeroSleepReturnX";
    private static final String CZ_SLEEP_RETURN_Y = "CraftZeroSleepReturnY";
    private static final String CZ_SLEEP_RETURN_Z = "CraftZeroSleepReturnZ";
    private static final String CZ_SLEEP_RETURN_YAW = "CraftZeroSleepReturnYaw";
    private static final String CZ_SLEEP_RETURN_PITCH = "CraftZeroSleepReturnPitch";
    private static final String CZ_PLAYER_REGEN_TIMER = "CraftZeroRegenTimer";
    private static final String CZ_PLAYER_PEACEFUL_REGEN_TIMER = "CraftZeroPeacefulRegenTimer";
    private static final String CZ_PLAYER_STARVATION_TIMER = "CraftZeroStarvationTimer";
    private static final String CZ_PLAYER_DROWN_TIMER = "CraftZeroDrownTimer";
    private static final String CZ_PLAYER_AIR_TICK_ACCUMULATOR = "CraftZeroAirTickAccumulator";
    private static final String CZ_PLAYER_INVINCIBILITY_TIMER = "CraftZeroInvincibilityTimer";
    private static final String CZ_PLAYER_HURT_INVULNERABILITY_TIMER = "CraftZeroHurtInvulnerabilityTimer";
    private static final String CZ_PLAYER_LAST_DAMAGE_AMOUNT = "CraftZeroLastDamageAmount";
    private static final String CZ_PLAYER_HURT_FLASH_TIMER = "CraftZeroHurtFlashTimer";
    private static final String CZ_CURSOR_ITEM = "CraftZeroCursorItem";
    private static final String CZ_RIDING_ENTITY_SAVE_ID = "CraftZeroRidingEntitySaveId";
    private static final String CZ_RIDING_ENTITY_TYPE = "CraftZeroRidingEntityType";

    private ReleaseLevelDat() {
    }

    static void write(Path path, SaveManager.LevelData level) throws IOException {
        write(path, level, 0L);
    }

    static void write(Path path, SaveManager.LevelData level, long sizeOnDisk) throws IOException {
        if (level == null) {
            return;
        }
        copyPreviousLevelDat(path);
        SafeFiles.writeAtomicBytes(path, stream -> {
            GZIPOutputStream gzip = new GZIPOutputStream(stream);
            DataOutputStream out = new DataOutputStream(gzip);
            out.writeByte(TAG_COMPOUND);
            writeString(out, "");
            writeNamedCompound(out, "Data");
            writeLong(out, "RandomSeed", level.seed);
            writeString(out, "LevelName", safeText(level.levelName, "Default World"));
            writeString(out, "generatorName", "default");
            writeInt(out, "generatorVersion", 1);
            writeInt(out, "GameType", level.getGameMode().id());
            writeByte(out, "MapFeatures", level.shouldGenerateStructures());
            writeInt(out, "SpawnX", level.spawnX);
            writeInt(out, "SpawnY", level.spawnY);
            writeInt(out, "SpawnZ", level.spawnZ);
            writeLong(out, "Time", releaseTime(level));
            writeLong(out, "LastPlayed", level.lastPlayed > 0 ? level.lastPlayed : System.currentTimeMillis());
            writeLong(out, "SizeOnDisk", Math.max(0L, sizeOnDisk));
            writeInt(out, "version", RELEASE_REGION_SAVE_VERSION);
            writeByte(out, "raining", "rain".equals(level.weatherState) || "thunder".equals(level.weatherState));
            writeInt(out, "rainTime", positiveOrDefault(level.weatherRainTime, 12_000));
            writeByte(out, "thundering", "thunder".equals(level.weatherState));
            writeInt(out, "thunderTime", positiveOrDefault(level.weatherThunderTime, 12_000));
            writeByte(out, "hardcore", level.hardcore);
            writeByte(out, "allowCommands", level.allowCheats);
            writeByte(out, "Difficulty", level.getDifficulty().id());
            writeInt(out, "Dimension", Dimension.fromSaveName(level.dimension).getId());
            writePlayer(out, level);
            out.writeByte(TAG_END);
            out.writeByte(TAG_END);
            out.flush();
            gzip.finish();
        }, SafeFiles.BackupPolicy.BAK);
    }

    static void writeSessionLock(Path path, long timestamp) throws IOException {
        SafeFiles.writeAtomicBytes(path, stream -> {
            DataOutputStream out = new DataOutputStream(stream);
            out.writeLong(timestamp);
            out.flush();
        }, SafeFiles.BackupPolicy.NONE);
    }

    static Long readSessionLock(Path path) throws IOException {
        if (path == null || !Files.isRegularFile(path)) {
            return null;
        }
        try (DataInputStream in = new DataInputStream(Files.newInputStream(path))) {
            return in.readLong();
        } catch (EOFException ignored) {
            return null;
        }
    }

    static PlayerSaveData readPlayer(Path path, SaveManager.LevelData level) throws IOException {
        Map<String, Object> root = readRoot(path);
        Map<String, Object> player = playerCompound(root);
        if (player.isEmpty()) {
            return null;
        }
        SaveManager.LevelData base = level == null ? new SaveManager.LevelData() : level;
        if (base.spawnY <= 0) {
            base.spawnY = 80;
        }
        return new PlayerSaveData(playerFrom(player, base), inventoryFrom(player));
    }

    static void writePlayer(Path path, SaveManager.LevelData level) throws IOException {
        if (level == null) {
            return;
        }
        SafeFiles.writeAtomicBytes(path, stream -> {
            GZIPOutputStream gzip = new GZIPOutputStream(stream);
            DataOutputStream out = new DataOutputStream(gzip);
            out.writeByte(TAG_COMPOUND);
            writeString(out, "");
            writePlayerPayload(out, level);
            out.writeByte(TAG_END);
            out.flush();
            gzip.finish();
        }, SafeFiles.BackupPolicy.BAK);
    }

    static SaveManager.LevelData read(Path path) throws IOException {
        Map<String, Object> root = readRoot(path);
        Map<String, Object> data = compound(root.get("Data"));
        if (data.isEmpty()) {
            data = root;
        }

        SaveManager.LevelData level = new SaveManager.LevelData();
        level.formatVersion = SaveManager.FORMAT_VERSION;
        level.targetVersion = SaveManager.TARGET_VERSION;
        level.levelName = string(data, "LevelName", "Imported World");
        level.lastPlayed = longValue(data, "LastPlayed", System.currentTimeMillis());
        level.gameMode = GameMode.fromId(intValue(data, "GameType", 0)).name();
        level.hardcore = byteBoolean(data, "hardcore", false);
        level.allowCheats = byteBoolean(data, "allowCommands", false);
        level.difficulty = Difficulty.fromId(intValue(data, "Difficulty", Difficulty.NORMAL.id())).name();
        level.seed = longValue(data, "RandomSeed", 0L);
        level.serverLevelSeed = Long.toString(level.seed);
        level.generateStructures = byteBoolean(data, "MapFeatures", true);
        level.generatorId = WorldGenerator.RELEASE_ONE;
        level.dimension = dimensionFromId(intValue(data, "Dimension", 0)).getSaveName();
        level.spawnX = intValue(data, "SpawnX", 0);
        level.spawnY = intValue(data, "SpawnY", 80);
        level.spawnZ = intValue(data, "SpawnZ", 0);
        long worldTime = Math.max(0L, longValue(data, "Time", 0L));
        level.worldTime = worldTime;
        level.time = Math.floorMod(worldTime, DayCycleManager.TICKS_PER_DAY);
        level.dayCount = (int) Math.min(Integer.MAX_VALUE, worldTime / DayCycleManager.TICKS_PER_DAY);
        level.moonPhase = Math.floorMod(level.dayCount, 8);
        boolean thundering = byteBoolean(data, "thundering", false);
        boolean raining = byteBoolean(data, "raining", false) || thundering;
        level.weatherState = thundering ? "thunder" : (raining ? "rain" : "clear");
        level.weatherRainTime = Math.max(1, intValue(data, "rainTime", 12_000));
        level.weatherThunderTime = Math.max(1, intValue(data, "thunderTime", 12_000));
        level.operators = new ArrayList<>();
        level.bannedPlayers = new ArrayList<>();
        level.bannedIps = new ArrayList<>();
        level.whitelist = new ArrayList<>();
        level.filledMaps = new ArrayList<>();
        Map<String, Object> playerData = compound(data.get("Player"));
        level.player = playerData.isEmpty() ? defaultPlayer(level) : playerFrom(playerData, level);
        level.inventory = playerData.isEmpty() ? defaultInventory() : inventoryFrom(playerData);
        level.droppedItems = new ArrayList<>();
        level.tileEntities = new ArrayList<>();
        level.entities = new ArrayList<>();
        level.movingPistons = new ArrayList<>();
        level.scheduledBlockTicks = new ArrayList<>();
        return level;
    }

    private static void copyPreviousLevelDat(Path path) throws IOException {
        if (path == null || !Files.isRegularFile(path)) {
            return;
        }
        Files.copy(path, path.resolveSibling(OLD_FILE_NAME), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
    }

    private static SaveManager.PlayerData playerFrom(Map<String, Object> data, SaveManager.LevelData level) {
        SaveManager.PlayerData player = defaultPlayer(level);
        player.x = (float) listDouble(data.get("Pos"), 0, player.x);
        player.y = (float) listDouble(data.get("Pos"), 1, player.y);
        player.z = (float) listDouble(data.get("Pos"), 2, player.z);
        player.motionX = (float) listDouble(data.get("Motion"), 0, 0.0d);
        player.motionY = (float) listDouble(data.get("Motion"), 1, 0.0d);
        player.motionZ = (float) listDouble(data.get("Motion"), 2, 0.0d);
        player.onGround = byteBoolean(data, "OnGround", true);
        float fallDistance = Math.max(0.0f, floatValue(data, "FallDistance", 0.0f));
        player.fallStartY = player.y + fallDistance;
        player.wasFalling = fallDistance > 0.0f && !player.onGround;
        player.yaw = listFloat(data.get("Rotation"), 0, 0.0f);
        player.pitch = listFloat(data.get("Rotation"), 1, 0.0f);
        player.health = clamp(shortValue(data, "Health", Math.round(PlayerStats.MAX_HEALTH)),
                0.0f, PlayerStats.MAX_HEALTH);
        player.hunger = clamp(intValue(data, "foodLevel", Math.round(PlayerStats.MAX_HUNGER)),
                0.0f, PlayerStats.MAX_HUNGER);
        player.saturation = clamp(floatValue(data, "foodSaturationLevel", Math.min(5.0f, player.hunger)),
                0.0f, player.hunger);
        player.exhaustion = clamp(floatValue(data, "foodExhaustionLevel", 0.0f),
                0.0f, PlayerStats.MAX_EXHAUSTION);
        player.air = clamp(shortValue(data, "Air", Math.round(PlayerStats.MAX_AIR_SECONDS * 20.0f)) / 20.0f,
                0.0f, PlayerStats.MAX_AIR_SECONDS);
        player.fireTicks = Math.max(0, shortValue(data, "Fire", 0));
        importPlayerRuntimeState(player, data);
        player.totalExperience = releaseTotalExperience(data);
        player.score = Math.max(0, intValue(data, "Score", 0));
        if (hasNumber(data, "SpawnX") && hasNumber(data, "SpawnY") && hasNumber(data, "SpawnZ")) {
            player.spawnX = intValue(data, "SpawnX", level.spawnX) + 0.5f;
            player.spawnY = intValue(data, "SpawnY", level.spawnY);
            player.spawnZ = intValue(data, "SpawnZ", level.spawnZ) + 0.5f;
            player.bedSpawnSet = true;
            player.bedSpawnX = intValue(data, "SpawnX", level.spawnX);
            player.bedSpawnY = intValue(data, "SpawnY", level.spawnY);
            player.bedSpawnZ = intValue(data, "SpawnZ", level.spawnZ);
        }
        player.achievements = new ArrayList<>();
        player.activeEffects = activeEffectsFrom(data.get("ActiveEffects"));
        importSleepingState(player, data);
        importPlayerRidingState(player, data);
        return player;
    }

    private static void importPlayerRidingState(SaveManager.PlayerData player, Map<String, Object> data) {
        if (player == null || data == null) {
            return;
        }
        int saveId = Math.max(0, intValue(data, CZ_RIDING_ENTITY_SAVE_ID, 0));
        String type = normalizedRidingType(stringValue(data, CZ_RIDING_ENTITY_TYPE, ""));
        if (saveId > 0 && type != null) {
            player.ridingEntitySaveId = saveId;
            player.ridingEntityType = type;
        }
    }

    private static void importSleepingState(SaveManager.PlayerData player, Map<String, Object> data) {
        if (player == null || data == null || !byteBoolean(data, "Sleeping", false)
                || !hasCraftZeroSleepingState(data)) {
            return;
        }
        player.sleeping = true;
        player.sleepingBedFootX = intValue(data, CZ_SLEEP_FOOT_X, 0);
        player.sleepingBedFootY = intValue(data, CZ_SLEEP_FOOT_Y, 0);
        player.sleepingBedFootZ = intValue(data, CZ_SLEEP_FOOT_Z, 0);
        player.sleepingBedHeadX = intValue(data, CZ_SLEEP_HEAD_X, 0);
        player.sleepingBedHeadY = intValue(data, CZ_SLEEP_HEAD_Y, 0);
        player.sleepingBedHeadZ = intValue(data, CZ_SLEEP_HEAD_Z, 0);
        player.sleepReturnX = floatValue(data, CZ_SLEEP_RETURN_X, player.x);
        player.sleepReturnY = floatValue(data, CZ_SLEEP_RETURN_Y, player.y);
        player.sleepReturnZ = floatValue(data, CZ_SLEEP_RETURN_Z, player.z);
        player.sleepReturnYaw = floatValue(data, CZ_SLEEP_RETURN_YAW, player.yaw);
        player.sleepReturnPitch = floatValue(data, CZ_SLEEP_RETURN_PITCH, player.pitch);
    }

    private static boolean hasCraftZeroSleepingState(Map<String, Object> data) {
        return hasNumber(data, CZ_SLEEP_FOOT_X)
                && hasNumber(data, CZ_SLEEP_FOOT_Y)
                && hasNumber(data, CZ_SLEEP_FOOT_Z)
                && hasNumber(data, CZ_SLEEP_HEAD_X)
                && hasNumber(data, CZ_SLEEP_HEAD_Y)
                && hasNumber(data, CZ_SLEEP_HEAD_Z)
                && hasNumber(data, CZ_SLEEP_RETURN_X)
                && hasNumber(data, CZ_SLEEP_RETURN_Y)
                && hasNumber(data, CZ_SLEEP_RETURN_Z)
                && hasNumber(data, CZ_SLEEP_RETURN_YAW)
                && hasNumber(data, CZ_SLEEP_RETURN_PITCH);
    }

    private static SaveManager.InventoryData inventoryFrom(Map<String, Object> playerData) {
        SaveManager.InventoryData inventory = defaultInventory();
        inventory.selectedSlot = clampInt(intValue(playerData, "SelectedItemSlot", 0), 0, Inventory.HOTBAR_SIZE - 1);
        for (Map<String, Object> item : compoundList(playerData.get("Inventory"))) {
            int slot = byteValue(item, "Slot", -1);
            SaveManager.StackData stack = stackFromItemCompound(item);
            if (stack == null) {
                continue;
            }
            if (slot >= 0 && slot < Inventory.HOTBAR_SIZE) {
                inventory.hotbar[slot] = stack;
            } else if (slot >= Inventory.HOTBAR_SIZE
                    && slot < Inventory.HOTBAR_SIZE + Inventory.MAIN_SIZE) {
                inventory.main[slot - Inventory.HOTBAR_SIZE] = stack;
            } else if (slot >= 80 && slot < 80 + Inventory.CRAFTING_SIZE) {
                inventory.crafting[slot - 80] = stack;
            } else if (slot >= 100 && slot <= 103) {
                inventory.armor[103 - slot] = stack;
            }
        }
        inventory.cursor = stackFromItemCompound(compound(playerData.get(CZ_CURSOR_ITEM)));
        return inventory;
    }

    private static SaveManager.StackData stackFromItemCompound(Map<String, Object> item) {
        int id = shortValue(item, "id", 0);
        int count = byteValue(item, "Count", 0);
        int damage = unsignedShortValue(item, "Damage", 0);
        ItemType type = ItemType.fromId(id, damage);
        if (type == null || count <= 0) {
            return null;
        }
        SaveManager.StackData stack = new SaveManager.StackData();
        stack.itemId = id;
        stack.count = Math.min(count, type.getMaxStackSize());
        if (type.isDamageable()) {
            stack.dataValue = type.getDataValue();
            stack.durability = Math.max(1, type.getMaxDurability() - Math.max(0, damage));
        } else if (type == ItemType.MAP) {
            stack.dataValue = type.getDataValue();
            stack.durability = Math.max(0, damage);
            stack.metadata = new HashMap<>();
            stack.metadata.put("map.initialized", "true");
            stack.metadata.put("map.id", MAP_ID_PREFIX + Math.max(0, damage));
            stack.metadata.put("map.scale", Integer.toString(RELEASE_MAP_DEFAULT_SCALE));
        } else {
            stack.dataValue = Math.max(0, damage);
            stack.durability = -1;
        }
        if (type == ItemType.POTION) {
            stack.potion = potionDataFromReleaseDamage(damage);
        }
        importStackTag(stack, compound(item.get("tag")));
        return stack;
    }

    private static void writePlayer(DataOutputStream out, SaveManager.LevelData level) throws IOException {
        writeNamedCompound(out, "Player");
        writePlayerPayload(out, level);
        out.writeByte(TAG_END);
    }

    private static void writePlayerPayload(DataOutputStream out, SaveManager.LevelData level) throws IOException {
        SaveManager.PlayerData player = level.player == null ? defaultPlayer(level) : level.player;
        SaveManager.InventoryData inventory = level.inventory == null ? defaultInventory() : level.inventory;
        writeDoubleList(out, "Pos", player.x, player.y, player.z);
        writeDoubleList(out, "Motion",
                player.motionX == null ? 0.0f : player.motionX,
                player.motionY == null ? 0.0f : player.motionY,
                player.motionZ == null ? 0.0f : player.motionZ);
        writeFloatList(out, "Rotation", player.yaw, player.pitch);
        writeShort(out, "Health", Math.round(clamp(player.health, 0.0f, PlayerStats.MAX_HEALTH)));
        writeShort(out, "Fire", Math.max(0, player.fireTicks));
        writeShort(out, "Air", Math.round(clamp(player.air, 0.0f, PlayerStats.MAX_AIR_SECONDS) * 20.0f));
        writeByte(out, "OnGround", player.onGround == null || player.onGround);
        writeFloat(out, "FallDistance", releasePlayerFallDistance(player));
        writeShort(out, "HurtTime", releasePlayerHurtTime(player));
        writeShort(out, "DeathTime", Math.max(0, player.deathTime));
        writeShort(out, "AttackTime", 0);
        writeInt(out, "Dimension", Dimension.fromSaveName(level.dimension).getId());
        writeInt(out, "SelectedItemSlot", clampInt(inventory.selectedSlot, 0, Inventory.HOTBAR_SIZE - 1));
        writeInt(out, "Score", Math.max(0, player.score));
        int totalExperience = Math.max(0, player.totalExperience);
        writeInt(out, "XpTotal", totalExperience);
        writeInt(out, "XpLevel", releaseExperienceLevel(totalExperience));
        writeFloat(out, "XpP", releaseExperienceProgress(totalExperience));
        writeInt(out, "foodLevel", Math.round(clamp(player.hunger, 0.0f, PlayerStats.MAX_HUNGER)));
        writeFloat(out, "foodSaturationLevel", clamp(player.saturation, 0.0f, PlayerStats.MAX_SATURATION));
        writeFloat(out, "foodExhaustionLevel", clamp(player.exhaustion, 0.0f, PlayerStats.MAX_EXHAUSTION));
        writeInt(out, "foodTickTimer", Math.max(0, player.foodTickTimer));
        writeSpawn(out, player);
        writeInventory(out, inventory);
        writeCursorItem(out, inventory);
        writeActiveEffects(out, player.activeEffects);
        writePlayerRuntimeState(out, player);
        writePlayerRidingState(out, player);
        boolean sleeping = player.hasCompleteSleepingState();
        writeByte(out, "Sleeping", sleeping);
        writeShort(out, "SleepTimer", sleeping ? 100 : 0);
        writeSleepingState(out, player);
    }

    private static void importPlayerRuntimeState(SaveManager.PlayerData player, Map<String, Object> data) {
        int hurtTime = Math.max(0, shortValue(data, "HurtTime", 0));
        player.hurtFlashTimer = clamp(hurtTime / 20.0f, 0.0f, 0.5f);
        player.deathTime = Math.max(0, shortValue(data, "DeathTime", 0));
        player.foodTickTimer = Math.max(0, intValue(data, "foodTickTimer", 0));
        player.regenTimer = clamp(floatValue(data, CZ_PLAYER_REGEN_TIMER, 0.0f), 0.0f, 4.0f);
        player.peacefulRegenTimer = clamp(floatValue(data, CZ_PLAYER_PEACEFUL_REGEN_TIMER, 0.0f), 0.0f, 1.0f);
        player.starvationTimer = clamp(floatValue(data, CZ_PLAYER_STARVATION_TIMER, 0.0f), 0.0f, 4.0f);
        player.drownTimer = clamp(floatValue(data, CZ_PLAYER_DROWN_TIMER, 0.0f), 0.0f, 1.0f);
        player.airTickAccumulator = clamp(floatValue(data, CZ_PLAYER_AIR_TICK_ACCUMULATOR, 0.0f), 0.0f, 0.9999f);
        player.invincibilityTimer = clamp(floatValue(data, CZ_PLAYER_INVINCIBILITY_TIMER, 0.0f), 0.0f, 5.0f);
        if (hasNumber(data, CZ_PLAYER_HURT_INVULNERABILITY_TIMER)) {
            player.hurtInvulnerabilityTimer = clamp(floatValue(data, CZ_PLAYER_HURT_INVULNERABILITY_TIMER, 0.0f),
                    0.0f, 1.0f);
            player.lastDamageAmount = clamp(floatValue(data, CZ_PLAYER_LAST_DAMAGE_AMOUNT, 0.0f),
                    0.0f, PlayerStats.MAX_HEALTH);
        } else if (hurtTime > 0) {
            player.hurtInvulnerabilityTimer = clamp((hurtTime + 10.0f) / 20.0f, 0.0f, 1.0f);
            player.lastDamageAmount = 1.0f;
        }
        if (hasNumber(data, CZ_PLAYER_HURT_FLASH_TIMER)) {
            player.hurtFlashTimer = clamp(floatValue(data, CZ_PLAYER_HURT_FLASH_TIMER, player.hurtFlashTimer),
                    0.0f, 0.5f);
        }
        if (player.foodTickTimer > 0 && player.regenTimer <= 0.0f && player.starvationTimer <= 0.0f) {
            float foodTimerSeconds = player.foodTickTimer / 20.0f;
            if (player.hunger >= 18.0f && player.health < PlayerStats.MAX_HEALTH) {
                player.regenTimer = clamp(foodTimerSeconds, 0.0f, 4.0f);
            } else if (player.hunger <= 0.0f && player.health > 0.0f) {
                player.starvationTimer = clamp(foodTimerSeconds, 0.0f, 4.0f);
            }
        }
    }

    private static void writePlayerRuntimeState(DataOutputStream out, SaveManager.PlayerData player)
            throws IOException {
        writeFloat(out, CZ_PLAYER_REGEN_TIMER, Math.max(0.0f, player.regenTimer));
        writeFloat(out, CZ_PLAYER_PEACEFUL_REGEN_TIMER, Math.max(0.0f, player.peacefulRegenTimer));
        writeFloat(out, CZ_PLAYER_STARVATION_TIMER, Math.max(0.0f, player.starvationTimer));
        writeFloat(out, CZ_PLAYER_DROWN_TIMER, Math.max(0.0f, player.drownTimer));
        writeFloat(out, CZ_PLAYER_AIR_TICK_ACCUMULATOR, Math.max(0.0f, player.airTickAccumulator));
        writeFloat(out, CZ_PLAYER_INVINCIBILITY_TIMER, Math.max(0.0f, player.invincibilityTimer));
        writeFloat(out, CZ_PLAYER_HURT_INVULNERABILITY_TIMER, Math.max(0.0f, player.hurtInvulnerabilityTimer));
        writeFloat(out, CZ_PLAYER_LAST_DAMAGE_AMOUNT, Math.max(0.0f, player.lastDamageAmount));
        writeFloat(out, CZ_PLAYER_HURT_FLASH_TIMER, Math.max(0.0f, player.hurtFlashTimer));
    }

    private static void writePlayerRidingState(DataOutputStream out, SaveManager.PlayerData player)
            throws IOException {
        if (player == null || player.ridingEntitySaveId <= 0) {
            return;
        }
        String type = normalizedRidingType(player.ridingEntityType);
        if (type == null) {
            return;
        }
        writeInt(out, CZ_RIDING_ENTITY_SAVE_ID, player.ridingEntitySaveId);
        writeString(out, CZ_RIDING_ENTITY_TYPE, type);
    }

    private static String normalizedRidingType(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "MINECART", "BOAT", "PIG" -> normalized;
            default -> null;
        };
    }

    private static float releasePlayerFallDistance(SaveManager.PlayerData player) {
        if (player == null || player.fallStartY == null || !Float.isFinite(player.fallStartY)) {
            return 0.0f;
        }
        return Math.max(0.0f, player.fallStartY - player.y);
    }

    private static int releasePlayerHurtTime(SaveManager.PlayerData player) {
        if (player == null) {
            return 0;
        }
        if (player.hurtFlashTimer > 0.0f && Float.isFinite(player.hurtFlashTimer)) {
            return Math.min(10, Math.max(0, Math.round(player.hurtFlashTimer * 20.0f)));
        }
        float visibleTicks = player.hurtInvulnerabilityTimer * 20.0f - 10.0f;
        return Math.min(10, Math.max(0, Math.round(visibleTicks)));
    }

    private static void writeSleepingState(DataOutputStream out, SaveManager.PlayerData player) throws IOException {
        if (player == null || !player.hasCompleteSleepingState()) {
            return;
        }
        writeInt(out, CZ_SLEEP_FOOT_X, player.sleepingBedFootX);
        writeInt(out, CZ_SLEEP_FOOT_Y, player.sleepingBedFootY);
        writeInt(out, CZ_SLEEP_FOOT_Z, player.sleepingBedFootZ);
        writeInt(out, CZ_SLEEP_HEAD_X, player.sleepingBedHeadX);
        writeInt(out, CZ_SLEEP_HEAD_Y, player.sleepingBedHeadY);
        writeInt(out, CZ_SLEEP_HEAD_Z, player.sleepingBedHeadZ);
        writeFloat(out, CZ_SLEEP_RETURN_X, player.sleepReturnX);
        writeFloat(out, CZ_SLEEP_RETURN_Y, player.sleepReturnY);
        writeFloat(out, CZ_SLEEP_RETURN_Z, player.sleepReturnZ);
        writeFloat(out, CZ_SLEEP_RETURN_YAW, player.sleepReturnYaw);
        writeFloat(out, CZ_SLEEP_RETURN_PITCH, player.sleepReturnPitch);
    }

    private static void writeSpawn(DataOutputStream out, SaveManager.PlayerData player) throws IOException {
        if (player.bedSpawnSet) {
            writeInt(out, "SpawnX", player.bedSpawnX);
            writeInt(out, "SpawnY", player.bedSpawnY);
            writeInt(out, "SpawnZ", player.bedSpawnZ);
            return;
        }
        if (player.spawnY > 0.0f) {
            writeInt(out, "SpawnX", (int) Math.floor(player.spawnX));
            writeInt(out, "SpawnY", (int) Math.floor(player.spawnY));
            writeInt(out, "SpawnZ", (int) Math.floor(player.spawnZ));
        }
    }

    private static void writeInventory(DataOutputStream out, SaveManager.InventoryData inventory)
            throws IOException {
        ArrayList<CompoundWriter> writers = new ArrayList<>();
        addInventoryWriters(writers, inventory.hotbar, 0);
        addInventoryWriters(writers, inventory.main, Inventory.HOTBAR_SIZE);
        addInventoryWriters(writers, inventory.crafting, 80);
        if (inventory.armor != null) {
            for (int i = 0; i < inventory.armor.length; i++) {
                SaveManager.StackData stack = inventory.armor[i];
                if (stack != null && stack.count > 0) {
                    int slot = 103 - i;
                    writers.add(itemOut -> {
                        writeByte(itemOut, "Slot", slot);
                        writeStackPayload(itemOut, stack);
                    });
                }
            }
        }
        writeCompoundList(out, "Inventory", writers);
    }

    private static void writeCursorItem(DataOutputStream out, SaveManager.InventoryData inventory)
            throws IOException {
        if (inventory == null || inventory.cursor == null || inventory.cursor.count <= 0) {
            return;
        }
        writeNamedCompound(out, CZ_CURSOR_ITEM);
        writeStackPayload(out, inventory.cursor);
        out.writeByte(TAG_END);
    }

    private static void addInventoryWriters(List<CompoundWriter> writers, SaveManager.StackData[] stacks,
            int baseSlot) {
        if (stacks == null) {
            return;
        }
        for (int i = 0; i < stacks.length; i++) {
            SaveManager.StackData stack = stacks[i];
            if (stack != null && stack.count > 0) {
                int slot = baseSlot + i;
                writers.add(itemOut -> {
                    writeByte(itemOut, "Slot", slot);
                    writeStackPayload(itemOut, stack);
                });
            }
        }
    }

    private static void writeStackPayload(DataOutputStream out, SaveManager.StackData stack) throws IOException {
        ItemType type = ItemType.fromId(stack.itemId, stack.dataValue);
        int releaseDamage = releaseItemDamage(type, stack);
        writeShort(out, "id", stack.itemId);
        writeByte(out, "Count", stack.count);
        writeShort(out, "Damage", releaseDamage);
        if (hasStackTag(stack)) {
            writeStackTag(out, stack);
        }
    }

    private static void importStackTag(SaveManager.StackData stack, Map<String, Object> tag) {
        if (tag.isEmpty()) {
            return;
        }
        Map<String, Object> display = compound(tag.get("display"));
        String customName = string(display, "Name", "");
        if (!customName.isBlank()) {
            stack.customName = customName;
        }
        importLegacyDisplayMetadata(stack, display);
        ItemType itemType = ItemType.fromId(stack.itemId, stack.dataValue);
        ArrayList<EnchantmentInstance> enchantments = new ArrayList<>();
        importEnchantments(enchantments, itemType, tag.get("ench"));
        importEnchantments(enchantments, itemType, tag.get("StoredEnchantments"));
        if (!enchantments.isEmpty()) {
            stack.enchantments = enchantments;
        }
        importCraftZeroMetadata(stack, tag);
        importLegacyTagMetadata(stack, tag);
    }

    private static void importEnchantments(ArrayList<EnchantmentInstance> out, ItemType itemType, Object value) {
        for (Map<String, Object> data : compoundList(value)) {
            EnchantmentType type = enchantmentTypeById(shortValue(data, "id", -1));
            if (type == null || containsEnchantment(out, type) || !canApplyEnchantment(itemType, type)) {
                continue;
            }
            int maxLevel = maxEnchantmentLevel(type);
            int level = Math.max(1, Math.min(maxLevel, shortValue(data, "lvl", 1)));
            if (level <= 0 || !compatibleWithImportedEnchantments(out, type)) {
                continue;
            }
            out.add(new EnchantmentInstance(type, level));
        }
    }

    private static boolean containsEnchantment(List<EnchantmentInstance> enchantments, EnchantmentType type) {
        for (EnchantmentInstance enchantment : enchantments) {
            if (enchantment != null && enchantment.type() == type) {
                return true;
            }
        }
        return false;
    }

    private static boolean compatibleWithImportedEnchantments(List<EnchantmentInstance> enchantments,
            EnchantmentType type) {
        for (EnchantmentInstance enchantment : enchantments) {
            if (enchantment != null && !EnchantmentResolver.compatible(enchantment.type(), type)) {
                return false;
            }
        }
        return true;
    }

    private static EnchantmentType enchantmentTypeById(int id) {
        for (EnchantmentType type : EnchantmentType.values()) {
            if (type.getId() == id) {
                return type;
            }
        }
        return null;
    }

    private static int maxEnchantmentLevel(EnchantmentType type) {
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

    private static boolean canApplyEnchantment(ItemType itemType, EnchantmentType enchantmentType) {
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

    private static void importCraftZeroMetadata(SaveManager.StackData stack, Map<String, Object> tag) {
        Map<String, Object> craftZero = compound(tag.get("CraftZero"));
        Map<String, Object> metadata = compound(craftZero.get("Metadata"));
        for (Map.Entry<String, Object> entry : metadata.entrySet()) {
            String value = metadataStringValue(entry.getValue());
            if (value != null) {
                putStackMetadata(stack, entry.getKey(), value);
            }
        }
    }

    private static void importLegacyDisplayMetadata(SaveManager.StackData stack, Map<String, Object> display) {
        for (Map.Entry<String, Object> entry : display.entrySet()) {
            String key = entry.getKey();
            if ("Name".equals(key)) {
                continue;
            }
            putEncodedLegacyMetadata(stack, LEGACY_DISPLAY_METADATA_PREFIX + key, entry.getValue());
        }
    }

    private static void importLegacyTagMetadata(SaveManager.StackData stack, Map<String, Object> tag) {
        for (Map.Entry<String, Object> entry : tag.entrySet()) {
            String key = entry.getKey();
            if (isKnownStackTagKey(key)) {
                continue;
            }
            putEncodedLegacyMetadata(stack, LEGACY_NBT_METADATA_PREFIX + key, entry.getValue());
        }
    }

    private static boolean isKnownStackTagKey(String key) {
        return "display".equals(key)
                || "ench".equals(key)
                || "CraftZero".equals(key);
    }

    private static String metadataStringValue(Object value) {
        if (value instanceof String text) {
            return text;
        }
        if (value instanceof Number number) {
            return number.toString();
        }
        return null;
    }

    private static void putStackMetadata(SaveManager.StackData stack, String key, String value) {
        if (key == null || key.isBlank() || value == null) {
            return;
        }
        if (stack.metadata == null) {
            stack.metadata = new HashMap<>();
        }
        stack.metadata.put(key, value);
    }

    private static void putEncodedLegacyMetadata(SaveManager.StackData stack, String key, Object value) {
        String encoded = encodeLegacyTag(value);
        if (encoded != null) {
            putStackMetadata(stack, key, encoded);
        }
    }

    private static boolean hasStackTag(SaveManager.StackData stack) {
        return hasDisplayTag(stack)
                || hasWritableEnchantments(stack.enchantments)
                || hasWritableLegacyMetadata(stack.metadata, LEGACY_NBT_METADATA_PREFIX, true)
                || hasWritableLegacyScalarMetadata(stack.metadata)
                || hasCraftZeroMetadata(stack.metadata);
    }

    private static void writeStackTag(DataOutputStream out, SaveManager.StackData stack) throws IOException {
        writeNamedCompound(out, "tag");
        writeDisplayTag(out, stack);
        writeEnchantmentList(out, "ench", stack.enchantments);
        writeLegacyMetadataTags(out, stack.metadata, LEGACY_NBT_METADATA_PREFIX, true);
        writeLegacyScalarMetadataTags(out, stack.metadata);
        writeCraftZeroMetadata(out, stack.metadata);
        out.writeByte(TAG_END);
    }

    private static void writeDisplayTag(DataOutputStream out, SaveManager.StackData stack) throws IOException {
        if (!hasDisplayTag(stack)) {
            return;
        }
        writeNamedCompound(out, "display");
        if (stack.customName != null && !stack.customName.isBlank()) {
            writeString(out, "Name", stack.customName);
        }
        writeLegacyMetadataTags(out, stack.metadata, LEGACY_DISPLAY_METADATA_PREFIX, false);
        out.writeByte(TAG_END);
    }

    private static boolean hasDisplayTag(SaveManager.StackData stack) {
        return stack != null
                && (stack.customName != null && !stack.customName.isBlank()
                        || hasWritableLegacyMetadata(stack.metadata, LEGACY_DISPLAY_METADATA_PREFIX, false));
    }

    private static boolean hasWritableEnchantments(List<EnchantmentInstance> enchantments) {
        if (enchantments == null || enchantments.isEmpty()) {
            return false;
        }
        for (EnchantmentInstance enchantment : enchantments) {
            if (enchantment != null && enchantment.type() != null && enchantment.level() > 0) {
                return true;
            }
        }
        return false;
    }

    private static void writeEnchantmentList(DataOutputStream out, String name,
            List<EnchantmentInstance> enchantments) throws IOException {
        if (enchantments == null || enchantments.isEmpty()) {
            return;
        }
        ArrayList<CompoundWriter> writers = new ArrayList<>();
        for (EnchantmentInstance enchantment : enchantments) {
            if (enchantment == null || enchantment.type() == null || enchantment.level() <= 0) {
                continue;
            }
            writers.add(enchantmentOut -> {
                writeShort(enchantmentOut, "id", enchantment.type().getId());
                writeShort(enchantmentOut, "lvl", enchantment.level());
            });
        }
        if (!writers.isEmpty()) {
            writeCompoundList(out, name, writers);
        }
    }

    private static void writeCraftZeroMetadata(DataOutputStream out, Map<String, String> metadata)
            throws IOException {
        if (!hasCraftZeroMetadata(metadata)) {
            return;
        }
        writeNamedCompound(out, "CraftZero");
        writeNamedCompound(out, "Metadata");
        for (Map.Entry<String, String> entry : new TreeMap<>(metadata).entrySet()) {
            if (shouldWriteCraftZeroMetadata(entry)) {
                writeString(out, entry.getKey(), entry.getValue());
            }
        }
        out.writeByte(TAG_END);
        out.writeByte(TAG_END);
    }

    private static boolean hasCraftZeroMetadata(Map<String, String> metadata) {
        if (metadata == null || metadata.isEmpty()) {
            return false;
        }
        for (Map.Entry<String, String> entry : metadata.entrySet()) {
            if (shouldWriteCraftZeroMetadata(entry)) {
                return true;
            }
        }
        return false;
    }

    private static boolean shouldWriteCraftZeroMetadata(Map.Entry<String, String> entry) {
        if (entry.getKey() == null || entry.getKey().isBlank() || entry.getValue() == null) {
            return false;
        }
        return !entry.getKey().startsWith(LEGACY_NBT_METADATA_PREFIX)
                && !entry.getKey().startsWith(LEGACY_DISPLAY_METADATA_PREFIX)
                && !entry.getKey().startsWith(LEGACY_SCALAR_NBT_METADATA_PREFIX);
    }

    private static boolean hasLegacyMetadata(Map<String, String> metadata, String prefix) {
        if (metadata == null || metadata.isEmpty()) {
            return false;
        }
        for (String key : metadata.keySet()) {
            if (isLegacyMetadataKey(key, prefix)) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasWritableLegacyMetadata(Map<String, String> metadata, String prefix,
            boolean skipKnownTopLevelTags) {
        if (metadata == null || metadata.isEmpty()) {
            return false;
        }
        for (Map.Entry<String, String> entry : metadata.entrySet()) {
            String key = entry.getKey();
            if (!isLegacyMetadataKey(key, prefix)) {
                continue;
            }
            String tagName = key.substring(prefix.length());
            if (skipKnownTopLevelTags && isKnownStackTagKey(tagName)) {
                continue;
            }
            LegacyTag tag = decodeLegacyTag(entry.getValue());
            if (tag != null && tag.type() != TAG_END) {
                return true;
            }
        }
        return false;
    }

    private static void writeLegacyMetadataTags(DataOutputStream out, Map<String, String> metadata,
            String prefix, boolean skipKnownTopLevelTags) throws IOException {
        if (metadata == null || metadata.isEmpty()) {
            return;
        }
        for (Map.Entry<String, String> entry : new TreeMap<>(metadata).entrySet()) {
            String key = entry.getKey();
            if (!isLegacyMetadataKey(key, prefix)) {
                continue;
            }
            String tagName = key.substring(prefix.length());
            if (skipKnownTopLevelTags && isKnownStackTagKey(tagName)) {
                continue;
            }
            LegacyTag tag = decodeLegacyTag(entry.getValue());
            if (tag == null || tag.type() == TAG_END) {
                continue;
            }
            out.writeByte(tag.type());
            writeString(out, tagName);
            writeLegacyPayload(out, tag.type(), tag.value());
        }
    }

    private static void writeLegacyScalarMetadataTags(DataOutputStream out, Map<String, String> metadata)
            throws IOException {
        if (metadata == null || metadata.isEmpty()) {
            return;
        }
        for (Map.Entry<String, String> entry : new TreeMap<>(metadata).entrySet()) {
            String key = entry.getKey();
            if (!isLegacyMetadataKey(key, LEGACY_SCALAR_NBT_METADATA_PREFIX)) {
                continue;
            }
            String tagName = key.substring(LEGACY_SCALAR_NBT_METADATA_PREFIX.length());
            if (isKnownStackTagKey(tagName)
                    || metadata.containsKey(LEGACY_NBT_METADATA_PREFIX + tagName)
                    || entry.getValue() == null) {
                continue;
            }
            writeString(out, tagName, entry.getValue());
        }
    }

    private static boolean hasWritableLegacyScalarMetadata(Map<String, String> metadata) {
        if (metadata == null || metadata.isEmpty()) {
            return false;
        }
        for (Map.Entry<String, String> entry : metadata.entrySet()) {
            String key = entry.getKey();
            if (!isLegacyMetadataKey(key, LEGACY_SCALAR_NBT_METADATA_PREFIX)) {
                continue;
            }
            String tagName = key.substring(LEGACY_SCALAR_NBT_METADATA_PREFIX.length());
            if (!isKnownStackTagKey(tagName)
                    && !metadata.containsKey(LEGACY_NBT_METADATA_PREFIX + tagName)
                    && entry.getValue() != null) {
                return true;
            }
        }
        return false;
    }

    private static boolean isLegacyMetadataKey(String key, String prefix) {
        if (key == null || !key.startsWith(prefix) || key.length() == prefix.length()) {
            return false;
        }
        return !key.substring(prefix.length()).isBlank();
    }

    private static String encodeLegacyTag(Object value) {
        int type = legacyTagType(value);
        if (type == TAG_END) {
            return null;
        }
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(bytes);
            out.writeByte(type);
            writeLegacyPayload(out, type, value);
            out.flush();
            return Base64.getEncoder().encodeToString(bytes.toByteArray());
        } catch (IOException | RuntimeException ignored) {
            return null;
        }
    }

    private static LegacyTag decodeLegacyTag(String encoded) {
        if (encoded == null || encoded.isBlank()) {
            return null;
        }
        try {
            byte[] bytes = Base64.getDecoder().decode(encoded);
            DataInputStream in = new DataInputStream(new ByteArrayInputStream(bytes));
            int type = in.readUnsignedByte();
            if (!isSupportedLegacyTagType(type)) {
                return null;
            }
            return new LegacyTag(type, readPayload(in, type));
        } catch (IOException | IllegalArgumentException ignored) {
            return null;
        }
    }

    private static boolean isSupportedLegacyTagType(int type) {
        return type >= TAG_BYTE && type <= TAG_INT_ARRAY;
    }

    private static int legacyTagType(Object value) {
        if (value instanceof Byte) {
            return TAG_BYTE;
        }
        if (value instanceof Short) {
            return TAG_SHORT;
        }
        if (value instanceof Integer) {
            return TAG_INT;
        }
        if (value instanceof Long) {
            return TAG_LONG;
        }
        if (value instanceof Float) {
            return TAG_FLOAT;
        }
        if (value instanceof Double) {
            return TAG_DOUBLE;
        }
        if (value instanceof byte[]) {
            return TAG_BYTE_ARRAY;
        }
        if (value instanceof String) {
            return TAG_STRING;
        }
        if (value instanceof List<?> || value instanceof Object[]) {
            return TAG_LIST;
        }
        if (value instanceof Map<?, ?>) {
            return TAG_COMPOUND;
        }
        if (value instanceof int[]) {
            return TAG_INT_ARRAY;
        }
        return TAG_END;
    }

    private static void writeLegacyPayload(DataOutputStream out, int type, Object value) throws IOException {
        switch (type) {
            case TAG_BYTE -> out.writeByte(((Number) value).intValue());
            case TAG_SHORT -> out.writeShort(((Number) value).intValue());
            case TAG_INT -> out.writeInt(((Number) value).intValue());
            case TAG_LONG -> out.writeLong(((Number) value).longValue());
            case TAG_FLOAT -> out.writeFloat(((Number) value).floatValue());
            case TAG_DOUBLE -> out.writeDouble(((Number) value).doubleValue());
            case TAG_BYTE_ARRAY -> {
                byte[] values = (byte[]) value;
                out.writeInt(values.length);
                out.write(values);
            }
            case TAG_STRING -> writeString(out, (String) value);
            case TAG_LIST -> writeLegacyListPayload(out, value);
            case TAG_COMPOUND -> writeLegacyCompoundPayload(out, compound(value));
            case TAG_INT_ARRAY -> {
                int[] values = (int[]) value;
                out.writeInt(values.length);
                for (int item : values) {
                    out.writeInt(item);
                }
            }
            default -> throw new IOException("Unsupported legacy NBT payload type: " + type);
        }
    }

    private static void writeLegacyListPayload(DataOutputStream out, Object value) throws IOException {
        Object[] values = legacyListValues(value);
        int elementType = legacyListElementType(values);
        out.writeByte(elementType);
        out.writeInt(elementType == TAG_END ? 0 : values.length);
        if (elementType == TAG_END) {
            return;
        }
        for (Object item : values) {
            writeLegacyPayload(out, elementType, item);
        }
    }

    private static Object[] legacyListValues(Object value) {
        if (value instanceof Object[] values) {
            return values;
        }
        if (value instanceof List<?> list) {
            return list.toArray();
        }
        return new Object[0];
    }

    private static int legacyListElementType(Object[] values) {
        int elementType = TAG_END;
        for (Object item : values) {
            int itemType = legacyTagType(item);
            if (itemType == TAG_END) {
                return TAG_END;
            }
            if (elementType == TAG_END) {
                elementType = itemType;
            } else if (elementType != itemType) {
                return TAG_END;
            }
        }
        return elementType;
    }

    private static void writeLegacyCompoundPayload(DataOutputStream out, Map<String, Object> values)
            throws IOException {
        for (Map.Entry<String, Object> entry : new TreeMap<>(values).entrySet()) {
            if (entry.getKey() == null || entry.getKey().isBlank()) {
                continue;
            }
            int type = legacyTagType(entry.getValue());
            if (type == TAG_END) {
                continue;
            }
            out.writeByte(type);
            writeString(out, entry.getKey());
            writeLegacyPayload(out, type, entry.getValue());
        }
        out.writeByte(TAG_END);
    }

    private static void writeActiveEffects(DataOutputStream out, List<StatusEffectInstance> effects)
            throws IOException {
        ArrayList<CompoundWriter> writers = new ArrayList<>();
        if (effects != null) {
            Set<StatusEffectType> seen = new HashSet<>();
            for (StatusEffectInstance effect : effects) {
                if (effect == null || effect.type() == null || effect.durationTicks() <= 0
                        || !seen.add(effect.type())) {
                    continue;
                }
                writers.add(effectOut -> {
                    writeByte(effectOut, "Id", effect.type().ordinal() + 1);
                    writeByte(effectOut, "Amplifier", Math.max(0, effect.amplifier()));
                    writeInt(effectOut, "Duration", effect.durationTicks());
                    writeByte(effectOut, "Ambient", false);
                });
            }
        }
        writeCompoundList(out, "ActiveEffects", writers);
    }

    private static Map<String, Object> readRoot(Path path) throws IOException {
        try (DataInputStream in = new DataInputStream(new GZIPInputStream(Files.newInputStream(path)))) {
            int type = in.readUnsignedByte();
            if (type != TAG_COMPOUND) {
                throw new IOException("level.dat root is not a compound");
            }
            readString(in);
            return readCompound(in);
        } catch (EOFException exception) {
            throw new IOException("level.dat ended unexpectedly", exception);
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
                byte[] data = new byte[readNbtLength(in, "byte array")];
                in.readFully(data);
                yield data;
            }
            case TAG_STRING -> readString(in);
            case TAG_LIST -> readList(in);
            case TAG_COMPOUND -> readCompound(in);
            case TAG_INT_ARRAY -> {
                int[] data = new int[readNbtLength(in, "int array")];
                for (int i = 0; i < data.length; i++) {
                    data[i] = in.readInt();
                }
                yield data;
            }
            default -> throw new IOException("Unsupported NBT tag type: " + type);
        };
    }

    private static List<Object> readList(DataInputStream in) throws IOException {
        int elementType = in.readUnsignedByte();
        int length = readNbtLength(in, "list");
        List<Object> values = new ArrayList<>(length);
        for (int i = 0; i < length; i++) {
            values.add(readPayload(in, elementType));
        }
        return values;
    }

    private static int readNbtLength(DataInputStream in, String label) throws IOException {
        int length = in.readInt();
        if (length < 0 || length > MAX_NBT_ARRAY_LENGTH) {
            throw new IOException("invalid level.dat NBT " + label + " length: " + length);
        }
        return length;
    }

    private static List<StatusEffectInstance> activeEffectsFrom(Object value) {
        ArrayList<StatusEffectInstance> effects = new ArrayList<>();
        Set<StatusEffectType> seen = new HashSet<>();
        for (Map<String, Object> effect : compoundList(value)) {
            StatusEffectType type = statusEffectByReleaseId(byteValue(effect, "Id", 0));
            int duration = Math.max(0, intValue(effect, "Duration", 0));
            if (type == null || duration <= 0 || !seen.add(type)) {
                continue;
            }
            effects.add(new StatusEffectInstance(type, duration,
                    Math.max(0, byteValue(effect, "Amplifier", 0))));
        }
        return effects;
    }

    private static StatusEffectType statusEffectByReleaseId(int id) {
        StatusEffectType[] values = StatusEffectType.values();
        int index = id - 1;
        return index >= 0 && index < values.length ? values[index] : null;
    }

    private static int releaseItemDamage(ItemType type, SaveManager.StackData stack) {
        if (type == ItemType.POTION) {
            return releasePotionDamage(stack.potion, stack.dataValue);
        }
        if (type == ItemType.MAP) {
            return releaseMapDamage(stack);
        }
        if (type != null && type.isDamageable() && stack.durability > 0) {
            return Math.max(0, type.getMaxDurability() - stack.durability);
        }
        return Math.max(0, stack.dataValue);
    }

    private static int releaseMapDamage(SaveManager.StackData stack) {
        if (stack.durability >= 0) {
            return stack.durability;
        }
        if (stack.metadata != null) {
            String raw = stack.metadata.get("map.id");
            if (raw != null) {
                String normalized = raw.startsWith(MAP_ID_PREFIX) ? raw.substring(MAP_ID_PREFIX.length()) : raw;
                try {
                    return Math.max(0, Integer.parseInt(normalized.trim()));
                } catch (NumberFormatException ignored) {
                    return Math.max(0, stack.dataValue);
                }
            }
        }
        return Math.max(0, stack.dataValue);
    }

    private static PotionData potionDataFromReleaseDamage(int damage) {
        int value = damage & 0xFFFF;
        boolean splash = (value & RELEASE_POTION_SPLASH_BIT) != 0;
        value &= ~RELEASE_POTION_SPLASH_BIT;
        boolean enhanced = (value & RELEASE_POTION_ENHANCED_BIT) != 0;
        boolean extended = (value & RELEASE_POTION_EXTENDED_BIT) != 0;
        int base = value & ~(RELEASE_POTION_ENHANCED_BIT | RELEASE_POTION_EXTENDED_BIT);
        PotionType type = switch (base) {
            case 0 -> PotionType.WATER;
            case 16 -> PotionType.AWKWARD;
            case 32 -> PotionType.THICK;
            case 64, RELEASE_POTION_EFFECT_BIT -> PotionType.MUNDANE;
            case RELEASE_POTION_EFFECT_BIT | 1 -> PotionType.REGENERATION;
            case RELEASE_POTION_EFFECT_BIT | 2 -> PotionType.SWIFTNESS;
            case RELEASE_POTION_EFFECT_BIT | 3 -> PotionType.FIRE_RESISTANCE;
            case RELEASE_POTION_EFFECT_BIT | 4 -> PotionType.POISON;
            case RELEASE_POTION_EFFECT_BIT | 5 -> PotionType.HEALING;
            case RELEASE_POTION_EFFECT_BIT | 8 -> PotionType.WEAKNESS;
            case RELEASE_POTION_EFFECT_BIT | 9 -> PotionType.STRENGTH;
            case RELEASE_POTION_EFFECT_BIT | 10 -> PotionType.SLOWNESS;
            case RELEASE_POTION_EFFECT_BIT | 12 -> PotionType.HARMING;
            default -> PotionType.WATER;
        };
        if (type == PotionType.WATER || type == PotionType.AWKWARD || type == PotionType.THICK) {
            extended = false;
            enhanced = false;
        } else if (type == PotionType.MUNDANE) {
            enhanced = false;
            extended = extended || base == 64;
        } else {
            if (!canExtendPotion(type)) {
                extended = false;
            }
            if (!canEnhancePotion(type)) {
                enhanced = false;
            }
            if (extended && enhanced) {
                extended = false;
            }
        }
        return new PotionData(type, splash, extended, enhanced);
    }

    private static int releasePotionDamage(PotionData potion, int fallbackDamage) {
        if (potion == null) {
            return Math.max(0, fallbackDamage);
        }
        int value = switch (potion.type()) {
            case WATER -> 0;
            case AWKWARD -> 16;
            case THICK -> 32;
            case MUNDANE -> potion.extended() ? 64 : RELEASE_POTION_EFFECT_BIT;
            case REGENERATION -> RELEASE_POTION_EFFECT_BIT | 1;
            case SWIFTNESS -> RELEASE_POTION_EFFECT_BIT | 2;
            case FIRE_RESISTANCE -> RELEASE_POTION_EFFECT_BIT | 3;
            case POISON -> RELEASE_POTION_EFFECT_BIT | 4;
            case HEALING -> RELEASE_POTION_EFFECT_BIT | 5;
            case WEAKNESS -> RELEASE_POTION_EFFECT_BIT | 8;
            case STRENGTH -> RELEASE_POTION_EFFECT_BIT | 9;
            case SLOWNESS -> RELEASE_POTION_EFFECT_BIT | 10;
            case HARMING -> RELEASE_POTION_EFFECT_BIT | 12;
        };
        if (potion.enhanced() && canEnhancePotion(potion.type())) {
            value |= RELEASE_POTION_ENHANCED_BIT;
        } else if (potion.extended() && canExtendPotion(potion.type())) {
            value |= RELEASE_POTION_EXTENDED_BIT;
        }
        if (potion.splash()) {
            value |= RELEASE_POTION_SPLASH_BIT;
        }
        return value;
    }

    private static boolean canExtendPotion(PotionType type) {
        return switch (type) {
            case REGENERATION, SWIFTNESS, FIRE_RESISTANCE, POISON, WEAKNESS, STRENGTH, SLOWNESS -> true;
            default -> false;
        };
    }

    private static boolean canEnhancePotion(PotionType type) {
        return switch (type) {
            case REGENERATION, SWIFTNESS, POISON, HEALING, STRENGTH, HARMING -> true;
            default -> false;
        };
    }

    private static SaveManager.PlayerData defaultPlayer(SaveManager.LevelData level) {
        SaveManager.PlayerData player = new SaveManager.PlayerData();
        player.x = level.spawnX + 0.5f;
        player.y = level.spawnY;
        player.z = level.spawnZ + 0.5f;
        player.health = PlayerStats.MAX_HEALTH;
        player.hunger = PlayerStats.MAX_HUNGER;
        player.saturation = 5.0f;
        player.air = PlayerStats.MAX_AIR_SECONDS;
        player.spawnX = level.spawnX + 0.5f;
        player.spawnY = level.spawnY;
        player.spawnZ = level.spawnZ + 0.5f;
        player.achievements = new ArrayList<>();
        player.activeEffects = new ArrayList<>();
        return player;
    }

    private static SaveManager.InventoryData defaultInventory() {
        SaveManager.InventoryData inventory = new SaveManager.InventoryData();
        inventory.hotbar = new SaveManager.StackData[Inventory.HOTBAR_SIZE];
        inventory.main = new SaveManager.StackData[Inventory.MAIN_SIZE];
        inventory.crafting = new SaveManager.StackData[Inventory.CRAFTING_SIZE];
        inventory.armor = new SaveManager.StackData[4];
        return inventory;
    }

    private static void writeNamedCompound(DataOutputStream out, String name) throws IOException {
        out.writeByte(TAG_COMPOUND);
        writeString(out, name);
    }

    private static void writeByte(DataOutputStream out, String name, boolean value) throws IOException {
        out.writeByte(TAG_BYTE);
        writeString(out, name);
        out.writeByte(value ? 1 : 0);
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

    private static void writeLong(DataOutputStream out, String name, long value) throws IOException {
        out.writeByte(TAG_LONG);
        writeString(out, name);
        out.writeLong(value);
    }

    private static void writeFloat(DataOutputStream out, String name, float value) throws IOException {
        out.writeByte(TAG_FLOAT);
        writeString(out, name);
        out.writeFloat(Float.isFinite(value) ? value : 0.0f);
    }

    private static void writeDoubleList(DataOutputStream out, String name, double... values) throws IOException {
        out.writeByte(TAG_LIST);
        writeString(out, name);
        out.writeByte(TAG_DOUBLE);
        out.writeInt(values.length);
        for (double value : values) {
            out.writeDouble(Double.isFinite(value) ? value : 0.0d);
        }
    }

    private static void writeFloatList(DataOutputStream out, String name, float... values) throws IOException {
        out.writeByte(TAG_LIST);
        writeString(out, name);
        out.writeByte(TAG_FLOAT);
        out.writeInt(values.length);
        for (float value : values) {
            out.writeFloat(Float.isFinite(value) ? value : 0.0f);
        }
    }

    private static void writeCompoundList(DataOutputStream out, String name, List<CompoundWriter> writers)
            throws IOException {
        out.writeByte(TAG_LIST);
        writeString(out, name);
        out.writeByte(TAG_COMPOUND);
        out.writeInt(writers.size());
        for (CompoundWriter writer : writers) {
            writer.write(out);
            out.writeByte(TAG_END);
        }
    }

    private static void writeString(DataOutputStream out, String name, String value) throws IOException {
        out.writeByte(TAG_STRING);
        writeString(out, name);
        writeString(out, value);
    }

    private static void writeString(DataOutputStream out, String value) throws IOException {
        byte[] bytes = safeText(value, "").getBytes(StandardCharsets.UTF_8);
        if (bytes.length > MAX_NBT_STRING_BYTES) {
            throw new IOException("NBT string too long");
        }
        out.writeShort(bytes.length);
        out.write(bytes);
    }

    private static String readString(DataInputStream in) throws IOException {
        int length = in.readUnsignedShort();
        byte[] bytes = new byte[length];
        in.readFully(bytes);
        return new String(bytes, StandardCharsets.UTF_8);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> compound(Object value) {
        return value instanceof Map<?, ?> map ? (Map<String, Object>) map : Map.of();
    }

    private static Map<String, Object> playerCompound(Map<String, Object> root) {
        Map<String, Object> player = compound(root.get("Player"));
        if (!player.isEmpty()) {
            return player;
        }
        Map<String, Object> data = compound(root.get("Data"));
        player = compound(data.get("Player"));
        if (!player.isEmpty()) {
            return player;
        }
        return looksLikePlayerCompound(root) ? root : Map.of();
    }

    private static boolean looksLikePlayerCompound(Map<String, Object> root) {
        return root.containsKey("Pos")
                || root.containsKey("Inventory")
                || root.containsKey("Health")
                || root.containsKey("foodLevel");
    }

    private static List<Map<String, Object>> compoundList(Object value) {
        List<Map<String, Object>> compounds = new ArrayList<>();
        if (value instanceof List<?> values) {
            for (Object item : values) {
                Map<String, Object> compound = compound(item);
                if (!compound.isEmpty()) {
                    compounds.add(compound);
                }
            }
        } else if (value instanceof Object[] values) {
            for (Object item : values) {
                Map<String, Object> compound = compound(item);
                if (!compound.isEmpty()) {
                    compounds.add(compound);
                }
            }
        }
        return compounds;
    }

    private static double listDouble(Object value, int index, double fallback) {
        Object item = listItem(value, index);
        if (!(item instanceof Number number)) {
            return fallback;
        }
        double parsed = number.doubleValue();
        return Double.isFinite(parsed) ? parsed : fallback;
    }

    private static float listFloat(Object value, int index, float fallback) {
        Object item = listItem(value, index);
        if (!(item instanceof Number number)) {
            return fallback;
        }
        float parsed = number.floatValue();
        return Float.isFinite(parsed) ? parsed : fallback;
    }

    private static Object listItem(Object value, int index) {
        if (index < 0) {
            return null;
        }
        if (value instanceof List<?> list) {
            return index < list.size() ? list.get(index) : null;
        }
        if (value instanceof Object[] array) {
            return index < array.length ? array[index] : null;
        }
        return null;
    }

    private static String string(Map<String, Object> data, String key, String fallback) {
        Object value = data.get(key);
        return value instanceof String text && !text.isBlank() ? text : fallback;
    }

    private static int intValue(Map<String, Object> data, String key, int fallback) {
        Object value = data.get(key);
        return value instanceof Number number ? number.intValue() : fallback;
    }

    private static int byteValue(Map<String, Object> data, String key, int fallback) {
        Object value = data.get(key);
        return value instanceof Number number ? number.byteValue() & 0xFF : fallback;
    }

    private static int shortValue(Map<String, Object> data, String key, int fallback) {
        Object value = data.get(key);
        return value instanceof Number number ? number.shortValue() : fallback;
    }

    private static int unsignedShortValue(Map<String, Object> data, String key, int fallback) {
        Object value = data.get(key);
        return value instanceof Number number ? number.shortValue() & 0xFFFF : fallback;
    }

    private static long longValue(Map<String, Object> data, String key, long fallback) {
        Object value = data.get(key);
        return value instanceof Number number ? number.longValue() : fallback;
    }

    private static float floatValue(Map<String, Object> data, String key, float fallback) {
        Object value = data.get(key);
        if (!(value instanceof Number number)) {
            return fallback;
        }
        float parsed = number.floatValue();
        return Float.isFinite(parsed) ? parsed : fallback;
    }

    private static boolean byteBoolean(Map<String, Object> data, String key, boolean fallback) {
        Object value = data.get(key);
        return value instanceof Number number ? number.byteValue() != 0 : fallback;
    }

    private static String stringValue(Map<String, Object> data, String key, String fallback) {
        Object value = data.get(key);
        return value instanceof String string ? string : fallback;
    }

    private static boolean hasNumber(Map<String, Object> data, String key) {
        return data.get(key) instanceof Number;
    }

    private static String safeText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private static int positiveOrDefault(Integer value, int fallback) {
        return value != null && value > 0 ? value : fallback;
    }

    private static float clamp(float value, float min, float max) {
        if (!Float.isFinite(value)) {
            return min;
        }
        return Math.max(min, Math.min(max, value));
    }

    private static int clampInt(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static long releaseTime(SaveManager.LevelData level) {
        if (level.worldTime != null) {
            return Math.max(0L, level.worldTime);
        }
        return Math.max(0L, (long) Math.floor(level.time));
    }

    private static int releaseTotalExperience(Map<String, Object> data) {
        int total = intValue(data, "XpTotal", -1);
        if (total > 0 || (total == 0 && (!hasNumber(data, "XpLevel") || intValue(data, "XpLevel", 0) <= 0))) {
            return Math.max(0, total);
        }
        int level = Math.max(0, intValue(data, "XpLevel", 0));
        float progress = clamp(floatValue(data, "XpP", 0.0f), 0.0f, 0.9999f);
        int base = PlayerProgression.experienceForLevel(level);
        int span = Math.max(1, PlayerProgression.experienceForLevel(level + 1) - base);
        return base + Math.max(0, Math.min(span - 1, (int) Math.floor(progress * span)));
    }

    private static int releaseExperienceLevel(int totalExperience) {
        int level = 0;
        int total = Math.max(0, totalExperience);
        while (PlayerProgression.experienceForLevel(level + 1) <= total) {
            level++;
        }
        return level;
    }

    private static float releaseExperienceProgress(int totalExperience) {
        int total = Math.max(0, totalExperience);
        int level = releaseExperienceLevel(total);
        int base = PlayerProgression.experienceForLevel(level);
        int span = Math.max(1, PlayerProgression.experienceForLevel(level + 1) - base);
        return clamp((total - base) / (float) span, 0.0f, 0.9999f);
    }

    private static Dimension dimensionFromId(int id) {
        for (Dimension dimension : Dimension.values()) {
            if (dimension.getId() == id) {
                return dimension;
            }
        }
        return Dimension.OVERWORLD;
    }

    @FunctionalInterface
    private interface CompoundWriter {
        void write(DataOutputStream out) throws IOException;
    }

    private record LegacyTag(int type, Object value) {
    }

    record PlayerSaveData(SaveManager.PlayerData player, SaveManager.InventoryData inventory) {
    }
}
