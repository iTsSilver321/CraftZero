package com.craftzero.entity;

import com.craftzero.combat.DamageSource;
import com.craftzero.inventory.ItemType;
import com.craftzero.inventory.ItemStack;
import com.craftzero.physics.AABB;
import com.craftzero.world.BlockType;
import com.craftzero.world.World;
import org.joml.Vector3f;

import java.util.Random;

/**
 * Represents a dropped item in the world.
 * Features Minecraft-style spinning and bobbing animation.
 */
public class DroppedItem {

    // Constants - Minecraft-like values
    public static final int DEFAULT_PICKUP_DELAY_TICKS = 10;
    public static final int THROWN_PICKUP_DELAY_TICKS = 40;
    public static final int MAX_HEALTH = 5;
    private static final float TICKS_PER_SECOND = 20.0f;
    private static final float GRAVITY = -16.0f;
    private static final float AIR_DRAG = 0.98f;
    private static final float DEFAULT_BLOCK_SLIPPERINESS = 0.6f;
    private static final float ICE_BLOCK_SLIPPERINESS = 0.98f;
    private static final float GROUND_BOUNCE = -0.5f;
    private static final float MOTION_STOP_EPSILON = 0.01f;
    private static final int LAVA_FEEDBACK_INTERVAL_TICKS = 25;
    private static final float INITIAL_VERTICAL_VELOCITY = 4.0f;
    private static final float INITIAL_HORIZONTAL_VELOCITY_RANGE = 2.0f;
    private static final float SPIN_SPEED = 90.0f; // degrees/second (1 rotation per 4 seconds)
    private static final float BOB_SPEED = 2.5f; // radians/second
    private static final float BOB_AMPLITUDE = 0.1f; // blocks (vertical movement range)
    public static final float DESPAWN_TIME_SECONDS = 300.0f; // 5 minutes
    private static final float ATTRACTION_RADIUS = 2.0f; // Start moving toward player
    private static final float PICKUP_RADIUS = 1.0f; // Actually collect item
    private static final float ATTRACTION_SPEED = 15.0f; // Speed when attracted to player

    // Visual
    private static final float SCALE = 0.25f; // Size relative to full block

    // Position
    private float x, y, z;

    // Physics
    private float velocityX, velocityY, velocityZ;
    private boolean onGround;
    private boolean launchInitialized;
    private int entityTicks;
    private float entityTickAccumulator;
    private boolean blockCellInitialized;
    private boolean blockCellChangedLastUpdate;
    private boolean lavaFeedbackIntervalElapsed;
    private int lastBlockX;
    private int lastBlockY;
    private int lastBlockZ;

    // Item data
    private ItemType itemType;
    private int count;
    private int durability;
    private ItemStack stackData;
    private int health = MAX_HEALTH;

    // Animation state
    private float age; // Seconds since spawn
    private float rotation; // Current Y rotation (degrees)
    private float bobPhase; // Phase for sine wave bobbing
    private boolean visualInitialized;
    private int pickupDelayTicks = DEFAULT_PICKUP_DELAY_TICKS;
    private float pickupDelayAccumulator;

    public DroppedItem(float x, float y, float z, ItemType itemType, int count) {
        this(x, y, z, itemType, count, (Random) null);
    }

    public DroppedItem(float x, float y, float z, ItemType itemType, int count, Random random) {
        this(x, y, z, new ItemStack(itemType, count, defaultDurability(itemType)),
                0.0f, INITIAL_VERTICAL_VELOCITY, 0.0f, false, true, random);
    }

    public DroppedItem(float x, float y, float z, ItemStack stack) {
        this(x, y, z, stack, (Random) null);
    }

    public DroppedItem(float x, float y, float z, ItemStack stack, Random random) {
        this(x, y, z, stack, 0.0f, INITIAL_VERTICAL_VELOCITY, 0.0f, true, true, random);
    }

    /**
     * Constructor with initial velocity (for thrown items).
     */
    public DroppedItem(float x, float y, float z, ItemType itemType, int count,
            float velX, float velY, float velZ) {
        this(x, y, z, itemType, count, velX, velY, velZ, null);
    }

    public DroppedItem(float x, float y, float z, ItemType itemType, int count,
            float velX, float velY, float velZ, Random random) {
        this(x, y, z, itemType, count, defaultDurability(itemType), velX, velY, velZ, random);
    }

    public DroppedItem(float x, float y, float z, ItemStack stack, float velX, float velY, float velZ) {
        this(x, y, z, stack, velX, velY, velZ, null);
    }

    public DroppedItem(float x, float y, float z, ItemStack stack, float velX, float velY, float velZ,
            Random random) {
        this(x, y, z, stack, velX, velY, velZ, true, random);
    }

    private DroppedItem(float x, float y, float z, ItemType itemType, int count, int durability,
            float velX, float velY, float velZ, Random random) {
        this(x, y, z, new ItemStack(itemType, count, durability), velX, velY, velZ, false, false, random);
    }

    private DroppedItem(float x, float y, float z, ItemStack stack,
            float velX, float velY, float velZ, boolean copyStack, Random random) {
        this(x, y, z, stack, velX, velY, velZ, copyStack, false, random);
    }

    private DroppedItem(float x, float y, float z, ItemStack stack,
            float velX, float velY, float velZ, boolean copyStack, boolean randomizeLaunch, Random random) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.stackData = copyStack ? stack.copy() : stack;
        this.itemType = this.stackData.getType();
        this.count = this.stackData.getCount();
        this.durability = this.stackData.getDurability();

        // Initial velocity
        this.velocityX = velX;
        this.velocityY = velY;
        this.velocityZ = velZ;
        this.onGround = false;
        this.launchInitialized = !randomizeLaunch;

        if (random != null && randomizeLaunch) {
            initializeLaunch(random);
        }
        if (random != null) {
            initializeVisuals(random);
        }
        this.age = 0;
    }

    public void attachToWorld(World world) {
        if (world == null) {
            return;
        }
        Random random = world.getRandom();
        if (!launchInitialized) {
            initializeLaunch(random);
        }
        if (!visualInitialized) {
            initializeVisuals(random);
        }
    }

    private void initializeLaunch(Random random) {
        velocityX = random.nextFloat() * INITIAL_HORIZONTAL_VELOCITY_RANGE * 2.0f
                - INITIAL_HORIZONTAL_VELOCITY_RANGE;
        velocityY = INITIAL_VERTICAL_VELOCITY;
        velocityZ = random.nextFloat() * INITIAL_HORIZONTAL_VELOCITY_RANGE * 2.0f
                - INITIAL_HORIZONTAL_VELOCITY_RANGE;
        onGround = false;
        launchInitialized = true;
    }

    private void initializeVisuals(Random random) {
        this.rotation = random.nextFloat() * 360.0f;
        this.bobPhase = random.nextFloat() * (float) (Math.PI * 2.0);
        this.visualInitialized = true;
    }

    private static int defaultDurability(ItemType type) {
        return type != null && type.isDamageable() ? type.getMaxDurability() : -1;
    }

    /**
     * Update physics and animation.
     * 
     * @return true if item should be removed (despawned)
     */
    public boolean update(float deltaTime, World world) {
        attachToWorld(world);
        age += deltaTime;
        updatePickupDelay(deltaTime);
        updateEntityTickCadence(deltaTime);

        // Check despawn
        if (age >= DESPAWN_TIME_SECONDS) {
            return true;
        }

        int previousBlockX = blockCellInitialized ? lastBlockX : blockCellX();
        int previousBlockY = blockCellInitialized ? lastBlockY : blockCellY();
        int previousBlockZ = blockCellInitialized ? lastBlockZ : blockCellZ();
        boolean hadPreviousBlockCell = blockCellInitialized;

        applyWaterCurrent(world, deltaTime);

        // Physics - gravity and collision
        if (onGround) {
            int blockX = (int) Math.floor(x);
            int blockZ = (int) Math.floor(z);
            int blockY = (int) Math.floor(y - 0.2f);

            if (blockY >= 0) {
                BlockType below = world.getBlockIfLoaded(blockX, blockY, blockZ, BlockType.AIR);
                if (!below.isSolid()) {
                    onGround = false; // Start falling again
                } else {
                    float groundFriction = groundFrictionFor(below);
                    x += velocityX * deltaTime;
                    z += velocityZ * deltaTime;
                    velocityX *= groundFriction;
                    velocityZ *= groundFriction;
                    if (Math.abs(velocityX) < MOTION_STOP_EPSILON) {
                        velocityX = 0.0f;
                    }
                    if (Math.abs(velocityZ) < MOTION_STOP_EPSILON) {
                        velocityZ = 0.0f;
                    }
                }
            }
        }

        if (!onGround) {
            velocityY += GRAVITY * deltaTime;

            // Apply horizontal velocity with friction
            x += velocityX * deltaTime;
            z += velocityZ * deltaTime;
            velocityX *= AIR_DRAG;
            velocityZ *= AIR_DRAG;

            y += velocityY * deltaTime;
            velocityY *= AIR_DRAG;

            // Simple ground check - find the block the item is trying to move into
            int blockX = (int) Math.floor(x);
            int blockZ = (int) Math.floor(z);
            int blockY = (int) Math.floor(y - 0.1f); // Check slightly below

            // Check block at feet level
            if (velocityY <= 0.0f && blockY >= 0) {
                BlockType below = world.getBlockIfLoaded(blockX, blockY, blockZ, BlockType.AIR);
                if (below.isSolid()) {
                    // Land on top of this block
                    y = blockY + 1.0f + 0.1f; // Slight offset above ground
                    float groundFriction = groundFrictionFor(below);
                    velocityY *= GROUND_BOUNCE;
                    velocityX *= groundFriction;
                    velocityZ *= groundFriction;
                    if (velocityY < MOTION_STOP_EPSILON) {
                        velocityY = 0.0f;
                        onGround = true;
                    } else {
                        onGround = false;
                    }
                }
            }

            // Prevent falling through world
            if (y < 0) {
                y = 1;
                velocityY = 0;
                onGround = true;
            }
        }

        recordBlockCellAfterUpdate(previousBlockX, previousBlockY, previousBlockZ, hadPreviousBlockCell);

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

    private void applyWaterCurrent(World world, float deltaTime) {
        AABB itemBox = new AABB(
                x - SCALE * 0.5f, y, z - SCALE * 0.5f,
                x + SCALE * 0.5f, y + SCALE, z + SCALE * 0.5f);
        Vector3f current = world.getFluidFlowVector(itemBox, true);
        if (current.lengthSquared() <= 0.000001f) {
            return;
        }
        float acceleration = World.FLUID_CURRENT_PUSH_PER_TICK * 20.0f * 20.0f
                * Math.max(0.0f, deltaTime);
        velocityX += current.x * acceleration;
        velocityY += current.y * acceleration;
        velocityZ += current.z * acceleration;
    }

    private void updateEntityTickCadence(float deltaTime) {
        lavaFeedbackIntervalElapsed = false;
        entityTickAccumulator += Math.max(0.0f, deltaTime) * TICKS_PER_SECOND;
        int elapsedTicks = (int) entityTickAccumulator;
        if (elapsedTicks <= 0) {
            return;
        }
        int previousTicks = entityTicks;
        entityTicks += elapsedTicks;
        lavaFeedbackIntervalElapsed = entityTicks / LAVA_FEEDBACK_INTERVAL_TICKS
                != previousTicks / LAVA_FEEDBACK_INTERVAL_TICKS;
        entityTickAccumulator -= elapsedTicks;
    }

    private void recordBlockCellAfterUpdate(int previousBlockX, int previousBlockY, int previousBlockZ,
            boolean hadPreviousBlockCell) {
        int currentBlockX = blockCellX();
        int currentBlockY = blockCellY();
        int currentBlockZ = blockCellZ();
        blockCellChangedLastUpdate = !hadPreviousBlockCell
                || previousBlockX != currentBlockX
                || previousBlockY != currentBlockY
                || previousBlockZ != currentBlockZ;
        blockCellInitialized = true;
        lastBlockX = currentBlockX;
        lastBlockY = currentBlockY;
        lastBlockZ = currentBlockZ;
    }

    private int blockCellX() {
        return (int) Math.floor(x);
    }

    private int blockCellY() {
        return (int) Math.floor(y);
    }

    private int blockCellZ() {
        return (int) Math.floor(z);
    }

    private static float groundFrictionFor(BlockType below) {
        float slipperiness = below == BlockType.ICE ? ICE_BLOCK_SLIPPERINESS : DEFAULT_BLOCK_SLIPPERINESS;
        return slipperiness * AIR_DRAG;
    }

    private void updatePickupDelay(float deltaTime) {
        if (pickupDelayTicks <= 0) {
            pickupDelayAccumulator = 0.0f;
            return;
        }
        pickupDelayAccumulator += Math.max(0.0f, deltaTime) * TICKS_PER_SECOND;
        int elapsedTicks = (int) pickupDelayAccumulator;
        if (elapsedTicks <= 0) {
            return;
        }
        pickupDelayTicks = Math.max(0, pickupDelayTicks - elapsedTicks);
        pickupDelayAccumulator -= elapsedTicks;
    }

    /**
     * Try to attract/collect this item toward a player.
     * 
     * @return true if item was collected
     */
    public boolean tryCollect(float playerX, float playerY, float playerZ, float deltaTime) {
        // Can't pickup during delay
        if (!canPickup()) {
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

    public void moveBy(float dx, float dy, float dz) {
        x += dx;
        y += dy;
        z += dz;
        velocityX += dx * 0.2f;
        velocityY += dy * 0.2f;
        velocityZ += dz * 0.2f;
        onGround = false;
    }

    public void addVelocity(float dx, float dy, float dz) {
        velocityX += dx;
        velocityY += dy;
        velocityZ += dz;
        onGround = false;
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

    public float getBobPhase() {
        return bobPhase;
    }

    public float getVelocityX() {
        return velocityX;
    }

    public float getVelocityY() {
        return velocityY;
    }

    public float getVelocityZ() {
        return velocityZ;
    }

    public boolean isOnGround() {
        return onGround;
    }

    public void setVelocity(float velocityX, float velocityY, float velocityZ) {
        this.velocityX = velocityX;
        this.velocityY = velocityY;
        this.velocityZ = velocityZ;
    }

    public boolean shouldApplyLavaFeedback() {
        return shouldRunItemCellWork();
    }

    public boolean shouldRunItemCellWork() {
        return blockCellChangedLastUpdate || lavaFeedbackIntervalElapsed;
    }

    public void setOnGround(boolean onGround) {
        this.onGround = onGround;
    }

    public void setAnimationState(float rotation, float bobPhase) {
        this.rotation = rotation;
        this.bobPhase = bobPhase;
        this.visualInitialized = true;
    }

    public float getScale() {
        return SCALE;
    }

    public ItemType getItemType() {
        return itemType;
    }

    public int getCount() {
        return count;
    }

    public int getDurability() {
        return durability;
    }

    public float getAge() {
        return age;
    }

    public int getHealth() {
        return health;
    }

    public void setHealth(int health) {
        this.health = Math.max(1, health);
    }

    public boolean isDestroyed() {
        return health <= 0;
    }

    public boolean damage(float amount, DamageSource source) {
        if (isDestroyed() || !Float.isFinite(amount) || amount <= 0.0f) {
            return false;
        }
        health = (int) (health - amount);
        if (health <= 0) {
            health = 0;
        }
        return true;
    }

    public void setAge(float age) {
        this.age = Math.max(0, age);
        float totalTicks = this.age * TICKS_PER_SECOND;
        entityTicks = (int) Math.floor(totalTicks);
        entityTickAccumulator = totalTicks - entityTicks;
        if (pickupDelayTicks == DEFAULT_PICKUP_DELAY_TICKS && pickupDelayAccumulator == 0.0f) {
            pickupDelayTicks = remainingDefaultPickupDelayTicks(this.age);
        }
    }

    private static int remainingDefaultPickupDelayTicks(float ageSeconds) {
        int elapsedTicks = (int) Math.floor(Math.max(0.0f, ageSeconds) * TICKS_PER_SECOND);
        return Math.max(0, DEFAULT_PICKUP_DELAY_TICKS - elapsedTicks);
    }

    public boolean canPickup() {
        return pickupDelayTicks <= 0;
    }

    public int getPickupDelayTicks() {
        return pickupDelayTicks;
    }

    public float getPickupDelayAccumulator() {
        return pickupDelayAccumulator;
    }

    public void setPickupDelayTicks(int pickupDelayTicks) {
        this.pickupDelayTicks = Math.max(0, pickupDelayTicks);
        this.pickupDelayAccumulator = 0.0f;
    }

    public void restorePickupDelayState(int pickupDelayTicks, float pickupDelayAccumulator) {
        this.pickupDelayTicks = Math.max(0, pickupDelayTicks);
        this.pickupDelayAccumulator = Float.isFinite(pickupDelayAccumulator)
                ? Math.max(0.0f, Math.min(0.9999f, pickupDelayAccumulator))
                : 0.0f;
        if (this.pickupDelayTicks <= 0) {
            this.pickupDelayAccumulator = 0.0f;
        }
    }

    public void setCount(int count) {
        this.count = count;
        if (stackData != null) {
            stackData.setCount(count);
        }
    }

    public int getMaxStackSize() {
        return toItemStack().getMaxStackSize();
    }

    public ItemStack toItemStack() {
        if (stackData == null) {
            stackData = new ItemStack(itemType, count, durability);
        }
        stackData.setCount(count);
        stackData.setDurability(durability);
        return stackData.copy();
    }

    public ItemStack getStack() {
        return toItemStack();
    }

    /**
     * Check if this item can merge with another of the same type.
     */
    public boolean canMergeWith(DroppedItem other) {
        return other != null
                && this.toItemStack().canMergeWith(other.toItemStack())
                && this.count < getMaxStackSize()
                && other.count > 0;
    }

    /**
     * Merge another item into this one.
     */
    public int mergeWith(DroppedItem other) {
        if (!canMergeWith(other)) {
            return 0;
        }
        int moved = Math.min(getMaxStackSize() - count, other.count);
        setCount(count + moved);
        other.setCount(other.count - moved);
        if (moved > 0) {
            pickupDelayTicks = Math.max(pickupDelayTicks, other.pickupDelayTicks);
            age = Math.min(age, other.age);
        }
        return moved;
    }

    public DroppedItem splitOff(int amount) {
        int moved = Math.min(Math.max(0, amount), count);
        if (moved <= 0) {
            return null;
        }
        ItemStack split = toItemStack();
        split.setCount(moved);
        setCount(count - moved);
        DroppedItem item = new DroppedItem(x, y, z, split, velocityX, velocityY, velocityZ);
        item.setAge(age);
        item.pickupDelayTicks = pickupDelayTicks;
        item.pickupDelayAccumulator = pickupDelayAccumulator;
        item.health = health;
        item.setOnGround(onGround);
        item.setAnimationState(rotation, bobPhase);
        item.entityTicks = entityTicks;
        item.entityTickAccumulator = entityTickAccumulator;
        item.blockCellInitialized = blockCellInitialized;
        item.blockCellChangedLastUpdate = blockCellChangedLastUpdate;
        item.lavaFeedbackIntervalElapsed = lavaFeedbackIntervalElapsed;
        item.lastBlockX = lastBlockX;
        item.lastBlockY = lastBlockY;
        item.lastBlockZ = lastBlockZ;
        return item;
    }
}
