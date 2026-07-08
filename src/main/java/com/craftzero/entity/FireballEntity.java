package com.craftzero.entity;

import com.craftzero.combat.DamageSource;
import com.craftzero.entity.mob.Ghast;
import com.craftzero.main.CombatRules;
import com.craftzero.main.Player;
import com.craftzero.physics.Raycast;
import com.craftzero.world.BlockType;
import com.craftzero.world.World;
import com.craftzero.world.WorldParticle;
import org.joml.Vector3f;
import org.joml.Vector3i;

public class FireballEntity extends Entity {
    public static final int DESPAWN_TICKS = 600;
    private static final int SHOOTER_COLLISION_GRACE_TICKS = 5;
    private static final int DEFLECTOR_COLLISION_GRACE_TICKS = 2;
    private static final float TRAIL_SMOKE_Y_OFFSET = 0.5f;
    private static final float TRAIL_SMOKE_SCALE = 0.20f;
    private static final int TRAIL_SMOKE_LIFETIME_TICKS = 16;
    private Entity shooter;
    private String remoteDeflectorPlayerId = "";
    private final boolean explosive;
    private boolean deflectedByPlayer;
    private int deflectorCollisionGraceTicks;

    public FireballEntity(float x, float y, float z, float motionX, float motionY, float motionZ,
            Entity shooter, boolean explosive) {
        super(explosive ? 0.6f : 0.35f, explosive ? 0.6f : 0.35f);
        this.shooter = shooter;
        this.explosive = explosive;
        setPosition(x, y, z);
        setMotion(motionX, motionY, motionZ);
        updateRotationFromMotion();
    }

    @Override
    public void tick() {
        super.tick();
        if (deflectorCollisionGraceTicks > 0) {
            deflectorCollisionGraceTicks--;
        }
        if (ticksExisted > DESPAWN_TICKS) {
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
                world.getEntitiesIncludingPending(), origin, direction, distance + width, shooterCollisionExclusion(),
                FireballEntity::isProjectileCollisionTarget);
        PlayerHit playerHit = findPlayerHit(origin, direction, distance + width);
        World.ProjectilePlayerHit remotePlayerHit = world.findRemoteProjectilePlayerHit(origin, direction,
                distance + width, remoteDeflectorCollisionExclusion());

        float blockDistance = blockHit.hit ? blockHit.distance : Float.MAX_VALUE;
        float entityDistance = entityHit.hit ? entityHit.distance : Float.MAX_VALUE;
        float playerDistance = playerHit.hit ? playerHit.distance : Float.MAX_VALUE;
        float remotePlayerDistance = remotePlayerHit.hit() ? remotePlayerHit.distance() : Float.MAX_VALUE;

        if (entityHit.hit && entityDistance <= blockDistance && entityDistance <= playerDistance
                && entityDistance <= remotePlayerDistance
                && entityHit.entity instanceof LivingEntity living) {
            hit(living, entityHit.hitPoint != null ? entityHit.hitPoint : pointAt(origin, direction, entityDistance));
            return;
        }
        if (entityHit.hit && entityDistance <= blockDistance && entityDistance <= playerDistance
                && entityDistance <= remotePlayerDistance
                && entityHit.entity instanceof PaintingEntity painting) {
            hitPainting(painting, entityHit.hitPoint != null ? entityHit.hitPoint
                    : pointAt(origin, direction, entityDistance));
            return;
        }
        if (entityHit.hit && entityDistance <= blockDistance && entityDistance <= playerDistance
                && entityDistance <= remotePlayerDistance
                && isVehicle(entityHit.entity)) {
            hitVehicle(entityHit.entity, entityHit.hitPoint != null ? entityHit.hitPoint
                    : pointAt(origin, direction, entityDistance));
            return;
        }
        if (playerHit.hit && playerDistance <= blockDistance && playerDistance <= entityDistance
                && playerDistance <= remotePlayerDistance) {
            hit(playerHit.player, playerHit.hitPoint);
            return;
        }
        if (remotePlayerHit.hit() && remotePlayerDistance <= blockDistance && remotePlayerDistance <= entityDistance
                && remotePlayerDistance <= playerDistance) {
            hitRemotePlayer(remotePlayerHit);
            return;
        }
        if (blockHit.hit) {
            Vector3f hitPoint = blockHit.hitPoint != null ? blockHit.hitPoint
                    : pointAt(origin, direction, blockDistance);
            setPosition(hitPoint.x, hitPoint.y, hitPoint.z);
            igniteBlockImpact(blockHit);
            explodeOrRemove(x, y, z);
            return;
        }

        x += motionX;
        y += motionY;
        z += motionZ;
        updateRotationFromMotion();
        updateInWater();
        updateWaterEntryParticles();
        emitFlightTrailParticles();
    }

    private void emitFlightTrailParticles() {
        if (world == null) {
            return;
        }
        if (inWater) {
            world.spawnProjectileWaterBubbleTrail(x, y, z, motionX, motionY, motionZ);
        }
        world.spawnParticle(WorldParticle.Type.SMOKE,
                x, y + TRAIL_SMOKE_Y_OFFSET, z,
                0.0f, 0.0f, 0.0f,
                TRAIL_SMOKE_SCALE, TRAIL_SMOKE_LIFETIME_TICKS);
    }

    private void hit(LivingEntity target, Vector3f hitPoint) {
        if (hitPoint != null) {
            setPosition(hitPoint.x, hitPoint.y, hitPoint.z);
        }
        if (target != null) {
            if (target instanceof EndCrystalEntity crystal) {
                crystal.damage(0.0f, DamageSource.entity(DamageSource.Type.GENERIC, this));
            } else {
                boolean applied = target.damage(explosive ? 6.0f : 5.0f, directHitDamageSource());
                recordReturnedFireballKill(target, applied);
                if (applied && !explosive) {
                    target.setOnFire(100);
                }
            }
        }
        explodeOrRemove(x, y, z);
    }

    private void hitPainting(PaintingEntity painting, Vector3f hitPoint) {
        if (hitPoint != null) {
            setPosition(hitPoint.x, hitPoint.y, hitPoint.z);
        }
        if (painting != null && !painting.isRemoved()) {
            painting.breakAsItem(false);
        }
        explodeOrRemove(x, y, z);
    }

    private void hitVehicle(Entity vehicle, Vector3f hitPoint) {
        if (hitPoint != null) {
            setPosition(hitPoint.x, hitPoint.y, hitPoint.z);
        }
        if (!explosive) {
            if (vehicle instanceof BoatEntity boat && !boat.isRemoved()) {
                boat.attack(5.0f, false);
            } else if (vehicle instanceof MinecartEntity cart && !cart.isRemoved()) {
                cart.attack(5.0f, false);
            }
        }
        explodeOrRemove(x, y, z);
    }

    private void hit(Player player, Vector3f hitPoint) {
        if (hitPoint != null) {
            setPosition(hitPoint.x, hitPoint.y, hitPoint.z);
        }
        if (player != null) {
            boolean applied = player.hurt(explosive ? 6.0f : 5.0f, directHitDamageSource());
            if (applied && !explosive) {
                player.setOnFire(100);
            }
        }
        explodeOrRemove(x, y, z);
    }

    private void hitRemotePlayer(World.ProjectilePlayerHit hit) {
        if (hit != null && hit.hit() && world != null) {
            if (hit.hitPoint() != null) {
                setPosition(hit.hitPoint().x, hit.hitPoint().y, hit.hitPoint().z);
            }
            world.damageRemoteProjectilePlayer(hit,
                    new World.ProjectilePlayerDamage(
                            explosive ? 6.0f : 5.0f,
                            "fire",
                            x, y, z,
                            CombatRules.ARROW_HORIZONTAL_KNOCKBACK,
                            CombatRules.ARROW_VERTICAL_KNOCKBACK,
                            explosive ? 0 : 100,
                            projectileSourcePlayerId()));
        }
        explodeOrRemove(x, y, z);
    }

    private void explodeOrRemove(float ex, float ey, float ez) {
        if (explosive && world != null) {
            world.explode(ex, ey, ez, 1.5f, true,
                    deflectedByPlayer ? directHitDamageSource() : null);
        }
        remove();
    }

    private static boolean isProjectileCollisionTarget(Entity entity) {
        return entity instanceof LivingEntity || entity instanceof PaintingEntity || isVehicle(entity);
    }

    private static boolean isVehicle(Entity entity) {
        return entity instanceof BoatEntity || entity instanceof MinecartEntity;
    }

    private DamageSource directHitDamageSource() {
        if (deflectedByPlayer) {
            return new DamageSource(DamageSource.Type.GENERIC, this, true, x, y, z,
                    CombatRules.ARROW_HORIZONTAL_KNOCKBACK,
                    CombatRules.ARROW_VERTICAL_KNOCKBACK,
                    0, true, remoteDeflectorPlayerId);
        }
        return DamageSource.entity(DamageSource.Type.FIRE, this,
                CombatRules.ARROW_HORIZONTAL_KNOCKBACK,
                CombatRules.ARROW_VERTICAL_KNOCKBACK);
    }

    private String projectileSourcePlayerId() {
        if (remoteDeflectorPlayerId != null && !remoteDeflectorPlayerId.isBlank()) {
            return remoteDeflectorPlayerId.trim();
        }
        return deflectedByPlayer ? "host" : "";
    }

    private void recordReturnedFireballKill(LivingEntity target, boolean damageApplied) {
        if (!damageApplied || !explosive || !deflectedByPlayer || !(target instanceof Ghast)
                || target.getHealth() > 0.0f || world == null || world.getPlayer() == null) {
            return;
        }
        world.getPlayer().getStats().getAchievements().recordReturnedFireballKill();
    }

    private void igniteBlockImpact(Raycast.RaycastResult blockHit) {
        if (explosive || world == null || blockHit == null || blockHit.previousBlockPos == null) {
            return;
        }
        Vector3i pos = blockHit.previousBlockPos;
        if (world.getBlockIfLoaded(pos.x, pos.y, pos.z, BlockType.BEDROCK) == BlockType.AIR) {
            world.setBlockIfLoaded(pos.x, pos.y, pos.z, BlockType.FIRE, 0);
        }
    }

    private PlayerHit findPlayerHit(Vector3f origin, Vector3f direction, float maxDistance) {
        if (world.getPlayer() == null || (deflectedByPlayer && deflectorCollisionGraceTicks > 0)) {
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

    private void updateRotationFromMotion() {
        float horizontal = (float) Math.sqrt(motionX * motionX + motionZ * motionZ);
        if (horizontal > 0.0001f || Math.abs(motionY) > 0.0001f) {
            yaw = (float) Math.toDegrees(Math.atan2(motionX, -motionZ));
            pitch = (float) Math.toDegrees(Math.atan2(motionY, horizontal));
        }
    }

    public boolean isExplosive() {
        return explosive;
    }

    public boolean isDeflectedByPlayer() {
        return deflectedByPlayer;
    }

    public Entity getShooter() {
        return shooter;
    }

    public void restoreShooter(Entity shooter) {
        this.shooter = shooter instanceof LivingEntity ? shooter : null;
        if (this.shooter != null) {
            deflectorCollisionGraceTicks = 0;
            remoteDeflectorPlayerId = "";
        }
    }

    private Entity shooterCollisionExclusion() {
        return ticksExisted < SHOOTER_COLLISION_GRACE_TICKS ? shooter : null;
    }

    public boolean deflectFromPlayer(Vector3f direction) {
        return deflectFromProjectile(direction, true);
    }

    public boolean deflectFromProjectile(Vector3f direction, boolean playerCredit) {
        if (direction == null || direction.lengthSquared() < 0.000001f) {
            return false;
        }
        float speed = (float) Math.sqrt(motionX * motionX + motionY * motionY + motionZ * motionZ);
        if (speed < 0.1f) {
            speed = explosive ? 0.55f : 0.45f;
        }
        Vector3f normalized = direction.normalize(new Vector3f());
        motionX = normalized.x * speed;
        motionY = normalized.y * speed;
        motionZ = normalized.z * speed;
        shooter = null;
        deflectedByPlayer = playerCredit;
        deflectorCollisionGraceTicks = playerCredit ? DEFLECTOR_COLLISION_GRACE_TICKS : 0;
        remoteDeflectorPlayerId = "";
        updateRotationFromMotion();
        return true;
    }

    public String getRemoteDeflectorPlayerId() {
        return remoteDeflectorPlayerId;
    }

    public void setRemoteDeflectorPlayerId(String remoteDeflectorPlayerId) {
        this.remoteDeflectorPlayerId = remoteDeflectorPlayerId == null ? "" : remoteDeflectorPlayerId;
    }

    public void setDeflectedByPlayer(boolean deflectedByPlayer) {
        this.deflectedByPlayer = deflectedByPlayer;
        if (deflectedByPlayer) {
            shooter = null;
            deflectorCollisionGraceTicks = DEFLECTOR_COLLISION_GRACE_TICKS;
        } else {
            deflectorCollisionGraceTicks = 0;
            remoteDeflectorPlayerId = "";
        }
    }

    private String remoteDeflectorCollisionExclusion() {
        return deflectedByPlayer && deflectorCollisionGraceTicks > 0 ? remoteDeflectorPlayerId : "";
    }

    private record PlayerHit(boolean hit, Player player, Vector3f hitPoint, float distance) {
        static PlayerHit miss() {
            return new PlayerHit(false, null, null, Float.MAX_VALUE);
        }
    }
}
