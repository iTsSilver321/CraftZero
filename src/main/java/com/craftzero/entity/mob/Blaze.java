package com.craftzero.entity.mob;

import com.craftzero.entity.ai.LineOfSightUtil;
import com.craftzero.entity.ai.TargetNearestGoal;
import com.craftzero.entity.ai.WanderGoal;
import com.craftzero.inventory.ItemType;
import com.craftzero.main.Player;

public class Blaze extends Mob {
    private int attackCooldown = 60;
    private int burstShots;
    private int burstCooldown;

    public Blaze() {
        super(MobDefinition.BLAZE.width(), MobDefinition.BLAZE.height(), MobDefinition.BLAZE.maxHealth());
        this.definition = MobDefinition.BLAZE;
        this.hostile = true;
        this.moveSpeed = MobDefinition.BLAZE.moveSpeed();
        this.experienceValue = MobDefinition.BLAZE.experienceValue();
        ai.addGoal(2, new TargetNearestGoal(this, ai, 32.0f));
        ai.addGoal(7, new WanderGoal(this, ai, 10.0f, 0.6f));
    }

    @Override
    public void tick() {
        Player player = world != null ? world.getPlayer() : null;
        if (player != null && !player.isCreative() && player.getDifficulty().allowsHostileSpawns()) {
            float dx = player.getPosition().x - x;
            float dy = player.getPosition().y + 1.0f - (y + height * 0.5f);
            float dz = player.getPosition().z - z;
            float distSq = dx * dx + dy * dy + dz * dz;
            if (distSq < 32.0f * 32.0f && LineOfSightUtil.hasLineOfSight(world, x, y + height * 0.5f, z,
                    player.getPosition().x, player.getPosition().y + 1.0f, player.getPosition().z)) {
                lookAt(player.getPosition().x, player.getPosition().y + 1.0f, player.getPosition().z);
                motionY += player.getPosition().y + 1.0f > y + height * 0.5f ? 0.02f : -0.01f;
                handleBurst(player);
            }
        }
        super.tick();
    }

    private void handleBurst(Player player) {
        if (burstShots > 0) {
            if (burstCooldown-- <= 0) {
                shootSmallFireball(player);
                burstShots--;
                burstCooldown = 6;
            }
            return;
        }
        if (--attackCooldown <= 0) {
            burstShots = 3;
            burstCooldown = 0;
            attackCooldown = 80;
        }
    }

    private void shootSmallFireball(Player player) {
        float sx = x;
        float sy = y + height * 0.55f;
        float sz = z;
        float dx = player.getPosition().x - sx;
        float dy = player.getPosition().y + 1.0f - sy;
        float dz = player.getPosition().z - sz;
        float dist = Math.max(0.1f, (float) Math.sqrt(dx * dx + dy * dy + dz * dz));
        world.spawnFireball(sx, sy, sz, dx / dist * 0.45f, dy / dist * 0.45f, dz / dist * 0.45f, this, false);
        performAttack();
    }

    @Override
    protected float getGravityPerTick() {
        return isOnGround() ? 0.08f : 0.02f;
    }

    @Override
    public void setOnFire(int ticks) {
        extinguish();
    }

    @Override
    public void dropLoot() {
        dropItems(ItemType.BLAZE_ROD, 0, 1);
    }

    @Override
    public String getTexturePath() {
        return "/textures/mob/fire.png";
    }

    @Override
    public MobModelType getModelType() {
        return MobModelType.BLAZE;
    }
}
