package com.craftzero.world;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class StructureStart {
    private final StructureType type;
    private final int chunkX;
    private final int chunkZ;
    private final List<StructurePiece> pieces = new ArrayList<>();
    private StructureBoundingBox bounds;

    public StructureStart(StructureType type, int chunkX, int chunkZ) {
        this.type = type;
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
    }

    public void addPiece(StructurePiece piece) {
        if (piece != null) {
            pieces.add(piece);
            bounds = null;
        }
    }

    public void place(World world, Chunk chunk, long seed, int targetChunkX, int targetChunkZ) {
        for (StructurePiece piece : pieces) {
            if (piece.intersectsChunk(targetChunkX, targetChunkZ)) {
                piece.place(world, chunk, seed, targetChunkX, targetChunkZ);
            }
        }
    }

    public boolean intersectsChunk(int targetChunkX, int targetChunkZ) {
        return bounds().intersectsChunk(targetChunkX, targetChunkZ);
    }

    public StructureBoundingBox bounds() {
        if (bounds == null) {
            bounds = StructureBoundingBox.union(pieces);
        }
        return bounds;
    }

    public StructureType type() {
        return type;
    }

    public int chunkX() {
        return chunkX;
    }

    public int chunkZ() {
        return chunkZ;
    }

    public List<StructurePiece> pieces() {
        return Collections.unmodifiableList(pieces);
    }
}
