package com.craftzero.entity.ai;

import com.craftzero.entity.LivingEntity;
import com.craftzero.main.Player;

/**
 * AI Goal: Move toward and attack the current target.
 * Includes cliff-aware pathfinding to avoid walking off edges.
 */
public class MeleeAttackGoal implements Goal {

    private final LivingEntity mob;
    private final MobAI ai;
    private final float damage;
    private final float attackRange;
    private final float chaseSpeed;

    private int pathRecalcCooldown;
    private int stuckTicks; // Track if mob is stuck
    private float lastX, lastZ; // Last position to detect stuck
    private static final int PATH_RECALC_INTERVAL = 20; // 1 second
    private static final int STUCK_THRESHOLD = 40; // 2 seconds without progress

    public MeleeAttackGoal(LivingEntity mob, MobAI ai, float damage, float attackRange, float chaseSpeed) {
        this.mob = mob;
        this.ai = ai;
        this.damage = damage;
        this.attackRange = attackRange;
        this.chaseSpeed = chaseSpeed;
        this.pathRecalcCooldown = 0;
        this.stuckTicks = 0;
    }

    public MeleeAttackGoal(LivingEntity mob, MobAI ai, float damage) {
        this(mob, ai, damage, 1.5f, 1.0f);
    }

    @Override
    public int getPriority() {
        return 3; // High priority when we have a target
    }

    @Override
    public boolean canUse() {
        return ai.hasMoveTarget(); // Has a player target to chase
    }

    @Override
    public boolean canContinue() {
        return ai.hasMoveTarget();
    }

    @Override
    public void start() {
        pathRecalcCooldown = 0;
        stuckTicks = 0;
        lastX = mob.getX();
        lastZ = mob.getZ();
    }

    @Override
    public void tick() {
        if (mob.getWorld() == null)
            return;

        Player player = mob.getWorld().getPlayer();
        if (player == null)
            return;

        float playerX = player.getPosition().x;
        float playerY = player.getPosition().y;
        float playerZ = player.getPosition().z;

        // Look at player
        mob.lookAt(playerX, playerY + 1.6f, playerZ);

        // Calculate distance
        float dx = playerX - mob.getX();
        float dy = playerY - mob.getY();
        float dz = playerZ - mob.getZ();
        float dist = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);

        // Update movement target periodically
        pathRecalcCooldown--;
        if (pathRecalcCooldown <= 0) {
            ai.setMoveTarget(playerX, playerZ);
            pathRecalcCooldown = PATH_RECALC_INTERVAL;
        }

        // Check if stuck
        float moveDist = (float) Math.sqrt(
                (mob.getX() - lastX) * (mob.getX() - lastX) +
                        (mob.getZ() - lastZ) * (mob.getZ() - lastZ));
        if (moveDist < 0.05f) {
            stuckTicks++;
        } else {
            stuckTicks = 0;
            lastX = mob.getX();
            lastZ = mob.getZ();
        }

        // Attack if in range
        if (dist <= attackRange && mob.canAttack()) {
            performAttack(player);
        } else if (stuckTicks < STUCK_THRESHOLD) {
            // Move toward player (with cliff awareness)
            moveTowardTarget();
        } else {
            // Stuck - try to find alternate path
            tryAlternatePath();
        }
    }

    private void moveTowardTarget() {
        float targetX = ai.getTargetX();
        float targetZ = ai.getTargetZ();

        float dx = targetX - mob.getX();
        float dz = targetZ - mob.getZ();
        float dist = (float) Math.sqrt(dx * dx + dz * dz);

        if (dist > 0.5f) {
            // Calculate target yaw
            float targetYaw = (float) Math.toDegrees(Math.atan2(dx, -dz));

            // Check if direct path leads to cliff
            if (LineOfSightUtil.isCliffAhead(mob.getWorld(),
                    mob.getX(), mob.getY(), mob.getZ(), targetYaw, 1.5f)) {
                // Find safe direction
                float safeYaw = LineOfSightUtil.findSafeDirection(
                        mob.getWorld(), mob.getX(), mob.getY(), mob.getZ(), targetYaw);

                if (safeYaw != targetYaw) {
                    // Use safe direction instead
                    mob.setMoveDirection(safeYaw, chaseSpeed * 0.7f); // Slower when avoiding
                    return;
                } else {
                    // No safe direction - stop to avoid falling
                    mob.stopMoving();
                    return;
                }
            }

            mob.setMoveDirection(targetYaw, chaseSpeed);
        } else {
            mob.stopMoving();
        }
    }

    private void tryAlternatePath() {
        // When stuck, try moving in a random valid direction
        float currentYaw = mob.getBodyYaw();
        float[] testAngles = { 90, -90, 45, -45, 135, -135, 180 };

        for (float offset : testAngles) {
            float testYaw = currentYaw + offset;
            if (!LineOfSightUtil.isCliffAhead(mob.getWorld(),
                    mob.getX(), mob.getY(), mob.getZ(), testYaw, 2.0f)) {
                mob.setMoveDirection(testYaw, chaseSpeed * 0.5f);
                stuckTicks = 0; // Reset stuck counter
                return;
            }
        }

        // Completely stuck (surrounded by cliffs) - just stop
        mob.stopMoving();
    }

    private void performAttack(Player player) {
        mob.performAttack();

        // Deal damage to player
        player.getStats().damage(damage);

        // Apply knockback to player
        float dx = player.getPosition().x - mob.getX();
        float dz = player.getPosition().z - mob.getZ();
        float dist = (float) Math.sqrt(dx * dx + dz * dz);

        if (dist > 0.01f) {
            float knockback = 0.4f;
            player.getVelocity().x += (dx / dist) * knockback;
            player.getVelocity().y += 0.3f;
            player.getVelocity().z += (dz / dist) * knockback;
        }
    }

    @Override
    public void stop() {
        mob.stopMoving();
        stuckTicks = 0;
    }
}
