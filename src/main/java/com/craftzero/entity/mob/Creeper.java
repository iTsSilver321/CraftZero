package com.craftzero.entity.mob;

import com.craftzero.entity.ai.*;
import com.craftzero.inventory.ItemType;

/**
 * Creeper mob - hostile, explodes when near player.
 */
public class Creeper extends Mob {

    private static final MobBalance.Spec SPEC = MobBalance.CREEPER;

    // Explosion mechanics
    private int fuseTime = 0;
    private static final int MAX_FUSE = 30; // 1.5 seconds
    private static final float EXPLOSION_POWER = 3.0f;
    private static final float IGNITE_DISTANCE = 3.0f;

    private boolean ignited = false;

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
        ignited = true;
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

    public void explode() {
        if (world != null) {
            world.explode(x, y + height * 0.5f, z, EXPLOSION_POWER);
        }
        remove(); // Creeper dies in explosion
    }

    @Override
    public void dropLoot() {
        dropItems(ItemType.GUNPOWDER, 0, 2);
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

    @Override
    public String getTexturePath() {
        return "/textures/mob/creeper.png";
    }

    @Override
    public MobModelType getModelType() {
        return MobModelType.CREEPER;
    }
}
