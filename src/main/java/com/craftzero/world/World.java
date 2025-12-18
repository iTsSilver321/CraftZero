package com.craftzero.world;

import com.craftzero.graphics.Camera;
import com.craftzero.graphics.Renderer;
import com.craftzero.graphics.Texture;
import com.craftzero.entity.DroppedItem;
import com.craftzero.math.Noise;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * World manager that handles chunk loading, generation, and rendering.
 * Implements procedural terrain generation with biomes.
 */
public class World {

    private static final int RENDER_DISTANCE = 8; // Chunks in each direction
    private static final int SEA_LEVEL = 62;
    private static final int BASE_HEIGHT = 64;

    private final Map<Long, Chunk> chunks;
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

    public World(long seed) {
        this.seed = seed;
        this.chunks = new HashMap<>();
        this.terrainNoise = new Noise(seed);
        this.biomeNoise = new Noise(seed + 1);
        this.caveNoise = new Noise(seed + 2);
        this.treeNoise = new Noise(seed + 3);
        this.caveGenerator = new CaveGenerator();
        this.ravineGenerator = new RavineGenerator();
        this.oreGenerator = new OreGenerator();
        this.random = new Random(seed);
        this.droppedItems = new ArrayList<>();
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
     * Get or generate a chunk at the specified coordinates.
     */
    public Chunk getChunk(int chunkX, int chunkZ) {
        long key = chunkKey(chunkX, chunkZ);
        Chunk chunk = chunks.get(key);

        if (chunk == null) {
            // Create and register chunk BEFORE generation to support cross-chunk decoration
            chunk = new Chunk(chunkX, chunkZ);
            chunks.put(key, chunk);
            generateChunkTerrain(chunk, chunkX, chunkZ);
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

        // Pass 2: Trees (Can overwrite neighbors safely if neighbors are generated)
        for (int x = 0; x < Chunk.WIDTH; x++) {
            for (int z = 0; z < Chunk.DEPTH; z++) {
                int globalX = worldX + x;
                int globalZ = worldZ + z;

                double biomeValue = biomeNoise.octaveNoise2D(globalX * 0.005, globalZ * 0.005, 4, 0.5);
                int height = calculateHeight(globalX, globalZ, biomeValue);
                BiomeType biome = getBiome(biomeValue);

                // Generate trees for forest biome
                if (biome == BiomeType.FOREST && height > SEA_LEVEL) {
                    double treeValue = treeNoise.noise2D(globalX * 0.5, globalZ * 0.5);
                    // Reduced density: threshold 0.7
                    if (treeValue > 0.7) {
                        // Spacing Check: Local Maximum (Radius 8 = ~16 blocks spacing)
                        // Significantly increased to prevent clumping
                        if (isLocalMaximum(globalX, globalZ, 8)) {
                            // Valid placement check (Must be on grass)
                            if (getBlock(globalX, height, globalZ) == BlockType.GRASS) {
                                generateTree(chunk, globalX, height + 1, globalZ);
                            }
                        }
                    }
                }
            }
        }
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

    private void generateTree(Chunk chunk, int x, int y, int z) {
        // x, z are GLOBAL coordinates
        // Taller trees: 5 to 7 blocks tall
        int trunkHeight = 5 + random.nextInt(3);

        // Trunk
        for (int i = 0; i < trunkHeight; i++) {
            setBlock(x, y + i, z, BlockType.OAK_LOG);
        }

        // Leaves - Fuller & Rounded pattern
        int h = trunkHeight;

        // Loop: Bottom (h-2), Middle (h-1), Top (h), Peak (h+1)
        // Raised canopy - starts at h-2 instead of h-3
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
                    // MC Oak logic: Cross for peak, 3x3 for top, 5x5-corners for bottom
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

                    // Force full square generation minus corners
                    if (getBlock(lx, ly, lz) != BlockType.OAK_LOG) {
                        setBlock(lx, ly, lz, BlockType.LEAVES);
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
     * Generate a tree at the specified position.
     */

    /**
     * Update chunks around the player.
     */
    public void update(Camera camera) {
        int playerChunkX = (int) Math.floor(camera.getPosition().x / Chunk.WIDTH);
        int playerChunkZ = (int) Math.floor(camera.getPosition().z / Chunk.DEPTH);

        // Load/generate chunks around player
        for (int dx = -RENDER_DISTANCE; dx <= RENDER_DISTANCE; dx++) {
            for (int dz = -RENDER_DISTANCE; dz <= RENDER_DISTANCE; dz++) {
                int chunkX = playerChunkX + dx;
                int chunkZ = playerChunkZ + dz;

                Chunk chunk = getChunk(chunkX, chunkZ);

                // Set up neighbor references for seamless rendering
                chunk.setNeighbors(
                        chunks.get(chunkKey(chunkX, chunkZ - 1)),
                        chunks.get(chunkKey(chunkX, chunkZ + 1)),
                        chunks.get(chunkKey(chunkX + 1, chunkZ)),
                        chunks.get(chunkKey(chunkX - 1, chunkZ)));

                // Build mesh if dirty
                if (chunk.isDirty()) {
                    chunk.buildMesh();
                }
            }
        }
    }

    /**
     * Render all visible chunks.
     */
    public void render(Renderer renderer, Camera camera) {
        int playerChunkX = (int) Math.floor(camera.getPosition().x / Chunk.WIDTH);
        int playerChunkZ = (int) Math.floor(camera.getPosition().z / Chunk.DEPTH);

        atlas.bind(0);
        renderer.beginRender(camera);

        for (int dx = -RENDER_DISTANCE; dx <= RENDER_DISTANCE; dx++) {
            for (int dz = -RENDER_DISTANCE; dz <= RENDER_DISTANCE; dz++) {
                int chunkX = playerChunkX + dx;
                int chunkZ = playerChunkZ + dz;

                Chunk chunk = chunks.get(chunkKey(chunkX, chunkZ));

                if (chunk != null && chunk.getMesh() != null && !chunk.isEmpty()) {
                    renderer.renderMesh(chunk.getMesh());
                }
            }
        }

        renderer.endRender();

        // Transparent pass (Water, Glass, Leaves)
        renderer.beginRender(camera);
        renderer.setDepthMask(false); // Disable depth writing
        org.lwjgl.opengl.GL11.glDisable(org.lwjgl.opengl.GL11.GL_CULL_FACE); // Render back faces (for water surface
                                                                             // from below)

        for (int dx = -RENDER_DISTANCE; dx <= RENDER_DISTANCE; dx++) {
            for (int dz = -RENDER_DISTANCE; dz <= RENDER_DISTANCE; dz++) {
                int chunkX = playerChunkX + dx;
                int chunkZ = playerChunkZ + dz;

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

        Chunk chunk = getChunk(chunkX, chunkZ);

        int localX = Math.floorMod(x, Chunk.WIDTH);
        int localZ = Math.floorMod(z, Chunk.DEPTH);

        return chunk.getBlock(localX, y, localZ);
    }

    /**
     * Get sky light level at world coordinates (0-15).
     */
    public int getSkyLight(int x, int y, int z) {
        if (y < 0 || y >= Chunk.HEIGHT) {
            return y >= Chunk.HEIGHT ? 15 : 0; // Above world = full light, below = none
        }

        int chunkX = Math.floorDiv(x, Chunk.WIDTH);
        int chunkZ = Math.floorDiv(z, Chunk.DEPTH);

        Chunk chunk = chunks.get(chunkKey(chunkX, chunkZ));
        if (chunk == null) {
            return 15; // Unloaded chunks default to full light
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

        Chunk chunk = getChunk(chunkX, chunkZ);

        int localX = Math.floorMod(x, Chunk.WIDTH);
        int localZ = Math.floorMod(z, Chunk.DEPTH);

        chunk.setBlock(localX, y, localZ, type);

        // Mark neighboring chunks as dirty if on border
        if (localX == 0) {
            Chunk neighbor = chunks.get(chunkKey(chunkX - 1, chunkZ));
            if (neighbor != null)
                neighbor.setDirty(true);
        }
        if (localX == Chunk.WIDTH - 1) {
            Chunk neighbor = chunks.get(chunkKey(chunkX + 1, chunkZ));
            if (neighbor != null)
                neighbor.setDirty(true);
        }
        if (localZ == 0) {
            Chunk neighbor = chunks.get(chunkKey(chunkX, chunkZ - 1));
            if (neighbor != null)
                neighbor.setDirty(true);
        }
        if (localZ == Chunk.DEPTH - 1) {
            Chunk neighbor = chunks.get(chunkKey(chunkX, chunkZ + 1));
            if (neighbor != null)
                neighbor.setDirty(true);
        }
    }

    public void cleanup() {
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
