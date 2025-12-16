package com.craftzero.crafting;

import com.craftzero.world.BlockType;
import java.util.ArrayList;
import java.util.List;

/**
 * Registry of all crafting recipes.
 * Provides recipe lookup for 2x2 (inventory) and 3x3 (crafting table) grids.
 */
public class CraftingRegistry {

        private static final List<CraftingRecipe> recipes2x2 = new ArrayList<>();
        private static final List<CraftingRecipe> recipes3x3 = new ArrayList<>();

        static {
                register2x2Recipes();
                register3x3Recipes();
        }

        private static void register2x2Recipes() {
                // Log -> 4 Planks (shapeless - any slot)
                recipes2x2.add(new CraftingRecipe(
                                new BlockType[] { BlockType.OAK_LOG, null, null, null },
                                BlockType.OAK_PLANKS, 4, true));

                // 2 Planks (vertical) -> 4 Sticks
                recipes2x2.add(new CraftingRecipe(
                                new BlockType[] { BlockType.OAK_PLANKS, null, BlockType.OAK_PLANKS, null },
                                BlockType.STICK, 4));
                recipes2x2.add(new CraftingRecipe(
                                new BlockType[] { null, BlockType.OAK_PLANKS, null, BlockType.OAK_PLANKS },
                                BlockType.STICK, 4));

                // 4 Planks -> Crafting Table
                recipes2x2.add(new CraftingRecipe(
                                new BlockType[] { BlockType.OAK_PLANKS, BlockType.OAK_PLANKS,
                                                BlockType.OAK_PLANKS, BlockType.OAK_PLANKS },
                                BlockType.CRAFTING_TABLE, 1));
        }

        private static void register3x3Recipes() {
                BlockType P = BlockType.OAK_PLANKS; // Planks
                BlockType S = BlockType.STICK; // Stick
                BlockType C = BlockType.COBBLESTONE; // Cobblestone
                BlockType I = BlockType.IRON_ORE; // Iron (ore for now, ingots later)
                BlockType D = BlockType.DIAMOND_ORE; // Diamond (ore for now)

                // ===== PICKAXES =====
                // Pattern: [M][M][M]
                // [ ][S][ ]
                // [ ][S][ ]

                // Wooden Pickaxe
                recipes3x3.add(CraftingRecipe.create3x3(
                                new BlockType[] { P, P, P, null, S, null, null, S, null },
                                BlockType.WOODEN_PICKAXE, 1));

                // Stone Pickaxe
                recipes3x3.add(CraftingRecipe.create3x3(
                                new BlockType[] { C, C, C, null, S, null, null, S, null },
                                BlockType.STONE_PICKAXE, 1));

                // Iron Pickaxe
                recipes3x3.add(CraftingRecipe.create3x3(
                                new BlockType[] { I, I, I, null, S, null, null, S, null },
                                BlockType.IRON_PICKAXE, 1));

                // Diamond Pickaxe
                recipes3x3.add(CraftingRecipe.create3x3(
                                new BlockType[] { D, D, D, null, S, null, null, S, null },
                                BlockType.DIAMOND_PICKAXE, 1));

                // ===== SHOVELS =====
                // Pattern: [ ][M][ ]
                // [ ][S][ ]
                // [ ][S][ ]

                // Wooden Shovel
                recipes3x3.add(CraftingRecipe.create3x3(
                                new BlockType[] { null, P, null, null, S, null, null, S, null },
                                BlockType.WOODEN_SHOVEL, 1));

                // Stone Shovel
                recipes3x3.add(CraftingRecipe.create3x3(
                                new BlockType[] { null, C, null, null, S, null, null, S, null },
                                BlockType.STONE_SHOVEL, 1));

                // Iron Shovel
                recipes3x3.add(CraftingRecipe.create3x3(
                                new BlockType[] { null, I, null, null, S, null, null, S, null },
                                BlockType.IRON_SHOVEL, 1));

                // Diamond Shovel
                recipes3x3.add(CraftingRecipe.create3x3(
                                new BlockType[] { null, D, null, null, S, null, null, S, null },
                                BlockType.DIAMOND_SHOVEL, 1));

                // ===== AXES =====
                // Pattern: [M][M][ ]
                // [M][S][ ]
                // [ ][S][ ]

                // Wooden Axe
                recipes3x3.add(CraftingRecipe.create3x3(
                                new BlockType[] { P, P, null, P, S, null, null, S, null },
                                BlockType.WOODEN_AXE, 1));

                // Stone Axe
                recipes3x3.add(CraftingRecipe.create3x3(
                                new BlockType[] { C, C, null, C, S, null, null, S, null },
                                BlockType.STONE_AXE, 1));

                // Iron Axe
                recipes3x3.add(CraftingRecipe.create3x3(
                                new BlockType[] { I, I, null, I, S, null, null, S, null },
                                BlockType.IRON_AXE, 1));

                // Diamond Axe
                recipes3x3.add(CraftingRecipe.create3x3(
                                new BlockType[] { D, D, null, D, S, null, null, S, null },
                                BlockType.DIAMOND_AXE, 1));

                // ===== BASIC RECIPES (also work in 3x3) =====

                // Log -> 4 Planks (shapeless, works anywhere in grid)
                // We add multiple positions for single-item shapeless
                for (int i = 0; i < 9; i++) {
                        BlockType[] pattern = new BlockType[9];
                        pattern[i] = BlockType.OAK_LOG;
                        recipes3x3.add(new CraftingRecipe(pattern, BlockType.OAK_PLANKS, 4, true, 3));
                }

                // 2 Planks vertical -> 4 Sticks (multiple column positions)
                // Column 0
                recipes3x3.add(CraftingRecipe.create3x3(
                                new BlockType[] { P, null, null, P, null, null, null, null, null },
                                BlockType.STICK, 4));
                // Column 1
                recipes3x3.add(CraftingRecipe.create3x3(
                                new BlockType[] { null, P, null, null, P, null, null, null, null },
                                BlockType.STICK, 4));
                // Column 2
                recipes3x3.add(CraftingRecipe.create3x3(
                                new BlockType[] { null, null, P, null, null, P, null, null, null },
                                BlockType.STICK, 4));
                // Middle rows
                recipes3x3.add(CraftingRecipe.create3x3(
                                new BlockType[] { null, null, null, P, null, null, P, null, null },
                                BlockType.STICK, 4));
                recipes3x3.add(CraftingRecipe.create3x3(
                                new BlockType[] { null, null, null, null, P, null, null, P, null },
                                BlockType.STICK, 4));
                recipes3x3.add(CraftingRecipe.create3x3(
                                new BlockType[] { null, null, null, null, null, P, null, null, P },
                                BlockType.STICK, 4));

                // 4 Planks (2x2) -> Crafting Table (multiple positions in 3x3)
                // Top-left
                recipes3x3.add(CraftingRecipe.create3x3(
                                new BlockType[] { P, P, null, P, P, null, null, null, null },
                                BlockType.CRAFTING_TABLE, 1));
                // Top-right
                recipes3x3.add(CraftingRecipe.create3x3(
                                new BlockType[] { null, P, P, null, P, P, null, null, null },
                                BlockType.CRAFTING_TABLE, 1));
                // Bottom-left
                recipes3x3.add(CraftingRecipe.create3x3(
                                new BlockType[] { null, null, null, P, P, null, P, P, null },
                                BlockType.CRAFTING_TABLE, 1));
                // Bottom-right
                recipes3x3.add(CraftingRecipe.create3x3(
                                new BlockType[] { null, null, null, null, P, P, null, P, P },
                                BlockType.CRAFTING_TABLE, 1));
        }

        /**
         * Find a 2x2 recipe that matches the given grid.
         */
        public static CraftingRecipe findRecipe(BlockType[] grid) {
                return findRecipe2x2(grid);
        }

        public static CraftingRecipe findRecipe2x2(BlockType[] grid) {
                for (CraftingRecipe recipe : recipes2x2) {
                        if (recipe.matches(grid)) {
                                return recipe;
                        }
                }
                return null;
        }

        /**
         * Find a 3x3 recipe that matches the given grid.
         */
        public static CraftingRecipe findRecipe3x3(BlockType[] grid) {
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
