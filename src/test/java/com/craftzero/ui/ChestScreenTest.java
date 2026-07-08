package com.craftzero.ui;

import com.craftzero.entity.ChestMinecartEntity;
import com.craftzero.inventory.Inventory;
import com.craftzero.inventory.ItemStack;
import com.craftzero.inventory.ItemType;
import com.craftzero.main.Player;
import com.craftzero.world.BlockType;
import com.craftzero.world.World;
import com.craftzero.world.tile.ChestTileEntity;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_1;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_9;
import static org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_LEFT;
import static org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_RIGHT;

class ChestScreenTest {
    @AfterEach
    void clearMouseButtons() throws Exception {
        ScreenDragTestSupport.clearMouseButtons(GLFW_MOUSE_BUTTON_LEFT, GLFW_MOUSE_BUTTON_RIGHT);
        clearHotbarKeys();
    }

    @Test
    @DisplayName("Double chest screen should expose north/west half before south/east half")
    void doubleChestUsesReleaseStyleInventoryOrder() {
        World world = new World(24L);
        try {
            world.setBlock(0, 70, 0, BlockType.CHEST);
            world.setBlock(1, 70, 0, BlockType.CHEST);
            ChestTileEntity west = (ChestTileEntity) world.getTileEntity(0, 70, 0);
            ChestTileEntity east = (ChestTileEntity) world.getTileEntity(1, 70, 0);
            west.getInventory()[0] = new ItemStack(ItemType.COAL, 1);
            east.getInventory()[0] = new ItemStack(ItemType.DIAMOND, 1);

            ChestScreen screen = new ChestScreen(new Inventory());
            screen.bindChests(world, east);

            assertEquals(54, screen.getContainerSize());
            assertSame(ItemType.COAL, screen.getItemInSlot(0).getType());
            assertSame(ItemType.DIAMOND, screen.getItemInSlot(ChestTileEntity.SIZE).getType());

            world.setBlock(4, 70, -1, BlockType.CHEST);
            world.setBlock(4, 70, 0, BlockType.CHEST);
            ChestTileEntity north = (ChestTileEntity) world.getTileEntity(4, 70, -1);
            ChestTileEntity south = (ChestTileEntity) world.getTileEntity(4, 70, 0);
            north.getInventory()[0] = new ItemStack(ItemType.IRON_INGOT, 1);
            south.getInventory()[0] = new ItemStack(ItemType.GOLD_INGOT, 1);

            screen.bindChests(world, south);

            assertSame(ItemType.IRON_INGOT, screen.getItemInSlot(0).getType());
            assertSame(ItemType.GOLD_INGOT, screen.getItemInSlot(ChestTileEntity.SIZE).getType());
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Chest screen can bind to a chest minecart inventory")
    void clickMovesStacksIntoChestMinecartInventory() throws Exception {
        Inventory inventory = new Inventory();
        ChestMinecartEntity minecart = new ChestMinecartEntity();
        ChestScreen screen = boundMinecartScreen(inventory, minecart);

        inventory.setCursorItem(new ItemStack(ItemType.DIAMOND, 5));
        click(screen, 0, false);

        assertNull(inventory.getCursorItem());
        assertSame(ItemType.DIAMOND, minecart.getInventory()[0].getType());
        assertEquals(5, minecart.getInventory()[0].getCount());

        minecart.getInventory()[1] = new ItemStack(ItemType.COAL, 4);
        click(screen, 1, false);

        assertNull(minecart.getInventory()[1]);
        assertSame(ItemType.COAL, inventory.getCursorItem().getType());
        assertEquals(4, inventory.getCursorItem().getCount());
        assertEquals(ChestMinecartEntity.SIZE, screen.getContainerSize());
    }

    @Test
    @DisplayName("Number keys should swap hovered chest slots with hotbar slots")
    void numberKeySwapsChestSlotWithHotbarSlot() throws Exception {
        Inventory inventory = new Inventory();
        ChestMinecartEntity minecart = new ChestMinecartEntity();
        ChestScreen screen = new ChestScreen(inventory);
        screen.openMinecart(minecart, 800, 600);
        minecart.getInventory()[0] = new ItemStack(ItemType.COAL, 4);
        inventory.getHotbar()[0] = new ItemStack(ItemType.DIAMOND, 1);

        double[] point = chestSlotCenter(screen, 0);
        ScreenDragTestSupport.setMousePosition(point[0], point[1]);
        ScreenDragTestSupport.pressKey(GLFW_KEY_1);
        screen.update();

        assertStack(minecart.getInventory()[0], ItemType.DIAMOND, 1);
        assertStack(inventory.getHotbar()[0], ItemType.COAL, 4);

        screen.close();
    }

    @Test
    @DisplayName("Chest drag should split held stacks across container slots")
    void leftDragSplitsCursorStackAcrossChestSlots() throws Exception {
        Inventory inventory = new Inventory();
        ChestMinecartEntity minecart = new ChestMinecartEntity();
        ChestScreen screen = new ChestScreen(inventory);
        screen.openMinecart(minecart, 800, 600);
        inventory.setCursorItem(new ItemStack(ItemType.DIRT, 9));

        ScreenDragTestSupport.drag(GLFW_MOUSE_BUTTON_LEFT, screen::update,
                chestSlotCenter(screen, 0),
                chestSlotCenter(screen, 1),
                chestSlotCenter(screen, 2));

        assertStack(minecart.getInventory()[0], ItemType.DIRT, 3);
        assertStack(minecart.getInventory()[1], ItemType.DIRT, 3);
        assertStack(minecart.getInventory()[2], ItemType.DIRT, 3);
        assertNull(inventory.getCursorItem());

        screen.close();
    }

    @Test
    @DisplayName("Chest minecart screen should only stay usable while the cart is alive and nearby")
    void chestMinecartScreenRequiresNearbyLiveCart() {
        Player player = new Player(0.0f, 70.0f, 0.0f);
        ChestMinecartEntity minecart = new ChestMinecartEntity(3.0f, 70.0f, 0.0f);
        ChestScreen screen = new ChestScreen(player.getInventory());
        screen.openMinecart(minecart, 800, 600);

        assertTrue(screen.isStillUsable(player));

        player.getPosition().set(12.0f, 70.0f, 0.0f);
        assertFalse(screen.isStillUsable(player));

        player.getPosition().set(0.0f, 70.0f, 0.0f);
        minecart.remove();
        assertFalse(screen.isStillUsable(player));

        screen.close();
    }

    @Test
    @DisplayName("Double-clicking with a carried stack should collect matching chest and player stacks")
    void doubleClickCollectsMatchingStacksAcrossChestAndPlayerInventory() throws Exception {
        Inventory inventory = new Inventory();
        ChestMinecartEntity minecart = new ChestMinecartEntity();
        ChestScreen screen = boundMinecartScreen(inventory, minecart);
        inventory.setCursorItem(new ItemStack(ItemType.COBBLESTONE, 10));
        minecart.getInventory()[0] = new ItemStack(ItemType.COBBLESTONE, 20);
        minecart.getInventory()[1] = new ItemStack(ItemType.DIRT, 64);
        inventory.getMainInventory()[0] = new ItemStack(ItemType.COBBLESTONE, 30);
        inventory.getHotbar()[0] = new ItemStack(ItemType.COBBLESTONE, 20);

        assertTrue(doubleClick(screen, 0));

        assertStack(inventory.getCursorItem(), ItemType.COBBLESTONE, 64);
        assertNull(minecart.getInventory()[0]);
        assertStack(minecart.getInventory()[1], ItemType.DIRT, 64);
        assertNull(inventory.getMainInventory()[0]);
        assertStack(inventory.getHotbar()[0], ItemType.COBBLESTONE, 16);
    }

    @Test
    @DisplayName("Chest screen should only stay usable for the same nearby chest tile")
    void chestScreenUsabilityRequiresSameNearbyChestTile() {
        World world = new World(1468L);
        try {
            world.setBlock(0, 70, 0, BlockType.CHEST);
            ChestTileEntity chest = (ChestTileEntity) world.getTileEntity(0, 70, 0);
            Player player = new Player(0.0f, 70.0f, 0.0f);
            ChestScreen screen = new ChestScreen(player.getInventory());
            screen.open(world, chest, 800, 600);

            assertTrue(screen.isStillUsable(player));

            player.getPosition().set(9.0f, 70.0f, 0.0f);
            assertFalse(screen.isStillUsable(player));

            player.getPosition().set(0.0f, 70.0f, 0.0f);
            world.setBlock(0, 70, 0, BlockType.AIR);
            assertFalse(screen.isStillUsable(player));

            world.setBlock(0, 70, 0, BlockType.CHEST);
            assertFalse(screen.isStillUsable(player));
            screen.close();
        } finally {
            world.cleanup();
        }
    }

    private static ChestScreen boundMinecartScreen(Inventory inventory, ChestMinecartEntity minecart) throws Exception {
        ChestScreen screen = new ChestScreen(inventory);
        Field minecartInventoryField = ChestScreen.class.getDeclaredField("minecartInventory");
        minecartInventoryField.setAccessible(true);
        minecartInventoryField.set(screen, minecart.getInventory());
        return screen;
    }

    private static void click(ChestScreen screen, int slot, boolean rightClick) throws Exception {
        Method handleClick = ChestScreen.class.getDeclaredMethod("handleClick", int.class, boolean.class);
        handleClick.setAccessible(true);
        handleClick.invoke(screen, slot, rightClick);
    }

    private static boolean doubleClick(ChestScreen screen, int slot) throws Exception {
        Method handleDoubleClick = ChestScreen.class.getDeclaredMethod("handleDoubleClick", int.class);
        handleDoubleClick.setAccessible(true);
        return (boolean) handleDoubleClick.invoke(screen, slot);
    }

    private static double[] chestSlotCenter(ChestScreen screen, int slot) {
        int col = slot % ChestScreen.COLS;
        int row = slot / ChestScreen.COLS;
        double x = screen.getWindowX() + (ChestScreen.TEX_CONTAINER_X + col * ChestScreen.TEX_SLOT_SIZE
                + ChestScreen.TEX_SLOT_SIZE / 2.0) * ChestScreen.GUI_SCALE;
        double y = screen.getWindowY() + (ChestScreen.TEX_CONTAINER_Y + row * ChestScreen.TEX_SLOT_SIZE
                + ChestScreen.TEX_SLOT_SIZE / 2.0) * ChestScreen.GUI_SCALE;
        return ScreenDragTestSupport.point(x, y);
    }

    private static void assertStack(ItemStack stack, ItemType type, int count) {
        assertSame(type, stack.getType());
        assertEquals(count, stack.getCount());
    }

    private static void clearHotbarKeys() throws Exception {
        for (int key = GLFW_KEY_1; key <= GLFW_KEY_9; key++) {
            ScreenDragTestSupport.clearKeys(key);
        }
    }
}
