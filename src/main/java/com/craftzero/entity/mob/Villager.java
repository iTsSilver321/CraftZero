package com.craftzero.entity.mob;

import com.craftzero.entity.ai.OpenDoorGoal;
import com.craftzero.entity.ai.AvoidEntityGoal;
import com.craftzero.entity.ai.PanicGoal;
import com.craftzero.entity.ai.WanderGoal;

/**
 * Release 1.0 villager runtime used by generated village structures.
 * Trading is intentionally absent because it was not part of Java Release 1.0.
 */
public class Villager extends Mob {
    public static final int PROFESSION_FARMER = 0;
    public static final int PROFESSION_LIBRARIAN = 1;
    public static final int PROFESSION_PRIEST = 2;
    public static final int PROFESSION_SMITH = 3;
    public static final int PROFESSION_BUTCHER = 4;

    private int profession;

    public Villager() {
        this(PROFESSION_FARMER);
    }

    public Villager(int profession) {
        super(MobDefinition.VILLAGER.width(), MobDefinition.VILLAGER.height(),
                MobDefinition.VILLAGER.maxHealth());
        this.definition = MobDefinition.VILLAGER;
        this.hostile = MobDefinition.VILLAGER.hostile();
        this.burnsInSunlight = MobDefinition.VILLAGER.burnsInSunlight();
        this.moveSpeed = MobDefinition.VILLAGER.moveSpeed();
        this.experienceValue = MobDefinition.VILLAGER.experienceValue();
        setProfession(profession);

        setupAI();
    }

    private void setupAI() {
        ai.addGoal(1, new PanicGoal(this, ai, 1.5f));
        ai.addGoal(2, new AvoidEntityGoal(this, ai, Zombie.class, 8.0f, 12.0f, 0.8f));
        ai.addGoal(2, new OpenDoorGoal(this, ai, true));
        ai.addGoal(7, new WanderGoal(this, ai, 8.0f, 0.6f));
    }

    public int getProfession() {
        return profession;
    }

    public void setProfession(int profession) {
        this.profession = Math.max(PROFESSION_FARMER, Math.min(PROFESSION_BUTCHER, profession));
    }

    @Override
    public void dropLoot() {
        // Release 1.0 villagers do not have ordinary mob drops.
    }

    @Override
    public String getTexturePath() {
        return switch (profession) {
            case PROFESSION_LIBRARIAN -> "/textures/mob/villager/librarian.png";
            case PROFESSION_PRIEST -> "/textures/mob/villager/priest.png";
            case PROFESSION_SMITH -> "/textures/mob/villager/smith.png";
            case PROFESSION_BUTCHER -> "/textures/mob/villager/butcher.png";
            default -> "/textures/mob/villager/farmer.png";
        };
    }

    @Override
    public MobModelType getModelType() {
        return MobModelType.VILLAGER;
    }
}
