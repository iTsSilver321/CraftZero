package com.craftzero.entity;

import com.craftzero.inventory.ItemStack;
import com.craftzero.inventory.ItemType;

/**
 * Transient Eye of Ender locator projectile.
 */
public class EyeOfEnderEntity extends Entity {
    private static final int LIFE_TICKS = 80;
    private final float targetX;
    private final float targetY;
    private final float targetZ;
    private final boolean dropsItem;

    public EyeOfEnderEntity(float x, float y, float z, float targetX, float targetY, float targetZ, boolean dropsItem) {
        super(0.25f, 0.25f);
        this.targetX = targetX;
        this.targetY = targetY;
        this.targetZ = targetZ;
        this.dropsItem = dropsItem;
        setPosition(x, y, z);
    }

    @Override
    public void updatePhysics(float deltaTime) {
        if (world == null || removed) {
            return;
        }
        float desiredX = (targetX - x) * 0.02f;
        float desiredZ = (targetZ - z) * 0.02f;
        float horizontal = (float) Math.sqrt((targetX - x) * (targetX - x) + (targetZ - z) * (targetZ - z));
        float desiredY = horizontal > 8.0f ? 0.12f : (targetY - y) * 0.02f;
        motionX += (desiredX - motionX) * 0.25f;
        motionY += (desiredY - motionY) * 0.25f;
        motionZ += (desiredZ - motionZ) * 0.25f;
        x += motionX;
        y += motionY;
        z += motionZ;
        updateRotationFromMotion();
    }

    @Override
    public void tick() {
        super.tick();
        if (ticksExisted >= LIFE_TICKS) {
            if (dropsItem && world != null) {
                world.spawnThrownStack(x, y, z, new ItemStack(ItemType.EYE_OF_ENDER, 1), 0.0f, 0.1f, 0.0f);
            }
            remove();
        }
    }

    private void updateRotationFromMotion() {
        float horizontal = (float) Math.sqrt(motionX * motionX + motionZ * motionZ);
        if (horizontal > 0.0001f || Math.abs(motionY) > 0.0001f) {
            yaw = (float) Math.toDegrees(Math.atan2(motionX, -motionZ));
            pitch = (float) Math.toDegrees(Math.atan2(motionY, horizontal));
        }
    }

    public float getTargetX() {
        return targetX;
    }

    public float getTargetY() {
        return targetY;
    }

    public float getTargetZ() {
        return targetZ;
    }
}
