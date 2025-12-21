package com.craftzero.entity.ai.pathfinding;

import java.util.ArrayList;
import java.util.List;

/**
 * A complete path from start to goal.
 * Contains a list of PathNodes to follow.
 */
public class Path {

    private final List<PathNode> nodes;
    private int currentIndex;
    private final PathNode target;

    public Path(List<PathNode> nodes, PathNode target) {
        this.nodes = nodes != null ? nodes : new ArrayList<>();
        this.currentIndex = 0;
        this.target = target;
    }

    /**
     * Create an empty (failed) path.
     */
    public static Path empty() {
        return new Path(new ArrayList<>(), null);
    }

    /**
     * Check if path is valid (has nodes to follow).
     */
    public boolean isValid() {
        return !nodes.isEmpty();
    }

    /**
     * Check if we've reached the end of the path.
     */
    public boolean isDone() {
        return currentIndex >= nodes.size();
    }

    /**
     * Get the current node to move toward.
     */
    public PathNode getCurrentNode() {
        if (currentIndex < nodes.size()) {
            return nodes.get(currentIndex);
        }
        return null;
    }

    /**
     * Get the final destination node.
     */
    public PathNode getFinalNode() {
        if (nodes.isEmpty())
            return null;
        return nodes.get(nodes.size() - 1);
    }

    /**
     * Advance to the next node.
     */
    public void advance() {
        if (currentIndex < nodes.size()) {
            currentIndex++;
        }
    }

    /**
     * Get current progress (0.0 to 1.0).
     */
    public float getProgress() {
        if (nodes.isEmpty())
            return 1.0f;
        return (float) currentIndex / nodes.size();
    }

    /**
     * Get number of nodes in path.
     */
    public int getLength() {
        return nodes.size();
    }

    /**
     * Get remaining nodes to visit.
     */
    public int getRemainingNodes() {
        return Math.max(0, nodes.size() - currentIndex);
    }

    /**
     * Check if entity is close enough to current node to advance.
     */
    public boolean shouldAdvance(float entityX, float entityY, float entityZ) {
        PathNode current = getCurrentNode();
        if (current == null)
            return false;

        float dx = entityX - current.getCenterX();
        float dy = entityY - current.getCenterY();
        float dz = entityZ - current.getCenterZ();

        // Horizontal distance (ignore Y for now - we're checking block position)
        float horizontalDist = (float) Math.sqrt(dx * dx + dz * dz);

        // Close enough horizontally and within 2 blocks vertically
        return horizontalDist < 0.6f && Math.abs(dy) < 2.0f;
    }

    /**
     * Get the target we're trying to reach.
     */
    public PathNode getTarget() {
        return target;
    }

    /**
     * Check if target has moved significantly (path needs recalculation).
     */
    public boolean isTargetStale(float newTargetX, float newTargetY, float newTargetZ) {
        if (target == null)
            return true;
        float dx = newTargetX - target.getCenterX();
        float dy = newTargetY - target.getCenterY();
        float dz = newTargetZ - target.getCenterZ();
        return dx * dx + dy * dy + dz * dz > 4.0f; // Moved more than 2 blocks
    }

    /**
     * Reset to start of path.
     */
    public void reset() {
        currentIndex = 0;
    }

    /**
     * Get all nodes (for debugging/rendering).
     */
    public List<PathNode> getAllNodes() {
        return nodes;
    }
}
