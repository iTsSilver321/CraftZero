package com.craftzero.world;

import java.util.Random;

/**
 * Release-era Nether cave carver based on the old MapGenCavesHell flow.
 */
public final class NetherCaveGenerator {
    private static final int RANGE = 8;
    private static final int MAX_CARVE_Y = 120;
    private static final float SOURCE_PI = 3.141593F;
    private static final float SOURCE_HALF_PI = 1.570796F;

    private final Random rand = new Random();

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

    private void generateLargeCaveNode(Chunk chunk, int targetChunkX, int targetChunkZ,
            double x, double y, double z) {
        generateCaveNode(chunk, targetChunkX, targetChunkZ, x, y, z,
                1.0F + rand.nextFloat() * 6.0F, 0.0F, 0.0F, -1, -1, 0.5D);
    }

    private void generateCaveNode(Chunk chunk, int targetChunkX, int targetChunkZ,
            double x, double y, double z, float radius, float yaw, float pitch,
            int step, int maxSteps, double verticalScale) {
        double targetCenterX = targetChunkX * 16 + 8;
        double targetCenterZ = targetChunkZ * 16 + 8;
        float yawVelocity = 0.0F;
        float pitchVelocity = 0.0F;
        Random random = new Random(rand.nextLong());

        if (maxSteps <= 0) {
            int fullLength = RANGE * 16 - 16;
            maxSteps = fullLength - random.nextInt(fullLength / 4);
        }

        boolean largeNode = false;
        if (step == -1) {
            step = maxSteps / 2;
            largeNode = true;
        }

        int splitStep = random.nextInt(maxSteps / 2) + maxSteps / 4;
        boolean slowPitch = random.nextInt(6) == 0;

        for (; step < maxSteps; step++) {
            double horizontalRadius = 1.5D
                    + ReleaseOneMath.sin((float) step * SOURCE_PI / (float) maxSteps) * radius;
            double verticalRadius = horizontalRadius * verticalScale;
            float cosPitch = ReleaseOneMath.cos(pitch);
            float sinPitch = ReleaseOneMath.sin(pitch);
            x += ReleaseOneMath.cos(yaw) * cosPitch;
            y += sinPitch;
            z += ReleaseOneMath.sin(yaw) * cosPitch;

            pitch *= slowPitch ? 0.92F : 0.7F;
            pitch += pitchVelocity * 0.1F;
            yaw += yawVelocity * 0.1F;
            pitchVelocity *= 0.9F;
            yawVelocity *= 0.75F;
            pitchVelocity += (random.nextFloat() - random.nextFloat()) * random.nextFloat() * 2.0F;
            yawVelocity += (random.nextFloat() - random.nextFloat()) * random.nextFloat() * 4.0F;

            if (!largeNode && step == splitStep && radius > 1.0F) {
                generateCaveNode(chunk, targetChunkX, targetChunkZ, x, y, z,
                        random.nextFloat() * 0.5F + 0.5F, yaw - SOURCE_HALF_PI,
                        pitch / 3.0F, step, maxSteps, 1.0D);
                generateCaveNode(chunk, targetChunkX, targetChunkZ, x, y, z,
                        random.nextFloat() * 0.5F + 0.5F, yaw + SOURCE_HALF_PI,
                        pitch / 3.0F, step, maxSteps, 1.0D);
                return;
            }

            if (!largeNode && random.nextInt(4) == 0) {
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

            carveEllipsoid(chunk, targetChunkX, targetChunkZ, x, y, z, horizontalRadius, verticalRadius);

            if (largeNode) {
                break;
            }
        }
    }

    private void recursiveGenerate(Chunk chunk, int originChunkX, int originChunkZ,
            int targetChunkX, int targetChunkZ) {
        int nodeCount = rand.nextInt(rand.nextInt(rand.nextInt(10) + 1) + 1);
        if (rand.nextInt(5) != 0) {
            nodeCount = 0;
        }

        for (int node = 0; node < nodeCount; node++) {
            double x = originChunkX * 16 + rand.nextInt(16);
            double y = rand.nextInt(128);
            double z = originChunkZ * 16 + rand.nextInt(16);
            int branches = 1;

            if (rand.nextInt(4) == 0) {
                generateLargeCaveNode(chunk, targetChunkX, targetChunkZ, x, y, z);
                branches += rand.nextInt(4);
            }

            for (int branch = 0; branch < branches; branch++) {
                float yaw = rand.nextFloat() * SOURCE_PI * 2.0F;
                float pitch = ((rand.nextFloat() - 0.5F) * 2.0F) / 8.0F;
                float radius = rand.nextFloat() * 2.0F + rand.nextFloat();
                generateCaveNode(chunk, targetChunkX, targetChunkZ,
                        x, y, z, radius * 2.0F, yaw, pitch, 0, 0, 0.5D);
            }
        }
    }

    private void carveEllipsoid(Chunk chunk, int chunkX, int chunkZ,
            double x, double y, double z, double horizontalRadius, double verticalRadius) {
        int minX = clamp((int) Math.floor(x - horizontalRadius) - chunkX * 16 - 1, 0, 16);
        int maxX = clamp((int) Math.floor(x + horizontalRadius) - chunkX * 16 + 1, 0, 16);
        int minY = clamp((int) Math.floor(y - verticalRadius) - 1, 1, MAX_CARVE_Y);
        int maxY = clamp((int) Math.floor(y + verticalRadius) + 1, 1, MAX_CARVE_Y);
        int minZ = clamp((int) Math.floor(z - horizontalRadius) - chunkZ * 16 - 1, 0, 16);
        int maxZ = clamp((int) Math.floor(z + horizontalRadius) - chunkZ * 16 + 1, 0, 16);

        if (touchesLava(chunk, minX, maxX, minY, maxY, minZ, maxZ)) {
            return;
        }

        for (int localX = minX; localX < maxX; localX++) {
            double normX = ((localX + chunkX * 16) + 0.5D - x) / horizontalRadius;
            for (int localZ = minZ; localZ < maxZ; localZ++) {
                double normZ = ((localZ + chunkZ * 16) + 0.5D - z) / horizontalRadius;
                for (int yy = maxY - 1; yy >= minY; yy--) {
                    double normY = (yy + 0.5D - y) / verticalRadius;
                    if (normY > -0.7D && normX * normX + normY * normY + normZ * normZ < 1.0D
                            && isCarvable(chunk.getBlock(localX, yy, localZ))) {
                        chunk.setBlock(localX, yy, localZ, BlockType.AIR);
                    }
                }
            }
        }
    }

    private static boolean touchesLava(Chunk chunk, int minX, int maxX, int minY, int maxY, int minZ, int maxZ) {
        for (int localX = minX; localX < maxX; localX++) {
            for (int localZ = minZ; localZ < maxZ; localZ++) {
                for (int y = maxY + 1; y >= minY - 1; y--) {
                    if (y < 0 || y >= Chunk.HEIGHT) {
                        continue;
                    }
                    if (chunk.getBlock(localX, y, localZ).isLava()) {
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
        return block == BlockType.NETHERRACK || block == BlockType.DIRT || block == BlockType.GRASS;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
