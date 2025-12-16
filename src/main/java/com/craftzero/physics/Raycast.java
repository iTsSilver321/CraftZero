package com.craftzero.physics;

import com.craftzero.world.BlockType;
import com.craftzero.world.World;
import org.joml.Vector3f;
import org.joml.Vector3i;

/**
 * DDA Raycasting for block selection and interaction.
 * Returns the exact block coordinates hit by a ray.
 */
public class Raycast {
    
    /**
     * Result of a raycast operation.
     */
    public static class RaycastResult {
        public final boolean hit;
        public final Vector3i blockPos;
        public final Vector3i previousBlockPos;  // For block placement
        public final int face;  // 0=top, 1=bottom, 2=north, 3=south, 4=east, 5=west
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
     * Cast a ray from origin in direction, returning the first solid block hit.
     * Uses DDA (Digital Differential Analyzer) algorithm.
     * 
     * @param world The world to cast ray in
     * @param origin Ray start position
     * @param direction Ray direction (should be normalized)
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
                    distance
                );
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
                    face = stepX > 0 ? 5 : 4;  // West or East
                } else {
                    z += stepZ;
                    distance = tMaxZ;
                    tMaxZ += tDeltaZ;
                    face = stepZ > 0 ? 2 : 3;  // North or South
                }
            } else {
                if (tMaxY < tMaxZ) {
                    y += stepY;
                    distance = tMaxY;
                    tMaxY += tDeltaY;
                    face = stepY > 0 ? 1 : 0;  // Bottom or Top
                } else {
                    z += stepZ;
                    distance = tMaxZ;
                    tMaxZ += tDeltaZ;
                    face = stepZ > 0 ? 2 : 3;  // North or South
                }
            }
        }
        
        return RaycastResult.miss();
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
