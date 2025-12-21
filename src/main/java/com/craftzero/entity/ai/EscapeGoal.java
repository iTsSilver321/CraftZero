package com.craftzero.entity.ai;

import com.craftzero.entity.LivingEntity;
import com.craftzero.world.BlockType;
import com.craftzero.world.World;

/**
 * AI Goal: Escape from a confined space (pit, trap, hole).
 * Activates when mob detects it's surrounded by walls.
 * Systematically scans for escape routes instead of random wandering.
 */
public class EscapeGoal implements Goal {

    private final LivingEntity mob;
    private final MobAI ai;
    private final float speed;

    private float escapeX, escapeY, escapeZ;
    private boolean hasEscape;
    private int scanCooldown;
    private int trappedTicks;

    private static final int SCAN_INTERVAL = 10; // Scan every 10 ticks (0.5 seconds) - less frantic
    private static final int TRAPPED_THRESHOLD = 40; // Consider trapped after 2 seconds of not moving

    public EscapeGoal(LivingEntity mob, MobAI ai, float speed) {
        this.mob = mob;
        this.ai = ai;
        this.speed = speed;
        this.hasEscape = false;
        this.scanCooldown = 0;
        this.trappedTicks = 0;
    }

    @Override
    public int getPriority() {
        return 1; // High priority - escaping is important
    }

    @Override
    public boolean canUse() {
        // Check if mob seems trapped (in pit or confined)
        if (mob.isTrapped() || isInPit()) {
            trappedTicks++;
            if (trappedTicks > TRAPPED_THRESHOLD) {
                return true;
            }
        } else {
            trappedTicks = 0;
        }
        return false;
    }

    @Override
    public boolean canContinue() {
        // Continue until we've escaped or found no route
        if (!hasEscape) {
            return trappedTicks > 0;
        }

        // Check if we've reached escape point
        float dx = escapeX - mob.getX();
        float dz = escapeZ - mob.getZ();
        float dist = (float) Math.sqrt(dx * dx + dz * dz);

        return dist > 1.0f;
    }

    @Override
    public void start() {
        hasEscape = false;
        scanCooldown = 0;
    }

    @Override
    public void tick() {
        scanCooldown--;

        if (scanCooldown <= 0) {
            // Systematically scan for escape routes
            if (findEscapeRoute()) {
                // Found escape! Navigate to it
                ai.navigateTo(escapeX, escapeY, escapeZ);
            }
            scanCooldown = SCAN_INTERVAL;
        }

        if (hasEscape) {
            // Move toward escape point
            float dx = escapeX - mob.getX();
            float dz = escapeZ - mob.getZ();
            float targetYaw = (float) Math.toDegrees(Math.atan2(dx, -dz));
            mob.setMoveDirection(targetYaw, speed);

            // Try to jump if escape is above us
            if (escapeY > mob.getY() + 0.5f && mob.isOnGround()) {
                mob.jump();
            }
        }
    }

    @Override
    public void stop() {
        hasEscape = false;
        trappedTicks = 0;
        mob.stopMoving();
    }

    /**
     * Check if mob is in a pit (surrounded by walls or drops).
     */
    private boolean isInPit() {
        World world = mob.getWorld();
        if (world == null)
            return false;

        int x = (int) Math.floor(mob.getX());
        int y = (int) Math.floor(mob.getY());
        int z = (int) Math.floor(mob.getZ());

        // Count blocked directions
        int blockedDirections = 0;
        int[][] directions = { { 1, 0 }, { -1, 0 }, { 0, 1 }, { 0, -1 } };

        for (int[] dir : directions) {
            int nx = x + dir[0];
            int nz = z + dir[1];

            // Check if direction is blocked (wall at feet or head level)
            BlockType feetBlock = world.getBlock(nx, y, nz);
            BlockType headBlock = world.getBlock(nx, y + 1, nz);

            if ((feetBlock != null && feetBlock.isSolid()) ||
                    (headBlock != null && headBlock.isSolid())) {
                blockedDirections++;
            }
        }

        // Trapped if 3+ directions are blocked
        return blockedDirections >= 3;
    }

    /**
     * Systematically scan for escape routes.
     * Checks all 8 directions at multiple distances.
     */
    private boolean findEscapeRoute() {
        World world = mob.getWorld();
        if (world == null)
            return false;

        float mobX = mob.getX();
        float mobY = mob.getY();
        float mobZ = mob.getZ();

        // Scan in 16 directions (every 22.5 degrees)
        for (int angleStep = 0; angleStep < 16; angleStep++) {
            float angle = angleStep * 22.5f;
            float rad = (float) Math.toRadians(angle);

            // Check at multiple distances
            for (float dist = 1.5f; dist <= 5.0f; dist += 1.0f) {
                float checkX = mobX + (float) Math.sin(rad) * dist;
                float checkZ = mobZ - (float) Math.cos(rad) * dist;

                // Check multiple Y levels (same level, up 1, up 2)
                for (int dy = 0; dy <= 2; dy++) {
                    float checkY = mobY + dy;

                    if (isValidEscapePoint(world, checkX, checkY, checkZ)) {
                        // Also verify we can path there
                        if (hasPathTo(world, mobX, mobY, mobZ, checkX, checkY, checkZ)) {
                            escapeX = checkX;
                            escapeY = checkY;
                            escapeZ = checkZ;
                            hasEscape = true;
                            return true;
                        }
                    }
                }
            }
        }

        return false;
    }

    /**
     * Check if a point is a valid escape destination.
     */
    private boolean isValidEscapePoint(World world, float x, float y, float z) {
        int bx = (int) Math.floor(x);
        int by = (int) Math.floor(y);
        int bz = (int) Math.floor(z);

        // Must have solid ground
        BlockType ground = world.getBlock(bx, by - 1, bz);
        if (ground == null || !ground.isSolid())
            return false;

        // Must have open space for feet and head
        BlockType feet = world.getBlock(bx, by, bz);
        BlockType head = world.getBlock(bx, by + 1, bz);

        if (feet != null && feet.isSolid())
            return false;
        if (head != null && head.isSolid())
            return false;

        // Must not be in a pit itself
        int blockedDirs = 0;
        int[][] dirs = { { 1, 0 }, { -1, 0 }, { 0, 1 }, { 0, -1 } };
        for (int[] dir : dirs) {
            BlockType b = world.getBlock(bx + dir[0], by, bz + dir[1]);
            if (b != null && b.isSolid())
                blockedDirs++;
        }

        return blockedDirs < 3; // Not trapped at this point
    }

    /**
     * Simple path check - can we walk/jump from start to end?
     */
    private boolean hasPathTo(World world, float x1, float y1, float z1,
            float x2, float y2, float z2) {
        float dx = x2 - x1;
        float dy = y2 - y1;
        float dz = z2 - z1;
        float dist = (float) Math.sqrt(dx * dx + dz * dz);

        if (dist < 0.1f)
            return true;

        // Normalize
        dx /= dist;
        dz /= dist;

        // Check path in steps
        float stepSize = 0.5f;
        float x = x1;
        float y = y1;
        float z = z1;

        for (float d = 0; d < dist; d += stepSize) {
            x += dx * stepSize;
            z += dz * stepSize;

            int bx = (int) Math.floor(x);
            int by = (int) Math.floor(y);
            int bz = (int) Math.floor(z);

            // Check if blocked
            BlockType feet = world.getBlock(bx, by, bz);
            BlockType head = world.getBlock(bx, by + 1, bz);

            if (feet != null && feet.isSolid()) {
                // Can we jump over?
                BlockType above = world.getBlock(bx, by + 2, bz);
                if (above == null || !above.isSolid()) {
                    y += 1; // Assume we can jump
                } else {
                    return false; // Blocked
                }
            }

            if (head != null && head.isSolid()) {
                return false; // Head blocked
            }
        }

        return true;
    }

    @Override
    public boolean isExclusive() {
        return true; // Escape overrides other movement
    }
}
