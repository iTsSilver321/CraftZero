package com.craftzero.graphics;

import com.craftzero.inventory.ItemStack;
import com.craftzero.inventory.ItemType;
import com.craftzero.progression.EnchantmentInstance;
import com.craftzero.progression.EnchantmentType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EnchantedItemVisualsTest {

    @Test
    @DisplayName("Enchanted glint should render only for non-empty enchanted stacks")
    void glintSelectionUsesStackEnchantments() {
        ItemStack sword = new ItemStack(ItemType.DIAMOND_SWORD, 1);
        ItemStack empty = new ItemStack(ItemType.DIAMOND_SWORD, 0);

        assertFalse(EnchantedItemVisuals.shouldDrawGlint(null));
        assertFalse(EnchantedItemVisuals.shouldDrawGlint(empty));
        assertFalse(EnchantedItemVisuals.shouldDrawGlint(sword));

        sword.addEnchantment(new EnchantmentInstance(EnchantmentType.SHARPNESS, 3));

        assertTrue(EnchantedItemVisuals.shouldDrawGlint(sword));
    }

    @Test
    @DisplayName("Glint color arrays should not expose shared mutable state")
    void glintColorsAreDefensiveCopies() {
        float[] color = EnchantedItemVisuals.glintColor();
        float[] wash = EnchantedItemVisuals.glintWashColor();

        color[0] = 0.0f;
        wash[0] = 0.0f;

        assertEquals(0.58f, EnchantedItemVisuals.glintColor()[0], 0.0001f);
        assertEquals(0.36f, EnchantedItemVisuals.glintWashColor()[0], 0.0001f);
    }

    @Test
    @DisplayName("Glint bands should stay inside the item icon bounds")
    void glintBandsStayInsideIconBounds() {
        int x = 10;
        int y = 20;
        int size = 32;

        List<EnchantedItemVisuals.Band> bands = EnchantedItemVisuals.glintBands(x, y, size, 0L);

        assertTrue(bands.size() >= 2);
        for (EnchantedItemVisuals.Band band : bands) {
            assertTrue(band.vertexCount() >= 3);
            assertEquals(band.vertexCount() * 2, band.copyVertices().length);
            assertInsideIcon(band, x, y, size);
        }
        assertTrue(EnchantedItemVisuals.glintBands(x, y, 0, 0L).isEmpty());
    }

    @Test
    @DisplayName("Glint bands should scroll across animation ticks")
    void glintBandsScrollAcrossAnimationTicks() {
        int x = 10;
        int y = 20;
        int size = 32;

        List<EnchantedItemVisuals.Band> start = EnchantedItemVisuals.glintBands(x, y, size, 0L);
        List<EnchantedItemVisuals.Band> later = EnchantedItemVisuals.glintBands(x, y, size, 10L);

        assertNotEquals(signature(start), signature(later));
        start.forEach(band -> assertInsideIcon(band, x, y, size));
        later.forEach(band -> assertInsideIcon(band, x, y, size));
    }

    private static void assertInsideIcon(EnchantedItemVisuals.Band band, int x, int y, int size) {
        float[] vertices = band.copyVertices();
        for (int i = 0; i < band.vertexCount(); i++) {
            assertInsideIcon(vertices[i * 2], vertices[i * 2 + 1], x, y, size);
        }
    }

    private static void assertInsideIcon(float px, float py, int x, int y, int size) {
        assertTrue(px >= x && px <= x + size, () -> "x outside icon bounds: " + px);
        assertTrue(py >= y && py <= y + size, () -> "y outside icon bounds: " + py);
    }

    private static String signature(List<EnchantedItemVisuals.Band> bands) {
        StringBuilder builder = new StringBuilder();
        for (EnchantedItemVisuals.Band band : bands) {
            builder.append(band.vertexCount()).append(':');
            for (float value : band.copyVertices()) {
                builder.append(Math.round(value * 100.0f)).append(',');
            }
            builder.append(';');
        }
        return builder.toString();
    }
}
