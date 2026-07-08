package com.craftzero.crafting;

import com.craftzero.inventory.ItemType;
import com.craftzero.inventory.ItemStack;
import com.craftzero.inventory.MapItemData;
import com.craftzero.world.World;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CraftingRegistryTest {
    @Test
    @DisplayName("Release 1.0 crafting registry should include the vanilla recipe table")
    void releaseOneRecipeCountMatchesVanilla() {
        assertEquals(174, CraftingRegistry.getAllRecipes().size());
    }


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

        assertRecipe(CraftingRegistry.findRecipe3x3(pickaxe(ItemType.GOLD_INGOT)),
                ItemType.GOLD_PICKAXE, 1);
        assertRecipe(CraftingRegistry.findRecipe3x3(hoe(ItemType.IRON_INGOT)),
                ItemType.IRON_HOE, 1);
        assertRecipe(CraftingRegistry.findRecipe3x3(new ItemType[] {
                null, ItemType.DIAMOND, ItemType.DIAMOND,
                null, ItemType.STICK, ItemType.DIAMOND,
                null, ItemType.STICK, null
        }), ItemType.DIAMOND_AXE, 1);
    }

    @Test
    @DisplayName("Tool recipes should not accept raw ore blocks")
    void toolRecipesDoNotUseOreBlocks() {
        assertNull(CraftingRegistry.findRecipe3x3(pickaxe(ItemType.IRON_ORE)));
        assertNull(CraftingRegistry.findRecipe3x3(pickaxe(ItemType.DIAMOND_ORE)));
    }

    @Test
    @DisplayName("Release 1.0 crafting repair should combine two matching damaged single items")
    void repairRecipeCombinesDamagedItems() {
        ItemStack first = damaged(ItemType.IRON_PICKAXE, 10);
        ItemStack second = damaged(ItemType.IRON_PICKAXE, 20);

        CraftingRecipe recipe = CraftingRegistry.findRecipe2x2(new ItemStack[] {
                first, null,
                null, second
        });

        assertRecipe(recipe, ItemType.IRON_PICKAXE, 1);
        int expectedDurability = Math.min(ItemType.IRON_PICKAXE.getMaxDurability(),
                10 + 20 + ItemType.IRON_PICKAXE.getMaxDurability() * 5 / 100);
        assertEquals(expectedDurability, recipe.getOutput().getDurability());
    }

    @Test
    @DisplayName("Release 1.0 crafting repair should reject stacked, mismatched, or non-damageable inputs")
    void repairRecipeRequiresTwoSingleMatchingDamageableStacks() {
        assertNull(CraftingRegistry.findRecipe2x2(new ItemStack[] {
                new ItemStack(ItemType.IRON_PICKAXE, 2), new ItemStack(ItemType.IRON_PICKAXE, 1),
                null, null
        }));

        assertNull(CraftingRegistry.findRecipe2x2(new ItemStack[] {
                new ItemStack(ItemType.IRON_PICKAXE, 1), new ItemStack(ItemType.IRON_SHOVEL, 1),
                null, null
        }));

        assertNull(CraftingRegistry.findRecipe2x2(new ItemStack[] {
                new ItemStack(ItemType.DIRT, 1), new ItemStack(ItemType.DIRT, 1),
                null, null
        }));
    }

    @Test
    @DisplayName("Release 1.0 map copying should preserve initialized map metadata")
    void mapCopyRecipePreservesInitializedMapData() {
        World world = new World(9014L);
        try {
            ItemStack exploredMap = new ItemStack(ItemType.MAP, 1);
            MapItemData.useMap(world, exploredMap, 0.5f, 0.5f);
            ItemStack blankMap = new ItemStack(ItemType.MAP, 1);

            CraftingRecipe recipe = CraftingRegistry.findRecipe3x3(new ItemStack[] {
                    exploredMap, blankMap, null,
                    null, null, null,
                    null, null, null
            });

            assertRecipe(recipe, ItemType.MAP, 2);
            ItemStack output = recipe.getOutput();
            assertTrue(MapItemData.isInitializedMap(output));
            assertEquals(exploredMap.getMetadata(), output.getMetadata());

            assertNull(CraftingRegistry.findRecipe3x3(new ItemStack[] {
                    new ItemStack(ItemType.MAP, 1), new ItemStack(ItemType.MAP, 1), null,
                    null, null, null,
                    null, null, null
            }));
        } finally {
            world.cleanup();
        }
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
    @DisplayName("Release 1.0 dispenser crafting should require an undamaged bow")
    void dispenserRecipeRequiresUndamagedBow() {
        ItemStack fullBow = new ItemStack(ItemType.BOW, 1);
        ItemStack damagedBow = damaged(ItemType.BOW, ItemType.BOW.getMaxDurability() - 1);

        assertRecipe(CraftingRegistry.findRecipe3x3(new ItemStack[] {
                new ItemStack(ItemType.COBBLESTONE, 1), new ItemStack(ItemType.COBBLESTONE, 1),
                        new ItemStack(ItemType.COBBLESTONE, 1),
                new ItemStack(ItemType.COBBLESTONE, 1), fullBow, new ItemStack(ItemType.COBBLESTONE, 1),
                new ItemStack(ItemType.COBBLESTONE, 1), new ItemStack(ItemType.REDSTONE, 1),
                        new ItemStack(ItemType.COBBLESTONE, 1)
        }), ItemType.DISPENSER, 1);

        assertNull(CraftingRegistry.findRecipe3x3(new ItemStack[] {
                new ItemStack(ItemType.COBBLESTONE, 1), new ItemStack(ItemType.COBBLESTONE, 1),
                        new ItemStack(ItemType.COBBLESTONE, 1),
                new ItemStack(ItemType.COBBLESTONE, 1), damagedBow, new ItemStack(ItemType.COBBLESTONE, 1),
                new ItemStack(ItemType.COBBLESTONE, 1), new ItemStack(ItemType.REDSTONE, 1),
                        new ItemStack(ItemType.COBBLESTONE, 1)
        }));
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

        assertRecipe(CraftingRegistry.findRecipe3x3(new ItemType[] {
                ItemType.SANDSTONE, ItemType.SANDSTONE, ItemType.SANDSTONE,
                null, null, null,
                null, null, null
        }), ItemType.SANDSTONE_SLAB, 3);
    }

    @Test
    @DisplayName("Release 1.0 crafting should include armor recipes")
    void armorRecipesWork() {
        assertRecipe(CraftingRegistry.findRecipe3x3(new ItemType[] {
                ItemType.LEATHER, ItemType.LEATHER, ItemType.LEATHER,
                ItemType.LEATHER, null, ItemType.LEATHER,
                null, null, null
        }), ItemType.LEATHER_HELMET, 1);

        assertRecipe(CraftingRegistry.findRecipe3x3(new ItemType[] {
                ItemType.IRON_INGOT, null, ItemType.IRON_INGOT,
                ItemType.IRON_INGOT, ItemType.IRON_INGOT, ItemType.IRON_INGOT,
                ItemType.IRON_INGOT, ItemType.IRON_INGOT, ItemType.IRON_INGOT
        }), ItemType.IRON_CHESTPLATE, 1);

        assertRecipe(CraftingRegistry.findRecipe3x3(new ItemType[] {
                ItemType.DIAMOND, ItemType.DIAMOND, ItemType.DIAMOND,
                ItemType.DIAMOND, null, ItemType.DIAMOND,
                ItemType.DIAMOND, null, ItemType.DIAMOND
        }), ItemType.DIAMOND_LEGGINGS, 1);

        assertRecipe(CraftingRegistry.findRecipe3x3(new ItemType[] {
                ItemType.GOLD_INGOT, null, ItemType.GOLD_INGOT,
                ItemType.GOLD_INGOT, null, ItemType.GOLD_INGOT,
                null, null, null
        }), ItemType.GOLD_BOOTS, 1);

        assertRecipe(CraftingRegistry.findRecipe3x3(new ItemType[] {
                ItemType.FIRE, ItemType.FIRE, ItemType.FIRE,
                ItemType.FIRE, null, ItemType.FIRE,
                null, null, null
        }), ItemType.CHAIN_HELMET, 1);
    }

    @Test
    @DisplayName("Release 1.0 crafting should include block compression and decompression")
    void storageAndBuildingRecipesWork() {
        assertRecipe(CraftingRegistry.findRecipe2x2(new ItemType[] {
                ItemType.SPRUCE_LOG, null,
                null, null
        }), ItemType.OAK_PLANKS, 4);

        assertRecipe(CraftingRegistry.findRecipe2x2(new ItemType[] {
                ItemType.SAND, ItemType.SAND,
                ItemType.SAND, ItemType.SAND
        }), ItemType.SANDSTONE, 1);

        assertRecipe(CraftingRegistry.findRecipe3x3(new ItemType[] {
                ItemType.LAPIS_LAZULI, ItemType.LAPIS_LAZULI, ItemType.LAPIS_LAZULI,
                ItemType.LAPIS_LAZULI, ItemType.LAPIS_LAZULI, ItemType.LAPIS_LAZULI,
                ItemType.LAPIS_LAZULI, ItemType.LAPIS_LAZULI, ItemType.LAPIS_LAZULI
        }), ItemType.LAPIS_BLOCK, 1);

        assertRecipe(CraftingRegistry.findRecipe2x2(new ItemType[] {
                ItemType.LAPIS_BLOCK, null,
                null, null
        }), ItemType.LAPIS_LAZULI, 9);

        assertRecipe(CraftingRegistry.findRecipe3x3(new ItemType[] {
                ItemType.GOLD_NUGGET, ItemType.GOLD_NUGGET, ItemType.GOLD_NUGGET,
                ItemType.GOLD_NUGGET, ItemType.GOLD_NUGGET, ItemType.GOLD_NUGGET,
                ItemType.GOLD_NUGGET, ItemType.GOLD_NUGGET, ItemType.GOLD_NUGGET
        }), ItemType.GOLD_INGOT, 1);
    }

    @Test
    @DisplayName("Release 1.0 food and utility recipes should work")
    void foodAndUtilityRecipesWork() {
        assertRecipe(CraftingRegistry.findRecipe3x3(new ItemType[] {
                ItemType.MILK_BUCKET, ItemType.MILK_BUCKET, ItemType.MILK_BUCKET,
                ItemType.SUGAR, ItemType.EGG, ItemType.SUGAR,
                ItemType.WHEAT, ItemType.WHEAT, ItemType.WHEAT
        }), ItemType.CAKE, 1);

        CraftingRecipe cake = CraftingRegistry.findRecipe3x3(new ItemType[] {
                ItemType.MILK_BUCKET, ItemType.MILK_BUCKET, ItemType.MILK_BUCKET,
                ItemType.SUGAR, ItemType.EGG, ItemType.SUGAR,
                ItemType.WHEAT, ItemType.WHEAT, ItemType.WHEAT
        });
        assertArrayEquals(new ItemType[] {
                ItemType.BUCKET, ItemType.BUCKET, ItemType.BUCKET,
                null, null, null,
                null, null, null
        }, cake.getRemainingItems(new ItemType[] {
                ItemType.MILK_BUCKET, ItemType.MILK_BUCKET, ItemType.MILK_BUCKET,
                ItemType.SUGAR, ItemType.EGG, ItemType.SUGAR,
                ItemType.WHEAT, ItemType.WHEAT, ItemType.WHEAT
        }));

        assertRecipe(CraftingRegistry.findRecipe3x3(new ItemType[] {
                ItemType.WHEAT, ItemType.COCOA_BEANS, ItemType.WHEAT,
                null, null, null,
                null, null, null
        }), ItemType.COOKIE, 8);

        assertRecipe(CraftingRegistry.findRecipe3x3(new ItemType[] {
                ItemType.GOLD_BLOCK, ItemType.GOLD_BLOCK, ItemType.GOLD_BLOCK,
                ItemType.GOLD_BLOCK, ItemType.APPLE, ItemType.GOLD_BLOCK,
                ItemType.GOLD_BLOCK, ItemType.GOLD_BLOCK, ItemType.GOLD_BLOCK
        }), ItemType.GOLDEN_APPLE, 1);

        assertRecipe(CraftingRegistry.findRecipe3x3(new ItemType[] {
                ItemType.PAPER, ItemType.PAPER, ItemType.PAPER,
                ItemType.PAPER, ItemType.COMPASS, ItemType.PAPER,
                ItemType.PAPER, ItemType.PAPER, ItemType.PAPER
        }), ItemType.MAP, 1);

        assertRecipe(CraftingRegistry.findRecipe3x3(new ItemType[] {
                ItemType.OAK_PLANKS, null, ItemType.OAK_PLANKS,
                null, ItemType.OAK_PLANKS, null,
                null, null, null
        }), ItemType.BOWL, 4);

        assertRecipe(CraftingRegistry.findRecipe3x3(new ItemType[] {
                ItemType.OAK_PLANKS, null, ItemType.OAK_PLANKS,
                ItemType.OAK_PLANKS, ItemType.OAK_PLANKS, ItemType.OAK_PLANKS,
                null, null, null
        }), ItemType.BOAT, 1);
    }

    @Test
    @DisplayName("Release 1.0 dye and wool recipes should work")
    void dyeRecipesWork() {
        assertRecipe(CraftingRegistry.findRecipe2x2(new ItemType[] {
                ItemType.BONE, null,
                null, null
        }), ItemType.BONE_MEAL, 3);

        assertRecipe(CraftingRegistry.findRecipe2x2(new ItemType[] {
                ItemType.RED_ROSE, null,
                null, null
        }), ItemType.ROSE_RED, 2);

        assertRecipe(CraftingRegistry.findRecipe2x2(new ItemType[] {
                ItemType.LAPIS_LAZULI, ItemType.ROSE_RED,
                null, null
        }), ItemType.PURPLE_DYE, 2);

        assertRecipe(CraftingRegistry.findRecipe2x2(new ItemType[] {
                ItemType.WHITE_WOOL, ItemType.INK_SAC,
                null, null
        }), ItemType.BLACK_WOOL, 1);

        assertRecipe(CraftingRegistry.findRecipe2x2(new ItemType[] {
                ItemType.WHITE_WOOL, ItemType.ORANGE_DYE,
                null, null
        }), ItemType.ORANGE_WOOL, 1);
    }

    @Test
    @DisplayName("Release 1.0 brewing and End recipes should work")
    void brewingAndEndRecipesWork() {
        assertRecipe(CraftingRegistry.findRecipe2x2(new ItemType[] {
                ItemType.BLAZE_ROD, null,
                null, null
        }), ItemType.BLAZE_POWDER, 2);

        assertRecipe(CraftingRegistry.findRecipe2x2(new ItemType[] {
                ItemType.MELON_SLICE, ItemType.GOLD_NUGGET,
                null, null
        }), ItemType.GLISTERING_MELON, 1);

        assertRecipe(CraftingRegistry.findRecipe3x3(new ItemType[] {
                null, ItemType.BLAZE_ROD, null,
                ItemType.COBBLESTONE, ItemType.COBBLESTONE, ItemType.COBBLESTONE,
                null, null, null
        }), ItemType.BREWING_STAND, 1);

        assertRecipe(CraftingRegistry.findRecipe3x3(new ItemType[] {
                null, ItemType.BOOK, null,
                ItemType.DIAMOND, ItemType.OBSIDIAN, ItemType.DIAMOND,
                ItemType.OBSIDIAN, ItemType.OBSIDIAN, ItemType.OBSIDIAN
        }), ItemType.ENCHANTING_TABLE, 1);

        assertRecipe(CraftingRegistry.findRecipe2x2(new ItemType[] {
                ItemType.ENDER_PEARL, ItemType.BLAZE_POWDER,
                null, null
        }), ItemType.EYE_OF_ENDER, 1);

        assertNull(CraftingRegistry.findRecipe3x3(new ItemType[] {
                ItemType.STONE, null, null,
                null, null, null,
                null, null, null
        }));
        assertRecipe(CraftingRegistry.findRecipe3x3(new ItemType[] {
                ItemType.STONE, null, null,
                ItemType.STONE, null, null,
                null, null, null
        }), ItemType.STONE_BUTTON, 1);
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

    private static ItemType[] hoe(ItemType material) {
        return new ItemType[] {
                material, material, null,
                null, ItemType.STICK, null,
                null, ItemType.STICK, null
        };
    }

    private static ItemStack damaged(ItemType type, int durability) {
        ItemStack stack = new ItemStack(type, 1);
        stack.setDurability(durability);
        return stack;
    }

    private static void assertRecipe(CraftingRecipe recipe, ItemType output, int count) {
        assertNotNull(recipe);
        assertSame(output, recipe.getOutputType());
        assertEquals(count, recipe.getOutputCount());
    }
}
