package com.craftzero.entity.ai;

import com.craftzero.entity.mob.Wolf;
import com.craftzero.main.Player;
import com.craftzero.physics.AABB;
import com.craftzero.world.BlockShape;
import com.craftzero.world.BlockType;
import com.craftzero.world.World;

/**
 * Release-era tamed wolf owner following for the single-player owner model.
 */
public class FollowOwnerGoal implements Goal {
    private static final float START_DISTANCE_SQ = 6.0f * 6.0f;
    private static final float STOP_DISTANCE_SQ = 2.0f * 2.0f;
    private static final float TELEPORT_DISTANCE_SQ = 12.0f * 12.0f;
    private static final float FOLLOW_SPEED = 1.0f;
    private static final int TELEPORT_HORIZONTAL_RADIUS = 2;
    private static final int TELEPORT_VERTICAL_RADIUS = 1;
    private static final float TELEPORT_COLLISION_EPSILON = 0.0001f;

    private final Wolf wolf;
    private final MobAI ai;

    public FollowOwnerGoal(Wolf wolf, MobAI ai) {
        this.wolf = wolf;
        this.ai = ai;
    }

    @Override
    public int getPriority() {
        return 6;
    }

    @Override
    public boolean canUse() {
        return hasOwnerToFollow() && distanceToOwnerSq() > START_DISTANCE_SQ;
    }

    @Override
    public boolean canContinue() {
        return hasOwnerToFollow() && distanceToOwnerSq() > STOP_DISTANCE_SQ;
    }

    @Override
    public void tick() {
        OwnerSnapshot owner = owner();
        if (owner == null) {
            return;
        }
        if (distanceToOwnerSq() >= TELEPORT_DISTANCE_SQ && tryTeleportNear(owner)) {
            ai.requestStopMoving();
            return;
        }

        float dx = owner.x() - wolf.getX();
        float dz = owner.z() - wolf.getZ();
        float distance = (float) Math.sqrt(dx * dx + dz * dz);
        if (distance <= 0.001f) {
            ai.requestStopMoving();
            return;
        }
        float targetYaw = (float) Math.toDegrees(Math.atan2(dx, -dz));
        ai.requestMoveDirection(targetYaw, FOLLOW_SPEED);
        wolf.lookAt(owner.x(), owner.eyeY(), owner.z());
    }

    @Override
    public void stop() {
        ai.requestStopMoving();
    }

    private boolean hasOwnerToFollow() {
        return wolf.isTamed()
                && !wolf.isSitting()
                && owner() != null;
    }

    private OwnerSnapshot owner() {
        World world = wolf.getWorld();
        if (world == null) {
            return null;
        }
        Player player = world.getPlayer();
        if (wolf.isOwnedBy(player) && player.getStats().getHealth() > 0) {
            return new OwnerSnapshot(player.getPosition().x, player.getPosition().y, player.getPosition().z,
                    player.getPosition().y + 1.6f);
        }
        for (World.RemotePlayerTarget target : world.remotePlayerViews(
                wolf.getX(), wolf.getY(), wolf.getZ(), Float.MAX_VALUE, false)) {
            if (target != null && target.valid() && wolf.isOwnedByName(target.username())) {
                return new OwnerSnapshot(target.x(), target.y(), target.z(), target.eyeY());
            }
        }
        return null;
    }

    private float distanceToOwnerSq() {
        OwnerSnapshot owner = owner();
        if (owner == null) {
            return Float.MAX_VALUE;
        }
        float dx = owner.x() - wolf.getX();
        float dy = owner.y() - wolf.getY();
        float dz = owner.z() - wolf.getZ();
        return dx * dx + dy * dy + dz * dz;
    }

    private boolean tryTeleportNear(OwnerSnapshot owner) {
        World world = wolf.getWorld();
        if (world == null) {
            return false;
        }
        int baseX = (int) Math.floor(owner.x());
        int baseY = (int) Math.floor(owner.y());
        int baseZ = (int) Math.floor(owner.z());
        for (int dx = -TELEPORT_HORIZONTAL_RADIUS; dx <= TELEPORT_HORIZONTAL_RADIUS; dx++) {
            for (int dz = -TELEPORT_HORIZONTAL_RADIUS; dz <= TELEPORT_HORIZONTAL_RADIUS; dz++) {
                if (Math.abs(dx) < TELEPORT_HORIZONTAL_RADIUS && Math.abs(dz) < TELEPORT_HORIZONTAL_RADIUS) {
                    continue;
                }
                int x = baseX + dx;
                int z = baseZ + dz;
                for (int dy = -TELEPORT_VERTICAL_RADIUS; dy <= TELEPORT_VERTICAL_RADIUS; dy++) {
                    int y = baseY + dy;
                    if (isSafeTeleportSpot(world, x, y, z)) {
                        wolf.setPosition(x + 0.5f, y, z + 0.5f);
                        wolf.setMotion(0.0f, 0.0f, 0.0f);
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private boolean isSafeTeleportSpot(World world, int x, int y, int z) {
        if (!world.isChunkGeneratedForBlock(x, z)) {
            return false;
        }
        BlockType below = world.getBlockIfLoaded(x, y - 1, z, null);
        if (below == null || !BlockShape.isOpaqueCube(below) || isUnsafeTeleportBlock(below)) {
            return false;
        }
        AABB landingBox = wolfBoxAt(x + 0.5f, y, z + 0.5f);
        return !intersectsUnsafeBlock(world, landingBox) && !isBlockedByLoadedCollision(world, landingBox);
    }

    private AABB wolfBoxAt(float x, float y, float z) {
        float halfWidth = wolf.getWidth() * 0.5f;
        return new AABB(
                x - halfWidth, y, z - halfWidth,
                x + halfWidth, y + wolf.getHeight(), z + halfWidth);
    }

    private boolean intersectsUnsafeBlock(World world, AABB box) {
        int minX = (int) Math.floor(box.getMin().x);
        int minY = (int) Math.floor(box.getMin().y);
        int minZ = (int) Math.floor(box.getMin().z);
        int maxX = (int) Math.floor(box.getMax().x - TELEPORT_COLLISION_EPSILON);
        int maxY = (int) Math.floor(box.getMax().y - TELEPORT_COLLISION_EPSILON);
        int maxZ = (int) Math.floor(box.getMax().z - TELEPORT_COLLISION_EPSILON);
        for (int bx = minX; bx <= maxX; bx++) {
            for (int by = minY; by <= maxY; by++) {
                for (int bz = minZ; bz <= maxZ; bz++) {
                    if (!world.isChunkGeneratedForBlock(bx, bz)
                            || isUnsafeTeleportBlock(world.getBlockIfLoaded(bx, by, bz, null))) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private boolean isBlockedByLoadedCollision(World world, AABB box) {
        int minX = (int) Math.floor(box.getMin().x);
        int minY = (int) Math.floor(box.getMin().y);
        int minZ = (int) Math.floor(box.getMin().z);
        int maxX = (int) Math.floor(box.getMax().x - TELEPORT_COLLISION_EPSILON);
        int maxY = (int) Math.floor(box.getMax().y - TELEPORT_COLLISION_EPSILON);
        int maxZ = (int) Math.floor(box.getMax().z - TELEPORT_COLLISION_EPSILON);
        for (int bx = minX; bx <= maxX; bx++) {
            for (int by = minY; by <= maxY; by++) {
                for (int bz = minZ; bz <= maxZ; bz++) {
                    for (AABB collision : world.getCollisionBoxesIfLoaded(bx, by, bz)) {
                        if (box.intersects(collision)) {
                            return true;
                        }
                    }
                }
            }
        }
        for (AABB collision : world.getMovingPistonCollisionBoxes(box)) {
            if (box.intersects(collision)) {
                return true;
            }
        }
        return false;
    }

    private boolean isUnsafeTeleportBlock(BlockType block) {
        return block == null
                || block.isFluid()
                || block == BlockType.FIRE
                || block == BlockType.CACTUS;
    }

    private record OwnerSnapshot(float x, float y, float z, float eyeY) {
    }
}
