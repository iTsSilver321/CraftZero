package com.craftzero.main;

import com.craftzero.entity.PrimedTntEntity;
import com.craftzero.inventory.ItemStack;
import com.craftzero.inventory.ItemType;
import com.craftzero.physics.Raycast;
import com.craftzero.world.Block;
import com.craftzero.world.BlockType;
import com.craftzero.world.World;
import com.craftzero.world.WorldSoundEvent;
import org.joml.Vector3i;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayerFireInteractionTest {
    @Test
    @DisplayName("Flint and steel fire placement should emit the Release-style ignition sound")
    void flintAndSteelFirePlacementEmitsSound() throws Exception {
        RecordingWorld world = new RecordingWorld(6272L);
        try {
            world.setBlock(0, 70, 0, BlockType.STONE, 0);
            Player player = new Player(0.0f, 70.0f, 0.0f);
            ItemStack flintAndSteel = new ItemStack(ItemType.FLINT_AND_STEEL, 1);
            player.getInventory().getHotbar()[player.getInventory().getSelectedSlot()] = flintAndSteel;
            setTargetBlock(player, new Raycast.RaycastResult(true,
                    new Vector3i(0, 70, 0),
                    new Vector3i(0, 71, 0),
                    Block.FACE_TOP,
                    1.0f));

            assertTrue(handleTargetedItemUse(player, world, flintAndSteel, BlockType.STONE));

            assertSame(BlockType.FIRE, world.getBlock(0, 71, 0));
            WorldSoundEvent sound = world.drainSoundEvents().get(0);
            assertEquals(WorldSoundEvent.FIRE_IGNITE, sound.soundId());
            assertEquals(1.0f, sound.volume(), 0.0001f);
            assertTrue(sound.pitch() >= 0.8f);
            assertTrue(sound.pitch() <= 1.2f);
            assertEquals(1, world.rebuildCount);
            assertEquals(0, world.lastRebuildX);
            assertEquals(71, world.lastRebuildY);
            assertEquals(0, world.lastRebuildZ);
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Flint and steel should not place unsupported fire")
    void flintAndSteelRejectsUnsupportedFirePlacement() throws Exception {
        World world = new World(6273L);
        try {
            world.setBlock(0, 120, 0, BlockType.STONE, 0);
            Player player = new Player(0.0f, 120.0f, 0.0f);
            ItemStack flintAndSteel = new ItemStack(ItemType.FLINT_AND_STEEL, 1);
            player.getInventory().getHotbar()[player.getInventory().getSelectedSlot()] = flintAndSteel;
            setTargetBlock(player, new Raycast.RaycastResult(true,
                    new Vector3i(0, 120, 0),
                    new Vector3i(1, 120, 0),
                    Block.FACE_EAST,
                    1.0f));
            int durability = flintAndSteel.getDurability();

            assertFalse(handleTargetedItemUse(player, world, flintAndSteel, BlockType.STONE));

            assertSame(BlockType.AIR, world.getBlock(1, 120, 0));
            assertEquals(durability, flintAndSteel.getDurability());
            assertTrue(world.drainSoundEvents().isEmpty());
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Flint and steel should prime TNT and refresh the removed block")
    void flintAndSteelPrimesTntAndRebuildsRemovedBlockMesh() throws Exception {
        RecordingWorld world = new RecordingWorld(6274L);
        try {
            world.setBlock(0, 70, 0, BlockType.TNT, 0);
            Player player = new Player(0.0f, 70.0f, 0.0f);
            ItemStack flintAndSteel = new ItemStack(ItemType.FLINT_AND_STEEL, 1);
            player.getInventory().getHotbar()[player.getInventory().getSelectedSlot()] = flintAndSteel;
            setTargetBlock(player, new Raycast.RaycastResult(true,
                    new Vector3i(0, 70, 0),
                    new Vector3i(0, 71, 0),
                    Block.FACE_TOP,
                    1.0f));
            int durability = flintAndSteel.getDurability();

            assertTrue(handleTargetedItemUse(player, world, flintAndSteel, BlockType.TNT));

            assertSame(BlockType.AIR, world.getBlock(0, 70, 0));
            world.updateEntities(1.0f / 20.0f);
            assertTrue(world.getEntities().stream().anyMatch(PrimedTntEntity.class::isInstance));
            assertEquals(durability - 1, flintAndSteel.getDurability());
            WorldSoundEvent sound = world.drainSoundEvents().get(0);
            assertEquals(WorldSoundEvent.TNT_FUSE, sound.soundId());
            assertTrue(player.isUsingItem());
            assertEquals(1, world.rebuildCount);
            assertEquals(0, world.lastRebuildX);
            assertEquals(70, world.lastRebuildY);
            assertEquals(0, world.lastRebuildZ);
        } finally {
            world.cleanup();
        }
    }

    private static void setTargetBlock(Player player, Raycast.RaycastResult result) throws Exception {
        Field field = Player.class.getDeclaredField("targetBlock");
        field.setAccessible(true);
        field.set(player, result);
    }

    private static boolean handleTargetedItemUse(Player player, World world, ItemStack stack,
            BlockType clickedBlock) throws Exception {
        Method method = Player.class.getDeclaredMethod("handleTargetedItemUse",
                World.class, ItemStack.class, BlockType.class);
        method.setAccessible(true);
        return (boolean) method.invoke(player, world, stack, clickedBlock);
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
