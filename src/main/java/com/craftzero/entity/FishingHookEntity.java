package com.craftzero.entity;

import com.craftzero.inventory.ItemStack;
import com.craftzero.inventory.ItemType;
import com.craftzero.main.Player;
import com.craftzero.physics.AABB;
import com.craftzero.world.BlockType;
import com.craftzero.world.World;
import com.craftzero.world.WorldParticle;
import org.joml.Vector3f;

import java.util.Random;
import java.util.function.Supplier;

/**
 * Release-era fishing bobber. It keeps the old cast/reel loop compact:
 * roll for bites in water, become catchable briefly, then return one raw fish when reeled.
 */
public class FishingHookEntity extends Entity {
    private static final float SIZE = 0.25f;
    public static final int DESPAWN_TICKS = 1200;
    private static final int CLEAR_BITE_CHANCE = 500;
    private static final int RAIN_BITE_CHANCE = 300;
    private static final int MIN_CATCHABLE_TICKS = 10;
    private static final int CATCHABLE_SPREAD_TICKS = 30;
    private static final float BITE_PARTICLE_WIDTH_MULTIPLIER = 20.0f;
    private static final float BITE_BUBBLE_SCALE = 0.055f;
    private static final int BITE_BUBBLE_LIFETIME_TICKS = 8;
    private static final float BITE_SPLASH_SCALE = 0.12f;
    private static final int BITE_SPLASH_LIFETIME_TICKS = 8;
    private static final float CATCH_SPLASH_VOLUME = 0.25f;
    private static final float CATCH_SPLASH_PITCH_SPREAD = 0.4f;
    private static final float MAX_OWNER_DISTANCE_SQ = 32.0f * 32.0f;
    private static final float GRAVITY_PER_TICK = 0.03f;
    private static final float REMOTE_PLAYER_HOOK_WIDTH = 0.6f;
    private static final float REMOTE_PLAYER_HOOK_SEARCH_PADDING = 2.0f;

    private final Player owner;
    private final Random injectedRandom;
    private Supplier<OwnerSnapshot> remoteOwnerSupplier;
    private OwnerSnapshot restoredOwnerSnapshot;
    private String remoteOwnerPlayerId = "";
    private String hookedRemotePlayerId = "";
    private int waitTicks;
    private int catchableTicks;
    private boolean stuckInGround;
    private Entity hookedEntity;

    public FishingHookEntity(float x, float y, float z, float motionX, float motionY, float motionZ,
            Player owner) {
        this(x, y, z, motionX, motionY, motionZ, owner, null);
    }

    FishingHookEntity(float x, float y, float z, float motionX, float motionY, float motionZ,
            Player owner, Random random) {
        super(SIZE, SIZE);
        this.owner = owner;
        this.injectedRandom = random;
        waitTicks = 0;
        setPosition(x, y, z);
        setMotion(motionX, motionY, motionZ);
        updateRotationFromMotion();
    }

    @Override
    public void setWorld(World world) {
        super.setWorld(world);
    }

    @Override
    public void tick() {
        super.tick();
        if (ticksExisted >= DESPAWN_TICKS || !hasLiveOwner() || !ownerHasFishingRod()
                || ownerDistanceSq() > MAX_OWNER_DISTANCE_SQ) {
            remove();
            clearOwnerHook();
            return;
        }
        if (hookedEntity != null) {
            if (hookedEntity.isRemoved()) {
                hookedEntity = null;
            } else {
                followHookedEntity();
                return;
            }
        }
        if (hasHookedRemotePlayer()) {
            if (followHookedRemotePlayer()) {
                return;
            }
            hookedRemotePlayerId = "";
        }
        if (isInWaterBlock()) {
            tickFishingWait();
        } else if (catchableTicks > 0) {
            catchableTicks = 0;
        }
    }

    @Override
    public void updatePhysics(float deltaTime) {
        if (world == null || removed) {
            return;
        }
        if (hookedEntity != null) {
            followHookedEntity();
            return;
        }
        if (hasHookedRemotePlayer()) {
            if (followHookedRemotePlayer()) {
                return;
            }
            hookedRemotePlayerId = "";
        }
        if (stuckInGround) {
            return;
        }
        super.updatePhysics(deltaTime);
        tryHookEntity();
        if (hookedEntity != null || hasHookedRemotePlayer()) {
            if (hookedEntity != null) {
                followHookedEntity();
            } else {
                followHookedRemotePlayer();
            }
            return;
        }
        if (onGround || collidedHorizontally) {
            stuckInGround = true;
            setMotion(0.0f, 0.0f, 0.0f);
        }
        updateRotationFromMotion();
    }

    public int reelIn() {
        return reelInWithResult().durabilityCost();
    }

    public ReelResult reelInWithResult() {
        if (removed) {
            return new ReelResult(0, false, false, false);
        }
        int durabilityCost = 0;
        boolean caughtFish = false;
        boolean pulledEntity = false;
        boolean pulledFromGround = false;
        boolean hasOwnerPosition = ownerPosition() != null;
        if (hookedEntity != null && !hookedEntity.isRemoved() && hasOwnerPosition) {
            pullHookedEntityToOwner();
            durabilityCost = 3;
            pulledEntity = true;
        } else if (hasHookedRemotePlayer() && hasOwnerPosition && pullHookedRemotePlayerToOwner()) {
            durabilityCost = 3;
            pulledEntity = true;
        } else if (catchableTicks > 0 && world != null && hasOwnerPosition) {
            spawnCaughtFishTowardOwner();
            durabilityCost = 1;
            caughtFish = true;
        } else if (stuckInGround) {
            durabilityCost = 2;
            pulledFromGround = true;
        }
        remove();
        clearOwnerHook();
        return new ReelResult(durabilityCost, caughtFish, pulledEntity, pulledFromGround);
    }

    public boolean isCatchable() {
        return catchableTicks > 0;
    }

    public Player getOwner() {
        return owner;
    }

    public void setRemoteOwnerSupplier(Supplier<OwnerSnapshot> remoteOwnerSupplier) {
        this.remoteOwnerSupplier = remoteOwnerSupplier;
    }

    public void setRemoteOwnerPlayerId(String remoteOwnerPlayerId) {
        this.remoteOwnerPlayerId = sanitizePlayerId(remoteOwnerPlayerId);
    }

    public String getRemoteOwnerPlayerId() {
        return remoteOwnerPlayerId;
    }

    public void restoreOwnerSnapshot(OwnerSnapshot ownerSnapshot) {
        restoredOwnerSnapshot = ownerSnapshot;
    }

    public OwnerSnapshot getOwnerSnapshot() {
        OwnerSnapshot snapshot = remoteOwnerSnapshot();
        if (snapshot != null) {
            return snapshot;
        }
        if (owner == null) {
            return null;
        }
        ItemStack held = owner.getInventory().getItemInHand();
        return new OwnerSnapshot(
                owner.getPosition().x,
                owner.getPosition().y,
                owner.getPosition().z,
                !owner.isDead(),
                held != null && !held.isEmpty() && held.getType() == ItemType.FISHING_ROD,
                owner.getCamera().getYaw(),
                owner.isSneaking());
    }

    public int getWaitTicks() {
        return waitTicks;
    }

    public int getCatchableTicks() {
        return catchableTicks;
    }

    public boolean isStuckInGround() {
        return stuckInGround;
    }

    public Entity getHookedEntity() {
        return hookedEntity != null && !hookedEntity.isRemoved() ? hookedEntity : null;
    }

    public String getHookedRemotePlayerId() {
        return hookedRemotePlayerId;
    }

    public void restoreHookedEntity(Entity entity) {
        if (entity == null) {
            hookedEntity = null;
            return;
        }
        if (isHookableEntity(entity)) {
            hookedEntity = entity;
            hookedRemotePlayerId = "";
            catchableTicks = 0;
            waitTicks = 0;
            stuckInGround = false;
            setMotion(0.0f, 0.0f, 0.0f);
            followHookedEntity();
        }
    }

    public void restoreHookedRemotePlayer(String playerId) {
        hookedRemotePlayerId = sanitizePlayerId(playerId);
        if (hookedRemotePlayerId.isBlank()) {
            return;
        }
        hookedEntity = null;
        catchableTicks = 0;
        waitTicks = 0;
        stuckInGround = false;
        setMotion(0.0f, 0.0f, 0.0f);
        followHookedRemotePlayer();
    }

    public void restoreFishingState(int waitTicks, int catchableTicks, boolean stuckInGround) {
        this.waitTicks = Math.max(0, waitTicks);
        this.catchableTicks = Math.max(0, catchableTicks);
        if (this.catchableTicks > 0) {
            this.waitTicks = 0;
        }
        this.stuckInGround = stuckInGround;
        if (stuckInGround) {
            setMotion(0.0f, 0.0f, 0.0f);
        }
    }

    public void setCatchableTicks(int catchableTicks) {
        this.catchableTicks = Math.max(0, catchableTicks);
        if (this.catchableTicks > 0) {
            this.waitTicks = 0;
        }
    }

    private void tryHookEntity() {
        if (world == null || hookedEntity != null || hasHookedRemotePlayer()) {
            return;
        }
        AABB path = sweptHookBox();
        for (Entity entity : world.getEntitiesIncludingPending()) {
            if (!isHookableEntity(entity)) {
                continue;
            }
            if (entity.getBoundingBox().expand(0.3f).intersects(path)) {
                hookedEntity = entity;
                hookedRemotePlayerId = "";
                catchableTicks = 0;
                waitTicks = 0;
                stuckInGround = false;
                setMotion(0.0f, 0.0f, 0.0f);
                return;
            }
        }
        tryHookRemotePlayer(path);
    }

    private AABB sweptHookBox() {
        AABB previous = getBoundingBoxAt(prevX, prevY, prevZ);
        AABB current = getBoundingBox();
        return new AABB(
                Math.min(previous.getMin().x, current.getMin().x),
                Math.min(previous.getMin().y, current.getMin().y),
                Math.min(previous.getMin().z, current.getMin().z),
                Math.max(previous.getMax().x, current.getMax().x),
                Math.max(previous.getMax().y, current.getMax().y),
                Math.max(previous.getMax().z, current.getMax().z))
                .expand(0.25f);
    }

    private boolean isHookableEntity(Entity entity) {
        return entity != null
                && entity != this
                && !entity.isRemoved()
                && (entity instanceof LivingEntity
                        || entity instanceof BoatEntity
                        || entity instanceof MinecartEntity);
    }

    private void tryHookRemotePlayer(AABB path) {
        for (World.RemotePlayerTarget target : world.remotePlayerViews(x, y, z, remoteHookSearchRange(path), false)) {
            if (!isHookableRemotePlayer(target)) {
                continue;
            }
            if (remotePlayerBox(target).expand(0.3f).intersects(path)) {
                hookedRemotePlayerId = target.playerId();
                hookedEntity = null;
                catchableTicks = 0;
                waitTicks = 0;
                stuckInGround = false;
                setMotion(0.0f, 0.0f, 0.0f);
                followHookedRemotePlayer(target);
                return;
            }
        }
    }

    private float remoteHookSearchRange(AABB path) {
        if (path == null) {
            return REMOTE_PLAYER_HOOK_SEARCH_PADDING;
        }
        float dx = path.getMax().x - path.getMin().x;
        float dy = path.getMax().y - path.getMin().y;
        float dz = path.getMax().z - path.getMin().z;
        return (float) Math.sqrt(dx * dx + dy * dy + dz * dz) + REMOTE_PLAYER_HOOK_SEARCH_PADDING;
    }

    private boolean isHookableRemotePlayer(World.RemotePlayerTarget target) {
        return target != null
                && target.valid()
                && !target.playerId().equals(remoteOwnerPlayerId);
    }

    private AABB remotePlayerBox(World.RemotePlayerTarget target) {
        return AABB.fromCenter(new Vector3f(target.x(), target.y(), target.z()),
                REMOTE_PLAYER_HOOK_WIDTH, target.height(), REMOTE_PLAYER_HOOK_WIDTH);
    }

    private void followHookedEntity() {
        if (hookedEntity == null || hookedEntity.isRemoved()) {
            hookedEntity = null;
            return;
        }
        stuckInGround = false;
        catchableTicks = 0;
        setMotion(0.0f, 0.0f, 0.0f);
        x = hookedEntity.getX();
        y = hookedEntity.getY() + hookedEntity.getHeight() * 0.8f;
        z = hookedEntity.getZ();
    }

    private boolean followHookedRemotePlayer() {
        if (world == null || !hasHookedRemotePlayer()) {
            return false;
        }
        return followHookedRemotePlayer(world.remotePlayerViewById(hookedRemotePlayerId));
    }

    private boolean followHookedRemotePlayer(World.RemotePlayerTarget target) {
        if (!isHookableRemotePlayer(target)) {
            return false;
        }
        stuckInGround = false;
        catchableTicks = 0;
        setMotion(0.0f, 0.0f, 0.0f);
        x = target.x();
        y = target.y() + target.height() * 0.8f;
        z = target.z();
        return true;
    }

    private void pullHookedEntityToOwner() {
        Vector3f ownerPosition = ownerPosition();
        if (ownerPosition == null) {
            return;
        }
        float dx = ownerPosition.x - hookedEntity.getX();
        float dy = ownerPosition.y - hookedEntity.getY();
        float dz = ownerPosition.z - hookedEntity.getZ();
        float distance = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        hookedEntity.addMotion(dx * 0.1f,
                dy * 0.1f + (float) Math.sqrt(distance) * 0.08f,
                dz * 0.1f);
    }

    private boolean pullHookedRemotePlayerToOwner() {
        if (world == null || !hasHookedRemotePlayer()) {
            return false;
        }
        Vector3f ownerPosition = ownerPosition();
        World.RemotePlayerTarget target = world.remotePlayerViewById(hookedRemotePlayerId);
        if (ownerPosition == null || !isHookableRemotePlayer(target)) {
            return false;
        }
        float dx = ownerPosition.x - target.x();
        float dy = ownerPosition.y - target.y();
        float dz = ownerPosition.z - target.z();
        float distance = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        return world.pullRemotePlayerTarget(hookedRemotePlayerId,
                dx * 0.1f,
                dy * 0.1f + (float) Math.sqrt(distance) * 0.08f,
                dz * 0.1f);
    }

    private void spawnCaughtFishTowardOwner() {
        Vector3f ownerPosition = ownerPosition();
        if (ownerPosition == null) {
            return;
        }
        float dx = ownerPosition.x - x;
        float dy = ownerPosition.y - y;
        float dz = ownerPosition.z - z;
        float distance = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        world.spawnThrownStack(x, y + 0.15f, z, new ItemStack(ItemType.RAW_FISH, 1),
                dx * 0.1f,
                dy * 0.1f + (float) Math.sqrt(distance) * 0.08f,
                dz * 0.1f);
    }

    private void tickFishingWait() {
        stuckInGround = false;
        Random random = randomSource();
        if (catchableTicks > 0) {
            catchableTicks--;
            return;
        }
        if (waitTicks > 0) {
            waitTicks--;
            return;
        }
        if (random.nextInt(biteChanceBound()) == 0) {
            catchableTicks = MIN_CATCHABLE_TICKS + random.nextInt(CATCHABLE_SPREAD_TICKS);
            motionY -= 0.2f;
            spawnCatchSplash();
        }
    }

    private int biteChanceBound() {
        return world != null && world.isRainingAt((int) Math.floor(x), (int) Math.floor(y) + 1,
                (int) Math.floor(z)) ? RAIN_BITE_CHANCE : CLEAR_BITE_CHANCE;
    }

    private Random randomSource() {
        return injectedRandom != null ? injectedRandom : world.getRandom();
    }

    private void spawnCatchSplash() {
        if (world != null) {
            world.playSound(com.craftzero.world.WorldSoundEvent.FISHING_SPLASH,
                    x, y + SIZE * 0.5f, z, CATCH_SPLASH_VOLUME, catchSplashPitch());
            spawnBiteParticles();
        }
    }

    private void spawnBiteParticles() {
        Random random = randomSource();
        int count = biteParticleCount();
        float waterlineY = (float) Math.floor(getBoundingBox().getMin().y) + 1.0f;
        for (int i = 0; i < count; i++) {
            float offsetX = (random.nextFloat() * 2.0f - 1.0f) * width;
            float offsetZ = (random.nextFloat() * 2.0f - 1.0f) * width;
            world.spawnParticle(WorldParticle.Type.BUBBLE,
                    x + offsetX,
                    waterlineY,
                    z + offsetZ,
                    motionX,
                    motionY - random.nextFloat() * 0.2f,
                    motionZ,
                    BITE_BUBBLE_SCALE,
                    BITE_BUBBLE_LIFETIME_TICKS);
        }
        for (int i = 0; i < count; i++) {
            float offsetX = (random.nextFloat() * 2.0f - 1.0f) * width;
            float offsetZ = (random.nextFloat() * 2.0f - 1.0f) * width;
            world.spawnParticle(WorldParticle.Type.SPLASH,
                    x + offsetX,
                    waterlineY,
                    z + offsetZ,
                    motionX,
                    motionY,
                    motionZ,
                    BITE_SPLASH_SCALE,
                    BITE_SPLASH_LIFETIME_TICKS);
        }
    }

    private int biteParticleCount() {
        return Math.max(1, (int) Math.ceil(1.0f + width * BITE_PARTICLE_WIDTH_MULTIPLIER));
    }

    private float catchSplashPitch() {
        Random random = randomSource();
        return 1.0f + (random.nextFloat() - random.nextFloat()) * CATCH_SPLASH_PITCH_SPREAD;
    }

    private boolean ownerHasFishingRod() {
        OwnerSnapshot snapshot = remoteOwnerSnapshot();
        if (snapshot != null) {
            return snapshot.alive() && snapshot.holdingFishingRod();
        }
        ItemStack held = owner.getInventory().getItemInHand();
        return held != null && !held.isEmpty() && held.getType() == ItemType.FISHING_ROD;
    }

    private float ownerDistanceSq() {
        Vector3f ownerPosition = ownerPosition();
        if (ownerPosition == null) {
            return Float.MAX_VALUE;
        }
        float dx = ownerPosition.x - x;
        float dy = ownerPosition.y - y;
        float dz = ownerPosition.z - z;
        return dx * dx + dy * dy + dz * dz;
    }

    private boolean hasLiveOwner() {
        OwnerSnapshot snapshot = remoteOwnerSnapshot();
        if (snapshot != null) {
            return snapshot.alive();
        }
        return owner != null && !owner.isDead();
    }

    private Vector3f ownerPosition() {
        OwnerSnapshot snapshot = remoteOwnerSnapshot();
        if (snapshot != null) {
            return new Vector3f(snapshot.x(), snapshot.y(), snapshot.z());
        }
        return owner == null ? null : owner.getPosition();
    }

    private OwnerSnapshot remoteOwnerSnapshot() {
        if (remoteOwnerSupplier != null) {
            return remoteOwnerSupplier.get();
        }
        return restoredOwnerSnapshot;
    }

    private boolean hasHookedRemotePlayer() {
        return !hookedRemotePlayerId.isBlank();
    }

    private String sanitizePlayerId(String playerId) {
        return playerId == null ? "" : playerId.trim();
    }

    private boolean isInWaterBlock() {
        if (world == null) {
            return false;
        }
        BlockType block = world.getBlockIfLoaded((int) Math.floor(x), (int) Math.floor(y),
                (int) Math.floor(z), BlockType.AIR);
        return block == BlockType.WATER || block == BlockType.FLOWING_WATER;
    }

    private void clearOwnerHook() {
        if (owner != null && owner.getFishingHook() == this) {
            owner.clearFishingHook(this);
        }
    }

    private void updateRotationFromMotion() {
        float horizontal = (float) Math.sqrt(motionX * motionX + motionZ * motionZ);
        if (horizontal > 0.0001f || Math.abs(motionY) > 0.0001f) {
            yaw = (float) Math.toDegrees(Math.atan2(motionX, -motionZ));
            pitch = (float) Math.toDegrees(Math.atan2(motionY, horizontal));
        }
    }

    @Override
    protected float getGravityPerTick() {
        return GRAVITY_PER_TICK;
    }

    @Override
    protected float getWaterGravityPerTick() {
        return 0.0f;
    }

    @Override
    protected float getWaterHorizontalDrag() {
        return 0.5f;
    }

    @Override
    protected float getWaterVerticalDrag() {
        return 0.5f;
    }

    public record ReelResult(int durabilityCost, boolean caughtFish, boolean pulledEntity, boolean pulledFromGround) {
    }

    public record OwnerSnapshot(float x, float y, float z, boolean alive, boolean holdingFishingRod, float yaw,
            boolean sneaking) {
    }
}
