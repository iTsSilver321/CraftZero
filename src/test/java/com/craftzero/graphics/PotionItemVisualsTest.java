package com.craftzero.graphics;

import com.craftzero.inventory.ItemStack;
import com.craftzero.inventory.ItemType;
import com.craftzero.progression.PotionData;
import com.craftzero.progression.PotionType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PotionItemVisualsTest {

    @Test
    @DisplayName("Potion item visuals should use Release-era liquid colors")
    void potionVisualsUseReleaseEraLiquidColors() {
        assertEquals(0x385DC6, PotionItemVisuals.liquidColorRgb(PotionData.water()));
        assertEquals(0x385DC6,
                PotionItemVisuals.liquidColorRgb(new PotionData(PotionType.AWKWARD, false, false, false)));
        assertEquals(0xCD5CAB,
                PotionItemVisuals.liquidColorRgb(new PotionData(PotionType.REGENERATION, false, false, false)));
        assertEquals(0x7CAFC6,
                PotionItemVisuals.liquidColorRgb(new PotionData(PotionType.SWIFTNESS, false, false, false)));
        assertEquals(0xE49A3A,
                PotionItemVisuals.liquidColorRgb(new PotionData(PotionType.FIRE_RESISTANCE, false, false, false)));
        assertEquals(0x4E9331,
                PotionItemVisuals.liquidColorRgb(new PotionData(PotionType.POISON, false, false, false)));
        assertEquals(0xF82423,
                PotionItemVisuals.liquidColorRgb(new PotionData(PotionType.HEALING, false, false, false)));
        assertEquals(0x484D48,
                PotionItemVisuals.liquidColorRgb(new PotionData(PotionType.WEAKNESS, false, false, false)));
        assertEquals(0x932423,
                PotionItemVisuals.liquidColorRgb(new PotionData(PotionType.STRENGTH, false, false, false)));
        assertEquals(0x5A6C81,
                PotionItemVisuals.liquidColorRgb(new PotionData(PotionType.SLOWNESS, false, false, false)));
        assertEquals(0x430A09,
                PotionItemVisuals.liquidColorRgb(new PotionData(PotionType.HARMING, false, false, false)));
    }

    @Test
    @DisplayName("Potion strength and duration variants should keep the same inventory liquid color")
    void potionModifiersDoNotChangeLiquidColor() {
        PotionData normal = new PotionData(PotionType.SWIFTNESS, false, false, false);
        PotionData extended = new PotionData(PotionType.SWIFTNESS, false, true, false);
        PotionData enhanced = new PotionData(PotionType.SWIFTNESS, false, false, true);

        assertEquals(PotionItemVisuals.liquidColorRgb(normal), PotionItemVisuals.liquidColorRgb(extended));
        assertEquals(PotionItemVisuals.liquidColorRgb(normal), PotionItemVisuals.liquidColorRgb(enhanced));
    }

    @Test
    @DisplayName("Potion overlays should render only for potion item stacks and mark splash bottles")
    void overlaySelectionUsesPotionStackMetadata() {
        ItemStack poison = new ItemStack(ItemType.POTION, 1);
        poison.setPotionData(new PotionData(PotionType.POISON, true, false, false));
        ItemStack glassBottle = new ItemStack(ItemType.GLASS_BOTTLE, 1);

        assertTrue(PotionItemVisuals.shouldDrawOverlay(poison));
        assertTrue(PotionItemVisuals.isSplash(poison));
        assertFalse(PotionItemVisuals.shouldDrawOverlay(glassBottle));
        assertFalse(PotionItemVisuals.isSplash(glassBottle));
    }

    @Test
    @DisplayName("Potion color floats should be derived from the packed RGB value")
    void liquidColorFloatArrayComesFromRgb() {
        ItemStack healing = new ItemStack(ItemType.POTION, 1);
        healing.setPotionData(new PotionData(PotionType.HEALING, false, false, false));

        assertArrayEquals(new float[] { 248 / 255.0f, 36 / 255.0f, 35 / 255.0f },
                PotionItemVisuals.liquidColor(healing), 0.0001f);
    }
}
