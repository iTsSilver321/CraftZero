package com.craftzero.world;

import com.craftzero.entity.EndCrystalEntity;
import com.craftzero.entity.mob.Mob;
import com.craftzero.entity.mob.MobDefinition;
import com.craftzero.entity.mob.MobFactory;
import com.craftzero.entity.mob.EnderDragon;
import com.craftzero.entity.mob.Sheep;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Release 1.0-style generator scaffold with chunk-safe Overworld population.
 */
public final class ReleaseOneWorldGenerator implements WorldGenerator {
    public static final int SEA_LEVEL = 63;
    static final int LAVA_LEVEL = 11;
    private static final int MAX_BASE_CHUNK_CACHE_ENTRIES = 512;
    private static final int VISIBLE_POPULATION_MIN_CHUNK_OFFSET = -1;
    private static final int VISIBLE_POPULATION_MAX_CHUNK_OFFSET = 0;
    private static final int SCRATCH_POPULATION_MIN_CHUNK_OFFSET = -2;
    private static final int SCRATCH_POPULATION_MAX_CHUNK_OFFSET = 1;
    private static final int SOURCE_POPULATION_LIGHT_MARGIN = 32;
    private static final byte[] SOURCE_BIG_TREE_AXIS_PAIRS = { 2, 0, 0, 1, 2, 1 };
    private static final double SOURCE_BIG_TREE_TWO_PI = 2.0D * 3.14159D;
    private static final int SPAWN_BIOME_SEARCH_RADIUS = 256;
    private static final int SPAWN_JITTER_RANGE = 64;
    private static final int SPAWN_RANDOM_ATTEMPTS = 1000;
    private static final int[] LIGHT_DX = { 1, -1, 0, 0, 0, 0 };
    private static final int[] LIGHT_DY = { 0, 0, 1, -1, 0, 0 };
    private static final int[] LIGHT_DZ = { 0, 0, 0, 0, 1, -1 };
    private static final BiomeType[] SOURCE_SPAWN_BIOMES = {
            BiomeType.FOREST,
            BiomeType.SWAMPLAND,
            BiomeType.TAIGA
    };
    private static final WorldGenSpawnEntry[] NO_WORLD_GEN_CREATURES = {};
    private static final WorldGenSpawnEntry[] STANDARD_WORLD_GEN_CREATURES = {
            new WorldGenSpawnEntry(MobDefinition.SHEEP, 12, 4, 4),
            new WorldGenSpawnEntry(MobDefinition.PIG, 10, 4, 4),
            new WorldGenSpawnEntry(MobDefinition.CHICKEN, 10, 4, 4),
            new WorldGenSpawnEntry(MobDefinition.COW, 8, 4, 4)
    };
    private static final WorldGenSpawnEntry[] FOREST_WORLD_GEN_CREATURES = {
            new WorldGenSpawnEntry(MobDefinition.SHEEP, 12, 4, 4),
            new WorldGenSpawnEntry(MobDefinition.PIG, 10, 4, 4),
            new WorldGenSpawnEntry(MobDefinition.CHICKEN, 10, 4, 4),
            new WorldGenSpawnEntry(MobDefinition.COW, 8, 4, 4),
            new WorldGenSpawnEntry(MobDefinition.WOLF, 5, 4, 4)
    };
    private static final WorldGenSpawnEntry[] TAIGA_WORLD_GEN_CREATURES = {
            new WorldGenSpawnEntry(MobDefinition.SHEEP, 12, 4, 4),
            new WorldGenSpawnEntry(MobDefinition.PIG, 10, 4, 4),
            new WorldGenSpawnEntry(MobDefinition.CHICKEN, 10, 4, 4),
            new WorldGenSpawnEntry(MobDefinition.COW, 8, 4, 4),
            new WorldGenSpawnEntry(MobDefinition.WOLF, 8, 4, 4)
    };
    private static final WorldGenSpawnEntry[] MUSHROOM_WORLD_GEN_CREATURES = {
            new WorldGenSpawnEntry(MobDefinition.MOOSHROOM, 8, 4, 8)
    };

    private final long seed;
    private final Dimension dimension;
    private final ReleaseOneOctaveNoise endNoise1;
    private final ReleaseOneOctaveNoise endNoise2;
    private final ReleaseOneOctaveNoise endNoise3;
    private final ReleaseOneOctaveNoise endNoise4;
    private final ReleaseOneOctaveNoise endNoise5;
    private final ReleaseOneOctaveNoise netherNoise1;
    private final ReleaseOneOctaveNoise netherNoise2;
    private final ReleaseOneOctaveNoise netherNoise3;
    private final ReleaseOneOctaveNoise slowsandGravelNoise;
    private final ReleaseOneOctaveNoise netherrackExclusivityNoise;
    private final ReleaseOneOctaveNoise netherNoise6;
    private final ReleaseOneOctaveNoise netherNoise7;
    private final ReleaseOneBiomeSource biomeSource;
    private final OverworldDensityField densityField;
    private final ConcurrentHashMap<Long, Chunk> overworldBaseChunkCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, Chunk> overworldCarvedChunkCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, Chunk> overworldStructureChunkCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, List<StructureBlockDelta>> overworldStructureDeltaCache =
            new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, Chunk> netherBaseChunkCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, Chunk> netherStructureChunkCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, List<StructureBlockDelta>> netherStructureDeltaCache =
            new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, Chunk> endBaseChunkCache = new ConcurrentHashMap<>();
    private final CaveGenerator caveGenerator = new CaveGenerator();
    private final NetherCaveGenerator netherCaveGenerator = new NetherCaveGenerator();
    private final RavineGenerator ravineGenerator = new RavineGenerator();
    private final OreGenerator oreGenerator = new OreGenerator();
    private final DungeonGenerator dungeonGenerator = new DungeonGenerator();
    private final StructureGenerator structureGenerator = new StructureGenerator();
    private final boolean generateStructures;

    public ReleaseOneWorldGenerator(long seed, Dimension dimension) {
        this(seed, dimension, true);
    }

    public ReleaseOneWorldGenerator(long seed, Dimension dimension, boolean generateStructures) {
        this.seed = seed;
        this.dimension = dimension == null ? Dimension.OVERWORLD : dimension;
        this.generateStructures = generateStructures;
        Random endRandom = new Random(seed);
        this.endNoise1 = new ReleaseOneOctaveNoise(endRandom, 16);
        this.endNoise2 = new ReleaseOneOctaveNoise(endRandom, 16);
        this.endNoise3 = new ReleaseOneOctaveNoise(endRandom, 8);
        this.endNoise4 = new ReleaseOneOctaveNoise(endRandom, 10);
        this.endNoise5 = new ReleaseOneOctaveNoise(endRandom, 16);
        Random netherRandom = new Random(seed);
        this.netherNoise1 = new ReleaseOneOctaveNoise(netherRandom, 16);
        this.netherNoise2 = new ReleaseOneOctaveNoise(netherRandom, 16);
        this.netherNoise3 = new ReleaseOneOctaveNoise(netherRandom, 8);
        this.slowsandGravelNoise = new ReleaseOneOctaveNoise(netherRandom, 4);
        this.netherrackExclusivityNoise = new ReleaseOneOctaveNoise(netherRandom, 4);
        this.netherNoise6 = new ReleaseOneOctaveNoise(netherRandom, 10);
        this.netherNoise7 = new ReleaseOneOctaveNoise(netherRandom, 16);
        this.biomeSource = new ReleaseOneBiomeSource(seed);
        this.densityField = new OverworldDensityField(seed, this::getBiomeForGenerationLayer);
    }

    public SpawnPoint findSafeSpawn() {
        if (dimension != Dimension.OVERWORLD) {
            return new SpawnPoint(0, 80, 0);
        }
        Random random = new Random(seed);
        int spawnX = 0;
        int spawnZ = 0;
        SpawnPoint biomeSpawn = findSpawnBiomePosition(0, 0, SPAWN_BIOME_SEARCH_RADIUS, random);
        if (biomeSpawn != null) {
            spawnX = biomeSpawn.x();
            spawnZ = biomeSpawn.z();
        }

        int attempts = 0;
        while (!canCoordinateBeSpawn(spawnX, spawnZ) && attempts < SPAWN_RANDOM_ATTEMPTS) {
            spawnX += random.nextInt(SPAWN_JITTER_RANGE) - random.nextInt(SPAWN_JITTER_RANGE);
            spawnZ += random.nextInt(SPAWN_JITTER_RANGE) - random.nextInt(SPAWN_JITTER_RANGE);
            attempts++;
        }
        return spawnPointAt(spawnX, spawnZ);
    }

    private SpawnPoint findSpawnBiomePosition(int originX, int originZ, int radius, Random random) {
        int minLayerX = (originX - radius) >> 2;
        int minLayerZ = (originZ - radius) >> 2;
        int maxLayerX = (originX + radius) >> 2;
        int maxLayerZ = (originZ + radius) >> 2;
        int width = maxLayerX - minLayerX + 1;
        int depth = maxLayerZ - minLayerZ + 1;
        int found = 0;
        int spawnX = 0;
        int spawnZ = 0;

        for (int index = 0; index < width * depth; index++) {
            int layerX = minLayerX + index % width;
            int layerZ = minLayerZ + index / width;
            BiomeType biome = getBiomeForGenerationLayer(layerX, layerZ);
            if (isSourceSpawnBiome(biome) && (found == 0 || random.nextInt(found + 1) == 0)) {
                spawnX = layerX << 2;
                spawnZ = layerZ << 2;
            }
            if (isSourceSpawnBiome(biome)) {
                found++;
            }
        }
        return found == 0 ? null : new SpawnPoint(spawnX, 0, spawnZ);
    }

    private static boolean isSourceSpawnBiome(BiomeType biome) {
        for (BiomeType candidate : SOURCE_SPAWN_BIOMES) {
            if (biome == candidate) {
                return true;
            }
        }
        return false;
    }

    private boolean canCoordinateBeSpawn(int blockX, int blockZ) {
        return spawnSurfaceAt(blockX, blockZ) == BlockType.GRASS;
    }

    private SpawnPoint spawnPointAt(int blockX, int blockZ) {
        BiomeType biome = getBiome(blockX, blockZ);
        int top = terrainTopY(blockX, blockZ, biome);
        return new SpawnPoint(blockX, top + 1, blockZ);
    }

    private BlockType spawnSurfaceAt(int blockX, int blockZ) {
        BiomeType biome = getBiome(blockX, blockZ);
        int top = terrainTopY(blockX, blockZ, biome);
        return baseBlockAt(blockX, top, blockZ);
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

    public boolean shouldGenerateStructures() {
        return generateStructures;
    }

    @Override
    public BiomeType getBiome(int blockX, int blockZ) {
        if (dimension == Dimension.NETHER) {
            return BiomeType.HELL;
        }
        if (dimension == Dimension.THE_END) {
            return BiomeType.SKY;
        }
        return biomeSource.getBiome(blockX, blockZ);
    }

    BiomeType getBiomeForGenerationLayer(int layerX, int layerZ) {
        if (dimension == Dimension.NETHER) {
            return BiomeType.HELL;
        }
        if (dimension == Dimension.THE_END) {
            return BiomeType.SKY;
        }
        return biomeSource.getBiomeForGenerationLayer(layerX, layerZ);
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
        generateOverworldCarvedChunk(chunk, chunkX, chunkZ);
        Random populationRandom = populationRandom(chunkX, chunkZ);
        if (generateStructures) {
            structureGenerator.generate(world, chunk, seed, chunkX, chunkZ, dimension, this, populationRandom);
        }
        SourceTreeScratch populationScratch = sourceStructureSideEffectScratch(chunk, chunkX, chunkZ);
        placeOverworldLakes(world, chunk, chunkX, chunkZ, populationRandom, populationScratch);
        placeOverworldDungeons(world, chunk, chunkX, chunkZ, populationScratch);
        placeOverworldOres(world, chunk, chunkX, chunkZ, populationScratch);
        stabilizeGeneratedFallingBlocks(chunk);
        populationScratch.clearTargetChunkOverlay();
        OverworldDecorationResult decoration = decorateOverworld(world, chunk, chunkX, chunkZ, populationScratch);
        spawnWorldGenCreatures(world, chunk, chunkX, chunkZ, decoration.random(), decoration.scratch());
        placeSnowLayers(chunk, chunkX, chunkZ, decoration.scratch());
    }

    private void generateOverworldCarvedChunk(Chunk chunk, int chunkX, int chunkZ) {
        generateOverworldBaseChunk(chunk, chunkX, chunkZ);
        caveGenerator.generate(chunk, seed);
        ravineGenerator.generate(chunk, seed);
        sealWaterFloors(chunk);
        repairCarvedSurface(chunk, chunkX, chunkZ);
    }

    private void generateOverworldBaseChunk(Chunk chunk, int chunkX, int chunkZ) {
        generateOverworldTerrain(chunk, chunkX, chunkZ);
        replaceOverworldSurface(chunk, chunkX, chunkZ);
        freezeOverworldWaterSurface(chunk, chunkX, chunkZ);
    }

    private void generateOverworldTerrain(Chunk chunk, int chunkX, int chunkZ) {
        int worldX = chunkX * Chunk.WIDTH;
        int worldZ = chunkZ * Chunk.DEPTH;

        for (int x = 0; x < Chunk.WIDTH; x++) {
            for (int z = 0; z < Chunk.DEPTH; z++) {
                int blockX = worldX + x;
                int blockZ = worldZ + z;
                BiomeType biome = getBiome(blockX, blockZ);
                for (int y = 0; y < Chunk.HEIGHT; y++) {
                    BlockType type = BlockType.AIR;
                    if (densityField.isSolid(blockX, y, blockZ, biome)) {
                        type = BlockType.STONE;
                    } else if (y <= SEA_LEVEL) {
                        type = BlockType.WATER;
                    }
                    chunk.setBlock(x, y, z, type);
                }
            }
        }
    }

    private void replaceOverworldSurface(Chunk chunk, int chunkX, int chunkZ) {
        Random random = new Random((long) chunkX * 0x4f9939f508L + (long) chunkZ * 0x1ef1565bd5L);
        double[] stoneNoise = densityField.stoneNoiseForChunk(chunkX, chunkZ);
        int worldX = chunkX * Chunk.WIDTH;
        int worldZ = chunkZ * Chunk.DEPTH;

        for (int x = 0; x < Chunk.WIDTH; x++) {
            for (int z = 0; z < Chunk.DEPTH; z++) {
                int blockX = worldX + x;
                int blockZ = worldZ + z;
                BiomeType biome = getBiome(blockX, blockZ);
                int thickness = (int) (stoneNoise[x + z * 16] / 3.0 + 3.0 + random.nextDouble() * 0.25);
                int run = -1;
                BlockType top = biome.getTopBlock();
                BlockType filler = biome.getFillerBlock();

                for (int y = Chunk.HEIGHT - 1; y >= 0; y--) {
                    if (y <= random.nextInt(5)) {
                        chunk.setBlock(x, y, z, BlockType.BEDROCK);
                        continue;
                    }

                    BlockType current = chunk.getBlock(x, y, z);
                    if (current == BlockType.AIR) {
                        run = -1;
                        continue;
                    }
                    if (current != BlockType.STONE) {
                        continue;
                    }

                    if (run == -1) {
                        if (thickness <= 0) {
                            top = BlockType.AIR;
                            filler = BlockType.STONE;
                        } else if (y >= SEA_LEVEL - 4 && y <= SEA_LEVEL + 1) {
                            top = biome.getTopBlock();
                            filler = biome.getFillerBlock();
                        }

                        if (y < SEA_LEVEL && top == BlockType.AIR) {
                            top = isSourceFreezingTemperature(blockX, blockZ) ? BlockType.ICE : BlockType.WATER;
                        }

                        run = thickness;
                        chunk.setBlock(x, y, z, y >= SEA_LEVEL - 1 ? top : filler);
                        continue;
                    }

                    if (run > 0) {
                        run--;
                        chunk.setBlock(x, y, z, filler);
                        if (run == 0 && filler == BlockType.SAND) {
                            run = random.nextInt(4);
                            filler = BlockType.SANDSTONE;
                        }
                    }
                }
            }
        }
    }

    private void freezeOverworldWaterSurface(Chunk chunk, int chunkX, int chunkZ) {
        int worldX = chunkX * Chunk.WIDTH;
        int worldZ = chunkZ * Chunk.DEPTH;
        for (int x = 0; x < Chunk.WIDTH; x++) {
            for (int z = 0; z < Chunk.DEPTH; z++) {
                int blockX = worldX + x;
                int blockZ = worldZ + z;
                if (!isSourceFreezingTemperature(blockX, blockZ)) {
                    continue;
                }
                for (int y = SEA_LEVEL; y >= 1; y--) {
                    if (chunk.getBlock(x, y, z).isWater() && chunk.getBlock(x, y + 1, z) == BlockType.AIR) {
                        chunk.setBlock(x, y, z, BlockType.ICE);
                        break;
                    }
                    if (chunk.getBlock(x, y, z).isSolid()) {
                        break;
                    }
                }
            }
        }
    }

    private void placeOverworldLakes(World world, Chunk chunk, int chunkX, int chunkZ) {
        placeOverworldLakes(world, chunk, chunkX, chunkZ, null);
    }

    private void placeOverworldLakes(World world, Chunk chunk, int chunkX, int chunkZ, Random currentOriginRandom) {
        SourceTreeScratch scratch = sourceStructureSideEffectScratch(chunk, chunkX, chunkZ);
        placeOverworldLakes(world, chunk, chunkX, chunkZ, currentOriginRandom, scratch);
    }

    private void placeOverworldLakes(World world, Chunk chunk, int chunkX, int chunkZ,
            Random currentOriginRandom, SourceTreeScratch scratch) {
        if (scratch == null) {
            scratch = sourceStructureSideEffectScratch(chunk, chunkX, chunkZ);
        }
        for (int originChunkX = chunkX + VISIBLE_POPULATION_MIN_CHUNK_OFFSET;
                originChunkX <= chunkX + VISIBLE_POPULATION_MAX_CHUNK_OFFSET; originChunkX++) {
            for (int originChunkZ = chunkZ + VISIBLE_POPULATION_MIN_CHUNK_OFFSET;
                    originChunkZ <= chunkZ + VISIBLE_POPULATION_MAX_CHUNK_OFFSET; originChunkZ++) {
                Random random = currentOriginRandom != null && originChunkX == chunkX && originChunkZ == chunkZ
                        ? currentOriginRandom
                        : overworldPopulationRandomAfterStructures(originChunkX, originChunkZ);
                if (structureGenerator.suppressesOverworldLakes(seed, originChunkX, originChunkZ, this)) {
                    continue;
                }
                int originX = originChunkX * Chunk.WIDTH;
                int originZ = originChunkZ * Chunk.DEPTH;

                if (random.nextInt(4) == 0) {
                    int x = originX + random.nextInt(16) + 8;
                    int y = random.nextInt(128);
                    int z = originZ + random.nextInt(16) + 8;
                    LakeCandidate lake = buildLakeCandidate(scratch, chunkX, chunkZ, false,
                            random, x, y, z, BlockType.WATER);
                    applyLakeCandidateToScratch(scratch, lake, true);
                }

                if (random.nextInt(8) == 0) {
                    int x = originX + random.nextInt(16) + 8;
                    int y = random.nextInt(random.nextInt(120) + 8);
                    int z = originZ + random.nextInt(16) + 8;
                    if (y < SEA_LEVEL || random.nextInt(10) == 0) {
                        LakeCandidate lake = buildLakeCandidate(scratch, chunkX, chunkZ, false,
                                random, x, y, z, BlockType.LAVA);
                        applyLakeCandidateToScratch(scratch, lake, true);
                    }
                }
            }
        }
        scratch.applyToChunk();
    }

    private void placeOverworldDungeons(World world, Chunk chunk, int chunkX, int chunkZ) {
        placeOverworldDungeons(world, chunk, chunkX, chunkZ, null);
    }

    private void placeOverworldDungeons(World world, Chunk chunk, int chunkX, int chunkZ,
            SourceTreeScratch scratch) {
        boolean carryingPopulationScratch = scratch != null;
        if (scratch == null) {
            scratch = sourceLakeSideEffectScratch(world, chunk, chunkX, chunkZ);
        } else {
            overlayMissingOverworldLakeSideEffects(world, scratch, chunkX, chunkZ);
        }
        DungeonGenerator.BlockReader blocks = overworldDungeonBlockReader(chunk, chunkX, chunkZ, scratch);
        DungeonGenerator.BlockWriter writer = overworldDungeonBlockWriter(chunk, chunkX, chunkZ, scratch);
        for (int originChunkX = chunkX + VISIBLE_POPULATION_MIN_CHUNK_OFFSET;
                originChunkX <= chunkX + VISIBLE_POPULATION_MAX_CHUNK_OFFSET; originChunkX++) {
            for (int originChunkZ = chunkZ + VISIBLE_POPULATION_MIN_CHUNK_OFFSET;
                    originChunkZ <= chunkZ + VISIBLE_POPULATION_MAX_CHUNK_OFFSET; originChunkZ++) {
                Random random = overworldDungeonRandom(world, originChunkX, originChunkZ);
                dungeonGenerator.generateFromOrigin(world, chunk, random, chunkX, chunkZ, originChunkX, originChunkZ,
                        blocks, writer);
            }
        }
        if (carryingPopulationScratch) {
            overlayMissingOverworldDungeonSideEffects(world, scratch, chunkX, chunkZ);
        }
    }

    private Random overworldDungeonRandom(World world, int originChunkX, int originChunkZ) {
        Random random = overworldPopulationRandomAfterStructures(originChunkX, originChunkZ);
        advanceOverworldLakeRandom(world, random, originChunkX, originChunkZ);
        return random;
    }

    private Random overworldPopulationRandomAfterStructures(int chunkX, int chunkZ) {
        Random random = populationRandom(chunkX, chunkZ);
        if (!generateStructures) {
            return random;
        }
        Chunk scratch = copyChunk(cachedOverworldCarvedChunk(chunkX, chunkZ));
        structureGenerator.generate(null, scratch, seed, chunkX, chunkZ, Dimension.OVERWORLD, this, random);
        return random;
    }

    private Random overworldOreRandom(World world, int originChunkX, int originChunkZ) {
        Random random = overworldDungeonRandom(world, originChunkX, originChunkZ);
        advanceOverworldDungeonRandom(world, random, originChunkX, originChunkZ);
        return random;
    }

    private void advanceOverworldDungeonRandom(World world, Random random, int originChunkX, int originChunkZ) {
        Chunk scratch = overworldChunkAfterStructures(originChunkX, originChunkZ);
        placeOverworldLakes(world, scratch, originChunkX, originChunkZ);
        SourceTreeScratch lakeScratch = sourceLakeSideEffectScratch(world, scratch, originChunkX, originChunkZ);
        dungeonGenerator.generateFromOrigin(null, scratch, random,
                originChunkX, originChunkZ, originChunkX, originChunkZ,
                overworldDungeonBlockReader(scratch, originChunkX, originChunkZ, lakeScratch),
                overworldDungeonBlockWriter(scratch, originChunkX, originChunkZ, lakeScratch));
    }

    private DungeonGenerator.BlockReader overworldDungeonBlockReader(Chunk chunk, int chunkX, int chunkZ) {
        return overworldDungeonBlockReader(chunk, chunkX, chunkZ, null);
    }

    private DungeonGenerator.BlockReader overworldDungeonBlockReader(Chunk chunk, int chunkX, int chunkZ,
            SourceTreeScratch scratch) {
        return (blockX, y, blockZ) -> {
            if (y < 0 || y >= Chunk.HEIGHT) {
                return BlockType.BEDROCK;
            }
            if (scratch != null) {
                return scratch.getBlock(blockX, y, blockZ);
            }
            if (containsBlock(chunkX, chunkZ, blockX, blockZ)) {
                return chunk.getBlock(blockX - chunkX * Chunk.WIDTH, y, blockZ - chunkZ * Chunk.DEPTH);
            }
            return carvedBlockAt(blockX, y, blockZ);
        };
    }

    private DungeonGenerator.BlockWriter overworldDungeonBlockWriter(Chunk chunk, int chunkX, int chunkZ,
            SourceTreeScratch scratch) {
        return (blockX, y, blockZ, block) -> {
            if (y < 0 || y >= Chunk.HEIGHT) {
                return false;
            }
            if (scratch != null) {
                scratch.setBlock(blockX, y, blockZ, block);
            }
            if (containsBlock(chunkX, chunkZ, blockX, blockZ)) {
                chunk.setBlock(blockX - chunkX * Chunk.WIDTH, y, blockZ - chunkZ * Chunk.DEPTH, block);
                return true;
            }
            return false;
        };
    }

    private void advanceOverworldLakeRandom(World world, Random random, int originChunkX, int originChunkZ) {
        if (structureGenerator.suppressesOverworldLakes(seed, originChunkX, originChunkZ, this)) {
            return;
        }

        int originX = originChunkX * Chunk.WIDTH;
        int originZ = originChunkZ * Chunk.DEPTH;
        Chunk originCarved = overworldChunkAfterStructures(originChunkX, originChunkZ);
        SourceTreeScratch scratch = new SourceTreeScratch(originCarved, originChunkX, originChunkZ);
        overlayOverworldStructureSideEffects(scratch, originChunkX, originChunkZ);

        if (random.nextInt(4) == 0) {
            int x = originX + random.nextInt(16) + 8;
            int y = random.nextInt(128);
            int z = originZ + random.nextInt(16) + 8;
            LakeCandidate lake = buildLakeCandidate(scratch, originChunkX, originChunkZ, false,
                    random, x, y, z, BlockType.WATER);
            applyLakeCandidateToScratch(scratch, lake, true);
        }

        if (random.nextInt(8) == 0) {
            int x = originX + random.nextInt(16) + 8;
            int y = random.nextInt(random.nextInt(120) + 8);
            int z = originZ + random.nextInt(16) + 8;
            if (y < SEA_LEVEL || random.nextInt(10) == 0) {
                buildLakeCandidate(scratch, originChunkX, originChunkZ, false,
                        random, x, y, z, BlockType.LAVA);
            }
        }
    }

    private void placeOverworldOres(World world, Chunk chunk, int chunkX, int chunkZ) {
        placeOverworldOres(world, chunk, chunkX, chunkZ, null);
    }

    private void placeOverworldOres(World world, Chunk chunk, int chunkX, int chunkZ,
            SourceTreeScratch scratch) {
        boolean carryingPopulationScratch = scratch != null;
        OreGenerator.BlockReader blocks = scratch == null ? null : scratch::getBlock;
        OreGenerator.BlockWriter writer = scratch == null ? null
                : (blockX, y, blockZ, block) -> setDecoratorBlock(chunk, chunkX, chunkZ,
                        scratch, blockX, y, blockZ, block);
        for (int originChunkX = chunkX + VISIBLE_POPULATION_MIN_CHUNK_OFFSET;
                originChunkX <= chunkX + VISIBLE_POPULATION_MAX_CHUNK_OFFSET; originChunkX++) {
            for (int originChunkZ = chunkZ + VISIBLE_POPULATION_MIN_CHUNK_OFFSET;
                    originChunkZ <= chunkZ + VISIBLE_POPULATION_MAX_CHUNK_OFFSET; originChunkZ++) {
                Random random = overworldOreRandom(world, originChunkX, originChunkZ);
                if (scratch == null) {
                    oreGenerator.generateFromOrigin(chunk, random, originChunkX, originChunkZ);
                } else {
                    oreGenerator.generateFromOrigin(random, originChunkX, originChunkZ, blocks, writer);
                }
            }
        }
        if (carryingPopulationScratch) {
            overlayMissingOverworldOreSideEffects(world, scratch, chunkX, chunkZ);
        }
    }

    private LakeCandidate buildLakeCandidate(Chunk chunk, int chunkX, int chunkZ, Random random,
            int centerX, int centerY, int centerZ, BlockType lakeBlock) {
        return buildLakeCandidate(chunk, chunkX, chunkZ, true, random, centerX, centerY, centerZ, lakeBlock);
    }

    private LakeCandidate buildLakeCandidate(Chunk chunk, int chunkX, int chunkZ, boolean requireChunkIntersection,
            Random random, int centerX, int centerY, int centerZ, BlockType lakeBlock) {
        int originX = centerX - 8;
        int originZ = centerZ - 8;
        int y = centerY;
        while (y > 0 && blockAtLakeCandidate(chunk, chunkX, chunkZ, originX, y, originZ) == BlockType.AIR) {
            y--;
        }
        int originY = y - 4;
        boolean[] mask = new boolean[16 * 16 * 8];
        int ellipsoids = random.nextInt(4) + 4;
        for (int i = 0; i < ellipsoids; i++) {
            double sizeX = random.nextDouble() * 6.0 + 3.0;
            double sizeY = random.nextDouble() * 4.0 + 2.0;
            double sizeZ = random.nextDouble() * 6.0 + 3.0;
            double centerLocalX = random.nextDouble() * (16.0 - sizeX - 2.0) + 1.0 + sizeX / 2.0;
            double centerLocalY = random.nextDouble() * (8.0 - sizeY - 4.0) + 2.0 + sizeY / 2.0;
            double centerLocalZ = random.nextDouble() * (16.0 - sizeZ - 2.0) + 1.0 + sizeZ / 2.0;
            for (int x = 1; x < 15; x++) {
                for (int z = 1; z < 15; z++) {
                    for (int localY = 1; localY < 7; localY++) {
                        double dx = (x - centerLocalX) / (sizeX / 2.0);
                        double dy = (localY - centerLocalY) / (sizeY / 2.0);
                        double dz = (z - centerLocalZ) / (sizeZ / 2.0);
                        if (dx * dx + dy * dy + dz * dz < 1.0) {
                            mask[lakeIndex(x, z, localY)] = true;
                        }
                    }
                }
            }
        }
        LakeCandidate candidate = new LakeCandidate(originX, originY, originZ, lakeBlock, mask, null);
        if (requireChunkIntersection && !lakeIntersectsChunk(candidate, chunkX, chunkZ)
                || !validateLakeCandidate(chunk, chunkX, chunkZ, candidate)) {
            return null;
        }
        if (lakeBlock.isLava()) {
            candidate = new LakeCandidate(originX, originY, originZ, lakeBlock, mask, lavaLakeShell(mask, random));
        }
        return candidate;
    }

    private LakeCandidate buildLakeCandidate(SourceTreeScratch scratch, int chunkX, int chunkZ,
            boolean requireChunkIntersection, Random random, int centerX, int centerY, int centerZ,
            BlockType lakeBlock) {
        int originX = centerX - 8;
        int originZ = centerZ - 8;
        int y = centerY;
        while (y > 0 && blockAtLakeCandidate(scratch, originX, y, originZ) == BlockType.AIR) {
            y--;
        }
        int originY = y - 4;
        boolean[] mask = new boolean[16 * 16 * 8];
        int ellipsoids = random.nextInt(4) + 4;
        for (int i = 0; i < ellipsoids; i++) {
            double sizeX = random.nextDouble() * 6.0 + 3.0;
            double sizeY = random.nextDouble() * 4.0 + 2.0;
            double sizeZ = random.nextDouble() * 6.0 + 3.0;
            double centerLocalX = random.nextDouble() * (16.0 - sizeX - 2.0) + 1.0 + sizeX / 2.0;
            double centerLocalY = random.nextDouble() * (8.0 - sizeY - 4.0) + 2.0 + sizeY / 2.0;
            double centerLocalZ = random.nextDouble() * (16.0 - sizeZ - 2.0) + 1.0 + sizeZ / 2.0;
            for (int x = 1; x < 15; x++) {
                for (int z = 1; z < 15; z++) {
                    for (int localY = 1; localY < 7; localY++) {
                        double dx = (x - centerLocalX) / (sizeX / 2.0);
                        double dy = (localY - centerLocalY) / (sizeY / 2.0);
                        double dz = (z - centerLocalZ) / (sizeZ / 2.0);
                        if (dx * dx + dy * dy + dz * dz < 1.0) {
                            mask[lakeIndex(x, z, localY)] = true;
                        }
                    }
                }
            }
        }
        LakeCandidate candidate = new LakeCandidate(originX, originY, originZ, lakeBlock, mask, null);
        if (requireChunkIntersection && !lakeIntersectsChunk(candidate, chunkX, chunkZ)
                || !validateLakeCandidate(scratch, candidate)) {
            return null;
        }
        if (lakeBlock.isLava()) {
            candidate = new LakeCandidate(originX, originY, originZ, lakeBlock, mask, lavaLakeShell(mask, random));
        }
        return candidate;
    }

    private boolean validateLakeCandidate(Chunk chunk, int chunkX, int chunkZ, LakeCandidate lake) {
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = 0; y < 8; y++) {
                    if (!isLakeBoundary(lake.mask(), x, z, y)) {
                        continue;
                    }
                    BlockType block = blockAtLakeCandidate(chunk, chunkX, chunkZ,
                            lake.originX() + x, lake.originY() + y, lake.originZ() + z);
                    if (y >= 4 && block.isFluid()) {
                        return false;
                    }
                    if (y < 4 && !block.isSolid() && block != lake.block()) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    private boolean validateLakeCandidate(SourceTreeScratch scratch, LakeCandidate lake) {
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = 0; y < 8; y++) {
                    if (!isLakeBoundary(lake.mask(), x, z, y)) {
                        continue;
                    }
                    BlockType block = blockAtLakeCandidate(scratch,
                            lake.originX() + x, lake.originY() + y, lake.originZ() + z);
                    if (y >= 4 && block.isFluid()) {
                        return false;
                    }
                    if (y < 4 && !block.isSolid() && block != lake.block()) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    private void applyLakeCandidate(Chunk chunk, int chunkX, int chunkZ, LakeCandidate lake) {
        if (lake == null) {
            return;
        }
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = 0; y < 8; y++) {
                    if (!lake.mask()[lakeIndex(x, z, y)]) {
                        continue;
                    }
                    setLakeBlockIfInChunk(chunk, chunkX, chunkZ,
                            lake.originX() + x, lake.originY() + y, lake.originZ() + z,
                            y < 4 ? lake.block() : BlockType.AIR);
                }
            }
        }

        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = 4; y < 8; y++) {
                    if (!lake.mask()[lakeIndex(x, z, y)]) {
                        continue;
                    }
                    restoreLakeSurfaceIfInChunk(chunk, chunkX, chunkZ,
                            lake.originX() + x, lake.originY() + y - 1, lake.originZ() + z);
                }
            }
        }

        if (lake.block().isLava()) {
            addLavaLakeStoneShell(chunk, chunkX, chunkZ, lake);
        } else {
            freezeLakeSurface(chunk, chunkX, chunkZ, lake);
        }
    }

    private void addLavaLakeStoneShell(Chunk chunk, int chunkX, int chunkZ, LakeCandidate lake) {
        boolean[] shell = lake.shell();
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = 0; y < 8; y++) {
                    if (shell == null || !shell[lakeIndex(x, z, y)]) {
                        continue;
                    }
                    int worldX = lake.originX() + x;
                    int worldY = lake.originY() + y;
                    int worldZ = lake.originZ() + z;
                    if (containsBlock(chunkX, chunkZ, worldX, worldZ)
                            && worldY >= 0 && worldY < Chunk.HEIGHT
                            && chunk.getBlock(worldX - chunkX * Chunk.WIDTH, worldY,
                                    worldZ - chunkZ * Chunk.DEPTH).isSolid()) {
                        chunk.setBlock(worldX - chunkX * Chunk.WIDTH, worldY,
                                worldZ - chunkZ * Chunk.DEPTH, BlockType.STONE);
                    }
                }
            }
        }
    }

    private void freezeLakeSurface(Chunk chunk, int chunkX, int chunkZ, LakeCandidate lake) {
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                int worldX = lake.originX() + x;
                int worldZ = lake.originZ() + z;
                if (!isSourceFreezingTemperature(worldX, worldZ)) {
                    continue;
                }
                for (int y = 3; y >= 0; y--) {
                    if (!lake.mask()[lakeIndex(x, z, y)]) {
                        continue;
                    }
                    int worldY = lake.originY() + y;
                    if (blockAtLakeCandidate(chunk, chunkX, chunkZ, worldX, worldY + 1, worldZ) == BlockType.AIR
                            && setLakeBlockIfInChunk(chunk, chunkX, chunkZ, worldX, worldY, worldZ, BlockType.ICE)) {
                        break;
                    }
                }
            }
        }
    }

    private void restoreLakeSurfaceIfInChunk(Chunk chunk, int chunkX, int chunkZ, int worldX, int worldY, int worldZ) {
        if (!containsBlock(chunkX, chunkZ, worldX, worldZ) || worldY < 0 || worldY >= Chunk.HEIGHT) {
            return;
        }
        int localX = worldX - chunkX * Chunk.WIDTH;
        int localZ = worldZ - chunkZ * Chunk.DEPTH;
        if (chunk.getBlock(localX, worldY, localZ) != BlockType.DIRT) {
            return;
        }
        BiomeType biome = getBiome(worldX, worldZ);
        if (biome.getTopBlock() == BlockType.MYCELIUM) {
            chunk.setBlock(localX, worldY, localZ, BlockType.MYCELIUM);
        } else {
            chunk.setBlock(localX, worldY, localZ, BlockType.GRASS);
        }
    }

    private boolean setLakeBlockIfInChunk(Chunk chunk, int chunkX, int chunkZ,
            int worldX, int worldY, int worldZ, BlockType block) {
        if (!containsBlock(chunkX, chunkZ, worldX, worldZ) || worldY < 0 || worldY >= Chunk.HEIGHT) {
            return false;
        }
        chunk.setBlock(worldX - chunkX * Chunk.WIDTH, worldY, worldZ - chunkZ * Chunk.DEPTH, block);
        return true;
    }

    private BlockType blockAtLakeCandidate(Chunk chunk, int chunkX, int chunkZ, int worldX, int worldY, int worldZ) {
        if (worldY < 0 || worldY >= Chunk.HEIGHT) {
            return BlockType.AIR;
        }
        if (containsBlock(chunkX, chunkZ, worldX, worldZ)) {
            return chunk.getBlock(worldX - chunkX * Chunk.WIDTH, worldY, worldZ - chunkZ * Chunk.DEPTH);
        }
        return carvedBlockAt(worldX, worldY, worldZ);
    }

    private BlockType blockAtLakeCandidate(SourceTreeScratch scratch, int worldX, int worldY, int worldZ) {
        if (worldY < 0 || worldY >= Chunk.HEIGHT) {
            return BlockType.AIR;
        }
        return scratch.getBlock(worldX, worldY, worldZ);
    }

    private static boolean lakeIntersectsChunk(LakeCandidate lake, int chunkX, int chunkZ) {
        int minX = chunkX * Chunk.WIDTH;
        int minZ = chunkZ * Chunk.DEPTH;
        return lake.originX() <= minX + Chunk.WIDTH - 1
                && lake.originX() + 15 >= minX
                && lake.originZ() <= minZ + Chunk.DEPTH - 1
                && lake.originZ() + 15 >= minZ;
    }

    private static boolean isLakeBoundary(boolean[] mask, int x, int z, int y) {
        if (mask[lakeIndex(x, z, y)]) {
            return false;
        }
        return x < 15 && mask[lakeIndex(x + 1, z, y)]
                || x > 0 && mask[lakeIndex(x - 1, z, y)]
                || z < 15 && mask[lakeIndex(x, z + 1, y)]
                || z > 0 && mask[lakeIndex(x, z - 1, y)]
                || y < 7 && mask[lakeIndex(x, z, y + 1)]
                || y > 0 && mask[lakeIndex(x, z, y - 1)];
    }

    private static int lakeIndex(int x, int z, int y) {
        return (x * 16 + z) * 8 + y;
    }

    private static boolean[] lavaLakeShell(boolean[] mask, Random random) {
        boolean[] shell = new boolean[mask.length];
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = 0; y < 8; y++) {
                    if (isLakeBoundary(mask, x, z, y) && (y < 4 || random.nextInt(2) != 0)) {
                        shell[lakeIndex(x, z, y)] = true;
                    }
                }
            }
        }
        return shell;
    }

    private Random populationRandom(int chunkX, int chunkZ) {
        Random random = new Random(seed);
        long xSeed = (random.nextLong() / 2L) * 2L + 1L;
        long zSeed = (random.nextLong() / 2L) * 2L + 1L;
        random.setSeed((long) chunkX * xSeed + (long) chunkZ * zSeed ^ seed);
        return random;
    }

    private record LakeCandidate(int originX, int originY, int originZ, BlockType block, boolean[] mask,
            boolean[] shell) {
    }

    private record OverworldDecorationResult(Random random, SourceTreeScratch scratch) {
    }

    private record DecoratorOrigin(int chunkX, int chunkZ, Random random) {
    }

    private record WorldGenSpawnEntry(MobDefinition mob, int weight, int minGroup, int maxGroup) {
    }

    private record SourceBlockPos(int x, int y, int z) {
    }

    private record SourceBlock(BlockType type, int metadata) {
    }

    private record SourceBigLeafNode(int x, int y, int z, int branchBaseY) {
    }

    private record SourceLightNode(int x, int y, int z, int light) {
    }

    private record GeneratedBlockLight(byte[] target, HashMap<SourceBlockPos, Integer> world,
            int baseX, int baseZ) {
    }

    private final class SourceTreeScratch {
        private final Chunk chunk;
        private final int chunkX;
        private final int chunkZ;
        private final HashMap<SourceBlockPos, SourceBlock> blocks = new HashMap<>();

        private SourceTreeScratch(Chunk chunk, int chunkX, int chunkZ) {
            this.chunk = chunk;
            this.chunkX = chunkX;
            this.chunkZ = chunkZ;
        }

        private BlockType getBlock(int blockX, int y, int blockZ) {
            if (y < 0 || y >= Chunk.HEIGHT) {
                return BlockType.AIR;
            }
            SourceBlockPos pos = new SourceBlockPos(blockX, y, blockZ);
            SourceBlock block = blocks.get(pos);
            if (block != null) {
                return block.type();
            }
            if (containsBlock(chunkX, chunkZ, blockX, blockZ)) {
                return chunk.getBlock(blockX - chunkX * Chunk.WIDTH, y, blockZ - chunkZ * Chunk.DEPTH);
            }
            return carvedBlockAt(blockX, y, blockZ);
        }

        private int getMetadata(int blockX, int y, int blockZ) {
            if (y < 0 || y >= Chunk.HEIGHT) {
                return 0;
            }
            SourceBlockPos pos = new SourceBlockPos(blockX, y, blockZ);
            SourceBlock block = blocks.get(pos);
            if (block != null) {
                return block.metadata();
            }
            if (containsBlock(chunkX, chunkZ, blockX, blockZ)) {
                return chunk.getBlockMetadata(blockX - chunkX * Chunk.WIDTH, y, blockZ - chunkZ * Chunk.DEPTH);
            }
            return 0;
        }

        private void setBlock(int blockX, int y, int blockZ, BlockType block) {
            setBlock(blockX, y, blockZ, block, 0);
        }

        private void setBlock(int blockX, int y, int blockZ, BlockType block, int metadata) {
            if (y >= 0 && y < Chunk.HEIGHT) {
                blocks.put(new SourceBlockPos(blockX, y, blockZ), new SourceBlock(block, metadata));
            }
        }

        private int topHeightMapBlockY(int blockX, int blockZ) {
            for (int y = Chunk.HEIGHT - 1; y >= 0; y--) {
                if (isSourceHeightMapBlock(getBlock(blockX, y, blockZ))) {
                    return y;
                }
            }
            return -1;
        }

        private int topSolidOrLiquidBlockY(int blockX, int blockZ) {
            for (int y = Chunk.HEIGHT - 1; y >= 0; y--) {
                if (isSourcePrecipitationHeightBlock(getBlock(blockX, y, blockZ))) {
                    return y;
                }
            }
            return -1;
        }

        private void applyToChunk() {
            int baseX = chunkX * Chunk.WIDTH;
            int baseZ = chunkZ * Chunk.DEPTH;
            for (var entry : blocks.entrySet()) {
                SourceBlockPos pos = entry.getKey();
                if (!containsBlock(chunkX, chunkZ, pos.x(), pos.z())) {
                    continue;
                }
                SourceBlock block = entry.getValue();
                chunk.setBlock(pos.x() - baseX, pos.y(), pos.z() - baseZ, block.type(), block.metadata());
            }
        }

        private void clearTargetChunkOverlay() {
            blocks.keySet().removeIf(pos -> containsBlock(chunkX, chunkZ, pos.x(), pos.z()));
        }
    }

    private final class NetherDecoratorScratch {
        private final Chunk chunk;
        private final int chunkX;
        private final int chunkZ;
        private final HashMap<SourceBlockPos, SourceBlock> blocks = new HashMap<>();

        private NetherDecoratorScratch(Chunk chunk, int chunkX, int chunkZ) {
            this.chunk = chunk;
            this.chunkX = chunkX;
            this.chunkZ = chunkZ;
        }

        private BlockType getBlock(int blockX, int y, int blockZ) {
            if (y < 0 || y >= Chunk.HEIGHT) {
                return BlockType.AIR;
            }
            SourceBlock block = blocks.get(new SourceBlockPos(blockX, y, blockZ));
            if (block != null) {
                return block.type();
            }
            if (containsBlock(chunkX, chunkZ, blockX, blockZ)) {
                return chunk.getBlock(blockX - chunkX * Chunk.WIDTH, y, blockZ - chunkZ * Chunk.DEPTH);
            }
            return netherBaseBlockAt(blockX, y, blockZ);
        }

        private void setBlock(int blockX, int y, int blockZ, BlockType type) {
            setBlock(blockX, y, blockZ, type, 0);
        }

        private void setBlock(int blockX, int y, int blockZ, BlockType type, int metadata) {
            if (y >= 0 && y < Chunk.HEIGHT) {
                blocks.put(new SourceBlockPos(blockX, y, blockZ), new SourceBlock(type, metadata));
            }
        }
    }

    private enum SourceTreeKind {
        NORMAL,
        BIRCH,
        BIG,
        SWAMP,
        TAIGA1,
        TAIGA2
    }

    private OverworldDecorationResult decorateOverworld(World world, Chunk chunk, int chunkX, int chunkZ) {
        return decorateOverworld(world, chunk, chunkX, chunkZ, null);
    }

    private OverworldDecorationResult decorateOverworld(World world, Chunk chunk, int chunkX, int chunkZ,
            SourceTreeScratch populationScratch) {
        Random random = overworldDecoratorRandom(world, chunkX, chunkZ);
        SourceTreeScratch scratch = populationScratch == null
                ? sourceDecoratorScratch(world, chunk, chunkX, chunkZ)
                : sourceDecoratorScratch(world, chunk, chunkX, chunkZ, populationScratch);
        DecoratorOrigin[] origins = placeOverworldDisks(world, chunk, chunkX, chunkZ, random, scratch);
        advanceOverworldTreeDecorators(scratch, origins);
        scratch.applyToChunk();
        placeHugeMushrooms(chunk, chunkX, chunkZ, origins, scratch);
        decorateOverworldDetails(chunk, chunkX, chunkZ, origins, scratch);
        return new OverworldDecorationResult(random, scratch);
    }

    private void spawnWorldGenCreatures(World world, Chunk chunk, int chunkX, int chunkZ, Random random) {
        spawnWorldGenCreatures(world, chunk, chunkX, chunkZ, random, null);
    }

    private void spawnWorldGenCreatures(World world, Chunk chunk, int chunkX, int chunkZ, Random random,
            SourceTreeScratch scratch) {
        if (world == null || dimension != Dimension.OVERWORLD) {
            return;
        }
        BiomeType biome = getBiome(chunkX * Chunk.WIDTH + 16, chunkZ * Chunk.DEPTH + 16);
        WorldGenSpawnEntry[] entries = worldGenCreatureEntries(biome);
        if (entries.length == 0) {
            return;
        }

        int originX = chunkX * Chunk.WIDTH + 8;
        int originZ = chunkZ * Chunk.DEPTH + 8;
        while (random.nextFloat() < worldGenCreatureSpawningChance(biome)) {
            WorldGenSpawnEntry entry = chooseWorldGenCreature(entries, world);
            int groupSize = entry.minGroup() + random.nextInt(entry.maxGroup() - entry.minGroup() + 1);
            int x = originX + random.nextInt(Chunk.WIDTH);
            int z = originZ + random.nextInt(Chunk.DEPTH);
            int groupBaseX = x;
            int groupBaseZ = z;

            for (int groupIndex = 0; groupIndex < groupSize; groupIndex++) {
                boolean placed = false;
                for (int attempt = 0; !placed && attempt < 4; attempt++) {
                    int y = sourceCreatureSpawnY(chunk, chunkX, chunkZ, x, z, scratch);
                    if (canSpawnWorldGenCreature(chunk, chunkX, chunkZ, entry.mob(), x, y, z, scratch)) {
                        float yaw = random.nextFloat() * 360.0F;
                        Mob mob = MobFactory.create(entry.mob());
                        if (mob != null) {
                            if (mob instanceof Sheep sheep) {
                                sheep.setWoolColor(world.nextReleaseOneWorldGenSheepColor());
                            }
                            mob.setPosition(x + 0.5F, y, z + 0.5F);
                            mob.setYaw(yaw);
                            world.stageGeneratedEntity(mob);
                        }
                        placed = true;
                    }

                    x += random.nextInt(5) - random.nextInt(5);
                    z += random.nextInt(5) - random.nextInt(5);
                    while (x < originX || x >= originX + Chunk.WIDTH
                            || z < originZ || z >= originZ + Chunk.DEPTH) {
                        x = groupBaseX + random.nextInt(5) - random.nextInt(5);
                        z = groupBaseZ + random.nextInt(5) - random.nextInt(5);
                    }
                }
            }
        }
    }

    private WorldGenSpawnEntry[] worldGenCreatureEntries(BiomeType biome) {
        return switch (biome) {
            case OCEAN, FROZEN_OCEAN, RIVER, FROZEN_RIVER, DESERT, DESERT_HILLS,
                    BEACH, HELL, SKY -> NO_WORLD_GEN_CREATURES;
            case MUSHROOM_ISLAND, MUSHROOM_ISLAND_SHORE -> MUSHROOM_WORLD_GEN_CREATURES;
            case FOREST, FOREST_HILLS -> FOREST_WORLD_GEN_CREATURES;
            case TAIGA, TAIGA_HILLS -> TAIGA_WORLD_GEN_CREATURES;
            default -> STANDARD_WORLD_GEN_CREATURES;
        };
    }

    private float worldGenCreatureSpawningChance(BiomeType biome) {
        return 0.1F;
    }

    private static WorldGenSpawnEntry chooseWorldGenCreature(WorldGenSpawnEntry[] entries, World world) {
        int totalWeight = 0;
        for (WorldGenSpawnEntry entry : entries) {
            totalWeight += Math.max(0, entry.weight());
        }
        int roll = world.nextReleaseOneWorldGenCreatureRandomInt(totalWeight);
        for (WorldGenSpawnEntry entry : entries) {
            roll -= Math.max(0, entry.weight());
            if (roll < 0) {
                return entry;
            }
        }
        return entries[0];
    }

    private boolean canSpawnWorldGenCreature(Chunk chunk, int chunkX, int chunkZ, MobDefinition mob,
            int blockX, int y, int blockZ) {
        return canSpawnWorldGenCreature(chunk, chunkX, chunkZ, mob, blockX, y, blockZ, null);
    }

    private boolean canSpawnWorldGenCreature(Chunk chunk, int chunkX, int chunkZ, MobDefinition mob,
            int blockX, int y, int blockZ, SourceTreeScratch scratch) {
        return sourceNormalCubeAt(chunk, chunkX, chunkZ, blockX, y - 1, blockZ, scratch)
                && !sourceNormalCubeAt(chunk, chunkX, chunkZ, blockX, y, blockZ, scratch)
                && !blockAtDecoratedOrBase(chunk, chunkX, chunkZ, blockX, y, blockZ, scratch).isFluid()
                && !sourceNormalCubeAt(chunk, chunkX, chunkZ, blockX, y + 1, blockZ, scratch);
    }

    private int sourceCreatureSpawnY(Chunk chunk, int chunkX, int chunkZ, int blockX, int blockZ,
            SourceTreeScratch scratch) {
        for (int y = Chunk.HEIGHT - 1; y > 0; y--) {
            if (isSourceFindTopSolidBlock(blockAtDecoratedOrBase(chunk, chunkX, chunkZ,
                    blockX, y, blockZ, scratch))) {
                return y + 1;
            }
        }
        return -1;
    }

    private static boolean isSourceFindTopSolidBlock(BlockType block) {
        return block != BlockType.AIR && block != BlockType.LEAVES && block.isSolid();
    }

    private boolean sourceNormalCubeAt(Chunk chunk, int chunkX, int chunkZ,
            int blockX, int y, int blockZ, SourceTreeScratch scratch) {
        if (y < 0 || y >= Chunk.HEIGHT) {
            return false;
        }
        return isSourceNormalCube(blockAtDecoratedOrBase(chunk, chunkX, chunkZ, blockX, y, blockZ, scratch));
    }

    private static boolean isSourceNormalCube(BlockType block) {
        if (block == BlockType.LEAVES || block == BlockType.CACTUS || block == BlockType.TNT) {
            return false;
        }
        return BlockShape.isOpaqueCube(block);
    }

    private BlockShape.BlockContext decoratedBlockContext(Chunk chunk, int chunkX, int chunkZ,
            int blockX, int y, int blockZ) {
        return decoratedBlockContext(chunk, chunkX, chunkZ, blockX, y, blockZ, null);
    }

    private BlockShape.BlockContext decoratedBlockContext(Chunk chunk, int chunkX, int chunkZ,
            int blockX, int y, int blockZ, SourceTreeScratch scratch) {
        return new BlockShape.BlockContext() {
            @Override
            public BlockType getBlock(int dx, int dy, int dz) {
                return blockAtDecoratedOrBase(chunk, chunkX, chunkZ,
                        blockX + dx, y + dy, blockZ + dz, scratch);
            }

            @Override
            public int getMetadata(int dx, int dy, int dz) {
                return metadataAtDecoratedOrBase(chunk, chunkX, chunkZ,
                        blockX + dx, y + dy, blockZ + dz, scratch);
            }
        };
    }

    private void decorateOverworldDetails(Chunk chunk, int chunkX, int chunkZ, DecoratorOrigin[] origins) {
        decorateOverworldDetails(chunk, chunkX, chunkZ, origins, null);
    }

    private void decorateOverworldDetails(Chunk chunk, int chunkX, int chunkZ, DecoratorOrigin[] origins,
            SourceTreeScratch scratch) {
        GeneratedBlockLight blockLight = generatedWorldBlockLightSnapshot(chunk, scratch);
        for (DecoratorOrigin origin : origins) {
            decorateOverworldDetailsFromOrigin(chunk, chunkX, chunkZ, origin, scratch, blockLight);
        }
    }

    private void decorateOverworldDetailsFromOrigin(Chunk chunk, int chunkX, int chunkZ, DecoratorOrigin origin) {
        decorateOverworldDetailsFromOrigin(chunk, chunkX, chunkZ, origin, null);
    }

    private void decorateOverworldDetailsFromOrigin(Chunk chunk, int chunkX, int chunkZ, DecoratorOrigin origin,
            SourceTreeScratch scratch) {
        decorateOverworldDetailsFromOrigin(chunk, chunkX, chunkZ, origin, scratch, null);
    }

    private void decorateOverworldDetailsFromOrigin(Chunk chunk, int chunkX, int chunkZ, DecoratorOrigin origin,
            SourceTreeScratch scratch, GeneratedBlockLight blockLight) {
        Random random = origin.random();
        int baseX = origin.chunkX() * Chunk.WIDTH;
        int baseZ = origin.chunkZ() * Chunk.DEPTH;
        BiomeType biome = getBiome(baseX + 16, baseZ + 16);

        for (int i = 0; i < positiveDecoratorCount(flowersPerChunk(biome)); i++) {
            placeFlowerScatter(chunk, chunkX, chunkZ, random, baseX, baseZ,
                    BlockType.YELLOW_FLOWER, scratch, blockLight);
            if (random.nextInt(4) == 0) {
                placeFlowerScatter(chunk, chunkX, chunkZ, random, baseX, baseZ,
                        BlockType.RED_ROSE, scratch, blockLight);
            }
        }
        for (int i = 0; i < positiveDecoratorCount(grassPerChunk(biome)); i++) {
            placeTallGrassScatter(chunk, chunkX, chunkZ, random, baseX, baseZ,
                    tallGrassMetadataForBiome(biome, random), scratch, blockLight);
        }
        for (int i = 0; i < deadBushPerChunk(biome); i++) {
            placeDeadBushScatter(chunk, chunkX, chunkZ, random, baseX, baseZ, scratch, blockLight);
        }
        for (int i = 0; i < waterlilyPerChunk(biome); i++) {
            placeWaterLilyScatter(chunk, chunkX, chunkZ, random, baseX, baseZ, scratch);
        }
        for (int i = 0; i < mushroomsPerChunk(biome); i++) {
            placeMushroomPair(chunk, chunkX, chunkZ, random, baseX, baseZ, scratch, blockLight);
        }
        if (random.nextInt(4) == 0) {
            placeFlowerScatter(chunk, chunkX, chunkZ, random, baseX, baseZ,
                    BlockType.BROWN_MUSHROOM, scratch, blockLight);
        }
        if (random.nextInt(8) == 0) {
            placeFlowerScatter(chunk, chunkX, chunkZ, random, baseX, baseZ,
                    BlockType.RED_MUSHROOM, scratch, blockLight);
        }
        for (int i = 0; i < reedsPerChunk(biome); i++) {
            placeBiomeReedScatter(chunk, chunkX, chunkZ, random, baseX, baseZ, scratch);
        }
        for (int i = 0; i < 10; i++) {
            placeReedScatter(chunk, chunkX, chunkZ, random, baseX, baseZ, scratch);
        }
        if (random.nextInt(32) == 0) {
            placePumpkinScatter(chunk, chunkX, chunkZ, random, baseX, baseZ, scratch);
        }
        for (int i = 0; i < cactiPerChunk(biome); i++) {
            placeCactusScatter(chunk, chunkX, chunkZ, random, baseX, baseZ, scratch);
        }
        for (int i = 0; i < 50; i++) {
            placeOverworldSpring(chunk, chunkX, chunkZ, random, origin.chunkX(), origin.chunkZ(),
                    BlockType.FLOWING_WATER, false, scratch);
        }
        for (int i = 0; i < 20; i++) {
            placeOverworldSpring(chunk, chunkX, chunkZ, random, origin.chunkX(), origin.chunkZ(),
                    BlockType.FLOWING_LAVA, true, scratch);
        }
    }

    private DecoratorOrigin[] placeOverworldDisks(World world, Chunk chunk, int chunkX, int chunkZ, Random random) {
        return placeOverworldDisks(world, chunk, chunkX, chunkZ, random, null);
    }

    private DecoratorOrigin[] placeOverworldDisks(World world, Chunk chunk, int chunkX, int chunkZ, Random random,
            SourceTreeScratch scratch) {
        int width = SCRATCH_POPULATION_MAX_CHUNK_OFFSET - SCRATCH_POPULATION_MIN_CHUNK_OFFSET + 1;
        DecoratorOrigin[] origins = new DecoratorOrigin[width * width];
        int index = 0;
        for (int originChunkX = chunkX + SCRATCH_POPULATION_MIN_CHUNK_OFFSET;
                originChunkX <= chunkX + SCRATCH_POPULATION_MAX_CHUNK_OFFSET; originChunkX++) {
            for (int originChunkZ = chunkZ + SCRATCH_POPULATION_MIN_CHUNK_OFFSET;
                    originChunkZ <= chunkZ + SCRATCH_POPULATION_MAX_CHUNK_OFFSET; originChunkZ++) {
                Random originRandom = originChunkX == chunkX && originChunkZ == chunkZ
                        ? random
                        : overworldDecoratorRandom(world, originChunkX, originChunkZ);
                placeCurrentOverworldDisks(chunk, chunkX, chunkZ, originRandom, originChunkX, originChunkZ, scratch);
                origins[index++] = new DecoratorOrigin(originChunkX, originChunkZ, originRandom);
            }
        }
        return origins;
    }

    private SourceTreeScratch advanceOverworldTreeDecorators(World world, Chunk chunk, int chunkX, int chunkZ,
            DecoratorOrigin[] origins) {
        SourceTreeScratch scratch = sourceLakeSideEffectScratch(world, chunk, chunkX, chunkZ);
        advanceOverworldTreeDecorators(scratch, origins);
        return scratch;
    }

    private void advanceOverworldTreeDecorators(SourceTreeScratch scratch, DecoratorOrigin[] origins) {
        for (DecoratorOrigin origin : origins) {
            advanceOverworldTreeDecoratorFromOrigin(scratch, origin);
        }
    }

    private SourceTreeScratch sourceDecoratorScratch(World world, Chunk chunk, int chunkX, int chunkZ) {
        SourceTreeScratch scratch = sourceLakeSideEffectScratch(world, chunk, chunkX, chunkZ);
        overlayOverworldDungeonSideEffects(world, scratch, chunkX, chunkZ);
        overlayOverworldOreSideEffects(world, scratch, chunkX, chunkZ);
        return scratch;
    }

    private SourceTreeScratch sourceDecoratorScratch(World world, Chunk chunk, int chunkX, int chunkZ,
            SourceTreeScratch scratch) {
        return scratch;
    }

    private SourceTreeScratch sourceLakeSideEffectScratch(World world, Chunk chunk, int chunkX, int chunkZ) {
        SourceTreeScratch scratch = sourceStructureSideEffectScratch(chunk, chunkX, chunkZ);
        overlayOverworldLakeSideEffects(world, scratch, chunkX, chunkZ);
        return scratch;
    }

    private SourceTreeScratch sourceStructureSideEffectScratch(Chunk chunk, int chunkX, int chunkZ) {
        SourceTreeScratch scratch = new SourceTreeScratch(chunk, chunkX, chunkZ);
        overlayOverworldStructureSideEffects(scratch, chunkX, chunkZ);
        return scratch;
    }

    private void overlayOverworldStructureSideEffects(SourceTreeScratch scratch, int chunkX, int chunkZ) {
        if (!generateStructures) {
            return;
        }
        for (int structureChunkX = chunkX + SCRATCH_POPULATION_MIN_CHUNK_OFFSET;
                structureChunkX <= chunkX + SCRATCH_POPULATION_MAX_CHUNK_OFFSET; structureChunkX++) {
            for (int structureChunkZ = chunkZ + SCRATCH_POPULATION_MIN_CHUNK_OFFSET;
                    structureChunkZ <= chunkZ + SCRATCH_POPULATION_MAX_CHUNK_OFFSET; structureChunkZ++) {
                if (structureChunkX == chunkX && structureChunkZ == chunkZ) {
                    continue;
                }
                overlayOverworldStructureChunk(scratch, structureChunkX, structureChunkZ);
            }
        }
    }

    private void overlayOverworldStructureChunk(SourceTreeScratch scratch, int structureChunkX, int structureChunkZ) {
        int baseX = structureChunkX * Chunk.WIDTH;
        int baseZ = structureChunkZ * Chunk.DEPTH;
        for (StructureBlockDelta delta : cachedOverworldStructureDeltas(structureChunkX, structureChunkZ)) {
            int blockX = baseX + delta.localX();
            int blockZ = baseZ + delta.localZ();
            if (!containsBlock(scratch.chunkX, scratch.chunkZ, blockX, blockZ)) {
                scratch.setBlock(blockX, delta.y(), blockZ, delta.type(), delta.metadata());
            }
        }
    }

    private Chunk overworldChunkAfterStructures(int chunkX, int chunkZ) {
        return copyChunk(cachedOverworldStructureChunk(chunkX, chunkZ));
    }

    private Chunk cachedOverworldStructureChunk(int chunkX, int chunkZ) {
        if (!generateStructures) {
            return cachedOverworldCarvedChunk(chunkX, chunkZ);
        }
        long key = World.chunkKey(chunkX, chunkZ);
        Chunk cached = overworldStructureChunkCache.get(key);
        if (cached != null) {
            return cached;
        }
        Chunk chunk = copyChunk(cachedOverworldCarvedChunk(chunkX, chunkZ));
        Random random = populationRandom(chunkX, chunkZ);
        structureGenerator.generate(null, chunk, seed, chunkX, chunkZ, Dimension.OVERWORLD, this, random);
        chunk.clearModified();
        if (overworldStructureChunkCache.size() > MAX_BASE_CHUNK_CACHE_ENTRIES) {
            overworldStructureChunkCache.clear();
            overworldStructureDeltaCache.clear();
        }
        Chunk previous = overworldStructureChunkCache.putIfAbsent(key, chunk);
        return previous == null ? chunk : previous;
    }

    private List<StructureBlockDelta> cachedOverworldStructureDeltas(int chunkX, int chunkZ) {
        if (!generateStructures) {
            return List.of();
        }
        long key = World.chunkKey(chunkX, chunkZ);
        List<StructureBlockDelta> cached = overworldStructureDeltaCache.get(key);
        if (cached != null) {
            return cached;
        }
        Chunk before = cachedOverworldCarvedChunk(chunkX, chunkZ);
        Chunk after = cachedOverworldStructureChunk(chunkX, chunkZ);
        ArrayList<StructureBlockDelta> deltas = new ArrayList<>();
        for (int y = 0; y < Chunk.HEIGHT; y++) {
            for (int z = 0; z < Chunk.DEPTH; z++) {
                for (int x = 0; x < Chunk.WIDTH; x++) {
                    BlockType type = after.getBlock(x, y, z);
                    int metadata = after.getBlockMetadata(x, y, z);
                    if (type != before.getBlock(x, y, z) || metadata != before.getBlockMetadata(x, y, z)) {
                        deltas.add(new StructureBlockDelta(x, y, z, type, metadata));
                    }
                }
            }
        }
        List<StructureBlockDelta> result = List.copyOf(deltas);
        if (overworldStructureDeltaCache.size() > MAX_BASE_CHUNK_CACHE_ENTRIES) {
            overworldStructureDeltaCache.clear();
        }
        List<StructureBlockDelta> previous = overworldStructureDeltaCache.putIfAbsent(key, result);
        return previous == null ? result : previous;
    }

    private void overlayChunkDifferences(SourceTreeScratch scratch, Chunk before, Chunk after,
            int sourceChunkX, int sourceChunkZ) {
        int baseX = sourceChunkX * Chunk.WIDTH;
        int baseZ = sourceChunkZ * Chunk.DEPTH;
        for (int y = 0; y < Chunk.HEIGHT; y++) {
            for (int z = 0; z < Chunk.DEPTH; z++) {
                for (int x = 0; x < Chunk.WIDTH; x++) {
                    BlockType type = after.getBlock(x, y, z);
                    int metadata = after.getBlockMetadata(x, y, z);
                    if (type == before.getBlock(x, y, z)
                            && metadata == before.getBlockMetadata(x, y, z)) {
                        continue;
                    }
                    int blockX = baseX + x;
                    int blockZ = baseZ + z;
                    if (!containsBlock(scratch.chunkX, scratch.chunkZ, blockX, blockZ)) {
                        scratch.setBlock(blockX, y, blockZ, type, metadata);
                    }
                }
            }
        }
    }

    private void overlayOverworldOreSideEffects(World world, SourceTreeScratch scratch, int chunkX, int chunkZ) {
        overlayOverworldOreSideEffects(world, scratch, chunkX, chunkZ, false);
    }

    private void overlayMissingOverworldOreSideEffects(World world, SourceTreeScratch scratch,
            int chunkX, int chunkZ) {
        overlayOverworldOreSideEffects(world, scratch, chunkX, chunkZ, true);
    }

    private void overlayOverworldOreSideEffects(World world, SourceTreeScratch scratch, int chunkX, int chunkZ,
            boolean skipVisibleOrigins) {
        OreGenerator.BlockReader blocks = scratch::getBlock;
        OreGenerator.BlockWriter writer = (blockX, y, blockZ, block) -> {
            if (!containsBlock(chunkX, chunkZ, blockX, blockZ)) {
                scratch.setBlock(blockX, y, blockZ, block);
            }
        };
        for (int originChunkX = chunkX + SCRATCH_POPULATION_MIN_CHUNK_OFFSET;
                originChunkX <= chunkX + SCRATCH_POPULATION_MAX_CHUNK_OFFSET; originChunkX++) {
            for (int originChunkZ = chunkZ + SCRATCH_POPULATION_MIN_CHUNK_OFFSET;
                    originChunkZ <= chunkZ + SCRATCH_POPULATION_MAX_CHUNK_OFFSET; originChunkZ++) {
                if (skipVisibleOrigins && isVisibleOverworldPopulationOrigin(chunkX, chunkZ,
                        originChunkX, originChunkZ)) {
                    continue;
                }
                Random random = overworldOreRandom(world, originChunkX, originChunkZ);
                oreGenerator.generateFromOrigin(random, originChunkX, originChunkZ, blocks, writer);
            }
        }
    }

    private void overlayOverworldDungeonSideEffects(World world, SourceTreeScratch scratch, int chunkX, int chunkZ) {
        overlayOverworldDungeonSideEffects(world, scratch, chunkX, chunkZ, false);
    }

    private void overlayMissingOverworldDungeonSideEffects(World world, SourceTreeScratch scratch,
            int chunkX, int chunkZ) {
        overlayOverworldDungeonSideEffects(world, scratch, chunkX, chunkZ, true);
    }

    private void overlayOverworldDungeonSideEffects(World world, SourceTreeScratch scratch,
            int chunkX, int chunkZ, boolean skipVisibleOrigins) {
        DungeonGenerator.BlockReader blocks = scratch::getBlock;
        DungeonGenerator.BlockWriter writer = (blockX, y, blockZ, block) -> {
            if (!containsBlock(chunkX, chunkZ, blockX, blockZ)) {
                scratch.setBlock(blockX, y, blockZ, block);
            }
            return false;
        };
        for (int originChunkX = chunkX + SCRATCH_POPULATION_MIN_CHUNK_OFFSET;
                originChunkX <= chunkX + SCRATCH_POPULATION_MAX_CHUNK_OFFSET; originChunkX++) {
            for (int originChunkZ = chunkZ + SCRATCH_POPULATION_MIN_CHUNK_OFFSET;
                    originChunkZ <= chunkZ + SCRATCH_POPULATION_MAX_CHUNK_OFFSET; originChunkZ++) {
                if (skipVisibleOrigins && isVisibleOverworldPopulationOrigin(chunkX, chunkZ,
                        originChunkX, originChunkZ)) {
                    continue;
                }
                Random random = overworldDungeonRandom(world, originChunkX, originChunkZ);
                dungeonGenerator.generateFromOrigin(null, scratch.chunk, random,
                        chunkX, chunkZ, originChunkX, originChunkZ, blocks, writer);
            }
        }
    }

    private void overlayOverworldLakeSideEffects(World world, SourceTreeScratch scratch, int chunkX, int chunkZ) {
        overlayOverworldLakeSideEffects(world, scratch, chunkX, chunkZ, false);
    }

    private void overlayMissingOverworldLakeSideEffects(World world, SourceTreeScratch scratch,
            int chunkX, int chunkZ) {
        overlayOverworldLakeSideEffects(world, scratch, chunkX, chunkZ, true);
    }

    private void overlayOverworldLakeSideEffects(World world, SourceTreeScratch scratch,
            int chunkX, int chunkZ, boolean skipVisibleOrigins) {
        for (int originChunkX = chunkX + SCRATCH_POPULATION_MIN_CHUNK_OFFSET;
                originChunkX <= chunkX + SCRATCH_POPULATION_MAX_CHUNK_OFFSET; originChunkX++) {
            for (int originChunkZ = chunkZ + SCRATCH_POPULATION_MIN_CHUNK_OFFSET;
                    originChunkZ <= chunkZ + SCRATCH_POPULATION_MAX_CHUNK_OFFSET; originChunkZ++) {
                if (skipVisibleOrigins && isVisibleOverworldPopulationOrigin(chunkX, chunkZ,
                        originChunkX, originChunkZ)) {
                    continue;
                }
                Random random = overworldPopulationRandomAfterStructures(originChunkX, originChunkZ);
                if (structureGenerator.suppressesOverworldLakes(seed, originChunkX, originChunkZ, this)) {
                    continue;
                }
                int originX = originChunkX * Chunk.WIDTH;
                int originZ = originChunkZ * Chunk.DEPTH;

                if (random.nextInt(4) == 0) {
                    int x = originX + random.nextInt(16) + 8;
                    int y = random.nextInt(128);
                    int z = originZ + random.nextInt(16) + 8;
                    applyLakeCandidateToScratch(scratch,
                            buildLakeCandidate(scratch, originChunkX, originChunkZ, false, random, x, y, z,
                                    BlockType.WATER));
                }

                if (random.nextInt(8) == 0) {
                    int x = originX + random.nextInt(16) + 8;
                    int y = random.nextInt(random.nextInt(120) + 8);
                    int z = originZ + random.nextInt(16) + 8;
                    if (y < SEA_LEVEL || random.nextInt(10) == 0) {
                        applyLakeCandidateToScratch(scratch,
                                buildLakeCandidate(scratch, originChunkX, originChunkZ, false, random, x, y, z,
                                        BlockType.LAVA));
                    }
                }
            }
        }
    }

    private boolean isVisibleOverworldPopulationOrigin(int chunkX, int chunkZ,
            int originChunkX, int originChunkZ) {
        return originChunkX >= chunkX + VISIBLE_POPULATION_MIN_CHUNK_OFFSET
                && originChunkX <= chunkX + VISIBLE_POPULATION_MAX_CHUNK_OFFSET
                && originChunkZ >= chunkZ + VISIBLE_POPULATION_MIN_CHUNK_OFFSET
                && originChunkZ <= chunkZ + VISIBLE_POPULATION_MAX_CHUNK_OFFSET;
    }

    private void applyLakeCandidateToScratch(SourceTreeScratch scratch, LakeCandidate lake) {
        applyLakeCandidateToScratch(scratch, lake, false);
    }

    private void applyLakeCandidateToScratch(SourceTreeScratch scratch, LakeCandidate lake,
            boolean includeTargetChunk) {
        if (lake == null) {
            return;
        }
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = 0; y < 8; y++) {
                    if (!lake.mask()[lakeIndex(x, z, y)]) {
                        continue;
                    }
                    setLakeScratchBlock(scratch, lake.originX() + x, lake.originY() + y, lake.originZ() + z,
                            y < 4 ? lake.block() : BlockType.AIR, includeTargetChunk);
                }
            }
        }

        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = 4; y < 8; y++) {
                    if (!lake.mask()[lakeIndex(x, z, y)]) {
                        continue;
                    }
                    restoreLakeSurfaceInScratch(scratch,
                            lake.originX() + x, lake.originY() + y - 1, lake.originZ() + z, includeTargetChunk);
                }
            }
        }

        if (lake.block().isLava()) {
            addLavaLakeStoneShellToScratch(scratch, lake, includeTargetChunk);
        } else {
            freezeLakeSurfaceInScratch(scratch, lake, includeTargetChunk);
        }
    }

    private void restoreLakeSurfaceInScratch(SourceTreeScratch scratch, int worldX, int worldY, int worldZ,
            boolean includeTargetChunk) {
        if (worldY < 0 || worldY >= Chunk.HEIGHT
                || !includeTargetChunk && containsBlock(scratch.chunkX, scratch.chunkZ, worldX, worldZ)
                || scratch.getBlock(worldX, worldY, worldZ) != BlockType.DIRT) {
            return;
        }
        BiomeType biome = getBiome(worldX, worldZ);
        setLakeScratchBlock(scratch, worldX, worldY, worldZ,
                biome.getTopBlock() == BlockType.MYCELIUM ? BlockType.MYCELIUM : BlockType.GRASS,
                includeTargetChunk);
    }

    private void addLavaLakeStoneShellToScratch(SourceTreeScratch scratch, LakeCandidate lake,
            boolean includeTargetChunk) {
        boolean[] shell = lake.shell();
        if (shell == null) {
            return;
        }
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = 0; y < 8; y++) {
                    if (!shell[lakeIndex(x, z, y)]) {
                        continue;
                    }
                    int worldX = lake.originX() + x;
                    int worldY = lake.originY() + y;
                    int worldZ = lake.originZ() + z;
                    if ((includeTargetChunk || !containsBlock(scratch.chunkX, scratch.chunkZ, worldX, worldZ))
                            && scratch.getBlock(worldX, worldY, worldZ).isSolid()) {
                        setLakeScratchBlock(scratch, worldX, worldY, worldZ, BlockType.STONE, includeTargetChunk);
                    }
                }
            }
        }
    }

    private void freezeLakeSurfaceInScratch(SourceTreeScratch scratch, LakeCandidate lake,
            boolean includeTargetChunk) {
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                int worldX = lake.originX() + x;
                int worldZ = lake.originZ() + z;
                if (!isSourceFreezingTemperature(worldX, worldZ)) {
                    continue;
                }
                for (int y = 3; y >= 0; y--) {
                    if (!lake.mask()[lakeIndex(x, z, y)]) {
                        continue;
                    }
                    int worldY = lake.originY() + y;
                    if ((includeTargetChunk || !containsBlock(scratch.chunkX, scratch.chunkZ, worldX, worldZ))
                            && scratch.getBlock(worldX, worldY + 1, worldZ) == BlockType.AIR) {
                        setLakeScratchBlock(scratch, worldX, worldY, worldZ, BlockType.ICE, includeTargetChunk);
                        break;
                    }
                }
            }
        }
    }

    private void setLakeScratchBlock(SourceTreeScratch scratch, int worldX, int worldY, int worldZ,
            BlockType block, boolean includeTargetChunk) {
        if (includeTargetChunk || !containsBlock(scratch.chunkX, scratch.chunkZ, worldX, worldZ)) {
            scratch.setBlock(worldX, worldY, worldZ, block);
        }
    }

    private void setExternalScratchBlock(SourceTreeScratch scratch, int worldX, int worldY, int worldZ,
            BlockType block) {
        setLakeScratchBlock(scratch, worldX, worldY, worldZ, block, false);
    }

    private void advanceOverworldTreeDecoratorFromOrigin(SourceTreeScratch scratch, DecoratorOrigin origin) {
        Random random = origin.random();
        int originX = origin.chunkX() * Chunk.WIDTH;
        int originZ = origin.chunkZ() * Chunk.DEPTH;
        BiomeType biome = getBiome(originX + 16, originZ + 16);
        int attempts = sourceTreesPerChunk(biome);
        if (random.nextInt(10) == 0) {
            attempts++;
        }
        for (int i = 0; i < attempts; i++) {
            int x = originX + random.nextInt(Chunk.WIDTH) + 8;
            int z = originZ + random.nextInt(Chunk.DEPTH) + 8;
            SourceTreeKind kind = sourceTreeKindForBiome(biome, random);
            int y = sourceHeightValue(scratch, x, z);
            advanceSourceTreeGenerator(scratch, random, x, y, z, kind);
        }
    }

    private int sourceTreesPerChunk(BiomeType biome) {
        return switch (biome) {
            case FOREST, FOREST_HILLS, TAIGA, TAIGA_HILLS -> 10;
            case SWAMPLAND -> 2;
            case PLAINS, DESERT, DESERT_HILLS, BEACH -> -999;
            case MUSHROOM_ISLAND, MUSHROOM_ISLAND_SHORE -> -100;
            default -> 0;
        };
    }

    private SourceTreeKind sourceTreeKindForBiome(BiomeType biome, Random random) {
        if (biome == BiomeType.TAIGA || biome == BiomeType.TAIGA_HILLS) {
            return random.nextInt(3) == 0 ? SourceTreeKind.TAIGA1 : SourceTreeKind.TAIGA2;
        }
        if (biome == BiomeType.FOREST || biome == BiomeType.FOREST_HILLS) {
            if (random.nextInt(5) == 0) {
                return SourceTreeKind.BIRCH;
            }
            if (random.nextInt(10) == 0) {
                return SourceTreeKind.BIG;
            }
            return SourceTreeKind.NORMAL;
        }
        if (biome == BiomeType.SWAMPLAND) {
            return SourceTreeKind.SWAMP;
        }
        return random.nextInt(10) == 0 ? SourceTreeKind.BIG : SourceTreeKind.NORMAL;
    }

    private void advanceSourceTreeGenerator(SourceTreeScratch scratch, Random random,
            int x, int y, int z, SourceTreeKind kind) {
        switch (kind) {
            case BIRCH -> advanceSourceNormalTree(scratch, random, x, y, z,
                    random.nextInt(3) + 5, 2, false);
            case BIG -> advanceSourceBigTree(scratch, random, x, y, z);
            case SWAMP -> advanceSourceSwampTree(scratch, random, x, y, z);
            case TAIGA1 -> advanceSourceTaiga1Tree(scratch, random, x, y, z);
            case TAIGA2 -> advanceSourceTaiga2Tree(scratch, random, x, y, z);
            case NORMAL -> advanceSourceNormalTree(scratch, random, x, y, z,
                    random.nextInt(3) + 4, 0, false);
        }
    }

    private void advanceSourceNormalTree(SourceTreeScratch scratch, Random random,
            int x, int y, int z, int height, int metadata, boolean broadCrown) {
        if (!canPlaceSourceNormalTree(scratch, x, y, z, height, broadCrown ? 3 : 2)) {
            return;
        }
        scratch.setBlock(x, y - 1, z, BlockType.DIRT);
        placeSourceNormalLeaves(scratch, random, x, y, z, height, metadata, broadCrown ? 2 : 1);
        for (int dy = 0; dy < height; dy++) {
            if (isSourceNormalTreeTrunkReplaceable(scratch.getBlock(x, y + dy, z))) {
                scratch.setBlock(x, y + dy, z, BlockType.OAK_LOG, metadata);
            }
        }
    }

    private void advanceSourceSwampTree(SourceTreeScratch scratch, Random random, int x, int y, int z) {
        int height = random.nextInt(4) + 5;
        while (scratch.getBlock(x, y - 1, z).isWater()) {
            y--;
        }
        if (!canPlaceSourceSwampTree(scratch, x, y, z, height)) {
            return;
        }
        scratch.setBlock(x, y - 1, z, BlockType.DIRT);
        placeSourceSwampLeavesAndVines(scratch, random, x, y, z, height);
        for (int dy = 0; dy < height; dy++) {
            if (isSourceSwampTreeTrunkReplaceable(scratch.getBlock(x, y + dy, z))) {
                scratch.setBlock(x, y + dy, z, BlockType.OAK_LOG, 0);
            }
        }
    }

    private void advanceSourceBigTree(SourceTreeScratch scratch, Random random, int x, int y, int z) {
        Random treeRandom = new Random(random.nextLong());
        int height = 5 + treeRandom.nextInt(12);
        int checkedHeight = sourceBigTreeValidatedHeight(scratch, x, y, z, height);
        if (checkedHeight < 0) {
            return;
        }
        height = checkedHeight;

        int trunkHeight = sourceBigTreeTrunkHeight(height);
        List<SourceBigLeafNode> nodes = sourceBigTreeLeafNodes(scratch, treeRandom, x, y, z, height, trunkHeight);
        scratch.setBlock(x, y - 1, z, BlockType.DIRT);
        for (SourceBigLeafNode node : nodes) {
            placeSourceBigLeafNode(scratch, node);
        }
        placeSourceBigBlockLine(scratch, new int[] { x, y, z }, new int[] { x, y + trunkHeight, z });
        for (SourceBigLeafNode node : nodes) {
            if (node.branchBaseY() - y >= height * 0.2D) {
                placeSourceBigBlockLine(scratch, new int[] { x, node.branchBaseY(), z },
                        new int[] { node.x(), node.y(), node.z() });
            }
        }
    }

    private int sourceBigTreeValidatedHeight(SourceTreeScratch scratch, int x, int y, int z, int height) {
        if (y < 1 || y + height + 1 > Chunk.HEIGHT) {
            return -1;
        }
        BlockType support = scratch.getBlock(x, y - 1, z);
        if (support != BlockType.GRASS && support != BlockType.DIRT) {
            return -1;
        }
        int obstruction = checkSourceBigBlockLine(scratch,
                new int[] { x, y, z },
                new int[] { x, y + height - 1, z });
        if (obstruction == -1) {
            return height;
        }
        return obstruction < 6 ? -1 : obstruction;
    }

    private int sourceBigTreeTrunkHeight(int height) {
        int trunkHeight = (int) (height * 0.618D);
        return trunkHeight >= height ? height - 1 : trunkHeight;
    }

    private List<SourceBigLeafNode> sourceBigTreeLeafNodes(SourceTreeScratch scratch, Random random,
            int x, int y, int z, int height, int trunkHeight) {
        int nodesPerLayer = (int) (1.382D + Math.pow(height / 13.0D, 2.0D));
        if (nodesPerLayer < 1) {
            nodesPerLayer = 1;
        }

        List<SourceBigLeafNode> nodes = new ArrayList<>();
        int leafY = y + height - 4;
        int trunkTopY = y + trunkHeight;
        int relativeY = leafY - y;
        nodes.add(new SourceBigLeafNode(x, leafY, z, trunkTopY));
        leafY--;
        relativeY--;

        while (relativeY >= 0) {
            float layerSize = sourceBigTreeLayerSize(height, relativeY);
            if (layerSize >= 0.0F) {
                for (int i = 0; i < nodesPerLayer; i++) {
                    double distance = layerSize * (random.nextFloat() + 0.328D);
                    double angle = random.nextFloat() * SOURCE_BIG_TREE_TWO_PI;
                    int nodeX = sourceFloor(distance * Math.sin(angle) + x + 0.5D);
                    int nodeZ = sourceFloor(distance * Math.cos(angle) + z + 0.5D);
                    int[] leafBase = { nodeX, leafY, nodeZ };
                    if (checkSourceBigBlockLine(scratch, leafBase, new int[] { nodeX, leafY + 4, nodeZ }) != -1) {
                        continue;
                    }

                    double horizontalDistance = Math.sqrt(Math.pow(Math.abs(x - nodeX), 2.0D)
                            + Math.pow(Math.abs(z - nodeZ), 2.0D));
                    double branchDrop = horizontalDistance * 0.381D;
                    int branchBaseY = leafY - branchDrop > trunkTopY
                            ? trunkTopY
                            : (int) (leafY - branchDrop);
                    if (checkSourceBigBlockLine(scratch, new int[] { x, branchBaseY, z }, leafBase) != -1) {
                        continue;
                    }
                    nodes.add(new SourceBigLeafNode(nodeX, leafY, nodeZ, branchBaseY));
                }
            }
            leafY--;
            relativeY--;
        }
        return nodes;
    }

    private float sourceBigTreeLayerSize(int height, int relativeY) {
        if (relativeY < height * 0.3D) {
            return -1.618F;
        }
        float halfHeight = height / 2.0F;
        float offset = halfHeight - relativeY;
        float radius;
        if (offset == 0.0F) {
            radius = halfHeight;
        } else if (Math.abs(offset) >= halfHeight) {
            radius = 0.0F;
        } else {
            radius = (float) Math.sqrt(Math.pow(Math.abs(halfHeight), 2.0D)
                    - Math.pow(Math.abs(offset), 2.0D));
        }
        return radius * 0.5F;
    }

    private float sourceBigTreeLeafSize(int offset) {
        if (offset < 0 || offset >= 4) {
            return -1.0F;
        }
        return offset == 0 || offset == 3 ? 2.0F : 3.0F;
    }

    private void placeSourceBigLeafNode(SourceTreeScratch scratch, SourceBigLeafNode node) {
        for (int offset = 0; offset < 4; offset++) {
            float radius = sourceBigTreeLeafSize(offset);
            if (radius >= 0.0F) {
                placeSourceBigLeafLayer(scratch, node.x(), node.y() + offset, node.z(), radius);
            }
        }
    }

    private void placeSourceBigLeafLayer(SourceTreeScratch scratch, int centerX, int y, int centerZ, float radius) {
        int blockRadius = (int) (radius + 0.618D);
        for (int dx = -blockRadius; dx <= blockRadius; dx++) {
            for (int dz = -blockRadius; dz <= blockRadius; dz++) {
                double distance = Math.sqrt(Math.pow(Math.abs(dx) + 0.5D, 2.0D)
                        + Math.pow(Math.abs(dz) + 0.5D, 2.0D));
                if (distance > radius) {
                    continue;
                }
                int leafX = centerX + dx;
                int leafZ = centerZ + dz;
                BlockType block = scratch.getBlock(leafX, y, leafZ);
                if (block == BlockType.AIR || block == BlockType.LEAVES) {
                    scratch.setBlock(leafX, y, leafZ, BlockType.LEAVES);
                }
            }
        }
    }

    private void placeSourceBigBlockLine(SourceTreeScratch scratch, int[] start, int[] end) {
        for (int[] point : sourceBigBlockLine(start, end, true)) {
            scratch.setBlock(point[0], point[1], point[2], BlockType.OAK_LOG);
        }
    }

    private int checkSourceBigBlockLine(SourceTreeScratch scratch, int[] start, int[] end) {
        List<int[]> points = sourceBigBlockLine(start, end, false);
        for (int i = 0; i < points.size(); i++) {
            int[] point = points.get(i);
            BlockType block = scratch.getBlock(point[0], point[1], point[2]);
            if (block != BlockType.AIR && block != BlockType.LEAVES) {
                return i;
            }
        }
        return -1;
    }

    private List<int[]> sourceBigBlockLine(int[] start, int[] end, boolean placement) {
        int[] delta = { end[0] - start[0], end[1] - start[1], end[2] - start[2] };
        int majorAxis = 0;
        for (int axis = 1; axis < 3; axis++) {
            if (Math.abs(delta[axis]) > Math.abs(delta[majorAxis])) {
                majorAxis = axis;
            }
        }
        if (delta[majorAxis] == 0) {
            return List.of();
        }

        int axisA = SOURCE_BIG_TREE_AXIS_PAIRS[majorAxis];
        int axisB = SOURCE_BIG_TREE_AXIS_PAIRS[majorAxis + 3];
        int step = delta[majorAxis] > 0 ? 1 : -1;
        double slopeA = (double) delta[axisA] / delta[majorAxis];
        double slopeB = (double) delta[axisB] / delta[majorAxis];
        int stop = delta[majorAxis] + step;
        List<int[]> points = new ArrayList<>();
        for (int distance = 0; distance != stop; distance += step) {
            int[] point = { 0, 0, 0 };
            if (placement) {
                point[majorAxis] = sourceFloor(start[majorAxis] + distance + 0.5D);
                point[axisA] = sourceFloor(start[axisA] + distance * slopeA + 0.5D);
                point[axisB] = sourceFloor(start[axisB] + distance * slopeB + 0.5D);
            } else {
                point[majorAxis] = start[majorAxis] + distance;
                point[axisA] = sourceFloor(start[axisA] + distance * slopeA);
                point[axisB] = sourceFloor(start[axisB] + distance * slopeB);
            }
            points.add(point);
        }
        return points;
    }

    private static int sourceFloor(double value) {
        int integer = (int) value;
        return value < integer ? integer - 1 : integer;
    }

    private void advanceSourceTaiga1Tree(SourceTreeScratch scratch, Random random, int x, int y, int z) {
        int height = random.nextInt(5) + 7;
        int leafStart = height - random.nextInt(2) - 3;
        int maxRadius = 1 + random.nextInt(height - leafStart + 1);
        if (!canPlaceSourceTaigaTree(scratch, x, y, z, height, leafStart, maxRadius)) {
            return;
        }
        scratch.setBlock(x, y - 1, z, BlockType.DIRT);
        int radius = 0;
        for (int leafY = y + height; leafY >= y + leafStart; leafY--) {
            for (int leafX = x - radius; leafX <= x + radius; leafX++) {
                int dx = leafX - x;
                for (int leafZ = z - radius; leafZ <= z + radius; leafZ++) {
                    int dz = leafZ - z;
                    boolean corner = Math.abs(dx) == radius && Math.abs(dz) == radius && radius > 0;
                    if (!corner && TreeFeature.isReplaceableForLeaves(scratch.getBlock(leafX, leafY, leafZ))) {
                        scratch.setBlock(leafX, leafY, leafZ, BlockType.LEAVES, 1);
                    }
                }
            }
            if (radius >= 1 && leafY == y + leafStart + 1) {
                radius--;
            } else if (radius < maxRadius) {
                radius++;
            }
        }
        for (int dy = 0; dy < height - 1; dy++) {
            if (isSourceNormalTreeTrunkReplaceable(scratch.getBlock(x, y + dy, z))) {
                scratch.setBlock(x, y + dy, z, BlockType.OAK_LOG, 1);
            }
        }
    }

    private void advanceSourceTaiga2Tree(SourceTreeScratch scratch, Random random, int x, int y, int z) {
        int height = random.nextInt(4) + 6;
        int topOffset = 1 + random.nextInt(2);
        int maxRadius = 2 + random.nextInt(2);
        if (!canPlaceSourceTaigaTree(scratch, x, y, z, height, topOffset, maxRadius)) {
            return;
        }
        scratch.setBlock(x, y - 1, z, BlockType.DIRT);
        int radius = random.nextInt(2);
        int lowerRadius = 1;
        int resetRadius = 0;
        for (int dy = 0; dy <= height - topOffset; dy++) {
            int leafY = y + height - dy;
            for (int leafX = x - radius; leafX <= x + radius; leafX++) {
                int dx = leafX - x;
                for (int leafZ = z - radius; leafZ <= z + radius; leafZ++) {
                    int dz = leafZ - z;
                    boolean corner = Math.abs(dx) == radius && Math.abs(dz) == radius && radius > 0;
                    if (!corner && TreeFeature.isReplaceableForLeaves(scratch.getBlock(leafX, leafY, leafZ))) {
                        scratch.setBlock(leafX, leafY, leafZ, BlockType.LEAVES, 1);
                    }
                }
            }
            if (radius >= lowerRadius) {
                radius = resetRadius;
                resetRadius = 1;
                if (++lowerRadius > maxRadius) {
                    lowerRadius = maxRadius;
                }
            } else {
                radius++;
            }
        }
        int trunkShorten = random.nextInt(3);
        for (int dy = 0; dy < height - trunkShorten; dy++) {
            if (isSourceNormalTreeTrunkReplaceable(scratch.getBlock(x, y + dy, z))) {
                scratch.setBlock(x, y + dy, z, BlockType.OAK_LOG, 1);
            }
        }
    }

    private boolean canPlaceSourceNormalTree(SourceTreeScratch scratch, int x, int y, int z,
            int height, int topRadius) {
        if (y < 1 || y + height + 1 > Chunk.HEIGHT) {
            return false;
        }
        for (int checkY = y; checkY <= y + height + 1; checkY++) {
            int radius = 1;
            if (checkY == y) {
                radius = 0;
            }
            if (checkY >= y + height - 1) {
                radius = topRadius;
            }
            if (!isSourceTreeVolumeClear(scratch, x, z, checkY, radius, false, y)) {
                return false;
            }
        }
        BlockType support = scratch.getBlock(x, y - 1, z);
        return (support == BlockType.GRASS || support == BlockType.DIRT) && y < Chunk.HEIGHT - height - 1;
    }

    private boolean canPlaceSourceSwampTree(SourceTreeScratch scratch, int x, int y, int z, int height) {
        if (y < 1 || y + height + 1 > Chunk.HEIGHT) {
            return false;
        }
        for (int checkY = y; checkY <= y + height + 1; checkY++) {
            int radius = checkY == y ? 0 : 1;
            if (checkY >= y + height - 1) {
                radius = 3;
            }
            if (!isSourceTreeVolumeClear(scratch, x, z, checkY, radius, true, y)) {
                return false;
            }
        }
        BlockType support = scratch.getBlock(x, y - 1, z);
        return (support == BlockType.GRASS || support == BlockType.DIRT) && y < Chunk.HEIGHT - height - 1;
    }

    private boolean canPlaceSourceTaigaTree(SourceTreeScratch scratch, int x, int y, int z,
            int height, int clearStart, int maxRadius) {
        if (y < 1 || y + height + 1 > Chunk.HEIGHT) {
            return false;
        }
        for (int checkY = y; checkY <= y + height + 1; checkY++) {
            int radius = checkY - y < clearStart ? 0 : maxRadius;
            if (!isSourceTreeVolumeClear(scratch, x, z, checkY, radius, false, y)) {
                return false;
            }
        }
        BlockType support = scratch.getBlock(x, y - 1, z);
        return (support == BlockType.GRASS || support == BlockType.DIRT) && y < Chunk.HEIGHT - height - 1;
    }

    private boolean isSourceTreeVolumeClear(SourceTreeScratch scratch, int centerX, int centerZ,
            int y, int radius, boolean swamp, int baseY) {
        for (int x = centerX - radius; x <= centerX + radius; x++) {
            for (int z = centerZ - radius; z <= centerZ + radius; z++) {
                if (y < 0 || y >= Chunk.HEIGHT) {
                    return false;
                }
                BlockType block = scratch.getBlock(x, y, z);
                if (block == BlockType.AIR || block == BlockType.LEAVES) {
                    continue;
                }
                if (swamp && block.isWater() && y <= baseY) {
                    continue;
                }
                return false;
            }
        }
        return true;
    }

    private void placeSourceNormalLeaves(SourceTreeScratch scratch, Random random,
            int x, int y, int z, int height, int metadata, int baseRadius) {
        for (int layerY = -3; layerY <= 0; layerY++) {
            int radius = baseRadius - layerY / 2;
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (Math.abs(dx) == radius && Math.abs(dz) == radius) {
                        if (random.nextInt(2) == 0 || layerY == 0) {
                            continue;
                        }
                    }
                    int leafX = x + dx;
                    int leafY = y + height + layerY;
                    int leafZ = z + dz;
                    if (TreeFeature.isReplaceableForLeaves(scratch.getBlock(leafX, leafY, leafZ))) {
                        scratch.setBlock(leafX, leafY, leafZ, BlockType.LEAVES, metadata);
                    }
                }
            }
        }
    }

    private void placeSourceSwampLeavesAndVines(SourceTreeScratch scratch, Random random,
            int x, int y, int z, int height) {
        for (int layerY = -3; layerY <= 0; layerY++) {
            int radius = 2 - layerY / 2;
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    boolean corner = Math.abs(dx) == radius && Math.abs(dz) == radius;
                    if (corner && (random.nextInt(2) == 0 || layerY == 0)) {
                        continue;
                    }
                    int leafX = x + dx;
                    int leafY = y + height + layerY;
                    int leafZ = z + dz;
                    if (TreeFeature.isReplaceableForLeaves(scratch.getBlock(leafX, leafY, leafZ))) {
                        scratch.setBlock(leafX, leafY, leafZ, BlockType.LEAVES, 0);
                    }
                }
            }
        }
        for (int leafY = y + height - 3; leafY <= y + height; leafY++) {
            int layerY = leafY - (y + height);
            int radius = 2 - layerY / 2;
            for (int leafX = x - radius; leafX <= x + radius; leafX++) {
                for (int leafZ = z - radius; leafZ <= z + radius; leafZ++) {
                    if (scratch.getBlock(leafX, leafY, leafZ) != BlockType.LEAVES) {
                        continue;
                    }
                    if (random.nextInt(4) == 0 && scratch.getBlock(leafX - 1, leafY, leafZ) == BlockType.AIR) {
                        placeSourceVines(scratch, leafX - 1, leafY, leafZ, 8);
                    }
                    if (random.nextInt(4) == 0 && scratch.getBlock(leafX + 1, leafY, leafZ) == BlockType.AIR) {
                        placeSourceVines(scratch, leafX + 1, leafY, leafZ, 2);
                    }
                    if (random.nextInt(4) == 0 && scratch.getBlock(leafX, leafY, leafZ - 1) == BlockType.AIR) {
                        placeSourceVines(scratch, leafX, leafY, leafZ - 1, 1);
                    }
                    if (random.nextInt(4) == 0 && scratch.getBlock(leafX, leafY, leafZ + 1) == BlockType.AIR) {
                        placeSourceVines(scratch, leafX, leafY, leafZ + 1, 4);
                    }
                }
            }
        }
    }

    private void placeSourceVines(SourceTreeScratch scratch, int x, int y, int z, int metadata) {
        for (int remaining = 5; remaining > 0 && scratch.getBlock(x, y, z) == BlockType.AIR; remaining--, y--) {
            scratch.setBlock(x, y, z, BlockType.VINES, metadata);
        }
    }

    private boolean isSourceNormalTreeTrunkReplaceable(BlockType block) {
        return block == BlockType.AIR || block == BlockType.LEAVES;
    }

    private boolean isSourceSwampTreeTrunkReplaceable(BlockType block) {
        return block == BlockType.AIR || block == BlockType.LEAVES || block.isWater();
    }

    private void placeHugeMushrooms(Chunk chunk, int chunkX, int chunkZ, DecoratorOrigin[] origins) {
        placeHugeMushrooms(chunk, chunkX, chunkZ, origins, null);
    }

    private void placeHugeMushrooms(Chunk chunk, int chunkX, int chunkZ, DecoratorOrigin[] origins,
            SourceTreeScratch scratch) {
        for (DecoratorOrigin origin : origins) {
            placeHugeMushroomFromOrigin(chunk, chunkX, chunkZ, origin.chunkX(), origin.chunkZ(), origin.random(),
                    scratch);
        }
    }

    private void placeHugeMushroomFromOrigin(Chunk chunk, int chunkX, int chunkZ,
            int originChunkX, int originChunkZ, Random random) {
        placeHugeMushroomFromOrigin(chunk, chunkX, chunkZ, originChunkX, originChunkZ, random, null);
    }

    private void placeHugeMushroomFromOrigin(Chunk chunk, int chunkX, int chunkZ,
            int originChunkX, int originChunkZ, Random random, SourceTreeScratch scratch) {
        int originX = originChunkX * Chunk.WIDTH;
        int originZ = originChunkZ * Chunk.DEPTH;
        BiomeType biome = getBiome(originX + 16, originZ + 16);
        if (biome == BiomeType.MUSHROOM_ISLAND || biome == BiomeType.MUSHROOM_ISLAND_SHORE) {
            placeHugeMushroom(chunk, chunkX, chunkZ, random,
                    originX + random.nextInt(Chunk.WIDTH) + 8,
                    originZ + random.nextInt(Chunk.DEPTH) + 8,
                    scratch);
        }
    }

    private Random overworldDecoratorRandom(World world, int chunkX, int chunkZ) {
        Random random = overworldOreRandom(world, chunkX, chunkZ);
        oreGenerator.advanceFromOrigin(random, chunkX, chunkZ);
        return random;
    }

    private void placeCurrentOverworldDisks(Chunk chunk, int chunkX, int chunkZ, Random random) {
        placeCurrentOverworldDisks(chunk, chunkX, chunkZ, random, chunkX, chunkZ);
    }

    private void placeCurrentOverworldDisks(Chunk chunk, int chunkX, int chunkZ, Random random,
            int originChunkX, int originChunkZ) {
        placeCurrentOverworldDisks(chunk, chunkX, chunkZ, random, originChunkX, originChunkZ, null);
    }

    private void placeCurrentOverworldDisks(Chunk chunk, int chunkX, int chunkZ, Random random,
            int originChunkX, int originChunkZ, SourceTreeScratch scratch) {
        for (int i = 0; i < 3; i++) {
            placeUnderwaterDisk(chunk, chunkX, chunkZ, random, originChunkX, originChunkZ,
                    BlockType.SAND, 7, 2, scratch);
        }
        placeUnderwaterDisk(chunk, chunkX, chunkZ, random, originChunkX, originChunkZ,
                BlockType.CLAY, 4, 1, scratch);
        placeUnderwaterDisk(chunk, chunkX, chunkZ, random, originChunkX, originChunkZ,
                BlockType.SAND, 7, 2, scratch);
    }

    private void placeUnderwaterDisk(Chunk chunk, int chunkX, int chunkZ, Random random,
            int originChunkX, int originChunkZ, BlockType replacement, int radiusBound, int verticalRadius) {
        placeUnderwaterDisk(chunk, chunkX, chunkZ, random, originChunkX, originChunkZ,
                replacement, radiusBound, verticalRadius, null);
    }

    private void placeUnderwaterDisk(Chunk chunk, int chunkX, int chunkZ, Random random,
            int originChunkX, int originChunkZ, BlockType replacement, int radiusBound, int verticalRadius,
            SourceTreeScratch scratch) {
        int centerX = originChunkX * Chunk.WIDTH + random.nextInt(16) + 8;
        int centerZ = originChunkZ * Chunk.DEPTH + random.nextInt(16) + 8;
        int centerY = underwaterDiskStartY(chunk, chunkX, chunkZ, centerX, centerZ, scratch);
        if (centerY < 0) {
            return;
        }
        int radius = random.nextInt(radiusBound - 2) + 2;
        int radiusSquared = radius * radius;
        for (int x = centerX - radius; x <= centerX + radius; x++) {
            for (int z = centerZ - radius; z <= centerZ + radius; z++) {
                int dx = x - centerX;
                int dz = z - centerZ;
                if (dx * dx + dz * dz > radiusSquared) {
                    continue;
                }
                for (int y = centerY - verticalRadius; y <= centerY + verticalRadius; y++) {
                    if (y < 0 || y >= Chunk.HEIGHT) {
                        continue;
                    }
                    BlockType current = blockAtDecoratedOrBase(chunk, chunkX, chunkZ, x, y, z, scratch);
                    if (replacement == BlockType.CLAY) {
                        if (current == BlockType.DIRT || current == BlockType.CLAY) {
                            setDecoratorBlock(chunk, chunkX, chunkZ, scratch, x, y, z, BlockType.CLAY);
                        }
                    } else if (current == BlockType.DIRT || current == BlockType.GRASS) {
                        setDecoratorBlock(chunk, chunkX, chunkZ, scratch, x, y, z, replacement);
                    }
                }
            }
        }
    }

    private int underwaterDiskStartY(Chunk chunk, int chunkX, int chunkZ, int blockX, int blockZ) {
        return underwaterDiskStartY(chunk, chunkX, chunkZ, blockX, blockZ, null);
    }

    private int underwaterDiskStartY(Chunk chunk, int chunkX, int chunkZ, int blockX, int blockZ,
            SourceTreeScratch scratch) {
        for (int y = Chunk.HEIGHT - 1; y > 0; y--) {
            BlockType block = blockAtDecoratedOrBase(chunk, chunkX, chunkZ, blockX, y, blockZ, scratch);
            if (block == BlockType.AIR || block.isWater() || block == BlockType.LEAVES) {
                continue;
            }
            int waterY = y + 1;
            if (waterY < Chunk.HEIGHT
                    && blockAtDecoratedOrBase(chunk, chunkX, chunkZ, blockX, waterY, blockZ, scratch).isWater()) {
                return waterY;
            }
            return -1;
        }
        return -1;
    }

    private void placeHugeMushroom(Chunk chunk, int chunkX, int chunkZ, Random random, int blockX, int blockZ) {
        placeHugeMushroom(chunk, chunkX, chunkZ, random, blockX, blockZ, null);
    }

    private void placeHugeMushroom(Chunk chunk, int chunkX, int chunkZ, Random random, int blockX, int blockZ,
            SourceTreeScratch scratch) {
        int y = sourceHeightValue(chunk, chunkX, chunkZ, blockX, blockZ, scratch);
        int typeIndex = random.nextInt(2);
        BlockType type = typeIndex == 0 ? BlockType.BROWN_MUSHROOM_BLOCK : BlockType.RED_MUSHROOM_BLOCK;
        int height = random.nextInt(3) + 4;
        if (y < 1 || y + height + 1 > Chunk.HEIGHT
                || !canGeneratedHugeMushroomStart(chunk, chunkX, chunkZ, blockX, y, blockZ, scratch)) {
            return;
        }
        for (int checkY = y; checkY <= y + height + 1; checkY++) {
            int radius = checkY == y ? 0 : 3;
            for (int checkX = blockX - radius; checkX <= blockX + radius; checkX++) {
                for (int checkZ = blockZ - radius; checkZ <= blockZ + radius; checkZ++) {
                    BlockType block = blockAtDecoratedOrBase(chunk, chunkX, chunkZ, checkX, checkY, checkZ,
                            scratch);
                    if (checkY < 0 || checkY >= Chunk.HEIGHT
                            || (block != BlockType.AIR && block != BlockType.LEAVES)) {
                        return;
                    }
                }
            }
        }

        setDecoratorBlock(chunk, chunkX, chunkZ, scratch, blockX, y - 1, blockZ, BlockType.DIRT);

        int capStartY = typeIndex == 1 ? y + height - 3 : y + height;
        for (int capY = capStartY; capY <= y + height; capY++) {
            int radius = capY < y + height ? 2 : 1;
            if (typeIndex == 0) {
                radius = 3;
            }

            for (int capX = blockX - radius; capX <= blockX + radius; capX++) {
                for (int capZ = blockZ - radius; capZ <= blockZ + radius; capZ++) {
                    int metadata = hugeMushroomCapMetadata(blockX, blockZ, capX, capZ, radius);
                    if (typeIndex == 0 || capY < y + height) {
                        if ((capX == blockX - radius || capX == blockX + radius)
                                && (capZ == blockZ - radius || capZ == blockZ + radius)) {
                            continue;
                        }
                        metadata = hugeMushroomEdgeMetadata(blockX, blockZ, capX, capZ, radius, metadata);
                    }
                    if (metadata == 5 && capY < y + height) {
                        metadata = 0;
                    }
                    if (metadata == 0 && capY < y + height - 1) {
                        continue;
                    }
                    BlockType existing = blockAtDecoratedOrBase(chunk, chunkX, chunkZ, capX, capY, capZ, scratch);
                    if (isSourceHugeMushroomOpaque(existing)) {
                        continue;
                    }
                    setDecoratorBlock(chunk, chunkX, chunkZ, scratch, capX, capY, capZ, type, metadata);
                }
            }
        }

        for (int dy = 0; dy < height; dy++) {
            int trunkY = y + dy;
            BlockType existing = blockAtDecoratedOrBase(chunk, chunkX, chunkZ, blockX, trunkY, blockZ, scratch);
            if (!isSourceHugeMushroomOpaque(existing)) {
                setDecoratorBlock(chunk, chunkX, chunkZ, scratch, blockX, trunkY, blockZ, type, 10);
            }
        }
    }

    private static boolean isSourceHugeMushroomOpaque(BlockType block) {
        return block != BlockType.LEAVES && BlockShape.isOpaqueCube(block);
    }

    private boolean canGeneratedHugeMushroomStart(Chunk chunk, int chunkX, int chunkZ,
            int blockX, int y, int blockZ, SourceTreeScratch scratch) {
        BlockType support = blockAtDecoratedOrBase(chunk, chunkX, chunkZ, blockX, y - 1, blockZ, scratch);
        if (support != BlockType.DIRT && support != BlockType.GRASS && support != BlockType.MYCELIUM) {
            return false;
        }
        return canGeneratedPlantStay(chunk, chunkX, chunkZ, blockX, y, blockZ, BlockType.BROWN_MUSHROOM, scratch);
    }

    private static int hugeMushroomCapMetadata(int centerX, int centerZ, int x, int z, int radius) {
        int metadata = 5;
        if (x == centerX - radius) {
            metadata--;
        }
        if (x == centerX + radius) {
            metadata++;
        }
        if (z == centerZ - radius) {
            metadata -= 3;
        }
        if (z == centerZ + radius) {
            metadata += 3;
        }
        return metadata;
    }

    private static int hugeMushroomEdgeMetadata(int centerX, int centerZ, int x, int z, int radius, int metadata) {
        if (x == centerX - (radius - 1) && z == centerZ - radius) {
            metadata = 1;
        }
        if (x == centerX - radius && z == centerZ - (radius - 1)) {
            metadata = 1;
        }
        if (x == centerX + (radius - 1) && z == centerZ - radius) {
            metadata = 3;
        }
        if (x == centerX + radius && z == centerZ - (radius - 1)) {
            metadata = 3;
        }
        if (x == centerX - (radius - 1) && z == centerZ + radius) {
            metadata = 7;
        }
        if (x == centerX - radius && z == centerZ + (radius - 1)) {
            metadata = 7;
        }
        if (x == centerX + (radius - 1) && z == centerZ + radius) {
            metadata = 9;
        }
        if (x == centerX + radius && z == centerZ + (radius - 1)) {
            metadata = 9;
        }
        return metadata;
    }

    private void placeFlowerScatter(Chunk chunk, int chunkX, int chunkZ, Random random,
            int originX, int originZ, BlockType type) {
        placeFlowerScatter(chunk, chunkX, chunkZ, random, originX, originZ, type, null);
    }

    private void placeFlowerScatter(Chunk chunk, int chunkX, int chunkZ, Random random,
            int originX, int originZ, BlockType type, SourceTreeScratch scratch) {
        placeFlowerScatter(chunk, chunkX, chunkZ, random, originX, originZ, type, scratch, null);
    }

    private void placeFlowerScatter(Chunk chunk, int chunkX, int chunkZ, Random random,
            int originX, int originZ, BlockType type, SourceTreeScratch scratch, GeneratedBlockLight blockLight) {
        placeFlowerScatter(chunk, chunkX, chunkZ, random,
                originX + random.nextInt(Chunk.WIDTH) + 8,
                random.nextInt(128),
                originZ + random.nextInt(Chunk.DEPTH) + 8,
                type,
                scratch,
                blockLight);
    }

    private void placeFlowerScatter(Chunk chunk, int chunkX, int chunkZ, Random random,
            int startX, int startY, int startZ, BlockType type) {
        placeFlowerScatter(chunk, chunkX, chunkZ, random, startX, startY, startZ, type, null);
    }

    private void placeFlowerScatter(Chunk chunk, int chunkX, int chunkZ, Random random,
            int startX, int startY, int startZ, BlockType type, SourceTreeScratch scratch) {
        placeFlowerScatter(chunk, chunkX, chunkZ, random, startX, startY, startZ, type, scratch, null);
    }

    private void placeFlowerScatter(Chunk chunk, int chunkX, int chunkZ, Random random,
            int startX, int startY, int startZ, BlockType type, SourceTreeScratch scratch,
            GeneratedBlockLight blockLight) {
        for (int i = 0; i < 64; i++) {
            int x = startX + random.nextInt(8) - random.nextInt(8);
            int y = startY + random.nextInt(4) - random.nextInt(4);
            int z = startZ + random.nextInt(8) - random.nextInt(8);
            placePlantIfValid(chunk, chunkX, chunkZ, x, y, z, type, 0, scratch, blockLight);
        }
    }

    private void placeTallGrassScatter(Chunk chunk, int chunkX, int chunkZ, Random random,
            int originX, int originZ, int metadata) {
        placeTallGrassScatter(chunk, chunkX, chunkZ, random, originX, originZ, metadata, null);
    }

    private void placeTallGrassScatter(Chunk chunk, int chunkX, int chunkZ, Random random,
            int originX, int originZ, int metadata, SourceTreeScratch scratch) {
        placeTallGrassScatter(chunk, chunkX, chunkZ, random, originX, originZ, metadata, scratch, null);
    }

    private void placeTallGrassScatter(Chunk chunk, int chunkX, int chunkZ, Random random,
            int originX, int originZ, int metadata, SourceTreeScratch scratch, GeneratedBlockLight blockLight) {
        int startX = originX + random.nextInt(Chunk.WIDTH) + 8;
        int startY = random.nextInt(128);
        int startZ = originZ + random.nextInt(Chunk.DEPTH) + 8;
        while (startY > 0) {
            BlockType block = blockAtDecoratedOrBase(chunk, chunkX, chunkZ, startX, startY, startZ, scratch);
            if (block != BlockType.AIR && block != BlockType.LEAVES) {
                break;
            }
            startY--;
        }
        for (int i = 0; i < 128; i++) {
            int x = startX + random.nextInt(8) - random.nextInt(8);
            int y = startY + random.nextInt(4) - random.nextInt(4);
            int z = startZ + random.nextInt(8) - random.nextInt(8);
            placePlantIfValid(chunk, chunkX, chunkZ, x, y, z, BlockType.TALL_GRASS, metadata, scratch, blockLight);
        }
    }

    private void placeDeadBushScatter(Chunk chunk, int chunkX, int chunkZ, Random random,
            int originX, int originZ) {
        placeDeadBushScatter(chunk, chunkX, chunkZ, random, originX, originZ, null);
    }

    private void placeDeadBushScatter(Chunk chunk, int chunkX, int chunkZ, Random random,
            int originX, int originZ, SourceTreeScratch scratch) {
        placeDeadBushScatter(chunk, chunkX, chunkZ, random, originX, originZ, scratch, null);
    }

    private void placeDeadBushScatter(Chunk chunk, int chunkX, int chunkZ, Random random,
            int originX, int originZ, SourceTreeScratch scratch, GeneratedBlockLight blockLight) {
        int startX = originX + random.nextInt(Chunk.WIDTH) + 8;
        int startY = random.nextInt(128);
        int startZ = originZ + random.nextInt(Chunk.DEPTH) + 8;
        while (startY > 0) {
            BlockType block = blockAtDecoratedOrBase(chunk, chunkX, chunkZ, startX, startY, startZ, scratch);
            if (block != BlockType.AIR && block != BlockType.LEAVES) {
                break;
            }
            startY--;
        }
        for (int i = 0; i < 4; i++) {
            int x = startX + random.nextInt(8) - random.nextInt(8);
            int y = startY + random.nextInt(4) - random.nextInt(4);
            int z = startZ + random.nextInt(8) - random.nextInt(8);
            placePlantIfValid(chunk, chunkX, chunkZ, x, y, z, BlockType.DEAD_BUSH, 0, scratch, blockLight);
        }
    }

    private void placeWaterLilyScatter(Chunk chunk, int chunkX, int chunkZ, Random random,
            int originX, int originZ) {
        placeWaterLilyScatter(chunk, chunkX, chunkZ, random, originX, originZ, null);
    }

    private void placeWaterLilyScatter(Chunk chunk, int chunkX, int chunkZ, Random random,
            int originX, int originZ, SourceTreeScratch scratch) {
        int startX = originX + random.nextInt(Chunk.WIDTH) + 8;
        int startZ = originZ + random.nextInt(Chunk.DEPTH) + 8;
        int startY = random.nextInt(128);
        while (startY > 0
                && blockAtDecoratedOrBase(chunk, chunkX, chunkZ, startX, startY - 1, startZ, scratch) == BlockType.AIR) {
            startY--;
        }
        for (int i = 0; i < 10; i++) {
            int x = startX + random.nextInt(8) - random.nextInt(8);
            int y = startY + random.nextInt(4) - random.nextInt(4);
            int z = startZ + random.nextInt(8) - random.nextInt(8);
            placePlantIfValid(chunk, chunkX, chunkZ, x, y, z, BlockType.LILY_PAD, 0, scratch);
        }
    }

    private void placeMushroomPair(Chunk chunk, int chunkX, int chunkZ, Random random,
            int originX, int originZ) {
        placeMushroomPair(chunk, chunkX, chunkZ, random, originX, originZ, null);
    }

    private void placeMushroomPair(Chunk chunk, int chunkX, int chunkZ, Random random,
            int originX, int originZ, SourceTreeScratch scratch) {
        placeMushroomPair(chunk, chunkX, chunkZ, random, originX, originZ, scratch, null);
    }

    private void placeMushroomPair(Chunk chunk, int chunkX, int chunkZ, Random random,
            int originX, int originZ, SourceTreeScratch scratch, GeneratedBlockLight blockLight) {
        if (random.nextInt(4) == 0) {
            int x = originX + random.nextInt(Chunk.WIDTH) + 8;
            int z = originZ + random.nextInt(Chunk.DEPTH) + 8;
            placeFlowerScatter(chunk, chunkX, chunkZ, random, x,
                    sourceHeightValue(chunk, chunkX, chunkZ, x, z, scratch), z, BlockType.BROWN_MUSHROOM,
                    scratch, blockLight);
        }
        if (random.nextInt(8) == 0) {
            int x = originX + random.nextInt(Chunk.WIDTH) + 8;
            int z = originZ + random.nextInt(Chunk.DEPTH) + 8;
            int y = random.nextInt(128);
            placeFlowerScatter(chunk, chunkX, chunkZ, random, x, y, z, BlockType.RED_MUSHROOM, scratch, blockLight);
        }
    }

    private void placeReedScatter(Chunk chunk, int chunkX, int chunkZ, Random random,
            int originX, int originZ) {
        placeReedScatter(chunk, chunkX, chunkZ, random, originX, originZ, null);
    }

    private void placeReedScatter(Chunk chunk, int chunkX, int chunkZ, Random random,
            int originX, int originZ, SourceTreeScratch scratch) {
        int startX = originX + random.nextInt(Chunk.WIDTH) + 8;
        int startY = random.nextInt(128);
        int startZ = originZ + random.nextInt(Chunk.DEPTH) + 8;
        placeReedScatterAt(chunk, chunkX, chunkZ, random, startX, startY, startZ, scratch);
    }

    private void placeBiomeReedScatter(Chunk chunk, int chunkX, int chunkZ, Random random,
            int originX, int originZ) {
        placeBiomeReedScatter(chunk, chunkX, chunkZ, random, originX, originZ, null);
    }

    private void placeBiomeReedScatter(Chunk chunk, int chunkX, int chunkZ, Random random,
            int originX, int originZ, SourceTreeScratch scratch) {
        int startX = originX + random.nextInt(Chunk.WIDTH) + 8;
        int startZ = originZ + random.nextInt(Chunk.DEPTH) + 8;
        int startY = random.nextInt(128);
        placeReedScatterAt(chunk, chunkX, chunkZ, random, startX, startY, startZ, scratch);
    }

    private void placeReedScatterAt(Chunk chunk, int chunkX, int chunkZ, Random random,
            int startX, int startY, int startZ) {
        placeReedScatterAt(chunk, chunkX, chunkZ, random, startX, startY, startZ, null);
    }

    private void placeReedScatterAt(Chunk chunk, int chunkX, int chunkZ, Random random,
            int startX, int startY, int startZ, SourceTreeScratch scratch) {
        for (int i = 0; i < 20; i++) {
            int x = startX + random.nextInt(4) - random.nextInt(4);
            int z = startZ + random.nextInt(4) - random.nextInt(4);
            if (blockAtDecoratedOrBase(chunk, chunkX, chunkZ, x, startY, z, scratch) != BlockType.AIR
                    || !hasAdjacentWaterAtBase(chunk, chunkX, chunkZ, x, startY - 1, z, scratch)) {
                continue;
            }
            int height = 2 + random.nextInt(random.nextInt(3) + 1);
            for (int dy = 0; dy < height; dy++) {
                int y = startY + dy;
                if (!canGeneratedPlantStay(chunk, chunkX, chunkZ, x, y, z, BlockType.SUGAR_CANE, scratch)) {
                    continue;
                }
                setDecoratorBlock(chunk, chunkX, chunkZ, scratch, x, y, z, BlockType.SUGAR_CANE);
            }
        }
    }

    private void placePumpkinScatter(Chunk chunk, int chunkX, int chunkZ, Random random,
            int originX, int originZ) {
        placePumpkinScatter(chunk, chunkX, chunkZ, random, originX, originZ, null);
    }

    private void placePumpkinScatter(Chunk chunk, int chunkX, int chunkZ, Random random,
            int originX, int originZ, SourceTreeScratch scratch) {
        int startX = originX + random.nextInt(Chunk.WIDTH) + 8;
        int startY = random.nextInt(128);
        int startZ = originZ + random.nextInt(Chunk.DEPTH) + 8;
        for (int i = 0; i < 64; i++) {
            int x = startX + random.nextInt(8) - random.nextInt(8);
            int y = startY + random.nextInt(4) - random.nextInt(4);
            int z = startZ + random.nextInt(8) - random.nextInt(8);
            if (canGeneratedPlantStay(chunk, chunkX, chunkZ, x, y, z, BlockType.PUMPKIN, scratch)) {
                setDecoratorBlock(chunk, chunkX, chunkZ, scratch, x, y, z, BlockType.PUMPKIN, random.nextInt(4));
            }
        }
    }

    private void placeCactusScatter(Chunk chunk, int chunkX, int chunkZ, Random random,
            int originX, int originZ) {
        placeCactusScatter(chunk, chunkX, chunkZ, random, originX, originZ, null);
    }

    private void placeCactusScatter(Chunk chunk, int chunkX, int chunkZ, Random random,
            int originX, int originZ, SourceTreeScratch scratch) {
        int startX = originX + random.nextInt(Chunk.WIDTH) + 8;
        int startY = random.nextInt(128);
        int startZ = originZ + random.nextInt(Chunk.DEPTH) + 8;
        for (int i = 0; i < 10; i++) {
            int x = startX + random.nextInt(8) - random.nextInt(8);
            int y = startY + random.nextInt(4) - random.nextInt(4);
            int z = startZ + random.nextInt(8) - random.nextInt(8);
            if (blockAtDecoratedOrBase(chunk, chunkX, chunkZ, x, y, z, scratch) != BlockType.AIR) {
                continue;
            }
            int height = 1 + random.nextInt(random.nextInt(3) + 1);
            for (int dy = 0; dy < height; dy++) {
                if (canGeneratedPlantStay(chunk, chunkX, chunkZ, x, y + dy, z, BlockType.CACTUS, scratch)) {
                    setDecoratorBlock(chunk, chunkX, chunkZ, scratch, x, y + dy, z, BlockType.CACTUS);
                }
            }
        }
    }

    private void placePlantIfValid(Chunk chunk, int chunkX, int chunkZ, int blockX, int y, int blockZ,
            BlockType type, int metadata) {
        placePlantIfValid(chunk, chunkX, chunkZ, blockX, y, blockZ, type, metadata, null);
    }

    private void placePlantIfValid(Chunk chunk, int chunkX, int chunkZ, int blockX, int y, int blockZ,
            BlockType type, int metadata, SourceTreeScratch scratch) {
        placePlantIfValid(chunk, chunkX, chunkZ, blockX, y, blockZ, type, metadata, scratch, null);
    }

    private void placePlantIfValid(Chunk chunk, int chunkX, int chunkZ, int blockX, int y, int blockZ,
            BlockType type, int metadata, SourceTreeScratch scratch, GeneratedBlockLight blockLight) {
        if (canGeneratedPlantStay(chunk, chunkX, chunkZ, blockX, y, blockZ, type, scratch, blockLight)) {
            setDecoratorBlock(chunk, chunkX, chunkZ, scratch, blockX, y, blockZ, type, metadata);
        }
    }

    private boolean canGeneratedPlantStay(Chunk chunk, int chunkX, int chunkZ,
            int blockX, int y, int blockZ, BlockType type) {
        return canGeneratedPlantStay(chunk, chunkX, chunkZ, blockX, y, blockZ, type, null,
                usesGeneratedBlockLight(type) ? generatedWorldBlockLightSnapshot(chunk) : null);
    }

    private boolean canGeneratedPlantStay(Chunk chunk, int chunkX, int chunkZ,
            int blockX, int y, int blockZ, BlockType type, SourceTreeScratch scratch) {
        return canGeneratedPlantStay(chunk, chunkX, chunkZ, blockX, y, blockZ, type, scratch,
                usesGeneratedBlockLight(type) ? generatedWorldBlockLightSnapshot(chunk, scratch) : null);
    }

    private boolean canGeneratedPlantStay(Chunk chunk, int chunkX, int chunkZ,
            int blockX, int y, int blockZ, BlockType type, SourceTreeScratch scratch,
            GeneratedBlockLight blockLight) {
        if (y <= 0 || y >= Chunk.HEIGHT) {
            return false;
        }
        if (scratch == null && !containsBlock(chunkX, chunkZ, blockX, blockZ)) {
            return false;
        }
        if (blockAtDecoratedOrBase(chunk, chunkX, chunkZ, blockX, y, blockZ, scratch) != BlockType.AIR) {
            return false;
        }
        BlockType below = blockAtDecoratedOrBase(chunk, chunkX, chunkZ, blockX, y - 1, blockZ, scratch);
        if (type == BlockType.DEAD_BUSH) {
            return below == BlockType.SAND
                    && hasGeneratedFlowerLight(chunk, chunkX, chunkZ, blockX, y, blockZ, scratch, blockLight);
        }
        if (type == BlockType.BROWN_MUSHROOM || type == BlockType.RED_MUSHROOM) {
            return canGeneratedMushroomStay(chunk, chunkX, chunkZ, blockX, y, blockZ, below, scratch, blockLight);
        }
        if (type == BlockType.SUGAR_CANE) {
            if (below == BlockType.SUGAR_CANE) {
                return true;
            }
            return (below == BlockType.GRASS || below == BlockType.DIRT || below == BlockType.SAND)
                    && hasAdjacentWaterAtBase(chunk, chunkX, chunkZ, blockX, y - 1, blockZ, scratch);
        }
        if (type == BlockType.CACTUS) {
            return (below == BlockType.SAND || below == BlockType.CACTUS)
                    && !BlockShape.blocksCactusGrowth(
                            blockAtDecoratedOrBase(chunk, chunkX, chunkZ, blockX + 1, y, blockZ, scratch))
                    && !BlockShape.blocksCactusGrowth(
                            blockAtDecoratedOrBase(chunk, chunkX, chunkZ, blockX - 1, y, blockZ, scratch))
                    && !BlockShape.blocksCactusGrowth(
                            blockAtDecoratedOrBase(chunk, chunkX, chunkZ, blockX, y, blockZ + 1, scratch))
                    && !BlockShape.blocksCactusGrowth(
                            blockAtDecoratedOrBase(chunk, chunkX, chunkZ, blockX, y, blockZ - 1, scratch));
        }
        if (type == BlockType.LILY_PAD) {
            return below.isWater()
                    && metadataAtDecoratedOrBase(chunk, chunkX, chunkZ, blockX, y - 1, blockZ, scratch) == 0;
        }
        if (type == BlockType.PUMPKIN) {
            return below == BlockType.GRASS;
        }
        if (type == BlockType.TALL_GRASS || type == BlockType.YELLOW_FLOWER || type == BlockType.RED_ROSE) {
            return (below == BlockType.GRASS || below == BlockType.DIRT || below == BlockType.FARMLAND)
                    && hasGeneratedFlowerLight(chunk, chunkX, chunkZ, blockX, y, blockZ, scratch, blockLight);
        }
        return below == BlockType.GRASS || below == BlockType.DIRT;
    }

    private static boolean usesGeneratedBlockLight(BlockType type) {
        return type == BlockType.DEAD_BUSH
                || type == BlockType.BROWN_MUSHROOM
                || type == BlockType.RED_MUSHROOM
                || type == BlockType.TALL_GRASS
                || type == BlockType.YELLOW_FLOWER
                || type == BlockType.RED_ROSE;
    }

    private boolean hasGeneratedFlowerLight(Chunk chunk, int chunkX, int chunkZ,
            int blockX, int y, int blockZ, SourceTreeScratch scratch, GeneratedBlockLight blockLight) {
        if (sourceCanBlockSeeTheSky(chunk, chunkX, chunkZ, blockX, y, blockZ, scratch)) {
            return true;
        }
        return blockLightAt(blockLight, blockX, y, blockZ) >= 8;
    }

    private boolean canGeneratedMushroomStay(Chunk chunk, int chunkX, int chunkZ,
            int blockX, int y, int blockZ, BlockType support) {
        return canGeneratedMushroomStay(chunk, chunkX, chunkZ, blockX, y, blockZ, support, null, null);
    }

    private boolean canGeneratedMushroomStay(Chunk chunk, int chunkX, int chunkZ,
            int blockX, int y, int blockZ, BlockType support, SourceTreeScratch scratch) {
        return canGeneratedMushroomStay(chunk, chunkX, chunkZ, blockX, y, blockZ, support, scratch, null);
    }

    private boolean canGeneratedMushroomStay(Chunk chunk, int chunkX, int chunkZ,
            int blockX, int y, int blockZ, BlockType support, SourceTreeScratch scratch,
            GeneratedBlockLight blockLight) {
        if (support == BlockType.MYCELIUM) {
            return true;
        }
        if (!BlockShape.isOpaqueCube(support)) {
            return false;
        }
        return !sourceCanBlockSeeTheSky(chunk, chunkX, chunkZ, blockX, y, blockZ, scratch)
                && blockLightAt(blockLight, blockX, y, blockZ) < 13;
    }

    private int positiveDecoratorCount(int count) {
        return Math.max(0, count);
    }

    private int flowersPerChunk(BiomeType biome) {
        return switch (biome) {
            case PLAINS -> 4;
            case SWAMPLAND -> -999;
            case MUSHROOM_ISLAND, MUSHROOM_ISLAND_SHORE -> -100;
            default -> 2;
        };
    }

    private int grassPerChunk(BiomeType biome) {
        return switch (biome) {
            case PLAINS -> 10;
            case FOREST, FOREST_HILLS -> 2;
            case MUSHROOM_ISLAND, MUSHROOM_ISLAND_SHORE -> -100;
            default -> 1;
        };
    }

    private int tallGrassMetadataForBiome(BiomeType biome, Random random) {
        return 1;
    }

    private int deadBushPerChunk(BiomeType biome) {
        return switch (biome) {
            case DESERT, DESERT_HILLS -> 2;
            case SWAMPLAND -> 1;
            default -> 0;
        };
    }

    private int waterlilyPerChunk(BiomeType biome) {
        return biome == BiomeType.SWAMPLAND ? 4 : 0;
    }

    private int mushroomsPerChunk(BiomeType biome) {
        return switch (biome) {
            case MUSHROOM_ISLAND, MUSHROOM_ISLAND_SHORE -> 1;
            case SWAMPLAND -> 8;
            default -> 0;
        };
    }

    private int reedsPerChunk(BiomeType biome) {
        return switch (biome) {
            case DESERT, DESERT_HILLS -> 50;
            case SWAMPLAND -> 10;
            default -> 0;
        };
    }

    private int cactiPerChunk(BiomeType biome) {
        return biome == BiomeType.DESERT || biome == BiomeType.DESERT_HILLS ? 10 : 0;
    }

    private void placeOverworldSpring(Chunk chunk, int chunkX, int chunkZ, Random random,
            int originChunkX, int originChunkZ, BlockType fluid, boolean lava) {
        placeOverworldSpring(chunk, chunkX, chunkZ, random, originChunkX, originChunkZ, fluid, lava, null);
    }

    private void placeOverworldSpring(Chunk chunk, int chunkX, int chunkZ, Random random,
            int originChunkX, int originChunkZ, BlockType fluid, boolean lava, SourceTreeScratch scratch) {
        int x = originChunkX * Chunk.WIDTH + random.nextInt(Chunk.WIDTH) + 8;
        int y = lava
                ? random.nextInt(random.nextInt(random.nextInt(112) + 8) + 8)
                : random.nextInt(random.nextInt(120) + 8);
        int z = originChunkZ * Chunk.DEPTH + random.nextInt(Chunk.DEPTH) + 8;
        if (y <= 0 || y >= Chunk.HEIGHT - 1) {
            return;
        }

        if (scratch == null && !containsBlock(chunkX, chunkZ, x, z)) {
            return;
        }
        if (blockAtDecoratedOrBase(chunk, chunkX, chunkZ, x, y + 1, z, scratch) != BlockType.STONE
                || blockAtDecoratedOrBase(chunk, chunkX, chunkZ, x, y - 1, z, scratch) != BlockType.STONE) {
            return;
        }
        BlockType current = blockAtDecoratedOrBase(chunk, chunkX, chunkZ, x, y, z, scratch);
        if (current != BlockType.AIR && current != BlockType.STONE) {
            return;
        }

        int stoneSides = 0;
        int airSides = 0;
        int[][] dirs = { { 1, 0 }, { -1, 0 }, { 0, 1 }, { 0, -1 } };
        for (int[] dir : dirs) {
            BlockType side = blockAtDecoratedOrBase(chunk, chunkX, chunkZ, x + dir[0], y, z + dir[1], scratch);
            if (side == BlockType.STONE) {
                stoneSides++;
            }
            if (side == BlockType.AIR) {
                airSides++;
            }
        }

        if (stoneSides == 3 && airSides == 1) {
            boolean water = fluid.isWater();
            setDecoratorBlock(chunk, chunkX, chunkZ, scratch, x, y, z, BlockType.stillVariant(water));
            int flowMetadata = water ? 1 : 2;
            for (int[] dir : dirs) {
                int flowX = x + dir[0];
                int flowZ = z + dir[1];
                if (blockAtDecoratedOrBase(chunk, chunkX, chunkZ, flowX, y, flowZ, scratch) == BlockType.AIR) {
                    setDecoratorBlock(chunk, chunkX, chunkZ, scratch, flowX, y, flowZ, fluid, flowMetadata);
                    break;
                }
            }
        }
    }

    private void setDecoratorBlock(Chunk chunk, int chunkX, int chunkZ, SourceTreeScratch scratch,
            int blockX, int y, int blockZ, BlockType type) {
        setDecoratorBlock(chunk, chunkX, chunkZ, scratch, blockX, y, blockZ, type, 0);
    }

    private void setDecoratorBlock(Chunk chunk, int chunkX, int chunkZ, SourceTreeScratch scratch,
            int blockX, int y, int blockZ, BlockType type, int metadata) {
        if (scratch != null) {
            scratch.setBlock(blockX, y, blockZ, type, metadata);
        }
        setIfInChunk(chunk, chunkX, chunkZ, blockX, y, blockZ, type, metadata);
    }

    private boolean hasAdjacentWaterAtBase(Chunk chunk, int chunkX, int chunkZ, int blockX, int y, int blockZ) {
        return hasAdjacentWaterAtBase(chunk, chunkX, chunkZ, blockX, y, blockZ, null);
    }

    private boolean hasAdjacentWaterAtBase(Chunk chunk, int chunkX, int chunkZ, int blockX, int y, int blockZ,
            SourceTreeScratch scratch) {
        return blockAtDecoratedOrBase(chunk, chunkX, chunkZ, blockX + 1, y, blockZ, scratch).isWater()
                || blockAtDecoratedOrBase(chunk, chunkX, chunkZ, blockX - 1, y, blockZ, scratch).isWater()
                || blockAtDecoratedOrBase(chunk, chunkX, chunkZ, blockX, y, blockZ + 1, scratch).isWater()
                || blockAtDecoratedOrBase(chunk, chunkX, chunkZ, blockX, y, blockZ - 1, scratch).isWater();
    }

    private BlockType blockAtDecoratedOrBase(Chunk chunk, int chunkX, int chunkZ, int blockX, int y, int blockZ) {
        return blockAtDecoratedOrBase(chunk, chunkX, chunkZ, blockX, y, blockZ, null);
    }

    private BlockType blockAtDecoratedOrBase(Chunk chunk, int chunkX, int chunkZ, int blockX, int y, int blockZ,
            SourceTreeScratch scratch) {
        if (scratch != null) {
            return scratch.getBlock(blockX, y, blockZ);
        }
        if (containsBlock(chunkX, chunkZ, blockX, blockZ)) {
            return chunk.getBlock(blockX - chunkX * Chunk.WIDTH, y, blockZ - chunkZ * Chunk.DEPTH);
        }
        return baseBlockAt(blockX, y, blockZ);
    }

    private int metadataAtDecoratedOrBase(Chunk chunk, int chunkX, int chunkZ, int blockX, int y, int blockZ) {
        return metadataAtDecoratedOrBase(chunk, chunkX, chunkZ, blockX, y, blockZ, null);
    }

    private int metadataAtDecoratedOrBase(Chunk chunk, int chunkX, int chunkZ, int blockX, int y, int blockZ,
            SourceTreeScratch scratch) {
        if (scratch != null) {
            return scratch.getMetadata(blockX, y, blockZ);
        }
        if (containsBlock(chunkX, chunkZ, blockX, blockZ) && y >= 0 && y < Chunk.HEIGHT) {
            return chunk.getBlockMetadata(blockX - chunkX * Chunk.WIDTH, y, blockZ - chunkZ * Chunk.DEPTH);
        }
        return 0;
    }

    private int topSolidOrLiquidBlockY(Chunk chunk, int chunkX, int chunkZ, int blockX, int blockZ) {
        return topSolidOrLiquidBlockY(chunk, chunkX, chunkZ, blockX, blockZ, null);
    }

    private int topSolidOrLiquidBlockY(Chunk chunk, int chunkX, int chunkZ, int blockX, int blockZ,
            SourceTreeScratch scratch) {
        for (int y = Chunk.HEIGHT - 1; y >= 0; y--) {
            BlockType block = blockAtDecoratedOrBase(chunk, chunkX, chunkZ, blockX, y, blockZ, scratch);
            if (isSourcePrecipitationHeightBlock(block)) {
                return y;
            }
        }
        return -1;
    }

    private int sourceHeightValue(SourceTreeScratch scratch, int blockX, int blockZ) {
        return scratch.topHeightMapBlockY(blockX, blockZ) + 1;
    }

    private int sourceHeightValue(Chunk chunk, int chunkX, int chunkZ, int blockX, int blockZ,
            SourceTreeScratch scratch) {
        return topHeightMapBlockY(chunk, chunkX, chunkZ, blockX, blockZ, scratch) + 1;
    }

    private int topHeightMapBlockY(Chunk chunk, int chunkX, int chunkZ, int blockX, int blockZ,
            SourceTreeScratch scratch) {
        for (int y = Chunk.HEIGHT - 1; y >= 0; y--) {
            BlockType block = blockAtDecoratedOrBase(chunk, chunkX, chunkZ, blockX, y, blockZ, scratch);
            if (isSourceHeightMapBlock(block)) {
                return y;
            }
        }
        return -1;
    }

    private boolean sourceCanBlockSeeTheSky(Chunk chunk, int chunkX, int chunkZ,
            int blockX, int y, int blockZ, SourceTreeScratch scratch) {
        return topHeightMapBlockY(chunk, chunkX, chunkZ, blockX, blockZ, scratch) <= y;
    }

    private static boolean isSourceHeightMapBlock(BlockType block) {
        return sourceLightOpacity(block) > 0;
    }

    private static boolean isSourcePrecipitationHeightBlock(BlockType block) {
        return block.isSolid() || block.isFluid();
    }

    private static int sourceLightOpacity(BlockType block) {
        return switch (block) {
            case AIR, GLASS, MOB_SPAWNER, CHEST, TORCH, FIRE, PISTON_HEAD, MOVING_PISTON,
                    YELLOW_FLOWER, RED_ROSE, BROWN_MUSHROOM, RED_MUSHROOM, TALL_GRASS,
                    DEAD_BUSH, SAPLING, REDSTONE_WIRE, CROPS, STANDING_SIGN, LADDER, RAIL,
                    POWERED_RAIL, DETECTOR_RAIL, WALL_SIGN, LEVER, STONE_PRESSURE_PLATE,
                    WOODEN_PRESSURE_PLATE, REDSTONE_TORCH_OFF, REDSTONE_TORCH_ON,
                    STONE_BUTTON, SNOW_LAYER, SUGAR_CANE, PORTAL, CAKE, TRAPDOOR,
                    GLASS_PANE, IRON_BARS, PUMPKIN_STEM, MELON_STEM, VINES, FENCE_GATE,
                    LILY_PAD, ENCHANTING_TABLE, BREWING_STAND, CAULDRON, END_PORTAL,
                    END_PORTAL_FRAME, DRAGON_EGG -> 0;
            case LEAVES, COBWEB -> 1;
            case FLOWING_WATER, WATER, ICE -> 3;
            case FLOWING_LAVA, LAVA, FARMLAND, STONE_SLAB, OAK_STAIRS, COBBLESTONE_STAIRS,
                    BRICK_STAIRS, STONE_BRICK_STAIRS, NETHER_BRICK_STAIRS, SOUL_SAND,
                    GLOWSTONE -> 255;
            default -> BlockShape.isOpaqueCube(block) ? 255 : 0;
        };
    }

    private static int sourceLightStep(BlockType block) {
        int opacity = sourceLightOpacity(block);
        return opacity == 0 ? 1 : opacity;
    }

    private boolean isSourceFreezingTemperature(int blockX, int blockZ) {
        return biomeSource.getTemperature(blockX, blockZ) <= 0.15F;
    }

    private void placeSnowLayers(Chunk chunk, int chunkX, int chunkZ) {
        placeSnowLayers(chunk, chunkX, chunkZ, null);
    }

    private void placeSnowLayers(Chunk chunk, int chunkX, int chunkZ, SourceTreeScratch scratch) {
        GeneratedBlockLight blockLight = generatedWorldBlockLightSnapshot(chunk, scratch);
        int minOffset = scratch == null ? VISIBLE_POPULATION_MIN_CHUNK_OFFSET : SCRATCH_POPULATION_MIN_CHUNK_OFFSET;
        int maxOffset = scratch == null ? VISIBLE_POPULATION_MAX_CHUNK_OFFSET : SCRATCH_POPULATION_MAX_CHUNK_OFFSET;
        for (int originChunkX = chunkX + minOffset; originChunkX <= chunkX + maxOffset; originChunkX++) {
            for (int originChunkZ = chunkZ + minOffset; originChunkZ <= chunkZ + maxOffset; originChunkZ++) {
                placeSnowLayersFromOrigin(chunk, chunkX, chunkZ, originChunkX, originChunkZ, scratch, blockLight);
            }
        }
    }

    private void placeSnowLayersFromOrigin(Chunk chunk, int chunkX, int chunkZ, int originChunkX, int originChunkZ,
            SourceTreeScratch scratch, GeneratedBlockLight blockLight) {
        int originX = originChunkX * Chunk.WIDTH + 8;
        int originZ = originChunkZ * Chunk.DEPTH + 8;
        for (int dx = 0; dx < Chunk.WIDTH; dx++) {
            for (int dz = 0; dz < Chunk.DEPTH; dz++) {
                int blockX = originX + dx;
                int blockZ = originZ + dz;
                if (scratch == null && !containsBlock(chunkX, chunkZ, blockX, blockZ)) {
                    continue;
                }
                BiomeType biome = getBiome(blockX, blockZ);
                int height = topSolidOrLiquidBlockY(chunk, chunkX, chunkZ, blockX, blockZ, scratch) + 1;
                int freezeY = height - 1;
                if (canFreezeSurfaceWater(chunk, chunkX, chunkZ, blockX, freezeY, blockZ, biome, blockLight,
                        scratch)) {
                    setDecoratorBlock(chunk, chunkX, chunkZ, scratch, blockX, freezeY, blockZ, BlockType.ICE);
                    clearLilyPadUnsupportedByFinalFreeze(chunk, chunkX, chunkZ, scratch,
                            blockX, freezeY + 1, blockZ);
                }
                if (canPlaceFinalSnowLayer(chunk, chunkX, chunkZ, blockX, height, blockZ, biome, blockLight,
                        scratch)) {
                    setDecoratorBlock(chunk, chunkX, chunkZ, scratch, blockX, height, blockZ,
                            BlockType.SNOW_LAYER, 0);
                }
            }
        }
    }

    private void clearLilyPadUnsupportedByFinalFreeze(Chunk chunk, int chunkX, int chunkZ,
            SourceTreeScratch scratch, int blockX, int y, int blockZ) {
        if (y >= 0 && y < Chunk.HEIGHT
                && blockAtDecoratedOrBase(chunk, chunkX, chunkZ, blockX, y, blockZ, scratch) == BlockType.LILY_PAD) {
            setDecoratorBlock(chunk, chunkX, chunkZ, scratch, blockX, y, blockZ, BlockType.AIR, 0);
        }
    }

    private boolean canFreezeSurfaceWater(Chunk chunk, int chunkX, int chunkZ, int blockX, int y, int blockZ,
            BiomeType biome, GeneratedBlockLight blockLight, SourceTreeScratch scratch) {
        if (!isSourceFreezingTemperature(blockX, blockZ) || y < 0 || y >= Chunk.HEIGHT) {
            return false;
        }
        BlockType block = blockAtDecoratedOrBase(chunk, chunkX, chunkZ, blockX, y, blockZ, scratch);
        if (!block.isWater() || metadataAtDecoratedOrBase(chunk, chunkX, chunkZ, blockX, y, blockZ, scratch) != 0
                || blockLightAt(blockLight, blockX, y, blockZ) >= 10) {
            return false;
        }
        return true;
    }

    private boolean canPlaceFinalSnowLayer(Chunk chunk, int chunkX, int chunkZ, int blockX, int y, int blockZ,
            BiomeType biome, GeneratedBlockLight blockLight, SourceTreeScratch scratch) {
        if (!isSourceFreezingTemperature(blockX, blockZ) || y <= 0 || y >= Chunk.HEIGHT) {
            return false;
        }
        if (blockAtDecoratedOrBase(chunk, chunkX, chunkZ, blockX, y, blockZ, scratch) != BlockType.AIR) {
            return false;
        }
        if (blockLightAt(blockLight, blockX, y, blockZ) >= 10) {
            return false;
        }
        BlockType support = blockAtDecoratedOrBase(chunk, chunkX, chunkZ, blockX, y - 1, blockZ, scratch);
        return support != BlockType.ICE && BlockShape.isOpaqueCube(support);
    }

    private static byte[] generatedBlockLightSnapshot(Chunk chunk) {
        byte[] light = new byte[Chunk.TOTAL_BLOCKS];
        PackedLightQueue queue = new PackedLightQueue(256);
        boolean hasEmitter = false;
        for (int y = 0; y < Chunk.HEIGHT; y++) {
            for (int z = 0; z < Chunk.DEPTH; z++) {
                for (int x = 0; x < Chunk.WIDTH; x++) {
                    int emission = chunk.getBlock(x, y, z).getLightEmission();
                    if (emission > 0) {
                        hasEmitter = true;
                        int index = Chunk.getIndex(x, y, z);
                        light[index] = (byte) emission;
                        queue.add(x, y, z, emission);
                    }
                }
            }
        }
        if (!hasEmitter) {
            return null;
        }

        while (!queue.isEmpty()) {
            int current = queue.poll();
            int currentLight = PackedLightQueue.light(current);
            for (int direction = 0; direction < LIGHT_DX.length; direction++) {
                int x = PackedLightQueue.x(current) + LIGHT_DX[direction];
                int y = PackedLightQueue.y(current) + LIGHT_DY[direction];
                int z = PackedLightQueue.z(current) + LIGHT_DZ[direction];
                if (!Chunk.isInBounds(x, y, z)) {
                    continue;
                }
                int nextLight = currentLight - sourceLightStep(chunk.getBlock(x, y, z));
                if (nextLight <= 0) {
                    continue;
                }
                int index = Chunk.getIndex(x, y, z);
                if (nextLight > (light[index] & 0xFF)) {
                    light[index] = (byte) nextLight;
                    queue.add(x, y, z, nextLight);
                }
            }
        }
        return light;
    }

    private byte[] generatedBlockLightSnapshot(Chunk chunk, SourceTreeScratch scratch) {
        GeneratedBlockLight blockLight = generatedWorldBlockLightSnapshot(chunk, scratch);
        return blockLight == null ? null : blockLight.target();
    }

    private GeneratedBlockLight generatedWorldBlockLightSnapshot(Chunk chunk) {
        byte[] light = generatedBlockLightSnapshot(chunk);
        if (light == null) {
            return null;
        }
        return new GeneratedBlockLight(light, null,
                chunk.getChunkX() * Chunk.WIDTH, chunk.getChunkZ() * Chunk.DEPTH);
    }

    private GeneratedBlockLight generatedWorldBlockLightSnapshot(Chunk chunk, SourceTreeScratch scratch) {
        if (scratch == null) {
            return generatedWorldBlockLightSnapshot(chunk);
        }
        byte[] light = new byte[Chunk.TOTAL_BLOCKS];
        HashMap<SourceBlockPos, Integer> visited = new HashMap<>();
        ArrayDeque<SourceLightNode> queue = new ArrayDeque<>();
        int baseX = chunk.getChunkX() * Chunk.WIDTH;
        int baseZ = chunk.getChunkZ() * Chunk.DEPTH;
        int margin = SOURCE_POPULATION_LIGHT_MARGIN;

        for (int y = 0; y < Chunk.HEIGHT; y++) {
            for (int z = 0; z < Chunk.DEPTH; z++) {
                for (int x = 0; x < Chunk.WIDTH; x++) {
                    enqueueGeneratedLight(queue, visited, light,
                            baseX + x, y, baseZ + z, chunk.getBlock(x, y, z).getLightEmission(),
                            baseX, baseZ);
                }
            }
        }
        for (var entry : scratch.blocks.entrySet()) {
            SourceBlockPos pos = entry.getKey();
            if (pos.x() < baseX - margin || pos.x() >= baseX + Chunk.WIDTH + margin
                    || pos.z() < baseZ - margin || pos.z() >= baseZ + Chunk.DEPTH + margin) {
                continue;
            }
            enqueueGeneratedLight(queue, visited, light, pos.x(), pos.y(), pos.z(),
                    entry.getValue().type().getLightEmission(), baseX, baseZ);
        }
        if (queue.isEmpty()) {
            return null;
        }

        while (!queue.isEmpty()) {
            SourceLightNode current = queue.removeFirst();
            for (int direction = 0; direction < LIGHT_DX.length; direction++) {
                int x = current.x() + LIGHT_DX[direction];
                int y = current.y() + LIGHT_DY[direction];
                int z = current.z() + LIGHT_DZ[direction];
                if (y < 0 || y >= Chunk.HEIGHT
                        || x < baseX - margin || x >= baseX + Chunk.WIDTH + margin
                        || z < baseZ - margin || z >= baseZ + Chunk.DEPTH + margin) {
                    continue;
                }
                int nextLight = current.light() - sourceLightStep(scratch.getBlock(x, y, z));
                if (nextLight <= 0) {
                    continue;
                }
                SourceBlockPos pos = new SourceBlockPos(x, y, z);
                if (nextLight <= visited.getOrDefault(pos, 0)) {
                    continue;
                }
                visited.put(pos, nextLight);
                if (containsBlock(chunk.getChunkX(), chunk.getChunkZ(), x, z)) {
                    light[Chunk.getIndex(x - baseX, y, z - baseZ)] = (byte) nextLight;
                }
                queue.addLast(new SourceLightNode(x, y, z, nextLight));
            }
        }
        return new GeneratedBlockLight(light, visited, baseX, baseZ);
    }

    private GeneratedBlockLight generatedWorldBlockLightSnapshot(Chunk chunk, NetherDecoratorScratch scratch) {
        if (scratch == null) {
            return generatedWorldBlockLightSnapshot(chunk);
        }
        byte[] light = new byte[Chunk.TOTAL_BLOCKS];
        HashMap<SourceBlockPos, Integer> visited = new HashMap<>();
        ArrayDeque<SourceLightNode> queue = new ArrayDeque<>();
        int baseX = chunk.getChunkX() * Chunk.WIDTH;
        int baseZ = chunk.getChunkZ() * Chunk.DEPTH;
        int margin = SOURCE_POPULATION_LIGHT_MARGIN;

        for (int y = 0; y < Chunk.HEIGHT; y++) {
            for (int z = 0; z < Chunk.DEPTH; z++) {
                for (int x = 0; x < Chunk.WIDTH; x++) {
                    enqueueGeneratedLight(queue, visited, light,
                            baseX + x, y, baseZ + z, chunk.getBlock(x, y, z).getLightEmission(),
                            baseX, baseZ);
                }
            }
        }
        for (var entry : scratch.blocks.entrySet()) {
            SourceBlockPos pos = entry.getKey();
            if (pos.x() < baseX - margin || pos.x() >= baseX + Chunk.WIDTH + margin
                    || pos.z() < baseZ - margin || pos.z() >= baseZ + Chunk.DEPTH + margin) {
                continue;
            }
            enqueueGeneratedLight(queue, visited, light, pos.x(), pos.y(), pos.z(),
                    entry.getValue().type().getLightEmission(), baseX, baseZ);
        }
        if (queue.isEmpty()) {
            return null;
        }

        while (!queue.isEmpty()) {
            SourceLightNode current = queue.removeFirst();
            for (int direction = 0; direction < LIGHT_DX.length; direction++) {
                int x = current.x() + LIGHT_DX[direction];
                int y = current.y() + LIGHT_DY[direction];
                int z = current.z() + LIGHT_DZ[direction];
                if (y < 0 || y >= Chunk.HEIGHT
                        || x < baseX - margin || x >= baseX + Chunk.WIDTH + margin
                        || z < baseZ - margin || z >= baseZ + Chunk.DEPTH + margin) {
                    continue;
                }
                int nextLight = current.light() - sourceLightStep(scratch.getBlock(x, y, z));
                if (nextLight <= 0) {
                    continue;
                }
                SourceBlockPos pos = new SourceBlockPos(x, y, z);
                if (nextLight <= visited.getOrDefault(pos, 0)) {
                    continue;
                }
                visited.put(pos, nextLight);
                if (containsBlock(chunk.getChunkX(), chunk.getChunkZ(), x, z)) {
                    light[Chunk.getIndex(x - baseX, y, z - baseZ)] = (byte) nextLight;
                }
                queue.addLast(new SourceLightNode(x, y, z, nextLight));
            }
        }
        return new GeneratedBlockLight(light, visited, baseX, baseZ);
    }

    private static void enqueueGeneratedLight(ArrayDeque<SourceLightNode> queue,
            HashMap<SourceBlockPos, Integer> visited, byte[] light, int x, int y, int z,
            int emission, int baseX, int baseZ) {
        if (emission <= 0) {
            return;
        }
        SourceBlockPos pos = new SourceBlockPos(x, y, z);
        if (emission <= visited.getOrDefault(pos, 0)) {
            return;
        }
        visited.put(pos, emission);
        if (x >= baseX && x < baseX + Chunk.WIDTH && z >= baseZ && z < baseZ + Chunk.DEPTH) {
            light[Chunk.getIndex(x - baseX, y, z - baseZ)] = (byte) emission;
        }
        queue.addLast(new SourceLightNode(x, y, z, emission));
    }

    private static final class PackedLightQueue {
        private int[] values;
        private int head;
        private int tail;

        PackedLightQueue(int initialCapacity) {
            values = new int[Math.max(16, initialCapacity)];
        }

        boolean isEmpty() {
            return head == tail;
        }

        void add(int x, int y, int z, int light) {
            if (tail >= values.length) {
                if (head > 0) {
                    int size = tail - head;
                    System.arraycopy(values, head, values, 0, size);
                    tail = size;
                    head = 0;
                } else {
                    values = java.util.Arrays.copyOf(values, values.length * 2);
                }
            }
            values[tail++] = pack(x, y, z, light);
        }

        int poll() {
            return values[head++];
        }

        private static int pack(int x, int y, int z, int light) {
            return (x & 15) | ((z & 15) << 4) | ((y & 127) << 8) | ((light & 15) << 15);
        }

        static int x(int packed) {
            return packed & 15;
        }

        static int z(int packed) {
            return (packed >>> 4) & 15;
        }

        static int y(int packed) {
            return (packed >>> 8) & 127;
        }

        static int light(int packed) {
            return (packed >>> 15) & 15;
        }
    }

    private static int blockLightAt(byte[] blockLight, int x, int y, int z) {
        if (blockLight == null || !Chunk.isInBounds(x, y, z)) {
            return 0;
        }
        return blockLight[Chunk.getIndex(x, y, z)] & 0xFF;
    }

    private static int blockLightAt(GeneratedBlockLight blockLight, int blockX, int y, int blockZ) {
        if (blockLight == null || y < 0 || y >= Chunk.HEIGHT) {
            return 0;
        }
        if (blockLight.world() != null) {
            return blockLight.world().getOrDefault(new SourceBlockPos(blockX, y, blockZ), 0);
        }
        int localX = blockX - blockLight.baseX();
        int localZ = blockZ - blockLight.baseZ();
        return blockLightAt(blockLight.target(), localX, y, localZ);
    }

    BlockType baseBlockAt(int blockX, int y, int blockZ) {
        if (y < 0 || y >= Chunk.HEIGHT) {
            return BlockType.AIR;
        }
        if (dimension != Dimension.OVERWORLD) {
            return BlockType.AIR;
        }
        int chunkX = Math.floorDiv(blockX, Chunk.WIDTH);
        int chunkZ = Math.floorDiv(blockZ, Chunk.DEPTH);
        Chunk chunk = cachedOverworldBaseChunk(chunkX, chunkZ);
        return chunk.getBlock(Math.floorMod(blockX, Chunk.WIDTH), y, Math.floorMod(blockZ, Chunk.DEPTH));
    }

    private BlockType carvedBlockAt(int blockX, int y, int blockZ) {
        if (y < 0 || y >= Chunk.HEIGHT) {
            return BlockType.AIR;
        }
        if (dimension != Dimension.OVERWORLD) {
            return BlockType.AIR;
        }
        int chunkX = Math.floorDiv(blockX, Chunk.WIDTH);
        int chunkZ = Math.floorDiv(blockZ, Chunk.DEPTH);
        Chunk chunk = cachedOverworldCarvedChunk(chunkX, chunkZ);
        return chunk.getBlock(Math.floorMod(blockX, Chunk.WIDTH), y, Math.floorMod(blockZ, Chunk.DEPTH));
    }

    private Chunk cachedOverworldBaseChunk(int chunkX, int chunkZ) {
        long key = World.chunkKey(chunkX, chunkZ);
        Chunk cached = overworldBaseChunkCache.get(key);
        if (cached != null) {
            return cached;
        }
        Chunk generated = new Chunk(chunkX, chunkZ);
        generateOverworldBaseChunk(generated, chunkX, chunkZ);
        if (overworldBaseChunkCache.size() > MAX_BASE_CHUNK_CACHE_ENTRIES) {
            overworldBaseChunkCache.clear();
        }
        Chunk previous = overworldBaseChunkCache.putIfAbsent(key, generated);
        return previous == null ? generated : previous;
    }

    private Chunk cachedOverworldCarvedChunk(int chunkX, int chunkZ) {
        long key = World.chunkKey(chunkX, chunkZ);
        Chunk cached = overworldCarvedChunkCache.get(key);
        if (cached != null) {
            return cached;
        }
        Chunk generated = new Chunk(chunkX, chunkZ);
        generateOverworldCarvedChunk(generated, chunkX, chunkZ);
        generated.clearModified();
        if (overworldCarvedChunkCache.size() > MAX_BASE_CHUNK_CACHE_ENTRIES) {
            overworldCarvedChunkCache.clear();
        }
        Chunk previous = overworldCarvedChunkCache.putIfAbsent(key, generated);
        return previous == null ? generated : previous;
    }

    int terrainTopY(int blockX, int blockZ) {
        return terrainTopY(blockX, blockZ, getBiome(blockX, blockZ));
    }

    int terrainTopY(int blockX, int blockZ, BiomeType biome) {
        return densityField.terrainTopY(blockX, blockZ, biome);
    }

    private BlockType topBlockFor(BiomeType biome, int terrainTop) {
        return biome.getTopBlock();
    }

    private BlockType fillerBlockFor(BiomeType biome, int terrainTop) {
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
                    if (!block.isFallingBlock()
                            || !BlockShape.canFallingBlockFallThrough(chunk.getBlock(x, y - 1, z))) {
                        continue;
                    }
                    BlockType filler = block == BlockType.SAND ? BlockType.SANDSTONE : BlockType.STONE;
                    int fillY = y - 1;
                    int filled = 0;
                    while (fillY > 0 && BlockShape.canFallingBlockFallThrough(chunk.getBlock(x, fillY, z))
                            && filled < 8) {
                        chunk.setBlock(x, fillY, z, filler);
                        fillY--;
                        filled++;
                    }
                    if (fillY <= 0 || BlockShape.canFallingBlockFallThrough(chunk.getBlock(x, fillY, z))) {
                        chunk.setBlock(x, y, z, filler);
                    }
                }
            }
        }
    }

    private void generateNether(World world, Chunk chunk, int chunkX, int chunkZ) {
        generateNetherBaseChunk(chunk, chunkX, chunkZ);
        Random populationRandom = netherDecoratorRandom(chunkX, chunkZ);
        if (generateStructures) {
            structureGenerator.generate(world, chunk, seed, chunkX, chunkZ, dimension, this, populationRandom);
        }
        decorateNether(chunk, chunkX, chunkZ, populationRandom);
    }

    private void generateNetherBaseChunk(Chunk chunk, int chunkX, int chunkZ) {
        generateNetherTerrain(chunk, chunkX, chunkZ);
        replaceNetherSurface(chunk, chunkX, chunkZ, netherSurfaceRandom(chunkX, chunkZ));
        netherCaveGenerator.generate(chunk, seed);
    }

    private Random netherSurfaceRandom(int chunkX, int chunkZ) {
        return new Random((long) chunkX * 0x4f9939f508L + (long) chunkZ * 0x1ef1565bd5L);
    }

    private Random netherDecoratorRandom(int chunkX, int chunkZ) {
        Random random = netherSurfaceRandom(chunkX, chunkZ);
        advanceNetherSurfaceRandom(random);
        return random;
    }

    private Random netherPopulationRandomAfterStructures(int chunkX, int chunkZ) {
        Random random = netherDecoratorRandom(chunkX, chunkZ);
        if (generateStructures) {
            structureGenerator.advancePlacementRandom(seed, chunkX, chunkZ, Dimension.NETHER, this, random);
        }
        return random;
    }

    private NetherDecoratorScratch sourceNetherDecoratorScratch(Chunk chunk, int chunkX, int chunkZ) {
        NetherDecoratorScratch scratch = new NetherDecoratorScratch(chunk, chunkX, chunkZ);
        overlayNetherStructureSideEffects(scratch, chunkX, chunkZ);
        return scratch;
    }

    private void overlayNetherStructureSideEffects(NetherDecoratorScratch scratch, int chunkX, int chunkZ) {
        if (!generateStructures) {
            return;
        }
        for (int sourceChunkX = chunkX + SCRATCH_POPULATION_MIN_CHUNK_OFFSET;
                sourceChunkX <= chunkX + SCRATCH_POPULATION_MAX_CHUNK_OFFSET; sourceChunkX++) {
            for (int sourceChunkZ = chunkZ + SCRATCH_POPULATION_MIN_CHUNK_OFFSET;
                    sourceChunkZ <= chunkZ + SCRATCH_POPULATION_MAX_CHUNK_OFFSET; sourceChunkZ++) {
                if (sourceChunkX == chunkX && sourceChunkZ == chunkZ) {
                    continue;
                }
                overlayNetherStructureChunk(scratch, sourceChunkX, sourceChunkZ);
            }
        }
    }

    private void overlayNetherStructureChunk(NetherDecoratorScratch scratch, int sourceChunkX, int sourceChunkZ) {
        int baseX = sourceChunkX * Chunk.WIDTH;
        int baseZ = sourceChunkZ * Chunk.DEPTH;
        for (StructureBlockDelta delta : cachedNetherStructureDeltas(sourceChunkX, sourceChunkZ)) {
            scratch.setBlock(baseX + delta.localX(), delta.y(), baseZ + delta.localZ(),
                    delta.type(), delta.metadata());
        }
    }

    private static void advanceNetherSurfaceRandom(Random random) {
        for (int x = 0; x < Chunk.WIDTH; x++) {
            for (int z = 0; z < Chunk.DEPTH; z++) {
                random.nextDouble();
                random.nextDouble();
                random.nextDouble();
                for (int y = Chunk.HEIGHT - 1; y >= 0; y--) {
                    if (y >= Chunk.HEIGHT - 1 - random.nextInt(5)) {
                        continue;
                    }
                    random.nextInt(5);
                }
            }
        }
    }

    private void generateNetherTerrain(Chunk chunk, int chunkX, int chunkZ) {
        int horizontalCells = 4;
        int verticalSamples = 17;
        int densityStride = horizontalCells + 1;
        double[] densities = netherDensities(chunkX * horizontalCells, 0, chunkZ * horizontalCells,
                horizontalCells + 1, verticalSamples, horizontalCells + 1);

        for (int cellX = 0; cellX < horizontalCells; cellX++) {
            for (int cellZ = 0; cellZ < horizontalCells; cellZ++) {
                for (int sampleY = 0; sampleY < 16; sampleY++) {
                    double yStep = 0.125;
                    double d000 = densities[((cellX + 0) * densityStride + (cellZ + 0)) * verticalSamples + sampleY];
                    double d001 = densities[((cellX + 0) * densityStride + (cellZ + 1)) * verticalSamples + sampleY];
                    double d100 = densities[((cellX + 1) * densityStride + (cellZ + 0)) * verticalSamples + sampleY];
                    double d101 = densities[((cellX + 1) * densityStride + (cellZ + 1)) * verticalSamples + sampleY];
                    double dy000 = (densities[((cellX + 0) * densityStride + (cellZ + 0)) * verticalSamples
                            + sampleY + 1] - d000) * yStep;
                    double dy001 = (densities[((cellX + 0) * densityStride + (cellZ + 1)) * verticalSamples
                            + sampleY + 1] - d001) * yStep;
                    double dy100 = (densities[((cellX + 1) * densityStride + (cellZ + 0)) * verticalSamples
                            + sampleY + 1] - d100) * yStep;
                    double dy101 = (densities[((cellX + 1) * densityStride + (cellZ + 1)) * verticalSamples
                            + sampleY + 1] - d101) * yStep;

                    for (int localYStep = 0; localYStep < 8; localYStep++) {
                        double xStep = 0.25;
                        double x0 = d000;
                        double x1 = d001;
                        double dx0 = (d100 - d000) * xStep;
                        double dx1 = (d101 - d001) * xStep;

                        for (int localXStep = 0; localXStep < 4; localXStep++) {
                            double zStep = 0.25;
                            double density = x0;
                            double dz = (x1 - x0) * zStep;

                            for (int localZStep = 0; localZStep < 4; localZStep++) {
                                int x = cellX * 4 + localXStep;
                                int y = sampleY * 8 + localYStep;
                                int z = cellZ * 4 + localZStep;
                                BlockType type = y < 32 ? BlockType.LAVA : BlockType.AIR;
                                if (density > 0.0) {
                                    type = BlockType.NETHERRACK;
                                }
                                chunk.setBlock(x, y, z, type);
                                density += dz;
                            }

                            x0 += dx0;
                            x1 += dx1;
                        }

                        d000 += dy000;
                        d001 += dy001;
                        d100 += dy100;
                        d101 += dy101;
                    }
                }
            }
        }
    }

    private double[] netherDensities(int startX, int startY, int startZ, int xSize, int ySize, int zSize) {
        double horizontalScale = 684.41200000000003;
        double verticalScale = 2053.2359999999999;
        double[] terrainShapeNoise = netherNoise6.generateNoiseOctaves(null, startX, startY, startZ, xSize, 1, zSize,
                1.0, 0.0, 1.0);
        double[] depthNoise = netherNoise7.generateNoiseOctaves(null, startX, startY, startZ, xSize, 1, zSize,
                100.0, 0.0, 100.0);
        double[] selectorNoise = netherNoise3.generateNoiseOctaves(null, startX, startY, startZ, xSize, ySize, zSize,
                horizontalScale / 80.0, verticalScale / 60.0, horizontalScale / 80.0);
        double[] minLimitNoise = netherNoise1.generateNoiseOctaves(null, startX, startY, startZ, xSize, ySize, zSize,
                horizontalScale, verticalScale, horizontalScale);
        double[] maxLimitNoise = netherNoise2.generateNoiseOctaves(null, startX, startY, startZ, xSize, ySize, zSize,
                horizontalScale, verticalScale, horizontalScale);
        double[] densities = new double[xSize * ySize * zSize];
        double[] verticalCurve = new double[ySize];

        for (int y = 0; y < ySize; y++) {
            verticalCurve[y] = Math.cos(y * Math.PI * 6.0 / ySize) * 2.0;
            double edge = y;
            if (y > ySize / 2) {
                edge = ySize - 1 - y;
            }
            if (edge < 4.0) {
                edge = 4.0 - edge;
                verticalCurve[y] -= edge * edge * edge * 10.0;
            }
        }

        int noiseIndex = 0;
        int horizontalIndex = 0;
        for (int x = 0; x < xSize; x++) {
            for (int z = 0; z < zSize; z++) {
                double terrainShape = (terrainShapeNoise[horizontalIndex] + 256.0) / 512.0;
                if (terrainShape > 1.0) {
                    terrainShape = 1.0;
                }

                double lowerCutoff = 0.0;
                double depth = depthNoise[horizontalIndex] / 8000.0;
                if (depth < 0.0) {
                    depth = -depth;
                }
                depth = depth * 3.0 - 3.0;
                if (depth < 0.0) {
                    depth /= 2.0;
                    if (depth < -1.0) {
                        depth = -1.0;
                    }
                    depth /= 1.4;
                    depth /= 2.0;
                    terrainShape = 0.0;
                } else {
                    if (depth > 1.0) {
                        depth = 1.0;
                    }
                    depth /= 6.0;
                }
                terrainShape += 0.5;
                depth = depth * ySize / 16.0;
                horizontalIndex++;

                for (int y = 0; y < ySize; y++) {
                    double density = 0.0;
                    double minLimit = minLimitNoise[noiseIndex] / 512.0;
                    double maxLimit = maxLimitNoise[noiseIndex] / 512.0;
                    double selector = (selectorNoise[noiseIndex] / 10.0 + 1.0) / 2.0;
                    if (selector < 0.0) {
                        density = minLimit;
                    } else if (selector > 1.0) {
                        density = maxLimit;
                    } else {
                        density = minLimit + (maxLimit - minLimit) * selector;
                    }

                    density -= verticalCurve[y];
                    if (y > ySize - 4) {
                        double fade = (double) (y - (ySize - 4)) / 3.0;
                        density = density * (1.0 - fade) + -10.0 * fade;
                    }
                    if (y < lowerCutoff) {
                        double fade = (lowerCutoff - y) / 4.0;
                        fade = Math.max(0.0, Math.min(1.0, fade));
                        density = density * (1.0 - fade) + -10.0 * fade;
                    }
                    densities[noiseIndex] = density;
                    noiseIndex++;
                }
            }
        }
        return densities;
    }

    private void replaceNetherSurface(Chunk chunk, int chunkX, int chunkZ, Random random) {
        double scale = 0.03125;
        double[] slowsandNoiseValues = slowsandGravelNoise.generateNoiseOctaves(null, chunkX * 16, chunkZ * 16, 0,
                16, 16, 1, scale, scale, 1.0);
        double[] gravelNoiseValues = slowsandGravelNoise.generateNoiseOctaves(null, chunkX * 16, 109,
                chunkZ * 16,
                16, 1, 16, scale, 1.0, scale);
        double[] exclusivityNoiseValues = netherrackExclusivityNoise.generateNoiseOctaves(null, chunkX * 16,
                chunkZ * 16, 0, 16, 16, 1, scale * 2.0, scale * 2.0, scale * 2.0);

        for (int x = 0; x < Chunk.WIDTH; x++) {
            for (int z = 0; z < Chunk.DEPTH; z++) {
                int noiseIndex = x + z * 16;
                boolean soulSand = slowsandNoiseValues[noiseIndex] + random.nextDouble() * 0.2 > 0.0;
                boolean gravel = gravelNoiseValues[noiseIndex] + random.nextDouble() * 0.2 > 0.0;
                int thickness = (int) (exclusivityNoiseValues[noiseIndex] / 3.0 + 3.0
                        + random.nextDouble() * 0.25);
                int run = -1;
                BlockType top = BlockType.NETHERRACK;
                BlockType filler = BlockType.NETHERRACK;

                for (int y = Chunk.HEIGHT - 1; y >= 0; y--) {
                    if (y >= Chunk.HEIGHT - 1 - random.nextInt(5)) {
                        chunk.setBlock(x, y, z, BlockType.BEDROCK);
                        continue;
                    }
                    if (y <= random.nextInt(5)) {
                        chunk.setBlock(x, y, z, BlockType.BEDROCK);
                        continue;
                    }

                    BlockType current = chunk.getBlock(x, y, z);
                    if (current == BlockType.AIR) {
                        run = -1;
                        continue;
                    }
                    if (current != BlockType.NETHERRACK) {
                        continue;
                    }

                    if (run == -1) {
                        if (thickness <= 0) {
                            top = BlockType.AIR;
                            filler = BlockType.NETHERRACK;
                        } else if (y >= 60 && y <= 65) {
                            top = BlockType.NETHERRACK;
                            filler = BlockType.NETHERRACK;
                            if (gravel) {
                                top = BlockType.GRAVEL;
                                filler = BlockType.NETHERRACK;
                            }
                            if (soulSand) {
                                top = BlockType.SOUL_SAND;
                                filler = BlockType.SOUL_SAND;
                            }
                        }
                        if (y < 64 && top == BlockType.AIR) {
                            top = BlockType.LAVA;
                        }

                        run = thickness;
                        chunk.setBlock(x, y, z, y >= 63 ? top : filler);
                        continue;
                    }

                    if (run > 0) {
                        run--;
                        chunk.setBlock(x, y, z, filler);
                    }
                }
            }
        }
    }

    private void decorateNether(Chunk chunk, int chunkX, int chunkZ) {
        decorateNether(chunk, chunkX, chunkZ, null);
    }

    private void decorateNether(Chunk chunk, int chunkX, int chunkZ, Random currentOriginRandom) {
        NetherDecoratorScratch scratch = sourceNetherDecoratorScratch(chunk, chunkX, chunkZ);
        for (int originChunkX = chunkX + SCRATCH_POPULATION_MIN_CHUNK_OFFSET;
                originChunkX <= chunkX + SCRATCH_POPULATION_MAX_CHUNK_OFFSET; originChunkX++) {
            for (int originChunkZ = chunkZ + SCRATCH_POPULATION_MIN_CHUNK_OFFSET;
                    originChunkZ <= chunkZ + SCRATCH_POPULATION_MAX_CHUNK_OFFSET; originChunkZ++) {
                Random random = currentOriginRandom != null && originChunkX == chunkX && originChunkZ == chunkZ
                        ? currentOriginRandom
                        : netherPopulationRandomAfterStructures(originChunkX, originChunkZ);
                decorateNetherFromOrigin(chunk, chunkX, chunkZ, scratch, originChunkX, originChunkZ, random);
            }
        }
    }

    private void decorateNetherFromOrigin(Chunk chunk, int chunkX, int chunkZ, int originChunkX, int originChunkZ) {
        decorateNetherFromOrigin(chunk, chunkX, chunkZ, null, originChunkX, originChunkZ);
    }

    private void decorateNetherFromOrigin(Chunk chunk, int chunkX, int chunkZ, NetherDecoratorScratch scratch,
            int originChunkX, int originChunkZ) {
        decorateNetherFromOrigin(chunk, chunkX, chunkZ, scratch, originChunkX, originChunkZ,
                netherPopulationRandomAfterStructures(originChunkX, originChunkZ));
    }

    private void decorateNetherFromOrigin(Chunk chunk, int chunkX, int chunkZ, NetherDecoratorScratch scratch,
            int originChunkX, int originChunkZ, Random random) {
        int originX = originChunkX * Chunk.WIDTH;
        int originZ = originChunkZ * Chunk.DEPTH;

        for (int i = 0; i < 8; i++) {
            placeNetherLavaSpring(chunk, chunkX, chunkZ, random, scratch,
                    originX + random.nextInt(Chunk.WIDTH) + 8,
                    random.nextInt(120) + 4,
                    originZ + random.nextInt(Chunk.DEPTH) + 8);
        }
        int fireCount = random.nextInt(random.nextInt(10) + 1) + 1;
        for (int i = 0; i < fireCount; i++) {
            placeNetherFire(chunk, chunkX, chunkZ, random, scratch,
                    originX + random.nextInt(Chunk.WIDTH) + 8,
                    random.nextInt(120) + 4,
                    originZ + random.nextInt(Chunk.DEPTH) + 8);
        }
        int ceilingGlowstoneCount = random.nextInt(random.nextInt(10) + 1);
        for (int i = 0; i < ceilingGlowstoneCount; i++) {
            placeGlowstoneCluster(chunk, chunkX, chunkZ, random, scratch,
                    originX + random.nextInt(Chunk.WIDTH) + 8,
                    random.nextInt(120) + 4,
                    originZ + random.nextInt(Chunk.DEPTH) + 8);
        }
        for (int i = 0; i < 10; i++) {
            placeGlowstoneCluster(chunk, chunkX, chunkZ, random, scratch,
                    originX + random.nextInt(Chunk.WIDTH) + 8,
                    random.nextInt(128),
                    originZ + random.nextInt(Chunk.DEPTH) + 8);
        }
        GeneratedBlockLight blockLight = generatedWorldBlockLightSnapshot(chunk, scratch);
        if (random.nextInt(1) == 0) {
            placeNetherMushroom(chunk, chunkX, chunkZ, random, scratch, BlockType.BROWN_MUSHROOM,
                    originX + random.nextInt(Chunk.WIDTH) + 8,
                    random.nextInt(128),
                    originZ + random.nextInt(Chunk.DEPTH) + 8,
                    blockLight);
        }
        if (random.nextInt(1) == 0) {
            placeNetherMushroom(chunk, chunkX, chunkZ, random, scratch, BlockType.RED_MUSHROOM,
                    originX + random.nextInt(Chunk.WIDTH) + 8,
                    random.nextInt(128),
                    originZ + random.nextInt(Chunk.DEPTH) + 8,
                    blockLight);
        }
    }

    private void placeNetherLavaSpring(Chunk chunk, int chunkX, int chunkZ, Random random, int x, int y, int z) {
        placeNetherLavaSpring(chunk, chunkX, chunkZ, random, null, x, y, z);
    }

    private void placeNetherLavaSpring(Chunk chunk, int chunkX, int chunkZ, Random random,
            NetherDecoratorScratch scratch, int x, int y, int z) {
        BlockType current = netherDecoratorBlockAt(chunk, chunkX, chunkZ, scratch, x, y, z);
        if (netherDecoratorBlockAt(chunk, chunkX, chunkZ, scratch, x, y + 1, z) != BlockType.NETHERRACK
                || (current != BlockType.AIR && current != BlockType.NETHERRACK)) {
            return;
        }
        int solidSides = 0;
        int airSides = 0;
        int[][] dirs = { { -1, 0, 0 }, { 1, 0, 0 }, { 0, 0, -1 }, { 0, 0, 1 }, { 0, -1, 0 } };
        for (int[] dir : dirs) {
            BlockType neighbor = netherDecoratorBlockAt(chunk, chunkX, chunkZ, scratch,
                    x + dir[0], y + dir[1], z + dir[2]);
            if (neighbor == BlockType.NETHERRACK) {
                solidSides++;
            } else if (neighbor == BlockType.AIR) {
                airSides++;
            }
        }
        if (solidSides == 4 && airSides == 1) {
            setNetherDecoratorBlock(chunk, chunkX, chunkZ, scratch, x, y, z, BlockType.LAVA);
            for (int[] dir : dirs) {
                int flowX = x + dir[0];
                int flowY = y + dir[1];
                int flowZ = z + dir[2];
                if (netherDecoratorBlockAt(chunk, chunkX, chunkZ, scratch, flowX, flowY, flowZ) == BlockType.AIR) {
                    setNetherDecoratorBlock(chunk, chunkX, chunkZ, scratch, flowX, flowY, flowZ,
                            BlockType.FLOWING_LAVA, dir[1] < 0 ? 8 : 1);
                    break;
                }
            }
        }
    }

    private void placeNetherFire(Chunk chunk, int chunkX, int chunkZ, Random random, int x, int y, int z) {
        placeNetherFire(chunk, chunkX, chunkZ, random, null, x, y, z);
    }

    private void placeNetherFire(Chunk chunk, int chunkX, int chunkZ, Random random, NetherDecoratorScratch scratch,
            int x, int y, int z) {
        for (int i = 0; i < 64; i++) {
            int px = x + random.nextInt(8) - random.nextInt(8);
            int py = y + random.nextInt(4) - random.nextInt(4);
            int pz = z + random.nextInt(8) - random.nextInt(8);
            if (netherDecoratorBlockAt(chunk, chunkX, chunkZ, scratch, px, py, pz) == BlockType.AIR
                    && netherDecoratorBlockAt(chunk, chunkX, chunkZ, scratch, px, py - 1, pz)
                            == BlockType.NETHERRACK) {
                setNetherDecoratorBlock(chunk, chunkX, chunkZ, scratch, px, py, pz, BlockType.FIRE);
            }
        }
    }

    private void placeGlowstoneCluster(Chunk chunk, int chunkX, int chunkZ, Random random, int x, int y, int z) {
        placeGlowstoneCluster(chunk, chunkX, chunkZ, random, null, x, y, z);
    }

    private void placeGlowstoneCluster(Chunk chunk, int chunkX, int chunkZ, Random random,
            NetherDecoratorScratch scratch, int x, int y, int z) {
        if (netherDecoratorBlockAt(chunk, chunkX, chunkZ, scratch, x, y, z) != BlockType.AIR
                || netherDecoratorBlockAt(chunk, chunkX, chunkZ, scratch, x, y + 1, z) != BlockType.NETHERRACK) {
            return;
        }
        setNetherDecoratorBlock(chunk, chunkX, chunkZ, scratch, x, y, z, BlockType.GLOWSTONE);
        for (int i = 0; i < 1500; i++) {
            int px = x + random.nextInt(8) - random.nextInt(8);
            int py = y - random.nextInt(12);
            int pz = z + random.nextInt(8) - random.nextInt(8);
            if (py < 0 || py >= Chunk.HEIGHT
                    || netherDecoratorBlockAt(chunk, chunkX, chunkZ, scratch, px, py, pz) != BlockType.AIR) {
                continue;
            }
            if (countAdjacentNether(chunk, chunkX, chunkZ, scratch, px, py, pz, BlockType.GLOWSTONE) == 1) {
                setNetherDecoratorBlock(chunk, chunkX, chunkZ, scratch, px, py, pz, BlockType.GLOWSTONE);
            }
        }
    }

    private void placeNetherPatch(Chunk chunk, int chunkX, int chunkZ, BlockType replacement, int radius,
            int x, int y, int z) {
        placeNetherPatch(chunk, chunkX, chunkZ, null, replacement, radius, x, y, z);
    }

    private void placeNetherPatch(Chunk chunk, int chunkX, int chunkZ, NetherDecoratorScratch scratch,
            BlockType replacement, int radius, int x, int y, int z) {
        if (netherDecoratorBlockAt(chunk, chunkX, chunkZ, scratch, x, y, z) != BlockType.NETHERRACK
                || !hasAdjacentNether(chunk, chunkX, chunkZ, scratch, x, y, z, BlockType.AIR)) {
            return;
        }
        int radiusSquared = radius * radius;
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                for (int dy = -1; dy <= 1; dy++) {
                    if (dx * dx + dz * dz + dy * dy * 2 > radiusSquared) {
                        continue;
                    }
                    int px = x + dx;
                    int py = y + dy;
                    int pz = z + dz;
                    if (netherDecoratorBlockAt(chunk, chunkX, chunkZ, scratch, px, py, pz)
                            == BlockType.NETHERRACK) {
                        setNetherDecoratorBlock(chunk, chunkX, chunkZ, scratch, px, py, pz, replacement);
                    }
                }
            }
        }
    }

    private void placeNetherMushroom(Chunk chunk, int chunkX, int chunkZ, Random random, BlockType mushroom,
            int x, int y, int z) {
        placeNetherMushroom(chunk, chunkX, chunkZ, random, null, mushroom, x, y, z,
                generatedWorldBlockLightSnapshot(chunk));
    }

    private void placeNetherMushroom(Chunk chunk, int chunkX, int chunkZ, Random random,
            NetherDecoratorScratch scratch, BlockType mushroom, int x, int y, int z) {
        placeNetherMushroom(chunk, chunkX, chunkZ, random, scratch, mushroom, x, y, z,
                generatedWorldBlockLightSnapshot(chunk, scratch));
    }

    private void placeNetherMushroom(Chunk chunk, int chunkX, int chunkZ, Random random,
            NetherDecoratorScratch scratch, BlockType mushroom, int x, int y, int z,
            GeneratedBlockLight blockLight) {
        for (int i = 0; i < 64; i++) {
            int px = x + random.nextInt(8) - random.nextInt(8);
            int py = y + random.nextInt(4) - random.nextInt(4);
            int pz = z + random.nextInt(8) - random.nextInt(8);
            BlockType support = netherDecoratorBlockAt(chunk, chunkX, chunkZ, scratch, px, py - 1, pz);
            if (netherDecoratorBlockAt(chunk, chunkX, chunkZ, scratch, px, py, pz) == BlockType.AIR
                    && canNetherMushroomStayOn(support, blockLight, px, py, pz)) {
                setNetherDecoratorBlock(chunk, chunkX, chunkZ, scratch, px, py, pz, mushroom);
            }
        }
    }

    private static boolean canNetherMushroomStayOn(BlockType support) {
        return canNetherMushroomStayOn(support, null, 0, 0, 0);
    }

    private static boolean canNetherMushroomStayOn(BlockType support, GeneratedBlockLight blockLight,
            int blockX, int y, int blockZ) {
        if (support == BlockType.MYCELIUM) {
            return true;
        }
        if (support != BlockType.SOUL_SAND && !BlockShape.isOpaqueCube(support)) {
            return false;
        }
        return blockLightAt(blockLight, blockX, y, blockZ) < 13;
    }

    private int countAdjacentNether(Chunk chunk, int chunkX, int chunkZ, int x, int y, int z, BlockType type) {
        return countAdjacentNether(chunk, chunkX, chunkZ, null, x, y, z, type);
    }

    private int countAdjacentNether(Chunk chunk, int chunkX, int chunkZ, NetherDecoratorScratch scratch,
            int x, int y, int z, BlockType type) {
        int count = 0;
        if (netherDecoratorBlockAt(chunk, chunkX, chunkZ, scratch, x + 1, y, z) == type) {
            count++;
        }
        if (netherDecoratorBlockAt(chunk, chunkX, chunkZ, scratch, x - 1, y, z) == type) {
            count++;
        }
        if (netherDecoratorBlockAt(chunk, chunkX, chunkZ, scratch, x, y + 1, z) == type) {
            count++;
        }
        if (netherDecoratorBlockAt(chunk, chunkX, chunkZ, scratch, x, y - 1, z) == type) {
            count++;
        }
        if (netherDecoratorBlockAt(chunk, chunkX, chunkZ, scratch, x, y, z + 1) == type) {
            count++;
        }
        if (netherDecoratorBlockAt(chunk, chunkX, chunkZ, scratch, x, y, z - 1) == type) {
            count++;
        }
        return count;
    }

    private boolean hasAdjacentNether(Chunk chunk, int chunkX, int chunkZ, int x, int y, int z, BlockType type) {
        return hasAdjacentNether(chunk, chunkX, chunkZ, null, x, y, z, type);
    }

    private boolean hasAdjacentNether(Chunk chunk, int chunkX, int chunkZ, NetherDecoratorScratch scratch,
            int x, int y, int z, BlockType type) {
        return countAdjacentNether(chunk, chunkX, chunkZ, scratch, x, y, z, type) > 0;
    }

    private BlockType netherDecoratorBlockAt(Chunk chunk, int chunkX, int chunkZ, int blockX, int y, int blockZ) {
        return netherDecoratorBlockAt(chunk, chunkX, chunkZ, null, blockX, y, blockZ);
    }

    private BlockType netherDecoratorBlockAt(Chunk chunk, int chunkX, int chunkZ, NetherDecoratorScratch scratch,
            int blockX, int y, int blockZ) {
        if (y < 0 || y >= Chunk.HEIGHT) {
            return BlockType.AIR;
        }
        if (scratch != null) {
            return scratch.getBlock(blockX, y, blockZ);
        }
        if (containsBlock(chunkX, chunkZ, blockX, blockZ)) {
            return chunk.getBlock(blockX - chunkX * Chunk.WIDTH, y, blockZ - chunkZ * Chunk.DEPTH);
        }
        return netherBaseBlockAt(blockX, y, blockZ);
    }

    private void setNetherDecoratorBlock(Chunk chunk, int chunkX, int chunkZ, NetherDecoratorScratch scratch,
            int blockX, int y, int blockZ, BlockType type) {
        setNetherDecoratorBlock(chunk, chunkX, chunkZ, scratch, blockX, y, blockZ, type, 0);
    }

    private void setNetherDecoratorBlock(Chunk chunk, int chunkX, int chunkZ, NetherDecoratorScratch scratch,
            int blockX, int y, int blockZ, BlockType type, int metadata) {
        if (scratch != null) {
            scratch.setBlock(blockX, y, blockZ, type, metadata);
        }
        setIfInChunk(chunk, chunkX, chunkZ, blockX, y, blockZ, type, metadata);
    }

    private BlockType netherBaseBlockAt(int blockX, int y, int blockZ) {
        if (y < 0 || y >= Chunk.HEIGHT) {
            return BlockType.AIR;
        }
        int chunkX = Math.floorDiv(blockX, Chunk.WIDTH);
        int chunkZ = Math.floorDiv(blockZ, Chunk.DEPTH);
        Chunk chunk = cachedNetherBaseChunk(chunkX, chunkZ);
        return chunk.getBlock(Math.floorMod(blockX, Chunk.WIDTH), y, Math.floorMod(blockZ, Chunk.DEPTH));
    }

    private Chunk cachedNetherBaseChunk(int chunkX, int chunkZ) {
        long key = World.chunkKey(chunkX, chunkZ);
        Chunk cached = netherBaseChunkCache.get(key);
        if (cached != null) {
            return cached;
        }
        Chunk generated = new Chunk(chunkX, chunkZ);
        generateNetherBaseChunk(generated, chunkX, chunkZ);
        if (netherBaseChunkCache.size() > MAX_BASE_CHUNK_CACHE_ENTRIES) {
            netherBaseChunkCache.clear();
        }
        Chunk previous = netherBaseChunkCache.putIfAbsent(key, generated);
        return previous == null ? generated : previous;
    }

    private Chunk cachedNetherStructureChunk(int chunkX, int chunkZ) {
        if (!generateStructures) {
            return cachedNetherBaseChunk(chunkX, chunkZ);
        }
        long key = World.chunkKey(chunkX, chunkZ);
        Chunk cached = netherStructureChunkCache.get(key);
        if (cached != null) {
            return cached;
        }
        Chunk chunk = copyChunk(cachedNetherBaseChunk(chunkX, chunkZ));
        Random random = netherDecoratorRandom(chunkX, chunkZ);
        structureGenerator.generate(null, chunk, seed, chunkX, chunkZ, Dimension.NETHER, this, random);
        chunk.clearModified();
        if (netherStructureChunkCache.size() > MAX_BASE_CHUNK_CACHE_ENTRIES) {
            netherStructureChunkCache.clear();
            netherStructureDeltaCache.clear();
        }
        Chunk previous = netherStructureChunkCache.putIfAbsent(key, chunk);
        return previous == null ? chunk : previous;
    }

    private List<StructureBlockDelta> cachedNetherStructureDeltas(int chunkX, int chunkZ) {
        if (!generateStructures) {
            return List.of();
        }
        long key = World.chunkKey(chunkX, chunkZ);
        List<StructureBlockDelta> cached = netherStructureDeltaCache.get(key);
        if (cached != null) {
            return cached;
        }
        Chunk before = cachedNetherBaseChunk(chunkX, chunkZ);
        Chunk after = cachedNetherStructureChunk(chunkX, chunkZ);
        ArrayList<StructureBlockDelta> deltas = new ArrayList<>();
        for (int y = 0; y < Chunk.HEIGHT; y++) {
            for (int z = 0; z < Chunk.DEPTH; z++) {
                for (int x = 0; x < Chunk.WIDTH; x++) {
                    BlockType type = after.getBlock(x, y, z);
                    int metadata = after.getBlockMetadata(x, y, z);
                    if (type != before.getBlock(x, y, z) || metadata != before.getBlockMetadata(x, y, z)) {
                        deltas.add(new StructureBlockDelta(x, y, z, type, metadata));
                    }
                }
            }
        }
        List<StructureBlockDelta> result = List.copyOf(deltas);
        if (netherStructureDeltaCache.size() > MAX_BASE_CHUNK_CACHE_ENTRIES) {
            netherStructureDeltaCache.clear();
        }
        List<StructureBlockDelta> previous = netherStructureDeltaCache.putIfAbsent(key, result);
        return previous == null ? result : previous;
    }

    private void generateEnd(World world, Chunk chunk, int chunkX, int chunkZ) {
        generateEndBaseChunk(chunk, chunkX, chunkZ);
        placeEndSpikes(world, chunk, chunkX, chunkZ);
        placeEndEntryPlatform(chunk, chunkX, chunkZ);
        if (world != null && chunkX == 0 && chunkZ == 0 && !world.hasEntityOfType(EnderDragon.class)) {
            EnderDragon dragon = new EnderDragon();
            dragon.setPosition(0.0f, 128.0f, 0.0f);
            dragon.setYaw(endDragonYaw(chunkX, chunkZ));
            world.stageGeneratedEntity(dragon);
        }
    }

    private void placeEndEntryPlatform(Chunk chunk, int chunkX, int chunkZ) {
        int cx = DimensionTransferService.END_PLATFORM_CENTER_X;
        int cy = DimensionTransferService.END_PLATFORM_Y;
        int cz = DimensionTransferService.END_PLATFORM_CENTER_Z;
        int radius = DimensionTransferService.END_PLATFORM_RADIUS;
        for (int x = cx - radius; x <= cx + radius; x++) {
            for (int z = cz - radius; z <= cz + radius; z++) {
                setIfInChunk(chunk, chunkX, chunkZ, x, cy, z, BlockType.OBSIDIAN);
                for (int y = cy + 1; y <= cy + DimensionTransferService.END_PLATFORM_CLEARANCE; y++) {
                    setIfInChunk(chunk, chunkX, chunkZ, x, y, z, BlockType.AIR);
                }
            }
        }
    }

    private void generateEndBaseChunk(Chunk chunk, int chunkX, int chunkZ) {
        int horizontalCells = 2;
        int verticalSamples = 33;
        int densityStride = horizontalCells + 1;
        double[] densities = endDensities(chunkX * horizontalCells, 0, chunkZ * horizontalCells,
                horizontalCells + 1, verticalSamples, horizontalCells + 1);

        for (int cellX = 0; cellX < horizontalCells; cellX++) {
            for (int cellZ = 0; cellZ < horizontalCells; cellZ++) {
                for (int sampleY = 0; sampleY < 32; sampleY++) {
                    double yStep = 0.25;
                    double d000 = densities[((cellX + 0) * densityStride + (cellZ + 0)) * verticalSamples + sampleY];
                    double d001 = densities[((cellX + 0) * densityStride + (cellZ + 1)) * verticalSamples + sampleY];
                    double d100 = densities[((cellX + 1) * densityStride + (cellZ + 0)) * verticalSamples + sampleY];
                    double d101 = densities[((cellX + 1) * densityStride + (cellZ + 1)) * verticalSamples + sampleY];
                    double dy000 = (densities[((cellX + 0) * densityStride + (cellZ + 0)) * verticalSamples
                            + sampleY + 1] - d000) * yStep;
                    double dy001 = (densities[((cellX + 0) * densityStride + (cellZ + 1)) * verticalSamples
                            + sampleY + 1] - d001) * yStep;
                    double dy100 = (densities[((cellX + 1) * densityStride + (cellZ + 0)) * verticalSamples
                            + sampleY + 1] - d100) * yStep;
                    double dy101 = (densities[((cellX + 1) * densityStride + (cellZ + 1)) * verticalSamples
                            + sampleY + 1] - d101) * yStep;

                    for (int localYStep = 0; localYStep < 4; localYStep++) {
                        double xStep = 0.125;
                        double x0 = d000;
                        double x1 = d001;
                        double dx0 = (d100 - d000) * xStep;
                        double dx1 = (d101 - d001) * xStep;

                        for (int localXStep = 0; localXStep < 8; localXStep++) {
                            double zStep = 0.125;
                            double density = x0;
                            double dz = (x1 - x0) * zStep;

                            for (int localZStep = 0; localZStep < 8; localZStep++) {
                                int x = cellX * 8 + localXStep;
                                int y = sampleY * 4 + localYStep;
                                int z = cellZ * 8 + localZStep;
                                chunk.setBlock(x, y, z, density > 0.0 ? BlockType.END_STONE : BlockType.AIR);
                                density += dz;
                            }

                            x0 += dx0;
                            x1 += dx1;
                        }

                        d000 += dy000;
                        d001 += dy001;
                        d100 += dy100;
                        d101 += dy101;
                    }
                }
            }
        }
    }

    private double[] endDensities(int startX, int startY, int startZ, int xSize, int ySize, int zSize) {
        double horizontalScale = 684.41200000000003 * 2.0;
        double verticalScale = 684.41200000000003;
        double[] radialNoise = endNoise4.generateNoiseOctaves(null, startX, startZ, xSize, zSize,
                1.121, 1.121, 0.5);
        double[] depthNoise = endNoise5.generateNoiseOctaves(null, startX, startZ, xSize, zSize,
                200.0, 200.0, 0.5);
        double[] selectorNoise = endNoise3.generateNoiseOctaves(null, startX, startY, startZ, xSize, ySize, zSize,
                horizontalScale / 80.0, verticalScale / 160.0, horizontalScale / 80.0);
        double[] minLimitNoise = endNoise1.generateNoiseOctaves(null, startX, startY, startZ, xSize, ySize, zSize,
                horizontalScale, verticalScale, horizontalScale);
        double[] maxLimitNoise = endNoise2.generateNoiseOctaves(null, startX, startY, startZ, xSize, ySize, zSize,
                horizontalScale, verticalScale, horizontalScale);
        double[] densities = new double[xSize * ySize * zSize];

        int noiseIndex = 0;
        int horizontalIndex = 0;
        for (int x = 0; x < xSize; x++) {
            for (int z = 0; z < zSize; z++) {
                double islandShape = (radialNoise[horizontalIndex] + 256.0) / 512.0;
                if (islandShape > 1.0) {
                    islandShape = 1.0;
                }

                double unusedDepth = depthNoise[horizontalIndex] / 8000.0;
                if (unusedDepth < 0.0) {
                    unusedDepth = -unusedDepth * 0.3;
                }
                unusedDepth = unusedDepth * 3.0 - 2.0;

                float radialX = x + startX;
                float radialZ = z + startZ;
                float radialFalloff = 100.0F - (float) Math.sqrt(radialX * radialX + radialZ * radialZ) * 8.0F;
                if (radialFalloff > 80.0F) {
                    radialFalloff = 80.0F;
                }
                if (radialFalloff < -100.0F) {
                    radialFalloff = -100.0F;
                }

                if (unusedDepth > 1.0) {
                    unusedDepth = 1.0;
                }
                unusedDepth = 0.0;
                if (islandShape < 0.0) {
                    islandShape = 0.0;
                }
                islandShape += 0.5;
                double centerY = ySize / 2.0;
                horizontalIndex++;

                for (int y = 0; y < ySize; y++) {
                    double minLimit = minLimitNoise[noiseIndex] / 512.0;
                    double maxLimit = maxLimitNoise[noiseIndex] / 512.0;
                    double selector = (selectorNoise[noiseIndex] / 10.0 + 1.0) / 2.0;
                    double density;
                    if (selector < 0.0) {
                        density = minLimit;
                    } else if (selector > 1.0) {
                        density = maxLimit;
                    } else {
                        density = minLimit + (maxLimit - minLimit) * selector;
                    }

                    density -= 8.0;
                    density += radialFalloff;

                    int topClamp = 2;
                    if (y > ySize / 2 - topClamp) {
                        double fade = (double) (y - (ySize / 2 - topClamp)) / 64.0;
                        fade = Math.max(0.0, Math.min(1.0, fade));
                        density = density * (1.0 - fade) + -3000.0 * fade;
                    }

                    int bottomClamp = 8;
                    if (y < bottomClamp) {
                        double fade = (double) (bottomClamp - y) / (bottomClamp - 1.0);
                        density = density * (1.0 - fade) + -30.0 * fade;
                    }

                    densities[noiseIndex] = density;
                    noiseIndex++;
                }
            }
        }
        return densities;
    }

    private void placeEndSpikes(World world, Chunk chunk, int chunkX, int chunkZ) {
        for (int originChunkX = chunkX - 1; originChunkX <= chunkX; originChunkX++) {
            for (int originChunkZ = chunkZ - 1; originChunkZ <= chunkZ; originChunkZ++) {
                EndSpikeCandidate spike = buildEndSpikeCandidate(originChunkX, originChunkZ);
                if (spike != null && spike.intersectsChunk(chunkX, chunkZ)) {
                    applyEndSpikeCandidate(world, chunk, chunkX, chunkZ, spike);
                }
            }
        }
    }

    private EndSpikeCandidate buildEndSpikeCandidate(int originChunkX, int originChunkZ) {
        Random random = endDecoratorRandom(originChunkX, originChunkZ);
        if (random.nextInt(5) != 0) {
            return null;
        }
        int px = originChunkX * Chunk.WIDTH + random.nextInt(Chunk.WIDTH) + 8;
        int pz = originChunkZ * Chunk.DEPTH + random.nextInt(Chunk.DEPTH) + 8;
        int surfaceY = highestEndStoneAt(px, pz);
        if (surfaceY < 0 || surfaceY >= Chunk.HEIGHT - 1
                || endBaseBlockAt(px, surfaceY + 1, pz) != BlockType.AIR) {
            return null;
        }

        int baseY = surfaceY + 1;
        int height = random.nextInt(32) + 6;
        int radius = random.nextInt(4) + 1;
        int topY = baseY + height;
        if (!endSpikeFootprintValid(px, surfaceY, pz, radius)) {
            return null;
        }
        float crystalYaw = random.nextFloat() * 360.0F;
        return new EndSpikeCandidate(px, baseY, pz, radius, topY, crystalYaw);
    }

    private float endDragonYaw(int chunkX, int chunkZ) {
        Random random = endDecoratorRandom(chunkX, chunkZ);
        if (random.nextInt(5) == 0) {
            int px = chunkX * Chunk.WIDTH + random.nextInt(Chunk.WIDTH) + 8;
            int pz = chunkZ * Chunk.DEPTH + random.nextInt(Chunk.DEPTH) + 8;
            int surfaceY = highestEndStoneAt(px, pz);
            if (surfaceY >= 0 && surfaceY < Chunk.HEIGHT - 1
                    && endBaseBlockAt(px, surfaceY + 1, pz) == BlockType.AIR) {
                random.nextInt(32);
                int radius = random.nextInt(4) + 1;
                if (endSpikeFootprintValid(px, surfaceY, pz, radius)) {
                    random.nextFloat();
                }
            }
        }
        return random.nextFloat() * 360.0F;
    }

    private Random endDecoratorRandom(int chunkX, int chunkZ) {
        Random random = populationRandom(chunkX, chunkZ);
        oreGenerator.advanceFromOrigin(random, chunkX, chunkZ);
        return random;
    }

    private boolean endSpikeFootprintValid(int px, int surfaceY, int pz, int radius) {
        for (int x = px - radius; x <= px + radius; x++) {
            for (int z = pz - radius; z <= pz + radius; z++) {
                int dx = x - px;
                int dz = z - pz;
                if (dx * dx + dz * dz <= radius * radius + 1
                        && endBaseBlockAt(x, surfaceY, z) != BlockType.END_STONE) {
                    return false;
                }
            }
        }
        return true;
    }

    private void applyEndSpikeCandidate(World world, Chunk chunk, int chunkX, int chunkZ,
            EndSpikeCandidate spike) {
        for (int x = spike.centerX() - spike.radius(); x <= spike.centerX() + spike.radius(); x++) {
            for (int z = spike.centerZ() - spike.radius(); z <= spike.centerZ() + spike.radius(); z++) {
                int dx = x - spike.centerX();
                int dz = z - spike.centerZ();
                if (dx * dx + dz * dz > spike.radius() * spike.radius() + 1) {
                    continue;
                }
                for (int y = spike.baseY(); y < spike.topY() && y < Chunk.HEIGHT; y++) {
                    setIfInChunk(chunk, chunkX, chunkZ, x, y, z, BlockType.OBSIDIAN);
                }
            }
        }
        if (world != null && containsBlock(chunkX, chunkZ, spike.centerX(), spike.centerZ())) {
            EndCrystalEntity crystal = new EndCrystalEntity(spike.centerX() + 0.5f, spike.topY(),
                    spike.centerZ() + 0.5f);
            crystal.setYaw(spike.crystalYaw());
            world.stageGeneratedEntity(crystal);
        }
        setIfInChunk(chunk, chunkX, chunkZ, spike.centerX(), spike.topY(), spike.centerZ(), BlockType.BEDROCK);
    }

    private int highestEndStoneAt(int blockX, int blockZ) {
        int chunkX = Math.floorDiv(blockX, Chunk.WIDTH);
        int chunkZ = Math.floorDiv(blockZ, Chunk.DEPTH);
        Chunk chunk = cachedEndBaseChunk(chunkX, chunkZ);
        return highestEndStone(chunk, Math.floorMod(blockX, Chunk.WIDTH), Math.floorMod(blockZ, Chunk.DEPTH));
    }

    private static int highestEndStone(Chunk chunk, int x, int z) {
        for (int y = Chunk.HEIGHT - 2; y >= 1; y--) {
            if (chunk.getBlock(x, y, z) == BlockType.END_STONE) {
                return y;
            }
        }
        return -1;
    }

    private BlockType endBaseBlockAt(int blockX, int y, int blockZ) {
        if (y < 0 || y >= Chunk.HEIGHT) {
            return BlockType.AIR;
        }
        int chunkX = Math.floorDiv(blockX, Chunk.WIDTH);
        int chunkZ = Math.floorDiv(blockZ, Chunk.DEPTH);
        Chunk chunk = cachedEndBaseChunk(chunkX, chunkZ);
        return chunk.getBlock(Math.floorMod(blockX, Chunk.WIDTH), y, Math.floorMod(blockZ, Chunk.DEPTH));
    }

    private Chunk cachedEndBaseChunk(int chunkX, int chunkZ) {
        long key = World.chunkKey(chunkX, chunkZ);
        Chunk cached = endBaseChunkCache.get(key);
        if (cached != null) {
            return cached;
        }
        Chunk generated = new Chunk(chunkX, chunkZ);
        generateEndBaseChunk(generated, chunkX, chunkZ);
        if (endBaseChunkCache.size() > MAX_BASE_CHUNK_CACHE_ENTRIES) {
            endBaseChunkCache.clear();
        }
        Chunk previous = endBaseChunkCache.putIfAbsent(key, generated);
        return previous == null ? generated : previous;
    }

    private record EndSpikeCandidate(int centerX, int baseY, int centerZ, int radius, int topY, float crystalYaw) {
        boolean intersectsChunk(int chunkX, int chunkZ) {
            int minX = chunkX * Chunk.WIDTH;
            int minZ = chunkZ * Chunk.DEPTH;
            int maxX = minX + Chunk.WIDTH - 1;
            int maxZ = minZ + Chunk.DEPTH - 1;
            return centerX + radius >= minX
                    && centerX - radius <= maxX
                    && centerZ + radius >= minZ
                    && centerZ - radius <= maxZ;
        }
    }

    private record StructureBlockDelta(int localX, int y, int localZ, BlockType type, int metadata) {
    }

    private static Chunk copyChunk(Chunk source) {
        Chunk copy = new Chunk(source.getX(), source.getZ());
        copy.loadBlockData(source.copyBlockIds(), source.copyBlockMetadata(), false);
        copy.clearModified();
        return copy;
    }

    private static boolean containsBlock(int chunkX, int chunkZ, int blockX, int blockZ) {
        int minX = chunkX * Chunk.WIDTH;
        int minZ = chunkZ * Chunk.DEPTH;
        return blockX >= minX && blockX < minX + Chunk.WIDTH
                && blockZ >= minZ && blockZ < minZ + Chunk.DEPTH;
    }

    private static void setIfInChunk(Chunk chunk, int chunkX, int chunkZ, int blockX, int y, int blockZ,
            BlockType type) {
        setIfInChunk(chunk, chunkX, chunkZ, blockX, y, blockZ, type, 0);
    }

    private static void setIfInChunk(Chunk chunk, int chunkX, int chunkZ, int blockX, int y, int blockZ,
            BlockType type, int metadata) {
        int localX = blockX - chunkX * Chunk.WIDTH;
        int localZ = blockZ - chunkZ * Chunk.DEPTH;
        if (Chunk.isInBounds(localX, y, localZ)) {
            chunk.setBlock(localX, y, localZ, type, metadata);
        }
    }
}
