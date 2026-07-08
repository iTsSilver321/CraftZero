package com.craftzero.entity.ai;

import com.craftzero.combat.DamageSource;
import com.craftzero.entity.LivingEntity;
import com.craftzero.entity.mob.Wolf;
import com.craftzero.main.CombatRules;
import com.craftzero.world.World;

/**
 * Tamed wolf combat assistance against the entity selected by owner actions.
 */
public class WolfAttackTargetGoal implements Goal {
    private static final float ATTACK_RANGE = 1.6f;
    private static final float CHASE_SPEED = 1.2f;
    private static final int PATH_RECALC_INTERVAL = 10;

    private final Wolf wolf;
    private final MobAI ai;
    private int pathRecalcCooldown;

    public WolfAttackTargetGoal(Wolf wolf, MobAI ai) {
        this.wolf = wolf;
        this.ai = ai;
    }

    @Override
    public int getPriority() {
        return 3;
    }

    @Override
    public boolean canUse() {
        return hasValidTarget();
    }

    @Override
    public boolean canContinue() {
        return hasValidTarget();
    }

    @Override
    public void start() {
        pathRecalcCooldown = 0;
    }

    @Override
    public void tick() {
        LivingEntity target = ai.getTarget();
        World.RemotePlayerTarget remoteTarget = ai.getRemotePlayerTarget();
        if (target != null) {
            tickLivingTarget(target);
            return;
        }
        if (remoteTarget != null) {
            tickRemoteTarget(remoteTarget);
            return;
        }
        if (!hasValidTarget()) {
            ai.clearTarget();
            ai.requestStopMoving();
        }
    }

    private void tickLivingTarget(LivingEntity target) {
        if (!hasValidLivingTarget(target)) {
            ai.clearTarget();
            ai.requestStopMoving();
            return;
        }
        wolf.lookAt(target.getX(), target.getY() + target.getHeight() * 0.85f, target.getZ());
        float dx = target.getX() - wolf.getX();
        float dy = target.getY() - wolf.getY();
        float dz = target.getZ() - wolf.getZ();
        float distance = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (distance <= ATTACK_RANGE && wolf.canAttack()
                && (distance <= 1.0f || hasLineOfSight(target))) {
            wolf.performAttack();
            target.damage(Wolf.TAMED_ATTACK_DAMAGE,
                    DamageSource.entity(DamageSource.Type.MOB_MELEE, wolf, 0.2f, 0.1f));
            return;
        }

        pathRecalcCooldown--;
        if (pathRecalcCooldown <= 0) {
            ai.setMoveTarget(target.getX(), target.getY(), target.getZ());
            pathRecalcCooldown = PATH_RECALC_INTERVAL;
        }
        if (distance > 0.001f) {
            ai.requestMoveToward(target.getX(), target.getZ(), CHASE_SPEED, ATTACK_RANGE * 0.5f, 1.5f);
        }
    }

    private void tickRemoteTarget(World.RemotePlayerTarget target) {
        if (!hasValidRemoteTarget(target)) {
            ai.clearRemotePlayerTarget();
            ai.requestStopMoving();
            return;
        }
        wolf.lookAt(target.x(), target.eyeY(), target.z());
        float dx = target.x() - wolf.getX();
        float dy = target.y() - wolf.getY();
        float dz = target.z() - wolf.getZ();
        float distance = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (distance <= ATTACK_RANGE && wolf.canAttack()
                && (distance <= 1.0f || hasLineOfSight(target))) {
            wolf.performAttack();
            wolf.getWorld().damageRemotePlayerTarget(target.playerId(),
                    new World.RemotePlayerDamage(
                            Wolf.TAMED_ATTACK_DAMAGE,
                            "mob_melee",
                            wolf.getX(), wolf.getY(), wolf.getZ(),
                            CombatRules.MOB_MELEE_HORIZONTAL_KNOCKBACK,
                            CombatRules.MOB_MELEE_VERTICAL_KNOCKBACK,
                            0));
            return;
        }

        pathRecalcCooldown--;
        if (pathRecalcCooldown <= 0) {
            ai.setMoveTarget(target.x(), target.y(), target.z());
            pathRecalcCooldown = PATH_RECALC_INTERVAL;
        }
        if (distance > 0.001f) {
            ai.requestMoveToward(target.x(), target.z(), CHASE_SPEED, ATTACK_RANGE * 0.5f, 1.5f);
        }
    }

    @Override
    public void stop() {
        ai.requestStopMoving();
    }

    private boolean hasValidTarget() {
        LivingEntity target = ai.getTarget();
        World.RemotePlayerTarget remoteTarget = ai.getRemotePlayerTarget();
        return hasValidLivingTarget(target) || hasValidRemoteTarget(remoteTarget);
    }

    private boolean hasValidLivingTarget(LivingEntity target) {
        return wolf.canAssistCombat()
                && target != null
                && target != wolf
                && !target.isDead()
                && !target.isRemoved()
                && wolf.distanceToSquared(target) <= Wolf.ASSIST_RANGE * Wolf.ASSIST_RANGE;
    }

    private boolean hasValidRemoteTarget(World.RemotePlayerTarget target) {
        if (!wolf.canAssistCombat() || target == null || !target.valid()) {
            return false;
        }
        float dx = target.x() - wolf.getX();
        float dy = target.y() - wolf.getY();
        float dz = target.z() - wolf.getZ();
        return dx * dx + dy * dy + dz * dz <= Wolf.ASSIST_RANGE * Wolf.ASSIST_RANGE;
    }

    private boolean hasLineOfSight(LivingEntity target) {
        return LineOfSightUtil.hasLineOfSight(
                wolf.getWorld(),
                wolf.getX(), wolf.getY() + wolf.getHeight() * 0.85f, wolf.getZ(),
                target.getX(), target.getY() + target.getHeight() * 0.85f, target.getZ());
    }

    private boolean hasLineOfSight(World.RemotePlayerTarget target) {
        return target != null
                && LineOfSightUtil.hasLineOfSight(
                        wolf.getWorld(),
                        wolf.getX(), wolf.getY() + wolf.getHeight() * 0.85f, wolf.getZ(),
                        target.x(), target.eyeY(), target.z());
    }
}
