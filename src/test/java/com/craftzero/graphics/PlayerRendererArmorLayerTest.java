package com.craftzero.graphics;

import com.craftzero.inventory.ItemStack;
import com.craftzero.inventory.ItemType;
import com.craftzero.progression.ArmorMaterial;
import com.craftzero.progression.ArmorSlot;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayerRendererArmorLayerTest {
    @Test
    @DisplayName("Armor rendering should keep equipped slots and materials separate")
    void armorLayersKeepSlotsAndMaterialsSeparate() {
        ItemStack[] armor = new ItemStack[4];
        armor[ArmorSlot.HELMET.getIndex()] = new ItemStack(ItemType.LEATHER_HELMET, 1);
        armor[ArmorSlot.CHESTPLATE.getIndex()] = new ItemStack(ItemType.DIAMOND_CHESTPLATE, 1);
        armor[ArmorSlot.LEGGINGS.getIndex()] = new ItemStack(ItemType.IRON_LEGGINGS, 1);
        armor[ArmorSlot.BOOTS.getIndex()] = new ItemStack(ItemType.GOLD_BOOTS, 1);

        List<PlayerRenderer.ArmorRenderLayer> layers = PlayerRenderer.armorRenderLayers(armor);

        assertEquals(4, layers.size());

        PlayerRenderer.ArmorRenderLayer helmet = layers.get(0);
        assertEquals(ArmorSlot.HELMET, helmet.slot());
        assertEquals(ArmorMaterial.LEATHER, helmet.material());
        assertEquals(1, helmet.textureLayer());
        assertEquals("/textures/armor/cloth_1.png", helmet.texturePath());
        assertTrue(helmet.renders(PlayerRenderer.ArmorModelPart.HEAD));
        assertFalse(helmet.renders(PlayerRenderer.ArmorModelPart.BODY));

        PlayerRenderer.ArmorRenderLayer chest = layers.get(1);
        assertEquals(ArmorSlot.CHESTPLATE, chest.slot());
        assertEquals(ArmorMaterial.DIAMOND, chest.material());
        assertEquals("/textures/armor/diamond_1.png", chest.texturePath());
        assertTrue(chest.renders(PlayerRenderer.ArmorModelPart.BODY));
        assertTrue(chest.renders(PlayerRenderer.ArmorModelPart.RIGHT_ARM));
        assertTrue(chest.renders(PlayerRenderer.ArmorModelPart.LEFT_ARM));
        assertFalse(chest.renders(PlayerRenderer.ArmorModelPart.HEAD));
        assertFalse(chest.renders(PlayerRenderer.ArmorModelPart.RIGHT_LEG));

        PlayerRenderer.ArmorRenderLayer leggings = layers.get(2);
        assertEquals(ArmorSlot.LEGGINGS, leggings.slot());
        assertEquals(ArmorMaterial.IRON, leggings.material());
        assertEquals(2, leggings.textureLayer());
        assertEquals("/textures/armor/iron_2.png", leggings.texturePath());
        assertTrue(leggings.renders(PlayerRenderer.ArmorModelPart.BODY));
        assertTrue(leggings.renders(PlayerRenderer.ArmorModelPart.RIGHT_LEG));
        assertTrue(leggings.renders(PlayerRenderer.ArmorModelPart.LEFT_LEG));
        assertFalse(leggings.renders(PlayerRenderer.ArmorModelPart.RIGHT_ARM));

        PlayerRenderer.ArmorRenderLayer boots = layers.get(3);
        assertEquals(ArmorSlot.BOOTS, boots.slot());
        assertEquals(ArmorMaterial.GOLD, boots.material());
        assertEquals("/textures/armor/gold_1.png", boots.texturePath());
        assertTrue(boots.renders(PlayerRenderer.ArmorModelPart.RIGHT_LEG));
        assertTrue(boots.renders(PlayerRenderer.ArmorModelPart.LEFT_LEG));
        assertFalse(boots.renders(PlayerRenderer.ArmorModelPart.BODY));
    }

    @Test
    @DisplayName("Empty and non-armor stacks should not create armor render layers")
    void armorLayersSkipEmptyAndNonArmorStacks() {
        ItemStack[] armor = new ItemStack[4];
        armor[ArmorSlot.HELMET.getIndex()] = new ItemStack(ItemType.PUMPKIN, 1);
        armor[ArmorSlot.CHESTPLATE.getIndex()] = new ItemStack(ItemType.STONE, 1);

        assertTrue(PlayerRenderer.armorRenderLayers(armor).isEmpty());
        assertTrue(PlayerRenderer.armorRenderLayers(null).isEmpty());
    }
}
