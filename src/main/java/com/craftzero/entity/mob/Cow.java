package com.craftzero.entity.mob;

import com.craftzero.entity.ai.*;
import com.craftzero.world.BlockType;

/**
 * Cow mob - passive, drops beef and leather.
 */
public class Cow extends Mob {

    private static final float WIDTH = 0.9f;
    private static final float HEIGHT = 1.4f;
    private static final float MAX_HEALTH = 10.0f;

    public Cow() {
        super(WIDTH, HEIGHT, MAX_HEALTH);
        this.hostile = false;
        this.burnsInSunlight = false;
        this.moveSpeed = 0.1f;
        this.experienceValue = 1;

        setupAI();
    }

    private void setupAI() {
        ai.addGoal(1, new PanicGoal(this, ai, 1.5f));
        ai.addGoal(7, new WanderGoal(this, ai, 8.0f, 0.6f));
    }

    @Override
    public void dropLoot() {
        // Drop 1-3 raw beef
        dropItems(BlockType.DIRT, 1, 3); // Placeholder for RAW_BEEF
        // Drop 0-2 leather
        dropItems(BlockType.DIRT, 0, 2); // Placeholder for LEATHER
    }

    @Override
    public String getTexturePath() {
        return "/textures/mob/cow.png";
    }

    @Override
    public MobModelType getModelType() {
        return MobModelType.QUADRUPED;
    }
}
