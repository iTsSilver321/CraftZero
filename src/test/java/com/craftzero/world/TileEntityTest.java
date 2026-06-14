package com.craftzero.world;

import com.craftzero.inventory.ItemStack;
import com.craftzero.inventory.ItemType;
import com.craftzero.entity.mob.MobDefinition;
import com.craftzero.world.tile.ChestTileEntity;
import com.craftzero.world.tile.MonsterSpawnerTileEntity;
import com.craftzero.world.tile.SignTileEntity;
import com.craftzero.world.tile.TileEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TileEntityTest {

    @Test
    @DisplayName("World should create and remove tile entities with their blocks")
    void createsAndRemovesTileEntities() {
        World world = new World(1L);
        try {
            world.setBlock(1, 70, 1, BlockType.CHEST, 3);

            TileEntity tile = world.getTileEntity(1, 70, 1);
            assertInstanceOf(ChestTileEntity.class, tile);
            assertEquals(3, world.getBlockMetadata(1, 70, 1));

            world.setBlock(1, 70, 1, BlockType.AIR);
            assertNull(world.getTileEntity(1, 70, 1));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Chest placement should allow doubles but reject triples")
    void chestPlacementRulesRejectTriples() {
        World world = new World(2L);
        try {
            assertTrue(world.canPlaceChestAt(0, 70, 0));
            world.setBlock(0, 70, 0, BlockType.CHEST);

            assertTrue(world.canPlaceChestAt(1, 70, 0));
            world.setBlock(1, 70, 0, BlockType.CHEST);

            assertFalse(world.canPlaceChestAt(2, 70, 0));
            assertFalse(world.canPlaceChestAt(0, 70, 1));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Breaking a chest should drop the chest and its contents")
    void breakingChestDropsContents() {
        World world = new World(3L);
        try {
            world.setBlock(0, 70, 0, BlockType.CHEST);
            ChestTileEntity chest = (ChestTileEntity) world.getTileEntity(0, 70, 0);
            chest.getInventory()[0] = new ItemStack(ItemType.DIAMOND, 3);

            assertTrue(world.breakBlock(0, 70, 0, true));

            assertSame(BlockType.AIR, world.getBlock(0, 70, 0));
            assertNull(world.getTileEntity(0, 70, 0));
            assertTrue(world.getDroppedItems().stream().anyMatch(item -> item.getItemType() == ItemType.CHEST));
            assertTrue(world.getDroppedItems().stream().anyMatch(item -> item.getItemType() == ItemType.DIAMOND
                    && item.getCount() == 3));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Signs should create tile entities and preserve text in memory")
    void signsCreateTileEntities() {
        World world = new World(4L);
        try {
            world.setBlock(0, 70, 0, BlockType.STANDING_SIGN, 0);
            assertInstanceOf(SignTileEntity.class, world.getTileEntity(0, 70, 0));
            SignTileEntity sign = (SignTileEntity) world.getTileEntity(0, 70, 0);
            sign.setLine(0, "CraftZero");
            assertEquals("CraftZero", sign.getLines()[0]);
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Mob spawner block should create and remove a monster spawner tile entity")
    void mobSpawnerBlockCreatesMonsterSpawnerTile() {
        World world = new World(44L);
        try {
            world.setBlock(0, 70, 0, BlockType.MOB_SPAWNER);

            TileEntity tile = world.getTileEntity(0, 70, 0);
            assertInstanceOf(MonsterSpawnerTileEntity.class, tile);
            MonsterSpawnerTileEntity spawner = (MonsterSpawnerTileEntity) tile;
            assertSame(MobDefinition.ZOMBIE, spawner.getMobDefinition());

            world.setBlock(0, 70, 0, BlockType.AIR);
            assertNull(world.getTileEntity(0, 70, 0));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Attachable blocks should break when their support is removed")
    void supportRemovalBreaksAttachables() {
        World world = new World(5L);
        try {
            world.setBlock(0, 70, 0, BlockType.STONE);
            world.setBlock(0, 71, 0, BlockType.TORCH, 5);
            assertSame(BlockType.TORCH, world.getBlock(0, 71, 0));

            world.breakBlock(0, 70, 0, false);

            assertSame(BlockType.AIR, world.getBlock(0, 71, 0));
            assertTrue(world.getDroppedItems().stream().anyMatch(item -> item.getItemType() == ItemType.TORCH));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Door and bed helpers should place and break paired blocks")
    void pairedBlocksPlaceAndBreakTogether() {
        World world = new World(6L);
        try {
            world.setBlock(0, 69, 0, BlockType.STONE);
            assertTrue(world.placeDoor(0, 70, 0, BlockType.WOODEN_DOOR, 0, null));
            assertSame(BlockType.WOODEN_DOOR, world.getBlock(0, 70, 0));
            assertSame(BlockType.WOODEN_DOOR, world.getBlock(0, 71, 0));

            world.breakBlock(0, 71, 0, true);
            assertSame(BlockType.AIR, world.getBlock(0, 70, 0));
            assertSame(BlockType.AIR, world.getBlock(0, 71, 0));
            assertTrue(world.getDroppedItems().stream().anyMatch(item -> item.getItemType() == ItemType.WOODEN_DOOR));

            world.setBlock(1, 69, 0, BlockType.STONE);
            world.setBlock(1, 69, -1, BlockType.STONE);
            assertNotNull(world.placeBed(1, 70, 0, 0, null));
            assertSame(BlockType.BED, world.getBlock(1, 70, 0));
            assertSame(BlockType.BED, world.getBlock(1, 70, -1));

            world.breakBlock(1, 70, -1, true);
            assertSame(BlockType.AIR, world.getBlock(1, 70, 0));
            assertSame(BlockType.AIR, world.getBlock(1, 70, -1));
            assertTrue(world.getDroppedItems().stream().anyMatch(item -> item.getItemType() == ItemType.BED));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Block shapes should expose non-cube collision and selection")
    void nonCubeShapesExposeExpectedBoxes() {
        World world = new World(7L);
        try {
            world.setBlock(0, 70, 0, BlockType.STONE_SLAB);
            assertEquals(1, world.getCollisionBoxes(0, 70, 0).size());
            assertEquals(70.5f, world.getCollisionBoxes(0, 70, 0).get(0).getMax().y, 0.001f);

            world.setBlock(1, 70, 0, BlockType.LADDER, Block.FACE_NORTH);
            assertTrue(world.getCollisionBoxes(1, 70, 0).isEmpty());
            assertFalse(world.getSelectionBoxes(1, 70, 0).isEmpty());
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Slabs should merge into double slabs and drop two slab items")
    void slabsMergeAndDropTwoItems() {
        World world = new World(8L);
        try {
            world.setBlock(0, 70, 0, BlockType.STONE_SLAB);
            assertTrue(world.tryMergeSlab(0, 70, 0));
            assertSame(BlockType.DOUBLE_STONE_SLAB, world.getBlock(0, 70, 0));

            world.breakBlock(0, 70, 0, true);
            assertTrue(world.getDroppedItems().stream()
                    .anyMatch(item -> item.getItemType() == ItemType.STONE_SLAB && item.getCount() == 2));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Torch and lit furnace should emit block light")
    void lightEmittersProduceBlockLight() {
        World world = new World(9L);
        try {
            world.setBlock(0, 70, 0, BlockType.TORCH, 5);
            assertTrue(world.getBlockLight(0, 70, 0) >= 14);

            world.setBlock(1, 70, 0, BlockType.LIT_FURNACE, 2);
            assertTrue(world.getBlockLight(1, 70, 0) >= 13);
        } finally {
            world.cleanup();
        }
    }
}
