package com.craftzero.entity.mob;

import com.craftzero.combat.DamageSource;
import com.craftzero.world.WorldSoundEvent;

public class MagmaCube extends Slime {
    public MagmaCube() {
        this(4);
    }

    public MagmaCube(int size) {
        super(size);
        this.definition = MobDefinition.MAGMA_CUBE;
        this.experienceValue = getSize();
    }

    @Override
    protected Slime createChild(int childSize) {
        return new MagmaCube(childSize);
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
    public void dropLoot() {
        // Java Release 1.0 had magma cream as a crafting ingredient, but
        // Magma Cubes did not start dropping it until Java 1.1.
    }

    @Override
    protected int nextIdleJumpDelay() {
        return super.nextIdleJumpDelay() * 4;
    }

    @Override
    protected float getJumpVelocity() {
        return 0.42f + getSize() * 0.1f;
    }

    @Override
    protected String getSlimeSquishSoundId() {
        return getSize() > 1 ? WorldSoundEvent.MAGMA_CUBE_BIG : WorldSoundEvent.MAGMA_CUBE_SMALL;
    }

    @Override
    protected String getSlimeJumpSoundId() {
        return WorldSoundEvent.MAGMA_CUBE_JUMP;
    }

    @Override
    protected boolean canDamagePlayerOnContact() {
        return true;
    }

    @Override
    protected float getPlayerContactDamage() {
        return getSize() + 2.0f;
    }

    @Override
    public String getTexturePath() {
        return "/textures/mob/lava.png";
    }
}
