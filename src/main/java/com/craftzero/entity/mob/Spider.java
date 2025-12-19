package com.craftzero.entity.mob;

import com.craftzero.entity.ai.*;
import com.craftzero.world.BlockType;

/**
 * Spider mob - hostile at night, neutral during day, wall climbing.
 */
public class Spider extends Mob {

    private static final float WIDTH = 1.4f; // Wide body
    private static final float HEIGHT = 0.9f;
    private static final float MAX_HEALTH = 16.0f;

    private boolean wasProvoked = false;

    public Spider() {
        super(WIDTH, HEIGHT, MAX_HEALTH);
        this.hostile = true;
        this.burnsInSunlight = false; // Spiders don't burn
        this.moveSpeed = 0.2f; // Faster than other mobs
        this.experienceValue = 5;

        setupAI();
    }

    private void setupAI() {
        ai.addGoal(2, new TargetNearestGoal(this, ai, 16.0f));
        ai.addGoal(3, new MeleeAttackGoal(this, ai, 2.0f, 1.5f, 1.2f));
        ai.addGoal(7, new WanderGoal(this, ai, 10.0f, 1.0f));
    }

    @Override
    public void tick() {
        super.tick();

        if (dead)
            return;

        // Wall climbing: if collided horizontally, move upward
        if (collidedHorizontally && !onGround) {
            motionY = 0.2f;
        }

        // Neutral during day unless provoked
        if (world != null && world.getDayCycleManager() != null) {
            float time = world.getDayCycleManager().getTime();
            boolean isDay = time >= 0 && time < 12000;

            if (isDay && !wasProvoked) {
                // Clear targeting during day if not provoked
                ai.clearMoveTarget();
            }
        }
    }

    @Override
    protected void onHurt(float amount, com.craftzero.entity.Entity source) {
        super.onHurt(amount, source);
        // Once hit, always hostile
        wasProvoked = true;
    }

    @Override
    public void dropLoot() {
        // Drop 0-2 string
        // TODO: Add STRING item when item system is expanded
        dropItems(BlockType.STONE, 0, 2); // Placeholder for string
    }

    @Override
    public String getTexturePath() {
        return "/textures/mob/spider.png";
    }

    @Override
    public MobModelType getModelType() {
        return MobModelType.SPIDER;
    }
}
