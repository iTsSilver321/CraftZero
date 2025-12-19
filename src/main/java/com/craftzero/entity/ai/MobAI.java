package com.craftzero.entity.ai;

import com.craftzero.entity.LivingEntity;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * AI controller for mobs.
 * Manages a list of goals and executes them based on priority.
 */
public class MobAI {

    private final LivingEntity mob;
    private final List<Goal> goals;
    private final List<Goal> activeGoals;

    // Target tracking
    private LivingEntity target;

    // Movement target
    private float targetX, targetZ;
    private boolean hasMovementTarget;

    public MobAI(LivingEntity mob) {
        this.mob = mob;
        this.goals = new ArrayList<>();
        this.activeGoals = new ArrayList<>();
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
    }

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

    /**
     * Set movement target position.
     */
    public void setMoveTarget(float x, float z) {
        this.targetX = x;
        this.targetZ = z;
        this.hasMovementTarget = true;
    }

    /**
     * Clear movement target.
     */
    public void clearMoveTarget() {
        this.hasMovementTarget = false;
    }

    /**
     * Check if there's a movement target.
     */
    public boolean hasMoveTarget() {
        return hasMovementTarget;
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
