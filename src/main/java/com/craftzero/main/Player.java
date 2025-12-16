package com.craftzero.main;

import com.craftzero.engine.Input;
import com.craftzero.entity.DroppedItem;
import com.craftzero.graphics.Camera;
import com.craftzero.physics.AABB;
import com.craftzero.physics.Raycast;
import com.craftzero.world.BlockType;

import com.craftzero.world.World;
import org.joml.Vector3f;
import org.joml.Vector3i;

import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.glfw.GLFW.*;

/**
 * Player class handling movement, physics, and block interaction.
 */
public class Player {

    // Player dimensions (Minecraft standard)
    private static final float WIDTH = 0.6f;
    private static final float HEIGHT = 1.8f;
    private static final float EYE_HEIGHT = 1.62f;

    // Physics constants
    private static final float GRAVITY = -32.0f;
    private static final float JUMP_VELOCITY = 9.0f;
    private static final float WALK_SPEED = 4.317f;
    private static final float SPRINT_SPEED = 5.612f;
    private static final float SNEAK_SPEED = 1.3f;
    private static final float FRICTION = 0.91f;
    private static final float AIR_FRICTION = 0.98f;
    private static final float ACCELERATION = 0.1f;

    // Sprint double-tap detection
    private static final float DOUBLE_TAP_TIME = 0.3f; // 300ms window for double-tap

    // Mouse sensitivity
    private static final float MOUSE_SENSITIVITY = 0.15f;

    // Block interaction
    private static final float REACH_DISTANCE = 5.0f;
    private static final float BREAK_COOLDOWN = 0.25f;
    private static final float PLACE_COOLDOWN = 0.25f;

    private Vector3f position;
    private Vector3f velocity;
    private Camera camera;
    private AABB boundingBox;

    private boolean onGround;
    private boolean sprinting;
    private boolean sneaking;
    private boolean flying; // Creative mode flight

    // Double-tap W sprint detection
    private float lastWPressTime;
    private boolean wWasReleased;

    private float breakCooldown;
    private float placeCooldown;

    // Currently selected block type for placement
    // Replaced by Inventory system
    // private BlockType selectedBlock = BlockType.COBBLESTONE;

    // Inventory system
    private com.craftzero.inventory.Inventory inventory;

    // Target block (for highlighting)
    private Raycast.RaycastResult targetBlock;

    // Block breaking progress
    private Vector3i breakingBlockPos; // Position of block currently being mined
    private float breakProgress; // 0.0 to 1.0 progress
    private BlockType currentBreakingBlock; // Block type being broken

    // Survival stats
    private PlayerStats stats;
    private float fallStartY; // Y position when started falling (for fall damage)
    private boolean wasFalling; // Track if player was falling last frame
    private boolean dropItemFromHand; // Q key drop flag
    private boolean wantsCraftingTable; // Flag for opening crafting table

    public Player(float x, float y, float z) {
        this.position = new Vector3f(x, y, z);
        this.velocity = new Vector3f();
        this.camera = new Camera(new Vector3f(x, y + EYE_HEIGHT, z));
        this.boundingBox = createBoundingBox();
        this.onGround = false;
        this.sprinting = false;
        this.sneaking = false;
        this.flying = false;
        this.breakCooldown = 0;
        this.placeCooldown = 0;
        this.lastWPressTime = -1f;
        this.wWasReleased = true;
        this.breakingBlockPos = null;
        this.breakProgress = 0f;
        this.currentBreakingBlock = null;
        this.stats = new PlayerStats();
        this.inventory = new com.craftzero.inventory.Inventory();
        this.fallStartY = y;
        this.wasFalling = false;
        this.dropItemFromHand = false;
    }

    private AABB createBoundingBox() {
        float halfWidth = WIDTH / 2;
        return new AABB(
                position.x - halfWidth, position.y, position.z - halfWidth,
                position.x + halfWidth, position.y + HEIGHT, position.z + halfWidth);
    }

    /**
     * Handle player input.
     */
    public void handleInput(float deltaTime) {
        // Track elapsed time for double-tap detection
        if (lastWPressTime >= 0) {
            lastWPressTime += deltaTime;
        }

        // Mouse look
        if (Input.isCursorLocked()) {
            float deltaX = (float) Input.getDeltaX() * MOUSE_SENSITIVITY;
            float deltaY = (float) Input.getDeltaY() * MOUSE_SENSITIVITY;
            camera.rotate(deltaX, deltaY);
        }

        // Movement input
        float forward = 0, strafe = 0;

        if (Input.isKeyDown(GLFW_KEY_W))
            forward += 1;
        if (Input.isKeyDown(GLFW_KEY_S))
            forward -= 1;
        if (Input.isKeyDown(GLFW_KEY_A))
            strafe -= 1;
        if (Input.isKeyDown(GLFW_KEY_D))
            strafe += 1;

        // Sneaking (Shift key) - not while flying
        sneaking = Input.isKeyDown(GLFW_KEY_LEFT_SHIFT) && !flying;

        // Sprint detection: Ctrl held OR double-tap W
        // Double-tap W: If W is pressed within DOUBLE_TAP_TIME of last W press, start
        // sprinting
        if (Input.isKeyPressed(GLFW_KEY_W)) {
            if (wWasReleased && lastWPressTime >= 0 && lastWPressTime < DOUBLE_TAP_TIME) {
                // Double-tap detected!
                sprinting = true;
            }
            lastWPressTime = 0; // Reset timer on W press
            wWasReleased = false;
        }

        // Track W release for double-tap detection
        if (Input.isKeyReleased(GLFW_KEY_W)) {
            wWasReleased = true;
        }

        // Ctrl also triggers sprint while moving forward
        if (Input.isKeyDown(GLFW_KEY_LEFT_CONTROL) && forward > 0) {
            sprinting = true;
        }

        // Stop sprinting if: moving backward, sneaking, or not moving forward
        if (forward <= 0 || sneaking) {
            sprinting = false;
        }

        // Clear double-tap timer if it expires
        if (lastWPressTime > DOUBLE_TAP_TIME) {
            lastWPressTime = -1;
        }

        // Flying toggle (F key)
        if (Input.isKeyPressed(GLFW_KEY_F)) {
            flying = !flying;
            if (flying) {
                velocity.y = 0;
            }
        }

        // Calculate movement speed based on state
        float speed;
        if (sneaking) {
            speed = SNEAK_SPEED;
        } else if (sprinting) {
            speed = SPRINT_SPEED;
        } else {
            speed = WALK_SPEED;
        }

        // Get camera direction for movement
        float yawRad = (float) Math.toRadians(camera.getYaw());
        float sinYaw = (float) Math.sin(yawRad);
        float cosYaw = (float) Math.cos(yawRad);

        // Calculate movement direction
        float moveX = (forward * sinYaw + strafe * cosYaw) * speed * ACCELERATION;
        float moveZ = (-forward * cosYaw + strafe * sinYaw) * speed * ACCELERATION;

        // Apply movement (normalized to 60 FPS)
        float frameScale = deltaTime * 60.0f;
        velocity.x += moveX * frameScale;
        velocity.z += moveZ * frameScale;

        // Flying controls
        if (flying) {
            if (Input.isKeyDown(GLFW_KEY_SPACE)) {
                velocity.y = speed;
            } else if (Input.isKeyDown(GLFW_KEY_LEFT_SHIFT)) {
                velocity.y = -speed;
            } else {
                velocity.y *= 0.5f;
            }
        } else {
            // Normal jump
            if (Input.isKeyPressed(GLFW_KEY_SPACE) && onGround) {
                velocity.y = JUMP_VELOCITY;
                onGround = false;
                stats.onJump(); // Drain hunger from jumping
            }
        }

        // Hotbar Scrolling
        float scrollY = (float) Input.getScrollY();
        if (scrollY != 0) {
            int current = inventory.getSelectedSlot();
            if (scrollY > 0) {
                current = (current - 1 + 9) % 9; // Scroll Up -> Previous Slot
            } else {
                current = (current + 1) % 9; // Scroll Down -> Next Slot
            }
            inventory.setSelectedSlot(current);
        }

        // Block type selection (number keys)
        if (Input.isKeyPressed(GLFW_KEY_1))
            inventory.setSelectedSlot(0);
        if (Input.isKeyPressed(GLFW_KEY_2))
            inventory.setSelectedSlot(1);
        if (Input.isKeyPressed(GLFW_KEY_3))
            inventory.setSelectedSlot(2);
        if (Input.isKeyPressed(GLFW_KEY_4))
            inventory.setSelectedSlot(3);
        if (Input.isKeyPressed(GLFW_KEY_5))
            inventory.setSelectedSlot(4);
        if (Input.isKeyPressed(GLFW_KEY_6))
            inventory.setSelectedSlot(5);
        if (Input.isKeyPressed(GLFW_KEY_7))
            inventory.setSelectedSlot(6);
        if (Input.isKeyPressed(GLFW_KEY_8))
            inventory.setSelectedSlot(7);
        if (Input.isKeyPressed(GLFW_KEY_9))
            inventory.setSelectedSlot(8);

        // Q key to drop one item from selected slot
        if (Input.isKeyPressed(GLFW_KEY_Q)) {
            dropItemFromHand = true; // Flag for Main to handle with world reference
        }

        // Update cooldowns
        if (breakCooldown > 0)
            breakCooldown -= deltaTime;
        if (placeCooldown > 0)
            placeCooldown -= deltaTime;
    }

    /**
     * Handle block breaking and placing.
     * 
     * @param world     the world
     * @param deltaTime time since last frame for progress calculation
     */

    public void handleBlockInteraction(World world, float deltaTime) {
        // Update target block
        targetBlock = Raycast.cast(world, camera.getPosition(), camera.getForward(), REACH_DISTANCE);

        // Handle block breaking (left click held down)
        if (Input.isButtonDown(GLFW_MOUSE_BUTTON_LEFT)) {
            if (targetBlock.hit) {
                Vector3i currentTarget = targetBlock.blockPos;
                BlockType targetType = world.getBlock(currentTarget.x, currentTarget.y, currentTarget.z);

                // Check if we're still mining the same block
                boolean sameBlock = breakingBlockPos != null &&
                        breakingBlockPos.x == currentTarget.x &&
                        breakingBlockPos.y == currentTarget.y &&
                        breakingBlockPos.z == currentTarget.z;

                if (!sameBlock) {
                    // Started mining a new block - reset progress
                    breakingBlockPos = new Vector3i(currentTarget);
                    breakProgress = 0f;
                    currentBreakingBlock = targetType;
                }

                // Check if block is breakable
                if (targetType.isBreakable()) {
                    float hardness = targetType.getHardness();

                    // Get held tool and calculate speed multiplier
                    com.craftzero.inventory.ItemStack heldItem = inventory.getItemInHand();
                    com.craftzero.inventory.ToolType toolType = com.craftzero.inventory.ToolType.NONE;
                    float speedMultiplier = 1.0f;

                    if (heldItem != null && heldItem.isTool()) {
                        toolType = heldItem.getType().getToolType();
                        // Check if tool is effective against this block
                        if (toolType.isEffectiveAgainst(targetType.getPreferredTool())) {
                            speedMultiplier = toolType.getSpeedMultiplier();
                        }
                    }

                    // Calculate progress increment (1/hardness per second, modified by tool speed)
                    float progressIncrement = (deltaTime * speedMultiplier) / hardness;
                    breakProgress += progressIncrement;

                    // Block is broken when progress reaches 1.0
                    if (breakProgress >= 1.0f) {
                        // Check harvest level - only drop if tool level is sufficient
                        boolean canHarvest = toolType.getMiningLevel() >= targetType.getHarvestLevel();

                        // Spawn dropped item at block center (leaves don't drop, and items require
                        // proper tool)
                        if (targetType != BlockType.LEAVES && canHarvest) {
                            world.spawnDroppedItem(
                                    currentTarget.x + 0.5f,
                                    currentTarget.y + 0.5f,
                                    currentTarget.z + 0.5f,
                                    targetType.getDroppedItem(), 1);
                        }

                        world.setBlock(currentTarget.x, currentTarget.y, currentTarget.z, BlockType.AIR);

                        // Consume tool durability
                        if (heldItem != null && heldItem.isTool()) {
                            boolean toolBroke = heldItem.useDurability();
                            if (toolBroke) {
                                // Tool broke - remove from inventory
                                inventory.getHotbar()[inventory.getSelectedSlot()] = null;
                            }
                        }

                        // Reset breaking state
                        breakingBlockPos = null;
                        breakProgress = 0f;
                        currentBreakingBlock = null;

                        // Small cooldown to prevent immediately starting to break next block
                        breakCooldown = 0.1f;
                    }
                }
            } else {
                // Not looking at a block - reset progress
                resetBreakingProgress();
            }
        } else {
            // Button released - reset progress
            resetBreakingProgress();
        }

        // Update break cooldown
        if (breakCooldown > 0) {
            breakCooldown -= deltaTime;
        }

        // Flag for opening crafting table
        wantsCraftingTable = false;

        // Place block (right click)
        if (Input.isButtonDown(GLFW_MOUSE_BUTTON_RIGHT) && placeCooldown <= 0) {
            if (targetBlock.hit) {
                // Check if clicking on a crafting table - open it instead of placing
                BlockType clickedBlock = world.getBlock(
                        targetBlock.blockPos.x,
                        targetBlock.blockPos.y,
                        targetBlock.blockPos.z);

                if (clickedBlock == BlockType.CRAFTING_TABLE) {
                    wantsCraftingTable = true;
                    placeCooldown = PLACE_COOLDOWN;
                } else if (targetBlock.previousBlockPos != null) {
                    Vector3i placePos = targetBlock.previousBlockPos;

                    // Check if placement would intersect with player
                    AABB blockBox = AABB.forBlock(placePos.x, placePos.y, placePos.z);
                    if (!blockBox.intersects(boundingBox)) {
                        // Use inventory item
                        com.craftzero.inventory.ItemStack stack = inventory.getItemInHand();
                        if (stack != null && !stack.isEmpty()) {
                            // Only place if it's a solid block (not items like STICK)
                            if (stack.getType().isSolid()) {
                                world.setBlock(placePos.x, placePos.y, placePos.z, stack.getType());
                                stack.remove(1);

                                // Clear slot if empty
                                if (stack.isEmpty()) {
                                    inventory.getHotbar()[inventory.getSelectedSlot()] = null;
                                }

                                placeCooldown = PLACE_COOLDOWN;
                            }
                        }
                    }
                }
            }
        }

        // Update place cooldown
        if (placeCooldown > 0) {
            placeCooldown -= deltaTime;
        }
    }

    /**
     * Reset block breaking progress.
     */
    private void resetBreakingProgress() {
        breakingBlockPos = null;
        breakProgress = 0f;
        currentBreakingBlock = null;
    }

    /**
     * Update physics.
     */
    public void update(float deltaTime, World world) {
        // Track falling for fall damage
        boolean isFalling = velocity.y < -0.1f && !onGround && !flying;

        if (isFalling && !wasFalling) {
            // Just started falling - record start position
            fallStartY = position.y;
        }
        wasFalling = isFalling;

        // Apply gravity (if not flying)
        if (!flying) {
            velocity.y += GRAVITY * deltaTime;
        }

        // Apply friction
        float friction = onGround ? FRICTION : AIR_FRICTION;
        velocity.x *= friction;
        velocity.z *= friction;

        // Clamp velocity
        float maxHorizontal = sprinting ? SPRINT_SPEED : WALK_SPEED;
        float horizontalSpeed = (float) Math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z);
        if (horizontalSpeed > maxHorizontal) {
            velocity.x = (velocity.x / horizontalSpeed) * maxHorizontal;
            velocity.z = (velocity.z / horizontalSpeed) * maxHorizontal;
        }

        // Terminal velocity
        if (velocity.y < -78.4f) {
            velocity.y = -78.4f;
        }

        // Store pre-collision state for fall damage check
        boolean wasOnGround = onGround;

        // Move with collision detection
        moveWithCollision(deltaTime, world);

        // Fall damage calculation (only when landing from a fall, not in fly mode)
        if (onGround && !wasOnGround && !flying) {
            float fallDistance = fallStartY - position.y;
            if (fallDistance > 3.0f) {
                // Minecraft formula: damage = fallDistance - 3
                float damage = fallDistance - 3.0f;
                stats.damage(damage);
            }
            fallStartY = position.y; // Reset fall start
        }

        // Update survival stats
        boolean isMoving = Math.abs(velocity.x) > 0.01f || Math.abs(velocity.z) > 0.01f;
        stats.update(deltaTime, sprinting, isMoving);

        // Collect nearby dropped items (only if inventory has space)
        java.util.List<DroppedItem> collected = world.collectNearbyItems(
                position.x, position.y + 0.9f, position.z, deltaTime, this);
        for (DroppedItem item : collected) {
            addToInventory(item.getBlockType(), item.getCount());
        }

        // Update camera position
        camera.setPosition(position.x, position.y + EYE_HEIGHT, position.z);
    }

    /**
     * Move player with collision detection.
     * Uses Minecraft-style independent axis resolution: process Y, then X, then Z.
     * After each axis move, the bounding box is updated before checking the next
     * axis.
     */
    private void moveWithCollision(float deltaTime, World world) {
        float dx = velocity.x * deltaTime;
        float dy = velocity.y * deltaTime;
        float dz = velocity.z * deltaTime;

        // Store original values to detect collisions
        float originalDx = dx;
        float originalDy = dy;
        float originalDz = dz;

        // Get all potential colliders for the entire movement
        List<AABB> colliders = getCollidingBlocks(world, dx, dy, dz);

        // === STEP 1: Resolve Y axis (gravity/jumping) ===
        for (AABB collider : colliders) {
            dy = boundingBox.clipYCollide(collider, dy);
        }
        boundingBox.move(0, dy, 0);
        position.y += dy;

        // Determine ground state from Y collision
        if (Math.abs(originalDy - dy) > 0.0001f) {
            if (originalDy < 0) {
                onGround = true;
            }
            velocity.y = 0;
        } else {
            onGround = false;
        }

        // === STEP 2: Resolve X axis ===
        for (AABB collider : colliders) {
            dx = boundingBox.clipXCollide(collider, dx);
        }
        boundingBox.move(dx, 0, 0);
        position.x += dx;

        if (Math.abs(originalDx - dx) > 0.0001f) {
            velocity.x = 0;
        }

        // === STEP 3: Resolve Z axis ===
        for (AABB collider : colliders) {
            dz = boundingBox.clipZCollide(collider, dz);
        }
        boundingBox.move(0, 0, dz);
        position.z += dz;

        if (Math.abs(originalDz - dz) > 0.0001f) {
            velocity.z = 0;
        }
    }

    /**
     * Get all solid blocks that could collide with the player's path.
     */
    private List<AABB> getCollidingBlocks(World world, float dx, float dy, float dz) {
        List<AABB> colliders = new ArrayList<>();

        // Expand bounding box by movement
        AABB expanded = boundingBox.expand(0.1f);
        AABB searchBox = new AABB(
                Math.min(expanded.getMin().x, expanded.getMin().x + dx),
                Math.min(expanded.getMin().y, expanded.getMin().y + dy),
                Math.min(expanded.getMin().z, expanded.getMin().z + dz),
                Math.max(expanded.getMax().x, expanded.getMax().x + dx),
                Math.max(expanded.getMax().y, expanded.getMax().y + dy),
                Math.max(expanded.getMax().z, expanded.getMax().z + dz));

        int minX = (int) Math.floor(searchBox.getMin().x);
        int minY = (int) Math.floor(searchBox.getMin().y);
        int minZ = (int) Math.floor(searchBox.getMin().z);
        int maxX = (int) Math.ceil(searchBox.getMax().x);
        int maxY = (int) Math.ceil(searchBox.getMax().y);
        int maxZ = (int) Math.ceil(searchBox.getMax().z);

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    BlockType block = world.getBlock(x, y, z);
                    if (block.isSolid()) {
                        colliders.add(AABB.forBlock(x, y, z));
                    }
                }
            }
        }

        return colliders;
    }

    // Getters

    public Camera getCamera() {
        return camera;
    }

    public Vector3f getPosition() {
        return position;
    }

    public void setPosition(float x, float y, float z) {
        position.set(x, y, z);
        boundingBox = createBoundingBox();
        camera.setPosition(x, y + EYE_HEIGHT, z);
    }

    public boolean isOnGround() {
        return onGround;
    }

    public boolean isFlying() {
        return flying;
    }

    public boolean isSprinting() {
        return sprinting;
    }

    /*
     * Deprecated
     * public BlockType getSelectedBlock() {
     * return BlockType.AIR;
     * }
     */

    public Raycast.RaycastResult getTargetBlock() {
        return targetBlock;
    }

    public Vector3f getVelocity() {
        return velocity;
    }

    public boolean isSneaking() {
        return sneaking;
    }

    /**
     * Get the position of the block currently being mined.
     * 
     * @return block position, or null if not mining
     */
    public Vector3i getBreakingBlockPos() {
        return breakingBlockPos;
    }

    /**
     * Get the current break progress (0.0 to 1.0).
     * 
     * @return break progress
     */
    public float getBreakProgress() {
        return breakProgress;
    }

    /**
     * Check if currently breaking a block.
     * 
     * @return true if breaking
     */
    public boolean isBreaking() {
        return breakingBlockPos != null && breakProgress > 0;
    }

    /**
     * Get player survival stats.
     * 
     * @return the player stats
     */
    public PlayerStats getStats() {
        return stats;
    }

    public com.craftzero.inventory.Inventory getInventory() {
        return inventory;
    }

    /**
     * Add an item to the player's inventory.
     * Tries hotbar first, then main inventory.
     */
    public boolean addToInventory(BlockType type, int count) {
        if (type == null || type == BlockType.AIR || count <= 0) {
            return false;
        }

        // Try to stack with existing items in hotbar
        com.craftzero.inventory.ItemStack[] hotbar = inventory.getHotbar();
        for (int i = 0; i < hotbar.length; i++) {
            if (hotbar[i] != null && hotbar[i].getType() == type) {
                int space = hotbar[i].getMaxStackSize() - hotbar[i].getCount();
                if (space >= count) {
                    hotbar[i].add(count);
                    return true;
                } else if (space > 0) {
                    hotbar[i].add(space);
                    count -= space;
                }
            }
        }

        // Try to stack with existing items in main inventory
        com.craftzero.inventory.ItemStack[] main = inventory.getMainInventory();
        for (int i = 0; i < main.length; i++) {
            if (main[i] != null && main[i].getType() == type) {
                int space = main[i].getMaxStackSize() - main[i].getCount();
                if (space >= count) {
                    main[i].add(count);
                    return true;
                } else if (space > 0) {
                    main[i].add(space);
                    count -= space;
                }
            }
        }

        // Try to find an empty slot in hotbar
        for (int i = 0; i < hotbar.length; i++) {
            if (hotbar[i] == null || hotbar[i].isEmpty()) {
                hotbar[i] = new com.craftzero.inventory.ItemStack(type, count);
                return true;
            }
        }

        // Try to find an empty slot in main inventory
        for (int i = 0; i < main.length; i++) {
            if (main[i] == null || main[i].isEmpty()) {
                main[i] = new com.craftzero.inventory.ItemStack(type, count);
                return true;
            }
        }

        // Inventory full
        return false;
    }

    /**
     * Check if inventory has space for the given item.
     * Does not modify inventory, just checks.
     */
    public boolean canAddToInventory(BlockType type, int count) {
        if (type == null || type == BlockType.AIR || count <= 0) {
            return false;
        }

        int remaining = count;

        // Check existing stacks in hotbar
        com.craftzero.inventory.ItemStack[] hotbar = inventory.getHotbar();
        for (int i = 0; i < hotbar.length && remaining > 0; i++) {
            if (hotbar[i] != null && hotbar[i].getType() == type) {
                int space = hotbar[i].getMaxStackSize() - hotbar[i].getCount();
                remaining -= space;
            }
        }
        if (remaining <= 0)
            return true;

        // Check existing stacks in main inventory
        com.craftzero.inventory.ItemStack[] main = inventory.getMainInventory();
        for (int i = 0; i < main.length && remaining > 0; i++) {
            if (main[i] != null && main[i].getType() == type) {
                int space = main[i].getMaxStackSize() - main[i].getCount();
                remaining -= space;
            }
        }
        if (remaining <= 0)
            return true;

        // Check for empty slots in hotbar
        for (int i = 0; i < hotbar.length; i++) {
            if (hotbar[i] == null || hotbar[i].isEmpty()) {
                return true; // At least one empty slot
            }
        }

        // Check for empty slots in main inventory
        for (int i = 0; i < main.length; i++) {
            if (main[i] == null || main[i].isEmpty()) {
                return true; // At least one empty slot
            }
        }

        return false;
    }

    /**
     * Check if player wants to drop an item (Q key pressed).
     */
    public boolean wantsToDropItem() {
        return dropItemFromHand;
    }

    /**
     * Clear the drop flag after handling.
     */
    public void clearDropFlag() {
        dropItemFromHand = false;
    }

    /**
     * Drop one item from the selected hotbar slot.
     * 
     * @return The block type dropped, or null if slot was empty
     */
    public BlockType dropOneFromHand() {
        com.craftzero.inventory.ItemStack[] hotbar = inventory.getHotbar();
        int slot = inventory.getSelectedSlot();

        if (hotbar[slot] != null && !hotbar[slot].isEmpty()) {
            BlockType type = hotbar[slot].getType();
            hotbar[slot].remove(1);

            if (hotbar[slot].isEmpty()) {
                hotbar[slot] = null;
            }

            return type;
        }

        return null;
    }

    /**
     * Check if player wants to open crafting table (right-clicked on one).
     */
    public boolean wantsCraftingTable() {
        return wantsCraftingTable;
    }
}
