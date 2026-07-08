package com.craftzero.main;

import com.craftzero.inventory.ItemStack;
import com.craftzero.inventory.ItemType;
import com.craftzero.inventory.MapItemData;
import com.craftzero.world.BlockType;
import com.craftzero.world.World;
import org.joml.Vector3f;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayerMapInteractionTest {
    private static final int DEFAULT_PIXEL_BLOCKS = 1 << MapItemData.DEFAULT_SCALE;

    @Test
    @DisplayName("Right-clicking a map should initialize/update it and play the hand-use animation")
    void immediateMapUseInitializesMap() throws Exception {
        World world = new World(9020L);
        try {
            placeMapPixelArea(world, BlockType.WATER);
            Player player = new Player(0.5f, 80.0f, 0.5f);
            player.getCamera().setYaw(90.0f);
            ItemStack map = new ItemStack(ItemType.MAP, 1);
            player.getInventory().getHotbar()[player.getInventory().getSelectedSlot()] = map;

            assertTrue(useImmediate(player, world, map));

            assertTrue(player.isUsingItem());
            MapItemData.View view = MapItemData.view(map);
            assertNotNull(view);
            assertTrue(view.initialized());
            assertEquals(64, view.playerPixelX());
            assertEquals(64, view.playerPixelZ());
            assertEquals(4, view.playerRotation());
            assertEquals(4, MapItemData.basePaletteIndex(view.colors()[64 + 64 * MapItemData.MAP_SIZE] & 0xFF));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Held selected maps should update during the normal player tick")
    void playerUpdateRefreshesHeldMap() {
        World world = new World(9021L);
        try {
            placeMapPixelArea(world, BlockType.SAND);
            Player player = new Player(0.5f, 80.0f, 0.5f);
            player.getCamera().setYaw(180.0f);
            ItemStack map = new ItemStack(ItemType.MAP, 1);
            player.getInventory().getHotbar()[player.getInventory().getSelectedSlot()] = map;

            player.update(1.0f / 20.0f, world);

            MapItemData.View view = MapItemData.view(map);
            assertNotNull(view);
            assertTrue(view.initialized());
            assertEquals(8, view.playerRotation());
            assertEquals(3, MapItemData.basePaletteIndex(view.colors()[64 + 64 * MapItemData.MAP_SIZE] & 0xFF));
        } finally {
            world.cleanup();
        }
    }

    private static boolean useImmediate(Player player, World world, ItemStack stack) throws Exception {
        Method method = Player.class.getDeclaredMethod("handleImmediateItemUse",
                World.class, ItemStack.class, Vector3f.class, Vector3f.class);
        method.setAccessible(true);
        return (boolean) method.invoke(player, world, stack,
                new Vector3f(0.5f, 81.5f, 0.5f),
                new Vector3f(0.0f, 0.0f, 1.0f));
    }

    private static void placeMapPixelArea(World world, BlockType type) {
        for (int dx = 0; dx < DEFAULT_PIXEL_BLOCKS; dx++) {
            for (int dz = 0; dz < DEFAULT_PIXEL_BLOCKS; dz++) {
                world.setBlock(dx, 127, dz, BlockType.AIR, 0);
                world.setBlock(dx, 126, dz, type, 0);
            }
        }
    }
}
