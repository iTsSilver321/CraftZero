package com.craftzero.world;

import com.craftzero.inventory.ItemType;
import com.craftzero.inventory.ToolType;

import java.util.HashMap;
import java.util.Map;

/**
 * Block-only registry targeting Minecraft Java Release 1.0 numeric block IDs.
 * Items and tools live in ItemType.
 */
public enum BlockType {

    // ID, Solid, Transparent, Hardness, Top, Bottom, Side, PreferredTool,
    // HarvestLevel. Texture indices match Terrain.png (16x16 grid).
    AIR(0, false, true, 0f, -1, -1, -1, ToolType.Category.NONE, 0),
    STONE(1, true, false, 5.0f, 1, 1, 1, ToolType.Category.PICKAXE, 1),
    GRASS(2, true, false, 1.5f, 0, 2, 3, ToolType.Category.SHOVEL, 0),
    DIRT(3, true, false, 1.3f, 2, 2, 2, ToolType.Category.SHOVEL, 0),
    COBBLESTONE(4, true, false, 6.0f, 16, 16, 16, ToolType.Category.PICKAXE, 1),
    OAK_PLANKS(5, true, false, 3.4f, 4, 4, 4, ToolType.Category.AXE, 0),
    SAPLING(6, false, true, 0.0f, 15, 15, 15, ToolType.Category.NONE, 0),
    BEDROCK(7, true, false, -1f, 17, 17, 17, ToolType.Category.NONE, 99),
    FLOWING_WATER(8, false, true, 0f, 205, 205, 205, ToolType.Category.NONE, 0),
    WATER(9, false, true, 0f, 205, 205, 205, ToolType.Category.NONE, 0),
    FLOWING_LAVA(10, false, true, 0f, 237, 237, 237, ToolType.Category.NONE, 0),
    LAVA(11, false, true, 0f, 237, 237, 237, ToolType.Category.NONE, 0),
    SAND(12, true, false, 0.8f, 18, 18, 18, ToolType.Category.SHOVEL, 0),
    GRAVEL(13, true, false, 1.0f, 19, 19, 19, ToolType.Category.SHOVEL, 0),
    GOLD_ORE(14, true, false, 8.4f, 32, 32, 32, ToolType.Category.PICKAXE, 3),
    IRON_ORE(15, true, false, 8.4f, 33, 33, 33, ToolType.Category.PICKAXE, 2),
    COAL_ORE(16, true, false, 7.6f, 34, 34, 34, ToolType.Category.PICKAXE, 1),
    OAK_LOG(17, true, false, 3.4f, 21, 21, 20, ToolType.Category.AXE, 0),
    LEAVES(18, true, true, 0.3f, 53, 53, 53, ToolType.Category.NONE, 0),
    GLASS(20, true, true, 0.5f, 49, 49, 49, ToolType.Category.NONE, 0),
    LAPIS_ORE(21, true, false, 7.6f, 160, 160, 160, ToolType.Category.PICKAXE, 2),
    LAPIS_BLOCK(22, true, false, 3.0f, 144, 144, 144, ToolType.Category.PICKAXE, 1),
    DISPENSER(23, true, false, 3.5f, 46, 45, 45, ToolType.Category.PICKAXE, 1),
    SANDSTONE(24, true, false, 4.0f, 192, 176, 176, ToolType.Category.PICKAXE, 1),
    NOTE_BLOCK(25, true, false, 0.8f, 74, 74, 74, ToolType.Category.AXE, 0),
    BED(26, true, true, 0.2f, 134, 4, 135, ToolType.Category.NONE, 0),
    POWERED_RAIL(27, false, true, 0.7f, 179, 179, 179, ToolType.Category.PICKAXE, 0),
    DETECTOR_RAIL(28, false, true, 0.7f, 195, 195, 195, ToolType.Category.PICKAXE, 0),
    STICKY_PISTON(29, true, false, 0.5f, 106, 107, 108, ToolType.Category.PICKAXE, 0),
    COBWEB(30, false, true, 4.0f, 11, 11, 11, ToolType.Category.SWORD, 0),
    TALL_GRASS(31, false, true, 0.0f, 39, 39, 39, ToolType.Category.NONE, 0),
    DEAD_BUSH(32, false, true, 0.0f, 55, 55, 55, ToolType.Category.NONE, 0),
    PISTON(33, true, false, 0.5f, 107, 107, 108, ToolType.Category.PICKAXE, 0),
    PISTON_HEAD(34, true, true, 0.5f, 107, 107, 108, ToolType.Category.PICKAXE, 0),
    WHITE_WOOL(35, true, false, 0.8f, 64, 64, 64, ToolType.Category.NONE, 0),
    MOVING_PISTON(36, true, true, -1f, 107, 107, 108, ToolType.Category.NONE, 99),
    YELLOW_FLOWER(37, false, true, 0.0f, 13, 13, 13, ToolType.Category.NONE, 0),
    RED_ROSE(38, false, true, 0.0f, 12, 12, 12, ToolType.Category.NONE, 0),
    BROWN_MUSHROOM(39, false, true, 0.0f, 29, 29, 29, ToolType.Category.NONE, 0),
    RED_MUSHROOM(40, false, true, 0.0f, 28, 28, 28, ToolType.Category.NONE, 0),
    GOLD_BLOCK(41, true, false, 3.0f, 23, 23, 23, ToolType.Category.PICKAXE, 2),
    IRON_BLOCK(42, true, false, 5.0f, 22, 22, 22, ToolType.Category.PICKAXE, 1),
    DOUBLE_STONE_SLAB(43, true, false, 4.0f, 6, 6, 5, ToolType.Category.PICKAXE, 1),
    STONE_SLAB(44, true, true, 2.0f, 6, 6, 5, ToolType.Category.PICKAXE, 1),
    BRICK(45, true, false, 3.4f, 7, 7, 7, ToolType.Category.PICKAXE, 1),
    TNT(46, true, false, 0.0f, 9, 10, 8, ToolType.Category.NONE, 0),
    BOOKSHELF(47, true, false, 2.3f, 4, 4, 35, ToolType.Category.AXE, 0),
    MOSSY_COBBLESTONE(48, true, false, 6.0f, 36, 36, 36, ToolType.Category.PICKAXE, 1),
    OBSIDIAN(49, true, false, 50.0f, 37, 37, 37, ToolType.Category.PICKAXE, 3),
    TORCH(50, false, true, 0.0f, 80, 80, 80, ToolType.Category.NONE, 0),
    FIRE(51, false, true, 0.0f, 31, 31, 31, ToolType.Category.NONE, 0),
    MOB_SPAWNER(52, true, true, 5.0f, 65, 65, 65, ToolType.Category.PICKAXE, 0),
    OAK_STAIRS(53, true, true, 3.4f, 4, 4, 4, ToolType.Category.AXE, 0),
    CHEST(54, true, false, 3.8f, 25, 25, 26, ToolType.Category.AXE, 0),
    REDSTONE_WIRE(55, false, true, 0.0f, 164, 164, 164, ToolType.Category.NONE, 0),
    DIAMOND_ORE(56, true, false, 10.0f, 50, 50, 50, ToolType.Category.PICKAXE, 3),
    DIAMOND_BLOCK(57, true, false, 5.0f, 24, 24, 24, ToolType.Category.PICKAXE, 2),
    CRAFTING_TABLE(58, true, false, 4.2f, 43, 4, 59, ToolType.Category.AXE, 0),
    CROPS(59, false, true, 0.0f, 88, 88, 88, ToolType.Category.NONE, 0),
    FARMLAND(60, true, true, 0.6f, 87, 2, 2, ToolType.Category.SHOVEL, 0),
    FURNACE(61, true, false, 3.5f, 62, 62, 45, ToolType.Category.PICKAXE, 1),
    LIT_FURNACE(62, true, false, 3.5f, 62, 62, 45, ToolType.Category.PICKAXE, 1),
    STANDING_SIGN(63, false, true, 1.0f, 4, 4, 4, ToolType.Category.AXE, 0),
    WOODEN_DOOR(64, true, true, 3.0f, 81, 81, 81, ToolType.Category.AXE, 0),
    LADDER(65, false, true, 0.4f, 83, 83, 83, ToolType.Category.NONE, 0),
    RAIL(66, false, true, 0.7f, 128, 128, 128, ToolType.Category.PICKAXE, 0),
    COBBLESTONE_STAIRS(67, true, true, 6.0f, 16, 16, 16, ToolType.Category.PICKAXE, 1),
    WALL_SIGN(68, false, true, 1.0f, 4, 4, 4, ToolType.Category.AXE, 0),
    LEVER(69, false, true, 0.5f, 96, 96, 96, ToolType.Category.NONE, 0),
    STONE_PRESSURE_PLATE(70, false, true, 0.5f, 1, 1, 1, ToolType.Category.PICKAXE, 0),
    IRON_DOOR(71, true, true, 5.0f, 82, 82, 82, ToolType.Category.PICKAXE, 2),
    WOODEN_PRESSURE_PLATE(72, false, true, 0.5f, 4, 4, 4, ToolType.Category.AXE, 0),
    REDSTONE_ORE(73, true, false, 7.6f, 51, 51, 51, ToolType.Category.PICKAXE, 2),
    GLOWING_REDSTONE_ORE(74, true, false, 7.6f, 51, 51, 51, ToolType.Category.PICKAXE, 2),
    REDSTONE_TORCH_OFF(75, false, true, 0.0f, 99, 99, 99, ToolType.Category.NONE, 0),
    REDSTONE_TORCH_ON(76, false, true, 0.0f, 115, 115, 115, ToolType.Category.NONE, 0),
    STONE_BUTTON(77, false, true, 0.5f, 1, 1, 1, ToolType.Category.PICKAXE, 0),
    ICE(79, true, true, 0.8f, 67, 67, 67, ToolType.Category.PICKAXE, 0),
    SNOW(80, true, false, 0.2f, 66, 66, 66, ToolType.Category.SHOVEL, 0),
    CACTUS(81, true, false, 0.4f, 70, 69, 70, ToolType.Category.NONE, 0),
    CLAY(82, true, false, 0.6f, 72, 72, 72, ToolType.Category.SHOVEL, 0),
    SUGAR_CANE(83, false, true, 0.0f, 73, 73, 73, ToolType.Category.NONE, 0),
    JUKEBOX(84, true, false, 2.0f, 75, 74, 74, ToolType.Category.AXE, 0),
    FENCE(85, true, true, 3.0f, 4, 4, 4, ToolType.Category.AXE, 0),
    PUMPKIN(86, true, false, 1.0f, 102, 102, 102, ToolType.Category.AXE, 0),
    NETHERRACK(87, true, false, 0.4f, 103, 103, 103, ToolType.Category.PICKAXE, 0),
    SOUL_SAND(88, true, false, 0.5f, 104, 104, 104, ToolType.Category.SHOVEL, 0),
    GLOWSTONE(89, true, false, 0.3f, 105, 105, 105, ToolType.Category.NONE, 0),
    PORTAL(90, false, true, -1f, 14, 14, 14, ToolType.Category.NONE, 99),
    JACK_O_LANTERN(91, true, false, 1.0f, 102, 102, 119, ToolType.Category.AXE, 0),
    CAKE(92, false, true, 0.5f, 121, 122, 124, ToolType.Category.NONE, 0),
    REDSTONE_REPEATER_OFF(93, false, true, 0.0f, 131, 131, 131, ToolType.Category.NONE, 0),
    REDSTONE_REPEATER_ON(94, false, true, 0.0f, 147, 147, 147, ToolType.Category.NONE, 0),
    TRAPDOOR(96, true, true, 3.0f, 84, 84, 84, ToolType.Category.AXE, 0),
    INFESTED_STONE(97, true, false, 3.0f, 1, 1, 1, ToolType.Category.PICKAXE, 0),
    STONE_BRICK(98, true, false, 4.0f, 54, 54, 54, ToolType.Category.PICKAXE, 1),
    BROWN_MUSHROOM_BLOCK(99, true, false, 1.0f, 14, 14, 14, ToolType.Category.AXE, 0),
    RED_MUSHROOM_BLOCK(100, true, false, 1.0f, 14, 14, 14, ToolType.Category.AXE, 0),
    IRON_BARS(101, true, true, 5.0f, 85, 85, 85, ToolType.Category.PICKAXE, 1),
    GLASS_PANE(102, true, true, 0.3f, 49, 49, 49, ToolType.Category.NONE, 0),
    MELON(103, true, false, 1.0f, 136, 136, 137, ToolType.Category.AXE, 0),
    PUMPKIN_STEM(104, false, true, 0.0f, 111, 111, 111, ToolType.Category.NONE, 0),
    MELON_STEM(105, false, true, 0.0f, 111, 111, 111, ToolType.Category.NONE, 0),
    VINES(106, false, true, 0.2f, 143, 143, 143, ToolType.Category.NONE, 0),
    FENCE_GATE(107, true, true, 3.0f, 4, 4, 4, ToolType.Category.AXE, 0),
    BRICK_STAIRS(108, true, true, 3.4f, 7, 7, 7, ToolType.Category.PICKAXE, 1),
    STONE_BRICK_STAIRS(109, true, true, 4.0f, 54, 54, 54, ToolType.Category.PICKAXE, 1),
    MYCELIUM(110, true, false, 0.6f, 77, 2, 78, ToolType.Category.SHOVEL, 0),
    LILY_PAD(111, false, true, 0.0f, 76, 76, 76, ToolType.Category.NONE, 0),
    NETHER_BRICK(112, true, false, 10.0f, 224, 224, 224, ToolType.Category.PICKAXE, 2),
    NETHER_BRICK_FENCE(113, true, true, 10.0f, 224, 224, 224, ToolType.Category.PICKAXE, 2),
    NETHER_BRICK_STAIRS(114, true, true, 10.0f, 224, 224, 224, ToolType.Category.PICKAXE, 2),
    NETHER_WART(115, false, true, 0.0f, 226, 226, 226, ToolType.Category.NONE, 0),
    ENCHANTING_TABLE(116, true, true, 5.0f, 166, 176, 167, ToolType.Category.PICKAXE, 0),
    BREWING_STAND(117, false, true, 0.5f, 157, 157, 157, ToolType.Category.PICKAXE, 0),
    CAULDRON(118, true, true, 2.0f, 154, 155, 156, ToolType.Category.PICKAXE, 0),
    END_PORTAL(119, false, true, -1f, 14, 14, 14, ToolType.Category.NONE, 99),
    END_PORTAL_FRAME(120, true, true, -1f, 174, 175, 175, ToolType.Category.NONE, 99),
    END_STONE(121, true, false, 3.0f, 175, 175, 175, ToolType.Category.PICKAXE, 1),
    DRAGON_EGG(122, true, true, 3.0f, 167, 167, 167, ToolType.Category.PICKAXE, 0);

    private static final Map<Integer, BlockType> BY_ID = new HashMap<>();
    private static final BlockType[] BY_ID_FAST;

    static {
        int maxId = 0;
        for (BlockType type : values()) {
            maxId = Math.max(maxId, type.id);
            BlockType previous = BY_ID.put(type.id, type);
            if (previous != null) {
                throw new IllegalStateException(
                        "Duplicate block id " + type.id + " for " + previous.name() + " and " + type.name());
            }
        }
        BY_ID_FAST = new BlockType[maxId + 1];
        for (BlockType type : values()) {
            BY_ID_FAST[type.id] = type;
        }
    }

    private final int id;
    private final boolean solid;
    private final boolean transparent;
    private final float hardness;
    private final int topTexture;
    private final int bottomTexture;
    private final int sideTexture;
    private final ToolType.Category preferredTool;
    private final int harvestLevel;
    private static volatile boolean fancyGraphics = true;

    public static final int ATLAS_SIZE = 16;
    public static final float TEXTURE_SIZE = 1.0f / ATLAS_SIZE;

    BlockType(int id, boolean solid, boolean transparent, float hardness,
            int topTexture, int bottomTexture, int sideTexture,
            ToolType.Category preferredTool, int harvestLevel) {
        this.id = id;
        this.solid = solid;
        this.transparent = transparent;
        this.hardness = hardness;
        this.topTexture = topTexture;
        this.bottomTexture = bottomTexture;
        this.sideTexture = sideTexture;
        this.preferredTool = preferredTool;
        this.harvestLevel = harvestLevel;
    }

    public ItemType getDroppedItem() {
        return switch (this) {
            case AIR, FLOWING_WATER, WATER, FLOWING_LAVA, LAVA, FIRE, LEAVES, GLASS, ICE,
                    PORTAL, END_PORTAL, MOVING_PISTON, PISTON_HEAD -> null;
            case GRASS -> ItemType.DIRT;
            case STONE -> ItemType.COBBLESTONE;
            case COAL_ORE -> ItemType.COAL;
            case DIAMOND_ORE -> ItemType.DIAMOND;
            case REDSTONE_ORE, GLOWING_REDSTONE_ORE -> ItemType.REDSTONE;
            case LAPIS_ORE -> ItemType.LAPIS_LAZULI;
            case LIT_FURNACE -> ItemType.FURNACE;
            case REDSTONE_TORCH_OFF, REDSTONE_TORCH_ON -> ItemType.REDSTONE_TORCH;
            case REDSTONE_REPEATER_OFF, REDSTONE_REPEATER_ON -> ItemType.REDSTONE_REPEATER;
            case STANDING_SIGN, WALL_SIGN -> ItemType.SIGN;
            case WOODEN_DOOR -> ItemType.WOODEN_DOOR;
            case IRON_DOOR -> ItemType.IRON_DOOR;
            case BED -> ItemType.BED;
            case DOUBLE_STONE_SLAB -> ItemType.STONE_SLAB;
            default -> ItemType.fromBlock(this);
        };
    }

    public int getId() {
        return id;
    }

    public boolean isSolid() {
        return solid;
    }

    public ToolType.Category getPreferredTool() {
        return preferredTool;
    }

    public int getHarvestLevel() {
        return harvestLevel;
    }

    public boolean isTransparent() {
        return transparent && (this != LEAVES || fancyGraphics);
    }

    public boolean isAir() {
        return this == AIR;
    }

    public static void setFancyGraphics(boolean enabled) {
        fancyGraphics = enabled;
    }

    public float getHardness() {
        return hardness;
    }

    public boolean isBreakable() {
        return hardness >= 0 && this != AIR && !isFluid();
    }

    public float[] getTextureCoords(int face) {
        return getTextureCoords(face, 0);
    }

    public float[] getTextureCoords(int face, int metadata) {
        int textureIndex = switch (face) {
            case 0 -> topTexture;
            case 1 -> bottomTexture;
            default -> sideTexture;
        };

        if (this == FURNACE || this == LIT_FURNACE) {
            if (face == metadataToFace(metadata)) {
                textureIndex = this == LIT_FURNACE ? 61 : 44;
            } else if (face != 0 && face != 1) {
                textureIndex = 45;
            }
        }

        if (textureIndex < 0) {
            return new float[] { 0, 0, 0, 0 };
        }

        int row = textureIndex / ATLAS_SIZE;
        int col = textureIndex % ATLAS_SIZE;

        float u1 = col * TEXTURE_SIZE;
        float v1 = row * TEXTURE_SIZE;
        float u2 = u1 + TEXTURE_SIZE;
        float v2 = v1 + TEXTURE_SIZE;

        float inset = 0.001f;
        return new float[] { u1 + inset, v1 + inset, u2 - inset, v2 - inset };
    }

    public static BlockType fromId(int id) {
        if (id >= 0 && id < BY_ID_FAST.length) {
            BlockType type = BY_ID_FAST[id];
            return type != null ? type : AIR;
        }
        return AIR;
    }

    public static boolean hasId(int id) {
        return id >= 0 && id < BY_ID_FAST.length && BY_ID_FAST[id] != null;
    }

    public boolean occludesFace() {
        return solid && !isTransparent() && BlockShape.isFullCube(this, 0);
    }

    public boolean hasTileEntity() {
        return this == CHEST || this == FURNACE || this == LIT_FURNACE || this == MOB_SPAWNER || isSign();
    }

    public boolean isFurnace() {
        return this == FURNACE || this == LIT_FURNACE;
    }

    public boolean isContainerBlock() {
        return this == CHEST || isFurnace() || this == CRAFTING_TABLE;
    }

    public boolean isDoor() {
        return this == WOODEN_DOOR || this == IRON_DOOR;
    }

    public boolean isBed() {
        return this == BED;
    }

    public boolean isSign() {
        return this == STANDING_SIGN || this == WALL_SIGN;
    }

    public boolean isFence() {
        return this == FENCE || this == NETHER_BRICK_FENCE;
    }

    public boolean isFenceGate() {
        return this == FENCE_GATE;
    }

    public boolean isStairs() {
        return this == OAK_STAIRS || this == COBBLESTONE_STAIRS || this == BRICK_STAIRS
                || this == STONE_BRICK_STAIRS || this == NETHER_BRICK_STAIRS;
    }

    public boolean isSlab() {
        return this == STONE_SLAB || this == DOUBLE_STONE_SLAB;
    }

    public boolean isWater() {
        return this == FLOWING_WATER || this == WATER;
    }

    public boolean isLava() {
        return this == FLOWING_LAVA || this == LAVA;
    }

    public boolean isFluid() {
        return isWater() || isLava();
    }

    public boolean isFlowingFluid() {
        return this == FLOWING_WATER || this == FLOWING_LAVA;
    }

    public boolean isStillFluid() {
        return this == WATER || this == LAVA;
    }

    public boolean isFallingBlock() {
        return this == SAND || this == GRAVEL;
    }

    public boolean isPlant() {
        return this == SAPLING || this == DEAD_BUSH || this == YELLOW_FLOWER || this == RED_ROSE
                || this == BROWN_MUSHROOM || this == RED_MUSHROOM;
    }

    public boolean isFragileSupportBlock() {
        return this == TORCH || this == FIRE || this == LADDER || isSign() || isPlant();
    }

    public boolean isFlammable() {
        return this == OAK_PLANKS || this == OAK_LOG || this == LEAVES || this == WHITE_WOOL
                || this == CRAFTING_TABLE || this == CHEST || this == WOODEN_DOOR || this == TRAPDOOR
                || this == FENCE || this == FENCE_GATE || this == OAK_STAIRS || this == FENCE_GATE
                || this == SAPLING || this == DEAD_BUSH || this == YELLOW_FLOWER || this == RED_ROSE
                || this == BROWN_MUSHROOM || this == RED_MUSHROOM || this == LADDER || isSign();
    }

    public static BlockType flowingVariant(boolean water) {
        return water ? FLOWING_WATER : FLOWING_LAVA;
    }

    public static BlockType stillVariant(boolean water) {
        return water ? WATER : LAVA;
    }

    public boolean isRenderTransparent() {
        return getRenderLayer() == BlockRenderLayer.TRANSLUCENT;
    }

    public BlockRenderLayer getRenderLayer() {
        return switch (this) {
            case LEAVES, FIRE, SAPLING, DEAD_BUSH, YELLOW_FLOWER, RED_ROSE, BROWN_MUSHROOM, RED_MUSHROOM ->
                BlockRenderLayer.CUTOUT;
            case FLOWING_WATER, WATER, FLOWING_LAVA, LAVA, GLASS, ICE -> BlockRenderLayer.TRANSLUCENT;
            default -> BlockRenderLayer.OPAQUE;
        };
    }

    public int getLightEmission() {
        return switch (this) {
            case TORCH -> 14;
            case FIRE -> 15;
            case LIT_FURNACE -> 13;
            case FLOWING_LAVA, LAVA -> 15;
            default -> 0;
        };
    }

    private static int metadataToFace(int metadata) {
        return switch (metadata) {
            case 2 -> com.craftzero.world.Block.FACE_NORTH;
            case 3 -> com.craftzero.world.Block.FACE_SOUTH;
            case 4 -> com.craftzero.world.Block.FACE_WEST;
            case 5 -> com.craftzero.world.Block.FACE_EAST;
            default -> com.craftzero.world.Block.FACE_NORTH;
        };
    }
}
