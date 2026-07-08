package com.craftzero.entity.mob;

import com.craftzero.combat.DamageSource;
import com.craftzero.entity.Entity;
import com.craftzero.entity.LivingEntity;
import com.craftzero.entity.ai.LineOfSightUtil;
import com.craftzero.entity.ai.TargetNearestGoal;
import com.craftzero.entity.ai.WanderGoal;
import com.craftzero.inventory.ItemType;
import com.craftzero.main.CombatRules;
import com.craftzero.main.Player;
import com.craftzero.world.BlockType;
import com.craftzero.world.World;
import com.craftzero.world.WorldParticle;
import com.craftzero.world.WorldSoundEvent;

public class Blaze extends Mob {
    private static final float RANGED_ATTACK_RANGE = 32.0f;
    private static final float CLOSE_ATTACK_RANGE = 2.0f;
    private static final float EASY_MELEE_DAMAGE = 4.0f;
    private static final int BURST_SHOTS = 3;
    private static final int BURST_INTERVAL_TICKS = 20;
    private static final int CHARGED_AFTER_TICKS = 10;
    private static final int POST_BURST_COOLDOWN_TICKS = 100;
    private static final int MELEE_COOLDOWN_TICKS = 20;
    private static final float SMALL_FIREBALL_SPEED = 0.45f;
    private static final int CHARGE_FLAME_PARTICLES = 8;
    private static final int CHARGE_SMOKE_PARTICLES = 4;
    private static final int SHOT_FLAME_PARTICLES = 6;
    private static final int AMBIENT_LARGE_SMOKE_PARTICLES = 2;
    private static final float AMBIENT_LARGE_SMOKE_SCALE = 0.30f;
    private static final int AMBIENT_LARGE_SMOKE_LIFETIME_TICKS = 22;

    private int attackCooldown = 60;
    private int burstShots;
    private int burstCooldown;

    public Blaze() {
        super(MobDefinition.BLAZE.width(), MobDefinition.BLAZE.height(), MobDefinition.BLAZE.maxHealth());
        this.definition = MobDefinition.BLAZE;
        this.hostile = true;
        this.moveSpeed = MobDefinition.BLAZE.moveSpeed();
        this.experienceValue = MobDefinition.BLAZE.experienceValue();
        ai.addGoal(2, new TargetNearestGoal(this, ai, 32.0f));
        ai.addGoal(7, new WanderGoal(this, ai, 10.0f, 0.6f));
    }

    @Override
    public void tick() {
        applyWaterDamage();
        LivingEntity livingTarget = ai.getTarget();
        if (tryAttackLivingTarget(livingTarget)) {
            finishTickWithAmbientSmoke();
            return;
        }
        Player player = world != null ? world.getPlayer() : null;
        World.RemotePlayerTarget remoteTarget = world != null
                ? world.nearestRemotePlayerTarget(x, y + getHeight() * 0.5f, z, RANGED_ATTACK_RANGE, true)
                : null;
        if (tryAttackNearestPlayerTarget(player, remoteTarget)) {
            finishTickWithAmbientSmoke();
            return;
        }
        cancelBurst();
        tickAttackCooldown();
        finishTickWithAmbientSmoke();
    }

    private boolean tryAttackNearestPlayerTarget(Player player, World.RemotePlayerTarget remoteTarget) {
        boolean localValid = player != null && !player.isCreative() && player.getDifficulty().allowsHostileSpawns();
        float localDistSq = localValid ? distanceSquaredToPlayer(player) : Float.MAX_VALUE;
        boolean remoteValid = remoteTarget != null && remoteTarget.valid();
        float remoteDistSq = remoteValid ? remoteTarget.distance() * remoteTarget.distance() : Float.MAX_VALUE;

        if (remoteValid && (!localValid || remoteDistSq <= localDistSq)) {
            return tryAttackRemotePlayerTarget(remoteTarget, remoteDistSq);
        }
        if (localValid) {
            float dx = player.getPosition().x - x;
            float dy = player.getPosition().y + 1.0f - (y + getHeight() * 0.5f);
            float dz = player.getPosition().z - z;
            float distSq = dx * dx + dy * dy + dz * dz;
            if (tryCloseRangeMelee(player, distSq)) {
                return true;
            }
            if (distSq < RANGED_ATTACK_RANGE * RANGED_ATTACK_RANGE
                    && LineOfSightUtil.hasLineOfSight(world, x, y + getHeight() * 0.5f, z,
                            player.getPosition().x, player.getPosition().y + 1.0f, player.getPosition().z)) {
                lookAt(player.getPosition().x, player.getPosition().y + 1.0f, player.getPosition().z);
                motionY += player.getPosition().y + 1.0f > y + getHeight() * 0.5f ? 0.02f : -0.01f;
                handleBurst(player);
                return true;
            }
        }
        return false;
    }

    @Override
    protected void tickWithoutAi() {
        super.tickWithoutAi();
        if (!isDead() && !isRemoved()) {
            emitAmbientSmokeParticles();
        }
    }

    private void finishTickWithAmbientSmoke() {
        super.tick();
        if (!isDead() && !isRemoved()) {
            emitAmbientSmokeParticles();
        }
    }

    private void emitAmbientSmokeParticles() {
        if (world == null) {
            return;
        }
        for (int i = 0; i < AMBIENT_LARGE_SMOKE_PARTICLES; i++) {
            float px = x + (random.nextFloat() - 0.5f) * getWidth();
            float py = y + random.nextFloat() * getHeight();
            float pz = z + (random.nextFloat() - 0.5f) * getWidth();
            world.spawnParticle(WorldParticle.Type.LARGE_SMOKE, px, py, pz,
                    0.0f, 0.0f, 0.0f,
                    AMBIENT_LARGE_SMOKE_SCALE, AMBIENT_LARGE_SMOKE_LIFETIME_TICKS);
        }
    }

    private boolean tryAttackLivingTarget(LivingEntity target) {
        if (target == null || target.isDead() || target.isRemoved() || world == null) {
            return false;
        }
        float targetX = target.getX();
        float targetY = target.getY() + target.getHeight() * 0.5f;
        float targetZ = target.getZ();
        float dx = targetX - x;
        float dy = targetY - (y + getHeight() * 0.5f);
        float dz = targetZ - z;
        float distSq = dx * dx + dy * dy + dz * dz;
        if (tryCloseRangeMelee(target, distSq)) {
            return true;
        }
        if (distSq >= RANGED_ATTACK_RANGE * RANGED_ATTACK_RANGE
                || !LineOfSightUtil.hasLineOfSight(world, x, y + getHeight() * 0.5f, z,
                        targetX, targetY, targetZ)) {
            cancelBurst();
            return false;
        }
        lookAt(targetX, targetY, targetZ);
        motionY += targetY > y + getHeight() * 0.5f ? 0.02f : -0.01f;
        handleBurst(target);
        return true;
    }

    private boolean tryAttackRemotePlayerTarget(World.RemotePlayerTarget target, float distSq) {
        if (target == null || !target.valid()) {
            return false;
        }
        if (tryCloseRangeMelee(target, distSq)) {
            return true;
        }
        if (distSq >= RANGED_ATTACK_RANGE * RANGED_ATTACK_RANGE
                || !LineOfSightUtil.hasLineOfSight(world, x, y + getHeight() * 0.5f, z,
                        target.x(), target.eyeY(), target.z())) {
            return false;
        }
        lookAt(target.x(), target.eyeY(), target.z());
        motionY += target.eyeY() > y + getHeight() * 0.5f ? 0.02f : -0.01f;
        handleBurstAt(target.x(), target.eyeY(), target.z());
        return true;
    }

    private void applyWaterDamage() {
        if (world == null || (!isTouchingWater() && !isExposedToRain())) {
            return;
        }
        damage(1.0f, DamageSource.point(DamageSource.Type.DROWN, x, y, z, 0.0f, 0.0f));
    }

    private boolean isTouchingWater() {
        int blockX = (int) Math.floor(x);
        int blockZ = (int) Math.floor(z);
        int feetY = (int) Math.floor(y + 0.1f);
        int headY = (int) Math.floor(y + getHeight() * 0.85f);
        return world.getBlockIfLoaded(blockX, feetY, blockZ, BlockType.AIR).isWater()
                || world.getBlockIfLoaded(blockX, headY, blockZ, BlockType.AIR).isWater();
    }

    private boolean isExposedToRain() {
        int blockX = (int) Math.floor(x);
        int blockY = (int) Math.floor(y + getHeight());
        int blockZ = (int) Math.floor(z);
        return world.isRainingAt(blockX, blockY, blockZ);
    }

    private void handleBurst(Player player) {
        handleBurstAt(player.getPosition().x, player.getPosition().y + 1.0f, player.getPosition().z);
    }

    private void handleBurst(LivingEntity target) {
        handleBurstAt(target.getX(), target.getY() + target.getHeight() * 0.5f, target.getZ());
    }

    private void handleBurstAt(float targetX, float targetY, float targetZ) {
        if (burstShots > 0) {
            if (--burstCooldown <= 0) {
                shootSmallFireballAt(targetX, targetY, targetZ);
                burstShots--;
                burstCooldown = burstShots > 0 ? BURST_INTERVAL_TICKS : 0;
                if (burstShots == 0) {
                    attackCooldown = POST_BURST_COOLDOWN_TICKS;
                }
            } else if (burstShots == BURST_SHOTS && burstCooldown == CHARGED_AFTER_TICKS) {
                spawnChargeParticles();
            }
            return;
        }
        if (attackCooldown > 0) {
            attackCooldown--;
            return;
        }
        burstShots = BURST_SHOTS;
        burstCooldown = BURST_INTERVAL_TICKS;
    }

    private boolean tryCloseRangeMelee(Player player, float distSq) {
        if (burstShots > 0 || attackCooldown > 0 || distSq >= CLOSE_ATTACK_RANGE * CLOSE_ATTACK_RANGE
                || !verticallyOverlapsPlayer(player)) {
            return false;
        }
        attackCooldown = MELEE_COOLDOWN_TICKS;
        player.hurt(EASY_MELEE_DAMAGE, DamageSource.entity(DamageSource.Type.MOB_MELEE, this,
                CombatRules.MOB_MELEE_HORIZONTAL_KNOCKBACK,
                CombatRules.MOB_MELEE_VERTICAL_KNOCKBACK));
        performAttack();
        return true;
    }

    private boolean tryCloseRangeMelee(LivingEntity target, float distSq) {
        if (burstShots > 0 || attackCooldown > 0 || distSq >= CLOSE_ATTACK_RANGE * CLOSE_ATTACK_RANGE
                || !verticallyOverlaps(target)) {
            return false;
        }
        attackCooldown = MELEE_COOLDOWN_TICKS;
        target.damage(EASY_MELEE_DAMAGE, DamageSource.entity(DamageSource.Type.MOB_MELEE, this,
                CombatRules.MOB_MELEE_HORIZONTAL_KNOCKBACK,
                CombatRules.MOB_MELEE_VERTICAL_KNOCKBACK));
        performAttack();
        return true;
    }

    private boolean tryCloseRangeMelee(World.RemotePlayerTarget target, float distSq) {
        if (burstShots > 0 || attackCooldown > 0 || target == null
                || distSq >= CLOSE_ATTACK_RANGE * CLOSE_ATTACK_RANGE || !verticallyOverlaps(target)) {
            return false;
        }
        attackCooldown = MELEE_COOLDOWN_TICKS;
        world.damageRemotePlayerTarget(target.playerId(),
                new World.RemotePlayerDamage(EASY_MELEE_DAMAGE, "mob_melee",
                        x, y, z,
                        CombatRules.MOB_MELEE_HORIZONTAL_KNOCKBACK,
                        CombatRules.MOB_MELEE_VERTICAL_KNOCKBACK,
                        0));
        performAttack();
        return true;
    }

    private float distanceSquaredToPlayer(Player player) {
        float dx = player.getPosition().x - x;
        float dy = player.getPosition().y + 1.0f - (y + getHeight() * 0.5f);
        float dz = player.getPosition().z - z;
        return dx * dx + dy * dy + dz * dz;
    }

    private boolean verticallyOverlapsPlayer(Player player) {
        float playerMinY = player.getPosition().y;
        float playerMaxY = playerMinY + 1.8f;
        return playerMaxY > y && playerMinY < y + getHeight();
    }

    private boolean verticallyOverlaps(LivingEntity target) {
        float targetMinY = target.getY();
        float targetMaxY = targetMinY + target.getHeight();
        return targetMaxY > y && targetMinY < y + getHeight();
    }

    private boolean verticallyOverlaps(World.RemotePlayerTarget target) {
        float targetMinY = target.y();
        float targetMaxY = targetMinY + target.height();
        return targetMaxY > y && targetMinY < y + getHeight();
    }

    private void tickAttackCooldown() {
        if (attackCooldown > 0) {
            attackCooldown--;
        }
    }

    private void cancelBurst() {
        burstShots = 0;
        burstCooldown = 0;
    }

    private void shootSmallFireball(Player player) {
        shootSmallFireballAt(player.getPosition().x, player.getPosition().y + 1.0f, player.getPosition().z);
    }

    private void shootSmallFireballAt(float targetX, float targetY, float targetZ) {
        float sx = x;
        float sy = y + getHeight() * 0.55f;
        float sz = z;
        float dx = targetX - sx;
        float dy = targetY - sy;
        float dz = targetZ - sz;
        float dist = Math.max(0.1f, (float) Math.sqrt(dx * dx + dy * dy + dz * dz));
        world.spawnFireball(sx, sy, sz, dx / dist * SMALL_FIREBALL_SPEED, dy / dist * SMALL_FIREBALL_SPEED,
                dz / dist * SMALL_FIREBALL_SPEED, this, false);
        spawnShotParticles(sx, sy, sz);
        performAttack();
    }

    private void spawnChargeParticles() {
        if (world == null) {
            return;
        }
        world.spawnEntityParticleBurst(WorldParticle.Type.FLAME, x, y, z,
                getWidth(), getHeight(), CHARGE_FLAME_PARTICLES);
        world.spawnEntityParticleBurst(WorldParticle.Type.SMOKE, x, y, z,
                getWidth(), getHeight(), CHARGE_SMOKE_PARTICLES);
    }

    private void spawnShotParticles(float sx, float sy, float sz) {
        if (world != null) {
            world.spawnParticleBurst(WorldParticle.Type.FLAME, sx, sy, sz, SHOT_FLAME_PARTICLES);
        }
    }

    @Override
    protected float getGravityPerTick() {
        return isOnGround() ? 0.08f : 0.02f;
    }

    @Override
    public void setOnFire(int ticks) {
        extinguish();
    }

    @Override
    public boolean damage(float amount, DamageSource source) {
        if (source != null && source.type() == DamageSource.Type.FIRE) {
            return false;
        }
        return super.damage(amount, source);
    }

    @Override
    protected void onHurt(float amount, Entity source) {
        super.onHurt(amount, source);
        playMobHurtSound(WorldSoundEvent.BLAZE_HURT);
    }

    @Override
    protected void onDeath() {
        playMobDeathSound(WorldSoundEvent.BLAZE_DEATH);
        super.onDeath();
    }

    @Override
    protected String getAmbientSoundId() {
        return WorldSoundEvent.BLAZE_BREATHE;
    }

    @Override
    public void dropLoot() {
        if (!hasRecentPlayerDamage()) {
            return;
        }
        int looting = Math.max(0, getRecentPlayerLootingLevel());
        int count = random.nextInt(2 + looting);
        if (count > 0) {
            dropItem(ItemType.BLAZE_ROD, count);
        }
    }

    @Override
    public String getTexturePath() {
        return "/textures/mob/fire.png";
    }

    @Override
    public MobModelType getModelType() {
        return MobModelType.BLAZE;
    }

    public int getAttackCooldown() {
        return attackCooldown;
    }

    public int getBurstShots() {
        return burstShots;
    }

    public int getBurstCooldown() {
        return burstCooldown;
    }

    public boolean isCharged() {
        return burstShots > 0 && (burstShots < BURST_SHOTS || burstCooldown <= CHARGED_AFTER_TICKS);
    }

    public void setAttackState(int attackCooldown, int burstShots, int burstCooldown) {
        this.attackCooldown = Math.max(0, attackCooldown);
        this.burstShots = Math.max(0, burstShots);
        this.burstCooldown = Math.max(0, burstCooldown);
    }
}
