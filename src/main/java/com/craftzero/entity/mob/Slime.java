package com.craftzero.entity.mob;

import com.craftzero.combat.DamageSource;
import com.craftzero.entity.Entity;
import com.craftzero.entity.ai.LineOfSightUtil;
import com.craftzero.inventory.ItemType;
import com.craftzero.main.CombatRules;
import com.craftzero.main.Player;
import com.craftzero.world.World;
import com.craftzero.world.WorldSoundEvent;

/**
 * Release-style slime with size-based health/damage and split behavior.
 */
public class Slime extends Mob {
    private final int size;
    private int jumpDelay;
    private float prevSquishAmount;
    private float squishAmount;

    public Slime() {
        this(4);
    }

    public Slime(int size) {
        super(0.6f * normalizeSize(size), 0.6f * normalizeSize(size), normalizeSize(size) * normalizeSize(size));
        this.size = normalizeSize(size);
        this.definition = MobDefinition.SLIME;
        this.hostile = true;
        this.burnsInSunlight = false;
        this.moveSpeed = 0.16f + this.size * 0.015f;
        this.experienceValue = this.size;
        this.jumpDelay = nextIdleJumpDelay();
    }

    private static int normalizeSize(int size) {
        if (size <= 1) {
            return 1;
        }
        if (size <= 2) {
            return 2;
        }
        return 4;
    }

    @Override
    public void tick() {
        tickSquishAnimation();
        if (dead) {
            super.tick();
            return;
        }
        Player player = world != null ? world.getPlayer() : null;
        World.RemotePlayerTarget remoteTarget = world != null
                ? world.nearestRemotePlayerTarget(x, y + getHeight() * 0.85f, z, 16.0f, true)
                : null;
        tryTargetNearestPlayer(player, remoteTarget);
        if (onGround && jumpDelay-- <= 0) {
            targetYaw = random.nextFloat() * 360.0f;
            forwardSpeed = 0.8f;
            motionY = getJumpVelocity();
            startJumpSquish();
            playSlimeJumpSound();
            jumpDelay = nextIdleJumpDelay();
        }
        updateAnimation();
        super.tick();
    }

    @Override
    protected void onDeath() {
        if (world != null && size > 1) {
            int children = 2 + random.nextInt(3);
            int childSize = size / 2;
            for (int i = 0; i < children; i++) {
                Slime child = createChild(childSize);
                float ox = ((i % 2) - 0.5f) * size * 0.25f;
                float oz = ((i / 2) - 0.5f) * size * 0.25f;
                child.setPosition(x + ox, y + 0.5f, z + oz);
                child.setYaw(random.nextFloat() * 360.0f);
                world.spawnEntity(child);
            }
        }
        playSlimeSquishSound();
        super.onDeath();
    }

    @Override
    protected void onHurt(float amount, Entity source) {
        super.onHurt(amount, source);
        if (health > 0.0f) {
            playSlimeSquishSound();
        }
    }

    @Override
    protected void onLanded(float fallDistance) {
        super.onLanded(fallDistance);
        squishAmount = -0.5f;
        if (!dead) {
            playSlimeJumpSound();
            spawnLandingParticles();
        }
    }

    @Override
    protected boolean isFallDamageImmune() {
        return true;
    }

    protected void spawnLandingParticles() {
        if (world != null) {
            world.spawnSlimeLandingParticles(x, y, z, width, size);
        }
    }

    private void tickSquishAnimation() {
        prevSquishAmount = squishAmount;
        squishAmount *= 0.6f;
    }

    private void startJumpSquish() {
        squishAmount = 1.0f;
    }

    protected Slime createChild(int childSize) {
        return new Slime(childSize);
    }

    protected int nextIdleJumpDelay() {
        return 10 + random.nextInt(20);
    }

    protected int nextTargetingJumpDelay() {
        return Math.max(1, nextIdleJumpDelay() / 3);
    }

    protected float getJumpVelocity() {
        return 0.42f;
    }

    protected String getSlimeSquishSoundId() {
        return WorldSoundEvent.SLIME;
    }

    protected String getSlimeJumpSoundId() {
        return getSlimeSquishSoundId();
    }

    protected String getSlimeAttackSoundId() {
        return WorldSoundEvent.SLIME_ATTACK;
    }

    protected void playSlimeJumpSound() {
        playSizedSlimeSound(getSlimeJumpSoundId());
    }

    protected void playSlimeSquishSound() {
        playSizedSlimeSound(getSlimeSquishSoundId());
    }

    protected void playSlimeAttackSound() {
        playSizedSlimeSound(getSlimeAttackSoundId());
    }

    protected void playSizedSlimeSound(String soundId) {
        if (world == null || soundId == null || soundId.isEmpty()) {
            return;
        }
        world.playSound(soundId, x, y + getHeight() * 0.5f, z,
                getSlimeSoundVolume(), getSlimeSoundPitch());
    }

    protected float getSlimeSoundVolume() {
        return 0.4f * size;
    }

    protected float getSlimeSoundPitch() {
        return mobSoundPitch() / 0.8f;
    }

    protected boolean canDamagePlayerOnContact() {
        return size > 1;
    }

    private void tryTargetNearestPlayer(Player player, World.RemotePlayerTarget remoteTarget) {
        boolean localValid = player != null && !player.isCreative() && player.getDifficulty().allowsHostileSpawns();
        float localDistSq = localValid ? distanceSquaredToPlayer(player) : Float.MAX_VALUE;
        boolean remoteValid = remoteTarget != null && remoteTarget.valid();
        float remoteDistSq = remoteValid ? remoteTarget.distance() * remoteTarget.distance() : Float.MAX_VALUE;

        if (remoteValid && (!localValid || remoteDistSq <= localDistSq)) {
            targetRemotePlayer(remoteTarget);
        } else if (localValid) {
            targetLocalPlayer(player);
        }
    }

    private void targetLocalPlayer(Player player) {
        lookAt(player.getPosition().x, player.getPosition().y + 1.0f, player.getPosition().z);
        float dx = player.getPosition().x - x;
        float dz = player.getPosition().z - z;
        jumpTowardTarget(dx, dz);
        if (canDamagePlayerOnContact(player) && canAttack()) {
            performAttack();
            boolean hit = player.hurt(getPlayerContactDamage(), DamageSource.entity(DamageSource.Type.MOB_MELEE, this,
                    CombatRules.MOB_MELEE_HORIZONTAL_KNOCKBACK,
                    CombatRules.MOB_MELEE_VERTICAL_KNOCKBACK));
            if (hit) {
                playSlimeAttackSound();
            }
        }
    }

    private void targetRemotePlayer(World.RemotePlayerTarget target) {
        lookAt(target.x(), target.eyeY(), target.z());
        float dx = target.x() - x;
        float dz = target.z() - z;
        jumpTowardTarget(dx, dz);
        if (canDamageRemotePlayerOnContact(target) && canAttack()) {
            performAttack();
            boolean hit = world.damageRemotePlayerTarget(target.playerId(),
                    new World.RemotePlayerDamage(getPlayerContactDamage(), "mob_melee",
                            x, y, z,
                            CombatRules.MOB_MELEE_HORIZONTAL_KNOCKBACK,
                            CombatRules.MOB_MELEE_VERTICAL_KNOCKBACK,
                            0));
            if (hit) {
                playSlimeAttackSound();
            }
        }
    }

    private void jumpTowardTarget(float dx, float dz) {
        float distSq = dx * dx + dz * dz;
        if (distSq >= 16.0f * 16.0f || !onGround || jumpDelay > 0) {
            return;
        }
        targetYaw = (float) Math.toDegrees(Math.atan2(dx, -dz));
        forwardSpeed = 1.0f;
        motionY = getJumpVelocity();
        startJumpSquish();
        playSlimeJumpSound();
        jumpDelay = nextTargetingJumpDelay();
    }

    private boolean canDamagePlayerOnContact(Player player) {
        return canDamagePlayerOnContact()
                && isPlayerWithinContactRange(player)
                && hasLineOfSightToPlayer(player);
    }

    private boolean canDamageRemotePlayerOnContact(World.RemotePlayerTarget target) {
        return canDamagePlayerOnContact()
                && isRemotePlayerWithinContactRange(target)
                && hasLineOfSightToRemotePlayer(target);
    }

    private float distanceSquaredToPlayer(Player player) {
        float dx = player.getPosition().x - x;
        float dy = player.getPosition().y - y;
        float dz = player.getPosition().z - z;
        return dx * dx + dy * dy + dz * dz;
    }

    private boolean isPlayerWithinContactRange(Player player) {
        float dx = player.getPosition().x - x;
        float dy = player.getPosition().y - y;
        float dz = player.getPosition().z - z;
        float attackRange = getPlayerContactAttackRange();
        return dx * dx + dy * dy + dz * dz < attackRange * attackRange;
    }

    private boolean isRemotePlayerWithinContactRange(World.RemotePlayerTarget target) {
        float dx = target.x() - x;
        float dy = target.y() - y;
        float dz = target.z() - z;
        float attackRange = getPlayerContactAttackRange();
        return dx * dx + dy * dy + dz * dz < attackRange * attackRange;
    }

    private boolean hasLineOfSightToPlayer(Player player) {
        return LineOfSightUtil.hasLineOfSight(world,
                x, y + getHeight() * 0.85f, z,
                player.getPosition().x, player.getPosition().y + 1.6f, player.getPosition().z);
    }

    private boolean hasLineOfSightToRemotePlayer(World.RemotePlayerTarget target) {
        return target != null && LineOfSightUtil.hasLineOfSight(world,
                x, y + getHeight() * 0.85f, z,
                target.x(), target.eyeY(), target.z());
    }

    protected float getPlayerContactDamage() {
        return size;
    }

    protected float getPlayerContactAttackRange() {
        return 0.6f * size;
    }

    @Override
    public void dropLoot() {
        if (size == 1) {
            dropItems(ItemType.SLIMEBALL, 0, 2);
        }
    }

    @Override
    public String getTexturePath() {
        return "/textures/mob/slime.png";
    }

    @Override
    public MobModelType getModelType() {
        return MobModelType.SLIME;
    }

    public int getSize() {
        return size;
    }

    public int getJumpDelay() {
        return jumpDelay;
    }

    public float getRenderSquishAmount(float partialTick) {
        float t = Math.max(0.0f, Math.min(1.0f, partialTick));
        return prevSquishAmount + (squishAmount - prevSquishAmount) * t;
    }

    public void setJumpDelay(int jumpDelay) {
        this.jumpDelay = Math.max(0, jumpDelay);
    }
}
