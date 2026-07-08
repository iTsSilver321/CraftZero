package com.craftzero.world;

import java.util.Random;

/**
 * A deterministic structure piece stored in world coordinates. Pieces place only
 * the portion that intersects the chunk currently being populated, so structures
 * can cross chunk borders without forcing neighbor chunk generation.
 */
public interface StructurePiece {
    StructureBoundingBox bounds();

    void place(World world, Chunk chunk, long seed, int chunkX, int chunkZ);

    default void place(World world, Chunk chunk, long seed, int chunkX, int chunkZ, Random placementRandom) {
        place(world, chunk, seed, chunkX, chunkZ);
    }

    default boolean intersectsChunk(int chunkX, int chunkZ) {
        return bounds().intersectsChunk(chunkX, chunkZ);
    }
}
