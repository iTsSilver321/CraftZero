package com.craftzero.crafting;

import com.craftzero.inventory.ItemStack;
import com.craftzero.inventory.ItemType;
import com.craftzero.inventory.MapItemData;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Release 1.0 crafting registry for both the 2x2 player grid and the 3x3
 * crafting table grid.
 */
public class CraftingRegistry {
        private static final List<CraftingRecipe> recipes = new ArrayList<>();

        private static final ItemType[] LOGS = {
                        ItemType.OAK_LOG, ItemType.SPRUCE_LOG, ItemType.BIRCH_LOG
        };

        private static final ItemType[] WOOLS_BY_METADATA = {
                        ItemType.WHITE_WOOL,
                        ItemType.ORANGE_WOOL,
                        ItemType.MAGENTA_WOOL,
                        ItemType.LIGHT_BLUE_WOOL,
                        ItemType.YELLOW_WOOL,
                        ItemType.LIME_WOOL,
                        ItemType.PINK_WOOL,
                        ItemType.GRAY_WOOL,
                        ItemType.LIGHT_GRAY_WOOL,
                        ItemType.CYAN_WOOL,
                        ItemType.PURPLE_WOOL,
                        ItemType.BLUE_WOOL,
                        ItemType.BROWN_WOOL,
                        ItemType.GREEN_WOOL,
                        ItemType.RED_WOOL,
                        ItemType.BLACK_WOOL
        };

        private static final ItemType[] DYES_BY_METADATA = {
                        ItemType.INK_SAC,
                        ItemType.ROSE_RED,
                        ItemType.CACTUS_GREEN,
                        ItemType.COCOA_BEANS,
                        ItemType.LAPIS_LAZULI,
                        ItemType.PURPLE_DYE,
                        ItemType.CYAN_DYE,
                        ItemType.LIGHT_GRAY_DYE,
                        ItemType.GRAY_DYE,
                        ItemType.PINK_DYE,
                        ItemType.LIME_DYE,
                        ItemType.DANDELION_YELLOW,
                        ItemType.LIGHT_BLUE_DYE,
                        ItemType.MAGENTA_DYE,
                        ItemType.ORANGE_DYE,
                        ItemType.BONE_MEAL
        };

        static {
                registerToolRecipes();
                registerWeaponRecipes();
                registerArmorRecipes();
                registerStorageBlockRecipes();
                registerBuildingRecipes();
                registerMechanismRecipes();
                registerFoodAndIngredientRecipes();
                registerDyeRecipes();
                sortLikeVanilla();
        }

        private static void registerToolRecipes() {
                addToolRecipes(ItemType.OAK_PLANKS, ItemType.WOODEN_PICKAXE, ItemType.WOODEN_SHOVEL,
                                ItemType.WOODEN_AXE, ItemType.WOODEN_HOE);
                addToolRecipes(ItemType.COBBLESTONE, ItemType.STONE_PICKAXE, ItemType.STONE_SHOVEL,
                                ItemType.STONE_AXE, ItemType.STONE_HOE);
                addToolRecipes(ItemType.IRON_INGOT, ItemType.IRON_PICKAXE, ItemType.IRON_SHOVEL,
                                ItemType.IRON_AXE, ItemType.IRON_HOE);
                addToolRecipes(ItemType.DIAMOND, ItemType.DIAMOND_PICKAXE, ItemType.DIAMOND_SHOVEL,
                                ItemType.DIAMOND_AXE, ItemType.DIAMOND_HOE);
                addToolRecipes(ItemType.GOLD_INGOT, ItemType.GOLD_PICKAXE, ItemType.GOLD_SHOVEL,
                                ItemType.GOLD_AXE, ItemType.GOLD_HOE);

                shaped(ItemType.SHEARS, 1, rows(" #", "# "), '#', ItemType.IRON_INGOT);
                shaped(ItemType.FISHING_ROD, 1, rows("  #", " #X", "# X"), '#', ItemType.STICK, 'X',
                                ItemType.STRING);
                shaped(ItemType.FLINT_AND_STEEL, 1, rows("A ", " B"), 'A', ItemType.IRON_INGOT, 'B',
                                ItemType.FLINT);
        }

        private static void registerWeaponRecipes() {
                addSwordRecipe(ItemType.OAK_PLANKS, ItemType.WOODEN_SWORD);
                addSwordRecipe(ItemType.COBBLESTONE, ItemType.STONE_SWORD);
                addSwordRecipe(ItemType.IRON_INGOT, ItemType.IRON_SWORD);
                addSwordRecipe(ItemType.DIAMOND, ItemType.DIAMOND_SWORD);
                addSwordRecipe(ItemType.GOLD_INGOT, ItemType.GOLD_SWORD);

                shaped(ItemType.BOW, 1, rows(" #X", "# X", " #X"), '#', ItemType.STICK, 'X', ItemType.STRING);
                shaped(ItemType.ARROW, 4, rows("X", "#", "Y"), 'X', ItemType.FLINT, '#', ItemType.STICK, 'Y',
                                ItemType.FEATHER);
        }

        private static void registerArmorRecipes() {
                addArmorSet(ItemType.LEATHER, ItemType.LEATHER_HELMET, ItemType.LEATHER_CHESTPLATE,
                                ItemType.LEATHER_LEGGINGS, ItemType.LEATHER_BOOTS);
                addArmorSet(ItemType.FIRE, ItemType.CHAIN_HELMET, ItemType.CHAIN_CHESTPLATE,
                                ItemType.CHAIN_LEGGINGS, ItemType.CHAIN_BOOTS);
                addArmorSet(ItemType.IRON_INGOT, ItemType.IRON_HELMET, ItemType.IRON_CHESTPLATE,
                                ItemType.IRON_LEGGINGS, ItemType.IRON_BOOTS);
                addArmorSet(ItemType.DIAMOND, ItemType.DIAMOND_HELMET, ItemType.DIAMOND_CHESTPLATE,
                                ItemType.DIAMOND_LEGGINGS, ItemType.DIAMOND_BOOTS);
                addArmorSet(ItemType.GOLD_INGOT, ItemType.GOLD_HELMET, ItemType.GOLD_CHESTPLATE,
                                ItemType.GOLD_LEGGINGS, ItemType.GOLD_BOOTS);
        }

        private static void registerStorageBlockRecipes() {
                addStorageBlock(ItemType.GOLD_INGOT, ItemType.GOLD_BLOCK);
                addStorageBlock(ItemType.IRON_INGOT, ItemType.IRON_BLOCK);
                addStorageBlock(ItemType.DIAMOND, ItemType.DIAMOND_BLOCK);
                addStorageBlock(ItemType.LAPIS_LAZULI, ItemType.LAPIS_BLOCK);

                shaped(ItemType.GOLD_INGOT, 1, rows("###", "###", "###"), '#', ItemType.GOLD_NUGGET);
                shaped(ItemType.GOLD_NUGGET, 9, rows("#"), '#', ItemType.GOLD_INGOT);
        }

        private static void registerBuildingRecipes() {
                shaped(ItemType.OAK_PLANKS, 4, rows("#"), '#', any(LOGS));
                shaped(ItemType.STICK, 4, rows("#", "#"), '#', ItemType.OAK_PLANKS);
                shaped(ItemType.CRAFTING_TABLE, 1, rows("##", "##"), '#', ItemType.OAK_PLANKS);
                shaped(ItemType.CHEST, 1, rows("###", "# #", "###"), '#', ItemType.OAK_PLANKS);
                shaped(ItemType.FURNACE, 1, rows("###", "# #", "###"), '#', ItemType.COBBLESTONE);

                shaped(ItemType.SANDSTONE, 1, rows("##", "##"), '#', ItemType.SAND);
                shaped(ItemType.STONE_BRICK, 4, rows("##", "##"), '#', ItemType.STONE);
                shaped(ItemType.BRICK, 1, rows("##", "##"), '#', ItemType.BRICK_ITEM);
                shaped(ItemType.CLAY, 1, rows("##", "##"), '#', ItemType.CLAY_BALL);
                shaped(ItemType.SNOW, 1, rows("##", "##"), '#', ItemType.SNOWBALL);
                shaped(ItemType.GLOWSTONE, 1, rows("##", "##"), '#', ItemType.GLOWSTONE_DUST);
                shaped(ItemType.WHITE_WOOL, 1, rows("##", "##"), '#', ItemType.STRING);

                shaped(ItemType.BOOKSHELF, 1, rows("###", "XXX", "###"), '#', ItemType.OAK_PLANKS, 'X',
                                ItemType.BOOK);
                shaped(ItemType.JUKEBOX, 1, rows("###", "#X#", "###"), '#', ItemType.OAK_PLANKS, 'X',
                                ItemType.DIAMOND);
                shaped(ItemType.NOTE_BLOCK, 1, rows("###", "#X#", "###"), '#', ItemType.OAK_PLANKS, 'X',
                                ItemType.REDSTONE);

                shaped(ItemType.STONE_SLAB, 3, rows("###"), '#', ItemType.STONE);
                shaped(ItemType.SANDSTONE_SLAB, 3, rows("###"), '#', ItemType.SANDSTONE);
                shaped(ItemType.WOODEN_SLAB, 3, rows("###"), '#', ItemType.OAK_PLANKS);
                shaped(ItemType.COBBLESTONE_SLAB, 3, rows("###"), '#', ItemType.COBBLESTONE);
                shaped(ItemType.BRICK_SLAB, 3, rows("###"), '#', ItemType.BRICK);
                shaped(ItemType.STONE_BRICK_SLAB, 3, rows("###"), '#', ItemType.STONE_BRICK);

                shaped(ItemType.OAK_STAIRS, 4, rows("#  ", "## ", "###"), '#', ItemType.OAK_PLANKS);
                shaped(ItemType.COBBLESTONE_STAIRS, 4, rows("#  ", "## ", "###"), '#', ItemType.COBBLESTONE);
                shaped(ItemType.BRICK_STAIRS, 4, rows("#  ", "## ", "###"), '#', ItemType.BRICK);
                shaped(ItemType.STONE_BRICK_STAIRS, 4, rows("#  ", "## ", "###"), '#', ItemType.STONE_BRICK);
                shaped(ItemType.NETHER_BRICK_STAIRS, 4, rows("#  ", "## ", "###"), '#', ItemType.NETHER_BRICK);

                shaped(ItemType.LADDER, 2, rows("# #", "###", "# #"), '#', ItemType.STICK);
                shaped(ItemType.WOODEN_DOOR, 1, rows("##", "##", "##"), '#', ItemType.OAK_PLANKS);
                shaped(ItemType.IRON_DOOR, 1, rows("##", "##", "##"), '#', ItemType.IRON_INGOT);
                shaped(ItemType.TRAPDOOR, 2, rows("###", "###"), '#', ItemType.OAK_PLANKS);
                shaped(ItemType.SIGN, 1, rows("###", "###", " X "), '#', ItemType.OAK_PLANKS, 'X',
                                ItemType.STICK);
                shaped(ItemType.FENCE, 2, rows("###", "###"), '#', ItemType.STICK);
                shaped(ItemType.NETHER_BRICK_FENCE, 6, rows("###", "###"), '#', ItemType.NETHER_BRICK);
                shaped(ItemType.FENCE_GATE, 1, rows("#W#", "#W#"), '#', ItemType.STICK, 'W',
                                ItemType.OAK_PLANKS);
                shaped(ItemType.BED, 1, rows("###", "XXX"), '#', any(WOOLS_BY_METADATA), 'X',
                                ItemType.OAK_PLANKS);
                shaped(ItemType.GLASS_PANE, 16, rows("###", "###"), '#', ItemType.GLASS);
                shaped(ItemType.IRON_BARS, 16, rows("###", "###"), '#', ItemType.IRON_INGOT);
        }

        private static void registerMechanismRecipes() {
                shaped(ItemType.TORCH, 4, rows("X", "#"), 'X', ItemType.COAL, '#', ItemType.STICK);
                shaped(ItemType.TORCH, 4, rows("X", "#"), 'X', ItemType.CHARCOAL, '#', ItemType.STICK);
                shaped(ItemType.REDSTONE_TORCH, 1, rows("X", "#"), 'X', ItemType.REDSTONE, '#',
                                ItemType.STICK);
                shaped(ItemType.LEVER, 1, rows("X", "#"), 'X', ItemType.STICK, '#', ItemType.COBBLESTONE);
                shaped(ItemType.STONE_BUTTON, 1, rows("#", "#"), '#', ItemType.STONE);
                shaped(ItemType.STONE_PRESSURE_PLATE, 1, rows("##"), '#', ItemType.STONE);
                shaped(ItemType.WOODEN_PRESSURE_PLATE, 1, rows("##"), '#', ItemType.OAK_PLANKS);

                shaped(ItemType.RAIL, 16, rows("X X", "X#X", "X X"), 'X', ItemType.IRON_INGOT, '#',
                                ItemType.STICK);
                shaped(ItemType.POWERED_RAIL, 6, rows("X X", "X#X", "XRX"), 'X', ItemType.GOLD_INGOT, '#',
                                ItemType.STICK, 'R', ItemType.REDSTONE);
                shaped(ItemType.DETECTOR_RAIL, 6, rows("X X", "X#X", "XRX"), 'X', ItemType.IRON_INGOT, '#',
                                ItemType.STONE_PRESSURE_PLATE, 'R', ItemType.REDSTONE);
                shaped(ItemType.MINECART, 1, rows("# #", "###"), '#', ItemType.IRON_INGOT);
                shaped(ItemType.CHEST_MINECART, 1, rows("A", "B"), 'A', ItemType.CHEST, 'B',
                                ItemType.MINECART);
                shaped(ItemType.FURNACE_MINECART, 1, rows("A", "B"), 'A', ItemType.FURNACE, 'B',
                                ItemType.MINECART);

                shaped(ItemType.REDSTONE_REPEATER, 1, rows("#X#", "III"), '#', ItemType.REDSTONE_TORCH, 'X',
                                ItemType.REDSTONE, 'I', ItemType.STONE);
                shaped(ItemType.PISTON, 1, rows("TTT", "#X#", "#R#"), 'T', ItemType.OAK_PLANKS, '#',
                                ItemType.COBBLESTONE, 'X', ItemType.IRON_INGOT, 'R', ItemType.REDSTONE);
                shaped(ItemType.STICKY_PISTON, 1, rows("S", "P"), 'S', ItemType.SLIMEBALL, 'P',
                                ItemType.PISTON);
                shaped(ItemType.DISPENSER, 1, rows("###", "#X#", "#R#"), '#', ItemType.COBBLESTONE, 'X',
                                ItemType.BOW, 'R', ItemType.REDSTONE);

                shaped(ItemType.TNT, 1, rows("X#X", "#X#", "X#X"), 'X', ItemType.GUNPOWDER, '#',
                                ItemType.SAND);
                shaped(ItemType.BUCKET, 1, rows("# #", " # "), '#', ItemType.IRON_INGOT);
                shaped(ItemType.CAULDRON, 1, rows("# #", "# #", "###"), '#', ItemType.IRON_INGOT);
                shaped(ItemType.BREWING_STAND, 1, rows(" B ", "###"), 'B', ItemType.BLAZE_ROD, '#',
                                ItemType.COBBLESTONE);
                shaped(ItemType.JACK_O_LANTERN, 1, rows("A", "B"), 'A', ItemType.PUMPKIN, 'B',
                                ItemType.TORCH);
                shaped(ItemType.ENCHANTING_TABLE, 1, rows(" B ", "D#D", "###"), 'B', ItemType.BOOK, 'D',
                                ItemType.DIAMOND, '#', ItemType.OBSIDIAN);
        }

        private static void registerFoodAndIngredientRecipes() {
                shaped(ItemType.PAPER, 3, rows("###"), '#', ItemType.SUGAR_CANE);
                shaped(ItemType.BOOK, 1, rows("#", "#", "#"), '#', ItemType.PAPER);
                shaped(ItemType.SUGAR, 1, rows("#"), '#', ItemType.SUGAR_CANE);
                shaped(ItemType.BREAD, 1, rows("###"), '#', ItemType.WHEAT);
                shaped(ItemType.BOWL, 4, rows("# #", " # "), '#', ItemType.OAK_PLANKS);
                shaped(ItemType.GLASS_BOTTLE, 3, rows("# #", " # "), '#', ItemType.GLASS);
                shapeless(ItemType.MUSHROOM_STEW, 1, ItemType.BROWN_MUSHROOM, ItemType.RED_MUSHROOM,
                                ItemType.BOWL);

                shaped(ItemType.CAKE, 1, rows("AAA", "BEB", "CCC"), 'A', ItemType.MILK_BUCKET, 'B',
                                ItemType.SUGAR, 'C', ItemType.WHEAT, 'E', ItemType.EGG);
                shaped(ItemType.COOKIE, 8, rows("#X#"), '#', ItemType.WHEAT, 'X', ItemType.COCOA_BEANS);
                shaped(ItemType.GOLDEN_APPLE, 1, rows("###", "#X#", "###"), '#', ItemType.GOLD_BLOCK, 'X',
                                ItemType.APPLE);
                shaped(ItemType.MELON_BLOCK, 1, rows("MMM", "MMM", "MMM"), 'M', ItemType.MELON_SLICE);
                shaped(ItemType.MELON_SEEDS, 1, rows("M"), 'M', ItemType.MELON_SLICE);
                shaped(ItemType.PUMPKIN_SEEDS, 4, rows("M"), 'M', ItemType.PUMPKIN);

                shapeless(ItemType.FERMENTED_SPIDER_EYE, 1, ItemType.SPIDER_EYE, ItemType.BROWN_MUSHROOM,
                                ItemType.SUGAR);
                shapeless(ItemType.GLISTERING_MELON, 1, ItemType.MELON_SLICE, ItemType.GOLD_NUGGET);
                shapeless(ItemType.MAGMA_CREAM, 1, ItemType.BLAZE_POWDER, ItemType.SLIMEBALL);
                shapeless(ItemType.EYE_OF_ENDER, 1, ItemType.ENDER_PEARL, ItemType.BLAZE_POWDER);
                shaped(ItemType.BLAZE_POWDER, 2, rows("M"), 'M', ItemType.BLAZE_ROD);

                shaped(ItemType.BOAT, 1, rows("# #", "###"), '#', ItemType.OAK_PLANKS);
                shaped(ItemType.PAINTING, 1, rows("###", "#X#", "###"), '#', ItemType.STICK, 'X',
                                any(WOOLS_BY_METADATA));
                shaped(ItemType.CLOCK, 1, rows(" # ", "#X#", " # "), '#', ItemType.GOLD_INGOT, 'X',
                                ItemType.REDSTONE);
                shaped(ItemType.COMPASS, 1, rows(" # ", "#X#", " # "), '#', ItemType.IRON_INGOT, 'X',
                                ItemType.REDSTONE);
                shaped(ItemType.MAP, 1, rows("###", "#X#", "###"), '#', ItemType.PAPER, 'X',
                                ItemType.COMPASS);
        }

        private static void registerDyeRecipes() {
                for (int dyeMetadata = 0; dyeMetadata < DYES_BY_METADATA.length; dyeMetadata++) {
                        int woolMetadata = ~dyeMetadata & 15;
                        shapeless(WOOLS_BY_METADATA[woolMetadata], 1, DYES_BY_METADATA[dyeMetadata],
                                        ItemType.WHITE_WOOL);
                }

                shapeless(ItemType.DANDELION_YELLOW, 2, ItemType.YELLOW_FLOWER);
                shapeless(ItemType.ROSE_RED, 2, ItemType.RED_ROSE);
                shapeless(ItemType.BONE_MEAL, 3, ItemType.BONE);
                shapeless(ItemType.PINK_DYE, 2, ItemType.ROSE_RED, ItemType.BONE_MEAL);
                shapeless(ItemType.ORANGE_DYE, 2, ItemType.ROSE_RED, ItemType.DANDELION_YELLOW);
                shapeless(ItemType.LIME_DYE, 2, ItemType.CACTUS_GREEN, ItemType.BONE_MEAL);
                shapeless(ItemType.GRAY_DYE, 2, ItemType.INK_SAC, ItemType.BONE_MEAL);
                shapeless(ItemType.LIGHT_GRAY_DYE, 2, ItemType.GRAY_DYE, ItemType.BONE_MEAL);
                shapeless(ItemType.LIGHT_GRAY_DYE, 3, ItemType.INK_SAC, ItemType.BONE_MEAL, ItemType.BONE_MEAL);
                shapeless(ItemType.LIGHT_BLUE_DYE, 2, ItemType.LAPIS_LAZULI, ItemType.BONE_MEAL);
                shapeless(ItemType.CYAN_DYE, 2, ItemType.LAPIS_LAZULI, ItemType.CACTUS_GREEN);
                shapeless(ItemType.PURPLE_DYE, 2, ItemType.LAPIS_LAZULI, ItemType.ROSE_RED);
                shapeless(ItemType.MAGENTA_DYE, 2, ItemType.PURPLE_DYE, ItemType.PINK_DYE);
                shapeless(ItemType.MAGENTA_DYE, 3, ItemType.LAPIS_LAZULI, ItemType.ROSE_RED,
                                ItemType.PINK_DYE);
                shapeless(ItemType.MAGENTA_DYE, 4, ItemType.LAPIS_LAZULI, ItemType.ROSE_RED,
                                ItemType.ROSE_RED, ItemType.BONE_MEAL);
        }

        private static void addToolRecipes(ItemType material, ItemType pickaxe, ItemType shovel, ItemType axe,
                        ItemType hoe) {
                shaped(pickaxe, 1, rows("XXX", " # ", " # "), 'X', material, '#', ItemType.STICK);
                shaped(shovel, 1, rows("X", "#", "#"), 'X', material, '#', ItemType.STICK);
                shaped(axe, 1, rows("XX", "X#", " #"), 'X', material, '#', ItemType.STICK);
                shaped(hoe, 1, rows("XX", " #", " #"), 'X', material, '#', ItemType.STICK);
        }

        private static void addSwordRecipe(ItemType material, ItemType sword) {
                shaped(sword, 1, rows("X", "X", "#"), 'X', material, '#', ItemType.STICK);
        }

        private static void addArmorSet(ItemType material, ItemType helmet, ItemType chestplate, ItemType leggings,
                        ItemType boots) {
                shaped(helmet, 1, rows("XXX", "X X"), 'X', material);
                shaped(chestplate, 1, rows("X X", "XXX", "XXX"), 'X', material);
                shaped(leggings, 1, rows("XXX", "X X", "X X"), 'X', material);
                shaped(boots, 1, rows("X X", "X X"), 'X', material);
        }

        private static void addStorageBlock(ItemType material, ItemType block) {
                shaped(block, 1, rows("###", "###", "###"), '#', material);
                shaped(material, 9, rows("#"), '#', block);
        }

        private static void shaped(ItemType output, int count, String[] rows, Object... keyValues) {
                Map<Character, CraftingRecipe.Ingredient> keys = new HashMap<>();
                for (int i = 0; i < keyValues.length; i += 2) {
                        keys.put((Character) keyValues[i], ingredient(keyValues[i + 1]));
                }

                int width = 0;
                for (String row : rows) {
                        width = Math.max(width, row.length());
                }

                CraftingRecipe.Ingredient[] pattern = new CraftingRecipe.Ingredient[width * rows.length];
                for (int y = 0; y < rows.length; y++) {
                        String row = rows[y];
                        for (int x = 0; x < width; x++) {
                                char key = x < row.length() ? row.charAt(x) : ' ';
                                pattern[x + y * width] = key == ' ' ? null : keys.get(key);
                        }
                }

                recipes.add(CraftingRecipe.shaped(width, rows.length, pattern, output, count));
        }

        private static void shapeless(ItemType output, int count, Object... ingredients) {
                CraftingRecipe.Ingredient[] pattern = new CraftingRecipe.Ingredient[ingredients.length];
                for (int i = 0; i < ingredients.length; i++) {
                        pattern[i] = ingredient(ingredients[i]);
                }
                recipes.add(CraftingRecipe.shapeless(pattern, output, count));
        }

        private static CraftingRecipe.Ingredient ingredient(Object value) {
                if (value instanceof CraftingRecipe.Ingredient ingredient) {
                        return ingredient;
                }
                if (value instanceof ItemType itemType) {
                        return CraftingRecipe.Ingredient.of(itemType);
                }
                throw new IllegalArgumentException("Unsupported recipe ingredient " + value);
        }

        private static CraftingRecipe.Ingredient any(ItemType... itemTypes) {
                return CraftingRecipe.Ingredient.anyOf(itemTypes);
        }

        private static String[] rows(String... rows) {
                return rows;
        }

        private static void sortLikeVanilla() {
                recipes.sort((left, right) -> {
                        if (left.isShapeless() && !right.isShapeless()) {
                                return 1;
                        }
                        if (right.isShapeless() && !left.isShapeless()) {
                                return -1;
                        }
                        return Comparator.comparingInt(CraftingRecipe::getRecipeSize).reversed()
                                        .compare(left, right);
                });
        }

        public static CraftingRecipe findRecipe(ItemType[] grid) {
                return findRecipe2x2(grid);
        }

        public static CraftingRecipe findRecipe(ItemStack[] grid) {
                return findRecipe2x2(grid);
        }

        public static CraftingRecipe findRecipe2x2(ItemType[] grid) {
                return findRecipeIn(grid);
        }

        public static CraftingRecipe findRecipe2x2(ItemStack[] grid) {
                return findRecipeIn(grid);
        }

        public static CraftingRecipe findRecipe3x3(ItemType[] grid) {
                return findRecipeIn(grid);
        }

        public static CraftingRecipe findRecipe3x3(ItemStack[] grid) {
                return findRecipeIn(grid);
        }

        private static CraftingRecipe findRecipeIn(ItemType[] grid) {
                for (CraftingRecipe recipe : recipes) {
                        if (recipe.matches(grid)) {
                                return recipe;
                        }
                }
                return null;
        }

        private static CraftingRecipe findRecipeIn(ItemStack[] grid) {
                CraftingRecipe repairRecipe = findRepairRecipe(grid);
                if (repairRecipe != null) {
                        return repairRecipe;
                }
                CraftingRecipe mapCopyRecipe = findMapCopyRecipe(grid);
                if (mapCopyRecipe != null) {
                        return mapCopyRecipe;
                }
                CraftingRecipe recipe = findRecipeIn(toItemTypes(grid));
                if (recipe != null && !isStackSensitiveRecipeValid(recipe, grid)) {
                        return null;
                }
                return recipe;
        }

        private static boolean isStackSensitiveRecipeValid(CraftingRecipe recipe, ItemStack[] grid) {
                if (recipe.getOutputType() != ItemType.DISPENSER) {
                        return true;
                }
                for (ItemStack stack : grid) {
                        if (stack != null && !stack.isEmpty() && stack.getType() == ItemType.BOW) {
                                return stack.getDurability() >= stack.getMaxDurability();
                        }
                }
                return true;
        }

        private static CraftingRecipe findRepairRecipe(ItemStack[] grid) {
                if (grid == null || (grid.length != 4 && grid.length != 9)) {
                        return null;
                }

                ItemStack first = null;
                ItemStack second = null;
                for (ItemStack stack : grid) {
                        if (stack == null || stack.isEmpty()) {
                                continue;
                        }
                        if (first == null) {
                                first = stack;
                        } else if (second == null) {
                                second = stack;
                        } else {
                                return null;
                        }
                }

                if (first == null || second == null || first.getCount() != 1 || second.getCount() != 1) {
                        return null;
                }

                ItemType type = first.getType();
                if (type == null || type != second.getType() || !type.isDamageable()) {
                        return null;
                }

                int maxDurability = type.getMaxDurability();
                if (maxDurability <= 0) {
                        return null;
                }

                int repairedDurability = Math.min(maxDurability,
                                Math.max(0, first.getDurability())
                                                + Math.max(0, second.getDurability())
                                                + maxDurability * 5 / 100);
                ItemStack output = new ItemStack(type, 1);
                output.setDurability(repairedDurability);

                return CraftingRecipe.shapelessWithOutputStack(new CraftingRecipe.Ingredient[] {
                                CraftingRecipe.Ingredient.of(type),
                                CraftingRecipe.Ingredient.of(type)
                }, output);
        }

        private static CraftingRecipe findMapCopyRecipe(ItemStack[] grid) {
                if (grid == null || (grid.length != 4 && grid.length != 9)) {
                        return null;
                }

                ItemStack source = null;
                int mapSlots = 0;
                for (ItemStack stack : grid) {
                        if (stack == null || stack.isEmpty()) {
                                continue;
                        }
                        if (stack.getType() != ItemType.MAP) {
                                return null;
                        }
                        mapSlots++;
                        if (MapItemData.isInitializedMap(stack)) {
                                if (source != null) {
                                        return null;
                                }
                                source = stack;
                        }
                }

                if (source == null || mapSlots < 2) {
                        return null;
                }

                ItemStack output = MapItemData.copyInitializedMap(source, mapSlots);
                if (output == null) {
                        return null;
                }
                CraftingRecipe.Ingredient[] ingredients = new CraftingRecipe.Ingredient[mapSlots];
                for (int i = 0; i < ingredients.length; i++) {
                        ingredients[i] = CraftingRecipe.Ingredient.of(ItemType.MAP);
                }
                return CraftingRecipe.shapelessWithOutputStack(ingredients, output);
        }

        private static ItemType[] toItemTypes(ItemStack[] grid) {
                if (grid == null) {
                        return new ItemType[0];
                }
                ItemType[] pattern = new ItemType[grid.length];
                for (int i = 0; i < grid.length; i++) {
                        pattern[i] = (grid[i] != null && !grid[i].isEmpty()) ? grid[i].getType() : null;
                }
                return pattern;
        }

        public static List<CraftingRecipe> getAllRecipes() {
                return List.copyOf(recipes);
        }
}
