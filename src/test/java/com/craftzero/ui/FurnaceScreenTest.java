package com.craftzero.ui;

import com.craftzero.inventory.Inventory;
import com.craftzero.inventory.ItemStack;
import com.craftzero.inventory.ItemType;
import com.craftzero.main.Player;
import com.craftzero.world.BlockType;
import com.craftzero.world.World;
import com.craftzero.world.tile.FurnaceTileEntity;
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

class FurnaceScreenTest {
    @AfterEach
    void clearMouseButtons() throws Exception {
        ScreenDragTestSupport.clearMouseButtons(GLFW_MOUSE_BUTTON_LEFT, GLFW_MOUSE_BUTTON_RIGHT);
        clearHotbarKeys();
    }

    @Test
    @DisplayName("Furnace output slot should never accept cursor items")
    void outputSlotOnlyMovesItemsOut() throws Exception {
        Inventory inventory = new Inventory();
        FurnaceTileEntity furnace = new FurnaceTileEntity(0, 70, 0);
        FurnaceScreen screen = boundScreen(inventory, furnace);

        furnace.getInventory()[FurnaceTileEntity.SLOT_OUTPUT] = new ItemStack(ItemType.IRON_INGOT, 1);
        inventory.setCursorItem(new ItemStack(ItemType.IRON_INGOT, 3));

        clickOutput(screen, false);

        assertEquals(4, inventory.getCursorItem().getCount());
        assertSame(ItemType.IRON_INGOT, inventory.getCursorItem().getType());
        assertNull(furnace.getInventory()[FurnaceTileEntity.SLOT_OUTPUT]);

        furnace.getInventory()[FurnaceTileEntity.SLOT_OUTPUT] = new ItemStack(ItemType.IRON_INGOT, 1);
        inventory.setCursorItem(new ItemStack(ItemType.GOLD_INGOT, 3));

        clickOutput(screen, false);

        assertEquals(3, inventory.getCursorItem().getCount());
        assertSame(ItemType.GOLD_INGOT, inventory.getCursorItem().getType());
        assertEquals(1, furnace.getInventory()[FurnaceTileEntity.SLOT_OUTPUT].getCount());
        assertSame(ItemType.IRON_INGOT, furnace.getInventory()[FurnaceTileEntity.SLOT_OUTPUT].getType());

        furnace.getInventory()[FurnaceTileEntity.SLOT_OUTPUT] = new ItemStack(ItemType.IRON_INGOT, 4);
        inventory.setCursorItem(null);

        clickOutput(screen, true);

        assertEquals(2, inventory.getCursorItem().getCount());
        assertSame(ItemType.IRON_INGOT, inventory.getCursorItem().getType());
        assertEquals(2, furnace.getInventory()[FurnaceTileEntity.SLOT_OUTPUT].getCount());
    }

    @Test
    @DisplayName("Furnace shift-click should route smeltable fuel items into input first")
    void shiftClickRoutesSmeltableFuelToInputBeforeFuelSlot() throws Exception {
        Inventory inventory = new Inventory();
        FurnaceTileEntity furnace = new FurnaceTileEntity(0, 70, 0);
        FurnaceScreen screen = boundScreen(inventory, furnace);

        inventory.getMainInventory()[0] = new ItemStack(ItemType.OAK_LOG, 1);
        shiftClick(screen, FurnaceTileEntity.SIZE, inventory.getMainInventory()[0]);

        assertNull(inventory.getMainInventory()[0]);
        assertSame(ItemType.OAK_LOG, furnace.getInventory()[FurnaceTileEntity.SLOT_INPUT].getType());
        assertNull(furnace.getInventory()[FurnaceTileEntity.SLOT_FUEL]);

        inventory.getMainInventory()[1] = new ItemStack(ItemType.COAL, 1);
        shiftClick(screen, FurnaceTileEntity.SIZE + 1, inventory.getMainInventory()[1]);

        assertNull(inventory.getMainInventory()[1]);
        assertSame(ItemType.COAL, furnace.getInventory()[FurnaceTileEntity.SLOT_FUEL].getType());
    }

    @Test
    @DisplayName("Furnace shift-click should move ordinary player items between main inventory and hotbar")
    void shiftClickMovesOrdinaryPlayerItemsBetweenMainAndHotbar() throws Exception {
        Inventory inventory = new Inventory();
        FurnaceTileEntity furnace = new FurnaceTileEntity(0, 70, 0);
        FurnaceScreen screen = boundScreen(inventory, furnace);

        inventory.getMainInventory()[0] = new ItemStack(ItemType.DIRT, 2);
        shiftClick(screen, FurnaceTileEntity.SIZE, inventory.getMainInventory()[0]);

        assertNull(inventory.getMainInventory()[0]);
        assertSame(ItemType.DIRT, inventory.getHotbar()[0].getType());
        assertEquals(2, inventory.getHotbar()[0].getCount());

        inventory.getHotbar()[1] = new ItemStack(ItemType.GRAVEL, 3);
        shiftClick(screen, FurnaceTileEntity.SIZE + Inventory.MAIN_SIZE + 1, inventory.getHotbar()[1]);

        assertNull(inventory.getHotbar()[1]);
        assertSame(ItemType.GRAVEL, inventory.getMainInventory()[0].getType());
        assertEquals(3, inventory.getMainInventory()[0].getCount());
    }

    @Test
    @DisplayName("Furnace direct placement should accept any input but only fuel in fuel slot")
    void furnaceDirectPlacementAcceptsAnyInputAndOnlyFuel() throws Exception {
        Inventory inventory = new Inventory();
        FurnaceTileEntity furnace = new FurnaceTileEntity(0, 70, 0);
        FurnaceScreen screen = boundScreen(inventory, furnace);

        inventory.setCursorItem(new ItemStack(ItemType.DIRT, 1));
        clickSlot(screen, FurnaceTileEntity.SLOT_INPUT, false);

        assertSame(ItemType.DIRT, furnace.getInventory()[FurnaceTileEntity.SLOT_INPUT].getType());
        assertNull(inventory.getCursorItem());

        inventory.setCursorItem(new ItemStack(ItemType.IRON_ORE, 1));
        clickSlot(screen, FurnaceTileEntity.SLOT_INPUT, false);

        assertSame(ItemType.IRON_ORE, furnace.getInventory()[FurnaceTileEntity.SLOT_INPUT].getType());
        assertSame(ItemType.DIRT, inventory.getCursorItem().getType());

        inventory.setCursorItem(new ItemStack(ItemType.IRON_ORE, 1));
        clickSlot(screen, FurnaceTileEntity.SLOT_FUEL, false);

        assertNull(furnace.getInventory()[FurnaceTileEntity.SLOT_FUEL]);
        assertSame(ItemType.IRON_ORE, inventory.getCursorItem().getType());

        inventory.setCursorItem(new ItemStack(ItemType.COAL, 1));
        clickSlot(screen, FurnaceTileEntity.SLOT_FUEL, false);

        assertSame(ItemType.COAL, furnace.getInventory()[FurnaceTileEntity.SLOT_FUEL].getType());
        assertNull(inventory.getCursorItem());
    }

    @Test
    @DisplayName("Number keys should obey furnace fuel and output slot rules")
    void numberKeySwapObeysFurnaceSlotRules() throws Exception {
        Inventory inventory = new Inventory();
        FurnaceTileEntity furnace = new FurnaceTileEntity(0, 70, 0);
        FurnaceScreen screen = new FurnaceScreen(inventory);
        screen.open(furnace, 800, 600);
        furnace.getInventory()[FurnaceTileEntity.SLOT_FUEL] = new ItemStack(ItemType.COAL, 3);
        inventory.getHotbar()[0] = new ItemStack(ItemType.DIRT, 1);

        double[] fuelPoint = furnaceSlotCenter(screen, FurnaceTileEntity.SLOT_FUEL);
        ScreenDragTestSupport.setMousePosition(fuelPoint[0], fuelPoint[1]);
        ScreenDragTestSupport.pressKey(GLFW_KEY_1);
        screen.update();

        assertStack(furnace.getInventory()[FurnaceTileEntity.SLOT_FUEL], ItemType.COAL, 3);
        assertStack(inventory.getHotbar()[0], ItemType.DIRT, 1);

        clearHotbarKeys();
        inventory.getHotbar()[0] = null;
        ScreenDragTestSupport.pressKey(GLFW_KEY_1);
        screen.update();

        assertNull(furnace.getInventory()[FurnaceTileEntity.SLOT_FUEL]);
        assertStack(inventory.getHotbar()[0], ItemType.COAL, 3);

        clearHotbarKeys();
        furnace.getInventory()[FurnaceTileEntity.SLOT_OUTPUT] = new ItemStack(ItemType.IRON_INGOT, 1);
        inventory.getHotbar()[0] = null;
        double[] outputPoint = furnaceSlotCenter(screen, FurnaceTileEntity.SLOT_OUTPUT);
        ScreenDragTestSupport.setMousePosition(outputPoint[0], outputPoint[1]);
        ScreenDragTestSupport.pressKey(GLFW_KEY_1);
        screen.update();

        assertStack(furnace.getInventory()[FurnaceTileEntity.SLOT_OUTPUT], ItemType.IRON_INGOT, 1);
        assertNull(inventory.getHotbar()[0]);

        screen.close();
    }

    @Test
    @DisplayName("Furnace drag should respect fuel and output slot placement rules")
    void dragRespectsFurnaceFuelAndOutputSlotRules() throws Exception {
        Inventory inventory = new Inventory();
        FurnaceTileEntity furnace = new FurnaceTileEntity(0, 70, 0);
        FurnaceScreen screen = new FurnaceScreen(inventory);
        screen.open(furnace, 800, 600);
        inventory.setCursorItem(new ItemStack(ItemType.COAL, 3));

        ScreenDragTestSupport.drag(GLFW_MOUSE_BUTTON_RIGHT, screen::update,
                furnaceSlotCenter(screen, FurnaceTileEntity.SLOT_FUEL),
                furnaceSlotCenter(screen, FurnaceTileEntity.SLOT_OUTPUT),
                furnaceSlotCenter(screen, FurnaceTileEntity.SIZE));

        assertStack(furnace.getInventory()[FurnaceTileEntity.SLOT_FUEL], ItemType.COAL, 1);
        assertNull(furnace.getInventory()[FurnaceTileEntity.SLOT_OUTPUT]);
        assertStack(inventory.getMainInventory()[0], ItemType.COAL, 1);
        assertStack(inventory.getCursorItem(), ItemType.COAL, 1);

        screen.close();
    }

    @Test
    @DisplayName("Furnace double-click should collect input/fuel/player stacks but skip output")
    void doubleClickCollectsFurnaceStacksButSkipsOutput() throws Exception {
        Inventory inventory = new Inventory();
        FurnaceTileEntity furnace = new FurnaceTileEntity(0, 70, 0);
        FurnaceScreen screen = boundScreen(inventory, furnace);
        inventory.setCursorItem(new ItemStack(ItemType.COAL, 10));
        furnace.getInventory()[FurnaceTileEntity.SLOT_INPUT] = new ItemStack(ItemType.DIRT, 1);
        furnace.getInventory()[FurnaceTileEntity.SLOT_FUEL] = new ItemStack(ItemType.COAL, 20);
        furnace.getInventory()[FurnaceTileEntity.SLOT_OUTPUT] = new ItemStack(ItemType.COAL, 20);
        inventory.getMainInventory()[0] = new ItemStack(ItemType.COAL, 40);

        assertTrue(doubleClick(screen, FurnaceTileEntity.SLOT_FUEL));

        assertStack(inventory.getCursorItem(), ItemType.COAL, 64);
        assertStack(furnace.getInventory()[FurnaceTileEntity.SLOT_INPUT], ItemType.DIRT, 1);
        assertNull(furnace.getInventory()[FurnaceTileEntity.SLOT_FUEL]);
        assertStack(furnace.getInventory()[FurnaceTileEntity.SLOT_OUTPUT], ItemType.COAL, 20);
        assertStack(inventory.getMainInventory()[0], ItemType.COAL, 6);
    }

    @Test
    @DisplayName("Furnace screen should only stay usable for the same nearby furnace tile")
    void furnaceScreenRequiresSameNearbyFurnaceTile() {
        World world = new World(8132L);
        try {
            world.setBlock(0, 70, 0, BlockType.FURNACE);
            FurnaceTileEntity furnace = (FurnaceTileEntity) world.getTileEntity(0, 70, 0);
            Player player = new Player(0.0f, 70.0f, 0.0f);
            FurnaceScreen screen = new FurnaceScreen(player.getInventory());
            screen.open(furnace, 800, 600);

            assertTrue(screen.isStillUsable(world, player));

            world.setBlockPreservingTile(0, 70, 0, BlockType.LIT_FURNACE, 0);
            assertTrue(screen.isStillUsable(world, player));

            player.getPosition().set(9.0f, 70.0f, 0.0f);
            assertFalse(screen.isStillUsable(world, player));

            player.getPosition().set(0.0f, 70.0f, 0.0f);
            world.setBlock(0, 70, 0, BlockType.AIR);
            assertFalse(screen.isStillUsable(world, player));

            world.setBlock(0, 70, 0, BlockType.FURNACE);
            assertFalse(screen.isStillUsable(world, player));
            screen.close();
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Furnace progress overlays should use Release 1.0 integer GUI geometry")
    void furnaceProgressOverlaysUseReleaseIntegerGeometry() {
        FurnaceTileEntity furnace = new FurnaceTileEntity(0, 70, 0);

        assertNull(FurnaceScreen.getBurnFlameOverlay(furnace));
        assertEquals(new FurnaceScreen.ProgressOverlay(79, 34, 176, 14, 1, 16),
                FurnaceScreen.getCookArrowOverlay(furnace));

        furnace.setCurrentFuelBurnTime(1600);
        furnace.setBurnTime(800);
        furnace.setCookTime(100);

        assertEquals(new FurnaceScreen.ProgressOverlay(56, 42, 176, 6, 14, 8),
                FurnaceScreen.getBurnFlameOverlay(furnace));
        assertEquals(new FurnaceScreen.ProgressOverlay(79, 34, 176, 14, 13, 16),
                FurnaceScreen.getCookArrowOverlay(furnace));
    }

    @Test
    @DisplayName("Furnace flame overlay should keep the Release 1.0 two-pixel ember")
    void furnaceFlameOverlayKeepsLastBurnTickEmber() {
        FurnaceTileEntity furnace = new FurnaceTileEntity(0, 70, 0);
        furnace.setCurrentFuelBurnTime(200);
        furnace.setBurnTime(1);

        assertEquals(new FurnaceScreen.ProgressOverlay(56, 48, 176, 12, 14, 2),
                FurnaceScreen.getBurnFlameOverlay(furnace));

        furnace.setCurrentFuelBurnTime(0);
        furnace.setBurnTime(100);

        assertEquals(new FurnaceScreen.ProgressOverlay(56, 42, 176, 6, 14, 8),
                FurnaceScreen.getBurnFlameOverlay(furnace));
    }

    private static FurnaceScreen boundScreen(Inventory inventory, FurnaceTileEntity furnace) throws Exception {
        FurnaceScreen screen = new FurnaceScreen(inventory);
        Field furnaceField = FurnaceScreen.class.getDeclaredField("furnace");
        furnaceField.setAccessible(true);
        furnaceField.set(screen, furnace);
        return screen;
    }

    private static void clickOutput(FurnaceScreen screen, boolean rightClick) throws Exception {
        clickSlot(screen, FurnaceTileEntity.SLOT_OUTPUT, rightClick);
    }

    private static void clickSlot(FurnaceScreen screen, int slot, boolean rightClick) throws Exception {
        Method handleClick = FurnaceScreen.class.getDeclaredMethod("handleClick", int.class, boolean.class);
        handleClick.setAccessible(true);
        handleClick.invoke(screen, slot, rightClick);
    }

    private static boolean doubleClick(FurnaceScreen screen, int slot) throws Exception {
        Method handleDoubleClick = FurnaceScreen.class.getDeclaredMethod("handleDoubleClick", int.class);
        handleDoubleClick.setAccessible(true);
        return (boolean) handleDoubleClick.invoke(screen, slot);
    }

    private static void shiftClick(FurnaceScreen screen, int slot, ItemStack stack) throws Exception {
        Method shiftClick = FurnaceScreen.class.getDeclaredMethod("shiftClick", int.class, ItemStack.class);
        shiftClick.setAccessible(true);
        shiftClick.invoke(screen, slot, stack);
    }

    private static double[] furnaceSlotCenter(FurnaceScreen screen, int slot) {
        int texX;
        int texY;
        if (slot == FurnaceTileEntity.SLOT_INPUT) {
            texX = FurnaceScreen.TEX_INPUT_X;
            texY = FurnaceScreen.TEX_INPUT_Y;
        } else if (slot == FurnaceTileEntity.SLOT_FUEL) {
            texX = FurnaceScreen.TEX_FUEL_X;
            texY = FurnaceScreen.TEX_FUEL_Y;
        } else if (slot == FurnaceTileEntity.SLOT_OUTPUT) {
            texX = FurnaceScreen.TEX_OUTPUT_X;
            texY = FurnaceScreen.TEX_OUTPUT_Y;
        } else {
            int playerIndex = slot - FurnaceTileEntity.SIZE;
            int col = playerIndex % FurnaceScreen.COLS;
            int row = playerIndex / FurnaceScreen.COLS;
            texX = FurnaceScreen.TEX_MAIN_INV_X + col * FurnaceScreen.TEX_SLOT_SIZE;
            texY = FurnaceScreen.TEX_MAIN_INV_Y + row * FurnaceScreen.TEX_SLOT_SIZE;
        }
        double x = screen.getWindowX() + (texX + FurnaceScreen.TEX_SLOT_SIZE / 2.0) * FurnaceScreen.GUI_SCALE;
        double y = screen.getWindowY() + (texY + FurnaceScreen.TEX_SLOT_SIZE / 2.0) * FurnaceScreen.GUI_SCALE;
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
