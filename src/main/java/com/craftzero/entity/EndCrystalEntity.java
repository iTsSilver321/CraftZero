package com.craftzero.entity;

import com.craftzero.combat.DamageSource;

public class EndCrystalEntity extends LivingEntity {
    private boolean exploded;

    public EndCrystalEntity() {
        super(2.0f, 2.0f, 5.0f);
    }

    public EndCrystalEntity(float x, float y, float z) {
        this();
        setPosition(x, y, z);
    }

    @Override
    public void updatePhysics(float deltaTime) {
        // End crystals are fixed entities.
    }

    @Override
    protected void onDeath() {
        if (!exploded && world != null) {
            exploded = true;
            world.explode(x, y, z, 3.0f);
        }
    }

    @Override
    public boolean damage(float amount, DamageSource source) {
        boolean applied = super.damage(amount, source);
        if (applied) {
            health = 0.0f;
        }
        return applied;
    }

    public boolean isExploded() {
        return exploded;
    }
}
