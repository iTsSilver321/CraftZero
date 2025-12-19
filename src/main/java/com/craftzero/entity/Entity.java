package com.craftzero.entity;

import com.craftzero.physics.AABB;
import com.craftzero.world.BlockType;
import com.craftzero.world.World;
import org.joml.Vector3f;

import java.util.List;

/**
 * Base class for all entities in the world (mobs, dropped items, projectiles).
 * 
 * COORDINATE SYSTEM: x, y, z represents the BOTTOM-CENTER (feet level).
 * The AABB extends from (x-width/2, y, z-width/2) to (x+width/2, y+height,
 * z+width/2).
 */
public abstract class Entity {

    // Current position (bottom-center / feet level)
    protected float x, y, z;

    // Previous position for render interpolation (20hz physics -> 60hz display)
    protected float prevX, prevY, prevZ;

    // Velocity
    protected float motionX, motionY, motionZ;

    // Rotation (degrees)
    protected float yaw; // Horizontal rotation (0-360)
    protected float pitch; // Vertical rotation (-90 to 90)
    protected float prevYaw, prevPitch; // For interpolation

    // Dimensions
    protected final float width;
    protected final float height;

    // Collision state
    protected boolean onGround;
    protected boolean collidedHorizontally;
    protected boolean collidedVertically;
    protected boolean inWater;

    // Physics constants
    protected static final float GRAVITY = -28.0f; // blocks/s^2 (Minecraft-like)
    protected static final float TERMINAL_VELOCITY = -78.4f; // blocks/s
    protected static final float AIR_RESISTANCE = 0.98f;
    protected static final float GROUND_FRICTION = 0.6f;

    // State
    protected boolean removed = false;
    protected int ticksExisted = 0;

    // Animation tracking
    protected float distanceWalked = 0.0f;
    protected float prevDistanceWalked = 0.0f;

    // Reference to world
    protected World world;

    public Entity(float width, float height) {
        this.width = width;
        this.height = height;
        this.yaw = 0;
        this.pitch = 0;
    }

    /**
     * Set the entity's position (bottom-center).
     */
    public void setPosition(float x, float y, float z) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.prevX = x;
        this.prevY = y;
        this.prevZ = z;
    }

    /**
     * Set the world this entity belongs to.
     */
    public void setWorld(World world) {
        this.world = world;
    }

    /**
     * Create the AABB for this entity at current position.
     * Uses bottom-center coordinate system.
     */
    public AABB getBoundingBox() {
        float halfWidth = width / 2.0f;
        return new AABB(
                x - halfWidth, y, z - halfWidth,
                x + halfWidth, y + height, z + halfWidth);
    }

    /**
     * Create the AABB at a specific position.
     */
    protected AABB getBoundingBoxAt(float px, float py, float pz) {
        float halfWidth = width / 2.0f;
        return new AABB(
                px - halfWidth, py, pz - halfWidth,
                px + halfWidth, py + height, pz + halfWidth);
    }

    /**
     * Main update tick. Called every physics frame.
     * ORDER:
     * 1. Capture previous position (for interpolation)
     * 2. Subclass tick logic (AI, timers, etc.)
     * Physics is called separately after tick().
     */
    public void tick() {
        // FIRST: Capture previous position for interpolation
        prevX = x;
        prevY = y;
        prevZ = z;
        prevYaw = yaw;
        prevPitch = pitch;
        prevDistanceWalked = distanceWalked;

        // Increment tick counter
        ticksExisted++;
    }

    /**
     * Update physics: gravity, movement, collision.
     * Called AFTER tick() so AI has set motion.
     * Uses per-tick motion values (Minecraft-style), NOT velocity per second.
     */
    public void updatePhysics(float deltaTime) {
        if (world == null)
            return;

        // Apply gravity if not on ground (per-tick, not per-second)
        // Minecraft gravity: -0.08 blocks/tick, terminal velocity around -3.92
        // blocks/tick
        if (!onGround) {
            motionY -= 0.08f; // Gravity per tick
            if (motionY < -3.92f) {
                motionY = -3.92f; // Terminal velocity
            }
        }

        // Apply air resistance (same as Minecraft: 0.98 per tick)
        motionX *= 0.98f;
        motionZ *= 0.98f;
        motionY *= 0.98f;

        // Move with collision (motion is already per-tick, no deltaTime needed)
        moveWithCollision(motionX, motionY, motionZ);

        // Ground friction (slipperiness - Minecraft uses 0.6 for dirt/stone)
        if (onGround) {
            float friction = 0.6f;
            motionX *= friction;
            motionZ *= friction;
        }

        // Track distance walked for animation
        float dx = x - prevX;
        float dz = z - prevZ;
        float horizontalDist = (float) Math.sqrt(dx * dx + dz * dz);
        distanceWalked += horizontalDist;

        // Check if in water
        updateInWater();
    }

    /**
     * Move entity with collision detection.
     * Uses Minecraft-style independent axis resolution.
     */
    protected void moveWithCollision(float dx, float dy, float dz) {
        if (world == null)
            return;

        // Store original values
        float originalDx = dx;
        float originalDy = dy;
        float originalDz = dz;

        // Get current bounding box
        AABB box = getBoundingBox();

        // Get all blocks that could collide
        List<AABB> colliders = getCollidingBlockBoxes(box, dx, dy, dz);

        // Resolve Y axis first (gravity/jumping)
        for (AABB blockBox : colliders) {
            dy = box.clipYCollide(blockBox, dy);
        }
        box.move(0, dy, 0);

        // Resolve X axis
        for (AABB blockBox : colliders) {
            dx = box.clipXCollide(blockBox, dx);
        }
        box.move(dx, 0, 0);

        // Resolve Z axis
        for (AABB blockBox : colliders) {
            dz = box.clipZCollide(blockBox, dz);
        }
        box.move(0, 0, dz);

        // Update position
        x += dx;
        y += dy;
        z += dz;

        // Update collision flags
        collidedHorizontally = (dx != originalDx) || (dz != originalDz);
        collidedVertically = dy != originalDy;
        onGround = collidedVertically && originalDy < 0;

        // Cancel velocity on collision
        if (dz != originalDz)
            motionZ = 0;

        // Push out of other entities
        pushOutOfEntities();
    }

    /**
     * Push this entity out of other entities.
     */
    protected void pushOutOfEntities() {
        if (world == null)
            return;

        List<Entity> entities = world.getEntities();
        AABB myBox = getBoundingBox();

        for (Entity other : entities) {
            if (other == this)
                continue;

            // Simple box intersection
            AABB otherBox = other.getBoundingBox();
            if (myBox.intersects(otherBox)) {
                // Calculate push vector
                float dx = x - other.x;
                float dz = z - other.z;
                float dist = (float) Math.abs(Math.max(Math.abs(dx), Math.abs(dz)));

                if (dist >= 0.01f) {
                    dist = (float) Math.sqrt(dist);
                    dx /= dist;
                    dz /= dist;

                    float pushStrength = 0.02f; // Reduced from 0.1f

                    // Apply push to both
                    if (this instanceof LivingEntity) {
                        this.motionX += dx * pushStrength;
                        this.motionZ += dz * pushStrength;
                    }
                    // If other is moving (not player controlled directly here, but general logic)
                    if (other instanceof LivingEntity) {
                        other.motionX -= dx * pushStrength;
                        other.motionZ -= dz * pushStrength;
                    }
                }
            }
        }
    }

    /**
     * Get all solid block AABBs that could intersect with entity movement.
     */
    protected List<AABB> getCollidingBlockBoxes(AABB box, float dx, float dy, float dz) {
        java.util.ArrayList<AABB> colliders = new java.util.ArrayList<>();

        // Expand box to cover movement path
        float minX = Math.min(box.getMin().x + dx, box.getMin().x) - 0.1f;
        float minY = Math.min(box.getMin().y + dy, box.getMin().y) - 0.1f;
        float minZ = Math.min(box.getMin().z + dz, box.getMin().z) - 0.1f;
        float maxX = Math.max(box.getMax().x + dx, box.getMax().x) + 0.1f;
        float maxY = Math.max(box.getMax().y + dy, box.getMax().y) + 0.1f;
        float maxZ = Math.max(box.getMax().z + dz, box.getMax().z) + 0.1f;

        int startX = (int) Math.floor(minX);
        int startY = (int) Math.floor(minY);
        int startZ = (int) Math.floor(minZ);
        int endX = (int) Math.ceil(maxX);
        int endY = (int) Math.ceil(maxY);
        int endZ = (int) Math.ceil(maxZ);

        for (int bx = startX; bx <= endX; bx++) {
            for (int by = startY; by <= endY; by++) {
                for (int bz = startZ; bz <= endZ; bz++) {
                    BlockType block = world.getBlock(bx, by, bz);
                    if (block != null && block.isSolid()) {
                        colliders.add(AABB.forBlock(bx, by, bz));
                    }
                }
            }
        }

        return colliders;
    }

    /**
     * Check if entity's head is in water.
     */
    protected void updateInWater() {
        if (world == null) {
            inWater = false;
            return;
        }

        int blockX = (int) Math.floor(x);
        int blockY = (int) Math.floor(y + height * 0.5f); // Check at body level
        int blockZ = (int) Math.floor(z);

        BlockType block = world.getBlock(blockX, blockY, blockZ);
        inWater = (block == BlockType.WATER);
    }

    /**
     * Get interpolated X position for rendering.
     */
    public float getRenderX(float partialTick) {
        return prevX + (x - prevX) * partialTick;
    }

    /**
     * Get interpolated Y position for rendering.
     */
    public float getRenderY(float partialTick) {
        return prevY + (y - prevY) * partialTick;
    }

    /**
     * Get interpolated Z position for rendering.
     */
    public float getRenderZ(float partialTick) {
        return prevZ + (z - prevZ) * partialTick;
    }

    /**
     * Get interpolated yaw for rendering.
     */
    public float getRenderYaw(float partialTick) {
        // Handle wrap-around for yaw interpolation
        float diff = yaw - prevYaw;
        if (diff > 180)
            diff -= 360;
        if (diff < -180)
            diff += 360;
        return prevYaw + diff * partialTick;
    }

    /**
     * Get interpolated pitch for rendering.
     */
    public float getRenderPitch(float partialTick) {
        return prevPitch + (pitch - prevPitch) * partialTick;
    }

    /**
     * Get current horizontal speed for animation.
     */
    public float getHorizontalSpeed() {
        return (float) Math.sqrt(motionX * motionX + motionZ * motionZ);
    }

    /**
     * Look at a specific position.
     */
    public void lookAt(float targetX, float targetY, float targetZ) {
        float dx = targetX - x;
        float dy = targetY - (y + height * 0.85f); // Eye level
        float dz = targetZ - z;

        float horizontalDist = (float) Math.sqrt(dx * dx + dz * dz);

        yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        pitch = (float) Math.toDegrees(Math.atan2(dy, horizontalDist));

        // Normalize yaw
        while (yaw < 0)
            yaw += 360;
        while (yaw >= 360)
            yaw -= 360;

        // Clamp pitch
        if (pitch > 90)
            pitch = 90;
        if (pitch < -90)
            pitch = -90;
    }

    /**
     * Mark entity for removal.
     */
    public void remove() {
        this.removed = true;
    }

    /**
     * Check if entity should be removed.
     */
    public boolean isRemoved() {
        return removed;
    }

    /**
     * Get distance to another entity.
     */
    public float distanceTo(Entity other) {
        float dx = other.x - this.x;
        float dy = other.y - this.y;
        float dz = other.z - this.z;
        return (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    /**
     * Get horizontal distance to another entity.
     */
    public float distanceToHorizontal(Entity other) {
        float dx = other.x - this.x;
        float dz = other.z - this.z;
        return (float) Math.sqrt(dx * dx + dz * dz);
    }

    /**
     * Get squared distance to another entity (faster for comparisons).
     */
    public float distanceToSquared(Entity other) {
        float dx = other.x - this.x;
        float dy = other.y - this.y;
        float dz = other.z - this.z;
        return dx * dx + dy * dy + dz * dz;
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

    public float getPrevX() {
        return prevX;
    }

    public float getPrevY() {
        return prevY;
    }

    public float getPrevZ() {
        return prevZ;
    }

    public float getMotionX() {
        return motionX;
    }

    public float getMotionY() {
        return motionY;
    }

    public float getMotionZ() {
        return motionZ;
    }

    public float getYaw() {
        return yaw;
    }

    public float getPitch() {
        return pitch;
    }

    public float getWidth() {
        return width;
    }

    public float getHeight() {
        return height;
    }

    public boolean isOnGround() {
        return onGround;
    }

    public boolean isCollidedHorizontally() {
        return collidedHorizontally;
    }

    public boolean isInWater() {
        return inWater;
    }

    public int getTicksExisted() {
        return ticksExisted;
    }

    public float getDistanceWalked() {
        return distanceWalked;
    }

    public World getWorld() {
        return world;
    }

    // Setters
    public void setMotion(float mx, float my, float mz) {
        this.motionX = mx;
        this.motionY = my;
        this.motionZ = mz;
    }

    public void addMotion(float mx, float my, float mz) {
        this.motionX += mx;
        this.motionY += my;
        this.motionZ += mz;
    }

    public void setYaw(float yaw) {
        this.yaw = yaw;
    }

    public void setPitch(float pitch) {
        this.pitch = pitch;
    }
}
