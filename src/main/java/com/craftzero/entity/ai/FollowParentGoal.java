package com.craftzero.entity.ai;

import com.craftzero.entity.Entity;
import com.craftzero.entity.mob.Mob;

/**
 * Release 1.0-style baby animal behavior: young passive animals trail nearby
 * adult parents of the same species until they are close enough.
 */
public class FollowParentGoal implements Goal {

    private static final float START_DISTANCE = 3.0f;
    private static final float SEARCH_RANGE = 8.0f;
    private static final float MAX_CONTINUE_DISTANCE = 16.0f;

    private final Mob mob;
    private final MobAI ai;
    private final float speed;
    private Mob targetParent;

    public FollowParentGoal(Mob mob, MobAI ai, float speed) {
        this.mob = mob;
        this.ai = ai;
        this.speed = speed;
    }

    @Override
    public boolean canUse() {
        targetParent = findNearestParent(SEARCH_RANGE);
        return targetParent != null;
    }

    @Override
    public boolean canContinue() {
        if (targetParent == null || !mob.canFollowParent(targetParent)) {
            return false;
        }
        float distanceSq = distanceSquared(targetParent);
        return distanceSq >= START_DISTANCE * START_DISTANCE
                && distanceSq <= MAX_CONTINUE_DISTANCE * MAX_CONTINUE_DISTANCE;
    }

    @Override
    public void tick() {
        if (targetParent == null) {
            ai.requestStopMoving();
            return;
        }

        mob.lookAt(targetParent.getX(), targetParent.getY() + targetParent.getHeight() * 0.85f, targetParent.getZ());

        float dx = targetParent.getX() - mob.getX();
        float dz = targetParent.getZ() - mob.getZ();
        float horizontalDistanceSq = dx * dx + dz * dz;
        if (horizontalDistanceSq <= START_DISTANCE * START_DISTANCE) {
            ai.requestStopMoving();
            return;
        }

        float yaw = (float) Math.toDegrees(Math.atan2(dx, -dz));
        ai.requestMoveDirection(yaw, speed);
    }

    @Override
    public void stop() {
        targetParent = null;
        ai.requestStopMoving();
    }

    private Mob findNearestParent(float maxRange) {
        if (mob.getWorld() == null || !mob.isBaby() || mob.isDead() || mob.isRemoved()) {
            return null;
        }

        float minDistanceSq = START_DISTANCE * START_DISTANCE;
        float bestDistanceSq = maxRange * maxRange;
        Mob best = null;
        for (Entity entity : mob.getWorld().getEntities()) {
            if (entity == mob || !(entity instanceof Mob candidate) || !mob.canFollowParent(candidate)) {
                continue;
            }
            float distanceSq = distanceSquared(candidate);
            if (distanceSq >= minDistanceSq && distanceSq <= bestDistanceSq) {
                bestDistanceSq = distanceSq;
                best = candidate;
            }
        }
        return best;
    }

    private float distanceSquared(Mob other) {
        float dx = other.getX() - mob.getX();
        float dy = other.getY() - mob.getY();
        float dz = other.getZ() - mob.getZ();
        return dx * dx + dy * dy + dz * dz;
    }
}
