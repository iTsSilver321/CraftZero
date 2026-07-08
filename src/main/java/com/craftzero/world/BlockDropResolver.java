package com.craftzero.world;

import com.craftzero.inventory.ItemStack;
import com.craftzero.inventory.ItemType;
import com.craftzero.inventory.ToolType;
import com.craftzero.progression.EnchantmentResolver;
import com.craftzero.progression.EnchantmentType;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Contextual Release 1.0 block drops.
 */
public final class BlockDropResolver {
    private static final int LEAF_SAPLING_CHANCE = 20;
    private static final int TALL_GRASS_SEED_CHANCE = 8;

    private BlockDropResolver() {
    }

    public static ItemStack getDrop(BlockType type, Random random) {
        return getDrop(type, random, null);
    }

    public static ItemStack getDrop(BlockType type, Random random, ItemType tool) {
        List<ItemStack> drops = getDrops(type, 0, random, tool);
        return drops.isEmpty() ? null : drops.get(0);
    }

    public static ItemStack getDropWithToolStack(BlockType type, Random random, ItemStack toolStack) {
        List<ItemStack> drops = getDropsWithToolStack(type, 0, random, toolStack);
        return drops.isEmpty() ? null : drops.get(0);
    }

    public static List<ItemStack> getDrops(BlockType type, int metadata, Random random) {
        return getDrops(type, metadata, random, null);
    }

    public static List<ItemStack> getDrops(BlockType type, int metadata, Random random, ItemType tool) {
        return getDropsInternal(type, metadata, random, tool, 0);
    }

    public static List<ItemStack> getDropsWithToolStack(BlockType type, int metadata, Random random,
            ItemStack toolStack) {
        if (type == null) {
            return List.of();
        }
        ItemType tool = toolStack == null || toolStack.isEmpty() ? null : toolStack.getType();
        if (EnchantmentResolver.has(toolStack, EnchantmentType.SILK_TOUCH) && canSilkHarvest(type, metadata)) {
            ItemType silkDrop = silkTouchDrop(type, metadata);
            return silkDrop == null ? List.of() : List.of(new ItemStack(silkDrop, 1));
        }
        return getDropsInternal(type, metadata, random, tool,
                EnchantmentResolver.getLevel(toolStack, EnchantmentType.FORTUNE));
    }

    private static List<ItemStack> getDropsInternal(BlockType type, int metadata, Random random, ItemType tool,
            int fortune) {
        if (type == null) {
            return List.of();
        }
        int fortuneLevel = Math.max(0, fortune);
        if (type == BlockType.COBWEB) {
            return cobwebDrops(tool);
        }
        if (type == BlockType.CROPS) {
            return cropDrops(metadata, random, fortuneLevel);
        }
        if (type == BlockType.PUMPKIN_STEM || type == BlockType.MELON_STEM) {
            return stemDrops(type, metadata, random, fortuneLevel);
        }
        if (type == BlockType.NETHER_WART) {
            return netherWartDrops(metadata, random);
        }
        if (type == BlockType.LEAVES) {
            if (tool == ItemType.SHEARS) {
                return List.of(new ItemStack(ItemType.fromBlock(BlockType.LEAVES, metadata & 3), 1));
            }
            return leafDrops(metadata, random, fortuneLevel);
        }
        if (type == BlockType.TALL_GRASS) {
            if (tool == ItemType.SHEARS) {
                return List.of(new ItemStack(ItemType.fromBlock(BlockType.TALL_GRASS, metadata), 1));
            }
            return tallGrassDrops(random, fortuneLevel);
        }
        if (type == BlockType.DEAD_BUSH) {
            return List.of();
        }
        if (type == BlockType.VINES) {
            return tool == ItemType.SHEARS ? List.of(new ItemStack(ItemType.VINES, 1)) : List.of();
        }
        if (type == BlockType.GRAVEL) {
            if (gravelDropsFlint(random, fortuneLevel)) {
                return List.of(new ItemStack(ItemType.FLINT, 1));
            }
            return List.of(new ItemStack(ItemType.GRAVEL, 1));
        }
        if (type == BlockType.COAL_ORE) {
            return List.of(new ItemStack(ItemType.COAL, applyOreFortune(1, random, fortuneLevel)));
        }
        if (type == BlockType.DIAMOND_ORE) {
            return List.of(new ItemStack(ItemType.DIAMOND, applyOreFortune(1, random, fortuneLevel)));
        }
        if (type == BlockType.REDSTONE_ORE || type == BlockType.GLOWING_REDSTONE_ORE) {
            int count = randomBetween(random, 4, 5) + randomBonus(random, fortuneLevel);
            return List.of(new ItemStack(ItemType.REDSTONE, count));
        }
        if (type == BlockType.LAPIS_ORE) {
            return List.of(new ItemStack(ItemType.LAPIS_LAZULI,
                    applyOreFortune(randomBetween(random, 4, 8), random, fortuneLevel)));
        }
        if (type == BlockType.CLAY) {
            return List.of(new ItemStack(ItemType.CLAY_BALL, 4));
        }
        if (type == BlockType.SNOW_LAYER) {
            return List.of(new ItemStack(ItemType.SNOWBALL, 1));
        }
        if (type == BlockType.SNOW) {
            return List.of(new ItemStack(ItemType.SNOWBALL, 4));
        }
        if (type == BlockType.GLOWSTONE) {
            int count = Math.min(4, randomBetween(random, 2, 4) + randomBonus(random, fortuneLevel));
            return List.of(new ItemStack(ItemType.GLOWSTONE_DUST, count));
        }
        if (type == BlockType.BOOKSHELF) {
            return List.of(new ItemStack(ItemType.BOOK, 3));
        }
        if (type == BlockType.MELON) {
            int count = Math.min(9, randomBetween(random, 3, 7) + randomBonus(random, fortuneLevel));
            return List.of(new ItemStack(ItemType.MELON_SLICE, count));
        }
        if (type == BlockType.BROWN_MUSHROOM_BLOCK || type == BlockType.RED_MUSHROOM_BLOCK) {
            return hugeMushroomDrops(type, random);
        }

        ItemType droppedItem = defaultDrop(type, metadata);
        if (droppedItem == null) {
            return List.of();
        }
        int count = type == BlockType.DOUBLE_STONE_SLAB ? 2 : 1;
        return List.of(new ItemStack(droppedItem, count));
    }

    private static ItemType defaultDrop(BlockType type, int metadata) {
        if (type == BlockType.DOUBLE_STONE_SLAB) {
            return ItemType.fromBlock(BlockType.STONE_SLAB, metadata & 7);
        }
        ItemType baseDrop = type.getDroppedItem();
        ItemType canonicalBlockItem = ItemType.fromBlock(type);
        if (baseDrop == canonicalBlockItem) {
            ItemType metadataDrop = ItemType.fromBlock(type, metadata & 15);
            if (metadataDrop != null) {
                return metadataDrop;
            }
        }
        return baseDrop;
    }

    private static List<ItemStack> cobwebDrops(ItemType tool) {
        if (tool == ItemType.SHEARS || isSword(tool)) {
            return List.of(new ItemStack(ItemType.STRING, 1));
        }
        return List.of();
    }

    private static boolean isSword(ItemType tool) {
        return tool != null
                && tool.isTool()
                && tool.getToolType().getCategory() == ToolType.Category.SWORD;
    }

    private static List<ItemStack> cropDrops(int metadata, Random random, int fortune) {
        int age = metadata & 7;
        if (age < 7) {
            return List.of(new ItemStack(ItemType.SEEDS, 1));
        }
        List<ItemStack> drops = new ArrayList<>();
        drops.add(new ItemStack(ItemType.WHEAT, 1));
        int seedCount = 0;
        for (int i = 0; i < 3 + Math.max(0, fortune); i++) {
            if (random == null || random.nextInt(15) <= age) {
                seedCount++;
            }
        }
        if (seedCount > 0) {
            drops.add(new ItemStack(ItemType.SEEDS, seedCount));
        }
        return drops;
    }

    private static List<ItemStack> netherWartDrops(int metadata, Random random) {
        int age = metadata & 3;
        int count = age >= 3 ? randomBetween(random, 2, 4) : 1;
        return List.of(new ItemStack(ItemType.NETHER_WART, count));
    }

    private static List<ItemStack> stemDrops(BlockType type, int metadata, Random random, int fortune) {
        ItemType seed = type == BlockType.PUMPKIN_STEM ? ItemType.PUMPKIN_SEEDS : ItemType.MELON_SEEDS;
        int age = metadata & 7;
        int count = 0;
        for (int i = 0; i < 3 + Math.max(0, fortune); i++) {
            if (random == null || random.nextInt(15) <= age) {
                count++;
            }
        }
        return count == 0 ? List.of() : List.of(new ItemStack(seed, count));
    }

    private static List<ItemStack> hugeMushroomDrops(BlockType type, Random random) {
        ItemType mushroom = type == BlockType.BROWN_MUSHROOM_BLOCK ? ItemType.BROWN_MUSHROOM : ItemType.RED_MUSHROOM;
        int count = random == null ? 0 : Math.max(0, random.nextInt(10) - 7);
        return count == 0 ? List.of() : List.of(new ItemStack(mushroom, count));
    }

    private static List<ItemStack> leafDrops(int metadata, Random random, int fortune) {
        int treeType = metadata & 3;
        List<ItemStack> drops = new ArrayList<>();
        if (chance(random, leafSaplingChance(fortune))) {
            ItemType sapling = ItemType.fromBlock(BlockType.SAPLING, treeType);
            if (sapling != null) {
                drops.add(new ItemStack(sapling, 1));
            }
        }
        return drops;
    }

    private static List<ItemStack> tallGrassDrops(Random random, int fortune) {
        if (random == null) {
            return List.of();
        }
        int attempts = fortune > 0 ? 1 + random.nextInt(fortune * 2 + 1) : 1;
        int seedCount = 0;
        for (int i = 0; i < attempts; i++) {
            if (chance(random, TALL_GRASS_SEED_CHANCE)) {
                seedCount++;
            }
        }
        return seedCount == 0 ? List.of() : List.of(new ItemStack(ItemType.SEEDS, seedCount));
    }

    private static boolean chance(Random random, int oneIn) {
        return random != null && random.nextInt(oneIn) == 0;
    }

    private static int leafSaplingChance(int fortune) {
        int chance = LEAF_SAPLING_CHANCE;
        if (fortune > 0) {
            chance -= 2 << fortune;
            if (chance < 10) {
                chance = 10;
            }
        }
        return chance;
    }

    private static boolean gravelDropsFlint(Random random, int fortune) {
        if (random == null) {
            return false;
        }
        int effectiveFortune = Math.min(3, Math.max(0, fortune));
        return random.nextInt(10 - effectiveFortune * 3) == 0;
    }

    private static int applyOreFortune(int base, Random random, int fortune) {
        if (fortune <= 0 || random == null) {
            return base;
        }
        int multiplier = random.nextInt(fortune + 2) - 1;
        if (multiplier < 0) {
            multiplier = 0;
        }
        return base * (multiplier + 1);
    }

    private static int randomBonus(Random random, int fortune) {
        return fortune <= 0 || random == null ? 0 : random.nextInt(fortune + 1);
    }

    private static int randomBetween(Random random, int min, int max) {
        if (random == null) {
            return min;
        }
        return min + random.nextInt(max - min + 1);
    }

    private static boolean canSilkHarvest(BlockType type, int metadata) {
        if (type == null || type.getBreakHardness() < 0.0f || type.hasTileEntity()) {
            return false;
        }
        return switch (type) {
            case AIR, BEDROCK, FLOWING_WATER, WATER, FLOWING_LAVA, LAVA, FIRE,
                    CAKE, MOB_SPAWNER, PORTAL, END_PORTAL, END_PORTAL_FRAME,
                    MOVING_PISTON, PISTON_HEAD, CROPS, PUMPKIN_STEM, MELON_STEM,
                    NETHER_WART, ICE, GLASS_PANE, INFESTED_STONE, DOUBLE_STONE_SLAB -> false;
            case LEAVES -> silkTouchDrop(type, metadata) != null;
            default -> BlockShape.isFullCube(type, metadata)
                    && silkTouchDrop(type, metadata) != null;
        };
    }

    private static ItemType silkTouchDrop(BlockType type, int metadata) {
        if (type == BlockType.GLOWING_REDSTONE_ORE) {
            return ItemType.REDSTONE_ORE;
        }
        return ItemType.fromBlock(type, metadata & 15);
    }
}
