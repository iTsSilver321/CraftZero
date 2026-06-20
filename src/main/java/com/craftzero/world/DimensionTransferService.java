package com.craftzero.world;

public final class DimensionTransferService {
    public static final float END_SPAWN_X = 100.5f;
    public static final float END_SPAWN_Y = 50.0f;
    public static final float END_SPAWN_Z = 0.5f;

    private DimensionTransferService() {
    }

    public static TransferTarget fromEndPortal(Dimension current, int overworldSpawnX, int overworldSpawnY,
            int overworldSpawnZ) {
        if (current == Dimension.THE_END) {
            return new TransferTarget(Dimension.OVERWORLD, overworldSpawnX + 0.5f, overworldSpawnY, overworldSpawnZ + 0.5f);
        }
        return new TransferTarget(Dimension.THE_END, END_SPAWN_X, END_SPAWN_Y, END_SPAWN_Z);
    }

    public record TransferTarget(Dimension dimension, float x, float y, float z) {
    }
}
