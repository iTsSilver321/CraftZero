package com.craftzero.entity.ai;

import com.craftzero.entity.mob.Mob;
import com.craftzero.main.Player;
import com.craftzero.world.World;

/**
 * AI Goal: Look at nearby player.
 * Low priority - just for visual interest when idle.
 * Makes passive mobs feel more alive and aware.
 */
public class LookAtPlayerGoal implements Goal {

    private final Mob mob;
    private final float range;
    private float lookDuration;
    private float lookX, lookY, lookZ;
    private boolean hasTarget;
    private Player targetPlayer;
    private String targetRemotePlayerId = "";

    private static final float MAX_LOOK_TIME = 40; // 2 seconds
    private static final float IDLE_MOTION_THRESHOLD_SQ = 0.0004f;

    public LookAtPlayerGoal(Mob mob, float range) {
        this.mob = mob;
        this.range = range;
        this.hasTarget = false;
    }

    @Override
    public int getPriority() {
        return 8; // Very low priority - only when idle
    }

    @Override
    public boolean canUse() {
        if (mob.getWorld() == null)
            return false;
        if (!isIdleEnoughToLook())
            return false;

        LookTarget target = findLookTarget(range);
        if (target == null)
            return false;

        // Random chance to start looking (not constant staring)
        if (mob.getRandom().nextFloat() > 0.1f)
            return false;

        setLookTarget(target);
        lookDuration = MAX_LOOK_TIME;
        hasTarget = true;

        return true;
    }

    @Override
    public boolean canContinue() {
        if (!hasTarget)
            return false;
        if (lookDuration <= 0)
            return false;
        if (!isIdleEnoughToLook())
            return false;

        LookTarget target = currentLookTarget(range * 1.5f);
        if (target == null) {
            return false;
        }
        setLookTarget(target);
        return true;
    }

    @Override
    public void start() {
        // Already set in canUse
    }

    @Override
    public void tick() {
        lookDuration--;

        LookTarget target = currentLookTarget(range * 1.5f);
        if (target != null) {
            setLookTarget(target);
        }

        // Look at player (just head rotation, not body)
        mob.lookAt(lookX, lookY, lookZ);
    }

    @Override
    public void stop() {
        hasTarget = false;
        targetPlayer = null;
        targetRemotePlayerId = "";
    }

    private LookTarget findLookTarget(float maxRange) {
        Player player = mob.getWorld() != null ? mob.getWorld().getPlayer() : null;
        boolean localValid = player != null && distanceToPlayer(player) <= maxRange;
        float localDistance = localValid ? distanceToPlayer(player) : Float.MAX_VALUE;
        World.RemotePlayerTarget remote = nearestRemoteLookTarget(maxRange);
        if (remote != null && remote.valid() && remote.distance() <= localDistance) {
            return LookTarget.remote(remote);
        }
        return localValid ? LookTarget.local(player) : null;
    }

    private LookTarget currentLookTarget(float maxRange) {
        if (mob.getWorld() == null) {
            return null;
        }
        if (targetRemotePlayerId != null && !targetRemotePlayerId.isBlank()) {
            World.RemotePlayerTarget remote = mob.getWorld().remotePlayerViewById(targetRemotePlayerId);
            return remote != null && remote.valid() && distanceToRemoteTarget(remote) <= maxRange
                    ? LookTarget.remote(remote)
                    : null;
        }
        return targetPlayer != null && distanceToPlayer(targetPlayer) <= maxRange
                ? LookTarget.local(targetPlayer)
                : null;
    }

    private World.RemotePlayerTarget nearestRemoteLookTarget(float maxRange) {
        if (mob.getWorld() == null) {
            return null;
        }
        for (World.RemotePlayerTarget target : mob.getWorld().remotePlayerViews(
                mob.getX(), mob.getY(), mob.getZ(), maxRange, false)) {
            if (target != null && target.valid()) {
                return target;
            }
        }
        return null;
    }

    private void setLookTarget(LookTarget target) {
        if (target == null) {
            return;
        }
        targetPlayer = target.player();
        targetRemotePlayerId = target.remoteTarget() == null ? "" : target.remoteTarget().playerId();
        if (target.player() != null) {
            lookX = target.player().getPosition().x;
            lookY = target.player().getPosition().y + 1.6f;
            lookZ = target.player().getPosition().z;
        } else if (target.remoteTarget() != null) {
            lookX = target.remoteTarget().x();
            lookY = target.remoteTarget().eyeY();
            lookZ = target.remoteTarget().z();
        }
    }

    private float distanceToPlayer(Player player) {
        float dx = player.getPosition().x - mob.getX();
        float dy = player.getPosition().y - mob.getY();
        float dz = player.getPosition().z - mob.getZ();
        return (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    private float distanceToRemoteTarget(World.RemotePlayerTarget target) {
        float dx = target.x() - mob.getX();
        float dy = target.y() - mob.getY();
        float dz = target.z() - mob.getZ();
        return (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    private boolean isIdleEnoughToLook() {
        float horizontalMotionSq = mob.getMotionX() * mob.getMotionX() + mob.getMotionZ() * mob.getMotionZ();
        return horizontalMotionSq <= IDLE_MOTION_THRESHOLD_SQ
                && !mob.getAI().hasMoveTarget()
                && !mob.getAI().isNavigating();
    }

    @Override
    public boolean isExclusive() {
        return false; // Looking doesn't prevent other actions
    }

    private record LookTarget(Player player, World.RemotePlayerTarget remoteTarget) {
        static LookTarget local(Player player) {
            return new LookTarget(player, null);
        }

        static LookTarget remote(World.RemotePlayerTarget target) {
            return new LookTarget(null, target);
        }
    }
}
