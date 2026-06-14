package com.craftzero.entity;

import com.craftzero.combat.DamageSource;
import com.craftzero.inventory.ItemType;
import com.craftzero.main.CombatRules;
import com.craftzero.main.Player;
import com.craftzero.physics.AABB;
import com.craftzero.physics.Raycast;
import com.craftzero.world.BlockType;
import org.joml.Vector3f;

/**
 * Release 1.0-style arrow projectile.
 */
public class ArrowEntity extends Entity {
    private static final float SIZE = 0.25f;
    private static final int DESPAWN_TICKS = 1200;
    private static final int STUCK_DESPAWN_TICKS = 1200;
    private static final float GRAVITY_PER_TICK = 0.05f;
    private static final float DRAG = 0.99f;

    private final Entity shooter;
    private final boolean playerOwned;
    private final float damage;
    private boolean inGround;
    private int stuckTicks;
    private int blockX;
    private int blockY;
    private int blockZ;

    public ArrowEntity(float x, float y, float z, float motionX, float motionY, float motionZ,
            Entity shooter, boolean playerOwned, float damage) {
        super(SIZE, SIZE);
        this.shooter = shooter;
        this.playerOwned = playerOwned;
        this.damage = damage;
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
        if (ticksExisted >= DESPAWN_TICKS) {
            remove();
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
        Raycast.EntityRaycastResult entityHit = Raycast.castEntities(
                world.getEntities(), origin, direction, distance + 0.25f, shooter);
        PlayerHit playerHit = findPlayerHit(origin, direction, distance + 0.25f);

        float blockDistance = blockHit.hit ? blockHit.distance : Float.MAX_VALUE;
        float entityDistance = entityHit.hit ? entityHit.distance : Float.MAX_VALUE;
        float playerDistance = playerHit.hit ? playerHit.distance : Float.MAX_VALUE;

        if (entityDistance <= blockDistance && entityDistance <= playerDistance && entityHit.entity != null) {
            hitEntity(entityHit.entity, direction);
            return;
        }
        if (playerDistance <= blockDistance && playerDistance <= entityDistance) {
            hitPlayer(playerHit.player, direction);
            return;
        }
        if (blockHit.hit) {
            stickInBlock(blockHit, origin, direction);
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

    private void hitEntity(LivingEntity target, Vector3f direction) {
        if (target == null || target.isDead()) {
            return;
        }
        target.damage(damage, DamageSource.entity(DamageSource.Type.ARROW, this,
                CombatRules.ARROW_HORIZONTAL_KNOCKBACK,
                CombatRules.ARROW_VERTICAL_KNOCKBACK));
        remove();
    }

    private void hitPlayer(Player player, Vector3f direction) {
        if (player == null || player.isDead()) {
            return;
        }
        player.hurt(damage, DamageSource.point(DamageSource.Type.ARROW, x, y, z,
                CombatRules.ARROW_HORIZONTAL_KNOCKBACK,
                CombatRules.ARROW_VERTICAL_KNOCKBACK));
        remove();
    }

    private void stickInBlock(Raycast.RaycastResult hit, Vector3f origin, Vector3f direction) {
        float impactDistance = Math.max(0.0f, hit.distance - 0.02f);
        x = origin.x + direction.x * impactDistance;
        y = origin.y + direction.y * impactDistance;
        z = origin.z + direction.z * impactDistance;
        motionX = 0;
        motionY = 0;
        motionZ = 0;
        inGround = true;
        stuckTicks = 0;
        blockX = hit.blockPos.x;
        blockY = hit.blockPos.y;
        blockZ = hit.blockPos.z;
    }

    private PlayerHit findPlayerHit(Vector3f origin, Vector3f direction, float maxDistance) {
        if (playerOwned || world.getPlayer() == null || ticksExisted < 3) {
            return PlayerHit.miss();
        }
        Player player = world.getPlayer();
        AABB box = player.getBoundingBox().expand(0.1f);
        float distance = rayIntersectsAABB(origin, direction, box);
        return distance >= 0 && distance <= maxDistance ? new PlayerHit(true, player, distance) : PlayerHit.miss();
    }

    private void tryPickup() {
        if (!playerOwned || world == null || world.getPlayer() == null) {
            return;
        }
        if (world.getBlockIfLoaded(blockX, blockY, blockZ, BlockType.AIR) == BlockType.AIR) {
            inGround = false;
            return;
        }
        Player player = world.getPlayer();
        Vector3f pos = player.getPosition();
        float dx = pos.x - x;
        float dy = (pos.y + 1.0f) - y;
        float dz = pos.z - z;
        if (dx * dx + dy * dy + dz * dz <= 1.2f * 1.2f && player.addToInventory(ItemType.ARROW, 1)) {
            remove();
        }
    }

    private void updateRotationFromMotion() {
        float horizontal = (float) Math.sqrt(motionX * motionX + motionZ * motionZ);
        if (horizontal > 0.0001f || Math.abs(motionY) > 0.0001f) {
            yaw = (float) Math.toDegrees(Math.atan2(motionX, -motionZ));
            pitch = (float) Math.toDegrees(Math.atan2(motionY, horizontal));
        }
    }

    private static float rayIntersectsAABB(Vector3f origin, Vector3f direction, AABB box) {
        float tMin = 0.0f;
        float tMax = Float.MAX_VALUE;
        float[] starts = { origin.x, origin.y, origin.z };
        float[] dirs = { direction.x, direction.y, direction.z };
        float[] mins = { box.getMin().x, box.getMin().y, box.getMin().z };
        float[] maxs = { box.getMax().x, box.getMax().y, box.getMax().z };

        for (int i = 0; i < 3; i++) {
            float dir = dirs[i];
            if (Math.abs(dir) < 0.0001f) {
                if (starts[i] < mins[i] || starts[i] > maxs[i]) {
                    return -1.0f;
                }
                continue;
            }
            float invD = 1.0f / dir;
            float t0 = (mins[i] - starts[i]) * invD;
            float t1 = (maxs[i] - starts[i]) * invD;
            if (invD < 0) {
                float tmp = t0;
                t0 = t1;
                t1 = tmp;
            }
            tMin = Math.max(tMin, t0);
            tMax = Math.min(tMax, t1);
            if (tMin > tMax) {
                return -1.0f;
            }
        }
        return tMin;
    }

    public boolean isInGround() {
        return inGround;
    }

    public boolean isPlayerOwned() {
        return playerOwned;
    }

    private record PlayerHit(boolean hit, Player player, float distance) {
        static PlayerHit miss() {
            return new PlayerHit(false, null, Float.MAX_VALUE);
        }
    }
}
