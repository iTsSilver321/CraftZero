package com.craftzero.entity;

import com.craftzero.combat.DamageSource;
import com.craftzero.main.CombatRules;
import com.craftzero.main.Player;
import com.craftzero.physics.Raycast;
import org.joml.Vector3f;

public class FireballEntity extends Entity {
    private static final int DESPAWN_TICKS = 600;
    private final Entity shooter;
    private final boolean explosive;

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
        Raycast.EntityRaycastResult entityHit = Raycast.castEntities(
                world.getEntities(), origin, direction, distance + width, shooter);
        PlayerHit playerHit = findPlayerHit(origin, direction, distance + width);

        float blockDistance = blockHit.hit ? blockHit.distance : Float.MAX_VALUE;
        float entityDistance = entityHit.hit ? entityHit.distance : Float.MAX_VALUE;
        float playerDistance = playerHit.hit ? playerHit.distance : Float.MAX_VALUE;

        if (entityDistance <= blockDistance && entityDistance <= playerDistance && entityHit.entity != null) {
            hit(entityHit.entity);
            return;
        }
        if (playerDistance <= blockDistance && playerDistance <= entityDistance) {
            hit(playerHit.player);
            return;
        }
        if (blockHit.hit) {
            explodeOrRemove(blockHit.blockPos.x + 0.5f, blockHit.blockPos.y + 0.5f, blockHit.blockPos.z + 0.5f);
            return;
        }

        x += motionX;
        y += motionY;
        z += motionZ;
        updateRotationFromMotion();
    }

    private void hit(LivingEntity target) {
        if (target != null) {
            target.damage(explosive ? 6.0f : 5.0f,
                    DamageSource.point(DamageSource.Type.FIRE, x, y, z,
                            CombatRules.ARROW_HORIZONTAL_KNOCKBACK,
                            CombatRules.ARROW_VERTICAL_KNOCKBACK));
            target.setOnFire(100);
        }
        explodeOrRemove(x, y, z);
    }

    private void hit(Player player) {
        if (player != null) {
            player.hurt(explosive ? 6.0f : 5.0f,
                    DamageSource.point(DamageSource.Type.FIRE, x, y, z,
                            CombatRules.ARROW_HORIZONTAL_KNOCKBACK,
                            CombatRules.ARROW_VERTICAL_KNOCKBACK));
        }
        explodeOrRemove(x, y, z);
    }

    private void explodeOrRemove(float ex, float ey, float ez) {
        if (explosive && world != null) {
            world.explode(ex, ey, ez, 1.5f);
        }
        remove();
    }

    private PlayerHit findPlayerHit(Vector3f origin, Vector3f direction, float maxDistance) {
        if (world.getPlayer() == null) {
            return PlayerHit.miss();
        }
        Player player = world.getPlayer();
        float distance = ArrowEntityRay.intersects(origin, direction, player.getBoundingBox().expand(0.1f));
        return distance >= 0 && distance <= maxDistance ? new PlayerHit(true, player, distance) : PlayerHit.miss();
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

    private record PlayerHit(boolean hit, Player player, float distance) {
        static PlayerHit miss() {
            return new PlayerHit(false, null, Float.MAX_VALUE);
        }
    }
}
