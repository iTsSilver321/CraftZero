package com.craftzero.entity.mob;

import com.craftzero.combat.DamageSource;
import com.craftzero.inventory.ItemType;
import com.craftzero.world.BlockType;

public class Squid extends Mob {
    private int swimTimer;
    private int airTicks;
    private float swimX;
    private float swimY;
    private float swimZ;
    private float squidPitch;
    private float prevSquidPitch;
    private float squidYaw;
    private float prevSquidYaw;
    private float squidRotation;
    private float prevSquidRotation;
    private float tentacleAngle;
    private float prevTentacleAngle;

    public Squid() {
        super(MobDefinition.SQUID.width(), MobDefinition.SQUID.height(), MobDefinition.SQUID.maxHealth());
        this.definition = MobDefinition.SQUID;
        this.hostile = false;
        this.burnsInSunlight = false;
        this.moveSpeed = MobDefinition.SQUID.moveSpeed();
        this.experienceValue = MobDefinition.SQUID.experienceValue();
        this.swimTimer = 0;
    }

    @Override
    public void tick() {
        prevSquidPitch = squidPitch;
        prevSquidYaw = squidYaw;
        prevSquidRotation = squidRotation;
        prevTentacleAngle = tentacleAngle;

        boolean water = world != null && world.getBlockIfLoaded((int) Math.floor(x), (int) Math.floor(y + 0.2f),
                (int) Math.floor(z), BlockType.AIR).isWater();
        if (water) {
            airTicks = 0;
            if (swimTimer-- <= 0) {
                chooseSwimVector();
                swimTimer = 40 + random.nextInt(80);
            }
            motionX += swimX * 0.018f;
            motionY += swimY * 0.014f + (float) Math.sin(ticksExisted * 0.08f) * 0.0015f;
            motionZ += swimZ * 0.018f;
            float horizontalSpeed = (float) Math.sqrt(motionX * motionX + motionZ * motionZ);
            if (horizontalSpeed > 0.002f) {
                yaw = (float) Math.toDegrees(Math.atan2(motionX, -motionZ));
            }
            pitch = (float) Math.toDegrees(-Math.atan2(motionY, Math.max(0.001f, horizontalSpeed)));
            squidYaw += wrapDegreesLocal(yaw - squidYaw) * 0.2f;
            squidPitch += (pitch - squidPitch) * 0.2f;
            squidRotation += 0.12f + horizontalSpeed * 1.8f;
            tentacleAngle = (float) Math.sin(squidRotation) * 0.55f + 0.35f;
        } else {
            airTicks++;
            squidPitch += (-90.0f - squidPitch) * 0.08f;
            tentacleAngle = (float) Math.sin(ticksExisted * 0.25f) * 0.25f;
            motionX *= 0.7f;
            motionZ *= 0.7f;
            if (onGround && ticksExisted % 20 == 0) {
                motionY += 0.22f;
                motionX += (random.nextFloat() - 0.5f) * 0.08f;
                motionZ += (random.nextFloat() - 0.5f) * 0.08f;
            }
            if (airTicks > 300 && airTicks % 20 == 0) {
                damage(1.0f, DamageSource.generic());
            }
        }
        tickWithoutAi();
    }

    private void chooseSwimVector() {
        double yawRad = random.nextDouble() * Math.PI * 2.0;
        swimX = (float) Math.sin(yawRad);
        swimZ = -(float) Math.cos(yawRad);
        swimY = (random.nextFloat() - 0.5f) * 0.45f;

        if (world != null) {
            BlockType below = world.getBlockIfLoaded((int) Math.floor(x), (int) Math.floor(y - 0.7f),
                    (int) Math.floor(z), BlockType.AIR);
            BlockType above = world.getBlockIfLoaded((int) Math.floor(x), (int) Math.floor(y + height + 0.6f),
                    (int) Math.floor(z), BlockType.AIR);
            if (!below.isWater()) {
                swimY = Math.max(swimY, 0.25f);
            }
            if (!above.isWater()) {
                swimY = Math.min(swimY, -0.35f);
            }
        }

        float length = (float) Math.sqrt(swimX * swimX + swimY * swimY + swimZ * swimZ);
        if (length > 0.0001f) {
            swimX /= length;
            swimY /= length;
            swimZ /= length;
        }
    }

    @Override
    protected float getWaterGravityPerTick() {
        return 0.0f;
    }

    @Override
    protected float getWaterHorizontalDrag() {
        return 0.82f;
    }

    @Override
    protected float getWaterVerticalDrag() {
        return 0.82f;
    }

    @Override
    protected boolean usesDefaultWaterBobbing() {
        return false;
    }

    @Override
    protected boolean shouldSurfaceFloatInWater() {
        return false;
    }

    @Override
    public void dropLoot() {
        dropItems(ItemType.INK_SAC, 1, 3);
    }

    @Override
    public String getTexturePath() {
        return "/textures/mob/squid.png";
    }

    @Override
    public MobModelType getModelType() {
        return MobModelType.SQUID;
    }

    public float getRenderSquidPitch(float partialTick) {
        return prevSquidPitch + (squidPitch - prevSquidPitch) * partialTick;
    }

    public float getRenderSquidYaw(float partialTick) {
        return prevSquidYaw + wrapDegreesLocal(squidYaw - prevSquidYaw) * partialTick;
    }

    public float getRenderTentacleAngle(float partialTick) {
        return prevTentacleAngle + (tentacleAngle - prevTentacleAngle) * partialTick;
    }

    public float getRenderSquidRotation(float partialTick) {
        return prevSquidRotation + (squidRotation - prevSquidRotation) * partialTick;
    }

    private static float wrapDegreesLocal(float angle) {
        while (angle >= 180.0f) {
            angle -= 360.0f;
        }
        while (angle < -180.0f) {
            angle += 360.0f;
        }
        return angle;
    }
}
