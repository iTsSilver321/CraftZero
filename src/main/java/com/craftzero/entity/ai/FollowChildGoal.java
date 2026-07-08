package com.craftzero.entity.ai;

import com.craftzero.entity.Entity;
import com.craftzero.entity.mob.Mob;

/**
 * Release 1.0-style post-breeding parent behavior: cooldown adults can trail a
 * nearby baby of the same species.
 */
public class FollowChildGoal implements Goal {

    private static final float STOP_DISTANCE = 2.5f;
    private static final float SEARCH_RANGE = 8.0f;
    private static final float MAX_CONTINUE_DISTANCE = 16.0f;

    private final Mob mob;
    private final MobAI ai;
    private final float speed;
    private Mob targetChild;

    public FollowChildGoal(Mob mob, MobAI ai, float speed) {
        this.mob = mob;
        this.ai = ai;
        this.speed = speed;
    }

    @Override
    public boolean canUse() {
        targetChild = findNearestChild(SEARCH_RANGE);
        return targetChild != null;
    }

    @Override
    public boolean canContinue() {
        if (targetChild == null || !mob.canFollowChild(targetChild)) {
            return false;
        }
        float distanceSq = distanceSquared(targetChild);
        return distanceSq >= STOP_DISTANCE * STOP_DISTANCE
                && distanceSq <= MAX_CONTINUE_DISTANCE * MAX_CONTINUE_DISTANCE;
    }

    @Override
    public void tick() {
        if (targetChild == null) {
            ai.requestStopMoving();
            return;
        }

        mob.lookAt(targetChild.getX(), targetChild.getY() + targetChild.getHeight() * 0.85f, targetChild.getZ());

        float dx = targetChild.getX() - mob.getX();
        float dz = targetChild.getZ() - mob.getZ();
        float horizontalDistanceSq = dx * dx + dz * dz;
        if (horizontalDistanceSq <= STOP_DISTANCE * STOP_DISTANCE) {
            ai.requestStopMoving();
            return;
        }

        float yaw = (float) Math.toDegrees(Math.atan2(dx, -dz));
        ai.requestMoveDirection(yaw, speed);
    }

    @Override
    public void stop() {
        targetChild = null;
        ai.requestStopMoving();
    }

    private Mob findNearestChild(float maxRange) {
        if (mob.getWorld() == null || mob.isBaby() || mob.isDead() || mob.isRemoved()) {
            return null;
        }

        float bestDistanceSq = maxRange * maxRange;
        Mob best = null;
        for (Entity entity : mob.getWorld().getEntities()) {
            if (entity == mob || !(entity instanceof Mob candidate) || !mob.canFollowChild(candidate)) {
                continue;
            }
            float distanceSq = distanceSquared(candidate);
            if (distanceSq <= bestDistanceSq) {
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
