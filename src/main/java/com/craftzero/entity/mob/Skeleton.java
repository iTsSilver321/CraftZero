package com.craftzero.entity.mob;

import com.craftzero.entity.Entity;
import com.craftzero.entity.ai.*;
import com.craftzero.inventory.ItemType;
import com.craftzero.world.WorldSoundEvent;

/**
 * Skeleton mob - hostile, ranged attack, burns in sunlight.
 */
public class Skeleton extends Mob {

    private static final MobBalance.Spec SPEC = MobBalance.SKELETON;
    private static final float SPIDER_JOCKEY_Y_OFFSET_SCALE = 0.75f;
    private final RangedAttackGoal rangedAttackGoal;
    private Spider ridingSpider;

    public Skeleton() {
        super(SPEC.width(), SPEC.height(), SPEC.maxHealth());
        this.definition = MobDefinition.SKELETON;
        this.hostile = SPEC.hostile();
        this.burnsInSunlight = SPEC.burnsInSunlight();
        this.moveSpeed = SPEC.moveSpeed();
        this.experienceValue = SPEC.experienceValue();
        this.rangedAttackGoal = RangedAttackGoal.releaseOneSkeleton(this, ai);

        setupAI();
    }

    private void setupAI() {
        // Skeletons prefer ranged combat - stay at distance
        ai.addGoal(2, new TargetNearestGoal(this, ai, 16.0f));
        ai.addGoal(3, rangedAttackGoal);
        ai.addGoal(7, new WanderGoal(this, ai, 10.0f, 0.8f));
    }

    public RangedAttackGoal.State getRangedAttackState() {
        return rangedAttackGoal.getState();
    }

    public boolean isRangedAttackActive() {
        return ai.isGoalActive(rangedAttackGoal) || rangedAttackGoal.hasRestoredActiveState();
    }

    public void restoreRangedAttackState(RangedAttackGoal.State state, boolean activeAtSave) {
        rangedAttackGoal.restoreState(state, activeAtSave);
    }

    public boolean mountSpider(Spider spider) {
        if (spider == null || spider.isRemoved() || spider.isDead() || ridingSpider != null) {
            return false;
        }
        ridingSpider = spider;
        syncRidingSpiderPosition(spider);
        return true;
    }

    public Spider getRidingSpider() {
        if (ridingSpider != null && (ridingSpider.isRemoved() || ridingSpider.isDead())) {
            ridingSpider = null;
        }
        return ridingSpider;
    }

    void clearRidingSpider(Spider spider) {
        if (ridingSpider == spider) {
            ridingSpider = null;
        }
    }

    void syncRidingSpiderPosition(Spider spider) {
        if (spider == null) {
            return;
        }
        prevX = x;
        prevY = y;
        prevZ = z;
        prevYaw = yaw;
        prevPitch = pitch;
        x = spider.getX();
        y = spider.getY() + spider.getHeight() * SPIDER_JOCKEY_Y_OFFSET_SCALE;
        z = spider.getZ();
        yaw = spider.getYaw();
        motionX = 0.0f;
        motionY = 0.0f;
        motionZ = 0.0f;
        fallStartY = y;
        falling = false;
        onGround = spider.isOnGround();
    }

    @Override
    public void tick() {
        Spider spider = getRidingSpider();
        if (spider == null) {
            super.tick();
            return;
        }
        ai.tick();
        tickWithoutAi();
        stopMoving();
        updateHeadLook();
        syncRidingSpiderPosition(spider);
    }

    @Override
    public void updatePhysics(float deltaTime) {
        Spider spider = getRidingSpider();
        if (spider == null) {
            super.updatePhysics(deltaTime);
            return;
        }
        syncRidingSpiderPosition(spider);
    }

    @Override
    public void dropLoot() {
        dropItems(ItemType.BONE, 0, 2);
        dropItems(ItemType.ARROW, 0, 2);
    }

    @Override
    protected void onHurt(float amount, Entity source) {
        super.onHurt(amount, source);
        playMobHurtSound(WorldSoundEvent.SKELETON_HURT);
    }

    @Override
    protected void onDeath() {
        playMobDeathSound(WorldSoundEvent.SKELETON_DEATH);
        super.onDeath();
    }

    @Override
    protected String getAmbientSoundId() {
        return WorldSoundEvent.SKELETON_IDLE;
    }

    @Override
    public String getTexturePath() {
        return "/textures/mob/skeleton.png";
    }

    @Override
    public MobModelType getModelType() {
        return MobModelType.SKELETON;
    }
}
