package com.craftzero.world;

import com.craftzero.inventory.ItemStack;
import com.craftzero.inventory.ItemType;
import com.craftzero.progression.EnchantmentInstance;
import com.craftzero.progression.EnchantmentType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class BlockDropResolverTest {
    @Test
    @DisplayName("Gravel should sometimes drop flint and otherwise drop gravel")
    void gravelDropsFlintByChance() {
        ItemStack flint = BlockDropResolver.getDrop(BlockType.GRAVEL, fixedIntRandom(0));
        ItemStack gravel = BlockDropResolver.getDrop(BlockType.GRAVEL, fixedIntRandom(1));

        assertSame(ItemType.FLINT, flint.getType());
        assertEquals(1, flint.getCount());
        assertSame(ItemType.GRAVEL, gravel.getType());
        assertEquals(1, gravel.getCount());
    }

    @Test
    @DisplayName("Simple block drops should preserve existing drop identities")
    void simpleDropsPreserveExistingIdentities() {
        assertSame(ItemType.COBBLESTONE, BlockDropResolver.getDrop(BlockType.STONE, fixedRandom(0.0f)).getType());
        assertSame(ItemType.STONE_SLAB, BlockDropResolver.getDrop(BlockType.DOUBLE_STONE_SLAB, fixedRandom(0.0f)).getType());
        assertEquals(2, BlockDropResolver.getDrop(BlockType.DOUBLE_STONE_SLAB, fixedRandom(0.0f)).getCount());
    }

    @Test
    @DisplayName("Glass-family and uncollectable blocks should not drop themselves")
    void glassAndIceDoNotDrop() {
        assertNull(BlockDropResolver.getDrop(BlockType.BEDROCK, fixedRandom(0.0f)));
        assertNull(BlockDropResolver.getDrop(BlockType.GLASS, fixedRandom(0.0f)));
        assertNull(BlockDropResolver.getDrop(BlockType.GLASS_PANE, fixedRandom(0.0f)));
        assertNull(BlockDropResolver.getDrop(BlockType.ICE, fixedRandom(0.0f)));
        assertNull(BlockDropResolver.getDrop(BlockType.MOB_SPAWNER, fixedRandom(0.0f)));
        assertNull(BlockDropResolver.getDrop(BlockType.INFESTED_STONE, fixedRandom(0.0f)));
        assertNull(BlockDropResolver.getDrop(BlockType.END_PORTAL_FRAME, fixedRandom(0.0f)));
    }

    @Test
    @DisplayName("Redstone and lapis ores should use Release-style multi-drop counts")
    void oreDropsUseReleaseStyleCounts() {
        ItemStack redstoneMin = BlockDropResolver.getDrop(BlockType.REDSTONE_ORE, fixedIntRandom(0));
        ItemStack redstoneMax = BlockDropResolver.getDrop(BlockType.REDSTONE_ORE, fixedIntRandom(1));
        ItemStack glowingRedstoneMax = BlockDropResolver.getDrop(BlockType.GLOWING_REDSTONE_ORE, fixedIntRandom(1));
        ItemStack lapisMin = BlockDropResolver.getDrop(BlockType.LAPIS_ORE, fixedIntRandom(0));
        ItemStack lapisMax = BlockDropResolver.getDrop(BlockType.LAPIS_ORE, fixedIntRandom(4));

        assertSame(ItemType.REDSTONE, redstoneMin.getType());
        assertEquals(4, redstoneMin.getCount());
        assertEquals(5, redstoneMax.getCount());
        assertSame(ItemType.REDSTONE, glowingRedstoneMax.getType());
        assertEquals(5, glowingRedstoneMax.getCount());
        assertSame(ItemType.LAPIS_LAZULI, lapisMin.getType());
        assertEquals(4, lapisMin.getCount());
        assertEquals(8, lapisMax.getCount());
    }

    @Test
    @DisplayName("Fortune should apply Release 1.0 block drop bonuses")
    void fortuneAppliesReleaseOneDropBonuses() {
        ItemStack fortune = enchantedTool(EnchantmentType.FORTUNE, 3);

        ItemStack flint = BlockDropResolver.getDropWithToolStack(BlockType.GRAVEL, fixedIntRandom(0), fortune);
        ItemStack diamond = BlockDropResolver.getDropWithToolStack(BlockType.DIAMOND_ORE,
                sequenceIntRandom(4), fortune);
        ItemStack lapis = BlockDropResolver.getDropWithToolStack(BlockType.LAPIS_ORE,
                sequenceIntRandom(0, 4), fortune);
        ItemStack redstone = BlockDropResolver.getDropWithToolStack(BlockType.REDSTONE_ORE,
                sequenceIntRandom(1, 3), fortune);
        ItemStack glowstone = BlockDropResolver.getDropWithToolStack(BlockType.GLOWSTONE,
                sequenceIntRandom(2, 3), fortune);
        ItemStack melon = BlockDropResolver.getDropWithToolStack(BlockType.MELON,
                sequenceIntRandom(4, 3), fortune);

        assertSame(ItemType.FLINT, flint.getType());
        assertEquals(4, diamond.getCount());
        assertEquals(16, lapis.getCount());
        assertEquals(8, redstone.getCount());
        assertEquals(4, glowstone.getCount());
        assertEquals(9, melon.getCount());
    }

    @Test
    @DisplayName("Fortune should add Release 1.0 crop and stem seed rolls")
    void fortuneAddsCropSeedRolls() {
        ItemStack fortune = enchantedTool(EnchantmentType.FORTUNE, 2);
        List<ItemStack> wheatDrops = BlockDropResolver.getDropsWithToolStack(BlockType.CROPS, 7,
                fixedIntRandom(0), fortune);
        List<ItemStack> stemDrops = BlockDropResolver.getDropsWithToolStack(BlockType.MELON_STEM, 7,
                fixedIntRandom(0), fortune);

        assertSame(ItemType.WHEAT, wheatDrops.get(0).getType());
        assertEquals(5, wheatDrops.get(1).getCount());
        assertSame(ItemType.MELON_SEEDS, stemDrops.get(0).getType());
        assertEquals(5, stemDrops.get(0).getCount());
    }

    @Test
    @DisplayName("Fortune should add Release 1.0 tall grass seed attempts")
    void fortuneAddsTallGrassSeedAttempts() {
        ItemStack fortune = enchantedTool(EnchantmentType.FORTUNE, 2);

        ItemStack seeds = BlockDropResolver.getDropWithToolStack(BlockType.TALL_GRASS,
                expectedBoundsRandom(new int[] { 5, 8, 8, 8, 8, 8 }, 4, 0, 1, 0, 1, 0), fortune);

        assertSame(ItemType.SEEDS, seeds.getType());
        assertEquals(3, seeds.getCount());
    }

    @Test
    @DisplayName("Fortune should improve Release 1.0 leaf sapling chance")
    void fortuneImprovesLeafSaplingChance() {
        ItemStack fortuneOne = enchantedTool(EnchantmentType.FORTUNE, 1);
        ItemStack fortuneTwo = enchantedTool(EnchantmentType.FORTUNE, 2);
        ItemStack fortuneThree = enchantedTool(EnchantmentType.FORTUNE, 3);

        assertSame(ItemType.SAPLING,
                BlockDropResolver.getDrop(BlockType.LEAVES,
                        expectedBoundsRandom(new int[] { 20 }, 0)).getType());
        assertSame(ItemType.SAPLING, BlockDropResolver.getDropWithToolStack(BlockType.LEAVES,
                expectedBoundsRandom(new int[] { 16 }, 0), fortuneOne).getType());
        assertSame(ItemType.SAPLING, BlockDropResolver.getDropWithToolStack(BlockType.LEAVES,
                expectedBoundsRandom(new int[] { 12 }, 0), fortuneTwo).getType());
        assertSame(ItemType.SAPLING, BlockDropResolver.getDropWithToolStack(BlockType.LEAVES,
                expectedBoundsRandom(new int[] { 10 }, 0), fortuneThree).getType());
        assertNull(BlockDropResolver.getDropWithToolStack(BlockType.LEAVES,
                expectedBoundsRandom(new int[] { 10 }, 1), fortuneThree));
    }

    @Test
    @DisplayName("Oak leaves should not use the post-1.0 apple roll")
    void oakLeavesDoNotRunPostReleaseAppleRoll() {
        List<ItemStack> appleMiss = BlockDropResolver.getDrops(BlockType.LEAVES, 0,
                expectedBoundsRandom(new int[] { 20 }, 1));
        List<ItemStack> saplingOnly = BlockDropResolver.getDrops(BlockType.LEAVES, 0,
                expectedBoundsRandom(new int[] { 20 }, 0));
        List<ItemStack> spruceMiss = BlockDropResolver.getDrops(BlockType.LEAVES, 1,
                expectedBoundsRandom(new int[] { 20 }, 1));

        assertTrue(appleMiss.isEmpty());
        assertEquals(1, saplingOnly.size());
        assertSame(ItemType.SAPLING, saplingOnly.get(0).getType());
        assertTrue(spruceMiss.isEmpty());
    }

    @Test
    @DisplayName("Fortune should not add the post-1.0 oak apple roll")
    void fortuneDoesNotAddPostReleaseOakAppleRoll() {
        ItemStack fortuneThree = enchantedTool(EnchantmentType.FORTUNE, 3);

        List<ItemStack> drops = BlockDropResolver.getDropsWithToolStack(BlockType.LEAVES, 0,
                expectedBoundsRandom(new int[] { 10 }, 1), fortuneThree);

        assertTrue(drops.isEmpty());
    }

    @Test
    @DisplayName("Silk Touch should return only Release 1.0 collectable blocks before Fortune-style drops")
    void silkTouchReturnsOnlyReleaseOneCollectableBlocksBeforeFortuneDrops() {
        ItemStack silk = enchantedTool(EnchantmentType.SILK_TOUCH, 1);
        silk.addEnchantment(new EnchantmentInstance(EnchantmentType.FORTUNE, 3));

        assertSame(ItemType.STONE, BlockDropResolver.getDropWithToolStack(BlockType.STONE,
                fixedIntRandom(0), silk).getType());
        assertSame(ItemType.GLASS, BlockDropResolver.getDropWithToolStack(BlockType.GLASS,
                fixedIntRandom(0), silk).getType());
        assertSame(ItemType.BOOKSHELF, BlockDropResolver.getDropWithToolStack(BlockType.BOOKSHELF,
                fixedIntRandom(0), silk).getType());
        assertSame(ItemType.COAL_ORE, BlockDropResolver.getDropWithToolStack(BlockType.COAL_ORE,
                fixedIntRandom(0), silk).getType());
        assertSame(ItemType.REDSTONE_ORE, BlockDropResolver.getDropWithToolStack(BlockType.GLOWING_REDSTONE_ORE,
                fixedIntRandom(0), silk).getType());
        assertNull(BlockDropResolver.getDropWithToolStack(BlockType.ICE, fixedIntRandom(0), silk));
        assertNull(BlockDropResolver.getDropWithToolStack(BlockType.GLASS_PANE, fixedIntRandom(0), silk));
        assertNull(BlockDropResolver.getDropWithToolStack(BlockType.MOB_SPAWNER, fixedIntRandom(0), silk));
        assertNull(BlockDropResolver.getDropWithToolStack(BlockType.INFESTED_STONE, fixedIntRandom(0), silk));
        ItemStack doubleSlab = BlockDropResolver.getDropWithToolStack(BlockType.DOUBLE_STONE_SLAB,
                fixedIntRandom(0), silk);
        assertSame(ItemType.STONE_SLAB, doubleSlab.getType());
        assertEquals(2, doubleSlab.getCount());
    }

    @Test
    @DisplayName("World block breaking should pass enchanted tool stacks into drop resolution")
    void worldBreakBlockPassesEnchantedToolStack() {
        World world = new World(9360L);
        try {
            world.setBlock(0, 100, 0, BlockType.GLASS, 0);

            assertTrue(world.breakBlockWithToolStack(0, 100, 0, true,
                    enchantedTool(EnchantmentType.SILK_TOUCH, 1)));

            assertEquals(1, world.getDroppedItems().stream()
                    .filter(item -> item.getItemType() == ItemType.GLASS)
                    .mapToInt(item -> item.getCount())
                    .sum());
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("World Silk Touch mining should keep post-1.0 ice and pane collection gated out")
    void worldSilkTouchMiningKeepsPostReleaseIceAndPaneCollectionGatedOut() {
        World world = new World(9361L);
        try {
            ItemStack silk = enchantedTool(EnchantmentType.SILK_TOUCH, 1);
            world.setBlock(0, 100, 0, BlockType.ICE, 0);
            world.setBlock(1, 100, 0, BlockType.GLASS_PANE, 0);

            assertTrue(world.breakBlockWithToolStack(0, 100, 0, true, silk));
            assertTrue(world.breakBlockWithToolStack(1, 100, 0, true, silk));

            assertSame(BlockType.AIR, world.getBlock(0, 100, 0));
            assertSame(BlockType.AIR, world.getBlock(1, 100, 0));
            assertTrue(world.getDroppedItems().stream()
                    .noneMatch(item -> item.getItemType() == ItemType.ICE
                            || item.getItemType() == ItemType.GLASS_PANE));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Component blocks should drop their Release 1.0 item outputs")
    void componentBlocksDropReleaseStyleItems() {
        ItemStack clay = BlockDropResolver.getDrop(BlockType.CLAY, fixedIntRandom(0));
        ItemStack snowLayer = BlockDropResolver.getDrop(BlockType.SNOW_LAYER, fixedIntRandom(0));
        ItemStack snow = BlockDropResolver.getDrop(BlockType.SNOW, fixedIntRandom(0));
        ItemStack books = BlockDropResolver.getDrop(BlockType.BOOKSHELF, fixedIntRandom(0));
        ItemStack spruceLog = BlockDropResolver.getDrops(BlockType.OAK_LOG, 1, fixedIntRandom(0)).get(0);
        ItemStack doubleStoneBrickSlab = BlockDropResolver.getDrops(BlockType.DOUBLE_STONE_SLAB, 5,
                fixedIntRandom(0)).get(0);
        ItemStack mossyStoneBrick = BlockDropResolver.getDrops(BlockType.STONE_BRICK, 1, fixedIntRandom(0)).get(0);
        ItemStack crackedStoneBrick = BlockDropResolver.getDrops(BlockType.STONE_BRICK, 2, fixedIntRandom(0)).get(0);
        ItemStack chiseledStoneBrick = BlockDropResolver.getDrops(BlockType.STONE_BRICK, 3, fixedIntRandom(0)).get(0);
        ItemStack glowstoneMin = BlockDropResolver.getDrop(BlockType.GLOWSTONE, fixedIntRandom(0));
        ItemStack glowstoneMax = BlockDropResolver.getDrop(BlockType.GLOWSTONE, fixedIntRandom(2));
        ItemStack melonMin = BlockDropResolver.getDrop(BlockType.MELON, fixedIntRandom(0));
        ItemStack melonMax = BlockDropResolver.getDrop(BlockType.MELON, fixedIntRandom(4));

        assertSame(ItemType.CLAY_BALL, clay.getType());
        assertEquals(4, clay.getCount());
        assertSame(ItemType.SNOWBALL, snowLayer.getType());
        assertEquals(1, snowLayer.getCount());
        assertSame(ItemType.SNOWBALL, snow.getType());
        assertEquals(4, snow.getCount());
        assertSame(ItemType.BOOK, books.getType());
        assertEquals(3, books.getCount());
        assertSame(ItemType.SPRUCE_LOG, spruceLog.getType());
        assertEquals(1, spruceLog.getCount());
        assertSame(ItemType.STONE_BRICK_SLAB, doubleStoneBrickSlab.getType());
        assertEquals(2, doubleStoneBrickSlab.getCount());
        assertSame(ItemType.MOSSY_STONE_BRICK, mossyStoneBrick.getType());
        assertSame(ItemType.CRACKED_STONE_BRICK, crackedStoneBrick.getType());
        assertSame(ItemType.CHISELED_STONE_BRICK, chiseledStoneBrick.getType());
        assertSame(ItemType.GLOWSTONE_DUST, glowstoneMin.getType());
        assertEquals(2, glowstoneMin.getCount());
        assertEquals(4, glowstoneMax.getCount());
        assertSame(ItemType.MELON_SLICE, melonMin.getType());
        assertEquals(3, melonMin.getCount());
        assertEquals(7, melonMax.getCount());
    }

    @Test
    @DisplayName("Huge mushroom cap blocks should drop sparse matching mushrooms")
    void hugeMushroomCapsDropMatchingMushroomsByReleaseRoll() {
        assertTrue(BlockDropResolver.getDrops(BlockType.BROWN_MUSHROOM_BLOCK, 0, fixedIntRandom(7)).isEmpty());

        ItemStack brownOne = BlockDropResolver.getDrop(BlockType.BROWN_MUSHROOM_BLOCK, fixedIntRandom(8));
        ItemStack redTwo = BlockDropResolver.getDrop(BlockType.RED_MUSHROOM_BLOCK, fixedIntRandom(9));

        assertSame(ItemType.BROWN_MUSHROOM, brownOne.getType());
        assertEquals(1, brownOne.getCount());
        assertSame(ItemType.RED_MUSHROOM, redTwo.getType());
        assertEquals(2, redTwo.getCount());
    }

    @Test
    @DisplayName("Leaves and tall grass should use Release 1.0 chance-based drops")
    void chanceBasedPlantDropsUseReleaseStyleRates() {
        ItemStack leafHit = BlockDropResolver.getDrop(BlockType.LEAVES, fixedIntRandom(0));
        ItemStack leafMiss = BlockDropResolver.getDrop(BlockType.LEAVES, fixedIntRandom(1));
        ItemStack grassHit = BlockDropResolver.getDrop(BlockType.TALL_GRASS, fixedIntRandom(0));
        ItemStack grassMiss = BlockDropResolver.getDrop(BlockType.TALL_GRASS, fixedIntRandom(1));

        assertSame(ItemType.SAPLING, leafHit.getType());
        assertEquals(1, leafHit.getCount());
        assertNull(leafMiss);
        assertSame(ItemType.SEEDS, grassHit.getType());
        assertEquals(1, grassHit.getCount());
        assertNull(grassMiss);
        assertNull(BlockDropResolver.getDrop(BlockType.DEAD_BUSH, fixedIntRandom(0)));

        ItemStack spruceLeafSapling = BlockDropResolver.getDrops(BlockType.LEAVES, 1, fixedIntRandom(0)).get(0);
        ItemStack birchLeafSapling = BlockDropResolver.getDrops(BlockType.LEAVES, 2, fixedIntRandom(0)).get(0);
        assertSame(ItemType.SPRUCE_SAPLING, spruceLeafSapling.getType());
        assertSame(ItemType.BIRCH_SAPLING, birchLeafSapling.getType());
    }

    @Test
    @DisplayName("Shears should collect Release 1.0 foliage blocks")
    void shearsCollectReleaseOneFoliageBlocks() {
        ItemStack leaves = BlockDropResolver.getDrop(BlockType.LEAVES, fixedIntRandom(0), ItemType.SHEARS);
        ItemStack shrub = BlockDropResolver.getDrop(BlockType.TALL_GRASS, fixedIntRandom(0), ItemType.SHEARS);
        ItemStack vines = BlockDropResolver.getDrop(BlockType.VINES, fixedIntRandom(0), ItemType.SHEARS);
        ItemStack spruceLeaves = BlockDropResolver.getDrops(BlockType.LEAVES, 1 | World.LEAF_PERSISTENT_BIT,
                fixedIntRandom(0), ItemType.SHEARS).get(0);
        ItemStack birchLeaves = BlockDropResolver.getDrops(BlockType.LEAVES, 2 | World.LEAF_PERSISTENT_BIT,
                fixedIntRandom(0), ItemType.SHEARS).get(0);
        ItemStack tallGrass = BlockDropResolver.getDrops(BlockType.TALL_GRASS, 1,
                fixedIntRandom(0), ItemType.SHEARS).get(0);
        ItemStack fern = BlockDropResolver.getDrops(BlockType.TALL_GRASS, 2,
                fixedIntRandom(0), ItemType.SHEARS).get(0);

        assertSame(ItemType.LEAVES, leaves.getType());
        assertEquals(1, leaves.getCount());
        assertSame(ItemType.SPRUCE_LEAVES, spruceLeaves.getType());
        assertEquals(1, spruceLeaves.getCount());
        assertSame(ItemType.BIRCH_LEAVES, birchLeaves.getType());
        assertEquals(1, birchLeaves.getCount());
        assertSame(ItemType.SHRUB, shrub.getType());
        assertEquals(1, shrub.getCount());
        assertSame(ItemType.TALL_GRASS, tallGrass.getType());
        assertEquals(1, tallGrass.getCount());
        assertSame(ItemType.FERN, fern.getType());
        assertEquals(1, fern.getCount());
        assertSame(ItemType.VINES, vines.getType());
        assertEquals(1, vines.getCount());
        assertNull(BlockDropResolver.getDrop(BlockType.VINES, fixedIntRandom(0)));
        assertNull(BlockDropResolver.getDrop(BlockType.DEAD_BUSH, fixedIntRandom(0)));
        assertNull(BlockDropResolver.getDrop(BlockType.DEAD_BUSH, fixedIntRandom(0), ItemType.SHEARS));
    }

    @Test
    @DisplayName("Cobwebs should drop string only when cut with swords or shears")
    void cobwebDropsRequireSwordOrShears() {
        assertNull(BlockDropResolver.getDrop(BlockType.COBWEB, fixedIntRandom(0)));
        assertTrue(BlockDropResolver.getDrops(BlockType.COBWEB, 0, fixedIntRandom(0), ItemType.IRON_PICKAXE).isEmpty());

        ItemStack swordDrop = BlockDropResolver.getDrop(BlockType.COBWEB, fixedIntRandom(0), ItemType.WOODEN_SWORD);
        ItemStack shearsDrop = BlockDropResolver.getDrop(BlockType.COBWEB, fixedIntRandom(0), ItemType.SHEARS);

        assertSame(ItemType.STRING, swordDrop.getType());
        assertEquals(1, swordDrop.getCount());
        assertSame(ItemType.STRING, shearsDrop.getType());
        assertEquals(1, shearsDrop.getCount());
    }

    private static Random fixedRandom(float value) {
        return new Random(0L) {
            @Override
            public float nextFloat() {
                return value;
            }
        };
    }

    private static Random fixedIntRandom(int value) {
        return new Random(0L) {
            @Override
            public int nextInt(int bound) {
                return Math.min(value, bound - 1);
            }
        };
    }

    private static Random expectedBoundsRandom(int[] expectedBounds, int... values) {
        return new Random(0L) {
            private int index;

            @Override
            public int nextInt(int bound) {
                assertTrue(index < expectedBounds.length,
                        "Unexpected nextInt(" + bound + ") after " + expectedBounds.length + " expected calls");
                assertEquals(expectedBounds[index], bound);
                int value = values[Math.min(index, values.length - 1)];
                index++;
                return Math.max(0, Math.min(value, bound - 1));
            }
        };
    }

    private static Random sequenceIntRandom(int... values) {
        return new Random(0L) {
            private int index;

            @Override
            public int nextInt(int bound) {
                int value = values[Math.min(index, values.length - 1)];
                index++;
                return Math.max(0, Math.min(value, bound - 1));
            }
        };
    }

    private static ItemStack enchantedTool(EnchantmentType type, int level) {
        ItemStack tool = new ItemStack(ItemType.DIAMOND_PICKAXE, 1);
        tool.addEnchantment(new EnchantmentInstance(type, level));
        return tool;
    }
}
