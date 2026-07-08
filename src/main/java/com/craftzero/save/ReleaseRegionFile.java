package com.craftzero.save;

import com.craftzero.entity.DroppedItem;
import com.craftzero.entity.BoatEntity;
import com.craftzero.entity.ExperienceOrbEntity;
import com.craftzero.entity.FurnaceMinecartEntity;
import com.craftzero.entity.LivingEntity;
import com.craftzero.entity.MinecartEntity;
import com.craftzero.entity.mob.MobDefinition;
import com.craftzero.inventory.ItemType;
import com.craftzero.inventory.ToolType;
import com.craftzero.main.CombatRules;
import com.craftzero.progression.ArmorMaterial;
import com.craftzero.progression.ArmorSlot;
import com.craftzero.progression.EnchantmentInstance;
import com.craftzero.progression.EnchantmentResolver;
import com.craftzero.progression.EnchantmentType;
import com.craftzero.progression.PotionData;
import com.craftzero.progression.PotionType;
import com.craftzero.world.Block;
import com.craftzero.world.Chunk;
import com.craftzero.world.BlockType;
import com.craftzero.world.DayCycleManager;
import com.craftzero.world.Dimension;
import com.craftzero.world.RedstoneEngine;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.InflaterInputStream;

final class ReleaseRegionFile {
    private static final int SECTOR_BYTES = 4096;
    private static final int HEADER_BYTES = SECTOR_BYTES * 2;
    private static final int CHUNKS_PER_REGION_AXIS = 32;
    private static final int CHUNKS_PER_REGION = CHUNKS_PER_REGION_AXIS * CHUNKS_PER_REGION_AXIS;
    private static final int COMPRESSION_GZIP = 1;
    private static final int COMPRESSION_ZLIB = 2;
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
    private static final int RELEASE_CHUNK_NIBBLE_BYTES = Chunk.TOTAL_BLOCKS / 2;
    private static final int MAX_NBT_ARRAY_LENGTH = 2_000_000;
    private static final int MAX_NBT_STRING_BYTES = 65_535;
    private static final int RELEASE_POTION_ENHANCED_BIT = 0x20;
    private static final int RELEASE_POTION_EXTENDED_BIT = 0x40;
    private static final int RELEASE_POTION_EFFECT_BIT = 0x2000;
    private static final int RELEASE_POTION_SPLASH_BIT = 0x4000;
    private static final String MAP_ID_PREFIX = "map_";
    private static final String LEGACY_NBT_METADATA_PREFIX = "legacyNbt.";
    private static final String LEGACY_DISPLAY_METADATA_PREFIX = "legacyDisplay.";
    private static final String LEGACY_SCALAR_NBT_METADATA_PREFIX = "nbt.";
    private static final String CZ_ITEM_ROTATION = "CraftZeroItemRotation";
    private static final String CZ_ITEM_BOB_PHASE = "CraftZeroItemBobPhase";
    private static final String CZ_ITEM_PICKUP_DELAY_ACCUMULATOR = "CraftZeroItemPickupDelayAccumulator";
    private static final String CZ_ENTITY_SAVE_ID = "CraftZeroEntitySaveId";
    private static final String CZ_ENTITY_TICKS_EXISTED = "CraftZeroTicksExisted";
    private static final String CZ_PROJECTILE_SHOOTER_SAVE_ID = "CraftZeroProjectileShooterSaveId";
    private static final String CZ_MOB_TARGET_SAVE_ID = "CraftZeroMobTargetSaveId";
    private static final String CZ_FISHING_HOOKED_ENTITY_SAVE_ID = "CraftZeroFishingHookedEntitySaveId";
    private static final String CZ_MINECART_PASSENGER_SAVE_ID = "CraftZeroMinecartPassengerSaveId";
    private static final String CZ_SPIDER_JOCKEY_RIDER_SAVE_ID = "CraftZeroSpiderJockeyRiderSaveId";
    private static final String CZ_ARROW_KNOCKBACK_HORIZONTAL = "CraftZeroArrowKnockbackHorizontal";
    private static final String CZ_ARROW_KNOCKBACK_VERTICAL = "CraftZeroArrowKnockbackVertical";
    private static final String CZ_ARROW_FIRE_TICKS_ON_HIT = "CraftZeroArrowFireTicksOnHit";
    private static final String CZ_ARROW_CRITICAL = "CraftZeroArrowCritical";
    private static final String CZ_PROJECTILE_PLAYER_OWNED = "CraftZeroProjectilePlayerOwned";
    private static final String CZ_FIREBALL_DEFLECTED_BY_PLAYER = "CraftZeroFireballDeflectedByPlayer";
    private static final String CZ_FISHING_WAIT_TICKS = "CraftZeroFishingWaitTicks";
    private static final String CZ_FISHING_CATCHABLE_TICKS = "CraftZeroFishingCatchableTicks";
    private static final String CZ_FISHING_STUCK_IN_GROUND = "CraftZeroFishingStuckInGround";
    private static final String CZ_EYE_TARGET_X = "CraftZeroEyeTargetX";
    private static final String CZ_EYE_TARGET_Y = "CraftZeroEyeTargetY";
    private static final String CZ_EYE_TARGET_Z = "CraftZeroEyeTargetZ";
    private static final int RELEASE_FISHING_MAX_WAIT_TICKS = 699;
    private static final int RELEASE_FISHING_MAX_CATCHABLE_TICKS = 39;

    private ReleaseRegionFile() {
    }

    static ChunkCodec.ChunkData readChunk(Path worldDir, Dimension dimension, int chunkX, int chunkZ)
            throws IOException {
        Path path = regionPath(worldDir, dimension, chunkX, chunkZ);
        if (!Files.isRegularFile(path)) {
            return null;
        }
        byte[] payload = readRegionPayloads(path).get(localChunkIndex(chunkX, chunkZ));
        if (payload == null) {
            return null;
        }
        return decodeChunkPayload(payload, chunkX, chunkZ);
    }

    static void writeChunk(Path worldDir, Dimension dimension, int chunkX, int chunkZ, ChunkCodec.ChunkData data)
            throws IOException {
        writeChunk(worldDir, dimension, chunkX, chunkZ, data, null);
    }

    static void writeChunk(Path worldDir, Dimension dimension, int chunkX, int chunkZ, ChunkCodec.ChunkData data,
            SaveManager.DimensionRuntimeData runtimeData) throws IOException {
        if (data == null) {
            return;
        }
        Path path = regionPath(worldDir, dimension, chunkX, chunkZ);
        Map<Integer, byte[]> payloads = readRegionPayloads(path);
        payloads.put(localChunkIndex(chunkX, chunkZ), encodeChunkPayload(chunkX, chunkZ, data, runtimeData));
        writeRegionPayloads(path, payloads);
    }

    static SaveManager.DimensionRuntimeData readRuntime(Path worldDir, Dimension dimension) throws IOException {
        Path regionDirectory = releaseDimensionRoot(worldDir, dimension).resolve("region");
        if (!Files.isDirectory(regionDirectory)) {
            return null;
        }
        SaveManager.DimensionRuntimeData runtime = new SaveManager.DimensionRuntimeData();
        Dimension normalized = dimension == null ? Dimension.OVERWORLD : dimension;
        runtime.dimension = normalized.getSaveName();
        runtime.droppedItems = new ArrayList<>();
        runtime.tileEntities = new ArrayList<>();
        runtime.entities = new ArrayList<>();
        runtime.movingPistons = new ArrayList<>();
        runtime.scheduledBlockTicks = new ArrayList<>();

        try (var paths = Files.list(regionDirectory)) {
            for (Path path : paths.filter(ReleaseRegionFile::isReleaseRegionFile).toList()) {
                for (byte[] payload : readRegionPayloads(path).values()) {
                    importChunkRuntime(readChunkLevel(payload), runtime);
                }
            }
        }
        return hasImportedRuntime(runtime) ? runtime : null;
    }

    private static boolean hasImportedRuntime(SaveManager.DimensionRuntimeData runtime) {
        if (runtime == null) {
            return false;
        }
        return runtime.time != null
                || runtime.worldTime != null
                || runtime.dayCount != null
                || runtime.moonPhase != null
                || runtime.weatherState != null
                || runtime.weatherRainTime != null
                || runtime.weatherThunderTime != null
                || !runtime.droppedItems.isEmpty()
                || !runtime.tileEntities.isEmpty()
                || !runtime.entities.isEmpty()
                || !runtime.movingPistons.isEmpty()
                || !runtime.scheduledBlockTicks.isEmpty();
    }

    static Path regionPath(Path worldDir, Dimension dimension, int chunkX, int chunkZ) {
        int regionX = Math.floorDiv(chunkX, CHUNKS_PER_REGION_AXIS);
        int regionZ = Math.floorDiv(chunkZ, CHUNKS_PER_REGION_AXIS);
        return releaseDimensionRoot(worldDir, dimension)
                .resolve("region")
                .resolve("r." + regionX + "." + regionZ + ".mcr");
    }

    private static boolean isReleaseRegionFile(Path path) {
        String name = path == null || path.getFileName() == null ? "" : path.getFileName().toString();
        return name.startsWith("r.") && name.endsWith(".mcr") && Files.isRegularFile(path);
    }

    private static Path releaseDimensionRoot(Path worldDir, Dimension dimension) {
        Dimension normalized = dimension == null ? Dimension.OVERWORLD : dimension;
        if (normalized == Dimension.NETHER) {
            return worldDir.resolve("DIM-1");
        }
        if (normalized == Dimension.THE_END) {
            return worldDir.resolve("DIM1");
        }
        return worldDir;
    }

    private static int localChunkIndex(int chunkX, int chunkZ) {
        int localX = Math.floorMod(chunkX, CHUNKS_PER_REGION_AXIS);
        int localZ = Math.floorMod(chunkZ, CHUNKS_PER_REGION_AXIS);
        return localX + localZ * CHUNKS_PER_REGION_AXIS;
    }

    private static Map<Integer, byte[]> readRegionPayloads(Path path) throws IOException {
        if (!Files.isRegularFile(path)) {
            return new TreeMap<>();
        }
        byte[] file = Files.readAllBytes(path);
        if (file.length < HEADER_BYTES) {
            throw new IOException("Release region header is incomplete: " + path);
        }
        TreeMap<Integer, byte[]> payloads = new TreeMap<>();
        for (int index = 0; index < CHUNKS_PER_REGION; index++) {
            int location = readInt(file, index * Integer.BYTES);
            int offset = location >>> 8;
            int sectors = location & 0xFF;
            if (offset == 0 || sectors == 0) {
                continue;
            }
            int byteOffset = offset * SECTOR_BYTES;
            int maxLength = sectors * SECTOR_BYTES - Integer.BYTES;
            if (byteOffset < HEADER_BYTES || byteOffset + Integer.BYTES > file.length || maxLength <= 0) {
                continue;
            }
            int length = readInt(file, byteOffset);
            if (length <= 1 || length > maxLength || byteOffset + Integer.BYTES + length > file.length) {
                continue;
            }
            payloads.put(index, Arrays.copyOfRange(file, byteOffset + Integer.BYTES,
                    byteOffset + Integer.BYTES + length));
        }
        return payloads;
    }

    private static void writeRegionPayloads(Path path, Map<Integer, byte[]> payloads) throws IOException {
        TreeMap<Integer, byte[]> ordered = new TreeMap<>(payloads);
        byte[] header = new byte[HEADER_BYTES];
        ByteArrayOutputStream body = new ByteArrayOutputStream();
        body.write(new byte[HEADER_BYTES]);

        int nextSector = 2;
        int timestamp = (int) Math.min(Integer.MAX_VALUE, System.currentTimeMillis() / 1000L);
        for (Map.Entry<Integer, byte[]> entry : ordered.entrySet()) {
            int index = entry.getKey();
            byte[] payload = entry.getValue();
            if (index < 0 || index >= CHUNKS_PER_REGION || payload == null || payload.length == 0) {
                continue;
            }
            int recordLength = Integer.BYTES + payload.length;
            int sectors = Math.max(1, divideCeil(recordLength, SECTOR_BYTES));
            if (sectors > 0xFF || nextSector > 0xFFFFFF) {
                throw new IOException("Release region file grew beyond legacy location table limits");
            }
            writeInt(header, index * Integer.BYTES, (nextSector << 8) | sectors);
            writeInt(header, SECTOR_BYTES + index * Integer.BYTES, timestamp);
            DataOutputStream out = new DataOutputStream(body);
            out.writeInt(payload.length);
            out.write(payload);
            int padding = sectors * SECTOR_BYTES - recordLength;
            if (padding > 0) {
                out.write(new byte[padding]);
            }
            nextSector += sectors;
        }

        byte[] region = body.toByteArray();
        System.arraycopy(header, 0, region, 0, header.length);
        SafeFiles.writeAtomicBytes(path, stream -> stream.write(region), SafeFiles.BackupPolicy.BAK);
    }

    private static int divideCeil(int value, int divisor) {
        return (value + divisor - 1) / divisor;
    }

    private static byte[] encodeChunkPayload(int chunkX, int chunkZ, ChunkCodec.ChunkData data,
            SaveManager.DimensionRuntimeData runtimeData) throws IOException {
        ByteArrayOutputStream payload = new ByteArrayOutputStream();
        payload.write(COMPRESSION_ZLIB);
        try (DeflaterOutputStream zlib = new DeflaterOutputStream(payload)) {
            zlib.write(writeChunkNbt(chunkX, chunkZ, data, runtimeData));
        }
        return payload.toByteArray();
    }

    private static ChunkCodec.ChunkData decodeChunkPayload(byte[] payload, int expectedChunkX, int expectedChunkZ)
            throws IOException {
        if (payload.length < 2) {
            throw new IOException("Release chunk payload is empty");
        }
        InputStream compressed = new ByteArrayInputStream(payload, 1, payload.length - 1);
        InputStream stream = switch (payload[0] & 0xFF) {
            case COMPRESSION_GZIP -> new GZIPInputStream(compressed);
            case COMPRESSION_ZLIB -> new InflaterInputStream(compressed);
            default -> throw new IOException("Unsupported Release chunk compression: " + (payload[0] & 0xFF));
        };
        Map<String, Object> root = readRoot(stream);
        Map<String, Object> level = compound(root.get("Level"));
        if (level.isEmpty()) {
            level = root;
        }
        int chunkX = intValue(level, "xPos", expectedChunkX);
        int chunkZ = intValue(level, "zPos", expectedChunkZ);
        if (chunkX != expectedChunkX || chunkZ != expectedChunkZ) {
            throw new IOException("Release chunk coordinate mismatch: " + chunkX + "," + chunkZ);
        }
        byte[] releaseBlocks = byteArray(level.get("Blocks"));
        if (releaseBlocks == null || releaseBlocks.length != Chunk.TOTAL_BLOCKS) {
            throw new IOException("Release chunk Blocks length is invalid");
        }
        byte[] releaseMetadata = nibbleArray(level.get("Data"));
        byte[] releaseSkyLight = nibbleArray(level.get("SkyLight"));
        byte[] releaseBlockLight = nibbleArray(level.get("BlockLight"));
        int[] releaseHeightMap = heightMap(level.get("HeightMap"));

        short[] blocks = new short[Chunk.TOTAL_BLOCKS];
        byte[] metadata = new byte[Chunk.TOTAL_BLOCKS];
        byte[] skyLight = releaseSkyLight == null ? null : new byte[Chunk.LIGHT_DATA_BYTES];
        byte[] blockLight = releaseBlockLight == null ? null : new byte[Chunk.LIGHT_DATA_BYTES];

        for (int x = 0; x < Chunk.WIDTH; x++) {
            for (int z = 0; z < Chunk.DEPTH; z++) {
                for (int y = 0; y < Chunk.HEIGHT; y++) {
                    int releaseIndex = releaseIndex(x, y, z);
                    int craftIndex = Chunk.getIndex(x, y, z);
                    blocks[craftIndex] = (short) (releaseBlocks[releaseIndex] & 0xFF);
                    metadata[craftIndex] = (byte) getNibble(releaseMetadata, releaseIndex);
                    if (skyLight != null) {
                        setNibble(skyLight, craftIndex, getNibble(releaseSkyLight, releaseIndex));
                    }
                    if (blockLight != null) {
                        setNibble(blockLight, craftIndex, getNibble(releaseBlockLight, releaseIndex));
                    }
                }
            }
        }
        return skyLight == null || blockLight == null || releaseHeightMap == null
                ? new ChunkCodec.ChunkData(blocks, metadata)
                : new ChunkCodec.ChunkData(blocks, metadata, skyLight, blockLight, releaseHeightMap);
    }

    private static Map<String, Object> readChunkLevel(byte[] payload) throws IOException {
        if (payload.length < 2) {
            throw new IOException("Release chunk payload is empty");
        }
        InputStream compressed = new ByteArrayInputStream(payload, 1, payload.length - 1);
        InputStream stream = switch (payload[0] & 0xFF) {
            case COMPRESSION_GZIP -> new GZIPInputStream(compressed);
            case COMPRESSION_ZLIB -> new InflaterInputStream(compressed);
            default -> throw new IOException("Unsupported Release chunk compression: " + (payload[0] & 0xFF));
        };
        Map<String, Object> root = readRoot(stream);
        Map<String, Object> level = compound(root.get("Level"));
        return level.isEmpty() ? root : level;
    }

    private static byte[] writeChunkNbt(int chunkX, int chunkZ, ChunkCodec.ChunkData data,
            SaveManager.DimensionRuntimeData runtimeData) throws IOException {
        byte[] releaseBlocks = new byte[Chunk.TOTAL_BLOCKS];
        byte[] releaseMetadata = new byte[RELEASE_CHUNK_NIBBLE_BYTES];
        byte[] releaseSkyLight = new byte[RELEASE_CHUNK_NIBBLE_BYTES];
        byte[] releaseBlockLight = new byte[RELEASE_CHUNK_NIBBLE_BYTES];
        int[] heightMap = data.heightMap() == null ? deriveHeightMap(data.blockIds()) : data.heightMap();

        for (int x = 0; x < Chunk.WIDTH; x++) {
            for (int z = 0; z < Chunk.DEPTH; z++) {
                for (int y = 0; y < Chunk.HEIGHT; y++) {
                    int craftIndex = Chunk.getIndex(x, y, z);
                    int releaseIndex = releaseIndex(x, y, z);
                    releaseBlocks[releaseIndex] = (byte) (data.blockIds()[craftIndex] & 0xFF);
                    setNibble(releaseMetadata, releaseIndex, data.metadata()[craftIndex] & 0x0F);
                    if (data.hasLightingData()) {
                        setNibble(releaseSkyLight, releaseIndex, getNibble(data.skyLight(), craftIndex));
                        setNibble(releaseBlockLight, releaseIndex, getNibble(data.blockLight(), craftIndex));
                    } else {
                        setNibble(releaseSkyLight, releaseIndex, y > heightMap[x + z * Chunk.WIDTH] ? 15 : 0);
                    }
                }
            }
        }

        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(bytes);
        out.writeByte(TAG_COMPOUND);
        writeString(out, "");
        writeNamedCompound(out, "Level");
        writeInt(out, "xPos", chunkX);
        writeInt(out, "zPos", chunkZ);
        writeLong(out, "LastUpdate", releaseChunkLastUpdate(runtimeData));
        writeByte(out, "TerrainPopulated", 1);
        writeByteArray(out, "Blocks", releaseBlocks);
        writeByteArray(out, "Data", releaseMetadata);
        writeByteArray(out, "SkyLight", releaseSkyLight);
        writeByteArray(out, "BlockLight", releaseBlockLight);
        writeByteArray(out, "HeightMap", releaseHeightMap(heightMap));
        writeEntityList(out, "Entities", chunkX, chunkZ, runtimeData);
        writeTileEntityList(out, "TileEntities", chunkX, chunkZ, runtimeData);
        writeTileTickList(out, "TileTicks", chunkX, chunkZ, runtimeData);
        out.writeByte(TAG_END);
        out.writeByte(TAG_END);
        out.flush();
        return bytes.toByteArray();
    }

    private static void importChunkRuntime(Map<String, Object> level, SaveManager.DimensionRuntimeData runtime) {
        if (level == null || runtime == null) {
            return;
        }
        importChunkLastUpdate(level, runtime);
        for (Map<String, Object> tile : compoundList(level.get("TileEntities"))) {
            if (isReleaseMovingPistonTile(tile)) {
                SaveManager.MovingPistonData data = importMovingPiston(tile);
                if (data != null) {
                    runtime.movingPistons.add(data);
                }
            } else {
                SaveManager.TileEntityData data = importTileEntity(tile);
                if (data != null) {
                    runtime.tileEntities.add(data);
                }
            }
        }
        for (Map<String, Object> entity : compoundList(level.get("Entities"))) {
            String id = stringValue(entity, "id", "");
            if ("Item".equals(id)) {
                SaveManager.DroppedItemData item = importDroppedItem(entity);
                if (item != null) {
                    runtime.droppedItems.add(item);
                }
                continue;
            }
            SaveManager.EntityData data = importEntity(entity);
            if (data != null) {
                runtime.entities.add(data);
            }
        }
        for (Map<String, Object> tick : compoundList(level.get("TileTicks"))) {
            SaveManager.ScheduledBlockTickData data = importTileTick(tick);
            if (data != null) {
                runtime.scheduledBlockTicks.add(data);
            }
        }
    }

    private static void importChunkLastUpdate(Map<String, Object> level, SaveManager.DimensionRuntimeData runtime) {
        long lastUpdate = longValue(level, "LastUpdate", -1L);
        if (lastUpdate < 0L || (runtime.worldTime != null && runtime.worldTime >= lastUpdate)) {
            return;
        }
        runtime.worldTime = lastUpdate;
        runtime.time = (float) Math.floorMod(lastUpdate, DayCycleManager.TICKS_PER_DAY);
        runtime.dayCount = (int) Math.min(Integer.MAX_VALUE, lastUpdate / DayCycleManager.TICKS_PER_DAY);
        runtime.moonPhase = Math.floorMod(runtime.dayCount, 8);
    }

    private static long releaseChunkLastUpdate(SaveManager.DimensionRuntimeData runtimeData) {
        return runtimeData != null && runtimeData.worldTime != null && runtimeData.worldTime >= 0L
                ? runtimeData.worldTime
                : 0L;
    }

    private static boolean isReleaseMovingPistonTile(Map<String, Object> tile) {
        String id = stringValue(tile, "id", "");
        return "Piston".equals(id) || "MovingPiston".equals(id);
    }

    private static SaveManager.MovingPistonData importMovingPiston(Map<String, Object> tile) {
        int y = intValue(tile, "y", -1);
        int facing = intValue(tile, "facing", 0) & 7;
        int carriedBlockId = intValue(tile, "blockId", BlockType.PISTON_HEAD.getId());
        int carriedMetadata = intValue(tile, "blockData", 0) & 15;
        BlockType carried = BlockType.fromId(carriedBlockId);
        if (y < 0 || y >= Chunk.HEIGHT || !isReleasePistonFacing(facing) || carried == null) {
            return null;
        }
        boolean extending = byteBoolean(tile, "extending", true);
        int x = intValue(tile, "x", 0);
        int z = intValue(tile, "z", 0);
        int dx = RedstoneEngine.faceToDx(facing);
        int dy = RedstoneEngine.faceToDy(facing);
        int dz = RedstoneEngine.faceToDz(facing);
        SaveManager.MovingPistonData data = new SaveManager.MovingPistonData();
        data.x = x;
        data.y = y;
        data.z = z;
        data.facing = facing;
        data.carriedBlockId = carriedBlockId;
        data.carriedMetadata = carriedMetadata;
        data.finalBlockId = !extending && carried == BlockType.PISTON_HEAD ? BlockType.AIR.getId() : carriedBlockId;
        data.finalMetadata = data.finalBlockId == BlockType.AIR.getId() ? 0 : carriedMetadata;
        if (extending) {
            data.fromX = x - dx;
            data.fromY = y - dy;
            data.fromZ = z - dz;
            data.toX = x;
            data.toY = y;
            data.toZ = z;
        } else if (carried == BlockType.PISTON_HEAD) {
            data.fromX = x;
            data.fromY = y;
            data.fromZ = z;
            data.toX = x - dx;
            data.toY = y - dy;
            data.toZ = z - dz;
        } else {
            data.fromX = x + dx;
            data.fromY = y + dy;
            data.fromZ = z + dz;
            data.toX = x;
            data.toY = y;
            data.toZ = z;
        }
        float progress = Math.max(0.0f, Math.min(1.0f, floatValue(tile, "progress", 0.0f)));
        data.elapsedTicks = Math.max(0, Math.min(RedstoneEngine.PISTON_MOVEMENT_TICKS,
                Math.round(progress * RedstoneEngine.PISTON_MOVEMENT_TICKS)));
        return data;
    }

    private static SaveManager.ScheduledBlockTickData importTileTick(Map<String, Object> tick) {
        int y = intValue(tick, "y", -1);
        int blockId = intValue(tick, "i", 0);
        int delay = intValue(tick, "t", 0);
        if (y < 0 || y >= Chunk.HEIGHT || blockId <= 0 || delay < 0) {
            return null;
        }
        SaveManager.ScheduledBlockTickData data = new SaveManager.ScheduledBlockTickData();
        data.x = intValue(tick, "x", 0);
        data.y = y;
        data.z = intValue(tick, "z", 0);
        data.blockId = blockId;
        data.delayTicks = delay;
        return data;
    }

    private static SaveManager.TileEntityData importTileEntity(Map<String, Object> tile) {
        String id = stringValue(tile, "id", "");
        SaveManager.TileEntityData data = new SaveManager.TileEntityData();
        data.x = intValue(tile, "x", 0);
        data.y = intValue(tile, "y", 0);
        data.z = intValue(tile, "z", 0);
        switch (id) {
            case "Chest" -> {
                data.type = "chest";
                data.inventory = inventoryFromItems(tile.get("Items"), 27);
            }
            case "Furnace" -> {
                data.type = "furnace";
                data.inventory = inventoryFromItems(tile.get("Items"), 3);
                data.burnTime = unsignedShortValue(tile, "BurnTime", 0);
                data.currentFuelBurnTime = Math.max(data.burnTime, 0);
                data.cookTime = unsignedShortValue(tile, "CookTime", 0);
            }
            case "Trap", "Dispenser" -> {
                data.type = "dispenser";
                data.inventory = inventoryFromItems(tile.get("Items"), 9);
            }
            case "Cauldron", "BrewingStand" -> {
                data.type = "brewing_stand";
                data.inventory = inventoryFromItems(tile.get("Items"), 4);
                data.brewTime = unsignedShortValue(tile, "BrewTime", 0);
            }
            case "Sign" -> {
                data.type = "sign";
                data.signText = new String[] {
                        trimSignLine(stringValue(tile, "Text1", "")),
                        trimSignLine(stringValue(tile, "Text2", "")),
                        trimSignLine(stringValue(tile, "Text3", "")),
                        trimSignLine(stringValue(tile, "Text4", ""))
                };
            }
            case "MobSpawner" -> {
                data.type = "mob_spawner";
                data.mobType = mobDefinitionName(stringValue(tile, "EntityId", "Pig"));
                data.spawnDelay = Math.max(0, shortValue(tile, "Delay", 20));
                data.minSpawnDelay = Math.max(0, shortValue(tile, "MinSpawnDelay", 200));
                data.maxSpawnDelay = Math.max(data.minSpawnDelay, shortValue(tile, "MaxSpawnDelay", 800));
                data.spawnCount = Math.max(1, shortValue(tile, "SpawnCount", 4));
                data.maxNearbyEntities = Math.max(1, shortValue(tile, "MaxNearbyEntities", 6));
            }
            case "Music" -> {
                data.type = "note_block";
                data.notePitch = byteValue(tile, "note", 0);
            }
            case "RecordPlayer" -> {
                data.type = "jukebox";
                int recordId = intValue(tile, "Record", 0);
                if (recordId > 0) {
                    data.record = stackData(recordId, 0, 1);
                }
            }
            case "EnchantTable" -> data.type = "enchanting_table";
            default -> {
                return null;
            }
        }
        return data;
    }

    private static SaveManager.DroppedItemData importDroppedItem(Map<String, Object> entity) {
        SaveManager.StackData stack = stackFromItemCompound(compound(entity.get("Item")));
        if (stack == null) {
            return null;
        }
        SaveManager.DroppedItemData data = new SaveManager.DroppedItemData();
        data.itemId = stack.itemId;
        data.dataValue = stack.dataValue;
        data.count = stack.count;
        data.durability = stack.durability;
        data.customName = stack.customName;
        data.enchantments = stack.enchantments;
        data.potion = stack.potion;
        data.metadata = stack.metadata;
        importPosition(entity, data);
        int ageTicks = Math.max(0, shortValue(entity, "Age", 0));
        data.age = Math.min(ageTicks / 20.0f, DroppedItem.DESPAWN_TIME_SECONDS - 1.0f);
        data.health = Math.max(1, Math.min(DroppedItem.MAX_HEALTH, shortValue(entity, "Health", DroppedItem.MAX_HEALTH)));
        data.pickupDelayTicks = Math.max(0, shortValue(entity, "PickupDelay", 0));
        data.pickupDelayAccumulator = clampUnitFloat(floatValue(entity, CZ_ITEM_PICKUP_DELAY_ACCUMULATOR, 0.0f));
        data.rotation = normalizeDegrees(floatValue(entity, CZ_ITEM_ROTATION, data.rotation));
        data.bobPhase = normalizeRadians(floatValue(entity, CZ_ITEM_BOB_PHASE, 0.0f));
        return data;
    }

    private static SaveManager.EntityData importEntity(Map<String, Object> entity) {
        String id = stringValue(entity, "id", "");
        SaveManager.EntityData data = new SaveManager.EntityData();
        importBaseEntity(entity, data);
        switch (id) {
            case "XPOrb" -> {
                data.type = "EXPERIENCE_ORB";
                data.experienceValue = Math.max(1, shortValue(entity, "Value", 1));
                data.pickupDelayTicks = Math.max(0, shortValue(entity, "PickupDelay", 0));
                data.orbHealth = Math.max(1,
                        Math.min(ExperienceOrbEntity.MAX_HEALTH, shortValue(entity, "Health", ExperienceOrbEntity.MAX_HEALTH)));
            }
            case "Arrow" -> {
                data.type = "ARROW";
                data.playerOwned = byteBoolean(entity, "player", false);
                data.damage = (float) doubleValue(entity, "damage", 2.0d);
                data.inGround = byteBoolean(entity, "inGround", false);
                data.stuckTicks = Math.max(0, shortValue(entity, "shake", 0));
                data.blockX = intValue(entity, "xTile", 0);
                data.blockY = intValue(entity, "yTile", 0);
                data.blockZ = intValue(entity, "zTile", 0);
                data.knockbackHorizontal = Math.max(0.0f, floatValue(entity,
                        CZ_ARROW_KNOCKBACK_HORIZONTAL, CombatRules.ARROW_HORIZONTAL_KNOCKBACK));
                data.knockbackVertical = Math.max(0.0f, floatValue(entity,
                        CZ_ARROW_KNOCKBACK_VERTICAL, CombatRules.ARROW_VERTICAL_KNOCKBACK));
                data.fireTicksOnHit = Math.max(0, intValue(entity, CZ_ARROW_FIRE_TICKS_ON_HIT, 0));
                data.critical = byteBoolean(entity, "crit", byteBoolean(entity, CZ_ARROW_CRITICAL, false));
            }
            case "Fireball", "SmallFireball" -> {
                data.type = "FIREBALL";
                data.explosive = "Fireball".equals(id);
                data.ownerPlayer = byteBoolean(entity, CZ_FIREBALL_DEFLECTED_BY_PLAYER, false);
            }
            case "Snowball" -> {
                data.type = "THROWN_ITEM";
                data.projectileItemId = ItemType.SNOWBALL.getId();
                data.playerOwned = byteBoolean(entity, CZ_PROJECTILE_PLAYER_OWNED, false);
            }
            case "Egg" -> {
                data.type = "THROWN_ITEM";
                data.projectileItemId = ItemType.EGG.getId();
                data.playerOwned = byteBoolean(entity, CZ_PROJECTILE_PLAYER_OWNED, false);
            }
            case "ThrownEnderpearl", "EnderPearl" -> {
                data.type = "ENDER_PEARL";
                data.ownerPlayer = byteBoolean(entity, CZ_PROJECTILE_PLAYER_OWNED, true);
            }
            case "CraftZeroFishingHook", "FishingHook", "FishHook" -> {
                data.type = "FISHING_HOOK";
                data.ownerPlayer = byteBoolean(entity, CZ_PROJECTILE_PLAYER_OWNED, true);
                data.fishingWaitTicks = clampInt(intValue(entity, CZ_FISHING_WAIT_TICKS, 0),
                        0, RELEASE_FISHING_MAX_WAIT_TICKS);
                data.fishingCatchableTicks = clampInt(intValue(entity, CZ_FISHING_CATCHABLE_TICKS, 0),
                        0, RELEASE_FISHING_MAX_CATCHABLE_TICKS);
                if (data.fishingCatchableTicks > 0) {
                    data.fishingWaitTicks = 0;
                }
                data.fishingHookStuckInGround = byteBoolean(entity, CZ_FISHING_STUCK_IN_GROUND, false);
            }
            case "ThrownPotion" -> {
                data.type = "SPLASH_POTION";
                PotionData potion = potionDataFromReleaseDamage(shortValue(entity, "Potion",
                        RELEASE_POTION_SPLASH_BIT));
                data.potion = new PotionData(potion.type(), true, potion.extended(), potion.enhanced());
            }
            case "EyeOfEnderSignal" -> {
                data.type = "EYE_OF_ENDER";
                data.targetX = floatValue(entity, CZ_EYE_TARGET_X, data.x);
                data.targetY = floatValue(entity, CZ_EYE_TARGET_Y, data.y);
                data.targetZ = floatValue(entity, CZ_EYE_TARGET_Z, data.z);
                data.dropsItem = byteBoolean(entity, "dropsItem", true);
            }
            case "FallingSand" -> {
                data.type = "FALLING_BLOCK";
                data.fallingBlockId = byteValue(entity, "Tile", intValue(entity, "TileID", 12));
                data.fallingBlockMetadata = byteValue(entity, "Data", 0);
            }
            case "PrimedTnt" -> {
                data.type = "PRIMED_TNT";
                data.fuseTicks = Math.max(0, byteValue(entity, "Fuse", 80));
                data.fuseTicksPresent = true;
            }
            case "Boat" -> {
                data.type = "BOAT";
                data.boatDamage = Math.max(0.0f, floatValue(entity, "Damage", 0.0f));
                data.rollingAmplitude = boundedRollingAmplitude(shortValue(entity, "HurtTime", 0),
                        BoatEntity.HIT_ROLLING_TICKS);
                data.rollingDirection = normalizedRollingDirection(intValue(entity, "ForwardDirection", 1));
            }
            case "EnderCrystal" -> {
                data.type = "END_CRYSTAL";
                data.health = 5.0f;
            }
            case "Painting" -> {
                data.type = "PAINTING";
                data.paintingArt = stringValue(entity, "Motive", "Kebab");
                data.paintingFacing = paintingFacing(byteValue(entity, "Direction", 0));
                data.x = intValue(entity, "TileX", (int) Math.floor(data.x)) + 0.5f;
                data.y = intValue(entity, "TileY", (int) Math.floor(data.y)) + 0.5f;
                data.z = intValue(entity, "TileZ", (int) Math.floor(data.z)) + 0.5f;
            }
            case "Minecart" -> importMinecart(entity, data);
            default -> {
                String mobName = mobDefinitionName(id);
                if (mobName == null) {
                    return null;
                }
                data.type = mobName;
                importMobState(entity, data);
            }
        }
        if (data.type == null) {
            return null;
        }
        importEntityReferences(entity, data);
        return data;
    }

    private static void importEntityReferences(Map<String, Object> entity, SaveManager.EntityData data) {
        data.entitySaveId = Math.max(0, intValue(entity, CZ_ENTITY_SAVE_ID, 0));
        if (isProjectileWithReleaseShooter(data.type)) {
            data.projectileShooterSaveId = Math.max(0, intValue(entity, CZ_PROJECTILE_SHOOTER_SAVE_ID, 0));
        }
        if (parseMobDefinition(data.type) != null) {
            data.mobTargetSaveId = Math.max(0, intValue(entity, CZ_MOB_TARGET_SAVE_ID, 0));
        }
        if ("FISHING_HOOK".equals(data.type)) {
            data.fishingHookedEntitySaveId = Math.max(0, intValue(entity, CZ_FISHING_HOOKED_ENTITY_SAVE_ID, 0));
        }
        if ("MINECART".equals(data.type)) {
            data.minecartPassengerSaveId = Math.max(0, intValue(entity, CZ_MINECART_PASSENGER_SAVE_ID, 0));
        }
        if (parseMobDefinition(data.type) == MobDefinition.SPIDER) {
            data.spiderJockeyRiderSaveId = Math.max(0, intValue(entity, CZ_SPIDER_JOCKEY_RIDER_SAVE_ID, 0));
        }
    }

    private static boolean isProjectileWithReleaseShooter(String type) {
        return "ARROW".equals(type)
                || "FIREBALL".equals(type)
                || "THROWN_ITEM".equals(type)
                || "SPLASH_POTION".equals(type);
    }

    private static void importMinecart(Map<String, Object> entity, SaveManager.EntityData data) {
        data.type = "MINECART";
        int kind = intValue(entity, "Type", 0);
        data.cartKind = switch (kind) {
            case 1 -> MinecartEntity.CartKind.CHEST.name();
            case 2 -> MinecartEntity.CartKind.FURNACE.name();
            default -> MinecartEntity.CartKind.RIDEABLE.name();
        };
        data.cartDamage = Math.max(0.0f, floatValue(entity, "Damage", 0.0f));
        data.rollingAmplitude = boundedRollingAmplitude(intValue(entity, "RollingAmplitude", 0),
                MinecartEntity.HIT_ROLLING_TICKS);
        data.rollingDirection = normalizedRollingDirection(intValue(entity, "RollingDirection", 1));
        if (kind == 1) {
            data.inventory = inventoryFromItems(entity.get("Items"), 27);
        }
        if (kind == 2) {
            data.fuelTicks = Math.max(0, Math.min(FurnaceMinecartEntity.MAX_FUEL_TICKS, shortValue(entity, "Fuel", 0)));
            data.pushX = (float) doubleValue(entity, "PushX", 0.0d);
            data.pushZ = (float) doubleValue(entity, "PushZ", 0.0d);
        }
    }

    private static int boundedRollingAmplitude(int value, int max) {
        return Math.max(0, Math.min(max, value));
    }

    private static int normalizedRollingDirection(int value) {
        return value < 0 ? -1 : 1;
    }

    private static int releaseRollingDirection(int value) {
        return value < 0 ? -1 : 1;
    }

    private static void importMobState(Map<String, Object> entity, SaveManager.EntityData data) {
        MobDefinition definition = parseMobDefinition(data.type);
        float maxHealth = definition == null ? 20.0f : definition.maxHealth();
        float minHealth = "ENDER_DRAGON".equals(data.type) ? 0.0f : 1.0f;
        data.health = Math.max(minHealth, Math.min(maxHealth,
                shortValue(entity, "Health", (int) maxHealth)));
        data.hurtTime = Math.max(0, Math.min(LivingEntity.MAX_HURT_TIME, shortValue(entity, "HurtTime", 0)));
        data.livingAttackCooldown = Math.max(0, shortValue(entity, "AttackTime", 0));
        data.fireTicks = Math.max(0, shortValue(entity, "Fire", 0));
        data.growingAge = intValue(entity, "Age", 0);
        data.loveTicks = Math.max(0, intValue(entity, "InLove", 0));
        data.airTicks = Math.max(0, shortValue(entity, "Air", 300));
        if ("ENDER_DRAGON".equals(data.type)) {
            data.dragonDeathTicks = Math.max(0, shortValue(entity, "DeathTime", 0));
            data.dragonDeathStarted = data.dragonDeathTicks > 0 || data.health <= 0.0f;
        }
        if ("SLIME".equals(data.type) || "MAGMA_CUBE".equals(data.type)) {
            data.slimeSize = Math.max(1, intValue(entity, "Size", 1));
        }
        if ("SHEEP".equals(data.type)) {
            data.sheared = byteBoolean(entity, "Sheared", false);
            data.woolColor = Math.max(0, Math.min(15, byteValue(entity, "Color", 0)));
        }
        if ("PIG".equals(data.type)) {
            data.saddled = byteBoolean(entity, "Saddle", false);
        }
        if ("WOLF".equals(data.type)) {
            data.angry = byteBoolean(entity, "Angry", false);
            data.tamed = byteBoolean(entity, "Tame", false) || !stringValue(entity, "Owner", "").isBlank();
            data.wolfSitting = byteBoolean(entity, "Sitting", false);
            data.wolfOwnerName = stringValue(entity, "Owner", "");
        }
        if ("CREEPER".equals(data.type)) {
            data.ignited = byteBoolean(entity, "ignited", false);
            data.creeperPowered = byteBoolean(entity, "powered", false);
            data.creeperFuseTicks = Math.max(0, Math.min(30, shortValue(entity, "Fuse", 0)));
        }
        if ("CHICKEN".equals(data.type)) {
            data.eggTimer = Math.max(0, intValue(entity, "EggLayTime", 0));
        }
        if ("ENDERMAN".equals(data.type)) {
            data.carriedBlockId = Math.max(0, shortValue(entity, "carried", 0));
            data.carriedMetadata = Math.max(0, Math.min(15, shortValue(entity, "carriedData", 0)));
        }
        if ("ZOMBIE_PIGMAN".equals(data.type)) {
            data.angerTicks = Math.max(0, shortValue(entity, "Anger", 0));
        }
        if ("VILLAGER".equals(data.type)) {
            data.profession = Math.max(0, Math.min(4, intValue(entity, "Profession", 0)));
        }
    }

    private static void importBaseEntity(Map<String, Object> entity, SaveManager.EntityData data) {
        importPosition(entity, data);
        data.yaw = listFloat(entity.get("Rotation"), 0, 0.0f);
        data.pitch = listFloat(entity.get("Rotation"), 1, 0.0f);
        data.onGround = byteBoolean(entity, "OnGround", false);
        float fallDistance = Math.max(0.0f, floatValue(entity, "FallDistance", 0.0f));
        data.fallStartY = data.y + fallDistance;
        data.falling = fallDistance > 0.0f && !Boolean.TRUE.equals(data.onGround);
        data.age = releaseEntityTicksExisted(entity);
    }

    private static int releaseEntityTicksExisted(Map<String, Object> entity) {
        int customAge = intValue(entity, CZ_ENTITY_TICKS_EXISTED, -1);
        if (customAge >= 0) {
            return customAge;
        }
        String id = stringValue(entity, "id", "");
        if (mobDefinition(id) != null) {
            return 0;
        }
        return Math.max(0, intValue(entity, "Age", 0));
    }

    private static void importPosition(Map<String, Object> entity, SaveManager.DroppedItemData data) {
        data.x = listFloat(entity.get("Pos"), 0, 0.0f);
        data.y = listFloat(entity.get("Pos"), 1, 0.0f);
        data.z = listFloat(entity.get("Pos"), 2, 0.0f);
        data.velocityX = listFloat(entity.get("Motion"), 0, 0.0f);
        data.velocityY = listFloat(entity.get("Motion"), 1, 0.0f);
        data.velocityZ = listFloat(entity.get("Motion"), 2, 0.0f);
        data.onGround = byteBoolean(entity, "OnGround", false);
        data.rotation = listFloat(entity.get("Rotation"), 0, 0.0f);
    }

    private static void importPosition(Map<String, Object> entity, SaveManager.EntityData data) {
        data.x = listFloat(entity.get("Pos"), 0, 0.0f);
        data.y = listFloat(entity.get("Pos"), 1, 0.0f);
        data.z = listFloat(entity.get("Pos"), 2, 0.0f);
        data.motionX = listFloat(entity.get("Motion"), 0, 0.0f);
        data.motionY = listFloat(entity.get("Motion"), 1, 0.0f);
        data.motionZ = listFloat(entity.get("Motion"), 2, 0.0f);
    }

    private static void writeEntityList(DataOutputStream out, String name, int chunkX, int chunkZ,
            SaveManager.DimensionRuntimeData runtimeData) throws IOException {
        ArrayList<CompoundWriter> writers = new ArrayList<>();
        if (runtimeData != null && runtimeData.droppedItems != null) {
            for (SaveManager.DroppedItemData item : runtimeData.droppedItems) {
                if (item != null && isEntityInChunk(item.x, item.z, chunkX, chunkZ)) {
                    writers.add(entityOut -> writeDroppedItemEntity(entityOut, item));
                }
            }
        }
        if (runtimeData != null && runtimeData.entities != null) {
            for (SaveManager.EntityData entity : runtimeData.entities) {
                if (entity != null && isEntityInChunk(entity.x, entity.z, chunkX, chunkZ)) {
                    CompoundWriter writer = writerForEntity(entity);
                    if (writer != null) {
                        writers.add(writer);
                    }
                }
            }
        }
        writeCompoundList(out, name, writers);
    }

    private static void writeTileEntityList(DataOutputStream out, String name, int chunkX, int chunkZ,
            SaveManager.DimensionRuntimeData runtimeData) throws IOException {
        ArrayList<CompoundWriter> writers = new ArrayList<>();
        if (runtimeData != null && runtimeData.tileEntities != null) {
            for (SaveManager.TileEntityData tile : runtimeData.tileEntities) {
                if (tile != null && isBlockInChunk(tile.x, tile.z, chunkX, chunkZ)) {
                    CompoundWriter writer = writerForTileEntity(tile);
                    if (writer != null) {
                        writers.add(writer);
                    }
                }
            }
        }
        if (runtimeData != null && runtimeData.movingPistons != null) {
            for (SaveManager.MovingPistonData piston : runtimeData.movingPistons) {
                if (piston != null && isBlockInChunk(piston.x, piston.z, chunkX, chunkZ)) {
                    CompoundWriter writer = writerForMovingPiston(piston);
                    if (writer != null) {
                        writers.add(writer);
                    }
                }
            }
        }
        writeCompoundList(out, name, writers);
    }

    private static void writeTileTickList(DataOutputStream out, String name, int chunkX, int chunkZ,
            SaveManager.DimensionRuntimeData runtimeData) throws IOException {
        ArrayList<CompoundWriter> writers = new ArrayList<>();
        if (runtimeData != null && runtimeData.scheduledBlockTicks != null) {
            for (SaveManager.ScheduledBlockTickData tick : runtimeData.scheduledBlockTicks) {
                if (tick != null
                        && tick.blockId > 0
                        && tick.delayTicks >= 0
                        && tick.y >= 0
                        && tick.y < Chunk.HEIGHT
                        && isBlockInChunk(tick.x, tick.z, chunkX, chunkZ)) {
                    writers.add(outTick -> {
                        writeInt(outTick, "i", tick.blockId);
                        writeInt(outTick, "x", tick.x);
                        writeInt(outTick, "y", tick.y);
                        writeInt(outTick, "z", tick.z);
                        writeInt(outTick, "t", tick.delayTicks);
                    });
                }
            }
        }
        writeCompoundList(out, name, writers);
    }

    private static CompoundWriter writerForTileEntity(SaveManager.TileEntityData tile) {
        return switch (tile.type == null ? "" : tile.type) {
            case "chest" -> out -> {
                writeString(out, "id", "Chest");
                writeBlockPosition(out, tile.x, tile.y, tile.z);
                writeItems(out, "Items", tile.inventory);
            };
            case "furnace" -> out -> {
                writeString(out, "id", "Furnace");
                writeBlockPosition(out, tile.x, tile.y, tile.z);
                writeItems(out, "Items", tile.inventory);
                writeShort(out, "BurnTime", tile.burnTime);
                writeShort(out, "CookTime", tile.cookTime);
            };
            case "dispenser" -> out -> {
                writeString(out, "id", "Trap");
                writeBlockPosition(out, tile.x, tile.y, tile.z);
                writeItems(out, "Items", tile.inventory);
            };
            case "brewing_stand" -> out -> {
                writeString(out, "id", "Cauldron");
                writeBlockPosition(out, tile.x, tile.y, tile.z);
                writeItems(out, "Items", tile.inventory);
                writeShort(out, "BrewTime", tile.brewTime);
            };
            case "sign" -> out -> {
                writeString(out, "id", "Sign");
                writeBlockPosition(out, tile.x, tile.y, tile.z);
                for (int i = 0; i < 4; i++) {
                    String text = tile.signText != null && i < tile.signText.length ? tile.signText[i] : "";
                    writeString(out, "Text" + (i + 1), trimSignLine(text));
                }
            };
            case "mob_spawner" -> out -> {
                writeString(out, "id", "MobSpawner");
                writeBlockPosition(out, tile.x, tile.y, tile.z);
                writeString(out, "EntityId", releaseMobId(tile.mobType));
                writeShort(out, "Delay", tile.spawnDelay);
                writeShort(out, "MinSpawnDelay", tile.minSpawnDelay);
                writeShort(out, "MaxSpawnDelay", tile.maxSpawnDelay);
                writeShort(out, "SpawnCount", tile.spawnCount);
                writeShort(out, "MaxNearbyEntities", tile.maxNearbyEntities);
            };
            case "note_block" -> out -> {
                writeString(out, "id", "Music");
                writeBlockPosition(out, tile.x, tile.y, tile.z);
                writeByte(out, "note", tile.notePitch);
            };
            case "jukebox" -> out -> {
                writeString(out, "id", "RecordPlayer");
                writeBlockPosition(out, tile.x, tile.y, tile.z);
                writeInt(out, "Record", tile.record == null ? 0 : tile.record.itemId);
            };
            case "enchanting_table" -> out -> {
                writeString(out, "id", "EnchantTable");
                writeBlockPosition(out, tile.x, tile.y, tile.z);
            };
            default -> null;
        };
    }

    private static CompoundWriter writerForMovingPiston(SaveManager.MovingPistonData piston) {
        BlockType carried = BlockType.fromId(piston.carriedBlockId);
        if (carried == null || piston.y < 0 || piston.y >= Chunk.HEIGHT || !isReleasePistonFacing(piston.facing)) {
            return null;
        }
        boolean extending = isMovingTowardFacing(piston);
        float progress = Math.max(0.0f, Math.min(1.0f,
                piston.elapsedTicks / (float) RedstoneEngine.PISTON_MOVEMENT_TICKS));
        return out -> {
            writeString(out, "id", "Piston");
            writeBlockPosition(out, piston.x, piston.y, piston.z);
            writeInt(out, "blockId", piston.carriedBlockId);
            writeInt(out, "blockData", piston.carriedMetadata & 15);
            writeInt(out, "facing", piston.facing & 7);
            writeFloat(out, "progress", progress);
            writeByte(out, "extending", extending ? 1 : 0);
        };
    }

    private static boolean isMovingTowardFacing(SaveManager.MovingPistonData piston) {
        float dx = piston.toX - piston.fromX;
        float dy = piston.toY - piston.fromY;
        float dz = piston.toZ - piston.fromZ;
        return dx * RedstoneEngine.faceToDx(piston.facing)
                + dy * RedstoneEngine.faceToDy(piston.facing)
                + dz * RedstoneEngine.faceToDz(piston.facing) > 0.0f;
    }

    private static boolean isReleasePistonFacing(int facing) {
        return facing >= Block.FACE_BOTTOM && facing <= Block.FACE_EAST;
    }

    private static CompoundWriter writerForEntity(SaveManager.EntityData entity) {
        return switch (entity.type == null ? "" : entity.type) {
            case "EXPERIENCE_ORB" -> out -> {
                writeString(out, "id", "XPOrb");
                writeBaseEntity(out, entity);
                writeShort(out, "Value", entity.experienceValue);
                writeShort(out, "Health", entity.orbHealth);
                writeShort(out, "Age", entity.age);
                writeShort(out, "PickupDelay", entity.pickupDelayTicks);
            };
            case "ARROW" -> out -> {
                writeString(out, "id", "Arrow");
                writeBaseEntity(out, entity);
                writeByte(out, "player", entity.playerOwned ? 1 : 0);
                writeByte(out, "inGround", entity.inGround ? 1 : 0);
                writeInt(out, "xTile", entity.blockX);
                writeInt(out, "yTile", entity.blockY);
                writeInt(out, "zTile", entity.blockZ);
                writeShort(out, "shake", entity.stuckTicks);
                writeDouble(out, "damage", entity.damage <= 0.0f ? 2.0d : entity.damage);
                writeByte(out, "crit", entity.critical ? 1 : 0);
                writeFloat(out, CZ_ARROW_KNOCKBACK_HORIZONTAL, Math.max(0.0f, entity.knockbackHorizontal));
                writeFloat(out, CZ_ARROW_KNOCKBACK_VERTICAL, Math.max(0.0f, entity.knockbackVertical));
                writeInt(out, CZ_ARROW_FIRE_TICKS_ON_HIT, Math.max(0, entity.fireTicksOnHit));
                writeByte(out, CZ_ARROW_CRITICAL, entity.critical ? 1 : 0);
            };
            case "FIREBALL" -> out -> {
                writeString(out, "id", entity.explosive ? "Fireball" : "SmallFireball");
                writeBaseEntity(out, entity);
                writeByte(out, CZ_FIREBALL_DEFLECTED_BY_PLAYER, entity.ownerPlayer ? 1 : 0);
            };
            case "ENDER_PEARL" -> out -> {
                writeString(out, "id", "ThrownEnderpearl");
                writeBaseEntity(out, entity);
                writeByte(out, CZ_PROJECTILE_PLAYER_OWNED, entity.ownerPlayer ? 1 : 0);
            };
            case "FISHING_HOOK" -> out -> {
                writeString(out, "id", "CraftZeroFishingHook");
                writeBaseEntity(out, entity);
                writeByte(out, CZ_PROJECTILE_PLAYER_OWNED, entity.ownerPlayer ? 1 : 0);
                writeInt(out, CZ_FISHING_WAIT_TICKS,
                        clampInt(entity.fishingWaitTicks, 0, RELEASE_FISHING_MAX_WAIT_TICKS));
                writeInt(out, CZ_FISHING_CATCHABLE_TICKS,
                        clampInt(entity.fishingCatchableTicks, 0, RELEASE_FISHING_MAX_CATCHABLE_TICKS));
                writeByte(out, CZ_FISHING_STUCK_IN_GROUND, entity.fishingHookStuckInGround ? 1 : 0);
            };
            case "THROWN_ITEM" -> out -> {
                writeString(out, "id", entity.projectileItemId == ItemType.EGG.getId() ? "Egg" : "Snowball");
                writeBaseEntity(out, entity);
                writeByte(out, CZ_PROJECTILE_PLAYER_OWNED, entity.playerOwned ? 1 : 0);
            };
            case "SPLASH_POTION" -> out -> {
                writeString(out, "id", "ThrownPotion");
                writeBaseEntity(out, entity);
                PotionData potion = entity.potion == null ? PotionData.water() : entity.potion;
                writeShort(out, "Potion", releasePotionDamage(
                        new PotionData(potion.type(), true, potion.extended(), potion.enhanced()),
                        RELEASE_POTION_SPLASH_BIT));
            };
            case "EYE_OF_ENDER" -> out -> {
                writeString(out, "id", "EyeOfEnderSignal");
                writeBaseEntity(out, entity);
                writeByte(out, "dropsItem", entity.dropsItem ? 1 : 0);
                writeFloat(out, CZ_EYE_TARGET_X, entity.targetX);
                writeFloat(out, CZ_EYE_TARGET_Y, entity.targetY);
                writeFloat(out, CZ_EYE_TARGET_Z, entity.targetZ);
            };
            case "FALLING_BLOCK" -> out -> {
                writeString(out, "id", "FallingSand");
                writeBaseEntity(out, entity);
                writeByte(out, "Tile", entity.fallingBlockId);
                writeByte(out, "Data", entity.fallingBlockMetadata);
            };
            case "PRIMED_TNT" -> out -> {
                writeString(out, "id", "PrimedTnt");
                writeBaseEntity(out, entity);
                writeByte(out, "Fuse", entity.fuseTicksPresent ? entity.fuseTicks : 80);
            };
            case "BOAT" -> out -> {
                writeString(out, "id", "Boat");
                writeBaseEntity(out, entity);
                writeFloat(out, "Damage", entity.boatDamage);
                writeShort(out, "HurtTime", entity.rollingAmplitude);
                writeInt(out, "ForwardDirection", releaseRollingDirection(entity.rollingDirection));
            };
            case "END_CRYSTAL" -> out -> {
                writeString(out, "id", "EnderCrystal");
                writeBaseEntity(out, entity);
            };
            case "PAINTING" -> out -> {
                writeString(out, "id", "Painting");
                writeBaseEntity(out, entity);
                writeString(out, "Motive", entity.paintingArt);
                writeByte(out, "Direction", releasePaintingDirection(entity.paintingFacing));
                writeInt(out, "TileX", (int) Math.floor(entity.x));
                writeInt(out, "TileY", (int) Math.floor(entity.y));
                writeInt(out, "TileZ", (int) Math.floor(entity.z));
            };
            case "MINECART" -> out -> writeMinecartEntity(out, entity);
            default -> parseMobDefinition(entity.type) == null ? null : out -> writeMobEntity(out, entity);
        };
    }

    private static void writeDroppedItemEntity(DataOutputStream out, SaveManager.DroppedItemData item)
            throws IOException {
        writeString(out, "id", "Item");
        writeEntityVectors(out, item.x, item.y, item.z, item.velocityX, item.velocityY, item.velocityZ,
                item.rotation, 0.0f);
        writeShort(out, "Health", item.health == null ? DroppedItem.MAX_HEALTH : item.health);
        writeShort(out, "Age", Math.max(0, Math.round(item.age * 20.0f)));
        writeShort(out, "PickupDelay", item.pickupDelayTicks == null ? 0 : item.pickupDelayTicks);
        writeByte(out, "OnGround", item.onGround ? 1 : 0);
        writeShort(out, "Fire", 0);
        writeShort(out, "Air", 300);
        writeFloat(out, "FallDistance", 0.0f);
        writeFloat(out, CZ_ITEM_ROTATION, item.rotation);
        writeFloat(out, CZ_ITEM_BOB_PHASE, item.bobPhase);
        writeFloat(out, CZ_ITEM_PICKUP_DELAY_ACCUMULATOR, Math.max(0.0f, item.pickupDelayAccumulator));
        writeItemCompound(out, "Item", stackData(item));
    }

    private static void writeMinecartEntity(DataOutputStream out, SaveManager.EntityData entity) throws IOException {
        writeString(out, "id", "Minecart");
        writeBaseEntity(out, entity);
        MinecartEntity.CartKind kind = cartKind(entity.cartKind);
        writeInt(out, "Type", switch (kind) {
            case CHEST -> 1;
            case FURNACE -> 2;
            default -> 0;
        });
        writeFloat(out, "Damage", entity.cartDamage);
        writeInt(out, "RollingAmplitude", entity.rollingAmplitude);
        writeInt(out, "RollingDirection", releaseRollingDirection(entity.rollingDirection));
        if (kind == MinecartEntity.CartKind.CHEST) {
            writeItems(out, "Items", entity.inventory);
        }
        if (kind == MinecartEntity.CartKind.FURNACE) {
            writeShort(out, "Fuel", entity.fuelTicks);
            writeDouble(out, "PushX", entity.pushX);
            writeDouble(out, "PushZ", entity.pushZ);
        }
    }

    private static void writeMobEntity(DataOutputStream out, SaveManager.EntityData entity) throws IOException {
        writeString(out, "id", releaseMobId(entity.type));
        writeBaseEntity(out, entity);
        writeShort(out, "Health", Math.round(entity.health));
        writeShort(out, "HurtTime", entity.hurtTime);
        writeShort(out, "DeathTime", "ENDER_DRAGON".equals(entity.type) ? entity.dragonDeathTicks : 0);
        writeShort(out, "AttackTime", entity.livingAttackCooldown);
        writeShort(out, "Fire", entity.fireTicks);
        writeInt(out, "Age", entity.growingAge);
        writeInt(out, "InLove", entity.loveTicks);
        if ("SLIME".equals(entity.type) || "MAGMA_CUBE".equals(entity.type)) {
            writeInt(out, "Size", entity.slimeSize <= 0 ? 1 : entity.slimeSize);
        }
        if ("SHEEP".equals(entity.type)) {
            writeByte(out, "Sheared", entity.sheared ? 1 : 0);
            writeByte(out, "Color", entity.woolColor);
        }
        if ("PIG".equals(entity.type)) {
            writeByte(out, "Saddle", entity.saddled ? 1 : 0);
        }
        if ("WOLF".equals(entity.type)) {
            writeByte(out, "Angry", entity.angry ? 1 : 0);
            writeByte(out, "Tame", entity.tamed ? 1 : 0);
            writeByte(out, "Sitting", entity.wolfSitting ? 1 : 0);
            writeString(out, "Owner", entity.wolfOwnerName);
        }
        if ("CREEPER".equals(entity.type)) {
            writeByte(out, "powered", entity.creeperPowered ? 1 : 0);
            writeShort(out, "Fuse", entity.creeperFuseTicks);
        }
        if ("CHICKEN".equals(entity.type)) {
            writeInt(out, "EggLayTime", entity.eggTimer);
        }
        if ("ENDERMAN".equals(entity.type)) {
            writeShort(out, "carried", entity.carriedBlockId);
            writeShort(out, "carriedData", entity.carriedMetadata);
        }
        if ("ZOMBIE_PIGMAN".equals(entity.type)) {
            writeShort(out, "Anger", entity.angerTicks);
        }
        if ("VILLAGER".equals(entity.type)) {
            writeInt(out, "Profession", entity.profession);
        }
    }

    private static void writeBaseEntity(DataOutputStream out, SaveManager.EntityData entity) throws IOException {
        writeEntityVectors(out, entity.x, entity.y, entity.z, entity.motionX, entity.motionY, entity.motionZ,
                entity.yaw, entity.pitch);
        writeShort(out, "Fire", entity.fireTicks);
        writeShort(out, "Air", entity.airTicks <= 0 ? 300 : entity.airTicks);
        writeByte(out, "OnGround", Boolean.TRUE.equals(entity.onGround) ? 1 : 0);
        float fallStartY = entity.fallStartY != null && Float.isFinite(entity.fallStartY)
                ? entity.fallStartY
                : entity.y;
        writeFloat(out, "FallDistance", Math.max(0.0f, fallStartY - entity.y));
        writeInt(out, CZ_ENTITY_TICKS_EXISTED, Math.max(0, entity.age));
        writeEntityReferences(out, entity);
    }

    private static void writeEntityReferences(DataOutputStream out, SaveManager.EntityData entity) throws IOException {
        writePositiveInt(out, CZ_ENTITY_SAVE_ID, entity.entitySaveId);
        if (isProjectileWithReleaseShooter(entity.type)) {
            writePositiveInt(out, CZ_PROJECTILE_SHOOTER_SAVE_ID, entity.projectileShooterSaveId);
        }
        if (parseMobDefinition(entity.type) != null) {
            writePositiveInt(out, CZ_MOB_TARGET_SAVE_ID, entity.mobTargetSaveId);
        }
        if ("FISHING_HOOK".equals(entity.type)) {
            writePositiveInt(out, CZ_FISHING_HOOKED_ENTITY_SAVE_ID, entity.fishingHookedEntitySaveId);
        }
        if ("MINECART".equals(entity.type)) {
            writePositiveInt(out, CZ_MINECART_PASSENGER_SAVE_ID, entity.minecartPassengerSaveId);
        }
        if (parseMobDefinition(entity.type) == MobDefinition.SPIDER) {
            writePositiveInt(out, CZ_SPIDER_JOCKEY_RIDER_SAVE_ID, entity.spiderJockeyRiderSaveId);
        }
    }

    private static void writePositiveInt(DataOutputStream out, String name, int value) throws IOException {
        if (value > 0) {
            writeInt(out, name, value);
        }
    }

    private static SaveManager.StackData[] inventoryFromItems(Object value, int size) {
        SaveManager.StackData[] inventory = new SaveManager.StackData[size];
        for (Map<String, Object> item : compoundList(value)) {
            int slot = byteValue(item, "Slot", -1);
            if (slot < 0 || slot >= inventory.length) {
                continue;
            }
            SaveManager.StackData stack = stackFromItemCompound(item);
            if (stack != null) {
                inventory[slot] = stack;
            }
        }
        return inventory;
    }

    private static SaveManager.StackData stackFromItemCompound(Map<String, Object> item) {
        int id = shortValue(item, "id", 0);
        int count = byteValue(item, "Count", 0);
        int damage = shortValue(item, "Damage", 0);
        SaveManager.StackData stack = stackData(id, damage, count);
        if (stack == null) {
            return null;
        }
        importStackTag(stack, compound(item.get("tag")));
        return stack;
    }

    private static SaveManager.StackData stackData(int id, int damage, int count) {
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
            putStackMetadata(stack, "map.initialized", "true");
            putStackMetadata(stack, "map.id", MAP_ID_PREFIX + Math.max(0, damage));
            putStackMetadata(stack, "map.scale", "3");
        } else {
            stack.dataValue = Math.max(0, damage);
            stack.durability = -1;
        }
        if (type == ItemType.POTION) {
            stack.potion = potionDataFromReleaseDamage(damage);
        }
        return stack;
    }

    private static SaveManager.StackData stackData(SaveManager.DroppedItemData item) {
        SaveManager.StackData stack = new SaveManager.StackData();
        stack.itemId = item.itemId;
        stack.dataValue = item.dataValue;
        stack.count = item.count;
        stack.durability = item.durability;
        stack.customName = item.customName;
        stack.enchantments = item.enchantments;
        stack.potion = item.potion;
        stack.metadata = item.metadata;
        return stack;
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

    private static void writeItems(DataOutputStream out, String name, SaveManager.StackData[] inventory)
            throws IOException {
        ArrayList<CompoundWriter> writers = new ArrayList<>();
        if (inventory != null) {
            for (int slot = 0; slot < inventory.length; slot++) {
                SaveManager.StackData stack = inventory[slot];
                if (stack != null && stack.count > 0) {
                    int capturedSlot = slot;
                    writers.add(itemOut -> {
                        writeByte(itemOut, "Slot", capturedSlot);
                        writeStackPayload(itemOut, stack);
                    });
                }
            }
        }
        writeCompoundList(out, name, writers);
    }

    private static void writeItemCompound(DataOutputStream out, String name, int itemId, int dataValue,
            int count, int durability) throws IOException {
        SaveManager.StackData stack = new SaveManager.StackData();
        stack.itemId = itemId;
        stack.dataValue = dataValue;
        stack.count = count;
        stack.durability = durability;
        writeItemCompound(out, name, stack);
    }

    private static void writeItemCompound(DataOutputStream out, String name, SaveManager.StackData stack)
            throws IOException {
        out.writeByte(TAG_COMPOUND);
        writeString(out, name);
        writeStackPayload(out, stack);
        out.writeByte(TAG_END);
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

    private static void writeStackPayload(DataOutputStream out, int itemId, int dataValue,
            int count, int durability) throws IOException {
        SaveManager.StackData stack = new SaveManager.StackData();
        stack.itemId = itemId;
        stack.dataValue = dataValue;
        stack.count = count;
        stack.durability = durability;
        writeStackPayload(out, stack);
    }

    private static void importStackTag(SaveManager.StackData stack, Map<String, Object> tag) {
        if (tag.isEmpty()) {
            return;
        }
        Map<String, Object> display = compound(tag.get("display"));
        String customName = stringValue(display, "Name", "");
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

    private static void writeEntityVectors(DataOutputStream out, float x, float y, float z,
            float motionX, float motionY, float motionZ, float yaw, float pitch) throws IOException {
        writeDoubleList(out, "Pos", x, y, z);
        writeDoubleList(out, "Motion", motionX, motionY, motionZ);
        writeFloatList(out, "Rotation", yaw, pitch);
    }

    private static void writeBlockPosition(DataOutputStream out, int x, int y, int z) throws IOException {
        writeInt(out, "x", x);
        writeInt(out, "y", y);
        writeInt(out, "z", z);
    }

    private static boolean isBlockInChunk(int x, int z, int chunkX, int chunkZ) {
        return Math.floorDiv(x, Chunk.WIDTH) == chunkX
                && Math.floorDiv(z, Chunk.DEPTH) == chunkZ;
    }

    private static boolean isEntityInChunk(float x, float z, int chunkX, int chunkZ) {
        return Math.floorDiv((int) Math.floor(x), Chunk.WIDTH) == chunkX
                && Math.floorDiv((int) Math.floor(z), Chunk.DEPTH) == chunkZ;
    }

    private static List<Map<String, Object>> compoundList(Object value) {
        Object[] values = listValue(value);
        if (values.length == 0) {
            return List.of();
        }
        ArrayList<Map<String, Object>> compounds = new ArrayList<>();
        for (Object item : values) {
            Map<String, Object> compound = compound(item);
            if (!compound.isEmpty()) {
                compounds.add(compound);
            }
        }
        return compounds;
    }

    private static Object[] listValue(Object value) {
        return value instanceof Object[] values ? values : new Object[0];
    }

    private static float listFloat(Object value, int index, float fallback) {
        Object[] values = listValue(value);
        if (index < 0 || index >= values.length || !(values[index] instanceof Number number)) {
            return fallback;
        }
        float parsed = number.floatValue();
        return Float.isFinite(parsed) ? parsed : fallback;
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

    private static float floatValue(Map<String, Object> data, String key, float fallback) {
        Object value = data.get(key);
        if (!(value instanceof Number number)) {
            return fallback;
        }
        float parsed = number.floatValue();
        return Float.isFinite(parsed) ? parsed : fallback;
    }

    private static float clampUnitFloat(float value) {
        return Float.isFinite(value) ? Math.max(0.0f, Math.min(0.9999f, value)) : 0.0f;
    }

    private static int clampInt(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static float normalizeDegrees(float value) {
        if (!Float.isFinite(value)) {
            return 0.0f;
        }
        float normalized = value % 360.0f;
        return normalized < 0.0f ? normalized + 360.0f : normalized;
    }

    private static float normalizeRadians(float value) {
        if (!Float.isFinite(value)) {
            return 0.0f;
        }
        float fullTurn = (float) (Math.PI * 2.0);
        float normalized = value % fullTurn;
        return normalized < 0.0f ? normalized + fullTurn : normalized;
    }

    private static double doubleValue(Map<String, Object> data, String key, double fallback) {
        Object value = data.get(key);
        if (!(value instanceof Number number)) {
            return fallback;
        }
        double parsed = number.doubleValue();
        return Double.isFinite(parsed) ? parsed : fallback;
    }

    private static long longValue(Map<String, Object> data, String key, long fallback) {
        Object value = data.get(key);
        return value instanceof Number number ? number.longValue() : fallback;
    }

    private static boolean byteBoolean(Map<String, Object> data, String key, boolean fallback) {
        Object value = data.get(key);
        return value instanceof Number number ? number.byteValue() != 0 : fallback;
    }

    private static String stringValue(Map<String, Object> data, String key, String fallback) {
        Object value = data.get(key);
        return value instanceof String text ? text : fallback;
    }

    private static String trimSignLine(String value) {
        String text = value == null ? "" : value;
        return text.length() <= 15 ? text : text.substring(0, 15);
    }

    private static int paintingFacing(int releaseDirection) {
        return switch (releaseDirection & 3) {
            case 1 -> Block.FACE_WEST;
            case 2 -> Block.FACE_NORTH;
            case 3 -> Block.FACE_EAST;
            default -> Block.FACE_SOUTH;
        };
    }

    private static int releasePaintingDirection(int face) {
        return switch (face) {
            case Block.FACE_WEST -> 1;
            case Block.FACE_NORTH -> 2;
            case Block.FACE_EAST -> 3;
            default -> 0;
        };
    }

    private static String mobDefinitionName(String releaseId) {
        MobDefinition definition = mobDefinition(releaseId);
        return definition == null ? null : definition.name();
    }

    private static MobDefinition parseMobDefinition(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return MobDefinition.valueOf(value);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private static MobDefinition mobDefinition(String releaseId) {
        if (releaseId == null || releaseId.isBlank()) {
            return null;
        }
        String normalized = releaseId.replace(" ", "").replace("_", "").toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "zombie" -> MobDefinition.ZOMBIE;
            case "skeleton" -> MobDefinition.SKELETON;
            case "creeper" -> MobDefinition.CREEPER;
            case "spider" -> MobDefinition.SPIDER;
            case "slime" -> MobDefinition.SLIME;
            case "enderman" -> MobDefinition.ENDERMAN;
            case "cavespider" -> MobDefinition.CAVE_SPIDER;
            case "silverfish" -> MobDefinition.SILVERFISH;
            case "giant" -> MobDefinition.GIANT;
            case "ghast" -> MobDefinition.GHAST;
            case "pigzombie", "zombiepigman" -> MobDefinition.ZOMBIE_PIGMAN;
            case "blaze" -> MobDefinition.BLAZE;
            case "lavaslime", "magmacube" -> MobDefinition.MAGMA_CUBE;
            case "enderdragon" -> MobDefinition.ENDER_DRAGON;
            case "pig" -> MobDefinition.PIG;
            case "cow" -> MobDefinition.COW;
            case "sheep" -> MobDefinition.SHEEP;
            case "chicken" -> MobDefinition.CHICKEN;
            case "squid" -> MobDefinition.SQUID;
            case "wolf" -> MobDefinition.WOLF;
            case "mushroomcow", "mooshroom" -> MobDefinition.MOOSHROOM;
            case "villager" -> MobDefinition.VILLAGER;
            case "snowman", "snowgolem" -> MobDefinition.SNOW_GOLEM;
            default -> parseMobDefinition(releaseId);
        };
    }

    private static String releaseMobId(String craftZeroType) {
        MobDefinition definition = parseMobDefinition(craftZeroType);
        if (definition == null) {
            return "Pig";
        }
        return switch (definition) {
            case CAVE_SPIDER -> "CaveSpider";
            case ZOMBIE_PIGMAN -> "PigZombie";
            case MAGMA_CUBE -> "LavaSlime";
            case ENDER_DRAGON -> "EnderDragon";
            case MOOSHROOM -> "MushroomCow";
            case SNOW_GOLEM -> "SnowMan";
            default -> {
                String[] parts = definition.name().toLowerCase(Locale.ROOT).split("_");
                StringBuilder builder = new StringBuilder();
                for (String part : parts) {
                    builder.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
                }
                yield builder.toString();
            }
        };
    }

    private static MinecartEntity.CartKind cartKind(String value) {
        if (value == null || value.isBlank()) {
            return MinecartEntity.CartKind.RIDEABLE;
        }
        try {
            return MinecartEntity.CartKind.valueOf(value);
        } catch (IllegalArgumentException ignored) {
            return MinecartEntity.CartKind.RIDEABLE;
        }
    }

    private static int[] deriveHeightMap(short[] blockIds) {
        int[] values = new int[Chunk.HEIGHT_MAP_SIZE];
        Arrays.fill(values, -1);
        for (int x = 0; x < Chunk.WIDTH; x++) {
            for (int z = 0; z < Chunk.DEPTH; z++) {
                for (int y = Chunk.HEIGHT - 1; y >= 0; y--) {
                    if (blockIds[Chunk.getIndex(x, y, z)] != 0) {
                        values[x + z * Chunk.WIDTH] = y;
                        break;
                    }
                }
            }
        }
        return values;
    }

    private static byte[] releaseHeightMap(int[] heightMap) {
        byte[] values = new byte[Chunk.HEIGHT_MAP_SIZE];
        for (int i = 0; i < values.length && i < heightMap.length; i++) {
            values[i] = (byte) Math.max(0, Math.min(255, heightMap[i]));
        }
        return values;
    }

    private static int[] heightMap(Object value) {
        if (value instanceof byte[] bytes && bytes.length == Chunk.HEIGHT_MAP_SIZE) {
            int[] result = new int[Chunk.HEIGHT_MAP_SIZE];
            for (int i = 0; i < result.length; i++) {
                result[i] = bytes[i] & 0xFF;
            }
            return result;
        }
        if (value instanceof int[] ints && ints.length == Chunk.HEIGHT_MAP_SIZE) {
            return Arrays.copyOf(ints, ints.length);
        }
        return null;
    }

    private static int releaseIndex(int x, int y, int z) {
        return (x << 11) | (z << 7) | y;
    }

    private static byte[] nibbleArray(Object value) throws IOException {
        byte[] bytes = byteArray(value);
        if (bytes == null) {
            return null;
        }
        if (bytes.length != RELEASE_CHUNK_NIBBLE_BYTES) {
            throw new IOException("Release chunk nibble array length is invalid");
        }
        return bytes;
    }

    private static int getNibble(byte[] values, int index) {
        if (values == null) {
            return 0;
        }
        int byteIndex = index >> 1;
        if (byteIndex < 0 || byteIndex >= values.length) {
            return 0;
        }
        return (index & 1) == 0 ? values[byteIndex] & 0x0F : (values[byteIndex] >>> 4) & 0x0F;
    }

    private static void setNibble(byte[] values, int index, int value) {
        int byteIndex = index >> 1;
        if (byteIndex < 0 || byteIndex >= values.length) {
            return;
        }
        int clamped = Math.max(0, Math.min(15, value));
        if ((index & 1) == 0) {
            values[byteIndex] = (byte) ((values[byteIndex] & 0xF0) | clamped);
        } else {
            values[byteIndex] = (byte) ((values[byteIndex] & 0x0F) | (clamped << 4));
        }
    }

    private static Map<String, Object> readRoot(InputStream stream) throws IOException {
        try (DataInputStream in = new DataInputStream(stream)) {
            int type = in.readUnsignedByte();
            if (type != TAG_COMPOUND) {
                throw new IOException("Release chunk root is not a compound");
            }
            readString(in);
            return readCompound(in);
        } catch (EOFException exception) {
            throw new IOException("Release chunk NBT ended unexpectedly", exception);
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
                int length = checkedLength(in.readInt(), "byte array");
                byte[] data = new byte[length];
                in.readFully(data);
                yield data;
            }
            case TAG_STRING -> readString(in);
            case TAG_LIST -> readList(in);
            case TAG_COMPOUND -> readCompound(in);
            case TAG_INT_ARRAY -> {
                int length = checkedLength(in.readInt(), "int array");
                int[] data = new int[length];
                for (int i = 0; i < data.length; i++) {
                    data[i] = in.readInt();
                }
                yield data;
            }
            default -> throw new IOException("Unsupported Release chunk NBT tag type: " + type);
        };
    }

    private static Object readList(DataInputStream in) throws IOException {
        int elementType = in.readUnsignedByte();
        int length = checkedLength(in.readInt(), "list");
        Object[] values = new Object[length];
        for (int i = 0; i < length; i++) {
            values[i] = readPayload(in, elementType);
        }
        return values;
    }

    private static int checkedLength(int length, String label) throws IOException {
        if (length < 0 || length > MAX_NBT_ARRAY_LENGTH) {
            throw new IOException("Invalid Release chunk " + label + " length: " + length);
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

    private static void writeDouble(DataOutputStream out, String name, double value) throws IOException {
        out.writeByte(TAG_DOUBLE);
        writeString(out, name);
        out.writeDouble(Double.isFinite(value) ? value : 0.0d);
    }

    private static void writeString(DataOutputStream out, String name, String value) throws IOException {
        out.writeByte(TAG_STRING);
        writeString(out, name);
        writeString(out, value == null ? "" : value);
    }

    private static void writeByteArray(DataOutputStream out, String name, byte[] values) throws IOException {
        out.writeByte(TAG_BYTE_ARRAY);
        writeString(out, name);
        out.writeInt(values.length);
        out.write(values);
    }

    private static void writeEmptyList(DataOutputStream out, String name, int elementType) throws IOException {
        out.writeByte(TAG_LIST);
        writeString(out, name);
        out.writeByte(elementType);
        out.writeInt(0);
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

    private static void writeString(DataOutputStream out, String value) throws IOException {
        byte[] bytes = value == null ? new byte[0] : value.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        if (bytes.length > MAX_NBT_STRING_BYTES) {
            throw new IOException("Release chunk NBT string too long");
        }
        out.writeShort(bytes.length);
        out.write(bytes);
    }

    private static String readString(DataInputStream in) throws IOException {
        int length = in.readUnsignedShort();
        if (length > MAX_NBT_STRING_BYTES) {
            throw new IOException("Release chunk NBT string too long");
        }
        byte[] bytes = new byte[length];
        in.readFully(bytes);
        return new String(bytes, java.nio.charset.StandardCharsets.UTF_8);
    }

    private static int readInt(byte[] values, int offset) {
        return ((values[offset] & 0xFF) << 24)
                | ((values[offset + 1] & 0xFF) << 16)
                | ((values[offset + 2] & 0xFF) << 8)
                | (values[offset + 3] & 0xFF);
    }

    private static void writeInt(byte[] values, int offset, int value) {
        values[offset] = (byte) (value >>> 24);
        values[offset + 1] = (byte) (value >>> 16);
        values[offset + 2] = (byte) (value >>> 8);
        values[offset + 3] = (byte) value;
    }

    @FunctionalInterface
    private interface CompoundWriter {
        void write(DataOutputStream out) throws IOException;
    }

    private record LegacyTag(int type, Object value) {
    }
}
