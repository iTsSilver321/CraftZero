package com.craftzero.entity;

import com.craftzero.main.Player;
import com.craftzero.physics.AABB;
import com.craftzero.physics.Raycast;
import com.craftzero.progression.PotionData;
import com.craftzero.progression.PotionEffectResolver;
import org.joml.Vector3f;

public class SplashPotionEntity extends Entity {
    private static final float SIZE = 0.25f;
    private static final int DESPAWN_TICKS = 1200;
    private static final float GRAVITY_PER_TICK = 0.03f;
    private static final float DRAG = 0.99f;
    private static final float SPLASH_RADIUS = 4.0f;

    private final Entity shooter;
    private final PotionData potionData;

    public SplashPotionEntity(float x, float y, float z, float motionX, float motionY, float motionZ,
            Entity shooter, PotionData potionData) {
        super(SIZE, SIZE);
        this.shooter = shooter;
        this.potionData = potionData == null ? PotionData.water() : potionData;
        setPosition(x, y, z);
        setMotion(motionX, motionY, motionZ);
    }

    @Override
    public void tick() {
        super.tick();
        if (ticksExisted >= DESPAWN_TICKS) {
            remove();
        }
    }

    @Override
    public void updatePhysics(float deltaTime) {
        if (world == null || removed) {
            return;
        }
        Vector3f origin = new Vector3f(x, y, z);
        Vector3f motion = new Vector3f(motionX, motionY, motionZ);
        float distance = motion.length();
        if (distance < 0.0001f) {
            splash();
            return;
        }
        Vector3f direction = motion.normalize(new Vector3f());

        Raycast.RaycastResult blockHit = Raycast.cast(world, origin, direction, distance);
        Raycast.EntityRaycastResult entityHit = Raycast.castEntities(
                world.getEntities(), origin, direction, distance + 0.25f, shooter);
        PlayerHit playerHit = findPlayerHit(origin, direction, distance + 0.25f);

        float blockDistance = blockHit.hit ? blockHit.distance : Float.MAX_VALUE;
        float entityDistance = entityHit.hit ? entityHit.distance : Float.MAX_VALUE;
        float playerDistance = playerHit.hit ? playerHit.distance : Float.MAX_VALUE;
        if (entityDistance <= blockDistance && entityDistance <= playerDistance) {
            splash();
            return;
        }
        if (playerDistance <= blockDistance && playerDistance <= entityDistance) {
            splash();
            return;
        }
        if (blockHit.hit) {
            splash();
            return;
        }

        x += motionX;
        y += motionY;
        z += motionZ;
        motionX *= DRAG;
        motionY = motionY * DRAG - GRAVITY_PER_TICK;
        motionZ *= DRAG;
    }

    private void splash() {
        if (world == null) {
            remove();
            return;
        }
        Player player = world.getPlayer();
        if (player != null) {
            float strength = splashStrength(player.getPosition().x, player.getPosition().y + 1.0f,
                    player.getPosition().z);
            if (strength > 0.0f) {
                PotionEffectResolver.applyToPlayer(player, potionData, strength);
            }
        }
        for (Entity entity : world.getEntities()) {
            if (entity instanceof LivingEntity living) {
                float strength = splashStrength(entity.getX(), entity.getY() + entity.getHeight() * 0.5f,
                        entity.getZ());
                if (strength > 0.0f) {
                    PotionEffectResolver.applyToLiving(living, potionData, strength);
                }
            }
        }
        remove();
    }

    private float splashStrength(float targetX, float targetY, float targetZ) {
        float dx = targetX - x;
        float dy = targetY - y;
        float dz = targetZ - z;
        float distance = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (distance > SPLASH_RADIUS) {
            return 0.0f;
        }
        return Math.max(0.0f, 1.0f - distance / SPLASH_RADIUS);
    }

    private PlayerHit findPlayerHit(Vector3f origin, Vector3f direction, float maxDistance) {
        if (world.getPlayer() == null || ticksExisted < 2) {
            return PlayerHit.miss();
        }
        Player player = world.getPlayer();
        AABB box = player.getBoundingBox().expand(0.1f);
        float distance = ArrowEntityRay.intersects(origin, direction, box);
        return distance >= 0 && distance <= maxDistance ? new PlayerHit(true, player, distance) : PlayerHit.miss();
    }

    public PotionData getPotionData() {
        return potionData;
    }

    private record PlayerHit(boolean hit, Player player, float distance) {
        static PlayerHit miss() {
            return new PlayerHit(false, null, Float.MAX_VALUE);
        }
    }
}
