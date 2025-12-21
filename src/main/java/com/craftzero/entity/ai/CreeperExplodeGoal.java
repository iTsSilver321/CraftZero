package com.craftzero.entity.ai;

import com.craftzero.entity.LivingEntity;
import com.craftzero.main.Player;

/**
 * AI Goal: Creeper-specific behavior - approach and explode.
 * When close to player, start fuse countdown and explode if not interrupted.
 */
public class CreeperExplodeGoal implements Goal {

    private final LivingEntity mob;
    private final MobAI ai;
    private final float explosionRange;
    private final float chaseSpeed;

    private int fuseTime;
    private int maxFuseTime;
    private boolean fusing;
    private float swellAmount; // For visual effect (creeper puffing up)

    public CreeperExplodeGoal(LivingEntity mob, MobAI ai, float explosionRange, int fuseTime) {
        this.mob = mob;
        this.ai = ai;
        this.explosionRange = explosionRange;
        this.maxFuseTime = fuseTime;
        this.fuseTime = 0;
        this.fusing = false;
        this.chaseSpeed = 1.0f;
        this.swellAmount = 0;
    }

    @Override
    public int getPriority() {
        return 2; // High priority when activated
    }

    @Override
    public boolean canUse() {
        return ai.hasMoveTarget(); // Has a player target
    }

    @Override
    public boolean canContinue() {
        if (!ai.hasMoveTarget()) {
            // Lost target - abort fuse
            if (fusing) {
                fuseTime = Math.max(0, fuseTime - 2); // Defuse faster
                if (fuseTime <= 0) {
                    fusing = false;
                }
            }
            return fusing; // Continue until defused
        }
        return true;
    }

    @Override
    public void start() {
        fusing = false;
        fuseTime = 0;
        swellAmount = 0;
    }

    @Override
    public void tick() {
        if (mob.getWorld() == null)
            return;

        Player player = mob.getWorld().getPlayer();
        if (player == null)
            return;

        float targetX = player.getPosition().x;
        float targetY = player.getPosition().y;
        float targetZ = player.getPosition().z;

        // Calculate distance
        float dx = targetX - mob.getX();
        float dy = targetY - mob.getY();
        float dz = targetZ - mob.getZ();
        float dist = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);

        // Look at player
        mob.lookAt(targetX, targetY + 1.6f, targetZ);

        if (dist <= explosionRange) {
            // In range - start/continue fuse
            fusing = true;
            fuseTime++;

            // Stop moving when fusing
            mob.stopMoving();

            // Update swell amount for visual
            swellAmount = Math.min(1.0f, (float) fuseTime / maxFuseTime);

            // Check for explosion
            if (fuseTime >= maxFuseTime) {
                explode();
            }

        } else {
            // Out of range
            if (fusing) {
                // Defuse countdown
                fuseTime = Math.max(0, fuseTime - 1);
                swellAmount = (float) fuseTime / maxFuseTime;

                if (fuseTime <= 0) {
                    fusing = false;
                }
            }

            // Chase player
            float targetYaw = (float) Math.toDegrees(Math.atan2(dx, -dz));
            mob.setMoveDirection(targetYaw, chaseSpeed);
        }
    }

    /**
     * Execute the explosion.
     */
    private void explode() {
        if (mob.getWorld() == null)
            return;

        Player player = mob.getWorld().getPlayer();
        float explosionPower = 3.0f; // Block destruction radius
        float damageRadius = 5.0f;

        // Deal damage to player if in range
        if (player != null) {
            float dx = player.getPosition().x - mob.getX();
            float dy = player.getPosition().y - mob.getY();
            float dz = player.getPosition().z - mob.getZ();
            float dist = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);

            if (dist <= damageRadius) {
                // Damage falls off with distance
                float damageFactor = 1.0f - (dist / damageRadius);
                float damage = damageFactor * 20.0f; // Up to 20 damage at point blank
                player.getStats().damage(damage);

                // Knockback
                float knockback = damageFactor * 1.5f;
                player.getVelocity().x += (dx / dist) * knockback;
                player.getVelocity().y += 0.5f;
                player.getVelocity().z += (dz / dist) * knockback;
            }
        }

        // TODO: Create explosion effect when explosion system is implemented
        // mob.getWorld().createExplosion(mob.getX(), mob.getY(), mob.getZ(),
        // explosionPower);

        // Remove the creeper
        mob.remove();
    }

    /**
     * Get swell amount for rendering (0.0 = normal, 1.0 = max swell).
     */
    public float getSwellAmount() {
        return swellAmount;
    }

    /**
     * Check if currently fusing.
     */
    public boolean isFusing() {
        return fusing;
    }

    @Override
    public void stop() {
        fusing = false;
        fuseTime = 0;
        swellAmount = 0;
    }

    @Override
    public boolean isExclusive() {
        return true;
    }
}
