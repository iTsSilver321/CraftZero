package com.craftzero.graphics;

/**
 * Manages GUI texture atlases for HUD elements like hearts, hunger, hotbar,
 * inventory, and crafting table backgrounds.
 * 
 * UV coordinates are based on Minecraft's texture layouts:
 * - icons.png (256x256): hearts, hunger, armor, experience bar
 * - gui.png (256x256): hotbar, widgets, buttons
 * - inventory.png: player inventory background
 * - crafting.png: crafting table background
 */
public class GuiTexture {

    private static Texture iconsTexture;
    private static Texture guiTexture;
    private static Texture inventoryTexture;
    private static Texture craftingTexture;
    private static Texture itemsTexture; // Items atlas (stick, tools, etc.)

    private static boolean initialized = false;

    /**
     * Load all GUI textures.
     */
    public static void init() throws Exception {
        if (initialized)
            return;

        iconsTexture = new Texture("/textures/gui/icons.png");
        guiTexture = new Texture("/textures/gui/gui.png");
        inventoryTexture = new Texture("/textures/gui/inventory.png");
        craftingTexture = new Texture("/textures/gui/crafting.png");
        itemsTexture = new Texture("/textures/item/items.png");

        initialized = true;
        System.out.println("GUI textures loaded successfully");
    }

    public static Texture getIconsTexture() {
        return iconsTexture;
    }

    public static Texture getGuiTexture() {
        return guiTexture;
    }

    public static Texture getInventoryTexture() {
        return inventoryTexture;
    }

    public static Texture getCraftingTexture() {
        return craftingTexture;
    }

    // ========== icons.png UV coordinates (256x256) ==========
    // Hearts are located in row 0, starting at (16, 0)
    // Each heart is 9x9 pixels

    private static final float ICONS_SIZE = 256f;

    // Heart container (empty background) - (16, 0)
    public static float[] getHeartContainerUV() {
        return getUV(16, 0, 9, 9, ICONS_SIZE);
    }

    // Full heart - (52, 0)
    public static float[] getFullHeartUV() {
        return getUV(52, 0, 9, 9, ICONS_SIZE);
    }

    // Half heart - (61, 0)
    public static float[] getHalfHeartUV() {
        return getUV(61, 0, 9, 9, ICONS_SIZE);
    }

    // Empty heart background - (16, 0)
    public static float[] getEmptyHeartUV() {
        return getUV(16, 0, 9, 9, ICONS_SIZE);
    }

    // Hunger (full drumstick) - (52, 27)
    public static float[] getFullHungerUV() {
        return getUV(52, 27, 9, 9, ICONS_SIZE);
    }

    // Hunger (half drumstick) - (61, 27)
    public static float[] getHalfHungerUV() {
        return getUV(61, 27, 9, 9, ICONS_SIZE);
    }

    // Hunger container (empty background) - (16, 27)
    public static float[] getHungerContainerUV() {
        return getUV(16, 27, 9, 9, ICONS_SIZE);
    }

    // ========== gui.png UV coordinates (256x256) ==========

    private static final float GUI_SIZE = 256f;

    // Hotbar (182x22 pixels at position 0,0)
    public static float[] getHotbarUV() {
        return getUV(0, 0, 182, 22, GUI_SIZE);
    }

    // Hotbar selection frame (24x24 at position 0, 22)
    public static float[] getHotbarSelectionUV() {
        return getUV(0, 22, 24, 24, GUI_SIZE);
    }

    // ========== Bubble UV coordinates (icons.png) ==========
    // Bubbles are in icons.png at row 16, 9x9 pixels each

    // Full bubble - (16, 18)
    public static float[] getFullBubbleUV() {
        return getUV(16, 18, 9, 9, ICONS_SIZE);
    }

    // Popping/empty bubble - (25, 18)
    public static float[] getEmptyBubbleUV() {
        return getUV(25, 18, 9, 9, ICONS_SIZE);
    }

    // ========== Items texture ==========

    private static final float ITEMS_SIZE = 256f; // items.png is 256x256, 16x16 grid

    public static Texture getItemsTexture() {
        return itemsTexture;
    }

    /**
     * Get UV coordinates for an item in items.png by grid position.
     * items.png is a 16x16 grid of 16x16 pixel sprites.
     */
    public static float[] getItemUV(int col, int row) {
        float u1 = col * 16f / ITEMS_SIZE;
        float v1 = row * 16f / ITEMS_SIZE;
        float u2 = (col + 1) * 16f / ITEMS_SIZE;
        float v2 = (row + 1) * 16f / ITEMS_SIZE;
        return new float[] { u1, v1, u2, v2 };
    }

    /**
     * Helper to calculate UV coordinates from pixel positions.
     * Returns [u1, v1, u2, v2] (top-left to bottom-right)
     */
    private static float[] getUV(int x, int y, int width, int height, float atlasSize) {
        float u1 = x / atlasSize;
        float v1 = y / atlasSize;
        float u2 = (x + width) / atlasSize;
        float v2 = (y + height) / atlasSize;
        return new float[] { u1, v1, u2, v2 };
    }

    public static void cleanup() {
        if (iconsTexture != null)
            iconsTexture.cleanup();
        if (guiTexture != null)
            guiTexture.cleanup();
        if (inventoryTexture != null)
            inventoryTexture.cleanup();
        if (craftingTexture != null)
            craftingTexture.cleanup();
        if (itemsTexture != null)
            itemsTexture.cleanup();
        initialized = false;
    }
}
