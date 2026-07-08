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
        this.airTicks = MAX_AIR_TICKS;
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
            airTicks = MAX_AIR_TICKS;
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
            squidRotation += 0.12f;
            squidPitch += (-90.0f - squidPitch) * 0.02f;
            pitch = squidPitch;
            tentacleAngle = Math.abs((float) Math.sin(squidRotation)) * (float) Math.PI * 0.25f;
            motionX = 0.0f;
            motionZ = 0.0f;
            tickDryOutAir();
        }
        tickWithoutAi();
    }

    private void tickDryOutAir() {
        if (dead) {
            return;
        }
        airTicks--;
        if (airTicks <= DROWN_DAMAGE_AIR_TICKS) {
            airTicks = 0;
            damage(DROWN_DAMAGE, DamageSource.point(DamageSource.Type.DROWN, x, y, z, 0.0f, 0.0f));
        }
    }

    private void chooseSwimVector() {
        double yawRad = random.nextDouble() * Math.PI * 2.0;
        swimX = (float) Math.sin(yawRad);
        swimZ = -(float) Math.cos(yawRad);
        swimY = (random.nextFloat() - 0.5f) * 0.45f;

        if (world != null) {
            BlockType below = world.getBlockIfLoaded((int) Math.floor(x), (int) Math.floor(y - 0.7f),
                    (int) Math.floor(z), BlockType.AIR);
            BlockType above = world.getBlockIfLoaded((int) Math.floor(x), (int) Math.floor(y + getHeight() + 0.6f),
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
    protected boolean canBreatheUnderwater() {
        return true;
    }

    @Override
    public void dropLoot() {
        int looting = Math.max(0, getRecentPlayerLootingLevel());
        dropItem(ItemType.INK_SAC, 1 + random.nextInt(3 + looting));
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

    public int getSwimTimer() {
        return swimTimer;
    }

    public int getAirTicks() {
        return airTicks;
    }

    public float getSwimX() {
        return swimX;
    }

    public float getSwimY() {
        return swimY;
    }

    public float getSwimZ() {
        return swimZ;
    }

    public float getSquidPitch() {
        return squidPitch;
    }

    public float getPrevSquidPitch() {
        return prevSquidPitch;
    }

    public float getSquidYaw() {
        return squidYaw;
    }

    public float getPrevSquidYaw() {
        return prevSquidYaw;
    }

    public float getSquidRotation() {
        return squidRotation;
    }

    public float getPrevSquidRotation() {
        return prevSquidRotation;
    }

    public float getTentacleAngle() {
        return tentacleAngle;
    }

    public float getPrevTentacleAngle() {
        return prevTentacleAngle;
    }

    public void setSwimState(int swimTimer, int airTicks, float swimX, float swimY, float swimZ,
            float squidPitch, float prevSquidPitch, float squidYaw, float prevSquidYaw,
            float squidRotation, float prevSquidRotation, float tentacleAngle, float prevTentacleAngle) {
        this.swimTimer = Math.max(0, swimTimer);
        this.airTicks = Math.max(DROWN_DAMAGE_AIR_TICKS, Math.min(MAX_AIR_TICKS, airTicks));
        this.swimX = swimX;
        this.swimY = swimY;
        this.swimZ = swimZ;
        this.squidPitch = squidPitch;
        this.prevSquidPitch = prevSquidPitch;
        this.squidYaw = squidYaw;
        this.prevSquidYaw = prevSquidYaw;
        this.squidRotation = squidRotation;
        this.prevSquidRotation = prevSquidRotation;
        this.tentacleAngle = tentacleAngle;
        this.prevTentacleAngle = prevTentacleAngle;
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
