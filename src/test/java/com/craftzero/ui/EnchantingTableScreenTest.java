package com.craftzero.ui;

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
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_1;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_9;
import static org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_LEFT;
import static org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_RIGHT;

class EnchantingTableScreenTest {
    @AfterEach
    void clearMouseButtons() throws Exception {
        ScreenDragTestSupport.clearMouseButtons(GLFW_MOUSE_BUTTON_LEFT, GLFW_MOUSE_BUTTON_RIGHT);
        clearHotbarKeys();
    }

    @Test
    @DisplayName("Enchanting table slot should accept any item but cap the slot at one")
    void tableSlotAcceptsAnyItemWithOneItemCap() throws Exception {
        Inventory inventory = new Inventory();
        EnchantingTableScreen screen = new EnchantingTableScreen(inventory);

        inventory.setCursorItem(new ItemStack(ItemType.DIRT, 3));
        clickSlot(screen, 0, false);

        assertSame(ItemType.DIRT, tableItem(screen).getType());
        assertEquals(1, tableItem(screen).getCount());
        assertEquals(2, inventory.getCursorItem().getCount());

        clickSlot(screen, 0, false);

        assertEquals(1, tableItem(screen).getCount());
        assertEquals(2, inventory.getCursorItem().getCount());

        clickSlot(screen, 0, true);

        assertEquals(1, tableItem(screen).getCount());
        assertEquals(2, inventory.getCursorItem().getCount());

        setTableItem(screen, null);
        inventory.setCursorItem(new ItemStack(ItemType.BOW, 1));

        clickSlot(screen, 0, false);

        assertSame(ItemType.BOW, tableItem(screen).getType());
        assertNull(inventory.getCursorItem());

        setTableItem(screen, null);
        inventory.setCursorItem(new ItemStack(ItemType.DIAMOND_SWORD, 1));

        clickSlot(screen, 0, false);

        assertSame(ItemType.DIAMOND_SWORD, tableItem(screen).getType());
        assertNull(inventory.getCursorItem());
    }

    @Test
    @DisplayName("Number keys should respect the enchanting table one-item slot cap")
    void numberKeySwapRespectsEnchantingTableSlotCap() throws Exception {
        World world = new World(6249L);
        try {
            Inventory inventory = new Inventory();
            EnchantingTableScreen screen = new EnchantingTableScreen(inventory);
            screen.open(world, new Vector3i(0, 70, 0), new com.craftzero.progression.PlayerProgression(), 800, 600);
            inventory.getHotbar()[0] = new ItemStack(ItemType.DIRT, 3);

            double[] point = enchantingSlotCenter(screen, 0);
            ScreenDragTestSupport.setMousePosition(point[0], point[1]);
            ScreenDragTestSupport.pressKey(GLFW_KEY_1);
            screen.update();

            assertNull(tableItem(screen));
            assertStack(inventory.getHotbar()[0], ItemType.DIRT, 3);

            clearHotbarKeys();
            inventory.getHotbar()[0] = new ItemStack(ItemType.DIAMOND_SWORD, 1);
            ScreenDragTestSupport.pressKey(GLFW_KEY_1);
            screen.update();

            assertStack(tableItem(screen), ItemType.DIAMOND_SWORD, 1);
            assertNull(inventory.getHotbar()[0]);

            screen.close();
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Enchanting drag should cap the table slot and continue into player inventory")
    void leftDragCapsTableSlotAndContinuesIntoPlayerInventory() throws Exception {
        World world = new World(6250L);
        try {
            Inventory inventory = new Inventory();
            EnchantingTableScreen screen = new EnchantingTableScreen(inventory);
            screen.open(world, new Vector3i(0, 70, 0), new com.craftzero.progression.PlayerProgression(), 800, 600);
            inventory.setCursorItem(new ItemStack(ItemType.DIRT, 3));

            ScreenDragTestSupport.drag(GLFW_MOUSE_BUTTON_LEFT, screen::update,
                    enchantingSlotCenter(screen, 0),
                    enchantingSlotCenter(screen, 1));

            assertStack(tableItem(screen), ItemType.DIRT, 1);
            assertStack(inventory.getMainInventory()[0], ItemType.DIRT, 2);
            assertNull(inventory.getCursorItem());

            screen.close();
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Enchanting double-click should collect matching table and player stacks")
    void doubleClickCollectsMatchingEnchantingAndPlayerStacks() throws Exception {
        Inventory inventory = new Inventory();
        EnchantingTableScreen screen = new EnchantingTableScreen(inventory);
        inventory.setCursorItem(new ItemStack(ItemType.DIRT, 10));
        setTableItem(screen, new ItemStack(ItemType.DIRT, 1));
        inventory.getMainInventory()[0] = new ItemStack(ItemType.DIRT, 20);
        inventory.getHotbar()[0] = new ItemStack(ItemType.GRAVEL, 4);

        assertTrue(doubleClick(screen, 0));

        assertStack(inventory.getCursorItem(), ItemType.DIRT, 31);
        assertNull(tableItem(screen));
        assertNull(inventory.getMainInventory()[0]);
        assertStack(inventory.getHotbar()[0], ItemType.GRAVEL, 4);
    }

    @Test
    @DisplayName("Enchanting shift-click should move one player item into an empty table slot")
    void shiftClickMovesOnePlayerItemIntoEmptyTableSlot() throws Exception {
        Inventory inventory = new Inventory();
        EnchantingTableScreen screen = new EnchantingTableScreen(inventory);

        inventory.getMainInventory()[0] = new ItemStack(ItemType.DIRT, 4);
        shiftClick(screen, 1, inventory.getMainInventory()[0]);

        assertSame(ItemType.DIRT, tableItem(screen).getType());
        assertEquals(1, tableItem(screen).getCount());
        assertEquals(3, inventory.getMainInventory()[0].getCount());

        inventory.getMainInventory()[1] = new ItemStack(ItemType.SUGAR, 2);
        shiftClick(screen, 2, inventory.getMainInventory()[1]);

        assertSame(ItemType.DIRT, tableItem(screen).getType());
        assertSame(ItemType.SUGAR, inventory.getMainInventory()[1].getType());
        assertEquals(2, inventory.getMainInventory()[1].getCount());

        shiftClick(screen, 0, tableItem(screen));

        assertNull(tableItem(screen));
        assertSame(ItemType.DIRT, inventory.getMainInventory()[0].getType());
        assertEquals(4, inventory.getMainInventory()[0].getCount());
    }

    @Test
    @DisplayName("Changing the enchanting slot should reroll Release-style offers")
    void tableSlotChangeRerollsOffers() throws Exception {
        CountingLongRandom random = new CountingLongRandom(11L, 22L);
        World world = new RandomOverrideWorld(6244L, random);
        try {
            Inventory inventory = new Inventory();
            EnchantingTableScreen screen = new EnchantingTableScreen(inventory);
            bindWorld(screen, world, new Vector3i(0, 70, 0));

            setItemInSlot(screen, 0, new ItemStack(ItemType.DIAMOND_SWORD, 1));
            long firstSeed = offerSeed(screen);
            assertEquals(11L, firstSeed);
            assertHasOffer(screen.getOffers());

            setItemInSlot(screen, 0, null);
            setItemInSlot(screen, 0, new ItemStack(ItemType.DIAMOND_SWORD, 1));

            assertEquals(22L, offerSeed(screen));
            assertNotEquals(firstSeed, offerSeed(screen));
            assertEquals(3, random.nextLongCalls());
            assertHasOffer(screen.getOffers());
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Enchanting offer seed rerolls should use the owning world's RNG")
    void offerSeedRerollUsesWorldRandom() throws Exception {
        CountingLongRandom random = new CountingLongRandom(44L);
        World world = new RandomOverrideWorld(6245L, random);
        try {
            Inventory inventory = new Inventory();
            EnchantingTableScreen screen = new EnchantingTableScreen(inventory);
            bindWorld(screen, world, new Vector3i(0, 70, 0));

            rerollOfferSeed(screen);

            assertEquals(44L, offerSeed(screen));
            assertEquals(1, random.nextLongCalls());
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Enchanting offers should expose Release-style phrase text and row states")
    void offerRowsExposeReleaseStylePhraseTextAndStates() throws Exception {
        CountingLongRandom random = new CountingLongRandom(77L, 88L);
        World world = new RandomOverrideWorld(6247L, random);
        try {
            Inventory inventory = new Inventory();
            EnchantingTableScreen screen = new EnchantingTableScreen(inventory);
            bindWorld(screen, world, new Vector3i(0, 70, 0));

            setItemInSlot(screen, 0, new ItemStack(ItemType.DIAMOND_SWORD, 1));
            int[] offers = screen.getOffers();
            String[] phrases = screen.getOfferPhrases();
            Random phraseRandom = new Random(77L);

            for (int i = 0; i < phrases.length; i++) {
                assertEquals(EnchantingTableScreen.generateOfferPhrase(phraseRandom), phrases[i]);
                int wordCount = phrases[i].split(" ").length;
                assertTrue(wordCount >= 3 && wordCount <= 4);
            }

            int cost = offers[2];
            assertTrue(cost > 0);
            assertEquals(EnchantingTableScreen.TEX_OFFER_DISABLED_V,
                    EnchantingTableScreen.offerTextureV(0, 50, true));
            assertEquals(EnchantingTableScreen.TEX_OFFER_DISABLED_V,
                    EnchantingTableScreen.offerTextureV(cost, cost - 1, true));
            assertEquals(EnchantingTableScreen.TEX_OFFER_NORMAL_V,
                    EnchantingTableScreen.offerTextureV(cost, cost, false));
            assertEquals(EnchantingTableScreen.TEX_OFFER_HOVER_V,
                    EnchantingTableScreen.offerTextureV(cost, cost, true));

            setItemInSlot(screen, 0, new ItemStack(ItemType.DIRT, 1));
            for (String phrase : screen.getOfferPhrases()) {
                assertEquals("", phrase);
            }
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Closing enchanting table should drop the table slot item instead of returning it")
    void closeQueuesTableItemForDrop() throws Exception {
        World world = new World(6246L);
        try {
            Inventory inventory = new Inventory();
            EnchantingTableScreen screen = new EnchantingTableScreen(inventory);
            screen.open(world, new Vector3i(0, 70, 0), new com.craftzero.progression.PlayerProgression(), 800, 600);
            setTableItem(screen, new ItemStack(ItemType.DIAMOND_SWORD, 1));
            inventory.getHotbar()[0] = new ItemStack(ItemType.DIRT, 1);

            screen.close();

            assertNull(tableItem(screen));
            assertSame(ItemType.DIRT, inventory.getHotbar()[0].getType());
            assertEquals(1, inventory.getHotbar()[0].getCount());
            List<ItemStack> drops = screen.getAndClearItemsToThrow();
            assertEquals(1, drops.size());
            assertSame(ItemType.DIAMOND_SWORD, drops.get(0).getType());
            assertEquals(1, drops.get(0).getCount());
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Enchanting table screen should only stay usable near the same table block")
    void enchantingTableScreenRequiresSameNearbyTableBlock() {
        World world = new World(8135L);
        try {
            world.setBlock(0, 70, 0, BlockType.ENCHANTING_TABLE);
            Player player = new Player(0.0f, 70.0f, 0.0f);
            EnchantingTableScreen screen = new EnchantingTableScreen(player.getInventory());
            screen.open(world, new Vector3i(0, 70, 0), new com.craftzero.progression.PlayerProgression(), 800, 600);

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

    private static void clickSlot(EnchantingTableScreen screen, int slot, boolean rightClick) throws Exception {
        Method handleClick = EnchantingTableScreen.class.getDeclaredMethod("handleClick", int.class, boolean.class);
        handleClick.setAccessible(true);
        handleClick.invoke(screen, slot, rightClick);
    }

    private static boolean doubleClick(EnchantingTableScreen screen, int slot) throws Exception {
        Method handleDoubleClick = EnchantingTableScreen.class.getDeclaredMethod("handleDoubleClick", int.class);
        handleDoubleClick.setAccessible(true);
        return (boolean) handleDoubleClick.invoke(screen, slot);
    }

    private static void shiftClick(EnchantingTableScreen screen, int slot, ItemStack stack) throws Exception {
        Method shiftClick = EnchantingTableScreen.class.getDeclaredMethod("shiftClick", int.class, ItemStack.class);
        shiftClick.setAccessible(true);
        shiftClick.invoke(screen, slot, stack);
    }

    private static void setItemInSlot(EnchantingTableScreen screen, int slot, ItemStack stack) throws Exception {
        Method setItemInSlot = EnchantingTableScreen.class.getDeclaredMethod("setItemInSlot", int.class,
                ItemStack.class);
        setItemInSlot.setAccessible(true);
        setItemInSlot.invoke(screen, slot, stack);
    }

    private static void bindWorld(EnchantingTableScreen screen, World world, Vector3i tablePos) throws Exception {
        Field worldField = EnchantingTableScreen.class.getDeclaredField("world");
        worldField.setAccessible(true);
        worldField.set(screen, world);
        Field tablePosField = EnchantingTableScreen.class.getDeclaredField("tablePos");
        tablePosField.setAccessible(true);
        tablePosField.set(screen, tablePos);
    }

    private static void rerollOfferSeed(EnchantingTableScreen screen) throws Exception {
        Method rerollOfferSeed = EnchantingTableScreen.class.getDeclaredMethod("rerollOfferSeed");
        rerollOfferSeed.setAccessible(true);
        rerollOfferSeed.invoke(screen);
    }

    private static long offerSeed(EnchantingTableScreen screen) throws Exception {
        Field field = EnchantingTableScreen.class.getDeclaredField("offerSeed");
        field.setAccessible(true);
        return (long) field.get(screen);
    }

    private static ItemStack tableItem(EnchantingTableScreen screen) throws Exception {
        Field tableItem = EnchantingTableScreen.class.getDeclaredField("tableItem");
        tableItem.setAccessible(true);
        return (ItemStack) tableItem.get(screen);
    }

    private static void setTableItem(EnchantingTableScreen screen, ItemStack stack) throws Exception {
        Field tableItem = EnchantingTableScreen.class.getDeclaredField("tableItem");
        tableItem.setAccessible(true);
        tableItem.set(screen, stack);
    }

    private static double[] enchantingSlotCenter(EnchantingTableScreen screen, int slot) {
        int texX;
        int texY;
        if (slot == 0) {
            texX = EnchantingTableScreen.TEX_TABLE_SLOT_X;
            texY = EnchantingTableScreen.TEX_TABLE_SLOT_Y;
        } else {
            int playerIndex = slot - 1;
            int col = playerIndex % EnchantingTableScreen.COLS;
            int row = playerIndex / EnchantingTableScreen.COLS;
            texX = EnchantingTableScreen.TEX_MAIN_INV_X + col * EnchantingTableScreen.TEX_SLOT_SIZE;
            texY = EnchantingTableScreen.TEX_MAIN_INV_Y + row * EnchantingTableScreen.TEX_SLOT_SIZE;
        }
        double x = screen.getWindowX() + (texX + EnchantingTableScreen.TEX_SLOT_SIZE / 2.0)
                * EnchantingTableScreen.GUI_SCALE;
        double y = screen.getWindowY() + (texY + EnchantingTableScreen.TEX_SLOT_SIZE / 2.0)
                * EnchantingTableScreen.GUI_SCALE;
        return ScreenDragTestSupport.point(x, y);
    }

    private static void assertStack(ItemStack stack, ItemType type, int count) {
        assertSame(type, stack.getType());
        assertEquals(count, stack.getCount());
    }

    private static void assertHasOffer(int[] offers) {
        for (int offer : offers) {
            if (offer > 0) {
                return;
            }
        }
        assertTrue(false, "Expected at least one enchanting offer");
    }

    private static void clearHotbarKeys() throws Exception {
        for (int key = GLFW_KEY_1; key <= GLFW_KEY_9; key++) {
            ScreenDragTestSupport.clearKeys(key);
        }
    }

    private static final class CountingLongRandom extends Random {
        private final long[] values;
        private int index;

        private CountingLongRandom(long... values) {
            this.values = values;
        }

        @Override
        public long nextLong() {
            long value = values[Math.min(index, values.length - 1)];
            index++;
            return value;
        }

        private int nextLongCalls() {
            return index;
        }
    }

    private static final class RandomOverrideWorld extends World {
        private final Random random;

        private RandomOverrideWorld(long seed, Random random) {
            super(seed);
            this.random = random;
        }

        @Override
        public Random getRandom() {
            return random;
        }
    }
}
