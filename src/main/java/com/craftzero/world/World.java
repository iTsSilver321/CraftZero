package com.craftzero.world;

import com.craftzero.combat.DamageSource;
import com.craftzero.combat.ExplosionExposure;
import com.craftzero.graphics.Camera;
import com.craftzero.graphics.Frustum;
import com.craftzero.graphics.Mesh;
import com.craftzero.graphics.Renderer;
import com.craftzero.graphics.Texture;
import com.craftzero.entity.ArrowEntity;
import com.craftzero.entity.ChestMinecartEntity;
import com.craftzero.entity.DroppedItem;
import com.craftzero.entity.Entity;
import com.craftzero.entity.ExperienceOrbEntity;
import com.craftzero.entity.FireballEntity;
import com.craftzero.entity.FallingBlockEntity;
import com.craftzero.entity.FurnaceMinecartEntity;
import com.craftzero.entity.MinecartEntity;
import com.craftzero.entity.PrimedTntEntity;
import com.craftzero.entity.SplashPotionEntity;
import com.craftzero.inventory.ItemType;
import com.craftzero.inventory.ItemStack;
import com.craftzero.main.CombatRules;
import com.craftzero.math.Noise;
import com.craftzero.physics.AABB;
import com.craftzero.progression.PotionData;
import com.craftzero.save.SaveManager;
import com.craftzero.world.tile.BlockPos;
import com.craftzero.world.tile.ChestTileEntity;
import com.craftzero.world.tile.BrewingStandTileEntity;
import com.craftzero.world.tile.DispenserTileEntity;
import com.craftzero.world.tile.FurnaceTileEntity;
import com.craftzero.world.tile.JukeboxTileEntity;
import com.craftzero.world.tile.MonsterSpawnerTileEntity;
import com.craftzero.world.tile.NoteBlockTileEntity;
import com.craftzero.world.tile.SignTileEntity;
import com.craftzero.world.tile.TileEntity;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * World manager that handles chunk loading, generation, and rendering.
 * Implements procedural terrain generation with biomes.
 */
public class World implements GeneratedStructureSink {

    private static final int DEFAULT_RENDER_DISTANCE = 8; // Chunks in each direction
    private static final int MAX_RENDER_DISTANCE = 16;
    private static final int MAX_MESH_UPLOADS_PER_FRAME = 3; // GPU uploads are the biggest main-thread spike.
    private static final int MAX_GENERATES_PER_FRAME = 4; // Limit async terrain generation submits.
    private static final int MAX_LIGHTINGS_PER_FRAME = 4; // Limit async lighting submits.
    private static final int MAX_MESHES_PER_FRAME = 3; // Limit async mesh building submits.
    private static final int MAX_PENDING_CHUNK_WORK = 8;
    private static final int MAX_CHUNK_SHELL_STEPS_PER_FRAME = 96;
    private static final int MAX_BLOCK_UPDATES_PER_TICK = 1000;
    private static final float DROPPED_ITEM_MERGE_RADIUS_SQ = 2.25f;
    private static final float DROPPED_ITEM_PICKUP_SCAN_RADIUS_SQ = 9.0f;
    private static final float DROPPED_ITEM_PICKUP_DELAY = 0.5f;
    private static final int WATER_TICK_DELAY = 5;
    private static final int LAVA_TICK_DELAY = 30;
    private static final int FIRE_TICK_DELAY = 30;
    private static final int FALLING_BLOCK_TICK_DELAY = 3;
    public static final int END_PORTAL_FRAME_EYE_BIT = 4;
    private static final int SEA_LEVEL = 62;
    private static final int BASE_HEIGHT = 64;
    private static final int[][] HORIZONTAL_DIRS = {
            { 1, 0 }, { -1, 0 }, { 0, 1 }, { 0, -1 }
    };

    private final ConcurrentHashMap<Long, Chunk> chunks;
    private final Noise terrainNoise;
    private final Noise biomeNoise;
    private final Noise treeNoise;
    private final CaveGenerator caveGenerator;
    private final RavineGenerator ravineGenerator;
    private final OreGenerator oreGenerator;
    private final WorldGenerator worldGenerator;
    private final String generatorId;
    private final Dimension dimension;
    private final long seed;
    private final Random random;
    private final PriorityQueue<ScheduledBlockTick> scheduledBlockTicks;
    private final Set<ScheduledTickKey> scheduledTickKeys;
    private final Set<Long> dynamicTickScannedChunks;
    private long blockTickClock;
    private long nextBlockTickSequence;
    private float blockTickAccumulator;
    private boolean chunkShellReady;
    private int chunkShellCenterX;
    private int chunkShellCenterZ;
    private int chunkShellCursor;
    private int renderDistance = DEFAULT_RENDER_DISTANCE;
    private int unloadDistance = DEFAULT_RENDER_DISTANCE + 2;
    private boolean smoothLighting = true;
    private boolean fancyGraphics = true;
    private boolean advancedOpenGl;

    private Texture atlas;
    private SaveManager saveManager;
    private boolean suppressNeighborSupportUpdates;

    // Dropped items in the world
    private final List<DroppedItem> droppedItems;
    private final ConcurrentHashMap<BlockPos, TileEntity> tileEntities;
    private final ConcurrentHashMap<BlockPos, TileEntity> generatedTileEntities;

    // Living entities (mobs)
    private final List<Entity> entities;
    private final List<Entity> entitiesToAdd;
    private final List<Entity> entitiesToRemove;
    private final ConcurrentLinkedQueue<Entity> generatedEntities;

    // Reference to player (for AI targeting)
    private com.craftzero.main.Player player;

    // Day/night cycle manager reference
    private DayCycleManager dayCycleManager;

    // Async mesh building infrastructure
    private final ExecutorService meshBuildPool;
    private final Set<Long> chunksBeingBuilt; // Chunks currently being processed
    private final ConcurrentLinkedQueue<ChunkMeshTask> completedMeshTasks;

    // Frustum culling
    private final Frustum frustum;
    private final org.joml.Matrix4f viewProjection;
    private final List<Chunk> visibleRenderChunks;

    // Pre-calculated spiral loading order (center-out, sorted by distance)
    private static final int[][] SPIRAL_OFFSETS;
    static {
        // Generate offsets for render distance
        int maxDist = MAX_RENDER_DISTANCE + 2;
        java.util.List<int[]> offsets = new java.util.ArrayList<>();
        for (int dx = -maxDist; dx <= maxDist; dx++) {
            for (int dz = -maxDist; dz <= maxDist; dz++) {
                offsets.add(new int[] { dx, dz, dx * dx + dz * dz }); // Store distance squared
            }
        }
        // Sort by distance (closest first)
        offsets.sort((a, b) -> Integer.compare(a[2], b[2]));
        SPIRAL_OFFSETS = offsets.toArray(new int[0][]);
    }

    // Task class to hold chunk and its built mesh data
    private static class ChunkMeshTask {
        final Chunk chunk;
        final ChunkMeshData meshData;
        final long expectedVersion;
        final Chunk.ChunkState fallbackState;

        ChunkMeshTask(Chunk chunk, ChunkMeshData meshData, long expectedVersion, Chunk.ChunkState fallbackState) {
            this.chunk = chunk;
            this.meshData = meshData;
            this.expectedVersion = expectedVersion;
            this.fallbackState = fallbackState;
        }
    }

    private record ScheduledTickKey(int x, int y, int z, BlockType type) {
    }

    private record ScheduledBlockTick(long dueTick, long sequence, ScheduledTickKey key)
            implements Comparable<ScheduledBlockTick> {
        @Override
        public int compareTo(ScheduledBlockTick other) {
            int dueCompare = Long.compare(dueTick, other.dueTick);
            return dueCompare != 0 ? dueCompare : Long.compare(sequence, other.sequence);
        }
    }

    public record ChunkAreaProgress(int total, int generated, int lighted, int readyChunks) {
        public float progress() {
            if (total <= 0) {
                return 1.0f;
            }
            int weighted = generated + lighted + readyChunks;
            return Math.min(1.0f, weighted / (float) (total * 3));
        }

        public boolean isReady() {
            return total > 0 && readyChunks >= total;
        }
    }

    public World(long seed) {
        this(seed, WorldGenerator.RELEASE_ONE);
    }

    public World(long seed, String generatorId) {
        this(seed, generatorId, null);
    }

    public World(long seed, String generatorId, Dimension dimension) {
        this.seed = seed;
        String requestedGeneratorId = generatorId == null || generatorId.isBlank()
                ? WorldGenerator.RELEASE_ONE
                : generatorId;
        this.worldGenerator = WorldGenerators.create(requestedGeneratorId, seed, dimension);
        this.dimension = worldGenerator != null ? worldGenerator.getDimension() : Dimension.OVERWORLD;
        this.generatorId = worldGenerator != null ? worldGenerator.getId() : requestedGeneratorId;
        this.chunks = new ConcurrentHashMap<>();
        this.terrainNoise = new Noise(seed);
        this.biomeNoise = new Noise(seed + 1);
        this.treeNoise = new Noise(seed + 3);
        this.caveGenerator = new CaveGenerator();
        this.ravineGenerator = new RavineGenerator();
        this.oreGenerator = new OreGenerator();
        this.random = new Random(seed);
        this.scheduledBlockTicks = new PriorityQueue<>();
        this.scheduledTickKeys = ConcurrentHashMap.newKeySet();
        this.dynamicTickScannedChunks = ConcurrentHashMap.newKeySet();
        this.blockTickClock = 0;
        this.nextBlockTickSequence = 0;
        this.blockTickAccumulator = 0.0f;
        this.chunkShellReady = false;
        this.chunkShellCenterX = Integer.MIN_VALUE;
        this.chunkShellCenterZ = Integer.MIN_VALUE;
        this.chunkShellCursor = 0;
        this.droppedItems = new ArrayList<>();
        this.tileEntities = new ConcurrentHashMap<>();
        this.generatedTileEntities = new ConcurrentHashMap<>();
        this.entities = new ArrayList<>();
        this.entitiesToAdd = new ArrayList<>();
        this.entitiesToRemove = new ArrayList<>();
        this.generatedEntities = new ConcurrentLinkedQueue<>();

        int workerCount = Math.max(2, Math.min(4, Runtime.getRuntime().availableProcessors() - 1));
        this.meshBuildPool = Executors.newFixedThreadPool(workerCount, r -> {
            Thread t = new Thread(r, "ChunkMeshBuilder");
            t.setDaemon(true);
            return t;
        });
        this.chunksBeingBuilt = ConcurrentHashMap.newKeySet();
        this.completedMeshTasks = new ConcurrentLinkedQueue<>();

        // Frustum culling
        this.frustum = new Frustum();
        this.viewProjection = new org.joml.Matrix4f();
        this.visibleRenderChunks = new ArrayList<>();
    }

    public void init() throws Exception {
        atlas = new Texture("/textures/terrain/Terrain.png");
        System.out.println("World initialized with seed: " + seed);
    }

    public void setRenderDistanceChunks(int chunks) {
        int clamped = Math.max(2, Math.min(MAX_RENDER_DISTANCE, chunks));
        if (clamped == renderDistance) {
            return;
        }
        renderDistance = clamped;
        unloadDistance = renderDistance + 2;
        chunkShellReady = false;
        chunkShellCursor = 0;
    }

    public int getRenderDistanceChunks() {
        return renderDistance;
    }

    public void setSmoothLighting(boolean smoothLighting) {
        if (this.smoothLighting == smoothLighting) {
            return;
        }
        this.smoothLighting = smoothLighting;
        ChunkMeshBuilder.setSmoothLightingEnabled(smoothLighting);
        for (Chunk chunk : chunks.values()) {
            chunk.setDirty(true);
        }
    }

    public void setFancyGraphics(boolean fancyGraphics) {
        if (this.fancyGraphics == fancyGraphics) {
            return;
        }
        this.fancyGraphics = fancyGraphics;
        BlockType.setFancyGraphics(fancyGraphics);
        for (Chunk chunk : chunks.values()) {
            chunk.setDirty(true);
        }
    }

    public void setAdvancedOpenGl(boolean advancedOpenGl) {
        this.advancedOpenGl = advancedOpenGl;
    }

    public int regenerateUnmodifiedChunksAround(float worldX, float worldZ, int radiusChunks) {
        int radius = Math.max(0, Math.min(4, radiusChunks));
        int centerChunkX = (int) Math.floor(worldX / Chunk.WIDTH);
        int centerChunkZ = (int) Math.floor(worldZ / Chunk.DEPTH);
        int regenerated = 0;
        for (int dz = -radius; dz <= radius; dz++) {
            for (int dx = -radius; dx <= radius; dx++) {
                int chunkX = centerChunkX + dx;
                int chunkZ = centerChunkZ + dz;
                long key = chunkKey(chunkX, chunkZ);
                Chunk existing = chunks.get(key);
                if (existing != null && existing.isModified()) {
                    continue;
                }
                Chunk chunk = existing == null ? createChunk(chunkX, chunkZ) : existing;
                if (existing == null) {
                    chunks.put(key, chunk);
                }
                chunksBeingBuilt.remove(key);
                chunk.cleanup();
                chunk.setState(Chunk.ChunkState.GENERATING);
                generateChunkTerrain(chunk, chunkX, chunkZ);
                chunk.setState(Chunk.ChunkState.GENERATED);
                reconcileTileEntitiesInChunk(chunk);
                scheduleTickableBlocksInChunk(chunk);
                if (chunk.hasAllNeighborsAtLeast(Chunk.ChunkState.GENERATED)) {
                    chunk.calculateSkyLight();
                    chunk.setState(Chunk.ChunkState.LIGHTED);
                }
                chunk.setDirty(true);
                regenerated++;
                refreshChunkAndNeighbors(chunkX, chunkZ);
            }
        }
        return regenerated;
    }

    public ReleaseOneWorldGenerator.SpawnPoint findSafeSpawn() {
        if (worldGenerator instanceof ReleaseOneWorldGenerator releaseOne) {
            return releaseOne.findSafeSpawn();
        }
        return new ReleaseOneWorldGenerator.SpawnPoint(0, 80, 0);
    }

    public void setSaveManager(SaveManager saveManager) {
        this.saveManager = saveManager;
    }

    /**
     * Generate a unique key for chunk coordinates.
     */
    public static long chunkKey(int chunkX, int chunkZ) {
        return ((long) chunkX << 32) | (chunkZ & 0xFFFFFFFFL);
    }

    /**
     * Get chunk at the specified coordinates, creating if needed.
     * Does NOT generate terrain immediately - that happens async in update().
     */
    public Chunk getChunk(int chunkX, int chunkZ) {
        long key = chunkKey(chunkX, chunkZ);
        Chunk chunk = chunks.get(key);

        if (chunk == null) {
            chunk = createChunk(chunkX, chunkZ);
            chunks.put(key, chunk);
            scanCreatedChunkForDynamicTicks(key, chunk);
        }

        return chunk;
    }

    /**
     * Get chunk with immediate terrain generation.
     * Used for block queries/edits that need data NOW.
     */
    public Chunk getChunkNow(int chunkX, int chunkZ) {
        long key = chunkKey(chunkX, chunkZ);
        Chunk chunk = chunks.get(key);

        if (chunk == null) {
            chunk = createChunk(chunkX, chunkZ);
            chunks.put(key, chunk);
            scanCreatedChunkForDynamicTicks(key, chunk);
        }

        // Force synchronous generation if needed
        if (chunk.getState() == Chunk.ChunkState.EMPTY) {
            chunk.setState(Chunk.ChunkState.GENERATING);
            generateChunkTerrain(chunk, chunkX, chunkZ);
            chunk.setState(Chunk.ChunkState.GENERATED);
            reconcileTileEntitiesInChunk(chunk);
        }

        return chunk;
    }

    private void scanCreatedChunkForDynamicTicks(long key, Chunk chunk) {
        if (chunk.getState().ordinal() >= Chunk.ChunkState.GENERATED.ordinal() && dynamicTickScannedChunks.add(key)) {
            scheduleTickableBlocksInChunk(chunk);
        }
    }

    public ChunkAreaProgress getChunkAreaProgress(float worldX, float worldZ, int radius) {
        int centerChunkX = (int) Math.floor(worldX / Chunk.WIDTH);
        int centerChunkZ = (int) Math.floor(worldZ / Chunk.DEPTH);
        int total = 0;
        int generated = 0;
        int lighted = 0;
        int ready = 0;
        for (int dz = -radius; dz <= radius; dz++) {
            for (int dx = -radius; dx <= radius; dx++) {
                total++;
                Chunk chunk = chunks.get(chunkKey(centerChunkX + dx, centerChunkZ + dz));
                if (chunk == null) {
                    continue;
                }
                Chunk.ChunkState state = chunk.getState();
                if (state.ordinal() >= Chunk.ChunkState.GENERATED.ordinal()) {
                    generated++;
                }
                if (state.ordinal() >= Chunk.ChunkState.LIGHTED.ordinal()) {
                    lighted++;
                }
                if (state == Chunk.ChunkState.READY && !chunk.isEmpty()) {
                    ready++;
                }
            }
        }
        return new ChunkAreaProgress(total, generated, lighted, ready);
    }

    private Chunk createChunk(int chunkX, int chunkZ) {
        Chunk chunk = new Chunk(chunkX, chunkZ);
        if (saveManager != null && saveManager.loadChunkIfExists(chunk, dimension)) {
            chunk.setState(Chunk.ChunkState.GENERATED);
            reconcileTileEntitiesInChunk(chunk);
        }
        return chunk;
    }

    private void generateChunkTerrain(Chunk chunk, int chunkX, int chunkZ) {
        if (worldGenerator != null) {
            worldGenerator.generateChunk(this, chunk, chunkX, chunkZ);
            return;
        }

        // Renamed from generateChunk to generateChunkTerrain

        int worldX = chunkX * Chunk.WIDTH;
        int worldZ = chunkZ * Chunk.DEPTH;

        // Pass 1: Local Terrain (No dependency on neighbors)
        for (int x = 0; x < Chunk.WIDTH; x++) {
            for (int z = 0; z < Chunk.DEPTH; z++) {
                int globalX = worldX + x;
                int globalZ = worldZ + z;

                // Get biome value
                double biomeValue = biomeNoise.octaveNoise2D(globalX * 0.005, globalZ * 0.005, 4, 0.5);
                int height = calculateHeight(globalX, globalZ, biomeValue);
                BiomeType biome = getBiome(biomeValue);

                for (int y = 0; y < Chunk.HEIGHT; y++) {
                    BlockType blockType = getBlockType(y, height, biome, globalX, globalZ);
                    // Cave generation

                    chunk.setBlock(x, y, z, blockType);
                }
            }
        }

        // Pass 1.5: Caves (Worm/Runner based)
        caveGenerator.generate(chunk, seed);

        // Pass 1.6: Ravines (Vertical Cracks)
        ravineGenerator.generate(chunk, seed);

        // Pass 1.7: Ores (Replace stone with ore clusters)
        oreGenerator.generate(chunk, seed);

        // Pass 2: Trees within this chunk
        for (int x = 0; x < Chunk.WIDTH; x++) {
            for (int z = 0; z < Chunk.DEPTH; z++) {
                int globalX = worldX + x;
                int globalZ = worldZ + z;
                generateTreeIfPresent(chunk, globalX, globalZ);
            }
        }

        // Pass 3: Check for trees in neighboring chunks (within 2 blocks of border)
        // that would have leaves extending into this chunk
        int leafRadius = 2; // Max leaf radius
        for (int x = -leafRadius; x < Chunk.WIDTH + leafRadius; x++) {
            for (int z = -leafRadius; z < Chunk.DEPTH + leafRadius; z++) {
                // Skip positions inside this chunk (already handled)
                if (x >= 0 && x < Chunk.WIDTH && z >= 0 && z < Chunk.DEPTH)
                    continue;

                int globalX = worldX + x;
                int globalZ = worldZ + z;
                generateTreeIfPresent(chunk, globalX, globalZ);
            }
        }

        chunk.clearModified();
    }

    /**
     * Check if a tree exists at this global position and generate its parts in the
     * given chunk.
     */
    private void generateTreeIfPresent(Chunk chunk, int globalX, int globalZ) {
        double biomeValue = biomeNoise.octaveNoise2D(globalX * 0.005, globalZ * 0.005, 4, 0.5);
        int height = calculateHeight(globalX, globalZ, biomeValue);
        BiomeType biome = getBiome(biomeValue);

        // Trees only spawn in forest biome, above sea level, and on grass (not beach
        // sand)
        // Beach is height <= SEA_LEVEL + 2, so we need height > SEA_LEVEL + 2 for grass
        if (biome == BiomeType.FOREST && height > SEA_LEVEL + 2) {
            double treeValue = treeNoise.noise2D(globalX * 0.5, globalZ * 0.5);
            if (treeValue > 0.7 && isLocalMaximum(globalX, globalZ, 8)) {
                // Deterministic height based on position
                int trunkHeight = 5 + getPositionHash(globalX, globalZ) % 3;
                generateTreeAtPosition(chunk, globalX, height + 1, globalZ, trunkHeight);
            }
        }
    }

    /**
     * Generate a deterministic hash for a position (for random-like but
     * reproducible values).
     */
    private int getPositionHash(int x, int z) {
        long hash = seed ^ (x * 73856093L) ^ (z * 19349663L);
        hash = hash ^ (hash >>> 16);
        return Math.abs((int) hash);
    }

    /**
     * Checks if the noise value at (x,z) is higher than all neighbors in radius.
     */
    private boolean isLocalMaximum(int x, int z, int radius) {
        double centerValue = treeNoise.noise2D(x * 0.5, z * 0.5);
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                if (dx == 0 && dz == 0)
                    continue;
                double neighborValue = treeNoise.noise2D((x + dx) * 0.5, (z + dz) * 0.5);
                if (neighborValue >= centerValue) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Generate tree parts at position (only places blocks within the target chunk).
     */
    private void generateTreeAtPosition(Chunk chunk, int x, int y, int z, int trunkHeight) {
        // x, z are GLOBAL coordinates

        // Trunk - only in this chunk's bounds
        for (int i = 0; i < trunkHeight; i++) {
            setBlockInChunk(chunk, x, y + i, z, BlockType.OAK_LOG);
        }

        // Leaves - Fuller & Rounded pattern (only within chunk bounds)
        int h = trunkHeight;

        // Loop: Bottom (h-2), Middle (h-1), Top (h), Peak (h+1)
        for (int ly = y + h - 2; ly <= y + h + 1; ly++) {
            int dy = ly - (y + h);
            int radius;

            // Shape logic - MC Oak style
            if (dy == 1)
                radius = 1; // Peak (Cross)
            else if (dy == 0)
                radius = 1; // Top body (3x3)
            else
                radius = 2; // Bottom body (5x5)

            for (int lx = x - radius; lx <= x + radius; lx++) {
                for (int lz = z - radius; lz <= z + radius; lz++) {
                    int dx = Math.abs(lx - x);
                    int dz = Math.abs(lz - z);
                    boolean place = false;

                    if (dy == 1) { // Peak: Cross
                        if (dx + dz <= 1)
                            place = true;
                    } else if (dy == 0) { // Top: 3x3
                        if (dx <= 1 && dz <= 1)
                            place = true;
                    } else { // Bottom: 5x5 minus corners
                        if (!(dx == 2 && dz == 2))
                            place = true;
                    }

                    if (!place)
                        continue;

                    // Only place if within chunk AND not overwriting trunk
                    int chunkWorldX = chunk.getChunkX() * Chunk.WIDTH;
                    int chunkWorldZ = chunk.getChunkZ() * Chunk.DEPTH;
                    int localLx = lx - chunkWorldX;
                    int localLz = lz - chunkWorldZ;

                    if (localLx >= 0 && localLx < Chunk.WIDTH && localLz >= 0 && localLz < Chunk.DEPTH) {
                        if (chunk.getBlock(localLx, ly, localLz) != BlockType.OAK_LOG) {
                            chunk.setBlock(localLx, ly, localLz, BlockType.LEAVES);
                        }
                    }
                }
            }
        }
    }

    /**
     * Calculate terrain height using noise.
     */
    private int calculateHeight(int x, int z, double biomeValue) {
        // Ensure safe spawn point (small platform)
        if (Math.abs(x) < 4 && Math.abs(z) < 4) {
            return 64;
        }

        // Base terrain
        double baseNoise = terrainNoise.octaveNoise2D(x * 0.01, z * 0.01, 4, 0.5);

        // Map biome value [-1, 1] to amplitude factor [8, 40]
        // Smooth interpolation to prevent terrain tears
        double t = (biomeValue + 1.0) * 0.5; // [0, 1]
        t = Math.max(0, Math.min(1, t)); // Clamp

        // Smoothstep for nicer transitions
        t = t * t * (3 - 2 * t);

        double lowAmp = 8.0; // Plains
        double highAmp = 40.0; // Mountains

        double amplitude = lowAmp + t * (highAmp - lowAmp);

        if (t > 0.7) {
            // Add extra roughness for mountains
            baseNoise += terrainNoise.octaveNoise2D(x * 0.02, z * 0.02, 2, 0.5) * 0.5;
        }

        int height = (int) (BASE_HEIGHT + baseNoise * amplitude);

        // Clamp height
        return Math.max(1, Math.min(height, Chunk.HEIGHT - 1));
    }

    /**
     * Determine biome from noise value.
     */
    private BiomeType getBiome(double biomeValue) {
        if (biomeValue < -0.3) {
            return BiomeType.PLAINS;
        } else if (biomeValue < 0.15) {
            return BiomeType.FOREST;
        } else if (biomeValue < 0.45) {
            return BiomeType.HILLS;
        } else {
            return BiomeType.MOUNTAINS;
        }
    }

    /**
     * Get block type based on height and biome.
     */
    private BlockType getBlockType(int y, int height, BiomeType biome, int x, int z) {
        // Bedrock at bottom
        if (y == 0) {
            return BlockType.BEDROCK;
        }

        // Water below sea level
        if (y <= SEA_LEVEL && y > height) {
            return BlockType.WATER;
        }

        // Above ground
        if (y > height) {
            return BlockType.AIR;
        }

        // Surface blocks based on biome
        if (y == height) {
            if (height <= SEA_LEVEL + 2) {
                return BlockType.SAND; // Beach
            }
            switch (biome) {
                case MOUNTAINS:
                    return height > 90 ? BlockType.SNOW : BlockType.STONE;
                case PLAINS:
                case FOREST:
                case HILLS:
                default:
                    return BlockType.GRASS;
            }
        }

        // Near surface
        if (y > height - 4) {
            if (height <= SEA_LEVEL + 2) {
                return BlockType.SAND;
            }
            return BlockType.DIRT;
        }

        if (y < height - 4) {
            // Ores are generated exclusively by OreGenerator's vein pass.
            return BlockType.STONE;
        }

        return BlockType.STONE;
    }

    /**
     * Update chunks around the player.
     * Uses staged loading: EMPTY → GENERATED → LIGHTED → READY
     * Chunks only mesh when all neighbors are LIGHTED to prevent border artifacts.
     */
    public void update(Camera camera) {
        int playerChunkX = (int) Math.floor(camera.getPosition().x / Chunk.WIDTH);
        int playerChunkZ = (int) Math.floor(camera.getPosition().z / Chunk.DEPTH);

        // Step 1: Process completed mesh build tasks (rate limited)
        int uploadsThisFrame = 0;
        while (uploadsThisFrame < MAX_MESH_UPLOADS_PER_FRAME) {
            ChunkMeshTask task = completedMeshTasks.poll();
            if (task == null)
                break;

            long key = chunkKey(task.chunk.getChunkX(), task.chunk.getChunkZ());
            if (chunks.get(key) != task.chunk || task.chunk.getState() != Chunk.ChunkState.MESHING) {
                chunksBeingBuilt.remove(key);
                continue;
            }
            if (task.chunk.getModificationVersion() != task.expectedVersion) {
                task.chunk.setState(task.fallbackState);
                task.chunk.setDirty(true);
                chunksBeingBuilt.remove(key);
                continue;
            }

            // Apply mesh data on main thread (GPU upload)
            task.chunk.applyMeshData(task.meshData);
            task.chunk.setState(Chunk.ChunkState.READY);
            chunksBeingBuilt.remove(key);
            uploadsThisFrame++;
        }

        ensureChunkShell(playerChunkX, playerChunkZ);

        // Push already-generated nearby chunks toward visibility before spending
        // worker capacity on farther empty terrain.
        progressPriorityMeshableChunks(playerChunkX, playerChunkZ);
        progressPriorityLightingChunks(playerChunkX, playerChunkZ);

        // Step 3: Progress chunks through states (with per-frame rate limiting)
        // Uses spiral order: closest chunks to player are processed first
        int generationsThisFrame = 0;
        int lightingsThisFrame = 0;
        int meshesThisFrame = 0;

        for (int[] offset : SPIRAL_OFFSETS) {
            if (generationsThisFrame >= MAX_GENERATES_PER_FRAME
                    && lightingsThisFrame >= MAX_LIGHTINGS_PER_FRAME
                    && meshesThisFrame >= MAX_MESHES_PER_FRAME) {
                break;
            }

            int dx = offset[0];
            int dz = offset[1];

            // Skip if outside render distance
            if (Math.abs(dx) > renderDistance || Math.abs(dz) > renderDistance)
                continue;

            int chunkX = playerChunkX + dx;
            int chunkZ = playerChunkZ + dz;
            long key = chunkKey(chunkX, chunkZ);
            Chunk chunk = chunks.get(key);

            if (chunk == null)
                continue;

            Chunk.ChunkState state = chunk.getState();
            // EMPTY → Submit for terrain generation (rate limited)
            if (state == Chunk.ChunkState.EMPTY && !chunksBeingBuilt.contains(key)) {
                if (generationsThisFrame >= MAX_GENERATES_PER_FRAME)
                    continue;
                if (chunksBeingBuilt.size() >= MAX_PENDING_CHUNK_WORK)
                    continue;
                generationsThisFrame++;

                chunk.setState(Chunk.ChunkState.GENERATING);
                chunksBeingBuilt.add(key);
                final Chunk chunkRef = chunk;
                final int cx = chunkX, cz = chunkZ;
                meshBuildPool.submit(() -> {
                    try {
                        generateChunkTerrain(chunkRef, cx, cz);
                        chunkRef.setState(Chunk.ChunkState.GENERATED);
                    } catch (Exception e) {
                        System.err.println("Error generating chunk: " + e.getMessage());
                    } finally {
                        chunksBeingBuilt.remove(chunkKey(cx, cz));
                    }
                });
            }

            // GENERATED → Submit for lighting (rate limited, ASYNC!)
            else if (state == Chunk.ChunkState.GENERATED && !chunksBeingBuilt.contains(key)) {
                if (dynamicTickScannedChunks.add(key)) {
                    reconcileTileEntitiesInChunk(chunk);
                    scheduleTickableBlocksInChunk(chunk);
                }
                if (chunk.hasAllNeighborsAtLeast(Chunk.ChunkState.GENERATED)) {
                    if (lightingsThisFrame >= MAX_LIGHTINGS_PER_FRAME)
                        continue;
                    if (chunksBeingBuilt.size() >= MAX_PENDING_CHUNK_WORK)
                        continue;
                    lightingsThisFrame++;

                    chunk.setState(Chunk.ChunkState.LIGHTING);
                    chunksBeingBuilt.add(key);
                    final Chunk chunkRef = chunk;
                    final long chunkKey = key;
                    meshBuildPool.submit(() -> {
                        try {
                            chunkRef.calculateSkyLight();
                            chunkRef.setState(Chunk.ChunkState.LIGHTED);
                        } catch (Exception e) {
                            System.err.println("Error lighting chunk: " + e.getMessage());
                            chunkRef.setState(Chunk.ChunkState.GENERATED);
                        } finally {
                            chunksBeingBuilt.remove(chunkKey);
                        }
                    });
                }
            }

            // LIGHTED → Submit for mesh building (rate limited)
            else if (state == Chunk.ChunkState.LIGHTED && !chunksBeingBuilt.contains(key)) {
                if (chunk.hasAllNeighborsAtLeast(Chunk.ChunkState.LIGHTED)) {
                    if (meshesThisFrame >= MAX_MESHES_PER_FRAME)
                        continue;
                    if (chunksBeingBuilt.size() >= MAX_PENDING_CHUNK_WORK)
                        continue;
                    meshesThisFrame++;

                    chunk.setState(Chunk.ChunkState.MESHING);
                    chunksBeingBuilt.add(key);
                    final Chunk chunkRef = chunk;
                    final long expectedVersion = chunkRef.getModificationVersion();
                    meshBuildPool.submit(() -> {
                        try {
                            ChunkMeshData meshData = ChunkMeshBuilder.buildMeshData(chunkRef);
                            completedMeshTasks.offer(new ChunkMeshTask(chunkRef, meshData, expectedVersion,
                                    Chunk.ChunkState.LIGHTED));
                        } catch (Exception e) {
                            System.err.println("Error building chunk mesh: " + e.getMessage());
                            chunkRef.setState(Chunk.ChunkState.LIGHTED);
                            chunksBeingBuilt.remove(chunkKey(chunkRef.getChunkX(), chunkRef.getChunkZ()));
                        }
                    });
                }
            }

            // READY but dirty (block placed/removed) → Rebuild mesh
            else if (state == Chunk.ChunkState.READY && chunk.isDirty() && !chunksBeingBuilt.contains(key)) {
                if (chunk.hasAllNeighborsAtLeast(Chunk.ChunkState.LIGHTED)) {
                    if (meshesThisFrame >= MAX_MESHES_PER_FRAME)
                        continue;
                    if (chunksBeingBuilt.size() >= MAX_PENDING_CHUNK_WORK)
                        continue;
                    meshesThisFrame++;

                    chunk.setState(Chunk.ChunkState.MESHING);
                    chunksBeingBuilt.add(key);
                    final Chunk chunkRef = chunk;
                    final long expectedVersion = chunkRef.getModificationVersion();
                    meshBuildPool.submit(() -> {
                        try {
                            chunkRef.calculateSkyLight();
                            ChunkMeshData meshData = ChunkMeshBuilder.buildMeshData(chunkRef);
                            completedMeshTasks.offer(new ChunkMeshTask(chunkRef, meshData, expectedVersion,
                                    Chunk.ChunkState.READY));
                        } catch (Exception e) {
                            System.err.println("Error rebuilding chunk mesh: " + e.getMessage());
                            chunkRef.setState(Chunk.ChunkState.READY);
                            chunksBeingBuilt.remove(chunkKey(chunkRef.getChunkX(), chunkRef.getChunkZ()));
                        }
                    });
                }
            }
        }

        // Step 4: Unload distant chunks
        List<Long> toUnload = new ArrayList<>();
        for (Map.Entry<Long, Chunk> entry : chunks.entrySet()) {
            Chunk chunk = entry.getValue();
            int dx = chunk.getChunkX() - playerChunkX;
            int dz = chunk.getChunkZ() - playerChunkZ;

            if (Math.abs(dx) > unloadDistance || Math.abs(dz) > unloadDistance) {
                toUnload.add(entry.getKey());
            }
        }

        for (Long key : toUnload) {
            Chunk chunk = chunks.get(key);
            if (chunk != null && chunk.isModified() && saveManager != null) {
                try {
                    saveManager.saveModifiedChunk(chunk, dimension);
                } catch (Exception e) {
                    System.err.println("Failed to flush modified chunk before unload "
                            + chunk.getChunkX() + "," + chunk.getChunkZ() + ": " + e.getMessage());
                    continue;
                }
            }
            chunk = chunks.remove(key);
            if (chunk != null) {
                removeScheduledTicksForChunk(chunk);
                chunk.cleanup();
            }
            chunksBeingBuilt.remove(key);
            dynamicTickScannedChunks.remove(key);
        }
    }

    private void progressPriorityLightingChunks(int playerChunkX, int playerChunkZ) {
        int lightingsThisFrame = 0;
        for (int[] offset : SPIRAL_OFFSETS) {
            if (lightingsThisFrame >= MAX_LIGHTINGS_PER_FRAME
                    || chunksBeingBuilt.size() >= MAX_PENDING_CHUNK_WORK) {
                return;
            }
            int dx = offset[0];
            int dz = offset[1];
            if (Math.abs(dx) > renderDistance || Math.abs(dz) > renderDistance) {
                continue;
            }
            int chunkX = playerChunkX + dx;
            int chunkZ = playerChunkZ + dz;
            long key = chunkKey(chunkX, chunkZ);
            Chunk chunk = chunks.get(key);
            if (chunk == null || chunk.getState() != Chunk.ChunkState.GENERATED
                    || chunksBeingBuilt.contains(key)) {
                continue;
            }
            if (dynamicTickScannedChunks.add(key)) {
                reconcileTileEntitiesInChunk(chunk);
                scheduleTickableBlocksInChunk(chunk);
            }
            if (!chunk.hasAllNeighborsAtLeast(Chunk.ChunkState.GENERATED)) {
                continue;
            }

            lightingsThisFrame++;
            chunk.setState(Chunk.ChunkState.LIGHTING);
            chunksBeingBuilt.add(key);
            final Chunk chunkRef = chunk;
            final long chunkKey = key;
            meshBuildPool.submit(() -> {
                try {
                    chunkRef.calculateSkyLight();
                    chunkRef.setState(Chunk.ChunkState.LIGHTED);
                } catch (Exception e) {
                    System.err.println("Error lighting chunk: " + e.getMessage());
                    chunkRef.setState(Chunk.ChunkState.GENERATED);
                } finally {
                    chunksBeingBuilt.remove(chunkKey);
                }
            });
        }
    }

    private void progressPriorityMeshableChunks(int playerChunkX, int playerChunkZ) {
        int meshesThisFrame = 0;
        for (int[] offset : SPIRAL_OFFSETS) {
            if (meshesThisFrame >= MAX_MESHES_PER_FRAME
                    || chunksBeingBuilt.size() >= MAX_PENDING_CHUNK_WORK) {
                return;
            }
            int dx = offset[0];
            int dz = offset[1];
            if (Math.abs(dx) > renderDistance || Math.abs(dz) > renderDistance) {
                continue;
            }
            int chunkX = playerChunkX + dx;
            int chunkZ = playerChunkZ + dz;
            long key = chunkKey(chunkX, chunkZ);
            Chunk chunk = chunks.get(key);
            if (chunk == null || chunksBeingBuilt.contains(key)) {
                continue;
            }

            Chunk.ChunkState state = chunk.getState();
            boolean dirtyReady = state == Chunk.ChunkState.READY && chunk.isDirty();
            boolean initialMesh = state == Chunk.ChunkState.LIGHTED;
            if ((!dirtyReady && !initialMesh) || !chunk.hasAllNeighborsAtLeast(Chunk.ChunkState.LIGHTED)) {
                continue;
            }

            meshesThisFrame++;
            chunk.setState(Chunk.ChunkState.MESHING);
            chunksBeingBuilt.add(key);
            final Chunk chunkRef = chunk;
            final Chunk.ChunkState fallbackState = dirtyReady ? Chunk.ChunkState.READY : Chunk.ChunkState.LIGHTED;
            final long expectedVersion = chunkRef.getModificationVersion();
            meshBuildPool.submit(() -> {
                try {
                    if (dirtyReady) {
                        chunkRef.calculateSkyLight();
                    }
                    ChunkMeshData meshData = ChunkMeshBuilder.buildMeshData(chunkRef);
                    completedMeshTasks.offer(new ChunkMeshTask(chunkRef, meshData, expectedVersion, fallbackState));
                } catch (Exception e) {
                    System.err.println("Error building chunk mesh: " + e.getMessage());
                    chunkRef.setState(fallbackState);
                    chunksBeingBuilt.remove(chunkKey(chunkRef.getChunkX(), chunkRef.getChunkZ()));
                }
            });
        }
    }

    private void ensureChunkShell(int playerChunkX, int playerChunkZ) {
        if (playerChunkX != chunkShellCenterX || playerChunkZ != chunkShellCenterZ) {
            chunkShellReady = false;
            chunkShellCenterX = playerChunkX;
            chunkShellCenterZ = playerChunkZ;
            chunkShellCursor = 0;
        }
        if (chunkShellReady) {
            return;
        }

        int steps = 0;
        int shellRadius = renderDistance + 1;
        while (chunkShellCursor < SPIRAL_OFFSETS.length && steps < MAX_CHUNK_SHELL_STEPS_PER_FRAME) {
            int[] offset = SPIRAL_OFFSETS[chunkShellCursor++];
            int dx = offset[0];
            int dz = offset[1];
            if (Math.abs(dx) > shellRadius || Math.abs(dz) > shellRadius) {
                continue;
            }
            steps++;

            int chunkX = playerChunkX + dx;
            int chunkZ = playerChunkZ + dz;
            long key = chunkKey(chunkX, chunkZ);
            if (!chunks.containsKey(key)) {
                chunks.put(key, createChunk(chunkX, chunkZ));
            }
            refreshChunkAndNeighbors(chunkX, chunkZ);
        }

        if (chunkShellCursor >= SPIRAL_OFFSETS.length) {
            chunkShellReady = true;
            chunkShellCursor = 0;
        }
    }

    private void refreshChunkAndNeighbors(int chunkX, int chunkZ) {
        refreshChunkNeighbors(chunkX, chunkZ);
        refreshChunkNeighbors(chunkX, chunkZ - 1);
        refreshChunkNeighbors(chunkX, chunkZ + 1);
        refreshChunkNeighbors(chunkX + 1, chunkZ);
        refreshChunkNeighbors(chunkX - 1, chunkZ);
    }

    private void refreshChunkNeighbors(int chunkX, int chunkZ) {
        Chunk chunk = chunks.get(chunkKey(chunkX, chunkZ));
        if (chunk == null) {
            return;
        }
        chunk.setNeighbors(
                chunks.get(chunkKey(chunkX, chunkZ - 1)),
                chunks.get(chunkKey(chunkX, chunkZ + 1)),
                chunks.get(chunkKey(chunkX + 1, chunkZ)),
                chunks.get(chunkKey(chunkX - 1, chunkZ)));
    }

    /**
     * Render all visible chunks with frustum culling.
     */
    /**
     * Render world chunks.
     * 
     * @param midPassAction Optional action to run between Opaque and Transparent
     *                      passes (e.g. Block Highlight).
     */
    public void render(Renderer renderer, Camera camera, Runnable midPassAction) {
        int playerChunkX = (int) Math.floor(camera.getPosition().x / Chunk.WIDTH);
        int playerChunkZ = (int) Math.floor(camera.getPosition().z / Chunk.DEPTH);

        // Update frustum for culling
        viewProjection.set(camera.getProjectionMatrix()).mul(camera.getViewMatrix());
        frustum.update(viewProjection);

        atlas.bind(0);

        visibleRenderChunks.clear();
        boolean hasCutout = false;
        boolean hasTranslucent = false;
        for (int dx = -renderDistance; dx <= renderDistance; dx++) {
            for (int dz = -renderDistance; dz <= renderDistance; dz++) {
                if (advancedOpenGl && dx * dx + dz * dz > renderDistance * renderDistance) {
                    continue;
                }
                int chunkX = playerChunkX + dx;
                int chunkZ = playerChunkZ + dz;
                int worldX = chunkX * Chunk.WIDTH;
                int worldZ = chunkZ * Chunk.DEPTH;
                if (!frustum.isChunkVisible(worldX, worldZ)) {
                    continue;
                }

                Chunk chunk = chunks.get(chunkKey(chunkX, chunkZ));
                if (chunk == null || chunk.isEmpty()) {
                    continue;
                }

                visibleRenderChunks.add(chunk);
                hasCutout |= chunk.getCutoutMesh() != null;
                hasTranslucent |= chunk.getTransparentMesh() != null;
            }
        }

        // --- PASS 1: OPAQUE ---
        renderer.beginRender(camera);
        renderer.useIdentityModelMatrix();

        // Explicitly ensure solid rendering state for opaque pass
        org.lwjgl.opengl.GL11.glEnable(org.lwjgl.opengl.GL11.GL_DEPTH_TEST);
        org.lwjgl.opengl.GL11.glDepthMask(true);
        org.lwjgl.opengl.GL11.glDisable(org.lwjgl.opengl.GL11.GL_BLEND);
        org.lwjgl.opengl.GL11.glEnable(org.lwjgl.opengl.GL11.GL_CULL_FACE);
        renderer.setAlphaCutoff(0.0f);

        for (Chunk chunk : visibleRenderChunks) {
            Mesh mesh = chunk.getMesh();
            if (mesh != null) {
                renderer.renderPreparedMesh(mesh);
            }
        }

        // --- PASS 2: CUTOUT ---
        // Alpha-tested blocks such as leaves must write depth so clouds/water do not
        // show through their solid pixels.
        if (hasCutout) {
            renderer.setAlphaCutoff(0.1f);

            for (Chunk chunk : visibleRenderChunks) {
                Mesh mesh = chunk.getCutoutMesh();
                if (mesh != null) {
                    renderer.renderPreparedMesh(mesh);
                }
            }
            renderer.setAlphaCutoff(0.0f);
        }
        renderer.endPreparedMeshBatch();
        renderer.endRender(); // End Opaque/Cutout Pass

        // --- MID-PASS ACTION (Highlight) ---
        if (midPassAction != null) {
            midPassAction.run();
        }

        // --- PASS 3: TRANSLUCENT ---
        if (hasTranslucent) {
            // Re-bind atlas in case midPassAction unbound it (fixes black water bug)
            atlas.bind(0);

            renderer.beginRender(camera);
            renderer.useIdentityModelMatrix();

            // CRITICAL: Re-enable blending for water/glass (BlockHighlight might have
            // disabled it)
            org.lwjgl.opengl.GL11.glEnable(org.lwjgl.opengl.GL11.GL_BLEND);
            org.lwjgl.opengl.GL11.glBlendFunc(org.lwjgl.opengl.GL11.GL_SRC_ALPHA,
                    org.lwjgl.opengl.GL11.GL_ONE_MINUS_SRC_ALPHA);

            renderer.setAlphaCutoff(0.0f);
            renderer.setDepthMask(false); // Disable depth writing
            org.lwjgl.opengl.GL11.glDisable(org.lwjgl.opengl.GL11.GL_CULL_FACE);

            visibleRenderChunks.sort((a, b) -> Float.compare(
                    chunkDistanceSquared(b, camera),
                    chunkDistanceSquared(a, camera)));
            for (Chunk chunk : visibleRenderChunks) {
                Mesh mesh = chunk.getTransparentMesh();
                if (mesh != null) {
                    renderer.renderPreparedMesh(mesh);
                }
            }

            renderer.endPreparedMeshBatch();
            org.lwjgl.opengl.GL11.glEnable(org.lwjgl.opengl.GL11.GL_CULL_FACE);
            renderer.setDepthMask(true); // Re-enable depth writing
            org.lwjgl.opengl.GL11.glDisable(org.lwjgl.opengl.GL11.GL_BLEND);
            renderer.endRender();
        }

        atlas.unbind();
    }

    private static float chunkDistanceSquared(Chunk chunk, Camera camera) {
        float cx = chunk.getWorldX() + Chunk.WIDTH * 0.5f;
        float cz = chunk.getWorldZ() + Chunk.DEPTH * 0.5f;
        float dx = cx - camera.getPosition().x;
        float dz = cz - camera.getPosition().z;
        return dx * dx + dz * dz;
    }

    /**
     * Get block at world coordinates.
     */
    public BlockType getBlock(int x, int y, int z) {
        if (y < 0 || y >= Chunk.HEIGHT) {
            return BlockType.AIR;
        }

        int chunkX = Math.floorDiv(x, Chunk.WIDTH);
        int chunkZ = Math.floorDiv(z, Chunk.DEPTH);

        Chunk chunk = getChunkNow(chunkX, chunkZ);

        int localX = Math.floorMod(x, Chunk.WIDTH);
        int localZ = Math.floorMod(z, Chunk.DEPTH);

        return chunk.getBlock(localX, y, localZ);
    }

    public BlockType getBlockIfLoaded(int x, int y, int z, BlockType fallback) {
        if (y < 0 || y >= Chunk.HEIGHT) {
            return fallback;
        }

        int chunkX = Math.floorDiv(x, Chunk.WIDTH);
        int chunkZ = Math.floorDiv(z, Chunk.DEPTH);
        Chunk chunk = chunks.get(chunkKey(chunkX, chunkZ));
        if (chunk == null || chunk.getState().ordinal() < Chunk.ChunkState.GENERATED.ordinal()) {
            return fallback;
        }

        int localX = Math.floorMod(x, Chunk.WIDTH);
        int localZ = Math.floorMod(z, Chunk.DEPTH);
        return chunk.getBlock(localX, y, localZ);
    }

    public Chunk getLoadedChunk(int chunkX, int chunkZ) {
        return chunks.get(chunkKey(chunkX, chunkZ));
    }

    public int getBlockMetadataIfLoaded(int x, int y, int z, int fallback) {
        if (y < 0 || y >= Chunk.HEIGHT) {
            return fallback;
        }

        int chunkX = Math.floorDiv(x, Chunk.WIDTH);
        int chunkZ = Math.floorDiv(z, Chunk.DEPTH);
        Chunk chunk = chunks.get(chunkKey(chunkX, chunkZ));
        if (chunk == null || chunk.getState().ordinal() < Chunk.ChunkState.GENERATED.ordinal()) {
            return fallback;
        }

        int localX = Math.floorMod(x, Chunk.WIDTH);
        int localZ = Math.floorMod(z, Chunk.DEPTH);
        return chunk.getBlockMetadata(localX, y, localZ);
    }

    public boolean isChunkGeneratedForBlock(int x, int z) {
        int chunkX = Math.floorDiv(x, Chunk.WIDTH);
        int chunkZ = Math.floorDiv(z, Chunk.DEPTH);
        Chunk chunk = chunks.get(chunkKey(chunkX, chunkZ));
        return chunk != null && chunk.getState().ordinal() >= Chunk.ChunkState.GENERATED.ordinal();
    }

    public boolean canSeeSky(int x, int y, int z) {
        if (y >= Chunk.HEIGHT) {
            return true;
        }
        if (!isChunkGeneratedForBlock(x, z)) {
            return false;
        }
        int startY = Math.max(0, y + 1);
        for (int checkY = startY; checkY < Chunk.HEIGHT; checkY++) {
            BlockType block = getBlockIfLoaded(x, checkY, z, BlockType.BEDROCK);
            if (!block.isAir() && !block.isTransparent()) {
                return false;
            }
        }
        return true;
    }

    public int getBlockMetadata(int x, int y, int z) {
        if (y < 0 || y >= Chunk.HEIGHT) {
            return 0;
        }

        int chunkX = Math.floorDiv(x, Chunk.WIDTH);
        int chunkZ = Math.floorDiv(z, Chunk.DEPTH);
        Chunk chunk = getChunkNow(chunkX, chunkZ);
        int localX = Math.floorMod(x, Chunk.WIDTH);
        int localZ = Math.floorMod(z, Chunk.DEPTH);
        return chunk.getBlockMetadata(localX, y, localZ);
    }

    public int getBlockLight(int x, int y, int z) {
        if (y < 0 || y >= Chunk.HEIGHT) {
            return 0;
        }

        int chunkX = Math.floorDiv(x, Chunk.WIDTH);
        int chunkZ = Math.floorDiv(z, Chunk.DEPTH);
        Chunk chunk = getChunkNow(chunkX, chunkZ);
        int localX = Math.floorMod(x, Chunk.WIDTH);
        int localZ = Math.floorMod(z, Chunk.DEPTH);
        chunk.calculateSkyLight();
        return chunk.getBlockLight(localX, y, localZ);
    }

    public int getBlockLightIfLoaded(int x, int y, int z, int fallback) {
        if (y < 0 || y >= Chunk.HEIGHT) {
            return fallback;
        }

        int chunkX = Math.floorDiv(x, Chunk.WIDTH);
        int chunkZ = Math.floorDiv(z, Chunk.DEPTH);
        Chunk chunk = chunks.get(chunkKey(chunkX, chunkZ));
        if (chunk == null || chunk.getState().ordinal() < Chunk.ChunkState.LIGHTED.ordinal()) {
            return fallback;
        }

        int localX = Math.floorMod(x, Chunk.WIDTH);
        int localZ = Math.floorMod(z, Chunk.DEPTH);
        return chunk.getBlockLight(localX, y, localZ);
    }

    public List<AABB> getCollisionBoxes(int x, int y, int z) {
        BlockType type = getBlock(x, y, z);
        int metadata = getBlockMetadata(x, y, z);
        return BlockShape.collisionShape(new BlockState(type, metadata), contextAt(x, y, z)).toAabbs(x, y, z);
    }

    public List<AABB> getCollisionBoxesIfLoaded(int x, int y, int z) {
        BlockType type = getBlockIfLoaded(x, y, z, BlockType.AIR);
        if (type == BlockType.AIR) {
            return List.of();
        }
        int metadata = getBlockMetadataIfLoaded(x, y, z, 0);
        return BlockShape.collisionShape(new BlockState(type, metadata), contextAtIfLoaded(x, y, z))
                .toAabbs(x, y, z);
    }

    public List<AABB> getSelectionBoxes(int x, int y, int z) {
        BlockType type = getBlock(x, y, z);
        int metadata = getBlockMetadata(x, y, z);
        return BlockShape.selectionShape(new BlockState(type, metadata), contextAt(x, y, z)).toAabbs(x, y, z);
    }

    public List<AABB> getSelectionBoxesIfLoaded(int x, int y, int z) {
        BlockType type = getBlockIfLoaded(x, y, z, BlockType.AIR);
        if (type == BlockType.AIR) {
            return List.of();
        }
        int metadata = getBlockMetadataIfLoaded(x, y, z, 0);
        return BlockShape.selectionShape(new BlockState(type, metadata), contextAtIfLoaded(x, y, z))
                .toAabbs(x, y, z);
    }

    public List<AABB> getPlacementCollisionBoxes(int x, int y, int z, BlockType type, int metadata) {
        return BlockShape.collisionShape(new BlockState(type, metadata), contextAt(x, y, z)).toAabbs(x, y, z);
    }

    public boolean canPlaceBlockAt(int x, int y, int z, BlockType type, int metadata, AABB playerBox) {
        if (y < 0 || y >= Chunk.HEIGHT || !BlockShape.isReplaceable(getBlock(x, y, z))) {
            return false;
        }
        if (!BlockShape.canPlaceAt(type, metadata, contextAt(x, y, z))) {
            return false;
        }
        if (playerBox != null) {
            for (AABB box : getPlacementCollisionBoxes(x, y, z, type, metadata)) {
                if (box.intersects(playerBox)) {
                    return false;
                }
            }
        }
        return true;
    }

    BlockShape.BlockContext contextAt(int x, int y, int z) {
        return new BlockShape.BlockContext() {
            @Override
            public BlockType getBlock(int dx, int dy, int dz) {
                return World.this.getBlock(x + dx, y + dy, z + dz);
            }

            @Override
            public int getMetadata(int dx, int dy, int dz) {
                return World.this.getBlockMetadata(x + dx, y + dy, z + dz);
            }
        };
    }

    public BlockShape.BlockContext contextAtIfLoaded(int x, int y, int z) {
        return new BlockShape.BlockContext() {
            @Override
            public BlockType getBlock(int dx, int dy, int dz) {
                return World.this.getBlockIfLoaded(x + dx, y + dy, z + dz, BlockType.AIR);
            }

            @Override
            public int getMetadata(int dx, int dy, int dz) {
                return World.this.getBlockMetadataIfLoaded(x + dx, y + dy, z + dz, 0);
            }
        };
    }

    /**
     * Get sky light level at world coordinates (0-15).
     * Returns 15 (full light) if chunk not yet LIGHTED, preventing hostile mob
     * spawning.
     */
    public int getSkyLight(int x, int y, int z) {
        if (y < 0 || y >= Chunk.HEIGHT) {
            return y >= Chunk.HEIGHT ? 15 : 0;
        }

        int chunkX = Math.floorDiv(x, Chunk.WIDTH);
        int chunkZ = Math.floorDiv(z, Chunk.DEPTH);

        Chunk chunk = chunks.get(chunkKey(chunkX, chunkZ));
        if (chunk == null) {
            return 15; // Unloaded chunks default to full light
        }

        // Don't return lighting data until chunk has been properly lit
        // This prevents hostile mobs from spawning before lighting is calculated
        if (chunk.getState().ordinal() < Chunk.ChunkState.LIGHTED.ordinal()) {
            return 15; // Not ready yet, return full light to prevent hostile spawns
        }

        int localX = Math.floorMod(x, Chunk.WIDTH);
        int localZ = Math.floorMod(z, Chunk.DEPTH);

        return chunk.getSkyLight(localX, y, localZ);
    }

    /**
     * Set block at world coordinates.
     */
    public void setBlock(int x, int y, int z, BlockType type) {
        setBlock(x, y, z, type, 0);
    }

    public void setBlock(int x, int y, int z, BlockType type, int metadata) {
        setBlockInternal(x, y, z, type, metadata, false);
    }

    public void setBlockPreservingTile(int x, int y, int z, BlockType type, int metadata) {
        setBlockInternal(x, y, z, type, metadata, true);
    }

    private void setBlockInternal(int x, int y, int z, BlockType type, int metadata, boolean preserveTile) {
        if (y < 0 || y >= Chunk.HEIGHT) {
            return;
        }

        int chunkX = Math.floorDiv(x, Chunk.WIDTH);
        int chunkZ = Math.floorDiv(z, Chunk.DEPTH);

        Chunk chunk = getChunkNow(chunkX, chunkZ);

        int localX = Math.floorMod(x, Chunk.WIDTH);
        int localZ = Math.floorMod(z, Chunk.DEPTH);

        BlockType previous = chunk.getBlock(localX, y, localZ);
        int previousMetadata = chunk.getBlockMetadata(localX, y, localZ);
        if (previous == type && previousMetadata == metadata) {
            return;
        }
        chunk.setBlock(localX, y, localZ, type, metadata);
        scheduledTickKeys.remove(new ScheduledTickKey(x, y, z, previous));

        BlockPos pos = new BlockPos(x, y, z);
        if (!preserveTile && previous.hasTileEntity()) {
            tileEntities.remove(pos);
        }
        if (type.hasTileEntity()) {
            tileEntities.computeIfAbsent(pos, key -> createTileEntityForBlock(type, x, y, z));
        } else if (!preserveTile) {
            tileEntities.remove(pos);
        }

        // Mark neighboring chunks as dirty if on border
        // Also recalculate their light since block changes affect cross-chunk lighting
        if (localX == 0) {
            Chunk neighbor = chunks.get(chunkKey(chunkX - 1, chunkZ));
            if (neighbor != null) {
                neighbor.setDirty(true);
                neighbor.markLightDirty();
            }
        }
        if (localX == Chunk.WIDTH - 1) {
            Chunk neighbor = chunks.get(chunkKey(chunkX + 1, chunkZ));
            if (neighbor != null) {
                neighbor.setDirty(true);
                neighbor.markLightDirty();
            }
        }
        if (localZ == 0) {
            Chunk neighbor = chunks.get(chunkKey(chunkX, chunkZ - 1));
            if (neighbor != null) {
                neighbor.setDirty(true);
                neighbor.markLightDirty();
            }
        }
        if (localZ == Chunk.DEPTH - 1) {
            Chunk neighbor = chunks.get(chunkKey(chunkX, chunkZ + 1));
            if (neighbor != null) {
                neighbor.setDirty(true);
                neighbor.markLightDirty();
            }
        }

        if (!suppressNeighborSupportUpdates) {
            notifyBlockChanged(x, y, z, previous, previousMetadata, type, metadata);
        }
    }

    public boolean setBlockIfLoaded(int x, int y, int z, BlockType type, int metadata) {
        if (y < 0 || y >= Chunk.HEIGHT) {
            return false;
        }
        int chunkX = Math.floorDiv(x, Chunk.WIDTH);
        int chunkZ = Math.floorDiv(z, Chunk.DEPTH);
        Chunk chunk = chunks.get(chunkKey(chunkX, chunkZ));
        if (chunk == null || chunk.getState().ordinal() < Chunk.ChunkState.GENERATED.ordinal()) {
            return false;
        }

        int localX = Math.floorMod(x, Chunk.WIDTH);
        int localZ = Math.floorMod(z, Chunk.DEPTH);
        BlockType previous = chunk.getBlock(localX, y, localZ);
        int previousMetadata = chunk.getBlockMetadata(localX, y, localZ);
        if (previous == type && previousMetadata == metadata) {
            return true;
        }

        chunk.setBlock(localX, y, localZ, type, metadata);
        scheduledTickKeys.remove(new ScheduledTickKey(x, y, z, previous));

        BlockPos pos = new BlockPos(x, y, z);
        if (previous.hasTileEntity() && !type.hasTileEntity()) {
            tileEntities.remove(pos);
        }
        if (type.hasTileEntity()) {
            tileEntities.computeIfAbsent(pos, key -> createTileEntityForBlock(type, x, y, z));
        } else {
            tileEntities.remove(pos);
        }

        chunk.setDirty(true);
        chunk.markLightDirty();
        markNeighborChunkDirtyForBorder(chunkX, chunkZ, localX, localZ);
        if (!suppressNeighborSupportUpdates) {
            notifyBlockChanged(x, y, z, previous, previousMetadata, type, metadata);
        }
        return true;
    }

    private void markNeighborChunkDirtyForBorder(int chunkX, int chunkZ, int localX, int localZ) {
        if (localX == 0) {
            markChunkDirtyIfLoaded(chunkX - 1, chunkZ);
        }
        if (localX == Chunk.WIDTH - 1) {
            markChunkDirtyIfLoaded(chunkX + 1, chunkZ);
        }
        if (localZ == 0) {
            markChunkDirtyIfLoaded(chunkX, chunkZ - 1);
        }
        if (localZ == Chunk.DEPTH - 1) {
            markChunkDirtyIfLoaded(chunkX, chunkZ + 1);
        }
    }

    private void markChunkDirtyIfLoaded(int chunkX, int chunkZ) {
        Chunk neighbor = chunks.get(chunkKey(chunkX, chunkZ));
        if (neighbor != null) {
            neighbor.setDirty(true);
            neighbor.markLightDirty();
        }
    }

    public void tickBlockUpdates(float deltaTime) {
        blockTickAccumulator += deltaTime * 20.0f;
        while (blockTickAccumulator >= 1.0f) {
            blockTickAccumulator -= 1.0f;
            advanceBlockTicks(1);
        }
    }

    public void advanceBlockTicks(int ticks) {
        for (int i = 0; i < ticks; i++) {
            blockTickClock++;
            int processed = 0;
            while (!scheduledBlockTicks.isEmpty()
                    && scheduledBlockTicks.peek().dueTick() <= blockTickClock
                    && processed < MAX_BLOCK_UPDATES_PER_TICK) {
                ScheduledBlockTick tick = scheduledBlockTicks.poll();
                if (!scheduledTickKeys.remove(tick.key())) {
                    continue;
                }
                ScheduledTickKey key = tick.key();
                if (getBlockIfLoaded(key.x(), key.y(), key.z(), null) != key.type()) {
                    continue;
                }
                if (requiresHorizontalTickNeighborhood(key.type())
                        && !hasLoadedHorizontalTickNeighborhood(key.x(), key.z())) {
                    continue;
                }
                tickScheduledBlock(key.x(), key.y(), key.z(), key.type());
                processed++;
            }
        }
    }

    private boolean hasLoadedHorizontalTickNeighborhood(int x, int z) {
        return isChunkGeneratedForBlock(x, z)
                && isChunkGeneratedForBlock(x + 1, z)
                && isChunkGeneratedForBlock(x - 1, z)
                && isChunkGeneratedForBlock(x, z + 1)
                && isChunkGeneratedForBlock(x, z - 1);
    }

    private boolean requiresHorizontalTickNeighborhood(BlockType type) {
        return type.isFluid() || type == BlockType.FIRE || RedstoneEngine.isRedstoneTickable(type);
    }

    public int getScheduledBlockTickCount() {
        return scheduledTickKeys.size();
    }

    public boolean hasScheduledBlockTick(int x, int y, int z, BlockType type) {
        return scheduledTickKeys.contains(new ScheduledTickKey(x, y, z, type));
    }

    public long getBlockTickClock() {
        return blockTickClock;
    }

    public void scheduleBlockTick(int x, int y, int z, BlockType type, int delayTicks) {
        if (type == null || !isTickableBlock(type) || y < 0 || y >= Chunk.HEIGHT) {
            return;
        }
        ScheduledTickKey key = new ScheduledTickKey(x, y, z, type);
        if (!scheduledTickKeys.add(key)) {
            return;
        }
        long dueTick = blockTickClock + Math.max(0, delayTicks);
        scheduledBlockTicks.add(new ScheduledBlockTick(dueTick, nextBlockTickSequence++, key));
    }

    private void removeScheduledTicksForChunk(Chunk chunk) {
        int minX = chunk.getWorldX();
        int maxX = minX + Chunk.WIDTH;
        int minZ = chunk.getWorldZ();
        int maxZ = minZ + Chunk.DEPTH;
        scheduledTickKeys.removeIf(key -> key.x() >= minX && key.x() < maxX
                && key.z() >= minZ && key.z() < maxZ);
        scheduledBlockTicks.removeIf(tick -> tick.key().x() >= minX && tick.key().x() < maxX
                && tick.key().z() >= minZ && tick.key().z() < maxZ);
    }

    public void scheduleMechanismUpdatesAround(int x, int y, int z) {
        RedstoneEngine.scheduleAround(this, x, y, z);
    }

    public boolean isBlockPowered(int x, int y, int z) {
        return RedstoneEngine.isBlockPowered(this, x, y, z);
    }

    public int getWeakPower(int x, int y, int z, int towardFace) {
        return RedstoneEngine.getWeakPower(this, x, y, z, towardFace);
    }

    public int getStrongPower(int x, int y, int z, int towardFace) {
        return RedstoneEngine.getStrongPower(this, x, y, z, towardFace);
    }

    private void scheduleTickableBlocksInChunk(Chunk chunk) {
        int worldX = chunk.getChunkX() * Chunk.WIDTH;
        int worldZ = chunk.getChunkZ() * Chunk.DEPTH;
        for (int y = 0; y < Chunk.HEIGHT; y++) {
            for (int z = 0; z < Chunk.DEPTH; z++) {
                for (int x = 0; x < Chunk.WIDTH; x++) {
                    BlockType type = chunk.getBlock(x, y, z);
                    int bx = worldX + x;
                    int bz = worldZ + z;
                    if (type.isFlowingFluid() || type == BlockType.FIRE
                            || RedstoneEngine.isRedstoneTickable(type)) {
                        scheduleBlockTick(bx, y, bz, type, getTickDelay(type));
                    }
                }
            }
        }
    }

    private boolean isTickableBlock(BlockType type) {
        return type.isFluid() || type == BlockType.FIRE || type.isFallingBlock()
                || RedstoneEngine.isRedstoneTickable(type);
    }

    private int getTickDelay(BlockType type) {
        if (type.isWater()) {
            return WATER_TICK_DELAY;
        }
        if (type.isLava()) {
            return LAVA_TICK_DELAY;
        }
        if (type == BlockType.FIRE) {
            return FIRE_TICK_DELAY;
        }
        if (type.isFallingBlock()) {
            return FALLING_BLOCK_TICK_DELAY;
        }
        if (RedstoneEngine.isRedstoneTickable(type)) {
            return RedstoneEngine.getTickDelay(type, 0);
        }
        return 1;
    }

    private void tickScheduledBlock(int x, int y, int z, BlockType type) {
        if (type.isFluid()) {
            updateFluidBlock(x, y, z, type);
        } else if (type == BlockType.FIRE) {
            updateFireBlock(x, y, z);
        } else if (type.isFallingBlock()) {
            updateFallingBlock(x, y, z, type);
        } else if (RedstoneEngine.isRedstoneTickable(type)) {
            RedstoneEngine.tick(this, x, y, z, type);
        }
    }

    private void notifyBlockChanged(int x, int y, int z, BlockType previous, int previousMetadata,
            BlockType current, int currentMetadata) {
        scheduleBlockTick(x, y, z, current, getTickDelay(current));
        updateNeighborSupport(x, y, z);
        scheduleNeighborBlockUpdates(x, y, z);
        scheduleMechanismUpdatesAround(x, y, z);
        tryMixFluidsAround(x, y, z);
        if (current.isLava()) {
            igniteAroundLava(x, y, z);
        }
    }

    private void scheduleNeighborBlockUpdates(int x, int y, int z) {
        int[][] dirs = {
                { 0, 0, 0 },
                { 1, 0, 0 }, { -1, 0, 0 },
                { 0, 1, 0 }, { 0, -1, 0 },
                { 0, 0, 1 }, { 0, 0, -1 }
        };
        for (int[] dir : dirs) {
            int nx = x + dir[0];
            int ny = y + dir[1];
            int nz = z + dir[2];
            BlockType neighbor = getBlockIfLoaded(nx, ny, nz, BlockType.AIR);
            if (neighbor.isFluid() || neighbor == BlockType.FIRE
                    || RedstoneEngine.isRedstoneTickable(neighbor)
                    || (neighbor.isFallingBlock() && ny > 0
                            && BlockShape.canFallThrough(getBlockIfLoaded(nx, ny - 1, nz, BlockType.AIR)))) {
                scheduleBlockTick(nx, ny, nz, neighbor, getTickDelay(neighbor));
            }
            if (neighbor.isLava()) {
                igniteAroundLava(nx, ny, nz);
            }
        }
    }

    public boolean isFluidSource(int x, int y, int z) {
        BlockType type = getBlock(x, y, z);
        return type.isFluid() && FluidState.isSource(getBlockMetadata(x, y, z));
    }

    public ItemType pickupFluidSource(int x, int y, int z) {
        BlockType type = getBlock(x, y, z);
        if (!isFluidSource(x, y, z)) {
            return null;
        }
        ItemType filled = type.isWater() ? ItemType.WATER_BUCKET : ItemType.LAVA_BUCKET;
        setBlock(x, y, z, BlockType.AIR, 0);
        return filled;
    }

    public boolean placeFluidSource(int x, int y, int z, boolean water, AABB playerBox) {
        BlockType source = water ? BlockType.WATER : BlockType.LAVA;
        if (y < 0 || y >= Chunk.HEIGHT || !BlockShape.isReplaceable(getBlock(x, y, z))) {
            return false;
        }
        if (playerBox != null) {
            for (AABB box : getPlacementCollisionBoxes(x, y, z, source, 0)) {
                if (box.intersects(playerBox)) {
                    return false;
                }
            }
        }
        setBlock(x, y, z, source, 0);
        return true;
    }

    public boolean canPlaceFallingBlockAt(int x, int y, int z, BlockType type, int metadata) {
        if (y < 0 || y >= Chunk.HEIGHT) {
            return false;
        }
        BlockType target = getBlock(x, y, z);
        if (target != BlockType.AIR && !target.isFluid()) {
            return false;
        }
        return !BlockShape.canFallThrough(getBlock(x, y - 1, z));
    }

    private void updateFluidBlock(int x, int y, int z, BlockType type) {
        boolean water = type.isWater();
        if (tryMixFluidAt(x, y, z)) {
            return;
        }

        int metadata = getBlockMetadata(x, y, z) & 15;
        int decayStep = water ? 1 : 2;

        if (metadata > 0) {
            int[] sourceCount = { 0 };
            int smallestFlowDecay = -100;
            for (int[] dir : HORIZONTAL_DIRS) {
                smallestFlowDecay = getSmallestFlowDecay(x + dir[0], y, z + dir[1],
                        smallestFlowDecay, water, sourceCount);
            }

            int newDecay = smallestFlowDecay + decayStep;
            if (newDecay >= 8 || smallestFlowDecay < 0) {
                newDecay = -1;
            }

            int aboveDecay = getFlowDecay(x, y + 1, z, water);
            if (aboveDecay >= 0) {
                newDecay = aboveDecay >= 8 ? aboveDecay : aboveDecay + 8;
            }

            if (water && sourceCount[0] >= 2) {
                BlockType below = getBlock(x, y - 1, z);
                if (below.isSolid() || (below.isWater() && (getBlockMetadata(x, y - 1, z) & 7) == 0)) {
                    newDecay = 0;
                }
            }

            if (newDecay != metadata) {
                if (newDecay < 0) {
                    setBlock(x, y, z, BlockType.AIR, 0);
                    return;
                }
                setBlock(x, y, z, newDecay == 0 ? BlockType.stillVariant(water) : BlockType.flowingVariant(water),
                        newDecay);
                metadata = newDecay;
            }
        }

        if (canFluidDisplace(x, y - 1, z, water)) {
            int downDecay = metadata >= 8 ? metadata : metadata + 8;
            flowIntoBlock(x, y - 1, z, water, downDecay, true);
        } else if (metadata >= 0 && (metadata == 0 || blocksFluidFlow(x, y - 1, z))) {
            int spreadDecay = metadata >= 8 ? 1 : metadata + decayStep;
            if (spreadDecay < 8) {
                boolean[] directions = getOptimalFlowDirections(x, y, z, water);
                for (int i = 0; i < HORIZONTAL_DIRS.length; i++) {
                    if (directions[i]) {
                        flowIntoBlock(x + HORIZONTAL_DIRS[i][0], y, z + HORIZONTAL_DIRS[i][1],
                                water, spreadDecay, false);
                    }
                }
            }
        }

        BlockType current = getBlock(x, y, z);
        if (current.isFluid()) {
            scheduleBlockTick(x, y, z, current, getTickDelay(current));
        }
    }

    private int getSmallestFlowDecay(int x, int y, int z, int currentSmallest, boolean water, int[] sourceCount) {
        int decay = getFlowDecay(x, y, z, water);
        if (decay < 0) {
            return currentSmallest;
        }
        if (decay == 0) {
            sourceCount[0]++;
        }
        if (decay >= 8) {
            decay = 0;
        }
        return currentSmallest >= 0 && decay >= currentSmallest ? currentSmallest : decay;
    }

    private int getFlowDecay(int x, int y, int z, boolean water) {
        BlockType type = getBlock(x, y, z);
        if (water ? !type.isWater() : !type.isLava()) {
            return -1;
        }
        return getBlockMetadata(x, y, z) & 15;
    }

    private boolean[] getOptimalFlowDirections(int x, int y, int z, boolean water) {
        boolean[] optimal = new boolean[4];
        int bestCost = 1000;
        for (int i = 0; i < HORIZONTAL_DIRS.length; i++) {
            int nx = x + HORIZONTAL_DIRS[i][0];
            int nz = z + HORIZONTAL_DIRS[i][1];
            if (blocksFluidFlow(nx, y, nz) || isSameFluid(nx, y, nz, water)) {
                continue;
            }
            int cost = !blocksFluidFlow(nx, y - 1, nz)
                    ? 0
                    : calculateFlowCost(nx, y, nz, 1, i, water);
            if (cost < bestCost) {
                bestCost = cost;
            }
        }
        for (int i = 0; i < HORIZONTAL_DIRS.length; i++) {
            int nx = x + HORIZONTAL_DIRS[i][0];
            int nz = z + HORIZONTAL_DIRS[i][1];
            if (blocksFluidFlow(nx, y, nz) || isSameFluid(nx, y, nz, water)) {
                continue;
            }
            int cost = !blocksFluidFlow(nx, y - 1, nz)
                    ? 0
                    : calculateFlowCost(nx, y, nz, 1, i, water);
            optimal[i] = cost == bestCost;
        }
        return optimal;
    }

    private int calculateFlowCost(int x, int y, int z, int depth, int previousDirection, boolean water) {
        int bestCost = 1000;
        for (int i = 0; i < HORIZONTAL_DIRS.length; i++) {
            if (i == oppositeHorizontalDirection(previousDirection)) {
                continue;
            }
            int nx = x + HORIZONTAL_DIRS[i][0];
            int nz = z + HORIZONTAL_DIRS[i][1];
            if (blocksFluidFlow(nx, y, nz) || isSameFluid(nx, y, nz, water)) {
                continue;
            }
            if (!blocksFluidFlow(nx, y - 1, nz)) {
                return depth;
            }
            if (depth >= 4) {
                continue;
            }
            int cost = calculateFlowCost(nx, y, nz, depth + 1, i, water);
            if (cost < bestCost) {
                bestCost = cost;
            }
        }
        return bestCost;
    }

    private int oppositeHorizontalDirection(int direction) {
        return switch (direction) {
            case 0 -> 1;
            case 1 -> 0;
            case 2 -> 3;
            default -> 2;
        };
    }

    private boolean isSameFluid(int x, int y, int z, boolean water) {
        BlockType type = getBlock(x, y, z);
        return water ? type.isWater() : type.isLava();
    }

    private boolean blocksFluidFlow(int x, int y, int z) {
        if (y < 0 || y >= Chunk.HEIGHT) {
            return true;
        }
        BlockType type = getBlock(x, y, z);
        if (type == BlockType.AIR || type.isFluid() || type == BlockType.FIRE
                || type == BlockType.TORCH || type.isPlant()) {
            return false;
        }
        if (type.isDoor() || type.isSign() || type == BlockType.LADDER) {
            return true;
        }
        return type.isSolid();
    }

    private boolean canFluidDisplace(int x, int y, int z, boolean water) {
        if (y < 0 || y >= Chunk.HEIGHT) {
            return false;
        }
        BlockType target = getBlock(x, y, z);
        if (water && target.isLava()) {
            return true;
        }
        if (!water && target.isWater()) {
            return true;
        }
        if (target.isFluid()) {
            return water ? target.isWater() : target.isLava();
        }
        if (target.isDoor() || target.isSign() || target == BlockType.LADDER) {
            return false;
        }
        return target == BlockType.AIR || target == BlockType.FIRE
                || target == BlockType.TORCH || target.isPlant() || !target.isSolid();
    }

    private void flowIntoBlock(int x, int y, int z, boolean water, int metadata, boolean downward) {
        BlockType target = getBlock(x, y, z);
        if (water && target.isLava()) {
            setBlock(x, y, z, (getBlockMetadata(x, y, z) & 7) == 0 ? BlockType.OBSIDIAN : BlockType.COBBLESTONE, 0);
            return;
        }
        if (!water && target.isWater()) {
            setBlock(x, y, z, downward ? BlockType.STONE : BlockType.COBBLESTONE, 0);
            return;
        }
        if (!canFluidDisplace(x, y, z, water)) {
            return;
        }
        if (target.isFluid() && (water ? target.isWater() : target.isLava())
                && FluidState.isStrongerOrEqual(getBlockMetadata(x, y, z), metadata)) {
            return;
        }
        if (target != BlockType.AIR && !target.isFluid()) {
            if (water) {
                breakBlock(x, y, z, true);
            } else {
                breakBlock(x, y, z, false);
            }
        }
        setBlock(x, y, z, BlockType.flowingVariant(water), metadata);
    }

    private void tryMixFluidsAround(int x, int y, int z) {
        tryMixFluidAt(x, y, z);
        int[][] dirs = {
                { 1, 0, 0 }, { -1, 0, 0 },
                { 0, 1, 0 }, { 0, -1, 0 },
                { 0, 0, 1 }, { 0, 0, -1 }
        };
        for (int[] dir : dirs) {
            tryMixFluidAt(x + dir[0], y + dir[1], z + dir[2]);
        }
    }

    private boolean tryMixFluidAt(int x, int y, int z) {
        BlockType type = getBlock(x, y, z);
        if (!type.isLava()) {
            return false;
        }
        int[][] dirs = {
                { 1, 0, 0 }, { -1, 0, 0 },
                { 0, 1, 0 }, { 0, 0, 1 }, { 0, 0, -1 }
        };
        for (int[] dir : dirs) {
            if (getBlock(x + dir[0], y + dir[1], z + dir[2]).isWater()) {
                setBlock(x, y, z, (getBlockMetadata(x, y, z) & 7) == 0 ? BlockType.OBSIDIAN : BlockType.COBBLESTONE, 0);
                return true;
            }
        }
        return false;
    }

    private void updateFallingBlock(int x, int y, int z, BlockType type) {
        if (y <= 0 || !BlockShape.canFallThrough(getBlock(x, y - 1, z))) {
            return;
        }
        int metadata = getBlockMetadata(x, y, z);
        setBlock(x, y, z, BlockType.AIR, 0);
        FallingBlockEntity falling = new FallingBlockEntity(type, metadata);
        falling.setPosition(x + 0.5f, y, z + 0.5f);
        spawnEntity(falling);
    }

    private void updateFireBlock(int x, int y, int z) {
        if (!canFireStay(x, y, z)) {
            setBlock(x, y, z, BlockType.AIR, 0);
            return;
        }
        int age = Math.min(15, getBlockMetadata(x, y, z) + (random.nextInt(3) == 0 ? 1 : 0));
        if (age != getBlockMetadata(x, y, z)) {
            setBlock(x, y, z, BlockType.FIRE, age);
        }
        spreadFireFrom(x, y, z, age);
        if (getBlock(x, y, z) == BlockType.FIRE) {
            if (age >= 15 && random.nextInt(4) == 0 && !hasFlammableNeighbor(x, y, z)) {
                setBlock(x, y, z, BlockType.AIR, 0);
            } else {
                scheduleBlockTick(x, y, z, BlockType.FIRE, FIRE_TICK_DELAY);
            }
        }
    }

    private boolean canFireStay(int x, int y, int z) {
        return getBlock(x, y - 1, z).isSolid() || hasFlammableNeighbor(x, y, z);
    }

    private boolean hasFlammableNeighbor(int x, int y, int z) {
        int[][] dirs = {
                { 1, 0, 0 }, { -1, 0, 0 },
                { 0, 1, 0 }, { 0, -1, 0 },
                { 0, 0, 1 }, { 0, 0, -1 }
        };
        for (int[] dir : dirs) {
            if (getBlock(x + dir[0], y + dir[1], z + dir[2]).isFlammable()) {
                return true;
            }
        }
        return false;
    }

    private void spreadFireFrom(int x, int y, int z, int age) {
        int[][] dirs = {
                { 1, 0, 0 }, { -1, 0, 0 },
                { 0, 1, 0 }, { 0, -1, 0 },
                { 0, 0, 1 }, { 0, 0, -1 }
        };
        for (int[] dir : dirs) {
            int nx = x + dir[0];
            int ny = y + dir[1];
            int nz = z + dir[2];
            BlockType target = getBlock(nx, ny, nz);
            if (target == BlockType.AIR && hasFlammableNeighbor(nx, ny, nz) && random.nextInt(6 + age) == 0) {
                setBlock(nx, ny, nz, BlockType.FIRE, Math.min(15, age + 1));
            } else if (target.isFlammable() && random.nextInt(18 + age) == 0) {
                setBlock(nx, ny, nz, BlockType.FIRE, Math.min(15, age + 1));
            }
        }
    }

    private void igniteAroundLava(int x, int y, int z) {
        for (int dy = 0; dy <= 1; dy++) {
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    int nx = x + dx;
                    int ny = y + dy;
                    int nz = z + dz;
                    if (getBlock(nx, ny, nz) == BlockType.AIR && hasFlammableNeighbor(nx, ny, nz)) {
                        setBlock(nx, ny, nz, BlockType.FIRE, 0);
                    }
                }
            }
        }
    }

    public boolean breakBlock(int x, int y, int z, boolean dropBlock) {
        BlockType type = getBlock(x, y, z);
        if (type == BlockType.AIR || type == BlockType.BEDROCK) {
            return false;
        }

        int metadata = getBlockMetadata(x, y, z);
        if (type.isDoor()) {
            return breakDoor(x, y, z, type, metadata, dropBlock);
        }
        if (type.isBed()) {
            return breakBed(x, y, z, metadata, dropBlock);
        }

        TileEntity tile = removeTileEntity(x, y, z);
        if (dropBlock) {
            ItemStack droppedStack = BlockDropResolver.getDrop(type, random);
            if (droppedStack != null && !droppedStack.isEmpty()) {
                spawnThrownStack(x + 0.5f, y + 0.5f, z + 0.5f, droppedStack,
                        (random.nextFloat() - 0.5f) * 0.12f,
                        0.18f,
                        (random.nextFloat() - 0.5f) * 0.12f);
            }
        }
        if (tile != null) {
            for (ItemStack stack : tile.getDrops()) {
                spawnThrownStack(x + 0.5f, y + 0.5f, z + 0.5f, stack,
                        (random.nextFloat() - 0.5f) * 0.25f,
                        0.2f,
                        (random.nextFloat() - 0.5f) * 0.25f);
            }
        }

        setBlock(x, y, z, BlockType.AIR, 0);
        return true;
    }

    private TileEntity createTileEntityForBlock(BlockType type, int x, int y, int z) {
        if (type == BlockType.CHEST) {
            return new ChestTileEntity(x, y, z);
        }
        if (type.isFurnace()) {
            return new FurnaceTileEntity(x, y, z);
        }
        if (type == BlockType.MOB_SPAWNER) {
            return new MonsterSpawnerTileEntity(x, y, z);
        }
        if (type == BlockType.BREWING_STAND) {
            return new BrewingStandTileEntity(x, y, z);
        }
        if (type == BlockType.DISPENSER) {
            return new DispenserTileEntity(x, y, z);
        }
        if (type == BlockType.NOTE_BLOCK) {
            return new NoteBlockTileEntity(x, y, z);
        }
        if (type == BlockType.JUKEBOX) {
            return new JukeboxTileEntity(x, y, z);
        }
        if (type.isSign()) {
            return new SignTileEntity(x, y, z);
        }
        return null;
    }

    public TileEntity getTileEntity(int x, int y, int z) {
        return tileEntities.get(new BlockPos(x, y, z));
    }

    public TileEntity removeTileEntity(int x, int y, int z) {
        return tileEntities.remove(new BlockPos(x, y, z));
    }

    public void putTileEntity(TileEntity tileEntity) {
        if (tileEntity != null) {
            tileEntities.put(tileEntity.getPos(), tileEntity);
        }
    }

    @Override
    public void stageGeneratedTileEntity(TileEntity tileEntity) {
        if (tileEntity != null) {
            generatedTileEntities.put(tileEntity.getPos(), tileEntity);
        }
    }

    @Override
    public void stageGeneratedEntity(Entity entity) {
        if (entity != null) {
            generatedEntities.add(entity);
        }
    }

    public Collection<TileEntity> getTileEntities() {
        return tileEntities.values();
    }

    public void replaceTileEntities(Collection<TileEntity> restored) {
        tileEntities.clear();
        for (TileEntity tile : restored) {
            putTileEntity(tile);
        }
        reconcileLoadedTileEntities();
    }

    public void reconcileLoadedTileEntities() {
        for (Chunk chunk : chunks.values()) {
            if (chunk.getState().ordinal() >= Chunk.ChunkState.GENERATED.ordinal()) {
                reconcileTileEntitiesInChunk(chunk);
            }
        }
    }

    private void reconcileTileEntitiesInChunk(Chunk chunk) {
        int worldX = chunk.getChunkX() * Chunk.WIDTH;
        int worldZ = chunk.getChunkZ() * Chunk.DEPTH;
        for (int y = 0; y < Chunk.HEIGHT; y++) {
            for (int z = 0; z < Chunk.DEPTH; z++) {
                for (int x = 0; x < Chunk.WIDTH; x++) {
                    BlockType type = chunk.getBlock(x, y, z);
                    if (type.hasTileEntity()) {
                        final int bx = worldX + x;
                        final int by = y;
                        final int bz = worldZ + z;
                        BlockPos pos = new BlockPos(bx, by, bz);
                        TileEntity staged = generatedTileEntities.remove(pos);
                        tileEntities.computeIfAbsent(pos, key -> {
                            return staged != null ? staged : createTileEntityForBlock(type, bx, by, bz);
                        });
                    } else if (!generatedTileEntities.isEmpty()) {
                        generatedTileEntities.remove(new BlockPos(worldX + x, y, worldZ + z));
                    }
                }
            }
        }
    }

    public void tickTileEntities(float deltaTime) {
        for (TileEntity tile : tileEntities.values()) {
            tile.tick(this, deltaTime);
        }
    }

    public boolean canPlaceChestAt(int x, int y, int z) {
        int adjacent = 0;
        for (int[] dir : HORIZONTAL_DIRS) {
            int nx = x + dir[0];
            int nz = z + dir[1];
            if (getBlock(nx, y, nz) == BlockType.CHEST) {
                adjacent++;
                if (adjacent > 1 || countAdjacentChests(nx, y, nz, x, z) > 0) {
                    return false;
                }
            }
        }
        return true;
    }

    public ChestTileEntity getAdjacentChest(ChestTileEntity chest) {
        BlockPos pos = chest.getPos();
        for (int[] dir : HORIZONTAL_DIRS) {
            TileEntity tile = getTileEntity(pos.x() + dir[0], pos.y(), pos.z() + dir[1]);
            if (tile instanceof ChestTileEntity adjacent) {
                return adjacent;
            }
        }
        return null;
    }

    private int countAdjacentChests(int x, int y, int z, int ignoreX, int ignoreZ) {
        int count = 0;
        for (int[] dir : HORIZONTAL_DIRS) {
            int nx = x + dir[0];
            int nz = z + dir[1];
            if (nx == ignoreX && nz == ignoreZ) {
                continue;
            }
            if (getBlock(nx, y, nz) == BlockType.CHEST) {
                count++;
            }
        }
        return count;
    }

    public boolean toggleBlock(int x, int y, int z) {
        if (RedstoneEngine.toggleInteractiveBlock(this, x, y, z)) {
            return true;
        }
        BlockType type = getBlock(x, y, z);
        int metadata = getBlockMetadata(x, y, z);
        if (type == BlockType.WOODEN_DOOR) {
            int lowerY = BlockShape.isDoorUpper(metadata) ? y - 1 : y;
            int lowerMetadata = getBlockMetadata(x, lowerY, z);
            setBlock(x, lowerY, z, type, lowerMetadata ^ 4);
            scheduleMechanismUpdatesAround(x, lowerY, z);
            return true;
        }
        if (type == BlockType.IRON_DOOR) {
            return false;
        }
        if (type == BlockType.TRAPDOOR || type == BlockType.FENCE_GATE) {
            setBlock(x, y, z, type, metadata ^ 4);
            scheduleMechanismUpdatesAround(x, y, z);
            return true;
        }
        return false;
    }

    public boolean addEyeToEndPortalFrame(int x, int y, int z) {
        if (getBlock(x, y, z) != BlockType.END_PORTAL_FRAME) {
            return false;
        }
        int metadata = getBlockMetadata(x, y, z);
        if ((metadata & END_PORTAL_FRAME_EYE_BIT) != 0) {
            return false;
        }
        setBlock(x, y, z, BlockType.END_PORTAL_FRAME, metadata | END_PORTAL_FRAME_EYE_BIT);
        tryActivateEndPortalAround(x, y, z);
        return true;
    }

    public boolean tryActivateEndPortalAround(int frameX, int frameY, int frameZ) {
        for (int centerX = frameX - 2; centerX <= frameX + 2; centerX++) {
            for (int centerZ = frameZ - 2; centerZ <= frameZ + 2; centerZ++) {
                if (isCompleteEndPortalFrame(centerX, frameY, centerZ)) {
                    for (int x = centerX - 1; x <= centerX + 1; x++) {
                        for (int z = centerZ - 1; z <= centerZ + 1; z++) {
                            setBlock(x, frameY, z, BlockType.END_PORTAL, 0);
                        }
                    }
                    return true;
                }
            }
        }
        return false;
    }

    public boolean isCompleteEndPortalFrame(int centerX, int y, int centerZ) {
        for (int x = centerX - 1; x <= centerX + 1; x++) {
            if (!hasEndPortalEye(x, y, centerZ - 2) || !hasEndPortalEye(x, y, centerZ + 2)) {
                return false;
            }
        }
        for (int z = centerZ - 1; z <= centerZ + 1; z++) {
            if (!hasEndPortalEye(centerX - 2, y, z) || !hasEndPortalEye(centerX + 2, y, z)) {
                return false;
            }
        }
        return true;
    }

    private boolean hasEndPortalEye(int x, int y, int z) {
        return getBlockIfLoaded(x, y, z, BlockType.AIR) == BlockType.END_PORTAL_FRAME
                && (getBlockMetadataIfLoaded(x, y, z, 0) & END_PORTAL_FRAME_EYE_BIT) != 0;
    }

    public boolean isEndPortalAt(int x, int y, int z) {
        return getBlockIfLoaded(x, y, z, BlockType.AIR) == BlockType.END_PORTAL;
    }

    public void ensureEndSpawnPlatform() {
        int cx = (int) Math.floor(DimensionTransferService.END_SPAWN_X);
        int cy = (int) DimensionTransferService.END_SPAWN_Y;
        int cz = (int) Math.floor(DimensionTransferService.END_SPAWN_Z);
        for (int x = cx - 2; x <= cx + 2; x++) {
            for (int z = cz - 2; z <= cz + 2; z++) {
                setBlock(x, cy - 1, z, BlockType.OBSIDIAN, 0);
                for (int y = cy; y <= cy + 3; y++) {
                    setBlock(x, y, z, BlockType.AIR, 0);
                }
            }
        }
    }

    public boolean placeDoor(int x, int y, int z, BlockType type, int facing, AABB playerBox) {
        if (!type.isDoor() || y + 1 >= Chunk.HEIGHT) {
            return false;
        }
        if (!canPlaceBlockAt(x, y, z, type, facing, playerBox)
                || !BlockShape.isReplaceable(getBlock(x, y + 1, z))) {
            return false;
        }
        if (playerBox != null) {
            for (AABB box : getPlacementCollisionBoxes(x, y + 1, z, type, 8)) {
                if (box.intersects(playerBox)) {
                    return false;
                }
            }
        }
        suppressNeighborSupportUpdates = true;
        try {
            setBlock(x, y, z, type, facing);
            setBlock(x, y + 1, z, type, 8);
        } finally {
            suppressNeighborSupportUpdates = false;
        }
        updateNeighborSupport(x, y, z);
        updateNeighborSupport(x, y + 1, z);
        return true;
    }

    public BlockPos placeBed(int x, int y, int z, int facing, AABB playerBox) {
        int[] dir = horizontalDirection(facing);
        int headX = x + dir[0];
        int headZ = z + dir[1];
        if (!canPlaceBlockAt(x, y, z, BlockType.BED, facing, playerBox)
                || !canPlaceBlockAt(headX, y, headZ, BlockType.BED, facing | 8, playerBox)) {
            return null;
        }
        suppressNeighborSupportUpdates = true;
        try {
            setBlock(x, y, z, BlockType.BED, facing);
            setBlock(headX, y, headZ, BlockType.BED, facing | 8);
        } finally {
            suppressNeighborSupportUpdates = false;
        }
        updateNeighborSupport(x, y, z);
        updateNeighborSupport(headX, y, headZ);
        return new BlockPos(x, y, z);
    }

    public boolean tryMergeSlab(int x, int y, int z) {
        if (getBlock(x, y, z) != BlockType.STONE_SLAB) {
            return false;
        }
        setBlock(x, y, z, BlockType.DOUBLE_STONE_SLAB, getBlockMetadata(x, y, z));
        return true;
    }

    public boolean isBlockSupported(int x, int y, int z) {
        BlockType type = getBlock(x, y, z);
        int metadata = getBlockMetadata(x, y, z);
        if (type == BlockType.AIR) {
            return true;
        }
        if (type.isDoor()) {
            if (BlockShape.isDoorUpper(metadata)) {
                return getBlock(x, y - 1, z) == type && !BlockShape.isDoorUpper(getBlockMetadata(x, y - 1, z));
            }
            return BlockShape.canSupportAttached(getBlock(x, y - 1, z)) && getBlock(x, y + 1, z) == type;
        }
        if (type.isBed()) {
            if (!BlockShape.canSupportAttached(getBlock(x, y - 1, z))) {
                return false;
            }
            int facing = metadata & 3;
            int[] dir = horizontalDirection(facing);
            int otherX = BlockShape.isBedHead(metadata) ? x - dir[0] : x + dir[0];
            int otherZ = BlockShape.isBedHead(metadata) ? z - dir[1] : z + dir[1];
            return getBlock(otherX, y, otherZ) == BlockType.BED;
        }
        return BlockShape.canPlaceAt(type, metadata, contextAt(x, y, z));
    }

    private boolean isBlockSupportedIfLoaded(int x, int y, int z) {
        if (!isChunkGeneratedForBlock(x, z)) {
            return true;
        }
        BlockType type = getBlockIfLoaded(x, y, z, BlockType.AIR);
        int metadata = getBlockMetadataIfLoaded(x, y, z, 0);
        if (type == BlockType.AIR) {
            return true;
        }
        if (type.isDoor()) {
            if (BlockShape.isDoorUpper(metadata)) {
                return getBlockIfLoaded(x, y - 1, z, BlockType.AIR) == type
                        && !BlockShape.isDoorUpper(getBlockMetadataIfLoaded(x, y - 1, z, 0));
            }
            return BlockShape.canSupportAttached(getBlockIfLoaded(x, y - 1, z, BlockType.BEDROCK))
                    && getBlockIfLoaded(x, y + 1, z, BlockType.AIR) == type;
        }
        if (type.isBed()) {
            if (!BlockShape.canSupportAttached(getBlockIfLoaded(x, y - 1, z, BlockType.BEDROCK))) {
                return false;
            }
            int facing = metadata & 3;
            int[] dir = horizontalDirection(facing);
            int otherX = BlockShape.isBedHead(metadata) ? x - dir[0] : x + dir[0];
            int otherZ = BlockShape.isBedHead(metadata) ? z - dir[1] : z + dir[1];
            if (!isChunkGeneratedForBlock(otherX, otherZ)) {
                return true;
            }
            return getBlockIfLoaded(otherX, y, otherZ, BlockType.BEDROCK) == BlockType.BED;
        }
        return BlockShape.canPlaceAt(type, metadata, contextAtIfLoaded(x, y, z));
    }

    private void updateNeighborSupport(int x, int y, int z) {
        int[][] dirs = {
                { 1, 0, 0 }, { -1, 0, 0 },
                { 0, 1, 0 }, { 0, -1, 0 },
                { 0, 0, 1 }, { 0, 0, -1 }
        };
        for (int[] dir : dirs) {
            int nx = x + dir[0];
            int ny = y + dir[1];
            int nz = z + dir[2];
            BlockType neighbor = getBlockIfLoaded(nx, ny, nz, BlockType.AIR);
            if (neighbor != BlockType.AIR && neighbor != BlockType.BEDROCK && !isBlockSupportedIfLoaded(nx, ny, nz)) {
                breakBlock(nx, ny, nz, true);
            }
        }
    }

    private boolean breakDoor(int x, int y, int z, BlockType type, int metadata, boolean dropBlock) {
        int lowerY = BlockShape.isDoorUpper(metadata) ? y - 1 : y;
        if (dropBlock) {
            ItemType item = type == BlockType.WOODEN_DOOR ? ItemType.WOODEN_DOOR : ItemType.IRON_DOOR;
            spawnDroppedItem(x + 0.5f, lowerY + 0.5f, z + 0.5f, item, 1);
        }
        suppressNeighborSupportUpdates = true;
        try {
            setBlock(x, lowerY, z, BlockType.AIR, 0);
            if (getBlock(x, lowerY + 1, z) == type) {
                setBlock(x, lowerY + 1, z, BlockType.AIR, 0);
            }
        } finally {
            suppressNeighborSupportUpdates = false;
        }
        updateNeighborSupport(x, lowerY, z);
        updateNeighborSupport(x, lowerY + 1, z);
        return true;
    }

    private boolean breakBed(int x, int y, int z, int metadata, boolean dropBlock) {
        int facing = metadata & 3;
        int[] dir = horizontalDirection(facing);
        int footX = BlockShape.isBedHead(metadata) ? x - dir[0] : x;
        int footZ = BlockShape.isBedHead(metadata) ? z - dir[1] : z;
        int headX = footX + dir[0];
        int headZ = footZ + dir[1];
        if (dropBlock) {
            spawnDroppedItem(footX + 0.5f, y + 0.5f, footZ + 0.5f, ItemType.BED, 1);
        }
        suppressNeighborSupportUpdates = true;
        try {
            setBlock(footX, y, footZ, BlockType.AIR, 0);
            if (getBlock(headX, y, headZ) == BlockType.BED) {
                setBlock(headX, y, headZ, BlockType.AIR, 0);
            }
        } finally {
            suppressNeighborSupportUpdates = false;
        }
        updateNeighborSupport(footX, y, footZ);
        updateNeighborSupport(headX, y, headZ);
        return true;
    }

    public static int[] horizontalDirection(int facing) {
        return switch (facing & 3) {
            case 0 -> new int[] { 0, -1 };
            case 1 -> new int[] { 1, 0 };
            case 2 -> new int[] { 0, 1 };
            default -> new int[] { -1, 0 };
        };
    }

    /**
     * Set block only in the given chunk's bounds (for decoration during
     * generation).
     * Used for trees to avoid placing leaves in chunks that aren't fully generated
     * yet.
     */
    private void setBlockInChunk(Chunk chunk, int worldX, int y, int worldZ, BlockType type) {
        if (y < 0 || y >= Chunk.HEIGHT)
            return;

        int chunkWorldX = chunk.getChunkX() * Chunk.WIDTH;
        int chunkWorldZ = chunk.getChunkZ() * Chunk.DEPTH;

        // Only place block if within this chunk's bounds
        int localX = worldX - chunkWorldX;
        int localZ = worldZ - chunkWorldZ;

        if (localX >= 0 && localX < Chunk.WIDTH && localZ >= 0 && localZ < Chunk.DEPTH) {
            chunk.setBlock(localX, y, localZ, type);
        }
        // Ignore blocks outside this chunk - they'll be generated when that chunk loads
    }

    public void cleanup() {
        // Shutdown async mesh building
        meshBuildPool.shutdownNow();
        chunksBeingBuilt.clear();
        completedMeshTasks.clear();

        for (Chunk chunk : chunks.values()) {
            chunk.cleanup();
        }
        chunks.clear();

        if (atlas != null) {
            atlas.cleanup();
        }
    }

    public long getSeed() {
        return seed;
    }

    public String getGeneratorId() {
        return generatorId;
    }

    public Dimension getDimension() {
        return dimension;
    }

    public StructureGenerator.StructureLocation locateStructure(StructureType type, int originX, int originZ) {
        ReleaseOneWorldGenerator generator = worldGenerator instanceof ReleaseOneWorldGenerator releaseOne
                ? releaseOne
                : new ReleaseOneWorldGenerator(seed, dimension);
        return new StructureGenerator().locate(seed, dimension, type, originX, originZ, generator);
    }

    public com.craftzero.world.BiomeType getReleaseBiome(int x, int z) {
        return worldGenerator != null
                ? worldGenerator.getBiome(x, z)
                : com.craftzero.world.BiomeType.PLAINS;
    }

    // ===== DROPPED ITEMS =====

    /**
     * Spawn a dropped item at the given position.
     */
    public void spawnDroppedItem(float x, float y, float z, ItemType type, int count) {
        if (type == null || count <= 0) {
            return;
        }
        addDroppedItem(new DroppedItem(x, y, z, type, count));
    }

    /**
     * Spawn a thrown item with initial velocity (for Q drop and inventory throw).
     */
    public void spawnThrownItem(float x, float y, float z, ItemType type, int count,
            float velX, float velY, float velZ) {
        if (type == null || count <= 0) {
            return;
        }
        addDroppedItem(new DroppedItem(x, y, z, type, count, velX, velY, velZ));
    }

    public void spawnThrownStack(float x, float y, float z, ItemStack stack,
            float velX, float velY, float velZ) {
        if (stack == null || stack.isEmpty()) {
            return;
        }
        addDroppedItem(new DroppedItem(x, y, z, stack, velX, velY, velZ));
    }

    private void addDroppedItem(DroppedItem item) {
        for (DroppedItem existing : droppedItems) {
            float dx = existing.getX() - item.getX();
            float dy = existing.getY() - item.getY();
            float dz = existing.getZ() - item.getZ();
            if (dx * dx + dy * dy + dz * dz > DROPPED_ITEM_MERGE_RADIUS_SQ) {
                continue;
            }
            if (existing.canMergeWith(item)) {
                existing.mergeWith(item);
                return;
            }
        }
        droppedItems.add(item);
    }

    /**
     * Update all dropped items (physics, animation, despawn).
     */
    public void updateDroppedItems(float deltaTime) {
        Iterator<DroppedItem> iterator = droppedItems.iterator();
        while (iterator.hasNext()) {
            DroppedItem item = iterator.next();
            if (item.update(deltaTime, this)) {
                iterator.remove(); // Despawned
            }
        }
    }

    /**
     * Try to collect nearby items for the player.
     * Only collects if player has inventory space.
     * 
     * @return List of collected items (for adding to inventory)
     */
    public List<DroppedItem> collectNearbyItems(float playerX, float playerY, float playerZ,
            float deltaTime, com.craftzero.main.Player player) {
        List<DroppedItem> collected = new ArrayList<>();
        Iterator<DroppedItem> iterator = droppedItems.iterator();

        while (iterator.hasNext()) {
            DroppedItem item = iterator.next();
            if (item.getAge() < DROPPED_ITEM_PICKUP_DELAY) {
                continue;
            }
            float dx = item.getX() - playerX;
            float dy = item.getY() - playerY;
            float dz = item.getZ() - playerZ;
            if (dx * dx + dy * dy + dz * dz > DROPPED_ITEM_PICKUP_SCAN_RADIUS_SQ) {
                continue;
            }
            if (player.canAddStackToInventory(item.toItemStack())) {
                if (item.tryCollect(playerX, playerY, playerZ, deltaTime)) {
                    collected.add(item);
                    iterator.remove();
                }
            }
        }

        return collected;
    }

    /**
     * Get all dropped items (for rendering).
     */
    public List<DroppedItem> getDroppedItems() {
        return droppedItems;
    }

    public void replaceDroppedItems(List<DroppedItem> items) {
        droppedItems.clear();
        droppedItems.addAll(items);
    }

    public java.util.Collection<Chunk> getLoadedChunks() {
        return chunks.values();
    }

    /**
     * Get the texture atlas (for dropped item rendering).
     */
    public Texture getAtlas() {
        return atlas;
    }

    // ===== ENTITY MANAGEMENT =====

    /**
     * Spawn an entity into the world.
     */
    public void spawnEntity(Entity entity) {
        entity.setWorld(this);
        entitiesToAdd.add(entity);
    }

    public ArrowEntity spawnArrow(float x, float y, float z, float motionX, float motionY, float motionZ,
            Entity shooter, boolean playerOwned, float damage) {
        ArrowEntity arrow = new ArrowEntity(x, y, z, motionX, motionY, motionZ, shooter, playerOwned, damage);
        spawnEntity(arrow);
        return arrow;
    }

    public FireballEntity spawnFireball(float x, float y, float z, float motionX, float motionY, float motionZ,
            Entity shooter, boolean explosive) {
        FireballEntity fireball = new FireballEntity(x, y, z, motionX, motionY, motionZ, shooter, explosive);
        spawnEntity(fireball);
        return fireball;
    }

    public SplashPotionEntity spawnSplashPotion(float x, float y, float z, float motionX, float motionY, float motionZ,
            Entity shooter, PotionData potionData) {
        SplashPotionEntity potion = new SplashPotionEntity(x, y, z, motionX, motionY, motionZ, shooter, potionData);
        spawnEntity(potion);
        return potion;
    }

    public MinecartEntity spawnMinecart(float x, float y, float z, MinecartEntity.CartKind kind) {
        MinecartEntity cart = switch (kind) {
            case CHEST -> new ChestMinecartEntity(x, y, z);
            case FURNACE -> new FurnaceMinecartEntity(x, y, z);
            default -> new MinecartEntity(x, y, z, MinecartEntity.CartKind.RIDEABLE);
        };
        spawnEntity(cart);
        return cart;
    }

    public boolean placeMinecartOnRail(int x, int y, int z, ItemType itemType) {
        BlockType rail = getBlockIfLoaded(x, y, z, BlockType.AIR);
        if (!RailShapeResolver.isRail(rail)) {
            return false;
        }
        MinecartEntity.CartKind kind = itemType == ItemType.CHEST_MINECART ? MinecartEntity.CartKind.CHEST
                : itemType == ItemType.FURNACE_MINECART ? MinecartEntity.CartKind.FURNACE
                        : MinecartEntity.CartKind.RIDEABLE;
        spawnMinecart(x + 0.5f, y + 0.1f, z + 0.5f, kind);
        return true;
    }

    public PrimedTntEntity spawnPrimedTnt(float x, float y, float z, int fuseTicks,
            float motionX, float motionY, float motionZ) {
        PrimedTntEntity tnt = new PrimedTntEntity(x, y, z, fuseTicks);
        tnt.setMotion(motionX, motionY, motionZ);
        spawnEntity(tnt);
        return tnt;
    }

    public PrimedTntEntity primeTnt(int x, int y, int z, int fuseTicks) {
        if (getBlockIfLoaded(x, y, z, BlockType.AIR) != BlockType.TNT) {
            return null;
        }
        setBlockIfLoaded(x, y, z, BlockType.AIR, 0);
        return spawnPrimedTnt(x + 0.5f, y, z + 0.5f, fuseTicks,
                (random.nextFloat() - 0.5f) * 0.04f, 0.2f, (random.nextFloat() - 0.5f) * 0.04f);
    }

    public void spawnExperience(float x, float y, float z, int amount) {
        int remaining = Math.max(0, amount);
        while (remaining > 0) {
            int value = ExperienceOrbEntity.getOrbValue(remaining);
            remaining -= value;
            spawnEntity(new ExperienceOrbEntity(x, y, z, value));
        }
    }

    /**
     * Remove an entity from the world.
     */
    public void removeEntity(Entity entity) {
        entitiesToRemove.add(entity);
    }

    /**
     * Update all entities in the world.
     */
    public void updateEntities(float deltaTime) {
        Entity generated;
        while ((generated = generatedEntities.poll()) != null) {
            generated.setWorld(this);
            entitiesToAdd.add(generated);
        }

        // Add pending entities
        entities.addAll(entitiesToAdd);
        entitiesToAdd.clear();

        // Update all entities
        Iterator<Entity> iterator = entities.iterator();
        while (iterator.hasNext()) {
            Entity entity = iterator.next();

            entity.tick();
            entity.updatePhysics(deltaTime);

            // Remove dead entities
            if (entity.isRemoved()) {
                entitiesToRemove.add(entity);
            }
        }

        // Remove pending entities
        entities.removeAll(entitiesToRemove);
        entitiesToRemove.clear();
    }

    /**
     * Get all entities in the world.
     */
    public List<Entity> getEntities() {
        return entities;
    }

    public boolean hasEntityIntersecting(float minX, float minY, float minZ,
            float maxX, float maxY, float maxZ, boolean includeDroppedItems) {
        AABB area = new AABB(minX, minY, minZ, maxX, maxY, maxZ);
        for (Entity entity : entities) {
            if (!entity.isRemoved() && entity.getBoundingBox().intersects(area)) {
                return true;
            }
        }
        for (Entity entity : entitiesToAdd) {
            if (!entity.isRemoved() && entity.getBoundingBox().intersects(area)) {
                return true;
            }
        }
        if (includeDroppedItems) {
            for (DroppedItem item : droppedItems) {
                if (item.getX() >= minX && item.getX() <= maxX
                        && item.getY() >= minY && item.getY() <= maxY
                        && item.getZ() >= minZ && item.getZ() <= maxZ) {
                    return true;
                }
            }
        }
        if (player != null && player.getBoundingBox().intersects(area)) {
            return true;
        }
        return false;
    }

    public boolean hasMinecartAt(int x, int y, int z) {
        AABB area = new AABB(x, y, z, x + 1, y + 1, z + 1);
        for (Entity entity : entities) {
            if (entity instanceof MinecartEntity && !entity.isRemoved() && entity.getBoundingBox().intersects(area)) {
                return true;
            }
        }
        for (Entity entity : entitiesToAdd) {
            if (entity instanceof MinecartEntity && !entity.isRemoved() && entity.getBoundingBox().intersects(area)) {
                return true;
            }
        }
        return false;
    }

    public boolean hasEntityOfType(Class<? extends Entity> type) {
        if (type == null) {
            return false;
        }
        for (Entity entity : entities) {
            if (type.isInstance(entity) && !entity.isRemoved()) {
                return true;
            }
        }
        for (Entity entity : entitiesToAdd) {
            if (type.isInstance(entity) && !entity.isRemoved()) {
                return true;
            }
        }
        for (Entity entity : generatedEntities) {
            if (type.isInstance(entity) && !entity.isRemoved()) {
                return true;
            }
        }
        return false;
    }

    public void replaceEntities(Collection<? extends Entity> restoredEntities) {
        entities.clear();
        entitiesToAdd.clear();
        entitiesToRemove.clear();
        if (restoredEntities == null) {
            return;
        }
        for (Entity entity : restoredEntities) {
            if (entity == null || entity.isRemoved()) {
                continue;
            }
            entity.setWorld(this);
            entities.add(entity);
        }
    }

    /**
     * Set the player reference (for AI targeting).
     */
    public void setPlayer(com.craftzero.main.Player player) {
        this.player = player;
    }

    /**
     * Get the player reference.
     */
    public com.craftzero.main.Player getPlayer() {
        return player;
    }

    /**
     * Set the day/night cycle manager reference.
     */
    public void setDayCycleManager(DayCycleManager manager) {
        this.dayCycleManager = manager;
    }

    /**
     * Get the day/night cycle manager.
     */
    public DayCycleManager getDayCycleManager() {
        return dayCycleManager;
    }

    /**
     * Create an explosion at the specified location.
     * Destroys blocks within radius and damages entities.
     * 
     * @param x     Center X
     * @param y     Center Y
     * @param z     Center Z
     * @param power Explosion power (radius = power * 1.5)
     */
    public void explode(float x, float y, float z, float power) {
        float radius = power * 1.5f;
        int intRadius = (int) Math.ceil(radius);

        // Destroy blocks
        for (int bx = (int) x - intRadius; bx <= (int) x + intRadius; bx++) {
            for (int by = (int) y - intRadius; by <= (int) y + intRadius; by++) {
                for (int bz = (int) z - intRadius; bz <= (int) z + intRadius; bz++) {
                    float dx = bx + 0.5f - x;
                    float dy = by + 0.5f - y;
                    float dz = bz + 0.5f - z;
                    float dist = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);

                    if (dist <= radius) {
                        BlockType block = getBlock(bx, by, bz);
                        // Don't destroy bedrock or air
                        if (block != BlockType.BEDROCK && block != BlockType.AIR) {
                            // Random chance to drop (30%) - skip liquids; containers drop contents
                            if (random.nextFloat() < 0.3f) {
                                breakBlock(bx, by, bz, true);
                            } else {
                                TileEntity tile = removeTileEntity(bx, by, bz);
                                if (tile != null) {
                                    for (ItemStack stack : tile.getDrops()) {
                                        spawnThrownStack(bx + 0.5f, by + 0.5f, bz + 0.5f, stack,
                                                (random.nextFloat() - 0.5f) * 0.25f,
                                                0.2f,
                                                (random.nextFloat() - 0.5f) * 0.25f);
                                    }
                                }
                                setBlock(bx, by, bz, BlockType.AIR);
                            }
                        }
                    }
                }
            }
        }

        // Damage entities
        for (Entity entity : entities) {
            float dx = entity.getX() - x;
            float dy = entity.getY() - y;
            float dz = entity.getZ() - z;
            float dist = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);

            if (dist <= radius * 2) {
                float damage = (1.0f - dist / (radius * 2)) * power * 7;
                if (entity instanceof com.craftzero.entity.LivingEntity living) {
                    float exposure = ExplosionExposure.sample(this, x, y, z, living.getBoundingBox());
                    damage *= exposure;
                    if (damage > 0.0f && living.damage(damage,
                            DamageSource.point(DamageSource.Type.EXPLOSION, x, y, z, 0.0f, 0.0f))) {
                        // Knockback away from explosion
                        if (dist > 0.1f) {
                            float knockback = (1.0f - dist / (radius * 2)) * power * 0.5f * exposure;
                            entity.addMotion(
                                    (dx / dist) * knockback,
                                    0.4f * knockback,
                                    (dz / dist) * knockback);
                        }
                    }
                }
            }
        }

        // Damage player if nearby
        if (player != null) {
            float dx = player.getPosition().x - x;
            float dy = player.getPosition().y - y;
            float dz = player.getPosition().z - z;
            float dist = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);

            if (dist <= radius * 2) {
                float exposure = ExplosionExposure.sample(this, x, y, z, player.getBoundingBox());
                float falloff = 1.0f - dist / (radius * 2);
                float damage = CombatRules.easyExplosionDamage(falloff * power * 7 * exposure);
                if (damage > 0.0f) {
                    if (dist > 0.1f) {
                        float knockback = falloff * power * 0.5f * exposure;
                        player.hurt(damage, DamageSource.point(DamageSource.Type.EXPLOSION,
                                x, y, z, knockback, 0.4f * knockback));
                    } else {
                        player.hurt(damage, DamageSource.point(DamageSource.Type.EXPLOSION,
                                x, y, z, 0.0f, 0.4f * exposure));
                    }
                }
            }
        }
    }

    /**
     * Biome types for terrain generation.
     */
    private enum BiomeType {
        PLAINS,
        FOREST,
        HILLS,
        MOUNTAINS
    }
}
