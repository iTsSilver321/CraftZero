package com.craftzero.entity.mob;

import com.craftzero.entity.ai.*;
import com.craftzero.world.BlockType;

/**
 * Creeper mob - hostile, explodes when near player.
 */
public class Creeper extends Mob {

    private static final float WIDTH = 0.6f;
    private static final float HEIGHT = 1.7f;
    private static final float MAX_HEALTH = 20.0f;

    // Explosion mechanics
    private int fuseTime = 0;
    private static final int MAX_FUSE = 30; // 1.5 seconds
    private static final float EXPLOSION_RADIUS = 3.0f;
    private static final float EXPLOSION_POWER = 3.0f;
    private static final float IGNITE_DISTANCE = 3.0f;

    private boolean ignited = false;
    private int fuseDelayAfterStopped = 0;

    public Creeper() {
        super(WIDTH, HEIGHT, MAX_HEALTH);
        this.hostile = true;
        this.burnsInSunlight = false; // Creepers don't burn
        this.moveSpeed = 0.15f;
        this.experienceValue = 5;

        setupAI();
    }

    private void setupAI() {
        ai.addGoal(2, new TargetNearestGoal(this, ai, 16.0f));
        ai.addGoal(3, new MeleeAttackGoal(this, ai, 0.0f, 2.0f, 1.0f)); // No damage, just approach
        ai.addGoal(7, new WanderGoal(this, ai, 10.0f, 0.8f));
    }

    @Override
    public void tick() {
        super.tick();

        if (dead || world == null)
            return;

        // Check distance to player for fuse
        if (world.getPlayer() != null) {
            float playerX = world.getPlayer().getPosition().x;
            float playerY = world.getPlayer().getPosition().y;
            float playerZ = world.getPlayer().getPosition().z;

            float dx = playerX - x;
            float dy = playerY - y;
            float dz = playerZ - z;
            float dist = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);

            if (dist <= IGNITE_DISTANCE) {
                // Start or continue fuse
                if (!ignited) {
                    ignited = true;
                    // TODO: Play hiss sound
                }
                fuseTime++;
                fuseDelayAfterStopped = 10; // Grace period

                if (fuseTime >= MAX_FUSE) {
                    explode();
                    return;
                }
            } else {
                // Player moved away - decrease fuse
                if (ignited) {
                    fuseDelayAfterStopped--;
                    if (fuseDelayAfterStopped <= 0) {
                        fuseTime = Math.max(0, fuseTime - 1);
                        if (fuseTime == 0) {
                            ignited = false;
                        }
                    }
                }
            }
        }
    }

    private void explode() {
        if (world != null) {
            world.explode(x, y + height * 0.5f, z, EXPLOSION_POWER);
        }
        remove(); // Creeper dies in explosion
    }

    @Override
    public void dropLoot() {
        // Drop 0-2 gunpowder
        // TODO: Add GUNPOWDER to BlockType
        dropItems(BlockType.SAND, 0, 2); // Placeholder
    }

    /**
     * Get fuse progress (0.0 to 1.0) for rendering.
     */
    public float getFuseProgress() {
        return (float) fuseTime / MAX_FUSE;
    }

    /**
     * Check if creeper is ignited (for rendering swelling effect).
     */
    public boolean isIgnited() {
        return ignited;
    }

    @Override
    public String getTexturePath() {
        return "/textures/mob/creeper.png";
    }

    @Override
    public MobModelType getModelType() {
        return MobModelType.CREEPER;
    }
}
