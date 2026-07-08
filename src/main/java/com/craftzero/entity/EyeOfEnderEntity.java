package com.craftzero.entity;

import com.craftzero.inventory.ItemStack;
import com.craftzero.inventory.ItemType;
import com.craftzero.world.BlockType;
import com.craftzero.world.WorldParticle;

import java.util.Random;

/**
 * Transient Eye of Ender locator projectile.
 */
public class EyeOfEnderEntity extends Entity {
    public static final int LIFE_TICKS = 80;
    private static final float FAR_TARGET_DISTANCE = 12.0f;
    private static final float FAR_TARGET_RISE = 8.0f;
    private static final float SOURCE_HORIZONTAL_ACCELERATION = 0.0025f;
    private static final float SOURCE_VERTICAL_ACCELERATION = 0.014999999f;
    private static final float SOURCE_CLOSE_TARGET_DISTANCE = 1.0f;
    private static final float SOURCE_CLOSE_DAMPING = 0.8f;
    private static final float TRAIL_BACKSTEP = 0.25f;
    private static final float TRAIL_SPREAD = 0.6f;
    private static final int WATER_TRAIL_BUBBLES = 4;
    private static final float WATER_TRAIL_BUBBLE_SCALE = 0.05f;
    private static final int WATER_TRAIL_BUBBLE_LIFETIME_TICKS = 8;
    private static final int SHATTER_ITEM_CRACK_PARTICLES = 8;
    private static final int SHATTER_PORTAL_RING_STEPS = 40;
    private static final float SHATTER_PORTAL_RADIUS = 5.0f;
    private static final float SHATTER_INNER_PORTAL_PULL = -5.0f;
    private static final float SHATTER_OUTER_PORTAL_PULL = -7.0f;
    private static final float SHATTER_PORTAL_Y_OFFSET = -0.4f;
    private static final float SHATTER_ITEM_CRACK_SCALE = 0.10f;
    private static final int SHATTER_ITEM_CRACK_LIFETIME_TICKS = 16;

    private float targetX;
    private float targetY;
    private float targetZ;
    private final boolean dropsItem;

    public EyeOfEnderEntity(float x, float y, float z, float targetX, float targetY, float targetZ, boolean dropsItem) {
        super(0.25f, 0.25f);
        this.targetX = targetX;
        this.targetY = targetY;
        this.targetZ = targetZ;
        this.dropsItem = dropsItem;
        setPosition(x, y, z);
    }

    public void moveTowards(float finalTargetX, float finalTargetY, float finalTargetZ) {
        float dx = finalTargetX - x;
        float dz = finalTargetZ - z;
        float horizontal = (float) Math.sqrt(dx * dx + dz * dz);
        if (horizontal > FAR_TARGET_DISTANCE) {
            targetX = x + dx / horizontal * FAR_TARGET_DISTANCE;
            targetY = y + FAR_TARGET_RISE;
            targetZ = z + dz / horizontal * FAR_TARGET_DISTANCE;
            return;
        }
        targetX = finalTargetX;
        targetY = finalTargetY;
        targetZ = finalTargetZ;
    }

    @Override
    public void updatePhysics(float deltaTime) {
        if (world == null || removed) {
            return;
        }
        x += motionX;
        y += motionY;
        z += motionZ;
        updateRotationFromMotion();

        steerTowardsTarget();
        updateFluidState();
        spawnPortalTrail();

        if (ticksExisted > LIFE_TICKS) {
            expire();
        }
    }

    private void updateRotationFromMotion() {
        float horizontal = (float) Math.sqrt(motionX * motionX + motionZ * motionZ);
        if (horizontal > 0.0001f || Math.abs(motionY) > 0.0001f) {
            yaw = (float) Math.toDegrees(Math.atan2(motionX, -motionZ));
            pitch = (float) Math.toDegrees(Math.atan2(motionY, horizontal));
        }
    }

    private void steerTowardsTarget() {
        float dx = targetX - x;
        float dz = targetZ - z;
        float targetDistance = (float) Math.sqrt(dx * dx + dz * dz);
        if (targetDistance <= 0.0001f) {
            return;
        }

        float currentHorizontal = (float) Math.sqrt(motionX * motionX + motionZ * motionZ);
        float nextHorizontal = currentHorizontal
                + (targetDistance - currentHorizontal) * SOURCE_HORIZONTAL_ACCELERATION;
        float angle = (float) Math.atan2(dz, dx);

        if (targetDistance < SOURCE_CLOSE_TARGET_DISTANCE) {
            nextHorizontal *= SOURCE_CLOSE_DAMPING;
            motionY *= SOURCE_CLOSE_DAMPING;
        }

        motionX = (float) Math.cos(angle) * nextHorizontal;
        motionZ = (float) Math.sin(angle) * nextHorizontal;
        if (y < targetY) {
            motionY += (1.0f - motionY) * SOURCE_VERTICAL_ACCELERATION;
        } else {
            motionY += (-1.0f - motionY) * SOURCE_VERTICAL_ACCELERATION;
        }
    }

    private void spawnPortalTrail() {
        if (world == null) {
            return;
        }
        if (inWater) {
            spawnWaterBubbleTrail();
            return;
        }
        Random random = world.getRandom();
        float px = x - motionX * TRAIL_BACKSTEP + (random.nextFloat() * TRAIL_SPREAD) - TRAIL_SPREAD * 0.5f;
        float py = y - motionY * TRAIL_BACKSTEP - 0.5f;
        float pz = z - motionZ * TRAIL_BACKSTEP + (random.nextFloat() * TRAIL_SPREAD) - TRAIL_SPREAD * 0.5f;
        float scale = 0.18f + random.nextFloat() * 0.04f;
        world.spawnParticle(WorldParticle.Type.PORTAL, px, py, pz,
                motionX, motionY, motionZ, scale, 20);
    }

    private void spawnWaterBubbleTrail() {
        for (int i = 0; i < WATER_TRAIL_BUBBLES; i++) {
            world.spawnParticle(WorldParticle.Type.BUBBLE,
                    x - motionX * TRAIL_BACKSTEP,
                    y - motionY * TRAIL_BACKSTEP,
                    z - motionZ * TRAIL_BACKSTEP,
                    motionX, motionY, motionZ,
                    WATER_TRAIL_BUBBLE_SCALE,
                    WATER_TRAIL_BUBBLE_LIFETIME_TICKS);
        }
    }

    private void updateFluidState() {
        if (world == null) {
            inWater = false;
            return;
        }
        int blockX = (int) Math.floor(x);
        int blockY = (int) Math.floor(y + getHeight() * 0.1f);
        int blockZ = (int) Math.floor(z);
        BlockType block = world.getBlockIfLoaded(blockX, blockY, blockZ, BlockType.AIR);
        inWater = block.isWater();
        updateWaterEntryParticles();
    }

    private void expire() {
        if (dropsItem && world != null) {
            world.spawnThrownStack(x, y, z, new ItemStack(ItemType.EYE_OF_ENDER, 1), 0.0f, 0.1f, 0.0f);
        } else if (world != null) {
            spawnShatterParticles();
        }
        remove();
    }

    private void spawnShatterParticles() {
        Random random = world.getRandom();
        int effectX = Math.round(x);
        int effectY = Math.round(y);
        int effectZ = Math.round(z);
        float centerX = effectX + 0.5f;
        float centerY = effectY;
        float centerZ = effectZ + 0.5f;

        for (int i = 0; i < SHATTER_ITEM_CRACK_PARTICLES; i++) {
            world.spawnParticle(WorldParticle.Type.ITEM_CRACK,
                    centerX, centerY, centerZ,
                    (float) random.nextGaussian() * 0.15f,
                    random.nextFloat() * 0.20f,
                    (float) random.nextGaussian() * 0.15f,
                    SHATTER_ITEM_CRACK_SCALE,
                    SHATTER_ITEM_CRACK_LIFETIME_TICKS,
                    WorldParticle.itemParticleData(ItemType.EYE_OF_ENDER));
        }

        for (int i = 0; i < SHATTER_PORTAL_RING_STEPS; i++) {
            float angle = (float) (i * Math.PI * 2.0 / SHATTER_PORTAL_RING_STEPS);
            float radialX = (float) Math.cos(angle);
            float radialZ = (float) Math.sin(angle);
            float particleX = centerX + radialX * SHATTER_PORTAL_RADIUS;
            float particleY = centerY + SHATTER_PORTAL_Y_OFFSET;
            float particleZ = centerZ + radialZ * SHATTER_PORTAL_RADIUS;
            world.spawnParticle(WorldParticle.Type.PORTAL,
                    particleX, particleY, particleZ,
                    radialX * SHATTER_INNER_PORTAL_PULL, 0.0f,
                    radialZ * SHATTER_INNER_PORTAL_PULL,
                    0.25f, 40);
            world.spawnParticle(WorldParticle.Type.PORTAL,
                    particleX, particleY, particleZ,
                    radialX * SHATTER_OUTER_PORTAL_PULL, 0.0f,
                    radialZ * SHATTER_OUTER_PORTAL_PULL,
                    0.25f, 40);
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

    public boolean dropsItem() {
        return dropsItem;
    }
}
