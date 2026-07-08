package com.craftzero.entity.mob;

import com.craftzero.entity.ai.MeleeAttackGoal;
import com.craftzero.entity.ai.TargetNearestGoal;
import com.craftzero.entity.ai.WanderGoal;

/**
 * Release-era Giant zombie entity. Giants are factory/spawner/save backed but
 * intentionally absent from natural spawn tables.
 */
public class Giant extends Mob {
    private static final float BASE_WIDTH = 0.6f;
    private static final float BASE_HEIGHT = 1.8f;
    private static final float GIANT_SCALE = 6.0f;
    public static final float ATTACK_DAMAGE = 50.0f;
    private static final float ATTACK_RANGE = 2.5f;
    private static final float TARGET_RANGE = 16.0f;

    public Giant() {
        super(BASE_WIDTH, BASE_HEIGHT, MobDefinition.GIANT.maxHealth());
        this.definition = MobDefinition.GIANT;
        this.hostile = MobDefinition.GIANT.hostile();
        this.burnsInSunlight = MobDefinition.GIANT.burnsInSunlight();
        this.moveSpeed = MobDefinition.GIANT.moveSpeed();
        this.experienceValue = MobDefinition.GIANT.experienceValue();

        setupAI();
    }

    private void setupAI() {
        ai.addGoal(2, new TargetNearestGoal(this, ai, TARGET_RANGE));
        ai.addGoal(3, new MeleeAttackGoal(this, ai, ATTACK_DAMAGE, ATTACK_RANGE, 1.0f));
        ai.addGoal(7, new WanderGoal(this, ai, 10.0f, 0.8f));
    }

    @Override
    public void dropLoot() {
        // The legacy Giant entity did not define ordinary zombie drops.
    }

    @Override
    public float getRenderScale() {
        return GIANT_SCALE * super.getRenderScale();
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
