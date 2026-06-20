package com.craftzero.world;

/**
 * Inclusive world-space bounding box for chunk-safe structure placement.
 */
public record StructureBoundingBox(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
    public StructureBoundingBox {
        if (minX > maxX || minY > maxY || minZ > maxZ) {
            throw new IllegalArgumentException("Invalid structure bounds");
        }
    }

    public boolean intersects(StructureBoundingBox other) {
        return other != null
                && maxX >= other.minX && minX <= other.maxX
                && maxY >= other.minY && minY <= other.maxY
                && maxZ >= other.minZ && minZ <= other.maxZ;
    }

    public boolean intersectsChunk(int chunkX, int chunkZ) {
        int chunkMinX = chunkX * Chunk.WIDTH;
        int chunkMinZ = chunkZ * Chunk.DEPTH;
        int chunkMaxX = chunkMinX + Chunk.WIDTH - 1;
        int chunkMaxZ = chunkMinZ + Chunk.DEPTH - 1;
        return maxX >= chunkMinX && minX <= chunkMaxX
                && maxZ >= chunkMinZ && minZ <= chunkMaxZ;
    }

    public boolean contains(int x, int y, int z) {
        return x >= minX && x <= maxX
                && y >= minY && y <= maxY
                && z >= minZ && z <= maxZ;
    }

    public int centerX() {
        return (minX + maxX) / 2;
    }

    public int centerY() {
        return (minY + maxY) / 2;
    }

    public int centerZ() {
        return (minZ + maxZ) / 2;
    }

    public static StructureBoundingBox union(Iterable<? extends StructurePiece> pieces) {
        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxY = Integer.MIN_VALUE;
        int maxZ = Integer.MIN_VALUE;
        boolean any = false;
        for (StructurePiece piece : pieces) {
            if (piece == null || piece.bounds() == null) {
                continue;
            }
            StructureBoundingBox box = piece.bounds();
            minX = Math.min(minX, box.minX());
            minY = Math.min(minY, box.minY());
            minZ = Math.min(minZ, box.minZ());
            maxX = Math.max(maxX, box.maxX());
            maxY = Math.max(maxY, box.maxY());
            maxZ = Math.max(maxZ, box.maxZ());
            any = true;
        }
        return any ? new StructureBoundingBox(minX, minY, minZ, maxX, maxY, maxZ)
                : new StructureBoundingBox(0, 0, 0, 0, 0, 0);
    }
}
