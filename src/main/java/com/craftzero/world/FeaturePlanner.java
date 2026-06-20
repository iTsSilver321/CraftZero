package com.craftzero.world;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

final class FeaturePlanner {
    private static final int TREE_ATTEMPTS = 8;
    private static final int SMALL_FEATURE_ATTEMPTS = 10;
    private static final int TREE_ACCEPTANCE_RADIUS_CHUNKS = 2;

    private final long seed;
    private final ReleaseOneWorldGenerator generator;

    FeaturePlanner(long seed, ReleaseOneWorldGenerator generator) {
        this.seed = seed;
        this.generator = generator;
    }

    List<TreeFeature.Candidate> acceptedTreesIntersectingChunk(int chunkX, int chunkZ) {
        List<TreeFeature.Candidate> raw = new ArrayList<>();
        for (int ox = chunkX - TREE_ACCEPTANCE_RADIUS_CHUNKS; ox <= chunkX + TREE_ACCEPTANCE_RADIUS_CHUNKS; ox++) {
            for (int oz = chunkZ - TREE_ACCEPTANCE_RADIUS_CHUNKS; oz <= chunkZ + TREE_ACCEPTANCE_RADIUS_CHUNKS; oz++) {
                raw.addAll(rawTreeCandidatesForOrigin(ox, oz));
            }
        }

        raw.sort(Comparator.comparingInt(TreeFeature.Candidate::priority).reversed()
                .thenComparingInt(TreeFeature.Candidate::rootX)
                .thenComparingInt(TreeFeature.Candidate::rootZ));

        List<TreeFeature.Candidate> accepted = new ArrayList<>();
        for (TreeFeature.Candidate candidate : raw) {
            if (!candidate.canPlace(generator::baseBlockAt)) {
                continue;
            }
            boolean blocked = false;
            for (TreeFeature.Candidate previous : accepted) {
                if (candidate.conflictsWith(previous)) {
                    blocked = true;
                    break;
                }
            }
            if (!blocked) {
                accepted.add(candidate);
            }
        }

        List<TreeFeature.Candidate> intersecting = new ArrayList<>();
        for (TreeFeature.Candidate candidate : accepted) {
            if (candidate.intersectsChunk(chunkX, chunkZ)) {
                intersecting.add(candidate);
            }
        }
        intersecting.sort(Comparator.comparingInt(TreeFeature.Candidate::rootX)
                .thenComparingInt(TreeFeature.Candidate::rootZ));
        return List.copyOf(intersecting);
    }

    List<SmallFeature> smallFeaturesForChunk(int chunkX, int chunkZ) {
        List<SmallFeature> features = new ArrayList<>();
        for (SmallFeature feature : rawSmallFeaturesForOrigin(chunkX, chunkZ)) {
            if (feature.intersectsChunk(chunkX, chunkZ)) {
                features.add(feature);
            }
        }
        features.sort(Comparator.comparingInt(SmallFeature::x).thenComparingInt(SmallFeature::z));
        return List.copyOf(features);
    }

    private List<TreeFeature.Candidate> rawTreeCandidatesForOrigin(int originChunkX, int originChunkZ) {
        Random random = populationRandom(originChunkX, originChunkZ, 0x54AEEF01L);
        List<TreeFeature.Candidate> candidates = new ArrayList<>();
        int baseX = originChunkX * Chunk.WIDTH;
        int baseZ = originChunkZ * Chunk.DEPTH;
        for (int i = 0; i < TREE_ATTEMPTS; i++) {
            int x = baseX + random.nextInt(Chunk.WIDTH);
            int z = baseZ + random.nextInt(Chunk.DEPTH);
            BiomeType biome = generator.getBiome(x, z);
            if (!biome.isTreeBiome() || biome == BiomeType.MUSHROOM_ISLAND || biome == BiomeType.MUSHROOM_ISLAND_SHORE) {
                random.nextInt();
                continue;
            }
            if (random.nextInt(treeChanceDivisor(biome)) != 0) {
                continue;
            }
            int y = generator.terrainTopY(x, z) + 1;
            int height = 4 + random.nextInt(3);
            int priority = random.nextInt();
            candidates.add(new TreeFeature.Candidate(x, y, z, height, priority));
        }
        return candidates;
    }

    private List<SmallFeature> rawSmallFeaturesForOrigin(int originChunkX, int originChunkZ) {
        Random random = populationRandom(originChunkX, originChunkZ, 0x233715A7L);
        List<SmallFeature> features = new ArrayList<>();
        int baseX = originChunkX * Chunk.WIDTH;
        int baseZ = originChunkZ * Chunk.DEPTH;
        for (int i = 0; i < SMALL_FEATURE_ATTEMPTS; i++) {
            int x = baseX + random.nextInt(Chunk.WIDTH);
            int z = baseZ + random.nextInt(Chunk.DEPTH);
            int y = generator.terrainTopY(x, z) + 1;
            BiomeType biome = generator.getBiome(x, z);
            BlockType type = chooseSmallFeature(biome, random);
            if (type != null) {
                int height = type == BlockType.CACTUS ? 2 + random.nextInt(2) : 1;
                features.add(new SmallFeature(x, y, z, type, height));
            }
        }
        return features;
    }

    private BlockType chooseSmallFeature(BiomeType biome, Random random) {
        if (biome == BiomeType.DESERT || biome == BiomeType.DESERT_HILLS) {
            return random.nextInt(5) == 0 ? BlockType.CACTUS : null;
        }
        if (biome == BiomeType.MUSHROOM_ISLAND || biome == BiomeType.MUSHROOM_ISLAND_SHORE) {
            return random.nextInt(4) == 0 ? (random.nextBoolean() ? BlockType.BROWN_MUSHROOM : BlockType.RED_MUSHROOM)
                    : null;
        }
        if (biome == BiomeType.SWAMPLAND && random.nextInt(4) == 0) {
            return random.nextBoolean() ? BlockType.BROWN_MUSHROOM : BlockType.RED_MUSHROOM;
        }
        if (biome.isTreeBiome() || biome == BiomeType.PLAINS) {
            return random.nextInt(5) == 0 ? (random.nextBoolean() ? BlockType.YELLOW_FLOWER : BlockType.RED_ROSE) : null;
        }
        return null;
    }

    private int treeChanceDivisor(BiomeType biome) {
        return switch (biome) {
            case FOREST, FOREST_HILLS -> 3;
            case TAIGA, TAIGA_HILLS, SWAMPLAND -> 6;
            default -> 10;
        };
    }

    private Random populationRandom(int chunkX, int chunkZ, long salt) {
        long mixed = seed ^ salt;
        mixed ^= (long) chunkX * 341873128712L;
        mixed ^= (long) chunkZ * 132897987541L;
        mixed ^= (mixed >>> 17);
        return new Random(mixed);
    }

    record SmallFeature(int x, int y, int z, BlockType type, int height) {
        boolean intersectsChunk(int chunkX, int chunkZ) {
            int minX = chunkX * Chunk.WIDTH;
            int minZ = chunkZ * Chunk.DEPTH;
            return x >= minX && x < minX + Chunk.WIDTH && z >= minZ && z < minZ + Chunk.DEPTH;
        }

        void placeInto(Chunk chunk, int chunkX, int chunkZ, TreeFeature.BlockQuery query) {
            int localX = x - chunkX * Chunk.WIDTH;
            int localZ = z - chunkZ * Chunk.DEPTH;
            if (!Chunk.isInBounds(localX, y, localZ)) {
                return;
            }
            BlockType support = query.getBlock(x, y - 1, z);
            if (type == BlockType.CACTUS) {
                if (support != BlockType.SAND) {
                    return;
                }
                for (int i = 0; i < height && y + i < Chunk.HEIGHT; i++) {
                    if (chunk.getBlock(localX, y + i, localZ) != BlockType.AIR) {
                        return;
                    }
                }
                for (int i = 0; i < height && y + i < Chunk.HEIGHT; i++) {
                    chunk.setBlock(localX, y + i, localZ, BlockType.CACTUS);
                }
                return;
            }
            if (!TreeFeature.isTreeSupport(support) && support != BlockType.MYCELIUM) {
                return;
            }
            if (chunk.getBlock(localX, y, localZ) == BlockType.AIR) {
                chunk.setBlock(localX, y, localZ, type);
            }
        }
    }
}
