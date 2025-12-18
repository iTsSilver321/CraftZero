package com.craftzero.world;

import java.util.Random;

/**
 * Generates large vertical cracks (ravines) in the world.
 * Similar to cave worms but scaled vertically and squashed horizontally.
 */
public class RavineGenerator {

    private static final int SEARCH_RADIUS = 2;
    private static final int RAVINE_CHANCE = 50; // 1 in 50 chunks (rare)
    private static final int SEA_LEVEL = 62;

    public void generate(Chunk chunk, long worldSeed) {
        int chunkX = chunk.getX();
        int chunkZ = chunk.getZ();

        for (int ox = chunkX - SEARCH_RADIUS; ox <= chunkX + SEARCH_RADIUS; ox++) {
            for (int oz = chunkZ - SEARCH_RADIUS; oz <= chunkZ + SEARCH_RADIUS; oz++) {

                long seed = worldSeed + (long) ox * 341873128712L + (long) oz * 132897987541L;
                Random rand = new Random(seed);

                if (rand.nextInt(RAVINE_CHANCE) == 0) {

                    double x = ox * Chunk.WIDTH + rand.nextInt(Chunk.WIDTH);
                    double y = rand.nextInt(20) + 20; // 20-40 (start lowish)
                    double z = oz * Chunk.DEPTH + rand.nextInt(Chunk.DEPTH);

                    float yaw = rand.nextFloat() * (float) Math.PI * 2.0f;
                    float pitch = (rand.nextFloat() - 0.5f) * 0.2f; // Very flat pitch
                    float width = rand.nextFloat() * 3.0f + 3.0f; // 3-6 radius width (narrow)

                    int length = rand.nextInt(50) + 80; // 80-130 blocks long

                    runRavine(chunk, rand, x, y, z, yaw, pitch, width, length);
                }
            }
        }
    }

    private void runRavine(Chunk chunk, Random rand, double x, double y, double z,
            float yaw, float pitch, float width, int length) {

        int chunkX = chunk.getX();
        int chunkZ = chunk.getZ();
        int minX = chunkX * Chunk.WIDTH;
        int maxX = minX + Chunk.WIDTH;
        int minZ = chunkZ * Chunk.DEPTH;
        int maxZ = minZ + Chunk.DEPTH;

        float yScale = 3.0f; // Stretch height by 3x (1 radius width = 3 height)
        // e.g. Width 4 -> Height 12 (Total ravine depth ~24 blocks)

        for (int step = 0; step < length; step++) {
            double hScale = Math.cos(pitch);
            x += Math.cos(yaw) * hScale;
            z += Math.sin(yaw) * hScale;
            y += Math.sin(pitch);

            // Wiggle
            pitch *= 0.7f; // Flatten strongly
            pitch += (rand.nextFloat() - rand.nextFloat()) * 0.05f;
            yaw += (rand.nextFloat() - rand.nextFloat()) * 0.1f; // Slow turns

            // Width varies smoothly
            float currentWidth = width * (1.0f + (float) Math.sin(step * 0.2f) * 0.3f);
            float currentHeight = currentWidth * yScale;

            // Optimization bounds check (extended for height)
            if (x < minX - currentWidth || x > maxX + currentWidth || z < minZ - currentWidth
                    || z > maxZ + currentWidth) {
                continue;
            }

            carveColumn(chunk, (int) x, (int) y, (int) z, currentWidth, currentHeight);
        }
    }

    private void carveColumn(Chunk chunk, int centerX, int centerY, int centerZ, float widthRadius,
            float heightRadius) {
        // Defines an elliptical column: (dx/w)^2 + (dy/h)^2 + (dz/w)^2 < 1

        int wR = (int) Math.ceil(widthRadius);
        int hR = (int) Math.ceil(heightRadius);

        for (int x = centerX - wR; x <= centerX + wR; x++) {
            for (int z = centerZ - wR; z <= centerZ + wR; z++) {

                double dx = x - centerX;
                double dz = z - centerZ;
                double distSqHorizontal = (dx * dx) + (dz * dz);

                // Optimized: Check horizontal first
                if (distSqHorizontal < widthRadius * widthRadius) {

                    for (int y = centerY - hR; y <= centerY + hR; y++) {

                        // Ellipsoid check involving Y
                        // (distH / w)^2 + (dy / h)^2 < 1
                        double dy = y - centerY;
                        double normalizedDist = (distSqHorizontal / (widthRadius * widthRadius)) +
                                ((dy * dy) / (heightRadius * heightRadius));

                        if (normalizedDist < 1.0) {

                            int localX = x - (chunk.getX() * Chunk.WIDTH);
                            int localZ = z - (chunk.getZ() * Chunk.DEPTH);

                            if (localX >= 0 && localX < Chunk.WIDTH &&
                                    localZ >= 0 && localZ < Chunk.DEPTH &&
                                    y >= 0 && y < Chunk.HEIGHT) {

                                BlockType current = chunk.getBlock(localX, y, localZ);
                                if (current == BlockType.WATER || current == BlockType.BEDROCK)
                                    continue;

                                // Ocean check
                                if (y < SEA_LEVEL && y < Chunk.HEIGHT - 1) {
                                    if (chunk.getBlock(localX, y + 1, localZ) == BlockType.WATER)
                                        continue;
                                }

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
}
