package com.craftzero.entity;

import com.craftzero.world.BlockType;
import com.craftzero.world.World;

/**
 * Represents a dropped item in the world.
 * Features Minecraft-style spinning and bobbing animation.
 */
public class DroppedItem {

    // Position
    private float x, y, z;

    // Physics
    private float velocityX, velocityY, velocityZ;
    private boolean onGround;

    // Item data
    private BlockType blockType;
    private int count;

    // Animation state
    private float age; // Seconds since spawn
    private float rotation; // Current Y rotation (degrees)
    private float bobPhase; // Phase for sine wave bobbing

    // Constants - Minecraft-like values
    private static final float GRAVITY = -25.0f;
    private static final float GROUND_FRICTION = 0.8f;
    private static final float SPIN_SPEED = 90.0f; // degrees/second (1 rotation per 4 seconds)
    private static final float BOB_SPEED = 2.5f; // radians/second
    private static final float BOB_AMPLITUDE = 0.1f; // blocks (vertical movement range)
    private static final float PICKUP_DELAY = 0.5f; // seconds before can be picked up
    private static final float DESPAWN_TIME = 300.0f; // 5 minutes
    private static final float ATTRACTION_RADIUS = 2.0f; // Start moving toward player
    private static final float PICKUP_RADIUS = 1.0f; // Actually collect item
    private static final float ATTRACTION_SPEED = 15.0f; // Speed when attracted to player

    // Visual
    private static final float SCALE = 0.25f; // Size relative to full block

    public DroppedItem(float x, float y, float z, BlockType blockType, int count) {
        this(x, y, z, blockType, count, 0, 4.0f, 0); // Default: slight upward pop
    }

    /**
     * Constructor with initial velocity (for thrown items).
     */
    public DroppedItem(float x, float y, float z, BlockType blockType, int count,
            float velX, float velY, float velZ) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.blockType = blockType;
        this.count = count;

        // Initial velocity
        this.velocityX = velX;
        this.velocityY = velY;
        this.velocityZ = velZ;
        this.onGround = false;

        // Random initial rotation
        this.rotation = (float) (Math.random() * 360.0);
        this.bobPhase = (float) (Math.random() * Math.PI * 2);
        this.age = 0;
    }

    /**
     * Update physics and animation.
     * 
     * @return true if item should be removed (despawned)
     */
    public boolean update(float deltaTime, World world) {
        age += deltaTime;

        // Check despawn
        if (age >= DESPAWN_TIME) {
            return true;
        }

        // Physics - gravity and collision
        if (!onGround) {
            velocityY += GRAVITY * deltaTime;

            // Apply horizontal velocity with friction
            x += velocityX * deltaTime;
            z += velocityZ * deltaTime;
            velocityX *= 0.98f;
            velocityZ *= 0.98f;

            y += velocityY * deltaTime;

            // Simple ground check - find the block the item is trying to move into
            int blockX = (int) Math.floor(x);
            int blockZ = (int) Math.floor(z);
            int blockY = (int) Math.floor(y - 0.1f); // Check slightly below

            // Check block at feet level
            if (blockY >= 0) {
                BlockType below = world.getBlock(blockX, blockY, blockZ);
                if (below.isSolid()) {
                    // Land on top of this block
                    y = blockY + 1.0f + 0.1f; // Slight offset above ground
                    velocityY = 0;
                    velocityX = 0;
                    velocityZ = 0;
                    onGround = true;
                }
            }

            // Prevent falling through world
            if (y < 0) {
                y = 1;
                velocityY = 0;
                onGround = true;
            }
        } else {
            // Re-check if ground disappeared (block broken below)
            int blockX = (int) Math.floor(x);
            int blockZ = (int) Math.floor(z);
            int blockY = (int) Math.floor(y - 0.2f);

            if (blockY >= 0) {
                BlockType below = world.getBlock(blockX, blockY, blockZ);
                if (!below.isSolid()) {
                    onGround = false; // Start falling again
                }
            }
        }

        // Animation - spinning
        rotation += SPIN_SPEED * deltaTime;
        if (rotation >= 360) {
            rotation -= 360;
        }

        // Animation - bobbing (only when on ground)
        if (onGround) {
            bobPhase += BOB_SPEED * deltaTime;
            if (bobPhase >= Math.PI * 2) {
                bobPhase -= (float) (Math.PI * 2);
            }
        }

        return false;
    }

    /**
     * Try to attract/collect this item toward a player.
     * 
     * @return true if item was collected
     */
    public boolean tryCollect(float playerX, float playerY, float playerZ, float deltaTime) {
        // Can't pickup during delay
        if (age < PICKUP_DELAY) {
            return false;
        }

        float dx = playerX - x;
        float dy = playerY - y;
        float dz = playerZ - z;
        float distance = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);

        // Collect if within pickup radius
        if (distance < PICKUP_RADIUS) {
            return true;
        }

        // Attract if within attraction radius
        if (distance < ATTRACTION_RADIUS && distance > 0.1f) {
            float speed = ATTRACTION_SPEED * deltaTime;
            float factor = speed / distance;

            x += dx * factor;
            y += dy * factor;
            z += dz * factor;

            // Lift off ground when attracted
            onGround = false;
        }

        return false;
    }

    // Getters
    public float getX() {
        return x;
    }

    public float getY() {
        return y;
    }

    public float getZ() {
        return z;
    }

    /**
     * Get the visual Y position including bob animation.
     * Uses abs(sin) to only bob upward - prevents items from clipping through
     * ground.
     */
    public float getVisualY() {
        if (onGround) {
            // Only bob upward (abs of sin gives 0 to 1 range, not -1 to 1)
            return y + Math.abs((float) Math.sin(bobPhase)) * BOB_AMPLITUDE;
        }
        return y;
    }

    public float getRotation() {
        return rotation;
    }

    public float getScale() {
        return SCALE;
    }

    public BlockType getBlockType() {
        return blockType;
    }

    public int getCount() {
        return count;
    }

    public float getAge() {
        return age;
    }

    public void setCount(int count) {
        this.count = count;
    }

    /**
     * Check if this item can merge with another of the same type.
     */
    public boolean canMergeWith(DroppedItem other) {
        return this.blockType == other.blockType &&
                this.count + other.count <= 64;
    }

    /**
     * Merge another item into this one.
     */
    public void mergeWith(DroppedItem other) {
        this.count += other.count;
    }
}
