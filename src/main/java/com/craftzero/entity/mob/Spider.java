package com.craftzero.entity.mob;

import com.craftzero.entity.ai.*;
import com.craftzero.inventory.ItemType;
import com.craftzero.main.CombatRules;

/**
 * Spider mob - hostile at night, neutral during day, wall climbing.
 */
public class Spider extends Mob {

    private boolean wasProvoked = false;
    private final String texturePath;

    public Spider() {
        this(MobDefinition.SPIDER, MobBalance.SPIDER.width(), MobBalance.SPIDER.height(),
                MobBalance.SPIDER.maxHealth(), "/textures/mob/spider.png");
    }

    protected Spider(MobDefinition definition, float width, float height, float maxHealth, String texturePath) {
        super(width, height, maxHealth);
        this.definition = definition;
        this.texturePath = texturePath;
        this.hostile = true;
        this.burnsInSunlight = false; // Spiders don't burn
        this.moveSpeed = definition.moveSpeed(); // Faster than other mobs
        this.experienceValue = definition.experienceValue();

        setupAI();
    }

    private void setupAI() {
        ai.addGoal(2, new TargetNearestGoal(this, ai, 16.0f, true, this::canTargetPlayer));
        ai.addGoal(3, new MeleeAttackGoal(this, ai, CombatRules.EASY_SPIDER_DAMAGE, 1.5f, 1.2f));
        ai.addGoal(7, new WanderGoal(this, ai, 10.0f, 1.0f));
    }

    private boolean canTargetPlayer() {
        if (wasProvoked || world == null) {
            return true;
        }
        return !isBrightEnoughToBeNeutral();
    }

    @Override
    public void tick() {
        super.tick();

        if (dead)
            return;

        // Wall climbing: if collided horizontally, move upward
        if (collidedHorizontally && !onGround) {
            motionY = 0.2f;
        }

        // Neutral in bright light unless provoked.
        if (!wasProvoked && isBrightEnoughToBeNeutral()) {
            ai.clearMoveTarget();
        }
    }

    private boolean isBrightEnoughToBeNeutral() {
        if (world == null) {
            return false;
        }
        int blockX = (int) Math.floor(x);
        int blockY = (int) Math.floor(y + height * 0.5f);
        int blockZ = (int) Math.floor(z);
        int skyLight = world.getSkyLight(blockX, blockY, blockZ);
        if (world.getDayCycleManager() != null) {
            skyLight = (int) (skyLight * world.getDayCycleManager().getSunBrightness());
        }
        int blockLight = world.getBlockLightIfLoaded(blockX, blockY, blockZ, 0);
        return Math.max(skyLight, blockLight) >= 12;
    }

    @Override
    protected void onHurt(float amount, com.craftzero.entity.Entity source) {
        super.onHurt(amount, source);
        // Once hit, always hostile
        wasProvoked = true;
    }

    @Override
    public void dropLoot() {
        dropItems(ItemType.STRING, 0, 2);
    }

    @Override
    public String getTexturePath() {
        return texturePath;
    }

    @Override
    public MobModelType getModelType() {
        return MobModelType.SPIDER;
    }
}
