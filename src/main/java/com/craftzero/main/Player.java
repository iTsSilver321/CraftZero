package com.craftzero.main;

import com.craftzero.engine.Input;
import com.craftzero.combat.DamageSource;
import com.craftzero.entity.ArrowEntity;
import com.craftzero.entity.DroppedItem;
import com.craftzero.entity.EyeOfEnderEntity;
import com.craftzero.entity.LivingEntity;
import com.craftzero.graphics.Camera;
import com.craftzero.inventory.ItemStack;
import com.craftzero.inventory.ItemStackOps;
import com.craftzero.inventory.ItemType;
import com.craftzero.physics.AABB;
import com.craftzero.physics.Raycast;
import com.craftzero.progression.ArmorMaterial;
import com.craftzero.progression.ArmorSlot;
import com.craftzero.progression.ArmorCalculator;
import com.craftzero.progression.EnchantmentResolver;
import com.craftzero.progression.EnchantmentType;
import com.craftzero.progression.PotionData;
import com.craftzero.progression.PotionEffectResolver;
import com.craftzero.progression.StatusEffectInstance;
import com.craftzero.progression.StatusEffectType;
import com.craftzero.world.Block;
import com.craftzero.world.BlockType;
import com.craftzero.world.BlockShape;
import com.craftzero.world.StructureGenerator;
import com.craftzero.world.StructureType;

import com.craftzero.world.World;
import com.craftzero.world.tile.BlockPos;
import com.craftzero.world.tile.JukeboxTileEntity;
import com.craftzero.world.tile.TileEntity;
import org.joml.Vector3f;
import org.joml.Vector3i;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

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
    private static final float REACH_DISTANCE = 5.0f; // Block reach (mining/placing)
    private static final float ENTITY_REACH = 3.0f; // Entity attack reach (Minecraft standard)
    private static final float BREAK_COOLDOWN = 0.25f;
    private static final float PLACE_COOLDOWN = 0.25f;
    private static final float BOW_MAX_DRAW_TIME = 1.0f;
    private static final float BOW_MIN_DRAW_TIME = 0.10f;

    private Vector3f position;
    private Vector3f prevPosition; // Previous position for render interpolation
    private Vector3f velocity;
    private Camera camera;
    private AABB boundingBox;

    private boolean onGround;
    private boolean sprinting;
    private boolean sneaking;
    private boolean flying; // Creative mode flight
    private GameMode gameMode = GameMode.SURVIVAL;
    private Difficulty difficulty = Difficulty.EASY;
    private GameSettings settings = GameSettings.defaults();
    private float mouseSensitivityMultiplier = 1.0f;
    private boolean invertMouse;
    private boolean viewBobbing = true;

    // Double-tap W sprint detection
    private float lastWPressTime;
    private boolean wWasReleased;
    private boolean sprintKnockbackUsed; // W-tap mechanic: true = bonus KB already used, need W release to reset

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
    private Vector3i requestedChestPos;
    private Vector3i requestedFurnacePos;
    private Vector3i requestedBrewingStandPos;
    private Vector3i requestedEnchantingTablePos;
    private Vector3i requestedSignEditPos;
    private Vector3i requestedBedUsePos;

    // World reference for lighting lookups
    private World world;

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

    // Use animation state (for block placing)
    private boolean isUsingItem;
    private float useProgress;
    private float prevUseProgress;
    private float useCooldown;
    private boolean isDrawingBow;
    private float bowDrawTime;

    // Slot switch animation state (for smooth item change)
    private int lastSelectedSlot = 0;
    private float slotSwitchProgress = 1.0f; // 0 = fully retracted, 1 = fully visible
    private float prevSlotSwitchProgress = 1.0f;
    private boolean isRetracting = false; // true = going down, false = coming up
    private ItemType lastHeldItemType = null; // Track for inventory changes
    private final Random random = new Random();

    // Death state
    private int deathTime = 0; // Ticks since death (for death animation)
    private float spawnX, spawnY, spawnZ; // Spawn point for respawn

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
        // Store spawn point for respawning
        this.spawnX = x;
        this.spawnY = y;
        this.spawnZ = z;
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
        // Block all input when dead
        if (stats.isDead()) {
            return;
        }

        // Track elapsed time for double-tap detection
        if (lastWPressTime >= 0) {
            lastWPressTime += deltaTime;
        }

        // Mouse look
        if (Input.isCursorLocked()) {
            float deltaX = (float) Input.getDeltaX() * MOUSE_SENSITIVITY * mouseSensitivityMultiplier;
            float deltaY = (float) Input.getDeltaY() * MOUSE_SENSITIVITY * mouseSensitivityMultiplier;
            if (invertMouse) {
                deltaY = -deltaY;
            }

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

        if (isActionDown(GameSettings.KeyBinding.FORWARD))
            forward += 1;
        if (isActionDown(GameSettings.KeyBinding.BACK))
            forward -= 1;
        if (isActionDown(GameSettings.KeyBinding.LEFT))
            strafe -= 1;
        if (isActionDown(GameSettings.KeyBinding.RIGHT))
            strafe += 1;

        // Normalize input vector to prevent faster diagonal movement
        if (forward != 0 || strafe != 0) {
            float length = (float) Math.sqrt(forward * forward + strafe * strafe);
            forward /= length;
            strafe /= length;
        }

        // Sneaking (Shift key) - not while flying
        sneaking = isActionDown(GameSettings.KeyBinding.SNEAK) && !flying;

        // Sprint detection: Ctrl held OR double-tap W
        // Double-tap W: If W is pressed within DOUBLE_TAP_TIME of last W press, start
        // sprinting
        if (isActionPressed(GameSettings.KeyBinding.FORWARD)) {
            if (wWasReleased && lastWPressTime >= 0 && lastWPressTime < DOUBLE_TAP_TIME) {
                // Double-tap detected!
                sprinting = true;
            }
            lastWPressTime = 0; // Reset timer on W press
            wWasReleased = false;
        }

        // Track W release for double-tap detection AND sprint knockback reset
        if (isActionReleased(GameSettings.KeyBinding.FORWARD)) {
            wWasReleased = true;
            sprintKnockbackUsed = false; // W-tap reset: releasing W allows sprint KB bonus again
        }

        // Ctrl also triggers sprint while moving forward
        if (Input.isKeyDown(GLFW_KEY_LEFT_CONTROL) && forward > 0) {
            sprinting = true;
        }

        // Stop sprinting if: moving backward, sneaking, or not moving forward
        if (forward <= 0 || sneaking || (!isCreative() && stats.getHunger() <= 6.0f)) {
            sprinting = false;
        }

        // Clear double-tap timer if it expires
        if (lastWPressTime > DOUBLE_TAP_TIME) {
            lastWPressTime = -1;
        }

        // Flying toggle (F key)
        if (Input.isKeyPressed(GLFW_KEY_F) && isCreative()) {
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
        speed *= stats.getMovementSpeedMultiplier();

        // Get camera direction for movement
        // In Mode 2 (Front View), we want Standard Control (W=Away, A=Left)
        // To make A move Screen Left (+X) while facing Camera (South/180),
        // we need to invert the strafe sign relative to the camera vector.
        float yawRad = (float) Math.toRadians(camera.getYaw() + (cameraMode == 2 ? 180 : 0));
        float sinYaw = (float) Math.sin(yawRad);
        float cosYaw = (float) Math.cos(yawRad);

        float strafeSign = (cameraMode == 2) ? -strafe : strafe;

        // Calculate movement direction
        float moveX = (forward * sinYaw + strafeSign * cosYaw) * speed * ACCELERATION;
        float moveZ = (-forward * cosYaw + strafeSign * sinYaw) * speed * ACCELERATION;

        // Apply movement (normalized to 60 FPS)
        float frameScale = deltaTime * 60.0f;
        velocity.x += moveX * frameScale;
        velocity.z += moveZ * frameScale;

        // Flying controls
        if (flying) {
            if (isActionDown(GameSettings.KeyBinding.JUMP)) {
                velocity.y = speed;
            } else if (isActionDown(GameSettings.KeyBinding.SNEAK)) {
                velocity.y = -speed;
            } else {
                velocity.y *= 0.5f;
            }
        } else {
            // Normal jump (Auto-jump enabled: use isKeyDown)
            // Disable ground jump if currently submerged to prevent "bouncing" on ocean
            // floor
            if (isActionDown(GameSettings.KeyBinding.JUMP) && onGround && !inWater) {
                velocity.y = JUMP_VELOCITY;
                onGround = false;
                if (!isCreative()) {
                    stats.onJump(); // Drain hunger from jumping
                }
            }
        }

        // Hotbar Scrolling
        float scrollY = (float) Input.getScrollY();
        if (scrollY != 0) {
            int current = inventory.getSelectedSlot();
            int newSlot;
            if (scrollY > 0) {
                newSlot = (current - 1 + 9) % 9; // Scroll Up -> Previous Slot
            } else {
                newSlot = (current + 1) % 9; // Scroll Down -> Next Slot
            }
            if (newSlot != current) {
                triggerSlotSwitch(newSlot);
            }
        }

        // Block type selection (number keys) - trigger switch animation
        int current = inventory.getSelectedSlot();
        if (Input.isKeyPressed(GLFW_KEY_1) && current != 0)
            triggerSlotSwitch(0);
        if (Input.isKeyPressed(GLFW_KEY_2) && current != 1)
            triggerSlotSwitch(1);
        if (Input.isKeyPressed(GLFW_KEY_3) && current != 2)
            triggerSlotSwitch(2);
        if (Input.isKeyPressed(GLFW_KEY_4) && current != 3)
            triggerSlotSwitch(3);
        if (Input.isKeyPressed(GLFW_KEY_5) && current != 4)
            triggerSlotSwitch(4);
        if (Input.isKeyPressed(GLFW_KEY_6) && current != 5)
            triggerSlotSwitch(5);
        if (Input.isKeyPressed(GLFW_KEY_7) && current != 6)
            triggerSlotSwitch(6);
        if (Input.isKeyPressed(GLFW_KEY_8) && current != 7)
            triggerSlotSwitch(7);
        if (Input.isKeyPressed(GLFW_KEY_9) && current != 8)
            triggerSlotSwitch(8);

        // Q key to drop one item from selected slot
        if (isActionPressed(GameSettings.KeyBinding.DROP)) {
            dropItemFromHand = true; // Flag for Main to handle with world reference
        }

        // Update cooldowns
        if (breakCooldown > 0)
            breakCooldown = Math.max(0.0f, breakCooldown - deltaTime);
        if (placeCooldown > 0)
            placeCooldown = Math.max(0.0f, placeCooldown - deltaTime);
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

        // Check for entity hit (shorter reach: 3.0 blocks for combat)
        Raycast.EntityRaycastResult entityHit = Raycast.castEntities(
                world.getEntities(), rayOrigin, rayDirection, ENTITY_REACH, null);

        // Handle left click - attack entities OR mine blocks
        if (isActionPressed(GameSettings.KeyBinding.ATTACK)) {
            // Initial click always triggers a swing
            swingArm();

            // Priority 1: Attack entity if in range
            if (entityHit.hit && entityHit.entity != null) {
                attackEntity(entityHit.entity);
            }
        }

        // Continuous left-click for mining (only if NOT attacking an entity)
        boolean miningInput = isCreative()
                ? isActionPressed(GameSettings.KeyBinding.ATTACK)
                : isActionDown(GameSettings.KeyBinding.ATTACK);
        if (miningInput) {
            // Only mine blocks if we didn't hit an entity
            if (!entityHit.hit && targetBlock.hit && breakCooldown <= 0) {
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
                            speedMultiplier += EnchantmentResolver.miningSpeedBonus(heldItem);
                        }
                    }

                    // Creative mode breaks blocks instantly.
                    float progressIncrement = isCreative() ? 1.0f : (deltaTime * speedMultiplier) / hardness;

                    // Underwater penalty (3x slower)
                    if (!isCreative() && inWater && !flying) {
                        progressIncrement /= 3.0f;
                    } else if (!isCreative() && !onGround && !flying) {
                        // Air/Jump penalty (2.5x slower)
                        progressIncrement /= 2.5f;
                    }

                    breakProgress += progressIncrement;

                    // Block is broken when progress reaches 1.0
                    if (breakProgress >= 1.0f) {
                        // Check harvest category and level - ore drops require the right tool family.
                        boolean canHarvest = isCreative() || targetType.getHarvestLevel() <= 0
                                || (toolType.isEffectiveAgainst(targetType.getPreferredTool())
                                        && toolType.getMiningLevel() >= targetType.getHarvestLevel());

                        world.breakBlock(currentTarget.x, currentTarget.y, currentTarget.z, canHarvest);

                        // Consume tool durability
                        if (!isCreative() && heldItem != null && heldItem.isTool()) {
                            boolean toolBroke = useDurabilityWithEnchantments(heldItem);
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
            } else if (!entityHit.hit) {
                // Not looking at a block OR entity - reset progress
                resetBreakingProgress();
            }
        } else {
            // Button released - reset progress
            resetBreakingProgress();
        }

        // Update break cooldown
        // Flag for opening crafting table
        wantsCraftingTable = false;
        requestedChestPos = null;
        requestedFurnacePos = null;
        requestedBrewingStandPos = null;
        requestedEnchantingTablePos = null;
        requestedSignEditPos = null;
        requestedBedUsePos = null;

        boolean bowHandled = handleBowUse(world, deltaTime, rayDirection);

        // Use item / place block (right click)
        if (!bowHandled && isActionPressed(GameSettings.KeyBinding.USE) && placeCooldown <= 0) {
            com.craftzero.inventory.ItemStack stack = inventory.getItemInHand();
            if (targetBlock.hit) {
                // Check if clicking on a crafting table - open it instead of placing
                BlockType clickedBlock = world.getBlock(
                        targetBlock.blockPos.x,
                        targetBlock.blockPos.y,
                        targetBlock.blockPos.z);

                if (clickedBlock == BlockType.CRAFTING_TABLE) {
                    wantsCraftingTable = true;
                    placeCooldown = PLACE_COOLDOWN;
                } else if (clickedBlock == BlockType.CHEST) {
                    requestedChestPos = new Vector3i(targetBlock.blockPos);
                    placeCooldown = PLACE_COOLDOWN;
                } else if (clickedBlock.isFurnace()) {
                    requestedFurnacePos = new Vector3i(targetBlock.blockPos);
                    placeCooldown = PLACE_COOLDOWN;
                } else if (clickedBlock == BlockType.BREWING_STAND) {
                    requestedBrewingStandPos = new Vector3i(targetBlock.blockPos);
                    placeCooldown = PLACE_COOLDOWN;
                } else if (clickedBlock == BlockType.ENCHANTING_TABLE) {
                    requestedEnchantingTablePos = new Vector3i(targetBlock.blockPos);
                    placeCooldown = PLACE_COOLDOWN;
                } else if (clickedBlock.isBed()) {
                    requestedBedUsePos = new Vector3i(targetBlock.blockPos);
                    placeCooldown = PLACE_COOLDOWN;
                } else if (world.toggleBlock(targetBlock.blockPos.x, targetBlock.blockPos.y, targetBlock.blockPos.z)) {
                    placeCooldown = PLACE_COOLDOWN;
                } else {
                    if (handleImmediateItemUse(world, stack)) {
                        placeCooldown = PLACE_COOLDOWN;
                    } else if (handleTargetedItemUse(world, stack, clickedBlock)) {
                        placeCooldown = PLACE_COOLDOWN;
                    } else if (handleBucketUse(world, stack)) {
                        placeCooldown = PLACE_COOLDOWN;
                    } else if (targetBlock.previousBlockPos != null && tryPlaceHeldItem(world, clickedBlock, stack)) {
                        placeCooldown = PLACE_COOLDOWN;
                    }
                }
            } else if (handleImmediateItemUse(world, stack)) {
                placeCooldown = PLACE_COOLDOWN;
            }
        }

    }

    private boolean handleBowUse(World world, float deltaTime, Vector3f rayDirection) {
        com.craftzero.inventory.ItemStack held = inventory.getItemInHand();
        boolean holdingBow = held != null && !held.isEmpty() && held.getType() == ItemType.BOW;
        if (!holdingBow) {
            isDrawingBow = false;
            bowDrawTime = 0.0f;
            return false;
        }

        if (isActionDown(GameSettings.KeyBinding.USE)) {
            if (hasArrow()) {
                isDrawingBow = true;
                bowDrawTime = Math.min(BOW_MAX_DRAW_TIME, bowDrawTime + deltaTime);
                isUsingItem = true;
                useProgress = Math.min(1.0f, bowDrawTime / BOW_MAX_DRAW_TIME);
                prevUseProgress = useProgress;
            }
            return true;
        }

        if (isActionReleased(GameSettings.KeyBinding.USE) && isDrawingBow) {
            fireBow(world, held, rayDirection, bowDrawTime);
            isDrawingBow = false;
            bowDrawTime = 0.0f;
            isUsingItem = false;
            useProgress = 0.0f;
            prevUseProgress = 0.0f;
            return true;
        }

        return isDrawingBow;
    }

    private void fireBow(World world, com.craftzero.inventory.ItemStack bow, Vector3f direction, float drawTime) {
        if (drawTime < BOW_MIN_DRAW_TIME) {
            return;
        }

        float charge = Math.min(1.0f, drawTime / BOW_MAX_DRAW_TIME);
        float power = (charge * charge + charge * 2.0f) / 3.0f;
        if (power < 0.1f) {
            return;
        }
        boolean infinity = bow != null && EnchantmentResolver.has(bow, EnchantmentType.INFINITY);
        if (!infinity && !consumeArrow()) {
            return;
        }

        Vector3f spawn = new Vector3f(position.x, position.y + EYE_HEIGHT - 0.1f, position.z)
                .add(new Vector3f(direction).mul(0.6f));
        float speed = 3.0f * power;
        float damage = 2.0f + 4.0f * power;
        int powerLevel = EnchantmentResolver.getLevel(bow, EnchantmentType.POWER);
        if (powerLevel > 0) {
            damage += 0.5f * powerLevel + 0.5f;
        }
        ArrowEntity arrow = world.spawnArrow(spawn.x, spawn.y, spawn.z,
                direction.x * speed,
                direction.y * speed,
                direction.z * speed,
                null,
                true,
                damage);
        int punch = EnchantmentResolver.getLevel(bow, EnchantmentType.PUNCH);
        if (punch > 0) {
            arrow.setKnockback(CombatRules.ARROW_HORIZONTAL_KNOCKBACK + punch * 0.25f,
                    CombatRules.ARROW_VERTICAL_KNOCKBACK);
        }
        if (EnchantmentResolver.has(bow, EnchantmentType.FLAME)) {
            arrow.setFireTicksOnHit(100);
        }

        swingArm();
        if (!isCreative() && bow != null && bow.isDamageable()) {
            boolean broke = useDurabilityWithEnchantments(bow);
            if (broke) {
                inventory.getHotbar()[inventory.getSelectedSlot()] = null;
            }
        }
    }

    private boolean hasArrow() {
        if (isCreative()) {
            return true;
        }
        return findArrowSlot(inventory.getHotbar()) >= 0 || findArrowSlot(inventory.getMainInventory()) >= 0;
    }

    private boolean consumeArrow() {
        if (isCreative()) {
            return true;
        }
        if (consumeArrowFrom(inventory.getHotbar())) {
            return true;
        }
        return consumeArrowFrom(inventory.getMainInventory());
    }

    private int findArrowSlot(com.craftzero.inventory.ItemStack[] slots) {
        for (int i = 0; i < slots.length; i++) {
            if (slots[i] != null && !slots[i].isEmpty() && slots[i].getType() == ItemType.ARROW) {
                return i;
            }
        }
        return -1;
    }

    private boolean consumeArrowFrom(com.craftzero.inventory.ItemStack[] slots) {
        int slot = findArrowSlot(slots);
        if (slot < 0) {
            return false;
        }
        slots[slot].remove(1);
        if (slots[slot].isEmpty()) {
            slots[slot] = null;
        }
        return true;
    }

    /**
     * Reset block breaking progress.
     */
    private void resetBreakingProgress() {
        breakingBlockPos = null;
        breakProgress = 0f;
        currentBreakingBlock = null;
    }

    private boolean tryPlaceHeldItem(World world, BlockType clickedBlock, com.craftzero.inventory.ItemStack stack) {
        if (stack == null || stack.isEmpty() || !stack.getType().isPlaceable()) {
            return false;
        }

        ItemType itemType = stack.getType();
        BlockType placedBlock = itemType.getPlacedBlock();

        if (BlockShape.blocksPlacementAgainst(clickedBlock, targetBlock.face) && placedBlock.isSolid()) {
            return false;
        }

        if (itemType == ItemType.STONE_SLAB && clickedBlock == BlockType.STONE_SLAB
                && world.tryMergeSlab(targetBlock.blockPos.x, targetBlock.blockPos.y, targetBlock.blockPos.z)) {
            consumePlacedStack(stack);
            return true;
        }

        Vector3i placePos = targetBlock.previousBlockPos;
        if (placePos == null) {
            return false;
        }

        if (placedBlock == BlockType.CHEST && !world.canPlaceChestAt(placePos.x, placePos.y, placePos.z)) {
            return false;
        }

        boolean placed = false;
        if (itemType == ItemType.WOODEN_DOOR || itemType == ItemType.IRON_DOOR) {
            placed = world.placeDoor(placePos.x, placePos.y, placePos.z, placedBlock, getHorizontalFacingIndex(), boundingBox);
        } else if (itemType == ItemType.BED) {
            BlockPos foot = world.placeBed(placePos.x, placePos.y, placePos.z, getHorizontalFacingIndex(), boundingBox);
            placed = foot != null;
        } else if (itemType == ItemType.SIGN) {
            placed = placeSign(world, placePos);
        } else {
            int metadata = getPlacementMetadata(placedBlock);
            if (world.canPlaceBlockAt(placePos.x, placePos.y, placePos.z, placedBlock, metadata, boundingBox)) {
                world.setBlock(placePos.x, placePos.y, placePos.z, placedBlock, metadata);
                placed = true;
            }
        }

        if (placed) {
            consumePlacedStack(stack);
        }
        return placed;
    }

    private boolean handleImmediateItemUse(World world, com.craftzero.inventory.ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        if (equipArmorFromHand(stack)) {
            startUseAnimation();
            return true;
        }
        FoodValue food = foodValue(stack.getType());
        if (food != null && (stats.getHunger() < PlayerStats.MAX_HUNGER || stack.getType() == ItemType.GOLDEN_APPLE)) {
            if (!isCreative()) {
                stats.feed(food.hunger(), food.saturation());
                if (stack.getType() == ItemType.ROTTEN_FLESH && Math.random() < 0.8) {
                    stats.addEffect(new StatusEffectInstance(StatusEffectType.HUNGER, 30 * 20, 0));
                }
                consumeFoodStack(world, stack);
            }
            startUseAnimation();
            return true;
        }
        if (stack.getType() == ItemType.MILK_BUCKET) {
            if (!isCreative()) {
                stats.clearEffects();
                replaceHeldItemAfterBucketUse(world, stack, ItemType.BUCKET);
            } else {
                startUseAnimation();
            }
            return true;
        }
        if (stack.getType() == ItemType.POTION) {
            return usePotion(world, stack);
        }
        if (stack.getType() == ItemType.EYE_OF_ENDER
                && (targetBlock == null || !targetBlock.hit
                        || world.getBlockIfLoaded(targetBlock.blockPos.x, targetBlock.blockPos.y,
                                targetBlock.blockPos.z, BlockType.AIR) != BlockType.END_PORTAL_FRAME)) {
            return throwEyeOfEnder(world, stack);
        }
        return false;
    }

    private boolean usePotion(World world, com.craftzero.inventory.ItemStack stack) {
        PotionData potion = stack.getPotionData();
        if (potion == null) {
            potion = PotionData.water();
        }
        if (potion.splash()) {
            Vector3f direction = camera.getForward();
            Vector3f spawn = new Vector3f(position.x, position.y + EYE_HEIGHT - 0.1f, position.z)
                    .add(new Vector3f(direction).mul(0.35f));
            world.spawnSplashPotion(spawn.x, spawn.y, spawn.z,
                    direction.x * 0.5f,
                    direction.y * 0.5f + 0.1f,
                    direction.z * 0.5f,
                    null,
                    potion);
            if (!isCreative()) {
                stack.remove(1);
                if (stack.isEmpty()) {
                    inventory.getHotbar()[inventory.getSelectedSlot()] = null;
                }
            }
            startUseAnimation();
            return true;
        }

        PotionEffectResolver.applyToPlayer(this, potion, 1.0f);
        if (!isCreative()) {
            replaceHeldItemAfterPotionUse(world, stack);
        } else {
            startUseAnimation();
        }
        return true;
    }

    private void replaceHeldItemAfterPotionUse(World world, com.craftzero.inventory.ItemStack stack) {
        com.craftzero.inventory.ItemStack[] hotbar = inventory.getHotbar();
        int selected = inventory.getSelectedSlot();
        if (stack.getCount() <= 1) {
            hotbar[selected] = new com.craftzero.inventory.ItemStack(ItemType.GLASS_BOTTLE, 1);
        } else {
            stack.remove(1);
            com.craftzero.inventory.ItemStack bottle = new com.craftzero.inventory.ItemStack(ItemType.GLASS_BOTTLE, 1);
            if (!inventory.addItem(bottle) && !bottle.isEmpty()) {
                Vector3f forward = camera.getForward();
                world.spawnThrownStack(position.x + forward.x * 0.5f, position.y + EYE_HEIGHT, position.z + forward.z * 0.5f,
                        bottle, forward.x * 0.2f, 0.2f, forward.z * 0.2f);
            }
        }
        startUseAnimation();
    }

    private boolean handleTargetedItemUse(World world, com.craftzero.inventory.ItemStack stack, BlockType clickedBlock) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        ItemType type = stack.getType();
        if ((type == ItemType.MINECART || type == ItemType.CHEST_MINECART || type == ItemType.FURNACE_MINECART)
                && (clickedBlock == BlockType.RAIL || clickedBlock == BlockType.POWERED_RAIL
                        || clickedBlock == BlockType.DETECTOR_RAIL)) {
            if (world.placeMinecartOnRail(targetBlock.blockPos.x, targetBlock.blockPos.y, targetBlock.blockPos.z, type)) {
                consumePlacedStack(stack);
                return true;
            }
        }
        if (type.isRecord() && clickedBlock == BlockType.JUKEBOX) {
            TileEntity tile = world.getTileEntity(targetBlock.blockPos.x, targetBlock.blockPos.y, targetBlock.blockPos.z);
            if (tile instanceof JukeboxTileEntity jukebox && !jukebox.hasRecord()) {
                if (jukebox.insertRecord(stack)) {
                    if (!isCreative()) {
                        stack.remove(1);
                        if (stack.isEmpty()) {
                            inventory.getHotbar()[inventory.getSelectedSlot()] = null;
                        }
                    }
                    startUseAnimation();
                    return true;
                }
            }
        }
        if (type == ItemType.GLASS_BOTTLE && fillBottle(world, stack, clickedBlock)) {
            return true;
        }
        if (type == ItemType.WATER_BUCKET && clickedBlock == BlockType.CAULDRON) {
            int level = world.getBlockMetadata(targetBlock.blockPos.x, targetBlock.blockPos.y, targetBlock.blockPos.z);
            if (level < 3) {
                world.setBlock(targetBlock.blockPos.x, targetBlock.blockPos.y, targetBlock.blockPos.z,
                        BlockType.CAULDRON, 3);
                if (!isCreative()) {
                    replaceHeldItemAfterBucketUse(world, stack, ItemType.BUCKET);
                } else {
                    startUseAnimation();
                }
                return true;
            }
        }
        if (isHoe(type) && targetBlock.face == Block.FACE_TOP
                && (clickedBlock == BlockType.DIRT || clickedBlock == BlockType.GRASS)) {
            int aboveY = targetBlock.blockPos.y + 1;
            if (world.getBlockIfLoaded(targetBlock.blockPos.x, aboveY, targetBlock.blockPos.z, BlockType.AIR) != BlockType.AIR) {
                return false;
            }
            world.setBlock(targetBlock.blockPos.x, targetBlock.blockPos.y, targetBlock.blockPos.z, BlockType.FARMLAND);
            damageHeldDurable(stack);
            startUseAnimation();
            return true;
        }
        if (type == ItemType.SEEDS && clickedBlock == BlockType.FARMLAND && targetBlock.face == Block.FACE_TOP) {
            int cropY = targetBlock.blockPos.y + 1;
            if (world.getBlockIfLoaded(targetBlock.blockPos.x, cropY, targetBlock.blockPos.z, BlockType.AIR) != BlockType.AIR) {
                return false;
            }
            world.setBlock(targetBlock.blockPos.x, cropY, targetBlock.blockPos.z, BlockType.CROPS, 0);
            consumePlacedStack(stack);
            return true;
        }
        if (type == ItemType.FLINT_AND_STEEL && targetBlock.previousBlockPos != null) {
            if (clickedBlock == BlockType.TNT) {
                world.primeTnt(targetBlock.blockPos.x, targetBlock.blockPos.y, targetBlock.blockPos.z, 80);
                damageHeldDurable(stack);
                startUseAnimation();
                return true;
            }
            Vector3i pos = targetBlock.previousBlockPos;
            if (world.getBlockIfLoaded(pos.x, pos.y, pos.z, BlockType.AIR) != BlockType.AIR) {
                return false;
            }
            if (world.canPlaceBlockAt(pos.x, pos.y, pos.z, BlockType.FIRE, 0, null)) {
                world.setBlock(pos.x, pos.y, pos.z, BlockType.FIRE, 0);
                damageHeldDurable(stack);
                startUseAnimation();
                return true;
            }
        }
        if (type == ItemType.EYE_OF_ENDER && clickedBlock == BlockType.END_PORTAL_FRAME) {
            boolean placedEye = world.addEyeToEndPortalFrame(targetBlock.blockPos.x, targetBlock.blockPos.y,
                    targetBlock.blockPos.z);
            if (!placedEye) {
                return false;
            }
            consumePlacedStack(stack);
            startUseAnimation();
            return true;
        }
        return false;
    }

    private boolean fillBottle(World world, com.craftzero.inventory.ItemStack stack, BlockType clickedBlock) {
        boolean filled = false;
        if ((clickedBlock == BlockType.WATER || clickedBlock == BlockType.FLOWING_WATER)
                && world.getBlockMetadata(targetBlock.blockPos.x, targetBlock.blockPos.y, targetBlock.blockPos.z) == 0) {
            filled = true;
        } else if (clickedBlock == BlockType.CAULDRON) {
            int level = world.getBlockMetadata(targetBlock.blockPos.x, targetBlock.blockPos.y, targetBlock.blockPos.z);
            if (level > 0) {
                world.setBlock(targetBlock.blockPos.x, targetBlock.blockPos.y, targetBlock.blockPos.z,
                        BlockType.CAULDRON, level - 1);
                filled = true;
            }
        }
        if (!filled) {
            return false;
        }
        if (!isCreative()) {
            replaceHeldItemAfterBottleFill(world, stack);
        } else {
            startUseAnimation();
        }
        return true;
    }

    private void replaceHeldItemAfterBottleFill(World world, com.craftzero.inventory.ItemStack stack) {
        com.craftzero.inventory.ItemStack waterBottle = new com.craftzero.inventory.ItemStack(ItemType.POTION, 1);
        waterBottle.setPotionData(PotionData.water());
        com.craftzero.inventory.ItemStack[] hotbar = inventory.getHotbar();
        int selected = inventory.getSelectedSlot();
        if (stack.getCount() <= 1) {
            hotbar[selected] = waterBottle;
        } else {
            stack.remove(1);
            if (!inventory.addItem(waterBottle) && !waterBottle.isEmpty()) {
                Vector3f forward = camera.getForward();
                world.spawnThrownStack(position.x + forward.x * 0.5f, position.y + EYE_HEIGHT,
                        position.z + forward.z * 0.5f, waterBottle, forward.x * 0.2f, 0.2f, forward.z * 0.2f);
            }
        }
        startUseAnimation();
    }

    private boolean throwEyeOfEnder(World world, com.craftzero.inventory.ItemStack stack) {
        StructureGenerator.StructureLocation target = world.locateStructure(StructureType.STRONGHOLD,
                (int) Math.floor(position.x), (int) Math.floor(position.z));
        if (target == null) {
            return false;
        }
        if (!isCreative()) {
            stack.remove(1);
            if (stack.isEmpty()) {
                inventory.getHotbar()[inventory.getSelectedSlot()] = null;
            }
        }
        boolean drops = Math.random() >= 0.20;
        EyeOfEnderEntity eye = new EyeOfEnderEntity(position.x, position.y + EYE_HEIGHT - 0.2f, position.z,
                target.blockX() + 0.5f, target.blockY() + 1.0f, target.blockZ() + 0.5f, drops);
        Vector3f forward = camera.getForward();
        eye.setMotion(forward.x * 0.5f, forward.y * 0.5f + 0.15f, forward.z * 0.5f);
        world.spawnEntity(eye);
        startUseAnimation();
        return true;
    }

    private boolean equipArmorFromHand(com.craftzero.inventory.ItemStack stack) {
        ArmorSlot slot = ArmorMaterial.slotOf(stack.getType());
        if (slot == null) {
            return false;
        }
        int index = slot.getIndex();
        com.craftzero.inventory.ItemStack[] armor = inventory.getArmor();
        com.craftzero.inventory.ItemStack previous = armor[index];
        armor[index] = stack.copy();
        armor[index].setCount(1);
        inventory.getHotbar()[inventory.getSelectedSlot()] = previous;
        return true;
    }

    private void consumeFoodStack(World world, com.craftzero.inventory.ItemStack stack) {
        ItemType type = stack.getType();
        if (type == ItemType.MUSHROOM_STEW) {
            inventory.getHotbar()[inventory.getSelectedSlot()] = new com.craftzero.inventory.ItemStack(ItemType.BOWL, 1);
            return;
        }
        stack.remove(1);
        if (stack.isEmpty()) {
            inventory.getHotbar()[inventory.getSelectedSlot()] = null;
        }
    }

    private boolean isHoe(ItemType type) {
        return type == ItemType.WOODEN_HOE || type == ItemType.STONE_HOE || type == ItemType.IRON_HOE
                || type == ItemType.DIAMOND_HOE || type == ItemType.GOLD_HOE;
    }

    private void damageHeldDurable(com.craftzero.inventory.ItemStack stack) {
        if (isCreative() || stack == null || !stack.isDamageable()) {
            return;
        }
        if (useDurabilityWithEnchantments(stack)) {
            inventory.getHotbar()[inventory.getSelectedSlot()] = null;
        }
    }

    private boolean useDurabilityWithEnchantments(com.craftzero.inventory.ItemStack stack) {
        if (stack == null || !stack.isDamageable()) {
            return false;
        }
        if (EnchantmentResolver.shouldPreventDurabilityLoss(stack, random)) {
            return false;
        }
        return stack.useDurability();
    }

    private FoodValue foodValue(ItemType type) {
        return switch (type) {
            case APPLE -> new FoodValue(4, 2.4f);
            case BREAD -> new FoodValue(5, 6.0f);
            case MUSHROOM_STEW -> new FoodValue(6, 7.2f);
            case RAW_PORKCHOP, RAW_BEEF -> new FoodValue(3, 1.8f);
            case COOKED_PORKCHOP, STEAK -> new FoodValue(8, 12.8f);
            case GOLDEN_APPLE -> new FoodValue(10, 24.0f);
            case COOKIE, MELON_SLICE -> new FoodValue(2, 1.2f);
            case RAW_FISH, RAW_CHICKEN -> new FoodValue(2, 1.2f);
            case COOKED_FISH -> new FoodValue(5, 6.0f);
            case COOKED_CHICKEN -> new FoodValue(6, 7.2f);
            case ROTTEN_FLESH -> new FoodValue(4, 0.8f);
            default -> null;
        };
    }

    private record FoodValue(float hunger, float saturation) {
    }

    private boolean handleBucketUse(World world, com.craftzero.inventory.ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        ItemType type = stack.getType();
        if (type == ItemType.BUCKET) {
            ItemType filledBucket = world.pickupFluidSource(targetBlock.blockPos.x, targetBlock.blockPos.y,
                    targetBlock.blockPos.z);
            if (filledBucket == null) {
                return false;
            }
            if (isCreative()) {
                startUseAnimation();
                return true;
            }
            replaceHeldItemAfterBucketUse(world, stack, filledBucket);
            return true;
        }
        if (type != ItemType.WATER_BUCKET && type != ItemType.LAVA_BUCKET) {
            return false;
        }
        Vector3i placePos = targetBlock.previousBlockPos;
        if (placePos == null) {
            return false;
        }
        boolean water = type == ItemType.WATER_BUCKET;
        if (!world.placeFluidSource(placePos.x, placePos.y, placePos.z, water, boundingBox)) {
            return false;
        }
        if (isCreative()) {
            startUseAnimation();
            return true;
        }
        replaceHeldItemAfterBucketUse(world, stack, ItemType.BUCKET);
        return true;
    }

    private void replaceHeldItemAfterBucketUse(World world, com.craftzero.inventory.ItemStack stack, ItemType replacement) {
        com.craftzero.inventory.ItemStack[] hotbar = inventory.getHotbar();
        int selected = inventory.getSelectedSlot();
        if (stack.getCount() <= 1) {
            hotbar[selected] = new com.craftzero.inventory.ItemStack(replacement, 1);
        } else {
            stack.remove(1);
            com.craftzero.inventory.ItemStack replacementStack = new com.craftzero.inventory.ItemStack(replacement, 1);
            if (!inventory.addItem(replacementStack) && !replacementStack.isEmpty()) {
                Vector3f forward = camera.getForward();
                world.spawnThrownStack(position.x + forward.x * 0.5f, position.y + EYE_HEIGHT, position.z + forward.z * 0.5f,
                        replacementStack, forward.x * 0.2f, 0.2f, forward.z * 0.2f);
            }
        }
        startUseAnimation();
    }

    private boolean placeSign(World world, Vector3i placePos) {
        if (targetBlock.face == Block.FACE_TOP) {
            int metadata = getSignRotationMetadata();
            if (world.canPlaceBlockAt(placePos.x, placePos.y, placePos.z, BlockType.STANDING_SIGN, metadata, boundingBox)) {
                world.setBlock(placePos.x, placePos.y, placePos.z, BlockType.STANDING_SIGN, metadata);
                requestedSignEditPos = new Vector3i(placePos);
                return true;
            }
            return false;
        }
        if (targetBlock.face == Block.FACE_BOTTOM) {
            return false;
        }
        int metadata = targetBlock.face;
        if (world.canPlaceBlockAt(placePos.x, placePos.y, placePos.z, BlockType.WALL_SIGN, metadata, boundingBox)) {
            world.setBlock(placePos.x, placePos.y, placePos.z, BlockType.WALL_SIGN, metadata);
            requestedSignEditPos = new Vector3i(placePos);
            return true;
        }
        return false;
    }

    private void consumePlacedStack(com.craftzero.inventory.ItemStack stack) {
        if (isCreative()) {
            startUseAnimation();
            return;
        }
        stack.remove(1);
        if (stack.isEmpty()) {
            inventory.getHotbar()[inventory.getSelectedSlot()] = null;
            slotSwitchProgress = 0.0f;
            isRetracting = false;
            prevSlotSwitchProgress = 0.0f;
        } else {
            startUseAnimation();
        }
    }

    private int getPlacementMetadata(BlockType placedBlock) {
        if (placedBlock == BlockType.CHEST || placedBlock.isFurnace()
                || placedBlock == BlockType.DISPENSER
                || placedBlock == BlockType.PISTON
                || placedBlock == BlockType.STICKY_PISTON) {
            return getPlacementFacingMetadata();
        }
        if (placedBlock == BlockType.TORCH
                || placedBlock == BlockType.REDSTONE_TORCH_ON
                || placedBlock == BlockType.REDSTONE_TORCH_OFF) {
            return targetBlock.face == Block.FACE_TOP ? 5 : targetBlock.face;
        }
        if (placedBlock == BlockType.STONE_BUTTON) {
            return targetBlock.face == Block.FACE_TOP ? 5 : targetBlock.face;
        }
        if (placedBlock == BlockType.LEVER) {
            return targetBlock.face == Block.FACE_TOP ? 5 : targetBlock.face;
        }
        if (placedBlock == BlockType.REDSTONE_REPEATER_OFF || placedBlock == BlockType.REDSTONE_REPEATER_ON) {
            return getHorizontalFacingIndex();
        }
        if (placedBlock == BlockType.LADDER || placedBlock == BlockType.WALL_SIGN) {
            return targetBlock.face;
        }
        if (placedBlock == BlockType.TRAPDOOR) {
            int metadata = targetBlock.face == Block.FACE_TOP || targetBlock.face == Block.FACE_BOTTOM
                    ? getHorizontalFacingIndex()
                    : horizontalIndexFromFace(targetBlock.face);
            float hitY = targetBlock.hitPoint == null
                    ? 0.5f
                    : targetBlock.hitPoint.y - (float) Math.floor(targetBlock.hitPoint.y);
            boolean topHalf = targetBlock.face == Block.FACE_BOTTOM
                    || (targetBlock.face != Block.FACE_TOP && hitY > 0.5f);
            return topHalf ? metadata | 8 : metadata;
        }
        if (placedBlock.isStairs() || placedBlock.isFenceGate()) {
            return getHorizontalFacingIndex();
        }
        return 0;
    }

    /**
     * Attack a living entity with the held weapon.
     * Implements Minecraft pre-1.9 combat with proper spam-click prevention.
     * 
     * CRITICAL: Knockback is ONLY applied if damage() returns true.
     * This prevents spam-clicking from stacking knockback during immunity.
     */
    private void attackEntity(LivingEntity target) {
        if (target == null || target.isDead())
            return;

        // 1. Calculate Damage
        float damage = 1.0f; // Base fist damage
        com.craftzero.inventory.ItemStack heldItem = inventory.getItemInHand();
        com.craftzero.inventory.ToolType toolType = com.craftzero.inventory.ToolType.NONE;

        if (heldItem != null && heldItem.isTool()) {
            toolType = heldItem.getType().getToolType();
            damage = toolType.getAttackDamage();
        }
        damage += EnchantmentResolver.attackDamageBonus(heldItem, target);
        damage += stats.getAttackDamageBonus();
        damage = Math.max(0.0f, damage);

        // 2. Critical Hit Check (Pre-1.9 Logic)
        boolean isCritical = velocity.y < 0 && !onGround && !inWater && !flying;
        if (isCritical) {
            damage *= 1.5f;
            // TODO: Spawn crit particles here
        }

        // 3. Attempt to deal damage FIRST - capture the result
        // The mob returns 'false' if it is currently invulnerable
        boolean successfulHit = target.damage(damage,
                DamageSource.point(DamageSource.Type.PLAYER_ATTACK,
                        position.x, position.y + EYE_HEIGHT, position.z,
                        0.0f, 0.0f));

        // 4. ONLY Apply Knockback and Effects if the hit was successful
        // AND the target is still alive (no knockback on killing blow - Minecraft
        // behavior)
        // NOTE: Check health directly because isDead() isn't set until next tick()
        if (successfulHit && target.getHealth() > 0) {

            float knockbackStrength = CombatRules.PLAYER_ATTACK_KNOCKBACK;
            knockbackStrength += EnchantmentResolver.getLevel(heldItem, EnchantmentType.KNOCKBACK) * 0.4f;

            // Sprint Knockback (W-Tap Mechanic)
            // Only apply bonus if: 1) Currently sprinting, 2) Haven't used bonus since last
            // W release
            boolean applySprintBonus = sprinting && !sprintKnockbackUsed;
            if (applySprintBonus) {
                knockbackStrength += CombatRules.PLAYER_ATTACK_SPRINT_BONUS;
                sprintKnockbackUsed = true; // Mark as used - requires W release to reset
                sprinting = false; // Cancel sprint (Minecraft behavior)
            }

            // B. Calculate Vector (Direction from Player to Mob)
            float dx = target.getX() - position.x;
            float dz = target.getZ() - position.z;
            float dist = (float) Math.sqrt(dx * dx + dz * dz);

            // C. Apply the Physics
            if (dist > 0.001f) { // Avoid divide by zero
                // Normalize and Multiply
                float kbX = (dx / dist) * knockbackStrength;
                float kbZ = (dz / dist) * knockbackStrength;

                target.addMotion(kbX, CombatRules.PLAYER_ATTACK_VERTICAL_KNOCKBACK, kbZ);

                // If applied sprint bonus, slow down player slightly (impact feel)
                if (applySprintBonus) {
                    velocity.x *= 0.6f;
                    velocity.z *= 0.6f;
                }
            }
            int fireAspect = EnchantmentResolver.getLevel(heldItem, EnchantmentType.FIRE_ASPECT);
            if (fireAspect > 0) {
                target.setOnFire(fireAspect * 80);
            }
        }

        // D. Durability is only consumed on valid hits (even if target died)
        if (successfulHit && !isCreative()) {
            com.craftzero.inventory.ItemStack heldItem2 = inventory.getItemInHand();
            if (heldItem2 != null && heldItem2.isTool()) {
                boolean toolBroke = useDurabilityWithEnchantments(heldItem2);
                if (toolBroke) {
                    inventory.getHotbar()[inventory.getSelectedSlot()] = null;
                }
            }
        }
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
        prevUseProgress = useProgress;

        // Handle death state - only increment death time, skip all physics
        if (stats.isDead()) {
            // Drop items on first frame of death
            if (deathTime == 0) {
                dropAllItems();
            }
            deathTime++;
            velocity.set(0, 0, 0); // Stop all movement
            return;
        }

        updateSwing(deltaTime);
        updateUse(deltaTime);
        updateSlotSwitch(deltaTime);
        updateTurretRotation();

        // Check if held item type changed (from inventory manipulation)
        // This triggers animation when moving items in/out of selected slot
        com.craftzero.inventory.ItemStack currentHeld = inventory.getItemInHand();
        ItemType currentType = (currentHeld != null && !currentHeld.isEmpty())
                ? currentHeld.getType()
                : null;
        if (currentType != lastHeldItemType && slotSwitchProgress >= 1.0f && !isUsingItem) {
            // Item type changed and not already animating - trigger animation
            if (lastHeldItemType != null || currentType != null) {
                triggerItemChangeAnimation();
            }
        }
        lastHeldItemType = currentType;

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
        int blockX = (int) Math.floor(position.x);
        int blockZ = (int) Math.floor(position.z);
        inWater = world.getBlockIfLoaded(blockX, (int) Math.floor(position.y), blockZ, BlockType.AIR).isWater() ||
                world.getBlockIfLoaded(blockX, (int) Math.floor(position.y + 1), blockZ, BlockType.AIR).isWater();

        headInWater = world.getBlockIfLoaded(blockX, (int) Math.floor(position.y + EYE_HEIGHT), blockZ,
                BlockType.AIR).isWater();
        boolean onLadder = isTouchingLadder(world);

        // Exit Detection: Player has fully breached (feet left water) while moving up
        // Trigger bobbing cooldown here to force a sink phase upon re-entry
        if (wasInWater && !inWater && velocity.y > 0) {
            surfaceBobbingTimer = 0.3f; // Reduced from 0.5f for faster cycle
        }

        // Update state for visual rendering
        this.headInWaterState = headInWater;

        // Update breath logic
        if (!isCreative()) {
            stats.updateAir(headInWater, deltaTime);
        }

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
            if (isActionDown(GameSettings.KeyBinding.JUMP)) {
                // Only swim up if the "bobbing cooldown" is inactive
                if (surfaceBobbingTimer <= 0) {
                    // Swim up (Significantly Faster)
                    velocity.y += 35.0f * deltaTime;
                    if (velocity.y > 15.0f)
                        velocity.y = 15.0f;
                }
            } else if (isActionDown(GameSettings.KeyBinding.SNEAK)) {
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
        } else if (onLadder) {
            fallStartY = position.y;
            if (isActionDown(GameSettings.KeyBinding.JUMP) || isActionDown(GameSettings.KeyBinding.FORWARD)) {
                velocity.y = 3.0f;
            } else if (isActionDown(GameSettings.KeyBinding.SNEAK) || isActionDown(GameSettings.KeyBinding.BACK)) {
                velocity.y = -3.0f;
            } else {
                velocity.y = Math.max(velocity.y, -2.0f);
            }
            velocity.x *= 0.65f;
            velocity.z *= 0.65f;
        } else {
            // Standard Air/Ground physics
            velocity.y += GRAVITY * deltaTime;

            // Apply friction
            float friction = onGround ? FRICTION : AIR_FRICTION;
            velocity.x *= friction;
            velocity.z *= friction;
        }

        // Clamp horizontal velocity
        float maxHorizontal = (sprinting ? SPRINT_SPEED : WALK_SPEED) * stats.getMovementSpeedMultiplier();
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
        if (!isCreative() && onGround && !wasOnGround && !flying && !inWater) {
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
        if (!isCreative()) {
            stats.update(deltaTime, sprinting, isMoving);
        }

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
        // Track if the held slot was empty before pickup
        com.craftzero.inventory.ItemStack heldBefore = inventory.getItemInHand();
        boolean wasEmpty = heldBefore == null || heldBefore.isEmpty();

        List<DroppedItem> collected = world.collectNearbyItems(
                position.x, position.y + 0.9f, position.z, deltaTime, this);
        for (DroppedItem item : collected) {
            addStackToInventory(item.toItemStack());
        }

        // Trigger animation if held slot changed from empty to having item
        if (!collected.isEmpty() && wasEmpty) {
            com.craftzero.inventory.ItemStack heldAfter = inventory.getItemInHand();
            if (heldAfter != null && !heldAfter.isEmpty()) {
                triggerItemChangeAnimation();
            }
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
            if (viewBobbing && onGround && !flying) {
                float walk = prevDistanceWalked + (distanceWalked - prevDistanceWalked) * partialTick;
                float bob = prevLimbSwingAmount + (limbSwingAmount - prevLimbSwingAmount) * partialTick;
                float clampedBob = Math.min(1.0f, Math.max(0.0f, bob));
                interpX += (float) Math.sin(walk * 6.0f) * 0.006f * clampedBob;
                eyeY += Math.abs((float) Math.cos(walk * 6.0f)) * 0.012f * clampedBob;
            }
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
            } else {
                // Third person front
                camX = interpX + offsetX;
                camY = eyeY + offsetY;
                camZ = interpZ + offsetZ;
            }

            // Camera collision: prevent camera from being inside blocks
            if (world != null) {
                // Ray-cast from player eye to camera position
                float dx = camX - interpX;
                float dy = camY - eyeY;
                float dz = camZ - interpZ;
                float totalDist = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);

                // Step along the ray and check for solid blocks
                float step = 0.1f;
                float safeDistance = totalDist;
                for (float t = step; t <= totalDist; t += step) {
                    float ratio = t / totalDist;
                    int checkX = (int) Math.floor(interpX + dx * ratio);
                    int checkY = (int) Math.floor(eyeY + dy * ratio);
                    int checkZ = (int) Math.floor(interpZ + dz * ratio);

                    BlockType block = world.getBlockIfLoaded(checkX, checkY, checkZ, BlockType.AIR);
                    if (!block.isAir() && !block.isTransparent()) {
                        // Found a solid block - camera should stop before this
                        safeDistance = Math.max(0.5f, t - step);
                        break;
                    }
                }

                // Move camera to safe distance
                if (safeDistance < totalDist) {
                    float ratio = safeDistance / totalDist;
                    camX = interpX + dx * ratio;
                    camY = eyeY + dy * ratio;
                    camZ = interpZ + dz * ratio;
                }
            }

            camera.setPosition(camX, camY, camZ);

            if (cameraMode == 1) {
                camera.setYaw(yawToUse);
                camera.setPitch(pitchToUse);
            } else {
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
                    colliders.addAll(world.getCollisionBoxesIfLoaded(x, y, z));
                }
            }
        }

        // Note: Mob collision is handled by pushNearbyMobs() instead of rigid collision
        // This prevents player from standing on mobs while still allowing push
        // interactions

        return colliders;
    }

    private boolean isTouchingLadder(World world) {
        int minX = (int) Math.floor(boundingBox.getMin().x);
        int minY = (int) Math.floor(boundingBox.getMin().y);
        int minZ = (int) Math.floor(boundingBox.getMin().z);
        int maxX = (int) Math.floor(boundingBox.getMax().x);
        int maxY = (int) Math.floor(boundingBox.getMax().y);
        int maxZ = (int) Math.floor(boundingBox.getMax().z);
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    if (world.getBlockIfLoaded(x, y, z, BlockType.AIR) == BlockType.LADDER) {
                        return true;
                    }
                }
            }
        }
        return false;
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
                    for (AABB collider : world.getCollisionBoxesIfLoaded(x, y, z)) {
                        if (collider.intersects(testBox)) {
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

    public World getWorld() {
        return world;
    }

    public void setWorld(World world) {
        this.world = world;
    }

    public Vector3f getPosition() {
        return position;
    }

    public AABB getBoundingBox() {
        return boundingBox;
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

        // Reset rotation offset to prevent "Owl Neck" during transition
        float targetYaw = camera.getYaw();
        if (cameraMode == 2)
            targetYaw += 180;

        renderYawOffset = targetYaw;
        prevRenderYawOffset = targetYaw;
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

    public boolean isCreative() {
        return gameMode == GameMode.CREATIVE;
    }

    public boolean isHardcore() {
        return gameMode == GameMode.HARDCORE;
    }

    public GameMode getGameMode() {
        return gameMode;
    }

    public void setGameMode(GameMode gameMode) {
        this.gameMode = gameMode == null ? GameMode.SURVIVAL : gameMode;
        if (!isCreative()) {
            flying = false;
        }
    }

    public Difficulty getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(Difficulty difficulty) {
        this.difficulty = isHardcore() ? Difficulty.HARD : (difficulty == null ? Difficulty.EASY : difficulty);
    }

    public void applySettings(GameSettings settings) {
        if (settings == null) {
            return;
        }
        this.settings = settings;
        this.mouseSensitivityMultiplier = settings.mouseSensitivityMultiplier();
        this.invertMouse = settings.isInvertYMouse();
        this.viewBobbing = settings.isViewBobbing();
    }

    private boolean isActionDown(GameSettings.KeyBinding binding) {
        int code = settings.getKeyBinding(binding);
        return code < 0 ? Input.isButtonDown(mouseButtonFromKeyCode(code)) : Input.isKeyDown(code);
    }

    private boolean isActionPressed(GameSettings.KeyBinding binding) {
        int code = settings.getKeyBinding(binding);
        return code < 0 ? Input.isButtonPressed(mouseButtonFromKeyCode(code)) : Input.isKeyPressed(code);
    }

    private boolean isActionReleased(GameSettings.KeyBinding binding) {
        int code = settings.getKeyBinding(binding);
        return code < 0 ? Input.isButtonReleased(mouseButtonFromKeyCode(code)) : Input.isKeyReleased(code);
    }

    private static int mouseButtonFromKeyCode(int keyCode) {
        return Math.max(0, keyCode + 100);
    }

    public void setMouseSensitivityMultiplier(float multiplier) {
        this.mouseSensitivityMultiplier = Math.max(0.1f, multiplier);
    }

    public void setInvertMouse(boolean invertMouse) {
        this.invertMouse = invertMouse;
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

    public boolean hurt(float amount, float sourceX, float sourceY, float sourceZ,
            float horizontalKnockback, float verticalKnockback) {
        return hurt(amount, DamageSource.point(DamageSource.Type.GENERIC, sourceX, sourceY, sourceZ,
                horizontalKnockback, verticalKnockback));
    }

    public boolean hurt(float amount, DamageSource source) {
        if (isCreative()) {
            return false;
        }
        if (source == null) {
            source = DamageSource.generic();
        }
        if (source.type() == DamageSource.Type.FIRE && stats.hasEffect(StatusEffectType.FIRE_RESISTANCE)) {
            return false;
        }
        float scaledAmount = difficulty.scaleIncomingDamage(amount);
        float protectedAmount = applyArmorProtection(scaledAmount, source);
        boolean applied = stats.damage(protectedAmount);
        if (!applied) {
            return false;
        }
        damageArmor();

        float dx = position.x - source.sourceX();
        float dz = position.z - source.sourceZ();
        float dist = (float) Math.sqrt(dx * dx + dz * dz);
        if (source.hasPosition() && dist > 0.001f && source.horizontalKnockback() > 0.0f) {
            velocity.x += (dx / dist) * source.horizontalKnockback();
            velocity.z += (dz / dist) * source.horizontalKnockback();
        }
        if (source.verticalKnockback() > 0.0f) {
            velocity.y = Math.max(velocity.y, source.verticalKnockback());
        }
        return true;
    }

    private float applyArmorProtection(float damage, DamageSource source) {
        return ArmorCalculator.reduceDamage(damage, inventory.getArmor(), source);
    }

    private void damageArmor() {
        com.craftzero.inventory.ItemStack[] armor = inventory.getArmor();
        for (int i = 0; i < armor.length; i++) {
            com.craftzero.inventory.ItemStack stack = armor[i];
            if (stack != null && stack.isDamageable() && useDurabilityWithEnchantments(stack)) {
                armor[i] = null;
            }
        }
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
    public boolean addToInventory(ItemType type, int count) {
        if (type == null || count <= 0) {
            return false;
        }
        return addStackToInventory(new ItemStack(type, count));
    }

    public boolean addStackToInventory(com.craftzero.inventory.ItemStack stack) {
        return inventory.addItem(stack);
    }

    public boolean canAddStackToInventory(ItemStack stack) {
        return inventory.canAddItem(stack);
    }

    /**
     * Check if inventory has space for the given item.
     * Does not modify inventory, just checks.
     */
    public boolean canAddToInventory(ItemType type, int count) {
        if (type == null || count <= 0) {
            return false;
        }
        return canAddStackToInventory(new ItemStack(type, count));
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
     * @return The item type dropped, or null if slot was empty
     */
    public com.craftzero.inventory.ItemStack dropOneFromHand() {
        com.craftzero.inventory.ItemStack[] hotbar = inventory.getHotbar();
        int slot = inventory.getSelectedSlot();

        if (hotbar[slot] != null && !hotbar[slot].isEmpty()) {
            if (isCreative()) {
                return ItemStackOps.copyWithCount(hotbar[slot], 1);
            }
            com.craftzero.inventory.ItemStack dropped = ItemStackOps.splitOne(hotbar[slot]);

            if (hotbar[slot].isEmpty()) {
                hotbar[slot] = null;
            }

            return dropped;
        }

        return null;
    }

    /**
     * Check if player wants to open crafting table (right-clicked on one).
     */
    public boolean wantsCraftingTable() {
        return wantsCraftingTable;
    }

    public Vector3i getAndClearChestOpenRequest() {
        Vector3i value = requestedChestPos != null ? new Vector3i(requestedChestPos) : null;
        requestedChestPos = null;
        return value;
    }

    public Vector3i getAndClearFurnaceOpenRequest() {
        Vector3i value = requestedFurnacePos != null ? new Vector3i(requestedFurnacePos) : null;
        requestedFurnacePos = null;
        return value;
    }

    public Vector3i getAndClearBrewingStandOpenRequest() {
        Vector3i value = requestedBrewingStandPos != null ? new Vector3i(requestedBrewingStandPos) : null;
        requestedBrewingStandPos = null;
        return value;
    }

    public Vector3i getAndClearEnchantingTableOpenRequest() {
        Vector3i value = requestedEnchantingTablePos != null ? new Vector3i(requestedEnchantingTablePos) : null;
        requestedEnchantingTablePos = null;
        return value;
    }

    public Vector3i getAndClearSignEditRequest() {
        Vector3i value = requestedSignEditPos != null ? new Vector3i(requestedSignEditPos) : null;
        requestedSignEditPos = null;
        return value;
    }

    public Vector3i getAndClearBedUseRequest() {
        Vector3i value = requestedBedUsePos != null ? new Vector3i(requestedBedUsePos) : null;
        requestedBedUsePos = null;
        return value;
    }

    public void setSpawnPosition(float x, float y, float z) {
        this.spawnX = x;
        this.spawnY = y;
        this.spawnZ = z;
    }

    public float getSpawnX() {
        return spawnX;
    }

    public float getSpawnY() {
        return spawnY;
    }

    public float getSpawnZ() {
        return spawnZ;
    }

    private int getPlacementFacingMetadata() {
        float yaw = camera.getYaw() % 360.0f;
        if (yaw < 0) {
            yaw += 360.0f;
        }
        if (yaw >= 315.0f || yaw < 45.0f) {
            return 3; // Player looking north, block front faces south
        }
        if (yaw < 135.0f) {
            return 4; // Player looking east, block front faces west
        }
        if (yaw < 225.0f) {
            return 2; // Player looking south, block front faces north
        }
        return 5; // Player looking west, block front faces east
    }

    private int getHorizontalFacingIndex() {
        float yaw = camera.getYaw() % 360.0f;
        if (yaw < 0) {
            yaw += 360.0f;
        }
        if (yaw >= 315.0f || yaw < 45.0f) {
            return 0;
        }
        if (yaw < 135.0f) {
            return 1;
        }
        if (yaw < 225.0f) {
            return 2;
        }
        return 3;
    }

    private int getSignRotationMetadata() {
        float yaw = camera.getYaw() % 360.0f;
        if (yaw < 0) {
            yaw += 360.0f;
        }
        return Math.round(yaw / 22.5f) & 15;
    }

    private int horizontalIndexFromFace(int face) {
        return switch (face) {
            case Block.FACE_NORTH -> 0;
            case Block.FACE_EAST -> 1;
            case Block.FACE_SOUTH -> 2;
            case Block.FACE_WEST -> 3;
            default -> 0;
        };
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
            // Faster swing when holding an item, normal speed when empty hand
            com.craftzero.inventory.ItemStack heldItem = inventory.getItemInHand();
            boolean holdingItem = heldItem != null && !heldItem.isEmpty();
            float swingSpeed = holdingItem ? 4.0f : 3.5f;

            swingProgress += deltaTime * swingSpeed;
            if (swingProgress >= 1.0f) {
                swingProgress = 0.0f;
                prevSwingProgress = 0.0f; // Reset prev too to avoid ghosting interpolation
                isSwinging = false;
                isMiningSwing = false;
            }
        } else {
            swingProgress = 0.0f;
            prevSwingProgress = 0.0f; // Keep in sync
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

            // Calculate diff from Camera Yaw (0 North, 180 South)
            float lookYaw = camera.getYaw();
            float diffFromLook = moveYaw - lookYaw;
            while (diffFromLook >= 180)
                diffFromLook -= 360;
            while (diffFromLook < -180)
                diffFromLook += 360;

            float targetYaw = moveYaw;

            if (cameraMode == 2) {
                // "When I press S make it behave like W" -> Backpedal (Face Forward while
                // moving Back)
                // If moving Towards Camera (Diff < 45), Flip to Face Away.
                if (Math.abs(diffFromLook) < 45) {
                    targetYaw += 180;
                }
            } else if (cameraMode == 1) {
                // "Make it also do that in mode 1" -> Backpedal
                // In Mode 1 (Back View), S moves Opposite to Camera (Diff > 135).
                // Flip to Face Forward (same direction as Camera).
                if (Math.abs(diffFromLook) > 135) {
                    targetYaw += 180;
                }
            }

            float bodyDiff = targetYaw - renderYawOffset;
            while (bodyDiff >= 180)
                bodyDiff -= 360;
            while (bodyDiff < -180)
                bodyDiff += 360;
            renderYawOffset += bodyDiff * 0.15f;
        } else {
            // Not moving - body slowly returns to face look direction (Idle)
            // In Mode 2, Face Camera (Camera + 180)
            float lookYaw = camera.getYaw();
            if (cameraMode == 2) {
                lookYaw += 180;
            }
            float bodyDiff = lookYaw - renderYawOffset;
            while (bodyDiff >= 180)
                bodyDiff -= 360;
            while (bodyDiff < -180)
                bodyDiff += 360;
            renderYawOffset += bodyDiff * 0.05f;
        }

        // Clamp head relative to body - account for Mode 2 offset
        float targetLookYaw = camera.getYaw();
        if (cameraMode == 2)
            targetLookYaw += 180;

        float headDiff = targetLookYaw - renderYawOffset;
        while (headDiff >= 180)
            headDiff -= 360;
        while (headDiff < -180)
            headDiff += 360;

        if (headDiff > 30)
            renderYawOffset = targetLookYaw - 30;
        if (headDiff < -30)
            renderYawOffset = targetLookYaw + 30;
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

    private void updateUse(float deltaTime) {
        if (isDrawingBow) {
            useProgress = Math.min(1.0f, bowDrawTime / BOW_MAX_DRAW_TIME);
            return;
        }

        // Update use cooldown
        if (useCooldown > 0) {
            useCooldown -= deltaTime;
        }

        if (isUsingItem) {
            // Use animation is faster than swing (about 0.2s)
            float useSpeed = 5.0f;

            useProgress += deltaTime * useSpeed;
            if (useProgress >= 1.0f) {
                useProgress = 0.0f;
                isUsingItem = false;
            }
        } else {
            useProgress = 0.0f;
        }
    }

    /**
     * Start the "use" animation (for block placing).
     */
    public void startUseAnimation() {
        // 150ms animation cooldown
        if (useCooldown > 0) {
            return;
        }

        isUsingItem = true;
        useProgress = 0;
        prevUseProgress = 0;
        useCooldown = 0.15f;
    }

    /**
     * Get the interpolated use progress for rendering.
     */
    public float getUseProgress(float partialTick) {
        return prevUseProgress + (useProgress - prevUseProgress) * partialTick;
    }

    /**
     * Check if the player is currently using an item (placing block).
     */
    public boolean isUsingItem() {
        return isUsingItem;
    }

    // ============== Death State ==============

    /**
     * Check if player is dead.
     */
    public boolean isDead() {
        return stats.isDead();
    }

    /**
     * Get death time in ticks (for death animation).
     */
    public int getDeathTime() {
        return deathTime;
    }

    /**
     * Respawn the player at spawn point with full health.
     */
    public void respawn() {
        // Reset stats (health, hunger, etc.)
        stats.respawn();

        // Clear inventory just in case (though it should be empty from death drop)
        inventory.clearInventory();

        // Reset death animation
        deathTime = 0;

        // Reset position to spawn point
        position.set(spawnX, spawnY, spawnZ);
        prevPosition.set(position);

        // Reset velocity
        velocity.set(0, 0, 0);

        // Reset physics state
        onGround = false;
        flying = false;
        sprinting = false;
        sneaking = false;
        fallStartY = spawnY;

        // Update bounding box
        boundingBox = createBoundingBox();

        // Reset camera
        camera.setPosition(spawnX, spawnY + EYE_HEIGHT, spawnZ);
    }

    // ============== Slot Switch Animation ==============

    private int pendingSlot = -1;

    /**
     * Trigger the item change animation (retract current, appear new).
     * Use this when the selected slot's contents change (pickup, inventory move).
     */
    public void triggerItemChangeAnimation() {
        // Only animate if not already animating
        if (slotSwitchProgress < 1.0f) {
            return; // Already animating
        }
        // Stay on same slot, just play animation
        pendingSlot = inventory.getSelectedSlot();
        isRetracting = true;
        slotSwitchProgress = 1.0f;
        prevSlotSwitchProgress = 1.0f;
    }

    /**
     * Trigger a slot switch animation. The hand retracts, slot changes, then
     * reappears.
     * Skip animation if switching between two empty slots (no visual change).
     */
    private void triggerSlotSwitch(int newSlot) {
        // Check if both current and new slots are empty - skip animation if so
        com.craftzero.inventory.ItemStack currentItem = inventory.getItemInHand();
        com.craftzero.inventory.ItemStack[] hotbar = inventory.getHotbar();
        com.craftzero.inventory.ItemStack newItem = (newSlot >= 0 && newSlot < hotbar.length) ? hotbar[newSlot] : null;

        boolean currentEmpty = currentItem == null || currentItem.isEmpty();
        boolean newEmpty = newItem == null || newItem.isEmpty();

        if (currentEmpty && newEmpty) {
            // Both empty - just switch immediately without animation
            inventory.setSelectedSlot(newSlot);
            return;
        }

        if (slotSwitchProgress < 1.0f) {
            // Already animating - immediately switch and continue
            inventory.setSelectedSlot(newSlot);
            pendingSlot = -1;
            return;
        }
        pendingSlot = newSlot;
        isRetracting = true;
        slotSwitchProgress = 1.0f;
        prevSlotSwitchProgress = 1.0f;
    }

    private void updateSlotSwitch(float deltaTime) {
        prevSlotSwitchProgress = slotSwitchProgress;

        float speed = 8.0f; // Fast animation (~0.125s each way)

        if (isRetracting) {
            // Hand going down (1.0 -> 0.0)
            slotSwitchProgress -= deltaTime * speed;
            if (slotSwitchProgress <= 0.0f) {
                slotSwitchProgress = 0.0f;
                isRetracting = false;
                // Switch the actual slot at the bottom
                if (pendingSlot >= 0) {
                    inventory.setSelectedSlot(pendingSlot);
                    lastSelectedSlot = pendingSlot;
                    pendingSlot = -1;
                }
            }
        } else if (slotSwitchProgress < 1.0f) {
            // Hand coming back up (0.0 -> 1.0)
            slotSwitchProgress += deltaTime * speed;
            if (slotSwitchProgress >= 1.0f) {
                slotSwitchProgress = 1.0f;
            }
        }
    }

    /**
     * Get the interpolated slot switch progress for rendering.
     * 0.0 = fully retracted, 1.0 = fully visible
     */
    public float getSlotSwitchProgress(float partialTick) {
        return prevSlotSwitchProgress + (slotSwitchProgress - prevSlotSwitchProgress) * partialTick;
    }

    /**
     * Drop all items in inventory into the world (on death).
     */
    public void dropAllItems() {
        if (world == null)
            return;

        float dropX = position.x;
        float dropY = position.y + 1.0f; // Drop at body height
        float dropZ = position.z;

        java.util.Random rand = new java.util.Random();
        int deathXp = stats.getProgression().deathDropExperience();
        if (deathXp > 0) {
            world.spawnExperience(dropX, dropY, dropZ, deathXp);
            stats.getProgression().clearExperience();
        }

        // Drop hotbar items
        com.craftzero.inventory.ItemStack[] hotbar = inventory.getHotbar();
        for (int i = 0; i < hotbar.length; i++) {
            if (hotbar[i] != null && !hotbar[i].isEmpty()) {
                // Random velocity for scatter effect
                float velX = (rand.nextFloat() - 0.5f) * 3.0f;
                float velY = rand.nextFloat() * 3.0f + 2.0f;
                float velZ = (rand.nextFloat() - 0.5f) * 3.0f;
                world.spawnThrownStack(dropX, dropY, dropZ, hotbar[i].copy(), velX, velY, velZ);
            }
        }

        // Drop main inventory items
        com.craftzero.inventory.ItemStack[] main = inventory.getMainInventory();
        for (int i = 0; i < main.length; i++) {
            if (main[i] != null && !main[i].isEmpty()) {
                float velX = (rand.nextFloat() - 0.5f) * 3.0f;
                float velY = rand.nextFloat() * 3.0f + 2.0f;
                float velZ = (rand.nextFloat() - 0.5f) * 3.0f;
                world.spawnThrownStack(dropX, dropY, dropZ, main[i].copy(), velX, velY, velZ);
            }
        }

        // Drop crafting grid items
        com.craftzero.inventory.ItemStack[] crafting = inventory.getCraftingGrid();
        for (int i = 0; i < crafting.length; i++) {
            if (crafting[i] != null && !crafting[i].isEmpty()) {
                float velX = (rand.nextFloat() - 0.5f) * 3.0f;
                float velY = rand.nextFloat() * 3.0f + 2.0f;
                float velZ = (rand.nextFloat() - 0.5f) * 3.0f;
                world.spawnThrownStack(dropX, dropY, dropZ, crafting[i].copy(), velX, velY, velZ);
            }
        }

        // Drop any item held in cursor
        com.craftzero.inventory.ItemStack cursor = inventory.getCursorItem();
        if (cursor != null && !cursor.isEmpty()) {
            float velX = (rand.nextFloat() - 0.5f) * 3.0f;
            float velY = rand.nextFloat() * 3.0f + 2.0f;
            float velZ = (rand.nextFloat() - 0.5f) * 3.0f;
            world.spawnThrownStack(dropX, dropY, dropZ, cursor.copy(), velX, velY, velZ);
            inventory.setCursorItem(null);
        }

        // Clear inventory after dropping
        inventory.clearInventory();
    }
}
