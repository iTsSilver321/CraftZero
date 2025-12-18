package com.craftzero.world;

import java.util.Random;

/**
 * Handles generating cave tunnels using a "worm" / runner algorithm.
 * Simulates worms starting in nearby chunks to carve continuous tunnels.
 * Enhanced for Minecraft-like generation: "tunnels inside tunnels", surface
 * openings, and branching.
 */
public class CaveGenerator {

    // How far out to check for worm start points
    private static final int SEARCH_RADIUS = 2;

    // Chance a chunk starts a cave system (approx 1 in 10 for denser caves)
    private static final int CAVE_CHANCE = 10;

    // Sea level for ocean check
    private static final int SEA_LEVEL = 62;

    /**
     * Carve caves into the given chunk.
     */
    public void generate(Chunk chunk, long worldSeed) {
        int chunkX = chunk.getX();
        int chunkZ = chunk.getZ();

        // Iterate over nearby chunks to see if a worm starts there and reaches us
        for (int ox = chunkX - SEARCH_RADIUS; ox <= chunkX + SEARCH_RADIUS; ox++) {
            for (int oz = chunkZ - SEARCH_RADIUS; oz <= chunkZ + SEARCH_RADIUS; oz++) {

                // Seed random based on ORIGIN chunk coordinates
                long seed = worldSeed + (long) ox * 341873128712L + (long) oz * 132897987541L;
                Random rand = new Random(seed);

                // Chance to start a cave system here
                if (rand.nextInt(CAVE_CHANCE) == 0) {

                    // "Nodes" or main worms in this system
                    int numWorms = rand.nextInt(3) + 1; // 1 to 3 main tunnel systems

                    for (int i = 0; i < numWorms; i++) {
                        // Start position
                        double x = ox * Chunk.WIDTH + rand.nextInt(Chunk.WIDTH);
                        double z = oz * Chunk.DEPTH + rand.nextInt(Chunk.DEPTH);

                        // Start height logic:
                        // 1. Surface caves (openings) - High up
                        // 2. Deep caves - Low down
                        double y;
                        if (rand.nextInt(4) == 0) {
                            // 25% chance for surface start (attempt to break ground)
                            y = rand.nextInt(40) + 60; // 60-100
                        } else {
                            // 75% chance for deep start
                            y = rand.nextInt(50) + 8; // 8-58
                        }

                        // Initial direction
                        float yaw = rand.nextFloat() * (float) Math.PI * 2.0f;
                        float pitch = (rand.nextFloat() - 0.5f) * 0.5f;

                        // Initial size (vary slightly)
                        float radius = rand.nextFloat() * 1.5f + 1.5f; // 1.5 to 3.0 radius

                        // Length
                        int length = rand.nextInt(60) + 60; // 60-120 blocks

                        // Start with branch depth 0
                        runWorm(chunk, rand, x, y, z, yaw, pitch, length, radius, 0);
                    }
                }
            }
        }
    }

    /**
     * Simulates the worm runner recursively.
     */
    private void runWorm(Chunk targetChunk, Random rand, double x, double y, double z,
            float yaw, float pitch, int length, float radius, int branchDepth) {

        int chunkX = targetChunk.getX();
        int chunkZ = targetChunk.getZ();

        // Bounds check
        int minX = chunkX * Chunk.WIDTH;
        int maxX = minX + Chunk.WIDTH;
        int minZ = chunkZ * Chunk.DEPTH;
        int maxZ = minZ + Chunk.DEPTH;

        for (int step = 0; step < length; step++) {
            // Move
            float horizontalScale = (float) Math.cos(pitch);
            x += Math.cos(yaw) * horizontalScale;
            z += Math.sin(yaw) * horizontalScale;
            y += Math.sin(pitch);

            // Turn and curve
            if (rand.nextBoolean()) {
                pitch *= 0.9f; // Gravity flatten
                pitch += (rand.nextFloat() - rand.nextFloat()) * 0.2f;
                yaw += (rand.nextFloat() - rand.nextFloat()) * 0.4f;
            }

            // Branching Logic ("Tunnels inside tunnels")
            // Chance to spawn a new branch at this node
            // Only branch if we haven't gone too deep (limit 1-2 levels)
            // And mostly in the middle of the tunnel life
            if (branchDepth < 1 && step > length / 4 && step < length * 3 / 4 && rand.nextInt(20) == 0) {
                // 5% chance per step in middle of worm to branch
                float branchYaw = yaw + (rand.nextFloat() * 2f - 1f) * 1.5f; // Sharp turn
                float branchPitch = (rand.nextFloat() - 0.5f) * 0.5f;
                float branchRadius = radius * (0.8f + rand.nextFloat() * 0.4f);
                int branchLength = rand.nextInt(length / 2) + length / 2;

                // Recurse!
                runWorm(targetChunk, rand, x, y, z, branchYaw, branchPitch, branchLength, branchRadius,
                        branchDepth + 1);
            }

            // Radius variation (rooms)
            float stepRadius = radius * (1.0f + (float) Math.sin(step * 0.1f) * 0.3f);

            // Optimization: rough bounds check
            if (x < minX - stepRadius || x > maxX + stepRadius || z < minZ - stepRadius || z > maxZ + stepRadius) {
                continue;
            }

            carveSphere(targetChunk, (int) x, (int) y, (int) z, stepRadius);
        }
    }

    private void carveSphere(Chunk chunk, int centerX, int centerY, int centerZ, float radius) {
        int r = (int) Math.ceil(radius);
        float radiusSq = radius * radius;

        for (int x = centerX - r; x <= centerX + r; x++) {
            for (int y = centerY - r; y <= centerY + r; y++) {
                for (int z = centerZ - r; z <= centerZ + r; z++) {

                    double dx = x - centerX;
                    double dy = y - centerY;
                    double dz = z - centerZ;

                    if (dx * dx + dy * dy + dz * dz < radiusSq) {
                        int localX = x - (chunk.getX() * Chunk.WIDTH);
                        int localZ = z - (chunk.getZ() * Chunk.DEPTH);

                        if (localX >= 0 && localX < Chunk.WIDTH &&
                                localZ >= 0 && localZ < Chunk.DEPTH &&
                                y >= 0 && y < Chunk.HEIGHT) {

                            BlockType current = chunk.getBlock(localX, y, localZ);

                            // 1. Don't carve water or bedrock
                            if (current == BlockType.WATER || current == BlockType.BEDROCK) {
                                continue;
                            }

                            // 2. OCEAN PUNCTURE SAFETY:
                            // If we are below sea level, check if the block ABOVE is water.
                            // If it is, DO NOT BREAK THIS BLOCK, or the ocean will leak in.
                            if (y < SEA_LEVEL) {
                                if (y < Chunk.HEIGHT - 1) {
                                    BlockType above = chunk.getBlock(localX, y + 1, localZ);
                                    if (above == BlockType.WATER) {
                                        continue;
                                    }
                                }
                            }

                            // Lava Pools at depth
                            if (y < 11) {
                                chunk.setBlock(localX, y, localZ, BlockType.LAVA);
                            } else {
                                if (current != BlockType.AIR && current != BlockType.LAVA) {
                                    chunk.setBlock(localX, y, localZ, BlockType.AIR);
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
