package com.craftzero.entity.mob;

import com.craftzero.combat.CombatTargetResolver;
import com.craftzero.combat.DamageSource;
import com.craftzero.entity.Entity;
import com.craftzero.entity.ai.*;
import com.craftzero.inventory.ItemType;
import com.craftzero.main.CombatRules;
import com.craftzero.main.Player;
import com.craftzero.world.World;
import com.craftzero.world.WorldSoundEvent;

/**
 * Spider mob - hostile at night, neutral during day, wall climbing.
 */
public class Spider extends Mob {
    private static final float NEUTRAL_BRIGHTNESS_THRESHOLD = 0.5f;
    private static final float LEAP_MIN_DISTANCE = 2.0f;
    private static final float LEAP_MAX_DISTANCE = 6.0f;
    private static final int LEAP_RANDOM_BOUND = 10;
    private static final float LEAP_HORIZONTAL_SPEED = 0.5f * 0.8f;
    private static final float LEAP_EXISTING_MOTION_SCALE = 0.2f;
    private static final float LEAP_VERTICAL_MOTION = 0.4f;

    private boolean wasProvoked = false;
    private final String texturePath;
    private boolean pendingLeap;
    private float pendingLeapX;
    private float pendingLeapZ;
    private Skeleton jockeyRider;

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
        if (ai.hasMoveTarget()) {
            return true;
        }
        return !isBrightEnoughToBeNeutral();
    }

    @Override
    public void tick() {
        super.tick();

        if (dead || isRemoved()) {
            clearJockeyRider();
            return;
        }

        syncJockeyRider();

        // Neutral spiders in bright light only drop an existing chase on the old
        // 1-in-100 interest-loss roll.
        if (!wasProvoked && ai.hasMoveTarget() && localBrightness() > NEUTRAL_BRIGHTNESS_THRESHOLD
                && random.nextInt(100) == 0) {
            ai.clearMoveTarget();
        }
    }

    public boolean mountJockey(Skeleton skeleton) {
        if (skeleton == null || skeleton.isRemoved() || skeleton.isDead() || jockeyRider != null) {
            return false;
        }
        jockeyRider = skeleton;
        skeleton.mountSpider(this);
        syncJockeyRider();
        return true;
    }

    public Skeleton getJockeyRider() {
        if (jockeyRider != null && (jockeyRider.isRemoved() || jockeyRider.isDead())) {
            jockeyRider = null;
        }
        return jockeyRider;
    }

    private void syncJockeyRider() {
        Skeleton rider = getJockeyRider();
        if (rider != null) {
            rider.syncRidingSpiderPosition(this);
        }
    }

    private void clearJockeyRider() {
        if (jockeyRider != null) {
            jockeyRider.clearRidingSpider(this);
            jockeyRider = null;
        }
    }

    @Override
    public void remove() {
        clearJockeyRider();
        super.remove();
    }

    private boolean isBrightEnoughToBeNeutral() {
        return localBrightness() >= NEUTRAL_BRIGHTNESS_THRESHOLD;
    }

    private float localBrightness() {
        if (world == null) {
            return 0.0f;
        }
        int blockX = (int) Math.floor(x);
        int blockY = (int) Math.floor(y + getHeight() * 0.5f);
        int blockZ = (int) Math.floor(z);
        int skyLight = world.getSkyLight(blockX, blockY, blockZ);
        if (world.getDayCycleManager() != null) {
            skyLight = (int) (skyLight * world.getDayCycleManager().getSunBrightness());
        }
        int blockLight = world.getBlockLightIfLoaded(blockX, blockY, blockZ, 0);
        return Math.max(skyLight, blockLight) / 15.0f;
    }

    @Override
    public boolean onMeleePursuit(Player player, float distance) {
        if (player == null || distance <= LEAP_MIN_DISTANCE || distance >= LEAP_MAX_DISTANCE || !isOnGround()) {
            return false;
        }
        return queueLeapToward(player.getPosition().x, player.getPosition().z);
    }

    @Override
    public boolean onRemoteMeleePursuit(World.RemotePlayerTarget target, float distance) {
        if (target == null || !target.valid()
                || distance <= LEAP_MIN_DISTANCE || distance >= LEAP_MAX_DISTANCE || !isOnGround()) {
            return false;
        }
        return queueLeapToward(target.x(), target.z());
    }

    private boolean queueLeapToward(float targetX, float targetZ) {
        if (random.nextInt(LEAP_RANDOM_BOUND) != 0) {
            return false;
        }
        float dx = targetX - x;
        float dz = targetZ - z;
        float horizontal = (float) Math.sqrt(dx * dx + dz * dz);
        if (horizontal < 0.0001f) {
            return false;
        }
        pendingLeapX = dx / horizontal * LEAP_HORIZONTAL_SPEED + motionX * LEAP_EXISTING_MOTION_SCALE;
        pendingLeapZ = dz / horizontal * LEAP_HORIZONTAL_SPEED + motionZ * LEAP_EXISTING_MOTION_SCALE;
        pendingLeap = true;
        return true;
    }

    @Override
    protected void updateAnimation() {
        super.updateAnimation();
        if (pendingLeap) {
            motionX = pendingLeapX;
            motionY = LEAP_VERTICAL_MOTION;
            motionZ = pendingLeapZ;
            pendingLeap = false;
        }
    }

    @Override
    protected void onHurt(float amount, Entity source) {
        super.onHurt(amount, source);
        playMobHurtSound(WorldSoundEvent.SPIDER_HURT);
        if (source != null) {
            // Once attacked by an entity, stay hostile even in bright light.
            wasProvoked = true;
        }
    }

    @Override
    public boolean damage(float amount, DamageSource source) {
        boolean playerAggression = isPlayerAggression(source);
        boolean applied = super.damage(amount, source);
        if (applied && playerAggression) {
            wasProvoked = true;
        }
        return applied;
    }

    private boolean isPlayerAggression(DamageSource source) {
        return CombatTargetResolver.isPlayerAggression(source);
    }

    @Override
    protected void onDeath() {
        playMobDeathSound(WorldSoundEvent.SPIDER_DEATH);
        super.onDeath();
    }

    @Override
    protected String getAmbientSoundId() {
        return WorldSoundEvent.SPIDER_IDLE;
    }

    public boolean isProvoked() {
        return wasProvoked;
    }

    @Override
    protected boolean isTouchingClimbableBlock() {
        return collidedHorizontally || super.isTouchingClimbableBlock();
    }

    public void setProvoked(boolean provoked) {
        this.wasProvoked = provoked;
    }

    @Override
    public void dropLoot() {
        dropItems(ItemType.STRING, 0, 2);
        dropRareSpiderEye();
    }

    private void dropRareSpiderEye() {
        if (!hasRecentPlayerDamage()) {
            return;
        }
        int looting = Math.max(0, getRecentPlayerLootingLevel());
        if (random.nextInt(3) == 0 || random.nextInt(1 + looting) > 0) {
            dropItem(ItemType.SPIDER_EYE, 1);
        }
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
