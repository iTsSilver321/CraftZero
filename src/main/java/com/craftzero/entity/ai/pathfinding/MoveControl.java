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
    private boolean hasDirection;
    private float directionYaw;
    private float directionSpeed;

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
        this.hasDirection = false;
    }

    /**
     * Set direct yaw/speed movement intent for this tick.
     */
    public void moveDirection(float yaw, float speed) {
        this.directionYaw = yaw;
        this.directionSpeed = speed;
        this.hasDirection = true;
        this.hasTarget = false;
    }

    /**
     * Stop all movement.
     */
    public void stop() {
        this.hasTarget = false;
        this.hasDirection = false;
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

        if (wantsToJump && entity.isOnGround() && jumpCooldown <= 0) {
            entity.setJumping(true);
            jumpCooldown = 10;
            wantsToJump = false;
        }

        if (hasDirection) {
            entity.setMoveDirection(directionYaw, directionSpeed);
            hasDirection = false;
            return;
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
            entity.stopMoving();
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
            entity.setJumping(true);
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
