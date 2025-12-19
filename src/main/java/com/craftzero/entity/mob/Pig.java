package com.craftzero.entity.mob;

import com.craftzero.entity.ai.*;
import com.craftzero.world.BlockType;

/**
 * Pig mob - passive, drops porkchop.
 */
public class Pig extends Mob {

    private static final float WIDTH = 0.9f;
    private static final float HEIGHT = 0.9f;
    private static final float MAX_HEALTH = 10.0f;

    public Pig() {
        super(WIDTH, HEIGHT, MAX_HEALTH);
        this.hostile = false;
        this.burnsInSunlight = false;
        this.moveSpeed = 0.1f;
        this.experienceValue = 1;

        setupAI();
    }

    private void setupAI() {
        ai.addGoal(1, new PanicGoal(this, ai, 1.5f)); // Run when hurt
        ai.addGoal(7, new WanderGoal(this, ai, 8.0f, 0.6f));
    }

    @Override
    public void dropLoot() {
        // Drop 1-3 raw porkchop
        // TODO: Add RAW_PORKCHOP to BlockType
        dropItems(BlockType.DIRT, 1, 3); // Placeholder
    }

    @Override
    public String getTexturePath() {
        return "/textures/mob/pig.png";
    }

    @Override
    public MobModelType getModelType() {
        return MobModelType.QUADRUPED;
    }
}
