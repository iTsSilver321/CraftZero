package com.craftzero.entity;

import com.craftzero.combat.DamageSource;
import com.craftzero.entity.mob.Wolf;
import com.craftzero.main.CombatRules;
import com.craftzero.progression.StatusEffectInstance;
import com.craftzero.progression.StatusEffectMath;
import com.craftzero.progression.StatusEffectType;
import com.craftzero.progression.StatusEffectVisuals;
import com.craftzero.world.BlockType;
import com.craftzero.world.WorldParticle;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Base class for all living entities (mobs, players).
 * Extends Entity with health, damage, death, and knockback mechanics.
 * 
 * Animation system based on Minecraft:
 * - bodyYaw: Direction the body is facing (rotates to match movement)
 * - headYaw: Direction the head is facing (can look around independently)
 * - limbSwing: Cycle position for leg animation (increases as mob walks)
 * - limbSwingAmount: Amplitude of leg swing (based on movement speed)
 */
public abstract class LivingEntity extends Entity {
    public static final int MAX_AIR_TICKS = 300;
    public static final int DROWN_DAMAGE_AIR_TICKS = -20;
    public static final float DROWN_DAMAGE = 2.0f;
    public static final int MAX_HURT_TIME = 10;
    public static final int MAX_INVULNERABLE_TIME = 20;
    public static final int RECENT_PLAYER_HIT_TICKS = 100;
    private static final int DROWN_BUBBLE_PARTICLES = 8;
    private static final float DROWN_BUBBLE_SCALE = 0.055f;
    private static final int DROWN_BUBBLE_LIFETIME_TICKS = 8;
    private static final int STATUS_EFFECT_PARTICLE_INTERVAL_TICKS = 5;
    private static final float STATUS_EFFECT_PARTICLE_SCALE = 0.20f;
    private static final int STATUS_EFFECT_PARTICLE_LIFETIME_TICKS = 20;
    private static final float BASE_JUMP_MOTION = 0.42f;
    private static final float JUMP_BOOST_MOTION_PER_LEVEL = 0.1f;
    private static final float WATER_MOVEMENT_SCALE = 0.35f;
    private static final float LAVA_MOVEMENT_SCALE = 0.18f;
    private static final float LAND_STEERING_RESPONSE = 0.20f;
    private static final float WATER_STEERING_RESPONSE = 0.16f;
    private static final float LAVA_STEERING_RESPONSE = 0.08f;

    // Health
    protected float health;
    protected float maxHealth;

    // Damage state
    protected int hurtTime; // Ticks since last damage (for animation)
    protected int hurtDuration = MAX_HURT_TIME; // Duration of hurt animation
    protected int invulnerableTime; // Ticks of invulnerability after damage
    protected int maxInvulnerableTime = MAX_INVULNERABLE_TIME; // 1 second of invulnerability
    protected Entity lastDamageSource;
    protected DamageSource lastDamageDetails;
    protected boolean lastDamageSourceHasPosition;
    protected float lastDamageSourceX;
    protected float lastDamageSourceY;
    protected float lastDamageSourceZ;
    protected float lastDamageAmount; // Amount of last damage for invuln frame comparison
    protected int recentPlayerHitTicks;
    protected int recentPlayerLootingLevel;

    // Death state
    protected int deathTime; // Ticks since death (for death animation)
    protected boolean dead;

    // Attack
    protected int attackCooldown;
    protected int maxAttackCooldown = 20; // 1 second between attacks

    // Fire
    protected int fireTicks; // Ticks remaining on fire
    protected int lastFireDamage; // Tick counter for fire damage
    protected int airTicks = MAX_AIR_TICKS;
    protected final List<StatusEffectInstance> activeEffects = new ArrayList<>();

    // Movement AI
    // moveSpeed is in BLOCKS PER TICK (Minecraft-style per-tick physics)
    protected float moveSpeed = 0.1f; // ~0.1 blocks/tick is typical mob walking speed
    protected boolean jumping;
    protected int jumpCooldown; // Cooldown to prevent infinite jumping
    protected int avoidanceCooldown; // Cooldown to prevent avoidance spam
    protected int continuousStuckTicks; // Track how long we are blocked by something tall
    protected int knockbackControlTicks; // Short AI steering lock after taking knockback
    protected boolean isTrapped; // TRUE = mob is completely stuck and waiting for escape
    protected boolean stuckOnLedge; // TRUE = mob is on a ledge and can't find safe path forward
    protected int escapeScanTimer; // Timer for periodic 360° escape scans
    protected int escapingTicks; // Commitment timer - mob actively escaping in a specific direction
    protected float escapeTargetX, escapeTargetZ; // Center of escape block

    // === AI MOVEMENT CONTROL ===
    // AI sets a TARGET direction and speed, body rotates smoothly toward it
    protected float targetYaw; // Direction AI wants to face/move toward
    protected float forwardSpeed; // Speed to move forward (0 = stopped)
    protected float turnSpeed = 8.0f; // Degrees per tick body can rotate

    // === HEAD LOOK BEHAVIOR ===
    // Head can look at targets or look around randomly
    protected float lookAtX, lookAtY, lookAtZ; // Target position to look at

    // =============================================================
    // PHYSICS OVERRIDES (Dynamic Physics)
    // =============================================================

    protected boolean hasLookTarget; // Whether there's a look target
    protected int lookTimer; // Timer for random head movements
    protected float targetHeadYaw; // Target head yaw (smooth interpolation)
    protected float targetHeadPitch; // Target head pitch

    // === MINECRAFT-STYLE ANIMATION VARIABLES ===
    // Body rotation (the body rotates to face movement direction)
    protected float bodyYaw; // Current body facing direction
    protected float prevBodyYaw; // Previous body yaw (for interpolation)

    // Head rotation (can look around independently from body)
    protected float headYaw; // Head yaw relative to body
    protected float headPitch; // Head pitch (up/down)

    // Limb swing animation (legs move based on walking)
    protected float limbSwing; // Current position in walk cycle
    protected float limbSwingAmount; // Amplitude of swing (0 = stopped, 1 = max speed)
    protected float prevLimbSwingAmount; // For interpolation

    protected java.util.Random lookRandom = new java.util.Random();

    public LivingEntity(float width, float height, float maxHealth) {
        super(width, height);
        this.maxHealth = maxHealth;
        this.health = maxHealth;
        this.dead = false;
    }

    @Override
    protected float getStepHeight() {
        return 0.5f;
    }

    @Override
    public void updatePhysics(float deltaTime) {
        super.updatePhysics(deltaTime);
        updateAirSupply();
    }

    @Override
    public void tick() {
        super.tick();

        // Store previous animation values for interpolation
        prevBodyYaw = bodyYaw;
        prevLimbSwingAmount = limbSwingAmount;

        // Decrement timers
        if (hurtTime > 0)
            hurtTime--;
        if (invulnerableTime > 0)
            invulnerableTime--;
        if (attackCooldown > 0)
            attackCooldown--;
        if (knockbackControlTicks > 0)
            knockbackControlTicks--;
        if (recentPlayerHitTicks > 0 && --recentPlayerHitTicks <= 0) {
            recentPlayerLootingLevel = 0;
        }
        tickStatusEffects();

        // Fire damage
        if (fireTicks > 0) {
            if (hasEffect(StatusEffectType.FIRE_RESISTANCE)) {
                extinguish();
            } else if (isWetFromWaterOrRain()) {
                extinguish();
            } else {
                fireTicks--;
                // Deal fire damage every 20 ticks (1 second)
                if (!hasEffect(StatusEffectType.FIRE_RESISTANCE) && ticksExisted - lastFireDamage >= 20) {
                    damage(1.0f, DamageSource.point(DamageSource.Type.FIRE, x, y, z, 0.0f, 0.0f));
                    lastFireDamage = ticksExisted;
                }
            }
        }

        // Death check
        if (health <= 0 && !dead) {
            dead = true;
            deathTime = 0;
            onDeath();
        }

        // Death animation
        if (dead) {
            deathTime++;
            if (deathTime >= 20) { // Remove after 1 second
                remove();
            }
        }

        // NOTE: updateAnimation() is called in Mob.tick() AFTER ai.tick()
        // This ensures motion is calculated using the CURRENT targetYaw from AI

        // ==========================================
        // SMART MOVEMENT & SITUATIONAL AWARENESS
        // ==========================================
        boolean wantsToMove = Math.abs(forwardSpeed) > 0.01f;

        // === ESCAPING STATE: Let AI handle complex escaping ===
        // LivingEntity only handles immediate cliff/obstacle avoidance
        if (escapingTicks > 0) {
            escapingTicks--;
            // AI is handling movement during escape
        }
        // === TRAPPED STATE: Clear after timeout, let AI handle ===
        else if (isTrapped) {
            // AI's EscapeGoal will handle finding escape routes
            // Just reset trapped state after a timeout to avoid permanent freeze
            escapeScanTimer--;
            if (escapeScanTimer <= 0) {
                isTrapped = false; // Allow AI to take control
                continuousStuckTicks = 0;
            }
        }
        // === NORMAL MOVEMENT: Jump, Veer, or Trap ===
        else if (onGround && wantsToMove && !inWater) {
            float yawRad = (float) Math.toRadians(bodyYaw);
            float dx = (float) Math.sin(yawRad);
            float dz = -(float) Math.cos(yawRad);

            // LEDGE CHECK: Don't walk off drops higher than 3 blocks
            float ledgeDist = 0.8f;
            float lx = x + dx * ledgeDist;
            float lz = z + dz * ledgeDist;
            // Check if there's ground within 3 blocks (safe fall distance)
            boolean isLedgeAhead = !hasGroundWithin(lx, y, lz, 3);

            // Only process ledge if we're not already committed to an escape direction
            if (isLedgeAhead && avoidanceCooldown <= 0) {
                // Stop before falling! Find a different path that doesn't lead off a cliff.
                float escapeYaw = findEscapeRoute();
                // Validate escape route doesn't lead to another cliff
                if (escapeYaw != Float.MAX_VALUE && !isLedgeInDirection(escapeYaw)) {
                    // COMMIT fully to escape direction (don't just partial rotate)
                    targetYaw = escapeYaw;
                    bodyYaw = escapeYaw; // Immediately turn to face escape
                    stuckOnLedge = false;
                    avoidanceCooldown = 15; // Commit for 15 ticks before re-evaluating
                } else {
                    // No safe route - signal AI to pick a new target
                    forwardSpeed = 0;
                    continuousStuckTicks = 0;
                    stuckOnLedge = true; // Signal to AI: pick a new destination!
                    avoidanceCooldown = 20; // Don't spam escape checks
                }
            } else if (isLedgeAhead) {
                // We're on cooldown - just keep moving in current direction
                // Don't re-evaluate, trust the previous escape direction
            }
            // 1. IMMEDIATE JUMP CHECK (Don't Think, Just Jump)
            else {
                float checkDist = 0.7f;
                float cx = x + dx * checkDist;
                float cz = z + dz * checkDist;

                if (canJumpAtLocation(cx, cz) && jumpCooldown <= 0) {
                    motionY = jumpMotion();
                    jumpCooldown = 15;
                    continuousStuckTicks = 0;
                }
                // 2. BLOCKED MOVEMENT - Find escape or enter trapped state
                else {
                    // Check if we're actually blocked
                    boolean isBlockedAhead = collidedHorizontally ||
                            evaluatePath(bodyYaw, 0.6f) < 2;

                    if (isBlockedAhead) {
                        continuousStuckTicks++;

                        // FORCE TRAPPED if stuck too long (escapes were false positives)
                        if (continuousStuckTicks > 15) {
                            isTrapped = true;
                            forwardSpeed = 0;
                            escapeScanTimer = 5;
                            avoidanceCooldown = 0;
                        }
                        // Only scan if not on cooldown (prevents oscillation)
                        else if (avoidanceCooldown <= 0) {
                            // Full 360° scan for escape route
                            float bestYaw = Float.MAX_VALUE;
                            int bestScore = -1;

                            for (float offset = 0; offset < 360; offset += 15) {
                                float testYaw = wrapDegrees(bodyYaw + offset);
                                int score1 = evaluatePath(testYaw, 0.5f);
                                int score2 = evaluatePath(testYaw, 1.0f);
                                int score = Math.max(score1, score2);

                                if (score > bestScore && score >= 2) {
                                    bestScore = score;
                                    bestYaw = testYaw;
                                }
                            }

                            if (bestYaw != Float.MAX_VALUE) {
                                // Found escape! COMMIT to this direction
                                targetYaw = bestYaw;
                                bodyYaw = bestYaw;
                                isTrapped = false;
                                avoidanceCooldown = 30; // COMMIT for 30 ticks
                            } else {
                                // NO ESCAPE - Enter trapped state
                                isTrapped = true;
                                forwardSpeed = 0;
                                escapeScanTimer = 5;
                            }
                        }
                    } else {
                        continuousStuckTicks = 0;
                        isTrapped = false;
                    }
                }
            }
        } else if (!isTrapped) {
            continuousStuckTicks = 0;
        }

        // Decrement cooldowns
        if (jumpCooldown > 0) {
            jumpCooldown--;
        }
        if (avoidanceCooldown > 0) {
            avoidanceCooldown--;
        }

        // Handle the jumping flag (for AI-requested jumps or auto-jump)
        // STRICTLY enforce height check for ALL jumping
        if (jumping && onGround && jumpCooldown <= 0) {
            float rad = (float) Math.toRadians(bodyYaw);
            float jx = x + (float) Math.sin(rad) * 0.7f;
            float jz = z - (float) Math.cos(rad) * 0.7f;

            if (canJumpAtLocation(jx, jz)) {
                motionY = jumpMotion();
                jumpCooldown = 20;
            } else {
                // Obstacle too high - kill jump request
                jumpCooldown = 60;
            }
            jumping = false; // Always use one request per tick
        } else {
            jumping = false; // Kill request if on cooldown or in air
        }
    }

    /**
     * Strictly verifies if an obstacle at (nx, nz) is climbable (exactly 1 block).
     */
    private boolean canJumpAtLocation(float nx, float nz) {
        // Must have solid block at feet level (something to push off)
        boolean feetBlocked = isSolidAt(nx, y + 0.5f, nz);
        // BUT it must be clear at 1.25 and 2.25 blocks high
        boolean isJumpableHeight = !isSolidAt(nx, y + 1.25f, nz) && !isSolidAt(nx, y + 2.25f, nz);

        return feetBlocked && isJumpableHeight;
    }

    /**
     * Scans 360 degrees in 30-degree increments to find ANY escape route.
     * Returns the yaw of the best path (highest score), or Float.MAX_VALUE if
     * trapped.
     */
    private float findEscapeRoute() {
        float bestYaw = Float.MAX_VALUE;
        int bestScore = -1;
        float lookDist = 0.8f; // SHORT distance for small spaces!

        // Scan all 12 directions (30° increments)
        for (float offset = 0; offset < 360; offset += 30) {
            float testYaw = wrapDegrees(bodyYaw + offset);
            int score = evaluatePath(testYaw, lookDist);

            // Find the best path (higher score = better)
            if (score > bestScore && score >= 2) {
                bestScore = score;
                bestYaw = testYaw;
            }
        }

        return bestYaw;
    }

    /**
     * Scores a direction to find the best path.
     * Score 3: Open air (Optimal)
     * Score 2: Jumpable 1-block step (Good)
     * Score 1: Hazard/Cliff (Avoid)
     * Score 0: Wall/Impassable (Blocked)
     */
    private int evaluatePath(float testYaw, float dist) {
        float rad = (float) Math.toRadians(testYaw);
        float tx = x + (float) Math.sin(rad) * dist;
        float tz = z - (float) Math.cos(rad) * dist;

        // Check feet level
        boolean solidFeet = isSolidAt(tx, y + 0.5f, tz);

        if (solidFeet) {
            // It's a block - can we jump it?
            if (canJumpAtLocation(tx, tz)) {
                return 2; // Jumpable
            } else {
                return 0; // Wall
            }
        }

        // Feet level is clear - is there a 2-block wall above?
        if (isSolidAt(tx, y + 1.0f, tz)) {
            return 0; // Low overhang/Wall
        }

        // Clear air - is there ground below?
        boolean hasGround = false;
        for (float dy = -0.5f; dy >= -3.0f; dy -= 1.0f) {
            if (isSolidAt(tx, y + dy, tz)) {
                hasGround = true;
                break;
            }
        }

        return hasGround ? 3 : 1; // Open air vs Cliff
    }

    /**
     * Helper to check if a world position is solid.
     */
    private boolean isSolidAt(float bx, float by, float bz) {
        if (world == null)
            return false;
        com.craftzero.world.BlockType bt = world.getBlockIfLoaded((int) Math.floor(bx), (int) Math.floor(by),
                (int) Math.floor(bz), com.craftzero.world.BlockType.BEDROCK);
        return bt != null && bt.isSolid();
    }

    /**
     * Check if moving in a direction would lead off a dangerous cliff (more than 3
     * blocks).
     */
    private boolean isLedgeInDirection(float testYaw) {
        float rad = (float) Math.toRadians(testYaw);
        float dx = (float) Math.sin(rad);
        float dz = -(float) Math.cos(rad);
        float ledgeDist = 0.8f;
        float lx = x + dx * ledgeDist;
        float lz = z + dz * ledgeDist;
        // Only treat as ledge if drop is more than 3 blocks
        return !hasGroundWithin(lx, y, lz, 3);
    }

    /**
     * Check if there's solid ground within maxFall blocks below the given position.
     * Used for safe drop detection (mobs can drop up to 3 blocks safely).
     * 
     * Position is at feet level. For a 1-block drop, ground is at y-1.
     * For a 3-block drop, ground is at y-3.
     */
    private boolean hasGroundWithin(float checkX, float checkY, float checkZ, int maxFall) {
        if (world == null)
            return false;

        // Check from y-0.5 down to y-maxFall to find any solid ground
        // Using 0.5 steps ensures we don't miss blocks at boundaries
        for (float dy = 0.5f; dy <= maxFall + 0.5f; dy += 1.0f) {
            if (isSolidAt(checkX, checkY - dy, checkZ)) {
                return true; // Found ground within safe fall distance
            }
        }
        return false; // No ground - dangerous cliff!
    }

    /**
     * Update Minecraft-style animation variables.
     * - Body SMOOTHLY rotates toward targetYaw (AI-set direction)
     * - Motion only happens when body is facing the right direction
     * - Head can look around independently with smooth transitions
     */
    protected void updateAnimation() {
        // === BODY ROTATION (Smooth turning toward target) ===
        float yawDiff = wrapDegrees(targetYaw - bodyYaw);

        // Limit rotation speed (smooth turning, not instant)
        float maxTurn = turnSpeed; // Degrees per tick (8 by default)
        float actualTurn = yawDiff;
        if (Math.abs(yawDiff) > maxTurn) {
            actualTurn = Math.signum(yawDiff) * maxTurn;
        }

        // Apply rotation (smooth)
        bodyYaw += actualTurn;
        bodyYaw = wrapDegrees(bodyYaw);

        // Sync entity yaw with body
        yaw = bodyYaw;

        // === FORWARD-ONLY MOVEMENT ===
        // Key principle: ONLY move forward when body is facing roughly the right
        // direction!
        // If not facing correctly, turn in place (no movement)
        float remainingYawDiff = Math.abs(wrapDegrees(targetYaw - bodyYaw));

        // Only move forward if:
        // 1. AI wants to move (forwardSpeed > 0)
        // 2. Body is facing within 45 degrees of target direction (more lenient)
        boolean isFacingCorrectly = remainingYawDiff < 45.0f;

        if (knockbackControlTicks > 0) {
            motionX *= 0.98f;
            motionZ *= 0.98f;
        } else if (forwardSpeed > 0.001f && isFacingCorrectly) {
            // Move forward in the direction body is facing
            float bodyYawRad = (float) Math.toRadians(bodyYaw);
            // MATCHING PLAYER.JAVA COORDINATE SYSTEM:
            // forward * sinYaw for X, -forward * cosYaw for Z
            // This makes -Z the forward direction (0 degrees)
            float modifiedMoveSpeed = moveSpeed * getMovementSpeedMultiplier() * fluidMovementScale();
            float targetMx = (float) Math.sin(bodyYawRad) * forwardSpeed * modifiedMoveSpeed;
            float targetMz = -(float) Math.cos(bodyYawRad) * forwardSpeed * modifiedMoveSpeed;

            // Interpolate velocity for "soft" movement (allows being pushed)
            float steeringResponse = fluidSteeringResponse();
            motionX += (targetMx - motionX) * steeringResponse;
            motionZ += (targetMz - motionZ) * steeringResponse;
        } else {
            // Not moving: slow down
            motionX *= 0.8f;
            motionZ *= 0.8f;
        }

        // === LIMB SWING (Leg Animation) ===
        float actualSpeed = (float) Math.sqrt(motionX * motionX + motionZ * motionZ);
        float targetSwingAmount = Math.min(actualSpeed * 10.0f, 1.0f);
        limbSwingAmount += (targetSwingAmount - limbSwingAmount) * 0.4f;

        if (limbSwingAmount > 0.01f) {
            limbSwing += actualSpeed * 6.0f;
        }

        // === HEAD LOOK BEHAVIOR ===
        updateHeadLook();

        // Smooth head rotation toward target
        float headYawDiff = wrapDegrees(targetHeadYaw - headYaw);
        headYaw += headYawDiff * 0.3f; // Smooth interpolation
        headYaw = Math.max(-70.0f, Math.min(70.0f, headYaw)); // Clamp to ±70°

        float headPitchDiff = targetHeadPitch - headPitch;
        headPitch += headPitchDiff * 0.3f;
        headPitch = Math.max(-40.0f, Math.min(40.0f, headPitch)); // Clamp

        // Final check: if very stuck, ensure forward animation stops
        if (isStuck()) {
            limbSwingAmount *= 0.5f;
        }
    }

    private float fluidMovementScale() {
        if (inWater) {
            return WATER_MOVEMENT_SCALE;
        }
        if (inLava) {
            return LAVA_MOVEMENT_SCALE;
        }
        return 1.0f;
    }

    private float fluidSteeringResponse() {
        if (inWater) {
            return WATER_STEERING_RESPONSE;
        }
        if (inLava) {
            return LAVA_STEERING_RESPONSE;
        }
        return LAND_STEERING_RESPONSE;
    }

    /**
     * Update head look behavior - look at player, random glances, idle behavior.
     */
    protected void updateHeadLook() {
        lookTimer--;

        if (hasLookTarget) {
            // Calculate angle to look target
            float dx = lookAtX - x;
            float dy = lookAtY - (y + getHeight() * 0.85f); // Eye level
            float dz = lookAtZ - z;
            float distXZ = (float) Math.sqrt(dx * dx + dz * dz);

            // Target angles relative to body (use -dz because -Z is forward)
            float targetAngle = (float) Math.toDegrees(Math.atan2(dx, -dz));
            targetHeadYaw = wrapDegrees(targetAngle - bodyYaw);
            targetHeadPitch = (float) Math.toDegrees(-Math.atan2(dy, distXZ));

            // Clear look target after timer expires
            if (lookTimer <= 0) {
                hasLookTarget = false;
                lookTimer = 40 + lookRandom.nextInt(60); // Reset timer
            }
        } else {
            // Random idle head movement
            if (lookTimer <= 0) {
                // Pick a new random look direction
                targetHeadYaw = (lookRandom.nextFloat() - 0.5f) * 60.0f; // ±30°
                targetHeadPitch = (lookRandom.nextFloat() - 0.5f) * 20.0f; // ±10°
                lookTimer = 80 + lookRandom.nextInt(100); // 4-9 seconds
            }
        }
    }

    /**
     * Make entity look at a position (called by AI).
     */
    public void lookAt(float x, float y, float z) {
        this.lookAtX = x;
        this.lookAtY = y;
        this.lookAtZ = z;
        this.hasLookTarget = true;
        this.lookTimer = 60; // Look for 3 seconds
    }

    /**
     * Set movement direction and speed (called by AI).
     * Body will smoothly rotate toward this direction.
     * 
     * @param yaw   Target facing direction
     * @param speed Forward speed (0-1, multiplied by moveSpeed)
     */
    public void setMoveDirection(float yaw, float speed) {
        this.targetYaw = yaw;
        this.forwardSpeed = speed;
    }

    /**
     * Stop moving (called by AI).
     */
    public void stopMoving() {
        this.forwardSpeed = 0;
    }

    /**
     * Wrap angle to -180 to 180 range.
     */
    protected float wrapDegrees(float angle) {
        while (angle > 180.0f)
            angle -= 360.0f;
        while (angle < -180.0f)
            angle += 360.0f;
        return angle;
    }

    @Override
    protected void onLanded(float fallDistance) {
        if (world != null) {
            world.trampleFarmlandBelow(getBoundingBox(), fallDistance);
        }
        if (isFallDamageImmune() || fallDistance <= 3.0f) {
            return;
        }
        int damage = (int) Math.ceil(fallDistance - 3.0f);
        if (world != null) {
            world.playFallSound(x, y, z, fallDistance);
        }
        damage(damage, DamageSource.point(DamageSource.Type.FALL, x, y, z, 0.0f, 0.0f));
    }

    protected boolean isFallDamageImmune() {
        return false;
    }

    protected boolean canBreatheUnderwater() {
        return hasEffect(StatusEffectType.WATER_BREATHING);
    }

    protected boolean isHeadUnderwater() {
        if (world == null) {
            return false;
        }
        int blockX = (int) Math.floor(x);
        int blockY = (int) Math.floor(y + getHeight() * 0.85f);
        int blockZ = (int) Math.floor(z);
        return world.getBlockIfLoaded(blockX, blockY, blockZ, BlockType.AIR).isWater();
    }

    private void updateAirSupply() {
        if (dead || canBreatheUnderwater()) {
            airTicks = MAX_AIR_TICKS;
            return;
        }
        if (isHeadUnderwater()) {
            airTicks--;
            if (airTicks <= DROWN_DAMAGE_AIR_TICKS) {
                airTicks = 0;
                spawnDrowningBubbles();
                damage(DROWN_DAMAGE, DamageSource.point(DamageSource.Type.DROWN, x, y, z, 0.0f, 0.0f));
            }
        } else {
            airTicks = MAX_AIR_TICKS;
        }
    }

    private void spawnDrowningBubbles() {
        if (world == null) {
            return;
        }
        for (int i = 0; i < DROWN_BUBBLE_PARTICLES; i++) {
            float particleX = x + lookRandom.nextFloat() - lookRandom.nextFloat();
            float particleY = y + lookRandom.nextFloat() - lookRandom.nextFloat();
            float particleZ = z + lookRandom.nextFloat() - lookRandom.nextFloat();
            world.spawnParticle(WorldParticle.Type.BUBBLE,
                    particleX, particleY, particleZ,
                    motionX, motionY, motionZ,
                    DROWN_BUBBLE_SCALE,
                    DROWN_BUBBLE_LIFETIME_TICKS);
        }
    }

    @Override
    protected boolean usesClimbablePhysics() {
        return true;
    }

    /**
     * Deal damage to this entity.
     * Implements Minecraft invulnerability frame logic:
     * - During invulnerability, only apply damage if it's higher than last damage
     * - Only the difference between new and old damage is applied
     * 
     * @param amount Damage amount
     * @param source Entity that caused the damage (can be null)
     * @return true if damage was dealt
     */
    public boolean damage(float amount, Entity source) {
        return damage(amount, source == null ? DamageSource.generic()
                : DamageSource.entity(DamageSource.Type.GENERIC, source,
                        CombatRules.ARROW_HORIZONTAL_KNOCKBACK,
                        CombatRules.ARROW_VERTICAL_KNOCKBACK));
    }

    public boolean damage(float amount, DamageSource source) {
        if (dead)
            return false;
        if (!Float.isFinite(amount) || amount <= 0.0f) {
            return false;
        }
        if (source == null) {
            source = DamageSource.generic();
        }
        if (source.type() == DamageSource.Type.FIRE && hasEffect(StatusEffectType.FIRE_RESISTANCE)) {
            return false;
        }
        amount = StatusEffectMath.applyResistanceReduction(amount, getEffectAmplifier(StatusEffectType.RESISTANCE));
        if (!Float.isFinite(amount) || amount <= 0.0f) {
            return false;
        }
        boolean playerCredit = source.playerCredit()
                || source.type() == DamageSource.Type.PLAYER_ATTACK
                || (source.type() == DamageSource.Type.ARROW
                        && source.entity() instanceof ArrowEntity arrow && arrow.isPlayerOwned())
                || (source.type() == DamageSource.Type.MOB_MELEE
                        && source.entity() instanceof Wolf wolf && wolf.isTamed());

        int hurtResistanceThreshold = source.usesHalfHurtResistanceWindow() ? maxInvulnerableTime / 2 : 0;
        boolean comparingAgainstRecentDamage = invulnerableTime > hurtResistanceThreshold;

        // Invulnerability frame logic (Minecraft style)
        if (comparingAgainstRecentDamage) {
            // During invulnerability, only deal damage if new damage is higher
            if (amount <= lastDamageAmount) {
                return false; // Reject weaker or equal damage
            }
            // Apply only the difference
            amount = amount - lastDamageAmount;
        }

        // Track this damage for future comparisons
        lastDamageAmount = comparingAgainstRecentDamage ? lastDamageAmount + amount : amount;

        if (playerCredit) {
            recentPlayerHitTicks = RECENT_PLAYER_HIT_TICKS;
            recentPlayerLootingLevel = source.type() == DamageSource.Type.PLAYER_ATTACK ? source.lootingLevel() : 0;
        } else {
            recentPlayerLootingLevel = 0;
        }

        health -= amount;
        hurtTime = hurtDuration;
        invulnerableTime = maxInvulnerableTime;
        rememberDamageSource(source);

        // Note: Knockback is now applied externally by Player.attackEntity()
        // The source-based knockback below is for mob attacks

        // Apply knockback from source (for mob attacks on player)
        if (source.hasPosition() && source.hasKnockback()) {
            float dx = x - source.sourceX();
            float dz = z - source.sourceZ();
            float dist = (float) Math.sqrt(dx * dx + dz * dz);

            if (Float.isFinite(dist) && dist > 0.01f) {
                float knockback = source.horizontalKnockback();
                float verticalKnockback = source.verticalKnockback();
                addMotion((dx / dist) * knockback, verticalKnockback, (dz / dist) * knockback);
            }
        }

        onHurt(amount, source.entity());
        return true;
    }

    protected void rememberDamageSource(DamageSource source) {
        lastDamageDetails = source;
        lastDamageSource = source == null ? null : source.entity();
        lastDamageSourceHasPosition = source != null && source.hasPosition();
        if (lastDamageSourceHasPosition) {
            lastDamageSourceX = source.sourceX();
            lastDamageSourceY = source.sourceY();
            lastDamageSourceZ = source.sourceZ();
        }
    }

    /**
     * Heal this entity.
     */
    public void heal(float amount) {
        if (!Float.isFinite(amount) || amount <= 0.0f) {
            return;
        }
        health = Math.min(health + amount, maxHealth);
    }

    /**
     * Set entity on fire.
     * 
     * @param ticks Duration in ticks
     */
    public void setOnFire(int ticks) {
        if (hasEffect(StatusEffectType.FIRE_RESISTANCE)) {
            extinguish();
            return;
        }
        if (fireTicks < ticks) {
            fireTicks = ticks;
            lastFireDamage = ticksExisted;
        }
    }

    public void setFireTicks(int ticks) {
        int clampedTicks = Math.max(0, ticks);
        if (clampedTicks > 0 && fireTicks <= 0) {
            lastFireDamage = ticksExisted;
        }
        fireTicks = clampedTicks;
    }

    /**
     * Extinguish fire.
     */
    public void extinguish() {
        fireTicks = 0;
    }

    protected boolean isWetFromWaterOrRain() {
        if (world == null) {
            return false;
        }
        int blockX = (int) Math.floor(x);
        int blockZ = (int) Math.floor(z);
        int feetY = (int) Math.floor(y + 0.1f);
        int headY = (int) Math.floor(y + getHeight() * 0.85f);
        return world.getBlockIfLoaded(blockX, feetY, blockZ, com.craftzero.world.BlockType.AIR).isWater()
                || world.getBlockIfLoaded(blockX, headY, blockZ, com.craftzero.world.BlockType.AIR).isWater()
                || world.isRainingAt(blockX, feetY, blockZ)
                || world.isRainingAt(blockX, headY, blockZ);
    }

    private void tickStatusEffects() {
        for (int i = activeEffects.size() - 1; i >= 0; i--) {
            StatusEffectInstance effect = activeEffects.get(i);
            applyStatusEffectTick(effect);
            StatusEffectInstance next = effect.ticked();
            if (next.expired()) {
                activeEffects.remove(i);
            } else {
                activeEffects.set(i, next);
            }
        }
        spawnStatusEffectParticle();
    }

    private void applyStatusEffectTick(StatusEffectInstance effect) {
        if (effect.type() == StatusEffectType.REGENERATION && isEffectReady(effect, 50)) {
            heal(1.0f);
        } else if (effect.type() == StatusEffectType.POISON && isEffectReady(effect, 25) && health > 1.0f) {
            health = Math.max(1.0f, health - 1.0f);
        }
    }

    private static boolean isEffectReady(StatusEffectInstance effect, int baseInterval) {
        int interval = effect.amplifier() >= 31 ? 0 : baseInterval >> effect.amplifier();
        return interval <= 0 || effect.durationTicks() % interval == 0;
    }

    private void spawnStatusEffectParticle() {
        if (world == null || activeEffects.isEmpty()
                || ticksExisted % STATUS_EFFECT_PARTICLE_INTERVAL_TICKS != 0) {
            return;
        }

        float particleX = x + (lookRandom.nextFloat() - 0.5f) * width;
        float particleY = y + 0.2f + lookRandom.nextFloat() * Math.max(0.1f, height - 0.2f);
        float particleZ = z + (lookRandom.nextFloat() - 0.5f) * width;
        world.spawnParticle(WorldParticle.Type.MOB_SPELL,
                particleX, particleY, particleZ,
                0.0f, 0.02f, 0.0f,
                STATUS_EFFECT_PARTICLE_SCALE,
                STATUS_EFFECT_PARTICLE_LIFETIME_TICKS,
                StatusEffectVisuals.mixedColor(activeEffects));
    }

    public void addEffect(StatusEffectInstance effect) {
        if (effect == null || effect.expired() || !isStatusEffectApplicable(effect)) {
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

    public List<StatusEffectInstance> getActiveEffects() {
        return Collections.unmodifiableList(activeEffects);
    }

    public void setActiveEffects(List<StatusEffectInstance> effects) {
        activeEffects.clear();
        if (effects != null) {
            for (StatusEffectInstance effect : effects) {
                if (effect != null && !effect.expired() && isStatusEffectApplicable(effect)) {
                    activeEffects.add(effect);
                }
            }
        }
    }

    protected boolean isStatusEffectApplicable(StatusEffectInstance effect) {
        return true;
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

    /**
     * Check if entity can attack (cooldown elapsed).
     */
    public boolean canAttack() {
        return attackCooldown <= 0;
    }

    public int getAttackCooldown() {
        return attackCooldown;
    }

    public void setAttackCooldown(int attackCooldown) {
        this.attackCooldown = Math.max(0, attackCooldown);
    }

    /**
     * Perform an attack (resets cooldown).
     */
    public void performAttack() {
        attackCooldown = maxAttackCooldown;
    }

    /**
     * Called when entity takes damage.
     */
    protected void onHurt(float amount, Entity source) {
        // Override in subclasses for sound effects, AI response, etc.
    }

    /**
     * Called when entity dies.
     */
    protected void onDeath() {
        spawnDeathSmokeParticles();
    }

    protected void spawnDeathSmokeParticles() {
        if (world != null) {
            world.spawnMobDeathParticles(x, y, z, width, height);
        }
    }

    public boolean hasRecentPlayerDamage() {
        return recentPlayerHitTicks > 0;
    }

    public int getRecentPlayerLootingLevel() {
        return recentPlayerLootingLevel;
    }

    public int getRecentPlayerHitTicks() {
        return recentPlayerHitTicks;
    }

    public int getInvulnerableTime() {
        return invulnerableTime;
    }

    public float getLastDamageAmount() {
        return lastDamageAmount;
    }

    public void restoreDamageState(int hurtTime, int invulnerableTime, float lastDamageAmount,
            int recentPlayerHitTicks, int recentPlayerLootingLevel) {
        this.hurtTime = clampTicks(hurtTime, hurtDuration);
        this.invulnerableTime = clampTicks(invulnerableTime, maxInvulnerableTime);
        this.lastDamageAmount = Float.isFinite(lastDamageAmount) ? Math.max(0.0f, lastDamageAmount) : 0.0f;
        this.recentPlayerHitTicks = clampTicks(recentPlayerHitTicks, RECENT_PLAYER_HIT_TICKS);
        this.recentPlayerLootingLevel = this.recentPlayerHitTicks > 0
                ? Math.max(0, recentPlayerLootingLevel)
                : 0;
    }

    public void restoreDeathAnimationState(boolean dead, int deathTime) {
        this.dead = dead;
        this.deathTime = dead ? clampTicks(deathTime, 20) : 0;
        if (dead && health > 0.0f) {
            health = 0.0f;
        }
    }

    private static int clampTicks(int ticks, int maxTicks) {
        return Math.max(0, Math.min(maxTicks, ticks));
    }

    /**
     * Get drops when this entity dies.
     * Override in mob classes.
     */
    public void dropLoot() {
        // Override in subclasses
    }

    /**
     * Move toward a target position.
     * Sets motion in blocks per second.
     * 
     * @param targetX Target X coordinate
     * @param targetZ Target Z coordinate
     * @param speed   Movement speed multiplier
     */
    protected void moveToward(float targetX, float targetZ, float speed) {
        float dx = targetX - x;
        float dz = targetZ - z;
        float dist = (float) Math.sqrt(dx * dx + dz * dz);

        if (dist > 0.1f) {
            // FIX: Don't allow AI to change direction/speed in air (preserves knockback)
            if (!onGround)
                return;

            // Calculate target yaw using atan2(dx, -dz) to match -Z forward convention
            float targetYaw = (float) Math.toDegrees(Math.atan2(dx, -dz));
            setMoveDirection(targetYaw, speed);
        } else {
            stopMoving();
        }
    }

    /**
     * Move forward based on current yaw.
     * Sets motion in blocks per second.
     */
    protected void moveForward(float speed) {
        // FIX: Don't allow AI to change direction/speed in air (preserves knockback)
        if (!onGround)
            return;

        float rad = (float) Math.toRadians(yaw);
        float modifiedMoveSpeed = moveSpeed * getMovementSpeedMultiplier();
        float moveX = -(float) Math.sin(rad) * speed * modifiedMoveSpeed;
        float moveZ = (float) Math.cos(rad) * speed * modifiedMoveSpeed;

        motionX = moveX;
        motionZ = moveZ;
    }

    /**
     * Add velocity to the entity (e.g. from knockback or pushing).
     */
    public void addMotion(float x, float y, float z) {
        float motionX = finiteMotion(x);
        float motionY = finiteMotion(y);
        float motionZ = finiteMotion(z);
        super.addMotion(motionX, motionY, motionZ);
        if (Math.abs(motionX) > 0.001f || Math.abs(motionZ) > 0.001f || motionY > 0.001f) {
            knockbackControlTicks = Math.max(knockbackControlTicks, 10);
        }
    }

    private static float finiteMotion(float value) {
        return Float.isFinite(value) ? value : 0.0f;
    }

    /**
     * Jump if on ground.
     */
    public void jump() {
        if (onGround) {
            motionY = jumpMotion();
        }
    }

    protected float jumpMotion() {
        int jumpBoost = getEffectAmplifier(StatusEffectType.JUMP_BOOST);
        return BASE_JUMP_MOTION + (jumpBoost >= 0 ? JUMP_BOOST_MOTION_PER_LEVEL * (jumpBoost + 1) : 0.0f);
    }

    /**
     * Set jump flag (for AI - will jump on next tick if on ground).
     */
    public void setJumping(boolean jumping) {
        this.jumping = jumping;
    }

    // Getters
    public float getHealth() {
        return health;
    }

    public float getMaxHealth() {
        return maxHealth;
    }

    public boolean isDead() {
        return dead;
    }

    public int getHurtTime() {
        return hurtTime;
    }

    public int getDeathTime() {
        return deathTime;
    }

    public boolean isOnFire() {
        return fireTicks > 0;
    }

    public int getFireTicks() {
        return fireTicks;
    }

    public int getAirTicks() {
        return airTicks;
    }

    public void setAirTicks(int airTicks) {
        this.airTicks = Math.max(DROWN_DAMAGE_AIR_TICKS, Math.min(MAX_AIR_TICKS, airTicks));
    }

    public Entity getLastDamageSource() {
        return lastDamageSource;
    }

    public DamageSource getLastDamageDetails() {
        return lastDamageDetails;
    }

    public boolean hasLastDamagePosition() {
        return lastDamageSourceHasPosition;
    }

    public float getLastDamageSourceX() {
        return lastDamageSourceX;
    }

    public float getLastDamageSourceY() {
        return lastDamageSourceY;
    }

    public float getLastDamageSourceZ() {
        return lastDamageSourceZ;
    }

    public float getMoveSpeed() {
        return moveSpeed;
    }

    /**
     * Returns true if this mob is in a trapped state (completely stuck).
     * AI goals should check this and stop requesting movement.
     */
    public boolean isTrapped() {
        return isTrapped;
    }

    /**
     * Returns true if this mob is actively escaping (committed to escape
     * direction).
     * AI goals should check this and not interfere with movement.
     */
    public boolean isEscaping() {
        return escapingTicks > 0;
    }

    /**
     * Clears the trapped state. Called when AI picks a new viable target.
     */
    public void clearTrapped() {
        isTrapped = false;
        stuckOnLedge = false;
        continuousStuckTicks = 0;
        avoidanceCooldown = 0;
    }

    /**
     * Returns true if mob is stuck on a ledge with no safe forward path.
     * AI goals should check this and immediately pick a new target.
     */
    public boolean isStuckOnLedge() {
        return stuckOnLedge;
    }

    // Setters
    public void setHealth(float health) {
        this.health = Math.min(health, maxHealth);
    }

    public void setMoveSpeed(float speed) {
        this.moveSpeed = speed;
    }

    // === ANIMATION GETTERS ===
    public float getLimbSwing() {
        return limbSwing;
    }

    public float getLimbSwingAmount() {
        return limbSwingAmount;
    }

    public float getPrevLimbSwingAmount() {
        return prevLimbSwingAmount;
    }

    public float getBodyYaw() {
        return bodyYaw;
    }

    public float getPrevBodyYaw() {
        return prevBodyYaw;
    }

    public void setRenderBodyYaw(float bodyYaw) {
        float wrapped = wrapDegrees(bodyYaw);
        this.bodyYaw = wrapped;
        this.prevBodyYaw = wrapped;
        this.targetYaw = wrapped;
        this.yaw = wrapped;
        this.prevYaw = wrapped;
    }

    public float getHeadYaw() {
        return headYaw;
    }

    public float getHeadPitch() {
        return headPitch;
    }

    /**
     * Returns true if the entity has been blocked by an obstacle for a while.
     */
    public boolean isStuck() {
        return continuousStuckTicks > 10;
    }
}
