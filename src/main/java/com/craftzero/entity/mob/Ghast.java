package com.craftzero.entity.mob;

import com.craftzero.combat.DamageSource;
import com.craftzero.entity.Entity;
import com.craftzero.entity.LivingEntity;
import com.craftzero.entity.ai.LineOfSightUtil;
import com.craftzero.inventory.ItemType;
import com.craftzero.main.Player;
import com.craftzero.world.World;
import com.craftzero.world.WorldSoundEvent;

public class Ghast extends Mob {
    private static final float ATTACK_RANGE = 64.0f;
    private static final int ATTACK_CHARGE_TICKS = 20;
    private static final int FIRE_TEXTURE_CHARGE_TICKS = 10;
    private static final int POST_FIRE_COOLDOWN_TICKS = 40;
    private static final float FIREBALL_SPEED = 0.55f;
    private static final float FIREBALL_FORWARD_OFFSET = 4.0f;
    private static final float FIREBALL_VERTICAL_OFFSET = 0.5f;
    private static final float SOUND_VOLUME = 10.0f;

    private int fireCooldown = 60;
    private int attackCharge;
    private int wanderCooldown;
    private float targetX, targetY, targetZ;

    public Ghast() {
        super(MobDefinition.GHAST.width(), MobDefinition.GHAST.height(), MobDefinition.GHAST.maxHealth());
        this.definition = MobDefinition.GHAST;
        this.hostile = true;
        this.moveSpeed = MobDefinition.GHAST.moveSpeed();
        this.experienceValue = MobDefinition.GHAST.experienceValue();
    }

    @Override
    public void tick() {
        if (isDead()) {
            super.tick();
            return;
        }
        if (world != null) {
            LivingEntity livingTarget = ai.getTarget();
            if (canAttack(livingTarget)) {
                float targetEyeX = livingTarget.getX();
                float targetEyeY = livingTarget.getY() + livingTarget.getHeight() * 0.5f;
                float targetEyeZ = livingTarget.getZ();
                lookAt(targetEyeX, targetEyeY, targetEyeZ);
                faceToward(targetEyeX - x, targetEyeZ - z);
                handleFireballAttack(targetEyeX, targetEyeY, targetEyeZ);
                super.tick();
                return;
            }
            Player player = world.getPlayer();
            World.RemotePlayerTarget remoteTarget = world.nearestRemotePlayerTarget(
                    x, y + getHeight() * 0.5f, z, ATTACK_RANGE, true);
            boolean canAttackPlayer = canAttack(player);
            boolean canAttackRemote = canAttack(remoteTarget);
            if (canAttackRemote && (!canAttackPlayer
                    || remoteTarget.distance() * remoteTarget.distance() <= distanceToSquaredPlayer(player))) {
                lookAt(remoteTarget.x(), remoteTarget.eyeY(), remoteTarget.z());
                faceToward(remoteTarget.x() - x, remoteTarget.z() - z);
                handleFireballAttack(remoteTarget.x(), remoteTarget.eyeY(), remoteTarget.z());
            } else if (canAttackPlayer) {
                float targetEyeX = player.getPosition().x;
                float targetEyeY = player.getPosition().y + 1.0f;
                float targetEyeZ = player.getPosition().z;
                lookAt(targetEyeX, targetEyeY, targetEyeZ);
                faceToward(targetEyeX - x, targetEyeZ - z);
                handleFireballAttack(targetEyeX, targetEyeY, targetEyeZ);
            } else {
                coolInterruptedAttack();
                wander();
            }
        }
        super.tick();
    }

    private boolean canAttack(Player player) {
        if (player == null || player.isCreative() || !player.getDifficulty().allowsHostileSpawns()) {
            return false;
        }
        return distanceToSquaredPlayer(player) < ATTACK_RANGE * ATTACK_RANGE
                && LineOfSightUtil.hasLineOfSight(world, x, y + getHeight() * 0.5f, z,
                        player.getPosition().x, player.getPosition().y + 1.0f, player.getPosition().z);
    }

    private boolean canAttack(LivingEntity target) {
        if (target == null || target == this || target.isDead() || target.isRemoved()) {
            return false;
        }
        float targetEyeX = target.getX();
        float targetEyeY = target.getY() + target.getHeight() * 0.5f;
        float targetEyeZ = target.getZ();
        float dx = targetEyeX - x;
        float dy = targetEyeY - y;
        float dz = targetEyeZ - z;
        return dx * dx + dy * dy + dz * dz < ATTACK_RANGE * ATTACK_RANGE
                && LineOfSightUtil.hasLineOfSight(world, x, y + getHeight() * 0.5f, z,
                        targetEyeX, targetEyeY, targetEyeZ);
    }

    private boolean canAttack(World.RemotePlayerTarget target) {
        return target != null
                && target.valid()
                && target.distance() < ATTACK_RANGE
                && LineOfSightUtil.hasLineOfSight(world, x, y + getHeight() * 0.5f, z,
                        target.x(), target.eyeY(), target.z());
    }

    private float distanceToSquaredPlayer(Player player) {
        float dx = player.getPosition().x - x;
        float dy = player.getPosition().y - y;
        float dz = player.getPosition().z - z;
        return dx * dx + dy * dy + dz * dz;
    }

    private void handleFireballAttack(float targetX, float targetY, float targetZ) {
        if (fireCooldown > 0) {
            fireCooldown--;
            attackCharge = 0;
            return;
        }
        attackCharge++;
        if (attackCharge == FIRE_TEXTURE_CHARGE_TICKS && world != null) {
            world.playSound(WorldSoundEvent.GHAST_CHARGE, x, y + getHeight() * 0.5f, z, 10.0f, 1.0f);
        }
        if (attackCharge >= ATTACK_CHARGE_TICKS) {
            shootFireballAt(targetX, targetY, targetZ, true);
            attackCharge = 0;
            fireCooldown = POST_FIRE_COOLDOWN_TICKS;
        }
    }

    private void coolInterruptedAttack() {
        if (attackCharge > 0) {
            attackCharge--;
        }
    }

    private void shootFireball(Player player, boolean explosive) {
        shootFireballAt(player.getPosition().x, player.getPosition().y + 1.0f, player.getPosition().z, explosive);
    }

    private void shootFireballAt(float targetX, float targetY, float targetZ, boolean explosive) {
        float horizontalDx = targetX - x;
        float horizontalDz = targetZ - z;
        float horizontalDist = Math.max(0.1f,
                (float) Math.sqrt(horizontalDx * horizontalDx + horizontalDz * horizontalDz));
        float sx = x + horizontalDx / horizontalDist * FIREBALL_FORWARD_OFFSET;
        float sy = y + getHeight() * 0.5f + FIREBALL_VERTICAL_OFFSET;
        float sz = z + horizontalDz / horizontalDist * FIREBALL_FORWARD_OFFSET;
        float dx = targetX - sx;
        float dy = targetY - sy;
        float dz = targetZ - sz;
        float dist = Math.max(0.1f, (float) Math.sqrt(dx * dx + dy * dy + dz * dz));
        world.spawnFireball(sx, sy, sz, dx / dist * FIREBALL_SPEED, dy / dist * FIREBALL_SPEED,
                dz / dist * FIREBALL_SPEED, this, explosive);
        world.playSound(WorldSoundEvent.GHAST_FIREBALL, sx, sy, sz, 10.0f, 1.0f);
        performAttack();
    }

    private void wander() {
        if (wanderCooldown-- <= 0 || distanceToTarget() < 2.0f) {
            targetX = x + (random.nextFloat() - 0.5f) * 32.0f;
            targetY = Math.max(12.0f, Math.min(118.0f, y + (random.nextFloat() - 0.5f) * 16.0f));
            targetZ = z + (random.nextFloat() - 0.5f) * 32.0f;
            wanderCooldown = 60 + random.nextInt(80);
        }
        float dx = targetX - x;
        float dy = targetY - y;
        float dz = targetZ - z;
        float dist = Math.max(0.1f, (float) Math.sqrt(dx * dx + dy * dy + dz * dz));
        motionX += dx / dist * 0.01f;
        motionY += dy / dist * 0.01f;
        motionZ += dz / dist * 0.01f;
        motionX *= 0.92f;
        motionY *= 0.92f;
        motionZ *= 0.92f;
        faceToward(motionX, motionZ);
    }

    private float distanceToTarget() {
        float dx = targetX - x;
        float dy = targetY - y;
        float dz = targetZ - z;
        return (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    @Override
    protected float getGravityPerTick() {
        return 0.0f;
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
    protected void onHurt(float amount, Entity attacker) {
        super.onHurt(amount, attacker);
        playMobHurtSound(WorldSoundEvent.GHAST_HURT);
    }

    @Override
    protected void onDeath() {
        super.onDeath();
        playMobDeathSound(WorldSoundEvent.GHAST_DEATH);
    }

    @Override
    protected String getAmbientSoundId() {
        return WorldSoundEvent.GHAST_IDLE;
    }

    @Override
    protected void playMobSound(String soundId) {
        if (world == null || soundId == null || soundId.isEmpty()) {
            return;
        }
        world.playSound(soundId, x, y + getHeight() * 0.5f, z, SOUND_VOLUME, mobSoundPitch());
    }

    @Override
    public void dropLoot() {
        dropItems(ItemType.GUNPOWDER, 0, 2);
        dropItems(ItemType.GHAST_TEAR, 0, 1);
    }

    @Override
    public String getTexturePath() {
        return attackCharge > FIRE_TEXTURE_CHARGE_TICKS
                ? "/textures/mob/ghast_fire.png"
                : "/textures/mob/ghast.png";
    }

    @Override
    public MobModelType getModelType() {
        return MobModelType.GHAST;
    }

    public int getFireCooldown() {
        return fireCooldown;
    }

    public int getAttackCharge() {
        return attackCharge;
    }

    public int getWanderCooldown() {
        return wanderCooldown;
    }

    public float getTargetX() {
        return targetX;
    }

    public float getTargetY() {
        return targetY;
    }

    public float getTargetZ() {
        return targetZ;
    }

    public void setFlightState(int fireCooldown, int wanderCooldown, float targetX, float targetY, float targetZ) {
        setFlightState(fireCooldown, 0, wanderCooldown, targetX, targetY, targetZ);
    }

    public void setFlightState(int fireCooldown, int attackCharge, int wanderCooldown,
            float targetX, float targetY, float targetZ) {
        this.fireCooldown = Math.max(0, fireCooldown);
        this.attackCharge = Math.max(0, attackCharge);
        this.wanderCooldown = Math.max(0, wanderCooldown);
        this.targetX = targetX;
        this.targetY = targetY;
        this.targetZ = targetZ;
    }

    private void faceToward(float dx, float dz) {
        if (dx * dx + dz * dz < 0.0001f) {
            return;
        }
        float facingYaw = (float) Math.toDegrees(Math.atan2(dx, -dz));
        targetYaw = facingYaw;
        bodyYaw = facingYaw;
        yaw = facingYaw;
    }
}
