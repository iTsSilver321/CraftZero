package com.craftzero.entity.ai;

import com.craftzero.entity.ArrowEntity;
import com.craftzero.entity.LivingEntity;
import com.craftzero.entity.mob.Mob;
import com.craftzero.main.CombatRules;
import com.craftzero.main.Player;
import com.craftzero.world.World;

/**
 * AI Goal: Attack from range with projectiles (arrows, fireballs, etc.)
 * Used by Skeletons to shoot arrows at players.
 */
public class RangedAttackGoal implements Goal {
    public record State(int attackCooldown, int strafeTime, boolean strafingClockwise, float strafeSpeed) {
    }

    private final Mob mob;
    private final MobAI ai;
    private final float attackRange;
    private final float minRange; // Don't shoot if too close
    private final int attackInterval; // Ticks between attacks
    private final float projectileSpeed;
    private final int initialAttackCooldown;
    private final boolean strafeInRange;
    private final boolean releaseOneSkeletonArrow;

    private int attackCooldown;
    private int strafeTime;
    private boolean strafingClockwise;
    private float strafeSpeed;
    private boolean resumeRestoredState;

    public RangedAttackGoal(Mob mob, MobAI ai, float attackRange, int attackInterval) {
        this(mob, ai, attackRange, attackInterval, 4.0f, 1.5f, attackInterval, true, false);
    }

    public static RangedAttackGoal releaseOneSkeleton(Mob mob, MobAI ai) {
        return new RangedAttackGoal(mob, ai, 10.0f, 30, 0.0f, 0.6f, 0, false, true);
    }

    private RangedAttackGoal(Mob mob, MobAI ai, float attackRange, int attackInterval, float minRange,
            float projectileSpeed, int initialAttackCooldown, boolean strafeInRange,
            boolean releaseOneSkeletonArrow) {
        this.mob = mob;
        this.ai = ai;
        this.attackRange = attackRange;
        this.minRange = minRange;
        this.attackInterval = attackInterval;
        this.projectileSpeed = projectileSpeed;
        this.initialAttackCooldown = Math.max(0, initialAttackCooldown);
        this.strafeInRange = strafeInRange;
        this.releaseOneSkeletonArrow = releaseOneSkeletonArrow;
        this.attackCooldown = 0;
        this.strafeTime = 0;
    }

    @Override
    public int getPriority() {
        return 3; // Same as melee attack - they're alternatives
    }

    @Override
    public boolean canUse() {
        return ai.hasMoveTarget() || hasLivingTarget() || ai.hasRemotePlayerTarget();
    }

    @Override
    public boolean canContinue() {
        return ai.hasMoveTarget() || hasLivingTarget() || ai.hasRemotePlayerTarget();
    }

    @Override
    public void start() {
        if (resumeRestoredState) {
            resumeRestoredState = false;
            return;
        }
        attackCooldown = initialAttackCooldown;
        strafeTime = 0;
        if (strafeInRange) {
            strafingClockwise = mob.getRandom().nextFloat() > 0.5f;
        }
        strafeSpeed = 0.5f;
    }

    public State getState() {
        return new State(attackCooldown, strafeTime, strafingClockwise, strafeSpeed);
    }

    public void restoreState(State state, boolean activeAtSave) {
        if (state == null) {
            return;
        }
        attackCooldown = Math.max(0, state.attackCooldown());
        strafeTime = Math.max(0, state.strafeTime());
        strafingClockwise = state.strafingClockwise();
        strafeSpeed = state.strafeSpeed() > 0.0f ? state.strafeSpeed() : 0.5f;
        resumeRestoredState = activeAtSave;
    }

    public boolean hasRestoredActiveState() {
        return resumeRestoredState;
    }

    @Override
    public void tick() {
        if (mob.getWorld() == null)
            return;

        LivingEntity target = ai.getTarget();
        if (target != null) {
            if (!isValidLivingTarget(target)) {
                ai.clearTarget();
                ai.clearMoveTarget();
                ai.requestStopMoving();
                return;
            }
            tickLivingTarget(target);
            return;
        }

        World.RemotePlayerTarget remoteTarget = ai.getRemotePlayerTarget();
        if (remoteTarget != null) {
            tickRemoteTarget(remoteTarget);
            return;
        }

        Player player = mob.getWorld().getPlayer();
        if (player == null || player.isCreative() || !player.getDifficulty().allowsHostileSpawns())
            return;

        float targetX = player.getPosition().x;
        float targetY = player.getPosition().y;
        float targetZ = player.getPosition().z;

        // Calculate distance
        float dx = targetX - mob.getX();
        float dy = targetY - mob.getY();
        float dz = targetZ - mob.getZ();
        float dist = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        boolean hasSight = hasLineOfSight(player);

        // Look at player
        mob.lookAt(targetX, targetY + 1.6f, targetZ);

        // Movement behavior
        if (dist <= attackRange && dist >= minRange && hasSight) {
            if (strafeInRange) {
                // In attack range - strafe instead of approaching
                strafeTime++;

                // Change strafe direction occasionally
                if (strafeTime >= 20) {
                    if (mob.getRandom().nextFloat() < 0.3f) {
                        strafingClockwise = !strafingClockwise;
                        strafeTime = 0;
                    }
                }

                // Calculate strafe direction (perpendicular to target)
                float targetYaw = (float) Math.toDegrees(Math.atan2(dx, -dz));
                float strafeYaw = targetYaw + (strafingClockwise ? 90 : -90);

                ai.requestSafeMoveDirection(strafeYaw, strafeSpeed, 1.5f);
            } else {
                ai.requestStopMoving();
            }

        } else if (dist > attackRange || !hasSight) {
            ai.setMoveTarget(targetX, targetY, targetZ);

        } else if (dist < minRange) {
            // Too close - back away
            float awayYaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
            ai.requestSafeMoveDirection(awayYaw, 0.6f, 1.5f);
        }

        // Attack logic
        attackCooldown--;
        if (attackCooldown <= 0 && dist <= attackRange && dist >= minRange && hasSight) {
            // Fire projectile!
            shootArrow(player);
            attackCooldown = attackInterval;
        }
    }

    private void tickLivingTarget(LivingEntity target) {
        float targetX = target.getX();
        float targetY = target.getY();
        float targetZ = target.getZ();

        float dx = targetX - mob.getX();
        float dy = targetY - mob.getY();
        float dz = targetZ - mob.getZ();
        float dist = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        boolean hasSight = hasLineOfSight(target);

        mob.lookAt(targetX, targetY + target.getHeight() * 0.85f, targetZ);

        if (dist <= attackRange && dist >= minRange && hasSight) {
            if (strafeInRange) {
                strafeTime++;
                if (strafeTime >= 20 && mob.getRandom().nextFloat() < 0.3f) {
                    strafingClockwise = !strafingClockwise;
                    strafeTime = 0;
                }

                float targetYaw = (float) Math.toDegrees(Math.atan2(dx, -dz));
                float strafeYaw = targetYaw + (strafingClockwise ? 90 : -90);
                ai.requestSafeMoveDirection(strafeYaw, strafeSpeed, 1.5f);
            } else {
                ai.requestStopMoving();
            }
        } else if (dist > attackRange || !hasSight) {
            ai.setMoveTarget(targetX, targetY, targetZ);
        } else if (dist < minRange) {
            float awayYaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
            ai.requestSafeMoveDirection(awayYaw, 0.6f, 1.5f);
        }

        attackCooldown--;
        if (attackCooldown <= 0 && dist <= attackRange && dist >= minRange && hasSight) {
            shootArrow(target);
            attackCooldown = attackInterval;
        }
    }

    private void tickRemoteTarget(World.RemotePlayerTarget target) {
        float targetX = target.x();
        float targetY = target.eyeY();
        float targetZ = target.z();

        float dx = targetX - mob.getX();
        float dy = targetY - (mob.getY() + mob.getHeight() * 0.85f);
        float dz = targetZ - mob.getZ();
        float dist = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        boolean hasSight = hasLineOfSight(target);

        mob.lookAt(targetX, targetY, targetZ);

        if (dist <= attackRange && dist >= minRange && hasSight) {
            if (strafeInRange) {
                strafeTime++;
                if (strafeTime >= 20 && mob.getRandom().nextFloat() < 0.3f) {
                    strafingClockwise = !strafingClockwise;
                    strafeTime = 0;
                }

                float targetYaw = (float) Math.toDegrees(Math.atan2(dx, -dz));
                float strafeYaw = targetYaw + (strafingClockwise ? 90 : -90);
                ai.requestSafeMoveDirection(strafeYaw, strafeSpeed, 1.5f);
            } else {
                ai.requestStopMoving();
            }
        } else if (dist > attackRange || !hasSight) {
            ai.setMoveTarget(target.x(), target.y(), target.z());
        } else if (dist < minRange) {
            float awayYaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
            ai.requestSafeMoveDirection(awayYaw, 0.6f, 1.5f);
        }

        attackCooldown--;
        if (attackCooldown <= 0 && dist <= attackRange && dist >= minRange && hasSight) {
            shootArrow(target);
            attackCooldown = attackInterval;
        }
    }

    /**
     * Shoot an arrow at the target.
     */
    private void shootArrow(Player target) {
        if (mob.getWorld() == null)
            return;

        shootArrowAt(target.getPosition().x,
                releaseOneSkeletonArrow ? target.getPosition().y + 1.42f : target.getPosition().y + 1.0f,
                target.getPosition().z);
    }

    private void shootArrow(LivingEntity target) {
        if (target == null) {
            return;
        }
        shootArrowAt(target.getX(), target.getY() + target.getHeight() * 0.85f, target.getZ());
    }

    private void shootArrow(World.RemotePlayerTarget target) {
        if (target == null) {
            return;
        }
        shootArrowAt(target.x(),
                releaseOneSkeletonArrow ? target.y() + 1.42f : target.eyeY(),
                target.z());
    }

    private void shootArrowAt(float targetX, float targetY, float targetZ) {
        if (mob.getWorld() == null)
            return;

        float spawnX = mob.getX();
        float spawnY = releaseOneSkeletonArrow ? mob.getY() + 1.4f : mob.getY() + mob.getHeight() * 0.75f;
        float spawnZ = mob.getZ();

        float dx = targetX - spawnX;
        float dy = targetY - spawnY;
        float dz = targetZ - spawnZ;
        if (releaseOneSkeletonArrow) {
            dy += Math.sqrt(dx * dx + dz * dz) * 0.2f;
        }
        float dist = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);

        // Normalize and apply speed
        dx = (dx / dist) * projectileSpeed;
        dy = (dy / dist) * projectileSpeed + (releaseOneSkeletonArrow ? 0.0f : 0.1f);
        dz = (dz / dist) * projectileSpeed;

        ArrowEntity arrow = mob.getWorld().spawnArrow(spawnX, spawnY, spawnZ, dx, dy, dz, mob, false,
                CombatRules.EASY_SKELETON_ARROW_DAMAGE);
        if (mob.isOnFire()) {
            arrow.setFireTicksOnHit(100);
        }
        mob.getWorld().playBowSound(spawnX, spawnY, spawnZ);

        // Visual feedback - attack animation
        mob.performAttack();
    }

    private boolean hasLineOfSight(Player player) {
        return LineOfSightUtil.hasLineOfSight(
                mob.getWorld(),
                mob.getX(), mob.getY() + mob.getHeight() * 0.85f, mob.getZ(),
                player.getPosition().x, player.getPosition().y + 1.6f, player.getPosition().z);
    }

    private boolean hasLineOfSight(LivingEntity target) {
        return LineOfSightUtil.hasLineOfSight(
                mob.getWorld(),
                mob.getX(), mob.getY() + mob.getHeight() * 0.85f, mob.getZ(),
                target.getX(), target.getY() + target.getHeight() * 0.85f, target.getZ());
    }

    private boolean hasLineOfSight(World.RemotePlayerTarget target) {
        return target != null
                && LineOfSightUtil.hasLineOfSight(
                        mob.getWorld(),
                        mob.getX(), mob.getY() + mob.getHeight() * 0.85f, mob.getZ(),
                        target.x(), target.eyeY(), target.z());
    }

    private boolean hasLivingTarget() {
        return isValidLivingTarget(ai.getTarget());
    }

    private boolean isValidLivingTarget(LivingEntity target) {
        return target != null
                && target != mob
                && !target.isDead()
                && !target.isRemoved();
    }

    @Override
    public void stop() {
        ai.requestStopMoving();
        attackCooldown = attackInterval;
    }

    @Override
    public boolean isExclusive() {
        return true; // Don't melee while shooting
    }
}
