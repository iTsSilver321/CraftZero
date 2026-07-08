package com.craftzero.entity.ai;

import com.craftzero.entity.mob.Mob;
import com.craftzero.world.BlockShape;
import com.craftzero.world.BlockType;
import com.craftzero.world.RedstoneEngine;
import com.craftzero.world.World;

/**
 * Release-era door helper used by villagers so passable wooden doors are also
 * opened in-world while the mob is moving through them.
 */
public class OpenDoorGoal implements Goal {

    private static final int CLOSE_DELAY_TICKS = 20;
    private static final int REOPEN_COOLDOWN_TICKS = 10;
    private static final float MOVEMENT_THRESHOLD_SQ = 0.0004f;
    private static final float[] FORWARD_PROBES = { 0.35f, 0.75f, 1.1f };

    private final Mob mob;
    private final MobAI ai;
    private final boolean closeDoor;

    private DoorTarget door;
    private int closeTicks;
    private int reopenCooldown;
    private boolean openedByGoal;

    public OpenDoorGoal(Mob mob, MobAI ai, boolean closeDoor) {
        this.mob = mob;
        this.ai = ai;
        this.closeDoor = closeDoor;
    }

    @Override
    public int getPriority() {
        return 2;
    }

    @Override
    public boolean canUse() {
        if (reopenCooldown > 0) {
            reopenCooldown--;
            return false;
        }
        if (!hasMovementIntent()) {
            return false;
        }

        door = findClosedWoodenDoor();
        return door != null;
    }

    @Override
    public boolean canContinue() {
        return closeDoor && openedByGoal && closeTicks > 0 && door != null && isWoodenDoor(door);
    }

    @Override
    public void start() {
        closeTicks = CLOSE_DELAY_TICKS;
        openedByGoal = door != null && mob.getWorld() != null
                && mob.getWorld().setWoodenDoorOpen(door.x(), door.y(), door.z(), true);
    }

    @Override
    public void tick() {
        if (closeTicks > 0) {
            closeTicks--;
        }
    }

    @Override
    public void stop() {
        if (closeDoor && openedByGoal && door != null && mob.getWorld() != null) {
            mob.getWorld().setWoodenDoorOpen(door.x(), door.y(), door.z(), false);
            reopenCooldown = REOPEN_COOLDOWN_TICKS;
        }
        door = null;
        openedByGoal = false;
        closeTicks = 0;
    }

    @Override
    public boolean isExclusive() {
        return false;
    }

    private boolean hasMovementIntent() {
        float motionSq = mob.getMotionX() * mob.getMotionX() + mob.getMotionZ() * mob.getMotionZ();
        return motionSq > MOVEMENT_THRESHOLD_SQ || ai.hasMoveTarget() || ai.isNavigating();
    }

    private DoorTarget findClosedWoodenDoor() {
        World world = mob.getWorld();
        if (world == null) {
            return null;
        }

        float yawRadians = (float) Math.toRadians(mob.getYaw());
        float forwardX = (float) Math.sin(yawRadians);
        float forwardZ = -(float) Math.cos(yawRadians);
        int baseY = (int) Math.floor(mob.getY());

        for (float probe : FORWARD_PROBES) {
            int x = (int) Math.floor(mob.getX() + forwardX * probe);
            int z = (int) Math.floor(mob.getZ() + forwardZ * probe);
            DoorTarget target = closedDoorAt(world, x, baseY, z);
            if (target != null) {
                return target;
            }
            target = closedDoorAt(world, x, baseY + 1, z);
            if (target != null) {
                return target;
            }
        }

        return null;
    }

    private static DoorTarget closedDoorAt(World world, int x, int y, int z) {
        BlockType type = world.getBlockIfLoaded(x, y, z, BlockType.AIR);
        if (type != BlockType.WOODEN_DOOR) {
            return null;
        }

        int metadata = world.getBlockMetadataIfLoaded(x, y, z, 0);
        int lowerY = BlockShape.isDoorUpper(metadata) ? y - 1 : y;
        BlockType lowerType = world.getBlockIfLoaded(x, lowerY, z, BlockType.AIR);
        int lowerMetadata = world.getBlockMetadataIfLoaded(x, lowerY, z, 0);
        if (lowerType != BlockType.WOODEN_DOOR || BlockShape.isDoorUpper(lowerMetadata)) {
            return null;
        }
        if ((lowerMetadata & RedstoneEngine.DOOR_OPEN_BIT) != 0) {
            return null;
        }
        return new DoorTarget(x, lowerY, z);
    }

    private boolean isWoodenDoor(DoorTarget target) {
        World world = mob.getWorld();
        return world != null
                && world.getBlockIfLoaded(target.x(), target.y(), target.z(), BlockType.AIR) == BlockType.WOODEN_DOOR;
    }

    private record DoorTarget(int x, int y, int z) {
    }
}
