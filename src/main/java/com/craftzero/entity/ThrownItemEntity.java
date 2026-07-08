package com.craftzero.entity;

import com.craftzero.combat.DamageSource;
import com.craftzero.entity.mob.Blaze;
import com.craftzero.entity.mob.Chicken;
import com.craftzero.entity.mob.Enderman;
import com.craftzero.entity.mob.Mob;
import com.craftzero.inventory.ItemType;
import com.craftzero.main.CombatRules;
import com.craftzero.main.Player;
import com.craftzero.physics.Raycast;
import com.craftzero.world.World;
import org.joml.Vector3f;

import java.util.Random;

public class ThrownItemEntity extends Entity {
    private static final float SIZE = 0.25f;
    public static final int DESPAWN_TICKS = 1200;
    private static final int SHOOTER_COLLISION_GRACE_TICKS = 2;
    private static final float GRAVITY_PER_TICK = 0.03f;
    private static final float DRAG = 0.99f;
    private static final float WATER_DRAG = 0.8f;

    private final ItemType itemType;
    private Entity shooter;
    private String remoteShooterPlayerId = "";
    private final boolean playerOwned;
    private final Random injectedRandom;

    public ThrownItemEntity(float x, float y, float z, float motionX, float motionY, float motionZ,
            ItemType itemType, Entity shooter) {
        this(x, y, z, motionX, motionY, motionZ, itemType, shooter, false, null);
    }

    public ThrownItemEntity(float x, float y, float z, float motionX, float motionY, float motionZ,
            ItemType itemType, Entity shooter, boolean playerOwned) {
        this(x, y, z, motionX, motionY, motionZ, itemType, shooter, playerOwned, null);
    }

    ThrownItemEntity(float x, float y, float z, float motionX, float motionY, float motionZ,
            ItemType itemType, Entity shooter, Random random) {
        this(x, y, z, motionX, motionY, motionZ, itemType, shooter, false, random);
    }

    ThrownItemEntity(float x, float y, float z, float motionX, float motionY, float motionZ,
            ItemType itemType, Entity shooter, boolean playerOwned, Random random) {
        super(SIZE, SIZE);
        this.itemType = itemType == null ? ItemType.SNOWBALL : itemType;
        this.shooter = shooter;
        this.playerOwned = playerOwned;
        this.injectedRandom = random;
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
            remove();
            return;
        }
        Vector3f direction = motion.normalize(new Vector3f());

        Raycast.RaycastResult blockHit = Raycast.cast(world, origin, direction, distance);
        Raycast.EntityRaycastResult entityHit = Raycast.castEntitiesMatching(
                world.getEntitiesIncludingPending(), origin, direction, distance + 0.25f, shooterCollisionExclusion(),
                ThrownItemEntity::isProjectileCollisionTarget);
        PlayerHit playerHit = findPlayerHit(origin, direction, distance + 0.25f);
        World.ProjectilePlayerHit remotePlayerHit = world.findRemoteProjectilePlayerHit(origin, direction,
                distance + 0.25f, remoteShooterCollisionExclusion());

        float blockDistance = blockHit.hit ? blockHit.distance : Float.MAX_VALUE;
        float entityDistance = entityHit.hit ? entityHit.distance : Float.MAX_VALUE;
        float playerDistance = playerHit.hit ? playerHit.distance : Float.MAX_VALUE;
        float remotePlayerDistance = remotePlayerHit.hit() ? remotePlayerHit.distance() : Float.MAX_VALUE;
        if (entityHit.hit && entityDistance <= blockDistance && entityDistance <= playerDistance
                && entityDistance <= remotePlayerDistance
                && entityHit.entity instanceof FireballEntity fireball) {
            impactFireball(fireball, direction,
                    entityHit.hitPoint != null ? entityHit.hitPoint : pointAt(origin, direction, entityDistance));
            return;
        }
        if (entityHit.hit && entityDistance <= blockDistance && entityDistance <= playerDistance
                && entityDistance <= remotePlayerDistance
                && entityHit.entity instanceof PaintingEntity painting) {
            impactPainting(painting, entityHit.hitPoint != null ? entityHit.hitPoint
                    : pointAt(origin, direction, entityDistance));
            return;
        }
        if (entityHit.hit && entityDistance <= blockDistance && entityDistance <= playerDistance
                && entityDistance <= remotePlayerDistance
                && isVehicle(entityHit.entity)) {
            impactVehicle(entityHit.hitPoint != null ? entityHit.hitPoint : pointAt(origin, direction, entityDistance));
            return;
        }
        if (entityHit.hit && entityDistance <= blockDistance && entityDistance <= playerDistance
                && entityDistance <= remotePlayerDistance
                && entityHit.entity instanceof LivingEntity living) {
            impact(living, entityHit.hitPoint != null ? entityHit.hitPoint : pointAt(origin, direction, entityDistance));
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
        updateInWater();
        updateWaterEntryParticles();
        if (inWater) {
            world.spawnProjectileWaterBubbleTrail(x, y, z, motionX, motionY, motionZ);
        }
        float drag = inWater ? WATER_DRAG : DRAG;
        motionX *= drag;
        motionY = motionY * drag - GRAVITY_PER_TICK;
        motionZ *= drag;
        updateRotationFromMotion();
    }

    private void impact(LivingEntity target, Vector3f hitPoint) {
        if (hitPoint != null) {
            setPosition(hitPoint.x, hitPoint.y, hitPoint.z);
        }
        if (target instanceof EndCrystalEntity crystal) {
            crystal.damage(0.0f, thrownDamageSource());
        } else if (target != null) {
            float damage = target instanceof Blaze && itemType == ItemType.SNOWBALL ? 3.0f : 0.0f;
            if (applyProjectileImpact(target, damage)) {
                applyImpactKnockback(target);
            }
        }
        finishImpact();
    }

    private boolean applyProjectileImpact(LivingEntity target, float damage) {
        if (damage > 0.0f) {
            return target.damage(damage, thrownDamageSource());
        }
        DamageSource source = thrownDamageSource();
        if (target instanceof Enderman) {
            return target.damage(0.0f, source);
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
        return true;
    }

    private void impactVehicle(Vector3f hitPoint) {
        if (hitPoint != null) {
            setPosition(hitPoint.x, hitPoint.y, hitPoint.z);
        }
        finishImpact();
    }

    private void impactPainting(PaintingEntity painting, Vector3f hitPoint) {
        if (hitPoint != null) {
            setPosition(hitPoint.x, hitPoint.y, hitPoint.z);
        }
        if (painting != null && !painting.isRemoved()) {
            painting.breakAsItem(false);
        }
        finishImpact();
    }

    private void applyImpactKnockback(LivingEntity target) {
        float horizontal = (float) Math.sqrt(motionX * motionX + motionZ * motionZ);
        if (target == null || horizontal <= 0.0001f) {
            return;
        }
        target.addMotion(
                motionX / horizontal * CombatRules.ARROW_HORIZONTAL_KNOCKBACK,
                CombatRules.ARROW_VERTICAL_KNOCKBACK,
                motionZ / horizontal * CombatRules.ARROW_HORIZONTAL_KNOCKBACK);
    }

    private void impactFireball(FireballEntity fireball, Vector3f direction, Vector3f hitPoint) {
        if (hitPoint != null) {
            setPosition(hitPoint.x, hitPoint.y, hitPoint.z);
        }
        if (fireball != null && !fireball.isRemoved()) {
            fireball.deflectFromProjectile(direction, playerOwned);
            if (playerOwned && !remoteShooterPlayerId.isBlank()) {
                fireball.setRemoteDeflectorPlayerId(remoteShooterPlayerId);
            }
        }
        finishImpact();
    }

    private void finishImpact() {
        spawnImpactParticles();
        if (itemType == ItemType.EGG) {
            hatchEgg();
        }
        remove();
    }

    private void spawnImpactParticles() {
        if (world != null) {
            if (itemType == ItemType.SNOWBALL || itemType == ItemType.EGG) {
                world.spawnSnowballPoofParticles(x, y, z);
            } else {
                world.spawnItemBreakParticles(itemType, x, y, z);
            }
        }
    }

    private DamageSource thrownDamageSource() {
        DamageSource source = DamageSource.thrownProjectile(this, playerOwned);
        return playerOwned && !remoteShooterPlayerId.isBlank()
                ? source.withPlayerId(remoteShooterPlayerId)
                : source;
    }

    private static boolean isProjectileCollisionTarget(Entity entity) {
        return entity instanceof LivingEntity
                || entity instanceof FireballEntity
                || entity instanceof PaintingEntity
                || isVehicle(entity);
    }

    private static boolean isVehicle(Entity entity) {
        return entity instanceof BoatEntity || entity instanceof MinecartEntity;
    }

    public boolean isPlayerOwned() {
        return playerOwned;
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
    }

    private Entity shooterCollisionExclusion() {
        return ticksExisted < SHOOTER_COLLISION_GRACE_TICKS ? shooter : null;
    }

    private String remoteShooterCollisionExclusion() {
        return ticksExisted < SHOOTER_COLLISION_GRACE_TICKS ? remoteShooterPlayerId : "";
    }

    private static Vector3f pointAt(Vector3f origin, Vector3f direction, float distance) {
        return new Vector3f(
                origin.x + direction.x * distance,
                origin.y + direction.y * distance,
                origin.z + direction.z * distance);
    }

    private void hatchEgg() {
        if (world == null) {
            return;
        }
        Random random = randomSource();
        if (random.nextInt(8) != 0) {
            return;
        }
        int count = random.nextInt(32) == 0 ? 4 : 1;
        for (int i = 0; i < count; i++) {
            Chicken chicken = new Chicken();
            chicken.setGrowingAge(Mob.BABY_GROWING_AGE);
            chicken.setPosition(x, y, z);
            world.spawnEntity(chicken);
        }
    }

    private Random randomSource() {
        return injectedRandom != null ? injectedRandom : world.getRandom();
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

    private void updateRotationFromMotion() {
        float horizontal = (float) Math.sqrt(motionX * motionX + motionZ * motionZ);
        if (horizontal > 0.0001f || Math.abs(motionY) > 0.0001f) {
            yaw = (float) Math.toDegrees(Math.atan2(motionX, -motionZ));
            pitch = (float) Math.toDegrees(Math.atan2(motionY, horizontal));
        }
    }

    public ItemType getItemType() {
        return itemType;
    }

    private record PlayerHit(boolean hit, Player player, Vector3f hitPoint, float distance) {
        static PlayerHit miss() {
            return new PlayerHit(false, null, null, Float.MAX_VALUE);
        }
    }
}
