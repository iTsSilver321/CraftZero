package com.craftzero.entity;

import com.craftzero.inventory.ItemStack;
import com.craftzero.inventory.ItemType;
import com.craftzero.world.BlockType;
import com.craftzero.world.Chunk;
import com.craftzero.world.World;
import com.craftzero.world.WorldParticle;
import org.joml.Vector3f;

import java.util.Random;

public class BoatEntity extends Entity {
    public static final int HIT_ROLLING_TICKS = 10;
    public static final float DAMAGE_PER_ATTACK_POINT = 10.0f;
    public static final float BREAK_DAMAGE = 40.0f;
    public static final float MAX_WATER_SPEED = 0.4f;
    public static final float CRASH_BREAK_SPEED = 0.15f;
    public static final float MAX_YAW_TURN_DEGREES = 20.0f;

    private static final float RIDER_ACCELERATION = 0.04f;
    private static final float SPLASH_WAKE_SPEED = 0.15f;
    private static final float SPLASH_WAKE_SIDE_OFFSET = 0.7f;
    private static final float SPLASH_WAKE_LENGTH = 0.8f;
    private static final float SPLASH_WAKE_Y_OFFSET = 0.125f;
    private static final float SPLASH_WAKE_SCALE = 0.16f;
    private static final int SPLASH_WAKE_LIFETIME_TICKS = 10;
    private static final float FRAGILE_BLOCK_CLEARANCE = 0.25f;
    private static final float ENTITY_COLLISION_MIN_AXIS = 0.01f;
    private static final float ENTITY_COLLISION_IMPULSE = 0.05f;
    private static final float BOAT_COLLISION_DAMPING = 0.5f;
    private static final int WATER_SAMPLE_SLICES = 5;

    private int rollingAmplitude;
    private int rollingDirection = 1;
    private float damage;
    private boolean playerPassenger;
    private float riderForward;
    private float riderStrafe;
    private float riderYaw;

    public BoatEntity() {
        super(1.5f, 0.6f);
    }

    public BoatEntity(float x, float y, float z) {
        this();
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
        if (world == null || removed) {
            return;
        }

        float previousSpeed = horizontalSpeed();
        float waterFraction = waterFraction();
        if (waterFraction > 0.0f) {
            if (waterFraction >= 1.0f) {
                if (motionY < 0.0f) {
                    motionY *= 0.5f;
                }
                motionY += 0.007f;
            } else {
                motionY += (waterFraction * 2.0f - 1.0f) * 0.04f;
            }
            applyWaterCurrent();
        } else if (!onGround) {
            motionY -= 0.08f;
        }

        applyRiderAcceleration();
        clampHorizontalSpeed();
        clearFragileBlocksAroundBoat();
        moveWithCollision(motionX, motionY, motionZ);

        if (collidedHorizontally && previousSpeed > CRASH_BREAK_SPEED) {
            breakAsCrashDrops();
            clearRiderInput();
            return;
        }

        updateYawFromTravel();

        emitSplashWake(waterFraction);

        motionX *= 0.99f;
        motionY *= 0.95f;
        motionZ *= 0.99f;
        if (onGround) {
            motionX *= 0.5f;
            motionZ *= 0.5f;
        }
        clearRiderInput();
    }

    private void emitSplashWake(float waterFraction) {
        float speed = horizontalSpeed();
        if (world == null || waterFraction <= 0.0f || speed <= SPLASH_WAKE_SPEED) {
            return;
        }
        Random random = world.getRandom();
        int count = (int) (1.0f + speed * 60.0f);
        float yawRadians = (float) Math.toRadians(yaw);
        float cosYaw = (float) Math.cos(yawRadians);
        float sinYaw = (float) Math.sin(yawRadians);
        for (int i = 0; i < count; i++) {
            float wakeSpread = random.nextFloat() * 2.0f - 1.0f;
            float side = (random.nextInt(2) * 2 - 1) * SPLASH_WAKE_SIDE_OFFSET;
            float particleX;
            float particleZ;
            if (random.nextBoolean()) {
                particleX = x - cosYaw * wakeSpread * SPLASH_WAKE_LENGTH + sinYaw * side;
                particleZ = z - sinYaw * wakeSpread * SPLASH_WAKE_LENGTH - cosYaw * side;
            } else {
                particleX = x + cosYaw + sinYaw * wakeSpread * SPLASH_WAKE_SIDE_OFFSET;
                particleZ = z + sinYaw - cosYaw * wakeSpread * SPLASH_WAKE_SIDE_OFFSET;
            }
            world.spawnParticle(WorldParticle.Type.SPLASH, particleX, y - SPLASH_WAKE_Y_OFFSET, particleZ,
                    motionX, motionY, motionZ, SPLASH_WAKE_SCALE, SPLASH_WAKE_LIFETIME_TICKS);
        }
    }

    private float waterFraction() {
        int waterSlices = 0;
        for (int i = 0; i < WATER_SAMPLE_SLICES; i++) {
            float sliceMinY = y + height * i / WATER_SAMPLE_SLICES;
            float sliceMaxY = y + height * (i + 1) / WATER_SAMPLE_SLICES;
            if (hasWaterInBox(x - width * 0.5f, sliceMinY, z - width * 0.5f,
                    x + width * 0.5f, sliceMaxY, z + width * 0.5f)) {
                waterSlices++;
            }
        }
        return waterSlices / (float) WATER_SAMPLE_SLICES;
    }

    private boolean hasWaterInBox(float minX, float minY, float minZ, float maxX, float maxY, float maxZ) {
        int startX = (int) Math.floor(minX);
        int startY = Math.max(0, (int) Math.floor(minY));
        int startZ = (int) Math.floor(minZ);
        int endX = (int) Math.floor(maxX - 0.0001f);
        int endY = Math.min(Chunk.HEIGHT - 1, (int) Math.floor(maxY - 0.0001f));
        int endZ = (int) Math.floor(maxZ - 0.0001f);
        for (int bx = startX; bx <= endX; bx++) {
            for (int by = startY; by <= endY; by++) {
                for (int bz = startZ; bz <= endZ; bz++) {
                    if (world.getBlockIfLoaded(bx, by, bz, BlockType.AIR).isWater()) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private void applyWaterCurrent() {
        Vector3f current = world.getFluidFlowVector(getBoundingBox(), true);
        if (current.lengthSquared() <= 0.000001f) {
            return;
        }
        motionX += current.x * World.FLUID_CURRENT_PUSH_PER_TICK;
        motionY += current.y * World.FLUID_CURRENT_PUSH_PER_TICK;
        motionZ += current.z * World.FLUID_CURRENT_PUSH_PER_TICK;
    }

    private void applyRiderAcceleration() {
        if (!playerPassenger) {
            return;
        }
        float forward = clamp(riderForward, -1.0f, 1.0f);
        float strafe = clamp(riderStrafe, -1.0f, 1.0f);
        if (forward == 0.0f && strafe == 0.0f) {
            return;
        }
        float yawRad = (float) Math.toRadians(riderYaw);
        float sinYaw = (float) Math.sin(yawRad);
        float cosYaw = (float) Math.cos(yawRad);
        motionX += (forward * sinYaw + strafe * cosYaw) * RIDER_ACCELERATION;
        motionZ += (-forward * cosYaw + strafe * sinYaw) * RIDER_ACCELERATION;
        yaw = riderYaw;
    }

    private void clampHorizontalSpeed() {
        motionX = clamp(motionX, -MAX_WATER_SPEED, MAX_WATER_SPEED);
        motionZ = clamp(motionZ, -MAX_WATER_SPEED, MAX_WATER_SPEED);
    }

    private float horizontalSpeed() {
        return (float) Math.sqrt(motionX * motionX + motionZ * motionZ);
    }

    private void updateYawFromTravel() {
        float dx = prevX - x;
        float dz = prevZ - z;
        if (dx * dx + dz * dz <= 0.001f) {
            return;
        }
        float targetYaw = (float) Math.toDegrees(Math.atan2(dz, dx));
        float deltaYaw = wrapDegrees(targetYaw - yaw);
        yaw += clamp(deltaYaw, -MAX_YAW_TURN_DEGREES, MAX_YAW_TURN_DEGREES);
    }

    private void clearFragileBlocksAroundBoat() {
        int minX = (int) Math.floor(x - width * 0.5f);
        int maxX = (int) Math.floor(x + width * 0.5f);
        int minY = Math.max(0, (int) Math.floor(y));
        int maxY = Math.min(Chunk.HEIGHT - 1, (int) Math.floor(y + height + FRAGILE_BLOCK_CLEARANCE));
        int minZ = (int) Math.floor(z - width * 0.5f);
        int maxZ = (int) Math.floor(z + width * 0.5f);
        for (int bx = minX; bx <= maxX; bx++) {
            for (int by = minY; by <= maxY; by++) {
                for (int bz = minZ; bz <= maxZ; bz++) {
                    BlockType type = world.getBlockIfLoaded(bx, by, bz, BlockType.AIR);
                    if (type == BlockType.LILY_PAD) {
                        world.breakBlock(bx, by, bz, true);
                    } else if (type == BlockType.SNOW_LAYER) {
                        world.setBlockIfLoaded(bx, by, bz, BlockType.AIR, 0);
                    }
                }
            }
        }
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
        dismountPlayer();
        dropLegacyComponentItems();
        remove();
    }

    void breakAsCrashDrops() {
        dismountPlayer();
        dropLegacyComponentItems();
        remove();
    }

    private void dropLegacyComponentItems() {
        if (world != null) {
            world.spawnThrownStack(x, y + 0.25f, z, new ItemStack(ItemType.OAK_PLANKS, 3), 0.0f, 0.15f, 0.0f);
            world.spawnThrownStack(x, y + 0.25f, z, new ItemStack(ItemType.STICK, 2), 0.0f, 0.15f, 0.0f);
        }
    }

    public boolean mountPlayer() {
        if (playerPassenger || removed) {
            return false;
        }
        playerPassenger = true;
        return true;
    }

    public void dismountPlayer() {
        playerPassenger = false;
        clearRiderInput();
    }

    public boolean hasPlayerPassenger() {
        return playerPassenger;
    }

    public void applyRiderInput(float yawDegrees, float forward, float strafe) {
        if (!playerPassenger) {
            return;
        }
        riderYaw = yawDegrees;
        riderForward = forward;
        riderStrafe = strafe;
    }

    public void collideWithBoat(BoatEntity other) {
        collideWithEntity(other, BOAT_COLLISION_DAMPING);
        if (other != null) {
            other.clampHorizontalSpeed();
        }
    }

    public void collideWithEntity(Entity other) {
        collideWithEntity(other, 1.0f);
    }

    private void collideWithEntity(Entity other, float pushMultiplier) {
        if (other == null || other == this || removed || other.isRemoved()) {
            return;
        }
        float dx = other.x - x;
        float dz = other.z - z;
        float maxAxis = Math.max(Math.abs(dx), Math.abs(dz));
        if (maxAxis < ENTITY_COLLISION_MIN_AXIS) {
            dx = motionX - other.motionX;
            dz = motionZ - other.motionZ;
            maxAxis = Math.max(Math.abs(dx), Math.abs(dz));
            if (maxAxis < ENTITY_COLLISION_MIN_AXIS) {
                dx = 1.0f;
                dz = 0.0f;
                maxAxis = 1.0f;
            }
        }

        float distance = (float) Math.sqrt(maxAxis);
        dx /= distance;
        dz /= distance;
        float pushScale = Math.min(1.0f, 1.0f / distance) * ENTITY_COLLISION_IMPULSE * pushMultiplier;
        float pushX = dx * pushScale;
        float pushZ = dz * pushScale;

        motionX -= pushX;
        motionZ -= pushZ;
        other.addMotion(pushX, 0.0f, pushZ);
        clampHorizontalSpeed();
    }

    private void clearRiderInput() {
        riderForward = 0.0f;
        riderStrafe = 0.0f;
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private static float wrapDegrees(float degrees) {
        degrees %= 360.0f;
        if (degrees >= 180.0f) {
            degrees -= 360.0f;
        }
        if (degrees < -180.0f) {
            degrees += 360.0f;
        }
        return degrees;
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
