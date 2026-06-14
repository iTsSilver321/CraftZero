package com.craftzero.entity.mob;

import com.craftzero.entity.ai.MeleeAttackGoal;
import com.craftzero.entity.ai.TargetNearestGoal;
import com.craftzero.entity.ai.WanderGoal;
import com.craftzero.main.CombatRules;
import com.craftzero.world.BlockType;

public class Silverfish extends Mob {
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
    protected void onHurt(float amount, com.craftzero.entity.Entity source) {
        wakeNearbyInfestedBlocks();
    }

    private void wakeNearbyInfestedBlocks() {
        if (world == null) {
            return;
        }
        int cx = (int) Math.floor(x);
        int cy = (int) Math.floor(y);
        int cz = (int) Math.floor(z);
        for (int dx = -5; dx <= 5; dx++) {
            for (int dy = -2; dy <= 2; dy++) {
                for (int dz = -5; dz <= 5; dz++) {
                    int bx = cx + dx;
                    int by = cy + dy;
                    int bz = cz + dz;
                    if (world.getBlockIfLoaded(bx, by, bz, BlockType.AIR) == BlockType.INFESTED_STONE) {
                        world.setBlock(bx, by, bz, BlockType.STONE);
                        Silverfish fish = new Silverfish();
                        fish.setPosition(bx + 0.5f, by + 0.1f, bz + 0.5f);
                        world.spawnEntity(fish);
                    }
                }
            }
        }
    }

    @Override
    public void dropLoot() {
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
