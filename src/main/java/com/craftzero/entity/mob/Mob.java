package com.craftzero.entity.mob;

import com.craftzero.entity.LivingEntity;
import com.craftzero.entity.ai.EscapeGoal;
import com.craftzero.entity.ai.LookAtPlayerGoal;
import com.craftzero.entity.ai.MobAI;
import com.craftzero.entity.ai.SwimGoal;
import com.craftzero.world.BlockType;

import java.util.Random;

/**
 * Base class for all mobs (hostile and passive).
 * Extends LivingEntity with AI and mob-specific behavior.
 */
public abstract class Mob extends LivingEntity {

    protected MobAI ai;
    protected Random random;

    // Mob type flags
    protected boolean hostile;
    protected boolean burnsInSunlight;

    // Experience dropped on death
    protected int experienceValue;

    // Despawn
    protected int despawnTimer;
    protected static final int DESPAWN_DISTANCE = 128; // Blocks
    protected static final int DESPAWN_TIME = 600; // 30 seconds with no player nearby

    protected Mob(float width, float height, float maxHealth) {
        super(width, height, maxHealth);
        this.ai = new MobAI(this);
        this.random = new Random();
        this.hostile = false;
        this.burnsInSunlight = false;
        this.experienceValue = 5;
        this.despawnTimer = 0;

        // Add base goals to all mobs
        ai.addGoal(0, new SwimGoal(this)); // Highest priority - don't drown
        ai.addGoal(1, new EscapeGoal(this, ai, 0.6f)); // Escape traps
        ai.addGoal(8, new LookAtPlayerGoal(this, 8.0f)); // Look at nearby players when idle
    }

    @Override
    public void tick() {
        if (dead)
            return;

        // Update AI FIRST - this sets targetYaw and forwardSpeed
        ai.tick();

        // THEN update animation - this calculates motion from targetYaw
        // CRITICAL: Must run AFTER AI so we use the CURRENT targetYaw!
        updateAnimation();

        // FINALLY call super.tick() (LivingEntity/Entity tick)
        // This captures prev positions and handles auto-jump based on CURRENT motion
        super.tick();

        // Check sunlight burning
        if (burnsInSunlight) {
            checkSunlightBurn();
        }

        // Check despawn
        checkDespawn();
    }

    /**
     * Check if mob should burn in sunlight.
     */
    protected void checkSunlightBurn() {
        if (world == null)
            return;

        // Only during day
        if (world.getDayCycleManager() == null)
            return;
        float time = world.getDayCycleManager().getTime();
        boolean isDay = time >= 0 && time < 12000;

        if (!isDay)
            return;

        // Check sky light at head level
        int blockX = (int) Math.floor(x);
        int blockY = (int) Math.floor(y + height);
        int blockZ = (int) Math.floor(z);

        int skyLight = world.getSkyLight(blockX, blockY, blockZ);

        if (skyLight >= 15) {
            // Burn! Set on fire for 8 seconds
            setOnFire(160);
        }
    }

    /**
     * Check if mob should despawn.
     */
    protected void checkDespawn() {
        if (world == null || world.getPlayer() == null)
            return;

        float playerX = world.getPlayer().getPosition().x;
        float playerY = world.getPlayer().getPosition().y;
        float playerZ = world.getPlayer().getPosition().z;

        float dx = playerX - x;
        float dy = playerY - y;
        float dz = playerZ - z;
        float dist = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);

        // Instant despawn if very far
        if (dist > DESPAWN_DISTANCE * 2) {
            remove();
            return;
        }

        // Timer-based despawn if far
        if (dist > DESPAWN_DISTANCE) {
            despawnTimer++;
            if (despawnTimer >= DESPAWN_TIME) {
                remove();
            }
        } else {
            despawnTimer = 0;
        }
    }

    @Override
    protected void onDeath() {
        super.onDeath();
        dropLoot();
    }

    /**
     * Drop loot when killed.
     * Override in subclasses.
     */
    @Override
    public abstract void dropLoot();

    /**
     * Spawn a dropped item at the mob's position.
     */
    protected void dropItem(BlockType type, int count) {
        if (world == null || type == null || count <= 0)
            return;

        // Random offset
        float ox = (random.nextFloat() - 0.5f) * 0.5f;
        float oz = (random.nextFloat() - 0.5f) * 0.5f;

        world.spawnDroppedItem(x + ox, y + 0.5f, z + oz, type, count);
    }

    /**
     * Drop items with random count.
     */
    protected void dropItems(BlockType type, int min, int max) {
        int count = min + random.nextInt(max - min + 1);
        if (count > 0) {
            dropItem(type, count);
        }
    }

    /**
     * Get the texture path for this mob.
     */
    public abstract String getTexturePath();

    /**
     * Get the model type for this mob.
     */
    public abstract MobModelType getModelType();

    // Getters
    public MobAI getAI() {
        return ai;
    }

    public boolean isHostile() {
        return hostile;
    }

    public boolean burnsInSunlight() {
        return burnsInSunlight;
    }

    public int getExperienceValue() {
        return experienceValue;
    }

    /**
     * Enum for different model types.
     */
    public enum MobModelType {
        HUMANOID, // Zombie, Skeleton
        CREEPER, // No arms
        SPIDER, // 8 legs
        QUADRUPED, // Pig, Cow, Sheep
        CHICKEN // Small with wings
    }
}
