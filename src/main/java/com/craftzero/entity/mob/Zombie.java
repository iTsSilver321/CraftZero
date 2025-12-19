package com.craftzero.entity.mob;

import com.craftzero.entity.ai.*;
import com.craftzero.world.BlockType;

/**
 * Zombie mob - hostile humanoid that burns in sunlight.
 */
public class Zombie extends Mob {

    private static final float WIDTH = 0.6f;
    private static final float HEIGHT = 1.95f;
    private static final float MAX_HEALTH = 20.0f;

    public Zombie() {
        super(WIDTH, HEIGHT, MAX_HEALTH);
        this.hostile = true;
        this.burnsInSunlight = true;
        this.moveSpeed = 0.15f;
        this.experienceValue = 5;

        setupAI();
    }

    private void setupAI() {
        // Priority 1: Panic on fire (highest)
        // Priority 3: Attack target
        ai.addGoal(3, new MeleeAttackGoal(this, ai, 3.0f, 1.5f, 1.0f));
        // Priority 2: Find target
        ai.addGoal(2, new TargetNearestGoal(this, ai, 16.0f));
        // Priority 7: Wander (lowest)
        ai.addGoal(7, new WanderGoal(this, ai, 10.0f, 0.8f));
    }

    @Override
    public void dropLoot() {
        // Drop 0-2 rotten flesh
        // For now, we'll drop the placeholder item
        // TODO: Add ROTTEN_FLESH to BlockType
        dropItems(BlockType.DIRT, 0, 2); // Placeholder
    }

    @Override
    public String getTexturePath() {
        return "/textures/mob/zombie.png";
    }

    @Override
    public MobModelType getModelType() {
        return MobModelType.HUMANOID;
    }
}
