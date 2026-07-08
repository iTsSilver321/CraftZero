package com.craftzero.entity.ai;

import com.craftzero.entity.Entity;
import com.craftzero.entity.LivingEntity;
import com.craftzero.entity.mob.Mob;
import com.craftzero.world.World;

/**
 * Moves a passive mob away from nearby threatening living entities.
 */
public class AvoidEntityGoal implements Goal {

    private static final float MIN_DISTANCE_SQ = 0.0001f;

    private final Mob mob;
    private final MobAI ai;
    private final Class<? extends LivingEntity> avoidType;
    private final float startDistance;
    private final float stopDistance;
    private final float speed;

    private LivingEntity threat;

    public AvoidEntityGoal(Mob mob, MobAI ai, Class<? extends LivingEntity> avoidType,
            float startDistance, float stopDistance, float speed) {
        this.mob = mob;
        this.ai = ai;
        this.avoidType = avoidType;
        this.startDistance = Math.max(0.0f, startDistance);
        this.stopDistance = Math.max(this.startDistance, stopDistance);
        this.speed = Math.max(0.0f, speed);
    }

    @Override
    public int getPriority() {
        return 2;
    }

    @Override
    public boolean canUse() {
        threat = nearestThreat(startDistance);
        return threat != null;
    }

    @Override
    public boolean canContinue() {
        return isValidThreat(threat) && mob.distanceToSquared(threat) <= stopDistance * stopDistance;
    }

    @Override
    public void tick() {
        if (!isValidThreat(threat)) {
            return;
        }
        float awayX = mob.getX() - threat.getX();
        float awayZ = mob.getZ() - threat.getZ();
        float distanceSq = awayX * awayX + awayZ * awayZ;
        if (distanceSq < MIN_DISTANCE_SQ) {
            float angle = mob.getRandom().nextFloat() * (float) Math.PI * 2.0f;
            awayX = (float) Math.cos(angle);
            awayZ = (float) Math.sin(angle);
        }
        float targetYaw = (float) Math.toDegrees(Math.atan2(awayX, -awayZ));
        ai.requestSafeMoveDirection(targetYaw, speed, 1.5f);
    }

    @Override
    public void stop() {
        threat = null;
        ai.requestStopMoving();
    }

    private LivingEntity nearestThreat(float range) {
        World world = mob.getWorld();
        if (world == null || avoidType == null) {
            return null;
        }
        float maxDistanceSq = range * range;
        LivingEntity nearest = null;
        for (Entity entity : world.getEntities()) {
            if (!avoidType.isInstance(entity)) {
                continue;
            }
            LivingEntity candidate = avoidType.cast(entity);
            if (!isValidThreat(candidate)) {
                continue;
            }
            float distanceSq = mob.distanceToSquared(candidate);
            if (distanceSq <= maxDistanceSq) {
                maxDistanceSq = distanceSq;
                nearest = candidate;
            }
        }
        return nearest;
    }

    private boolean isValidThreat(LivingEntity candidate) {
        return candidate != null
                && candidate != mob
                && !candidate.isDead()
                && !candidate.isRemoved();
    }
}
