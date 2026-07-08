package com.craftzero.progression;

import com.craftzero.entity.LivingEntity;
import com.craftzero.entity.mob.Mob;
import com.craftzero.entity.mob.MobDefinition;
import com.craftzero.inventory.ItemStack;
import com.craftzero.inventory.ItemType;
import com.craftzero.inventory.ToolType;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public final class EnchantmentResolver {
    private EnchantmentResolver() {
    }

    public static int getLevel(ItemStack stack, EnchantmentType type) {
        if (stack == null || stack.isEmpty() || type == null) {
            return 0;
        }
        int best = 0;
        for (EnchantmentInstance enchantment : stack.getEnchantments()) {
            if (enchantment.type() == type) {
                best = Math.max(best, enchantment.level());
            }
        }
        return best;
    }

    public static boolean has(ItemStack stack, EnchantmentType type) {
        return getLevel(stack, type) > 0;
    }

    public static boolean isEnchantable(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        if (!stack.getEnchantments().isEmpty()) {
            return false;
        }
        ItemType type = stack.getType();
        return isTableEnchantableTool(type) || ArmorMaterial.materialOf(type) != null;
    }

    public static int enchantability(ItemType type) {
        ArmorMaterial armor = ArmorMaterial.materialOf(type);
        if (armor != null) {
            return armor.getEnchantability();
        }
        if (type == null || !type.isTool()) {
            return 0;
        }
        if (!isTableEnchantableTool(type)) {
            return 0;
        }
        return switch (type.getToolType().getTier()) {
            case WOOD -> 15;
            case STONE -> 5;
            case IRON -> 14;
            case DIAMOND -> 10;
            case GOLD -> 22;
            default -> 0;
        };
    }

    public static int offerCost(Random random, int slot, int bookshelfPower, ItemStack stack) {
        if (!isEnchantable(stack)) {
            return 0;
        }
        int shelves = Math.min(30, Math.max(0, bookshelfPower));
        int base = random.nextInt(8) + 1 + (shelves >> 1) + random.nextInt(shelves + 1);
        int cost = switch (slot) {
            case 0 -> Math.max(base / 3, 1);
            case 1 -> base * 2 / 3 + 1;
            default -> Math.max(base, shelves * 2);
        };
        return Math.min(50, cost);
    }

    public static List<EnchantmentInstance> generate(Random random, ItemStack stack, int cost) {
        if (!isEnchantable(stack) || cost <= 0) {
            return List.of();
        }
        int level = adjustedEnchantmentLevel(random, stack, cost);

        List<EnchantmentInstance> candidates = candidatesFor(stack.getType(), level);
        if (candidates.isEmpty()) {
            return List.of();
        }
        List<EnchantmentInstance> result = new ArrayList<>();
        result.add(pickWeighted(random, candidates));
        int chanceLevel = level;
        while (random.nextInt(50) <= chanceLevel) {
            chanceLevel >>= 1;
            List<EnchantmentInstance> compatible = new ArrayList<>();
            for (EnchantmentInstance candidate : candidates) {
                if (!contains(result, candidate.type()) && compatibleWithAll(result, candidate.type())) {
                    compatible.add(candidate);
                }
            }
            if (compatible.isEmpty()) {
                break;
            }
            result.add(pickWeighted(random, compatible));
        }
        return result;
    }

    public static float attackDamageBonus(ItemStack weapon) {
        if (weapon == null || weapon.isEmpty()) {
            return 0.0f;
        }
        return getLevel(weapon, EnchantmentType.SHARPNESS) * 1.25f;
    }

    public static float attackDamageBonus(ItemStack weapon, LivingEntity target) {
        float bonus = attackDamageBonus(weapon);
        if (weapon == null || weapon.isEmpty() || target == null) {
            return bonus;
        }
        if (isUndead(target)) {
            bonus = Math.max(bonus, getLevel(weapon, EnchantmentType.SMITE) * 2.5f);
        }
        if (isArthropod(target)) {
            bonus = Math.max(bonus, getLevel(weapon, EnchantmentType.BANE_OF_ARTHROPODS) * 2.5f);
        }
        return bonus;
    }

    public static float miningSpeedBonus(ItemStack tool) {
        int efficiency = getLevel(tool, EnchantmentType.EFFICIENCY);
        return efficiency <= 0 ? 0.0f : efficiency * efficiency + 1.0f;
    }

    public static boolean shouldPreventDurabilityLoss(ItemStack stack, Random random) {
        int unbreaking = getLevel(stack, EnchantmentType.UNBREAKING);
        if (unbreaking <= 0 || random == null) {
            return false;
        }
        if (ArmorMaterial.materialOf(stack.getType()) != null && random.nextFloat() < 0.6f) {
            return false;
        }
        return random.nextInt(unbreaking + 1) > 0;
    }

    static int adjustedEnchantmentLevel(Random random, ItemStack stack, int cost) {
        int enchantability = Math.max(1, enchantability(stack == null ? null : stack.getType()));
        int bonusBound = enchantability / 4 + 1;
        int adjusted = 1 + random.nextInt(bonusBound) + random.nextInt(bonusBound) + cost;
        float variance = (random.nextFloat() + random.nextFloat() - 1.0f) * 0.15f;
        return Math.max(1, Math.round(adjusted * (1.0f + variance)));
    }

    private static List<EnchantmentInstance> candidatesFor(ItemType type, int level) {
        List<EnchantmentInstance> candidates = new ArrayList<>();
        ArmorMaterial armor = ArmorMaterial.materialOf(type);
        if (armor != null) {
            addIfInRange(candidates, EnchantmentType.PROTECTION, level, 4);
            addIfInRange(candidates, EnchantmentType.FIRE_PROTECTION, level, 4);
            addIfInRange(candidates, EnchantmentType.BLAST_PROTECTION, level, 4);
            addIfInRange(candidates, EnchantmentType.PROJECTILE_PROTECTION, level, 4);
            addIfInRange(candidates, EnchantmentType.UNBREAKING, level, 3);
            if (ArmorMaterial.slotOf(type) == ArmorSlot.BOOTS) {
                addIfInRange(candidates, EnchantmentType.FEATHER_FALLING, level, 4);
            }
            if (ArmorMaterial.slotOf(type) == ArmorSlot.HELMET) {
                addIfInRange(candidates, EnchantmentType.RESPIRATION, level, 3);
                addIfInRange(candidates, EnchantmentType.AQUA_AFFINITY, level, 1);
            }
            return candidates;
        }
        if (type == null || !type.isTool()) {
            return candidates;
        }
        ToolType.Category category = type.getToolType().getCategory();
        if (!isTableEnchantableTool(type)) {
            return candidates;
        }
        if (category == ToolType.Category.SWORD) {
            addIfInRange(candidates, EnchantmentType.SHARPNESS, level, 5);
            addIfInRange(candidates, EnchantmentType.SMITE, level, 5);
            addIfInRange(candidates, EnchantmentType.BANE_OF_ARTHROPODS, level, 5);
            addIfInRange(candidates, EnchantmentType.KNOCKBACK, level, 2);
            addIfInRange(candidates, EnchantmentType.FIRE_ASPECT, level, 2);
            addIfInRange(candidates, EnchantmentType.LOOTING, level, 3);
        } else if (category == ToolType.Category.PICKAXE || category == ToolType.Category.SHOVEL
                || category == ToolType.Category.AXE) {
            addIfInRange(candidates, EnchantmentType.EFFICIENCY, level, 5);
            addIfInRange(candidates, EnchantmentType.SILK_TOUCH, level, 1);
            addIfInRange(candidates, EnchantmentType.FORTUNE, level, 3);
        }
        addIfInRange(candidates, EnchantmentType.UNBREAKING, level, 3);
        return candidates;
    }

    private static boolean isTableEnchantableTool(ItemType type) {
        if (type == null || !type.isTool()) {
            return false;
        }
        ToolType.Category category = type.getToolType().getCategory();
        return category == ToolType.Category.SWORD
                || category == ToolType.Category.PICKAXE
                || category == ToolType.Category.SHOVEL
                || category == ToolType.Category.AXE;
    }

    private static void addIfInRange(List<EnchantmentInstance> out, EnchantmentType type, int level, int maxLevel) {
        for (int enchantLevel = 1; enchantLevel <= maxLevel; enchantLevel++) {
            if (canApplyAtAdjustedLevel(type, enchantLevel, level)) {
                out.add(new EnchantmentInstance(type, enchantLevel));
            }
        }
    }

    static boolean canApplyAtAdjustedLevel(EnchantmentType type, int enchantmentLevel, int adjustedLevel) {
        if (type == null || enchantmentLevel <= 0) {
            return false;
        }
        return adjustedLevel >= minEnchantability(type, enchantmentLevel)
                && adjustedLevel <= maxEnchantability(type, enchantmentLevel);
    }

    private static int minEnchantability(EnchantmentType type, int level) {
        return switch (type) {
            case PROTECTION -> 1 + (level - 1) * 11;
            case FIRE_PROTECTION -> 10 + (level - 1) * 8;
            case FEATHER_FALLING -> 5 + (level - 1) * 6;
            case BLAST_PROTECTION -> 5 + (level - 1) * 8;
            case PROJECTILE_PROTECTION -> 3 + (level - 1) * 6;
            case RESPIRATION -> level * 10;
            case AQUA_AFFINITY -> 1;
            case SHARPNESS -> 1 + (level - 1) * 11;
            case SMITE, BANE_OF_ARTHROPODS -> 5 + (level - 1) * 8;
            case KNOCKBACK -> 5 + (level - 1) * 20;
            case FIRE_ASPECT -> 10 + (level - 1) * 20;
            case LOOTING, FORTUNE -> 15 + (level - 1) * 9;
            case EFFICIENCY -> 1 + (level - 1) * 10;
            case SILK_TOUCH -> 15;
            case UNBREAKING -> 5 + (level - 1) * 8;
            case POWER, PUNCH, FLAME, INFINITY -> Integer.MAX_VALUE;
        };
    }

    private static int maxEnchantability(EnchantmentType type, int level) {
        return switch (type) {
            case PROTECTION -> minEnchantability(type, level) + 20;
            case FIRE_PROTECTION -> minEnchantability(type, level) + 12;
            case FEATHER_FALLING -> minEnchantability(type, level) + 10;
            case BLAST_PROTECTION -> minEnchantability(type, level) + 12;
            case PROJECTILE_PROTECTION -> minEnchantability(type, level) + 15;
            case RESPIRATION -> minEnchantability(type, level) + 30;
            case AQUA_AFFINITY -> 41;
            case SHARPNESS, SMITE, BANE_OF_ARTHROPODS -> minEnchantability(type, level) + 20;
            case KNOCKBACK, FIRE_ASPECT, LOOTING, EFFICIENCY, SILK_TOUCH,
                    UNBREAKING, FORTUNE -> minEnchantability(type, level) + 50;
            case POWER, PUNCH, FLAME, INFINITY -> Integer.MIN_VALUE;
        };
    }

    private static EnchantmentInstance pickWeighted(Random random, List<EnchantmentInstance> candidates) {
        int totalWeight = 0;
        for (EnchantmentInstance candidate : candidates) {
            totalWeight += weight(candidate.type());
        }
        int roll = random.nextInt(Math.max(1, totalWeight));
        for (EnchantmentInstance candidate : candidates) {
            roll -= weight(candidate.type());
            if (roll < 0) {
                return candidate;
            }
        }
        return candidates.get(candidates.size() - 1);
    }

    private static int weight(EnchantmentType type) {
        return switch (type) {
            case PROTECTION, SHARPNESS, EFFICIENCY -> 10;
            case FIRE_PROTECTION, FEATHER_FALLING, PROJECTILE_PROTECTION,
                    SMITE, BANE_OF_ARTHROPODS, KNOCKBACK, UNBREAKING -> 5;
            case BLAST_PROTECTION, RESPIRATION, AQUA_AFFINITY,
                    FIRE_ASPECT, LOOTING, FORTUNE,
                    POWER, PUNCH, FLAME, INFINITY -> 2;
            case SILK_TOUCH -> 1;
        };
    }

    private static boolean contains(List<EnchantmentInstance> enchantments, EnchantmentType type) {
        for (EnchantmentInstance enchantment : enchantments) {
            if (enchantment.type() == type) {
                return true;
            }
        }
        return false;
    }

    private static boolean compatibleWithAll(List<EnchantmentInstance> enchantments, EnchantmentType type) {
        for (EnchantmentInstance enchantment : enchantments) {
            if (!compatible(enchantment.type(), type)) {
                return false;
            }
        }
        return true;
    }

    public static boolean compatible(EnchantmentType a, EnchantmentType b) {
        if (a == b) {
            return false;
        }
        if (isProtection(a) && isProtection(b)) {
            return false;
        }
        if (isDamageEnchant(a) && isDamageEnchant(b)) {
            return false;
        }
        return !((a == EnchantmentType.SILK_TOUCH && b == EnchantmentType.FORTUNE)
                || (a == EnchantmentType.FORTUNE && b == EnchantmentType.SILK_TOUCH));
    }

    private static boolean isProtection(EnchantmentType type) {
        return type == EnchantmentType.PROTECTION || type == EnchantmentType.FIRE_PROTECTION
                || type == EnchantmentType.BLAST_PROTECTION || type == EnchantmentType.PROJECTILE_PROTECTION;
    }

    private static boolean isDamageEnchant(EnchantmentType type) {
        return type == EnchantmentType.SHARPNESS || type == EnchantmentType.SMITE
                || type == EnchantmentType.BANE_OF_ARTHROPODS;
    }

    private static boolean isUndead(LivingEntity entity) {
        if (entity instanceof Mob mob) {
            MobDefinition definition = mob.getDefinition();
            return definition == MobDefinition.ZOMBIE
                    || definition == MobDefinition.SKELETON
                    || definition == MobDefinition.ZOMBIE_PIGMAN
                    || definition == MobDefinition.GIANT;
        }
        return false;
    }

    private static boolean isArthropod(LivingEntity entity) {
        if (entity instanceof Mob mob) {
            MobDefinition definition = mob.getDefinition();
            return definition == MobDefinition.SPIDER
                    || definition == MobDefinition.CAVE_SPIDER
                    || definition == MobDefinition.SILVERFISH;
        }
        return false;
    }
}
