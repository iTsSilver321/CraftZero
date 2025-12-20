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
    // Physics constants
    private static final float GRAVITY = -28.0f; // Reduced from -32.0f for floatier feel
    private static final float JUMP_VELOCITY = 9.0f; // Reduced from 9.5f
    private static final float WALK_SPEED = 4.0f; // Reduced from 4.317f
    private static final float SPRINT_SPEED = 5.2f; // Reduced from 5.612f
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
    private Vector3f prevPosition; // Previous position for render interpolation
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

    // Water State (Promoted to fields for access in handleInput)
    private boolean inWater;
    private boolean headInWater;
    private float surfaceBobbingTimer; // Timer to disable swimming at surface

    // Third-person camera support
    private int cameraMode = 0; // 0=First person, 1=Third person back, 2=Third person front
    private long lastCameraToggleTime = 0;
    private float distanceWalked = 0.0f; // For walk animation
    private float prevDistanceWalked = 0.0f; // For animation interpolation
    private float bodyYaw = 0.0f; // Player body rotation
    private float prevBodyYaw = 0.0f; // For rotation interpolation
    // Orbit angles for third-person camera (stored separately so setLookTarget can
    // override view direction)
    private float orbitYaw = 0.0f;
    private float orbitPitch = 0.0f;

    // Animation State
    private boolean isSwinging;
    private boolean isMiningSwing;
    private float swingProgress;
    private float prevSwingProgress;
    private float swingCooldown; // 200ms cooldown between swing animations
    private float renderYawOffset; // The "Turret" body yaw
    private float prevRenderYawOffset;
    private float limbSwingAmount;
    private float prevLimbSwingAmount;

    public Player(float x, float y, float z) {
        this.position = new Vector3f(x, y, z);
        this.prevPosition = new Vector3f(x, y, z);
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

            if (cameraMode == 0) {
                camera.rotate(deltaX, deltaY);
            } else {
                // In 3rd person, update orbit angles directly
                // (Decouples input from camera visual state)
                orbitYaw += deltaX;
                orbitPitch += deltaY;

                // Clamp pitch
                if (orbitPitch > 90.0f)
                    orbitPitch = 90.0f;
                if (orbitPitch < -90.0f)
                    orbitPitch = -90.0f;

                // Keep yaw in reasonable range (optional but good)
                if (orbitYaw > 360.0f)
                    orbitYaw -= 360.0f;
                if (orbitYaw < 0.0f)
                    orbitYaw += 360.0f;
            }
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

        // Normalize input vector to prevent faster diagonal movement
        if (forward != 0 || strafe != 0) {
            float length = (float) Math.sqrt(forward * forward + strafe * strafe);
            forward /= length;
            strafe /= length;
        }

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

        // Camera mode toggle (F5 key) - cycles: First Person -> Third Person Back ->
        // Third Person Front

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
        // In Mode 2 (Front View), forward means "Away from Camera" (North if Camera
        // looks South)
        // Camera Yaw + 180 gives the correct "Forward" vector for the player
        float yawRad = (float) Math.toRadians(camera.getYaw() + (cameraMode == 2 ? 180 : 0));
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
            // Normal jump (Auto-jump enabled: use isKeyDown)
            // Disable ground jump if currently submerged to prevent "bouncing" on ocean
            // floor
            if (Input.isKeyDown(GLFW_KEY_SPACE) && onGround && !inWater) {
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
        // Always use eye position for raycast origin - reach should be from the
        // player's body, not the camera
        float currentEyeHeight = sneaking ? EYE_HEIGHT - 0.125f : EYE_HEIGHT;
        Vector3f rayOrigin = new Vector3f(position.x, position.y + currentEyeHeight, position.z);

        // Mode 0: from camera (which follows mouse)
        // Mode 1 & 2: from orbit angles (which follow mouse)
        float rayYaw = (cameraMode == 0) ? camera.getYaw() : orbitYaw;
        float rayPitch = (cameraMode == 0) ? camera.getPitch() : orbitPitch;

        // In front-facing mode, the vertical orbit is inverted relative to the
        // character's gaze.
        // If the camera is orbitally high (positive pitch), the character must look UP
        // (negative pitch) to face towards the camera.
        if (cameraMode == 2) {
            rayPitch = -rayPitch;
        }

        float yawRad = (float) Math.toRadians(rayYaw);
        float pitchRad = (float) Math.toRadians(rayPitch);

        // Calculate direction vector manually to ensure it's independent of visual
        // camera overrides
        Vector3f rayDirection = new Vector3f(
                (float) (Math.sin(yawRad) * Math.cos(pitchRad)),
                (float) (-Math.sin(pitchRad)),
                (float) (-Math.cos(yawRad) * Math.cos(pitchRad)));
        rayDirection.normalize();

        // Update target block using REACH_DISTANCE from eyes
        targetBlock = Raycast.cast(world, rayOrigin, rayDirection, REACH_DISTANCE);

        // Handle block interaction (left click)
        if (Input.isButtonPressed(GLFW_MOUSE_BUTTON_LEFT)) {
            // Initial click always triggers a swing
            swingArm();
        }

        if (Input.isButtonDown(GLFW_MOUSE_BUTTON_LEFT)) {
            if (targetBlock.hit && breakCooldown <= 0) {
                // We are actively mining - keep the arm swinging
                if (!isSwinging) {
                    swingArm();
                }

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

                    // Underwater penalty (5x slower)
                    if (inWater && !flying) {
                        progressIncrement /= 5.0f;
                    }

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
        // FIRST: Store previous values for render interpolation
        prevPosition.set(position);
        prevDistanceWalked = distanceWalked;
        prevRenderYawOffset = renderYawOffset;
        prevSwingProgress = swingProgress;

        updateSwing(deltaTime);
        updateTurretRotation();

        // Update physics state (falling, etc.)
        boolean wasOnGround = onGround;

        // Update body yaw to follow camera (for interpolation)
        bodyYaw = camera.getYaw();

        // Track falling for fall damage
        boolean isFalling = velocity.y < -0.1f && !onGround && !flying;

        if (isFalling && !wasFalling) {
            // Just started falling - record start position
            fallStartY = position.y;
        }
        wasFalling = isFalling;

        // Store previous water state
        boolean wasInWater = inWater;

        // Check water state
        inWater = world.getBlock((int) Math.floor(position.x), (int) Math.floor(position.y),
                (int) Math.floor(position.z)) == BlockType.WATER ||
                world.getBlock((int) Math.floor(position.x), (int) Math.floor(position.y + 1),
                        (int) Math.floor(position.z)) == BlockType.WATER;

        headInWater = world.getBlock((int) Math.floor(position.x), (int) Math.floor(position.y + EYE_HEIGHT),
                (int) Math.floor(position.z)) == BlockType.WATER;

        // Exit Detection: Player has fully breached (feet left water) while moving up
        // Trigger bobbing cooldown here to force a sink phase upon re-entry
        if (wasInWater && !inWater && velocity.y > 0) {
            surfaceBobbingTimer = 0.3f; // Reduced from 0.5f for faster cycle
        }

        // Update state for visual rendering
        this.headInWaterState = headInWater;

        // Update breath logic
        stats.updateAir(headInWater, deltaTime);

        // Apply movement physics
        if (flying) {
            // Creative flight (already handled in handleInput)
            // No gravity, high friction handled by input velocity setting
        } else if (inWater) {
            // Water physics
            // Apply reduced gravity (sink faster - 0.6f, separate from air gravity)
            velocity.y += GRAVITY * 0.6f * deltaTime;

            // Update Bobbing Timer
            if (surfaceBobbingTimer > 0) {
                surfaceBobbingTimer -= deltaTime;
            }

            // Vertical movement (Swimming)
            if (Input.isKeyDown(GLFW_KEY_SPACE)) {
                // Only swim up if the "bobbing cooldown" is inactive
                if (surfaceBobbingTimer <= 0) {
                    // Swim up (Significantly Faster)
                    velocity.y += 35.0f * deltaTime;
                    if (velocity.y > 15.0f)
                        velocity.y = 15.0f;
                }
            } else if (Input.isKeyDown(GLFW_KEY_LEFT_SHIFT)) {
                // Swim down (Faster)
                velocity.y -= 35.0f * deltaTime;
                if (velocity.y < -15.0f)
                    velocity.y = -15.0f;
            }

            // Drag in water (Frame-rate independent)
            // 0.82 reference at 60 FPS
            float dragFactor = (float) Math.pow(0.82, deltaTime * 60.0f);

            // Momentum Preservation (Cannonball effect)
            if (velocity.y < -10.0f) {
                // Falling fast into water -> Glide down
                velocity.y *= 0.95f; // Cannonball entry preserves more momentum
                velocity.x *= dragFactor;
                velocity.z *= dragFactor;
            } else {
                velocity.x *= dragFactor;
                velocity.z *= dragFactor;
                velocity.y *= dragFactor;
            }

            // Reset fall distance
            fallStartY = position.y;
        } else {
            // Standard Air/Ground physics
            velocity.y += GRAVITY * deltaTime;

            // Apply friction
            float friction = onGround ? FRICTION : AIR_FRICTION;
            velocity.x *= friction;
            velocity.z *= friction;
        }

        // Clamp horizontal velocity
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
        // boolean wasOnGround = onGround; // Moved to top of method

        // Move with collision detection
        moveWithCollision(deltaTime, world);

        // Fall damage calculation (only when landing from a fall, not in fly mode or
        // water)
        if (onGround && !wasOnGround && !flying && !inWater) {
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

        // Track distance walked for animation
        float dx = position.x - prevPosition.x;
        float dz = position.z - prevPosition.z;
        distanceWalked += (float) Math.sqrt(dx * dx + dz * dz);

        // Smoothed limb swing amount
        prevLimbSwingAmount = limbSwingAmount;
        float horizontalSpeedForLimb = (float) Math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z);
        float targetAmount = horizontalSpeedForLimb < 0.01f ? 0 : Math.min(horizontalSpeedForLimb * 4.0f, 1.0f);
        // Linear interpolation for smoothing (about 0.4 speed)
        limbSwingAmount += (targetAmount - limbSwingAmount) * 0.4f;

        // Push mobs when player collides with them (Minecraft-style)
        pushNearbyMobs(world, deltaTime);

        // Collect nearby dropped items (only if inventory has space)
        List<DroppedItem> collected = world.collectNearbyItems(
                position.x, position.y + 0.9f, position.z, deltaTime, this);
        for (DroppedItem item : collected) {
            addToInventory(item.getBlockType(), item.getCount());
        }

        // Update camera position based on camera mode
        updateCameraPosition();
    }

    /**
     * Update camera position based on camera mode.
     * Mode 0: First person (camera at eye level)
     * Mode 1: Third person back (camera behind player)
     * Mode 2: Third person front (camera in front of player, looking at player)
     * 
     * In modes 1 and 2, orbit angles control position while camera yaw/pitch is
     * synced in mode 0-1 but overridden in mode 2 to look at player.
     */
    private void updateCameraPosition() {
        // Reduce eye height when sneaking
        float currentEyeHeight = sneaking ? EYE_HEIGHT - 0.125f : EYE_HEIGHT;
        float eyeY = position.y + currentEyeHeight;

        if (cameraMode == 0) {
            // First person - camera at eye position
            camera.setPosition(position.x, eyeY, position.z);
            // Sync orbit angles from camera for when switching to 3rd person
            orbitYaw = camera.getYaw();
            orbitPitch = camera.getPitch();
        } else {
            // In 3rd person, sync orbit from camera (mouse changes camera yaw/pitch)
            // In 3rd person, orbit angles are master (updated by Input)
            // NO SYNC from camera here - this breaks the feedback loop!
            // orbitYaw = camera.getYaw();
            // orbitPitch = camera.getPitch();

            // Third person - camera orbits around player using orbit angles
            float distance = 6.0f; // Distance from player
            float yawRad = (float) Math.toRadians(orbitYaw);
            float pitchRad = (float) Math.toRadians(orbitPitch);

            // Calculate camera offset based on yaw and pitch
            float horizontal = (float) Math.cos(pitchRad) * distance;
            float vertical = (float) Math.sin(pitchRad) * distance;

            float offsetX = (float) Math.sin(yawRad) * horizontal;
            float offsetZ = (float) -Math.cos(yawRad) * horizontal;
            float offsetY = vertical;

            float camX, camY, camZ;

            if (cameraMode == 1) {
                // Third person back - camera behind player, keeps camera yaw/pitch
                camX = position.x - offsetX;
                camY = eyeY + offsetY;
                camZ = position.z - offsetZ;
                camera.setPosition(camX, camY, camZ);
                camera.setYaw(orbitYaw);
                camera.setPitch(orbitPitch);
            } else {
                // Third person front - camera in front of player, looks at player
                camX = position.x + offsetX;
                camY = eyeY + offsetY;
                camZ = position.z + offsetZ;
                camera.setPosition(camX, camY, camZ);
                // Make camera look back at the player (this changes camera yaw/pitch)
                camera.setLookTarget(position.x, eyeY, position.z);
            }
        }
    }

    /**
     * Set camera position based on interpolated player position for smooth 60fps
     * rendering.
     * This eliminates 3rd person camera jitter caused by physics running at 20fps.
     * NOTE: This overwrites the camera position set in update(), which is fine as
     * it's
     * called every frame before render.
     */
    public void setInterpolatedCameraPosition(float partialTick) {
        // Interpolate player position
        float interpX = prevPosition.x + (position.x - prevPosition.x) * partialTick;
        float interpY = prevPosition.y + (position.y - prevPosition.y) * partialTick;
        float interpZ = prevPosition.z + (position.z - prevPosition.z) * partialTick;

        // Reduce eye height when sneaking
        float currentEyeHeight = sneaking ? EYE_HEIGHT - 0.125f : EYE_HEIGHT;
        float eyeY = interpY + currentEyeHeight;

        if (cameraMode == 0) {
            // First person - camera at interpolated eye position
            camera.setPosition(interpX, eyeY, interpZ);
        } else {
            // Third person logic using interpolated origin

            // Use current orbit angles
            // For mode 1, camera.getYaw() is valid and smooth (mouse input)
            // For mode 2, camera.getYaw() is locked to player, so use orbitYaw
            // Use current orbit angles (Master)
            // Decoupled input means orbitYaw is always up to date
            float yawToUse = orbitYaw;
            float pitchToUse = orbitPitch;

            float distance = 6.0f;
            float yawRad = (float) Math.toRadians(yawToUse);
            float pitchRad = (float) Math.toRadians(pitchToUse);

            float horizontal = (float) Math.cos(pitchRad) * distance;
            float vertical = (float) Math.sin(pitchRad) * distance;

            float offsetX = (float) Math.sin(yawRad) * horizontal;
            float offsetZ = (float) -Math.cos(yawRad) * horizontal;
            float offsetY = vertical;

            float camX, camY, camZ;

            if (cameraMode == 1) {
                // Third person back
                camX = interpX - offsetX;
                camY = eyeY + offsetY;
                camZ = interpZ - offsetZ;
                camera.setPosition(camX, camY, camZ);
                camera.setYaw(yawToUse);
                camera.setPitch(pitchToUse);
            } else {
                // Third person front
                camX = interpX + offsetX;
                camY = eyeY + offsetY;
                camZ = interpZ + offsetZ;
                camera.setPosition(camX, camY, camZ);
                // Camera looks at interpolated player position
                camera.setLookTarget(interpX, eyeY, interpZ);
            }
        }
    }

    /**
     * Push nearby mobs away from player.
     * Minecraft-style soft collision - player can push mobs but not stand on them.
     */
    private void pushNearbyMobs(World world, float deltaTime) {
        for (com.craftzero.entity.Entity entity : world.getEntities()) {
            if (entity instanceof com.craftzero.entity.LivingEntity mob) {
                AABB mobBox = mob.getBoundingBox();
                if (mobBox != null && boundingBox.intersects(mobBox)) {
                    // Calculate push direction (from mob center to player center)
                    float dx = mob.getX() - position.x;
                    float dz = mob.getZ() - position.z;
                    float dist = (float) Math.sqrt(dx * dx + dz * dz);

                    if (dist > 0.01f) {
                        // Normalize and apply push force
                        float pushStrength = 0.05f; // Reduced from 0.15f (too aggressive)
                        float pushX = (dx / dist) * pushStrength;
                        float pushZ = (dz / dist) * pushStrength;

                        // Push the mob away
                        mob.addMotion(pushX, 0, pushZ);
                    }
                }
            }
        }
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

        // Safe Walk (Sneaking on edges)
        if (sneaking && onGround) {
            float step = 0.05f;

            // Check X axis
            while (dx != 0) {
                if (hasGroundBelow(world, dx, 0f))
                    break;
                if (Math.abs(dx) < step)
                    dx = 0;
                else
                    dx -= Math.signum(dx) * step;
            }

            // Check Z axis
            while (dz != 0) {
                if (hasGroundBelow(world, 0f, dz))
                    break;
                if (Math.abs(dz) < step)
                    dz = 0;
                else
                    dz -= Math.signum(dz) * step;
            }

            // Check Combined (Diagonal) - ensures we don't fall off corners
            while (dx != 0 && dz != 0 && !hasGroundBelow(world, dx, dz)) {
                if (Math.abs(dx) < step)
                    dx = 0;
                else
                    dx -= Math.signum(dx) * step;

                if (Math.abs(dz) < step)
                    dz = 0;
                else
                    dz -= Math.signum(dz) * step;
            }
        }

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
     * Also includes mob bounding boxes for player-mob collision.
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

        // Note: Mob collision is handled by pushNearbyMobs() instead of rigid collision
        // This prevents player from standing on mobs while still allowing push
        // interactions

        return colliders;
    }

    /**
     * Check if there is solid ground below the proposed movement.
     */
    private boolean hasGroundBelow(World world, float dx, float dz) {
        // Create a test box moved by dx/dz and slightly down (-0.1f)
        AABB testBox = new AABB(
                boundingBox.getMin().x + dx, boundingBox.getMin().y - 0.1f, boundingBox.getMin().z + dz,
                boundingBox.getMax().x + dx, boundingBox.getMax().y - 0.1f, boundingBox.getMax().z + dz);

        // Scan for blocks
        int minX = (int) Math.floor(testBox.getMin().x);
        int minY = (int) Math.floor(testBox.getMin().y);
        int minZ = (int) Math.floor(testBox.getMin().z);
        int maxX = (int) Math.ceil(testBox.getMax().x);
        int maxY = (int) Math.ceil(testBox.getMax().y);
        int maxZ = (int) Math.ceil(testBox.getMax().z);

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    BlockType block = world.getBlock(x, y, z);
                    if (block.isSolid()) {
                        // Check actual intersection
                        if (AABB.forBlock(x, y, z).intersects(testBox)) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    // Getters

    public Camera getCamera() {
        return camera;
    }

    public Vector3f getPosition() {
        return position;
    }

    public Vector3f getPrevPosition() {
        return prevPosition;
    }

    public float getDistanceWalked() {
        return distanceWalked;
    }

    public float getPrevDistanceWalked() {
        return prevDistanceWalked;
    }

    public float getLimbSwingAmount(float partialTick) {
        return prevLimbSwingAmount + (limbSwingAmount - prevLimbSwingAmount) * partialTick;
    }

    public float getBodyYaw() {
        return bodyYaw;
    }

    public float getPrevBodyYaw() {
        return prevBodyYaw;
    }

    public int getCameraMode() {
        return cameraMode;
    }

    public void setCameraMode(int mode) {
        this.cameraMode = mode % 3; // Cycle through 0, 1, 2
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
     * Check if player's head is currently underwater.
     * Uses the world reference passed to update() or needs world passed here.
     * Since this is a simple getter, we'll need to store the state during update.
     */
    private boolean headInWaterState = false;

    public boolean isHeadInWater() {
        return headInWaterState;
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

    public void cycleCameraMode() {
        long now = System.currentTimeMillis();
        // Debounce toggle (200ms) to prevent rapid glitching
        if (now - lastCameraToggleTime > 200) {
            int oldMode = cameraMode;
            cameraMode = (cameraMode + 1) % 3;
            lastCameraToggleTime = now;
            System.out.println("Camera Mode toggled to: " + cameraMode);

            // Sync Orientation to prevents flipping
            if (oldMode == 0) {
                // FPS -> 3rd Person: Init orbit from current view direction
                orbitYaw = camera.getYaw();
                orbitPitch = camera.getPitch();
            } else if (cameraMode == 0) {
                // 3rd Person -> FPS: Align camera to orbit direction
                camera.setYaw(orbitYaw);
                camera.setPitch(orbitPitch);
            }
        }
    }

    private void updateSwing(float deltaTime) {
        // Update swing cooldown
        if (swingCooldown > 0) {
            swingCooldown -= deltaTime;
        }

        if (isSwinging) {
            // Air swing speed: Slow for visibility. Mining: 6.4f as before.
            float swingSpeed = isMiningSwing ? 6.4f : 6.0f;

            swingProgress += deltaTime * swingSpeed;
            if (swingProgress >= 1.0f) {
                swingProgress = 0.0f;
                isSwinging = false;
                isMiningSwing = false;
            }
        } else {
            swingProgress = 0.0f;
            isMiningSwing = false;
        }
    }

    private void updateTurretRotation() {
        // Body rotation is based on MOVEMENT direction when strafing/diagonal
        // NOT triggered by mouse movement or forward/backward movement

        float velX = velocity.x;
        float velZ = velocity.z;
        float speed = velX * velX + velZ * velZ;

        if (speed > 0.001f) {
            // Calculate movement yaw
            // atan2(velX, -velZ) correctly maps move coords to camera yaw (0 = North/-Z)
            float moveYaw = (float) Math.toDegrees(Math.atan2(velX, -velZ));

            // Normalize to -180 to 180
            while (moveYaw >= 180)
                moveYaw -= 360;
            while (moveYaw < -180)
                moveYaw += 360;

            // Check angle difference between movement and look
            float lookYaw = camera.getYaw();
            float diffFromLook = moveYaw - lookYaw;
            while (diffFromLook >= 180)
                diffFromLook -= 360;
            while (diffFromLook < -180)
                diffFromLook += 360;

            // Trigger animation instantly when clearly strafing (>30 degrees from look)
            // Forward (0°±30) and backward (180°±30) are excluded
            if (Math.abs(diffFromLook) > 30 && Math.abs(diffFromLook) < 150) {
                // Strafing or diagonal - body slowly rotates toward movement direction
                float bodyDiff = moveYaw - renderYawOffset;
                while (bodyDiff >= 180)
                    bodyDiff -= 360;
                while (bodyDiff < -180)
                    bodyDiff += 360;
                renderYawOffset += bodyDiff * 0.1f;
            } else {
                // Forward/backward - body MUST face look direction (reset strafe pose)
                float bodyDiff = lookYaw - renderYawOffset;
                while (bodyDiff >= 180)
                    bodyDiff -= 360;
                while (bodyDiff < -180)
                    bodyDiff += 360;
                renderYawOffset += bodyDiff * 0.1f;
            }
        } else {
            // Not moving - body slowly returns to face look direction
            float bodyDiff = camera.getYaw() - renderYawOffset;
            while (bodyDiff >= 180)
                bodyDiff -= 360;
            while (bodyDiff < -180)
                bodyDiff += 360;
            renderYawOffset += bodyDiff * 0.05f;
        }

        // Clamp head relative to body - head can turn 30 degrees before body follows
        float headDiff = camera.getYaw() - renderYawOffset;
        while (headDiff >= 180)
            headDiff -= 360;
        while (headDiff < -180)
            headDiff += 360;

        if (headDiff > 30)
            renderYawOffset = camera.getYaw() - 30;
        if (headDiff < -30)
            renderYawOffset = camera.getYaw() + 30;
    }

    public void swingArm() {
        // 200ms animation cooldown - prevents jitter from rapid clicks
        if (swingCooldown > 0) {
            return; // Still in cooldown, ignore this swing request
        }

        isSwinging = true;
        swingProgress = 0;
        prevSwingProgress = 0;
        swingCooldown = 0.2f; // 200ms cooldown
        isMiningSwing = targetBlock != null && targetBlock.hit;
    }

    public float getSwingProgress(float partialTick) {
        return prevSwingProgress + (swingProgress - prevSwingProgress) * partialTick;
    }

    public float getRenderYawOffset(float partialTick) {
        return prevRenderYawOffset + (renderYawOffset - prevRenderYawOffset) * partialTick;
    }
}
