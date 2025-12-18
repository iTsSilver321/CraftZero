package com.craftzero.world;

import java.util.Random;

/**
 * Generates ores in the world using standard Minecraft Beta 1.8 / Release 1.0
 * logic.
 * Implements the "WorldGenMinable" vein algorithm (oblate spheroid along a
 * line).
 */
public class OreGenerator {

    public void generate(Chunk chunk, long seed) {
        long oreSeed = seed + (long) chunk.getX() * 341873128712L + (long) chunk.getZ() * 132897987541L;
        Random rand = new Random(oreSeed);

        // A. Dirt & Gravel
        generateOre(chunk, rand, BlockType.DIRT, 20, 32, 0, 128);
        generateOre(chunk, rand, BlockType.GRAVEL, 10, 32, 0, 128);

        // B. Coal Ore
        generateOre(chunk, rand, BlockType.COAL_ORE, 20, 16, 0, 128);

        // C. Iron Ore
        generateOre(chunk, rand, BlockType.IRON_ORE, 20, 8, 0, 64);

        // D. Gold Ore
        generateOre(chunk, rand, BlockType.GOLD_ORE, 2, 8, 0, 32);

        // E. Redstone Ore
        generateOre(chunk, rand, BlockType.REDSTONE_ORE, 8, 7, 0, 16);

        // F. Diamond Ore
        generateOre(chunk, rand, BlockType.DIAMOND_ORE, 1, 7, 0, 16);

        // G. Lapis Lazuli (Bell Curve)
        for (int i = 0; i < 1; i++) {
            int x = rand.nextInt(Chunk.WIDTH);
            int z = rand.nextInt(Chunk.DEPTH);
            // Bell curve centered around 16
            int y = rand.nextInt(16) + rand.nextInt(16);
            generateVein(chunk, rand, x, y, z, BlockType.LAPIS_ORE, 6);
        }
    }

    private void generateOre(Chunk chunk, Random rand, BlockType ore, int tries, int size, int minY, int maxY) {
        for (int i = 0; i < tries; i++) {
            int x = rand.nextInt(Chunk.WIDTH);
            int z = rand.nextInt(Chunk.DEPTH);
            int y = rand.nextInt(maxY - minY) + minY; // range [minY, maxY)

            generateVein(chunk, rand, x, y, z, ore, size);
        }
    }

    /**
     * The "Vein" Algorithm (WorldGenMinable logic).
     * Generates an oblate spheroid along a line.
     */
    private void generateVein(Chunk chunk, Random rand, int startX, int startY, int startZ, BlockType ore,
            int numberOfBlocks) {
        // Define line segment logic similar to MC
        float f = rand.nextFloat() * (float) Math.PI;

        // Calculate start/end points for the line
        double sizeScale = numberOfBlocks / 8.0;
        double dx = Math.sin(f) * sizeScale;
        double dz = Math.cos(f) * sizeScale;

        double x1 = startX + 0.5 + dx;
        double x2 = startX + 0.5 - dx;
        double z1 = startZ + 0.5 + dz;
        double z2 = startZ + 0.5 - dz;

        double y1 = startY + rand.nextInt(3) - 2;
        double y2 = startY + rand.nextInt(3) - 2;

        // Iterate steps (blobs along the line)
        for (int i = 0; i <= numberOfBlocks; i++) {
            // Interpolate center point
            double percent = (double) i / (double) numberOfBlocks;
            double cx = x1 + (x2 - x1) * percent;
            double cy = y1 + (y2 - y1) * percent;
            double cz = z1 + (z2 - z1) * percent;

            // Radius based on sine (thick in middle, thin at ends)
            double radius = rand.nextDouble() * numberOfBlocks / 16.0;
            double sphereRadius = (Math.sin(percent * Math.PI) + 1.0) * radius + 1.0;

            // Bounding box for this step's sphere
            int minX = (int) Math.floor(cx - sphereRadius / 2.0);
            int maxX = (int) Math.floor(cx + sphereRadius / 2.0);
            int minY = (int) Math.floor(cy - sphereRadius / 2.0);
            int maxY = (int) Math.floor(cy + sphereRadius / 2.0);
            int minZ = (int) Math.floor(cz - sphereRadius / 2.0);
            int maxZ = (int) Math.floor(cz + sphereRadius / 2.0);

            // Iterate blocks in the sphere
            for (int x = minX; x <= maxX; x++) {
                double xDist = (x + 0.5 - cx) / (sphereRadius / 2.0);
                if (xDist * xDist < 1.0) {
                    for (int y = minY; y <= maxY; y++) {
                        double yDist = (y + 0.5 - cy) / (sphereRadius / 2.0);
                        if (xDist * xDist + yDist * yDist < 1.0) {
                            for (int z = minZ; z <= maxZ; z++) {
                                double zDist = (z + 0.5 - cz) / (sphereRadius / 2.0);
                                if (xDist * xDist + yDist * yDist + zDist * zDist < 1.0) {

                                    if (Chunk.isInBounds(x, y, z)) {
                                        BlockType current = chunk.getBlock(x, y, z);
                                        // Target Block: Stone
                                        if (current == BlockType.STONE) {
                                            chunk.setBlock(x, y, z, ore);
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
}
