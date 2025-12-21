package com.craftzero.entity.ai;

import com.craftzero.entity.LivingEntity;
import com.craftzero.entity.ai.pathfinding.MoveControl;
import com.craftzero.entity.ai.pathfinding.Navigator;
import com.craftzero.entity.ai.pathfinding.PathNode;

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

    // Navigation and movement
    private final Navigator navigator;
    private final MoveControl moveControl;

    // Target tracking
    private LivingEntity target;
    private float targetX, targetY, targetZ;
    private boolean hasTarget;

    public MobAI(LivingEntity mob) {
        this.mob = mob;
        this.goals = new ArrayList<>();
        this.activeGoals = new ArrayList<>();

        // Initialize navigation (will be null if world not set yet)
        if (mob.getWorld() != null) {
            this.navigator = new Navigator(mob, mob.getWorld());
            this.moveControl = new MoveControl(mob);
        } else {
            this.navigator = null;
            this.moveControl = null;
        }

        this.hasTarget = false;
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

    /**
     * Update AI each tick.
     */
    public void tick() {
        // Evaluate which goals should be running
        for (Goal goal : goals) {
            if (activeGoals.contains(goal)) {
                // Goal is running - check if it should stop
                if (!goal.canContinue()) {
                    goal.stop();
                    activeGoals.remove(goal);
                }
            } else {
                // Goal is not running - check if it should start
                if (goal.canUse()) {
                    // Check if this goal conflicts with active goals
                    if (goal.isExclusive()) {
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
        }

        // Tick all active goals
        for (Goal goal : activeGoals) {
            goal.tick();
        }

        // Update navigation and movement
        if (navigator != null && moveControl != null) {
            PathNode nextNode = navigator.tick();
            if (nextNode != null) {
                // Move toward next path node
                moveControl.moveTo(
                        nextNode.getCenterX(),
                        nextNode.getCenterY(),
                        nextNode.getCenterZ(),
                        getMovementSpeed());
            }
            moveControl.tick();
        }
    }

    // ==================== Navigation ====================

    /**
     * Navigate to a position using A* pathfinding.
     */
    public void navigateTo(float x, float y, float z) {
        this.targetX = x;
        this.targetY = y;
        this.targetZ = z;
        this.hasTarget = true;

        if (navigator != null) {
            navigator.moveTo(x, y, z);
        }
    }

    /**
     * Stop navigation.
     */
    public void stopNavigation() {
        this.hasTarget = false;
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

    // ==================== Target Management ====================

    /**
     * Set the current attack target.
     */
    public void setTarget(LivingEntity target) {
        this.target = target;
    }

    /**
     * Get the current attack target.
     */
    public LivingEntity getTarget() {
        return target;
    }

    /**
     * Check if there's a valid target.
     */
    public boolean hasTarget() {
        return target != null && !target.isDead() && !target.isRemoved();
    }

    /**
     * Clear the current target.
     */
    public void clearTarget() {
        this.target = null;
    }

    // ==================== Legacy Movement Target ====================
    // Kept for backward compatibility with existing goals

    /**
     * Set movement target position (legacy - use navigateTo instead).
     */
    public void setMoveTarget(float x, float z) {
        this.targetX = x;
        this.targetZ = z;
        this.hasTarget = true;

        // Also trigger navigation
        if (navigator != null) {
            navigator.moveTo(x, mob.getY(), z);
        }
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

    public float getTargetZ() {
        return targetZ;
    }

    public LivingEntity getMob() {
        return mob;
    }

    /**
     * Get movement speed based on mob state.
     */
    private float getMovementSpeed() {
        // Base speed, can be modified by goals
        return 0.7f;
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
