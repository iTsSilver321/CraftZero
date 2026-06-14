package com.craftzero.entity.ai;

import com.craftzero.entity.LivingEntity;
import com.craftzero.main.Player;
import com.craftzero.entity.mob.Creeper;

/**
 * AI Goal: Creeper-specific behavior - approach and explode.
 * When close to player, start fuse countdown and explode if not interrupted.
 */
public class CreeperExplodeGoal implements Goal {

    private final LivingEntity mob;
    private final MobAI ai;
    private final float explosionRange;
    private final float chaseSpeed;

    private int maxFuseTime;

    public CreeperExplodeGoal(LivingEntity mob, MobAI ai, float explosionRange, int fuseTime) {
        this.mob = mob;
        this.ai = ai;
        this.explosionRange = explosionRange;
        this.maxFuseTime = fuseTime;
        this.chaseSpeed = 1.0f;
    }

    @Override
    public int getPriority() {
        return 2; // High priority when activated
    }

    @Override
    public boolean canUse() {
        return ai.hasMoveTarget(); // Has a player target
    }

    @Override
    public boolean canContinue() {
        return ai.hasMoveTarget() || isCreeperIgnited();
    }

    @Override
    public void start() {
    }

    @Override
    public void tick() {
        if (mob.getWorld() == null)
            return;

        Player player = mob.getWorld().getPlayer();
        if (player == null || player.isCreative() || !player.getDifficulty().allowsHostileSpawns()) {
            coolFuse();
            ai.clearMoveTarget();
            return;
        }

        float targetX = player.getPosition().x;
        float targetY = player.getPosition().y;
        float targetZ = player.getPosition().z;

        // Calculate distance
        float dx = targetX - mob.getX();
        float dy = targetY - mob.getY();
        float dz = targetZ - mob.getZ();
        float dist = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        boolean hasSight = hasLineOfSight(player);

        // Look at player
        mob.lookAt(targetX, targetY + 1.6f, targetZ);

        if (dist <= explosionRange && hasSight) {
            // In range - start/continue fuse
            // Stop moving when fusing
            ai.requestStopMoving();

            // Check for explosion
            int fuseTime = advanceFuse();
            if (fuseTime >= maxFuseTime) {
                explode();
            }

        } else {
            coolFuse();

            // Chase player
            float targetYaw = (float) Math.toDegrees(Math.atan2(dx, -dz));
            ai.requestMoveDirection(targetYaw, chaseSpeed);
        }
    }

    private boolean hasLineOfSight(Player player) {
        return LineOfSightUtil.hasLineOfSight(
                mob.getWorld(),
                mob.getX(), mob.getY() + mob.getHeight() * 0.85f, mob.getZ(),
                player.getPosition().x, player.getPosition().y + 1.6f, player.getPosition().z);
    }

    /**
     * Execute the explosion.
     */
    private void explode() {
        if (mob instanceof Creeper creeper) {
            creeper.explode();
        } else {
            mob.remove();
        }
    }

    private int advanceFuse() {
        if (mob instanceof Creeper creeper) {
            return creeper.advanceFuse();
        }
        return maxFuseTime;
    }

    private void coolFuse() {
        if (mob instanceof Creeper creeper) {
            creeper.coolFuse();
        }
    }

    private boolean isCreeperIgnited() {
        return mob instanceof Creeper creeper && creeper.isIgnited();
    }

    @Override
    public void stop() {
        if (!ai.hasMoveTarget() && mob instanceof Creeper creeper) {
            creeper.resetFuse();
        }
    }

    @Override
    public boolean isExclusive() {
        return true;
    }
}
