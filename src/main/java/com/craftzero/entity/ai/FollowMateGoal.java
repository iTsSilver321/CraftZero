package com.craftzero.entity.ai;

import com.craftzero.entity.Entity;
import com.craftzero.entity.mob.Mob;

/**
 * Release 1.0-style love-mode mate seeking for ageable animals.
 */
public class FollowMateGoal implements Goal {

    private static final float STOP_DISTANCE = 2.0f;
    private static final float CONTINUE_RANGE_MULTIPLIER = 1.25f;

    private final Mob mob;
    private final MobAI ai;
    private final float range;
    private final float speed;
    private Mob targetMate;

    public FollowMateGoal(Mob mob, MobAI ai, float range, float speed) {
        this.mob = mob;
        this.ai = ai;
        this.range = range;
        this.speed = speed;
    }

    @Override
    public boolean canUse() {
        targetMate = findNearestMate(range);
        return targetMate != null;
    }

    @Override
    public boolean canContinue() {
        float continueRange = range * CONTINUE_RANGE_MULTIPLIER;
        return targetMate != null
                && mob.canSeekBreedingMate(targetMate)
                && distanceSquared(targetMate) <= continueRange * continueRange;
    }

    @Override
    public void tick() {
        if (targetMate == null) {
            ai.requestStopMoving();
            return;
        }

        mob.lookAt(targetMate.getX(), targetMate.getY() + targetMate.getHeight() * 0.85f, targetMate.getZ());

        float dx = targetMate.getX() - mob.getX();
        float dz = targetMate.getZ() - mob.getZ();
        float distanceSq = dx * dx + dz * dz;
        if (distanceSq <= STOP_DISTANCE * STOP_DISTANCE) {
            ai.requestStopMoving();
            return;
        }

        float yaw = (float) Math.toDegrees(Math.atan2(dx, -dz));
        ai.requestMoveDirection(yaw, speed);
    }

    @Override
    public void stop() {
        targetMate = null;
        ai.requestStopMoving();
    }

    private Mob findNearestMate(float maxRange) {
        if (mob.getWorld() == null || !mob.isInLove()) {
            return null;
        }
        float bestDistanceSq = maxRange * maxRange;
        Mob best = null;
        for (Entity entity : mob.getWorld().getEntities()) {
            if (entity == mob || !(entity instanceof Mob mate) || !mob.canSeekBreedingMate(mate)) {
                continue;
            }
            float distanceSq = distanceSquared(mate);
            if (distanceSq < bestDistanceSq) {
                bestDistanceSq = distanceSq;
                best = mate;
            }
        }
        return best;
    }

    private float distanceSquared(Mob mate) {
        float dx = mate.getX() - mob.getX();
        float dy = mate.getY() - mob.getY();
        float dz = mate.getZ() - mob.getZ();
        return dx * dx + dy * dy + dz * dz;
    }
}
