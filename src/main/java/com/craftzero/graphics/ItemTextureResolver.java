package com.craftzero.graphics;

import com.craftzero.inventory.ItemType;

/**
 * Shared item texture lookup for HUD, inventory, dropped items, and held items.
 */
public final class ItemTextureResolver {
    private static final float ITEMS_SIZE = 256.0f;
    private static final float CELL_SIZE = 16.0f;

    private ItemTextureResolver() {
    }

    public static boolean usesItemsAtlas(ItemType type) {
        return type != null && type.usesItemTexture();
    }

    public static float[] getUv(ItemType type) {
        if (type == null) {
            return new float[] { 0, 0, 0, 0 };
        }
        if (usesItemsAtlas(type)) {
            int[] pos = type.getItemTexturePos();
            return getItemsUv(pos[0], pos[1]);
        }
        return type.getTextureCoords(com.craftzero.world.Block.FACE_SOUTH);
    }

    public static float[] getItemsUv(int col, int row) {
        float u1 = col * CELL_SIZE / ITEMS_SIZE;
        float v1 = row * CELL_SIZE / ITEMS_SIZE;
        float u2 = (col + 1) * CELL_SIZE / ITEMS_SIZE;
        float v2 = (row + 1) * CELL_SIZE / ITEMS_SIZE;
        return new float[] { u1, v1, u2, v2 };
    }
}
