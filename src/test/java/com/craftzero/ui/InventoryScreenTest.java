package com.craftzero.ui;

import com.craftzero.inventory.Inventory;
import com.craftzero.inventory.ItemStack;
import com.craftzero.inventory.ItemType;
import com.craftzero.progression.ArmorSlot;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT_SHIFT;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_1;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_2;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_9;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT_SHIFT;
import static org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_LEFT;
import static org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_RIGHT;

class InventoryScreenTest {
    @AfterEach
    void clearInputState() throws Exception {
        setKeyDown(GLFW_KEY_LEFT_SHIFT, false);
        setKeyDown(GLFW_KEY_RIGHT_SHIFT, false);
        clearHotbarKeys();
        setMouseButton(GLFW_MOUSE_BUTTON_LEFT, false, false, false);
        setMouseButton(GLFW_MOUSE_BUTTON_RIGHT, false, false, false);
    }

    @Test
    @DisplayName("Closing player inventory should drop carried and 2x2 crafting stacks")
    void closeQueuesCursorAndCraftingGridStacksForDrop() {
        Inventory inventory = new Inventory();
        InventoryScreen screen = new InventoryScreen(inventory);
        screen.open(800, 600);

        inventory.getCraftingGrid()[0] = new ItemStack(ItemType.OAK_PLANKS, 2);
        inventory.getCraftingGrid()[3] = new ItemStack(ItemType.STICK, 1);
        inventory.setCursorItem(new ItemStack(ItemType.DIAMOND, 4));

        screen.close();

        assertNull(inventory.getCraftingGrid()[0]);
        assertNull(inventory.getCraftingGrid()[3]);
        assertNull(inventory.getCursorItem());

        List<ItemStack> drops = screen.getAndClearItemsToThrow();
        assertEquals(3, drops.size());
        assertStack(drops.get(0), ItemType.OAK_PLANKS, 2);
        assertStack(drops.get(1), ItemType.STICK, 1);
        assertStack(drops.get(2), ItemType.DIAMOND, 4);
        assertTrue(screen.getAndClearItemsToThrow().isEmpty());
    }

    @Test
    @DisplayName("Shift-clicking 2x2 crafting grid should use hotbar space when main inventory is full")
    void shiftClickCraftingGridUsesWholePlayerInventory() throws Exception {
        Inventory inventory = new Inventory();
        InventoryScreen screen = new InventoryScreen(inventory);
        for (int i = 0; i < inventory.getMainInventory().length; i++) {
            inventory.getMainInventory()[i] = new ItemStack(ItemType.DIRT, ItemType.DIRT.getMaxStackSize());
        }
        inventory.getCraftingGrid()[0] = new ItemStack(ItemType.STICK, 2);

        setKeyDown(GLFW_KEY_LEFT_SHIFT, true);
        try {
            clickSlot(screen, 36, false);
        } finally {
            setKeyDown(GLFW_KEY_LEFT_SHIFT, false);
        }

        assertNull(inventory.getCraftingGrid()[0]);
        assertSame(ItemType.STICK, inventory.getHotbar()[0].getType());
        assertEquals(2, inventory.getHotbar()[0].getCount());
    }

    @Test
    @DisplayName("Player inventory output should preserve dynamic repair durability")
    void playerInventoryOutputUsesStackAwareRepairRecipe() throws Exception {
        Inventory inventory = new Inventory();
        InventoryScreen screen = new InventoryScreen(inventory);
        inventory.getCraftingGrid()[0] = damaged(ItemType.IRON_PICKAXE, 10);
        inventory.getCraftingGrid()[3] = damaged(ItemType.IRON_PICKAXE, 20);

        clickSlot(screen, InventoryScreen.CRAFTING_OUTPUT_SLOT, false);

        ItemStack cursor = inventory.getCursorItem();
        assertNotNull(cursor);
        assertSame(ItemType.IRON_PICKAXE, cursor.getType());
        int expectedDurability = Math.min(ItemType.IRON_PICKAXE.getMaxDurability(),
                10 + 20 + ItemType.IRON_PICKAXE.getMaxDurability() * 5 / 100);
        assertEquals(expectedDurability, cursor.getDurability());
        assertNull(inventory.getCraftingGrid()[0]);
        assertNull(inventory.getCraftingGrid()[3]);
    }

    @Test
    @DisplayName("Player inventory shift-click output should craft repeated results into storage")
    void shiftClickPlayerInventoryOutputCraftsRepeatedResults() throws Exception {
        Inventory inventory = new Inventory();
        InventoryScreen screen = new InventoryScreen(inventory);
        inventory.getCraftingGrid()[0] = new ItemStack(ItemType.OAK_LOG, 3);

        setKeyDown(GLFW_KEY_RIGHT_SHIFT, true);
        clickSlot(screen, InventoryScreen.CRAFTING_OUTPUT_SLOT, false);

        assertNull(inventory.getCraftingGrid()[0]);
        assertStack(inventory.getHotbar()[0], ItemType.OAK_PLANKS, 12);
    }

    @Test
    @DisplayName("Player inventory crafting output should notify crafted result")
    void playerInventoryCraftingOutputNotifiesCraftedResult() throws Exception {
        Inventory inventory = new Inventory();
        InventoryScreen screen = new InventoryScreen(inventory);
        List<ItemType> crafted = new ArrayList<>();
        inventory.setCraftedItemListener(stack -> crafted.add(stack.getType()));
        inventory.getCraftingGrid()[0] = new ItemStack(ItemType.OAK_PLANKS, 1);
        inventory.getCraftingGrid()[1] = new ItemStack(ItemType.OAK_PLANKS, 1);
        inventory.getCraftingGrid()[2] = new ItemStack(ItemType.OAK_PLANKS, 1);
        inventory.getCraftingGrid()[3] = new ItemStack(ItemType.OAK_PLANKS, 1);

        clickSlot(screen, InventoryScreen.CRAFTING_OUTPUT_SLOT, false);

        assertStack(inventory.getCursorItem(), ItemType.CRAFTING_TABLE, 1);
        assertEquals(List.of(ItemType.CRAFTING_TABLE), crafted);
    }

    @Test
    @DisplayName("Release inventory armor slots should use the classic texture coordinates")
    void armorSlotHitDetectionUsesReleaseInventoryCoordinates() throws Exception {
        InventoryScreen screen = new InventoryScreen(new Inventory());
        screen.open(800, 600);

        for (int row = 0; row < InventoryScreen.ARMOR_SLOT_COUNT; row++) {
            int x = screen.getWindowX() + (int) (InventoryScreen.TEX_ARMOR_X * InventoryScreen.GUI_SCALE) + 1;
            int y = screen.getWindowY() + (int) ((InventoryScreen.TEX_ARMOR_Y
                    + row * InventoryScreen.TEX_SLOT_SIZE) * InventoryScreen.GUI_SCALE) + 1;

            assertEquals(InventoryScreen.ARMOR_SLOT_START + row, slotAt(screen, x, y));
        }
    }

    @Test
    @DisplayName("Clicking armor slots should equip only matching armor pieces")
    void armorSlotsAcceptOnlyMatchingArmorPieces() throws Exception {
        Inventory inventory = new Inventory();
        InventoryScreen screen = new InventoryScreen(inventory);

        inventory.setCursorItem(new ItemStack(ItemType.IRON_HELMET, 1));
        clickSlot(screen, InventoryScreen.ARMOR_SLOT_START + ArmorSlot.HELMET.getIndex(), false);

        assertNull(inventory.getCursorItem());
        assertSame(ItemType.IRON_HELMET, inventory.getArmor()[ArmorSlot.HELMET.getIndex()].getType());

        inventory.setCursorItem(new ItemStack(ItemType.IRON_BOOTS, 1));
        clickSlot(screen, InventoryScreen.ARMOR_SLOT_START + ArmorSlot.HELMET.getIndex(), false);

        assertSame(ItemType.IRON_BOOTS, inventory.getCursorItem().getType());
        assertSame(ItemType.IRON_HELMET, inventory.getArmor()[ArmorSlot.HELMET.getIndex()].getType());
    }

    @Test
    @DisplayName("Shift-clicking armor in the player inventory should equip an empty matching slot")
    void shiftClickInventoryArmorEquipsEmptyArmorSlot() throws Exception {
        Inventory inventory = new Inventory();
        InventoryScreen screen = new InventoryScreen(inventory);
        inventory.getMainInventory()[0] = new ItemStack(ItemType.IRON_BOOTS, 1);

        setKeyDown(GLFW_KEY_LEFT_SHIFT, true);
        clickSlot(screen, 0, false);

        assertNull(inventory.getMainInventory()[0]);
        assertSame(ItemType.IRON_BOOTS, inventory.getArmor()[ArmorSlot.BOOTS.getIndex()].getType());
    }

    @Test
    @DisplayName("Shift-clicking occupied armor slots should move armor back to player storage")
    void shiftClickEquippedArmorMovesToPlayerInventory() throws Exception {
        Inventory inventory = new Inventory();
        InventoryScreen screen = new InventoryScreen(inventory);
        inventory.getArmor()[ArmorSlot.CHESTPLATE.getIndex()] = new ItemStack(ItemType.IRON_CHESTPLATE, 1);

        setKeyDown(GLFW_KEY_RIGHT_SHIFT, true);
        clickSlot(screen, InventoryScreen.ARMOR_SLOT_START + ArmorSlot.CHESTPLATE.getIndex(), false);

        assertNull(inventory.getArmor()[ArmorSlot.CHESTPLATE.getIndex()]);
        assertSame(ItemType.IRON_CHESTPLATE, inventory.getHotbar()[0].getType());
    }

    @Test
    @DisplayName("Shift-clicking armor should not overwrite an occupied armor slot")
    void shiftClickArmorFallsBackToStorageWhenArmorSlotOccupied() throws Exception {
        Inventory inventory = new Inventory();
        InventoryScreen screen = new InventoryScreen(inventory);
        inventory.getArmor()[ArmorSlot.BOOTS.getIndex()] = new ItemStack(ItemType.DIAMOND_BOOTS, 1);
        inventory.getMainInventory()[0] = new ItemStack(ItemType.IRON_BOOTS, 1);

        setKeyDown(GLFW_KEY_LEFT_SHIFT, true);
        clickSlot(screen, 0, false);

        assertNull(inventory.getMainInventory()[0]);
        assertSame(ItemType.DIAMOND_BOOTS, inventory.getArmor()[ArmorSlot.BOOTS.getIndex()].getType());
        assertSame(ItemType.IRON_BOOTS, inventory.getHotbar()[0].getType());
    }

    @Test
    @DisplayName("Number keys should swap hovered player inventory slots with matching hotbar slots")
    void numberKeySwapsHoveredMainSlotWithHotbarSlot() throws Exception {
        Inventory inventory = new Inventory();
        InventoryScreen screen = new InventoryScreen(inventory);
        screen.open(800, 600);
        inventory.getMainInventory()[0] = new ItemStack(ItemType.DIRT, 12);
        inventory.getHotbar()[1] = new ItemStack(ItemType.COAL, 3);

        setMouseAtInventorySlot(screen, InventoryScreen.MAIN_SLOT_START);
        ScreenDragTestSupport.pressKey(GLFW_KEY_2);
        screen.update();

        assertStack(inventory.getMainInventory()[0], ItemType.COAL, 3);
        assertStack(inventory.getHotbar()[1], ItemType.DIRT, 12);
    }

    @Test
    @DisplayName("Number keys should obey armor slot restrictions")
    void numberKeyArmorSwapUsesArmorSlotRules() throws Exception {
        Inventory inventory = new Inventory();
        InventoryScreen screen = new InventoryScreen(inventory);
        screen.open(800, 600);
        int helmetSlot = InventoryScreen.ARMOR_SLOT_START + ArmorSlot.HELMET.getIndex();
        inventory.getHotbar()[0] = new ItemStack(ItemType.IRON_BOOTS, 1);

        setMouseAtInventorySlot(screen, helmetSlot);
        ScreenDragTestSupport.pressKey(GLFW_KEY_1);
        screen.update();

        assertNull(inventory.getArmor()[ArmorSlot.HELMET.getIndex()]);
        assertStack(inventory.getHotbar()[0], ItemType.IRON_BOOTS, 1);

        clearHotbarKeys();
        inventory.getHotbar()[0] = new ItemStack(ItemType.IRON_HELMET, 1);
        ScreenDragTestSupport.pressKey(GLFW_KEY_1);
        screen.update();

        assertStack(inventory.getArmor()[ArmorSlot.HELMET.getIndex()], ItemType.IRON_HELMET, 1);
        assertNull(inventory.getHotbar()[0]);
    }

    @Test
    @DisplayName("Left-dragging a carried stack should split it across player inventory slots")
    void leftDragSplitsCursorStackAcrossInventorySlots() throws Exception {
        Inventory inventory = new Inventory();
        InventoryScreen screen = new InventoryScreen(inventory);
        screen.open(800, 600);
        inventory.setCursorItem(new ItemStack(ItemType.COBBLESTONE, 9));

        dragInventorySlots(screen, GLFW_MOUSE_BUTTON_LEFT, 0, 1, 2);

        assertStack(inventory.getMainInventory()[0], ItemType.COBBLESTONE, 3);
        assertStack(inventory.getMainInventory()[1], ItemType.COBBLESTONE, 3);
        assertStack(inventory.getMainInventory()[2], ItemType.COBBLESTONE, 3);
        assertNull(inventory.getCursorItem());
    }

    @Test
    @DisplayName("Right-dragging a carried stack should place one item in each player slot")
    void rightDragPlacesOneItemAcrossInventorySlots() throws Exception {
        Inventory inventory = new Inventory();
        InventoryScreen screen = new InventoryScreen(inventory);
        screen.open(800, 600);
        inventory.setCursorItem(new ItemStack(ItemType.STICK, 5));

        dragInventorySlots(screen, GLFW_MOUSE_BUTTON_RIGHT, 0, 1, 2);

        assertStack(inventory.getMainInventory()[0], ItemType.STICK, 1);
        assertStack(inventory.getMainInventory()[1], ItemType.STICK, 1);
        assertStack(inventory.getMainInventory()[2], ItemType.STICK, 1);
        assertStack(inventory.getCursorItem(), ItemType.STICK, 2);
    }

    @Test
    @DisplayName("Double-clicking with a carried stack should collect matching player inventory stacks")
    void doubleClickCollectsMatchingStacksIntoCursor() throws Exception {
        Inventory inventory = new Inventory();
        InventoryScreen screen = new InventoryScreen(inventory);
        inventory.setCursorItem(new ItemStack(ItemType.COBBLESTONE, 10));
        inventory.getMainInventory()[0] = new ItemStack(ItemType.COBBLESTONE, 12);
        inventory.getMainInventory()[1] = new ItemStack(ItemType.DIRT, 64);
        inventory.getHotbar()[0] = new ItemStack(ItemType.COBBLESTONE, 40);
        inventory.getCraftingGrid()[0] = new ItemStack(ItemType.COBBLESTONE, 8);

        assertTrue(doubleClick(screen, 0));

        assertStack(inventory.getCursorItem(), ItemType.COBBLESTONE, 64);
        assertNull(inventory.getMainInventory()[0]);
        assertStack(inventory.getMainInventory()[1], ItemType.DIRT, 64);
        assertNull(inventory.getHotbar()[0]);
        assertStack(inventory.getCraftingGrid()[0], ItemType.COBBLESTONE, 6);
    }

    @Test
    @DisplayName("A fast second left-click should collect matching stacks through the live input path")
    void updateDoubleClickCollectsMatchingStacksIntoCursor() throws Exception {
        Inventory inventory = new Inventory();
        InventoryScreen screen = new InventoryScreen(inventory);
        screen.open(800, 600);
        inventory.getMainInventory()[0] = new ItemStack(ItemType.COBBLESTONE, 10);
        inventory.getMainInventory()[1] = new ItemStack(ItemType.COBBLESTONE, 20);

        clickInventorySlot(screen, GLFW_MOUSE_BUTTON_LEFT, 0);
        clickInventorySlot(screen, GLFW_MOUSE_BUTTON_LEFT, 0);

        assertStack(inventory.getCursorItem(), ItemType.COBBLESTONE, 30);
        assertNull(inventory.getMainInventory()[0]);
        assertNull(inventory.getMainInventory()[1]);
    }

    @Test
    @DisplayName("A double-click attempt without matches should keep carrying the picked-up stack")
    void updateDoubleClickWithoutMatchesDoesNotPlaceCursorBack() throws Exception {
        Inventory inventory = new Inventory();
        InventoryScreen screen = new InventoryScreen(inventory);
        screen.open(800, 600);
        inventory.getMainInventory()[0] = new ItemStack(ItemType.COBBLESTONE, 10);
        inventory.getMainInventory()[1] = new ItemStack(ItemType.DIRT, 20);

        clickInventorySlot(screen, GLFW_MOUSE_BUTTON_LEFT, 0);
        clickInventorySlot(screen, GLFW_MOUSE_BUTTON_LEFT, 0);

        assertStack(inventory.getCursorItem(), ItemType.COBBLESTONE, 10);
        assertNull(inventory.getMainInventory()[0]);
        assertStack(inventory.getMainInventory()[1], ItemType.DIRT, 20);
    }

    private static void assertStack(ItemStack stack, ItemType type, int count) {
        assertSame(type, stack.getType());
        assertEquals(count, stack.getCount());
    }

    private static ItemStack damaged(ItemType type, int durability) {
        ItemStack stack = new ItemStack(type, 1);
        stack.setDurability(durability);
        return stack;
    }

    private static void clickSlot(InventoryScreen screen, int slot, boolean rightClick) throws Exception {
        Method handleClick = InventoryScreen.class.getDeclaredMethod("handleClick", int.class, boolean.class);
        handleClick.setAccessible(true);
        handleClick.invoke(screen, slot, rightClick);
    }

    private static boolean doubleClick(InventoryScreen screen, int slot) throws Exception {
        Method handleDoubleClick = InventoryScreen.class.getDeclaredMethod("handleDoubleClick", int.class);
        handleDoubleClick.setAccessible(true);
        return (boolean) handleDoubleClick.invoke(screen, slot);
    }

    private static int slotAt(InventoryScreen screen, int x, int y) throws Exception {
        Method getSlotAtPosition = InventoryScreen.class.getDeclaredMethod("getSlotAtPosition", int.class, int.class);
        getSlotAtPosition.setAccessible(true);
        return (int) getSlotAtPosition.invoke(screen, x, y);
    }

    private static void dragInventorySlots(InventoryScreen screen, int button, int... slots) throws Exception {
        setMouseAtInventorySlot(screen, slots[0]);
        setMouseButton(button, true, true, false);
        screen.update();
        for (int i = 1; i < slots.length; i++) {
            setMouseAtInventorySlot(screen, slots[i]);
            setMouseButton(button, true, false, false);
            screen.update();
        }
        setMouseAtInventorySlot(screen, slots[slots.length - 1]);
        setMouseButton(button, false, false, true);
        screen.update();
        setMouseButton(button, false, false, false);
    }

    private static void clickInventorySlot(InventoryScreen screen, int button, int slot) throws Exception {
        setMouseAtInventorySlot(screen, slot);
        setMouseButton(button, true, true, false);
        screen.update();
        setMouseButton(button, false, false, true);
        screen.update();
        setMouseButton(button, false, false, false);
    }

    private static void setMouseAtInventorySlot(InventoryScreen screen, int slot) throws Exception {
        int col;
        int row;
        int texX;
        int texY;
        if (slot >= InventoryScreen.HOTBAR_SLOT_START && slot < InventoryScreen.CRAFTING_SLOT_START) {
            col = slot - InventoryScreen.HOTBAR_SLOT_START;
            texX = InventoryScreen.TEX_HOTBAR_X + col * InventoryScreen.TEX_SLOT_SIZE
                    + InventoryScreen.TEX_SLOT_SIZE / 2;
            texY = InventoryScreen.TEX_HOTBAR_Y + InventoryScreen.TEX_SLOT_SIZE / 2;
        } else if (slot >= InventoryScreen.ARMOR_SLOT_START
                && slot < InventoryScreen.ARMOR_SLOT_START + InventoryScreen.ARMOR_SLOT_COUNT) {
            row = slot - InventoryScreen.ARMOR_SLOT_START;
            texX = InventoryScreen.TEX_ARMOR_X + InventoryScreen.TEX_SLOT_SIZE / 2;
            texY = InventoryScreen.TEX_ARMOR_Y + row * InventoryScreen.TEX_SLOT_SIZE
                    + InventoryScreen.TEX_SLOT_SIZE / 2;
        } else if (slot >= InventoryScreen.CRAFTING_SLOT_START && slot < InventoryScreen.CRAFTING_OUTPUT_SLOT) {
            int craftingSlot = slot - InventoryScreen.CRAFTING_SLOT_START;
            col = craftingSlot % InventoryScreen.CRAFTING_COLS;
            row = craftingSlot / InventoryScreen.CRAFTING_COLS;
            texX = InventoryScreen.TEX_CRAFT_GRID_X + col * InventoryScreen.TEX_SLOT_SIZE
                    + InventoryScreen.TEX_SLOT_SIZE / 2;
            texY = InventoryScreen.TEX_CRAFT_GRID_Y + row * InventoryScreen.TEX_SLOT_SIZE
                    + InventoryScreen.TEX_SLOT_SIZE / 2;
        } else {
            col = slot % InventoryScreen.COLS;
            row = slot / InventoryScreen.COLS;
            texX = InventoryScreen.TEX_MAIN_INV_X + col * InventoryScreen.TEX_SLOT_SIZE
                    + InventoryScreen.TEX_SLOT_SIZE / 2;
            texY = InventoryScreen.TEX_MAIN_INV_Y + row * InventoryScreen.TEX_SLOT_SIZE
                    + InventoryScreen.TEX_SLOT_SIZE / 2;
        }
        setMousePosition(screen.getWindowX() + texX * InventoryScreen.GUI_SCALE,
                screen.getWindowY() + texY * InventoryScreen.GUI_SCALE);
    }

    private static void setKeyDown(int key, boolean down) throws Exception {
        Field keys = com.craftzero.engine.Input.class.getDeclaredField("keys");
        keys.setAccessible(true);
        ((boolean[]) keys.get(null))[key] = down;
    }

    private static void setMouseButton(int button, boolean down, boolean pressed, boolean released) throws Exception {
        setInputButtonArray("buttons", button, down);
        setInputButtonArray("buttonsPressed", button, pressed);
        setInputButtonArray("buttonsReleased", button, released);
    }

    private static void setInputButtonArray(String fieldName, int button, boolean value) throws Exception {
        Field field = com.craftzero.engine.Input.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        ((boolean[]) field.get(null))[button] = value;
    }

    private static void setMousePosition(double x, double y) throws Exception {
        Field mouseX = com.craftzero.engine.Input.class.getDeclaredField("mouseX");
        Field mouseY = com.craftzero.engine.Input.class.getDeclaredField("mouseY");
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
