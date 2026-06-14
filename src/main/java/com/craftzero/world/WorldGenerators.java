package com.craftzero.world;

public final class WorldGenerators {
    private WorldGenerators() {
    }

    public static WorldGenerator create(String generatorId, long seed) {
        return create(generatorId, seed, null);
    }

    public static WorldGenerator create(String generatorId, long seed, Dimension dimension) {
        Dimension effectiveDimension = dimension != null ? dimension : dimensionFromGeneratorId(generatorId);
        if (WorldGenerator.RELEASE_ONE.equals(generatorId) || generatorId == null || generatorId.isBlank()
                || isReleaseDimensionGenerator(generatorId)) {
            return new ReleaseOneWorldGenerator(seed, effectiveDimension);
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
