package com.craftzero.world;

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
        this.densityField = new OverworldDensityField(terrainNoise, detailNoise);
        this.featurePlanner = new FeaturePlanner(seed, this);
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
            case THE_END -> generateEnd(chunk, chunkX, chunkZ);
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
        repairCarvedSurface(chunk, chunkX, chunkZ);
        oreGenerator.generate(chunk, seed);
        decorateOverworld(chunk, chunkX, chunkZ);
        dungeonGenerator.generate(world, chunk, seed, chunkX, chunkZ);
        structureGenerator.generate(world, chunk, seed, chunkX, chunkZ, dimension, this);
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
            return biome.isFrozen() && y == SEA_LEVEL ? BlockType.ICE : BlockType.WATER;
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

    private static int highestTerrainBlock(Chunk chunk, int x, int z) {
        for (int y = Chunk.HEIGHT - 1; y >= 0; y--) {
            BlockType block = chunk.getBlock(x, y, z);
            if (block.isSolid() && block != BlockType.BEDROCK && block != BlockType.CACTUS) {
                return y;
            }
        }
        return -1;
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

    private void generateEnd(Chunk chunk, int chunkX, int chunkZ) {
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
    }
}
