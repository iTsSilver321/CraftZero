package com.craftzero.entity.mob;

/**
 * Release 1.0 Easy-baseline values for the mobs currently implemented.
 */
public final class MobBalance {
    public record Spec(float width, float height, float maxHealth, float moveSpeed,
            boolean hostile, boolean burnsInSunlight, int experienceValue) {
    }

    public static final Spec ZOMBIE = new Spec(0.6f, 1.95f, 20.0f, 0.15f, true, true, 5);
    public static final Spec SKELETON = new Spec(0.6f, 1.95f, 20.0f, 0.15f, true, true, 5);
    public static final Spec CREEPER = new Spec(0.6f, 1.7f, 20.0f, 0.15f, true, false, 5);
    public static final Spec SPIDER = new Spec(1.4f, 0.9f, 16.0f, 0.2f, true, false, 5);
    public static final Spec PIG = new Spec(0.9f, 0.9f, 10.0f, 0.1f, false, false, 1);
    public static final Spec COW = new Spec(0.9f, 1.4f, 10.0f, 0.1f, false, false, 1);
    public static final Spec SHEEP = new Spec(0.9f, 1.3f, 8.0f, 0.1f, false, false, 1);
    public static final Spec CHICKEN = new Spec(0.4f, 0.7f, 4.0f, 0.12f, false, false, 1);

    private MobBalance() {
    }
}
