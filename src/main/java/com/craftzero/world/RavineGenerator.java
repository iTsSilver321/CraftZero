package com.craftzero.world;

import java.util.Random;

/**
 * Release-era ravine carver adapted from MapGenRavine.
 */
public class RavineGenerator {
    private static final int RANGE = 8;
    private static final int MAX_CARVE_Y = 120;
    private static final int LAVA_Y = 10;
    private static final float SOURCE_PI = 3.141593F;

    private final Random rand = new Random();
    private final float[] verticalScale = new float[1024];

    public void generate(Chunk chunk, long worldSeed) {
        int chunkX = chunk.getX();
        int chunkZ = chunk.getZ();
        rand.setSeed(worldSeed);
        long xSeed = rand.nextLong();
        long zSeed = rand.nextLong();

        for (int originX = chunkX - RANGE; originX <= chunkX + RANGE; originX++) {
            for (int originZ = chunkZ - RANGE; originZ <= chunkZ + RANGE; originZ++) {
                rand.setSeed((long) originX * xSeed ^ (long) originZ * zSeed ^ worldSeed);
                recursiveGenerate(chunk, originX, originZ, chunkX, chunkZ);
            }
        }
    }

    protected void generateRavine(long seed, Chunk chunk, int targetChunkX, int targetChunkZ,
            double x, double y, double z, float radius, float yaw, float pitch,
            int step, int maxSteps, double yScale) {
        Random random = new Random(seed);
        double targetCenterX = targetChunkX * 16 + 8;
        double targetCenterZ = targetChunkZ * 16 + 8;
        float yawVelocity = 0.0F;
        float pitchVelocity = 0.0F;

        if (maxSteps <= 0) {
            int fullLength = RANGE * 16 - 16;
            maxSteps = fullLength - random.nextInt(fullLength / 4);
        }

        boolean singleNode = false;
        if (step == -1) {
            step = maxSteps / 2;
            singleNode = true;
        }

        float scale = 1.0F;
        for (int i = 0; i < Chunk.HEIGHT; i++) {
            if (i == 0 || random.nextInt(3) == 0) {
                scale = 1.0F + random.nextFloat() * random.nextFloat();
            }
            verticalScale[i] = scale * scale;
        }

        for (; step < maxSteps; step++) {
            double horizontalRadius = 1.5D
                    + ReleaseOneMath.sin((float) step * SOURCE_PI / (float) maxSteps) * radius;
            double verticalRadius = horizontalRadius * yScale;
            horizontalRadius *= random.nextFloat() * 0.25D + 0.75D;
            verticalRadius *= random.nextFloat() * 0.25D + 0.75D;
            float cosPitch = ReleaseOneMath.cos(pitch);
            float sinPitch = ReleaseOneMath.sin(pitch);
            x += ReleaseOneMath.cos(yaw) * cosPitch;
            y += sinPitch;
            z += ReleaseOneMath.sin(yaw) * cosPitch;
            pitch *= 0.7F;
            pitch += pitchVelocity * 0.05F;
            yaw += yawVelocity * 0.05F;
            pitchVelocity *= 0.8F;
            yawVelocity *= 0.5F;
            pitchVelocity += (random.nextFloat() - random.nextFloat()) * random.nextFloat() * 2.0F;
            yawVelocity += (random.nextFloat() - random.nextFloat()) * random.nextFloat() * 4.0F;

            if (!singleNode && random.nextInt(4) == 0) {
                continue;
            }

            double dx = x - targetCenterX;
            double dz = z - targetCenterZ;
            double remaining = maxSteps - step;
            double reach = radius + 2.0F + 16.0F;
            if (dx * dx + dz * dz - remaining * remaining > reach * reach) {
                return;
            }

            if (x < targetCenterX - 16.0D - horizontalRadius * 2.0D
                    || z < targetCenterZ - 16.0D - horizontalRadius * 2.0D
                    || x > targetCenterX + 16.0D + horizontalRadius * 2.0D
                    || z > targetCenterZ + 16.0D + horizontalRadius * 2.0D) {
                continue;
            }

            carveRavineEllipsoid(chunk, targetChunkX, targetChunkZ, x, y, z,
                    horizontalRadius, verticalRadius);

            if (singleNode) {
                break;
            }
        }
    }

    private void recursiveGenerate(Chunk chunk, int originChunkX, int originChunkZ,
            int targetChunkX, int targetChunkZ) {
        if (rand.nextInt(50) != 0) {
            return;
        }

        double x = originChunkX * 16 + rand.nextInt(16);
        double y = rand.nextInt(rand.nextInt(40) + 8) + 20;
        double z = originChunkZ * 16 + rand.nextInt(16);
        float yaw = rand.nextFloat() * SOURCE_PI * 2.0F;
        float pitch = ((rand.nextFloat() - 0.5F) * 2.0F) / 8.0F;
        float radius = (rand.nextFloat() * 2.0F + rand.nextFloat()) * 2.0F;
        generateRavine(rand.nextLong(), chunk, targetChunkX, targetChunkZ,
                x, y, z, radius, yaw, pitch, 0, 0, 3.0D);
    }

    private void carveRavineEllipsoid(Chunk chunk, int chunkX, int chunkZ,
            double x, double y, double z, double horizontalRadius, double verticalRadius) {
        int minX = clamp((int) Math.floor(x - horizontalRadius) - chunkX * 16 - 1, 0, 16);
        int maxX = clamp((int) Math.floor(x + horizontalRadius) - chunkX * 16 + 1, 0, 16);
        int minY = clamp((int) Math.floor(y - verticalRadius) - 1, 1, MAX_CARVE_Y);
        int maxY = clamp((int) Math.floor(y + verticalRadius) + 1, 1, MAX_CARVE_Y);
        int minZ = clamp((int) Math.floor(z - horizontalRadius) - chunkZ * 16 - 1, 0, 16);
        int maxZ = clamp((int) Math.floor(z + horizontalRadius) - chunkZ * 16 + 1, 0, 16);

        if (touchesWater(chunk, minX, maxX, minY, maxY, minZ, maxZ)) {
            return;
        }

        for (int localX = minX; localX < maxX; localX++) {
            double normX = ((localX + chunkX * 16) + 0.5D - x) / horizontalRadius;
            for (int localZ = minZ; localZ < maxZ; localZ++) {
                double normZ = ((localZ + chunkZ * 16) + 0.5D - z) / horizontalRadius;
                if (normX * normX + normZ * normZ >= 1.0D) {
                    continue;
                }
                boolean foundGrass = false;
                for (int yy = maxY - 1; yy >= minY; yy--) {
                    double normY = (yy + 0.5D - y) / verticalRadius;
                    if ((normX * normX + normZ * normZ) * verticalScale[yy]
                            + (normY * normY) / 6.0D >= 1.0D) {
                        continue;
                    }
                    BlockType block = chunk.getBlock(localX, yy, localZ);
                    if (block == BlockType.GRASS) {
                        foundGrass = true;
                    }
                    if (isCarvable(block)) {
                        if (yy < LAVA_Y) {
                            chunk.setBlock(localX, yy, localZ, BlockType.LAVA);
                        } else {
                            chunk.setBlock(localX, yy, localZ, BlockType.AIR);
                            if (foundGrass && yy > 0 && chunk.getBlock(localX, yy - 1, localZ) == BlockType.DIRT) {
                                chunk.setBlock(localX, yy - 1, localZ, BlockType.GRASS);
                            }
                        }
                    }
                }
            }
        }
    }

    private static boolean touchesWater(Chunk chunk, int minX, int maxX, int minY, int maxY, int minZ, int maxZ) {
        for (int localX = minX; localX < maxX; localX++) {
            for (int localZ = minZ; localZ < maxZ; localZ++) {
                for (int y = maxY + 1; y >= minY - 1; y--) {
                    if (y < 0 || y >= Chunk.HEIGHT) {
                        continue;
                    }
                    BlockType block = chunk.getBlock(localX, y, localZ);
                    if (block.isWater()) {
                        return true;
                    }
                    if (y != minY - 1 && localX != minX && localX != maxX - 1
                            && localZ != minZ && localZ != maxZ - 1) {
                        y = minY;
                    }
                }
            }
        }
        return false;
    }

    private static boolean isCarvable(BlockType block) {
        return block == BlockType.STONE || block == BlockType.DIRT || block == BlockType.GRASS;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
