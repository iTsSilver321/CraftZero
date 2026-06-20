package com.craftzero.world;

/**
 * Loaded-safe redstone power view used by mechanisms and tests.
 */
public interface PowerQuery {
    int getWeakPower(int x, int y, int z, int towardFace);

    int getStrongPower(int x, int y, int z, int towardFace);

    default boolean isPowered(int x, int y, int z) {
        return RedstoneEngine.isBlockPowered(this, x, y, z);
    }
}
