package com.craftzero.world;

/**
 * Immutable block state used by shape, placement, rendering, and save logic.
 * Release 1.0 stores most per-block variants as a block id plus 4-bit metadata.
 */
public record BlockState(BlockType type, int metadata) {
    public BlockState {
        if (type == null) {
            type = BlockType.AIR;
        }
        metadata &= 15;
    }

    public static BlockState of(BlockType type) {
        return new BlockState(type, 0);
    }

    public boolean isAir() {
        return type.isAir();
    }

    public boolean isFluid() {
        return type.isFluid();
    }
}
