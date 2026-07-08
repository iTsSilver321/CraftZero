package com.craftzero.progression;

import java.util.List;

/**
 * Shared Release-era visual data for potion/status effects.
 */
public final class StatusEffectVisuals {
    public static final int DEFAULT_COLOR = 0x7F7F7F;
    public static final int DEFAULT_POTION_COLOR = 0x385DC6;

    private StatusEffectVisuals() {
    }

    public static int color(StatusEffectType type) {
        if (type == null) {
            return DEFAULT_COLOR;
        }
        return switch (type) {
            case SPEED -> 0x7CAFC6;
            case SLOWNESS -> 0x5A6C81;
            case HASTE -> 0xD9C043;
            case MINING_FATIGUE -> 0x4A4217;
            case STRENGTH -> 0x932423;
            case INSTANT_HEALTH -> 0xF82423;
            case INSTANT_DAMAGE -> 0x430A09;
            case JUMP_BOOST -> 0x22FF4C;
            case NAUSEA -> 0x551D4A;
            case REGENERATION -> 0xCD5CAB;
            case RESISTANCE -> 0x99453A;
            case FIRE_RESISTANCE -> 0xE49A3A;
            case WATER_BREATHING -> 0x2E5299;
            case INVISIBILITY -> 0x7F8392;
            case BLINDNESS -> 0x1F1F23;
            case NIGHT_VISION -> 0x1F1FA1;
            case HUNGER -> 0x587653;
            case WEAKNESS -> 0x484D48;
            case POISON -> 0x4E9331;
        };
    }

    public static int mixedColor(List<StatusEffectInstance> effects) {
        if (effects == null || effects.isEmpty()) {
            return DEFAULT_COLOR;
        }

        int totalRed = 0;
        int totalGreen = 0;
        int totalBlue = 0;
        int totalWeight = 0;
        for (StatusEffectInstance effect : effects) {
            if (effect == null || effect.expired()) {
                continue;
            }
            int weight = Math.max(1, effect.amplifier() + 1);
            int color = color(effect.type());
            totalRed += ((color >> 16) & 0xFF) * weight;
            totalGreen += ((color >> 8) & 0xFF) * weight;
            totalBlue += (color & 0xFF) * weight;
            totalWeight += weight;
        }
        if (totalWeight <= 0) {
            return DEFAULT_COLOR;
        }
        int red = totalRed / totalWeight;
        int green = totalGreen / totalWeight;
        int blue = totalBlue / totalWeight;
        return (red << 16) | (green << 8) | blue;
    }

    public static int potionColor(PotionData data) {
        PotionType type = data == null || data.type() == null ? PotionType.WATER : data.type();
        return switch (type) {
            case REGENERATION -> color(StatusEffectType.REGENERATION);
            case SWIFTNESS -> color(StatusEffectType.SPEED);
            case FIRE_RESISTANCE -> color(StatusEffectType.FIRE_RESISTANCE);
            case POISON -> color(StatusEffectType.POISON);
            case HEALING -> color(StatusEffectType.INSTANT_HEALTH);
            case WEAKNESS -> color(StatusEffectType.WEAKNESS);
            case STRENGTH -> color(StatusEffectType.STRENGTH);
            case SLOWNESS -> color(StatusEffectType.SLOWNESS);
            case HARMING -> color(StatusEffectType.INSTANT_DAMAGE);
            case WATER, AWKWARD, THICK, MUNDANE -> DEFAULT_POTION_COLOR;
        };
    }
}
