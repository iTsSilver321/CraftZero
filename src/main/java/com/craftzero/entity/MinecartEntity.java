package com.craftzero.entity;

import com.craftzero.inventory.ItemStack;
import com.craftzero.inventory.ItemType;
import com.craftzero.world.BlockShape;
import com.craftzero.world.BlockType;
import com.craftzero.world.RailShapeResolver;
import com.craftzero.world.RedstoneEngine;

public class MinecartEntity extends Entity {
    public enum CartKind {
        RIDEABLE,
        CHEST,
        FURNACE
    }

    private static final float MAX_RAIL_AXIS_SPEED = 0.40f;
    private static final float MAX_PROJECTED_RAIL_SPEED = 2.0f;
    private static final float OFF_RAIL_GRAVITY = 0.04f;
    private static final float OFF_RAIL_GROUND_DAMPING = 0.5f;
    private static final float OFF_RAIL_AIR_DRAG = 0.95f;
    private static final float ASCENDING_RAIL_ACCELERATION = 0.0078125f;
    private static final float UNPOWERED_POWERED_RAIL_STOP_SPEED = 0.03f;
    private static final float EMPTY_RAIL_DRAG = 0.96f;
    private static final float OCCUPIED_RAIL_DRAG = 0.997f;
    private static final float COLLISION_EPSILON = 0.0001f;
    private static final float COLLISION_IMPULSE = 0.05f;
    private static final float NON_CART_COLLISION_MIN_DISTANCE_SQ = 0.0001f;
    private static final float NON_CART_COLLISION_CART_IMPULSE = 0.1f;
    private static final float NON_CART_COLLISION_ENTITY_IMPULSE_MULTIPLIER = 0.25f;
    private static final float LIVING_MOUNT_MIN_SPEED_SQ = 0.01f;
    private static final float POST_COLLISION_DAMPING = 0.2f;
    private static final float FURNACE_COLLISION_DAMPING = 0.95f;
    private static final float YAW_UPDATE_MIN_MOVE_SQ = 0.001f;
    private static final float PASSENGER_Y_OFFSET = 0.1f;
    public static final int HIT_ROLLING_TICKS = 10;
    public static final float DAMAGE_PER_ATTACK_POINT = 10.0f;
    public static final float BREAK_DAMAGE = 40.0f;

    private final CartKind kind;

    public record NonCartCollisionPush(float cartX, float cartZ, float entityX, float entityZ) {
    }

    private int rollingAmplitude;
    private int rollingDirection = 1;
    private float damage;
    private boolean playerPassenger;
    private LivingEntity livingPassenger;
    private boolean riderInputStartedCart;

    public MinecartEntity(CartKind kind) {
        super(0.98f, 0.70f);
        this.kind = kind == null ? CartKind.RIDEABLE : kind;
    }

    public MinecartEntity(float x, float y, float z, CartKind kind) {
        this(kind);
        setPosition(x, y, z);
    }

    @Override
    public void tick() {
        super.tick();
        if (rollingAmplitude > 0) {
            rollingAmplitude--;
        }
        if (damage > 0.0f) {
            damage = Math.max(0.0f, damage - 1.0f);
        }
    }

    @Override
    public void updatePhysics(float deltaTime) {
        boolean skipUnpoweredPoweredRailBrake = riderInputStartedCart;
        riderInputStartedCart = false;
        if (world == null || removed) {
            return;
        }
        int railX = (int) Math.floor(x);
        int railZ = (int) Math.floor(z);
        int railY = RailShapeResolver.findMinecartRailY(world, railX, (int) Math.floor(y), railZ);
        if (railY == Integer.MIN_VALUE) {
            updateOffRailPhysics();
            return;
        }

        BlockType rail = world.getBlockIfLoaded(railX, railY, railZ, BlockType.AIR);
        int metadata = world.getBlockMetadataIfLoaded(railX, railY, railZ, 0);
        int shape = metadata & 7;
        if (rail == BlockType.RAIL && metadata >= RailShapeResolver.CURVE_SOUTH_EAST) {
            shape = metadata;
        }
        if (rail == BlockType.DETECTOR_RAIL) {
            RedstoneEngine.updateRailDetectorForCart(world, this);
        }

        float startRailPathY = sampleRailPathY(x, y, z);
        y = railY + 0.0625f;
        motionY = 0.0f;
        applyRailShape(shape);
        clampHorizontalSpeed();

        boolean poweredRailActive = rail == BlockType.POWERED_RAIL
                && (metadata & RedstoneEngine.RAIL_POWERED_BIT) != 0;
        if (rail == BlockType.POWERED_RAIL && !poweredRailActive) {
            brakeUnpoweredPoweredRail(skipUnpoweredPoweredRailBrake);
        }

        applySpecialCartForces();
        projectPositionToRailPath(railX, railZ, shape);

        float moveX = railMovementDelta(motionX);
        float moveZ = railMovementDelta(motionZ);
        x += moveX;
        z += moveZ;
        if (moveX * moveX + moveZ * moveZ > YAW_UPDATE_MIN_MOVE_SQ) {
            yaw = (float) Math.toDegrees(Math.atan2(moveX, -moveZ));
        }

        float endRailPathY = sampleRailPathY(x, y, z);
        setYToRailPath(x, y, z);
        applyRailFriction();
        applyRailHeightSpeedAdjustment(startRailPathY, endRailPathY);
        realignMotionAfterCellCrossing(railX, railZ);
        if (poweredRailActive) {
            acceleratePoweredRail(shape);
        }
        clampHorizontalSpeed();
        RedstoneEngine.updateRailDetectorForCart(world, this);
        syncPassengerPosition();
    }

    private float railDragMultiplier() {
        return hasAnyPassenger() ? OCCUPIED_RAIL_DRAG : EMPTY_RAIL_DRAG;
    }

    private void updateOffRailPhysics() {
        motionY -= OFF_RAIL_GRAVITY;
        motionX = clamp(motionX, -MAX_RAIL_AXIS_SPEED, MAX_RAIL_AXIS_SPEED);
        motionZ = clamp(motionZ, -MAX_RAIL_AXIS_SPEED, MAX_RAIL_AXIS_SPEED);
        if (onGround) {
            motionX *= OFF_RAIL_GROUND_DAMPING;
            motionY *= OFF_RAIL_GROUND_DAMPING;
            motionZ *= OFF_RAIL_GROUND_DAMPING;
        }
        moveWithCollision(motionX, motionY, motionZ);
        if (!onGround) {
            motionX *= OFF_RAIL_AIR_DRAG;
            motionY *= OFF_RAIL_AIR_DRAG;
            motionZ *= OFF_RAIL_AIR_DRAG;
        }
        syncPassengerPosition();
    }

    private float railMovementDelta(float motion) {
        float displacement = hasAnyPassenger() ? motion * 0.75f : motion;
        return clamp(displacement, -MAX_RAIL_AXIS_SPEED, MAX_RAIL_AXIS_SPEED);
    }

    private float sampleRailPathY(float sampleX, float sampleY, float sampleZ) {
        int railX = (int) Math.floor(sampleX);
        int railZ = (int) Math.floor(sampleZ);
        int railY = RailShapeResolver.findMinecartRailY(world, railX, (int) Math.floor(sampleY), railZ);
        if (railY == Integer.MIN_VALUE) {
            return Float.NaN;
        }
        BlockType rail = world.getBlockIfLoaded(railX, railY, railZ, BlockType.AIR);
        int shape = railShapeAt(rail, world.getBlockMetadataIfLoaded(railX, railY, railZ, 0));
        float localX = sampleX - (float) Math.floor(sampleX);
        float localZ = sampleZ - (float) Math.floor(sampleZ);
        return switch (shape) {
            case RailShapeResolver.ASCENDING_EAST -> railY + 0.5f + localX;
            case RailShapeResolver.ASCENDING_WEST -> railY + 1.5f - localX;
            case RailShapeResolver.ASCENDING_NORTH -> railY + 1.5f - localZ;
            case RailShapeResolver.ASCENDING_SOUTH -> railY + 0.5f + localZ;
            default -> railY + 0.5f;
        };
    }

    private void setYToRailPath(float sampleX, float sampleY, float sampleZ) {
        float railEntityY = sampleRailEntityY(sampleX, sampleY, sampleZ);
        if (!Float.isNaN(railEntityY)) {
            y = railEntityY;
        }
    }

    private float sampleRailEntityY(float sampleX, float sampleY, float sampleZ) {
        int railX = (int) Math.floor(sampleX);
        int railZ = (int) Math.floor(sampleZ);
        int railY = RailShapeResolver.findMinecartRailY(world, railX, (int) Math.floor(sampleY), railZ);
        if (railY == Integer.MIN_VALUE) {
            return Float.NaN;
        }
        BlockType rail = world.getBlockIfLoaded(railX, railY, railZ, BlockType.AIR);
        int shape = railShapeAt(rail, world.getBlockMetadataIfLoaded(railX, railY, railZ, 0));
        float localX = sampleX - (float) Math.floor(sampleX);
        float localZ = sampleZ - (float) Math.floor(sampleZ);
        return switch (shape) {
            case RailShapeResolver.ASCENDING_EAST -> railY + 0.0625f + localX * 0.5f;
            case RailShapeResolver.ASCENDING_WEST -> railY + 0.0625f + (1.0f - localX) * 0.5f;
            case RailShapeResolver.ASCENDING_NORTH -> railY + 0.0625f + (1.0f - localZ) * 0.5f;
            case RailShapeResolver.ASCENDING_SOUTH -> railY + 0.0625f + localZ * 0.5f;
            default -> railY + 0.0625f;
        };
    }

    private int railShapeAt(BlockType rail, int metadata) {
        return RailShapeResolver.shapeFromMetadata(rail, metadata);
    }

    private void applyRailShape(int shape) {
        switch (shape) {
            case RailShapeResolver.ASCENDING_EAST -> {
                motionX -= ASCENDING_RAIL_ACCELERATION;
                y += (float) ((x - Math.floor(x)) * 0.5f);
            }
            case RailShapeResolver.ASCENDING_WEST -> {
                motionX += ASCENDING_RAIL_ACCELERATION;
                y += (float) ((1.0f - (x - Math.floor(x))) * 0.5f);
            }
            case RailShapeResolver.ASCENDING_NORTH -> {
                motionZ += ASCENDING_RAIL_ACCELERATION;
                y += (float) ((1.0f - (z - Math.floor(z))) * 0.5f);
            }
            case RailShapeResolver.ASCENDING_SOUTH -> {
                motionZ -= ASCENDING_RAIL_ACCELERATION;
                y += (float) ((z - Math.floor(z)) * 0.5f);
            }
            default -> {
            }
        }
        projectMotionToRail(shape);
    }

    private void projectPositionToRailPath(int railX, int railZ, int shape) {
        float startX = railX + 0.5f + railStartOffsetX(shape) * 0.5f;
        float startZ = railZ + 0.5f + railStartOffsetZ(shape) * 0.5f;
        float endX = railX + 0.5f + railEndOffsetX(shape) * 0.5f;
        float endZ = railZ + 0.5f + railEndOffsetZ(shape) * 0.5f;
        float dx = endX - startX;
        float dz = endZ - startZ;
        float t;
        if (dx == 0.0f) {
            x = railX + 0.5f;
            t = z - railZ;
        } else if (dz == 0.0f) {
            z = railZ + 0.5f;
            t = x - railX;
        } else {
            t = ((x - startX) * dx + (z - startZ) * dz) * 2.0f;
        }
        x = startX + dx * t;
        z = startZ + dz * t;
    }

    private int railStartOffsetX(int shape) {
        return switch (shape) {
            case RailShapeResolver.EAST_WEST,
                    RailShapeResolver.ASCENDING_EAST,
                    RailShapeResolver.ASCENDING_WEST -> -1;
            default -> 0;
        };
    }

    private int railStartOffsetZ(int shape) {
        return switch (shape) {
            case RailShapeResolver.NORTH_SOUTH,
                    RailShapeResolver.ASCENDING_NORTH,
                    RailShapeResolver.ASCENDING_SOUTH,
                    RailShapeResolver.CURVE_NORTH_WEST,
                    RailShapeResolver.CURVE_NORTH_EAST -> -1;
            case RailShapeResolver.CURVE_SOUTH_EAST,
                    RailShapeResolver.CURVE_SOUTH_WEST -> 1;
            default -> 0;
        };
    }

    private int railEndOffsetX(int shape) {
        return switch (shape) {
            case RailShapeResolver.EAST_WEST,
                    RailShapeResolver.ASCENDING_EAST,
                    RailShapeResolver.ASCENDING_WEST,
                    RailShapeResolver.CURVE_SOUTH_EAST,
                    RailShapeResolver.CURVE_NORTH_EAST -> 1;
            case RailShapeResolver.CURVE_SOUTH_WEST,
                    RailShapeResolver.CURVE_NORTH_WEST -> -1;
            default -> 0;
        };
    }

    private int railEndOffsetZ(int shape) {
        return switch (shape) {
            case RailShapeResolver.NORTH_SOUTH,
                    RailShapeResolver.ASCENDING_NORTH,
                    RailShapeResolver.ASCENDING_SOUTH -> 1;
            default -> 0;
        };
    }

    private void projectMotionToRail(int shape) {
        int dx = railVectorX(shape);
        int dz = railVectorZ(shape);
        float length = (float) Math.sqrt(dx * dx + dz * dz);
        if (length <= 0.0f) {
            return;
        }
        float dot = motionX * dx + motionZ * dz;
        if (dot < 0.0f) {
            dx = -dx;
            dz = -dz;
        }
        float speed = (float) Math.sqrt(motionX * motionX + motionZ * motionZ);
        if (speed > MAX_PROJECTED_RAIL_SPEED) {
            speed = MAX_PROJECTED_RAIL_SPEED;
        }
        motionX = speed * dx / length;
        motionZ = speed * dz / length;
    }

    private int railVectorX(int shape) {
        return switch (shape) {
            case RailShapeResolver.NORTH_SOUTH,
                    RailShapeResolver.ASCENDING_NORTH,
                    RailShapeResolver.ASCENDING_SOUTH -> 0;
            case RailShapeResolver.CURVE_SOUTH_WEST,
                    RailShapeResolver.CURVE_NORTH_WEST -> -1;
            case RailShapeResolver.CURVE_SOUTH_EAST,
                    RailShapeResolver.CURVE_NORTH_EAST -> 1;
            default -> 2;
        };
    }

    private int railVectorZ(int shape) {
        return switch (shape) {
            case RailShapeResolver.EAST_WEST,
                    RailShapeResolver.ASCENDING_EAST,
                    RailShapeResolver.ASCENDING_WEST -> 0;
            case RailShapeResolver.CURVE_SOUTH_EAST,
                    RailShapeResolver.CURVE_SOUTH_WEST -> -1;
            case RailShapeResolver.CURVE_NORTH_WEST,
                    RailShapeResolver.CURVE_NORTH_EAST -> 1;
            default -> 2;
        };
    }

    private void acceleratePoweredRail(int shape) {
        float speed = (float) Math.sqrt(motionX * motionX + motionZ * motionZ);
        if (speed < 0.01f) {
            launchFromAdjacentBlock(shape);
            return;
        }
        motionX += motionX / speed * 0.06f;
        motionZ += motionZ / speed * 0.06f;
    }

    private void brakeUnpoweredPoweredRail(boolean skipBrake) {
        if (skipBrake) {
            return;
        }
        float speed = (float) Math.sqrt(motionX * motionX + motionZ * motionZ);
        if (speed < UNPOWERED_POWERED_RAIL_STOP_SPEED) {
            motionX = 0.0f;
            motionZ = 0.0f;
        } else {
            motionX *= 0.5f;
            motionZ *= 0.5f;
        }
    }

    private void launchFromAdjacentBlock(int shape) {
        int railX = (int) Math.floor(x);
        int railY = (int) Math.floor(y);
        int railZ = (int) Math.floor(z);
        if (RailShapeResolver.isEastWest(shape)) {
            boolean westBlocked = isOpaqueBlock(railX - 1, railY, railZ);
            boolean eastBlocked = isOpaqueBlock(railX + 1, railY, railZ);
            if (westBlocked && !eastBlocked) {
                motionX = 0.02f;
            } else if (eastBlocked && !westBlocked) {
                motionX = -0.02f;
            }
        } else if (RailShapeResolver.isNorthSouth(shape)) {
            boolean northBlocked = isOpaqueBlock(railX, railY, railZ - 1);
            boolean southBlocked = isOpaqueBlock(railX, railY, railZ + 1);
            if (northBlocked && !southBlocked) {
                motionZ = 0.02f;
            } else if (southBlocked && !northBlocked) {
                motionZ = -0.02f;
            }
        }
    }

    private boolean isOpaqueBlock(int x, int y, int z) {
        return world != null && BlockShape.isOpaqueCube(world.getBlockIfLoaded(x, y, z, BlockType.AIR));
    }

    protected void applySpecialCartForces() {
    }

    protected void applyRailFriction() {
        float railDrag = railDragMultiplier();
        motionX *= railDrag;
        motionZ *= railDrag;
    }

    private void applyRailHeightSpeedAdjustment(float startPathY, float endPathY) {
        if (Float.isNaN(startPathY) || Float.isNaN(endPathY)) {
            return;
        }
        float speed = (float) Math.sqrt(motionX * motionX + motionZ * motionZ);
        if (speed <= 0.0f) {
            return;
        }
        float adjustedSpeed = speed + (startPathY - endPathY) * 0.05f;
        motionX = motionX / speed * adjustedSpeed;
        motionZ = motionZ / speed * adjustedSpeed;
    }

    private void realignMotionAfterCellCrossing(int startRailX, int startRailZ) {
        int currentRailX = (int) Math.floor(x);
        int currentRailZ = (int) Math.floor(z);
        int crossedX = currentRailX - startRailX;
        int crossedZ = currentRailZ - startRailZ;
        if (crossedX == 0 && crossedZ == 0) {
            return;
        }
        float speed = (float) Math.sqrt(motionX * motionX + motionZ * motionZ);
        motionX = speed * crossedX;
        motionZ = speed * crossedZ;
    }

    private void clampHorizontalSpeed() {
        motionX = clamp(motionX, -MAX_RAIL_AXIS_SPEED, MAX_RAIL_AXIS_SPEED);
        motionZ = clamp(motionZ, -MAX_RAIL_AXIS_SPEED, MAX_RAIL_AXIS_SPEED);
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    public void collideWithMinecart(MinecartEntity other) {
        if (other == null || other == this || removed || other.removed) {
            return;
        }
        float dx = other.x - x;
        float dz = other.z - z;
        float distanceSq = dx * dx + dz * dz;
        if (distanceSq < COLLISION_EPSILON) {
            dx = motionX - other.motionX;
            dz = motionZ - other.motionZ;
            distanceSq = dx * dx + dz * dz;
            if (distanceSq < COLLISION_EPSILON) {
                dx = 1.0f;
                dz = 0.0f;
                distanceSq = 1.0f;
            }
        }

        float distance = (float) Math.sqrt(distanceSq);
        float nx = dx / distance;
        float nz = dz / distance;
        float impulseScale = COLLISION_IMPULSE * Math.min(1.0f, 1.0f / distance);
        float impulseX = nx * impulseScale;
        float impulseZ = nz * impulseScale;

        if (other.kind == CartKind.FURNACE && kind != CartKind.FURNACE) {
            motionX = motionX * POST_COLLISION_DAMPING + other.motionX - impulseX;
            motionZ = motionZ * POST_COLLISION_DAMPING + other.motionZ - impulseZ;
            other.motionX *= FURNACE_COLLISION_DAMPING;
            other.motionZ *= FURNACE_COLLISION_DAMPING;
        } else if (other.kind != CartKind.FURNACE && kind == CartKind.FURNACE) {
            other.motionX = other.motionX * POST_COLLISION_DAMPING + motionX + impulseX;
            other.motionZ = other.motionZ * POST_COLLISION_DAMPING + motionZ + impulseZ;
            motionX *= FURNACE_COLLISION_DAMPING;
            motionZ *= FURNACE_COLLISION_DAMPING;
        } else {
            float averageX = (motionX + other.motionX) * 0.5f;
            float averageZ = (motionZ + other.motionZ) * 0.5f;
            motionX = motionX * POST_COLLISION_DAMPING + averageX - impulseX;
            motionZ = motionZ * POST_COLLISION_DAMPING + averageZ - impulseZ;
            other.motionX = other.motionX * POST_COLLISION_DAMPING + averageX + impulseX;
            other.motionZ = other.motionZ * POST_COLLISION_DAMPING + averageZ + impulseZ;
        }
        clampHorizontalSpeed();
        other.clampHorizontalSpeed();
    }

    public void collideWithLivingEntity(LivingEntity entity) {
        if (entity == null || removed || entity.isRemoved()) {
            return;
        }
        if (entity == livingPassenger) {
            syncPassengerPosition();
            return;
        }
        if (entityRidesAnotherMinecart(entity)) {
            return;
        }
        if (mountLivingEntityFromCollision(entity)) {
            return;
        }
        NonCartCollisionPush push = computeNonCartCollisionPush(entity.x, entity.z);
        if (push == null) {
            return;
        }

        motionX += push.cartX();
        motionZ += push.cartZ();
        entity.motionX += push.entityX();
        entity.motionZ += push.entityZ();
        clampHorizontalSpeed();
    }

    public NonCartCollisionPush computeNonCartCollisionPush(float entityX, float entityZ) {
        float dx = entityX - x;
        float dz = entityZ - z;
        float distanceSq = dx * dx + dz * dz;
        if (distanceSq < NON_CART_COLLISION_MIN_DISTANCE_SQ) {
            dx = motionX;
            dz = motionZ;
            distanceSq = dx * dx + dz * dz;
            if (distanceSq < NON_CART_COLLISION_MIN_DISTANCE_SQ) {
                dx = 1.0f;
                dz = 0.0f;
                distanceSq = 1.0f;
            }
        }

        float distance = (float) Math.sqrt(distanceSq);
        float nx = dx / distance;
        float nz = dz / distance;
        float impulse = Math.min(1.0f, 1.0f / distance) * NON_CART_COLLISION_CART_IMPULSE;
        float entityImpulse = impulse * NON_CART_COLLISION_ENTITY_IMPULSE_MULTIPLIER;
        return new NonCartCollisionPush(
                -nx * impulse,
                -nz * impulse,
                nx * entityImpulse,
                nz * entityImpulse);
    }

    public void bump(float amount) {
        damage += amount;
        rollingAmplitude = HIT_ROLLING_TICKS;
    }

    public boolean attack(float amount, boolean creative) {
        if (removed || amount <= 0.0f) {
            return false;
        }
        rollingDirection = -rollingDirection;
        rollingAmplitude = HIT_ROLLING_TICKS;
        damage += amount * DAMAGE_PER_ATTACK_POINT;
        if (creative) {
            dismountPlayer();
            remove();
        } else if (damage > BREAK_DAMAGE) {
            dropAsItem();
        }
        return true;
    }

    public void dropAsItem() {
        dismountAllPassengers();
        if (world != null) {
            world.spawnThrownStack(x, y + 0.25f, z, new ItemStack(ItemType.MINECART, 1), 0.0f, 0.15f, 0.0f);
        }
        remove();
    }

    public boolean mountPlayer() {
        if (kind != CartKind.RIDEABLE || playerPassenger || hasLivingPassenger() || removed) {
            return false;
        }
        playerPassenger = true;
        return true;
    }

    public void dismountPlayer() {
        playerPassenger = false;
        riderInputStartedCart = false;
    }

    public boolean hasPlayerPassenger() {
        return playerPassenger;
    }

    public boolean mountLivingEntity(LivingEntity entity) {
        if (kind != CartKind.RIDEABLE || playerPassenger || hasLivingPassenger()
                || entity == null || entity.isRemoved() || entity.isDead()
                || entityRidesAnotherMinecart(entity)) {
            return false;
        }
        livingPassenger = entity;
        syncPassengerPosition();
        return true;
    }

    private boolean mountLivingEntityFromCollision(LivingEntity entity) {
        float horizontalSpeedSq = motionX * motionX + motionZ * motionZ;
        if (horizontalSpeedSq <= LIVING_MOUNT_MIN_SPEED_SQ) {
            return false;
        }
        return mountLivingEntity(entity);
    }

    private boolean entityRidesAnotherMinecart(LivingEntity entity) {
        if (world == null || entity == null) {
            return false;
        }
        for (Entity candidate : world.getEntities()) {
            if (candidate instanceof MinecartEntity cart
                    && cart != this
                    && !cart.isRemoved()
                    && cart.livingPassenger == entity
                    && cart.hasLivingPassenger()) {
                return true;
            }
        }
        return false;
    }

    public void dismountLivingPassenger() {
        livingPassenger = null;
    }

    private void dismountAllPassengers() {
        dismountPlayer();
        dismountLivingPassenger();
    }

    public LivingEntity getLivingPassenger() {
        return hasLivingPassenger() ? livingPassenger : null;
    }

    public boolean hasLivingPassenger() {
        if (livingPassenger == null) {
            return false;
        }
        if (livingPassenger.isRemoved() || livingPassenger.isDead()) {
            livingPassenger = null;
            return false;
        }
        return true;
    }

    public boolean hasAnyPassenger() {
        return playerPassenger || hasLivingPassenger();
    }

    public void syncPassengerPosition() {
        if (!hasLivingPassenger()) {
            return;
        }
        livingPassenger.stopMoving();
        livingPassenger.setMotion(motionX, motionY, motionZ);
        livingPassenger.setPosition(x, y + PASSENGER_Y_OFFSET, z);
    }

    public void applyRiderInput(float yawDegrees) {
        if (!playerPassenger) {
            return;
        }
        float speedSq = motionX * motionX + motionZ * motionZ;
        if (speedSq >= 0.01f) {
            return;
        }
        float yawRad = (float) Math.toRadians(yawDegrees);
        motionX += (float) Math.sin(yawRad) * 0.1f;
        motionZ += (float) -Math.cos(yawRad) * 0.1f;
        riderInputStartedCart = true;
    }

    public CartKind getKind() {
        return kind;
    }

    public ItemType getItemType() {
        return switch (kind) {
            case CHEST -> ItemType.CHEST_MINECART;
            case FURNACE -> ItemType.FURNACE_MINECART;
            default -> ItemType.MINECART;
        };
    }

    public int getRollingAmplitude() {
        return rollingAmplitude;
    }

    public int getRollingDirection() {
        return rollingDirection;
    }

    public void restoreRollingState(int rollingAmplitude, int rollingDirection) {
        this.rollingAmplitude = Math.max(0, Math.min(HIT_ROLLING_TICKS, rollingAmplitude));
        this.rollingDirection = rollingDirection < 0 ? -1 : 1;
    }

    public float getDamage() {
        return damage;
    }

    public void setDamage(float damage) {
        this.damage = Math.max(0.0f, damage);
    }
}
