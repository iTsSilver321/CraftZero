package com.craftzero.entity;

import com.craftzero.combat.DamageSource;
import com.craftzero.main.Player;
import com.craftzero.physics.Raycast;
import com.craftzero.progression.PotionData;
import com.craftzero.progression.PotionEffectResolver;
import com.craftzero.world.World;
import org.joml.Vector3f;

public class SplashPotionEntity extends Entity {
    private static final float SIZE = 0.25f;
    public static final int DESPAWN_TICKS = 1200;
    private static final int SHOOTER_COLLISION_GRACE_TICKS = 2;
    private static final float GRAVITY_PER_TICK = 0.03f;
    private static final float DRAG = 0.99f;
    private static final float SPLASH_RADIUS = 4.0f;

    private Entity shooter;
    private String remoteShooterPlayerId = "";
    private boolean playerOwned;
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
            splash(null);
            return;
        }
        Vector3f direction = motion.normalize(new Vector3f());

        Raycast.RaycastResult blockHit = Raycast.cast(world, origin, direction, distance);
        Raycast.EntityRaycastResult entityHit = Raycast.castEntitiesMatching(
                world.getEntitiesIncludingPending(), origin, direction, distance + 0.25f, shooterCollisionExclusion(),
                SplashPotionEntity::isProjectileCollisionTarget);
        PlayerHit playerHit = findPlayerHit(origin, direction, distance + 0.25f);
        World.ProjectilePlayerHit remotePlayerHit = world.findRemoteProjectilePlayerHit(origin, direction,
                distance + 0.25f, remoteShooterCollisionExclusion());

        float blockDistance = blockHit.hit ? blockHit.distance : Float.MAX_VALUE;
        float entityDistance = entityHit.hit ? entityHit.distance : Float.MAX_VALUE;
        float playerDistance = playerHit.hit ? playerHit.distance : Float.MAX_VALUE;
        float remotePlayerDistance = remotePlayerHit.hit() ? remotePlayerHit.distance() : Float.MAX_VALUE;
        if (entityHit.hit && entityDistance <= blockDistance && entityDistance <= playerDistance
                && entityDistance <= remotePlayerDistance) {
            if (entityHit.entity instanceof PaintingEntity painting) {
                splashPainting(painting, entityHit.hitPoint != null ? entityHit.hitPoint
                        : pointAt(origin, direction, entityDistance));
                return;
            }
            if (isVehicle(entityHit.entity)) {
                splash(null, entityHit.hitPoint != null ? entityHit.hitPoint
                        : pointAt(origin, direction, entityDistance));
                return;
            }
            splash(entityHit.entity, entityHit.hitPoint != null ? entityHit.hitPoint
                    : pointAt(origin, direction, entityDistance));
            return;
        }
        if (playerHit.hit && playerDistance <= blockDistance && playerDistance <= entityDistance
                && playerDistance <= remotePlayerDistance) {
            splash(playerHit.player, playerHit.hitPoint);
            return;
        }
        if (remotePlayerHit.hit() && remotePlayerDistance <= blockDistance && remotePlayerDistance <= entityDistance
                && remotePlayerDistance <= playerDistance) {
            splash(remotePlayerHit, remotePlayerHit.hitPoint());
            return;
        }
        if (blockHit.hit) {
            splash(null, blockHit.hitPoint != null ? blockHit.hitPoint : pointAt(origin, direction, blockDistance));
            return;
        }

        x += motionX;
        y += motionY;
        z += motionZ;
        motionX *= DRAG;
        motionY = motionY * DRAG - GRAVITY_PER_TICK;
        motionZ *= DRAG;
    }

    private void splash(Object directHit, Vector3f hitPoint) {
        if (world == null) {
            remove();
            return;
        }
        if (hitPoint != null) {
            setPosition(hitPoint.x, hitPoint.y, hitPoint.z);
        }
        world.spawnSplashPotionParticles(x, y, z, potionData);
        DamageSource instantDamageSource = splashDamageSource();
        Player player = world.getPlayer();
        if (player != null) {
            float strength = player == directHit
                    ? 1.0f
                    : splashStrength(player.getPosition().x, player.getPosition().y + 1.0f,
                            player.getPosition().z);
            if (strength > 0.0f) {
                PotionEffectResolver.applyToPlayer(player, potionData, strength, instantDamageSource);
            }
        }
        for (Entity entity : world.getEntitiesIncludingPending()) {
            if (entity instanceof LivingEntity living) {
                float strength = entity == directHit
                        ? 1.0f
                        : splashStrength(entity.getX(), entity.getY() + entity.getHeight() * 0.5f,
                                entity.getZ());
                if (strength > 0.0f) {
                    PotionEffectResolver.applyToLiving(living, potionData, strength, instantDamageSource);
                }
            }
        }
        String directRemotePlayerId = directHit instanceof World.ProjectilePlayerHit hit ? hit.playerId() : "";
        world.splashRemoteProjectilePlayers(x, y, z, potionData, directRemotePlayerId);
        remove();
    }

    private void splashPainting(PaintingEntity painting, Vector3f hitPoint) {
        if (painting != null && !painting.isRemoved()) {
            painting.breakAsItem(false);
        }
        splash(null, hitPoint);
    }

    private void splash(Object directHit) {
        splash(directHit, null);
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
        float distance = Raycast.intersectsAabb(origin, direction, Raycast.playerPickBox(player.getBoundingBox()));
        return distance >= 0 && distance <= maxDistance
                ? new PlayerHit(true, player, pointAt(origin, direction, distance), distance)
                : PlayerHit.miss();
    }

    private static Vector3f pointAt(Vector3f origin, Vector3f direction, float distance) {
        return new Vector3f(
                origin.x + direction.x * distance,
                origin.y + direction.y * distance,
                origin.z + direction.z * distance);
    }

    public PotionData getPotionData() {
        return potionData;
    }

    public Entity getShooter() {
        return shooter;
    }

    public void restoreShooter(Entity shooter) {
        this.shooter = shooter instanceof LivingEntity ? shooter : null;
    }

    public String getRemoteShooterPlayerId() {
        return remoteShooterPlayerId;
    }

    public void setRemoteShooterPlayerId(String remoteShooterPlayerId) {
        this.remoteShooterPlayerId = remoteShooterPlayerId == null ? "" : remoteShooterPlayerId;
        if (!this.remoteShooterPlayerId.isBlank()) {
            this.playerOwned = true;
        }
    }

    public boolean isPlayerOwned() {
        return playerOwned;
    }

    public void setPlayerOwned(boolean playerOwned) {
        this.playerOwned = playerOwned;
    }

    private DamageSource splashDamageSource() {
        DamageSource source = DamageSource.point(DamageSource.Type.MAGIC, x, y, z, 0.0f, 0.0f);
        if (playerOwned || !remoteShooterPlayerId.isBlank()) {
            source = source.withPlayerCredit(true);
        }
        if (!remoteShooterPlayerId.isBlank()) {
            source = source.withPlayerId(remoteShooterPlayerId);
        }
        return source;
    }

    private Entity shooterCollisionExclusion() {
        return ticksExisted < SHOOTER_COLLISION_GRACE_TICKS ? shooter : null;
    }

    private String remoteShooterCollisionExclusion() {
        return ticksExisted < SHOOTER_COLLISION_GRACE_TICKS ? remoteShooterPlayerId : "";
    }

    private static boolean isProjectileCollisionTarget(Entity entity) {
        return entity instanceof LivingEntity || entity instanceof PaintingEntity || isVehicle(entity);
    }

    private static boolean isVehicle(Entity entity) {
        return entity instanceof BoatEntity || entity instanceof MinecartEntity;
    }

    private record PlayerHit(boolean hit, Player player, Vector3f hitPoint, float distance) {
        static PlayerHit miss() {
            return new PlayerHit(false, null, null, Float.MAX_VALUE);
        }
    }
}
