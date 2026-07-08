package com.craftzero.inventory;

import com.craftzero.progression.EnchantmentInstance;
import com.craftzero.progression.EnchantmentType;
import com.craftzero.progression.PotionData;
import com.craftzero.progression.PotionType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ItemStackTest {

    @Test
    @DisplayName("Normal items should use their registry max stack size")
    void normalItemsUseRegistryStackSize() {
        ItemStack stack = new ItemStack(ItemType.DIRT, 12);

        assertEquals(64, stack.getMaxStackSize());
        assertEquals(-1, stack.getDurability());
        assertFalse(stack.isTool());
    }

    @Test
    @DisplayName("Tools should stack to one and initialize durability")
    void toolsUseDurability() {
        ItemStack stack = new ItemStack(ItemType.IRON_PICKAXE, 1);

        assertEquals(1, stack.getMaxStackSize());
        assertTrue(stack.isTool());
        assertEquals(ItemType.IRON_PICKAXE.getToolType().getMaxDurability(), stack.getDurability());
        assertEquals(stack.getMaxDurability(), stack.getDurability());
    }

    @Test
    @DisplayName("Damageable non-tool items should initialize and preserve durability")
    void damageableNonToolsUseDurability() {
        ItemStack stack = new ItemStack(ItemType.BOW, 1);

        assertFalse(stack.isTool());
        assertTrue(stack.isDamageable());
        assertEquals(1, stack.getMaxStackSize());
        assertEquals(385, stack.getDurability());

        stack.useDurability();
        ItemStack copy = stack.copy();
        assertEquals(384, copy.getDurability());
        assertSame(ItemType.BOW, copy.getType());
    }

    @Test
    @DisplayName("ItemStack copy should preserve count and durability")
    void copyPreservesDurability() {
        ItemStack stack = new ItemStack(ItemType.DIAMOND_PICKAXE, 1);
        stack.useDurability();
        stack.useDurability();

        ItemStack copy = stack.copy();

        assertNotSame(stack, copy);
        assertSame(stack.getType(), copy.getType());
        assertEquals(stack.getCount(), copy.getCount());
        assertEquals(stack.getDurability(), copy.getDurability());
    }

    @Test
    @DisplayName("ItemStack should preserve metadata-backed item identity")
    void stackPreservesMetadataIdentity() {
        ItemStack coal = new ItemStack(ItemType.COAL, 1);
        ItemStack charcoal = new ItemStack(ItemType.CHARCOAL, 1);

        assertEquals(263, coal.getType().getId());
        assertEquals(263, charcoal.getType().getId());
        assertEquals(0, coal.getType().getDataValue());
        assertEquals(1, charcoal.getType().getDataValue());
        assertNotSame(coal.getType(), charcoal.getType());
    }

    @Test
    @DisplayName("Maps should preserve Release-era item damage identity")
    void mapsPreserveItemDamageIdentity() {
        ItemStack blankMap = new ItemStack(ItemType.MAP, 1);
        assertEquals(-1, blankMap.getDurability());
        assertFalse(blankMap.isDamageable());
        assertTrue(blankMap.usesItemDamageIdentity());

        ItemStack filledMap = new ItemStack(ItemType.MAP, 1, 7);
        ItemStack copy = filledMap.copy();

        assertEquals(7, filledMap.getDurability());
        assertEquals(7, copy.getDurability());
        assertFalse(filledMap.useDurability());
        assertEquals(7, filledMap.getDurability());
    }

    @Test
    @DisplayName("Copy and merge checks should preserve structured stack metadata")
    void copyAndMergeRespectStructuredMetadata() {
        ItemStack enchanted = new ItemStack(ItemType.DIAMOND_SWORD, 1);
        enchanted.useDurability();
        enchanted.setCustomName("Sharp");
        enchanted.addEnchantment(new EnchantmentInstance(EnchantmentType.SHARPNESS, 3));
        enchanted.putMetadata("origin", "test");

        ItemStack copy = enchanted.copy();

        assertNotSame(enchanted, copy);
        assertEquals(enchanted.getDurability(), copy.getDurability());
        assertEquals(enchanted.getCustomName(), copy.getCustomName());
        assertEquals(enchanted.getEnchantments(), copy.getEnchantments());
        assertEquals("test", copy.getMetadata().get("origin"));
        assertTrue(enchanted.canMergeWith(copy));

        ItemStack potion = new ItemStack(ItemType.POTION, 1);
        potion.setPotionData(new PotionData(PotionType.POISON, false, false, false));
        ItemStack splashPotion = potion.copy();
        splashPotion.setPotionData(new PotionData(PotionType.POISON, true, false, false));

        assertFalse(potion.canMergeWith(splashPotion));
    }

    @Test
    @DisplayName("Inventory should split stacks by max size")
    void inventorySplitsStacksByMaxSize() {
        Inventory inventory = new Inventory();
        ItemStack incoming = new ItemStack(ItemType.DIRT, 70);

        assertTrue(inventory.addItem(incoming));

        assertTrue(incoming.isEmpty());
        assertEquals(64, inventory.getHotbar()[0].getCount());
        assertEquals(6, inventory.getHotbar()[1].getCount());
    }

    @Test
    @DisplayName("Inventory should split signs by their Release 1.0 stack size")
    void inventorySplitsSignsByReleaseStackSize() {
        Inventory inventory = new Inventory();
        ItemStack incoming = new ItemStack(ItemType.SIGN, 17);

        assertTrue(inventory.addItem(incoming));

        assertTrue(incoming.isEmpty());
        assertEquals(16, inventory.getHotbar()[0].getCount());
        assertEquals(1, inventory.getHotbar()[1].getCount());
    }

    @Test
    @DisplayName("Inventory should split golden apples by their Release 1.0 stack size")
    void inventorySplitsGoldenApplesByReleaseStackSize() {
        Inventory inventory = new Inventory();
        ItemStack incoming = new ItemStack(ItemType.GOLDEN_APPLE, 65);

        assertTrue(inventory.addItem(incoming));

        assertTrue(incoming.isEmpty());
        assertEquals(64, inventory.getHotbar()[0].getCount());
        assertEquals(1, inventory.getHotbar()[1].getCount());
    }

    @Test
    @DisplayName("Inventory should preserve tool durability when moving stacks")
    void inventoryPreservesToolDurability() {
        Inventory inventory = new Inventory();
        ItemStack incoming = new ItemStack(ItemType.STONE_PICKAXE, 1);
        incoming.useDurability();
        incoming.useDurability();
        int durability = incoming.getDurability();

        assertTrue(inventory.addItem(incoming));

        assertTrue(incoming.isEmpty());
        assertEquals(durability, inventory.getHotbar()[0].getDurability());
    }
}
