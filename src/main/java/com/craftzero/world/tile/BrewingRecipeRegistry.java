package com.craftzero.world.tile;

import com.craftzero.inventory.ItemStack;
import com.craftzero.inventory.ItemType;
import com.craftzero.progression.PotionData;
import com.craftzero.progression.PotionType;

import java.util.ArrayList;
import java.util.List;

/**
 * Release 1.0 brewing recipes: three bottle slots plus one ingredient, no fuel.
 */
public final class BrewingRecipeRegistry {
    private BrewingRecipeRegistry() {
    }

    public static boolean isIngredient(ItemStack stack) {
        return stack != null && !stack.isEmpty() && switch (stack.getType()) {
            case NETHER_WART, REDSTONE, GLOWSTONE_DUST, GUNPOWDER, SUGAR, MAGMA_CREAM, GHAST_TEAR,
                    SPIDER_EYE, FERMENTED_SPIDER_EYE, BLAZE_POWDER, GLISTERING_MELON -> true;
            default -> false;
        };
    }

    public static boolean isBottleSlotItem(ItemStack stack) {
        return stack != null && !stack.isEmpty() && stack.getType() == ItemType.POTION;
    }

    public static ItemStack brew(ItemStack bottle, ItemStack ingredient) {
        if (bottle == null || bottle.isEmpty() || ingredient == null || ingredient.isEmpty()) {
            return null;
        }
        if (!isBottleSlotItem(bottle)) {
            return null;
        }
        PotionData current = bottle.getPotionData();
        if (current == null) {
            current = PotionData.water();
        }
        PotionData result = brew(current, ingredient.getType());
        if (result == null || result.equals(current)) {
            return null;
        }
        ItemStack stack = bottle.copy();
        stack.setCount(1);
        stack.setPotionData(result);
        return stack;
    }

    public static PotionData brew(PotionData current, ItemType ingredient) {
        if (current == null) {
            current = PotionData.water();
        }
        if (ingredient == ItemType.GUNPOWDER) {
            return current.splash() ? null : new PotionData(current.type(), true, current.extended(), current.enhanced());
        }
        boolean splash = current.splash();
        if (ingredient == ItemType.REDSTONE && canExtend(current)) {
            return new PotionData(current.type(), splash, true, false);
        }
        if (ingredient == ItemType.GLOWSTONE_DUST && canEnhance(current)) {
            return new PotionData(current.type(), splash, false, true);
        }
        if (ingredient == ItemType.FERMENTED_SPIDER_EYE) {
            PotionType corrupt = corrupt(current.type());
            if (corrupt != null) {
                boolean keepExtended = current.extended()
                        && (corrupt == PotionType.SLOWNESS || corrupt == PotionType.WEAKNESS);
                boolean keepEnhanced = current.enhanced() && corrupt == PotionType.HARMING;
                return new PotionData(corrupt, splash, keepExtended, keepEnhanced);
            }
        }

        if (current.type() == PotionType.WATER) {
            return switch (ingredient) {
                case NETHER_WART -> new PotionData(PotionType.AWKWARD, splash, false, false);
                case GLOWSTONE_DUST -> new PotionData(PotionType.THICK, splash, false, false);
                case FERMENTED_SPIDER_EYE -> new PotionData(PotionType.WEAKNESS, splash, false, false);
                case REDSTONE -> new PotionData(PotionType.MUNDANE, splash, true, false);
                case SUGAR, MAGMA_CREAM, GHAST_TEAR, SPIDER_EYE, BLAZE_POWDER, GLISTERING_MELON ->
                    new PotionData(PotionType.MUNDANE, splash, false, false);
                default -> null;
            };
        }

        if (current.type() == PotionType.AWKWARD) {
            return switch (ingredient) {
                case SUGAR -> new PotionData(PotionType.SWIFTNESS, splash, false, false);
                case MAGMA_CREAM -> new PotionData(PotionType.FIRE_RESISTANCE, splash, false, false);
                case GHAST_TEAR -> new PotionData(PotionType.REGENERATION, splash, false, false);
                case SPIDER_EYE -> new PotionData(PotionType.POISON, splash, false, false);
                case GLISTERING_MELON -> new PotionData(PotionType.HEALING, splash, false, false);
                case BLAZE_POWDER -> new PotionData(PotionType.STRENGTH, splash, false, false);
                default -> null;
            };
        }
        return null;
    }

    public static List<PotionData> creativePotions() {
        List<PotionData> potions = new ArrayList<>();
        add(potions, new PotionData(PotionType.WATER, false, false, false));
        add(potions, new PotionData(PotionType.AWKWARD, false, false, false));
        add(potions, new PotionData(PotionType.THICK, false, false, false));
        add(potions, new PotionData(PotionType.MUNDANE, false, false, false));
        add(potions, new PotionData(PotionType.MUNDANE, false, true, false));
        for (PotionType type : List.of(PotionType.REGENERATION, PotionType.SWIFTNESS, PotionType.FIRE_RESISTANCE,
                PotionType.POISON, PotionType.HEALING, PotionType.WEAKNESS, PotionType.STRENGTH,
                PotionType.SLOWNESS, PotionType.HARMING)) {
            add(potions, new PotionData(type, false, false, false));
            if (canExtend(new PotionData(type, false, false, false))) {
                add(potions, new PotionData(type, false, true, false));
            }
            if (canEnhance(new PotionData(type, false, false, false))) {
                add(potions, new PotionData(type, false, false, true));
            }
        }
        int normalCount = potions.size();
        for (int i = 0; i < normalCount; i++) {
            PotionData potion = potions.get(i);
            add(potions, new PotionData(potion.type(), true, potion.extended(), potion.enhanced()));
        }
        return potions;
    }

    private static void add(List<PotionData> potions, PotionData potion) {
        if (!potions.contains(potion)) {
            potions.add(potion);
        }
    }

    private static boolean canExtend(PotionData potion) {
        if (potion.extended() || potion.enhanced()) {
            return false;
        }
        return switch (potion.type()) {
            case REGENERATION, SWIFTNESS, FIRE_RESISTANCE, POISON, WEAKNESS, STRENGTH, SLOWNESS -> true;
            default -> false;
        };
    }

    private static boolean canEnhance(PotionData potion) {
        if (potion.extended() || potion.enhanced()) {
            return false;
        }
        return switch (potion.type()) {
            case REGENERATION, SWIFTNESS, POISON, HEALING, STRENGTH, HARMING -> true;
            default -> false;
        };
    }

    private static PotionType corrupt(PotionType type) {
        return switch (type) {
            case HEALING, POISON -> PotionType.HARMING;
            case SWIFTNESS, FIRE_RESISTANCE -> PotionType.SLOWNESS;
            case STRENGTH, REGENERATION, WATER, AWKWARD, THICK, MUNDANE -> PotionType.WEAKNESS;
            default -> null;
        };
    }
}
