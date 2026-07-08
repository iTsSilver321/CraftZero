package com.craftzero.world;

import com.craftzero.inventory.ItemType;
import com.craftzero.physics.AABB;

/**
 * Short-lived visual effect particle. Particles are transient render state and
 * are intentionally not saved as entities.
 */
public class WorldParticle {
    public static final float RED_DUST_DEFAULT_COLOR_DATA = -1.0f;

    public enum Type {
        HEART,
        EXPLODE,
        SMOKE,
        LARGE_SMOKE,
        SNOWBALL_POOF,
        FLAME,
        SPLASH,
        CRIT,
        MAGIC_CRIT,
        FOOTSTEP,
        NOTE,
        PORTAL,
        MOB_SPELL,
        SPELL,
        INSTANT_SPELL,
        LARGE_EXPLOSION,
        HUGE_EXPLOSION,
        RAIN,
        SNOW,
        BUBBLE,
        SNOW_SHOVEL,
        SLIME,
        BLOCK_CRACK,
        BLOCK_DUST,
        DRIP_WATER,
        DRIP_LAVA,
        LAVA,
        RED_DUST,
        ITEM_CRACK,
        ITEM_PICKUP,
        ENCHANTMENT_TABLE,
        SUSPENDED,
        DEPTH_SUSPEND,
        TOWN_AURA
    }

    private static final int BLOCK_PARTICLE_METADATA_VALUES = 16;
    private static final int BLOCK_PARTICLE_FACES = 6;
    private static final int BLOCK_PARTICLE_STRIDE = BLOCK_PARTICLE_METADATA_VALUES * BLOCK_PARTICLE_FACES;
    private static final float COLLISION_EPSILON = 0.001f;
    private static final float MAX_COLLISION_STEP = 0.25f;
    private static final float SOURCE_SCALE_RAMP = 32.0f;
    public static final float DRIP_BOB_TICKS = 40.0f;
    private static final float DRIP_GRAVITY_PER_TICK = 0.06f;
    private static final float DRIP_BOB_DAMPING = 0.02f;
    private static final float DRIP_DRAG = 0.98f;
    private static final float BUBBLE_ACCELERATION_PER_TICK = 0.002f;
    private static final float BUBBLE_DRAG = 0.85f;
    private static final float CRIT_INPUT_MOTION_SCALE = 0.40f;
    private static final float CRIT_GRAVITY_PER_TICK = 0.02f;
    private static final float CRIT_DRAG = 0.70f;
    private static final float CRIT_GROUND_HORIZONTAL_DRAG = 0.70f;
    private static final float FRAGMENT_GRAVITY_PER_TICK = 0.04f;
    private static final float FRAGMENT_DRAG = 0.98f;
    private static final float SMOKE_ACCELERATION_PER_TICK = 0.004f;
    private static final float SMOKE_DRAG = 0.96f;
    private static final float EXPLODE_DRAG = 0.90f;
    private static final float SPLASH_GRAVITY_PER_TICK = 0.04f;
    private static final float SPLASH_DRAG = 0.98f;
    private static final float LAVA_GRAVITY_PER_TICK = 0.03f;
    private static final float LAVA_DRAG = 0.999f;
    private static final float RAIN_GRAVITY_PER_TICK = 0.06f;
    private static final float RAIN_DRAG = 0.98f;
    private static final float SNOW_SHOVEL_GRAVITY_PER_TICK = 0.03f;
    private static final float FLAME_DRAG = 0.96f;
    private static final float HEART_UPWARD_MOTION_PER_TICK = 0.10f;
    private static final float HEART_DRAG = 0.86f;
    private static final int HEART_SOURCE_LIFETIME_TICKS = 16;
    private static final float NOTE_UPWARD_MOTION_PER_TICK = 0.20f;
    private static final float NOTE_DRAG = 0.66f;
    private static final int NOTE_SOURCE_LIFETIME_TICKS = 6;
    private static final float AURA_INPUT_MOTION_SCALE = 0.02f;
    private static final float AURA_DRAG = 0.99f;
    private static final float GROUND_HORIZONTAL_DAMPING = 0.70f;

    private final Type type;
    private float x;
    private float y;
    private float z;
    private float prevX;
    private float prevY;
    private float prevZ;
    private float motionX;
    private float motionY;
    private float motionZ;
    private final float spawnX;
    private final float spawnY;
    private final float spawnZ;
    private final float baseScale;
    private final float lifetimeTicks;
    private final float data;
    private final boolean hasTarget;
    private final float targetX;
    private final float targetY;
    private final float targetZ;
    private float ageTicks;
    private boolean onGround;
    private boolean waterDripSplashPending;

    public WorldParticle(Type type, float x, float y, float z,
            float motionX, float motionY, float motionZ,
            float baseScale, int lifetimeTicks) {
        this(type, x, y, z, motionX, motionY, motionZ, baseScale, lifetimeTicks, 0.0f);
    }

    public WorldParticle(Type type, float x, float y, float z,
            float motionX, float motionY, float motionZ,
            float baseScale, int lifetimeTicks, float data) {
        this(type, x, y, z, motionX, motionY, motionZ, baseScale, lifetimeTicks, data,
                false, x, y, z);
    }

    public WorldParticle(Type type, float x, float y, float z,
            float motionX, float motionY, float motionZ,
            float baseScale, int lifetimeTicks, float data,
            float targetX, float targetY, float targetZ) {
        this(type, x, y, z, motionX, motionY, motionZ, baseScale, lifetimeTicks, data,
                true, targetX, targetY, targetZ);
    }

    private WorldParticle(Type type, float x, float y, float z,
            float motionX, float motionY, float motionZ,
            float baseScale, int lifetimeTicks, float data,
            boolean hasTarget, float targetX, float targetY, float targetZ) {
        this(type, x, y, z, motionX, motionY, motionZ, baseScale, lifetimeTicks, data,
                hasTarget, targetX, targetY, targetZ, true);
    }

    private WorldParticle(Type type, float x, float y, float z,
            float motionX, float motionY, float motionZ,
            float baseScale, int lifetimeTicks, float data,
            boolean hasTarget, float targetX, float targetY, float targetZ,
            boolean normalizeSourceMotion) {
        this.type = type == null ? Type.SMOKE : type;
        this.x = finiteOrZero(x);
        this.y = finiteOrZero(y);
        this.z = finiteOrZero(z);
        this.prevX = this.x;
        this.prevY = this.y;
        this.prevZ = this.z;
        this.spawnX = this.x;
        this.spawnY = this.y;
        this.spawnZ = this.z;
        float safeMotionX = finiteOrZero(motionX);
        float safeMotionY = finiteOrZero(motionY);
        float safeMotionZ = finiteOrZero(motionZ);
        if (!normalizeSourceMotion) {
            this.motionX = safeMotionX;
            this.motionY = safeMotionY;
            this.motionZ = safeMotionZ;
        } else if (isCritType(this.type)) {
            this.motionX = safeMotionX * CRIT_INPUT_MOTION_SCALE;
            this.motionY = safeMotionY * CRIT_INPUT_MOTION_SCALE;
            this.motionZ = safeMotionZ * CRIT_INPUT_MOTION_SCALE;
        } else if (this.type == Type.HEART) {
            this.motionX = 0.0f;
            this.motionY = HEART_UPWARD_MOTION_PER_TICK;
            this.motionZ = 0.0f;
        } else if (this.type == Type.NOTE) {
            this.motionX = 0.0f;
            this.motionY = NOTE_UPWARD_MOTION_PER_TICK;
            this.motionZ = 0.0f;
        } else if (this.type == Type.SUSPENDED) {
            this.motionX = 0.0f;
            this.motionY = 0.0f;
            this.motionZ = 0.0f;
        } else if (isAuraType(this.type)) {
            this.motionX = safeMotionX * AURA_INPUT_MOTION_SCALE;
            this.motionY = safeMotionY * AURA_INPUT_MOTION_SCALE;
            this.motionZ = safeMotionZ * AURA_INPUT_MOTION_SCALE;
        } else {
            this.motionX = safeMotionX;
            this.motionY = safeMotionY;
            this.motionZ = safeMotionZ;
        }
        this.baseScale = Math.max(0.01f, finiteOrDefault(baseScale, 0.01f));
        this.lifetimeTicks = sourceLifetimeTicks(this.type, lifetimeTicks);
        this.data = finiteOrZero(data);
        this.hasTarget = hasTarget && allFinite(targetX, targetY, targetZ);
        this.targetX = this.hasTarget ? targetX : this.x;
        this.targetY = this.hasTarget ? targetY : this.y;
        this.targetZ = this.hasTarget ? targetZ : this.z;
    }

    public static WorldParticle fromNetwork(Type type, float x, float y, float z,
            float motionX, float motionY, float motionZ,
            float baseScale, int lifetimeTicks, float data,
            boolean hasTarget, float targetX, float targetY, float targetZ) {
        return new WorldParticle(type, x, y, z, motionX, motionY, motionZ, baseScale, lifetimeTicks, data,
                hasTarget, targetX, targetY, targetZ, false);
    }

    public boolean update(float deltaTime) {
        return update(null, deltaTime);
    }

    public boolean update(World world, float deltaTime) {
        if (!Float.isFinite(deltaTime) || deltaTime < 0.0f || !isFinitePosition()) {
            return true;
        }
        prevX = x;
        prevY = y;
        prevZ = z;
        onGround = false;

        float tickDelta = Math.max(0.0f, deltaTime) * 20.0f;
        if (!Float.isFinite(tickDelta)) {
            return true;
        }
        if (type == Type.PORTAL) {
            return updatePortal(tickDelta);
        } else if (type == Type.HEART) {
            return updateRisingSourceParticle(world, tickDelta, HEART_DRAG);
        } else if (type == Type.NOTE) {
            return updateRisingSourceParticle(world, tickDelta, NOTE_DRAG);
        } else if (isAuraType(type)) {
            return updateAuraParticle(world, tickDelta);
        } else if (isFragmentType(type)) {
            return updateFragmentParticle(world, tickDelta);
        } else if (isSmokeLiftType(type)) {
            float drag = type == Type.EXPLODE ? EXPLODE_DRAG : SMOKE_DRAG;
            return updateDriftingParticle(world, tickDelta, SMOKE_ACCELERATION_PER_TICK, drag, true);
        } else if (type == Type.SPELL || type == Type.INSTANT_SPELL) {
            return updateDriftingParticle(world, tickDelta, SMOKE_ACCELERATION_PER_TICK, SMOKE_DRAG, true);
        } else if (type == Type.SPLASH) {
            return updateDriftingParticle(world, tickDelta, -SPLASH_GRAVITY_PER_TICK, SPLASH_DRAG, false);
        } else if (type == Type.LAVA) {
            return updateDriftingParticle(world, tickDelta, -LAVA_GRAVITY_PER_TICK, LAVA_DRAG, false);
        } else if (type == Type.SNOW_SHOVEL) {
            return updateDriftingParticle(world, tickDelta, -SNOW_SHOVEL_GRAVITY_PER_TICK, AURA_DRAG, false);
        } else if (type == Type.FLAME) {
            return updateDriftingParticle(world, tickDelta, 0.0f, FLAME_DRAG, false);
        } else if (type == Type.RAIN) {
            return updateRainParticle(world, tickDelta);
        }
        ageTicks += tickDelta;
        if (usesTargetInterpolation()) {
            return ageTicks >= lifetimeTicks;
        } else if (isDrip()) {
            return updateDrip(world, deltaTime, tickDelta);
        } else if (isCrit()) {
            return updateCrit(world, deltaTime, tickDelta);
        } else if (type == Type.BUBBLE) {
            motionY += BUBBLE_ACCELERATION_PER_TICK * tickDelta;
        }
        moveWithCollision(world, motionX * deltaTime, motionY * deltaTime, motionZ * deltaTime);
        float drag = switch (type) {
            case ITEM_PICKUP, ENCHANTMENT_TABLE -> 1.0f;
            case BUBBLE -> BUBBLE_DRAG;
            case SNOW_SHOVEL, MOB_SPELL, SUSPENDED, DEPTH_SUSPEND, TOWN_AURA -> AURA_DRAG;
            case SPLASH -> 0.98f;
            case FLAME -> 0.96f;
            case SMOKE, LARGE_SMOKE, SPELL, INSTANT_SPELL, RED_DUST -> 0.96f;
            case EXPLODE -> 0.90f;
            default -> 0.92f;
        };
        motionX *= drag;
        motionY *= drag;
        motionZ *= drag;
        if (type == Type.BUBBLE && world != null && !isInsideWater(world)) {
            return true;
        }
        if (type == Type.SUSPENDED && world != null && !isInsideWater(world)) {
            return true;
        }
        return ageTicks >= lifetimeTicks;
    }

    private boolean updatePortal(float tickDelta) {
        float nextAge = Math.min(lifetimeTicks, ageTicks + tickDelta);
        float motionAge = Math.max(0.0f, nextAge - 1.0f);
        float life = motionAge / lifetimeTicks;
        float curve = 1.0f - (-life + life * life * 2.0f);
        x = spawnX + motionX * curve;
        y = spawnY + motionY * curve + (1.0f - life);
        z = spawnZ + motionZ * curve;
        ageTicks += tickDelta;
        return ageTicks >= lifetimeTicks;
    }

    private boolean updateRisingSourceParticle(World world, float tickDelta, float drag) {
        ageTicks += tickDelta;
        moveWithCollision(world, motionX * tickDelta, motionY * tickDelta, motionZ * tickDelta);
        if (Math.abs(y - prevY) < COLLISION_EPSILON) {
            motionX *= 1.10f;
            motionZ *= 1.10f;
        }
        motionX *= drag;
        motionY *= drag;
        motionZ *= drag;
        if (onGround) {
            motionX *= GROUND_HORIZONTAL_DAMPING;
            motionZ *= GROUND_HORIZONTAL_DAMPING;
        }
        return ageTicks >= lifetimeTicks;
    }

    private boolean updateAuraParticle(World world, float tickDelta) {
        ageTicks += tickDelta;
        moveWithCollision(world, motionX * tickDelta, motionY * tickDelta, motionZ * tickDelta);
        motionX *= AURA_DRAG;
        motionY *= AURA_DRAG;
        motionZ *= AURA_DRAG;
        return ageTicks >= lifetimeTicks;
    }

    private boolean updateFragmentParticle(World world, float tickDelta) {
        ageTicks += tickDelta;
        motionY -= FRAGMENT_GRAVITY_PER_TICK * tickDelta;
        moveWithCollision(world, motionX * tickDelta, motionY * tickDelta, motionZ * tickDelta);
        motionX *= FRAGMENT_DRAG;
        motionY *= FRAGMENT_DRAG;
        motionZ *= FRAGMENT_DRAG;
        if (onGround) {
            motionX *= GROUND_HORIZONTAL_DAMPING;
            motionZ *= GROUND_HORIZONTAL_DAMPING;
        }
        return ageTicks >= lifetimeTicks;
    }

    private boolean updateDriftingParticle(World world, float tickDelta, float accelerationY, float drag,
            boolean boostHorizontalWhenYBlocked) {
        ageTicks += tickDelta;
        motionY += accelerationY * tickDelta;
        moveWithCollision(world, motionX * tickDelta, motionY * tickDelta, motionZ * tickDelta);
        if (boostHorizontalWhenYBlocked && Math.abs(y - prevY) < COLLISION_EPSILON) {
            motionX *= 1.10f;
            motionZ *= 1.10f;
        }
        motionX *= drag;
        motionY *= drag;
        motionZ *= drag;
        if (onGround) {
            motionX *= GROUND_HORIZONTAL_DAMPING;
            motionZ *= GROUND_HORIZONTAL_DAMPING;
        }
        return ageTicks >= lifetimeTicks;
    }

    private boolean updateRainParticle(World world, float tickDelta) {
        ageTicks += tickDelta;
        motionY -= RAIN_GRAVITY_PER_TICK * tickDelta;
        moveWithCollision(world, motionX * tickDelta, motionY * tickDelta, motionZ * tickDelta);
        motionX *= RAIN_DRAG;
        motionY *= RAIN_DRAG;
        motionZ *= RAIN_DRAG;
        if (onGround) {
            motionX *= GROUND_HORIZONTAL_DAMPING;
            motionZ *= GROUND_HORIZONTAL_DAMPING;
            return true;
        }
        return isInsideLiquid(world) || ageTicks >= lifetimeTicks;
    }

    private boolean updateDrip(World world, float deltaTime, float tickDelta) {
        motionY -= DRIP_GRAVITY_PER_TICK * tickDelta;
        if (isDripBobPhase(0.0f)) {
            motionX *= DRIP_BOB_DAMPING;
            motionY *= DRIP_BOB_DAMPING;
            motionZ *= DRIP_BOB_DAMPING;
        }
        moveWithCollision(world, motionX * deltaTime, motionY * deltaTime, motionZ * deltaTime);
        motionX *= DRIP_DRAG;
        motionY *= DRIP_DRAG;
        motionZ *= DRIP_DRAG;

        if (onGround) {
            if (type == Type.DRIP_WATER) {
                waterDripSplashPending = true;
                return true;
            }
            motionX *= GROUND_HORIZONTAL_DAMPING;
            motionZ *= GROUND_HORIZONTAL_DAMPING;
        }
        return isInsideLiquid(world) || ageTicks >= lifetimeTicks;
    }

    private boolean updateCrit(World world, float deltaTime, float tickDelta) {
        moveWithCollision(world, motionX * deltaTime, motionY * deltaTime, motionZ * deltaTime);
        motionX *= CRIT_DRAG;
        motionY *= CRIT_DRAG;
        motionZ *= CRIT_DRAG;
        motionY -= CRIT_GRAVITY_PER_TICK * tickDelta;
        if (onGround) {
            motionX *= CRIT_GROUND_HORIZONTAL_DRAG;
            motionZ *= CRIT_GROUND_HORIZONTAL_DRAG;
        }
        return ageTicks >= lifetimeTicks;
    }

    private void moveWithCollision(World world, float dx, float dy, float dz) {
        if (!allFinite(dx, dy, dz) || !isFinitePosition()) {
            return;
        }
        if (world == null || !collidesWithBlocks()) {
            x += dx;
            y += dy;
            z += dz;
            return;
        }
        float largestMove = Math.max(Math.abs(dx), Math.max(Math.abs(dy), Math.abs(dz)));
        int steps = Math.max(1, (int) Math.ceil(largestMove / MAX_COLLISION_STEP));
        float stepX = dx / steps;
        float stepY = dy / steps;
        float stepZ = dz / steps;
        for (int i = 0; i < steps; i++) {
            moveWithCollisionStep(world, stepX, stepY, stepZ);
        }
    }

    private void moveWithCollisionStep(World world, float dx, float dy, float dz) {
        if (!allFinite(dx, dy, dz)) {
            return;
        }
        if (dx != 0.0f) {
            float nextX = x + dx;
            AABB blocker = blockingBoxAt(world, nextX, y, z);
            if (blocker == null) {
                x = nextX;
            } else {
                x = dx > 0.0f ? blocker.getMin().x - COLLISION_EPSILON
                        : blocker.getMax().x + COLLISION_EPSILON;
                motionX = 0.0f;
            }
        }
        if (dy != 0.0f) {
            float nextY = y + dy;
            AABB blocker = blockingBoxAt(world, x, nextY, z);
            if (blocker == null) {
                y = nextY;
            } else {
                y = dy > 0.0f ? blocker.getMin().y - COLLISION_EPSILON
                        : blocker.getMax().y + COLLISION_EPSILON;
                motionY = 0.0f;
                if (dy < 0.0f) {
                    onGround = true;
                }
            }
        }
        if (dz != 0.0f) {
            float nextZ = z + dz;
            AABB blocker = blockingBoxAt(world, x, y, nextZ);
            if (blocker == null) {
                z = nextZ;
            } else {
                z = dz > 0.0f ? blocker.getMin().z - COLLISION_EPSILON
                        : blocker.getMax().z + COLLISION_EPSILON;
                motionZ = 0.0f;
            }
        }
    }

    private boolean collidesWithBlocks() {
        return switch (type) {
            case HEART, NOTE, SMOKE, LARGE_SMOKE, SNOWBALL_POOF, FLAME, SPLASH, CRIT, MAGIC_CRIT, RAIN, SNOW,
                    SNOW_SHOVEL, SLIME, BLOCK_CRACK, BLOCK_DUST, DRIP_WATER, DRIP_LAVA, LAVA, RED_DUST,
                    ITEM_CRACK -> true;
            default -> false;
        };
    }

    private static AABB blockingBoxAt(World world, float px, float py, float pz) {
        if (world == null || !allFinite(px, py, pz) || py < 0.0f || py >= Chunk.HEIGHT) {
            return null;
        }
        int blockX = (int) Math.floor(px);
        int blockY = (int) Math.floor(py);
        int blockZ = (int) Math.floor(pz);
        for (AABB box : world.getCollisionBoxesIfLoaded(blockX, blockY, blockZ)) {
            if (containsParticlePoint(box, px, py, pz)) {
                return box;
            }
        }
        return null;
    }

    private boolean isDrip() {
        return type == Type.DRIP_WATER || type == Type.DRIP_LAVA;
    }

    private boolean isCrit() {
        return isCritType(type);
    }

    private static boolean isFragmentType(Type type) {
        return type == Type.BLOCK_CRACK || type == Type.BLOCK_DUST || type == Type.SLIME
                || type == Type.ITEM_CRACK || type == Type.SNOWBALL_POOF;
    }

    private static boolean isSmokeLiftType(Type type) {
        return type == Type.SMOKE || type == Type.LARGE_SMOKE || type == Type.EXPLODE;
    }

    private static boolean isAuraType(Type type) {
        return type == Type.MOB_SPELL || type == Type.DEPTH_SUSPEND || type == Type.TOWN_AURA;
    }

    private static boolean isCritType(Type type) {
        return type == Type.CRIT || type == Type.MAGIC_CRIT;
    }

    private boolean isInsideLiquid(World world) {
        if (world == null || !isFinitePosition() || y < 0.0f || y >= Chunk.HEIGHT) {
            return false;
        }
        int blockX = (int) Math.floor(x);
        int blockY = (int) Math.floor(y);
        int blockZ = (int) Math.floor(z);
        return world.getBlockIfLoaded(blockX, blockY, blockZ, BlockType.AIR).isFluid();
    }

    private boolean isInsideWater(World world) {
        if (world == null || !isFinitePosition() || y < 0.0f || y >= Chunk.HEIGHT) {
            return false;
        }
        int blockX = (int) Math.floor(x);
        int blockY = (int) Math.floor(y);
        int blockZ = (int) Math.floor(z);
        return world.getBlockIfLoaded(blockX, blockY, blockZ, BlockType.AIR).isWater();
    }

    private static boolean containsParticlePoint(AABB box, float px, float py, float pz) {
        if (box == null || !box.isFinite() || !allFinite(px, py, pz)) {
            return false;
        }
        return px > box.getMin().x + COLLISION_EPSILON && px < box.getMax().x - COLLISION_EPSILON
                && py > box.getMin().y + COLLISION_EPSILON && py < box.getMax().y - COLLISION_EPSILON
                && pz > box.getMin().z + COLLISION_EPSILON && pz < box.getMax().z - COLLISION_EPSILON;
    }

    public Type getType() {
        return type;
    }

    public float getX() {
        return x;
    }

    public float getY() {
        return y;
    }

    public float getZ() {
        return z;
    }

    public float getRenderX(float partialTick) {
        float framePartialTick = sanitizePartialTick(partialTick);
        if (type == Type.ITEM_PICKUP && hasTarget) {
            float progress = pickupProgress(framePartialTick);
            return x + (targetX - x) * progress;
        }
        if (type == Type.ENCHANTMENT_TABLE && hasTarget) {
            float remaining = 1.0f - particleLife(framePartialTick);
            return x + (targetX - x) * remaining;
        }
        return prevX + (x - prevX) * framePartialTick;
    }

    public float getRenderY(float partialTick) {
        float framePartialTick = sanitizePartialTick(partialTick);
        if (type == Type.ITEM_PICKUP && hasTarget) {
            float progress = pickupProgress(framePartialTick);
            return y + (targetY - y) * progress;
        }
        if (type == Type.ENCHANTMENT_TABLE && hasTarget) {
            float life = particleLife(framePartialTick);
            float remaining = 1.0f - life;
            float sag = life * life;
            sag *= sag;
            return y + (targetY - y) * remaining - sag * 1.2f;
        }
        return prevY + (y - prevY) * framePartialTick;
    }

    public float getRenderZ(float partialTick) {
        float framePartialTick = sanitizePartialTick(partialTick);
        if (type == Type.ITEM_PICKUP && hasTarget) {
            float progress = pickupProgress(framePartialTick);
            return z + (targetZ - z) * progress;
        }
        if (type == Type.ENCHANTMENT_TABLE && hasTarget) {
            float remaining = 1.0f - particleLife(framePartialTick);
            return z + (targetZ - z) * remaining;
        }
        return prevZ + (z - prevZ) * framePartialTick;
    }

    public float getScale(float partialTick) {
        float age = Math.min(lifetimeTicks, ageTicks + sanitizePartialTick(partialTick));
        float life = age / lifetimeTicks;
        if (type == Type.FOOTSTEP) {
            return baseScale * Math.max(0.05f, 1.0f - life);
        }
        if (type == Type.PORTAL) {
            float remaining = 1.0f - life;
            return baseScale * Math.max(0.0f, Math.min(1.0f, 1.0f - remaining * remaining));
        }
        if (type == Type.HEART || type == Type.NOTE) {
            return baseScale * Math.max(0.0f, Math.min(1.0f, life * SOURCE_SCALE_RAMP));
        }
        if (type == Type.SUSPENDED || isAuraType(type)) {
            return baseScale;
        }
        if (isFragmentType(type)) {
            return baseScale;
        }
        if (type == Type.RED_DUST || type == Type.SMOKE || type == Type.LARGE_SMOKE || type == Type.SNOW_SHOVEL
                || type == Type.CRIT || type == Type.MAGIC_CRIT) {
            return baseScale * Math.max(0.0f, Math.min(1.0f, life * SOURCE_SCALE_RAMP));
        }
        if (type == Type.FLAME) {
            return baseScale * Math.max(0.5f, 1.0f - life * life * 0.5f);
        }
        float fade = 1.0f - life * 0.35f;
        return baseScale * Math.max(0.35f, fade);
    }

    public float getAgeTicks() {
        return ageTicks;
    }

    public float getLifetimeTicks() {
        return lifetimeTicks;
    }

    public float getBaseScale() {
        return baseScale;
    }

    public float getData() {
        return data;
    }

    public float getMotionX() {
        return motionX;
    }

    public float getMotionY() {
        return motionY;
    }

    public float getMotionZ() {
        return motionZ;
    }

    public boolean hasTarget() {
        return hasTarget;
    }

    public float getTargetX() {
        return targetX;
    }

    public float getTargetY() {
        return targetY;
    }

    public float getTargetZ() {
        return targetZ;
    }

    private boolean usesTargetInterpolation() {
        return hasTarget && (type == Type.ITEM_PICKUP || type == Type.ENCHANTMENT_TABLE);
    }

    private float pickupProgress(float partialTick) {
        float life = particleLife(partialTick);
        return life * life;
    }

    private float particleLife(float partialTick) {
        float age = Math.min(lifetimeTicks, ageTicks + sanitizePartialTick(partialTick));
        return age / lifetimeTicks;
    }

    private static int sourceLifetimeTicks(Type type, int lifetimeTicks) {
        if (type == Type.HEART) {
            return HEART_SOURCE_LIFETIME_TICKS;
        }
        if (type == Type.NOTE) {
            return NOTE_SOURCE_LIFETIME_TICKS;
        }
        return Math.max(1, lifetimeTicks);
    }

    public boolean isOnGround() {
        return onGround;
    }

    public boolean isDripBobPhase(float partialTick) {
        return isDrip() && ageTicks + sanitizePartialTick(partialTick) <= DRIP_BOB_TICKS;
    }

    public boolean consumeWaterDripSplashPending() {
        boolean pending = waterDripSplashPending;
        waterDripSplashPending = false;
        return pending;
    }

    public static float blockParticleData(BlockType type, int metadata, int face) {
        int ordinal = type == null ? BlockType.AIR.ordinal() : type.ordinal();
        int clampedMetadata = Math.max(0, Math.min(BLOCK_PARTICLE_METADATA_VALUES - 1, metadata));
        int clampedFace = Math.max(0, Math.min(BLOCK_PARTICLE_FACES - 1, face));
        return ordinal * BLOCK_PARTICLE_STRIDE + clampedMetadata * BLOCK_PARTICLE_FACES + clampedFace;
    }

    public BlockType getBlockParticleType() {
        int encoded = encodedParticleData();
        int ordinal = encoded / BLOCK_PARTICLE_STRIDE;
        BlockType[] values = BlockType.values();
        return ordinal >= 0 && ordinal < values.length ? values[ordinal] : BlockType.AIR;
    }

    public int getBlockParticleMetadata() {
        int encoded = encodedParticleData();
        return (encoded % BLOCK_PARTICLE_STRIDE) / BLOCK_PARTICLE_FACES;
    }

    public int getBlockParticleFace() {
        int encoded = encodedParticleData();
        return encoded % BLOCK_PARTICLE_FACES;
    }

    public static float itemParticleData(ItemType type) {
        return type == null ? -1.0f : type.ordinal();
    }

    public ItemType getItemParticleType() {
        int ordinal = Float.isFinite(data) ? Math.round(data) : -1;
        ItemType[] values = ItemType.values();
        return ordinal >= 0 && ordinal < values.length ? values[ordinal] : null;
    }

    public boolean isValid() {
        return isFinitePosition()
                && allFinite(prevX, prevY, prevZ, motionX, motionY, motionZ, spawnX, spawnY, spawnZ,
                        baseScale, lifetimeTicks, data)
                && lifetimeTicks > 0.0f
                && (!hasTarget || allFinite(targetX, targetY, targetZ));
    }

    private int encodedParticleData() {
        return Math.max(0, Math.round(Float.isFinite(data) ? data : 0.0f));
    }

    private boolean isFinitePosition() {
        return allFinite(x, y, z);
    }

    private static float sanitizePartialTick(float partialTick) {
        if (!Float.isFinite(partialTick)) {
            return 0.0f;
        }
        return Math.max(0.0f, Math.min(1.0f, partialTick));
    }

    private static boolean allFinite(float... values) {
        for (float value : values) {
            if (!Float.isFinite(value)) {
                return false;
            }
        }
        return true;
    }

    private static float finiteOrZero(float value) {
        return Float.isFinite(value) ? value : 0.0f;
    }

    private static float finiteOrDefault(float value, float fallback) {
        return Float.isFinite(value) ? value : fallback;
    }
}
