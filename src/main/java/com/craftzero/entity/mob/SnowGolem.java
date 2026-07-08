package com.craftzero.entity.mob;

import com.craftzero.combat.DamageSource;
import com.craftzero.entity.Entity;
import com.craftzero.entity.ai.LineOfSightUtil;
import com.craftzero.entity.ai.WanderGoal;
import com.craftzero.inventory.ItemType;
import com.craftzero.world.BlockType;

/**
 * Release 1.0 Snow Golem utility mob.
 */
public class SnowGolem extends Mob {
    private static final float TARGET_RANGE = 10.0f;
    private static final int ATTACK_INTERVAL_TICKS = 20;
    private static final float SNOWBALL_SOURCE_Y_OFFSET = 1.1f;
    private static final float SNOWBALL_TARGET_EYE_FRACTION = 0.85f;
    private static final float SNOWBALL_TARGET_Y_DROP = 1.1f;
    private static final float SNOWBALL_ARC_PER_HORIZONTAL_BLOCK = 0.2f;
    private static final float SNOWBALL_THROW_SPEED = 1.6f;
    private static final float SNOW_TRAIL_TEMPERATURE_LIMIT = 0.8f;
    private static final float HOT_BIOME_DAMAGE_TEMPERATURE = 1.0f;
    private int attackCooldown;

    public SnowGolem() {
        super(MobDefinition.SNOW_GOLEM.width(), MobDefinition.SNOW_GOLEM.height(),
                MobDefinition.SNOW_GOLEM.maxHealth());
        this.definition = MobDefinition.SNOW_GOLEM;
        this.hostile = MobDefinition.SNOW_GOLEM.hostile();
        this.burnsInSunlight = MobDefinition.SNOW_GOLEM.burnsInSunlight();
        this.moveSpeed = MobDefinition.SNOW_GOLEM.moveSpeed();
        this.experienceValue = MobDefinition.SNOW_GOLEM.experienceValue();

        ai.addGoal(7, new WanderGoal(this, ai, 8.0f, 0.8f));
    }

    @Override
    public void tick() {
        if (isDead()) {
            super.tick();
            return;
        }
        applyEnvironmentRules();
        if (attackCooldown > 0) {
            attackCooldown--;
        }
        Mob target = findSnowballTarget();
        if (target != null) {
            lookAt(target.getX(), target.getY() + target.getHeight() * 0.5f, target.getZ());
            if (attackCooldown <= 0) {
                throwSnowballAt(target);
                attackCooldown = ATTACK_INTERVAL_TICKS;
            }
        }
        super.tick();
    }

    private void applyEnvironmentRules() {
        if (world == null) {
            return;
        }
        if (isWet()) {
            damage(1.0f, DamageSource.point(DamageSource.Type.DROWN, x, y, z, 0.0f, 0.0f));
        }
        int blockX = (int) Math.floor(x);
        int blockZ = (int) Math.floor(z);
        if (world.getReleaseBiome(blockX, blockZ).getTemperature() > HOT_BIOME_DAMAGE_TEMPERATURE) {
            damage(1.0f, DamageSource.point(DamageSource.Type.FIRE, x, y, z, 0.0f, 0.0f));
        }
        placeSnowTrail();
    }

    private boolean isWet() {
        int blockX = (int) Math.floor(x);
        int blockZ = (int) Math.floor(z);
        int feetY = (int) Math.floor(y + 0.1f);
        int headY = (int) Math.floor(y + getHeight() * 0.85f);
        return world.getBlockIfLoaded(blockX, feetY, blockZ, BlockType.AIR).isWater()
                || world.getBlockIfLoaded(blockX, headY, blockZ, BlockType.AIR).isWater()
                || world.isRainingAt(blockX, headY, blockZ);
    }

    private void placeSnowTrail() {
        for (int i = 0; i < 4; i++) {
            int blockX = (int) Math.floor(x + (i % 2 * 2 - 1) * 0.25f);
            int blockY = (int) Math.floor(y);
            int blockZ = (int) Math.floor(z + (i / 2 % 2 * 2 - 1) * 0.25f);
            if (world.getReleaseBiome(blockX, blockZ).getTemperature() >= SNOW_TRAIL_TEMPERATURE_LIMIT) {
                continue;
            }
            if (world.getBlockIfLoaded(blockX, blockY, blockZ, BlockType.AIR) != BlockType.AIR) {
                continue;
            }
            if (world.canPlaceBlockAt(blockX, blockY, blockZ, BlockType.SNOW_LAYER, 0, null)) {
                world.setBlock(blockX, blockY, blockZ, BlockType.SNOW_LAYER, 0);
            }
        }
    }

    private Mob findSnowballTarget() {
        if (world == null) {
            return null;
        }
        Mob best = null;
        float bestDistanceSq = TARGET_RANGE * TARGET_RANGE;
        for (Entity entity : world.getEntities()) {
            if (!(entity instanceof Mob mob) || mob == this || !mob.isHostile() || mob.isRemoved() || mob.isDead()) {
                continue;
            }
            float dx = mob.getX() - x;
            float dy = mob.getY() + mob.getHeight() * 0.5f - (y + getHeight() * 0.75f);
            float dz = mob.getZ() - z;
            float distanceSq = dx * dx + dy * dy + dz * dz;
            if (distanceSq >= bestDistanceSq) {
                continue;
            }
            if (!LineOfSightUtil.hasLineOfSight(world, x, y + getHeight() * 0.75f, z,
                    mob.getX(), mob.getY() + mob.getHeight() * 0.5f, mob.getZ())) {
                continue;
            }
            best = mob;
            bestDistanceSq = distanceSq;
        }
        return best;
    }

    private void throwSnowballAt(Mob target) {
        float sx = x;
        float sy = y + SNOWBALL_SOURCE_Y_OFFSET;
        float sz = z;
        float tx = target.getX();
        float ty = target.getY() + target.getHeight() * SNOWBALL_TARGET_EYE_FRACTION - SNOWBALL_TARGET_Y_DROP;
        float tz = target.getZ();
        float dx = tx - sx;
        float dy = ty - sy;
        float dz = tz - sz;
        float horizontal = (float) Math.sqrt(dx * dx + dz * dz);
        dy += horizontal * SNOWBALL_ARC_PER_HORIZONTAL_BLOCK;
        float dist = Math.max(0.1f, (float) Math.sqrt(dx * dx + dy * dy + dz * dz));
        world.spawnThrownItemProjectile(sx, sy, sz,
                dx / dist * SNOWBALL_THROW_SPEED,
                dy / dist * SNOWBALL_THROW_SPEED,
                dz / dist * SNOWBALL_THROW_SPEED,
                ItemType.SNOWBALL, this);
        world.playBowSound(sx, sy, sz);
        performAttack();
    }

    public int getSnowballAttackCooldown() {
        return attackCooldown;
    }

    public void setSnowballAttackCooldown(int attackCooldown) {
        this.attackCooldown = Math.max(0, attackCooldown);
    }

    @Override
    public void dropLoot() {
        dropItemsWithoutLooting(ItemType.SNOWBALL, 0, 15);
    }

    @Override
    public String getTexturePath() {
        return "/textures/mob/snowman.png";
    }

    @Override
    public MobModelType getModelType() {
        return MobModelType.SNOW_GOLEM;
    }
}
