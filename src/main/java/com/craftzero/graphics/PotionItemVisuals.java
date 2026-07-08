package com.craftzero.graphics;

import com.craftzero.inventory.ItemStack;
import com.craftzero.inventory.ItemType;
import com.craftzero.progression.PotionData;
import com.craftzero.progression.StatusEffectVisuals;

/**
 * Release-era potion item colors for GUI and inventory bottle overlays.
 */
public final class PotionItemVisuals {
    private static final int DEFAULT_BOTTLE_COLOR = StatusEffectVisuals.DEFAULT_POTION_COLOR;

    private PotionItemVisuals() {
    }

    public static boolean shouldDrawOverlay(ItemStack stack) {
        return stack != null && !stack.isEmpty() && stack.getType() == ItemType.POTION;
    }

    public static boolean isSplash(ItemStack stack) {
        return shouldDrawOverlay(stack) && stack.getPotionData() != null && stack.getPotionData().splash();
    }

    public static int liquidColorRgb(ItemStack stack) {
        if (!shouldDrawOverlay(stack)) {
            return DEFAULT_BOTTLE_COLOR;
        }
        return liquidColorRgb(stack.getPotionData());
    }

    public static int liquidColorRgb(PotionData data) {
        return StatusEffectVisuals.potionColor(data);
    }

    public static float[] liquidColor(ItemStack stack) {
        int rgb = liquidColorRgb(stack);
        return new float[] {
                ((rgb >> 16) & 0xFF) / 255.0f,
                ((rgb >> 8) & 0xFF) / 255.0f,
                (rgb & 0xFF) / 255.0f
        };
    }
}
