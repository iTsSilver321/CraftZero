package com.craftzero.entity.mob;

import com.craftzero.inventory.ItemType;
import com.craftzero.world.BlockType;

public class Squid extends Mob {
    private int swimTimer;

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
        boolean water = world != null && world.getBlockIfLoaded((int) Math.floor(x), (int) Math.floor(y + 0.2f),
                (int) Math.floor(z), BlockType.AIR).isWater();
        if (water) {
            if (swimTimer-- <= 0) {
                yaw = random.nextFloat() * 360.0f;
                pitch = -20.0f + random.nextFloat() * 40.0f;
                swimTimer = 40 + random.nextInt(80);
            }
            float yawRad = (float) Math.toRadians(yaw);
            motionX += (float) Math.sin(yawRad) * 0.015f;
            motionZ += -(float) Math.cos(yawRad) * 0.015f;
            motionY += Math.sin(ticksExisted * 0.08f) * 0.01f;
        } else {
            motionX *= 0.7f;
            motionZ *= 0.7f;
        }
        super.tick();
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
}
