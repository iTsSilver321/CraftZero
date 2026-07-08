package com.craftzero.world;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Release 1.0-style biome layer stack.
 *
 * The old generator builds biomes through seeded continent/island, zoom, river,
 * mushroom-shore, and Voronoi layers. This keeps the same broad structure without
 * relying on modern multi-noise biome placement.
 */
final class ReleaseOneBiomeSource {
    private static final int MAX_CACHE_ENTRIES = 262_144;
    private static final BiomeType[] BY_ID = new BiomeType[21];

    static {
        for (BiomeType biome : BiomeType.values()) {
            BY_ID[biome.getId()] = biome;
        }
    }

    private final Object layerLock = new Object();
    private final Layer generationLayer;
    private final Layer finalLayer;
    private final Layer temperatureLayer;
    private final ConcurrentHashMap<Long, BiomeType> cache = new ConcurrentHashMap<>();

    ReleaseOneBiomeSource(long seed) {
        Layer continents = new IslandLayer(1L);
        continents = new FuzzyZoomLayer(2000L, continents);
        continents = new AddIslandLayer(1L, continents);
        continents = new ZoomLayer(2001L, continents);
        continents = new AddIslandLayer(2L, continents);
        continents = new AddSnowLayer(2L, continents);
        continents = new ZoomLayer(2002L, continents);
        continents = new AddIslandLayer(3L, continents);
        continents = new ZoomLayer(2003L, continents);
        continents = new AddIslandLayer(4L, continents);
        continents = new AddMushroomIslandLayer(5L, continents);

        Layer biomes = new BiomeLayer(200L, continents);
        Layer river = new RiverInitLayer(100L, continents);
        for (int i = 0; i < 6; i++) {
            river = new ZoomLayer(1000L + i, river);
        }
        river = new RiverLayer(1L, river);
        river = new SmoothLayer(1000L, river);

        for (int i = 0; i < 2; i++) {
            biomes = new ZoomLayer(1000L + i, biomes);
        }
        Layer temperature = new TemperatureLayer(biomes);
        for (int i = 0; i < 4; i++) {
            biomes = new ZoomLayer(1000L + i, biomes);
            if (i == 0) {
                biomes = new AddIslandLayer(3L, biomes);
                biomes = new MushroomShoreLayer(1000L, biomes);
            }
            temperature = new SmoothZoomLayer(1000L + i, temperature);
            temperature = new TemperatureMixLayer(temperature, biomes, i);
        }
        biomes = new SmoothLayer(1000L, biomes);

        Layer full = new RiverMixLayer(100L, biomes, river);
        for (int i = 0; i < 2; i++) {
            temperature = new SmoothZoomLayer(1000L + i, temperature);
        }
        Layer voronoi = new VoronoiLayer(10L, full);
        voronoi.initWorldSeed(seed);
        temperature.initWorldSeed(seed);
        this.generationLayer = full;
        this.finalLayer = voronoi;
        this.temperatureLayer = temperature;
    }

    BiomeType getBiome(int blockX, int blockZ) {
        long key = (((long) blockX) << 32) ^ (blockZ & 0xFFFFFFFFL);
        BiomeType cached = cache.get(key);
        if (cached != null) {
            return cached;
        }
        BiomeType computed;
        synchronized (layerLock) {
            computed = biomeById(finalLayer.getCached(blockX, blockZ));
        }
        clearIfOversized(cache);
        BiomeType previous = cache.putIfAbsent(key, computed);
        return previous == null ? computed : previous;
    }

    BiomeType getBiomeForGenerationLayer(int layerX, int layerZ) {
        synchronized (layerLock) {
            return biomeById(generationLayer.getCached(layerX, layerZ));
        }
    }

    float getTemperature(int blockX, int blockZ) {
        synchronized (layerLock) {
            return Math.min(1.0F, temperatureLayer.getCached(blockX, blockZ) / 65536.0F);
        }
    }

    private static BiomeType biomeById(int id) {
        return id >= 0 && id < BY_ID.length && BY_ID[id] != null ? BY_ID[id] : BiomeType.PLAINS;
    }

    private static int sourceTemperatureInt(int biomeId) {
        return (int) (sourceTemperature(biomeById(biomeId)) * 65536.0F);
    }

    private static float sourceTemperature(BiomeType biome) {
        return switch (biome) {
            case FROZEN_OCEAN, FROZEN_RIVER, ICE_PLAINS, ICE_MOUNTAINS -> 0.0F;
            case EXTREME_HILLS, EXTREME_HILLS_EDGE -> 0.2F;
            case TAIGA, TAIGA_HILLS -> 0.3F;
            case FOREST, FOREST_HILLS -> 0.7F;
            case PLAINS, SWAMPLAND, BEACH -> 0.8F;
            case MUSHROOM_ISLAND, MUSHROOM_ISLAND_SHORE -> 0.9F;
            case DESERT, DESERT_HILLS, HELL -> 2.0F;
            default -> 0.5F;
        };
    }

    private static void clearIfOversized(ConcurrentHashMap<?, ?> cache) {
        if (cache.size() > MAX_CACHE_ENTRIES) {
            cache.clear();
        }
    }

    private abstract static class Layer {
        private static final long MULTIPLIER = 6364136223846793005L;
        private static final long ADDEND = 1442695040888963407L;

        final Layer parent;
        private final long baseSeed;
        private long worldSeed;
        private long chunkSeed;
        private final ConcurrentHashMap<Long, Integer> cache = new ConcurrentHashMap<>();

        Layer(long salt, Layer parent) {
            this.parent = parent;
            long seed = salt;
            seed = mix(seed, salt);
            seed = mix(seed, salt);
            seed = mix(seed, salt);
            this.baseSeed = seed;
        }

        void initWorldSeed(long seed) {
            if (parent != null) {
                parent.initWorldSeed(seed);
            }
            worldSeed = seed;
            worldSeed = mix(worldSeed, baseSeed);
            worldSeed = mix(worldSeed, baseSeed);
            worldSeed = mix(worldSeed, baseSeed);
        }

        void initChunkSeed(long x, long z) {
            chunkSeed = worldSeed;
            chunkSeed = mix(chunkSeed, x);
            chunkSeed = mix(chunkSeed, z);
            chunkSeed = mix(chunkSeed, x);
            chunkSeed = mix(chunkSeed, z);
        }

        int nextInt(int bound) {
            int value = (int) ((chunkSeed >> 24) % bound);
            if (value < 0) {
                value += bound;
            }
            chunkSeed = mix(chunkSeed, worldSeed);
            return value;
        }

        int getCached(int x, int z) {
            long key = (((long) x) << 32) ^ (z & 0xFFFFFFFFL);
            Integer cached = cache.get(key);
            if (cached != null) {
                return cached;
            }
            int computed = get(x, z);
            clearIfOversized(cache);
            Integer previous = cache.putIfAbsent(key, computed);
            return previous == null ? computed : previous;
        }

        abstract int get(int x, int z);

        private static long mix(long seed, long salt) {
            return seed * (seed * MULTIPLIER + ADDEND) + salt;
        }
    }

    private static final class IslandLayer extends Layer {
        IslandLayer(long salt) {
            super(salt, null);
        }

        @Override
        int get(int x, int z) {
            initChunkSeed(x, z);
            if (x == 0 && z == 0) {
                return BiomeType.PLAINS.getId();
            }
            return nextInt(10) == 0 ? BiomeType.PLAINS.getId() : BiomeType.OCEAN.getId();
        }
    }

    private static class ZoomLayer extends Layer {
        ZoomLayer(long salt, Layer parent) {
            super(salt, parent);
        }

        @Override
        int get(int x, int z) {
            int parentX = Math.floorDiv(x, 2);
            int parentZ = Math.floorDiv(z, 2);
            int localX = Math.floorMod(x, 2);
            int localZ = Math.floorMod(z, 2);
            int a = parent.getCached(parentX, parentZ);
            if (localX == 0 && localZ == 0) {
                return a;
            }
            int b = parent.getCached(parentX + 1, parentZ);
            int c = parent.getCached(parentX, parentZ + 1);
            int d = parent.getCached(parentX + 1, parentZ + 1);
            initChunkSeed(parentX << 1, parentZ << 1);
            if (localX == 1 && localZ == 0) {
                return choose(a, b);
            }
            if (localX == 0) {
                return choose(a, c);
            }
            return modeOrRandom(a, b, c, d);
        }

        int choose(int a, int b) {
            return nextInt(2) == 0 ? a : b;
        }

        int modeOrRandom(int a, int b, int c, int d) {
            if (b == c && c == d) {
                return b;
            }
            if (a == b && a == c) {
                return a;
            }
            if (a == b && a == d) {
                return a;
            }
            if (a == c && a == d) {
                return a;
            }
            if (a == b && c != d) {
                return a;
            }
            if (a == c && b != d) {
                return a;
            }
            if (a == d && b != c) {
                return a;
            }
            if (b == a && c != d) {
                return b;
            }
            if (b == c && a != d) {
                return b;
            }
            if (b == d && a != c) {
                return b;
            }
            if (c == a && b != d) {
                return c;
            }
            if (c == b && a != d) {
                return c;
            }
            if (c == d && a != b) {
                return c;
            }
            if (d == a && b != c) {
                return c;
            }
            if (d == b && a != c) {
                return c;
            }
            if (d == c && a != b) {
                return c;
            }
            return switch (nextInt(4)) {
                case 0 -> a;
                case 1 -> b;
                case 2 -> c;
                default -> d;
            };
        }
    }

    private static final class FuzzyZoomLayer extends ZoomLayer {
        FuzzyZoomLayer(long salt, Layer parent) {
            super(salt, parent);
        }

        @Override
        int modeOrRandom(int a, int b, int c, int d) {
            return switch (nextInt(4)) {
                case 0 -> a;
                case 1 -> b;
                case 2 -> c;
                default -> d;
            };
        }
    }

    private static final class SmoothZoomLayer extends Layer {
        SmoothZoomLayer(long salt, Layer parent) {
            super(salt, parent);
        }

        @Override
        int get(int x, int z) {
            int parentX = Math.floorDiv(x, 2);
            int parentZ = Math.floorDiv(z, 2);
            int localX = Math.floorMod(x, 2);
            int localZ = Math.floorMod(z, 2);
            int northwest = parent.getCached(parentX, parentZ);
            if (localX == 0 && localZ == 0) {
                return northwest;
            }

            int southwest = parent.getCached(parentX, parentZ + 1);
            int northeast = parent.getCached(parentX + 1, parentZ);
            initChunkSeed(parentX << 1, parentZ << 1);
            int vertical = lerp(northwest, southwest, nextInt(256));
            if (localX == 0) {
                return vertical;
            }
            int horizontal = lerp(northwest, northeast, nextInt(256));
            if (localZ == 0) {
                return horizontal;
            }

            int southeast = parent.getCached(parentX + 1, parentZ + 1);
            int top = lerp(northwest, northeast, nextInt(256));
            int bottom = lerp(southwest, southeast, nextInt(256));
            return lerp(top, bottom, nextInt(256));
        }

        private static int lerp(int a, int b, int weight) {
            return a + ((b - a) * weight) / 256;
        }
    }

    private static final class AddIslandLayer extends Layer {
        AddIslandLayer(long salt, Layer parent) {
            super(salt, parent);
        }

        @Override
        int get(int x, int z) {
            int center = parent.getCached(x, z);
            int northWest = parent.getCached(x - 1, z - 1);
            int northEast = parent.getCached(x + 1, z - 1);
            int southWest = parent.getCached(x - 1, z + 1);
            int southEast = parent.getCached(x + 1, z + 1);
            initChunkSeed(x, z);

            if (isOcean(center)
                    && (!isOcean(northWest) || !isOcean(northEast)
                            || !isOcean(southWest) || !isOcean(southEast))) {
                int candidate = chooseLand(northWest, northEast, southWest, southEast);
                if (nextInt(3) == 0) {
                    return candidate;
                }
                return candidate == BiomeType.ICE_PLAINS.getId()
                        ? BiomeType.FROZEN_OCEAN.getId()
                        : BiomeType.OCEAN.getId();
            }
            if (!isOcean(center)
                    && (isOcean(northWest) || isOcean(northEast)
                            || isOcean(southWest) || isOcean(southEast))) {
                if (nextInt(5) == 0) {
                    return center == BiomeType.ICE_PLAINS.getId()
                            ? BiomeType.FROZEN_OCEAN.getId()
                            : BiomeType.OCEAN.getId();
                }
                return center;
            }
            return center;
        }

        private int chooseLand(int northWest, int northEast, int southWest, int southEast) {
            int selected = BiomeType.PLAINS.getId();
            int choices = 1;
            if (!isOcean(northWest) && nextInt(choices++) == 0) {
                selected = northWest;
            }
            if (!isOcean(northEast) && nextInt(choices++) == 0) {
                selected = northEast;
            }
            if (!isOcean(southWest) && nextInt(choices++) == 0) {
                selected = southWest;
            }
            if (!isOcean(southEast) && nextInt(choices++) == 0) {
                selected = southEast;
            }
            return selected;
        }
    }

    private static final class AddSnowLayer extends Layer {
        AddSnowLayer(long salt, Layer parent) {
            super(salt, parent);
        }

        @Override
        int get(int x, int z) {
            int center = parent.getCached(x, z);
            if (isOcean(center)) {
                return center;
            }
            initChunkSeed(x, z);
            return nextInt(5) == 0 ? BiomeType.ICE_PLAINS.getId() : BiomeType.PLAINS.getId();
        }
    }

    private static final class AddMushroomIslandLayer extends Layer {
        AddMushroomIslandLayer(long salt, Layer parent) {
            super(salt, parent);
        }

        @Override
        int get(int x, int z) {
            int center = parent.getCached(x, z);
            if (!isOcean(center)) {
                return center;
            }
            if (!isOcean(parent.getCached(x - 1, z - 1))
                    || !isOcean(parent.getCached(x + 1, z - 1))
                    || !isOcean(parent.getCached(x - 1, z + 1))
                    || !isOcean(parent.getCached(x + 1, z + 1))) {
                return center;
            }
            initChunkSeed(x, z);
            return nextInt(100) == 0 ? BiomeType.MUSHROOM_ISLAND.getId() : center;
        }
    }

    private static final class BiomeLayer extends Layer {
        private static final int[] WARM_BIOMES = {
                BiomeType.DESERT.getId(),
                BiomeType.FOREST.getId(),
                BiomeType.EXTREME_HILLS.getId(),
                BiomeType.SWAMPLAND.getId(),
                BiomeType.PLAINS.getId(),
                BiomeType.TAIGA.getId()
        };

        BiomeLayer(long salt, Layer parent) {
            super(salt, parent);
        }

        @Override
        int get(int x, int z) {
            int center = parent.getCached(x, z);
            if (isOcean(center) || center == BiomeType.MUSHROOM_ISLAND.getId()) {
                return center;
            }
            initChunkSeed(x, z);
            return center == BiomeType.PLAINS.getId()
                    ? WARM_BIOMES[nextInt(WARM_BIOMES.length)]
                    : BiomeType.ICE_PLAINS.getId();
        }
    }

    private static final class TemperatureLayer extends Layer {
        TemperatureLayer(Layer parent) {
            super(0L, parent);
        }

        @Override
        int get(int x, int z) {
            return sourceTemperatureInt(parent.getCached(x, z));
        }
    }

    private static final class TemperatureMixLayer extends Layer {
        private final Layer temperature;
        private final int mixIndex;

        TemperatureMixLayer(Layer temperature, Layer biomeParent, int mixIndex) {
            super(0L, biomeParent);
            this.temperature = temperature;
            this.mixIndex = mixIndex;
        }

        @Override
        void initWorldSeed(long seed) {
            super.initWorldSeed(seed);
            temperature.initWorldSeed(seed);
        }

        @Override
        int get(int x, int z) {
            int previous = temperature.getCached(x, z);
            int target = sourceTemperatureInt(parent.getCached(x, z));
            return previous + (target - previous) / (mixIndex * 2 + 1);
        }
    }

    private static final class MushroomShoreLayer extends Layer {
        MushroomShoreLayer(long salt, Layer parent) {
            super(salt, parent);
        }

        @Override
        int get(int x, int z) {
            int biome = parent.getCached(x, z);
            if (biome == BiomeType.MUSHROOM_ISLAND.getId()) {
                int north = parent.getCached(x, z - 1);
                int east = parent.getCached(x + 1, z);
                int west = parent.getCached(x - 1, z);
                int south = parent.getCached(x, z + 1);
                return north == BiomeType.OCEAN.getId() || east == BiomeType.OCEAN.getId()
                        || west == BiomeType.OCEAN.getId() || south == BiomeType.OCEAN.getId()
                        ? BiomeType.MUSHROOM_ISLAND_SHORE.getId()
                        : biome;
            }
            return biome;
        }
    }

    private static final class RiverInitLayer extends Layer {
        RiverInitLayer(long salt, Layer parent) {
            super(salt, parent);
        }

        @Override
        int get(int x, int z) {
            int center = parent.getCached(x, z);
            initChunkSeed(x, z);
            return center <= 0 ? 0 : nextInt(2) + 2;
        }
    }

    private static final class RiverLayer extends Layer {
        RiverLayer(long salt, Layer parent) {
            super(salt, parent);
        }

        @Override
        int get(int x, int z) {
            int center = parent.getCached(x, z);
            int north = parent.getCached(x, z - 1);
            int east = parent.getCached(x + 1, z);
            int south = parent.getCached(x, z + 1);
            int west = parent.getCached(x - 1, z);
            if (center == 0 || north == 0 || east == 0 || south == 0 || west == 0) {
                return BiomeType.RIVER.getId();
            }
            return center != north || center != east || center != south || center != west
                    ? BiomeType.RIVER.getId()
                    : -1;
        }
    }

    private static final class SmoothLayer extends Layer {
        SmoothLayer(long salt, Layer parent) {
            super(salt, parent);
        }

        @Override
        int get(int x, int z) {
            int center = parent.getCached(x, z);
            int north = parent.getCached(x, z - 1);
            int east = parent.getCached(x + 1, z);
            int south = parent.getCached(x, z + 1);
            int west = parent.getCached(x - 1, z);
            if (north == south && east == west) {
                initChunkSeed(x, z);
                return nextInt(2) == 0 ? north : east;
            }
            if (north == south) {
                return north;
            }
            if (east == west) {
                return east;
            }
            return center;
        }
    }

    private static final class RiverMixLayer extends Layer {
        private final Layer riverLayer;

        RiverMixLayer(long salt, Layer biomeLayer, Layer riverLayer) {
            super(salt, biomeLayer);
            this.riverLayer = riverLayer;
        }

        @Override
        void initWorldSeed(long seed) {
            super.initWorldSeed(seed);
            riverLayer.initWorldSeed(seed);
        }

        @Override
        int get(int x, int z) {
            int biome = parent.getCached(x, z);
            int river = riverLayer.getCached(x, z);
            if (biome == BiomeType.OCEAN.getId() || river < 0) {
                return biome;
            }
            if (biome == BiomeType.ICE_PLAINS.getId()) {
                return BiomeType.FROZEN_RIVER.getId();
            }
            if (biome == BiomeType.MUSHROOM_ISLAND.getId() || biome == BiomeType.MUSHROOM_ISLAND_SHORE.getId()) {
                return BiomeType.MUSHROOM_ISLAND_SHORE.getId();
            }
            return BiomeType.RIVER.getId();
        }
    }

    private static final class VoronoiLayer extends Layer {
        VoronoiLayer(long salt, Layer parent) {
            super(salt, parent);
        }

        @Override
        int get(int x, int z) {
            int adjustedX = x - 2;
            int adjustedZ = z - 2;
            int cellX = Math.floorDiv(adjustedX, 4);
            int cellZ = Math.floorDiv(adjustedZ, 4);
            double localX = Math.floorMod(adjustedX, 4) / 4.0;
            double localZ = Math.floorMod(adjustedZ, 4) / 4.0;

            double d00 = distance(cellX, cellZ, localX, localZ, 0.0, 0.0);
            double d10 = distance(cellX + 1, cellZ, localX, localZ, 1.0, 0.0);
            double d01 = distance(cellX, cellZ + 1, localX, localZ, 0.0, 1.0);
            double d11 = distance(cellX + 1, cellZ + 1, localX, localZ, 1.0, 1.0);
            if (d00 < d10 && d00 < d01 && d00 < d11) {
                return parent.getCached(cellX, cellZ);
            }
            if (d10 < d00 && d10 < d01 && d10 < d11) {
                return parent.getCached(cellX + 1, cellZ);
            }
            if (d01 < d00 && d01 < d10 && d01 < d11) {
                return parent.getCached(cellX, cellZ + 1);
            }
            return parent.getCached(cellX + 1, cellZ + 1);
        }

        private double distance(int cellX, int cellZ, double localX, double localZ, double baseX, double baseZ) {
            initChunkSeed(cellX << 2, cellZ << 2);
            double jitterX = (nextInt(1024) / 1024.0 - 0.5) * 0.9;
            double jitterZ = (nextInt(1024) / 1024.0 - 0.5) * 0.9;
            double dx = localX - baseX + jitterX;
            double dz = localZ - baseZ + jitterZ;
            return dx * dx + dz * dz;
        }
    }

    private static boolean isOcean(int id) {
        return id == BiomeType.OCEAN.getId();
    }
}
