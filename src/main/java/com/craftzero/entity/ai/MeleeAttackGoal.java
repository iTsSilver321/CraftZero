package com.craftzero.entity.ai;

import com.craftzero.entity.LivingEntity;
import com.craftzero.main.Player;

/**
 * AI Goal: Move toward and attack the current target.
 */
public class MeleeAttackGoal implements Goal {

    private final LivingEntity mob;
    private final MobAI ai;
    private final float damage;
    private final float attackRange;
    private final float chaseSpeed;

    private int pathRecalcCooldown;
    private static final int PATH_RECALC_INTERVAL = 20; // 1 second

    public MeleeAttackGoal(LivingEntity mob, MobAI ai, float damage, float attackRange, float chaseSpeed) {
        this.mob = mob;
        this.ai = ai;
        this.damage = damage;
        this.attackRange = attackRange;
        this.chaseSpeed = chaseSpeed;
        this.pathRecalcCooldown = 0;
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

        // Attack if in range
        if (dist <= attackRange && mob.canAttack()) {
            performAttack(player);
        } else {
            // Move toward player
            moveTowardTarget();
        }
    }

    private void moveTowardTarget() {
        float targetX = ai.getTargetX();
        float targetZ = ai.getTargetZ();

        float dx = targetX - mob.getX();
        float dz = targetZ - mob.getZ();
        float dist = (float) Math.sqrt(dx * dx + dz * dz);

        if (dist > 0.5f) {
            // Calculate target yaw and use new movement system
            float targetYaw = (float) Math.toDegrees(Math.atan2(dx, -dz));
            mob.setMoveDirection(targetYaw, chaseSpeed);
        } else {
            mob.stopMoving();
        }
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
    }
}
