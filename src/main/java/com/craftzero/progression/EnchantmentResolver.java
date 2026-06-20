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
        return type.isTool() || type == ItemType.BOW || ArmorMaterial.materialOf(type) != null;
    }

    public static int enchantability(ItemType type) {
        ArmorMaterial armor = ArmorMaterial.materialOf(type);
        if (armor != null) {
            return armor.getEnchantability();
        }
        if (type == null || !type.isTool()) {
            return type == ItemType.BOW ? 1 : 0;
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
        int enchantability = Math.max(1, enchantability(stack.getType()));
        int adjusted = 1 + random.nextInt((enchantability >> 1) + 1)
                + random.nextInt((enchantability >> 1) + 1) + cost;
        float variance = (random.nextFloat() + random.nextFloat() - 1.0f) * 0.15f;
        int level = Math.max(1, Math.round(adjusted * (1.0f + variance)));

        List<EnchantmentInstance> candidates = candidatesFor(stack.getType(), level);
        if (candidates.isEmpty()) {
            return List.of();
        }
        List<EnchantmentInstance> result = new ArrayList<>();
        result.add(candidates.get(random.nextInt(candidates.size())));
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
            result.add(compatible.get(random.nextInt(compatible.size())));
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
        return unbreaking > 0 && random != null && random.nextInt(unbreaking + 1) > 0;
    }

    private static List<EnchantmentInstance> candidatesFor(ItemType type, int level) {
        List<EnchantmentInstance> candidates = new ArrayList<>();
        ArmorMaterial armor = ArmorMaterial.materialOf(type);
        if (armor != null) {
            addIfInRange(candidates, EnchantmentType.PROTECTION, level, 1, 12, 4);
            addIfInRange(candidates, EnchantmentType.FIRE_PROTECTION, level, 10, 18, 4);
            addIfInRange(candidates, EnchantmentType.BLAST_PROTECTION, level, 5, 13, 4);
            addIfInRange(candidates, EnchantmentType.PROJECTILE_PROTECTION, level, 3, 9, 4);
            addIfInRange(candidates, EnchantmentType.UNBREAKING, level, 5, 55, 3);
            if (ArmorMaterial.slotOf(type) == ArmorSlot.BOOTS) {
                addIfInRange(candidates, EnchantmentType.FEATHER_FALLING, level, 5, 11, 4);
            }
            if (ArmorMaterial.slotOf(type) == ArmorSlot.HELMET) {
                addIfInRange(candidates, EnchantmentType.RESPIRATION, level, 10, 40, 3);
                addIfInRange(candidates, EnchantmentType.AQUA_AFFINITY, level, 1, 41, 1);
            }
            return candidates;
        }
        if (type == ItemType.BOW) {
            addIfInRange(candidates, EnchantmentType.POWER, level, 1, 16, 5);
            addIfInRange(candidates, EnchantmentType.PUNCH, level, 12, 37, 2);
            addIfInRange(candidates, EnchantmentType.FLAME, level, 20, 50, 1);
            addIfInRange(candidates, EnchantmentType.INFINITY, level, 20, 50, 1);
            addIfInRange(candidates, EnchantmentType.UNBREAKING, level, 5, 55, 3);
            return candidates;
        }
        if (type == null || !type.isTool()) {
            return candidates;
        }
        ToolType.Category category = type.getToolType().getCategory();
        if (category == ToolType.Category.SWORD) {
            addIfInRange(candidates, EnchantmentType.SHARPNESS, level, 1, 21, 5);
            addIfInRange(candidates, EnchantmentType.SMITE, level, 5, 25, 5);
            addIfInRange(candidates, EnchantmentType.BANE_OF_ARTHROPODS, level, 5, 25, 5);
            addIfInRange(candidates, EnchantmentType.KNOCKBACK, level, 5, 55, 2);
            addIfInRange(candidates, EnchantmentType.FIRE_ASPECT, level, 10, 60, 2);
            addIfInRange(candidates, EnchantmentType.LOOTING, level, 15, 65, 3);
        } else if (category == ToolType.Category.PICKAXE || category == ToolType.Category.SHOVEL
                || category == ToolType.Category.AXE) {
            addIfInRange(candidates, EnchantmentType.EFFICIENCY, level, 1, 51, 5);
            addIfInRange(candidates, EnchantmentType.SILK_TOUCH, level, 15, 65, 1);
            addIfInRange(candidates, EnchantmentType.FORTUNE, level, 15, 65, 3);
        }
        addIfInRange(candidates, EnchantmentType.UNBREAKING, level, 5, 55, 3);
        return candidates;
    }

    private static void addIfInRange(List<EnchantmentInstance> out, EnchantmentType type, int level,
            int minBase, int maxBase, int maxLevel) {
        for (int enchantLevel = 1; enchantLevel <= maxLevel; enchantLevel++) {
            int min = minBase + (enchantLevel - 1) * 10;
            int max = maxBase + (enchantLevel - 1) * 10;
            if (level >= min && level <= max) {
                out.add(new EnchantmentInstance(type, enchantLevel));
            }
        }
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
                    || definition == MobDefinition.ZOMBIE_PIGMAN;
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
