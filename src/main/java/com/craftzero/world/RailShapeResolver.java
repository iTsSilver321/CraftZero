package com.craftzero.world;

/**
 * Metadata resolver for Release 1.0 rail shapes.
 */
public final class RailShapeResolver {
    public static final int NORTH_SOUTH = 0;
    public static final int EAST_WEST = 1;
    public static final int ASCENDING_EAST = 2;
    public static final int ASCENDING_WEST = 3;
    public static final int ASCENDING_NORTH = 4;
    public static final int ASCENDING_SOUTH = 5;
    public static final int CURVE_SOUTH_EAST = 6;
    public static final int CURVE_SOUTH_WEST = 7;
    public static final int CURVE_NORTH_WEST = 8;
    public static final int CURVE_NORTH_EAST = 9;

    private RailShapeResolver() {
    }

    public static boolean isRail(BlockType type) {
        return type == BlockType.RAIL || type == BlockType.POWERED_RAIL || type == BlockType.DETECTOR_RAIL;
    }

    public static void updateRailAt(World world, int x, int y, int z) {
        BlockType type = world.getBlockIfLoaded(x, y, z, BlockType.AIR);
        if (!isRail(type)) {
            return;
        }
        if (!BlockShape.canPlaceAt(type, world.getBlockMetadataIfLoaded(x, y, z, 0), world.contextAtIfLoaded(x, y, z))) {
            world.breakBlock(x, y, z, true);
            return;
        }

        int metadata = world.getBlockMetadataIfLoaded(x, y, z, 0);
        boolean powered = (metadata & RedstoneEngine.RAIL_POWERED_BIT) != 0;
        if (type == BlockType.POWERED_RAIL) {
            powered = RedstoneEngine.isBlockPowered(world, x, y, z);
        }

        int shape = resolveShape(world, x, y, z, type);
        int newMetadata = shape | (powered ? RedstoneEngine.RAIL_POWERED_BIT : 0);
        if (newMetadata != metadata) {
            world.setBlockIfLoaded(x, y, z, type, newMetadata);
        }
    }

    public static int resolveShape(World world, int x, int y, int z, BlockType type) {
        boolean north = hasRail(world, x, y, z - 1);
        boolean south = hasRail(world, x, y, z + 1);
        boolean west = hasRail(world, x - 1, y, z);
        boolean east = hasRail(world, x + 1, y, z);

        if (type == BlockType.RAIL) {
            if (south && east && !north && !west) {
                return CURVE_SOUTH_EAST;
            }
            if (south && west && !north && !east) {
                return CURVE_SOUTH_WEST;
            }
            if (north && west && !south && !east) {
                return CURVE_NORTH_WEST;
            }
            if (north && east && !south && !west) {
                return CURVE_NORTH_EAST;
            }
        }

        if (east && hasRailAt(world, x + 1, y + 1, z)) {
            return ASCENDING_EAST;
        }
        if (west && hasRailAt(world, x - 1, y + 1, z)) {
            return ASCENDING_WEST;
        }
        if (north && hasRailAt(world, x, y + 1, z - 1)) {
            return ASCENDING_NORTH;
        }
        if (south && hasRailAt(world, x, y + 1, z + 1)) {
            return ASCENDING_SOUTH;
        }

        if ((east || west) && !(north || south)) {
            return EAST_WEST;
        }
        if ((north || south) && !(east || west)) {
            return NORTH_SOUTH;
        }
        if (east || west) {
            return EAST_WEST;
        }
        return NORTH_SOUTH;
    }

    private static boolean hasRail(World world, int x, int y, int z) {
        return hasRailAt(world, x, y, z) || hasRailAt(world, x, y + 1, z) || hasRailAt(world, x, y - 1, z);
    }

    public static boolean hasRailAt(World world, int x, int y, int z) {
        return isRail(world.getBlockIfLoaded(x, y, z, BlockType.AIR));
    }

    public static int findRailY(World world, int x, int y, int z) {
        if (hasRailAt(world, x, y, z)) {
            return y;
        }
        if (hasRailAt(world, x, y - 1, z)) {
            return y - 1;
        }
        if (hasRailAt(world, x, y + 1, z)) {
            return y + 1;
        }
        return Integer.MIN_VALUE;
    }

    public static boolean isNorthSouth(int shape) {
        return shape == NORTH_SOUTH || shape == ASCENDING_NORTH || shape == ASCENDING_SOUTH;
    }

    public static boolean isEastWest(int shape) {
        return shape == EAST_WEST || shape == ASCENDING_EAST || shape == ASCENDING_WEST;
    }

    public static boolean isAscending(int shape) {
        return shape >= ASCENDING_EAST && shape <= ASCENDING_SOUTH;
    }
}
