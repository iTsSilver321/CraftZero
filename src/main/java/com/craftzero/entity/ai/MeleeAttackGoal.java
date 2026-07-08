package com.craftzero.entity.ai;

import com.craftzero.combat.DamageSource;
import com.craftzero.entity.LivingEntity;
import com.craftzero.entity.mob.Mob;
import com.craftzero.main.CombatRules;
import com.craftzero.main.Player;
import com.craftzero.world.World;

import java.util.function.BooleanSupplier;

/**
 * AI Goal: Move toward and attack the current target.
 * Includes cliff-aware pathfinding to avoid walking off edges.
 */
public class MeleeAttackGoal implements Goal {
    public record State(int pathRecalcCooldown, int stuckTicks, float lastX, float lastZ) {
    }

    private final LivingEntity mob;
    private final MobAI ai;
    private final float damage;
    private final float attackRange;
    private final float chaseSpeed;
    private final BooleanSupplier canAttackPredicate;

    private int pathRecalcCooldown;
    private int stuckTicks; // Track if mob is stuck
    private float lastX, lastZ; // Last position to detect stuck
    private boolean resumeRestoredState;
    private static final int PATH_RECALC_INTERVAL = 10;
    private static final int STUCK_THRESHOLD = 30;

    public MeleeAttackGoal(LivingEntity mob, MobAI ai, float damage, float attackRange, float chaseSpeed) {
        this(mob, ai, damage, attackRange, chaseSpeed, () -> true);
    }

    public MeleeAttackGoal(LivingEntity mob, MobAI ai, float damage, float attackRange, float chaseSpeed,
            BooleanSupplier canAttackPredicate) {
        this.mob = mob;
        this.ai = ai;
        this.damage = damage;
        this.attackRange = attackRange;
        this.chaseSpeed = chaseSpeed;
        this.canAttackPredicate = canAttackPredicate == null ? () -> true : canAttackPredicate;
        this.pathRecalcCooldown = 0;
        this.stuckTicks = 0;
    }

    public MeleeAttackGoal(LivingEntity mob, MobAI ai, float damage) {
        this(mob, ai, damage, 1.5f, 1.0f);
    }

    @Override
    public int getPriority() {
        return 3; // High priority when we have a target
    }

    @Override
    public boolean canUse() {
        return canAttackPredicate.getAsBoolean()
                && (ai.hasMoveTarget() || hasLivingTarget() || ai.hasRemotePlayerTarget());
    }

    @Override
    public boolean canContinue() {
        return canAttackPredicate.getAsBoolean()
                && (ai.hasMoveTarget() || hasLivingTarget() || ai.hasRemotePlayerTarget());
    }

    @Override
    public void start() {
        if (resumeRestoredState) {
            resumeRestoredState = false;
            return;
        }
        pathRecalcCooldown = 0;
        stuckTicks = 0;
        lastX = mob.getX();
        lastZ = mob.getZ();
    }

    public State getState() {
        return new State(pathRecalcCooldown, stuckTicks, lastX, lastZ);
    }

    public void restoreState(State state, boolean activeAtSave) {
        if (state == null) {
            return;
        }
        pathRecalcCooldown = Math.max(0, state.pathRecalcCooldown());
        stuckTicks = Math.max(0, state.stuckTicks());
        lastX = state.lastX();
        lastZ = state.lastZ();
        resumeRestoredState = activeAtSave;
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

        float playerX = player.getPosition().x;
        float playerY = player.getPosition().y;
        float playerZ = player.getPosition().z;

        // Look at player
        mob.lookAt(playerX, playerY + 1.6f, playerZ);

        // Calculate distance
        float dx = playerX - mob.getX();
        float dy = playerY - mob.getY();
        float dz = playerZ - mob.getZ();
        float dist = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);

        // Update movement target periodically
        pathRecalcCooldown--;
        if (pathRecalcCooldown <= 0) {
            ai.setMoveTarget(playerX, playerY, playerZ);
            pathRecalcCooldown = PATH_RECALC_INTERVAL;
        }

        // Check if stuck
        float moveDist = (float) Math.sqrt(
                (mob.getX() - lastX) * (mob.getX() - lastX) +
                        (mob.getZ() - lastZ) * (mob.getZ() - lastZ));
        if (moveDist < 0.05f) {
            stuckTicks++;
        } else {
            stuckTicks = 0;
            lastX = mob.getX();
            lastZ = mob.getZ();
        }

        Mob attackingMob = mob instanceof Mob candidate ? candidate : null;

        // Let source-specific pursuit behavior, such as Enderman stare handling or
        // spider leaping, intercept before a close-range swing is applied.
        if (attackingMob != null && attackingMob.onMeleePursuit(player, dist)) {
            ai.requestStopMoving();
        } else if (dist <= attackRange && mob.canAttack() && hasLineOfSight(player)) {
            performAttack(player);
        } else if (mob.isStuckOnLedge()) {
            // Stuck on ledge - immediately try alternate path
            tryAlternatePath();
            mob.clearTrapped();
        } else if (stuckTicks < STUCK_THRESHOLD) {
            // Move toward player (with cliff awareness)
            moveTowardTarget();
        } else {
            // Stuck - try to find alternate path
            tryAlternatePath();
        }
    }

    private void tickLivingTarget(LivingEntity target) {
        float targetX = target.getX();
        float targetY = target.getY();
        float targetZ = target.getZ();

        mob.lookAt(targetX, targetY + target.getHeight() * 0.85f, targetZ);

        float dx = targetX - mob.getX();
        float dy = targetY - mob.getY();
        float dz = targetZ - mob.getZ();
        float dist = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);

        pathRecalcCooldown--;
        if (pathRecalcCooldown <= 0) {
            ai.setMoveTarget(targetX, targetY, targetZ);
            pathRecalcCooldown = PATH_RECALC_INTERVAL;
        }

        float moveDist = (float) Math.sqrt(
                (mob.getX() - lastX) * (mob.getX() - lastX) +
                        (mob.getZ() - lastZ) * (mob.getZ() - lastZ));
        if (moveDist < 0.05f) {
            stuckTicks++;
        } else {
            stuckTicks = 0;
            lastX = mob.getX();
            lastZ = mob.getZ();
        }

        if (dist <= attackRange && mob.canAttack() && hasLineOfSight(target)) {
            performAttack(target);
        } else if (mob.isStuckOnLedge()) {
            tryAlternatePath();
            mob.clearTrapped();
        } else if (stuckTicks < STUCK_THRESHOLD) {
            moveTowardTarget();
        } else {
            tryAlternatePath();
        }
    }

    private void tickRemoteTarget(World.RemotePlayerTarget target) {
        float targetX = target.x();
        float targetY = target.y();
        float targetZ = target.z();

        mob.lookAt(targetX, target.eyeY(), targetZ);

        float dx = targetX - mob.getX();
        float dy = targetY - mob.getY();
        float dz = targetZ - mob.getZ();
        float dist = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);

        pathRecalcCooldown--;
        if (pathRecalcCooldown <= 0) {
            ai.setMoveTarget(targetX, targetY, targetZ);
            pathRecalcCooldown = PATH_RECALC_INTERVAL;
        }

        float moveDist = (float) Math.sqrt(
                (mob.getX() - lastX) * (mob.getX() - lastX) +
                        (mob.getZ() - lastZ) * (mob.getZ() - lastZ));
        if (moveDist < 0.05f) {
            stuckTicks++;
        } else {
            stuckTicks = 0;
            lastX = mob.getX();
            lastZ = mob.getZ();
        }

        Mob attackingMob = mob instanceof Mob candidate ? candidate : null;

        if (attackingMob != null && attackingMob.onRemoteMeleePursuit(target, dist)) {
            ai.requestStopMoving();
        } else if (dist <= attackRange && mob.canAttack() && hasLineOfSight(target)) {
            performAttack(target);
        } else if (mob.isStuckOnLedge()) {
            tryAlternatePath();
            mob.clearTrapped();
        } else if (stuckTicks < STUCK_THRESHOLD) {
            moveTowardTarget();
        } else {
            tryAlternatePath();
        }
    }

    private void moveTowardTarget() {
        // The active move target is already fed to MobAI/Navigator above. Let the
        // navigator's path nodes drive normal pursuit; direct steering is reserved for
        // the alternate-path fallback below so mobs do not constantly cut corners or
        // walk straight into obstacles.
    }

    private void tryAlternatePath() {
        // When stuck, try moving in a random valid direction
        float currentYaw = mob.getBodyYaw();
        float[] testAngles = { 90, -90, 45, -45, 135, -135, 180 };

        for (float offset : testAngles) {
            float testYaw = currentYaw + offset;
            if (ai.requestSafeMoveDirection(testYaw, chaseSpeed * 0.5f, 2.0f)) {
                stuckTicks = 0; // Reset stuck counter
                return;
            }
        }

        // Completely stuck (surrounded by cliffs) - just stop
        ai.requestStopMoving();
    }

    private void performAttack(Player player) {
        mob.performAttack();

        boolean hit = player.hurt(damage, DamageSource.entity(DamageSource.Type.MOB_MELEE, mob,
                CombatRules.MOB_MELEE_HORIZONTAL_KNOCKBACK,
                CombatRules.MOB_MELEE_VERTICAL_KNOCKBACK));
        if (hit && mob instanceof Mob attackingMob) {
            attackingMob.onSuccessfulMeleeHit(player);
        }
    }

    private void performAttack(LivingEntity target) {
        mob.performAttack();
        target.damage(damage, DamageSource.entity(DamageSource.Type.MOB_MELEE, mob,
                CombatRules.MOB_MELEE_HORIZONTAL_KNOCKBACK,
                CombatRules.MOB_MELEE_VERTICAL_KNOCKBACK));
    }

    private void performAttack(World.RemotePlayerTarget target) {
        mob.performAttack();
        boolean hit = mob.getWorld().damageRemotePlayerTarget(target.playerId(),
                new World.RemotePlayerDamage(
                        damage,
                        "mob_melee",
                        mob.getX(), mob.getY(), mob.getZ(),
                        CombatRules.MOB_MELEE_HORIZONTAL_KNOCKBACK,
                        CombatRules.MOB_MELEE_VERTICAL_KNOCKBACK,
                        0));
        if (hit && mob instanceof Mob attackingMob) {
            attackingMob.onSuccessfulRemoteMeleeHit(target);
        }
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
        stuckTicks = 0;
    }
}
