package com.craftzero.entity;

import com.craftzero.physics.AABB;
import com.craftzero.world.BlockType;
import com.craftzero.world.World;
import lombok.Getter;
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
    @Getter
    protected float x, y, z;

    // Previous position for render interpolation (20hz physics -> 60hz display)
    @Getter
    protected float prevX, prevY, prevZ;

    // Velocity
    @Getter
    protected float motionX, motionY, motionZ;

    // Rotation (degrees)
    @Getter
    protected float yaw; // Horizontal rotation (0-360)
    @Getter
    protected float pitch; // Vertical rotation (-90 to 90)
    protected float prevYaw, prevPitch; // For interpolation

    // Dimensions
    @Getter
    protected final float width;
    @Getter
    protected final float height;

    // Collision state
    protected boolean onGround;
    @Getter
    protected boolean collidedHorizontally;
    protected boolean collidedVertically;
    @Getter
    protected boolean inWater;

    // Physics constants
    // Physics constants (Standard Minecraft Values)
    protected static final float GRAVITY = -28.0f;
    protected static final float TERMINAL_VELOCITY = -78.4f;
    protected static final float AIR_RESISTANCE = 0.98f;
    protected static final float GROUND_FRICTION = 0.6f;

    // State
    protected boolean removed = false;
    @Getter
    protected int ticksExisted = 0;

    // Animation tracking
    @Getter
    protected float distanceWalked = 0.0f;
    protected float prevDistanceWalked = 0.0f;

    // Reference to world
    @Getter
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

        // Check water state first
        updateInWater();

        if (inWater) {
            // === WATER PHYSICS ===
            // Reduced gravity (sink slower)
            if (!onGround) {
                motionY -= 0.02f; // Much less than normal 0.08
                if (motionY < -0.8f) {
                    motionY = -0.8f; // Slower terminal velocity in water
                }
            }

            // Water drag (slows movement significantly)
            float waterDrag = 0.8f;
            motionX *= waterDrag;
            motionZ *= waterDrag;
            motionY *= 0.9f; // Slightly less drag on Y for bobbing feel

            // Bobbing effect - subtle buoyancy when in water
            // Keep this weak to avoid glitchy appearance
            float bobbing = (float) Math.sin(ticksExisted * 0.15f) * 0.01f;
            motionY += bobbing;

            // Swim up (try to reach surface)
            BlockType blockAbove = world.getBlock((int) Math.floor(x), (int) Math.floor(y + height + 0.5f),
                    (int) Math.floor(z));
            if (blockAbove != BlockType.WATER) {
                // Near surface - gentle upward force to help exit water
                if (motionY < 0.15f) {
                    motionY += 0.06f; // Surface float
                }
            }

        } else {
            // === NORMAL PHYSICS ===
            // Apply gravity if not on ground
            // 1. Gravity
            if (!onGround) {
                motionY -= getGravityPerTick(); // Use dynamic gravity (standard 0.08)
                if (motionY < -3.92f) {
                    motionY = -3.92f; // Terminal velocity
                }
            }

            // 2. Air Resistance
            float drag = getAirResistance(); // Use dynamic drag (standard 0.98)
            motionX *= drag;
            motionZ *= drag;
            motionY *= 0.98f; // Y drag usually stays standard
        }

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
        int blockY = (int) Math.floor(y + height * 0.1f); // Check at feet level for natural water exit
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

    // Getters (generated by Lombok @Getter on fields)

    public boolean isOnGround() {
        return onGround;
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

    // Physics hooks for subclasses to override (e.g. for knockback effects)

    protected float getGravityPerTick() {
        return 0.08f; // Standard Minecraft gravity
    }

    protected float getAirResistance() {
        return 0.98f; // Standard Minecraft air drag
    }
}
