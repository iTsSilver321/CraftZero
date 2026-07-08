package com.craftzero.entity.mob;

import com.craftzero.combat.CombatTargetResolver;
import com.craftzero.combat.DamageSource;
import com.craftzero.entity.Entity;
import com.craftzero.entity.LivingEntity;
import com.craftzero.entity.ai.FollowOwnerGoal;
import com.craftzero.entity.ai.MeleeAttackGoal;
import com.craftzero.entity.ai.WanderGoal;
import com.craftzero.entity.ai.WolfAttackTargetGoal;
import com.craftzero.entity.ai.WolfHuntSheepGoal;
import com.craftzero.inventory.ItemStack;
import com.craftzero.inventory.ItemType;
import com.craftzero.main.Player;
import com.craftzero.world.World;
import com.craftzero.world.WorldParticle;
import com.craftzero.world.WorldSoundEvent;

import java.util.Random;

/**
 * Wolf mob - passive Release 1.0 forest/taiga creature population support.
 */
public class Wolf extends Mob {

    private static final MobBalance.Spec SPEC = MobBalance.WOLF;
    private static final float WILD_MAX_HEALTH = SPEC.maxHealth();
    private static final float TAMED_MAX_HEALTH = 20.0f;
    public static final float WILD_ATTACK_DAMAGE = 2.0f;
    public static final float TAMED_ATTACK_DAMAGE = 4.0f;
    public static final float ASSIST_RANGE = 32.0f;
    private static final float ANGER_ALERT_HORIZONTAL_RANGE = 16.0f;
    private static final float ANGER_ALERT_VERTICAL_RANGE = 10.0f;
    private static final float BEG_RANGE = 8.0f;
    private static final int TAME_PARTICLE_TICKS = 20;
    private static final float SHAKE_TIMER_STEP = 0.05f;
    private static final float SHAKE_DURATION = 2.0f;
    private static final float SHAKE_ANGLE_DURATION = 1.8f;
    private static final float SHAKE_PARTICLE_START = 0.4f;
    private static final float SHAKE_PARTICLE_AMOUNT = 7.0f;
    private static final float MAX_SAVED_SHAKE_TIME = SHAKE_DURATION + SHAKE_TIMER_STEP;
    private static final float WHINE_HEALTH_THRESHOLD = 10.0f;

    private boolean angry;
    private String angryRemotePlayerId = "";
    private boolean tamed;
    private boolean sitting;
    private String ownerName;
    private boolean begging;
    private TameParticle tameParticle = TameParticle.NONE;
    private int tameParticleTicks;
    private boolean wet;
    private boolean shaking;
    private float shakeTime;
    private float prevShakeTime;

    public enum TameParticle {
        NONE,
        HEART,
        SMOKE
    }

    public Wolf() {
        super(SPEC.width(), SPEC.height(), SPEC.maxHealth());
        this.definition = MobDefinition.WOLF;
        this.hostile = SPEC.hostile();
        this.burnsInSunlight = SPEC.burnsInSunlight();
        this.moveSpeed = SPEC.moveSpeed();
        this.experienceValue = SPEC.experienceValue();

        setupAI();
    }

    private void setupAI() {
        ai.addGoal(3, new MeleeAttackGoal(this, ai, WILD_ATTACK_DAMAGE, 1.5f, 1.2f, this::isAngry));
        ai.addGoal(3, new WolfAttackTargetGoal(this, ai));
        ai.addGoal(4, new WolfHuntSheepGoal(this, ai));
        ai.addGoal(6, new FollowOwnerGoal(this, ai));
        ai.addGoal(7, new WanderGoal(this, ai, 8.0f, 0.8f));
    }

    @Override
    public void tick() {
        updateBeggingState();
        tickTameParticles();
        if (angry) {
            updateAngryPlayerTarget();
        }
        if (sitting) {
            ai.stopNavigation();
            stopMoving();
            tickWithoutAi();
            return;
        }
        super.tick();
    }

    @Override
    public void updatePhysics(float deltaTime) {
        super.updatePhysics(deltaTime);
        updateWetShakeState();
    }

    @Override
    public void dropLoot() {
        // Wolves do not drop items in Release 1.0.
    }

    @Override
    protected void onHurt(float amount, Entity source) {
        super.onHurt(amount, source);
        playMobHurtSound(WorldSoundEvent.WOLF_HURT);
    }

    @Override
    protected void onDeath() {
        playMobDeathSound(WorldSoundEvent.WOLF_DEATH);
        super.onDeath();
    }

    @Override
    protected void tickAmbientSound() {
        if (world == null) {
            return;
        }
        if (random.nextInt(1000) < ambientSoundTime++) {
            ambientSoundTime = -Math.max(1, getAmbientSoundIntervalTicks());
            playMobSound(wolfAmbientSoundId());
        }
    }

    private String wolfAmbientSoundId() {
        if (angry) {
            return WorldSoundEvent.WOLF_GROWL;
        }
        if (random.nextInt(3) == 0) {
            return tamed && getHealth() < WHINE_HEALTH_THRESHOLD
                    ? WorldSoundEvent.WOLF_WHINE
                    : WorldSoundEvent.WOLF_PANTING;
        }
        return WorldSoundEvent.WOLF_BARK;
    }

    @Override
    public boolean damage(float amount, DamageSource source) {
        boolean applied = super.damage(amount, source);
        if (!applied || getHealth() <= 0.0f) {
            return applied;
        }
        if (tamed) {
            setSitting(false);
            String remoteAggressorId = remoteAggressorId(source);
            LivingEntity retaliationTarget = remoteAggressorId.isBlank() ? retaliationTargetFrom(source) : null;
            if (!remoteAggressorId.isBlank()) {
                setAssistRemotePlayerTarget(remoteAggressorId);
            } else if (retaliationTarget != null) {
                setAssistTarget(retaliationTarget);
            }
        } else if (isPlayerAggression(source)) {
            becomeAngryAtPlayer(remoteAggressorId(source), true);
        }
        return applied;
    }

    @Override
    public String getTexturePath() {
        if (tamed) {
            return "/textures/mob/wolf_tame.png";
        }
        if (angry) {
            return "/textures/mob/wolf_angry.png";
        }
        return "/textures/mob/wolf.png";
    }

    @Override
    public MobModelType getModelType() {
        return MobModelType.WOLF;
    }

    public boolean isAngry() {
        return angry;
    }

    public void setAngry(boolean angry) {
        this.angry = angry;
        if (!angry) {
            angryRemotePlayerId = "";
            ai.clearMoveTarget();
            ai.clearRemotePlayerTarget();
        }
    }

    public boolean isTamed() {
        return tamed;
    }

    public void setTamed(boolean tamed) {
        this.tamed = tamed;
        this.maxHealth = tamed ? TAMED_MAX_HEALTH : WILD_MAX_HEALTH;
        if (health > maxHealth) {
            health = maxHealth;
        }
        if (!tamed) {
            ownerName = null;
            sitting = false;
        }
    }

    public boolean isSitting() {
        return sitting;
    }

    public void setSitting(boolean sitting) {
        this.sitting = sitting && tamed;
        if (this.sitting) {
            ai.stopNavigation();
            stopMoving();
        }
    }

    public boolean toggleSitting() {
        if (!tamed) {
            return false;
        }
        setSitting(!sitting);
        return true;
    }

    public boolean isBegging() {
        return begging;
    }

    public TameParticle getTameParticle() {
        return tameParticle;
    }

    public int getTameParticleTicks() {
        return tameParticleTicks;
    }

    public boolean isWet() {
        return wet;
    }

    public boolean isShaking() {
        return shaking;
    }

    public float getShakeTime() {
        return shakeTime;
    }

    public float getPrevShakeTime() {
        return prevShakeTime;
    }

    public void setWetShakeState(boolean wet, boolean shaking, float shakeTime, float prevShakeTime) {
        this.wet = wet;
        this.shaking = wet && shaking;
        this.shakeTime = clampShakeTime(shakeTime);
        this.prevShakeTime = clampShakeTime(prevShakeTime);
        if (!this.wet) {
            this.shaking = false;
            this.shakeTime = 0.0f;
            this.prevShakeTime = 0.0f;
        } else if (!this.shaking) {
            this.shakeTime = 0.0f;
            this.prevShakeTime = 0.0f;
        } else if (this.prevShakeTime > this.shakeTime) {
            this.prevShakeTime = this.shakeTime;
        }
    }

    public static boolean isValidSavedShakeTime(float value) {
        return Float.isFinite(value) && value >= 0.0f && value <= MAX_SAVED_SHAKE_TIME;
    }

    public float getShakeAngle(float partialTick, float timeOffset) {
        if (!wet && !shaking) {
            return 0.0f;
        }
        float tick = Math.max(0.0f, Math.min(1.0f, partialTick));
        float progress = (prevShakeTime + (shakeTime - prevShakeTime) * tick + timeOffset)
                / SHAKE_ANGLE_DURATION;
        if (progress < 0.0f) {
            return 0.0f;
        }
        progress = Math.min(1.0f, progress);
        return (float) (Math.sin(progress * Math.PI)
                * Math.sin(progress * Math.PI * 11.0f)
                * 0.15f * Math.PI);
    }

    public String getOwnerName() {
        return ownerName;
    }

    public void setOwnerName(String ownerName) {
        String cleaned = ownerName == null ? "" : ownerName.trim();
        this.ownerName = cleaned.isEmpty() ? null : cleaned;
    }

    public boolean hasOwner() {
        return ownerName != null && !ownerName.isBlank();
    }

    public boolean isOwnedBy(Player player) {
        return player != null && isOwnedByName(player.getPlayerName());
    }

    public boolean isOwnedByName(String username) {
        return tamed
                && username != null
                && !username.isBlank()
                && hasOwner()
                && ownerName.equalsIgnoreCase(username.trim());
    }

    public boolean canAssistCombat() {
        return tamed && hasOwner() && !sitting && !isDead() && !isRemoved();
    }

    public boolean canHuntSheep() {
        return !tamed && !angry && !isDead() && !isRemoved();
    }

    public boolean setAssistTarget(LivingEntity target) {
        if (!canAssistCombat() || !canAttackTarget(target)) {
            return false;
        }
        ai.setTarget(target);
        ai.setMoveTarget(target.getX(), target.getY(), target.getZ());
        return true;
    }

    public boolean setAssistRemotePlayerTarget(String playerId) {
        if (!canAssistCombat() || world == null || playerId == null || playerId.isBlank()) {
            return false;
        }
        World.RemotePlayerTarget target = world.remotePlayerTargetById(playerId);
        if (target == null || !target.valid()) {
            return false;
        }
        ai.setRemotePlayerTarget(target);
        ai.setMoveTarget(target.x(), target.y(), target.z());
        return true;
    }

    public LivingEntity getAssistTarget() {
        return ai.getTarget();
    }

    private boolean canAttackTarget(LivingEntity target) {
        return target != null
                && target != this
                && !target.isDead()
                && !target.isRemoved()
                && !(target instanceof Creeper)
                && !(target instanceof Ghast);
    }

    private boolean isPlayerAggression(DamageSource source) {
        return CombatTargetResolver.isPlayerAggression(source);
    }

    private String remoteAggressorId(DamageSource source) {
        return CombatTargetResolver.remotePlayerId(source);
    }

    private LivingEntity retaliationTargetFrom(DamageSource source) {
        LivingEntity living = CombatTargetResolver.livingAttacker(source);
        if (canAttackTarget(living)) {
            return living;
        }
        return null;
    }

    private void becomeAngryAtPlayer(boolean alertPack) {
        becomeAngryAtPlayer("", alertPack);
    }

    private void becomeAngryAtPlayer(String remotePlayerId, boolean alertPack) {
        angry = true;
        angryRemotePlayerId = remotePlayerId == null ? "" : remotePlayerId.trim();
        updateAngryPlayerTarget();
        if (alertPack) {
            alertNearbyWildWolves(angryRemotePlayerId);
        }
    }

    private void alertNearbyWildWolves(String remotePlayerId) {
        if (world == null) {
            return;
        }
        for (Entity entity : world.getEntities()) {
            if (!(entity instanceof Wolf wolf) || wolf == this || !wolf.canAcceptPackAngerFrom(this)) {
                continue;
            }
            wolf.becomeAngryAtPlayer(remotePlayerId, false);
        }
    }

    private boolean canAcceptPackAngerFrom(Wolf source) {
        if (source == null || tamed || angry || isDead() || isRemoved() || getHealth() <= 0.0f) {
            return false;
        }
        float dx = Math.abs(getX() - source.getX());
        float dy = Math.abs(getY() - source.getY());
        float dz = Math.abs(getZ() - source.getZ());
        return dx <= ANGER_ALERT_HORIZONTAL_RANGE
                && dy <= ANGER_ALERT_VERTICAL_RANGE
                && dz <= ANGER_ALERT_HORIZONTAL_RANGE;
    }

    private void updateAngryPlayerTarget() {
        if (world == null) {
            ai.clearMoveTarget();
            ai.clearRemotePlayerTarget();
            return;
        }
        if (!angryRemotePlayerId.isBlank()) {
            World.RemotePlayerTarget target = world.remotePlayerTargetById(angryRemotePlayerId);
            if (target != null && target.valid()) {
                ai.setRemotePlayerTarget(target);
                ai.setMoveTarget(target.x(), target.y(), target.z());
                return;
            }
        }
        Player player = world.getPlayer();
        if (player == null || player.getStats().getHealth() <= 0 || player.isCreative()
                || !player.getDifficulty().allowsHostileSpawns()) {
            ai.clearMoveTarget();
            ai.clearRemotePlayerTarget();
            return;
        }
        ai.clearRemotePlayerTarget();
        ai.setMoveTarget(player.getPosition().x, player.getPosition().y, player.getPosition().z);
    }

    public boolean canAcceptBone() {
        return !tamed && !angry;
    }

    public boolean tryTameWithBone(Random random) {
        if (!canAcceptBone()) {
            return false;
        }
        Random source = random == null ? this.random : random;
        if (source.nextInt(3) == 0) {
            setTamed(true);
            setAngry(false);
            setSitting(true);
            setHealth(getMaxHealth());
            startTameParticles(TameParticle.HEART);
            return true;
        }
        startTameParticles(TameParticle.SMOKE);
        return false;
    }

    public boolean canEatMeat(ItemType itemType) {
        return tamed && isWolfMeat(itemType) && getHealth() < getMaxHealth();
    }

    public boolean feedMeat(ItemType itemType) {
        if (!canEatMeat(itemType)) {
            return false;
        }
        heal(wolfMeatHealAmount(itemType));
        return true;
    }

    public static boolean isWolfMeat(ItemType itemType) {
        return itemType == ItemType.RAW_PORKCHOP
                || itemType == ItemType.COOKED_PORKCHOP
                || itemType == ItemType.RAW_BEEF
                || itemType == ItemType.STEAK
                || itemType == ItemType.RAW_CHICKEN
                || itemType == ItemType.COOKED_CHICKEN
                || itemType == ItemType.ROTTEN_FLESH;
    }

    private static float wolfMeatHealAmount(ItemType itemType) {
        return switch (itemType) {
            case COOKED_PORKCHOP, STEAK -> 8.0f;
            case COOKED_CHICKEN -> 6.0f;
            case ROTTEN_FLESH -> 4.0f;
            case RAW_PORKCHOP, RAW_BEEF -> 3.0f;
            case RAW_CHICKEN -> 2.0f;
            default -> 0.0f;
        };
    }

    private void updateBeggingState() {
        begging = false;
        if (world == null || isDead() || isRemoved()) {
            return;
        }
        float bestDistanceSq = BEG_RANGE * BEG_RANGE;
        float lookX = 0.0f;
        float lookY = 0.0f;
        float lookZ = 0.0f;
        Player player = world.getPlayer();
        if (player != null) {
            ItemStack held = player.getInventory().getItemInHand();
            if (held != null && !held.isEmpty() && isBeggingItem(player.getPlayerName(), held.getType())) {
                float dx = player.getPosition().x - getX();
                float dy = player.getPosition().y - getY();
                float dz = player.getPosition().z - getZ();
                float distanceSq = dx * dx + dy * dy + dz * dz;
                if (distanceSq <= bestDistanceSq) {
                    bestDistanceSq = distanceSq;
                    lookX = player.getPosition().x;
                    lookY = player.getPosition().y + 1.4f;
                    lookZ = player.getPosition().z;
                    begging = true;
                }
            }
        }
        for (World.RemotePlayerTarget target : world.remotePlayerViews(
                getX(), getY() + getHeight() * 0.5f, getZ(), BEG_RANGE, false)) {
            if (target == null || !target.valid()
                    || target.heldItem() == null
                    || !isBeggingItem(target.username(), target.heldItem())) {
                continue;
            }
            float distanceSq = target.distance() * target.distance();
            if (distanceSq <= bestDistanceSq) {
                bestDistanceSq = distanceSq;
                lookX = target.x();
                lookY = target.eyeY();
                lookZ = target.z();
                begging = true;
            }
        }
        if (begging) {
            lookAt(lookX, lookY, lookZ);
        }
    }

    private boolean isBeggingItem(String username, ItemType itemType) {
        if (!tamed) {
            return !angry && itemType == ItemType.BONE;
        }
        return isOwnedByName(username) && isWolfMeat(itemType);
    }

    private void startTameParticles(TameParticle particle) {
        tameParticle = particle == null ? TameParticle.NONE : particle;
        tameParticleTicks = tameParticle == TameParticle.NONE ? 0 : TAME_PARTICLE_TICKS;
        if (world != null && tameParticle != TameParticle.NONE) {
            WorldParticle.Type type = tameParticle == TameParticle.HEART
                    ? WorldParticle.Type.HEART
                    : WorldParticle.Type.SMOKE;
            spawnTameParticleBurst(type);
        }
    }

    private void spawnTameParticleBurst(WorldParticle.Type type) {
        for (int i = 0; i < 7; i++) {
            float px = x + (random.nextFloat() * 2.0f - 1.0f) * getWidth();
            float py = y + 0.5f + random.nextFloat() * getHeight();
            float pz = z + (random.nextFloat() * 2.0f - 1.0f) * getWidth();
            float motionX = (float) (random.nextGaussian() * 0.02);
            float motionY = (float) (random.nextGaussian() * 0.02);
            float motionZ = (float) (random.nextGaussian() * 0.02);
            world.spawnParticle(type, px, py, pz, motionX, motionY, motionZ, 0.28f, 20);
        }
    }

    private void tickTameParticles() {
        if (tameParticleTicks > 0) {
            tameParticleTicks--;
            if (tameParticleTicks == 0) {
                tameParticle = TameParticle.NONE;
            }
        }
    }

    private void updateWetShakeState() {
        if (world == null || isDead() || isRemoved()) {
            return;
        }
        if (isInWater()) {
            wet = true;
            shaking = false;
            shakeTime = 0.0f;
            prevShakeTime = 0.0f;
            return;
        }
        if (!wet) {
            shaking = false;
            shakeTime = 0.0f;
            prevShakeTime = 0.0f;
            return;
        }
        if (!shaking && canStartShakingOnGround() && !ai.hasMoveTarget()) {
            shaking = true;
            shakeTime = 0.0f;
            prevShakeTime = 0.0f;
            playMobSound(WorldSoundEvent.WOLF_SHAKE);
        }
        if (!shaking) {
            return;
        }
        prevShakeTime = shakeTime;
        shakeTime += SHAKE_TIMER_STEP;
        if (prevShakeTime >= SHAKE_DURATION) {
            wet = false;
            shaking = false;
            shakeTime = 0.0f;
            prevShakeTime = 0.0f;
            return;
        }
        spawnShakeParticles();
    }

    private boolean canStartShakingOnGround() {
        return isOnGround() || getBlockBelowFeet().isSolid();
    }

    private void spawnShakeParticles() {
        if (shakeTime <= SHAKE_PARTICLE_START || world == null) {
            return;
        }
        float particleWave = (float) Math.sin((shakeTime - SHAKE_PARTICLE_START) * Math.PI);
        int amount = Math.max(0, (int) (particleWave * SHAKE_PARTICLE_AMOUNT));
        for (int i = 0; i < amount; i++) {
            float px = getX() + (random.nextFloat() * 2.0f - 1.0f) * getWidth() * 0.5f;
            float py = getY() + 0.8f + random.nextFloat() * getHeight() * 0.15f;
            float pz = getZ() + (random.nextFloat() * 2.0f - 1.0f) * getWidth() * 0.5f;
            world.spawnParticle(WorldParticle.Type.SPLASH, px, py, pz,
                    getMotionX(), getMotionY(), getMotionZ(), 0.16f + random.nextFloat() * 0.08f, 8);
        }
    }

    private static float clampShakeTime(float value) {
        if (!Float.isFinite(value)) {
            return 0.0f;
        }
        return Math.max(0.0f, Math.min(MAX_SAVED_SHAKE_TIME, value));
    }
}
