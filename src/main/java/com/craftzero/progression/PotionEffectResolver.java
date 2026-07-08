package com.craftzero.progression;

import com.craftzero.combat.DamageSource;
import com.craftzero.entity.LivingEntity;
import com.craftzero.entity.mob.Mob;
import com.craftzero.entity.mob.MobDefinition;
import com.craftzero.main.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * Release 1.0 potion identity and effect application.
 */
public final class PotionEffectResolver {
    private PotionEffectResolver() {
    }

    public static List<StatusEffectInstance> effects(PotionData potion) {
        List<StatusEffectInstance> effects = new ArrayList<>();
        if (potion == null) {
            return effects;
        }
        int amplifier = potion.enhanced() ? 1 : 0;
        switch (potion.type()) {
            case REGENERATION -> effects.add(new StatusEffectInstance(StatusEffectType.REGENERATION,
                    potion.extended() ? 2400 : potion.enhanced() ? 440 : 900, amplifier));
            case SWIFTNESS -> effects.add(new StatusEffectInstance(StatusEffectType.SPEED,
                    potion.extended() ? 9600 : potion.enhanced() ? 1800 : 3600, amplifier));
            case FIRE_RESISTANCE -> effects.add(new StatusEffectInstance(StatusEffectType.FIRE_RESISTANCE,
                    potion.extended() ? 9600 : 3600, 0));
            case POISON -> effects.add(new StatusEffectInstance(StatusEffectType.POISON,
                    potion.extended() ? 2400 : potion.enhanced() ? 440 : 900, amplifier));
            case STRENGTH -> effects.add(new StatusEffectInstance(StatusEffectType.STRENGTH,
                    potion.extended() ? 9600 : potion.enhanced() ? 1800 : 3600, amplifier));
            case SLOWNESS -> effects.add(new StatusEffectInstance(StatusEffectType.SLOWNESS,
                    potion.extended() ? 4800 : 1800, 0));
            case WEAKNESS -> effects.add(new StatusEffectInstance(StatusEffectType.WEAKNESS,
                    potion.extended() ? 4800 : 1800, 0));
            default -> {
            }
        }
        return effects;
    }

    public static boolean isInstant(PotionData potion) {
        return potion != null && (potion.type() == PotionType.HEALING || potion.type() == PotionType.HARMING);
    }

    public static void applyToPlayer(Player player, PotionData potion, float strength) {
        applyToPlayer(player, potion, strength, DamageSource.magic());
    }

    public static void applyToPlayer(Player player, PotionData potion, float strength, DamageSource harmfulSource) {
        if (player == null || potion == null) {
            return;
        }
        if (isInstant(potion)) {
            boolean undead = false;
            applyInstantToPlayer(player, potion, strength, undead, harmfulSource);
            return;
        }
        for (StatusEffectInstance effect : effects(potion)) {
            int duration = Math.round(effect.durationTicks() * strength);
            if (duration > 20) {
                player.getStats().addEffect(new StatusEffectInstance(effect.type(), duration, effect.amplifier()));
            }
        }
    }

    public static void applyToLiving(LivingEntity entity, PotionData potion, float strength) {
        applyToLiving(entity, potion, strength, DamageSource.magic());
    }

    public static void applyToLiving(LivingEntity entity, PotionData potion, float strength,
            DamageSource harmfulSource) {
        if (entity == null || potion == null) {
            return;
        }
        boolean undead = isUndead(entity);
        if (isInstant(potion)) {
            applyInstantToLiving(entity, potion, strength, undead, harmfulSource);
            return;
        }
        for (StatusEffectInstance effect : effects(potion)) {
            int duration = Math.round(effect.durationTicks() * strength);
            if (duration > 20) {
                entity.addEffect(new StatusEffectInstance(effect.type(), duration, effect.amplifier()));
            }
        }
    }

    private static void applyInstantToPlayer(Player player, PotionData potion, float strength, boolean undead,
            DamageSource harmfulSource) {
        boolean healing = potion.type() == PotionType.HEALING;
        if (healing != undead) {
            int amount = instantAmount(potion, strength, true);
            if (amount <= 0) {
                return;
            }
            player.getStats().heal(amount);
        } else {
            int amount = instantAmount(potion, strength, false);
            if (amount <= 0) {
                return;
            }
            player.hurt(amount, harmfulSource == null ? DamageSource.magic() : harmfulSource);
        }
    }

    private static void applyInstantToLiving(LivingEntity entity, PotionData potion, float strength, boolean undead,
            DamageSource harmfulSource) {
        boolean healing = potion.type() == PotionType.HEALING;
        if (healing != undead) {
            int amount = instantAmount(potion, strength, true);
            if (amount <= 0) {
                return;
            }
            entity.heal(amount);
        } else {
            int amount = instantAmount(potion, strength, false);
            if (amount <= 0) {
                return;
            }
            entity.damage(amount, harmfulSource == null ? DamageSource.magic() : harmfulSource);
        }
    }

    private static int instantAmount(PotionData potion, float strength, boolean beneficial) {
        int amplifier = potion.enhanced() ? 1 : 0;
        int base = (beneficial ? 4 : 6) << amplifier;
        return (int) (base * Math.max(0.0f, strength) + 0.5f);
    }

    public static boolean isUndead(LivingEntity entity) {
        if (entity instanceof Mob mob) {
            MobDefinition definition = mob.getDefinition();
            return definition == MobDefinition.ZOMBIE
                    || definition == MobDefinition.SKELETON
                    || definition == MobDefinition.ZOMBIE_PIGMAN
                    || definition == MobDefinition.GIANT;
        }
        return false;
    }

    public static String displayName(PotionData potion) {
        if (potion == null || potion.type() == PotionType.WATER) {
            return potion != null && potion.splash() ? "Splash Water Bottle" : "Water Bottle";
        }
        String prefix = potion.splash() ? "Splash Potion of " : "Potion of ";
        String name = switch (potion.type()) {
            case AWKWARD -> "Awkward";
            case THICK -> "Thick";
            case MUNDANE -> "Mundane";
            case REGENERATION -> "Regeneration";
            case SWIFTNESS -> "Swiftness";
            case FIRE_RESISTANCE -> "Fire Resistance";
            case POISON -> "Poison";
            case HEALING -> "Healing";
            case WEAKNESS -> "Weakness";
            case STRENGTH -> "Strength";
            case SLOWNESS -> "Slowness";
            case HARMING -> "Harming";
            default -> potion.type().name();
        };
        if (potion.enhanced()) {
            name += " II";
        }
        if (potion.extended()) {
            name += " Extended";
        }
        return prefix + name;
    }
}
