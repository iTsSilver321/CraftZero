package com.craftzero.world;

final class TreeFeature {
    static final int CROWN_RADIUS = 2;
    static final int MIN_SPACING = 6;
    static final int MIN_SPACING_SQUARED = MIN_SPACING * MIN_SPACING;

    interface BlockQuery {
        BlockType getBlock(int x, int y, int z);
    }

    private TreeFeature() {
    }

    record Candidate(int rootX, int rootY, int rootZ, int height, int priority) {
        boolean intersectsChunk(int chunkX, int chunkZ) {
            int minX = chunkX * Chunk.WIDTH;
            int minZ = chunkZ * Chunk.DEPTH;
            int maxX = minX + Chunk.WIDTH - 1;
            int maxZ = minZ + Chunk.DEPTH - 1;
            return rootX + CROWN_RADIUS >= minX && rootX - CROWN_RADIUS <= maxX
                    && rootZ + CROWN_RADIUS >= minZ && rootZ - CROWN_RADIUS <= maxZ;
        }

        boolean conflictsWith(Candidate other) {
            int dx = rootX - other.rootX;
            int dz = rootZ - other.rootZ;
            return dx * dx + dz * dz < MIN_SPACING_SQUARED;
        }

        boolean canPlace(BlockQuery query) {
            if (rootY <= 1 || rootY + height + 2 >= Chunk.HEIGHT) {
                return false;
            }
            BlockType support = query.getBlock(rootX, rootY - 1, rootZ);
            if (!isTreeSupport(support)) {
                return false;
            }
            for (int y = rootY; y < rootY + height; y++) {
                if (!isReplaceableForTrunk(query.getBlock(rootX, y, rootZ))) {
                    return false;
                }
            }
            for (int y = rootY + height - 2; y <= rootY + height + 1; y++) {
                int radius = y >= rootY + height ? 1 : CROWN_RADIUS;
                for (int dx = -radius; dx <= radius; dx++) {
                    for (int dz = -radius; dz <= radius; dz++) {
                        if (Math.abs(dx) == radius && Math.abs(dz) == radius && y > rootY + height - 1) {
                            continue;
                        }
                        if (!isReplaceableForLeaves(query.getBlock(rootX + dx, y, rootZ + dz))) {
                            return false;
                        }
                    }
                }
            }
            return true;
        }

        void placeInto(Chunk chunk, int chunkX, int chunkZ) {
            for (int y = rootY; y < rootY + height; y++) {
                setIfInChunk(chunk, chunkX, chunkZ, rootX, y, rootZ, BlockType.OAK_LOG, true);
            }
            for (int y = rootY + height - 2; y <= rootY + height + 1; y++) {
                int radius = y >= rootY + height ? 1 : CROWN_RADIUS;
                for (int dx = -radius; dx <= radius; dx++) {
                    for (int dz = -radius; dz <= radius; dz++) {
                        if (Math.abs(dx) == radius && Math.abs(dz) == radius && y > rootY + height - 1) {
                            continue;
                        }
                        setIfInChunk(chunk, chunkX, chunkZ, rootX + dx, y, rootZ + dz, BlockType.LEAVES, false);
                    }
                }
            }
        }
    }

    static boolean isTreeSupport(BlockType block) {
        return block == BlockType.GRASS || block == BlockType.DIRT || block == BlockType.MYCELIUM;
    }

    static boolean isReplaceableForTrunk(BlockType block) {
        return block == BlockType.AIR || block == BlockType.LEAVES || block.isPlant() || block == BlockType.FIRE;
    }

    static boolean isReplaceableForLeaves(BlockType block) {
        return block == BlockType.AIR || block == BlockType.LEAVES || block.isPlant() || block == BlockType.FIRE;
    }

    private static void setIfInChunk(Chunk chunk, int chunkX, int chunkZ, int worldX, int y, int worldZ,
            BlockType type, boolean trunk) {
        if (y < 0 || y >= Chunk.HEIGHT) {
            return;
        }
        int localX = worldX - chunkX * Chunk.WIDTH;
        int localZ = worldZ - chunkZ * Chunk.DEPTH;
        if (!Chunk.isInBounds(localX, y, localZ)) {
            return;
        }
        BlockType current = chunk.getBlock(localX, y, localZ);
        if (trunk) {
            if (isReplaceableForTrunk(current)) {
                chunk.setBlock(localX, y, localZ, type);
            }
        } else if (isReplaceableForLeaves(current)) {
            chunk.setBlock(localX, y, localZ, type);
        }
    }
}
