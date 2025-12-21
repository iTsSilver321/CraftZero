package com.craftzero.entity.ai.pathfinding;

import com.craftzero.world.BlockType;
import com.craftzero.world.World;

/**
 * Evaluates blocks for pathfinding.
 * Determines if blocks are walkable, blocked, or dangerous.
 */
public class PathNodeEvaluator {

    private final World world;
    private final float entityWidth;
    private final float entityHeight;

    // Maximum fall distance before considering it a cliff
    private static final int MAX_FALL_DISTANCE = 3;

    public PathNodeEvaluator(World world, float entityWidth, float entityHeight) {
        this.world = world;
        this.entityWidth = entityWidth;
        this.entityHeight = entityHeight;
    }

    /**
     * Evaluate a node at the given position.
     * Sets the node's type and penalty.
     */
    public void evaluateNode(PathNode node) {
        int x = node.x;
        int y = node.y;
        int z = node.z;

        // Check if entity can stand here
        BlockType groundBlock = getBlock(x, y - 1, z);
        BlockType feetBlock = getBlock(x, y, z);
        BlockType headBlock = getBlock(x, y + 1, z);

        // Must have solid ground
        if (groundBlock == null || !groundBlock.isSolid()) {
            // Check if this is a cliff (drop > MAX_FALL_DISTANCE)
            if (isCliff(x, y, z)) {
                node.type = PathNode.NodeType.OPEN_BELOW;
                node.penalty = 100; // High cost - avoid cliffs
                return;
            }
            // Might be able to drop down
            int dropY = findGround(x, y, z);
            if (dropY >= 0) {
                node.y = dropY; // Adjust to actual ground level
            } else {
                node.type = PathNode.NodeType.BLOCKED;
                return;
            }
        }

        // Check if feet and head space is clear
        if (feetBlock != null && feetBlock.isSolid() && !isDoor(feetBlock)) {
            node.type = PathNode.NodeType.BLOCKED;
            return;
        }
        if (headBlock != null && headBlock.isSolid() && !isDoor(headBlock)) {
            node.type = PathNode.NodeType.BLOCKED;
            return;
        }

        // Check for dangerous blocks
        if (isDangerous(groundBlock) || isDangerous(feetBlock)) {
            node.type = PathNode.NodeType.DANGEROUS;
            node.penalty = 1000; // Avoid at all costs
            return;
        }

        // Check for water
        if (isWater(feetBlock)) {
            node.type = PathNode.NodeType.WATER;
            node.penalty = 5; // Slower in water
            return;
        }

        // Check for fence (can't jump over)
        if (isFence(groundBlock)) {
            node.type = PathNode.NodeType.FENCE;
            node.penalty = 50; // Very hard to cross
            return;
        }

        // Normal walkable ground
        node.type = PathNode.NodeType.WALKABLE;
        node.penalty = 0;

        // Add small penalty for certain slowing blocks
        if (groundBlock != null) {
            if (groundBlock.name().contains("SOUL") || groundBlock.name().contains("SAND")) {
                node.penalty = 2; // Slightly slower
            }
        }
    }

    /**
     * Check if position is a cliff (large drop).
     */
    private boolean isCliff(int x, int y, int z) {
        for (int checkY = y - 1; checkY >= y - MAX_FALL_DISTANCE - 1; checkY--) {
            BlockType block = getBlock(x, checkY, z);
            if (block != null && block.isSolid()) {
                return false; // Found ground within safe distance
            }
        }
        return true; // No ground found - cliff!
    }

    /**
     * Find ground level below a position.
     */
    private int findGround(int x, int y, int z) {
        for (int checkY = y - 1; checkY >= y - MAX_FALL_DISTANCE; checkY--) {
            BlockType block = getBlock(x, checkY, z);
            if (block != null && block.isSolid()) {
                return checkY + 1; // Stand on top of this block
            }
        }
        return -1; // No ground found
    }

    /**
     * Check if block is dangerous (lava, fire, cactus).
     */
    private boolean isDangerous(BlockType block) {
        if (block == null)
            return false;
        String name = block.name().toUpperCase();
        return name.contains("LAVA") ||
                name.contains("FIRE") ||
                name.contains("CACTUS") ||
                name.contains("MAGMA");
    }

    /**
     * Check if block is water.
     */
    private boolean isWater(BlockType block) {
        if (block == null)
            return false;
        return block.name().toUpperCase().contains("WATER");
    }

    /**
     * Check if block is a fence.
     */
    private boolean isFence(BlockType block) {
        if (block == null)
            return false;
        return block.name().toUpperCase().contains("FENCE");
    }

    /**
     * Check if block is a door.
     */
    private boolean isDoor(BlockType block) {
        if (block == null)
            return false;
        return block.name().toUpperCase().contains("DOOR");
    }

    /**
     * Get block at position (with bounds check).
     */
    private BlockType getBlock(int x, int y, int z) {
        if (world == null || y < 0 || y > 255)
            return null;
        return world.getBlock(x, y, z);
    }

    /**
     * Check if an entity can move from one node to another.
     * Used for checking if a step is valid.
     */
    public boolean canMoveBetween(PathNode from, PathNode to) {
        // Height difference check
        int dy = to.y - from.y;
        if (dy > 1)
            return false; // Can only jump up 1 block
        if (dy < -MAX_FALL_DISTANCE)
            return false; // Too far to drop

        // Check if destination is walkable
        if (to.type == PathNode.NodeType.BLOCKED)
            return false;
        if (to.type == PathNode.NodeType.DANGEROUS)
            return false;

        return true;
    }

    /**
     * Get the base movement cost between two adjacent nodes.
     */
    public float getMovementCost(PathNode from, PathNode to) {
        float baseCost = 1.0f;

        // Diagonal movement costs more
        int dx = Math.abs(to.x - from.x);
        int dz = Math.abs(to.z - from.z);
        if (dx > 0 && dz > 0) {
            baseCost = 1.414f; // sqrt(2)
        }

        // Jumping up costs more
        if (to.y > from.y) {
            baseCost += 0.5f;
        }

        // Add node penalty
        baseCost += to.penalty;

        return baseCost;
    }
}
