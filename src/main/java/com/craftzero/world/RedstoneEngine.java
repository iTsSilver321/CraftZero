package com.craftzero.world;

import com.craftzero.entity.MinecartEntity;
import com.craftzero.inventory.ItemStack;
import com.craftzero.inventory.ItemType;
import com.craftzero.physics.AABB;
import com.craftzero.world.tile.DispenserTileEntity;
import com.craftzero.world.tile.JukeboxTileEntity;
import com.craftzero.world.tile.NoteBlockTileEntity;
import com.craftzero.world.tile.TileEntity;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * Release 1.0-style redstone and mechanism update helper.
 *
 * The helper deliberately uses loaded-safe world queries. Redstone should never
 * be the reason an unloaded chunk gets generated.
 */
public final class RedstoneEngine {
    public static final int POWERED_BIT = 8;
    public static final int DOOR_OPEN_BIT = 4;
    public static final int REPEATER_DELAY_SHIFT = 2;
    public static final int REPEATER_DELAY_MASK = 12;
    public static final int PISTON_EXTENDED_BIT = 8;
    public static final int PISTON_HEAD_STICKY_BIT = 8;
    public static final int RAIL_POWERED_BIT = 8;
    public static final int BUTTON_DELAY_TICKS = 20;
    public static final int WOOD_BUTTON_DELAY_TICKS = 30;
    public static final int REPEATER_BASE_DELAY_TICKS = 2;
    public static final int TORCH_DELAY_TICKS = 2;
    public static final int REDSTONE_UPDATE_DELAY_TICKS = 1;
    public static final int PRESSURE_PLATE_DELAY_TICKS = 20;
    public static final int DETECTOR_RAIL_DELAY_TICKS = 20;
    public static final int DISPENSER_DELAY_TICKS = 4;
    public static final int TNT_FUSE_TICKS = 80;
    public static final int STICKY_PISTON_SHORT_PULSE_TICKS = 3;
    public static final int PISTON_MOVEMENT_TICKS = 2;
    public static final int TORCH_BURNOUT_RECOVERY_TICKS = 160;
    private static final int PISTON_MAX_PUSH_BLOCKS = 12;
    private static final int DUST_SETTLE_MAX_NODES = 4096;
    private static final int DUST_SETTLE_MAX_PASSES = 32;
    private static final float DISPENSER_GENERIC_OFFSET = 0.6f;
    private static final float DISPENSER_GENERIC_Y_OFFSET = -0.3f;
    private static final float DISPENSER_GENERIC_MIN_SPEED = 0.2f;
    private static final float DISPENSER_GENERIC_RANDOM_SPEED = 0.1f;
    private static final float DISPENSER_GENERIC_SPREAD = 6.0f;
    private static final double DISPENSER_SPREAD_SCALE = 0.0075;
    private static final float DETECTOR_RAIL_MINECART_INSET = 0.125f;
    private static final float PRESSURE_PLATE_ENTITY_INSET = 0.125f;
    private static final float PRESSURE_PLATE_ENTITY_HEIGHT = 0.25f;
    private static final float PRESSURE_PLATE_SOUND_Y_OFFSET = 0.1f;

    private static final int[][] DIRS = {
            { 0, 1, 0, Block.FACE_TOP },
            { 0, -1, 0, Block.FACE_BOTTOM },
            { 0, 0, -1, Block.FACE_NORTH },
            { 0, 0, 1, Block.FACE_SOUTH },
            { 1, 0, 0, Block.FACE_EAST },
            { -1, 0, 0, Block.FACE_WEST }
    };
    private static final int[][] HORIZONTAL_DIRS = {
            { 1, 0 },
            { -1, 0 },
            { 0, 1 },
            { 0, -1 }
    };
    private static final int[][] RAIL_NEIGHBOR_OFFSETS = {
            { 0, 0, 0 },
            { 0, 1, 0 }, { 0, -1, 0 },
            { 1, 0, 0 }, { -1, 0, 0 }, { 0, 0, 1 }, { 0, 0, -1 },
            { 2, 0, 0 }, { -2, 0, 0 }, { 0, 0, 2 }, { 0, 0, -2 },
            { 1, 1, 0 }, { -1, 1, 0 }, { 0, 1, 1 }, { 0, 1, -1 },
            { 1, -1, 0 }, { -1, -1, 0 }, { 0, -1, 1 }, { 0, -1, -1 },
            { 2, 1, 0 }, { -2, 1, 0 }, { 0, 1, 2 }, { 0, 1, -2 },
            { 2, -1, 0 }, { -2, -1, 0 }, { 0, -1, 2 }, { 0, -1, -2 }
    };
    private static final int[][] DUST_NEIGHBOR_OFFSETS = {
            { 0, 0, 0 },
            { 1, 0, 0 }, { -1, 0, 0 }, { 0, 0, 1 }, { 0, 0, -1 },
            { 1, 1, 0 }, { -1, 1, 0 }, { 0, 1, 1 }, { 0, 1, -1 },
            { 1, -1, 0 }, { -1, -1, 0 }, { 0, -1, 1 }, { 0, -1, -1 }
    };

    private static final Map<World, Map<Long, ArrayDeque<Long>>> TORCH_TOGGLES = new WeakHashMap<>();
    private static final Map<World, Map<Long, Boolean>> OPENABLE_POWER_STATES = new WeakHashMap<>();
    private static final Map<World, Map<Long, StickyPistonExtension>> STICKY_PISTON_EXTENSIONS = new WeakHashMap<>();
    private static final Set<BlockType> PISTON_DISPLACEABLE = EnumSet.of(
            BlockType.AIR,
            BlockType.FLOWING_WATER,
            BlockType.WATER,
            BlockType.FLOWING_LAVA,
            BlockType.LAVA,
            BlockType.FIRE);
    private static final Set<BlockType> PISTON_IMMOVABLE = EnumSet.of(
            BlockType.BEDROCK,
            BlockType.OBSIDIAN,
            BlockType.PORTAL,
            BlockType.END_PORTAL,
            BlockType.END_PORTAL_FRAME,
            BlockType.PISTON_HEAD,
            BlockType.MOVING_PISTON,
            BlockType.LOCKED_CHEST);
    private static final Set<BlockType> PISTON_DESTROY_ON_PUSH = EnumSet.of(
            BlockType.SAPLING,
            BlockType.TALL_GRASS,
            BlockType.DEAD_BUSH,
            BlockType.YELLOW_FLOWER,
            BlockType.RED_ROSE,
            BlockType.BROWN_MUSHROOM,
            BlockType.RED_MUSHROOM,
            BlockType.COBWEB,
            BlockType.TORCH,
            BlockType.REDSTONE_TORCH_OFF,
            BlockType.REDSTONE_TORCH_ON,
            BlockType.REDSTONE_WIRE,
            BlockType.REDSTONE_REPEATER_OFF,
            BlockType.REDSTONE_REPEATER_ON,
            BlockType.CROPS,
            BlockType.CACTUS,
            BlockType.SUGAR_CANE,
            BlockType.LADDER,
            BlockType.LEVER,
            BlockType.STONE_BUTTON,
            BlockType.STONE_PRESSURE_PLATE,
            BlockType.WOODEN_PRESSURE_PLATE,
            BlockType.SNOW_LAYER,
            BlockType.CAKE,
            BlockType.PUMPKIN_STEM,
            BlockType.MELON_STEM,
            BlockType.VINES,
            BlockType.LILY_PAD,
            BlockType.NETHER_WART,
            BlockType.WOODEN_DOOR,
            BlockType.IRON_DOOR,
            BlockType.BED);

    private RedstoneEngine() {
    }

    private record StickyPistonExtension(long tick, boolean movedBlock) {
    }

    private record DustNode(int x, int y, int z) {
    }

    private record PistonPushResult(boolean success, boolean movedBlock) {
    }

    private record PistonPullResult(BlockType type, int metadata) {
        boolean hasBlock() {
            return type != null && type != BlockType.AIR;
        }
    }

    private enum PistonMobility {
        CLEAR,
        DESTROY,
        MOVE,
        BLOCK
    }

    public static boolean isRedstoneTickable(BlockType type) {
        return type == BlockType.REDSTONE_WIRE
                || type == BlockType.REDSTONE_TORCH_OFF
                || type == BlockType.REDSTONE_TORCH_ON
                || type == BlockType.REDSTONE_REPEATER_OFF
                || type == BlockType.REDSTONE_REPEATER_ON
                || type == BlockType.STONE_BUTTON
                || type == BlockType.STONE_PRESSURE_PLATE
                || type == BlockType.WOODEN_PRESSURE_PLATE
                || type == BlockType.RAIL
                || type == BlockType.POWERED_RAIL
                || type == BlockType.DETECTOR_RAIL
                || type == BlockType.WOODEN_DOOR
                || type == BlockType.IRON_DOOR
                || type == BlockType.TRAPDOOR
                || type == BlockType.FENCE_GATE
                || type == BlockType.PISTON
                || type == BlockType.STICKY_PISTON
                || type == BlockType.TNT
                || type == BlockType.DISPENSER;
    }

    public static int getTickDelay(BlockType type, int metadata) {
        if (type == BlockType.STONE_BUTTON) {
            return BUTTON_DELAY_TICKS;
        }
        if (type == BlockType.STONE_PRESSURE_PLATE || type == BlockType.WOODEN_PRESSURE_PLATE) {
            return PRESSURE_PLATE_DELAY_TICKS;
        }
        if (type == BlockType.DETECTOR_RAIL) {
            return DETECTOR_RAIL_DELAY_TICKS;
        }
        if (type == BlockType.DISPENSER) {
            return DISPENSER_DELAY_TICKS;
        }
        if (type == BlockType.REDSTONE_REPEATER_OFF || type == BlockType.REDSTONE_REPEATER_ON) {
            return repeaterDelayTicks(metadata);
        }
        if (type == BlockType.REDSTONE_TORCH_OFF || type == BlockType.REDSTONE_TORCH_ON) {
            return TORCH_DELAY_TICKS;
        }
        return REDSTONE_UPDATE_DELAY_TICKS;
    }

    public static int repeaterDelayTicks(int metadata) {
        int setting = ((metadata & REPEATER_DELAY_MASK) >> REPEATER_DELAY_SHIFT) + 1;
        return setting * REPEATER_BASE_DELAY_TICKS;
    }

    public static boolean isBlockPowered(PowerQuery query, int x, int y, int z) {
        for (int[] dir : DIRS) {
            int nx = x + dir[0];
            int ny = y + dir[1];
            int nz = z + dir[2];
            int face = opposite(dir[3]);
            if (query.getWeakPower(nx, ny, nz, face) > 0 || query.getStrongPower(nx, ny, nz, face) > 0) {
                return true;
            }
        }
        return false;
    }

    public static void rememberPoweredOpenableState(World world, int x, int y, int z) {
        BlockType type = world.getBlockIfLoaded(x, y, z, BlockType.AIR);
        int metadata = world.getBlockMetadataIfLoaded(x, y, z, 0);
        if (!isPoweredOpenable(type)) {
            return;
        }
        int lowerY = type.isDoor() && BlockShape.isDoorUpper(metadata) ? y - 1 : y;
        poweredOpenableStates(world).put(pack(x, lowerY, z), isOpenablePowered(world, x, lowerY, z, type));
    }

    public static void clearPoweredOpenableState(World world, int x, int y, int z, BlockType type, int metadata) {
        if (!isPoweredOpenable(type)) {
            return;
        }
        int lowerY = type.isDoor() && BlockShape.isDoorUpper(metadata) ? y - 1 : y;
        Map<Long, Boolean> states = OPENABLE_POWER_STATES.get(world);
        if (states != null) {
            states.remove(pack(x, lowerY, z));
            if (states.isEmpty()) {
                OPENABLE_POWER_STATES.remove(world);
            }
        }
    }

    public static void clearBlockRuntimeState(World world, int x, int y, int z,
            BlockType previous, int previousMetadata, BlockType current, int currentMetadata) {
        if (world == null || previous == current) {
            return;
        }
        clearPoweredOpenableState(world, x, y, z, previous, previousMetadata);
        clearPoweredOpenableState(world, x, y, z, current, currentMetadata);
        if (previous == BlockType.STICKY_PISTON || current == BlockType.STICKY_PISTON) {
            removeStickyPistonExtension(world, x, y, z);
        }
    }

    public static void clearRuntimeState(World world) {
        if (world == null) {
            return;
        }
        TORCH_TOGGLES.remove(world);
        OPENABLE_POWER_STATES.remove(world);
        STICKY_PISTON_EXTENSIONS.remove(world);
    }

    public static int getWeakPower(World world, int x, int y, int z, int towardFace) {
        BlockType type = world.getBlockIfLoaded(x, y, z, BlockType.AIR);
        int metadata = world.getBlockMetadataIfLoaded(x, y, z, 0);
        return getWeakPower(world, x, y, z, type, metadata, towardFace);
    }

    public static int getStrongPower(World world, int x, int y, int z, int towardFace) {
        BlockType type = world.getBlockIfLoaded(x, y, z, BlockType.AIR);
        int metadata = world.getBlockMetadataIfLoaded(x, y, z, 0);
        return getStrongPower(world, x, y, z, type, metadata, towardFace);
    }

    private static int getWeakPower(World world, int x, int y, int z, BlockType type, int metadata, int towardFace) {
        int directPower = getDirectWeakPower(world, x, y, z, type, metadata, towardFace);
        if (directPower > 0) {
            return directPower;
        }
        if (type.isSolid() && !type.isTransparent()) {
            return isBlockStronglyPowered(world, x, y, z) ? 15 : 0;
        }
        return 0;
    }

    private static int getDirectWeakPower(World world, int x, int y, int z,
            BlockType type, int metadata, int towardFace) {
        if (type == BlockType.REDSTONE_WIRE) {
            return redstoneWirePower(world, x, y, z, metadata, towardFace);
        }
        if (type == BlockType.REDSTONE_TORCH_ON) {
            return torchPowersFace(metadata, towardFace) ? 15 : 0;
        }
        if (isRepeaterOn(type) && repeaterOutputFace(metadata) == towardFace) {
            return 15;
        }
        if (isPoweredControl(type, metadata)) {
            return 15;
        }
        if (type == BlockType.DETECTOR_RAIL && (metadata & RAIL_POWERED_BIT) != 0) {
            return 15;
        }
        return 0;
    }

    private static int getStrongPower(World world, int x, int y, int z, BlockType type, int metadata, int towardFace) {
        if (type == BlockType.REDSTONE_TORCH_ON) {
            return torchStrongPowersFace(metadata, towardFace) ? 15 : 0;
        }
        if (isRepeaterOn(type) && repeaterOutputFace(metadata) == towardFace) {
            return 15;
        }
        if (type == BlockType.LEVER && (metadata & POWERED_BIT) != 0) {
            int outwardFace = BlockShape.leverOutwardFaceFromMetadata(metadata);
            return towardFace == opposite(outwardFace) ? 15 : 0;
        }
        if (type == BlockType.STONE_BUTTON && (metadata & POWERED_BIT) != 0) {
            int outwardFace = BlockShape.buttonOutwardFaceFromMetadata(metadata);
            return towardFace == opposite(outwardFace) ? 15 : 0;
        }
        if ((type == BlockType.STONE_PRESSURE_PLATE || type == BlockType.WOODEN_PRESSURE_PLATE)
                && (metadata & 1) != 0) {
            return towardFace == Block.FACE_BOTTOM ? 15 : 0;
        }
        if (type == BlockType.DETECTOR_RAIL && (metadata & RAIL_POWERED_BIT) != 0) {
            return towardFace == Block.FACE_BOTTOM ? 15 : 0;
        }
        return 0;
    }

    private static int redstoneWirePower(World world, int x, int y, int z, int metadata, int towardFace) {
        int power = Math.max(0, Math.min(15, metadata & 15));
        if (power == 0 || towardFace == Block.FACE_BOTTOM) {
            return 0;
        }
        if (towardFace == Block.FACE_TOP) {
            return power;
        }

        boolean west = redstoneWireConnectsTo(world, x, y, z, Block.FACE_WEST);
        boolean east = redstoneWireConnectsTo(world, x, y, z, Block.FACE_EAST);
        boolean north = redstoneWireConnectsTo(world, x, y, z, Block.FACE_NORTH);
        boolean south = redstoneWireConnectsTo(world, x, y, z, Block.FACE_SOUTH);

        if (!north && !south && !west && !east) {
            return isHorizontalFace(towardFace) ? power : 0;
        }
        return switch (towardFace) {
            case Block.FACE_NORTH -> north && !west && !east ? power : 0;
            case Block.FACE_SOUTH -> south && !west && !east ? power : 0;
            case Block.FACE_WEST -> west && !north && !south ? power : 0;
            case Block.FACE_EAST -> east && !north && !south ? power : 0;
            default -> 0;
        };
    }

    private static boolean redstoneWireConnectsTo(World world, int x, int y, int z, int face) {
        int nx = x + faceToDx(face);
        int nz = z + faceToDz(face);
        int towardWire = opposite(face);

        if (redstoneConnectorAt(world, nx, y, nz, towardWire)) {
            return true;
        }
        BlockType neighbor = world.getBlockIfLoaded(nx, y, nz, BlockType.AIR);
        if (!canRedstoneClimbOn(neighbor)
                && redstoneConnectorAt(world, nx, y - 1, nz, towardWire)) {
            return true;
        }
        return !BlockShape.isOpaqueCube(world.getBlockIfLoaded(x, y + 1, z, BlockType.AIR))
                && canRedstoneClimbOn(neighbor)
                && redstoneConnectorAt(world, nx, y + 1, nz, towardWire);
    }

    private static boolean canRedstoneClimbOn(BlockType type) {
        return BlockShape.isOpaqueCube(type) || type == BlockType.GLOWSTONE;
    }

    private static boolean redstoneConnectorAt(World world, int x, int y, int z, int faceTowardWire) {
        BlockType type = world.getBlockIfLoaded(x, y, z, BlockType.AIR);
        int metadata = world.getBlockMetadataIfLoaded(x, y, z, 0);
        if (type == BlockType.REDSTONE_WIRE) {
            return true;
        }
        if (type == BlockType.REDSTONE_REPEATER_OFF || type == BlockType.REDSTONE_REPEATER_ON) {
            return faceTowardWire == repeaterInputFace(metadata)
                    || faceTowardWire == repeaterOutputFace(metadata);
        }
        return type == BlockType.REDSTONE_TORCH_OFF
                || type == BlockType.REDSTONE_TORCH_ON
                || type == BlockType.LEVER
                || type == BlockType.STONE_BUTTON
                || type == BlockType.STONE_PRESSURE_PLATE
                || type == BlockType.WOODEN_PRESSURE_PLATE
                || type == BlockType.DETECTOR_RAIL;
    }

    private static boolean isHorizontalFace(int face) {
        return face == Block.FACE_NORTH || face == Block.FACE_SOUTH
                || face == Block.FACE_EAST || face == Block.FACE_WEST;
    }

    public static boolean isPoweredControl(BlockType type, int metadata) {
        return type == BlockType.LEVER && (metadata & POWERED_BIT) != 0
                || type == BlockType.STONE_BUTTON && (metadata & POWERED_BIT) != 0
                || type == BlockType.STONE_PRESSURE_PLATE && (metadata & 1) != 0
                || type == BlockType.WOODEN_PRESSURE_PLATE && (metadata & 1) != 0;
    }

    public static boolean isRepeaterOn(BlockType type) {
        return type == BlockType.REDSTONE_REPEATER_ON;
    }

    public static int repeaterOutputFace(int metadata) {
        return horizontalIndexToFace(metadata & 3);
    }

    public static int repeaterInputFace(int metadata) {
        return opposite(repeaterOutputFace(metadata));
    }

    public static int horizontalIndexToFace(int index) {
        return switch (index & 3) {
            case 0 -> Block.FACE_SOUTH;
            case 1 -> Block.FACE_WEST;
            case 2 -> Block.FACE_NORTH;
            default -> Block.FACE_EAST;
        };
    }

    public static int faceToDx(int face) {
        return switch (face) {
            case Block.FACE_EAST -> 1;
            case Block.FACE_WEST -> -1;
            default -> 0;
        };
    }

    public static int faceToDy(int face) {
        return switch (face) {
            case Block.FACE_TOP -> 1;
            case Block.FACE_BOTTOM -> -1;
            default -> 0;
        };
    }

    public static int faceToDz(int face) {
        return switch (face) {
            case Block.FACE_SOUTH -> 1;
            case Block.FACE_NORTH -> -1;
            default -> 0;
        };
    }

    private static int directionToFace(int dx, int dy, int dz) {
        if (dx > 0) {
            return Block.FACE_EAST;
        }
        if (dx < 0) {
            return Block.FACE_WEST;
        }
        if (dy > 0) {
            return Block.FACE_TOP;
        }
        if (dy < 0) {
            return Block.FACE_BOTTOM;
        }
        if (dz > 0) {
            return Block.FACE_SOUTH;
        }
        return Block.FACE_NORTH;
    }

    public static int opposite(int face) {
        return switch (face) {
            case Block.FACE_TOP -> Block.FACE_BOTTOM;
            case Block.FACE_BOTTOM -> Block.FACE_TOP;
            case Block.FACE_NORTH -> Block.FACE_SOUTH;
            case Block.FACE_SOUTH -> Block.FACE_NORTH;
            case Block.FACE_EAST -> Block.FACE_WEST;
            case Block.FACE_WEST -> Block.FACE_EAST;
            default -> face;
        };
    }

    public static boolean toggleInteractiveBlock(World world, int x, int y, int z) {
        BlockType type = world.getBlockIfLoaded(x, y, z, BlockType.AIR);
        int metadata = world.getBlockMetadataIfLoaded(x, y, z, 0);
        if (type == BlockType.LEVER) {
            int newMetadata = metadata ^ POWERED_BIT;
            world.setBlockIfLoaded(x, y, z, type, newMetadata);
            playRedstoneClickSound(world, x, y, z, (newMetadata & POWERED_BIT) != 0);
            world.scheduleMechanismUpdatesAround(x, y, z);
            return true;
        }
        if (type == BlockType.STONE_BUTTON) {
            if ((metadata & POWERED_BIT) == 0) {
                world.setBlockIfLoaded(x, y, z, type, metadata | POWERED_BIT);
                playRedstoneClickSound(world, x, y, z, true);
                world.scheduleBlockTick(x, y, z, type, BUTTON_DELAY_TICKS);
                world.scheduleMechanismUpdatesAround(x, y, z);
            }
            return true;
        }
        if (type == BlockType.REDSTONE_REPEATER_OFF || type == BlockType.REDSTONE_REPEATER_ON) {
            int delay = ((metadata & REPEATER_DELAY_MASK) >> REPEATER_DELAY_SHIFT);
            delay = (delay + 1) & 3;
            int newMetadata = (metadata & ~REPEATER_DELAY_MASK) | (delay << REPEATER_DELAY_SHIFT);
            world.setBlockIfLoaded(x, y, z, type, newMetadata);
            world.rescheduleBlockTick(x, y, z, type, repeaterDelayTicks(newMetadata));
            world.scheduleMechanismUpdatesAround(x, y, z);
            return true;
        }
        if (type == BlockType.NOTE_BLOCK) {
            TileEntity tile = world.getTileEntity(x, y, z);
            if (tile instanceof NoteBlockTileEntity note) {
                note.cyclePitch();
                note.play(world);
                return true;
            }
        }
        if (type == BlockType.JUKEBOX) {
            if ((metadata & 1) == 0) {
                return false;
            }
            TileEntity tile = world.getTileEntity(x, y, z);
            if (tile instanceof JukeboxTileEntity jukebox) {
                jukebox.ejectRecord(world);
            }
            return true;
        }
        return false;
    }

    public static void tick(World world, int x, int y, int z, BlockType type) {
        int metadata = world.getBlockMetadataIfLoaded(x, y, z, 0);
        if (type == BlockType.REDSTONE_WIRE) {
            updateRedstoneWire(world, x, y, z);
            return;
        }
        if (type == BlockType.REDSTONE_TORCH_ON || type == BlockType.REDSTONE_TORCH_OFF) {
            updateRedstoneTorch(world, x, y, z, type, metadata);
            return;
        }
        if (type == BlockType.REDSTONE_REPEATER_ON || type == BlockType.REDSTONE_REPEATER_OFF) {
            updateRepeater(world, x, y, z, type, metadata);
            return;
        }
        if (type == BlockType.STONE_BUTTON) {
            if ((metadata & POWERED_BIT) != 0) {
                world.setBlockIfLoaded(x, y, z, type, metadata & ~POWERED_BIT);
                playRedstoneClickSound(world, x, y, z, false);
                world.scheduleMechanismUpdatesAround(x, y, z);
            }
            return;
        }
        if (type == BlockType.STONE_PRESSURE_PLATE || type == BlockType.WOODEN_PRESSURE_PLATE) {
            updatePressurePlate(world, x, y, z, type, metadata);
            return;
        }
        if (type == BlockType.RAIL || type == BlockType.POWERED_RAIL || type == BlockType.DETECTOR_RAIL) {
            RailShapeResolver.updateRailAt(world, x, y, z);
            if (world.getBlockIfLoaded(x, y, z, BlockType.AIR) == BlockType.DETECTOR_RAIL) {
                metadata = world.getBlockMetadataIfLoaded(x, y, z, metadata);
                updateDetectorRail(world, x, y, z, metadata);
            }
            return;
        }
        if (type == BlockType.WOODEN_DOOR || type == BlockType.IRON_DOOR
                || type == BlockType.TRAPDOOR || type == BlockType.FENCE_GATE) {
            updatePoweredOpenable(world, x, y, z, type, metadata);
            return;
        }
        if (type == BlockType.PISTON || type == BlockType.STICKY_PISTON) {
            updatePiston(world, x, y, z, type, metadata);
            return;
        }
        if (type == BlockType.TNT) {
            if (isBlockPowered(world, x, y, z)) {
                world.primeTnt(x, y, z, TNT_FUSE_TICKS);
            }
            return;
        }
        if (type == BlockType.DISPENSER) {
            updateDispenser(world, x, y, z, metadata);
            return;
        }
        if (type == BlockType.NOTE_BLOCK) {
            updateNoteBlock(world, x, y, z, metadata);
            return;
        }
    }

    public static void updateRedstoneWire(World world, int x, int y, int z) {
        if (world.getBlockIfLoaded(x, y, z, BlockType.AIR) != BlockType.REDSTONE_WIRE) {
            return;
        }
        settleRedstoneWireNetwork(world, x, y, z);
    }

    private static void settleRedstoneWireNetwork(World world, int x, int y, int z) {
        Map<Long, DustNode> nodes = collectRedstoneWireNetwork(world, x, y, z);
        if (nodes.isEmpty()) {
            return;
        }

        Map<Long, Integer> powers = new HashMap<>();
        for (Map.Entry<Long, DustNode> entry : nodes.entrySet()) {
            DustNode node = entry.getValue();
            powers.put(entry.getKey(), world.getBlockMetadataIfLoaded(node.x(), node.y(), node.z(), 0) & 15);
        }

        Set<Long> dirty = new java.util.HashSet<>();
        for (int pass = 0; pass < DUST_SETTLE_MAX_PASSES; pass++) {
            boolean changed = false;
            Map<Long, Integer> next = new HashMap<>(powers);
            for (Map.Entry<Long, DustNode> entry : nodes.entrySet()) {
                DustNode node = entry.getValue();
                int power = Math.max(getBlockInputPower(world, node.x(), node.y(), node.z()),
                        getAdjacentWirePower(world, node.x(), node.y(), node.z(), powers) - 1);
                power = Math.max(0, Math.min(15, power));
                int previous = powers.getOrDefault(entry.getKey(), 0);
                if (previous != power) {
                    next.put(entry.getKey(), power);
                    dirty.add(entry.getKey());
                    changed = true;
                }
            }
            powers = next;
            if (!changed) {
                break;
            }
        }

        List<DustNode> changedNodes = new ArrayList<>();
        for (Map.Entry<Long, DustNode> entry : nodes.entrySet()) {
            if (!dirty.contains(entry.getKey())) {
                continue;
            }
            DustNode node = entry.getValue();
            if (world.getBlockIfLoaded(node.x(), node.y(), node.z(), BlockType.AIR) != BlockType.REDSTONE_WIRE) {
                continue;
            }
            int oldPower = world.getBlockMetadataIfLoaded(node.x(), node.y(), node.z(), 0) & 15;
            int newPower = powers.getOrDefault(entry.getKey(), 0);
            if (oldPower != newPower && world.setBlockIfLoaded(node.x(), node.y(), node.z(),
                    BlockType.REDSTONE_WIRE, newPower)) {
                changedNodes.add(node);
            }
        }

        for (DustNode node : changedNodes) {
            scheduleAdjacentRedstoneWires(world, node.x(), node.y(), node.z());
            world.scheduleMechanismUpdatesAround(node.x(), node.y(), node.z());
        }
    }

    private static Map<Long, DustNode> collectRedstoneWireNetwork(World world, int x, int y, int z) {
        Map<Long, DustNode> nodes = new LinkedHashMap<>();
        ArrayDeque<DustNode> queue = new ArrayDeque<>();
        enqueueDustNode(world, nodes, queue, x, y, z);
        while (!queue.isEmpty() && nodes.size() < DUST_SETTLE_MAX_NODES) {
            DustNode node = queue.removeFirst();
            enqueueConnectedDustNeighbors(world, nodes, queue, node);
        }
        return nodes;
    }

    private static void enqueueConnectedDustNeighbors(World world, Map<Long, DustNode> nodes,
            ArrayDeque<DustNode> queue, DustNode node) {
        for (int[] dir : HORIZONTAL_DIRS) {
            int nx = node.x() + dir[0];
            int nz = node.z() + dir[1];
            BlockType neighbor = world.getBlockIfLoaded(nx, node.y(), nz, BlockType.AIR);
            enqueueDustNode(world, nodes, queue, nx, node.y(), nz);
            if (!canRedstoneClimbOn(neighbor)) {
                enqueueDustNode(world, nodes, queue, nx, node.y() - 1, nz);
            } else if (!BlockShape.isOpaqueCube(world.getBlockIfLoaded(node.x(), node.y() + 1, node.z(),
                    BlockType.AIR))) {
                enqueueDustNode(world, nodes, queue, nx, node.y() + 1, nz);
            }
        }
    }

    private static void enqueueDustNode(World world, Map<Long, DustNode> nodes,
            ArrayDeque<DustNode> queue, int x, int y, int z) {
        if (y < 0 || y >= Chunk.HEIGHT
                || nodes.size() >= DUST_SETTLE_MAX_NODES
                || world.getBlockIfLoaded(x, y, z, BlockType.AIR) != BlockType.REDSTONE_WIRE) {
            return;
        }
        long key = pack(x, y, z);
        if (nodes.containsKey(key)) {
            return;
        }
        DustNode node = new DustNode(x, y, z);
        nodes.put(key, node);
        queue.addLast(node);
    }

    private static int getBlockInputPower(World world, int x, int y, int z) {
        int power = 0;
        for (int[] dir : DIRS) {
            int nx = x + dir[0];
            int ny = y + dir[1];
            int nz = z + dir[2];
            int face = opposite(dir[3]);
            BlockType neighbor = world.getBlockIfLoaded(nx, ny, nz, BlockType.AIR);
            if (neighbor == BlockType.REDSTONE_WIRE) {
                continue;
            }
            power = Math.max(power, getWeakPower(world, nx, ny, nz, face));
            power = Math.max(power, getStrongPower(world, nx, ny, nz, face));
        }
        return power;
    }

    private static int getAdjacentWirePower(World world, int x, int y, int z, Map<Long, Integer> settledPowers) {
        int power = 0;
        for (int[] dir : HORIZONTAL_DIRS) {
            int nx = x + dir[0];
            int nz = z + dir[1];
            BlockType neighbor = world.getBlockIfLoaded(nx, y, nz, BlockType.AIR);
            power = Math.max(power, wirePowerAt(world, nx, y, nz, settledPowers));
            if (!canRedstoneClimbOn(neighbor)) {
                power = Math.max(power, wirePowerAt(world, nx, y - 1, nz, settledPowers));
            } else if (neighbor != BlockType.GLOWSTONE
                    && !BlockShape.isOpaqueCube(world.getBlockIfLoaded(x, y + 1, z, BlockType.BEDROCK))) {
                power = Math.max(power, wirePowerAt(world, nx, y + 1, nz, settledPowers));
            }
        }
        return power;
    }

    private static void scheduleAdjacentRedstoneWires(World world, int x, int y, int z) {
        for (int[] dir : HORIZONTAL_DIRS) {
            int nx = x + dir[0];
            int nz = z + dir[1];
            BlockType neighbor = world.getBlockIfLoaded(nx, y, nz, BlockType.AIR);
            scheduleRedstoneWireIfPresent(world, nx, y, nz);
            if (!canRedstoneClimbOn(neighbor)) {
                scheduleRedstoneWireIfPresent(world, nx, y - 1, nz);
            } else if (!BlockShape.isOpaqueCube(world.getBlockIfLoaded(x, y + 1, z, BlockType.AIR))) {
                scheduleRedstoneWireIfPresent(world, nx, y + 1, nz);
            }
        }
    }

    private static void scheduleRedstoneWireIfPresent(World world, int x, int y, int z) {
        if (world.getBlockIfLoaded(x, y, z, BlockType.AIR) == BlockType.REDSTONE_WIRE) {
            world.scheduleBlockTick(x, y, z, BlockType.REDSTONE_WIRE, 0);
        }
    }

    private static int wirePowerAt(World world, int x, int y, int z, Map<Long, Integer> settledPowers) {
        Integer settledPower = settledPowers == null ? null : settledPowers.get(pack(x, y, z));
        if (settledPower != null) {
            return Math.max(0, Math.min(15, settledPower));
        }
        return world.getBlockIfLoaded(x, y, z, BlockType.AIR) == BlockType.REDSTONE_WIRE
                ? world.getBlockMetadataIfLoaded(x, y, z, 0) & 15
                : 0;
    }

    private static void updateRedstoneTorch(World world, int x, int y, int z, BlockType type, int metadata) {
        boolean powered = isTorchAttachedBlockPowered(world, x, y, z, metadata);
        boolean on = type == BlockType.REDSTONE_TORCH_ON;
        if (on && powered) {
            long now = world.getBlockTickClock();
            recordTorchToggle(world, now, x, y, z);
            world.setBlockIfLoaded(x, y, z, BlockType.REDSTONE_TORCH_OFF, metadata);
            if (isTorchBurnedOut(world, now, x, y, z)) {
                world.playRedstoneTorchBurnoutFeedback(x, y, z);
                world.scheduleBlockTick(x, y, z, BlockType.REDSTONE_TORCH_OFF, TORCH_BURNOUT_RECOVERY_TICKS);
            }
            world.scheduleMechanismUpdatesAround(x, y, z);
        } else if (!on && !powered && !isTorchBurnedOut(world, world.getBlockTickClock(), x, y, z)) {
            world.setBlockIfLoaded(x, y, z, BlockType.REDSTONE_TORCH_ON, metadata);
            world.scheduleMechanismUpdatesAround(x, y, z);
        } else if (!on && !powered) {
            world.scheduleBlockTick(x, y, z, BlockType.REDSTONE_TORCH_OFF, TORCH_DELAY_TICKS);
        }
    }

    private static boolean isTorchAttachedBlockPowered(World world, int x, int y, int z, int metadata) {
        int outwardFace = torchOutwardFace(metadata);
        int sx = x + faceToDx(opposite(outwardFace));
        int sy = y + faceToDy(opposite(outwardFace));
        int sz = z + faceToDz(opposite(outwardFace));
        return isBlockPowered(world, sx, sy, sz);
    }

    private static boolean torchPowersFace(int metadata, int towardFace) {
        int outwardFace = torchOutwardFace(metadata);
        return towardFace != opposite(outwardFace);
    }

    private static boolean torchStrongPowersFace(int metadata, int towardFace) {
        return towardFace == Block.FACE_TOP && torchPowersFace(metadata, towardFace);
    }

    private static int torchOutwardFace(int metadata) {
        int outwardFace = BlockShape.torchOutwardFaceFromMetadata(metadata);
        return outwardFace >= 0 ? outwardFace : Block.FACE_TOP;
    }

    private static void recordTorchToggle(World world, long now, int x, int y, int z) {
        long key = pack(x, y, z);
        ArrayDeque<Long> toggles = torchToggles(world).computeIfAbsent(key, ignored -> new ArrayDeque<>());
        toggles.addLast(now);
        while (!toggles.isEmpty() && now - toggles.peekFirst() > 60) {
            toggles.removeFirst();
        }
    }

    private static boolean isTorchBurnedOut(World world, long now, int x, int y, int z) {
        Map<Long, ArrayDeque<Long>> togglesByPosition = TORCH_TOGGLES.get(world);
        if (togglesByPosition == null) {
            return false;
        }
        long key = pack(x, y, z);
        ArrayDeque<Long> toggles = togglesByPosition.get(key);
        if (toggles == null) {
            return false;
        }
        while (!toggles.isEmpty() && now - toggles.peekFirst() > 60) {
            toggles.removeFirst();
        }
        if (toggles.isEmpty()) {
            togglesByPosition.remove(key);
            if (togglesByPosition.isEmpty()) {
                TORCH_TOGGLES.remove(world);
            }
            return false;
        }
        return toggles.size() >= 8;
    }

    private static Map<Long, ArrayDeque<Long>> torchToggles(World world) {
        return TORCH_TOGGLES.computeIfAbsent(world, ignored -> new HashMap<>());
    }

    private static void updateRepeater(World world, int x, int y, int z, BlockType type, int metadata) {
        int inputFace = repeaterInputFace(metadata);
        int ix = x + faceToDx(inputFace);
        int iy = y + faceToDy(inputFace);
        int iz = z + faceToDz(inputFace);
        boolean powered = isPoweredWireForRepeaterInput(world, ix, iy, iz)
                || getWeakPower(world, ix, iy, iz, opposite(inputFace)) > 0
                || getStrongPower(world, ix, iy, iz, opposite(inputFace)) > 0
                || isOpaqueBlockRelayingDirectPower(world, ix, iy, iz);
        if (powered && type == BlockType.REDSTONE_REPEATER_OFF) {
            world.setBlockIfLoaded(x, y, z, BlockType.REDSTONE_REPEATER_ON, metadata);
            world.scheduleMechanismUpdatesAround(x, y, z);
        } else if (!powered && type == BlockType.REDSTONE_REPEATER_ON) {
            world.setBlockIfLoaded(x, y, z, BlockType.REDSTONE_REPEATER_OFF, metadata);
            world.scheduleMechanismUpdatesAround(x, y, z);
        }
    }

    private static boolean isPoweredWireForRepeaterInput(World world, int x, int y, int z) {
        return world.getBlockIfLoaded(x, y, z, BlockType.AIR) == BlockType.REDSTONE_WIRE
                && (world.getBlockMetadataIfLoaded(x, y, z, 0) & 15) > 0;
    }

    private static void updatePressurePlate(World world, int x, int y, int z, BlockType type, int metadata) {
        boolean wooden = type == BlockType.WOODEN_PRESSURE_PLATE;
        boolean occupied = wooden
                ? world.hasEntityIntersecting(x + PRESSURE_PLATE_ENTITY_INSET, y,
                        z + PRESSURE_PLATE_ENTITY_INSET,
                        x + 1.0f - PRESSURE_PLATE_ENTITY_INSET,
                        y + PRESSURE_PLATE_ENTITY_HEIGHT,
                        z + 1.0f - PRESSURE_PLATE_ENTITY_INSET, true)
                : world.hasLivingEntityIntersecting(x + PRESSURE_PLATE_ENTITY_INSET, y,
                        z + PRESSURE_PLATE_ENTITY_INSET,
                        x + 1.0f - PRESSURE_PLATE_ENTITY_INSET,
                        y + PRESSURE_PLATE_ENTITY_HEIGHT,
                        z + 1.0f - PRESSURE_PLATE_ENTITY_INSET);
        boolean powered = (metadata & 1) != 0;
        if (occupied != powered) {
            world.setBlockIfLoaded(x, y, z, type, occupied ? 1 : 0);
            playPressurePlateClickSound(world, x, y, z, occupied);
            world.scheduleMechanismUpdatesAround(x, y, z);
        }
        if (occupied) {
            world.scheduleBlockTick(x, y, z, type, PRESSURE_PLATE_DELAY_TICKS);
        }
    }

    private static void updateDetectorRail(World world, int x, int y, int z, int metadata) {
        if (world.getBlockIfLoaded(x, y, z, BlockType.AIR) != BlockType.DETECTOR_RAIL) {
            return;
        }
        boolean occupied = world.hasMinecartIntersecting(
                x + DETECTOR_RAIL_MINECART_INSET,
                y,
                z + DETECTOR_RAIL_MINECART_INSET,
                x + 1.0f - DETECTOR_RAIL_MINECART_INSET,
                y + 1.0f - DETECTOR_RAIL_MINECART_INSET,
                z + 1.0f - DETECTOR_RAIL_MINECART_INSET);
        boolean powered = (metadata & RAIL_POWERED_BIT) != 0;
        if (occupied != powered) {
            int newMetadata = occupied ? metadata | RAIL_POWERED_BIT : metadata & ~RAIL_POWERED_BIT;
            world.setBlockIfLoaded(x, y, z, BlockType.DETECTOR_RAIL, newMetadata);
            world.scheduleMechanismUpdatesAround(x, y, z);
        }
        if (occupied) {
            world.scheduleBlockTick(x, y, z, BlockType.DETECTOR_RAIL, DETECTOR_RAIL_DELAY_TICKS);
        }
    }

    private static void playRedstoneClickSound(World world, int x, int y, int z, boolean powered) {
        world.playSound(WorldSoundEvent.REDSTONE_CLICK,
                x + 0.5f, y + 0.5f, z + 0.5f,
                0.3f, WorldSoundEvent.redstoneClickPitch(powered));
    }

    private static void playPressurePlateClickSound(World world, int x, int y, int z, boolean powered) {
        world.playSound(WorldSoundEvent.REDSTONE_CLICK,
                x + 0.5f, y + PRESSURE_PLATE_SOUND_Y_OFFSET, z + 0.5f,
                0.3f, WorldSoundEvent.redstoneClickPitch(powered));
    }

    private static void updatePoweredOpenable(World world, int x, int y, int z, BlockType type, int metadata) {
        int lowerY = type.isDoor() && BlockShape.isDoorUpper(metadata) ? y - 1 : y;
        if (type.isDoor() && BlockShape.isDoorUpper(metadata)) {
            BlockType lowerType = world.getBlockIfLoaded(x, lowerY, z, BlockType.AIR);
            int lowerMetadata = world.getBlockMetadataIfLoaded(x, lowerY, z, 0);
            if (lowerType != type || BlockShape.isDoorUpper(lowerMetadata)) {
                return;
            }
        }
        int effectiveMetadata = type.isDoor() ? world.getBlockMetadataIfLoaded(x, lowerY, z, metadata) : metadata;
        boolean powered = isOpenablePowered(world, x, lowerY, z, type);
        Map<Long, Boolean> states = poweredOpenableStates(world);
        long key = pack(x, lowerY, z);
        Boolean previousPowered = states.get(key);
        boolean open = (effectiveMetadata & DOOR_OPEN_BIT) != 0;
        boolean shouldChange = previousPowered == null
                ? powered && !open
                : previousPowered != powered && powered != open;
        if (shouldChange) {
            boolean newOpen = !open;
            if (world.setBlockIfLoaded(x, lowerY, z, type, effectiveMetadata ^ DOOR_OPEN_BIT)) {
                rebuildOpenableMeshes(world, x, lowerY, z, type);
                world.playOpenableSound(type, x, lowerY, z, newOpen);
            }
        }
        states.put(key, powered);
    }

    private static void rebuildOpenableMeshes(World world, int x, int y, int z, BlockType type) {
        world.rebuildBlockMeshesNow(x, y, z);
        if (type.isDoor() && world.getBlockIfLoaded(x, y + 1, z, BlockType.AIR) == type
                && BlockShape.isDoorUpper(world.getBlockMetadataIfLoaded(x, y + 1, z, 0))) {
            world.rebuildBlockMeshesNow(x, y + 1, z);
        }
    }

    private static boolean isPoweredOpenable(BlockType type) {
        return type == BlockType.WOODEN_DOOR || type == BlockType.IRON_DOOR
                || type == BlockType.TRAPDOOR || type == BlockType.FENCE_GATE;
    }

    private static boolean isOpenablePowered(World world, int x, int lowerY, int z, BlockType type) {
        return isBlockPowered(world, x, lowerY, z)
                || (type.isDoor() && isBlockPowered(world, x, lowerY + 1, z));
    }

    private static Map<Long, Boolean> poweredOpenableStates(World world) {
        return OPENABLE_POWER_STATES.computeIfAbsent(world, ignored -> new HashMap<>());
    }

    private static void updatePiston(World world, int x, int y, int z, BlockType type, int metadata) {
        int facing = metadata & 7;
        if (!isPistonFacing(facing)) {
            facing = Block.FACE_NORTH;
        }
        boolean powered = isPistonPowered(world, x, y, z, facing);
        boolean extended = (metadata & PISTON_EXTENDED_BIT) != 0;
        int fx = faceToDx(facing);
        int fy = faceToDy(facing);
        int fz = faceToDz(facing);
        if (powered && !extended) {
            PistonPushResult push = tryPushBlocks(world, x + fx, y + fy, z + fz, fx, fy, fz);
            if (push.success()) {
                world.setBlockIfLoaded(x, y, z, type, metadata | PISTON_EXTENDED_BIT);
                world.playSound(WorldSoundEvent.PISTON_EXTEND,
                        x + 0.5f, y + 0.5f, z + 0.5f, 0.5f,
                        WorldSoundEvent.pistonExtendPitch(world.getRandom()));
                if (type == BlockType.STICKY_PISTON) {
                    stickyPistonExtensions(world).put(pack(x, y, z),
                            new StickyPistonExtension(world.getBlockTickClock(), push.movedBlock()));
                }
                world.pushEntitiesIntersectingBlock(x + fx, y + fy, z + fz, fx, fy, fz);
                int headMetadata = facing | (type == BlockType.STICKY_PISTON ? PISTON_HEAD_STICKY_BIT : 0);
                world.startMovingPiston(x + fx, y + fy, z + fz, facing,
                        BlockType.PISTON_HEAD, headMetadata,
                        BlockType.PISTON_HEAD, headMetadata,
                        x, y, z,
                        x + fx, y + fy, z + fz);
            }
        } else if (!powered && extended) {
            BlockType front = world.getBlockIfLoaded(x + fx, y + fy, z + fz, BlockType.AIR);
            int frontMetadata = world.getBlockMetadataIfLoaded(x + fx, y + fy, z + fz, facing);
            World.MovingPistonState movingHead = null;
            if (front == BlockType.MOVING_PISTON) {
                movingHead = world.getMovingPistonState(x + fx, y + fy, z + fz);
                if (movingHead != null && movingHead.restoredFromSave()
                        && movingHead.carriedType() == BlockType.PISTON_HEAD
                        && movingHead.finalType() == BlockType.PISTON_HEAD
                        && !hasLoadedPistonPowerQuery(world, x, y, z, facing)) {
                    return;
                }
            }
            boolean hasHead = front == BlockType.PISTON_HEAD || front == BlockType.MOVING_PISTON;
            world.setBlockIfLoaded(x, y, z, type, metadata & ~PISTON_EXTENDED_BIT);
            world.playSound(WorldSoundEvent.PISTON_RETRACT,
                    x + 0.5f, y + 0.5f, z + 0.5f, 0.5f,
                    WorldSoundEvent.pistonRetractPitch(world.getRandom()));
            PistonPullResult pull = new PistonPullResult(BlockType.AIR, 0);
            if (type == BlockType.STICKY_PISTON && shouldStickyPistonPull(world, x, y, z)) {
                pull = takeStickyBlock(world, x + fx, y + fy, z + fz, fx, fy, fz);
            }
            if (pull.hasBlock()) {
                world.startMovingPiston(x + fx, y + fy, z + fz, facing,
                        pull.type(), pull.metadata(),
                        pull.type(), pull.metadata(),
                        x + fx + fx, y + fy + fy, z + fz + fz,
                        x + fx, y + fy, z + fz);
            } else if (hasHead) {
                int headMetadata = pistonHeadMetadataForRetraction(type, facing, front, frontMetadata, movingHead);
                world.startMovingPiston(x + fx, y + fy, z + fz, facing,
                        BlockType.PISTON_HEAD, headMetadata,
                        BlockType.AIR, 0,
                        x + fx, y + fy, z + fz,
                        x, y, z);
            }
        }
    }

    private static int pistonHeadMetadataForRetraction(BlockType pistonType, int facing, BlockType front,
            int frontMetadata, World.MovingPistonState movingHead) {
        boolean movingHeadCarriesExtension = front == BlockType.MOVING_PISTON
                && movingHead != null
                && movingHead.carriedType() == BlockType.PISTON_HEAD;
        int metadata = movingHeadCarriesExtension ? movingHead.carriedMetadata() : frontMetadata;
        if ((metadata & 7) != facing) {
            metadata = facing;
        }
        if (pistonType == BlockType.STICKY_PISTON) {
            metadata |= PISTON_HEAD_STICKY_BIT;
        }
        return metadata;
    }

    private static boolean hasLoadedPistonPowerQuery(World world, int x, int y, int z, int facing) {
        if (!world.isChunkGeneratedForBlock(x, z)) {
            return false;
        }
        for (int[] dir : DIRS) {
            if (dir[3] == facing) {
                continue;
            }
            if (!world.isChunkGeneratedForBlock(x + dir[0], z + dir[2])) {
                return false;
            }
        }
        return world.isChunkGeneratedForBlock(x, z);
    }

    private static boolean shouldStickyPistonPull(World world, int x, int y, int z) {
        StickyPistonExtension extension = removeStickyPistonExtension(world, x, y, z);
        if (extension == null) {
            return true;
        }
        boolean shortPulse = world.getBlockTickClock() - extension.tick() < STICKY_PISTON_SHORT_PULSE_TICKS;
        return !shortPulse || !extension.movedBlock();
    }

    private static StickyPistonExtension removeStickyPistonExtension(World world, int x, int y, int z) {
        Map<Long, StickyPistonExtension> extensions = STICKY_PISTON_EXTENSIONS.get(world);
        if (extensions == null) {
            return null;
        }
        StickyPistonExtension extension = extensions.remove(pack(x, y, z));
        if (extensions.isEmpty()) {
            STICKY_PISTON_EXTENSIONS.remove(world);
        }
        return extension;
    }

    private static Map<Long, StickyPistonExtension> stickyPistonExtensions(World world) {
        return STICKY_PISTON_EXTENSIONS.computeIfAbsent(world, ignored -> new HashMap<>());
    }

    private static boolean isPistonFacing(int face) {
        return face == Block.FACE_TOP || face == Block.FACE_BOTTOM || face == Block.FACE_NORTH
                || face == Block.FACE_SOUTH || face == Block.FACE_EAST || face == Block.FACE_WEST;
    }

    private static boolean isPistonPowered(World world, int x, int y, int z, int facing) {
        for (int[] dir : DIRS) {
            if (dir[3] == facing) {
                continue;
            }
            if (isPowerProvidedTo(world, x + dir[0], y + dir[1], z + dir[2], opposite(dir[3]))) {
                return true;
            }
        }
        return isBlockPowered(world, x, y + 1, z);
    }

    private static boolean isPowerProvidedTo(World world, int x, int y, int z, int towardFace) {
        BlockType type = world.getBlockIfLoaded(x, y, z, BlockType.AIR);
        int metadata = world.getBlockMetadataIfLoaded(x, y, z, 0);
        return getDirectWeakPower(world, x, y, z, type, metadata, towardFace) > 0
                || getStrongPower(world, x, y, z, type, metadata, towardFace) > 0
                || isOpaqueBlockRelayingDirectPower(world, x, y, z);
    }

    private static PistonPushResult tryPushBlocks(World world, int x, int y, int z, int dx, int dy, int dz) {
        int count = 0;
        int cx = x;
        int cy = y;
        int cz = z;
        while (count <= PISTON_MAX_PUSH_BLOCKS) {
            if (cy < 0 || cy >= Chunk.HEIGHT) {
                return new PistonPushResult(false, false);
            }
            BlockType type = world.getBlockIfLoaded(cx, cy, cz, BlockType.BEDROCK);
            int metadata = world.getBlockMetadataIfLoaded(cx, cy, cz, 0);
            PistonMobility mobility = pistonMobility(type, metadata);
            if (mobility == PistonMobility.CLEAR) {
                break;
            }
            if (mobility == PistonMobility.DESTROY) {
                world.breakBlock(cx, cy, cz, true);
                break;
            }
            if (mobility == PistonMobility.BLOCK) {
                return new PistonPushResult(false, false);
            }
            count++;
            if (count > PISTON_MAX_PUSH_BLOCKS) {
                return new PistonPushResult(false, false);
            }
            cx += dx;
            cy += dy;
            cz += dz;
        }
        for (int i = count - 1; i >= 0; i--) {
            int sx = x + dx * i;
            int sy = y + dy * i;
            int sz = z + dz * i;
            int tx = sx + dx;
            int ty = sy + dy;
            int tz = sz + dz;
            BlockType moved = world.getBlockIfLoaded(sx, sy, sz, BlockType.AIR);
            int movedMetadata = world.getBlockMetadataIfLoaded(sx, sy, sz, 0);
            world.pushEntitiesIntersectingBlock(tx, ty, tz, dx, dy, dz);
            world.setBlockIfLoaded(sx, sy, sz, BlockType.AIR, 0);
            world.startMovingPiston(tx, ty, tz, directionToFace(dx, dy, dz),
                    moved, movedMetadata,
                    moved, movedMetadata,
                    sx, sy, sz,
                    tx, ty, tz);
        }
        return new PistonPushResult(true, count > 0);
    }

    private static PistonPullResult takeStickyBlock(World world, int frontX, int frontY, int frontZ,
            int dx, int dy, int dz) {
        int sx = frontX + dx;
        int sy = frontY + dy;
        int sz = frontZ + dz;
        BlockType pulled = world.getBlockIfLoaded(sx, sy, sz, BlockType.AIR);
        int metadata = world.getBlockMetadataIfLoaded(sx, sy, sz, 0);
        if (sy < 0 || sy >= Chunk.HEIGHT || pistonMobility(pulled, metadata) != PistonMobility.MOVE) {
            return new PistonPullResult(BlockType.AIR, 0);
        }
        world.setBlockIfLoaded(sx, sy, sz, BlockType.AIR, 0);
        return new PistonPullResult(pulled, metadata);
    }

    private static PistonMobility pistonMobility(BlockType type, int metadata) {
        if (PISTON_DISPLACEABLE.contains(type)) {
            return PistonMobility.CLEAR;
        }
        if (PISTON_DESTROY_ON_PUSH.contains(type)) {
            return PistonMobility.DESTROY;
        }
        if (PISTON_IMMOVABLE.contains(type)
                || type.getHardness() < 0.0f
                || type.hasTileEntity()) {
            return PistonMobility.BLOCK;
        }
        if ((type == BlockType.PISTON || type == BlockType.STICKY_PISTON)
                && (metadata & PISTON_EXTENDED_BIT) != 0) {
            return PistonMobility.BLOCK;
        }
        return PistonMobility.MOVE;
    }

    private static void updateDispenser(World world, int x, int y, int z, int metadata) {
        boolean powered = isDispenserPowered(world, x, y, z);
        boolean triggered = (metadata & POWERED_BIT) != 0;
        if (powered && !triggered) {
            world.setBlockIfLoaded(x, y, z, BlockType.DISPENSER, metadata | POWERED_BIT);
            TileEntity tile = world.getTileEntity(x, y, z);
            if (tile instanceof DispenserTileEntity dispenser) {
                dispenser.dispense(world);
            }
        } else if (!powered && triggered) {
            world.setBlockIfLoaded(x, y, z, BlockType.DISPENSER, metadata & ~POWERED_BIT);
        }
    }

    private static boolean isDispenserPowered(World world, int x, int y, int z) {
        return isBlockPowered(world, x, y, z) || isBlockPowered(world, x, y + 1, z);
    }

    private static void updateNoteBlock(World world, int x, int y, int z, int metadata) {
        boolean powered = isBlockPowered(world, x, y, z);
        boolean triggered = (metadata & POWERED_BIT) != 0;
        TileEntity tile = world.getTileEntity(x, y, z);
        if (powered && !triggered) {
            world.setBlockIfLoaded(x, y, z, BlockType.NOTE_BLOCK, metadata | POWERED_BIT);
            if (tile instanceof NoteBlockTileEntity note) {
                note.play(world);
            }
        } else if (!powered && triggered) {
            world.setBlockIfLoaded(x, y, z, BlockType.NOTE_BLOCK, metadata & ~POWERED_BIT);
        }
    }

    private static boolean isBlockStronglyPowered(World world, int x, int y, int z) {
        for (int[] dir : DIRS) {
            int nx = x + dir[0];
            int ny = y + dir[1];
            int nz = z + dir[2];
            int face = opposite(dir[3]);
            if (getStrongPower(world, nx, ny, nz, face) > 0) {
                return true;
            }
        }
        return false;
    }

    public static boolean isBlockPowered(World world, int x, int y, int z) {
        if (isBlockDirectlyPowered(world, x, y, z)) {
            return true;
        }
        for (int[] dir : DIRS) {
            int nx = x + dir[0];
            int ny = y + dir[1];
            int nz = z + dir[2];
            if (isOpaqueBlockRelayingDirectPower(world, nx, ny, nz)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isBlockDirectlyPowered(World world, int x, int y, int z) {
        for (int[] dir : DIRS) {
            int nx = x + dir[0];
            int ny = y + dir[1];
            int nz = z + dir[2];
            int face = opposite(dir[3]);
            BlockType type = world.getBlockIfLoaded(nx, ny, nz, BlockType.AIR);
            int metadata = world.getBlockMetadataIfLoaded(nx, ny, nz, 0);
            if (getDirectWeakPower(world, nx, ny, nz, type, metadata, face) > 0
                    || getStrongPower(world, nx, ny, nz, type, metadata, face) > 0) {
                return true;
            }
        }
        return false;
    }

    private static boolean isOpaqueBlockRelayingDirectPower(World world, int x, int y, int z) {
        BlockType type = world.getBlockIfLoaded(x, y, z, BlockType.AIR);
        return BlockShape.isOpaqueCube(type) && isBlockDirectlyPowered(world, x, y, z);
    }

    public static void scheduleAround(World world, int x, int y, int z) {
        for (int[] dir : DIRS) {
            scheduleAtAndQuasiConnectedMechanisms(world, x + dir[0], y + dir[1], z + dir[2]);
        }
        int[][] extra = { { 2, 0, 0 }, { -2, 0, 0 }, { 0, 0, 2 }, { 0, 0, -2 } };
        for (int[] dir : extra) {
            scheduleAtAndQuasiConnectedMechanisms(world, x + dir[0], y + dir[1], z + dir[2]);
        }
        scheduleAtAndQuasiConnectedMechanisms(world, x, y, z);
        scheduleRelayBackedQuasiPowerQueries(world, x, y, z);
        scheduleDustNeighborsAround(world, x, y, z);
        scheduleRailNeighborsAround(world, x, y, z);
    }

    public static void scheduleAt(World world, int x, int y, int z) {
        BlockType type = world.getBlockIfLoaded(x, y, z, BlockType.AIR);
        if (type == BlockType.NOTE_BLOCK) {
            tick(world, x, y, z, type);
            return;
        }
        if (isRedstoneTickable(type)) {
            int metadata = world.getBlockMetadataIfLoaded(x, y, z, 0);
            world.scheduleBlockTick(x, y, z, type, getTickDelay(type, metadata));
        }
        if (type != BlockType.AIR && BlockShape.canSupportAttached(type)) {
            scheduleAttachedTickables(world, x, y, z);
        }
    }

    private static void scheduleAtAndQuasiConnectedMechanisms(World world, int x, int y, int z) {
        scheduleAt(world, x, y, z);
        scheduleMechanismBelowPowerQuery(world, x, y, z);
    }

    private static void scheduleMechanismBelowPowerQuery(World world, int x, int y, int z) {
        BlockType type = world.getBlockIfLoaded(x, y - 1, z, BlockType.AIR);
        if (!usesQuasiPowerQueryAbove(type)) {
            return;
        }
        int metadata = world.getBlockMetadataIfLoaded(x, y - 1, z, 0);
        world.scheduleBlockTick(x, y - 1, z, type, getTickDelay(type, metadata));
    }

    private static boolean usesQuasiPowerQueryAbove(BlockType type) {
        return type == BlockType.PISTON
                || type == BlockType.STICKY_PISTON
                || type == BlockType.DISPENSER;
    }

    private static void scheduleRelayBackedQuasiPowerQueries(World world, int x, int y, int z) {
        scheduleQuasiPowerQueriesBesideRelay(world, x, y, z);
        for (int[] dir : DIRS) {
            scheduleQuasiPowerQueriesBesideRelay(world, x + dir[0], y + dir[1], z + dir[2]);
        }
    }

    private static void scheduleQuasiPowerQueriesBesideRelay(World world, int x, int y, int z) {
        BlockType type = world.getBlockIfLoaded(x, y, z, BlockType.AIR);
        if (!BlockShape.isOpaqueCube(type)) {
            return;
        }
        for (int[] dir : DIRS) {
            scheduleMechanismBelowPowerQuery(world, x + dir[0], y + dir[1], z + dir[2]);
        }
    }

    private static void scheduleAttachedTickables(World world, int x, int y, int z) {
        for (int[] dir : DIRS) {
            int nx = x + dir[0];
            int ny = y + dir[1];
            int nz = z + dir[2];
            BlockType type = world.getBlockIfLoaded(nx, ny, nz, BlockType.AIR);
            if (isRedstoneTickable(type)) {
                int metadata = world.getBlockMetadataIfLoaded(nx, ny, nz, 0);
                world.scheduleBlockTick(nx, ny, nz, type, getTickDelay(type, metadata));
            }
        }
    }

    private static void scheduleDustNeighborsAround(World world, int x, int y, int z) {
        for (int[] offset : DUST_NEIGHBOR_OFFSETS) {
            scheduleRedstoneWireIfPresent(world, x + offset[0], y + offset[1], z + offset[2]);
        }
    }

    private static void scheduleRailNeighborsAround(World world, int x, int y, int z) {
        for (int[] offset : RAIL_NEIGHBOR_OFFSETS) {
            int rx = x + offset[0];
            int ry = y + offset[1];
            int rz = z + offset[2];
            BlockType type = world.getBlockIfLoaded(rx, ry, rz, BlockType.AIR);
            if (RailShapeResolver.isRail(type)) {
                world.scheduleBlockTick(rx, ry, rz, type, 0);
                RailShapeResolver.scheduleConnectedRailTicks(world, rx, ry, rz);
            }
        }
    }

    public static void updateRailDetectorForCart(World world, MinecartEntity cart) {
        if (cart == null || cart.getBoundingBox() == null) {
            return;
        }
        AABB box = cart.getBoundingBox();
        int minX = (int) Math.floor(box.getMin().x);
        int maxX = (int) Math.floor(box.getMax().x);
        int minY = (int) Math.floor(box.getMin().y);
        int maxY = (int) Math.floor(box.getMax().y);
        int minZ = (int) Math.floor(box.getMin().z);
        int maxZ = (int) Math.floor(box.getMax().z);
        for (int y = minY; y <= maxY; y++) {
            for (int z = minZ; z <= maxZ; z++) {
                for (int x = minX; x <= maxX; x++) {
                    if (world.getBlockIfLoaded(x, y, z, BlockType.AIR) == BlockType.DETECTOR_RAIL
                            && detectorRailSearchBox(x, y, z).intersects(box)) {
                        world.scheduleBlockTick(x, y, z, BlockType.DETECTOR_RAIL, 0);
                    }
                }
            }
        }
    }

    private static AABB detectorRailSearchBox(int x, int y, int z) {
        return new AABB(
                x + DETECTOR_RAIL_MINECART_INSET,
                y,
                z + DETECTOR_RAIL_MINECART_INSET,
                x + 1.0f - DETECTOR_RAIL_MINECART_INSET,
                y + 1.0f - DETECTOR_RAIL_MINECART_INSET,
                z + 1.0f - DETECTOR_RAIL_MINECART_INSET);
    }

    public static void dropDispenserItem(World world, int x, int y, int z, int metadata, ItemStack stack) {
        int face = metadataToOutputFace(metadata);
        Random random = world.getRandom();
        float ox = x + 0.5f + faceToDx(face) * 0.7f;
        float oy = y + 0.5f + faceToDy(face) * 0.7f;
        float oz = z + 0.5f + faceToDz(face) * 0.7f;
        ItemType itemType = stack.getType();
        if (itemType == ItemType.ARROW) {
            float[] motion = dispenserProjectileMotion(face, 1.1f, 6.0f, random);
            world.spawnArrow(ox, oy, oz, motion[0], motion[1], motion[2], null, false, 2.0f);
        } else if (itemType == ItemType.EGG || itemType == ItemType.SNOWBALL) {
            float[] motion = dispenserProjectileMotion(face, 1.1f, 6.0f, random);
            world.spawnThrownItemProjectile(ox, oy, oz, motion[0], motion[1], motion[2], itemType, null);
        } else if (itemType == ItemType.POTION && stack.getPotionData() != null && stack.getPotionData().splash()) {
            float[] motion = dispenserProjectileMotion(face, 1.375f, 3.0f, random);
            world.spawnSplashPotion(ox, oy, oz, motion[0], motion[1], motion[2], null, stack.getPotionData());
        } else {
            float dropX = x + 0.5f + faceToDx(face) * DISPENSER_GENERIC_OFFSET;
            float dropY = y + 0.5f + faceToDy(face) * DISPENSER_GENERIC_OFFSET + DISPENSER_GENERIC_Y_OFFSET;
            float dropZ = z + 0.5f + faceToDz(face) * DISPENSER_GENERIC_OFFSET;
            float[] motion = dispenserGenericItemMotion(face, random);
            world.spawnThrownStack(dropX, dropY, dropZ, stack, motion[0], motion[1], motion[2]);
        }
    }

    static float[] dispenserGenericItemMotion(int face, Random random) {
        random = dispenserRandom(random);
        double speed = random.nextDouble() * DISPENSER_GENERIC_RANDOM_SPEED + DISPENSER_GENERIC_MIN_SPEED;
        double dx = faceToDx(face) * speed;
        double dy = 0.20000000298023224D;
        double dz = faceToDz(face) * speed;
        dx += random.nextGaussian() * DISPENSER_SPREAD_SCALE * DISPENSER_GENERIC_SPREAD;
        dy += random.nextGaussian() * DISPENSER_SPREAD_SCALE * DISPENSER_GENERIC_SPREAD;
        dz += random.nextGaussian() * DISPENSER_SPREAD_SCALE * DISPENSER_GENERIC_SPREAD;
        return new float[] { (float) dx, (float) dy, (float) dz };
    }

    private static float[] dispenserProjectileMotion(int face, float speed, float spread, Random random) {
        random = dispenserRandom(random);
        double dx = faceToDx(face);
        double dy = faceToDy(face);
        double dz = faceToDz(face);
        if (dy == 0.0) {
            dy = 0.1;
        }
        double length = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (length < 0.0001) {
            dz = -1.0;
            length = 1.0;
        }
        dx /= length;
        dy /= length;
        dz /= length;
        dx += random.nextGaussian() * DISPENSER_SPREAD_SCALE * spread;
        dy += random.nextGaussian() * DISPENSER_SPREAD_SCALE * spread;
        dz += random.nextGaussian() * DISPENSER_SPREAD_SCALE * spread;
        dx *= speed;
        dy *= speed;
        dz *= speed;
        return new float[] { (float) dx, (float) dy, (float) dz };
    }

    private static Random dispenserRandom(Random random) {
        return random == null ? new Random(0L) : random;
    }

    public static int metadataToOutputFace(int metadata) {
        int face = metadata & 7;
        return switch (face) {
            case Block.FACE_NORTH, Block.FACE_SOUTH, Block.FACE_EAST, Block.FACE_WEST -> face;
            default -> Block.FACE_NORTH;
        };
    }

    private static long pack(int x, int y, int z) {
        long lx = ((long) x & 0x3ffffffL) << 38;
        long lz = ((long) z & 0x3ffffffL) << 12;
        long ly = (long) y & 0xfffL;
        return lx | lz | ly;
    }
}
