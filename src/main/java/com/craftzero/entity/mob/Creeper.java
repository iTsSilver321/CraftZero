package com.craftzero.entity.mob;

import com.craftzero.entity.ArrowEntity;
import com.craftzero.entity.Entity;
import com.craftzero.entity.ai.*;
import com.craftzero.inventory.ItemType;
import com.craftzero.world.WorldSoundEvent;

/**
 * Creeper mob - hostile, explodes when near player.
 */
public class Creeper extends Mob {

    private static final MobBalance.Spec SPEC = MobBalance.CREEPER;

    // Explosion mechanics
    private int fuseTime = 0;
    private static final int MAX_FUSE = 30; // 1.5 seconds
    private static final float EXPLOSION_POWER = 3.0f;
    private static final float POWERED_EXPLOSION_POWER = 6.0f;
    private static final float IGNITE_DISTANCE = 3.0f;
    private static final ItemType[] SKELETON_RECORD_DROPS = {
            ItemType.RECORD_13,
            ItemType.RECORD_CAT
    };

    private boolean ignited = false;
    private boolean powered;

    public Creeper() {
        super(SPEC.width(), SPEC.height(), SPEC.maxHealth());
        this.definition = MobDefinition.CREEPER;
        this.hostile = SPEC.hostile();
        this.burnsInSunlight = SPEC.burnsInSunlight(); // Creepers don't burn
        this.moveSpeed = SPEC.moveSpeed();
        this.experienceValue = SPEC.experienceValue();

        setupAI();
    }

    private void setupAI() {
        ai.addGoal(2, new TargetNearestGoal(this, ai, 16.0f));
        ai.addGoal(3, new CreeperExplodeGoal(this, ai, IGNITE_DISTANCE, MAX_FUSE));
        ai.addGoal(7, new WanderGoal(this, ai, 10.0f, 0.8f));
    }

    @Override
    public void tick() {
        super.tick();
    }

    public int advanceFuse() {
        boolean firstIgnitionTick = !ignited;
        ignited = true;
        if (firstIgnitionTick && world != null) {
            world.playSound(WorldSoundEvent.CREEPER_FUSE, x, y + getHeight() * 0.5f, z, 1.0f, 0.5f);
        }
        fuseTime++;
        return fuseTime;
    }

    public void coolFuse() {
        if (!ignited) {
            return;
        }
        fuseTime = Math.max(0, fuseTime - 1);
        if (fuseTime == 0) {
            ignited = false;
        }
    }

    public void resetFuse() {
        fuseTime = 0;
        ignited = false;
    }

    public int getMaxFuseTime() {
        return MAX_FUSE;
    }

    public int getFuseTime() {
        return fuseTime;
    }

    public void setFuseState(int fuseTime, boolean ignited) {
        this.fuseTime = Math.max(0, Math.min(MAX_FUSE, fuseTime));
        this.ignited = ignited && this.fuseTime > 0;
    }

    public void explode() {
        if (world != null) {
            world.explode(x, y + getHeight() * 0.5f, z, getExplosionPower());
        }
        remove(); // Creeper dies in explosion
    }

    @Override
    public void dropLoot() {
        dropItems(ItemType.GUNPOWDER, 0, 2);
        if (wasKilledBySkeleton()) {
            dropItem(SKELETON_RECORD_DROPS[random.nextInt(SKELETON_RECORD_DROPS.length)], 1);
        }
    }

    @Override
    protected void onHurt(float amount, Entity source) {
        super.onHurt(amount, source);
        playMobHurtSound(WorldSoundEvent.CREEPER_HURT);
    }

    @Override
    protected void onDeath() {
        playMobDeathSound(WorldSoundEvent.CREEPER_DEATH);
        super.onDeath();
    }

    private boolean wasKilledBySkeleton() {
        Entity source = lastDamageSource;
        if (source instanceof Skeleton) {
            return true;
        }
        return source instanceof ArrowEntity arrow && arrow.getShooter() instanceof Skeleton;
    }

    /**
     * Get fuse progress (0.0 to 1.0) for rendering.
     */
    public float getFuseProgress() {
        return (float) fuseTime / MAX_FUSE;
    }

    /**
     * Check if creeper is ignited (for rendering swelling effect).
     */
    public boolean isIgnited() {
        return ignited;
    }

    public boolean isPowered() {
        return powered;
    }

    public void setPowered(boolean powered) {
        this.powered = powered;
    }

    public float getExplosionPower() {
        return powered ? POWERED_EXPLOSION_POWER : EXPLOSION_POWER;
    }

    @Override
    public String getTexturePath() {
        return "/textures/mob/creeper.png";
    }

    @Override
    public MobModelType getModelType() {
        return MobModelType.CREEPER;
    }
}
