package com.craftzero.entity.ai;

import com.craftzero.entity.mob.Sheep;
import com.craftzero.world.BlockType;
import com.craftzero.world.World;

/**
 * Release-era sheep grazing: pause, eat tall grass or grass blocks, then notify
 * the sheep so wool/growth state can update.
 */
public class EatGrassGoal implements Goal {
    public static final int EATING_TICKS = 40;

    private static final int ADULT_GRAZE_CHANCE = 1000;
    private static final int CHILD_GRAZE_CHANCE = 50;

    private final Sheep sheep;
    private final MobAI ai;
    private int eatingTimer;
    private boolean consumedGrass;

    public EatGrassGoal(Sheep sheep, MobAI ai) {
        this.sheep = sheep;
        this.ai = ai;
    }

    @Override
    public int getPriority() {
        return 2;
    }

    @Override
    public boolean canUse() {
        World world = sheep.getWorld();
        if (world == null || sheep.isDead() || sheep.isRemoved()) {
            return false;
        }
        int chance = sheep.isBaby() ? CHILD_GRAZE_CHANCE : ADULT_GRAZE_CHANCE;
        return sheep.getRandom().nextInt(chance) == 0 && hasGrassToEat(world);
    }

    @Override
    public boolean canContinue() {
        return eatingTimer > 0;
    }

    @Override
    public void start() {
        eatingTimer = EATING_TICKS;
        consumedGrass = false;
        sheep.setEatingGrassTimer(eatingTimer);
        ai.requestStopMoving();
    }

    @Override
    public void tick() {
        ai.requestStopMoving();
        if (!consumedGrass && eatingTimer == 4) {
            consumedGrass = consumeGrass();
            if (consumedGrass) {
                sheep.onAteGrass();
            }
        }
        eatingTimer--;
        sheep.setEatingGrassTimer(eatingTimer);
    }

    @Override
    public void stop() {
        eatingTimer = 0;
        sheep.setEatingGrassTimer(0);
        ai.requestStopMoving();
    }

    private boolean hasGrassToEat(World world) {
        int blockX = (int) Math.floor(sheep.getX());
        int blockY = (int) Math.floor(sheep.getY());
        int blockZ = (int) Math.floor(sheep.getZ());
        return world.getBlockIfLoaded(blockX, blockY, blockZ, BlockType.AIR) == BlockType.TALL_GRASS
                || world.getBlockIfLoaded(blockX, blockY - 1, blockZ, BlockType.AIR) == BlockType.GRASS;
    }

    private boolean consumeGrass() {
        World world = sheep.getWorld();
        if (world == null) {
            return false;
        }

        int blockX = (int) Math.floor(sheep.getX());
        int blockY = (int) Math.floor(sheep.getY());
        int blockZ = (int) Math.floor(sheep.getZ());

        if (world.getBlockIfLoaded(blockX, blockY, blockZ, BlockType.AIR) == BlockType.TALL_GRASS) {
            return world.setBlockIfLoaded(blockX, blockY, blockZ, BlockType.AIR, 0);
        }
        if (world.getBlockIfLoaded(blockX, blockY - 1, blockZ, BlockType.AIR) == BlockType.GRASS) {
            return world.setBlockIfLoaded(blockX, blockY - 1, blockZ, BlockType.DIRT, 0);
        }
        return false;
    }
}
