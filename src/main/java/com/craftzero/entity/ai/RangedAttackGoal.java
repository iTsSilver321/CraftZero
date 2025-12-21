package com.craftzero.entity.ai;

import com.craftzero.entity.LivingEntity;
import com.craftzero.main.Player;

/**
 * AI Goal: Attack from range with projectiles (arrows, fireballs, etc.)
 * Used by Skeletons to shoot arrows at players.
 */
public class RangedAttackGoal implements Goal {

    private final LivingEntity mob;
    private final MobAI ai;
    private final float attackRange;
    private final float minRange; // Don't shoot if too close
    private final int attackInterval; // Ticks between attacks
    private final float projectileSpeed;

    private int attackCooldown;
    private int strafeTime;
    private boolean strafingClockwise;
    private float strafeSpeed;

    public RangedAttackGoal(LivingEntity mob, MobAI ai, float attackRange, int attackInterval) {
        this.mob = mob;
        this.ai = ai;
        this.attackRange = attackRange;
        this.minRange = 4.0f; // Don't shoot if closer than 4 blocks
        this.attackInterval = attackInterval;
        this.projectileSpeed = 1.5f;
        this.attackCooldown = 0;
        this.strafeTime = 0;
    }

    @Override
    public int getPriority() {
        return 3; // Same as melee attack - they're alternatives
    }

    @Override
    public boolean canUse() {
        return ai.hasMoveTarget(); // Has a player target
    }

    @Override
    public boolean canContinue() {
        return ai.hasMoveTarget();
    }

    @Override
    public void start() {
        attackCooldown = attackInterval / 2; // Start with half cooldown
        strafeTime = 0;
        strafingClockwise = Math.random() > 0.5;
        strafeSpeed = 0.5f;
    }

    @Override
    public void tick() {
        if (mob.getWorld() == null)
            return;

        Player player = mob.getWorld().getPlayer();
        if (player == null)
            return;

        float targetX = player.getPosition().x;
        float targetY = player.getPosition().y;
        float targetZ = player.getPosition().z;

        // Calculate distance
        float dx = targetX - mob.getX();
        float dy = targetY - mob.getY();
        float dz = targetZ - mob.getZ();
        float dist = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);

        // Look at player
        mob.lookAt(targetX, targetY + 1.6f, targetZ);

        // Movement behavior
        if (dist <= attackRange && dist >= minRange) {
            // In attack range - strafe instead of approaching
            strafeTime++;

            // Change strafe direction occasionally
            if (strafeTime >= 20) {
                if (Math.random() < 0.3) {
                    strafingClockwise = !strafingClockwise;
                    strafeTime = 0;
                }
            }

            // Calculate strafe direction (perpendicular to target)
            float targetYaw = (float) Math.toDegrees(Math.atan2(dx, -dz));
            float strafeYaw = targetYaw + (strafingClockwise ? 90 : -90);

            mob.setMoveDirection(strafeYaw, strafeSpeed);

        } else if (dist > attackRange) {
            // Too far - move closer
            float targetYaw = (float) Math.toDegrees(Math.atan2(dx, -dz));
            mob.setMoveDirection(targetYaw, 0.8f);

        } else if (dist < minRange) {
            // Too close - back away
            float awayYaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
            mob.setMoveDirection(awayYaw, 0.6f);
        }

        // Attack logic
        attackCooldown--;
        if (attackCooldown <= 0 && dist <= attackRange && dist >= minRange) {
            // Fire projectile!
            shootArrow(player);
            attackCooldown = attackInterval;
        }
    }

    /**
     * Shoot an arrow at the target.
     */
    private void shootArrow(Player target) {
        if (mob.getWorld() == null)
            return;

        // Calculate spawn position (from mob's "hand" level)
        float spawnX = mob.getX();
        float spawnY = mob.getY() + mob.getHeight() * 0.75f;
        float spawnZ = mob.getZ();

        // Calculate direction to target (with slight prediction)
        float targetX = target.getPosition().x;
        float targetY = target.getPosition().y + 1.0f; // Aim at chest
        float targetZ = target.getPosition().z;

        float dx = targetX - spawnX;
        float dy = targetY - spawnY;
        float dz = targetZ - spawnZ;
        float dist = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);

        // Normalize and apply speed
        dx = (dx / dist) * projectileSpeed;
        dy = (dy / dist) * projectileSpeed + 0.1f; // Add slight arc
        dz = (dz / dist) * projectileSpeed;

        // TODO: Spawn arrow projectile when Arrow entity is implemented
        // mob.getWorld().spawnArrow(spawnX, spawnY, spawnZ, dx, dy, dz, mob);

        // Visual feedback - attack animation
        mob.performAttack();
    }

    @Override
    public void stop() {
        mob.stopMoving();
        attackCooldown = 0;
    }

    @Override
    public boolean isExclusive() {
        return true; // Don't melee while shooting
    }
}
