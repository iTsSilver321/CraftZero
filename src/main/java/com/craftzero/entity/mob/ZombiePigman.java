package com.craftzero.entity.mob;

import com.craftzero.entity.Entity;
import com.craftzero.entity.ai.MeleeAttackGoal;
import com.craftzero.entity.ai.TargetNearestGoal;
import com.craftzero.entity.ai.WanderGoal;
import com.craftzero.inventory.ItemType;
import com.craftzero.main.CombatRules;

public class ZombiePigman extends Mob {
    private int angerTicks;

    public ZombiePigman() {
        super(MobDefinition.ZOMBIE_PIGMAN.width(), MobDefinition.ZOMBIE_PIGMAN.height(),
                MobDefinition.ZOMBIE_PIGMAN.maxHealth());
        this.definition = MobDefinition.ZOMBIE_PIGMAN;
        this.hostile = true;
        this.moveSpeed = MobDefinition.ZOMBIE_PIGMAN.moveSpeed();
        this.experienceValue = MobDefinition.ZOMBIE_PIGMAN.experienceValue();
        ai.addGoal(2, new TargetNearestGoal(this, ai, 32.0f, true, () -> angerTicks > 0));
        ai.addGoal(3, new MeleeAttackGoal(this, ai, CombatRules.EASY_ZOMBIE_DAMAGE, 1.5f, 1.1f));
        ai.addGoal(7, new WanderGoal(this, ai, 12.0f, 0.75f));
    }

    @Override
    public void tick() {
        if (angerTicks > 0) {
            angerTicks--;
        }
        super.tick();
    }

    @Override
    protected void onHurt(float amount, Entity source) {
        angerTicks = 400 + random.nextInt(400);
        if (world == null) {
            return;
        }
        for (Entity entity : world.getEntities()) {
            if (entity instanceof ZombiePigman pigman && pigman != this && distanceToSquared(pigman) <= 32.0f * 32.0f) {
                pigman.angerTicks = Math.max(pigman.angerTicks, angerTicks);
            }
        }
    }

    @Override
    public void setOnFire(int ticks) {
        extinguish();
    }

    @Override
    public void dropLoot() {
        dropItems(ItemType.ROTTEN_FLESH, 0, 1);
        dropItems(ItemType.GOLD_NUGGET, 0, 1);
    }

    @Override
    public String getTexturePath() {
        return "/textures/mob/pigzombie.png";
    }

    @Override
    public MobModelType getModelType() {
        return MobModelType.HUMANOID;
    }

    public int getAngerTicks() {
        return angerTicks;
    }

    public void setAngerTicks(int angerTicks) {
        this.angerTicks = Math.max(0, angerTicks);
    }
}
