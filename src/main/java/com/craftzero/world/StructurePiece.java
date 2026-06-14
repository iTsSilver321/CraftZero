package com.craftzero.world;

public interface StructurePiece {
    void place(World world, Chunk chunk, long seed, int chunkX, int chunkZ);
}
