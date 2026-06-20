package com.craftzero.entity;

public class PrimedTntEntity extends Entity {
    private int fuseTicks;

    public PrimedTntEntity(float x, float y, float z, int fuseTicks) {
        super(0.98f, 0.98f);
        this.fuseTicks = Math.max(1, fuseTicks);
        setPosition(x, y, z);
    }

    @Override
    public void tick() {
        super.tick();
        fuseTicks--;
        if (fuseTicks <= 0) {
            if (world != null) {
                world.explode(x, y + 0.5f, z, 4.0f);
            }
            remove();
        }
    }

    @Override
    protected float getGravityPerTick() {
        return 0.04f;
    }

    @Override
    protected float getAirResistance() {
        return 0.98f;
    }

    public int getFuseTicks() {
        return fuseTicks;
    }

    public void setFuseTicks(int fuseTicks) {
        this.fuseTicks = Math.max(1, fuseTicks);
    }
}
