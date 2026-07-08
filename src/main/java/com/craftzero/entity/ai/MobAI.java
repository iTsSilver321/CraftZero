package com.craftzero.entity.ai;

import com.craftzero.entity.LivingEntity;
import com.craftzero.entity.ai.pathfinding.MoveControl;
import com.craftzero.entity.ai.pathfinding.Navigator;
import com.craftzero.entity.ai.pathfinding.PathNode;
import com.craftzero.world.World;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * AI controller for mobs.
 * Manages goals, navigation, and movement control.
 * 
 * Architecture (Minecraft-style):
 * - GoalSelector: Decides what to do (attack, wander, flee)
 * - Navigator: Calculates path to destination (A* pathfinding)
 * - MoveControl: Handles physics of following path
 */
public class MobAI {

    private final LivingEntity mob;
    private final List<Goal> goals;
    private final List<Goal> activeGoals;

    // Navigation and movement (lazy initialized when world is available)
    private Navigator navigator;
    private MoveControl moveControl;
    private boolean navigationInitialized = false;

    // Target tracking
    private LivingEntity target;
    private World.RemotePlayerTarget remotePlayerTarget;
    private float targetX, targetY, targetZ;
    private boolean hasTarget;
    private int lastNavigationBlockX = Integer.MIN_VALUE;
    private int lastNavigationBlockY = Integer.MIN_VALUE;
    private int lastNavigationBlockZ = Integer.MIN_VALUE;
    private boolean pendingDirectMove;
    private float pendingMoveYaw;
    private float pendingMoveSpeed;
    private boolean pendingStop;
    private boolean pendingJump;

    public MobAI(LivingEntity mob) {
        this.mob = mob;
        this.goals = new ArrayList<>();
        this.activeGoals = new ArrayList<>();
        // Navigator and MoveControl are lazy-initialized in
        // ensureNavigationInitialized()
        // because world is not yet set when Mob constructor runs
        this.hasTarget = false;
    }

    /**
     * Lazy initialization of navigation components.
     * Called when navigation is first needed and world is available.
     */
    private void ensureNavigationInitialized() {
        if (!navigationInitialized && mob.getWorld() != null) {
            this.navigator = new Navigator(mob, mob.getWorld());
            this.moveControl = new MoveControl(mob);
            navigationInitialized = true;
            if (hasTarget) {
                retargetNavigatorToCurrentTarget();
            }
        }
    }

    /**
     * Add a goal with the specified priority.
     */
    public void addGoal(int priority, Goal goal) {
        if (goal instanceof PrioritizedGoal) {
            goals.add(goal);
        } else {
            goals.add(new PrioritizedGoal(priority, goal));
        }
        goals.sort(Comparator.comparingInt(Goal::getPriority));
    }

    public boolean isGoalActive(Goal goal) {
        if (goal == null) {
            return false;
        }
        for (Goal active : activeGoals) {
            Goal unwrapped = unwrap(active);
            if (active == goal || unwrapped == goal) {
                return true;
            }
        }
        return false;
    }

    public <T extends Goal> T getGoal(Class<T> type) {
        if (type == null) {
            return null;
        }
        for (Goal goal : goals) {
            Goal unwrapped = unwrap(goal);
            if (type.isInstance(unwrapped)) {
                return type.cast(unwrapped);
            }
        }
        return null;
    }

    public void stopGoals(List<Class<? extends Goal>> goalTypes) {
        if (goalTypes == null || goalTypes.isEmpty()) {
            return;
        }
        java.util.Iterator<Goal> activeIterator = activeGoals.iterator();
        while (activeIterator.hasNext()) {
            Goal goal = activeIterator.next();
            Goal unwrapped = unwrap(goal);
            for (Class<? extends Goal> type : goalTypes) {
                if (type != null && type.isInstance(unwrapped)) {
                    goal.stop();
                    activeIterator.remove();
                    break;
                }
            }
        }
    }

    private static Goal unwrap(Goal goal) {
        return goal instanceof PrioritizedGoal prioritized ? prioritized.wrapped : goal;
    }

    /**
     * Update AI each tick.
     */
    public void tick() {
        // Ensure navigation is initialized (lazy init when world becomes available)
        ensureNavigationInitialized();
        pendingDirectMove = false;
        pendingStop = false;
        pendingJump = false;

        // Evaluate which goals should be running
        // Use iterator to safely remove while iterating
        java.util.Iterator<Goal> activeIterator = activeGoals.iterator();
        while (activeIterator.hasNext()) {
            Goal goal = activeIterator.next();
            if (!goal.canContinue()) {
                goal.stop();
                activeIterator.remove();
            }
        }

        // Check for new goals to start
        for (Goal goal : goals) {
            if (!activeGoals.contains(goal) && goal.canUse()) {
                // Check if this goal conflicts with active goals
                if (goal.isExclusive()) {
                    boolean blockedByActiveExclusive = activeGoals.stream()
                            .anyMatch(active -> active.isExclusive() && active.getPriority() <= goal.getPriority());
                    if (blockedByActiveExclusive) {
                        continue;
                    }
                    // Stop lower priority goals
                    activeGoals.removeIf(active -> {
                        if (active.getPriority() > goal.getPriority()) {
                            active.stop();
                            return true;
                        }
                        return false;
                    });
                }
                goal.start();
                activeGoals.add(goal);
            }
        }

        // Tick all active goals (create copy to avoid ConcurrentModificationException)
        for (Goal goal : new java.util.ArrayList<>(activeGoals)) {
            goal.tick();
        }

        // Update navigation and movement
        if (navigator != null && moveControl != null) {
            boolean hasMovementIntent = false;
            if (pendingJump) {
                moveControl.jump();
            }
            if (pendingStop) {
                navigator.stop();
                moveControl.stop();
                hasMovementIntent = true;
            } else if (pendingDirectMove) {
                moveControl.moveDirection(pendingMoveYaw, pendingMoveSpeed);
                hasMovementIntent = true;
            } else {
                PathNode nextNode = navigator.tick();
                if (nextNode != null) {
                    // Move toward next path node
                    moveControl.moveTo(
                            nextNode.getCenterX(),
                            nextNode.getCenterY(),
                            nextNode.getCenterZ(),
                            getMovementSpeed());
                    hasMovementIntent = true;
                }
            }
            if (hasMovementIntent) {
                moveControl.tick();
            } else if (!isNavigating()) {
                moveControl.stop();
            }
        }
    }

    // ==================== Navigation ====================

    /**
     * Navigate to a position using A* pathfinding.
     */
    public void navigateTo(float x, float y, float z) {
        if (!allFinite(x, y, z)) {
            stopNavigation();
            return;
        }
        this.targetX = x;
        this.targetY = y;
        this.targetZ = z;
        this.hasTarget = true;

        int blockX = (int) Math.floor(x);
        int blockY = (int) Math.floor(y);
        int blockZ = (int) Math.floor(z);
        if (navigator != null && shouldRetargetNavigation(blockX, blockY, blockZ)) {
            retargetNavigatorToCurrentTarget();
        }
    }

    /**
     * Stop navigation.
     */
    public void stopNavigation() {
        this.hasTarget = false;
        this.lastNavigationBlockX = Integer.MIN_VALUE;
        this.lastNavigationBlockY = Integer.MIN_VALUE;
        this.lastNavigationBlockZ = Integer.MIN_VALUE;
        if (navigator != null) {
            navigator.stop();
        }
        if (moveControl != null) {
            moveControl.stop();
        }
    }

    /**
     * Check if navigator has reached target.
     */
    public boolean hasReachedTarget() {
        return navigator != null && navigator.hasReachedTarget();
    }

    /**
     * Check if actively navigating.
     */
    public boolean isNavigating() {
        return navigator != null && navigator.isNavigating();
    }

    /**
     * Get navigator for direct control.
     */
    public Navigator getNavigator() {
        return navigator;
    }

    /**
     * Get move control for direct control.
     */
    public MoveControl getMoveControl() {
        return moveControl;
    }

    /**
     * Request direct movement for this AI tick. Goals should use this instead of
     * mutating entity motion directly.
     */
    public void requestMoveDirection(float yaw, float speed) {
        this.pendingMoveYaw = yaw;
        this.pendingMoveSpeed = speed;
        this.pendingDirectMove = true;
        this.pendingStop = false;
    }

    public boolean requestSafeMoveDirection(float yaw, float speed, float cliffCheckDistance) {
        if (!Float.isFinite(yaw) || !Float.isFinite(speed) || speed <= 0.0f) {
            requestStopMoving();
            return false;
        }
        World world = mob.getWorld();
        float checkDistance = Math.max(0.5f, cliffCheckDistance);
        if (world != null && LineOfSightUtil.isCliffAhead(world,
                mob.getX(), mob.getY(), mob.getZ(), yaw, checkDistance)) {
            float safeYaw = LineOfSightUtil.findSafeDirection(world,
                    mob.getX(), mob.getY(), mob.getZ(), yaw, checkDistance);
            if (LineOfSightUtil.isCliffAhead(world, mob.getX(), mob.getY(), mob.getZ(), safeYaw, checkDistance)) {
                requestStopMoving();
                return false;
            }
            yaw = safeYaw;
        }
        requestMoveDirection(yaw, speed);
        return true;
    }

    public boolean requestMoveToward(float x, float z, float speed, float stopDistance, float cliffCheckDistance) {
        if (!Float.isFinite(x) || !Float.isFinite(z)) {
            requestStopMoving();
            return false;
        }
        float dx = x - mob.getX();
        float dz = z - mob.getZ();
        float distanceSq = dx * dx + dz * dz;
        float stop = Math.max(0.0f, stopDistance);
        if (distanceSq <= stop * stop) {
            requestStopMoving();
            return false;
        }
        float yaw = (float) Math.toDegrees(Math.atan2(dx, -dz));
        return requestSafeMoveDirection(yaw, speed, cliffCheckDistance);
    }

    public void requestStopMoving() {
        this.pendingStop = true;
        this.pendingDirectMove = false;
    }

    public void requestJump() {
        this.pendingJump = true;
    }

    // ==================== Target Management ====================

    /**
     * Set the current attack target.
     */
    public void setTarget(LivingEntity target) {
        if (!isValidAttackTarget(target)) {
            clearTarget();
            return;
        }
        this.remotePlayerTarget = null;
        this.target = target;
    }

    public void setRemotePlayerTarget(World.RemotePlayerTarget target) {
        if (target == null || !target.valid()) {
            this.remotePlayerTarget = null;
            return;
        }
        this.target = null;
        this.remotePlayerTarget = target;
    }

    /**
     * Get the current attack target.
     */
    public LivingEntity getTarget() {
        if (!isValidAttackTarget(target)) {
            target = null;
        }
        return target;
    }

    public World.RemotePlayerTarget getRemotePlayerTarget() {
        if (remotePlayerTarget == null || mob.getWorld() == null) {
            remotePlayerTarget = null;
            return null;
        }
        remotePlayerTarget = mob.getWorld().remotePlayerTargetById(remotePlayerTarget.playerId());
        if (remotePlayerTarget == null || !remotePlayerTarget.valid()) {
            remotePlayerTarget = null;
        }
        return remotePlayerTarget;
    }

    /**
     * Check if there's a valid target.
     */
    public boolean hasTarget() {
        return getTarget() != null;
    }

    public boolean hasAttackTarget() {
        return getTarget() != null || getRemotePlayerTarget() != null;
    }

    public boolean hasRemotePlayerTarget() {
        return getRemotePlayerTarget() != null;
    }

    /**
     * Clear the current target.
     */
    public void clearTarget() {
        this.target = null;
        this.remotePlayerTarget = null;
    }

    public void clearRemotePlayerTarget() {
        this.remotePlayerTarget = null;
    }

    private boolean isValidAttackTarget(LivingEntity candidate) {
        return candidate != null
                && candidate != mob
                && !candidate.isDead()
                && !candidate.isRemoved();
    }

    // ==================== Legacy Movement Target ====================
    // Kept for backward compatibility with existing goals

    /**
     * Set movement target position (legacy - use navigateTo instead).
     */
    public void setMoveTarget(float x, float z) {
        setMoveTarget(x, mob.getY(), z);
    }

    public void setMoveTarget(float x, float y, float z) {
        if (!allFinite(x, y, z)) {
            clearMoveTarget();
            return;
        }
        this.targetX = x;
        this.targetY = y;
        this.targetZ = z;
        this.hasTarget = true;

        // Also trigger navigation
        int blockX = (int) Math.floor(x);
        int blockY = (int) Math.floor(y);
        int blockZ = (int) Math.floor(z);
        if (navigator != null && shouldRetargetNavigation(blockX, blockY, blockZ)) {
            retargetNavigatorToCurrentTarget();
        }
    }

    private void retargetNavigatorToCurrentTarget() {
        if (navigator == null) {
            return;
        }
        navigator.moveTo(targetX, targetY, targetZ);
        lastNavigationBlockX = (int) Math.floor(targetX);
        lastNavigationBlockY = (int) Math.floor(targetY);
        lastNavigationBlockZ = (int) Math.floor(targetZ);
    }

    private boolean shouldRetargetNavigation(int blockX, int blockY, int blockZ) {
        if (blockX == lastNavigationBlockX && blockY == lastNavigationBlockY && blockZ == lastNavigationBlockZ) {
            return false;
        }
        int dx = blockX - lastNavigationBlockX;
        int dy = blockY - lastNavigationBlockY;
        int dz = blockZ - lastNavigationBlockZ;
        return lastNavigationBlockX == Integer.MIN_VALUE
                || lastNavigationBlockY == Integer.MIN_VALUE
                || dy != 0
                || dx * dx + dz * dz >= 2
                || !isNavigating();
    }

    private static boolean allFinite(float... values) {
        for (float value : values) {
            if (!Float.isFinite(value)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Clear movement target.
     */
    public void clearMoveTarget() {
        this.hasTarget = false;
        stopNavigation();
    }

    /**
     * Check if there's a movement target.
     */
    public boolean hasMoveTarget() {
        return hasTarget;
    }

    public float getTargetX() {
        return targetX;
    }

    public float getTargetY() {
        return targetY;
    }

    public float getTargetZ() {
        return targetZ;
    }

    public LivingEntity getMob() {
        return mob;
    }

    /**
     * Get movement speed based on mob state.
     * Uses the mob's moveSpeed attribute.
     */
    private float getMovementSpeed() {
        // Scale the mob's moveSpeed (which is per-tick) to navigator speed
        // Typical moveSpeed is 0.1f (blocks/tick), scale to ~0.7 for navigation
        return mob.getMoveSpeed() * 7.0f;
    }

    /**
     * Wrapper to add priority to a goal.
     */
    private static class PrioritizedGoal implements Goal {
        private final int priority;
        private final Goal wrapped;

        PrioritizedGoal(int priority, Goal wrapped) {
            this.priority = priority;
            this.wrapped = wrapped;
        }

        @Override
        public int getPriority() {
            return priority;
        }

        @Override
        public boolean canUse() {
            return wrapped.canUse();
        }

        @Override
        public boolean canContinue() {
            return wrapped.canContinue();
        }

        @Override
        public void start() {
            wrapped.start();
        }

        @Override
        public void tick() {
            wrapped.tick();
        }

        @Override
        public void stop() {
            wrapped.stop();
        }

        @Override
        public boolean isExclusive() {
            return wrapped.isExclusive();
        }
    }
}
