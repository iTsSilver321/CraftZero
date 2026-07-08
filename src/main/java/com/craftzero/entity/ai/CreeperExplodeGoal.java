package com.craftzero.entity.ai;

import com.craftzero.entity.LivingEntity;
import com.craftzero.main.Player;
import com.craftzero.entity.mob.Creeper;
import com.craftzero.world.World;

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
        return ai.hasMoveTarget() || ai.hasTarget() || ai.hasRemotePlayerTarget();
    }

    @Override
    public boolean canContinue() {
        return ai.hasMoveTarget() || ai.getTarget() != null || ai.hasRemotePlayerTarget() || isCreeperIgnited();
    }

    @Override
    public void start() {
    }

    @Override
    public void tick() {
        if (mob.getWorld() == null)
            return;

        LivingEntity livingTarget = ai.getTarget();
        if (livingTarget != null) {
            if (!isValidLivingTarget(livingTarget)) {
                ai.clearTarget();
                ai.clearMoveTarget();
                ai.requestStopMoving();
                coolFuse();
                return;
            }
            tickLivingTarget(livingTarget);
            return;
        }

        World.RemotePlayerTarget remoteTarget = ai.getRemotePlayerTarget();
        if (remoteTarget != null) {
            tickRemoteTarget(remoteTarget);
            return;
        }

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

            ai.setMoveTarget(targetX, targetY, targetZ);
            ai.requestMoveToward(targetX, targetZ, chaseSpeed, explosionRange * 0.5f, 1.5f);
        }
    }

    private void tickLivingTarget(LivingEntity target) {
        float targetX = target.getX();
        float targetY = target.getY();
        float targetZ = target.getZ();

        float dx = targetX - mob.getX();
        float dy = targetY - mob.getY();
        float dz = targetZ - mob.getZ();
        float dist = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        boolean hasSight = hasLineOfSight(target);

        mob.lookAt(targetX, targetY + target.getHeight() * 0.85f, targetZ);

        if (dist <= explosionRange && hasSight) {
            ai.requestStopMoving();
            int fuseTime = advanceFuse();
            if (fuseTime >= maxFuseTime) {
                explode();
            }
        } else {
            coolFuse();
            ai.setMoveTarget(targetX, targetY, targetZ);
            ai.requestMoveToward(targetX, targetZ, chaseSpeed, explosionRange * 0.5f, 1.5f);
        }
    }

    private void tickRemoteTarget(World.RemotePlayerTarget target) {
        if (target == null || !target.valid()) {
            ai.clearRemotePlayerTarget();
            ai.clearMoveTarget();
            coolFuse();
            return;
        }

        float targetX = target.x();
        float targetY = target.y();
        float targetZ = target.z();

        float dx = targetX - mob.getX();
        float dy = targetY - mob.getY();
        float dz = targetZ - mob.getZ();
        float dist = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        boolean hasSight = hasLineOfSight(target);

        mob.lookAt(targetX, target.eyeY(), targetZ);

        if (dist <= explosionRange && hasSight) {
            ai.requestStopMoving();
            int fuseTime = advanceFuse();
            if (fuseTime >= maxFuseTime) {
                explode();
            }
        } else {
            coolFuse();
            ai.setMoveTarget(targetX, targetY, targetZ);
            ai.requestMoveToward(targetX, targetZ, chaseSpeed, explosionRange * 0.5f, 1.5f);
        }
    }

    private boolean hasLineOfSight(Player player) {
        return LineOfSightUtil.hasLineOfSight(
                mob.getWorld(),
                mob.getX(), mob.getY() + mob.getHeight() * 0.85f, mob.getZ(),
                player.getPosition().x, player.getPosition().y + 1.6f, player.getPosition().z);
    }

    private boolean hasLineOfSight(LivingEntity target) {
        return LineOfSightUtil.hasLineOfSight(
                mob.getWorld(),
                mob.getX(), mob.getY() + mob.getHeight() * 0.85f, mob.getZ(),
                target.getX(), target.getY() + target.getHeight() * 0.85f, target.getZ());
    }

    private boolean hasLineOfSight(World.RemotePlayerTarget target) {
        return target != null
                && LineOfSightUtil.hasLineOfSight(
                        mob.getWorld(),
                        mob.getX(), mob.getY() + mob.getHeight() * 0.85f, mob.getZ(),
                        target.x(), target.eyeY(), target.z());
    }

    private boolean isValidLivingTarget(LivingEntity target) {
        return target != null
                && target != mob
                && !target.isDead()
                && !target.isRemoved();
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
