package com.craftzero.world;

import com.craftzero.entity.EndCrystalEntity;
import com.craftzero.entity.mob.EnderDragon;

import com.craftzero.math.Noise;

import java.util.Random;

/**
 * Release 1.0-style generator scaffold with chunk-safe Overworld population.
 */
public final class ReleaseOneWorldGenerator implements WorldGenerator {
    public static final int SEA_LEVEL = 63;
    static final int LAVA_LEVEL = 11;

    private final long seed;
    private final Dimension dimension;
    private final Noise terrainNoise;
    private final Noise detailNoise;
    private final Noise biomeNoise;
    private final OverworldDensityField densityField;
    private final FeaturePlanner featurePlanner;
    private final CaveGenerator caveGenerator = new CaveGenerator();
    private final RavineGenerator ravineGenerator = new RavineGenerator();
    private final OreGenerator oreGenerator = new OreGenerator();
    private final DungeonGenerator dungeonGenerator = new DungeonGenerator();
    private final StructureGenerator structureGenerator = new StructureGenerator();

    public ReleaseOneWorldGenerator(long seed, Dimension dimension) {
        this.seed = seed;
        this.dimension = dimension == null ? Dimension.OVERWORLD : dimension;
        this.terrainNoise = new Noise(seed);
        this.detailNoise = new Noise(seed ^ 0x5DEECE66DL);
        this.biomeNoise = new Noise(seed + 1009L);
        this.densityField = new OverworldDensityField(terrainNoise, detailNoise, this::getBiome);
        this.featurePlanner = new FeaturePlanner(seed, this);
    }

    public SpawnPoint findSafeSpawn() {
        if (dimension != Dimension.OVERWORLD) {
            return new SpawnPoint(0, 80, 0);
        }
        int maxRadius = 384;
        for (int radius = 0; radius <= maxRadius; radius += 8) {
            for (int x = -radius; x <= radius; x += 8) {
                SpawnPoint spawn = safeSpawnAtEdge(x, -radius, radius);
                if (spawn != null) {
                    return spawn;
                }
                spawn = safeSpawnAtEdge(x, radius, radius);
                if (spawn != null) {
                    return spawn;
                }
            }
            for (int z = -radius + 8; z <= radius - 8; z += 8) {
                SpawnPoint spawn = safeSpawnAtEdge(-radius, z, radius);
                if (spawn != null) {
                    return spawn;
                }
                spawn = safeSpawnAtEdge(radius, z, radius);
                if (spawn != null) {
                    return spawn;
                }
            }
        }
        return new SpawnPoint(0, 80, 0);
    }

    private SpawnPoint safeSpawnAtEdge(int blockX, int blockZ, int radius) {
        if (radius == 0 && (blockX != 0 || blockZ != 0)) {
            return null;
        }
        BiomeType biome = getBiome(blockX, blockZ);
        if (biome.isOceanic() || biome == BiomeType.RIVER || biome == BiomeType.FROZEN_RIVER
                || biome == BiomeType.DESERT_HILLS || biome == BiomeType.EXTREME_HILLS
                || biome == BiomeType.ICE_MOUNTAINS) {
            return null;
        }
        int top = terrainTopY(blockX, blockZ, biome);
        if (top < SEA_LEVEL || top > 92) {
            return null;
        }
        BlockType surface = baseBlockAt(blockX, top, blockZ, biome, top, fillerDepth(blockX, blockZ));
        if (surface.isWater() || surface == BlockType.ICE || !surface.isSolid()) {
            return null;
        }
        if (baseBlockAt(blockX, top + 1, blockZ) != BlockType.AIR
                || baseBlockAt(blockX, top + 2, blockZ) != BlockType.AIR) {
            return null;
        }
        int[][] checks = { { 4, 0 }, { -4, 0 }, { 0, 4 }, { 0, -4 }, { 4, 4 }, { -4, -4 } };
        for (int[] check : checks) {
            int neighborTop = terrainTopY(blockX + check[0], blockZ + check[1]);
            if (Math.abs(neighborTop - top) > 5) {
                return null;
            }
            BlockType neighborSea = baseBlockAt(blockX + check[0], SEA_LEVEL, blockZ + check[1]);
            if (neighborSea.isWater() || neighborSea == BlockType.ICE) {
                return null;
            }
        }
        return new SpawnPoint(blockX, top + 1, blockZ);
    }

    public record SpawnPoint(int x, int y, int z) {
    }

    @Override
    public String getId() {
        return switch (dimension) {
            case OVERWORLD -> RELEASE_ONE;
            case NETHER -> "minecraft_java_1_0_nether";
            case THE_END -> "minecraft_java_1_0_end";
        };
    }

    @Override
    public Dimension getDimension() {
        return dimension;
    }

    @Override
    public BiomeType getBiome(int blockX, int blockZ) {
        if (dimension == Dimension.NETHER) {
            return BiomeType.HELL;
        }
        if (dimension == Dimension.THE_END) {
            return BiomeType.SKY;
        }

        double continental = biomeNoise.octaveNoise2D(blockX * 0.0017, blockZ * 0.0017, 4, 0.5);
        double climate = biomeNoise.octaveNoise2D((blockX + 4000) * 0.0025, (blockZ - 4000) * 0.0025, 3, 0.55);
        double weirdness = biomeNoise.octaveNoise2D((blockX - 7000) * 0.004, (blockZ + 7000) * 0.004, 2, 0.5);

        if (continental < -0.42) {
            return climate < -0.2 ? BiomeType.FROZEN_OCEAN : BiomeType.OCEAN;
        }
        if (Math.abs(weirdness) < 0.035 && continental < 0.22) {
            return climate < -0.18 ? BiomeType.FROZEN_RIVER : BiomeType.RIVER;
        }
        if (continental > 0.62 && climate > 0.45) {
            return continental > 0.72 ? BiomeType.MUSHROOM_ISLAND : BiomeType.MUSHROOM_ISLAND_SHORE;
        }
        if (climate < -0.46) {
            return weirdness > 0.35 ? BiomeType.ICE_MOUNTAINS : BiomeType.ICE_PLAINS;
        }
        if (climate > 0.48) {
            return weirdness > 0.12 ? BiomeType.DESERT_HILLS : BiomeType.DESERT;
        }
        if (continental > 0.42) {
            return weirdness > 0.35 ? BiomeType.EXTREME_HILLS : BiomeType.EXTREME_HILLS_EDGE;
        }
        if (climate < -0.16) {
            return weirdness > 0.28 ? BiomeType.TAIGA_HILLS : BiomeType.TAIGA;
        }
        if (climate > 0.18 && weirdness < -0.25) {
            return BiomeType.SWAMPLAND;
        }
        if (weirdness > 0.18) {
            return BiomeType.FOREST_HILLS;
        }
        return climate > -0.02 ? BiomeType.FOREST : BiomeType.PLAINS;
    }

    @Override
    public void generateChunk(World world, Chunk chunk, int chunkX, int chunkZ) {
        switch (dimension) {
            case NETHER -> generateNether(world, chunk, chunkX, chunkZ);
            case THE_END -> generateEnd(world, chunk, chunkX, chunkZ);
            case OVERWORLD -> generateOverworld(world, chunk, chunkX, chunkZ);
        }
        chunk.clearModified();
    }

    private void generateOverworld(World world, Chunk chunk, int chunkX, int chunkZ) {
        int worldX = chunkX * Chunk.WIDTH;
        int worldZ = chunkZ * Chunk.DEPTH;

        for (int x = 0; x < Chunk.WIDTH; x++) {
            for (int z = 0; z < Chunk.DEPTH; z++) {
                int blockX = worldX + x;
                int blockZ = worldZ + z;
                BiomeType biome = getBiome(blockX, blockZ);
                int terrainTop = terrainTopY(blockX, blockZ, biome);
                int fillerDepth = fillerDepth(blockX, blockZ);
                for (int y = 0; y < Chunk.HEIGHT; y++) {
                    chunk.setBlock(x, y, z, baseBlockAt(blockX, y, blockZ, biome, terrainTop, fillerDepth));
                }
            }
        }

        caveGenerator.generate(chunk, seed);
        ravineGenerator.generate(chunk, seed);
        sealWaterFloors(chunk);
        repairCarvedSurface(chunk, chunkX, chunkZ);
        oreGenerator.generate(chunk, seed);
        stabilizeGeneratedFallingBlocks(chunk);
        decorateOverworld(chunk, chunkX, chunkZ);
        if (world != null) {
            dungeonGenerator.generate(world, chunk, seed, chunkX, chunkZ);
            structureGenerator.generate(world, chunk, seed, chunkX, chunkZ, dimension, this);
        }
    }

    private void decorateOverworld(Chunk chunk, int chunkX, int chunkZ) {
        for (TreeFeature.Candidate tree : featurePlanner.acceptedTreesIntersectingChunk(chunkX, chunkZ)) {
            tree.placeInto(chunk, chunkX, chunkZ);
        }
        for (FeaturePlanner.SmallFeature feature : featurePlanner.smallFeaturesForChunk(chunkX, chunkZ)) {
            feature.placeInto(chunk, chunkX, chunkZ, this::baseBlockAt);
        }
    }

    BlockType baseBlockAt(int blockX, int y, int blockZ) {
        if (y < 0 || y >= Chunk.HEIGHT) {
            return BlockType.AIR;
        }
        BiomeType biome = getBiome(blockX, blockZ);
        return baseBlockAt(blockX, y, blockZ, biome, terrainTopY(blockX, blockZ, biome), fillerDepth(blockX, blockZ));
    }

    private BlockType baseBlockAt(int blockX, int y, int blockZ, BiomeType biome, int terrainTop, int fillerDepth) {
        if (y == 0 || (y <= 4 && isBedrock(blockX, y, blockZ))) {
            return BlockType.BEDROCK;
        }
        if (densityField.isSolid(blockX, y, blockZ, biome)) {
            if (y == terrainTop) {
                return topBlockFor(biome, terrainTop);
            }
            if (y >= terrainTop - fillerDepth) {
                return fillerBlockFor(biome, terrainTop);
            }
            return BlockType.STONE;
        }
        if (y <= SEA_LEVEL && y > terrainTop) {
            return biome.canFreezeWater() && y == SEA_LEVEL ? BlockType.ICE : BlockType.WATER;
        }
        return BlockType.AIR;
    }

    int terrainTopY(int blockX, int blockZ) {
        return terrainTopY(blockX, blockZ, getBiome(blockX, blockZ));
    }

    int terrainTopY(int blockX, int blockZ, BiomeType biome) {
        return densityField.terrainTopY(blockX, blockZ, biome);
    }

    private int fillerDepth(int blockX, int blockZ) {
        Random random = new Random(seed ^ ((long) blockX * 73428767L) ^ ((long) blockZ * 912931L) ^ 0x51F15EEDL);
        return 3 + random.nextInt(3);
    }

    private boolean isBedrock(int blockX, int y, int blockZ) {
        if (y == 0) {
            return true;
        }
        if (y > 4) {
            return false;
        }
        Random random = new Random(seed ^ ((long) blockX * 341873128712L) ^ ((long) blockZ * 132897987541L)
                ^ ((long) y * 42317861L));
        return random.nextInt(5) >= y;
    }

    private BlockType topBlockFor(BiomeType biome, int terrainTop) {
        if (biome == BiomeType.DESERT || biome == BiomeType.DESERT_HILLS || biome == BiomeType.BEACH
                || biome.isOceanic() || biome == BiomeType.RIVER || biome == BiomeType.FROZEN_RIVER
                || terrainTop <= SEA_LEVEL + 1) {
            return BlockType.SAND;
        }
        return biome.getTopBlock();
    }

    private BlockType fillerBlockFor(BiomeType biome, int terrainTop) {
        if (biome == BiomeType.DESERT || biome == BiomeType.DESERT_HILLS || biome == BiomeType.BEACH
                || biome.isOceanic() || biome == BiomeType.RIVER || biome == BiomeType.FROZEN_RIVER
                || terrainTop <= SEA_LEVEL + 1) {
            return BlockType.SAND;
        }
        return biome.getFillerBlock();
    }

    private void repairCarvedSurface(Chunk chunk, int chunkX, int chunkZ) {
        int worldX = chunkX * Chunk.WIDTH;
        int worldZ = chunkZ * Chunk.DEPTH;
        for (int x = 0; x < Chunk.WIDTH; x++) {
            for (int z = 0; z < Chunk.DEPTH; z++) {
                int blockX = worldX + x;
                int blockZ = worldZ + z;
                int y = highestTerrainBlock(chunk, x, z);
                if (y <= 4 || y >= Chunk.HEIGHT - 1) {
                    continue;
                }
                BlockType current = chunk.getBlock(x, y, z);
                if (current == BlockType.STONE || current == BlockType.DIRT || current == BlockType.SAND) {
                    BiomeType biome = getBiome(blockX, blockZ);
                    chunk.setBlock(x, y, z, topBlockFor(biome, y));
                    BlockType filler = fillerBlockFor(biome, y);
                    for (int dy = 1; dy <= 3 && y - dy > 4; dy++) {
                        if (chunk.getBlock(x, y - dy, z) == BlockType.STONE) {
                            chunk.setBlock(x, y - dy, z, filler);
                        }
                    }
                }
            }
        }
    }

    private void sealWaterFloors(Chunk chunk) {
        for (int x = 0; x < Chunk.WIDTH; x++) {
            for (int z = 0; z < Chunk.DEPTH; z++) {
                for (int y = SEA_LEVEL; y > 1; y--) {
                    BlockType block = chunk.getBlock(x, y, z);
                    if (!block.isWater() && block != BlockType.ICE) {
                        continue;
                    }
                    int fillY = y - 1;
                    int filled = 0;
                    while (fillY > 0 && filled < 12) {
                        BlockType below = chunk.getBlock(x, fillY, z);
                        if (below.isSolid() && !below.isWater() && below != BlockType.ICE) {
                            break;
                        }
                        chunk.setBlock(x, fillY, z, fillY >= SEA_LEVEL - 4 ? BlockType.SAND : BlockType.STONE);
                        fillY--;
                        filled++;
                    }
                }
            }
        }
    }

    private static int highestTerrainBlock(Chunk chunk, int x, int z) {
        for (int y = Chunk.HEIGHT - 1; y >= 0; y--) {
            BlockType block = chunk.getBlock(x, y, z);
            if (block.isSolid() && block != BlockType.BEDROCK && block != BlockType.CACTUS) {
                return y;
            }
        }
        return -1;
    }

    private void stabilizeGeneratedFallingBlocks(Chunk chunk) {
        for (int x = 0; x < Chunk.WIDTH; x++) {
            for (int z = 0; z < Chunk.DEPTH; z++) {
                for (int y = 1; y < Chunk.HEIGHT; y++) {
                    BlockType block = chunk.getBlock(x, y, z);
                    if (!block.isFallingBlock() || !BlockShape.canFallThrough(chunk.getBlock(x, y - 1, z))) {
                        continue;
                    }
                    BlockType filler = block == BlockType.SAND ? BlockType.SANDSTONE : BlockType.STONE;
                    int fillY = y - 1;
                    int filled = 0;
                    while (fillY > 0 && BlockShape.canFallThrough(chunk.getBlock(x, fillY, z)) && filled < 8) {
                        chunk.setBlock(x, fillY, z, filler);
                        fillY--;
                        filled++;
                    }
                    if (fillY <= 0 || BlockShape.canFallThrough(chunk.getBlock(x, fillY, z))) {
                        chunk.setBlock(x, y, z, filler);
                    }
                }
            }
        }
    }

    private void generateNether(World world, Chunk chunk, int chunkX, int chunkZ) {
        int worldX = chunkX * Chunk.WIDTH;
        int worldZ = chunkZ * Chunk.DEPTH;
        Random random = new Random(seed ^ 0xBEEFL ^ chunkX * 341873128712L ^ chunkZ * 132897987541L);
        for (int x = 0; x < Chunk.WIDTH; x++) {
            for (int z = 0; z < Chunk.DEPTH; z++) {
                int blockX = worldX + x;
                int blockZ = worldZ + z;
                for (int y = 0; y < Chunk.HEIGHT; y++) {
                    BlockType type;
                    if (y <= 4 || y >= Chunk.HEIGHT - 5) {
                        type = random.nextInt(5) == 0 ? BlockType.NETHERRACK : BlockType.BEDROCK;
                    } else {
                        double density = terrainNoise.octaveNoise3D(blockX * 0.035, y * 0.035, blockZ * 0.035, 4, 0.55)
                                - Math.abs(y - 64) / 88.0;
                        if (density > -0.08) {
                            type = BlockType.NETHERRACK;
                        } else if (y < 32) {
                            type = BlockType.LAVA;
                        } else {
                            type = BlockType.AIR;
                        }
                    }
                    chunk.setBlock(x, y, z, type);
                }
            }
        }
        structureGenerator.generate(world, chunk, seed, chunkX, chunkZ, dimension, this);
    }

    private void generateEnd(World world, Chunk chunk, int chunkX, int chunkZ) {
        int worldX = chunkX * Chunk.WIDTH;
        int worldZ = chunkZ * Chunk.DEPTH;
        for (int x = 0; x < Chunk.WIDTH; x++) {
            for (int z = 0; z < Chunk.DEPTH; z++) {
                int blockX = worldX + x;
                int blockZ = worldZ + z;
                double distance = Math.sqrt(blockX * blockX + blockZ * blockZ);
                double island = 78.0 - distance * 0.045
                        + terrainNoise.octaveNoise2D(blockX * 0.02, blockZ * 0.02, 4, 0.5) * 10.0;
                for (int y = 0; y < Chunk.HEIGHT; y++) {
                    chunk.setBlock(x, y, z, y >= 48 && y <= island ? BlockType.END_STONE : BlockType.AIR);
                }
            }
        }
        generateEndPillars(world, chunk, chunkX, chunkZ);
        if (chunkX == 0 && chunkZ == 0 && !world.hasEntityOfType(EnderDragon.class)) {
            EnderDragon dragon = new EnderDragon();
            dragon.setPosition(0.0f, 84.0f, 0.0f);
            world.stageGeneratedEntity(dragon);
        }
    }

    private void generateEndPillars(World world, Chunk chunk, int chunkX, int chunkZ) {
        boolean crystalsAlreadyRestored = world.hasEntityOfType(EndCrystalEntity.class);
        for (int i = 0; i < 10; i++) {
            double angle = i * Math.PI * 2.0D / 10.0D;
            int px = (int) Math.round(Math.cos(angle) * 42.0D);
            int pz = (int) Math.round(Math.sin(angle) * 42.0D);
            Random random = new Random(seed ^ 0xEEDC0FFEL ^ i * 918273645L);
            int radius = 2 + random.nextInt(3);
            int topY = 74 + random.nextInt(30);
            for (int x = px - radius; x <= px + radius; x++) {
                for (int z = pz - radius; z <= pz + radius; z++) {
                    int dx = x - px;
                    int dz = z - pz;
                    if (dx * dx + dz * dz > radius * radius) {
                        continue;
                    }
                    for (int y = 48; y <= topY; y++) {
                        setIfInChunk(chunk, chunkX, chunkZ, x, y, z, BlockType.OBSIDIAN);
                    }
                }
            }
            setIfInChunk(chunk, chunkX, chunkZ, px, topY + 1, pz, BlockType.BEDROCK);
            if (!crystalsAlreadyRestored && containsBlock(chunkX, chunkZ, px, pz)) {
                world.stageGeneratedEntity(new EndCrystalEntity(px + 0.5f, topY + 2.0f, pz + 0.5f));
            }
        }
    }

    private static boolean containsBlock(int chunkX, int chunkZ, int blockX, int blockZ) {
        int minX = chunkX * Chunk.WIDTH;
        int minZ = chunkZ * Chunk.DEPTH;
        return blockX >= minX && blockX < minX + Chunk.WIDTH
                && blockZ >= minZ && blockZ < minZ + Chunk.DEPTH;
    }

    private static void setIfInChunk(Chunk chunk, int chunkX, int chunkZ, int blockX, int y, int blockZ,
            BlockType type) {
        int localX = blockX - chunkX * Chunk.WIDTH;
        int localZ = blockZ - chunkZ * Chunk.DEPTH;
        if (Chunk.isInBounds(localX, y, localZ)) {
            chunk.setBlock(localX, y, localZ, type);
        }
    }
}
