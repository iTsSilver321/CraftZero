package com.craftzero.main;

import com.craftzero.progression.AchievementTracker;
import com.craftzero.progression.PlayerProgression;
import com.craftzero.progression.StatusEffectInstance;
import com.craftzero.progression.StatusEffectType;
import com.craftzero.world.BlockType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

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

    // FoodStats-style exhaustion. Release 1.0 drains one saturation/food point
    // only after exhaustion crosses the 4.0 threshold.
    private static final float EXHAUSTION_THRESHOLD = 4.0f;
    public static final float MAX_EXHAUSTION = 40.0f;
    private static final float EXHAUSTION_EPSILON = 0.00001f;
    private static final float EXHAUSTION_WALKING_PER_BLOCK = 0.01f;
    private static final float EXHAUSTION_SPRINTING_PER_BLOCK = 0.1f;
    private static final float EXHAUSTION_JUMPING = 0.2f;
    private static final float EXHAUSTION_BLOCK_BREAK = 0.025f;
    private static final float EXHAUSTION_ATTACK = 0.3f;
    private static final float EXHAUSTION_HURT = 0.3f;
    private float exhaustion = 0f;

    // Regeneration
    private static final float REGEN_THRESHOLD = 18.0f; // Hunger level needed to regenerate
    private static final float REGEN_INTERVAL_SECONDS = 4.0f;
    private static final float EXHAUSTION_NATURAL_REGEN = 3.0f;
    private static final float PEACEFUL_REGEN_INTERVAL_SECONDS = 1.0f;
    private float regenTimer = 0f;
    private float peacefulRegenTimer = 0f;

    // Starvation
    private static final float STARVATION_DAMAGE = 1.0f;
    private static final float STARVATION_INTERVAL_SECONDS = 4.0f;
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
    private static final float AIR_TICKS_PER_SECOND = 20.0f;
    private float currentAir;
    private float drownTimer = 0f;
    private float airTickAccumulator = 0f;
    private final PlayerProgression progression;
    private final AchievementTracker achievements;
    private final PlayerStatistics statistics;
    private final List<StatusEffectInstance> activeEffects;

    public PlayerStats() {
        this.health = MAX_HEALTH;
        this.hunger = MAX_HUNGER;
        this.saturation = 15.0f; // Start with high saturation (requested 15)
        this.invincibilityTimer = SPAWN_INVINCIBILITY_TIME;
        this.currentAir = MAX_AIR_SECONDS;
        this.progression = new PlayerProgression();
        this.achievements = new AchievementTracker();
        this.statistics = new PlayerStatistics();
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
        update(deltaTime, isSprinting, isMoving, Difficulty.NORMAL);
    }

    public void update(float deltaTime, boolean isSprinting, boolean isMoving, Difficulty difficulty) {
        update(deltaTime, isSprinting, isMoving, difficulty, 0.0f);
    }

    public void update(float deltaTime, boolean isSprinting, boolean isMoving, Difficulty difficulty,
            float horizontalDistance) {
        if (isDead)
            return;

        Difficulty activeDifficulty = difficulty == null ? Difficulty.NORMAL : difficulty;

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

        if (isMoving && horizontalDistance > 0.0f) {
            addExhaustion(horizontalDistance
                    * (isSprinting ? EXHAUSTION_SPRINTING_PER_BLOCK : EXHAUSTION_WALKING_PER_BLOCK));
        }
        processExhaustion(activeDifficulty);

        if (activeDifficulty == Difficulty.PEACEFUL && health < MAX_HEALTH) {
            peacefulRegenTimer += deltaTime;
            while (peacefulRegenTimer >= PEACEFUL_REGEN_INTERVAL_SECONDS && health < MAX_HEALTH) {
                peacefulRegenTimer -= PEACEFUL_REGEN_INTERVAL_SECONDS;
                heal(1.0f);
            }
        } else {
            peacefulRegenTimer = 0f;
        }

        // Health regeneration when hunger is high.
        if (hunger >= REGEN_THRESHOLD && health < MAX_HEALTH) {
            regenTimer += deltaTime;
            while (regenTimer >= REGEN_INTERVAL_SECONDS && health < MAX_HEALTH) {
                regenTimer -= REGEN_INTERVAL_SECONDS;
                heal(1.0f);
                addExhaustion(EXHAUSTION_NATURAL_REGEN);
            }
        } else {
            regenTimer = 0f;
        }

        // Starvation damage when hunger is 0 (respects invincibility and
        // Release-era difficulty floors).
        if (activeDifficulty != Difficulty.PEACEFUL && hunger <= 0 && invincibilityTimer <= 0) {
            starvationTimer += deltaTime;
            while (starvationTimer >= STARVATION_INTERVAL_SECONDS && !isDead) {
                starvationTimer -= STARVATION_INTERVAL_SECONDS;
                float minimumHealth = starvationMinimumHealth(activeDifficulty);
                if (health > minimumHealth) {
                    damageInternal(Math.min(STARVATION_DAMAGE, health - minimumHealth));
                }
            }
        } else {
            starvationTimer = 0f;
        }
    }

    private static float starvationMinimumHealth(Difficulty difficulty) {
        return switch (difficulty) {
            case HARD -> 0.0f;
            case NORMAL -> 1.0f;
            case EASY -> 10.0f;
            case PEACEFUL -> MAX_HEALTH;
        };
    }

    /**
     * Update air/breath stats.
     * 
     * @param isUnderwater whether player's head is underwater
     * @param deltaTime    time since last frame
     */
    public void updateAir(boolean isUnderwater, float deltaTime) {
        updateAir(isUnderwater, deltaTime, 0, null);
    }

    public void updateAir(boolean isUnderwater, float deltaTime, int respirationLevel, Random random) {
        if (isDead)
            return;

        float safeDelta = Math.max(0.0f, deltaTime);
        if (isUnderwater) {
            if (hasEffect(StatusEffectType.WATER_BREATHING)) {
                currentAir = MAX_AIR_SECONDS;
                drownTimer = 0f;
                airTickAccumulator = 0f;
                return;
            }
            airTickAccumulator += safeDelta * AIR_TICKS_PER_SECOND;
            while (airTickAccumulator >= 1.0f) {
                airTickAccumulator -= 1.0f;
                if (currentAir > 0.0f) {
                    if (shouldConsumeAir(respirationLevel, random)) {
                        currentAir = Math.max(0.0f, currentAir - 1.0f / AIR_TICKS_PER_SECOND);
                    }
                    if (currentAir > 0.0f) {
                        drownTimer = 0f;
                    }
                } else {
                    currentAir = 0.0f;
                    // Drowning damage (2.0 damage every second)
                    drownTimer += 1.0f / AIR_TICKS_PER_SECOND;
                    if (drownTimer >= 1.0f) {
                        damageInternal(2.0f);
                        drownTimer = 0f;
                    }
                }
            }
        } else {
            // Recover air quickly when out of water
            currentAir = Math.min(MAX_AIR_SECONDS, currentAir + safeDelta * 5.0f);
            drownTimer = 0f;
            airTickAccumulator = 0f;
        }
    }

    private static boolean shouldConsumeAir(int respirationLevel, Random random) {
        int level = Math.max(0, respirationLevel);
        if (level <= 0 || random == null) {
            return true;
        }
        return random.nextInt(level + 1) == 0;
    }

    /**
     * Apply damage to the player (respects invincibility).
     * 
     * @param amount damage amount
     */
    public boolean damage(float amount) {
        return damage(amount, false);
    }

    public boolean damage(float amount, boolean halfHurtResistanceWindow) {
        if (isDead || amount <= 0 || invincibilityTimer > 0) {
            return false;
        }
        float hurtResistanceThreshold = halfHurtResistanceWindow
                ? CombatRules.PLAYER_HURT_INVULNERABILITY_TICKS / 40.0f
                : 0.0f;
        if (hurtInvulnerabilityTimer > hurtResistanceThreshold) {
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

    public void grantInvincibility(float seconds) {
        invincibilityTimer = Math.max(invincibilityTimer, seconds);
        hurtInvulnerabilityTimer = 0.0f;
        lastDamageAmount = 0.0f;
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
        if (effect.type() == StatusEffectType.REGENERATION && isEffectReady(effect, 50)) {
            heal(1.0f);
        } else if (effect.type() == StatusEffectType.POISON && isEffectReady(effect, 25) && health > 1.0f) {
            health = Math.max(1.0f, health - 1.0f);
        } else if (effect.type() == StatusEffectType.HUNGER) {
            addExhaustion(0.025f * (effect.amplifier() + 1));
        }
    }

    private static boolean isEffectReady(StatusEffectInstance effect, int baseInterval) {
        int interval = effect.amplifier() >= 31 ? 0 : baseInterval >> effect.amplifier();
        return interval <= 0 || effect.durationTicks() % interval == 0;
    }

    /**
     * Internal damage method that ignores invincibility (for starvation/drowning).
     */
    private void damageInternal(float amount) {
        if (isDead)
            return;
        float previousHealth = health;
        health = Math.max(0, health - amount);
        statistics.recordDamageTaken(previousHealth - health);
        if (health <= 0) {
            isDead = true;
            if (previousHealth > 0.0f) {
                statistics.recordDeath();
            }
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
        saturation = Math.min(hunger, saturation + saturationPoints);
    }

    /**
     * Add action exhaustion to the hidden FoodStats buffer.
     *
     * @param amount exhaustion to add
     */
    public void addExhaustion(float amount) {
        if (isDead || amount <= 0.0f) {
            return;
        }
        exhaustion = Math.min(MAX_EXHAUSTION, exhaustion + amount);
    }

    private void processExhaustion(Difficulty difficulty) {
        while (exhaustion > EXHAUSTION_THRESHOLD + EXHAUSTION_EPSILON) {
            exhaustion = Math.max(0.0f, exhaustion - EXHAUSTION_THRESHOLD);
            if (saturation > 0.0f) {
                saturation = Math.max(0.0f, saturation - 1.0f);
            } else if (difficulty != Difficulty.PEACEFUL) {
                hunger = Math.max(0.0f, hunger - 1.0f);
            }
        }
    }

    /**
     * Called when player jumps - adds Release-style exhaustion.
     */
    public void onJump() {
        statistics.recordJump();
        addExhaustion(EXHAUSTION_JUMPING);
    }

    public void onBlockBreak() {
        statistics.recordBlockMined();
        addExhaustion(EXHAUSTION_BLOCK_BREAK);
    }

    public void onBlockBreak(BlockType type) {
        statistics.recordBlockMined(type);
        addExhaustion(EXHAUSTION_BLOCK_BREAK);
    }

    public void onAttack() {
        addExhaustion(EXHAUSTION_ATTACK);
    }

    public void onHurt() {
        addExhaustion(EXHAUSTION_HURT);
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
        regenTimer = 0f;
        peacefulRegenTimer = 0f;
        exhaustion = 0f;
        drownTimer = 0f;
        airTickAccumulator = 0f;
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
        restore(health, hunger, saturation, currentAir, 0.0f);
    }

    public void restore(float health, float hunger, float saturation, float currentAir, float exhaustion) {
        this.health = Math.max(0, Math.min(MAX_HEALTH, health));
        this.hunger = Math.max(0, Math.min(MAX_HUNGER, hunger));
        this.saturation = Math.max(0, Math.min(this.hunger, saturation));
        this.currentAir = Math.max(0, Math.min(MAX_AIR_SECONDS, currentAir));
        this.isDead = this.health <= 0;
        this.starvationTimer = 0f;
        this.regenTimer = 0f;
        this.peacefulRegenTimer = 0f;
        this.exhaustion = Math.max(0.0f, Math.min(MAX_EXHAUSTION, exhaustion));
        this.drownTimer = 0f;
        this.airTickAccumulator = 0f;
        this.invincibilityTimer = 0f;
        this.hurtInvulnerabilityTimer = 0f;
        this.lastDamageAmount = 0f;
    }

    public void restoreRuntimeState(float regenTimer, float peacefulRegenTimer, float starvationTimer,
            float drownTimer, float airTickAccumulator, float invincibilityTimer,
            float hurtInvulnerabilityTimer, float lastDamageAmount) {
        this.regenTimer = clampFinite(regenTimer, 0.0f, REGEN_INTERVAL_SECONDS);
        this.peacefulRegenTimer = clampFinite(peacefulRegenTimer, 0.0f, PEACEFUL_REGEN_INTERVAL_SECONDS);
        this.starvationTimer = clampFinite(starvationTimer, 0.0f, STARVATION_INTERVAL_SECONDS);
        this.drownTimer = clampFinite(drownTimer, 0.0f, 1.0f);
        this.airTickAccumulator = clampFinite(airTickAccumulator, 0.0f, 0.9999f);
        this.invincibilityTimer = clampFinite(invincibilityTimer, 0.0f, SPAWN_INVINCIBILITY_TIME);
        this.hurtInvulnerabilityTimer = clampFinite(hurtInvulnerabilityTimer, 0.0f,
                CombatRules.PLAYER_HURT_INVULNERABILITY_TICKS / 20.0f);
        this.lastDamageAmount = clampFinite(lastDamageAmount, 0.0f, MAX_HEALTH);
        if (this.hurtInvulnerabilityTimer <= 0.0f) {
            this.lastDamageAmount = 0.0f;
        }
        if (isDead) {
            this.regenTimer = 0.0f;
            this.peacefulRegenTimer = 0.0f;
            this.starvationTimer = 0.0f;
            this.drownTimer = 0.0f;
            this.airTickAccumulator = 0.0f;
        }
    }

    public void restoreFoodTickTimer(int ticks) {
        float seconds = Math.max(0, ticks) / 20.0f;
        if (hunger >= REGEN_THRESHOLD && health < MAX_HEALTH && !isDead) {
            regenTimer = clampFinite(seconds, 0.0f, REGEN_INTERVAL_SECONDS);
        } else if (hunger <= 0.0f && !isDead) {
            starvationTimer = clampFinite(seconds, 0.0f, STARVATION_INTERVAL_SECONDS);
        }
    }

    public int getFoodTickTimerTicks() {
        if (hunger >= REGEN_THRESHOLD && health < MAX_HEALTH && !isDead) {
            return Math.max(0, Math.round(regenTimer * 20.0f));
        }
        if (hunger <= 0.0f && !isDead) {
            return Math.max(0, Math.round(starvationTimer * 20.0f));
        }
        return 0;
    }

    public int getHurtTimeTicks() {
        float visibleTicks = Math.max(0.0f, hurtInvulnerabilityTimer * 20.0f
                - CombatRules.PLAYER_HURT_INVULNERABILITY_TICKS / 2.0f);
        return Math.min(10, Math.round(visibleTicks));
    }

    public float getRegenTimer() {
        return regenTimer;
    }

    public float getPeacefulRegenTimer() {
        return peacefulRegenTimer;
    }

    public float getStarvationTimer() {
        return starvationTimer;
    }

    public float getDrownTimer() {
        return drownTimer;
    }

    public float getAirTickAccumulator() {
        return airTickAccumulator;
    }

    public float getInvincibilityTimer() {
        return invincibilityTimer;
    }

    public float getHurtInvulnerabilityTimer() {
        return hurtInvulnerabilityTimer;
    }

    public float getLastDamageAmount() {
        return lastDamageAmount;
    }

    private static float clampFinite(float value, float min, float max) {
        if (!Float.isFinite(value)) {
            return min;
        }
        return Math.max(min, Math.min(max, value));
    }

    public PlayerProgression getProgression() {
        return progression;
    }

    public AchievementTracker getAchievements() {
        return achievements;
    }

    public PlayerStatistics getStatistics() {
        return statistics;
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
                        || (effect.amplifier() == existing.amplifier()
                                && effect.durationTicks() > existing.durationTicks())) {
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

    public float getMiningSpeedMultiplier() {
        float multiplier = 1.0f;
        int haste = getEffectAmplifier(StatusEffectType.HASTE);
        if (haste >= 0) {
            multiplier *= 1.0f + 0.2f * (haste + 1);
        }
        int fatigue = getEffectAmplifier(StatusEffectType.MINING_FATIGUE);
        if (fatigue >= 0) {
            multiplier *= Math.max(0.0f, 1.0f - 0.2f * (fatigue + 1));
        }
        return multiplier;
    }

    public float getAttackDamageBonus() {
        float bonus = 0.0f;
        int strength = getEffectAmplifier(StatusEffectType.STRENGTH);
        if (strength >= 0) {
            bonus += 3 << strength;
        }
        int weakness = getEffectAmplifier(StatusEffectType.WEAKNESS);
        if (weakness >= 0) {
            bonus -= 2 << weakness;
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

    public float getExhaustion() {
        return exhaustion;
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
