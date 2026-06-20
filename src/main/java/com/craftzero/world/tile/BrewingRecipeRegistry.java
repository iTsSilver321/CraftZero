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

    public static ItemStack brew(ItemStack bottle, ItemStack ingredient) {
        if (bottle == null || bottle.isEmpty() || ingredient == null || ingredient.isEmpty()) {
            return null;
        }
        if (bottle.getType() != ItemType.POTION) {
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
        if (current.splash()) {
            return null;
        }
        if (ingredient == ItemType.REDSTONE && canExtend(current)) {
            return new PotionData(current.type(), false, true, false);
        }
        if (ingredient == ItemType.GLOWSTONE_DUST && canEnhance(current)) {
            return new PotionData(current.type(), false, false, true);
        }
        if (ingredient == ItemType.FERMENTED_SPIDER_EYE) {
            PotionType corrupt = corrupt(current.type());
            if (corrupt != null) {
                boolean keepExtended = current.extended()
                        && (corrupt == PotionType.SLOWNESS || corrupt == PotionType.WEAKNESS);
                boolean keepEnhanced = current.enhanced() && corrupt == PotionType.HARMING;
                return new PotionData(corrupt, false, keepExtended, keepEnhanced);
            }
        }

        if (current.type() == PotionType.WATER) {
            return switch (ingredient) {
                case NETHER_WART -> new PotionData(PotionType.AWKWARD, false, false, false);
                case GLOWSTONE_DUST -> new PotionData(PotionType.THICK, false, false, false);
                case FERMENTED_SPIDER_EYE -> new PotionData(PotionType.WEAKNESS, false, false, false);
                case SUGAR, MAGMA_CREAM, GHAST_TEAR, SPIDER_EYE, BLAZE_POWDER, GLISTERING_MELON, REDSTONE ->
                    new PotionData(PotionType.MUNDANE, false, false, false);
                default -> null;
            };
        }

        if (current.type() == PotionType.AWKWARD) {
            return switch (ingredient) {
                case SUGAR -> new PotionData(PotionType.SWIFTNESS, false, false, false);
                case MAGMA_CREAM -> new PotionData(PotionType.FIRE_RESISTANCE, false, false, false);
                case GHAST_TEAR -> new PotionData(PotionType.REGENERATION, false, false, false);
                case SPIDER_EYE -> new PotionData(PotionType.POISON, false, false, false);
                case GLISTERING_MELON -> new PotionData(PotionType.HEALING, false, false, false);
                case BLAZE_POWDER -> new PotionData(PotionType.STRENGTH, false, false, false);
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
            case STRENGTH, REGENERATION, WATER -> PotionType.WEAKNESS;
            default -> null;
        };
    }
}
