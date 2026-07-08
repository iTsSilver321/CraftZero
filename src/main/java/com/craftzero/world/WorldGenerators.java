package com.craftzero.world;

public final class WorldGenerators {
    private WorldGenerators() {
    }

    public static WorldGenerator create(String generatorId, long seed) {
        return create(generatorId, seed, null, true);
    }

    public static WorldGenerator create(String generatorId, long seed, Dimension dimension) {
        return create(generatorId, seed, dimension, true);
    }

    public static WorldGenerator create(String generatorId, long seed, Dimension dimension,
            boolean generateStructures) {
        Dimension effectiveDimension = isReleaseDimensionGenerator(generatorId)
                ? dimensionFromGeneratorId(generatorId)
                : dimension != null ? dimension : Dimension.OVERWORLD;
        if (WorldGenerator.RELEASE_ONE.equals(generatorId) || generatorId == null || generatorId.isBlank()
                || isReleaseDimensionGenerator(generatorId)) {
            return new ReleaseOneWorldGenerator(seed, effectiveDimension, generateStructures);
        }
        return null;
    }

    public static String generatorIdFor(Dimension dimension) {
        return switch (dimension == null ? Dimension.OVERWORLD : dimension) {
            case OVERWORLD -> WorldGenerator.RELEASE_ONE;
            case NETHER -> "minecraft_java_1_0_nether";
            case THE_END -> "minecraft_java_1_0_end";
        };
    }

    public static boolean isSupportedGeneratorId(String generatorId) {
        return WorldGenerator.RELEASE_ONE.equals(generatorId)
                || WorldGenerator.LEGACY_CRAFTZERO.equals(generatorId)
                || isReleaseDimensionGenerator(generatorId);
    }

    private static boolean isReleaseDimensionGenerator(String generatorId) {
        return "minecraft_java_1_0_nether".equals(generatorId) || "minecraft_java_1_0_end".equals(generatorId);
    }

    private static Dimension dimensionFromGeneratorId(String generatorId) {
        if ("minecraft_java_1_0_nether".equals(generatorId)) {
            return Dimension.NETHER;
        }
        if ("minecraft_java_1_0_end".equals(generatorId)) {
            return Dimension.THE_END;
        }
        return Dimension.OVERWORLD;
    }
}
