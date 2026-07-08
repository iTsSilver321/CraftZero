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
    public static final int PORTAL_AXIS_X = 1;
    public static final int PORTAL_AXIS_Z = 2;
    private static final int VINE_NORTH_BIT = 1;
    private static final int VINE_EAST_BIT = 2;
    private static final int VINE_SOUTH_BIT = 4;
    private static final int VINE_WEST_BIT = 8;
    private static final int DOOR_HINGE_BIT = 16;
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

    public static VoxelShape renderShape(BlockState state, BlockContext context) {
        return VoxelShape.of(renderBoxes(state.type(), state.metadata(), context));
    }

    public static VoxelShape collisionShape(BlockState state, BlockContext context) {
        BlockType type = state.type();
        int metadata = state.metadata();
        if (type == BlockType.AIR || type.isFluid() || type == BlockType.FIRE
                || type == BlockType.TORCH || type == BlockType.REDSTONE_TORCH_OFF
                || type == BlockType.REDSTONE_TORCH_ON || type == BlockType.LADDER || type.isSign()
                || isGroundCoverPlant(type) || type == BlockType.SUGAR_CANE || type == BlockType.NETHER_WART
                || type.isCrop() || BlockBehavior.of(type) == BlockBehavior.RAIL
                || BlockBehavior.of(type) == BlockBehavior.REDSTONE_DUST
                || BlockBehavior.of(type) == BlockBehavior.REDSTONE_REPEATER
                || type == BlockType.LEVER || type == BlockType.STONE_BUTTON
                || type == BlockType.STONE_PRESSURE_PLATE || type == BlockType.WOODEN_PRESSURE_PLATE
                || type == BlockType.COBWEB || type == BlockType.VINES
                || type == BlockType.PORTAL || type == BlockType.END_PORTAL) {
            return VoxelShape.EMPTY;
        }
        if (type.isFence()) {
            return VoxelShape.of(fenceBoxes(context));
        }
        if (type.isFenceGate()) {
            return isFenceGateOpen(metadata)
                    ? VoxelShape.EMPTY
                    : VoxelShape.of(List.of(fenceGateCollisionBox(metadata)));
        }
        if (type == BlockType.GLASS_PANE || type == BlockType.IRON_BARS) {
            return VoxelShape.of(paneCollisionBoxes(context));
        }
        if (type == BlockType.CACTUS) {
            return VoxelShape.of(List.of(cactusCollisionBox()));
        }
        if (type == BlockType.SNOW_LAYER) {
            return VoxelShape.of(snowLayerCollisionBoxes(metadata));
        }
        if (type == BlockType.BREWING_STAND) {
            return VoxelShape.of(brewingStandCollisionBoxes());
        }
        if (type == BlockType.CAKE) {
            return VoxelShape.of(cakeCollisionBoxes(metadata));
        }
        if (type == BlockType.SOUL_SAND) {
            return VoxelShape.of(List.of(new Cuboid(0, 0, 0, 1, 14 * ONE, 1)));
        }
        return renderShape(state, context);
    }

    public static VoxelShape selectionShape(BlockState state, BlockContext context) {
        BlockType type = state.type();
        if (type == BlockType.AIR || type.isFluid() || type == BlockType.FIRE
                || type == BlockType.PORTAL || type == BlockType.END_PORTAL) {
            return VoxelShape.EMPTY;
        }
        if (type == BlockType.GLASS_PANE || type == BlockType.IRON_BARS) {
            return VoxelShape.of(List.of(paneSelectionBox(context)));
        }
        if (type == BlockType.CACTUS) {
            return VoxelShape.of(FULL_LIST);
        }
        if (type == BlockType.STANDING_SIGN) {
            return VoxelShape.of(List.of(standingSignSelectionBox()));
        }
        return renderShape(state, context);
    }

    public static List<Cuboid> getRenderBoxes(BlockType type, int metadata, BlockContext context) {
        return renderShape(new BlockState(type, metadata), context).boxes();
    }

    private static List<Cuboid> renderBoxes(BlockType type, int metadata, BlockContext context) {
        if (type == BlockType.AIR) {
            return EMPTY_LIST;
        }
        if (type.isFluid()) {
            return List.of(new Cuboid(0, 0, 0, 1, 14 * ONE, 1));
        }
        if (isGroundCoverPlant(type)) {
            return plantSelectionBoxes(type);
        }
        if (type == BlockType.SUGAR_CANE) {
            return List.of(new Cuboid(2 * ONE, 0, 2 * ONE, 14 * ONE, 1, 14 * ONE));
        }
        if (type == BlockType.NETHER_WART) {
            return netherWartBoxes(metadata);
        }
        if (type.isCrop()) {
            return cropSelectionBoxes(type, metadata);
        }
        if (type == BlockType.CACTUS) {
            return List.of(cactusCollisionBox());
        }
        if (type == BlockType.LILY_PAD) {
            return List.of(new Cuboid(0, 0, 0, 1, 1.0f / 64.0f, 1));
        }
        if (type == BlockType.GLASS_PANE || type == BlockType.IRON_BARS) {
            return paneBoxes(context);
        }
        if (type == BlockType.SNOW_LAYER) {
            return snowLayerBoxes(metadata);
        }
        if (type == BlockType.FIRE) {
            return List.of(new Cuboid(0, 0, 0, 1, 1, 1));
        }
        if (type == BlockType.PORTAL) {
            return portalBoxes(metadata);
        }
        if (type == BlockType.END_PORTAL) {
            return List.of(new Cuboid(0, 0, 0, 1, ONE, 1));
        }
        if (type == BlockType.CHEST) {
            return List.of(new Cuboid(ONE, 0, ONE, 15 * ONE, 14 * ONE, 15 * ONE));
        }
        if (type == BlockType.PISTON || type == BlockType.STICKY_PISTON) {
            return pistonBaseBoxes(metadata);
        }
        if (type == BlockType.PISTON_HEAD) {
            return pistonHeadBoxes(metadata);
        }
        if (type == BlockType.MOVING_PISTON) {
            return pistonHeadBoxes(metadata & 7);
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
            return ladderBoxes(metadata);
        }
        if (type == BlockType.VINES) {
            return vineBoxes(metadata, context);
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
        if (type == BlockType.CAKE) {
            return cakeBoxes(metadata);
        }
        if (type == BlockType.BED) {
            return List.of(new Cuboid(0, 0, 0, 1, 9 * ONE, 1));
        }
        if (type == BlockType.BREWING_STAND) {
            return brewingStandBoxes(metadata);
        }
        if (type == BlockType.CAULDRON) {
            return cauldronBoxes();
        }
        if (type == BlockType.ENCHANTING_TABLE) {
            return List.of(new Cuboid(0, 0, 0, 1, 12 * ONE, 1));
        }
        if (type == BlockType.END_PORTAL_FRAME) {
            return endPortalFrameBoxes(metadata);
        }
        if (type == BlockType.DRAGON_EGG) {
            return List.of(new Cuboid(ONE, 0, ONE, 15 * ONE, 1, 15 * ONE));
        }
        if (type == BlockType.FARMLAND) {
            return List.of(new Cuboid(0, 0, 0, 1, 15 * ONE, 1));
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
        return collisionShape(new BlockState(type, metadata), context).boxes();
    }

    public static List<Cuboid> getSelectionBoxes(BlockType type, int metadata, BlockContext context) {
        return selectionShape(new BlockState(type, metadata), context).boxes();
    }

    public static boolean isFullCube(BlockType type, int metadata) {
        return type != BlockType.AIR && renderShape(new BlockState(type, metadata), emptyContext()).isFullCube();
    }

    public static boolean isOpaqueCube(BlockType type) {
        return type.isSolid() && !type.isTransparent() && isFullCube(type, 0);
    }

    public static boolean canSupportAttached(BlockType type) {
        return isOpaqueCube(type) || type == BlockType.GLASS || type == BlockType.CHEST || type.isFurnace();
    }

    public static boolean canSupportBed(BlockType type) {
        return isOpaqueCube(type);
    }

    public static boolean canSupportDoor(BlockType type) {
        return isOpaqueCube(type);
    }

    public static boolean canFenceConnectTo(BlockType type) {
        return type.isFence() || type.isFenceGate() || isOpaqueCube(type);
    }

    public static boolean canPaneConnectTo(BlockType type) {
        return type == BlockType.GLASS_PANE || type == BlockType.IRON_BARS || type == BlockType.GLASS
                || isOpaqueCube(type);
    }

    public static boolean blocksCactusGrowth(BlockType type) {
        if (type == BlockType.AIR || type.isFluid()
                || isGroundCoverPlant(type) || type.isCrop()) {
            return false;
        }
        return switch (type) {
            case TORCH, REDSTONE_TORCH_OFF, REDSTONE_TORCH_ON, LADDER,
                    RAIL, POWERED_RAIL, DETECTOR_RAIL, LEVER, STONE_BUTTON,
                    REDSTONE_REPEATER_OFF, REDSTONE_REPEATER_ON,
                    SUGAR_CANE, NETHER_WART, VINES, LILY_PAD,
                    PORTAL, END_PORTAL -> false;
            default -> true;
        };
    }

    public static boolean isReplaceable(BlockType type) {
        return type == BlockType.AIR || type.isFluid() || type == BlockType.FIRE
                || type == BlockType.SNOW_LAYER || isGroundCoverPlant(type);
    }

    public static boolean canPlaceAt(BlockType type, int metadata, BlockContext context) {
        if (type == BlockType.TORCH || type == BlockType.REDSTONE_TORCH_OFF || type == BlockType.REDSTONE_TORCH_ON) {
            return hasTorchSupport(metadata, context);
        }
        if (type == BlockType.REDSTONE_WIRE) {
            return canSupportRedstoneWire(context);
        }
        if (type == BlockType.RAIL || type == BlockType.POWERED_RAIL
                || type == BlockType.DETECTOR_RAIL) {
            return canSupportRail(metadata, context);
        }
        if (type == BlockType.REDSTONE_REPEATER_OFF
                || type == BlockType.REDSTONE_REPEATER_ON || type == BlockType.STONE_PRESSURE_PLATE
                || type == BlockType.WOODEN_PRESSURE_PLATE) {
            return canSupportNormalTop(context);
        }
        if (type == BlockType.LEVER) {
            int face = leverOutwardFaceFromMetadata(metadata);
            return canSupportLeverFace(face, context);
        }
        if (type == BlockType.STONE_BUTTON) {
            int face = buttonOutwardFaceFromMetadata(metadata);
            return isHorizontalFace(face) && canSupportButtonFace(face, context);
        }
        if (type == BlockType.LADDER || type == BlockType.WALL_SIGN) {
            int face = wallAttachmentFaceFromMetadata(metadata);
            return isHorizontalFace(face) && canSupportFace(face, context);
        }
        if (type == BlockType.VINES) {
            return canVineStay(metadata, context);
        }
        if (type.isCrop()) {
            return context.getBlock(0, -1, 0) == BlockType.FARMLAND;
        }
        if (type == BlockType.CACTUS) {
            return canCactusStay(context);
        }
        if (type == BlockType.SUGAR_CANE) {
            return canSugarCaneStay(context);
        }
        if (type == BlockType.NETHER_WART) {
            return context.getBlock(0, -1, 0) == BlockType.SOUL_SAND;
        }
        if (type == BlockType.LILY_PAD) {
            return context.getBlock(0, -1, 0).isWater()
                    && context.getMetadata(0, -1, 0) == 0;
        }
        if (type == BlockType.SNOW_LAYER) {
            return canSnowLayerStay(context);
        }
        if (type == BlockType.FIRE) {
            return canFireStay(context);
        }
        if (isGroundCoverPlant(type)) {
            return canPlantStay(type, context);
        }
        if (type == BlockType.BED) {
            return canSupportBed(context.getBlock(0, -1, 0));
        }
        if (type == BlockType.STANDING_SIGN || type == BlockType.WOODEN_DOOR
                || type == BlockType.IRON_DOOR) {
            if (type.isDoor()) {
                return canSupportDoor(context.getBlock(0, -1, 0));
            }
            return canSupportAbove(context);
        }
        if (type == BlockType.CAKE) {
            return canSupportAbove(context);
        }
        if (type == BlockType.TRAPDOOR) {
            return canSupportTrapdoorHorizontalIndex(metadata & 3, context);
        }
        if (type == BlockType.PISTON_HEAD) {
            return hasAttachedExtendedPistonBase(metadata, context);
        }
        return true;
    }

    public static boolean usesCrossedSprite(BlockType type) {
        return isGroundCoverPlant(type) || type.isCrop() || type == BlockType.COBWEB
                || type == BlockType.SUGAR_CANE
                || type == BlockType.NETHER_WART || type == BlockType.FIRE;
    }

    public static boolean blocksPlacementAgainst(BlockType type, int face) {
        return face == Block.FACE_TOP && (type == BlockType.TORCH || type == BlockType.FIRE
                || isGroundCoverPlant(type) || type == BlockType.SUGAR_CANE || type == BlockType.NETHER_WART
                || type.isCrop());
    }

    public static boolean canFallThrough(BlockType type) {
        return type == BlockType.AIR || type.isFluid() || type == BlockType.FIRE
                || type == BlockType.TORCH || type == BlockType.REDSTONE_TORCH_OFF
                || type == BlockType.REDSTONE_TORCH_ON || isGroundCoverPlant(type)
                || type == BlockType.SUGAR_CANE || type == BlockType.NETHER_WART
                || BlockBehavior.of(type) == BlockBehavior.RAIL
                || BlockBehavior.of(type) == BlockBehavior.REDSTONE_DUST
                || BlockBehavior.of(type) == BlockBehavior.REDSTONE_REPEATER
                || type == BlockType.LEVER || type == BlockType.STONE_BUTTON
                || type == BlockType.STONE_PRESSURE_PLATE || type == BlockType.WOODEN_PRESSURE_PLATE
                || type == BlockType.CAKE || type == BlockType.VINES || type.isCrop()
                || type == BlockType.SNOW_LAYER
                || type == BlockType.PORTAL || type == BlockType.END_PORTAL;
    }

    public static boolean canFallingBlockFallThrough(BlockType type) {
        return type == BlockType.AIR || type.isFluid() || type == BlockType.FIRE;
    }

    public static boolean canFallingBlockReplace(BlockType type) {
        return type == BlockType.AIR || type.isFluid() || type == BlockType.FIRE || isGroundCoverPlant(type);
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
        float inset = 0.15f;
        if (metadata == 1) {
            return List.of(new Cuboid(0.0f, 0.2f, 0.5f - inset,
                    inset * 2.0f, 0.8f, 0.5f + inset));
        }
        if (metadata == 2) {
            return List.of(new Cuboid(1.0f - inset * 2.0f, 0.2f, 0.5f - inset,
                    1.0f, 0.8f, 0.5f + inset));
        }
        if (metadata == 3) {
            return List.of(new Cuboid(0.5f - inset, 0.2f, 0.0f,
                    0.5f + inset, 0.8f, inset * 2.0f));
        }
        if (metadata == 4) {
            return List.of(new Cuboid(0.5f - inset, 0.2f, 1.0f - inset * 2.0f,
                    0.5f + inset, 0.8f, 1.0f));
        }

        inset = 0.1f;
        return List.of(new Cuboid(0.5f - inset, 0.0f, 0.5f - inset,
                0.5f + inset, 0.6f, 0.5f + inset));
    }

    private static List<Cuboid> plantSelectionBoxes(BlockType type) {
        if (type == BlockType.SAPLING || type == BlockType.TALL_GRASS || type == BlockType.DEAD_BUSH) {
            return List.of(new Cuboid(0.1f, 0.0f, 0.1f, 0.9f, 0.8f, 0.9f));
        }
        if (type == BlockType.BROWN_MUSHROOM || type == BlockType.RED_MUSHROOM) {
            return List.of(new Cuboid(0.3f, 0.0f, 0.3f, 0.7f, 0.4f, 0.7f));
        }
        return List.of(new Cuboid(0.3f, 0.0f, 0.3f, 0.7f, 0.6f, 0.7f));
    }

    private static boolean isGroundCoverPlant(BlockType type) {
        return type.isPlant() || type == BlockType.TALL_GRASS;
    }

    private static List<Cuboid> cropSelectionBoxes(BlockType type, int metadata) {
        if (type == BlockType.PUMPKIN_STEM || type == BlockType.MELON_STEM) {
            float inset = 0.125f;
            float height = (2 + Math.max(0, Math.min(7, metadata)) * 2) * ONE;
            return List.of(new Cuboid(0.5f - inset, 0.0f, 0.5f - inset,
                    0.5f + inset, Math.min(1.0f, height), 0.5f + inset));
        }
        return List.of(new Cuboid(0.0f, 0.0f, 0.0f, 1.0f, 0.25f, 1.0f));
    }

    private static List<Cuboid> netherWartBoxes(int metadata) {
        return List.of(new Cuboid(0.0f, 0.0f, 0.0f, 1.0f, 0.25f, 1.0f));
    }

    private static List<Cuboid> snowLayerBoxes(int metadata) {
        return List.of(new Cuboid(0, 0, 0, 1, snowLayerHeight(metadata), 1));
    }

    private static List<Cuboid> snowLayerCollisionBoxes(int metadata) {
        int layers = metadata & 7;
        if (layers == 0) {
            return EMPTY_LIST;
        }
        return List.of(new Cuboid(0, 0, 0, 1, layers * 0.125f, 1));
    }

    private static Cuboid cactusCollisionBox() {
        return new Cuboid(ONE, 0, ONE, 15 * ONE, 15 * ONE, 15 * ONE);
    }

    private static float snowLayerHeight(int metadata) {
        return (2 * (1 + (metadata & 7))) * ONE;
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

    private static List<Cuboid> ladderBoxes(int metadata) {
        float thickness = 0.125f;
        return switch (metadata & 7) {
            case 2 -> List.of(new Cuboid(0.0f, 0.0f, 1.0f - thickness, 1.0f, 1.0f, 1.0f));
            case 3 -> List.of(new Cuboid(0.0f, 0.0f, 0.0f, 1.0f, 1.0f, thickness));
            case 4 -> List.of(new Cuboid(1.0f - thickness, 0.0f, 0.0f, 1.0f, 1.0f, 1.0f));
            case 5 -> List.of(new Cuboid(0.0f, 0.0f, 0.0f, thickness, 1.0f, 1.0f));
            default -> List.of(new Cuboid(0.0f, 0.0f, 1.0f - thickness, 1.0f, 1.0f, 1.0f));
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

    private static Cuboid standingSignSelectionBox() {
        return new Cuboid(0.25f, 0.0f, 0.25f, 0.75f, 1.0f, 0.75f);
    }

    private static List<Cuboid> wallSignBoxes(int metadata) {
        float minY = 0.28125f;
        float maxY = 0.78125f;
        float thickness = 0.125f;
        return switch (metadata & 7) {
            case 2 -> List.of(new Cuboid(0.0f, minY, 1.0f - thickness, 1.0f, maxY, 1.0f));
            case 3 -> List.of(new Cuboid(0.0f, minY, 0.0f, 1.0f, maxY, thickness));
            case 4 -> List.of(new Cuboid(1.0f - thickness, minY, 0.0f, 1.0f, maxY, 1.0f));
            case 5 -> List.of(new Cuboid(0.0f, minY, 0.0f, thickness, maxY, 1.0f));
            default -> List.of(new Cuboid(0.0f, minY, 1.0f - thickness, 1.0f, maxY, 1.0f));
        };
    }

    private static List<Cuboid> pressurePlateBoxes(int metadata) {
        float height = (metadata & 1) != 0 ? ONE / 2.0f : ONE;
        return List.of(new Cuboid(ONE, 0, ONE, 15 * ONE, height, 15 * ONE));
    }

    private static List<Cuboid> pistonBaseBoxes(int metadata) {
        if ((metadata & RedstoneEngine.PISTON_EXTENDED_BIT) == 0) {
            return FULL_LIST;
        }
        return switch (metadata & 7) {
            case Block.FACE_BOTTOM -> List.of(new Cuboid(0, 4 * ONE, 0, 1, 1, 1));
            case Block.FACE_TOP -> List.of(new Cuboid(0, 0, 0, 1, 12 * ONE, 1));
            case Block.FACE_NORTH -> List.of(new Cuboid(0, 0, 4 * ONE, 1, 1, 1));
            case Block.FACE_SOUTH -> List.of(new Cuboid(0, 0, 0, 1, 1, 12 * ONE));
            case Block.FACE_EAST -> List.of(new Cuboid(0, 0, 0, 12 * ONE, 1, 1));
            case Block.FACE_WEST -> List.of(new Cuboid(4 * ONE, 0, 0, 1, 1, 1));
            default -> FULL_LIST;
        };
    }

    private static List<Cuboid> pistonHeadBoxes(int metadata) {
        return switch (metadata & 7) {
            case Block.FACE_BOTTOM -> List.of(
                    new Cuboid(0, 0, 0, 1, 4 * ONE, 1),
                    new Cuboid(6 * ONE, 4 * ONE, 6 * ONE, 10 * ONE, 1, 10 * ONE));
            case Block.FACE_TOP -> List.of(
                    new Cuboid(0, 12 * ONE, 0, 1, 1, 1),
                    new Cuboid(6 * ONE, 0, 6 * ONE, 10 * ONE, 12 * ONE, 10 * ONE));
            case Block.FACE_NORTH -> List.of(
                    new Cuboid(0, 0, 0, 1, 1, 4 * ONE),
                    new Cuboid(4 * ONE, 6 * ONE, 4 * ONE, 12 * ONE, 10 * ONE, 1));
            case Block.FACE_SOUTH -> List.of(
                    new Cuboid(0, 0, 12 * ONE, 1, 1, 1),
                    new Cuboid(4 * ONE, 6 * ONE, 0, 12 * ONE, 10 * ONE, 12 * ONE));
            case Block.FACE_EAST -> List.of(
                    new Cuboid(12 * ONE, 0, 0, 1, 1, 1),
                    new Cuboid(0, 6 * ONE, 4 * ONE, 12 * ONE, 10 * ONE, 12 * ONE));
            case Block.FACE_WEST -> List.of(
                    new Cuboid(0, 0, 0, 4 * ONE, 1, 1),
                    new Cuboid(4 * ONE, 6 * ONE, 4 * ONE, 1, 10 * ONE, 12 * ONE));
            default -> FULL_LIST;
        };
    }

    private static boolean hasAttachedExtendedPistonBase(int metadata, BlockContext context) {
        int facing = pistonFacing(metadata);
        int dx = -RedstoneEngine.faceToDx(facing);
        int dy = -RedstoneEngine.faceToDy(facing);
        int dz = -RedstoneEngine.faceToDz(facing);
        BlockType base = context.getBlock(dx, dy, dz);
        int baseMetadata = context.getMetadata(dx, dy, dz);
        return (base == BlockType.PISTON || base == BlockType.STICKY_PISTON)
                && (baseMetadata & RedstoneEngine.PISTON_EXTENDED_BIT) != 0
                && pistonFacing(baseMetadata) == facing;
    }

    private static int pistonFacing(int metadata) {
        int facing = metadata & 7;
        return switch (facing) {
            case Block.FACE_TOP, Block.FACE_BOTTOM, Block.FACE_NORTH,
                    Block.FACE_SOUTH, Block.FACE_EAST, Block.FACE_WEST -> facing;
            default -> Block.FACE_NORTH;
        };
    }

    private static List<Cuboid> buttonBoxes(int metadata) {
        float protrusion = (metadata & 8) != 0 ? ONE : 2 * ONE;
        return switch (metadata & 7) {
            case 1 -> List.of(new Cuboid(0, 6 * ONE, 5 * ONE, protrusion, 10 * ONE, 11 * ONE));
            case 2 -> List.of(new Cuboid(1 - protrusion, 6 * ONE, 5 * ONE, 1, 10 * ONE, 11 * ONE));
            case 3 -> List.of(new Cuboid(5 * ONE, 6 * ONE, 0, 11 * ONE, 10 * ONE, protrusion));
            case 4 -> List.of(new Cuboid(5 * ONE, 6 * ONE, 1 - protrusion, 11 * ONE, 10 * ONE, 1));
            default -> List.of(new Cuboid(5 * ONE, 6 * ONE, 0, 11 * ONE, 10 * ONE, protrusion));
        };
    }

    private static List<Cuboid> leverBoxes(int metadata) {
        int orientation = metadata & 7;
        float inset = 0.1875f;
        if (orientation == 1) {
            return List.of(new Cuboid(0.0f, 0.2f, 0.5f - inset,
                    inset * 2.0f, 0.8f, 0.5f + inset));
        }
        if (orientation == 2) {
            return List.of(new Cuboid(1.0f - inset * 2.0f, 0.2f, 0.5f - inset,
                    1.0f, 0.8f, 0.5f + inset));
        }
        if (orientation == 3) {
            return List.of(new Cuboid(0.5f - inset, 0.2f, 0.0f,
                    0.5f + inset, 0.8f, inset * 2.0f));
        }
        if (orientation == 4) {
            return List.of(new Cuboid(0.5f - inset, 0.2f, 1.0f - inset * 2.0f,
                    0.5f + inset, 0.8f, 1.0f));
        }

        inset = 0.25f;
        if (orientation == 0 || orientation == 7) {
            return List.of(new Cuboid(0.5f - inset, 0.4f, 0.5f - inset,
                    0.5f + inset, 1.0f, 0.5f + inset));
        }
        return List.of(new Cuboid(0.5f - inset, 0.0f, 0.5f - inset,
                0.5f + inset, 0.6f, 0.5f + inset));
    }

    public static int leverMetadataFromFace(int face) {
        return leverMetadataFromFace(face, false);
    }

    public static int leverMetadataFromFace(int face, boolean alternateVerticalAxis) {
        return switch (face) {
            case Block.FACE_EAST -> 1;
            case Block.FACE_WEST -> 2;
            case Block.FACE_SOUTH -> 3;
            case Block.FACE_NORTH -> 4;
            case Block.FACE_TOP -> alternateVerticalAxis ? 6 : 5;
            case Block.FACE_BOTTOM -> alternateVerticalAxis ? 7 : 0;
            default -> -1;
        };
    }

    public static int torchMetadataFromFace(int face) {
        return switch (face) {
            case Block.FACE_EAST -> 1;
            case Block.FACE_WEST -> 2;
            case Block.FACE_SOUTH -> 3;
            case Block.FACE_NORTH -> 4;
            case Block.FACE_TOP -> 5;
            default -> -1;
        };
    }

    public static int buttonMetadataFromFace(int face) {
        return switch (face) {
            case Block.FACE_EAST -> 1;
            case Block.FACE_WEST -> 2;
            case Block.FACE_SOUTH -> 3;
            case Block.FACE_NORTH -> 4;
            default -> -1;
        };
    }

    public static int buttonOutwardFaceFromMetadata(int metadata) {
        return switch (metadata & 7) {
            case 1 -> Block.FACE_EAST;
            case 2 -> Block.FACE_WEST;
            case 3 -> Block.FACE_SOUTH;
            case 4 -> Block.FACE_NORTH;
            default -> -1;
        };
    }

    public static int wallAttachmentMetadataFromFace(int face) {
        return switch (face) {
            case Block.FACE_NORTH -> 2;
            case Block.FACE_SOUTH -> 3;
            case Block.FACE_WEST -> 4;
            case Block.FACE_EAST -> 5;
            default -> -1;
        };
    }

    public static int wallAttachmentFaceFromMetadata(int metadata) {
        return switch (metadata & 7) {
            case 2 -> Block.FACE_NORTH;
            case 3 -> Block.FACE_SOUTH;
            case 4 -> Block.FACE_WEST;
            case 5 -> Block.FACE_EAST;
            default -> -1;
        };
    }

    public static int torchOutwardFaceFromMetadata(int metadata) {
        return switch (metadata) {
            case 1 -> Block.FACE_EAST;
            case 2 -> Block.FACE_WEST;
            case 3 -> Block.FACE_SOUTH;
            case 4 -> Block.FACE_NORTH;
            case 5, 0 -> Block.FACE_TOP;
            default -> -1;
        };
    }

    public static int vineMetadataFromFace(int face) {
        return switch (face) {
            case Block.FACE_NORTH -> VINE_NORTH_BIT;
            case Block.FACE_EAST -> VINE_EAST_BIT;
            case Block.FACE_SOUTH -> VINE_SOUTH_BIT;
            case Block.FACE_WEST -> VINE_WEST_BIT;
            default -> -1;
        };
    }

    public static int leverOutwardFaceFromMetadata(int metadata) {
        return switch (metadata & 7) {
            case 1 -> Block.FACE_EAST;
            case 2 -> Block.FACE_WEST;
            case 3 -> Block.FACE_SOUTH;
            case 4 -> Block.FACE_NORTH;
            case 5, 6 -> Block.FACE_TOP;
            case 0, 7 -> Block.FACE_BOTTOM;
            default -> -1;
        };
    }

    private static int effectiveDoorMetadata(int metadata, BlockContext context) {
        int lowerMetadata = isDoorUpper(metadata) ? context.getMetadata(0, -1, 0) : metadata;
        int upperMetadata = isDoorUpper(metadata) ? metadata : context.getMetadata(0, 1, 0);
        int hinge = (upperMetadata & 1) != 0 ? DOOR_HINGE_BIT : 0;
        return (lowerMetadata & 7) | hinge;
    }

    private static Cuboid doorBox(int metadata) {
        int facing = metadata & 3;
        boolean open = (metadata & 4) != 0;
        boolean hinge = (metadata & DOOR_HINGE_BIT) != 0;
        float t = 3 * ONE;
        if (!open) {
            return switch (facing) {
                case 0 -> new Cuboid(0, 0, 0, t, 1, 1);
                case 1 -> new Cuboid(0, 0, 0, 1, 1, t);
                case 2 -> new Cuboid(1 - t, 0, 0, 1, 1, 1);
                default -> new Cuboid(0, 0, 1 - t, 1, 1, 1);
            };
        }
        return switch (facing) {
            case 0 -> hinge
                    ? new Cuboid(0, 0, 1 - t, 1, 1, 1)
                    : new Cuboid(0, 0, 0, 1, 1, t);
            case 1 -> hinge
                    ? new Cuboid(0, 0, 0, t, 1, 1)
                    : new Cuboid(1 - t, 0, 0, 1, 1, 1);
            case 2 -> hinge
                    ? new Cuboid(0, 0, 0, 1, 1, t)
                    : new Cuboid(0, 0, 1 - t, 1, 1, 1);
            default -> hinge
                    ? new Cuboid(1 - t, 0, 0, 1, 1, 1)
                    : new Cuboid(0, 0, 0, t, 1, 1);
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
            case 0 -> new Cuboid(0, 0, 1 - t, 1, 1, 1);
            case 1 -> new Cuboid(0, 0, 0, t, 1, 1);
            case 2 -> new Cuboid(0, 0, 0, 1, 1, t);
            case 3 -> new Cuboid(1 - t, 0, 0, 1, 1, 1);
            default -> new Cuboid(0, 0, 0, 1, t, 1);
        };
    }

    private static List<Cuboid> cakeBoxes(int metadata) {
        int bites = Math.max(0, Math.min(World.CAKE_LAST_BITE_METADATA, metadata));
        return List.of(new Cuboid((1 + bites * 2) * ONE, 0, ONE, 15 * ONE, 8 * ONE, 15 * ONE));
    }

    private static List<Cuboid> cakeCollisionBoxes(int metadata) {
        int bites = Math.max(0, Math.min(World.CAKE_LAST_BITE_METADATA, metadata));
        return List.of(new Cuboid((1 + bites * 2) * ONE, 0, ONE, 15 * ONE, 7 * ONE, 15 * ONE));
    }

    private static List<Cuboid> endPortalFrameBoxes(int metadata) {
        Cuboid base = new Cuboid(0, 0, 0, 1, 13 * ONE, 1);
        if ((metadata & World.END_PORTAL_FRAME_EYE_BIT) == 0) {
            return List.of(base);
        }
        return List.of(base, new Cuboid(5 * ONE, 13 * ONE, 5 * ONE, 11 * ONE, 1, 11 * ONE));
    }

    private static List<Cuboid> portalBoxes(int metadata) {
        float inset = 6 * ONE;
        float max = 10 * ONE;
        if ((metadata & 3) == PORTAL_AXIS_Z) {
            return List.of(new Cuboid(inset, 0, 0, max, 1, 1));
        }
        return List.of(new Cuboid(0, 0, inset, 1, 1, max));
    }

    private static List<Cuboid> vineBoxes(int metadata, BlockContext context) {
        int sideBits = metadata & 15;
        float minX = 1.0f;
        float minY = 1.0f;
        float minZ = 1.0f;
        float maxX = 0.0f;
        float maxY = 0.0f;
        float maxZ = 0.0f;
        boolean hasSide = sideBits > 0;

        if ((sideBits & VINE_EAST_BIT) != 0) {
            maxX = Math.max(maxX, ONE);
            minX = 0.0f;
            minY = 0.0f;
            maxY = 1.0f;
            minZ = 0.0f;
            maxZ = 1.0f;
        }
        if ((sideBits & VINE_WEST_BIT) != 0) {
            minX = Math.min(minX, 1.0f - ONE);
            maxX = 1.0f;
            minY = 0.0f;
            maxY = 1.0f;
            minZ = 0.0f;
            maxZ = 1.0f;
        }
        if ((sideBits & VINE_SOUTH_BIT) != 0) {
            maxZ = Math.max(maxZ, ONE);
            minZ = 0.0f;
            minX = 0.0f;
            maxX = 1.0f;
            minY = 0.0f;
            maxY = 1.0f;
        }
        if ((sideBits & VINE_NORTH_BIT) != 0) {
            minZ = Math.min(minZ, 1.0f - ONE);
            maxZ = 1.0f;
            minX = 0.0f;
            maxX = 1.0f;
            minY = 0.0f;
            maxY = 1.0f;
        }
        if (!hasSide) {
            if (!canSupportAttached(context.getBlock(0, 1, 0))) {
                return FULL_LIST;
            }
            minY = Math.min(minY, 1.0f - ONE);
            maxY = 1.0f;
            minX = 0.0f;
            maxX = 1.0f;
            minZ = 0.0f;
            maxZ = 1.0f;
        }

        return List.of(new Cuboid(minX, minY, minZ, maxX, maxY, maxZ));
    }

    private static List<Cuboid> paneBoxes(BlockContext context) {
        List<Cuboid> boxes = new ArrayList<>();
        float min = 7 * ONE;
        float max = 9 * ONE;
        boxes.add(new Cuboid(min, 0, min, max, 1, max));
        if (canPaneConnectTo(context.getBlock(0, 0, -1))) {
            boxes.add(new Cuboid(min, 0, 0, max, 1, min));
        }
        if (canPaneConnectTo(context.getBlock(0, 0, 1))) {
            boxes.add(new Cuboid(min, 0, max, max, 1, 1));
        }
        if (canPaneConnectTo(context.getBlock(1, 0, 0))) {
            boxes.add(new Cuboid(max, 0, min, 1, 1, max));
        }
        if (canPaneConnectTo(context.getBlock(-1, 0, 0))) {
            boxes.add(new Cuboid(0, 0, min, min, 1, max));
        }
        return boxes;
    }

    private static Cuboid paneSelectionBox(BlockContext context) {
        float minX = 7 * ONE;
        float maxX = 9 * ONE;
        float minZ = 7 * ONE;
        float maxZ = 9 * ONE;
        boolean north = canPaneConnectTo(context.getBlock(0, 0, -1));
        boolean south = canPaneConnectTo(context.getBlock(0, 0, 1));
        boolean east = canPaneConnectTo(context.getBlock(1, 0, 0));
        boolean west = canPaneConnectTo(context.getBlock(-1, 0, 0));
        boolean connected = north || south || east || west;

        if ((!west || !east) && connected) {
            if (west && !east) {
                minX = 0.0f;
            } else if (!west && east) {
                maxX = 1.0f;
            }
        } else {
            minX = 0.0f;
            maxX = 1.0f;
        }

        if ((!north || !south) && connected) {
            if (north && !south) {
                minZ = 0.0f;
            } else if (!north && south) {
                maxZ = 1.0f;
            }
        } else {
            minZ = 0.0f;
            maxZ = 1.0f;
        }

        return new Cuboid(minX, 0.0f, minZ, maxX, 1.0f, maxZ);
    }

    private static List<Cuboid> paneCollisionBoxes(BlockContext context) {
        List<Cuboid> boxes = new ArrayList<>();
        boolean north = canPaneConnectTo(context.getBlock(0, 0, -1));
        boolean south = canPaneConnectTo(context.getBlock(0, 0, 1));
        boolean east = canPaneConnectTo(context.getBlock(1, 0, 0));
        boolean west = canPaneConnectTo(context.getBlock(-1, 0, 0));
        boolean connected = north || south || east || west;

        if ((!west || !east) && connected) {
            if (west && !east) {
                boxes.add(new Cuboid(0.0f, 0.0f, 7 * ONE, 0.5f, 1.0f, 9 * ONE));
            } else if (!west && east) {
                boxes.add(new Cuboid(0.5f, 0.0f, 7 * ONE, 1.0f, 1.0f, 9 * ONE));
            }
        } else {
            boxes.add(new Cuboid(0.0f, 0.0f, 7 * ONE, 1.0f, 1.0f, 9 * ONE));
        }

        if ((!north || !south) && connected) {
            if (north && !south) {
                boxes.add(new Cuboid(7 * ONE, 0.0f, 0.0f, 9 * ONE, 1.0f, 0.5f));
            } else if (!north && south) {
                boxes.add(new Cuboid(7 * ONE, 0.0f, 0.5f, 9 * ONE, 1.0f, 1.0f));
            }
        } else {
            boxes.add(new Cuboid(7 * ONE, 0.0f, 0.0f, 9 * ONE, 1.0f, 1.0f));
        }

        return boxes;
    }

    private static List<Cuboid> brewingStandBoxes(int metadata) {
        List<Cuboid> boxes = new ArrayList<>();
        boxes.add(new Cuboid(2 * ONE, 0, 2 * ONE, 14 * ONE, 2 * ONE, 14 * ONE));
        boxes.add(new Cuboid(7 * ONE, 0, 7 * ONE, 9 * ONE, 14 * ONE, 9 * ONE));
        if ((metadata & 1) != 0) {
            boxes.add(new Cuboid(4 * ONE, 2 * ONE, 4 * ONE, 7 * ONE, 7 * ONE, 7 * ONE));
        }
        if ((metadata & 2) != 0) {
            boxes.add(new Cuboid(9 * ONE, 2 * ONE, 4 * ONE, 12 * ONE, 7 * ONE, 7 * ONE));
        }
        if ((metadata & 4) != 0) {
            boxes.add(new Cuboid(6 * ONE, 2 * ONE, 9 * ONE, 10 * ONE, 7 * ONE, 12 * ONE));
        }
        return boxes;
    }

    private static List<Cuboid> brewingStandCollisionBoxes() {
        return List.of(
                new Cuboid(7 * ONE, 0, 7 * ONE, 9 * ONE, 14 * ONE, 9 * ONE),
                new Cuboid(0, 0, 0, 1, 2 * ONE, 1));
    }

    private static List<Cuboid> cauldronBoxes() {
        float wall = 2 * ONE;
        return List.of(
                new Cuboid(0, 0, 0, 1, 5 * ONE, 1),
                new Cuboid(0, 0, 0, 1, 1, wall),
                new Cuboid(0, 0, 1 - wall, 1, 1, 1),
                new Cuboid(0, 0, wall, wall, 1, 1 - wall),
                new Cuboid(1 - wall, 0, wall, 1, 1, 1 - wall));
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
        if ((facing & 1) == 0) {
            boxes.add(new Cuboid(0, 0, 6 * ONE, 2 * ONE, 1.5f, 10 * ONE));
            boxes.add(new Cuboid(14 * ONE, 0, 6 * ONE, 1, 1.5f, 10 * ONE));
            if (open) {
                float leafMinZ = facing == 0 ? 6 * ONE : 0;
                float leafMaxZ = facing == 0 ? 1 : 10 * ONE;
                boxes.add(new Cuboid(0, 6 * ONE, leafMinZ, 2 * ONE, 18 * ONE, leafMaxZ));
                boxes.add(new Cuboid(14 * ONE, 6 * ONE, leafMinZ, 1, 18 * ONE, leafMaxZ));
            } else {
                boxes.add(new Cuboid(2 * ONE, 6 * ONE, 6 * ONE, 14 * ONE, 18 * ONE, 10 * ONE));
            }
        } else {
            boxes.add(new Cuboid(6 * ONE, 0, 0, 10 * ONE, 1.5f, 2 * ONE));
            boxes.add(new Cuboid(6 * ONE, 0, 14 * ONE, 10 * ONE, 1.5f, 1));
            if (open) {
                float leafMinX = facing == 1 ? 6 * ONE : 0;
                float leafMaxX = facing == 1 ? 1 : 10 * ONE;
                boxes.add(new Cuboid(leafMinX, 6 * ONE, 0, leafMaxX, 18 * ONE, 2 * ONE));
                boxes.add(new Cuboid(leafMinX, 6 * ONE, 14 * ONE, leafMaxX, 18 * ONE, 1));
            } else {
                boxes.add(new Cuboid(6 * ONE, 6 * ONE, 2 * ONE, 10 * ONE, 18 * ONE, 14 * ONE));
            }
        }
        return boxes;
    }

    private static Cuboid fenceGateCollisionBox(int metadata) {
        int facing = metadata & 3;
        if ((facing & 1) == 0) {
            return new Cuboid(0, 0, 6 * ONE, 1, 1.5f, 10 * ONE);
        }
        return new Cuboid(6 * ONE, 0, 0, 10 * ONE, 1.5f, 1);
    }

    private static boolean hasTorchSupport(int metadata, BlockContext context) {
        return switch (metadata) {
            case 1 -> canSupportAttached(context.getBlock(-1, 0, 0));
            case 2 -> canSupportAttached(context.getBlock(1, 0, 0));
            case 3 -> canSupportAttached(context.getBlock(0, 0, -1));
            case 4 -> canSupportAttached(context.getBlock(0, 0, 1));
            case 5, 0 -> canSupportAbove(context);
            default -> false;
        };
    }

    private static boolean canSupportAbove(BlockContext context) {
        return canSupportAttached(context.getBlock(0, -1, 0));
    }

    private static boolean canSupportNormalTop(BlockContext context) {
        return isOpaqueCube(context.getBlock(0, -1, 0));
    }

    private static boolean canSupportRail(int metadata, BlockContext context) {
        if (!canSupportNormalTop(context)) {
            return false;
        }
        return switch (metadata & 7) {
            case 2 -> isOpaqueCube(context.getBlock(1, 0, 0));
            case 3 -> isOpaqueCube(context.getBlock(-1, 0, 0));
            case 4 -> isOpaqueCube(context.getBlock(0, 0, -1));
            case 5 -> isOpaqueCube(context.getBlock(0, 0, 1));
            default -> true;
        };
    }

    private static boolean canSupportRedstoneWire(BlockContext context) {
        BlockType below = context.getBlock(0, -1, 0);
        return isOpaqueCube(below) || below == BlockType.GLOWSTONE;
    }

    private static boolean canPlantStay(BlockType type, BlockContext context) {
        BlockType below = context.getBlock(0, -1, 0);
        if (type == BlockType.DEAD_BUSH) {
            return below == BlockType.SAND;
        }
        if (type == BlockType.BROWN_MUSHROOM || type == BlockType.RED_MUSHROOM) {
            return below == BlockType.MYCELIUM || isOpaqueCube(below);
        }
        return below == BlockType.GRASS || below == BlockType.DIRT || below == BlockType.FARMLAND;
    }

    private static boolean canCactusStay(BlockContext context) {
        BlockType below = context.getBlock(0, -1, 0);
        if (below != BlockType.SAND && below != BlockType.CACTUS) {
            return false;
        }
        return !blocksCactusGrowth(context.getBlock(1, 0, 0))
                && !blocksCactusGrowth(context.getBlock(-1, 0, 0))
                && !blocksCactusGrowth(context.getBlock(0, 0, 1))
                && !blocksCactusGrowth(context.getBlock(0, 0, -1));
    }

    private static boolean canSugarCaneStay(BlockContext context) {
        BlockType below = context.getBlock(0, -1, 0);
        if (below == BlockType.SUGAR_CANE) {
            return true;
        }
        if (below != BlockType.GRASS && below != BlockType.DIRT && below != BlockType.SAND) {
            return false;
        }
        return context.getBlock(1, -1, 0).isWater()
                || context.getBlock(-1, -1, 0).isWater()
                || context.getBlock(0, -1, 1).isWater()
                || context.getBlock(0, -1, -1).isWater();
    }

    private static boolean canSnowLayerStay(BlockContext context) {
        BlockType below = context.getBlock(0, -1, 0);
        return below == BlockType.LEAVES || isOpaqueCube(below);
    }

    private static boolean canFireStay(BlockContext context) {
        if (isOpaqueCube(context.getBlock(0, -1, 0))) {
            return true;
        }
        return canCatchFire(context.getBlock(1, 0, 0))
                || canCatchFire(context.getBlock(-1, 0, 0))
                || canCatchFire(context.getBlock(0, 1, 0))
                || canCatchFire(context.getBlock(0, -1, 0))
                || canCatchFire(context.getBlock(0, 0, 1))
                || canCatchFire(context.getBlock(0, 0, -1));
    }

    private static boolean canCatchFire(BlockType type) {
        return type.getFireEncouragement() > 0;
    }

    private static boolean canVineStay(int metadata, BlockContext context) {
        if (vineBitCanStay(metadata, VINE_WEST_BIT, Block.FACE_WEST, context)
                || vineBitCanStay(metadata, VINE_EAST_BIT, Block.FACE_EAST, context)
                || vineBitCanStay(metadata, VINE_NORTH_BIT, Block.FACE_NORTH, context)
                || vineBitCanStay(metadata, VINE_SOUTH_BIT, Block.FACE_SOUTH, context)) {
            return true;
        }
        if ((metadata & 15) == 0 && canSupportAttached(context.getBlock(0, 1, 0))) {
            return true;
        }
        int legacyFace = metadata & 7;
        return isHorizontalFace(legacyFace) && canSupportFace(legacyFace, context);
    }

    private static boolean vineBitCanStay(int metadata, int bit, int face, BlockContext context) {
        if ((metadata & bit) == 0) {
            return false;
        }
        if (canSupportFace(face, context)) {
            return true;
        }
        return context.getBlock(0, 1, 0) == BlockType.VINES
                && (context.getMetadata(0, 1, 0) & bit) != 0;
    }

    private static boolean isHorizontalFace(int face) {
        return face == Block.FACE_NORTH || face == Block.FACE_SOUTH
                || face == Block.FACE_EAST || face == Block.FACE_WEST;
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

    private static boolean canSupportButtonFace(int face, BlockContext context) {
        return switch (face) {
            case Block.FACE_NORTH -> isOpaqueCube(context.getBlock(0, 0, 1));
            case Block.FACE_SOUTH -> isOpaqueCube(context.getBlock(0, 0, -1));
            case Block.FACE_EAST -> isOpaqueCube(context.getBlock(-1, 0, 0));
            case Block.FACE_WEST -> isOpaqueCube(context.getBlock(1, 0, 0));
            default -> false;
        };
    }

    private static boolean canSupportLeverFace(int face, BlockContext context) {
        return switch (face) {
            case Block.FACE_TOP -> isOpaqueCube(context.getBlock(0, -1, 0));
            case Block.FACE_BOTTOM -> isOpaqueCube(context.getBlock(0, 1, 0));
            case Block.FACE_NORTH -> isOpaqueCube(context.getBlock(0, 0, 1));
            case Block.FACE_SOUTH -> isOpaqueCube(context.getBlock(0, 0, -1));
            case Block.FACE_EAST -> isOpaqueCube(context.getBlock(-1, 0, 0));
            case Block.FACE_WEST -> isOpaqueCube(context.getBlock(1, 0, 0));
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

    private static boolean canSupportTrapdoorHorizontalIndex(int index, BlockContext context) {
        return switch (index & 3) {
            case 0 -> canSupportTrapdoorAnchor(context.getBlock(0, 0, 1));
            case 1 -> canSupportTrapdoorAnchor(context.getBlock(-1, 0, 0));
            case 2 -> canSupportTrapdoorAnchor(context.getBlock(0, 0, -1));
            case 3 -> canSupportTrapdoorAnchor(context.getBlock(1, 0, 0));
            default -> false;
        };
    }

    private static boolean canSupportTrapdoorAnchor(BlockType type) {
        return isOpaqueCube(type) || type == BlockType.GLOWSTONE || type.isSlab() || type.isStairs();
    }
}
