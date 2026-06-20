package com.craftzero.entity;

public class FurnaceMinecartEntity extends MinecartEntity {
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
        }
    }

    @Override
    protected void applySpecialCartForces() {
        if (fuelTicks <= 0) {
            pushX *= 0.9f;
            pushZ *= 0.9f;
            return;
        }
        float len = (float) Math.sqrt(pushX * pushX + pushZ * pushZ);
        if (len > 0.001f) {
            addMotion(pushX / len * 0.04f, 0.0f, pushZ / len * 0.04f);
        }
    }

    public void addFuel(float sourceX, float sourceZ) {
        fuelTicks += 3600;
        float dx = getX() - sourceX;
        float dz = getZ() - sourceZ;
        float len = (float) Math.sqrt(dx * dx + dz * dz);
        if (len > 0.001f) {
            pushX = dx / len;
            pushZ = dz / len;
        }
    }

    public int getFuelTicks() {
        return fuelTicks;
    }

    public void setFuelTicks(int fuelTicks) {
        this.fuelTicks = Math.max(0, fuelTicks);
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
}
