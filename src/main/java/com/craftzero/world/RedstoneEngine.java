package com.craftzero.world;

import com.craftzero.entity.MinecartEntity;
import com.craftzero.inventory.ItemStack;
import com.craftzero.world.tile.DispenserTileEntity;
import com.craftzero.world.tile.JukeboxTileEntity;
import com.craftzero.world.tile.NoteBlockTileEntity;
import com.craftzero.world.tile.TileEntity;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;

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
    public static final int RAIL_POWERED_BIT = 8;
    public static final int BUTTON_DELAY_TICKS = 20;
    public static final int WOOD_BUTTON_DELAY_TICKS = 30;
    public static final int REPEATER_BASE_DELAY_TICKS = 2;
    public static final int TORCH_DELAY_TICKS = 2;
    public static final int REDSTONE_UPDATE_DELAY_TICKS = 1;
    public static final int PRESSURE_PLATE_DELAY_TICKS = 10;
    public static final int DETECTOR_RAIL_DELAY_TICKS = 10;
    public static final int TNT_FUSE_TICKS = 80;

    private static final int[][] DIRS = {
            { 0, 1, 0, Block.FACE_TOP },
            { 0, -1, 0, Block.FACE_BOTTOM },
            { 0, 0, -1, Block.FACE_NORTH },
            { 0, 0, 1, Block.FACE_SOUTH },
            { 1, 0, 0, Block.FACE_EAST },
            { -1, 0, 0, Block.FACE_WEST }
    };

    private static final Map<Long, ArrayDeque<Long>> TORCH_TOGGLES = new HashMap<>();

    private RedstoneEngine() {
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
                || type == BlockType.DISPENSER
                || type == BlockType.NOTE_BLOCK
                || type == BlockType.JUKEBOX;
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
        if (type == BlockType.REDSTONE_WIRE) {
            return Math.max(0, Math.min(15, metadata & 15));
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
        if (type.isSolid() && !type.isTransparent()) {
            return isBlockStronglyPowered(world, x, y, z) ? 15 : 0;
        }
        return 0;
    }

    private static int getStrongPower(World world, int x, int y, int z, BlockType type, int metadata, int towardFace) {
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
            world.setBlockIfLoaded(x, y, z, type, metadata ^ POWERED_BIT);
            world.scheduleMechanismUpdatesAround(x, y, z);
            return true;
        }
        if (type == BlockType.STONE_BUTTON) {
            if ((metadata & POWERED_BIT) == 0) {
                world.setBlockIfLoaded(x, y, z, type, metadata | POWERED_BIT);
                world.scheduleBlockTick(x, y, z, type, BUTTON_DELAY_TICKS);
                world.scheduleMechanismUpdatesAround(x, y, z);
            }
            return true;
        }
        if (type == BlockType.REDSTONE_REPEATER_OFF || type == BlockType.REDSTONE_REPEATER_ON) {
            int delay = ((metadata & REPEATER_DELAY_MASK) >> REPEATER_DELAY_SHIFT);
            delay = (delay + 1) & 3;
            world.setBlockIfLoaded(x, y, z, type, (metadata & ~REPEATER_DELAY_MASK) | (delay << REPEATER_DELAY_SHIFT));
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
            TileEntity tile = world.getTileEntity(x, y, z);
            if (tile instanceof JukeboxTileEntity jukebox && jukebox.hasRecord()) {
                world.spawnThrownStack(x + 0.5f, y + 1.0f, z + 0.5f, jukebox.removeRecord(), 0.0f, 0.1f, 0.0f);
                return true;
            }
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
            if (type == BlockType.DETECTOR_RAIL) {
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
        if (type == BlockType.JUKEBOX) {
            updateJukebox(world, x, y, z, metadata);
        }
    }

    public static void updateRedstoneWire(World world, int x, int y, int z) {
        if (world.getBlockIfLoaded(x, y, z, BlockType.AIR) != BlockType.REDSTONE_WIRE) {
            return;
        }
        int old = world.getBlockMetadataIfLoaded(x, y, z, 0) & 15;
        int power = Math.max(getBlockInputPower(world, x, y, z), getAdjacentWirePower(world, x, y, z) - 1);
        power = Math.max(0, Math.min(15, power));
        if (old != power) {
            world.setBlockIfLoaded(x, y, z, BlockType.REDSTONE_WIRE, power);
            world.scheduleMechanismUpdatesAround(x, y, z);
        }
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

    private static int getAdjacentWirePower(World world, int x, int y, int z) {
        int power = 0;
        int[][] horizontal = { { 1, 0 }, { -1, 0 }, { 0, 1 }, { 0, -1 } };
        for (int[] dir : horizontal) {
            power = Math.max(power, wirePowerAt(world, x + dir[0], y, z + dir[1]));
            if (world.getBlockIfLoaded(x + dir[0], y, z + dir[1], BlockType.AIR).isAir()) {
                power = Math.max(power, wirePowerAt(world, x + dir[0], y - 1, z + dir[1]));
            } else if (!world.getBlockIfLoaded(x + dir[0], y + 1, z + dir[1], BlockType.BEDROCK).isSolid()) {
                power = Math.max(power, wirePowerAt(world, x + dir[0], y + 1, z + dir[1]));
            }
        }
        return power;
    }

    private static int wirePowerAt(World world, int x, int y, int z) {
        return world.getBlockIfLoaded(x, y, z, BlockType.AIR) == BlockType.REDSTONE_WIRE
                ? world.getBlockMetadataIfLoaded(x, y, z, 0) & 15
                : 0;
    }

    private static void updateRedstoneTorch(World world, int x, int y, int z, BlockType type, int metadata) {
        boolean powered = isTorchAttachedBlockPowered(world, x, y, z, metadata);
        boolean on = type == BlockType.REDSTONE_TORCH_ON;
        if (on && powered) {
            recordTorchToggle(world.getBlockTickClock(), x, y, z);
            world.setBlockIfLoaded(x, y, z, BlockType.REDSTONE_TORCH_OFF, metadata);
            world.scheduleMechanismUpdatesAround(x, y, z);
        } else if (!on && !powered && !isTorchBurnedOut(world.getBlockTickClock(), x, y, z)) {
            world.setBlockIfLoaded(x, y, z, BlockType.REDSTONE_TORCH_ON, metadata);
            world.scheduleMechanismUpdatesAround(x, y, z);
        }
    }

    private static boolean isTorchAttachedBlockPowered(World world, int x, int y, int z, int metadata) {
        int face = metadata == 5 || metadata == 0 ? Block.FACE_BOTTOM : metadata & 7;
        int sx = x + faceToDx(opposite(face));
        int sy = y + faceToDy(opposite(face));
        int sz = z + faceToDz(opposite(face));
        return isBlockPowered(world, sx, sy, sz);
    }

    private static boolean torchPowersFace(int metadata, int towardFace) {
        int attachedFace = metadata == 5 || metadata == 0 ? Block.FACE_BOTTOM : metadata & 7;
        return towardFace != opposite(attachedFace);
    }

    private static void recordTorchToggle(long now, int x, int y, int z) {
        long key = pack(x, y, z);
        ArrayDeque<Long> toggles = TORCH_TOGGLES.computeIfAbsent(key, ignored -> new ArrayDeque<>());
        toggles.addLast(now);
        while (!toggles.isEmpty() && now - toggles.peekFirst() > 60) {
            toggles.removeFirst();
        }
    }

    private static boolean isTorchBurnedOut(long now, int x, int y, int z) {
        ArrayDeque<Long> toggles = TORCH_TOGGLES.get(pack(x, y, z));
        if (toggles == null) {
            return false;
        }
        while (!toggles.isEmpty() && now - toggles.peekFirst() > 60) {
            toggles.removeFirst();
        }
        return toggles.size() >= 8;
    }

    private static void updateRepeater(World world, int x, int y, int z, BlockType type, int metadata) {
        int inputFace = repeaterInputFace(metadata);
        int ix = x + faceToDx(inputFace);
        int iy = y + faceToDy(inputFace);
        int iz = z + faceToDz(inputFace);
        boolean powered = getWeakPower(world, ix, iy, iz, opposite(inputFace)) > 0
                || getStrongPower(world, ix, iy, iz, opposite(inputFace)) > 0;
        if (powered && type == BlockType.REDSTONE_REPEATER_OFF) {
            world.setBlockIfLoaded(x, y, z, BlockType.REDSTONE_REPEATER_ON, metadata);
            world.scheduleMechanismUpdatesAround(x, y, z);
        } else if (!powered && type == BlockType.REDSTONE_REPEATER_ON) {
            world.setBlockIfLoaded(x, y, z, BlockType.REDSTONE_REPEATER_OFF, metadata);
            world.scheduleMechanismUpdatesAround(x, y, z);
        }
    }

    private static void updatePressurePlate(World world, int x, int y, int z, BlockType type, int metadata) {
        boolean occupied = world.hasEntityIntersecting(x + 0.0625f, y, z + 0.0625f,
                x + 0.9375f, y + 0.25f, z + 0.9375f,
                type == BlockType.WOODEN_PRESSURE_PLATE);
        boolean powered = (metadata & 1) != 0;
        if (occupied != powered) {
            world.setBlockIfLoaded(x, y, z, type, occupied ? 1 : 0);
            world.scheduleMechanismUpdatesAround(x, y, z);
        }
        if (occupied) {
            world.scheduleBlockTick(x, y, z, type, PRESSURE_PLATE_DELAY_TICKS);
        }
    }

    private static void updateDetectorRail(World world, int x, int y, int z, int metadata) {
        boolean occupied = world.hasMinecartAt(x, y, z);
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

    private static void updatePoweredOpenable(World world, int x, int y, int z, BlockType type, int metadata) {
        int lowerY = type.isDoor() && BlockShape.isDoorUpper(metadata) ? y - 1 : y;
        int effectiveMetadata = type.isDoor() ? world.getBlockMetadataIfLoaded(x, lowerY, z, metadata) : metadata;
        boolean powered = isBlockPowered(world, x, lowerY, z)
                || (type.isDoor() && isBlockPowered(world, x, lowerY + 1, z));
        boolean open = (effectiveMetadata & DOOR_OPEN_BIT) != 0;
        if (powered != open) {
            world.setBlockIfLoaded(x, lowerY, z, type, effectiveMetadata ^ DOOR_OPEN_BIT);
        }
    }

    private static void updatePiston(World world, int x, int y, int z, BlockType type, int metadata) {
        int facing = metadata & 7;
        if (!isPistonFacing(facing)) {
            facing = Block.FACE_NORTH;
        }
        boolean powered = isBlockPowered(world, x, y, z);
        boolean extended = (metadata & PISTON_EXTENDED_BIT) != 0;
        int fx = faceToDx(facing);
        int fy = faceToDy(facing);
        int fz = faceToDz(facing);
        if (powered && !extended) {
            if (tryPushBlocks(world, x + fx, y + fy, z + fz, fx, fy, fz)) {
                world.setBlockIfLoaded(x, y, z, type, metadata | PISTON_EXTENDED_BIT);
                world.setBlockIfLoaded(x + fx, y + fy, z + fz, BlockType.PISTON_HEAD, facing);
            }
        } else if (!powered && extended) {
            if (world.getBlockIfLoaded(x + fx, y + fy, z + fz, BlockType.AIR) == BlockType.PISTON_HEAD) {
                world.setBlockIfLoaded(x + fx, y + fy, z + fz, BlockType.AIR, 0);
            }
            world.setBlockIfLoaded(x, y, z, type, metadata & ~PISTON_EXTENDED_BIT);
            if (type == BlockType.STICKY_PISTON) {
                pullStickyBlock(world, x + fx, y + fy, z + fz, fx, fy, fz);
            }
        }
    }

    private static boolean isPistonFacing(int face) {
        return face == Block.FACE_TOP || face == Block.FACE_BOTTOM || face == Block.FACE_NORTH
                || face == Block.FACE_SOUTH || face == Block.FACE_EAST || face == Block.FACE_WEST;
    }

    private static boolean tryPushBlocks(World world, int x, int y, int z, int dx, int dy, int dz) {
        int count = 0;
        int cx = x;
        int cy = y;
        int cz = z;
        while (count < 13) {
            BlockType type = world.getBlockIfLoaded(cx, cy, cz, BlockType.BEDROCK);
            if (type == BlockType.AIR || BlockShape.isReplaceable(type)) {
                break;
            }
            if (!canPistonMove(type) || cy < 0 || cy >= Chunk.HEIGHT) {
                return false;
            }
            count++;
            if (count > 12) {
                return false;
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
            world.setBlockIfLoaded(tx, ty, tz, moved, movedMetadata);
            world.setBlockIfLoaded(sx, sy, sz, BlockType.AIR, 0);
        }
        return true;
    }

    private static void pullStickyBlock(World world, int frontX, int frontY, int frontZ, int dx, int dy, int dz) {
        int sx = frontX + dx;
        int sy = frontY + dy;
        int sz = frontZ + dz;
        BlockType pulled = world.getBlockIfLoaded(sx, sy, sz, BlockType.AIR);
        if (pulled == BlockType.AIR || !canPistonMove(pulled)) {
            return;
        }
        int metadata = world.getBlockMetadataIfLoaded(sx, sy, sz, 0);
        world.setBlockIfLoaded(frontX, frontY, frontZ, pulled, metadata);
        world.setBlockIfLoaded(sx, sy, sz, BlockType.AIR, 0);
    }

    private static boolean canPistonMove(BlockType type) {
        return type != BlockType.AIR
                && type != BlockType.BEDROCK
                && type != BlockType.OBSIDIAN
                && type != BlockType.PISTON_HEAD
                && type != BlockType.MOVING_PISTON
                && !type.hasTileEntity();
    }

    private static void updateDispenser(World world, int x, int y, int z, int metadata) {
        boolean powered = isBlockPowered(world, x, y, z);
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

    private static void updateJukebox(World world, int x, int y, int z, int metadata) {
        boolean powered = isBlockPowered(world, x, y, z);
        boolean triggered = (metadata & POWERED_BIT) != 0;
        TileEntity tile = world.getTileEntity(x, y, z);
        if (powered && !triggered) {
            world.setBlockIfLoaded(x, y, z, BlockType.JUKEBOX, metadata | POWERED_BIT);
            if (tile instanceof JukeboxTileEntity jukebox) {
                jukebox.play(world);
            }
        } else if (!powered && triggered) {
            world.setBlockIfLoaded(x, y, z, BlockType.JUKEBOX, metadata & ~POWERED_BIT);
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
        return isBlockPowered(new PowerQuery() {
            @Override
            public int getWeakPower(int px, int py, int pz, int towardFace) {
                return RedstoneEngine.getWeakPower(world, px, py, pz, towardFace);
            }

            @Override
            public int getStrongPower(int px, int py, int pz, int towardFace) {
                return RedstoneEngine.getStrongPower(world, px, py, pz, towardFace);
            }
        }, x, y, z);
    }

    public static void scheduleAround(World world, int x, int y, int z) {
        for (int[] dir : DIRS) {
            scheduleAt(world, x + dir[0], y + dir[1], z + dir[2]);
        }
        int[][] extra = { { 2, 0, 0 }, { -2, 0, 0 }, { 0, 0, 2 }, { 0, 0, -2 } };
        for (int[] dir : extra) {
            scheduleAt(world, x + dir[0], y + dir[1], z + dir[2]);
        }
        scheduleAt(world, x, y, z);
    }

    public static void scheduleAt(World world, int x, int y, int z) {
        BlockType type = world.getBlockIfLoaded(x, y, z, BlockType.AIR);
        if (isRedstoneTickable(type)) {
            int metadata = world.getBlockMetadataIfLoaded(x, y, z, 0);
            world.scheduleBlockTick(x, y, z, type, getTickDelay(type, metadata));
        }
    }

    public static void updateRailDetectorForCart(World world, MinecartEntity cart) {
        int railY = RailShapeResolver.findRailY(world, (int) Math.floor(cart.getX()), (int) Math.floor(cart.getY()),
                (int) Math.floor(cart.getZ()));
        if (railY == Integer.MIN_VALUE) {
            return;
        }
        int x = (int) Math.floor(cart.getX());
        int z = (int) Math.floor(cart.getZ());
        if (world.getBlockIfLoaded(x, railY, z, BlockType.AIR) == BlockType.DETECTOR_RAIL) {
            world.scheduleBlockTick(x, railY, z, BlockType.DETECTOR_RAIL, 0);
        }
    }

    public static void dropDispenserItem(World world, int x, int y, int z, int metadata, ItemStack stack) {
        int face = metadataToOutputFace(metadata);
        float ox = x + 0.5f + faceToDx(face) * 0.7f;
        float oy = y + 0.5f + faceToDy(face) * 0.7f;
        float oz = z + 0.5f + faceToDz(face) * 0.7f;
        float vx = faceToDx(face) * 0.25f;
        float vy = face == Block.FACE_TOP ? 0.25f : face == Block.FACE_BOTTOM ? -0.05f : 0.1f;
        float vz = faceToDz(face) * 0.25f;
        if (stack.getType() == com.craftzero.inventory.ItemType.ARROW) {
            world.spawnArrow(ox, oy, oz, vx * 3.0f, vy * 3.0f, vz * 3.0f, null, false, 2.0f);
        } else if (stack.getType() == com.craftzero.inventory.ItemType.TNT) {
            world.spawnPrimedTnt(ox, oy, oz, TNT_FUSE_TICKS, vx, 0.2f, vz);
        } else {
            world.spawnThrownStack(ox, oy, oz, stack, vx, vy, vz);
        }
    }

    public static int metadataToOutputFace(int metadata) {
        int face = metadata & 7;
        return isPistonFacing(face) ? face : Block.FACE_NORTH;
    }

    private static long pack(int x, int y, int z) {
        long lx = ((long) x & 0x3ffffffL) << 38;
        long lz = ((long) z & 0x3ffffffL) << 12;
        long ly = (long) y & 0xfffL;
        return lx | lz | ly;
    }
}
