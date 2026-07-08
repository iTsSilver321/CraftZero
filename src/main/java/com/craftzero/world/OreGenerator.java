package com.craftzero.world;

import java.util.Random;

/**
 * Generates ores in the world using standard Minecraft Beta 1.8 / Release 1.0
 * logic.
 * Implements the "WorldGenMinable" vein algorithm (oblate spheroid along a
 * line) from raw population origins.
 */
public class OreGenerator {
    private static final float SOURCE_PI = 3.141593F;

    @FunctionalInterface
    interface BlockReader {
        BlockType getBlock(int worldX, int y, int worldZ);
    }

    @FunctionalInterface
    interface BlockWriter {
        void setBlock(int worldX, int y, int worldZ, BlockType block);
    }

    public void generate(Chunk chunk, long seed) {
        for (int originChunkX = chunk.getX() - 1; originChunkX <= chunk.getX(); originChunkX++) {
            for (int originChunkZ = chunk.getZ() - 1; originChunkZ <= chunk.getZ(); originChunkZ++) {
                generateFromOrigin(chunk, seed, originChunkX, originChunkZ);
            }
        }
    }

    private void generateFromOrigin(Chunk chunk, long seed, int originChunkX, int originChunkZ) {
        Random rand = populationRandom(seed, originChunkX, originChunkZ);
        generateFromOrigin(chunk, rand, originChunkX, originChunkZ);
    }

    void generateFromOrigin(Chunk chunk, Random rand, int originChunkX, int originChunkZ) {
        if (chunk == null) {
            generateFromOrigin(rand, originChunkX, originChunkZ, null, null);
            return;
        }
        generateFromOrigin(rand, originChunkX, originChunkZ, defaultReader(chunk), defaultWriter(chunk));
    }

    void generateFromOrigin(Random rand, int originChunkX, int originChunkZ,
            BlockReader blocks, BlockWriter writer) {
        int originX = originChunkX * Chunk.WIDTH;
        int originZ = originChunkZ * Chunk.DEPTH;

        // A. Dirt & Gravel
        generateOre(blocks, writer, rand, originX, originZ, BlockType.DIRT, 20, 32, 0, 128);
        generateOre(blocks, writer, rand, originX, originZ, BlockType.GRAVEL, 10, 32, 0, 128);

        // B. Coal Ore
        generateOre(blocks, writer, rand, originX, originZ, BlockType.COAL_ORE, 20, 16, 0, 128);

        // C. Iron Ore
        generateOre(blocks, writer, rand, originX, originZ, BlockType.IRON_ORE, 20, 8, 0, 64);

        // D. Gold Ore
        generateOre(blocks, writer, rand, originX, originZ, BlockType.GOLD_ORE, 2, 8, 0, 32);

        // E. Redstone Ore
        generateOre(blocks, writer, rand, originX, originZ, BlockType.REDSTONE_ORE, 8, 7, 0, 16);

        // F. Diamond Ore
        generateOre(blocks, writer, rand, originX, originZ, BlockType.DIAMOND_ORE, 1, 7, 0, 16);

        // G. Lapis Lazuli (Bell Curve)
        for (int i = 0; i < 1; i++) {
            int x = originX + rand.nextInt(Chunk.WIDTH);
            // Bell curve centered around 16
            int y = rand.nextInt(16) + rand.nextInt(16);
            int z = originZ + rand.nextInt(Chunk.DEPTH);
            generateVein(blocks, writer, rand, x, y, z, BlockType.LAPIS_ORE, 6);
        }
    }

    void advanceFromOrigin(Random rand, int originChunkX, int originChunkZ) {
        generateFromOrigin(null, rand, originChunkX, originChunkZ);
    }

    private void generateOre(Chunk chunk, Random rand, int originX, int originZ,
            BlockType ore, int tries, int size, int minY, int maxY) {
        generateOre(defaultReader(chunk), defaultWriter(chunk), rand, originX, originZ, ore, tries, size, minY, maxY);
    }

    private void generateOre(BlockReader blocks, BlockWriter writer, Random rand, int originX, int originZ,
            BlockType ore, int tries, int size, int minY, int maxY) {
        for (int i = 0; i < tries; i++) {
            int x = originX + rand.nextInt(Chunk.WIDTH);
            int y = rand.nextInt(maxY - minY) + minY; // range [minY, maxY)
            int z = originZ + rand.nextInt(Chunk.DEPTH);

            generateVein(blocks, writer, rand, x, y, z, ore, size);
        }
    }

    /**
     * The "Vein" Algorithm (WorldGenMinable logic).
     * Generates an oblate spheroid along a line.
     */
    private void generateVein(Chunk chunk, Random rand, int startX, int startY, int startZ, BlockType ore,
            int numberOfBlocks) {
        generateVein(defaultReader(chunk), defaultWriter(chunk), rand, startX, startY, startZ, ore, numberOfBlocks);
    }

    private void generateVein(BlockReader blocks, BlockWriter writer, Random rand,
            int startX, int startY, int startZ, BlockType ore, int numberOfBlocks) {
        // Define line segment logic similar to MC
        float f = rand.nextFloat() * SOURCE_PI;

        // Calculate start/end points for the line
        double dx = (ReleaseOneMath.sin(f) * (float) numberOfBlocks) / 8.0F;
        double dz = (ReleaseOneMath.cos(f) * (float) numberOfBlocks) / 8.0F;

        double sourceX = (float) (startX + 8);
        double sourceZ = (float) (startZ + 8);
        double x1 = sourceX + dx;
        double x2 = sourceX - dx;
        double z1 = sourceZ + dz;
        double z2 = sourceZ - dz;

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
            double sphereRadius = (ReleaseOneMath.sin(i * SOURCE_PI / numberOfBlocks) + 1.0) * radius + 1.0;
            if (blocks == null || writer == null) {
                continue;
            }

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

                                    if (y >= 0 && y < Chunk.HEIGHT
                                            && blocks.getBlock(x, y, z) == BlockType.STONE) {
                                        writer.setBlock(x, y, z, ore);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private static BlockReader defaultReader(Chunk chunk) {
        if (chunk == null) {
            return null;
        }
        return (worldX, y, worldZ) -> {
            if (y < 0 || y >= Chunk.HEIGHT || !containsBlock(chunk, worldX, worldZ)) {
                return null;
            }
            return chunk.getBlock(worldX - chunk.getX() * Chunk.WIDTH, y,
                    worldZ - chunk.getZ() * Chunk.DEPTH);
        };
    }

    private static BlockWriter defaultWriter(Chunk chunk) {
        if (chunk == null) {
            return null;
        }
        return (worldX, y, worldZ, block) -> {
            if (y < 0 || y >= Chunk.HEIGHT || !containsBlock(chunk, worldX, worldZ)) {
                return;
            }
            chunk.setBlock(worldX - chunk.getX() * Chunk.WIDTH, y,
                    worldZ - chunk.getZ() * Chunk.DEPTH, block);
        };
    }

    private static Random populationRandom(long seed, int chunkX, int chunkZ) {
        Random random = new Random(seed);
        long xSeed = (random.nextLong() / 2L) * 2L + 1L;
        long zSeed = (random.nextLong() / 2L) * 2L + 1L;
        random.setSeed((long) chunkX * xSeed + (long) chunkZ * zSeed ^ seed);
        return random;
    }

    private static boolean containsBlock(Chunk chunk, int worldX, int worldZ) {
        int minX = chunk.getX() * Chunk.WIDTH;
        int minZ = chunk.getZ() * Chunk.DEPTH;
        return worldX >= minX && worldX < minX + Chunk.WIDTH
                && worldZ >= minZ && worldZ < minZ + Chunk.DEPTH;
    }
}
