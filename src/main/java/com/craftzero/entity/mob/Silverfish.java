package com.craftzero.entity.mob;

import com.craftzero.entity.ai.MeleeAttackGoal;
import com.craftzero.entity.ai.TargetNearestGoal;
import com.craftzero.entity.ai.WanderGoal;
import com.craftzero.main.CombatRules;
import com.craftzero.world.BlockType;
import com.craftzero.world.WorldSoundEvent;

public class Silverfish extends Mob {
    private static final int WAKE_HORIZONTAL_RANGE = 5;
    private static final int WAKE_VERTICAL_RANGE = 10;

    private static final int[][] HIDE_DIRECTIONS = {
            { 0, -1, 0 },
            { 0, 1, 0 },
            { 0, 0, -1 },
            { 0, 0, 1 },
            { 1, 0, 0 },
            { -1, 0, 0 }
    };

    public Silverfish() {
        super(MobDefinition.SILVERFISH.width(), MobDefinition.SILVERFISH.height(),
                MobDefinition.SILVERFISH.maxHealth());
        this.definition = MobDefinition.SILVERFISH;
        this.hostile = true;
        this.moveSpeed = MobDefinition.SILVERFISH.moveSpeed();
        this.experienceValue = MobDefinition.SILVERFISH.experienceValue();
        ai.addGoal(2, new TargetNearestGoal(this, ai, 16.0f));
        ai.addGoal(3, new MeleeAttackGoal(this, ai, CombatRules.EASY_ZOMBIE_DAMAGE * 0.5f, 1.0f, 1.2f));
        ai.addGoal(7, new WanderGoal(this, ai, 8.0f, 0.9f));
    }

    @Override
    public void tick() {
        super.tick();
        tryHideInNearbyBlock();
    }

    private void tryHideInNearbyBlock() {
        if (world == null || dead || removed || ai.hasMoveTarget() || ai.isNavigating()) {
            return;
        }
        int[] direction = HIDE_DIRECTIONS[random.nextInt(HIDE_DIRECTIONS.length)];
        int blockX = (int) Math.floor(x) + direction[0];
        int blockY = (int) Math.floor(y + 0.5f) + direction[1];
        int blockZ = (int) Math.floor(z) + direction[2];
        BlockType block = world.getBlockIfLoaded(blockX, blockY, blockZ, BlockType.AIR);
        int infestedMetadata = infestedMetadataForHost(block);
        if (infestedMetadata < 0) {
            return;
        }
        if (world.setBlockIfLoaded(blockX, blockY, blockZ, BlockType.INFESTED_STONE, infestedMetadata)) {
            remove();
        }
    }

    private static int infestedMetadataForHost(BlockType block) {
        return switch (block) {
            case STONE -> 0;
            case COBBLESTONE -> 1;
            case STONE_BRICK -> 2;
            default -> -1;
        };
    }

    @Override
    protected void onHurt(float amount, com.craftzero.entity.Entity source) {
        super.onHurt(amount, source);
        playMobHurtSound(WorldSoundEvent.SILVERFISH_HURT);
        wakeNearbyInfestedBlocks();
    }

    private void wakeNearbyInfestedBlocks() {
        if (world == null) {
            return;
        }
        int cx = (int) Math.floor(x);
        int cy = (int) Math.floor(y);
        int cz = (int) Math.floor(z);
        for (int xIndex = 0; xIndex <= WAKE_HORIZONTAL_RANGE * 2; xIndex++) {
            int dx = sourceSearchOffset(xIndex);
            for (int yIndex = 0; yIndex <= WAKE_VERTICAL_RANGE * 2; yIndex++) {
                int dy = sourceSearchOffset(yIndex);
                for (int zIndex = 0; zIndex <= WAKE_HORIZONTAL_RANGE * 2; zIndex++) {
                    int dz = sourceSearchOffset(zIndex);
                    int bx = cx + dx;
                    int by = cy + dy;
                    int bz = cz + dz;
                    if (world.getBlockIfLoaded(bx, by, bz, BlockType.AIR) == BlockType.INFESTED_STONE) {
                        world.breakBlock(bx, by, bz, false);
                    }
                }
            }
        }
    }

    private static int sourceSearchOffset(int index) {
        if (index == 0) {
            return 0;
        }
        int magnitude = (index + 1) / 2;
        return (index & 1) == 1 ? magnitude : -magnitude;
    }

    @Override
    public void dropLoot() {
    }

    @Override
    protected void onDeath() {
        playMobDeathSound(WorldSoundEvent.SILVERFISH_DEATH);
        super.onDeath();
    }

    @Override
    protected String getAmbientSoundId() {
        return WorldSoundEvent.SILVERFISH_IDLE;
    }

    @Override
    public String getTexturePath() {
        return "/textures/mob/silverfish.png";
    }

    @Override
    public MobModelType getModelType() {
        return MobModelType.SILVERFISH;
    }
}
