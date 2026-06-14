package com.craftzero.entity.mob;

import com.craftzero.entity.ai.*;
import com.craftzero.inventory.ItemType;
import com.craftzero.main.CombatRules;

/**
 * Zombie mob - hostile humanoid that burns in sunlight.
 */
public class Zombie extends Mob {

    private static final MobBalance.Spec SPEC = MobBalance.ZOMBIE;

    public Zombie() {
        super(SPEC.width(), SPEC.height(), SPEC.maxHealth());
        this.definition = MobDefinition.ZOMBIE;
        this.hostile = SPEC.hostile();
        this.burnsInSunlight = SPEC.burnsInSunlight();
        this.moveSpeed = SPEC.moveSpeed();
        this.experienceValue = SPEC.experienceValue();

        setupAI();
    }

    private void setupAI() {
        // Priority 1: Panic on fire (highest)
        // Priority 3: Attack target
        ai.addGoal(3, new MeleeAttackGoal(this, ai, CombatRules.EASY_ZOMBIE_DAMAGE, 1.5f, 1.0f));
        // Priority 2: Find target
        ai.addGoal(2, new TargetNearestGoal(this, ai, 16.0f));
        // Priority 7: Wander (lowest)
        ai.addGoal(7, new WanderGoal(this, ai, 10.0f, 0.8f));
    }

    @Override
    public void dropLoot() {
        dropItems(ItemType.ROTTEN_FLESH, 0, 2);
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
