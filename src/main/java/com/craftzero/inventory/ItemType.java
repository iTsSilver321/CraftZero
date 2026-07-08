package com.craftzero.inventory;

import com.craftzero.world.BlockType;

import java.util.HashMap;
import java.util.Map;

/**
 * Item registry targeting Minecraft Java Release 1.0 item IDs.
 * Block items keep their canonical block ID.
 */
public enum ItemType {
    STONE(1, BlockType.STONE),
    GRASS(2, BlockType.GRASS),
    DIRT(3, BlockType.DIRT),
    COBBLESTONE(4, BlockType.COBBLESTONE),
    OAK_PLANKS(5, BlockType.OAK_PLANKS),
    SAPLING(6, BlockType.SAPLING),
    SPRUCE_SAPLING(6, 1, 64, BlockType.SAPLING, 1),
    BIRCH_SAPLING(6, 2, 64, BlockType.SAPLING, 2),
    BEDROCK(7, BlockType.BEDROCK),
    SAND(12, BlockType.SAND),
    GRAVEL(13, BlockType.GRAVEL),
    GOLD_ORE(14, BlockType.GOLD_ORE),
    IRON_ORE(15, BlockType.IRON_ORE),
    COAL_ORE(16, BlockType.COAL_ORE),
    OAK_LOG(17, BlockType.OAK_LOG),
    SPRUCE_LOG(17, 1, 64, BlockType.OAK_LOG, 1),
    BIRCH_LOG(17, 2, 64, BlockType.OAK_LOG, 2),
    LEAVES(18, BlockType.LEAVES),
    SPRUCE_LEAVES(18, 1, 64, BlockType.LEAVES, 1),
    BIRCH_LEAVES(18, 2, 64, BlockType.LEAVES, 2),
    SPONGE(19, BlockType.SPONGE),
    GLASS(20, BlockType.GLASS),
    LAPIS_ORE(21, BlockType.LAPIS_ORE),
    LAPIS_BLOCK(22, BlockType.LAPIS_BLOCK),
    DISPENSER(23, BlockType.DISPENSER),
    SANDSTONE(24, BlockType.SANDSTONE),
    NOTE_BLOCK(25, BlockType.NOTE_BLOCK),
    POWERED_RAIL(27, BlockType.POWERED_RAIL),
    DETECTOR_RAIL(28, BlockType.DETECTOR_RAIL),
    STICKY_PISTON(29, BlockType.STICKY_PISTON),
    COBWEB(30, BlockType.COBWEB),
    SHRUB(31, BlockType.TALL_GRASS),
    TALL_GRASS(31, 1, 64, BlockType.TALL_GRASS, 1),
    FERN(31, 2, 64, BlockType.TALL_GRASS, 2),
    DEAD_BUSH(32, BlockType.DEAD_BUSH),
    PISTON(33, BlockType.PISTON),
    YELLOW_FLOWER(37, BlockType.YELLOW_FLOWER),
    RED_ROSE(38, BlockType.RED_ROSE),
    BROWN_MUSHROOM(39, BlockType.BROWN_MUSHROOM),
    RED_MUSHROOM(40, BlockType.RED_MUSHROOM),
    GOLD_BLOCK(41, BlockType.GOLD_BLOCK),
    IRON_BLOCK(42, BlockType.IRON_BLOCK),
    DOUBLE_STONE_SLAB(43, 0, 64, BlockType.DOUBLE_STONE_SLAB, 0),
    DOUBLE_SANDSTONE_SLAB(43, 1, 64, BlockType.DOUBLE_STONE_SLAB, 1),
    DOUBLE_WOODEN_SLAB(43, 2, 64, BlockType.DOUBLE_STONE_SLAB, 2),
    DOUBLE_COBBLESTONE_SLAB(43, 3, 64, BlockType.DOUBLE_STONE_SLAB, 3),
    DOUBLE_BRICK_SLAB(43, 4, 64, BlockType.DOUBLE_STONE_SLAB, 4),
    DOUBLE_STONE_BRICK_SLAB(43, 5, 64, BlockType.DOUBLE_STONE_SLAB, 5),
    STONE_SLAB(44, BlockType.STONE_SLAB),
    SANDSTONE_SLAB(44, 1, 64, BlockType.STONE_SLAB, 1),
    WOODEN_SLAB(44, 2, 64, BlockType.STONE_SLAB, 2),
    COBBLESTONE_SLAB(44, 3, 64, BlockType.STONE_SLAB, 3),
    BRICK_SLAB(44, 4, 64, BlockType.STONE_SLAB, 4),
    STONE_BRICK_SLAB(44, 5, 64, BlockType.STONE_SLAB, 5),
    WHITE_WOOL(35, 0, 64, BlockType.WHITE_WOOL, 0),
    ORANGE_WOOL(35, 1, 64, BlockType.WHITE_WOOL, 1),
    MAGENTA_WOOL(35, 2, 64, BlockType.WHITE_WOOL, 2),
    LIGHT_BLUE_WOOL(35, 3, 64, BlockType.WHITE_WOOL, 3),
    YELLOW_WOOL(35, 4, 64, BlockType.WHITE_WOOL, 4),
    LIME_WOOL(35, 5, 64, BlockType.WHITE_WOOL, 5),
    PINK_WOOL(35, 6, 64, BlockType.WHITE_WOOL, 6),
    GRAY_WOOL(35, 7, 64, BlockType.WHITE_WOOL, 7),
    LIGHT_GRAY_WOOL(35, 8, 64, BlockType.WHITE_WOOL, 8),
    CYAN_WOOL(35, 9, 64, BlockType.WHITE_WOOL, 9),
    PURPLE_WOOL(35, 10, 64, BlockType.WHITE_WOOL, 10),
    BLUE_WOOL(35, 11, 64, BlockType.WHITE_WOOL, 11),
    BROWN_WOOL(35, 12, 64, BlockType.WHITE_WOOL, 12),
    GREEN_WOOL(35, 13, 64, BlockType.WHITE_WOOL, 13),
    RED_WOOL(35, 14, 64, BlockType.WHITE_WOOL, 14),
    BLACK_WOOL(35, 15, 64, BlockType.WHITE_WOOL, 15),
    BRICK(45, BlockType.BRICK),
    TNT(46, BlockType.TNT),
    BOOKSHELF(47, BlockType.BOOKSHELF),
    MOSSY_COBBLESTONE(48, BlockType.MOSSY_COBBLESTONE),
    OBSIDIAN(49, BlockType.OBSIDIAN),
    TORCH(50, BlockType.TORCH),
    FIRE(51, BlockType.FIRE),
    MOB_SPAWNER(52, BlockType.MOB_SPAWNER),
    OAK_STAIRS(53, BlockType.OAK_STAIRS),
    CHEST(54, BlockType.CHEST),
    DIAMOND_ORE(56, BlockType.DIAMOND_ORE),
    DIAMOND_BLOCK(57, BlockType.DIAMOND_BLOCK),
    CRAFTING_TABLE(58, BlockType.CRAFTING_TABLE),
    FARMLAND(60, BlockType.FARMLAND),
    FURNACE(61, BlockType.FURNACE),
    LADDER(65, BlockType.LADDER),
    RAIL(66, BlockType.RAIL),
    COBBLESTONE_STAIRS(67, BlockType.COBBLESTONE_STAIRS),
    LEVER(69, BlockType.LEVER),
    STONE_PRESSURE_PLATE(70, BlockType.STONE_PRESSURE_PLATE),
    WOODEN_PRESSURE_PLATE(72, BlockType.WOODEN_PRESSURE_PLATE),
    REDSTONE_ORE(73, BlockType.REDSTONE_ORE),
    REDSTONE_TORCH(76, 64, BlockType.REDSTONE_TORCH_ON, 3, 6),
    STONE_BUTTON(77, BlockType.STONE_BUTTON),
    SNOW_LAYER(78, BlockType.SNOW_LAYER),
    ICE(79, BlockType.ICE),
    SNOW(80, BlockType.SNOW),
    CACTUS(81, BlockType.CACTUS),
    CLAY(82, BlockType.CLAY),
    JUKEBOX(84, BlockType.JUKEBOX),
    FENCE(85, BlockType.FENCE),
    PUMPKIN(86, BlockType.PUMPKIN),
    NETHERRACK(87, BlockType.NETHERRACK),
    SOUL_SAND(88, BlockType.SOUL_SAND),
    GLOWSTONE(89, BlockType.GLOWSTONE),
    JACK_O_LANTERN(91, BlockType.JACK_O_LANTERN),
    LOCKED_CHEST(95, BlockType.LOCKED_CHEST),
    TRAPDOOR(96, BlockType.TRAPDOOR),
    INFESTED_STONE(97, 0, 64, BlockType.INFESTED_STONE, 0),
    INFESTED_COBBLESTONE(97, 1, 64, BlockType.INFESTED_STONE, 1),
    INFESTED_STONE_BRICK(97, 2, 64, BlockType.INFESTED_STONE, 2),
    STONE_BRICK(98, BlockType.STONE_BRICK),
    MOSSY_STONE_BRICK(98, 1, 64, BlockType.STONE_BRICK, 1),
    CRACKED_STONE_BRICK(98, 2, 64, BlockType.STONE_BRICK, 2),
    CHISELED_STONE_BRICK(98, 3, 64, BlockType.STONE_BRICK, 3),
    BROWN_MUSHROOM_BLOCK(99, BlockType.BROWN_MUSHROOM_BLOCK),
    RED_MUSHROOM_BLOCK(100, BlockType.RED_MUSHROOM_BLOCK),
    IRON_BARS(101, BlockType.IRON_BARS),
    GLASS_PANE(102, BlockType.GLASS_PANE),
    MELON_BLOCK(103, BlockType.MELON),
    VINES(106, BlockType.VINES),
    FENCE_GATE(107, BlockType.FENCE_GATE),
    BRICK_STAIRS(108, BlockType.BRICK_STAIRS),
    STONE_BRICK_STAIRS(109, BlockType.STONE_BRICK_STAIRS),
    MYCELIUM(110, BlockType.MYCELIUM),
    LILY_PAD(111, BlockType.LILY_PAD),
    NETHER_BRICK(112, BlockType.NETHER_BRICK),
    NETHER_BRICK_FENCE(113, BlockType.NETHER_BRICK_FENCE),
    NETHER_BRICK_STAIRS(114, BlockType.NETHER_BRICK_STAIRS),
    ENCHANTING_TABLE(116, BlockType.ENCHANTING_TABLE),
    END_PORTAL_FRAME(120, BlockType.END_PORTAL_FRAME),
    END_STONE(121, BlockType.END_STONE),
    DRAGON_EGG(122, BlockType.DRAGON_EGG),

    IRON_SHOVEL(256, ToolType.IRON_SHOVEL, 2, 5),
    IRON_PICKAXE(257, ToolType.IRON_PICKAXE, 2, 6),
    IRON_AXE(258, ToolType.IRON_AXE, 2, 7),
    FLINT_AND_STEEL(259, 1, 64, 5, 0, true),
    APPLE(260, 64, 10, 0),
    BOW(261, 1, 385, 5, 1, true),
    ARROW(262, 64, 5, 2),
    COAL(263, 0, 64, 7, 4),
    CHARCOAL(263, 1, 64, 7, 4),
    DIAMOND(264, 64, 7, 3),
    IRON_INGOT(265, 64, 7, 1),
    GOLD_INGOT(266, 64, 7, 2),
    IRON_SWORD(267, ToolType.IRON_SWORD, 2, 4),
    WOODEN_SWORD(268, ToolType.WOODEN_SWORD, 0, 4),
    WOODEN_SHOVEL(269, ToolType.WOODEN_SHOVEL, 0, 5),
    WOODEN_PICKAXE(270, ToolType.WOODEN_PICKAXE, 0, 6),
    WOODEN_AXE(271, ToolType.WOODEN_AXE, 0, 7),
    STONE_SWORD(272, ToolType.STONE_SWORD, 1, 4),
    STONE_SHOVEL(273, ToolType.STONE_SHOVEL, 1, 5),
    STONE_PICKAXE(274, ToolType.STONE_PICKAXE, 1, 6),
    STONE_AXE(275, ToolType.STONE_AXE, 1, 7),
    DIAMOND_SWORD(276, ToolType.DIAMOND_SWORD, 3, 4),
    DIAMOND_SHOVEL(277, ToolType.DIAMOND_SHOVEL, 3, 5),
    DIAMOND_PICKAXE(278, ToolType.DIAMOND_PICKAXE, 3, 6),
    DIAMOND_AXE(279, ToolType.DIAMOND_AXE, 3, 7),
    STICK(280, 64, 5, 3),
    BOWL(281, 64, 7, 5),
    MUSHROOM_STEW(282, 1, 8, 4),
    GOLD_SWORD(283, ToolType.GOLD_SWORD, 4, 4),
    GOLD_SHOVEL(284, ToolType.GOLD_SHOVEL, 4, 5),
    GOLD_PICKAXE(285, ToolType.GOLD_PICKAXE, 4, 6),
    GOLD_AXE(286, ToolType.GOLD_AXE, 4, 7),
    STRING(287, 64, 8, 1),
    FEATHER(288, 64, 8, 2),
    GUNPOWDER(289, 64, 8, 4),
    WOODEN_HOE(290, ToolType.WOODEN_HOE, 6, 0),
    STONE_HOE(291, ToolType.STONE_HOE, 6, 1),
    IRON_HOE(292, ToolType.IRON_HOE, 6, 2),
    DIAMOND_HOE(293, ToolType.DIAMOND_HOE, 6, 3),
    GOLD_HOE(294, ToolType.GOLD_HOE, 6, 4),
    SEEDS(295, 64, 9, 0),
    WHEAT(296, 64, 9, 1),
    BREAD(297, 64, 9, 2),
    LEATHER_HELMET(298, 1, 56, 0, 0, true),
    LEATHER_CHESTPLATE(299, 1, 81, 1, 0, true),
    LEATHER_LEGGINGS(300, 1, 76, 2, 0, true),
    LEATHER_BOOTS(301, 1, 66, 3, 0, true),
    CHAIN_HELMET(302, 1, 166, 0, 1, true),
    CHAIN_CHESTPLATE(303, 1, 241, 1, 1, true),
    CHAIN_LEGGINGS(304, 1, 226, 2, 1, true),
    CHAIN_BOOTS(305, 1, 196, 3, 1, true),
    IRON_HELMET(306, 1, 166, 0, 2, true),
    IRON_CHESTPLATE(307, 1, 241, 1, 2, true),
    IRON_LEGGINGS(308, 1, 226, 2, 2, true),
    IRON_BOOTS(309, 1, 196, 3, 2, true),
    DIAMOND_HELMET(310, 1, 364, 0, 3, true),
    DIAMOND_CHESTPLATE(311, 1, 529, 1, 3, true),
    DIAMOND_LEGGINGS(312, 1, 496, 2, 3, true),
    DIAMOND_BOOTS(313, 1, 430, 3, 3, true),
    GOLD_HELMET(314, 1, 78, 0, 4, true),
    GOLD_CHESTPLATE(315, 1, 113, 1, 4, true),
    GOLD_LEGGINGS(316, 1, 106, 2, 4, true),
    GOLD_BOOTS(317, 1, 92, 3, 4, true),
    FLINT(318, 64, 6, 0),
    RAW_PORKCHOP(319, 64, 7, 5),
    COOKED_PORKCHOP(320, 64, 8, 5),
    PAINTING(321, 64, 10, 1),
    GOLDEN_APPLE(322, 64, 11, 0),
    SIGN(323, 16, BlockType.STANDING_SIGN, 10, 2),
    WOODEN_DOOR(324, 1, BlockType.WOODEN_DOOR, 11, 2),
    BUCKET(325, 16, 10, 4),
    WATER_BUCKET(326, 1, 11, 4),
    LAVA_BUCKET(327, 1, 12, 4),
    MINECART(328, 1, 7, 8),
    SADDLE(329, 1, 8, 6),
    IRON_DOOR(330, 1, BlockType.IRON_DOOR, 12, 2),
    REDSTONE(331, 64, BlockType.REDSTONE_WIRE, 8, 3),
    SNOWBALL(332, 16, 14, 0),
    BOAT(333, 1, 8, 8),
    LEATHER(334, 64, 7, 6),
    MILK_BUCKET(335, 1, 13, 4),
    BRICK_ITEM(336, 64, 6, 1),
    CLAY_BALL(337, 64, 9, 3),
    SUGAR_CANE(338, 64, BlockType.SUGAR_CANE, 11, 1),
    PAPER(339, 64, 10, 3),
    BOOK(340, 64, 11, 3),
    SLIMEBALL(341, 64, 14, 1),
    CHEST_MINECART(342, 1, 7, 9),
    FURNACE_MINECART(343, 1, 7, 10),
    EGG(344, 16, 12, 0),
    COMPASS(345, 64, 6, 3),
    FISHING_ROD(346, 1, 64, 5, 4, true),
    CLOCK(347, 64, 6, 4),
    GLOWSTONE_DUST(348, 64, 9, 4),
    RAW_FISH(349, 64, 9, 5),
    COOKED_FISH(350, 64, 10, 5),
    INK_SAC(351, 0, 64, 8, 7),
    ROSE_RED(351, 1, 64, 9, 7),
    CACTUS_GREEN(351, 2, 64, 9, 7),
    COCOA_BEANS(351, 3, 64, 9, 7),
    LAPIS_LAZULI(351, 4, 64, 9, 7),
    PURPLE_DYE(351, 5, 64, 9, 7),
    CYAN_DYE(351, 6, 64, 9, 7),
    LIGHT_GRAY_DYE(351, 7, 64, 9, 7),
    GRAY_DYE(351, 8, 64, 9, 7),
    PINK_DYE(351, 9, 64, 9, 7),
    LIME_DYE(351, 10, 64, 9, 7),
    DANDELION_YELLOW(351, 11, 64, 9, 7),
    LIGHT_BLUE_DYE(351, 12, 64, 9, 7),
    MAGENTA_DYE(351, 13, 64, 9, 7),
    ORANGE_DYE(351, 14, 64, 9, 7),
    BONE_MEAL(351, 15, 64, 9, 7),
    BONE(352, 64, 7, 7),
    SUGAR(353, 64, 13, 0),
    CAKE(354, 1, BlockType.CAKE, 13, 1),
    BED(355, 1, BlockType.BED, 13, 2),
    REDSTONE_REPEATER(356, 64, BlockType.REDSTONE_REPEATER_OFF, 6, 5),
    COOKIE(357, 64, 12, 5),
    MAP(358, 1, 12, 3),
    SHEARS(359, 1, 238, 13, 5, true),
    MELON_SLICE(360, 64, 13, 6),
    PUMPKIN_SEEDS(361, 64, 13, 3),
    MELON_SEEDS(362, 64, 14, 3),
    RAW_BEEF(363, 64, 8, 5),
    STEAK(364, 64, 9, 5),
    RAW_CHICKEN(365, 64, 9, 5),
    COOKED_CHICKEN(366, 64, 10, 5),
    ROTTEN_FLESH(367, 64, 9, 6),
    ENDER_PEARL(368, 16, 11, 5),
    BLAZE_ROD(369, 64, 12, 6),
    GHAST_TEAR(370, 64, 11, 6),
    GOLD_NUGGET(371, 64, 12, 7),
    NETHER_WART(372, 64, BlockType.NETHER_WART, 11, 7),
    POTION(373, 1, 13, 7),
    GLASS_BOTTLE(374, 64, 12, 4),
    SPIDER_EYE(375, 64, 11, 8),
    FERMENTED_SPIDER_EYE(376, 64, 12, 8),
    BLAZE_POWDER(377, 64, 13, 8),
    MAGMA_CREAM(378, 64, 13, 9),
    BREWING_STAND(379, 64, BlockType.BREWING_STAND, 12, 9),
    CAULDRON(380, 64, BlockType.CAULDRON, 10, 9),
    EYE_OF_ENDER(381, 64, 11, 9),
    GLISTERING_MELON(382, 64, 9, 9),
    RECORD_13(2256, 1, 0, 15),
    RECORD_CAT(2257, 1, 1, 15),
    RECORD_BLOCKS(2258, 1, 2, 15),
    RECORD_CHIRP(2259, 1, 3, 15),
    RECORD_FAR(2260, 1, 4, 15),
    RECORD_MALL(2261, 1, 5, 15),
    RECORD_MELLOHI(2262, 1, 6, 15),
    RECORD_STAL(2263, 1, 7, 15),
    RECORD_STRAD(2264, 1, 8, 15),
    RECORD_WARD(2265, 1, 9, 15),
    RECORD_11(2266, 1, 10, 15);

    private static final Map<Long, ItemType> BY_ID_AND_DATA = new HashMap<>();
    private static final Map<BlockType, ItemType> BY_BLOCK = new HashMap<>();
    private static final Map<Long, ItemType> BY_BLOCK_AND_DATA = new HashMap<>();

    static {
        for (ItemType type : values()) {
            ItemType previous = BY_ID_AND_DATA.put(key(type.id, type.dataValue), type);
            if (previous != null) {
                throw new IllegalStateException(
                        "Duplicate item id/data " + type.id + ":" + type.dataValue
                                + " for " + previous.name() + " and " + type.name());
            }

            if (type.placedBlock != null) {
                ItemType previousVariant = BY_BLOCK_AND_DATA.put(key(type.placedBlock.getId(), type.placedBlockMetadata),
                        type);
                if (previousVariant != null) {
                    throw new IllegalStateException(
                            "Duplicate block item for " + type.placedBlock + ":" + type.placedBlockMetadata + ": "
                                    + previousVariant.name() + " and " + type.name());
                }
                if (type.placedBlockMetadata == 0 && BY_BLOCK.put(type.placedBlock, type) != null) {
                    throw new IllegalStateException(
                            "Duplicate canonical block item for " + type.placedBlock);
                }
            }
        }
    }

    private final int id;
    private final int dataValue;
    private final int maxStackSize;
    private final int maxDurability;
    private final BlockType placedBlock;
    private final int placedBlockMetadata;
    private final ToolType toolType;
    private final int itemTextureCol;
    private final int itemTextureRow;

    ItemType(int id, BlockType placedBlock) {
        this.id = id;
        this.dataValue = 0;
        this.maxStackSize = 64;
        this.maxDurability = -1;
        this.placedBlock = placedBlock;
        this.placedBlockMetadata = 0;
        this.toolType = ToolType.NONE;
        this.itemTextureCol = -1;
        this.itemTextureRow = -1;
    }

    ItemType(int id, int maxStackSize, BlockType placedBlock, int itemTextureCol, int itemTextureRow) {
        this.id = id;
        this.dataValue = 0;
        this.maxStackSize = maxStackSize;
        this.maxDurability = -1;
        this.placedBlock = placedBlock;
        this.placedBlockMetadata = 0;
        this.toolType = ToolType.NONE;
        this.itemTextureCol = itemTextureCol;
        this.itemTextureRow = itemTextureRow;
    }

    ItemType(int id, int dataValue, int maxStackSize, BlockType placedBlock, int placedBlockMetadata) {
        this.id = id;
        this.dataValue = dataValue;
        this.maxStackSize = maxStackSize;
        this.maxDurability = -1;
        this.placedBlock = placedBlock;
        this.placedBlockMetadata = placedBlockMetadata;
        this.toolType = ToolType.NONE;
        this.itemTextureCol = -1;
        this.itemTextureRow = -1;
    }

    ItemType(int id, int dataValue, int maxStackSize, BlockType placedBlock, int placedBlockMetadata,
            int itemTextureCol, int itemTextureRow) {
        this.id = id;
        this.dataValue = dataValue;
        this.maxStackSize = maxStackSize;
        this.maxDurability = -1;
        this.placedBlock = placedBlock;
        this.placedBlockMetadata = placedBlockMetadata;
        this.toolType = ToolType.NONE;
        this.itemTextureCol = itemTextureCol;
        this.itemTextureRow = itemTextureRow;
    }

    ItemType(int id, ToolType toolType, int itemTextureCol, int itemTextureRow) {
        this.id = id;
        this.dataValue = 0;
        this.maxStackSize = 1;
        this.maxDurability = toolType.getMaxDurability();
        this.placedBlock = null;
        this.placedBlockMetadata = 0;
        this.toolType = toolType;
        this.itemTextureCol = itemTextureCol;
        this.itemTextureRow = itemTextureRow;
    }

    ItemType(int id, int maxStackSize, int itemTextureCol, int itemTextureRow) {
        this(id, 0, maxStackSize, itemTextureCol, itemTextureRow);
    }

    ItemType(int id, int maxStackSize, int maxDurability, int itemTextureCol, int itemTextureRow,
            boolean damageable) {
        this.id = id;
        this.dataValue = 0;
        this.maxStackSize = maxStackSize;
        this.maxDurability = damageable ? maxDurability : -1;
        this.placedBlock = null;
        this.placedBlockMetadata = 0;
        this.toolType = ToolType.NONE;
        this.itemTextureCol = itemTextureCol;
        this.itemTextureRow = itemTextureRow;
    }

    ItemType(int id, int dataValue, int maxStackSize, int itemTextureCol, int itemTextureRow) {
        this.id = id;
        this.dataValue = dataValue;
        this.maxStackSize = maxStackSize;
        this.maxDurability = -1;
        this.placedBlock = null;
        this.placedBlockMetadata = 0;
        this.toolType = ToolType.NONE;
        this.itemTextureCol = itemTextureCol;
        this.itemTextureRow = itemTextureRow;
    }

    public int getId() {
        return id;
    }

    public int getDataValue() {
        return dataValue;
    }

    public int getMaxStackSize() {
        return maxStackSize;
    }

    public int getMaxDurability() {
        return maxDurability;
    }

    public boolean isDamageable() {
        return maxDurability > 0;
    }

    public boolean isBlockItem() {
        return placedBlock != null;
    }

    public boolean isPlaceable() {
        return placedBlock != null && placedBlock != BlockType.AIR && placedBlock != BlockType.LIT_FURNACE;
    }

    public BlockType getPlacedBlock() {
        return placedBlock;
    }

    public int getPlacedBlockMetadata() {
        return placedBlockMetadata;
    }

    public ItemType getContainerItem() {
        return switch (this) {
            case WATER_BUCKET, LAVA_BUCKET, MILK_BUCKET -> BUCKET;
            default -> null;
        };
    }

    public ItemType getCraftingRemainder() {
        return getContainerItem();
    }

    public boolean isTool() {
        return toolType != ToolType.NONE;
    }

    public boolean isRecord() {
        return id >= 2256 && id <= 2266;
    }

    public ToolType getToolType() {
        return toolType;
    }

    public ItemRenderProfile getRenderProfile() {
        if (isFullCubeBlockItem()) {
            return ItemRenderProfile.block();
        }
        if (isBlockItem() && !usesItemTexture()) {
            return ItemRenderProfile.terrainSprite();
        }
        if (isTool() || this == BOW) {
            return ItemRenderProfile.toolSprite();
        }
        return switch (this) {
            case STICK, ARROW, BONE, STRING, FEATHER -> ItemRenderProfile.skinnySprite();
            case SIGN, WOODEN_DOOR, IRON_DOOR, BED, BUCKET, WATER_BUCKET, LAVA_BUCKET, MILK_BUCKET,
                    MINECART, CHEST_MINECART, FURNACE_MINECART, BOAT, CAKE, BREWING_STAND, CAULDRON ->
                ItemRenderProfile.largeSprite();
            default -> ItemRenderProfile.materialSprite();
        };
    }

    public boolean isFullCubeBlockItem() {
        if (!isBlockItem() || usesItemTexture()) {
            return false;
        }
        return switch (placedBlock) {
            case SAPLING, POWERED_RAIL, DETECTOR_RAIL, COBWEB, TALL_GRASS, DEAD_BUSH,
                    YELLOW_FLOWER, RED_ROSE, BROWN_MUSHROOM, RED_MUSHROOM, TORCH, FIRE,
                    MOB_SPAWNER, REDSTONE_WIRE, CROPS, FARMLAND, LADDER, RAIL, LEVER,
                    STONE_PRESSURE_PLATE, WOODEN_PRESSURE_PLATE, REDSTONE_TORCH_OFF,
                    REDSTONE_TORCH_ON, STONE_BUTTON, CACTUS, SUGAR_CANE, CAKE,
                    REDSTONE_REPEATER_OFF, REDSTONE_REPEATER_ON, TRAPDOOR, IRON_BARS,
                    GLASS_PANE, VINES, LILY_PAD, NETHER_WART, BREWING_STAND, CAULDRON,
                    STANDING_SIGN, WALL_SIGN, WOODEN_DOOR, IRON_DOOR, BED, STONE_SLAB,
                    DOUBLE_STONE_SLAB, OAK_STAIRS, COBBLESTONE_STAIRS, BRICK_STAIRS,
                    STONE_BRICK_STAIRS, FENCE, NETHER_BRICK_FENCE, FENCE_GATE,
                    ENCHANTING_TABLE, END_PORTAL_FRAME, DRAGON_EGG -> false;
            default -> placedBlock.isSolid() && !placedBlock.isTransparent();
        };
    }

    public boolean usesItemTexture() {
        return itemTextureCol >= 0 && itemTextureRow >= 0;
    }

    public int[] getItemTexturePos() {
        return usesItemTexture() ? new int[] { itemTextureCol, itemTextureRow } : null;
    }

    public float[] getTextureCoords(int face) {
        return placedBlock != null ? placedBlock.getTextureCoords(face, placedBlockMetadata) : new float[] { 0, 0, 0, 0 };
    }

    public String getDisplayName() {
        String[] parts = name().split("_");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (sb.length() > 0) {
                sb.append(' ');
            }
            sb.append(part.charAt(0)).append(part.substring(1).toLowerCase());
        }
        return sb.toString();
    }

    public static ItemType fromId(int id) {
        return fromId(id, 0);
    }

    public static ItemType fromId(int id, int dataValue) {
        ItemType exact = BY_ID_AND_DATA.get(key(id, dataValue));
        return exact != null ? exact : BY_ID_AND_DATA.get(key(id, 0));
    }

    public static boolean hasId(int id) {
        return hasId(id, 0);
    }

    public static boolean hasId(int id, int dataValue) {
        return BY_ID_AND_DATA.containsKey(key(id, dataValue));
    }

    public static ItemType fromBlock(BlockType block) {
        return BY_BLOCK.get(block);
    }

    public static ItemType fromBlock(BlockType block, int metadata) {
        ItemType exact = BY_BLOCK_AND_DATA.get(key(block.getId(), metadata & 15));
        return exact != null ? exact : fromBlock(block);
    }

    private static long key(int id, int dataValue) {
        return (((long) id) << 32) ^ (dataValue & 0xFFFFFFFFL);
    }
}
