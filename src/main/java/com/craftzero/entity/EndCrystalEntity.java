package com.craftzero.entity;

import com.craftzero.combat.DamageSource;
import com.craftzero.progression.StatusEffectInstance;
import com.craftzero.progression.StatusEffectType;
import com.craftzero.world.BlockType;
import com.craftzero.world.Dimension;

import java.util.Collections;
import java.util.List;

public class EndCrystalEntity extends LivingEntity {
    public static final float EXPLOSION_POWER = 6.0f;

    private boolean exploded;
    private boolean destructionNotified;
    private int innerRotation;
    private int prevInnerRotation;

    public EndCrystalEntity() {
        super(2.0f, 2.0f, 5.0f);
    }

    public EndCrystalEntity(float x, float y, float z) {
        this();
        setPosition(x, y, z);
    }

    @Override
    public void tick() {
        super.tick();
        prevInnerRotation = innerRotation;
        innerRotation++;
    }

    @Override
    public void updatePhysics(float deltaTime) {
        if (world != null && world.getDimension() == Dimension.THE_END) {
            int blockX = (int) Math.floor(x);
            int blockY = (int) Math.floor(y);
            int blockZ = (int) Math.floor(z);
            if (world.getBlockIfLoaded(blockX, blockY, blockZ, BlockType.AIR) != BlockType.FIRE) {
                world.setBlockIfLoaded(blockX, blockY, blockZ, BlockType.FIRE, 0);
            }
        }
        // End crystals are fixed entities.
    }

    @Override
    protected void onDeath() {
        explodeOnce();
    }

    @Override
    public boolean damage(float amount, DamageSource source) {
        if (dead || isRemoved() || exploded || !Float.isFinite(amount)) {
            return false;
        }
        if (source != null && source.type() == DamageSource.Type.FIRE) {
            return false;
        }
        if (amount <= 0.0f && source == null) {
            return false;
        }
        health = 0.0f;
        dead = true;
        deathTime = 0;
        destroy(source == null || source.type() != DamageSource.Type.EXPLOSION);
        remove();
        return true;
    }

    @Override
    public void setOnFire(int ticks) {
        // Vanilla End crystals are not living mobs; the fire they maintain should not burn them down.
    }

    @Override
    public void heal(float amount) {
        // End crystals are not living mobs and cannot be healed by potion-style systems.
    }

    @Override
    public void setHealth(float health) {
        // Keep the compatibility health value stable; any non-fire damage destroys the crystal.
    }

    @Override
    public void addEffect(StatusEffectInstance effect) {
        // End crystals do not carry living-entity potion state.
    }

    @Override
    public void setActiveEffects(List<StatusEffectInstance> effects) {
        // End crystals do not carry living-entity potion state.
    }

    @Override
    public List<StatusEffectInstance> getActiveEffects() {
        return Collections.emptyList();
    }

    @Override
    public boolean hasEffect(StatusEffectType type) {
        return false;
    }

    public boolean isExploded() {
        return exploded;
    }

    public float getRenderInnerRotation(float partialTick) {
        float t = Math.max(0.0f, Math.min(1.0f, partialTick));
        return prevInnerRotation + (innerRotation - prevInnerRotation) * t;
    }

    public int getInnerRotation() {
        return innerRotation;
    }

    @Override
    public void setTicksExisted(int ticksExisted) {
        super.setTicksExisted(ticksExisted);
        innerRotation = getTicksExisted();
        prevInnerRotation = innerRotation;
    }

    private void explodeOnce() {
        destroy(true);
    }

    private void destroy(boolean createExplosion) {
        if (world == null) {
            return;
        }
        if (!destructionNotified) {
            destructionNotified = true;
            world.onEndCrystalDestroyed(this);
        }
        if (createExplosion && !exploded) {
            exploded = true;
            world.explode(x, y, z, EXPLOSION_POWER);
        }
    }
}
