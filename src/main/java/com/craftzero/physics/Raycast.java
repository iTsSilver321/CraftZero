package com.craftzero.physics;

import com.craftzero.entity.Entity;
import com.craftzero.entity.LivingEntity;
import com.craftzero.world.BlockType;
import com.craftzero.world.World;
import org.joml.Vector3f;
import org.joml.Vector3i;

import java.util.List;

/**
 * DDA Raycasting for block selection, entity interaction, and combat.
 * Returns the exact block/entity coordinates hit by a ray.
 */
public class Raycast {

    /**
     * Result of a raycast operation.
     */
    public static class RaycastResult {
        public final boolean hit;
        public final Vector3i blockPos;
        public final Vector3i previousBlockPos; // For block placement
        public final int face; // 0=top, 1=bottom, 2=north, 3=south, 4=east, 5=west
        public final float distance;

        public RaycastResult(boolean hit, Vector3i blockPos, Vector3i previousBlockPos, int face, float distance) {
            this.hit = hit;
            this.blockPos = blockPos;
            this.previousBlockPos = previousBlockPos;
            this.face = face;
            this.distance = distance;
        }

        public static RaycastResult miss() {
            return new RaycastResult(false, null, null, -1, Float.MAX_VALUE);
        }
    }

    /**
     * Result of an entity raycast operation.
     */
    public static class EntityRaycastResult {
        public final boolean hit;
        public final LivingEntity entity;
        public final float distance;
        public final Vector3f hitPoint;

        public EntityRaycastResult(boolean hit, LivingEntity entity, float distance, Vector3f hitPoint) {
            this.hit = hit;
            this.entity = entity;
            this.distance = distance;
            this.hitPoint = hitPoint;
        }

        public static EntityRaycastResult miss() {
            return new EntityRaycastResult(false, null, Float.MAX_VALUE, null);
        }
    }

    /**
     * Cast a ray from origin in direction, returning the first solid block hit.
     * Uses DDA (Digital Differential Analyzer) algorithm.
     * 
     * @param world       The world to cast ray in
     * @param origin      Ray start position
     * @param direction   Ray direction (should be normalized)
     * @param maxDistance Maximum distance to check
     * @return RaycastResult with hit information
     */
    public static RaycastResult cast(World world, Vector3f origin, Vector3f direction, float maxDistance) {
        // Current voxel coordinates
        int x = (int) Math.floor(origin.x);
        int y = (int) Math.floor(origin.y);
        int z = (int) Math.floor(origin.z);

        // Previous position for block placement
        int prevX = x, prevY = y, prevZ = z;

        // Direction signs
        int stepX = direction.x > 0 ? 1 : (direction.x < 0 ? -1 : 0);
        int stepY = direction.y > 0 ? 1 : (direction.y < 0 ? -1 : 0);
        int stepZ = direction.z > 0 ? 1 : (direction.z < 0 ? -1 : 0);

        // Calculate tMax and tDelta
        float tMaxX = intBound(origin.x, direction.x);
        float tMaxY = intBound(origin.y, direction.y);
        float tMaxZ = intBound(origin.z, direction.z);

        float tDeltaX = direction.x != 0 ? Math.abs(1.0f / direction.x) : Float.MAX_VALUE;
        float tDeltaY = direction.y != 0 ? Math.abs(1.0f / direction.y) : Float.MAX_VALUE;
        float tDeltaZ = direction.z != 0 ? Math.abs(1.0f / direction.z) : Float.MAX_VALUE;

        float distance = 0;
        int face = -1;

        // March through voxels
        while (distance < maxDistance) {
            // Check current voxel
            BlockType block = world.getBlock(x, y, z);

            if (block.isSolid()) {
                return new RaycastResult(
                        true,
                        new Vector3i(x, y, z),
                        new Vector3i(prevX, prevY, prevZ),
                        face,
                        distance);
            }

            // Save previous position
            prevX = x;
            prevY = y;
            prevZ = z;

            // Step to next voxel
            if (tMaxX < tMaxY) {
                if (tMaxX < tMaxZ) {
                    x += stepX;
                    distance = tMaxX;
                    tMaxX += tDeltaX;
                    face = stepX > 0 ? 5 : 4; // West or East
                } else {
                    z += stepZ;
                    distance = tMaxZ;
                    tMaxZ += tDeltaZ;
                    face = stepZ > 0 ? 2 : 3; // North or South
                }
            } else {
                if (tMaxY < tMaxZ) {
                    y += stepY;
                    distance = tMaxY;
                    tMaxY += tDeltaY;
                    face = stepY > 0 ? 1 : 0; // Bottom or Top
                } else {
                    z += stepZ;
                    distance = tMaxZ;
                    tMaxZ += tDeltaZ;
                    face = stepZ > 0 ? 2 : 3; // North or South
                }
            }
        }

        return RaycastResult.miss();
    }

    /**
     * Cast a ray to find the first living entity hit.
     * Uses AABB-ray intersection with hitbox expansion for forgiving hit detection.
     * 
     * @param entities    List of entities to check
     * @param origin      Ray start position
     * @param direction   Ray direction (should be normalized)
     * @param maxDistance Maximum distance to check (3.0 blocks for combat)
     * @param exclude     Entity to exclude from check (usually the player)
     * @return EntityRaycastResult with hit information
     */
    public static EntityRaycastResult castEntities(List<Entity> entities, Vector3f origin,
            Vector3f direction, float maxDistance, Entity exclude) {

        LivingEntity closestEntity = null;
        float closestDistance = maxDistance;
        Vector3f closestHitPoint = null;

        // Hitbox expansion for forgiving hit detection (Minecraft uses ~0.1-0.3)
        float expansion = 0.1f;

        for (Entity entity : entities) {
            if (entity == exclude)
                continue;
            if (!(entity instanceof LivingEntity living))
                continue;
            if (living.isDead())
                continue;

            // Get entity AABB with expansion
            AABB box = living.getBoundingBox();
            if (box == null)
                continue;

            // Expand the box slightly for more forgiving hit detection
            AABB expandedBox = box.expand(expansion);

            // Ray-AABB intersection test
            float t = rayIntersectsAABB(origin, direction, expandedBox);

            if (t >= 0 && t < closestDistance) {
                closestDistance = t;
                closestEntity = living;
                closestHitPoint = new Vector3f(
                        origin.x + direction.x * t,
                        origin.y + direction.y * t,
                        origin.z + direction.z * t);
            }
        }

        if (closestEntity != null) {
            return new EntityRaycastResult(true, closestEntity, closestDistance, closestHitPoint);
        }

        return EntityRaycastResult.miss();
    }

    /**
     * Ray-AABB intersection using the slab method.
     * Returns the distance to intersection, or -1 if no hit.
     */
    private static float rayIntersectsAABB(Vector3f origin, Vector3f direction, AABB box) {
        float tMin = 0.0f;
        float tMax = Float.MAX_VALUE;

        Vector3f min = box.getMin();
        Vector3f max = box.getMax();

        // X slab
        if (Math.abs(direction.x) < 0.0001f) {
            if (origin.x < min.x || origin.x > max.x) {
                return -1;
            }
        } else {
            float invD = 1.0f / direction.x;
            float t0 = (min.x - origin.x) * invD;
            float t1 = (max.x - origin.x) * invD;
            if (invD < 0) {
                float tmp = t0;
                t0 = t1;
                t1 = tmp;
            }
            tMin = Math.max(tMin, t0);
            tMax = Math.min(tMax, t1);
            if (tMin > tMax)
                return -1;
        }

        // Y slab
        if (Math.abs(direction.y) < 0.0001f) {
            if (origin.y < min.y || origin.y > max.y) {
                return -1;
            }
        } else {
            float invD = 1.0f / direction.y;
            float t0 = (min.y - origin.y) * invD;
            float t1 = (max.y - origin.y) * invD;
            if (invD < 0) {
                float tmp = t0;
                t0 = t1;
                t1 = tmp;
            }
            tMin = Math.max(tMin, t0);
            tMax = Math.min(tMax, t1);
            if (tMin > tMax)
                return -1;
        }

        // Z slab
        if (Math.abs(direction.z) < 0.0001f) {
            if (origin.z < min.z || origin.z > max.z) {
                return -1;
            }
        } else {
            float invD = 1.0f / direction.z;
            float t0 = (min.z - origin.z) * invD;
            float t1 = (max.z - origin.z) * invD;
            if (invD < 0) {
                float tmp = t0;
                t0 = t1;
                t1 = tmp;
            }
            tMin = Math.max(tMin, t0);
            tMax = Math.min(tMax, t1);
            if (tMin > tMax)
                return -1;
        }

        return tMin;
    }

    /**
     * Calculate the distance to the first integer boundary in a direction.
     */
    private static float intBound(float s, float ds) {
        if (ds == 0) {
            return Float.MAX_VALUE;
        }

        if (ds < 0) {
            s = -s;
            ds = -ds;
            if (Math.floor(s) == s) {
                return 0;
            }
        }

        return (1 - (s - (float) Math.floor(s))) / ds;
    }
}
