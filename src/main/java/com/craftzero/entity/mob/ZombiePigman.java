package com.craftzero.entity.mob;

import com.craftzero.combat.CombatTargetResolver;
import com.craftzero.combat.DamageSource;
import com.craftzero.entity.Entity;
import com.craftzero.entity.ai.MeleeAttackGoal;
import com.craftzero.entity.ai.TargetNearestGoal;
import com.craftzero.entity.ai.WanderGoal;
import com.craftzero.inventory.ItemType;
import com.craftzero.main.CombatRules;
import com.craftzero.main.Player;
import com.craftzero.world.World;
import com.craftzero.world.WorldSoundEvent;

public class ZombiePigman extends Mob {
    private static final int ANGER_MIN_TICKS = 400;
    private static final int ANGER_RANDOM_TICKS = 400;
    private static final int ANGRY_SOUND_DELAY_BOUND = 40;
    private static final float ANGRY_SOUND_VOLUME = 2.0f;

    private int angerTicks;
    private int angerSoundDelay;

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
        tickAngerState();
        super.tick();
    }

    private void tickAngerState() {
        if (angerTicks <= 0) {
            angerSoundDelay = 0;
            return;
        }

        angerTicks--;
        if (angerTicks == 0) {
            angerSoundDelay = 0;
            ai.clearMoveTarget();
            ai.clearTarget();
            return;
        }

        if (angerSoundDelay > 0 && --angerSoundDelay == 0) {
            playAngrySound();
        }
    }

    private void angerNearbyPigmen(DamageSource source) {
        int anger = ANGER_MIN_TICKS + random.nextInt(ANGER_RANDOM_TICKS);
        String remoteProvokerId = remoteProvokerId(source);
        Player provoker = remoteProvokerId.isBlank() ? resolvePlayerProvoker() : null;
        setAngerState(anger, random.nextInt(ANGRY_SOUND_DELAY_BOUND));
        chaseProvoker(provoker, remoteProvokerId);
        if (world == null) {
            return;
        }
        for (Entity entity : world.getEntities()) {
            if (entity instanceof ZombiePigman pigman && pigman != this && distanceToSquared(pigman) <= 32.0f * 32.0f) {
                pigman.setAngerState(Math.max(pigman.angerTicks, anger), pigman.random.nextInt(ANGRY_SOUND_DELAY_BOUND));
                pigman.chaseProvoker(provoker, remoteProvokerId);
            }
        }
    }

    private void setAngerState(int angerTicks, int angerSoundDelay) {
        this.angerTicks = Math.max(0, angerTicks);
        this.angerSoundDelay = this.angerTicks > 0 ? Math.max(0, angerSoundDelay) : 0;
    }

    private void playAngrySound() {
        if (world != null) {
            world.playSound(WorldSoundEvent.ZOMBIE_PIGMAN_ANGRY, x, y + getHeight() * 0.5f, z,
                    ANGRY_SOUND_VOLUME, mobSoundPitch());
        }
    }

    private void chaseProvoker(Player provoker, String remoteProvokerId) {
        if (remoteProvokerId != null && !remoteProvokerId.isBlank() && world != null) {
            World.RemotePlayerTarget target = world.remotePlayerTargetById(remoteProvokerId);
            if (target != null && target.valid()) {
                ai.setRemotePlayerTarget(target);
                ai.setMoveTarget(target.x(), target.y(), target.z());
                return;
            }
        }
        if (!isValidProvoker(provoker)) {
            return;
        }
        ai.clearRemotePlayerTarget();
        ai.setMoveTarget(provoker.getPosition().x, provoker.getPosition().y, provoker.getPosition().z);
    }

    private boolean isValidProvoker(Player provoker) {
        return provoker != null
                && provoker.getStats().getHealth() > 0.0f
                && !provoker.isCreative()
                && provoker.getDifficulty().allowsHostileSpawns();
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
        boolean playerAggression = isPlayerAggression(source);
        boolean applied = super.damage(amount, source);
        if (applied && playerAggression) {
            angerNearbyPigmen(source);
        }
        return applied;
    }

    private Player resolvePlayerProvoker() {
        return world == null ? null : world.getPlayer();
    }

    private String remoteProvokerId(DamageSource source) {
        return CombatTargetResolver.remotePlayerId(source);
    }

    private boolean isPlayerAggression(DamageSource source) {
        return CombatTargetResolver.isPlayerAggression(source);
    }

    @Override
    public void dropLoot() {
        dropItems(ItemType.ROTTEN_FLESH, 0, 1);
        dropItems(ItemType.GOLD_NUGGET, 0, 1);
    }

    @Override
    protected void onHurt(float amount, Entity source) {
        super.onHurt(amount, source);
        playMobHurtSound(WorldSoundEvent.ZOMBIE_PIGMAN_HURT);
    }

    @Override
    protected void onDeath() {
        playMobDeathSound(WorldSoundEvent.ZOMBIE_PIGMAN_DEATH);
        super.onDeath();
    }

    @Override
    protected String getAmbientSoundId() {
        return WorldSoundEvent.ZOMBIE_PIGMAN_IDLE;
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
        setAngerState(angerTicks, angerSoundDelay);
    }

    public int getAngerSoundDelay() {
        return angerSoundDelay;
    }
}
