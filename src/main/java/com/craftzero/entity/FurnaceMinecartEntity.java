package com.craftzero.entity;

import com.craftzero.inventory.ItemStack;
import com.craftzero.inventory.ItemType;
import com.craftzero.world.WorldParticle;

public class FurnaceMinecartEntity extends MinecartEntity {
    public static final int FUEL_TICKS_PER_COAL = 3600;
    public static final int MAX_FUEL_TICKS = 32767;
    private static final float POWERED_DRAG = 0.8f;
    private static final float UNPOWERED_DRAG = 0.98f;
    private static final float ENGINE_FORCE = 0.05f;
    private static final float PUSH_EPSILON_SQ = 1.0e-4f;
    private static final float MOTION_EPSILON_SQ = 0.001f;
    private static final int EXHAUST_SMOKE_CHANCE = 4;
    private static final float EXHAUST_SMOKE_Y_OFFSET = 0.8f;
    private static final float EXHAUST_SMOKE_SCALE = 0.30f;
    private static final int EXHAUST_SMOKE_LIFETIME_TICKS = 22;

    private int fuelTicks;
    private float pushX;
    private float pushZ;

    public FurnaceMinecartEntity() {
        super(CartKind.FURNACE);
    }

    public FurnaceMinecartEntity(float x, float y, float z) {
        super(x, y, z, CartKind.FURNACE);
    }

    @Override
    public void tick() {
        super.tick();
        if (fuelTicks > 0) {
            fuelTicks--;
            if (fuelTicks == 0) {
                clearPush();
            }
        }
    }

    @Override
    public void updatePhysics(float deltaTime) {
        super.updatePhysics(deltaTime);
        emitExhaustSmoke();
    }

    private void emitExhaustSmoke() {
        if (world == null || removed || fuelTicks <= 0
                || world.getRandom().nextInt(EXHAUST_SMOKE_CHANCE) != 0) {
            return;
        }
        world.spawnParticle(WorldParticle.Type.LARGE_SMOKE, getX(), getY() + EXHAUST_SMOKE_Y_OFFSET, getZ(),
                0.0f, 0.0f, 0.0f, EXHAUST_SMOKE_SCALE, EXHAUST_SMOKE_LIFETIME_TICKS);
    }

    @Override
    protected void applySpecialCartForces() {
    }

    @Override
    protected void applyRailFriction() {
        if (fuelTicks <= 0) {
            clearPush();
            motionX *= UNPOWERED_DRAG;
            motionZ *= UNPOWERED_DRAG;
            return;
        }
        float pushLengthSq = pushX * pushX + pushZ * pushZ;
        if (pushLengthSq > PUSH_EPSILON_SQ) {
            float pushLength = (float) Math.sqrt(pushLengthSq);
            float normalizedPushX = pushX / pushLength;
            float normalizedPushZ = pushZ / pushLength;
            motionX = motionX * POWERED_DRAG + normalizedPushX * ENGINE_FORCE;
            motionZ = motionZ * POWERED_DRAG + normalizedPushZ * ENGINE_FORCE;
            updatePushFromAlignedMotion(normalizedPushX, normalizedPushZ);
        } else {
            motionX *= UNPOWERED_DRAG;
            motionZ *= UNPOWERED_DRAG;
        }
    }

    private void updatePushFromAlignedMotion(float normalizedPushX, float normalizedPushZ) {
        float motionLengthSq = motionX * motionX + motionZ * motionZ;
        if (motionLengthSq <= MOTION_EPSILON_SQ) {
            pushX = normalizedPushX;
            pushZ = normalizedPushZ;
            return;
        }
        if (normalizedPushX * motionX + normalizedPushZ * motionZ < 0.0f) {
            clearPush();
        } else {
            pushX = motionX;
            pushZ = motionZ;
        }
    }

    public void addFuel(float sourceX, float sourceZ) {
        fuelTicks = Math.min(MAX_FUEL_TICKS, fuelTicks + FUEL_TICKS_PER_COAL);
        setPushDirectionFrom(sourceX, sourceZ);
    }

    public void setPushDirectionFrom(float sourceX, float sourceZ) {
        pushX = getX() - sourceX;
        pushZ = getZ() - sourceZ;
    }

    public int getFuelTicks() {
        return fuelTicks;
    }

    public void setFuelTicks(int fuelTicks) {
        this.fuelTicks = Math.max(0, Math.min(MAX_FUEL_TICKS, fuelTicks));
    }

    public float getPushX() {
        return pushX;
    }

    public float getPushZ() {
        return pushZ;
    }

    public void setPush(float pushX, float pushZ) {
        this.pushX = pushX;
        this.pushZ = pushZ;
    }

    private void clearPush() {
        pushX = 0.0f;
        pushZ = 0.0f;
    }

    @Override
    public void dropAsItem() {
        if (world != null) {
            world.spawnThrownStack(getX(), getY() + 0.25f, getZ(),
                    new ItemStack(ItemType.FURNACE, 1), 0.0f, 0.15f, 0.0f);
        }
        super.dropAsItem();
    }
}
