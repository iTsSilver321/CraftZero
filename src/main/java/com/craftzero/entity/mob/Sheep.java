package com.craftzero.entity.mob;

import com.craftzero.entity.ai.*;
import com.craftzero.inventory.ItemType;

/**
 * Sheep mob - passive, drops wool.
 */
public class Sheep extends Mob {

    private static final MobBalance.Spec SPEC = MobBalance.SHEEP;

    // Wool color (0 = white)
    private int woolColor = 0;
    private boolean sheared = false;

    public Sheep() {
        super(SPEC.width(), SPEC.height(), SPEC.maxHealth());
        this.definition = MobDefinition.SHEEP;
        this.hostile = SPEC.hostile();
        this.burnsInSunlight = SPEC.burnsInSunlight();
        this.moveSpeed = SPEC.moveSpeed();
        this.experienceValue = SPEC.experienceValue();

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
            dropItem(ItemType.WHITE_WOOL, 1);
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
            dropItems(ItemType.WHITE_WOOL, 1, 3);
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
