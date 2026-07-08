package com.craftzero.entity.mob;

/**
 * Mooshroom mob - mushroom island cow variant.
 */
public class Mooshroom extends Cow {

    private static final MobBalance.Spec SPEC = MobBalance.MOOSHROOM;

    public Mooshroom() {
        super();
        this.definition = MobDefinition.MOOSHROOM;
        this.hostile = SPEC.hostile();
        this.burnsInSunlight = SPEC.burnsInSunlight();
        this.moveSpeed = SPEC.moveSpeed();
        this.experienceValue = SPEC.experienceValue();
    }

    @Override
    public String getTexturePath() {
        return "/textures/mob/redcow.png";
    }

    @Override
    protected Mob createBreedingChild(Mob mate) {
        return new Mooshroom();
    }
}
