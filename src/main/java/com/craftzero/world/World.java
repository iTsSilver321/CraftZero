package com.craftzero.world;

import com.craftzero.graphics.Camera;
import com.craftzero.graphics.Frustum;
import com.craftzero.graphics.Renderer;
import com.craftzero.graphics.Texture;
import com.craftzero.entity.DroppedItem;
import com.craftzero.entity.Entity;
import com.craftzero.math.Noise;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
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
public class World {

    private static final int RENDER_DISTANCE = 12; // Chunks in each direction
    private static final int UNLOAD_DISTANCE = RENDER_DISTANCE + 2; // Chunks beyond this are unloaded
    private static final int MAX_MESH_UPLOADS_PER_FRAME = 3; // Limit GPU uploads per frame
    private static final int MAX_GENERATES_PER_FRAME = 2; // Limit async terrain generation submits
    private static final int MAX_LIGHTINGS_PER_FRAME = 2; // Limit async lighting submits
    private static final int MAX_MESHES_PER_FRAME = 2; // Limit async mesh building submits
    private static final int SEA_LEVEL = 62;
    private static final int BASE_HEIGHT = 64;

    private final ConcurrentHashMap<Long, Chunk> chunks;
    private final Noise terrainNoise;
    private final Noise biomeNoise;
    private final Noise caveNoise;
    private final Noise treeNoise;
    private final CaveGenerator caveGenerator;
    private final RavineGenerator ravineGenerator;
    private final OreGenerator oreGenerator;
    private final long seed;
    private final Random random;

    private Texture atlas;

    // Dropped items in the world
    private final List<DroppedItem> droppedItems;

    // Living entities (mobs)
    private final List<Entity> entities;
    private final List<Entity> entitiesToAdd;
    private final List<Entity> entitiesToRemove;

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

    // Pre-calculated spiral loading order (center-out, sorted by distance)
    private static final int[][] SPIRAL_OFFSETS;
    static {
        // Generate offsets for render distance
        int maxDist = 14; // Slightly more than RENDER_DISTANCE
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

        ChunkMeshTask(Chunk chunk, ChunkMeshData meshData) {
            this.chunk = chunk;
            this.meshData = meshData;
        }
    }

    public World(long seed) {
        this.seed = seed;
        this.chunks = new ConcurrentHashMap<>();
        this.terrainNoise = new Noise(seed);
        this.biomeNoise = new Noise(seed + 1);
        this.caveNoise = new Noise(seed + 2);
        this.treeNoise = new Noise(seed + 3);
        this.caveGenerator = new CaveGenerator();
        this.ravineGenerator = new RavineGenerator();
        this.oreGenerator = new OreGenerator();
        this.random = new Random(seed);
        this.droppedItems = new ArrayList<>();
        this.entities = new ArrayList<>();
        this.entitiesToAdd = new ArrayList<>();
        this.entitiesToRemove = new ArrayList<>();

        // Async mesh building with 4 worker threads
        this.meshBuildPool = Executors.newFixedThreadPool(4, r -> {
            Thread t = new Thread(r, "ChunkMeshBuilder");
            t.setDaemon(true);
            return t;
        });
        this.chunksBeingBuilt = ConcurrentHashMap.newKeySet();
        this.completedMeshTasks = new ConcurrentLinkedQueue<>();

        // Frustum culling
        this.frustum = new Frustum();
        this.viewProjection = new org.joml.Matrix4f();
    }

    public void init() throws Exception {
        atlas = new Texture("/textures/terrain/Terrain.png");
        System.out.println("World initialized with seed: " + seed);
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
            chunk = new Chunk(chunkX, chunkZ);
            chunks.put(key, chunk);
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
            chunk = new Chunk(chunkX, chunkZ);
            chunks.put(key, chunk);
        }

        // Force synchronous generation if needed
        if (chunk.getState() == Chunk.ChunkState.EMPTY) {
            chunk.setState(Chunk.ChunkState.GENERATING);
            generateChunkTerrain(chunk, chunkX, chunkZ);
            chunk.setState(Chunk.ChunkState.GENERATED);
        }

        return chunk;
    }

    private void generateChunkTerrain(Chunk chunk, int chunkX, int chunkZ) {
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

        // Underground - add ore generation
        // Underground - add ore generation
        if (y < height - 4) {
            // Diamond (rare, very deep)
            if (y < 16) {
                double oreNoise = caveNoise.noise3D(x * 0.1, y * 0.1, z * 0.1);
                if (oreNoise > 0.82) {
                    return BlockType.DIAMOND_ORE;
                }
            }

            // Gold (rare, deep)
            if (y < 32) {
                double oreNoise = caveNoise.noise3D(x * 0.15 + 100, y * 0.15, z * 0.15);
                if (oreNoise > 0.78) {
                    return BlockType.GOLD_ORE;
                }
            }

            // Iron (common, anywhere underground)
            double oreNoise = caveNoise.noise3D(x * 0.12 + 200, y * 0.12, z * 0.12);
            if (oreNoise > 0.70) {
                return BlockType.IRON_ORE;
            }

            // Coal (very common)
            oreNoise = caveNoise.noise3D(x * 0.1 + 300, y * 0.1, z * 0.1);
            if (oreNoise > 0.65) {
                return BlockType.COAL_ORE;
            }

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

            // Apply mesh data on main thread (GPU upload)
            task.chunk.applyMeshData(task.meshData);
            task.chunk.setState(Chunk.ChunkState.READY);
            chunksBeingBuilt.remove(chunkKey(task.chunk.getChunkX(), task.chunk.getChunkZ()));
            uploadsThisFrame++;
        }

        // Step 2: Ensure all chunks in range exist and set up neighbors
        for (int dx = -RENDER_DISTANCE - 1; dx <= RENDER_DISTANCE + 1; dx++) {
            for (int dz = -RENDER_DISTANCE - 1; dz <= RENDER_DISTANCE + 1; dz++) {
                int chunkX = playerChunkX + dx;
                int chunkZ = playerChunkZ + dz;
                long key = chunkKey(chunkX, chunkZ);

                // Get or create chunk (does NOT generate terrain anymore)
                Chunk chunk = chunks.get(key);
                if (chunk == null) {
                    chunk = new Chunk(chunkX, chunkZ);
                    chunks.put(key, chunk);
                }

                // Set up neighbor references
                chunk.setNeighbors(
                        chunks.get(chunkKey(chunkX, chunkZ - 1)),
                        chunks.get(chunkKey(chunkX, chunkZ + 1)),
                        chunks.get(chunkKey(chunkX + 1, chunkZ)),
                        chunks.get(chunkKey(chunkX - 1, chunkZ)));
            }
        }

        // Step 3: Progress chunks through states (with per-frame rate limiting)
        // Uses spiral order: closest chunks to player are processed first
        int generationsThisFrame = 0;
        int lightingsThisFrame = 0;
        int meshesThisFrame = 0;

        for (int[] offset : SPIRAL_OFFSETS) {
            int dx = offset[0];
            int dz = offset[1];

            // Skip if outside render distance
            if (Math.abs(dx) > RENDER_DISTANCE || Math.abs(dz) > RENDER_DISTANCE)
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
                if (chunk.hasAllNeighborsAtLeast(Chunk.ChunkState.GENERATED)) {
                    if (lightingsThisFrame >= MAX_LIGHTINGS_PER_FRAME)
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
                    meshesThisFrame++;

                    chunk.setState(Chunk.ChunkState.MESHING);
                    chunksBeingBuilt.add(key);
                    final Chunk chunkRef = chunk;
                    meshBuildPool.submit(() -> {
                        try {
                            ChunkMeshData meshData = ChunkMeshBuilder.buildMeshData(chunkRef);
                            completedMeshTasks.offer(new ChunkMeshTask(chunkRef, meshData));
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
                    meshesThisFrame++;

                    chunk.setState(Chunk.ChunkState.MESHING);
                    chunksBeingBuilt.add(key);
                    final Chunk chunkRef = chunk;
                    meshBuildPool.submit(() -> {
                        try {
                            chunkRef.calculateSkyLight();
                            ChunkMeshData meshData = ChunkMeshBuilder.buildMeshData(chunkRef);
                            completedMeshTasks.offer(new ChunkMeshTask(chunkRef, meshData));
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

            if (Math.abs(dx) > UNLOAD_DISTANCE || Math.abs(dz) > UNLOAD_DISTANCE) {
                toUnload.add(entry.getKey());
            }
        }

        for (Long key : toUnload) {
            Chunk chunk = chunks.remove(key);
            if (chunk != null) {
                chunk.cleanup();
            }
            chunksBeingBuilt.remove(key);
        }
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

        // --- PASS 1: OPAQUE ---
        renderer.beginRender(camera);

        // Explicitly ensure solid rendering state for opaque pass
        org.lwjgl.opengl.GL11.glEnable(org.lwjgl.opengl.GL11.GL_DEPTH_TEST);
        org.lwjgl.opengl.GL11.glDepthMask(true);
        org.lwjgl.opengl.GL11.glDisable(org.lwjgl.opengl.GL11.GL_BLEND);
        org.lwjgl.opengl.GL11.glEnable(org.lwjgl.opengl.GL11.GL_CULL_FACE);

        for (int dx = -RENDER_DISTANCE; dx <= RENDER_DISTANCE; dx++) {
            for (int dz = -RENDER_DISTANCE; dz <= RENDER_DISTANCE; dz++) {
                int chunkX = playerChunkX + dx;
                int chunkZ = playerChunkZ + dz;

                // Frustum cull
                int worldX = chunkX * Chunk.WIDTH;
                int worldZ = chunkZ * Chunk.DEPTH;
                if (!frustum.isChunkVisible(worldX, worldZ)) {
                    continue;
                }

                Chunk chunk = chunks.get(chunkKey(chunkX, chunkZ));

                if (chunk != null && chunk.getMesh() != null && !chunk.isEmpty()) {
                    renderer.renderMesh(chunk.getMesh());
                }
            }
        }
        renderer.endRender(); // End Opaque Pass

        // --- MID-PASS ACTION (Highlight) ---
        if (midPassAction != null) {
            midPassAction.run();
        }

        // --- PASS 2: TRANSPARENT ---
        // Re-bind atlas in case midPassAction unbound it (fixes black water bug)
        atlas.bind(0);

        renderer.beginRender(camera);

        // CRITICAL: Re-enable blending for water/glass (BlockHighlight might have
        // disabled it)
        org.lwjgl.opengl.GL11.glEnable(org.lwjgl.opengl.GL11.GL_BLEND);
        org.lwjgl.opengl.GL11.glBlendFunc(org.lwjgl.opengl.GL11.GL_SRC_ALPHA,
                org.lwjgl.opengl.GL11.GL_ONE_MINUS_SRC_ALPHA);

        renderer.setDepthMask(false); // Disable depth writing
        org.lwjgl.opengl.GL11.glDisable(org.lwjgl.opengl.GL11.GL_CULL_FACE);

        for (int dx = -RENDER_DISTANCE; dx <= RENDER_DISTANCE; dx++) {
            for (int dz = -RENDER_DISTANCE; dz <= RENDER_DISTANCE; dz++) {
                int chunkX = playerChunkX + dx;
                int chunkZ = playerChunkZ + dz;

                // Frustum cull
                int worldX = chunkX * Chunk.WIDTH;
                int worldZ = chunkZ * Chunk.DEPTH;
                if (!frustum.isChunkVisible(worldX, worldZ)) {
                    continue;
                }

                Chunk chunk = chunks.get(chunkKey(chunkX, chunkZ));

                if (chunk != null && !chunk.isEmpty()) {
                    if (chunk.getTransparentMesh() != null) {
                        renderer.renderMesh(chunk.getTransparentMesh());
                    }
                }
            }
        }

        org.lwjgl.opengl.GL11.glEnable(org.lwjgl.opengl.GL11.GL_CULL_FACE);
        renderer.setDepthMask(true); // Re-enable depth writing
        renderer.endRender();

        atlas.unbind();
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
        if (y < 0 || y >= Chunk.HEIGHT) {
            return;
        }

        int chunkX = Math.floorDiv(x, Chunk.WIDTH);
        int chunkZ = Math.floorDiv(z, Chunk.DEPTH);

        Chunk chunk = getChunkNow(chunkX, chunkZ);

        int localX = Math.floorMod(x, Chunk.WIDTH);
        int localZ = Math.floorMod(z, Chunk.DEPTH);

        chunk.setBlock(localX, y, localZ, type);

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

    // ===== DROPPED ITEMS =====

    /**
     * Spawn a dropped item at the given position.
     */
    public void spawnDroppedItem(float x, float y, float z, BlockType type, int count) {
        if (type == null || type == BlockType.AIR || count <= 0) {
            return;
        }
        droppedItems.add(new DroppedItem(x, y, z, type, count));
    }

    /**
     * Spawn a thrown item with initial velocity (for Q drop and inventory throw).
     */
    public void spawnThrownItem(float x, float y, float z, BlockType type, int count,
            float velX, float velY, float velZ) {
        if (type == null || type == BlockType.AIR || count <= 0) {
            return;
        }
        droppedItems.add(new DroppedItem(x, y, z, type, count, velX, velY, velZ));
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
            // Check if player can hold this item before trying to collect
            if (player.canAddToInventory(item.getBlockType(), item.getCount())) {
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
                            // Random chance to drop (30%) - skip liquids
                            if (random.nextFloat() < 0.3f && block != BlockType.WATER && block != BlockType.LAVA) {
                                spawnDroppedItem(bx + 0.5f, by + 0.5f, bz + 0.5f, block, 1);
                            }
                            setBlock(bx, by, bz, BlockType.AIR);
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
                    living.damage(damage, null);
                    // Knockback away from explosion
                    if (dist > 0.1f) {
                        float knockback = (1.0f - dist / (radius * 2)) * power * 0.5f;
                        entity.addMotion(
                                (dx / dist) * knockback,
                                0.4f * knockback,
                                (dz / dist) * knockback);
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
                float damage = (1.0f - dist / (radius * 2)) * power * 7;
                player.getStats().damage(damage);
                // Knockback
                if (dist > 0.1f) {
                    float knockback = (1.0f - dist / (radius * 2)) * power * 0.5f;
                    player.getVelocity().x += (dx / dist) * knockback;
                    player.getVelocity().y += 0.4f * knockback;
                    player.getVelocity().z += (dz / dist) * knockback;
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
