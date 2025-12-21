package com.craftzero.entity.ai;

import com.craftzero.entity.LivingEntity;
import com.craftzero.world.BlockType;
import com.craftzero.world.World;

/**
 * AI Goal: Swim when in water.
 * Highest priority - mobs should always float and swim.
 * Prevents drowning and allows navigation through water.
 */
public class SwimGoal implements Goal {

    private final LivingEntity mob;

    public SwimGoal(LivingEntity mob) {
        this.mob = mob;
    }

    @Override
    public int getPriority() {
        return 0; // Highest priority - swimming overrides everything
    }

    @Override
    public boolean canUse() {
        // Activate when in water
        return mob.isInWater();
    }

    @Override
    public boolean canContinue() {
        return mob.isInWater();
    }

    @Override
    public void start() {
        // Nothing special to do on start
    }

    @Override
    public void tick() {
        World world = mob.getWorld();
        if (world == null)
            return;

        // Check if head is submerged
        int headY = (int) Math.floor(mob.getY() + mob.getHeight() * 0.85f);
        BlockType headBlock = world.getBlock(
                (int) Math.floor(mob.getX()),
                headY,
                (int) Math.floor(mob.getZ()));

        boolean headUnderwater = (headBlock == BlockType.WATER);

        if (headUnderwater) {
            // Swim up to surface
            mob.addMotion(0, 0.04f, 0);
        }

        // Random swimming motion (slight side-to-side)
        if (mob.getTicksExisted() % 20 == 0) {
            float swimDir = (float) (Math.random() - 0.5f) * 0.02f;
            mob.addMotion(swimDir, 0, swimDir);
        }
    }

    @Override
    public void stop() {
        // Nothing to clean up
    }

    @Override
    public boolean isExclusive() {
        return false; // Swimming doesn't prevent other goals entirely
    }
}
