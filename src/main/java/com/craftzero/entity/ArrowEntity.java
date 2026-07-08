package com.craftzero.entity;

import com.craftzero.combat.DamageSource;
import com.craftzero.entity.mob.Mob;
import com.craftzero.entity.mob.Skeleton;
import com.craftzero.inventory.ItemType;
import com.craftzero.main.CombatRules;
import com.craftzero.main.Player;
import com.craftzero.physics.Raycast;
import com.craftzero.world.BlockType;
import com.craftzero.world.World;
import com.craftzero.world.WorldParticle;
import org.joml.Vector3f;

/**
 * Release 1.0-style arrow projectile.
 */
public class ArrowEntity extends Entity {
    private static final float SIZE = 0.25f;
    public static final int STUCK_DESPAWN_TICKS = 1200;
    private static final int SHOOTER_COLLISION_GRACE_TICKS = 5;
    private static final float GRAVITY_PER_TICK = 0.05f;
    private static final float DRAG = 0.99f;
    private static final float WATER_DRAG = 0.8f;

    private Entity shooter;
    private String remoteShooterPlayerId = "";
    private final boolean playerOwned;
    private final float damage;
    private final float launchX;
    private final float launchY;
    private final float launchZ;
    private float knockbackHorizontal = CombatRules.ARROW_HORIZONTAL_KNOCKBACK;
    private float knockbackVertical = CombatRules.ARROW_VERTICAL_KNOCKBACK;
    private int fireTicksOnHit;
    private boolean critical;
    private boolean inGround;
    private int stuckTicks;
    private int blockX;
    private int blockY;
    private int blockZ;
    private BlockType stuckBlockType;

    public ArrowEntity(float x, float y, float z, float motionX, float motionY, float motionZ,
            Entity shooter, boolean playerOwned, float damage) {
        super(SIZE, SIZE);
        this.shooter = shooter;
        this.playerOwned = playerOwned;
        this.damage = damage;
        this.launchX = x;
        this.launchY = y;
        this.launchZ = z;
        setPosition(x, y, z);
        setMotion(motionX, motionY, motionZ);
        updateRotationFromMotion();
    }

    @Override
    public void tick() {
        super.tick();
        if (inGround) {
            stuckTicks++;
            tryPickup();
            if (stuckTicks >= STUCK_DESPAWN_TICKS) {
                remove();
            }
            return;
        }
    }

    @Override
    public void updatePhysics(float deltaTime) {
        if (world == null || removed || inGround) {
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
                ArrowEntity::isProjectileCollisionTarget);
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
            deflectFireball(fireball, direction);
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
        if (entityHit.hit && entityDistance <= blockDistance && entityDistance <= playerDistance
                && entityDistance <= remotePlayerDistance
                && entityHit.entity instanceof LivingEntity living) {
            hitEntity(living, direction);
            return;
        }
        if (playerHit.hit && playerDistance <= blockDistance && playerDistance <= entityDistance
                && playerDistance <= remotePlayerDistance) {
            hitPlayer(playerHit.player, direction);
            return;
        }
        if (remotePlayerHit.hit() && remotePlayerDistance <= blockDistance && remotePlayerDistance <= entityDistance
                && remotePlayerDistance <= playerDistance) {
            hitRemotePlayer(remotePlayerHit);
            return;
        }
        if (blockHit.hit) {
            stickInBlock(blockHit, origin, direction);
            return;
        }

        spawnCriticalTrail(origin, motion);

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

    private void hitEntity(LivingEntity target, Vector3f direction) {
        if (target == null || target.isDead()) {
            return;
        }
        boolean applied = target.damage(damageWithCriticalBonus(), arrowDamageSource());
        if (applied && fireTicksOnHit > 0) {
            target.setOnFire(fireTicksOnHit);
        }
        if (applied && target.getHealth() <= 0.0f) {
            recordPlayerOwnedKill(target);
        }
        remove();
    }

    private void deflectFireball(FireballEntity fireball, Vector3f direction) {
        if (fireball == null || fireball.isRemoved()) {
            return;
        }
        fireball.deflectFromProjectile(direction, playerOwned);
        if (playerOwned && !remoteShooterPlayerId.isBlank()) {
            fireball.setRemoteDeflectorPlayerId(remoteShooterPlayerId);
        }
        remove();
    }

    private void hitPainting(PaintingEntity painting, Vector3f hitPoint) {
        if (hitPoint != null) {
            setPosition(hitPoint.x, hitPoint.y, hitPoint.z);
        }
        if (painting != null && !painting.isRemoved()) {
            painting.breakAsItem(false);
        }
        remove();
    }

    private void hitVehicle(Entity vehicle, Vector3f hitPoint) {
        if (hitPoint != null) {
            setPosition(hitPoint.x, hitPoint.y, hitPoint.z);
        }
        if (vehicle instanceof BoatEntity boat && !boat.isRemoved()) {
            boat.attack(damageWithCriticalBonus(), false);
        } else if (vehicle instanceof MinecartEntity cart && !cart.isRemoved()) {
            cart.attack(damageWithCriticalBonus(), false);
        }
        remove();
    }

    private static boolean isProjectileCollisionTarget(Entity entity) {
        return entity instanceof LivingEntity || entity instanceof FireballEntity || entity instanceof PaintingEntity
                || isVehicle(entity);
    }

    private static boolean isVehicle(Entity entity) {
        return entity instanceof BoatEntity || entity instanceof MinecartEntity;
    }

    private static Vector3f pointAt(Vector3f origin, Vector3f direction, float distance) {
        return new Vector3f(
                origin.x + direction.x * distance,
                origin.y + direction.y * distance,
                origin.z + direction.z * distance);
    }

    private void hitPlayer(Player player, Vector3f direction) {
        if (player == null || player.isDead()) {
            return;
        }
        boolean applied = player.hurt(damageWithCriticalBonus(), arrowDamageSource());
        if (applied && fireTicksOnHit > 0) {
            player.setOnFire(fireTicksOnHit);
        }
        remove();
    }

    private void hitRemotePlayer(World.ProjectilePlayerHit hit) {
        if (hit == null || !hit.hit() || world == null) {
            return;
        }
        if (hit.hitPoint() != null) {
            setPosition(hit.hitPoint().x, hit.hitPoint().y, hit.hitPoint().z);
        }
        world.damageRemoteProjectilePlayer(hit,
                new World.ProjectilePlayerDamage(
                        damageWithCriticalBonus(),
                        "arrow",
                        x, y, z,
                        knockbackHorizontal,
                        knockbackVertical,
                        fireTicksOnHit,
                        projectileSourcePlayerId()));
        remove();
    }

    private DamageSource arrowDamageSource() {
        DamageSource source = DamageSource.entity(DamageSource.Type.ARROW, this,
                knockbackHorizontal,
                knockbackVertical);
        if (playerOwned) {
            source = source.withPlayerCredit(true);
        }
        String sourcePlayerId = projectileSourcePlayerId();
        return sourcePlayerId.isBlank() ? source : source.withPlayerId(sourcePlayerId);
    }

    private String projectileSourcePlayerId() {
        if (remoteShooterPlayerId != null && !remoteShooterPlayerId.isBlank()) {
            return remoteShooterPlayerId.trim();
        }
        return playerOwned ? "host" : "";
    }

    private float damageWithCriticalBonus() {
        if (!critical || world == null) {
            return damage;
        }
        int base = Math.max(1, (int) Math.ceil(damage));
        return damage + world.getRandom().nextInt(base / 2 + 2);
    }

    private void spawnCriticalTrail(Vector3f origin, Vector3f motion) {
        if (!critical || world == null) {
            return;
        }
        for (int i = 0; i < 4; i++) {
            float step = i / 4.0f;
            world.spawnParticle(WorldParticle.Type.CRIT,
                    origin.x + motion.x * step,
                    origin.y + motion.y * step,
                    origin.z + motion.z * step,
                    -motion.x,
                    -motion.y + 0.2f,
                    -motion.z,
                    0.16f,
                    8);
        }
    }

    private void recordPlayerOwnedKill(LivingEntity target) {
        if (!playerOwned || target == null || world == null || world.getPlayer() == null) {
            return;
        }
        if (target instanceof Mob mob && mob.isHostile()) {
            world.getPlayer().getStats().getAchievements().recordMonsterKilled();
        }
        if (target instanceof Skeleton) {
            world.getPlayer().getStats().getAchievements().recordSkeletonSniped(distanceFromLaunch(target));
        }
    }

    private float distanceFromLaunch(LivingEntity target) {
        float dx = target.getX() - launchX;
        float dy = target.getY() - launchY;
        float dz = target.getZ() - launchZ;
        return (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    private void stickInBlock(Raycast.RaycastResult hit, Vector3f origin, Vector3f direction) {
        float impactDistance = Math.max(0.0f, hit.distance - 0.02f);
        x = origin.x + direction.x * impactDistance;
        y = origin.y + direction.y * impactDistance;
        z = origin.z + direction.z * impactDistance;
        motionX = direction.x * 0.05f;
        motionY = direction.y * 0.05f;
        motionZ = direction.z * 0.05f;
        inGround = true;
        stuckTicks = 0;
        blockX = hit.blockPos.x;
        blockY = hit.blockPos.y;
        blockZ = hit.blockPos.z;
        stuckBlockType = world == null ? null : world.getBlockIfLoaded(blockX, blockY, blockZ, BlockType.AIR);
    }

    private PlayerHit findPlayerHit(Vector3f origin, Vector3f direction, float maxDistance) {
        if (world.getPlayer() == null || (playerOwned && ticksExisted < SHOOTER_COLLISION_GRACE_TICKS)) {
            return PlayerHit.miss();
        }
        Player player = world.getPlayer();
        float distance = Raycast.intersectsAabb(origin, direction, Raycast.playerPickBox(player.getBoundingBox()));
        return distance >= 0 && distance <= maxDistance ? new PlayerHit(true, player, distance) : PlayerHit.miss();
    }

    private Entity shooterCollisionExclusion() {
        return ticksExisted < SHOOTER_COLLISION_GRACE_TICKS ? shooter : null;
    }

    private String remoteShooterCollisionExclusion() {
        return ticksExisted < SHOOTER_COLLISION_GRACE_TICKS ? remoteShooterPlayerId : "";
    }

    private void tryPickup() {
        if (!playerOwned || world == null || world.getPlayer() == null) {
            return;
        }
        BlockType currentBlock = world.getBlockIfLoaded(blockX, blockY, blockZ, BlockType.AIR);
        if (stuckBlockType == null && currentBlock != BlockType.AIR) {
            stuckBlockType = currentBlock;
        }
        if (currentBlock == BlockType.AIR || currentBlock != stuckBlockType) {
            releaseFromStuckBlock();
            return;
        }
        Player player = world.getPlayer();
        Vector3f pos = player.getPosition();
        float dx = pos.x - x;
        float dy = (pos.y + 1.0f) - y;
        float dz = pos.z - z;
        if (dx * dx + dy * dy + dz * dz <= 1.2f * 1.2f && player.addToInventory(ItemType.ARROW, 1)) {
            world.playItemPickupSound(x, y, z);
            remove();
        }
    }

    private void releaseFromStuckBlock() {
        inGround = false;
        stuckTicks = 0;
        stuckBlockType = null;
        float speedSq = motionX * motionX + motionY * motionY + motionZ * motionZ;
        if (speedSq < 0.0001f) {
            float pitchRad = (float) Math.toRadians(pitch);
            float yawRad = (float) Math.toRadians(yaw);
            float horizontal = (float) Math.cos(pitchRad) * 0.05f;
            motionX = (float) Math.sin(yawRad) * horizontal;
            motionY = (float) Math.sin(pitchRad) * 0.05f;
            motionZ = -(float) Math.cos(yawRad) * horizontal;
        }
    }

    private void updateRotationFromMotion() {
        float horizontal = (float) Math.sqrt(motionX * motionX + motionZ * motionZ);
        if (horizontal > 0.0001f || Math.abs(motionY) > 0.0001f) {
            yaw = (float) Math.toDegrees(Math.atan2(motionX, -motionZ));
            pitch = (float) Math.toDegrees(Math.atan2(motionY, horizontal));
        }
    }

    public boolean isInGround() {
        return inGround;
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

    public float getDamage() {
        return damage;
    }

    public float getKnockbackHorizontal() {
        return knockbackHorizontal;
    }

    public float getKnockbackVertical() {
        return knockbackVertical;
    }

    public int getFireTicksOnHit() {
        return fireTicksOnHit;
    }

    public boolean isCritical() {
        return critical;
    }

    public int getStuckTicks() {
        return stuckTicks;
    }

    public int getBlockX() {
        return blockX;
    }

    public int getBlockY() {
        return blockY;
    }

    public int getBlockZ() {
        return blockZ;
    }

    public void setKnockback(float horizontal, float vertical) {
        this.knockbackHorizontal = Math.max(0.0f, horizontal);
        this.knockbackVertical = Math.max(0.0f, vertical);
    }

    public void setFireTicksOnHit(int fireTicksOnHit) {
        this.fireTicksOnHit = Math.max(0, fireTicksOnHit);
    }

    public void setCritical(boolean critical) {
        this.critical = critical;
    }

    public void setStuckInBlock(int blockX, int blockY, int blockZ, int stuckTicks) {
        this.inGround = true;
        this.stuckTicks = Math.max(0, stuckTicks);
        this.blockX = blockX;
        this.blockY = blockY;
        this.blockZ = blockZ;
        this.stuckBlockType = null;
    }

    public void restoreStuckState(boolean inGround, int blockX, int blockY, int blockZ, int stuckTicks) {
        this.inGround = inGround;
        this.stuckTicks = inGround ? Math.max(0, stuckTicks) : 0;
        this.blockX = blockX;
        this.blockY = blockY;
        this.blockZ = blockZ;
        this.stuckBlockType = null;
    }

    private record PlayerHit(boolean hit, Player player, float distance) {
        static PlayerHit miss() {
            return new PlayerHit(false, null, Float.MAX_VALUE);
        }
    }
}
