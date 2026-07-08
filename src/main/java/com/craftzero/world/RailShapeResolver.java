package com.craftzero.world;

/**
 * Metadata resolver for Release 1.0 rail shapes.
 */
public final class RailShapeResolver {
    private static final int POWERED_RAIL_PROPAGATION_LIMIT = 8;

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
        int shape = resolveShape(world, x, y, z, type);
        if (type == BlockType.POWERED_RAIL) {
            boolean powered = isPoweredRailActivated(world, x, y, z, shape);
            int newMetadata = shape | (powered ? RedstoneEngine.RAIL_POWERED_BIT : 0);
            if (newMetadata != metadata) {
                world.setBlockIfLoaded(x, y, z, type, newMetadata);
                scheduleConnectedRailTicks(world, x, y, z);
                scheduleConnectedPoweredRails(world, x, y, z, shape);
            }
            return;
        }
        boolean detectorPowered = type == BlockType.DETECTOR_RAIL
                && (metadata & RedstoneEngine.RAIL_POWERED_BIT) != 0;
        int newMetadata = shape | (detectorPowered ? RedstoneEngine.RAIL_POWERED_BIT : 0);
        if (newMetadata != metadata) {
            world.setBlockIfLoaded(x, y, z, type, newMetadata);
            scheduleConnectedRailTicks(world, x, y, z);
        }
    }

    public static int resolveShape(World world, int x, int y, int z, BlockType type) {
        boolean poweredJunction = RedstoneEngine.isBlockPowered(world, x, y, z);
        boolean normalRail = type == BlockType.RAIL;
        boolean north = canConnectRail(world, x, y, z, x, y, z - 1);
        boolean south = canConnectRail(world, x, y, z, x, y, z + 1);
        boolean west = canConnectRail(world, x, y, z, x - 1, y, z);
        boolean east = canConnectRail(world, x, y, z, x + 1, y, z);

        int shape = -1;
        if ((north || south) && !west && !east) {
            shape = NORTH_SOUTH;
        }
        if ((west || east) && !north && !south) {
            shape = EAST_WEST;
        }

        if (normalRail) {
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

        if (shape < 0) {
            if (north || south) {
                shape = NORTH_SOUTH;
            }
            if (west || east) {
                shape = EAST_WEST;
            }
            if (normalRail) {
                shape = junctionCurve(poweredJunction, north, south, west, east, shape);
            }
        }

        if (shape == NORTH_SOUTH) {
            if (hasRailAt(world, x, y + 1, z - 1)) {
                return ASCENDING_NORTH;
            }
            if (hasRailAt(world, x, y + 1, z + 1)) {
                return ASCENDING_SOUTH;
            }
        }
        if (shape == EAST_WEST) {
            if (hasRailAt(world, x + 1, y + 1, z)) {
                return ASCENDING_EAST;
            }
            if (hasRailAt(world, x - 1, y + 1, z)) {
                return ASCENDING_WEST;
            }
        }
        return shape < 0 ? NORTH_SOUTH : shape;
    }

    private static boolean hasRail(World world, int x, int y, int z) {
        return hasRailAt(world, x, y, z) || hasRailAt(world, x, y + 1, z) || hasRailAt(world, x, y - 1, z);
    }

    private static int junctionCurve(boolean powered, boolean north, boolean south, boolean west, boolean east,
            int fallback) {
        int shape = fallback;
        if (powered) {
            if (south && east) {
                shape = CURVE_SOUTH_EAST;
            }
            if (west && south) {
                shape = CURVE_SOUTH_WEST;
            }
            if (east && north) {
                shape = CURVE_NORTH_EAST;
            }
            if (north && west) {
                shape = CURVE_NORTH_WEST;
            }
        } else {
            if (north && west) {
                shape = CURVE_NORTH_WEST;
            }
            if (east && north) {
                shape = CURVE_NORTH_EAST;
            }
            if (west && south) {
                shape = CURVE_SOUTH_WEST;
            }
            if (south && east) {
                shape = CURVE_SOUTH_EAST;
            }
        }
        return shape;
    }

    private static boolean canConnectRail(World world, int railX, int railY, int railZ,
            int neighborX, int neighborY, int neighborZ) {
        RailConnection neighbor = findRailConnection(world, neighborX, neighborY, neighborZ);
        if (neighbor == null) {
            return false;
        }
        int metadata = world.getBlockMetadataIfLoaded(neighbor.x(), neighbor.y(), neighbor.z(), 0);
        BlockType type = world.getBlockIfLoaded(neighbor.x(), neighbor.y(), neighbor.z(), BlockType.AIR);
        int shape = shapeFromMetadata(type, metadata);
        RailConnection[] connections = normalizedConnectionsForShape(world, neighbor.x(), neighbor.y(),
                neighbor.z(), shape);
        for (RailConnection connection : connections) {
            if (connection.x() == railX && connection.z() == railZ) {
                return true;
            }
        }
        return connections.length != 2;
    }

    private static RailConnection findRailConnection(World world, int x, int y, int z) {
        if (hasRailAt(world, x, y, z)) {
            return new RailConnection(x, y, z);
        }
        if (hasRailAt(world, x, y + 1, z)) {
            return new RailConnection(x, y + 1, z);
        }
        if (hasRailAt(world, x, y - 1, z)) {
            return new RailConnection(x, y - 1, z);
        }
        return null;
    }

    public static int shapeFromMetadata(BlockType type, int metadata) {
        return type == BlockType.RAIL ? metadata : metadata & 7;
    }

    private static RailConnection[] normalizedConnectionsForShape(World world, int x, int y, int z, int shape) {
        java.util.ArrayList<RailConnection> normalized = new java.util.ArrayList<>(2);
        for (RailConnection connection : sourceConnectionTargetsForShape(x, y, z, shape)) {
            RailConnection actual = findRailConnection(world, connection.x(), connection.y(), connection.z());
            if (actual != null) {
                normalized.add(actual);
            }
        }
        return normalized.toArray(RailConnection[]::new);
    }

    private static RailConnection[] sourceConnectionTargetsForShape(int x, int y, int z, int shape) {
        return switch (shape) {
            case EAST_WEST -> new RailConnection[] {
                    new RailConnection(x - 1, y, z),
                    new RailConnection(x + 1, y, z)
            };
            case ASCENDING_EAST -> new RailConnection[] {
                    new RailConnection(x - 1, y, z),
                    new RailConnection(x + 1, y + 1, z)
            };
            case ASCENDING_WEST -> new RailConnection[] {
                    new RailConnection(x - 1, y + 1, z),
                    new RailConnection(x + 1, y, z)
            };
            case ASCENDING_NORTH -> new RailConnection[] {
                    new RailConnection(x, y + 1, z - 1),
                    new RailConnection(x, y, z + 1)
            };
            case ASCENDING_SOUTH -> new RailConnection[] {
                    new RailConnection(x, y, z - 1),
                    new RailConnection(x, y + 1, z + 1)
            };
            case CURVE_SOUTH_EAST -> new RailConnection[] {
                    new RailConnection(x + 1, y, z),
                    new RailConnection(x, y, z + 1)
            };
            case CURVE_SOUTH_WEST -> new RailConnection[] {
                    new RailConnection(x - 1, y, z),
                    new RailConnection(x, y, z + 1)
            };
            case CURVE_NORTH_WEST -> new RailConnection[] {
                    new RailConnection(x - 1, y, z),
                    new RailConnection(x, y, z - 1)
            };
            case CURVE_NORTH_EAST -> new RailConnection[] {
                    new RailConnection(x + 1, y, z),
                    new RailConnection(x, y, z - 1)
            };
            default -> new RailConnection[] {
                    new RailConnection(x, y, z - 1),
                    new RailConnection(x, y, z + 1)
            };
        };
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

    public static int findMinecartRailY(World world, int x, int y, int z) {
        if (hasRailAt(world, x, y, z)) {
            return y;
        }
        if (hasRailAt(world, x, y - 1, z)) {
            return y - 1;
        }
        int upperY = y + 1;
        if (hasRailAt(world, x, upperY, z) && hasAscendingRailLeadingTo(world, x, upperY, z)) {
            return upperY;
        }
        return Integer.MIN_VALUE;
    }

    private static boolean hasAscendingRailLeadingTo(World world, int x, int y, int z) {
        int lowerY = y - 1;
        return railShapeAt(world, x - 1, lowerY, z) == ASCENDING_EAST
                || railShapeAt(world, x + 1, lowerY, z) == ASCENDING_WEST
                || railShapeAt(world, x, lowerY, z + 1) == ASCENDING_NORTH
                || railShapeAt(world, x, lowerY, z - 1) == ASCENDING_SOUTH;
    }

    private static int railShapeAt(World world, int x, int y, int z) {
        BlockType type = world.getBlockIfLoaded(x, y, z, BlockType.AIR);
        if (!isRail(type)) {
            return -1;
        }
        return shapeFromMetadata(type, world.getBlockMetadataIfLoaded(x, y, z, 0));
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

    public static void scheduleConnectedRailTicks(World world, int x, int y, int z) {
        BlockType type = world.getBlockIfLoaded(x, y, z, BlockType.AIR);
        if (!isRail(type)) {
            return;
        }
        int metadata = world.getBlockMetadataIfLoaded(x, y, z, 0);
        int shape = shapeFromMetadata(type, metadata);
        java.util.HashSet<Long> visited = new java.util.HashSet<>();
        scheduleConnectedRailTicks(world, x, y, z, shape, 0, visited);
    }

    private static void scheduleConnectedRailTicks(World world, int x, int y, int z, int shape,
            int distance, java.util.Set<Long> visited) {
        BlockType type = world.getBlockIfLoaded(x, y, z, BlockType.AIR);
        if (!isRail(type)) {
            return;
        }
        long key = pack(x, y, z);
        if (!visited.add(key)) {
            return;
        }
        world.scheduleBlockTick(x, y, z, type, 0);
        if (distance >= POWERED_RAIL_PROPAGATION_LIMIT) {
            return;
        }
        for (RailConnection connection : connectionsForShape(x, y, z, shape)) {
            BlockType connectedType = world.getBlockIfLoaded(connection.x(), connection.y(), connection.z(),
                    BlockType.AIR);
            if (!isRail(connectedType)) {
                continue;
            }
            int connectedMetadata = world.getBlockMetadataIfLoaded(connection.x(), connection.y(),
                    connection.z(), 0);
            int connectedShape = shapeFromMetadata(connectedType, connectedMetadata);
            if (!connectsTo(connectedShape, connection.x(), connection.y(), connection.z(), x, y, z)) {
                continue;
            }
            scheduleConnectedRailTicks(world, connection.x(), connection.y(), connection.z(), connectedShape,
                    distance + 1, visited);
        }
    }

    private static boolean isPoweredRailActivated(World world, int x, int y, int z, int shape) {
        if (RedstoneEngine.isBlockPowered(world, x, y, z)) {
            return true;
        }
        java.util.HashSet<Long> visited = new java.util.HashSet<>();
        visited.add(pack(x, y, z));
        for (RailConnection connection : connectionsForShape(x, y, z, shape)) {
            if (receivesRailPowerFrom(world, connection.x(), connection.y(), connection.z(), x, y, z,
                    1, visited)) {
                return true;
            }
        }
        return false;
    }

    private static boolean receivesRailPowerFrom(World world, int x, int y, int z,
            int fromX, int fromY, int fromZ, int distance, java.util.Set<Long> visited) {
        if (distance > POWERED_RAIL_PROPAGATION_LIMIT) {
            return false;
        }
        if (world.getBlockIfLoaded(x, y, z, BlockType.AIR) != BlockType.POWERED_RAIL) {
            return false;
        }
        int metadata = world.getBlockMetadataIfLoaded(x, y, z, 0);
        int shape = shapeFromMetadata(BlockType.POWERED_RAIL, metadata);
        if (!connectsTo(shape, x, y, z, fromX, fromY, fromZ)) {
            return false;
        }
        if (RedstoneEngine.isBlockPowered(world, x, y, z)) {
            return true;
        }
        if (distance == POWERED_RAIL_PROPAGATION_LIMIT) {
            return false;
        }
        long key = pack(x, y, z);
        if (!visited.add(key)) {
            return false;
        }
        for (RailConnection connection : connectionsForShape(x, y, z, shape)) {
            if (connection.x() == fromX && connection.y() == fromY && connection.z() == fromZ) {
                continue;
            }
            if (receivesRailPowerFrom(world, connection.x(), connection.y(), connection.z(), x, y, z,
                    distance + 1, visited)) {
                return true;
            }
        }
        return false;
    }

    private static void scheduleConnectedPoweredRails(World world, int x, int y, int z, int shape) {
        java.util.HashSet<Long> visited = new java.util.HashSet<>();
        visited.add(pack(x, y, z));
        scheduleConnectedPoweredRails(world, x, y, z, shape, 0, visited);
    }

    private static void scheduleConnectedPoweredRails(World world, int x, int y, int z, int shape,
            int distance, java.util.Set<Long> visited) {
        if (distance >= POWERED_RAIL_PROPAGATION_LIMIT) {
            return;
        }
        for (RailConnection connection : connectionsForShape(x, y, z, shape)) {
            if (world.getBlockIfLoaded(connection.x(), connection.y(), connection.z(), BlockType.AIR)
                    != BlockType.POWERED_RAIL) {
                continue;
            }
            int neighborMetadata = world.getBlockMetadataIfLoaded(connection.x(), connection.y(), connection.z(), 0);
            int neighborShape = shapeFromMetadata(BlockType.POWERED_RAIL, neighborMetadata);
            if (!connectsTo(neighborShape, connection.x(), connection.y(), connection.z(), x, y, z)) {
                continue;
            }
            long key = pack(connection.x(), connection.y(), connection.z());
            if (!visited.add(key)) {
                continue;
            }
            world.scheduleBlockTick(connection.x(), connection.y(), connection.z(), BlockType.POWERED_RAIL,
                    RedstoneEngine.REDSTONE_UPDATE_DELAY_TICKS);
            scheduleConnectedPoweredRails(world, connection.x(), connection.y(), connection.z(), neighborShape,
                    distance + 1, visited);
        }
    }

    private static boolean connectsTo(int shape, int x, int y, int z, int targetX, int targetY, int targetZ) {
        for (RailConnection connection : connectionsForShape(x, y, z, shape)) {
            if (connection.x() == targetX && connection.y() == targetY && connection.z() == targetZ) {
                return true;
            }
        }
        return false;
    }

    private static RailConnection[] connectionsForShape(int x, int y, int z, int shape) {
        return switch (shape) {
            case EAST_WEST -> new RailConnection[] {
                    new RailConnection(x - 1, y, z),
                    new RailConnection(x + 1, y, z),
                    new RailConnection(x - 1, y - 1, z),
                    new RailConnection(x + 1, y - 1, z)
            };
            case ASCENDING_EAST -> new RailConnection[] {
                    new RailConnection(x - 1, y, z),
                    new RailConnection(x + 1, y + 1, z)
            };
            case ASCENDING_WEST -> new RailConnection[] {
                    new RailConnection(x - 1, y + 1, z),
                    new RailConnection(x + 1, y, z)
            };
            case ASCENDING_NORTH -> new RailConnection[] {
                    new RailConnection(x, y + 1, z - 1),
                    new RailConnection(x, y, z + 1)
            };
            case ASCENDING_SOUTH -> new RailConnection[] {
                    new RailConnection(x, y, z - 1),
                    new RailConnection(x, y + 1, z + 1)
            };
            case CURVE_SOUTH_EAST -> new RailConnection[] {
                    new RailConnection(x + 1, y, z),
                    new RailConnection(x, y, z + 1)
            };
            case CURVE_SOUTH_WEST -> new RailConnection[] {
                    new RailConnection(x - 1, y, z),
                    new RailConnection(x, y, z + 1)
            };
            case CURVE_NORTH_WEST -> new RailConnection[] {
                    new RailConnection(x - 1, y, z),
                    new RailConnection(x, y, z - 1)
            };
            case CURVE_NORTH_EAST -> new RailConnection[] {
                    new RailConnection(x + 1, y, z),
                    new RailConnection(x, y, z - 1)
            };
            default -> new RailConnection[] {
                    new RailConnection(x, y, z - 1),
                    new RailConnection(x, y, z + 1),
                    new RailConnection(x, y - 1, z - 1),
                    new RailConnection(x, y - 1, z + 1)
            };
        };
    }

    private static long pack(int x, int y, int z) {
        long lx = ((long) x & 0x3ffffffL) << 38;
        long lz = ((long) z & 0x3ffffffL) << 12;
        long ly = (long) y & 0xfffL;
        return lx | lz | ly;
    }

    private record RailConnection(int x, int y, int z) {
    }
}
