package com.craftzero.world;

/**
 * Versioned terrain generator entrypoint. Legacy CraftZero generation is kept
 * as a null generator inside World so old saves do not drift.
 */
public interface WorldGenerator {
    String LEGACY_CRAFTZERO = "craftzero_custom_v1";
    String RELEASE_ONE = "minecraft_java_1_0";

    String getId();

    Dimension getDimension();

    BiomeType getBiome(int blockX, int blockZ);

    void generateChunk(World world, Chunk chunk, int chunkX, int chunkZ);
}
