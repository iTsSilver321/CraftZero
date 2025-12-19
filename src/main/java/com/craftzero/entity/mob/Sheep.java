package com.craftzero.entity.mob;

import com.craftzero.entity.ai.*;
import com.craftzero.world.BlockType;

/**
 * Sheep mob - passive, drops wool.
 */
public class Sheep extends Mob {

    private static final float WIDTH = 0.9f;
    private static final float HEIGHT = 1.3f;
    private static final float MAX_HEALTH = 8.0f;

    // Wool color (0 = white)
    private int woolColor = 0;
    private boolean sheared = false;

    public Sheep() {
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
        // Drop 1 wool block
        if (!sheared) {
            // TODO: Add WHITE_WOOL when block types are expanded
            dropItem(BlockType.SNOW, 1); // Placeholder for wool
        }
    }

    /**
     * Shear the sheep.
     * 
     * @return true if successfully sheared
     */
    public boolean shear() {
        if (!sheared) {
            sheared = true;
            // Drop 1-3 wool
            // TODO: Add WHITE_WOOL when block types are expanded
            dropItems(BlockType.SNOW, 1, 3); // Placeholder for wool
            return true;
        }
        return false;
    }

    public boolean isSheared() {
        return sheared;
    }

    public int getWoolColor() {
        return woolColor;
    }

    @Override
    public String getTexturePath() {
        return "/textures/mob/sheep.png";
    }

    @Override
    public MobModelType getModelType() {
        return MobModelType.QUADRUPED;
    }
}
