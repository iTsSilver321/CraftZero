package com.craftzero.physics;

import com.craftzero.entity.Entity;
import com.craftzero.entity.LivingEntity;
import com.craftzero.world.BlockType;
import com.craftzero.world.FluidState;
import com.craftzero.world.World;
import org.joml.Vector3f;
import org.joml.Vector3i;

import java.util.List;
import java.util.function.Predicate;

/**
 * DDA Raycasting for block selection, entity interaction, and combat.
 * Returns the exact block/entity coordinates hit by a ray.
 */
public class Raycast {
    private static final float RAY_EPSILON = 0.0001f;

    /**
     * Result of a raycast operation.
     */
    public static class RaycastResult {
        public final boolean hit;
        public final Vector3i blockPos;
        public final Vector3i previousBlockPos; // For block placement
        public final int face; // 0=top, 1=bottom, 2=north, 3=south, 4=east, 5=west
        public final float distance;
        public final AABB selectionBox;
        public final Vector3f hitPoint;

        public RaycastResult(boolean hit, Vector3i blockPos, Vector3i previousBlockPos, int face, float distance) {
            this(hit, blockPos, previousBlockPos, face, distance, null);
        }

        public RaycastResult(boolean hit, Vector3i blockPos, Vector3i previousBlockPos, int face, float distance,
                AABB selectionBox) {
            this(hit, blockPos, previousBlockPos, face, distance, selectionBox, null);
        }

        public RaycastResult(boolean hit, Vector3i blockPos, Vector3i previousBlockPos, int face, float distance,
                AABB selectionBox, Vector3f hitPoint) {
            this.hit = hit;
            this.blockPos = blockPos;
            this.previousBlockPos = previousBlockPos;
            this.face = face;
            this.distance = distance;
            this.selectionBox = selectionBox;
            this.hitPoint = hitPoint;
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
        public final Entity entity;
        public final float distance;
        public final Vector3f hitPoint;

        public EntityRaycastResult(boolean hit, Entity entity, float distance, Vector3f hitPoint) {
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
        Vector3f rayDirection = normalizedDirection(direction);
        if (world == null || !isFiniteVector(origin) || rayDirection == null
                || !Float.isFinite(maxDistance) || maxDistance <= 0.0f) {
            return RaycastResult.miss();
        }

        // Current voxel coordinates
        int x = (int) Math.floor(origin.x);
        int y = (int) Math.floor(origin.y);
        int z = (int) Math.floor(origin.z);

        // Previous position for block placement
        int prevX = x, prevY = y, prevZ = z;

        // Direction signs
        int stepX = rayDirection.x > 0 ? 1 : (rayDirection.x < 0 ? -1 : 0);
        int stepY = rayDirection.y > 0 ? 1 : (rayDirection.y < 0 ? -1 : 0);
        int stepZ = rayDirection.z > 0 ? 1 : (rayDirection.z < 0 ? -1 : 0);

        // Calculate tMax and tDelta
        float tMaxX = intBound(origin.x, rayDirection.x);
        float tMaxY = intBound(origin.y, rayDirection.y);
        float tMaxZ = intBound(origin.z, rayDirection.z);

        float tDeltaX = rayDirection.x != 0 ? Math.abs(1.0f / rayDirection.x) : Float.MAX_VALUE;
        float tDeltaY = rayDirection.y != 0 ? Math.abs(1.0f / rayDirection.y) : Float.MAX_VALUE;
        float tDeltaZ = rayDirection.z != 0 ? Math.abs(1.0f / rayDirection.z) : Float.MAX_VALUE;

        float distance = 0;
        int face = -1;

        // March through voxels
        while (distance < maxDistance) {
            // Check current voxel
            BlockHit blockHit = rayIntersectsBlock(world.getSelectionBoxesIfLoaded(x, y, z), origin, rayDirection,
                    maxDistance);
            if (blockHit != null) {
                return new RaycastResult(
                        true,
                        new Vector3i(x, y, z),
                        new Vector3i(prevX, prevY, prevZ),
                        blockHit.face,
                        blockHit.distance,
                        blockHit.box,
                        new Vector3f(
                                origin.x + rayDirection.x * blockHit.distance,
                                origin.y + rayDirection.y * blockHit.distance,
                                origin.z + rayDirection.z * blockHit.distance));
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
     * Cast a bucket-style ray that can hit source liquid blocks while keeping
     * ordinary selection blocks as blockers. Release-era bucket and bottle use did
     * a liquid-aware trace instead of changing the normal crosshair selection ray.
     */
    public static RaycastResult castFluidSource(World world, Vector3f origin, Vector3f direction, float maxDistance) {
        Vector3f rayDirection = normalizedDirection(direction);
        if (world == null || !isFiniteVector(origin) || rayDirection == null
                || !Float.isFinite(maxDistance) || maxDistance <= 0.0f) {
            return RaycastResult.miss();
        }

        int x = (int) Math.floor(origin.x);
        int y = (int) Math.floor(origin.y);
        int z = (int) Math.floor(origin.z);

        int prevX = x, prevY = y, prevZ = z;

        int stepX = rayDirection.x > 0 ? 1 : (rayDirection.x < 0 ? -1 : 0);
        int stepY = rayDirection.y > 0 ? 1 : (rayDirection.y < 0 ? -1 : 0);
        int stepZ = rayDirection.z > 0 ? 1 : (rayDirection.z < 0 ? -1 : 0);

        float tMaxX = intBound(origin.x, rayDirection.x);
        float tMaxY = intBound(origin.y, rayDirection.y);
        float tMaxZ = intBound(origin.z, rayDirection.z);

        float tDeltaX = rayDirection.x != 0 ? Math.abs(1.0f / rayDirection.x) : Float.MAX_VALUE;
        float tDeltaY = rayDirection.y != 0 ? Math.abs(1.0f / rayDirection.y) : Float.MAX_VALUE;
        float tDeltaZ = rayDirection.z != 0 ? Math.abs(1.0f / rayDirection.z) : Float.MAX_VALUE;

        float distance = 0;

        while (distance < maxDistance) {
            BlockType type = world.getBlockIfLoaded(x, y, z, BlockType.AIR);
            if (type.isFluid() && FluidState.isSource(world.getBlockMetadataIfLoaded(x, y, z, 0))) {
                AABB fluidBox = new AABB(x, y, z, x + 1, y + 1, z + 1);
                BlockHit hit = rayIntersectsAABBWithFace(origin, rayDirection, fluidBox);
                if (hit != null && hit.distance >= 0 && hit.distance <= maxDistance) {
                    return new RaycastResult(
                            true,
                            new Vector3i(x, y, z),
                            new Vector3i(prevX, prevY, prevZ),
                            hit.face,
                            hit.distance,
                            hit.box,
                            new Vector3f(
                                    origin.x + rayDirection.x * hit.distance,
                                    origin.y + rayDirection.y * hit.distance,
                                    origin.z + rayDirection.z * hit.distance));
                }
            } else if (type != BlockType.AIR) {
                BlockHit blocker = rayIntersectsBlock(world.getSelectionBoxesIfLoaded(x, y, z), origin, rayDirection,
                        maxDistance);
                if (blocker != null) {
                    return RaycastResult.miss();
                }
            }

            prevX = x;
            prevY = y;
            prevZ = z;

            if (tMaxX < tMaxY) {
                if (tMaxX < tMaxZ) {
                    x += stepX;
                    distance = tMaxX;
                    tMaxX += tDeltaX;
                } else {
                    z += stepZ;
                    distance = tMaxZ;
                    tMaxZ += tDeltaZ;
                }
            } else {
                if (tMaxY < tMaxZ) {
                    y += stepY;
                    distance = tMaxY;
                    tMaxY += tDeltaY;
                } else {
                    z += stepZ;
                    distance = tMaxZ;
                    tMaxZ += tDeltaZ;
                }
            }
        }

        return RaycastResult.miss();
    }

    private static BlockHit rayIntersectsBlock(List<AABB> boxes, Vector3f origin, Vector3f direction,
            float maxDistance) {
        if (boxes == null) {
            return null;
        }
        BlockHit closest = null;
        for (AABB box : boxes) {
            BlockHit hit = rayIntersectsAABBWithFace(origin, direction, box);
            if (hit != null && hit.distance >= 0 && hit.distance <= maxDistance
                    && (closest == null || hit.distance < closest.distance)) {
                closest = hit;
            }
        }
        return closest;
    }

    private static BlockHit rayIntersectsAABBWithFace(Vector3f origin, Vector3f direction, AABB box) {
        if (!isFiniteVector(origin) || !isFiniteVector(direction) || box == null || !box.isFinite()) {
            return null;
        }
        box = new AABB(box.getMin(), box.getMax());
        float tMin = 0.0f;
        float tMax = Float.MAX_VALUE;
        int hitFace = -1;

        float[] originValues = { origin.x, origin.y, origin.z };
        float[] directionValues = { direction.x, direction.y, direction.z };
        float[] mins = { box.getMin().x, box.getMin().y, box.getMin().z };
        float[] maxs = { box.getMax().x, box.getMax().y, box.getMax().z };

        for (int axis = 0; axis < 3; axis++) {
            float dir = directionValues[axis];
            float start = originValues[axis];
            if (Math.abs(dir) < RAY_EPSILON) {
                if (start < mins[axis] || start > maxs[axis]) {
                    return null;
                }
                continue;
            }

            float invD = 1.0f / dir;
            float t0 = (mins[axis] - start) * invD;
            float t1 = (maxs[axis] - start) * invD;
            int nearFace = faceForAxis(axis, dir > 0);
            if (invD < 0) {
                float tmp = t0;
                t0 = t1;
                t1 = tmp;
                nearFace = faceForAxis(axis, false);
            }
            if (t0 > tMin) {
                tMin = t0;
                hitFace = nearFace;
            }
            tMax = Math.min(tMax, t1);
            if (tMin > tMax) {
                return null;
            }
        }

        return new BlockHit(tMin, hitFace, box);
    }

    private static int faceForAxis(int axis, boolean positiveDirection) {
        return switch (axis) {
            case 0 -> positiveDirection ? com.craftzero.world.Block.FACE_WEST : com.craftzero.world.Block.FACE_EAST;
            case 1 -> positiveDirection ? com.craftzero.world.Block.FACE_BOTTOM : com.craftzero.world.Block.FACE_TOP;
            default -> positiveDirection ? com.craftzero.world.Block.FACE_NORTH : com.craftzero.world.Block.FACE_SOUTH;
        };
    }

    private static class BlockHit {
        final float distance;
        final int face;
        final AABB box;

        BlockHit(float distance, int face, AABB box) {
            this.distance = distance;
            this.face = face;
            this.box = box;
        }
    }

    public static AABB entityPickBox(Entity entity) {
        if (entity == null) {
            return null;
        }
        return expandForEntityPick(entity.getBoundingBox(), entity.getCollisionBorderSize());
    }

    public static AABB playerPickBox(AABB playerBounds) {
        return expandForEntityPick(playerBounds, Entity.DEFAULT_COLLISION_BORDER_SIZE);
    }

    public static AABB expandForEntityPick(AABB bounds, float collisionBorderSize) {
        if (bounds == null || !bounds.isFinite()) {
            return null;
        }
        return bounds.expand(Math.max(0.0f, finiteOrZero(collisionBorderSize)));
    }

    public static Vector3f pointAt(Vector3f origin, Vector3f direction, float distance) {
        Vector3f rayDirection = normalizedDirection(direction);
        if (!isFiniteVector(origin) || rayDirection == null || !Float.isFinite(distance)) {
            return new Vector3f();
        }
        return new Vector3f(
                origin.x + rayDirection.x * distance,
                origin.y + rayDirection.y * distance,
                origin.z + rayDirection.z * distance);
    }

    /**
     * Cast a ray to find the first living entity hit.
     * Uses the Release-era entity collision border when expanding hit boxes.
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
        Vector3f rayDirection = normalizedDirection(direction);
        if (entities == null || !isFiniteVector(origin) || rayDirection == null
                || !Float.isFinite(maxDistance) || maxDistance <= 0.0f) {
            return EntityRaycastResult.miss();
        }

        LivingEntity closestEntity = null;
        float closestDistance = maxDistance;
        Vector3f closestHitPoint = null;

        for (Entity entity : entities) {
            if (entity == null || entity == exclude || entity.isRemoved())
                continue;
            if (!(entity instanceof LivingEntity living))
                continue;
            if (living.isDead())
                continue;

            // Get entity AABB with expansion
            AABB box = entityPickBox(living);
            if (box == null)
                continue;

            // Ray-AABB intersection test
            float t = intersectsAabb(origin, rayDirection, box);

            if (t >= 0 && t < closestDistance) {
                closestDistance = t;
                closestEntity = living;
                closestHitPoint = pointAt(origin, rayDirection, t);
            }
        }

        if (closestEntity != null) {
            return new EntityRaycastResult(true, closestEntity, closestDistance, closestHitPoint);
        }

        return EntityRaycastResult.miss();
    }

    public static EntityRaycastResult castEntitiesMatching(List<Entity> entities, Vector3f origin,
            Vector3f direction, float maxDistance, Entity exclude, Predicate<Entity> predicate) {
        Vector3f rayDirection = normalizedDirection(direction);
        if (entities == null || !isFiniteVector(origin) || rayDirection == null
                || !Float.isFinite(maxDistance) || maxDistance <= 0.0f) {
            return EntityRaycastResult.miss();
        }

        Entity closestEntity = null;
        float closestDistance = maxDistance;
        Vector3f closestHitPoint = null;

        for (Entity entity : entities) {
            if (entity == null || entity == exclude || entity.isRemoved()) {
                continue;
            }
            if (predicate != null && !predicate.test(entity)) {
                continue;
            }
            if (entity instanceof LivingEntity living && living.isDead()) {
                continue;
            }

            AABB box = entityPickBox(entity);
            if (box == null) {
                continue;
            }

            float t = intersectsAabb(origin, rayDirection, box);

            if (t >= 0 && t < closestDistance) {
                closestDistance = t;
                closestEntity = entity;
                closestHitPoint = pointAt(origin, rayDirection, t);
            }
        }

        if (closestEntity != null) {
            return new EntityRaycastResult(true, closestEntity, closestDistance, closestHitPoint);
        }

        return EntityRaycastResult.miss();
    }

    /**
     * Cast a ray to find the first entity hit, including non-living entities such
     * as minecarts that can be interacted with by right click.
     */
    public static EntityRaycastResult castAnyEntity(List<Entity> entities, Vector3f origin,
            Vector3f direction, float maxDistance, Entity exclude) {
        Vector3f rayDirection = normalizedDirection(direction);
        if (entities == null || !isFiniteVector(origin) || rayDirection == null
                || !Float.isFinite(maxDistance) || maxDistance <= 0.0f) {
            return EntityRaycastResult.miss();
        }

        Entity closestEntity = null;
        float closestDistance = maxDistance;
        Vector3f closestHitPoint = null;

        for (Entity entity : entities) {
            if (entity == null || entity == exclude || entity.isRemoved()) {
                continue;
            }
            if (entity instanceof LivingEntity living && living.isDead()) {
                continue;
            }

            AABB box = entityPickBox(entity);
            if (box == null) {
                continue;
            }

            float t = intersectsAabb(origin, rayDirection, box);

            if (t >= 0 && t < closestDistance) {
                closestDistance = t;
                closestEntity = entity;
                closestHitPoint = pointAt(origin, rayDirection, t);
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
    public static float intersectsAabb(Vector3f origin, Vector3f direction, AABB box) {
        Vector3f rayDirection = normalizedDirection(direction);
        if (!isFiniteVector(origin) || rayDirection == null || box == null || !box.isFinite()) {
            return -1.0f;
        }
        float tMin = 0.0f;
        float tMax = Float.MAX_VALUE;

        AABB safeBox = new AABB(box.getMin(), box.getMax());
        Vector3f min = safeBox.getMin();
        Vector3f max = safeBox.getMax();

        // X slab
        if (Math.abs(rayDirection.x) < RAY_EPSILON) {
            if (origin.x < min.x || origin.x > max.x) {
                return -1;
            }
        } else {
            float invD = 1.0f / rayDirection.x;
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
        if (Math.abs(rayDirection.y) < RAY_EPSILON) {
            if (origin.y < min.y || origin.y > max.y) {
                return -1;
            }
        } else {
            float invD = 1.0f / rayDirection.y;
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
        if (Math.abs(rayDirection.z) < RAY_EPSILON) {
            if (origin.z < min.z || origin.z > max.z) {
                return -1;
            }
        } else {
            float invD = 1.0f / rayDirection.z;
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

    private static Vector3f normalizedDirection(Vector3f direction) {
        if (!isFiniteVector(direction)) {
            return null;
        }
        float lengthSquared = direction.x * direction.x + direction.y * direction.y + direction.z * direction.z;
        if (!Float.isFinite(lengthSquared) || lengthSquared <= RAY_EPSILON * RAY_EPSILON) {
            return null;
        }
        return new Vector3f(direction).mul(1.0f / (float) Math.sqrt(lengthSquared));
    }

    private static boolean isFiniteVector(Vector3f value) {
        return value != null
                && Float.isFinite(value.x)
                && Float.isFinite(value.y)
                && Float.isFinite(value.z);
    }

    private static float finiteOrZero(float value) {
        return Float.isFinite(value) ? value : 0.0f;
    }
}
