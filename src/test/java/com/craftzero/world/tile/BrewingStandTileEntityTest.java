package com.craftzero.world.tile;

import com.craftzero.inventory.ItemStack;
import com.craftzero.inventory.ItemType;
import com.craftzero.progression.PotionData;
import com.craftzero.progression.PotionType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BrewingStandTileEntityTest {

    @Test
    @DisplayName("Brewing stand should brew three bottles after 600 ticks")
    void brewsAfterSixHundredTicks() {
        BrewingStandTileEntity stand = new BrewingStandTileEntity(0, 64, 0);
        for (int i = 0; i < 3; i++) {
            ItemStack bottle = new ItemStack(ItemType.POTION, 1);
            bottle.setPotionData(PotionData.water());
            stand.getInventory()[i] = bottle;
        }
        stand.getInventory()[BrewingStandTileEntity.SLOT_INGREDIENT] = new ItemStack(ItemType.NETHER_WART, 1);

        stand.tick(null, 29.95f);
        assertTrue(stand.getBrewTime() > 0);
        assertSame(PotionType.WATER, stand.getInventory()[0].getPotionData().type());

        stand.tick(null, 0.05f);
        assertSame(PotionType.AWKWARD, stand.getInventory()[0].getPotionData().type());
        assertSame(PotionType.AWKWARD, stand.getInventory()[1].getPotionData().type());
        assertSame(PotionType.AWKWARD, stand.getInventory()[2].getPotionData().type());
        assertNull(stand.getInventory()[BrewingStandTileEntity.SLOT_INGREDIENT]);
        assertEquals(0, stand.getBrewTime());
    }

    @Test
    @DisplayName("Brewing registry should reject invalid bottle and ingredient combinations")
    void rejectsInvalidRecipes() {
        ItemStack dirt = new ItemStack(ItemType.DIRT, 1);
        ItemStack sugar = new ItemStack(ItemType.SUGAR, 1);

        assertNull(BrewingRecipeRegistry.brew(dirt, sugar));
        assertFalse(BrewingRecipeRegistry.isIngredient(new ItemStack(ItemType.DIAMOND, 1)));
    }
}
