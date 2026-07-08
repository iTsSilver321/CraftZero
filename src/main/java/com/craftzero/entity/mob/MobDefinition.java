package com.craftzero.entity.mob;

import com.craftzero.world.Dimension;

import java.util.EnumSet;
import java.util.Set;

/**
 * Canonical Release 1.0 mob definitions. Implemented mobs can still keep their
 * class-specific AI, but spawning, saving, drops, and rendering should key off
 * this table as the project fills in the remaining mob ecosystem.
 */
public enum MobDefinition {
    ZOMBIE("Zombie", MobCategory.MONSTER, EnumSet.of(Dimension.OVERWORLD), ModelFamily.HUMANOID,
            0.6f, 1.95f, 20.0f, 0.15f, true, 5, 1, 4),
    SKELETON("Skeleton", MobCategory.MONSTER, EnumSet.of(Dimension.OVERWORLD), ModelFamily.SKELETON,
            0.6f, 1.95f, 20.0f, 0.15f, true, 5, 1, 4),
    CREEPER("Creeper", MobCategory.MONSTER, EnumSet.of(Dimension.OVERWORLD), ModelFamily.CREEPER,
            0.6f, 1.7f, 20.0f, 0.15f, false, 5, 1, 4),
    SPIDER("Spider", MobCategory.MONSTER, EnumSet.of(Dimension.OVERWORLD), ModelFamily.SPIDER,
            1.4f, 0.9f, 16.0f, 0.2f, false, 5, 1, 4),
    SLIME("Slime", MobCategory.MONSTER, EnumSet.of(Dimension.OVERWORLD), ModelFamily.SLIME,
            0.6f, 0.6f, 16.0f, 0.2f, false, 4, 1, 1),
    ENDERMAN("Enderman", MobCategory.MONSTER, EnumSet.of(Dimension.OVERWORLD, Dimension.THE_END), ModelFamily.HUMANOID,
            0.6f, 2.9f, 40.0f, 0.3f, false, 5, 1, 4),
    CAVE_SPIDER("Cave Spider", MobCategory.MONSTER, EnumSet.of(Dimension.OVERWORLD), ModelFamily.SPIDER,
            0.7f, 0.5f, 12.0f, 0.2f, false, 5, 1, 4),
    SILVERFISH("Silverfish", MobCategory.MONSTER, EnumSet.of(Dimension.OVERWORLD), ModelFamily.SILVERFISH,
            0.3f, 0.7f, 8.0f, 0.25f, false, 5, 1, 4),
    GIANT("Giant", MobCategory.MONSTER, EnumSet.of(Dimension.OVERWORLD), ModelFamily.HUMANOID,
            3.6f, 10.8f, 100.0f, 0.5f, false, 5, 1, 1),
    GHAST("Ghast", MobCategory.MONSTER, EnumSet.of(Dimension.NETHER), ModelFamily.GHAST,
            4.0f, 4.0f, 10.0f, 0.1f, false, 5, 1, 1),
    ZOMBIE_PIGMAN("Zombie Pigman", MobCategory.MONSTER, EnumSet.of(Dimension.NETHER), ModelFamily.HUMANOID,
            0.6f, 1.95f, 20.0f, 0.23f, false, 5, 4, 4),
    BLAZE("Blaze", MobCategory.MONSTER, EnumSet.of(Dimension.NETHER), ModelFamily.BLAZE,
            0.6f, 1.8f, 20.0f, 0.23f, false, 10, 1, 4),
    MAGMA_CUBE("Magma Cube", MobCategory.MONSTER, EnumSet.of(Dimension.NETHER), ModelFamily.SLIME,
            0.6f, 0.6f, 16.0f, 0.2f, false, 4, 1, 4),
    ENDER_DRAGON("Ender Dragon", MobCategory.BOSS, EnumSet.of(Dimension.THE_END), ModelFamily.DRAGON,
            16.0f, 8.0f, 200.0f, 0.7f, false, 0, 1, 1),

    PIG("Pig", MobCategory.CREATURE, EnumSet.of(Dimension.OVERWORLD), ModelFamily.QUADRUPED,
            0.9f, 0.9f, 10.0f, 0.1f, false, 1, 4, 4),
    COW("Cow", MobCategory.CREATURE, EnumSet.of(Dimension.OVERWORLD), ModelFamily.QUADRUPED,
            0.9f, 1.4f, 10.0f, 0.1f, false, 1, 4, 4),
    SHEEP("Sheep", MobCategory.CREATURE, EnumSet.of(Dimension.OVERWORLD), ModelFamily.QUADRUPED,
            0.9f, 1.3f, 8.0f, 0.1f, false, 1, 4, 4),
    CHICKEN("Chicken", MobCategory.CREATURE, EnumSet.of(Dimension.OVERWORLD), ModelFamily.CHICKEN,
            0.4f, 0.7f, 4.0f, 0.12f, false, 1, 4, 4),
    SQUID("Squid", MobCategory.WATER_CREATURE, EnumSet.of(Dimension.OVERWORLD), ModelFamily.SQUID,
            0.95f, 0.95f, 10.0f, 0.08f, false, 1, 1, 4),
    WOLF("Wolf", MobCategory.CREATURE, EnumSet.of(Dimension.OVERWORLD), ModelFamily.WOLF,
            0.6f, 0.85f, 8.0f, 0.3f, false, 1, 4, 4),
    MOOSHROOM("Mooshroom", MobCategory.CREATURE, EnumSet.of(Dimension.OVERWORLD), ModelFamily.QUADRUPED,
            0.9f, 1.4f, 10.0f, 0.1f, false, 1, 4, 8),
    VILLAGER("Villager", MobCategory.AMBIENT, EnumSet.of(Dimension.OVERWORLD), ModelFamily.VILLAGER,
            0.6f, 1.8f, 20.0f, 0.2f, false, 0, 1, 1),
    SNOW_GOLEM("Snow Golem", MobCategory.UTILITY, EnumSet.of(Dimension.OVERWORLD), ModelFamily.SNOW_GOLEM,
            0.7f, 1.9f, 4.0f, 0.2f, false, 0, 1, 1);

    private final String displayName;
    private final MobCategory category;
    private final Set<Dimension> dimensions;
    private final ModelFamily modelFamily;
    private final float width;
    private final float height;
    private final float maxHealth;
    private final float moveSpeed;
    private final boolean burnsInSunlight;
    private final int experienceValue;
    private final int minPackSize;
    private final int maxPackSize;

    MobDefinition(String displayName, MobCategory category, Set<Dimension> dimensions, ModelFamily modelFamily,
            float width, float height, float maxHealth, float moveSpeed, boolean burnsInSunlight,
            int experienceValue, int minPackSize, int maxPackSize) {
        this.displayName = displayName;
        this.category = category;
        this.dimensions = Set.copyOf(dimensions);
        this.modelFamily = modelFamily;
        this.width = width;
        this.height = height;
        this.maxHealth = maxHealth;
        this.moveSpeed = moveSpeed;
        this.burnsInSunlight = burnsInSunlight;
        this.experienceValue = experienceValue;
        this.minPackSize = minPackSize;
        this.maxPackSize = maxPackSize;
    }

    public String displayName() {
        return displayName;
    }

    public MobCategory category() {
        return category;
    }

    public Set<Dimension> dimensions() {
        return dimensions;
    }

    public ModelFamily modelFamily() {
        return modelFamily;
    }

    public float width() {
        return width;
    }

    public float height() {
        return height;
    }

    public float maxHealth() {
        return maxHealth;
    }

    public float moveSpeed() {
        return moveSpeed;
    }

    public boolean hostile() {
        return category == MobCategory.MONSTER || category == MobCategory.BOSS;
    }

    public boolean burnsInSunlight() {
        return burnsInSunlight;
    }

    public int experienceValue() {
        return experienceValue;
    }

    public int minPackSize() {
        return minPackSize;
    }

    public int maxPackSize() {
        return maxPackSize;
    }

    public boolean canSpawnIn(Dimension dimension) {
        return dimensions.contains(dimension);
    }

    public enum MobCategory {
        MONSTER,
        CREATURE,
        WATER_CREATURE,
        AMBIENT,
        UTILITY,
        BOSS
    }

    public enum ModelFamily {
        HUMANOID,
        SKELETON,
        CREEPER,
        SPIDER,
        QUADRUPED,
        CHICKEN,
        SQUID,
        WOLF,
        VILLAGER,
        SNOW_GOLEM,
        SLIME,
        GHAST,
        BLAZE,
        SILVERFISH,
        DRAGON
    }
}
