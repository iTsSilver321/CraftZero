package com.craftzero.entity.mob;

import com.craftzero.entity.ai.*;
import com.craftzero.world.BlockType;

/**
 * Skeleton mob - hostile, ranged attack, burns in sunlight.
 */
public class Skeleton extends Mob {

    private static final float WIDTH = 0.6f;
    private static final float HEIGHT = 1.95f;
    private static final float MAX_HEALTH = 20.0f;

    // Ranged attack settings
    private int shootCooldown = 0;
    private static final int SHOOT_INTERVAL = 40; // 2 seconds between shots

    public Skeleton() {
        super(WIDTH, HEIGHT, MAX_HEALTH);
        this.hostile = true;
        this.burnsInSunlight = true;
        this.moveSpeed = 0.15f;
        this.experienceValue = 5;

        setupAI();
    }

    private void setupAI() {
        // Skeletons prefer ranged combat - stay at distance
        ai.addGoal(2, new TargetNearestGoal(this, ai, 16.0f));
        // Attack at range (handled in tick)
        ai.addGoal(7, new WanderGoal(this, ai, 10.0f, 0.8f));
    }

    @Override
    public void tick() {
        super.tick();

        if (dead)
            return;

        // Handle ranged attack
        if (shootCooldown > 0) {
            shootCooldown--;
        }

        if (ai.hasMoveTarget() && world != null && world.getPlayer() != null) {
            float playerX = world.getPlayer().getPosition().x;
            float playerY = world.getPlayer().getPosition().y;
            float playerZ = world.getPlayer().getPosition().z;

            float dx = playerX - x;
            float dy = playerY - y;
            float dz = playerZ - z;
            float dist = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);

            // Maintain distance (8-14 blocks)
            if (dist < 8) {
                // Move away - negate dx and dz for "opposite" direction
                float awayYaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
                setMoveDirection(awayYaw, 1.0f);
            } else if (dist > 14) {
                // Move closer
                float towardYaw = (float) Math.toDegrees(Math.atan2(dx, -dz));
                setMoveDirection(towardYaw, 1.0f);
            } else {
                // In range - stop and shoot
                stopMoving();
            }

            // Look at player
            lookAt(playerX, playerY + 1.6f, playerZ);

            // Shoot arrow
            if (dist <= 16 && shootCooldown <= 0) {
                shootArrow(playerX, playerY + 1.0f, playerZ);
                shootCooldown = SHOOT_INTERVAL;
            }
        }
    }

    private void shootArrow(float targetX, float targetY, float targetZ) {
        if (world == null)
            return;

        // Calculate arrow velocity toward target
        float dx = targetX - x;
        float dy = targetY - (y + height * 0.85f);
        float dz = targetZ - z;
        float dist = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);

        if (dist < 0.1f)
            return;

        float speed = 1.5f;
        float velX = (dx / dist) * speed;
        float velY = (dy / dist) * speed + 0.1f; // Slight arc
        float velZ = (dz / dist) * speed;

        // TODO: Spawn EntityArrow when implemented
        // For now, just deal direct damage with some chance to miss
        if (random.nextFloat() < 0.7f) { // 70% accuracy
            world.getPlayer().getStats().damage(3.0f);
        }
    }

    @Override
    public void dropLoot() {
        // Drop 0-2 bones and 0-2 arrows
        // TODO: Add BONE and ARROW items when item system is expanded
        dropItems(BlockType.STONE, 0, 2); // Placeholder for bones
    }

    @Override
    public String getTexturePath() {
        return "/textures/mob/skeleton.png";
    }

    @Override
    public MobModelType getModelType() {
        return MobModelType.HUMANOID;
    }
}
