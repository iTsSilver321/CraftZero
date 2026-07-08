package com.craftzero.entity.ai;

import com.craftzero.combat.DamageSource;
import com.craftzero.entity.Entity;
import com.craftzero.entity.mob.Sheep;
import com.craftzero.entity.mob.Wolf;

/**
 * Release-era wild wolf behavior: untamed calm wolves hunt nearby sheep.
 */
public class WolfHuntSheepGoal implements Goal {
    private static final float TARGET_RANGE = 16.0f;
    private static final float ATTACK_RANGE = 1.6f;
    private static final float CHASE_SPEED = 1.2f;
    private static final int TARGET_CHANCE = 200;
    private static final int PATH_RECALC_INTERVAL_TICKS = 10;

    private final Wolf wolf;
    private final MobAI ai;
    private Sheep targetSheep;
    private int pathRecalcCooldown;

    public WolfHuntSheepGoal(Wolf wolf, MobAI ai) {
        this.wolf = wolf;
        this.ai = ai;
    }

    @Override
    public int getPriority() {
        return 4;
    }

    @Override
    public boolean canUse() {
        if (!wolf.canHuntSheep()) {
            targetSheep = null;
            return false;
        }
        if (wolf.getRandom().nextInt(TARGET_CHANCE) != 0) {
            return false;
        }
        targetSheep = findNearestSheep(TARGET_RANGE);
        return targetSheep != null;
    }

    @Override
    public boolean canContinue() {
        return isValidTarget(targetSheep, TARGET_RANGE * 1.5f);
    }

    @Override
    public void start() {
        pathRecalcCooldown = 0;
    }

    @Override
    public void tick() {
        if (!isValidTarget(targetSheep, TARGET_RANGE * 1.5f)) {
            ai.requestStopMoving();
            return;
        }

        wolf.lookAt(targetSheep.getX(), targetSheep.getY() + targetSheep.getHeight() * 0.85f, targetSheep.getZ());

        float dx = targetSheep.getX() - wolf.getX();
        float dy = targetSheep.getY() - wolf.getY();
        float dz = targetSheep.getZ() - wolf.getZ();
        float distance = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (distance <= ATTACK_RANGE && wolf.canAttack() && hasLineOfSight(targetSheep)) {
            wolf.performAttack();
            targetSheep.damage(Wolf.WILD_ATTACK_DAMAGE,
                    DamageSource.entity(DamageSource.Type.MOB_MELEE, wolf, 0.2f, 0.1f));
            return;
        }

        pathRecalcCooldown--;
        if (pathRecalcCooldown <= 0) {
            ai.setMoveTarget(targetSheep.getX(), targetSheep.getY(), targetSheep.getZ());
            pathRecalcCooldown = PATH_RECALC_INTERVAL_TICKS;
        }
        if (distance > 0.001f) {
            ai.requestMoveToward(targetSheep.getX(), targetSheep.getZ(), CHASE_SPEED, ATTACK_RANGE * 0.5f, 1.5f);
        }
    }

    @Override
    public void stop() {
        targetSheep = null;
        ai.requestStopMoving();
    }

    private Sheep findNearestSheep(float range) {
        if (wolf.getWorld() == null) {
            return null;
        }
        Sheep nearest = null;
        float bestDistanceSq = range * range;
        for (Entity entity : wolf.getWorld().getEntities()) {
            if (!(entity instanceof Sheep sheep) || !isValidTarget(sheep, range)) {
                continue;
            }
            float distanceSq = wolf.distanceToSquared(sheep);
            if (distanceSq < bestDistanceSq) {
                bestDistanceSq = distanceSq;
                nearest = sheep;
            }
        }
        return nearest;
    }

    private boolean isValidTarget(Sheep sheep, float range) {
        return wolf.canHuntSheep()
                && sheep != null
                && !sheep.isDead()
                && !sheep.isRemoved()
                && wolf.distanceToSquared(sheep) <= range * range;
    }

    private boolean hasLineOfSight(Sheep sheep) {
        return LineOfSightUtil.hasLineOfSight(
                wolf.getWorld(),
                wolf.getX(), wolf.getY() + wolf.getHeight() * 0.85f, wolf.getZ(),
                sheep.getX(), sheep.getY() + sheep.getHeight() * 0.85f, sheep.getZ());
    }
}
