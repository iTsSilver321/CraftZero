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
        BlockType headBlock = world.getBlockIfLoaded(
                (int) Math.floor(mob.getX()),
                headY,
                (int) Math.floor(mob.getZ()),
                BlockType.AIR);

        boolean headUnderwater = headBlock.isWater();

        if (headUnderwater && mob.getMotionY() < 0.12f) {
            // Small upward intent only when the head is actually underwater.
            // Horizontal steering stays with the active navigation/goal so mobs do
            // not fight themselves at the water surface.
            mob.addMotion(0, 0.045f, 0);
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
