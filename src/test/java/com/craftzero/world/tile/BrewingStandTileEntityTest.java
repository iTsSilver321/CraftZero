package com.craftzero.world.tile;

import com.craftzero.inventory.ItemStack;
import com.craftzero.inventory.ItemType;
import com.craftzero.progression.PotionData;
import com.craftzero.progression.PotionType;
import com.craftzero.world.BlockType;
import com.craftzero.world.World;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BrewingStandTileEntityTest {

    @Test
    @DisplayName("Brewing stand should count down 400 legacy brewing ticks")
    void brewsAfterFourHundredCountdownTicks() {
        BrewingStandTileEntity stand = new BrewingStandTileEntity(0, 64, 0);
        for (int i = 0; i < 3; i++) {
            ItemStack bottle = new ItemStack(ItemType.POTION, 1);
            bottle.setPotionData(PotionData.water());
            stand.getInventory()[i] = bottle;
        }
        stand.getInventory()[BrewingStandTileEntity.SLOT_INGREDIENT] = new ItemStack(ItemType.NETHER_WART, 1);

        stand.tick(null, 1.0f / 20.0f);
        assertEquals(BrewingStandTileEntity.BREW_TIME_TOTAL, stand.getBrewTime());
        stand.tick(null, (BrewingStandTileEntity.BREW_TIME_TOTAL - 1) / 20.0f);
        assertEquals(1, stand.getBrewTime());
        assertSame(PotionType.WATER, stand.getInventory()[0].getPotionData().type());

        stand.tick(null, 1.0f / 20.0f);
        assertSame(PotionType.AWKWARD, stand.getInventory()[0].getPotionData().type());
        assertSame(PotionType.AWKWARD, stand.getInventory()[1].getPotionData().type());
        assertSame(PotionType.AWKWARD, stand.getInventory()[2].getPotionData().type());
        assertNull(stand.getInventory()[BrewingStandTileEntity.SLOT_INGREDIENT]);
        assertEquals(0, stand.getBrewTime());
    }

    @Test
    @DisplayName("Brewing stand should cancel active brewing when the ingredient changes")
    void ingredientChangeCancelsActiveBrew() {
        BrewingStandTileEntity stand = new BrewingStandTileEntity(0, 64, 0);
        ItemStack bottle = new ItemStack(ItemType.POTION, 1);
        bottle.setPotionData(PotionData.water());
        stand.getInventory()[BrewingStandTileEntity.SLOT_BOTTLE_0] = bottle;
        stand.getInventory()[BrewingStandTileEntity.SLOT_INGREDIENT] = new ItemStack(ItemType.NETHER_WART, 1);

        stand.tick(null, 1.0f / 20.0f);
        assertEquals(BrewingStandTileEntity.BREW_TIME_TOTAL, stand.getBrewTime());

        stand.getInventory()[BrewingStandTileEntity.SLOT_INGREDIENT] = new ItemStack(ItemType.SUGAR, 1);
        stand.tick(null, 1.0f / 20.0f);

        assertEquals(0, stand.getBrewTime());
        assertSame(PotionType.WATER, stand.getInventory()[BrewingStandTileEntity.SLOT_BOTTLE_0].getPotionData().type());
    }

    @Test
    @DisplayName("Brewing stand should mirror occupied bottle slots into block metadata and refresh meshes")
    void bottleSlotsUpdateBlockMetadataAndRefreshMeshes() {
        RecordingWorld world = new RecordingWorld(5153L);
        try {
            world.setBlock(0, 70, 0, BlockType.BREWING_STAND, 0);
            BrewingStandTileEntity stand = (BrewingStandTileEntity) world.getTileEntity(0, 70, 0);
            stand.getInventory()[BrewingStandTileEntity.SLOT_BOTTLE_0] = new ItemStack(ItemType.POTION, 1);
            stand.getInventory()[BrewingStandTileEntity.SLOT_BOTTLE_2] = new ItemStack(ItemType.POTION, 1);

            stand.tick(world, 1.0f / 20.0f);

            assertEquals(5, stand.getFilledSlots());
            assertEquals(5, world.getBlockMetadata(0, 70, 0));
            assertEquals(1, world.rebuildCount);
            assertEquals(0, world.lastRebuildX);
            assertEquals(70, world.lastRebuildY);
            assertEquals(0, world.lastRebuildZ);

            stand.tick(world, 1.0f / 20.0f);

            assertEquals(1, world.rebuildCount);
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Brewing registry should reject invalid bottle and ingredient combinations")
    void rejectsInvalidRecipes() {
        ItemStack dirt = new ItemStack(ItemType.DIRT, 1);
        ItemStack sugar = new ItemStack(ItemType.SUGAR, 1);

        assertNull(BrewingRecipeRegistry.brew(dirt, sugar));
        assertFalse(BrewingRecipeRegistry.isIngredient(new ItemStack(ItemType.DIAMOND, 1)));
    }

    @Test
    @DisplayName("Ghast tear and magma cream should make mundane from water and effects from awkward")
    void ghastTearAndMagmaCreamUseReleaseOneBaseAndEffectPaths() {
        assertEquals(new PotionData(PotionType.MUNDANE, false, false, false),
                BrewingRecipeRegistry.brew(PotionData.water(), ItemType.GHAST_TEAR));
        assertEquals(new PotionData(PotionType.MUNDANE, false, false, false),
                BrewingRecipeRegistry.brew(PotionData.water(), ItemType.MAGMA_CREAM));

        PotionData awkward = new PotionData(PotionType.AWKWARD, false, false, false);
        assertEquals(new PotionData(PotionType.REGENERATION, false, false, false),
                BrewingRecipeRegistry.brew(awkward, ItemType.GHAST_TEAR));
        assertEquals(new PotionData(PotionType.FIRE_RESISTANCE, false, false, false),
                BrewingRecipeRegistry.brew(awkward, ItemType.MAGMA_CREAM));
    }

    @Test
    @DisplayName("Brewing registry should keep legacy mundane-to-weakness transformations")
    void mundaneWeaknessRecipesMatchReleaseOne() {
        PotionData extendedMundane = BrewingRecipeRegistry.brew(PotionData.water(), ItemType.REDSTONE);
        assertEquals(new PotionData(PotionType.MUNDANE, false, true, false), extendedMundane);

        PotionData extendedWeakness = BrewingRecipeRegistry.brew(extendedMundane, ItemType.FERMENTED_SPIDER_EYE);
        assertEquals(new PotionData(PotionType.WEAKNESS, false, true, false), extendedWeakness);

        PotionData mundane = BrewingRecipeRegistry.brew(PotionData.water(), ItemType.SUGAR);
        assertEquals(new PotionData(PotionType.MUNDANE, false, false, false), mundane);
        assertEquals(new PotionData(PotionType.WEAKNESS, false, false, false),
                BrewingRecipeRegistry.brew(mundane, ItemType.FERMENTED_SPIDER_EYE));
        assertEquals(new PotionData(PotionType.WEAKNESS, false, false, false),
                BrewingRecipeRegistry.brew(new PotionData(PotionType.AWKWARD, false, false, false),
                        ItemType.FERMENTED_SPIDER_EYE));
        assertEquals(new PotionData(PotionType.WEAKNESS, false, false, false),
                BrewingRecipeRegistry.brew(new PotionData(PotionType.THICK, false, false, false),
                        ItemType.FERMENTED_SPIDER_EYE));
    }

    @Test
    @DisplayName("Brewing modifiers should preserve splash potion delivery")
    void splashPotionBrewingPreservesSplashDelivery() {
        PotionData splashWater = new PotionData(PotionType.WATER, true, false, false);
        PotionData splashAwkward = BrewingRecipeRegistry.brew(splashWater, ItemType.NETHER_WART);
        assertEquals(new PotionData(PotionType.AWKWARD, true, false, false), splashAwkward);

        PotionData splashSwiftness = BrewingRecipeRegistry.brew(splashAwkward, ItemType.SUGAR);
        assertEquals(new PotionData(PotionType.SWIFTNESS, true, false, false), splashSwiftness);
        assertEquals(new PotionData(PotionType.SWIFTNESS, true, true, false),
                BrewingRecipeRegistry.brew(splashSwiftness, ItemType.REDSTONE));

        PotionData splashHealing = new PotionData(PotionType.HEALING, true, false, false);
        assertEquals(new PotionData(PotionType.HEALING, true, false, true),
                BrewingRecipeRegistry.brew(splashHealing, ItemType.GLOWSTONE_DUST));

        PotionData splashPoisonTwo = new PotionData(PotionType.POISON, true, false, true);
        assertEquals(new PotionData(PotionType.HARMING, true, false, true),
                BrewingRecipeRegistry.brew(splashPoisonTwo, ItemType.FERMENTED_SPIDER_EYE));

        assertNull(BrewingRecipeRegistry.brew(splashWater, ItemType.GUNPOWDER));
    }

    private static final class RecordingWorld extends World {
        private int rebuildCount;
        private int lastRebuildX;
        private int lastRebuildY;
        private int lastRebuildZ;

        private RecordingWorld(long seed) {
            super(seed);
        }

        @Override
        public void rebuildBlockMeshesNow(int x, int y, int z) {
            rebuildCount++;
            lastRebuildX = x;
            lastRebuildY = y;
            lastRebuildZ = z;
        }
    }
}
