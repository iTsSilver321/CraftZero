package com.craftzero.entity.ai.pathfinding;

/**
 * A single node in a pathfinding path.
 * Represents one block position with A* algorithm data.
 */
public class PathNode implements Comparable<PathNode> {

    // Block coordinates
    public final int x;
    public int y; // Non-final to allow ground level adjustment
    public final int z;

    // A* algorithm costs
    public float gCost; // Cost from start to this node
    public float hCost; // Heuristic (estimated cost to goal)
    public float fCost; // gCost + hCost (total cost)

    // Parent node for path reconstruction
    public PathNode parent;

    // Node type/penalty
    public NodeType type = NodeType.WALKABLE;
    public float penalty = 0; // Additional cost (water, soul sand, etc.)

    // State for A* algorithm
    public boolean visited = false;
    public boolean inOpenSet = false;

    public PathNode(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    /**
     * Calculate heuristic distance to target (Manhattan distance with Y weight).
     */
    public void calculateHeuristic(int targetX, int targetY, int targetZ) {
        // Manhattan distance, but Y costs more (climbing is harder)
        float dx = Math.abs(targetX - x);
        float dy = Math.abs(targetY - y);
        float dz = Math.abs(targetZ - z);
        this.hCost = dx + dy * 1.5f + dz;
    }

    /**
     * Calculate total cost (f = g + h).
     */
    public void calculateFCost() {
        this.fCost = gCost + hCost + penalty;
    }

    /**
     * Get the center position of this block.
     */
    public float getCenterX() {
        return x + 0.5f;
    }

    public float getCenterY() {
        return y;
    }

    public float getCenterZ() {
        return z + 0.5f;
    }

    /**
     * Check if this is the same block position.
     */
    public boolean matches(int bx, int by, int bz) {
        return x == bx && y == by && z == bz;
    }

    @Override
    public int compareTo(PathNode other) {
        // Lower fCost = higher priority
        int fCompare = Float.compare(this.fCost, other.fCost);
        if (fCompare != 0)
            return fCompare;
        // Tie-breaker: prefer lower hCost (closer to goal)
        return Float.compare(this.hCost, other.hCost);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!(obj instanceof PathNode other))
            return false;
        return x == other.x && y == other.y && z == other.z;
    }

    @Override
    public int hashCode() {
        return x * 31 * 31 + y * 31 + z;
    }

    @Override
    public String toString() {
        return "PathNode(" + x + ", " + y + ", " + z + " f=" + fCost + ")";
    }

    /**
     * Types of nodes for pathfinding.
     */
    public enum NodeType {
        WALKABLE, // Normal ground
        BLOCKED, // Solid block, can't pass
        DANGEROUS, // Lava, fire, cactus - avoid
        WATER, // Swimmable but slow
        OPEN_BELOW, // Air with drop below (cliff)
        DOOR, // Special handling for doors
        FENCE // Can't jump over
    }
}
