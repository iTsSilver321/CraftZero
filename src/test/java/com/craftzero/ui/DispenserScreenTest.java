package com.craftzero.ui;

import com.craftzero.inventory.Inventory;
import com.craftzero.inventory.ItemStack;
import com.craftzero.inventory.ItemType;
import com.craftzero.main.Player;
import com.craftzero.world.BlockType;
import com.craftzero.world.World;
import com.craftzero.world.tile.DispenserTileEntity;
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

class DispenserScreenTest {
    @AfterEach
    void clearMouseButtons() throws Exception {
        ScreenDragTestSupport.clearMouseButtons(GLFW_MOUSE_BUTTON_LEFT, GLFW_MOUSE_BUTTON_RIGHT);
        clearHotbarKeys();
    }

    @Test
    @DisplayName("Dispenser screen should move cursor stacks into the 3x3 inventory")
    void clickMovesStacksIntoDispenserInventory() throws Exception {
        Inventory inventory = new Inventory();
        DispenserTileEntity dispenser = new DispenserTileEntity(0, 70, 0);
        DispenserScreen screen = boundScreen(inventory, dispenser);

        inventory.setCursorItem(new ItemStack(ItemType.ARROW, 8));
        click(screen, 0, false);

        assertNull(inventory.getCursorItem());
        assertSame(ItemType.ARROW, dispenser.getInventory()[0].getType());
        assertEquals(8, dispenser.getInventory()[0].getCount());

        inventory.setCursorItem(new ItemStack(ItemType.DIAMOND, 3));
        click(screen, 0, false);

        assertSame(ItemType.ARROW, inventory.getCursorItem().getType());
        assertEquals(8, inventory.getCursorItem().getCount());
        assertSame(ItemType.DIAMOND, dispenser.getInventory()[0].getType());
        assertEquals(3, dispenser.getInventory()[0].getCount());
    }

    @Test
    @DisplayName("Dispenser screen should support Release-style right-click splitting")
    void rightClickSplitsAndPlacesOneItem() throws Exception {
        Inventory inventory = new Inventory();
        DispenserTileEntity dispenser = new DispenserTileEntity(0, 70, 0);
        DispenserScreen screen = boundScreen(inventory, dispenser);

        dispenser.getInventory()[0] = new ItemStack(ItemType.ARROW, 5);
        click(screen, 0, true);

        assertSame(ItemType.ARROW, inventory.getCursorItem().getType());
        assertEquals(3, inventory.getCursorItem().getCount());
        assertEquals(2, dispenser.getInventory()[0].getCount());

        click(screen, 1, true);

        assertSame(ItemType.ARROW, dispenser.getInventory()[1].getType());
        assertEquals(1, dispenser.getInventory()[1].getCount());
        assertEquals(2, inventory.getCursorItem().getCount());
    }

    @Test
    @DisplayName("Number keys should swap hovered dispenser slots with hotbar slots")
    void numberKeySwapsDispenserSlotWithHotbarSlot() throws Exception {
        Inventory inventory = new Inventory();
        DispenserTileEntity dispenser = new DispenserTileEntity(0, 70, 0);
        DispenserScreen screen = new DispenserScreen(inventory);
        screen.open(dispenser, 800, 600);
        dispenser.getInventory()[0] = new ItemStack(ItemType.ARROW, 8);
        inventory.getHotbar()[0] = new ItemStack(ItemType.SNOWBALL, 16);

        double[] point = dispenserSlotCenter(screen, 0);
        ScreenDragTestSupport.setMousePosition(point[0], point[1]);
        ScreenDragTestSupport.pressKey(GLFW_KEY_1);
        screen.update();

        assertStack(dispenser.getInventory()[0], ItemType.SNOWBALL, 16);
        assertStack(inventory.getHotbar()[0], ItemType.ARROW, 8);

        screen.close();
    }

    @Test
    @DisplayName("Dispenser drag should split held stacks across the 3x3 inventory")
    void leftDragSplitsCursorStackAcrossDispenserSlots() throws Exception {
        Inventory inventory = new Inventory();
        DispenserTileEntity dispenser = new DispenserTileEntity(0, 70, 0);
        DispenserScreen screen = new DispenserScreen(inventory);
        screen.open(dispenser, 800, 600);
        inventory.setCursorItem(new ItemStack(ItemType.ARROW, 9));

        ScreenDragTestSupport.drag(GLFW_MOUSE_BUTTON_LEFT, screen::update,
                dispenserSlotCenter(screen, 0),
                dispenserSlotCenter(screen, 1),
                dispenserSlotCenter(screen, 2));

        assertStack(dispenser.getInventory()[0], ItemType.ARROW, 3);
        assertStack(dispenser.getInventory()[1], ItemType.ARROW, 3);
        assertStack(dispenser.getInventory()[2], ItemType.ARROW, 3);
        assertNull(inventory.getCursorItem());

        screen.close();
    }

    @Test
    @DisplayName("Dispenser double-click should collect matching container and player stacks")
    void doubleClickCollectsMatchingDispenserAndPlayerStacks() throws Exception {
        Inventory inventory = new Inventory();
        DispenserTileEntity dispenser = new DispenserTileEntity(0, 70, 0);
        DispenserScreen screen = boundScreen(inventory, dispenser);
        inventory.setCursorItem(new ItemStack(ItemType.ARROW, 10));
        dispenser.getInventory()[0] = new ItemStack(ItemType.ARROW, 20);
        dispenser.getInventory()[1] = new ItemStack(ItemType.SNOWBALL, 16);
        inventory.getMainInventory()[0] = new ItemStack(ItemType.ARROW, 40);

        assertTrue(doubleClick(screen, 0));

        assertStack(inventory.getCursorItem(), ItemType.ARROW, 64);
        assertNull(dispenser.getInventory()[0]);
        assertStack(dispenser.getInventory()[1], ItemType.SNOWBALL, 16);
        assertStack(inventory.getMainInventory()[0], ItemType.ARROW, 6);
    }

    @Test
    @DisplayName("Dispenser screen shift-click should move player stacks into the 3x3 inventory")
    void shiftClickMovesPlayerStacksIntoDispenserInventory() throws Exception {
        Inventory inventory = new Inventory();
        DispenserTileEntity dispenser = new DispenserTileEntity(0, 70, 0);
        DispenserScreen screen = boundScreen(inventory, dispenser);

        inventory.getMainInventory()[0] = new ItemStack(ItemType.SNOWBALL, 16);
        shiftClick(screen, DispenserTileEntity.SIZE, inventory.getMainInventory()[0]);

        assertNull(inventory.getMainInventory()[0]);
        assertSame(ItemType.SNOWBALL, dispenser.getInventory()[0].getType());
        assertEquals(16, dispenser.getInventory()[0].getCount());
    }

    @Test
    @DisplayName("Dispenser screen should only stay usable for the same nearby dispenser tile")
    void dispenserScreenRequiresSameNearbyDispenserTile() {
        World world = new World(8133L);
        try {
            world.setBlock(0, 70, 0, BlockType.DISPENSER);
            DispenserTileEntity dispenser = (DispenserTileEntity) world.getTileEntity(0, 70, 0);
            Player player = new Player(0.0f, 70.0f, 0.0f);
            DispenserScreen screen = new DispenserScreen(player.getInventory());
            screen.open(dispenser, 800, 600);

            assertTrue(screen.isStillUsable(world, player));

            player.getPosition().set(9.0f, 70.0f, 0.0f);
            assertFalse(screen.isStillUsable(world, player));

            player.getPosition().set(0.0f, 70.0f, 0.0f);
            world.setBlock(0, 70, 0, BlockType.AIR);
            assertFalse(screen.isStillUsable(world, player));

            world.setBlock(0, 70, 0, BlockType.DISPENSER);
            assertFalse(screen.isStillUsable(world, player));
            screen.close();
        } finally {
            world.cleanup();
        }
    }

    private static DispenserScreen boundScreen(Inventory inventory, DispenserTileEntity dispenser) throws Exception {
        DispenserScreen screen = new DispenserScreen(inventory);
        Field dispenserField = DispenserScreen.class.getDeclaredField("dispenser");
        dispenserField.setAccessible(true);
        dispenserField.set(screen, dispenser);
        return screen;
    }

    private static void click(DispenserScreen screen, int slot, boolean rightClick) throws Exception {
        Method handleClick = DispenserScreen.class.getDeclaredMethod("handleClick", int.class, boolean.class);
        handleClick.setAccessible(true);
        handleClick.invoke(screen, slot, rightClick);
    }

    private static boolean doubleClick(DispenserScreen screen, int slot) throws Exception {
        Method handleDoubleClick = DispenserScreen.class.getDeclaredMethod("handleDoubleClick", int.class);
        handleDoubleClick.setAccessible(true);
        return (boolean) handleDoubleClick.invoke(screen, slot);
    }

    private static void shiftClick(DispenserScreen screen, int slot, ItemStack stack) throws Exception {
        Method shiftClick = DispenserScreen.class.getDeclaredMethod("shiftClick", int.class, ItemStack.class);
        shiftClick.setAccessible(true);
        shiftClick.invoke(screen, slot, stack);
    }

    private static double[] dispenserSlotCenter(DispenserScreen screen, int slot) {
        int col = slot % DispenserScreen.DISPENSER_COLS;
        int row = slot / DispenserScreen.DISPENSER_COLS;
        double x = screen.getWindowX() + (DispenserScreen.TEX_CONTAINER_X + col * DispenserScreen.TEX_SLOT_SIZE
                + DispenserScreen.TEX_SLOT_SIZE / 2.0) * DispenserScreen.GUI_SCALE;
        double y = screen.getWindowY() + (DispenserScreen.TEX_CONTAINER_Y + row * DispenserScreen.TEX_SLOT_SIZE
                + DispenserScreen.TEX_SLOT_SIZE / 2.0) * DispenserScreen.GUI_SCALE;
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
