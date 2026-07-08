package com.craftzero.entity.mob;

import com.craftzero.entity.Entity;
import com.craftzero.entity.ai.*;
import com.craftzero.inventory.ItemType;
import com.craftzero.world.WorldSoundEvent;

import java.util.Random;

/**
 * Sheep mob - passive, drops wool.
 */
public class Sheep extends Mob {

    private static final MobBalance.Spec SPEC = MobBalance.SHEEP;
    private static final ItemType[] WOOL_BY_COLOR = {
            ItemType.WHITE_WOOL,
            ItemType.ORANGE_WOOL,
            ItemType.MAGENTA_WOOL,
            ItemType.LIGHT_BLUE_WOOL,
            ItemType.YELLOW_WOOL,
            ItemType.LIME_WOOL,
            ItemType.PINK_WOOL,
            ItemType.GRAY_WOOL,
            ItemType.LIGHT_GRAY_WOOL,
            ItemType.CYAN_WOOL,
            ItemType.PURPLE_WOOL,
            ItemType.BLUE_WOOL,
            ItemType.BROWN_WOOL,
            ItemType.GREEN_WOOL,
            ItemType.RED_WOOL,
            ItemType.BLACK_WOOL
    };
    private static final float[][] FLEECE_COLOR_BY_COLOR = {
            { 1.0f, 1.0f, 1.0f },
            { 0.95f, 0.7f, 0.2f },
            { 0.9f, 0.5f, 0.85f },
            { 0.6f, 0.7f, 0.95f },
            { 0.9f, 0.9f, 0.2f },
            { 0.5f, 0.8f, 0.1f },
            { 0.95f, 0.7f, 0.8f },
            { 0.3f, 0.3f, 0.3f },
            { 0.6f, 0.6f, 0.6f },
            { 0.3f, 0.6f, 0.7f },
            { 0.7f, 0.4f, 0.9f },
            { 0.2f, 0.4f, 0.8f },
            { 0.5f, 0.4f, 0.3f },
            { 0.4f, 0.5f, 0.2f },
            { 0.8f, 0.3f, 0.3f },
            { 0.1f, 0.1f, 0.1f }
    };

    // Wool color (0 = white)
    private int woolColor = 0;
    private boolean sheared = false;
    private int eatingGrassTimer = 0;

    public Sheep() {
        super(SPEC.width(), SPEC.height(), SPEC.maxHealth());
        this.definition = MobDefinition.SHEEP;
        this.hostile = SPEC.hostile();
        this.burnsInSunlight = SPEC.burnsInSunlight();
        this.moveSpeed = SPEC.moveSpeed();
        this.experienceValue = SPEC.experienceValue();

        setupAI();
    }

    private void setupAI() {
        ai.addGoal(1, new PanicGoal(this, ai, 1.5f));
        ai.addGoal(2, new EatGrassGoal(this, ai));
        ai.addGoal(7, new WanderGoal(this, ai, 8.0f, 0.6f));
    }

    @Override
    public void tick() {
        super.tick();
        if (isDead() || isRemoved()) {
            setEatingGrassTimer(0);
        }
    }

    @Override
    public void dropLoot() {
        // Drop 1 wool block
        if (!sheared) {
            dropItem(woolItem(), 1);
        }
    }

    @Override
    protected String getAmbientSoundId() {
        return WorldSoundEvent.SHEEP_IDLE;
    }

    @Override
    protected void onHurt(float amount, Entity source) {
        super.onHurt(amount, source);
        playMobHurtSound(WorldSoundEvent.SHEEP_HURT);
    }

    @Override
    protected void onDeath() {
        playMobDeathSound(WorldSoundEvent.SHEEP_DEATH);
        super.onDeath();
    }

    @Override
    protected boolean isBreedingItem(ItemType itemType) {
        return itemType == ItemType.WHEAT;
    }

    @Override
    protected boolean isBreedingCompatible(Mob mate) {
        return mate instanceof Sheep;
    }

    @Override
    protected Mob createBreedingChild(Mob mate) {
        Sheep child = new Sheep();
        if (mate instanceof Sheep other && random.nextBoolean()) {
            child.setWoolColor(other.getWoolColor());
        } else {
            child.setWoolColor(woolColor);
        }
        return child;
    }

    /**
     * Shear the sheep.
     * 
     * @return true if successfully sheared
     */
    public boolean shear() {
        if (!sheared && !isBaby()) {
            sheared = true;
            // Drop 1-3 wool
            dropItemsWithoutLooting(woolItem(), 1, 3);
            return true;
        }
        return false;
    }

    public boolean dye(int woolColor) {
        int clampedColor = Math.max(0, Math.min(15, woolColor));
        if (sheared || this.woolColor == clampedColor) {
            return false;
        }
        this.woolColor = clampedColor;
        return true;
    }

    public boolean isSheared() {
        return sheared;
    }

    public void setSheared(boolean sheared) {
        this.sheared = sheared;
    }

    public void onAteGrass() {
        sheared = false;
        if (isBaby()) {
            growingAge = Math.min(0, growingAge + 60);
        }
    }

    public int getEatingGrassTimer() {
        return eatingGrassTimer;
    }

    public void setEatingGrassTimer(int eatingGrassTimer) {
        this.eatingGrassTimer = Math.max(0, Math.min(EatGrassGoal.EATING_TICKS, eatingGrassTimer));
    }

    public float getGrassEatingHeadOffsetScale(float partialTick) {
        if (eatingGrassTimer <= 0) {
            return 0.0f;
        }
        if (eatingGrassTimer >= 4 && eatingGrassTimer <= 36) {
            return 1.0f;
        }
        float partial = clampPartialTick(partialTick);
        if (eatingGrassTimer < 4) {
            return Math.max(0.0f, (eatingGrassTimer - partial) / 4.0f);
        }
        return Math.max(0.0f, (40.0f - eatingGrassTimer + partial) / 4.0f);
    }

    public float getGrassEatingHeadPitch(float partialTick) {
        if (eatingGrassTimer <= 0) {
            return 0.0f;
        }
        float partial = clampPartialTick(partialTick);
        if (eatingGrassTimer > 4 && eatingGrassTimer <= 36) {
            float phase = (eatingGrassTimer - 4.0f - partial) / 32.0f * (float) Math.PI * 28.7f;
            return (float) Math.PI / 5.0f + 0.21991149f * (float) Math.sin(phase);
        }
        return (float) Math.PI / 5.0f;
    }

    public int getWoolColor() {
        return woolColor;
    }

    public void setWoolColor(int woolColor) {
        this.woolColor = Math.max(0, Math.min(15, woolColor));
    }

    public void applyNaturalSpawnColor(Random random) {
        setWoolColor(randomFleeceColor(random));
    }

    public static int woolColorForDye(ItemType dye) {
        if (dye == null) {
            return -1;
        }
        return switch (dye) {
            case INK_SAC -> 15;
            case ROSE_RED -> 14;
            case CACTUS_GREEN -> 13;
            case COCOA_BEANS -> 12;
            case LAPIS_LAZULI -> 11;
            case PURPLE_DYE -> 10;
            case CYAN_DYE -> 9;
            case LIGHT_GRAY_DYE -> 8;
            case GRAY_DYE -> 7;
            case PINK_DYE -> 6;
            case LIME_DYE -> 5;
            case DANDELION_YELLOW -> 4;
            case LIGHT_BLUE_DYE -> 3;
            case MAGENTA_DYE -> 2;
            case ORANGE_DYE -> 1;
            case BONE_MEAL -> 0;
            default -> -1;
        };
    }

    public static int randomFleeceColor(Random random) {
        Random source = random == null ? new Random() : random;
        int roll = source.nextInt(100);
        if (roll < 5) {
            return 15;
        }
        if (roll < 10) {
            return 7;
        }
        if (roll < 15) {
            return 8;
        }
        if (roll < 18) {
            return 12;
        }
        return source.nextInt(500) == 0 ? 6 : 0;
    }

    public float[] getFleeceColor() {
        return FLEECE_COLOR_BY_COLOR[woolColor].clone();
    }

    private static float clampPartialTick(float partialTick) {
        return Math.max(0.0f, Math.min(1.0f, partialTick));
    }

    private ItemType woolItem() {
        return WOOL_BY_COLOR[Math.max(0, Math.min(WOOL_BY_COLOR.length - 1, woolColor))];
    }

    @Override
    public String getTexturePath() {
        return "/textures/mob/sheep.png";
    }

    @Override
    public MobModelType getModelType() {
        return MobModelType.QUADRUPED;
    }
}
