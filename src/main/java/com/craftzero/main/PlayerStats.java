package com.craftzero.main;

/**
 * Player survival stats: health, hunger, and saturation.
 * Implements Minecraft-style mechanics:
 * - 20 health points (10 hearts)
 * - 20 hunger points (10 drumsticks)
 * - Saturation as hidden hunger buffer
 * - Health regeneration when hunger is high
 * - Hunger drain from activities
 */
public class PlayerStats {

    // Max values
    public static final float MAX_HEALTH = 20.0f;
    public static final float MAX_HUNGER = 20.0f;
    public static final float MAX_SATURATION = 20.0f;

    // Current values
    private float health;
    private float hunger;
    private float saturation;

    // Hunger drain rates (per second)
    private static final float HUNGER_DRAIN_IDLE = 0.0f; // No drain while idle
    private static final float HUNGER_DRAIN_WALKING = 0.005f; // Slow drain while walking
    private static final float HUNGER_DRAIN_SPRINTING = 0.1f; // Faster drain while sprinting
    private static final float HUNGER_DRAIN_JUMPING = 0.05f; // Drain per jump

    // Regeneration
    private static final float REGEN_THRESHOLD = 18.0f; // Hunger level needed to regenerate
    private static final float REGEN_RATE = 0.5f; // Health per second when regenerating
    private static final float REGEN_HUNGER_COST = 0.75f; // Hunger consumed per health point regenerated

    // Starvation
    private static final float STARVATION_DAMAGE = 0.5f; // Damage per second when starving
    private float starvationTimer = 0f;

    // Spawn invincibility
    private static final float SPAWN_INVINCIBILITY_TIME = 5.0f; // 5 seconds of invincibility after spawn
    private float invincibilityTimer = SPAWN_INVINCIBILITY_TIME;

    // State tracking
    private boolean isDead = false;

    public PlayerStats() {
        this.health = MAX_HEALTH;
        this.hunger = MAX_HUNGER;
        this.saturation = 5.0f; // Start with some saturation
        this.invincibilityTimer = SPAWN_INVINCIBILITY_TIME;
    }

    /**
     * Update stats each frame.
     * 
     * @param deltaTime   time since last frame
     * @param isSprinting whether player is sprinting
     * @param isMoving    whether player is moving
     */
    public void update(float deltaTime, boolean isSprinting, boolean isMoving) {
        if (isDead)
            return;

        // Update invincibility timer
        if (invincibilityTimer > 0) {
            invincibilityTimer -= deltaTime;
        }

        // Hunger drain from activities
        float drainRate = HUNGER_DRAIN_IDLE;
        if (isMoving) {
            drainRate = isSprinting ? HUNGER_DRAIN_SPRINTING : HUNGER_DRAIN_WALKING;
        }

        // Drain saturation first, then hunger
        if (drainRate > 0) {
            float drain = drainRate * deltaTime;
            if (saturation > 0) {
                saturation = Math.max(0, saturation - drain);
            } else {
                hunger = Math.max(0, hunger - drain);
            }
        }

        // Health regeneration when hunger is high
        if (hunger >= REGEN_THRESHOLD && health < MAX_HEALTH) {
            float regenAmount = REGEN_RATE * deltaTime;
            float actualRegen = Math.min(regenAmount, MAX_HEALTH - health);
            health += actualRegen;

            // Consume hunger for regeneration
            float hungerCost = actualRegen * REGEN_HUNGER_COST;
            if (saturation > 0) {
                saturation = Math.max(0, saturation - hungerCost);
            } else {
                hunger = Math.max(0, hunger - hungerCost);
            }
        }

        // Starvation damage when hunger is 0 (respects invincibility)
        if (hunger <= 0 && invincibilityTimer <= 0) {
            starvationTimer += deltaTime;
            if (starvationTimer >= 1.0f) {
                damageInternal(STARVATION_DAMAGE);
                starvationTimer = 0f;
            }
        } else {
            starvationTimer = 0f;
        }
    }

    /**
     * Apply damage to the player (respects invincibility).
     * 
     * @param amount damage amount
     */
    public void damage(float amount) {
        if (isDead || invincibilityTimer > 0)
            return;
        damageInternal(amount);
    }

    /**
     * Internal damage method that ignores invincibility (for starvation).
     */
    private void damageInternal(float amount) {
        if (isDead)
            return;
        health = Math.max(0, health - amount);
        if (health <= 0) {
            isDead = true;
        }
    }

    /**
     * Heal the player.
     * 
     * @param amount heal amount
     */
    public void heal(float amount) {
        if (isDead)
            return;
        health = Math.min(MAX_HEALTH, health + amount);
    }

    /**
     * Feed the player (eating food).
     * 
     * @param foodPoints       hunger points restored
     * @param saturationPoints saturation points added
     */
    public void feed(float foodPoints, float saturationPoints) {
        if (isDead)
            return;
        hunger = Math.min(MAX_HUNGER, hunger + foodPoints);
        saturation = Math.min(MAX_SATURATION, saturation + saturationPoints);
    }

    /**
     * Called when player jumps - drains some hunger.
     */
    public void onJump() {
        if (saturation > 0) {
            saturation = Math.max(0, saturation - HUNGER_DRAIN_JUMPING);
        } else {
            hunger = Math.max(0, hunger - HUNGER_DRAIN_JUMPING);
        }
    }

    /**
     * Respawn the player (reset all stats).
     */
    public void respawn() {
        health = MAX_HEALTH;
        hunger = MAX_HUNGER;
        saturation = 5.0f;
        isDead = false;
        starvationTimer = 0f;
        invincibilityTimer = SPAWN_INVINCIBILITY_TIME;
    }

    /**
     * Check if player is currently invincible.
     */
    public boolean isInvincible() {
        return invincibilityTimer > 0;
    }

    // Getters

    public float getHealth() {
        return health;
    }

    public float getHunger() {
        return hunger;
    }

    public float getSaturation() {
        return saturation;
    }

    public boolean isDead() {
        return isDead;
    }

    /**
     * Get health as number of full hearts (0-10).
     */
    public int getFullHearts() {
        return (int) (health / 2);
    }

    /**
     * Check if there's a half heart to display.
     */
    public boolean hasHalfHeart() {
        return (health % 2) >= 1;
    }

    /**
     * Get hunger as number of full drumsticks (0-10).
     */
    public int getFullHungerBars() {
        return (int) (hunger / 2);
    }

    /**
     * Check if there's a half hunger bar to display.
     */
    public boolean hasHalfHungerBar() {
        return (hunger % 2) >= 1;
    }
}
