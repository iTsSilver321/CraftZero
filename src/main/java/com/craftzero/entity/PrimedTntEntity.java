package com.craftzero.entity;

import com.craftzero.combat.DamageSource;
import com.craftzero.world.WorldParticle;

public class PrimedTntEntity extends Entity {
    private static final float GRAVITY_PER_TICK = 0.04f;
    private static final float DRAG = 0.98f;
    private static final float GROUND_HORIZONTAL_DRAG = 0.7f;
    private static final float GROUND_VERTICAL_BOUNCE = -0.5f;

    private int fuseTicks;
    private boolean playerOwned;
    private String remoteOwnerPlayerId = "";

    public PrimedTntEntity(float x, float y, float z, int fuseTicks) {
        super(0.98f, 0.98f);
        this.fuseTicks = Math.max(0, fuseTicks);
        setPosition(x, y, z);
    }

    @Override
    public void tick() {
        super.tick();
        if (fuseTicks-- <= 0) {
            if (world != null) {
                world.explode(x, y + 0.5f, z, 4.0f, false, explosionDamageSource());
            }
            remove();
        } else if (world != null) {
            world.spawnParticle(WorldParticle.Type.SMOKE, x, y + 0.5f, z,
                    0.0f, 0.0f, 0.0f, 0.35f, 20);
        }
    }

    @Override
    public void updatePhysics(float deltaTime) {
        if (world == null || removed) {
            return;
        }

        motionY -= GRAVITY_PER_TICK;
        float attemptedMotionY = motionY;

        moveWithCollision(motionX, motionY, motionZ);

        motionX *= DRAG;
        motionZ *= DRAG;
        if (onGround) {
            motionX *= GROUND_HORIZONTAL_DRAG;
            motionZ *= GROUND_HORIZONTAL_DRAG;
            motionY = attemptedMotionY * DRAG * GROUND_VERTICAL_BOUNCE;
        } else {
            motionY *= DRAG;
        }
    }

    public int getFuseTicks() {
        return fuseTicks;
    }

    public void setFuseTicks(int fuseTicks) {
        this.fuseTicks = Math.max(0, fuseTicks);
    }

    public boolean isPlayerOwned() {
        return playerOwned;
    }

    public void setPlayerOwned(boolean playerOwned) {
        this.playerOwned = playerOwned;
    }

    public String getRemoteOwnerPlayerId() {
        return remoteOwnerPlayerId;
    }

    public void setRemoteOwnerPlayerId(String remoteOwnerPlayerId) {
        this.remoteOwnerPlayerId = remoteOwnerPlayerId == null ? "" : remoteOwnerPlayerId.trim();
        if (!this.remoteOwnerPlayerId.isBlank()) {
            this.playerOwned = true;
        }
    }

    private DamageSource explosionDamageSource() {
        DamageSource source = DamageSource.point(DamageSource.Type.EXPLOSION,
                x, y + 0.5f, z, 0.0f, 0.0f);
        if (playerOwned || !remoteOwnerPlayerId.isBlank()) {
            source = source.withPlayerCredit(true);
        }
        if (!remoteOwnerPlayerId.isBlank()) {
            source = source.withPlayerId(remoteOwnerPlayerId);
        }
        return source;
    }
}
