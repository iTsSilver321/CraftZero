package com.craftzero.entity.mob;

import java.util.EnumSet;
import java.util.Set;

/**
 * Single creation point for runtime, save/load, spawners, and generated
 * structures. Definitions that are declared for future parity but not backed by
 * behavior stay out of IMPLEMENTED so they cannot leak into gameplay.
 */
public final class MobFactory {
    private static final Set<MobDefinition> IMPLEMENTED = EnumSet.of(
            MobDefinition.ZOMBIE,
            MobDefinition.SKELETON,
            MobDefinition.CREEPER,
            MobDefinition.SPIDER,
            MobDefinition.PIG,
            MobDefinition.COW,
            MobDefinition.SHEEP,
            MobDefinition.CHICKEN,
            MobDefinition.WOLF,
            MobDefinition.MOOSHROOM,
            MobDefinition.VILLAGER,
            MobDefinition.SLIME,
            MobDefinition.SQUID,
            MobDefinition.ENDERMAN,
            MobDefinition.CAVE_SPIDER,
            MobDefinition.SILVERFISH,
            MobDefinition.GIANT,
            MobDefinition.GHAST,
            MobDefinition.ZOMBIE_PIGMAN,
            MobDefinition.BLAZE,
            MobDefinition.MAGMA_CUBE,
            MobDefinition.ENDER_DRAGON,
            MobDefinition.SNOW_GOLEM);

    private MobFactory() {
    }

    public static boolean isImplemented(MobDefinition definition) {
        return definition != null && IMPLEMENTED.contains(definition);
    }

    public static Set<MobDefinition> implementedDefinitions() {
        return Set.copyOf(IMPLEMENTED);
    }

    public static Mob create(MobDefinition definition) {
        if (!isImplemented(definition)) {
            return null;
        }
        return switch (definition) {
            case ZOMBIE -> new Zombie();
            case SKELETON -> new Skeleton();
            case CREEPER -> new Creeper();
            case SPIDER -> new Spider();
            case PIG -> new Pig();
            case COW -> new Cow();
            case SHEEP -> new Sheep();
            case CHICKEN -> new Chicken();
            case WOLF -> new Wolf();
            case MOOSHROOM -> new Mooshroom();
            case VILLAGER -> new Villager();
            case SLIME -> new Slime(4);
            case SQUID -> new Squid();
            case ENDERMAN -> new Enderman();
            case CAVE_SPIDER -> new CaveSpider();
            case SILVERFISH -> new Silverfish();
            case GIANT -> new Giant();
            case GHAST -> new Ghast();
            case ZOMBIE_PIGMAN -> new ZombiePigman();
            case BLAZE -> new Blaze();
            case MAGMA_CUBE -> new MagmaCube(4);
            case ENDER_DRAGON -> new EnderDragon();
            case SNOW_GOLEM -> new SnowGolem();
            default -> null;
        };
    }
}
