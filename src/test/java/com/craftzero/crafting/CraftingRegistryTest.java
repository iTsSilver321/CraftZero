package com.craftzero.crafting;

import com.craftzero.inventory.ItemType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CraftingRegistryTest {

    @Test
    @DisplayName("2x2 crafting should make planks, sticks, and crafting tables")
    void twoByTwoRecipesWork() {
        assertRecipe(CraftingRegistry.findRecipe2x2(new ItemType[] {
                ItemType.OAK_LOG, null,
                null, null
        }), ItemType.OAK_PLANKS, 4);

        assertRecipe(CraftingRegistry.findRecipe2x2(new ItemType[] {
                ItemType.OAK_PLANKS, null,
                ItemType.OAK_PLANKS, null
        }), ItemType.STICK, 4);

        assertRecipe(CraftingRegistry.findRecipe2x2(new ItemType[] {
                ItemType.OAK_PLANKS, ItemType.OAK_PLANKS,
                ItemType.OAK_PLANKS, ItemType.OAK_PLANKS
        }), ItemType.CRAFTING_TABLE, 1);
    }

    @Test
    @DisplayName("3x3 crafting should make wood, stone, iron, and diamond tools")
    void toolRecipesUseProcessedMaterials() {
        assertRecipe(CraftingRegistry.findRecipe3x3(pickaxe(ItemType.OAK_PLANKS)),
                ItemType.WOODEN_PICKAXE, 1);
        assertRecipe(CraftingRegistry.findRecipe3x3(pickaxe(ItemType.COBBLESTONE)),
                ItemType.STONE_PICKAXE, 1);
        assertRecipe(CraftingRegistry.findRecipe3x3(pickaxe(ItemType.IRON_INGOT)),
                ItemType.IRON_PICKAXE, 1);
        assertRecipe(CraftingRegistry.findRecipe3x3(pickaxe(ItemType.DIAMOND)),
                ItemType.DIAMOND_PICKAXE, 1);

        assertRecipe(CraftingRegistry.findRecipe3x3(sword(ItemType.OAK_PLANKS)),
                ItemType.WOODEN_SWORD, 1);
        assertRecipe(CraftingRegistry.findRecipe3x3(sword(ItemType.COBBLESTONE)),
                ItemType.STONE_SWORD, 1);
        assertRecipe(CraftingRegistry.findRecipe3x3(sword(ItemType.IRON_INGOT)),
                ItemType.IRON_SWORD, 1);
        assertRecipe(CraftingRegistry.findRecipe3x3(sword(ItemType.DIAMOND)),
                ItemType.DIAMOND_SWORD, 1);
    }

    @Test
    @DisplayName("Tool recipes should not accept raw ore blocks")
    void toolRecipesDoNotUseOreBlocks() {
        assertNull(CraftingRegistry.findRecipe3x3(pickaxe(ItemType.IRON_ORE)));
        assertNull(CraftingRegistry.findRecipe3x3(pickaxe(ItemType.DIAMOND_ORE)));
    }

    @Test
    @DisplayName("3x3 crafting should make chest and furnace")
    void containerRecipesWork() {
        ItemType P = ItemType.OAK_PLANKS;
        ItemType C = ItemType.COBBLESTONE;

        assertRecipe(CraftingRegistry.findRecipe3x3(new ItemType[] {
                P, P, P,
                P, null, P,
                P, P, P
        }), ItemType.CHEST, 1);

        assertRecipe(CraftingRegistry.findRecipe3x3(new ItemType[] {
                C, C, C,
                C, null, C,
                C, C, C
        }), ItemType.FURNACE, 1);
    }

    @Test
    @DisplayName("3x3 crafting should make Release 1.0 interaction blocks")
    void interactionBlockRecipesWork() {
        ItemType P = ItemType.OAK_PLANKS;
        ItemType S = ItemType.STICK;

        assertRecipe(CraftingRegistry.findRecipe2x2(new ItemType[] {
                ItemType.COAL, null,
                S, null
        }), ItemType.TORCH, 4);

        assertRecipe(CraftingRegistry.findRecipe3x3(new ItemType[] {
                S, null, S,
                S, S, S,
                S, null, S
        }), ItemType.LADDER, 2);

        assertRecipe(CraftingRegistry.findRecipe3x3(new ItemType[] {
                P, P, null,
                P, P, null,
                P, P, null
        }), ItemType.WOODEN_DOOR, 1);

        assertRecipe(CraftingRegistry.findRecipe3x3(new ItemType[] {
                P, P, P,
                P, P, P,
                null, null, null
        }), ItemType.TRAPDOOR, 2);

        assertRecipe(CraftingRegistry.findRecipe3x3(new ItemType[] {
                P, P, P,
                P, P, P,
                null, S, null
        }), ItemType.SIGN, 1);

        assertRecipe(CraftingRegistry.findRecipe3x3(new ItemType[] {
                S, S, S,
                S, S, S,
                null, null, null
        }), ItemType.FENCE, 2);

        assertRecipe(CraftingRegistry.findRecipe3x3(new ItemType[] {
                S, P, S,
                S, P, S,
                null, null, null
        }), ItemType.FENCE_GATE, 1);

        assertRecipe(CraftingRegistry.findRecipe3x3(new ItemType[] {
                ItemType.WHITE_WOOL, ItemType.WHITE_WOOL, ItemType.WHITE_WOOL,
                P, P, P,
                null, null, null
        }), ItemType.BED, 1);
    }

    @Test
    @DisplayName("3x3 crafting should make bows and arrows")
    void bowAndArrowRecipesWork() {
        ItemType S = ItemType.STICK;
        ItemType T = ItemType.STRING;

        assertRecipe(CraftingRegistry.findRecipe3x3(new ItemType[] {
                null, S, T,
                S, null, T,
                null, S, T
        }), ItemType.BOW, 1);

        assertRecipe(CraftingRegistry.findRecipe3x3(new ItemType[] {
                T, S, null,
                T, null, S,
                T, S, null
        }), ItemType.BOW, 1);

        assertRecipe(CraftingRegistry.findRecipe3x3(new ItemType[] {
                null, ItemType.FLINT, null,
                null, S, null,
                null, ItemType.FEATHER, null
        }), ItemType.ARROW, 4);
    }

    @Test
    @DisplayName("Bucket recipe should use three iron ingots in a V")
    void bucketRecipeWorks() {
        assertRecipe(CraftingRegistry.findRecipe3x3(new ItemType[] {
                ItemType.IRON_INGOT, null, ItemType.IRON_INGOT,
                null, ItemType.IRON_INGOT, null,
                null, null, null
        }), ItemType.BUCKET, 1);
    }

    @Test
    @DisplayName("Slab and stair recipes should use Release 1.0 output counts")
    void slabAndStairRecipeCountsWork() {
        assertRecipe(CraftingRegistry.findRecipe3x3(new ItemType[] {
                ItemType.STONE, ItemType.STONE, ItemType.STONE,
                null, null, null,
                null, null, null
        }), ItemType.STONE_SLAB, 3);

        assertRecipe(CraftingRegistry.findRecipe3x3(new ItemType[] {
                ItemType.COBBLESTONE, null, null,
                ItemType.COBBLESTONE, ItemType.COBBLESTONE, null,
                ItemType.COBBLESTONE, ItemType.COBBLESTONE, ItemType.COBBLESTONE
        }), ItemType.COBBLESTONE_STAIRS, 4);
    }

    private static ItemType[] pickaxe(ItemType material) {
        return new ItemType[] {
                material, material, material,
                null, ItemType.STICK, null,
                null, ItemType.STICK, null
        };
    }

    private static ItemType[] sword(ItemType material) {
        return new ItemType[] {
                null, material, null,
                null, material, null,
                null, ItemType.STICK, null
        };
    }

    private static void assertRecipe(CraftingRecipe recipe, ItemType output, int count) {
        assertNotNull(recipe);
        assertSame(output, recipe.getOutputType());
        assertEquals(count, recipe.getOutputCount());
    }
}
