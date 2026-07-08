package com.craftzero.entity.ai;

import com.craftzero.entity.mob.Mob;
import com.craftzero.inventory.ItemStack;
import com.craftzero.main.Player;
import com.craftzero.world.World;
import org.joml.Vector3f;

/**
 * Release 1.0-style animal temptation: breedable animals follow a nearby player
 * holding their breeding item.
 */
public class FollowBreedingItemGoal implements Goal {

    private static final float STOP_DISTANCE = 2.5f;

    private final Mob mob;
    private final MobAI ai;
    private final float range;
    private final float speed;
    private Player targetPlayer;
    private World.RemotePlayerTarget targetRemotePlayer;

    public FollowBreedingItemGoal(Mob mob, MobAI ai, float range, float speed) {
        this.mob = mob;
        this.ai = ai;
        this.range = range;
        this.speed = speed;
    }

    @Override
    public boolean canUse() {
        Player player = findTemptingPlayer(range);
        World.RemotePlayerTarget remotePlayer = findTemptingRemotePlayer(range);
        if (player == null && remotePlayer == null) {
            return false;
        }
        if (shouldUseRemoteTarget(player, remotePlayer)) {
            targetPlayer = null;
            targetRemotePlayer = remotePlayer;
        } else {
            targetPlayer = player;
            targetRemotePlayer = null;
        }
        return true;
    }

    @Override
    public boolean canContinue() {
        if (targetPlayer != null) {
            return canFollow(targetPlayer, range * 1.25f);
        }
        if (targetRemotePlayer == null || mob.getWorld() == null) {
            return false;
        }
            targetRemotePlayer = mob.getWorld().remotePlayerViewById(targetRemotePlayer.playerId());
        return canFollow(targetRemotePlayer, range * 1.25f);
    }

    @Override
    public void tick() {
        if (targetPlayer != null) {
            tickToward(targetPlayer.getPosition().x, targetPlayer.getPosition().y + 1.6f,
                    targetPlayer.getPosition().z);
            return;
        }
        if (targetRemotePlayer != null) {
            tickToward(targetRemotePlayer.x(), targetRemotePlayer.eyeY(), targetRemotePlayer.z());
            return;
        }
        ai.requestStopMoving();
    }

    private void tickToward(float targetX, float targetEyeY, float targetZ) {
        mob.lookAt(targetX, targetEyeY, targetZ);

        float dx = targetX - mob.getX();
        float dz = targetZ - mob.getZ();
        float distanceSq = dx * dx + dz * dz;
        if (distanceSq <= STOP_DISTANCE * STOP_DISTANCE) {
            ai.requestStopMoving();
            return;
        }

        float yaw = (float) Math.toDegrees(Math.atan2(dx, -dz));
        ai.requestMoveDirection(yaw, speed);
    }

    @Override
    public void stop() {
        targetPlayer = null;
        targetRemotePlayer = null;
        ai.requestStopMoving();
    }

    private Player findTemptingPlayer(float maxRange) {
        if (mob.getWorld() == null || !mob.canFollowBreedingItem()) {
            return null;
        }
        Player player = mob.getWorld().getPlayer();
        if (player == null || !canFollow(player, maxRange)) {
            return null;
        }
        return player;
    }

    private World.RemotePlayerTarget findTemptingRemotePlayer(float maxRange) {
        if (mob.getWorld() == null || !mob.canFollowBreedingItem()) {
            return null;
        }
        for (World.RemotePlayerTarget target : mob.getWorld().remotePlayerViews(
                mob.getX(), mob.getY(), mob.getZ(), maxRange, false)) {
            if (canFollow(target, maxRange)) {
                return target;
            }
        }
        return null;
    }

    private boolean shouldUseRemoteTarget(Player player, World.RemotePlayerTarget remotePlayer) {
        if (remotePlayer == null) {
            return false;
        }
        if (player == null) {
            return true;
        }
        return remotePlayer.distance() <= distanceToPlayer(player);
    }

    private boolean canFollow(Player player, float maxRange) {
        if (!isHoldingTemptingItem(player)) {
            return false;
        }
        Vector3f position = player.getPosition();
        float dx = position.x - mob.getX();
        float dy = position.y - mob.getY();
        float dz = position.z - mob.getZ();
        return dx * dx + dy * dy + dz * dz <= maxRange * maxRange;
    }

    private boolean canFollow(World.RemotePlayerTarget target, float maxRange) {
        if (target == null || !target.valid()
                || target.heldItem() == null
                || !mob.isTemptedByItem(target.heldItem())) {
            return false;
        }
        float dx = target.x() - mob.getX();
        float dy = target.y() - mob.getY();
        float dz = target.z() - mob.getZ();
        return dx * dx + dy * dy + dz * dz <= maxRange * maxRange;
    }

    private float distanceToPlayer(Player player) {
        if (player == null) {
            return Float.MAX_VALUE;
        }
        Vector3f position = player.getPosition();
        float dx = position.x - mob.getX();
        float dy = position.y - mob.getY();
        float dz = position.z - mob.getZ();
        return (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    private boolean isHoldingTemptingItem(Player player) {
        if (player == null || player.getInventory() == null) {
            return false;
        }
        ItemStack stack = player.getInventory().getItemInHand();
        return stack != null && !stack.isEmpty() && mob.isTemptedByItem(stack.getType());
    }
}
