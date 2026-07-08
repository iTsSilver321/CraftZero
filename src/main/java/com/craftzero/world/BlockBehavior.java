package com.craftzero.world;

/**
 * Release 1.0 behavior categories used by simulation systems that need to
 * branch on block semantics without duplicating id/name checks.
 */
public enum BlockBehavior {
    NORMAL,
    FLUID,
    FALLING,
    PLANT,
    FIRE,
    RAIL,
    REDSTONE_DUST,
    REDSTONE_POWER_SOURCE,
    REDSTONE_REPEATER,
    PISTON,
    CONTAINER,
    PORTAL,
    CROP,
    SPECIAL;

    public static BlockBehavior of(BlockType type) {
        if (type == null || type == BlockType.AIR) {
            return SPECIAL;
        }
        if (type.isFluid()) {
            return FLUID;
        }
        if (type.isFallingBlock()) {
            return FALLING;
        }
        return switch (type) {
            case SAPLING, TALL_GRASS, DEAD_BUSH, YELLOW_FLOWER, RED_ROSE,
                    BROWN_MUSHROOM, RED_MUSHROOM, SUGAR_CANE, CACTUS, VINES,
                    LILY_PAD, NETHER_WART -> PLANT;
            case CROPS, PUMPKIN_STEM, MELON_STEM -> CROP;
            case FIRE -> FIRE;
            case RAIL, POWERED_RAIL, DETECTOR_RAIL -> RAIL;
            case REDSTONE_WIRE -> REDSTONE_DUST;
            case REDSTONE_TORCH_OFF, REDSTONE_TORCH_ON, LEVER, STONE_BUTTON,
                    STONE_PRESSURE_PLATE, WOODEN_PRESSURE_PLATE -> REDSTONE_POWER_SOURCE;
            case REDSTONE_REPEATER_OFF, REDSTONE_REPEATER_ON -> REDSTONE_REPEATER;
            case PISTON, STICKY_PISTON, PISTON_HEAD, MOVING_PISTON -> PISTON;
            case CHEST, FURNACE, LIT_FURNACE, DISPENSER, NOTE_BLOCK, JUKEBOX,
                    BREWING_STAND, CAULDRON, ENCHANTING_TABLE, MOB_SPAWNER -> CONTAINER;
            case PORTAL, END_PORTAL, END_PORTAL_FRAME -> PORTAL;
            case BED, CAKE, DRAGON_EGG, LOCKED_CHEST -> SPECIAL;
            default -> NORMAL;
        };
    }
}
