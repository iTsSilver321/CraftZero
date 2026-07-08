package com.craftzero.ui;

import com.craftzero.engine.Input;
import com.craftzero.inventory.Inventory;
import com.craftzero.inventory.ItemStack;
import com.craftzero.inventory.ItemType;
import com.craftzero.main.Player;
import com.craftzero.world.BlockType;
import com.craftzero.world.World;
import org.joml.Vector3i;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT_SHIFT;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_1;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_9;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT_SHIFT;
import static org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_LEFT;

class CraftingTableScreenTest {

    @AfterEach
    void clearInputState() throws Exception {
        setKeyDown(GLFW_KEY_LEFT_SHIFT, false);
        setKeyDown(GLFW_KEY_RIGHT_SHIFT, false);
        clearHotbarKeys();
        setMouseButton(GLFW_MOUSE_BUTTON_LEFT, false, false, false);
    }

    @Test
    @DisplayName("Crafting table output shift-click should honor either Shift key")
    void rightShiftClickOutputCraftsIntoPlayerInventory() throws Exception {
        Inventory inventory = new Inventory();
        CraftingTableScreen screen = new CraftingTableScreen(inventory);
        craftingGrid(screen)[0] = new ItemStack(ItemType.OAK_LOG, 1);

        setKeyDown(GLFW_KEY_RIGHT_SHIFT, true);
        clickSlot(screen, 9, false);

        assertNull(craftingGrid(screen)[0]);
        assertSame(ItemType.OAK_PLANKS, inventory.getHotbar()[0].getType());
        assertEquals(4, inventory.getHotbar()[0].getCount());
    }

    @Test
    @DisplayName("Crafting table shift-click should move crafting-grid items back to player inventory")
    void shiftClickCraftingGridMovesItemToPlayerInventory() throws Exception {
        Inventory inventory = new Inventory();
        CraftingTableScreen screen = new CraftingTableScreen(inventory);
        craftingGrid(screen)[0] = new ItemStack(ItemType.DIRT, 5);

        setKeyDown(GLFW_KEY_LEFT_SHIFT, true);
        clickSlot(screen, 0, false);

        assertNull(craftingGrid(screen)[0]);
        assertSame(ItemType.DIRT, inventory.getHotbar()[0].getType());
        assertEquals(5, inventory.getHotbar()[0].getCount());
    }

    @Test
    @DisplayName("Crafting table output should preserve dynamic repair durability")
    void craftingTableOutputUsesStackAwareRepairRecipe() throws Exception {
        Inventory inventory = new Inventory();
        CraftingTableScreen screen = new CraftingTableScreen(inventory);
        craftingGrid(screen)[0] = damaged(ItemType.IRON_PICKAXE, 10);
        craftingGrid(screen)[4] = damaged(ItemType.IRON_PICKAXE, 20);

        clickSlot(screen, 9, false);

        ItemStack cursor = inventory.getCursorItem();
        assertNotNull(cursor);
        assertSame(ItemType.IRON_PICKAXE, cursor.getType());
        int expectedDurability = Math.min(ItemType.IRON_PICKAXE.getMaxDurability(),
                10 + 20 + ItemType.IRON_PICKAXE.getMaxDurability() * 5 / 100);
        assertEquals(expectedDurability, cursor.getDurability());
        assertNull(craftingGrid(screen)[0]);
        assertNull(craftingGrid(screen)[4]);
    }

    @Test
    @DisplayName("Crafting table shift-click output should notify each crafted result")
    void craftingTableShiftClickOutputNotifiesCraftedResults() throws Exception {
        Inventory inventory = new Inventory();
        CraftingTableScreen screen = new CraftingTableScreen(inventory);
        List<ItemType> crafted = new ArrayList<>();
        inventory.setCraftedItemListener(stack -> crafted.add(stack.getType()));
        craftingGrid(screen)[0] = new ItemStack(ItemType.OAK_LOG, 2);

        setKeyDown(GLFW_KEY_LEFT_SHIFT, true);
        clickSlot(screen, 9, false);

        assertNull(craftingGrid(screen)[0]);
        assertStack(inventory.getHotbar()[0], ItemType.OAK_PLANKS, 8);
        assertEquals(List.of(ItemType.OAK_PLANKS, ItemType.OAK_PLANKS), crafted);
    }

    @Test
    @DisplayName("Crafting table cake output should return empty buckets to the crafting grid")
    void cakeOutputReturnsEmptyBucketsToCraftingGrid() throws Exception {
        Inventory inventory = new Inventory();
        CraftingTableScreen screen = new CraftingTableScreen(inventory);
        setCakeRecipe(screen);

        clickSlot(screen, 9, false);

        assertStack(inventory.getCursorItem(), ItemType.CAKE, 1);
        assertStack(craftingGrid(screen)[0], ItemType.BUCKET, 1);
        assertStack(craftingGrid(screen)[1], ItemType.BUCKET, 1);
        assertStack(craftingGrid(screen)[2], ItemType.BUCKET, 1);
        for (int i = 3; i < 9; i++) {
            assertNull(craftingGrid(screen)[i]);
        }
    }

    @Test
    @DisplayName("Crafting table shift-click cake output should move cake and keep bucket remainders")
    void shiftClickCakeOutputKeepsBucketRemainders() throws Exception {
        Inventory inventory = new Inventory();
        CraftingTableScreen screen = new CraftingTableScreen(inventory);
        setCakeRecipe(screen);

        setKeyDown(GLFW_KEY_LEFT_SHIFT, true);
        clickSlot(screen, 9, false);

        assertStack(inventory.getHotbar()[0], ItemType.CAKE, 1);
        assertStack(craftingGrid(screen)[0], ItemType.BUCKET, 1);
        assertStack(craftingGrid(screen)[1], ItemType.BUCKET, 1);
        assertStack(craftingGrid(screen)[2], ItemType.BUCKET, 1);
        for (int i = 3; i < 9; i++) {
            assertNull(craftingGrid(screen)[i]);
        }
    }

    @Test
    @DisplayName("Crafting table should drop container remainders that cannot fit grid or inventory")
    void occupiedIngredientSlotsDropOverflowRemainders() throws Exception {
        Inventory inventory = new Inventory();
        CraftingTableScreen screen = new CraftingTableScreen(inventory);
        fillPlayerStorage(inventory);
        setCakeRecipe(screen);
        craftingGrid(screen)[0] = new ItemStack(ItemType.MILK_BUCKET, 2);
        craftingGrid(screen)[1] = new ItemStack(ItemType.MILK_BUCKET, 2);
        craftingGrid(screen)[2] = new ItemStack(ItemType.MILK_BUCKET, 2);

        clickSlot(screen, 9, false);

        assertStack(inventory.getCursorItem(), ItemType.CAKE, 1);
        assertStack(craftingGrid(screen)[0], ItemType.MILK_BUCKET, 1);
        assertStack(craftingGrid(screen)[1], ItemType.MILK_BUCKET, 1);
        assertStack(craftingGrid(screen)[2], ItemType.MILK_BUCKET, 1);

        List<ItemStack> drops = screen.getAndClearItemsToThrow();
        assertEquals(3, drops.size());
        for (ItemStack drop : drops) {
            assertStack(drop, ItemType.BUCKET, 1);
        }
    }

    @Test
    @DisplayName("Crafting table shift-click should transfer between main inventory and hotbar")
    void shiftClickPlayerInventoryTransfersBetweenMainAndHotbar() throws Exception {
        Inventory inventory = new Inventory();
        CraftingTableScreen screen = new CraftingTableScreen(inventory);
        inventory.getMainInventory()[0] = new ItemStack(ItemType.COAL, 2);
        inventory.getHotbar()[0] = new ItemStack(ItemType.DIAMOND, 1);

        setKeyDown(GLFW_KEY_LEFT_SHIFT, true);
        clickSlot(screen, 10, false);

        assertNull(inventory.getMainInventory()[0]);
        assertSame(ItemType.COAL, inventory.getHotbar()[1].getType());
        assertEquals(2, inventory.getHotbar()[1].getCount());

        clickSlot(screen, 37, false);

        assertNull(inventory.getHotbar()[0]);
        assertSame(ItemType.DIAMOND, inventory.getMainInventory()[0].getType());
        assertEquals(1, inventory.getMainInventory()[0].getCount());
    }

    @Test
    @DisplayName("Number keys should move hotbar stacks into crafting grid slots")
    void numberKeySwapsHotbarStackIntoCraftingGrid() throws Exception {
        Inventory inventory = new Inventory();
        CraftingTableScreen screen = new CraftingTableScreen(inventory);
        screen.open(800, 600);
        inventory.getHotbar()[0] = new ItemStack(ItemType.OAK_PLANKS, 4);

        setMouseAtCraftingSlot(screen, 0);
        ScreenDragTestSupport.pressKey(GLFW_KEY_1);
        screen.update();

        assertStack(craftingGrid(screen)[0], ItemType.OAK_PLANKS, 4);
        assertNull(inventory.getHotbar()[0]);
    }

    @Test
    @DisplayName("Number keys should not replace crafting output slots")
    void numberKeyDoesNotSwapIntoCraftingOutputSlot() throws Exception {
        Inventory inventory = new Inventory();
        CraftingTableScreen screen = new CraftingTableScreen(inventory);
        screen.open(800, 600);
        craftingGrid(screen)[0] = new ItemStack(ItemType.OAK_LOG, 1);
        inventory.getHotbar()[0] = new ItemStack(ItemType.DIRT, 1);

        setMouseAtOutputSlot(screen);
        ScreenDragTestSupport.pressKey(GLFW_KEY_1);
        screen.update();

        assertStack(craftingGrid(screen)[0], ItemType.OAK_LOG, 1);
        assertStack(inventory.getHotbar()[0], ItemType.DIRT, 1);
        assertNull(inventory.getCursorItem());
    }

    @Test
    @DisplayName("Crafting table screen should only stay usable near the same workbench block")
    void craftingTableScreenRequiresSameNearbyWorkbenchBlock() {
        World world = new World(8136L);
        try {
            world.setBlock(0, 70, 0, BlockType.CRAFTING_TABLE);
            Player player = new Player(0.0f, 70.0f, 0.0f);
            CraftingTableScreen screen = new CraftingTableScreen(player.getInventory());
            screen.open(world, new Vector3i(0, 70, 0), 800, 600);

            assertTrue(screen.isStillUsable(player));

            player.getPosition().set(9.0f, 70.0f, 0.0f);
            assertFalse(screen.isStillUsable(player));

            player.getPosition().set(0.0f, 70.0f, 0.0f);
            world.setBlock(0, 70, 0, BlockType.AIR);
            assertFalse(screen.isStillUsable(player));
            screen.close();
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Crafting table left-drag should split held stacks across the crafting grid")
    void leftDragSplitsCursorStackAcrossCraftingGrid() throws Exception {
        Inventory inventory = new Inventory();
        CraftingTableScreen screen = new CraftingTableScreen(inventory);
        screen.open(800, 600);
        inventory.setCursorItem(new ItemStack(ItemType.COBBLESTONE, 8));

        dragCraftingSlots(screen, 0, 1, 3, 4);

        assertStack(craftingGrid(screen)[0], ItemType.COBBLESTONE, 2);
        assertStack(craftingGrid(screen)[1], ItemType.COBBLESTONE, 2);
        assertStack(craftingGrid(screen)[3], ItemType.COBBLESTONE, 2);
        assertStack(craftingGrid(screen)[4], ItemType.COBBLESTONE, 2);
        assertNull(inventory.getCursorItem());
    }

    @Test
    @DisplayName("Crafting table double-click should collect matching grid and player stacks")
    void doubleClickCollectsMatchingCraftingAndPlayerStacks() throws Exception {
        Inventory inventory = new Inventory();
        CraftingTableScreen screen = new CraftingTableScreen(inventory);
        inventory.setCursorItem(new ItemStack(ItemType.COBBLESTONE, 10));
        craftingGrid(screen)[0] = new ItemStack(ItemType.COBBLESTONE, 8);
        inventory.getMainInventory()[0] = new ItemStack(ItemType.COBBLESTONE, 20);
        inventory.getHotbar()[0] = new ItemStack(ItemType.DIRT, 64);

        assertTrue(doubleClick(screen, 0));

        assertStack(inventory.getCursorItem(), ItemType.COBBLESTONE, 38);
        assertNull(craftingGrid(screen)[0]);
        assertNull(inventory.getMainInventory()[0]);
        assertStack(inventory.getHotbar()[0], ItemType.DIRT, 64);
    }

    @Test
    @DisplayName("Crafting table live double-click should collect through the input path")
    void updateDoubleClickCollectsMatchingCraftingStacks() throws Exception {
        Inventory inventory = new Inventory();
        CraftingTableScreen screen = new CraftingTableScreen(inventory);
        screen.open(800, 600);
        craftingGrid(screen)[0] = new ItemStack(ItemType.COBBLESTONE, 10);
        craftingGrid(screen)[1] = new ItemStack(ItemType.COBBLESTONE, 20);

        clickCraftingSlot(screen, 0);
        clickCraftingSlot(screen, 0);

        assertStack(inventory.getCursorItem(), ItemType.COBBLESTONE, 30);
        assertNull(craftingGrid(screen)[0]);
        assertNull(craftingGrid(screen)[1]);
    }

    private static ItemStack[] craftingGrid(CraftingTableScreen screen) throws Exception {
        Field craftingGrid = CraftingTableScreen.class.getDeclaredField("craftingGrid");
        craftingGrid.setAccessible(true);
        return (ItemStack[]) craftingGrid.get(screen);
    }

    private static ItemStack damaged(ItemType type, int durability) {
        ItemStack stack = new ItemStack(type, 1);
        stack.setDurability(durability);
        return stack;
    }

    private static void setCakeRecipe(CraftingTableScreen screen) throws Exception {
        ItemStack[] grid = craftingGrid(screen);
        grid[0] = new ItemStack(ItemType.MILK_BUCKET, 1);
        grid[1] = new ItemStack(ItemType.MILK_BUCKET, 1);
        grid[2] = new ItemStack(ItemType.MILK_BUCKET, 1);
        grid[3] = new ItemStack(ItemType.SUGAR, 1);
        grid[4] = new ItemStack(ItemType.EGG, 1);
        grid[5] = new ItemStack(ItemType.SUGAR, 1);
        grid[6] = new ItemStack(ItemType.WHEAT, 1);
        grid[7] = new ItemStack(ItemType.WHEAT, 1);
        grid[8] = new ItemStack(ItemType.WHEAT, 1);
    }

    private static void fillPlayerStorage(Inventory inventory) {
        for (int i = 0; i < inventory.getHotbar().length; i++) {
            inventory.getHotbar()[i] = new ItemStack(ItemType.DIRT, ItemType.DIRT.getMaxStackSize());
        }
        for (int i = 0; i < inventory.getMainInventory().length; i++) {
            inventory.getMainInventory()[i] = new ItemStack(ItemType.DIRT, ItemType.DIRT.getMaxStackSize());
        }
    }

    private static void clickSlot(CraftingTableScreen screen, int slot, boolean rightClick) throws Exception {
        Method handleClick = CraftingTableScreen.class.getDeclaredMethod("handleClick", int.class, boolean.class);
        handleClick.setAccessible(true);
        handleClick.invoke(screen, slot, rightClick);
    }

    private static boolean doubleClick(CraftingTableScreen screen, int slot) throws Exception {
        Method handleDoubleClick = CraftingTableScreen.class.getDeclaredMethod("handleDoubleClick", int.class);
        handleDoubleClick.setAccessible(true);
        return (boolean) handleDoubleClick.invoke(screen, slot);
    }

    private static void assertStack(ItemStack stack, ItemType type, int count) {
        assertNotNull(stack);
        assertSame(type, stack.getType());
        assertEquals(count, stack.getCount());
    }

    private static void dragCraftingSlots(CraftingTableScreen screen, int... slots) throws Exception {
        setMouseAtCraftingSlot(screen, slots[0]);
        setMouseButton(GLFW_MOUSE_BUTTON_LEFT, true, true, false);
        screen.update();
        for (int i = 1; i < slots.length; i++) {
            setMouseAtCraftingSlot(screen, slots[i]);
            setMouseButton(GLFW_MOUSE_BUTTON_LEFT, true, false, false);
            screen.update();
        }
        setMouseAtCraftingSlot(screen, slots[slots.length - 1]);
        setMouseButton(GLFW_MOUSE_BUTTON_LEFT, false, false, true);
        screen.update();
        setMouseButton(GLFW_MOUSE_BUTTON_LEFT, false, false, false);
    }

    private static void clickCraftingSlot(CraftingTableScreen screen, int slot) throws Exception {
        setMouseAtCraftingSlot(screen, slot);
        setMouseButton(GLFW_MOUSE_BUTTON_LEFT, true, true, false);
        screen.update();
        setMouseButton(GLFW_MOUSE_BUTTON_LEFT, false, false, true);
        screen.update();
        setMouseButton(GLFW_MOUSE_BUTTON_LEFT, false, false, false);
    }

    private static void setMouseAtCraftingSlot(CraftingTableScreen screen, int slot) throws Exception {
        int col = slot % CraftingTableScreen.CRAFTING_COLS;
        int row = slot / CraftingTableScreen.CRAFTING_COLS;
        int texX = CraftingTableScreen.TEX_CRAFT_GRID_X + col * CraftingTableScreen.TEX_SLOT_SIZE
                + CraftingTableScreen.TEX_SLOT_SIZE / 2;
        int texY = CraftingTableScreen.TEX_CRAFT_GRID_Y + row * CraftingTableScreen.TEX_SLOT_SIZE
                + CraftingTableScreen.TEX_SLOT_SIZE / 2;
        setMousePosition(screen.getWindowX() + texX * CraftingTableScreen.GUI_SCALE,
                screen.getWindowY() + texY * CraftingTableScreen.GUI_SCALE);
    }

    private static void setMouseAtOutputSlot(CraftingTableScreen screen) throws Exception {
        int texX = CraftingTableScreen.TEX_CRAFT_OUTPUT_X + CraftingTableScreen.TEX_SLOT_SIZE / 2;
        int texY = CraftingTableScreen.TEX_CRAFT_OUTPUT_Y + CraftingTableScreen.TEX_SLOT_SIZE / 2;
        setMousePosition(screen.getWindowX() + texX * CraftingTableScreen.GUI_SCALE,
                screen.getWindowY() + texY * CraftingTableScreen.GUI_SCALE);
    }

    private static void setKeyDown(int key, boolean down) throws Exception {
        Field keys = Input.class.getDeclaredField("keys");
        keys.setAccessible(true);
        ((boolean[]) keys.get(null))[key] = down;
    }

    private static void setMouseButton(int button, boolean down, boolean pressed, boolean released) throws Exception {
        setInputButtonArray("buttons", button, down);
        setInputButtonArray("buttonsPressed", button, pressed);
        setInputButtonArray("buttonsReleased", button, released);
    }

    private static void setInputButtonArray(String fieldName, int button, boolean value) throws Exception {
        Field field = Input.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        ((boolean[]) field.get(null))[button] = value;
    }

    private static void setMousePosition(double x, double y) throws Exception {
        Field mouseX = Input.class.getDeclaredField("mouseX");
        Field mouseY = Input.class.getDeclaredField("mouseY");
        mouseX.setAccessible(true);
        mouseY.setAccessible(true);
        mouseX.setDouble(null, x);
        mouseY.setDouble(null, y);
    }

    private static void clearHotbarKeys() throws Exception {
        for (int key = GLFW_KEY_1; key <= GLFW_KEY_9; key++) {
            ScreenDragTestSupport.clearKeys(key);
        }
    }
}
