package com.craftzero.main;

import com.craftzero.engine.Input;
import com.craftzero.combat.DamageSource;
import com.craftzero.entity.ArrowEntity;
import com.craftzero.entity.BoatEntity;
import com.craftzero.entity.ChestMinecartEntity;
import com.craftzero.entity.DroppedItem;
import com.craftzero.entity.Entity;
import com.craftzero.entity.EndCrystalEntity;
import com.craftzero.entity.EnderPearlEntity;
import com.craftzero.entity.EyeOfEnderEntity;
import com.craftzero.entity.FishingHookEntity;
import com.craftzero.entity.FireballEntity;
import com.craftzero.entity.FurnaceMinecartEntity;
import com.craftzero.entity.LivingEntity;
import com.craftzero.entity.MinecartEntity;
import com.craftzero.entity.PaintingEntity;
import com.craftzero.entity.ThrownItemEntity;
import com.craftzero.entity.mob.Cow;
import com.craftzero.entity.mob.Mob;
import com.craftzero.entity.mob.Mooshroom;
import com.craftzero.entity.mob.Pig;
import com.craftzero.entity.mob.Sheep;
import com.craftzero.entity.mob.Wolf;
import com.craftzero.graphics.Camera;
import com.craftzero.inventory.Inventory;
import com.craftzero.inventory.ItemStack;
import com.craftzero.inventory.ItemStackOps;
import com.craftzero.inventory.ItemType;
import com.craftzero.inventory.MapItemData;
import com.craftzero.inventory.ToolType;
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
import com.craftzero.progression.StatusEffectMath;
import com.craftzero.progression.StatusEffectType;
import com.craftzero.progression.StatusEffectVisuals;
import com.craftzero.world.Block;
import com.craftzero.world.BlockHarvestRules;
import com.craftzero.world.BlockType;
import com.craftzero.world.BlockShape;
import com.craftzero.world.StructureGenerator;
import com.craftzero.world.StructureType;

import com.craftzero.world.World;
import com.craftzero.world.WorldParticle;
import com.craftzero.world.WorldSoundEvent;
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
    public enum EntityActionType {
        ATTACK,
        USE
    }

    public enum ItemActionType {
        BOW,
        THROW_ITEM,
        ENDER_PEARL,
        EYE_OF_ENDER,
        SPLASH_POTION,
        CONSUME_FOOD,
        DRINK_MILK,
        DRINK_POTION,
        USE_MAP,
        EQUIP_ARMOR,
        PLAY_NOTE_BLOCK,
        TUNE_NOTE_BLOCK,
        INSERT_RECORD,
        EJECT_RECORD,
        PLACE_BOAT,
        PLACE_PAINTING,
        PLACE_MINECART,
        FISHING_CAST,
        FISHING_REEL
    }

    public record EntityActionRecord(Entity entity, EntityActionType actionType) {
    }

    public record ItemActionRecord(ItemActionType actionType, ItemType itemType,
            float directionX, float directionY, float directionZ, float power,
            int blockX, int blockY, int blockZ, int blockFace) {
    }

    public interface PlayerActionHandler {
        PlayerHit findAttackTarget(Vector3f origin, Vector3f direction, float maxDistance);

        boolean attackPlayer(PlayerHit hit, PlayerAttack attack);
    }

    public interface DeathDropHandler {
        void dropExperience(float x, float y, float z, int amount);

        void dropStack(int sourceSlot, float x, float y, float z, ItemStack stack,
                float velocityX, float velocityY, float velocityZ, int pickupDelayTicks);
    }

    public record PlayerHit(String playerId, float distance, Vector3f hitPoint) {
        public PlayerHit {
            playerId = playerId == null ? "" : playerId;
            distance = Math.max(0.0f, distance);
            hitPoint = hitPoint == null ? new Vector3f() : new Vector3f(hitPoint);
        }

        public boolean hit() {
            return !playerId.isBlank();
        }
    }

    public record PlayerAttack(float damage, float sourceX, float sourceY, float sourceZ,
            float horizontalKnockback, float verticalKnockback, int fireTicks, boolean sprintKnockback) {
        public PlayerAttack {
            damage = Math.max(0.0f, damage);
            horizontalKnockback = Math.max(0.0f, horizontalKnockback);
            verticalKnockback = Math.max(0.0f, verticalKnockback);
            fireTicks = Math.max(0, fireTicks);
        }
    }

    // Player dimensions (Minecraft standard)
    private static final float WIDTH = 0.6f;
    private static final float HEIGHT = 1.8f;
    private static final float EYE_HEIGHT = 1.62f;
    private static final float SLEEPING_BED_HEIGHT = 0.5625f;
    private static final float SLEEPING_EYE_HEIGHT = 0.2f;

    // Physics constants
    private static final float GRAVITY = -32.0f;
    private static final float JUMP_VELOCITY = 9.5f;
    private static final float JUMP_BOOST_VELOCITY_BONUS = 2.0f;
    private static final float WALK_SPEED = 4.317f;
    private static final float SPRINT_SPEED = 5.612f;
    private static final float SNEAK_SPEED = WALK_SPEED * 0.3f;
    private static final float BASE_GROUND_FRICTION = 0.91f;
    private static final float DEFAULT_BLOCK_SLIPPERINESS = 0.6f;
    private static final float ICE_BLOCK_SLIPPERINESS = 0.98f;
    private static final float AIR_FRICTION = 0.98f;
    private static final float AIR_ACCELERATION_PER_SECOND = 6.0f;
    private static final float WATER_ACCELERATION_PER_SECOND = 8.0f;
    private static final float LAVA_ACCELERATION_PER_SECOND = 2.0f;
    private static final float NORMAL_GROUND_NO_INPUT_BRAKE = 0.35f;
    private static final float NORMAL_GROUND_STOP_SPEED = 0.08f;
    private static final float SLIPPERY_GROUND_FRICTION_THRESHOLD = 0.75f;
    private static final float COBWEB_HORIZONTAL_DRAG = 0.25f;
    private static final float COBWEB_VERTICAL_DRAG = 0.05f;
    private static final float SOUL_SAND_HORIZONTAL_DRAG = 0.4f;
    private static final float CLIMBABLE_AXIS_SPEED = 0.15f * 20.0f;
    private static final float CLIMBABLE_WALL_BUMP_SPEED = 0.2f * 20.0f;
    private static final float WATER_DRAG = 0.8f;
    private static final float WATER_GRAVITY_ACCELERATION = -0.02f * 20.0f * 20.0f;
    private static final float WATER_SWIM_UP_ACCELERATION = 0.04f * 20.0f * 20.0f;
    private static final float FLUID_CURRENT_ACCELERATION = World.FLUID_CURRENT_PUSH_PER_TICK * 20.0f * 20.0f;
    private static final float LAVA_DRAG = 0.5f;
    private static final float LAVA_GRAVITY_ACCELERATION = -0.02f * 20.0f * 20.0f;
    private static final float LAVA_SWIM_UP_ACCELERATION = 0.04f * 20.0f * 20.0f;
    private static final float BOAT_COLLISION_MIN_AXIS = 0.01f;
    private static final float BOAT_COLLISION_IMPULSE = 0.05f;
    private static final float LIVING_COLLISION_MIN_AXIS = 0.01f;
    private static final float LIVING_COLLISION_IMPULSE = 0.05f;
    private static final float STEP_HEIGHT = 0.5f;
    private static final float COLLISION_EPSILON = 0.0001f;

    // Sprint double-tap detection
    private static final float DOUBLE_TAP_TIME = 0.3f; // 300ms window for double-tap

    // Mouse sensitivity
    private static final float MOUSE_SENSITIVITY = 0.15f;

    // Block interaction
    private static final float REACH_DISTANCE = 5.0f; // Block reach (mining/placing)
    private static final float ENTITY_REACH = 3.0f; // Entity attack reach (Minecraft standard)
    private static final float BREAK_COOLDOWN = 0.25f;
    private static final float PLACE_COOLDOWN = 0.25f;
    private static final float BLOCK_BREAK_TICKS_PER_SECOND = 20.0f;
    private static final float HARVESTABLE_BLOCK_STRENGTH_DIVISOR = 30.0f;
    private static final float NON_HARVESTABLE_BLOCK_STRENGTH_DIVISOR = 100.0f;
    private static final float RELEASE_ONE_MINING_PENALTY = 5.0f;
    private static final float COBWEB_CUTTING_SPEED = 15.0f;
    private static final float SHEARS_LEAVES_SPEED = 15.0f;
    private static final float SHEARS_WOOL_SPEED = 5.0f;
    static final float MOOSHROOM_SHEAR_PARTICLE_SCALE = 1.0f;
    static final int MOOSHROOM_SHEAR_PARTICLE_LIFETIME_TICKS = 16;
    private static final float BOW_MAX_DRAW_TIME = 1.0f;
    private static final float BOW_MIN_DRAW_TIME = 0.10f;
    private static final float CONSUMABLE_USE_TIME = 1.6f;
    private static final float CONSUMABLE_TICK_SOUND_INTERVAL = 0.2f;
    private static final int CONSUMABLE_TICK_CRUMB_PARTICLES = 1;
    private static final int CONSUMABLE_FINISH_CRUMB_PARTICLES = 16;
    private static final float CONSUMABLE_CRUMB_POSITION_FORWARD = 0.6f;
    private static final float CONSUMABLE_CRUMB_POSITION_HORIZONTAL_SPREAD = 0.3f;
    private static final float CONSUMABLE_CRUMB_POSITION_MIN_Y = -0.3f;
    private static final float CONSUMABLE_CRUMB_POSITION_Y_SPREAD = 0.6f;
    private static final float CONSUMABLE_CRUMB_MOTION_HORIZONTAL_SPREAD = 0.1f;
    private static final float CONSUMABLE_CRUMB_MOTION_MIN_Y = 0.15f;
    private static final float CONSUMABLE_CRUMB_MOTION_Y_SPREAD = 0.1f;
    private static final float CONSUMABLE_CRUMB_SCALE = 0.10f;
    private static final int CONSUMABLE_CRUMB_LIFETIME_TICKS = 12;
    static final float CAKE_SLICE_HUNGER = 2.0f;
    static final float CAKE_SLICE_SATURATION = 0.4f;
    private static final float SWING_ANIMATION_TICKS = 6.0f;
    private static final float SWING_ANIMATION_SPEED = 20.0f / SWING_ANIMATION_TICKS;
    private static final float STATUS_EFFECT_PARTICLE_INTERVAL_SECONDS = 0.25f;
    private static final float STATUS_EFFECT_PARTICLE_SCALE = 0.20f;
    private static final int STATUS_EFFECT_PARTICLE_LIFETIME_TICKS = 20;
    private static final float HURT_FLASH_DURATION_SECONDS = 0.5f;
    private static final float STEP_SOUND_DISTANCE_SCALE = 0.6f;
    private static final float FIRE_TICKS_PER_SECOND = 20.0f;
    private static final int FIRE_DAMAGE_INTERVAL_TICKS = 20;
    private static final float FIRE_DAMAGE = 1.0f;

    private Vector3f position;
    private Vector3f prevPosition; // Previous position for render interpolation
    private Vector3f velocity;
    private Camera camera;
    private AABB boundingBox;

    private boolean onGround;
    private boolean sprinting;
    private boolean sneaking;
    private boolean flying; // Creative mode flight
    private boolean movementInputActive;
    private boolean inputForwardDown;
    private boolean inputBackwardDown;
    private boolean inputLeftDown;
    private boolean inputRightDown;
    private boolean inputJumpDown;
    private GameMode gameMode = GameMode.SURVIVAL;
    private Difficulty difficulty = Difficulty.EASY;
    private GameSettings settings = GameSettings.defaults();
    private String playerName = GameSettings.DEFAULT_PLAYER_NAME;
    private float mouseSensitivityMultiplier = 1.0f;
    private boolean invertMouse;
    private boolean viewBobbing = true;
    private boolean smoothCamera;
    private float smoothMouseDeltaX;
    private float smoothMouseDeltaY;

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
    private boolean wantsCraftingTable; // Legacy flag for opening crafting table
    private Vector3i requestedCraftingTablePos;
    private Vector3i requestedChestPos;
    private ChestMinecartEntity requestedChestMinecart;
    private Vector3i requestedFurnacePos;
    private Vector3i requestedDispenserPos;
    private Vector3i requestedBrewingStandPos;
    private Vector3i requestedEnchantingTablePos;
    private Vector3i requestedSignEditPos;
    private Vector3i requestedBedUsePos;
    private MinecartEntity ridingMinecart;
    private BoatEntity ridingBoat;
    private Pig ridingPig;
    private com.craftzero.entity.Entity lastDismountedVehicle;
    private float mountedForwardInput;
    private float mountedStrafeInput;
    private float mountedYawInput;
    private FishingHookEntity fishingHook;
    private float minecartRideStartX;
    private float minecartRideStartZ;
    private boolean trackingMinecartRide;

    // World reference for lighting lookups
    private World world;

    // Water State (Promoted to fields for access in handleInput)
    private boolean inWater;
    private boolean inLava;
    private boolean headInWater;
    private boolean wasInWaterForParticles;
    private int fireTicks;
    private int fireDamageCooldownTicks;
    private float fireTickAccumulator;

    // Third-person camera support
    private int cameraMode = 0; // 0=First person, 1=Third person back, 2=Third person front
    private long lastCameraToggleTime = 0;
    private float distanceWalked = 0.0f; // For walk animation
    private float prevDistanceWalked = 0.0f; // For animation interpolation
    private float stepSoundDistance = 0.0f;
    private int nextStepSoundDistance = 1;
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
    private float renderYawOffset; // The "Turret" body yaw
    private float prevRenderYawOffset;
    private float limbSwingAmount;
    private float prevLimbSwingAmount;
    private float experiencePickupCooldown;

    // Use animation state (for block placing)
    private boolean isUsingItem;
    private float useProgress;
    private float prevUseProgress;
    private float useCooldown;
    private boolean isDrawingBow;
    private boolean isBlockingItem;
    private float bowDrawTime;
    private float statusEffectParticleTimer;
    private float hurtFlashTimer;
    private boolean isConsumingItem;
    private ItemType consumingItemType;
    private int consumingSlot = -1;
    private float consumableUseTime;
    private float consumableTickSoundTimer;
    private boolean consumableUseHeldThisFrame;

    // Slot switch animation state (for smooth item change)
    private int lastSelectedSlot = 0;
    private float slotSwitchProgress = 1.0f; // 0 = fully retracted, 1 = fully visible
    private float prevSlotSwitchProgress = 1.0f;
    private boolean isRetracting = false; // true = going down, false = coming up
    private ItemType lastHeldItemType = null; // Track for inventory changes
    private final Random random = new Random();
    private final List<EntityActionRecord> entityActionRecords = new ArrayList<>();
    private final List<ItemActionRecord> itemActionRecords = new ArrayList<>();
    private PlayerActionHandler playerActionHandler;
    private DeathDropHandler deathDropHandler;

    // Death state
    private int deathTime = 0; // Ticks since death (for death animation)
    private float spawnX, spawnY, spawnZ; // Spawn point for respawn
    private boolean bedSpawnSet;
    private int bedSpawnX, bedSpawnY, bedSpawnZ;
    private boolean sleeping;
    private BlockPos sleepingBedFootPos;
    private BlockPos sleepingBedHeadPos;
    private int sleepingBedFacing;
    private float sleepingRenderYaw;
    private float sleepReturnX, sleepReturnY, sleepReturnZ;
    private float sleepReturnYaw, sleepReturnPitch;

    public Player(float x, float y, float z) {
        float safeX = finiteOrZero(x);
        float safeY = finiteOrZero(y);
        float safeZ = finiteOrZero(z);
        this.position = new Vector3f(safeX, safeY, safeZ);
        this.prevPosition = new Vector3f(safeX, safeY, safeZ);
        this.velocity = new Vector3f();
        this.camera = new Camera(new Vector3f(safeX, safeY + EYE_HEIGHT, safeZ));
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
        this.inventory.setItemAddedListener(stack -> {
            stats.getAchievements().recordCollectedItem(stack.getType());
            stats.getStatistics().recordItemPickup(stack.getType(), stack.getCount());
        });
        this.inventory.setCraftedItemListener(stack -> {
            stats.getAchievements().recordCrafted(stack.getType());
            stats.getStatistics().recordItemCrafted(stack.getType(), stack.getCount());
        });
        this.fallStartY = safeY;
        this.wasFalling = false;
        this.dropItemFromHand = false;
        // Store spawn point for respawning
        this.spawnX = safeX;
        this.spawnY = safeY;
        this.spawnZ = safeZ;
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
        if (stats.isDead() || sleeping) {
            clearMovementInputState();
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
            if (smoothCamera) {
                float smoothing = Math.min(1.0f, Math.max(0.05f, deltaTime * 5.0f));
                smoothMouseDeltaX += (deltaX - smoothMouseDeltaX) * smoothing;
                smoothMouseDeltaY += (deltaY - smoothMouseDeltaY) * smoothing;
                deltaX = smoothMouseDeltaX;
                deltaY = smoothMouseDeltaY;
            } else {
                smoothMouseDeltaX = deltaX;
                smoothMouseDeltaY = deltaY;
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
        inputForwardDown = isActionDown(GameSettings.KeyBinding.FORWARD);
        inputBackwardDown = isActionDown(GameSettings.KeyBinding.BACK);
        inputLeftDown = isActionDown(GameSettings.KeyBinding.LEFT);
        inputRightDown = isActionDown(GameSettings.KeyBinding.RIGHT);
        inputJumpDown = isActionDown(GameSettings.KeyBinding.JUMP);

        if (inputForwardDown)
            forward += 1;
        if (inputBackwardDown)
            forward -= 1;
        if (inputLeftDown)
            strafe -= 1;
        if (inputRightDown)
            strafe += 1;

        movementInputActive = forward != 0 || strafe != 0;

        // Normalize input vector to prevent faster diagonal movement
        if (forward != 0 || strafe != 0) {
            float length = (float) Math.sqrt(forward * forward + strafe * strafe);
            forward /= length;
            strafe /= length;
        }

        // Sneaking (Shift key) - not while flying
        sneaking = isActionDown(GameSettings.KeyBinding.SNEAK) && !flying;

        clearRemovedVehicleRefs();
        if (isRidingVehicle()) {
            if (sneaking) {
                dismountVehicle();
            } else {
                mountedForwardInput = forward;
                mountedStrafeInput = strafe;
                mountedYawInput = camera.getYaw();
                applyMountedInput(forward, strafe);
                sprinting = false;
                velocity.set(0.0f, 0.0f, 0.0f);
                if (breakCooldown > 0)
                    breakCooldown = Math.max(0.0f, breakCooldown - deltaTime);
                if (placeCooldown > 0)
                    placeCooldown = Math.max(0.0f, placeCooldown - deltaTime);
                return;
            }
        } else {
            mountedForwardInput = 0.0f;
            mountedStrafeInput = 0.0f;
            mountedYawInput = camera.getYaw();
        }

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

        float acceleration = horizontalAccelerationPerSecond(speed);
        float moveX = (forward * sinYaw + strafeSign * cosYaw) * acceleration;
        float moveZ = (-forward * cosYaw + strafeSign * sinYaw) * acceleration;

        velocity.x += moveX * deltaTime;
        velocity.z += moveZ * deltaTime;

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
            if (inputJumpDown && onGround && !inWater) {
                velocity.y = jumpVelocity();
                onGround = false;
                if (!isCreative()) {
                    stats.onJump();
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
        if (sleeping) {
            targetBlock = null;
            resetBreakingProgress();
            return;
        }

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
                world.getEntitiesIncludingPending(), rayOrigin, rayDirection, ENTITY_REACH, null);
        Raycast.EntityRaycastResult anyEntityHit = Raycast.castAnyEntity(
                world.getEntitiesIncludingPending(), rayOrigin, rayDirection, ENTITY_REACH, null);
        PlayerHit playerHit = findPlayerActionTarget(rayOrigin, rayDirection, ENTITY_REACH);
        boolean playerHitBeforeBlock = isPlayerHitBeforeBlock(playerHit, targetBlock);
        boolean playerHitBeforeEntity = playerHitBeforeBlock && isPlayerHitBeforeEntity(playerHit, anyEntityHit);

        // Handle left click - attack entities OR mine blocks
        boolean handledAttackBlockUse = false;
        if (isActionPressed(GameSettings.KeyBinding.ATTACK)) {
            // Initial click always triggers a swing
            swingArm();

            // Priority 1: Attack entity if in range
            if (playerHitBeforeEntity && attackPlayer(playerHit)) {
                handledAttackBlockUse = true;
            } else if (entityHit.hit && entityHit.entity instanceof LivingEntity living) {
                recordEntityAction(living, EntityActionType.ATTACK);
                attackEntity(living);
            } else if (isEntityHitBeforeBlock(anyEntityHit, targetBlock)
                    && anyEntityHit.entity instanceof MinecartEntity minecart) {
                recordEntityAction(minecart, EntityActionType.ATTACK);
                attackMinecart(minecart);
            } else if (isEntityHitBeforeBlock(anyEntityHit, targetBlock)
                    && anyEntityHit.entity instanceof BoatEntity boat) {
                recordEntityAction(boat, EntityActionType.ATTACK);
                attackBoat(boat);
            } else if (isEntityHitBeforeBlock(anyEntityHit, targetBlock)
                    && anyEntityHit.entity instanceof PaintingEntity painting) {
                recordEntityAction(painting, EntityActionType.ATTACK);
                attackPainting(painting);
            } else if (isEntityHitBeforeBlock(anyEntityHit, targetBlock)
                    && anyEntityHit.entity instanceof FireballEntity fireball) {
                recordEntityAction(fireball, EntityActionType.ATTACK);
                attackFireball(fireball, rayDirection);
            } else if (targetBlock.hit
                    && world.getBlockIfLoaded(targetBlock.blockPos.x, targetBlock.blockPos.y,
                            targetBlock.blockPos.z, BlockType.AIR) == BlockType.NOTE_BLOCK) {
                if (world.playNoteBlock(targetBlock.blockPos.x, targetBlock.blockPos.y, targetBlock.blockPos.z)) {
                    recordItemAction(ItemActionType.PLAY_NOTE_BLOCK, null, rayDirection, 0.0f,
                            targetBlock.blockPos.x, targetBlock.blockPos.y, targetBlock.blockPos.z);
                }
            }
        }

        // Continuous left-click for mining (only if NOT attacking an entity)
        boolean miningInput = isCreative()
                ? isActionPressed(GameSettings.KeyBinding.ATTACK)
                : isActionDown(GameSettings.KeyBinding.ATTACK);
        boolean entityBlocksMining = entityHit.hit
                || playerHitBeforeBlock
                || (isEntityHitBeforeBlock(anyEntityHit, targetBlock)
                        && (anyEntityHit.entity instanceof MinecartEntity
                                || anyEntityHit.entity instanceof BoatEntity
                                || anyEntityHit.entity instanceof PaintingEntity
                                || anyEntityHit.entity instanceof FireballEntity));
        if (miningInput && !handledAttackBlockUse) {
            // Only mine blocks if we didn't hit an entity
            if (!entityBlocksMining && targetBlock.hit && breakCooldown <= 0) {
                // We are actively mining - keep the arm swinging
                if (!isSwinging) {
                    swingArm();
                }

                Vector3i currentTarget = targetBlock.blockPos;
                BlockType targetType = world.getBlockIfLoaded(currentTarget.x, currentTarget.y, currentTarget.z,
                        BlockType.AIR);
                int targetMetadata = world.getBlockMetadataIfLoaded(currentTarget.x, currentTarget.y, currentTarget.z,
                        0);
                boolean handledDragonEggTeleport = false;

                if (targetType == BlockType.REDSTONE_ORE || targetType == BlockType.GLOWING_REDSTONE_ORE) {
                    world.activateRedstoneOre(currentTarget.x, currentTarget.y, currentTarget.z);
                    world.rebuildBlockMeshesNow(currentTarget.x, currentTarget.y, currentTarget.z);
                }

                if (targetType == BlockType.DRAGON_EGG && isActionPressed(GameSettings.KeyBinding.ATTACK)) {
                    BlockPos newPos = world.teleportDragonEgg(currentTarget.x, currentTarget.y, currentTarget.z);
                    if (newPos != null) {
                        rebuildMovedBlockMeshes(world, currentTarget, newPos);
                        resetBreakingProgress();
                        breakCooldown = 0.1f;
                        handledDragonEggTeleport = true;
                    }
                }

                if (!handledDragonEggTeleport) {
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
                        float hardness = targetType.getBreakHardness();

                        // Get held tool and calculate speed multiplier
                        com.craftzero.inventory.ItemStack heldItem = inventory.getItemInHand();
                        ItemType heldType = heldItem == null || heldItem.isEmpty() ? null : heldItem.getType();
                        com.craftzero.inventory.ToolType toolType = heldType == null
                                ? com.craftzero.inventory.ToolType.NONE
                                : heldType.getToolType();
                        float speedMultiplier = computeBlockBreakSpeedMultiplier(targetType, heldItem)
                                * stats.getMiningSpeedMultiplier();

                        // Check harvest category and level - ore drops require the right tool family.
                        boolean canHarvest = isCreative()
                                || BlockHarvestRules.canHarvest(targetType, heldType, toolType);

                        float progressIncrement = isCreative()
                                ? 1.0f
                                : computeSurvivalBlockBreakProgressIncrement(deltaTime, hardness, speedMultiplier,
                                        canHarvest, headInWater, onGround, flying, hasAquaAffinity());

                        float nextBreakProgress = breakProgress + progressIncrement;
                        if (isActionPressed(GameSettings.KeyBinding.ATTACK) && nextBreakProgress < 1.0f) {
                            world.spawnBlockHitParticle(currentTarget.x, currentTarget.y, currentTarget.z,
                                    targetBlock.face, targetType, targetMetadata);
                        }
                        breakProgress = nextBreakProgress;

                        // Block is broken when progress reaches 1.0
                        if (breakProgress >= 1.0f) {
                            if (world.breakBlockWithToolStack(currentTarget.x, currentTarget.y, currentTarget.z,
                                    canHarvest, heldItem)) {
                                world.spawnBlockDestroyParticles(currentTarget.x, currentTarget.y, currentTarget.z,
                                        targetType, targetMetadata);
                                world.playBlockBreakSound(targetType, currentTarget.x, currentTarget.y, currentTarget.z);
                                if (!isCreative()) {
                                    stats.onBlockBreak(targetType);
                                }
                                stats.getAchievements().recordBlockBroken(targetType);
                                world.rebuildBlockMeshesNow(currentTarget.x, currentTarget.y, currentTarget.z);
                            }

                            // Consume tool durability
                            if (shouldDamageHeldItemOnBlockBreak(heldItem, targetType, heldType)) {
                                damageHeldDurable(heldItem);
                            }

                            // Reset breaking state
                            breakingBlockPos = null;
                            breakProgress = 0f;
                            currentBreakingBlock = null;

                            // Small cooldown to prevent immediately starting to break next block
                            breakCooldown = 0.1f;
                        }
                    }
                }
            } else if (!entityBlocksMining) {
                // Not looking at a block OR entity - reset progress
                resetBreakingProgress();
            }
        } else if (!handledAttackBlockUse) {
            // Button released - reset progress
            resetBreakingProgress();
        }

        // Update break cooldown
        // Flag for opening crafting table
        wantsCraftingTable = false;
        requestedCraftingTablePos = null;
        requestedChestPos = null;
        requestedChestMinecart = null;
        requestedFurnacePos = null;
        requestedDispenserPos = null;
        requestedBrewingStandPos = null;
        requestedEnchantingTablePos = null;
        requestedSignEditPos = null;
        requestedBedUsePos = null;

        boolean bowHandled = handleBowUse(world, deltaTime, rayDirection);
        boolean swordBlockingHandled = !bowHandled && handleSwordBlocking();
        boolean consumableHandled = !bowHandled && !swordBlockingHandled && continueHeldConsumableUse();

        // Use item / place block (right click)
        if (!bowHandled && !swordBlockingHandled && !consumableHandled
                && isActionPressed(GameSettings.KeyBinding.USE) && placeCooldown <= 0) {
            com.craftzero.inventory.ItemStack stack = inventory.getItemInHand();
            if (handleEntityUse(stack, anyEntityHit, targetBlock)) {
                recordEntityAction(anyEntityHit.entity, EntityActionType.USE);
                placeCooldown = PLACE_COOLDOWN;
            } else if (handleFluidSourceItemUse(world, stack, rayOrigin, rayDirection)) {
                placeCooldown = PLACE_COOLDOWN;
            } else if (targetBlock.hit) {
                // Check if clicking on a crafting table - open it instead of placing
                BlockType clickedBlock = world.getBlockIfLoaded(
                        targetBlock.blockPos.x,
                        targetBlock.blockPos.y,
                        targetBlock.blockPos.z,
                        BlockType.AIR);
                boolean useClickedBlock = shouldUseClickedBlockBeforePlacement(stack);

                if (useClickedBlock && clickedBlock == BlockType.CRAFTING_TABLE) {
                    wantsCraftingTable = true;
                    requestedCraftingTablePos = new Vector3i(targetBlock.blockPos);
                    placeCooldown = PLACE_COOLDOWN;
                } else if (useClickedBlock
                        && (clickedBlock == BlockType.REDSTONE_ORE || clickedBlock == BlockType.GLOWING_REDSTONE_ORE)) {
                    world.activateRedstoneOre(targetBlock.blockPos.x, targetBlock.blockPos.y, targetBlock.blockPos.z);
                    world.rebuildBlockMeshesNow(targetBlock.blockPos.x, targetBlock.blockPos.y, targetBlock.blockPos.z);
                    placeCooldown = PLACE_COOLDOWN;
                } else if (useClickedBlock && clickedBlock == BlockType.CHEST) {
                    requestedChestPos = new Vector3i(targetBlock.blockPos);
                    placeCooldown = PLACE_COOLDOWN;
                } else if (useClickedBlock && clickedBlock.isFurnace()) {
                    requestedFurnacePos = new Vector3i(targetBlock.blockPos);
                    placeCooldown = PLACE_COOLDOWN;
                } else if (useClickedBlock && clickedBlock == BlockType.DISPENSER) {
                    requestedDispenserPos = new Vector3i(targetBlock.blockPos);
                    placeCooldown = PLACE_COOLDOWN;
                } else if (useClickedBlock && clickedBlock == BlockType.BREWING_STAND) {
                    requestedBrewingStandPos = new Vector3i(targetBlock.blockPos);
                    placeCooldown = PLACE_COOLDOWN;
                } else if (useClickedBlock && clickedBlock == BlockType.ENCHANTING_TABLE) {
                    requestedEnchantingTablePos = new Vector3i(targetBlock.blockPos);
                    placeCooldown = PLACE_COOLDOWN;
                } else if (useClickedBlock && clickedBlock == BlockType.CAKE && eatCake(world)) {
                    placeCooldown = PLACE_COOLDOWN;
                } else if (useClickedBlock && clickedBlock.isBed()) {
                    requestedBedUsePos = new Vector3i(targetBlock.blockPos);
                    placeCooldown = PLACE_COOLDOWN;
                } else if (useClickedBlock && clickedBlock == BlockType.DRAGON_EGG) {
                    BlockPos newPos = world.teleportDragonEgg(targetBlock.blockPos.x, targetBlock.blockPos.y,
                            targetBlock.blockPos.z);
                    if (newPos != null) {
                        rebuildMovedBlockMeshes(world, targetBlock.blockPos, newPos);
                    }
                    placeCooldown = PLACE_COOLDOWN;
                } else if (useClickedBlock
                        && world.toggleBlock(targetBlock.blockPos.x, targetBlock.blockPos.y, targetBlock.blockPos.z,
                        getHorizontalFacingIndex())) {
                    startUseAnimation();
                    recordToggledBlockItemAction(clickedBlock, stack, rayDirection, targetBlock.blockPos);
                    rebuildToggledBlockMeshes(world, targetBlock.blockPos, clickedBlock);
                    placeCooldown = PLACE_COOLDOWN;
                } else {
                    if (handleImmediateItemUse(world, stack, rayOrigin, rayDirection)) {
                        placeCooldown = PLACE_COOLDOWN;
                    } else if (beginHeldConsumableUse(world, stack)) {
                        placeCooldown = PLACE_COOLDOWN;
                    } else if (handleTargetedItemUse(world, stack, clickedBlock)) {
                        placeCooldown = PLACE_COOLDOWN;
                    } else if (handleBucketUse(world, stack)) {
                        placeCooldown = PLACE_COOLDOWN;
                    } else if (targetBlock.previousBlockPos != null && tryPlaceHeldItem(world, clickedBlock, stack)) {
                        placeCooldown = PLACE_COOLDOWN;
                    }
                }
            } else if (handleImmediateItemUse(world, stack, rayOrigin, rayDirection)) {
                placeCooldown = PLACE_COOLDOWN;
            } else if (beginHeldConsumableUse(world, stack)) {
                placeCooldown = PLACE_COOLDOWN;
            }
        }

    }

    private void rebuildMovedBlockMeshes(World world, Vector3i from, BlockPos to) {
        world.rebuildBlockMeshesNow(from.x, from.y, from.z);
        world.rebuildBlockMeshesNow(to.x(), to.y(), to.z());
    }

    private void rebuildToggledBlockMeshes(World world, Vector3i pos, BlockType clickedBlock) {
        if (clickedBlock == BlockType.JUKEBOX) {
            return;
        }
        if (clickedBlock.isDoor()) {
            int metadata = world.getBlockMetadataIfLoaded(pos.x, pos.y, pos.z, 0);
            int lowerY = BlockShape.isDoorUpper(metadata) ? pos.y - 1 : pos.y;
            if (world.getBlockIfLoaded(pos.x, lowerY, pos.z, BlockType.AIR) == clickedBlock
                    && !BlockShape.isDoorUpper(world.getBlockMetadataIfLoaded(pos.x, lowerY, pos.z, 0))) {
                world.rebuildBlockMeshesNow(pos.x, lowerY, pos.z);
                int upperY = lowerY + 1;
                if (world.getBlockIfLoaded(pos.x, upperY, pos.z, BlockType.AIR) == clickedBlock
                        && BlockShape.isDoorUpper(world.getBlockMetadataIfLoaded(pos.x, upperY, pos.z, 0))) {
                    world.rebuildBlockMeshesNow(pos.x, upperY, pos.z);
                }
                return;
            }
        }
        world.rebuildBlockMeshesNow(pos.x, pos.y, pos.z);
    }

    private void recordToggledBlockItemAction(BlockType clickedBlock, ItemStack stack, Vector3f direction,
            Vector3i pos) {
        if (clickedBlock == null || pos == null) {
            return;
        }
        if (clickedBlock == BlockType.NOTE_BLOCK) {
            recordItemAction(ItemActionType.TUNE_NOTE_BLOCK, null, direction, 0.0f, pos.x, pos.y, pos.z);
        } else if (clickedBlock == BlockType.JUKEBOX) {
            ItemType type = stack == null || stack.isEmpty() ? null : stack.getType();
            recordItemAction(ItemActionType.EJECT_RECORD, type, direction, 0.0f, pos.x, pos.y, pos.z);
        }
    }

    private boolean isEntityHitBeforeBlock(Raycast.EntityRaycastResult entityHit, Raycast.RaycastResult blockHit) {
        return entityHit != null && entityHit.hit && entityHit.entity != null
                && (blockHit == null || !blockHit.hit || entityHit.distance <= blockHit.distance);
    }

    private PlayerHit findPlayerActionTarget(Vector3f origin, Vector3f direction, float maxDistance) {
        if (playerActionHandler == null || origin == null || direction == null || maxDistance <= 0.0f) {
            return null;
        }
        PlayerHit hit = playerActionHandler.findAttackTarget(origin, direction, maxDistance);
        return hit != null && hit.hit() ? hit : null;
    }

    private boolean isPlayerHitBeforeBlock(PlayerHit playerHit, Raycast.RaycastResult blockHit) {
        return playerHit != null && playerHit.hit()
                && (blockHit == null || !blockHit.hit || playerHit.distance() <= blockHit.distance);
    }

    private boolean isPlayerHitBeforeEntity(PlayerHit playerHit, Raycast.EntityRaycastResult entityHit) {
        return playerHit != null && playerHit.hit()
                && (entityHit == null || !entityHit.hit || entityHit.entity == null
                        || playerHit.distance() <= entityHit.distance);
    }

    private boolean attackPlayer(PlayerHit hit) {
        if (playerActionHandler == null || hit == null || !hit.hit()) {
            return false;
        }
        PlayerAttack attack = buildPlayerAttack();
        if (attack.damage() <= 0.0f || !playerActionHandler.attackPlayer(hit, attack)) {
            return false;
        }
        finishSuccessfulPlayerAttack(attack);
        return true;
    }

    private PlayerAttack buildPlayerAttack() {
        ItemStack heldItem = inventory.getItemInHand();
        float damage = 1.0f;
        if (heldItem != null && heldItem.isTool()) {
            damage = heldItem.getType().getToolType().getAttackDamage();
        }
        damage += EnchantmentResolver.attackDamageBonus(heldItem);
        damage += stats.getAttackDamageBonus();

        boolean critical = velocity.y < 0 && !onGround && !inWater && !flying;
        if (critical) {
            damage *= 1.5f;
        }

        float knockbackStrength = CombatRules.PLAYER_ATTACK_KNOCKBACK;
        knockbackStrength += EnchantmentResolver.getLevel(heldItem, EnchantmentType.KNOCKBACK) * 0.4f;
        boolean applySprintBonus = sprinting && !sprintKnockbackUsed;
        if (applySprintBonus) {
            knockbackStrength += CombatRules.PLAYER_ATTACK_SPRINT_BONUS;
        }
        int fireTicks = EnchantmentResolver.getLevel(heldItem, EnchantmentType.FIRE_ASPECT) * 80;
        return new PlayerAttack(Math.max(0.0f, damage),
                position.x, position.y + EYE_HEIGHT, position.z,
                knockbackStrength, CombatRules.PLAYER_ATTACK_VERTICAL_KNOCKBACK,
                fireTicks, applySprintBonus);
    }

    private void finishSuccessfulPlayerAttack(PlayerAttack attack) {
        stats.getStatistics().recordSuccessfulAttack(attack.damage());
        stats.getAchievements().recordOverkillHit(attack.damage());
        if (!isCreative()) {
            stats.onAttack();
        }
        if (attack.sprintKnockback()) {
            sprintKnockbackUsed = true;
            sprinting = false;
            velocity.x *= 0.6f;
            velocity.z *= 0.6f;
        }
        if (!isCreative()) {
            ItemStack heldItem = inventory.getItemInHand();
            if (heldItem != null && heldItem.isTool()) {
                boolean toolBroke = useDurabilityWithEnchantments(heldItem);
                if (toolBroke) {
                    inventory.getHotbar()[inventory.getSelectedSlot()] = null;
                }
            }
        }
    }

    public void clearMovementInputState() {
        movementInputActive = false;
        inputForwardDown = false;
        inputBackwardDown = false;
        inputLeftDown = false;
        inputRightDown = false;
        inputJumpDown = false;
    }

    public void applyRemoteInputState(boolean forward, boolean backward, boolean left, boolean right,
            boolean jumping, boolean sneaking, boolean sprinting) {
        inputForwardDown = forward;
        inputBackwardDown = backward;
        inputLeftDown = left;
        inputRightDown = right;
        inputJumpDown = jumping;
        movementInputActive = forward || backward || left || right;
        this.sneaking = sneaking;
        this.sprinting = sprinting && forward && !backward && !sneaking;
    }

    public List<EntityActionRecord> drainEntityActionRecords() {
        if (entityActionRecords.isEmpty()) {
            return List.of();
        }
        List<EntityActionRecord> drained = new ArrayList<>(entityActionRecords);
        entityActionRecords.clear();
        return drained;
    }

    public List<ItemActionRecord> drainItemActionRecords() {
        if (itemActionRecords.isEmpty()) {
            return List.of();
        }
        List<ItemActionRecord> drained = new ArrayList<>(itemActionRecords);
        itemActionRecords.clear();
        return drained;
    }

    private void recordEntityAction(Entity entity, EntityActionType actionType) {
        if (entity != null && actionType != null) {
            entityActionRecords.add(new EntityActionRecord(entity, actionType));
        }
    }

    private void recordItemAction(ItemActionType actionType, ItemType itemType, Vector3f direction, float power) {
        recordItemAction(actionType, itemType, direction, power, Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE);
    }

    private void recordItemAction(ItemActionType actionType, ItemType itemType, Vector3f direction, float power,
            int blockX, int blockY, int blockZ) {
        recordItemAction(actionType, itemType, direction, power, blockX, blockY, blockZ, Integer.MIN_VALUE);
    }

    private void recordItemAction(ItemActionType actionType, ItemType itemType, Vector3f direction, float power,
            int blockX, int blockY, int blockZ, int blockFace) {
        if (actionType == null) {
            return;
        }
        Vector3f normalized = direction == null ? new Vector3f() : new Vector3f(direction);
        if (normalized.lengthSquared() > 0.0001f) {
            normalized.normalize();
        }
        itemActionRecords.add(new ItemActionRecord(actionType, itemType,
                normalized.x, normalized.y, normalized.z, Math.max(0.0f, power),
                blockX, blockY, blockZ, blockFace));
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

    private boolean handleSwordBlocking() {
        com.craftzero.inventory.ItemStack held = inventory.getItemInHand();
        boolean holdingSword = isSwordStack(held);
        if (!holdingSword || !isActionDown(GameSettings.KeyBinding.USE)) {
            if (isBlockingItem) {
                isUsingItem = false;
                useProgress = 0.0f;
                prevUseProgress = 0.0f;
            }
            isBlockingItem = false;
            return false;
        }

        isBlockingItem = true;
        isUsingItem = true;
        return true;
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
        if (!consumeArrow()) {
            return;
        }

        Vector3f spawn = new Vector3f(position.x, position.y + EYE_HEIGHT - 0.1f, position.z)
                .add(new Vector3f(direction).mul(0.6f));
        float speed = 3.0f * power;
        float damage = 2.0f + 4.0f * power;
        ArrowEntity arrow = world.spawnArrow(spawn.x, spawn.y, spawn.z,
                direction.x * speed,
                direction.y * speed,
                direction.z * speed,
                null,
                true,
                damage);
        arrow.setCritical(power >= 1.0f);
        recordItemAction(ItemActionType.BOW, ItemType.BOW, direction, power);
        world.playBowSound(spawn.x, spawn.y, spawn.z);

        recordItemUse(bow);
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

    private float jumpVelocity() {
        int jumpBoost = stats.getEffectAmplifier(StatusEffectType.JUMP_BOOST);
        return JUMP_VELOCITY + (jumpBoost >= 0 ? JUMP_BOOST_VELOCITY_BONUS * (jumpBoost + 1) : 0.0f);
    }

    static float computeBlockBreakSpeedMultiplier(BlockType targetType, ItemStack heldItem) {
        if (targetType == null || heldItem == null || heldItem.isEmpty()) {
            return 1.0f;
        }

        ItemType heldType = heldItem.getType();
        if (targetType == BlockType.COBWEB && isCobwebHarvestTool(heldType)) {
            return COBWEB_CUTTING_SPEED;
        }
        if (heldType == ItemType.SHEARS) {
            if (targetType == BlockType.LEAVES) {
                return SHEARS_LEAVES_SPEED;
            }
            if (targetType == BlockType.WHITE_WOOL) {
                return SHEARS_WOOL_SPEED;
            }
        }
        if (heldItem.isTool()) {
            ToolType toolType = heldType.getToolType();
            if (toolType.isEffectiveAgainst(targetType.getPreferredTool())) {
                return toolType.getSpeedMultiplier() + EnchantmentResolver.miningSpeedBonus(heldItem);
            }
        }
        return 1.0f;
    }

    static float computeSurvivalBlockBreakProgressIncrement(float deltaTime, float hardness, float speedMultiplier,
            boolean canHarvest, boolean headInWater, boolean onGround, boolean flying) {
        return computeSurvivalBlockBreakProgressIncrement(deltaTime, hardness, speedMultiplier,
                canHarvest, headInWater, onGround, flying, false);
    }

    static float computeSurvivalBlockBreakProgressIncrement(float deltaTime, float hardness, float speedMultiplier,
            boolean canHarvest, boolean headInWater, boolean onGround, boolean flying, boolean hasAquaAffinity) {
        if (hardness <= 0.0f) {
            return 1.0f;
        }

        float strength = speedMultiplier / hardness
                / (canHarvest ? HARVESTABLE_BLOCK_STRENGTH_DIVISOR : NON_HARVESTABLE_BLOCK_STRENGTH_DIVISOR);
        if (headInWater && !flying && !hasAquaAffinity) {
            strength /= RELEASE_ONE_MINING_PENALTY;
        }
        if (!onGround && !flying) {
            strength /= RELEASE_ONE_MINING_PENALTY;
        }
        return strength * deltaTime * BLOCK_BREAK_TICKS_PER_SECOND;
    }

    private boolean hasAquaAffinity() {
        ItemStack[] armor = inventory == null ? null : inventory.getArmor();
        if (armor == null || ArmorSlot.HELMET.getIndex() >= armor.length) {
            return false;
        }
        return EnchantmentResolver.has(armor[ArmorSlot.HELMET.getIndex()], EnchantmentType.AQUA_AFFINITY);
    }

    private int respirationLevel() {
        ItemStack[] armor = inventory == null ? null : inventory.getArmor();
        if (armor == null || ArmorSlot.HELMET.getIndex() >= armor.length) {
            return 0;
        }
        return EnchantmentResolver.getLevel(armor[ArmorSlot.HELMET.getIndex()], EnchantmentType.RESPIRATION);
    }

    private boolean tryPlaceHeldItem(World world, BlockType clickedBlock, com.craftzero.inventory.ItemStack stack) {
        if (stack == null || stack.isEmpty() || !stack.getType().isPlaceable()) {
            return false;
        }

        ItemType itemType = stack.getType();
        BlockType placedBlock = itemType.getPlacedBlock();
        boolean replaceClickedBlock = BlockShape.isReplaceable(clickedBlock);

        if (!replaceClickedBlock && BlockShape.blocksPlacementAgainst(clickedBlock, targetBlock.face)
                && placedBlock.isSolid()) {
            return false;
        }

        if (placedBlock == BlockType.STONE_SLAB && clickedBlock == BlockType.STONE_SLAB
                && world.getBlockMetadataIfLoaded(targetBlock.blockPos.x, targetBlock.blockPos.y,
                        targetBlock.blockPos.z, -1) == itemType.getPlacedBlockMetadata()
                && world.tryMergeSlab(targetBlock.blockPos.x, targetBlock.blockPos.y, targetBlock.blockPos.z)) {
            consumePlacedStack(stack);
            world.playBlockPlaceSound(placedBlock, targetBlock.blockPos.x, targetBlock.blockPos.y, targetBlock.blockPos.z);
            world.rebuildBlockMeshesNow(targetBlock.blockPos.x, targetBlock.blockPos.y, targetBlock.blockPos.z);
            return true;
        }

        Vector3i placePos = replaceClickedBlock ? targetBlock.blockPos : targetBlock.previousBlockPos;
        if (placePos == null) {
            return false;
        }

        if (placedBlock == BlockType.CHEST && !world.canPlaceChestAt(placePos.x, placePos.y, placePos.z)) {
            return false;
        }
        if (placedBlock == BlockType.VINES && BlockShape.vineMetadataFromFace(targetBlock.face) < 0) {
            return false;
        }

        boolean placed = false;
        if (itemType == ItemType.WOODEN_DOOR || itemType == ItemType.IRON_DOOR) {
            placed = world.placeDoor(placePos.x, placePos.y, placePos.z, placedBlock, getHorizontalFacingIndex(), boundingBox);
        } else if (itemType == ItemType.BED) {
            BlockPos foot = world.placeBed(placePos.x, placePos.y, placePos.z, getHorizontalFacingIndex(), boundingBox);
            placed = foot != null;
        } else if (itemType == ItemType.TRAPDOOR) {
            placed = world.placeTrapdoor(placePos.x, placePos.y, placePos.z, targetBlock.face, boundingBox);
        } else if (itemType == ItemType.STONE_BUTTON) {
            placed = world.placeStoneButton(placePos.x, placePos.y, placePos.z, targetBlock.face, boundingBox);
        } else if (itemType == ItemType.LEVER) {
            placed = world.placeLever(placePos.x, placePos.y, placePos.z, targetBlock.face, boundingBox);
        } else if (itemType == ItemType.SIGN) {
            placed = placeSign(world, placePos);
        } else {
            int metadata = getPlacementMetadata(itemType, placedBlock);
            if (world.canPlaceBlockAt(placePos.x, placePos.y, placePos.z, placedBlock, metadata, boundingBox)) {
                world.setBlock(placePos.x, placePos.y, placePos.z, placedBlock, metadata);
                placed = true;
            }
        }

        if (placed) {
            consumePlacedStack(stack);
            world.playBlockPlaceSound(placedBlock, placePos.x, placePos.y, placePos.z);
            world.rebuildBlockMeshesNow(placePos.x, placePos.y, placePos.z);
        }
        return placed;
    }

    private boolean shouldUseClickedBlockBeforePlacement(ItemStack stack) {
        return !sneaking
                || stack == null
                || stack.isEmpty()
                || !stack.getType().isPlaceable()
                || targetBlock == null
                || targetBlock.previousBlockPos == null;
    }

    private boolean handleImmediateItemUse(World world, com.craftzero.inventory.ItemStack stack,
            Vector3f rayOrigin, Vector3f rayDirection) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        if (handleFluidSourceItemUse(world, stack, rayOrigin, rayDirection)) {
            return true;
        }
        if (stack.getType() == ItemType.BOAT && placeBoatFromView(world, stack, rayOrigin, rayDirection)) {
            return true;
        }
        if ((stack.getType() == ItemType.EGG || stack.getType() == ItemType.SNOWBALL)
                && throwThrownItemProjectile(world, stack, rayDirection)) {
            return true;
        }
        if (stack.getType() == ItemType.ENDER_PEARL && throwEnderPearl(world, stack, rayDirection)) {
            return true;
        }
        if (stack.getType() == ItemType.FISHING_ROD && useFishingRod(world, stack, rayDirection)) {
            return true;
        }
        if (MapItemData.useMap(world, stack, position.x, position.z, camera.getYaw())) {
            recordItemAction(ItemActionType.USE_MAP, ItemType.MAP, rayDirection, 0.0f);
            recordItemUse(stack);
            startUseAnimation();
            return true;
        }
        if (equipArmorFromHand(stack)) {
            recordItemAction(ItemActionType.EQUIP_ARMOR, stack.getType(), rayDirection, 0.0f);
            recordItemUse(stack);
            startUseAnimation();
            return true;
        }
        if (stack.getType() == ItemType.POTION) {
            return useSplashPotion(world, stack);
        }
        if (stack.getType() == ItemType.EYE_OF_ENDER
                && (targetBlock == null || !targetBlock.hit
                        || world.getBlockIfLoaded(targetBlock.blockPos.x, targetBlock.blockPos.y,
                                targetBlock.blockPos.z, BlockType.AIR) != BlockType.END_PORTAL_FRAME)) {
            return throwEyeOfEnder(world, stack);
        }
        return false;
    }

    private boolean handleFluidSourceItemUse(World world, com.craftzero.inventory.ItemStack stack,
            Vector3f rayOrigin, Vector3f rayDirection) {
        if (world == null || stack == null || stack.isEmpty() || rayOrigin == null || rayDirection == null) {
            return false;
        }
        ItemType type = stack.getType();
        if (type != ItemType.BUCKET && type != ItemType.GLASS_BOTTLE && type != ItemType.LILY_PAD) {
            return false;
        }
        Raycast.RaycastResult fluidTarget = Raycast.castFluidSource(world, rayOrigin, rayDirection, REACH_DISTANCE);
        if (fluidTarget == null || !fluidTarget.hit) {
            return false;
        }
        BlockType fluid = world.getBlockIfLoaded(fluidTarget.blockPos.x, fluidTarget.blockPos.y,
                fluidTarget.blockPos.z, BlockType.AIR);
        if (type == ItemType.BUCKET) {
            ItemType filledBucket = world.pickupFluidSource(fluidTarget.blockPos.x, fluidTarget.blockPos.y,
                    fluidTarget.blockPos.z);
            if (filledBucket == null) {
                return false;
            }
            recordItemUse(type);
            if (isCreative()) {
                startUseAnimation();
                world.rebuildBlockMeshesNow(fluidTarget.blockPos.x, fluidTarget.blockPos.y, fluidTarget.blockPos.z);
                return true;
            }
            replaceHeldItemAfterBucketUse(world, stack, filledBucket);
            world.rebuildBlockMeshesNow(fluidTarget.blockPos.x, fluidTarget.blockPos.y, fluidTarget.blockPos.z);
            return true;
        }
        if (type == ItemType.LILY_PAD) {
            if (!fluid.isWater()
                    || !world.placeLilyPadOnWater(fluidTarget.blockPos.x, fluidTarget.blockPos.y,
                            fluidTarget.blockPos.z, boundingBox)) {
                return false;
            }
            consumePlacedStack(stack);
            world.playBlockPlaceSound(BlockType.LILY_PAD,
                    fluidTarget.blockPos.x, fluidTarget.blockPos.y + 1, fluidTarget.blockPos.z);
            world.rebuildBlockMeshesNow(fluidTarget.blockPos.x, fluidTarget.blockPos.y + 1, fluidTarget.blockPos.z);
            return true;
        }
        if (!fluid.isWater()) {
            return false;
        }
        recordItemUse(type);
        if (!isCreative()) {
            replaceHeldItemAfterBottleFill(world, stack);
        } else {
            startUseAnimation();
        }
        return true;
    }

    private boolean continueHeldConsumableUse() {
        if (!isConsumingItem) {
            return false;
        }
        if (!isActionDown(GameSettings.KeyBinding.USE) || !heldConsumableMatches()) {
            cancelHeldConsumableUse();
            return false;
        }
        consumableUseHeldThisFrame = true;
        return true;
    }

    private boolean beginHeldConsumableUse(World world, com.craftzero.inventory.ItemStack stack) {
        if (isConsumingItem || world == null || stack == null || stack.isEmpty() || !canStartHeldConsumableUse(stack)) {
            return false;
        }
        isConsumingItem = true;
        consumingItemType = stack.getType();
        consumingSlot = inventory.getSelectedSlot();
        consumableUseTime = 0.0f;
        consumableTickSoundTimer = 0.0f;
        consumableUseHeldThisFrame = true;
        isUsingItem = true;
        useProgress = 0.0f;
        prevUseProgress = 0.0f;
        playHeldConsumableTickSound(world);
        return true;
    }

    private boolean canStartHeldConsumableUse(com.craftzero.inventory.ItemStack stack) {
        ItemType type = stack.getType();
        FoodValue food = foodValue(type);
        if (food != null) {
            return stats.getHunger() < PlayerStats.MAX_HUNGER || type == ItemType.GOLDEN_APPLE;
        }
        if (type == ItemType.MILK_BUCKET) {
            return true;
        }
        if (type == ItemType.POTION) {
            PotionData potion = potionDataOrWater(stack);
            return !potion.splash();
        }
        return false;
    }

    private boolean heldConsumableMatches() {
        if (consumingSlot < 0 || consumingSlot >= inventory.getHotbar().length
                || inventory.getSelectedSlot() != consumingSlot) {
            return false;
        }
        com.craftzero.inventory.ItemStack held = inventory.getHotbar()[consumingSlot];
        return held != null && !held.isEmpty() && held.getType() == consumingItemType;
    }

    private boolean placeBoatFromView(World world, com.craftzero.inventory.ItemStack stack,
            Vector3f rayOrigin, Vector3f rayDirection) {
        if (rayOrigin == null || rayDirection == null) {
            return false;
        }
        for (float distance = 0.0f; distance <= REACH_DISTANCE; distance += 0.1f) {
            int x = (int) Math.floor(rayOrigin.x + rayDirection.x * distance);
            int y = (int) Math.floor(rayOrigin.y + rayDirection.y * distance);
            int z = (int) Math.floor(rayOrigin.z + rayDirection.z * distance);
            BlockType type = world.getBlockIfLoaded(x, y, z, BlockType.AIR);
            if (type.isWater()) {
                if (!world.placeBoatOnWater(x, y, z, camera.getYaw())) {
                    return false;
                }
                consumePlacedStack(stack);
                recordItemAction(ItemActionType.PLACE_BOAT, ItemType.BOAT, rayDirection, 0.0f);
                return true;
            }
            if (type.isSolid()) {
                return false;
            }
        }
        return false;
    }

    private boolean eatCake(World world) {
        return targetBlock != null && targetBlock.hit && eatCakeAt(world, targetBlock.blockPos);
    }

    private boolean eatCakeAt(World world, Vector3i pos) {
        if (pos == null || world.getBlockIfLoaded(pos.x, pos.y, pos.z, BlockType.AIR) != BlockType.CAKE) {
            return false;
        }
        if (isCreative() || stats.getHunger() >= PlayerStats.MAX_HUNGER) {
            return false;
        }
        if (!world.eatCakeSlice(pos.x, pos.y, pos.z)) {
            return false;
        }
        stats.feed(CAKE_SLICE_HUNGER, CAKE_SLICE_SATURATION);
        playEatCompleteSounds(world);
        world.rebuildBlockMeshesNow(pos.x, pos.y, pos.z);
        return true;
    }

    private boolean useSplashPotion(World world, com.craftzero.inventory.ItemStack stack) {
        PotionData potion = potionDataOrWater(stack);
        if (!potion.splash()) {
            return false;
        }
        Vector3f direction = camera.getForward();
        Vector3f spawn = new Vector3f(position.x, position.y + EYE_HEIGHT - 0.1f, position.z)
                .add(new Vector3f(direction).mul(0.35f));
        com.craftzero.entity.SplashPotionEntity potionEntity = world.spawnSplashPotion(spawn.x, spawn.y, spawn.z,
                direction.x * 0.5f,
                direction.y * 0.5f + 0.1f,
                direction.z * 0.5f,
                null,
                potion);
        potionEntity.setPlayerOwned(true);
        world.playThrowSound(spawn.x, spawn.y, spawn.z);
        recordItemAction(ItemActionType.SPLASH_POTION, ItemType.POTION, direction, 0.0f);
        if (!isCreative()) {
            stack.remove(1);
            if (stack.isEmpty()) {
                inventory.getHotbar()[inventory.getSelectedSlot()] = null;
            }
        }
        recordItemUse(stack.getType());
        startUseAnimation();
        return true;
    }

    private PotionData potionDataOrWater(com.craftzero.inventory.ItemStack stack) {
        PotionData potion = stack == null ? null : stack.getPotionData();
        return potion == null ? PotionData.water() : potion;
    }

    private void completeHeldConsumableUse(World world) {
        if (!heldConsumableMatches()) {
            cancelHeldConsumableUse();
            return;
        }
        com.craftzero.inventory.ItemStack stack = inventory.getHotbar()[consumingSlot];
        ItemType type = stack.getType();
        FoodValue food = foodValue(type);
        if (food != null) {
            completeFoodUse(world, stack, food);
        } else if (type == ItemType.MILK_BUCKET) {
            completeMilkUse(world, stack);
        } else if (type == ItemType.POTION) {
            completeDrinkablePotionUse(world, stack);
        }
        clearHeldConsumableUse();
    }

    private void completeFoodUse(World world, com.craftzero.inventory.ItemStack stack, FoodValue food) {
        recordItemAction(ItemActionType.CONSUME_FOOD, stack.getType(), camera.getForward(), 0.0f);
        recordItemUse(stack);
        spawnFoodUseParticles(world, stack.getType(), CONSUMABLE_FINISH_CRUMB_PARTICLES);
        if (!isCreative()) {
            stats.feed(food.hunger(), food.saturation());
            applyFoodSideEffects(stack.getType());
            consumeFoodStack(world, stack);
        }
        playEatCompleteSounds(world);
    }

    private void applyFoodSideEffects(ItemType type) {
        if (type == ItemType.ROTTEN_FLESH && random.nextFloat() < 0.8f) {
            stats.addEffect(new StatusEffectInstance(StatusEffectType.HUNGER, 30 * 20, 0));
        } else if (type == ItemType.RAW_CHICKEN && random.nextFloat() < 0.3f) {
            stats.addEffect(new StatusEffectInstance(StatusEffectType.HUNGER, 30 * 20, 0));
        } else if (type == ItemType.SPIDER_EYE) {
            stats.addEffect(new StatusEffectInstance(StatusEffectType.POISON, 5 * 20, 0));
        } else if (type == ItemType.GOLDEN_APPLE) {
            stats.addEffect(new StatusEffectInstance(StatusEffectType.REGENERATION, 30 * 20, 0));
        }
    }

    private void completeMilkUse(World world, com.craftzero.inventory.ItemStack stack) {
        recordItemAction(ItemActionType.DRINK_MILK, ItemType.MILK_BUCKET, camera.getForward(), 0.0f);
        recordItemUse(stack);
        stats.clearEffects();
        playDrinkSound(world);
        if (!isCreative()) {
            replaceHeldItemAfterBucketUse(world, stack, ItemType.BUCKET);
        }
    }

    private void completeDrinkablePotionUse(World world, com.craftzero.inventory.ItemStack stack) {
        recordItemAction(ItemActionType.DRINK_POTION, ItemType.POTION, camera.getForward(), 0.0f);
        recordItemUse(stack);
        PotionEffectResolver.applyToPlayer(this, potionDataOrWater(stack), 1.0f);
        playDrinkSound(world);
        if (!isCreative()) {
            replaceHeldItemAfterPotionUse(world, stack);
        }
    }

    private void cancelHeldConsumableUse() {
        clearHeldConsumableUse();
    }

    private void clearHeldConsumableUse() {
        isConsumingItem = false;
        consumingItemType = null;
        consumingSlot = -1;
        consumableUseTime = 0.0f;
        consumableTickSoundTimer = 0.0f;
        consumableUseHeldThisFrame = false;
        isUsingItem = false;
        useProgress = 0.0f;
        prevUseProgress = 0.0f;
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

    private boolean handleEntityUse(com.craftzero.inventory.ItemStack stack,
            Raycast.EntityRaycastResult entityHit, Raycast.RaycastResult blockHit) {
        if (!entityHit.hit || entityHit.entity == null) {
            return false;
        }
        if (entityHit.entity.isRemoved()) {
            return false;
        }
        if (blockHit != null && blockHit.hit && blockHit.distance < entityHit.distance) {
            return false;
        }

        if (stack != null && !stack.isEmpty() && entityHit.entity instanceof Sheep sheep) {
            if (stack.getType() == ItemType.SHEARS) {
                return shearSheep(sheep, stack);
            }
            if (Sheep.woolColorForDye(stack.getType()) >= 0) {
                dyeSheep(sheep, stack);
                return true;
            }
        }

        if (entityHit.entity instanceof Mooshroom mooshroom) {
            if (stack != null && !stack.isEmpty() && stack.getType() == ItemType.BOWL) {
                return fillBowlFromMooshroom(mooshroom, stack);
            }
            if (stack != null && !stack.isEmpty() && stack.getType() == ItemType.SHEARS) {
                return shearMooshroom(mooshroom, stack);
            }
        }

        if (stack != null && !stack.isEmpty() && stack.getType() == ItemType.BUCKET
                && entityHit.entity instanceof Cow cow) {
            return milkCow(cow, stack);
        }

        if (entityHit.entity instanceof Mob mob && feedBreedingAnimal(mob, stack)) {
            return true;
        }

        if (entityHit.entity instanceof Pig pig) {
            if (stack != null && !stack.isEmpty() && stack.getType() == ItemType.SADDLE && !pig.isSaddled()) {
                return saddlePig(pig, stack);
            }
            if (pig.isSaddled()) {
                return mountPig(pig);
            }
        }

        if (entityHit.entity instanceof Wolf wolf) {
            return handleWolfUse(wolf, stack);
        }

        if (entityHit.entity instanceof BoatEntity boat) {
            return mountBoat(boat);
        }

        if (entityHit.entity instanceof MinecartEntity minecart
                && minecart.getKind() == MinecartEntity.CartKind.RIDEABLE) {
            return mountMinecart(minecart);
        }

        if (entityHit.entity instanceof ChestMinecartEntity chestCart) {
            requestedChestMinecart = chestCart;
            startUseAnimation();
            return true;
        }

        if (entityHit.entity instanceof FurnaceMinecartEntity furnaceCart) {
            if (stack != null && !stack.isEmpty() && isFurnaceMinecartFuel(stack.getType())) {
                furnaceCart.addFuel(position.x, position.z);
                consumePlacedStack(stack);
            } else {
                furnaceCart.setPushDirectionFrom(position.x, position.z);
                startUseAnimation();
            }
            return true;
        }

        return false;
    }

    boolean shearSheep(Sheep sheep, com.craftzero.inventory.ItemStack shears) {
        if (sheep == null || sheep.isRemoved() || shears == null || shears.isEmpty()
                || shears.getType() != ItemType.SHEARS) {
            return false;
        }
        if (!sheep.shear()) {
            return false;
        }
        recordItemUse(shears);
        if (!isCreative()) {
            damageHeldDurable(shears);
        }
        startUseAnimation();
        return true;
    }

    boolean dyeSheep(Sheep sheep, com.craftzero.inventory.ItemStack dye) {
        if (sheep == null || sheep.isRemoved() || dye == null || dye.isEmpty()) {
            return false;
        }
        int woolColor = Sheep.woolColorForDye(dye.getType());
        if (woolColor < 0 || !sheep.dye(woolColor)) {
            return false;
        }
        consumePlacedStack(dye);
        return true;
    }

    boolean fillBowlFromMooshroom(Mooshroom mooshroom, com.craftzero.inventory.ItemStack bowl) {
        if (mooshroom == null || mooshroom.isRemoved() || bowl == null || bowl.isEmpty()
                || bowl.getType() != ItemType.BOWL || mooshroom.isBaby()) {
            return false;
        }
        recordItemUse(bowl);
        replaceHeldContainerItem(worldForEntity(mooshroom), bowl,
                new com.craftzero.inventory.ItemStack(ItemType.MUSHROOM_STEW, 1));
        return true;
    }

    boolean milkCow(Cow cow, com.craftzero.inventory.ItemStack bucket) {
        if (cow == null || cow.isRemoved() || bucket == null || bucket.isEmpty()
                || bucket.getType() != ItemType.BUCKET || cow.isBaby()) {
            return false;
        }
        recordItemUse(bucket);
        replaceHeldContainerItem(worldForEntity(cow), bucket,
                new com.craftzero.inventory.ItemStack(ItemType.MILK_BUCKET, 1));
        return true;
    }

    boolean feedBreedingAnimal(Mob mob, com.craftzero.inventory.ItemStack stack) {
        if (mob == null || mob.isRemoved() || stack == null || stack.isEmpty()) {
            return false;
        }
        if (!mob.feedBreedingItem(stack.getType())) {
            return false;
        }
        consumePlacedStack(stack);
        return true;
    }

    boolean shearMooshroom(Mooshroom mooshroom, com.craftzero.inventory.ItemStack shears) {
        if (mooshroom == null || mooshroom.isRemoved() || shears == null || shears.isEmpty()
                || shears.getType() != ItemType.SHEARS || mooshroom.isBaby()) {
            return false;
        }
        World entityWorld = worldForEntity(mooshroom);
        if (entityWorld == null) {
            return false;
        }

        Cow cow = new Cow();
        cow.setPosition(mooshroom.getX(), mooshroom.getY(), mooshroom.getZ());
        cow.setYaw(mooshroom.getYaw());
        cow.setPitch(mooshroom.getPitch());
        cow.setMotion(mooshroom.getMotionX(), mooshroom.getMotionY(), mooshroom.getMotionZ());
        cow.setHealth(mooshroom.getHealth());
        entityWorld.spawnEntity(cow);
        mooshroom.remove();
        entityWorld.spawnParticle(WorldParticle.Type.LARGE_EXPLOSION,
                mooshroom.getX(), mooshroom.getY() + mooshroom.getHeight() * 0.5f, mooshroom.getZ(),
                0.0f, 0.0f, 0.0f,
                MOOSHROOM_SHEAR_PARTICLE_SCALE, MOOSHROOM_SHEAR_PARTICLE_LIFETIME_TICKS);
        for (int i = 0; i < 5; i++) {
            entityWorld.spawnDroppedItem(mooshroom.getX(), mooshroom.getY() + mooshroom.getHeight() * 0.5f,
                    mooshroom.getZ(), ItemType.RED_MUSHROOM, 1);
        }
        recordItemUse(shears);
        if (!isCreative()) {
            damageHeldDurable(shears);
        }
        startUseAnimation();
        return true;
    }

    boolean saddlePig(Pig pig, com.craftzero.inventory.ItemStack saddle) {
        if (pig == null || pig.isRemoved() || saddle == null || saddle.isEmpty()
                || saddle.getType() != ItemType.SADDLE || !pig.saddle()) {
            return false;
        }
        consumePlacedStack(saddle);
        return true;
    }

    boolean tameWolf(Wolf wolf, com.craftzero.inventory.ItemStack bones, Random random) {
        if (wolf == null || wolf.isRemoved() || bones == null || bones.isEmpty()
                || bones.getType() != ItemType.BONE || !wolf.canAcceptBone()) {
            return false;
        }
        if (wolf.tryTameWithBone(random)) {
            wolf.setOwnerName(playerName);
        }
        consumePlacedStack(bones);
        return true;
    }

    private boolean handleWolfUse(Wolf wolf, com.craftzero.inventory.ItemStack stack) {
        if (stack != null && !stack.isEmpty()) {
            if (stack.getType() == ItemType.BONE && wolf.canAcceptBone()) {
                return tameWolf(wolf, stack, random);
            }
            if (Wolf.isWolfMeat(stack.getType()) && feedWolf(wolf, stack)) {
                return true;
            }
        }
        return toggleWolfSitting(wolf);
    }

    boolean feedWolf(Wolf wolf, com.craftzero.inventory.ItemStack meat) {
        if (wolf == null || wolf.isRemoved() || meat == null || meat.isEmpty()
                || !wolf.isOwnedBy(this) || !wolf.canEatMeat(meat.getType())) {
            return false;
        }
        wolf.feedMeat(meat.getType());
        consumePlacedStack(meat);
        return true;
    }

    boolean toggleWolfSitting(Wolf wolf) {
        if (wolf == null || wolf.isRemoved() || !wolf.isOwnedBy(this) || !wolf.toggleSitting()) {
            return false;
        }
        startUseAnimation();
        return true;
    }

    private boolean isFurnaceMinecartFuel(ItemType type) {
        return type == ItemType.COAL || type == ItemType.CHARCOAL;
    }

    private World worldForEntity(com.craftzero.entity.Entity entity) {
        return entity != null && entity.getWorld() != null ? entity.getWorld() : world;
    }

    private void replaceHeldContainerItem(World world, com.craftzero.inventory.ItemStack stack,
            com.craftzero.inventory.ItemStack result) {
        if (isCreative()) {
            startUseAnimation();
            return;
        }

        com.craftzero.inventory.ItemStack[] hotbar = inventory.getHotbar();
        int selected = inventory.getSelectedSlot();
        if (stack.getCount() <= 1 && hotbar[selected] == stack) {
            hotbar[selected] = result;
            startUseAnimation();
            return;
        }

        stack.remove(1);
        if (stack.isEmpty() && hotbar[selected] == stack) {
            hotbar[selected] = null;
        }
        if (!inventory.addItem(result) && result != null && !result.isEmpty() && world != null) {
            Vector3f forward = camera.getForward();
            world.spawnThrownStack(position.x + forward.x * 0.5f, position.y + EYE_HEIGHT,
                    position.z + forward.z * 0.5f, result, forward.x * 0.2f, 0.2f, forward.z * 0.2f);
        }
        startUseAnimation();
    }

    boolean mountMinecart(MinecartEntity minecart) {
        if (minecart == null || minecart.isRemoved() || minecart.getKind() != MinecartEntity.CartKind.RIDEABLE) {
            return false;
        }
        if (ridingMinecart == minecart) {
            startUseAnimation();
            return true;
        }
        dismountCurrentVehicle();
        if (!minecart.mountPlayer()) {
            return false;
        }
        ridingMinecart = minecart;
        startMinecartRideTracking(minecart);
        syncRidingPosition();
        startUseAnimation();
        return true;
    }

    boolean mountBoat(BoatEntity boat) {
        if (boat == null || boat.isRemoved()) {
            return false;
        }
        if (ridingBoat == boat) {
            startUseAnimation();
            return true;
        }
        dismountCurrentVehicle();
        if (!boat.mountPlayer()) {
            return false;
        }
        ridingBoat = boat;
        syncRidingPosition();
        startUseAnimation();
        return true;
    }

    boolean mountPig(Pig pig) {
        if (pig == null || pig.isRemoved() || !pig.isSaddled()) {
            return false;
        }
        if (ridingPig == pig) {
            startUseAnimation();
            return true;
        }
        dismountCurrentVehicle();
        if (!pig.mountPlayer()) {
            return false;
        }
        ridingPig = pig;
        syncRidingPosition();
        startUseAnimation();
        return true;
    }

    public void dismountMinecart() {
        if (ridingMinecart == null) {
            return;
        }
        MinecartEntity cart = ridingMinecart;
        lastDismountedVehicle = cart;
        cart.dismountPlayer();
        ridingMinecart = null;
        clearMinecartRideTracking();
        clearMountedInput();
        Vector3f dismount = findSafeVehicleDismountPosition(cart.getX(), cart.getY() + 0.1f,
                cart.getZ(), cart.getWidth(), cart.getWidth());
        setPosition(dismount.x, dismount.y, dismount.z);
        velocity.set(cart.getMotionX(), 0.0f, cart.getMotionZ());
        fallStartY = position.y;
    }

    public void dismountBoat() {
        if (ridingBoat == null) {
            return;
        }
        BoatEntity boat = ridingBoat;
        lastDismountedVehicle = boat;
        boat.dismountPlayer();
        ridingBoat = null;
        clearMountedInput();
        Vector3f dismount = findSafeVehicleDismountPosition(boat.getX(), boat.getY() + 0.1f,
                boat.getZ(), boat.getWidth(), boat.getWidth());
        setPosition(dismount.x, dismount.y, dismount.z);
        velocity.set(boat.getMotionX(), 0.0f, boat.getMotionZ());
        fallStartY = position.y;
    }

    public void dismountPig() {
        if (ridingPig == null) {
            return;
        }
        Pig pig = ridingPig;
        lastDismountedVehicle = pig;
        pig.dismountPlayer();
        ridingPig = null;
        clearMountedInput();
        Vector3f dismount = findSafeVehicleDismountPosition(pig.getX(), pig.getY() + 0.1f,
                pig.getZ(), pig.getWidth(), pig.getWidth());
        setPosition(dismount.x, dismount.y, dismount.z);
        velocity.set(pig.getMotionX(), 0.0f, pig.getMotionZ());
        fallStartY = position.y;
    }

    private Vector3f findSafeVehicleDismountPosition(float vehicleX, float vehicleY, float vehicleZ,
            float vehicleWidth, float vehicleDepth) {
        float sideX = Math.max(vehicleWidth * 0.5f + WIDTH * 0.5f + 0.05f, vehicleWidth);
        float sideZ = Math.max(vehicleDepth * 0.5f + WIDTH * 0.5f + 0.05f, vehicleDepth);
        float[][] offsets = {
                { sideX, 0.0f },
                { -sideX, 0.0f },
                { 0.0f, sideZ },
                { 0.0f, -sideZ },
                { sideX, sideZ },
                { sideX, -sideZ },
                { -sideX, sideZ },
                { -sideX, -sideZ },
                { 0.0f, 0.0f }
        };
        for (float[] offset : offsets) {
            float x = vehicleX + offset[0];
            float z = vehicleZ + offset[1];
            if (isPlayerBoxClearAt(x, vehicleY, z)) {
                return new Vector3f(x, vehicleY, z);
            }
        }
        return new Vector3f(vehicleX + vehicleWidth, vehicleY, vehicleZ);
    }

    private boolean isPlayerBoxClearAt(float x, float y, float z) {
        if (world == null) {
            return true;
        }
        float halfWidth = WIDTH * 0.5f;
        AABB candidate = new AABB(x - halfWidth, y, z - halfWidth,
                x + halfWidth, y + HEIGHT, z + halfWidth);
        int minX = (int) Math.floor(candidate.getMin().x);
        int minY = (int) Math.floor(candidate.getMin().y);
        int minZ = (int) Math.floor(candidate.getMin().z);
        int maxX = (int) Math.floor(candidate.getMax().x - 0.0001f);
        int maxY = (int) Math.floor(candidate.getMax().y - 0.0001f);
        int maxZ = (int) Math.floor(candidate.getMax().z - 0.0001f);
        for (int bx = minX; bx <= maxX; bx++) {
            for (int by = minY; by <= maxY; by++) {
                for (int bz = minZ; bz <= maxZ; bz++) {
                    for (AABB collision : world.getCollisionBoxesIfLoaded(bx, by, bz)) {
                        if (candidate.intersects(collision)) {
                            return false;
                        }
                    }
                }
            }
        }
        return true;
    }

    private void dismountVehicle() {
        if (ridingMinecart != null) {
            dismountMinecart();
            return;
        }
        if (ridingBoat != null) {
            dismountBoat();
            return;
        }
        if (ridingPig != null) {
            dismountPig();
        }
    }

    private void dismountCurrentVehicle() {
        if (ridingMinecart != null) {
            ridingMinecart.dismountPlayer();
            ridingMinecart = null;
            clearMinecartRideTracking();
        }
        if (ridingBoat != null) {
            ridingBoat.dismountPlayer();
            ridingBoat = null;
        }
        if (ridingPig != null) {
            ridingPig.dismountPlayer();
            ridingPig = null;
        }
        clearMountedInput();
    }

    private void clearMountedInput() {
        mountedForwardInput = 0.0f;
        mountedStrafeInput = 0.0f;
        mountedYawInput = camera.getYaw();
    }

    private void clearRemovedVehicleRefs() {
        if (ridingMinecart != null && ridingMinecart.isRemoved()) {
            ridingMinecart = null;
            clearMinecartRideTracking();
        }
        if (ridingBoat != null && ridingBoat.isRemoved()) {
            ridingBoat = null;
        }
        if (ridingPig != null && ridingPig.isRemoved()) {
            ridingPig = null;
        }
    }

    private boolean isRidingVehicle() {
        return isRidingMinecart() || isRidingBoat() || isRidingPig();
    }

    private void applyMountedInput(float forward, float strafe) {
        if (isRidingMinecart() && forward > 0.0f) {
            ridingMinecart.applyRiderInput(camera.getYaw());
        }
        if (isRidingBoat()) {
            ridingBoat.applyRiderInput(camera.getYaw(), forward, strafe);
        }
    }

    public void syncRidingPosition() {
        syncRidingPosition(false);
    }

    private void syncRidingPosition(boolean recordTravel) {
        clearRemovedVehicleRefs();
        if (ridingMinecart != null) {
            if (recordTravel) {
                stats.getStatistics().recordDistanceByMinecart(horizontalTravelDistance(ridingMinecart));
            }
            setPosition(ridingMinecart.getX(), ridingMinecart.getY() + 0.1f, ridingMinecart.getZ());
            recordMinecartRideProgress(ridingMinecart);
            velocity.set(0.0f, 0.0f, 0.0f);
            onGround = true;
            fallStartY = position.y;
            return;
        }
        if (ridingBoat != null) {
            if (recordTravel) {
                stats.getStatistics().recordDistanceByBoat(horizontalTravelDistance(ridingBoat));
            }
            setPosition(ridingBoat.getX(), ridingBoat.getY() + 0.1f, ridingBoat.getZ());
            velocity.set(0.0f, 0.0f, 0.0f);
            onGround = true;
            fallStartY = position.y;
            return;
        }
        if (ridingPig != null) {
            if (recordTravel) {
                stats.getStatistics().recordDistanceByPig(horizontalTravelDistance(ridingPig));
            }
            setPosition(ridingPig.getX(), ridingPig.getY() + 0.1f, ridingPig.getZ());
            velocity.set(0.0f, 0.0f, 0.0f);
            onGround = true;
            fallStartY = position.y;
        }
    }

    public boolean isRidingMinecart() {
        return ridingMinecart != null && !ridingMinecart.isRemoved();
    }

    public MinecartEntity getRidingMinecart() {
        return isRidingMinecart() ? ridingMinecart : null;
    }

    public boolean isRidingBoat() {
        return ridingBoat != null && !ridingBoat.isRemoved();
    }

    public BoatEntity getRidingBoat() {
        return isRidingBoat() ? ridingBoat : null;
    }

    public boolean isRidingPig() {
        return ridingPig != null && !ridingPig.isRemoved();
    }

    public Pig getRidingPig() {
        return isRidingPig() ? ridingPig : null;
    }

    public float getMountedForwardInput() {
        return mountedForwardInput;
    }

    public float getMountedStrafeInput() {
        return mountedStrafeInput;
    }

    public float getMountedYawInput() {
        return mountedYawInput;
    }

    public com.craftzero.entity.Entity drainLastDismountedVehicle() {
        com.craftzero.entity.Entity vehicle = lastDismountedVehicle;
        lastDismountedVehicle = null;
        return vehicle;
    }

    public boolean restoreVehicleMount(Entity entity) {
        if (entity == null || entity.isRemoved()) {
            return false;
        }
        dismountCurrentVehicle();
        if (entity instanceof MinecartEntity minecart
                && minecart.getKind() == MinecartEntity.CartKind.RIDEABLE
                && minecart.mountPlayer()) {
            ridingMinecart = minecart;
            startMinecartRideTracking(minecart);
            syncRidingPosition();
            return true;
        }
        if (entity instanceof BoatEntity boat && boat.mountPlayer()) {
            ridingBoat = boat;
            syncRidingPosition();
            return true;
        }
        if (entity instanceof Pig pig && pig.isSaddled() && pig.mountPlayer()) {
            ridingPig = pig;
            syncRidingPosition();
            return true;
        }
        return false;
    }

    public void collideWithMinecart(MinecartEntity cart) {
        if (cart == null || cart.isRemoved() || isRidingVehicle()) {
            return;
        }
        MinecartEntity.NonCartCollisionPush push = cart.computeNonCartCollisionPush(position.x, position.z);
        if (push == null) {
            return;
        }

        cart.addMotion(push.cartX(), 0.0f, push.cartZ());
        velocity.x += push.entityX();
        velocity.z += push.entityZ();
    }

    public void collideWithBoat(BoatEntity boat) {
        if (boat == null || boat.isRemoved() || isRidingVehicle()) {
            return;
        }
        float dx = position.x - boat.getX();
        float dz = position.z - boat.getZ();
        float maxAxis = Math.max(Math.abs(dx), Math.abs(dz));
        if (maxAxis < BOAT_COLLISION_MIN_AXIS) {
            dx = boat.getMotionX() - velocity.x;
            dz = boat.getMotionZ() - velocity.z;
            maxAxis = Math.max(Math.abs(dx), Math.abs(dz));
            if (maxAxis < BOAT_COLLISION_MIN_AXIS) {
                dx = 1.0f;
                dz = 0.0f;
                maxAxis = 1.0f;
            }
        }

        float distance = (float) Math.sqrt(maxAxis);
        dx /= distance;
        dz /= distance;
        float pushScale = Math.min(1.0f, 1.0f / distance) * BOAT_COLLISION_IMPULSE;
        float pushX = dx * pushScale;
        float pushZ = dz * pushScale;

        boat.addMotion(-pushX, 0.0f, -pushZ);
        velocity.x += pushX;
        velocity.z += pushZ;
    }

    public FishingHookEntity getFishingHook() {
        return fishingHook != null && !fishingHook.isRemoved() ? fishingHook : null;
    }

    public void clearFishingHook(FishingHookEntity hook) {
        if (fishingHook == hook) {
            fishingHook = null;
        }
    }

    public void attachFishingHook(FishingHookEntity hook) {
        fishingHook = hook;
    }

    private boolean handleTargetedItemUse(World world, com.craftzero.inventory.ItemStack stack, BlockType clickedBlock) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        ItemType type = stack.getType();
        if (type == ItemType.PAINTING && isHorizontalFace(targetBlock.face)) {
            if (world.placePainting(targetBlock.blockPos.x, targetBlock.blockPos.y,
                    targetBlock.blockPos.z, targetBlock.face) == null) {
                return false;
            }
            consumePlacedStack(stack);
            recordItemAction(ItemActionType.PLACE_PAINTING, type, camera.getForward(), 0.0f,
                    targetBlock.blockPos.x, targetBlock.blockPos.y, targetBlock.blockPos.z, targetBlock.face);
            return true;
        }
        if ((type == ItemType.MINECART || type == ItemType.CHEST_MINECART || type == ItemType.FURNACE_MINECART)
                && (clickedBlock == BlockType.RAIL || clickedBlock == BlockType.POWERED_RAIL
                        || clickedBlock == BlockType.DETECTOR_RAIL)) {
            if (world.placeMinecartOnRail(targetBlock.blockPos.x, targetBlock.blockPos.y, targetBlock.blockPos.z, type)) {
                consumePlacedStack(stack);
                recordItemAction(ItemActionType.PLACE_MINECART, type, camera.getForward(), 0.0f,
                        targetBlock.blockPos.x, targetBlock.blockPos.y, targetBlock.blockPos.z);
                return true;
            }
        }
        if (type.isRecord() && clickedBlock == BlockType.JUKEBOX) {
            TileEntity tile = world.getTileEntity(targetBlock.blockPos.x, targetBlock.blockPos.y, targetBlock.blockPos.z);
            if (tile instanceof JukeboxTileEntity jukebox
                    && !jukebox.hasRecord()
                    && world.getBlockMetadataIfLoaded(targetBlock.blockPos.x, targetBlock.blockPos.y,
                            targetBlock.blockPos.z, -1) == 0) {
                if (jukebox.insertRecord(world, stack)) {
                    recordItemUse(stack);
                    jukebox.play(world);
                    recordItemAction(ItemActionType.INSERT_RECORD, type, camera.getForward(), 0.0f,
                            targetBlock.blockPos.x, targetBlock.blockPos.y, targetBlock.blockPos.z);
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
            if (world.fillCauldronFromWaterBucket(targetBlock.blockPos.x, targetBlock.blockPos.y,
                    targetBlock.blockPos.z)) {
                recordItemUse(type);
                if (!isCreative()) {
                    replaceHeldItemAfterBucketUse(world, stack, ItemType.BUCKET);
                } else {
                    startUseAnimation();
                }
                world.rebuildBlockMeshesNow(targetBlock.blockPos.x, targetBlock.blockPos.y, targetBlock.blockPos.z);
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
            recordItemUse(type);
            damageHeldDurable(stack);
            startUseAnimation();
            world.rebuildBlockMeshesNow(targetBlock.blockPos.x, targetBlock.blockPos.y, targetBlock.blockPos.z);
            return true;
        }
        if (type == ItemType.BONE_MEAL) {
            if (world.applyBoneMealToPlant(targetBlock.blockPos.x, targetBlock.blockPos.y, targetBlock.blockPos.z)) {
                consumePlacedStack(stack);
                world.rebuildBlockMeshesNow(targetBlock.blockPos.x, targetBlock.blockPos.y, targetBlock.blockPos.z);
                return true;
            }
        }
        BlockType cropToPlant = cropBlockForSeed(type);
        if (cropToPlant != null && clickedBlock == BlockType.FARMLAND && targetBlock.face == Block.FACE_TOP) {
            int cropY = targetBlock.blockPos.y + 1;
            if (world.getBlockIfLoaded(targetBlock.blockPos.x, cropY, targetBlock.blockPos.z, BlockType.AIR) != BlockType.AIR) {
                return false;
            }
            world.setBlock(targetBlock.blockPos.x, cropY, targetBlock.blockPos.z, cropToPlant, 0);
            consumePlacedStack(stack);
            world.rebuildBlockMeshesNow(targetBlock.blockPos.x, cropY, targetBlock.blockPos.z);
            return true;
        }
        if (type == ItemType.FLINT_AND_STEEL && targetBlock.previousBlockPos != null) {
            if (clickedBlock == BlockType.TNT) {
                if (world.primeTntByPlayer(targetBlock.blockPos.x, targetBlock.blockPos.y,
                        targetBlock.blockPos.z, 80) == null) {
                    return false;
                }
                recordItemUse(type);
                damageHeldDurable(stack);
                startUseAnimation();
                world.rebuildBlockMeshesNow(targetBlock.blockPos.x, targetBlock.blockPos.y, targetBlock.blockPos.z);
                return true;
            }
            Vector3i pos = targetBlock.previousBlockPos;
            if (world.getBlockIfLoaded(pos.x, pos.y, pos.z, BlockType.AIR) != BlockType.AIR) {
                return false;
            }
            if (world.canPlaceBlockAt(pos.x, pos.y, pos.z, BlockType.FIRE, 0, null)) {
                world.setBlock(pos.x, pos.y, pos.z, BlockType.FIRE, 0);
                world.playFireIgniteSound(pos.x + 0.5f, pos.y + 0.5f, pos.z + 0.5f);
                recordItemUse(type);
                damageHeldDurable(stack);
                startUseAnimation();
                world.rebuildBlockMeshesNow(pos.x, pos.y, pos.z);
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
            rebuildEndPortalFrameUseMeshes(world, targetBlock.blockPos.x, targetBlock.blockPos.y,
                    targetBlock.blockPos.z);
            return true;
        }
        return false;
    }

    private boolean fillBottle(World world, com.craftzero.inventory.ItemStack stack, BlockType clickedBlock) {
        boolean filled = false;
        boolean drainedCauldron = false;
        if ((clickedBlock == BlockType.WATER || clickedBlock == BlockType.FLOWING_WATER)
                && world.getBlockMetadataIfLoaded(targetBlock.blockPos.x, targetBlock.blockPos.y,
                        targetBlock.blockPos.z, -1) == 0) {
            filled = true;
        } else if (clickedBlock == BlockType.CAULDRON) {
            filled = world.drainCauldronIntoBottle(targetBlock.blockPos.x, targetBlock.blockPos.y,
                    targetBlock.blockPos.z);
            drainedCauldron = filled;
        }
        if (!filled) {
            return false;
        }
        recordItemUse(stack);
        if (!isCreative()) {
            replaceHeldItemAfterBottleFill(world, stack);
        } else {
            startUseAnimation();
        }
        if (drainedCauldron) {
            world.rebuildBlockMeshesNow(targetBlock.blockPos.x, targetBlock.blockPos.y, targetBlock.blockPos.z);
        }
        return true;
    }

    private void rebuildEndPortalFrameUseMeshes(World world, int frameX, int frameY, int frameZ) {
        world.rebuildBlockMeshesNow(frameX, frameY, frameZ);
        for (int dx = -3; dx <= 3; dx++) {
            for (int dz = -3; dz <= 3; dz++) {
                int x = frameX + dx;
                int z = frameZ + dz;
                if (world.isEndPortalAt(x, frameY, z)) {
                    world.rebuildBlockMeshesNow(x, frameY, z);
                }
            }
        }
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

    private static BlockType cropBlockForSeed(ItemType type) {
        return switch (type) {
            case SEEDS -> BlockType.CROPS;
            case PUMPKIN_SEEDS -> BlockType.PUMPKIN_STEM;
            case MELON_SEEDS -> BlockType.MELON_STEM;
            default -> null;
        };
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
        recordItemUse(stack.getType());
        boolean drops = random.nextFloat() >= 0.20f;
        EyeOfEnderEntity eye = new EyeOfEnderEntity(position.x, position.y + EYE_HEIGHT - 0.2f, position.z,
                target.blockX() + 0.5f, target.blockY() + 1.0f, target.blockZ() + 0.5f, drops);
        eye.moveTowards(target.blockX() + 0.5f, target.blockY() + 1.0f, target.blockZ() + 0.5f);
        Vector3f forward = camera.getForward();
        eye.setMotion(forward.x * 0.5f, forward.y * 0.5f + 0.15f, forward.z * 0.5f);
        world.spawnEntity(eye);
        world.playThrowSound(position.x, position.y + EYE_HEIGHT, position.z);
        recordItemAction(ItemActionType.EYE_OF_ENDER, ItemType.EYE_OF_ENDER, forward, 0.0f);
        startUseAnimation();
        return true;
    }

    boolean throwThrownItemProjectile(World world, com.craftzero.inventory.ItemStack stack, Vector3f direction) {
        if (world == null || stack == null || stack.isEmpty()
                || (stack.getType() != ItemType.EGG && stack.getType() != ItemType.SNOWBALL)
                || direction == null) {
            return false;
        }
        Vector3f normalized = new Vector3f(direction);
        if (normalized.lengthSquared() < 0.0001f) {
            return false;
        }
        normalized.normalize();
        Vector3f spawn = new Vector3f(position.x, position.y + EYE_HEIGHT - 0.1f, position.z)
                .add(new Vector3f(normalized).mul(0.35f));
        ThrownItemEntity projectile = world.spawnThrownItemProjectile(spawn.x, spawn.y, spawn.z,
                normalized.x * 1.5f,
                normalized.y * 1.5f,
                normalized.z * 1.5f,
                stack.getType(),
                null,
                true);
        projectile.setYaw(camera.getYaw());
        projectile.setPitch(camera.getPitch());
        world.playThrowSound(spawn.x, spawn.y, spawn.z);
        recordItemAction(ItemActionType.THROW_ITEM, stack.getType(), normalized, 0.0f);
        if (!isCreative()) {
            stack.remove(1);
            if (stack.isEmpty()) {
                inventory.getHotbar()[inventory.getSelectedSlot()] = null;
            }
        }
        recordItemUse(stack.getType());
        startUseAnimation();
        return true;
    }

    boolean throwEnderPearl(World world, com.craftzero.inventory.ItemStack stack, Vector3f direction) {
        if (world == null || stack == null || stack.isEmpty() || stack.getType() != ItemType.ENDER_PEARL
                || direction == null) {
            return false;
        }
        Vector3f normalized = new Vector3f(direction);
        if (normalized.lengthSquared() < 0.0001f) {
            return false;
        }
        normalized.normalize();
        Vector3f spawn = new Vector3f(position.x, position.y + EYE_HEIGHT - 0.1f, position.z)
                .add(new Vector3f(normalized).mul(0.35f));
        EnderPearlEntity pearl = world.spawnEnderPearl(spawn.x, spawn.y, spawn.z,
                normalized.x * 1.5f,
                normalized.y * 1.5f,
                normalized.z * 1.5f,
                this);
        pearl.setYaw(camera.getYaw());
        pearl.setPitch(camera.getPitch());
        world.playThrowSound(spawn.x, spawn.y, spawn.z);
        recordItemAction(ItemActionType.ENDER_PEARL, ItemType.ENDER_PEARL, normalized, 0.0f);
        if (!isCreative()) {
            stack.remove(1);
            if (stack.isEmpty()) {
                inventory.getHotbar()[inventory.getSelectedSlot()] = null;
            }
        }
        recordItemUse(stack.getType());
        startUseAnimation();
        return true;
    }

    boolean useFishingRod(World world, com.craftzero.inventory.ItemStack stack, Vector3f direction) {
        if (world == null || stack == null || stack.isEmpty() || stack.getType() != ItemType.FISHING_ROD
                || direction == null) {
            return false;
        }
        if (fishingHook != null && !fishingHook.isRemoved()) {
            if (fishingHook.getOwner() != this) {
                fishingHook.remove();
                fishingHook = null;
                recordItemUse(stack);
                recordItemAction(ItemActionType.FISHING_REEL, ItemType.FISHING_ROD, direction, 0.0f);
                startUseAnimation();
                return true;
            }
            FishingHookEntity.ReelResult reelResult = fishingHook.reelInWithResult();
            recordItemUse(stack);
            recordItemAction(ItemActionType.FISHING_REEL, ItemType.FISHING_ROD, direction, 0.0f);
            if (reelResult.caughtFish()) {
                stats.getStatistics().recordFishCaught();
            }
            damageHeldDurable(stack, reelResult.durabilityCost());
            startUseAnimation();
            return true;
        }
        Vector3f normalized = new Vector3f(direction);
        if (normalized.lengthSquared() < 0.0001f) {
            return false;
        }
        normalized.normalize();
        Vector3f spawn = new Vector3f(position.x, position.y + EYE_HEIGHT - 0.15f, position.z)
                .add(new Vector3f(normalized).mul(0.45f));
        fishingHook = new FishingHookEntity(spawn.x, spawn.y, spawn.z,
                normalized.x * 1.5f,
                normalized.y * 1.5f + 0.1f,
                normalized.z * 1.5f,
                this);
        fishingHook.setYaw(camera.getYaw());
        fishingHook.setPitch(camera.getPitch());
        world.spawnEntity(fishingHook);
        world.playThrowSound(spawn.x, spawn.y, spawn.z);
        recordItemUse(stack);
        recordItemAction(ItemActionType.FISHING_CAST, ItemType.FISHING_ROD, normalized, 0.0f);
        startUseAnimation();
        return true;
    }

    private void playEatCompleteSounds(World world) {
        world.playEatSound(position.x, position.y + EYE_HEIGHT * 0.5f, position.z);
        world.playBurpSound(position.x, position.y + EYE_HEIGHT * 0.5f, position.z);
    }

    private void playDrinkSound(World world) {
        world.playDrinkSound(position.x, position.y + EYE_HEIGHT * 0.5f, position.z);
    }

    private void playHeldConsumableTickSound(World world) {
        if (world == null || consumingItemType == null) {
            return;
        }
        if (consumingItemType == ItemType.MILK_BUCKET || consumingItemType == ItemType.POTION) {
            playDrinkSound(world);
        } else {
            spawnFoodUseParticles(world, consumingItemType, CONSUMABLE_TICK_CRUMB_PARTICLES);
            world.playEatSound(position.x, position.y + EYE_HEIGHT * 0.5f, position.z);
        }
    }

    private void spawnFoodUseParticles(World world, ItemType type, int count) {
        if (world == null || type == null || count <= 0) {
            return;
        }
        Random particleRandom = world.getRandom();
        for (int i = 0; i < count; i++) {
            Vector3f motion = rotateUseParticleVector(
                    (particleRandom.nextFloat() - 0.5f) * CONSUMABLE_CRUMB_MOTION_HORIZONTAL_SPREAD,
                    particleRandom.nextFloat() * CONSUMABLE_CRUMB_MOTION_Y_SPREAD + CONSUMABLE_CRUMB_MOTION_MIN_Y,
                    0.0f);
            Vector3f offset = rotateUseParticleVector(
                    (particleRandom.nextFloat() - 0.5f) * CONSUMABLE_CRUMB_POSITION_HORIZONTAL_SPREAD,
                    -particleRandom.nextFloat() * CONSUMABLE_CRUMB_POSITION_Y_SPREAD + CONSUMABLE_CRUMB_POSITION_MIN_Y,
                    CONSUMABLE_CRUMB_POSITION_FORWARD);
            world.spawnParticle(WorldParticle.Type.ITEM_CRACK,
                    position.x + offset.x,
                    position.y + EYE_HEIGHT + offset.y,
                    position.z + offset.z,
                    motion.x,
                    motion.y,
                    motion.z,
                    CONSUMABLE_CRUMB_SCALE,
                    CONSUMABLE_CRUMB_LIFETIME_TICKS,
                    WorldParticle.itemParticleData(type));
        }
    }

    private Vector3f rotateUseParticleVector(float x, float y, float z) {
        float pitchRadians = (float) Math.toRadians(-camera.getPitch());
        float yawRadians = (float) Math.toRadians(-camera.getYaw());
        float cosPitch = (float) Math.cos(pitchRadians);
        float sinPitch = (float) Math.sin(pitchRadians);
        float rotatedY = y * cosPitch - z * sinPitch;
        float rotatedZ = y * sinPitch + z * cosPitch;
        float cosYaw = (float) Math.cos(yawRadians);
        float sinYaw = (float) Math.sin(yawRadians);
        return new Vector3f(
                x * cosYaw + rotatedZ * sinYaw,
                rotatedY,
                -x * sinYaw + rotatedZ * cosYaw);
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
            replaceHeldFoodContainer(world, stack, ItemType.BOWL);
            return;
        }
        stack.remove(1);
        if (stack.isEmpty()) {
            inventory.getHotbar()[inventory.getSelectedSlot()] = null;
        }
    }

    private void replaceHeldFoodContainer(World world, com.craftzero.inventory.ItemStack stack, ItemType container) {
        com.craftzero.inventory.ItemStack[] hotbar = inventory.getHotbar();
        int selected = inventory.getSelectedSlot();
        if (stack.getCount() <= 1) {
            hotbar[selected] = new com.craftzero.inventory.ItemStack(container, 1);
            return;
        }
        stack.remove(1);
        com.craftzero.inventory.ItemStack containerStack = new com.craftzero.inventory.ItemStack(container, 1);
        if (!inventory.addItem(containerStack) && !containerStack.isEmpty() && world != null) {
            Vector3f forward = camera.getForward();
            world.spawnThrownStack(position.x + forward.x * 0.5f, position.y + EYE_HEIGHT, position.z + forward.z * 0.5f,
                    containerStack, forward.x * 0.2f, 0.2f, forward.z * 0.2f);
        }
    }

    private boolean isHoe(ItemType type) {
        return type == ItemType.WOODEN_HOE || type == ItemType.STONE_HOE || type == ItemType.IRON_HOE
                || type == ItemType.DIAMOND_HOE || type == ItemType.GOLD_HOE;
    }

    private void damageHeldDurable(com.craftzero.inventory.ItemStack stack) {
        damageHeldDurable(stack, 1);
    }

    private void damageHeldDurable(com.craftzero.inventory.ItemStack stack, int amount) {
        if (isCreative() || stack == null || !stack.isDamageable() || amount <= 0) {
            return;
        }
        for (int i = 0; i < amount; i++) {
            if (useDurabilityWithEnchantments(stack)) {
                inventory.getHotbar()[inventory.getSelectedSlot()] = null;
                return;
            }
        }
    }

    private boolean useDurabilityWithEnchantments(com.craftzero.inventory.ItemStack stack) {
        if (stack == null || !stack.isDamageable()) {
            return false;
        }
        if (EnchantmentResolver.shouldPreventDurabilityLoss(stack, random)) {
            return false;
        }
        ItemType type = stack.getType();
        boolean depleted = stack.useDurability();
        if (depleted) {
            if (world != null) {
                world.spawnItemBreakParticles(type, position.x, position.y + EYE_HEIGHT * 0.75f, position.z);
            }
            stats.getStatistics().recordItemDepleted(type);
        }
        return depleted;
    }

    private static float horizontalTravelDistance(Entity entity) {
        float dx = entity.getX() - entity.getPrevX();
        float dz = entity.getZ() - entity.getPrevZ();
        return (float) Math.sqrt(dx * dx + dz * dz);
    }

    private FoodValue foodValue(ItemType type) {
        return switch (type) {
            case APPLE -> new FoodValue(4, 2.4f);
            case BREAD -> new FoodValue(5, 6.0f);
            case MUSHROOM_STEW -> new FoodValue(6, 7.2f);
            case RAW_PORKCHOP, RAW_BEEF -> new FoodValue(3, 1.8f);
            case COOKED_PORKCHOP, STEAK -> new FoodValue(8, 12.8f);
            case GOLDEN_APPLE -> new FoodValue(10, 24.0f);
            case COOKIE, RAW_FISH -> new FoodValue(2, 0.4f);
            case MELON_SLICE, RAW_CHICKEN -> new FoodValue(2, 1.2f);
            case COOKED_FISH -> new FoodValue(5, 6.0f);
            case COOKED_CHICKEN -> new FoodValue(6, 7.2f);
            case ROTTEN_FLESH -> new FoodValue(4, 0.8f);
            case SPIDER_EYE -> new FoodValue(2, 3.2f);
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
            recordItemUse(type);
            if (isCreative()) {
                startUseAnimation();
                world.rebuildBlockMeshesNow(targetBlock.blockPos.x, targetBlock.blockPos.y, targetBlock.blockPos.z);
                return true;
            }
            replaceHeldItemAfterBucketUse(world, stack, filledBucket);
            world.rebuildBlockMeshesNow(targetBlock.blockPos.x, targetBlock.blockPos.y, targetBlock.blockPos.z);
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
        BlockType placedFluid = world.getBlockIfLoaded(placePos.x, placePos.y, placePos.z, BlockType.AIR);
        boolean placedSource = placedFluid == (water ? BlockType.WATER : BlockType.LAVA);
        recordItemUse(type);
        if (isCreative()) {
            startUseAnimation();
            if (placedSource) {
                world.rebuildBlockMeshesNow(placePos.x, placePos.y, placePos.z);
            }
            return true;
        }
        replaceHeldItemAfterBucketUse(world, stack, ItemType.BUCKET);
        if (placedSource) {
            world.rebuildBlockMeshesNow(placePos.x, placePos.y, placePos.z);
        }
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
        int metadata = BlockShape.wallAttachmentMetadataFromFace(targetBlock.face);
        if (metadata < 0) {
            return false;
        }
        if (world.canPlaceBlockAt(placePos.x, placePos.y, placePos.z, BlockType.WALL_SIGN, metadata, boundingBox)) {
            world.setBlock(placePos.x, placePos.y, placePos.z, BlockType.WALL_SIGN, metadata);
            requestedSignEditPos = new Vector3i(placePos);
            return true;
        }
        return false;
    }

    private void consumePlacedStack(com.craftzero.inventory.ItemStack stack) {
        recordItemUse(stack);
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
        }
        startUseAnimation();
    }

    private void recordItemUse(com.craftzero.inventory.ItemStack stack) {
        if (stack != null && !stack.isEmpty()) {
            recordItemUse(stack.getType());
        }
    }

    private void recordItemUse(ItemType type) {
        stats.getStatistics().recordItemUsed(type);
    }

    private int getPlacementMetadata(ItemType itemType, BlockType placedBlock) {
        if (placedBlock == BlockType.PISTON || placedBlock == BlockType.STICKY_PISTON) {
            return getPistonPlacementMetadata(targetBlock != null ? targetBlock.previousBlockPos : null);
        }
        if (placedBlock == BlockType.CHEST || placedBlock.isFurnace()
                || placedBlock == BlockType.DISPENSER) {
            return getPlacementFacingMetadata();
        }
        if (placedBlock == BlockType.TORCH
                || placedBlock == BlockType.REDSTONE_TORCH_ON
                || placedBlock == BlockType.REDSTONE_TORCH_OFF) {
            return BlockShape.torchMetadataFromFace(targetBlock.face);
        }
        if (placedBlock == BlockType.STONE_BUTTON) {
            return BlockShape.buttonMetadataFromFace(targetBlock.face);
        }
        if (placedBlock == BlockType.LEVER) {
            int metadata = BlockShape.leverMetadataFromFace(targetBlock.face);
            return metadata >= 0 ? metadata : 5;
        }
        if (placedBlock == BlockType.REDSTONE_REPEATER_OFF || placedBlock == BlockType.REDSTONE_REPEATER_ON) {
            return getHorizontalFacingIndex();
        }
        if (placedBlock == BlockType.LADDER || placedBlock == BlockType.WALL_SIGN) {
            return BlockShape.wallAttachmentMetadataFromFace(targetBlock.face);
        }
        if (placedBlock == BlockType.VINES) {
            int metadata = BlockShape.vineMetadataFromFace(targetBlock.face);
            return metadata >= 0 ? metadata : 0;
        }
        if (placedBlock == BlockType.TRAPDOOR) {
            return World.horizontalIndexFromFace(targetBlock.face);
        }
        if (placedBlock.isStairs() || placedBlock.isFenceGate()) {
            return getHorizontalFacingIndex();
        }
        if (placedBlock == BlockType.LEAVES) {
            return itemType.getPlacedBlockMetadata() | World.LEAF_PERSISTENT_BIT;
        }
        return itemType.getPlacedBlockMetadata();
    }

    /**
     * Attack a living entity with the held weapon.
     * Implements Minecraft pre-1.9 combat with proper spam-click prevention.
     *
     * CRITICAL: Knockback is ONLY applied if damage() returns true.
     * This prevents spam-clicking from stacking knockback during immunity.
     */
    private void attackMinecart(MinecartEntity minecart) {
        if (minecart == null || minecart.isRemoved()) {
            return;
        }
        com.craftzero.inventory.ItemStack heldItem = inventory.getItemInHand();
        float damage = 1.0f;
        if (heldItem != null && heldItem.isTool()) {
            damage = heldItem.getType().getToolType().getAttackDamage();
        }
        if (minecart.attack(damage, isCreative()) && !isCreative()
                && heldItem != null && heldItem.isTool()) {
            boolean toolBroke = useDurabilityWithEnchantments(heldItem);
            if (toolBroke) {
                inventory.getHotbar()[inventory.getSelectedSlot()] = null;
            }
        }
    }

    private void attackBoat(BoatEntity boat) {
        if (boat == null || boat.isRemoved()) {
            return;
        }
        com.craftzero.inventory.ItemStack heldItem = inventory.getItemInHand();
        float damage = 1.0f;
        if (heldItem != null && heldItem.isTool()) {
            damage = heldItem.getType().getToolType().getAttackDamage();
        }
        if (boat.attack(damage, isCreative()) && !isCreative()
                && heldItem != null && heldItem.isTool()) {
            boolean toolBroke = useDurabilityWithEnchantments(heldItem);
            if (toolBroke) {
                inventory.getHotbar()[inventory.getSelectedSlot()] = null;
            }
        }
    }

    private void attackPainting(PaintingEntity painting) {
        if (painting == null || painting.isRemoved()) {
            return;
        }
        painting.breakAsItem(isCreative());
    }

    private void attackFireball(FireballEntity fireball, Vector3f direction) {
        if (fireball == null || fireball.isRemoved()) {
            return;
        }
        fireball.deflectFromPlayer(direction);
    }

    private void attackEntity(LivingEntity target) {
        if (target == null || target.isDead())
            return;

        if (target instanceof EndCrystalEntity crystal) {
            attackEndCrystal(crystal);
            return;
        }

        // 1. Calculate Damage
        float damage = 1.0f; // Base fist damage
        com.craftzero.inventory.ItemStack heldItem = inventory.getItemInHand();
        com.craftzero.inventory.ToolType toolType = com.craftzero.inventory.ToolType.NONE;

        if (heldItem != null && heldItem.isTool()) {
            toolType = heldItem.getType().getToolType();
            damage = toolType.getAttackDamage();
        }
        float enchantmentDamageBonus = EnchantmentResolver.attackDamageBonus(heldItem, target);
        damage += enchantmentDamageBonus;
        damage += stats.getAttackDamageBonus();
        damage = Math.max(0.0f, damage);

        // 2. Critical Hit Check (Pre-1.9 Logic)
        boolean isCritical = velocity.y < 0 && !onGround && !inWater && !flying;
        if (isCritical) {
            damage *= 1.5f;
        }

        // 3. Attempt to deal damage FIRST - capture the result
        // The mob returns 'false' if it is currently invulnerable
        float targetHealthBefore = target.getHealth();
        boolean successfulHit = target.damage(damage,
                DamageSource.playerAttack(
                        position.x, position.y + EYE_HEIGHT, position.z,
                        EnchantmentResolver.getLevel(heldItem, EnchantmentType.LOOTING)));
        if (successfulHit) {
            stats.getStatistics().recordSuccessfulAttack(Math.min(damage, targetHealthBefore));
            recordMonsterKillIfNeeded(target);
            stats.getAchievements().recordOverkillHit(damage);
        }
        if (successfulHit && isCritical) {
            spawnCriticalHitParticles(target);
        }
        if (successfulHit && enchantmentDamageBonus > 0.0f) {
            spawnMagicCriticalHitParticles(target);
        }
        if (successfulHit && !isCreative()) {
            stats.onAttack();
        }

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
        if (successfulHit && target.getHealth() > 0) {
            notifyTamedWolvesOfCombatTarget(target);
        }
    }

    private void attackEndCrystal(EndCrystalEntity crystal) {
        if (crystal == null || crystal.isDead() || crystal.isRemoved()) {
            return;
        }
        ItemStack heldItem = inventory.getItemInHand();
        boolean successfulHit = crystal.damage(0.0f,
                DamageSource.playerAttack(position.x, position.y + EYE_HEIGHT, position.z, 0));
        if (!successfulHit) {
            return;
        }
        if (!isCreative()) {
            stats.onAttack();
            if (heldItem != null && heldItem.isTool()) {
                boolean toolBroke = useDurabilityWithEnchantments(heldItem);
                if (toolBroke) {
                    inventory.getHotbar()[inventory.getSelectedSlot()] = null;
                }
            }
        }
    }

    private void spawnCriticalHitParticles(LivingEntity target) {
        if (world != null && target != null) {
            world.spawnEntityCritEmitter(WorldParticle.Type.CRIT, target);
        }
    }

    private void spawnMagicCriticalHitParticles(LivingEntity target) {
        if (world != null && target != null) {
            world.spawnEntityCritEmitter(WorldParticle.Type.MAGIC_CRIT, target);
        }
    }

    private void startMinecartRideTracking(MinecartEntity minecart) {
        minecartRideStartX = minecart.getX();
        minecartRideStartZ = minecart.getZ();
        trackingMinecartRide = true;
    }

    private void clearMinecartRideTracking() {
        trackingMinecartRide = false;
        minecartRideStartX = 0.0f;
        minecartRideStartZ = 0.0f;
    }

    private void recordMinecartRideProgress(MinecartEntity minecart) {
        if (!trackingMinecartRide || minecart == null) {
            return;
        }
        float dx = minecart.getX() - minecartRideStartX;
        float dz = minecart.getZ() - minecartRideStartZ;
        stats.getAchievements().recordMinecartRideDistance((float) Math.sqrt(dx * dx + dz * dz));
    }

    private void recordMonsterKillIfNeeded(LivingEntity target) {
        if (target instanceof Mob mob && mob.isHostile() && target.getHealth() <= 0.0f) {
            stats.getAchievements().recordMonsterKilled();
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
        updateHurtFlash(deltaTime);

        // Handle death state - only increment death time, skip all physics
        if (stats.isDead()) {
            // Drop items on first frame of death
            if (deathTime == 0) {
                dropAllItems();
            }
            clearHeldConsumableUse();
            deathTime++;
            velocity.set(0, 0, 0); // Stop all movement
            return;
        }

        if (sleeping) {
            clearHeldConsumableUse();
            isSwinging = false;
            isUsingItem = false;
            isDrawingBow = false;
            isBlockingItem = false;
            swingProgress = 0.0f;
            prevSwingProgress = 0.0f;
            useProgress = 0.0f;
            prevUseProgress = 0.0f;
            velocity.set(0.0f, 0.0f, 0.0f);
            sprinting = false;
            sneaking = false;
            flying = false;
            onGround = true;
            wasFalling = false;
            fallStartY = position.y;
            prevLimbSwingAmount = limbSwingAmount;
            limbSwingAmount = 0.0f;
            bodyYaw = sleepingRenderYaw;
            renderYawOffset = sleepingRenderYaw;
            updateSleepingCameraPosition();
            return;
        }

        updateSwing(deltaTime);
        updateUse(deltaTime, world);
        updateSlotSwitch(deltaTime);
        updateTurretRotation();

        // Check if held item type changed (from inventory manipulation)
        // This triggers animation when moving items in/out of selected slot
        com.craftzero.inventory.ItemStack currentHeld = inventory.getItemInHand();
        ItemType currentType = (currentHeld != null && !currentHeld.isEmpty())
                ? currentHeld.getType()
                : null;
        if (currentType == ItemType.MAP) {
            MapItemData.updateHeldMap(world, currentHeld, position.x, position.z, camera.getYaw());
        }
        if (currentType != lastHeldItemType && slotSwitchProgress >= 1.0f && !isUsingItem) {
            // Item type changed and not already animating - trigger animation
            if (lastHeldItemType != null || currentType != null) {
                triggerItemChangeAnimation();
            }
        }
        lastHeldItemType = currentType;

        if (isRidingVehicle()) {
            stats.getStatistics().recordPlayTime(deltaTime);
            syncRidingPosition(true);
            sprinting = false;
            flying = false;
            return;
        }

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

        // Check water state
        boolean wasInWater = wasInWaterForParticles;
        int blockX = (int) Math.floor(position.x);
        int blockZ = (int) Math.floor(position.z);
        inWater = world.getBlockIfLoaded(blockX, (int) Math.floor(position.y), blockZ, BlockType.AIR).isWater() ||
                world.getBlockIfLoaded(blockX, (int) Math.floor(position.y + 1), blockZ, BlockType.AIR).isWater();
        inLava = world.getBlockIfLoaded(blockX, (int) Math.floor(position.y), blockZ, BlockType.AIR).isLava() ||
                world.getBlockIfLoaded(blockX, (int) Math.floor(position.y + 1), blockZ, BlockType.AIR).isLava();

        headInWater = world.getBlockIfLoaded(blockX, (int) Math.floor(position.y + EYE_HEIGHT), blockZ,
                BlockType.AIR).isWater();
        boolean onClimbable = isTouchingClimbable(world);
        boolean inCobweb = isTouchingBlock(world, BlockType.COBWEB);
        boolean touchingSoulSand = isTouchingBlock(world, BlockType.SOUL_SAND);

        // Update state for visual rendering
        this.headInWaterState = headInWater;
        if (inWater && !wasInWater) {
            world.spawnEntityWaterEntryParticles(position.x, position.y, position.z, WIDTH,
                    velocity.x, velocity.y, velocity.z);
        }
        wasInWaterForParticles = inWater;

        // Update breath logic
        if (!isCreative()) {
            stats.updateAir(headInWater, deltaTime, respirationLevel(), random);
        }

        // Apply movement physics
        if (flying) {
            // Creative flight (already handled in handleInput)
            // No gravity, high friction handled by input velocity setting
        } else if (inWater) {
            if (isActionDown(GameSettings.KeyBinding.JUMP)) {
                velocity.y += WATER_SWIM_UP_ACCELERATION * deltaTime;
            }

            float dragFactor = perTickDrag(WATER_DRAG, deltaTime);
            velocity.x *= dragFactor;
            velocity.z *= dragFactor;
            velocity.y *= dragFactor;
            velocity.y += WATER_GRAVITY_ACCELERATION * deltaTime;
            applyWaterCurrent(world, deltaTime);

            fallStartY = position.y;
        } else if (inLava) {
            if (isActionDown(GameSettings.KeyBinding.JUMP)) {
                velocity.y += LAVA_SWIM_UP_ACCELERATION * deltaTime;
            }

            float dragFactor = perTickDrag(LAVA_DRAG, deltaTime);
            velocity.x *= dragFactor;
            velocity.z *= dragFactor;
            velocity.y *= dragFactor;
            velocity.y += LAVA_GRAVITY_ACCELERATION * deltaTime;
            applyFluidCurrent(world, deltaTime, false);

            fallStartY = position.y;
        } else if (onClimbable) {
            fallStartY = position.y;
            if (isActionDown(GameSettings.KeyBinding.SNEAK)) {
                velocity.y = Math.max(velocity.y, 0.0f);
            } else {
                velocity.y = Math.max(velocity.y, -CLIMBABLE_AXIS_SPEED);
            }
            velocity.x = clamp(velocity.x, -CLIMBABLE_AXIS_SPEED, CLIMBABLE_AXIS_SPEED);
            velocity.z = clamp(velocity.z, -CLIMBABLE_AXIS_SPEED, CLIMBABLE_AXIS_SPEED);
        } else {
            // Standard Air/Ground physics
            velocity.y += GRAVITY * deltaTime;

            // Apply friction
            float friction = onGround ? getGroundFriction(world) : AIR_FRICTION;
            velocity.x *= friction;
            velocity.z *= friction;
            applyNormalGroundStopBrake(world, friction);
        }

        if (inCobweb) {
            float horizontalDrag = perTickDrag(COBWEB_HORIZONTAL_DRAG, deltaTime);
            velocity.x *= horizontalDrag;
            velocity.z *= horizontalDrag;
            velocity.y *= perTickDrag(COBWEB_VERTICAL_DRAG, deltaTime);
            fallStartY = position.y;
        }
        if (touchingSoulSand) {
            float soulSandDrag = perTickDrag(SOUL_SAND_HORIZONTAL_DRAG, deltaTime);
            velocity.x *= soulSandDrag;
            velocity.z *= soulSandDrag;
        }

        // Clamp horizontal velocity
        float maxHorizontal = (sneaking ? SNEAK_SPEED : sprinting ? SPRINT_SPEED : WALK_SPEED)
                * stats.getMovementSpeedMultiplier();
        float horizontalSpeed = (float) Math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z);
        if (!onClimbable && horizontalSpeed > maxHorizontal) {
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
        boolean collidedHorizontally = moveWithCollision(deltaTime, world);
        world.schedulePressurePlateUpdatesForAabb(boundingBox, true);
        world.activateRedstoneOreBelow(boundingBox);
        world.applyPlayerHazardContact(this);
        updateFireState(deltaTime, world);
        if (inCobweb) {
            velocity.set(0.0f, 0.0f, 0.0f);
        } else if (onClimbable && collidedHorizontally) {
            velocity.y = CLIMBABLE_WALL_BUMP_SPEED;
        }

        // Fall landing effects: farmland trampling applies even when fall damage does not.
        if (onGround && !wasOnGround && !flying && !inWater) {
            float fallDistance = fallStartY - position.y;
            world.trampleFarmlandBelow(boundingBox, fallDistance);
            if (fallDistance > 0.0f) {
                stats.getStatistics().recordDistanceFallen(fallDistance);
            }
            if (!isCreative() && fallDistance > 3.0f) {
                int damage = (int) Math.ceil(fallDistance - 3.0f);
                world.playFallSound(position.x, position.y, position.z, fallDistance);
                hurt(damage, DamageSource.point(DamageSource.Type.FALL,
                        position.x, position.y, position.z, 0.0f, 0.0f));
            }
            fallStartY = position.y; // Reset fall start
        }

        float dx = position.x - prevPosition.x;
        float dy = position.y - prevPosition.y;
        float dz = position.z - prevPosition.z;
        float horizontalDistance = (float) Math.sqrt(dx * dx + dz * dz);

        // Update survival stats from the actual post-collision movement distance.
        boolean isMoving = horizontalDistance > 0.001f;
        stats.getStatistics().recordPlayTime(deltaTime);
        recordTravelStatistics(horizontalDistance, dy, inWater, headInWater, onClimbable, isMoving);
        if (!isCreative()) {
            stats.update(deltaTime, sprinting, isMoving, difficulty, horizontalDistance);
        }
        spawnStatusEffectParticle(world, deltaTime);

        // Track distance walked for animation
        distanceWalked += horizontalDistance;
        updateSprintBlockParticles(world);
        updateStepSound(world, horizontalDistance);

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

    private void recordTravelStatistics(float horizontalDistance, float verticalDistance, boolean inWater,
            boolean headInWater, boolean onClimbable, boolean isMoving) {
        float absVerticalDistance = Math.abs(verticalDistance);
        if (!isMoving && absVerticalDistance <= 0.001f) {
            return;
        }
        float fullDistance = (float) Math.sqrt(horizontalDistance * horizontalDistance
                + verticalDistance * verticalDistance);
        if (headInWater) {
            stats.getStatistics().recordDistanceDove(fullDistance);
        } else if (inWater) {
            stats.getStatistics().recordDistanceSwum(fullDistance);
        } else if (onClimbable) {
            if (verticalDistance > 0.001f) {
                stats.getStatistics().recordDistanceClimbed(verticalDistance);
            }
        } else if (flying) {
            stats.getStatistics().recordDistanceFlown(fullDistance);
        } else if (onGround) {
            stats.getStatistics().recordDistanceWalked(horizontalDistance);
        } else if (isMoving) {
            stats.getStatistics().recordDistanceFlown(horizontalDistance);
        }
    }

    private void updateFireState(float deltaTime, World world) {
        if (stats.hasEffect(StatusEffectType.FIRE_RESISTANCE)) {
            extinguish();
            return;
        }
        if (fireTicks <= 0) {
            fireTicks = 0;
            fireDamageCooldownTicks = 0;
            fireTickAccumulator = 0.0f;
            return;
        }
        if (inWater || headInWater || isWetFromWeather(world)) {
            extinguish();
            return;
        }

        fireTickAccumulator += Math.max(0.0f, deltaTime) * FIRE_TICKS_PER_SECOND;
        int ticksToProcess = (int) fireTickAccumulator;
        if (ticksToProcess <= 0) {
            return;
        }
        fireTickAccumulator -= ticksToProcess;

        for (int i = 0; i < ticksToProcess && fireTicks > 0 && !stats.isDead(); i++) {
            fireTicks--;
            if (fireDamageCooldownTicks > 0) {
                fireDamageCooldownTicks--;
            }
            if (fireDamageCooldownTicks <= 0) {
                fireDamageCooldownTicks = FIRE_DAMAGE_INTERVAL_TICKS;
                if (!stats.hasEffect(StatusEffectType.FIRE_RESISTANCE)) {
                    hurt(FIRE_DAMAGE, DamageSource.point(DamageSource.Type.FIRE,
                            position.x, position.y, position.z, 0.0f, 0.0f));
                }
            }
        }
    }

    private boolean isWetFromWeather(World world) {
        if (world == null) {
            return false;
        }
        int blockX = (int) Math.floor(position.x);
        int blockZ = (int) Math.floor(position.z);
        int feetY = (int) Math.floor(position.y + 0.1f);
        int headY = (int) Math.floor(position.y + HEIGHT * 0.85f);
        return world.isRainingAt(blockX, feetY, blockZ)
                || world.isRainingAt(blockX, headY, blockZ);
    }

    private void spawnStatusEffectParticle(World world, float deltaTime) {
        List<StatusEffectInstance> activeEffects = stats.getActiveEffects();
        if (world == null || activeEffects.isEmpty()) {
            statusEffectParticleTimer = 0.0f;
            return;
        }

        statusEffectParticleTimer += Math.max(0.0f, deltaTime);
        if (statusEffectParticleTimer < STATUS_EFFECT_PARTICLE_INTERVAL_SECONDS) {
            return;
        }
        statusEffectParticleTimer %= STATUS_EFFECT_PARTICLE_INTERVAL_SECONDS;

        float particleX = position.x + (random.nextFloat() - 0.5f) * WIDTH;
        float particleY = position.y + 0.2f + random.nextFloat() * (HEIGHT - 0.2f);
        float particleZ = position.z + (random.nextFloat() - 0.5f) * WIDTH;
        world.spawnParticle(WorldParticle.Type.MOB_SPELL,
                particleX, particleY, particleZ,
                0.0f, 0.02f, 0.0f,
                STATUS_EFFECT_PARTICLE_SCALE,
                STATUS_EFFECT_PARTICLE_LIFETIME_TICKS,
                StatusEffectVisuals.mixedColor(activeEffects));
    }

    private void updateStepSound(World world, float horizontalDistance) {
        if (world == null || horizontalDistance <= 0.0f || flying || inWater || inLava || !onGround) {
            return;
        }
        stepSoundDistance += horizontalDistance * STEP_SOUND_DISTANCE_SCALE;
        if (stepSoundDistance <= nextStepSoundDistance) {
            return;
        }
        nextStepSoundDistance = (int) stepSoundDistance + 1;
        int x = (int) Math.floor(position.x);
        int y = (int) Math.floor(boundingBox.getMin().y - 0.2f);
        int z = (int) Math.floor(position.z);
        BlockType steppedBlock = world.getBlockIfLoaded(x, y, z, BlockType.AIR);
        world.playBlockStepSound(steppedBlock, position.x, position.y, position.z);
    }

    private void updateSprintBlockParticles(World world) {
        if (world == null || !sprinting || flying || inWater) {
            return;
        }
        int x = (int) Math.floor(position.x);
        int y = (int) Math.floor(boundingBox.getMin().y - 0.2f);
        int z = (int) Math.floor(position.z);
        BlockType steppedBlock = world.getBlockIfLoaded(x, y, z, BlockType.AIR);
        world.spawnSprintBlockParticle(position.x, boundingBox.getMin().y, position.z, WIDTH,
                velocity.x, velocity.z, steppedBlock, world.getBlockMetadataIfLoaded(x, y, z, 0));
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
        if (sleeping) {
            updateSleepingCameraPosition();
            return;
        }

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

        if (sleeping) {
            camera.setPosition(interpX, interpY + SLEEPING_EYE_HEIGHT, interpZ);
            camera.setYaw(sleepingRenderYaw);
            camera.setPitch(0.0f);
            return;
        }

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

    private void updateSleepingCameraPosition() {
        camera.setPosition(position.x, position.y + SLEEPING_EYE_HEIGHT, position.z);
        camera.setYaw(sleepingRenderYaw);
        camera.setPitch(0.0f);
        orbitYaw = sleepingRenderYaw;
        orbitPitch = 0.0f;
    }

    /**
     * Push nearby mobs away from player.
     * Minecraft-style soft collision - player can push mobs but not stand on them.
     */
    private void pushNearbyMobs(World world, float deltaTime) {
        for (com.craftzero.entity.Entity entity : world.getEntities()) {
            if (entity instanceof com.craftzero.entity.LivingEntity mob) {
                if (mob.isRemoved() || mob.isDead()) {
                    continue;
                }
                AABB mobBox = mob.getBoundingBox();
                if (mobBox != null && boundingBox.intersects(mobBox)) {
                    float dx = mob.getX() - position.x;
                    float dz = mob.getZ() - position.z;
                    float maxAxis = Math.max(Math.abs(dx), Math.abs(dz));
                    if (maxAxis < LIVING_COLLISION_MIN_AXIS) {
                        dx = mob.getMotionX() - velocity.x;
                        dz = mob.getMotionZ() - velocity.z;
                        maxAxis = Math.max(Math.abs(dx), Math.abs(dz));
                        if (maxAxis < LIVING_COLLISION_MIN_AXIS) {
                            dx = 1.0f;
                            dz = 0.0f;
                            maxAxis = 1.0f;
                        }
                    }

                    float distance = (float) Math.sqrt(maxAxis);
                    float pushScale = Math.min(1.0f, 1.0f / distance) * LIVING_COLLISION_IMPULSE;
                    float pushX = (dx / distance) * pushScale;
                    float pushZ = (dz / distance) * pushScale;

                    mob.addMotion(pushX, 0.0f, pushZ);
                    velocity.x -= pushX;
                    velocity.z -= pushZ;
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
    private boolean moveWithCollision(float deltaTime, World world) {
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

        AABB startBox = copyBox(boundingBox);
        List<AABB> colliders = getCollidingBlocks(world, startBox, dx, dy, dz);
        MovementClip clipped = clipMovement(startBox, dx, dy, dz, colliders);
        boolean flatHorizontalCollision = horizontalCollision(originalDx, originalDz, clipped);
        boolean stepped = false;

        if (flatHorizontalCollision && onGround && !inWater && !inLava) {
            MovementClip steppedClip = clipStepMovement(world, startBox, dx, dy, dz);
            if (steppedClip != null
                    && steppedClip.horizontalDistanceSq() > clipped.horizontalDistanceSq() + COLLISION_EPSILON) {
                clipped = steppedClip;
                stepped = true;
            }
        }

        boundingBox = clipped.box();
        syncPositionToBoundingBox();

        boolean collidedHorizontally = horizontalCollision(originalDx, originalDz, clipped);
        if (Math.abs(originalDx - clipped.dx()) > COLLISION_EPSILON) {
            velocity.x = 0.0f;
        }
        if (Math.abs(originalDy - clipped.dy()) > COLLISION_EPSILON) {
            onGround = originalDy < 0 || stepped;
            velocity.y = 0.0f;
        } else {
            onGround = stepped;
        }
        if (Math.abs(originalDz - clipped.dz()) > COLLISION_EPSILON) {
            velocity.z = 0.0f;
        }
        return collidedHorizontally;
    }

    private MovementClip clipMovement(AABB startBox, float dx, float dy, float dz, List<AABB> colliders) {
        AABB box = copyBox(startBox);
        for (AABB collider : colliders) {
            dy = box.clipYCollide(collider, dy);
        }
        box.move(0, dy, 0);

        for (AABB collider : colliders) {
            dx = box.clipXCollide(collider, dx);
        }
        box.move(dx, 0, 0);

        for (AABB collider : colliders) {
            dz = box.clipZCollide(collider, dz);
        }
        box.move(0, 0, dz);
        return new MovementClip(box, dx, dy, dz);
    }

    private MovementClip clipStepMovement(World world, AABB startBox, float dx, float dy, float dz) {
        List<AABB> colliders = getCollidingBlocks(world, startBox, dx, dy + STEP_HEIGHT, dz);
        AABB box = copyBox(startBox);
        float stepUp = STEP_HEIGHT;
        for (AABB collider : colliders) {
            stepUp = box.clipYCollide(collider, stepUp);
        }
        if (stepUp <= COLLISION_EPSILON) {
            return null;
        }
        box.move(0, stepUp, 0);

        float steppedDx = dx;
        for (AABB collider : colliders) {
            steppedDx = box.clipXCollide(collider, steppedDx);
        }
        box.move(steppedDx, 0, 0);

        float steppedDz = dz;
        for (AABB collider : colliders) {
            steppedDz = box.clipZCollide(collider, steppedDz);
        }
        box.move(0, 0, steppedDz);

        float stepDown = dy - stepUp;
        for (AABB collider : colliders) {
            stepDown = box.clipYCollide(collider, stepDown);
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

    private void syncPositionToBoundingBox() {
        position.x = (boundingBox.getMin().x + boundingBox.getMax().x) * 0.5f;
        position.y = boundingBox.getMin().y;
        position.z = (boundingBox.getMin().z + boundingBox.getMax().z) * 0.5f;
    }

    /**
     * Get all solid blocks that could collide with the player's path.
     * Also includes mob bounding boxes for player-mob collision.
     */
    private List<AABB> getCollidingBlocks(World world, float dx, float dy, float dz) {
        return getCollidingBlocks(world, boundingBox, dx, dy, dz);
    }

    private List<AABB> getCollidingBlocks(World world, AABB sourceBox, float dx, float dy, float dz) {
        List<AABB> colliders = new ArrayList<>();

        // Expand bounding box by movement
        AABB expanded = sourceBox.expand(0.1f);
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
        colliders.addAll(world.getMovingPistonCollisionBoxes(searchBox));

        // Note: Mob collision is handled by pushNearbyMobs() instead of rigid collision
        // This prevents player from standing on mobs while still allowing push
        // interactions

        return colliders;
    }

    private record MovementClip(AABB box, float dx, float dy, float dz) {
        float horizontalDistanceSq() {
            return dx * dx + dz * dz;
        }
    }

    private boolean isTouchingClimbable(World world) {
        int minX = (int) Math.floor(boundingBox.getMin().x);
        int minY = (int) Math.floor(boundingBox.getMin().y);
        int minZ = (int) Math.floor(boundingBox.getMin().z);
        int maxX = (int) Math.floor(boundingBox.getMax().x - 0.0001f);
        int maxY = (int) Math.floor(boundingBox.getMax().y - 0.0001f);
        int maxZ = (int) Math.floor(boundingBox.getMax().z - 0.0001f);
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    BlockType type = world.getBlockIfLoaded(x, y, z, BlockType.AIR);
                    if (type == BlockType.LADDER || type == BlockType.VINES) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private boolean isTouchingBlock(World world, BlockType target) {
        int minX = (int) Math.floor(boundingBox.getMin().x);
        int minY = (int) Math.floor(boundingBox.getMin().y);
        int minZ = (int) Math.floor(boundingBox.getMin().z);
        int maxX = (int) Math.floor(boundingBox.getMax().x - 0.0001f);
        int maxY = (int) Math.floor(boundingBox.getMax().y - 0.0001f);
        int maxZ = (int) Math.floor(boundingBox.getMax().z - 0.0001f);
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    if (world.getBlockIfLoaded(x, y, z, BlockType.AIR) == target) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static float perTickDrag(float drag, float deltaTime) {
        return (float) Math.pow(drag, Math.max(0.0f, deltaTime * 20.0f));
    }

    private void applyWaterCurrent(World world, float deltaTime) {
        applyFluidCurrent(world, deltaTime, true);
    }

    private void applyFluidCurrent(World world, float deltaTime, boolean water) {
        Vector3f current = world.getFluidFlowVector(boundingBox, water);
        if (current.lengthSquared() <= 0.000001f) {
            return;
        }
        float acceleration = FLUID_CURRENT_ACCELERATION * Math.max(0.0f, deltaTime);
        velocity.x += current.x * acceleration;
        velocity.y += current.y * acceleration;
        velocity.z += current.z * acceleration;
    }

    private static float clamp(float value, float min, float max) {
        if (!Float.isFinite(value)) {
            return Float.isFinite(min) ? min : 0.0f;
        }
        float safeMin = finiteOrZero(min);
        float safeMax = finiteOrZero(max);
        if (safeMin > safeMax) {
            float swap = safeMin;
            safeMin = safeMax;
            safeMax = swap;
        }
        return Math.max(safeMin, Math.min(safeMax, value));
    }

    private static float finiteOrZero(float value) {
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

    private float getGroundFriction(World world) {
        int blockX = (int) Math.floor(position.x);
        int blockY = (int) Math.floor(position.y - 0.0001f);
        int blockZ = (int) Math.floor(position.z);
        float slipperiness = world.getBlockIfLoaded(blockX, blockY, blockZ, BlockType.AIR) == BlockType.ICE
                ? ICE_BLOCK_SLIPPERINESS
                : DEFAULT_BLOCK_SLIPPERINESS;
        return slipperiness * BASE_GROUND_FRICTION;
    }

    private float horizontalAccelerationPerSecond(float speed) {
        if (inWater) {
            return WATER_ACCELERATION_PER_SECOND * stats.getMovementSpeedMultiplier();
        }
        if (inLava) {
            return LAVA_ACCELERATION_PER_SECOND * stats.getMovementSpeedMultiplier();
        }
        if (flying || !onGround || world == null) {
            return speed * AIR_ACCELERATION_PER_SECOND;
        }
        float friction = Math.max(0.05f, getGroundFriction(world));
        return speed * (1.0f - friction) / friction * 20.0f;
    }

    private void applyNormalGroundStopBrake(World world, float friction) {
        if (world == null || movementInputActive || !onGround || friction >= SLIPPERY_GROUND_FRICTION_THRESHOLD) {
            return;
        }
        velocity.x *= NORMAL_GROUND_NO_INPUT_BRAKE;
        velocity.z *= NORMAL_GROUND_NO_INPUT_BRAKE;
        float horizontalSpeed = (float) Math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z);
        if (horizontalSpeed < NORMAL_GROUND_STOP_SPEED) {
            velocity.x = 0.0f;
            velocity.z = 0.0f;
        }
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

    public void setPlayerActionHandler(PlayerActionHandler playerActionHandler) {
        this.playerActionHandler = playerActionHandler;
    }

    public void setDeathDropHandler(DeathDropHandler deathDropHandler) {
        this.deathDropHandler = deathDropHandler;
    }

    public void updateExperiencePickupCooldown(float deltaTime) {
        if (experiencePickupCooldown > 0.0f) {
            experiencePickupCooldown = Math.max(0.0f, experiencePickupCooldown - deltaTime);
        }
    }

    public boolean canPickupExperience() {
        return experiencePickupCooldown <= 0.0f;
    }

    public void onExperiencePickedUp() {
        experiencePickupCooldown = 2.0f / 20.0f;
    }

    public Vector3f getPosition() {
        return position;
    }

    public float getEyeY() {
        if (sleeping) {
            return position.y + SLEEPING_EYE_HEIGHT;
        }
        return position.y + (sneaking ? EYE_HEIGHT - 0.125f : EYE_HEIGHT);
    }

    public AABB getBoundingBox() {
        return boundingBox;
    }

    public float getWidth() {
        return WIDTH;
    }

    public float getHeight() {
        return HEIGHT;
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
        if (sleeping) {
            return 0.0f;
        }
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
        if (!allFinite(x, y, z)) {
            return;
        }
        position.set(x, y, z);
        boundingBox = createBoundingBox();
        camera.setPosition(x, y + EYE_HEIGHT, z);
    }

    public void applyRemotePose(float x, float y, float z, float yaw, float pitch, boolean onGround) {
        if (!allFinite(x, y, z)) {
            return;
        }
        float safeYaw = normalizeYaw(yaw);
        float safePitch = clampPitch(pitch);
        float dx = x - position.x;
        float dz = z - position.z;
        prevPosition.set(position);
        prevDistanceWalked = distanceWalked;
        prevLimbSwingAmount = limbSwingAmount;
        prevRenderYawOffset = renderYawOffset;
        prevBodyYaw = bodyYaw;
        position.set(x, y, z);
        boundingBox = createBoundingBox();
        camera.setPosition(x, y + EYE_HEIGHT, z);
        camera.setYaw(safeYaw);
        camera.setPitch(safePitch);
        renderYawOffset = safeYaw;
        bodyYaw = safeYaw;
        this.onGround = onGround;
        float horizontalDistance = (float) Math.sqrt(dx * dx + dz * dz);
        distanceWalked += horizontalDistance;
        float movementAmount = movementInputActive ? (sprinting ? 0.95f : sneaking ? 0.35f : 0.65f) : 0.0f;
        float deltaAmount = Math.min(1.0f, horizontalDistance * 4.0f);
        limbSwingAmount = Math.max(deltaAmount, movementAmount);
    }

    public void teleportFromEnderPearl(float x, float y, float z) {
        if (!allFinite(x, y, z)) {
            return;
        }
        dismountVehicle();
        setPosition(x, y, z);
        velocity.set(0.0f, 0.0f, 0.0f);
        fallStartY = y;
        wasFalling = false;
        hurt(5.0f, DamageSource.point(DamageSource.Type.FALL, x, y, z, 0.0f, 0.0f));
    }

    public void placeAfterDimensionTransfer(float x, float y, float z) {
        if (!allFinite(x, y, z)) {
            return;
        }
        dismountCurrentVehicle();
        setPosition(x, y, z);
        velocity.set(0.0f, 0.0f, 0.0f);
        fallStartY = y;
        wasFalling = false;
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

    public String getPlayerName() {
        return playerName;
    }

    public void setPlayerName(String playerName) {
        String cleaned = playerName == null ? "" : playerName.trim();
        this.playerName = cleaned.isEmpty() ? GameSettings.DEFAULT_PLAYER_NAME : cleaned;
    }

    public void applySettings(GameSettings settings) {
        if (settings == null) {
            return;
        }
        this.settings = settings;
        setPlayerName(settings.getPlayerName());
        this.mouseSensitivityMultiplier = settings.mouseSensitivityMultiplier();
        this.invertMouse = settings.isInvertYMouse();
        this.viewBobbing = settings.isViewBobbing();
    }

    private boolean isActionDown(GameSettings.KeyBinding binding) {
        return GameInput.isBindingDown(settings, binding);
    }

    private boolean isActionPressed(GameSettings.KeyBinding binding) {
        return GameInput.isBindingPressed(settings, binding);
    }

    private boolean isActionReleased(GameSettings.KeyBinding binding) {
        return GameInput.isBindingReleased(settings, binding);
    }

    public void setMouseSensitivityMultiplier(float multiplier) {
        this.mouseSensitivityMultiplier = Math.max(0.1f, multiplier);
    }

    public void setInvertMouse(boolean invertMouse) {
        this.invertMouse = invertMouse;
    }

    public void toggleSmoothCamera() {
        setSmoothCamera(!smoothCamera);
    }

    public void setSmoothCamera(boolean smoothCamera) {
        this.smoothCamera = smoothCamera;
        if (!smoothCamera) {
            smoothMouseDeltaX = 0.0f;
            smoothMouseDeltaY = 0.0f;
        }
    }

    public boolean isSmoothCamera() {
        return smoothCamera;
    }

    public boolean isSprinting() {
        return sprinting;
    }

    public void setOnFire(int ticks) {
        if (stats.hasEffect(StatusEffectType.FIRE_RESISTANCE)) {
            extinguish();
            return;
        }
        if (ticks > fireTicks) {
            fireTicks = Math.max(0, ticks);
            fireDamageCooldownTicks = FIRE_DAMAGE_INTERVAL_TICKS;
            fireTickAccumulator = 0.0f;
        }
    }

    public void setFireTicks(int ticks) {
        fireTicks = Math.max(0, ticks);
        fireDamageCooldownTicks = fireTicks > 0 ? FIRE_DAMAGE_INTERVAL_TICKS : 0;
        fireTickAccumulator = 0.0f;
    }

    public void extinguish() {
        fireTicks = 0;
        fireDamageCooldownTicks = 0;
        fireTickAccumulator = 0.0f;
    }

    public boolean isOnFire() {
        return fireTicks > 0;
    }

    public int getFireTicks() {
        return fireTicks;
    }

    public float getHurtFlash() {
        return HURT_FLASH_DURATION_SECONDS <= 0.0f
                ? 0.0f
                : Math.max(0.0f, Math.min(1.0f, hurtFlashTimer / HURT_FLASH_DURATION_SECONDS));
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

    public void addVelocity(float motionX, float motionY, float motionZ) {
        if (!Float.isFinite(motionX) || !Float.isFinite(motionY) || !Float.isFinite(motionZ)) {
            return;
        }
        velocity.add(motionX, motionY, motionZ);
    }

    public float getFallStartY() {
        return fallStartY;
    }

    public boolean wasFalling() {
        return wasFalling;
    }

    public float getHurtFlashTimer() {
        return hurtFlashTimer;
    }

    public void restoreMovementState(float motionX, float motionY, float motionZ,
            boolean onGround, float fallStartY, boolean wasFalling) {
        if (!Float.isFinite(motionX)
                || !Float.isFinite(motionY)
                || !Float.isFinite(motionZ)
                || !Float.isFinite(fallStartY)) {
            return;
        }
        velocity.set(motionX, motionY, motionZ);
        this.onGround = onGround;
        this.fallStartY = fallStartY;
        this.wasFalling = wasFalling;
    }

    public void restoreDeathState(int deathTime, float hurtFlashTimer) {
        this.deathTime = Math.max(0, deathTime);
        this.hurtFlashTimer = Float.isFinite(hurtFlashTimer)
                ? Math.max(0.0f, Math.min(HURT_FLASH_DURATION_SECONDS, hurtFlashTimer))
                : 0.0f;
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
        if (!Float.isFinite(amount) || amount <= 0.0f) {
            return false;
        }
        if (source == null) {
            source = DamageSource.generic();
        }
        if (source.type() == DamageSource.Type.FIRE && stats.hasEffect(StatusEffectType.FIRE_RESISTANCE)) {
            return false;
        }
        float scaledAmount = source.scalesWithDifficulty() ? difficulty.scaleIncomingDamage(amount) : amount;
        float blockAdjustedAmount = applySwordBlocking(scaledAmount, source);
        float protectedAmount = applyArmorProtection(blockAdjustedAmount, source);
        float resistanceAdjustedAmount = StatusEffectMath.applyResistanceReduction(protectedAmount,
                stats.getEffectAmplifier(StatusEffectType.RESISTANCE));
        if (!Float.isFinite(resistanceAdjustedAmount) || resistanceAdjustedAmount <= 0.0f) {
            return false;
        }
        boolean applied = stats.damage(resistanceAdjustedAmount, source.usesHalfHurtResistanceWindow());
        if (!applied) {
            return false;
        }
        stats.onHurt();
        hurtFlashTimer = HURT_FLASH_DURATION_SECONDS;
        if (world != null) {
            world.playSound(WorldSoundEvent.PLAYER_HURT,
                    position.x, position.y + EYE_HEIGHT * 0.5f, position.z,
                    1.0f, 1.0f);
        }
        if (!source.bypassesArmor()) {
            damageArmor(blockAdjustedAmount);
        }

        float dx = position.x - source.sourceX();
        float dz = position.z - source.sourceZ();
        float dist = (float) Math.sqrt(dx * dx + dz * dz);
        if (source.hasPosition() && Float.isFinite(dist) && dist > 0.001f && source.horizontalKnockback() > 0.0f) {
            velocity.x += (dx / dist) * source.horizontalKnockback();
            velocity.z += (dz / dist) * source.horizontalKnockback();
        }
        if (source.verticalKnockback() > 0.0f) {
            velocity.y = Math.max(velocity.y, source.verticalKnockback());
        }
        if (source.entity() instanceof LivingEntity attacker) {
            notifyTamedWolvesOfCombatTarget(attacker);
        }
        return true;
    }

    private float applySwordBlocking(float damage, DamageSource source) {
        if (damage <= 0.0f || source == null || !source.canBeBlockedBySword()
                || !isBlockingItem || !isSwordStack(inventory.getItemInHand())) {
            return damage;
        }
        float blockedDamage = (float) Math.floor((damage + 1.0f) * 0.5f);
        return Math.min(damage, Math.max(1.0f, blockedDamage));
    }

    private void notifyTamedWolvesOfCombatTarget(LivingEntity target) {
        if (world == null || target == null || target.isDead() || target.isRemoved()) {
            return;
        }
        for (com.craftzero.entity.Entity entity : world.getEntities()) {
            if (entity instanceof Wolf wolf
                    && wolf.isOwnedBy(this)
                    && wolf.distanceToSquared(target) <= Wolf.ASSIST_RANGE * Wolf.ASSIST_RANGE) {
                wolf.setAssistTarget(target);
            }
        }
    }

    private float applyArmorProtection(float damage, DamageSource source) {
        if (source != null && source.bypassesArmor()) {
            return damage;
        }
        return ArmorCalculator.reduceDamage(damage, inventory.getArmor(), source);
    }

    private void damageArmor(float incomingDamage) {
        int durabilityLoss = armorDurabilityLoss(incomingDamage);
        if (durabilityLoss <= 0) {
            return;
        }
        com.craftzero.inventory.ItemStack[] armor = inventory.getArmor();
        for (int i = 0; i < armor.length; i++) {
            com.craftzero.inventory.ItemStack stack = armor[i];
            if (stack == null || !stack.isDamageable()) {
                continue;
            }
            for (int damage = 0; damage < durabilityLoss; damage++) {
                if (useDurabilityWithEnchantments(stack)) {
                    armor[i] = null;
                    break;
                }
            }
        }
    }

    private static int armorDurabilityLoss(float incomingDamage) {
        if (incomingDamage <= 0.0f) {
            return 0;
        }
        return Math.max(1, (int) (incomingDamage / 4.0f));
    }

    public boolean isSneaking() {
        return sneaking;
    }

    public boolean isInputForwardDown() {
        return inputForwardDown;
    }

    public boolean isInputBackwardDown() {
        return inputBackwardDown;
    }

    public boolean isInputLeftDown() {
        return inputLeftDown;
    }

    public boolean isInputRightDown() {
        return inputRightDown;
    }

    public boolean isInputJumpDown() {
        return inputJumpDown;
    }

    public void setRemoteSneaking(boolean sneaking) {
        this.sneaking = sneaking;
        if (sneaking) {
            this.sprinting = false;
        }
    }

    public void setRemoteSprinting(boolean sprinting) {
        this.sprinting = sprinting && !sneaking;
    }

    public void applyRemoteUseState(boolean usingItem, boolean blocking, boolean drawingBow, float remoteUseProgress) {
        float clampedProgress = clamp(remoteUseProgress, 0.0f, 1.0f);
        prevUseProgress = useProgress;
        isBlockingItem = blocking;
        isDrawingBow = drawingBow;
        if (drawingBow) {
            isUsingItem = true;
            bowDrawTime = clampedProgress * BOW_MAX_DRAW_TIME;
            useProgress = clampedProgress;
            return;
        }
        bowDrawTime = 0.0f;
        if (blocking) {
            isUsingItem = true;
            useProgress = Math.max(useProgress, clampedProgress);
            return;
        }
        if (usingItem) {
            isUsingItem = true;
            useProgress = Math.max(useProgress, clampedProgress);
            return;
        }
        if (!isUsingItem || useProgress <= 0.0f || useProgress >= 1.0f) {
            isUsingItem = false;
            useProgress = 0.0f;
            prevUseProgress = 0.0f;
        }
    }

    public void playRemoteSwingAnimation() {
        swingArm();
    }

    public void playRemoteUseAnimation() {
        if (isBlockingItem || isDrawingBow) {
            return;
        }
        startUseAnimation();
    }

    public void tickRemoteAnimations(float deltaTime) {
        float dt = Math.max(0.0f, Math.min(deltaTime, 0.25f));
        updateSwing(dt);
        prevUseProgress = useProgress;
        if (isDrawingBow) {
            bowDrawTime = Math.min(BOW_MAX_DRAW_TIME, bowDrawTime + dt);
            useProgress = Math.min(1.0f, bowDrawTime / BOW_MAX_DRAW_TIME);
            return;
        }
        if (isBlockingItem) {
            useProgress = Math.min(1.0f, useProgress + dt * 8.0f);
            return;
        }
        if (isUsingItem) {
            useProgress += dt * 5.0f;
            if (useProgress >= 1.0f) {
                useProgress = 0.0f;
                prevUseProgress = 0.0f;
                isUsingItem = false;
            }
            return;
        }
        useProgress = 0.0f;
        prevUseProgress = 0.0f;
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

    public int countAddableToInventory(ItemStack stack) {
        return inventory.countAddable(stack);
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
                com.craftzero.inventory.ItemStack dropped = ItemStackOps.copyWithCount(hotbar[slot], 1);
                stats.getStatistics().recordItemDropped(dropped.getType(), dropped.getCount());
                return dropped;
            }
            com.craftzero.inventory.ItemStack dropped = ItemStackOps.splitOne(hotbar[slot]);

            if (hotbar[slot].isEmpty()) {
                hotbar[slot] = null;
            }

            stats.getStatistics().recordItemDropped(dropped.getType(), dropped.getCount());
            return dropped;
        }

        return null;
    }

    /**
     * Check if player wants to open crafting table (right-clicked on one).
     */
    public boolean wantsCraftingTable() {
        return wantsCraftingTable || requestedCraftingTablePos != null;
    }

    public Vector3i getAndClearCraftingTableOpenRequest() {
        Vector3i value = requestedCraftingTablePos != null ? new Vector3i(requestedCraftingTablePos) : null;
        requestedCraftingTablePos = null;
        wantsCraftingTable = false;
        return value;
    }

    public Vector3i getAndClearChestOpenRequest() {
        Vector3i value = requestedChestPos != null ? new Vector3i(requestedChestPos) : null;
        requestedChestPos = null;
        return value;
    }

    public ChestMinecartEntity getAndClearChestMinecartOpenRequest() {
        ChestMinecartEntity value = requestedChestMinecart;
        requestedChestMinecart = null;
        return value;
    }

    public Vector3i getAndClearFurnaceOpenRequest() {
        Vector3i value = requestedFurnacePos != null ? new Vector3i(requestedFurnacePos) : null;
        requestedFurnacePos = null;
        return value;
    }

    public Vector3i getAndClearDispenserOpenRequest() {
        Vector3i value = requestedDispenserPos != null ? new Vector3i(requestedDispenserPos) : null;
        requestedDispenserPos = null;
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

    public void startSleepingInBed(BlockPos footPos, BlockPos headPos) {
        if (footPos == null || headPos == null) {
            return;
        }
        if (!sleeping) {
            sleepReturnX = position.x;
            sleepReturnY = position.y;
            sleepReturnZ = position.z;
            sleepReturnYaw = camera.getYaw();
            sleepReturnPitch = camera.getPitch();
        }
        sleeping = true;
        sleepingBedFootPos = footPos;
        sleepingBedHeadPos = headPos;
        sleepingBedFacing = bedFacingFromParts(footPos, headPos);
        sleepingRenderYaw = yawFromHorizontalFacing(sleepingBedFacing);
        float centerX = (footPos.x() + headPos.x() + 1.0f) * 0.5f;
        float centerZ = (footPos.z() + headPos.z() + 1.0f) * 0.5f;
        setPosition(centerX, footPos.y() + SLEEPING_BED_HEIGHT, centerZ);
        prevPosition.set(position);
        velocity.set(0.0f, 0.0f, 0.0f);
        sprinting = false;
        sneaking = false;
        flying = false;
        resetBreakingProgress();
        clearHeldConsumableUse();
        isSwinging = false;
        isMiningSwing = false;
        isUsingItem = false;
        isDrawingBow = false;
        isBlockingItem = false;
        swingProgress = 0.0f;
        prevSwingProgress = 0.0f;
        useProgress = 0.0f;
        prevUseProgress = 0.0f;
        bowDrawTime = 0.0f;
        requestedChestPos = null;
        requestedChestMinecart = null;
        requestedFurnacePos = null;
        requestedDispenserPos = null;
        requestedBrewingStandPos = null;
        requestedEnchantingTablePos = null;
        requestedSignEditPos = null;
        requestedBedUsePos = null;
        boundingBox = createBoundingBox();
        updateSleepingCameraPosition();
    }

    public void wakeFromBed(BlockPos wakePos) {
        if (!sleeping) {
            return;
        }
        float x = wakePos == null ? sleepReturnX : wakePos.x() + 0.5f;
        float y = wakePos == null ? sleepReturnY : wakePos.y();
        float z = wakePos == null ? sleepReturnZ : wakePos.z() + 0.5f;
        clearSleepingState();
        setPosition(x, y, z);
        prevPosition.set(position);
        velocity.set(0.0f, 0.0f, 0.0f);
        fallStartY = position.y;
        wasFalling = false;
        onGround = false;
        boundingBox = createBoundingBox();
        camera.setYaw(sleepReturnYaw);
        camera.setPitch(sleepReturnPitch);
        camera.setPosition(position.x, position.y + EYE_HEIGHT, position.z);
    }

    public boolean isSleeping() {
        return sleeping;
    }

    public BlockPos getSleepingBedFootPos() {
        return sleepingBedFootPos;
    }

    public BlockPos getSleepingBedHeadPos() {
        return sleepingBedHeadPos;
    }

    public int getSleepingBedFacing() {
        return sleepingBedFacing;
    }

    public float getSleepingRenderYaw() {
        return sleepingRenderYaw;
    }

    public float getSleepReturnX() {
        return sleepReturnX;
    }

    public float getSleepReturnY() {
        return sleepReturnY;
    }

    public float getSleepReturnZ() {
        return sleepReturnZ;
    }

    public float getSleepReturnYaw() {
        return sleepReturnYaw;
    }

    public float getSleepReturnPitch() {
        return sleepReturnPitch;
    }

    public void restoreSleepingState(BlockPos footPos, BlockPos headPos,
            float returnX, float returnY, float returnZ, float returnYaw, float returnPitch) {
        if (footPos == null || headPos == null
                || !Float.isFinite(returnX)
                || !Float.isFinite(returnY)
                || !Float.isFinite(returnZ)
                || !Float.isFinite(returnYaw)
                || !Float.isFinite(returnPitch)) {
            return;
        }
        sleepReturnX = returnX;
        sleepReturnY = returnY;
        sleepReturnZ = returnZ;
        sleepReturnYaw = returnYaw;
        sleepReturnPitch = returnPitch;
        sleeping = true;
        sleepingBedFootPos = footPos;
        sleepingBedHeadPos = headPos;
        sleepingBedFacing = bedFacingFromParts(footPos, headPos);
        sleepingRenderYaw = yawFromHorizontalFacing(sleepingBedFacing);
        float centerX = (footPos.x() + headPos.x() + 1.0f) * 0.5f;
        float centerZ = (footPos.z() + headPos.z() + 1.0f) * 0.5f;
        setPosition(centerX, footPos.y() + SLEEPING_BED_HEIGHT, centerZ);
        prevPosition.set(position);
        velocity.set(0.0f, 0.0f, 0.0f);
        onGround = true;
        wasFalling = false;
        fallStartY = position.y;
        sprinting = false;
        sneaking = false;
        flying = false;
        boundingBox = createBoundingBox();
        updateSleepingCameraPosition();
    }

    public void setSpawnPosition(float x, float y, float z) {
        if (setSpawnPositionRaw(x, y, z)) {
            clearBedSpawn();
        }
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

    public void setBedSpawnPosition(BlockPos bedFoot, float x, float y, float z) {
        if (!setSpawnPositionRaw(x, y, z)) {
            clearBedSpawn();
            return;
        }
        if (bedFoot == null) {
            clearBedSpawn();
            return;
        }
        bedSpawnSet = true;
        bedSpawnX = bedFoot.x();
        bedSpawnY = bedFoot.y();
        bedSpawnZ = bedFoot.z();
    }

    private boolean setSpawnPositionRaw(float x, float y, float z) {
        if (!allFinite(x, y, z)) {
            return false;
        }
        this.spawnX = x;
        this.spawnY = y;
        this.spawnZ = z;
        return true;
    }

    public void clearBedSpawn() {
        bedSpawnSet = false;
        bedSpawnX = 0;
        bedSpawnY = 0;
        bedSpawnZ = 0;
    }

    public boolean hasBedSpawn() {
        return bedSpawnSet;
    }

    public BlockPos getBedSpawnPos() {
        return bedSpawnSet ? new BlockPos(bedSpawnX, bedSpawnY, bedSpawnZ) : null;
    }

    private void clearSleepingState() {
        sleeping = false;
        sleepingBedFootPos = null;
        sleepingBedHeadPos = null;
        sleepingBedFacing = 0;
        sleepingRenderYaw = 0.0f;
    }

    private static int bedFacingFromParts(BlockPos footPos, BlockPos headPos) {
        int dx = headPos.x() - footPos.x();
        int dz = headPos.z() - footPos.z();
        if (dx > 0) {
            return 1;
        }
        if (dz > 0) {
            return 2;
        }
        if (dx < 0) {
            return 3;
        }
        return 0;
    }

    private static float yawFromHorizontalFacing(int facing) {
        return switch (facing & 3) {
            case 1 -> 90.0f;
            case 2 -> 180.0f;
            case 3 -> 270.0f;
            default -> 0.0f;
        };
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

    int getPistonPlacementMetadata(Vector3i placePos) {
        if (placePos != null
                && Math.abs(position.x - placePos.x) < 2.0f
                && Math.abs(position.z - placePos.z) < 2.0f) {
            double verticalReference = position.y + 0.2d;
            if (verticalReference - placePos.y > 2.0d) {
                return Block.FACE_TOP;
            }
            if (placePos.y - verticalReference > 0.0d) {
                return Block.FACE_BOTTOM;
            }
        }
        return getPlacementFacingMetadata();
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

    private static boolean isHorizontalFace(int face) {
        return face == Block.FACE_NORTH || face == Block.FACE_SOUTH
                || face == Block.FACE_EAST || face == Block.FACE_WEST;
    }

    private int getSignRotationMetadata() {
        float yaw = camera.getYaw() % 360.0f;
        if (yaw < 0) {
            yaw += 360.0f;
        }
        return Math.round(yaw / 22.5f) & 15;
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
        if (isSwinging) {
            swingProgress += deltaTime * SWING_ANIMATION_SPEED;
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

    private void updateHurtFlash(float deltaTime) {
        if (hurtFlashTimer > 0.0f) {
            hurtFlashTimer = Math.max(0.0f, hurtFlashTimer - Math.max(0.0f, deltaTime));
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
        if (isSwinging && swingProgress < 0.5f) {
            return;
        }

        isSwinging = true;
        swingProgress = 0;
        prevSwingProgress = 0;
        isMiningSwing = targetBlock != null && targetBlock.hit;
    }

    public float getSwingProgress(float partialTick) {
        return prevSwingProgress + (swingProgress - prevSwingProgress) * partialTick;
    }

    public float getRenderYawOffset(float partialTick) {
        return prevRenderYawOffset + (renderYawOffset - prevRenderYawOffset) * partialTick;
    }

    private void updateUse(float deltaTime, World world) {
        if (isDrawingBow) {
            useProgress = Math.min(1.0f, bowDrawTime / BOW_MAX_DRAW_TIME);
            return;
        }

        if (isBlockingItem) {
            useProgress = Math.min(1.0f, useProgress + deltaTime * 8.0f);
            return;
        }

        if (isConsumingItem) {
            updateHeldConsumableUse(deltaTime, world);
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

    private void updateHeldConsumableUse(float deltaTime, World world) {
        if (!consumableUseHeldThisFrame || !heldConsumableMatches()) {
            cancelHeldConsumableUse();
            return;
        }

        consumableUseTime = Math.min(CONSUMABLE_USE_TIME, consumableUseTime + Math.max(0.0f, deltaTime));
        useProgress = Math.min(1.0f, consumableUseTime / CONSUMABLE_USE_TIME);
        consumableTickSoundTimer += Math.max(0.0f, deltaTime);
        while (consumableTickSoundTimer >= CONSUMABLE_TICK_SOUND_INTERVAL
                && consumableUseTime < CONSUMABLE_USE_TIME) {
            playHeldConsumableTickSound(world);
            consumableTickSoundTimer -= CONSUMABLE_TICK_SOUND_INTERVAL;
        }

        if (consumableUseTime >= CONSUMABLE_USE_TIME) {
            completeHeldConsumableUse(world);
            return;
        }
        consumableUseHeldThisFrame = false;
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

    public boolean isBlockingItem() {
        return isBlockingItem;
    }

    public boolean isDrawingBow() {
        return isDrawingBow;
    }

    public boolean isEatingOrDrinkingItem() {
        com.craftzero.inventory.ItemStack held = inventory.getItemInHand();
        if (held == null || held.isEmpty()) {
            return false;
        }
        ItemType type = held.getType();
        return foodValue(type) != null || type == ItemType.MILK_BUCKET || type == ItemType.POTION;
    }

    private boolean isSwordStack(com.craftzero.inventory.ItemStack stack) {
        return stack != null
                && !stack.isEmpty()
                && isSwordItem(stack.getType());
    }

    private boolean shouldDamageHeldItemOnBlockBreak(com.craftzero.inventory.ItemStack heldItem,
            BlockType blockType, ItemType heldType) {
        return heldItem != null
                && !heldItem.isEmpty()
                && (heldItem.isTool() || (heldType == ItemType.SHEARS && isShearsHarvestBlock(blockType)));
    }

    private boolean isShearsHarvestBlock(BlockType blockType) {
        return blockType == BlockType.COBWEB
                || blockType == BlockType.LEAVES
                || blockType == BlockType.TALL_GRASS
                || blockType == BlockType.VINES;
    }

    private static boolean isCobwebHarvestTool(ItemType heldType) {
        return heldType == ItemType.SHEARS || isSwordItem(heldType);
    }

    private static boolean isSwordItem(ItemType type) {
        return type != null
                && type.isTool()
                && type.getToolType().getCategory() == ToolType.Category.SWORD;
    }

    public void prepareForWorldJoin() {
        clearSleepingState();
        velocity.set(0, 0, 0);
        prevPosition.set(position);
        fallStartY = position.y;
        wasFalling = false;
        onGround = false;
        breakingBlockPos = null;
        breakProgress = 0.0f;
        currentBreakingBlock = null;
        isSwinging = false;
        isUsingItem = false;
        isDrawingBow = false;
        isBlockingItem = false;
        isConsumingItem = false;
        consumingItemType = null;
        consumingSlot = -1;
        consumableUseTime = 0.0f;
        consumableTickSoundTimer = 0.0f;
        consumableUseHeldThisFrame = false;
        useProgress = 0.0f;
        prevUseProgress = 0.0f;
        bowDrawTime = 0.0f;
        stats.grantInvincibility(3.0f);
        boundingBox = createBoundingBox();
        camera.setPosition(position.x, position.y + EYE_HEIGHT, position.z);
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
        clearSleepingState();

        // Reset stats (health, hunger, etc.)
        stats.respawn();

        // Clear inventory just in case (though it should be empty from death drop)
        inventory.clearInventory();

        // Reset death animation
        deathTime = 0;
        hurtFlashTimer = 0.0f;

        // Reset position to spawn point
        setPosition(spawnX, spawnY, spawnZ);
        prevPosition.set(position);

        // Reset velocity
        velocity.set(0, 0, 0);

        // Reset physics state
        onGround = false;
        flying = false;
        sprinting = false;
        sneaking = false;
        fallStartY = position.y;

        // Update bounding box/camera after setPosition rejects malformed saved spawn data.
        boundingBox = createBoundingBox();
        camera.setPosition(position.x, position.y + EYE_HEIGHT, position.z);
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
            dropDeathExperience(dropX, dropY, dropZ, deathXp);
        }
        stats.getProgression().clearExperience();

        // Drop hotbar items
        com.craftzero.inventory.ItemStack[] hotbar = inventory.getHotbar();
        for (int i = 0; i < hotbar.length; i++) {
            if (hotbar[i] != null && !hotbar[i].isEmpty()) {
                // Random velocity for scatter effect
                float velX = (rand.nextFloat() - 0.5f) * 3.0f;
                float velY = rand.nextFloat() * 3.0f + 2.0f;
                float velZ = (rand.nextFloat() - 0.5f) * 3.0f;
                dropDeathStack(i, dropX, dropY, dropZ, hotbar[i].copy(), velX, velY, velZ);
            }
        }

        // Drop main inventory items
        com.craftzero.inventory.ItemStack[] main = inventory.getMainInventory();
        for (int i = 0; i < main.length; i++) {
            if (main[i] != null && !main[i].isEmpty()) {
                float velX = (rand.nextFloat() - 0.5f) * 3.0f;
                float velY = rand.nextFloat() * 3.0f + 2.0f;
                float velZ = (rand.nextFloat() - 0.5f) * 3.0f;
                dropDeathStack(Inventory.HOTBAR_SIZE + i, dropX, dropY, dropZ, main[i].copy(), velX, velY, velZ);
            }
        }

        // Drop equipped armor
        com.craftzero.inventory.ItemStack[] armor = inventory.getArmor();
        for (int i = 0; i < armor.length; i++) {
            if (armor[i] != null && !armor[i].isEmpty()) {
                float velX = (rand.nextFloat() - 0.5f) * 3.0f;
                float velY = rand.nextFloat() * 3.0f + 2.0f;
                float velZ = (rand.nextFloat() - 0.5f) * 3.0f;
                dropDeathStack(Inventory.HOTBAR_SIZE + Inventory.MAIN_SIZE + Inventory.CRAFTING_SIZE + i,
                        dropX, dropY, dropZ, armor[i].copy(), velX, velY, velZ);
            }
        }

        // Drop crafting grid items
        com.craftzero.inventory.ItemStack[] crafting = inventory.getCraftingGrid();
        for (int i = 0; i < crafting.length; i++) {
            if (crafting[i] != null && !crafting[i].isEmpty()) {
                float velX = (rand.nextFloat() - 0.5f) * 3.0f;
                float velY = rand.nextFloat() * 3.0f + 2.0f;
                float velZ = (rand.nextFloat() - 0.5f) * 3.0f;
                dropDeathStack(Inventory.HOTBAR_SIZE + Inventory.MAIN_SIZE + i,
                        dropX, dropY, dropZ, crafting[i].copy(), velX, velY, velZ);
            }
        }

        // Drop any item held in cursor
        com.craftzero.inventory.ItemStack cursor = inventory.getCursorItem();
        if (cursor != null && !cursor.isEmpty()) {
            float velX = (rand.nextFloat() - 0.5f) * 3.0f;
            float velY = rand.nextFloat() * 3.0f + 2.0f;
            float velZ = (rand.nextFloat() - 0.5f) * 3.0f;
            dropDeathStack(Inventory.HOTBAR_SIZE + Inventory.MAIN_SIZE + Inventory.CRAFTING_SIZE
                    + inventory.getArmor().length, dropX, dropY, dropZ, cursor.copy(), velX, velY, velZ);
            inventory.setCursorItem(null);
        }

        // Clear inventory after dropping
        inventory.clearInventory();
    }

    private void dropDeathExperience(float x, float y, float z, int amount) {
        if (deathDropHandler != null) {
            deathDropHandler.dropExperience(x, y, z, amount);
        } else {
            world.spawnExperience(x, y, z, amount);
        }
    }

    private void dropDeathStack(int sourceSlot, float x, float y, float z, ItemStack stack,
            float velocityX, float velocityY, float velocityZ) {
        if (deathDropHandler != null) {
            deathDropHandler.dropStack(sourceSlot, x, y, z, stack, velocityX, velocityY, velocityZ,
                    DroppedItem.THROWN_PICKUP_DELAY_TICKS);
        } else {
            world.spawnThrownStack(x, y, z, stack, velocityX, velocityY, velocityZ,
                    DroppedItem.THROWN_PICKUP_DELAY_TICKS);
        }
    }
}
