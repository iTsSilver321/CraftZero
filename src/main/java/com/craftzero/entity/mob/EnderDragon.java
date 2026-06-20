package com.craftzero.entity.mob;

import com.craftzero.combat.DamageSource;
import com.craftzero.entity.EndCrystalEntity;
import com.craftzero.entity.Entity;
import com.craftzero.main.CombatRules;
import com.craftzero.main.Player;
import com.craftzero.world.BlockType;

public class EnderDragon extends Mob {
    private float targetX;
    private float targetY;
    private float targetZ;
    private int targetCooldown;
    private int deathTicks;

    public EnderDragon() {
        super(MobDefinition.ENDER_DRAGON.width(), MobDefinition.ENDER_DRAGON.height(),
                MobDefinition.ENDER_DRAGON.maxHealth());
        this.definition = MobDefinition.ENDER_DRAGON;
        this.hostile = true;
        this.moveSpeed = MobDefinition.ENDER_DRAGON.moveSpeed();
        this.experienceValue = MobDefinition.ENDER_DRAGON.experienceValue();
        chooseTarget();
    }

    @Override
    public void tick() {
        prevX = x;
        prevY = y;
        prevZ = z;
        prevYaw = yaw;
        prevPitch = pitch;
        ticksExisted++;
        prevBodyYaw = bodyYaw;
        prevLimbSwingAmount = limbSwingAmount;
        if (hurtTime > 0) {
            hurtTime--;
        }
        if (invulnerableTime > 0) {
            invulnerableTime--;
        }
        if (health <= 0.0f) {
            if (!dead) {
                dead = true;
                onDeath();
            }
            deathTicks++;
            motionX *= 0.92f;
            motionY *= 0.92f;
            motionZ *= 0.92f;
            if (deathTicks > 120) {
                remove();
            }
            return;
        }
        healFromCrystals();
        fly();
        damageNearbyPlayer();
    }

    private void fly() {
        if (targetCooldown-- <= 0 || distanceToTarget() < 8.0f) {
            chooseTarget();
        }
        float dx = targetX - x;
        float dy = targetY - y;
        float dz = targetZ - z;
        float distance = Math.max(0.1f, (float) Math.sqrt(dx * dx + dy * dy + dz * dz));
        motionX += dx / distance * 0.035f;
        motionY += dy / distance * 0.025f;
        motionZ += dz / distance * 0.035f;
        motionX *= 0.91f;
        motionY *= 0.91f;
        motionZ *= 0.91f;
        x += motionX;
        y += motionY;
        z += motionZ;
        yaw = (float) Math.toDegrees(Math.atan2(motionX, -motionZ));
        bodyYaw = yaw;
        limbSwing += 0.18f;
        limbSwingAmount = 0.35f;
    }

    private void chooseTarget() {
        double angle = random.nextDouble() * Math.PI * 2.0;
        float radius = 48.0f + random.nextFloat() * 42.0f;
        targetX = (float) Math.cos(angle) * radius;
        targetZ = (float) Math.sin(angle) * radius;
        targetY = 68.0f + random.nextFloat() * 34.0f;
        targetCooldown = 80 + random.nextInt(80);
    }

    private float distanceToTarget() {
        float dx = targetX - x;
        float dy = targetY - y;
        float dz = targetZ - z;
        return (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    private void healFromCrystals() {
        if (world == null || ticksExisted % 10 != 0 || health >= maxHealth) {
            return;
        }
        for (Entity entity : world.getEntities()) {
            if (entity instanceof EndCrystalEntity && !entity.isRemoved()) {
                float dx = entity.getX() - x;
                float dy = entity.getY() - y;
                float dz = entity.getZ() - z;
                if (dx * dx + dy * dy + dz * dz <= 64.0f * 64.0f) {
                    heal(1.0f);
                    return;
                }
            }
        }
    }

    private void damageNearbyPlayer() {
        if (world == null || world.getPlayer() == null || ticksExisted % 10 != 0) {
            return;
        }
        Player player = world.getPlayer();
        float dx = player.getPosition().x - x;
        float dy = player.getPosition().y + 1.0f - (y + height * 0.5f);
        float dz = player.getPosition().z - z;
        if (dx * dx + dy * dy + dz * dz <= 7.0f * 7.0f) {
            player.hurt(5.0f, DamageSource.point(DamageSource.Type.GENERIC, x, y + height * 0.5f, z,
                    CombatRules.MOB_MELEE_HORIZONTAL_KNOCKBACK * 2.0f,
                    CombatRules.MOB_MELEE_VERTICAL_KNOCKBACK + 0.2f));
        }
    }

    @Override
    public void updatePhysics(float deltaTime) {
        // The dragon's flight integration happens in tick().
    }

    @Override
    protected void onDeath() {
        if (world == null) {
            return;
        }
        int cx = 0;
        int cy = 64;
        int cz = 0;
        for (int x = cx - 2; x <= cx + 2; x++) {
            for (int z = cz - 2; z <= cz + 2; z++) {
                world.setBlock(x, cy, z, BlockType.BEDROCK);
            }
        }
        for (int x = cx - 1; x <= cx + 1; x++) {
            for (int z = cz - 1; z <= cz + 1; z++) {
                world.setBlock(x, cy + 1, z, BlockType.END_PORTAL);
            }
        }
        world.setBlock(cx, cy + 2, cz, BlockType.DRAGON_EGG);
        world.spawnExperience(cx + 0.5f, cy + 2.0f, cz + 0.5f, 12000);
    }

    @Override
    public void dropLoot() {
    }

    @Override
    public String getTexturePath() {
        return "/textures/mob/enderdragon/dragon.png";
    }

    @Override
    public MobModelType getModelType() {
        return MobModelType.DRAGON;
    }

    public int getDeathTicks() {
        return deathTicks;
    }
}
