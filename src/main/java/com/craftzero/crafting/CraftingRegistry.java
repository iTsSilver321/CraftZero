package com.craftzero.crafting;

import com.craftzero.inventory.ItemType;

import java.util.ArrayList;
import java.util.List;

/**
 * Registry of crafting recipes for the player inventory and crafting table.
 */
public class CraftingRegistry {

        private static final List<CraftingRecipe> recipes2x2 = new ArrayList<>();
        private static final List<CraftingRecipe> recipes3x3 = new ArrayList<>();

        static {
                register2x2Recipes();
                register3x3Recipes();
        }

        private static void register2x2Recipes() {
                recipes2x2.add(new CraftingRecipe(
                                new ItemType[] { ItemType.OAK_LOG, null, null, null },
                                ItemType.OAK_PLANKS, 4, true));

                recipes2x2.add(new CraftingRecipe(
                                new ItemType[] { ItemType.OAK_PLANKS, null, ItemType.OAK_PLANKS, null },
                                ItemType.STICK, 4));
                recipes2x2.add(new CraftingRecipe(
                                new ItemType[] { null, ItemType.OAK_PLANKS, null, ItemType.OAK_PLANKS },
                                ItemType.STICK, 4));

                recipes2x2.add(new CraftingRecipe(
                                new ItemType[] { ItemType.OAK_PLANKS, ItemType.OAK_PLANKS,
                                                ItemType.OAK_PLANKS, ItemType.OAK_PLANKS },
                                ItemType.CRAFTING_TABLE, 1));

                recipes2x2.add(new CraftingRecipe(
                                new ItemType[] { ItemType.COAL, null, ItemType.STICK, null },
                                ItemType.TORCH, 4));
                recipes2x2.add(new CraftingRecipe(
                                new ItemType[] { null, ItemType.COAL, null, ItemType.STICK },
                                ItemType.TORCH, 4));
                recipes2x2.add(new CraftingRecipe(
                                new ItemType[] { ItemType.CHARCOAL, null, ItemType.STICK, null },
                                ItemType.TORCH, 4));
                recipes2x2.add(new CraftingRecipe(
                                new ItemType[] { null, ItemType.CHARCOAL, null, ItemType.STICK },
                                ItemType.TORCH, 4));
        }

        private static void register3x3Recipes() {
                ItemType P = ItemType.OAK_PLANKS;
                ItemType S = ItemType.STICK;
                ItemType C = ItemType.COBBLESTONE;
                ItemType I = ItemType.IRON_INGOT;
                ItemType D = ItemType.DIAMOND;

                addToolSet(P, S, ItemType.WOODEN_PICKAXE, ItemType.WOODEN_SHOVEL,
                                ItemType.WOODEN_AXE, ItemType.WOODEN_SWORD);
                addToolSet(C, S, ItemType.STONE_PICKAXE, ItemType.STONE_SHOVEL,
                                ItemType.STONE_AXE, ItemType.STONE_SWORD);
                addToolSet(I, S, ItemType.IRON_PICKAXE, ItemType.IRON_SHOVEL,
                                ItemType.IRON_AXE, ItemType.IRON_SWORD);
                addToolSet(D, S, ItemType.DIAMOND_PICKAXE, ItemType.DIAMOND_SHOVEL,
                                ItemType.DIAMOND_AXE, ItemType.DIAMOND_SWORD);

                for (int i = 0; i < 9; i++) {
                        ItemType[] pattern = new ItemType[9];
                        pattern[i] = ItemType.OAK_LOG;
                        recipes3x3.add(new CraftingRecipe(pattern, ItemType.OAK_PLANKS, 4, true, 3));
                }

                recipes3x3.add(CraftingRecipe.create3x3(
                                new ItemType[] { P, null, null, P, null, null, null, null, null },
                                ItemType.STICK, 4));
                recipes3x3.add(CraftingRecipe.create3x3(
                                new ItemType[] { null, P, null, null, P, null, null, null, null },
                                ItemType.STICK, 4));
                recipes3x3.add(CraftingRecipe.create3x3(
                                new ItemType[] { null, null, P, null, null, P, null, null, null },
                                ItemType.STICK, 4));
                recipes3x3.add(CraftingRecipe.create3x3(
                                new ItemType[] { null, null, null, P, null, null, P, null, null },
                                ItemType.STICK, 4));
                recipes3x3.add(CraftingRecipe.create3x3(
                                new ItemType[] { null, null, null, null, P, null, null, P, null },
                                ItemType.STICK, 4));
                recipes3x3.add(CraftingRecipe.create3x3(
                                new ItemType[] { null, null, null, null, null, P, null, null, P },
                                ItemType.STICK, 4));

                recipes3x3.add(CraftingRecipe.create3x3(
                                new ItemType[] { P, P, null, P, P, null, null, null, null },
                                ItemType.CRAFTING_TABLE, 1));
                recipes3x3.add(CraftingRecipe.create3x3(
                                new ItemType[] { null, P, P, null, P, P, null, null, null },
                                ItemType.CRAFTING_TABLE, 1));
                recipes3x3.add(CraftingRecipe.create3x3(
                                new ItemType[] { null, null, null, P, P, null, P, P, null },
                                ItemType.CRAFTING_TABLE, 1));
                recipes3x3.add(CraftingRecipe.create3x3(
                                new ItemType[] { null, null, null, null, P, P, null, P, P },
                                ItemType.CRAFTING_TABLE, 1));

                recipes3x3.add(CraftingRecipe.create3x3(
                                new ItemType[] { P, P, P, P, null, P, P, P, P },
                                ItemType.CHEST, 1));

                recipes3x3.add(CraftingRecipe.create3x3(
                                new ItemType[] { C, C, C, C, null, C, C, C, C },
                                ItemType.FURNACE, 1));

                addTorch3x3(ItemType.COAL);
                addTorch3x3(ItemType.CHARCOAL);

                addBowRecipes(S, ItemType.STRING);
                addArrowRecipes(ItemType.FLINT, S, ItemType.FEATHER);

                recipes3x3.add(CraftingRecipe.create3x3(
                                new ItemType[] { I, null, I, null, I, null, null, null, null },
                                ItemType.BUCKET, 1));
                recipes3x3.add(CraftingRecipe.create3x3(
                                new ItemType[] { null, null, null, I, null, I, null, I, null },
                                ItemType.BUCKET, 1));

                recipes3x3.add(CraftingRecipe.create3x3(
                                new ItemType[] { S, null, S, S, S, S, S, null, S },
                                ItemType.LADDER, 2));
                recipes3x3.add(CraftingRecipe.create3x3(
                                new ItemType[] { S, S, null, S, S, null, S, S, null },
                                ItemType.LADDER, 2));

                addDoorRecipes(P, ItemType.WOODEN_DOOR);
                addDoorRecipes(I, ItemType.IRON_DOOR);

                recipes3x3.add(CraftingRecipe.create3x3(
                                new ItemType[] { P, P, P, P, P, P, null, null, null },
                                ItemType.TRAPDOOR, 2));
                recipes3x3.add(CraftingRecipe.create3x3(
                                new ItemType[] { null, null, null, P, P, P, P, P, P },
                                ItemType.TRAPDOOR, 2));

                recipes3x3.add(CraftingRecipe.create3x3(
                                new ItemType[] { P, P, P, P, P, P, null, S, null },
                                ItemType.SIGN, 1));

                recipes3x3.add(CraftingRecipe.create3x3(
                                new ItemType[] { ItemType.WHITE_WOOL, ItemType.WHITE_WOOL, ItemType.WHITE_WOOL,
                                                P, P, P, null, null, null },
                                ItemType.BED, 1));

                recipes3x3.add(CraftingRecipe.create3x3(
                                new ItemType[] { S, S, S, S, S, S, null, null, null },
                                ItemType.FENCE, 2));
                recipes3x3.add(CraftingRecipe.create3x3(
                                new ItemType[] { null, null, null, S, S, S, S, S, S },
                                ItemType.FENCE, 2));
                recipes3x3.add(CraftingRecipe.create3x3(
                                new ItemType[] { S, P, S, S, P, S, null, null, null },
                                ItemType.FENCE_GATE, 1));
                recipes3x3.add(CraftingRecipe.create3x3(
                                new ItemType[] { null, null, null, S, P, S, S, P, S },
                                ItemType.FENCE_GATE, 1));

                addSlabRecipe(ItemType.STONE, ItemType.STONE_SLAB);
                addSlabRecipe(ItemType.SANDSTONE, ItemType.STONE_SLAB);
                addSlabRecipe(P, ItemType.STONE_SLAB);
                addSlabRecipe(C, ItemType.STONE_SLAB);
                addSlabRecipe(ItemType.BRICK, ItemType.STONE_SLAB);
                addSlabRecipe(ItemType.STONE_BRICK, ItemType.STONE_SLAB);

                addStairsRecipe(P, ItemType.OAK_STAIRS);
                addStairsRecipe(C, ItemType.COBBLESTONE_STAIRS);
                addStairsRecipe(ItemType.BRICK, ItemType.BRICK_STAIRS);
                addStairsRecipe(ItemType.STONE_BRICK, ItemType.STONE_BRICK_STAIRS);
                addStairsRecipe(ItemType.NETHER_BRICK, ItemType.NETHER_BRICK_STAIRS);
        }

        private static void addTorch3x3(ItemType fuel) {
                recipes3x3.add(CraftingRecipe.create3x3(
                                new ItemType[] { fuel, null, null, ItemType.STICK, null, null, null, null, null },
                                ItemType.TORCH, 4));
                recipes3x3.add(CraftingRecipe.create3x3(
                                new ItemType[] { null, fuel, null, null, ItemType.STICK, null, null, null, null },
                                ItemType.TORCH, 4));
                recipes3x3.add(CraftingRecipe.create3x3(
                                new ItemType[] { null, null, fuel, null, null, ItemType.STICK, null, null, null },
                                ItemType.TORCH, 4));
                recipes3x3.add(CraftingRecipe.create3x3(
                                new ItemType[] { null, null, null, fuel, null, null, ItemType.STICK, null, null },
                                ItemType.TORCH, 4));
                recipes3x3.add(CraftingRecipe.create3x3(
                                new ItemType[] { null, null, null, null, fuel, null, null, ItemType.STICK, null },
                                ItemType.TORCH, 4));
                recipes3x3.add(CraftingRecipe.create3x3(
                                new ItemType[] { null, null, null, null, null, fuel, null, null, ItemType.STICK },
                                ItemType.TORCH, 4));
        }

        private static void addBowRecipes(ItemType stick, ItemType string) {
                recipes3x3.add(CraftingRecipe.create3x3(
                                new ItemType[] { null, stick, string, stick, null, string, null, stick, string },
                                ItemType.BOW, 1));
                recipes3x3.add(CraftingRecipe.create3x3(
                                new ItemType[] { string, stick, null, string, null, stick, string, stick, null },
                                ItemType.BOW, 1));
        }

        private static void addArrowRecipes(ItemType flint, ItemType stick, ItemType feather) {
                recipes3x3.add(CraftingRecipe.create3x3(
                                new ItemType[] { flint, null, null, stick, null, null, feather, null, null },
                                ItemType.ARROW, 4));
                recipes3x3.add(CraftingRecipe.create3x3(
                                new ItemType[] { null, flint, null, null, stick, null, null, feather, null },
                                ItemType.ARROW, 4));
                recipes3x3.add(CraftingRecipe.create3x3(
                                new ItemType[] { null, null, flint, null, null, stick, null, null, feather },
                                ItemType.ARROW, 4));
        }

        private static void addDoorRecipes(ItemType material, ItemType output) {
                recipes3x3.add(CraftingRecipe.create3x3(
                                new ItemType[] { material, material, null, material, material, null, material,
                                                material, null },
                                output, 1));
                recipes3x3.add(CraftingRecipe.create3x3(
                                new ItemType[] { null, material, material, null, material, material, null, material,
                                                material },
                                output, 1));
        }

        private static void addSlabRecipe(ItemType material, ItemType output) {
                recipes3x3.add(CraftingRecipe.create3x3(
                                new ItemType[] { material, material, material, null, null, null, null, null, null },
                                output, 3));
                recipes3x3.add(CraftingRecipe.create3x3(
                                new ItemType[] { null, null, null, material, material, material, null, null, null },
                                output, 3));
                recipes3x3.add(CraftingRecipe.create3x3(
                                new ItemType[] { null, null, null, null, null, null, material, material, material },
                                output, 3));
        }

        private static void addStairsRecipe(ItemType material, ItemType output) {
                recipes3x3.add(CraftingRecipe.create3x3(
                                new ItemType[] { material, null, null, material, material, null, material, material,
                                                material },
                                output, 4));
                recipes3x3.add(CraftingRecipe.create3x3(
                                new ItemType[] { null, null, material, null, material, material, material, material,
                                                material },
                                output, 4));
        }

        private static void addToolSet(ItemType material, ItemType stick, ItemType pickaxe, ItemType shovel,
                        ItemType axe, ItemType sword) {
                recipes3x3.add(CraftingRecipe.create3x3(
                                new ItemType[] { material, material, material, null, stick, null, null, stick, null },
                                pickaxe, 1));
                recipes3x3.add(CraftingRecipe.create3x3(
                                new ItemType[] { null, material, null, null, stick, null, null, stick, null },
                                shovel, 1));
                recipes3x3.add(CraftingRecipe.create3x3(
                                new ItemType[] { material, material, null, material, stick, null, null, stick, null },
                                axe, 1));
                recipes3x3.add(CraftingRecipe.create3x3(
                                new ItemType[] { null, material, null, null, material, null, null, stick, null },
                                sword, 1));
        }

        public static CraftingRecipe findRecipe(ItemType[] grid) {
                return findRecipe2x2(grid);
        }

        public static CraftingRecipe findRecipe2x2(ItemType[] grid) {
                for (CraftingRecipe recipe : recipes2x2) {
                        if (recipe.matches(grid)) {
                                return recipe;
                        }
                }
                return null;
        }

        public static CraftingRecipe findRecipe3x3(ItemType[] grid) {
                for (CraftingRecipe recipe : recipes3x3) {
                        if (recipe.matches(grid)) {
                                return recipe;
                        }
                }
                return null;
        }

        public static List<CraftingRecipe> getAllRecipes() {
                List<CraftingRecipe> all = new ArrayList<>();
                all.addAll(recipes2x2);
                all.addAll(recipes3x3);
                return all;
        }
}
