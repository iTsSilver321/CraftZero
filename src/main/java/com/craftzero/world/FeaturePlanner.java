package com.craftzero.world;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

final class FeaturePlanner {
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

    private List<TreeFeature.Candidate> rawTreeCandidatesForOrigin(int originChunkX, int originChunkZ) {
        Random random = populationRandom(originChunkX, originChunkZ, 0x54AEEF01L);
        List<TreeFeature.Candidate> candidates = new ArrayList<>();
        int baseX = originChunkX * Chunk.WIDTH;
        int baseZ = originChunkZ * Chunk.DEPTH;
        BiomeType biome = generator.getBiome(baseX + 16, baseZ + 16);
        int attempts = treesPerChunk(biome);
        if (attempts < 0) {
            return candidates;
        }
        if (random.nextInt(10) == 0) {
            attempts++;
        }
        for (int i = 0; i < attempts; i++) {
            int x = baseX + random.nextInt(Chunk.WIDTH) + 8;
            int z = baseZ + random.nextInt(Chunk.DEPTH) + 8;
            int y = generator.terrainTopY(x, z) + 1;
            TreeSpec spec = treeSpecForBiome(biome, random);
            if (spec.kind() == TreeFeature.Kind.SWAMP) {
                while (y > 1 && generator.baseBlockAt(x, y - 1, z).isWater()) {
                    y--;
                }
            }
            int priority = random.nextInt();
            candidates.add(new TreeFeature.Candidate(x, y, z, spec.height(), priority, spec.metadata(), spec.kind(),
                    spec.dataA(), spec.dataB(), spec.dataC(), spec.dataD()));
        }
        return candidates;
    }

    private int treesPerChunk(BiomeType biome) {
        return switch (biome) {
            case FOREST, FOREST_HILLS, TAIGA, TAIGA_HILLS -> 10;
            case SWAMPLAND -> 2;
            case PLAINS, DESERT, DESERT_HILLS, BEACH,
                    MUSHROOM_ISLAND, MUSHROOM_ISLAND_SHORE -> -999;
            default -> 0;
        };
    }

    private TreeSpec treeSpecForBiome(BiomeType biome, Random random) {
        if (biome == BiomeType.TAIGA || biome == BiomeType.TAIGA_HILLS) {
            if (random.nextInt(3) == 0) {
                int height = random.nextInt(5) + 7;
                int leafStart = height - random.nextInt(2) - 3;
                int maxRadius = 1 + random.nextInt(height - leafStart + 1);
                return new TreeSpec(height, 1, TreeFeature.Kind.TAIGA1, leafStart, maxRadius, 0, 0);
            }
            int height = random.nextInt(4) + 6;
            int topOffset = 1 + random.nextInt(2);
            int maxRadius = 2 + random.nextInt(2);
            int initialRadius = random.nextInt(2);
            int trunkShorten = random.nextInt(3);
            return new TreeSpec(height, 1, TreeFeature.Kind.TAIGA2, topOffset, maxRadius, initialRadius, trunkShorten);
        }
        if (biome == BiomeType.FOREST || biome == BiomeType.FOREST_HILLS) {
            if (random.nextInt(5) == 0) {
                return TreeSpec.normal(5 + random.nextInt(3), 2, random);
            }
            if (random.nextInt(10) == 0) {
                return TreeSpec.big(random);
            }
        }
        if (biome == BiomeType.SWAMPLAND) {
            return TreeSpec.swamp(5 + random.nextInt(4), random);
        }
        if (random.nextInt(10) == 0) {
            return TreeSpec.big(random);
        }
        return TreeSpec.normal(4 + random.nextInt(3), 0, random);
    }

    private Random populationRandom(int chunkX, int chunkZ, long salt) {
        long mixed = seed ^ salt;
        mixed ^= (long) chunkX * 341873128712L;
        mixed ^= (long) chunkZ * 132897987541L;
        mixed ^= (mixed >>> 17);
        return new Random(mixed);
    }

    private record TreeSpec(int height, int metadata, TreeFeature.Kind kind, int dataA, int dataB, int dataC,
            int dataD) {
        private static TreeSpec normal(int height, int metadata, Random random) {
            return new TreeSpec(height, metadata, TreeFeature.Kind.NORMAL, normalLeafCornerMask(random), 0, 0, 0);
        }

        private static TreeSpec swamp(int height, Random random) {
            int cornerMask = 0;
            for (int i = 0; i < 16; i++) {
                if (random.nextInt(2) != 0) {
                    cornerMask |= 1 << i;
                }
            }
            return new TreeSpec(height, 0, TreeFeature.Kind.SWAMP, cornerMask, random.nextInt(), random.nextInt(), 0);
        }

        private static TreeSpec big(Random random) {
            long seed = random.nextLong();
            int height = 5 + new Random(seed).nextInt(12);
            return new TreeSpec(height, 0, TreeFeature.Kind.BIG, (int) (seed >>> 32), (int) seed, 0, 0);
        }

        private static int normalLeafCornerMask(Random random) {
            int mask = 0;
            for (int i = 0; i < 16; i++) {
                if (random.nextInt(2) == 0) {
                    mask |= 1 << i;
                }
            }
            return mask;
        }
    }
}
