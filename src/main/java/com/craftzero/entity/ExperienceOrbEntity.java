package com.craftzero.entity;

import com.craftzero.main.Player;
import com.craftzero.world.BlockType;

/**
 * Release 1.0-style experience orb entity.
 */
public class ExperienceOrbEntity extends Entity {
    private static final int DESPAWN_TICKS = 6000;
    private static final float ATTRACTION_RADIUS = 8.0f;
    private static final float PICKUP_RADIUS = 1.0f;
    private static final float GRAVITY_PER_TICK = 0.03f;

    private int value;
    private int health = 5;
    private int pickupDelayTicks = 2;

    public ExperienceOrbEntity(float x, float y, float z, int value) {
        super(0.5f, 0.5f);
        this.value = Math.max(1, value);
        setPosition(x, y, z);
        yaw = (float) (Math.random() * 360.0);
        motionX = (float) ((Math.random() * 0.2 - 0.1) * 2.0);
        motionY = (float) (Math.random() * 0.2 * 2.0);
        motionZ = (float) ((Math.random() * 0.2 - 0.1) * 2.0);
    }

    @Override
    public void tick() {
        super.tick();
        if (pickupDelayTicks > 0) {
            pickupDelayTicks--;
        }
        if (ticksExisted >= DESPAWN_TICKS) {
            remove();
            return;
        }
        attractAndCollect();
    }

    @Override
    public void updatePhysics(float deltaTime) {
        if (world == null || removed) {
            return;
        }
        BlockType current = world.getBlockIfLoaded((int) Math.floor(x), (int) Math.floor(y),
                (int) Math.floor(z), BlockType.AIR);
        if (current == BlockType.LAVA || current == BlockType.FLOWING_LAVA) {
            motionY = 0.2f;
            motionX = (float) ((Math.random() - Math.random()) * 0.2);
            motionZ = (float) ((Math.random() - Math.random()) * 0.2);
        }
        super.updatePhysics(deltaTime);
    }

    private void attractAndCollect() {
        if (world == null || world.getPlayer() == null) {
            return;
        }
        Player player = world.getPlayer();
        if (player.isDead()) {
            return;
        }
        float dx = player.getPosition().x - x;
        float dy = player.getPosition().y + 1.0f - y;
        float dz = player.getPosition().z - z;
        float distanceSq = dx * dx + dy * dy + dz * dz;
        if (pickupDelayTicks <= 0 && distanceSq <= PICKUP_RADIUS * PICKUP_RADIUS) {
            player.getStats().getProgression().addExperience(value);
            remove();
            return;
        }
        if (distanceSq > ATTRACTION_RADIUS * ATTRACTION_RADIUS || distanceSq <= 0.0001f) {
            return;
        }
        float distance = (float) Math.sqrt(distanceSq);
        float strength = 1.0f - distance / ATTRACTION_RADIUS;
        strength *= strength;
        motionX += (dx / distance) * strength * 0.1f;
        motionY += (dy / distance) * strength * 0.1f;
        motionZ += (dz / distance) * strength * 0.1f;
    }

    @Override
    protected float getGravityPerTick() {
        return GRAVITY_PER_TICK;
    }

    public int getValue() {
        return value;
    }

    public int getHealth() {
        return health;
    }

    public void setHealth(int health) {
        this.health = Math.max(1, health);
    }

    public int getPickupDelayTicks() {
        return pickupDelayTicks;
    }

    public void setPickupDelayTicks(int pickupDelayTicks) {
        this.pickupDelayTicks = Math.max(0, pickupDelayTicks);
    }

    public static int getOrbValue(int amount) {
        if (amount >= 2477) {
            return 2477;
        }
        if (amount >= 1237) {
            return 1237;
        }
        if (amount >= 617) {
            return 617;
        }
        if (amount >= 307) {
            return 307;
        }
        if (amount >= 149) {
            return 149;
        }
        if (amount >= 73) {
            return 73;
        }
        if (amount >= 37) {
            return 37;
        }
        if (amount >= 17) {
            return 17;
        }
        if (amount >= 7) {
            return 7;
        }
        if (amount >= 3) {
            return 3;
        }
        return 1;
    }
}
