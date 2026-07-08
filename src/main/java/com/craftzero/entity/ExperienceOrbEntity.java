package com.craftzero.entity;

import com.craftzero.combat.DamageSource;
import com.craftzero.main.Player;
import com.craftzero.physics.AABB;
import com.craftzero.world.BlockType;
import com.craftzero.world.World;
import com.craftzero.world.WorldSoundEvent;

import java.util.Random;

/**
 * Release 1.0-style experience orb entity.
 */
public class ExperienceOrbEntity extends Entity {
    public static final int DESPAWN_TICKS = 6000;
    public static final int MAX_HEALTH = 5;
    private static final float ATTRACTION_RADIUS = 8.0f;
    private static final float PICKUP_RADIUS = 1.0f;
    private static final float GRAVITY_PER_TICK = 0.03f;
    private static final float DRAG = 0.98f;
    private static final float GROUND_BOUNCE = -0.9f;

    private int value;
    private int health = MAX_HEALTH;
    private int pickupDelayTicks;
    private final Random injectedRandom;
    private boolean launchInitialized;

    public ExperienceOrbEntity(float x, float y, float z, int value) {
        this(x, y, z, value, null);
    }

    public ExperienceOrbEntity(float x, float y, float z, int value, Random random) {
        super(0.5f, 0.5f);
        this.value = Math.max(1, value);
        this.injectedRandom = random;
        setPosition(x, y, z);
        if (random != null) {
            initializeLaunch(random);
        }
    }

    @Override
    public void setWorld(World world) {
        super.setWorld(world);
        if (!launchInitialized && world != null) {
            initializeLaunch(world.getRandom());
        }
    }

    @Override
    public void setMotion(float mx, float my, float mz) {
        super.setMotion(mx, my, mz);
        launchInitialized = true;
    }

    private void initializeLaunch(Random random) {
        yaw = random.nextFloat() * 360.0f;
        super.setMotion((random.nextFloat() * 0.2f - 0.1f) * 2.0f,
                random.nextFloat() * 0.2f * 2.0f,
                (random.nextFloat() * 0.2f - 0.1f) * 2.0f);
        launchInitialized = true;
    }

    private Random randomSource() {
        return injectedRandom != null ? injectedRandom : world.getRandom();
    }

    @Override
    public void tick() {
        super.tick();
        if (pickupDelayTicks > 0) {
            pickupDelayTicks--;
        }
        if (ticksExisted >= DESPAWN_TICKS) {
            remove();
            return;
        }
        attractAndCollect();
    }

    @Override
    public void updatePhysics(float deltaTime) {
        if (world == null || removed) {
            return;
        }
        motionY -= GRAVITY_PER_TICK;
        BlockType current = world.getBlockIfLoaded((int) Math.floor(x), (int) Math.floor(y),
                (int) Math.floor(z), BlockType.AIR);
        if (current == BlockType.LAVA || current == BlockType.FLOWING_LAVA) {
            Random random = randomSource();
            motionY = 0.2f;
            motionX = (random.nextFloat() - random.nextFloat()) * 0.2f;
            motionZ = (random.nextFloat() - random.nextFloat()) * 0.2f;
            world.playSound(WorldSoundEvent.FIZZ, x, y, z, 0.4f, 2.0f + random.nextFloat() * 0.4f);
        }

        float attemptedMotionY = motionY;
        moveWithCollision(motionX, motionY, motionZ);

        float horizontalDrag = onGround ? getGroundFriction() * DRAG : DRAG;
        motionX *= horizontalDrag;
        motionY *= DRAG;
        motionZ *= horizontalDrag;
        if (onGround) {
            motionY = attemptedMotionY * DRAG * GROUND_BOUNCE;
        }
    }

    private void attractAndCollect() {
        if (world == null) {
            return;
        }
        if (collectLocalPlayerExperience()) {
            return;
        }

        AttractionTarget target = nearestAttractionTarget();
        if (target == null || target.distanceSq() > ATTRACTION_RADIUS * ATTRACTION_RADIUS
                || target.distanceSq() <= 0.0001f) {
            return;
        }
        float dx = target.x() - x;
        float dy = target.y() - y;
        float dz = target.z() - z;
        float distance = (float) Math.sqrt(target.distanceSq());
        float strength = 1.0f - distance / ATTRACTION_RADIUS;
        strength *= strength;
        motionX += (dx / distance) * strength * 0.1f;
        motionY += (dy / distance) * strength * 0.1f;
        motionZ += (dz / distance) * strength * 0.1f;
    }

    private boolean collectLocalPlayerExperience() {
        Player player = world.getPlayer();
        if (player == null || player.isDead()) {
            return false;
        }
        if (pickupDelayTicks > 0 || !intersectsPickupBox(player) || !player.canPickupExperience()) {
            return false;
        }
        int previousLevel = player.getStats().getProgression().getLevel();
        player.getStats().getProgression().addExperience(value);
        boolean leveledUp = player.getStats().getProgression().getLevel() > previousLevel;
        player.onExperiencePickedUp();
        world.playExperiencePickupSound(x, y, z);
        if (leveledUp) {
            world.playExperienceLevelUpSound(player.getPosition().x, player.getEyeY(), player.getPosition().z);
        }
        remove();
        return true;
    }

    private AttractionTarget nearestAttractionTarget() {
        AttractionTarget closest = localAttractionTarget();
        for (World.RemotePlayerTarget target : world.remotePlayerViews(x, y, z, ATTRACTION_RADIUS, false)) {
            if (target == null || !target.valid()) {
                continue;
            }
            float distanceSq = distanceSq(target.x(), target.eyeY(), target.z());
            if (closest == null || distanceSq < closest.distanceSq()) {
                closest = new AttractionTarget(target.x(), target.eyeY(), target.z(), distanceSq);
            }
        }
        return closest;
    }

    private AttractionTarget localAttractionTarget() {
        Player player = world.getPlayer();
        if (player == null || player.isDead()) {
            return null;
        }
        float targetX = player.getPosition().x;
        float targetY = player.getEyeY();
        float targetZ = player.getPosition().z;
        return new AttractionTarget(targetX, targetY, targetZ, distanceSq(targetX, targetY, targetZ));
    }

    private boolean intersectsPickupBox(Player player) {
        AABB playerBox = player == null ? null : player.getBoundingBox();
        if (playerBox == null || getBoundingBox() == null) {
            return false;
        }
        return playerBox.expand(PICKUP_RADIUS).intersects(getBoundingBox());
    }

    private float distanceSq(float targetX, float targetY, float targetZ) {
        float dx = targetX - x;
        float dy = targetY - y;
        float dz = targetZ - z;
        return dx * dx + dy * dy + dz * dz;
    }

    public int getValue() {
        return value;
    }

    public int getHealth() {
        return health;
    }

    public boolean damage(float amount, DamageSource source) {
        if (removed || !Float.isFinite(amount) || amount <= 0.0f) {
            return false;
        }
        health = (int) (health - amount);
        if (health <= 0) {
            remove();
        }
        return true;
    }

    public void setHealth(int health) {
        this.health = Math.max(1, health);
    }

    public int getPickupDelayTicks() {
        return pickupDelayTicks;
    }

    public void setPickupDelayTicks(int pickupDelayTicks) {
        this.pickupDelayTicks = Math.max(0, pickupDelayTicks);
    }

    public static int getOrbValue(int amount) {
        if (amount >= 2477) {
            return 2477;
        }
        if (amount >= 1237) {
            return 1237;
        }
        if (amount >= 617) {
            return 617;
        }
        if (amount >= 307) {
            return 307;
        }
        if (amount >= 149) {
            return 149;
        }
        if (amount >= 73) {
            return 73;
        }
        if (amount >= 37) {
            return 37;
        }
        if (amount >= 17) {
            return 17;
        }
        if (amount >= 7) {
            return 7;
        }
        if (amount >= 3) {
            return 3;
        }
        return 1;
    }

    private record AttractionTarget(float x, float y, float z, float distanceSq) {
    }
}
