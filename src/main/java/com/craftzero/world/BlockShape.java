package com.craftzero.world;

import com.craftzero.physics.AABB;

import java.util.ArrayList;
import java.util.List;

/**
 * Release 1.0-style block shapes for rendering, ray selection, and collision.
 * Cuboids are local to the block position.
 */
public final class BlockShape {
    public static final Cuboid FULL = new Cuboid(0, 0, 0, 1, 1, 1);
    private static final List<Cuboid> FULL_LIST = List.of(FULL);
    private static final List<Cuboid> EMPTY_LIST = List.of();

    private static final float ONE = 1.0f / 16.0f;

    private BlockShape() {
    }

    public record Cuboid(float minX, float minY, float minZ, float maxX, float maxY, float maxZ) {
        public AABB toAabb(int x, int y, int z) {
            return new AABB(x + minX, y + minY, z + minZ, x + maxX, y + maxY, z + maxZ);
        }

        public boolean isFullCube() {
            return minX == 0 && minY == 0 && minZ == 0 && maxX == 1 && maxY == 1 && maxZ == 1;
        }
    }

    public interface BlockContext {
        BlockType getBlock(int dx, int dy, int dz);

        int getMetadata(int dx, int dy, int dz);
    }

    public static List<Cuboid> getRenderBoxes(BlockType type, int metadata, BlockContext context) {
        if (type == BlockType.AIR) {
            return EMPTY_LIST;
        }
        if (type.isFluid()) {
            return List.of(new Cuboid(0, 0, 0, 1, 14 * ONE, 1));
        }
        if (type.isPlant()) {
            return plantSelectionBoxes(type);
        }
        if (type == BlockType.FIRE) {
            return List.of(new Cuboid(0, 0, 0, 1, 1, 1));
        }
        if (type == BlockType.CHEST) {
            return List.of(new Cuboid(ONE, 0, ONE, 15 * ONE, 14 * ONE, 15 * ONE));
        }
        if (type == BlockType.TORCH) {
            return torchBoxes(metadata);
        }
        if (type == BlockType.REDSTONE_WIRE || type == BlockType.RAIL || type == BlockType.POWERED_RAIL
                || type == BlockType.DETECTOR_RAIL || type == BlockType.REDSTONE_REPEATER_OFF
                || type == BlockType.REDSTONE_REPEATER_ON) {
            return List.of(new Cuboid(0, 0, 0, 1, ONE, 1));
        }
        if (type == BlockType.STONE_PRESSURE_PLATE || type == BlockType.WOODEN_PRESSURE_PLATE) {
            return pressurePlateBoxes(metadata);
        }
        if (type == BlockType.STONE_BUTTON) {
            return buttonBoxes(metadata);
        }
        if (type == BlockType.LEVER) {
            return leverBoxes(metadata);
        }
        if (type == BlockType.REDSTONE_TORCH_OFF || type == BlockType.REDSTONE_TORCH_ON) {
            return torchBoxes(metadata);
        }
        if (type == BlockType.LADDER) {
            return wallPlate(metadata, ONE);
        }
        if (type == BlockType.STANDING_SIGN) {
            return standingSignBoxes(metadata);
        }
        if (type == BlockType.WALL_SIGN) {
            return wallSignBoxes(metadata);
        }
        if (type.isDoor()) {
            return List.of(doorBox(effectiveDoorMetadata(metadata, context)));
        }
        if (type == BlockType.TRAPDOOR) {
            return List.of(trapdoorBox(metadata));
        }
        if (type == BlockType.BED) {
            return List.of(new Cuboid(0, 0, 0, 1, 9 * ONE, 1));
        }
        if (type == BlockType.STONE_SLAB) {
            return List.of(new Cuboid(0, 0, 0, 1, 0.5f, 1));
        }
        if (type.isStairs()) {
            return stairBoxes(metadata);
        }
        if (type.isFence()) {
            return fenceBoxes(context);
        }
        if (type.isFenceGate()) {
            return fenceGateBoxes(metadata);
        }
        return FULL_LIST;
    }

    public static List<Cuboid> getCollisionBoxes(BlockType type, int metadata, BlockContext context) {
        if (type == BlockType.AIR || type.isFluid() || type == BlockType.FIRE
                || type == BlockType.TORCH || type == BlockType.REDSTONE_TORCH_OFF
                || type == BlockType.REDSTONE_TORCH_ON || type == BlockType.LADDER || type.isSign()
                || type.isPlant() || BlockBehavior.of(type) == BlockBehavior.RAIL
                || BlockBehavior.of(type) == BlockBehavior.REDSTONE_DUST
                || BlockBehavior.of(type) == BlockBehavior.REDSTONE_REPEATER
                || type == BlockType.LEVER || type == BlockType.STONE_BUTTON
                || type == BlockType.STONE_PRESSURE_PLATE || type == BlockType.WOODEN_PRESSURE_PLATE) {
            return EMPTY_LIST;
        }
        if (type.isFence()) {
            return fenceBoxes(context);
        }
        if (type.isFenceGate() && isFenceGateOpen(metadata)) {
            return EMPTY_LIST;
        }
        return getRenderBoxes(type, metadata, context);
    }

    public static List<Cuboid> getSelectionBoxes(BlockType type, int metadata, BlockContext context) {
        if (type == BlockType.AIR || type.isFluid() || type == BlockType.FIRE) {
            return EMPTY_LIST;
        }
        return getRenderBoxes(type, metadata, context);
    }

    public static boolean isFullCube(BlockType type, int metadata) {
        return type != BlockType.AIR && getRenderBoxes(type, metadata, emptyContext()).stream()
                .anyMatch(Cuboid::isFullCube)
                && getRenderBoxes(type, metadata, emptyContext()).size() == 1;
    }

    public static boolean isOpaqueCube(BlockType type) {
        return type.isSolid() && !type.isTransparent() && isFullCube(type, 0);
    }

    public static boolean canSupportAttached(BlockType type) {
        return isOpaqueCube(type) || type == BlockType.GLASS || type == BlockType.CHEST || type.isFurnace();
    }

    public static boolean canFenceConnectTo(BlockType type) {
        return type.isFence() || type.isFenceGate() || isOpaqueCube(type);
    }

    public static boolean isReplaceable(BlockType type) {
        return type == BlockType.AIR || type.isFluid() || type == BlockType.FIRE;
    }

    public static boolean canPlaceAt(BlockType type, int metadata, BlockContext context) {
        if (type == BlockType.TORCH || type == BlockType.REDSTONE_TORCH_OFF || type == BlockType.REDSTONE_TORCH_ON) {
            return hasTorchSupport(metadata, context);
        }
        if (type == BlockType.REDSTONE_WIRE || type == BlockType.RAIL || type == BlockType.POWERED_RAIL
                || type == BlockType.DETECTOR_RAIL || type == BlockType.REDSTONE_REPEATER_OFF
                || type == BlockType.REDSTONE_REPEATER_ON || type == BlockType.STONE_PRESSURE_PLATE
                || type == BlockType.WOODEN_PRESSURE_PLATE) {
            return canSupportAbove(context);
        }
        if (type == BlockType.LEVER || type == BlockType.STONE_BUTTON) {
            return canSupportHorizontalIndex(metadata & 7, context) || canSupportAbove(context);
        }
        if (type == BlockType.LADDER || type == BlockType.WALL_SIGN) {
            return canSupportFace(metadata, context);
        }
        if (type.isPlant()) {
            return canPlantStay(type, context);
        }
        if (type == BlockType.STANDING_SIGN || type == BlockType.BED || type == BlockType.WOODEN_DOOR
                || type == BlockType.IRON_DOOR) {
            return canSupportAbove(context);
        }
        if (type == BlockType.CACTUS) {
            return canCactusStay(context);
        }
        if (type == BlockType.TRAPDOOR) {
            return canSupportHorizontalIndex(metadata & 3, context);
        }
        return true;
    }

    public static boolean usesCrossedSprite(BlockType type) {
        return type.isPlant() || type == BlockType.FIRE;
    }

    public static boolean blocksPlacementAgainst(BlockType type, int face) {
        return face == Block.FACE_TOP && (type == BlockType.TORCH || type == BlockType.FIRE || type.isPlant());
    }

    public static boolean canFallThrough(BlockType type) {
        return type == BlockType.AIR || type.isFluid() || type == BlockType.FIRE
                || type == BlockType.TORCH || type == BlockType.REDSTONE_TORCH_OFF
                || type == BlockType.REDSTONE_TORCH_ON || type.isPlant()
                || BlockBehavior.of(type) == BlockBehavior.RAIL
                || BlockBehavior.of(type) == BlockBehavior.REDSTONE_DUST
                || BlockBehavior.of(type) == BlockBehavior.REDSTONE_REPEATER
                || type == BlockType.LEVER || type == BlockType.STONE_BUTTON
                || type == BlockType.STONE_PRESSURE_PLATE || type == BlockType.WOODEN_PRESSURE_PLATE;
    }

    public static int oppositeFace(int face) {
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

    public static boolean isDoorUpper(int metadata) {
        return (metadata & 8) != 0;
    }

    public static boolean isBedHead(int metadata) {
        return (metadata & 8) != 0;
    }

    public static boolean isFenceGateOpen(int metadata) {
        return (metadata & 4) != 0;
    }

    public static boolean isTrapdoorOpen(int metadata) {
        return (metadata & 4) != 0;
    }

    public static boolean isTrapdoorTop(int metadata) {
        return (metadata & 8) != 0;
    }

    private static BlockContext emptyContext() {
        return new BlockContext() {
            @Override
            public BlockType getBlock(int dx, int dy, int dz) {
                return BlockType.AIR;
            }

            @Override
            public int getMetadata(int dx, int dy, int dz) {
                return 0;
            }
        };
    }

    private static List<Cuboid> torchBoxes(int metadata) {
        float min = 7 * ONE;
        float max = 9 * ONE;
        if (metadata == 5 || metadata == 0) {
            return List.of(new Cuboid(min, 0, min, max, 10 * ONE, max));
        }
        return switch (metadata) {
            case Block.FACE_NORTH -> List.of(new Cuboid(min, 3 * ONE, 0, max, 13 * ONE, 3 * ONE));
            case Block.FACE_SOUTH -> List.of(new Cuboid(min, 3 * ONE, 13 * ONE, max, 13 * ONE, 1));
            case Block.FACE_EAST -> List.of(new Cuboid(13 * ONE, 3 * ONE, min, 1, 13 * ONE, max));
            case Block.FACE_WEST -> List.of(new Cuboid(0, 3 * ONE, min, 3 * ONE, 13 * ONE, max));
            default -> List.of(new Cuboid(min, 0, min, max, 10 * ONE, max));
        };
    }

    private static List<Cuboid> plantSelectionBoxes(BlockType type) {
        float min = 3 * ONE;
        float max = 13 * ONE;
        float height = type == BlockType.DEAD_BUSH ? 13 * ONE : 0.8f;
        return List.of(new Cuboid(min, 0, min, max, height, max));
    }

    private static List<Cuboid> wallPlate(int metadata, float thickness) {
        return switch (metadata) {
            case Block.FACE_NORTH -> List.of(new Cuboid(0, 0, 0, 1, 1, thickness));
            case Block.FACE_SOUTH -> List.of(new Cuboid(0, 0, 1 - thickness, 1, 1, 1));
            case Block.FACE_EAST -> List.of(new Cuboid(1 - thickness, 0, 0, 1, 1, 1));
            case Block.FACE_WEST -> List.of(new Cuboid(0, 0, 0, thickness, 1, 1));
            default -> List.of(new Cuboid(0, 0, 0, 1, 1, thickness));
        };
    }

    private static List<Cuboid> standingSignBoxes(int metadata) {
        List<Cuboid> boxes = new ArrayList<>();
        boxes.add(new Cuboid(7 * ONE, 0, 7 * ONE, 9 * ONE, 9 * ONE, 9 * ONE));
        if ((metadata & 15) >= 4 && (metadata & 15) < 12) {
            boxes.add(new Cuboid(7 * ONE, 8 * ONE, 2 * ONE, 9 * ONE, 16 * ONE, 14 * ONE));
        } else {
            boxes.add(new Cuboid(2 * ONE, 8 * ONE, 7 * ONE, 14 * ONE, 16 * ONE, 9 * ONE));
        }
        return boxes;
    }

    private static List<Cuboid> wallSignBoxes(int metadata) {
        float t = ONE;
        return switch (metadata) {
            case Block.FACE_NORTH -> List.of(new Cuboid(2 * ONE, 4 * ONE, 0, 14 * ONE, 12 * ONE, t));
            case Block.FACE_SOUTH -> List.of(new Cuboid(2 * ONE, 4 * ONE, 1 - t, 14 * ONE, 12 * ONE, 1));
            case Block.FACE_EAST -> List.of(new Cuboid(1 - t, 4 * ONE, 2 * ONE, 1, 12 * ONE, 14 * ONE));
            case Block.FACE_WEST -> List.of(new Cuboid(0, 4 * ONE, 2 * ONE, t, 12 * ONE, 14 * ONE));
            default -> List.of(new Cuboid(2 * ONE, 4 * ONE, 0, 14 * ONE, 12 * ONE, t));
        };
    }

    private static List<Cuboid> pressurePlateBoxes(int metadata) {
        float height = (metadata & 1) != 0 ? ONE / 2.0f : ONE;
        return List.of(new Cuboid(ONE, 0, ONE, 15 * ONE, height, 15 * ONE));
    }

    private static List<Cuboid> buttonBoxes(int metadata) {
        float protrusion = (metadata & 8) != 0 ? ONE : 2 * ONE;
        return switch (metadata & 7) {
            case Block.FACE_NORTH -> List.of(new Cuboid(5 * ONE, 6 * ONE, 0, 11 * ONE, 10 * ONE, protrusion));
            case Block.FACE_SOUTH -> List.of(new Cuboid(5 * ONE, 6 * ONE, 1 - protrusion, 11 * ONE, 10 * ONE, 1));
            case Block.FACE_EAST -> List.of(new Cuboid(1 - protrusion, 6 * ONE, 5 * ONE, 1, 10 * ONE, 11 * ONE));
            case Block.FACE_WEST -> List.of(new Cuboid(0, 6 * ONE, 5 * ONE, protrusion, 10 * ONE, 11 * ONE));
            default -> List.of(new Cuboid(5 * ONE, 6 * ONE, 0, 11 * ONE, 10 * ONE, protrusion));
        };
    }

    private static List<Cuboid> leverBoxes(int metadata) {
        int orientation = metadata & 7;
        if (orientation == 5 || orientation == 6 || orientation == 0) {
            return List.of(new Cuboid(5 * ONE, 0, 5 * ONE, 11 * ONE, 6 * ONE, 11 * ONE));
        }
        return switch (orientation) {
            case Block.FACE_NORTH -> List.of(new Cuboid(5 * ONE, 4 * ONE, 0, 11 * ONE, 12 * ONE, 3 * ONE));
            case Block.FACE_SOUTH -> List.of(new Cuboid(5 * ONE, 4 * ONE, 13 * ONE, 11 * ONE, 12 * ONE, 1));
            case Block.FACE_EAST -> List.of(new Cuboid(13 * ONE, 4 * ONE, 5 * ONE, 1, 12 * ONE, 11 * ONE));
            case Block.FACE_WEST -> List.of(new Cuboid(0, 4 * ONE, 5 * ONE, 3 * ONE, 12 * ONE, 11 * ONE));
            default -> List.of(new Cuboid(5 * ONE, 0, 5 * ONE, 11 * ONE, 6 * ONE, 11 * ONE));
        };
    }

    private static int effectiveDoorMetadata(int metadata, BlockContext context) {
        return isDoorUpper(metadata) ? context.getMetadata(0, -1, 0) : metadata;
    }

    private static Cuboid doorBox(int metadata) {
        int facing = metadata & 3;
        boolean open = (metadata & 4) != 0;
        float t = 3 * ONE;
        int shape = open ? (facing + 1) & 3 : facing;
        return switch (shape) {
            case 0 -> new Cuboid(0, 0, 0, 1, 1, t);
            case 1 -> new Cuboid(1 - t, 0, 0, 1, 1, 1);
            case 2 -> new Cuboid(0, 0, 1 - t, 1, 1, 1);
            default -> new Cuboid(0, 0, 0, t, 1, 1);
        };
    }

    private static Cuboid trapdoorBox(int metadata) {
        float t = 3 * ONE;
        if (!isTrapdoorOpen(metadata)) {
            return isTrapdoorTop(metadata)
                    ? new Cuboid(0, 1 - t, 0, 1, 1, 1)
                    : new Cuboid(0, 0, 0, 1, t, 1);
        }
        return switch (metadata & 3) {
            case 0 -> new Cuboid(0, 0, 0, 1, 1, t);
            case 1 -> new Cuboid(1 - t, 0, 0, 1, 1, 1);
            case 2 -> new Cuboid(0, 0, 1 - t, 1, 1, 1);
            case 3 -> new Cuboid(0, 0, 0, t, 1, 1);
            default -> new Cuboid(0, 0, 0, 1, t, 1);
        };
    }

    private static List<Cuboid> stairBoxes(int metadata) {
        List<Cuboid> boxes = new ArrayList<>();
        boxes.add(new Cuboid(0, 0, 0, 1, 0.5f, 1));
        switch (metadata & 3) {
            case 0 -> boxes.add(new Cuboid(0, 0.5f, 0.5f, 1, 1, 1));
            case 1 -> boxes.add(new Cuboid(0, 0.5f, 0, 1, 1, 0.5f));
            case 2 -> boxes.add(new Cuboid(0.5f, 0.5f, 0, 1, 1, 1));
            default -> boxes.add(new Cuboid(0, 0.5f, 0, 0.5f, 1, 1));
        }
        return boxes;
    }

    private static List<Cuboid> fenceBoxes(BlockContext context) {
        List<Cuboid> boxes = new ArrayList<>();
        boxes.add(new Cuboid(6 * ONE, 0, 6 * ONE, 10 * ONE, 1.5f, 10 * ONE));
        if (canFenceConnectTo(context.getBlock(0, 0, -1))) {
            boxes.add(new Cuboid(6 * ONE, 6 * ONE, 0, 10 * ONE, 18 * ONE, 10 * ONE));
        }
        if (canFenceConnectTo(context.getBlock(0, 0, 1))) {
            boxes.add(new Cuboid(6 * ONE, 6 * ONE, 6 * ONE, 10 * ONE, 18 * ONE, 1));
        }
        if (canFenceConnectTo(context.getBlock(1, 0, 0))) {
            boxes.add(new Cuboid(6 * ONE, 6 * ONE, 6 * ONE, 1, 18 * ONE, 10 * ONE));
        }
        if (canFenceConnectTo(context.getBlock(-1, 0, 0))) {
            boxes.add(new Cuboid(0, 6 * ONE, 6 * ONE, 10 * ONE, 18 * ONE, 10 * ONE));
        }
        return boxes;
    }

    private static List<Cuboid> fenceGateBoxes(int metadata) {
        boolean open = isFenceGateOpen(metadata);
        int facing = metadata & 3;
        List<Cuboid> boxes = new ArrayList<>();
        boxes.add(new Cuboid(0, 0, 6 * ONE, 2 * ONE, 1.5f, 10 * ONE));
        boxes.add(new Cuboid(14 * ONE, 0, 6 * ONE, 1, 1.5f, 10 * ONE));
        if (!open) {
            if ((facing & 1) == 0) {
                boxes.add(new Cuboid(2 * ONE, 6 * ONE, 6 * ONE, 14 * ONE, 18 * ONE, 10 * ONE));
            } else {
                boxes.clear();
                boxes.add(new Cuboid(6 * ONE, 0, 0, 10 * ONE, 1.5f, 2 * ONE));
                boxes.add(new Cuboid(6 * ONE, 0, 14 * ONE, 10 * ONE, 1.5f, 1));
                boxes.add(new Cuboid(6 * ONE, 6 * ONE, 2 * ONE, 10 * ONE, 18 * ONE, 14 * ONE));
            }
        }
        return boxes;
    }

    private static boolean hasTorchSupport(int metadata, BlockContext context) {
        if (metadata == 5 || metadata == 0) {
            return canSupportAbove(context);
        }
        return canSupportFace(metadata, context);
    }

    private static boolean canSupportAbove(BlockContext context) {
        return canSupportAttached(context.getBlock(0, -1, 0));
    }

    private static boolean canPlantStay(BlockType type, BlockContext context) {
        BlockType below = context.getBlock(0, -1, 0);
        if (type == BlockType.DEAD_BUSH) {
            return below == BlockType.SAND;
        }
        if (type == BlockType.BROWN_MUSHROOM || type == BlockType.RED_MUSHROOM) {
            return below == BlockType.GRASS || below == BlockType.DIRT || below == BlockType.STONE
                    || below == BlockType.COBBLESTONE || below == BlockType.OAK_PLANKS;
        }
        return below == BlockType.GRASS || below == BlockType.DIRT;
    }

    private static boolean canCactusStay(BlockContext context) {
        BlockType below = context.getBlock(0, -1, 0);
        if (below != BlockType.SAND && below != BlockType.CACTUS) {
            return false;
        }
        return !context.getBlock(1, 0, 0).isSolid()
                && !context.getBlock(-1, 0, 0).isSolid()
                && !context.getBlock(0, 0, 1).isSolid()
                && !context.getBlock(0, 0, -1).isSolid();
    }

    private static boolean canSupportFace(int face, BlockContext context) {
        return switch (face) {
            case Block.FACE_NORTH -> canSupportAttached(context.getBlock(0, 0, 1));
            case Block.FACE_SOUTH -> canSupportAttached(context.getBlock(0, 0, -1));
            case Block.FACE_EAST -> canSupportAttached(context.getBlock(-1, 0, 0));
            case Block.FACE_WEST -> canSupportAttached(context.getBlock(1, 0, 0));
            default -> false;
        };
    }

    private static boolean canSupportHorizontalIndex(int index, BlockContext context) {
        return switch (index & 3) {
            case 0 -> canSupportAttached(context.getBlock(0, 0, 1));
            case 1 -> canSupportAttached(context.getBlock(-1, 0, 0));
            case 2 -> canSupportAttached(context.getBlock(0, 0, -1));
            case 3 -> canSupportAttached(context.getBlock(1, 0, 0));
            default -> false;
        };
    }
}
