package com.craftzero.entity.mob;

import com.craftzero.combat.CombatTargetResolver;
import com.craftzero.combat.DamageSource;
import com.craftzero.entity.Entity;
import com.craftzero.entity.LivingEntity;
import com.craftzero.entity.ai.EscapeGoal;
import com.craftzero.entity.ai.FollowBreedingItemGoal;
import com.craftzero.entity.ai.FollowChildGoal;
import com.craftzero.entity.ai.FollowMateGoal;
import com.craftzero.entity.ai.FollowParentGoal;
import com.craftzero.entity.ai.LookAtPlayerGoal;
import com.craftzero.entity.ai.MobAI;
import com.craftzero.entity.ai.SwimGoal;
import com.craftzero.inventory.ItemType;
import com.craftzero.main.Player;
import com.craftzero.progression.StatusEffectInstance;
import com.craftzero.progression.StatusEffectType;
import com.craftzero.world.BlockType;
import com.craftzero.world.World;
import com.craftzero.world.WorldParticle;

import java.util.List;
import java.util.Random;

/**
 * Base class for all mobs (hostile and passive).
 * Extends LivingEntity with AI and mob-specific behavior.
 */
public abstract class Mob extends LivingEntity {

    public static final int BABY_GROWING_AGE = -24000;
    public static final int BREEDING_COOLDOWN_AGE = 6000;
    public static final int LOVE_MODE_TICKS = 600;
    public static final float BREEDING_MATE_FOLLOW_SPEED = 0.2f;
    public static final float BREEDING_ITEM_FOLLOW_SPEED = 0.25f;
    public static final float PARENT_FOLLOW_SPEED = 0.25f;
    public static final int BREEDING_SPAWN_DELAY_TICKS = 60;
    public static final int BREEDING_HEART_BURST_COUNT = 7;
    private static final float BABY_RENDER_SCALE = 0.5f;
    private static final float BREEDING_MATE_SEARCH_RANGE = 8.0f;
    private static final int LOVE_PARTICLE_INTERVAL_TICKS = 10;
    private static final int DEFAULT_AMBIENT_SOUND_INTERVAL_TICKS = 80;

    protected MobAI ai;
    protected Random random;

    // Mob type flags
    protected boolean hostile;
    protected boolean burnsInSunlight;
    protected MobDefinition definition;

    // Experience dropped on death
    protected int experienceValue;

    // Despawn
    protected int despawnTimer;
    protected static final float HARD_DESPAWN_DISTANCE_SQ = 128.0f * 128.0f;
    protected static final float SOFT_DESPAWN_DISTANCE_SQ = 32.0f * 32.0f;
    protected static final int SOFT_DESPAWN_MIN_AGE = 600;
    protected static final int SOFT_DESPAWN_RANDOM_BOUND = 800;

    // Ageable mobs use a separate counter from Entity ticks existed.
    protected int growingAge;
    protected int loveTicks;
    private Mob breedingMate;
    private int breedingSpawnDelay;
    protected int ambientSoundTime;

    protected Mob(float width, float height, float maxHealth) {
        super(width, height, maxHealth);
        this.ai = new MobAI(this);
        this.random = new Random();
        this.hostile = false;
        this.burnsInSunlight = false;
        this.experienceValue = 5;
        this.despawnTimer = 0;
        this.growingAge = 0;
        this.loveTicks = 0;
        this.breedingMate = null;
        this.breedingSpawnDelay = 0;
        this.ambientSoundTime = 0;

        // Add base goals to all mobs
        ai.addGoal(0, new SwimGoal(this)); // Highest priority - don't drown
        ai.addGoal(1, new EscapeGoal(this, ai, 0.6f)); // Escape traps
        ai.addGoal(3, new FollowMateGoal(this, ai, 8.0f, BREEDING_MATE_FOLLOW_SPEED));
        ai.addGoal(4, new FollowBreedingItemGoal(this, ai, 10.0f, BREEDING_ITEM_FOLLOW_SPEED));
        ai.addGoal(5, new FollowParentGoal(this, ai, PARENT_FOLLOW_SPEED));
        ai.addGoal(6, new FollowChildGoal(this, ai, PARENT_FOLLOW_SPEED));
        ai.addGoal(8, new LookAtPlayerGoal(this, 8.0f)); // Look at nearby players when idle
    }

    @Override
    public void tick() {
        // When dead, only handle death animation - skip AI and movement
        if (dead) {
            // Store previous position for interpolation
            prevX = x;
            prevY = y;
            prevZ = z;

            // Increment death timer (handled by LivingEntity.tick() normally, but we need
            // it here)
            deathTime++;
            if (deathTime >= 20) { // Remove after 1 second
                remove();
            }
            return;
        }

        if (removeOnPeacefulDifficulty()) {
            return;
        }

        // Update AI FIRST - this sets targetYaw and forwardSpeed
        ai.tick();

        // THEN update animation - this calculates motion from targetYaw
        // CRITICAL: Must run AFTER AI so we use the CURRENT targetYaw!
        updateAnimation();

        // FINALLY call super.tick() (LivingEntity/Entity tick)
        // This captures prev positions and handles auto-jump based on CURRENT motion
        super.tick();
        tickGrowingAge();
        tickBreeding();
        tickAmbientSound();

        // Check sunlight burning
        if (burnsInSunlight) {
            checkSunlightBurn();
        }

        // Check despawn
        checkDespawn();
    }

    protected void tickWithoutAi() {
        if (dead) {
            prevX = x;
            prevY = y;
            prevZ = z;
            deathTime++;
            if (deathTime >= 20) {
                remove();
            }
            return;
        }

        if (removeOnPeacefulDifficulty()) {
            return;
        }

        super.tick();
        tickGrowingAge();
        tickBreeding();
        tickAmbientSound();

        if (burnsInSunlight) {
            checkSunlightBurn();
        }
        checkDespawn();
    }

    private boolean removeOnPeacefulDifficulty() {
        if (world == null || world.getPlayer() == null || definition == null
                || definition.category() != MobDefinition.MobCategory.MONSTER) {
            return false;
        }
        if (world.getPlayer().getDifficulty().allowsHostileSpawns()) {
            return false;
        }
        remove();
        return true;
    }

    /**
     * Check if mob should burn in sunlight.
     */
    protected void checkSunlightBurn() {
        if (world == null)
            return;

        if (world.getDayCycleManager() == null)
            return;

        if (isWetFromWaterOrRain()) {
            extinguish();
            return;
        }

        if (!world.getDayCycleManager().isDaylightBurnTime()) {
            return;
        }

        int blockX = (int) Math.floor(x);
        int blockY = (int) Math.floor(y);
        int blockZ = (int) Math.floor(z);

        if (world.canSeeSky(blockX, blockY, blockZ) && shouldIgniteFromDaylight(blockX, blockY, blockZ)) {
            setOnFire(160);
        }
    }

    private boolean shouldIgniteFromDaylight(int blockX, int blockY, int blockZ) {
        float brightness = daylightBurnBrightness(blockX, blockY, blockZ);
        return brightness > 0.5f && random.nextFloat() * 30.0f < (brightness - 0.4f) * 2.0f;
    }

    private float daylightBurnBrightness(int blockX, int blockY, int blockZ) {
        int skyLight = world.getSkyLight(blockX, blockY, blockZ);
        if (world.getDayCycleManager() != null) {
            skyLight = (int) (skyLight * world.getDayCycleManager().getSunBrightness());
        }
        int blockLight = world.getBlockLightIfLoaded(blockX, blockY, blockZ, 0);
        return Math.max(skyLight, blockLight) / 15.0f;
    }

    /**
     * Check if mob should despawn.
     */
    protected void checkDespawn() {
        if (!canDespawn() || world == null)
            return;

        float distanceSq = nearestPlayerDespawnDistanceSq();
        if (distanceSq == Float.MAX_VALUE) {
            return;
        }

        if (distanceSq > HARD_DESPAWN_DISTANCE_SQ) {
            remove();
            return;
        }

        if (distanceSq < SOFT_DESPAWN_DISTANCE_SQ) {
            despawnTimer = 0;
            return;
        }

        despawnTimer++;
        if (despawnTimer > SOFT_DESPAWN_MIN_AGE && random.nextInt(SOFT_DESPAWN_RANDOM_BOUND) == 0) {
            remove();
        }
    }

    private float nearestPlayerDespawnDistanceSq() {
        float nearest = Float.MAX_VALUE;
        Player player = world.getPlayer();
        if (player != null && player.getStats().getHealth() > 0.0f) {
            nearest = distanceSquaredTo(player.getPosition().x, player.getPosition().y, player.getPosition().z);
        }

        World.RemotePlayerTarget remoteTarget = world.nearestRemotePlayerTarget(
                x, y, z, (float) Math.sqrt(HARD_DESPAWN_DISTANCE_SQ), false);
        if (remoteTarget != null && remoteTarget.valid()) {
            nearest = Math.min(nearest, remoteTarget.distance() * remoteTarget.distance());
        }
        return nearest;
    }

    private float distanceSquaredTo(float targetX, float targetY, float targetZ) {
        float dx = targetX - x;
        float dy = targetY - y;
        float dz = targetZ - z;
        return dx * dx + dy * dy + dz * dz;
    }

    @Override
    protected void onDeath() {
        super.onDeath();
        recordPlayerKillStatistics();
        if (isBaby()) {
            return;
        }
        dropLoot();
        if (world != null && hasRecentPlayerDamage()) {
            int droppedExperience = experienceDropValue();
            if (droppedExperience > 0) {
                world.spawnExperience(x, y + getHeight() * 0.5f, z, droppedExperience);
            }
        }
    }

    private void recordPlayerKillStatistics() {
        if (world == null || world.getPlayer() == null || !hasRecentPlayerDamage()) {
            return;
        }
        Player player = world.getPlayer();
        player.getStats().getStatistics().recordMobKill(isHostile());
        if (isHostile()) {
            player.getStats().getAchievements().recordMonsterKilled();
        }
    }

    protected int experienceDropValue() {
        if (definition == null) {
            return experienceValue;
        }
        return switch (definition.category()) {
            case CREATURE, WATER_CREATURE -> 1 + random.nextInt(3);
            default -> experienceValue;
        };
    }

    @Override
    protected boolean isStatusEffectApplicable(StatusEffectInstance effect) {
        if (!super.isStatusEffectApplicable(effect) || effect == null) {
            return false;
        }
        if (definition == null) {
            return true;
        }
        if (isUndeadDefinition(definition)
                && (effect.type() == StatusEffectType.REGENERATION || effect.type() == StatusEffectType.POISON)) {
            return false;
        }
        return !isSpiderDefinition(definition) || effect.type() != StatusEffectType.POISON;
    }

    private static boolean isUndeadDefinition(MobDefinition definition) {
        return definition == MobDefinition.ZOMBIE
                || definition == MobDefinition.SKELETON
                || definition == MobDefinition.ZOMBIE_PIGMAN
                || definition == MobDefinition.GIANT;
    }

    private static boolean isSpiderDefinition(MobDefinition definition) {
        return definition == MobDefinition.SPIDER
                || definition == MobDefinition.CAVE_SPIDER;
    }

    protected void playMobHurtSound(String soundId) {
        if (health > 0.0f) {
            playMobSound(soundId);
        }
    }

    protected void playMobDeathSound(String soundId) {
        playMobSound(soundId);
    }

    protected void tickAmbientSound() {
        String soundId = getAmbientSoundId();
        if (world == null || soundId == null || soundId.isEmpty()) {
            return;
        }
        if (random.nextInt(1000) < ambientSoundTime++) {
            ambientSoundTime = -Math.max(1, getAmbientSoundIntervalTicks());
            playMobSound(soundId);
        }
    }

    protected String getAmbientSoundId() {
        return null;
    }

    protected int getAmbientSoundIntervalTicks() {
        return DEFAULT_AMBIENT_SOUND_INTERVAL_TICKS;
    }

    protected void playMobSound(String soundId) {
        if (world == null || soundId == null || soundId.isEmpty()) {
            return;
        }
        world.playSound(soundId, x, y + getHeight() * 0.5f, z, 1.0f, mobSoundPitch());
    }

    protected float mobSoundPitch() {
        return (random.nextFloat() - random.nextFloat()) * 0.2f + 1.0f;
    }

    /**
     * Drop loot when killed.
     * Override in subclasses.
     */
    @Override
    public abstract void dropLoot();

    /**
     * Spawn a dropped item at the mob's position.
     */
    protected void dropItem(ItemType type, int count) {
        if (isBaby() || world == null || type == null || count <= 0)
            return;

        // Random offset
        float ox = (random.nextFloat() - 0.5f) * 0.5f;
        float oz = (random.nextFloat() - 0.5f) * 0.5f;

        world.spawnDroppedItem(x + ox, y + 0.5f, z + oz, type, count);
    }

    /**
     * Drop items with random count.
     */
    protected void dropItems(ItemType type, int min, int max) {
        dropItems(type, min, max, Math.max(0, getRecentPlayerLootingLevel()));
    }

    protected void dropItemsWithoutLooting(ItemType type, int min, int max) {
        dropItems(type, min, max, 0);
    }

    private void dropItems(ItemType type, int min, int max, int looting) {
        if (isBaby()) {
            return;
        }
        int count = min + random.nextInt(max - min + 1);
        if (looting > 0) {
            count += random.nextInt(looting + 1);
        }
        if (count > 0) {
            dropItem(type, count);
        }
    }

    public void onSuccessfulMeleeHit(Player player) {
    }

    public void onSuccessfulRemoteMeleeHit(World.RemotePlayerTarget target) {
    }

    public boolean onMeleePursuit(Player player, float distance) {
        return false;
    }

    public boolean onRemoteMeleePursuit(World.RemotePlayerTarget target, float distance) {
        return false;
    }

    /**
     * Get the texture path for this mob.
     */
    public abstract String getTexturePath();

    /**
     * Get the model type for this mob.
     */
    public abstract MobModelType getModelType();

    // Getters
    public MobAI getAI() {
        return ai;
    }

    public Random getRandom() {
        return random;
    }

    public boolean isHostile() {
        return hostile;
    }

    public boolean burnsInSunlight() {
        return burnsInSunlight;
    }

    public int getExperienceValue() {
        return experienceValue;
    }

    public MobDefinition getDefinition() {
        return definition;
    }

    public int getGrowingAge() {
        return growingAge;
    }

    public void setGrowingAge(int growingAge) {
        int previousGrowingAge = this.growingAge;
        this.growingAge = growingAge;
        if (growingAge != 0) {
            loveTicks = 0;
            clearBreedingProgress();
        }
        if (becameAgeNeutral(previousGrowingAge, this.growingAge)) {
            clearAgeGatedMovement();
        }
    }

    public boolean isBaby() {
        return growingAge < 0;
    }

    @Override
    public float getWidth() {
        return super.getWidth() * getRenderScale();
    }

    @Override
    public float getHeight() {
        return super.getHeight() * getRenderScale();
    }

    public float getRenderScale() {
        return isBaby() ? BABY_RENDER_SCALE : 1.0f;
    }

    protected void tickGrowingAge() {
        int previousGrowingAge = growingAge;
        if (growingAge < 0) {
            growingAge++;
        } else if (growingAge > 0) {
            growingAge--;
        }
        if (becameAgeNeutral(previousGrowingAge, growingAge)) {
            clearAgeGatedMovement();
        }
    }

    private boolean becameAgeNeutral(int previousGrowingAge, int currentGrowingAge) {
        return previousGrowingAge != 0 && currentGrowingAge == 0;
    }

    private void clearAgeGatedMovement() {
        clearBreedingProgress();
        ai.stopGoals(List.of(FollowMateGoal.class, FollowBreedingItemGoal.class,
                FollowParentGoal.class, FollowChildGoal.class));
        ai.clearMoveTarget();
        stopMoving();
        motionX = 0.0f;
        motionZ = 0.0f;
    }

    protected void tickBreeding() {
        if (loveTicks <= 0 || world == null || isBaby() || growingAge != 0) {
            if (loveTicks > 0 && (isBaby() || growingAge != 0)) {
                loveTicks = 0;
            }
            clearBreedingProgress();
            return;
        }
        loveTicks--;
        if (loveTicks % LOVE_PARTICLE_INTERVAL_TICKS == 0) {
            spawnLoveParticle();
        }
        if (breedingMate == null || !canMateWith(breedingMate)) {
            breedingMate = findBreedingMate();
            breedingSpawnDelay = 0;
        }
        if (breedingMate == null) {
            return;
        }
        breedingSpawnDelay++;
        if (breedingSpawnDelay >= BREEDING_SPAWN_DELAY_TICKS) {
            breedWith(breedingMate);
            clearBreedingProgress();
        }
    }

    private void spawnLoveParticle() {
        float px = x + (random.nextFloat() * 2.0f - 1.0f) * getWidth();
        float py = y + 0.5f + random.nextFloat() * getHeight();
        float pz = z + (random.nextFloat() * 2.0f - 1.0f) * getWidth();
        float motionX = (float) (random.nextGaussian() * 0.02);
        float motionY = (float) (random.nextGaussian() * 0.02);
        float motionZ = (float) (random.nextGaussian() * 0.02);
        world.spawnParticle(WorldParticle.Type.HEART,
                px,
                py,
                pz,
                motionX,
                motionY,
                motionZ,
                0.28f,
                20);
    }

    private Mob findBreedingMate() {
        float rangeSq = BREEDING_MATE_SEARCH_RANGE * BREEDING_MATE_SEARCH_RANGE;
        for (Entity entity : world.getEntities()) {
            if (entity == this || !(entity instanceof Mob mate) || !canMateWith(mate)) {
                continue;
            }
            float dx = mate.getX() - x;
            float dy = mate.getY() - y;
            float dz = mate.getZ() - z;
            if (dx * dx + dy * dy + dz * dz <= rangeSq) {
                return mate;
            }
        }
        return null;
    }

    private void clearBreedingProgress() {
        breedingMate = null;
        breedingSpawnDelay = 0;
    }

    protected boolean canMateWith(Mob mate) {
        return mate != null
                && mate.getClass() == getClass()
                && mate.loveTicks > 0
                && mate.growingAge == 0
                && !mate.isBaby()
                && !mate.isRemoved()
                && !mate.isDead()
                && isBreedingCompatible(mate);
    }

    public boolean canSeekBreedingMate(Mob mate) {
        return loveTicks > 0
                && growingAge == 0
                && !isBaby()
                && !isRemoved()
                && !isDead()
                && canMateWith(mate);
    }

    public boolean canFollowParent(Mob parent) {
        return isBaby()
                && parent != null
                && parent.getClass() == getClass()
                && !parent.isBaby()
                && !parent.isRemoved()
                && !parent.isDead()
                && isBreedingCompatible(parent);
    }

    public boolean canFollowChild(Mob child) {
        return growingAge > 0
                && !isBaby()
                && child != null
                && child.getClass() == getClass()
                && child.isBaby()
                && !child.isRemoved()
                && !child.isDead()
                && isBreedingCompatible(child);
    }

    private void breedWith(Mob mate) {
        Mob child = createBreedingChild(mate);
        if (child == null) {
            return;
        }
        loveTicks = 0;
        mate.loveTicks = 0;
        setGrowingAge(BREEDING_COOLDOWN_AGE);
        mate.setGrowingAge(BREEDING_COOLDOWN_AGE);

        child.setGrowingAge(BABY_GROWING_AGE);
        child.setPosition(x, y, z);
        child.setRenderBodyYaw(getYaw());
        child.setPitch(getPitch());
        world.spawnEntity(child);
        spawnBreedingHeartBurst();
    }

    private void spawnBreedingHeartBurst() {
        for (int i = 0; i < BREEDING_HEART_BURST_COUNT; i++) {
            float px = x + (random.nextFloat() * 2.0f - 1.0f) * getWidth();
            float py = y + 0.5f + random.nextFloat() * getHeight();
            float pz = z + (random.nextFloat() * 2.0f - 1.0f) * getWidth();
            float motionX = (float) (random.nextGaussian() * 0.02);
            float motionY = (float) (random.nextGaussian() * 0.02);
            float motionZ = (float) (random.nextGaussian() * 0.02);
            world.spawnParticle(WorldParticle.Type.HEART, px, py, pz, motionX, motionY, motionZ, 0.28f, 20);
        }
    }

    public boolean feedBreedingItem(ItemType itemType) {
        if (!isBreedingItem(itemType) || isBaby() || growingAge != 0 || isRemoved() || isDead()) {
            return false;
        }
        clearBreedingProgress();
        loveTicks = LOVE_MODE_TICKS;
        if (world != null) {
            spawnBreedingHeartBurst();
        }
        return true;
    }

    protected boolean isBreedingItem(ItemType itemType) {
        return false;
    }

    public boolean isTemptedByItem(ItemType itemType) {
        return isBreedingItem(itemType);
    }

    public boolean canFollowBreedingItem() {
        return growingAge == 0 && loveTicks <= 0 && !isBaby() && !isRemoved() && !isDead();
    }

    protected boolean isBreedingCompatible(Mob mate) {
        return false;
    }

    protected Mob createBreedingChild(Mob mate) {
        return null;
    }

    public boolean isInLove() {
        return loveTicks > 0;
    }

    public int getLoveTicks() {
        return loveTicks;
    }

    public void setLoveTicks(int loveTicks) {
        this.loveTicks = Math.max(0, loveTicks);
        if (this.loveTicks == 0) {
            clearBreedingProgress();
        }
    }

    protected boolean canDespawn() {
        if (definition == null) {
            return false;
        }
        return switch (definition.category()) {
            case MONSTER, WATER_CREATURE -> true;
            default -> false;
        };
    }

    @Override
    protected void onHurt(float amount, Entity source) {
        if (loveTicks > 0) {
            loveTicks = 0;
            clearBreedingProgress();
        }
        assignRetaliationTarget(retaliationDamageSource(source));
        super.onHurt(amount, source);
    }

    private DamageSource retaliationDamageSource(Entity source) {
        DamageSource details = getLastDamageDetails();
        if (details != null && details.entity() == source) {
            return details;
        }
        return source == null ? null : DamageSource.entity(DamageSource.Type.GENERIC, source);
    }

    private void assignRetaliationTarget(DamageSource source) {
        if (!hostile || source == null) {
            return;
        }
        String remotePlayerId = CombatTargetResolver.remotePlayerId(source);
        if (!remotePlayerId.isBlank() && world != null) {
            World.RemotePlayerTarget target = world.remotePlayerTargetById(remotePlayerId);
            if (target != null && target.valid()) {
                ai.setRemotePlayerTarget(target);
                ai.setMoveTarget(target.x(), target.y(), target.z());
                return;
            }
        }

        LivingEntity attacker = retaliationTargetFromDamageSource(source);
        if (attacker == null) {
            return;
        }
        ai.setTarget(attacker);
        ai.setMoveTarget(attacker.getX(), attacker.getY(), attacker.getZ());
    }

    private LivingEntity retaliationTargetFromDamageSource(DamageSource source) {
        if (!hostile || source == null) {
            return null;
        }
        LivingEntity living = CombatTargetResolver.livingAttacker(source);
        if (canRetaliateAgainst(living)) {
            return living;
        }
        return null;
    }

    protected LivingEntity retaliationTargetFrom(Entity source) {
        if (!hostile || source == null) {
            return null;
        }
        LivingEntity living = CombatTargetResolver.livingAttacker(source);
        if (canRetaliateAgainst(living)) {
            return living;
        }
        return null;
    }

    protected boolean canRetaliateAgainst(LivingEntity attacker) {
        return attacker != null
                && attacker != this
                && !attacker.isDead()
                && !attacker.isRemoved();
    }

    /**
     * Enum for different model types.
     */
    public enum MobModelType {
        HUMANOID, // Zombie (thick limbs)
        SKELETON, // Skeleton (thin limbs)
        CREEPER, // No arms
        SPIDER, // 8 legs
        QUADRUPED, // Pig, Cow, Sheep
        CHICKEN, // Small with wings
        SLIME,
        SQUID,
        ENDERMAN,
        SILVERFISH,
        GHAST,
        BLAZE,
        WOLF,
        SNOW_GOLEM,
        VILLAGER,
        DRAGON
    }
}
