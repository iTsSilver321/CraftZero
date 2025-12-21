package com.craftzero.entity.ai.pathfinding;

import com.craftzero.entity.LivingEntity;

/**
 * Controls mob movement physics when following a path.
 * Handles turning, walking, and jumping.
 */
public class MoveControl {

    private final LivingEntity entity;

    // Current movement target
    private float targetX, targetY, targetZ;
    private float speed;
    private boolean hasTarget;

    // Jump control
    private boolean wantsToJump;
    private int jumpCooldown;

    public MoveControl(LivingEntity entity) {
        this.entity = entity;
        this.hasTarget = false;
        this.jumpCooldown = 0;
    }

    /**
     * Set movement target.
     */
    public void moveTo(float x, float y, float z, float speed) {
        this.targetX = x;
        this.targetY = y;
        this.targetZ = z;
        this.speed = speed;
        this.hasTarget = true;
    }

    /**
     * Stop all movement.
     */
    public void stop() {
        this.hasTarget = false;
        entity.stopMoving();
    }

    /**
     * Request a jump.
     */
    public void jump() {
        if (jumpCooldown <= 0) {
            wantsToJump = true;
        }
    }

    /**
     * Update movement each tick.
     */
    public void tick() {
        if (jumpCooldown > 0) {
            jumpCooldown--;
        }

        if (!hasTarget) {
            return;
        }

        // Calculate direction to target
        float dx = targetX - entity.getX();
        float dy = targetY - entity.getY();
        float dz = targetZ - entity.getZ();
        float horizontalDist = (float) Math.sqrt(dx * dx + dz * dz);

        // Check if we've reached the target
        if (horizontalDist < 0.3f) {
            hasTarget = false;
            return;
        }

        // Calculate target yaw
        float targetYaw = (float) Math.toDegrees(Math.atan2(dx, -dz));

        // Check if we need to jump (target is higher)
        if (dy > 0.5f && entity.isOnGround() && jumpCooldown <= 0) {
            wantsToJump = true;
        }

        // Check if we're blocked and need to jump
        if (entity.isCollidedHorizontally() && entity.isOnGround() && jumpCooldown <= 0) {
            wantsToJump = true;
        }

        // Execute jump
        if (wantsToJump && entity.isOnGround() && jumpCooldown <= 0) {
            entity.addMotion(0, 0.42f, 0); // Standard jump height
            jumpCooldown = 10; // Cooldown to prevent spam
            wantsToJump = false;
        }

        // Set move direction (entity handles smooth rotation)
        entity.setMoveDirection(targetYaw, speed);
    }

    /**
     * Check if currently moving to a target.
     */
    public boolean isMoving() {
        return hasTarget;
    }

    /**
     * Get remaining distance to target.
     */
    public float getDistanceToTarget() {
        if (!hasTarget)
            return 0;
        float dx = targetX - entity.getX();
        float dz = targetZ - entity.getZ();
        return (float) Math.sqrt(dx * dx + dz * dz);
    }
}
