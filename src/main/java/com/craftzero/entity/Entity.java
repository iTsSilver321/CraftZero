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
    protected boolean inLava;
    protected boolean falling;
    protected float fallStartY;

    // Physics constants
    // Physics constants (Standard Minecraft Values)
    protected static final float GRAVITY = -28.0f;
    protected static final float TERMINAL_VELOCITY = -78.4f;
    protected static final float AIR_RESISTANCE = 0.98f;
    protected static final float GROUND_FRICTION = 0.6f;
    protected static final float COBWEB_HORIZONTAL_DRAG = 0.25f;
    protected static final float COBWEB_VERTICAL_DRAG = 0.05f;
    protected static final float SOUL_SAND_HORIZONTAL_DRAG = 0.4f;
    protected static final float CLIMBABLE_AXIS_MOTION = 0.15f;
    protected static final float CLIMBABLE_WALL_BUMP_MOTION = 0.2f;
    protected static final float COLLISION_EPSILON = 0.0001f;
    public static final float DEFAULT_COLLISION_BORDER_SIZE = 0.1f;

    // State
    protected boolean removed = false;
    protected int ticksExisted = 0;
    private boolean waterParticleStateInitialized;
    private boolean wasInWaterForParticles;

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
        if (!allFinite(x, y, z)) {
            return;
        }
        this.x = x;
        this.y = y;
        this.z = z;
        this.prevX = x;
        this.prevY = y;
        this.prevZ = z;
        this.fallStartY = y;
        this.falling = false;
    }

    public void applyRemotePose(float x, float y, float z, float yaw, float pitch,
            float motionX, float motionY, float motionZ, boolean onGround) {
        if (!allFinite(x, y, z)) {
            return;
        }
        float dx = x - this.x;
        float dz = z - this.z;
        this.prevX = this.x;
        this.prevY = this.y;
        this.prevZ = this.z;
        this.prevYaw = this.yaw;
        this.prevPitch = this.pitch;
        this.prevDistanceWalked = this.distanceWalked;
        this.x = x;
        this.y = y;
        this.z = z;
        setYaw(yaw);
        setPitch(pitch);
        setMotion(motionX, motionY, motionZ);
        this.onGround = onGround;
        this.distanceWalked += Math.sqrt(dx * dx + dz * dz);
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
        float halfWidth = getWidth() / 2.0f;
        return new AABB(
                x - halfWidth, y, z - halfWidth,
                x + halfWidth, y + getHeight(), z + halfWidth);
    }

    /**
     * Release-era ray picking expands entity hit boxes by a small collision border.
     */
    public float getCollisionBorderSize() {
        return DEFAULT_COLLISION_BORDER_SIZE;
    }

    /**
     * Create the AABB at a specific position.
     */
    protected AABB getBoundingBoxAt(float px, float py, float pz) {
        float halfWidth = getWidth() / 2.0f;
        return new AABB(
                px - halfWidth, py, pz - halfWidth,
                px + halfWidth, py + getHeight(), pz + halfWidth);
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

        boolean wasOnGround = onGround;

        // Check water state first
        updateInWater();
        updateWaterEntryParticles();
        updateInLava();
        boolean inCobweb = isTouchingBlock(BlockType.COBWEB);
        boolean touchingSoulSand = isTouchingBlock(BlockType.SOUL_SAND);
        boolean onClimbable = usesClimbablePhysics() && isTouchingClimbableBlock();

        if (inWater) {
            float waterDrag = getWaterHorizontalDrag();
            motionX *= waterDrag;
            motionZ *= waterDrag;
            motionY *= getWaterVerticalDrag();
            if (!onGround) {
                motionY -= getWaterGravityPerTick();
            }
            applyFluidCurrent(true);

        } else if (inLava) {
            motionX *= getLavaHorizontalDrag();
            motionZ *= getLavaHorizontalDrag();
            motionY *= getLavaVerticalDrag();
            if (!onGround) {
                motionY -= getLavaGravityPerTick();
            }
            applyFluidCurrent(false);
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

        if (inCobweb) {
            motionX *= COBWEB_HORIZONTAL_DRAG;
            motionY *= COBWEB_VERTICAL_DRAG;
            motionZ *= COBWEB_HORIZONTAL_DRAG;
        }

        if (onClimbable) {
            motionX = clampFloat(motionX, -CLIMBABLE_AXIS_MOTION, CLIMBABLE_AXIS_MOTION);
            motionZ = clampFloat(motionZ, -CLIMBABLE_AXIS_MOTION, CLIMBABLE_AXIS_MOTION);
            if (motionY < -CLIMBABLE_AXIS_MOTION) {
                motionY = -CLIMBABLE_AXIS_MOTION;
            }
        }

        if (inWater || inLava || inCobweb || onClimbable) {
            fallStartY = y;
            falling = false;
        } else {
            boolean nowFalling = motionY < -0.1f && !onGround;
            if (nowFalling && !falling) {
                fallStartY = y;
            }
            falling = nowFalling;
        }

        // Move with collision (motion is already per-tick, no deltaTime needed)
        moveWithCollision(motionX, motionY, motionZ);
        if (inCobweb) {
            motionX = 0.0f;
            motionY = 0.0f;
            motionZ = 0.0f;
        } else if (onClimbable && collidedHorizontally) {
            motionY = CLIMBABLE_WALL_BUMP_MOTION;
        }
        if (onClimbable) {
            fallStartY = y;
            falling = false;
        }

        if (onGround && !wasOnGround) {
            float fallDistance = Math.max(0.0f, fallStartY - y);
            onLanded(fallDistance);
            fallStartY = y;
            falling = false;
        } else if (onGround) {
            fallStartY = y;
            falling = false;
        }

        // Ground friction (slipperiness - Minecraft uses 0.6 for dirt/stone)
        if (onGround) {
            float friction = getGroundFriction();
            motionX *= friction;
            motionZ *= friction;
        }
        if (touchingSoulSand || isTouchingBlock(BlockType.SOUL_SAND)) {
            motionX *= SOUL_SAND_HORIZONTAL_DRAG;
            motionZ *= SOUL_SAND_HORIZONTAL_DRAG;
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

        float originalDx = dx;
        float originalDy = dy;
        float originalDz = dz;
        boolean wasOnGround = onGround;

        AABB startBox = getBoundingBox();
        List<AABB> colliders = getCollidingBlockBoxes(startBox, dx, dy, dz);
        MovementClip clipped = clipMovement(startBox, dx, dy, dz, colliders);
        boolean flatHorizontalCollision = horizontalCollision(originalDx, originalDz, clipped);
        boolean stepped = false;
        float stepHeight = getStepHeight();

        if (flatHorizontalCollision && wasOnGround && stepHeight > 0.0f && !inWater && !inLava) {
            MovementClip steppedClip = clipStepMovement(startBox, dx, dy, dz, stepHeight);
            if (steppedClip != null
                    && steppedClip.horizontalDistanceSq() > clipped.horizontalDistanceSq() + COLLISION_EPSILON) {
                clipped = steppedClip;
                stepped = true;
            }
        }

        x = (clipped.box().getMin().x + clipped.box().getMax().x) * 0.5f;
        y = clipped.box().getMin().y;
        z = (clipped.box().getMin().z + clipped.box().getMax().z) * 0.5f;

        collidedHorizontally = horizontalCollision(originalDx, originalDz, clipped);
        collidedVertically = Math.abs(originalDy - clipped.dy()) > COLLISION_EPSILON;
        onGround = (collidedVertically && originalDy < 0) || stepped;

        if (Math.abs(originalDx - clipped.dx()) > COLLISION_EPSILON) {
            motionX = 0.0f;
        }
        if (collidedVertically) {
            motionY = 0.0f;
        }
        if (Math.abs(originalDz - clipped.dz()) > COLLISION_EPSILON) {
            motionZ = 0.0f;
        }

        // Push out of other entities
        pushOutOfEntities();
    }

    private MovementClip clipMovement(AABB startBox, float dx, float dy, float dz, List<AABB> colliders) {
        AABB box = copyBox(startBox);
        for (AABB blockBox : colliders) {
            dy = box.clipYCollide(blockBox, dy);
        }
        box.move(0, dy, 0);

        for (AABB blockBox : colliders) {
            dx = box.clipXCollide(blockBox, dx);
        }
        box.move(dx, 0, 0);

        for (AABB blockBox : colliders) {
            dz = box.clipZCollide(blockBox, dz);
        }
        box.move(0, 0, dz);
        return new MovementClip(box, dx, dy, dz);
    }

    private MovementClip clipStepMovement(AABB startBox, float dx, float dy, float dz, float stepHeight) {
        List<AABB> colliders = getCollidingBlockBoxes(startBox, dx, dy + stepHeight, dz);
        AABB box = copyBox(startBox);
        float stepUp = stepHeight;
        for (AABB blockBox : colliders) {
            stepUp = box.clipYCollide(blockBox, stepUp);
        }
        if (stepUp <= COLLISION_EPSILON) {
            return null;
        }
        box.move(0, stepUp, 0);

        float steppedDx = dx;
        for (AABB blockBox : colliders) {
            steppedDx = box.clipXCollide(blockBox, steppedDx);
        }
        box.move(steppedDx, 0, 0);

        float steppedDz = dz;
        for (AABB blockBox : colliders) {
            steppedDz = box.clipZCollide(blockBox, steppedDz);
        }
        box.move(0, 0, steppedDz);

        float stepDown = dy - stepUp;
        for (AABB blockBox : colliders) {
            stepDown = box.clipYCollide(blockBox, stepDown);
        }
        box.move(0, stepDown, 0);
        return new MovementClip(box, steppedDx, stepUp + stepDown, steppedDz);
    }

    private boolean horizontalCollision(float originalDx, float originalDz, MovementClip clip) {
        return Math.abs(originalDx - clip.dx()) > COLLISION_EPSILON
                || Math.abs(originalDz - clip.dz()) > COLLISION_EPSILON;
    }

    private static AABB copyBox(AABB box) {
        return new AABB(box.getMin(), box.getMax());
    }

    protected float getStepHeight() {
        return 0.0f;
    }

    private record MovementClip(AABB box, float dx, float dy, float dz) {
        float horizontalDistanceSq() {
            return dx * dx + dz * dz;
        }
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
                float dist = Math.max(Math.abs(dx), Math.abs(dz));

                if (dist >= 0.01f) {
                    dist = (float) Math.sqrt(dist);
                    dx /= dist;
                    dz /= dist;

                    float pushStrength = Math.min(1.0f, 1.0f / dist) * 0.05f;

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
                    colliders.addAll(world.getCollisionBoxesIfLoaded(bx, by, bz));
                }
            }
        }
        colliders.addAll(world.getMovingPistonCollisionBoxes(
                new AABB(minX, minY, minZ, maxX, maxY, maxZ)));

        return colliders;
    }

    protected boolean isTouchingBlock(BlockType target) {
        if (world == null || target == null) {
            return false;
        }
        AABB box = getBoundingBox();
        int minX = (int) Math.floor(box.getMin().x);
        int minY = (int) Math.floor(box.getMin().y);
        int minZ = (int) Math.floor(box.getMin().z);
        int maxX = (int) Math.floor(box.getMax().x - 0.0001f);
        int maxY = (int) Math.floor(box.getMax().y - 0.0001f);
        int maxZ = (int) Math.floor(box.getMax().z - 0.0001f);
        for (int bx = minX; bx <= maxX; bx++) {
            for (int by = minY; by <= maxY; by++) {
                for (int bz = minZ; bz <= maxZ; bz++) {
                    if (world.getBlockIfLoaded(bx, by, bz, BlockType.AIR) == target) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    protected boolean usesClimbablePhysics() {
        return false;
    }

    protected boolean isTouchingClimbableBlock() {
        if (world == null) {
            return false;
        }
        BlockType type = world.getBlockIfLoaded((int) Math.floor(x),
                (int) Math.floor(getBoundingBox().getMin().y),
                (int) Math.floor(z), BlockType.AIR);
        return type == BlockType.LADDER || type == BlockType.VINES;
    }

    private static float clampFloat(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    protected void onLanded(float fallDistance) {
    }

    protected float getGroundFriction() {
        BlockType below = getBlockBelowFeet();
        if (below == BlockType.ICE) {
            return 0.98f;
        }
        return GROUND_FRICTION;
    }

    protected BlockType getBlockBelowFeet() {
        if (world == null) {
            return BlockType.AIR;
        }
        return world.getBlockIfLoaded((int) Math.floor(x), (int) Math.floor(y - 0.0001f),
                (int) Math.floor(z), BlockType.AIR);
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
        int blockY = (int) Math.floor(y + getHeight() * 0.1f); // Check at feet level for natural water exit
        int blockZ = (int) Math.floor(z);

        BlockType block = world.getBlockIfLoaded(blockX, blockY, blockZ, BlockType.AIR);
        inWater = block.isWater();
    }

    protected void updateWaterEntryParticles() {
        if (world == null) {
            waterParticleStateInitialized = false;
            wasInWaterForParticles = false;
            return;
        }
        if (!waterParticleStateInitialized) {
            waterParticleStateInitialized = true;
            wasInWaterForParticles = inWater;
            return;
        }
        if (inWater && !wasInWaterForParticles) {
            world.spawnEntityWaterEntryParticles(x, y, z, width, motionX, motionY, motionZ);
        }
        wasInWaterForParticles = inWater;
    }

    protected void updateInLava() {
        if (world == null) {
            inLava = false;
            return;
        }

        int blockX = (int) Math.floor(x);
        int blockY = (int) Math.floor(y + getHeight() * 0.1f);
        int blockZ = (int) Math.floor(z);

        BlockType block = world.getBlockIfLoaded(blockX, blockY, blockZ, BlockType.AIR);
        inLava = block.isLava();
    }

    private void applyFluidCurrent(boolean water) {
        Vector3f current = world.getFluidFlowVector(getBoundingBox(), water);
        if (current.lengthSquared() <= 0.000001f) {
            return;
        }
        motionX += current.x * World.FLUID_CURRENT_PUSH_PER_TICK;
        motionY += current.y * World.FLUID_CURRENT_PUSH_PER_TICK;
        motionZ += current.z * World.FLUID_CURRENT_PUSH_PER_TICK;
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
        if (!allFinite(targetX, targetY, targetZ, x, y, z)) {
            return;
        }
        float dx = targetX - x;
        float dy = targetY - (y + getHeight() * 0.85f); // Eye level
        float dz = targetZ - z;

        float horizontalDist = (float) Math.sqrt(dx * dx + dz * dz);

        setYaw((float) Math.toDegrees(Math.atan2(-dx, dz)));
        setPitch((float) Math.toDegrees(Math.atan2(dy, horizontalDist)));
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

    public World getWorld() {
        return world;
    }

    public boolean isCollidedHorizontally() {
        return collidedHorizontally;
    }

    public boolean isInWater() {
        return inWater;
    }

    public boolean isInLava() {
        return inLava;
    }

    public int getTicksExisted() {
        return ticksExisted;
    }

    // Getters

    public boolean isOnGround() {
        return onGround;
    }

    public float getFallStartY() {
        return fallStartY;
    }

    public boolean isFalling() {
        return falling;
    }

    public void restoreSavedPhysicsState(boolean onGround, float fallStartY, boolean falling) {
        if (!Float.isFinite(fallStartY)) {
            return;
        }
        this.onGround = onGround;
        this.fallStartY = fallStartY;
        this.falling = falling && !onGround;
    }

    // Setters
    public void setMotion(float mx, float my, float mz) {
        this.motionX = finiteMotion(mx);
        this.motionY = finiteMotion(my);
        this.motionZ = finiteMotion(mz);
    }

    public void setTicksExisted(int ticksExisted) {
        this.ticksExisted = Math.max(0, ticksExisted);
    }

    public void addMotion(float mx, float my, float mz) {
        this.motionX = finiteMotion(this.motionX + mx);
        this.motionY = finiteMotion(this.motionY + my);
        this.motionZ = finiteMotion(this.motionZ + mz);
    }

    private static float finiteMotion(float value) {
        return Float.isFinite(value) ? value : 0.0f;
    }

    private static boolean allFinite(float... values) {
        if (values == null) {
            return false;
        }
        for (float value : values) {
            if (!Float.isFinite(value)) {
                return false;
            }
        }
        return true;
    }

    public void setYaw(float yaw) {
        this.yaw = normalizeYaw(yaw);
    }

    public void setPitch(float pitch) {
        this.pitch = clampPitch(pitch);
    }

    private static float normalizeYaw(float yaw) {
        if (!Float.isFinite(yaw)) {
            return 0.0f;
        }
        float normalized = yaw % 360.0f;
        return normalized < 0.0f ? normalized + 360.0f : normalized;
    }

    private static float clampPitch(float pitch) {
        if (!Float.isFinite(pitch)) {
            return 0.0f;
        }
        return Math.max(-90.0f, Math.min(90.0f, pitch));
    }

    // Physics hooks for subclasses to override (e.g. for knockback effects)

    protected float getGravityPerTick() {
        return 0.08f; // Standard Minecraft gravity
    }

    protected float getAirResistance() {
        return 0.98f; // Standard Minecraft air drag
    }

    protected float getWaterGravityPerTick() {
        return 0.02f;
    }

    protected float getWaterHorizontalDrag() {
        return 0.8f;
    }

    protected float getWaterVerticalDrag() {
        return 0.8f;
    }

    protected float getLavaGravityPerTick() {
        return 0.02f;
    }

    protected float getLavaHorizontalDrag() {
        return 0.5f;
    }

    protected float getLavaVerticalDrag() {
        return 0.5f;
    }
}
