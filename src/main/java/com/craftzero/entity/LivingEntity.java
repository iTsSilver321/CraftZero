package com.craftzero.entity;

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

    // Health
    protected float health;
    protected float maxHealth;

    // Damage state
    protected int hurtTime; // Ticks since last damage (for animation)
    protected int hurtDuration = 10; // Duration of hurt animation
    protected int invulnerableTime; // Ticks of invulnerability after damage
    protected int maxInvulnerableTime = 20; // 1 second of invulnerability
    protected Entity lastDamageSource;

    // Death state
    protected int deathTime; // Ticks since death (for death animation)
    protected boolean dead;

    // Attack
    protected int attackCooldown;
    protected int maxAttackCooldown = 20; // 1 second between attacks

    // Fire
    protected int fireTicks; // Ticks remaining on fire
    protected int lastFireDamage; // Tick counter for fire damage

    // Movement AI
    // moveSpeed is in BLOCKS PER TICK (Minecraft-style per-tick physics)
    protected float moveSpeed = 0.1f; // ~0.1 blocks/tick is typical mob walking speed
    protected boolean jumping;
    protected int jumpCooldown; // Cooldown to prevent infinite jumping
    protected int avoidanceCooldown; // Cooldown to prevent avoidance spam
    protected int continuousStuckTicks; // Track how long we are blocked by something tall
    protected boolean isTrapped; // TRUE = mob is completely stuck and waiting for escape
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

        // Fire damage
        if (fireTicks > 0) {
            fireTicks--;
            // Deal fire damage every 20 ticks (1 second)
            if (ticksExisted - lastFireDamage >= 20) {
                damage(1.0f, null);
                lastFireDamage = ticksExisted;
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

        // === ESCAPING STATE: Navigate toward escape block center ===
        if (escapingTicks > 0) {
            escapingTicks--;

            // Calculate direction to escape target
            float dx = escapeTargetX - x;
            float dz = escapeTargetZ - z;
            float distToTarget = (float) Math.sqrt(dx * dx + dz * dz);

            if (distToTarget < 0.3f) {
                // Close enough to target - escape complete
                escapingTicks = 0;
            } else {
                // Navigate toward target center
                targetYaw = (float) Math.toDegrees(Math.atan2(dx, -dz));
                float yawDiff = wrapDegrees(targetYaw - bodyYaw);
                bodyYaw += yawDiff * 0.3f;
                forwardSpeed = 1.0f;
            }
        }
        // === TRAPPED STATE: Complete freeze + periodic escape scan ===
        else if (isTrapped) {
            // Force stop all movement while trapped
            forwardSpeed = 0;

            // Periodic 360° escape scan (VERY FAST - every 5 ticks)
            escapeScanTimer--;
            if (escapeScanTimer <= 0) {
                escapeScanTimer = 5; // Scan every 5 ticks (1/4 second)

                // Fine 360° scan with 15° increments and multi-distance
                float bestYaw = Float.MAX_VALUE;
                int bestScore = -1;
                float bestDist = 0;

                for (float offset = 0; offset < 360; offset += 15) {
                    float testYaw = wrapDegrees(bodyYaw + offset);
                    for (float testDist : new float[] { 0.5f, 1.0f, 1.5f }) {
                        int score = evaluatePath(testYaw, testDist);
                        if (score > bestScore && score >= 2) {
                            bestScore = score;
                            bestYaw = testYaw;
                            bestDist = testDist;
                        }
                    }
                }

                if (bestYaw != Float.MAX_VALUE) {
                    // FOUND AN ESCAPE! Calculate block center
                    float rad = (float) Math.toRadians(bestYaw);
                    float targetBlockX = x + (float) Math.sin(rad) * bestDist;
                    float targetBlockZ = z - (float) Math.cos(rad) * bestDist;

                    // Snap to block center
                    escapeTargetX = (float) Math.floor(targetBlockX) + 0.5f;
                    escapeTargetZ = (float) Math.floor(targetBlockZ) + 0.5f;

                    targetYaw = bestYaw;
                    bodyYaw = bestYaw;
                    isTrapped = false;
                    continuousStuckTicks = 0;
                    escapingTicks = 40; // COMMIT for 2 seconds
                }
            }
        }
        // === NORMAL MOVEMENT: Jump, Veer, or Trap ===
        else if (onGround && wantsToMove) {
            float yawRad = (float) Math.toRadians(bodyYaw);
            float dx = (float) Math.sin(yawRad);
            float dz = -(float) Math.cos(yawRad);

            // LEDGE CHECK: Don't walk off 1+ block drops
            float ledgeDist = 0.8f;
            float lx = x + dx * ledgeDist;
            float lz = z + dz * ledgeDist;
            boolean isLedgeAhead = !isSolidAt(lx, y - 1.0f, lz); // No ground 1 block below

            if (isLedgeAhead) {
                // Stop before falling! Find a different path.
                float escapeYaw = findEscapeRoute();
                if (escapeYaw != Float.MAX_VALUE) {
                    targetYaw = escapeYaw;
                    bodyYaw += wrapDegrees(targetYaw - bodyYaw) * 0.5f;
                } else {
                    forwardSpeed = 0; // Just stop
                }
                avoidanceCooldown = 20;
            }
            // 1. IMMEDIATE JUMP CHECK (Don't Think, Just Jump)
            else {
                float checkDist = 0.7f;
                float cx = x + dx * checkDist;
                float cz = z + dz * checkDist;

                if (canJumpAtLocation(cx, cz) && jumpCooldown <= 0) {
                    motionY = 0.42f;
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
                motionY = 0.42f;
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
        com.craftzero.world.BlockType bt = world.getBlock((int) Math.floor(bx), (int) Math.floor(by),
                (int) Math.floor(bz));
        return bt != null && bt.isSolid();
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

        if (forwardSpeed > 0.001f && isFacingCorrectly) {
            // Move forward in the direction body is facing
            float bodyYawRad = (float) Math.toRadians(bodyYaw);
            // MATCHING PLAYER.JAVA COORDINATE SYSTEM:
            // forward * sinYaw for X, -forward * cosYaw for Z
            // This makes -Z the forward direction (0 degrees)
            float targetMx = (float) Math.sin(bodyYawRad) * forwardSpeed * moveSpeed;
            float targetMz = -(float) Math.cos(bodyYawRad) * forwardSpeed * moveSpeed;

            // Interpolate velocity for "soft" movement (allows being pushed)
            motionX += (targetMx - motionX) * 0.2f;
            motionZ += (targetMz - motionZ) * 0.2f;
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

    /**
     * Update head look behavior - look at player, random glances, idle behavior.
     */
    protected void updateHeadLook() {
        lookTimer--;

        if (hasLookTarget) {
            // Calculate angle to look target
            float dx = lookAtX - x;
            float dy = lookAtY - (y + height * 0.85f); // Eye level
            float dz = lookAtZ - z;
            float distXZ = (float) Math.sqrt(dx * dx + dz * dz);

            // Target angles relative to body
            float targetAngle = (float) Math.toDegrees(Math.atan2(dx, dz));
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

    /**
     * Deal damage to this entity.
     * 
     * @param amount Damage amount
     * @param source Entity that caused the damage (can be null)
     * @return true if damage was dealt
     */
    public boolean damage(float amount, Entity source) {
        if (dead)
            return false;
        if (invulnerableTime > 0)
            return false;

        health -= amount;
        hurtTime = hurtDuration;
        invulnerableTime = maxInvulnerableTime;
        lastDamageSource = source;

        // Apply knockback from source
        if (source != null) {
            float dx = x - source.getX();
            float dz = z - source.getZ();
            float dist = (float) Math.sqrt(dx * dx + dz * dz);

            if (dist > 0.01f) {
                float knockback = 0.4f;
                motionX += (dx / dist) * knockback;
                motionY += 0.3f; // Slight upward knockback
                motionZ += (dz / dist) * knockback;
            }
        }

        onHurt(amount, source);
        return true;
    }

    /**
     * Heal this entity.
     */
    public void heal(float amount) {
        health = Math.min(health + amount, maxHealth);
    }

    /**
     * Set entity on fire.
     * 
     * @param ticks Duration in ticks
     */
    public void setOnFire(int ticks) {
        if (fireTicks < ticks) {
            fireTicks = ticks;
            lastFireDamage = ticksExisted;
        }
    }

    /**
     * Extinguish fire.
     */
    public void extinguish() {
        fireTicks = 0;
    }

    /**
     * Check if entity can attack (cooldown elapsed).
     */
    public boolean canAttack() {
        return attackCooldown <= 0;
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
        // Override in subclasses for drops, effects, etc.
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
        float rad = (float) Math.toRadians(yaw);
        float moveX = -(float) Math.sin(rad) * speed * moveSpeed;
        float moveZ = (float) Math.cos(rad) * speed * moveSpeed;

        motionX = moveX;
        motionZ = moveZ;
    }

    /**
     * Add velocity to the entity (e.g. from knockback or pushing).
     */
    public void addMotion(float x, float y, float z) {
        this.motionX += x;
        this.motionY += y;
        this.motionZ += z;
    }

    /**
     * Jump if on ground.
     */
    public void jump() {
        if (onGround) {
            motionY = 0.42f;
        }
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

    public Entity getLastDamageSource() {
        return lastDamageSource;
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
        continuousStuckTicks = 0;
        avoidanceCooldown = 0;
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
