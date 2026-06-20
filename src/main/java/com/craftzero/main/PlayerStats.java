package com.craftzero.main;

import com.craftzero.progression.PlayerProgression;
import com.craftzero.progression.StatusEffectInstance;
import com.craftzero.progression.StatusEffectType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
    private static final float HUNGER_DRAIN_WALKING = 0.002f; // Very slow drain while walking
    private static final float HUNGER_DRAIN_SPRINTING = 0.03f; // Slower drain while sprinting
    private static final float HUNGER_DRAIN_JUMPING = 0.01f; // Reduced drain per jump

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
    private float hurtInvulnerabilityTimer = 0.0f;
    private float lastDamageAmount = 0.0f;

    // State tracking
    private boolean isDead = false;

    // Air / Breath
    public static final float MAX_AIR_SECONDS = 15.0f; // 15 seconds of breath
    private float currentAir;
    private float drownTimer = 0f;
    private final PlayerProgression progression;
    private final List<StatusEffectInstance> activeEffects;

    public PlayerStats() {
        this.health = MAX_HEALTH;
        this.hunger = MAX_HUNGER;
        this.saturation = 15.0f; // Start with high saturation (requested 15)
        this.invincibilityTimer = SPAWN_INVINCIBILITY_TIME;
        this.currentAir = MAX_AIR_SECONDS;
        this.progression = new PlayerProgression();
        this.activeEffects = new ArrayList<>();
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
        if (hurtInvulnerabilityTimer > 0) {
            hurtInvulnerabilityTimer -= deltaTime;
            if (hurtInvulnerabilityTimer <= 0) {
                lastDamageAmount = 0.0f;
            }
        }
        tickEffects(deltaTime);

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
     * Update air/breath stats.
     * 
     * @param isUnderwater whether player's head is underwater
     * @param deltaTime    time since last frame
     */
    public void updateAir(boolean isUnderwater, float deltaTime) {
        if (isDead)
            return;

        if (isUnderwater) {
            currentAir -= deltaTime;
            if (currentAir <= 0) {
                currentAir = 0;
                // Drowning damage (2.0 damage every second)
                drownTimer += deltaTime;
                if (drownTimer >= 1.0f) {
                    damageInternal(2.0f);
                    drownTimer = 0f;
                }
            } else {
                drownTimer = 0f;
            }
        } else {
            // Recover air quickly when out of water
            currentAir = Math.min(MAX_AIR_SECONDS, currentAir + deltaTime * 5.0f);
            drownTimer = 0f;
        }
    }

    /**
     * Apply damage to the player (respects invincibility).
     * 
     * @param amount damage amount
     */
    public boolean damage(float amount) {
        if (isDead || amount <= 0 || invincibilityTimer > 0) {
            return false;
        }
        if (hurtInvulnerabilityTimer > 0) {
            if (amount <= lastDamageAmount) {
                return false;
            }
            damageInternal(amount - lastDamageAmount);
            lastDamageAmount = amount;
            return true;
        }
        damageInternal(amount);
        hurtInvulnerabilityTimer = CombatRules.PLAYER_HURT_INVULNERABILITY_TICKS / 20.0f;
        lastDamageAmount = amount;
        return true;
    }

    private void tickEffects(float deltaTime) {
        int ticks = Math.max(1, Math.round(deltaTime * 20.0f));
        for (int step = 0; step < ticks; step++) {
            for (int i = activeEffects.size() - 1; i >= 0; i--) {
                StatusEffectInstance effect = activeEffects.get(i);
                applyEffectTick(effect);
                StatusEffectInstance next = effect.ticked();
                if (next.expired()) {
                    activeEffects.remove(i);
                } else {
                    activeEffects.set(i, next);
                }
            }
        }
    }

    private void applyEffectTick(StatusEffectInstance effect) {
        if (effect.type() == StatusEffectType.REGENERATION && effect.durationTicks() % 50 == 0) {
            heal(1.0f + effect.amplifier());
        } else if (effect.type() == StatusEffectType.POISON && effect.durationTicks() % 25 == 0 && health > 1.0f) {
            health = Math.max(1.0f, health - (1.0f + effect.amplifier()));
        } else if (effect.type() == StatusEffectType.HUNGER && effect.durationTicks() % 20 == 0) {
            hunger = Math.max(0, hunger - (0.025f * (effect.amplifier() + 1)));
        }
    }

    /**
     * Internal damage method that ignores invincibility (for starvation/drowning).
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
        currentAir = MAX_AIR_SECONDS;
        isDead = false;
        starvationTimer = 0f;
        drownTimer = 0f;
        invincibilityTimer = SPAWN_INVINCIBILITY_TIME;
        hurtInvulnerabilityTimer = 0.0f;
        lastDamageAmount = 0.0f;
        activeEffects.clear();
    }

    /**
     * Check if player is currently invincible.
     */
    public boolean isInvincible() {
        return invincibilityTimer > 0 || hurtInvulnerabilityTimer > 0;
    }

    public void restore(float health, float hunger, float saturation, float currentAir) {
        this.health = Math.max(0, Math.min(MAX_HEALTH, health));
        this.hunger = Math.max(0, Math.min(MAX_HUNGER, hunger));
        this.saturation = Math.max(0, Math.min(MAX_SATURATION, saturation));
        this.currentAir = Math.max(0, Math.min(MAX_AIR_SECONDS, currentAir));
        this.isDead = this.health <= 0;
        this.starvationTimer = 0f;
        this.drownTimer = 0f;
        this.invincibilityTimer = 0f;
        this.hurtInvulnerabilityTimer = 0f;
        this.lastDamageAmount = 0f;
    }

    public PlayerProgression getProgression() {
        return progression;
    }

    public List<StatusEffectInstance> getActiveEffects() {
        return Collections.unmodifiableList(activeEffects);
    }

    public void setActiveEffects(List<StatusEffectInstance> effects) {
        activeEffects.clear();
        if (effects != null) {
            for (StatusEffectInstance effect : effects) {
                if (effect != null && !effect.expired()) {
                    activeEffects.add(effect);
                }
            }
        }
    }

    public void addEffect(StatusEffectInstance effect) {
        if (effect == null || effect.expired()) {
            return;
        }
        for (int i = 0; i < activeEffects.size(); i++) {
            StatusEffectInstance existing = activeEffects.get(i);
            if (existing.type() == effect.type()) {
                if (effect.amplifier() > existing.amplifier()
                        || effect.durationTicks() > existing.durationTicks()) {
                    activeEffects.set(i, effect);
                }
                return;
            }
        }
        activeEffects.add(effect);
    }

    public void clearEffects() {
        activeEffects.clear();
    }

    public boolean hasEffect(StatusEffectType type) {
        return getEffectAmplifier(type) >= 0;
    }

    public int getEffectAmplifier(StatusEffectType type) {
        if (type == null) {
            return -1;
        }
        int best = -1;
        for (StatusEffectInstance effect : activeEffects) {
            if (effect.type() == type && !effect.expired()) {
                best = Math.max(best, effect.amplifier());
            }
        }
        return best;
    }

    public float getMovementSpeedMultiplier() {
        float multiplier = 1.0f;
        int speed = getEffectAmplifier(StatusEffectType.SPEED);
        if (speed >= 0) {
            multiplier += 0.2f * (speed + 1);
        }
        int slowness = getEffectAmplifier(StatusEffectType.SLOWNESS);
        if (slowness >= 0) {
            multiplier -= 0.15f * (slowness + 1);
        }
        return Math.max(0.1f, multiplier);
    }

    public float getAttackDamageBonus() {
        float bonus = 0.0f;
        int strength = getEffectAmplifier(StatusEffectType.STRENGTH);
        if (strength >= 0) {
            bonus += 3.0f * (strength + 1);
        }
        int weakness = getEffectAmplifier(StatusEffectType.WEAKNESS);
        if (weakness >= 0) {
            bonus -= 0.5f * (weakness + 1);
        }
        return bonus;
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

    public float getCurrentAir() {
        return currentAir;
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
