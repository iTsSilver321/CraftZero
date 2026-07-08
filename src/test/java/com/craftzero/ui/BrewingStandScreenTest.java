package com.craftzero.ui;

import com.craftzero.inventory.Inventory;
import com.craftzero.inventory.ItemStack;
import com.craftzero.inventory.ItemType;
import com.craftzero.main.Player;
import com.craftzero.progression.AchievementTracker;
import com.craftzero.progression.AchievementType;
import com.craftzero.progression.PotionData;
import com.craftzero.progression.PotionType;
import com.craftzero.world.BlockType;
import com.craftzero.world.World;
import com.craftzero.world.tile.BrewingStandTileEntity;
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

class BrewingStandScreenTest {
    @AfterEach
    void clearMouseButtons() throws Exception {
        ScreenDragTestSupport.clearMouseButtons(GLFW_MOUSE_BUTTON_LEFT, GLFW_MOUSE_BUTTON_RIGHT);
        clearHotbarKeys();
    }

    @Test
    @DisplayName("Brewing stand slots should reject invalid direct placement and merge")
    void brewingSlotsRejectInvalidDirectPlacementAndMerge() throws Exception {
        Inventory inventory = new Inventory();
        BrewingStandTileEntity brewingStand = new BrewingStandTileEntity(0, 70, 0);
        BrewingStandScreen screen = boundScreen(inventory, brewingStand);

        inventory.setCursorItem(new ItemStack(ItemType.DIRT, 1));
        clickSlot(screen, BrewingStandTileEntity.SLOT_INGREDIENT, false);

        assertNull(brewingStand.getInventory()[BrewingStandTileEntity.SLOT_INGREDIENT]);
        assertSame(ItemType.DIRT, inventory.getCursorItem().getType());

        brewingStand.getInventory()[BrewingStandTileEntity.SLOT_INGREDIENT] = new ItemStack(ItemType.DIRT, 1);
        inventory.setCursorItem(new ItemStack(ItemType.DIRT, 3));

        clickSlot(screen, BrewingStandTileEntity.SLOT_INGREDIENT, false);

        assertEquals(1, brewingStand.getInventory()[BrewingStandTileEntity.SLOT_INGREDIENT].getCount());
        assertEquals(3, inventory.getCursorItem().getCount());

        clickSlot(screen, BrewingStandTileEntity.SLOT_INGREDIENT, true);

        assertEquals(1, brewingStand.getInventory()[BrewingStandTileEntity.SLOT_INGREDIENT].getCount());
        assertEquals(3, inventory.getCursorItem().getCount());

        brewingStand.getInventory()[BrewingStandTileEntity.SLOT_INGREDIENT] = null;
        inventory.setCursorItem(new ItemStack(ItemType.NETHER_WART, 1));

        clickSlot(screen, BrewingStandTileEntity.SLOT_INGREDIENT, false);

        assertSame(ItemType.NETHER_WART,
                brewingStand.getInventory()[BrewingStandTileEntity.SLOT_INGREDIENT].getType());
        assertNull(inventory.getCursorItem());
    }

    @Test
    @DisplayName("Brewing bottle slots should accept one glass bottle at a time")
    void brewingBottleSlotsAcceptSingleGlassBottles() throws Exception {
        Inventory inventory = new Inventory();
        BrewingStandTileEntity brewingStand = new BrewingStandTileEntity(0, 70, 0);
        BrewingStandScreen screen = boundScreen(inventory, brewingStand);

        inventory.setCursorItem(new ItemStack(ItemType.GLASS_BOTTLE, 3));
        clickSlot(screen, BrewingStandTileEntity.SLOT_BOTTLE_0, false);

        assertSame(ItemType.GLASS_BOTTLE, brewingStand.getInventory()[BrewingStandTileEntity.SLOT_BOTTLE_0].getType());
        assertEquals(1, brewingStand.getInventory()[BrewingStandTileEntity.SLOT_BOTTLE_0].getCount());
        assertEquals(2, inventory.getCursorItem().getCount());

        clickSlot(screen, BrewingStandTileEntity.SLOT_BOTTLE_0, false);

        assertEquals(1, brewingStand.getInventory()[BrewingStandTileEntity.SLOT_BOTTLE_0].getCount());
        assertEquals(2, inventory.getCursorItem().getCount());

        clickSlot(screen, BrewingStandTileEntity.SLOT_BOTTLE_1, true);

        assertSame(ItemType.GLASS_BOTTLE, brewingStand.getInventory()[BrewingStandTileEntity.SLOT_BOTTLE_1].getType());
        assertEquals(1, brewingStand.getInventory()[BrewingStandTileEntity.SLOT_BOTTLE_1].getCount());
        assertEquals(1, inventory.getCursorItem().getCount());
    }

    @Test
    @DisplayName("Number keys should respect brewing bottle slot caps")
    void numberKeySwapRespectsBrewingBottleSlotCap() throws Exception {
        Inventory inventory = new Inventory();
        BrewingStandTileEntity brewingStand = new BrewingStandTileEntity(0, 70, 0);
        BrewingStandScreen screen = new BrewingStandScreen(inventory);
        screen.open(brewingStand, 800, 600);
        inventory.getHotbar()[0] = new ItemStack(ItemType.GLASS_BOTTLE, 2);

        double[] point = brewingSlotCenter(screen, BrewingStandTileEntity.SLOT_BOTTLE_0);
        ScreenDragTestSupport.setMousePosition(point[0], point[1]);
        ScreenDragTestSupport.pressKey(GLFW_KEY_1);
        screen.update();

        assertNull(brewingStand.getInventory()[BrewingStandTileEntity.SLOT_BOTTLE_0]);
        assertStack(inventory.getHotbar()[0], ItemType.GLASS_BOTTLE, 2);

        clearHotbarKeys();
        inventory.getHotbar()[0] = new ItemStack(ItemType.GLASS_BOTTLE, 1);
        ScreenDragTestSupport.pressKey(GLFW_KEY_1);
        screen.update();

        assertStack(brewingStand.getInventory()[BrewingStandTileEntity.SLOT_BOTTLE_0], ItemType.GLASS_BOTTLE, 1);
        assertNull(inventory.getHotbar()[0]);

        screen.close();
    }

    @Test
    @DisplayName("Brewing drag should place one bottle in each bottle slot")
    void rightDragPlacesOneBottleInEachBottleSlot() throws Exception {
        Inventory inventory = new Inventory();
        BrewingStandTileEntity brewingStand = new BrewingStandTileEntity(0, 70, 0);
        BrewingStandScreen screen = new BrewingStandScreen(inventory);
        screen.open(brewingStand, 800, 600);
        inventory.setCursorItem(new ItemStack(ItemType.GLASS_BOTTLE, 5));

        ScreenDragTestSupport.drag(GLFW_MOUSE_BUTTON_RIGHT, screen::update,
                brewingSlotCenter(screen, BrewingStandTileEntity.SLOT_BOTTLE_0),
                brewingSlotCenter(screen, BrewingStandTileEntity.SLOT_BOTTLE_1),
                brewingSlotCenter(screen, BrewingStandTileEntity.SLOT_BOTTLE_2));

        assertStack(brewingStand.getInventory()[BrewingStandTileEntity.SLOT_BOTTLE_0], ItemType.GLASS_BOTTLE, 1);
        assertStack(brewingStand.getInventory()[BrewingStandTileEntity.SLOT_BOTTLE_1], ItemType.GLASS_BOTTLE, 1);
        assertStack(brewingStand.getInventory()[BrewingStandTileEntity.SLOT_BOTTLE_2], ItemType.GLASS_BOTTLE, 1);
        assertStack(inventory.getCursorItem(), ItemType.GLASS_BOTTLE, 2);

        screen.close();
    }

    @Test
    @DisplayName("Brewing double-click should collect matching ingredient stacks")
    void doubleClickCollectsMatchingIngredientStacks() throws Exception {
        Inventory inventory = new Inventory();
        BrewingStandTileEntity brewingStand = new BrewingStandTileEntity(0, 70, 0);
        BrewingStandScreen screen = boundScreen(inventory, brewingStand);
        inventory.setCursorItem(new ItemStack(ItemType.NETHER_WART, 10));
        brewingStand.getInventory()[BrewingStandTileEntity.SLOT_INGREDIENT] =
                new ItemStack(ItemType.NETHER_WART, 20);
        brewingStand.getInventory()[BrewingStandTileEntity.SLOT_BOTTLE_0] =
                new ItemStack(ItemType.GLASS_BOTTLE, 1);
        inventory.getMainInventory()[0] = new ItemStack(ItemType.NETHER_WART, 40);

        assertTrue(doubleClick(screen, BrewingStandTileEntity.SLOT_INGREDIENT));

        assertStack(inventory.getCursorItem(), ItemType.NETHER_WART, 64);
        assertNull(brewingStand.getInventory()[BrewingStandTileEntity.SLOT_INGREDIENT]);
        assertStack(brewingStand.getInventory()[BrewingStandTileEntity.SLOT_BOTTLE_0], ItemType.GLASS_BOTTLE, 1);
        assertStack(inventory.getMainInventory()[0], ItemType.NETHER_WART, 6);
    }

    @Test
    @DisplayName("Brewing shift-click should route bottles, ingredients, and ordinary fallback like the source container")
    void shiftClickRoutesBrewingStacksLikeSourceContainer() throws Exception {
        Inventory inventory = new Inventory();
        BrewingStandTileEntity brewingStand = new BrewingStandTileEntity(0, 70, 0);
        BrewingStandScreen screen = boundScreen(inventory, brewingStand);

        inventory.getMainInventory()[0] = new ItemStack(ItemType.GLASS_BOTTLE, 5);
        shiftClick(screen, BrewingStandTileEntity.SIZE, inventory.getMainInventory()[0]);

        for (int i = 0; i < 3; i++) {
            assertSame(ItemType.GLASS_BOTTLE, brewingStand.getInventory()[i].getType());
            assertEquals(1, brewingStand.getInventory()[i].getCount());
        }
        assertEquals(2, inventory.getMainInventory()[0].getCount());

        inventory.getMainInventory()[1] = new ItemStack(ItemType.NETHER_WART, 4);
        shiftClick(screen, BrewingStandTileEntity.SIZE + 1, inventory.getMainInventory()[1]);

        assertNull(inventory.getMainInventory()[1]);
        assertSame(ItemType.NETHER_WART,
                brewingStand.getInventory()[BrewingStandTileEntity.SLOT_INGREDIENT].getType());
        assertEquals(4, brewingStand.getInventory()[BrewingStandTileEntity.SLOT_INGREDIENT].getCount());

        inventory.getMainInventory()[2] = new ItemStack(ItemType.SUGAR, 2);
        shiftClick(screen, BrewingStandTileEntity.SIZE + 2, inventory.getMainInventory()[2]);

        assertNull(inventory.getMainInventory()[2]);
        assertSame(ItemType.SUGAR, inventory.getHotbar()[0].getType());
        assertEquals(2, inventory.getHotbar()[0].getCount());
        assertSame(ItemType.NETHER_WART,
                brewingStand.getInventory()[BrewingStandTileEntity.SLOT_INGREDIENT].getType());
    }

    @Test
    @DisplayName("Taking a brewed potion from a bottle slot should unlock Local Brewery")
    void takingBrewedPotionUnlocksLocalBrewery() throws Exception {
        Inventory inventory = new Inventory();
        BrewingStandTileEntity brewingStand = new BrewingStandTileEntity(0, 70, 0);
        BrewingStandScreen screen = boundScreen(inventory, brewingStand);
        AchievementTracker tracker = unlockedIntoFireTracker();
        screen.setAchievementTracker(tracker);
        brewingStand.getInventory()[BrewingStandTileEntity.SLOT_BOTTLE_0] =
                potion(PotionType.AWKWARD);

        clickSlot(screen, BrewingStandTileEntity.SLOT_BOTTLE_0, false);

        assertTrue(tracker.isUnlocked(AchievementType.LOCAL_BREWERY));
        assertStack(inventory.getCursorItem(), ItemType.POTION, 1);
        assertEquals(PotionType.AWKWARD, inventory.getCursorItem().getPotionData().type());
        assertNull(brewingStand.getInventory()[BrewingStandTileEntity.SLOT_BOTTLE_0]);
    }

    @Test
    @DisplayName("Taking a water potion from a bottle slot should not unlock Local Brewery")
    void takingWaterPotionDoesNotUnlockLocalBrewery() throws Exception {
        Inventory inventory = new Inventory();
        BrewingStandTileEntity brewingStand = new BrewingStandTileEntity(0, 70, 0);
        BrewingStandScreen screen = boundScreen(inventory, brewingStand);
        AchievementTracker tracker = unlockedIntoFireTracker();
        screen.setAchievementTracker(tracker);
        brewingStand.getInventory()[BrewingStandTileEntity.SLOT_BOTTLE_0] =
                potion(PotionType.WATER);

        clickSlot(screen, BrewingStandTileEntity.SLOT_BOTTLE_0, false);

        assertFalse(tracker.isUnlocked(AchievementType.LOCAL_BREWERY));
        assertStack(inventory.getCursorItem(), ItemType.POTION, 1);
    }

    @Test
    @DisplayName("Brewing stand screen should only stay usable for the same nearby brewing tile")
    void brewingStandScreenRequiresSameNearbyBrewingTile() {
        World world = new World(8134L);
        try {
            world.setBlock(0, 70, 0, BlockType.BREWING_STAND);
            BrewingStandTileEntity brewingStand = (BrewingStandTileEntity) world.getTileEntity(0, 70, 0);
            Player player = new Player(0.0f, 70.0f, 0.0f);
            BrewingStandScreen screen = new BrewingStandScreen(player.getInventory());
            screen.open(brewingStand, 800, 600);

            assertTrue(screen.isStillUsable(world, player));

            player.getPosition().set(9.0f, 70.0f, 0.0f);
            assertFalse(screen.isStillUsable(world, player));

            player.getPosition().set(0.0f, 70.0f, 0.0f);
            world.setBlock(0, 70, 0, BlockType.AIR);
            assertFalse(screen.isStillUsable(world, player));

            world.setBlock(0, 70, 0, BlockType.BREWING_STAND);
            assertFalse(screen.isStillUsable(world, player));
            screen.close();
        } finally {
            world.cleanup();
        }
    }

    private static BrewingStandScreen boundScreen(Inventory inventory, BrewingStandTileEntity brewingStand)
            throws Exception {
        BrewingStandScreen screen = new BrewingStandScreen(inventory);
        Field brewingStandField = BrewingStandScreen.class.getDeclaredField("brewingStand");
        brewingStandField.setAccessible(true);
        brewingStandField.set(screen, brewingStand);
        return screen;
    }

    private static void clickSlot(BrewingStandScreen screen, int slot, boolean rightClick) throws Exception {
        Method handleClick = BrewingStandScreen.class.getDeclaredMethod("handleClick", int.class, boolean.class);
        handleClick.setAccessible(true);
        handleClick.invoke(screen, slot, rightClick);
    }

    private static boolean doubleClick(BrewingStandScreen screen, int slot) throws Exception {
        Method handleDoubleClick = BrewingStandScreen.class.getDeclaredMethod("handleDoubleClick", int.class);
        handleDoubleClick.setAccessible(true);
        return (boolean) handleDoubleClick.invoke(screen, slot);
    }

    private static void shiftClick(BrewingStandScreen screen, int slot, ItemStack stack) throws Exception {
        Method shiftClick = BrewingStandScreen.class.getDeclaredMethod("shiftClick", int.class, ItemStack.class);
        shiftClick.setAccessible(true);
        shiftClick.invoke(screen, slot, stack);
    }

    private static double[] brewingSlotCenter(BrewingStandScreen screen, int slot) {
        int texX;
        int texY;
        if (slot == BrewingStandTileEntity.SLOT_BOTTLE_0) {
            texX = BrewingStandScreen.TEX_BOTTLE_0_X;
            texY = BrewingStandScreen.TEX_BOTTLE_0_Y;
        } else if (slot == BrewingStandTileEntity.SLOT_BOTTLE_1) {
            texX = BrewingStandScreen.TEX_BOTTLE_1_X;
            texY = BrewingStandScreen.TEX_BOTTLE_1_Y;
        } else if (slot == BrewingStandTileEntity.SLOT_BOTTLE_2) {
            texX = BrewingStandScreen.TEX_BOTTLE_2_X;
            texY = BrewingStandScreen.TEX_BOTTLE_2_Y;
        } else if (slot == BrewingStandTileEntity.SLOT_INGREDIENT) {
            texX = BrewingStandScreen.TEX_INGREDIENT_X;
            texY = BrewingStandScreen.TEX_INGREDIENT_Y;
        } else {
            int playerIndex = slot - BrewingStandTileEntity.SIZE;
            int col = playerIndex % BrewingStandScreen.COLS;
            int row = playerIndex / BrewingStandScreen.COLS;
            texX = BrewingStandScreen.TEX_MAIN_INV_X + col * BrewingStandScreen.TEX_SLOT_SIZE;
            texY = BrewingStandScreen.TEX_MAIN_INV_Y + row * BrewingStandScreen.TEX_SLOT_SIZE;
        }
        double x = screen.getWindowX() + (texX + BrewingStandScreen.TEX_SLOT_SIZE / 2.0)
                * BrewingStandScreen.GUI_SCALE;
        double y = screen.getWindowY() + (texY + BrewingStandScreen.TEX_SLOT_SIZE / 2.0)
                * BrewingStandScreen.GUI_SCALE;
        return ScreenDragTestSupport.point(x, y);
    }

    private static void assertStack(ItemStack stack, ItemType type, int count) {
        assertSame(type, stack.getType());
        assertEquals(count, stack.getCount());
    }

    private static ItemStack potion(PotionType type) {
        ItemStack stack = new ItemStack(ItemType.POTION, 1);
        stack.setPotionData(new PotionData(type, false, false, false));
        return stack;
    }

    private static AchievementTracker unlockedIntoFireTracker() {
        AchievementTracker tracker = new AchievementTracker();
        assertTrue(tracker.unlock(AchievementType.OPEN_INVENTORY));
        assertTrue(tracker.unlock(AchievementType.MINE_WOOD));
        assertTrue(tracker.unlock(AchievementType.BUILD_WORKBENCH));
        assertTrue(tracker.unlock(AchievementType.BUILD_PICKAXE));
        assertTrue(tracker.unlock(AchievementType.BUILD_FURNACE));
        assertTrue(tracker.unlock(AchievementType.ACQUIRE_IRON));
        assertTrue(tracker.unlock(AchievementType.DIAMONDS));
        assertTrue(tracker.unlock(AchievementType.PORTAL));
        assertTrue(tracker.unlock(AchievementType.BLAZE_ROD));
        return tracker;
    }

    private static void clearHotbarKeys() throws Exception {
        for (int key = GLFW_KEY_1; key <= GLFW_KEY_9; key++) {
            ScreenDragTestSupport.clearKeys(key);
        }
    }
}
