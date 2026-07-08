package com.craftzero.progression;

import com.craftzero.inventory.ItemType;
import com.craftzero.world.BlockType;
import com.craftzero.world.Dimension;

/**
 * Release 1.0 achievement ids and parent relationships.
 */
public enum AchievementType {
    OPEN_INVENTORY("openInventory", "Taking Inventory", "Press 'E' to open your inventory", null,
            ItemType.BOOK, 0, 0, false),
    MINE_WOOD("mineWood", "Getting Wood", "Attack a tree until a block of wood pops out", OPEN_INVENTORY,
            ItemType.OAK_LOG, 2, 1, false),
    BUILD_WORKBENCH("buildWorkBench", "Benchmarking", "Craft a workbench with four blocks of planks", MINE_WOOD,
            ItemType.CRAFTING_TABLE, 4, -1, false),
    BUILD_PICKAXE("buildPickaxe", "Time to Mine!", "Use planks and sticks to make a pickaxe", BUILD_WORKBENCH,
            ItemType.WOODEN_PICKAXE, 4, 2, false),
    BUILD_BETTER_PICKAXE("buildBetterPickaxe", "Getting an Upgrade", "Construct a better pickaxe", BUILD_PICKAXE,
            ItemType.STONE_PICKAXE, 6, 2, false),
    BUILD_FURNACE("buildFurnace", "Hot Topic", "Construct a furnace out of eight cobblestone blocks",
            BUILD_PICKAXE, ItemType.FURNACE, 3, 4, false),
    ACQUIRE_IRON("acquireIron", "Acquire Hardware", "Smelt an iron ingot", BUILD_FURNACE,
            ItemType.IRON_INGOT, 1, 4, false),
    ON_A_RAIL("onARail", "On A Rail", "Travel by minecart at least 1 km from where you started", ACQUIRE_IRON,
            ItemType.RAIL, 2, 3, true),
    DIAMONDS("diamonds", "DIAMONDS!", "Acquire diamonds with your iron tools", ACQUIRE_IRON,
            ItemType.DIAMOND, -1, 5, false),
    BUILD_HOE("buildHoe", "Time to Farm!", "Use planks and sticks to make a hoe", BUILD_WORKBENCH,
            ItemType.WOODEN_HOE, 2, -3, false),
    MAKE_BREAD("makeBread", "Bake Bread", "Turn wheat into bread", BUILD_HOE,
            ItemType.BREAD, -1, -3, false),
    BAKE_CAKE("bakeCake", "The Lie", "Bake cake using wheat, sugar, milk and eggs", BUILD_HOE,
            ItemType.CAKE, 0, -5, false),
    BUILD_SWORD("buildSword", "Time to Strike!", "Use planks and sticks to make a sword", BUILD_WORKBENCH,
            ItemType.WOODEN_SWORD, 6, -1, false),
    KILL_ENEMY("killEnemy", "Monster Hunter", "Attack and destroy a monster", BUILD_SWORD,
            ItemType.BONE, 8, -1, false),
    KILL_COW("killCow", "Cow Tipper", "Harvest some leather", BUILD_SWORD,
            ItemType.LEATHER, 7, -3, false),
    FLY_PIG("flyPig", "When Pigs Fly", "Fly a pig off a cliff", KILL_COW,
            ItemType.SADDLE, 8, -4, true),
    SNIPE_SKELETON("snipeSkeleton", "Sniper Duel", "Kill a skeleton with an arrow from more than 50 meters",
            KILL_ENEMY, ItemType.BOW, 7, 0, true),
    COOK_FISH("cookFish", "Delicious Fish", "Catch and cook a fish", BUILD_FURNACE,
            ItemType.COOKED_FISH, 2, 6, false),
    PORTAL("portal", "We Need to Go Deeper", "Build a portal to the Nether", DIAMONDS,
            ItemType.OBSIDIAN, -1, 7, false),
    RETURN_TO_SENDER("ghast", "Return to Sender", "Destroy a Ghast with a fireball", PORTAL,
            ItemType.GHAST_TEAR, -4, 8, true),
    BLAZE_ROD("blazeRod", "Into Fire", "Relieve a Blaze of its rod", PORTAL,
            ItemType.BLAZE_ROD, 0, 9, false),
    LOCAL_BREWERY("potion", "Local Brewery", "Brew a potion", BLAZE_ROD,
            ItemType.POTION, 2, 8, false),
    THE_END("theEnd", "The End?", "Locate the End", BLAZE_ROD,
            ItemType.EYE_OF_ENDER, 3, 10, true),
    THE_END2("theEnd2", "The End.", "Defeat the Ender Dragon", THE_END,
            ItemType.DRAGON_EGG, 4, 13, true),
    ENCHANTMENTS("enchantments", "Enchanter",
            "Use a book, obsidian and diamonds to construct an enchantment table", DIAMONDS,
            ItemType.ENCHANTING_TABLE, -4, 4, false),
    OVERKILL("overkill", "Overkill", "Deal nine hearts of damage in a single hit", ENCHANTMENTS,
            ItemType.DIAMOND_SWORD, -4, 1, true),
    BOOKCASE("bookcase", "Librarian", "Build some bookshelves to improve your enchantment table", ENCHANTMENTS,
            ItemType.BOOKSHELF, -3, 6, false);

    private final String id;
    private final String title;
    private final String description;
    private final AchievementType parent;
    private final ItemType icon;
    private final int displayColumn;
    private final int displayRow;
    private final boolean special;

    AchievementType(String id, String title, String description, AchievementType parent,
            ItemType icon, int displayColumn, int displayRow, boolean special) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.parent = parent;
        this.icon = icon;
        this.displayColumn = displayColumn;
        this.displayRow = displayRow;
        this.special = special;
    }

    public String id() {
        return id;
    }

    public String title() {
        return title;
    }

    public String description() {
        return description;
    }

    public AchievementType parent() {
        return parent;
    }

    public ItemType icon() {
        return icon;
    }

    public int displayColumn() {
        return displayColumn;
    }

    public int displayRow() {
        return displayRow;
    }

    public boolean special() {
        return special;
    }

    public static AchievementType fromId(String id) {
        if (id == null) {
            return null;
        }
        for (AchievementType type : values()) {
            if (type.id.equals(id)) {
                return type;
            }
        }
        return null;
    }

    public static AchievementType forCraftedItem(ItemType item) {
        if (item == null) {
            return null;
        }
        if (item == ItemType.CRAFTING_TABLE) {
            return BUILD_WORKBENCH;
        }
        if (isWoodPickaxe(item)) {
            return BUILD_PICKAXE;
        }
        if (isBetterPickaxe(item)) {
            return BUILD_BETTER_PICKAXE;
        }
        if (item == ItemType.FURNACE) {
            return BUILD_FURNACE;
        }
        if (isHoe(item)) {
            return BUILD_HOE;
        }
        if (item == ItemType.BREAD) {
            return MAKE_BREAD;
        }
        if (item == ItemType.CAKE) {
            return BAKE_CAKE;
        }
        if (isSword(item)) {
            return BUILD_SWORD;
        }
        if (item == ItemType.ENCHANTING_TABLE) {
            return ENCHANTMENTS;
        }
        if (item == ItemType.BOOKSHELF) {
            return BOOKCASE;
        }
        return null;
    }

    public static AchievementType forCollectedItem(ItemType item) {
        if (item == null) {
            return null;
        }
        return switch (item) {
            case OAK_LOG, SPRUCE_LOG, BIRCH_LOG -> MINE_WOOD;
            case IRON_INGOT -> ACQUIRE_IRON;
            case DIAMOND -> DIAMONDS;
            case LEATHER -> KILL_COW;
            case COOKED_FISH -> COOK_FISH;
            case BLAZE_ROD -> BLAZE_ROD;
            default -> null;
        };
    }

    public static AchievementType forBrokenBlock(BlockType block) {
        return block == BlockType.OAK_LOG ? MINE_WOOD : null;
    }

    public static AchievementType forDimensionTravel(Dimension from, Dimension to) {
        if (to == Dimension.NETHER) {
            return PORTAL;
        }
        if (to == Dimension.THE_END) {
            return THE_END;
        }
        if (from == Dimension.THE_END && to == Dimension.OVERWORLD) {
            return THE_END2;
        }
        return null;
    }

    private static boolean isWoodPickaxe(ItemType item) {
        return item == ItemType.WOODEN_PICKAXE;
    }

    private static boolean isBetterPickaxe(ItemType item) {
        return item == ItemType.STONE_PICKAXE
                || item == ItemType.IRON_PICKAXE
                || item == ItemType.DIAMOND_PICKAXE
                || item == ItemType.GOLD_PICKAXE;
    }

    private static boolean isHoe(ItemType item) {
        return item == ItemType.WOODEN_HOE
                || item == ItemType.STONE_HOE
                || item == ItemType.IRON_HOE
                || item == ItemType.DIAMOND_HOE
                || item == ItemType.GOLD_HOE;
    }

    private static boolean isSword(ItemType item) {
        return item == ItemType.WOODEN_SWORD
                || item == ItemType.STONE_SWORD
                || item == ItemType.IRON_SWORD
                || item == ItemType.DIAMOND_SWORD
                || item == ItemType.GOLD_SWORD;
    }
}
