package com.craftzero.entity;

import com.craftzero.combat.DamageSource;
import com.craftzero.entity.mob.Enderman;
import com.craftzero.main.Player;
import com.craftzero.physics.Raycast;
import com.craftzero.world.World;
import com.craftzero.world.WorldParticle;
import org.joml.Vector3f;

import java.util.Random;
import java.util.function.Consumer;

public class EnderPearlEntity extends Entity {
    private static final float SIZE = 0.25f;
    public static final int DESPAWN_TICKS = 1200;
    private static final int OWNER_COLLISION_GRACE_TICKS = 2;
    private static final float GRAVITY_PER_TICK = 0.03f;
    private static final float DRAG = 0.99f;
    private static final int IMPACT_PORTAL_PARTICLES = 32;
    private static final float IMPACT_PARTICLE_SCALE = 0.25f;
    private static final int IMPACT_PARTICLE_LIFETIME_TICKS = 20;

    private final Player owner;
    private String remoteOwnerPlayerId = "";
    private Consumer<EnderPearlEntity> impactCallback;

    public EnderPearlEntity(float x, float y, float z, float motionX, float motionY, float motionZ, Player owner) {
        super(SIZE, SIZE);
        this.owner = owner;
        setPosition(x, y, z);
        setMotion(motionX, motionY, motionZ);
        updateRotationFromMotion();
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
            impact(null, origin);
            return;
        }
        Vector3f direction = motion.normalize(new Vector3f());

        Raycast.RaycastResult blockHit = Raycast.cast(world, origin, direction, distance);
        Raycast.EntityRaycastResult entityHit = Raycast.castEntitiesMatching(
                world.getEntitiesIncludingPending(), origin, direction, distance + 0.25f, null,
                EnderPearlEntity::isProjectileCollisionTarget);
        PlayerHit playerHit = findPlayerHit(origin, direction, distance + 0.25f);
        World.ProjectilePlayerHit remotePlayerHit = world.findRemoteProjectilePlayerHit(origin, direction,
                distance + 0.25f, remoteOwnerCollisionExclusion());

        float blockDistance = blockHit.hit ? blockHit.distance : Float.MAX_VALUE;
        float entityDistance = entityHit.hit ? entityHit.distance : Float.MAX_VALUE;
        float playerDistance = playerHit.hit ? playerHit.distance : Float.MAX_VALUE;
        float remotePlayerDistance = remotePlayerHit.hit() ? remotePlayerHit.distance() : Float.MAX_VALUE;
        if (entityHit.hit && entityDistance <= blockDistance && entityDistance <= playerDistance
                && entityDistance <= remotePlayerDistance) {
            impactEntity(entityHit.entity,
                    entityHit.hitPoint != null ? entityHit.hitPoint : pointAt(origin, direction, entityDistance));
            return;
        }
        if (playerHit.hit && playerDistance <= blockDistance && playerDistance <= entityDistance
                && playerDistance <= remotePlayerDistance) {
            impact(null, playerHit.hitPoint);
            return;
        }
        if (remotePlayerHit.hit() && remotePlayerDistance <= blockDistance && remotePlayerDistance <= entityDistance
                && remotePlayerDistance <= playerDistance) {
            impact(null, remotePlayerHit.hitPoint());
            return;
        }
        if (blockHit.hit) {
            impact(null, blockHit.hitPoint != null ? blockHit.hitPoint : pointAt(origin, direction, blockDistance));
            return;
        }

        x += motionX;
        y += motionY;
        z += motionZ;
        motionX *= DRAG;
        motionY = motionY * DRAG - GRAVITY_PER_TICK;
        motionZ *= DRAG;
        updateRotationFromMotion();
    }

    private void impactEntity(Entity entity, Vector3f hitPoint) {
        if (entity instanceof PaintingEntity painting) {
            painting.breakAsItem(false);
            impact(null, hitPoint);
            return;
        }
        if (entity instanceof LivingEntity living) {
            impact(living, hitPoint);
            return;
        }
        impact(null, hitPoint);
    }

    private void impact(LivingEntity target, Vector3f hitPoint) {
        if (hitPoint != null) {
            setPosition(hitPoint.x, hitPoint.y, hitPoint.z);
        }
        spawnImpactParticles();
        if (target != null) {
            DamageSource source = pearlContactDamageSource();
            boolean handled = target.damage(0.0f, source);
            if (!handled && !(target instanceof Enderman)) {
                recordZeroDamageContact(target, source);
            }
        }
        if (owner != null && world != null && world.getPlayer() == owner) {
            owner.teleportFromEnderPearl(x, y, z);
        }
        Consumer<EnderPearlEntity> callback = impactCallback;
        impactCallback = null;
        if (callback != null) {
            callback.accept(this);
        }
        remove();
    }

    private void spawnImpactParticles() {
        if (world == null) {
            return;
        }
        Random random = world.getRandom();
        for (int i = 0; i < IMPACT_PORTAL_PARTICLES; i++) {
            world.spawnParticle(WorldParticle.Type.PORTAL,
                    x,
                    y + (float) (random.nextDouble() * 2.0d),
                    z,
                    (float) random.nextGaussian(),
                    0.0f,
                    (float) random.nextGaussian(),
                    IMPACT_PARTICLE_SCALE, IMPACT_PARTICLE_LIFETIME_TICKS);
        }
    }

    private PlayerHit findPlayerHit(Vector3f origin, Vector3f direction, float maxDistance) {
        Player player = world.getPlayer();
        if (player == null || (player == owner && ticksExisted < OWNER_COLLISION_GRACE_TICKS)) {
            return PlayerHit.miss();
        }
        float distance = Raycast.intersectsAabb(origin, direction, Raycast.playerPickBox(player.getBoundingBox()));
        if (distance < 0 || distance > maxDistance) {
            return PlayerHit.miss();
        }
        return new PlayerHit(true, pointAt(origin, direction, distance), distance);
    }

    private static Vector3f pointAt(Vector3f origin, Vector3f direction, float distance) {
        return new Vector3f(
                origin.x + direction.x * distance,
                origin.y + direction.y * distance,
                origin.z + direction.z * distance);
    }

    private static boolean isProjectileCollisionTarget(Entity entity) {
        return entity instanceof LivingEntity
                || entity instanceof PaintingEntity
                || entity instanceof BoatEntity
                || entity instanceof MinecartEntity;
    }

    private void updateRotationFromMotion() {
        float horizontal = (float) Math.sqrt(motionX * motionX + motionZ * motionZ);
        if (horizontal > 0.0001f || Math.abs(motionY) > 0.0001f) {
            yaw = (float) Math.toDegrees(Math.atan2(motionX, -motionZ));
            pitch = (float) Math.toDegrees(Math.atan2(motionY, horizontal));
        }
    }

    public Player getOwner() {
        return owner;
    }

    public String getRemoteOwnerPlayerId() {
        return remoteOwnerPlayerId;
    }

    public void setRemoteOwnerPlayerId(String remoteOwnerPlayerId) {
        this.remoteOwnerPlayerId = remoteOwnerPlayerId == null ? "" : remoteOwnerPlayerId.trim();
    }

    public void setImpactCallback(Consumer<EnderPearlEntity> impactCallback) {
        this.impactCallback = impactCallback;
    }

    private String remoteOwnerCollisionExclusion() {
        return ticksExisted < OWNER_COLLISION_GRACE_TICKS ? remoteOwnerPlayerId : "";
    }

    private DamageSource pearlContactDamageSource() {
        DamageSource source = DamageSource.entity(DamageSource.Type.GENERIC, this);
        if (owner != null || !remoteOwnerPlayerId.isBlank()) {
            source = source.withPlayerCredit(true);
        }
        if (!remoteOwnerPlayerId.isBlank()) {
            source = source.withPlayerId(remoteOwnerPlayerId);
        }
        return source;
    }

    private void recordZeroDamageContact(LivingEntity target, DamageSource source) {
        if (target == null || source == null || target.isDead() || target.isRemoved()) {
            return;
        }
        target.hurtTime = target.hurtDuration;
        target.rememberDamageSource(source);
        if (source.playerCredit()) {
            target.recentPlayerHitTicks = LivingEntity.RECENT_PLAYER_HIT_TICKS;
            target.recentPlayerLootingLevel = 0;
        } else {
            target.recentPlayerLootingLevel = 0;
        }
        target.onHurt(0.0f, source.entity());
    }

    private record PlayerHit(boolean hit, Vector3f hitPoint, float distance) {
        static PlayerHit miss() {
            return new PlayerHit(false, null, Float.MAX_VALUE);
        }
    }
}
