package com.craftzero.entity.ai;

import com.craftzero.entity.LivingEntity;
import com.craftzero.main.Player;
import com.craftzero.world.World;
import java.util.function.BooleanSupplier;

/**
 * AI Goal: Target the nearest player within range.
 * Requires line-of-sight if configured.
 */
public class TargetNearestGoal implements Goal {
    public record State(int checkCooldown, int sightLostTicks, int targetRefreshCooldown) {
    }

    private final LivingEntity mob;
    private final MobAI ai;
    private final float range;
    private final boolean requireSight;
    private final BooleanSupplier canTargetPredicate;

    private int checkCooldown;
    private int sightLostTicks; // How long we've been without sight
    private int targetRefreshCooldown;
    private static final int CHECK_INTERVAL = 10; // Check every 0.5 seconds
    private static final int SIGHT_MEMORY = 40; // Remember target for 2 seconds without sight

    public TargetNearestGoal(LivingEntity mob, MobAI ai, float range) {
        this(mob, ai, range, true); // Default: require sight
    }

    public TargetNearestGoal(LivingEntity mob, MobAI ai, float range, boolean requireSight) {
        this(mob, ai, range, requireSight, () -> true);
    }

    public TargetNearestGoal(LivingEntity mob, MobAI ai, float range, boolean requireSight,
            BooleanSupplier canTargetPredicate) {
        this.mob = mob;
        this.ai = ai;
        this.range = range;
        this.requireSight = requireSight;
        this.canTargetPredicate = canTargetPredicate;
        this.checkCooldown = 0;
        this.sightLostTicks = 0;
        this.targetRefreshCooldown = 0;
    }

    public State getState() {
        return new State(checkCooldown, sightLostTicks, targetRefreshCooldown);
    }

    public void restoreState(State state) {
        if (state == null) {
            return;
        }
        checkCooldown = Math.max(0, state.checkCooldown());
        sightLostTicks = Math.max(0, state.sightLostTicks());
        targetRefreshCooldown = Math.max(0, state.targetRefreshCooldown());
    }

    @Override
    public int getPriority() {
        return 2; // High priority
    }

    @Override
    public boolean canUse() {
        if (checkCooldown > 0) {
            checkCooldown--;
            return ai.hasMoveTarget() || ai.hasRemotePlayerTarget(); // Keep current target if we have one
        }

        if (!canTargetPredicate.getAsBoolean()) {
            ai.clearMoveTarget();
            ai.clearTarget();
            return false;
        }

        checkCooldown = CHECK_INTERVAL;
        return findTarget();
    }

    @Override
    public boolean canContinue() {
        if ((!ai.hasMoveTarget() && !ai.hasRemotePlayerTarget()) || !canTargetPredicate.getAsBoolean()) {
            return false;
        }

        World.RemotePlayerTarget remoteTarget = ai.getRemotePlayerTarget();
        if (remoteTarget != null) {
            return canContinueRemote(remoteTarget);
        }

        Player player = mob.getWorld() != null ? mob.getWorld().getPlayer() : null;
        if (player == null || player.getStats().getHealth() <= 0 || player.isCreative()
                || !player.getDifficulty().allowsHostileSpawns()) {
            return false;
        }

        // Check distance
        float dist = distanceToPlayer(player);
        if (dist > range * 1.5f) {
            return false; // Too far
        }

        // Check line of sight
        if (requireSight) {
            if (hasLineOfSight(player)) {
                sightLostTicks = 0; // Reset sight timer
            } else {
                sightLostTicks++;
                if (sightLostTicks > SIGHT_MEMORY) {
                    return false; // Lost sight for too long
                }
            }
        }

        // Update target position
        targetRefreshCooldown--;
        if (targetRefreshCooldown <= 0) {
            ai.setMoveTarget(player.getPosition().x, player.getPosition().y, player.getPosition().z);
            targetRefreshCooldown = CHECK_INTERVAL;
        }
        return true;
    }

    @Override
    public void start() {
        sightLostTicks = 0;
        targetRefreshCooldown = 0;
    }

    @Override
    public void tick() {
        World.RemotePlayerTarget remoteTarget = ai.getRemotePlayerTarget();
        if (remoteTarget != null) {
            mob.lookAt(remoteTarget.x(), remoteTarget.eyeY(), remoteTarget.z());
            return;
        }
        Player player = mob.getWorld() != null ? mob.getWorld().getPlayer() : null;
        if (player != null) {
            // Look at player
            mob.lookAt(player.getPosition().x, player.getPosition().y + 1.6f, player.getPosition().z);
        }
    }

    @Override
    public void stop() {
        ai.clearMoveTarget();
        ai.clearRemotePlayerTarget();
        sightLostTicks = 0;
    }

    /**
     * Find the nearest valid target.
     */
    private boolean findTarget() {
        if (mob.getWorld() == null)
            return false;

        if (!canTargetPredicate.getAsBoolean()) {
            return false;
        }

        World.RemotePlayerTarget assignedRemoteTarget = ai.getRemotePlayerTarget();
        if (assignedRemoteTarget != null
                && distanceToRemoteTarget(assignedRemoteTarget) <= range
                && (!requireSight || hasLineOfSight(assignedRemoteTarget))) {
            ai.setMoveTarget(assignedRemoteTarget.x(), assignedRemoteTarget.y(), assignedRemoteTarget.z());
            return true;
        }

        Player player = mob.getWorld().getPlayer();
        boolean localValid = isValidLocalTarget(player);
        float localDist = localValid ? distanceToPlayer(player) : Float.MAX_VALUE;
        if (localDist > range) {
            localValid = false;
        }

        World.RemotePlayerTarget remoteTarget = mob.getWorld().nearestRemotePlayerTarget(
                mob.getX(), mob.getY(), mob.getZ(), range, requireSight);
        if (remoteTarget != null && remoteTarget.valid() && (!localValid || remoteTarget.distance() <= localDist)) {
            ai.setRemotePlayerTarget(remoteTarget);
            ai.setMoveTarget(remoteTarget.x(), remoteTarget.y(), remoteTarget.z());
            return true;
        }

        if (!localValid || (requireSight && !hasLineOfSight(player))) {
            return false;
        }

        ai.clearRemotePlayerTarget();
        ai.setMoveTarget(player.getPosition().x, player.getPosition().y, player.getPosition().z);
        return true;
    }

    private boolean canContinueRemote(World.RemotePlayerTarget target) {
        if (target == null || !target.valid()) {
            return false;
        }
        float dist = distanceToRemoteTarget(target);
        if (dist > range * 1.5f) {
            return false;
        }
        if (requireSight) {
            if (hasLineOfSight(target)) {
                sightLostTicks = 0;
            } else {
                sightLostTicks++;
                if (sightLostTicks > SIGHT_MEMORY) {
                    return false;
                }
            }
        }
        targetRefreshCooldown--;
        if (targetRefreshCooldown <= 0) {
            ai.setMoveTarget(target.x(), target.y(), target.z());
            targetRefreshCooldown = CHECK_INTERVAL;
        }
        return true;
    }

    /**
     * Check if we can see the player (no solid blocks in the way).
     */
    private boolean hasLineOfSight(Player player) {
        if (mob.getWorld() == null)
            return false;

        // Eye position of mob
        float eyeY = mob.getY() + mob.getHeight() * 0.85f;

        // Target position (player eye level)
        float targetY = player.getPosition().y + 1.6f;

        return LineOfSightUtil.hasLineOfSight(
                mob.getWorld(),
                mob.getX(), eyeY, mob.getZ(),
                player.getPosition().x, targetY, player.getPosition().z);
    }

    private boolean hasLineOfSight(World.RemotePlayerTarget target) {
        return target != null && mob.getWorld() != null
                && LineOfSightUtil.hasLineOfSight(
                        mob.getWorld(),
                        mob.getX(), mob.getY() + mob.getHeight() * 0.85f, mob.getZ(),
                        target.x(), target.eyeY(), target.z());
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

    private boolean isValidLocalTarget(Player player) {
        return player != null
                && player.getStats().getHealth() > 0.0f
                && !player.isCreative()
                && player.getDifficulty().allowsHostileSpawns()
                && (!requireSight || hasLineOfSight(player));
    }

    @Override
    public boolean isExclusive() {
        return false; // Targeting doesn't prevent other goals
    }
}
