package com.craftzero.entity.mob;

import com.craftzero.inventory.ItemType;

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
    public void dropLoot() {
        if (getSize() == 1) {
            dropItems(ItemType.MAGMA_CREAM, 0, 1);
        }
    }

    @Override
    public String getTexturePath() {
        return "/textures/mob/lava.png";
    }
}
