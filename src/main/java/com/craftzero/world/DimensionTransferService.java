package com.craftzero.world;

public final class DimensionTransferService {
    public static final int END_PLATFORM_CENTER_X = 100;
    public static final int END_PLATFORM_Y = 48;
    public static final int END_PLATFORM_CENTER_Z = 0;
    public static final int END_PLATFORM_RADIUS = 2;
    public static final int END_PLATFORM_CLEARANCE = 3;
    public static final float END_SPAWN_X = END_PLATFORM_CENTER_X + 0.5f;
    public static final float END_SPAWN_Y = END_PLATFORM_Y + 1.0f;
    public static final float END_SPAWN_Z = END_PLATFORM_CENTER_Z + 0.5f;
    public static final float NETHER_SCALE = 8.0f;

    private DimensionTransferService() {
    }

    public static TransferTarget fromEndPortal(Dimension current, int overworldSpawnX, int overworldSpawnY,
            int overworldSpawnZ) {
        if (current == Dimension.THE_END) {
            return new TransferTarget(Dimension.OVERWORLD, overworldSpawnX + 0.5f, overworldSpawnY,
                    overworldSpawnZ + 0.5f);
        }
        return new TransferTarget(Dimension.THE_END, END_SPAWN_X, END_SPAWN_Y, END_SPAWN_Z);
    }

    public static TransferTarget fromNetherPortal(Dimension current, float x, float y, float z) {
        if (current == Dimension.OVERWORLD) {
            return new TransferTarget(Dimension.NETHER, x / NETHER_SCALE, clampPortalY(y), z / NETHER_SCALE, true);
        }
        if (current == Dimension.NETHER) {
            return new TransferTarget(Dimension.OVERWORLD, x * NETHER_SCALE, clampPortalY(y), z * NETHER_SCALE, true);
        }
        return null;
    }

    private static float clampPortalY(float y) {
        return Math.max(4.0f, Math.min(Chunk.HEIGHT - 6.0f, y));
    }

    public record TransferTarget(Dimension dimension, float x, float y, float z, boolean prepareNetherPortal) {
        public TransferTarget(Dimension dimension, float x, float y, float z) {
            this(dimension, x, y, z, false);
        }
    }
}
