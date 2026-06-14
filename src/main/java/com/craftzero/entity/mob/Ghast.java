package com.craftzero.entity.mob;

import com.craftzero.entity.ai.LineOfSightUtil;
import com.craftzero.inventory.ItemType;
import com.craftzero.main.Player;

public class Ghast extends Mob {
    private int fireCooldown = 60;
    private int wanderCooldown;
    private float targetX, targetY, targetZ;

    public Ghast() {
        super(MobDefinition.GHAST.width(), MobDefinition.GHAST.height(), MobDefinition.GHAST.maxHealth());
        this.definition = MobDefinition.GHAST;
        this.hostile = true;
        this.moveSpeed = MobDefinition.GHAST.moveSpeed();
        this.experienceValue = MobDefinition.GHAST.experienceValue();
    }

    @Override
    public void tick() {
        if (world != null) {
            Player player = world.getPlayer();
            if (player != null && !player.isCreative() && player.getDifficulty().allowsHostileSpawns()
                    && distanceToSquaredPlayer(player) < 64.0f * 64.0f
                    && LineOfSightUtil.hasLineOfSight(world, x, y + height * 0.5f, z,
                            player.getPosition().x, player.getPosition().y + 1.0f, player.getPosition().z)) {
                lookAt(player.getPosition().x, player.getPosition().y + 1.0f, player.getPosition().z);
                if (--fireCooldown <= 0) {
                    shootFireball(player, true);
                    fireCooldown = 40 + random.nextInt(30);
                }
            } else {
                fireCooldown = Math.max(20, fireCooldown - 1);
                wander();
            }
        }
        super.tick();
    }

    private float distanceToSquaredPlayer(Player player) {
        float dx = player.getPosition().x - x;
        float dy = player.getPosition().y - y;
        float dz = player.getPosition().z - z;
        return dx * dx + dy * dy + dz * dz;
    }

    private void shootFireball(Player player, boolean explosive) {
        float sx = x;
        float sy = y + height * 0.5f;
        float sz = z;
        float dx = player.getPosition().x - sx;
        float dy = player.getPosition().y + 1.0f - sy;
        float dz = player.getPosition().z - sz;
        float dist = Math.max(0.1f, (float) Math.sqrt(dx * dx + dy * dy + dz * dz));
        world.spawnFireball(sx, sy, sz, dx / dist * 0.55f, dy / dist * 0.55f, dz / dist * 0.55f, this, explosive);
        performAttack();
    }

    private void wander() {
        if (wanderCooldown-- <= 0 || distanceToTarget() < 2.0f) {
            targetX = x + (random.nextFloat() - 0.5f) * 32.0f;
            targetY = Math.max(12.0f, Math.min(118.0f, y + (random.nextFloat() - 0.5f) * 16.0f));
            targetZ = z + (random.nextFloat() - 0.5f) * 32.0f;
            wanderCooldown = 60 + random.nextInt(80);
        }
        float dx = targetX - x;
        float dy = targetY - y;
        float dz = targetZ - z;
        float dist = Math.max(0.1f, (float) Math.sqrt(dx * dx + dy * dy + dz * dz));
        motionX += dx / dist * 0.01f;
        motionY += dy / dist * 0.01f;
        motionZ += dz / dist * 0.01f;
        motionX *= 0.92f;
        motionY *= 0.92f;
        motionZ *= 0.92f;
    }

    private float distanceToTarget() {
        float dx = targetX - x;
        float dy = targetY - y;
        float dz = targetZ - z;
        return (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    @Override
    protected float getGravityPerTick() {
        return 0.0f;
    }

    @Override
    public void dropLoot() {
        dropItems(ItemType.GUNPOWDER, 0, 2);
        dropItems(ItemType.GHAST_TEAR, 0, 1);
    }

    @Override
    public String getTexturePath() {
        return "/textures/mob/ghast.png";
    }

    @Override
    public MobModelType getModelType() {
        return MobModelType.GHAST;
    }
}
