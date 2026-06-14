package com.craftzero.world;

import com.craftzero.inventory.ItemStack;
import com.craftzero.inventory.ItemType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class BlockDropResolverTest {
    @Test
    @DisplayName("Gravel should sometimes drop flint and otherwise drop gravel")
    void gravelDropsFlintByChance() {
        ItemStack flint = BlockDropResolver.getDrop(BlockType.GRAVEL, fixedRandom(0.05f));
        ItemStack gravel = BlockDropResolver.getDrop(BlockType.GRAVEL, fixedRandom(0.50f));

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
        assertNull(BlockDropResolver.getDrop(BlockType.LEAVES, fixedRandom(0.0f)));
    }

    @Test
    @DisplayName("Glass and ice should not drop themselves")
    void glassAndIceDoNotDrop() {
        assertNull(BlockDropResolver.getDrop(BlockType.GLASS, fixedRandom(0.0f)));
        assertNull(BlockDropResolver.getDrop(BlockType.ICE, fixedRandom(0.0f)));
    }

    @Test
    @DisplayName("Redstone and lapis ores should use Release-style multi-drop counts")
    void oreDropsUseReleaseStyleCounts() {
        ItemStack redstoneMin = BlockDropResolver.getDrop(BlockType.REDSTONE_ORE, fixedIntRandom(0));
        ItemStack redstoneMax = BlockDropResolver.getDrop(BlockType.REDSTONE_ORE, fixedIntRandom(1));
        ItemStack lapisMin = BlockDropResolver.getDrop(BlockType.LAPIS_ORE, fixedIntRandom(0));
        ItemStack lapisMax = BlockDropResolver.getDrop(BlockType.LAPIS_ORE, fixedIntRandom(4));

        assertSame(ItemType.REDSTONE, redstoneMin.getType());
        assertEquals(4, redstoneMin.getCount());
        assertEquals(5, redstoneMax.getCount());
        assertSame(ItemType.LAPIS_LAZULI, lapisMin.getType());
        assertEquals(4, lapisMin.getCount());
        assertEquals(8, lapisMax.getCount());
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
}
