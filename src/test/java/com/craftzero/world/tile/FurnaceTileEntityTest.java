package com.craftzero.world.tile;

import com.craftzero.inventory.ItemStack;
import com.craftzero.inventory.ItemType;
import com.craftzero.world.BlockType;
import com.craftzero.world.World;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FurnaceTileEntityTest {

    @Test
    @DisplayName("Furnace should consume fuel, smelt input, and toggle lit block")
    void furnaceSmeltsAndTogglesLitBlock() {
        World world = new World(4L);
        try {
            world.setBlock(0, 70, 0, BlockType.FURNACE, 3);
            FurnaceTileEntity furnace = (FurnaceTileEntity) world.getTileEntity(0, 70, 0);
            furnace.getInventory()[FurnaceTileEntity.SLOT_INPUT] = new ItemStack(ItemType.IRON_ORE, 1);
            furnace.getInventory()[FurnaceTileEntity.SLOT_FUEL] = new ItemStack(ItemType.COAL, 1);

            furnace.tick(world, 10.0f);

            assertSame(BlockType.LIT_FURNACE, world.getBlock(0, 70, 0));
            assertEquals(3, world.getBlockMetadata(0, 70, 0));
            assertNull(furnace.getInventory()[FurnaceTileEntity.SLOT_INPUT]);
            assertNull(furnace.getInventory()[FurnaceTileEntity.SLOT_FUEL]);
            assertSame(ItemType.IRON_INGOT, furnace.getInventory()[FurnaceTileEntity.SLOT_OUTPUT].getType());
            assertEquals(1, furnace.getInventory()[FurnaceTileEntity.SLOT_OUTPUT].getCount());
            assertTrue(furnace.getBurnTime() > 0);
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Furnace should not smelt when output is blocked")
    void furnaceDoesNotSmeltWithBlockedOutput() {
        World world = new World(5L);
        try {
            world.setBlock(0, 70, 0, BlockType.FURNACE);
            FurnaceTileEntity furnace = (FurnaceTileEntity) world.getTileEntity(0, 70, 0);
            furnace.getInventory()[FurnaceTileEntity.SLOT_INPUT] = new ItemStack(ItemType.GOLD_ORE, 1);
            furnace.getInventory()[FurnaceTileEntity.SLOT_FUEL] = new ItemStack(ItemType.STICK, 1);
            furnace.getInventory()[FurnaceTileEntity.SLOT_OUTPUT] = new ItemStack(ItemType.DIRT, 1);

            furnace.tick(world, 10.0f);

            assertSame(ItemType.GOLD_ORE, furnace.getInventory()[FurnaceTileEntity.SLOT_INPUT].getType());
            assertSame(ItemType.STICK, furnace.getInventory()[FurnaceTileEntity.SLOT_FUEL].getType());
            assertSame(ItemType.DIRT, furnace.getInventory()[FurnaceTileEntity.SLOT_OUTPUT].getType());
            assertEquals(0, furnace.getBurnTime());
            assertEquals(0, furnace.getCookTime());
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Smelting registry should include Release 1.0 fuel and output variants")
    void smeltingRegistryContainsVariants() {
        assertSame(ItemType.CHARCOAL, SmeltingRegistry.getResult(new ItemStack(ItemType.OAK_LOG, 1)).getType());
        assertSame(ItemType.CACTUS_GREEN, SmeltingRegistry.getResult(new ItemStack(ItemType.CACTUS, 1)).getType());
        assertEquals(1600, FuelRegistry.getBurnTime(new ItemStack(ItemType.CHARCOAL, 1)));
        assertEquals(300, FuelRegistry.getBurnTime(new ItemStack(ItemType.OAK_PLANKS, 1)));
        assertEquals(100, FuelRegistry.getBurnTime(new ItemStack(ItemType.STICK, 1)));
    }
}
