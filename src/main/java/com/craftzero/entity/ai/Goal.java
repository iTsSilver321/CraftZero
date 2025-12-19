package com.craftzero.entity.ai;

/**
 * Base interface for AI goals.
 * Goals are evaluated by priority and executed when conditions are met.
 */
public interface Goal {

    /**
     * Priority of this goal (lower = higher priority).
     * Used to determine which goal should run when multiple are available.
     */
    default int getPriority() {
        return 0;
    }

    /**
     * Check if this goal can start executing.
     * Called every tick when the goal is not active.
     */
    boolean canUse();

    /**
     * Check if this goal should continue executing.
     * Called every tick when the goal is active.
     * Default implementation returns canUse().
     */
    default boolean canContinue() {
        return canUse();
    }

    /**
     * Called when the goal starts executing.
     */
    default void start() {
    }

    /**
     * Called every tick while the goal is active.
     */
    void tick();

    /**
     * Called when the goal stops executing.
     */
    default void stop() {
    }

    /**
     * Whether this goal requires exclusive control.
     * If true, lower priority goals will be stopped.
     */
    default boolean isExclusive() {
        return true;
    }
}
