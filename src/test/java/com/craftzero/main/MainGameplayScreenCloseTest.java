package com.craftzero.main;

import com.craftzero.inventory.ItemStack;
import com.craftzero.inventory.ItemType;
import com.craftzero.ui.ChestScreen;
import com.craftzero.ui.FurnaceScreen;
import com.craftzero.ui.InventoryScreen;
import com.craftzero.save.SaveManager;
import com.craftzero.world.BlockType;
import com.craftzero.world.DayCycleManager;
import com.craftzero.world.World;
import com.craftzero.world.tile.ChestTileEntity;
import com.craftzero.world.tile.FurnaceTileEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MainGameplayScreenCloseTest {
    @TempDir
    Path tempDir;

    @Test
    @DisplayName("Closing a chest through the global screen path should drop the cursor stack")
    void closingChestThroughGlobalPathDropsCursorStack() throws Exception {
        World world = new World(6293L);
        try {
            Player player = new Player(0.0f, 70.0f, 0.0f);
            ChestScreen chestScreen = new ChestScreen(player.getInventory());
            world.setBlock(0, 70, 0, BlockType.CHEST);
            ChestTileEntity chest = (ChestTileEntity) world.getTileEntity(0, 70, 0);
            chestScreen.open(world, chest, 800, 600);
            player.getInventory().setCursorItem(new ItemStack(ItemType.DIAMOND, 4));

            Main main = new Main();
            setField(main, "world", world);
            setField(main, "player", player);
            setField(main, "chestScreen", chestScreen);

            assertTrue(closeGameplayScreen(main));

            assertFalse(chestScreen.isOpen());
            assertNull(player.getInventory().getCursorItem());
            assertEquals(1, world.getDroppedItems().size());
            assertSame(ItemType.DIAMOND, world.getDroppedItems().get(0).getItemType());
            assertEquals(4, world.getDroppedItems().get(0).getCount());
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Open chest screen should close when the backing chest disappears")
    void openChestScreenClosesWhenBackingChestDisappears() throws Exception {
        World world = new World(7721L);
        try {
            Player player = new Player(0.0f, 70.0f, 0.0f);
            ChestScreen chestScreen = new ChestScreen(player.getInventory());
            world.setBlock(0, 70, 0, BlockType.CHEST);
            ChestTileEntity chest = (ChestTileEntity) world.getTileEntity(0, 70, 0);
            chestScreen.open(world, chest, 800, 600);
            player.getInventory().setCursorItem(new ItemStack(ItemType.DIAMOND, 3));

            Main main = new Main();
            setField(main, "world", world);
            setField(main, "player", player);
            setField(main, "chestScreen", chestScreen);

            world.setBlock(0, 70, 0, BlockType.AIR);
            updateChestScreen(main);

            assertFalse(chestScreen.isOpen());
            assertNull(player.getInventory().getCursorItem());
            assertEquals(1, world.getDroppedItems().size());
            assertSame(ItemType.DIAMOND, world.getDroppedItems().get(0).getItemType());
            assertEquals(3, world.getDroppedItems().get(0).getCount());
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Open furnace screen should close when the backing furnace disappears")
    void openFurnaceScreenClosesWhenBackingFurnaceDisappears() throws Exception {
        World world = new World(7722L);
        try {
            Player player = new Player(0.0f, 70.0f, 0.0f);
            FurnaceScreen furnaceScreen = new FurnaceScreen(player.getInventory());
            world.setBlock(0, 70, 0, BlockType.FURNACE);
            FurnaceTileEntity furnace = (FurnaceTileEntity) world.getTileEntity(0, 70, 0);
            furnaceScreen.open(furnace, 800, 600);
            player.getInventory().setCursorItem(new ItemStack(ItemType.DIAMOND, 2));

            Main main = new Main();
            setField(main, "world", world);
            setField(main, "player", player);
            setField(main, "furnaceScreen", furnaceScreen);

            world.setBlock(0, 70, 0, BlockType.AIR);
            updateFurnaceScreen(main);

            assertFalse(furnaceScreen.isOpen());
            assertNull(player.getInventory().getCursorItem());
            assertEquals(1, world.getDroppedItems().size());
            assertSame(ItemType.DIAMOND, world.getDroppedItems().get(0).getItemType());
            assertEquals(2, world.getDroppedItems().get(0).getCount());
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Closing player inventory through the global screen path should drop carried and crafting stacks")
    void closingInventoryThroughGlobalPathDropsCursorAndCraftingStacks() throws Exception {
        World world = new World(4915L);
        try {
            Player player = new Player(0.0f, 70.0f, 0.0f);
            InventoryScreen inventoryScreen = new InventoryScreen(player.getInventory());
            inventoryScreen.open(800, 600);
            player.getInventory().getCraftingGrid()[0] = new ItemStack(ItemType.OAK_PLANKS, 2);
            player.getInventory().getCraftingGrid()[3] = new ItemStack(ItemType.STICK, 1);
            player.getInventory().setCursorItem(new ItemStack(ItemType.DIAMOND, 4));

            Main main = new Main();
            setField(main, "world", world);
            setField(main, "player", player);
            setField(main, "inventoryScreen", inventoryScreen);

            assertTrue(closeGameplayScreen(main));

            assertFalse(inventoryScreen.isOpen());
            assertNull(player.getInventory().getCraftingGrid()[0]);
            assertNull(player.getInventory().getCraftingGrid()[3]);
            assertNull(player.getInventory().getCursorItem());
            assertEquals(3, world.getDroppedItems().size());
            assertSame(ItemType.OAK_PLANKS, world.getDroppedItems().get(0).getItemType());
            assertEquals(2, world.getDroppedItems().get(0).getCount());
            assertSame(ItemType.STICK, world.getDroppedItems().get(1).getItemType());
            assertEquals(1, world.getDroppedItems().get(1).getCount());
            assertSame(ItemType.DIAMOND, world.getDroppedItems().get(2).getItemType());
            assertEquals(4, world.getDroppedItems().get(2).getCount());
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Saving unloads should increment and persist Games quit statistics")
    void savingUnloadRecordsGamesQuitStatistic() throws Exception {
        Path worldDir = tempDir.resolve("quit-stat-world");
        SaveManager saveManager = new SaveManager(worldDir);
        World world = new World(6294L);
        Player player = new Player(0.0f, 70.0f, 0.0f);
        DayCycleManager dayCycle = new DayCycleManager();
        world.setPlayer(player);
        world.setDayCycleManager(dayCycle);

        Main main = new Main();
        setField(main, "world", world);
        setField(main, "player", player);
        setField(main, "saveManager", saveManager);
        setField(main, "dayCycleManager", dayCycle);

        unloadWorld(main, true);

        assertEquals(1, player.getStats().getStatistics().getGamesQuit());
        SaveManager.LevelData saved = saveManager.loadLevelIfExists();
        assertNotNull(saved);
        assertEquals(1, saved.player.statGamesQuit);
    }

    private static boolean closeGameplayScreen(Main main) throws Exception {
        Method method = Main.class.getDeclaredMethod("closeGameplayScreen");
        method.setAccessible(true);
        return (boolean) method.invoke(main);
    }

    private static void updateChestScreen(Main main) throws Exception {
        Method method = Main.class.getDeclaredMethod("updateChestScreen");
        method.setAccessible(true);
        method.invoke(main);
    }

    private static void unloadWorld(Main main, boolean save) throws Exception {
        Method method = Main.class.getDeclaredMethod("unloadWorld", boolean.class);
        method.setAccessible(true);
        method.invoke(main, save);
    }

    private static void updateFurnaceScreen(Main main) throws Exception {
        Method method = Main.class.getDeclaredMethod("updateFurnaceScreen");
        method.setAccessible(true);
        method.invoke(main);
    }

    private static void setField(Main main, String fieldName, Object value) throws Exception {
        Field field = Main.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(main, value);
    }
}
