package com.craftzero.world;

import com.craftzero.entity.ArrowEntity;
import com.craftzero.entity.MinecartEntity;
import com.craftzero.entity.PrimedTntEntity;
import com.craftzero.inventory.ItemStack;
import com.craftzero.inventory.ItemType;
import com.craftzero.world.tile.DispenserTileEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MechanismSprintTest {
    @Test
    @DisplayName("Redstone wire carries power into TNT without generating extra chunks")
    void redstoneWirePowersTnt() {
        World world = new World(5100L);
        try {
            world.setBlock(0, 99, 0, BlockType.STONE);
            world.setBlock(1, 99, 0, BlockType.STONE);
            world.setBlock(0, 100, 0, BlockType.REDSTONE_TORCH_ON, 5);
            world.setBlock(1, 100, 0, BlockType.REDSTONE_WIRE, 0);
            world.setBlock(2, 100, 0, BlockType.TNT, 0);

            world.advanceBlockTicks(8);
            world.updateEntities(1.0f / 20.0f);

            assertTrue(world.getBlockMetadataIfLoaded(1, 100, 0, 0) > 0);
            assertSame(BlockType.AIR, world.getBlockIfLoaded(2, 100, 0, BlockType.BEDROCK));
            assertTrue(world.getEntities().stream().anyMatch(entity -> entity instanceof PrimedTntEntity));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Powered piston pushes one movable block and creates a piston head")
    void pistonPushesBlock() {
        World world = new World(5101L);
        try {
            world.setBlock(0, 100, 0, BlockType.PISTON, Block.FACE_EAST);
            for (int x = 1; x <= 13; x++) {
                world.setBlock(x, 100, 0, BlockType.AIR, 0);
            }
            world.setBlock(1, 100, 0, BlockType.STONE, 0);
            world.setBlock(-1, 99, 0, BlockType.STONE, 0);
            world.setBlock(-1, 100, 0, BlockType.LEVER, 5 | RedstoneEngine.POWERED_BIT);

            world.advanceBlockTicks(4);

            assertSame(BlockType.PISTON, world.getBlock(0, 100, 0));
            assertTrue((world.getBlockMetadata(0, 100, 0) & RedstoneEngine.PISTON_EXTENDED_BIT) != 0);
            assertSame(BlockType.PISTON_HEAD, world.getBlock(1, 100, 0));
            assertSame(BlockType.STONE, world.getBlock(2, 100, 0));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Minecart placement on detector rail powers the detector")
    void detectorRailSeesMinecart() {
        World world = new World(5102L);
        try {
            world.setBlock(0, 99, 0, BlockType.STONE);
            world.setBlock(0, 100, 0, BlockType.DETECTOR_RAIL, 0);

            assertTrue(world.placeMinecartOnRail(0, 100, 0, ItemType.MINECART));
            world.updateEntities(1.0f / 20.0f);
            world.advanceBlockTicks(12);

            assertTrue(world.getEntities().stream().anyMatch(entity -> entity instanceof MinecartEntity));
            assertTrue((world.getBlockMetadata(0, 100, 0) & RedstoneEngine.RAIL_POWERED_BIT) != 0);
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Dispenser consumes one slot item and fires arrows when powered")
    void dispenserFiresArrow() {
        World world = new World(5103L);
        try {
            world.setBlock(0, 100, 0, BlockType.DISPENSER, Block.FACE_EAST);
            DispenserTileEntity dispenser = (DispenserTileEntity) world.getTileEntity(0, 100, 0);
            dispenser.getInventory()[0] = new ItemStack(ItemType.ARROW, 2);
            world.setBlock(-1, 99, 0, BlockType.STONE, 0);
            world.setBlock(-1, 100, 0, BlockType.LEVER, 5 | RedstoneEngine.POWERED_BIT);

            world.advanceBlockTicks(4);
            world.updateEntities(1.0f / 20.0f);

            assertEquals(1, dispenser.getInventory()[0].getCount());
            assertTrue(world.getEntities().stream().anyMatch(entity -> entity instanceof ArrowEntity));
        } finally {
            world.cleanup();
        }
    }
}
