package com.craftzero.main;

import com.craftzero.inventory.ItemStack;
import com.craftzero.inventory.ItemType;
import com.craftzero.physics.Raycast;
import com.craftzero.progression.PotionData;
import com.craftzero.world.Block;
import com.craftzero.world.BlockType;
import com.craftzero.world.Dimension;
import com.craftzero.world.World;
import com.craftzero.world.WorldGenerator;
import com.craftzero.world.WorldParticle;
import com.craftzero.world.WorldSoundEvent;
import org.joml.Vector3f;
import org.joml.Vector3i;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayerBucketInteractionTest {
    @Test
    @DisplayName("Empty buckets should use a source-fluid ray and replace the held bucket")
    void emptyBucketPicksUpSourceWaterFromViewRay() throws Exception {
        RecordingWorld world = new RecordingWorld(6282L);
        try {
            world.setBlock(0, 120, 2, BlockType.WATER, 0);
            Player player = new Player(0.0f, 120.0f, 0.0f);
            ItemStack bucket = new ItemStack(ItemType.BUCKET, 1);
            player.getInventory().getHotbar()[player.getInventory().getSelectedSlot()] = bucket;

            assertTrue(useImmediate(player, world, bucket));

            assertSame(BlockType.AIR, world.getBlock(0, 120, 2));
            assertSame(ItemType.WATER_BUCKET, player.getInventory().getItemInHand().getType());
            assertEquals(1, world.rebuildCount);
            assertEquals(0, world.lastRebuildX);
            assertEquals(120, world.lastRebuildY);
            assertEquals(2, world.lastRebuildZ);
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Glass bottles should fill from source water through the source-fluid ray")
    void glassBottleFillsFromSourceWaterFromViewRay() throws Exception {
        World world = new World(6283L);
        try {
            world.setBlock(0, 120, 2, BlockType.WATER, 0);
            Player player = new Player(0.0f, 120.0f, 0.0f);
            ItemStack bottle = new ItemStack(ItemType.GLASS_BOTTLE, 1);
            player.getInventory().getHotbar()[player.getInventory().getSelectedSlot()] = bottle;

            assertTrue(useImmediate(player, world, bottle));

            ItemStack held = player.getInventory().getItemInHand();
            assertSame(ItemType.POTION, held.getType());
            assertEquals(PotionData.water(), held.getPotionData());
            assertSame(BlockType.WATER, world.getBlock(0, 120, 2));
        } finally {
            world.cleanup();
        }
    }


    @Test
    @DisplayName("Glass bottles should not fill from non-source flowing water")
    void glassBottleRejectsFlowingWaterFromViewRay() throws Exception {
        World world = new World(6286L);
        try {
            world.setBlock(0, 120, 2, BlockType.FLOWING_WATER, 5);
            Player player = new Player(0.0f, 120.0f, 0.0f);
            ItemStack bottle = new ItemStack(ItemType.GLASS_BOTTLE, 1);
            player.getInventory().getHotbar()[player.getInventory().getSelectedSlot()] = bottle;

            assertFalse(useImmediate(player, world, bottle));

            assertSame(ItemType.GLASS_BOTTLE, player.getInventory().getItemInHand().getType());
            assertSame(BlockType.FLOWING_WATER, world.getBlock(0, 120, 2));
            assertEquals(5, world.getBlockMetadata(0, 120, 2));
        } finally {
            world.cleanup();
        }
    }


    @Test
    @DisplayName("Water bucket use should fizzle and leave an empty bucket in the Nether")
    void waterBucketUseEvaporatesInTheNether() throws Exception {
        World world = new World(6284L, WorldGenerator.RELEASE_ONE, Dimension.NETHER);
        try {
            world.setBlock(0, 120, 1, BlockType.AIR, 0);
            world.setBlock(0, 120, 2, BlockType.STONE, 0);
            Player player = new Player(0.0f, 120.0f, 0.0f);
            ItemStack bucket = new ItemStack(ItemType.WATER_BUCKET, 1);
            player.getInventory().getHotbar()[player.getInventory().getSelectedSlot()] = bucket;
            setTargetBlock(player, new Raycast.RaycastResult(true,
                    new Vector3i(0, 120, 2),
                    new Vector3i(0, 120, 1),
                    Block.FACE_NORTH,
                    1.0f));

            assertTrue(handleBucketUse(player, world, bucket));

            assertSame(BlockType.AIR, world.getBlock(0, 120, 1));
            assertSame(ItemType.BUCKET, player.getInventory().getItemInHand().getType());
            assertEquals(WorldSoundEvent.FIZZ, world.getSoundEvents().get(0).soundId());
            assertEquals(8, world.getParticles().stream()
                    .filter(particle -> particle.getType() == WorldParticle.Type.LARGE_SMOKE)
                    .count());
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Targeted empty bucket use should refresh the removed fluid source")
    void targetedEmptyBucketPickupRebuildsFluidSourceMesh() throws Exception {
        RecordingWorld world = new RecordingWorld(6289L);
        try {
            world.setBlock(0, 120, 2, BlockType.LAVA, 0);
            Player player = new Player(0.0f, 120.0f, 0.0f);
            ItemStack bucket = new ItemStack(ItemType.BUCKET, 1);
            player.getInventory().getHotbar()[player.getInventory().getSelectedSlot()] = bucket;
            setTargetBlock(player, new Raycast.RaycastResult(true,
                    new Vector3i(0, 120, 2),
                    new Vector3i(0, 120, 1),
                    Block.FACE_NORTH,
                    1.0f));

            assertTrue(handleBucketUse(player, world, bucket));

            assertSame(BlockType.AIR, world.getBlock(0, 120, 2));
            assertSame(ItemType.LAVA_BUCKET, player.getInventory().getItemInHand().getType());
            assertTrue(player.isUsingItem());
            assertEquals(1, world.rebuildCount);
            assertEquals(0, world.lastRebuildX);
            assertEquals(120, world.lastRebuildY);
            assertEquals(2, world.lastRebuildZ);
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Water bucket placement should refresh the placed fluid source")
    void waterBucketPlacementRebuildsPlacedFluidSourceMesh() throws Exception {
        RecordingWorld world = new RecordingWorld(6290L);
        try {
            world.setBlock(0, 120, 2, BlockType.STONE, 0);
            Player player = new Player(0.0f, 120.0f, 0.0f);
            ItemStack bucket = new ItemStack(ItemType.WATER_BUCKET, 1);
            player.getInventory().getHotbar()[player.getInventory().getSelectedSlot()] = bucket;
            setTargetBlock(player, new Raycast.RaycastResult(true,
                    new Vector3i(0, 120, 2),
                    new Vector3i(0, 120, 1),
                    Block.FACE_NORTH,
                    1.0f));

            assertTrue(handleBucketUse(player, world, bucket));

            assertSame(BlockType.WATER, world.getBlock(0, 120, 1));
            assertSame(ItemType.BUCKET, player.getInventory().getItemInHand().getType());
            assertTrue(player.isUsingItem());
            assertEquals(1, world.rebuildCount);
            assertEquals(0, world.lastRebuildX);
            assertEquals(120, world.lastRebuildY);
            assertEquals(1, world.lastRebuildZ);
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Water buckets should refresh cauldron level after filling")
    void waterBucketFillingCauldronRebuildsCauldronMesh() throws Exception {
        RecordingWorld world = new RecordingWorld(6287L);
        try {
            world.setBlock(0, 120, 2, BlockType.CAULDRON, 0);
            Player player = new Player(0.0f, 120.0f, 0.0f);
            ItemStack bucket = new ItemStack(ItemType.WATER_BUCKET, 1);
            player.getInventory().getHotbar()[player.getInventory().getSelectedSlot()] = bucket;
            setTargetBlock(player, new Raycast.RaycastResult(true,
                    new Vector3i(0, 120, 2),
                    new Vector3i(0, 120, 1),
                    Block.FACE_NORTH,
                    1.0f));

            assertTrue(handleTargetedItemUse(player, world, bucket, BlockType.CAULDRON));

            assertEquals(World.CAULDRON_MAX_LEVEL, world.getCauldronLevel(0, 120, 2));
            assertSame(ItemType.BUCKET, player.getInventory().getItemInHand().getType());
            assertTrue(player.isUsingItem());
            assertEquals(1, world.rebuildCount);
            assertEquals(0, world.lastRebuildX);
            assertEquals(120, world.lastRebuildY);
            assertEquals(2, world.lastRebuildZ);
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Glass bottles should refresh cauldron level after draining")
    void glassBottleDrainingCauldronRebuildsCauldronMesh() throws Exception {
        RecordingWorld world = new RecordingWorld(6288L);
        try {
            world.setBlock(0, 120, 2, BlockType.CAULDRON, 2);
            Player player = new Player(0.0f, 120.0f, 0.0f);
            ItemStack bottle = new ItemStack(ItemType.GLASS_BOTTLE, 1);
            player.getInventory().getHotbar()[player.getInventory().getSelectedSlot()] = bottle;
            setTargetBlock(player, new Raycast.RaycastResult(true,
                    new Vector3i(0, 120, 2),
                    new Vector3i(0, 120, 1),
                    Block.FACE_NORTH,
                    1.0f));

            assertTrue(handleTargetedItemUse(player, world, bottle, BlockType.CAULDRON));

            assertEquals(1, world.getCauldronLevel(0, 120, 2));
            assertSame(ItemType.POTION, player.getInventory().getItemInHand().getType());
            assertEquals(PotionData.water(), player.getInventory().getItemInHand().getPotionData());
            assertTrue(player.isUsingItem());
            assertEquals(1, world.rebuildCount);
            assertEquals(0, world.lastRebuildX);
            assertEquals(120, world.lastRebuildY);
            assertEquals(2, world.lastRebuildZ);
        } finally {
            world.cleanup();
        }
    }

    private static void setTargetBlock(Player player, Raycast.RaycastResult result) throws Exception {
        Field field = Player.class.getDeclaredField("targetBlock");
        field.setAccessible(true);
        field.set(player, result);
    }

    private static boolean handleBucketUse(Player player, World world, ItemStack stack) throws Exception {
        Method method = Player.class.getDeclaredMethod("handleBucketUse", World.class, ItemStack.class);
        method.setAccessible(true);
        return (boolean) method.invoke(player, world, stack);
    }

    private static boolean handleTargetedItemUse(Player player, World world, ItemStack stack,
            BlockType clickedBlock) throws Exception {
        Method method = Player.class.getDeclaredMethod("handleTargetedItemUse",
                World.class, ItemStack.class, BlockType.class);
        method.setAccessible(true);
        return (boolean) method.invoke(player, world, stack, clickedBlock);
    }

    private static boolean useImmediate(Player player, World world, ItemStack stack) throws Exception {
        Method method = Player.class.getDeclaredMethod("handleImmediateItemUse",
                World.class, ItemStack.class, Vector3f.class, Vector3f.class);
        method.setAccessible(true);
        return (boolean) method.invoke(player, world, stack,
                new Vector3f(0.5f, 120.5f, 0.5f),
                new Vector3f(0.0f, 0.0f, 1.0f));
    }

    private static final class RecordingWorld extends World {
        private int rebuildCount;
        private int lastRebuildX;
        private int lastRebuildY;
        private int lastRebuildZ;

        private RecordingWorld(long seed) {
            super(seed);
        }

        @Override
        public void rebuildBlockMeshesNow(int x, int y, int z) {
            rebuildCount++;
            lastRebuildX = x;
            lastRebuildY = y;
            lastRebuildZ = z;
        }
    }
}
